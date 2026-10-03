package org.dandeliondaily.planahead.render;

import java.time.LocalDate;

import org.dandeliondaily.outlook.service.PlanningOutlookService.OutlookResult;
import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.junit.Assert;
import org.openimmunizationsoftware.pt.model.PlanningOutlook;
import org.junit.Before;
import org.junit.Test;

public class PlanAheadPageRendererTest {

    private PlanAheadPageRenderer renderer;

    @Before
    public void setUp() {
        renderer = new PlanAheadPageRenderer();
    }

    @Test
    public void renderTemplateSelectionCellHtml_rendersValidToggleHandler() {
        PlanAheadBoardModel.TemplateCardModel templateCard = new PlanAheadBoardModel.TemplateCardModel();
        templateCard.setTemplateActionNextId(42);
        templateCard.setDescription("Prep status update");

        PlanAheadBoardModel.DayHeaderModel dayHeader = new PlanAheadBoardModel.DayHeaderModel();
        dayHeader.setDayKey("2026-04-04");

        String html = renderer.renderTemplateSelectionCellHtml(templateCard, dayHeader);

        Assert.assertTrue(html.contains("paToggleTemplateDay(42,'2026-04-04', this)"));
        Assert.assertFalse(html.contains("paToggleTemplateDay(42,'\"2026-04-04'"));
    }

    @Test
    public void renderTemplateSelectionCellHtml_rendersTemplateDescriptionHtml() {
        PlanAheadBoardModel.TemplateCardModel templateCard = new PlanAheadBoardModel.TemplateCardModel();
        templateCard.setTemplateActionNextId(42);
        templateCard.setDescription("<i>I will</i> Prep status update");

        PlanAheadBoardModel.DayHeaderModel dayHeader = new PlanAheadBoardModel.DayHeaderModel();
        dayHeader.setDayKey("2026-04-04");

        String html = renderer.renderTemplateSelectionCellHtml(templateCard, dayHeader);

        Assert.assertTrue(html.contains("<i>I will</i> Prep status update"));
    }

    // -----------------------------------------------------------------------
    // Edit modal structure (Step 21 — renderer-level assertions)
    // -----------------------------------------------------------------------

    @Test
    public void editModal_containsModalOverlayAndId() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditModal\""));
        Assert.assertTrue(html.contains("class=\"pa-modal-overlay\""));
    }

    @Test
    public void editModal_containsDateField() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditNextActionDate\""));
    }

    @Test
    public void editModal_containsActionTypeChipWrapper() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditTypeWrap\""));
        Assert.assertTrue(html.contains("class=\"pa-action-type-chips\""));
    }

    @Test
    public void editModal_containsAllElevenActionTypeChips() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("data-action-type=\"WILL\""));
        Assert.assertTrue(html.contains("data-action-type=\"MIGHT\""));
        Assert.assertTrue(html.contains("data-action-type=\"WOULD_LIKE_TO\""));
        Assert.assertTrue(html.contains("data-action-type=\"WILL_CONTACT\""));
        Assert.assertTrue(html.contains("data-action-type=\"WILL_MEET\""));
        Assert.assertTrue(html.contains("data-action-type=\"REVIEW\""));
        Assert.assertTrue(html.contains("data-action-type=\"DOCUMENT\""));
        Assert.assertTrue(html.contains("data-action-type=\"WILL_FOLLOW_UP\""));
        Assert.assertTrue(html.contains("data-action-type=\"COMMITTED_TO\""));
        Assert.assertTrue(html.contains("data-action-type=\"GOAL\""));
        Assert.assertTrue(html.contains("data-action-type=\"WAITING\""));
    }

    @Test
    public void editModal_chipButtonsHaveCorrectClasses() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("class=\"pa-chip pa-action-type-btn\""));
    }

    @Test
    public void editModal_containsHiddenActionTypeInput() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditNextActionType\""));
    }

    @Test
    public void editModal_containsTimeSlotSection() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditSlotWrap\""));
        Assert.assertTrue(html.contains("id=\"paEditTimeSlot\""));
    }

    @Test
    public void editModal_timeSlotSectionInitiallyHidden() {
        String html = renderer.buildEditModalBodyHtml();
        // The slot wrapper should start hidden (shown only in personal mode via JS)
        Assert.assertTrue(html.contains("id=\"paEditSlotWrap\" style=\"display:none;\""));
    }

    @Test
    public void editModal_containsContactSelectField() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditContactWrap\""));
        Assert.assertTrue(html.contains("id=\"paEditNextContactId\""));
    }

    @Test
    public void editModal_containsTimeEstimateField() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditNextTimeEstimate\""));
        Assert.assertTrue(html.contains("id=\"paEditEstimateWrap\""));
    }

    @Test
    public void editModal_containsLinkUrlField() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditLinkUrl\""));
    }

    @Test
    public void editModal_containsAdvancedSection() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditAdvancedSection\""));
        Assert.assertTrue(html.contains("class=\"pa-advanced-section\""));
    }

    @Test
    public void editModal_advancedSectionContainsTargetAndDeadline() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditNextTargetDate\""));
        Assert.assertTrue(html.contains("id=\"paEditNextDeadlineDate\""));
    }

    @Test
    public void editModal_advancedSectionContainsNotesField() {
        String html = renderer.buildEditModalBodyHtml();
        Assert.assertTrue(html.contains("id=\"paEditNextNote\""));
    }

    @Test
    public void computeFirstDisplayedDayTotal_returnsFirstDayBillMins() {
        PlanAheadBoardModel boardModel = new PlanAheadBoardModel();

        PlanAheadBoardModel.DayHeaderModel dayOne = new PlanAheadBoardModel.DayHeaderModel();
        dayOne.setBillMins(390);
        PlanAheadBoardModel.DayHeaderModel dayTwo = new PlanAheadBoardModel.DayHeaderModel();
        dayTwo.setBillMins(510);

        boardModel.getDayHeaders().add(dayOne);
        boardModel.getDayHeaders().add(dayTwo);

        Assert.assertEquals(390, renderer.computeFirstDisplayedDayTotal(boardModel));
    }

    @Test
    public void renderCompletedCellHtml_isReadOnlyAndNotDraggable() {
        PlanAheadBoardModel.TimeSpentRowModel completedRow = new PlanAheadBoardModel.TimeSpentRowModel();
        completedRow.setTodayKey("2026-05-26");

        PlanAheadBoardModel.CardModel card = new PlanAheadBoardModel.CardModel();
        card.setActionNextId(77);
        card.setDescription("Wrap up ticket");
        card.setProjectName("Tracker");
        card.setEstimateMins(45);
        card.setEstimateDisplay("0:45");
        completedRow.getCards().add(card);

        String html = renderer.renderCompletedCellHtml(completedRow, true);

        Assert.assertTrue(html.contains("pa-card-completed"));
        Assert.assertTrue(html.contains("pa-card-est-readonly"));
        Assert.assertFalse(html.contains("draggable=\"true\""));
        Assert.assertFalse(html.contains("pa-card-edit-link"));
        Assert.assertFalse(html.contains("pa-card-est-editable"));
    }

    @Test
    public void renderOverdueCellHtml_includesRowKeyForTotalRecalc() {
        PlanAheadBoardModel.OverdueRowModel overdueRow = new PlanAheadBoardModel.OverdueRowModel();
        overdueRow.setTodayKey("2026-05-26");

        PlanAheadBoardModel.CardModel card = new PlanAheadBoardModel.CardModel();
        card.setActionNextId(88);
        card.setDescription("Late task");
        card.setProjectName("Tracker");
        card.setEstimateMins(30);
        card.setEstimateDisplay("0:30");
        overdueRow.getCards().add(card);

        String html = renderer.renderOverdueCellHtml(overdueRow, true);

        Assert.assertTrue(html.contains("data-row=\"overdue\""));
    }

    @Test
    public void renderCompletedRowLabel_showsTimeSpent() {
        PlanAheadBoardModel boardModel = new PlanAheadBoardModel();
        boardModel.setMode("WORK");

        PlanAheadBoardModel.DayHeaderModel dayHeader = new PlanAheadBoardModel.DayHeaderModel();
        dayHeader.setDayKey("2026-05-26");
        dayHeader.setBillMins(480);
        boardModel.getDayHeaders().add(dayHeader);

        PlanAheadBoardModel.TimeSpentRowModel completedRow = new PlanAheadBoardModel.TimeSpentRowModel();
        completedRow.setTodayKey("2026-05-26");
        PlanAheadBoardModel.CardModel card = new PlanAheadBoardModel.CardModel();
        card.setActionNextId(99);
        card.setDescription("Finished work");
        card.setProjectName("Tracker");
        card.setEstimateMins(30);
        card.setEstimateDisplay("0:30");
        completedRow.getCards().add(card);
        boardModel.setTimeSpentRow(completedRow);

        String html = renderer.renderCompletedCellHtml(boardModel.getTimeSpentRow(), true);

        Assert.assertTrue(html.contains("data-row=\"completed\""));
        Assert.assertTrue(html.contains("pa-card-completed"));
    }

    @Test
    public void outlookBanner_isOpenAndEscapedWhenWeekHasOutlook() {
        PlanningOutlook weekText = new PlanningOutlook();
        weekText.setOutlookText("Ship <3.5> & review the narrative");
        OutlookResult week = new OutlookResult(weekText, "WEEK", LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 10), false);
        OutlookResult month = new OutlookResult(null, "MONTH", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), false);

        String html = renderer.renderOutlookBannerHtml(week, month);

        Assert.assertTrue(html, html.contains("<details class=\"pa-outlook\" open>"));
        Assert.assertTrue(html, html.contains("Week of Oct 4 - Oct 10"));
        Assert.assertTrue(html, html.contains("Ship &lt;3.5&gt; &amp; review the narrative"));
        Assert.assertTrue(html, html.contains("href=\"OutlooksServlet#week-2026-10-04\">Edit</a>"));
        Assert.assertTrue(html, html.contains("October 2026 outlook &middot; <em>none written yet</em>"));
    }

    @Test
    public void outlookBanner_isCollapsedWithWriteLinkWhenWeekHasNoOutlook() {
        OutlookResult week = new OutlookResult(null, "WEEK", LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 10), false);

        String html = renderer.renderOutlookBannerHtml(week, null);

        Assert.assertTrue(html, html.contains("<details class=\"pa-outlook\">"));
        Assert.assertTrue(html, html.contains("none written yet"));
        Assert.assertTrue(html, html.contains("href=\"OutlooksServlet#week-2026-10-04\">Write one</a>"));
        Assert.assertFalse(html, html.contains("pa-outlook-month"));
    }
}
