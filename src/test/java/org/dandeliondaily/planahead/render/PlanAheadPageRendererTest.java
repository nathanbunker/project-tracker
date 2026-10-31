package org.dandeliondaily.planahead.render;

import org.dandeliondaily.planahead.model.PlanAheadBoardModel;
import org.junit.Assert;
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
}
