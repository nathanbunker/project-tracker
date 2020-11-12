/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
@SuppressWarnings("serial")
public class ProjectTodoServlet extends ProjectServlet {

  protected static final String PARAM_ACTION_ID = "actionId";

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
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

      int actionId = Integer.parseInt(request.getParameter(PARAM_ACTION_ID));

      ProjectAction projectAction = (ProjectAction) dataSession.get(ProjectAction.class, actionId);


      Project project = projectAction.getProject();

      List<Project> projectSelectedList = setupProjectList(appReq, project);

      TimeTracker timeTracker = appReq.getTimeTracker();



      ProjectContactAssigned projectContactAssignedForThisUser =
          getProjectContactAssigned(webUser, dataSession, project);

      if (action != null) {
      }

      appReq.setTitle("Projects");
      printHtmlHead(appReq);
      
      if (false) {

      out.println(
          "<form name=\"projectAction\" method=\"post\" action=\"ProjectTodoServlet\" id=\"saveProjectActionForm\">");
      out.println("<input type=\"hidden\" name=\"" + PARAM_ACTION_ID + "\" value=\""
          + projectAction.getActionId() + "\">");
      {

        ProjectContact projectContact =
            (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId());
        projectAction.setContact(projectContact);
        ProjectContact nextProjectContact = null;
        if (projectAction.getNextContactId() != null && projectAction.getNextContactId() > 0) {
          nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class,
              projectAction.getNextContactId());
          projectAction.setNextProjectContact(nextProjectContact);
        }
        out.println("<h2>" + projectAction.getNextDescriptionForDisplay(webUser.getProjectContact())
            + "</h2>");

      }

      out.println("      <input type=\"submit\" name=\"action\" value=\"Save\">");
      out.println("</form>");
      }
      else {
        out.println("Hello!");
      }

      printHtmlFoot(appReq);

    } finally {
      appReq.close();
    }
  }

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
