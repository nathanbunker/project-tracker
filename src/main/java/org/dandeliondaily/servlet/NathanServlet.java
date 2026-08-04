package org.dandeliondaily.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dandeliondaily.nathan.NathanAccessService;
import org.dandeliondaily.nathan.NathanAccessService.CreatedAccess;
import org.dandeliondaily.nathan.NathanContent;
import org.dandeliondaily.nathan.NathanContent.CareerStory;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.NathanAccess;
import org.openimmunizationsoftware.pt.model.NathanAccessEvent;
import org.openimmunizationsoftware.pt.servlet.ClientServlet;
import org.openimmunizationsoftware.pt.util.WebEscaper;

public class NathanServlet extends ClientServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(NathanServlet.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ACCESS_REQUEST_NOTIFICATION_EMAIL = "Nathan.Bunker@gmail.com";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$", Pattern.CASE_INSENSITIVE);

    public static final String SESSION_ACCESS_ID = "NATHAN_ACCESS_ID";
    public static final String SESSION_CSRF = "NATHAN_CSRF";
    private static final String SESSION_FLASH_MESSAGE = "NATHAN_FLASH_MESSAGE";
    private static final String SESSION_FLASH_LINK = "NATHAN_FLASH_LINK";

    private final NathanAccessService accessService = new NathanAccessService();
    private final NathanContent content = new NathanContent();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            Session dataSession = appReq.getDataSession();
            String suppliedToken = trim(request.getParameter("access"));
            if ("GET".equalsIgnoreCase(request.getMethod()) && suppliedToken.length() > 0) {
                NathanAccess access = accessService.validateToken(dataSession, suppliedToken);
                if (access != null) {
                    request.getSession().setAttribute(SESSION_ACCESS_ID, Integer.valueOf(access.getNathanAccessId()));
                }
                response.setHeader("Cache-Control", "no-store");
                response.sendRedirect(request.getContextPath() + "/nathan");
                return;
            }

            Integer sessionAccessId = getSessionAccessId(request.getSession());
            boolean visitorAuthorized = sessionAccessId != null
                    && accessService.accessExists(dataSession, sessionAccessId.intValue());
            boolean authorized = appReq.isAdmin() || visitorAuthorized;

            if ("POST".equalsIgnoreCase(request.getMethod())) {
                handlePost(appReq, authorized, sessionAccessId);
                return;
            }

            if (visitorAuthorized) {
                accessService.recordPageView(dataSession, sessionAccessId.intValue());
            }

            printNathanHead(appReq);
            renderNathanHeader(appReq.getOut(), authorized);
            renderPage(appReq, authorized, sessionAccessId);
            printNathanFoot(appReq.getOut());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to render Nathan page", e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            appReq.close();
        }
    }

    private void handlePost(AppReq appReq, boolean authorized, Integer sessionAccessId) throws Exception {
        HttpServletRequest request = appReq.getRequest();
        HttpServletResponse response = appReq.getResponse();
        HttpSession webSession = request.getSession();
        if (!csrfValid(webSession, request.getParameter("csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = trim(request.getParameter("action"));
        if ("requestAccess".equals(action)) {
            String email = normalizeEmail(request.getParameter("email"));
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                setFlash(webSession, "Enter a valid email address.", null);
                redirectToPage(request, response);
                return;
            }
            accessService.requestAccess(appReq.getDataSession(), email);
            sendAccessRequestNotification(appReq.getDataSession(), email);
            setFlash(webSession, "Your request has been received.", null);
            redirectToPage(request, response);
            return;
        }

        if ("recordEvent".equals(action)) {
            if (!authorized || sessionAccessId == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String contentKey = trim(request.getParameter("contentKey"));
            if (isStoryKey(contentKey)) {
                accessService.recordEvent(appReq.getDataSession(), sessionAccessId.intValue(),
                        NathanAccessEvent.TYPE_STORY_OPEN, contentKey);
            }
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        if (!appReq.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("createInvitation".equals(action)) {
            Date expiresAt = parseExpiration(request.getParameter("expiresAt"));
            CreatedAccess created = accessService.createInvitation(appReq.getDataSession(),
                    appReq.getWebUser().getWebUserId(), clip(request.getParameter("label"), 200),
                    normalizeOptionalEmail(request.getParameter("email")), clip(request.getParameter("notes"), 2000),
                    expiresAt);
            String link = buildAccessUrl(request, appReq.getDataSession(), created.getRawToken());
            setFlash(webSession, "Invitation created. This exact link is shown only now.", link);
        } else if ("approveAccess".equals(action)) {
            approveAccess(appReq, parseId(request.getParameter("accessId")));
        } else if ("disableAccess".equals(action)) {
            accessService.setEnabled(appReq.getDataSession(), parseId(request.getParameter("accessId")), false);
            setFlash(webSession, "Access disabled.", null);
        } else if ("enableAccess".equals(action)) {
            accessService.setEnabled(appReq.getDataSession(), parseId(request.getParameter("accessId")), true);
            setFlash(webSession, "Access enabled.", null);
        }
        redirectToPage(request, response);
    }

    private void sendAccessRequestNotification(Session session, String requesterEmail) {
        try {
            String body = "<p>" + WebEscaper.escapeHtml(requesterEmail)
                    + " requested access to Nathan's extended Dandelion Daily page.</p>"
                    + "<p>Sign in to Dandelion Daily and visit the Nathan page to review the request.</p>";
            new MailManager(session).sendEmail("Dandelion: Nathan page access requested", body,
                    ACCESS_REQUEST_NOTIFICATION_EMAIL);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to email Nathan access request notification", e);
        }
    }

    private void approveAccess(AppReq appReq, int accessId) {
        CreatedAccess created = accessService.approveRequest(appReq.getDataSession(), accessId,
                appReq.getWebUser().getWebUserId());
        if (created == null) {
            setFlash(appReq.getWebSession(), "The request is no longer pending.", null);
            return;
        }
        String link = buildAccessUrl(appReq.getRequest(), appReq.getDataSession(), created.getRawToken());
        String email = created.getAccess().getEmail();
        boolean emailEnabled = TrackerKeysManager.getApplicationKeyValueBoolean(
                TrackerKeysManager.KEY_SYSTEM_EMAIL_ENABLE, false, appReq.getDataSession());
        String message = "Request approved. Copy the generated link manually.";
        if (emailEnabled && email != null) {
            try {
                String body = "<p>Nathan approved your request to view his extended page.</p>"
                        + "<p><a href=\"" + WebEscaper.escapeHtml(link) + "\">Open Nathan's page</a></p>";
                new MailManager(appReq.getDataSession()).sendEmail("Access to Nathan's page", body, email);
                message = "Request approved and the access link was emailed. The link is also shown below.";
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to email approved Nathan access", e);
                message = "Request approved, but email delivery failed. Copy the generated link manually.";
            }
        }
        setFlash(appReq.getWebSession(), message, link);
    }

    private void renderPage(AppReq appReq, boolean authorized, Integer sessionAccessId) {
        PrintWriter out = appReq.getOut();
        String csrf = getOrCreateCsrf(appReq.getWebSession());
        out.println("<main id='about' class='nathan-page'>");
        renderFlash(out, appReq.getWebSession());
        out.println("<section class='nathan-content'>" + content.getPublicHtml() + "</section>");
        if (!authorized) {
            renderRequestForm(out, csrf);
        } else {
            out.println("<div class='nathan-divider'></div>");
            out.println("<section id='work' class='nathan-content nathan-anchor'>"
                    + content.getExtendedIntroductionHtml() + "</section>");
            renderStories(out, csrf, sessionAccessId != null);
            out.println("<section id='resume' class='nathan-content nathan-resume nathan-anchor'>");
            out.println("<h2>Résumé</h2>");
            out.println("<p><a class='nathan-download' href='" + appReq.getRequest().getContextPath()
                    + "/nathan/resume'>Download traditional résumé (PDF)</a></p>");
            out.println(content.getExtendedConclusionHtml());
            out.println("</section>");
        }
        if (appReq.isAdmin()) {
            renderAdministration(appReq, csrf);
        }
        printEventScript(out, csrf, sessionAccessId != null);
        out.println("</main>");
    }

    private void printNathanHead(AppReq appReq) {
        appReq.getResponse().setContentType("text/html;charset=UTF-8");
        PrintWriter out = appReq.getOut();
        out.println("<!doctype html><html lang='en'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<title>Nathan Bunker | Work, Approach, and Experience</title>");
        printStyles(out);
        out.println("</head><body>");
    }

    private void renderNathanHeader(PrintWriter out, boolean authorized) {
        out.println("<header class='nathan-header'><div class='nathan-header-inner'>");
        out.println("<a class='nathan-brand' href='#about' aria-label='Nathan Bunker, back to top'>"
                + "<span class='nathan-monogram' aria-hidden='true'>NB</span>"
                + "<span><strong>Nathan Bunker</strong><small>Work, approach &amp; experience</small></span></a>");
        if (authorized) {
            out.println("<nav class='nathan-nav' aria-label='Page sections'>"
                    + "<a href='#about'>About</a><a href='#work'>Work</a>"
                    + "<a href='#career-lessons'>Career lessons</a><a href='#resume'>Résumé</a></nav>");
        }
        out.println("</div></header>");
    }

    private static void printNathanFoot(PrintWriter out) {
        out.println("</body></html>");
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

    private void renderRequestForm(PrintWriter out, String csrf) {
        out.println("<section class='nathan-access-request'>");
        out.println("<h2>Request access</h2>");
        out.println("<form method='POST' action='nathan'>");
        hidden(out, "action", "requestAccess");
        hidden(out, "csrf", csrf);
        out.println("<label for='nathanEmail'>Email address</label>");
        out.println("<div class='nathan-form-row'><input id='nathanEmail' name='email' type='email' maxlength='254' required>"
                + "<button type='submit'>Request access</button></div>");
        out.println("</form></section>");
    }

    private void renderStories(PrintWriter out, String csrf, boolean trackEvents) {
        out.println("<section id='career-lessons' class='nathan-stories nathan-anchor'><h2>Career lessons</h2>");
        out.println("<p>These stories explain more than a résumé can. Open any story to continue reading.</p>");
        for (CareerStory story : content.getCareerStories()) {
            out.println("<details class='nathan-story' data-story-key='" + story.getKey() + "'>");
            out.println("<summary><span>" + WebEscaper.escapeHtml(story.getTitle()) + "</span></summary>");
            out.println("<div class='nathan-story-summary'>" + story.getSummaryHtml() + "</div>");
            out.println("<div class='nathan-story-full'>" + story.getNarrativeHtml() + "</div>");
            out.println("</details>");
        }
        out.println("</section>");
    }

    private void renderAdministration(AppReq appReq, String csrf) {
        PrintWriter out = appReq.getOut();
        Session session = appReq.getDataSession();
        out.println("<section class='nathan-admin'><h2>Access administration</h2>");
        out.println("<h3>Create invitation</h3><form method='POST' action='nathan' class='nathan-admin-form'>");
        hidden(out, "action", "createInvitation");
        hidden(out, "csrf", csrf);
        out.println("<label>Label<input name='label' maxlength='200'></label>");
        out.println("<label>Email (optional)<input name='email' type='email' maxlength='254'></label>");
        out.println("<label>Expiration (optional)<input name='expiresAt' type='date'></label>");
        out.println("<label class='nathan-wide'>Notes<textarea name='notes' maxlength='2000' rows='3'></textarea></label>");
        out.println("<div class='nathan-wide'><button type='submit'>Create Link</button></div></form>");
        renderAccessTable(out, "Pending requests", accessService.listByStatus(session, NathanAccess.STATUS_PENDING),
                csrf, session, NathanAccess.STATUS_PENDING);
        renderAccessTable(out, "Active access", accessService.listByStatus(session, NathanAccess.STATUS_ACTIVE),
                csrf, session, NathanAccess.STATUS_ACTIVE);
        renderAccessTable(out, "Disabled access", accessService.listByStatus(session, NathanAccess.STATUS_DISABLED),
                csrf, session, NathanAccess.STATUS_DISABLED);
        out.println("</section>");
    }

    private void renderAccessTable(PrintWriter out, String title, List<NathanAccess> records, String csrf,
            Session session, String status) {
        out.println("<h3>" + title + "</h3>");
        if (records.isEmpty()) {
            out.println("<p class='nathan-muted'>None.</p>");
            return;
        }
        out.println("<div class='nathan-table-wrap'><table><thead><tr><th>Label / email</th><th>Type</th>"
                + "<th>Created / requested</th><th>Use</th><th>Activity</th><th>Action</th></tr></thead><tbody>");
        for (NathanAccess access : records) {
            out.println("<tr><td><strong>" + h(access.getLabel()) + "</strong><br>" + h(access.getEmail())
                    + (access.getNotes() == null ? "" : "<div class='nathan-muted'>" + h(access.getNotes()) + "</div>")
                    + "</td><td>" + h(access.getAccessType()) + "</td><td>" + format(access.getCreatedAt())
                    + (access.getRequestedAt() == null ? "" : "<br>Requested " + format(access.getRequestedAt()))
                    + (access.getExpiresAt() == null ? "" : "<br>Expires " + format(access.getExpiresAt()))
                    + "</td><td>First: " + format(access.getFirstUsedAt()) + "<br>Last: "
                    + format(access.getLastUsedAt()) + "<br>Views: " + access.getUseCount() + "</td><td>"
                    + renderActivity(session, access.getNathanAccessId()) + "</td><td>");
            if (NathanAccess.STATUS_PENDING.equals(status)) {
                actionForm(out, csrf, access.getNathanAccessId(), "approveAccess", "Approve");
                actionForm(out, csrf, access.getNathanAccessId(), "disableAccess", "Disable/Reject");
            } else if (NathanAccess.STATUS_ACTIVE.equals(status)) {
                actionForm(out, csrf, access.getNathanAccessId(), "disableAccess", "Disable");
            } else {
                actionForm(out, csrf, access.getNathanAccessId(), "enableAccess", "Enable");
            }
            out.println("</td></tr>");
        }
        out.println("</tbody></table></div>");
    }

    private String renderActivity(Session session, int accessId) {
        Set<String> labels = new LinkedHashSet<String>();
        for (NathanAccessEvent event : accessService.listActivity(session, accessId)) {
            if (NathanAccessEvent.TYPE_RESUME_DOWNLOAD.equals(event.getEventType())) {
                labels.add("Résumé viewed");
            } else if (NathanAccessEvent.TYPE_STORY_OPEN.equals(event.getEventType())) {
                labels.add("Opened “" + storyTitle(event.getContentKey()) + "”");
            }
        }
        if (labels.isEmpty()) {
            return "<span class='nathan-muted'>No content activity</span>";
        }
        StringBuilder html = new StringBuilder();
        for (String label : labels) {
            if (html.length() > 0) {
                html.append("<br>");
            }
            html.append(h(label));
        }
        return html.toString();
    }

    private void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println(":root{color-scheme:light;scroll-behavior:smooth}*{box-sizing:border-box}body{margin:0;background:#fffdf8;color:#26332a;font-family:Georgia,'Times New Roman',serif}");
        out.println(".nathan-header{position:sticky;top:0;z-index:20;border-bottom:1px solid #c8d2c6;background:rgba(255,253,248,.97);box-shadow:0 2px 12px rgba(38,51,42,.06)}.nathan-header-inner{max-width:1080px;min-height:76px;margin:0 auto;padding:10px 22px;display:flex;align-items:center;justify-content:space-between;gap:28px}.nathan-brand{display:flex;align-items:center;gap:11px;color:#24362a;text-decoration:none;white-space:nowrap}.nathan-brand strong{display:block;font-size:17px;line-height:1.2}.nathan-brand small{display:block;margin-top:3px;color:#68756b;font-family:Arial,sans-serif;font-size:11px;letter-spacing:0;text-transform:uppercase}.nathan-monogram{width:42px;height:42px;display:grid;place-items:center;border:1px solid #49654a;background:#304d37;color:#fff;font-size:15px;font-weight:bold}.nathan-nav{display:flex;align-items:center;gap:4px;font-family:Arial,sans-serif;font-size:14px}.nathan-nav a{padding:9px 10px;color:#304d37;text-decoration:none;border-bottom:2px solid transparent}.nathan-nav a:hover,.nathan-nav a:focus{border-color:#789078;color:#18261d;outline:none}");
        out.println(".nathan-page{max-width:980px;margin:0 auto;padding:34px 22px 60px;line-height:1.65}.nathan-anchor{scroll-margin-top:100px}");
        out.println(".nathan-page h1{font-family:Georgia,serif;font-size:42px;margin:8px 0 4px;color:#24362a;letter-spacing:0}");
        out.println(".nathan-page h2{font-family:Georgia,serif;font-size:27px;margin:34px 0 8px;color:#304d37;letter-spacing:0}");
        out.println(".nathan-page h3{font-size:18px;margin:24px 0 6px;color:#3f5b45;letter-spacing:0}");
        out.println(".nathan-content{max-width:76ch}.nathan-content blockquote{margin:18px 0;padding:2px 18px;border-left:4px solid #91a88f;color:#45564a}");
        out.println(".nathan-divider{height:1px;background:#cfd8ce;margin:38px 0}.nathan-access-request,.nathan-admin{margin-top:32px;padding:20px;border:1px solid #b9c8b8;background:#f3f7f1;border-radius:6px}");
        out.println(".nathan-form-row{display:flex;gap:8px;max-width:620px}.nathan-form-row input{flex:1}.nathan-page input,.nathan-page textarea{box-sizing:border-box;padding:8px;border:1px solid #9eac9d;background:#fff;font:inherit}.nathan-page button,.nathan-download{display:inline-block;padding:8px 13px;border:1px solid #49654a;background:#49654a;color:#fff;text-decoration:none;cursor:pointer;font:inherit;border-radius:3px}");
        out.println(".nathan-stories{margin-top:34px}.nathan-story{margin:14px 0;border:1px solid #c8d2c6;background:#fff;border-radius:5px}.nathan-story summary{cursor:pointer;padding:14px 17px;font-weight:bold;font-size:18px;color:#304d37}.nathan-story-summary,.nathan-story-full{padding:0 18px 12px;max-width:76ch}.nathan-story-full{padding-top:8px;border-top:1px solid #e0e6df}.nathan-story:not([open]) .nathan-story-full{display:none}");
        out.println(".nathan-flash{padding:12px 15px;margin-bottom:18px;background:#e7f1e5;border-left:4px solid #4f704f}.nathan-generated-link{display:block;margin-top:8px;padding:10px;background:#fff;overflow-wrap:anywhere;font-weight:bold}.nathan-admin{margin-top:52px}.nathan-admin-form{display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px}.nathan-admin-form label{display:flex;flex-direction:column}.nathan-wide{grid-column:1/-1}.nathan-table-wrap{overflow-x:auto}.nathan-admin table{width:100%;border-collapse:collapse;font-size:13px;background:#fff}.nathan-admin th,.nathan-admin td{padding:8px;border:1px solid #ccd5ca;text-align:left;vertical-align:top}.nathan-admin form{margin:3px 0}.nathan-muted{color:#68756b;font-size:.92em}");
        out.println("@media(max-width:700px){.nathan-header-inner{min-height:auto;padding:9px 12px;display:block}.nathan-brand small{display:none}.nathan-monogram{width:34px;height:34px}.nathan-nav{margin-top:8px;overflow-x:auto;white-space:nowrap;border-top:1px solid #e0e6df}.nathan-nav a{padding:9px 8px}.nathan-page{padding:24px 12px 50px}.nathan-anchor{scroll-margin-top:125px}.nathan-page h1{font-size:34px}.nathan-form-row{flex-direction:column}.nathan-admin-form{grid-template-columns:1fr}.nathan-wide{grid-column:auto}}@media(prefers-reduced-motion:reduce){:root{scroll-behavior:auto}}");
        out.println("</style>");
    }

    private void printEventScript(PrintWriter out, String csrf, boolean trackEvents) {
        if (!trackEvents) {
            return;
        }
        out.println("<script>(function(){var seen={};document.querySelectorAll('.nathan-story').forEach(function(el){"
                + "el.addEventListener('toggle',function(){var key=el.getAttribute('data-story-key');if(!el.open||seen[key])return;seen[key]=true;"
                + "var body=new URLSearchParams();body.append('action','recordEvent');body.append('csrf','"
                + WebEscaper.escapeHtml(csrf) + "');body.append('contentKey',key);"
                + "fetch('nathan',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:body.toString()}).catch(function(){});"
                + "});});})();</script>");
    }

    private static void renderFlash(PrintWriter out, HttpSession session) {
        String message = (String) session.getAttribute(SESSION_FLASH_MESSAGE);
        String link = (String) session.getAttribute(SESSION_FLASH_LINK);
        session.removeAttribute(SESSION_FLASH_MESSAGE);
        session.removeAttribute(SESSION_FLASH_LINK);
        if (message != null) {
            out.println("<div class='nathan-flash'>" + h(message));
            if (link != null) {
                out.println("<a class='nathan-generated-link' href='" + h(link) + "'>" + h(link) + "</a>");
            }
            out.println("</div>");
        }
    }

    private static void actionForm(PrintWriter out, String csrf, int accessId, String action, String label) {
        out.println("<form method='POST' action='nathan'>");
        hidden(out, "action", action);
        hidden(out, "csrf", csrf);
        hidden(out, "accessId", String.valueOf(accessId));
        out.println("<button type='submit'>" + h(label) + "</button></form>");
    }

    private static void hidden(PrintWriter out, String name, String value) {
        out.println("<input type='hidden' name='" + h(name) + "' value='" + h(value) + "'>");
    }

    private static String getOrCreateCsrf(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_CSRF);
        if (token == null) {
            byte[] bytes = new byte[24];
            SECURE_RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_CSRF, token);
        }
        return token;
    }

    private static boolean csrfValid(HttpSession session, String supplied) {
        Object expected = session.getAttribute(SESSION_CSRF);
        return expected instanceof String && expected.equals(supplied);
    }

    private static void setFlash(HttpSession session, String message, String link) {
        session.setAttribute(SESSION_FLASH_MESSAGE, message);
        if (link != null) {
            session.setAttribute(SESSION_FLASH_LINK, link);
        }
    }

    private static void redirectToPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/nathan");
    }

    private static Integer getSessionAccessId(HttpSession session) {
        Object value = session.getAttribute(SESSION_ACCESS_ID);
        return value instanceof Integer ? (Integer) value : null;
    }

    private static boolean isStoryKey(String key) {
        return "serve-client".equals(key) || "authority-without-power".equals(key)
                || "build-more-ideas".equals(key);
    }

    private static String storyTitle(String key) {
        if ("serve-client".equals(key)) return "Serve the Client. Build the Field.";
        if ("authority-without-power".equals(key)) return "Earn Authority Without Power";
        if ("build-more-ideas".equals(key)) return "Build More Ideas. Own Them Less.";
        return key == null ? "Story" : key;
    }

    private static String buildAccessUrl(HttpServletRequest request, Session session, String rawToken) {
        String external = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, "", session);
        String base;
        if (external != null && external.trim().length() > 0) {
            base = external.trim();
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            base += "/nathan";
        } else {
            base = request.getRequestURL().toString();
        }
        try {
            return base + "?access=" + URLEncoder.encode(rawToken, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Date parseExpiration(String value) {
        String normalized = trim(value);
        if (normalized.length() == 0) return null;
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(normalized);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            return calendar.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid expiration date");
        }
    }

    private static int parseId(String value) {
        try { return Integer.parseInt(trim(value)); } catch (NumberFormatException e) { return 0; }
    }

    private static String normalizeEmail(String value) {
        return trim(value).toLowerCase();
    }

    private static String normalizeOptionalEmail(String value) {
        String email = normalizeEmail(value);
        if (email.length() == 0) return null;
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new IllegalArgumentException("Invalid email address");
        return email;
    }

    private static String clip(String value, int maxLength) {
        String normalized = trim(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String format(Date value) {
        return value == null ? "—" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(value);
    }

    private static String h(String value) {
        return WebEscaper.escapeHtml(value);
    }
}