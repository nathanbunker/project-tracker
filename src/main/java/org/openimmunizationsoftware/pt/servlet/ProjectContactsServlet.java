/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectContactsServlet extends ClientServlet
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
    if (webUser == null)
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try
    {
      Project project = (Project) session.getAttribute(SESSION_VAR_PROJECT);
      String nameFirst = request.getParameter("nameFirst");
      String nameLast = request.getParameter("nameLast");

      printHtmlHead(out, "Contacts", request);

      Session dataSession = getDataSession(session);
      Query query;

      out.println("<div class=\"main\">");
      out.println("<form action=\"ProjectContactsServlet\" method=\"GET\">");
      out.println("First <input type=\"text\" name=\"nameFirst\" value=\"" + n(nameFirst) + "\" size=\"15\">");
      out.println("Last <input type=\"text\" name=\"nameLast\" value=\"" + n(nameLast) + "\" size=\"15\">");
      out.println("<input type=\"submit\" name=\"action\" value=\"Search\" >");
      out.println("</form>");

      String action = request.getParameter("action");
      if (action != null && action.equals("Search"))
      {

        if (!nameFirst.equals("") || !nameLast.equals(""))
        {
          out.println("<table class=\"boxed\">");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"title\" colspan=\"5\">Search Results</th>");
          out.println("  </tr>");
          out.println("  <tr class=\"boxed\">");
          out.println("    <th class=\"boxed\">Name</th>");
          out.println("    <th class=\"boxed\">Phone</th>");
          out.println("    <th class=\"boxed\">Organization</th>");
          out.println("    <th class=\"boxed\">Actions</th>");
          out.println("  </tr>");
          query = dataSession
              .createQuery("from ProjectContact where providerId = ? and nameFirst like ? and nameLast like ? order by nameFirst, nameLast");
          query.setParameter(0, webUser.getProviderId());
          query.setParameter(1, nameFirst + "%");
          query.setParameter(2, nameLast + "%");

          List<ProjectContact> projectContactList = query.list();
          for (ProjectContact projectContact : projectContactList)
          {
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectContactServlet?projectContactId=" + projectContact.getContactId()
                + "\" class=\"button\">" + projectContact.getNameFirst() + " " + projectContact.getNameLast() + "</a></td>");
            out.println("    <td class=\"boxed\">" + projectContact.getNumberPhone() + "</td>");
            out.println("    <td class=\"boxed\">" + projectContact.getOrganizationName() + "</td>");
            out.println("    <td class=\"boxed\">");
            if (project != null && !project.isAssigned(projectContact))
            {
              out.println(" <font size=\"-1\"><a href=\"ProjectServlet?action=AddContact&projectId=" + project.getProjectId() + "&contactId="
                  + projectContact.getContactId() + "\" class=\"box\">Assign to " + project.getProjectName() + " </a></font>");
            }
            out.println("</td>");
            out.println("  </tr>");
          }
          out.println("</table>");
          out.println("<h2>Create a New Contact</h2>");
          out.println("<p>If you do not see your contact in the list above you can <a href=\"ProjectContactEditServlet?nameFirst=" + nameFirst
              + "&nameLast=" + nameLast + "\">create</a> one.</p>");
        }
      }
      out.println("</div>");

      printHtmlFoot(out);

    } finally
    {
      out.close();
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
