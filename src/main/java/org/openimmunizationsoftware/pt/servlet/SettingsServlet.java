/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
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
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      HttpSession session = request.getSession(true);
      PrintWriter out = appReq.getOut();
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();

      if (action != null) {
        if (action.equals("Save")) {
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, webUser,
              request.getParameter("displaySize"), dataSession);
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, webUser,
              request.getParameter("displayColor"), dataSession);
          String timeZone = request.getParameter("timeZone");
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TIME_ZONE, webUser, timeZone,
              dataSession);
          webUser.setTimeZone(TimeZone.getTimeZone(timeZone));

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
              "from ProjectCategory where provider = :provider order by sortOrder, clientName");
          query.setParameter("provider", webUser.getProvider());
          @SuppressWarnings("unchecked")
          List<ProjectCategory> projectCategoryList = query.list();
          String categoryCode = request.getParameter("categoryCode");
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
            String clientName = request.getParameter("clientName_" + c);
            String sortOrder = request.getParameter("sortOrder_" + c);
            String clientAcronym = request.getParameter("clientAcronym_" + c);
            String visible = request.getParameter("visible_" + c);
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
              String clientName = request.getParameter("clientName");
              if (clientName.length() > 150) {
                appReq.setMessageProblem("Client name is too long (>150), truncating. ");
                clientName = clientName.substring(0, 150);
              }
              String sortOrder = request.getParameter("sortOrder");
              String clientAcronym = request.getParameter("clientAcronym");
              String visible = request.getParameter("visible");
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
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Time Zone</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"timeZone\">");
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
            "from ProjectCategory where provider = :provider order by sortOrder, clientName");
        query.setParameter("provider", webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<ProjectCategory> projectCategoryList = query.list();
        for (ProjectCategory projectCategory : projectCategoryList) {
          String c = projectCategory.getCategoryCode();
          out.println("  <tr class=\"boxed\">");
          out.println("     <td>" + projectCategory.getCategoryCode() + "</td>");
          out.println("     <td><input type=\"text\" size=\"30\" name=\"clientName_" + c
              + "\" value=\"" + projectCategory.getClientName() + "\"></td>");
          out.println("     <td><input type=\"text\" size=\"3\" name=\"sortOrder_" + c
              + "\" value=\"" + n(projectCategory.getSortOrder()) + "\"></td>");
          out.println("     <td><input type=\"text\" size=\"7\" name=\"clientAcronym_" + c
              + "\" value=\"" + n(projectCategory.getClientAcronym()) + "\"></td>");
          out.println(
              "     <td><input type=\"checkbox\" name=\"visible_" + c + "\""
                  + (projectCategory.getVisible() != null
                      && projectCategory.getVisible().equals("Y") ? " checked=\"true\"" : "")
                  + "\"></td>");
          out.println("  </tr>");
        }
        out.println("  <tr class=\"boxed\">");
        out.println(
            "     <td><input type=\"text\" size=\"7\" name=\"categoryCode\" value=\"\"></td>");
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

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
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
