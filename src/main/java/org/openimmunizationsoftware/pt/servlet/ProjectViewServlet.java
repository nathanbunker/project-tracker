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
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectProvider;

/**
 * 
 * @author nathan
 */
public class ProjectViewServlet extends ClientServlet
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
   
    PrintWriter out = response.getWriter();
    try
    {
      int projectId = Integer.parseInt(request.getParameter("projectId"));
      String providerName = request.getParameter("providerName");

      Session dataSession = getDataSession(session);

      Query query = dataSession.createQuery("from Project where projectId = ? ");
      query.setParameter(0, projectId);
      Project project = ((List<Project>) query.list()).get(0);
      ProjectsServlet.loadProjectsObject(dataSession, project);
      
      ProjectProvider projectProvider = (ProjectProvider) dataSession.get(ProjectProvider.class, project.getProviderId());
      
      if (projectProvider == null || !projectProvider.getProviderName().equals(providerName))
      {
        RequestDispatcher dispatcher = request.getRequestDispatcher("HomeServlet");
        dispatcher.forward(request, response);
        return;
      }

      printHtmlHead(out, "Projects", request);

      out.println("<div class=\"main\">");

      out.println("<div id=\"projectInfo\">");
      out.println("<table class=\"boxed-fill\">");
      out.println("  <tr>");
      out.println("    <th class=\"title\" colspan=\"2\">Project Information</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project Name</th>");
      out.println("    <td class=\"boxed\">" + project.getProjectName() + "</td>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <td class=\"boxed\">" + (project.getProjectClient() != null ? project.getProjectClient().getClientName() : "") + "</td>");
      out.println("  </tr>");
      if (project.getVendorName() != null && !project.getVendorName().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Vendor</th>");
        out.println("    <td class=\"boxed\">" + project.getVendorName() + "</td>");
        out.println("  </tr>");
      }
      if (project.getSystemName() != null && !project.getSystemName().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">System</th>");
        out.println("    <td class=\"boxed\">" + n(project.getSystemName()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getDescription() != null && !project.getDescription().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Description</th>");
        out.println("    <td class=\"boxed\">" + addBreaks(project.getDescription()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getIisSubmissionCode() != null && !project.getIisSubmissionCode().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">IIS Sub Code</th>");
        out.println("    <td class=\"boxed\">" + n(project.getIisSubmissionCode()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getIisFacilityId() != null && !project.getIisFacilityId().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">IIS Facility Id</th>");
        out.println("    <td class=\"boxed\">" + n(project.getIisFacilityId()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getMedicalOrganization() != null && !project.getMedicalOrganization().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Medical Org</th>");
        out.println("    <td class=\"boxed\">" + n(project.getMedicalOrganization()) + "</td>");
        out.println("  </tr>");
      }
      if (project.getIisRegionCode() != null && !project.getIisRegionCode().equals(""))
      {
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">IIS Region Code</th>");
        out.println("    <td class=\"boxed\">" + n(project.getIisRegionCode()) + "</td>");
        out.println("  </tr>");
      }
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Phase</th>");
      out.println("    <td class=\"boxed\">" + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "") + "</td>");
      out.println("  </tr>");
      out.println("</table>");
      out.println("</div>");

      out.println("<br>");

      out.println("<table class=\"boxed-fill\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"title\" colspan=\"5\">Contacts Assigned</th>");
      out.println("  </tr>");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <th class=\"boxed\">Organization</th>");
      out.println("    <th class=\"boxed\">Phone</th>");
      out.println("    <th class=\"boxed\">Email</th>");
      out.println("  </tr>");
      query = dataSession.createQuery("from ProjectContactAssigned where id.projectId = ?");
      query.setParameter(0, projectId);
      List<ProjectContactAssigned> projectContactAssignedList = query.list();
      List<ProjectContact> projectContactList = new ArrayList<ProjectContact>();
      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList)
      {
        query = dataSession.createQuery("from ProjectContact where contactId = ?");
        query.setParameter(0, projectContactAssigned.getId().getContactId());
        ProjectContact projectContact = ((List<ProjectContact>) query.list()).get(0);
        projectContactList.add(projectContact);
        projectContactAssigned.setProjectContact(projectContact);
        projectContactAssigned.setProject(project);
      }
      Collections.sort(projectContactAssignedList, new Comparator<ProjectContactAssigned>() {
        public int compare(ProjectContactAssigned arg0, ProjectContactAssigned arg1)
        {
          if (arg0.getProjectContact().getNameFirst().equals(arg1.getProjectContact().getNameFirst()))
          {
            return arg0.getProjectContact().getNameLast().compareTo(arg1.getProjectContact().getNameLast());
          }
          return arg0.getProjectContact().getNameFirst().compareTo(arg1.getProjectContact().getNameFirst());
        }

      });
      project.setProjectContactAssignedList(projectContactAssignedList);

      for (ProjectContactAssigned projectContactAssigned : projectContactAssignedList)
      {
        ProjectContact projectContact = projectContactAssigned.getProjectContact();
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"ProjectContactServlet?projectContactId=" + projectContact.getContactId()
            + "\" class=\"button\">" + projectContact.getName() + "</a></td>");
        out.println("    <td class=\"boxed\">" + n(projectContact.getOrganizationName()) + "</td>");
        out.println("    <td class=\"boxed\">" + n(projectContact.getNumberPhone()) + "</td>");
        out.println("    <td class=\"boxed\">");
        if (projectContact.getEmail() != null && !projectContact.getEmail().equals(""))
        {
          String projectName = project.getProjectName();
          out.println(" <a href=\"mailto:" + projectContact.getEmail() + "?subject=" + encode(projectName + " Project")
              + "\">" + projectContact.getEmail() + "</a>");
        }
        out.println("</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("</div>");
      out.println("<div id=\"takeAction\">");

      query = dataSession.createQuery("from ProjectAction where projectId = ? and nextDescription <> '' and nextActionId = 0 order by nextDue asc");
      query.setParameter(0, projectId);
      List<ProjectAction> projectActionList = query.list();

      out.println("<input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\">");
      out.println("  <table class=\"boxed\" width=\"100%\">");
      out.println("    <tr>");
      out.println("      <th class=\"title\">Todo List</th>");
      out.println("    </tr>");
      out.println("    <td class=\"outside\">");
      if (projectActionList.size() > 0)
      {
        out.println("<table class=\"inside\" width=\"100%\">");
        out.println("  <tr>");
        out.println("    <th class=\"inside\">Date</th>");
        out.println("    <th class=\"inside\">Name</th>");
        out.println("    <th class=\"inside\">To Do</th>");
        out.println("  </tr>");
        SimpleDateFormat sdf11 = new SimpleDateFormat("MM/dd/yyyy");
        for (ProjectAction projectAction1 : projectActionList)
        {

          ProjectContact projectContact1 = (ProjectContact) dataSession.get(ProjectContact.class, projectAction1.getContactId());
          ProjectContact nextProjectContact = null;
          if (projectAction1.getNextContactId() != null && projectAction1.getNextContactId() > 0)
          {
            nextProjectContact = (ProjectContact) dataSession.get(ProjectContact.class, projectAction1.getNextContactId());
            projectAction1.setNextProjectContact(nextProjectContact);
          }
          out.println("  <tr>");
          if (projectAction1.getNextDue() != null)
          {
            out.println("    <td class=\"inside\">" + sdf11.format(projectAction1.getNextDue()) + "</td>");
          } else
          {
            out.println("    <td class=\"inside\">&nbsp;</td>");
          }
          out.println("    <td class=\"inside\">" + projectContact1.getNameFirst() + " " + projectContact1.getNameLast() + "</td>");

          out.println("    <td class=\"inside\">" + projectAction1.getNextDescriptionForDisplay() + "</td>");
          out.println("  </tr>");
        }
        out.println("</table>");
      } else
      {
        out.println("<i>no items</i>");
      }

      out.println("    </td>");
      out.println("  </tr>");


      out.println("</table>");
      out.println("</div>");

      out.println("<div class=\"main\">");
      out.println("<h2>Actions Taken</h2>");
      out.println("<table class=\"boxed-fill\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Date &amp; Time</th>");
      out.println("    <th class=\"boxed\">Name</th>");
      out.println("    <th class=\"boxed\">Action</th>");
      out.println("  </tr>");
      query = dataSession.createQuery("from ProjectAction where projectId = ? and actionDescription <> '' order by actionDate desc");
      query.setParameter(0, projectId);
      projectActionList = query.list();
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aaa");
      for (ProjectAction projectAction : projectActionList)
      {
        ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class, projectAction.getContactId());
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">" + sdf.format(projectAction.getActionDate()) + "</td>");
        out.println("    <td class=\"boxed\">" + projectContact.getNameFirst() + " " + projectContact.getNameLast() + "</td>");
        out.println("    <td class=\"boxed\">" + nbsp(projectAction.getActionDescription()));
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("</div>");

      printHtmlFoot(out);

    } finally
    {
      out.close();
    }
  }


  private String encode(String s) throws UnsupportedEncodingException
  {
    s = URLEncoder.encode(s, "UTF-8");
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray())
    {
      if (c == '+')
      {
        sb.append("%20");
      } else
      {
        sb.append(c);
      }
    }
    return sb.toString();
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

  /**
   * Returns a short description of the servlet.
   * 
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "DQA Tester Home Page";
  }// </editor-fold>

 

}
