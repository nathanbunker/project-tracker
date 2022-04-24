/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;


import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ContactEvent;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectContactEditServlet extends ClientServlet {

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
  @SuppressWarnings("unchecked")
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


      ProjectContact projectContact;

      if (request.getParameter("projectContactId") == null
          || request.getParameter("projectContactId").equals("")) {
        projectContact = new ProjectContact();
        projectContact.setProvider(webUser.getProvider());
        projectContact.setEmailAlert("Y");
        projectContact.setNameFirst(n(request.getParameter("nameFirst")));
        projectContact.setNameLast(n(request.getParameter("nameLast")));
      } else {
        int projectContactId = Integer.parseInt(request.getParameter("projectContactId"));
        Query query = dataSession.createQuery("from ProjectContact where contactId = ? ");
        query.setParameter(0, projectContactId);
        projectContact = ((List<ProjectContact>) query.list()).get(0);
      }

      if (action != null) {
        String message = null;
        if (action.equals("Save")) {
          projectContact.setNameLast(trim(request.getParameter("nameLast"), 60));
          projectContact.setNameFirst(trim(request.getParameter("nameFirst"), 60));
          projectContact.setOrganizationName(trim(request.getParameter("organizationName"), 90));
          projectContact.setDepartmentName(trim(request.getParameter("departmentName"), 90));
          projectContact.setPositionTitle(trim(request.getParameter("positionTitle"), 90));
          projectContact.setPhoneNumber(trim(request.getParameter("numberPhone"), 30));
          projectContact.setEmailAddress(trim(request.getParameter("email"), 60));
          projectContact.setContactInfo(trim(request.getParameter("contactInfo"), 1500));
          if (projectContact.getNameFirst().equals("")) {
            message = "First name is required";
          } else if (projectContact.getNameLast().equals("")) {
            message = "Last name is required";
          }
          if (message != null) {
            appReq.setMessageProblem(message);
          } else {
            Transaction trans = dataSession.beginTransaction();
            try {
              dataSession.saveOrUpdate(projectContact);
            } finally {
              trans.commit();
            }
            response.sendRedirect(
                "ProjectContactServlet?projectContactId=" + projectContact.getContactId());
            return;
          }
        } else if (action.equals("Save Event")) {
          int eventId = Integer.parseInt(request.getParameter("eventId"));
          ContactEvent contactEvent;
          if (eventId == 0) {
            contactEvent = new ContactEvent();
            contactEvent.setProjectContact(projectContact);
          } else {
            contactEvent = (ContactEvent) dataSession.get(ContactEvent.class, eventId);
          }
          contactEvent.setEventType(request.getParameter("eventType"));
          contactEvent.setEventNum(Integer.parseInt(request.getParameter("eventNum")));
          try {
            contactEvent.setEventDate(sdf.parse(request.getParameter("eventDate")));
          } catch (ParseException pe) {
            message = "Unable to parse event date: " + pe.getMessage();
          }
          if (message != null) {
            appReq.setMessageProblem(message);
          } else {
            Transaction trans = dataSession.beginTransaction();
            try {
              dataSession.saveOrUpdate(contactEvent);
            } finally {
              trans.commit();
            }
            response.sendRedirect(
                "ProjectContactServlet?projectContactId=" + projectContact.getContactId());
            return;
          }
        }
      }

      appReq.setTitle("Contacts");
      printHtmlHead(appReq);

      out.println("<form action=\"ProjectContactEditServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"projectContactId\" value=\""
          + n(request.getParameter("projectContactId")) + "\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th  class=\"title\" colspan=\"2\">Edit Contact</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"nameFirst\" value=\""
          + n(projectContact.getNameFirst())
          + "\" size=\"15\"> <input type=\"text\" name=\"nameLast\" value=\""
          + n(projectContact.getNameLast()) + "\" size=\"15\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Organization</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"organizationName\" value=\""
          + n(projectContact.getOrganizationName()) + "\" size=\"15\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Department</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"departmentName\" value=\""
          + n(projectContact.getDepartmentName()) + "\" size=\"40\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Position</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"positionTitle\" value=\""
          + n(projectContact.getPositionTitle()) + "\" size=\"40\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Email</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"email\" value=\""
          + n(projectContact.getEmailAddress()) + "\" size=\"30\"></td>");
      out.println("  <tr class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Phone</th>");
      out.println("    <td class=\"boxed\"><input type=\"text\" name=\"numberPhone\" value=\""
          + n(projectContact.getPhoneNumber()) + "\" size=\"15\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Time Zone</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"timeZone\">");
      out.println("        <option value=\"\" selected></option>");
      for (String tz : TimeZone.getAvailableIDs()) {
        if (tz.equals(projectContact.getTimeZone())) {
          out.println("        <option value=\"" + tz + "\" selected>" + tz + "</option>");
        } else {
          out.println("        <option value=\"" + tz + "\">" + tz + "</option>");
        }
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");

      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Other Info</th>");
      out.println("    <td class=\"boxed\"><textarea name=\"contactInfo\" cols=\"30\" rows=\"5\">"
          + n(projectContact.getContactInfo()) + "</textarea></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");
      out.println("<br/>");

      Query query = dataSession.createQuery("from ContactEvent where projectContact = ?");
      query.setParameter(0, projectContact);
      List<ContactEvent> contactEventList = query.list();
      for (ContactEvent contactEvent : contactEventList) {
        out.println("<form action=\"ProjectContactEditServlet\" method=\"POST\">");
        out.println("<input type=\"hidden\" name=\"projectContactId\" value=\""
            + n(request.getParameter("projectContactId")) + "\">");
        out.println(
            "<input type=\"hidden\" name=\"eventId\" value=\"" + contactEvent.getEventId() + "\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("     <th  class=\"title\" colspan=\"2\">Edit Contact Event</td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Type</th>");
        out.println("    <td class=\"boxed\">");
        out.println("      <select name=\"eventType\">");
        for (String eventType : ContactEvent.EVENT_TYPE) {
          if (eventType.equals(contactEvent.getEventType())) {
            out.println("        <option value=\"" + eventType + "\" selected>"
                + ContactEvent.getEventTypeLabel(eventType) + "</option>");
          } else {
            out.println("        <option value=\"" + eventType + "\">"
                + ContactEvent.getEventTypeLabel(eventType) + "</option>");
          }
        }
        out.println("      </select>");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Event Date</th>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"eventDate\" value=\""
            + sdf.format(contactEvent.getEventDate()) + "\" size=\"10\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Event Num</th>");
        out.println("    <td class=\"boxed\"><input type=\"text\" name=\"eventNum\" value=\""
            + contactEvent.getEventNum() + "\" size=\"5\"></td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println(
            "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save Event\"></td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("</form>");
        out.println("<br/>");
      }
      out.println("<form action=\"ProjectContactEditServlet\" method=\"POST\">");
      out.println("<input type=\"hidden\" name=\"projectContactId\" value=\""
          + n(request.getParameter("projectContactId")) + "\">");
      out.println("<input type=\"hidden\" name=\"eventId\" value=\"0\">");
      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("     <th  class=\"title\" colspan=\"2\">New Contact Event</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Type</th>");
      out.println("    <td class=\"boxed\">");
      out.println("      <select name=\"eventType\">");
      for (String eventType : ContactEvent.EVENT_TYPE) {
        out.println("        <option value=\"" + eventType + "\">"
            + ContactEvent.getEventTypeLabel(eventType) + "</option>");
      }
      out.println("      </select>");
      out.println("    </td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Event Date</th>");
      out.println(
          "    <td class=\"boxed\"><input type=\"text\" name=\"eventDate\" value=\"\" size=\"10\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Event Num</th>");
      out.println(
          "    <td class=\"boxed\"><input type=\"text\" name=\"eventNum\" value=\"0\" size=\"5\"></td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println(
          "    <td class=\"boxed-submit\" colspan=\"2\"><input type=\"submit\" name=\"action\" value=\"Save Event\"></td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</form>");
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
