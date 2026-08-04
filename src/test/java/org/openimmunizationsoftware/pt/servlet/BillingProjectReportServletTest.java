package org.openimmunizationsoftware.pt.servlet;

import org.junit.Assert;
import org.junit.Test;

public class BillingProjectReportServletTest {

    @Test
    public void shouldExcludeClosed_defaultsToTrueBeforeFormSubmission() {
        Assert.assertTrue(BillingProjectReportServlet.shouldExcludeClosed(null, null));
    }

    @Test
    public void shouldExcludeClosed_isFalseWhenSubmittedWithoutCheckbox() {
        Assert.assertFalse(BillingProjectReportServlet.shouldExcludeClosed("Y", null));
    }

    @Test
    public void shouldExcludeClosed_isTrueWhenSubmittedWithCheckbox() {
        Assert.assertTrue(BillingProjectReportServlet.shouldExcludeClosed("Y", "Y"));
    }
}