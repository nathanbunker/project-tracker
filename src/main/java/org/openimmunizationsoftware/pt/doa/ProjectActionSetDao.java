package org.openimmunizationsoftware.pt.doa;

import java.util.Date;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ActionSetType;
import org.openimmunizationsoftware.pt.model.ProjectActionSet;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ProjectActionSetDao {

    private final Session session;

    public ProjectActionSetDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public ProjectActionSetDao(Session session) {
        this.session = session;
    }

    public ProjectActionSet createStandardActionSet(WebUser webUser) {
        ProjectActionSet actionSet = new ProjectActionSet();
        actionSet.setActionSetType(ActionSetType.STANDARD);
        actionSet.setCreatedByWebUserId(webUser.getWebUserId());
        actionSet.setCreatedByWebUser(webUser);
        actionSet.setCreatedDate(new Date());
        session.save(actionSet);
        return actionSet;
    }

    public ProjectActionSet getActionSet(int actionSetId) {
        return (ProjectActionSet) session.get(ProjectActionSet.class, actionSetId);
    }
}
