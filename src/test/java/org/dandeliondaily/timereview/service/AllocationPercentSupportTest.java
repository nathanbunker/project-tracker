package org.dandeliondaily.timereview.service;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AllocationPercentSupportTest {

    private AllocationPercentSupport support;

    @Before
    public void setUp() {
        support = new AllocationPercentSupport();
    }

    @Test
    public void percentToBasisPoints_isExact() {
        Assert.assertEquals(625, support.percentToBasisPoints(new BigDecimal("6.25")));
        Assert.assertEquals(500, support.percentToBasisPoints(new BigDecimal("5.00")));
        Assert.assertEquals(10000, support.percentToBasisPoints(new BigDecimal("100.00")));
    }

    @Test
    public void basisPointsToPercent_isExact() {
        Assert.assertEquals(new BigDecimal("6.25"), support.basisPointsToPercent(625));
        Assert.assertEquals(new BigDecimal("40.00"), support.basisPointsToPercent(4000));
    }
}