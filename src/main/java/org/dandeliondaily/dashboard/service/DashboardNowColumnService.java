package org.dandeliondaily.dashboard.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.dandeliondaily.dashboard.model.DashboardNowColumnModel;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.ProjectIssueDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectIssue;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.ActionTaken;

public class DashboardNowColumnService {

    private static final int OPEN_ACTION_LIMIT = 50;
    private static final int COMPLETED_LIMIT = 20;
    private static final int COMPLETED_DAYS = 90;

    public DashboardNowColumnModel buildModel(AppReq appReq) {
        DashboardNowColumnModel model = new DashboardNowColumnModel();

        WebUser webUser = appReq.getWebUser();
        Session dataSession = appReq.getDataSession();

        // Real data wiring starts here: use the same AppReq session-backed selection
        // used by downstream UI handlers without direct servlet coupling.
        ActionNext currentAction = appReq.getCompletingAction();
        Project currentProject = appReq.getProject();
        if (currentAction != null && currentAction.getProject() != null) {
            currentProject = currentAction.getProject();
        }

        appReq.setProjectSelected(currentProject);
        appReq.setProjectActionSelected(currentAction);

        model.setCurrentAction(createCurrentActionModel(webUser, dataSession, currentAction));
        model.setCurrentProject(createCurrentProjectModel(currentProject));

        if (currentProject != null) {
            List<ActionNext> openActions = loadOpenProjectActions(dataSession, currentProject);
            sortBacklogActions(openActions);
            model.setScheduledActions(buildScheduledItems(webUser, openActions, currentAction));
            model.setIdeaActions(buildIdeaItems(webUser, openActions, currentAction));
            model.setUnscheduledActions(buildUnscheduledItems(webUser, openActions, currentAction));
            model.setTemplatedActions(buildTemplatedItems(webUser, openActions));
            model.setRecentCompleted(buildRecentCompleted(webUser, dataSession, currentProject));
            model.setOpenIssues(buildOpenIssueItems(webUser, dataSession, currentProject));
            model.setTakenToday(buildTakenToday(webUser, dataSession, currentProject));
            model.setTakenActions(buildTakenActions(webUser, dataSession, currentProject));
        }

        return model;
    }

    private DashboardNowColumnModel.CurrentAction createCurrentActionModel(WebUser webUser, Session dataSession,
            ActionNext currentAction) {
        DashboardNowColumnModel.CurrentAction model = new DashboardNowColumnModel.CurrentAction();
        if (currentAction == null) {
            return model;
        }

        model.setAvailable(true);
        model.setTrackable(currentAction.isBillable());
        model.setActionNextId(currentAction.getActionNextId());
        model.setTitle(n(currentAction.getNextDescription(), "No action description"));
        model.setSummary(n(currentAction.getNextSummary(), ""));
        model.setDescriptionHtml(currentAction.getNextDescriptionForDisplay(webUser.getProjectContact()));
        model.setEstimateDisplay(displayTime(currentAction.getNextTimeEstimateForDisplay()));
        model.setTimeSpentTodayDisplay(loadTodayTimeSpentDisplay(webUser, dataSession, currentAction));
        model.setStatusLabel(resolveStatusLabel(currentAction));
        model.setActionTypeLabel(n(currentAction.getNextActionType(), "-"));
        model.setNotes(parseNotes(currentAction.getNextNotes()));
        model.setLinkUrl(n(currentAction.getLinkUrl(), ""));
        if (currentAction.getNextDeadlineDate() != null) {
            model.setDeadlineDisplay(webUser.getDateFormatService().formatPattern(currentAction.getNextDeadlineDate(),
                    webUser.getDateDisplayPatternWithWeekdayShort(), webUser.getTimeZone()));
        }
        return model;
    }

    private DashboardNowColumnModel.CurrentProject createCurrentProjectModel(Project currentProject) {
        DashboardNowColumnModel.CurrentProject model = new DashboardNowColumnModel.CurrentProject();
        if (currentProject == null) {
            return model;
        }

        model.setAvailable(true);
        model.setProjectId(currentProject.getProjectId());
        model.setName(n(currentProject.getProjectName(), "Unnamed project"));
        model.setHandle(n(currentProject.getProjectHandle(), ""));
        model.setDescription(n(currentProject.getDescription(), "No project description available yet."));
        return model;
    }

    private List<DashboardNowColumnModel.ScheduledActionItem> buildScheduledItems(WebUser webUser,
            List<ActionNext> openActions, ActionNext currentAction) {
        List<DashboardNowColumnModel.ScheduledActionItem> items = new ArrayList<>();
        for (ActionNext action : openActions) {
            if (action.getNextActionDate() == null || action.isTemplate()) {
                continue;
            }
            DashboardNowColumnModel.ScheduledActionItem item = new DashboardNowColumnModel.ScheduledActionItem();
            item.setDateLabel(formatDateOnly(webUser, action.getNextActionDate()));
            item.setDescriptionHtml(action.getNextDescriptionForDisplay(webUser.getProjectContact()));
            item.setActionNextId(action.getActionNextId());
            item.setCurrentSelection(currentAction != null
                    && currentAction.getActionNextId() == action.getActionNextId());
            item.setToday(isTodayDate(webUser, action.getNextActionDate()));
            items.add(item);
        }
        return items;
    }

    private List<DashboardNowColumnModel.UnscheduledActionItem> buildUnscheduledItems(WebUser webUser,
            List<ActionNext> openActions, ActionNext currentAction) {
        List<DashboardNowColumnModel.UnscheduledActionItem> items = new ArrayList<>();
        for (ActionNext action : openActions) {
            if (action.getNextActionDate() != null || action.isTemplate()
                    || ProjectNextActionType.WOULD_LIKE_TO.equals(action.getNextActionType())) {
                continue;
            }
            DashboardNowColumnModel.UnscheduledActionItem item = new DashboardNowColumnModel.UnscheduledActionItem();
            item.setDescriptionHtml(action.getNextDescriptionForDisplay(webUser.getProjectContact()));
            item.setActionNextId(action.getActionNextId());
            item.setCurrentSelection(currentAction != null
                    && currentAction.getActionNextId() == action.getActionNextId());
            items.add(item);
        }
        return items;
    }

    private List<DashboardNowColumnModel.UnscheduledActionItem> buildIdeaItems(WebUser webUser,
            List<ActionNext> openActions, ActionNext currentAction) {
        List<DashboardNowColumnModel.UnscheduledActionItem> items = new ArrayList<>();
        for (ActionNext action : openActions) {
            if (action.getNextActionDate() != null || action.isTemplate()
                    || !ProjectNextActionType.WOULD_LIKE_TO.equals(action.getNextActionType())) {
                continue;
            }
            DashboardNowColumnModel.UnscheduledActionItem item = new DashboardNowColumnModel.UnscheduledActionItem();
            item.setDescriptionHtml(action.getNextDescriptionForDisplay(webUser.getProjectContact()));
            item.setActionNextId(action.getActionNextId());
            item.setCurrentSelection(currentAction != null
                    && currentAction.getActionNextId() == action.getActionNextId());
            items.add(item);
        }
        return items;
    }

    private List<DashboardNowColumnModel.TemplatedActionItem> buildTemplatedItems(WebUser webUser,
            List<ActionNext> openActions) {
        List<DashboardNowColumnModel.TemplatedActionItem> items = new ArrayList<>();
        for (ActionNext action : openActions) {
            if (!action.isTemplate()) {
                continue;
            }
            DashboardNowColumnModel.TemplatedActionItem item = new DashboardNowColumnModel.TemplatedActionItem();
            item.setDescriptionHtml(action.getNextDescriptionForDisplay(webUser.getProjectContact()));
            item.setActionNextId(action.getActionNextId());
            items.add(item);
        }
        return items;
    }

    private String formatDateOnly(WebUser webUser, Date date) {
        LocalDate localDate = toStoredLocalDate(date, webUser);
        if (localDate == null) {
            return "";
        }
        return localDate.format(DateTimeFormatter.ofPattern(webUser.getDateDisplayPatternWithWeekdayShort()));
    }

    private LocalDate toStoredLocalDate(Date date, WebUser webUser) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return LocalDate.parse(sdf.format(date));
    }

    private boolean isTodayDate(WebUser webUser, Date date) {
        LocalDate actionDate = toStoredLocalDate(date, webUser);
        LocalDate today = toStoredLocalDate(webUser.getToday(), webUser);
        return actionDate != null && today != null && today.equals(actionDate);
    }

    private List<DashboardNowColumnModel.RecentCompletedItem> buildRecentCompleted(WebUser webUser,
            Session dataSession, Project currentProject) {
        Calendar cutoff = webUser.getCalendar();
        cutoff.setTime(webUser.getToday());
        cutoff.add(Calendar.DAY_OF_MONTH, -COMPLETED_DAYS);

        Query query = dataSession.createQuery(
                "from ActionNext an "
                        + "where an.projectId = :projectId "
                        + "and an.nextActionStatusString = :completedStatus "
                        + "and an.nextChangeDate >= :cutoff "
                        + "order by an.nextChangeDate desc");
        query.setParameter("projectId", currentProject.getProjectId());
        query.setParameter("completedStatus", ProjectNextActionStatus.COMPLETED.getId());
        query.setParameter("cutoff", cutoff.getTime());
        query.setMaxResults(COMPLETED_LIMIT);

        @SuppressWarnings("unchecked")
        List<ActionNext> completedActions = query.list();

        List<DashboardNowColumnModel.RecentCompletedItem> items = new ArrayList<>();
        for (ActionNext action : completedActions) {
            DashboardNowColumnModel.RecentCompletedItem item = new DashboardNowColumnModel.RecentCompletedItem();
            item.setActionNextId(action.getActionNextId());
            item.setDateLabel(webUser.getDateFormatService().formatPattern(action.getNextChangeDate(),
                    webUser.getDateDisplayPatternWithWeekdayShort(), webUser.getTimeZone()));
            item.setWhatHappened(n(action.getNextSummary(), n(action.getNextDescription(), "-")));
            items.add(item);
        }
        return items;
    }

    private List<ActionNext> loadOpenProjectActions(Session dataSession, Project currentProject) {
        Query query = dataSession.createQuery(
                "select distinct an from ActionNext an "
                        + "left join fetch an.project "
                        + "left join fetch an.contact "
                        + "left join fetch an.nextProjectContact "
                        + "where an.projectId = :projectId and an.nextDescription <> '' "
                        + "and (an.nextActionStatusString = :readyStatus or an.nextActionStatusString = :proposedStatus)");
        query.setParameter("projectId", currentProject.getProjectId());
        query.setParameter("readyStatus", ProjectNextActionStatus.READY.getId());
        query.setParameter("proposedStatus", ProjectNextActionStatus.PROPOSED.getId());
        query.setMaxResults(OPEN_ACTION_LIMIT);
        @SuppressWarnings("unchecked")
        List<ActionNext> openProjectActions = query.list();
        return openProjectActions;
    }

    private void sortBacklogActions(List<ActionNext> openProjectActions) {
        Collections.sort(openProjectActions, new Comparator<ActionNext>() {
            public int compare(ActionNext left, ActionNext right) {
                Date leftDate = left.getNextActionDate();
                Date rightDate = right.getNextActionDate();
                if (leftDate == null && rightDate != null) {
                    return 1;
                }
                if (leftDate != null && rightDate == null) {
                    return -1;
                }
                if (leftDate != null && rightDate != null) {
                    int dateCompare = leftDate.compareTo(rightDate);
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                }

                Date leftChanged = left.getNextChangeDate();
                Date rightChanged = right.getNextChangeDate();
                if (leftChanged == null && rightChanged == null) {
                    return 0;
                }
                if (leftChanged == null) {
                    return 1;
                }
                if (rightChanged == null) {
                    return -1;
                }
                return rightChanged.compareTo(leftChanged);
            }
        });
    }

    private String loadTodayTimeSpentDisplay(WebUser webUser, Session dataSession, ActionNext currentAction) {
        Query query = dataSession.createQuery(
                "select sum(billMins) from BillEntry where action.actionNextId = :actionNextId "
                        + "and startTime >= :today and startTime < :tomorrow");
        query.setParameter("actionNextId", currentAction.getActionNextId());

        Calendar calendar = webUser.getCalendar();
        calendar.setTime(webUser.getToday());
        query.setParameter("today", calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        query.setParameter("tomorrow", calendar.getTime());

        @SuppressWarnings("unchecked")
        List<Long> billMinsList = query.list();
        int billMins = 0;
        if (billMinsList.size() > 0 && billMinsList.get(0) != null) {
            billMins = billMinsList.get(0).intValue();
        }
        return ActionNext.getTimeForDisplay(billMins);
    }

    private String resolveStatusLabel(ActionNext projectAction) {
        if (projectAction.getNextActionStatus() != null) {
            return projectAction.getNextActionStatus().getLabel();
        }
        return "Unknown";
    }

    private String displayTime(String value) {
        return n(value, "-");
    }

    private String n(String value, String defaultValue) {
        if (value == null || value.trim().equals("")) {
            return defaultValue;
        }
        return value;
    }

    private List<String> parseNotes(String nextNotes) {
        List<String> noteList = new ArrayList<String>();
        if (nextNotes == null || nextNotes.trim().length() == 0) {
            return noteList;
        }
        String[] lines = nextNotes.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            if (trimmed.startsWith("- ")) {
                trimmed = trimmed.substring(2).trim();
            }
            if (trimmed.length() > 0) {
                noteList.add(trimmed);
            }
        }
        return noteList;
    }

    private List<DashboardNowColumnModel.OpenIssueItem> buildOpenIssueItems(WebUser webUser, Session dataSession,
            Project currentProject) {
        ProjectIssueDao dao = new ProjectIssueDao(dataSession);
        List<ProjectIssue> issues = dao.listOpenIssuesForProject(currentProject);
        List<DashboardNowColumnModel.OpenIssueItem> items = new ArrayList<>();
        for (ProjectIssue issue : issues) {
            DashboardNowColumnModel.OpenIssueItem item = new DashboardNowColumnModel.OpenIssueItem();
            item.setProjectIssueId(issue.getProjectIssueId());
            item.setIssueText(n(issue.getIssueText(), ""));
            item.setIssueTypeEmoji(issue.getIssueType().toDisplayEmoji());
            item.setIssueTypeValue(issue.getIssueType().name());
            item.setCreatedDate(issue.getCreatedDate());
            item.setCreatedDisplay(webUser.getDateFormatService().formatPattern(issue.getCreatedDate(),
                    webUser.getDateDisplayPatternWithWeekdayShort(), webUser.getTimeZone()));
            items.add(item);
        }
        return items;
    }

    private List<DashboardNowColumnModel.TakenActionItem> buildTakenToday(WebUser webUser, Session dataSession,
            Project currentProject) {
        Calendar cal = webUser.getCalendar();
        cal.setTime(webUser.getToday());
        Date todayStart = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrowStart = cal.getTime();

        Query query = dataSession.createQuery(
                "select at from ActionTaken at left join fetch at.contact"
                        + " where at.projectId = :projectId"
                        + " and at.actionDate >= :todayStart"
                        + " and at.actionDate < :tomorrowStart"
                        + " order by at.actionDate desc");
        query.setParameter("projectId", currentProject.getProjectId());
        query.setParameter("todayStart", todayStart);
        query.setParameter("tomorrowStart", tomorrowStart);
        @SuppressWarnings("unchecked")
        List<ActionTaken> rows = query.list();
        return buildTakenItemList(webUser, rows);
    }

    private List<DashboardNowColumnModel.TakenActionItem> buildTakenActions(WebUser webUser, Session dataSession,
            Project currentProject) {
        Calendar cutoff = webUser.getCalendar();
        cutoff.setTime(webUser.getToday());
        cutoff.add(Calendar.DAY_OF_MONTH, -COMPLETED_DAYS);

        Query query = dataSession.createQuery(
                "select at from ActionTaken at left join fetch at.contact"
                        + " where at.projectId = :projectId"
                        + " and at.actionDate >= :cutoff"
                        + " order by at.actionDate desc");
        query.setParameter("projectId", currentProject.getProjectId());
        query.setParameter("cutoff", cutoff.getTime());
        @SuppressWarnings("unchecked")
        List<ActionTaken> rows = query.list();
        return buildTakenItemList(webUser, rows);
    }

    private List<DashboardNowColumnModel.TakenActionItem> buildTakenItemList(WebUser webUser,
            List<ActionTaken> rows) {
        List<DashboardNowColumnModel.TakenActionItem> items = new ArrayList<>();
        for (ActionTaken at : rows) {
            DashboardNowColumnModel.TakenActionItem item = new DashboardNowColumnModel.TakenActionItem();
            item.setActionTakenId(at.getActionTakenId());
            item.setDescription(n(at.getActionDescription(), "-"));
            item.setDateLabel(webUser.getDateFormatService().formatPattern(at.getActionDate(),
                    webUser.getDateDisplayPatternWithWeekdayShort(), webUser.getTimeZone()));
            String who = "";
            if (at.getContact() != null) {
                who = n(at.getContact().getName(), "");
            }
            item.setWhoLabel(who);
            items.add(item);
        }
        return items;
    }
}