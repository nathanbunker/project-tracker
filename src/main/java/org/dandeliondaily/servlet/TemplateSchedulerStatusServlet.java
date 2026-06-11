package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.planahead.service.SchedulerRunRecord;
import org.dandeliondaily.planahead.service.SchedulerStatusHolder;
import org.dandeliondaily.planahead.service.TemplateSchedulerListener;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

/**
 * Admin-only page showing the current operational status of the
 * TemplateSchedulerListener: last run time, instances generated, any errors,
 * and a rolling table of recent runs.
 *
 * Also provides a "Run Now" action so an admin can trigger an immediate
 * generation pass without restarting the application.
 */
public class TemplateSchedulerStatusServlet extends ClientServlet {

    private static final long serialVersionUID = 1L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }
            if (!appReq.isAdmin()) {
                forwardToHome(request, response);
                return;
            }

            // "Run Now" action — triggers an immediate scheduler pass
            if ("runNow".equals(request.getParameter("action"))) {
                TemplateSchedulerListener.triggerNow();
                response.sendRedirect("TemplateSchedulerStatusServlet?triggered=1");
                return;
            }

            boolean triggered = "1".equals(request.getParameter("triggered"));

            appReq.setTitle("Scheduler Status");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();

            out.println("<div class=\"main\">");
            out.println("<h1>Template Scheduler Status</h1>");

            if (triggered) {
                out.println("<p style='padding:8px 14px;background:#e8f5e9;border-left:4px solid #388e3c;"
                        + "color:#1b5e20;margin-bottom:12px;'>Run submitted. Refresh in a moment to see results.</p>");
            }

            SchedulerRunRecord current = SchedulerStatusHolder.getCurrentRun();
            SchedulerRunRecord last = SchedulerStatusHolder.getLastCompletedRun();
            List<SchedulerRunRecord> recentRuns = SchedulerStatusHolder.getRecentRuns();

            // ---- Status banner -----------------------------------------------
            if (current != null) {
                out.println("<p style='padding:8px 14px;background:#e3f2fd;border-left:4px solid #1976d2;"
                        + "color:#0d47a1;'>"
                        + "&#9654; Running now &mdash; started " + esc(current.formatStartTime())
                        + " (" + current.formatDuration() + " so far)</p>");
            } else if (recentRuns.isEmpty()) {
                out.println("<p style='padding:8px 14px;background:#fff8e1;border-left:4px solid #f9a825;"
                        + "color:#6d4c00;'>"
                        + "&#9888; No runs recorded yet. The scheduler fires on startup and then every"
                        + " hour at :15.</p>");
            } else {
                SchedulerRunRecord.Outcome outcome = last.getOutcome();
                if (outcome == SchedulerRunRecord.Outcome.OK
                        || outcome == SchedulerRunRecord.Outcome.NO_TEMPLATES) {
                    out.println("<p style='padding:8px 14px;background:#e8f5e9;border-left:4px solid #388e3c;"
                            + "color:#1b5e20;'>"
                            + "&#10003; Scheduler is healthy. Last run: "
                            + esc(last.formatStartTime()) + "</p>");
                } else {
                    out.println("<p style='padding:8px 14px;background:#fdecea;border-left:4px solid #c62828;"
                            + "color:#7f0000;'>"
                            + "&#9888; Last run had errors &mdash; " + esc(last.formatStartTime())
                            + ". See error details below.</p>");
                }
            }

            // ---- Last run card -----------------------------------------------
            if (last != null) {
                out.println("<h2>Last Completed Run</h2>");
                out.println("<table class='boxed'>");
                out.println("<tr class='boxed'><th class='boxed'>Started</th>"
                        + "<td class='boxed'>" + esc(last.formatStartTime()) + "</td></tr>");
                out.println("<tr class='boxed'><th class='boxed'>Duration</th>"
                        + "<td class='boxed'>" + esc(last.formatDuration()) + "</td></tr>");
                out.println("<tr class='boxed'><th class='boxed'>Users processed</th>"
                        + "<td class='boxed'>" + last.getPairsProcessed() + "</td></tr>");
                out.println("<tr class='boxed'><th class='boxed'>New instances created</th>"
                        + "<td class='boxed'>" + last.getInstancesGenerated() + "</td></tr>");
                out.println("<tr class='boxed'><th class='boxed'>Errors</th>"
                        + "<td class='boxed'>" + last.getPairsWithErrors() + "</td></tr>");
                out.println("<tr class='boxed'><th class='boxed'>Status</th>"
                        + "<td class='boxed'>" + formatOutcome(last.getOutcome()) + "</td></tr>");
                out.println("</table>");
            }

            // ---- Recent run history table -----------------------------------
            if (!recentRuns.isEmpty()) {
                out.println("<h2>Recent Runs</h2>");
                out.println("<p style='color:#64748b;font-size:0.9em;'>Showing last "
                        + recentRuns.size() + " completed run(s). History is in-memory and resets on app restart.</p>");
                out.println("<table class='boxed'>");
                out.println("<tr class='boxed'>"
                        + "<th class='boxed'>Time</th>"
                        + "<th class='boxed'>Duration</th>"
                        + "<th class='boxed'>Users</th>"
                        + "<th class='boxed'>Created</th>"
                        + "<th class='boxed'>Errors</th>"
                        + "<th class='boxed'>Status</th>"
                        + "</tr>");
                SimpleDateFormat timeFmt = new SimpleDateFormat("MM-dd HH:mm");
                for (SchedulerRunRecord run : recentRuns) {
                    boolean hasError = run.getPairsWithErrors() > 0
                            || run.getOutcome() == SchedulerRunRecord.Outcome.FATAL;
                    String rowBg = hasError ? "background:#fff5f5;" : "";
                    out.println("<tr class='boxed' style='" + rowBg + "'>"
                            + "<td class='boxed'>" + timeFmt.format(run.getStartTime()) + "</td>"
                            + "<td class='boxed'>" + run.formatDuration() + "</td>"
                            + "<td class='boxed'>" + run.getPairsProcessed() + "</td>"
                            + "<td class='boxed'>" + run.getInstancesGenerated() + "</td>"
                            + "<td class='boxed'>" + run.getPairsWithErrors() + "</td>"
                            + "<td class='boxed'>" + formatOutcome(run.getOutcome()) + "</td>"
                            + "</tr>");
                }
                out.println("</table>");
            }

            // ---- Most recent error detail -----------------------------------
            SchedulerRunRecord runWithError = null;
            for (SchedulerRunRecord run : recentRuns) {
                if (run.getLastErrorMessage() != null) {
                    runWithError = run;
                    break;
                }
            }
            if (runWithError != null) {
                out.println("<h2>Most Recent Error</h2>");
                out.println("<p style='color:#7f0000;font-weight:bold;'>"
                        + esc(runWithError.getLastErrorMessage()) + "</p>");
                out.println("<p style='color:#64748b;font-size:0.85em;'>From run at "
                        + esc(runWithError.formatStartTime()) + "</p>");
                if (runWithError.getLastErrorStackTrace() != null) {
                    out.println("<pre style='background:#f8f8f8;border:1px solid #ddd;padding:12px;"
                            + "overflow:auto;font-size:11px;max-height:400px;white-space:pre-wrap;'>"
                            + esc(runWithError.getLastErrorStackTrace()) + "</pre>");
                }
            }

            // ---- Run Now button ---------------------------------------------
            out.println("<h2>Actions</h2>");
            out.println("<form method='POST' action='TemplateSchedulerStatusServlet'>");
            out.println("<input type='hidden' name='action' value='runNow'/>");
            out.println("<input type='submit' value='Run Now'"
                    + " title='Submit an immediate generation pass without waiting for the next scheduled run'"
                    + " onclick=\"return confirm('Submit a template generation run now?');\"/>");
            out.println("</form>");
            out.println("<p style='color:#64748b;font-size:0.85em;margin-top:4px;'>"
                    + "Submits an immediate run. Refresh the page after a few seconds to see results.</p>");

            // ---- Navigation -------------------------------------------------
            out.println("<p style='margin-top:24px;'>"
                    + "<a href='TemplateManagementServlet'>Template Management</a>"
                    + " &nbsp;|&nbsp; "
                    + "<a href='AdminSettingsServlet'>Admin Settings</a>"
                    + " &nbsp;|&nbsp; "
                    + "<a href='HomeServlet'>Home</a>"
                    + "</p>");

            out.println("</div>");
            printHtmlFoot(appReq);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private String formatOutcome(SchedulerRunRecord.Outcome outcome) {
        switch (outcome) {
            case OK:
                return "<span style='color:#2e7d32;font-weight:bold;'>OK</span>";
            case NO_TEMPLATES:
                return "<span style='color:#1565c0;'>No templates</span>";
            case PARTIAL_ERRORS:
                return "<span style='color:#e65100;font-weight:bold;'>Partial errors</span>";
            case FATAL:
                return "<span style='color:#b71c1c;font-weight:bold;'>FATAL</span>";
            case RUNNING:
                return "<span style='color:#1565c0;'>Running&#8230;</span>";
            default:
                return esc(outcome.name());
        }
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
