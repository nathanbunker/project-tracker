/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.ChatAgent;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.PrioritySpecial;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectActionServlet extends ClientServlet {

  private static final String ID_TIME_RUNNING = "TimeRunning";
  private static final String ID_TIME_TODAY = "TimeToday";
  private static final String ID_TIME_OTHER = "TimeOther";
  private static final String ID_TIME_MIGHT = "TimeMight";
  private static final String ID_TIME_WILL = "TimeWill";
  private static final String ID_TIME_COMMITTED = "TimeCommitted";
  private static final String ID_TIME_COMPLETED = "TimeCompleted";
  private static final String ID_TIME_WILL_MEET = "TimeWillMeet";
  private static final String ID_EST = "Est";
  private static final String ID_ACT = "Act";
  private static final String SAVE_PROJECT_ACTION_FORM = "saveProjectActionForm";
  private static final String FORM_ACTION_NOW = "ActionNow";
  private static final String FORM_ACTION_NEXT = "ActionNext";
  private static final String PARAM_SEND_EMAIL_TO = "sendEmailTo";
  private static final String PARAM_START_SENTANCE = "startSentance";
  private static final String PARAM_PRIORITY_SPECIAL = "prioritySpecial";
  private static final String PARAM_NEXT_CONTACT_ID = "nextContactId";
  private static final String PARAM_NEXT_ACTION_TYPE = "nextActionType";
  private static final String PARAM_TEMPLATE_TYPE = "templateType";
  private static final String PARAM_LINK_URL = "linkUrl";
  private static final String PARAM_NEXT_DEADLINE = "nextDeadline";
  private static final String PARAM_NEXT_DUE = "nextDue";
  private static final String PARAM_NEXT_DESCRIPTION = "nextDescription";
  private static final String PARAM_NEXT_NOTES = "nextNotes";
  private static final String PARAM_NEXT_NOTE = "nextNote";
  private static final String PARAM_NEXT_SUMMARY = "nextSummary";
  private static final String PARAM_NEXT_TIME_ESTIMATE = "nextTimeEstimate";
  private static final String PARAM_NEXT_PROJECT_ID = "nextProjectId";
  private static final String PARAM_PROJECT_ID = "projectId";
  protected static final String PARAM_COMPLETING_ACTION_ID = "completingActionId";
  private static final String PARAM_EDIT_ACTION_ID = "editActionId";
  protected static final String PARAM_ACTION = "action";
  protected static final String ACTION_START_TIMER = "StartTimer";
  private static final String ACTION_STOP_TIMER = "StopTimer";
  private static final String ACTION_NOTE = "Note";
  private static final String ACTION_PROPOSE = "Propose";
  private static final String ACTION_FEEDBACK = "Feedback";
  private static final String ACTION_COMPLETED = "Completed";
  private static final String ACTION_SAVE = "Save";
  private static final String ACTION_DELETE = "Delete";
  private static final String ACTION_START = "Start";
  private static final String ACTION_REFRESH_TIME = "RefreshTime";
  private static final String ACTION_SUGGEST = "Suggest";

  private static final String LIST_START = " - ";
  private static final String SYSTEM_INSTRUCTIONS = "You are a helpful assistant tasked with helping a professional report about progress that is being made on a project.";

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

      readCompletingAction(request, appReq, dataSession);

      Project project = appReq.getProject();
      ProjectAction completingAction = appReq.getCompletingAction();
      if (completingAction != null) {
        project = completingAction.getProject();
      }
      appReq.setProject(project);
      appReq.setProjectSelected(project);
      appReq.setCompletingAction(completingAction);
      appReq.setProjectActionSelected(completingAction);

      List<ChatAgent> chatAgentList = new ArrayList<ChatAgent>();

      if (completingAction != null) {
        readNotesSummaryUpdateTimeTracker(request, appReq, dataSession, completingAction);
      }

      String emailBody = null;

      List<ProjectAction> projectActionTakenList = null;
      List<ProjectAction> projectActionScheduledList = null;
      if (project != null) {
        projectActionTakenList = ProjectServlet.getProjectActionsTakenList(dataSession, project);
        projectActionScheduledList = getAllProjectActionsScheduledList(appReq, project, dataSession);
      }

      String nextSuggest = null;

      if (action != null) {
        if (action.equals(ACTION_START_TIMER)) {
          startTimer(appReq, dataSession, completingAction);
        } else if (action.equals(ACTION_REFRESH_TIME)) {
          printOutRefreshedTimeJson(response, appReq, webUser, dataSession, out, completingAction);
          return;
        } else if (action.equals(ACTION_STOP_TIMER)) {
          stopTimer(appReq, dataSession);
        } else if (action.equals(ACTION_PROPOSE)) {
          chatPropose(appReq, completingAction, chatAgentList, projectActionTakenList, projectActionScheduledList);
        } else if (action.equals(ACTION_FEEDBACK)) {
          chatFeedback(appReq, completingAction, chatAgentList, projectActionTakenList, projectActionScheduledList);
        } else if (action.equals(ACTION_SUGGEST)) {
          nextSuggest = chatNext(appReq, completingAction, chatAgentList, projectActionTakenList,
              projectActionScheduledList);
        } else {
          ProjectAction editProjectAction = readEditProjectAction(appReq);
          if (action.equals(ACTION_COMPLETED)) {
            ProjectNextActionStatus nextActionStatus = ProjectNextActionStatus.COMPLETED;
            String nextDescription = completingAction.getNextSummary();
            closeAction(appReq, editProjectAction, project, nextDescription, nextActionStatus);
            emailBody = sendEmail(request, appReq, webUser, dataSession, project, editProjectAction, emailBody);
            completingAction = null;
          } else if (action.equals(ACTION_SAVE) || action.equals(ACTION_START)) {
            Project nextProject = project;
            Date originalNextDue = null;
            if (editProjectAction == null) {
              String nextProjectIdString = request.getParameter(PARAM_NEXT_PROJECT_ID);
              if (nextProjectIdString != null) {
                nextProject = (Project) dataSession.get(Project.class, Integer.parseInt(nextProjectIdString));
              }
            } else {
              originalNextDue = editProjectAction.getNextDue();
            }
            editProjectAction = saveProjectAction(appReq, editProjectAction, nextProject);
            Date nextDue = editProjectAction.getNextDue();
            if (action.equals(ACTION_START)) {
              completingAction = editProjectAction;
              setupProjectActionAndSaveToAppReq(appReq, dataSession, completingAction);
              project = completingAction.getProject();
              projectActionTakenList = ProjectServlet.getProjectActionsTakenList(dataSession, project);
            } else if (nextDue != null && !webUser.isToday(nextDue)
                && (completingAction != null && completingAction.equals(editProjectAction))
                && (originalNextDue == null || !nextDue.equals(originalNextDue))) {
              // if next action is being saved and the user didn't signal they wanted to work
              // on this next.
              // Then we should shift off this task if it's been rescheduled to a future date
              completingAction = null;
            }
          } else if (action.equals(ACTION_DELETE)) {
            String nextDescription = "";
            ProjectNextActionStatus nextActionStatus = ProjectNextActionStatus.CANCELLED;
            closeAction(appReq, editProjectAction, project, nextDescription, nextActionStatus);
            if (appReq.getCompletingAction() != null && appReq.getCompletingAction().equals(editProjectAction)) {
              completingAction = null;
            }
          }
          // refresh the project actions taken and scheduled lists
          projectActionTakenList = ProjectServlet.getProjectActionsTakenList(dataSession, project);
          projectActionScheduledList = getAllProjectActionsScheduledList(appReq, project, dataSession);
        }
      }

      List<ProjectAction> projectActionDueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
      List<List<ProjectAction>> projectActionDueNextWorkingDayListList = new ArrayList<>();
      {
        int daysFound = 0;
        int dayOffset = 1;
        while (daysFound < 5 && dayOffset < 14) {
          List<ProjectAction> projectActionDueNextWorkingDayList = getProjectActionListForToday(webUser, dataSession,
              dayOffset);
          while (projectActionDueNextWorkingDayList.size() == 0 && dayOffset < 14) {
            // if there are no actions scheduled for tomorrow, then we should look at the
            // next day
            dayOffset++;
            projectActionDueNextWorkingDayList = getProjectActionListForToday(webUser, dataSession, dayOffset);
          }
          prepareProjectActionList(dataSession, projectActionDueNextWorkingDayList, webUser);
          projectActionDueNextWorkingDayListList.add(projectActionDueNextWorkingDayList);
          dayOffset++;
          daysFound++;
        }

      }
      if (completingAction == null && projectActionDueTodayList.size() > 0) {
        completingAction = projectActionDueTodayList.get(0);
        setupProjectActionAndSaveToAppReq(appReq, dataSession, completingAction);
        project = completingAction.getProject();
        projectActionTakenList = ProjectServlet.getProjectActionsTakenList(dataSession, project);
        projectActionScheduledList = getAllProjectActionsScheduledList(appReq, project, dataSession);
      }
      List<ProjectAction> projectActionClosedTodayList = getProjectActionListClosedToday(webUser, dataSession);

      if (prepareProjectActionListAndIdentifyOverdue(dataSession, projectActionDueTodayList, webUser).size() > 0) {
        // TOOD print a nicer message and a link to clean these up
        appReq.setMessageProblem(
            "There are actions overdue that are not shown here, only showing what is scheduled for today.");
      }

      if (completingAction != null) {
        TimeTracker timeTracker = appReq.getTimeTracker();
        if (timeTracker != null) {
          timeTracker.update(completingAction, dataSession);
        }
      }

      // register project so it shows up in list of projects recently referred to
      if (project != null) {
        ProjectServlet.setupProjectList(appReq, project);
      }
      appReq.setTitle("Actions");
      printHtmlHead(appReq);
      ProjectServlet.printOutEmailSent(out, emailBody);

      Set<String> formNameSet = new HashSet<String>();

      Date nextDue = webUser.getCalendar().getTime();
      SimpleDateFormat sdf1 = webUser.getDateFormat();

      Calendar cIndicated = webUser.getCalendar();
      cIndicated.setTime(nextDue);

      List<Project> projectList = getProjectList(webUser, dataSession);

      if (completingAction == null) {
        printTimeManagementBox(appReq, projectActionDueTodayList);
        out.println("<h2>Good Job!</h2>");
        out.println("<p>You have no more actions to take today. Have a great evening! </p>");
        printActionsCompletedForToday(webUser, out, projectActionClosedTodayList);
      } else {

        List<ProjectContactAssigned> projectContactAssignedList = ProjectServlet
            .getProjectContactAssignedList(dataSession, project.getProjectId());
        List<ProjectContact> projectContactList = ProjectServlet.getProjectContactList(dataSession, project,
            projectContactAssignedList);

        out.println("<div id=\"three-column-container\">");

        // ---------------------------------------------------------------------------------------------------
        // ACTION NEXT
        // ---------------------------------------------------------------------------------------------------
        out.println("<div id=\"actionNext\">");
        printActionsNext(appReq, project, projectContactList, projectList, projectActionTakenList,
            projectActionScheduledList, formNameSet);
        if (nextSuggest != null) {
          out.println("<h3>Suggested Next Actions</h3>");
          out.println(nextSuggest);
        }
        out.println("</div>");

        // ---------------------------------------------------------------------------------------------------
        // ACTION NOW
        // ---------------------------------------------------------------------------------------------------
        out.println("<div id=\"actionNow\">");
        printActionNow(appReq, webUser, out, project, completingAction, projectList, projectContactList, formNameSet);
        out.println("</div>");

        // ---------------------------------------------------------------------------------------------------
        // ACTION LATER
        // ---------------------------------------------------------------------------------------------------
        out.println("<div id=\"actionLater\">");
        printTimeManagementBox(appReq, projectActionDueTodayList);
        printActionsScheduledForToday(webUser, out, projectActionDueTodayList);
        printActionsCompletedForToday(webUser, out, projectActionClosedTodayList);
        {
          List<TimeAdder> timeAdderList = new ArrayList<>();
          for (List<ProjectAction> projectActionDueNextWorkingDayList : projectActionDueNextWorkingDayListList) {
            Date workingDayDate = projectActionDueNextWorkingDayList.get(0).getNextDue();
            TimeAdder timeAdder = new TimeAdder(projectActionDueNextWorkingDayList, appReq, workingDayDate);
            timeAdderList.add(timeAdder);
          }

          out.println("<table class=\"boxed\">");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"title\">Planning</th>");
          for (List<ProjectAction> projectActionDueNextWorkingDayList : projectActionDueNextWorkingDayListList) {
            Date workingDayDate = projectActionDueNextWorkingDayList.get(0).getNextDue();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd");
            out.println("    <th class=\"boxed\">" + sdf.format(workingDayDate) + "</th>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Will Meet</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(timeAdder.getWillMeetEst()) + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Committed</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(timeAdder.getCommittedEst()) + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Will</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println("    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(timeAdder.getWillEst()) + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Might</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(timeAdder.getMightEst()) + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Planned</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println("    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(timeAdder.getWillAct()) + "</td>");
          }
          out.println("  </tr>");
          out.println("</table><br/>");
        }
        for (List<ProjectAction> projectActionDueNextWorkingDayList : projectActionDueNextWorkingDayListList) {
          Date workingDayDate = projectActionDueNextWorkingDayList.get(0).getNextDue();
          printActionsScheduledForNextWorkingDay(webUser, out, projectActionDueNextWorkingDayList, workingDayDate);
          printTimeManagementBoxForNextWorkingDay(appReq, projectActionDueNextWorkingDayList, workingDayDate);
        }
        out.println("</div>");

        out.println("</div>");
      }

      for (ChatAgent chatAgent : chatAgentList) {
        if (chatAgent != null) {
          out.println("<h2>" + chatAgent.getTitle() + " Chat Log</h2>");
          out.println("<h3>Request</h3>");
          out.println("<pre>" + chatAgent.getRawRequestBody() + "</pre>");
          out.println("<h3>Response</h3>");
          out.println("<pre>" + chatAgent.getRawResponseBody() + "</pre>");
          if (chatAgent.getResponseText() != null) {
            out.println("<h3>Response Text</h3>");
            out.println("<p>" + chatAgent.getResponseText() + "</p>");
          }
          if (chatAgent.getResponseError() != null) {
            out.println("<h3>Error</h3>");
            out.println("<p>" + chatAgent.getResponseError() + "</p>");
          }
        }

      }
      printHtmlFoot(appReq);

    } catch (

    Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private void printOutRefreshedTimeJson(HttpServletResponse response, AppReq appReq, WebUser webUser,
      Session dataSession,
      PrintWriter out, ProjectAction completingAction) throws IOException, StreamWriteException, DatabindException {
    // This is a JSON call
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    String timeRunningString = "0:00";
    if (completingAction != null) {
      timeRunningString = TimeTracker.formatTime(completingAction.getNextTimeActual());
    }
    List<ProjectAction> projectActionDueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
    TimeAdder timeAdder = new TimeAdder(projectActionDueTodayList, appReq);
    Map<String, String> timeData = new HashMap<>();
    timeData.put(ID_TIME_RUNNING, timeRunningString);
    timeData.put(ID_TIME_TODAY, getFullDayAndTime());
    timeData.put(ID_TIME_OTHER + ID_EST, ProjectAction.getTimeForDisplay(timeAdder.getOtherEst()));
    timeData.put(ID_TIME_OTHER + ID_ACT, ProjectAction.getTimeForDisplay(timeAdder.getOtherAct()));
    timeData.put(ID_TIME_MIGHT + ID_EST, ProjectAction.getTimeForDisplay(timeAdder.getMightEst()));
    timeData.put(ID_TIME_MIGHT + ID_ACT, ProjectAction.getTimeForDisplay(timeAdder.getMightAct()));
    timeData.put(ID_TIME_WILL + ID_EST, ProjectAction.getTimeForDisplay(timeAdder.getWillEst()));
    timeData.put(ID_TIME_WILL + ID_ACT, ProjectAction.getTimeForDisplay(timeAdder.getWillAct()));
    timeData.put(ID_TIME_COMMITTED + ID_EST, ProjectAction.getTimeForDisplay(timeAdder.getCommittedEst()));
    timeData.put(ID_TIME_COMMITTED + ID_ACT, ProjectAction.getTimeForDisplay(timeAdder.getCommittedAct()));
    timeData.put(ID_TIME_COMPLETED + ID_ACT, ProjectAction.getTimeForDisplay(timeAdder.getCompletedAct()));
    timeData.put(ID_TIME_COMPLETED + ID_EST, ProjectAction.getTimeForDisplay(timeAdder.getCompletedAct()));
    timeData.put(ID_TIME_WILL_MEET + ID_EST, ProjectAction.getTimeForDisplay(timeAdder.getWillMeetEst()));
    timeData.put(ID_TIME_WILL_MEET + ID_ACT, ProjectAction.getTimeForDisplay(timeAdder.getWillMeetAct()));

    // Use Jackson to convert the map to JSON and write it to the response
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(out, timeData);
  }

  private void printActionNow(AppReq appReq, WebUser webUser, PrintWriter out, Project project,
      ProjectAction completingAction, List<Project> projectList, List<ProjectContact> projectContactList,
      Set<String> formNameSet) {
    String formName = "" + completingAction.getActionId();
    out.println(
        "<form name=\"actionNowForm\" id=\"actionNowForm\" method=\"post\" action=\"ProjectActionServlet\">");
    out.println(
        "<input type=\"hidden\" name=\"" + PARAM_PROJECT_ID + "\" value=\"" + project.getProjectId() + "\">");
    if (completingAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_ID + "\" value=\""
          + completingAction.getActionId() + "\">");
    }
    printActionNow(appReq, webUser, out, completingAction, projectContactList,
        FORM_ACTION_NOW);
    printPressEnterScript(out);
    printFetchAndUpdateTimesScript(out, completingAction);
    out.println("</form>");

    printEditProjectActionForm(appReq, completingAction, projectContactList, formName, formNameSet, project,
        projectList);

  }

  private List<Project> getProjectList(WebUser webUser, Session dataSession) {
    List<Project> projectList;
    {

      String queryString = "from Project where provider = ?";
      queryString += " and phaseCode <> 'Clos'";
      queryString += " order by projectName";
      Query query = dataSession.createQuery(queryString);
      query.setParameter(0, webUser.getProvider());
      projectList = query.list();
    }
    return projectList;
  }

  private void printActionsNext(AppReq appReq, Project project, List<ProjectContact> projectContactList,
      List<Project> projectList, List<ProjectAction> projectActionTakenList,
      List<ProjectAction> projectActionScheduledList, Set<String> formNameSet) {

    WebUser webUser = appReq.getWebUser();
    PrintWriter out = appReq.getOut();
    out.println("<h2>" + project.getProjectName());
    out.println("</h2>");
    {
      String link = "ProjectEditServlet?" + PARAM_PROJECT_ID + "=" + project.getProjectId();
      if (project.getDescription() == null || project.getDescription().equals("")) {
        // put edit button here
        out.println("<p><a href=\"" + link + "\" class=\"edit-link\">Add Description</a></p>");
      } else {
        out.println("<p>" + project.getDescription() + " <a href=\"" + link + "\" class=\"edit-link\">Edit</a></p>");
      }
    }
    out.println("<button id=\"editButton0\" type=\"button\">Add Action</button>");
    {
      String link = "ProjectActionServlet?" + PARAM_PROJECT_ID + "=" + project.getProjectId() + "&" + PARAM_ACTION
          + "=" + ACTION_SUGGEST;
      // print out button that will use link
      out.println("<button id=\"suggestButton0\" type=\"button\" onclick=\"window.location.href='" + link
          + "'\">Suggest</button>");
    }

    List<String> dateLabelList = new ArrayList<String>();
    Map<String, List<ProjectAction>> projectActionMap = new HashMap<String, List<ProjectAction>>();
    setupListAndMap(webUser, projectActionScheduledList, dateLabelList, projectActionMap);

    // Go through dateLabelList and print out actions for each entry, as a list
    for (String label : dateLabelList) {
      List<ProjectAction> paList = projectActionMap.get(label);
      if (paList != null && paList.size() > 0) {
        out.println("<h4>" + label + "</h4>");
        out.println("<ul>");
        for (ProjectAction pa : paList) {
          out.println("<li>");
          out.println(
              "<a href=\"ProjectActionServlet?" + PARAM_COMPLETING_ACTION_ID + "=" + pa.getActionId() + "\">"
                  + pa.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a>");
          out.println(" <a href=\"javascript: void(0); \" onclick=\" document.getElementById('formDialog"
              + pa.getActionId() + "').style.display = 'flex';\" class=\"edit-link\">Edit</a>");
          out.println("</li>");
        }
        out.println("</ul>");
      }
    }
    printEditProjectActionForm(appReq, null, projectContactList, "" +
        0, formNameSet, project, projectList);
    for (ProjectAction pa : projectActionScheduledList) {
      printEditProjectActionForm(appReq, pa, projectContactList, "" +
          pa.getActionId(), formNameSet, project, projectList);
    }

    if (projectActionTakenList.size() > 0) {
      out.println("<h4>Recent Past Actions</h4>");
      out.println("<ul>");
      int maxCount = 12;
      // need Mon 10/25 date pattern
      SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd");
      for (ProjectAction pa : projectActionTakenList) {
        out.println("<li>");
        out.println(sdf.format(pa.getActionDate()) + ": ");
        out.println(pa.getActionDescription());
        out.println("</li>");
        maxCount--;
        if (maxCount == 0) {
          break;
        }
      }
      out.println("</ul>");
    }

  }

  private List<ProjectAction> getAllProjectActionsScheduledList(AppReq appReq, Project project, Session dataSession) {
    List<ProjectAction> allProjectActionsList = null;
    {
      Query query = dataSession.createQuery(
          "from ProjectAction where projectId = ? and nextDescription <> '' and nextActionId = 0 order by nextDue asc");
      query.setParameter(0, project.getProjectId());
      allProjectActionsList = query.list();
      for (ProjectAction pa : allProjectActionsList) {
        setupProjectAction(dataSession, pa);
      }
    }
    return allProjectActionsList;
  }

  private void printPressEnterScript(PrintWriter out) {
    out.println("<script>");
    out.println("document.getElementById(\"nextNotes\").addEventListener(\"keydown\", function(event) {");
    out.println("  if (event.key === \"Enter\" && !event.shiftKey) { // Shift + Enter for new line");
    out.println("    event.preventDefault(); // Prevent the default Enter behavior in textarea");
    out.println("    // Set the desired action value\n");
    out.println("    const form = document.getElementById(\"actionNowForm\");");
    out.println("    const actionInput = document.createElement(\"input\");");
    out.println("    actionInput.type = \"hidden\";");
    out.println("    actionInput.name = \"action\";");
    out.println("    actionInput.value = \"Note\";");
    out.println("    // Add this temporary hidden input to the form and submit");
    out.println("    form.appendChild(actionInput);");
    out.println("    console.log(\"submitting form\");");
    out.println("    console.log(form);");
    out.println("    console.log(typeof form.submit);");
    out.println("    form.submit();");
    out.println("    // Remove the temporary input to prevent it from persisting");
    out.println("    form.removeChild(actionInput);");
    out.println("  }");
    out.println("});");
    out.println("</script>");
  }

  private void printFetchAndUpdateTimesScript(PrintWriter out, ProjectAction completingAction) {
    String link = "ProjectActionServlet?" + PARAM_ACTION + "=" + ACTION_REFRESH_TIME + "&" + PARAM_COMPLETING_ACTION_ID
        + "=" + completingAction.getActionId();
    out.println("<script>");
    out.println("async function fetchAndUpdateTimes() { ");
    out.println("    try { ");
    out.println("        const response = await fetch('" + link + "'); ");
    out.println("        const data = await response.json();\n");
    out.println("        // Update each cell based on the JSON keys\n");
    out.println(
        "        document.getElementById(\"" + ID_TIME_RUNNING + "\").innerText = data." + ID_TIME_RUNNING + ";\n");
    out.println("        document.getElementById(\"" + ID_TIME_TODAY + "\").innerText = data." + ID_TIME_TODAY + ";\n");
    printFieldTransferScript(out, ID_TIME_COMMITTED);
    printFieldTransferScript(out, ID_TIME_WILL_MEET);
    printFieldTransferScript(out, ID_TIME_WILL);
    printFieldTransferScript(out, ID_TIME_MIGHT);
    printFieldTransferScript(out, ID_TIME_OTHER);
    out.println("    } catch (error) {\n");
    out.println("        console.error(\"Error fetching time updates:\", error);\n");
    out.println("    }\n");
    out.println("}\n");
    out.println("\n");
    out.println("// Fetch and update every 60 seconds (1 minute)\n");
    out.println("setInterval(fetchAndUpdateTimes, 60000);");
    out.println("fetchAndUpdateTimes();");
    out.println("</script>");
  }

  private void printFieldTransferScript(PrintWriter out, String field) {
    String fieldEst = field + ID_EST;
    String fieldAct = field + ID_ACT;
    out.println("        document.getElementById(\"" + fieldEst + "\").innerText = data." + fieldEst + ";\n");
    out.println("        document.getElementById(\"" + fieldAct + "\").innerText = data." + fieldAct + ";\n");
  }

  private void setupListAndMap(WebUser webUser, List<ProjectAction> allProjectActionsList,
      List<String> dateLabelList, Map<String, List<ProjectAction>> projectActionMap) {
    // create a list of Date objects, starting with today and going out at least 7
    // days until the following Friday
    Map<Date, String> dateToLabelMap = new HashMap<Date, String>();
    dateLabelList.add("Goals");
    dateLabelList.add("Overdue");
    List<Date> dateList = new ArrayList<Date>();

    Calendar c = getCalendarForTodayNoTime(webUser);
    int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
    int count = 0;
    boolean nextWeek = false;
    SimpleDateFormat daynameSdf = new SimpleDateFormat("EEEE");
    while (count < 7 || dayOfWeek != Calendar.SATURDAY) {
      String label = daynameSdf.format(c.getTime());
      if (count > 1 && dayOfWeek == Calendar.MONDAY) {
        nextWeek = true;
      }
      if (count == 0) {
        label = "Today";
      } else if (nextWeek) {
        label = "Next " + label;
      }
      dateLabelList.add(label);
      Date date = c.getTime();
      dateList.add(date);
      dateToLabelMap.put(date, label);
      c.add(Calendar.DAY_OF_MONTH, 1);
      dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
      count++;
    }
    dateLabelList.add("Proposed");
    dateLabelList.add("Templates");

    for (ProjectAction pa : allProjectActionsList) {
      if (pa.getNextActionType().equals(ProjectNextActionType.GOAL)) {
        addToMap(projectActionMap, pa, "Goals");
      } else if (pa.isTemplate()) {
        addToMap(projectActionMap, pa, "Templates");
      } else if (pa.getNextActionStatus() != null
          && pa.getNextActionStatus().equals(ProjectNextActionStatus.PROPOSED)) {
        addToMap(projectActionMap, pa, "Proposed");
      } else if (pa.hasNextDue()) {
        String label = "Overdue";
        if (pa.getNextDue().before(dateList.get(0))) {
          addToMap(projectActionMap, pa, label);
        } else {
          label = "Today";
          boolean found = false;
          for (int i = 0; i < dateList.size(); i++) {
            Date date = dateList.get(i);
            if (sameDayOrBefore(pa, date)) {
              label = dateToLabelMap.get(date);
              addToMap(projectActionMap, pa, label);
              found = true;
              break;
            }
          }
          if (!found) {
            addToMap(projectActionMap, pa, "Due Later");
          }
        }
      } else {
        addToMap(projectActionMap, pa, "Sometime");
      }
    }
  }

  private static boolean sameDayOrBefore(ProjectAction pa, Date dateFromDateList) {
    Date nextDue = pa.getNextDue();
    // need to chop off the time from the date
    Calendar c = Calendar.getInstance();
    c.setTime(dateFromDateList);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    dateFromDateList = c.getTime();
    c = Calendar.getInstance();
    c.setTime(nextDue);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    nextDue = c.getTime();

    return dateFromDateList.equals(nextDue) || nextDue.before(dateFromDateList);
  }

  private static boolean before(ProjectAction pa, Date dateFromDateList) {
    Date nextDue = pa.getNextDue();
    // need to chop off the time from the date
    Calendar c = Calendar.getInstance();
    c.setTime(dateFromDateList);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    dateFromDateList = c.getTime();
    c = Calendar.getInstance();
    c.setTime(nextDue);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    nextDue = c.getTime();

    return nextDue.before(dateFromDateList);
  }

  private void addToMap(Map<String, List<ProjectAction>> projectActionMap, ProjectAction pa, String key) {
    List<ProjectAction> paList = projectActionMap.get(key);
    if (paList == null) {
      paList = new ArrayList<ProjectAction>();
      projectActionMap.put(key, paList);
    }
    paList.add(pa);
  }

  private void readCompletingAction(HttpServletRequest request, AppReq appReq, Session dataSession) {
    String completingActionIdString = request.getParameter(PARAM_COMPLETING_ACTION_ID);
    if (completingActionIdString != null) {
      ProjectAction completingProjectAction = (ProjectAction) dataSession.get(ProjectAction.class,
          Integer.parseInt(completingActionIdString));
      setupProjectActionAndSaveToAppReq(appReq, dataSession, completingProjectAction);
    }
  }

  private ProjectAction readEditProjectAction(AppReq appReq) {
    Session dataSession = appReq.getDataSession();
    HttpServletRequest request = appReq.getRequest();
    String actionIdString = request.getParameter(PARAM_EDIT_ACTION_ID);
    ProjectAction editProjectAction = null;
    if (actionIdString != null) {
      editProjectAction = (ProjectAction) dataSession.get(ProjectAction.class,
          Integer.parseInt(actionIdString));
      setupProjectAction(dataSession, editProjectAction);
    }
    return editProjectAction;
  }

  private ProjectAction saveProjectAction(AppReq appReq,
      ProjectAction editProjectAction, Project nextProject) {
    HttpServletRequest request = appReq.getRequest();
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    if (editProjectAction == null) {
      editProjectAction = new ProjectAction();
      editProjectAction.setProject(nextProject);
      editProjectAction.setProjectId(nextProject.getProjectId());
      editProjectAction.setActionDescription("");
    }
    editProjectAction.setContactId(webUser.getContactId());
    editProjectAction.setContact(webUser.getProjectContact());
    int nextTimeEstimate = 0;
    if (request.getParameter(PARAM_NEXT_TIME_ESTIMATE) != null) {
      try {
        nextTimeEstimate = Integer.parseInt(request.getParameter(PARAM_NEXT_TIME_ESTIMATE));
      } catch (NumberFormatException nfe) {
        nextTimeEstimate = 0;
      }
    }
    editProjectAction.setActionDate(new Date());
    editProjectAction.setActionDescription("");
    editProjectAction.setNextDescription(trim(request.getParameter(PARAM_NEXT_DESCRIPTION), 1200));
    if (nextTimeEstimate > 0) {
      editProjectAction.setNextTimeEstimate(nextTimeEstimate);
    }
    editProjectAction.setNextActionId(0);

    String nextNote = request.getParameter(PARAM_NEXT_NOTE);
    if (nextNote != null && nextNote.length() > 0) {
      if (editProjectAction.getNextNotes() != null && editProjectAction.getNextNotes().trim().length() > 0) {
        nextNote = editProjectAction.getNextNotes() + "\n - " + nextNote;
      } else {
        nextNote = LIST_START + nextNote;
      }
      editProjectAction.setNextNotes(nextNote);
    }

    editProjectAction.setNextDue(ProjectServlet.parseDate(appReq, request.getParameter(PARAM_NEXT_DUE)));
    editProjectAction.setNextDeadline(ProjectServlet.parseDate(appReq, request.getParameter(PARAM_NEXT_DEADLINE)));
    String linkUrl = request.getParameter(PARAM_LINK_URL);
    if (linkUrl == null || linkUrl.equals("")) {
      editProjectAction.setLinkUrl(null);
    } else {
      editProjectAction.setLinkUrl(linkUrl);
    }
    String templateTypeString = request.getParameter(PARAM_TEMPLATE_TYPE);
    if (templateTypeString == null || templateTypeString.equals("")) {
      editProjectAction.setTemplateType(null);
    } else {
      editProjectAction.setTemplateType(TemplateType.getTemplateType(templateTypeString));
    }
    String prioritySpecialString = request.getParameter(PARAM_PRIORITY_SPECIAL);
    if (prioritySpecialString == null || prioritySpecialString.equals("")) {
      editProjectAction.setPrioritySpecial(null);
    } else {
      editProjectAction.setPrioritySpecial(PrioritySpecial.getPrioritySpecial(prioritySpecialString));
    }

    String nextActionType = request.getParameter(PARAM_NEXT_ACTION_TYPE);
    editProjectAction.setNextActionType(nextActionType);
    int priorityLevel = editProjectAction.getProject().getPriorityLevel();
    editProjectAction.setPriorityLevel(priorityLevel);
    String nextContactIdString = request.getParameter(PARAM_NEXT_CONTACT_ID);
    if (nextContactIdString != null && nextContactIdString.length() > 0) {
      editProjectAction.setNextContactId(Integer.parseInt(nextContactIdString));
      editProjectAction.setNextProjectContact(
          (ProjectContact) dataSession.get(ProjectContact.class, editProjectAction.getNextContactId()));
    }
    editProjectAction.setProvider(webUser.getProvider());
    if (editProjectAction.getNextActionStatus() == null) {
      if (editProjectAction.hasNextDescription()) {
        if (editProjectAction.hasNextDue()) {
          editProjectAction.setNextActionStatus(ProjectNextActionStatus.READY);
        } else {
          editProjectAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
        }
      }
    }

    Transaction trans = dataSession.beginTransaction();
    dataSession.saveOrUpdate(editProjectAction);
    trans.commit();
    return editProjectAction;
  }

  private void closeAction(AppReq appReq, ProjectAction projectAction, Project project,
      String nextDescription, ProjectNextActionStatus nextActionStatus) {
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    ProjectAction nextAction = new ProjectAction();
    nextAction.setProject(project);
    nextAction.setProjectId(project.getProjectId());
    nextAction.setActionDate(new Date());
    nextAction.setActionDescription(nextDescription);
    nextAction.setNextDescription("");
    nextAction.setProvider(webUser.getProvider());
    nextAction.setContact(webUser.getProjectContact());
    Transaction trans = dataSession.beginTransaction();
    dataSession.saveOrUpdate(nextAction);
    projectAction.setNextActionId(nextAction.getActionId());
    projectAction.setNextActionStatus(nextActionStatus);
    dataSession.update(projectAction);
    trans.commit();
  }

  private String sendEmail(HttpServletRequest request, AppReq appReq, WebUser webUser, Session dataSession,
      Project project, ProjectAction completingAction, String emailBody) {
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
          .getParameter(PARAM_SEND_EMAIL_TO + projectContactAssigned.getId().getContactId()) != null) {
        query = dataSession.createQuery("from ProjectContact where contactId = ?");
        query.setParameter(0, projectContactAssigned.getId().getContactId());
        ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
        sendEmailToList.add(projectContact);
      }
    }
    if (!userAssignedToProject) {
      ProjectServlet.assignContact(project.getProjectId(), dataSession, webUser.getContactId());
    }

    if (sendEmailToList.size() > 0) {

      StringBuilder msg = new StringBuilder();
      msg.append("<p><i>");
      msg.append(webUser.getProjectContact().getName());
      msg.append(" writes: </i>");
      msg.append(completingAction.getActionDescription());
      msg.append("</p>");
      if (completingAction.getNextDescription() != null
          && !completingAction.getNextDescription().equals("")) {
        msg.append("<p>");
        msg.append(completingAction.getNextDescriptionForDisplay(webUser.getProjectContact()));
        if (completingAction.getNextDue() != null) {
          SimpleDateFormat sdf11 = webUser.getDateFormat();
          msg.append(" ");
          msg.append(sdf11.format(completingAction.getNextDue()));
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
        appReq.setMessageProblem("Unable to send email: " + e.getMessage());
        emailBody = null;
      }
    }
    return emailBody;
  }

  private void stopTimer(AppReq appReq, Session dataSession) {
    TimeTracker timeTracker = appReq.getTimeTracker();
    if (timeTracker != null) {
      timeTracker.stopClock(dataSession);
    }
  }

  private void startTimer(AppReq appReq, Session dataSession, ProjectAction completingAction) {
    TimeTracker timeTracker = appReq.getTimeTracker();
    if (timeTracker != null && completingAction != null) {
      timeTracker.startClock(completingAction.getProject(), completingAction, dataSession);
    }
  }

  private void readNotesSummaryUpdateTimeTracker(HttpServletRequest request, AppReq appReq, Session dataSession,
      ProjectAction completingAction) {
    String nextSummary = request.getParameter(PARAM_NEXT_SUMMARY);
    String nextNotes = request.getParameter(PARAM_NEXT_NOTES);
    boolean isChanged = false;
    if (nextSummary != null) {
      completingAction.setNextSummary(nextSummary);
      isChanged = true;
    }
    if (nextNotes != null && nextNotes.length() > 0) {
      if (completingAction.getNextNotes() != null && completingAction.getNextNotes().trim().length() > 0) {
        nextNotes = completingAction.getNextNotes() + "\n - " + nextNotes;
      } else {
        nextNotes = LIST_START + nextNotes;
      }
      completingAction.setNextNotes(nextNotes);
      isChanged = true;
    }
    TimeTracker timeTracker = appReq.getTimeTracker();
    if (timeTracker != null && timeTracker.isRunningClock()) {
      int mins = timeTracker.getTotalMinsForAction(completingAction);
      completingAction.setNextTimeActual(mins);
      isChanged = true;
    }
    {
      // query the Bill Entry table for the bill entry with the same actionId, sum up
      // the time spent
      // Here is the query: select sum(bill_mins) from bill_entry where action_id =
      // {action_id}
      Query query = dataSession.createQuery("select sum(billMins) from BillEntry where action = :action "
          + "and startTime >= :today and startTime < :tomorrow");
      query.setParameter("action", completingAction);
      Calendar calendar = getCalendarForTodayNoTime(appReq.getWebUser());
      query.setParameter("today", calendar.getTime());
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      query.setParameter("tomorrow", calendar.getTime());
      List<Long> billMinsList = query.list();
      if (billMinsList.size() > 0) {
        if (billMinsList.get(0) != null) {
          int billMins = billMinsList.get(0).intValue();
          if (completingAction.getNextTimeActual() == null || completingAction.getNextTimeActual() != billMins) {
            completingAction.setNextTimeActual(billMins);
            isChanged = true;
          }
        }
      }
    }
    if (isChanged) {
      Transaction transaction = dataSession.beginTransaction();
      dataSession.update(completingAction);
      transaction.commit();
    }
  }

  private Calendar getCalendarForTodayNoTime(WebUser webUser) {
    Calendar calendar = webUser.getCalendar();
    calendar.setTime(new Date());
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar;
  }

  private void chatPropose(AppReq appReq, ProjectAction completingAction, List<ChatAgent> chatAgentList,
      List<ProjectAction> projectActionTakenList, List<ProjectAction> projectActionScheduledList) {

    String proposePrompt = getProposePrompt(completingAction, appReq, projectActionTakenList,
        projectActionScheduledList);

    ChatAgent chatAgent = null;
    chatAgent = new ChatAgent("Propose", SYSTEM_INSTRUCTIONS);
    chatAgent.chat(proposePrompt);
    chatAgentList.add(chatAgent);

    if (chatAgent.hasResponse()) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String likelyDatePrefix1 = sdf.format(new Date());
      String likelyDatePrefix2 = "- " + likelyDatePrefix1;
      String chatGPTResponse = chatAgent.getResponseText();
      String nextSummary;
      if (chatGPTResponse.startsWith(likelyDatePrefix1)) {
        nextSummary = chatGPTResponse.substring(likelyDatePrefix1.length()).trim();
      } else if (chatGPTResponse.startsWith(likelyDatePrefix2)) {
        nextSummary = chatGPTResponse.substring(likelyDatePrefix2.length()).trim();
      } else {
        nextSummary = chatGPTResponse;
      }
      completingAction.setNextSummary(nextSummary);
    }
  }

  private void chatFeedback(AppReq appReq, ProjectAction completingAction,
      List<ChatAgent> chatAgentList, List<ProjectAction> projectActionTakenList,
      List<ProjectAction> projectActionScheduledList) {

    String feedbackPrompt = getFeedbackPrompt(completingAction, appReq, projectActionTakenList,
        projectActionScheduledList);

    ChatAgent chatAgent = null;
    chatAgent = new ChatAgent("Feedback", SYSTEM_INSTRUCTIONS);
    chatAgent.setResponseFormat(ChatAgent.RESPONSE_FORMAT_JSON);
    chatAgent.chat(feedbackPrompt);
    if (chatAgent.hasResponse()) {
      try {
        String json = chatAgent.getResponseText();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(json);
        String nextFeedback = jsonNode.get("html").asText();
        // set on Project Action and save
        if (nextFeedback != null) {
          Session dataSession = appReq.getDataSession();
          completingAction.setNextFeedback(nextFeedback);
          Transaction transaction = dataSession.beginTransaction();
          dataSession.update(completingAction);
          transaction.commit();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  private String chatNext(AppReq appReq, ProjectAction completingAction,
      List<ChatAgent> chatAgentList, List<ProjectAction> projectActionTakenList,
      List<ProjectAction> projectActionScheduledList) {

    String nextPrompt = getNextPrompt(completingAction, appReq, projectActionTakenList, projectActionScheduledList);

    String nextSuggest = null;
    ChatAgent chatAgent = null;
    chatAgent = new ChatAgent("Next Steps", SYSTEM_INSTRUCTIONS);

    chatAgent.setResponseFormat(ChatAgent.RESPONSE_FORMAT_JSON);
    chatAgent.chat(nextPrompt);
    if (chatAgent.hasResponse()) {
      try {

        String json = chatAgent.getResponseText();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(json);
        nextSuggest = jsonNode.get("html").asText();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return nextSuggest;
  }

  private String getProposePrompt(ProjectAction completingAction, AppReq appReq,
      List<ProjectAction> projectActionTakenList, List<ProjectAction> projectActionScheduledList) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    String proposePrompt = getBasePrompt(completingAction, appReq, projectActionTakenList, projectActionScheduledList);
    proposePrompt += "The recent actions taken are previously generated summaries of actions taken for this project. "
        + "Please create a succinct summary of what action was taken based on the next action I am working on and the next action notes I took (if any) "
        + "while completing this action. "
        + "Keep in mind that this summary will be added to the list of recent actions taken, so should be in the same format as the other summaries. "
        + "Be careful to only document what has occured and do not mention what is planned to happen next. \n";
    proposePrompt += "I will report this and the list of completed actions to my supervisor and other contacts as this action having been completed on today's date "
        + sdf.format(new Date())
        + ". Please give me only the text of the update, as it would appear after the date and no other commentary. Thanks!";

    return proposePrompt;
  }

  private String getFeedbackPrompt(ProjectAction completingAction, AppReq appReq,
      List<ProjectAction> projectActionTakenList, List<ProjectAction> projectActionScheduledList) {
    String feedbackPrompt = getBasePrompt(completingAction, appReq, projectActionTakenList, projectActionScheduledList);
    feedbackPrompt += "Please review my current summary and give me three to five questions my superisor or others might have that could help me clarify and add more detail to this update. "
        + " Please give this to me as a list of items for me to consider. Give this to me as list of unordered items in an HTML list. "
        + " Send me a JSON response where the key 'html' contains the HTML code for a list of suggestions.. Thanks! \n";

    return feedbackPrompt;
  }

  private String getNextPrompt(ProjectAction completingAction, AppReq appReq,
      List<ProjectAction> projectActionTakenList, List<ProjectAction> projectActionScheduledList) {
    String nextPrompt = getBasePrompt(completingAction, appReq, projectActionTakenList, projectActionScheduledList);
    nextPrompt += "Please review my current summary of what I accomplished and what I have accomplished in the past with this project and suggest a list "
        + "of two or three next steps I should indicate in my project tracker. These statements should be short and should start with one of these phrases:  "
        + "I will ..., I might ..., I have committed ..., I will meet ..., I will review ..., I will document ..., I will follow up ..., or I am waiting for ... \n\n";
    List<ProjectAction> projectActionsScheduledAndCompletedList = getProjectActionsScheduledAndCompletedList(
        appReq.getDataSession(), completingAction.getProject().getProjectId());
    if (projectActionsScheduledAndCompletedList.size() > 0) {
      nextPrompt += "Here are examples of steps previously taken on this project: \n";
      for (ProjectAction projectAction : projectActionsScheduledAndCompletedList) {
        nextPrompt += LIST_START + projectAction.getNextDescription() + " \n";
      }
    }
    nextPrompt += " Please give this to me as a list of items for me to consider. Give this to me as list of unordered items in an HTML list. "
        + " Send me a JSON response where the key 'html' contains the HTML code for a list of suggestions. Thanks! \n";
    return nextPrompt;
  }

  private String getBasePrompt(ProjectAction completingAction, AppReq appReq,
      List<ProjectAction> projectActionTakenList, List<ProjectAction> projectActionScheduledList) {
    Project project = completingAction.getProject();
    WebUser webUser = appReq.getWebUser();
    String basePrompt = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    basePrompt = "I have taken action on a recent project and need to provide an update to be included in reporting to my "
        + "supervisor and other project participants.  \n\n"
        + "Project name: " + project.getProjectName() + " \n"
        + "Project description: " + project.getDescription() + " \n"
        + "Recent actions taken: \n";
    int limit = 12;
    for (ProjectAction pa : projectActionTakenList) {
      basePrompt += LIST_START + sdf.format(pa.getActionDate()) + " " + pa.getActionDescription() + " \n";
      limit--;
      if (limit == 0) {
        break;
      }
    }
    if (projectActionScheduledList.size() > 1) {
      basePrompt += "Actions scheduled in the future: \n";
      for (ProjectAction projectAction : projectActionScheduledList) {
        if (projectAction.equals(completingAction)) {
          continue;
        }
        basePrompt += LIST_START + sdf.format(projectAction.getNextDue()) + " "
            + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + " \n";
      }
    }
    basePrompt += "Now working on this next action: "
        + completingAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + " \n";
    if (completingAction.getNextNotes() != null && completingAction.getNextNotes().length() > 0) {
      basePrompt += "Next action notes: \n" + completingAction.getNextNotes() + " \n";
    }

    if (completingAction.getNextSummary() != null && completingAction.getNextSummary().length() > 0) {
      basePrompt += "The current summary is: \n" + completingAction.getNextSummary() + " \n";
    }
    return basePrompt;
  }

  private void printEditProjectActionForm(AppReq appReq, ProjectAction editProjectAction,
      List<ProjectContact> projectContactList, String formName, Set<String> formNameSet, Project project,
      List<Project> projectList) {
    if (formNameSet.contains(formName)) {
      return;
    }
    formNameSet.add(formName);
    WebUser webUser = appReq.getWebUser();
    PrintWriter out = appReq.getOut();
    out.println("<div id=\"formDialog" + formName + "\" class=\"dialog\">");
    out.println("<form name=\"projectAction" + formName
        + "\" method=\"post\" action=\"ProjectActionServlet\" id=\"" + SAVE_PROJECT_ACTION_FORM + formName
        + "\" class=\"editForm\">");
    SimpleDateFormat sdf = webUser.getDateFormat();
    out.println(" <script>");
    out.println("  document.addEventListener('DOMContentLoaded', function() {");
    out.println("      const editButton = document.getElementById('editButton" + formName + "');");
    out.println("      const cancelButton = document.getElementById('cancelButton" + formName + "');");
    out.println("      const formDialog = document.getElementById('formDialog" + formName + "');");
    out.println("      if (editButton != null) {");
    out.println("        editButton.addEventListener('click', () => {");
    out.println("          formDialog.style.display = 'flex'; // Display the dialog");
    out.println("        });");
    out.println("      }");
    out.println("      if (cancelButton != null) {");
    out.println("        cancelButton.addEventListener('click', () => {");
    out.println("          formDialog.style.display = 'none';");
    out.println("        });");
    out.println("      }");
    out.println("  });");
    out.println("    function selectProjectActionType" + formName + "(actionType)");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm" + formName + "'];");
    out.println("      var found = false; ");
    out.println("      var label = makeIStatement" + formName
        + "(actionType, form.nextContactId.options[form.nextContactId.selectedIndex].text);");
    out.println("      form." + PARAM_START_SENTANCE + ".value = label;");
    out.println("      form.nextActionType.value = actionType;");
    out.println("      enableForm" + formName + "(); ");
    out.println("    }");
    out.println("    ");
    out.println("    function enableForm" + formName + "()");
    out.println("    {");
    out.println("      var form = document.forms['" + SAVE_PROJECT_ACTION_FORM + formName + "'];");
    out.println("      form." + PARAM_NEXT_DUE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_DESCRIPTION + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_CONTACT_ID + ".disabled = false;");
    out.println("      form." + PARAM_START_SENTANCE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_TIME_ESTIMATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_DEADLINE + ".disabled = false;");
    out.println("      form." + PARAM_LINK_URL + ".disabled = false;");
    out.println("      form." + PARAM_TEMPLATE_TYPE + ".disabled = false;");
    out.println("      form." + PARAM_PRIORITY_SPECIAL + ".disabled = false;");
    out.println("      if (form." + PARAM_NEXT_DUE + ".value == \"\")");
    out.println("      {");
    out.println("       document.projectAction" + formName + "." + PARAM_NEXT_DUE + ".value = '"
        + sdf.format(new Date()) + "';");
    out.println("      }");
    out.println("    }");
    out.println("    function setNextAction" + formName + "(nextActionDate) {");
    out.println("      document.projectAction" + formName + "." + PARAM_NEXT_DUE + ".value = nextActionDate;");
    out.println("      enableForm" + formName + "(); ");
    out.println("    }");
    out.println("    function setNextDeadline" + formName + "(nextDeadline) {");
    out.println("      document.projectAction" + formName + "." + PARAM_NEXT_DEADLINE + ".value = nextDeadline;");
    out.println("    }");
    ProjectServlet.printGenerateSelectNextTimeEstimateFunction(out, formName);
    out.println("  </script>");
    if (editProjectAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_EDIT_ACTION_ID + "\" value=\""
          + editProjectAction.getActionId() + "\">");
    }
    printCurrentActionEdit(appReq, webUser, out, editProjectAction, project, projectContactList, formName, projectList);

    out.println("</form>");
    out.println("</div>");
  }

  private void printActionsScheduledForToday(WebUser webUser, PrintWriter out, List<ProjectAction> projectActionList) {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"4\">All actions scheduled for today</th>");
    out.println("  </tr>");
    printDueTable(webUser, out, ProjectNextActionType.OVERDUE_TO, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.COMMITTED_TO, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_CONTACT, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_MEET, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.MIGHT, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.GOAL, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_FOLLOW_UP, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WAITING, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_REVIEW, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_DOCUMENT, projectActionList);
    out.println("</table><br/>");
  }

  private void printActionsScheduledForNextWorkingDay(WebUser webUser, PrintWriter out,
      List<ProjectAction> projectActionList, Date workingDay) {
    String title = "All actions scheduled for ";
    // print name of next working day, either "Monday" or "Next Monday". that format
    Calendar nextWorkingDay = Calendar.getInstance();
    nextWorkingDay.setTime(workingDay);
    SimpleDateFormat daynameSdf = new SimpleDateFormat("EEEE dd MMMM yyyy");
    title += daynameSdf.format(nextWorkingDay.getTime());
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"4\">" + title + "</th>");
    out.println("  </tr>");
    printDueTable(webUser, out, ProjectNextActionType.OVERDUE_TO, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.COMMITTED_TO, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_CONTACT, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_MEET, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.MIGHT, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.GOAL, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_FOLLOW_UP, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WAITING, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_REVIEW, projectActionList);
    printDueTable(webUser, out, ProjectNextActionType.WILL_DOCUMENT, projectActionList);
    out.println("</table><br/>");
    // print out size of list
  }

  private void printActionsCompletedForToday(WebUser webUser, PrintWriter out, List<ProjectAction> projectActionList) {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"4\">All actions completed for today</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Completed</th>");
    out.println("    <th class=\"boxed\">Est</th>");
    out.println("    <th class=\"boxed\">Act</th>");
    out.println("  </tr>");
    printActionItems(webUser, out, projectActionList);
    out.println("</table><br/>");
  }

  private void printTimeManagementBox(AppReq appReq, List<ProjectAction> projectActionList) {
    PrintWriter out = appReq.getOut();
    TimeAdder timeAdder = new TimeAdder(projectActionList, appReq);
    out.println("<table class=\"boxed float-right\">");
    printTimeTotal(out, "Completed", ID_TIME_COMPLETED, timeAdder.getCompletedAct(), timeAdder.getCompletedAct());
    printTimeTotal(out, "Will Meet", ID_TIME_WILL_MEET, timeAdder.getWillEst(), timeAdder.getWillAct());
    printTimeTotal(out, "Committed", ID_TIME_COMMITTED, timeAdder.getCommittedEst(), timeAdder.getCommittedAct());
    printTimeTotal(out, "Will", ID_TIME_WILL, timeAdder.getWillEst(), timeAdder.getWillAct());
    printTimeTotal(out, "Might", ID_TIME_MIGHT, timeAdder.getMightEst(), timeAdder.getMightAct());
    printTimeTotal(out, "Other", ID_TIME_OTHER, timeAdder.getOtherEst(), timeAdder.getOtherAct());
    out.println("</table>");
    out.println("<h3 id=\"" + ID_TIME_TODAY + "\">" + getFullDayAndTime() + "</h3>");
    if ((timeAdder.getCommittedEst() + timeAdder.getWillMeetEst() + timeAdder.getWillEst()) == 0) {
      out.println("<p>You have finished everything you said you would do today. Good job! </p>");
    } else if (timeAdder.getCompletedAct() > (8 * 60)) {
      out.println(
          "<p><span class=\"fail\">Time to be done!</span> You have spent a full day working already. You should not be working now. </p>");
    } else if (timeAdder.getWillAct() > (8 * 60)) {
      out.println("<p><span class=\"fail\">You are over committed for today.</span> Time to re-plan your day. </p>");
    } else if (timeAdder.getCompletedAct() < 30) {
      out.println("<p>Good morning! Welcome to another day of productivity. </p>");
    } else {
      out.println("<p>Good job! You are on track to finish your day on time. </p>");
    }
    Random Random = new Random();
    String quote = QUOTES[Random.nextInt(QUOTES.length)];
    out.println("<h4>Get Inspired</h4>");
    out.println("<q>" + quote + "</q>");
  }

  private void printTimeManagementBoxForNextWorkingDay(AppReq appReq, List<ProjectAction> projectActionList,
      Date date) {
    PrintWriter out = appReq.getOut();
    TimeAdder timeAdder = new TimeAdder(projectActionList, appReq, date);
    out.println("<table class=\"boxed\">");
    printTimeTotal(out, "Completed", ID_TIME_COMPLETED, timeAdder.getCompletedAct(), timeAdder.getCompletedAct());
    printTimeTotal(out, "Will Meet", ID_TIME_WILL_MEET, timeAdder.getWillEst(), timeAdder.getWillAct());
    printTimeTotal(out, "Committed", ID_TIME_COMMITTED, timeAdder.getCommittedEst(), timeAdder.getCommittedAct());
    printTimeTotal(out, "Will", ID_TIME_WILL, timeAdder.getWillEst(), timeAdder.getWillAct());
    printTimeTotal(out, "Might", ID_TIME_MIGHT, timeAdder.getMightEst(), timeAdder.getMightAct());
    printTimeTotal(out, "Other", ID_TIME_OTHER, timeAdder.getOtherEst(), timeAdder.getOtherAct());
    out.println("</table>");
    if (timeAdder.getWillAct() > (8 * 60)) {
      out.println(
          "<p><span class=\"fail\">You are over committed for this working day.</span> You need to plan this day. </p>");
    } else if (timeAdder.getWillAct() < (7 * 60)) {
      out.println("<p>You can schedule more time this working day. </p>");
    } else {
      out.println("<p>This day is full.</p>");
    }
  }

  private String getFullDayAndTime() {
    SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy HH:mm z");
    String fullDayAndTime = sdf.format(new Date());
    return fullDayAndTime;
  }

  private void printSendEmailSelection(PrintWriter out, String formName, List<ProjectContact> projectContactList) {
    out.println("<h3>Send Email</h3>");
    for (ProjectContact projectContact1 : projectContactList) {
      out.println("<span class=\"together\"><input type=\"checkbox\" name=\"" + PARAM_SEND_EMAIL_TO
          + projectContact1.getContactId() + "\" value=\"Y\"/>");
      out.println("<font size=\"-1\"><a href=\"javascript: void clickForEmail" + formName + "('"
          + projectContact1.getContactId() + "');\" class=\"button\">" + projectContact1.getName()
          + "</a></font></span>");
    }
  }

  private void printEditNextAction(HttpServletRequest request, WebUser webUser, PrintWriter out,
      ProjectAction projectAction, Project project, String formName, String disabled,
      List<ProjectContact> projectContactList,
      List<Project> projectList) {
    SimpleDateFormat sdf1;
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"inside\">");
    SimpleDateFormat sdf2 = webUser.getDateFormat("MM/dd/yyyy hh:mm aaa");
    {
      sdf1 = webUser.getDateFormat();
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Project</th>");
      out.println("          <td>");
      if (projectAction == null) {
        out.println(
            "            <select name=\"" + PARAM_NEXT_PROJECT_ID + "\" onchange=\"enableForm" + formName + "()\">");
        for (Project p : projectList) {
          out.println("              <option value=\"" + p.getProjectId() + "\""
              + (project != null && project.getProjectId() == p.getProjectId() ? " selected" : "")
              + ">" + p.getProjectName() + "</option>");
        }
        out.println("            </select>");
      } else {
        // print project name
        out.println("            " + project.getProjectName());
        out.println(
            "<input type=\"hidden\" name=\"" + PARAM_NEXT_PROJECT_ID + "\" value=\"" + projectAction.getProjectId()
                + "\">");

      }
      out.println("          </td>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">When</th>");
      {
        String nextDueString = projectAction == null || projectAction.getNextDue() == null
            ? request.getParameter(PARAM_NEXT_DUE)
            : sdf2.format(projectAction.getNextDue());
        out.println(
            "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_NEXT_DUE
                + "\" size=\"10\" value=\""
                + n(nextDueString) + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      }
      out.println("            <font size=\"-1\">");
      Calendar calendar = webUser.getCalendar();
      SimpleDateFormat day = webUser.getDateFormat("EEE");
      out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
          + sdf2.format(calendar.getTime()) + "');\" class=\"button\">Today</a>");
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
          + sdf2.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 6; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
              + sdf2.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
              + sdf2.format(calendar.getTime()) + "');\" class=\"button\">"
              + day.format(calendar.getTime()) + "</a>");

        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
          nextWeek = true;
        }
      }
      calendar.set(Calendar.MONTH, 11);
      calendar.set(Calendar.DAY_OF_MONTH, 31);
      out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
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
            + formName + "('" + ProjectNextActionType.WILL + "');\" class=\"button\"> will</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.MIGHT + "');\" class=\"button\">might</a>, ");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.WILL_CONTACT + "');\" class=\"button\">will contact</a>, ");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.WILL_MEET + "');\" class=\"button\">will meet</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.WILL_REVIEW + "');\" class=\"button\">will review</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.WILL_DOCUMENT + "');\" class=\"button\">will document</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.WILL_FOLLOW_UP + "');\" class=\"button\">will follow up</a>");
    out.println("            </font><br/>");
    out.println("            I have: ");
    out.println("            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
        + formName + "('" + ProjectNextActionType.COMMITTED_TO
        + "');\" class=\"button\">committed</a>,");
    out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
        + ProjectNextActionType.GOAL + "');\" class=\"button\">set goal</a></font>");
    out.println("            I am:");
    out.println("            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
        + formName + "('" + ProjectNextActionType.WAITING + "');\" class=\"button\">waiting</a>");
    out.println("            <br>");
    {
      String nextActionType = projectAction == null ? ProjectNextActionType.WILL
          : projectAction.getNextActionType();
      out.println("            <input type=\"hidden\" name=\"" + PARAM_NEXT_ACTION_TYPE + "\" value=\""
          + nextActionType + "\">");
      out.println("<script>");
      out.println("  window.addEventListener('load', function() { selectProjectActionType"
          + formName + "('" + nextActionType + "'); }); ");
      out.println("</script>");
    }
    out.println("            </font>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("        <tr>");
    out.println("          <th class=\"inside\">What</th>");
    out.println("          <td class=\"inside\"> ");

    out.println(
        "            <input name=\"" + PARAM_START_SENTANCE + "\" size=\"40\" value=\"I will:\"" + disabled
            + ">");
    out.println("          </td>");
    out.println("          <th class=\"inside\">Who</th>");
    out.println("          <td class=\"inside\"> ");
    out.println("              <select name=\"nextContactId\" onchange=\"selectProjectActionType"
        + formName + "(form.nextActionType.value);\"" + disabled
        + "><option value=\"\">none</option>");
    String nextContactId = n(request.getParameter(PARAM_NEXT_CONTACT_ID));
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
        "            <textarea name=\"" + PARAM_NEXT_DESCRIPTION + "\" rows=\"1\" onkeydown=\"resetRefresh()\""
            + disabled + ">" + (projectAction == null ? "" : projectAction.getNextDescription())
            + "</textarea>");
    out.println("          </td>");
    out.println("        </tr>");
    {
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Note</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><textarea rows=\"3\" name=\"" + PARAM_NEXT_NOTE
              + "\" size=\"30\" onkeydown=\"resetRefresh()\" " + disabled + ">"
              + n(projectAction == null || projectAction.getNextNotes() == null
                  ? ""
                  : projectAction.getNextNotes())
              + "</textarea></td>");
      out.println("        </tr>");
    }
    out.println("        <tr>");
    out.println("          <th class=\"inside\">Time</th>");
    out.println("          <td class=\"inside\" colspan=\"3\">");
    out.println("            <input type=\"text\" name=\"" + PARAM_NEXT_TIME_ESTIMATE + "\" size=\"3\" value=\""
        + (projectAction == null ? "" : projectAction.getNextTimeEstimateMinsForDisplay())
        + "\" onkeydown=\"resetRefresh()\"" + disabled + "> mins ");
    out.println("            <font size=\"-1\">");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('5');\" class=\"button\"> 5m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('10');\" class=\"button\"> 10m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('20');\" class=\"button\"> 20m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('30');\" class=\"button\"> 30m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('40');\" class=\"button\"> 40m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('60');\" class=\"button\"> 60m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('70');\" class=\"button\"> 70m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('90');\" class=\"button\"> 90m</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('120');\" class=\"button\"> 2h</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
        + "('240');\" class=\"button\"> 4h</a>");
    out.println("              <a href=\"javascript: void selectNextTimeEstimate" + formName
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
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_NEXT_DEADLINE
              + "\" size=\"10\" value=\""
              + n(projectAction == null || projectAction.getNextDeadline() == null
                  ? request.getParameter(PARAM_NEXT_DEADLINE)
                  : sdf1.format(projectAction.getNextDeadline()))
              + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      out.println("            <font size=\"-1\">");
      sdf1 = webUser.getDateFormat();
      calendar.add(Calendar.DAY_OF_MONTH, 2);
      out.println("              <a href=\"javascript: void setNextDeadline" + formName + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 7; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextDeadline" + formName + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextDeadline" + formName + "('"
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
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_LINK_URL
              + "\" size=\"30\" value=\""
              + n(projectAction == null || projectAction.getLinkUrl() == null
                  ? ""
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
              ? request.getParameter(PARAM_TEMPLATE_TYPE)
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
              ? request.getParameter(PARAM_PRIORITY_SPECIAL)
              : projectAction.getPrioritySpecial().getId())
          + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      // default empty option for no template
      out.println("             <option value=\"\">none</option>");
      for (PrioritySpecial prioritySpecial : PrioritySpecial.values()) {
        out.println("             <option value=\"" + prioritySpecial.getId() + "\""
            + (projectAction != null && projectAction.getPrioritySpecial() == prioritySpecial ? " selected"
                : "")
            + ">" + prioritySpecial.getLabel() + "</option>");
      }
      out.println("            </select>");
      out.println("          </td>");
      out.println("        </tr>");
    }
    out.println("      </table>");
    out.println("    </td>");
    out.println("  </tr>");
  }

  private void printActionNow(AppReq appReq, WebUser webUser, PrintWriter out, ProjectAction completingAction,
      List<ProjectContact> projectContactList,
      String formName) {
    SimpleDateFormat sdf11 = webUser.getDateFormat();
    out.println("<h3>" + completingAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</h3>");
    out.println("<p>" + getNextActionTitle(completingAction));
    out.println(" <a href=\"javascript: void(0); \" onclick=\" document.getElementById('formDialog"
        + completingAction.getActionId() + "').style.display = 'flex';\" class=\"edit-link\">Edit</a>");
    if (completingAction.getLinkUrl() != null && completingAction.getLinkUrl().length() > 0) {
      out.println("<br/>Link: <a href=\"" + completingAction.getLinkUrl() + "\" target=\"_blank\">"
          + trim(completingAction.getLinkUrl(), 40) + "</a>");
    }
    if (completingAction.getNextDeadline() != null) {
      if (completingAction.getNextDeadline().after(new Date())) {
        out.println("    <br/>Deadline: " + sdf11.format(completingAction.getNextDeadline()));
      } else {
        out.println("    <br/><span class=\"fail\">Deadline Overdue:</span> "
            + sdf11.format(completingAction.getNextDeadline()));
      }
    }
    out.println("</p>");
    out.println("<h3>Notes");
    out.println(
        "<span class=\"float-right\" style=\"font-size: 14px;\">" + getTimeString(appReq, completingAction)
            + "</span>");
    out.println("</h3>");
    if (completingAction.getNextNotes() != null) {
      out.println(convertToHtmlList(completingAction.getNextNotes()));
    }
    if (completingAction.getNextFeedback() != null && !completingAction.getNextFeedback().equals("")) {
      out.println("<h4>Next Step Suggestions</h4>");
      out.println("" + completingAction.getNextFeedback() + "");
    }
    out.println("<textarea name=\"nextNotes\" id=\"nextNotes\" rows=\"7\" onkeydown=\"resetRefresh()\"></textarea>");
    out.println("<br/><span class=\"right\">");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_NOTE + "\"/>");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_PROPOSE + "\"/>");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_FEEDBACK + "\"/>");
    out.println("</span>");

    out.println("<h3>Summary</h3>");
    out.println("<textarea name=\"nextSummary\" rows=\"12\" onkeydown=\"resetRefresh()\">"
        + n(completingAction.getNextSummary()) + "</textarea>");
    printSendEmailSelection(out, formName, projectContactList);
    out.println("<br/><span class=\"right\">");
    out.println("<input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_ID + "\" value=\""
        + completingAction.getActionId() + "\"/>");
    out.println("<input type=\"hidden\" name=\"" + PARAM_EDIT_ACTION_ID + "\" value=\""
        + completingAction.getActionId() + "\"/>");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_COMPLETED + "\"/>");
    out.println("</span>");

  }

  private static String convertToHtmlList(String input) {
    // Use StringBuilder for efficient string manipulation
    StringBuilder html = new StringBuilder("<ul>\n");

    // Split the input string by new lines
    String[] lines = input.split("\\r?\\n");

    for (String line : lines) {
      // Check if the line starts with " - " and trim whitespace
      if (line.startsWith(" - ")) {
        // Extract the text after " - " and wrap it in <li> tags
        html.append("  <li>").append(escapeHtml(line.substring(3).trim())).append("</li>\n");
      }
    }

    html.append("</ul>");
    return html.toString();
  }

  private static String escapeHtml(String text) {
    // Escape HTML special characters to prevent XSS
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private String getTimeString(AppReq appReq, ProjectAction completingAction) {
    String timeString = TimeTracker.formatTime(completingAction.getNextTimeEstimate()) + " ";
    {
      TimeTracker timeTracker = appReq.getTimeTracker();
      if (timeTracker != null) {
        String t = TimeTracker.formatTime(completingAction.getNextTimeActual());
        if (timeTracker.isRunningClock()) {
          timeString += "<a href=\"ProjectActionServlet?" + PARAM_ACTION + "=" + ACTION_STOP_TIMER
              + "\" class=\"timerRunning\" id=\"" + ID_TIME_RUNNING + "\">" + t + "</a>";
        } else {
          timeString += "<a href=\"ProjectActionServlet?" + PARAM_COMPLETING_ACTION_ID + "="
              + completingAction.getActionId()
              + "&" + PARAM_ACTION + "=" + ACTION_START_TIMER + "\" class=\"timerStopped\" id=\"" + ID_TIME_RUNNING
              + "\">" + t
              + "</a>";
        }
      } else {
        // hidden text with time
        timeString += "<span id=\"" + ID_TIME_RUNNING + "\">"
            + TimeTracker.formatTime(completingAction.getNextTimeActual())
            + "</span>";
      }
    }
    return timeString;
  }

  private String getNextActionTitle(ProjectAction completingAction) {
    String title = "Action to take";
    ProjectNextActionStatus nextActionStatus = completingAction.getNextActionStatus();
    if (nextActionStatus == null) {
      return title;
    }
    switch (completingAction.getNextActionStatus()) {
      case PROPOSED:
        title = "Proposed";
        break;
      case READY:
        title = "Scheduled today";
        break;
      case COMPLETED:
        title = "Completed";
        break;
      case CANCELLED:
        title = "Cancelled";
        break;
    }
    return title;
  }

  private void printCurrentActionEdit(AppReq appReq, WebUser webUser, PrintWriter out, ProjectAction projectAction,
      Project project,
      List<ProjectContact> projectContactList,
      String formName, List<Project> projectList) {

    String disabled = "";
    out.println("<table class=\"boxed-full\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\">Edit Action</th>");
    out.println("  </tr>");
    printEditNextAction(appReq.getRequest(), webUser, out, projectAction, project, formName, disabled,
        projectContactList, projectList);
    out.println("</table>");
    out.println("<span class=\"right\">");
    out.println("  <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\">"
        + ACTION_SAVE + "</button>");
    if (projectAction == null || appReq.getCompletingAction() == null
        || !projectAction.equals(appReq.getCompletingAction())) {
      out.println("  <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_START + "\">"
          + ACTION_START + "</button>");
    }
    if (projectAction != null) {
      out.println("  <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_DELETE + "\">"
          + ACTION_DELETE + "</button>");
    }
    out.println("  <button type=\"button\" id=\"cancelButton" + formName + "\">Cancel</button>");
    out.println("</span>");

  }

  private void setupProjectAction(Session dataSession, ProjectAction projectAction) {
    projectAction
        .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
    projectAction.setContact(
        (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
    if (projectAction.getNextContactId() != null && projectAction.getNextContactId() > 0) {
      projectAction.setNextProjectContact((ProjectContact) dataSession.get(ProjectContact.class,
          projectAction.getNextContactId()));
    }
  }

  private void setupProjectActionAndSaveToAppReq(AppReq appReq, Session dataSession,
      ProjectAction completingProjectAction) {
    completingProjectAction
        .setProject((Project) dataSession.get(Project.class, completingProjectAction.getProjectId()));
    completingProjectAction.setContact(
        (ProjectContact) dataSession.get(ProjectContact.class, completingProjectAction.getContactId()));
    if (completingProjectAction.getNextContactId() != null && completingProjectAction.getNextContactId() > 0) {
      completingProjectAction.setNextProjectContact((ProjectContact) dataSession.get(ProjectContact.class,
          completingProjectAction.getNextContactId()));
    }
    appReq.setCompletingAction(completingProjectAction);
    appReq.setProject(completingProjectAction.getProject());
  }

  private List<ProjectAction> getProjectActionListClosedToday(WebUser webUser, Session dataSession) {
    Date today = TimeTracker.createToday(webUser).getTime();
    Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
    Query query = dataSession.createQuery(
        "from ProjectAction where provider = :provider and (contactId = :contactId or nextContactId = :nextContactId) "
            + "and nextActionId <> 0 and nextDescription <> '' "
            + "and nextDue >= :today and nextDue < :tomorrow "
            + "order by nextTimeActual DESC, nextTimeEstimate DESC");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter(PARAM_NEXT_CONTACT_ID, webUser.getContactId());
    query.setParameter("today", today);
    query.setParameter("tomorrow", tomorrow);
    List<ProjectAction> projectActionList = query.list();
    // setup the prjoect action
    for (ProjectAction projectAction : projectActionList) {
      setupProjectAction(dataSession, projectAction);
    }
    return projectActionList;
  }

  private static List<ProjectAction> getProjectActionListForToday(WebUser webUser, Session dataSession, int dayOffset) {
    Date today = TimeTracker.createToday(webUser).getTime();
    Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
    if (dayOffset > 0) {
      // add number of days to today and tomorrow to get new "today" and "tomorrow"
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(today);
      calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
      today = calendar.getTime();
      calendar.setTime(tomorrow);
      calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
      tomorrow = calendar.getTime();
    }
    Query query = dataSession.createQuery(
        "from ProjectAction where provider = :provider and (contactId = :contactId or nextContactId = :nextContactId) "
            + "and nextActionId = 0 and nextDescription <> '' "
            + "and nextDue >= :today and nextDue < :tomorrow "
            + "order by nextDue, priority_level DESC, nextTimeEstimate, actionDate");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter(PARAM_NEXT_CONTACT_ID, webUser.getContactId());
    query.setParameter("today", today);
    query.setParameter("tomorrow", tomorrow);
    @SuppressWarnings("unchecked")
    List<ProjectAction> projectActionList = query.list();
    // sorth the projectActionList first by the defaultPriority from the
    // ProjectNextActionType and then by the priority_level
    projectActionList.sort((pa1, pa2) -> {
      PrioritySpecial ps1 = pa1.getPrioritySpecial();
      PrioritySpecial ps2 = pa2.getPrioritySpecial();
      // If one of the priorities is special, then we need to sort by the special
      // priority, unless they are the same
      if ((ps1 != null || ps2 != null) && ps1 != ps2) {
        // very complicated logic to sort by priority special
        // FIRST must go first before any SECOND, or any other priority without a
        // special priority
        if (ps1 == PrioritySpecial.FIRST) {
          return -1;
        } else if (ps2 == PrioritySpecial.FIRST) {
          return 1;
        }
        // SECOND must go after any FIRST, but before any other priority without a
        // special priority
        if (ps1 == PrioritySpecial.SECOND) {
          return -1;
        } else if (ps2 == PrioritySpecial.SECOND) {
          return 1;
        }
        // LAST must go last after any other priority without a special priority and
        // PENULTIMATE
        if (ps1 == PrioritySpecial.LAST) {
          return 1;
        } else if (ps2 == PrioritySpecial.LAST) {
          return -1;
        }
        // PENUlTIMATE must go last after any other priority without a special priority,
        // but before any LAST
        if (ps1 == PrioritySpecial.PENULTIMATE) {
          return 1;
        } else if (ps2 == PrioritySpecial.PENULTIMATE) {
          return -1;
        }
      }
      int p1 = ProjectNextActionType.defaultPriority(pa1.getNextActionType());
      int p2 = ProjectNextActionType.defaultPriority(pa2.getNextActionType());
      if (p1 != p2) {
        return p2 - p1;
      }
      return pa2.getPriorityLevel() - pa1.getPriorityLevel();
    });
    return projectActionList;
  }

  private static void printTimeTotal(PrintWriter out, String title, String idBase, int timeEst, int timeAct) {
    String hide = "";
    if (timeEst == 0) {
      hide = " style=\"display: none;\"";
    }
    out.println("  <tr class=\"boxed\"" + hide + ">");
    out.println("    <th class=\"boxed\">" + title + "</th>");
    String idBaseEst = idBase + ID_EST;
    String idBaseAct = idBase + ID_ACT;
    String timeEstString = ProjectAction.getTimeForDisplay(timeEst);
    String timeActString = ProjectAction.getTimeForDisplay(timeAct);
    out.println("    <td class=\"boxed\" id=\"" + idBaseEst + "\">" + timeEstString + "</th>");
    out.println("    <td class=\"boxed\" id=\"" + idBaseAct + "\">" + timeActString + "</th>");
    out.println("  </tr>");
  }

  protected static List<ProjectAction> prepareProjectActionListAndIdentifyOverdue(
      Session dataSession, List<ProjectAction> projectActionList, WebUser webUser) {
    List<ProjectAction> projectActionListOverdue = new ArrayList<ProjectAction>();

    {
      Date today = TimeTracker.createToday(webUser).getTime();
      for (ProjectAction projectAction : projectActionList) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        if (projectAction.getProject() == null) {
          continue;
        }
        if (projectAction.getNextDue() == null || before(projectAction, today)) {
          projectActionListOverdue.add(projectAction);
        }
        projectAction.setContact(
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
        if (projectAction.getNextContactId() != null && projectAction.getNextContactId() > 0) {
          projectAction.setNextProjectContact((ProjectContact) dataSession.get(ProjectContact.class,
              projectAction.getNextContactId()));
        }
      }
    }
    return projectActionListOverdue;
  }

  protected static void prepareProjectActionList(
      Session dataSession, List<ProjectAction> projectActionList, WebUser webUser) {
    {
      for (ProjectAction projectAction : projectActionList) {
        projectAction
            .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
        if (projectAction.getProject() == null) {
          continue;
        }
        projectAction.setContact(
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
        if (projectAction.getNextContactId() != null && projectAction.getNextContactId() > 0) {
          projectAction.setNextProjectContact((ProjectContact) dataSession.get(ProjectContact.class,
              projectAction.getNextContactId()));
        }
      }
    }
  }

  private static void printDueTable(WebUser webUser, PrintWriter out,
      String nextActionType, List<ProjectAction> projectActionList) {

    List<ProjectAction> paList = new ArrayList<ProjectAction>();
    if (nextActionType == null) {
      paList.addAll(projectActionList);
    } else {
      for (ProjectAction projectAction : projectActionList) {
        if (projectAction.getNextActionType() != null
            && projectAction.getNextActionType().equals(nextActionType)) {
          paList.add(projectAction);
        }
      }
    }
    if (paList.size() == 0) {
      return;
    }
    if (nextActionType != null) {
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">" + ProjectNextActionType.getLabel(nextActionType) + "</th>");
      out.println("    <th class=\"boxed\">Est</th>");
      out.println("    <th class=\"boxed\">Act</th>");
      out.println("  </tr>");
    }
    printActionItems(webUser, out, paList);
  }

  private static void printActionItems(WebUser webUser, PrintWriter out, List<ProjectAction> paList) {
    for (ProjectAction projectAction : paList) {
      String link = "ProjectActionServlet?" + PARAM_COMPLETING_ACTION_ID + "=" + projectAction.getActionId();
      out.println("  <tr class=\"boxed\">");
      if (projectAction.getProject() == null) {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      } else {
        out.println("    <td class=\"boxed\"><a href=\"" + link + "\" class=\"button\">"
            + projectAction.getProject().getProjectName() + "</a></td>");
      }
      out.println("    <td class=\"boxed\"><a href=\"" + link + "\" class=\"button\">"
          + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a></td>");
      if (projectAction.getNextTimeEstimate() == null || projectAction.getNextTimeEstimate() == 0) {
        out.println("    <td class=\"boxed\">&nbsp;</a></td>");
      } else {
        out.println(
            "    <td class=\"boxed\">" + projectAction.getNextTimeEstimateForDisplay() + "</a></td>");
      }
      if (projectAction.getNextTimeActual() == null || projectAction.getNextTimeActual() == 0) {
        out.println("    <td class=\"boxed\">&nbsp;</a></td>");
      } else {
        out.println(
            "    <td class=\"boxed\">" + projectAction.getNextTimeActualForDisplay() + "</a></td>");
      }
      out.println("  </tr>");
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

  private static final String[] QUOTES = {
      "Carpe Diem",
      "Either you run the day or the day runs you",
      "Everything is hard before it is easy",
      "Success is the sum of small efforts repeated day in and day out",
      "The future depends on what you do today",
      "Don't watch the clock; do what it does. Keep going",
      "Start where you are, use what you have, do what you can",
      "Dream big, start small, act now",
      "It always seems impossible until it's done",
      "If not now, when?",
      "Your only limit is you",
      "Act as if what you do makes a difference. It does",
      "You don't have to be great to start, but you have to start to be great",
      "Opportunities don't happen. You create them",
      "You don't find willpower, you create it",
      "Great things never come from comfort zones",
      "Do something today that your future self will thank you for",
      "The journey of a thousand miles begins with one step",
      "Don't stop when you're tired. Stop when you're done",
      "You are never too old to set another goal or to dream a new dream",
      "Small steps every day",
      "The way to get started is to quit talking and begin doing",
      "Wake up with determination, go to bed with satisfaction",
      "Push yourself, because no one else is going to do it for you",
      "If you're going through hell, keep going",
      "The best way out is always through",
      "Success doesn't just find you. You have to go out and get it",
      "If you can dream it, you can do it",
      "Focus on the step in front of you, not the whole staircase",
      "Stop doubting yourself, work hard, and make it happen",
      "Success is what comes after you stop making excuses",
      "Make today so awesome that yesterday gets jealous",
      "Do one thing every day that scares you",
      "Success is liking yourself, liking what you do, and liking how you do it",
      "Stay patient and trust your journey",
      "Take the risk or lose the chance",
      "Believe in yourself and all that you are",
      "You're only one decision away from a totally different life",
      "Believe you can and you're halfway there",
      "The secret of getting ahead is getting started",
      "Don't wish for it, work for it",
      "Hustle in silence and let your success make the noise",
      "Don't count the days; make the days count",
      "Don't wait for opportunity. Create it",
      "Make yourself proud",
      "What you do today can improve all your tomorrows",
      "Rise up and attack the day with enthusiasm",
      "Hard work beats talent when talent doesn't work hard",
      "The dream is free; the hustle is sold separately",
      "Be stronger than your excuses",
      "A goal without a plan is just a wish",
      "If you want to fly, give up everything that weighs you down"
  };

  private static List<ProjectAction> getProjectActionsScheduledAndCompletedList(Session dataSession, int projectId) {
    List<ProjectAction> projectActionList;
    Query query = dataSession.createQuery(
        "from ProjectAction pa \n" +
            "where pa.nextDescription <> '' \n" +
            "  and pa.nextActionId <> 0> \n" +
            "  and exists ( \n" +
            "    select 1 \n" +
            "    from ProjectAction nextPa \n" +
            "    where nextPa.actionId = pa.nextActionId \n" +
            "      and nextPa.actionDescription <> '' \n" +
            "  )");
    query.setParameter(0, projectId);
    projectActionList = query.list();
    return projectActionList;
  }

}
