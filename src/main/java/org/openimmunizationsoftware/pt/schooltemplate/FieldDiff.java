package org.openimmunizationsoftware.pt.schooltemplate;

/** A single changed field shown on the import preview screen. */
public class FieldDiff {

    private final String label;
    private final String oldValue;
    private final String newValue;

    public FieldDiff(String label, String oldValue, String newValue) {
        this.label = label;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getLabel() {
        return label;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
