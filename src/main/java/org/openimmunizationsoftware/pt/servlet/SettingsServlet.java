/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ProjectClient;
import org.openimmunizationsoftware.pt.model.ProjectClientId;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class SettingsServlet extends ClientServlet {

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);

    if (webUser == null) {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }
    PrintWriter out = response.getWriter();
    try {
      Session dataSession = getDataSession(session);

      String action = request.getParameter("action");
      if (action != null) {
        if (action.equals("Save")) {
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, webUser,
              request.getParameter("displaySize"), dataSession);
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, webUser,
              request.getParameter("displayColor"), dataSession);
          if (webUser.isUserTypeAdmin()) {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TRACK_TIME, webUser,
                request.getParameter("trackTime") != null ? "Y" : "N", dataSession);
          }
        } else if (action.equals("Save Admin") && webUser.isUserTypeAdmin()) {

          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME,
              request.getParameter("reportDailyTime"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL,
              request.getParameter("externalUrl"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS,
              request.getParameter("smtpAddress"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS,
              request.getParameter("useSmtps") != null ? "Y" : "N", dataSession);
          TrackerKeysManager.saveApplicationKeyValue(
              TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME,
              request.getParameter("emailSmtpsUsername"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(
              TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD,
              request.getParameter("smtpsPassword"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT,
              request.getParameter("emailSmtpsPort"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY,
              request.getParameter("emailReply"), dataSession);
        } else if (action.equals("Save System Wide Message") && webUser.isUserTypeAdmin()) {
          setSystemWideMessage(request.getParameter("systemWideMessage"));
        } else if (action.equals("Save Categories") && webUser.isUserTypeAdmin()) {
          Query query = dataSession.createQuery(
              "from ProjectClient where id.providerId = ? order by sortOrder, clientName");
          query.setParameter(0, webUser.getProviderId());
          List<ProjectClient> projectClientList = query.list();
          String clientCode = request.getParameter("clientCode");
          if (clientCode.length() > 15) {
            request.setAttribute(REQUEST_VAR_MESSAGE,
                "Category code is too long (>15), so new category not created. ");
            clientCode = "";
          }
          boolean clientCodeIsUnique = true;
          for (ProjectClient projectClient : projectClientList) {
            String c = projectClient.getId().getClientCode();
            if (c.equalsIgnoreCase(clientCode)) {
              clientCodeIsUnique = false;
            }
            String clientName = request.getParameter("clientName_" + c);
            String sortOrder = request.getParameter("sortOrder_" + c);
            String clientAcronym = request.getParameter("clientAcronym_" + c);
            String visible = request.getParameter("visible_" + c);
            if (!clientName.equals("")) {
              projectClient.setClientName(clientName);
            }
            if (!sortOrder.equals("")) {
              try {
                projectClient.setSortOrder(Integer.parseInt(sortOrder));
              } catch (NumberFormatException nfe) {
                // do nothing
              }
            }
            if (!clientAcronym.equals("")) {
              projectClient.setClientAcronym(clientAcronym);
            }
            projectClient.setVisible(visible == null ? "N" : "Y");
            Transaction trans = dataSession.beginTransaction();
            dataSession.update(projectClient);
            trans.commit();
          }
          if (!clientCode.equals("") && clientCodeIsUnique) {
            if (clientCodeIsUnique) {
              String clientName = request.getParameter("clientName");
              if (clientName.length() > 150) {
                request.setAttribute(REQUEST_VAR_MESSAGE,
                    "Client name is too long (>150), truncating. ");
                clientName = clientName.substring(0, 150);
              }
              String sortOrder = request.getParameter("sortOrder");
              String clientAcronym = request.getParameter("clientAcronym");
              String visible = request.getParameter("visible");
              if (!clientName.equals("")) {
                ProjectClientId projectClientId = new ProjectClientId();
                projectClientId.setClientCode(clientCode);
                projectClientId.setProviderId(webUser.getProviderId());
                ProjectClient projectClient = new ProjectClient(projectClientId, clientName);
                if (!sortOrder.equals("")) {
                  try {
                    projectClient.setSortOrder(Integer.parseInt(sortOrder));
                  } catch (NumberFormatException nfe) {
                    // do nothing
                  }
                }
                if (!clientAcronym.equals("")) {
                  if (clientAcronym.length() > 15) {
                    request.setAttribute(REQUEST_VAR_MESSAGE,
                        "Client acronym is too long (>15), not setting. ");
                  } else {
                    projectClient.setClientAcronym(clientAcronym);
                  }
                }
                projectClient.setVisible(visible == null ? "N" : "Y");
                Transaction trans = dataSession.beginTransaction();
                dataSession.save(projectClient);
                trans.commit();
              } else {
                request.setAttribute(REQUEST_VAR_MESSAGE,
                    "Category name was not set, so new category not created. ");
              }
            } else {
              request.setAttribute(REQUEST_VAR_MESSAGE,
                  "Category code was not unique, so new category not created. ");
            }

          }
        }
      }
      printHtmlHead(out, "Settings", request);

      String displaySize = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE,
          "small", webUser, dataSession);
      String displayColor = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, "",
          webUser, dataSession);

      out.println("<form action=\"SettingsServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th  class=\"title\" colspan=\"2\">Personal Settings</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Display Size</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"displaySize\">");
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
      out.println("      <select name=\"displayColor\">");
      for (CssServlet.DisplayColor dc : CssServlet.DisplayColor.values()) {
        String dcl = dc.getLabel();
        out.println("        <option value=\"" + dcl + "\""
            + (displayColor.equals(dcl) ? " selected" : "") + ">" + dcl + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      if (webUser.isUserTypeAdmin()) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Track Time</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"checkBox\" name=\"trackTime\" value=\"Y\""
            + (n(TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_TRACK_TIME, "N", webUser,
                dataSession)).equals("Y") ? " checked" : "")
            + ">");
        out.println("    </td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
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
            "from ProjectClient where id.providerId = ? order by sortOrder, clientName");
        query.setParameter(0, webUser.getProviderId());
        List<ProjectClient> projectClientList = query.list();
        for (ProjectClient projectClient : projectClientList) {
          String c = projectClient.getId().getClientCode();
          out.println("  <tr class=\"boxed\">");
          out.println("     <td>" + projectClient.getId().getClientCode() + "</td>");
          out.println("     <td><input type=\"text\" size=\"30\" name=\"clientName_" + c
              + "\" value=\"" + projectClient.getClientName() + "\"></td>");
          out.println("     <td><input type=\"text\" size=\"3\" name=\"sortOrder_" + c
              + "\" value=\"" + n(projectClient.getSortOrder()) + "\"></td>");
          out.println("     <td><input type=\"text\" size=\"7\" name=\"clientAcronym_" + c
              + "\" value=\"" + n(projectClient.getClientAcronym()) + "\"></td>");
          out.println("     <td><input type=\"checkbox\" name=\"visible_" + c + "\""
              + (projectClient.getVisible() != null && projectClient.getVisible().equals("Y")
                  ? " checked=\"true\""
                  : "")
              + "\"></td>");
          out.println("  </tr>");
        }
        out.println("  <tr class=\"boxed\">");
        out.println(
            "     <td><input type=\"text\" size=\"7\" name=\"clientCode\" value=\"\"></td>");
        out.println(
            "     <td><input type=\"text\" size=\"30\" name=\"clientName\" value=\"\"></td>");
        out.println("     <td><input type=\"text\" size=\"3\" name=\"sortOrder\" value=\"\"></td>");
        out.println(
            "     <td><input type=\"text\" size=\"7\" name=\"clientAcronym\" value=\"\"></td>");
        out.println("     <td><input type=\"checkbox\" name=\"visible\" checked=\"true\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed-submit\" colspan=\"5\"><input type=\"submit\" name=\"action\" value=\"Save Categories\"></td>");
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
        out.println("      <input type=\"text\" name=\"externalUrl\" size=\"50\" value=\""
            + n(TrackerKeysManager
                .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, dataSession))
            + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server URL</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"smtpAddress\" size=\"\" value=\""
            + n(TrackerKeysManager
                .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS, dataSession))
            + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server use SMTPS</th>");
        out.println("    <td class=\"boxed\">");
        out.println(
            "      <input type=\"checkBox\" name=\"useSmtps\" value=\"Y\"" + (n(TrackerKeysManager
                .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, dataSession))
                    .equals("Y") ? " checked" : "")
                + ">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server username</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"emailSmtpsUsername\" size=\"30\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, dataSession))
            + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server password</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"smtpsPassword\" size=\"30\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession))
            + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server port</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"emailSmtpsPort\" size=\"3\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, dataSession))
            + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email reply address</th>");
        out.println("    <td class=\"boxed\">");
        out.println(
            "      <input type=\"text\" name=\"emailReply\" size=\"50\" value=\""
                + n(TrackerKeysManager
                    .getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY, dataSession))
                + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Daily Report Run Time</th>");
        out.println("    <td class=\"boxed\">");
        out.println(
            "      <input type=\"text\" name=\"reportDailyTime\" size=\"7\" value=\""
                + n(TrackerKeysManager
                    .getApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME, dataSession))
                + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save Admin\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("</form>");
        out.println("<br />");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th  class=\"title\" colspan=\"2\">Last Used by Users</td>");
        out.println("  </tr>");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa");
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
        out.println("      <input type=\"text\" name=\"systemWideMessage\" size=\"50\" value=\""
            + n(getSystemWideMessage()) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save System Wide Message\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("</form>");

      }

      printHtmlFoot(out);

    } finally {
      out.close();
    }
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
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
   *          servlet request
   * @param response
   *          servlet response
   * @throws ServletException
   *           if a servlet-specific error occurs
   * @throws IOException
   *           if an I/O error occurs
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
}
