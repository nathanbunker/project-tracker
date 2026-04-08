package org.openimmunizationsoftware.pt.model;

public class BillCodeId implements java.io.Serializable {

    private static final long serialVersionUID = 1517975497756549050L;

    private Integer workspaceId;
    private String billCode;

    public BillCodeId() {
    }

    public BillCodeId(Integer workspaceId, String billCode) {
        this.workspaceId = workspaceId;
        this.billCode = billCode;
    }

    public BillCodeId(String providerId, String billCode) {
        setProviderId(providerId);
        this.billCode = billCode;
    }

    public Integer getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Integer workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getProviderId() {
        return workspaceId == null ? null : String.valueOf(workspaceId);
    }

    public void setProviderId(String providerId) {
        if (providerId == null || providerId.trim().equals("")) {
            this.workspaceId = null;
            return;
        }
        this.workspaceId = Integer.valueOf(providerId.trim());
    }

    public String getBillCode() {
        return billCode;
    }

    public void setBillCode(String billCode) {
        this.billCode = billCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BillCodeId)) {
            return false;
        }
        BillCodeId castOther = (BillCodeId) other;
        return safeEquals(workspaceId, castOther.workspaceId)
                && safeEquals(billCode, castOther.billCode);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (workspaceId == null ? 0 : workspaceId.hashCode());
        result = 37 * result + (billCode == null ? 0 : billCode.hashCode());
        return result;
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
