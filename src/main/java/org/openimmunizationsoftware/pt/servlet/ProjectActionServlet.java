/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.ChatAgent;
import org.openimmunizationsoftware.pt.manager.MailManager;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectActionServlet extends ClientServlet {

  private static final String SAVE_PROJECT_ACTION_FORM = "saveProjectActionForm";
  private static final String FORM_ACTION_NOW = "ActionNow";
  private static final String FORM_ACTION_NEXT = "ActionNext";
  private static final String PARAM_COMPLETED = "completed";
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
  private static final String PARAM_ACTION_DESCRIPTION = "actionDescription";
  private static final String PARAM_NEXT_NOTES = "nextNotes";
  private static final String PARAM_NEXT_SUMMARY = "nextSummary";
  private static final String PARAM_NEXT_TIME_ESTIMATE = "nextTimeEstimate";
  private static final String PARAM_PROJECT_ID = "projectId";
  protected static final String PARAM_COMPLETING_ACTION_ID = "completingActionId";
  private static final String PARAM_ACTION_ID = "actionId";
  private static final String PARAM_ACTION = "action";
  private static final String ACTION_START_TIMER = "StartTimer";
  private static final String ACTION_STOP_TIMER = "StopTimer";
  private static final String ACTION_NOTE = "Note";
  private static final String ACTION_PROPOSE = "Propose";
  private static final String ACTION_FEEDBACK = "Feedback";
  private static final String ACTION_COMPLETED = "Completed";
  private static final String ACTION_COMPLETED_AND_SUGGEST = "Completed and Suggest";
  private static final String ACTION_CREATE = "Create";
  private static final String ACTION_UPDATE = "Update";

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

      String completingActionIdString = request.getParameter(PARAM_COMPLETING_ACTION_ID);
      if (completingActionIdString != null) {
        ProjectAction completingProjectAction = (ProjectAction) dataSession.get(ProjectAction.class,
            Integer.parseInt(completingActionIdString));
        setupProjectActionAndSaveToAppReq(appReq, dataSession, completingProjectAction);
      }

      String actionIdString = request.getParameter(PARAM_ACTION_ID);
      ProjectAction projectAction = null;
      if (actionIdString != null) {
        projectAction = (ProjectAction) dataSession.get(ProjectAction.class,
            Integer.parseInt(actionIdString));
        setupProjectAction(appReq, dataSession, projectAction);
      }

      Project project = appReq.getProject();
      ProjectAction completingAction = appReq.getProjectAction();
      if (completingAction != null) {
        project = completingAction.getProject();
      }
      appReq.setProject(project);
      appReq.setProjectSelected(project);
      appReq.setProjectAction(completingAction);
      appReq.setProjectActionSelected(completingAction);

      String nextSummary = request.getParameter(PARAM_NEXT_SUMMARY);
      String nextNotes = request.getParameter(PARAM_NEXT_NOTES);

      String systemInsructions = "You are a helpful assistant tasked with helping a professional report about progress that is being made on a project.";
      ChatAgent chatAgent = null;

      String proposePrompt = "";
      String feedbackPrompt = "";
      String nextPrompt = "";
      String nextFeedback = null;
      String nextSuggest = null;

      boolean isCompleted = false;

      if (completingAction != null) {
        boolean isChanged = false;
        if (nextSummary != null) {
          completingAction.setNextSummary(nextSummary);
          isChanged = true;
        }
        if (nextNotes != null && nextNotes.length() > 0) {
          if (completingAction.getNextNotes() != null && completingAction.getNextNotes().trim().length() > 0) {
            nextNotes = completingAction.getNextNotes() + "\n - " + nextNotes;
          } else {
            nextNotes = " - " + nextNotes;
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
          Query query = dataSession.createQuery("select sum(billMins) from BillEntry where action = :action");
          query.setParameter("action", completingAction);
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
          ;
          dataSession.update(completingAction);
          transaction.commit();
        }
        if (nextNotes != null || nextSummary != null) {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

          String basePrompt;

          basePrompt = "I have taken action on a recent project and need to provide an update to be included in reporting to my supervisor and other project participants.  \n\n"
              + "Project name: " + project.getProjectName() + " \n"
              + "Project description: " + project.getDescription() + " \n"
              + "Recent actions taken: \n";
          List<ProjectAction> projectActionList = ProjectServlet.getProjectActionsTakenList(dataSession, project);
          int limit = 12;
          for (ProjectAction pa : projectActionList) {
            basePrompt += " - " + sdf.format(pa.getActionDate()) + " " + pa.getActionDescription() + " \n";
            limit--;
            if (limit == 0) {
              break;
            }
          }
          basePrompt += "Working on this next action: "
              + completingAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + " \n";
          if (completingAction.getNextNotes() != null && completingAction.getNextNotes().length() > 0) {
            basePrompt += "Next action notes: \n" + completingAction.getNextNotes() + " \n";
          }

          if (completingAction.getNextSummary() != null && completingAction.getNextSummary().length() > 0) {
            basePrompt += "The current summary is: \n" + completingAction.getNextSummary() + " \n";
          }
          proposePrompt = basePrompt;
          proposePrompt += "The recent actions taken are previously generated summaries of actions taken for this project. "
              + "Please create a succinct summary of what action was taken based on the next action I am working on and the next action notes I took (if any) "
              + "while completing this action. "
              + "Keep in mind that this summary will be added to the list of recent actions taken, so should be in the same format as the other summaries. "
              + "Be careful to only document what has occured and do not mention what is planned to happen next. \n";
          proposePrompt += "I will report this and the list of completed actions to my supervisor and other contacts as this action having been completed on today's date "
              + sdf.format(new Date())
              + ". Please give me only the text of the update, as it would appear after the date and no other commentary. Thanks!";

          feedbackPrompt = basePrompt;
          feedbackPrompt += "Please review my current summary and give me three to five questions my superisor or others might have that could help me clarify and add more detail to this update. "
              + " Please give this to me as a list of items for me to consider. Give this to me as list of unordered items in an HTML list. "
              + " Send me a JSON response where the key 'html' contains the HTML code for a list of suggestions.. Thanks! \n";

          nextPrompt = basePrompt;
          nextPrompt += "Please review my current summary of what I accomplished and what I have accomplished in the past with this project and suggest a list "
              + "of two or three next steps I should indicate in my project tracker  "
              + " Please give this to me as a list of items for me to consider. Give this to me as list of unordered items in an HTML list. "
              + " Send me a JSON response where the key 'html' contains the HTML code for a list of suggestions.. Thanks! \n";
        }
      }

      String message = null;
      String emailBody = null;

      if (action != null) {

        if (action.equals(ACTION_START_TIMER)) {
          TimeTracker timeTracker = appReq.getTimeTracker();
          if (timeTracker != null && completingAction != null) {
            timeTracker.startClock(completingAction.getProject(), completingAction, dataSession);
          }
        } else if (action.equals(ACTION_STOP_TIMER)) {
          TimeTracker timeTracker = appReq.getTimeTracker();
          if (timeTracker != null) {
            timeTracker.stopClock(dataSession);
          }
        } else if (action.equals(ACTION_PROPOSE)) {
          chatAgent = new ChatAgent(systemInsructions);
          chatAgent.chat(proposePrompt);

          if (chatAgent.hasResponse()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String likelyDatePrefix1 = sdf.format(new Date());
            String likelyDatePrefix2 = "- " + likelyDatePrefix1;
            String chatGPTResponse = chatAgent.getResponseText();
            if (chatGPTResponse.startsWith(likelyDatePrefix1)) {
              nextSummary = chatGPTResponse.substring(likelyDatePrefix1.length()).trim();
            } else if (chatGPTResponse.startsWith(likelyDatePrefix2)) {
              nextSummary = chatGPTResponse.substring(likelyDatePrefix2.length()).trim();
            } else {
              nextSummary = chatGPTResponse;
            }
            completingAction.setNextSummary(nextSummary);
          }
          action = ACTION_UPDATE;
        } else if (action.equals(ACTION_FEEDBACK)) {
          chatAgent = new ChatAgent(systemInsructions);
          chatAgent.setResponseFormat(ChatAgent.RESPONSE_FORMAT_JSON);
          chatAgent.chat(feedbackPrompt);
          if (chatAgent.hasResponse()) {
            String json = chatAgent.getResponseText();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);
            nextFeedback = jsonNode.get("html").asText();
            // set on Project Action and save
            if (nextFeedback != null) {
              completingAction.setNextFeedback(nextFeedback);
              Transaction transaction = dataSession.beginTransaction();
              dataSession.update(completingAction);
              transaction.commit();
            }
          }
          action = ACTION_UPDATE;
        } else if (action.equals(ACTION_COMPLETED)) {
          isCompleted = true;
          action = ACTION_UPDATE;
        } else if (action.equals(ACTION_COMPLETED_AND_SUGGEST)) {
          isCompleted = true;
          chatAgent = new ChatAgent(systemInsructions);
          chatAgent.setResponseFormat(ChatAgent.RESPONSE_FORMAT_JSON);
          chatAgent.chat(nextPrompt);
          if (chatAgent.hasResponse()) {
            String json = chatAgent.getResponseText();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);
            nextSuggest = jsonNode.get("html").asText();
          }
          action = ACTION_UPDATE;
        } else if (action.equals(ACTION_CREATE)) {
          projectAction.setContactId(webUser.getContactId());
          projectAction.setContact(webUser.getProjectContact());
          int nextTimeEstimate = 0;
          if (request.getParameter(PARAM_NEXT_TIME_ESTIMATE) != null) {
            try {
              nextTimeEstimate = Integer.parseInt(request.getParameter(PARAM_NEXT_TIME_ESTIMATE));
            } catch (NumberFormatException nfe) {
              nextTimeEstimate = 0;
            }
          }
          projectAction.setActionDate(new Date());
          projectAction.setActionDescription("");
          projectAction.setNextDescription(trim(request.getParameter(PARAM_NEXT_DESCRIPTION), 1200));
          if (nextTimeEstimate > 0) {
            projectAction.setNextTimeEstimate(nextTimeEstimate);
          }
          projectAction.setNextActionId(0);

          projectAction.setNextDue(ProjectServlet.parseDate(appReq, request.getParameter(PARAM_NEXT_DUE)));
          projectAction.setNextDeadline(ProjectServlet.parseDate(appReq, request.getParameter(PARAM_NEXT_DEADLINE)));
          String linkUrl = request.getParameter(PARAM_LINK_URL);
          if (linkUrl == null || linkUrl.equals("")) {
            projectAction.setLinkUrl(null);
          } else {
            projectAction.setLinkUrl(linkUrl);
          }
          String templateTypeString = request.getParameter(PARAM_TEMPLATE_TYPE);
          if (templateTypeString == null || templateTypeString.equals("")) {
            projectAction.setTemplateType(null);
          } else {
            projectAction.setTemplateType(TemplateType.getTemplateType(templateTypeString));
          }
          String prioritySpecialString = request.getParameter(PARAM_PRIORITY_SPECIAL);
          if (prioritySpecialString == null || prioritySpecialString.equals("")) {
            projectAction.setPrioritySpecial(null);
          } else {
            projectAction.setPrioritySpecial(PrioritySpecial.getPrioritySpecial(prioritySpecialString));
          }

          String nextActionType = request.getParameter(PARAM_NEXT_ACTION_TYPE);
          projectAction.setNextActionType(nextActionType);
          int priorityLevel = project.getPriorityLevel();
          projectAction.setPriorityLevel(priorityLevel);
          String nextContactIdString = request.getParameter(PARAM_NEXT_CONTACT_ID);
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
          {
            String[] completed = request.getParameterValues(PARAM_COMPLETED);

            Transaction trans = dataSession.beginTransaction();
            try {
              nextFeedback = null;
              dataSession.saveOrUpdate(projectAction);
              if (completed != null && completed.length > 0) {
                for (String c : completed) {
                  ProjectAction paCompleted = (ProjectAction) dataSession.get(ProjectAction.class, Integer.parseInt(c));
                  paCompleted.setNextActionId(completingAction.getActionId());
                  paCompleted.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
                  dataSession.update(paCompleted);
                  if (nextFeedback == null && paCompleted.getNextFeedback() != null) {
                    nextFeedback = paCompleted.getNextFeedback();
                  }
                }
              }
              if (nextFeedback != null) {
                completingAction.setNextFeedback(nextFeedback);
                dataSession.update(completingAction);
              }
            } finally {
              trans.commit();
            }

          }

        }
        if (action.equals(ACTION_UPDATE)) {
          completingAction.setContactId(webUser.getContactId());
          completingAction.setContact(webUser.getProjectContact());
          int nextTimeEstimate = 0;
          if (request.getParameter(PARAM_NEXT_TIME_ESTIMATE) != null) {
            try {
              nextTimeEstimate = Integer.parseInt(request.getParameter(PARAM_NEXT_TIME_ESTIMATE));
            } catch (NumberFormatException nfe) {
              nextTimeEstimate = 0;
            }
          }
          completingAction.setActionDate(new Date());
          completingAction.setActionDescription("");
          completingAction.setNextDescription(trim(request.getParameter(PARAM_NEXT_DESCRIPTION), 1200));
          if (nextTimeEstimate > 0) {
            completingAction.setNextTimeEstimate(nextTimeEstimate);
          }
          completingAction.setNextActionId(0);

          completingAction.setNextDue(ProjectServlet.parseDate(appReq, request.getParameter(PARAM_NEXT_DUE)));
          completingAction.setNextDeadline(ProjectServlet.parseDate(appReq, request.getParameter(PARAM_NEXT_DEADLINE)));
          String linkUrl = request.getParameter(PARAM_LINK_URL);
          if (linkUrl == null || linkUrl.equals("")) {
            completingAction.setLinkUrl(null);
          } else {
            completingAction.setLinkUrl(linkUrl);
          }
          String templateTypeString = request.getParameter(PARAM_TEMPLATE_TYPE);
          if (templateTypeString == null || templateTypeString.equals("")) {
            completingAction.setTemplateType(null);
          } else {
            completingAction.setTemplateType(TemplateType.getTemplateType(templateTypeString));
          }
          String prioritySpecialString = request.getParameter(PARAM_PRIORITY_SPECIAL);
          if (prioritySpecialString == null || prioritySpecialString.equals("")) {
            completingAction.setPrioritySpecial(null);
          } else {
            completingAction.setPrioritySpecial(PrioritySpecial.getPrioritySpecial(prioritySpecialString));
          }

          String nextActionType = request.getParameter(PARAM_NEXT_ACTION_TYPE);
          completingAction.setNextActionType(nextActionType);
          int priorityLevel = project.getPriorityLevel();
          completingAction.setPriorityLevel(priorityLevel);
          String nextContactIdString = request.getParameter(PARAM_NEXT_CONTACT_ID);
          if (nextContactIdString != null && nextContactIdString.length() > 0) {
            completingAction.setNextContactId(Integer.parseInt(nextContactIdString));
            completingAction.setNextProjectContact(
                (ProjectContact) dataSession.get(ProjectContact.class, completingAction.getNextContactId()));
          }
          completingAction.setProvider(webUser.getProvider());
          if (completingAction.hasNextDescription()) {
            if (completingAction.hasNextDue()) {
              completingAction.setNextActionStatus(ProjectNextActionStatus.READY);
            } else {
              completingAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
            }
          }
          if (message == null) {
            String[] completed = request.getParameterValues(PARAM_COMPLETED);

            Transaction trans = dataSession.beginTransaction();
            try {
              nextFeedback = null;
              dataSession.saveOrUpdate(completingAction);
              if (completed != null && completed.length > 0) {
                for (String c : completed) {
                  ProjectAction paCompleted = (ProjectAction) dataSession.get(ProjectAction.class, Integer.parseInt(c));
                  paCompleted.setNextActionId(completingAction.getActionId());
                  if (completingAction.hasActionDescription()) {
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
                completingAction.setNextFeedback(nextFeedback);
                dataSession.update(completingAction);
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
                message = "Unable to send email: " + e.getMessage();
                emailBody = null;
              }
            }

          } else {
            appReq.setMessageProblem(message);
          }

          completingAction = null;
          appReq.setProject(null);
          appReq.setProjectAction(null);
        }
      }

      List<ProjectAction> projectActionList = getProjectActionListForToday(webUser, dataSession);
      if (completingAction == null && projectActionList.size() > 0) {
        completingAction = projectActionList.get(0);
        setupProjectActionAndSaveToAppReq(appReq, dataSession, completingAction);
        project = completingAction.getProject();
      }
      List<ProjectAction> projectActionClosedTodayList = getProjectActionListClosedToday(webUser, dataSession);

      if (prepareProjectActionListAndIdentifyOverdue(dataSession, projectActionList, webUser).size() > 0) {
        // TOOD print a nicer message and a link to clean these up
        message = "There are actions overdue that are not shown here, only showing what is scheduled for today.";
      }
      appReq.setMessageProblem(message);
      appReq.setTitle("Actions");
      printHtmlHead(appReq);
      ProjectServlet.printOutEmailSent(out, emailBody);

      Date nextDue = new Date();
      SimpleDateFormat sdf1 = webUser.getDateFormat();

      Calendar cIndicated = webUser.getCalendar();
      cIndicated.setTime(nextDue);

      int projectId = 0;
      if (completingAction != null) {
        completingAction.getProject().getProjectId();
      } else if (project != null) {
        projectId = project.getProjectId();
      }
      String disabled = (completingAction == null ? " disabled" : "");
      if (true) {
      }

      if (completingAction == null) {
        out.println("<h2>Actions for Today</h2>");
        out.println("<p>You have no more actions to take today. Have a great evening! </p>");
      } else {

        List<ProjectContactAssigned> projectContactAssignedList = ProjectServlet
            .getProjectContactAssignedList(dataSession, project.getProjectId());
        List<ProjectContact> projectContactList = ProjectServlet.getProjectContactList(dataSession, project,
            projectContactAssignedList);

        out.println("<div id=\"three-column-container\">");
        // ---------------------------------------------------------------------------------------------------
        // ACTION NOW
        // ---------------------------------------------------------------------------------------------------
        out.println("<div id=\"actionNow\">");
        printFormStart(webUser, out, project, completingAction, FORM_ACTION_NOW);
        printActionNow(appReq, webUser, out, completingAction, projectContactList, FORM_ACTION_NOW);
        out.println("</form>");
        out.println("</div>");

        // ---------------------------------------------------------------------------------------------------
        // ACTION NEXT
        // ---------------------------------------------------------------------------------------------------
        out.println("<div id=\"actionNext\">");
        printProjectTable(out, project, projectId);
        printFormStart(webUser, out, project, null, "NextAction");
        {

          {

            Query query;

            query = dataSession.createQuery(
                "from ProjectAction where projectId = ? and nextDescription <> '' and nextActionId = 0 order by nextDue asc");
            query.setParameter(0, projectId);
            List<ProjectAction> projectActionTodoList = query.list();
            List<ProjectAction> projectActionTemplateList = new ArrayList<ProjectAction>();
            List<ProjectAction> projectActionGoalList = new ArrayList<ProjectAction>();
            {
              Date today = new Date();
              for (Iterator<ProjectAction> it = projectActionTodoList.iterator(); it.hasNext();) {
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

            out.println("  <table class=\"boxed\">");
            out.println("    <tr>");
            out.println("      <th class=\"title\">To Do List</th>");
            out.println("    </tr>");
            out.println("    <tr>");
            out.println("      <td class=\"outside\">");
            printTodoList(projectId, webUser, dataSession, out, projectActionTodoList);

            out.println("      </td>");
            out.println("    </tr>");
            out.println("  <tr>");
            out.println("    <th class=\"title\">What is next?</th>");
            out.println("  </tr>");
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
            printEditNextAction(request, webUser, out, null, FORM_ACTION_NEXT, disabled,
                projectContactList);
            out.println("  <tr>");
            out.println("    <th class=\"title\">Save Action</th>");
            out.println("  </tr>");
            out.println("  <tr>");
            out.println("    <td class=\"boxed-submit\">");
            out.println("      <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_CREATE + "\">");
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
        }

        out.println("</form>");
        out.println("</div>");

        // ---------------------------------------------------------------------------------------------------
        // ACTION LATER
        // ---------------------------------------------------------------------------------------------------
        out.println("<div id=\"actionLater\">");
        {
          printTimeManagementBox(appReq, webUser, out, projectActionList, cIndicated);
          printActionsForToday(webUser, out, projectActionList, nextDue, sdf1, cIndicated);
        }
        out.println("</div>");

        out.println("</div>");
      }

      if (!isCompleted && completingAction != null && completingAction.getNextFeedback() != null) {
        out.println("<h3>Feedback</h3>");
        out.println("" + completingAction.getNextFeedback() + "");
      }

      if (isCompleted && nextSuggest != null) {
        out.println("<h3>Next Step Suggestions</h3>");
        out.println("" + completingAction.getNextFeedback() + "");
      }

      if (chatAgent != null) {
        out.println("<h2>Chat with GPT-4o</h2>");
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
      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private void printActionsForToday(WebUser webUser, PrintWriter out, List<ProjectAction> projectActionList,
      Date nextDue,
      SimpleDateFormat sdf1, Calendar cIndicated) {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"3\">All actions scheduled for today</th>");
    out.println("  </tr>");
    printDueTable(webUser, out, sdf1, ProjectNextActionType.OVERDUE_TO, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.COMMITTED_TO, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_CONTACT, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_MEET, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.MIGHT, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.GOAL, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_FOLLOW_UP, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WAITING, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_REVIEW, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_DOCUMENT, nextDue, projectActionList, cIndicated);
    out.println("</table><br/>");
  }

  private void printTimeManagementBox(AppReq appReq, WebUser webUser, PrintWriter out,
      List<ProjectAction> projectActionList, Calendar cIndicated) {
    int timeSpentSoFar = 0;
    {
      TimeTracker timeTracker = appReq.getTimeTracker();
      if (timeTracker != null) {
        timeSpentSoFar = timeTracker.getTotalMinsBillable();
      }
    }
    int nextTimeEstimateTotal = 0;
    int nextTimeEstimateCommit = 0;
    int nextTimeEstimateWill = 0;
    int nextTimeEstimateWillMeet = 0;
    int nextTimeEstimateMight = 0;
    for (ProjectAction pa : projectActionList) {
      if (!sameDay(cIndicated, pa.getNextDue(), webUser)) {
        continue;
      }
      if (pa.getNextTimeEstimate() != null) {
        int nextTimeEstimate = pa.getNextTimeEstimate();
        int nextTimeActual = pa.getNextTimeActual() == null ? 0 : pa.getNextTimeActual();
        if (nextTimeActual <= nextTimeEstimate) {
          nextTimeEstimate = nextTimeActual - nextTimeEstimate;
        } else {
          nextTimeEstimate = nextTimeActual;
        }

        nextTimeEstimateTotal += nextTimeEstimate;
        if (ProjectNextActionType.COMMITTED_TO.equals(pa.getNextActionType())
            || ProjectNextActionType.OVERDUE_TO.equals(pa.getNextActionType())) {
          nextTimeEstimateCommit += pa.getNextTimeEstimate();
        } else if (ProjectNextActionType.WILL.equals(pa.getNextActionType())
            || ProjectNextActionType.WILL_CONTACT.equals(pa.getNextActionType())) {
          nextTimeEstimateWill += pa.getNextTimeEstimate();
        } else if (ProjectNextActionType.WILL_MEET.equals(pa.getNextActionType())) {
          nextTimeEstimateWillMeet += pa.getNextTimeEstimate();
        } else if (ProjectNextActionType.MIGHT.equals(pa.getNextActionType())) {
          nextTimeEstimateMight += pa.getNextTimeEstimate();
        }

      }
    }

    printTimeEstimateBox(out, timeSpentSoFar, nextTimeEstimateTotal, nextTimeEstimateCommit, nextTimeEstimateWill,
        nextTimeEstimateWillMeet, nextTimeEstimateMight);
  }

  private void printSendEmailSelection(PrintWriter out, String formName, List<ProjectContact> projectContactList) {
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
      out.println("          <span class=\"together\"><input type=\"checkbox\" name=\"" + PARAM_SEND_EMAIL_TO
          + projectContact1.getContactId() + "\" value=\"Y\"/>");
      out.println("<font size=\"-1\"><a href=\"javascript: void clickForEmail" + formName + "('"
          + projectContact1.getContactId() + "');\" class=\"button\">" + projectContact1.getName()
          + "</a></font></span>");
    }
    out.println("          </td>");
    out.println("        </tr>");
    out.println("      </table>");
    out.println("    </td>");
    out.println("  </tr>");
  }

  private void printProjectTable(PrintWriter out, Project project, int projectId) {
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
    if (project.getDescription() != null && !project.getDescription().equals("")) {
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Description</th>");
      out.println("    <td class=\"boxed\">" + addBreaks(project.getDescription()) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");
    out.println("<br/>");
  }

  private void printEditNextAction(HttpServletRequest request, WebUser webUser, PrintWriter out,
      ProjectAction completingAction, String formName, String disabled, List<ProjectContact> projectContactList) {
    SimpleDateFormat sdf1;
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"inside\">");
    SimpleDateFormat sdf2 = webUser.getDateFormat("MM/dd/yyyy hh:mm aaa");
    {
      sdf1 = webUser.getDateFormat();
      out.println("        <tr>");
      out.println("          <th class=\"inside\">When</th>");
      {
        String nextDueString = completingAction == null || completingAction.getNextDue() == null
            ? request.getParameter(PARAM_NEXT_DUE)
            : sdf2.format(completingAction.getNextDue());
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
      String nextActionType = completingAction == null ? ProjectNextActionType.WILL
          : completingAction.getNextActionType();
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
            completingAction == null ? projectContact1.getContactId() : completingAction.getContactId()));
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
            + disabled + ">" + (completingAction == null ? "" : completingAction.getNextDescription())
            + "</textarea>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("        <tr>");
    out.println("          <th class=\"inside\">Time</th>");
    out.println("          <td class=\"inside\" colspan=\"3\">");
    out.println("            <input type=\"text\" name=\"" + PARAM_NEXT_TIME_ESTIMATE + "\" size=\"3\" value=\""
        + (completingAction == null ? "" : completingAction.getNextTimeEstimateMinsForDisplay())
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
              + n(completingAction == null || completingAction.getNextDeadline() == null
                  ? request.getParameter(PARAM_NEXT_DEADLINE)
                  : sdf1.format(completingAction.getNextDeadline()))
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
              + n(completingAction == null || completingAction.getLinkUrl() == null
                  ? request.getParameter(PARAM_LINK_URL)
                  : completingAction.getLinkUrl())
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
          + n((completingAction == null || completingAction.getTemplateType() == null)
              ? request.getParameter(PARAM_TEMPLATE_TYPE)
              : completingAction.getTemplateType().getId())
          + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      // default empty option for no template
      out.println("             <option value=\"\">none</option>");
      for (TemplateType templateType : TemplateType.values()) {
        out.println("             <option value=\"" + templateType.getId() + "\""
            + (completingAction != null && completingAction.getTemplateType() == templateType ? " selected" : "")
            + ">" + templateType.getLabel() + "</option>");
      }
      out.println("            </select>");
      // now do Priority Special
      out.println("            Priority: ");
      out.println("           <select name=\"prioritySpecial\" value=\""
          + n((completingAction == null || completingAction.getPrioritySpecial() == null)
              ? request.getParameter(PARAM_PRIORITY_SPECIAL)
              : completingAction.getPrioritySpecial().getId())
          + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
      // default empty option for no template
      out.println("             <option value=\"\">none</option>");
      for (PrioritySpecial prioritySpecial : PrioritySpecial.values()) {
        out.println("             <option value=\"" + prioritySpecial.getId() + "\""
            + (completingAction != null && completingAction.getPrioritySpecial() == prioritySpecial ? " selected"
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

  private void printFormStart(WebUser webUser, PrintWriter out, Project project, ProjectAction completingAction,
      String formName) {
    out.println("<form name=\"projectAction" + formName
        + "\" method=\"post\" action=\"ProjectActionServlet\" id=\"" + SAVE_PROJECT_ACTION_FORM + formName
        + "\">");
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
    out.println("      form." + PARAM_START_SENTANCE + ".value = label;");
    out.println("      form.nextActionType.value = actionType;");
    out.println("      enableForm" + formName + "(); ");
    out.println("    }");
    out.println("    ");
    out.println("    function enableForm" + formName + "()");
    out.println("    {");
    out.println("      var form = document.forms['saveProjectActionForm" + formName + "'];");
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
    out.println("    ");
    ProjectServlet.generateSelectNextTimeEstimateFunction(out, formName);

    out.println("    ");
    out.println("  </script>");
    out.println("<input type=\"hidden\" name=\"" + PARAM_PROJECT_ID + "\" value=\"" + project.getProjectId() + "\">");
    if (completingAction != null) {
      out.println("<input type=\"hidden\" name=\"" + PARAM_ACTION_ID + "\" value=\""
          + completingAction.getActionId() + "\">");
    }
  }

  private void printActionNow(AppReq appReq, WebUser webUser, PrintWriter out, ProjectAction completingAction,
      List<ProjectContact> projectContactList,
      String formName) {
    SimpleDateFormat sdf11 = webUser.getDateFormat();
    out.println("<table class=\"boxed-full\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\">" + getNextActionTitle(completingAction) + " "
        + getTimeString(appReq, completingAction) + "</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"boxed-fill\">");
    out.println("        <tr>");
    out.println("          <th width=\"15%\">Action</th>");
    printActionDescription(webUser, out, sdf11, completingAction, null, new Date());
    out.println("        </tr>");

    out.println("        <tr>");
    out.println("          <th>Notes</th>");
    out.println("          <td class=\"inside\">");
    if (completingAction.getNextNotes() != null) {
      out.println(completingAction.getNextNotes().replace("\n", "<br/>"));
      out.println("<br/>");
    }
    out.println(
        "            <textarea name=\"nextNotes\" id=\"nextNotes\" rows=\"7\" onkeydown=\"resetRefresh()\"></textarea>");
    out.println("            <br/><span class=\"right\">");
    out.println("              <input type=\"hidden\" name=\"" + PARAM_ACTION_ID + "\" value=\""
        + completingAction.getActionId() + "\"/>");
    out.println("              <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_NOTE + "\"/>");
    out.println(
        "              <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_PROPOSE + "\"/>");
    out.println(
        "              <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_FEEDBACK + "\"/>");
    out.println("            </span>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("      </table>");
    out.println("    </td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <th class=\"title\">Summary</th>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td class=\"outside\">");
    out.println("      <table class=\"boxed-fill\">");
    out.println("        <tr>");
    out.println("          <th width=\"15%\">Action Taken</th>");
    out.println("          <td class=\"inside\">");
    out.println("            <textarea name=\"nextSummary\" rows=\"12\" onkeydown=\"resetRefresh()\">"
        + n(completingAction.getNextSummary()) + "</textarea>");
    out.println("          </td>");
    out.println("        </tr>");
    out.println("      </table>");
    out.println("    </td>");
    out.println("  </tr>");
    printSendEmailSelection(out, formName, projectContactList);
    out.println("  <tr>");
    out.println("    <td class=\"boxed-submit\">");
    out.println(
        "       <input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_ID + "\" value=\""
            + completingAction.getActionId() + "\"/>");
    out.println("     <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_COMPLETED + "\"/>");
    out.println("     <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_COMPLETED_AND_SUGGEST
        + "\"/>");
    out.println("    </td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("<br/>");

    out.println("<script>");
    out.println("document.getElementById(\"nextNotes\").addEventListener(\"keydown\", function(event) {");
    out.println("  if (event.key === \"Enter\" && !event.shiftKey) { // Shift + Enter for new line");
    out.println("    event.preventDefault(); // Prevent the default Enter behavior in textarea");
    out.println("    // Set the desired action value\n");
    out.println("      const form = document.getElementById(\"" + SAVE_PROJECT_ACTION_FORM + FORM_ACTION_NOW + "\");");
    out.println("    const actionInput = document.createElement(\"input\");");
    out.println("    actionInput.type = \"hidden\";");
    out.println("    actionInput.name = \"action\";");
    out.println("    actionInput.value = \"Note\";");
    out.println("    // Add this temporary hidden input to the form and submit");
    out.println("    form.appendChild(actionInput);");
    out.println("    form.submit();");
    out.println("    // Remove the temporary input to prevent it from persisting");
    out.println("    form.removeChild(actionInput);");
    out.println("  }");
    out.println("});");
    out.println("</script>");

    printCurrentActionEdit(appReq, webUser, out, completingAction, projectContactList, formName);
  }

  private String getTimeString(AppReq appReq, ProjectAction completingAction) {
    String timeString = "<span class=\"right\">" + TimeTracker.formatTime(completingAction.getNextTimeEstimate());
    {
      TimeTracker timeTracker = appReq.getTimeTracker();
      if (timeTracker != null) {
        String t = TimeTracker.formatTime(completingAction.getNextTimeActual());
        if (timeTracker.isRunningClock()) {
          timeString += "<a href=\"ProjectActionServlet?" + PARAM_ACTION + "=" + ACTION_STOP_TIMER
              + "\" class=\"timerRunning\">"
              + t + "</a>";
        } else {
          timeString += "<a href=\"ProjectActionServlet?" + PARAM_COMPLETING_ACTION_ID + "="
              + completingAction.getActionId()
              + "&" + PARAM_ACTION + "=" + ACTION_START_TIMER + "\" class=\"timerStopped\">" + t + "</a>";
        }
      }
    }
    timeString += "</span>";
    return timeString;
  }

  private String getNextActionTitle(ProjectAction completingAction) {
    String title = "Action Now";
    switch (completingAction.getNextActionStatus()) {
      case PROPOSED:
        title = "Proposed";
        break;
      case READY:
        title = "Ready to Work On";
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

  private void printCurrentActionEdit(AppReq appReq, WebUser webUser, PrintWriter out, ProjectAction completingAction,
      List<ProjectContact> projectContactList,
      String formName) {

    String disabled = "";
    out.println("<table class=\"boxed-full\">");
    out.println("  <tr>");
    out.println("    <th class=\"title\">Edit Action</th>");
    out.println("  </tr>");
    printEditNextAction(appReq.getRequest(), webUser, out, completingAction, formName, disabled,
        projectContactList);
    out.println("  <tr>");
    out.println("    <td class=\"boxed-submit\">");
    out.println("      <input type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_UPDATE + "\">");
    out.println("    </td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("<br/>");
  }

  private void setupProjectAction(AppReq appReq, Session dataSession,
      ProjectAction projectAction) {
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
    appReq.setProjectAction(completingProjectAction);
    appReq.setProject(completingProjectAction.getProject());
  }

  private static List<ProjectAction> getProjectActionListClosedToday(WebUser webUser, Session dataSession) {
    Date today = TimeTracker.createToday(webUser).getTime();
    Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
    Query query = dataSession.createQuery(
        "from ProjectAction where provider = :provider and (contactId = :contactId or nextContactId = :nextContactId) "
            + "and nextActionId <> 0 and nextDescription <> '' "
            + "and nextDue >= :today and nextDue < :tomorrow "
            + "order by nextDue, priority_level DESC, nextTimeEstimate, actionDate");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter(PARAM_NEXT_CONTACT_ID, webUser.getContactId());
    query.setParameter("today", today);
    query.setParameter("tomorrow", tomorrow);
    List<ProjectAction> projectActionList = query.list();
    return projectActionList;
  }

  private static List<ProjectAction> getProjectActionListForToday(WebUser webUser, Session dataSession) {
    Date today = TimeTracker.createToday(webUser).getTime();
    Date tomorrow = TimeTracker.createTomorrow(webUser).getTime();
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

  private static void printTimeEstimateBox(PrintWriter out, int timeSpentSoFar, int nextTimeEstimateTotal,
      int nextTimeEstimateCommit, int nextTimeEstimateWill, int nextTimeEstimateWillMeet,
      int nextTimeEstimateMight) {
    out.println("<table class=\"boxed\">");
    int runningTotal = 0;
    runningTotal = printTimeTotal(out, runningTotal, "Spent So Far", timeSpentSoFar);
    if (nextTimeEstimateTotal > 0) {
      runningTotal = printTimeTotal(out, runningTotal, "Will Meet", nextTimeEstimateWillMeet);
      runningTotal = printTimeTotal(out, runningTotal, "Committed", nextTimeEstimateCommit);
      runningTotal = printTimeTotal(out, runningTotal, "Will", nextTimeEstimateWill);
      runningTotal = printTimeTotal(out, runningTotal, "Might", nextTimeEstimateMight);
      runningTotal = printTimeTotal(out, runningTotal, "Other", nextTimeEstimateTotal - runningTotal);
    }
    out.println("</table><br/>");
  }

  private static int printTimeTotal(PrintWriter out, int runningTotal, String title, int time) {
    if (time > 0) {
      runningTotal += time;
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">" + title + "</th>");
      out.println("    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(time) + "</th>");
      out.println(
          "    <td class=\"boxed\">" + ProjectAction.getTimeForDisplay(runningTotal) + "</th>");
      out.println("  </tr>");
    }
    return runningTotal;
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
        if (projectAction.getNextDue() == null || projectAction.getNextDue().before(today)) {
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

  private static void printDueTable(WebUser webUser, PrintWriter out, SimpleDateFormat sdf1,
      String nextActionType, Date nextDue, List<ProjectAction> projectActionList,
      Calendar cIndicated) {

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
      out.println("    <th class=\"boxed\">" + ProjectNextActionType.getLabel(nextActionType) + "</th>");
      out.println("    <th class=\"boxed\">Est</th>");
      out.println("    <th class=\"boxed\">Act</th>");
      out.println("  </tr>");
    }

    for (ProjectAction projectAction : paList) {
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\"><a href=\"ProjectActionServlet?" + PARAM_COMPLETING_ACTION_ID + "="
          + projectAction.getActionId() + "\" class=\"button\">"
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

  private static boolean sameDay(Calendar c1, Date d, WebUser webUser) {
    if (d == null) {
      return false;
    }
    Calendar c2 = webUser.getCalendar();
    c2.setTime(d);
    boolean s = sameDay(c1, c2);
    return s;
  }

  private static boolean sameDay(Calendar c1, Calendar c2) {
    return c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
        && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
        && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
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

  private static void printActionDescription(WebUser webUser, PrintWriter out, SimpleDateFormat sdf11,
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

  private static void printTodoList(int projectId, WebUser webUser, Session dataSession,
      PrintWriter out, List<ProjectAction> projectActionList) {
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
        out.println("    <td class=\"inside\"><input type=\"checkbox\" name=\"completed\" value=\""
            + pa.getActionId() + "\"></td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    } else {
      out.println("<i>no items</i>");
    }
  }

  private static void printTemplatesOrGoals(WebUser webUser, int projectId, Session dataSession,
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
      out.println("    <td class=\"inside\"><input type=\"checkbox\" name=\"" + PARAM_COMPLETED + "\" value=\""
          + pa.getActionId() + "\"></td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }
}
