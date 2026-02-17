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
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class AppReq {

  private static final String SESSION_VAR_TIME_TRACKER = "timeTracker";
  private static final String SESSION_VAR_WEB_USER = "webUser";
  private static final String SESSION_VAR_PROJECT_ID_LIST = "projectIdList";
  private static final String SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST = "projectContactAssignedList";
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
  private ProjectActionNext actionTrackTime = null;
  private Project projectSelected = null;
  private ProjectActionNext projectActionSelected = null;
  private WebUser webUser = null;

  private String title = "";
  private String displaySize = "small";
  private String displayColor = "";
  private Project project = null;
  private Project parentProject = null;
  private ProjectActionNext completingAction = null;
  private ProjectActionNext projectActionParent = null;
  private String action = null;
  private List<WebUser> childWebUserList = null;
  private List<Integer> projectIdList = null;
  private List<Project> projectSelectedList = null;
  private List<ProjectContactAssigned> projectContactAssignedList = null;

  public ProjectActionNext getProjectActionSelected() {
    return projectActionSelected;
  }

  public ProjectActionNext getCompletingAction() {
    return completingAction;
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
      webSession.setAttribute(SESSION_VAR_PROJECT_SELECTED_LIST,
          createProjectIdList(projectSelectedList));
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
        actionTrackTime = loadProjectActionFromSession(
            webUser.getParentWebUser() == null ? SESSION_VAR_ACTION : SESSION_VAR_PARENT_ACTION);
        if (actionTrackTime == null) {
          projectTrackTime = loadProjectFromSession(
              webUser.getParentWebUser() == null ? SESSION_VAR_PROJECT : SESSION_VAR_PARENT_PROJECT);
        } else {
          projectTrackTime = actionTrackTime.getProject();
        }
        if (projectTrackTime != null) {
          timeTracker.update(projectTrackTime, actionTrackTime, dataSession);
        }
      }
      projectSelected = loadProjectFromSession(SESSION_VAR_PROJECT);
      projectActionSelected = loadProjectActionFromSession(SESSION_VAR_ACTION);
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

    project = loadProjectFromSession(SESSION_VAR_PROJECT);
    completingAction = loadProjectActionFromSession(SESSION_VAR_ACTION);
    parentProject = loadProjectFromSession(SESSION_VAR_PARENT_PROJECT);
    action = request.getParameter(PARAM_ACTION);
    childWebUserList = (List<WebUser>) webSession.getAttribute(SESSION_VAR_CHILD_WEB_USER_LIST);
    projectIdList = (List<Integer>) webSession.getAttribute(SESSION_VAR_PROJECT_ID_LIST);
    projectSelectedList = loadProjectListFromSession(SESSION_VAR_PROJECT_SELECTED_LIST);
    projectContactAssignedList = loadProjectContactAssignedListFromSession(SESSION_VAR_PROJECT_CONTACT_ASSIGNED_LIST);
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
      webSession.setAttribute(SESSION_VAR_PROJECT, project.getProjectId());
    }
  }

  public void setCompletingAction(ProjectActionNext projectAction) {
    this.completingAction = projectAction;
    if (projectAction == null) {
      webSession.removeAttribute(SESSION_VAR_ACTION);
    } else {
      webSession.setAttribute(SESSION_VAR_ACTION, projectAction.getActionNextId());
      setProject(projectAction.getProject());
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
      webSession.setAttribute(SESSION_VAR_PARENT_PROJECT, parentProject.getProjectId());
    }
  }

  public ProjectActionNext getProjectActionParent() {
    return projectActionParent;
  }

  public void setProjectActionParent(ProjectActionNext projectActionParent) {
    this.projectActionParent = projectActionParent;
    if (projectActionParent == null) {
      webSession.removeAttribute(SESSION_VAR_PARENT_ACTION);
    } else {
      webSession.setAttribute(SESSION_VAR_PARENT_ACTION, projectActionParent.getActionNextId());
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
          createProjectContactAssignedIdList(projectContactAssignedList));
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

  public void setProjectActionSelected(ProjectActionNext projectActionSelected) {
    this.projectActionSelected = projectActionSelected;
  }

  private Project loadProjectFromSession(String key) {
    Integer projectId = readProjectIdFromSession(key);
    if (projectId == null) {
      return null;
    }
    return (Project) dataSession.get(Project.class, projectId);
  }

  private Integer readProjectIdFromSession(String key) {
    Object value = webSession.getAttribute(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Project) {
      Project project = (Project) value;
      webSession.setAttribute(key, project.getProjectId());
      return project.getProjectId();
    }
    return null;
  }

  private ProjectActionNext loadProjectActionFromSession(String key) {
    Integer actionNextId = readProjectActionNextIdFromSession(key);
    if (actionNextId == null) {
      return null;
    }
    return (ProjectActionNext) dataSession.get(ProjectActionNext.class, actionNextId);
  }

  private Integer readProjectActionNextIdFromSession(String key) {
    Object value = webSession.getAttribute(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof ProjectActionNext) {
      ProjectActionNext action = (ProjectActionNext) value;
      webSession.setAttribute(key, action.getActionNextId());
      return action.getActionNextId();
    }
    return null;
  }

  private List<Project> loadProjectListFromSession(String key) {
    Object value = webSession.getAttribute(key);
    if (value == null) {
      return null;
    }
    if (value instanceof List<?>) {
      List<?> list = (List<?>) value;
      if (list.isEmpty()) {
        return new ArrayList<Project>();
      }
      if (list.get(0) instanceof Project) {
        @SuppressWarnings("unchecked")
        List<Project> projectList = (List<Project>) list;
        webSession.setAttribute(key, createProjectIdList(projectList));
        return loadProjectsById(createProjectIdList(projectList));
      }
      if (list.get(0) instanceof Integer) {
        @SuppressWarnings("unchecked")
        List<Integer> projectIdList = (List<Integer>) list;
        return loadProjectsById(projectIdList);
      }
    }
    return null;
  }

  private List<ProjectContactAssigned> loadProjectContactAssignedListFromSession(String key) {
    Object value = webSession.getAttribute(key);
    if (value == null) {
      return null;
    }
    if (value instanceof List<?>) {
      List<?> list = (List<?>) value;
      if (list.isEmpty()) {
        return new ArrayList<ProjectContactAssigned>();
      }
      if (list.get(0) instanceof ProjectContactAssigned) {
        @SuppressWarnings("unchecked")
        List<ProjectContactAssigned> assignedList = (List<ProjectContactAssigned>) list;
        List<ProjectContactAssignedId> idList = createProjectContactAssignedIdList(assignedList);
        webSession.setAttribute(key, idList);
        return loadProjectContactAssignedById(idList);
      }
      if (list.get(0) instanceof ProjectContactAssignedId) {
        @SuppressWarnings("unchecked")
        List<ProjectContactAssignedId> idList = (List<ProjectContactAssignedId>) list;
        return loadProjectContactAssignedById(idList);
      }
    }
    return null;
  }

  private List<Project> loadProjectsById(List<Integer> projectIdList) {
    List<Project> projectList = new ArrayList<Project>();
    if (projectIdList == null) {
      return projectList;
    }
    for (Integer projectId : projectIdList) {
      if (projectId != null) {
        Project project = (Project) dataSession.get(Project.class, projectId);
        if (project != null) {
          projectList.add(project);
        }
      }
    }
    return projectList;
  }

  private List<Integer> createProjectIdList(List<Project> projectList) {
    List<Integer> projectIdList = new ArrayList<Integer>();
    if (projectList == null) {
      return projectIdList;
    }
    for (Project project : projectList) {
      if (project != null) {
        projectIdList.add(project.getProjectId());
      }
    }
    return projectIdList;
  }

  private List<ProjectContactAssignedId> createProjectContactAssignedIdList(
      List<ProjectContactAssigned> projectContactAssignedList) {
    List<ProjectContactAssignedId> idList = new ArrayList<ProjectContactAssignedId>();
    if (projectContactAssignedList == null) {
      return idList;
    }
    for (ProjectContactAssigned assigned : projectContactAssignedList) {
      if (assigned != null) {
        idList.add(assigned.getId());
      }
    }
    return idList;
  }

  private List<ProjectContactAssigned> loadProjectContactAssignedById(
      List<ProjectContactAssignedId> idList) {
    List<ProjectContactAssigned> assignedList = new ArrayList<ProjectContactAssigned>();
    if (idList == null) {
      return assignedList;
    }
    for (ProjectContactAssignedId id : idList) {
      if (id != null) {
        ProjectContactAssigned assigned = (ProjectContactAssigned) dataSession.get(ProjectContactAssigned.class, id);
        if (assigned != null) {
          assignedList.add(assigned);
        }
      }
    }
    return assignedList;
  }
}
