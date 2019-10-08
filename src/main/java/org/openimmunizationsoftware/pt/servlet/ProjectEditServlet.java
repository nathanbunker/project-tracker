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
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectPhase;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ProjectReviewServlet.Interval;

/**
 * 
 * @author nathan
 */
public class ProjectEditServlet extends ClientServlet {

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
      printHtmlHead(out, "Projects", request);

      Project project = null;
      Session dataSession = getDataSession(session);
      ProjectContactAssigned projectContactAssignedForThisUser = null;
      if (request.getParameter("projectId") != null
          && !request.getParameter("projectId").equals("0")) {
        int projectId = Integer.parseInt(request.getParameter("projectId"));
        project = (Project) dataSession.get(Project.class, projectId);
        projectContactAssignedForThisUser =
            ProjectServlet.getProjectContactAssigned(webUser, dataSession, project);
      } else {
        project = new Project();
        project.setProvider(webUser.getProvider());
      }

      String action = request.getParameter("action");
      if (action != null) {
        String message = null;
        if (action.equals("Save")) {
          project.setDescription(trim(request.getParameter("description"), 1200));
          project.setCategoryCode(request.getParameter("categoryCode"));
          project.setPhaseCode(request.getParameter("phaseCode"));
          project.setProjectId(Integer.parseInt(request.getParameter("projectId")));
          project.setPriorityLevel(Integer.parseInt(request.getParameter("priorityLevel")));
          project.setSystemName(trim(request.getParameter("systemName"), 30));
          project.setVendorName(trim(request.getParameter("vendorName"), 45));
          project.setIisSubmissionCode(trim(request.getParameter("iisSubmissionCode"), 30));
          project.setIisFacilityId(trim(request.getParameter("iisFacilityId"), 30));
          project.setMedicalOrganization(trim(request.getParameter("medicalOrganization"), 60));
          project.setIisRegionCode(trim(request.getParameter("iisRegionCode"), 30));
          project.setUsername(webUser.getUsername());
          project.setProjectName(trim(request.getParameter("projectName"), 100));
          if (webUser.isTrackTime()) {
            project.setBillCode(request.getParameter("billCode"));
          }
          if (project.getProjectName().equals("")) {
            message = "Project name is required";
          }
        }
        if (message != null) {
          request.setAttribute(REQUEST_VAR_MESSAGE, message);
        } else {
          Transaction trans = dataSession.beginTransaction();
          try {
            dataSession.saveOrUpdate(project);
            if (projectContactAssignedForThisUser == null) {
              ProjectContactAssigned projectContactAssigned = new ProjectContactAssigned();
              projectContactAssigned.setId(new ProjectContactAssignedId());
              projectContactAssigned.getId().setContactId(webUser.getContactId());
              projectContactAssigned.getId().setProjectId(project.getProjectId());
              projectContactAssigned.setEmailAlert("Y");
              projectContactAssigned
                  .setUpdateDue(Integer.parseInt(request.getParameter("updateEvery")));
              dataSession.save(projectContactAssigned);
            } else {
              projectContactAssignedForThisUser
                  .setUpdateDue(Integer.parseInt(request.getParameter("updateEvery")));
              dataSession.update(projectContactAssignedForThisUser);
            }
          } finally {
            trans.commit();
          }
          response.sendRedirect("ProjectServlet?projectId=" + project.getProjectId());
          return;
        }
      }

      out.println("<form action=\"ProjectEditServlet\" method=\"POST\">");
      out.println(
          "<input type=\"hidden\" name=\"projectId\" value=\"" + project.getProjectId() + "\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"2\">Edit Project Information</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project Name</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"projectName\" value=\""
          + n(project.getProjectName()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <td class=\"boxed\"><select name=\"categoryCode\">");
      {
        Query query = dataSession.createQuery(
            "from ProjectCategory where provider = :provider order by sortOrder, clientName");
        query.setParameter("provider", webUser.getProvider());
        List<ProjectCategory> projectCategoryList = query.list();
        for (ProjectCategory projectCategory : projectCategoryList) {
          if (projectCategory.getCategoryCode().startsWith("PER-")) {
            if (!projectCategory.getCategoryCode().equals("PER-" + webUser.getContactId())) {
              continue;
            }
          }
          if (projectCategory.getCategoryCode().equals(project.getCategoryCode())) {
            out.println("      <option value=\"" + projectCategory.getCategoryCode() + "\" selected>"
                + projectCategory.getClientNameForDropdown() + "</option>");
          } else {
            out.println("      <option value=\"" + projectCategory.getCategoryCode() + "\">"
                + projectCategory.getClientNameForDropdown() + "</option>");
          }
        }
        out.println("      </select>");
      }
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Priority Level</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"priorityLevel\" value=\""
          + project.getPriorityLevel() + "\" size=\"3\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Vendor</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"vendorName\" value=\""
          + n(project.getVendorName()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">System</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"systemName\" value=\""
          + n(project.getSystemName()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">IIS Submission Code</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"iisSubmissionCode\" value=\""
          + n(project.getIisSubmissionCode()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">IIS Facility Id</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"iisFacilityId\" value=\""
          + n(project.getIisFacilityId()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Medical Organization</th>");
      out.println(
          "    <td class=\"boxed\"><input type=\"text\" name=\"medicalOrganization\" value=\""
              + n(project.getMedicalOrganization()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">IIS Region Code</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"iisRegionCode\" value=\""
          + n(project.getIisRegionCode()) + "\" size=\"30\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Description</th>");
      out.println(
          "    <td class=\"boxed\"><textarea type=\"text\" name=\"description\" cols=\"30\" rows=\"5\">"
              + n(project.getDescription()) + "</textarea></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Phase</th>");
      out.println("    <td class=\"boxed\"><select name=\"phaseCode\">");
      {
        Query query = dataSession.createQuery("from ProjectPhase");
        List<ProjectPhase> projectPhaseList = query.list();
        for (ProjectPhase projectPhase : projectPhaseList) {
          if (projectPhase.getPhaseCode().equals(project.getPhaseCode())) {
            out.println("      <option value=\"" + projectPhase.getPhaseCode() + "\" selected>"
                + projectPhase.getPhaseLabel() + "</option>");
          } else {
            out.println("      <option value=\"" + projectPhase.getPhaseCode() + "\">"
                + projectPhase.getPhaseLabel() + "</option>");
          }
        }
        out.println("      </select>");
      }
      out.println("    </td>");
      out.println("  </tr>");
      if (webUser.isTrackTime()) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Bill Code</th>");
        out.println("    <td class=\"boxed\"><select name=\"billCode\">");
        Query query = dataSession.createQuery(
            "from BillCode where provider = :provider and visible = 'Y' order by billLabel");
        query.setParameter("provider", webUser.getProvider());
        List<BillCode> billCodeList = query.list();
        for (BillCode billCode : billCodeList) {
          if (billCode.getBillCode().equals(project.getBillCode())) {
            out.println("      <option value=\"" + billCode.getBillCode() + "\" selected>"
                + billCode.getBillLabel() + "</option>");
          } else {
            out.println("      <option value=\"" + billCode.getBillCode() + "\">"
                + billCode.getBillLabel() + "</option>");
          }
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
      }
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Update Every</th>");
        out.println("    <td class=\"boxed\"><select name=\"updateEvery\">");
        out.println("      <option value=\"0\">none</option>");
        boolean found = false;
        if (projectContactAssignedForThisUser != null
            && projectContactAssignedForThisUser.getUpdateDue() == 0) {
          found = true;
        }
        for (Interval interval : Interval.values()) {
          if (!found && projectContactAssignedForThisUser != null
              && projectContactAssignedForThisUser.getUpdateDue() <= interval.getDays()) {
            out.println("      <option value=\"" + interval.getDays() + "\" selected>"
                + interval.getDescription() + "</option>");
            found = true;
          } else {
            out.println("      <option value=\"" + interval.getDays() + "\">"
                + interval.getDescription() + "</option>");
          }
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
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
