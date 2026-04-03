package org.openimmunizationsoftware.pt.manager;

import org.junit.Assert;
import org.junit.Test;

public class TimeAdderTest {

    @Test
    public void calculateRemainingEstimateReturnsDifferenceWhenActualIsLower() {
        int remaining = TimeAdder.calculateRemainingEstimate(120, 45);
        Assert.assertEquals(75, remaining);
    }

    @Test
    public void calculateRemainingEstimateReturnsZeroWhenActualEqualsEstimate() {
        int remaining = TimeAdder.calculateRemainingEstimate(60, 60);
        Assert.assertEquals(0, remaining);
    }

    @Test
    public void calculateRemainingEstimateReturnsZeroWhenActualExceedsEstimate() {
        int remaining = TimeAdder.calculateRemainingEstimate(30, 45);
        Assert.assertEquals(0, remaining);
    }

    @Test
    public void calculateRemainingEstimateTreatsNullActualAsZero() {
        int remaining = TimeAdder.calculateRemainingEstimate(25, null);
        Assert.assertEquals(25, remaining);
    }

    @Test
    public void calculateRemainingEstimateReturnsZeroWhenEstimateIsNull() {
        int remaining = TimeAdder.calculateRemainingEstimate(null, 10);
        Assert.assertEquals(0, remaining);
    }
}
