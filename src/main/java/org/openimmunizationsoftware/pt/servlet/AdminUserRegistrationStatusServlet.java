package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.WebUser;

public class AdminUserRegistrationStatusServlet extends ClientServlet {

  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      if (appReq.isLoggedOut()) {
        forwardToHome(request, response);
        return;
      }

      if (!appReq.isAdmin()) {
        forwardToHome(request, response);
        return;
      }

      Session dataSession = appReq.getDataSession();
      Query query = dataSession.createQuery(
          "from WebUser where registrationStatus in (:activeStatus, :pendingStatus) order by createdDate desc, webUserId desc");
      query.setParameter("activeStatus", WebUser.REGISTRATION_STATUS_ACTIVE);
      query.setParameter("pendingStatus", WebUser.REGISTRATION_STATUS_PENDING);
      @SuppressWarnings("unchecked")
      List<WebUser> webUsers = query.list();

      appReq.setTitle("Registered and Pending Users");
      printHtmlHead(appReq);
      PrintWriter out = appReq.getOut();
      out.println("<div class=\"main\">");
      out.println("<h1>Registered and Pending Users</h1>");
      out.println("<p>Shows users who completed registration and users who started registration.</p>");

      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"3\">User Registration Status</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <th class=\"boxed\">Email Address</th>");
      out.println("    <th class=\"boxed\">Registration Status</th>");
      out.println("  </tr>");

      if (webUsers.isEmpty()) {
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" colspan=\"3\">No users found.</td>");
        out.println("  </tr>");
      } else {
        for (WebUser listedUser : webUsers) {
          String fullName = (n(listedUser.getFirstName()) + " " + n(listedUser.getLastName())).trim();
          if (fullName.length() == 0) {
            fullName = "(Name not provided)";
          }

          out.println("  <tr class=\"boxed\">");
          out.println("    <td class=\"boxed\">" + h(fullName) + "</td>");
          out.println("    <td class=\"boxed\">" + h(n(listedUser.getEmailAddress())) + "</td>");
          out.println("    <td class=\"boxed\">" + h(n(listedUser.getRegistrationStatus())) + "</td>");
          out.println("  </tr>");
        }
      }

      out.println("</table>");
      out.println("<p><a href=\"HomeServlet\">Back to Home</a></p>");
      out.println("</div>");
      printHtmlFoot(appReq);
    } finally {
      appReq.close();
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  @Override
  public String getServletInfo() {
    return "Admin user registration status";
  }

  private static String h(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }
}
