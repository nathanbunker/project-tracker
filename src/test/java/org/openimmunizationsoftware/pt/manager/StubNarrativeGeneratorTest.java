package org.openimmunizationsoftware.pt.manager;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;

public class StubNarrativeGeneratorTest {

    @Test
    public void generateDailyMarkdownProducesHeadings() {
        LocalDate day = LocalDate.of(2026, 2, 10);
        List<ProjectActionTaken> completed = Collections.emptyList();
        Map<Integer, Integer> timeByProject = new LinkedHashMap<Integer, Integer>();
        Map<Integer, String> projectNames = new LinkedHashMap<Integer, String>();
        List<ProjectNarrative> projectNarratives = Collections.emptyList();
        List<ProjectActionNext> waiting = Collections.emptyList();

        GenerationContext ctx = new GenerationContext(day, day, "", completed, timeByProject, projectNames,
                projectNarratives, waiting);
        NarrativeGenerator generator = new StubNarrativeGenerator();

        String markdown = generator.generateDailyMarkdown(ctx);

        Assert.assertTrue(markdown.contains("# Summary"));
        Assert.assertTrue(markdown.contains("# Time By Project"));
        Assert.assertTrue(markdown.contains("# Completed Actions"));
    }
}
