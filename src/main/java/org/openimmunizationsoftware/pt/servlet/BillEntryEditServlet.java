/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillEntryEditServlet extends ClientServlet {

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
      if (appReq.isLoggedOut() || appReq.isDependentWebUser()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      SimpleDateFormat sdf = webUser.getTimeFormat();

      int billId = Integer.parseInt(request.getParameter("billId"));
      BillEntry billEntry = (BillEntry) dataSession.get(BillEntry.class, billId);
      TimeTracker timeTracker = appReq.getTimeTracker();
      String billDate = request.getParameter("billDate");

      ProjectAction projectAction = billEntry.getAction();
      if (projectAction != null) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        projectAction.setContact(
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
      }

      if (action != null) {
        String message = null;
        if (action.equals("Save")) {
          int projectId = Integer.parseInt(request.getParameter("projectId"));
          if (billEntry.getProjectId() != projectId) {
            billEntry.setAction(null);
          }
          billEntry.setProjectId(projectId);
          billEntry.setCategoryCode(request.getParameter("categoryCode"));
          billEntry.setBillCode(request.getParameter("billCode"));
          try {
            billEntry.setStartTime(sdf.parse(request.getParameter("startTime")));
          } catch (ParseException pe) {
            message = "Unable to parse start time: " + pe.getMessage();
          }
          try {
            billEntry.setEndTime(sdf.parse(request.getParameter("endTime")));
          } catch (ParseException pe) {
            message = "Unable to parse end time: " + pe.getMessage();
          }
          billEntry.setBillable(request.getParameter("billable") != null ? "Y" : "N");
          billEntry.setBillCode(request.getParameter("billCode"));
          if (message != null) {
            appReq.setMessageProblem(message);
          } else {
            billEntry.setBillMins(TimeTracker.calculateMins(billEntry));
            Transaction trans = dataSession.beginTransaction();
            try {
              dataSession.update(billEntry);
            } finally {
              trans.commit();
            }
            timeTracker.init(webUser, dataSession);
            response.sendRedirect("BillEntriesServlet?billDate=" + billDate);
            return;
          }
        }
      }

      appReq.setTitle("Track");
      printHtmlHead(appReq);

      out.println("<form action=\"BillEntryEditServlet\" method=\"POST\">");
      out.println(
          " <input type=\"hidden\" name=\"billId\" value=\"" + billEntry.getBillId() + "\">");
      out.println(" <input type=\"hidden\" name=\"billDate\" value=\"" + billDate + "\">");

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"6\">Edit Bill Entries</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <td class=\"boxed\"><select name=\"categoryCode\">");
      Query query = dataSession.createQuery("from ProjectCategory where provider = :provider");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<ProjectCategory> projectCategoryList = query.list();
      for (ProjectCategory projectCategory : projectCategoryList) {
        if (projectCategory.getCategoryCode().equals(billEntry.getCategoryCode())) {
          out.println("      <option value=\"" + projectCategory.getCategoryCode() + "\" selected>"
              + projectCategory.getClientName() + "</option>");
        } else {
          out.println("      <option value=\"" + projectCategory.getCategoryCode() + "\">"
              + projectCategory.getClientName() + "</option>");
        }
      }
      out.println("    </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <td class=\"boxed\"><select name=\"projectId\">");
      query = dataSession.createQuery(
          "from Project where provider = :provider order by categoryCode, projectName");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<Project> projectList = query.list();
      for (Project project : projectList) {
        if (project.getProjectId() == billEntry.getProjectId()) {
          out.println("      <option value=\"" + project.getProjectId() + "\" selected>"
              + project.getCategoryCode() + " " + project.getProjectName() + "</option>");
        } else {
          out.println("      <option value=\"" + project.getProjectId() + "\">"
              + project.getCategoryCode() + " " + project.getProjectName() + "</option>");
        }
      }
      out.println("    </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Provider</th>");
      out.println("    <td class=\"boxed\"><select name=\"billCode\">");
      query = dataSession.createQuery(
          "from BillCode where provider = :provider and visible = 'Y' order by billLabel");
      query.setParameter("provider", webUser.getProvider());
      @SuppressWarnings("unchecked")
      List<BillCode> billCodeList = query.list();
      for (BillCode billCode : billCodeList) {
        if (billCode.getBillCode().equals(billEntry.getBillCode())) {
          out.println("      <option value=\"" + billCode.getBillCode() + "\" selected>"
              + billCode.getBillLabel() + "</option>");
        } else {
          out.println("      <option value=\"" + billCode.getBillCode() + "\">"
              + billCode.getBillLabel() + "</option>");
        }
      }
      out.println("    </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("    <td class=\"boxed\">"
          + (billEntry.getAction() == null ? "" : billEntry.getAction().getNextDescriptionForDisplay(null)) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Start Time</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"startTime\" value=\""
          + sdf.format(billEntry.getStartTime()) + "\" size=\"20\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">End Time</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"endTime\" value=\""
          + sdf.format(billEntry.getEndTime()) + "\" size=\"20\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Time</th>");
      out.println(
          "    <td class=\"boxed\">" + TimeTracker.formatTime(billEntry.getBillMins()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Billable</th>");
      out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"billable\" value=\"Y\""
          + (billEntry.getBillable().equals("Y") ? " checked" : "") + "></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
      out.println("  </tr>");
      out.println("</table> ");
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

}
