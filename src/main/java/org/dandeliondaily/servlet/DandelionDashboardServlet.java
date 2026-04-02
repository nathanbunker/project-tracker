package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.dandeliondaily.dashboard.model.DashboardNowColumnModel;
import org.dandeliondaily.dashboard.model.DashboardNextColumnModel;
import org.dandeliondaily.dashboard.model.DashboardTodayColumnModel;
import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.dandeliondaily.dashboard.render.DashboardPageRenderer;
import org.dandeliondaily.dashboard.render.TimeGaugeRenderer;
import org.dandeliondaily.dashboard.service.DashboardCurrentActionService;
import org.dandeliondaily.dashboard.service.DashboardNowColumnService;
import org.dandeliondaily.dashboard.service.DashboardNextColumnService;
import org.dandeliondaily.dashboard.service.DashboardTimeGaugeService;
import org.dandeliondaily.dashboard.service.DashboardTodayColumnService;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class DandelionDashboardServlet extends ClientServlet {

    private static final long serialVersionUID = 6049052526445852256L;

    private final DashboardPageRenderer dashboardPageRenderer = new DashboardPageRenderer();
    private final DashboardNowColumnService dashboardNowColumnService = new DashboardNowColumnService();
    private final DashboardTodayColumnService dashboardTodayColumnService = new DashboardTodayColumnService();
    private final DashboardCurrentActionService dashboardCurrentActionService = new DashboardCurrentActionService();
    private final DashboardTimeGaugeService dashboardTimeGaugeService = new DashboardTimeGaugeService();
    private final DashboardNextColumnService dashboardNextColumnService = new DashboardNextColumnService();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            String action = request.getParameter("action");

            // Handle AJAX requests for today column modals
            if ("loadActionData".equals(action)) {
                handleLoadActionData(appReq);
                return;
            }
            if ("editAction".equals(action)) {
                handleEditAction(appReq);
                return;
            }
            if ("loadReprioritizeData".equals(action)) {
                handleLoadReprioritizeData(appReq);
                return;
            }
            if ("reprioritizeAction".equals(action)) {
                handleReprioritizeAction(appReq);
                return;
            }
            if ("loadRescheduleData".equals(action)) {
                handleLoadRescheduleData(appReq);
                return;
            }
            if ("rescheduleAction".equals(action)) {
                handleRescheduleAction(appReq);
                return;
            }
            if ("refreshHeaderGauges".equals(action)) {
                handleRefreshHeaderGauges(appReq);
                return;
            }
            if ("addCurrentActionNote".equals(action)) {
                handleAddCurrentActionNote(appReq);
                return;
            }

            dashboardCurrentActionService.handleSelectAction(appReq);
            dashboardCurrentActionService.handleCurrentActionWork(appReq);
            dashboardTodayColumnService.handleQuickCapture(appReq);
            dashboardCurrentActionService.ensureCurrentActionSelected(appReq);

            appReq.setTitle("Dashboard");
            DashboardNowColumnModel nowColumnModel = dashboardNowColumnService.buildModel(appReq);
            DashboardTodayColumnModel todayColumnModel = dashboardTodayColumnService.buildModel(appReq);
            TimeGaugeModel nowGaugeModel = dashboardTimeGaugeService.buildNowGauge(appReq);
            TimeGaugeModel todayGaugeModel = dashboardTimeGaugeService.buildTodayGauge(appReq);
            dashboardTimeGaugeService.updateTodayGaugePlanned(todayGaugeModel,
                    todayColumnModel.getTotals().getPlannedMinutes());
            DashboardNextColumnModel nextColumnModel = dashboardNextColumnService.buildModel(appReq,
                    dashboardTimeGaugeService);
            printHtmlHead(appReq);
            dashboardPageRenderer.render(appReq, nowColumnModel, todayColumnModel, nextColumnModel,
                    nowGaugeModel, todayGaugeModel);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
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

    private void handleLoadActionData(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        int actionNextId = Integer.parseInt(actionNextIdStr);

        ProjectActionNext action = (ProjectActionNext) appReq.getDataSession()
                .get(ProjectActionNext.class, actionNextId);

        if (action == null) {
            sendJsonResponse(appReq, false, "Action not found", null);
            return;
        }

        // Build response JSON
        String dateFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("nextActionDate", action.getNextActionDate() != null ? sdf.format(action.getNextActionDate()) : "");
        data.put("nextActionType", action.getNextActionType() != null ? action.getNextActionType() : "");
        data.put("projectName", action.getProject() != null && action.getProject().getProjectName() != null
                ? action.getProject().getProjectName()
                : "");
        Integer nextContactId = action.getNextContactId();
        data.put("nextContactId", nextContactId != null && nextContactId.intValue() > 0 ? nextContactId : "");
        data.put("nextDescription", action.getNextDescription() != null ? action.getNextDescription() : "");
        data.put("nextTimeEstimate", action.getNextTimeEstimate() != null ? action.getNextTimeEstimate() : 0);
        data.put("nextTargetDate", action.getNextTargetDate() != null ? sdf.format(action.getNextTargetDate()) : "");
        data.put("nextDeadlineDate",
                action.getNextDeadlineDate() != null ? sdf.format(action.getNextDeadlineDate()) : "");
        data.put("linkUrl", action.getLinkUrl() != null ? action.getLinkUrl() : "");
        data.put("nextNote", action.getNextNotes() != null ? action.getNextNotes() : "");

        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleEditAction(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        int actionNextId = Integer.parseInt(actionNextIdStr);

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();

        try {
            ProjectActionNext action = (ProjectActionNext) dataSession
                    .get(ProjectActionNext.class, actionNextId);

            if (action == null) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action not found", null);
                return;
            }

            // Update action fields from request
            String nextActionDate = appReq.getRequest().getParameter("nextActionDate");
            String nextActionType = appReq.getRequest().getParameter("nextActionType");
            String nextContactIdStr = appReq.getRequest().getParameter("nextContactId");
            String nextDescription = appReq.getRequest().getParameter("nextDescription");
            String nextTimeEstimateStr = appReq.getRequest().getParameter("nextTimeEstimate");
            String nextTargetDate = appReq.getRequest().getParameter("nextTargetDate");
            String nextDeadlineDate = appReq.getRequest().getParameter("nextDeadlineDate");
            String linkUrl = appReq.getRequest().getParameter("linkUrl");
            String nextNote = appReq.getRequest().getParameter("nextNote");

            // Parse and set dates
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

            if (nextActionDate != null && nextActionDate.length() > 0) {
                try {
                    action.setNextActionDate(sdf.parse(nextActionDate));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            if (nextActionType != null && nextActionType.length() > 0) {
                action.setNextActionType(nextActionType);
            }

            if (nextContactIdStr != null && nextContactIdStr.length() > 0) {
                try {
                    action.setNextContactId(Integer.parseInt(nextContactIdStr));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            if (nextDescription != null) {
                action.setNextDescription(nextDescription);
            }

            if (nextTimeEstimateStr != null && nextTimeEstimateStr.length() > 0) {
                try {
                    int mins = Integer.parseInt(nextTimeEstimateStr);
                    action.setNextTimeEstimate(mins);
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            if (nextTargetDate != null && nextTargetDate.length() > 0) {
                try {
                    action.setNextTargetDate(sdf.parse(nextTargetDate));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            if (nextDeadlineDate != null && nextDeadlineDate.length() > 0) {
                try {
                    action.setNextDeadlineDate(sdf.parse(nextDeadlineDate));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }

            if (linkUrl != null) {
                action.setLinkUrl(linkUrl);
            }

            if (nextNote != null) {
                action.setNextNotes(nextNote);
            }

            // Save the updated action
            dataSession.update(action);
            transaction.commit();

            sendJsonResponse(appReq, true, "Action saved successfully", null);
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
            sendJsonResponse(appReq, false, "Error saving action: " + e.getMessage(), null);
        }
    }

    private void sendJsonResponse(AppReq appReq, boolean success, String message, Map<String, Object> data)
            throws Exception {
        appReq.getResponse().setContentType("application/json; charset=UTF-8");
        PrintWriter out = appReq.getResponse().getWriter();

        // Build JSON manually
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
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                appendJsonValue(json, java.lang.reflect.Array.get(value, i));
            }
            json.append("]");
        } else {
            json.append("\"").append(escapeJson(value.toString())).append("\"");
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    private void handleLoadReprioritizeData(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        int actionNextId = Integer.parseInt(actionNextIdStr);

        Session dataSession = appReq.getDataSession();
        ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);

        if (action == null) {
            sendJsonResponse(appReq, false, "Action not found", null);
            return;
        }

        // Build the today model to get all actions in the same section as this one
        DashboardTodayColumnModel todayModel = dashboardTodayColumnService.buildModel(appReq);

        // Find which section this action belongs to
        String currentActionSection = null;
        List<Integer> sectionActionIds = new ArrayList<>();

        // Check in action groups
        for (DashboardTodayColumnModel.TodayActionGroupModel group : todayModel.getActionGroups()) {
            boolean foundInGroup = false;
            for (DashboardTodayColumnModel.TodayActionItemModel item : group.getItems()) {
                if (item.getActionNextId() == actionNextId) {
                    currentActionSection = group.getTitle();
                    foundInGroup = true;
                    break;
                }
                sectionActionIds.add(item.getActionNextId());
            }
            if (foundInGroup)
                break;
        }

        // If not found in action groups, check completed section
        if (currentActionSection == null) {
            for (DashboardTodayColumnModel.TodayActionItemModel item : todayModel.getCompletedToday()) {
                if (item.getActionNextId() == actionNextId) {
                    currentActionSection = "Completed";
                    break;
                }
                sectionActionIds.add(item.getActionNextId());
            }
        }

        // Load all actions in the section (excluding current one)
        List<Map<String, Object>> actionList = new ArrayList<>();
        for (Integer id : sectionActionIds) {
            if (id != actionNextId) {
                ProjectActionNext sectionAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class, id);
                if (sectionAction != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", sectionAction.getActionNextId());
                    item.put("description",
                            sectionAction.getNextDescription() != null ? sectionAction.getNextDescription() : "");
                    item.put("order", sectionAction.getCompletionOrder());
                    actionList.add(item);
                }
            }
        }

        // Sort by completion order
        actionList.sort((a, b) -> {
            int orderA = ((Number) a.get("order")).intValue();
            int orderB = ((Number) b.get("order")).intValue();
            return orderA - orderB;
        });

        // Build response
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("description", action.getNextDescription() != null ? action.getNextDescription() : "");
        data.put("actions", actionList);

        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleReprioritizeAction(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        int actionNextId = Integer.parseInt(actionNextIdStr);
        String moveType = appReq.getRequest().getParameter("moveType");

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();

        try {
            ProjectActionNext currentAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                    actionNextId);
            if (currentAction == null) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action not found", null);
                return;
            }

            // Build the today model to get all actions in the same section
            DashboardTodayColumnModel todayModel = dashboardTodayColumnService.buildModel(appReq);

            // Find which section this action belongs to and get all section actions
            List<ProjectActionNext> sectionActions = new ArrayList<>();
            int actionIndex = -1;

            for (DashboardTodayColumnModel.TodayActionGroupModel group : todayModel.getActionGroups()) {
                List<Integer> groupActionIds = new ArrayList<>();
                int currentIndex = -1;
                for (DashboardTodayColumnModel.TodayActionItemModel item : group.getItems()) {
                    groupActionIds.add(item.getActionNextId());
                    if (item.getActionNextId() == actionNextId) {
                        currentIndex = groupActionIds.size() - 1;
                    }
                }

                if (currentIndex >= 0) {
                    // This is the correct section
                    for (Integer id : groupActionIds) {
                        ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, id);
                        if (action != null) {
                            sectionActions.add(action);
                        }
                    }
                    actionIndex = currentIndex;
                    break;
                }
            }

            // Handle completed section if needed
            if (actionIndex < 0) {
                List<Integer> completedIds = new ArrayList<>();
                int currentIndex = -1;
                for (DashboardTodayColumnModel.TodayActionItemModel item : todayModel.getCompletedToday()) {
                    completedIds.add(item.getActionNextId());
                    if (item.getActionNextId() == actionNextId) {
                        currentIndex = completedIds.size() - 1;
                    }
                }

                if (currentIndex >= 0) {
                    for (Integer id : completedIds) {
                        ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, id);
                        if (action != null) {
                            sectionActions.add(action);
                        }
                    }
                    actionIndex = currentIndex;
                }
            }

            if (actionIndex < 0 || sectionActions.isEmpty()) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action section not found", null);
                return;
            }

            // Sort section actions by completion order
            sectionActions.sort((a, b) -> a.getCompletionOrder() - b.getCompletionOrder());

            // Find the action again after sorting (it may have moved)
            actionIndex = -1;
            for (int i = 0; i < sectionActions.size(); i++) {
                if (sectionActions.get(i).getActionNextId() == actionNextId) {
                    actionIndex = i;
                    break;
                }
            }

            if (actionIndex < 0) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action not found in section", null);
                return;
            }

            // Perform the move operation
            int targetIndex = -1;

            if ("first".equals(moveType)) {
                targetIndex = 0;
            } else if ("up".equals(moveType)) {
                targetIndex = actionIndex - 1;
            } else if ("down".equals(moveType)) {
                targetIndex = actionIndex + 1;
            } else if ("last".equals(moveType)) {
                targetIndex = sectionActions.size() - 1;
            } else if ("before".equals(moveType)) {
                String targetActionIdStr = appReq.getRequest().getParameter("targetActionId");
                int targetActionId = Integer.parseInt(targetActionIdStr);
                for (int i = 0; i < sectionActions.size(); i++) {
                    if (sectionActions.get(i).getActionNextId() == targetActionId) {
                        targetIndex = i;
                        break;
                    }
                }
            }

            // Validate target index
            if (targetIndex < 0 || targetIndex >= sectionActions.size() || targetIndex == actionIndex) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Invalid move operation", null);
                return;
            }

            // Perform the swap
            ProjectActionNext targetAction = sectionActions.get(targetIndex);
            int currentOrder = currentAction.getCompletionOrder();
            int targetOrder = targetAction.getCompletionOrder();

            currentAction.setCompletionOrder(targetOrder);
            currentAction.setNextChangeDate(new Date());
            targetAction.setCompletionOrder(currentOrder);
            targetAction.setNextChangeDate(new Date());

            dataSession.update(currentAction);
            dataSession.update(targetAction);
            transaction.commit();

            sendJsonResponse(appReq, true, "Action moved successfully", null);
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
            sendJsonResponse(appReq, false, "Error moving action: " + e.getMessage(), null);
        }
    }

    private void handleLoadRescheduleData(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        int actionNextId = Integer.parseInt(actionNextIdStr);

        ProjectActionNext action = (ProjectActionNext) appReq.getDataSession()
                .get(ProjectActionNext.class, actionNextId);

        if (action == null) {
            sendJsonResponse(appReq, false, "Action not found", null);
            return;
        }

        String dateFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("description", action.getNextDescription() != null ? action.getNextDescription() : "");
        data.put("nextActionDate", action.getNextActionDate() != null ? sdf.format(action.getNextActionDate()) : "");

        sendJsonResponse(appReq, true, "OK", data);
    }

    private void handleRescheduleAction(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        int actionNextId = Integer.parseInt(actionNextIdStr);
        String nextActionDateStr = appReq.getRequest().getParameter("nextActionDate");

        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();

        try {
            ProjectActionNext action = (ProjectActionNext) dataSession
                    .get(ProjectActionNext.class, actionNextId);

            if (action == null) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action not found", null);
                return;
            }

            // Parse and validate the new date
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            if (nextActionDateStr != null && nextActionDateStr.length() > 0) {
                try {
                    java.util.Date newDate = sdf.parse(nextActionDateStr);
                    // Validate that the new date is not in the past
                    java.util.Date today = new java.util.Date();
                    // Reset time components for comparison (midnight)
                    java.util.Calendar todayCalendar = java.util.Calendar.getInstance();
                    todayCalendar.setTime(today);
                    todayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    todayCalendar.set(java.util.Calendar.MINUTE, 0);
                    todayCalendar.set(java.util.Calendar.SECOND, 0);
                    todayCalendar.set(java.util.Calendar.MILLISECOND, 0);

                    java.util.Calendar newCalendar = java.util.Calendar.getInstance();
                    newCalendar.setTime(newDate);
                    newCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    newCalendar.set(java.util.Calendar.MINUTE, 0);
                    newCalendar.set(java.util.Calendar.SECOND, 0);
                    newCalendar.set(java.util.Calendar.MILLISECOND, 0);

                    if (newCalendar.before(todayCalendar)) {
                        transaction.rollback();
                        sendJsonResponse(appReq, false, "Cannot schedule action to a past date", null);
                        return;
                    }

                    action.setNextActionDate(newDate);
                    action.setNextChangeDate(new Date());

                } catch (java.text.ParseException e) {
                    transaction.rollback();
                    sendJsonResponse(appReq, false, "Invalid date format", null);
                    return;
                }
            }

            // Save the updated action
            dataSession.update(action);
            transaction.commit();

            sendJsonResponse(appReq, true, "Action rescheduled successfully", null);
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
            sendJsonResponse(appReq, false, "Error rescheduling action: " + e.getMessage(), null);
        }
    }

    private void handleRefreshHeaderGauges(AppReq appReq) throws Exception {
        Session dataSession = appReq.getDataSession();
        ProjectActionNext completingAction = appReq.getCompletingAction();

        // Keep the running billing entry synchronized with real elapsed time.
        if (completingAction != null) {
            ProjectActionNext persistedAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                    completingAction.getActionNextId());
            if (persistedAction != null) {
                appReq.setCompletingAction(persistedAction);
                if (appReq.getTimeTracker() != null && appReq.getTimeTracker().isRunningClock()) {
                    appReq.getTimeTracker().update(persistedAction, dataSession);
                }
            }
        }

        TimeGaugeModel nowGaugeModel = dashboardTimeGaugeService.buildNowGauge(appReq);
        DashboardTodayColumnModel todayColumnModel = dashboardTodayColumnService.buildModel(appReq);
        TimeGaugeModel todayGaugeModel = dashboardTimeGaugeService.buildTodayGauge(appReq);
        dashboardTimeGaugeService.updateTodayGaugePlanned(todayGaugeModel,
                todayColumnModel.getTotals().getPlannedMinutes());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nowGaugeHtml", renderGaugeHtml(nowGaugeModel));
        data.put("todayGaugeHtml", renderGaugeHtml(todayGaugeModel));
        data.put("todayCurrentTime", formatCurrentUserTime(appReq.getWebUser()));
        sendJsonResponse(appReq, true, "OK", data);
    }

    private String formatCurrentUserTime(WebUser webUser) {
        return webUser.getDateFormatService().formatPattern(new Date(), "hh:mm:ss aaa z", webUser.getTimeZone());
    }

    private String renderGaugeHtml(TimeGaugeModel model) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        new TimeGaugeRenderer().render(printWriter, model);
        printWriter.flush();
        return stringWriter.toString();
    }

    private void handleAddCurrentActionNote(AppReq appReq) throws Exception {
        String actionNextIdStr = appReq.getRequest().getParameter("actionNextId");
        String nextNote = appReq.getRequest().getParameter("nextNote");
        if (actionNextIdStr == null || nextNote == null || nextNote.trim().length() == 0) {
            sendJsonResponse(appReq, false, "Note is required", null);
            return;
        }

        int actionNextId = Integer.parseInt(actionNextIdStr);
        Session dataSession = appReq.getDataSession();
        Transaction transaction = dataSession.beginTransaction();
        try {
            ProjectActionNext action = (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
            if (action == null) {
                transaction.rollback();
                sendJsonResponse(appReq, false, "Action not found", null);
                return;
            }

            String noteToAdd = nextNote.trim();
            String existing = action.getNextNotes();
            String updatedNotes;
            if (existing != null && existing.trim().length() > 0) {
                updatedNotes = existing + "\n - " + noteToAdd;
            } else {
                updatedNotes = " - " + noteToAdd;
            }
            action.setNextNotes(updatedNotes);
            action.setNextChangeDate(new Date());

            dataSession.update(action);
            transaction.commit();
            sendJsonResponse(appReq, true, "Note added", null);
        } catch (Exception e) {
            transaction.rollback();
            sendJsonResponse(appReq, false, "Unable to add note", null);
        }
    }
}