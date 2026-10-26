package org.openimmunizationsoftware.pt.doa;

import org.hibernate.Query;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.LinkedMeeting;

public class LinkedMeetingDao {
    private final Session session;

    public LinkedMeetingDao(Session session) {
        this.session = session;
    }

    public LinkedMeeting findByAction(int workspaceId, int actionNextId) {
        return (LinkedMeeting) session.createQuery(
                "from LinkedMeeting where workspaceId = :workspaceId and actionNextId = :actionNextId")
                .setInteger("workspaceId", workspaceId).setInteger("actionNextId", actionNextId).uniqueResult();
    }

    public LinkedMeeting findByActionForUpdate(int workspaceId, int actionNextId) {
        return (LinkedMeeting) session.createQuery(
                "from LinkedMeeting m where m.workspaceId = :workspaceId and m.actionNextId = :actionNextId")
                .setInteger("workspaceId", workspaceId).setInteger("actionNextId", actionNextId)
                .setLockMode("m", LockMode.UPGRADE).uniqueResult();
    }

    public LinkedMeeting findByIdentity(int workspaceId, LinkedMeeting identity) {
        Query query = session.createQuery("from LinkedMeeting where workspaceId = :workspaceId "
                + "and sourceSystem = :sourceSystem and sourceNamespace = :sourceNamespace "
                + "and externalEventUid = :externalEventUid and externalOccurrenceId = :externalOccurrenceId");
        query.setInteger("workspaceId", workspaceId);
        query.setString("sourceSystem", identity.getSourceSystem());
        query.setString("sourceNamespace", identity.getSourceNamespace());
        query.setString("externalEventUid", identity.getExternalEventUid());
        query.setString("externalOccurrenceId", identity.getExternalOccurrenceId());
        return (LinkedMeeting) query.uniqueResult();
    }
}
