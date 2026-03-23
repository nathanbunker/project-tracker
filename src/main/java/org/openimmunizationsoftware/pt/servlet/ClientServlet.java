/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.SoftwareVersion;
import org.openimmunizationsoftware.pt.manager.TimeEntry;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillCodeId;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ClientServlet extends HttpServlet {

  private static final long serialVersionUID = 8641751774755499569L;

  public static String PARAM_ACTION = "action";

  public static final HashMap<String, Date> webUserLastUsedDate = new HashMap<String, Date>();
  public static String systemWideMessage = "";

  public static String getSystemWideMessage() {
    return systemWideMessage;
  }

  public static void setSystemWideMessage(String systemWideMessage) {
    ClientServlet.systemWideMessage = systemWideMessage;
  }

  protected void printHtmlHead(AppReq appReq) {
    HttpServletResponse response = appReq.getResponse();
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = appReq.getOut();
    String title = appReq.getTitle();
    TimeTracker timeTracker = appReq.getTimeTracker();
    Project projectSelected = appReq.getProjectSelected();
    ProjectActionNext completingAction = appReq.getCompletingAction();

    out.println("<html>");
    out.println("  <head>");
    out.println("   <meta charset=\"UTF-8\">");

    if (timeTracker != null) {
      if (timeTracker.isRunningClock()) {
        out.println("    <title>PT " + timeTracker.getTotalMinsBillableForDisplay() + " " + title
            + "</title>");
      } else {
        out.println("    <title>PT (idle) " + title + "</title>");
      }
    } else {
      out.println("    <title>PT " + title + "</title>");
    }

    String displayColor = appReq.getDisplayColor();
    String displaySize = appReq.getDisplaySize();

    try {
      out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"CssServlet?displaySize="
          + displaySize + "&displayColor=" + URLEncoder.encode(displayColor, "UTF-8") + "\" />");
    } catch (UnsupportedEncodingException uex) {
      uex.printStackTrace();
    }
    out.println("    <script>");
    out.println("    var refreshCount = 0;");
    out.println("    function checkRefresh()");
    out.println("    {");
    out.println("      refreshCount++;");
    out.println("      if (refreshCount > 500)");
    out.println("      {");
    String link = "HomeServlet";
    if (title.equals("Projects") && projectSelected != null) {
      link = "ProjectServlet?projectId=" + projectSelected.getProjectId();
    } else if (title.equals("Actions") && completingAction != null) {
      link = "ProjectActionServlet?" + ProjectActionServlet.PARAM_COMPLETING_ACTION_NEXT_ID + "="
          + completingAction.getActionNextId();
    }
    out.println("        window.location.href=\"" + link + "\"");
    out.println("      }");
    out.println("      else");
    out.println("      {");
    out.println("        setTimeout('checkRefresh()', 1000);");
    out.println("      }");
    out.println("    }");
    out.println("    ");
    out.println("    function resetRefresh()");
    out.println("    {");
    out.println("      refreshCount = 0;");
    out.println("    }");
    out.println("    ");
    out.println("    checkRefresh();");
    out.println("    ");
    out.println("      function toggleLayer(whichLayer) ");
    out.println("      {");
    out.println("        var elem, vis;");
    out.println("        if (document.getElementById) ");
    out.println("          elem = document.getElementById(whichLayer);");
    out.println("        else if (document.all) ");
    out.println("          elem = document.all[whichLayer] ");
    out.println("        else if (document.layers) ");
    out.println("          elem = document.layers[whichLayer]");
    out.println("        vis = elem.style;");
    out.println(
        "        if (vis.display == '' && elem.offsetWidth != undefined && elem.offsetHeight != undefined) ");
    out.println(
        "          vis.display = (elem.offsetWidth != 0 && elem.offsetHeight != 0) ? 'block' : 'none';");
    out.println(
        "        vis.display = (vis.display == '' || vis.display == 'block') ? 'none' : 'block';");
    out.println("      }");
    out.println("      function toggleVisibility(id) ");
    out.println("      {");
    out.println("        var box = document.getElementById(id);");
    out.println("        if (box.style.visibility == 'visible') { ");
    out.println("          box.style.visibility = 'hidden'; ");
    out.println("        } else {");
    out.println("          box.style.visibility = 'visible'; ");
    out.println("        }");
    out.println("      }");

    out.println("      var timeout = 5000;");
    out.println("      var closeTimer = 0;");
    out.println("      var ddBox = 0;");
    out.println("      function boxOpen(id) ");
    out.println("      { ");
    out.println("        boxCancelCloseTime(); ");
    out.println("        if (ddBox) ddBox.style.visibility = 'hidden'; ");
    out.println("        ddBox = document.getElementById(id);");
    out.println("        ddBox.style.visibility = 'visible';");
    out.println("      } ");
    out.println("      function boxClose() ");
    out.println("      {");
    out.println("        if(ddBox) ddBox.style.visibility = 'hidden'");
    out.println("      }");
    out.println("      function boxCloseTime() { ");
    out.println("        closetimer = window.setTimeout(boxClose, timeout); ");
    out.println("      }");
    out.println("      function boxCancelCloseTime() ");
    out.println("      {");
    out.println("        if (closeTimer)");
    out.println("        { ");
    out.println("          window.clearTimeout(closeTimer);");
    out.println("          closeTimer = null;");
    out.println("        }");
    out.println("      }");

    out.println("      function showHide(shID) {");
    out.println("        if (document.getElementById(shID)) {");
    out.println("          if (document.getElementById(shID).style.display != 'none') {");
    out.println("            document.getElementById(shID).style.display = 'none';");
    out.println("          }");
    out.println("          else {");
    out.println("            document.getElementById(shID).style.display = 'none';");
    out.println("          }");
    out.println("        }");
    out.println("      }");
    out.println("    </script>");
    out.println("  </head>");
    out.println("  <body>");
    out.println(makeMenu(appReq));

    {
      String message = appReq.getMessageProblem();
      if (message != null) {
        out.println("<p class=\"fail\">" + message + "</p>");
      }
    }
    {
      String message = appReq.getMessageConfirmation();
      if (message != null) {
        out.println("<p>" + message + "</p>");
      }
    }

    if (!systemWideMessage.equals("")) {
      out.println("<p class=\"fail\">" + systemWideMessage + "</p>");
    }
  }

  public static String makeMenu(AppReq appReq) {
    String title = appReq.getTitle();

    boolean loggedIn = appReq.isLoggedIn();
    TimeTracker timeTracker = appReq.getTimeTracker();
    WebUser webUser = appReq.getWebUser();

    List<String[]> menuList = new ArrayList<String[]>();

    if (loggedIn) {
      menuList.add(new String[] { Authenticate.APP_DEFAULT_HOME, "Dandelion" });
      menuList.add(new String[] { "ProjectActionServlet", "Actions" });
      menuList.add(new String[] { "BillEntriesServlet", "Time" });
      menuList.add(new String[] { "m/todo?filterSubmitted=Y&showPersonal=Y", "Mobile" });
    } else {
      menuList.add(new String[] { Authenticate.APP_DEFAULT_HOME, "Dandelion" });
      menuList.add(new String[] { "LoginServlet", "Login" });
      menuList.add(new String[] { "RegistrationServlet", "Register" });
    }
    StringBuilder result = new StringBuilder();
    result.append("    <table class=\"menu\"><tr><td>");
    for (int i = 0; i < menuList.size(); i++) {
      String[] menu = menuList.get(i);
      if (i > 0) {
        // result.append(" &bull; ");
        result.append(" ");
      }
      String styleClass = "menuLink";
      if (menu[1].equals(title)) {
        styleClass = "menuLinkSelected";
      }
      result.append("<a class=\"");
      result.append(styleClass);
      result.append("\" href=\"");
      result.append(menu[0]);
      result.append("\">");
      result.append(menu[1]);
      result.append("</a>");

    }

    result.append("</td><td class=\"right\">");
    if (loggedIn) {
      Project project = appReq.getProject();
      ProjectActionNext completingAction = appReq.getCompletingAction();
      if (project == null && completingAction != null) {
        project = completingAction.getProject();
      }
      if (project != null) {
        result.append("<a href=\"ProjectServlet?projectId=" + project.getProjectId()
            + "\" class=\"menuLink\">" + project.getProjectName() + "</a>");
      }
      if (timeTracker != null) {
        String time = timeTracker.getTotalMinsBillableForDisplay();
        int minsForWeek = 0;
        {
          Session dataSession = appReq.getDataSession();
          Date today = new Date();
          TimeTracker timeTrackerForWeek = new TimeTracker(webUser, today, Calendar.WEEK_OF_YEAR, dataSession);
          minsForWeek = calculateWeeklyRoundedMinutesLikeTrackServlet(dataSession, timeTrackerForWeek);
        }
        if (minsForWeek > 0) {
          time += " <font size=\"-1\">" + TimeTracker.formatTime(minsForWeek) + "</font>";
        }
        result.append(" <span class=\"");
        result.append(timeTracker.isRunningClock() ? "workStatusWorking\">WORKING" : "workStatusStopped\">NOT WORKING");
        result.append("</span>");
        result.append(" <a href=\"");
        result.append(createWorkToggleLink(project, completingAction, timeTracker));
        result.append("\" class=\"workToggleButton\" title=\"");
        result.append(timeTracker.isRunningClock() ? "Stop Working" : "Start Working");
        result.append("\">");
        result.append(timeTracker.isRunningClock() ? "&#9632;" : "&#9654;");
        result.append("</a>");
        result.append(" <span class=\"");
        result.append(timeTracker.isRunningClock() ? "timerRunningLabel\">" : "timerStoppedLabel\">");
        result.append(time);
        result.append("</span>");
      }
    } else {
      result.append("&nbsp;");
    }
    result.append("</td></tr></table><br>");
    return result.toString();
  }

  private static String createWorkToggleLink(Project project, ProjectActionNext completingAction,
      TimeTracker timeTracker) {
    if (timeTracker != null && timeTracker.isRunningClock()) {
      return "ProjectActionServlet?" + ProjectActionServlet.PARAM_ACTION + "=StopTimer";
    }
    StringBuilder link = new StringBuilder("ProjectActionServlet");
    boolean hasParam = false;
    if (project != null || completingAction != null) {
      link.append("?");
      if (project != null) {
        link.append("projectId=").append(project.getProjectId());
        hasParam = true;
      }
      if (completingAction != null) {
        if (hasParam) {
          link.append("&");
        }
        link.append(ProjectActionServlet.PARAM_COMPLETING_ACTION_NEXT_ID)
            .append("=")
            .append(completingAction.getActionNextId());
        hasParam = true;
      }
      if (hasParam) {
        link.append("&");
      }
      link.append(ProjectActionServlet.PARAM_ACTION).append("=")
          .append(ProjectActionServlet.ACTION_START_TIMER);
      return link.toString();
    }
    return "ProjectActionServlet";
  }

  protected void printDandelionLocation(PrintWriter out, String sectionName) {
    out.println("<table class=\"boxed\">");
    out.println("  <tr class=\"boxed\">");
    out.println("    <td class=\"boxed\"><a href=\"HomeServlet\">Dandelion</a> &raquo; "
        + n(sectionName) + "</td>");
    out.println("  </tr>");
    out.println("</table><br/>");
  }

  public static void printFooter(PrintWriter out) {
    out.println("    <p>Open Immunization Software - Dandelion - Version "
        + SoftwareVersion.VERSION + "</p>");
  }

  public static BillCode resolveBillCode(Session dataSession, ProjectProvider provider,
      String billCodeString) {
    if (provider == null || billCodeString == null || billCodeString.equals("")) {
      return null;
    }
    BillCodeId billCodeId = new BillCodeId(provider.getProviderId(), billCodeString);
    return (BillCode) dataSession.get(BillCode.class, billCodeId);
  }

  public static BillCode resolveBillCode(Session dataSession, Project project) {
    if (project == null) {
      return null;
    }
    return resolveBillCode(dataSession, project.getProvider(), project.getBillCode());
  }

  private static int calculateWeeklyRoundedMinutesLikeTrackServlet(Session dataSession,
      TimeTracker timeTrackerForWeek) {
    int totalTimeInMinutes = 0;
    HashMap<Integer, Integer> projectMap = timeTrackerForWeek.getTotalMinsForProjectMap();
    for (Integer projectId : projectMap.keySet()) {
      Project project = (Project) dataSession.get(Project.class, projectId);
      if (project == null) {
        continue;
      }
      String billCodeString = project.getBillCode();
      if (billCodeString == null) {
        continue;
      }
      BillCode billCode = resolveBillCode(dataSession, project);
      if (billCode == null || !"Y".equals(billCode.getBillable())) {
        continue;
      }
      Integer projectMinutes = projectMap.get(projectId);
      if (projectMinutes != null) {
        totalTimeInMinutes += TimeEntry.adjustMinutes(projectMinutes);
      }
    }
    return totalTimeInMinutes;
  }

  protected void printHtmlFoot(AppReq appReq) {
    PrintWriter out = appReq.getOut();
    switch (appReq.getAppType()) {
      case SENTIMENT:
        out.println("    </div>");
        out.println("  </body>");
        out.println("</html>");
        break;
      case TRACKER:
        printFooter(out);
        out.println("  </body>");
        out.println("</html>");
        break;
    }
  }

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>

  protected static String nbsp(String s) {
    if (s == null || s.equals("")) {
      return "&nbsp;";
    }
    return s;
  }

  protected static String n(String s) {
    if (s == null || s.equals("")) {
      return "";
    }
    return s;
  }

  protected static String n(Integer i) {
    if (i == null) {
      return "";
    }
    return "" + i;
  }

  protected static String n(Object value) {
    if (value == null) {
      return "";
    }
    return String.valueOf(value);
  }

  protected String addBreaks(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i = i + 20) {
      String t;
      if ((i + 20) < s.length()) {
        t = s.substring(i, i + 20);
        sb.append(t);
      } else {
        t = s.substring(i);
        sb.append(t);
        int pos = t.indexOf(" ");
        if (pos == -1) {
          sb.append(" ");
        }
      }
    }
    return sb.toString();
  }

  protected String n(String s, String d) {
    if (s == null || s.equals("")) {
      return d;
    }
    return s;
  }

  protected static String trim(String s, int length) {
    if (s == null) {
      return "";
    }
    if (s.length() < length) {
      return s;
    }
    return s.substring(0, length);
  }

  protected String trimForDisplay(String s, int length) {
    if (s == null) {
      return "";
    }
    if (s.length() < length) {
      return s;
    }
    return s.substring(0, length) + "...";
  }

  protected void forwardToHome(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
    dispatcher.forward(request, response);
  }
}
