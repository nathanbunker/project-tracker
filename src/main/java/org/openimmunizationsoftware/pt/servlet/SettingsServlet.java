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
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.WebApiClient;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class SettingsServlet extends ClientServlet {

  private static final String ACTION_SAVE = "Save";
  private static final String ACTION_SAVE_ADMIN = "Save Admin";
  private static final String ACTION_SAVE_CATEGORIES = "Save Categories";
  private static final String ACTION_SAVE_SYSTEM_WIDE_MESSAGE = "Save System Wide Message";
  private static final String ACTION_CREATE_API_KEY = "Create API Key";
  private static final String ACTION_DELETE_API_KEY = "Delete API Key";

  private static final String PARAM_ACTION = "action";
  private static final String PARAM_CLIENT_ACRONYM = "clientAcronym";
  private static final String PARAM_CLIENT_ACRONYM_PREFIX = "clientAcronym_";
  private static final String PARAM_CLIENT_NAME = "clientName";
  private static final String PARAM_CLIENT_NAME_PREFIX = "clientName_";
  private static final String PARAM_DISPLAY_COLOR = "displayColor";
  private static final String PARAM_DISPLAY_SIZE = "displaySize";
  private static final String PARAM_EMAIL_REPLY = "emailReply";
  private static final String PARAM_EMAIL_SMTPS_PORT = "emailSmtpsPort";
  private static final String PARAM_EMAIL_SMTPS_USERNAME = "emailSmtpsUsername";
  private static final String PARAM_EXTERNAL_URL = "externalUrl";
  private static final String PARAM_REPORT_DAILY_TIME = "reportDailyTime";
  private static final String PARAM_SMTP_ADDRESS = "smtpAddress";
  private static final String PARAM_SMTPS_PASSWORD = "smtpsPassword";
  private static final String PARAM_SORT_ORDER = "sortOrder";
  private static final String PARAM_SORT_ORDER_PREFIX = "sortOrder_";
  private static final String PARAM_SYSTEM_WIDE_MESSAGE = "systemWideMessage";
  private static final String PARAM_TIME_ZONE = "timeZone";
  private static final String PARAM_TRACK_TIME = "trackTime";
  private static final String PARAM_USE_SMTPS = "useSmtps";
  private static final String PARAM_VISIBLE = "visible";
  private static final String PARAM_VISIBLE_PREFIX = "visible_";
  private static final String PARAM_CATEGORY_CODE = "categoryCode";
  private static final String PARAM_API_KEY_AGENT_NAME = "apiKeyAgentName";
  private static final String PARAM_API_KEY_CLIENT_ID = "apiKeyClientId";

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
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, webUser,
              request.getParameter(PARAM_DISPLAY_SIZE), dataSession);
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, webUser,
              request.getParameter(PARAM_DISPLAY_COLOR), dataSession);
          String timeZone = request.getParameter(PARAM_TIME_ZONE);
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TIME_ZONE, webUser, timeZone,
              dataSession);
          webUser.setTimeZone(TimeZone.getTimeZone(timeZone));

          if (webUser.isUserTypeAdmin()) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TRACK_TIME, webUser,
                request.getParameter(PARAM_TRACK_TIME) != null ? "Y" : "N", dataSession);
          }
        } else if (action.equals(ACTION_SAVE_ADMIN) && webUser.isUserTypeAdmin()) {

          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME,
              request.getParameter(PARAM_REPORT_DAILY_TIME), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL,
              request.getParameter(PARAM_EXTERNAL_URL), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS,
              request.getParameter(PARAM_SMTP_ADDRESS), dataSession);
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
        } else if (action.equals(ACTION_SAVE_SYSTEM_WIDE_MESSAGE) && webUser.isUserTypeAdmin()) {
          setSystemWideMessage(request.getParameter(PARAM_SYSTEM_WIDE_MESSAGE));
        } else if (action.equals(ACTION_SAVE_CATEGORIES) && webUser.isUserTypeAdmin()) {
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
            if (categoryCodeIsUnique) {
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
                appReq
                    .setMessageProblem("Category name was not set, so new category not created. ");
              }
            } else {
              appReq
                  .setMessageProblem("Category code was not unique, so new category not created. ");
            }

          }
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
                  "from WebApiClient where username = :username and enabled = true and lower(agentName) = :agentNameLower and providerId is null");
            } else {
              query = dataSession.createQuery(
                  "from WebApiClient where username = :username and enabled = true and lower(agentName) = :agentNameLower and providerId = :providerId");
              query.setParameter("providerId", providerId);
            }
            query.setParameter("username", webUser.getUsername());
            query.setParameter("agentNameLower", agentName.toLowerCase());
            @SuppressWarnings("unchecked")
            List<WebApiClient> existingClients = query.list();
            if (existingClients.size() > 0) {
              appReq.setMessageProblem(
                  "Agent name already exists, so new API key not created. ");
            } else {
              WebApiClient client = new WebApiClient();
              client.setUsername(webUser.getUsername());
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
                  && webUser.getUsername().equals(client.getUsername())
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

      String displaySize = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE,
          "small", webUser, dataSession);
      String displayColor = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, "",
          webUser, dataSession);
      String timeZone = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_TIME_ZONE,
          WebUser.AMERICA_DENVER, webUser, dataSession);

      out.println("<form action=\"SettingsServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th  class=\"title\" colspan=\"2\">Personal Settings</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Display Size</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"" + PARAM_DISPLAY_SIZE + "\">");
      for (String ds : CssServlet.DISPLAY_SIZE) {
        out.println("        <option value=\"" + ds + "\""
            + (displaySize.equals(ds) ? " selected" : "") + ">" + ds + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Display Color</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"" + PARAM_DISPLAY_COLOR + "\">");
      for (CssServlet.DisplayColor dc : CssServlet.DisplayColor.values()) {
        String dcl = dc.getLabel();
        out.println("        <option value=\"" + dcl + "\""
            + (displayColor.equals(dcl) ? " selected" : "") + ">" + dcl + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Time Zone</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"" + PARAM_TIME_ZONE + "\">");
      for (String tz : TimeZone.getAvailableIDs()) {
        out.println("        <option value=\"" + tz + "\""
            + (timeZone.equals(tz) ? " selected" : "") + ">" + tz + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      if (webUser.isUserTypeAdmin()) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Track Time</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"checkBox\" name=\"" + PARAM_TRACK_TIME
            + "\" value=\"Y\""
            + (n(TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_TRACK_TIME, "N", webUser,
                dataSession)).equals("Y") ? " checked" : "")
            + ">");
        out.println("    </td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\""
          + PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      if (webUser.isUserTypeAdmin()) {
        out.println("<form action=\"SettingsServlet\" method=\"POST\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th class=\"title\" colspan=\"5\">Categories</td>");
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
              + "\"></td>");
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

        out.println("<form action=\"SettingsServlet\" method=\"POST\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th  class=\"title\" colspan=\"2\">System System</td>");
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
        out.println("     <th  class=\"title\" colspan=\"2\">Last Used by Users</td>");
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
        out.println("<form action=\"SettingsServlet\" method=\"POST\">");
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

      }

      String providerId = webUser.getProvider() == null ? null : webUser.getProvider().getProviderId();
      Query apiKeyQuery;
      if (providerId == null) {
        apiKeyQuery = dataSession.createQuery(
            "from WebApiClient where username = :username and enabled = true and providerId is null order by createDate desc");
      } else {
        apiKeyQuery = dataSession.createQuery(
            "from WebApiClient where username = :username and enabled = true and providerId = :providerId order by createDate desc");
        apiKeyQuery.setParameter("providerId", providerId);
      }
      apiKeyQuery.setParameter("username", webUser.getUsername());
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

    } catch (Exception e) {
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
}
