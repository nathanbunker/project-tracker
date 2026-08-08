package org.dandeliondaily.weeklyreport.service;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

public class WeeklyReportDataServiceTest {
    @Test
    public void calculatesPercentAgainstAllRoundedWorkedMinutes() {
        Assert.assertEquals(new BigDecimal("40.00"), WeeklyReportDataService.percent(60, 150));
        Assert.assertEquals(new BigDecimal("0.00"), WeeklyReportDataService.percent(60, 0));
    }

    @Test
    public void formatsCompletedActionAsPrintableFirstPersonStatement() {
        ProjectContact contact = new ProjectContact();
        contact.setContactId(7);
        WebUser owner = new WebUser();
        owner.setProjectContact(contact);
        ActionNext action = new ActionNext();
        action.setContact(contact);
        action.setNextActionType(ProjectNextActionType.WILL);
        action.setNextDescription("work on InteropHub");

        Assert.assertEquals("I will work on InteropHub",
                WeeklyReportDataService.printableDescription(action, owner));
    }
}