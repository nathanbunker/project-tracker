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
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

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
          appReq.setProjectAction(projectAction);
          appReq.setProject(projectAction.getProject());
        }

        String message = null;
        if (action != null) {
            // May do something in the future here
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

        List<ProjectAction> projectActionList = getProjectActionList(webUser, dataSession);
      
        if (prepareProjectActionListAndIdentifyOverdue(dataSession, projectActionList, webUser).size() > 0) {
          // TOOD print a nicer message and a link to clean these up
          message = "There are actions overdue that are not shown here, only showing what is scheduled for today.";
        }
        appReq.setMessageProblem(message);
        appReq.setTitle("Home");
        printHtmlHead(appReq);

        printActionsDue(projectActionList, webUser, out, dataSession, appReq);

        out.println("</div>");
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

    out.println("<div class=\"projectInfo\">");
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

    out.println("<div class=\"takeAction\">");
    ProjectAction projectAction = appReq.getProjectAction();
    if (projectAction != null) {
      SimpleDateFormat sdf11 = webUser.getDateFormat();
      Project project = projectAction.getProject();
      String link = "<a href=\"ActionServlet?actionId=" + projectAction.getActionId() + "\">";
      out.println("<form action=\"ActionServlet\" method=\"POST\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\">Action</th>");
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
      out.println("            <textarea name=\"nextNotes\" rows=\"10\" cols=\"60\"></textarea>");
      out.println("            <br/>");
      out.println("            <input type=\"submit\" name=\"action\" value=\"Propose\"/>");
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
      out.println("          <th>Summary</th>");
      out.println("        </tr>");
      out.println("        <tr>");
      out.println("          <th>Notes</th>");
      out.println("          <td class=\"inside\">");
      out.println("            <textarea name=\"summary\" rows=\"10\" cols=\"60\"></textarea>");
      out.println("          </td>");
      out.println("        </tr>");
      out.println("      </table>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <td class=\"boxed-submit\">");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Save\"/>");
      out.println("     <input type=\"submit\" name=\"action\" value=\"Completed\"/>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");
    }
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
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"title\" colspan=\"3\">" + ProjectNextActionType.getLabel(nextActionType) + "</th>");
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Time</th>");
    out.println("    <th class=\"boxed\">To Do</th>");
    out.println("  </tr>");


    for (ProjectAction projectAction : paList) {
      out.println("  <tr class=\"boxed\">");
      out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
          + projectAction.getProject().getProjectId() + "\" class=\"button\">"
          + projectAction.getProject().getProjectName() + "</a></td>");
      if (projectAction.getNextTimeEstimate() == null || projectAction.getNextTimeEstimate() == 0) {
        out.println("    <td class=\"boxed\">&nbsp;</a></td>");
      } else {
        out.println(
            "    <td class=\"boxed\">" + projectAction.getNextTimeEstimateForDisplay() + "</a></td>");
      }
      out.println("    <td class=\"boxed\"><a href=\"ActionServlet?actionId="
          + projectAction.getActionId() + "\" class=\"button\">"
          + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a></td>");
  
      out.println("  </tr>");
    }

    out.println("</table><br/>");
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
