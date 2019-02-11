/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class LoginServlet extends ClientServlet
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
    HttpSession session = request.getSession(true);
    String action = request.getParameter("action");
    boolean loginSuccess = false;
    if (action != null)
    {
      if (action.equals("Login"))
      {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String message = null;
        if (username != null && username.length() > 0 && password != null && password.length() > 0)
        {
          Session dataSession = getDataSession(session);
          Query query = dataSession.createQuery("from WebUser where username = ? and password = ?");
          query.setParameter(0, username);
          query.setParameter(1, password);
          List<WebUser> webUserList = query.list();

          if (webUserList.size() > 0)
          {
            WebUser webUser = webUserList.get(0);
            session.setAttribute(SESSION_VAR_WEB_USER, webUser);
            loginSuccess = true;

            ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class, webUser.getContactId());
            webUser.setProjectContact(projectContact);

            webUser.setTrackTime(TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_TRACK_TIME, "N", webUser, dataSession).equalsIgnoreCase("Y"));
            webUser.setManageBudget(TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_MANAGE_BUDGET, "N", webUser, dataSession).equalsIgnoreCase(
                "Y"));

            query = dataSession.createQuery("from WebUser where parentWebUser = ? order by username");
            query.setParameter(0, webUser);
            List<WebUser> childWebUserList = query.list();
            if (childWebUserList.size() > 0)
            {
              for (WebUser childWebUser : childWebUserList)
              {
                projectContact = (ProjectContact) dataSession.get(ProjectContact.class, childWebUser.getContactId());
                childWebUser.setProjectContact(projectContact);
              }
              session.setAttribute("childWebUserList", childWebUserList);
            }

            message = "Welcome " + projectContact.getNameFirst() + " " + projectContact.getNameLast();

            if (webUser.isTrackTime())
            {
              TimeTracker timeTracker = new TimeTracker(webUser, dataSession);
              session.setAttribute(SESSION_VAR_TIME_TRACKER, timeTracker);
            }

          } else
          {
            message = "Invalid username or password";
          }
        } else
        {
          message = "Username and password required";
        }
        if (message != null)
        {
          request.setAttribute(REQUEST_VAR_MESSAGE, message);
        }
      } else if (action.equals("Logout"))
      {

        session.invalidate();
        session = request.getSession(true);
      }
    }
    WebUser webUser = (WebUser) session.getAttribute(SESSION_VAR_WEB_USER);

    if (!loginSuccess)
    {
      String username = request.getParameter("username");
      if (username == null)
      {
        username = "";
      }
      String password = request.getParameter("password");
      if (password == null)
      {
        password = "";
      }
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter out = response.getWriter();
      try
      {
        printHtmlHead(out, "Login", request);
        out.println("<form action=\"LoginServlet\" method=\"POST\">");
        out.println("<table>");
        out.println("  <tr>");
        out.println("    <td>Username</td>");
        out.println("    <td><input type=\"text\" name=\"username\" value=\"" + username + "\"></td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <td>Password</td>");
        out.println("    <td><input type=\"password\" name=\"password\" value=\"" + password + "\"></td>");
        out.println("  </tr>");
        out.println("  <tr>");
        out.println("    <td colspan=\"2\" align=\"right\"><input type=\"submit\" name=\"action\" value=\"Login\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("<p>&nbsp;</p>");

        out.println("</form>");
        printHtmlFoot(out);
      } finally
      {
        out.close();
      }
    } else
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }
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

}
