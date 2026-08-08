package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsBoard;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsRenderer;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsService;
import org.openimmunizationsoftware.pt.billing.BillingProjectAssignmentsService.MoveResult;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BillingProjectAssignmentsServlet extends ClientServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BillingProjectAssignmentsService service = new BillingProjectAssignmentsService();
    private final BillingProjectAssignmentsRenderer renderer = new BillingProjectAssignmentsRenderer();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        boolean moveAction = "moveProject".equals(request.getParameter("action"));
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }
            if (!appReq.getWebUser().isUserTypeAdmin()) {
                if (moveAction) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    sendJson(appReq, false, "Administrator access is required.", null);
                } else {
                    renderAccessDenied(appReq);
                }
                return;
            }
            if (moveAction) {
                handleMove(appReq);
                return;
            }

            BillingProjectAssignmentsBoard board = service.buildBoard(appReq);
            appReq.setTitle("Billing Project Assignments");
            printHtmlHead(appReq);
            printDandelionLocation(appReq.getOut(), "Billing / Project Assignments");
            printBillingAdminNav(appReq.getOut(), "Project Assignments");
            renderer.render(appReq.getOut(), board);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
            if (moveAction && !response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                sendJson(appReq, false, message(e), null);
            } else if (!response.isCommitted()) {
                appReq.setTitle("Billing Project Assignments");
                printHtmlHead(appReq);
                appReq.getOut().println("<p class=\"fail\">Unable to load billing project assignments.</p>");
                printHtmlFoot(appReq);
            }
        } finally {
            appReq.close();
        }
    }

    private void handleMove(AppReq appReq) throws IOException {
        if (!"POST".equalsIgnoreCase(appReq.getRequest().getMethod())) {
            appReq.getResponse().setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            sendJson(appReq, false, "Project moves require POST.", null);
            return;
        }
        Integer projectId = parseInteger(appReq.getRequest().getParameter("projectId"));
        Integer updateEvery = parseInteger(appReq.getRequest().getParameter("updateEvery"));
        String billCode = trimToEmpty(appReq.getRequest().getParameter("billCode"));
        if (projectId == null || projectId.intValue() <= 0) {
            sendInvalidRequest(appReq, "Project is required.");
            return;
        }
        if (billCode.length() == 0) {
            sendInvalidRequest(appReq, "Bill code is required.");
            return;
        }
        if (updateEvery == null || updateEvery.intValue() < 0) {
            sendInvalidRequest(appReq, "Update cadence is invalid.");
            return;
        }

        MoveResult result = service.moveProject(appReq, projectId.intValue(), billCode, updateEvery.intValue());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("projectId", projectId);
        data.put("billCode", billCode);
        data.put("updateEvery", updateEvery);
        data.put("budgetCleared", Boolean.valueOf(result.isBudgetCleared()));
        sendJson(appReq, true, result.isBudgetCleared()
                ? "Project moved. Contract budget cleared."
                : "Project moved.", data);
    }

    private void renderAccessDenied(AppReq appReq) {
        appReq.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        appReq.setTitle("Billing Project Assignments");
        printHtmlHead(appReq);
        printDandelionLocation(appReq.getOut(), "Billing / Project Assignments");
        appReq.getOut().println("<p class=\"fail\">Administrator access is required.</p>");
        printHtmlFoot(appReq);
    }

    private void sendInvalidRequest(AppReq appReq, String message) throws IOException {
        appReq.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
        sendJson(appReq, false, message, null);
    }

    private void sendJson(AppReq appReq, boolean success, String message, Map<String, Object> data)
            throws IOException {
        appReq.getResponse().setContentType("application/json;charset=UTF-8");
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("success", Boolean.valueOf(success));
        payload.put("message", message);
        if (data != null) {
            payload.put("data", data);
        }
        OBJECT_MAPPER.writeValue(appReq.getOut(), payload);
    }

    static Integer parseInteger(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String message(Exception e) {
        return e.getMessage() == null || e.getMessage().trim().length() == 0
                ? "Unable to move project."
                : e.getMessage();
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