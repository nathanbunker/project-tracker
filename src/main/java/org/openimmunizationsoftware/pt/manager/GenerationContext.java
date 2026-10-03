package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

public class GenerationContext {

    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String prompt;
    private final List<ActionTaken> completedActions;
    private final Map<Integer, Integer> timeByProject;
    private final Map<Integer, String> projectNames;
    private final Map<Integer, Project> projectsById;
    private final Map<Integer, List<String>> openIssuesByProject;
    private final List<ProjectNarrative> projectNarratives;
    private final List<ActionNext> waitingActions;
    private final List<ActionNext> completedActionDetails;
    private final List<ActionNext> upcomingActions;
    private final List<TrackerNarrative> approvedDailyNarratives;
    private final String timeZoneId;
    private final String weekOutlook;
    private final String nextWeekOutlook;
    private final Map<String, String> monthOutlooks;

    public GenerationContext(LocalDate periodStart, LocalDate periodEnd, String prompt,
            List<ActionTaken> completedActions, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, Map<Integer, Project> projectsById,
            Map<Integer, List<String>> openIssuesByProject, List<ProjectNarrative> projectNarratives,
            List<ActionNext> waitingActions) {
        this(periodStart, periodEnd, prompt, completedActions, timeByProject, projectNames, projectsById,
                openIssuesByProject, projectNarratives, waitingActions, java.util.Collections.<ActionNext>emptyList(),
                java.util.Collections.<ActionNext>emptyList(),
                java.util.Collections.<TrackerNarrative>emptyList(), "UTC");
    }

    public GenerationContext(LocalDate periodStart, LocalDate periodEnd, String prompt,
            List<ActionTaken> completedActions, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, Map<Integer, Project> projectsById,
            Map<Integer, List<String>> openIssuesByProject, List<ProjectNarrative> projectNarratives,
            List<ActionNext> waitingActions, List<ActionNext> completedActionDetails,
            List<ActionNext> upcomingActions, List<TrackerNarrative> approvedDailyNarratives, String timeZoneId) {
        this(periodStart, periodEnd, prompt, completedActions, timeByProject, projectNames, projectsById,
                openIssuesByProject, projectNarratives, waitingActions, completedActionDetails, upcomingActions,
                approvedDailyNarratives, timeZoneId, null, null, java.util.Collections.<String, String>emptyMap());
    }

    /**
     * Full constructor. The outlooks are the user's stated intent: the outlook for
     * the reported week, the one for the following week, and the month outlook(s)
     * the reported week falls in, keyed by a month label. Weekly narratives only.
     */
    public GenerationContext(LocalDate periodStart, LocalDate periodEnd, String prompt,
            List<ActionTaken> completedActions, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, Map<Integer, Project> projectsById,
            Map<Integer, List<String>> openIssuesByProject, List<ProjectNarrative> projectNarratives,
            List<ActionNext> waitingActions, List<ActionNext> completedActionDetails,
            List<ActionNext> upcomingActions, List<TrackerNarrative> approvedDailyNarratives, String timeZoneId,
            String weekOutlook, String nextWeekOutlook, Map<String, String> monthOutlooks) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.prompt = prompt;
        this.completedActions = completedActions;
        this.timeByProject = timeByProject;
        this.projectNames = projectNames;
        this.projectsById = projectsById;
        this.openIssuesByProject = openIssuesByProject;
        this.projectNarratives = projectNarratives;
        this.waitingActions = waitingActions;
        this.completedActionDetails = completedActionDetails;
        this.upcomingActions = upcomingActions;
        this.approvedDailyNarratives = approvedDailyNarratives;
        this.timeZoneId = timeZoneId;
        this.weekOutlook = weekOutlook;
        this.nextWeekOutlook = nextWeekOutlook;
        this.monthOutlooks = monthOutlooks == null ? java.util.Collections.<String, String>emptyMap()
                : monthOutlooks;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<ActionTaken> getCompletedActions() {
        return completedActions;
    }

    public Map<Integer, Integer> getTimeByProject() {
        return timeByProject;
    }

    public Map<Integer, String> getProjectNames() {
        return projectNames;
    }

    public Map<Integer, Project> getProjectsById() {
        return projectsById;
    }

    public Map<Integer, List<String>> getOpenIssuesByProject() {
        return openIssuesByProject;
    }

    public List<ProjectNarrative> getProjectNarratives() {
        return projectNarratives;
    }

    public List<ActionNext> getWaitingActions() {
        return waitingActions;
    }

    public List<ActionNext> getCompletedActionDetails() {
        return completedActionDetails;
    }

    public List<ActionNext> getUpcomingActions() {
        return upcomingActions;
    }

    public List<TrackerNarrative> getApprovedDailyNarratives() {
        return approvedDailyNarratives;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public String getWeekOutlook() {
        return weekOutlook;
    }

    public String getNextWeekOutlook() {
        return nextWeekOutlook;
    }

    public Map<String, String> getMonthOutlooks() {
        return monthOutlooks;
    }
}
