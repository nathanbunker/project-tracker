package org.dandeliondaily.projecthealth.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dandeliondaily.projecthealth.service.ProjectDefinitionImportService.ProjectDefinitionPatch;
import org.junit.Test;

public class ProjectDefinitionImportServiceTest {

    private final ProjectDefinitionImportService service = new ProjectDefinitionImportService();

    @Test
    public void parsesSinglePartialProjectAndJoinsSuccessCriteria() {
        List<ProjectDefinitionPatch> patches = service.parse("{\"projectName\":\"Apollo\","
                + "\"currentFocus\":\"Ship review\",\"successCriteria\":[\"Approved\",\"Released\"]}");

        assertEquals(1, patches.size());
        ProjectDefinitionPatch patch = patches.get(0);
        assertEquals("Apollo", patch.getProjectName());
        assertFalse(patch.isDescriptionPresent());
        assertTrue(patch.isCurrentFocusPresent());
        assertEquals("Ship review", patch.getCurrentFocus());
        assertEquals("Approved\nReleased", patch.getSuccessCriteria());
    }

    @Test
    public void parsesJsonArrayAndExplicitNullForClearing() {
        List<ProjectDefinitionPatch> patches = service.parse("[{\"projectName\":\"One\",\"description\":null},"
                + "{\"projectName\":\"Two\",\"projectOutcome\":\"Done\"}]");

        assertEquals(2, patches.size());
        assertTrue(patches.get(0).isDescriptionPresent());
        assertNull(patches.get(0).getDescription());
        assertFalse(patches.get(0).isProjectOutcomePresent());
        assertEquals("Done", patches.get(1).getProjectOutcome());
    }

    @Test
    public void parsesJsonLines() {
        List<ProjectDefinitionPatch> patches = service.parse(
                "{\"projectName\":\"One\",\"description\":\"First\"}\n"
                        + "{\"projectName\":\"Two\",\"currentFocus\":\"Second\"}");

        assertEquals(2, patches.size());
        assertEquals("First", patches.get(0).getDescription());
        assertEquals("Second", patches.get(1).getCurrentFocus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownFieldsToExposeTypos() {
        service.parse("{\"projectName\":\"One\",\"outcome\":\"Misspelled key\"}");
    }
}
