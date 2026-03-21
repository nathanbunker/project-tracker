package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.WebUser;

public class AdminSettingsServlet extends ClientServlet {

    private static final String ACTION_SAVE_ADMIN = "Save Admin";
    private static final String ACTION_SAVE_CATEGORIES = "Save Categories";
    private static final String ACTION_SAVE_SYSTEM_WIDE_MESSAGE = "Save System Wide Message";

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_CLIENT_ACRONYM = "clientAcronym";
    private static final String PARAM_CLIENT_ACRONYM_PREFIX = "clientAcronym_";
    private static final String PARAM_CLIENT_NAME = "clientName";
    private static final String PARAM_CLIENT_NAME_PREFIX = "clientName_";
    private static final String PARAM_EMAIL_ENABLE = "emailEnable";
    private static final String PARAM_EMAIL_DEBUG = "emailDebug";
    private static final String PARAM_EMAIL_REPLY = "emailReply";
    private static final String PARAM_SEND_TEST_EMAIL = "sendTestEmail";
    private static final String PARAM_TEST_EMAIL_TO = "testEmailTo";
    private static final String PARAM_EMAIL_SMTPS_PORT = "emailSmtpsPort";
    private static final String PARAM_EMAIL_SMTPS_USERNAME = "emailSmtpsUsername";
    private static final String PARAM_EXTERNAL_URL = "externalUrl";
    private static final String PARAM_REPORT_DAILY_TIME = "reportDailyTime";
    private static final String PARAM_SMTP_ADDRESS = "smtpAddress";
    private static final String PARAM_SMTPS_PASSWORD = "smtpsPassword";
    private static final String PARAM_SORT_ORDER = "sortOrder";
    private static final String PARAM_SORT_ORDER_PREFIX = "sortOrder_";
    private static final String PARAM_SYSTEM_WIDE_MESSAGE = "systemWideMessage";
    private static final String PARAM_USE_SMTPS = "useSmtps";
    private static final String PARAM_VISIBLE = "visible";
    private static final String PARAM_VISIBLE_PREFIX = "visible_";
    private static final String PARAM_CATEGORY_CODE = "categoryCode";

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
            PrintWriter out = appReq.getOut();

            appReq.setTitle("Settings");

            if (!webUser.isUserTypeAdmin()) {
                printHtmlHead(appReq);
                out.println("<table class=\"boxed\">");
                out.println("  <tr class=\"boxed\">");
                out.println("    <th class=\"title\" colspan=\"2\">Admin Settings</th>");
                out.println("  </tr>");
                out.println("  <tr class=\"boxed\">");
                out.println(
                        "    <td class=\"boxed\">Only administrators can access this page. <a href=\"SettingsServlet\">Return to Settings</a></td>");
                out.println("  </tr>");
                out.println("</table>");
                printHtmlFoot(appReq);
                return;
            }

            String action = appReq.getAction();
            if (action != null) {
                if (action.equals(ACTION_SAVE_ADMIN)) {
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME,
                            request.getParameter(PARAM_REPORT_DAILY_TIME), dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL,
                            request.getParameter(PARAM_EXTERNAL_URL), dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS,
                            request.getParameter(PARAM_SMTP_ADDRESS), dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_ENABLE,
                            request.getParameter(PARAM_EMAIL_ENABLE) != null ? "Y" : "N", dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_DEBUG,
                            request.getParameter(PARAM_EMAIL_DEBUG) != null ? "Y" : "N", dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS,
                            request.getParameter(PARAM_USE_SMTPS) != null ? "Y" : "N", dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME,
                            request.getParameter(PARAM_EMAIL_SMTPS_USERNAME), dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD,
                            request.getParameter(PARAM_SMTPS_PASSWORD), dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT,
                            request.getParameter(PARAM_EMAIL_SMTPS_PORT), dataSession);
                    TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY,
                            request.getParameter(PARAM_EMAIL_REPLY), dataSession);

                    if (request.getParameter(PARAM_SEND_TEST_EMAIL) != null) {
                        String testEmailTo = trim(request.getParameter(PARAM_TEST_EMAIL_TO), 254);
                        if (testEmailTo.equals("")) {
                            testEmailTo = trim(request.getParameter(PARAM_EMAIL_REPLY), 254);
                        }
                        if (testEmailTo.equals("")) {
                            testEmailTo = trim(request.getParameter(PARAM_EMAIL_SMTPS_USERNAME), 254);
                        }
                        if (testEmailTo.equals("") && webUser.getEmailAddress() != null) {
                            testEmailTo = webUser.getEmailAddress();
                        }
                        if (testEmailTo.equals("")) {
                            appReq.setMessageProblem(
                                    "Unable to send test email: no recipient address provided. ");
                        } else {
                            try {
                                MailManager mailManager = new MailManager(dataSession);
                                StringBuilder body = new StringBuilder();
                                body.append("<html><body>");
                                body.append("<p>This is a test email from Project Tracker settings.</p>");
                                body.append("<p>Time: ").append(new Date()).append("</p>");
                                body.append("</body></html>");
                                mailManager.sendEmail("Project Tracker Test Email", body.toString(), testEmailTo);
                                appReq.setMessageConfirmation("Test email sent to " + testEmailTo + ".");
                            } catch (Exception e) {
                                appReq.setMessageProblem("Unable to send test email: " + e.getMessage());
                            }
                        }
                    }
                } else if (action.equals(ACTION_SAVE_SYSTEM_WIDE_MESSAGE)) {
                    setSystemWideMessage(request.getParameter(PARAM_SYSTEM_WIDE_MESSAGE));
                } else if (action.equals(ACTION_SAVE_CATEGORIES)) {
                    Query query = dataSession.createQuery(
                            "from ProjectCategory where provider = :provider order by sortOrder, clientName");
                    query.setParameter("provider", webUser.getProvider());
                    @SuppressWarnings("unchecked")
                    List<ProjectCategory> projectCategoryList = query.list();
                    String categoryCode = request.getParameter(PARAM_CATEGORY_CODE);
                    if (categoryCode.length() > 15) {
                        appReq.setMessageProblem(
                                "Category code is too long (>15), so new category not created. ");
                        categoryCode = "";
                    }
                    boolean categoryCodeIsUnique = true;
                    for (ProjectCategory projectCategory : projectCategoryList) {
                        String c = projectCategory.getCategoryCode();
                        if (c.equalsIgnoreCase(categoryCode)) {
                            categoryCodeIsUnique = false;
                        }
                        String clientName = request.getParameter(PARAM_CLIENT_NAME_PREFIX + c);
                        String sortOrder = request.getParameter(PARAM_SORT_ORDER_PREFIX + c);
                        String clientAcronym = request.getParameter(PARAM_CLIENT_ACRONYM_PREFIX + c);
                        String visible = request.getParameter(PARAM_VISIBLE_PREFIX + c);
                        if (!clientName.equals("")) {
                            projectCategory.setClientName(clientName);
                        }
                        if (!sortOrder.equals("")) {
                            try {
                                projectCategory.setSortOrder(Integer.parseInt(sortOrder));
                            } catch (NumberFormatException nfe) {
                                // do nothing
                            }
                        }
                        if (!clientAcronym.equals("")) {
                            projectCategory.setClientAcronym(clientAcronym);
                        }
                        projectCategory.setVisible(visible == null ? "N" : "Y");
                        Transaction trans = dataSession.beginTransaction();
                        dataSession.update(projectCategory);
                        trans.commit();
                    }
                    if (!categoryCode.equals("") && categoryCodeIsUnique) {
                        String clientName = request.getParameter(PARAM_CLIENT_NAME);
                        if (clientName.length() > 150) {
                            appReq.setMessageProblem("Client name is too long (>150), truncating. ");
                            clientName = clientName.substring(0, 150);
                        }
                        String sortOrder = request.getParameter(PARAM_SORT_ORDER);
                        String clientAcronym = request.getParameter(PARAM_CLIENT_ACRONYM);
                        String visible = request.getParameter(PARAM_VISIBLE);
                        if (!clientName.equals("")) {
                            ProjectCategory projectCategory = new ProjectCategory();
                            projectCategory.setCategoryCode(categoryCode);
                            projectCategory.setProvider(webUser.getProvider());
                            projectCategory.setClientName(clientName);
                            if (!sortOrder.equals("")) {
                                try {
                                    projectCategory.setSortOrder(Integer.parseInt(sortOrder));
                                } catch (NumberFormatException nfe) {
                                    // do nothing
                                }
                            }
                            if (!clientAcronym.equals("")) {
                                if (clientAcronym.length() > 15) {
                                    appReq.setMessageProblem("Client acronym is too long (>15), not setting. ");
                                } else {
                                    projectCategory.setClientAcronym(clientAcronym);
                                }
                            }
                            projectCategory.setVisible(visible == null ? "N" : "Y");
                            Transaction trans = dataSession.beginTransaction();
                            dataSession.save(projectCategory);
                            trans.commit();
                        } else {
                            appReq.setMessageProblem("Category name was not set, so new category not created. ");
                        }
                    } else if (!categoryCode.equals("")) {
                        appReq.setMessageProblem("Category code was not unique, so new category not created. ");
                    }
                }
            }

            printHtmlHead(appReq);

            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th class=\"title\" colspan=\"2\">Admin Settings</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println(
                    "    <td class=\"boxed\">Manage system-wide and administrator-only options here. <a href=\"SettingsServlet\">Return to Settings</a></td>");
            out.println("  </tr>");
            out.println("</table>");

            out.println("<br/>");
            out.println("<form action=\"AdminSettingsServlet\" method=\"POST\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th class=\"title\" colspan=\"5\">Categories</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th>Unique Code</th>");
            out.println("     <th>Category</th>");
            out.println("     <th>Sort Order</th>");
            out.println("     <th>Client Acronym</th>");
            out.println("     <th>Visible</th>");
            out.println("  </tr>");
            Query query = dataSession.createQuery(
                    "from ProjectCategory where provider = :provider order by sortOrder, clientName");
            query.setParameter("provider", webUser.getProvider());
            @SuppressWarnings("unchecked")
            List<ProjectCategory> projectCategoryList = query.list();
            for (ProjectCategory projectCategory : projectCategoryList) {
                String c = projectCategory.getCategoryCode();
                out.println("  <tr class=\"boxed\">");
                out.println("     <td>" + projectCategory.getCategoryCode() + "</td>");
                out.println("     <td><input type=\"text\" size=\"30\" name=\""
                        + PARAM_CLIENT_NAME_PREFIX + c + "\" value=\"" + projectCategory.getClientName()
                        + "\"></td>");
                out.println("     <td><input type=\"text\" size=\"3\" name=\""
                        + PARAM_SORT_ORDER_PREFIX + c + "\" value=\"" + n(projectCategory.getSortOrder())
                        + "\"></td>");
                out.println("     <td><input type=\"text\" size=\"7\" name=\""
                        + PARAM_CLIENT_ACRONYM_PREFIX + c + "\" value=\""
                        + n(projectCategory.getClientAcronym()) + "\"></td>");
                out.println("     <td><input type=\"checkbox\" name=\"" + PARAM_VISIBLE_PREFIX + c
                        + "\""
                        + (projectCategory.getVisible() != null
                                && projectCategory.getVisible().equals("Y") ? " checked=\"true\"" : "")
                        + "></td>");
                out.println("  </tr>");
            }
            out.println("  <tr class=\"boxed\">");
            out.println("     <td><input type=\"text\" size=\"7\" name=\"" + PARAM_CATEGORY_CODE
                    + "\" value=\"\"></td>");
            out.println("     <td><input type=\"text\" size=\"30\" name=\"" + PARAM_CLIENT_NAME
                    + "\" value=\"\"></td>");
            out.println("     <td><input type=\"text\" size=\"3\" name=\"" + PARAM_SORT_ORDER
                    + "\" value=\"\"></td>");
            out.println("     <td><input type=\"text\" size=\"7\" name=\"" + PARAM_CLIENT_ACRONYM
                    + "\" value=\"\"></td>");
            out.println("     <td><input type=\"checkbox\" name=\"" + PARAM_VISIBLE
                    + "\" checked=\"true\"></td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed-submit\" colspan=\"5\"><input type=\"submit\" name=\""
                    + PARAM_ACTION + "\" value=\"" + ACTION_SAVE_CATEGORIES + "\"></td>");
            out.println("  </tr>");
            out.println("</table>");
            out.println("</form>");

            out.println("<br/>");
            out.println("<form action=\"AdminSettingsServlet\" method=\"POST\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th class=\"title\" colspan=\"2\">System Settings</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">External URL</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_EXTERNAL_URL
                    + "\" size=\"50\" value=\""
                    + n(TrackerKeysManager
                            .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email server URL</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_SMTP_ADDRESS
                    + "\" size=\"\" value=\""
                    + n(TrackerKeysManager
                            .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email enabled</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"checkBox\" name=\"" + PARAM_EMAIL_ENABLE
                    + "\" value=\"Y\"" + (n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_ENABLE, dataSession)).equals("Y")
                                    ? " checked"
                                    : "")
                    + ">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email debug logging</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"checkBox\" name=\"" + PARAM_EMAIL_DEBUG
                    + "\" value=\"Y\"" + (n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_DEBUG, dataSession)).equals("Y")
                                    ? " checked"
                                    : "")
                    + ">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email server use SMTPS</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"checkBox\" name=\"" + PARAM_USE_SMTPS
                    + "\" value=\"Y\"" + (n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, dataSession)).equals("Y")
                                    ? " checked"
                                    : "")
                    + ">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email server username</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_EMAIL_SMTPS_USERNAME
                    + "\" size=\"30\" value=\""
                    + n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email server password</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_SMTPS_PASSWORD
                    + "\" size=\"30\" value=\""
                    + n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email server port</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_EMAIL_SMTPS_PORT
                    + "\" size=\"3\" value=\""
                    + n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Email reply address</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_EMAIL_REPLY
                    + "\" size=\"50\" value=\"" + n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Gmail setup hint</th>");
            out.println(
                    "    <td class=\"boxed\">Use smtp.gmail.com, App Password as email server password, and port 587 with SMTPS unchecked (or port 465 with SMTPS checked).</td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Send test email now</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"checkbox\" name=\"" + PARAM_SEND_TEST_EMAIL + "\" value=\"Y\"> ");
            out.println("      To: <input type=\"text\" name=\"" + PARAM_TEST_EMAIL_TO + "\" size=\"40\" value=\"\"> ");
            out.println("      <span class=\"small\">(optional: defaults to reply address, then SMTP username)</span>");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Daily Report Run Time</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_REPORT_DAILY_TIME
                    + "\" size=\"7\" value=\"" + n(TrackerKeysManager.getApplicationKeyValue(
                            TrackerKeysManager.KEY_REPORT_DAILY_TIME, dataSession))
                    + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
                    + PARAM_ACTION + "\" value=\"" + ACTION_SAVE_ADMIN + "\"></td>");
            out.println("  </tr>");
            out.println("</table>");
            out.println("</form>");

            out.println("<br />");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th class=\"title\" colspan=\"2\">Last Used by Users</th>");
            out.println("  </tr>");
            SimpleDateFormat sdf = webUser.getTimeFormat();
            for (String username : ClientServlet.webUserLastUsedDate.keySet()) {
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\">" + username + "</td>");
                out.println("    <td class=\"boxed\">"
                        + sdf.format(ClientServlet.webUserLastUsedDate.get(username)) + "</td>");
                out.println("  </tr>");
            }
            out.println("</table>");

            out.println("<br/>");
            out.println("<form action=\"AdminSettingsServlet\" method=\"POST\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">System Wide Message</th>");
            out.println("    <td class=\"boxed\">");
            out.println("      <input type=\"text\" name=\"" + PARAM_SYSTEM_WIDE_MESSAGE
                    + "\" size=\"50\" value=\""
                    + n(getSystemWideMessage()) + "\">");
            out.println("    </td>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
                    + PARAM_ACTION + "\" value=\"" + ACTION_SAVE_SYSTEM_WIDE_MESSAGE + "\"></td>");
            out.println("  </tr>");
            out.println("</table>");
            out.println("</form>");

            out.println("<br/>");
            out.println("<table class=\"boxed\">");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th class=\"title\" colspan=\"2\">Database Connection Settings</th>");
            out.println("  </tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("     <th>Variable</th>");
            out.println("     <th>Value</th>");
            out.println("  </tr>");
            printDbVariables(out, dataSession, "character_set_%");
            printDbVariables(out, dataSession, "collation_%");
            out.println("</table>");

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

    @Override
    public String getServletInfo() {
        return "Project Tracker Admin Settings";
    }

    private static void printDbVariables(PrintWriter out, Session dataSession, String pattern) {
        Query query = dataSession.createSQLQuery("SHOW VARIABLES LIKE :pattern");
        query.setParameter("pattern", pattern);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            out.println("  <tr class=\"boxed\">");
            out.println("     <td>" + n(row[0]) + "</td>");
            out.println("     <td>" + n(row[1]) + "</td>");
            out.println("  </tr>");
        }
    }
}
