package org.dandeliondaily.focus.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ActionNext;

public class FocusProjectActionsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);
    private static final ZoneId ZONE = ZoneId.of("America/Denver");

    private final FocusProjectActionsService service = new FocusProjectActionsService();

    @Test
    public void group_splitsByDate() {
        ActionNext overdue = action(1, TODAY.minusDays(3), 0);
        ActionNext today = action(2, TODAY, 0);
        ActionNext tomorrow = action(3, TODAY.plusDays(1), 0);
        ActionNext later = action(4, TODAY.plusDays(9), 0);

        FocusProjectActionsService.ProjectActions actions = service.group(
                Arrays.asList(later, tomorrow, today, overdue), TODAY, ZONE);

        Assert.assertEquals(Arrays.asList(overdue), actions.getOverdue());
        Assert.assertEquals(Arrays.asList(today), actions.getToday());
        Assert.assertEquals(Arrays.asList(tomorrow), actions.getTomorrow());
        Assert.assertEquals(Arrays.asList(later), actions.getLater());
        Assert.assertEquals(4, actions.getOpenCount());
    }

    @Test
    public void group_ordersByDateThenCompletionOrderWithUnorderedLast() {
        ActionNext unordered = action(1, TODAY, 0);
        ActionNext second = action(2, TODAY, 2);
        ActionNext first = action(3, TODAY, 1);
        ActionNext laterEarly = action(4, TODAY.plusDays(5), 0);
        ActionNext laterLate = action(5, TODAY.plusDays(20), 0);

        FocusProjectActionsService.ProjectActions actions = service.group(
                Arrays.asList(unordered, laterLate, second, first, laterEarly), TODAY, ZONE);

        Assert.assertEquals(Arrays.asList(first, second, unordered), actions.getToday());
        Assert.assertEquals(Arrays.asList(laterEarly, laterLate), actions.getLater());
    }

    @Test
    public void group_keepsOnlyTodaysRecurringInstances() {
        ActionNext recurringPast = action(1, TODAY.minusDays(1), 0);
        recurringPast.setTemplateActionNextId(Integer.valueOf(900));
        ActionNext recurringToday = action(2, TODAY, 0);
        recurringToday.setTemplateActionNextId(Integer.valueOf(900));
        ActionNext recurringTomorrow = action(3, TODAY.plusDays(1), 0);
        recurringTomorrow.setTemplateActionNextId(Integer.valueOf(900));
        ActionNext oneOffFar = action(4, TODAY.plusDays(60), 0);

        FocusProjectActionsService.ProjectActions actions = service.group(
                Arrays.asList(recurringPast, recurringToday, recurringTomorrow, oneOffFar), TODAY, ZONE);

        Assert.assertTrue(actions.getOverdue().isEmpty());
        Assert.assertEquals(Arrays.asList(recurringToday), actions.getToday());
        Assert.assertTrue(actions.getTomorrow().isEmpty());
        Assert.assertEquals(Arrays.asList(oneOffFar), actions.getLater());
        Assert.assertTrue(FocusProjectActionsService.isRecurring(recurringToday));
        Assert.assertFalse(FocusProjectActionsService.isRecurring(oneOffFar));
    }

    private ActionNext action(int id, LocalDate date, int completionOrder) {
        ActionNext action = new ActionNext();
        action.setActionNextId(id);
        action.setNextActionDate(java.sql.Date.valueOf(date));
        action.setCompletionOrder(completionOrder);
        return action;
    }
}
