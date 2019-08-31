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
import org.openimmunizationsoftware.pt.model.ContactEvent;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectContactServlet extends ClientServlet {

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
      int projectContactId = Integer.parseInt(request.getParameter("projectContactId"));
      Session dataSession = getDataSession(session);
      Query query = dataSession.createQuery("from ProjectContact where contactId = ? ");
      query.setParameter(0, projectContactId);
      ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
      String action = request.getParameter("action");
      if (action != null) {
        if (action.equals("RemoveContact")) {
          int projectId = Integer.parseInt(request.getParameter("projectId"));
          ProjectServlet.removeContact(projectId, dataSession, projectContactId);
        } else if (action.equals("AddContact")) {
          int projectId = Integer.parseInt(request.getParameter("projectId"));
          ProjectServlet.assignContact(projectId, dataSession, projectContactId);
        }
      }

      printHtmlHead(out, "Contacts", request);

      out.println("<div class=\"main\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <th class=\"title\" colspan=\"2\"><span class=\"right\"><font size=\"-1\"><a href=\"ProjectContactEditServlet?projectContactId="
              + projectContactId
              + "\" class=\"box\">Edit</a></font></span>Contact Information</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <td class=\"boxed\">" + projectContact.getName() + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Organization</th>");
      out.println(
          "    <td class=\"boxed\">" + nbsp(projectContact.getOrganizationName()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Department</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getDepartmentName()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Position</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getPositionTitle()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Email</th>");
      out.println("    <td class=\"boxed\"><a href=\"mailto:" + projectContact.getEmail()
          + "\" class=\"button\">" + nbsp(projectContact.getEmail()) + "</a>");
      out.println("</td>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Cell</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getNumberCell()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Fax</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getNumberFax()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Phone</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getNumberPhone()) + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Time Zone</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getTimeZone()) + "</td>");
      out.println("  </tr>");
      if (projectContact.getAddressStreet1() != null
          && !projectContact.getAddressStreet1().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Address</th>");
        out.println("    <td class=\"boxed\">" + projectContact.getAddressStreet1());
        if (!projectContact.getAddressStreet2().equals("")) {
          out.println("    <br>" + projectContact.getAddressStreet2());
        }
        if (projectContact.getAddressCity().equals("")) {
          out.println("    <br>" + projectContact.getAddressState());
        } else {
          out.println("    <br>" + projectContact.getAddressCity() + ", "
              + projectContact.getAddressState());
        }
        if (!projectContact.getAddressCountry().equals("")) {
          out.println("    <br>" + projectContact.getAddressCountry());
        }
        out.println("</td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Other Info</th>");
      out.println("    <td class=\"boxed\">" + nbsp(projectContact.getContactInfo()) + "</td>");
      out.println("  </tr>");
      query = dataSession.createQuery("from ContactEvent where projectContact = ?");
      query.setParameter(0, projectContact);
      List<ContactEvent> contactEventList = query.list();
      SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
      for (ContactEvent contactEvent : contactEventList) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">" + contactEvent.getEventTypeLabel() + "</th>");
        out.println("    <td class=\"boxed\">" + sdf.format(contactEvent.getEventDate()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("<h2>Assigned Projects</h2>");

      query = dataSession.createQuery("from ProjectContactAssigned where id.contactId = ?");
      query.setParameter(0, projectContactId);
      List<ProjectContactAssigned> projectContactAssignedList = query.list();
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Phase</th>");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("  </tr>");
      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
        Project project =
            (Project) dataSession.get(Project.class, projectContactAssigned.getId().getProjectId());
        if (project != null) {
          ProjectsServlet.loadProjectsObject(dataSession, project);
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">"
              + (project.getProjectClient() != null ? project.getProjectClient().getClientName()
                  : "")
              + "</td>");
          out.println(
              "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                  + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
          out.println("    <td class=\"boxed\">"
              + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "")
              + "</td>");
          out.println("    <td class=\"boxed\">");
          out.println(
              "      <font size=\"-1\"><a href=\"ProjectContactServlet?action=RemoveContact&projectId="
                  + project.getProjectId() + "&projectContactId=" + projectContact.getContactId()
                  + "\" class=\"box\">Remove</a></font>");
          out.println("    </td>");
          out.println("  </tr>");
        }
      }
      out.println("</table>");

      out.println("<h2>Other Projects</h2>");

      List<Project> projectSelectedList =
          (List<Project>) session.getAttribute(SESSION_VAR_PROJECT_SELECTED_LIST);
      query = dataSession
          .createQuery("from ProjectContactAssigned where id.projectId = ? and id.contactId = ?");

      if (projectSelectedList != null) {
        out.println("<table class=\"boxed\">");
        out.println("  <tr>");
        out.println("    <th class=\"title\" colspan=\"4\">Previously Selected</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Category</th>");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Phase</th>");
        out.println("    <th class=\"boxed\">Action</th>");
        out.println("  </tr>");

        for (Project project : projectSelectedList) {
          query.setParameter(0, project.getProjectId());
          query.setParameter(1, projectContact.getContactId());
          List<ProjectContactAssigned> assigned = query.list();
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">"
              + (project.getProjectClient() != null ? project.getProjectClient().getClientName()
                  : "")
              + "</td>");
          out.println(
              "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                  + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
          out.println("    <td class=\"boxed\">"
              + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "")
              + "</td>");
          out.println("    <td class=\"boxed\">");
          if (assigned.size() == 0) {
            out.println(
                "      <font size=\"-1\"><a href=\"ProjectContactServlet?action=AddContact&projectId="
                    + project.getProjectId() + "&projectContactId=" + projectContact.getContactId()
                    + "\" class=\"box\">Assign</a></font>");
          }
          out.println("    </td>");
          out.println("  </tr>");
        }
        out.println("</table>");
        out.println("<br/>");
      }

      List<Integer> projectIdList =
          (List<Integer>) session.getAttribute(SESSION_VAR_PROJECT_ID_LIST);
      if (projectIdList != null) {
        out.println("<table class=\"boxed\">");
        out.println("  <tr>");
        out.println("    <th class=\"title\" colspan=\"4\">Previously Searched</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Category</th>");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Phase</th>");
        out.println("    <th class=\"boxed\">Action</th>");
        out.println("  </tr>");

        for (int projectId : projectIdList) {
          Project project = (Project) dataSession.get(Project.class, projectId);
          ProjectsServlet.loadProjectsObject(dataSession, project);
          query.setParameter(0, project.getProjectId());
          query.setParameter(1, projectContact.getContactId());
          List<ProjectContactAssigned> assigned = query.list();
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">"
              + (project.getProjectClient() != null ? project.getProjectClient().getClientName()
                  : "")
              + "</td>");
          out.println(
              "    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId()
                  + "\" class=\"button\">" + project.getProjectName() + "</a></td>");
          out.println("    <td class=\"boxed\">"
              + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "")
              + "</td>");
          out.println("    <td class=\"boxed\">");
          if (assigned.size() == 0) {
            out.println(
                "      <font size=\"-1\"><a href=\"ProjectContactServlet?action=AddContact&projectId="
                    + project.getProjectId() + "&projectContactId=" + projectContact.getContactId()
                    + "\" class=\"box\">Assign</a></font>");
          }
          out.println("    </td>");
          out.println("  </tr>");
        }
        out.println("</table>");
      }

      out.println("</div>");
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
