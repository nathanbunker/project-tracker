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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ActionServlet extends ClientServlet {

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
        
        String actionIdString = request.getParameter("actionId");
        if (actionIdString != null) {
          ProjectAction projectAction = (ProjectAction) dataSession.get(ProjectAction.class, Integer.parseInt(actionIdString));
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
        
        Project project = appReq.getProject();
        ProjectAction projectAction = appReq.getProjectAction();
        if (projectAction != null) {
          project = projectAction.getProject();
        }
        appReq.setProject(project);
        appReq.setProjectSelected(project);
        appReq.setProjectAction(projectAction);
        appReq.setProjectActionSelected(projectAction);
        
        String requestBody = null;
        String responseBody = null;
        String nextSummary = request.getParameter("nextSummary");
        String nextNotes = request.getParameter("nextNotes");
        
        String systemInsructions = "You are a helpful assistant tasked with helping a professional report about progress that is being made on a project.";
        String prompt = "";

        
        if (projectAction != null)
        {
          boolean isChanged = false;
          if (nextSummary != null) {
            projectAction.setNextSummary(nextSummary);
            isChanged = true;
          }
          if (nextNotes != null && nextNotes.length() > 0) {
            if (projectAction.getNextNotes() != null && projectAction.getNextNotes().trim().length() > 0) {
              nextNotes = projectAction.getNextNotes() + "\n - " + nextNotes;
            }
            else {
              nextNotes = " - " + nextNotes;
            }
            projectAction.setNextNotes(nextNotes);
            isChanged = true;
          }
          {
            // query the Bill Entry table for the bill entry with the same actionId, sum up the time spent
            // Here is the query: select sum(bill_mins) from bill_entry where action_id = {action_id}
            Query query = dataSession.createQuery("select sum(billMins) from BillEntry where action = :action");
            query.setParameter("action", projectAction);
            List<Long> billMinsList = query.list();
            if (billMinsList.size() > 0) {
              int billMins = billMinsList.get(0).intValue();
              if (projectAction.getNextTimeActual() == null || projectAction.getNextTimeActual() != billMins) {
                projectAction.setNextTimeActual(billMins);
                isChanged = true;
              }
            }
          }
          if (isChanged) {
            Transaction transaction = dataSession.beginTransaction();;
            dataSession.update(projectAction);
            transaction.commit();
          }
          if (nextNotes != null || nextSummary != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            
            prompt = "I have taken action on a recent project and need to provide an update to be included in reporting to my supervisor and other project participants.  \n\n"
              + "Project name: " + project.getProjectName() + " \n"
              + "Project description: " + project.getDescription() + " \n"
              + "Recent actions taken: \n";
            List<ProjectAction> projectActionList = ProjectServlet.getProjectActionsTakenList(dataSession, project);
            int limit = 20;
            for (ProjectAction pa : projectActionList) {
              prompt += " - " + sdf.format(pa.getActionDate()) + " " + pa.getActionDescription() + " \n";
              limit--;
              if (limit == 0) {
                break;
              }
            }
            prompt += "Working on this next action: " + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + " \n";
            if (projectAction.getNextNotes() != null && projectAction.getNextNotes().length() > 0) {
              prompt += "Next action notes: \n" + projectAction.getNextNotes() + " \n";
            }
            if (projectAction.getNextSummary() != null && projectAction.getNextSummary().length() > 0) {
              prompt += "The current summary is: \n" + projectAction.getNextSummary() + " \n";
              prompt += "Please rewrite the summary for this action. ";
            }
            else {
              prompt += "Please create a draft summary for this action. ";
            }
            prompt += "The recent actions taken are previously generated summaries of actions taken for this project. Use the previously document recent actions taken as examples of what to create, although keep in mind that actions written before November 2024 are often too short and lack detail. Please use the working on next action and the next action notes as the basis for creating a complete update of the action taken. It will join the list of recent actions taken.  \n";
            prompt += "I will repoprt this to my supervisor and other contacts as this action having been completed on today's date " + sdf.format(new Date()) + ". Please give me only the text of the update, as it would appear after the date and no other commentary. Thanks!";
          }
        }
        
        String message = null;
        String chatGPTResponse = null;
        String chatGPTError = null;
        if (action != null) {


          if (action.equals("Propose")) {
            // API endpoint
            String endpoint = "https://api.openai.com/v1/chat/completions";
            ObjectMapper objectMapper = new ObjectMapper();

            String apiKey = System.getenv("CHATGPT_API_KEY_TOMCAT");

            // Request parameters
            String modelId = "gpt-4o";

            // Create an HTTP client
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
              // Create a POST request
              HttpPost postRequest = new HttpPost(endpoint);
              postRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
              postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");


              // Create the request body
              requestBody =
                  "{\"model\": \""
                      + modelId
                      + "\", \"messages\": [{\"role\": \"system\", \"content\": [{ \"type\": \"text\", \"text\":  " 
                      + objectMapper.writeValueAsString(systemInsructions) + "}]}, "
                      + "{\"role\": \"user\", \"content\": [{ \"type\": \"text\", \"text\":  "
                      + objectMapper.writeValueAsString(prompt) + "}]}], " 
                      + "\"temperature\": 1, \"max_tokens\": 2048, \"top_p\": 1, \"frequency_penalty\": 0, \"presence_penalty\": 0, \"response_format\": {\"type\": \"text\"}}";
              
                      postRequest.setEntity(new StringEntity(requestBody));

              // Send the request and get the response
              HttpResponse postResponse = httpClient.execute(postRequest);

              // Process the response
              responseBody = EntityUtils.toString(postResponse.getEntity());

              JsonNode rootNode = objectMapper.readTree(responseBody);
              if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
                // Extract the "content" field from the first choice
                JsonNode firstChoice = rootNode.get("choices").get(0);
                JsonNode messageNode = firstChoice.get("message");
                if (messageNode != null && messageNode.has("content")) {
                    String returnedText = messageNode.get("content").asText();
                    chatGPTResponse = returnedText;
                } else {
                  chatGPTError = "Unable to find the \"content\" field";
                }
            } else {
              chatGPTError = "Unable to find the \"choices\" field";
            }
            } catch (IOException ex) {
              chatGPTError = ex.getMessage();
            }


            if (chatGPTError == null && chatGPTResponse != null)
            {
              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
              String likelyDatePrefix1 = sdf.format(new Date());
              String likelyDatePrefix2 = "- " + likelyDatePrefix1;
              if (chatGPTResponse.startsWith(likelyDatePrefix1)) { 
                nextSummary = chatGPTResponse.substring(likelyDatePrefix1.length()).trim();
              } else if (chatGPTResponse.startsWith(likelyDatePrefix2)) { 
                nextSummary = chatGPTResponse.substring(likelyDatePrefix2.length()).trim();
              } else {
                nextSummary = chatGPTResponse;
              }
              projectAction.setNextSummary(nextSummary);
            }
          }
            // May do something in the future here
        }


        List<ProjectAction> projectActionList = getProjectActionList(webUser, dataSession);
      
        if (prepareProjectActionListAndIdentifyOverdue(dataSession, projectActionList, webUser).size() > 0) {
          // TOOD print a nicer message and a link to clean these up
          message = "There are actions overdue that are not shown here, only showing what is scheduled for today.";
        }
        appReq.setMessageProblem(message);
        appReq.setTitle("Home");
        printHtmlHead(appReq);

        printActionsDue(projectActionList, webUser, out, dataSession, appReq);

        out.println("<h2>Chat with GPT-4o</h2>");
        out.println("<h3>Prompt</h3>");
        out.println("<pre>" + prompt + "</pre>");
        if (requestBody != null) {
          out.println("<h3>Request</h3>");
          out.println("<pre>" + requestBody + "</pre>");
          out.println("<h3>Response</h3>");
          out.println("<pre>" + responseBody + "</pre>");
          if (chatGPTResponse != null) {
            out.println("<h3>Response Text</h3>");
            out.println("<p>" + chatGPTResponse + "</p>");
          }
          if (chatGPTError != null) {
            out.println("<h3>Error</h3>");
            out.println("<p>" + chatGPTError + "</p>");
          }
        }

      
        printHtmlFoot(appReq);
      
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  protected static void printActionsDue(List<ProjectAction> projectActionList, WebUser webUser, PrintWriter out, Session dataSession, AppReq appReq) {
        Date nextDue = new Date();
    SimpleDateFormat sdf1 = webUser.getDateFormat();
        
    out.println("<div class=\"main\">");
    Calendar cIndicated = webUser.getCalendar();
    cIndicated.setTime(nextDue);



    out.println("<div class=\"actionNow\">");
    if (appReq.getProjectAction() != null) {
      ProjectAction projectAction = appReq.getProjectAction();
      SimpleDateFormat sdf11 = webUser.getDateFormat();
      Project project = projectAction.getProject();
      String link = "<a href=\"ActionServlet?actionId=" + projectAction.getActionId() + "\">";
      out.println("<form action=\"ActionServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"actionId\" value=\"" + projectAction.getActionId() + "\"/>");  
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\">Get it done!</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"outside\">");
      out.println("      <table class=\"boxed-fill\">");
      out.println("        <tr>");
      out.println("          <th>Action</th>");
      ProjectServlet.printActionDescription(webUser, out, sdf11, projectAction, link, new Date());
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th>Project</th>");
      out.println("          <td class=\"inside\">");
      out.println(project.getProjectName());
      out.println("          </td>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th>Notes</th>");
      out.println("          <td class=\"inside\">");
      if (projectAction.getNextNotes() != null) {
        out.println(projectAction.getNextNotes().replace("\n", "<br/>"));
        out.println("<br/>");
      }
      out.println("            <textarea name=\"nextNotes\" rows=\"7\" cols=\"70\" onkeydown=\"resetRefresh()\"></textarea>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("      </table>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <th class=\"title\">Proposed Summary</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"outside\">");
      out.println("      <table class=\"boxed-fill\">");
      out.println("        <tr>");
      out.println("          <th>Action Taken</th>");
      out.println("          <td class=\"inside\">");
      out.println("            <textarea name=\"nextSummary\" rows=\"10\" cols=\"70\" onkeydown=\"resetRefresh()\">" + projectAction.getNextSummary() + "</textarea>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("      </table>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"boxed-submit\">");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Propose\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Save\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Completed\"/>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");
    }
    out.println("</div>");


    out.println("<div class=\"actionToday\">");
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"3\">What do you want to do today?</th>");
    out.println("  </tr>");
    printDueTable(webUser, out, sdf1, ProjectNextActionType.OVERDUE_TO, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.COMMITTED_TO, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_CONTACT, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_MEET, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.MIGHT, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WILL_RUN_ERRAND, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.GOAL, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.TASK, nextDue, projectActionList, cIndicated);
    printDueTable(webUser, out, sdf1, ProjectNextActionType.WAITING, nextDue, projectActionList, cIndicated);
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

    out.println("</div>");
  }

  private static List<ProjectAction> getProjectActionList(WebUser webUser, Session dataSession) {
    Query query = dataSession.createQuery(
        "from ProjectAction where provider = :provider and (contactId = :contactId or nextContactId = :nextContactId) "
            + "and nextActionId = 0 and nextDescription <> '' "
            + "order by nextDue, priority_level DESC, nextTimeEstimate, actionDate");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter("nextContactId", webUser.getContactId());
    @SuppressWarnings("unchecked")
    List<ProjectAction> projectActionList = query.list();
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
      runningTotal =
          printTimeTotal(out, runningTotal, "Other", nextTimeEstimateTotal - runningTotal);
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

    List <ProjectAction> paList = new ArrayList<ProjectAction>();
    for (ProjectAction projectAction : projectActionList) {
      if (!sameDay(cIndicated, projectAction.getNextDue(), webUser)) {
        continue;
      }
      if (projectAction.getNextActionType() != null
          && projectAction.getNextActionType().equals(nextActionType)) {
        paList.add(projectAction);
      }
    }
    if (paList.size() == 0) {
      return;
    }
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">" + ProjectNextActionType.getLabel(nextActionType) + "</th>");
    out.println("    <th class=\"boxed\">Est</th>");
    out.println("    <th class=\"boxed\">Act</th>");
    out.println("  </tr>");


    for (ProjectAction projectAction : paList) {
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\"><a href=\"ActionServlet?actionId="
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
