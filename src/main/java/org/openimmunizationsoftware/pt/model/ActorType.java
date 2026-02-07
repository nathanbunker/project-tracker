package org.openimmunizationsoftware.pt.model;

public enum ActorType {
    USER("user", "User"),
    AI("ai", "AI"),
    SYSTEM("system", "System");

    ActorType(String id, String label) {
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

    public static ActorType getActorType(String id) {
        for (ActorType actorType : ActorType.values()) {
            if (actorType.getId().equals(id)) {
                return actorType;
            }
        }
        return null;
    }
}
