package org.dandeliondaily.narrative.model;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.pt.model.TrackerNarrative;

public class TrackerNarrativeViewModel {

    private TrackerNarrativeScope scope;
    private boolean generationAvailable;
    private String generationUnavailableMessage;
    private TrackerNarrative activeNarrative;
    private TrackerNarrative approvedNarrative;
    private final List<TrackerNarrative> narratives = new ArrayList<TrackerNarrative>();
    private final List<TrackerNarrative> historyItems = new ArrayList<TrackerNarrative>();

    public TrackerNarrativeScope getScope() {
        return scope;
    }

    public void setScope(TrackerNarrativeScope scope) {
        this.scope = scope;
    }

    public boolean isGenerationAvailable() {
        return generationAvailable;
    }

    public void setGenerationAvailable(boolean generationAvailable) {
        this.generationAvailable = generationAvailable;
    }

    public String getGenerationUnavailableMessage() {
        return generationUnavailableMessage;
    }

    public void setGenerationUnavailableMessage(String generationUnavailableMessage) {
        this.generationUnavailableMessage = generationUnavailableMessage;
    }

    public TrackerNarrative getActiveNarrative() {
        return activeNarrative;
    }

    public void setActiveNarrative(TrackerNarrative activeNarrative) {
        this.activeNarrative = activeNarrative;
    }

    public TrackerNarrative getApprovedNarrative() {
        return approvedNarrative;
    }

    public void setApprovedNarrative(TrackerNarrative approvedNarrative) {
        this.approvedNarrative = approvedNarrative;
    }

    public List<TrackerNarrative> getNarratives() {
        return narratives;
    }

    public List<TrackerNarrative> getHistoryItems() {
        return historyItems;
    }

    public boolean hasAnyNarrative() {
        return !narratives.isEmpty();
    }
}
