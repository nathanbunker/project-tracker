package org.openimmunizationsoftware.pt;

import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Manages persistent "remember me" login cookies.
 *
 * <p>
 * A secure random token is generated at login, stored as a SHA-256 hash in
 * the database, and placed in an HttpOnly cookie. On subsequent requests the
 * cookie is validated against the hash; if valid the user session is restored
 * transparently.
 * </p>
 *
 * <p>
 * The token and its expiry are rolled forward on every successful cookie
 * restore so that the user stays logged in as long as they access the app at
 * least once per week.
 * </p>
 */
public class RememberMeManager {

    public static final String COOKIE_NAME = "rememberMe";
    /**
     * Seven days in seconds – maximum time between accesses before re-login is
     * required.
     */
    public static final int MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * Generates a fresh token, stores its hash and expiry in the database, and
     * adds the remember-me cookie to the response.
     *
     * @param response    the HTTP response to add the cookie to
     * @param webUser     the authenticated user; must already be persisted
     * @param dataSession an open Hibernate session
     */
    public static void issueRememberMeCookie(HttpServletResponse response, WebUser webUser,
            Session dataSession) {
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);
        Date expiry = new Date(System.currentTimeMillis() + (long) MAX_AGE_SECONDS * 1000L);

        Transaction trans = dataSession.beginTransaction();
        webUser.setRememberMeTokenHash(tokenHash);
        webUser.setRememberMeExpiry(expiry);
        dataSession.update(webUser);
        trans.commit();

        setCookie(response, webUser.getWebUserId(), rawToken);
    }

    /**
     * Extends the stored expiry by another week and refreshes the browser
     * cookie's max-age, keeping the same raw token value.
     *
     * @param response    the HTTP response to add the renewed cookie to
     * @param webUser     the current user
     * @param rawToken    the raw (un-hashed) token that was read from the cookie
     * @param dataSession an open Hibernate session
     */
    public static void renewRememberMeCookie(HttpServletResponse response, WebUser webUser,
            String rawToken, Session dataSession) {
        Date newExpiry = new Date(System.currentTimeMillis() + (long) MAX_AGE_SECONDS * 1000L);

        Transaction trans = dataSession.beginTransaction();
        webUser.setRememberMeExpiry(newExpiry);
        dataSession.update(webUser);
        trans.commit();

        setCookie(response, webUser.getWebUserId(), rawToken);
    }

    /**
     * Clears the remember-me token from the database and removes the browser
     * cookie. Call this on logout.
     *
     * @param response    the HTTP response
     * @param webUser     the user being logged out (may be {@code null})
     * @param dataSession an open Hibernate session
     */
    public static void clearRememberMeCookie(HttpServletResponse response, WebUser webUser,
            Session dataSession) {
        if (webUser != null && webUser.getRememberMeTokenHash() != null) {
            Transaction trans = dataSession.beginTransaction();
            webUser.setRememberMeTokenHash(null);
            webUser.setRememberMeExpiry(null);
            dataSession.update(webUser);
            trans.commit();
        }
        expireCookie(response);
    }

    /**
     * Removes the browser cookie without touching the database. Use this when
     * the token cannot be looked up (e.g., malformed cookie value).
     */
    public static void expireCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /** Returns the SHA-256 hex digest of the supplied token string. */
    public static String hashToken(String rawToken) {
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

    private static void setCookie(HttpServletResponse response, int webUserId, String rawToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, webUserId + ":" + rawToken);
        cookie.setMaxAge(MAX_AGE_SECONDS);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
