package org.openimmunizationsoftware.pt.model;

/**
 * ProcessStage enum represents the process stage of an action.
 * See docs/process-stage-domain.md for detailed documentation.
 */
public enum ProcessStage {
    FIRST("1", "First"), SECOND("2", "Second"), PENULTIMATE("P", "Penultimate"),
    LAST("L", "Last");

    ProcessStage(String id, String label) {
        this.id = id;
        this.label = label;
    }

    private String id = "";
    private String label = "";

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Get ProcessStage by id.
     * For details on process stage values, see docs/process-stage-domain.md
     */
    public static ProcessStage getProcessStage(String id) {
        for (ProcessStage processStage : ProcessStage.values()) {
            if (processStage.getId().equals(id)) {
                return processStage;
            }
        }
        return null;
    }
}
