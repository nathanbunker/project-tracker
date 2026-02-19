package org.openimmunizationsoftware.pt.mobile.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ProjectServlet;

/**
 * Mobile Action page placeholder.
 * 
 * @author nathan
 */
public class ActionServlet extends MobileBaseServlet {

    private static final String SAVE_PROJECT_ACTION_FORM = "saveProjectActionForm";
    private static final String PARAM_START_SENTANCE = "startSentance";
    private static final String PARAM_NEXT_CONTACT_ID = "nextContactId";
    private static final String PARAM_NEXT_ACTION_TYPE = "nextActionType";
    private static final String PARAM_LINK_URL = "linkUrl";
    private static final String PARAM_NEXT_ACTION_DATE = "nextActionDate";
    private static final String PARAM_NEXT_DESCRIPTION = "nextDescription";
    private static final String PARAM_NEXT_NOTE = "nextNote";
    private static final String PARAM_NEXT_PROJECT_ID = "nextProjectId";
    private static final String PARAM_PROJECT_ID = "projectId";
    private static final String PARAM_EDIT_ACTION_NEXT_ID = "editActionNextId";
    private static final String PARAM_ACTION_NEXT_ID = "actionNextId";
    private static final String PARAM_ACTION = "action";

    private static final String ACTION_SAVE = "Save";
    private static final String ACTION_START = "Start";
    private static final String ACTION_DELETE = "Delete";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            String action = request.getParameter(PARAM_ACTION);
            boolean showWork = isShowWork(request);
            boolean showPersonal = isShowPersonal(request);

            List<Project> projectList = getProjectList(webUser, dataSession, showWork, showPersonal);
            ProjectActionNext editProjectAction = readEditProjectAction(appReq);
            Project project = resolveProject(request, dataSession, projectList, editProjectAction);

            if (action != null) {
                if (action.equals(ACTION_SAVE) || action.equals(ACTION_START)) {
                    Project nextProject = project;
                    if (editProjectAction == null) {
                        String nextProjectIdString = request.getParameter(PARAM_NEXT_PROJECT_ID);
                        if (nextProjectIdString != null) {
                            nextProject = (Project) dataSession.get(Project.class,
                                    Integer.parseInt(nextProjectIdString));
                        }
                    }
                    editProjectAction = saveProjectAction(appReq, editProjectAction, nextProject);
                    project = editProjectAction.getProject();

                    // Redirect back to the Project page for this project
                    if (project != null) {
                        response.sendRedirect("project?" + PARAM_PROJECT_ID + "=" + project.getProjectId());
                        return;
                    }
                } else if (action.equals(ACTION_DELETE) && editProjectAction != null && project != null) {
                    closeAction(appReq, editProjectAction, project, "", ProjectNextActionStatus.CANCELLED);
                    appReq.setMessageConfirmation("Action deleted");
                    editProjectAction = null;
                }
            }

            List<ProjectContact> projectContactList = Collections.emptyList();
            if (project != null) {
                List<ProjectContactAssigned> projectContactAssignedList = ProjectServlet
                        .getProjectContactAssignedList(dataSession, project.getProjectId());
                projectContactList = ProjectServlet.getProjectContactList(dataSession, project,
                        projectContactAssignedList);
            }

            appReq.setTitle("Action");
            printHtmlHead(appReq, "Action");
            PrintWriter out = appReq.getOut();
            out.println("<h1>Action</h1>");

            if (project == null) {
                out.println("<p class=\"fail\">No active projects found.</p>");
            } else {
                String formName = editProjectAction != null ? String.valueOf(editProjectAction.getActionNextId()) : "0";
                printEditProjectActionForm(appReq, editProjectAction, projectContactList, formName, project,
                        projectList);
            }

            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
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

    private Project resolveProject(HttpServletRequest request, Session dataSession, List<Project> projectList,
            ProjectActionNext editProjectAction) {
        if (editProjectAction != null && editProjectAction.getProject() != null) {
            return editProjectAction.getProject();
        }

        String projectIdString = request.getParameter(PARAM_PROJECT_ID);
        if (projectIdString == null) {
            projectIdString = request.getParameter(PARAM_NEXT_PROJECT_ID);
        }
        if (projectIdString != null) {
            return (Project) dataSession.get(Project.class, Integer.parseInt(projectIdString));
        }

        if (projectList.isEmpty()) {
            return null;
        }
        return projectList.get(0);
    }

    private ProjectActionNext readEditProjectAction(AppReq appReq) {
        Session dataSession = appReq.getDataSession();
        HttpServletRequest request = appReq.getRequest();
        String actionNextIdString = request.getParameter(PARAM_EDIT_ACTION_NEXT_ID);
        if (actionNextIdString == null) {
            actionNextIdString = request.getParameter(PARAM_ACTION_NEXT_ID);
        }
        ProjectActionNext editProjectAction = null;
        if (actionNextIdString != null) {
            editProjectAction = (ProjectActionNext) dataSession.get(ProjectActionNext.class,
                    Integer.parseInt(actionNextIdString));
        }
        return editProjectAction;
    }

    private ProjectActionNext saveProjectAction(AppReq appReq,
            ProjectActionNext editProjectAction, Project nextProject) {
        HttpServletRequest request = appReq.getRequest();
        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();
        boolean isNewAction = editProjectAction == null;
        if (isNewAction) {
            editProjectAction = new ProjectActionNext();
            editProjectAction.setProject(nextProject);
            editProjectAction.setProjectId(nextProject.getProjectId());
            editProjectAction.setBillable(resolveBillable(dataSession, nextProject));
        }
        editProjectAction.setContactId(webUser.getContactId());
        editProjectAction.setContact(webUser.getProjectContact());
        editProjectAction.setNextDescription(trim(request.getParameter(PARAM_NEXT_DESCRIPTION), 1200));
        editProjectAction.setNextChangeDate(new Date());
        String nextNote = request.getParameter(PARAM_NEXT_NOTE);
        if (nextNote != null && nextNote.length() > 0) {
            if (editProjectAction.getNextNotes() != null && editProjectAction.getNextNotes().trim().length() > 0) {
                nextNote = editProjectAction.getNextNotes() + "\n - " + nextNote;
            } else {
                nextNote = " - " + nextNote;
            }
            editProjectAction.setNextNotes(nextNote);
        }

        editProjectAction
                .setNextActionDate(appReq.getWebUser().parseDate(request.getParameter(PARAM_NEXT_ACTION_DATE)));
        String linkUrl = request.getParameter(PARAM_LINK_URL);
        if (linkUrl == null || linkUrl.equals("")) {
            editProjectAction.setLinkUrl(null);
        } else {
            editProjectAction.setLinkUrl(linkUrl);
        }

        String nextActionType = request.getParameter(PARAM_NEXT_ACTION_TYPE);
        editProjectAction.setNextActionType(nextActionType);
        int priorityLevel = editProjectAction.getProject().getPriorityLevel();
        editProjectAction.setPriorityLevel(priorityLevel);
        String nextContactIdString = request.getParameter(PARAM_NEXT_CONTACT_ID);
        if (nextContactIdString != null && nextContactIdString.length() > 0) {
            editProjectAction.setNextContactId(Integer.parseInt(nextContactIdString));
        }
        editProjectAction.setProvider(webUser.getProvider());
        if (editProjectAction.getNextActionStatus() == null) {
            if (editProjectAction.hasNextDescription()) {
                if (editProjectAction.hasNextActionDate()) {
                    editProjectAction.setNextActionStatus(ProjectNextActionStatus.READY);
                } else {
                    editProjectAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
                }
            }
        }

        Transaction trans = dataSession.beginTransaction();
        dataSession.saveOrUpdate(editProjectAction);
        trans.commit();
        return editProjectAction;
    }

    private boolean resolveBillable(Session dataSession, Project project) {
        if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
            return false;
        }
        BillCode billCode = (BillCode) dataSession.get(BillCode.class, project.getBillCode());
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private void closeAction(AppReq appReq, ProjectActionNext projectAction, Project project,
            String nextDescription, ProjectNextActionStatus nextActionStatus) {
        Session dataSession = appReq.getDataSession();
        Transaction trans = dataSession.beginTransaction();
        projectAction.setNextActionStatus(nextActionStatus);
        projectAction.setNextChangeDate(new Date());
        dataSession.update(projectAction);
        trans.commit();
    }

    private List<Project> getProjectList(WebUser webUser, Session dataSession, boolean showWork,
            boolean showPersonal) {
        String queryString = "from Project where provider = ?";
        queryString += " and phaseCode <> 'Clos'";
        queryString += " order by projectName";
        Query query = dataSession.createQuery(queryString);
        query.setParameter(0, webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<Project> allProjects = query.list();
        List<Project> filteredProjects = new ArrayList<Project>();
        for (Project project : allProjects) {
            boolean billable = resolveBillable(dataSession, project);
            if ((billable && showWork) || (!billable && showPersonal)) {
                filteredProjects.add(project);
            }
        }
        return filteredProjects;
    }

    private void printEditProjectActionForm(AppReq appReq, ProjectActionNext editProjectAction,
            List<ProjectContact> projectContactList, String formName, Project project,
            List<Project> projectList) {
        WebUser webUser = appReq.getWebUser();
        PrintWriter out = appReq.getOut();
        out.println("<form name=\"projectAction" + formName
                + "\" method=\"post\" action=\"action\" id=\"" + SAVE_PROJECT_ACTION_FORM + formName
                + "\">");
        SimpleDateFormat sdf = webUser.getDateFormat();
        out.println(" <script>");
        out.println("    function selectProjectActionType" + formName + "(actionType)");
        out.println("    {");
        out.println("      var form = document.forms['saveProjectActionForm" + formName + "'];");
        out.println("      var label = makeIStatement" + formName
                + "(actionType, form.nextContactId.options[form.nextContactId.selectedIndex].text);");
        out.println("      form." + PARAM_START_SENTANCE + ".value = label;");
        out.println("      form.nextActionType.value = actionType;");
        out.println("      enableForm" + formName + "(); ");
        out.println("    }");
        out.println("    ");
        out.println("    function enableForm" + formName + "()");
        out.println("    {");
        out.println("      var form = document.forms['" + SAVE_PROJECT_ACTION_FORM + formName + "'];");
        out.println("      form." + PARAM_NEXT_ACTION_DATE + ".disabled = false;");
        out.println("      form." + PARAM_NEXT_DESCRIPTION + ".disabled = false;");
        out.println("      form." + PARAM_NEXT_CONTACT_ID + ".disabled = false;");
        out.println("      form." + PARAM_START_SENTANCE + ".disabled = false;");
        out.println("      form." + PARAM_LINK_URL + ".disabled = false;");
        out.println("      if (form." + PARAM_NEXT_ACTION_DATE + ".value == \"\")");
        out.println("      {");
        out.println("       document.projectAction" + formName + "." + PARAM_NEXT_ACTION_DATE + ".value = '"
                + sdf.format(new Date()) + "';");
        out.println("      }");
        out.println("    }");
        out.println("    function setNextAction" + formName + "(nextActionDate) {");
        out.println(
                "      document.projectAction" + formName + "." + PARAM_NEXT_ACTION_DATE + ".value = nextActionDate;");
        out.println("      enableForm" + formName + "(); ");
        out.println("    }");
        printMakeIStatementFunction(out, formName);
        out.println("  </script>");
        if (editProjectAction != null) {
            out.println("<input type=\"hidden\" name=\"" + PARAM_EDIT_ACTION_NEXT_ID + "\" value=\""
                    + editProjectAction.getActionNextId() + "\">");
        }
        printCurrentActionEdit(appReq, webUser, out, editProjectAction, project, projectContactList, formName,
                projectList);

        out.println("</form>");
    }

    private void printCurrentActionEdit(AppReq appReq, WebUser webUser, PrintWriter out,
            ProjectActionNext projectAction,
            Project project,
            List<ProjectContact> projectContactList,
            String formName, List<Project> projectList) {

        String disabled = "";
        out.println("<table class=\"boxed-mobile\">");
        out.println("  <tr>");
        out.println("    <th class=\"title\">Edit Action</th>");
        out.println("  </tr>");
        printEditNextAction(appReq.getRequest(), webUser, out, projectAction, project, formName, disabled,
                projectContactList, projectList);
        out.println("</table>");
        out.println("  <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_SAVE + "\">"
                + ACTION_SAVE + "</button>");
        out.println("  <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_START + "\">"
                + ACTION_START + "</button>");
        if (projectAction != null) {
            out.println("  <button type=\"submit\" name=\"" + PARAM_ACTION + "\" value=\"" + ACTION_DELETE + "\">"
                    + ACTION_DELETE + "</button>");
        }

    }

    private void printEditNextAction(HttpServletRequest request, WebUser webUser, PrintWriter out,
            ProjectActionNext projectAction, Project project, String formName, String disabled,
            List<ProjectContact> projectContactList,
            List<Project> projectList) {
        SimpleDateFormat sdf1;
        out.println("  <tr>");
        out.println("    <td class=\"outside\">");
        out.println("      <table class=\"inside\">");
        SimpleDateFormat sdf2 = webUser.getDateFormat("MM/dd/yyyy");
        {
            sdf1 = webUser.getDateFormat();
            out.println("        <tr>");
            out.println("          <th class=\"inside\">Project</th>");
            out.println("          <td>");
            if (projectAction == null) {
                out.println(
                        "            <select name=\"" + PARAM_NEXT_PROJECT_ID + "\" onchange=\"enableForm" + formName
                                + "()\">");
                for (Project p : projectList) {
                    out.println("              <option value=\"" + p.getProjectId() + "\""
                            + (project != null && project.getProjectId() == p.getProjectId() ? " selected" : "")
                            + ">" + p.getProjectName() + "</option>");
                }
                out.println("            </select>");
            } else {
                out.println("            " + project.getProjectName());
                out.println(
                        "<input type=\"hidden\" name=\"" + PARAM_NEXT_PROJECT_ID + "\" value=\""
                                + projectAction.getProjectId()
                                + "\">");

            }
            out.println("          </td>");
            out.println("        </tr>");
            out.println("        <tr>");
            out.println("          <th class=\"inside\">When</th>");
            {
                String nextActionDateString = projectAction == null || projectAction.getNextActionDate() == null
                        ? request.getParameter(PARAM_NEXT_ACTION_DATE)
                        : sdf2.format(projectAction.getNextActionDate());
                out.println(
                        "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\""
                                + PARAM_NEXT_ACTION_DATE
                                + "\" size=\"10\" value=\""
                                + n(nextActionDateString) + "\" onkeydown=\"resetRefresh()\"" + disabled + ">");
            }
            out.println("            <font size=\"-1\">");
            Calendar calendar = webUser.getCalendar();
            SimpleDateFormat day = webUser.getDateFormat("EEE");
            out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
                    + sdf2.format(calendar.getTime()) + "');\" class=\"button\">Today</a>");
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
                    + sdf2.format(calendar.getTime()) + "');\" class=\"button\">"
                    + day.format(calendar.getTime()) + "</a>");
            boolean nextWeek = false;
            for (int i = 0; i < 6; i++) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                if (nextWeek) {
                    out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
                            + sdf2.format(calendar.getTime()) + "');\" class=\"button\">Next-"
                            + day.format(calendar.getTime()) + "</a>");
                } else {
                    out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
                            + sdf2.format(calendar.getTime()) + "');\" class=\"button\">"
                            + day.format(calendar.getTime()) + "</a>");

                }
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    nextWeek = true;
                }
            }
            calendar.set(Calendar.MONTH, 11);
            calendar.set(Calendar.DAY_OF_MONTH, 31);
            out.println("              <a href=\"javascript: void setNextAction" + formName + "('"
                    + sdf1.format(calendar.getTime()) + "');\" class=\"button\">EOY</a>");
            out.println("</font>");

            out.println("          </td>");
            out.println("        </tr>");
        }
        out.println("        <tr>");
        out.println("          <th class=\"inside\">Action</th>");
        out.println("          <td class=\"inside\" colspan=\"3\">");
        out.println(
                "            I: <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
                        + formName + "('" + ProjectNextActionType.WILL + "');\" class=\"button\"> will</a>,");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.MIGHT + "');\" class=\"button\">might</a>, ");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.WILL_CONTACT + "');\" class=\"button\">will contact</a>, ");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.WILL_MEET + "');\" class=\"button\">will meet</a>,");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.WILL_REVIEW + "');\" class=\"button\">will review</a>,");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.WILL_DOCUMENT + "');\" class=\"button\">will document</a>,");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.WILL_FOLLOW_UP + "');\" class=\"button\">will follow up</a>");
        out.println("            </font><br/>");
        out.println("            I have: ");
        out.println("            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
                + formName + "('" + ProjectNextActionType.COMMITTED_TO
                + "');\" class=\"button\">committed</a>,");
        out.println("            <a href=\"javascript: void selectProjectActionType" + formName + "('"
                + ProjectNextActionType.GOAL + "');\" class=\"button\">set goal</a></font>");
        out.println("            I am:");
        out.println("            <font size=\"-1\"><a href=\"javascript: void selectProjectActionType"
                + formName + "('" + ProjectNextActionType.WAITING + "');\" class=\"button\">waiting</a>");
        out.println("            <br>");
        {
            String nextActionType = projectAction == null ? ProjectNextActionType.WILL
                    : projectAction.getNextActionType();
            out.println("            <input type=\"hidden\" name=\"" + PARAM_NEXT_ACTION_TYPE + "\" value=\""
                    + nextActionType + "\">");
            out.println("<script>");
            out.println("  window.addEventListener('load', function() { selectProjectActionType"
                    + formName + "('" + nextActionType + "'); }); ");
            out.println("</script>");
        }
        out.println("            </font>");
        out.println("          </td>");
        out.println("        </tr>");
        out.println("        <tr>");
        out.println("          <th class=\"inside\">What</th>");
        out.println("          <td class=\"inside\"> ");

        out.println(
                "            <input name=\"" + PARAM_START_SENTANCE + "\" size=\"40\" value=\"I will:\"" + disabled
                        + ">");
        out.println("          </td>");
        out.println("          <th class=\"inside\">Who</th>");
        out.println("          <td class=\"inside\"> ");
        out.println("              <select name=\"nextContactId\" onchange=\"selectProjectActionType"
                + formName + "(form.nextActionType.value);\"" + disabled
                + "><option value=\"\">none</option>");
        String nextContactId = n(request.getParameter(PARAM_NEXT_CONTACT_ID));
        for (ProjectContact projectContact1 : projectContactList) {
            if (projectContact1.getContactId() != webUser.getProjectContact().getContactId()) {
                boolean selected = nextContactId.equals(Integer.toString(
                        projectAction == null ? projectContact1.getContactId() : projectAction.getContactId()));
                out.println("                  <option value=\"" + projectContact1.getContactId() + "\""
                        + (selected ? " selected" : "") + ">" + projectContact1.getName() + "</option>");
            }
        }
        out.println("            </select>");
        out.println("          </td>");
        out.println("        </tr>");
        out.println("        <tr>");
        out.println("          <th class=\"inside\"></th>");
        out.println("          <td class=\"inside\" colspan=\"3\"> ");
        out.println(
                "            <textarea name=\"" + PARAM_NEXT_DESCRIPTION + "\" rows=\"1\" onkeydown=\"resetRefresh()\""
                        + disabled + ">" + (projectAction == null ? "" : projectAction.getNextDescription())
                        + "</textarea>");
        out.println("          </td>");
        out.println("        </tr>");
        {
            out.println("        <tr>");
            out.println("          <th class=\"inside\">Note</th>");
            out.println(
                    "          <td class=\"inside\" colspan=\"3\"><textarea rows=\"3\" name=\"" + PARAM_NEXT_NOTE
                            + "\" size=\"30\" onkeydown=\"resetRefresh()\" " + disabled + "></textarea></td>");
            out.println("        </tr>");
        }
        {
            out.println("        <tr>");
            out.println("          <th class=\"inside\">Link</th>");
            out.println(
                    "          <td class=\"inside\" colspan=\"3\"><input type=\"text\" name=\"" + PARAM_LINK_URL
                            + "\" size=\"30\" value=\""
                            + n(projectAction == null || projectAction.getLinkUrl() == null
                                    ? ""
                                    : projectAction.getLinkUrl())
                            + "\" onkeydown=\"resetRefresh()\"" + disabled + "></td>");
            out.println("        </tr>");
        }
        out.println("      </table>");
        out.println("    </td>");
        out.println("  </tr>");
    }

    private static void printMakeIStatementFunction(PrintWriter out, String formName) {
        out.println("    function makeIStatement" + formName + "(actionType, nextContactName)");
        out.println("    {");
        out.println("      if (nextContactName == 'none')");
        out.println("      {");
        out.println("        nextContactName = '';\n      }");
        out.println("      if (actionType == '" + ProjectNextActionType.WILL_CONTACT + "')");
        out.println("      {");
        out.println("        if (nextContactName == '')");
        out.println("        {");
        out.println("          return \"I will make contact about:\";");
        out.println("        } else");
        out.println("        {");
        out.println("          return \"I will contact \" + nextContactName + \" about:\";");
        out.println("        }");
        out.println("      } else if (actionType == '" + ProjectNextActionType.COMMITTED_TO + "')");
        out.println("      {");
        out.println("        if (nextContactName == '')");
        out.println("        {");
        out.println("          return \"I have committed to:\";");
        out.println("        } else");
        out.println("        {");
        out.println("          return \"I have committed to \" + nextContactName + \" to:\";");
        out.println("        }");
        out.println("      } else if (actionType == '" + ProjectNextActionType.GOAL + "')");
        out.println("      {");
        out.println("          if (nextContactName == '')");
        out.println("          {");
        out.println("            return \"I have set goal to:\";");
        out.println("          } else");
        out.println("          {");
        out.println("            return \"I have set goal with \" + nextContactName + \" to:\";");
        out.println("          }");
        out.println("      } else if (actionType == '" + ProjectNextActionType.WAITING + "')");
        out.println("      {");
        out.println("        if (nextContactName == '')");
        out.println("        {");
        out.println("          return \"I am waiting for:\";");
        out.println("        } else");
        out.println("        {");
        out.println("          return \"I am waiting for \" + nextContactName + \" to:\";");
        out.println("        }");
        out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_FOLLOW_UP + "')");
        out.println("      {");
        out.println("        if (nextContactName == '')");
        out.println("        {");
        out.println("          return \"I will follow up:\";");
        out.println("        } else");
        out.println("        {");
        out.println("          return \"I will follow up with \" + nextContactName + \" to:\";");
        out.println("        }");
        out.println("      } else if (actionType == '" + ProjectNextActionType.MIGHT + "')");
        out.println("      {");
        out.println("          return \"I might:\";");
        out.println("      } else if (actionType == '" + ProjectNextActionType.GOAL + "')");
        out.println("      {");
        out.println("          return \"I have a goal to:\";");
        out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_REVIEW + "')");
        out.println("      {");
        out.println("          return \"I will review:\";");
        out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_DOCUMENT + "')");
        out.println("      {");
        out.println("          return \"I will document:\";");
        out.println("      } else if (actionType == '" + ProjectNextActionType.WILL_MEET + "')");
        out.println("      {");
        out.println("        if (nextContactName == '')");
        out.println("        {");
        out.println("          return \"I will meet:\";");
        out.println("        } else");
        out.println("        {");
        out.println("          return \"I will meet with \" + nextContactName + \" to:\";");
        out.println("        }");
        out.println("      } else");
        out.println("      {");
        out.println("        return \"I will:\";");
        out.println("      }");
        out.println("    }");
        out.println("    ");
    }
}
