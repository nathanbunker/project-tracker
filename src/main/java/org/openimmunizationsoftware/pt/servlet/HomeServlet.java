/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
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
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class HomeServlet extends ClientServlet {

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
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();

      if (webUser == null) {
        appReq.setTitle("Home");
        printHtmlHead(appReq);
        out.println("<h1>Project Tracker</h1>");
        printHtmlFoot(appReq);
      } else {
        SimpleDateFormat sdf = webUser.getDateFormat();

        String date = request.getParameter("date");
        String nextActionType = request.getParameter("nextActionType");
        if (nextActionType == null) {
          nextActionType = "";
        }
        Date nextDue = null;
        if (date == null) {
          nextDue = new Date();
        } else {
          try {
            nextDue = sdf.parse(date);
          } catch (ParseException pe) {
            nextDue = new Date();
          }
        }
        String message = null;
        if (action != null) {
          if (action.equals("DoToday") || action.equals("DoNextWeek")
              || action.equals("DoTomorrow")) {
            int actionId = Integer.parseInt(request.getParameter("actionId"));
            ProjectAction projectAction =
                (ProjectAction) dataSession.get(ProjectAction.class, actionId);
            Transaction trans = dataSession.beginTransaction();
            try {
              Calendar calendar = TimeTracker.createToday(webUser);
              if (action.equals("DoTomorrow")) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
              } else if (action.equals("DoNextWeek")) {
                calendar.add(Calendar.DAY_OF_MONTH, 7);
              }
              projectAction.setNextDue(calendar.getTime());
            } finally {
              trans.commit();
            }
          } else if (action.equals("UpdateAction")) {
            int actionId = Integer.parseInt(request.getParameter("actionId"));
            ProjectAction projectAction =
                (ProjectAction) dataSession.get(ProjectAction.class, actionId);
            Transaction trans = dataSession.beginTransaction();
            try {
              Date oldDateDue = projectAction.getNextDue();
              try {
                projectAction.setNextDue(sdf.parse(request.getParameter("changeNextDue")));
              } catch (ParseException pe) {
                message = "Unable to parse next due date: " + pe.getMessage();
              }
              projectAction.setNextActionType(request.getParameter("changeNextActionType"));
              if (oldDateDue != null && oldDateDue.before(projectAction.getNextDue())) {
                if (projectAction.getNextActionType() != null) {
                  if (projectAction.getNextActionType()
                      .equals(ProjectNextActionType.COMMITTED_TO)) {
                    projectAction.setNextActionType(ProjectNextActionType.OVERDUE_TO);
                  } else if (projectAction.getNextActionType()
                      .equals(ProjectNextActionType.MIGHT)) {
                    projectAction.setNextActionType(ProjectNextActionType.WILL);
                  }
                }
              }
              if (request.getParameter("nextTimeEstimate") != null
                  && !request.getParameter("nextTimeEstimate").equals("")) {
                try {
                  projectAction.setNextTimeEstimate(
                      Integer.parseInt(request.getParameter("nextTimeEstimate")));
                } catch (NumberFormatException nfe) {
                  //
                  projectAction.setNextTimeEstimate(0);
                }
              } else {
                projectAction.setNextTimeEstimate(0);
              }

            } finally {
              trans.commit();
            }
          } else if (action.equals("Switch")) {
            String username = request.getParameter("childWebUserName");
            boolean switched = false;
            if (webUser.getParentWebUser() != null
                && webUser.getParentWebUser().getUsername().equals(username)) {
              webUser = webUser.getParentWebUser();
              appReq.setWebUser(webUser);
              switched = true;
            } else if (!webUser.getUsername().equals(username)) {
              @SuppressWarnings("unchecked")
              List<WebUser> childWebUserList = appReq.getChildWebUserList();
              if (childWebUserList != null) {
                for (WebUser childWebUser : childWebUserList) {
                  if (childWebUser.getUsername().equals(username)) {
                    Project project = appReq.getProject();
                    if (webUser.getParentWebUser() == null && project != null) {
                      appReq.setParentProject(project);
                    }
                    appReq.setProject(null);
                    appReq.setProjectAction(null);
                    webUser = childWebUser;
                    appReq.setWebUser(webUser);
                    switched = true;
                    break;
                  }
                }
              }
            }
            if (switched) {
              ProjectProvider projectProvider = webUser.getProvider();
              message = "Welcome " + webUser.getProjectContact().getName() + " to "
                  + projectProvider.getProviderName();
              appReq.setProjectIdList(null);
              appReq.setProjectSelectedList(null);
            }
          }
        }

        appReq.setMessageProblem(message);
        boolean showLink = true;

        appReq.setTitle("Home");
        printHtmlHead(appReq);

        printActionsDue(webUser, out, dataSession, nextActionType, nextDue, showLink, true);

        @SuppressWarnings("unchecked")
        List<WebUser> childWebUserList = appReq.getChildWebUserList();
        if (childWebUserList != null) {
          out.println("<h2>Select Provider</h2>");
          out.println("<table class=\"boxed\">");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"title\" colspan=\"1\">Choose Provider</th>");
          out.println("  </tr>");
          if (webUser.getParentWebUser() != null) {
            ProjectProvider projectProvider = webUser.getParentWebUser().getProvider();
            out.println("  <tr class=\"boxed\">");
            String switchLink = "HomeServlet?action=Switch&childWebUserName="
                + webUser.getParentWebUser().getUsername();
            String switchLabel = webUser.getParentWebUser().getUsername() + " on "
                + projectProvider.getProviderName();
            out.println("    <td class=\"boxed\"><a href=\"" + switchLink + "\" class=\"button\">"
                + switchLabel + "</a></td>");
            out.println("  </tr>");
          } else {
            ProjectProvider projectProvider = webUser.getProvider();
            out.println("  <tr class=\"boxed\">");
            String switchLink =
                "HomeServlet?action=Switch&childWebUserName=" + webUser.getUsername();
            String switchLabel = webUser.getUsername() + " on " + projectProvider.getProviderName();
            out.println("    <td class=\"boxed\"><a href=\"" + switchLink + "\" class=\"button\">"
                + switchLabel + "</a></td>");
            out.println("  </tr>");
          }
          for (WebUser childWebUser : childWebUserList) {
            ProjectProvider projectProvider = childWebUser.getProvider();
            out.println("  <tr class=\"boxed\">");
            String switchLink =
                "HomeServlet?action=Switch&childWebUserName=" + childWebUser.getUsername();
            String switchLabel =
                childWebUser.getUsername() + " on " + projectProvider.getProviderName();
            out.println("    <td class=\"boxed\"><a href=\"" + switchLink + "\" class=\"button\">"
                + switchLabel + "</a></td>");
            out.println("  </tr>");
          }
        }
        out.println("</table>");
        out.println("<h2>Logout</h2>");
        out.println(
            "<p>If you are finished you can <a href=\"LoginServlet?action=Logout\">logout</a>.</p>");
        out.println("</div>");
        printHtmlFoot(appReq);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  protected static void printActionsDue(WebUser webUser, PrintWriter out, Session dataSession,
      String nextActionType, Date nextDue, boolean showLink, boolean showMenu) {
    SimpleDateFormat sdf1 = webUser.getDateFormat();
    Query query = dataSession.createQuery(
        "from ProjectAction where provider = :provider and (contactId = :contactId or nextContactId = :nextContactId) "
            + "and nextActionId = 0 and nextDescription <> '' "
            + "order by nextDue, priority_level DESC, nextTimeEstimate, actionDate");
    query.setParameter("provider", webUser.getProvider());
    query.setParameter("contactId", webUser.getContactId());
    query.setParameter("nextContactId", webUser.getContactId());
    @SuppressWarnings("unchecked")
    List<ProjectAction> projectActionList = query.list();

    List<ProjectAction> projectActionListOverdue =
        prepareProjectActionListAndIdentifyOverdue(dataSession, projectActionList, webUser);
    out.println("<div class=\"main\">");
    if (projectActionListOverdue.size() > 0) {
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"3\">Actions Overdue</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Time</th>");
      out.println("    <th class=\"boxed\">To Do</th>");
      out.println("  </tr>");

      int nextTimeEstimateTotal = 0;
      int nextTimeEstimateCommit = 0;
      int nextTimeEstimateWill = 0;
      int nextTimeEstimateWillMeet = 0;
      int nextTimeEstimateMight = 0;
      for (ProjectAction projectAction : projectActionListOverdue) {
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
      for (ProjectAction projectAction : projectActionListOverdue) {
        printActionOverdueLine(webUser, out, sdf1, nextActionType, nextDue, projectAction,
            showLink);
      }
      out.println("</table><br/>");

      printTimeEstimateBox(out, nextTimeEstimateTotal, nextTimeEstimateCommit, nextTimeEstimateWill,
          nextTimeEstimateWillMeet, nextTimeEstimateMight);

    }

    Calendar cIndicated = webUser.getCalendar();
    cIndicated.setTime(nextDue);
    Calendar cToday = webUser.getCalendar();
    Calendar cTomorrow = webUser.getCalendar();
    cTomorrow.add(Calendar.DAY_OF_MONTH, 1);

    Calendar nextDueTomorrowCalendar = webUser.getCalendar();
    nextDueTomorrowCalendar.setTime(nextDue);
    nextDueTomorrowCalendar.add(Calendar.DAY_OF_MONTH, 1);
    while (nextDueTomorrowCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        || nextDueTomorrowCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
      nextDueTomorrowCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }
    Date nextDueTomorrow = nextDueTomorrowCalendar.getTime();

    if (showLink) {
      printScript(out, sdf1, nextActionType, nextDue);
    }
    if (showMenu) {
      printMenuForm(out, sdf1, nextActionType, nextDue, webUser);
    }
    printDueTable(webUser, out, sdf1, nextActionType, nextDue, projectActionList, cIndicated,
        cToday, cTomorrow, showLink);
    printDueTable(webUser, out, sdf1, nextActionType, nextDueTomorrow, projectActionList,
        nextDueTomorrowCalendar, cToday, cTomorrow, showLink);
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

  private static void printScript(PrintWriter out, SimpleDateFormat sdf1, String nextActionType,
      Date nextDue) {
    out.println("        <script>");
    out.println("          function setNextAction(nextActionDate)");
    out.println("          {");
    out.println("            document.setdate.date.value = nextActionDate;");
    out.println("            document.setdate.submit();");
    out.println("          }");
    out.println("          function changeNextAction(nextActionDate, id)");
    out.println("          {");
    out.println("            var form = document.getElementById(id);");
    out.println("            form.changeNextDue.value = nextActionDate;");
    out.println("            form.submit();");
    out.println("          }");
    out.println("          function changeNextTimeEstimate(timeInMinutes, id)");
    out.println("          {");
    out.println("            var form = document.getElementById(id);");
    out.println("            form.nextTimeEstimate.value = timeInMinutes;");
    out.println("            form.submit();");
    out.println("          }");
    out.println("          function changeNextActionType(nextActionType, id)");
    out.println("          {");
    out.println("            var form = document.getElementById(id);");
    out.println("            form.changeNextActionType.value = nextActionType;");
    out.println("            form.submit();");
    out.println("          }");
    out.println("        </script>");
  }

  private static void printMenuForm(PrintWriter out, SimpleDateFormat sdf1, String nextActionType,
      Date nextDue, WebUser webUser) {
    out.println("<form action=\"HomeServlet\" method=\"GET\" name=\"setdate\">");
    out.println("            <input type=\"hidden\" name=\"nextActionType\" value=\""
        + nextActionType + "\">");
    out.println("            I: <font size=\"-1\"><a href=\"HomeServlet?nextActionType="
        + ProjectNextActionType.WILL + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.WILL) ? "box" : "button") + "\"> will</a>,");
    out.println("            <a href=\"HomeServlet?nextActionType=" + ProjectNextActionType.MIGHT
        + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.MIGHT) ? "box" : "button")
        + "\">might</a>, ");
    out.println("            <a href=\"HomeServlet?nextActionType="
        + ProjectNextActionType.WILL_CONTACT + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.WILL_CONTACT) ? "box" : "button")
        + "\">will contact</a>");
    out.println("            <a href=\"HomeServlet?nextActionType="
        + ProjectNextActionType.WILL_MEET + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.WILL_MEET) ? "box" : "button")
        + "\">will meet</a></font>");
    out.println("            <br/> ");
    out.println("            I have: ");
    out.println("            <font size=\"-1\"><a href=\"HomeServlet?nextActionType="
        + ProjectNextActionType.COMMITTED_TO + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + ((nextActionType.equals(ProjectNextActionType.COMMITTED_TO)
            || nextActionType.equals(ProjectNextActionType.OVERDUE_TO)) ? "box" : "button")
        + "\">committed</a>,");
    out.println("            <a href=\"HomeServlet?nextActionType=" + ProjectNextActionType.GOAL
        + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.GOAL) ? "box" : "button")
        + "\">set goal</a>");
    out.println("            <a href=\"HomeServlet?nextActionType=" + ProjectNextActionType.TASK
        + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.TASK) ? "box" : "button")
        + "\">set task</a></font>");
    out.println("            <br/> ");
    out.println("            I am:");
    out.println("            <font size=\"-1\"><a href=\"HomeServlet?nextActionType="
        + ProjectNextActionType.WAITING + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.WAITING) ? "box" : "button")
        + "\">waiting</a>,");
    out.println("            <a href=\"HomeServlet?nextActionType=" + ProjectNextActionType.ASKS_TO
        + "&date=" + sdf1.format(nextDue) + "\" class=\""
        + (nextActionType.equals(ProjectNextActionType.ASKS_TO) ? "box" : "button")
        + "\">asking</a></font>");
    out.println("           <br>Due <input type=\"text\" name=\"date\" value=\""
        + sdf1.format(nextDue) + "\" size=\"10\" onchange=\"this.form.submit()\">");
    out.println("            <font size=\"-1\">");
    Calendar calendar = webUser.getCalendar();
    SimpleDateFormat day = webUser.getDateFormat("EEE");
    out.println("              <a href=\"javascript: void setNextAction('"
        + sdf1.format(calendar.getTime()) + "');\" class=\""
        + (sameDay(calendar, nextDue, webUser) ? "box" : "button") + "\">Today</a>");
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    out.println(
        "              <a href=\"javascript: void setNextAction('" + sdf1.format(calendar.getTime())
            + "');\" class=\"" + (sameDay(calendar, nextDue, webUser) ? "box" : "button") + "\">"
            + day.format(calendar.getTime()) + "</a>");
    boolean nextWeek = false;
    for (int i = 0; i < 6; i++) {
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      if (nextWeek) {
        out.println("              <a href=\"javascript: void setNextAction('"
            + sdf1.format(calendar.getTime()) + "');\" class=\""
            + (sameDay(calendar, nextDue, webUser) ? "box" : "button") + "\">Next-"
            + day.format(calendar.getTime()) + "</a>");
      } else {
        out.println("              <a href=\"javascript: void setNextAction('"
            + sdf1.format(calendar.getTime()) + "');\" class=\""
            + (sameDay(calendar, nextDue, webUser) ? "box" : "button") + "\">"
            + day.format(calendar.getTime()) + "</a>");

      }
      if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
        nextWeek = true;
      }
    }
    out.println("</font>");
    out.println("</form>");
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
      Calendar cIndicated, Calendar cToday, Calendar cTomorrow, boolean showLink) {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    if (sameDay(cToday, cIndicated)) {
      out.println("    <th class=\"title\" colspan=\"3\">Due Today</th>");
    } else if (sameDay(cTomorrow, cIndicated)) {
      out.println("    <th class=\"title\" colspan=\"3\">Due Tomorrow</th>");
    } else {
      out.println("    <th class=\"title\" colspan=\"3\">Due " + sdf1.format(nextDue) + "</th>");
    }
    out.println("  </tr>");
    out.println("  <tr class=\"boxed\">");
    out.println("    <th class=\"boxed\">Project</th>");
    out.println("    <th class=\"boxed\">Time</th>");
    out.println("    <th class=\"boxed\">To Do</th>");
    out.println("  </tr>");

    int askingAndWaitingCount = 0;
    int nextTimeEstimateTotal = 0;
    int nextTimeEstimateCommit = 0;
    int nextTimeEstimateWill = 0;
    int nextTimeEstimateWillMeet = 0;
    int nextTimeEstimateMight = 0;
    for (ProjectAction projectAction : projectActionList) {
      if (!sameDay(cIndicated, projectAction.getNextDue(), webUser)) {
        continue;
      }
      if (!nextActionType.equals("") && !nextActionType.equals(projectAction.getNextActionType())) {
        continue;
      }
      if ((ProjectNextActionType.ASKS_TO.equals(projectAction.getNextActionType())
          || ProjectNextActionType.WAITING.equals(projectAction.getNextActionType()))
          && projectAction.getContactId() == webUser.getContactId()) {
        askingAndWaitingCount++;
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

    for (ProjectAction projectAction : projectActionList) {
      if (projectAction.getNextActionType() != null
          && projectAction.getNextActionType().equals(ProjectNextActionType.OVERDUE_TO)) {
        printActionLine(webUser, out, sdf1, nextActionType, nextDue, cIndicated, projectAction,
            showLink);
      }
    }
    for (ProjectAction projectAction : projectActionList) {
      if (projectAction.getNextActionType() != null
          && projectAction.getNextActionType().equals(ProjectNextActionType.COMMITTED_TO)) {
        printActionLine(webUser, out, sdf1, nextActionType, nextDue, cIndicated, projectAction,
            showLink);
      }
    }
    for (ProjectAction projectAction : projectActionList) {
      if (projectAction.getNextActionType() != null
          && (projectAction.getNextActionType().equals(ProjectNextActionType.WILL)
              || projectAction.getNextActionType().equals(ProjectNextActionType.WILL_CONTACT))) {
        printActionLine(webUser, out, sdf1, nextActionType, nextDue, cIndicated, projectAction,
            showLink);
      }
    }
    for (ProjectAction projectAction : projectActionList) {
      if (projectAction.getNextActionType() != null
          && !projectAction.getNextActionType().equals(ProjectNextActionType.OVERDUE_TO)
          && !projectAction.getNextActionType().equals(ProjectNextActionType.COMMITTED_TO)
          && !projectAction.getNextActionType().equals(ProjectNextActionType.WILL)
          && !projectAction.getNextActionType().equals(ProjectNextActionType.WILL_CONTACT)
          && !((ProjectNextActionType.ASKS_TO.equals(projectAction.getNextActionType())
              || ProjectNextActionType.WAITING.equals(projectAction.getNextActionType()))
              && projectAction.getContactId() == webUser.getContactId())) {
        printActionLine(webUser, out, sdf1, nextActionType, nextDue, cIndicated, projectAction,
            showLink);
      }
    }

    out.println("</table><br/>");

    if (askingAndWaitingCount > 0) {
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"3\">Asking or Waiting</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Time</th>");
      out.println("    <th class=\"boxed\">To Do</th>");
      out.println("  </tr>");
      for (ProjectAction projectAction : projectActionList) {
        if (!sameDay(cIndicated, projectAction.getNextDue(), webUser)) {
          continue;
        }
        if (!nextActionType.equals("")
            && !nextActionType.equals(projectAction.getNextActionType())) {
          continue;
        }
        if ((ProjectNextActionType.ASKS_TO.equals(projectAction.getNextActionType())
            || ProjectNextActionType.WAITING.equals(projectAction.getNextActionType()))
            && projectAction.getContactId() == webUser.getContactId()) {
          printActionLine(webUser, out, sdf1, nextActionType, nextDue, cIndicated, projectAction,
              showLink);
        }
      }
      out.println("</table><br/>");
    }


    printTimeEstimateBox(out, nextTimeEstimateTotal, nextTimeEstimateCommit, nextTimeEstimateWill,
        nextTimeEstimateWillMeet, nextTimeEstimateMight);
  }

  private static void printActionLine(WebUser webUser, PrintWriter out, SimpleDateFormat sdf1,
      String nextActionType, Date nextDue, Calendar cIndicated, ProjectAction projectAction,
      boolean showLink) {
    if (!sameDay(cIndicated, projectAction.getNextDue(), webUser)) {
      return;
    }
    if (!nextActionType.equals("") && !nextActionType.equals(projectAction.getNextActionType())) {
      return;
    }
    out.println("  <tr class=\"boxed\">");
    if (showLink) {
      out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
          + projectAction.getProject().getProjectId() + "\" class=\"button\">"
          + projectAction.getProject().getProjectName() + "</a></td>");
    } else {
      out.println(
          "    <td class=\"boxed\">" + projectAction.getProject().getProjectName() + "</td>");
    }
    if (projectAction.getNextTimeEstimate() == null || projectAction.getNextTimeEstimate() == 0) {
      out.println("    <td class=\"boxed\">&nbsp;</a></td>");
    } else {
      out.println(
          "    <td class=\"boxed\">" + projectAction.getNextTimeEstimateForDisplay() + "</a></td>");
    }
    printOutAction(webUser, out, sdf1, nextActionType, nextDue, projectAction, showLink);

    out.println("  </tr>");
  }

  private static void printActionOverdueLine(WebUser webUser, PrintWriter out,
      SimpleDateFormat sdf1, String nextActionType, Date nextDue, ProjectAction projectAction,
      boolean showLink) {
    out.println("  <tr class=\"boxed\">");
    if (showLink) {
      out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId="
          + projectAction.getProject().getProjectId() + "\" class=\"button\">"
          + projectAction.getProject().getProjectName() + "</a></td>");
    } else {
      out.println(
          "    <td class=\"boxed\">" + projectAction.getProject().getProjectName() + "</td>");

    }
    if (projectAction.getNextTimeEstimate() != null && projectAction.getNextTimeEstimate() > 0) {
      out.println(
          "    <td class=\"boxed\">" + projectAction.getNextTimeEstimateForDisplay() + "</td>");
    } else {
      out.println("    <td class=\"boxed\">&nbsp;</td>");
    }

    printOutAction(webUser, out, sdf1, nextActionType, nextDue, projectAction, showLink);

    out.println("  </tr>");
  }

  private static String printOutAction(WebUser webUser, PrintWriter out, SimpleDateFormat sdf1,
      String nextActionType, Date nextDue, ProjectAction projectAction, boolean showLink) {
    String changeBoxId = "changeBox" + projectAction.getActionId();
    if (showLink) {
      out.println("    <td class=\"boxed\"><a href=\"javascript: void toggleLayer('" + changeBoxId
          + "'); \" class=\"button\">"
          + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()) + "</a>");
    } else {
      out.println("    <td class=\"boxed\">"
          + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact()));
    }
    Calendar today = TimeTracker.createToday(webUser);
    if (projectAction.getNextDue() != null && projectAction.getNextDue().before(today.getTime())) {
      today.add(Calendar.DAY_OF_MONTH, -1);
      if (!projectAction.getNextDue().before(today.getTime())) {
        out.println("    <span class=\"fail\">Due Yesterday</span>");
      } else {
        out.println(
            "    <span class=\"fail\">Due " + sdf1.format(projectAction.getNextDue()) + "</span>");
      }
    }

    if (showLink) {
      out.println("<div class=\"editAction\" id=\"" + changeBoxId + "\">");
      String changeFormId = "changeForm" + projectAction.getActionId();
      out.println(
          "        <form action=\"HomeServlet\" method=\"GET\" id=\"" + changeFormId + "\">");
      out.println("            <input type=\"hidden\" name=\"action\" value=\"UpdateAction\">");
      out.println("            <input type=\"hidden\" name=\"actionId\" value=\""
          + projectAction.getActionId() + "\">");
      out.println("            <input type=\"hidden\" name=\"changeNextActionType\" value=\""
          + projectAction.getNextActionType() + "\">");
      out.println("            <input type=\"hidden\" name=\"nextActionType\" value=\""
          + nextActionType + "\">");
      out.println("            <input type=\"hidden\" name=\"date\" value=\"" + sdf1.format(nextDue)
          + "\">");
      out.println(
          "            I: <font size=\"-1\"><a href=\"javascript: void changeNextActionType('"
              + ProjectNextActionType.WILL + "', '" + changeFormId + "'); \" class=\""
              + (projectAction.getNextActionType().equals(ProjectNextActionType.WILL) ? "box"
                  : "button")
              + "\"> will</a>,");
      out.println("            <a href=\"javascript: void changeNextActionType('M', '"
          + changeFormId + "'); \" class=\""
          + (projectAction.getNextActionType().equals("M") ? "box" : "button") + "\">might</a>, ");
      out.println("            <a href=\"javascript: void changeNextActionType('"
          + ProjectNextActionType.WILL_CONTACT + "', '" + changeFormId + "'); \" class=\""
          + (projectAction.getNextActionType().equals(ProjectNextActionType.WILL_CONTACT) ? "box"
              : "button")
          + "\">will contact</a>");
      out.println("            <a href=\"HomeServlet?nextActionType="
          + ProjectNextActionType.WILL_MEET + "&date=" + sdf1.format(nextDue) + "\" class=\""
          + (nextActionType.equals(ProjectNextActionType.WILL_MEET) ? "box" : "button")
          + "\">will meet</a></font>");
      out.println("            <br/> ");
      out.println("            I have: ");
      out.println("            <font size=\"-1\"><a href=\"javascript: void changeNextActionType('"
          + ProjectNextActionType.COMMITTED_TO + "', '" + changeFormId + "'); \" class=\""
          + ((projectAction.getNextActionType().equals(ProjectNextActionType.COMMITTED_TO)
              || projectAction.getNextActionType().equals(ProjectNextActionType.OVERDUE_TO)) ? "box"
                  : "button")
          + "\">committed</a>,");
      out.println("            <a href=\"javascript: void changeNextActionType('G', '"
          + changeFormId + "'); \" class=\""
          + (projectAction.getNextActionType().equals("G") ? "box" : "button") + "\">set goal</a>");
      out.println("            <a href=\"HomeServlet?nextActionType=" + ProjectNextActionType.TASK
          + "&date=" + sdf1.format(nextDue) + "\" class=\""
          + (nextActionType.equals(ProjectNextActionType.TASK) ? "box" : "button")
          + "\">set task</a></font>");
      out.println("            I am:");
      out.println(
          "            <font size=\"-1\"><a href=\"javascript: void changeNextActionType('W', '"
              + changeFormId + "'); \" class=\""
              + (projectAction.getNextActionType().equals("W") ? "box" : "button")
              + "\">waiting</a>,");
      out.println("            <a href=\"javascript: void setNextActionType('A', '" + changeFormId
          + "'); \" class=\"" + (projectAction.getNextActionType().equals("A") ? "box" : "button")
          + "\">asking</a></font>");
      out.println("           <br> Due <input type=\"text\" name=\"changeNextDue\" value=\""
          + (projectAction.getNextDue() == null ? "" : sdf1.format(projectAction.getNextDue()))
          + "\" size=\"10\" onchange=\"this.form.submit()\">");
      out.println("            <font size=\"-1\">");
      Calendar calendar = webUser.getCalendar();
      SimpleDateFormat day = webUser.getDateFormat("EEE");
      out.println("              <a href=\"javascript: void changeNextAction('"
          + sdf1.format(calendar.getTime()) + "', '" + changeFormId + "');\" class=\""
          + (sameDay(calendar, projectAction.getNextDue(), webUser) ? "box" : "button")
          + "\">Today</a>");
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      out.println("              <a href=\"javascript: void changeNextAction('"
          + sdf1.format(calendar.getTime()) + "', '" + changeFormId + "');\" class=\""
          + (sameDay(calendar, projectAction.getNextDue(), webUser) ? "box" : "button") + "\">"
          + day.format(calendar.getTime()) + "</a>");
      boolean nextWeek = false;
      for (int i = 0; i < 6; i++) {
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        if (nextWeek) {
          out.println("              <a href=\"javascript: void changeNextAction('"
              + sdf1.format(calendar.getTime()) + "', '" + changeFormId + "');\" class=\""
              + (sameDay(calendar, projectAction.getNextDue(), webUser) ? "box" : "button")
              + "\">Next-" + day.format(calendar.getTime()) + "</a>");
        } else {
          out.println("              <a href=\"javascript: void changeNextAction('"
              + sdf1.format(calendar.getTime()) + "', '" + changeFormId + "');\" class=\""
              + (sameDay(calendar, projectAction.getNextDue(), webUser) ? "box" : "button") + "\">"
              + day.format(calendar.getTime()) + "</a>");

        }
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
          nextWeek = true;
        }
      }
      out.println("</font>");
      out.println("           <br> Time <input type=\"text\" name=\"nextTimeEstimate\" value=\""
          + (projectAction.getNextTimeEstimate() != null && projectAction.getNextTimeEstimate() > 0
              ? ("" + projectAction.getNextTimeEstimate())
              : "")
          + "\" size=\"3\" onchange=\"this.form.submit()\"> mins");
      out.println("            <font size=\"-1\">");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('5', '"
          + changeFormId + "');\" class=\"button\"> 5m</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('10', '"
          + changeFormId + "');\" class=\"button\"> 10m</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('20', '"
          + changeFormId + "');\" class=\"button\"> 20m</a>,");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('40', '"
          + changeFormId + "');\" class=\"button\"> 40m</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('60', '"
          + changeFormId + "');\" class=\"button\"> 60m</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('80', '"
          + changeFormId + "');\" class=\"button\"> 80m</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('120', '"
          + changeFormId + "');\" class=\"button\"> 2h</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('240', '"
          + changeFormId + "');\" class=\"button\"> 4h</a>");
      out.println("              <a href=\"javascript: void changeNextTimeEstimate('360', '"
          + changeFormId + "');\" class=\"button\"> 6h</a>");
      out.println("            </font>");
      if (projectAction.getNextDeadline() != null) {
        out.println("<br/>Deadline:  " + sdf1.format(projectAction.getNextDeadline()));
      }
      out.println("</form></div>");
    }

    out.println("</td>");
    return changeBoxId;
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
