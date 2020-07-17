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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.SoftwareVersion;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ClientServlet extends HttpServlet {

  private static final long serialVersionUID = 8641751774755499569L;

  public static final HashMap<String, Date> webUserLastUsedDate = new HashMap<String, Date>();
  public static String systemWideMessage = "";

  public static String getSystemWideMessage() {
    return systemWideMessage;
  }

  public static void setSystemWideMessage(String systemWideMessage) {
    ClientServlet.systemWideMessage = systemWideMessage;
  }

  protected static void printHtmlHead(AppReq appReq) {
    HttpServletResponse response = appReq.getResponse();
    response.setContentType("text/html;charset=UTF-8");

    switch (appReq.getAppType()) {
      case SENTIMENT:
        printSentimentHeader(appReq);
        break;
      case TRACKER:
        printTrackerHeader(appReq);
        break;
    }

  }

  private static void printTrackerHeader(AppReq appReq) {
    PrintWriter out = appReq.getOut();
    String title = appReq.getTitle();
    TimeTracker timeTracker = appReq.getTimeTracker();
    Project projectSelected = appReq.getProjectSelected();

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

  private static void printSentimentHeader(AppReq appReq) {
    PrintWriter out = appReq.getOut();

    out.println("<!DOCTYPE html>");
    out.println("<html lang=\"en\">");
    out.println("  <head>");
    out.println("    <title>W3.CSS Template</title>");
    out.println("    <meta charset=\"UTF-8\">");
    out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
    out.println("    <link rel=\"stylesheet\" href=\"css/w3.css\">");
    out.println(
        "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Lato\">");
    out.println(
        "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css\">");
    out.println("    <style>");
    out.println("      body {");
    out.println("        font-family: \"Lato\", sans-serif");
    out.println("      }");
    out.println("      .mySlides {");
    out.println("        display: none");
    out.println("      }");
    out.println("    </style>");
    out.println("  </head>");
    out.println("  <body>");

    // nav bar
    out.println("    <div class=\"w3-top\">");
    out.println("      <div class=\"w3-bar w3-black w3-card\">");
    out.println(
        "        <a class=\"w3-bar-item w3-button w3-padding-large w3-hide-medium w3-hide-large w3-right\"");
    out.println("          href=\"javascript:void(0)\" onclick=\"myFunction()\" ");
    out.println("          title=\"Toggle Navigation Menu\"><i class=\"fa fa-bars\"></i></a>");
    out.println(
        "        <a href=\"home\" class=\"w3-bar-item w3-button w3-padding-large\">Welcome</a>");
    out.println(
        "        <a href=\"home\" class=\"w3-bar-item w3-button w3-padding-large w3-hide-small\">My Contacts</a>");
    out.println(
        "        <a href=\"home\" class=\"w3-bar-item w3-button w3-padding-large w3-hide-small\">My Orders</a>");
    out.println(
        "        <a href=\"home\" class=\"w3-bar-item w3-button w3-padding-large w3-hide-small\">Buy Cards</a>");
    out.println(
        "        <a href=\"login\" class=\"w3-padding-large w3-hover-red w3-hide-small w3-right\">Login or Sign Up</a>");
    out.println("      </div>");
    out.println("    </div>");
    out.println("    <div class=\"w3-content\" style=\"max-width: 2000px; margin-top: 46px\">");
  }

  public static String makeMenu(AppReq appReq) {
    String title = appReq.getTitle();

    boolean loggedIn = appReq.isLoggedIn();
    TimeTracker timeTracker = appReq.getTimeTracker();
    WebUser webUser = appReq.getWebUser();

    List<String[]> menuList = new ArrayList<String[]>();

    if (loggedIn) {
      if (webUser.getParentWebUser() == null) {
        menuList.add(new String[] {Authenticate.APP_DEFAULT_HOME, "Home"});
      } else {
        ProjectProvider projectProvider = webUser.getProvider();
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
      Project project = appReq.getProject();
      if (appReq.isParentWebUser()) {
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
        Project parentProject = appReq.getParentProject();
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
    out.println("    <p>Open Immunization Software - Project Tracker - Version "
        + SoftwareVersion.VERSION + "</p>");
  }

  public static void printHtmlFoot(AppReq appReq) {
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

  protected void forwardToHome(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
    dispatcher.forward(request, response);
  }
}
