package org.openimmunizationsoftware.pt.api.v1.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;

@Schema(name = "ProjectNarrative")
public class ProjectNarrativeDto {

    @Schema(example = "101")
    private int narrativeId;
    @Schema(example = "123")
    private int projectId;
    @Schema(example = "456")
    private int contactId;
    @Schema(example = "provider-1")
    private String providerId;
    private Date narrativeDate;
    private Date lastUpdated;
    @Schema(example = "NOTE")
    private String narrativeVerb;
    private String narrativeText;

    public static ProjectNarrativeDto from(ProjectNarrative narrative) {
        ProjectNarrativeDto dto = new ProjectNarrativeDto();
        dto.setNarrativeId(narrative.getNarrativeId());
        dto.setProjectId(narrative.getProjectId());
        dto.setContactId(narrative.getContactId());
        dto.setProviderId(narrative.getProviderId());
        dto.setNarrativeDate(narrative.getNarrativeDate());
        dto.setLastUpdated(narrative.getLastUpdated());
        dto.setNarrativeVerb(narrative.getNarrativeVerbString());
        dto.setNarrativeText(narrative.getNarrativeText());
        return dto;
    }

    public int getNarrativeId() {
        return narrativeId;
    }

    public void setNarrativeId(int narrativeId) {
        this.narrativeId = narrativeId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Date getNarrativeDate() {
        return narrativeDate;
    }

    public void setNarrativeDate(Date narrativeDate) {
        this.narrativeDate = narrativeDate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getNarrativeVerb() {
        return narrativeVerb;
    }

    public void setNarrativeVerb(String narrativeVerb) {
        this.narrativeVerb = narrativeVerb;
    }

    public String getNarrativeText() {
        return narrativeText;
    }

    public void setNarrativeText(String narrativeText) {
        this.narrativeText = narrativeText;
    }
}
