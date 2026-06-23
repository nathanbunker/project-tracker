package org.dandeliondaily.dashboard.service;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.Project;

public class ProjectDisplayLabelServiceTest {

    private ProjectDisplayLabelService service;

    @Before
    public void setUp() {
        service = new ProjectDisplayLabelService();
    }

    @Test
    public void buildDisplayName_returnsRawProjectNameWhenNoSharedProjectsResolved() {
        Project privateProject = project(1, "Country Interview");

        String displayName = service.buildDisplayName(privateProject, Collections.<Project>emptyList());

        Assert.assertEquals("Country Interview", displayName);
    }

    @Test
    public void buildDisplayName_returnsPrivateNameWhenSharedProjectExists() {
        Project privateProject = project(1, "Country Interview");
        Project sharedProject = project(2, "NHS England");

        String displayName = service.buildDisplayName(privateProject, Arrays.asList(sharedProject));

        Assert.assertEquals("Country Interview", displayName);
    }

    @Test
    public void buildCompositeDisplayName_fallsBackToPrivateNameWhenSharedNameMissing() {
        Project privateProject = project(1, "Country Interview");
        Project sharedProject = project(2, "");

        String displayName = service.buildCompositeDisplayName(privateProject, sharedProject);

        Assert.assertEquals("Country Interview", displayName);
    }

    private Project project(int projectId, String projectName) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setProjectName(projectName);
        return project;
    }
}