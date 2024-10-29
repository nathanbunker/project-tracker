package org.openimmunizationsoftware.pt.model;

public enum ProjectNextActionStatus {
    PROPOSED("P", "Proposed"), READY("R", "Ready"), COMPLETED("C", "Completed"), CANCELLED("X", "Cancelled");
    
    ProjectNextActionStatus(String id, String label) {
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

    public static ProjectNextActionStatus getProjectNextActionStatus(String id) {
        for (ProjectNextActionStatus templateType : ProjectNextActionStatus.values()) {
            if (templateType.getId().equals(id)) {
                return templateType;
            }
        }
        return null;
    }
}
