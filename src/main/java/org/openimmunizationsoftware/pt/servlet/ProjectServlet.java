/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectServlet extends ClientServlet {

  public static final String PARAM_PROJECT_ID = "projectId";

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
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();

      int projectId = Integer.parseInt(request.getParameter(PARAM_PROJECT_ID));

      Project project = setupProject(dataSession, projectId);
      appReq.setProjectSelected(project);
      appReq.setProject(project);

      List<Project> projectSelectedList = setupProjectList(appReq, project);

      TimeTracker timeTracker = appReq.getTimeTracker();

      ProjectContactAssigned projectContactAssignedForThisUser = getProjectContactAssigned(webUser, dataSession,
          project);

      if (action != null) {
        if (action.equals("RemoveContact")) {
          int contactId = Integer.parseInt(request.getParameter("contactId"));
          removeContact(projectId, dataSession, contactId);
        } else if (action.equals("AddContact")) {
          int contactId = Integer.parseInt(request.getParameter("contactId"));
          assignContact(projectId, dataSession, contactId);
        } else if (action.equals("StartTimer")) {
          if (timeTracker != null) {
            if (webUser.getParentWebUser() != null) {
              Project parentProject = appReq.getParentProject();
              if (parentProject != null) {
                timeTracker.startClock(parentProject, null, dataSession);
              }
            } else {
              timeTracker.startClock(project, null, dataSession);
            }
          }
        }
      } else {
        if (timeTracker != null) {
          if (webUser.getParentWebUser() != null) {
            Project parentProject = appReq.getParentProject();
            if (parentProject != null) {
              timeTracker.update(parentProject, null, dataSession);
            }
          } else {
            timeTracker.update(project, null, dataSession);
          }
        }
      }

      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      out.println("<div class=\"main\">");

      out.println("<div id=\"projectNavigation\">");
      out.println("<table class=\"boxed-fill\" >");
      out.println("  <tr>");
      out.println("    <th class=\"title\">");
      List<Integer> projectIdList = appReq.getProjectIdList();
      if (projectIdList != null && projectIdList.size() > 1) {
        int pos = 0;
        for (int i = 0; i < projectIdList.size(); i++) {
          int id = projectIdList.get(i);

          if (id == projectId) {
            pos = i;
            break;
          }
        }
        int prev = 0;
        int next = 0;
        if (pos > 0) {
          prev = projectIdList.get(pos - 1);
        }
        if ((pos + 1) < projectIdList.size()) {
          next = projectIdList.get(pos + 1);
        }
        out.println("<div class=\"right\">");
        if (prev != 0) {
          out.println("    <font size=\"-1\"><a href=\"ProjectServlet?projectId=" + prev
              + "\" class=\"box\">Prev</a></font>");
        }
        out.println((pos + 1) + " of " + projectIdList.size());
        if (next != 0) {
          out.println("    <font size=\"-1\"><a href=\"ProjectServlet?projectId=" + next
              + "\" class=\"box\">Next</a></font>");
        }
        out.println("</div>");
      }
      out.println("    Navigate</th>");
      out.println("  </tr>");
      boolean first = true;
      for (Project p : projectSelectedList) {
        if (first) {
          first = false;
          continue;
        }
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + p.getProjectId()
            + "\" class=\"button\">" + p.getProjectName() + "</a></td>");
        out.println("  </tr>");
      }

      out.println("</table>");
      out.println("<br/>");
      out.println("</div>");

      out.println("<div id=\"projectInfo\">");
      out.println("<table class=\"boxed-fill\">");
      out.println("  <tr>");
      out.println(
          "    <th class=\"title\" colspan=\"2\"><span class=\"right\"><font size=\"-1\"><a href=\"ProjectEditServlet?projectId="
              + projectId + "\" class=\"box\">Edit</a></font></span>Project Information</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project Name</th>");
      out.println("    <td class=\"boxed\">" + project.getProjectName() + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <td class=\"boxed\">"
          + (project.getProjectCategory() != null ? project.getProjectCategory().getClientName()
              : "")
          + "</td>");
      out.println("  </tr>");
      if (project.getDescription() != null && !project.getDescription().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Description</th>");
        out.println("    <td class=\"boxed\">" + addBreaks(project.getDescription()) + "</td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Phase</th>");
      out.println("    <td class=\"boxed\">"
          + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "")
          + "</td>");
      out.println("  </tr>");
      if (projectContactAssignedForThisUser != null) {
        if (projectContactAssignedForThisUser.getUpdateDue() > 0) {
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Update Every</th>");
          out.println("    <td class=\"boxed\">"
              + ProjectReviewServlet.makeLabel(projectContactAssignedForThisUser.getUpdateDue())
              + "</td>");
          out.println("  </tr>");
        }
      }
      out.println("</table>");
      out.println("</div>");

      out.println("<br>");

      out.println("<table class=\"boxed-fill\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"5\">Contacts Assigned</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <th class=\"boxed\">Organization</th>");
      out.println("    <th class=\"boxed\">Phone</th>");
      out.println("    <th class=\"boxed\">Actions</th>");
      out.println("  </tr>");

      List<ProjectContactAssigned> projectContactAssignedList = getProjectContactAssignedList(dataSession, projectId);
      Collections.sort(projectContactAssignedList, new Comparator<ProjectContactAssigned>() {
        public int compare(ProjectContactAssigned arg0, ProjectContactAssigned arg1) {
          if (arg0.getProjectContact().getNameFirst()
              .equals(arg1.getProjectContact().getNameFirst())) {
            return arg0.getProjectContact().getNameLast()
                .compareTo(arg1.getProjectContact().getNameLast());
          }
          return arg0.getProjectContact().getNameFirst()
              .compareTo(arg1.getProjectContact().getNameFirst());
        }
      });
      project.setProjectContactAssignedList(projectContactAssignedList);

      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
        ProjectContact projectContact = projectContactAssigned.getProjectContact();
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"ProjectContactServlet?projectContactId="
            + projectContact.getContactId() + "\" class=\"button\">" + projectContact.getName()
            + "</a></td>");
        out.println("    <td class=\"boxed\">" + n(projectContact.getOrganizationName()) + "</td>");
        out.println("    <td class=\"boxed\">" + n(projectContact.getPhoneNumber()) + "</td>");
        out.println("    <td class=\"boxed\">");
        if (projectContact.getEmailAddress() != null
            && !projectContact.getEmailAddress().equals("")) {
          // new URI("mailto", )
          String projectName = project.getProjectName();
          out.println(" <font size=\"-1\"><a href=\"mailto:" + projectContact.getEmailAddress()
              + "?subject=" + encode(projectName + " Project")
              + "\" class=\"box\">Email</a></font>");
        }
        out.println(" <font size=\"-1\"><a href=\"ProjectServlet?action=RemoveContact&projectId="
            + projectId + "&contactId=" + projectContact.getContactId()
            + "\" class=\"box\">Remove</a></font>");
        out.println("</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("</div>");

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  public static List<ProjectContactAssigned> getProjectContactAssignedList(Session dataSession,
      int projectId) {
    Query query = dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
    query.setParameter(0, projectId);
    @SuppressWarnings("unchecked")
    List<ProjectContactAssigned> projectContactAssignedList = query.list();
    return projectContactAssignedList;
  }

  public static List<ProjectContact> getProjectContactList(Session dataSession, Project project,
      List<ProjectContactAssigned> projectContactAssignedList) {
    Query query;
    List<ProjectContact> projectContactList = new ArrayList<ProjectContact>();
    for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
      query = dataSession.createQuery("from ProjectContact where contactId = ?");
      query.setParameter(0, projectContactAssigned.getId().getContactId());
      @SuppressWarnings("unchecked")
      List<ProjectContact> projectContactResultList = query.list();
      ProjectContact projectContact = projectContactResultList.get(0);
      projectContactList.add(projectContact);
      projectContactAssigned.setProjectContact(projectContact);
      projectContactAssigned.setProject(project);
    }
    return projectContactList;
  }

  protected static List<Project> setupProjectList(AppReq appReq, Project project) {
    List<Project> projectSelectedListOld = appReq.getProjectSelectedList();

    if (projectSelectedListOld == null) {
      projectSelectedListOld = new ArrayList<Project>();
      appReq.setProjectSelectedList(projectSelectedListOld);
    }
    List<Project> projectSelectedList;
    if (projectSelectedListOld.size() > 0
        && projectSelectedListOld.get(0).getProjectId() == project.getProjectId()) {
      projectSelectedList = projectSelectedListOld;
    } else {
      projectSelectedList = new ArrayList<Project>();
      projectSelectedList.add(project);
      int pos = 0;

      for (ListIterator<Project> it = projectSelectedListOld.listIterator(); it.hasNext()
          && pos < 7;) {
        Project projectSelected = it.next();
        if (projectSelected.getProjectId() != project.getProjectId()) {
          projectSelectedList.add(projectSelected);
        }
        pos++;
      }
      appReq.setProjectSelectedList(projectSelectedList);
    }
    return projectSelectedList;
  }

  public static Project setupProject(Session dataSession, int projectId) {
    Project project;
    {
      Query query = dataSession.createQuery("from Project where projectId = ? ");
      query.setParameter(0, projectId);
      @SuppressWarnings("unchecked")
      List<Project> projectList = query.list();
      project = projectList.get(0);
      ProjectsServlet.loadProjectsObject(dataSession, project);
    }
    return project;
  }

  protected static ProjectContactAssigned getProjectContactAssigned(WebUser webUser,
      Session dataSession, Project project) {
    Query query;
    ProjectContactAssigned projectContactAssignedForThisUser = null;
    query = dataSession
        .createQuery("from ProjectContactAssigned where id.contactId = ? and id.projectId = ?");
    query.setParameter(0, webUser.getContactId());
    query.setParameter(1, project.getProjectId());
    @SuppressWarnings("unchecked")
    List<ProjectContactAssigned> projectContactAssignedForThisUserList = query.list();
    if (projectContactAssignedForThisUserList.size() > 0) {
      projectContactAssignedForThisUser = projectContactAssignedForThisUserList.get(0);
    }
    return projectContactAssignedForThisUser;
  }

  private String encode(String s) throws UnsupportedEncodingException {
    s = URLEncoder.encode(s, "UTF-8");
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      if (c == '+') {
        sb.append("%20");
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  protected static void removeContact(int projectId, Session dataSession, int contactId) {
    Query query;
    query = dataSession
        .createQuery("from ProjectContactAssigned where id.projectId = ? and id.contactId =?");
    query.setParameter(0, projectId);
    query.setParameter(1, contactId);
    @SuppressWarnings("unchecked")
    List<ProjectContactAssigned> projectContactAssignedList = query.list();
    if (projectContactAssignedList.size() > 0) {
      Transaction trans = dataSession.beginTransaction();
      try {
        dataSession.delete(projectContactAssignedList.get(0));
      } finally {
        trans.commit();
      }
    }
  }

  protected static void assignContact(int projectId, Session dataSession, int contactId) {
    Query query;
    query = dataSession
        .createQuery("from ProjectContactAssigned where id.projectId = ? and id.contactId = ?");
    query.setParameter(0, projectId);
    query.setParameter(1, contactId);
    @SuppressWarnings("unchecked")
    List<ProjectContactAssigned> projectContactAssignedList = query.list();
    if (projectContactAssignedList.size() == 0) {
      Transaction trans = dataSession.beginTransaction();
      try {
        ProjectContactAssigned projectContactAssigned = new ProjectContactAssigned();
        projectContactAssigned.setId(new ProjectContactAssignedId());
        projectContactAssigned.getId().setProjectId(projectId);
        projectContactAssigned.getId().setContactId(contactId);
        projectContactAssigned.setEmailAlert("Y");
        dataSession.save(projectContactAssigned);
      } finally {
        trans.commit();
      }
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
