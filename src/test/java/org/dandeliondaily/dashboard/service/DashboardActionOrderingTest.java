package org.dandeliondaily.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.WebUser;

public class DashboardActionOrderingTest {

    private final WebUser webUser = new WebUser();
    private final java.sql.Date futureDay = java.sql.Date.valueOf(LocalDate.now().plusDays(30));

    @Test
    public void requestedActionsGoFirstInTheirBucketInTheGivenOrder() {
        ActionNext will1 = action(1, ProjectNextActionType.WILL, 1);
        ActionNext will2 = action(2, ProjectNextActionType.WILL, 2);
        ActionNext will3 = action(3, ProjectNextActionType.WILL, 3);
        ActionNext might = action(4, ProjectNextActionType.MIGHT, 4);
        ActionNext committed = action(5, ProjectNextActionType.COMMITTED_TO, 0);

        List<ActionNext> result = DashboardActionOrdering.applyRequestedOrder(
                Arrays.asList(will1, will2, will3, might, committed), Arrays.asList(will3, will1), webUser);

        Assert.assertEquals(Arrays.asList(5, 3, 1, 2, 4), ids(result));
    }

    @Test
    public void orderingAcrossBucketsDoesNotMoveBuckets() {
        ActionNext will = action(1, ProjectNextActionType.WILL, 1);
        ActionNext might = action(2, ProjectNextActionType.MIGHT, 2);

        List<ActionNext> result = DashboardActionOrdering.applyRequestedOrder(
                Arrays.asList(will, might), Arrays.asList(might, will), webUser);

        Assert.assertEquals(Arrays.asList(1, 2), ids(result));
    }

    @Test
    public void unsetOrdersFollowSetOrdersInsideABucket() {
        ActionNext unset = action(1, ProjectNextActionType.WILL, 0);
        ActionNext set = action(2, ProjectNextActionType.WILL, 5);
        List<ActionNext> list = new ArrayList<ActionNext>(Arrays.asList(unset, set));

        DashboardActionOrdering.sortByBucketThenCompletionOrder(list, webUser);

        Assert.assertEquals(Arrays.asList(2, 1), ids(list));
    }

    @Test
    public void requestedActionNotOnTheDayIsIgnored() {
        ActionNext will = action(1, ProjectNextActionType.WILL, 1);
        ActionNext elsewhere = action(9, ProjectNextActionType.WILL, 1);

        List<ActionNext> result = DashboardActionOrdering.applyRequestedOrder(
                Arrays.asList(will), Arrays.asList(elsewhere), webUser);

        Assert.assertEquals(Arrays.asList(1), ids(result));
    }

    @Test
    public void labelsMatchDashboardGroups() {
        Assert.assertEquals("Will", DashboardActionOrdering.getBucketLabel(
                DashboardActionOrdering.getCompletionBucket(action(1, ProjectNextActionType.WILL_CONTACT, 0),
                        webUser)));
        ActionNext personal = action(2, ProjectNextActionType.WILL, 0);
        personal.setBillable(false);
        Assert.assertEquals("Not on Dashboard", DashboardActionOrdering.getBucketLabel(
                DashboardActionOrdering.getCompletionBucket(personal, webUser)));
    }

    private ActionNext action(int id, String type, int completionOrder) {
        ActionNext action = new ActionNext();
        action.setActionNextId(id);
        action.setNextActionType(type);
        action.setBillable(true);
        action.setNextActionDate(futureDay);
        action.setCompletionOrder(completionOrder);
        return action;
    }

    private List<Integer> ids(List<ActionNext> actions) {
        List<Integer> ids = new ArrayList<Integer>();
        for (ActionNext action : actions) {
            ids.add(Integer.valueOf(action.getActionNextId()));
        }
        return ids;
    }
}
