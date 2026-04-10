package org.openimmunizationsoftware.pt.doa;

import java.util.Date;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ActionSetDao {

    private final Session session;

    public ActionSetDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public ActionSetDao(Session session) {
        this.session = session;
    }

    public ActionSet createStandardActionSet(WebUser webUser) {
        ActionSet actionSet = new ActionSet();
        actionSet.setActionSetType(ActionSetType.STANDARD);
        actionSet.setCreatedByWebUserId(webUser.getWebUserId());
        actionSet.setCreatedByWebUser(webUser);
        actionSet.setCreatedDate(new Date());
        session.save(actionSet);
        return actionSet;
    }

    public ActionSet getActionSet(int actionSetId) {
        return (ActionSet) session.get(ActionSet.class, actionSetId);
    }
}
