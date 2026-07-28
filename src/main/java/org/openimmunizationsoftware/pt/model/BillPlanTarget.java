package org.openimmunizationsoftware.pt.model;

public class BillPlanTarget implements java.io.Serializable {

    private static final long serialVersionUID = 5365526752730822974L;

    private int billPlanTargetId;
    private BillPlan billPlan;
    private String billCode;
    private String targetMode = BillPlanTargetMode.PERCENT.getCode();
    private Integer annualTargetBps;
    private Integer steeringTargetBps;
    private Integer billBudgetId;
    private String variancePolicy = BillPlanVariancePolicy.CARRY_FORWARD.getCode();
    private int displayOrder;
    private String targetNote;

    public int getBillPlanTargetId() {
        return billPlanTargetId;
    }

    public void setBillPlanTargetId(int billPlanTargetId) {
        this.billPlanTargetId = billPlanTargetId;
    }

    public BillPlan getBillPlan() {
        return billPlan;
    }

    public void setBillPlan(BillPlan billPlan) {
        this.billPlan = billPlan;
    }

    public Integer getBillPlanId() {
        return billPlan == null ? null : billPlan.getBillPlanId();
    }

    public String getBillCode() {
        return billCode;
    }

    public void setBillCode(String billCode) {
        this.billCode = billCode;
    }

    public String getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
    }

    public Integer getAnnualTargetBps() {
        return annualTargetBps;
    }

    public void setAnnualTargetBps(Integer annualTargetBps) {
        this.annualTargetBps = annualTargetBps;
    }

    public Integer getSteeringTargetBps() {
        return steeringTargetBps;
    }

    public void setSteeringTargetBps(Integer steeringTargetBps) {
        this.steeringTargetBps = steeringTargetBps;
    }

    public Integer getBillBudgetId() {
        return billBudgetId;
    }

    public void setBillBudgetId(Integer billBudgetId) {
        this.billBudgetId = billBudgetId;
    }

    public String getVariancePolicy() {
        return variancePolicy;
    }

    public void setVariancePolicy(String variancePolicy) {
        this.variancePolicy = variancePolicy;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getTargetNote() {
        return targetNote;
    }

    public void setTargetNote(String targetNote) {
        this.targetNote = targetNote;
    }
}