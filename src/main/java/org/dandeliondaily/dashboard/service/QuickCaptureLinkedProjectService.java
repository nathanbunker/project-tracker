package org.dandeliondaily.dashboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.Project;

public class QuickCaptureLinkedProjectService {

    private final ProjectDisplayLabelService projectDisplayLabelService = new ProjectDisplayLabelService();
    private final QuickCaptureProjectTokenService projectTokenService = new QuickCaptureProjectTokenService();

    public static class LinkedAliasResolution {
        private final String aliasLabel;
        private final Project privateProject;
        private final List<Project> targetProjects;

        public LinkedAliasResolution(String aliasLabel, Project privateProject, List<Project> targetProjects) {
            this.aliasLabel = aliasLabel;
            this.privateProject = privateProject;
            this.targetProjects = targetProjects;
        }

        public String getAliasLabel() {
            return aliasLabel;
        }

        public Project getPrivateProject() {
            return privateProject;
        }

        public List<Project> getTargetProjects() {
            return targetProjects;
        }
    }

    public List<String> listAliasLabels(Session dataSession, List<Project> privateProjects) {
        List<LinkedAliasResolution> resolutions = listAliasResolutions(dataSession, privateProjects);
        List<String> labels = new ArrayList<String>();
        for (LinkedAliasResolution resolution : resolutions) {
            labels.add(resolution.getAliasLabel());
        }
        return labels;
    }

    public LinkedAliasResolution resolveAliasLabel(Session dataSession, List<Project> privateProjects,
            String aliasLabel) {
        if (aliasLabel == null || aliasLabel.trim().length() == 0) {
            return null;
        }
        String normalized = aliasLabel.trim().toLowerCase();
        for (LinkedAliasResolution resolution : listAliasResolutions(dataSession, privateProjects)) {
            if (resolution.getAliasLabel().toLowerCase().equals(normalized)) {
                return resolution;
            }
        }
        return null;
    }

    public List<LinkedAliasResolution> listAliasResolutions(Session dataSession, List<Project> privateProjects) {
        List<LinkedAliasResolution> aliases = new ArrayList<LinkedAliasResolution>();
        if (dataSession == null || privateProjects == null || privateProjects.isEmpty()) {
            return aliases;
        }

        Map<String, LinkedAliasResolution> byLabel = new LinkedHashMap<String, LinkedAliasResolution>();
        for (Project privateProject : privateProjects) {
            if (privateProject == null || privateProject.getProjectId() <= 0
                    || privateProject.getLinkedPatchWorkspaceId() == null) {
                continue;
            }
            List<Project> linkedPatchProjects = projectDisplayLabelService.resolveLinkedPatchProjects(dataSession,
                    privateProject);
            if (linkedPatchProjects.isEmpty()) {
                continue;
            }

            for (Project linkedPatchProject : linkedPatchProjects) {
                LinkedAliasResolution aliasResolution = createAliasResolution(privateProject, linkedPatchProject);
                String aliasLabel = aliasResolution.getAliasLabel();
                String aliasKey = aliasLabel.toLowerCase();
                if (!byLabel.containsKey(aliasKey)) {
                    byLabel.put(aliasKey, aliasResolution);
                }
            }
        }

        aliases.addAll(byLabel.values());
        return aliases;
    }

    LinkedAliasResolution createAliasResolution(Project privateProject, Project linkedPatchProject) {
        List<Project> targetProjects = new ArrayList<Project>();
        if (privateProject != null) {
            targetProjects.add(privateProject);
        }
        if (linkedPatchProject != null) {
            targetProjects.add(linkedPatchProject);
        }
        String aliasLabel = projectTokenService.buildCompositeToken(privateProject, linkedPatchProject);
        return new LinkedAliasResolution(aliasLabel, privateProject, targetProjects);
    }
}
