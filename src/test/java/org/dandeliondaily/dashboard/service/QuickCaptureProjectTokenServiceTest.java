package org.dandeliondaily.dashboard.service;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.Project;

public class QuickCaptureProjectTokenServiceTest {

    private QuickCaptureProjectTokenService service;

    @Before
    public void setUp() {
        service = new QuickCaptureProjectTokenService();
    }

    @Test
    public void getToken_prefersHandleAndFallsBackToName() {
        Assert.assertEquals("Bridges", service.getToken(project("Building Bridges", " Bridges ")));
        Assert.assertEquals("Building Bridges", service.getToken(project(" Building Bridges ", null)));
    }

    @Test
    public void findFirstByToken_isCaseInsensitiveAndKeepsListOrder() {
        Project first = project("First Project", "Shared");
        Project second = project("Second Project", "shared");

        Project found = service.findFirstByToken(Arrays.asList(first, second), " SHARED ");

        Assert.assertSame(first, found);
    }

    private Project project(String projectName, String projectHandle) {
        Project project = new Project();
        project.setProjectName(projectName);
        project.setProjectHandle(projectHandle);
        return project;
    }
}