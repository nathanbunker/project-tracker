/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.ReportSchedule;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.report.ReportBatch;
import org.openimmunizationsoftware.pt.report.definition.ReportDefinition;
import org.openimmunizationsoftware.pt.report.definition.ReportParameter;

/**
 * 
 * @author nathan
 */
public class ReportServlet extends ClientServlet {

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
      printHtmlHead(out, "Reports", request);

      Query query = dataSession.createQuery("from ReportProfile where profileId = ?");
      query.setParameter(0, Integer.parseInt(request.getParameter("profileId")));
      List<ReportProfile> reportProfileList = query.list();
      ReportProfile reportProfile = reportProfileList.get(0);
      ReportsServlet.loadReportProfileObject(dataSession, reportProfile);

      ReportProfile extendsReportProfile = null;
      if (reportProfile.getExtendsProfileId() > 0) {
        query = dataSession.createQuery("from ReportProfile where profileId = ?");
        query.setParameter(0, reportProfile.getExtendsProfileId());
        reportProfileList = query.list();
        extendsReportProfile = reportProfileList.get(0);
        ReportsServlet.loadReportProfileObject(dataSession, extendsReportProfile);
      }

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      if (webUser.isUserTypeAdmin()) {
        out.println(
            "    <th class=\"title\" colspan=\"2\">Report<span class=\"right\"><font size=\"-1\"><a href=\"ReportEditServlet?profileId="
                + reportProfile.getProfileId() + "\" class=\"box\">Edit</a></font></span></th>");
      } else {
        out.println("    <th class=\"title\" colspan=\"2\">Report</th>");
      }
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Report</th>");
      out.println("    <td class=\"boxed\">" + reportProfile.getProfileLabel() + "</td>");
      out.println("  </tr>");
      if (extendsReportProfile != null) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Extends</th>");
        out.println("    <td class=\"boxed\">" + extendsReportProfile.getProfileLabel() + "</td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Created By</th>");
      out.println("    <td class=\"boxed\">" + n(reportProfile.getUsername()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Type</th>");
      out.println("    <td class=\"boxed\">" + n(reportProfile.getProfileType()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Enabled</th>");
      out.println(
          "    <td class=\"boxed\">" + (reportProfile.isEnabled() ? "Yes" : "No") + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Extendable</th>");
      out.println(
          "    <td class=\"boxed\">" + (reportProfile.isExtendable() ? "Yes" : "No") + "</td>");
      out.println("  </tr>");
      SimpleDateFormat sdf = webUser.getDateFormat("MM/dd/yyyy HH:mm:ss");
      ReportSchedule reportSchedule = reportProfile.getReportSchedule();
      if (reportSchedule != null) {
        out.println("  <tr>");
        out.println("    <th class=\"title\" colspan=\"2\">Schedule</th>");
        out.println("  </tr>");
        if (reportSchedule.getDateStart() != null) {
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Start Date</th>");
          out.println(
              "    <td class=\"boxed\">" + sdf.format(reportSchedule.getDateStart()) + "</td>");
          out.println("  </tr>");
        }
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Method</th>");
        out.println("    <td class=\"boxed\">" + n(reportSchedule.getMethodText()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Send Period</th>");
        out.println("    <td class=\"boxed\">" + n(reportSchedule.getPeriod()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Send To</th>");
        out.println("    <td class=\"boxed\">" + n(reportSchedule.getLocation()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <td class=\"boxed\">" + n(reportSchedule.getStatus()) + "</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Name</th>");
        out.println("    <td class=\"boxed\">" + n(reportSchedule.getName()) + "</td>");
        out.println("  </tr>");
      }
      ReportDefinition reportDefinition = reportProfile.getReportDefinition();
      List<ReportParameter> reportParameterList = null;
      try {
        reportParameterList = reportDefinition.getReportParameters();
        if (reportParameterList.size() > 0) {
          out.println("  <tr>");
          out.println("    <th class=\"title\" colspan=\"2\">Parameters for Report</th>");
          out.println("  </tr>");
          for (ReportParameter reportParameter : reportParameterList) {
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">" + reportParameter.getLabel() + "</th>");
            out.println("    <td class=\"boxed\">"
                + reportParameter.getValue(reportProfile, dataSession) + "</td>");
            out.println("  </tr>");
          }
        }
      } catch (Exception e) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" colspan=\"2\">Unable to read parameter list: "
            + e.getMessage() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("<br/>");
      out.println("<form action=\"ReportRunServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"profileId\" value=\""
          + reportProfile.getProfileId() + "\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"2\">Run Now</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Run Date</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"runDate\" value=\""
          + webUser.getDateFormat("MM/dd/yyyy hh:mm:ss a").format(new Date()) + "\" size=\"20\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Period</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"period\">");
      List<String[]> optionList = new ArrayList<String[]>();
      optionList.add(new String[] {ReportSchedule.REPORT_DAILY, "Daily Report"});
      optionList.add(new String[] {ReportSchedule.REPORT_WEEKLY, "Weekly Report"});
      optionList.add(new String[] {ReportSchedule.REPORT_MONTHLY, "Monthly Report"});
      optionList.add(new String[] {ReportSchedule.REPORT_YEARLY, "Yearly Report"});
      for (String[] option : optionList) {
        out.println("        <option value=\"" + option[0] + "\">" + option[1] + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      if (reportParameterList != null) {
        try {
          if (reportParameterList.size() > 0) {
            out.println("  <tr>");
            out.println("    <th class=\"title\" colspan=\"2\">Parameters for Report</th>");
            out.println("  </tr>");
            for (ReportParameter reportParameter : reportParameterList) {
              out.println("  <tr class=\"boxed\">");
              out.println("    <th class=\"boxed\">" + reportParameter.getLabel() + "</th>");
              out.println("    <td class=\"boxed\">"
                  + reportParameter.toHtml(reportProfile, request) + "</td>");
              out.println("  </tr>");
            }
          }
        } catch (Exception e) {
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\" colspan=\"2\">Unable to read parameter list: <pre>");
          e.printStackTrace(out);
          out.println("</pre></td>");
          out.println("  </tr>");
        }
      }

      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Run\"><input type=\"submit\" name=\"action\" value=\"Email\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      printHtmlFoot(out);

    } finally {
      out.close();
    }
  }

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

}
