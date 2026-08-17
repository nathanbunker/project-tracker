package org.dandeliondaily.weeklyreport.render;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ActivityItem;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.AllocationRow;
import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel.ProjectActivity;
import org.dandeliondaily.weeklyreport.model.WeeklyTimeSummary;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

public class WeeklyReportRendererTest {
    @Test
    public void rendersRequiredSectionsAndPreservesPublicCapability() {
        LocalDate sunday = LocalDate.of(2026, 7, 26);
        WeeklyReportViewModel model = new WeeklyReportViewModel();
        WeeklyReport report = new WeeklyReport();
        report.setReportName("AIRA & Weekly");
        report.setRootBillCode("AIRA");
        model.setReport(report);
        model.setWeekStart(sunday);
        WeeklyTimeSummary selected = new WeeklyTimeSummary(sunday);
        selected.setAllWorkedMinutes(150);
        selected.setScopedMinutes(60);
        model.setSelectedWeek(selected);
        model.getHistory().add(selected);
        model.setApprovedNarrativeHtml("<p><strong>Approved</strong></p>");
        model.setFourWeekAllWorkedMinutes(150);
        model.setFiscalYearAllWorkedMinutes(300);

        AllocationRow row = new AllocationRow();
        row.setBillCode("AIRA-1");
        row.setFundingSource("Federal");
        row.setWeekMinutes(60);
        row.setFourWeekMinutes(60);
        row.setFiscalYearMinutes(120);
        row.setWeekPercent(new BigDecimal("40.00"));
        row.setFourWeekPercent(new BigDecimal("40.00"));
        row.setFiscalYearPercent(new BigDecimal("40.00"));
        model.getAllocationRows().put(row.getBillCode(), row);

        Project project = new Project();
        project.setProjectId(7);
        project.setProjectName("Immunization Registry");
        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(7);
        activity.setProject(project);
        activity.setBillCode("AIRA-1");
        activity.setRoundedMinutes(60);
        model.getProjectActivities().put(WeeklyReportViewModel.projectKey(7, "AIRA-1"), activity);

        WebUser owner = new WebUser();
        owner.setFirstName("Report");
        owner.setLastName("Owner");
        StringWriter buffer = new StringWriter();
        new WeeklyReportRenderer().render(new PrintWriter(buffer), model, owner, sunday,
                "PublicWeeklyReportServlet", "public key");

        String html = buffer.toString();
        Assert.assertTrue(html.contains("<h1>Report Owner</h1>"));
        Assert.assertTrue(html.contains("Prepared for AIRA &amp; Weekly"));
        Assert.assertTrue(html.contains("AIRA &amp; Weekly"));
        Assert.assertTrue(html.contains("Weekly Briefing"));
        Assert.assertTrue(html.contains("Funding Source Summary"));
        Assert.assertTrue(html.contains("Billing Allocation"));
        Assert.assertTrue(html.contains("Project Activity"));
        Assert.assertTrue(html.contains("Eight-Week History"));
        Assert.assertTrue(html.contains("key=public+key"));
        Assert.assertTrue(html.contains("class=\"wr-code-toggle\""));
        Assert.assertTrue(html.contains("class=\"wr-alloc-detail\""));
        Assert.assertTrue(html.contains("PublicWeeklyReportProjectServlet"));
        Assert.assertFalse(html.contains("WeeklyReportBillingServlet"));
        Assert.assertTrue(html.contains("class=\"wr-page\""));
        Assert.assertTrue(html.contains("class=\"wr-header\""));
        Assert.assertTrue(html.contains("class=\"wr-table\""));
        Assert.assertTrue(html.contains("Jul 26, 2026"));
    }

    @Test
    public void rendersCompletedActionAndModernProjectDetail() {
        LocalDate sunday = LocalDate.of(2026, 7, 26);
        WeeklyReportViewModel model = new WeeklyReportViewModel();
        WeeklyReport report = new WeeklyReport();
        report.setReportName("AIRA Weekly");
        model.setReport(report);
        model.setWeekStart(sunday);
        WeeklyTimeSummary selected = new WeeklyTimeSummary(sunday);
        model.setSelectedWeek(selected);

        Project project = new Project();
        project.setProjectId(42);
        project.setProjectName("InteropHub");
        project.setCurrentFocusText("Production readiness");
        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(42);
        activity.setProject(project);
        activity.setBillCode("AIRA");
        activity.setRoundedMinutes(120);
        activity.getCompletedItems().add(new ActivityItem(new Date(0), "I will work on InteropHub", null, true));
        model.getProjectActivities().put(WeeklyReportViewModel.projectKey(42, "AIRA"), activity);

        WebUser owner = new WebUser();
        StringWriter reportBuffer = new StringWriter();
        new WeeklyReportRenderer().render(new PrintWriter(reportBuffer), model, owner, sunday,
                "WeeklyReportServlet", null);
        Assert.assertTrue(reportBuffer.toString().contains(
                "<strong>Completed - </strong>I will work on InteropHub"));

        StringWriter detailBuffer = new StringWriter();
        new WeeklyReportRenderer().renderProjectDetail(new PrintWriter(detailBuffer), model, owner, 42, "AIRA",
                "WeeklyReportServlet?weeklyReportId=1&amp;week=2026-07-26");
        String detailHtml = detailBuffer.toString();
        Assert.assertTrue(detailHtml.contains("class=\"wr-page\""));
        Assert.assertTrue(detailHtml.contains("Weekly report / Project detail"));
        Assert.assertTrue(detailHtml.contains("Project Summary"));
        Assert.assertTrue(detailHtml.contains("Completed This Week"));
        Assert.assertTrue(detailHtml.contains("<strong>Completed - </strong>I will work on InteropHub"));
    }

    @Test
    public void usesLinkedContactNameForLegacyOwner() {
        LocalDate sunday = LocalDate.of(2026, 7, 26);
        WeeklyReportViewModel model = new WeeklyReportViewModel();
        WeeklyReport report = new WeeklyReport();
        report.setReportName("AIRA Weekly");
        model.setReport(report);
        model.setWeekStart(sunday);
        model.setSelectedWeek(new WeeklyTimeSummary(sunday));

        ProjectContact contact = new ProjectContact();
        contact.setNameFirst("Nathan");
        contact.setNameLast("Bunker");
        WebUser owner = new WebUser();
        owner.setUsername("nbunker_aira");
        owner.setProjectContact(contact);

        StringWriter buffer = new StringWriter();
        new WeeklyReportRenderer().render(new PrintWriter(buffer), model, owner, sunday,
                "WeeklyReportServlet", null);

        Assert.assertTrue(buffer.toString().contains("<h1>Nathan Bunker</h1>"));
        Assert.assertFalse(buffer.toString().contains("<h1>nbunker_aira</h1>"));
    }
}