package org.openimmunizationsoftware.pt.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BillPlan implements java.io.Serializable {

    private static final long serialVersionUID = -5098260347488129862L;

    private int billPlanId;
    private int workspaceId;
    private int webUserId;
    private String billPlanCode;
    private String planLabel;
    private Date fiscalStartDate;
    private Date fiscalEndDate;
    private int versionNum;
    private Date effectiveDate;
    private String percentBasis = BillPlanPercentBasis.ALL_WORKED_TIME.getCode();
    private String planStatus = BillPlanStatus.DRAFT.getCode();
    private Integer supersedesBillPlanId;
    private String changeNote;
    private Date createdAt;
    private List<BillPlanTarget> targets = new ArrayList<BillPlanTarget>();

    public int getBillPlanId() {
        return billPlanId;
    }

    public void setBillPlanId(int billPlanId) {
        this.billPlanId = billPlanId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getWebUserId() {
        return webUserId;
    }

    public void setWebUserId(int webUserId) {
        this.webUserId = webUserId;
    }

    public String getBillPlanCode() {
        return billPlanCode;
    }

    public void setBillPlanCode(String billPlanCode) {
        this.billPlanCode = billPlanCode;
    }

    public String getPlanLabel() {
        return planLabel;
    }

    public void setPlanLabel(String planLabel) {
        this.planLabel = planLabel;
    }

    public Date getFiscalStartDate() {
        return fiscalStartDate;
    }

    public void setFiscalStartDate(Date fiscalStartDate) {
        this.fiscalStartDate = fiscalStartDate;
    }

    public Date getFiscalEndDate() {
        return fiscalEndDate;
    }

    public void setFiscalEndDate(Date fiscalEndDate) {
        this.fiscalEndDate = fiscalEndDate;
    }

    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getPercentBasis() {
        return percentBasis;
    }

    public void setPercentBasis(String percentBasis) {
        this.percentBasis = percentBasis;
    }

    public String getPlanStatus() {
        return planStatus;
    }

    public void setPlanStatus(String planStatus) {
        this.planStatus = planStatus;
    }

    public Integer getSupersedesBillPlanId() {
        return supersedesBillPlanId;
    }

    public void setSupersedesBillPlanId(Integer supersedesBillPlanId) {
        this.supersedesBillPlanId = supersedesBillPlanId;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<BillPlanTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<BillPlanTarget> targets) {
        this.targets = targets;
    }
}