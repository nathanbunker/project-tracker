/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillEntry;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectClient;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class BillEntriesServlet extends ClientServlet
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
    if (webUser == null || webUser.getParentWebUser() != null)
    {
      RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
      dispatcher.forward(request, response);
      return;
    }

    PrintWriter out = response.getWriter();
    try
    {
      Session dataSession = getDataSession(session);

      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      String billDateString = request.getParameter("billDate");
      Date billDate = null;
      if ((billDateString != null && billDateString.length() > 0))
      {
        try
        {
          billDate = sdf.parse(billDateString);
        } catch (ParseException pe)
        {
          request.setAttribute(REQUEST_VAR_MESSAGE, "Unable to parse date: " + pe.getMessage());
        }
      } else
      {
        billDateString = sdf.format(new Date());
      }

      printHtmlHead(out, "Track", request);

      out.println("<form action=\"BillEntriesServlet\" method=\"GET\">");
      out.println("Date");
      out.println("<input type=\"text\" name=\"billDate\" value=\"" + billDateString + "\" size=\"10\">");
      out.println("<input type=\"submit\" name=\"action\" value=\"Refresh\">");
      out.println("</form>");

      Query query;

      Calendar t = TimeTracker.createToday();
      if (billDate != null)
      {
        t.setTime(billDate);
      }
      Date today = t.getTime();
      t.add(Calendar.DAY_OF_MONTH, 1);
      Date tomorrow = t.getTime();
      query = dataSession.createQuery("from BillEntry where username = ? and startTime >= ? and startTime < ? order by startTime");
      query.setParameter(0, webUser.getUsername());
      query.setParameter(1, today);
      query.setParameter(2, tomorrow);
      List<BillEntry> billEntryList = query.list();

      out.println("<table class=\"boxed\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"7\">Bill Entries</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Bill Code</th>");
      out.println("    <th class=\"boxed\">Start</th>");
      out.println("    <th class=\"boxed\">End</th>");
      out.println("    <th class=\"boxed\">Time</th>");
      out.println("    <th class=\"boxed\">Bill</th>");
      out.println("  </tr>");
      SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm aaa");
      for (BillEntry billEntry : billEntryList)
      {
        String clientCode = billEntry.getClientCode();
        String providerId = billEntry.getProviderId();
        ProjectClient projectClient = TrackServlet.getClient(dataSession, clientCode, providerId);
        Project project = (Project) dataSession.get(Project.class, billEntry.getProjectId());
        BillCode billCode = null;
        if (billEntry.getBillCode() != null)
        {
          billCode = (BillCode) dataSession.get(BillCode.class, billEntry.getBillCode());
        }

        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">" + (projectClient != null ? projectClient.getClientName() : "") + "</td>");
        if (project != null)
        {
          out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId() + "\" class=\"button\">"
              + project.getProjectName() + "</a></td>");
        } else
        {
          out.println("    <td class=\"boxed\"></td>");
        }
        out.println("    <td class=\"boxed\">" + (billCode != null ? billCode.getBillLabel() : "") + "</td>");
        out.println("    <td class=\"boxed\">" + timeFormat.format(billEntry.getStartTime()) + "</td>");
        out.println("    <td class=\"boxed\">" + timeFormat.format(billEntry.getEndTime()) + "</td>");
        out.println("    <td class=\"boxed\"><a href=\"BillEntryEditServlet?billId=" + billEntry.getBillId() + "&billDate=" + billDateString
            + "\" class=\"button\">" + TimeTracker.formatTime(billEntry.getBillMins()) + "</a></td>");
        out.println("    <td class=\"boxed\">" + billEntry.getBillable() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table> ");
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
