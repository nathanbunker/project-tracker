/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.ReportSchedule;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ReportsServlet extends ClientServlet
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
      printHtmlHead(out, "Reports", request);

      Query query = dataSession.createQuery("from ReportProfile where providerId = ? and useStatus = 'E' order by profileLabel");
      query.setParameter(0, webUser.getProviderId());
      List<ReportProfile> reportProfileList = query.list();

      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Report</th>");
      out.println("    <th class=\"boxed\">Type</th>");
      out.println("    <th class=\"boxed\">Send</th>");
      out.println("    <th class=\"boxed\">Status</th>");
      out.println("  </tr>");
      for (ReportProfile reportProfile : reportProfileList)
      {
        loadReportProfileObject(dataSession, reportProfile);
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"ReportServlet?profileId=" + reportProfile.getProfileId() + "\" class=\"button\">"
            + reportProfile.getProfileLabel() + "</a></td>");
        out.println("    <td class=\"boxed\">" + reportProfile.getProfileType() + "</td>");

        ReportSchedule reportSchedule = reportProfile.getReportSchedule();
        if (reportSchedule != null)
        {
          out.println("    <td class=\"boxed\">" + (reportSchedule.getPeriod() != null ? reportSchedule.getPeriodText() : "") + "</td>");
          out.println("    <td class=\"boxed\">" + (reportSchedule.getStatus() != null ? reportSchedule.getStatusText() : "") + "</td>");
        } else
        {
          out.println("    <td class=\"boxed\">&nbsp;</td>");
          out.println("    <td class=\"boxed\">&nbsp;</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("<h2>Create a New Report</h2>");
      query = dataSession.createQuery("from ReportProfile where providerId = 0 and extendStatus = 'E' order by profileLabel");
      List<ReportProfile> extendReportProfileList = query.list();
      out.println("<p>Choose a template to create a new report:</p>");
      out.println("<ul>");
      for (ReportProfile reportProfile : extendReportProfileList)
      {
        out.println("  <li><a href=\"ReportEditServlet?extendsProfileId=" + reportProfile.getProfileId() + "\">" + reportProfile.getProfileLabel()
            + "</a></li>");
      }
      out.println("</ul>");
      if (webUser.isUserTypeAdmin())
      {
        out.println("<h2>Reports Run</h2>");
        out.println("<p><a href=\"ReportRunServlet\">See all reports</a> that have been sent since Tracker was initialized.</p>");
      }
      printHtmlFoot(out);

    } finally
    {
      out.close();
    }
  }

  protected static void loadReportProfileObject(Session dataSession, ReportProfile reportProfile)
  {
    Query query = dataSession.createQuery("from ReportSchedule where profileId = ?");
    query.setParameter(0, reportProfile.getProfileId());
    List<ReportSchedule> reportScheduleList = query.list();
    reportProfile.setReportSchedule(reportScheduleList.size() > 0 ? reportScheduleList.get(0) : null);
    reportProfile.setReportCSS(TrackerKeysManager.getKeyContent(TrackerKeysManager.KEY_REPORT_CSS, TrackerKeysManager.KEY_TYPE_REPORT,
        String.valueOf(reportProfile.getProfileId()), dataSession));
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

}
