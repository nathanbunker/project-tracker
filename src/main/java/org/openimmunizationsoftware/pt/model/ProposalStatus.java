package org.openimmunizationsoftware.pt.model;

public enum ProposalStatus {
    NEW("new", "New"),
    SHOWN("shown", "Shown"),
    ACCEPTED("accepted", "Accepted"),
    REJECTED("rejected", "Rejected"),
    SUPERSEDED("superseded", "Superseded");

    ProposalStatus(String id, String label) {
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

    public static ProposalStatus getProposalStatus(String id) {
        for (ProposalStatus proposalStatus : ProposalStatus.values()) {
            if (proposalStatus.getId().equals(id)) {
                return proposalStatus;
            }
        }
        return null;
    }
}
