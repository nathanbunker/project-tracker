package org.openimmunizationsoftware.pt.billing;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.billing.BillingProjectReportFormatter.ProjectReportItem;
import org.openimmunizationsoftware.pt.model.Project;

public class BillingProjectReportFormatterTest {

    @Test
    public void formatProject_includesEveryRequestedProjectField() {
        Project project = new Project();
        project.setProjectName("Vaccine Exchange");
        project.setProjectHandle("VX");
        project.setDescription("Exchange project details");
        project.setCurrentFocusText("Finish onboarding");
        project.setOutcomeText("Reliable exchange");
        project.setSuccessCriteriaText("All partners connected");
        project.setProjectStatus("ACTIVE");
        project.setBillCode("BILL-42");

        ProjectReportItem item = new ProjectReportItem();
        item.setProject(project);
        item.setTags(Arrays.asList("interop", "planning"));
        item.setUpdateEveryDays(13);

        String markdown = new BillingProjectReportFormatter().formatProject(item);

        Assert.assertTrue(markdown.contains("## Vaccine Exchange"));
        Assert.assertTrue(markdown.contains("**Project Name:** Vaccine Exchange"));
        Assert.assertTrue(markdown.contains("**Project Handle:** VX"));
        Assert.assertTrue(markdown.contains("**Tags:** interop, planning"));
        Assert.assertTrue(markdown.contains("### Description\n\nExchange project details"));
        Assert.assertTrue(markdown.contains("### Current Focus\n\nFinish onboarding"));
        Assert.assertTrue(markdown.contains("### Project Outcome\n\nReliable exchange"));
        Assert.assertTrue(markdown.contains("### Success Criteria\n\nAll partners connected"));
        Assert.assertTrue(markdown.contains("**Status:** ACTIVE"));
        Assert.assertTrue(markdown.contains("**Bill Code:** BILL-42"));
        Assert.assertTrue(markdown.contains("**Update Every:** Two Weeks"));
    }
}