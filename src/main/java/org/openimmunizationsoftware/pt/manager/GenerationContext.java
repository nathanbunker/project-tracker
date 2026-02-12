package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;

public class GenerationContext {

    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String prompt;
    private final List<ProjectActionTaken> completedActions;
    private final Map<Integer, Integer> timeByProject;
    private final Map<Integer, String> projectNames;
    private final List<ProjectNarrative> projectNarratives;
    private final List<ProjectActionNext> waitingActions;

    public GenerationContext(LocalDate periodStart, LocalDate periodEnd, String prompt,
            List<ProjectActionTaken> completedActions, Map<Integer, Integer> timeByProject,
            Map<Integer, String> projectNames, List<ProjectNarrative> projectNarratives,
            List<ProjectActionNext> waitingActions) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.prompt = prompt;
        this.completedActions = completedActions;
        this.timeByProject = timeByProject;
        this.projectNames = projectNames;
        this.projectNarratives = projectNarratives;
        this.waitingActions = waitingActions;
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

    public List<ProjectActionTaken> getCompletedActions() {
        return completedActions;
    }

    public Map<Integer, Integer> getTimeByProject() {
        return timeByProject;
    }

    public Map<Integer, String> getProjectNames() {
        return projectNames;
    }

    public List<ProjectNarrative> getProjectNarratives() {
        return projectNarratives;
    }

    public List<ProjectActionNext> getWaitingActions() {
        return waitingActions;
    }
}
