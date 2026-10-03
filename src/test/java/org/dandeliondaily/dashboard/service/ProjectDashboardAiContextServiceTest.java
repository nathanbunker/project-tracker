package org.dandeliondaily.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.dandeliondaily.outlook.service.PlanningOutlookService.OutlookResult;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;
import org.openimmunizationsoftware.pt.model.ProjectAiNote;

public class ProjectDashboardAiContextServiceTest {

    private static final Function<Date, String> FIXED_LABEL = new Function<Date, String>() {
        @Override
        public String apply(Date date) {
            return "Fri 10/02";
        }
    };

    @Test
    public void aiThoughtsAreLabeledAsAssistantObservations() {
        StringBuilder sb = new StringBuilder();
        ProjectDashboardAiContextService.appendAiThoughts(sb, Collections.singletonList(note("  check the IG  ")),
                FIXED_LABEL);
        String text = sb.toString();
        Assert.assertTrue(text, text.contains("AI Thoughts ("));
        Assert.assertTrue(text, text.contains("not established facts or decisions the user made"));
        Assert.assertTrue(text, text.contains("- [Fri 10/02] check the IG\n"));
    }

    @Test
    public void aiThoughtsSayNoneWhenEmpty() {
        StringBuilder sb = new StringBuilder();
        ProjectDashboardAiContextService.appendAiThoughts(sb, new ArrayList<ProjectAiNote>(), FIXED_LABEL);
        Assert.assertTrue(sb.toString().endsWith("- (none)\n"));
    }

    @Test
    public void aiThoughtsAreCapped() {
        List<ProjectAiNote> notes = new ArrayList<ProjectAiNote>();
        for (int i = 0; i < ProjectDashboardAiContextService.MAX_AI_THOUGHTS + 5; i++) {
            notes.add(note("thought " + i));
        }
        StringBuilder sb = new StringBuilder();
        ProjectDashboardAiContextService.appendAiThoughts(sb, notes, FIXED_LABEL);
        Assert.assertTrue(sb.toString().contains("thought " + (ProjectDashboardAiContextService.MAX_AI_THOUGHTS - 1)));
        Assert.assertFalse(sb.toString().contains("thought " + ProjectDashboardAiContextService.MAX_AI_THOUGHTS));
    }

    @Test
    public void outlooksIncludeTextAndMarkMissingPeriods() {
        PlanningOutlook monthOutlook = new PlanningOutlook();
        monthOutlook.setOutlookText("October: finish the Conformance Reboot.");
        OutlookResult month = new OutlookResult(monthOutlook, "MONTH", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), false);
        OutlookResult week = new OutlookResult(null, "WEEK", LocalDate.of(2026, 9, 27), LocalDate.of(2026, 10, 3),
                false);
        StringBuilder sb = new StringBuilder();
        ProjectDashboardAiContextService.appendOutlooks(sb, month, week);
        String text = sb.toString();
        Assert.assertTrue(text, text.contains("use only what is relevant to this project"));
        Assert.assertTrue(text, text.contains("This month (2026-10-01 to 2026-10-31): \nOctober: finish the "
                + "Conformance Reboot.\n"));
        Assert.assertTrue(text, text.contains("This week (2026-09-27 to 2026-10-03): (none recorded)\n"));
    }

    private static ProjectAiNote note(String text) {
        ProjectAiNote note = new ProjectAiNote();
        note.setNoteText(text);
        note.setCreatedAt(new Date());
        return note;
    }
}
