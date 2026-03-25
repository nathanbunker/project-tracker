/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.WebApiClient;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class SettingsServlet extends ClientServlet {

  private static final String ACTION_SAVE = "Save";
  private static final String ACTION_SAVE_REPORT_SETTINGS = "Save Report Settings";
  private static final String ACTION_CREATE_API_KEY = "Create API Key";
  private static final String ACTION_DELETE_API_KEY = "Delete API Key";

  private static final String PARAM_ACTION = "action";
  private static final String PARAM_DISPLAY_COLOR = "displayColor";
  private static final String PARAM_DISPLAY_SIZE = "displaySize";
  private static final String PARAM_TIME_ZONE_USER = "timeZoneUser";
  private static final String PARAM_TIME_ZONE_APPLICATION = "timeZoneApplication";
  private static final String PARAM_DATE_DISPLAY_FORMAT = "dateDisplayFormat";
  private static final String PARAM_DATE_ENTRY_FORMAT = "dateEntryFormat";
  private static final String PARAM_TIME_DISPLAY_FORMAT = "timeDisplayFormat";
  private static final String PARAM_TIME_ENTRY_FORMAT = "timeEntryFormat";
  private static final String PARAM_TRACK_TIME = "trackTime";
  private static final String PARAM_REPORT_DAILY_ENABLED = "reportDailyEnabled";
  private static final String PARAM_API_KEY_AGENT_NAME = "apiKeyAgentName";
  private static final String PARAM_API_KEY_CLIENT_ID = "apiKeyClientId";
  private static final String PARAM_FIRST_NAME = "firstName";
  private static final String PARAM_LAST_NAME = "lastName";
  private static final String PARAM_WORKFLOW_TYPE = "workflowType";

  private static final String WORKFLOW_TYPE_STANDARD = "STANDARD";
  private static final String WORKFLOW_TYPE_STUDENT = "STUDENT";

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      PrintWriter out = appReq.getOut();
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();

      if (action != null) {
        if (action.equals(ACTION_SAVE)) {
          String firstName = request.getParameter(PARAM_FIRST_NAME);
          String lastName = request.getParameter(PARAM_LAST_NAME);
          String workflowType = request.getParameter(PARAM_WORKFLOW_TYPE);

          firstName = firstName == null ? "" : firstName.trim();
          lastName = lastName == null ? "" : lastName.trim();
          workflowType = WORKFLOW_TYPE_STUDENT.equals(workflowType) ? WORKFLOW_TYPE_STUDENT
              : WORKFLOW_TYPE_STANDARD;

          if (firstName.length() < 1 || lastName.length() < 1) {
            appReq.setMessageProblem("First name and last name must each have at least one character.");
          } else {
            WebUser webUserDb = (WebUser) dataSession.get(WebUser.class, webUser.getWebUserId());
            Transaction trans = dataSession.beginTransaction();
            webUserDb.setFirstName(firstName);
            webUserDb.setLastName(lastName);
            webUserDb.setWorkflowType(workflowType);
            dataSession.update(webUserDb);
            trans.commit();

            webUser.setFirstName(firstName);
            webUser.setLastName(lastName);
            webUser.setWorkflowType(workflowType);
          }

          if (request.getParameter(PARAM_DISPLAY_SIZE) != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, webUser,
                request.getParameter(PARAM_DISPLAY_SIZE), dataSession);
          }
          if (request.getParameter(PARAM_DISPLAY_COLOR) != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, webUser,
                request.getParameter(PARAM_DISPLAY_COLOR), dataSession);
          }
          String timeZoneUser = request.getParameter(PARAM_TIME_ZONE_USER);
          String timeZoneApplication = request.getParameter(PARAM_TIME_ZONE_APPLICATION);
          String dateDisplayFormat = request.getParameter(PARAM_DATE_DISPLAY_FORMAT);
          String dateEntryFormat = request.getParameter(PARAM_DATE_ENTRY_FORMAT);
          String timeDisplayFormat = request.getParameter(PARAM_TIME_DISPLAY_FORMAT);
          String timeEntryFormat = request.getParameter(PARAM_TIME_ENTRY_FORMAT);
          if (timeZoneUser != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TIME_ZONE, webUser, timeZoneUser,
                dataSession);
            webUser.setTimeZone(TimeZone.getTimeZone(timeZoneUser));
          }
          if (dateDisplayFormat != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DATE_DISPLAY_FORMAT, webUser,
                dateDisplayFormat, dataSession);
            webUser.setDateDisplayPattern(dateDisplayFormat);
          }
          if (dateEntryFormat != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DATE_ENTRY_FORMAT, webUser,
                dateEntryFormat, dataSession);
            webUser.setDateEntryPattern(dateEntryFormat);
          }
          if (timeDisplayFormat != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TIME_DISPLAY_FORMAT, webUser,
                timeDisplayFormat, dataSession);
            webUser.setTimeDisplayPattern(timeDisplayFormat);
          }
          if (timeEntryFormat != null) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TIME_ENTRY_FORMAT, webUser,
                timeEntryFormat, dataSession);
            webUser.setTimeEntryPattern(timeEntryFormat);
          }
          if (timeZoneApplication != null) {
            TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_TIME_ZONE,
                timeZoneApplication, dataSession);
          }

          if (webUser.isUserTypeAdmin()) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TRACK_TIME, webUser,
                request.getParameter(PARAM_TRACK_TIME) != null ? "Y" : "N", dataSession);
          }
        } else if (action.equals(ACTION_SAVE_REPORT_SETTINGS)) {
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_ENABLED, webUser,
              request.getParameter(PARAM_REPORT_DAILY_ENABLED) != null ? "Y" : "N", dataSession);
        }
        if (action.equals(ACTION_CREATE_API_KEY)) {
          String agentName = request.getParameter(PARAM_API_KEY_AGENT_NAME);
          if (agentName == null || agentName.trim().equals("")) {
            appReq.setMessageProblem("Agent name is required to create an API key. ");
          } else {
            agentName = agentName.trim();
            String providerId = webUser.getProvider() == null ? null
                : webUser.getProvider().getProviderId();
            Query query = null;
            if (providerId == null) {
              query = dataSession.createQuery(
                  "from WebApiClient where webUser = :webUser and enabled = true and lower(agentName) = :agentNameLower and providerId is null");
            } else {
              query = dataSession.createQuery(
                  "from WebApiClient where webUser = :webUser and enabled = true and lower(agentName) = :agentNameLower and providerId = :providerId");
              query.setParameter("providerId", providerId);
            }
            query.setParameter("webUser", webUser);
            query.setParameter("agentNameLower", agentName.toLowerCase());
            @SuppressWarnings("unchecked")
            List<WebApiClient> existingClients = query.list();
            if (existingClients.size() > 0) {
              appReq.setMessageProblem(
                  "Agent name already exists, so new API key not created. ");
            } else {
              WebApiClient client = new WebApiClient();
              client.setWebUser(webUser);
              client.setProviderId(providerId);
              client.setAgentName(agentName);
              client.setEnabled(true);
              client.setCreateDate(new Date());
              client.setApiKey(generateUniqueApiKey(dataSession));
              Transaction trans = dataSession.beginTransaction();
              dataSession.save(client);
              trans.commit();
            }
          }
        } else if (action.equals(ACTION_DELETE_API_KEY)) {
          String clientIdString = request.getParameter(PARAM_API_KEY_CLIENT_ID);
          if (clientIdString != null && !clientIdString.trim().equals("")) {
            try {
              int clientId = Integer.parseInt(clientIdString);
              WebApiClient client = (WebApiClient) dataSession.get(WebApiClient.class, clientId);
              String providerId = webUser.getProvider() == null ? null
                  : webUser.getProvider().getProviderId();
              if (client != null && client.isEnabled()
                  && webUser.equals(client.getWebUser())
                  && sameProvider(providerId, client.getProviderId())) {
                client.setEnabled(false);
                Transaction trans = dataSession.beginTransaction();
                dataSession.update(client);
                trans.commit();
              }
            } catch (NumberFormatException nfe) {
              // do nothing
            }
          }
        }
      }
      appReq.setTitle("Settings");
      printHtmlHead(appReq);
      printDandelionLocation(out, "Setup / Settings");

      if (webUser.isUserTypeAdmin()) {
        out.println("<br/>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed\">Need system-wide controls? <a href=\"AdminSettingsServlet\">Open Admin Settings</a></td>");
        out.println("  </tr>");
        out.println("</table>");
      }

      boolean userDailyReportsEnabled = TrackerKeysManager.getUserKeyValueBooleanNoFallback(
          TrackerKeysManager.KEY_REPORT_DAILY_ENABLED, false, webUser, dataSession);

      out.println("<br/>");
      out.println("<form action=\"SettingsServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th class=\"title\" colspan=\"2\">Profile Settings</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">First Name</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + PARAM_FIRST_NAME
          + "\" size=\"30\" value=\"" + h(webUser.getFirstName()) + "\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Last Name</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + PARAM_LAST_NAME
          + "\" size=\"30\" value=\"" + h(webUser.getLastName()) + "\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Workflow Type</th>");
      out.println("    <td class=\"boxed\"><select name=\"" + PARAM_WORKFLOW_TYPE + "\">");
      String selectedWorkflowType = webUser.getWorkflowType() == null ? WORKFLOW_TYPE_STANDARD
          : webUser.getWorkflowType();
      out.println("      <option value=\"" + WORKFLOW_TYPE_STANDARD + "\""
          + (WORKFLOW_TYPE_STANDARD.equals(selectedWorkflowType) ? " selected" : "")
          + ">Standard</option>");
      out.println("      <option value=\"" + WORKFLOW_TYPE_STUDENT + "\""
          + (WORKFLOW_TYPE_STUDENT.equals(selectedWorkflowType) ? " selected" : "")
          + ">School</option>");
      out.println("    </select></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
          + PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      out.println("<br/>");
      out.println("<form action=\"SettingsServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th class=\"title\" colspan=\"2\">Automatic Report Emails</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Receive automatic report emails</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <input type=\"checkbox\" name=\"" + PARAM_REPORT_DAILY_ENABLED
          + "\" value=\"Y\"" + (userDailyReportsEnabled ? " checked" : "") + "> ");
      out.println(
          "      <span class=\"small\">Both this setting and the admin daily report switch must be enabled before automatic reports send.</span>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
          + PARAM_ACTION + "\" value=\"" + ACTION_SAVE_REPORT_SETTINGS + "\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      String providerId = webUser.getProvider() == null ? null : webUser.getProvider().getProviderId();
      Query apiKeyQuery;
      if (providerId == null) {
        apiKeyQuery = dataSession.createQuery(
            "from WebApiClient where webUser = :webUser and enabled = true and providerId is null order by createDate desc");
      } else {
        apiKeyQuery = dataSession.createQuery(
            "from WebApiClient where webUser = :webUser and enabled = true and providerId = :providerId order by createDate desc");
        apiKeyQuery.setParameter("providerId", providerId);
      }
      apiKeyQuery.setParameter("webUser", webUser);
      @SuppressWarnings("unchecked")
      List<WebApiClient> apiClients = apiKeyQuery.list();
      if (apiClients.size() > 0) {
        out.println("<br/>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th class=\"title\" colspan=\"5\">Web API Client Keys</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th>Agent Name</th>");
        out.println("     <th>Created Date</th>");
        out.println("     <th>Last Used Date</th>");
        out.println("     <th>API Key</th>");
        out.println("     <th>Action</th>");
        out.println("  </tr>");
        SimpleDateFormat apiKeyDateFormat = webUser.getTimeFormat();
        for (WebApiClient client : apiClients) {
          out.println("  <tr class=\"boxed\">");
          out.println("     <td>" + n(client.getAgentName()) + "</td>");
          out.println("     <td>"
              + (client.getCreateDate() == null ? "" : apiKeyDateFormat.format(client.getCreateDate()))
              + "</td>");
          out.println("     <td>"
              + (client.getLastUsedDate() == null ? "" : apiKeyDateFormat.format(client.getLastUsedDate()))
              + "</td>");
          out.println("     <td>" + n(client.getApiKey()) + "</td>");
          out.println("     <td>");
          out.println("       <form action=\"SettingsServlet\" method=\"POST\">");
          out.println("         <input type=\"hidden\" name=\"" + PARAM_API_KEY_CLIENT_ID
              + "\" value=\"" + client.getClientId() + "\">");
          out.println("         <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\""
              + ACTION_DELETE_API_KEY + "\">");
          out.println("       </form>");
          out.println("     </td>");
          out.println("  </tr>");
        }
        out.println("</table>");
      }
      out.println("<br/>");
      out.println("<form action=\"SettingsServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th class=\"title\" colspan=\"2\">Create Web API Client Key</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Agent Name</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <input type=\"text\" name=\"" + PARAM_API_KEY_AGENT_NAME
          + "\" size=\"30\" value=\"\">");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
          + PARAM_ACTION + "\" value=\"" + ACTION_CREATE_API_KEY + "\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      printHtmlFoot(appReq);

    } catch (

    Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }

  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the
  // code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "DQA Tester Home Page";
  }// </editor-fold>

  private static boolean sameProvider(String expectedProviderId, String actualProviderId) {
    if (expectedProviderId == null) {
      return actualProviderId == null;
    }
    return expectedProviderId.equals(actualProviderId);
  }

  private static String generateUniqueApiKey(Session dataSession) {
    String apiKey = "";
    while (true) {
      apiKey = UUID.randomUUID().toString().replace("-", "");
      Query query = dataSession
          .createQuery("select clientId from WebApiClient where apiKey = :apiKey");
      query.setParameter("apiKey", apiKey);
      @SuppressWarnings("unchecked")
      List<Integer> matches = query.list();
      if (matches.isEmpty()) {
        return apiKey;
      }
    }
  }

  private static String h(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }

}
