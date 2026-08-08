package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

public class StubNarrativeGeneratorTest {

    @Test
    public void generateDailyMarkdownProducesHeadings() {
        LocalDate day = LocalDate.of(2026, 2, 10);
        List<ActionTaken> completed = Collections.emptyList();
        Map<Integer, Integer> timeByProject = new LinkedHashMap<Integer, Integer>();
        Map<Integer, String> projectNames = new LinkedHashMap<Integer, String>();
        Map<Integer, Project> projectsById = new LinkedHashMap<Integer, Project>();
        Map<Integer, List<String>> openIssuesByProject = new LinkedHashMap<Integer, List<String>>();
        List<ProjectNarrative> projectNarratives = Collections.emptyList();
        List<ActionNext> waiting = Collections.emptyList();

        GenerationContext ctx = new GenerationContext(day, day, "", completed, timeByProject, projectNames,
                projectsById, openIssuesByProject, projectNarratives, waiting);
        NarrativeGenerator generator = new StubNarrativeGenerator();

        String markdown = generator.generateMarkdown("DAILY", ctx);

        Assert.assertTrue(markdown.contains("# Summary"));
        Assert.assertTrue(markdown.contains("# Time By Project"));
        Assert.assertTrue(markdown.contains("# Completed Actions"));
    }

    @Test
    public void weeklyPromptRequestsSupervisorBriefingWithoutRepeatingTables() {
        LocalDate sunday = LocalDate.of(2026, 8, 2);
        GenerationContext ctx = new GenerationContext(sunday, sunday.plusDays(6), "",
                Collections.<ActionTaken>emptyList(), Collections.<Integer, Integer>emptyMap(),
                Collections.<Integer, String>emptyMap(), Collections.<Integer, Project>emptyMap(),
                Collections.<Integer, List<String>>emptyMap(), Collections.<ProjectNarrative>emptyList(),
                Collections.<ActionNext>emptyList());

        String prompt = OpenAiNarrativeGenerator.buildPromptForInspection("WEEKLY", ctx);

        Assert.assertTrue(prompt.contains("## Supervisor Attention"));
        Assert.assertTrue(prompt.contains("## Next Week"));
        Assert.assertTrue(prompt.contains("Do not repeat those tables"));
        Assert.assertTrue(prompt.contains("Period: 2026-08-02 through 2026-08-08"));
    }

    @Test
    public void weeklyPayloadUsesApprovedBriefingsOutcomesFocusAndUpcomingWork() {
        LocalDate sunday = LocalDate.of(2026, 8, 2);
        Project project = new Project();
        project.setProjectId(7);
        project.setProjectName("Weekly Reports");
        project.setOutcomeText("Supervisors can understand the week quickly.");
        project.setCurrentFocusText("Validate the report with a supervisor.");

        TrackerNarrative daily = new TrackerNarrative();
        daily.setPeriodStart(java.sql.Date.valueOf(sunday.plusDays(1)));
        daily.setMarkdownFinal("## Summary\nShipped the report shell.");

        ActionNext completed = new ActionNext();
        completed.setProject(project);
        completed.setNextSummary("The access-key workflow passed validation.");

        ActionNext upcoming = new ActionNext();
        upcoming.setProject(project);
        upcoming.setNextDescription("Review the weekly briefing with a supervisor.");
        upcoming.setNextActionDate(java.sql.Date.valueOf(sunday.plusWeeks(1)));

        ProjectNarrative note = new ProjectNarrative();
        note.setProject(project);
        note.setNarrativeVerb(ProjectNarrativeVerb.NOTE);
        note.setNarrativeText("Reviewed/no comments");

        ProjectNarrative risk = new ProjectNarrative();
        risk.setProject(project);
        risk.setNarrativeVerb(ProjectNarrativeVerb.RISK);
        risk.setNarrativeText("Production migration state is not verified.");

        Map<Integer, Integer> time = new LinkedHashMap<Integer, Integer>();
        time.put(Integer.valueOf(7), Integer.valueOf(180));
        Map<Integer, String> names = new LinkedHashMap<Integer, String>();
        names.put(Integer.valueOf(7), project.getProjectName());
        Map<Integer, Project> projects = new LinkedHashMap<Integer, Project>();
        projects.put(Integer.valueOf(7), project);

        GenerationContext ctx = new GenerationContext(sunday, sunday.plusDays(6), "",
                Collections.<ActionTaken>emptyList(), time, names, projects,
                Collections.<Integer, List<String>>emptyMap(), Arrays.asList(note, risk),
                Collections.<ActionNext>emptyList(), Arrays.asList(completed), Arrays.asList(upcoming),
                Arrays.asList(daily), "America/Denver");

        String prompt = OpenAiNarrativeGenerator.buildPromptForInspection("WEEKLY", ctx);

        Assert.assertTrue(prompt.contains("Shipped the report shell"));
        Assert.assertTrue(prompt.contains("access-key workflow passed validation"));
        Assert.assertTrue(prompt.contains("Validate the report with a supervisor"));
        Assert.assertTrue(prompt.contains("Production migration state is not verified"));
        Assert.assertTrue(prompt.contains("Review the weekly briefing with a supervisor"));
        Assert.assertFalse(prompt.contains("Reviewed/no comments"));
        Assert.assertTrue(prompt.contains("Timezone: America/Denver"));
    }

    @Test
    public void dailyPromptRetainsDailyStructure() {
        LocalDate day = LocalDate.of(2026, 8, 8);
        GenerationContext ctx = new GenerationContext(day, day, "", Collections.<ActionTaken>emptyList(),
                Collections.<Integer, Integer>emptyMap(), Collections.<Integer, String>emptyMap(),
                Collections.<Integer, Project>emptyMap(), Collections.<Integer, List<String>>emptyMap(),
                Collections.<ProjectNarrative>emptyList(), Collections.<ActionNext>emptyList());

        String prompt = OpenAiNarrativeGenerator.buildPromptForInspection("DAILY", ctx);

        Assert.assertTrue(prompt.contains("# Daily Summary - {DATE}"));
        Assert.assertFalse(prompt.contains("## Supervisor Attention"));
    }
}
