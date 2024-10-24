package org.openimmunizationsoftware.pt.model;

public enum TemplateType {
    DAILY("D", "Daily"), WEEKLY("W", "Weekly"), MONTHLY("M", "Monthly"), 
    QUARTERLY("Q", "Quarterly"), YEARLY("Y", "Yearly");
    TemplateType(String id, String label) {
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

    public static TemplateType getTemplateType(String id) {
        for (TemplateType templateType : TemplateType.values()) {
            if (templateType.getId().equals(id)) {
                return templateType;
            }
        }
        return null;
    }
}
