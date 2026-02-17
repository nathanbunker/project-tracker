package org.openimmunizationsoftware.pt.api.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.api.common.HibernateRequestContext;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionChangeLog;
import org.openimmunizationsoftware.pt.model.ProjectActionProposal;

public class ActionChangeLogService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    public void logChange(String providerId, int actionNextId, int projectId, String actorType,
            String actorId, String sourceType, Integer proposalId, Map<String, FromTo> patch,
            String reason) {
        ProjectActionNext action = requireAction(providerId, actionNextId, projectId);
        Session session = HibernateRequestContext.getCurrentSession();

        ProjectActionChangeLog changeLog = new ProjectActionChangeLog();
        changeLog.setAction(action);
        changeLog.setActionNextId(actionNextId);
        changeLog.setProjectId(projectId);
        if (proposalId != null) {
            ProjectActionProposal proposal = (ProjectActionProposal) session.get(
                    ProjectActionProposal.class, proposalId);
            changeLog.setProposal(proposal);
            changeLog.setProposalId(proposalId);
        }
        changeLog.setChangeDate(new Date());
        changeLog.setActorTypeString(actorType);
        changeLog.setActorId(actorId);
        changeLog.setSourceType(sourceType);
        changeLog.setChangePatch(serializePatch(patch));
        changeLog.setChangeSummary(buildSummary(patch));
        changeLog.setChangeReason(reason);

        session.save(changeLog);
    }

    private ProjectActionNext requireAction(String providerId, int actionNextId, int projectId) {
        Session session = HibernateRequestContext.getCurrentSession();
        Query query = session.createQuery(
                "from ProjectActionNext pan where pan.actionNextId = :actionNextId and pan.projectId = :projectId "
                        + "and pan.provider.providerId = :providerId");
        query.setInteger("actionNextId", actionNextId);
        query.setInteger("projectId", projectId);
        query.setString("providerId", providerId);
        ProjectActionNext action = (ProjectActionNext) query.uniqueResult();
        if (action == null) {
            throw new IllegalStateException("Action not found for provider.");
        }
        return action;
    }

    private String serializePatch(Map<String, FromTo> patch) {
        if (patch == null || patch.isEmpty()) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(patch);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize change patch.", e);
        }
    }

    private String buildSummary(Map<String, FromTo> patch) {
        if (patch == null || patch.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (Map.Entry<String, FromTo> entry : patch.entrySet()) {
            FromTo change = entry.getValue();
            joiner.add(entry.getKey() + ": " + formatValue(change.getFrom()) + " -> "
                    + formatValue(change.getTo()));
        }
        return joiner.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            return sdf.format((Date) value);
        }
        return String.valueOf(value);
    }

    public static class FromTo {

        private Object from;
        private Object to;

        public FromTo() {
        }

        public FromTo(Object from, Object to) {
            this.from = from;
            this.to = to;
        }

        public Object getFrom() {
            return from;
        }

        public void setFrom(Object from) {
            this.from = from;
        }

        public Object getTo() {
            return to;
        }

        public void setTo(Object to) {
            this.to = to;
        }
    }
}
