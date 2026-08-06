package org.dandeliondaily.dashboard.service;

import java.sql.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;

public class ActionRecoveryServiceTest {

    private ActionRecoveryService service;

    @Before
    public void setUp() {
        service = new ActionRecoveryService();
    }

    @Test
    public void isAvailable_returnsTrueBeforeExpiry() {
        Assert.assertTrue(service.isAvailable(61000L, 60000L));
    }

    @Test
    public void isAvailable_returnsFalseAtExpiry() {
        Assert.assertFalse(service.isAvailable(60000L, 60000L));
    }

    @Test
    public void applyRestore_reopensActionForToday() {
        ActionNext action = new ActionNext();
        action.setNextActionStatus(ProjectNextActionStatus.CANCELLED);
        action.setCompletionOrder(7);
        Date today = Date.valueOf("2026-08-06");
        Date now = Date.valueOf("2026-08-06");

        service.applyRestore(action, today, now);

        Assert.assertEquals(ProjectNextActionStatus.READY, action.getNextActionStatus());
        Assert.assertEquals(today, action.getNextActionDate());
        Assert.assertEquals(0, action.getCompletionOrder());
        Assert.assertEquals(now, action.getNextChangeDate());
    }
}