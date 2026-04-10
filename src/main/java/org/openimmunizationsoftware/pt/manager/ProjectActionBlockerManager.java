package org.openimmunizationsoftware.pt.manager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ProjectActionBlockerManager {

    private ProjectActionBlockerManager() {
        // utility class
    }

    public static ActionNext unblockActionsBlockedBy(Session dataSession, WebUser webUser,
            ActionNext blockerAction) {
        if (dataSession == null || webUser == null || blockerAction == null || blockerAction.getActionNextId() <= 0) {
            return null;
        }

        Calendar calendar = webUser.getCalendar();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date today = calendar.getTime();

        Query query = dataSession
                .createQuery(
                        "from ActionNext where blockedBy.actionNextId = :blockedByActionNextId order by priorityLevel desc, nextChangeDate asc");
        query.setParameter("blockedByActionNextId", blockerAction.getActionNextId());
        @SuppressWarnings("unchecked")
        List<ActionNext> blockedActions = query.list();
        ActionNext firstUnblockedAction = null;
        for (ActionNext blockedAction : blockedActions) {
            blockedAction.setBlockedBy(null);
            blockedAction.setNextActionDate(today);
            blockedAction.setNextChangeDate(new Date());
            dataSession.update(blockedAction);
            if (firstUnblockedAction == null) {
                firstUnblockedAction = blockedAction;
            }
        }
        return firstUnblockedAction;
    }
}
