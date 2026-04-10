package org.dandeliondaily.patch.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dandeliondaily.dashboard.service.ActionSentenceImportService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.ActionSetDao;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ActionSet;
import org.openimmunizationsoftware.pt.model.ActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectStatus;
import org.openimmunizationsoftware.pt.model.ProjectTag;
import org.openimmunizationsoftware.pt.model.ProjectTagMap;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.HandleValidationSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PatchSeedImportService {

    private static final int MAX_PROJECT_NAME = 100;
    private static final int MAX_PROJECT_DESCRIPTION = 1200;
    private static final int MAX_TAG_NAME = 100;
    private static final int MAX_ACTION_TAKEN = 12000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActionSentenceImportService actionSentenceImportService = new ActionSentenceImportService();

    public void importSeedPackage(Session dataSession, WebUser webUser, int workspaceId, String jsonSeedPackage)
            throws SeedImportException {
        List<SeedItem> seedItems = parseSeedItems(jsonSeedPackage);
        if (seedItems.isEmpty()) {
            return;
        }

        TagLookup tagLookup = loadTagLookup(dataSession, workspaceId);
        int nextTagSortOrder = resolveNextTagSortOrder(dataSession, workspaceId);

        Set<String> projectNameKeys = new LinkedHashSet<String>();
        for (int i = 0; i < seedItems.size(); i++) {
            SeedItem seedItem = seedItems.get(i);
            if (seedItem == null) {
                continue;
            }

            String projectName = clip(seedItem.name, MAX_PROJECT_NAME);
            if (projectName.length() == 0) {
                throw new SeedImportException("Seed item " + (i + 1) + " is missing required name.");
            }
            String projectKey = normalizeKey(projectName);
            if (projectNameKeys.contains(projectKey)) {
                continue;
            }
            projectNameKeys.add(projectKey);

            Project project = createProject(dataSession, webUser, workspaceId, projectName,
                    clip(seedItem.description, MAX_PROJECT_DESCRIPTION));

            TagImportResult tagImportResult = resolveProjectTags(dataSession, webUser, workspaceId, project,
                    seedItem.tags, tagLookup, nextTagSortOrder);
            nextTagSortOrder = tagImportResult.nextSortOrder;

            ActionNext nextAction = null;
            if (hasText(seedItem.actionNext)) {
                nextAction = actionSentenceImportService.buildActionFromSentence(webUser, dataSession, project, null,
                        seedItem.actionNext.trim(), Integer.valueOf(workspaceId));
                if (nextAction != null) {
                    dataSession.save(nextAction);
                }
            }

            if (hasText(seedItem.actionTaken)) {
                ActionSet actionSet = nextAction == null ? null : nextAction.getActionSet();
                if (actionSet == null) {
                    actionSet = new ActionSetDao(dataSession).createStandardActionSet(webUser);
                }
                ActionTaken actionTaken = new ActionTaken();
                actionTaken.setProject(project);
                actionTaken.setProjectId(project.getProjectId());
                actionTaken.setActionDate(new Date());
                actionTaken.setActionDescription(clip(seedItem.actionTaken, MAX_ACTION_TAKEN));
                actionTaken.setWorkspaceId(Integer.valueOf(workspaceId));
                actionTaken.setContact(webUser.getProjectContact());
                actionTaken.setContactId(webUser.getContactId());
                actionTaken.setActionSet(actionSet);
                dataSession.save(actionTaken);
            }
        }
    }

    private Project createProject(Session dataSession, WebUser webUser, int workspaceId, String projectName,
            String description) {
        Project project = new Project();
        project.setWorkspaceId(Integer.valueOf(workspaceId));
        project.setProjectName(projectName);
        project.setDescription(hasText(description) ? description : null);
        project.setProjectHandle(HandleValidationSupport.resolveHandle("", projectName, 60));
        project.setProjectStatus(ProjectStatus.ACTIVE.getDatabaseValue());
        project.setPriorityLevel(0);
        project.setBillCode(".");
        project.setCreatedByWebUserId(webUser.getWebUserId());
        project.setLastModifiedByWebUserId(webUser.getWebUserId());
        project.setWebUser(webUser);
        dataSession.save(project);
        dataSession.flush();
        return project;
    }

    private TagImportResult resolveProjectTags(Session dataSession, WebUser webUser, int workspaceId, Project project,
            List<String> rawTags, TagLookup tagLookup, int nextSortOrder) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new TagImportResult(nextSortOrder);
        }

        Set<Integer> mappedTagIds = new LinkedHashSet<Integer>();
        for (String rawTag : rawTags) {
            String tagName = clip(rawTag, MAX_TAG_NAME);
            if (tagName.length() == 0) {
                continue;
            }

            ProjectTag tag = findTag(tagLookup, tagName);
            if (tag == null) {
                tag = new ProjectTag();
                tag.setWorkspaceId(workspaceId);
                tag.setTagName(tagName);
                tag.setTagHandle(HandleValidationSupport.resolveHandle("", tagName, 60));
                tag.setTagStatus(ProjectTag.STATUS_ACTIVE);
                tag.setSortOrder(Integer.valueOf(nextSortOrder));
                nextSortOrder += 10;
                tag.setCreatedByWebUserId(webUser.getWebUserId());
                tag.setCreatedDate(new Date());
                dataSession.save(tag);
                dataSession.flush();
                registerTag(tagLookup, tag);
            } else if (!ProjectTag.STATUS_ACTIVE.equalsIgnoreCase(tag.getTagStatus())) {
                tag.setTagStatus(ProjectTag.STATUS_ACTIVE);
                dataSession.update(tag);
            }

            mappedTagIds.add(Integer.valueOf(tag.getProjectTagId()));
        }

        for (Integer projectTagId : mappedTagIds) {
            ProjectTagMap map = new ProjectTagMap();
            map.setProjectId(project.getProjectId());
            map.setProjectTagId(projectTagId.intValue());
            map.setCreatedDate(new Date());
            dataSession.save(map);
        }

        return new TagImportResult(nextSortOrder);
    }

    @SuppressWarnings("unchecked")
    private TagLookup loadTagLookup(Session dataSession, int workspaceId) {
        List<ProjectTag> existingTags = dataSession.createQuery("from ProjectTag where workspaceId = :workspaceId")
                .setInteger("workspaceId", workspaceId).list();
        TagLookup tagLookup = new TagLookup();
        for (ProjectTag tag : existingTags) {
            registerTag(tagLookup, tag);
        }
        return tagLookup;
    }

    private void registerTag(TagLookup tagLookup, ProjectTag tag) {
        String nameKey = normalizeKey(tag.getTagName());
        String handleKey = normalizeKey(tag.getTagHandle());
        if (nameKey.length() > 0) {
            tagLookup.byName.put(nameKey, tag);
        }
        if (handleKey.length() > 0) {
            tagLookup.byHandle.put(handleKey, tag);
        }
    }

    private ProjectTag findTag(TagLookup tagLookup, String rawTag) {
        String normalizedInput = normalizeKey(rawTag);
        if (normalizedInput.length() == 0) {
            return null;
        }

        ProjectTag byName = tagLookup.byName.get(normalizedInput);
        if (byName != null) {
            return byName;
        }
        ProjectTag byHandle = tagLookup.byHandle.get(normalizedInput);
        if (byHandle != null) {
            return byHandle;
        }

        String resolvedHandle = normalizeKey(HandleValidationSupport.resolveHandle("", rawTag, 60));
        if (resolvedHandle.length() == 0) {
            return null;
        }
        return tagLookup.byHandle.get(resolvedHandle);
    }

    private int resolveNextTagSortOrder(Session dataSession, int workspaceId) {
        Query maxSortOrderQuery = dataSession
                .createQuery("select max(sortOrder) from ProjectTag where workspaceId = :workspaceId")
                .setInteger("workspaceId", workspaceId);
        Number maxSortOrder = (Number) maxSortOrderQuery.uniqueResult();
        if (maxSortOrder == null) {
            return 10;
        }
        int value = maxSortOrder.intValue();
        if (value < 0) {
            return 10;
        }
        return value + 10;
    }

    private List<SeedItem> parseSeedItems(String jsonSeedPackage) throws SeedImportException {
        String payload = jsonSeedPackage == null ? "" : jsonSeedPackage.trim();
        if (payload.length() == 0) {
            return new ArrayList<SeedItem>();
        }
        try {
            List<SeedItem> seedItems = objectMapper.readValue(payload, new TypeReference<List<SeedItem>>() {
            });
            if (seedItems == null) {
                return new ArrayList<SeedItem>();
            }
            return seedItems;
        } catch (Exception e) {
            throw new SeedImportException("JSON seed is not valid: " + e.getMessage(), e);
        }
    }

    private String clip(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private static class TagLookup {
        private final Map<String, ProjectTag> byName = new LinkedHashMap<String, ProjectTag>();
        private final Map<String, ProjectTag> byHandle = new LinkedHashMap<String, ProjectTag>();
    }

    private static class TagImportResult {
        private final int nextSortOrder;

        private TagImportResult(int nextSortOrder) {
            this.nextSortOrder = nextSortOrder;
        }
    }

    public static class SeedItem {
        public String name;
        public String description;
        public List<String> tags;
        public String actionNext;
        public String actionTaken;
    }

    public static class SeedImportException extends Exception {
        private static final long serialVersionUID = 1L;

        public SeedImportException(String message) {
            super(message);
        }

        public SeedImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
