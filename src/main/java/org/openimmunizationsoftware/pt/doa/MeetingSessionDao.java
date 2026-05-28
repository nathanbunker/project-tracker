package org.openimmunizationsoftware.pt.doa;

import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.MeetingSession;

public class MeetingSessionDao {

    private final Session session;

    public MeetingSessionDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public MeetingSessionDao(Session session) {
        this.session = session;
    }

    public MeetingSession save(MeetingSession meetingSession) {
        validateForSaveOrUpdate(meetingSession);
        session.save(meetingSession);
        return meetingSession;
    }

    public void update(MeetingSession meetingSession) {
        validateForSaveOrUpdate(meetingSession);
        if (meetingSession.getMeetingSessionId() == null || meetingSession.getMeetingSessionId().intValue() <= 0) {
            throw new IllegalArgumentException("meetingSessionId is required for update.");
        }
        session.update(meetingSession);
    }

    public MeetingSession getById(Integer id) {
        if (id == null) {
            return null;
        }
        return (MeetingSession) session.get(MeetingSession.class, id);
    }

    public MeetingSession getByWorkspaceAndInteropHubMeetingId(Integer workspaceId,
            Integer interopHubMeetingId) {
        if (workspaceId == null || interopHubMeetingId == null) {
            return null;
        }
        Query query = session.createQuery(
                "from MeetingSession where workspaceId = :workspaceId and interopHubMeetingId = :interopHubMeetingId");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setInteger("interopHubMeetingId", interopHubMeetingId.intValue());
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<MeetingSession> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    public MeetingSession getByWorkspaceAndMeetingKey(Integer workspaceId, String meetingKey) {
        if (workspaceId == null || meetingKey == null || meetingKey.trim().length() == 0) {
            return null;
        }
        Query query = session.createQuery(
                "from MeetingSession where workspaceId = :workspaceId and meetingKey = :meetingKey");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setString("meetingKey", meetingKey);
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<MeetingSession> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<MeetingSession> listByWorkspace(Integer workspaceId, int limit) {
        Query query = session.createQuery(
                "from MeetingSession where workspaceId = :workspaceId order by scheduledStart desc, meetingSessionId desc");
        query.setInteger("workspaceId", workspaceId.intValue());
        applyLimit(query, limit);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingSession> listUpcomingByWorkspace(Integer workspaceId, Date now, int limit) {
        Query query = session.createQuery(
                "from MeetingSession where workspaceId = :workspaceId and scheduledStart >= :now "
                        + "order by scheduledStart asc, meetingSessionId asc");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setTimestamp("now", now);
        applyLimit(query, limit);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingSession> listByParentProject(Integer workspaceId, Integer parentProjectId, int limit) {
        Query query = session.createQuery(
                "from MeetingSession where workspaceId = :workspaceId and parentProjectId = :parentProjectId "
                        + "order by scheduledStart desc, meetingSessionId desc");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setInteger("parentProjectId", parentProjectId.intValue());
        applyLimit(query, limit);
        return query.list();
    }

    private void validateForSaveOrUpdate(MeetingSession meetingSession) {
        if (meetingSession == null) {
            throw new IllegalArgumentException("meetingSession is required.");
        }
        if (meetingSession.getWorkspaceId() == null || meetingSession.getWorkspaceId().intValue() <= 0) {
            throw new IllegalArgumentException("workspaceId is required.");
        }
        if (meetingSession.getMeetingName() == null || meetingSession.getMeetingName().trim().length() == 0) {
            throw new IllegalArgumentException("meetingName is required.");
        }
        if (meetingSession.getScheduledStart() == null) {
            throw new IllegalArgumentException("scheduledStart is required.");
        }
        if (meetingSession.getStatus() == null || !MeetingSession.isValidStatus(meetingSession.getStatus())) {
            throw new IllegalArgumentException("status is required and must be valid.");
        }
        if (meetingSession.getCreatedAt() == null) {
            throw new IllegalArgumentException("createdAt is required.");
        }
        if (meetingSession.getUpdatedAt() == null) {
            throw new IllegalArgumentException("updatedAt is required.");
        }
    }

    private void applyLimit(Query query, int limit) {
        if (limit > 0) {
            query.setMaxResults(limit);
        }
    }
}
