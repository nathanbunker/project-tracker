/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ReportDefinitionEditServlet extends ClientServlet {

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
    AppReq appReq = new AppReq(request, response);
    try {
      WebUser webUser = appReq.getWebUser();
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();



      ReportProfile reportProfile = null;

      Query query = dataSession.createQuery("from ReportProfile where profileId = ?");
      query.setParameter(0, Integer.parseInt(request.getParameter("profileId")));
      List<ReportProfile> reportProfileList = query.list();
      reportProfile = reportProfileList.get(0);
      ReportsServlet.loadReportProfileObject(dataSession, reportProfile);

      if (action != null) {
        if (action.equals("Save")) {
          String message = null;
          reportProfile.setDefinition(request.getParameter("definition").getBytes());
          reportProfile.clearReportDefinition();
          // TODO check report

          if (message != null) {
            appReq.setMessageProblem(message);
          } else {
            Transaction transaction = dataSession.beginTransaction();
            try {
              dataSession.saveOrUpdate(reportProfile);
            } finally {
              transaction.commit();
            }
            response.sendRedirect("ReportEditServlet?profileId=" + reportProfile.getProfileId());
            return;
          }
        }
      }

      printHtmlHead(appReq);
      out.println("<form action=\"ReportDefinitionEditServlet\" method=\"POST\">");
      if (reportProfile.getProfileId() > 0) {
        out.println("  <input type=\"hidden\" name=\"profileId\" value=\""
            + reportProfile.getProfileId() + "\">");
      }
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\">Report Definition</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\"><textarea name=\"definition\" cols=\"80\" rows=\"60\">"
          + (reportProfile.getDefinition() == null ? "" : new String(reportProfile.getDefinition()))
          + "</textarea></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");

      printHtmlFoot(appReq);

    } finally {
      appReq.close();
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
