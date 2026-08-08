package org.openimmunizationsoftware.pt.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

public class BillingProjectAssignmentsServletTest {

    @Test
    public void parseInteger_handlesValidAndInvalidValues() {
        Assert.assertEquals(Integer.valueOf(42), BillingProjectAssignmentsServlet.parseInteger(" 42 "));
        Assert.assertNull(BillingProjectAssignmentsServlet.parseInteger("not-a-number"));
        Assert.assertNull(BillingProjectAssignmentsServlet.parseInteger(null));
    }

    @Test
    public void servlet_declaresGetAndPostDispatchMethods() throws Exception {
        Assert.assertNotNull(BillingProjectAssignmentsServlet.class.getDeclaredMethod(
                "doGet", HttpServletRequest.class, HttpServletResponse.class));
        Assert.assertNotNull(BillingProjectAssignmentsServlet.class.getDeclaredMethod(
                "doPost", HttpServletRequest.class, HttpServletResponse.class));
    }
}