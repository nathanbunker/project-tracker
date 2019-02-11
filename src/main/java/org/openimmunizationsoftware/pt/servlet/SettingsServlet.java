/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class SettingsServlet extends ClientServlet
{

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
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
  protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);

    if (webUser == null)
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }
    PrintWriter out = response.getWriter();
    try
    {
      Session dataSession = getDataSession(session);

      String action = request.getParameter("action");
      if (action != null)
      {
        if (action.equals("Save"))
        {
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, webUser, request.getParameter("displaySize"), dataSession);
          TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, webUser, request.getParameter("displayColor"), dataSession);
          if (webUser.isUserTypeAdmin())
          {
            TrackerKeysManager.saveKeyValue(TrackerKeysManager.KEY_TRACK_TIME, webUser, request.getParameter("trackTime") != null ? "Y" : "N",
                dataSession);
          }
        } else if (action.equals("Save Admin") && webUser.isUserTypeAdmin())
        {
          
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME, request.getParameter("reportDailyTime"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, request.getParameter("externalUrl"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS, request.getParameter("smtpAddress"), dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, request.getParameter("useSmtps") != null ? "Y"
              : "N", dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, request.getParameter("emailSmtpsUsername"),
              dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, request.getParameter("smtpsPassword"),
              dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, request.getParameter("emailSmtpsPort"),
              dataSession);
          TrackerKeysManager.saveApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY, request.getParameter("emailReply"), dataSession);
        } else if (action.equals("Save System Wide Message") && webUser.isUserTypeAdmin())
        {
          setSystemWideMessage(request.getParameter("systemWideMessage"));
        }
      }
      printHtmlHead(out, "Settings", request);

      String displaySize = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, "small", webUser, dataSession);
      String displayColor = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, "", webUser, dataSession);

      out.println("<form action=\"SettingsServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th  class=\"title\" colspan=\"2\">Personal Settings</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Display Size</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"displaySize\">");
      for (String ds : CssServlet.DISPLAY_SIZE)
      {
        out.println("        <option value=\"" + ds + "\"" + (displaySize.equals(ds) ? " selected" : "") + ">" + ds + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Display Color</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"displayColor\">");
      for (CssServlet.DisplayColor dc : CssServlet.DisplayColor.values())
      {
        String dcl = dc.getLabel();
        out.println("        <option value=\"" + dcl + "\"" + (displayColor.equals(dcl) ? " selected" : "") + ">" + dcl + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      if (webUser.isUserTypeAdmin())
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Track Time</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"checkBox\" name=\"trackTime\" value=\"Y\""
            + (n(TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_TRACK_TIME, "N", webUser, dataSession)).equals("Y") ? " checked" : "") + ">");
        out.println("    </td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      if (webUser.isUserTypeAdmin())
      {
        out.println("<form action=\"SettingsServlet\" method=\"POST\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th  class=\"title\" colspan=\"2\">System System</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">External URL</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"externalUrl\" size=\"50\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server URL</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"smtpAddress\" size=\"\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_SMTP_ADDRESS, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server use SMTPS</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"checkBox\" name=\"useSmtps\" value=\"Y\""
            + (n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_USE_SMTPS, dataSession)).equals("Y") ? " checked" : "")
            + ">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server username</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"emailSmtpsUsername\" size=\"30\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_USERNAME, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server password</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"smtpsPassword\" size=\"30\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PASSWORD, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email server port</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"emailSmtpsPort\" size=\"3\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_SMTPS_PORT, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Email reply address</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"emailReply\" size=\"50\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_SYSTEM_EMAIL_REPLY, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Daily Report Run Time</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"reportDailyTime\" size=\"7\" value=\""
            + n(TrackerKeysManager.getApplicationKeyValue(TrackerKeysManager.KEY_REPORT_DAILY_TIME, dataSession)) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save Admin\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("</form>");
        out.println("<br />");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th  class=\"title\" colspan=\"2\">Last Used by Users</td>");
        out.println("  </tr>");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa");
        for (String username : ClientServlet.webUserLastUsedDate.keySet())
        {
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">" + username + "</td>");
          out.println("    <td class=\"boxed\">" + sdf.format(ClientServlet.webUserLastUsedDate.get(username)) + "</td>");
          out.println("  </tr>");
        }
        out.println("</table>");
        out.println("<br/>");
        out.println("<form action=\"SettingsServlet\" method=\"POST\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">System Wide Message</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"systemWideMessage\" size=\"50\" value=\"" + n(getSystemWideMessage()) + "\">");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save System Wide Message\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("</form>");

      }

      printHtmlFoot(out);

    } finally
    {
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "DQA Tester Home Page";
  }// </editor-fold>
}
