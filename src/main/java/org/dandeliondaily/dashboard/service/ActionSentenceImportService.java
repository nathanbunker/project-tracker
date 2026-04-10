package org.dandeliondaily.dashboard.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.WorkspaceRegistry;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ActionNext;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.doa.ActionSetDao;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;

public class ActionSentenceImportService {

    public int importActionsFromText(WebUser webUser, Session dataSession, Project defaultProject,
            List<Project> projectList, String bulkImportText) {
        if (bulkImportText == null || bulkImportText.trim().length() == 0) {
            return 0;
        }
        int importedCount = 0;
        String[] lines = bulkImportText.split("\\r?\\n");
        for (String line : lines) {
            if (line == null || line.trim().length() == 0) {
                continue;
            }
            ActionNext created = saveNewActionFromSentence(webUser, dataSession, defaultProject, projectList,
                    line);
            if (created != null) {
                importedCount++;
            }
        }
        return importedCount;
    }

    public ActionNext saveNewActionFromSentence(WebUser webUser, Session dataSession,
            Project defaultProject, List<Project> projectList, String sentenceInput) {
        ActionNext nextAction = buildActionFromSentence(webUser, dataSession, defaultProject, projectList,
                sentenceInput, null);
        if (nextAction == null) {
            return null;
        }

        Transaction trans = dataSession.beginTransaction();
        dataSession.saveOrUpdate(nextAction);
        trans.commit();
        return nextAction;
    }

    public ActionNext buildActionFromSentence(WebUser webUser, Session dataSession,
            Project defaultProject, List<Project> projectList, String sentenceInput, Integer workspaceIdOverride) {
        if (sentenceInput == null || sentenceInput.trim().length() == 0) {
            return null;
        }
        UrlExtractionResult urlResult = extractAndRemoveUrl(sentenceInput);
        String extractedUrl = urlResult.extractedUrl;
        sentenceInput = urlResult.cleanedText;

        String projectName = "";
        String actionPart = sentenceInput;
        String[] parts = sentenceInput.split(":", 2);
        if (parts.length == 2) {
            projectName = parts[0].trim();
            actionPart = parts[1].trim();
        }

        Project foundProject = null;
        if (projectName.length() > 0 && projectList != null) {
            for (Project project : projectList) {
                if (project != null && project.getProjectName() != null
                        && project.getProjectName().equalsIgnoreCase(projectName)) {
                    foundProject = project;
                    break;
                }
            }
        }
        if (foundProject == null) {
            if (defaultProject == null) {
                return null;
            }
            foundProject = defaultProject;
            if (projectName.length() > 0) {
                actionPart = projectName + " " + actionPart;
            }
        }

        String actionVerb = "I will";
        String actionToTake = actionPart;
        String whenToTakeAction = "";
        int nextTimeEstimate = 20;
        if (actionPart.startsWith("I will meet ")) {
            actionVerb = "I will meet";
            actionToTake = actionPart.substring("I will meet ".length()).trim();
            nextTimeEstimate = 60;
        } else if (actionPart.startsWith("I will ")) {
            actionVerb = "I will";
            actionToTake = actionPart.substring("I will ".length()).trim();
        } else if (actionPart.startsWith("I might ")) {
            actionVerb = "I might";
            actionToTake = actionPart.substring("I might ".length()).trim();
        } else if (actionPart.startsWith("I would like to ")
                || actionPart.equals("I would like to")
                || actionPart.startsWith("I would like to:")) {
            actionVerb = "I would like to";
            actionToTake = actionPart.substring("I would like to".length()).trim();
            if (actionToTake.startsWith(":")) {
                actionToTake = actionToTake.substring(1).trim();
            }
        } else if (actionPart.startsWith("I have committed ")) {
            actionVerb = "I have committed";
            actionToTake = actionPart.substring("I have committed ".length()).trim();
        } else if (actionPart.startsWith("I have set goal to")) {
            actionVerb = "I have set goal to";
            actionToTake = actionPart.substring("I have set goal to".length()).trim();
        } else if (actionPart.startsWith("I am waiting ") || actionPart.equals("I am waiting")
                || actionPart.startsWith("I am waiting:")) {
            actionVerb = "I am waiting";
            actionToTake = actionPart.substring("I am waiting".length()).trim();
            if (actionToTake.startsWith(":")) {
                actionToTake = actionToTake.substring(1).trim();
            }
            nextTimeEstimate = 5;
        }

        String[] tokens = actionToTake.trim().split("\\s+");
        if (tokens.length >= 1) {
            String lastToken = tokens[tokens.length - 1];
            String secondLastToken = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
            if (tokens.length > 3) {
                String thirdLastToken = tokens.length >= 3 ? tokens[tokens.length - 3] : "";
                if (thirdLastToken.equals("for") && isNumeric(secondLastToken)) {
                    try {
                        nextTimeEstimate = Integer.parseInt(secondLastToken);
                    } catch (NumberFormatException e) {
                        nextTimeEstimate = 20;
                    }
                    if (lastToken.equals("hours") || lastToken.equals("hour")) {
                        nextTimeEstimate *= 60;
                    }
                    actionToTake = String.join(" ", Arrays.copyOf(tokens, tokens.length - 3)).trim();
                    tokens = actionToTake.trim().split("\\s+");
                    lastToken = tokens.length >= 1 ? tokens[tokens.length - 1] : "";
                    secondLastToken = tokens.length >= 2 ? tokens[tokens.length - 2] : "";
                }
            }

            boolean foundDate = false;
            if (lastToken.chars().filter(ch -> ch == '/').count() == 2) {
                whenToTakeAction = lastToken;
                foundDate = true;
            } else {
                String lower = lastToken.toLowerCase();
                if (lower.equals("today") || lower.equals("tomorrow")) {
                    whenToTakeAction = lastToken;
                    foundDate = true;
                } else {
                    String[] days = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
                    for (String day : days) {
                        if (lower.equals(day)) {
                            if (secondLastToken.equalsIgnoreCase("next")) {
                                whenToTakeAction = "next " + lastToken;
                                actionToTake = String.join(" ", Arrays.copyOf(tokens, tokens.length - 2)).trim();
                            } else {
                                whenToTakeAction = lastToken;
                                actionToTake = String.join(" ", Arrays.copyOf(tokens, tokens.length - 1)).trim();
                            }
                            foundDate = true;
                            break;
                        }
                    }
                }
            }
            if (foundDate && whenToTakeAction != null && actionToTake.endsWith(whenToTakeAction)) {
                actionToTake = actionToTake.substring(0, actionToTake.length() - whenToTakeAction.length()).trim();
            }
        }

        ActionNext nextAction = new ActionNext();
        nextAction.setProject(foundProject);
        nextAction.setProjectId(foundProject.getProjectId());
        nextAction.setContactId(webUser.getContactId());
        Date actionDate = parseWhenToTakeAction(webUser, whenToTakeAction);
        if (actionVerb.equals("I will")) {
            nextAction.setNextActionType(ProjectNextActionType.WILL);
        } else if (actionVerb.equals("I might")) {
            nextAction.setNextActionType(ProjectNextActionType.MIGHT);
        } else if (actionVerb.equals("I would like to")) {
            nextAction.setNextActionType(ProjectNextActionType.WOULD_LIKE_TO);
        } else if (actionVerb.equals("I have committed")) {
            nextAction.setNextActionType(ProjectNextActionType.COMMITTED_TO);
        } else if (actionVerb.equals("I will meet")) {
            nextAction.setNextActionType(ProjectNextActionType.WILL_MEET);
        } else if (actionVerb.equals("I have set goal to")) {
            nextAction.setNextActionType(ProjectNextActionType.GOAL);
        } else if (actionVerb.equals("I am waiting")) {
            nextAction.setNextActionType(ProjectNextActionType.WAITING);
        } else {
            nextAction.setNextActionType(ProjectNextActionType.WILL);
        }
        nextAction.setNextActionDate(actionDate);
        nextAction.setNextDescription(actionToTake);
        nextAction.setNextTimeEstimate(nextTimeEstimate);
        nextAction.setNextChangeDate(new Date());
        Integer workspaceId = workspaceIdOverride != null ? workspaceIdOverride
                : WorkspaceRegistry.getWorkspaceIdForWebUserId(webUser.getWebUserId());
        nextAction.setWorkspaceId(workspaceId);
        nextAction.setContact(webUser.getProjectContact());
        nextAction.setBillable(resolveBillable(dataSession, foundProject));
        if (extractedUrl != null && extractedUrl.length() > 0) {
            nextAction.setLinkUrl(extractedUrl);
        }
        defaultPersonalTimeSlot(nextAction);
        if (nextAction.getNextActionStatus() == null) {
            if (nextAction.hasNextDescription()) {
                if (nextAction.hasNextActionDate()) {
                    nextAction.setNextActionStatus(ProjectNextActionStatus.READY);
                } else {
                    nextAction.setNextActionStatus(ProjectNextActionStatus.PROPOSED);
                }
            }
        }
        nextAction.setActionSet(new ActionSetDao(dataSession).createStandardActionSet(webUser));
        return nextAction;
    }

    private boolean resolveBillable(Session dataSession, Project project) {
        if (project == null || project.getBillCode() == null || project.getBillCode().equals("")) {
            return false;
        }
        BillCode billCode = ClientServlet.resolveBillCode(dataSession, project);
        return billCode != null && "Y".equalsIgnoreCase(billCode.getBillable());
    }

    private Date parseWhenToTakeAction(WebUser webUser, String whenToTakeAction) {
        Date actionDate = webUser.getCalendar().getTime();
        if (whenToTakeAction == null || whenToTakeAction.length() == 0) {
            return actionDate;
        }

        Calendar calendar = webUser.getCalendar();
        String lower = whenToTakeAction.trim().toLowerCase();
        String[] days = { "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
        boolean isNext = lower.startsWith("next ");
        String dayName = isNext ? lower.substring(5).trim() : lower;
        int dayOfWeek = -1;
        for (int i = 0; i < days.length; i++) {
            if (days[i].equals(dayName)) {
                dayOfWeek = i + 1;
                break;
            }
        }
        if (lower.equals("today")) {
            // keep today
        } else if (lower.equals("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else if (dayOfWeek != -1) {
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysUntil = dayOfWeek - currentDayOfWeek;
            if (isNext) {
                if (daysUntil <= 0) {
                    daysUntil += 7;
                }
                daysUntil += 7;
            } else if (daysUntil < 0) {
                daysUntil += 7;
            }
            calendar.add(Calendar.DAY_OF_YEAR, daysUntil);
        } else {
            try {
                Date parsedDate = webUser.getDateFormat().parse(whenToTakeAction);
                calendar.setTime(parsedDate);
            } catch (Exception e) {
                // leave current date as fallback
            }
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        actionDate = calendar.getTime();
        return actionDate;
    }

    private void defaultPersonalTimeSlot(ActionNext projectAction) {
        if (projectAction != null && !projectAction.isBillable() && projectAction.getTimeSlot() == null) {
            projectAction.setTimeSlot(TimeSlot.AFTERNOON);
        }
    }

    private UrlExtractionResult extractAndRemoveUrl(String text) {
        if (text == null || text.isEmpty()) {
            return new UrlExtractionResult(text, null);
        }
        int urlStartIndex = text.indexOf("https://");
        if (urlStartIndex == -1) {
            return new UrlExtractionResult(text, null);
        }
        int urlEndIndex = text.indexOf(' ', urlStartIndex);
        if (urlEndIndex == -1) {
            urlEndIndex = text.length();
        }
        String extractedUrl = text.substring(urlStartIndex, urlEndIndex);
        String cleanedText = text.substring(0, urlStartIndex) + text.substring(urlEndIndex);
        cleanedText = cleanedText.trim().replaceAll("\\s+", " ");
        return new UrlExtractionResult(cleanedText, extractedUrl);
    }

    private static class UrlExtractionResult {
        private final String cleanedText;
        private final String extractedUrl;

        private UrlExtractionResult(String cleanedText, String extractedUrl) {
            this.cleanedText = cleanedText;
            this.extractedUrl = extractedUrl;
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}