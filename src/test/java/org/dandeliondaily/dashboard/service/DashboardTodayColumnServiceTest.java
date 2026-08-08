package org.dandeliondaily.dashboard.service;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.Project;

public class DashboardTodayColumnServiceTest {

    private DashboardTodayColumnService service;

    @Before
    public void setUp() {
        service = new DashboardTodayColumnService();
    }

    @Test
    public void buildOrderedQuickCaptureNames_usesHandlesWithNameFallbackAndAliases() {
        Project handledProject = project("Building Bridges", "Bridges");
        Project fallbackProject = project("Internal Planning", null);

        LinkedHashSet<String> names = service.buildOrderedQuickCaptureNames(
                Arrays.asList(handledProject, fallbackProject),
                Arrays.asList("Bridges / Shared"));

        Assert.assertEquals(Arrays.asList("Bridges", "Internal Planning", "Bridges / Shared"),
                Arrays.asList(names.toArray(new String[names.size()])));
    }

    private Project project(String projectName, String projectHandle) {
        Project project = new Project();
        project.setProjectName(projectName);
        project.setProjectHandle(projectHandle);
        return project;
    }
}