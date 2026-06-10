package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;

public class AdminSettingsServlet extends ClientServlet {

    private static final String ACTION_SAVE_SMTP = "Save SMTP Settings";

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SMTP_HOST = "smtpHost";
    private static final String PARAM_SMTP_PORT = "smtpPort";
    private static final String PARAM_SMTP_USERNAME = "smtpUsername";
    private static final String PARAM_SMTP_PASSWORD = "smtpPassword";
    private static final String PARAM_SMTP_USE_SMTPS = "useSmtps";
    private static final String PARAM_EMAIL_ENABLED = "emailEnabled";
    private static final String PARAM_EMAIL_REPLY = "emailReply";
    private static final String PARAM_EMAIL_DEBUG = "emailDebug";

    private static final int MAX_HOST_LEN = 254;
    private static final int MAX_USERNAME_LEN = 254;
    private static final int MAX_PASSWORD_LEN = 254;
    private static final int MAX_REPLY_LEN = 254;

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

            Session dataSession = appReq.getDataSession();
            String action = request.getParameter(PARAM_ACTION);
            if (ACTION_SAVE_SMTP.equals(action)) {
                handleSaveSmtpSettings(appReq);
            }

            SmtpSettingsForm smtpSettings = loadSmtpSettingsForm(dataSession);

            appReq.setTitle("Admin Settings");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            out.println("<div class=\"main\">");
            out.println("<h1>Admin Settings</h1>");
            out.println(
                    "<p>Configure system-wide email delivery settings used for notifications and account flows.</p>");

            out.println("<form method=\"POST\" action=\"AdminSettingsServlet\">");
            out.println("<table class=\"boxed\">\n"
                    + "  <tr class=\"boxed\"><th class=\"title\" colspan=\"2\">SMTP Settings</th></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">Email Enabled</th><td class=\"boxed\"><input type=\"checkbox\" name=\""
                    + PARAM_EMAIL_ENABLED + "\" value=\"Y\"" + (smtpSettings.emailEnabled ? " checked" : "")
                    + "/> <span class=\"small\">If not checked, app email is disabled.</span></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">SMTP Host</th><td class=\"boxed\"><input type=\"text\" name=\""
                    + PARAM_SMTP_HOST + "\" size=\"50\" maxlength=\"" + MAX_HOST_LEN + "\" value=\""
                    + h(smtpSettings.smtpHost) + "\"/></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">SMTP Port</th><td class=\"boxed\"><input type=\"text\" name=\""
                    + PARAM_SMTP_PORT + "\" size=\"8\" maxlength=\"5\" value=\"" + h(smtpSettings.smtpPort)
                    + "\"/> <span class=\"small\">Use 587 for STARTTLS or 465 for SMTPS.</span></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">SMTP Username</th><td class=\"boxed\"><input type=\"text\" name=\""
                    + PARAM_SMTP_USERNAME + "\" size=\"50\" maxlength=\"" + MAX_USERNAME_LEN + "\" value=\""
                    + h(smtpSettings.smtpUsername) + "\"/></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">SMTP Password</th><td class=\"boxed\"><input type=\"password\" name=\""
                    + PARAM_SMTP_PASSWORD + "\" size=\"50\" maxlength=\"" + MAX_PASSWORD_LEN
                    + "\" value=\"\" autocomplete=\"new-password\"/> <span class=\"small\">"
                    + (smtpSettings.passwordSet ? "Password is currently set. Leave blank to keep existing password."
                            : "No password is currently set.")
                    + "</span></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">Use SMTPS</th><td class=\"boxed\"><input type=\"checkbox\" name=\""
                    + PARAM_SMTP_USE_SMTPS + "\" value=\"Y\"" + (smtpSettings.useSmtps ? " checked" : "")
                    + "/> <span class=\"small\">Unchecked uses SMTP + STARTTLS.</span></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">Reply Address</th><td class=\"boxed\"><input type=\"text\" name=\""
                    + PARAM_EMAIL_REPLY + "\" size=\"50\" maxlength=\"" + MAX_REPLY_LEN + "\" value=\""
                    + h(smtpSettings.replyAddress) + "\"/> <span class=\"small\">Optional.</span></td></tr>\n"
                    + "  <tr class=\"boxed\"><th class=\"boxed\">Email Debug Logging</th><td class=\"boxed\"><input type=\"checkbox\" name=\""
                    + PARAM_EMAIL_DEBUG + "\" value=\"Y\"" + (smtpSettings.emailDebug ? " checked" : "")
                    + "/> <span class=\"small\">Enables JavaMail session debug output.</span></td></tr>\n"
                    + "  <tr class=\"boxed\"><td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
                    + PARAM_ACTION + "\" value=\"" + ACTION_SAVE_SMTP + "\"/></td></tr>\n"
                    + "</table>");
            out.println("</form>");

            out.println("<p class=\"small\">Settings are stored at application scope and apply to all users.</p>");
            out.println("<p><a href=\"DandelionDashboardServlet\">Open Dashboard</a></p>");
            out.println("<p><a href=\"SettingsServlet\">Back to Settings</a></p>");
            out.println("</div>");
            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    private void handleSaveSmtpSettings(AppReq appReq) {
        HttpServletRequest request = appReq.getRequest();
        Session dataSession = appReq.getDataSession();

        String smtpHost = clip(trimToEmpty(request.getParameter(PARAM_SMTP_HOST)), MAX_HOST_LEN);
        String smtpPortValue = trimToEmpty(request.getParameter(PARAM_SMTP_PORT));
        String smtpUsername = clip(trimToEmpty(request.getParameter(PARAM_SMTP_USERNAME)), MAX_USERNAME_LEN);
        String smtpPassword = clip(trimToEmpty(request.getParameter(PARAM_SMTP_PASSWORD)), MAX_PASSWORD_LEN);
        String replyAddress = clip(trimToEmpty(request.getParameter(PARAM_EMAIL_REPLY)), MAX_REPLY_LEN);

        boolean emailEnabled = checkboxChecked(request, PARAM_EMAIL_ENABLED);
        boolean useSmtps = checkboxChecked(request, PARAM_SMTP_USE_SMTPS);
        boolean emailDebug = checkboxChecked(request, PARAM_EMAIL_DEBUG);

        String existingPassword = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession);
        if (smtpPassword.length() == 0) {
            smtpPassword = existingPassword;
        }

        if (smtpPortValue.length() == 0) {
            smtpPortValue = useSmtps ? "465" : "587";
        }

        int smtpPort = 0;
        try {
            smtpPort = Integer.parseInt(smtpPortValue);
        } catch (NumberFormatException nfe) {
            appReq.setMessageProblem("SMTP port must be a number between 1 and 65535.");
            return;
        }
        if (smtpPort < 1 || smtpPort > 65535) {
            appReq.setMessageProblem("SMTP port must be between 1 and 65535.");
            return;
        }

        if (emailEnabled) {
            if (smtpHost.length() == 0) {
                appReq.setMessageProblem("SMTP host is required when email is enabled.");
                return;
            }
            if (smtpUsername.length() == 0) {
                appReq.setMessageProblem("SMTP username is required when email is enabled.");
                return;
            }
            if (smtpPassword.length() == 0) {
                appReq.setMessageProblem("SMTP password is required when email is enabled.");
                return;
            }
        }

        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS,
                smtpHost, dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT,
                String.valueOf(smtpPort), dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME,
                smtpUsername, dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD,
                smtpPassword, dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS,
                useSmtps ? "Y" : "N", dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_ENABLE,
                emailEnabled ? "Y" : "N", dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY,
                replyAddress, dataSession);
        TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_DEBUG,
                emailDebug ? "Y" : "N", dataSession);

        appReq.setMessageConfirmation("SMTP settings saved.");
    }

    private SmtpSettingsForm loadSmtpSettingsForm(Session dataSession) {
        SmtpSettingsForm form = new SmtpSettingsForm();
        form.smtpHost = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS, dataSession);
        form.smtpUsername = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, dataSession);
        form.replyAddress = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY, dataSession);
        form.useSmtps = TrackerKeysManager.getApplicationKeyValueBoolean(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, false, dataSession);
        form.emailEnabled = TrackerKeysManager.getApplicationKeyValueBoolean(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_ENABLE, false, dataSession);
        form.emailDebug = TrackerKeysManager.getApplicationKeyValueBoolean(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_DEBUG, false, dataSession);

        String configuredPort = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, dataSession);
        if (configuredPort == null || configuredPort.trim().equals("")) {
            form.smtpPort = form.useSmtps ? "465" : "587";
        } else {
            form.smtpPort = configuredPort.trim();
        }

        String existingPassword = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession);
        form.passwordSet = existingPassword != null && !existingPassword.trim().equals("");
        return form;
    }

    private boolean checkboxChecked(HttpServletRequest request, String paramName) {
        return request.getParameter(paramName) != null;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String clip(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private String h(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static class SmtpSettingsForm {
        private String smtpHost = "";
        private String smtpPort = "";
        private String smtpUsername = "";
        private String replyAddress = "";
        private boolean useSmtps = false;
        private boolean emailEnabled = false;
        private boolean emailDebug = false;
        private boolean passwordSet = false;
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
