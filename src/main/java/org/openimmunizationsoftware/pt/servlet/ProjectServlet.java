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
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectServlet extends ClientServlet {

  private static final String PARAM_PROJECT_ID = "projectId";

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

      Project project = setupProject(appReq, dataSession, projectId);

      List<Project> projectSelectedList = setupProjectList(appReq, project);

      TimeTracker timeTracker = appReq.getTimeTracker();

      ProjectContactAssigned projectContactAssignedForThisUser =
          getProjectContactAssigned(webUser, dataSession, project);

      String emailBody = null;
      if (action != null) {
        if (action.equals("Save")) {
          ProjectAction projectAction = new ProjectAction();
          projectAction.setProjectId(project.getProjectId());
          projectAction.setContactId(webUser.getContactId());
          projectAction.setContact(webUser.getProjectContact());
          Date actionDate = new Date();
          try {
            actionDate = sdf.parse(request.getParameter("actionDate"));
          } catch (Exception e) {
            appReq.setMessageProblem("Unable to read action date: " + e);
          }
          int nextTimeEstimate = 0;
          if (request.getParameter("nextTimeEstimate") != null) {
            try {
              nextTimeEstimate = Integer.parseInt(request.getParameter("nextTimeEstimate"));
            } catch (NumberFormatException nfe) {
              nextTimeEstimate = 0;
            }
          }
          projectAction.setActionDate(actionDate);
          projectAction
              .setActionDescription(trim(request.getParameter("actionDescription"), 12000));
          projectAction.setNextDescription(trim(request.getParameter("nextDescription"), 1200));
          if (nextTimeEstimate > 0) {
            projectAction.setNextTimeEstimate(nextTimeEstimate);
          }
          projectAction.setNextActionId(0);

          projectAction.setNextDue(parseDate(appReq, request.getParameter("nextDue")));
          projectAction.setNextDeadline(parseDate(appReq, request.getParameter("nextDeadline")));

          String nextActionType = request.getParameter("nextActionType");
          projectAction.setNextActionType(nextActionType);
          int priorityLevel = project.getPriorityLevel();
          if (nextActionType != null) {
            if (nextActionType.equals("T")) {
              priorityLevel += 1;
            } else if (nextActionType.equals("N") || nextActionType.equals("W")
                || nextActionType.equals("E") || nextActionType.equals("A")) {
              priorityLevel -= 1;
            }
          }
          projectAction.setPriorityLevel(priorityLevel);
          String nextContactIdString = request.getParameter("nextContactId");
          if (nextContactIdString != null && nextContactIdString.length() > 0) {
            projectAction.setNextContactId(Integer.parseInt(nextContactIdString));
            projectAction.setNextProjectContact((ProjectContact) dataSession
                .get(ProjectContact.class, projectAction.getNextContactId()));
          }
          projectAction.setProvider(webUser.getProvider());
          String message = appReq.getMessageProblem();
          if (message == null) {
            String[] completed = request.getParameterValues("completed");

            Transaction trans = dataSession.beginTransaction();
            try {
              dataSession.save(projectAction);
              if (completed != null && completed.length > 0) {
                for (String c : completed) {
                  ProjectAction paCompleted =
                      (ProjectAction) dataSession.get(ProjectAction.class, Integer.parseInt(c));
                  paCompleted.setNextActionId(projectAction.getActionId());
                  dataSession.update(paCompleted);
                }
              }
            } finally {
              trans.commit();
            }
            boolean userAssignedToProject = false;
            Query query =
                dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
            query.setParameter(0, projectId);
            List<ProjectContactAssigned> projectContactAssignedList = query.list();
            List<ProjectContact> sendEmailToList = new ArrayList<ProjectContact>();
            for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
              if (projectContactAssigned.getProjectContact() == webUser.getProjectContact()) {
                userAssignedToProject = true;
              }
              if (request.getParameter(
                  "sendEmailTo" + projectContactAssigned.getId().getContactId()) != null) {
                query = dataSession.createQuery("from ProjectContact where contactId = ?");
                query.setParameter(0, projectContactAssigned.getId().getContactId());
                ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
                sendEmailToList.add(projectContact);
              }
            }
            if (!userAssignedToProject) {
              assignContact(projectId, dataSession, webUser.getContactId());
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
                timeTracker.startClock(parentProject, dataSession);
              }
            } else {
              timeTracker.startClock(project, dataSession);
            }
          }
        }
      }

      appReq.setTitle("Projects");
      printHtmlHead(appReq);

      out.println("<div class=\"main\">");
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
      Query query = dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
      query.setParameter(0, projectId);
      List<ProjectContactAssigned> projectContactAssignedList = query.list();
      List<ProjectContact> projectContactList = new ArrayList<ProjectContact>();
      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList) {
        query = dataSession.createQuery("from ProjectContact where contactId = ?");
        query.setParameter(0, projectContactAssigned.getId().getContactId());
        ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
        projectContactList.add(projectContact);
        projectContactAssigned.setProjectContact(projectContact);
        projectContactAssigned.setProject(project);
      }
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

      query = dataSession.createQuery(
          "from ProjectAction where projectId = ? and nextDescription <> '' and nextActionId = 0 order by nextDue asc");
      query.setParameter(0, projectId);
      List<ProjectAction> projectActionList = query.list();
      List<ProjectAction> projectActionGoalList = new ArrayList<ProjectAction>();
      {
        Date today = new Date();
        for (Iterator<ProjectAction> it = projectActionList.iterator(); it.hasNext();) {
          ProjectAction projectAction = it.next();
          if (projectAction.getNextActionType().equals(ProjectNextActionType.GOAL)
              && (projectAction.getNextDue() == null || projectAction.getNextDue().after(today))) {
            it.remove();
            projectActionGoalList.add(projectAction);
          }
        }
      }

      printOutScript(out, projectId, webUser);
      out.println(
          "<form name=\"projectAction\" method=\"post\" action=\"ProjectServlet\" id=\"saveProjectActionForm\">");
      out.println("<input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\">");
      out.println("  <table class=\"boxed\">");
      out.println("    <tr>");
      out.println("      <th class=\"title\">Todo List</th>");
      out.println("    </tr>");
      out.println("    <td class=\"outside\">");
      if (projectActionList.size() > 0) {
        out.println("<table class=\"inside\" width=\"100%\">");
        out.println("  <tr>");
        out.println("    <th class=\"inside\">Date</th>");
        out.println("    <th class=\"inside\">Name</th>");
        out.println("    <th class=\"inside\">To Do</th>");
        out.println("    <th class=\"inside\">Status</th>");
        out.println("    <th class=\"inside\">Comp</th>");
        out.println("  </tr>");
        SimpleDateFormat sdf11 = webUser.getDateFormat();
        for (ProjectAction projectAction1 : projectActionList) {

          ProjectContact projectContact1 =
              (ProjectContact) dataSession.get(ProjectContact.class, projectAction1.getContactId());
          projectAction1.setContact(projectContact1);
          ProjectContact nextProjectContact = null;
          if (projectAction1.getNextContactId() != null && projectAction1.getNextContactId() > 0) {
            nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class,
                projectAction1.getNextContactId());
            projectAction1.setNextProjectContact(nextProjectContact);
          }
          out.println("  <tr>");
          if (projectAction1.getNextDue() != null) {
            out.println(
                "    <td class=\"inside\">" + sdf11.format(projectAction1.getNextDue()) + "</td>");
          } else {
            out.println("    <td class=\"inside\">&nbsp;</td>");
          }
          out.println("    <td class=\"inside\">" + projectContact1.getNameFirst() + " "
              + projectContact1.getNameLast() + "</td>");

          {
            String link = "ProjectTodoServlet?" + ProjectTodoServlet.PARAM_ACTION_ID + "="
                + projectAction1.getActionId();
            out.println("    <td class=\"inside\"><a href=\"" + link + "\">"
                + projectAction1.getNextDescriptionForDisplay(webUser.getProjectContact())
                + "</a>");
          }
          if (projectAction1.getNextTimeEstimate() != null
              && projectAction1.getNextTimeEstimate() > 0) {
            out.println(" (time estimate: " + projectAction1.getNextTimeEstimateForDisplay() + ")");
          }
          out.println("</td>");
          if (projectAction1.getNextDue() != null) {
            Date today = new Date();
            Calendar calendar1 = webUser.getCalendar();
            calendar1.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = calendar1.getTime();
            if (projectAction1.getNextDue().after(today)) {
              out.println("    <td class=\"inside\"></td>");
            } else if (projectAction1.getNextDue().after(yesterday)) {
              out.println("    <td class=\"inside-highlight\">Due Today</td>");
            } else {
              out.println("    <td class=\"inside-highlight\">Overdue</td>");
            }
          } else {
            out.println("    <td class=\"inside\">&nbsp;</td>");
          }
          out.println(
              "    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
                  + projectAction1.getActionId() + "\"></td>");
          out.println("  </tr>");
        }
        out.println("</table>");
      } else {
        out.println("<i>no items</i>");
      }

      out.println("    </td>");
      out.println("  </tr>");

      out.println("  <tr>");
      out.println("    <th class=\"title\">What happened?</th>");
      out.println("  </tr>");
      out.println("    <td class=\"outside\">");

      SimpleDateFormat sdf1 = webUser.getDateFormat("MM/dd/yyyy hh:mm aaa");

      out.println("      <table class=\"inside\">");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">When</th>");
      out.println(
          "          <td class=\"inside\"><input type=\"text\" name=\"actionDate\" size=\"20\" value=\""
              + sdf1.format(new Date()) + "\" onkeydown=\"resetRefresh()\"></td>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Action</th>");
      out.println(
          "          <td class=\"inside\"><textarea name=\"actionDescription\" cols=\"60\" rows=\"7\" onkeydown=\"resetRefresh()\"></textarea></td>");
      out.println("        </tr>");
      out.println("        <script>");
      out.println("          function setNextAction(nextActionDate)");
      out.println("          {");
      out.println("            document.projectAction.nextDue.value = nextActionDate;");
      out.println("            enableForm(); ");
      out.println("          }");
      out.println("          function setNextDeadline(nextDeadline)");
      out.println("          {");
      out.println("            document.projectAction.nextDeadline.value = nextDeadline;");
      out.println("          }");
      out.println("        </script>");
      out.println("      </table>");
      out.println("   </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <th class=\"title\">What is next?</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"outside\">");
      out.println("      <table class=\"inside\">");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">When</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"nextDue\" size=\"10\" value=\""
              + n(request.getParameter("nextDue")) + "\" onkeydown=\"resetRefresh()\" disabled>");
      out.println("            <font size=\"-1\">");
      Calendar calendar = webUser.getCalendar();
      sdf1 = webUser.getDateFormat();
      SimpleDateFormat day = webUser.getDateFormat("EEE");
      out.println("              <a href=\"javascript: void setNextAction('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Today</a>");
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      out.println("              <a href=\"javascript: void setNextAction('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 6; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextAction('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextAction('"
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
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Action</th>");
      out.println("          <td class=\"inside\" colspan=\"3\">");
      out.println(
          "            I: <font size=\"-1\"><a href=\"javascript: void selectProjectActionType('"
              + ProjectNextActionType.WILL + "');\" class=\"button\"> will</a>,");
      out.println("            <a href=\"javascript: void selectProjectActionType('"
          + ProjectNextActionType.MIGHT + "');\" class=\"button\">might</a>, ");
      out.println("            <a href=\"javascript: void selectProjectActionType('"
          + ProjectNextActionType.WILL_CONTACT + "');\" class=\"button\">will contact</a>, ");
      out.println("            <a href=\"javascript: void selectProjectActionType('"
          + ProjectNextActionType.WILL_MEET + "');\" class=\"button\">will meet</a></font>");
      out.println("            I have: ");
      out.println(
          "            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType('"
              + ProjectNextActionType.COMMITTED_TO + "');\" class=\"button\">committed</a>,");
      out.println("            <a href=\"javascript: void selectProjectActionType('"
          + ProjectNextActionType.GOAL + "');\" class=\"button\">set goal</a></font>");
      out.println("            <br/>");
      out.println("            I am:");
      out.println(
          "            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType('"
              + ProjectNextActionType.WAITING + "');\" class=\"button\">waiting</a>,");
      out.println("            <a href=\"javascript: void selectProjectActionType('"
          + ProjectNextActionType.ASKS_TO + "');\" class=\"button\">asking</a></font>");
      out.println("            <br>");
      out.println("            <input type=\"hidden\" name=\"nextActionType\" value=\""
          + ProjectNextActionType.WILL + "\">");
      out.println("            </font>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">What</th>");
      out.println("          <td class=\"inside\"> ");
      out.println(
          "            <input name=\"startSentance\" size=\"40\" value=\"I will:\" disabled>");
      out.println("          </td>");
      out.println("          <th class=\"inside\">Who</th>");
      out.println("          <td class=\"inside\"> ");
      out.println(
          "              <select name=\"nextContactId\" onchange=\"selectProjectActionType(form.nextActionType.value);\" disabled><option value=\"\">none</option>");
      String nextContactId = n(request.getParameter("nextContactId"));
      for (ProjectContact projectContact1 : projectContactList) {
        if (projectContact1.getContactId() != webUser.getProjectContact().getContactId()) {
          boolean selected = nextContactId.equals(projectContact1.getContactId());
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
          "            <textarea name=\"nextDescription\" cols=\"60\" rows=\"2\" onkeydown=\"resetRefresh()\" disabled></textarea>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Time</th>");
      out.println("          <td class=\"inside\" colspan=\"3\">");
      out.println(
          "            <input type=\"text\" name=\"nextTimeEstimate\" size=\"3\" value=\"\" onkeydown=\"resetRefresh()\" disabled> mins ");
      out.println("            <font size=\"-1\">");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('5');\" class=\"button\"> 5m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('10');\" class=\"button\"> 10m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('20');\" class=\"button\"> 20m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('30');\" class=\"button\"> 30m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('40');\" class=\"button\"> 40m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('60');\" class=\"button\"> 60m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('70');\" class=\"button\"> 70m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('90');\" class=\"button\"> 90m</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('120');\" class=\"button\"> 2h</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('240');\" class=\"button\"> 4h</a>");
      out.println(
          "              <a href=\"javascript: void selectNextTimeEstimate('360');\" class=\"button\"> 6h</a>");
      out.println("            </font> ");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Deadline</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"nextDeadline\" size=\"10\" value=\""
              + n(request.getParameter("nextDeadline"))
              + "\" onkeydown=\"resetRefresh()\" disabled>");
      out.println("            <font size=\"-1\">");
      sdf1 = webUser.getDateFormat();
      out.println("              <a href=\"javascript: void setNextDeadline('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Today</a>");
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      out.println("              <a href=\"javascript: void setNextDeadline('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      nextWeek = false;
      for (int i = 0; i < 6; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextDeadline('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextDeadline('"
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
      out.println("      </table>");
      out.println("    </td>");
      out.println("  </tr>");
      if (projectActionGoalList.size() > 0) {
        out.println("    <tr>");
        out.println("      <th class=\"title\">Goals in the Future</th>");
        out.println("    </tr>");
        out.println("    <td class=\"outside\">");
        out.println("<table class=\"inside\" width=\"100%\">");
        out.println("  <tr>");
        out.println("    <th class=\"inside\">Date</th>");
        out.println("    <th class=\"inside\">Name</th>");
        out.println("    <th class=\"inside\">To Do</th>");
        out.println("    <th class=\"inside\">Status</th>");
        out.println("    <th class=\"inside\">Comp</th>");
        out.println("  </tr>");
        SimpleDateFormat sdf11 = webUser.getDateFormat();
        for (ProjectAction projectAction1 : projectActionGoalList) {

          ProjectContact projectContact1 =
              (ProjectContact) dataSession.get(ProjectContact.class, projectAction1.getContactId());
          projectAction1.setContact(projectContact1);
          ProjectContact nextProjectContact = null;
          if (projectAction1.getNextContactId() != null && projectAction1.getNextContactId() > 0) {
            nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class,
                projectAction1.getNextContactId());
            projectAction1.setNextProjectContact(nextProjectContact);
          }
          out.println("  <tr>");
          if (projectAction1.getNextDue() != null) {
            out.println(
                "    <td class=\"inside\">" + sdf11.format(projectAction1.getNextDue()) + "</td>");
          } else {
            out.println("    <td class=\"inside\">&nbsp;</td>");
          }
          out.println("    <td class=\"inside\">" + projectContact1.getNameFirst() + " "
              + projectContact1.getNameLast() + "</td>");
          out.println("    <td class=\"inside\">"
              + projectAction1.getNextDescriptionForDisplay(webUser.getProjectContact()));
          if (projectAction1.getNextTimeEstimate() != null
              && projectAction1.getNextTimeEstimate() > 0) {
            out.println(" (time estimate: " + projectAction1.getNextTimeEstimateForDisplay() + ")");
          }
          out.println("</td>");
          if (projectAction1.getNextDue() != null) {
            Date today = new Date();
            Calendar calendar1 = webUser.getCalendar();
            calendar1.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = calendar1.getTime();
            if (projectAction1.getNextDue().after(today)) {
              out.println("    <td class=\"inside\"></td>");
            } else if (projectAction1.getNextDue().after(yesterday)) {
              out.println("    <td class=\"inside-highlight\">Due Today</td>");
            } else {
              out.println("    <td class=\"inside-highlight\">Overdue</td>");
            }
          } else {
            out.println("    <td class=\"inside\">&nbsp;</td>");
          }
          out.println(
              "    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
                  + projectAction1.getActionId() + "\"></td>");
          out.println("  </tr>");
        }
        out.println("</table>");
        out.println("    </td>");
        out.println("  </tr>");
      }



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
        out.println("<font size=\"-1\"><a href=\"javascript: void clickForEmail('"
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
      out.println("</form>");
      out.println("</div>");

      out.println("<div class=\"main\">");
      out.println("<h2>Actions Taken</h2>");
      out.println("<table class=\"boxed-fill\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Date &amp; Time</th>");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("  </tr>");
      query = dataSession.createQuery(
          "from ProjectAction where projectId = ? and actionDescription <> '' order by actionDate desc");
      query.setParameter(0, projectId);
      projectActionList = query.list();
      for (ProjectAction projectAction : projectActionList) {
        ProjectContact projectContact =
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId());
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed\">" + sdf.format(projectAction.getActionDate()) + "</td>");
        out.println("    <td class=\"boxed\">" + projectContact.getNameFirst() + " "
            + projectContact.getNameLast() + "</td>");
        out.println("    <td class=\"boxed\">" + nbsp(projectAction.getActionDescription()));
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("</div>");

      printHtmlFoot(appReq);

    } finally {
      appReq.close();
    }
  }

  protected List<Project> setupProjectList(AppReq appReq, Project project) {
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
  protected Project setupProject(AppReq appReq, Session dataSession, int projectId) {
    Project project;
    {
      Query query = dataSession.createQuery("from Project where projectId = ? ");
      query.setParameter(0, projectId);
      project = ((List<Project>) query.list()).get(0);
      ProjectsServlet.loadProjectsObject(dataSession, project);
    }
    appReq.setProject(project);
    return project;
  }

  private Date parseDate(AppReq appReq, String dateString) {
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

  private void printOutScript(PrintWriter out, int projectId, WebUser webUser) {
    SimpleDateFormat sdf = webUser.getDateFormat();
    out.println(" <script>");
    out.println("    function clickForEmail(projectContactId) { ");
    out.println("      var form = document.forms['saveProjectActionForm']; ");
    out.println("      var checkBox = form['sendEmailTo' + projectContactId];");
    out.println("      checkBox.checked = !checkBox.checked; ");
    out.println("    }");
    out.println("    function selectProjectActionType(actionType)");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm'];");
    out.println("      var found = false; ");
    out.println(
        "      var label = makeIStatement(actionType, form.nextContactId.options[form.nextContactId.selectedIndex].text);");
    out.println("      form.startSentance.value = label;");
    out.println("      form.nextActionType.value = actionType;");
    out.println("      enableForm(); ");
    out.println("    }");
    out.println("    ");
    out.println("    function enableForm()");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm'];");
    out.println("      form.nextDue.disabled = false;");
    out.println("      form.nextDescription.disabled = false;");
    out.println("      form.nextContactId.disabled = false;");
    out.println("      form.startSentance.disabled = false;");
    out.println("      form.nextTimeEstimate.disabled = false;");
    out.println("      if (form.nextDue.value == \"\")");
    out.println("      {");
    out.println("       document.projectAction.nextDue.value = '" + sdf.format(new Date()) + "';");
    out.println("      }");
    out.println("    }");
    out.println("    ");
    out.println("    function selectNextTimeEstimate(timeInMinutes)");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm'];");
    out.println("      form.nextTimeEstimate.value = timeInMinutes;");
    out.println("    }");
    out.println("    ");
    out.println("    function makeIStatement(actionType, nextContactName)");
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
    out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_RUN_ERRAND + "')");
    out.println("      {");
    out.println("          return \"I will run errand to:\";");
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
    out.println("      } else if (actionType == '" + ProjectNextActionType.ASKS_TO + "')");
    out.println("      {");
    out.println("        if (nextContactName == '')");
    out.println("        {");
    out.println("          return \"I am asking for:\";");
    out.println("        } else");
    out.println("        {");
    out.println("          return \"I am asking \" + nextContactName + \" to:\";");
    out.println("        }");
    out.println("      } else if (actionType == '" + ProjectNextActionType.MIGHT + "')");
    out.println("      {");
    out.println("          return \"I might:\";");
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
    out.println("  </script>");
  }

}
