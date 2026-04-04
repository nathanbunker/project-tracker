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
}