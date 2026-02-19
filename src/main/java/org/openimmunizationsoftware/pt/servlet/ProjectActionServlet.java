/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ProjectNarrativeDao;
import org.openimmunizationsoftware.pt.manager.ChatAgent;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeAdder;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
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
  private static final String PARAM_SEND_EMAIL_TO = "sendEmailTo";
  private static final String PARAM_START_SENTANCE = "startSentance";
  private static final String PARAM_PROCESS_STAGE = "processStage";
  private static final String PARAM_TIME_SLOT = "timeSlot";
  private static final String PARAM_NEXT_CONTACT_ID = "nextContactId";
  private static final String PARAM_NEXT_ACTION_TYPE = "nextActionType";
  private static final String PARAM_TEMPLATE_TYPE = "templateType";
  private static final String PARAM_LINK_URL = "linkUrl";
  private static final String PARAM_NEXT_TARGET_DATE = "nextTargetDate";
  private static final String PARAM_NEXT_DEADLINE_DATE = "nextDeadlineDate";
  private static final String PARAM_NEXT_ACTION_DATE = "nextActionDate";
  private static final String PARAM_NEXT_DESCRIPTION = "nextDescription";
  private static final String PARAM_NEXT_NOTES = "nextNotes";
  private static final String PARAM_NEXT_NOTE = "nextNote";
  private static final String PARAM_NEXT_SUMMARY = "nextSummary";
  private static final String PARAM_NEXT_TIME_ESTIMATE = "nextTimeEstimate";
  private static final String PARAM_NEXT_PROJECT_ID = "nextProjectId";
  private static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_ACTION_NEXT_ID = "actionNextId";
  private static final String PARAM_POSTPONE_ACTION_NEXT_ID = "postponeActionNextId";
  private static final String PARAM_SENTENCE_INPUT = "sentenceInput";
  protected static final String PARAM_COMPLETING_ACTION_NEXT_ID = "completingActionNextId";
  private static final String PARAM_EDIT_ACTION_NEXT_ID = "editActionNextId";
  private static final String PARAM_SHOW_WORK = "showWork";
  private static final String PARAM_SHOW_PERSONAL = "showPersonal";
  private static final String PARAM_FILTER_SUBMITTED = "filterSubmitted";
  private static final String SESSION_SHOW_WORK = "projectAction.showWork";
  private static final String SESSION_SHOW_PERSONAL = "projectAction.showPersonal";
  private static final String REQUEST_SHOW_WORK = "projectAction.requestShowWork";
  private static final String REQUEST_SHOW_PERSONAL = "projectAction.requestShowPersonal";
  protected static final String PARAM_ACTION = "action";
  protected static final String ACTION_START_TIMER = "StartTimer";
  private static final String ACTION_STOP_TIMER = "StopTimer";
  private static final String ACTION_NOTE = "Note";
  private static final String ACTION_PROPOSE = "Propose";
  private static final String ACTION_FEEDBACK = "Feedback";
  private static final String ACTION_COMPLETED = "Completed";
  private static final String ACTION_SCHEDULE = "Schedule";
  private static final String ACTION_SCHEDULE_AND_START = "Schedule and Start";
  private static final String ACTION_SAVE = "Save";
  private static final String ACTION_DELETE = "Delete";
  private static final String ACTION_START = "Start";
  private static final String ACTION_REFRESH_TIME = "RefreshTime";
  private static final String ACTION_SUGGEST = "Suggest";
  private static final String ACTION_POSTPONE_NEXT_WORKING_DAY = "PostponeNextWorkingDay";

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
      boolean showWork = isShowWork(request);
      boolean showPersonal = isShowPersonal(request);

      readCompletingAction(request, appReq, dataSession);

      Project project = appReq.getProject();
      ProjectActionNext completingAction = appReq.getCompletingAction();
      if (completingAction != null) {
        project = completingAction.getProject();
      }
      appReq.setProject(project);
      appReq.setProjectSelected(project);
      appReq.setCompletingAction(completingAction);
      appReq.setProjectActionSelected(completingAction);

      List<ChatAgent> chatAgentList = new ArrayList<ChatAgent>();
      List<Project> projectList = getProjectList(webUser, dataSession);

      if (completingAction != null) {
        readNotesSummaryUpdateTimeTracker(request, appReq, dataSession, completingAction);
      }

      String emailBody = null;
      String sentenceInput = request.getParameter(PARAM_SENTENCE_INPUT);
      ProjectActionNext nextAction = null;
      ProjectActionNext editProjectAction = readEditProjectAction(appReq);
      if (sentenceInput != null && !sentenceInput.trim().isEmpty()) {
        nextAction = saveNewAction(webUser, dataSession, completingAction, projectList, editProjectAction,
            nextAction,
            sentenceInput);
        if (nextAction != null) {
          if (nextAction.getProjectId() != project.getProjectId()) {
            project = nextAction.getProject();
          }
          if (action.equals(ACTION_SCHEDULE_AND_START)) {
            completingAction = nextAction;
          }
        }
        if (completingAction != null) {
          project = completingAction.getProject();
        }
      }

      List<ProjectActionTaken> projectActionTakenList = null;
      List<ProjectActionNext> projectActionScheduledList = null;
      if (project != null) {
        projectActionTakenList = getProjectActionsTakenList(dataSession, project);
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
        } else if (action.equals(ACTION_POSTPONE_NEXT_WORKING_DAY)) {
          ProjectActionNext postponedAction = postponeActionToNextWorkingDay(appReq, dataSession);
          if (postponedAction != null && completingAction != null
              && postponedAction.getActionNextId() == completingAction.getActionNextId()) {
            completingAction = null;
            appReq.setCompletingAction(null);
          }
        } else if (action.equals(ACTION_PROPOSE)) {
          chatPropose(appReq, completingAction, chatAgentList, projectActionTakenList, projectActionScheduledList);
        } else if (action.equals(ACTION_FEEDBACK)) {
          chatFeedback(appReq, completingAction, chatAgentList, projectActionTakenList, projectActionScheduledList);
        } else if (action.equals(ACTION_SUGGEST)) {
          nextSuggest = chatNext(appReq, completingAction, chatAgentList, projectActionTakenList,
              projectActionScheduledList);
        } else {
          if (action.equals(ACTION_COMPLETED) && nextAction == null) {
            ProjectNextActionStatus nextActionStatus = ProjectNextActionStatus.COMPLETED;
            String nextDescription = completingAction.getNextSummary();
            closeAction(appReq, editProjectAction, project, nextDescription, nextActionStatus);
            emailBody = sendEmail(request, appReq, webUser, dataSession, project, editProjectAction, emailBody);
            completingAction = null;
          } else if (action.equals(ACTION_SAVE) || action.equals(ACTION_START)) {
            Project nextProject = project;
            Date originalNextActionDate = null;
            if (editProjectAction == null) {
              String nextProjectIdString = request.getParameter(PARAM_NEXT_PROJECT_ID);
              if (nextProjectIdString != null) {
                nextProject = (Project) dataSession.get(Project.class, Integer.parseInt(nextProjectIdString));
              }
            } else {
              originalNextActionDate = editProjectAction.getNextActionDate();
            }
            editProjectAction = saveProjectAction(appReq, editProjectAction, nextProject);
            Date nextActionDate = editProjectAction.getNextActionDate();
            if (action.equals(ACTION_START)) {
              completingAction = editProjectAction;
              appReq.setCompletingAction(completingAction);
              project = completingAction.getProject();
              projectActionTakenList = getProjectActionsTakenList(dataSession, project);
            } else if (nextActionDate != null && !webUser.isToday(nextActionDate)
                && (completingAction != null && completingAction.equals(editProjectAction))
                && (originalNextActionDate == null || !nextActionDate.equals(originalNextActionDate))) {
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
          projectActionTakenList = getProjectActionsTakenList(dataSession, project);
          projectActionScheduledList = getAllProjectActionsScheduledList(appReq, project, dataSession);
        }
      }
      List<ProjectActionNext> projectActionDueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
      projectActionDueTodayList = filterProjectActionList(projectActionDueTodayList, showWork, showPersonal);
      List<ProjectActionNext> projectActionOverdueList = getProjectActionListForToday(webUser, dataSession, -1);
      projectActionOverdueList = filterProjectActionList(projectActionOverdueList, showWork, showPersonal);
      List<List<ProjectActionNext>> projectActionDueNextWorkingDayListList = new ArrayList<>();
      Calendar planningCalendar = getCalendarForTodayNoTime(webUser);
      planningCalendar.add(Calendar.DAY_OF_MONTH, 1);
      Date planningStartDate = planningCalendar.getTime();
      planningCalendar.add(Calendar.DAY_OF_MONTH, 13);
      Date planningEndDate = planningCalendar.getTime();
      List<ProjectActionNext> planningRangeList = getProjectActionListForPlanningRange(webUser, dataSession,
          planningStartDate, planningEndDate);
      planningRangeList = filterProjectActionList(planningRangeList, showWork, showPersonal);
      Map<Date, List<ProjectActionNext>> planningBuckets = new HashMap<>();
      for (ProjectActionNext projectAction : planningRangeList) {
        Date bucketDate = projectAction.getNextActionDate();
        List<ProjectActionNext> bucketList = planningBuckets.get(bucketDate);
        if (bucketList == null) {
          bucketList = new ArrayList<>();
          planningBuckets.put(bucketDate, bucketList);
        }
        bucketList.add(projectAction);
      }

      int daysFound = 0;
      for (int dayOffset = 1; dayOffset < 14 && daysFound < 5; dayOffset++) {
        Calendar dayCalendar = getCalendarForTodayNoTime(webUser);
        dayCalendar.add(Calendar.DAY_OF_MONTH, dayOffset);
        Date dayKey = dayCalendar.getTime();
        List<ProjectActionNext> projectActionDueNextWorkingDayList = planningBuckets.get(dayKey);
        if (projectActionDueNextWorkingDayList == null || projectActionDueNextWorkingDayList.isEmpty()) {
          continue;
        }
        sortProjectActionList(projectActionDueNextWorkingDayList);
        projectActionDueNextWorkingDayListList.add(projectActionDueNextWorkingDayList);
        daysFound++;
      }
      if (completingAction != null && !shouldDisplayProjectAction(completingAction, showWork, showPersonal)) {
        completingAction = null;
        appReq.setCompletingAction(null);
      }
      if (completingAction == null && projectActionDueTodayList.size() > 0) {
        completingAction = projectActionDueTodayList.get(0);
        appReq.setCompletingAction(completingAction);
        project = completingAction.getProject();
        projectActionTakenList = getProjectActionsTakenList(dataSession, project);
        projectActionScheduledList = getAllProjectActionsScheduledList(appReq, project, dataSession);
      }
      List<ProjectActionNext> projectActionClosedTodayList = getProjectActionListClosedToday(webUser, dataSession);
      projectActionClosedTodayList = filterProjectActionList(projectActionClosedTodayList, showWork, showPersonal);
      List<ProjectNarrativeDao.ActionWithMinutes> deletedActionsWithTimeToday = new ArrayList<>();
      List<ProjectNarrativeDao.Action> deletedActionsWithoutTimeToday = new ArrayList<>();
      if (project != null) {
        ProjectNarrativeDao narrativeDao = new ProjectNarrativeDao(dataSession);
        LocalDate today = webUser.getLocalDateToday();
        deletedActionsWithTimeToday = narrativeDao.getDeletedActionsWithTimeForProjectOnDate(project.getProjectId(),
            today);
        deletedActionsWithoutTimeToday = narrativeDao.getDeletedActionsWithoutTimeForProjectOnDate(
            project.getProjectId(),
            today);
      }
      if (completingAction != null) {
        TimeTracker timeTracker = appReq.getTimeTracker();
        if (timeTracker != null) {
          timeTracker.update(completingAction, dataSession);
        }
      }

      if (completingAction != null) {
        project = completingAction.getProject();
      }
      appReq.setProject(project);
      appReq.setProjectSelected(project);
      appReq.setCompletingAction(completingAction);
      appReq.setProjectActionSelected(completingAction);
      projectActionScheduledList = filterProjectActionList(projectActionScheduledList, showWork, showPersonal);

      // register project so it shows up in list of projects recently referred to
      if (project != null) {
        ProjectServlet.setupProjectList(appReq, project);
      }
      appReq.setTitle("Actions");
      printHtmlHead(appReq);

      Set<String> formNameSet = new HashSet<String>();

      Date nextActionDate = webUser.getCalendar().getTime();
      Calendar cIndicated = webUser.getCalendar();
      cIndicated.setTime(nextActionDate);

      if (completingAction == null) {
        printTimeManagementBox(appReq, projectActionDueTodayList);
        out.println("<h2>Good Job!</h2>");
        out.println("<p>You have no more actions to take today. Have a great evening! </p>");
        printActionsCompletedForToday(appReq, projectActionClosedTodayList);
      } else {

        List<ProjectContactAssigned> projectContactAssignedList = ProjectServlet
            .getProjectContactAssignedList(dataSession, project.getProjectId());
        List<ProjectContact> projectContactList = ProjectServlet.getProjectContactList(dataSession, project,
            projectContactAssignedList);

        out.println("<div id=\"three-column-container\">");

        // ------------------------------------------------------------------------------
        // ACTION NEXT
        // ------------------------------------------------------------------------------
        out.println("<div id=\"actionNext\">");
        printActionsNext(appReq, project, projectContactList, projectList, projectActionTakenList,
            projectActionScheduledList, formNameSet);
        if (nextSuggest != null) {
          out.println("<h3>Suggested Next Actions</h3>");
          out.println(nextSuggest);
        }
        out.println("</div>");

        // ------------------------------------------------------------------------------
        // ACTION NOW
        // ------------------------------------------------------------------------------
        out.println("<div id=\"actionNow\">");
        printActionNow(appReq, webUser, out, project, completingAction, projectList, projectContactList, formNameSet);
        out.println("</div>");

        // ------------------------------------------------------------------------------
        // ACTION LATER
        // ------------------------------------------------------------------------------
        out.println("<div id=\"actionLater\">");
        printTimeManagementBox(appReq, projectActionDueTodayList);
        printActionsScheduledForToday(appReq, projectActionDueTodayList, projectActionOverdueList);
        printActionsCompletedForToday(appReq, projectActionClosedTodayList);
        printActionsDeletedWithTimeForToday(appReq, deletedActionsWithTimeToday);
        printDeletedActionsWithoutTimeForToday(appReq, deletedActionsWithoutTimeToday);

        {
          List<TimeAdder> timeAdderList = new ArrayList<>();
          for (List<ProjectActionNext> projectActionDueNextWorkingDayList : projectActionDueNextWorkingDayListList) {
            Date workingDayDate = projectActionDueNextWorkingDayList.get(0).getNextActionDate();
            TimeAdder timeAdder = new TimeAdder(projectActionDueNextWorkingDayList, appReq, workingDayDate);
            timeAdderList.add(timeAdder);
          }

          out.println("<table class=\"boxed\">");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"title\">Planning</th>");
          for (List<ProjectActionNext> projectActionDueNextWorkingDayList : projectActionDueNextWorkingDayListList) {
            Date workingDayDate = projectActionDueNextWorkingDayList.get(0).getNextActionDate();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd");
            out.println("    <th class=\"boxed\">" + sdf.format(workingDayDate) + "</th>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Will Meet</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(timeAdder.getWillMeetEst())
                    + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Committed</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(timeAdder.getCommittedEst())
                    + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Will</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(timeAdder.getWillEst())
                    + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Might</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(timeAdder.getMightEst())
                    + "</td>");
          }
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Planned</th>");
          for (TimeAdder timeAdder : timeAdderList) {
            out.println(
                "    <td class=\"boxed\">" + ProjectActionNext.getTimeForDisplay(timeAdder.getWillAct())
                    + "</td>");
          }
          out.println("  </tr>");
          out.println("</table><br/>");
        }
        for (List<ProjectActionNext> projectActionDueNextWorkingDayList : projectActionDueNextWorkingDayListList) {
          Date workingDayDate = projectActionDueNextWorkingDayList.get(0).getNextActionDate();
          printActionsScheduledForNextWorkingDay(appReq, projectActionDueNextWorkingDayList, workingDayDate);
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

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private static boolean isNumeric(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    for (char c : str.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  private void resolveAndStoreShowPreferences(HttpServletRequest request) {
    if (request.getAttribute(REQUEST_SHOW_WORK) instanceof Boolean
        && request.getAttribute(REQUEST_SHOW_PERSONAL) instanceof Boolean) {
      return;
    }

    HttpSession session = request.getSession();
    boolean hasFilterSubmitted = request.getParameter(PARAM_FILTER_SUBMITTED) != null;
    boolean hasShowWorkParam = request.getParameter(PARAM_SHOW_WORK) != null;
    boolean hasShowPersonalParam = request.getParameter(PARAM_SHOW_PERSONAL) != null;

    boolean showWork;
    boolean showPersonal;
    if (hasFilterSubmitted) {
      showWork = hasShowWorkParam;
      showPersonal = hasShowPersonalParam;
    } else {
      Boolean sessionShowWork = (Boolean) session.getAttribute(SESSION_SHOW_WORK);
      Boolean sessionShowPersonal = (Boolean) session.getAttribute(SESSION_SHOW_PERSONAL);
      if (sessionShowWork == null && sessionShowPersonal == null) {
        showWork = true;
        showPersonal = true;
      } else {
        showWork = sessionShowWork != null ? sessionShowWork.booleanValue() : true;
        showPersonal = sessionShowPersonal != null ? sessionShowPersonal.booleanValue() : true;
      }
    }

    if (!showWork && !showPersonal) {
      showWork = true;
      showPersonal = true;
    }

    session.setAttribute(SESSION_SHOW_WORK, Boolean.valueOf(showWork));
    session.setAttribute(SESSION_SHOW_PERSONAL, Boolean.valueOf(showPersonal));
    request.setAttribute(REQUEST_SHOW_WORK, Boolean.valueOf(showWork));
    request.setAttribute(REQUEST_SHOW_PERSONAL, Boolean.valueOf(showPersonal));
  }

  private boolean isShowWork(HttpServletRequest request) {
    resolveAndStoreShowPreferences(request);
    return ((Boolean) request.getAttribute(REQUEST_SHOW_WORK)).booleanValue();
  }

  private boolean isShowPersonal(HttpServletRequest request) {
    resolveAndStoreShowPreferences(request);
    return ((Boolean) request.getAttribute(REQUEST_SHOW_PERSONAL)).booleanValue();
  }

  private boolean shouldDisplayProjectAction(ProjectActionNext projectAction, boolean showWork,
      boolean showPersonal) {
    if (projectAction == null) {
      return false;
    }
    if (projectAction.isBillable()) {
      return showWork;
    }
    return showPersonal;
  }

  private List<ProjectActionNext> filterProjectActionList(List<ProjectActionNext> projectActionList,
      boolean showWork, boolean showPersonal) {
    List<ProjectActionNext> filteredList = new ArrayList<ProjectActionNext>();
    if (projectActionList == null) {
      return filteredList;
    }
    for (ProjectActionNext projectAction : projectActionList) {
      if (shouldDisplayProjectAction(projectAction, showWork, showPersonal)) {
        filteredList.add(projectAction);
      }
    }
    return filteredList;
  }

  private boolean resolveBillable(Session dataSession, Project project) {
    if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
      return false;
    }
    BillCode billCode = (BillCode) dataSession.get(BillCode.class, project.getBillCode());
    return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
  }

  private ProjectActionNext saveNewAction(WebUser webUser, Session dataSession,
      ProjectActionNext completingAction, List<Project> projectList, ProjectActionNext editProjectAction,
      ProjectActionNext nextAction, String sentenceInput) {
    String projectName = "";
    String actionPart = sentenceInput;
    String[] parts = sentenceInput.split(":", 2);
    if (parts.length == 2) {
      projectName = parts[0].trim();
      actionPart = parts[1].trim();
    }

    Project foundProject = null;
    for (Project p : projectList) {
      if (p.getProjectName().equalsIgnoreCase(projectName)) {
        foundProject = p;
        break;
      }
    }
    if (foundProject == null) {
      ProjectActionNext sourceAction = editProjectAction != null ? editProjectAction : completingAction;
      if (sourceAction == null || sourceAction.getProject() == null) {
        return null;
      }
      // deafulting to current project
      foundProject = sourceAction.getProject();
      actionPart = projectName + " " + actionPart;
    }

    String actionVerb = "I will";
    String actionToTake = actionPart;
    String whenToTakeAction = "";
    int nextTimeEstimate = 20; // default to 20 minutes
    if (actionPart.startsWith("I will meet ")) {
      actionVerb = "I will meet";
      actionToTake = actionPart.substring("I will meet ".length()).trim();
      nextTimeEstimate = 60; // default to 60 minutes for meetings
    } else if (actionPart.startsWith("I will ")) {
      actionVerb = "I will";
      actionToTake = actionPart.substring("I will ".length()).trim();
    } else if (actionPart.startsWith("I might ")) {
      actionVerb = "I might";
      actionToTake = actionPart.substring("I might ".length()).trim();
    } else if (actionPart.startsWith("I have committed ")) {
      actionVerb = "I have committed";
      actionToTake = actionPart.substring("I have committed ".length()).trim();
    } else if (actionPart.startsWith("I have set goal to")) {
      actionVerb = "I have set goal to";
      actionToTake = actionPart.substring("I have set goal to".length()).trim();
    }
    // Parse the action description to extract time estimates and due dates (e.g.,
    // "for 2 hours", "today", "next Monday")
    // Split actionToTake into tokens
    String[] tokens = actionToTake.trim().split("\\s+");
    if (tokens.length >= 1) {
      String lastToken = tokens[tokens.length - 1];
      String secondLastToken = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
      if (tokens.length > 3) {
        String thirdLastToken = tokens.length >= 3 ? tokens[tokens.length - 3] : "";
        if (thirdLastToken.equals("for") && isNumeric(secondLastToken)) {
          try {
            nextTimeEstimate = Integer.parseInt(secondLastToken);
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          if (lastToken.equals("hours") || lastToken.equals("hour")) {
            nextTimeEstimate *= 60; // convert hours to minutes
          }
          // now need to strip off these tokens from actionToTake
          actionToTake = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 3)).trim();
          tokens = actionToTake.trim().split("\\s+");
          lastToken = tokens.length >= 1 ? tokens[tokens.length - 1] : "";
          secondLastToken = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
        }
      }

      boolean foundDate = false;

      // Check if last token looks like a date (contains two slashes)
      if (lastToken.chars().filter(ch -> ch == '/').count() == 2) {
        whenToTakeAction = lastToken;
        foundDate = true;
      } else {
        // Check for "today" or "tomorrow"
        String lower = lastToken.toLowerCase();
        if (lower.equals("today") || lower.equals("tomorrow")) {
          whenToTakeAction = lastToken;
          foundDate = true;
        } else {
          // Check for day of week
          String[] days = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
          for (String day : days) {
            if (lower.equals(day)) {
              if (secondLastToken.equalsIgnoreCase("next")) {
                whenToTakeAction = "next " + lastToken;
                // Remove both tokens from actionToTake
                actionToTake = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 2)).trim();
              } else {
                whenToTakeAction = lastToken;
                // Remove last token from actionToTake
                actionToTake = String.join(" ", java.util.Arrays.copyOf(tokens, tokens.length - 1)).trim();
              }
              foundDate = true;
              break;
            }
          }
        }
      }
      if (foundDate) {
        // Remove the date token(s) from actionToTake if not already done
        if (whenToTakeAction != null && actionToTake.endsWith(whenToTakeAction)) {
          actionToTake = actionToTake.substring(0, actionToTake.length() - whenToTakeAction.length()).trim();
        } else if (!whenToTakeAction.isEmpty() && !actionToTake.isEmpty()
            && !actionToTake.endsWith(whenToTakeAction)) {
          // Already handled above for "next <day>"
        } else if (!foundDate) {
          // No date found, do nothing
        }
      }
    }
    // Now we have the project, the action verb, the action to take, and when to
    // take action
    nextAction = new ProjectActionNext();
    nextAction.setProject(foundProject);
    nextAction.setProjectId(foundProject.getProjectId());
    nextAction.setContactId(webUser.getContactId());
    Date actionDate = parseWhenToTakeAction(webUser, whenToTakeAction);
    if (actionVerb.equals("I will")) {
      nextAction.setNextActionType(ProjectNextActionType.WILL);
    } else if (actionVerb.equals("I might")) {
      nextAction.setNextActionType(ProjectNextActionType.MIGHT);
    } else if (actionVerb.equals("I have committed")) {
      nextAction.setNextActionType(ProjectNextActionType.COMMITTED_TO);
    } else if (actionVerb.equals("I will meet")) {
      nextAction.setNextActionType(ProjectNextActionType.WILL_MEET);
    } else if (actionVerb.equals("I have set goal to")) {
      nextAction.setNextActionType(ProjectNextActionType.GOAL);
    } else {
      nextAction.setNextActionType(ProjectNextActionType.WILL);
    }
    nextAction.setNextActionDate(actionDate);
    nextAction.setNextDescription(actionToTake);
    nextAction.setNextTimeEstimate(nextTimeEstimate);
    nextAction.setNextChangeDate(new Date());
    nextAction.setProvider(webUser.getProvider());
    nextAction.setContact(webUser.getProjectContact());
    nextAction.setBillable(resolveBillable(dataSession, foundProject));
    if (nextAction.getNextActionStatus() == null) {
      if (nextAction.hasNextDescription()) {
        if (nextAction.hasNextActionDate()) {
          nextAction.setNextActionStatus(ProjectNextActionStatus.READY);
        } else {
          nextAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
        }
      }
    }
    Transaction trans = dataSession.beginTransaction();
    dataSession.saveOrUpdate(nextAction);
    if (completingAction != null && completingAction.getNextSummary() != null
        && !completingAction.getNextSummary().trim().isEmpty()) {
      ProjectActionTaken actionTaken = new ProjectActionTaken();
      ProjectActionNext actionToClose = editProjectAction != null ? editProjectAction : completingAction;
      if (actionToClose != null) {
        actionTaken.setProject(actionToClose.getProject());
        actionTaken.setProjectId(actionToClose.getProjectId());
        actionTaken.setActionDate(new Date());
        actionTaken.setActionDescription(completingAction.getNextSummary());
        actionTaken.setProvider(webUser.getProvider());
        actionTaken.setContact(webUser.getProjectContact());
        actionTaken.setContactId(webUser.getContactId());
        dataSession.saveOrUpdate(actionTaken);

        actionToClose.setNextActionStatus(ProjectNextActionStatus.COMPLETED);
        actionToClose.setNextChangeDate(new Date());
        dataSession.update(actionToClose);
      }
    }
    trans.commit();

    return nextAction;
  }

  private Date parseWhenToTakeAction(WebUser webUser, String whenToTakeAction) {
    Date actionDate = webUser.getCalendar().getTime();
    if (whenToTakeAction != null && !whenToTakeAction.isEmpty()) {
      Calendar calendar = webUser.getCalendar();
      String lower = whenToTakeAction.trim().toLowerCase();
      String[] days = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
      boolean isNext = lower.startsWith("next ");
      String dayName = isNext ? lower.substring(5).trim() : lower;
      int dayOfWeek = -1;
      for (int i = 0; i < days.length; i++) {
        if (days[i].equals(dayName)) {
          dayOfWeek = i + 1; // Calendar.SUNDAY = 1
          break;
        }
      }
      if (lower.equals("today")) {
        // already set to today
      } else if (lower.equals("tomorrow")) {
        calendar.add(Calendar.DAY_OF_YEAR, 1);
      } else if (dayOfWeek != -1) {
        if (isNext) {
          // Move at least one day forward to tomorrow
          calendar.add(Calendar.DAY_OF_YEAR, 1);
          // Advance to the next Sunday (start of next week)
          while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
          }
          // Now advance to the desired day of week
          while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
          }
        } else {
          // Move forward until we hit the desired day (could be today)
          int todayDow = calendar.get(Calendar.DAY_OF_WEEK);
          int daysUntil = (dayOfWeek - todayDow + 7) % 7;
          if (daysUntil == 0) {
            // If today is the day, keep as is
          } else {
            calendar.add(Calendar.DAY_OF_YEAR, daysUntil);
          }
        }
      } else {
        // Try to parse as a date
        SimpleDateFormat sdf = webUser.getDateFormat();
        try {
          Date parsedDate = sdf.parse(whenToTakeAction);
          calendar.setTime(parsedDate);
        } catch (Exception e) {
          calendar.setTime(new Date());
        }
      }
      actionDate = calendar.getTime();
    }
    return actionDate;
  }

  private void printOutRefreshedTimeJson(HttpServletResponse response, AppReq appReq, WebUser webUser,
      Session dataSession,
      PrintWriter out, ProjectActionNext completingAction) throws IOException, StreamWriteException, DatabindException {
    // This is a JSON call
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    String timeRunningString = "0:00";
    if (completingAction != null) {
      TimeTracker timeTracker = appReq.getTimeTracker();
      if (timeTracker != null && timeTracker.isRunningClock()) {
        timeTracker.update(completingAction, dataSession);
      }
      Query query = dataSession.createQuery(
          "select sum(billMins) from BillEntry where action.actionNextId = :actionNextId "
              + "and startTime >= :today and startTime < :tomorrow");
      query.setParameter("actionNextId", completingAction.getActionNextId());
      Calendar calendar = getCalendarForTodayNoTime(webUser);
      query.setParameter("today", calendar.getTime());
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      query.setParameter("tomorrow", calendar.getTime());
      @SuppressWarnings("unchecked")
      List<Long> billMinsList = query.list();
      int billMins = 0;
      if (billMinsList.size() > 0 && billMinsList.get(0) != null) {
        billMins = billMinsList.get(0).intValue();
      }
      timeRunningString = TimeTracker.formatTime(billMins);
    }
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    boolean showPersonal = isShowPersonal(request);
    List<ProjectActionNext> projectActionDueTodayList = getProjectActionListForToday(webUser, dataSession, 0);
    projectActionDueTodayList = filterProjectActionList(projectActionDueTodayList, showWork, showPersonal);
    TimeAdder timeAdder = new TimeAdder(projectActionDueTodayList, appReq);
    Map<String, String> timeData = new HashMap<>();
    timeData.put(ID_TIME_RUNNING, timeRunningString);
    timeData.put(ID_TIME_TODAY, getFullDayAndTime());
    timeData.put(ID_TIME_OTHER + ID_EST, ProjectActionNext.getTimeForDisplay(timeAdder.getOtherEst()));
    timeData.put(ID_TIME_OTHER + ID_ACT, ProjectActionNext.getTimeForDisplay(timeAdder.getOtherAct()));
    timeData.put(ID_TIME_MIGHT + ID_EST, ProjectActionNext.getTimeForDisplay(timeAdder.getMightEst()));
    timeData.put(ID_TIME_MIGHT + ID_ACT, ProjectActionNext.getTimeForDisplay(timeAdder.getMightAct()));
    timeData.put(ID_TIME_WILL + ID_EST, ProjectActionNext.getTimeForDisplay(timeAdder.getWillEst()));
    timeData.put(ID_TIME_WILL + ID_ACT, ProjectActionNext.getTimeForDisplay(timeAdder.getWillAct()));
    timeData.put(ID_TIME_COMMITTED + ID_EST, ProjectActionNext.getTimeForDisplay(timeAdder.getCommittedEst()));
    timeData.put(ID_TIME_COMMITTED + ID_ACT, ProjectActionNext.getTimeForDisplay(timeAdder.getCommittedAct()));
    timeData.put(ID_TIME_COMPLETED + ID_ACT, ProjectActionNext.getTimeForDisplay(timeAdder.getCompletedAct()));
    timeData.put(ID_TIME_COMPLETED + ID_EST, ProjectActionNext.getTimeForDisplay(timeAdder.getCompletedAct()));
    timeData.put(ID_TIME_WILL_MEET + ID_EST, ProjectActionNext.getTimeForDisplay(timeAdder.getWillMeetEst()));
    timeData.put(ID_TIME_WILL_MEET + ID_ACT, ProjectActionNext.getTimeForDisplay(timeAdder.getWillMeetAct()));

    // Use Jackson to convert the map to JSON and write it to the response
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(out, timeData);
  }

  private void printActionNow(AppReq appReq, WebUser webUser, PrintWriter out, Project project,
      ProjectActionNext completingAction, List<Project> projectList, List<ProjectContact> projectContactList,
      Set<String> formNameSet) {
    String formName = "" + completingAction.getActionNextId();
    out.println(
        "<form name=\"actionNowForm\" id=\"actionNowForm\" method=\"post\" action=\"ProjectActionServlet\">");
    out.println(
        "<input type=\"hidden\" name=\"" + PARAM_PROJECT_ID + "\" value=\"" + project.getProjectId() + "\">");
    if (completingAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_NEXT_ID + "\" value=\""
          + completingAction.getActionNextId() + "\">");
    }

    printActionNow(appReq, webUser, out, completingAction, projectContactList,
        FORM_ACTION_NOW, projectList);
    printPressEnterScript(out);
    printFetchAndUpdateTimesScript(appReq, out, completingAction);
    out.println("</form>");

    printEditProjectActionForm(appReq, completingAction, projectContactList, formName, formNameSet, project,
        projectList);

  }

  private List<Project> getProjectList(WebUser webUser, Session dataSession) {
    String queryString = "from Project where provider = ?";
    queryString += " and phaseCode <> 'Clos'";
    queryString += " order by projectName";
    Query query = dataSession.createQuery(queryString);
    query.setParameter(0, webUser.getProvider());
    @SuppressWarnings("unchecked")
    List<Project> projectList = query.list();
    return projectList;
  }

  private void printActionsNext(AppReq appReq, Project project, List<ProjectContact> projectContactList,
      List<Project> projectList, List<ProjectActionTaken> projectActionTakenList,
      List<ProjectActionNext> projectActionScheduledList, Set<String> formNameSet) {

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
    Map<String, List<ProjectActionNext>> projectActionMap = new HashMap<String, List<ProjectActionNext>>();
    setupListAndMap(webUser, projectActionScheduledList, dateLabelList, projectActionMap);

    // Go through dateLabelList and print out actions for each entry, as a list
    for (String label : dateLabelList) {
      List<ProjectActionNext> paList = projectActionMap.get(label);
      if (paList != null && paList.size() > 0) {
        out.println("<h4>" + label + "</h4>");
        out.println("<ul>");
        for (ProjectActionNext pa : paList) {
          out.println("<li>");
          out.println(
              "<a href=\"ProjectActionServlet?" + PARAM_COMPLETING_ACTION_NEXT_ID + "=" + pa.getActionNextId() + "\">"
                  + pa.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a>");
          out.println(" <a href=\"javascript: void(0); \" onclick=\" document.getElementById('formDialog"
              + pa.getActionNextId() + "').style.display = 'flex';\" class=\"edit-link\">Edit</a>");
          out.println("</li>");
        }
        out.println("</ul>");
      }
    }
    printEditProjectActionForm(appReq, null, projectContactList, "" +
        0, formNameSet, project, projectList);
    for (ProjectActionNext pa : projectActionScheduledList) {
      printEditProjectActionForm(appReq, pa, projectContactList, "" +
          pa.getActionNextId(), formNameSet, project, projectList);
    }

    if (projectActionTakenList.size() > 0) {
      out.println("<h4>Recent Past Actions</h4>");
      out.println("<ul>");
      int maxCount = 12;
      SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM yyyy");
      for (ProjectActionTaken pa : projectActionTakenList) {
        out.println("<li>");
        out.println(pa.getActionDescription());
        out.println(" (" + sdf.format(pa.getActionDate()) + ")");
        out.println("</li>");
        maxCount--;
        if (maxCount == 0) {
          break;
        }
      }
      out.println("</ul>");
    }

  }

  private List<ProjectActionNext> getAllProjectActionsScheduledList(AppReq appReq, Project project,
      Session dataSession) {
    Query query = dataSession.createQuery(
        "select distinct pan from ProjectActionNext pan "
            + "left join fetch pan.project "
            + "left join fetch pan.contact "
            + "left join fetch pan.nextProjectContact "
            + "where pan.projectId = ? and pan.nextDescription <> '' "
            + "and pan.nextActionStatusString = :nextActionStatus "
            + "order by pan.nextActionDate asc");
    query.setParameter(0, project.getProjectId());
    query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> allProjectActionsList = query.list();
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

  private void printFetchAndUpdateTimesScript(AppReq appReq, PrintWriter out,
      ProjectActionNext completingAction) {
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    boolean showPersonal = isShowPersonal(request);
    String link = "ProjectActionServlet?" + PARAM_ACTION + "=" + ACTION_REFRESH_TIME + "&"
        + PARAM_COMPLETING_ACTION_NEXT_ID
        + "=" + completingAction.getActionNextId()
        + (showWork ? "&" + PARAM_SHOW_WORK + "=Y" : "")
        + (showPersonal ? "&" + PARAM_SHOW_PERSONAL + "=Y" : "");
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

  private void setupListAndMap(WebUser webUser, List<ProjectActionNext> allProjectActionsList,
      List<String> dateLabelList, Map<String, List<ProjectActionNext>> projectActionMap) {
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

    for (ProjectActionNext pa : allProjectActionsList) {
      if (pa.getNextActionType().equals(ProjectNextActionType.GOAL)) {
        addToMap(projectActionMap, pa, "Goals");
      } else if (pa.isTemplate()) {
        addToMap(projectActionMap, pa, "Templates");
      } else if (pa.getNextActionStatus() != null
          && pa.getNextActionStatus().equals(ProjectNextActionStatus.PROPOSED)) {
        addToMap(projectActionMap, pa, "Proposed");
      } else if (pa.hasNextActionDate()) {
        String label = "Overdue";
        if (pa.getNextActionDate().before(dateList.get(0))) {
          addToMap(projectActionMap, pa, label);
        } else {
          label = "Today";
          boolean found = false;
          for (int i = 0; i < dateList.size(); i++) {
            Date date = dateList.get(i);
            if (sameDayOrBefore(pa, date, webUser)) {
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

  private static boolean sameDayOrBefore(ProjectActionNext pa, Date dateFromDateList,
      WebUser webUser) {
    Date nextActionDate = pa.getNextActionDate();
    // need to chop off the time from the date
    Calendar c = webUser.getCalendar(dateFromDateList);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    dateFromDateList = c.getTime();

    return dateFromDateList.equals(nextActionDate) || nextActionDate.before(dateFromDateList);
  }

  private void addToMap(Map<String, List<ProjectActionNext>> projectActionMap, ProjectActionNext pa, String key) {
    List<ProjectActionNext> paList = projectActionMap.get(key);
    if (paList == null) {
      paList = new ArrayList<ProjectActionNext>();
      projectActionMap.put(key, paList);
    }
    paList.add(pa);
  }

  private void readCompletingAction(HttpServletRequest request, AppReq appReq, Session dataSession) {
    String completingActionNextIdString = request.getParameter(PARAM_COMPLETING_ACTION_NEXT_ID);
    if (completingActionNextIdString != null) {
      ProjectActionNext completingProjectAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
          Integer.parseInt(completingActionNextIdString));
      appReq.setCompletingAction(completingProjectAction);
    }
  }

  private ProjectActionNext readEditProjectAction(AppReq appReq) {
    Session dataSession = appReq.getDataSession();
    HttpServletRequest request = appReq.getRequest();
    String actionNextIdString = request.getParameter(PARAM_EDIT_ACTION_NEXT_ID);
    ProjectActionNext editProjectAction = null;
    if (actionNextIdString != null) {
      editProjectAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
          Integer.parseInt(actionNextIdString));
    }
    return editProjectAction;
  }

  private ProjectActionNext saveProjectAction(AppReq appReq,
      ProjectActionNext editProjectAction, Project nextProject) {
    HttpServletRequest request = appReq.getRequest();
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    boolean isNewAction = editProjectAction == null;
    if (isNewAction) {
      editProjectAction = new ProjectActionNext();
      editProjectAction.setProject(nextProject);
      editProjectAction.setProjectId(nextProject.getProjectId());
      editProjectAction.setBillable(resolveBillable(dataSession, nextProject));
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
    editProjectAction.setNextDescription(trim(request.getParameter(PARAM_NEXT_DESCRIPTION), 1200));
    if (nextTimeEstimate > 0) {
      editProjectAction.setNextTimeEstimate(nextTimeEstimate);
    }
    editProjectAction.setNextChangeDate(new Date());
    String nextNote = request.getParameter(PARAM_NEXT_NOTE);
    if (nextNote != null && nextNote.length() > 0) {
      if (editProjectAction.getNextNotes() != null && editProjectAction.getNextNotes().trim().length() > 0) {
        nextNote = editProjectAction.getNextNotes() + "\n - " + nextNote;
      } else {
        nextNote = LIST_START + nextNote;
      }
      editProjectAction.setNextNotes(nextNote);
    }

    editProjectAction.setNextActionDate(parseDate(appReq, request.getParameter(PARAM_NEXT_ACTION_DATE)));
    editProjectAction.setNextTargetDate(parseDate(appReq, request.getParameter(PARAM_NEXT_TARGET_DATE)));
    editProjectAction.setNextDeadlineDate(parseDate(appReq, request.getParameter(PARAM_NEXT_DEADLINE_DATE)));
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
    String processStageString = request.getParameter(PARAM_PROCESS_STAGE);
    if (processStageString == null || processStageString.equals("")) {
      editProjectAction.setProcessStage(null);
    } else {
      editProjectAction.setProcessStage(ProcessStage.getProcessStage(processStageString));
    }
    String timeSlotString = request.getParameter(PARAM_TIME_SLOT);
    if (timeSlotString == null || timeSlotString.equals("")) {
      editProjectAction.setTimeSlot(null);
    } else {
      editProjectAction.setTimeSlot(TimeSlot.getTimeSlot(timeSlotString));
    }

    String nextActionType = request.getParameter(PARAM_NEXT_ACTION_TYPE);
    editProjectAction.setNextActionType(nextActionType);
    int priorityLevel = editProjectAction.getProject().getPriorityLevel();
    editProjectAction.setPriorityLevel(priorityLevel);
    String nextContactIdString = request.getParameter(PARAM_NEXT_CONTACT_ID);
    if (nextContactIdString != null && nextContactIdString.length() > 0) {
      editProjectAction.setNextContactId(Integer.parseInt(nextContactIdString));
    }
    editProjectAction.setProvider(webUser.getProvider());
    if (editProjectAction.getNextActionStatus() == null) {
      if (editProjectAction.hasNextDescription()) {
        if (editProjectAction.hasNextActionDate()) {
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

  private void closeAction(AppReq appReq, ProjectActionNext projectAction, Project project,
      String nextDescription, ProjectNextActionStatus nextActionStatus) {
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    Transaction trans = dataSession.beginTransaction();
    if (nextDescription != null && !nextDescription.trim().isEmpty()) {
      ProjectActionTaken actionTaken = new ProjectActionTaken();
      actionTaken.setProject(project);
      actionTaken.setProjectId(project.getProjectId());
      actionTaken.setActionDate(new Date());
      actionTaken.setActionDescription(nextDescription);
      actionTaken.setProvider(webUser.getProvider());
      actionTaken.setContact(webUser.getProjectContact());
      actionTaken.setContactId(webUser.getContactId());
      dataSession.saveOrUpdate(actionTaken);
    }
    projectAction.setNextActionStatus(nextActionStatus);
    projectAction.setNextChangeDate(new Date());
    dataSession.update(projectAction);
    trans.commit();
  }

  private String sendEmail(HttpServletRequest request, AppReq appReq, WebUser webUser, Session dataSession,
      Project project, ProjectActionNext completingAction, String emailBody) {
    boolean userAssignedToProject = false;
    Query query = dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
    query.setParameter(0, project.getProjectId());
    @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        List<ProjectContact> projectContactList = query.list();
        ProjectContact projectContact = projectContactList.get(0);
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
      msg.append(completingAction.getNextSummary());
      msg.append("</p>");
      if (completingAction.getNextDescription() != null
          && !completingAction.getNextDescription().equals("")) {
        msg.append("<p>");
        msg.append(completingAction.getNextDescriptionForDisplay(webUser.getProjectContact()));
        if (completingAction.getNextActionDate() != null) {
          SimpleDateFormat sdf11 = webUser.getDateFormat();
          msg.append(" ");
          msg.append(sdf11.format(completingAction.getNextActionDate()));
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

  private void startTimer(AppReq appReq, Session dataSession, ProjectActionNext completingAction) {
    TimeTracker timeTracker = appReq.getTimeTracker();
    if (timeTracker != null && completingAction != null) {
      timeTracker.startClock(completingAction.getProject(), completingAction, dataSession);
    }
  }

  private void readNotesSummaryUpdateTimeTracker(HttpServletRequest request, AppReq appReq, Session dataSession,
      ProjectActionNext completingAction) {
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
      timeTracker.update(completingAction, dataSession);
    }
    {
      // query the Bill Entry table for the bill entry with the same actionNextId, sum
      // up
      // the time spent
      // Here is the query: select sum(bill_mins) from bill_entry where action_id =
      // {action_id}
      Query query = dataSession.createQuery(
          "select sum(billMins) from BillEntry where action.actionNextId = :actionNextId "
              + "and startTime >= :today and startTime < :tomorrow");
      query.setParameter("actionNextId", completingAction.getActionNextId());
      Calendar calendar = getCalendarForTodayNoTime(appReq.getWebUser());
      query.setParameter("today", calendar.getTime());
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      query.setParameter("tomorrow", calendar.getTime());
      @SuppressWarnings("unchecked")
      List<Long> billMinsList = query.list();
      if (billMinsList.size() > 0) {
        if (billMinsList.get(0) != null) {
          int billMins = billMinsList.get(0).intValue();
          if (completingAction.getNextTimeActual() == null || completingAction.getNextTimeActual() != billMins) {
            completingAction.setNextTimeActual(billMins);
            isChanged = true;
          }
        } else if (completingAction.getNextTimeActual() == null
            || completingAction.getNextTimeActual().intValue() != 0) {
          completingAction.setNextTimeActual(0);
          isChanged = true;
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

  private ProjectActionNext postponeActionToNextWorkingDay(AppReq appReq, Session dataSession) {
    HttpServletRequest request = appReq.getRequest();
    String actionNextIdString = request.getParameter(PARAM_POSTPONE_ACTION_NEXT_ID);
    if (actionNextIdString == null) {
      return null;
    }
    int actionNextId;
    try {
      actionNextId = Integer.parseInt(actionNextIdString);
    } catch (NumberFormatException nfe) {
      return null;
    }
    ProjectActionNext projectAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
        actionNextId);
    if (projectAction == null) {
      return null;
    }
    Calendar todayCalendar = getCalendarForTodayNoTime(appReq.getWebUser());
    Calendar calendar = getCalendarForTodayNoTime(appReq.getWebUser());
    if (projectAction.getNextActionDate() != null && projectAction.getNextActionDate().after(todayCalendar.getTime())) {
      calendar.setTime(projectAction.getNextActionDate());
    }
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    if (dayOfWeek == Calendar.FRIDAY) {
      calendar.add(Calendar.DAY_OF_MONTH, 3);
    } else if (dayOfWeek == Calendar.SATURDAY) {
      calendar.add(Calendar.DAY_OF_MONTH, 2);
    } else {
      calendar.add(Calendar.DAY_OF_MONTH, 1);
    }
    Date postponedDueDate = calendar.getTime();
    projectAction.setNextActionDate(postponedDueDate);
    if (ProjectNextActionType.COMMITTED_TO.equals(projectAction.getNextActionType())) {
      projectAction.setNextActionType(ProjectNextActionType.OVERDUE_TO);
    }

    // Case 1: nextDeadline set but not nextTarget - keep same logic as today
    if (projectAction.getNextDeadlineDate() != null && projectAction.getNextTargetDate() == null) {
      Date deadlineDate = projectAction.getNextDeadlineDate();
      Date postponedDateOnly = postponedDueDate;
      if (postponedDateOnly.equals(deadlineDate)) {
        projectAction.setNextActionType(ProjectNextActionType.COMMITTED_TO);
      } else if (postponedDateOnly.after(deadlineDate)) {
        projectAction.setNextActionType(ProjectNextActionType.OVERDUE_TO);
      }
    }
    // Case 2: nextDeadline set AND nextTarget is set
    else if (projectAction.getNextDeadlineDate() != null && projectAction.getNextTargetDate() != null) {
      Date targetDate = projectAction.getNextTargetDate();
      Date deadlineDate = projectAction.getNextDeadlineDate();
      Date postponedDateOnly = postponedDueDate;

      // At or past deadline date â†’ OVERDUE_TO
      if (postponedDateOnly.equals(deadlineDate) || postponedDateOnly.after(deadlineDate)) {
        projectAction.setNextActionType(ProjectNextActionType.OVERDUE_TO);
      }
      // At or past target date (but before deadline) â†’ COMMITTED_TO
      else if (postponedDateOnly.equals(targetDate) || postponedDateOnly.after(targetDate)) {
        projectAction.setNextActionType(ProjectNextActionType.COMMITTED_TO);
      }
    }
    // Case 3: nextDeadline NOT set, but nextTarget IS set
    else if (projectAction.getNextDeadlineDate() == null && projectAction.getNextTargetDate() != null) {
      Date targetDate = projectAction.getNextTargetDate();
      Date postponedDateOnly = postponedDueDate;

      // At or past target date â†’ COMMITTED_TO
      if (postponedDateOnly.equals(targetDate) || postponedDateOnly.after(targetDate)) {
        projectAction.setNextActionType(ProjectNextActionType.COMMITTED_TO);
      }
    }
    projectAction.setNextChangeDate(new Date());
    Transaction transaction = dataSession.beginTransaction();
    dataSession.update(projectAction);
    transaction.commit();
    return projectAction;
  }

  private void chatPropose(AppReq appReq, ProjectActionNext completingAction, List<ChatAgent> chatAgentList,
      List<ProjectActionTaken> projectActionTakenList, List<ProjectActionNext> projectActionScheduledList) {

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

  private void chatFeedback(AppReq appReq, ProjectActionNext completingAction,
      List<ChatAgent> chatAgentList, List<ProjectActionTaken> projectActionTakenList,
      List<ProjectActionNext> projectActionScheduledList) {

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

  private String chatNext(AppReq appReq, ProjectActionNext completingAction,
      List<ChatAgent> chatAgentList, List<ProjectActionTaken> projectActionTakenList,
      List<ProjectActionNext> projectActionScheduledList) {

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

  private String getProposePrompt(ProjectActionNext completingAction, AppReq appReq,
      List<ProjectActionTaken> projectActionTakenList, List<ProjectActionNext> projectActionScheduledList) {
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

  private String getFeedbackPrompt(ProjectActionNext completingAction, AppReq appReq,
      List<ProjectActionTaken> projectActionTakenList, List<ProjectActionNext> projectActionScheduledList) {
    String feedbackPrompt = getBasePrompt(completingAction, appReq, projectActionTakenList, projectActionScheduledList);
    feedbackPrompt += "Please review my current summary and give me three to five questions my superisor or others might have that could help me clarify and add more detail to this update. "
        + " Please give this to me as a list of items for me to consider. Give this to me as list of unordered items in an HTML list. "
        + " Send me a JSON response where the key 'html' contains the HTML code for a list of suggestions.. Thanks! \n";

    return feedbackPrompt;
  }

  private String getNextPrompt(ProjectActionNext completingAction, AppReq appReq,
      List<ProjectActionTaken> projectActionTakenList, List<ProjectActionNext> projectActionScheduledList) {
    String nextPrompt = getBasePrompt(completingAction, appReq, projectActionTakenList, projectActionScheduledList);
    nextPrompt += "Please review my current summary of what I accomplished and what I have accomplished in the past with this project and suggest a list "
        + "of two or three next steps I should indicate in my project tracker. These statements should be short and should start with one of these phrases:  "
        + "I will ..., I might ..., I have committed ..., I will meet ..., I will review ..., I will document ..., I will follow up ..., or I am waiting for ... \n\n";
    List<ProjectActionNext> projectActionsScheduledAndCompletedList = getProjectActionsScheduledAndCompletedList(
        appReq.getDataSession(), completingAction.getProject().getProjectId());
    if (projectActionsScheduledAndCompletedList.size() > 0) {
      nextPrompt += "Here are examples of steps previously taken on this project: \n";
      for (ProjectActionNext projectAction : projectActionsScheduledAndCompletedList) {
        nextPrompt += LIST_START + projectAction.getNextDescription() + " \n";
      }
    }
    nextPrompt += " Please give this to me as a list of items for me to consider. Give this to me as list of unordered items in an HTML list. "
        + " Send me a JSON response where the key 'html' contains the HTML code for a list of suggestions. Thanks! \n";
    return nextPrompt;
  }

  private String getBasePrompt(ProjectActionNext completingAction, AppReq appReq,
      List<ProjectActionTaken> projectActionTakenList, List<ProjectActionNext> projectActionScheduledList) {
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
    for (ProjectActionTaken pa : projectActionTakenList) {
      basePrompt += LIST_START + sdf.format(pa.getActionDate()) + " " + pa.getActionDescription() + " \n";
      limit--;
      if (limit == 0) {
        break;
      }
    }
    if (projectActionScheduledList.size() > 1) {
      basePrompt += "Actions scheduled in the future: \n";
      for (ProjectActionNext projectAction : projectActionScheduledList) {
        if (projectAction.equals(completingAction)) {
          continue;
        }
        basePrompt += LIST_START + sdf.format(projectAction.getNextActionDate()) + " "
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

  private void printEditProjectActionForm(AppReq appReq, ProjectActionNext editProjectAction,
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
    out.println("      form." + PARAM_NEXT_ACTION_DATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_DESCRIPTION + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_CONTACT_ID + ".disabled = false;");
    out.println("      form." + PARAM_START_SENTANCE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_TIME_ESTIMATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_TARGET_DATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_DEADLINE_DATE + ".disabled = false;");
    out.println("      form." + PARAM_LINK_URL + ".disabled = false;");
    out.println("      form." + PARAM_TEMPLATE_TYPE + ".disabled = false;");
    out.println("      form." + PARAM_PROCESS_STAGE + ".disabled = false;");
    out.println("      if (form." + PARAM_NEXT_ACTION_DATE + ".value == \"\")");
    out.println("      {");
    out.println("       document.projectAction" + formName + "." + PARAM_NEXT_ACTION_DATE + ".value = '"
        + sdf.format(new Date()) + "';");
    out.println("      }");
    out.println("    }");
    out.println("    function setNextAction" + formName + "(nextActionDate) {");
    out.println("      document.projectAction" + formName + "." + PARAM_NEXT_ACTION_DATE + ".value = nextActionDate;");
    out.println("      enableForm" + formName + "(); ");
    out.println("    }");
    out.println("    function setNextDeadlineDate" + formName + "(nextDeadlineDate) {");
    out.println(
        "      document.projectAction" + formName + "." + PARAM_NEXT_DEADLINE_DATE + ".value = nextDeadlineDate;");
    out.println("    }");
    out.println("    function setNextTargetDate" + formName + "(nextTargetDate) {");
    out.println(
        "      document.projectAction" + formName + "." + PARAM_NEXT_TARGET_DATE + ".value = nextTargetDate;");
    out.println("    }");
    printGenerateSelectNextTimeEstimateFunction(out, formName);
    out.println("  </script>");
    if (editProjectAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_EDIT_ACTION_NEXT_ID + "\" value=\""
          + editProjectAction.getActionNextId() + "\">");
    }
    printCurrentActionEdit(appReq, webUser, out, editProjectAction, project, projectContactList, formName, projectList);

    out.println("</form>");
    out.println("</div>");
  }

  private void printActionsScheduledForToday(AppReq appReq, List<ProjectActionNext> projectActionList,
      List<ProjectActionNext> projectActionOverdueList) {
    PrintWriter out = appReq.getOut();
    out.println("<table class=\"boxed\">");
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    int colspan = showWork ? 4 : 2;
    if (projectActionOverdueList.size() > 0) {
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Overdue</th>");
      if (showWork) {
        out.println("    <th class=\"boxed\">Est</th>");
        out.println("    <th class=\"boxed\">Act</th>");
      }
      out.println("  </tr>");
      printActionItems(projectActionOverdueList, appReq, showWork);
    }
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"" + colspan + "\">All actions scheduled for today</th>");
    out.println("  </tr>");
    printDueTable(appReq, ProjectNextActionType.OVERDUE_TO, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.COMMITTED_TO, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_CONTACT, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_MEET, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.MIGHT, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.GOAL, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_FOLLOW_UP, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WAITING, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_REVIEW, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_DOCUMENT, projectActionList, showWork);
    out.println("</table><br/>");
  }

  private void printActionsScheduledForNextWorkingDay(AppReq appReq,
      List<ProjectActionNext> projectActionList, Date workingDay) {
    PrintWriter out = appReq.getOut();
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    int colspan = showWork ? 4 : 2;
    String title = "All actions scheduled for ";
    // print name of next working day, either "Monday" or "Next Monday". that format
    Calendar nextWorkingDay = appReq.getWebUser().getCalendar(workingDay);
    SimpleDateFormat daynameSdf = new SimpleDateFormat("EEEE dd MMMM yyyy");
    title += daynameSdf.format(nextWorkingDay.getTime());
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"" + colspan + "\">" + title + "</th>");
    out.println("  </tr>");
    printDueTable(appReq, ProjectNextActionType.OVERDUE_TO, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.COMMITTED_TO, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_CONTACT, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_MEET, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.MIGHT, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.GOAL, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_FOLLOW_UP, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WAITING, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_REVIEW, projectActionList, showWork);
    printDueTable(appReq, ProjectNextActionType.WILL_DOCUMENT, projectActionList, showWork);
    out.println("</table><br/>");
    // print out size of list
  }

  private void printActionsCompletedForToday(AppReq appReq, List<ProjectActionNext> projectActionList) {
    PrintWriter out = appReq.getOut();
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    int colspan = showWork ? 4 : 2;
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"" + colspan + "\">All actions completed for today</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Completed</th>");
    if (showWork) {
      out.println("    <th class=\"boxed\">Est</th>");
      out.println("    <th class=\"boxed\">Act</th>");
    }
    out.println("  </tr>");
    printActionItems(projectActionList, appReq, showWork);
    out.println("</table><br/>");
  }

  private void printActionsDeletedWithTimeForToday(AppReq appReq,
      List<ProjectNarrativeDao.ActionWithMinutes> deletedActions) {
    if (deletedActions == null || deletedActions.isEmpty()) {
      return;
    }
    Project project = appReq.getProjectSelected();
    if (project == null) {
      project = appReq.getProject();
    }
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    int colspan = showWork ? 4 : 2;
    PrintWriter out = appReq.getOut();
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"" + colspan + "\">Deleted actions with time today</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Deleted</th>");
    if (showWork) {
      out.println("    <th class=\"boxed\">Est</th>");
      out.println("    <th class=\"boxed\">Act</th>");
    }
    out.println("  </tr>");
    for (ProjectNarrativeDao.ActionWithMinutes action : deletedActions) {
      String description = action.getDescription() == null ? "" : action.getDescription();
      String link = "ProjectActionServlet?" + PARAM_COMPLETING_ACTION_NEXT_ID + "=" + action.getActionTakenId();
      out.println("  <tr class=\"boxed\">");
      if (project == null) {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      } else {
        out.println("    <td class=\"boxed\"><a href=\"" + link + "\" class=\"button\">"
            + escapeHtml(project.getProjectName()) + "</a></td>");
      }
      out.println("    <td class=\"boxed\"><a href=\"" + link + "\" class=\"button\">"
          + escapeHtml(description) + "</a></td>");
      if (showWork) {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
        out.println("    <td class=\"boxed\">" + TimeTracker.formatTime(action.getMinutes()) + "</td>");
      }
      out.println("  </tr>");
    }
    out.println("</table><br/>");
  }

  private void printDeletedActionsWithoutTimeForToday(AppReq appReq,
      List<ProjectNarrativeDao.Action> deletedActions) {
    if (deletedActions == null || deletedActions.isEmpty()) {
      return;
    }
    PrintWriter out = appReq.getOut();
    out.print("<p>Deleted actions with no time: ");
    for (int i = 0; i < deletedActions.size(); i++) {
      ProjectNarrativeDao.Action action = deletedActions.get(i);
      String description = action.getDescription() == null ? "" : action.getDescription();
      String link = "ProjectActionServlet?" + PARAM_COMPLETING_ACTION_NEXT_ID + "=" + action.getActionTakenId();
      if (i > 0) {
        out.print(", ");
      }
      out.print("<a href=\"" + link + "\" class=\"button\">" + escapeHtml(description) + "</a>");
    }
    out.println("</p>");
  }

  private void printTimeManagementBox(AppReq appReq, List<ProjectActionNext> projectActionList) {
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
    HttpServletRequest request = appReq.getRequest();
    boolean showWork = isShowWork(request);
    boolean showPersonal = isShowPersonal(request);
    out.println("<h4>Show</h4>");
    out.println("<form id=\"workPersonalFilterForm\" method=\"GET\" action=\"ProjectActionServlet\">");
    out.println("  <input type=\"hidden\" name=\"" + PARAM_FILTER_SUBMITTED + "\" value=\"Y\"/>");
    ProjectActionNext completingAction = appReq.getCompletingAction();
    if (completingAction != null) {
      out.println("  <input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_NEXT_ID + "\" value=\""
          + completingAction.getActionNextId() + "\"/>");
    }
    Project project = appReq.getProject();
    if (project != null) {
      out.println("  <input type=\"hidden\" name=\"" + PARAM_PROJECT_ID + "\" value=\""
          + project.getProjectId() + "\"/>");
    }
    out.println("  <label><input type=\"checkbox\" name=\"" + PARAM_SHOW_WORK
        + "\" value=\"Y\" onchange=\"this.form.submit()\""
        + (showWork ? " checked" : "") + "> Work</label>");
    out.println("  <label><input type=\"checkbox\" name=\"" + PARAM_SHOW_PERSONAL
        + "\" value=\"Y\" onchange=\"this.form.submit()\""
        + (showPersonal ? " checked" : "") + "> Personal</label>");
    out.println("</form>");
  }

  private void printTimeManagementBoxForNextWorkingDay(AppReq appReq, List<ProjectActionNext> projectActionList,
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
      ProjectActionNext projectAction, Project project, String formName, String disabled,
      List<ProjectContact> projectContactList,
      List<Project> projectList) {
    SimpleDateFormat sdf1;
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"inside\">");
    SimpleDateFormat sdf2 = webUser.getDateFormat("MM/dd/yyyy");
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
        String nextActionDateString = projectAction == null || projectAction.getNextActionDate() == null
            ? request.getParameter(PARAM_NEXT_ACTION_DATE)
            : sdf2.format(projectAction.getNextActionDate());
        out.println(
            "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_NEXT_ACTION_DATE
                + "\" size=\"10\" value=\""
                + n(nextActionDateString) + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
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
              + "\" size=\"30\" onkeydown=\"resetRefresh()\" " + disabled + "></textarea></td>");
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
      out.println("          <th class=\"inside\">Target</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_NEXT_TARGET_DATE
              + "\" size=\"10\" value=\""
              + n(projectAction == null || projectAction.getNextTargetDate() == null
                  ? request.getParameter(PARAM_NEXT_TARGET_DATE)
                  : sdf1.format(projectAction.getNextTargetDate()))
              + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      out.println("            <font size=\"-1\">");
      sdf1 = webUser.getDateFormat();
      calendar.add(Calendar.DAY_OF_MONTH, 2);
      out.println("              <a href=\"javascript: void setNextTargetDate" + formName + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 7; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextTargetDate" + formName + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextTargetDate" + formName + "('"
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
      Calendar calendar = webUser.getCalendar();
      SimpleDateFormat day = webUser.getDateFormat("EEE");
      out.println("        <tr>");
      out.println("          <th class=\"inside\">Deadline</th>");
      out.println(
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_NEXT_DEADLINE_DATE
              + "\" size=\"10\" value=\""
              + n(projectAction == null || projectAction.getNextDeadlineDate() == null
                  ? request.getParameter(PARAM_NEXT_DEADLINE_DATE)
                  : sdf1.format(projectAction.getNextDeadlineDate()))
              + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      out.println("            <font size=\"-1\">");
      sdf1 = webUser.getDateFormat();
      calendar.add(Calendar.DAY_OF_MONTH, 2);
      out.println("              <a href=\"javascript: void setNextDeadlineDate" + formName + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 7; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextDeadlineDate" + formName + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextDeadlineDate" + formName + "('"
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
      out.println("            Process Stage: ");
      out.println("           <select name=\"processStage\" value=\""
          + n((projectAction == null || projectAction.getProcessStage() == null)
              ? request.getParameter(PARAM_PROCESS_STAGE)
              : projectAction.getProcessStage().getId())
          + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      // default empty option for no template
      out.println("             <option value=\"\">none</option>");
      for (ProcessStage processStage : ProcessStage.values()) {
        out.println("             <option value=\"" + processStage.getId() + "\""
            + (projectAction != null && projectAction.getProcessStage() == processStage ? " selected"
                : "")
            + ">" + processStage.getLabel() + "</option>");
      }
      out.println("            </select>");
      out.println("          </td>");
      out.println("        </tr>");
    }
    out.println("      </table>");
    out.println("    </td>");
    out.println("  </tr>");
  }

  private void printActionNow(AppReq appReq, WebUser webUser, PrintWriter out, ProjectActionNext completingAction,
      List<ProjectContact> projectContactList,
      String formName, List<Project> projectList) {
    SimpleDateFormat sdf11 = webUser.getDateFormat();
    out.println("<h3>" + completingAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</h3>");
    out.println("<p>" + getNextActionTitle(completingAction));
    out.println(" <a href=\"javascript: void(0); \" onclick=\" document.getElementById('formDialog"
        + completingAction.getActionNextId() + "').style.display = 'flex';\" class=\"edit-link\">Edit</a>");
    if (completingAction.getLinkUrl() != null && completingAction.getLinkUrl().length() > 0) {
      out.println("<br/>Link: <a href=\"" + completingAction.getLinkUrl() + "\" target=\"_blank\">"
          + trim(completingAction.getLinkUrl(), 40) + "</a>");
    }
    if (completingAction.getNextDeadlineDate() != null) {
      Date today = normalizeDate(webUser, webUser.now());
      Date deadlineDate = completingAction.getNextDeadlineDate();
      if (deadlineDate.after(today)) {
        out.println("    <br/>Deadline: " + sdf11.format(completingAction.getNextDeadlineDate()));
      } else {
        out.println("    <br/><span class=\"fail\">Deadline Overdue:</span> "
            + sdf11.format(completingAction.getNextDeadlineDate()));
      }
    }
    out.println("</p>");
    out.println("<h3>Notes");
    String timeString = getTimeString(appReq, completingAction);
    if (!timeString.isEmpty()) {
      out.println(
          "<span class=\"float-right\" style=\"font-size: 14px;\">" + timeString + "</span>");
    }
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
    out.println("<input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_NEXT_ID + "\" value=\""
        + completingAction.getActionNextId() + "\"/>");
    out.println("<input type=\"hidden\" name=\"" + PARAM_EDIT_ACTION_NEXT_ID + "\" value=\""
        + completingAction.getActionNextId() + "\"/>");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_COMPLETED + "\"/>");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_DELETE + "\"/>");
    out.println("</span>");
    out.println("<h3>Next Action</h3>");
    out.println("<div class=\"input-container\">");
    out.println(
        "<input type=\"text\" id=\"sentenceInput\" name=\"" + PARAM_SENTENCE_INPUT
            + "\" placeholder=\"Type your sentence...\" autocomplete=\"off\">");
    out.println("        <div id=\"suggestions\"></div>");
    out.println("    </div>");
    printScriptForSuggestions(out, projectList);
    out.println("<br/><span class=\"right\">");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_SCHEDULE + "\"/>");
    out.println("<input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_SCHEDULE_AND_START + "\"/>");
    out.println("</span>");
  }

  private void printScriptForSuggestions(PrintWriter out, List<Project> projectList) {
    out.println("      <script>");
    out.print("const projectNames = [");
    for (int i = 0; i < projectList.size(); i++) {
      Project project = projectList.get(i);
      out.print("\"" + escapeHtml(project.getProjectName()) + "\"");
      if (i < projectList.size() - 1) {
        out.print(", ");
      }
    }
    out.println("];");
    out.println(
        "const actionVerbs = [\"I will\", \"I have committed\", \"I might\", \"I will meet\", \"I have set goal to\"];");
    out.println("const dateSuggestions = [\"today\", \"tomorrow\", \"Monday\", \"next Monday\", \"10/05/2025\"];");
    out.println("");
    out.println("const input = document.getElementById(\"sentenceInput\");");
    out.println("const suggestionsBox = document.getElementById(\"suggestions\");");
    out.println("");
    out.println("let currentSuggestions = [];");
    out.println("let selectedIndex = -1;");
    out.println("");
    out.println("input.addEventListener(\"input\", () => {");
    out.println("    const text = input.value;");
    out.println("    const colonIndex = text.indexOf(\":\");");
    out.println("");
    out.println("    let suggestions = [];");
    out.println("    if (colonIndex === -1) {");
    out.println("        // Suggest project names");
    out.println(
        "        suggestions = projectNames.filter(name => name.toLowerCase().startsWith(text.toLowerCase()));");
    out.println("      currentSuggestions = suggestions;");
    out.println("    } else {");
    out.println("        const beforeColon = text.substring(0, colonIndex).trim();");
    out.println("        const afterColon = text.substring(colonIndex + 1).trim();");
    out.println("");
    out.println("        // Validate project name");
    out.println("        if (!projectNames.includes(beforeColon)) {");
    out.println(
        "            suggestions = projectNames.filter(name => name.toLowerCase().includes(beforeColon.toLowerCase()));");
    out.println("        } else if (afterColon.length === 0) {");
    out.println("            // Suggest action verbs if none entered yet");
    out.println("            suggestions = actionVerbs;");
    out.println("        } else {");
    out.println("            // Filter action verbs based on typed input");
    out.println(
        "            suggestions = actionVerbs.filter(verb => verb.toLowerCase().startsWith(afterColon.toLowerCase()));");
    out.println("        }");
    out.println("        currentSuggestions = suggestions;");
    out.println("    }");
    out.println("    selectedIndex = -1;");
    out.println("    showSuggestions(suggestions, text);");
    out.println("});");
    out.println("");
    out.println("// Helper to show suggestions");
    out.println("function showSuggestions(suggestions, text) {");
    out.println("    suggestionsBox.innerHTML = \"\";");
    out.println("    suggestionsBox.style.display = suggestions.length ? \"block\" : \"none\";");
    out.println("    suggestions.forEach((suggestion, i) => {");
    out.println("        const div = document.createElement(\"div\");");
    out.println("        div.textContent = suggestion;");
    out.println("        if (i === selectedIndex) {");
    out.println("           div.style.backgroundColor = \"#e0e0e0\";");
    out.println("        }");
    out.println("        div.addEventListener(\"click\", () => {");
    out.println("            input.value = suggestion;");
    out.println("            acceptSuggestion(suggestion, text);");
    out.println("        });");
    out.println("        suggestionsBox.appendChild(div);");
    out.println("    });");
    out.println("}");
    out.println("");
    out.println("function acceptSuggestion(suggestion, text) {");
    out.println("    if (text.indexOf(\":\") === -1) {");
    out.println("        input.value = `${suggestion}: `;");
    out.println("    } else {");
    out.println("       const [beforeColon] = text.split(\":\");");
    out.println("       input.value = `${beforeColon.trim()}: ${suggestion} `;");
    out.println("    }");
    out.println("    suggestionsBox.style.display = \"none\";");
    out.println("    selectedIndex = -1;");
    out.println("}");
    out.println("");
    out.println("input.addEventListener(\"keydown\", (e) => {");
    out.println("    const visible = suggestionsBox.style.display === \"block\";");
    out.println("    if (visible && (e.key === \"ArrowDown\" || e.key === \"ArrowUp\")) {");
    out.println("        e.preventDefault();");
    out.println("        const count = currentSuggestions.length;");
    out.println("        if (e.key === \"ArrowDown\") {");
    out.println("            selectedIndex = (selectedIndex + 1) % count;");
    out.println("        } else {");
    out.println("            selectedIndex = (selectedIndex - 1 + count) % count;");
    out.println("        }");
    out.println("        showSuggestions(currentSuggestions, input.value);");
    out.println("    }");
    out.println("    if (visible && (e.key === \"Enter\" || e.key === \"Tab\")) {");
    out.println("        if (selectedIndex < 0) {");
    out.println("          selectedIndex = 0;"); // Default to first suggestion if none selected
    out.println("        }");
    // alert the selectedIndex and currentSuggestions length
    out.println(
        "        console.log(`Selected Index: ${selectedIndex}, Suggestions Length: ${currentSuggestions.length}`);");
    out.println("        if (selectedIndex < currentSuggestions.length) {");
    // console log if this path is taken
    out.println("            console.log(`Accepting suggestion: ${currentSuggestions[selectedIndex]}`);");
    out.println("            e.preventDefault();");
    out.println("            acceptSuggestion(currentSuggestions[selectedIndex], input.value);");
    out.println("        }");
    out.println("    }");
    out.println("    if (e.key === \"Escape\") {");
    out.println("        suggestionsBox.style.display = \"none\";");
    out.println("        selectedIndex = -1;");
    out.println("    }");
    out.println("});");
    out.println("");
    out.println("      </script>");
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

  private String getTimeString(AppReq appReq, ProjectActionNext completingAction) {
    if (!completingAction.isBillable()) {
      return "";
    }
    String timeString = TimeTracker.formatTime(completingAction.getNextTimeEstimate()) + " ";
    {
      TimeTracker timeTracker = appReq.getTimeTracker();
      if (timeTracker != null) {
        String t = TimeTracker.formatTime(completingAction.getNextTimeActual());
        if (timeTracker.isRunningClock()) {
          timeString += "<a href=\"ProjectActionServlet?" + PARAM_ACTION + "=" + ACTION_STOP_TIMER
              + "\" class=\"timerRunning\" id=\"" + ID_TIME_RUNNING + "\">" + t + "</a>";
        } else {
          timeString += "<a href=\"ProjectActionServlet?" + PARAM_COMPLETING_ACTION_NEXT_ID + "="
              + completingAction.getActionNextId()
              + "&" + PARAM_ACTION + "=" + ACTION_START_TIMER + "\" class=\"timerStopped\" id=\""
              + ID_TIME_RUNNING
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

  private String getNextActionTitle(ProjectActionNext completingAction) {
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

  private void printCurrentActionEdit(AppReq appReq, WebUser webUser, PrintWriter out, ProjectActionNext projectAction,
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

  private List<ProjectActionNext> getProjectActionListClosedToday(WebUser webUser, Session dataSession) {
    Date today = TimeTracker.createToday(webUser).getTime();
    Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
    Query query = dataSession.createQuery(
        "select distinct pan from ProjectActionNext pan "
            + "left join fetch pan.project "
            + "left join fetch pan.contact "
            + "left join fetch pan.nextProjectContact "
            + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
            + "and pan.nextActionStatusString = :nextActionStatus and pan.nextDescription <> '' "
            + "and pan.nextChangeDate >= :today and pan.nextChangeDate < :tomorrow "
            + "order by pan.nextTimeActual DESC, pan.nextTimeEstimate DESC");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter(PARAM_NEXT_CONTACT_ID, webUser.getContactId());
    query.setParameter("nextActionStatus", ProjectNextActionStatus.COMPLETED.getId());
    query.setParameter("today", today);
    query.setParameter("tomorrow", tomorrow);
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> projectActionList = query.list();
    return projectActionList;
  }

  private static List<ProjectActionNext> getProjectActionListForToday(WebUser webUser, Session dataSession,
      int dayOffset) {
    Date today = TimeTracker.createToday(webUser).getTime();
    Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
    if (dayOffset > 0) {
      // add number of days to today and tomorrow to get new "today" and "tomorrow"
      Calendar calendar = webUser.getCalendar(today);
      calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
      today = calendar.getTime();
      calendar.setTime(tomorrow);
      calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
      tomorrow = calendar.getTime();
    } else if (dayOffset < 0) {
      // getting overdue actions, going back a year
      Calendar calendar = webUser.getCalendar(today);
      calendar.add(Calendar.YEAR, -1);
      today = calendar.getTime();
      calendar.setTime(tomorrow);
      calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
      tomorrow = calendar.getTime();
    }
    Query query = dataSession.createQuery(
        "select distinct pan from ProjectActionNext pan "
            + "left join fetch pan.project "
            + "left join fetch pan.contact "
            + "left join fetch pan.nextProjectContact "
            + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
            + "and pan.nextDescription <> '' "
            + "and pan.nextActionStatusString = :nextActionStatus "
            + "and pan.nextActionDate >= :today and pan.nextActionDate < :tomorrow "
            + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter(PARAM_NEXT_CONTACT_ID, webUser.getContactId());
    query.setParameter("nextActionStatus", ProjectNextActionStatus.READY.getId());
    query.setParameter("today", today);
    query.setParameter("tomorrow", tomorrow);
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> projectActionList = query.list();
    sortProjectActionList(projectActionList);
    return projectActionList;
  }

  private static List<ProjectActionNext> getProjectActionListForPlanningRange(WebUser webUser, Session dataSession,
      Date startDate, Date endDate) {
    Query query = dataSession.createQuery(
        "select distinct pan from ProjectActionNext pan "
            + "left join fetch pan.project "
            + "left join fetch pan.contact "
            + "left join fetch pan.nextProjectContact "
            + "where pan.provider = :provider and (pan.contactId = :contactId or pan.nextContactId = :nextContactId) "
            + "and pan.nextDescription <> '' "
            + "and pan.nextActionDate >= :startDate and pan.nextActionDate < :endDate "
            + "order by pan.nextActionDate, pan.priorityLevel DESC, pan.nextTimeEstimate, pan.nextChangeDate");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter(PARAM_NEXT_CONTACT_ID, webUser.getContactId());
    query.setParameter("startDate", startDate);
    query.setParameter("endDate", endDate);
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> projectActionList = query.list();
    return projectActionList;
  }

  private static Date normalizeDate(WebUser webUser, Date date) {
    return webUser.startOfDay(date);
  }

  private static void sortProjectActionList(List<ProjectActionNext> projectActionList) {
    // sort the projectActionList first by the defaultPriority from the
    // ProjectNextActionType and then by the priority_level
    projectActionList.sort((pa1, pa2) -> {
      ProcessStage ps1 = pa1.getProcessStage();
      ProcessStage ps2 = pa2.getProcessStage();
      // If one of the priorities is special, then we need to sort by the special
      // priority, unless they are the same
      if ((ps1 != null || ps2 != null) && ps1 != ps2) {
        // very complicated logic to sort by priority special
        // FIRST must go first before any SECOND, or any other priority without a
        // special priority
        if (ps1 == ProcessStage.FIRST) {
          return -1;
        } else if (ps2 == ProcessStage.FIRST) {
          return 1;
        }
        // SECOND must go after any FIRST, but before any other priority without a
        // special priority
        if (ps1 == ProcessStage.SECOND) {
          return -1;
        } else if (ps2 == ProcessStage.SECOND) {
          return 1;
        }
        // LAST must go last after any other priority without a special priority and
        // PENULTIMATE
        if (ps1 == ProcessStage.LAST) {
          return 1;
        } else if (ps2 == ProcessStage.LAST) {
          return -1;
        }
        // PENUlTIMATE must go last after any other priority without a special priority,
        // but before any LAST
        if (ps1 == ProcessStage.PENULTIMATE) {
          return 1;
        } else if (ps2 == ProcessStage.PENULTIMATE) {
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
    String timeEstString = ProjectActionNext.getTimeForDisplay(timeEst);
    String timeActString = ProjectActionNext.getTimeForDisplay(timeAct);
    out.println("    <td class=\"boxed\" id=\"" + idBaseEst + "\">" + timeEstString + "</th>");
    out.println("    <td class=\"boxed\" id=\"" + idBaseAct + "\">" + timeActString + "</th>");
    out.println("  </tr>");
  }

  protected static void prepareProjectActionList(Session dataSession, List<ProjectActionNext> projectActionList,
      WebUser webUser) {
    {
      for (ProjectActionNext projectAction : projectActionList) {
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

  private static void printDueTable(AppReq appReq, String nextActionType, List<ProjectActionNext> projectActionList,
      boolean showWork) {
    PrintWriter out = appReq.getOut();

    List<ProjectActionNext> paList = new ArrayList<ProjectActionNext>();
    if (nextActionType == null) {
      paList.addAll(projectActionList);
    } else {
      for (ProjectActionNext projectAction : projectActionList) {
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
      if (showWork) {
        out.println("    <th class=\"boxed\">Est</th>");
        out.println("    <th class=\"boxed\">Act</th>");
      }
      out.println("  </tr>");
    }
    printActionItems(paList, appReq, showWork);
  }

  private static void printActionItems(List<ProjectActionNext> paList, AppReq appReq, boolean showWork) {
    PrintWriter out = appReq.getOut();
    WebUser webUser = appReq.getWebUser();
    for (ProjectActionNext projectAction : paList) {
      String link = "ProjectActionServlet?" + PARAM_COMPLETING_ACTION_NEXT_ID + "=" + projectAction.getActionNextId();
      String postponeLink = "ProjectActionServlet?" + PARAM_ACTION + "=" + ACTION_POSTPONE_NEXT_WORKING_DAY
          + "&" + PARAM_POSTPONE_ACTION_NEXT_ID + "=" + projectAction.getActionNextId();
      out.println("  <tr class=\"boxed\">");
      if (projectAction.getProject() == null) {
        out.println("    <td class=\"boxed\">&nbsp;</td>");
      } else {
        out.println("    <td class=\"boxed\"><a href=\"" + link + "\" class=\"button\">"
            + projectAction.getProject().getProjectName() + "</a></td>");
      }
      out.println("    <td class=\"boxed\"><a href=\"" + link + "\" class=\"button\">"
          + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a> "
          + "<a href=\"" + postponeLink
          + "\" class=\"button\" style=\"opacity: 0.6;\" title=\"Put off until next working day\">&#8594;</a></td>");
      if (showWork) {
        if (!projectAction.isBillable() || projectAction.getNextTimeEstimate() == null
            || projectAction.getNextTimeEstimate() == 0) {
          out.println("    <td class=\"boxed\">&nbsp;</a></td>");
        } else {
          out.println(
              "    <td class=\"boxed\">" + projectAction.getNextTimeEstimateForDisplay() + "</a></td>");
        }
        if (!projectAction.isBillable() || projectAction.getNextTimeActual() == null
            || projectAction.getNextTimeActual() == 0) {
          out.println("    <td class=\"boxed\">&nbsp;</a></td>");
        } else {
          out.println(
              "    <td class=\"boxed\">" + projectAction.getNextTimeActualForDisplay() + "</a></td>");
        }
      }
      out.println("  </tr>");
    }
  }

  protected static Date parseDate(AppReq appReq, String dateString) {
    Date date = null;
    if (dateString != null && dateString.length() > 0) {
      SimpleDateFormat sdf1 = appReq.getWebUser().getDateFormat("MM/dd/yyyy");
      try {
        date = sdf1.parse(dateString);
      } catch (Exception e) {
        // try again
        sdf1 = appReq.getWebUser().getDateFormat("MM/dd/yyyy");
        try {
          date = sdf1.parse(dateString);
        } catch (Exception e2) {
          appReq.setMessageProblem("Unable to read date: " + e2);
        }
      }
    }
    return date;
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

  protected static List<ProjectActionTaken> getProjectActionsTakenList(Session dataSession, Project project) {
    return getProjectActionsTakenList(dataSession, project.getProjectId());
  }

  private static List<ProjectActionTaken> getProjectActionsTakenList(Session dataSession, int projectId) {
    Query query = dataSession.createQuery(
        "select distinct pat from ProjectActionTaken pat "
            + "left join fetch pat.project "
            + "left join fetch pat.contact "
            + "where pat.projectId = ? and pat.actionDescription <> '' order by pat.actionDate desc");
    query.setParameter(0, projectId);
    @SuppressWarnings("unchecked")
    List<ProjectActionTaken> projectActionList = query.list();
    return projectActionList;
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

  public static void printProjectUpdateForm(AppReq appReq, int projectId,
      List<ProjectContact> projectContactList, ProjectActionNext projectAction,
      ProjectActionNext completingProjectAction) {
    HttpServletRequest request = appReq.getRequest();
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();
    PrintWriter out = appReq.getOut();
    Query query;

    query = dataSession.createQuery(
        "select distinct pan from ProjectActionNext pan "
            + "left join fetch pan.project "
            + "left join fetch pan.contact "
            + "left join fetch pan.nextProjectContact "
            + "where pan.projectId = ? and pan.nextDescription <> '' "
            + "and (pan.nextActionStatusString = :readyStatus "
            + "or pan.nextActionStatusString = :proposedStatus) "
            + "order by pan.nextActionDate asc");
    query.setParameter(0, projectId);
    query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
    query.setParameter("proposedStatus", ProjectNextActionStatus.PROPOSED.getId());
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> projectActionList = query.list();
    List<ProjectActionNext> projectActionTemplateList = new ArrayList<ProjectActionNext>();
    List<ProjectActionNext> projectActionGoalList = new ArrayList<ProjectActionNext>();
    {
      Date today = new Date();
      for (Iterator<ProjectActionNext> it = projectActionList.iterator(); it.hasNext();) {
        ProjectActionNext pa = it.next();
        if (pa.getNextActionDate() == null || pa.getNextActionDate().after(today)) {
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
    out.println("<input type=\"hidden\" name=\"" + PARAM_PROJECT_ID + "\" value=\"" + projectId + "\">");
    if (projectAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_ACTION_NEXT_ID + "\" value=\""
          + projectAction.getActionNextId() + "\">");
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
    String actionDescription = n(request.getParameter("actionDescription"));
    if (actionDescription.equals("")) {
      if (projectAction != null) {
        actionDescription = n(projectAction.getNextSummary());
      } else if (completingProjectAction != null) {
        actionDescription = n(completingProjectAction.getNextSummary());
      }
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
        "            document.projectAction" + projectId + ".nextActionDate.value = nextActionDate;");
    out.println("            enableForm" + projectId + "(); ");
    out.println("          }");
    out.println("          function setNextDeadlineDate" + projectId + "(nextDeadlineDate)");
    out.println("          {");
    out.println(
        "            document.projectAction" + projectId + ".nextDeadlineDate.value = nextDeadlineDate;");
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
    SimpleDateFormat sdf1 = webUser.getDateFormat("MM/dd/yyyy");
    {
      out.println("        <tr>");
      out.println("          <th class=\"inside\">When</th>");
      {
        String nextActionDate = projectAction == null || projectAction.getNextActionDate() == null
            ? request.getParameter(PARAM_NEXT_ACTION_DATE)
            : sdf1.format(projectAction.getNextActionDate());
        out.println(
            "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"nextActionDate\" size=\"10\" value=\""
                + n(nextActionDate) + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
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
        "            <input name=\"" + PARAM_START_SENTANCE + "\" size=\"40\" value=\"I will:\"" + disabled + ">");
    out.println("          </td>");
    out.println("          <th class=\"inside\">Who</th>");
    out.println("          <td class=\"inside\"> ");
    out.println("              <select name=\"nextContactId\" onchange=\"selectProjectActionType"
        + projectId + "(form.nextActionType.value);\"" + disabled
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
          "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"nextDeadlineDate\" size=\"10\" value=\""
              + n(projectAction == null || projectAction.getNextDeadlineDate() == null
                  ? request.getParameter(PARAM_NEXT_DEADLINE_DATE)
                  : sdf1.format(projectAction.getNextDeadlineDate()))
              + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      out.println("            <font size=\"-1\">");
      sdf1 = webUser.getDateFormat();
      calendar.add(Calendar.DAY_OF_MONTH, 2);
      out.println("              <a href=\"javascript: void setNextDeadlineDate" + projectId + "('"
          + sdf1.format(calendar.getTime()) + "');\" class=\"button\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 7; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void setNextDeadlineDate" + projectId + "('"
              + sdf1.format(calendar.getTime()) + "');\" class=\"button\">Next-"
              + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void setNextDeadlineDate" + projectId + "('"
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
      out.println("             <option value=\"\">none</option>");
      for (TemplateType templateType : TemplateType.values()) {
        out.println("             <option value=\"" + templateType.getId() + "\""
            + (projectAction != null && projectAction.getTemplateType() == templateType ? " selected" : "")
            + ">" + templateType.getLabel() + "</option>");
      }
      out.println("            </select>");
      // Show Process Stage only for billable projects, Time Slot only for
      // non-billable
      if (projectAction != null && projectAction.isBillable()) {
        out.println("            Process Stage: ");
        out.println("           <select name=\"processStage\" value=\""
            + n((projectAction == null || projectAction.getProcessStage() == null)
                ? request.getParameter(PARAM_PROCESS_STAGE)
                : projectAction.getProcessStage().getId())
            + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
        out.println("             <option value=\"\">none</option>");
        for (ProcessStage processStage : ProcessStage.values()) {
          out.println("             <option value=\"" + processStage.getId() + "\""
              + (projectAction != null && projectAction.getProcessStage() == processStage ? " selected" : "")
              + ">" + processStage.getLabel() + "</option>");
        }
        out.println("            </select>");
        // Hidden variable to carry Time Slot value even though not displayed
        out.println("            <input type=\"hidden\" name=\"timeSlot\" value=\""
            + n((projectAction == null || projectAction.getTimeSlot() == null)
                ? ""
                : projectAction.getTimeSlot().getId())
            + "\" />");
      } else {
        out.println("            Time Slot: ");
        out.println("           <select name=\"timeSlot\" value=\""
            + n((projectAction == null || projectAction.getTimeSlot() == null)
                ? request.getParameter(PARAM_TIME_SLOT)
                : projectAction.getTimeSlot().getId())
            + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
        out.println("             <option value=\"\">none</option>");
        for (TimeSlot timeSlot : TimeSlot.values()) {
          out.println("             <option value=\"" + timeSlot.getId() + "\""
              + (projectAction != null && projectAction.getTimeSlot() == timeSlot ? " selected" : "")
              + ">" + timeSlot.getLabel() + "</option>");
        }
        out.println("            </select>");
        // Hidden variable to carry Process Stage value even though not displayed
        out.println("            <input type=\"hidden\" name=\"processStage\" value=\""
            + n((projectAction == null || projectAction.getProcessStage() == null)
                ? ""
                : projectAction.getProcessStage().getId())
            + "\" />");
      }
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
      out.println("          <span class=\"together\"><input type=\"checkbox\" name=\""
          + PARAM_SEND_EMAIL_TO + projectContact1.getContactId() + "\" value=\"Y\"/>");
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
      PrintWriter out, List<ProjectActionNext> projectActionGoalList) {
    out.println("<table class=\"inside\" width=\"100%\">");
    out.println("  <tr>");
    out.println("    <th class=\"inside\">Date</th>");
    out.println("    <th class=\"inside\">Time</th>");
    out.println("    <th class=\"inside\">To Do</th>");
    out.println("    <th class=\"inside\">Status</th>");
    out.println("    <th class=\"inside\">Comp</th>");
    out.println("  </tr>");
    SimpleDateFormat sdf11 = webUser.getDateFormat();
    for (ProjectActionNext pa : projectActionGoalList) {
      Date today = new Date();
      Calendar calendar1 = webUser.getCalendar();
      calendar1.add(Calendar.DAY_OF_MONTH, -1);
      Date yesterday = calendar1.getTime();
      String editActionLink = "<a href=\"ProjectServlet?" + PARAM_PROJECT_ID + "=" + projectId + "&"
          + PARAM_ACTION_NEXT_ID + "=" + pa.getActionNextId() + "\" class=\"button\">";
      ProjectContact projectContact1 = (ProjectContact) dataSession.get(ProjectContact.class, pa.getContactId());
      pa.setContact(projectContact1);
      ProjectContact nextProjectContact = null;
      if (pa.getNextContactId() != null && pa.getNextContactId() > 0) {
        nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class, pa.getNextContactId());
        pa.setNextProjectContact(nextProjectContact);
      }
      out.println("  <tr>");
      if (pa.getNextActionDate() != null) {
        out.println("    <td class=\"inside\">" + editActionLink + sdf11.format(pa.getNextActionDate())
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
      if (pa.getNextActionDate() != null) {
        if (pa.getNextActionDate().after(today)) {
          out.println("    <td class=\"inside\"></td>");
        } else if (pa.getNextActionDate().after(yesterday)) {
          out.println("    <td class=\"inside-highlight\">Due Today</td>");
        } else {
          out.println("    <td class=\"inside-highlight\">Overdue</td>");
        }
      } else {
        out.println("    <td class=\"inside\">&nbsp;</td>");
      }
      out.println("    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
          + pa.getActionNextId() + "\"></td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }

  protected static void printTodoList(int projectId, WebUser webUser, Session dataSession,
      PrintWriter out, List<ProjectActionNext> projectActionList, ProjectActionNext completingProjectAction) {
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
      for (ProjectActionNext pa : projectActionList) {
        String workActionLink = "<a href=\"ProjectActionServlet?" + ProjectActionServlet.PARAM_COMPLETING_ACTION_NEXT_ID
            + "=" + pa.getActionNextId()
            + "\" class=\"button\">";
        String editActionLink = "<a href=\"ProjectServlet?" + PARAM_PROJECT_ID + "=" + projectId
            + "&" + PARAM_ACTION_NEXT_ID + "=" + pa.getActionNextId() + "\" class=\"button\">";
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
        if (pa.getNextActionDate() != null) {
          out.println("    <td class=\"inside\">" + editActionLink + sdf11.format(pa.getNextActionDate())
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
        if (pa.getNextActionDate() != null) {
          if (pa.getNextActionDate().after(today)) {
            out.println("    <td class=\"inside\"></td>");
          } else if (pa.getNextActionDate().after(yesterday)) {
            out.println("    <td class=\"inside-highlight\">Due Today</td>");
          } else {
            out.println("    <td class=\"inside-highlight\">Overdue</td>");
          }
        } else {
          out.println("    <td class=\"inside\">&nbsp;</td>");
        }
        String checked = "";
        if (completingProjectAction != null
            && completingProjectAction.getActionNextId() == pa.getActionNextId()) {
          checked = " checked";
        }
        out.println("    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
            + pa.getActionNextId() + "\"" + checked + "></td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    } else {
      out.println("<i>no items</i>");
    }
  }

  protected static void printActionDescription(WebUser webUser, PrintWriter out, SimpleDateFormat sdf11,
      ProjectActionNext pa, String editActionLink, Date today) {
    String additionalContent = "";
    if (pa.getLinkUrl() != null && pa.getLinkUrl().length() > 0) {
      additionalContent = " [<a href=\"" + pa.getLinkUrl() + "\" target=\"_blank\">link</a>]";
    }
    if (pa.getNextDeadlineDate() != null) {
      Date todayDateOnly = normalizeDate(webUser, today);
      Date deadlineDateOnly = pa.getNextDeadlineDate();
      if (deadlineDateOnly.after(todayDateOnly)) {
        additionalContent += "    <br/>Deadline: " + sdf11.format(pa.getNextDeadlineDate());
      } else {
        additionalContent += "    <br/>Deadline: <span class=\"fail\">"
            + sdf11.format(pa.getNextDeadlineDate()) + "</span>";
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

  protected static void printOutScript(PrintWriter out, String formName, WebUser webUser) {
    SimpleDateFormat sdf = webUser.getDateFormat();
    out.println(" <script>");
    out.println("    function clickForEmail" + formName + "(projectContactId) { ");
    out.println("      var form = document.forms['" + SAVE_PROJECT_ACTION_FORM + formName + "']; ");
    out.println("      var checkBox = form['" + PARAM_SEND_EMAIL_TO + "' + projectContactId];");
    out.println("      checkBox.checked = !checkBox.checked; ");
    out.println("    }");
    out.println("    function selectProjectActionType" + formName + "(actionType)");
    out.println("    {");
    out.println("      var form = document.forms['" + SAVE_PROJECT_ACTION_FORM + formName + "'];");
    out.println("      var label = makeIStatement" + formName
        + "(actionType, form." + PARAM_NEXT_CONTACT_ID + ".options[form." + PARAM_NEXT_CONTACT_ID
        + ".selectedIndex].text);");
    out.println("      form." + PARAM_START_SENTANCE + ".value = label;");
    out.println("      form." + PARAM_NEXT_ACTION_TYPE + ".value = actionType;");
    out.println("      enableForm" + formName + "(); ");
    out.println("    }");
    out.println("    ");
    out.println("    function enableForm" + formName + "()");
    out.println("    {");
    out.println("      var form = document.forms['" + SAVE_PROJECT_ACTION_FORM + formName + "'];");
    out.println("      form." + PARAM_NEXT_ACTION_DATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_DESCRIPTION + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_CONTACT_ID + ".disabled = false;");
    out.println("      form." + PARAM_START_SENTANCE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_TIME_ESTIMATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_TARGET_DATE + ".disabled = false;");
    out.println("      form." + PARAM_NEXT_DEADLINE_DATE + ".disabled = false;");
    out.println("      form." + PARAM_LINK_URL + ".disabled = false;");
    out.println("      form." + PARAM_TEMPLATE_TYPE + ".disabled = false;");
    out.println("      form." + PARAM_PROCESS_STAGE + ".disabled = false;");
    out.println("      if (form." + PARAM_NEXT_ACTION_DATE + ".value == \"\")");
    out.println("      {");
    out.println("       document.projectAction" + formName + "." + PARAM_NEXT_ACTION_DATE + ".value = '"
        + sdf.format(new Date()) + "';");
    ;
    out.println("      }");
    out.println("    }");
    out.println("    ");
    printGenerateSelectNextTimeEstimateFunction(out, formName);
    out.println("  </script>");
  }

  protected static void printGenerateSelectNextTimeEstimateFunction(PrintWriter out, String formName) {
    out.println("    function selectNextTimeEstimate" + formName + "(timeInMinutes)");
    out.println("    {");
    out.println("      var form = document.forms['" + SAVE_PROJECT_ACTION_FORM + formName + "'];");
    out.println("      form." + PARAM_NEXT_TIME_ESTIMATE + ".value = timeInMinutes;");
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

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "DQA Tester Home Page";
  }// </editor-fold>

  private static List<ProjectActionNext> getProjectActionsScheduledAndCompletedList(Session dataSession,
      int projectId) {
    Query query = dataSession.createQuery(
        "from ProjectActionNext pan "
            + "where pan.projectId = :projectId "
            + "and pan.nextDescription <> '' "
            + "and pan.nextActionStatusString = :nextActionStatus");
    query.setParameter("projectId", projectId);
    query.setParameter("nextActionStatus", ProjectNextActionStatus.COMPLETED.getId());
    @SuppressWarnings("unchecked")
    List<ProjectActionNext> projectActionList = query.list();
    return projectActionList;
  }
}
