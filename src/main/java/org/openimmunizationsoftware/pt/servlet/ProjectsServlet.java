/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectClient;
import org.openimmunizationsoftware.pt.model.ProjectPhase;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class ProjectsServlet extends ClientServlet
{

  private static final String MEDICAL_ORGANIZATION = "Medical Organization";
  private static final String IIS_FACILITY_ID = "IIS Facility Id";
  private static final String IIS_SUBMISSION_CODE = "IIS Submission Code";
  private static final String IIS_REGION_CODE = "IIS Region Code";
  private static final String DESCRIPTION = "Description";
  private static final String SYSTEM_NAME = "System Name";
  private static final String VENDOR_NAME = "Vendor Name";
  private static final String PROVIDER_NAME = "Provider Name";
  private static final String PROJECT_NAME = "Project Name";
  private static final String NOT_CLOSED = "NOT_CLOSED";
  private static final String ALL = "ALL";

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
      Session dataSession = getDataSession(session);
      printHtmlHead(out, "Projects", request);
      String clientCode = n(request.getParameter("clientCode"));
      String phaseCode = n(request.getParameter("phaseCode"), NOT_CLOSED);
      String searchField = n(request.getParameter("searchField"), PROJECT_NAME);

      out.println("<form action=\"ProjectsServlet\" method=\"GET\">");
      out.println("Search ");
      out.println("<select name=\"searchField\">");
      out.println("  <option value=\"" + PROJECT_NAME + "\"" + (searchField.equals(PROJECT_NAME) ? " selected" : "") + ">Project Name</option>");
      out.println("  <option value=\"" + PROVIDER_NAME + "\"" + (searchField.equals(PROVIDER_NAME) ? " selected" : "") + ">Provider Name</option>");
      out.println("  <option value=\"" + VENDOR_NAME + "\"" + (searchField.equals(VENDOR_NAME) ? " selected" : "") + ">Vendor Name</option>");
      out.println("  <option value=\"" + SYSTEM_NAME + "\"" + (searchField.equals(SYSTEM_NAME) ? " selected" : "") + ">System Name</option>");
      out.println("  <option value=\"" + DESCRIPTION + "\"" + (searchField.equals(DESCRIPTION) ? " selected" : "") + ">Description</option>");
      out.println("  <option value=\"" + IIS_SUBMISSION_CODE + "\"" + (searchField.equals(IIS_SUBMISSION_CODE) ? " selected" : "")
          + ">IIS Submission Code</option>");
      out.println("  <option value=\"" + IIS_FACILITY_ID + "\"" + (searchField.equals(IIS_FACILITY_ID) ? " selected" : "")
          + ">IIS Facility Id</option>");
      out.println("  <option value=\"" + MEDICAL_ORGANIZATION + "\"" + (searchField.equals(MEDICAL_ORGANIZATION) ? " selected" : "")
          + ">Medical Organization</option>");
      out.println("</select>");
      out.println("for <input type=\"text\" name=\"searchText\" value=\"" + n(request.getParameter("searchText")) + "\" size=\"15\" >");
      out.println("<br>");
      out.println("Limit by Category ");
      out.println("<select name=\"clientCode\">");
      out.println("  <option value=\"\">ALL</option>");
      Query query = dataSession.createQuery("from ProjectClient where id.providerId = ? and visible = 'Y' order by client_name");
      query.setParameter(0, webUser.getProviderId());
      List<ProjectClient> projectClientList = query.list();
      for (ProjectClient projectClient : projectClientList)
      {
        if (clientCode.equals(projectClient.getId().getClientCode()))
        {
          out.println("  <option value=\"" + projectClient.getId().getClientCode() + "\" selected>" + projectClient.getClientName() + "</option>");

        } else
        {
          out.println("  <option value=\"" + projectClient.getId().getClientCode() + "\">" + projectClient.getClientName() + "</option>");
        }
      }
      out.println("</select>");
      out.println("&nbsp; Phase ");
      out.println("<select name=\"phaseCode\">");
      if (phaseCode.equals(NOT_CLOSED))
      {
        out.println("  <option value=\"NOT_CLOSED\" selected>ALL - not closed</option>");
      } else
      {
        out.println("  <option value=\"NOT_CLOSED\">ALL - not closed</option>");
      }
      if (phaseCode.equals(ALL))
      {
        out.println("  <option value=\"ALL\" selected>ALL</option>");
      } else
      {
        out.println("  <option value=\"ALL\">ALL</option>");
      }
      query = dataSession.createQuery("from ProjectPhase order by phase_label");
      List<ProjectPhase> projectPhaseList = query.list();
      for (ProjectPhase projectPhase : projectPhaseList)
      {
        if (phaseCode.equals(projectPhase.getPhaseCode()))
        {
          out.println("  <option value=\"" + projectPhase.getPhaseCode() + "\" selected>" + projectPhase.getPhaseLabel() + "</option>");
        } else
        {
          out.println("  <option value=\"" + projectPhase.getPhaseCode() + "\">" + projectPhase.getPhaseLabel() + "</option>");
        }
      }
      out.println("</select>");
      out.println("<input type=\"submit\" name=\"action\" value=\"Search\" >");
      out.println("</form>");

      List<Project> projectList;
      {
        String searchText = n(request.getParameter("searchText"));
        String queryString = "from Project where providerId = ?";
        if (!searchText.equals(""))
        {
          if (searchField.equals(PROJECT_NAME))
          {
            queryString += " and projectName like ?";
          } else if (searchField.equals(PROVIDER_NAME))
          {
            queryString += " and providerName like ?";
          } else if (searchField.equals(VENDOR_NAME))
          {
            queryString += " and vendorName like ?";
          } else if (searchField.equals(SYSTEM_NAME))
          {
            queryString += " and systemName like ?";
          } else if (searchField.equals(DESCRIPTION))
          {
            queryString += " and description like ?";
          } else if (searchField.equals(IIS_SUBMISSION_CODE))
          {
            queryString += " and iisSubmissionCode like ?";
          } else if (searchField.equals(IIS_FACILITY_ID))
          {
            queryString += " and iisFacilityId like ?";
          } else if (searchField.equals(MEDICAL_ORGANIZATION))
          {
            queryString += " and medicalOrganization like ?";
          } else
          {
            queryString += " and projectName like ?";
          }
        }
        if (!phaseCode.equals("") && !phaseCode.equals(ALL))
        {
          if (phaseCode.equals(NOT_CLOSED))
          {
            queryString += " and phaseCode <> 'Clos'";
          } else
          {
            queryString += " and phaseCode = ?";
          }
        }
        if (!clientCode.equals(""))
        {
          queryString += " and clientCode = ?";
        }
        queryString += " order by projectName, clientCode";
        query = dataSession.createQuery(queryString);
        query.setParameter(0, webUser.getProviderId());
        int i = 0;
        if (!searchText.equals(""))
        {
          i++;
          query.setParameter(i, searchText + "%");
        }
        if (!phaseCode.equals("") && !phaseCode.equals(NOT_CLOSED) && !phaseCode.equals(ALL))
        {
          i++;
          query.setParameter(i, phaseCode);
        }
        if (!clientCode.equals(""))
        {
          i++;
          query.setParameter(i, clientCode);
        }
      }

      projectList = query.list();
      List<Integer> projectIdList = new ArrayList<Integer>();
      session.setAttribute(SESSION_VAR_PROJECT_ID_LIST, projectIdList);
      session.removeAttribute(SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST);

      out.println("<table class=\"boxed\">");
      out.println("  <tr class=\"boxed\">");
      out.println("    <th class=\"boxed\">Project</th>");
      out.println("    <th class=\"boxed\">Category</th>");
      out.println("    <th class=\"boxed\">Phase</th>");
      if (!phaseCode.equals(PROJECT_NAME))
      {
        if (searchField.equals(PROVIDER_NAME))
        {
          out.println("    <th class=\"boxed\">Provider Name</th>");
        } else if (searchField.equals(VENDOR_NAME))
        {
          out.println("    <th class=\"boxed\">Vendor Name</th>");
        } else if (searchField.equals(SYSTEM_NAME))
        {
          out.println("    <th class=\"boxed\">System Name</th>");
        } else if (searchField.equals(IIS_SUBMISSION_CODE))
        {
          out.println("    <th class=\"boxed\">IIS Submission Code</th>");
        } else if (searchField.equals(IIS_FACILITY_ID))
        {
          out.println("    <th class=\"boxed\">IIS Facility Id</th>");
        } else if (searchField.equals(MEDICAL_ORGANIZATION))
        {
          out.println("    <th class=\"boxed\">Medical Organization</th>");
        } else if (searchField.equals(DESCRIPTION))
        {
          out.println("    <th class=\"boxed\">Description</th>");
        }
      }
      out.println("  </tr>");
      for (Project project : projectList)
      {
        projectIdList.add(project.getProjectId());
        loadProjectsObject(dataSession, project);
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\"><a href=\"ProjectServlet?projectId=" + project.getProjectId() + "\" class=\"button\">"
            + project.getProjectName() + "</a></td>");
        out.println("    <td class=\"boxed\">" + (project.getProjectClient() != null ? project.getProjectClient().getClientName() : "") + "</td>");
        out.println("    <td class=\"boxed\">" + (project.getProjectPhase() != null ? project.getProjectPhase().getPhaseLabel() : "") + "</td>");
        if (!phaseCode.equals(PROJECT_NAME))
        {
          if (searchField.equals(PROVIDER_NAME))
          {
            out.println("    <td class=\"boxed\">" + n(project.getProviderName()) + "</td>");
          } else if (searchField.equals(VENDOR_NAME))
          {
            out.println("    <td class=\"boxed\">" + n(project.getVendorName()) + "</td>");
          } else if (searchField.equals(SYSTEM_NAME))
          {
            out.println("    <td class=\"boxed\">" + n(project.getSystemName()) + "</td>");
          } else if (searchField.equals(IIS_SUBMISSION_CODE))
          {
            out.println("    <td class=\"boxed\">" + n(project.getIisSubmissionCode()) + "</td>");
          } else if (searchField.equals(IIS_FACILITY_ID))
          {
            out.println("    <td class=\"boxed\">" + n(project.getIisFacilityId()) + "</td>");
          } else if (searchField.equals(MEDICAL_ORGANIZATION))
          {
            out.println("    <td class=\"boxed\">" + n(project.getMedicalOrganization()) + "</td>");
          } else if (searchField.equals(IIS_REGION_CODE))
          {
            out.println("    <td class=\"boxed\">" + n(project.getIisRegionCode()) + "</td>");
          } else if (searchField.equals(DESCRIPTION))
          {
            out.println("    <td class=\"boxed\">" + trimForDisplay(project.getDescription(), 60) + "</td>");
          } 
        }
        out.println("  </tr>");
      }
      out.println("</table>");
      out.println("<h2>Create a New Project</h2>");
      out.println("<p>If you do not see your project in the list above you can <a href=\"ProjectEditServlet\">create</a> one.</p>");
      out.println("<h2>Project Review</h2>");
      out.println("<p>On a regular basis you should <a href=\"ProjectReviewServlet\">review your projects</a> to ensure that each is getting the attention it deserves. </p>");
      printHtmlFoot(out);

    } finally
    {
      out.close();
    }
  }

  protected static void loadProjectsObject(Session dataSession, Project project)
  {
    Query query1 = dataSession.createQuery("from ProjectClient where id.clientCode = ? and id.providerId = ?");
    query1.setParameter(0, project.getClientCode());
    query1.setParameter(1, project.getProviderId());
    List<ProjectClient> projectClientList = query1.list();
    project.setProjectClient(projectClientList.size() > 0 ? projectClientList.get(0) : null);
    ProjectPhase projectPhase = (ProjectPhase) dataSession.get(ProjectPhase.class, project.getPhaseCode());
    project.setProjectPhase(projectPhase);
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
