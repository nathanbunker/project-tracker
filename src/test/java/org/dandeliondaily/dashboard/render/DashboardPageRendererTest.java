package org.dandeliondaily.dashboard.render;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DashboardPageRendererTest {

    private DashboardPageRenderer renderer;

    @Before
    public void setUp() {
        renderer = new DashboardPageRenderer();
    }

    @Test
    public void shouldApplyTodayChipAlert_returnsFalseForOverdueWhenCountIsZero() {
        Assert.assertFalse(renderer.shouldApplyTodayChipAlert("overdue", 0));
    }

    @Test
    public void shouldApplyTodayChipAlert_returnsTrueForOverdueWhenCountIsOne() {
        Assert.assertTrue(renderer.shouldApplyTodayChipAlert("overdue", 1));
    }

    @Test
    public void shouldApplyTodayChipAlert_returnsTrueForOverdueWhenCountIsTwo() {
        Assert.assertTrue(renderer.shouldApplyTodayChipAlert("overdue", 2));
    }

    @Test
    public void shouldApplyTodayChipAlert_returnsFalseForNonOverdueSections() {
        Assert.assertFalse(renderer.shouldApplyTodayChipAlert("waiting", 2));
    }
}