package org.dandeliondaily.dashboard.service;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.Project;

public class DashboardCurrentActionServiceTest {

    private DashboardCurrentActionService service;

    @Before
    public void setUp() {
        service = new DashboardCurrentActionService();
    }

    @Test
    public void shouldRefreshCurrentAction_returnsTrueWhenCurrentActionMovesToFutureDay() {
        ActionNext currentAction = action(101);
        ActionNext movedAction = action(101);

        Assert.assertTrue(service.shouldRefreshCurrentAction(currentAction, movedAction,
                LocalDate.of(2026, 4, 23), LocalDate.of(2026, 4, 22)));
    }

    @Test
    public void shouldRefreshCurrentAction_returnsFalseWhenActionStaysToday() {
        ActionNext currentAction = action(101);
        ActionNext movedAction = action(101);

        Assert.assertFalse(service.shouldRefreshCurrentAction(currentAction, movedAction,
                LocalDate.of(2026, 4, 22), LocalDate.of(2026, 4, 22)));
    }

    @Test
    public void shouldRefreshCurrentAction_returnsFalseForDifferentAction() {
        ActionNext currentAction = action(101);
        ActionNext movedAction = action(202);

        Assert.assertFalse(service.shouldRefreshCurrentAction(currentAction, movedAction,
                LocalDate.of(2026, 4, 23), LocalDate.of(2026, 4, 22)));
    }

    @Test
    public void syncTimerWithCurrentAction_startsReplacementWhenTimerWasRunning() {
        RecordingTimerHandoff timerHandoff = new RecordingTimerHandoff();
        ActionNext replacementAction = actionWithProject(303);

        service.syncTimerWithCurrentAction(true, replacementAction, timerHandoff);

        Assert.assertSame(replacementAction, timerHandoff.startedAction);
        Assert.assertFalse(timerHandoff.stopped);
    }

    @Test
    public void syncTimerWithCurrentAction_stopsTimerWhenNoReplacementExists() {
        RecordingTimerHandoff timerHandoff = new RecordingTimerHandoff();

        service.syncTimerWithCurrentAction(true, null, timerHandoff);

        Assert.assertNull(timerHandoff.startedAction);
        Assert.assertTrue(timerHandoff.stopped);
    }

    @Test
    public void syncTimerWithCurrentAction_doesNothingWhenTimerWasNotRunning() {
        RecordingTimerHandoff timerHandoff = new RecordingTimerHandoff();

        service.syncTimerWithCurrentAction(false, actionWithProject(404), timerHandoff);

        Assert.assertNull(timerHandoff.startedAction);
        Assert.assertFalse(timerHandoff.stopped);
    }

    private ActionNext action(int actionNextId) {
        ActionNext action = new ActionNext();
        action.setActionNextId(actionNextId);
        return action;
    }

    private ActionNext actionWithProject(int actionNextId) {
        ActionNext action = action(actionNextId);
        Project project = new Project();
        project.setProjectId(actionNextId + 1000);
        action.setProject(project);
        return action;
    }

    private static class RecordingTimerHandoff implements DashboardCurrentActionService.TimerHandoff {
        private ActionNext startedAction;
        private boolean stopped;

        @Override
        public void start(ActionNext action) {
            this.startedAction = action;
        }

        @Override
        public void stop() {
            this.stopped = true;
        }
    }
}