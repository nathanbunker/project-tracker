/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openimmunizationsoftware.pt.CentralControl;
import org.openimmunizationsoftware.pt.SoftwareVersion;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ClientServlet extends HttpServlet {

  public static final String SESSION_VAR_TIME_TRACKER = "timeTracker";
  public static final String SESSION_VAR_DATA_SESSION = "dataSession";
  public static final String SESSION_VAR_WEB_USER = "webUser";
  public static final String SESSION_VAR_PROJECT_ID_LIST = "projectIdList";
  public static final String SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST =
      "projectContactAssignedList";
  public static final String SESSION_VAR_PROJECT_SELECTED_LIST = "projectSelectedList";
  public static final String SESSION_VAR_PROJECT = "project";
  public static final String SESSION_VAR_PARENT_PROJECT = "parentProject";
  public static final String SESSION_VAR_CHILD_WEB_USER_LIST = "childWebUserList";

  public static final HashMap<String, Date> webUserLastUsedDate = new HashMap<String, Date>();

  public static final String REQUEST_VAR_MESSAGE = "message";

  public static String systemWideMessage = "";

  public static String getSystemWideMessage() {
    return systemWideMessage;
  }

  public static void setSystemWideMessage(String systemWideMessage) {
    ClientServlet.systemWideMessage = systemWideMessage;
  }

  protected static void printHtmlHead(PrintWriter out, String title, HttpServletRequest request) {
    HttpSession session = request.getSession();
    TimeTracker timeTracker = null;
    Project projectTrackTime = null;
    Project projectSelected = null;
    WebUser webUser = null;
    Session dataSession = null;

    if (session != null) {
      dataSession = getDataSession(session);
      webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);
      if (webUser != null) {
        webUserLastUsedDate.put(webUser.getUsername(), new Date());
        timeTracker = (TimeTracker) session.getAttribute(SESSION_VAR_TIME_TRACKER);
        if (timeTracker != null) {
          projectTrackTime = (Project) session
              .getAttribute(webUser.getParentWebUser() == null ? SESSION_VAR_PROJECT
                  : SESSION_VAR_PARENT_PROJECT);
          if (projectTrackTime != null) {
            timeTracker.update(projectTrackTime, dataSession);
          }
        }
        projectSelected = (Project) session.getAttribute(SESSION_VAR_PROJECT);
      }
    }
    out.println("<html>");
    out.println("  <head>");

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

    String displaySize = "small";
    String displayColor = "";
    if (webUser != null) {
      displaySize = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, "small",
          webUser, getDataSession(session));
      displayColor = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, "",
          webUser, getDataSession(session));
    }
    if (request.getParameter("displayColor") != null) {
      displayColor = request.getParameter("displayColor");
    }

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
    out.println(makeMenu(request, title));

    String message = (String) request.getAttribute(REQUEST_VAR_MESSAGE);
    if (message != null) {
      out.println("<p class=\"fail\">" + message + "</p>");
    }

    if (!systemWideMessage.equals("")) {
      out.println("<p class=\"fail\">" + systemWideMessage + "</p>");
    }

  }

  public static String makeMenu(HttpServletRequest request) {
    return makeMenu(request, "&nbsp;");
  }

  public static String makeMenu(HttpServletRequest request, String title) {
    boolean loggedIn = false;
    HttpSession session = request.getSession();
    TimeTracker timeTracker = null;
    WebUser webUser = null;
    Session dataSession = null;

    if (session != null) {
      dataSession = getDataSession(session);
      webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);
      loggedIn = webUser != null;
      timeTracker = (TimeTracker) session.getAttribute(SESSION_VAR_TIME_TRACKER);
    }
    List<String[]> menuList = new ArrayList<String[]>();

    if (loggedIn) {
      if (webUser.getParentWebUser() == null) {
        menuList.add(new String[] {Authenticate.APP_DEFAULT_HOME, "Home"});
      } else {
        ProjectProvider projectProvider =
            (ProjectProvider) dataSession.get(ProjectProvider.class, webUser.getProviderId());
        menuList.add(new String[] {Authenticate.APP_DEFAULT_HOME,
            projectProvider.getProviderName() + " Home"});
      }
      menuList.add(new String[] {"ProjectsServlet", "Projects"});
      menuList.add(new String[] {"ProjectContactsServlet", "Contacts"});

      if (webUser.isUserTypeAdmin()) {
        menuList.add(new String[] {"ReportsServlet", "Reports"});
      }

      if (webUser.getParentWebUser() == null) {
        if (timeTracker != null) {
          menuList.add(new String[] {"TrackServlet", "Track"});
        }
        if (webUser.isManageBudget()) {
          menuList.add(new String[] {"BudgetServlet", "Budget"});
        }
      }
      menuList.add(new String[] {"SettingsServlet", "Settings"});
    } else {
      menuList.add(new String[] {Authenticate.APP_DEFAULT_HOME, "Home"});
      menuList.add(new String[] {"LoginServlet", "Login"});
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
      Project project = (Project) session.getAttribute(SESSION_VAR_PROJECT);
      if (webUser.getParentWebUser() == null) {
        if (project != null) {
          result.append("<a href=\"ProjectServlet?projectId=" + project.getProjectId()
              + "\" class=\"menuLink\">" + project.getProjectName() + "</a>");
          if (timeTracker != null) {
            String time = timeTracker.getTotalMinsBillableForDisplay();
            Integer mins = timeTracker.getTotalMinsForProjectMap().get(project.getProjectId());
            if (mins != null) {
              time += " <font size=\"-1\">" + TimeTracker.formatTime(mins) + "</font>";
            }
            if (timeTracker.isRunningClock()) {
              result.append("<a href=\"TrackServlet?action=StopTimer\" class=\"timerRunning\">"
                  + time + "</a>");
            } else {
              result.append("<a href=\"ProjectServlet?projectId=" + project.getProjectId()
                  + "&action=StartTimer\" class=\"timerStopped\">" + time + "</a>");
            }
          }
        }
      } else {
        Project parentProject = (Project) session.getAttribute(SESSION_VAR_PARENT_PROJECT);
        if (parentProject != null && project != null) {
          result.append("<a href=\"ProjectServlet?projectId=" + project.getProjectId()
              + "\" class=\"menuLink\">" + project.getProjectName() + " - "
              + parentProject.getProjectName() + "</a>");
          if (timeTracker != null && parentProject != null && timeTracker.isRunningClock()) {
            String time = timeTracker.getTotalMinsBillableForDisplay();
            Integer mins =
                timeTracker.getTotalMinsForProjectMap().get(parentProject.getProjectId());
            if (mins != null) {
              time += " <font size=\"-1\">" + TimeTracker.formatTime(mins) + "</font>";
            }
            result.append("<a href=\"TrackServlet?action=StopTimer\" class=\"timerRunning\">" + time
                + "</a>");
          }
        } else if (project != null) {
          result.append("<a href=\"ProjectServlet?projectId=" + project.getProjectId()
              + "\" class=\"menuLink\">" + project.getProjectName() + "</a>");
        }

      }
    } else {
      result.append("&nbsp;");
    }
    result.append("</td></tr></table><br>");
    return result.toString();
  }

  public static void printFooter(PrintWriter out) {
    SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    out.println(
        "    <p>Open Immunization Software - Project Tracker - Version " + SoftwareVersion.VERSION
            + "<br>" + "Current server time is " + sdf.format(System.currentTimeMillis()) + "</p>");

  }

  public static void printHtmlFoot(PrintWriter out) {
    printFooter(out);
    out.println("  </body>");
    out.println("</html>");
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

  public static org.hibernate.Session getDataSession(HttpSession session) {
    org.hibernate.Session dataSession =
        (org.hibernate.Session) session.getAttribute(SESSION_VAR_DATA_SESSION);
    if (dataSession == null) {
      SessionFactory factory = CentralControl.getSessionFactory();
      dataSession = factory.openSession();
      session.setAttribute(SESSION_VAR_DATA_SESSION, dataSession);
    }
    return dataSession;
  }

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

  protected String trim(String s, int length) {
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

  private static String getTimeForDisplay(int min) {
    int hour = min / 60;
    min = min % 60;
    if (min < 10) {
      return hour + ":0" + min;
    } else {
      return hour + ":" + min;
    }
  }
}
