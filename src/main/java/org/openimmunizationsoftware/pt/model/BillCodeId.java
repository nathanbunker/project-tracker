package org.openimmunizationsoftware.pt.model;

public class BillCodeId implements java.io.Serializable {

    private static final long serialVersionUID = 1517975497756549050L;

    private String providerId;
    private String billCode;

    public BillCodeId() {
    }

    public BillCodeId(String providerId, String billCode) {
        this.providerId = providerId;
        this.billCode = billCode;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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
        return safeEquals(providerId, castOther.providerId)
                && safeEquals(billCode, castOther.billCode);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (providerId == null ? 0 : providerId.hashCode());
        result = 37 * result + (billCode == null ? 0 : billCode.hashCode());
        return result;
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
