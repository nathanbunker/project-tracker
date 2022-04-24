/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectContactsServlet extends ClientServlet {

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
      SimpleDateFormat sdf = webUser.getDateFormat();

      Project project = appReq.getProject();
      String nameFirst = request.getParameter("nameFirst");
      String nameLast = request.getParameter("nameLast");

      appReq.setTitle("Contacts");
      printHtmlHead(appReq);

      Query query;

      out.println("<div class=\"main\">");
      out.println("<form action=\"ProjectContactsServlet\" method=\"GET\">");
      out.println("First <input type=\"text\" name=\"nameFirst\" value=\"" + n(nameFirst)
          + "\" size=\"15\">");
      out.println(
          "Last <input type=\"text\" name=\"nameLast\" value=\"" + n(nameLast) + "\" size=\"15\">");
      out.println("<input type=\"submit\" name=\"action\" value=\"Search\" >");
      out.println("</form>");

      if (action != null && action.equals("Search")) {

        if (!nameFirst.equals("") || !nameLast.equals("")) {
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
          query = dataSession.createQuery(
              "from ProjectContact where provider = :provider and nameFirst like :nameFirst and nameLast like :nameLast "
                  + "order by nameFirst, nameLast");
          query.setParameter("provider", webUser.getProvider());
          query.setParameter("nameFirst", nameFirst + "%");
          query.setParameter("nameLast", nameLast + "%");

          List<ProjectContact> projectContactList = query.list();
          for (ProjectContact projectContact : projectContactList) {
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><a href=\"ProjectContactServlet?projectContactId="
                + projectContact.getContactId() + "\" class=\"button\">"
                + projectContact.getNameFirst() + " " + projectContact.getNameLast() + "</a></td>");
            out.println("    <td class=\"boxed\">" + projectContact.getPhoneNumber() + "</td>");
            out.println(
                "    <td class=\"boxed\">" + projectContact.getOrganizationName() + "</td>");
            out.println("    <td class=\"boxed\">");
            if (project != null && !project.isAssigned(projectContact)) {
              out.println(" <font size=\"-1\"><a href=\"ProjectServlet?action=AddContact&projectId="
                  + project.getProjectId() + "&contactId=" + projectContact.getContactId()
                  + "\" class=\"box\">Assign to " + project.getProjectName() + " </a></font>");
            }
            out.println("</td>");
            out.println("  </tr>");
          }
          out.println("</table>");
          out.println("<h2>Create a New Contact</h2>");
          out.println(
              "<p>If you do not see your contact in the list above you can <a href=\"ProjectContactEditServlet?nameFirst="
                  + nameFirst + "&nameLast=" + nameLast + "\">create</a> one.</p>");
        }
      }
      out.println("</div>");

      printHtmlFoot(appReq);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
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

}
