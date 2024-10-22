/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillEntriesServlet extends ClientServlet {

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
      if (appReq.isLoggedOut() || appReq.isDependentWebUser()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getDateFormat();



      String billDateString = request.getParameter("billDate");
      Date billDate = null;
      if ((billDateString != null && billDateString.length() > 0)) {
        try {
          billDate = sdf.parse(billDateString);
        } catch (ParseException pe) {
          appReq.setMessageProblem("Unable to parse date: " + pe.getMessage());
        }
      } else {
        billDateString = sdf.format(new Date());
      }

      appReq.setTitle("Track");
      printHtmlHead(appReq);


      out.println("<form action=\"BillEntriesServlet\" method=\"GET\">");
      out.println("Date");
      out.println(
          "<input type=\"text\" name=\"billDate\" value=\"" + billDateString + "\" size=\"10\">");
      out.println("<input type=\"submit\" name=\"action\" value=\"Refresh\">");
      out.println("</form>");

      Query query;

      Calendar t = TimeTracker.createToday(webUser);
      if (billDate != null) {
        t.setTime(billDate);
      }
      Date today = t.getTime();
      t.add(Calendar.DAY_OF_MONTH, 1);
      Date tomorrow = t.getTime();
      query = dataSession.createQuery(
          "from BillEntry where username = ? and startTime >= ? and startTime < ? order by startTime");
      query.setParameter(0, webUser.getUsername());
      query.setParameter(1, today);
      query.setParameter(2, tomorrow);
      List<BillEntry> billEntryList = query.list();

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"7\">Bill Entries</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <th class=\"boxed\">Start</th>");
      out.println("    <th class=\"boxed\">End</th>");
      out.println("    <th class=\"boxed\">Time</th>");
      out.println("    <th class=\"boxed\">Bill</th>");
      out.println("  </tr>");
      SimpleDateFormat timeFormat = webUser.getDateFormat("h:mm aaa");
      for (BillEntry billEntry : billEntryList) {
        String categoryCode = billEntry.getCategoryCode();
        ProjectCategory projectCategory =
            TrackServlet.getClient(dataSession, categoryCode, billEntry.getProvider());
        Project project = (Project) dataSession.get(Project.class, billEntry.getProjectId());
        ProjectAction projectAction = billEntry.getAction();
        BillCode billCode = null;
        if (billEntry.getBillCode() != null) {
          billCode = (BillCode) dataSession.get(BillCode.class, billEntry.getBillCode());
        }

        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">"
            + (projectCategory != null ? projectCategory.getClientName() : "") + "</td>");
        if (project != null) {
          out.println(
              "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                  + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
        } else {
          out.println("    <td class=\"boxed\"></td>");
        }
        if (projectAction != null) {
          out.println(
              "    <td class=\"boxed\"><a href=\"ActionServlet?actionId=" + projectAction.getActionId()
                  + "\" class=\"button\">" + projectAction.getNextDescriptionForDisplay(null) + "</a></td>");
        } else {
          out.println("    <td class=\"boxed\"></td>");
        }
        out.println("    <td class=\"boxed\">" + (billCode != null ? billCode.getBillLabel() : "")
            + "</td>");
        out.println(
            "    <td class=\"boxed\">" + timeFormat.format(billEntry.getStartTime()) + "</td>");
        out.println(
            "    <td class=\"boxed\">" + timeFormat.format(billEntry.getEndTime()) + "</td>");
        out.println("    <td class=\"boxed\"><a href=\"BillEntryEditServlet?billId="
            + billEntry.getBillId() + "&billDate=" + billDateString + "\" class=\"button\">"
            + TimeTracker.formatTime(billEntry.getBillMins()) + "</a></td>");
        out.println("    <td class=\"boxed\">" + billEntry.getBillable() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table> ");
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


}
