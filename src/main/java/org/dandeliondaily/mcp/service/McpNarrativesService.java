package org.dandeliondaily.mcp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.TrackerNarrativeDao;
import org.openimmunizationsoftware.pt.model.TrackerNarrative;

/**
 * get_narratives (docs/MCP-Feedback.md item 8, I-8): read-only access to the
 * generated daily/weekly TrackerNarrative reports, for reviewing what
 * actually happened across a period before setting the next one's outlook.
 */
public class McpNarrativesService {

    public Map<String, Object> listNarratives(Session session, int contactId, String periodType, LocalDate from,
            LocalDate to) {
        List<TrackerNarrative> narratives = new TrackerNarrativeDao(session)
                .findByContactAndTypeInPeriodStartRange(contactId, periodType, from, to);
        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        for (TrackerNarrative narrative : narratives) {
            mapped.add(toMap(narrative));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("narratives", mapped);
        return result;
    }

    private Map<String, Object> toMap(TrackerNarrative narrative) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("narrativeId", Integer.valueOf(narrative.getNarrativeId()));
        map.put("narrativeType", narrative.getNarrativeType());
        map.put("displayTitle", narrative.getDisplayTitle());
        map.put("periodStart", McpActionContextSupport.toIso(narrative.getPeriodStart()));
        map.put("periodEnd", McpActionContextSupport.toIso(narrative.getPeriodEnd()));
        map.put("reviewStatus", narrative.getReviewStatusString());
        String text = narrative.getMarkdownFinal() != null ? narrative.getMarkdownFinal()
                : narrative.getMarkdownGenerated();
        map.put("text", text);
        map.put("isFinal", Boolean.valueOf(narrative.getMarkdownFinal() != null));
        map.put("lastUpdated", McpActionContextSupport.toIso(narrative.getLastUpdated()));
        return map;
    }
}
