package org.openimmunizationsoftware.pt.api.v1.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.api.v1.resource.dto.SyncProjectUpsertItem;
import org.openimmunizationsoftware.pt.model.Project;

public class ExternalSyncServiceTest {

    private ExternalSyncService service;

    @Before
    public void setUp() {
        service = new ExternalSyncService();
    }

    @Test
    public void applyProjectFields_defaultsOmittedHandleToNameOnCreate() {
        Project project = new Project();
        SyncProjectUpsertItem item = new SyncProjectUpsertItem();
        item.setProjectName("France");

        service.applyProjectFields(project, item, true);

        Assert.assertEquals("France", project.getProjectHandle());
    }

    @Test
    public void applyProjectFields_preservesHandleWhenOmittedOnUpdate() {
        Project project = new Project();
        project.setProjectHandle("Existing");
        SyncProjectUpsertItem item = new SyncProjectUpsertItem();
        item.setProjectName("Renamed Project");

        service.applyProjectFields(project, item, false);

        Assert.assertEquals("Existing", project.getProjectHandle());
    }

    @Test
    public void applyProjectFields_doesNotDefaultExplicitBlankHandleOnCreate() {
        Project project = new Project();
        SyncProjectUpsertItem item = new SyncProjectUpsertItem();
        item.setProjectName("France");
        item.setProjectHandle("");

        service.applyProjectFields(project, item, true);

        Assert.assertNull(project.getProjectHandle());
    }
}