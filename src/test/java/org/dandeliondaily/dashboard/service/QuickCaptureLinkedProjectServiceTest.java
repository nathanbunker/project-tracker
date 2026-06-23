package org.dandeliondaily.dashboard.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.Project;

public class QuickCaptureLinkedProjectServiceTest {

    private QuickCaptureLinkedProjectService service;

    @Before
    public void setUp() {
        service = new QuickCaptureLinkedProjectService();
    }

    @Test
    public void createAliasResolution_targetsPrivateAndSelectedSharedOnly() {
        Project privateProject = project(10, "Country Interview");
        Project sharedProject = project(20, "Mexico");

        QuickCaptureLinkedProjectService.LinkedAliasResolution resolution = service.createAliasResolution(
                privateProject,
                sharedProject);

        Assert.assertEquals("Country Interview / Mexico", resolution.getAliasLabel());
        Assert.assertSame(privateProject, resolution.getPrivateProject());

        List<Project> targetProjects = resolution.getTargetProjects();
        Assert.assertEquals(2, targetProjects.size());
        Assert.assertSame(privateProject, targetProjects.get(0));
        Assert.assertSame(sharedProject, targetProjects.get(1));
    }

    private Project project(int projectId, String projectName) {
        Project project = new Project();
        project.setProjectId(projectId);
        project.setProjectName(projectName);
        return project;
    }
}
