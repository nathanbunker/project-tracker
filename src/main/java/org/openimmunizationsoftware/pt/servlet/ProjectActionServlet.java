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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.ChatAgent;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.PrioritySpecial;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectActionServlet extends ClientServlet {

  protected static final String PARAM_COMPLETING_ACTION_ID = "completingActionId";

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
        ProjectAction projectAction = (ProjectAction) dataSession.get(ProjectAction.class,
            Integer.parseInt(completingActionIdString));
        setupProjectActionAndSaveToAppReq(appReq, dataSession, projectAction);
      }

      Project project = appReq.getProject();
      ProjectAction projectAction = appReq.getProjectAction();
      if (projectAction != null) {
        project = projectAction.getProject();
      }
      appReq.setProject(project);
      appReq.setProjectSelected(project);
      appReq.setProjectAction(projectAction);
      appReq.setProjectActionSelected(projectAction);

      String nextSummary = request.getParameter("nextSummary");
      String nextNotes = request.getParameter("nextNotes");

      String systemInsructions = "You are a helpful assistant tasked with helping a professional report about progress that is being made on a project.";
      ChatAgent chatAgent = null;

      String proposePrompt = "";
      String feedbackPrompt = "";
      String nextPrompt = "";
      String nextFeedback = null;
      String nextSuggest = null;

      boolean isCompleted = false;

      if (projectAction != null) {
        boolean isChanged = false;
        if (nextSummary != null) {
          projectAction.setNextSummary(nextSummary);
          isChanged = true;
        }
        if (nextNotes != null && nextNotes.length() > 0) {
          if (projectAction.getNextNotes() != null && projectAction.getNextNotes().trim().length() > 0) {
            nextNotes = projectAction.getNextNotes() + "\n - " + nextNotes;
          } else {
            nextNotes = " - " + nextNotes;
          }
          projectAction.setNextNotes(nextNotes);
          isChanged = true;
        }
        {
          // query the Bill Entry table for the bill entry with the same actionId, sum up
          // the time spent
          // Here is the query: select sum(bill_mins) from bill_entry where action_id =
          // {action_id}
          Query query = dataSession.createQuery("select sum(billMins) from BillEntry where action = :action");
          query.setParameter("action", projectAction);
          List<Long> billMinsList = query.list();
          if (billMinsList.size() > 0) {
            if (billMinsList.get(0) != null) {
              int billMins = billMinsList.get(0).intValue();
              if (projectAction.getNextTimeActual() == null || projectAction.getNextTimeActual() != billMins) {
                projectAction.setNextTimeActual(billMins);
                isChanged = true;
              }
            }
          }
        }
        if (isChanged) {
          Transaction transaction = dataSession.beginTransaction();
          ;
          dataSession.update(projectAction);
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
              + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + " \n";
          if (projectAction.getNextNotes() != null && projectAction.getNextNotes().length() > 0) {
            basePrompt += "Next action notes: \n" + projectAction.getNextNotes() + " \n";
          }

          if (projectAction.getNextSummary() != null && projectAction.getNextSummary().length() > 0) {
            basePrompt += "The current summary is: \n" + projectAction.getNextSummary() + " \n";
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

        if (action.equals("Propose")) {
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
            projectAction.setNextSummary(nextSummary);
          }
        } else if (action.equals("Feedback")) {
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
              projectAction.setNextFeedback(nextFeedback);
              Transaction transaction = dataSession.beginTransaction();
              dataSession.update(projectAction);
              transaction.commit();
            }
          }
        } else if (action.equals("Completed")) {
          isCompleted = true;
        } else if (action.equals("Completed and Suggest")) {
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
        } else if (action.equals("Save")) {
          emailBody = ProjectServlet.saveProjectAction(appReq, project, emailBody);
          projectAction = null;
          appReq.setProject(null);
          appReq.setProjectAction(null);
        }
      }

      List<ProjectAction> projectActionList = getProjectActionListForToday(webUser, dataSession);
      if (projectAction == null && projectActionList.size() > 0) {
        projectAction = projectActionList.get(0);
        setupProjectActionAndSaveToAppReq(appReq, dataSession, projectAction);
      }

      if (prepareProjectActionListAndIdentifyOverdue(dataSession, projectActionList, webUser).size() > 0) {
        // TOOD print a nicer message and a link to clean these up
        message = "There are actions overdue that are not shown here, only showing what is scheduled for today.";
      } else if (projectActionList.size() == 0) {
        message = "You have completed all tasks for today. Have a great evening!";
      }
      appReq.setMessageProblem(message);
      appReq.setTitle("Actions");
      printHtmlHead(appReq);
      out.println("<div class=\"main\">");
      ProjectServlet.printOutEmailSent(out, emailBody);

      printActionsDue(projectActionList, isCompleted, webUser, out, dataSession, appReq);

      if (!isCompleted && projectAction != null && projectAction.getNextFeedback() != null) {
        out.println("<h3>Feedback</h3>");
        out.println("" + projectAction.getNextFeedback() + "");
      }

      if (isCompleted && nextSuggest != null) {
        out.println("<h3>Next Step Suggestions</h3>");
        out.println("" + projectAction.getNextFeedback() + "");
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
      out.println("</div>");

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private void setupProjectActionAndSaveToAppReq(AppReq appReq, Session dataSession, ProjectAction projectAction) {
    projectAction
        .setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
    projectAction.setContact(
        (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId()));
    if (projectAction.getNextContactId() != null && projectAction.getNextContactId() > 0) {
      projectAction.setNextProjectContact((ProjectContact) dataSession.get(ProjectContact.class,
          projectAction.getNextContactId()));
    }
    appReq.setProjectAction(projectAction);
    appReq.setProject(projectAction.getProject());
  }

  protected static void printActionsDue(List<ProjectAction> projectActionList, boolean isCompleted, WebUser webUser,
      PrintWriter out, Session dataSession, AppReq appReq) {
    Date nextDue = new Date();
    SimpleDateFormat sdf1 = webUser.getDateFormat();

    out.println("<div class=\"main\">");
    Calendar cIndicated = webUser.getCalendar();
    cIndicated.setTime(nextDue);

    out.println("<div id=\"actionsToday\">");
    if (appReq.getProjectAction() != null) {
      ProjectAction projectAction = appReq.getProjectAction();
      SimpleDateFormat sdf11 = webUser.getDateFormat();
      int projectId = projectAction.getProject().getProjectId();
      out.println("<form name=\"projectAction" + projectId
          + "\" method=\"post\" action=\"ProjectActionServlet\" id=\"saveProjectActionForm" + projectId
          + "\">");
      out.println("<table class=\"boxed-full\">");

      out.println("  <tr>");
      out.println("    <th class=\"title\">Work on this action now</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"outside\">");
      out.println("      <table class=\"boxed-fill\">");
      out.println("        <tr>");
      out.println("          <th width=\"15%\">Action</th>");
      ProjectServlet.printActionDescription(webUser, out, sdf11, projectAction, null, new Date());
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th>Notes</th>");
      out.println("          <td class=\"inside\">");
      if (projectAction.getNextNotes() != null) {
        out.println(projectAction.getNextNotes().replace("\n", "<br/>"));
        out.println("<br/>");
      }
      out.println("            <textarea name=\"nextNotes\" rows=\"7\" onkeydown=\"resetRefresh()\"></textarea>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("      </table>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"boxed-submit\">");
      out.println("       <input type=\"hidden\" name=\"actionId\" value=\"" + projectAction.getActionId() + "\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Note\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Propose\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Feedback\"/>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <th class=\"title\">Proposed Summary</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"outside\">");
      out.println("      <table class=\"boxed-fill\">");
      out.println("        <tr>");
      out.println("          <th width=\"15%\">Action Taken</th>");
      out.println("          <td class=\"inside\">");
      out.println("            <textarea name=\"nextSummary\" rows=\"12\" onkeydown=\"resetRefresh()\">"
          + n(projectAction.getNextSummary()) + "</textarea>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("      </table>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"boxed-submit\">");
      out.println(
          "       <input type=\"hidden\" name=\"" + PARAM_COMPLETING_ACTION_ID +  "\" value=\"" + projectAction.getActionId() + "\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Completed\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Completed and Suggest\"/>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("<br/>");

      out.println("<div id=\"takeAction\">");
      Project project = projectAction.getProject();
      projectId = project.getProjectId();
      List<ProjectContactAssigned> projectContactAssignedList = ProjectServlet
          .getProjectContactAssignedList(dataSession, projectId);
      List<ProjectContact> projectContactList = ProjectServlet.getProjectContactList(dataSession, project,
          projectContactAssignedList);
      ProjectServlet.printProjectUpdateForm(appReq, projectId, projectContactList, null, (isCompleted ? projectAction : null));
      out.println("</div>");

      out.println("</form>");
    }

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

    int nextTimeEstimateTotal = 0;
    int nextTimeEstimateCommit = 0;
    int nextTimeEstimateWill = 0;
    int nextTimeEstimateWillMeet = 0;
    int nextTimeEstimateMight = 0;
    for (ProjectAction projectAction : projectActionList) {
      if (!sameDay(cIndicated, projectAction.getNextDue(), webUser)) {
        continue;
      }
      if (projectAction.getNextTimeEstimate() != null) {
        nextTimeEstimateTotal += projectAction.getNextTimeEstimate();
        if (ProjectNextActionType.COMMITTED_TO.equals(projectAction.getNextActionType())
            || ProjectNextActionType.OVERDUE_TO.equals(projectAction.getNextActionType())) {
          nextTimeEstimateCommit += projectAction.getNextTimeEstimate();
        } else if (ProjectNextActionType.WILL.equals(projectAction.getNextActionType())
            || ProjectNextActionType.WILL_CONTACT.equals(projectAction.getNextActionType())) {
          nextTimeEstimateWill += projectAction.getNextTimeEstimate();
        } else if (ProjectNextActionType.WILL_MEET.equals(projectAction.getNextActionType())) {
          nextTimeEstimateWillMeet += projectAction.getNextTimeEstimate();
        } else if (ProjectNextActionType.MIGHT.equals(projectAction.getNextActionType())) {
          nextTimeEstimateMight += projectAction.getNextTimeEstimate();
        }

      }
    }
    printTimeEstimateBox(out, nextTimeEstimateTotal, nextTimeEstimateCommit, nextTimeEstimateWill,
        nextTimeEstimateWillMeet, nextTimeEstimateMight);

    out.println("</div>");
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
    query.setParameter("nextContactId", webUser.getContactId());
    query.setParameter("today", today);
    query.setParameter("tomorrow", tomorrow);
    @SuppressWarnings("unchecked")
    List<ProjectAction> projectActionList = query.list();
    // sorth the projectActionList first by the defaultPriority from the
    // ProjectNextActionType and then by the priority_level
    projectActionList.sort((pa1, pa2) -> {
      PrioritySpecial ps1 = pa1.getPrioritySpecial();
      PrioritySpecial ps2 = pa2.getPrioritySpecial();
      // If one of the priorities is special, then we need to sort by the special priority, unless they are the same
      if ((ps1 != null || ps2 != null) && ps1 != ps2)
      {
        // very complicated logic to sort by priority special
        // FIRST must go first before any SECOND, or any other priority without a special priority
        if (ps1 == PrioritySpecial.FIRST) {
          return -1;
        } else if (ps2 == PrioritySpecial.FIRST) {
            return 1;
        }
        // SECOND must go after any FIRST, but before any other priority without a special priority
        if (ps1 == PrioritySpecial.SECOND) {
          return -1;
        } else if (ps2 == PrioritySpecial.SECOND) {
          return 1;
        }
        // LAST must go last after any other priority without a special priority and PENULTIMATE
        if (ps1 == PrioritySpecial.LAST) {
          return 1;
        } else if (ps2 == PrioritySpecial.LAST) {
          return -1;
        }
        // PENUlTIMATE must go last after any other priority without a special priority, but before any LAST
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

  private static void printTimeEstimateBox(PrintWriter out, int nextTimeEstimateTotal,
      int nextTimeEstimateCommit, int nextTimeEstimateWill, int nextTimeEstimateWillMeet,
      int nextTimeEstimateMight) {
    if (nextTimeEstimateTotal > 0) {
      out.println("<table class=\"boxed\">");
      int runningTotal = 0;
      runningTotal = printTimeTotal(out, runningTotal, "Will Meet", nextTimeEstimateWillMeet);
      runningTotal = printTimeTotal(out, runningTotal, "Committed", nextTimeEstimateCommit);
      runningTotal = printTimeTotal(out, runningTotal, "Will", nextTimeEstimateWill);
      runningTotal = printTimeTotal(out, runningTotal, "Might", nextTimeEstimateMight);
      runningTotal = printTimeTotal(out, runningTotal, "Other", nextTimeEstimateTotal - runningTotal);
      out.println("</table><br/>");
    }
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
}
