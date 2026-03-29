package org.openimmunizationsoftware.pt;

import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.model.RememberMeToken;
import org.openimmunizationsoftware.pt.model.WebUser;

/**
 * Manages persistent "remember me" login cookies.
 *
 * <p>
 * Each login on any device inserts a new row into {@code remember_me_token},
 * so multiple devices can be logged in simultaneously. Logging out only
 * removes the token for the current device.
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
     * Generates a fresh token, inserts a new row into {@code remember_me_token},
     * and adds the remember-me cookie to the response. Existing tokens for
     * other devices are not disturbed.
     */
    public static void issueRememberMeCookie(HttpServletResponse response, WebUser webUser,
            Session dataSession) {
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);
        Date expiry = new Date(System.currentTimeMillis() + (long) MAX_AGE_SECONDS * 1000L);

        RememberMeToken token = new RememberMeToken();
        token.setWebUserId(webUser.getWebUserId());
        token.setTokenHash(tokenHash);
        token.setExpiry(expiry);
        Transaction trans = dataSession.beginTransaction();
        dataSession.save(token);
        trans.commit();

        setCookie(response, webUser.getWebUserId(), rawToken);
    }

    /**
     * Extends the stored expiry by another week and refreshes the browser
     * cookie's max-age for the specific device token.
     */
    public static void renewRememberMeCookie(HttpServletResponse response, WebUser webUser,
            String rawToken, Session dataSession) {
        String tokenHash = hashToken(rawToken);
        RememberMeToken token = findToken(dataSession, webUser.getWebUserId(), tokenHash);
        if (token != null) {
            Date newExpiry = new Date(System.currentTimeMillis() + (long) MAX_AGE_SECONDS * 1000L);
            Transaction trans = dataSession.beginTransaction();
            token.setExpiry(newExpiry);
            dataSession.update(token);
            trans.commit();
        }
        setCookie(response, webUser.getWebUserId(), rawToken);
    }

    /**
     * Deletes the specific device token identified by {@code tokenHash} from the
     * database and removes the browser cookie. Only the current device is logged
     * out; other devices remain unaffected.
     *
     * @param tokenHash the SHA-256 hash of the token stored in the current device's
     *                  cookie, or {@code null} if the cookie was never set
     */
    public static void clearRememberMeCookie(HttpServletResponse response, String tokenHash,
            Session dataSession) {
        if (tokenHash != null && dataSession != null) {
            Query q = dataSession.createQuery("from RememberMeToken where tokenHash = :hash");
            q.setParameter("hash", tokenHash);
            @SuppressWarnings("unchecked")
            List<RememberMeToken> tokens = q.list();
            if (!tokens.isEmpty()) {
                Transaction trans = dataSession.beginTransaction();
                for (RememberMeToken t : tokens) {
                    dataSession.delete(t);
                }
                trans.commit();
            }
        }
        expireCookie(response);
    }

    /**
     * Looks up a {@link RememberMeToken} by user id and token hash.
     *
     * @return the matching token, or {@code null} if none exists
     */
    public static RememberMeToken findToken(Session dataSession, int webUserId, String tokenHash) {
        Query q = dataSession.createQuery(
                "from RememberMeToken where webUserId = :uid and tokenHash = :hash");
        q.setParameter("uid", webUserId);
        q.setParameter("hash", tokenHash);
        @SuppressWarnings("unchecked")
        List<RememberMeToken> results = q.list();
        return results.isEmpty() ? null : results.get(0);
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
