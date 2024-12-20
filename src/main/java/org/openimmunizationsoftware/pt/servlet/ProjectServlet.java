/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.PrioritySpecial;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectServlet extends ClientServlet {

  private static final boolean ENABLE_TODO_SERVLET = false;
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_ACTION_ID = "actionId";

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
  @SuppressWarnings("unchecked")
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

      int projectId = Integer.parseInt(request.getParameter(PARAM_PROJECT_ID));

      Project project = setupProject(dataSession, projectId);
      appReq.setProjectSelected(project);
      appReq.setProject(project);
      appReq.setCompletingAction(null);
      appReq.setProjectActionSelected(null);

      List<Project> projectSelectedList = setupProjectList(appReq, project);

      TimeTracker timeTracker = appReq.getTimeTracker();

      ProjectContactAssigned projectContactAssignedForThisUser = getProjectContactAssigned(webUser, dataSession,
          project);

      ProjectAction projectAction = null;
      String emailBody = null;
      if (action != null) {
        if (action.equals("Save")) {
          emailBody = saveProjectAction(appReq, project, emailBody);
        } else if (action.equals("RemoveContact")) {
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
      } else if (appReq.getRequest().getParameter(PARAM_ACTION_ID) != null) {
        int actionId = Integer.parseInt(appReq.getRequest().getParameter(PARAM_ACTION_ID));
        projectAction = (ProjectAction) dataSession.get(ProjectAction.class, actionId);
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
      printOutEmailSent(out, emailBody);

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
      if (project.getVendorName() != null && !project.getVendorName().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Vendor</th>");
        out.println("    <td class=\"boxed\">" + project.getVendorName() + "</td>");
        out.println("  </tr>");
      }
      if (project.getSystemName() != null && !project.getSystemName().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">System</th>");
        out.println("    <td class=\"boxed\">" + n(project.getSystemName()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getDescription() != null && !project.getDescription().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Description</th>");
        out.println("    <td class=\"boxed\">" + addBreaks(project.getDescription()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getIisSubmissionCode() != null && !project.getIisSubmissionCode().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">IIS Sub Code</th>");
        out.println("    <td class=\"boxed\">" + n(project.getIisSubmissionCode()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getIisFacilityId() != null && !project.getIisFacilityId().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">IIS Facility Id</th>");
        out.println("    <td class=\"boxed\">" + n(project.getIisFacilityId()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getMedicalOrganization() != null
          && !project.getMedicalOrganization().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Medical Org</th>");
        out.println("    <td class=\"boxed\">" + n(project.getMedicalOrganization()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getIisRegionCode() != null && !project.getIisRegionCode().equals("")) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">IIS Region Code</th>");
        out.println("    <td class=\"boxed\">" + n(project.getIisRegionCode()) + "</td>");
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
      List<ProjectContact> projectContactList = getProjectContactList(dataSession, project, projectContactAssignedList);
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

      out.println("<div id=\"takeAction\">");
      out.println("<form name=\"projectAction" + projectId
          + "\" method=\"post\" action=\"ProjectServlet\" id=\"saveProjectActionForm" + projectId
          + "\">");
      printProjectUpdateForm(appReq, projectId, projectContactList, projectAction, null);
      out.println("</form>");
      out.println("</div>");

      printActionsTaken(dataSession, out, sdf, projectId);

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  protected static void printOutEmailSent(PrintWriter out, String emailBody) {
    if (emailBody != null) {
      out.println("<table class=\"boxed-fill\"");
      out.println("  <tr>");
      out.println("   <th class=\"title\">Email Sent</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("   <td class=\"boxed\">");
      out.println(emailBody);
      out.println("   </td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("<br>");
    }
  }

  private void printActionsTaken(Session dataSession, PrintWriter out, SimpleDateFormat sdf,
      int projectId) {
    out.println("<div class=\"main\">");
    out.println("<h2>Actions Taken</h2>");
    out.println("<table class=\"boxed-fill\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Date &amp; Time</th>");
    out.println("    <th class=\"boxed\">Name</th>");
    out.println("    <th class=\"boxed\">Action</th>");
    out.println("  </tr>");
    List<ProjectAction> projectActionList = getProjectActionsTakenList(dataSession, projectId);
    for (ProjectAction pa : projectActionList) {
      ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class, pa.getContactId());
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\">" + sdf.format(pa.getActionDate()) + "</td>");
      out.println("    <td class=\"boxed\">" + projectContact.getNameFirst() + " "
          + projectContact.getNameLast() + "</td>");
      out.println("    <td class=\"boxed\">" + nbsp(pa.getActionDescription()));
      out.println("  </tr>");
    }
    out.println("</table>");
    out.println("</div>");
  }

  protected static List<ProjectAction> getProjectActionsTakenList(Session dataSession, Project project) {
    return getProjectActionsTakenList(dataSession, project.getProjectId());
  }

  private static List<ProjectAction> getProjectActionsTakenList(Session dataSession, int projectId) {
    List<ProjectAction> projectActionList;
    Query query = dataSession.createQuery(
        "from ProjectAction where projectId = ? and actionDescription <> '' order by actionDate desc");
    query.setParameter(0, projectId);
    projectActionList = query.list();
    return projectActionList;
  }

  public static String saveProjectAction(AppReq appReq, Project project, String emailBody) {

    HttpServletRequest request = appReq.getRequest();
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    SimpleDateFormat sdf = webUser.getDateFormat();
    ProjectAction projectAction;
    if (appReq.getRequest().getParameter(PARAM_ACTION_ID) != null) {
      int actionId = Integer.parseInt(appReq.getRequest().getParameter(PARAM_ACTION_ID));
      projectAction = (ProjectAction) dataSession.get(ProjectAction.class, actionId);
    } else {
      projectAction = new ProjectAction();
      projectAction.setProjectId(project.getProjectId());
    }
    projectAction.setContactId(webUser.getContactId());
    projectAction.setContact(webUser.getProjectContact());
    Date actionDate = new Date();
    int nextTimeEstimate = 0;
    if (request.getParameter("nextTimeEstimate") != null) {
      try {
        nextTimeEstimate = Integer.parseInt(request.getParameter("nextTimeEstimate"));
      } catch (NumberFormatException nfe) {
        nextTimeEstimate = 0;
      }
    }
    projectAction.setActionDate(actionDate);
    projectAction.setActionDescription(trim(request.getParameter("actionDescription"), 12000));
    projectAction.setNextDescription(trim(request.getParameter("nextDescription"), 1200));
    if (nextTimeEstimate > 0) {
      projectAction.setNextTimeEstimate(nextTimeEstimate);
    }
    projectAction.setNextActionId(0);

    projectAction.setNextDue(parseDate(appReq, request.getParameter("nextDue")));
    projectAction.setNextDeadline(parseDate(appReq, request.getParameter("nextDeadline")));
    String linkUrl = request.getParameter("linkUrl");
    if (linkUrl == null || linkUrl.equals("")) {
      projectAction.setLinkUrl(null);
    } else {
      projectAction.setLinkUrl(linkUrl);
    }
    String templateTypeString = request.getParameter("templateType");
    if (templateTypeString == null || templateTypeString.equals("")) {
      projectAction.setTemplateType(null);
    } else {
      projectAction.setTemplateType(TemplateType.getTemplateType(templateTypeString));
    }
    String prioritySpecialString = request.getParameter("prioritySpecial");
    if (prioritySpecialString == null || prioritySpecialString.equals("")) {
      projectAction.setPrioritySpecial(null);
    } else {
      projectAction.setPrioritySpecial(PrioritySpecial.getPrioritySpecial(prioritySpecialString));
    }

    String nextActionType = request.getParameter("nextActionType");
    projectAction.setNextActionType(nextActionType);
    int priorityLevel = autoSetPriority(project, nextActionType);
    projectAction.setPriorityLevel(priorityLevel);
    String nextContactIdString = request.getParameter("nextContactId");
    if (nextContactIdString != null && nextContactIdString.length() > 0) {
      projectAction.setNextContactId(Integer.parseInt(nextContactIdString));
      projectAction.setNextProjectContact(
          (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getNextContactId()));
    }
    projectAction.setProvider(webUser.getProvider());
    if (projectAction.hasNextDescription()) {
      if (projectAction.hasNextDue()) {
        projectAction.setNextActionStatus(ProjectNextActionStatus.READY);
      } else {
        projectAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
      }
    }
    String message = appReq.getMessageProblem();
    if (message == null) {
      String[] completed = request.getParameterValues("completed");

      Transaction trans = dataSession.beginTransaction();
      try {
        String nextFeedback = null;
        dataSession.saveOrUpdate(projectAction);
        if (completed != null && completed.length > 0) {
          for (String c : completed) {
            ProjectAction paCompleted = (ProjectAction) dataSession.get(ProjectAction.class, Integer.parseInt(c));
            paCompleted.setNextActionId(projectAction.getActionId());
            if (projectAction.hasActionDescription()) {
              paCompleted.setNextActionStatus(ProjectNextActionStatus.COMPLETED);
            } else {
              paCompleted.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
            }
            dataSession.update(paCompleted);
            if (nextFeedback == null && paCompleted.getNextFeedback() != null) {
              nextFeedback = paCompleted.getNextFeedback();
            }
          }
        }
        if (nextFeedback != null) {
          projectAction.setNextFeedback(nextFeedback);
          dataSession.update(projectAction);
        }
      } finally {
        trans.commit();
      }
      boolean userAssignedToProject = false;
      Query query = dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
      query.setParameter(0, project.getProjectId());
      List<ProjectContactAssigned> projectContactAssignedList = query.list();
      List<ProjectContact> sendEmailToList = new ArrayList<ProjectContact>();
      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
        if (projectContactAssigned.getProjectContact() == webUser.getProjectContact()) {
          userAssignedToProject = true;
        }
        if (request
            .getParameter("sendEmailTo" + projectContactAssigned.getId().getContactId()) != null) {
          query = dataSession.createQuery("from ProjectContact where contactId = ?");
          query.setParameter(0, projectContactAssigned.getId().getContactId());
          ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
          sendEmailToList.add(projectContact);
        }
      }
      if (!userAssignedToProject) {
        assignContact(project.getProjectId(), dataSession, webUser.getContactId());
      }

      if (sendEmailToList.size() > 0) {

        StringBuilder msg = new StringBuilder();
        msg.append("<p><i>");
        msg.append(webUser.getProjectContact().getName());
        msg.append(" writes: </i>");
        msg.append(projectAction.getActionDescription());
        msg.append("</p>");
        if (projectAction.getNextDescription() != null
            && !projectAction.getNextDescription().equals("")) {
          msg.append("<p>");
          msg.append(projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()));
          if (projectAction.getNextDue() != null) {
            SimpleDateFormat sdf11 = webUser.getDateFormat();
            msg.append(" ");
            msg.append(sdf11.format(projectAction.getNextDue()));
          }
          msg.append("</p>");
        }
        if (sendEmailToList.size() == 1) {
          msg.append("<p>This update was sent to you from the Tracker. </p>");
        } else {
          msg.append("<p>This update was sent to you");
          for (ProjectContact pc : sendEmailToList) {
            msg.append(", ");
            msg.append(pc.getName());
          }
          msg.append(" from the Tracker</p>");
        }

        msg.append("</body></html>");
        try {
          MailManager mailManager = new MailManager(dataSession);
          emailBody = msg.toString();
          String emailMessage = "<html><head></head><body>" + emailBody + "</body></html>";
          for (ProjectContact pc : sendEmailToList) {
            mailManager.sendEmail(project.getProjectName() + " Project Update", emailMessage,
                pc.getEmailAddress());
          }
        } catch (Exception e) {
          message = "Unable to send email: " + e.getMessage();
          emailBody = null;
        }
      }

    } else {
      appReq.setMessageProblem(message);
    }
    return emailBody;
  }

  protected static int autoSetPriority(Project project, String nextActionType) {
    int priorityLevel = project.getPriorityLevel();
    if (nextActionType != null) {
      if (nextActionType.equals("T")) {
        priorityLevel += 1;
      } else if (nextActionType.equals("N") || nextActionType.equals("W")
          || nextActionType.equals("E") || nextActionType.equals("A")) {
        priorityLevel -= 1;
      }
    }
    return priorityLevel;
  }

  public static List<ProjectContactAssigned> getProjectContactAssignedList(Session dataSession,
      int projectId) {
    Query query = dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
    query.setParameter(0, projectId);
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
      ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
      projectContactList.add(projectContact);
      projectContactAssigned.setProjectContact(projectContact);
      projectContactAssigned.setProject(project);
    }
    return projectContactList;
  }

  public static void printProjectUpdateForm(AppReq appReq, int projectId,
      List<ProjectContact> projectContactList, ProjectAction projectAction, ProjectAction completingProjectAction) {
    HttpServletRequest request = appReq.getRequest();
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    PrintWriter out = appReq.getOut();
    Query query;

    query = dataSession.createQuery(
        "from ProjectAction where projectId = ? and nextDescription <> '' and nextActionId = 0 order by nextDue asc");
    query.setParameter(0, projectId);
    List<ProjectAction> projectActionList = query.list();
    List<ProjectAction> projectActionTemplateList = new ArrayList<ProjectAction>();
    List<ProjectAction> projectActionGoalList = new ArrayList<ProjectAction>();
    {
      Date today = new Date();
      for (Iterator<ProjectAction> it = projectActionList.iterator(); it.hasNext();) {
        ProjectAction pa = it.next();
        if (pa.getNextDue() == null || pa.getNextDue().after(today)) {
          if (pa.isTemplate()) {
            projectActionTemplateList.add(pa);
            it.remove();
          } else if (pa.isGoal()) {
            projectActionGoalList.add(pa);
            it.remove();
          }
        }
      }
    }

    String disabled = (projectAction == null ? " disabled" : "");
    String formName = "" + projectId;
    printOutScript(out, formName, webUser);
    out.println("<input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\">");
    if (projectAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_ACTION_ID + "\" value=\""
          + projectAction.getActionId() + "\">");
    }
    out.println("  <table class=\"boxed\">");
    out.println("    <tr>");
    out.println("      <th class=\"title\">To Do List</th>");
    out.println("    </tr>");
    out.println("    <td class=\"outside\">");
    printTodoList(projectId, webUser, dataSession, out, projectActionList, completingProjectAction);

    out.println("    </td>");
    out.println("  </tr>");
    out.println("    <td class=\"outside\">");

    out.println("      <table class=\"inside\" width=\"100%\">");
    out.println("        <tr>");
    out.println("          <th class=\"inside\">Action</th>");
    String actionDescription = "";
    if (projectAction != null) {
      actionDescription = projectAction.getActionDescription();
    } else if (completingProjectAction != null) {
      actionDescription = completingProjectAction.getNextSummary();
    }
    out.println(
        "          <td class=\"inside\"><textarea name=\"actionDescription\" rows=\"7\" onkeydown=\"resetRefresh()\">"
            + actionDescription
            + "</textarea></td>");
    out.println("        </tr>");
    out.println("        <script>");
    out.println("          function setNextAction" + projectId + "(nextActionDate)");
    out.println("          {");
    out.println(
        "            document.projectAction" + projectId + ".nextDue.value = nextActionDate;");
    out.println("            enableForm" + projectId + "(); ");
    out.println("          }");
    out.println("          function setNextDeadline" + projectId + "(nextDeadline)");
    out.println("          {");
    out.println(
        "            document.projectAction" + projectId + ".nextDeadline.value = nextDeadline;");
    out.println("          }");
    out.println("        </script>");
    out.println("      </table>");
    out.println("   </td>");
    out.println("  </tr>");
    out.println("  </table>");
    out.println("  </br>");
    out.println("  <table class=\"boxed\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\">What is next?</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"inside\">");
    SimpleDateFormat sdf1 = webUser.getDateFormat("MM/dd/yyyy hh:mm aaa");
    {
      sdf1 = webUser.getDateFormat();
      out.println("        <tr>");
      out.println("          <th class=\"inside\">When</th>");
      {
        String nextDue = projectAction == null || projectAction.getNextDue() == null
            ? request.getParameter("nextDue")
            : sdf1.format(projectAction.getNextDue());
        out.println(
            "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"nextDue\" size=\"10\" value=\""
                + n(nextDue) + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      }
      out.println("            <font size=\"-1\">");
      Calendar calendar = webUser.getCalendar();
      SimpleDateFormat day = webUser.getDateFormat("EEE");
      out.println("              <a href=\"javascript: void setNextAction" + projectId + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Today</a>");
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      out.println("              <a href=\"javascript: void setNextAction" + projectId + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 6; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextAction" + projectId + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextAction" + projectId + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
              + day.format(calendar.getTime()) + "</a>");

        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
          nextWeek = true;
        }
      }
      calendar.set(Calendar.MONTH, 11);
      calendar.set(Calendar.DAY_OF_MONTH, 31);
      out.println("              <a href=\"javascript: void setNextAction" + projectId + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">EOY</a>");
      out.println("</font>");

      out.println("          </td>");
      out.println("        </tr>");
    }
    out.println("        <tr>");
    out.println("          <th class=\"inside\">Action</th>");
    out.println("          <td class=\"inside\" colspan=\"3\">");
    out.println(
        "            I: <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
            + projectId + "('" + ProjectNextActionType.WILL + "');\" class=\"button\"> will</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.MIGHT + "');\" class=\"button\">might</a>, ");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.WILL_CONTACT + "');\" class=\"button\">will contact</a>, ");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.WILL_MEET + "');\" class=\"button\">will meet</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.WILL_REVIEW + "');\" class=\"button\">will review</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.WILL_DOCUMENT + "');\" class=\"button\">will document</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.WILL_FOLLOW_UP + "');\" class=\"button\">will follow up</a>");
    out.println("            </font><br/>");
    out.println("            I have: ");
    out.println("            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
        + projectId + "('" + ProjectNextActionType.COMMITTED_TO
        + "');\" class=\"button\">committed</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + projectId + "('"
        + ProjectNextActionType.GOAL + "');\" class=\"button\">set goal</a></font>");
    out.println("            I am:");
    out.println("            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
        + projectId + "('" + ProjectNextActionType.WAITING + "');\" class=\"button\">waiting</a>");
    out.println("            <br>");
    {
      String nextActionType = projectAction == null ? ProjectNextActionType.WILL : projectAction.getNextActionType();
      out.println("            <input type=\"hidden\" name=\"nextActionType\" value=\""
          + nextActionType + "\">");
      out.println("<script>");
      out.println("  window.addEventListener('load', function() { selectProjectActionType"
          + projectId + "('" + nextActionType + "'); }); ");
      out.println("</script>");
    }
    out.println("            </font>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("        <tr>");
    out.println("          <th class=\"inside\">What</th>");
    out.println("          <td class=\"inside\"> ");

    out.println(
        "            <input name=\"startSentance\" size=\"40\" value=\"I will:\"" + disabled + ">");
    out.println("          </td>");
    out.println("          <th class=\"inside\">Who</th>");
    out.println("          <td class=\"inside\"> ");
    out.println("              <select name=\"nextContactId\" onchange=\"selectProjectActionType"
        + projectId + "(form.nextActionType.value);\"" + disabled
        + "><option value=\"\">none</option>");
    String nextContactId = n(request.getParameter("nextContactId"));
    for (ProjectContact projectContact1 : projectContactList) {
      if (projectContact1.getContactId() != webUser.getProjectContact().getContactId()) {
        boolean selected = nextContactId.equals(Integer.toString(
            projectAction == null ? projectContact1.getContactId() : projectAction.getContactId()));
        out.println("                  <option value=\"" + projectContact1.getContactId() + "\""
            + (selected ? " selected" : "") + ">" + projectContact1.getName() + "</option>");
      }
    }
    out.println("            </select>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("        <tr>");
    out.println("          <th class=\"inside\"></th>");
    out.println("          <td class=\"inside\" colspan=\"3\"> ");
    out.println(
        "            <textarea name=\"nextDescription\" rows=\"2\" onkeydown=\"resetRefresh()\""
            + disabled + ">" + (projectAction == null ? "" : projectAction.getNextDescription())
            + "</textarea>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("        <tr>");
    out.println("          <th class=\"inside\">Time</th>");
    out.println("          <td class=\"inside\" colspan=\"3\">");
    out.println("            <input type=\"text\" name=\"nextTimeEstimate\" size=\"3\" value=\""
        + (projectAction == null ? "" : projectAction.getNextTimeEstimateMinsForDisplay())
        + "\" onkeydown=\"resetRefresh()\"" + disabled + "> mins ");
    out.println("            <font size=\"-1\">");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('5');\" class=\"button\"> 5m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('10');\" class=\"button\"> 10m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('20');\" class=\"button\"> 20m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('30');\" class=\"button\"> 30m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('40');\" class=\"button\"> 40m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('60');\" class=\"button\"> 60m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('70');\" class=\"button\"> 70m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('90');\" class=\"button\"> 90m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('120');\" class=\"button\"> 2h</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('240');\" class=\"button\"> 4h</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + projectId
        + "('360');\" class=\"button\"> 6h</a>");
    out.println("            </font> ");
    out.println("          </td>");
    out.println("        </tr>");
    {
      Calendar calendar = webUser.getCalendar();
      SimpleDateFormat day = webUser.getDateFormat("EEE");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Deadline</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"nextDeadline\" size=\"10\" value=\""
              + n(projectAction == null || projectAction.getNextDeadline() == null
                  ? request.getParameter("nextDeadline")
                  : sdf1.format(projectAction.getNextDeadline()))
              + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      out.println("            <font size=\"-1\">");
      sdf1 = webUser.getDateFormat();
      calendar.add(Calendar.DAY_OF_MONTH, 2);
      out.println("              <a href=\"javascript: void setNextDeadline" + projectId + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 7; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextDeadline" + projectId + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextDeadline" + projectId + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
              + day.format(calendar.getTime()) + "</a>");
        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
          nextWeek = true;
        }
      }
      out.println("</font>");
      out.println("          </td>");
      out.println("        </tr>");
    }
    {
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Link</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"linkUrl\" size=\"30\" value=\""
              + n(projectAction == null || projectAction.getLinkUrl() == null
                  ? request.getParameter("linkUrl")
                  : projectAction.getLinkUrl())
              + "\" onkeydown=\"resetRefresh()\"" + disabled + "></td>");
      out.println("        </tr>");
    }
    {
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Special</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\">");
      out.println("            Template: ");
      out.println("            <select name=\"templateType\" value=\""
          + n((projectAction == null || projectAction.getTemplateType() == null)
              ? request.getParameter("templateType")
              : projectAction.getTemplateType().getId())
          + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      // default empty option for no template
      out.println("             <option value=\"\">none</option>");
      for (TemplateType templateType : TemplateType.values()) {
        out.println("             <option value=\"" + templateType.getId() + "\""
            + (projectAction != null && projectAction.getTemplateType() == templateType ? " selected" : "")
            + ">" + templateType.getLabel() + "</option>");
      }
      out.println("            </select>");
      // now do Priority Special
      out.println("            Priority: ");
      out.println("           <select name=\"prioritySpecial\" value=\""
          + n((projectAction == null || projectAction.getPrioritySpecial() == null)
              ? request.getParameter("prioritySpecial")
              : projectAction.getPrioritySpecial().getId())
          + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      // default empty option for no template
      out.println("             <option value=\"\">none</option>");
      for (PrioritySpecial prioritySpecial : PrioritySpecial.values()) {
        out.println("             <option value=\"" + prioritySpecial.getId() + "\""
            + (projectAction != null && projectAction.getPrioritySpecial() == prioritySpecial ? " selected" : "")
            + ">" + prioritySpecial.getLabel() + "</option>");
      }
      out.println("            </select>");
      out.println("          </td>");
      out.println("        </tr>");
    }
    out.println("      </table>");
    out.println("    </td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th class=\"title\">Send Email Alert</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"inside\">");
    out.println("        <tr>");
    out.println("          <th class=\"inside\">Who</th>");
    out.println("          <td class=\"inside\">");
    for (ProjectContact projectContact1 : projectContactList) {
      out.println("          <span class=\"together\"><input type=\"checkbox\" name=\"sendEmailTo"
          + projectContact1.getContactId() + "\" value=\"Y\"/>");
      out.println("<font size=\"-1\"><a href=\"javascript: void clickForEmail" + projectId + "('"
          + projectContact1.getContactId() + "');\" class=\"button\">" + projectContact1.getName()
          + "</a></font></span>");
    }

    out.println("          </td>");
    out.println("        </tr>");
    out.println("      </table>");
    out.println("    </td>");
    out.println("  <tr>");
    out.println("    <th class=\"title\">Save Action</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td class=\"boxed-submit\">");
    out.println("      <input type=\"submit\" name=\"action\" value=\"Save\">");
    out.println("    </td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("<br/>");

    out.println("  <table class=\"boxed\">");
    if (projectActionTemplateList.size() > 0) {
      out.println("    <tr>");
      out.println("      <th class=\"title\">Templates</th>");
      out.println("    </tr>");
      out.println("    <td class=\"outside\">");
      printTemplatesOrGoals(webUser, projectId, dataSession, out, projectActionTemplateList);
      out.println("    </td>");
      out.println("  </tr>");
    }
    if (projectActionGoalList.size() > 0) {
      out.println("    <tr>");
      out.println("      <th class=\"title\">Goals for project</th>");
      out.println("    </tr>");
      out.println("    <td class=\"outside\">");
      printTemplatesOrGoals(webUser, projectId, dataSession, out, projectActionGoalList);
      out.println("    </td>");
      out.println("  </tr>");
    }
    out.println("</table>");

  }

  protected static void printTemplatesOrGoals(WebUser webUser, int projectId, Session dataSession,
      PrintWriter out, List<ProjectAction> projectActionGoalList) {
    out.println("<table class=\"inside\" width=\"100%\">");
    out.println("  <tr>");
    out.println("    <th class=\"inside\">Date</th>");
    out.println("    <th class=\"inside\">Time</th>");
    out.println("    <th class=\"inside\">To Do</th>");
    out.println("    <th class=\"inside\">Status</th>");
    out.println("    <th class=\"inside\">Comp</th>");
    out.println("  </tr>");
    SimpleDateFormat sdf11 = webUser.getDateFormat();
    for (ProjectAction pa : projectActionGoalList) {
      Date today = new Date();
      Calendar calendar1 = webUser.getCalendar();
      calendar1.add(Calendar.DAY_OF_MONTH, -1);
      Date yesterday = calendar1.getTime();
      String editActionLink = "<a href=\"ProjectServlet?" + PARAM_PROJECT_ID + "=" + projectId + "&"
          + PARAM_ACTION_ID + "=" + pa.getActionId() + "\" class=\"button\">";
      ProjectContact projectContact1 = (ProjectContact) dataSession.get(ProjectContact.class, pa.getContactId());
      pa.setContact(projectContact1);
      ProjectContact nextProjectContact = null;
      if (pa.getNextContactId() != null && pa.getNextContactId() > 0) {
        nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class, pa.getNextContactId());
        pa.setNextProjectContact(nextProjectContact);
      }
      out.println("  <tr>");
      if (pa.getNextDue() != null) {
        out.println("    <td class=\"inside\">" + editActionLink + sdf11.format(pa.getNextDue())
            + "</a></td>");
      } else {
        out.println("    <td class=\"inside\">&nbsp;</td>");
      }
      if (pa.getNextTimeEstimate() != null && pa.getNextTimeEstimate() > 0) {
        out.println("    <td class=\"inside\">" + pa.getNextTimeEstimateForDisplay() + "</td>");
      } else {
        out.println("    <td class=\"inside\">&nbsp;</td>");
      }
      out.println("    <td class=\"inside\">" + editActionLink
          + pa.getNextDescriptionForDisplay(webUser.getProjectContact()));
      if (pa.getNextTimeEstimate() != null && pa.getNextTimeEstimate() > 0) {
        out.println(" (time estimate: " + pa.getNextTimeEstimateForDisplay() + ")");
      }
      out.println("</a></td>");
      if (pa.getNextDue() != null) {
        if (pa.getNextDue().after(today)) {
          out.println("    <td class=\"inside\"></td>");
        } else if (pa.getNextDue().after(yesterday)) {
          out.println("    <td class=\"inside-highlight\">Due Today</td>");
        } else {
          out.println("    <td class=\"inside-highlight\">Overdue</td>");
        }
      } else {
        out.println("    <td class=\"inside\">&nbsp;</td>");
      }
      out.println("    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
          + pa.getActionId() + "\"></td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }

  protected static void printTodoList(int projectId, WebUser webUser, Session dataSession,
      PrintWriter out, List<ProjectAction> projectActionList, ProjectAction completingProjectAction) {
    if (projectActionList.size() > 0) {
      out.println("<table class=\"inside\" width=\"100%\">");
      out.println("  <tr>");
      out.println("    <th class=\"inside\">Date</th>");
      out.println("    <th class=\"inside\">Time</th>");
      out.println("    <th class=\"inside\">To Do</th>");
      out.println("    <th class=\"inside\">Status</th>");
      out.println("    <th class=\"inside\">Comp</th>");
      out.println("  </tr>");
      SimpleDateFormat sdf11 = webUser.getDateFormat();
      for (ProjectAction pa : projectActionList) {
        String workActionLink = "<a href=\"ProjectActionServlet?" + ProjectActionServlet.PARAM_COMPLETING_ACTION_ID
            + "=" + pa.getActionId()
            + "\" class=\"button\">";
        String editActionLink = "<a href=\"ProjectServlet?" + PARAM_PROJECT_ID + "=" + projectId
            + "&" + PARAM_ACTION_ID + "=" + pa.getActionId() + "\" class=\"button\">";
        Date today = new Date();
        Calendar calendar1 = webUser.getCalendar();
        calendar1.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = calendar1.getTime();

        ProjectContact projectContact1 = (ProjectContact) dataSession.get(ProjectContact.class, pa.getContactId());
        pa.setContact(projectContact1);
        ProjectContact nextProjectContact = null;
        if (pa.getNextContactId() != null && pa.getNextContactId() > 0) {
          nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class, pa.getNextContactId());
          pa.setNextProjectContact(nextProjectContact);
        }
        out.println("  <tr>");
        if (pa.getNextDue() != null) {
          out.println("    <td class=\"inside\">" + editActionLink + sdf11.format(pa.getNextDue())
              + "</a></td>");
        } else {
          out.println("    <td class=\"inside\">&nbsp;</td>");
        }

        if (pa.getNextTimeEstimate() != null && pa.getNextTimeEstimate() > 0) {
          out.println("    <td class=\"inside\">" + workActionLink + pa.getNextTimeEstimateForDisplay() + "</a></td>");
        } else {
          out.println("    <td class=\"inside\">&nbsp;</td>");
        }

        printActionDescription(webUser, out, sdf11, pa, editActionLink, today);
        if (pa.getNextDue() != null) {
          if (pa.getNextDue().after(today)) {
            out.println("    <td class=\"inside\"></td>");
          } else if (pa.getNextDue().after(yesterday)) {
            out.println("    <td class=\"inside-highlight\">Due Today</td>");
          } else {
            out.println("    <td class=\"inside-highlight\">Overdue</td>");
          }
        } else {
          out.println("    <td class=\"inside\">&nbsp;</td>");
        }
        String checked = "";
        if (completingProjectAction != null && completingProjectAction.getActionId() == pa.getActionId()) {
          checked = " checked";
        }
        out.println("    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
            + pa.getActionId() + "\"" + checked + "></td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    } else {
      out.println("<i>no items</i>");
    }
  }

  protected static void printActionDescription(WebUser webUser, PrintWriter out, SimpleDateFormat sdf11,
      ProjectAction pa,
      String editActionLink, Date today) {
    String additionalContent = "";
    if (pa.getLinkUrl() != null && pa.getLinkUrl().length() > 0) {
      additionalContent = " [<a href=\"" + pa.getLinkUrl() + "\" target=\"_blank\">link</a>]";
    }
    if (pa.getNextDeadline() != null) {
      if (pa.getNextDeadline().after(today)) {
        additionalContent += "    <br/>Deadline: " + sdf11.format(pa.getNextDeadline());
      } else {
        additionalContent += "    <br/>Deadline: <span class=\"fail\">"
            + sdf11.format(pa.getNextDeadline()) + "</span>";
      }
    }
    if (editActionLink == null) {
      out.println("    <td class=\"inside\">"
          + pa.getNextDescriptionForDisplay(webUser.getProjectContact()) + additionalContent + "</td>");
    } else {
      out.println("    <td class=\"inside\">" + editActionLink
          + pa.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a>" + additionalContent + "</td>");
    }
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

  @SuppressWarnings("unchecked")
  public static Project setupProject(Session dataSession, int projectId) {
    Project project;
    {
      Query query = dataSession.createQuery("from Project where projectId = ? ");
      query.setParameter(0, projectId);
      project = ((List<Project>) query.list()).get(0);
      ProjectsServlet.loadProjectsObject(dataSession, project);
    }
    return project;
  }

  protected static Date parseDate(AppReq appReq, String dateString) {
    Date date = null;
    if (dateString != null && dateString.length() > 0) {
      SimpleDateFormat sdf1 = appReq.getWebUser().getDateFormat("MM/dd/yyyy hh:mm aaa");
      try {
        date = sdf1.parse(dateString);
      } catch (Exception e) {
        // try again
        sdf1 = appReq.getWebUser().getDateFormat();
        try {
          date = sdf1.parse(dateString);
        } catch (Exception e2) {
          appReq.setMessageProblem("Unable to read date: " + e2);
        }
      }
    }
    return date;
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

  protected static void printOutScript(PrintWriter out, String formName, WebUser webUser) {
    SimpleDateFormat sdf = webUser.getDateFormat();
    out.println(" <script>");
    out.println("    function clickForEmail" + formName + "(projectContactId) { ");
    out.println("      var form = document.forms['saveProjectActionForm" + formName + "']; ");
    out.println("      var checkBox = form['sendEmailTo' + projectContactId];");
    out.println("      checkBox.checked = !checkBox.checked; ");
    out.println("    }");
    out.println("    function selectProjectActionType" + formName + "(actionType)");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm" + formName + "'];");
    out.println("      var found = false; ");
    out.println("      var label = makeIStatement" + formName
        + "(actionType, form.nextContactId.options[form.nextContactId.selectedIndex].text);");
    out.println("      form.startSentance.value = label;");
    out.println("      form.nextActionType.value = actionType;");
    out.println("      enableForm" + formName + "(); ");
    out.println("    }");
    out.println("    ");
    out.println("    function enableForm" + formName + "()");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm" + formName + "'];");
    out.println("      form.nextDue.disabled = false;");
    out.println("      form.nextDescription.disabled = false;");
    out.println("      form.nextContactId.disabled = false;");
    out.println("      form.startSentance.disabled = false;");
    out.println("      form.nextTimeEstimate.disabled = false;");
    out.println("      form.nextDeadline.disabled = false;");
    out.println("      form.linkUrl.disabled = false;");
    out.println("      form.templateType.disabled = false;");
    out.println("      form.prioritySpecial.disabled = false;");
    out.println("      if (form.nextDue.value == \"\")");
    out.println("      {");
    out.println("       document.projectAction" + formName + ".nextDue.value = '"
        + sdf.format(new Date()) + "';");
    out.println("      }");
    out.println("    }");
    out.println("    ");
    printGenerateSelectNextTimeEstimateFunction(out, formName);
    out.println("  </script>");
  }

  protected static void printGenerateSelectNextTimeEstimateFunction(PrintWriter out, String formName) {
    out.println("    function selectNextTimeEstimate" + formName + "(timeInMinutes)");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm" + formName + "'];");
    out.println("      form.nextTimeEstimate.value = timeInMinutes;");
    out.println("    }");
    out.println("    ");
    out.println("    function makeIStatement" + formName + "(actionType, nextContactName)");
    out.println("    {");
    out.println("      if (nextContactName == 'none')");
    out.println("      {");
    out.println("        nextContactName = '';");
    out.println("      }");
    out.println("      if (actionType == '" + ProjectNextActionType.WILL_CONTACT + "')");
    out.println("      {");
    out.println("        if (nextContactName == '')");
    out.println("        {");
    out.println("          return \"I will make contact about:\";");
    out.println("        } else");
    out.println("        {");
    out.println("          return \"I will contact \" + nextContactName + \" about:\";");
    out.println("        }");
    out.println("      } else if (actionType == '" + ProjectNextActionType.COMMITTED_TO + "')");
    out.println("      {");
    out.println("        if (nextContactName == '')");
    out.println("        {");
    out.println("          return \"I have committed to:\";");
    out.println("        } else");
    out.println("        {");
    out.println("          return \"I have committed to \" + nextContactName + \" to:\";");
    out.println("        }");
    out.println("      } else if (actionType == '" + ProjectNextActionType.GOAL + "')");
    out.println("      {");
    out.println("          if (nextContactName == '')");
    out.println("          {");
    out.println("            return \"I have set goal to:\";");
    out.println("          } else");
    out.println("          {");
    out.println("            return \"I have set goal with \" + nextContactName + \" to:\";");
    out.println("          }");
    out.println("      } else if (actionType == '" + ProjectNextActionType.WAITING + "')");
    out.println("      {");
    out.println("        if (nextContactName == '')");
    out.println("        {");
    out.println("          return \"I am waiting for:\";");
    out.println("        } else");
    out.println("        {");
    out.println("          return \"I am waiting for \" + nextContactName + \" to:\";");
    out.println("        }");
    out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_FOLLOW_UP + "')");
    out.println("      {");
    out.println("        if (nextContactName == '')");
    out.println("        {");
    out.println("          return \"I will follow up:\";");
    out.println("        } else");
    out.println("        {");
    out.println("          return \"I will follow up with \" + nextContactName + \" to:\";");
    out.println("        }");
    out.println("      } else if (actionType == '" + ProjectNextActionType.MIGHT + "')");
    out.println("      {");
    out.println("          return \"I might:\";");
    out.println("      } else if (actionType == '" + ProjectNextActionType.GOAL + "')");
    out.println("      {");
    out.println("          return \"I have a goal to:\";");
    out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_REVIEW + "')");
    out.println("      {");
    out.println("          return \"I will review:\";");
    out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_DOCUMENT + "')");
    out.println("      {");
    out.println("          return \"I will document:\";");
    out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_MEET + "')");
    out.println("      {");
    out.println("        if (nextContactName == '')");
    out.println("        {");
    out.println("          return \"I will meet:\";");
    out.println("        } else");
    out.println("        {");
    out.println("          return \"I will meet with \" + nextContactName + \" to:\";");
    out.println("        }");
    out.println("      } else");
    out.println("      {");
    out.println("        return \"I will:\";");
    out.println("      }");
    out.println("    }");
    out.println("    ");
  }

}
