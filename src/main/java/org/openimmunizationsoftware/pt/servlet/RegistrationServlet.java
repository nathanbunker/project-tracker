package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.format.DateFormatService;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ProcessStage;
import org.openimmunizationsoftware.pt.model.Project;
import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.ProjectContactAssigned;
import org.openimmunizationsoftware.pt.model.ProjectContactAssignedId;
import org.openimmunizationsoftware.pt.model.ProjectNextActionStatus;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectProvider;
import org.openimmunizationsoftware.pt.model.TemplateType;
import org.openimmunizationsoftware.pt.model.TimeSlot;
import org.openimmunizationsoftware.pt.model.TrackerKeys;
import org.openimmunizationsoftware.pt.model.TrackerKeysId;
import org.openimmunizationsoftware.pt.model.WebUser;

public class RegistrationServlet extends ClientServlet {

    private static final String ACTION_REGISTER = "Register";
    private static final String ACTION_COMPLETE_SETUP = "Get Started";

    private static final String PARAM_FIRST_NAME = "firstName";
    private static final String PARAM_LAST_NAME = "lastName";
    private static final String PARAM_EMAIL = "emailAddress";
    private static final String PARAM_STATUS = "status";
    private static final String PARAM_TIME_ZONE = "timeZone";
    private static final String PARAM_DATE_FORMAT = "dateFormat";
    private static final String PARAM_TIME_FORMAT = "timeFormat";
    private static final String PARAM_TRACK_TIME = "trackTime";
    private static final String PARAM_WORK_PROJECTS = "workProjects";
    private static final String PARAM_PERSONAL_PROJECTS = "personalProjects";

    private static final String STATUS_SETUP = "setup";
    private static final int MAGIC_LINK_MINUTES_VALID = 20;
    private static final int EXTRA_TEMPLATE_ROWS = 3;
    private static final boolean TEMP_SHOW_MAGIC_LINK_ON_PAGE = true;

    private static final String SECTION_WORK = "work";
    private static final String SECTION_PERSONAL = "personal";

    private static final String DEFAULT_WORK_PROJECTS = "Overhead, Email, Meetings";
    private static final String DEFAULT_PERSONAL_PROJECTS = "Finances, Exercise, Family, Home";

    private static final String[][] DATE_FORMAT_OPTIONS = {
            { DateFormatService.PATTERN_DATE_SHORT, "MM/dd/yyyy (US)" },
            { DateFormatService.PATTERN_DATE_SHORT_EU, "dd/MM/yyyy (Europe)" },
            { DateFormatService.PATTERN_TRANSPORT_DATE, "yyyy-MM-dd (ISO)" } };

    private static final String[][] TIME_FORMAT_OPTIONS = {
            { DateFormatService.PATTERN_TIME_12H, "hh:mm AM/PM (12-hour)" },
            { DateFormatService.PATTERN_TIME_24H, "HH:mm (24-hour)" } };

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}][\\p{L} .'-]{1,58}[\\p{L}]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            Session dataSession = appReq.getDataSession();
            String action = appReq.getAction();
            PrintWriter out = appReq.getOut();
            WebUser sessionWebUser = appReq.getWebUser();
            boolean onboardingMode = sessionWebUser != null && !hasProvider(sessionWebUser);

            if (sessionWebUser != null && hasProvider(sessionWebUser)) {
                response.sendRedirect("HomeServlet");
                return;
            }

            appReq.setTitle("Register");

            if (onboardingMode) {
                ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class,
                        sessionWebUser.getContactId());
                SetupFormData setupFormData = readSetupFormData(request, sessionWebUser, projectContact);
                if (STATUS_SETUP.equalsIgnoreCase(safe(request.getParameter(PARAM_STATUS)))) {
                    appReq.setMessageConfirmation(
                            "Your email has been verified. Finish setup below so your starter workspace can be created.");
                }
                if (ACTION_COMPLETE_SETUP.equals(action)) {
                    if (handleSetup(response, appReq, dataSession, sessionWebUser, projectContact,
                            setupFormData)) {
                        return;
                    }
                }
                printHtmlHead(appReq);
                printSetupForm(out, setupFormData);
                printHtmlFoot(appReq);
                return;
            }

            String firstName = safe(request.getParameter(PARAM_FIRST_NAME));
            String lastName = safe(request.getParameter(PARAM_LAST_NAME));
            String emailAddress = safe(request.getParameter(PARAM_EMAIL));

            if (ACTION_REGISTER.equals(action)) {
                handleRegistration(request, appReq, dataSession, firstName, lastName, emailAddress);
            }

            printHtmlHead(appReq);
            printRegistrationForm(out, firstName, lastName, emailAddress);
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    private void handleRegistration(HttpServletRequest request, AppReq appReq, Session dataSession,
            String firstNameInput, String lastNameInput, String emailInput) {
        String firstName = normalizeWhitespace(firstNameInput);
        String lastName = normalizeWhitespace(lastNameInput);
        String emailAddress = normalizeEmail(emailInput);

        String error = validateRegistration(firstName, lastName, emailAddress);
        if (error != null) {
            appReq.setMessageProblem(error);
            return;
        }

        Query existingQuery = dataSession
                .createQuery("from WebUser where lower(emailAddress) = ? order by webUserId desc");
        existingQuery.setParameter(0, emailAddress.toLowerCase());
        @SuppressWarnings("unchecked")
        List<WebUser> existingUsers = existingQuery.list();
        if (!existingUsers.isEmpty()) {
            appReq.setMessageProblem(
                    "That email address is already registered. Use Login to request a magic link.");
            return;
        }

        Date now = new Date();
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);
        Date expiry = new Date(System.currentTimeMillis() + (MAGIC_LINK_MINUTES_VALID * 60L * 1000L));

        WebUser webUser;
        Transaction trans = dataSession.beginTransaction();
        try {
            ProjectContact projectContact = new ProjectContact();
            projectContact.setNameFirst(firstName);
            projectContact.setNameLast(lastName);
            projectContact.setEmailAddress(emailAddress);
            projectContact.setEmailConfirmed(false);
            projectContact.setEmailAlert("N");
            ProjectProvider defaultProvider = (ProjectProvider) dataSession.get(ProjectProvider.class, "12");
            if (defaultProvider == null) {
                Query providerQuery = dataSession.createQuery("from ProjectProvider order by providerId");
                providerQuery.setMaxResults(1);
                defaultProvider = (ProjectProvider) providerQuery.uniqueResult();
            }
            projectContact.setProvider(defaultProvider);
            dataSession.save(projectContact);

            webUser = new WebUser();
            webUser.setUsername(generateUniqueUsername(dataSession, firstName, lastName, emailAddress));
            webUser.setFirstName(firstName);
            webUser.setLastName(lastName);
            webUser.setEmailAddress(emailAddress);
            webUser.setEmailVerified(false);
            webUser.setContactId(projectContact.getContactId());
            webUser.setPassword(null);
            webUser.setProvider(null);
            webUser.setUserType(WebUser.USER_TYPE_USER);
            webUser.setRegistrationStatus("ACTIVE");
            webUser.setCreatedDate(now);
            webUser.setVerifiedDate(null);
            webUser.setLastLoginDate(null);
            webUser.setMagicLinkTokenHash(tokenHash);
            webUser.setMagicLinkExpiry(expiry);
            dataSession.save(webUser);

            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            appReq.setMessageProblem("Unable to create registration: " + e.getMessage());
            return;
        }

        String magicLink = buildMagicLinkUrl(request, dataSession, webUser.getWebUserId(), rawToken);

        if (TEMP_SHOW_MAGIC_LINK_ON_PAGE) {
            appReq.setMessageConfirmation("Temporary test mode: "
                    + "<a href=\"" + magicLink + "\">Open your registration magic link</a>"
                    + " (valid for " + MAGIC_LINK_MINUTES_VALID + " minutes)");
            return;
        }

        String body = "<p>Welcome to Dandelion.</p>"
                + "<p><a href=\"" + magicLink + "\">Verify your email and continue registration</a></p>"
                + "<p>This link expires in " + MAGIC_LINK_MINUTES_VALID + " minutes.</p>";
        try {
            MailManager mailManager = new MailManager(dataSession);
            mailManager.sendEmail("Dandelion Registration Link", body, emailAddress);
            appReq.setMessageConfirmation("Registration started. Check your email for the magic link.");
        } catch (Exception e) {
            appReq.setMessageProblem("Unable to send registration magic link: " + e.getMessage());
        }
    }

    private boolean handleSetup(HttpServletResponse response, AppReq appReq, Session dataSession,
            WebUser sessionWebUser, ProjectContact sessionProjectContact, SetupFormData setupFormData)
            throws IOException {
        String validationError = validateSetup(setupFormData);
        if (validationError != null) {
            appReq.setMessageProblem(validationError);
            return false;
        }

        Transaction transaction = dataSession.beginTransaction();
        try {
            WebUser webUser = (WebUser) dataSession.get(WebUser.class, sessionWebUser.getWebUserId());
            ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class,
                    sessionProjectContact.getContactId());
            Date now = new Date();

            ProjectProvider provider = createUniqueProvider(dataSession, setupFormData.firstName,
                    setupFormData.lastName);
            dataSession.save(provider);

            projectContact.setNameFirst(setupFormData.firstName);
            projectContact.setNameLast(setupFormData.lastName);
            projectContact.setTimeZone(setupFormData.timeZone);
            projectContact.setProvider(provider);
            projectContact.setEmailConfirmed(true);
            dataSession.update(projectContact);

            webUser.setFirstName(setupFormData.firstName);
            webUser.setLastName(setupFormData.lastName);
            webUser.setProvider(provider);
            webUser.setEmailVerified(true);
            if (webUser.getVerifiedDate() == null) {
                webUser.setVerifiedDate(now);
            }
            dataSession.update(webUser);

            saveUserKeyValue(dataSession, webUser, TrackerKeysManager.KEY_TRACK_TIME,
                    setupFormData.trackTime ? "Y" : "N");
            saveUserKeyValue(dataSession, webUser, TrackerKeysManager.KEY_TIME_ZONE, setupFormData.timeZone);
            saveUserKeyValue(dataSession, webUser, TrackerKeysManager.KEY_DATE_DISPLAY_FORMAT,
                    setupFormData.dateFormat);
            saveUserKeyValue(dataSession, webUser, TrackerKeysManager.KEY_DATE_ENTRY_FORMAT,
                    setupFormData.dateFormat);
            saveUserKeyValue(dataSession, webUser, TrackerKeysManager.KEY_TIME_DISPLAY_FORMAT,
                    setupFormData.timeFormat);
            saveUserKeyValue(dataSession, webUser, TrackerKeysManager.KEY_TIME_ENTRY_FORMAT,
                    setupFormData.timeFormat);

            BillCode workBillCode = createBillCode(provider, "Work", "Work", "Y");
            BillCode personalBillCode = createBillCode(provider, "Personal", "Personal", "N");
            dataSession.save(workBillCode);
            dataSession.save(personalBillCode);

            LinkedHashMap<String, Project> workProjectMap = new LinkedHashMap<String, Project>();
            LinkedHashMap<String, Project> personalProjectMap = new LinkedHashMap<String, Project>();

            for (String projectName : setupFormData.workProjectNames) {
                createProjectIfNeeded(dataSession, workProjectMap, projectName, workBillCode, provider, webUser,
                        projectContact);
            }
            for (String projectName : setupFormData.personalProjectNames) {
                createProjectIfNeeded(dataSession, personalProjectMap, projectName, personalBillCode, provider,
                        webUser, projectContact);
            }

            Date templateAnchorDate = calculateEndOfYear(webUser);
            for (TemplateRow templateRow : setupFormData.getEnabledTemplateRows()) {
                boolean workSection = SECTION_WORK.equals(templateRow.section);
                BillCode billCode = workSection ? workBillCode : personalBillCode;
                LinkedHashMap<String, Project> projectMap = workSection ? workProjectMap : personalProjectMap;
                Project project = createProjectIfNeeded(dataSession, projectMap, templateRow.projectName, billCode,
                        provider, webUser, projectContact);
                ProjectActionNext templateAction = createTemplateAction(projectContact, provider, project,
                        templateRow, templateAnchorDate);
                dataSession.save(templateAction);
                for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
                    ProjectActionNext scheduledAction = createScheduledAction(webUser, projectContact, provider,
                            project, templateAction, webUser.addDays(webUser.getToday(), dayOffset));
                    dataSession.save(scheduledAction);
                }
            }

            transaction.commit();

            WebUser refreshedWebUser = (WebUser) dataSession.get(WebUser.class, webUser.getWebUserId());
            if (refreshedWebUser.getProvider() != null) {
                refreshedWebUser.getProvider().getProviderId();
            }
            refreshedWebUser.setProjectContact(projectContact);
            refreshedWebUser.setTrackTime(setupFormData.trackTime);
            refreshedWebUser.setTimeZone(TimeZone.getTimeZone(setupFormData.timeZone));
            refreshedWebUser.setDateDisplayPattern(setupFormData.dateFormat);
            refreshedWebUser.setDateEntryPattern(setupFormData.dateFormat);
            refreshedWebUser.setTimeDisplayPattern(setupFormData.timeFormat);
            refreshedWebUser.setTimeEntryPattern(setupFormData.timeFormat);
            appReq.setWebUser(refreshedWebUser);
            if (refreshedWebUser.isTrackTime()) {
                appReq.setTimeTracker(new TimeTracker(refreshedWebUser, dataSession));
            } else {
                appReq.setTimeTracker(null);
            }
            response.sendRedirect("ProjectActionServlet");
            return true;
        } catch (Exception e) {
            transaction.rollback();
            appReq.setMessageProblem("Unable to finish registration setup: " + e.getMessage());
            return false;
        }
    }

    private String validateRegistration(String firstName, String lastName, String emailAddress) {
        if (firstName.length() < 3 || firstName.length() > 60) {
            return "First name must be between 3 and 60 characters";
        }
        if (lastName.length() < 3 || lastName.length() > 60) {
            return "Last name must be between 3 and 60 characters";
        }
        if (!NAME_PATTERN.matcher(firstName).matches()) {
            return "First name has invalid characters";
        }
        if (!NAME_PATTERN.matcher(lastName).matches()) {
            return "Last name has invalid characters";
        }
        if (emailAddress.length() < 5 || emailAddress.length() > 254
                || !EMAIL_PATTERN.matcher(emailAddress).matches()) {
            return "Email address does not look valid";
        }
        return null;
    }

    private String validateSetup(SetupFormData setupFormData) {
        String registrationError = validateRegistration(setupFormData.firstName, setupFormData.lastName,
                setupFormData.emailAddress);
        if (registrationError != null) {
            return registrationError;
        }
        if (!isRecognizedTimeZone(setupFormData.timeZone)) {
            return "Please select a valid time zone";
        }
        if (!isAllowedOption(DATE_FORMAT_OPTIONS, setupFormData.dateFormat)) {
            return "Please select a supported date format";
        }
        if (!isAllowedOption(TIME_FORMAT_OPTIONS, setupFormData.timeFormat)) {
            return "Please select a supported time format";
        }
        if (setupFormData.workProjectNames.isEmpty()) {
            return "Add at least one work project name";
        }
        if (setupFormData.personalProjectNames.isEmpty()) {
            return "Add at least one personal project name";
        }
        if (setupFormData.getEnabledTemplateRows().isEmpty()) {
            return "Enable at least one daily action so we can create your starter plan";
        }
        for (TemplateRow templateRow : setupFormData.getEnabledTemplateRows()) {
            if (normalizeWhitespace(templateRow.projectName).equals("")) {
                return "Every enabled template needs a project name";
            }
            if (normalizeWhitespace(templateRow.action).equals("")) {
                return "Every enabled template needs an action description";
            }
            if (SECTION_WORK.equals(templateRow.section)
                    && !safe(templateRow.processStageId).equals("")
                    && ProcessStage.getProcessStage(templateRow.processStageId) == null) {
                return "One of the work template process stages is invalid";
            }
            if (SECTION_PERSONAL.equals(templateRow.section)
                    && !safe(templateRow.timeSlotId).equals("")
                    && TimeSlot.getTimeSlot(templateRow.timeSlotId) == null) {
                return "One of the personal template time slots is invalid";
            }
            if (!safe(templateRow.timeEstimate).equals("")) {
                try {
                    TimeTracker.readTime(templateRow.timeEstimate);
                } catch (Exception e) {
                    return "Time estimate values must be whole minutes or h:mm";
                }
            }
        }
        return null;
    }

    private SetupFormData readSetupFormData(HttpServletRequest request, WebUser webUser,
            ProjectContact projectContact) {
        SetupFormData formData = createDefaultSetupFormData(webUser, projectContact);
        if (request.getParameter(PARAM_FIRST_NAME) != null || request.getParameter(PARAM_WORK_PROJECTS) != null
                || ACTION_COMPLETE_SETUP.equals(request.getParameter("action"))) {
            formData.firstName = normalizeWhitespace(request.getParameter(PARAM_FIRST_NAME));
            formData.lastName = normalizeWhitespace(request.getParameter(PARAM_LAST_NAME));
            formData.timeZone = safe(request.getParameter(PARAM_TIME_ZONE));
            formData.dateFormat = safe(request.getParameter(PARAM_DATE_FORMAT));
            formData.timeFormat = safe(request.getParameter(PARAM_TIME_FORMAT));
            formData.trackTime = request.getParameter(PARAM_TRACK_TIME) != null;
            formData.workProjects = safe(request.getParameter(PARAM_WORK_PROJECTS));
            formData.personalProjects = safe(request.getParameter(PARAM_PERSONAL_PROJECTS));
            formData.workProjectNames = parseProjectNames(formData.workProjects);
            formData.personalProjectNames = parseProjectNames(formData.personalProjects);
            readTemplateRows(request, formData.workTemplateRows, SECTION_WORK);
            readTemplateRows(request, formData.personalTemplateRows, SECTION_PERSONAL);
        }
        return formData;
    }

    private SetupFormData createDefaultSetupFormData(WebUser webUser, ProjectContact projectContact) {
        SetupFormData formData = new SetupFormData();
        formData.emailAddress = safe(webUser.getEmailAddress());
        formData.firstName = normalizeWhitespace(firstNonEmpty(webUser.getFirstName(),
                projectContact == null ? null : projectContact.getNameFirst()));
        formData.lastName = normalizeWhitespace(firstNonEmpty(webUser.getLastName(),
                projectContact == null ? null : projectContact.getNameLast()));
        formData.timeZone = WebUser.AMERICA_DENVER;
        if (projectContact != null && projectContact.getTimeZone() != null
                && !projectContact.getTimeZone().trim().equals("")) {
            formData.timeZone = projectContact.getTimeZone().trim();
        }
        formData.dateFormat = DateFormatService.PATTERN_DATE_SHORT;
        formData.timeFormat = DateFormatService.PATTERN_TIME_12H;
        formData.trackTime = true;
        formData.workProjects = DEFAULT_WORK_PROJECTS;
        formData.personalProjects = DEFAULT_PERSONAL_PROJECTS;
        formData.workProjectNames = parseProjectNames(formData.workProjects);
        formData.personalProjectNames = parseProjectNames(formData.personalProjects);

        formData.workTemplateRows.add(createWorkTemplateRow(true, "Email", "clear email start of day",
                ProcessStage.FIRST, "0:20"));
        formData.workTemplateRows.add(createWorkTemplateRow(true, "Email", "clear email end of day",
                ProcessStage.PENULTIMATE, "0:20"));
        formData.workTemplateRows.add(createWorkTemplateRow(true, "Overhead", "plan for today",
                ProcessStage.SECOND, "0:15"));
        formData.workTemplateRows.add(createWorkTemplateRow(true, "Overhead", "plan for tomorrow",
                ProcessStage.LAST, "0:15"));

        formData.personalTemplateRows.add(createPersonalTemplateRow(true, "Exercise", "workout",
                TimeSlot.AFTERNOON, "1:00"));

        appendBlankTemplateRows(formData.workTemplateRows, SECTION_WORK, EXTRA_TEMPLATE_ROWS);
        appendBlankTemplateRows(formData.personalTemplateRows, SECTION_PERSONAL, EXTRA_TEMPLATE_ROWS);
        return formData;
    }

    private TemplateRow createWorkTemplateRow(boolean enabled, String projectName, String action,
            ProcessStage processStage, String timeEstimate) {
        TemplateRow templateRow = new TemplateRow();
        templateRow.section = SECTION_WORK;
        templateRow.enabled = enabled;
        templateRow.projectName = projectName;
        templateRow.action = action;
        templateRow.processStageId = processStage == null ? "" : processStage.getId();
        templateRow.timeEstimate = timeEstimate;
        return templateRow;
    }

    private TemplateRow createPersonalTemplateRow(boolean enabled, String projectName, String action,
            TimeSlot timeSlot, String timeEstimate) {
        TemplateRow templateRow = new TemplateRow();
        templateRow.section = SECTION_PERSONAL;
        templateRow.enabled = enabled;
        templateRow.projectName = projectName;
        templateRow.action = action;
        templateRow.timeSlotId = timeSlot == null ? "" : timeSlot.getId();
        templateRow.timeEstimate = timeEstimate;
        return templateRow;
    }

    private void appendBlankTemplateRows(List<TemplateRow> templateRows, String section, int count) {
        for (int i = 0; i < count; i++) {
            TemplateRow templateRow = new TemplateRow();
            templateRow.section = section;
            templateRow.enabled = false;
            templateRows.add(templateRow);
        }
    }

    private void readTemplateRows(HttpServletRequest request, List<TemplateRow> templateRows,
            String section) {
        for (int i = 0; i < templateRows.size(); i++) {
            TemplateRow templateRow = templateRows.get(i);
            templateRow.enabled = request.getParameter(section + "TemplateEnabled" + i) != null;
            templateRow.projectName = normalizeWhitespace(request.getParameter(section + "TemplateProject" + i));
            templateRow.action = normalizeWhitespace(request.getParameter(section + "TemplateAction" + i));
            templateRow.processStageId = safe(request.getParameter(section + "TemplateProcessStage" + i));
            templateRow.timeSlotId = safe(request.getParameter(section + "TemplateTimeSlot" + i));
            templateRow.timeEstimate = normalizeWhitespace(request.getParameter(section + "TemplateTime" + i));
        }
    }

    private List<String> parseProjectNames(String projectListString) {
        LinkedHashSet<String> projectNames = new LinkedHashSet<String>();
        for (String part : safe(projectListString).split(",")) {
            String projectName = normalizeWhitespace(part);
            if (!projectName.equals("")) {
                projectNames.add(projectName);
            }
        }
        return new ArrayList<String>(projectNames);
    }

    private ProjectProvider createUniqueProvider(Session dataSession, String firstName, String lastName) {
        String baseProviderName = normalizeWhitespace(firstName + " " + lastName);
        if (baseProviderName.equals("")) {
            baseProviderName = "New Provider";
        }
        String baseProviderId = sanitizeProviderId(firstName + lastName);
        if (baseProviderId.equals("")) {
            baseProviderId = "provider";
        }
        int suffix = 1;
        while (true) {
            String candidateName = suffix == 1 ? baseProviderName : baseProviderName + " " + suffix;
            String candidateId = suffix == 1 ? baseProviderId : appendSuffix(baseProviderId, String.valueOf(suffix));
            Query query = dataSession.createQuery(
                    "select count(*) from ProjectProvider where providerId = ? or providerName = ?");
            query.setParameter(0, candidateId);
            query.setParameter(1, candidateName);
            Number count = (Number) query.uniqueResult();
            if (count == null || count.longValue() == 0) {
                ProjectProvider projectProvider = new ProjectProvider();
                projectProvider.setProviderId(candidateId);
                projectProvider.setProviderName(candidateName);
                return projectProvider;
            }
            suffix++;
        }
    }

    private String sanitizeProviderId(String value) {
        String sanitized = safe(value).replaceAll("[^A-Za-z0-9]", "");
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }
        return sanitized;
    }

    private String appendSuffix(String base, String suffix) {
        int maxBaseLength = 30 - suffix.length();
        String trimmedBase = base;
        if (trimmedBase.length() > maxBaseLength) {
            trimmedBase = trimmedBase.substring(0, maxBaseLength);
        }
        return trimmedBase + suffix;
    }

    private BillCode createBillCode(ProjectProvider provider, String billCodeValue, String billLabel,
            String billable) {
        BillCode billCode = new BillCode();
        billCode.setProvider(provider);
        billCode.setBillCode(billCodeValue);
        billCode.setBillLabel(billLabel);
        billCode.setBillable(billable);
        billCode.setVisible("Y");
        billCode.setEstimateMin(0);
        billCode.setBillRate(0);
        billCode.setBillRound(1);
        return billCode;
    }

    private Project createProjectIfNeeded(Session dataSession, Map<String, Project> projectMap,
            String projectName, BillCode billCode, ProjectProvider provider, WebUser webUser,
            ProjectContact projectContact) {
        String key = normalizeLookupKey(projectName);
        Project existing = projectMap.get(key);
        if (existing != null) {
            return existing;
        }

        Project project = new Project();
        project.setProvider(provider);
        project.setProviderName(provider.getProviderName());
        project.setWebUser(webUser);
        project.setProjectName(trim(projectName, 100));
        project.setBillCode(billCode.getBillCode());
        project.setPhaseCode("Unkn");
        project.setPriorityLevel(0);
        dataSession.save(project);

        ProjectContactAssigned projectContactAssigned = new ProjectContactAssigned();
        ProjectContactAssignedId projectContactAssignedId = new ProjectContactAssignedId();
        projectContactAssignedId.setContactId(projectContact.getContactId());
        projectContactAssignedId.setProjectId(project.getProjectId());
        projectContactAssigned.setId(projectContactAssignedId);
        projectContactAssigned.setEmailAlert("Y");
        projectContactAssigned.setUpdateDue(0);
        dataSession.save(projectContactAssigned);

        projectMap.put(key, project);
        return project;
    }

    private ProjectActionNext createTemplateAction(ProjectContact projectContact, ProjectProvider provider,
            Project project, TemplateRow templateRow, Date templateAnchorDate) {
        ProjectActionNext templateAction = new ProjectActionNext();
        templateAction.setProjectId(project.getProjectId());
        templateAction.setContactId(projectContact.getContactId());
        templateAction.setContact(projectContact);
        templateAction.setProvider(provider);
        templateAction.setNextActionStatus(ProjectNextActionStatus.READY);
        templateAction.setNextChangeDate(new Date());
        templateAction.setNextDescription(trim(templateRow.action, 1200));
        templateAction.setBillable(SECTION_WORK.equals(templateRow.section));
        templateAction.setNextActionDate(templateAnchorDate);
        templateAction.setNextActionType(ProjectNextActionType.WILL);
        templateAction.setNextTimeEstimate(readTimeEstimate(templateRow.timeEstimate));
        templateAction.setPriorityLevel(project.getPriorityLevel());
        templateAction.setTemplateType(TemplateType.DAILY);
        templateAction.setProcessStage(ProcessStage.getProcessStage(templateRow.processStageId));
        TimeSlot timeSlot = TimeSlot.getTimeSlot(templateRow.timeSlotId);
        if (!SECTION_WORK.equals(templateRow.section) && timeSlot == null) {
            timeSlot = TimeSlot.AFTERNOON;
        }
        templateAction.setTimeSlot(timeSlot);
        return templateAction;
    }

    private ProjectActionNext createScheduledAction(WebUser webUser, ProjectContact projectContact,
            ProjectProvider provider, Project project, ProjectActionNext templateAction, Date actionDate) {
        ProjectActionNext projectAction = new ProjectActionNext();
        projectAction.setProjectId(project.getProjectId());
        projectAction.setContactId(projectContact.getContactId());
        projectAction.setContact(projectContact);
        projectAction.setProvider(provider);
        projectAction.setNextActionStatus(ProjectNextActionStatus.READY);
        projectAction.setNextChangeDate(new Date());
        projectAction.setNextDescription(templateAction.getNextDescription());
        projectAction.setBillable(templateAction.isBillable());
        projectAction.setNextActionDate(actionDate);
        projectAction.setNextActionType(ProjectNextActionType.WILL);
        projectAction.setNextTimeEstimate(templateAction.getNextTimeEstimate());
        projectAction.setPriorityLevel(project.getPriorityLevel());
        projectAction.setTemplateActionNextId(templateAction.getActionNextId());
        projectAction.setProcessStage(templateAction.getProcessStage());
        projectAction.setTimeSlot(templateAction.getTimeSlot());
        return projectAction;
    }

    private int readTimeEstimate(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized.equals("")) {
            return 0;
        }
        return TimeTracker.readTime(normalized);
    }

    private Date calculateEndOfYear(WebUser webUser) {
        java.util.Calendar calendar = webUser.getCalendar();
        calendar.add(java.util.Calendar.MONTH, 1);
        calendar.set(java.util.Calendar.MONTH, 11);
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 31);
        return calendar.getTime();
    }

    private void saveUserKeyValue(Session dataSession, WebUser webUser, String keyName, String value) {
        TrackerKeysId trackerKeysId = new TrackerKeysId(keyName, TrackerKeysManager.KEY_TYPE_USER,
                webUser.getUsername());
        TrackerKeys trackerKeys = (TrackerKeys) dataSession.get(TrackerKeys.class, trackerKeysId);
        if (trackerKeys == null) {
            trackerKeys = new TrackerKeys();
            trackerKeys.setId(trackerKeysId);
            trackerKeys.setKeyValue(value);
            dataSession.save(trackerKeys);
        } else {
            trackerKeys.setKeyValue(value);
            dataSession.update(trackerKeys);
        }
    }

    private String generateUniqueUsername(Session dataSession, String firstName, String lastName,
            String emailAddress) {
        String base = emailAddress.toLowerCase();
        int atPos = base.indexOf('@');
        if (atPos > 0) {
            base = base.substring(0, atPos);
        }
        base = base.replaceAll("[^a-z0-9_]", "");
        if (base.length() < 3) {
            String f = firstName.toLowerCase().replaceAll("[^a-z]", "");
            String l = lastName.toLowerCase().replaceAll("[^a-z]", "");
            base = (f + "_" + l).replaceAll("[^a-z0-9_]", "");
        }
        if (base.length() < 3) {
            base = "user";
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }

        String candidate = base;
        int suffix = 1;
        while (usernameExists(dataSession, candidate)) {
            candidate = base + "_" + suffix;
            if (candidate.length() > 30) {
                candidate = candidate.substring(0, 30);
            }
            suffix++;
        }
        return candidate;
    }

    private boolean usernameExists(Session dataSession, String username) {
        Query query = dataSession.createQuery("select count(*) from WebUser where username = ?");
        query.setParameter(0, username);
        Number count = (Number) query.uniqueResult();
        return count != null && count.longValue() > 0;
    }

    private String buildMagicLinkUrl(HttpServletRequest request, Session dataSession, int webUserId,
            String rawToken) {
        String externalUrl = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, "", dataSession);
        String baseLoginUrl;
        if (externalUrl == null || externalUrl.trim().equals("")) {
            String requestUrl = request.getRequestURL().toString();
            String servletPath = request.getServletPath();
            int servletPathStart = requestUrl.indexOf(servletPath);
            String rootUrl = servletPathStart > 0 ? requestUrl.substring(0, servletPathStart + 1)
                    : requestUrl;
            baseLoginUrl = rootUrl + "LoginServlet";
        } else {
            String normalized = externalUrl.trim();
            if (normalized.endsWith("LoginServlet")) {
                baseLoginUrl = normalized;
            } else {
                if (!normalized.endsWith("/")) {
                    normalized += "/";
                }
                baseLoginUrl = normalized + "LoginServlet";
            }
        }

        try {
            return baseLoginUrl
                    + "?action=" + URLEncoder.encode("MagicLogin", "UTF-8")
                    + "&magicUserId=" + webUserId
                    + "&magicToken=" + URLEncoder.encode(rawToken, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return baseLoginUrl
                    + "?action=MagicLogin"
                    + "&magicUserId=" + webUserId
                    + "&magicToken=" + rawToken;
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to hash token", e);
        }
    }

    private void printRegistrationForm(PrintWriter out, String firstName, String lastName,
            String emailAddress) {
        out.println("<h2>Register</h2>");
        out.println(
                "<p>Start with your name and email address. We will send you a magic link so you can finish setup.</p>");
        out.println("<form action=\"RegistrationServlet\" method=\"POST\">");
        out.println("<table class=\"boxed\">");
        out.println("  <tr><th class=\"title\" colspan=\"2\">Create Account</th></tr>");
        out.println("  <tr><th class=\"boxed\">First name</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_FIRST_NAME + "\" size=\"40\" value=\"" + escapeHtml(firstName) + "\"></td></tr>");
        out.println("  <tr><th class=\"boxed\">Last name</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_LAST_NAME + "\" size=\"40\" value=\"" + escapeHtml(lastName) + "\"></td></tr>");
        out.println("  <tr><th class=\"boxed\">Email address</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_EMAIL + "\" size=\"40\" value=\"" + escapeHtml(emailAddress) + "\"></td></tr>");
        out.println(
                "  <tr><td class=\"boxed-submit\" colspan=\"2\" align=\"right\"><input type=\"submit\" name=\"action\" value=\""
                        + ACTION_REGISTER + "\"></td></tr>");
        out.println("</table>");
        out.println("</form>");
    }

    private void printSetupForm(PrintWriter out, SetupFormData formData) {
        out.println("<h2>Finish Registration</h2>");
        out.println(
                "<p>We will create your starter workspace, two bill codes, your projects, and a week of suggested daily actions.</p>");
        out.println("<form action=\"RegistrationServlet\" method=\"POST\" accept-charset=\"UTF-8\">");

        out.println("<table class=\"boxed\">");
        out.println("  <tr><th class=\"title\" colspan=\"2\">Profile</th></tr>");
        out.println("  <tr><th class=\"boxed\">First Name</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_FIRST_NAME + "\" value=\"" + escapeHtml(formData.firstName) + "\" size=\"40\"></td></tr>");
        out.println("  <tr><th class=\"boxed\">Last Name</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_LAST_NAME + "\" value=\"" + escapeHtml(formData.lastName) + "\" size=\"40\"></td></tr>");
        out.println("  <tr><th class=\"boxed\">Time Zone</th><td class=\"boxed\"><select name=\""
                + PARAM_TIME_ZONE + "\">");
        for (String timeZoneId : TimeZone.getAvailableIDs()) {
            out.println("    <option value=\"" + timeZoneId + "\""
                    + (timeZoneId.equals(formData.timeZone) ? " selected" : "") + ">" + timeZoneId + "</option>");
        }
        out.println("  </select></td></tr>");
        out.println("  <tr><th class=\"boxed\">Date Format</th><td class=\"boxed\"><select name=\""
                + PARAM_DATE_FORMAT + "\">");
        printOptionList(out, DATE_FORMAT_OPTIONS, formData.dateFormat);
        out.println("  </select></td></tr>");
        out.println("  <tr><th class=\"boxed\">Time Format</th><td class=\"boxed\"><select name=\""
                + PARAM_TIME_FORMAT + "\">");
        printOptionList(out, TIME_FORMAT_OPTIONS, formData.timeFormat);
        out.println("  </select></td></tr>");
        out.println("  <tr><th class=\"boxed\">Track Time</th><td class=\"boxed\"><input type=\"checkbox\" name=\""
                + PARAM_TRACK_TIME + "\" value=\"Y\"" + (formData.trackTime ? " checked" : "")
                + "> Enabled</td></tr>");
        out.println("</table>");

        out.println("<br/>");
        out.println("<table class=\"boxed\">");
        out.println("  <tr><th class=\"title\" colspan=\"2\">Starter Projects</th></tr>");
        out.println("  <tr><th class=\"boxed\">Work Projects</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_WORK_PROJECTS + "\" value=\"" + escapeHtml(formData.workProjects)
                + "\" size=\"90\"></td></tr>");
        out.println(
                "  <tr><td class=\"boxed\"></td><td class=\"boxed\">Suggestions: Overhead, Email, Meetings. Add more names separated by commas.</td></tr>");
        out.println("  <tr><th class=\"boxed\">Personal Projects</th><td class=\"boxed\"><input type=\"text\" name=\""
                + PARAM_PERSONAL_PROJECTS + "\" value=\"" + escapeHtml(formData.personalProjects)
                + "\" size=\"90\"></td></tr>");
        out.println(
                "  <tr><td class=\"boxed\"></td><td class=\"boxed\">Suggestions: Finances, Exercise, Family, Home. Add more names separated by commas.</td></tr>");
        out.println(
                "  <tr><td class=\"boxed\" colspan=\"2\">We will create bill codes named Work and Personal automatically.</td></tr>");
        out.println("</table>");

        out.println("<h3>Work Templates</h3>");
        out.println(
                "<p>These rows will create daily work actions. Project names can match your list above or introduce a new project. Action text should read naturally after \"I will\" and should stay lower-case for the phone workflow.</p>");
        printTemplateTable(out, formData.workTemplateRows, false);

        out.println("<h3>Personal Templates</h3>");
        out.println(
                "<p>These rows will create daily personal actions. Use the time slot to place them in your day. Action text should read naturally after \"I will\" and should stay lower-case for the phone workflow.</p>");
        printTemplateTable(out, formData.personalTemplateRows, true);

        out.println("<br/>");
        out.println("<input type=\"submit\" name=\"action\" value=\"" + ACTION_COMPLETE_SETUP + "\">");
        out.println("</form>");
    }

    private void printTemplateTable(PrintWriter out, List<TemplateRow> templateRows,
            boolean personalTable) {
        String section = personalTable ? SECTION_PERSONAL : SECTION_WORK;
        out.println("<table class=\"boxed-full\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"boxed\">Use</th>");
        out.println("    <th class=\"boxed\">Project Name</th>");
        out.println("    <th class=\"boxed\">I will</th>");
        out.println("    <th class=\"boxed\">Process Stage / Time Slot</th>");
        out.println("    <th class=\"boxed\">Time Estimate</th>");
        out.println("  </tr>");
        for (int i = 0; i < templateRows.size(); i++) {
            TemplateRow templateRow = templateRows.get(i);
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\"><input type=\"checkbox\" name=\"" + section
                    + "TemplateEnabled" + i + "\" value=\"Y\"" + (templateRow.enabled ? " checked" : "")
                    + "></td>");
            out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + section + "TemplateProject"
                    + i + "\" value=\"" + escapeHtml(templateRow.projectName) + "\" size=\"22\"></td>");
            out.println("    <td class=\"boxed\">I will <input type=\"text\" name=\"" + section
                    + "TemplateAction" + i + "\" value=\"" + escapeHtml(templateRow.action)
                    + "\" size=\"36\" autocapitalize=\"off\" spellcheck=\"false\" placeholder=\"write this in lower-case\"></td>");
            out.println("    <td class=\"boxed\">");
            if (personalTable) {
                out.println("      <select name=\"" + section + "TemplateTimeSlot" + i + "\">");
                for (TimeSlot timeSlot : TimeSlot.values()) {
                    out.println("        <option value=\"" + timeSlot.getId() + "\""
                            + (timeSlot.getId().equals(templateRow.timeSlotId) ? " selected" : "")
                            + ">" + timeSlot.getLabel() + "</option>");
                }
                out.println("      </select>");
            } else {
                out.println("      <select name=\"" + section + "TemplateProcessStage" + i + "\">");
                out.println("        <option value=\"\"></option>");
                for (ProcessStage processStage : ProcessStage.values()) {
                    out.println("        <option value=\"" + processStage.getId() + "\""
                            + (processStage.getId().equals(templateRow.processStageId) ? " selected" : "")
                            + ">" + processStage.getLabel() + "</option>");
                }
                out.println("      </select>");
            }
            out.println("    </td>");
            out.println("    <td class=\"boxed\"><input type=\"text\" name=\"" + section + "TemplateTime"
                    + i + "\" value=\"" + escapeHtml(templateRow.timeEstimate)
                    + "\" size=\"6\" placeholder=\"0:15\"></td>");
            out.println("  </tr>");
        }
        out.println("</table>");
    }

    private static void printOptionList(PrintWriter out, String[][] options, String selectedValue) {
        for (String[] option : options) {
            String value = option[0];
            String label = option[1];
            out.println("    <option value=\"" + value + "\""
                    + (value.equals(selectedValue) ? " selected" : "") + ">" + label + "</option>");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeWhitespace(String value) {
        return safe(value).trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String value) {
        return safe(value).trim().toLowerCase();
    }

    private String normalizeLookupKey(String value) {
        return normalizeWhitespace(value).toLowerCase();
    }

    private boolean isRecognizedTimeZone(String value) {
        for (String timeZoneId : TimeZone.getAvailableIDs()) {
            if (timeZoneId.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedOption(String[][] options, String value) {
        for (String[] option : options) {
            if (option[0].equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasProvider(WebUser webUser) {
        return webUser != null && webUser.getProvider() != null
                && webUser.getProvider().getProviderId() != null
                && !webUser.getProvider().getProviderId().trim().equals("");
    }

    private String firstNonEmpty(String primary, String fallback) {
        if (primary != null && !primary.trim().equals("")) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private static class SetupFormData {
        private String emailAddress = "";
        private String firstName = "";
        private String lastName = "";
        private String timeZone = WebUser.AMERICA_DENVER;
        private String dateFormat = DateFormatService.PATTERN_DATE_SHORT;
        private String timeFormat = DateFormatService.PATTERN_TIME_12H;
        private boolean trackTime = true;
        private String workProjects = DEFAULT_WORK_PROJECTS;
        private String personalProjects = DEFAULT_PERSONAL_PROJECTS;
        private List<String> workProjectNames = new ArrayList<String>();
        private List<String> personalProjectNames = new ArrayList<String>();
        private List<TemplateRow> workTemplateRows = new ArrayList<TemplateRow>();
        private List<TemplateRow> personalTemplateRows = new ArrayList<TemplateRow>();

        private List<TemplateRow> getEnabledTemplateRows() {
            List<TemplateRow> enabledRows = new ArrayList<TemplateRow>();
            for (TemplateRow templateRow : workTemplateRows) {
                if (templateRow.enabled) {
                    enabledRows.add(templateRow);
                }
            }
            for (TemplateRow templateRow : personalTemplateRows) {
                if (templateRow.enabled) {
                    enabledRows.add(templateRow);
                }
            }
            return enabledRows;
        }
    }

    private static class TemplateRow {
        private String section = "";
        private boolean enabled = false;
        private String projectName = "";
        private String action = "";
        private String processStageId = "";
        private String timeSlotId = "";
        private String timeEstimate = "";
    }
}
