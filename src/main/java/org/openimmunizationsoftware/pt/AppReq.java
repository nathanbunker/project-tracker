package org.openimmunizationsoftware.pt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectAction;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class AppReq {

  private static final String SESSION_VAR_TIME_TRACKER = "timeTracker";
  private static final String SESSION_VAR_DATA_SESSION = "dataSession";
  private static final String SESSION_VAR_WEB_USER = "webUser";
  private static final String SESSION_VAR_PROJECT_ID_LIST = "projectIdList";
  private static final String SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST =
      "projectContactAssignedList";
  private static final String SESSION_VAR_PROJECT_SELECTED_LIST = "projectSelectedList";
  private static final String SESSION_VAR_PROJECT = "project";
  private static final String SESSION_VAR_PARENT_PROJECT = "parentProject";
  private static final String SESSION_VAR_ACTION = "action";
  private static final String SESSION_VAR_PARENT_ACTION = "parentAction";
  private static final String SESSION_VAR_CHILD_WEB_USER_LIST = "childWebUserList";
  private static final String SESSION_VAR_APP_TYPE = "appType";

  public static final String PARAM_ACTION = "action";

  private PrintWriter out = null;
  private HttpServletRequest request = null;
  private HttpServletResponse response = null;
  private Session dataSession = null;
  private HttpSession webSession = null;
  private String messageProblem = null;
  private String messageConfirmation = null;

  private AppType appType = AppType.TRACKER;
  private TimeTracker timeTracker = null;
  private Project projectTrackTime = null;
  private ProjectAction actionTrackTime = null;
  private Project projectSelected = null;
  private ProjectAction projectActionSelected = null;
  private WebUser webUser = null;

  private String title = "";
  private String displaySize = "small";
  private String displayColor = "";
  private Project project = null;
  private Project parentProject = null;
  private ProjectAction projectAction = null;
  private ProjectAction projectActionParent = null;
  private String action = null;
  private List<WebUser> childWebUserList = null;
  private List<Integer> projectIdList = null;
  private List<Project> projectSelectedList = null;
  private List<ProjectContactAssigned> projectContactAssignedList = null;


  public ProjectAction getProjectActionSelected() {
    return projectActionSelected;
  }

  public ProjectAction getProjectAction() {
    return projectAction;
  }

  public List<Integer> getProjectIdList() {
    return projectIdList;
  }

  public void setProjectIdList(List<Integer> projectIdList) {
    this.projectIdList = projectIdList;
    if (projectIdList == null) {
      webSession.removeAttribute(SESSION_VAR_PROJECT_ID_LIST);
    } else {
      webSession.setAttribute(SESSION_VAR_PROJECT_ID_LIST, projectIdList);
    }
  }

  public List<Project> getProjectSelectedList() {
    return projectSelectedList;
  }

  public void setProjectSelectedList(List<Project> projectSelectedList) {
    this.projectSelectedList = projectSelectedList;
    if (projectSelectedList == null) {
      webSession.removeAttribute(SESSION_VAR_PROJECT_SELECTED_LIST);
    } else {
      webSession.setAttribute(SESSION_VAR_PROJECT_SELECTED_LIST, projectSelectedList);
    }
  }

  public boolean isAdmin() {
    return webUser != null && webUser.isUserTypeAdmin();
  }

  public AppType getAppType() {
    return appType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public boolean isLoggedIn() {
    return webUser != null;
  }

  public boolean isLoggedOut() {
    return webUser == null;
  }

  public boolean isParentWebUser() {
    return isLoggedIn() && webUser.getParentWebUser() == null;
  }

  public boolean isDependentWebUser() {
    return isLoggedIn() && webUser.getParentWebUser() != null;
  }

  public TimeTracker getTimeTracker() {
    return timeTracker;
  }

  public Project getProjectTrackTime() {
    return projectTrackTime;
  }

  public Project getProjectSelected() {
    return projectSelected;
  }

  public WebUser getWebUser() {
    return webUser;
  }

  public String getMessageProblem() {
    return messageProblem;
  }

  public void setMessageProblem(String messageProblem) {
    this.messageProblem = messageProblem;
  }

  public String getMessageConfirmation() {
    return messageConfirmation;
  }

  public void setMessageConfirmation(String messageConfirmation) {
    this.messageConfirmation = messageConfirmation;
  }

  @SuppressWarnings("unchecked")
  public AppReq(HttpServletRequest request, HttpServletResponse response) throws IOException {
    out = new PrintWriter(response.getOutputStream());
    this.request = request;
    this.response = response;
    webSession = request.getSession(true);
    webUser = (WebUser) webSession.getAttribute(SESSION_VAR_WEB_USER);
    SessionFactory factory = CentralControl.getSessionFactory();
    dataSession = factory.openSession();
    if (webUser != null) {
      ClientServlet.webUserLastUsedDate.put(webUser.getUsername(), new Date());
      timeTracker = (TimeTracker) webSession.getAttribute(SESSION_VAR_TIME_TRACKER);
      if (timeTracker != null) {
        actionTrackTime = (ProjectAction) webSession.getAttribute(webUser.getParentWebUser() == null ? SESSION_VAR_ACTION: SESSION_VAR_PARENT_ACTION);
        if (actionTrackTime == null) {
          projectTrackTime = (Project) webSession.getAttribute(
              webUser.getParentWebUser() == null ? SESSION_VAR_PROJECT : SESSION_VAR_PARENT_PROJECT);
        }
        else {
          projectTrackTime = actionTrackTime.getProject();
        }
        if (projectTrackTime != null) {
          timeTracker.update(projectTrackTime, actionTrackTime, dataSession);
        }
      }
      projectSelected = (Project) webSession.getAttribute(SESSION_VAR_PROJECT);
      projectActionSelected = (ProjectAction) webSession.getAttribute(SESSION_VAR_ACTION);
    }

    appType = (AppType) webSession.getAttribute(SESSION_VAR_APP_TYPE);
    if (appType == null) {
      appType = AppType.TRACKER;
      webSession.setAttribute(SESSION_VAR_APP_TYPE, appType);
    }
    title = "Home";

    displaySize = "small";
    displayColor = "";
    if (webUser != null) {
      displaySize = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_SIZE, "small",
          webUser, dataSession);
      displayColor = TrackerKeysManager.getKeyValue(TrackerKeysManager.KEY_DISPLAY_COLOR, "",
          webUser, dataSession);
    }
    if (request.getParameter("displayColor") != null) {
      displayColor = request.getParameter("displayColor");
    }

    project = (Project) webSession.getAttribute(SESSION_VAR_PROJECT);
    projectAction = (ProjectAction) webSession.getAttribute(SESSION_VAR_ACTION);
    parentProject = (Project) webSession.getAttribute(SESSION_VAR_PARENT_PROJECT);
    action = request.getParameter(PARAM_ACTION);
    childWebUserList = (List<WebUser>) webSession.getAttribute(SESSION_VAR_CHILD_WEB_USER_LIST);
    projectIdList = (List<Integer>) webSession.getAttribute(SESSION_VAR_PROJECT_ID_LIST);
    projectSelectedList =
        (List<Project>) webSession.getAttribute(SESSION_VAR_PROJECT_SELECTED_LIST);
    projectContactAssignedList = (List<ProjectContactAssigned>) webSession
        .getAttribute(SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST);
  }

  public List<WebUser> getChildWebUserList() {
    return childWebUserList;
  }

  public void setChildWebUserList(List<WebUser> childWebUserList) {
    this.childWebUserList = childWebUserList;
    if (childWebUserList == null) {
      webSession.removeAttribute(SESSION_VAR_CHILD_WEB_USER_LIST);
    } else {
      webSession.setAttribute(SESSION_VAR_CHILD_WEB_USER_LIST, childWebUserList);
    }
  }

  public void setWebUser(WebUser webUser) {
    this.webUser = webUser;
    if (webUser == null) {
      webSession.removeAttribute(SESSION_VAR_WEB_USER);
    } else {
      webSession.setAttribute(SESSION_VAR_WEB_USER, webUser);
    }
  }

  public void close() {
    if (out != null) {
      out.close();
      out = null;
    }
    if (dataSession != null) {
      dataSession.close();
      dataSession = null;
    }
  }


  public PrintWriter getOut() {
    return out;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public HttpServletResponse getResponse() {
    return response;
  }

  public HttpSession getWebSession() {
    return webSession;
  }

  public Session getDataSession() {
    return dataSession;
  }

  public String getDisplaySize() {
    return displaySize;
  }

  public String getDisplayColor() {
    return displayColor;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
    if (project == null) {
      webSession.removeAttribute(SESSION_VAR_PROJECT);
    } else {
      webSession.setAttribute(SESSION_VAR_PROJECT, project);
    }
  }

  public void setProjectAction(ProjectAction projectAction) {
    this.projectAction = projectAction;
    if (projectAction == null) {
      webSession.removeAttribute(SESSION_VAR_ACTION);
    } else {
      webSession.setAttribute(SESSION_VAR_ACTION, projectAction);
    }
  }

  public Project getParentProject() {
    return parentProject;
  }

  public void setParentProject(Project parentProject) {
    this.parentProject = parentProject;
    if (parentProject == null) {
      webSession.removeAttribute(SESSION_VAR_PARENT_PROJECT);
    } else {
      webSession.setAttribute(SESSION_VAR_PARENT_PROJECT, parentProject);
    }
  }
  
  public ProjectAction getProjectActionParent() {
    return projectActionParent;
  }

  public void setProjectActionParent(ProjectAction projectActionParent) {
    this.projectActionParent = projectActionParent;
    if (projectActionParent == null) {
      webSession.removeAttribute(SESSION_VAR_PARENT_ACTION);
    } else {
      webSession.setAttribute(SESSION_VAR_PARENT_ACTION, projectActionParent);
    }
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }


  public List<Project> createProjectList() {
    List<Project> projectList = new ArrayList<Project>();

    if (projectIdList != null && projectIdList.size() > 0) {
      for (int projectId : projectIdList) {
        projectList.add((Project) dataSession.get(Project.class, projectId));
      }
    }
    return projectList;
  }

  public List<ProjectContactAssigned> getProjectContactAssignedList() {
    return projectContactAssignedList;
  }

  public void setProjectContactAssignedList(
      List<ProjectContactAssigned> projectContactAssignedList) {
    this.projectContactAssignedList = projectContactAssignedList;
    if (projectContactAssignedList == null) {
      webSession.removeAttribute(SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST);
    } else {
      webSession.setAttribute(SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST,
          projectContactAssignedList);
    }
  }

  public void setTimeTracker(TimeTracker timeTracker) {
    if (timeTracker == null) {
      webSession.removeAttribute(SESSION_VAR_TIME_TRACKER);
    } else {
      webSession.setAttribute(SESSION_VAR_TIME_TRACKER, timeTracker);
    }
  }

  public void logout() {
    webSession.invalidate();
    webSession = request.getSession(true);
  }


  public void setProjectSelected(Project projectSelected) {
    this.projectSelected = projectSelected;
  }

  public void setProjectActionSelected(ProjectAction projectActionSelected) {
    this.projectActionSelected = projectActionSelected;
  }

}


