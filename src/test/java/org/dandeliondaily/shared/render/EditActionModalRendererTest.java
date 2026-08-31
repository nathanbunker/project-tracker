package org.dandeliondaily.shared.render;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EditActionModalRendererTest {

    private EditActionModalRenderer renderer;

    @Before
    public void setUp() {
        renderer = new EditActionModalRenderer();
    }

    @Test
    public void buildModalActionsHtml_omitsCompleteButtonWhenNotConfigured() {
        EditActionModalRenderer.Config config = baseConfig();

        String html = renderer.buildModalActionsHtml(config);

        Assert.assertFalse(html.contains("eaCompleteToggleBtn"));
        Assert.assertFalse(html.contains("eaCompleteSection"));
    }

    @Test
    public void buildModalActionsHtml_rendersCompleteButtonAfterSaveAndStart() {
        EditActionModalRenderer.Config config = baseConfig();
        config.completeOnClick = "eaToggleCompleteSection()";
        config.completeSubmitOnClick = "ddSubmitEdit('complete')";

        String html = renderer.buildModalActionsHtml(config);

        int saveAndStartIndex = html.indexOf("Save and Start");
        int completeButtonIndex = html.indexOf("eaCompleteToggleBtn");
        int cancelIndex = html.indexOf(config.cancelOnClick);

        Assert.assertTrue("Save and Start button must be present", saveAndStartIndex >= 0);
        Assert.assertTrue("Complete button must be present", completeButtonIndex >= 0);
        Assert.assertTrue("Complete button must come after Save and Start", completeButtonIndex > saveAndStartIndex);
        Assert.assertTrue("Complete button must come before Cancel", completeButtonIndex < cancelIndex);
    }

    @Test
    public void buildModalActionsHtml_completeSectionHiddenByDefault() {
        EditActionModalRenderer.Config config = baseConfig();
        config.completeOnClick = "eaToggleCompleteSection()";
        config.completeSubmitOnClick = "ddSubmitEdit('complete')";

        String html = renderer.buildModalActionsHtml(config);

        int sectionIndex = html.indexOf("id=\"eaCompleteSection\"");
        Assert.assertTrue("Complete section must be rendered", sectionIndex >= 0);
        int nextLineEnd = html.indexOf('\n', sectionIndex);
        String openingTagLine = html.substring(sectionIndex, nextLineEnd < 0 ? html.length() : nextLineEnd);
        Assert.assertTrue("Complete section must start hidden", openingTagLine.contains("display:none"));
    }

    @Test
    public void buildModalActionsHtml_completeSectionIncludesExpectedFields() {
        EditActionModalRenderer.Config config = baseConfig();
        config.completeOnClick = "eaToggleCompleteSection()";
        config.completeSubmitOnClick = "ddSubmitEdit('complete')";

        String html = renderer.buildModalActionsHtml(config);

        Assert.assertTrue(html.contains("id=\"eaCompleteDescription\""));
        Assert.assertTrue(html.contains("id=\"eaCompleteDate\""));
        Assert.assertTrue(html.contains("id=\"eaCompleteTime\""));
        Assert.assertTrue(html.contains("id=\"eaCompleteDuration\""));
        Assert.assertTrue(html.contains("id=\"eaCompleteError\""));
    }

    private EditActionModalRenderer.Config baseConfig() {
        EditActionModalRenderer.Config config = new EditActionModalRenderer.Config();
        config.saveOnClick = "ddSubmitEdit('save')";
        config.saveAndStartOnClick = "ddSubmitEdit('saveAndStart')";
        config.cancelOnClick = "ddCloseActionModal('editActionModal')";
        config.deleteOnClick = "ddDeleteAction(event)";
        return config;
    }
}
