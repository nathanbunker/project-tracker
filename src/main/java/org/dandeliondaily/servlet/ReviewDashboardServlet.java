package org.dandeliondaily.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;
import org.dandeliondaily.timereview.model.TimeReviewDayModel;
import org.dandeliondaily.timereview.render.TimeReviewRenderer;
import org.dandeliondaily.timereview.service.TimeReviewService;

public class ReviewDashboardServlet extends ClientServlet {

    private static final long serialVersionUID = 7994954314099883550L;

    private static final String PARAM_REVIEW_DATE = "reviewDate";
    private static final String PARAM_SCOPE = "scope";

    private final TimeReviewService timeReviewService = new TimeReviewService();
    private final TimeReviewRenderer renderer = new TimeReviewRenderer();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            TimeTracker timeTracker = appReq.getTimeTracker();

            appReq.setTitle("Review");

            String quickScope = normalizeScope(request.getParameter(PARAM_SCOPE));
            String selectedDateParam = request.getParameter(PARAM_REVIEW_DATE);
            Date requestedDate = timeReviewService.parseIsoDay(webUser, selectedDateParam);
            String action = appReq.getAction();

            List<Date> trackedDays = timeReviewService.listTrackedDays(webUser, dataSession);
            Date selectedDay = timeReviewService.resolveSelectedDay(webUser, requestedDate, trackedDays);

            if (selectedDateParam == null || selectedDateParam.trim().length() == 0) {
                selectedDay = resolveSelectedDayFromScope(webUser, selectedDay, trackedDays, quickScope);
            }

            Integer lockedBillEntryId = timeTracker == null ? null : timeTracker.getBillEntryId();

            String pageMessage = null;
            if ("SaveTime".equals(action)) {
                pageMessage = handleSaveTime(request, webUser, dataSession, selectedDay, lockedBillEntryId);
            }

            TimeReviewDayModel dayModel = timeReviewService.buildDayModel(webUser, dataSession, selectedDay,
                    lockedBillEntryId);
            Map<String, List<Date>> groupedDays = timeReviewService.groupTrackedDaysByMonth(webUser, trackedDays);
            TimeReviewRenderer.EditFormModel editForm = buildEditForm(request, dayModel, webUser, dataSession);

            printHtmlHead(appReq);
            renderer.renderPage(appReq.getOut(), webUser, dayModel, trackedDays, groupedDays, quickScope, editForm,
                    dayModel.getSelectedDateIso(), pageMessage);
            printHtmlFoot(appReq);

            if (timeTracker != null && webUser.isSameDay(selectedDay, webUser.getToday())) {
                timeTracker.init(webUser, dataSession);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private String handleSaveTime(HttpServletRequest request, WebUser webUser, Session dataSession, Date selectedDay,
            Integer lockedBillEntryId) {
        int billId = parseInt(request.getParameter("billId"), 0);
        if (billId <= 0) {
            return "Bill entry was not selected.";
        }
        String startTime = request.getParameter("startTime");
        String endTime = request.getParameter("endTime");

        String error = timeReviewService.updateEntryTime(webUser, dataSession, billId, startTime, endTime, selectedDay,
                lockedBillEntryId);
        if (error == null) {
            return "Time entry saved.";
        }
        return error;
    }

    private TimeReviewRenderer.EditFormModel buildEditForm(HttpServletRequest request, TimeReviewDayModel dayModel,
            WebUser webUser, Session dataSession) {
        int editBillId = parseInt(request.getParameter("editBillId"), 0);
        if (editBillId <= 0) {
            return null;
        }

        BillEntry billEntry = (BillEntry) dataSession.get(BillEntry.class, editBillId);
        if (billEntry == null || billEntry.getWebUser() == null
                || billEntry.getWebUser().getWebUserId() != webUser.getWebUserId()) {
            return null;
        }

        if (dayModel.getLockedBillEntryId() != null && dayModel.getLockedBillEntryId().intValue() == editBillId) {
            return null;
        }

        TimeReviewRenderer.EditFormModel formModel = new TimeReviewRenderer.EditFormModel();
        formModel.setBillId(editBillId);
        SimpleDateFormat sdf = webUser.getDateFormat(webUser.getTimeDisplayPattern());
        formModel.setStartTime(sdf.format(billEntry.getStartTime()));
        formModel.setEndTime(sdf.format(billEntry.getEndTime()));
        return formModel;
    }

    private Date resolveSelectedDayFromScope(WebUser webUser, Date defaultDay, List<Date> trackedDays, String scope) {
        if ("today".equals(scope)) {
            return defaultDay;
        }
        if ("week".equals(scope)) {
            Date weekStart = getWeekStart(webUser, defaultDay);
            Date candidate = null;
            for (Date trackedDay : trackedDays) {
                if (!trackedDay.before(weekStart) && !trackedDay.after(defaultDay)) {
                    if (candidate == null || trackedDay.after(candidate)) {
                        candidate = trackedDay;
                    }
                }
            }
            return candidate != null ? candidate : defaultDay;
        }
        if ("month".equals(scope)) {
            Calendar selectedMonth = webUser.getCalendar(defaultDay);
            Date candidate = null;
            for (Date trackedDay : trackedDays) {
                Calendar trackedCal = webUser.getCalendar(trackedDay);
                if (selectedMonth.get(Calendar.YEAR) == trackedCal.get(Calendar.YEAR)
                        && selectedMonth.get(Calendar.MONTH) == trackedCal.get(Calendar.MONTH)) {
                    if (candidate == null || trackedDay.after(candidate)) {
                        candidate = trackedDay;
                    }
                }
            }
            return candidate != null ? candidate : defaultDay;
        }
        return defaultDay;
    }

    private Date getWeekStart(WebUser webUser, Date selectedDay) {
        Calendar cal = webUser.getCalendar(selectedDay);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.trim().length() == 0) {
            return "today";
        }
        String normalized = scope.trim().toLowerCase();
        if ("today".equals(normalized) || "week".equals(normalized) || "month".equals(normalized)) {
            return normalized;
        }
        return "today";
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
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
}
