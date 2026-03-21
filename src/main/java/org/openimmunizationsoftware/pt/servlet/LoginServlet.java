/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.List;
import java.util.TimeZone;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.manager.TimeTracker;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * 
 * @author nathan
 */
public class LoginServlet extends ClientServlet {

  private static final String ACTION_LOGIN = "Login";
  private static final String ACTION_LOGOUT = "Logout";
  private static final String ACTION_SEND_MAGIC_LINK = "Send Magic Link";
  private static final String ACTION_MAGIC_LOGIN = "MagicLogin";

  private static final String PARAM_MAGIC_EMAIL = "magicEmail";
  private static final String PARAM_MAGIC_USER_ID = "magicUserId";
  private static final String PARAM_MAGIC_TOKEN = "magicToken";

  private static final int MAGIC_LINK_MINUTES_VALID = 20;
  // Temporary testing mode: show generated magic link on page instead of sending
  // email.
  private static final boolean TEMP_SHOW_MAGIC_LINK_ON_PAGE = true;

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AppReq appReq = new AppReq(request, response);
    try {
      Session dataSession = appReq.getDataSession();
      String action = appReq.getAction();
      PrintWriter out = appReq.getOut();
      boolean loginSuccess = false;
      if (action != null) {
        if (action.equals(ACTION_LOGIN)) {
          loginSuccess = login(request, appReq, dataSession, loginSuccess);
        } else if (action.equals(ACTION_SEND_MAGIC_LINK)) {
          sendMagicLink(request, appReq, dataSession);
        } else if (action.equals(ACTION_MAGIC_LOGIN)) {
          loginSuccess = loginWithMagicLink(request, appReq, dataSession);
        } else if (action.equals(ACTION_LOGOUT)) {
          appReq.logout();
          response.sendRedirect("LoginServlet");
          return;
        }
      }
      if (!loginSuccess) {
        String username = request.getParameter("username");
        if (username == null) {
          username = "";
        }
        String password = request.getParameter("password");
        if (password == null) {
          password = "";
        }
        String magicEmail = request.getParameter(PARAM_MAGIC_EMAIL);
        if (magicEmail == null) {
          magicEmail = "";
        }
        String uiMode = request.getParameter("uiMode");
        if (uiMode == null || uiMode.equals("")) {
          uiMode = "desktop";
        }
        try {
          appReq.setTitle("Login");
          printHtmlHead(appReq);
          printLoginForm(out, username, password, magicEmail, uiMode);
          printHtmlFoot(appReq);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          appReq.close();
        }
      } else {
        WebUser webUser = appReq.getWebUser();
        if (webUser != null && !hasProvider(webUser)) {
          if (!webUser.isEmailVerified()) {
            Date now = new Date();
            webUser.setEmailVerified(true);
            if (webUser.getVerifiedDate() == null) {
              webUser.setVerifiedDate(now);
            }
            Transaction verifyTrans = dataSession.beginTransaction();
            dataSession.update(webUser);
            verifyTrans.commit();
          }
          response.sendRedirect("RegistrationServlet?status=setup");
          return;
        }
        String uiMode = request.getParameter("uiMode");
        String target = "HomeServlet";
        if (uiMode != null && uiMode.equals("mobile")) {
          target = "m/todo";
        }
        response.sendRedirect(target);
        return;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      appReq.close();
    }
  }

  private boolean login(HttpServletRequest request, AppReq appReq, Session dataSession,
      boolean loginSuccess) {
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String message = null;
    if (username != null && username.length() > 0 && password != null && password.length() > 0) {

      Query query = dataSession.createQuery("from WebUser where username = ? and password = ?");
      query.setParameter(0, username);
      query.setParameter(1, password);
      @SuppressWarnings("unchecked")
      List<WebUser> webUserList = query.list();

      if (webUserList.size() > 0) {
        WebUser webUser = hydrateWebUserForSession(dataSession, webUserList.get(0).getWebUserId());
        webUser.setLastLoginDate(new Date());
        Transaction trans = dataSession.beginTransaction();
        dataSession.update(webUser);
        trans.commit();

        String welcomeMessage = initializeUserSession(appReq, dataSession, webUser);
        loginSuccess = true;
        message = welcomeMessage;

      } else {
        message = "Invalid username or password";
      }
    } else {
      message = "Username and password required";
    }
    if (message != null) {
      appReq.setMessageProblem(message);
    }
    return loginSuccess;
  }

  private boolean loginWithMagicLink(HttpServletRequest request, AppReq appReq, Session dataSession) {
    String magicUserId = request.getParameter(PARAM_MAGIC_USER_ID);
    String magicToken = request.getParameter(PARAM_MAGIC_TOKEN);
    if (magicUserId == null || magicToken == null || magicUserId.equals("") || magicToken.equals("")) {
      appReq.setMessageProblem("Magic link is invalid or missing");
      return false;
    }

    int webUserId = 0;
    try {
      webUserId = Integer.parseInt(magicUserId);
    } catch (NumberFormatException nfe) {
      appReq.setMessageProblem("Magic link is invalid or missing");
      return false;
    }

    WebUser webUser = hydrateWebUserForSession(dataSession, webUserId);
    if (webUser == null || webUser.getMagicLinkTokenHash() == null || webUser.getMagicLinkExpiry() == null) {
      appReq.setMessageProblem("Magic link is invalid or expired");
      return false;
    }

    String tokenHash = hashToken(magicToken);
    Date now = new Date();
    if (!webUser.getMagicLinkTokenHash().equals(tokenHash) || webUser.getMagicLinkExpiry().before(now)) {
      appReq.setMessageProblem("Magic link is invalid or expired");
      return false;
    }

    webUser.setMagicLinkTokenHash(null);
    webUser.setMagicLinkExpiry(null);
    webUser.setLastLoginDate(now);
    webUser.setEmailVerified(true);
    if (webUser.getVerifiedDate() == null) {
      webUser.setVerifiedDate(now);
    }

    Transaction trans = dataSession.beginTransaction();
    dataSession.update(webUser);
    trans.commit();

    initializeUserSession(appReq, dataSession, webUser);
    appReq.setMessageConfirmation("Signed in using magic link");
    return true;
  }

  private void sendMagicLink(HttpServletRequest request, AppReq appReq, Session dataSession) {
    String emailAddress = request.getParameter(PARAM_MAGIC_EMAIL);
    if (emailAddress == null) {
      emailAddress = "";
    }
    emailAddress = emailAddress.trim();
    if (emailAddress.equals("")) {
      appReq.setMessageProblem("Email address is required to send a magic link");
      return;
    }

    Query query = dataSession.createQuery(
        "from WebUser where lower(emailAddress) = ? and registrationStatus = ? order by webUserId desc");
    query.setParameter(0, emailAddress.toLowerCase());
    query.setParameter(1, "ACTIVE");
    @SuppressWarnings("unchecked")
    List<WebUser> webUserList = query.list();

    if (webUserList.size() > 0) {
      WebUser webUser = selectMagicLinkCandidate(webUserList);
      if (webUser == null) {
        appReq.setMessageConfirmation("If that email is registered, a sign-in link has been sent.");
        return;
      }
      webUser = hydrateWebUserForSession(dataSession, webUser.getWebUserId());
      String rawToken = UUID.randomUUID().toString().replace("-", "")
          + UUID.randomUUID().toString().replace("-", "");
      String tokenHash = hashToken(rawToken);
      Date expiry = new Date(System.currentTimeMillis() + (MAGIC_LINK_MINUTES_VALID * 60L * 1000L));

      Transaction trans = dataSession.beginTransaction();
      webUser.setMagicLinkTokenHash(tokenHash);
      webUser.setMagicLinkExpiry(expiry);
      dataSession.update(webUser);
      trans.commit();

      String magicLink = buildMagicLinkUrl(request, dataSession, webUser.getWebUserId(), rawToken);
      if (TEMP_SHOW_MAGIC_LINK_ON_PAGE) {
        appReq.setMessageConfirmation("Temporary test mode: "
            + "<a href=\"" + magicLink + "\">Open magic sign-in link</a>"
            + " (valid for " + MAGIC_LINK_MINUTES_VALID + " minutes)");
        return;
      } else {
        String body = "<p>A sign-in link was requested for your Project Tracker account.</p>"
            + "<p><a href=\"" + magicLink + "\">Sign in to Project Tracker</a></p>"
            + "<p>This link expires in " + MAGIC_LINK_MINUTES_VALID + " minutes.</p>";
        try {
          MailManager mailManager = new MailManager(dataSession);
          mailManager.sendEmail("Project Tracker Sign-In Link", body, webUser.getEmailAddress());
        } catch (Exception e) {
          appReq.setMessageProblem("Unable to send magic link email: " + e.getMessage());
          return;
        }
      }
    }

    appReq.setMessageConfirmation("If that email is registered, a sign-in link has been sent.");
  }

  private WebUser selectMagicLinkCandidate(List<WebUser> webUserList) {
    WebUser preferred = null;
    for (WebUser candidate : webUserList) {
      if (candidate.getUsername() != null && !candidate.getUsername().trim().equals("")) {
        preferred = candidate;
        break;
      }
    }
    if (preferred != null) {
      return preferred;
    }
    return webUserList.size() > 0 ? webUserList.get(0) : null;
  }

  private WebUser hydrateWebUserForSession(Session dataSession, int webUserId) {
    WebUser webUser = (WebUser) dataSession.get(WebUser.class, webUserId);
    if (webUser != null && webUser.getProvider() != null) {
      // Force provider id to initialize the association before storing user in
      // session.
      webUser.getProvider().getProviderId();
    }
    return webUser;
  }

  private String initializeUserSession(AppReq appReq, Session dataSession, WebUser webUser) {
    appReq.setWebUser(webUser);
    ProjectContact projectContact = (ProjectContact) dataSession.get(ProjectContact.class,
        webUser.getContactId());
    webUser.setProjectContact(projectContact);

    webUser.setTrackTime(TrackerKeysManager
        .getKeyValue(TrackerKeysManager.KEY_TRACK_TIME, "N", webUser, dataSession)
        .equalsIgnoreCase("Y"));

    webUser.setTimeZone(TimeZone.getTimeZone(TrackerKeysManager.getKeyValue(
        TrackerKeysManager.KEY_TIME_ZONE, WebUser.AMERICA_DENVER, webUser, dataSession)));
    webUser.setDateDisplayPattern(TrackerKeysManager.getKeyValue(
        TrackerKeysManager.KEY_DATE_DISPLAY_FORMAT,
        webUser.getDateDisplayPattern(), webUser, dataSession));
    webUser.setDateEntryPattern(TrackerKeysManager.getKeyValue(
        TrackerKeysManager.KEY_DATE_ENTRY_FORMAT,
        webUser.getDateEntryPattern(), webUser, dataSession));
    webUser.setTimeDisplayPattern(TrackerKeysManager.getKeyValue(
        TrackerKeysManager.KEY_TIME_DISPLAY_FORMAT,
        webUser.getTimeDisplayPattern(), webUser, dataSession));
    webUser.setTimeEntryPattern(TrackerKeysManager.getKeyValue(
        TrackerKeysManager.KEY_TIME_ENTRY_FORMAT,
        webUser.getTimeEntryPattern(), webUser, dataSession));

    if (webUser.isTrackTime()) {
      TimeTracker timeTracker = new TimeTracker(webUser, dataSession);
      appReq.setTimeTracker(timeTracker);
    }

    return "Welcome " + projectContact.getNameFirst() + " " + projectContact.getNameLast();
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
          + "?action=" + URLEncoder.encode(ACTION_MAGIC_LOGIN, "UTF-8")
          + "&" + PARAM_MAGIC_USER_ID + "=" + webUserId
          + "&" + PARAM_MAGIC_TOKEN + "=" + URLEncoder.encode(rawToken, "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      return baseLoginUrl
          + "?action=" + ACTION_MAGIC_LOGIN
          + "&" + PARAM_MAGIC_USER_ID + "=" + webUserId
          + "&" + PARAM_MAGIC_TOKEN + "=" + rawToken;
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

  private void printLoginForm(PrintWriter out, String username, String password, String magicEmail,
      String uiMode) {
    String escapedUsername = escapeHtml(username);
    String escapedPassword = escapeHtml(password);
    String escapedMagicEmail = escapeHtml(magicEmail);

    out.println("<form action=\"LoginServlet\" method=\"POST\">");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <td>Username</td>");
    out.println("    <td><input type=\"text\" name=\"username\" value=\"" + escapedUsername + "\"></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>Password</td>");
    out.println(
        "    <td><input type=\"password\" name=\"password\" value=\"" + escapedPassword + "\"></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>Mode</td>");
    out.println("    <td>");
    out.println("      <input type=\"radio\" name=\"uiMode\" value=\"desktop\""
        + (uiMode.equals("desktop") ? " checked" : "") + "> Desktop");
    out.println("      <input type=\"radio\" name=\"uiMode\" value=\"mobile\""
        + (uiMode.equals("mobile") ? " checked" : "") + "> Mobile");
    out.println("    </td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println(
        "    <td colspan=\"2\" align=\"right\"><input type=\"submit\" name=\"action\" value=\"Login\"></td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("</form>");

    out.println("<p>&nbsp;</p>");
    out.println("<form action=\"LoginServlet\" method=\"POST\">");
    out.println("<table>");
    out.println("  <tr>");
    out.println("    <td colspan=\"2\"><b>Email Magic Link</b></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td>Email</td>");
    out.println("    <td><input type=\"text\" name=\"" + PARAM_MAGIC_EMAIL
        + "\" value=\"" + escapedMagicEmail + "\" size=\"40\"></td>");
    out.println("  </tr>");
    out.println("  <tr>");
    out.println("    <td colspan=\"2\" align=\"right\"><input type=\"submit\" name=\"action\" value=\""
        + ACTION_SEND_MAGIC_LINK + "\"></td>");
    out.println("  </tr>");
    out.println("</table>");
    out.println("</form>");
    out.println("<p>&nbsp;</p>");
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

  private boolean hasProvider(WebUser webUser) {
    return webUser.getProvider() != null
        && webUser.getProvider().getProviderId() != null
        && !webUser.getProvider().getProviderId().trim().equals("");
  }

  // <editor-fold defaultstate="collapsed"
  // desc="HttpServlet methods. Click on the + sign on the left to edit the
  // code.">

  /**
   * Handles the HTTP <code>GET</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   * 
   * @param request
   *                 servlet request
   * @param response
   *                 servlet response
   * @throws ServletException
   *                          if a servlet-specific error occurs
   * @throws IOException
   *                          if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    processRequest(request, response);
  }

}
