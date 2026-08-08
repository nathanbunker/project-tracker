package org.openimmunizationsoftware.pt.servlet;

import java.sql.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.WebUser;

public class BillFundingSourceEditServletTest {

    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void dateOnlyValuesRoundTripWithoutTimezoneAdjustment() {
        WebUser webUser = new WebUser();
        webUser.setTimeZone(TimeZone.getTimeZone("Pacific/Kiritimati"));
        webUser.setDateEntryPattern("MM/dd/yyyy");
        webUser.setDateDisplayPattern("MM/dd/yyyy");

        Date startDate = BillFundingSourceEditServlet.parseDateOnly("08/01/2026", webUser);
        Date endDate = BillFundingSourceEditServlet.parseDateOnly("07/31/2027", webUser);

        Assert.assertEquals("2026-08-01", startDate.toString());
        Assert.assertEquals("2027-07-31", endDate.toString());
        Assert.assertEquals("08/01/2026", BillFundingSourceEditServlet.formatDateOnly(startDate, webUser));
        Assert.assertEquals("07/31/2027", BillFundingSourceEditServlet.formatDateOnly(endDate, webUser));
    }
}