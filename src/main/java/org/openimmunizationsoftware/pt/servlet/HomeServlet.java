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
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class HomeServlet extends ClientServlet
{

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
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
  protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    response.setContentType("text/html;charset=UTF-8");
    HttpSession session = request.getSession(true);
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);

    PrintWriter out = response.getWriter();
    try
    {
      SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
      if (webUser != null)
      {
        Session dataSession = getDataSession(session);

        String action = request.getParameter("action");
        String date = request.getParameter("date");
        String nextActionType = request.getParameter("nextActionType");
        if (nextActionType == null)
        {
          nextActionType = "";
        }
        Date nextDue = null;
        if (date == null)
        {
          nextDue = new Date();
        } else
        {
          try
          {
            nextDue = sdf1.parse(date);
          } catch (ParseException pe)
          {
            nextDue = new Date();
          }
        }
        String message = null;
        if (action != null)
        {
          if (action.equals("DoToday") || action.equals("DoNextWeek") || action.equals("DoTomorrow"))
          {
            int actionId = Integer.parseInt(request.getParameter("actionId"));
            ProjectAction projectAction = (ProjectAction) dataSession.get(ProjectAction.class, actionId);
            Transaction trans = dataSession.beginTransaction();
            try
            {
              Calendar calendar = TimeTracker.createToday();
              if (action.equals("DoTomorrow"))
              {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
              } else if (action.equals("DoNextWeek"))
              {
                calendar.add(Calendar.DAY_OF_MONTH, 7);
              }
              projectAction.setNextDue(calendar.getTime());
            } finally
            {
              trans.commit();
            }
          } else if (action.equals("UpdateAction"))
          {
            int actionId = Integer.parseInt(request.getParameter("actionId"));
            ProjectAction projectAction = (ProjectAction) dataSession.get(ProjectAction.class, actionId);
            Transaction trans = dataSession.beginTransaction();
            try
            {
              Calendar calendar = TimeTracker.createToday();
              try
              {
                projectAction.setNextDue(sdf1.parse(request.getParameter("changeNextDue")));
              } catch (ParseException pe)
              {
                message = "Unable to parse next due date: " + pe.getMessage();
              }
              projectAction.setNextActionType(request.getParameter("changeNextActionType"));
            } finally
            {
              trans.commit();
            }
          } else if (action.equals("Switch"))
          {
            String username = request.getParameter("childWebUserName");
            boolean switched = false;
            if (webUser.getParentWebUser() != null && webUser.getParentWebUser().getUsername().equals(username))
            {
              webUser = webUser.getParentWebUser();
              session.setAttribute(SESSION_VAR_WEB_USER, webUser);
              Project parentProject = (Project) session.getAttribute(SESSION_VAR_PARENT_PROJECT);
              if (parentProject != null)
              {
                session.setAttribute(SESSION_VAR_PROJECT, parentProject);
              } else
              {
                session.removeAttribute(SESSION_VAR_PROJECT);
              }
              switched = true;
            } else if (!webUser.getUsername().equals(username))
            {
              List<WebUser> childWebUserList = (List<WebUser>) session.getAttribute("childWebUserList");
              if (childWebUserList != null)
              {
                for (WebUser childWebUser : childWebUserList)
                {
                  if (childWebUser.getUsername().equals(username))
                  {
                    Project project = (Project) session.getAttribute(SESSION_VAR_PROJECT);
                    if (webUser.getParentWebUser() == null && project != null)
                    {
                      session.setAttribute(SESSION_VAR_PARENT_PROJECT, project);
                    }
                    session.removeAttribute(SESSION_VAR_PROJECT);
                    webUser = childWebUser;
                    session.setAttribute(SESSION_VAR_WEB_USER, webUser);
                    switched = true;
                    break;
                  }
                }
              }
            }
            if (switched)
            {
              ProjectProvider projectProvider = (ProjectProvider) dataSession.get(ProjectProvider.class, webUser.getProviderId());
              message = "Welcome " + webUser.getProjectContact().getName() + " to " + projectProvider.getProviderName();

              session.removeAttribute(SESSION_VAR_PROJECT_ID_LIST);
              session.removeAttribute(SESSION_VAR_PROJECT_SELECTED_LIST);

            }
          }
        }

        Query query = dataSession
            .createQuery("from ProjectAction where providerId = ? and contactId = ? and nextActionId = 0 and nextDescription <> '' order by nextDue, actionDate");
        query.setParameter(0, webUser.getProviderId());
        query.setParameter(1, webUser.getContactId());
        List<ProjectAction> projectActionList = query.list();

        List<ProjectAction> projectActionListOverdue = new ArrayList<ProjectAction>();

        Date today = TimeTracker.createToday().getTime();
        for (ProjectAction projectAction : projectActionList)
        {
          projectAction.setProject((Project) dataSession.get(Project.class, projectAction.getProjectId()));
          if (projectAction.getProject() == null)
          {
            continue;
          }
          if (projectAction.getNextDue() == null || projectAction.getNextDue().before(today))
          {
            projectActionListOverdue.add(projectAction);
          }
          if (projectAction.getNextContactId() != null && projectAction.getNextContactId() > 0)
          {
            projectAction.setNextProjectContact((ProjectContact) dataSession.get(ProjectContact.class, projectAction.getNextContactId()));
          }
        }
        if (message != null)
        {
          request.setAttribute(REQUEST_VAR_MESSAGE, message);
        }
        printHtmlHead(out, "Home", request);
        out.println("<div class=\"main\">");
        if (projectActionListOverdue.size() > 0)
        {
          out.println("<table class=\"boxed\">");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"title\" colspan=\"4\">Actions Overdue</th>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Project</th>");
          out.println("    <th class=\"boxed\">Todo</th>");
          out.println("  </tr>");

          for (ProjectAction projectAction : projectActionListOverdue)
          {
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + projectAction.getProject().getProjectId()
                + "\" class=\"button\">" + projectAction.getProject().getProjectName() + "</a></td>");

            printOutAction(out, sdf1, nextActionType, nextDue, projectAction);

            out.println("  </tr>");
          }
          out.println("</table><br/>");

        }

        Calendar cIndicated = Calendar.getInstance();
        cIndicated.setTime(nextDue);
        Calendar cToday = Calendar.getInstance();
        Calendar cTomorrow = Calendar.getInstance();

        {
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
          out.println("          function changeNextActionType(nextActionType, id)");
          out.println("          {");
          out.println("            var form = document.getElementById(id);");
          out.println("            form.changeNextActionType.value = nextActionType;");
          out.println("            form.submit();");
          out.println("          }");
          out.println("        </script>");
          out.println("<form action=\"HomeServlet\" method=\"GET\" name=\"setdate\">");
          out.println("            <input type=\"hidden\" name=\"nextActionType\" value=\"" + nextActionType + "\">");
          out.println("            I: <font size=\"-1\"><a href=\"HomeServlet?nextActionType=D&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("D") ? "box" : "button") + "\"> will</a>,");
          out.println("            <a href=\"HomeServlet?nextActionType=M&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("M") ? "box" : "button") + "\">might</a>, ");
          out.println("            <a href=\"HomeServlet?nextActionType=C&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("C") ? "box" : "button") + "\">will contact</a></font>");
          out.println("            I have: ");
          out.println("            <font size=\"-1\"><a href=\"HomeServlet?nextActionType=T&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("T") ? "box" : "button") + "\">committed</a>,");
          out.println("            <a href=\"HomeServlet?nextActionType=G&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("G") ? "box" : "button") + "\">set goal</a></font>");
          out.println("            I am:");
          out.println("            <font size=\"-1\"><a href=\"HomeServlet?nextActionType=W&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("W") ? "box" : "button") + "\">waiting</a>,");
          out.println("            <a href=\"HomeServlet?nextActionType=A&date=" + sdf1.format(nextDue) + "\" class=\""
              + (nextActionType.equals("A") ? "box" : "button") + "\">asking</a></font>");
          out.println("           <br>Due <input type=\"text\" name=\"date\" value=\"" + sdf1.format(nextDue)
              + "\" size=\"10\" onchange=\"this.form.submit()\">");
          out.println("            <font size=\"-1\">");
          Calendar calendar = Calendar.getInstance();
          SimpleDateFormat day = new SimpleDateFormat("EEE");
          out.println("              <a href=\"javascript: void setNextAction('" + sdf1.format(calendar.getTime()) + "');\" class=\""
              + (sameDay(calendar, nextDue) ? "box" : "button") + "\">Today</a>");
          calendar.add(Calendar.DAY_OF_MONTH, 1);
          out.println("              <a href=\"javascript: void setNextAction('" + sdf1.format(calendar.getTime()) + "');\" class=\""
              + (sameDay(calendar, nextDue) ? "box" : "button") + "\">" + day.format(calendar.getTime()) + "</a>");
          boolean nextWeek = false;
          for (int i = 0; i < 6; i++)
          {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            if (nextWeek)
            {
              out.println("              <a href=\"javascript: void setNextAction('" + sdf1.format(calendar.getTime()) + "');\" class=\""
                  + (sameDay(calendar, nextDue) ? "box" : "button") + "\">Next-" + day.format(calendar.getTime()) + "</a>");
            } else
            {
              out.println("              <a href=\"javascript: void setNextAction('" + sdf1.format(calendar.getTime()) + "');\" class=\""
                  + (sameDay(calendar, nextDue) ? "box" : "button") + "\">" + day.format(calendar.getTime()) + "</a>");

            }
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            {
              nextWeek = true;
            }
          }
          out.println("</font>");
          out.println("</form>");
        }
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        if (sameDay(cToday, cIndicated))
        {
          out.println("    <th class=\"title\" colspan=\"3\">Due Today</th>");
        } else if (sameDay(cTomorrow, cIndicated))
        {
          out.println("    <th class=\"title\" colspan=\"3\">Due Tomorrow</th>");
        } else
        {
          out.println("    <th class=\"title\" colspan=\"3\">Due " + sdf1.format(nextDue) + "</th>");
        }
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Project</th>");
        out.println("    <th class=\"boxed\">Todo</th>");
        out.println("  </tr>");

        for (ProjectAction projectAction : projectActionList)
        {
          if (!sameDay(cIndicated, projectAction.getNextDue()))
          {
            continue;
          }
          if (!nextActionType.equals("") && !nextActionType.equals(projectAction.getNextActionType()))
          {
            continue;
          }
          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + projectAction.getProject().getProjectId()
              + "\" class=\"button\">" + projectAction.getProject().getProjectName() + "</a></td>");
          printOutAction(out, sdf1, nextActionType, nextDue, projectAction);

          out.println("  </tr>");
        }
        out.println("</table>");
        List<WebUser> childWebUserList = (List<WebUser>) session.getAttribute("childWebUserList");
        if (childWebUserList != null)
        {
          out.println("<h2>Select Provider</h2>");
          out.println("<table class=\"boxed\">");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"title\" colspan=\"1\">Choose Provider</th>");
          out.println("  </tr>");
          if (webUser.getParentWebUser() != null)
          {
            ProjectProvider projectProvider = (ProjectProvider) dataSession.get(ProjectProvider.class, webUser.getParentWebUser().getProviderId());
            out.println("  <tr class=\"boxed\">");
            String switchLink = "HomeServlet?action=Switch&childWebUserName=" + webUser.getParentWebUser().getUsername();
            String switchLabel = webUser.getParentWebUser().getUsername() + " on " + projectProvider.getProviderName();
            out.println("    <td class=\"boxed\"><a href=\"" + switchLink + "\" class=\"button\">" + switchLabel + "</a></td>");
            out.println("  </tr>");
          } else
          {
            ProjectProvider projectProvider = (ProjectProvider) dataSession.get(ProjectProvider.class, webUser.getProviderId());
            out.println("  <tr class=\"boxed\">");
            String switchLink = "HomeServlet?action=Switch&childWebUserName=" + webUser.getUsername();
            String switchLabel = webUser.getUsername() + " on " + projectProvider.getProviderName();
            out.println("    <td class=\"boxed\"><a href=\"" + switchLink + "\" class=\"button\">" + switchLabel + "</a></td>");
            out.println("  </tr>");
          }
          for (WebUser childWebUser : childWebUserList)
          {
            ProjectProvider projectProvider = (ProjectProvider) dataSession.get(ProjectProvider.class, childWebUser.getProviderId());
            out.println("  <tr class=\"boxed\">");
            String switchLink = "HomeServlet?action=Switch&childWebUserName=" + childWebUser.getUsername();
            String switchLabel = childWebUser.getUsername() + " on " + projectProvider.getProviderName();
            out.println("    <td class=\"boxed\"><a href=\"" + switchLink + "\" class=\"button\">" + switchLabel + "</a></td>");
            out.println("  </tr>");
          }
        }
        out.println("</table>");
        out.println("<h2>Logout</h2>");
        out.println("<p>If you are finished you can <a href=\"LoginServlet?action=Logout\">logout</a>.</p>");
        out.println("</div>");
        printHtmlFoot(out);
      } else
      {
        printHtmlHead(out, "Home", request);

        out.println("<h1>Project Tracker</h1>");
        printHtmlFoot(out);
      }

    } finally
    {
      out.close();
    }
  }

  private String printOutAction(PrintWriter out, SimpleDateFormat sdf1, String nextActionType, Date nextDue, ProjectAction projectAction)
  {
    String changeBoxId = "changeBox" + projectAction.getActionId();
    out.println("    <td class=\"boxed\"><a href=\"javascript: void toggleLayer('" + changeBoxId + "'); \" class=\"button\">"
        + projectAction.getNextDescriptionForDisplay() + "</a>");
    Calendar today = TimeTracker.createToday();
    if (projectAction.getNextDue() != null && projectAction.getNextDue().before(today.getTime()))
    {
      today.add(Calendar.DAY_OF_MONTH, -1);
      if (!projectAction.getNextDue().before(today.getTime()))
      {
        out.println("    <span class=\"fail\">Due Yesterday</span>");
      } else
      {
        out.println("    <span class=\"fail\">Due " + sdf1.format(projectAction.getNextDue()) + "</span>");
      }
    }

    out.println("<div class=\"editAction\" id=\"" + changeBoxId + "\">");
    String changeFormId = "changeForm" + projectAction.getActionId();
    out.println("        <form action=\"HomeServlet\" method=\"GET\" id=\"" + changeFormId + "\">");
    out.println("            <input type=\"hidden\" name=\"action\" value=\"UpdateAction\">");
    out.println("            <input type=\"hidden\" name=\"actionId\" value=\"" + projectAction.getActionId() + "\">");
    out.println("            <input type=\"hidden\" name=\"changeNextActionType\" value=\"" + projectAction.getNextActionType() + "\">");
    out.println("            <input type=\"hidden\" name=\"nextActionType\" value=\"" + nextActionType + "\">");
    out.println("            <input type=\"hidden\" name=\"date\" value=\"" + sdf1.format(nextDue) + "\">");
    out.println("            I: <font size=\"-1\"><a href=\"javascript: void changeNextActionType('D', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("D") ? "box" : "button") + "\"> will</a>,");
    out.println("            <a href=\"javascript: void changeNextActionType('M', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("M") ? "box" : "button") + "\">might</a>, ");
    out.println("            <a href=\"javascript: void changeNextActionType('C', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("C") ? "box" : "button") + "\">will contact</a></font>");
    out.println("            I have: ");
    out.println("            <font size=\"-1\"><a href=\"javascript: void changeNextActionType('T', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("T") ? "box" : "button") + "\">committed</a>,");
    out.println("            <a href=\"javascript: void changeNextActionType('G', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("G") ? "box" : "button") + "\">set goal</a></font>");
    out.println("            I am:");
    out.println("            <font size=\"-1\"><a href=\"javascript: void changeNextActionType('W', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("W") ? "box" : "button") + "\">waiting</a>,");
    out.println("            <a href=\"javascript: void setNextActionType('A', '" + changeFormId + "'); \" class=\""
        + (projectAction.getNextActionType().equals("A") ? "box" : "button") + "\">asking</a></font>");
    out.println("           <br> Due <input type=\"text\" name=\"changeNextDue\" value=\"" + sdf1.format(projectAction.getNextDue())
        + "\" size=\"10\" onchange=\"this.form.submit()\">");
    out.println("            <font size=\"-1\">");
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat day = new SimpleDateFormat("EEE");
    out.println("              <a href=\"javascript: void changeNextAction('" + sdf1.format(calendar.getTime()) + "', '" + changeFormId
        + "');\" class=\"" + (sameDay(calendar, projectAction.getNextDue()) ? "box" : "button") + "\">Today</a>");
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    out.println("              <a href=\"javascript: void changeNextAction('" + sdf1.format(calendar.getTime()) + "', '" + changeFormId
        + "');\" class=\"" + (sameDay(calendar, projectAction.getNextDue()) ? "box" : "button") + "\">" + day.format(calendar.getTime()) + "</a>");
    boolean nextWeek = false;
    for (int i = 0; i < 6; i++)
    {
      calendar.add(Calendar.DAY_OF_MONTH, 1);
      if (nextWeek)
      {
        out.println("              <a href=\"javascript: void changeNextAction('" + sdf1.format(calendar.getTime()) + "', '" + changeFormId
            + "');\" class=\"" + (sameDay(calendar, projectAction.getNextDue()) ? "box" : "button") + "\">Next-" + day.format(calendar.getTime())
            + "</a>");
      } else
      {
        out.println("              <a href=\"javascript: void changeNextAction('" + sdf1.format(calendar.getTime()) + "', '" + changeFormId
            + "');\" class=\"" + (sameDay(calendar, projectAction.getNextDue()) ? "box" : "button") + "\">" + day.format(calendar.getTime()) + "</a>");

      }
      if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
      {
        nextWeek = true;
      }
    }
    out.println("</font>");
    out.println("</form></div>");

    out.println("</td>");
    return changeBoxId;
  }

  private boolean sameDay(Calendar c1, Date d)
  {
    Calendar c2 = Calendar.getInstance();
    c2.setTime(d);
    boolean s = sameDay(c1, c2);
    return s;
  }

  private boolean sameDay(Calendar c1, Calendar c2)
  {
    return c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "DQA Tester Home Page";
  }// </editor-fold>
}
