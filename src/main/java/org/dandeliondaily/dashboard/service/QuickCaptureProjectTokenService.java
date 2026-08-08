package org.dandeliondaily.dashboard.service;

import java.util.List;

import org.openimmunizationsoftware.pt.model.Project;

public class QuickCaptureProjectTokenService {

    public String getToken(Project project) {
        if (project == null) {
            return "";
        }
        String projectHandle = n(project.getProjectHandle());
        return projectHandle.length() > 0 ? projectHandle : n(project.getProjectName());
    }

    public Project findFirstByToken(List<Project> projects, String token) {
        String normalizedToken = n(token);
        if (projects == null || normalizedToken.length() == 0) {
            return null;
        }
        for (Project project : projects) {
            if (getToken(project).equalsIgnoreCase(normalizedToken)) {
                return project;
            }
        }
        return null;
    }

    public String buildCompositeToken(Project privateProject, Project linkedPatchProject) {
        String privateToken = getToken(privateProject);
        String sharedToken = getToken(linkedPatchProject);
        if (privateToken.length() == 0) {
            return sharedToken;
        }
        if (sharedToken.length() == 0) {
            return privateToken;
        }
        return privateToken + " / " + sharedToken;
    }

    private String n(String value) {
        return value == null ? "" : value.trim();
    }
}