package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.MeetingSessionTopic;

public class MeetingSessionTopicDao {

    private final Session session;

    public MeetingSessionTopicDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public MeetingSessionTopicDao(Session session) {
        this.session = session;
    }

    public MeetingSessionTopic save(MeetingSessionTopic topic) {
        validateForSaveOrUpdate(topic);
        session.save(topic);
        return topic;
    }

    public void update(MeetingSessionTopic topic) {
        validateForSaveOrUpdate(topic);
        if (topic.getMeetingSessionTopicId() == null || topic.getMeetingSessionTopicId().intValue() <= 0) {
            throw new IllegalArgumentException("meetingSessionTopicId is required for update.");
        }
        session.update(topic);
    }

    public MeetingSessionTopic getById(Integer id) {
        if (id == null) {
            return null;
        }
        return (MeetingSessionTopic) session.get(MeetingSessionTopic.class, id);
    }

    public MeetingSessionTopic getByWorkspaceAndInteropHubAgendaItemId(Integer workspaceId,
            Integer interopHubAgendaItemId) {
        if (workspaceId == null || interopHubAgendaItemId == null) {
            return null;
        }
        Query query = session.createQuery(
                "from MeetingSessionTopic where workspaceId = :workspaceId and interopHubAgendaItemId = :interopHubAgendaItemId");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setInteger("interopHubAgendaItemId", interopHubAgendaItemId.intValue());
        query.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<MeetingSessionTopic> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<MeetingSessionTopic> listByMeetingSession(Integer meetingSessionId) {
        Query query = session.createQuery(
                "from MeetingSessionTopic where meetingSessionId = :meetingSessionId "
                        + "order by displayOrder asc, meetingSessionTopicId asc");
        query.setInteger("meetingSessionId", meetingSessionId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingSessionTopic> listByProject(Integer workspaceId, Integer projectId, int limit) {
        Query query = session.createQuery(
                "from MeetingSessionTopic where workspaceId = :workspaceId and projectId = :projectId "
                        + "order by displayOrder asc, meetingSessionTopicId asc");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setInteger("projectId", projectId.intValue());
        applyLimit(query, limit);
        return query.list();
    }

    private void validateForSaveOrUpdate(MeetingSessionTopic topic) {
        if (topic == null) {
            throw new IllegalArgumentException("topic is required.");
        }
        if (topic.getMeetingSessionId() == null || topic.getMeetingSessionId().intValue() <= 0) {
            throw new IllegalArgumentException("meetingSessionId is required.");
        }
        if (topic.getWorkspaceId() == null || topic.getWorkspaceId().intValue() <= 0) {
            throw new IllegalArgumentException("workspaceId is required.");
        }
        if (topic.getTitle() == null || topic.getTitle().trim().length() == 0) {
            throw new IllegalArgumentException("title is required.");
        }
        if (topic.getDisplayOrder() == null) {
            throw new IllegalArgumentException("displayOrder is required.");
        }
        if (topic.getStatus() == null || !MeetingSessionTopic.isValidStatus(topic.getStatus())) {
            throw new IllegalArgumentException("status is required and must be valid.");
        }
        if (topic.getCreatedAt() == null) {
            throw new IllegalArgumentException("createdAt is required.");
        }
        if (topic.getUpdatedAt() == null) {
            throw new IllegalArgumentException("updatedAt is required.");
        }
    }

    private void applyLimit(Query query, int limit) {
        if (limit > 0) {
            query.setMaxResults(limit);
        }
    }
}
