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
}
