package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.doa.WeUserDependencyDao;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.WeUserDependency;
import org.openimmunizationsoftware.pt.model.WebUser;

public class DependentAccountsServlet extends ClientServlet {

    private static final long serialVersionUID = 1L;

    private static final String ACTION_REQUEST = "Request";
    private static final String ACTION_ACCEPT = "Accept";
    private static final String ACTION_REJECT = "Reject";
    private static final String ACTION_REMOVE = "Remove";

    private static final String PARAM_INVITE_EMAIL = "inviteEmail";
    private static final String PARAM_INVITE_TOKEN = "inviteToken";
    private static final String PARAM_DEPENDENCY_ID = "dependencyId";

    /** Invite links are valid for 72 hours. */
    private static final int INVITE_HOURS_VALID = 72;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut()) {
                forwardToHome(request, response);
                return;
            }

            WebUser webUser = appReq.getWebUser();
            Session dataSession = appReq.getDataSession();
            String action = appReq.getAction();
            PrintWriter out = appReq.getOut();

            // Resolve a pending invite token passed in the URL (GET or POST).
            String inviteToken = request.getParameter(PARAM_INVITE_TOKEN);
            WeUserDependency pendingInviteForMe = null;
            if (inviteToken != null && !inviteToken.trim().isEmpty()) {
                pendingInviteForMe = resolveInviteToken(inviteToken.trim(), webUser, dataSession, appReq);
            }

            // Handle POST actions.
            if (action != null) {
                if (action.equals(ACTION_REQUEST)) {
                    handleRequest(request, appReq, webUser, dataSession);
                } else if (action.equals(ACTION_ACCEPT)) {
                    pendingInviteForMe = handleAccept(request, appReq, webUser, dataSession);
                } else if (action.equals(ACTION_REJECT)) {
                    pendingInviteForMe = handleReject(request, appReq, webUser, dataSession);
                } else if (action.equals(ACTION_REMOVE)) {
                    handleRemove(request, appReq, webUser, dataSession);
                }
            }

            appReq.setTitle("Dependent Accounts");
            printHtmlHead(appReq);
            printDandelionLocation(out, "Setup / Dependent Accounts");

            // If an invite is pending for the current user, show it prominently first.
            if (pendingInviteForMe != null && "invited".equals(pendingInviteForMe.getDependencyStatus())) {
                printPendingInviteSection(out, pendingInviteForMe, inviteToken);
            }

            // Section: My dependents (I am the guardian).
            printMyDependentsSection(out, webUser, dataSession);

            // Section: Accounts I am a dependent of.
            printGuardiansOfMeSection(out, webUser, dataSession);

            // Section: Request a new dependent.
            printRequestForm(out);

            out.println("<br/>");
            out.println("<p><a href=\"HomeServlet\">Back to Home</a></p>");
            out.println("</div>");
            printHtmlFoot(appReq);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            appReq.close();
        }
    }

    // -------------------------------------------------------------------------
    // Action handlers
    // -------------------------------------------------------------------------

    private void handleRequest(HttpServletRequest request, AppReq appReq,
            WebUser webUser, Session dataSession) {
        String email = n(request.getParameter(PARAM_INVITE_EMAIL)).trim();
        if (email.isEmpty()) {
            appReq.setMessageProblem("Please enter an email address for the dependent account.");
            return;
        }

        // Look up the target user.
        Query q = dataSession.createQuery(
                "from WebUser where lower(emailAddress) = :email and registrationStatus = 'ACTIVE'");
        q.setString("email", email.toLowerCase());
        q.setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<WebUser> found = q.list();

        if (found.isEmpty()) {
            appReq.setMessageProblem(
                    "No active account found for that email address. "
                            + "The person must register before they can be added as a dependent.");
            return;
        }

        WebUser dependentUser = found.get(0);

        if (dependentUser.getWebUserId() == webUser.getWebUserId()) {
            appReq.setMessageProblem("You cannot add yourself as a dependent account.");
            return;
        }

        // Check for an existing non-ended relationship.
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        Query existQ = dataSession.createQuery(
                "from WeUserDependency wud "
                        + "where wud.guardianWebUser.webUserId = :gid "
                        + "  and wud.dependentWebUser.webUserId = :did "
                        + "  and wud.dependencyStatus in ('invited', 'active')");
        existQ.setInteger("gid", webUser.getWebUserId());
        existQ.setInteger("did", dependentUser.getWebUserId());
        existQ.setMaxResults(1);
        if (existQ.uniqueResult() != null) {
            appReq.setMessageProblem(
                    "A pending or active dependency relationship already exists for that account.");
            return;
        }

        // Create the invite record.
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);
        Date expiry = new Date(System.currentTimeMillis()
                + (INVITE_HOURS_VALID * 60L * 60L * 1000L));

        WeUserDependency dep = new WeUserDependency();
        dep.setGuardianWebUser(webUser);
        dep.setDependentWebUser(dependentUser);
        dep.setInviteEmail(email);
        dep.setInviteTokenHash(tokenHash);
        dep.setInviteExpiry(expiry);
        dep.setDependencyStatus("invited");
        dep.setCreatedDate(new Date());

        Transaction trans = dataSession.beginTransaction();
        dao.save(dep);
        trans.commit();

        String inviteUrl = buildInviteUrl(request, dataSession, rawToken);

        // Send email to dependent user asking them to approve.
        String guardianName = webUser.getEmailAddress();
        String body = "<p>" + escapeHtml(guardianName) + " would like to add your account as a "
                + "dependent account in Dandelion.</p>"
                + "<p><a href=\"" + inviteUrl + "\">Review and respond to this request</a></p>"
                + "<p>This link expires in " + INVITE_HOURS_VALID + " hours. "
                + "If you did not expect this invitation, you can ignore it.</p>";

        boolean sent = false;
        try {
            MailManager mail = new MailManager(dataSession);
            mail.sendEmail("Dandelion: Dependent Account Request", body,
                    dependentUser.getEmailAddress());
            sent = true;
        } catch (Exception e) {
            // Email sending failed — fall through to show link on page.
        }

        if (!sent) {
            appReq.setMessageConfirmation(
                    "Invitation created. Email could not be sent automatically — "
                            + "share this link with " + escapeHtml(email) + ": "
                            + "<a href=\"" + inviteUrl + "\">" + escapeHtml(inviteUrl) + "</a>");
        } else {
            appReq.setMessageConfirmation(
                    "Invitation sent to " + escapeHtml(email)
                            + ". They will need to follow the link in the email to accept.");
        }
    }

    private WeUserDependency handleAccept(HttpServletRequest request, AppReq appReq,
            WebUser webUser, Session dataSession) {
        WeUserDependency dep = loadDependencyForDependent(request, webUser, dataSession);
        if (dep == null) {
            appReq.setMessageProblem("Invitation not found or already resolved.");
            return null;
        }
        Transaction trans = dataSession.beginTransaction();
        dep.setDependencyStatus("active");
        dep.setAcceptedDate(new Date());
        dep.setInviteTokenHash(null);
        dep.setInviteExpiry(null);
        dataSession.update(dep);
        trans.commit();
        appReq.setMessageConfirmation("You are now a dependent account of "
                + escapeHtml(dep.getGuardianWebUser().getEmailAddress()) + ".");
        return null;
    }

    private WeUserDependency handleReject(HttpServletRequest request, AppReq appReq,
            WebUser webUser, Session dataSession) {
        WeUserDependency dep = loadDependencyForDependent(request, webUser, dataSession);
        if (dep == null) {
            appReq.setMessageProblem("Invitation not found or already resolved.");
            return null;
        }
        Transaction trans = dataSession.beginTransaction();
        dep.setDependencyStatus("rejected");
        dep.setEndedDate(new Date());
        dep.setInviteTokenHash(null);
        dep.setInviteExpiry(null);
        dataSession.update(dep);
        trans.commit();
        appReq.setMessageConfirmation("Invitation rejected.");
        return null;
    }

    private void handleRemove(HttpServletRequest request, AppReq appReq,
            WebUser webUser, Session dataSession) {
        String idParam = request.getParameter(PARAM_DEPENDENCY_ID);
        if (idParam == null || idParam.trim().isEmpty()) {
            return;
        }
        int dependencyId;
        try {
            dependencyId = Integer.parseInt(idParam.trim());
        } catch (NumberFormatException e) {
            return;
        }
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        WeUserDependency dep = dao.getById(dependencyId);
        if (dep == null) {
            return;
        }
        // Only the guardian of this record can remove it.
        if (dep.getGuardianWebUser().getWebUserId() != webUser.getWebUserId()) {
            appReq.setMessageProblem("You do not have permission to remove that dependency.");
            return;
        }
        Transaction trans = dataSession.beginTransaction();
        dep.setDependencyStatus("ended");
        dep.setEndedDate(new Date());
        dataSession.update(dep);
        trans.commit();
        appReq.setMessageConfirmation("Dependent account relationship removed.");
    }

    // -------------------------------------------------------------------------
    // Invite token helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the raw invite token: finds the matching {@link WeUserDependency},
     * validates expiry, and verifies that the currently logged-in user is the
     * intended dependent. Returns the dependency if it is still pending, or
     * null with an error message set on {@code appReq}.
     */
    private WeUserDependency resolveInviteToken(String rawToken, WebUser webUser,
            Session dataSession, AppReq appReq) {
        String tokenHash = hashToken(rawToken);
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        WeUserDependency dep = dao.findByInviteTokenHash(tokenHash);
        if (dep == null) {
            appReq.setMessageProblem("Invitation link is invalid.");
            return null;
        }
        if (!"invited".equals(dep.getDependencyStatus())) {
            appReq.setMessageProblem("This invitation has already been resolved.");
            return null;
        }
        if (dep.getInviteExpiry() != null && dep.getInviteExpiry().before(new Date())) {
            appReq.setMessageProblem("This invitation link has expired.");
            return null;
        }
        // Make sure the logged-in user is the intended dependent.
        if (dep.getDependentWebUser().getWebUserId() != webUser.getWebUserId()) {
            appReq.setMessageProblem(
                    "This invitation was sent to a different account. "
                            + "Please log in with the account that received the invitation email.");
            return null;
        }
        return dep;
    }

    private WeUserDependency loadDependencyForDependent(HttpServletRequest request,
            WebUser webUser, Session dataSession) {
        String idParam = request.getParameter(PARAM_DEPENDENCY_ID);
        if (idParam == null || idParam.trim().isEmpty()) {
            return null;
        }
        int dependencyId;
        try {
            dependencyId = Integer.parseInt(idParam.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        WeUserDependency dep = dao.getById(dependencyId);
        if (dep == null) {
            return null;
        }
        if (dep.getDependentWebUser().getWebUserId() != webUser.getWebUserId()) {
            return null;
        }
        if (!"invited".equals(dep.getDependencyStatus())) {
            return null;
        }
        return dep;
    }

    // -------------------------------------------------------------------------
    // Page rendering
    // -------------------------------------------------------------------------

    private void printPendingInviteSection(PrintWriter out, WeUserDependency dep,
            String rawToken) {
        String guardianEmail = escapeHtml(dep.getGuardianWebUser().getEmailAddress());
        String tokenParam = rawToken == null ? "" : escapeHtml(rawToken);

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Pending Invitation</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" colspan=\"2\">");
        out.println("      <strong>" + guardianEmail + "</strong> would like to add your account "
                + "as a dependent account.");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\">");
        out.println("      <form action=\"DependentAccountsServlet\" method=\"POST\">");
        out.println("        <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                + dep.getDependencyId() + "\">");
        if (!tokenParam.isEmpty()) {
            out.println("        <input type=\"hidden\" name=\"" + PARAM_INVITE_TOKEN + "\" value=\""
                    + tokenParam + "\">");
        }
        out.println("        <input type=\"submit\" name=\"action\" value=\"" + ACTION_ACCEPT + "\">");
        out.println("      </form>");
        out.println("    </td>");
        out.println("    <td class=\"boxed\">");
        out.println("      <form action=\"DependentAccountsServlet\" method=\"POST\">");
        out.println("        <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                + dep.getDependencyId() + "\">");
        out.println("        <input type=\"submit\" name=\"action\" value=\"" + ACTION_REJECT + "\">");
        out.println("      </form>");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("</table>");
        out.println("<br/>");
    }

    private void printMyDependentsSection(PrintWriter out, WebUser webUser, Session dataSession) {
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        List<WeUserDependency> deps = dao.listByGuardianWebUserId(webUser.getWebUserId());

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"3\">My Dependent Accounts</th>");
        out.println("  </tr>");

        boolean any = false;
        for (WeUserDependency dep : deps) {
            String status = dep.getDependencyStatus();
            if ("ended".equals(status) || "expired".equals(status)) {
                continue;
            }
            any = true;
            String who = dep.getDependentWebUser() != null
                    ? escapeHtml(dep.getDependentWebUser().getEmailAddress())
                    : escapeHtml(dep.getInviteEmail()) + " (pending)";
            String statusLabel = "invited".equals(status) ? "Pending" : capitalize(status);

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">" + who + "</td>");
            out.println("    <td class=\"boxed\">" + statusLabel + "</td>");
            out.println("    <td class=\"boxed\">");
            if ("active".equals(status)) {
                out.println("      <a href=\"ScheduleSchoolServlet?dependencyId=" + dep.getDependencyId()
                        + "\" class=\"button\">Schedule School</a>");
                out.println("      <a href=\"StudentStoreServlet?dependencyId=" + dep.getDependencyId()
                        + "\" class=\"button\">Manage Store</a>");
            }
            if ("active".equals(status) || "invited".equals(status)) {
                out.println("      <form action=\"DependentAccountsServlet\" method=\"POST\""
                        + " style=\"display:inline\">");
                out.println("        <input type=\"hidden\" name=\"" + PARAM_DEPENDENCY_ID + "\" value=\""
                        + dep.getDependencyId() + "\">");
                out.println("        <input type=\"submit\" name=\"action\" value=\"" + ACTION_REMOVE
                        + "\" onclick=\"return confirm('Remove this dependency?');\">");
                out.println("      </form>");
            }
            out.println("    </td>");
            out.println("  </tr>");
        }

        if (!any) {
            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\" colspan=\"3\">No dependent accounts yet.</td>");
            out.println("  </tr>");
        }

        out.println("</table>");
        out.println("<br/>");
    }

    private void printGuardiansOfMeSection(PrintWriter out, WebUser webUser, Session dataSession) {
        WeUserDependencyDao dao = new WeUserDependencyDao(dataSession);
        List<WeUserDependency> deps = dao.listByDependentWebUserId(webUser.getWebUserId());

        // Filter to relevant statuses.
        boolean any = false;
        for (WeUserDependency dep : deps) {
            String s = dep.getDependencyStatus();
            if ("ended".equals(s) || "expired".equals(s) || "rejected".equals(s)) {
                continue;
            }
            any = true;
        }

        if (!any) {
            return; // Don't render section if empty.
        }

        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">I Am a Dependent Of</th>");
        out.println("  </tr>");

        for (WeUserDependency dep : deps) {
            String s = dep.getDependencyStatus();
            if ("ended".equals(s) || "expired".equals(s) || "rejected".equals(s)) {
                continue;
            }
            String guardianEmail = escapeHtml(dep.getGuardianWebUser().getEmailAddress());
            String statusLabel = "invited".equals(s) ? "Pending your acceptance" : capitalize(s);

            out.println("  <tr class=\"boxed\">");
            out.println("    <td class=\"boxed\">" + guardianEmail + "</td>");
            out.println("    <td class=\"boxed\">" + statusLabel + "</td>");
            out.println("  </tr>");
        }

        out.println("</table>");
        out.println("<br/>");
    }

    private void printRequestForm(PrintWriter out) {
        out.println("<table class=\"boxed\">");
        out.println("  <tr class=\"boxed\">");
        out.println("    <th class=\"title\" colspan=\"2\">Request a Dependent Account</th>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <td class=\"boxed\" colspan=\"2\">");
        out.println("      Enter the email address of the account you want to add as a dependent. "
                + "They will receive an email asking them to approve.");
        out.println("    </td>");
        out.println("  </tr>");
        out.println("  <tr class=\"boxed\">");
        out.println("    <form action=\"DependentAccountsServlet\" method=\"POST\">");
        out.println("    <td class=\"boxed\">Email address</td>");
        out.println("    <td class=\"boxed\">");
        out.println("      <input type=\"text\" name=\"" + PARAM_INVITE_EMAIL + "\" size=\"40\">");
        out.println("      <input type=\"submit\" name=\"action\" value=\"" + ACTION_REQUEST + "\">");
        out.println("    </td>");
        out.println("    </form>");
        out.println("  </tr>");
        out.println("</table>");
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String buildInviteUrl(HttpServletRequest request, Session dataSession, String rawToken) {
        String externalUrl = TrackerKeysManager.getApplicationKeyValue(
                TrackerKeysManager.KEY_SYSTEM_EXTERNAL_URL, "", dataSession);
        String base;
        if (externalUrl == null || externalUrl.trim().isEmpty()) {
            String requestUrl = request.getRequestURL().toString();
            String servletPath = request.getServletPath();
            int idx = requestUrl.indexOf(servletPath);
            String rootUrl = idx > 0 ? requestUrl.substring(0, idx + 1) : requestUrl;
            base = rootUrl + "DependentAccountsServlet";
        } else {
            String norm = externalUrl.trim();
            if (!norm.endsWith("/")) {
                norm += "/";
            }
            base = norm + "DependentAccountsServlet";
        }
        try {
            return base + "?" + PARAM_INVITE_TOKEN + "=" + URLEncoder.encode(rawToken, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return base + "?" + PARAM_INVITE_TOKEN + "=" + rawToken;
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

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // -------------------------------------------------------------------------
    // Servlet boilerplate
    // -------------------------------------------------------------------------

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
}
