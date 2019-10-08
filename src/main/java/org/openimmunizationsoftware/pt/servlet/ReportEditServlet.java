/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.ReportSchedule;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.report.definition.ReportDefinition;
import org.openimmunizationsoftware.pt.report.definition.ReportParameter;

/**
 * 
 * @author nathan
 */
public class ReportEditServlet extends ClientServlet {

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

      ReportProfile reportProfile = null;
      ReportProfile extendsReportProfile = null;
      SimpleDateFormat sdf = webUser.getDateFormat("MM/dd/yyyy HH:mm:ss");

      if (request.getParameter("profileId") != null) {

        Query query = dataSession.createQuery("from ReportProfile where profileId = ?");
        query.setParameter(0, Integer.parseInt(request.getParameter("profileId")));
        List<ReportProfile> reportProfileList = query.list();
        reportProfile = reportProfileList.get(0);
        ReportsServlet.loadReportProfileObject(dataSession, reportProfile);

        if (reportProfile.getExtendsProfileId() > 0) {
          query = dataSession.createQuery("from ReportProfile where profileId = ?");
          query.setParameter(0, reportProfile.getExtendsProfileId());
          reportProfileList = query.list();
          extendsReportProfile = reportProfileList.get(0);
          ReportsServlet.loadReportProfileObject(dataSession, extendsReportProfile);
        }
      } else {
        Query query = dataSession.createQuery("from ReportProfile where profileId = ?");
        query.setParameter(0, Integer.parseInt(request.getParameter("extendsProfileId")));
        List<ReportProfile> reportProfileList = query.list();
        extendsReportProfile = reportProfileList.get(0);
        ReportsServlet.loadReportProfileObject(dataSession, extendsReportProfile);

        reportProfile = new ReportProfile();
        reportProfile.setExtendsProfileId(extendsReportProfile.getProfileId());
        reportProfile.setProvider(webUser.getProvider());
        reportProfile.setUsername(webUser.getUsername());
        reportProfile.setReportSchedule(new ReportSchedule());
        reportProfile.setProfileType(extendsReportProfile.getProfileType());

      }

      String action = request.getParameter("action");
      if (action != null) {
        if (action.equals("Save")) {
          String message = null;
          reportProfile.setProfileLabel(request.getParameter("profileLabel"));
          reportProfile
              .setUseStatus(request.getParameter("useStatus") != null ? ReportProfile.ENABLED
                  : ReportProfile.DISABLED);
          reportProfile
              .setExtendStatus(request.getParameter("extendStatus") != null ? ReportProfile.ENABLED
                  : ReportProfile.DISABLED);
          ReportSchedule reportSchedule = reportProfile.getReportSchedule();

          try {
            reportSchedule.setDateStart(sdf.parse(request.getParameter("dateStart")));
          } catch (ParseException pe) {
            message = "Unable to parse start date and time: " + pe.getMessage();
          }

          reportSchedule.setMethod(request.getParameter("method"));
          reportSchedule.setPeriod(request.getParameter("period"));
          reportSchedule.setLocation(request.getParameter("location"));
          reportSchedule.setStatus(request.getParameter("status"));
          reportSchedule.setName(request.getParameter("name"));

          if (message != null) {
            request.setAttribute(REQUEST_VAR_MESSAGE, message);
          } else {
            Transaction transaction = dataSession.beginTransaction();
            try {
              dataSession.saveOrUpdate(reportProfile);
              if (reportSchedule.getProfileId() == 0) {
                reportSchedule.setProfileId(reportProfile.getProfileId());
              }
              dataSession.saveOrUpdate(reportSchedule);

            } finally {
              transaction.commit();
            }
            ReportDefinition reportDefinition = reportProfile.getReportDefinition();
            try {
              List<ReportParameter> reportParameterList = reportDefinition.getReportParameters();
              for (ReportParameter reportParameter : reportParameterList) {
                String value = request.getParameter(reportParameter.getName());
                if (value == null) {
                  value = "";
                }
                TrackerKeysManager.saveReportKeyValue(reportParameter.getName(), reportProfile,
                    value, dataSession);
              }
              response.sendRedirect("ReportServlet?profileId=" + reportProfile.getProfileId());
              return;
            } catch (Exception e) {
              e.printStackTrace();
              message = "Unable to save report key value: " + e.getMessage();
            }
          }
        }
      }

      printHtmlHead(out, "Reports", request);
      out.println("<form action=\"ReportEditServlet\" method=\"POST\">");
      if (reportProfile.getProfileId() > 0) {
        out.println("  <input type=\"hidden\" name=\"profileId\" value=\""
            + reportProfile.getProfileId() + "\">");
      }
      if (extendsReportProfile.getProfileId() > 0) {
        out.println("  <input type=\"hidden\" name=\"extendsProfileId\" value=\""
            + extendsReportProfile.getProfileId() + "\">");
      }
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      if (webUser.isUserTypeAdmin()) {
        out.println(
            "    <th class=\"title\" colspan=\"2\">Report<span class=\"right\"><font size=\"-1\"><a href=\"ReportDefinitionEditServlet?profileId="
                + reportProfile.getProfileId()
                + "\" class=\"box\">Edit Definition</a></font></span></th>");
      } else {
        out.println("    <th class=\"title\" colspan=\"2\">Report</th>");
      }
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Report</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"profileLabel\" value=\""
          + n(reportProfile.getProfileLabel()) + "\" size=\"30\"></td>");
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
      out.println("    <th class=\"boxed\">Usable</th>");
      out.println(
          "    <td class=\"boxed\"><input type=\"checkbox\" name=\"useStatus\" value=\"yes\""
              + (reportProfile.isEnabled() ? " checked=\"checked\"" : "") + "></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Extendable</th>");
      out.println(
          "    <td class=\"boxed\"><input type=\"checkbox\" name=\"extendStatus\" value=\"yes\""
              + (reportProfile.isExtendable() ? " checked=\"checked\"" : "") + "></td>");
      out.println("  </tr>");
      ReportSchedule reportSchedule = reportProfile.getReportSchedule();
      if (reportSchedule != null) {
        out.println("  <tr>");
        out.println("    <th class=\"title\" colspan=\"2\">Schedule &amp; Delivery</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Start Date</th>");
        if (reportSchedule.getDateStart() != null) {
          out.println("    <td class=\"boxed\"><input type=\"text\" name=\"dateStart\" value=\""
              + sdf.format(reportSchedule.getDateStart()) + "\" size=\"20\"></td>");
        } else {
          out.println("    <td class=\"boxed\"><input type=\"text\" name=\"dateStart\" value=\""
              + sdf.format(new Date()) + "\" size=\"20\"></td>");
        }
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Method</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"method\">");
        out.println("        <option value=\"" + ReportSchedule.METHOD_NONE + "\""
            + (ReportSchedule.METHOD_NONE.equals(reportSchedule.getMethod()) ? " selected=\"true\""
                : "")
            + ">None</option>");
        out.println("        <option value=\"" + ReportSchedule.METHOD_EMAIL + "\""
            + (ReportSchedule.METHOD_EMAIL.equals(reportSchedule.getMethod()) ? " selected=\"true\""
                : "")
            + ">Email</option>");
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Send Period</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"period\">");
        for (String[] period : ReportSchedule.PERIODS) {
          out.println("        <option value=\"" + period[0] + "\""
              + (period[0].equals(reportSchedule.getPeriod()) ? " selected=\"true\"" : "") + ">"
              + period[1] + "</option>");
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Send To</th>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"location\" value=\""
            + n(reportSchedule.getLocation()) + "\" size=\"40\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Status</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"status\">");
        for (String[] status : ReportSchedule.STATUSES) {
          out.println("        <option value=\"" + status[0] + "\""
              + (status[0].equals(reportSchedule.getStatus()) ? " selected=\"true\"" : "") + ">"
              + status[1] + "</option>");
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Name</th>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"name\" value=\""
            + n(reportSchedule.getName()) + "\" size=\"30\"></td>");
        out.println("  </tr>");
      }
      if (reportProfile.getProfileId() > 0) {
        ReportDefinition reportDefinition = reportProfile.getReportDefinition();
        try {
          List<ReportParameter> reportParameterList = reportDefinition.getReportParameters();
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
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
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
