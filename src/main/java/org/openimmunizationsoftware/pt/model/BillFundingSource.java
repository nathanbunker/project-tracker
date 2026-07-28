package org.openimmunizationsoftware.pt.model;

import java.util.Date;

public class BillFundingSource implements java.io.Serializable {

    private static final long serialVersionUID = -5150445544925988323L;

    private int fundingSourceId;
    private int workspaceId;
    private String fundingSourceCode;
    private String fundingSourceLabel;
    private String fundingSourceType;
    private Date startDate;
    private Date endDate;
    private String visible = "Y";

    public int getFundingSourceId() {
        return fundingSourceId;
    }

    public void setFundingSourceId(int fundingSourceId) {
        this.fundingSourceId = fundingSourceId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getFundingSourceCode() {
        return fundingSourceCode;
    }

    public void setFundingSourceCode(String fundingSourceCode) {
        this.fundingSourceCode = fundingSourceCode;
    }

    public String getFundingSourceLabel() {
        return fundingSourceLabel;
    }

    public void setFundingSourceLabel(String fundingSourceLabel) {
        this.fundingSourceLabel = fundingSourceLabel;
    }

    public String getFundingSourceType() {
        return fundingSourceType;
    }

    public void setFundingSourceType(String fundingSourceType) {
        this.fundingSourceType = fundingSourceType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }
}