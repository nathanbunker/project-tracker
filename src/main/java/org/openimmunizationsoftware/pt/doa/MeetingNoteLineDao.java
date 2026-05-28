package org.openimmunizationsoftware.pt.doa;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.MeetingNoteLine;

public class MeetingNoteLineDao {

    private final Session session;

    public MeetingNoteLineDao() {
        this.session = HibernateRequestContext.getCurrentSession();
    }

    public MeetingNoteLineDao(Session session) {
        this.session = session;
    }

    public MeetingNoteLine save(MeetingNoteLine line) {
        validateForSaveOrUpdate(line);
        session.save(line);
        return line;
    }

    public void update(MeetingNoteLine line) {
        validateForSaveOrUpdate(line);
        if (line.getMeetingNoteLineId() == null || line.getMeetingNoteLineId().intValue() <= 0) {
            throw new IllegalArgumentException("meetingNoteLineId is required for update.");
        }
        session.update(line);
    }

    public MeetingNoteLine getById(Integer id) {
        if (id == null) {
            return null;
        }
        return (MeetingNoteLine) session.get(MeetingNoteLine.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<MeetingNoteLine> listByMeetingSessionTopic(Integer meetingSessionTopicId) {
        Query query = session.createQuery(
                "from MeetingNoteLine where meetingSessionTopicId = :meetingSessionTopicId "
                        + "order by parentMeetingNoteLineId asc, displayOrder asc, meetingNoteLineId asc");
        query.setInteger("meetingSessionTopicId", meetingSessionTopicId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingNoteLine> listTopLevelByMeetingSessionTopic(Integer meetingSessionTopicId) {
        Query query = session.createQuery(
                "from MeetingNoteLine where meetingSessionTopicId = :meetingSessionTopicId "
                        + "and parentMeetingNoteLineId is null order by displayOrder asc, meetingNoteLineId asc");
        query.setInteger("meetingSessionTopicId", meetingSessionTopicId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingNoteLine> listChildren(Integer parentMeetingNoteLineId) {
        Query query = session.createQuery(
                "from MeetingNoteLine where parentMeetingNoteLineId = :parentMeetingNoteLineId "
                        + "order by displayOrder asc, meetingNoteLineId asc");
        query.setInteger("parentMeetingNoteLineId", parentMeetingNoteLineId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingNoteLine> listByNarrativeCategory(Integer workspaceId, Integer projectId,
            String narrativeCategory, int limit) {
        Query query = session.createQuery(
                "select line from MeetingNoteLine line, MeetingSessionTopic topic "
                        + "where line.meetingSessionTopicId = topic.meetingSessionTopicId "
                        + "and line.workspaceId = :workspaceId and topic.projectId = :projectId "
                        + "and line.narrativeCategory = :narrativeCategory "
                        + "order by line.updatedAt desc, line.meetingNoteLineId desc");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setInteger("projectId", projectId.intValue());
        query.setString("narrativeCategory", narrativeCategory);
        applyLimit(query, limit);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingNoteLine> listActionCandidates(Integer workspaceId, Integer meetingSessionTopicId) {
        Query query = session.createQuery(
                "from MeetingNoteLine where workspaceId = :workspaceId "
                        + "and meetingSessionTopicId = :meetingSessionTopicId "
                        + "and actionType is not null and actionType <> '' "
                        + "and linkedNextActionId is null "
                        + "order by displayOrder asc, meetingNoteLineId asc");
        query.setInteger("workspaceId", workspaceId.intValue());
        query.setInteger("meetingSessionTopicId", meetingSessionTopicId.intValue());
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<MeetingNoteLine> listByLinkedNextAction(Integer linkedNextActionId) {
        Query query = session.createQuery(
                "from MeetingNoteLine where linkedNextActionId = :linkedNextActionId "
                        + "order by meetingNoteLineId asc");
        query.setInteger("linkedNextActionId", linkedNextActionId.intValue());
        return query.list();
    }

    private void validateForSaveOrUpdate(MeetingNoteLine line) {
        if (line == null) {
            throw new IllegalArgumentException("line is required.");
        }
        if (line.getMeetingSessionTopicId() == null || line.getMeetingSessionTopicId().intValue() <= 0) {
            throw new IllegalArgumentException("meetingSessionTopicId is required.");
        }
        if (line.getWorkspaceId() == null || line.getWorkspaceId().intValue() <= 0) {
            throw new IllegalArgumentException("workspaceId is required.");
        }
        if (line.getDisplayOrder() == null) {
            throw new IllegalArgumentException("displayOrder is required.");
        }
        if (line.getNoteText() == null || line.getNoteText().trim().length() == 0) {
            throw new IllegalArgumentException("noteText is required.");
        }
        if (line.getNarrativeCategory() != null
                && !MeetingNoteLine.isValidNarrativeCategory(line.getNarrativeCategory())) {
            throw new IllegalArgumentException("narrativeCategory must be null or valid.");
        }
        if (line.getActionType() != null && !MeetingNoteLine.isValidActionType(line.getActionType())) {
            throw new IllegalArgumentException("actionType must be null or valid.");
        }
        if (line.getVisibility() == null || !MeetingNoteLine.isValidVisibility(line.getVisibility())) {
            throw new IllegalArgumentException("visibility is required and must be valid.");
        }
        if (line.getCreatedAt() == null) {
            throw new IllegalArgumentException("createdAt is required.");
        }
        if (line.getUpdatedAt() == null) {
            throw new IllegalArgumentException("updatedAt is required.");
        }
    }

    private void applyLimit(Query query, int limit) {
        if (limit > 0) {
            query.setMaxResults(limit);
        }
    }
}
