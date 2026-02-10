package org.openimmunizationsoftware.pt.model;

public enum ProjectNarrativeVerb {
    NOTE("NOTE", "Note"),
    DECISION("DECISION", "Decision"),
    INSIGHT("INSIGHT", "Insight"),
    RISK("RISK", "Risk"),
    OPPORTUNITY("OPPORTUNITY", "Opportunity");

    private final String id;
    private final String label;

    ProjectNarrativeVerb(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static ProjectNarrativeVerb fromId(String id) {
        if (id == null) {
            return null;
        }
        for (ProjectNarrativeVerb verb : ProjectNarrativeVerb.values()) {
            if (verb.id.equals(id)) {
                return verb;
            }
        }
        return null;
    }
}
