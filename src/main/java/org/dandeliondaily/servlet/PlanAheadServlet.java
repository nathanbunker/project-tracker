package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.dashboard.service.DashboardTodayColumnService;
import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.dandeliondaily.planahead.model.PlanAheadMutationResult;
import org.dandeliondaily.planahead.render.PlanAheadPageRenderer;
import org.dandeliondaily.planahead.service.PlanAheadBoardService;
import org.dandeliondaily.planahead.service.PlanAheadDayCapacityService;
import org.dandeliondaily.planahead.service.PlanAheadMutationService;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class PlanAheadServlet extends ClientServlet {

    private static final long serialVersionUID = 3972742500115325586L;

    private final PlanAheadBoardService boardService = new PlanAheadBoardService();
    private final PlanAheadPageRenderer pageRenderer = new PlanAheadPageRenderer();
    private final PlanAheadDayCapacityService dayCapacityService = new PlanAheadDayCapacityService();
    private final PlanAheadMutationService mutationService = new PlanAheadMutationService();
    private final DashboardTodayColumnService dashboardTodayColumnService = new DashboardTodayColumnService();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            String action = request.getParameter("action");
            if ("saveDayCapacity".equals(action)) {
                handleSaveDayCapacity(appReq);
                return;
            }
            if ("loadBoard".equals(action)) {
                handleLoadBoard(appReq);
                return;
            }
            if ("moveCard".equals(action)) {
                handleMutationResult(appReq, mutationService.moveCard(appReq));
                return;
            }
            if ("loadCardEdit".equals(action)) {
                handleMutationResult(appReq, mutationService.loadCardEdit(appReq));
                return;
            }
            if ("saveCardEdit".equals(action)) {
                handleMutationResult(appReq, mutationService.saveCardEdit(appReq));
                return;
            }
            if ("loadTemplateEdit".equals(action)) {
                handleMutationResult(appReq, mutationService.loadTemplateEdit(appReq));
                return;
            }
            if ("saveTemplateEdit".equals(action)) {
                handleMutationResult(appReq, mutationService.saveTemplateEdit(appReq));
                return;
            }
            if ("deleteTemplateEdit".equals(action)) {
                handleMutationResult(appReq, mutationService.deleteTemplateEdit(appReq));
                return;
            }
            if ("toggleTemplateDay".equals(action)) {
                handleMutationResult(appReq, mutationService.toggleTemplateDay(appReq));
                return;
            }
            if ("refreshDayHeaders".equals(action)) {
                handleRefreshDayHeaders(appReq);
                return;
            }
            if ("shiftWindowForward".equals(action)) {
                handleShiftWindowForward(appReq);
                return;
            }

            if ("Schedule".equals(action)) {
                dashboardTodayColumnService.handleQuickCapture(appReq);
            }

            appReq.setTitle("Plan Ahead");
            Date windowStart = boardService.resolveWindowStart(appReq);
            PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);
            printHtmlHead(appReq);
            pageRenderer.render(appReq, boardModel);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void handleShiftWindowForward(AppReq appReq) throws Exception {
        Date currentStart = boardService.resolveWindowStart(appReq);
        java.util.Calendar c = appReq.getWebUser().getCalendar();
        c.setTime(currentStart);
        int days = 1;
        String daysParam = appReq.getRequest().getParameter("days");
        if (daysParam != null && daysParam.trim().length() > 0) {
            try {
                days = Integer.parseInt(daysParam.trim());
            } catch (NumberFormatException nfe) {
                days = 1;
            }
        }
        c.add(java.util.Calendar.DAY_OF_MONTH, Math.max(days, 1));
        String nextStart = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        appReq.getResponse().sendRedirect("PlanAheadServlet?windowStart=" + nextStart);
    }

    private void handleRefreshDayHeaders(AppReq appReq) throws Exception {
        String dayKeysCsv = appReq.getRequest().getParameter("dayKeys");
        if (dayKeysCsv == null || dayKeysCsv.trim().length() == 0) {
            sendJsonResponse(appReq, false, "dayKeys parameter is required", null);
            return;
        }
        Set<String> requestedKeys = new LinkedHashSet<String>();
        for (String part : dayKeysCsv.split(",")) {
            String k = part.trim();
            if (k.length() > 0) {
                requestedKeys.add(k);
            }
        }
        if (requestedKeys.isEmpty()) {
            sendJsonResponse(appReq, false, "dayKeys must contain at least one valid key", null);
            return;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);

        Map<String, Object> dayHeaders = new LinkedHashMap<String, Object>();
        for (PlanAheadBoardModel.DayHeaderModel dayHeader : boardModel.getDayHeaders()) {
            if (requestedKeys.contains(dayHeader.getDayKey())) {
                Map<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("headerHtml", pageRenderer.renderDayHeader(dayHeader));
                entry.put("plannedMins", dayHeader.getPlannedMins());
                entry.put("availableMins", dayHeader.getAvailableMins());
                entry.put("billMins", dayHeader.getBillMins());
                dayHeaders.put(dayHeader.getDayKey(), entry);
            }
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("dayHeaders", dayHeaders);
        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleLoadBoard(AppReq appReq) throws Exception {
        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);

        StringBuilder boardHtml = new StringBuilder();
        for (PlanAheadBoardModel.RowModel row : boardModel.getRows()) {
            boardHtml.append(row.getRowLabel()).append("|");
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("windowStart", boardModel.getWindowStartKey());
        data.put("windowEnd", boardModel.getWindowEndKey());
        data.put("boardSummary", boardHtml.toString());
        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleSaveDayCapacity(AppReq appReq) throws Exception {
        String billDate = appReq.getRequest().getParameter("billDate");
        String billMinsString = appReq.getRequest().getParameter("billMins");
        String workStatus = appReq.getRequest().getParameter("workStatus");

        Date parsedDay = parseDay(billDate);
        if (parsedDay == null) {
            sendJsonResponse(appReq, false, "billDate must be in yyyy-MM-dd format", null);
            return;
        }

        Integer parsedMinutes = parseMinutesInput(billMinsString);
        if (parsedMinutes == null) {
            sendJsonResponse(appReq, false, "billMins must be minutes or h:mm (for example 0:00 or 08:30)", null);
            return;
        }
        int billMins = parsedMinutes.intValue();

        try {
            dayCapacityService.saveDayCapacity(appReq, parsedDay, billMins, workStatus);
        } catch (IllegalArgumentException iae) {
            sendJsonResponse(appReq, false, iae.getMessage(), null);
            return;
        }

        Date windowStart = boardService.resolveWindowStart(appReq);
        PlanAheadBoardModel boardModel = boardService.buildBoard(appReq, windowStart);
        String dayKey = new SimpleDateFormat("yyyy-MM-dd").format(parsedDay);

        PlanAheadBoardModel.DayHeaderModel targetDay = null;
        for (PlanAheadBoardModel.DayHeaderModel dayHeader : boardModel.getDayHeaders()) {
            if (dayKey.equals(dayHeader.getDayKey())) {
                targetDay = dayHeader;
                break;
            }
        }

        if (targetDay == null) {
            sendJsonResponse(appReq, false, "Saved day is not in the visible window", null);
            return;
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("dayKey", dayKey);
        data.put("dayStatusHtml", pageRenderer.renderDayStatusCell(targetDay));
        data.put("dayHeaderHtml", pageRenderer.renderDayHeader(targetDay));
        data.put("dayAvailableMinutes", targetDay.getAvailableMins());
        data.put("dayPlannedMinutes", targetDay.getPlannedMins());
        sendJsonResponse(appReq, true, "Day capacity updated", data);
    }

    private Integer parseMinutesInput(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return Integer.valueOf(0);
        }
        if (text.matches("\\d+")) {
            try {
                return Integer.valueOf(Integer.parseInt(text));
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        if (text.matches("\\d{1,3}:\\d{2}")) {
            String[] parts = text.split(":");
            try {
                int hours = Integer.parseInt(parts[0]);
                int mins = Integer.parseInt(parts[1]);
                if (hours < 0 || mins < 0 || mins > 59) {
                    return null;
                }
                return Integer.valueOf((hours * 60) + mins);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    private void handleMutationResult(AppReq appReq, PlanAheadMutationResult result) throws Exception {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("affectedCellsHtml", result.getAffectedCellsHtml());
        data.put("affectedHeadersHtml", result.getAffectedHeadersHtml());
        data.put("data", result.getData());
        sendJsonResponse(appReq, result.isSuccess(), result.getMessage(), data);
    }

    private Date parseDay(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            return sdf.parse(value.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private void sendJsonResponse(AppReq appReq, boolean success, String message, Map<String, Object> data)
            throws Exception {
        appReq.getResponse().setContentType("application/json; charset=UTF-8");
        PrintWriter out = appReq.getResponse().getWriter();

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success).append(",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");

        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                json.append(",\"").append(entry.getKey()).append("\":");
                appendJsonValue(json, entry.getValue());
            }
        }

        json.append("}");
        out.println(json.toString());
        out.flush();
    }

    @SuppressWarnings("unchecked")
    private void appendJsonValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String) {
            json.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map) {
            json.append("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                appendJsonValue(json, entry.getValue());
                first = false;
            }
            json.append("}");
        } else if (value instanceof Iterable) {
            json.append("[");
            boolean first = true;
            for (Object item : (Iterable<Object>) value) {
                if (!first) {
                    json.append(",");
                }
                appendJsonValue(json, item);
                first = false;
            }
            json.append("]");
        } else if (value.getClass().isArray()) {
            json.append("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                appendJsonValue(json, Array.get(value, i));
            }
            json.append("]");
        } else {
            json.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
