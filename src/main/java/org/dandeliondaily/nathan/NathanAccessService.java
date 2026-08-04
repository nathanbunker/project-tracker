package org.dandeliondaily.nathan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.model.NathanAccess;
import org.openimmunizationsoftware.pt.model.NathanAccessEvent;

public class NathanAccessService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void requestAccess(Session session, String email) {
        Date now = new Date();
        Query query = session.createQuery("from NathanAccess where accessType = :type and status = :status "
                + "and lower(email) = :email order by nathanAccessId desc");
        query.setString("type", NathanAccess.TYPE_REQUEST);
        query.setString("status", NathanAccess.STATUS_PENDING);
        query.setString("email", email);
        query.setMaxResults(1);
        NathanAccess access = (NathanAccess) query.uniqueResult();

        Transaction transaction = session.beginTransaction();
        try {
            if (access == null) {
                access = new NathanAccess();
                access.setCreatedAt(now);
                access.setAccessType(NathanAccess.TYPE_REQUEST);
                access.setStatus(NathanAccess.STATUS_PENDING);
                access.setEmail(email);
                access.setRequestedAt(now);
                session.save(access);
            } else {
                access.setRequestedAt(now);
                session.update(access);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    public CreatedAccess createInvitation(Session session, int adminUserId, String label, String email,
            String notes, Date expiresAt) {
        String rawToken = generateToken();
        NathanAccess access = new NathanAccess();
        access.setCreatedAt(new Date());
        access.setCreatedByUserId(Integer.valueOf(adminUserId));
        access.setAccessType(NathanAccess.TYPE_INVITATION);
        access.setStatus(NathanAccess.STATUS_ACTIVE);
        access.setTokenHash(hashToken(rawToken));
        access.setLabel(emptyToNull(label));
        access.setEmail(emptyToNull(email));
        access.setNotes(emptyToNull(notes));
        access.setExpiresAt(expiresAt);
        save(session, access);
        return new CreatedAccess(access, rawToken);
    }

    public CreatedAccess approveRequest(Session session, int accessId, int adminUserId) {
        NathanAccess access = (NathanAccess) session.get(NathanAccess.class, accessId);
        if (access == null || !NathanAccess.TYPE_REQUEST.equals(access.getAccessType())
                || !NathanAccess.STATUS_PENDING.equals(access.getStatus())) {
            return null;
        }
        String rawToken = generateToken();
        Transaction transaction = session.beginTransaction();
        try {
            access.setStatus(NathanAccess.STATUS_ACTIVE);
            access.setTokenHash(hashToken(rawToken));
            access.setApprovedAt(new Date());
            access.setApprovedByUserId(Integer.valueOf(adminUserId));
            session.update(access);
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
        return new CreatedAccess(access, rawToken);
    }

    public NathanAccess validateToken(Session session, String rawToken) {
        if (rawToken == null || rawToken.length() < 32 || rawToken.length() > 200) {
            return null;
        }
        Query query = session.createQuery("from NathanAccess where tokenHash = :tokenHash and status = :status");
        query.setString("tokenHash", hashToken(rawToken));
        query.setString("status", NathanAccess.STATUS_ACTIVE);
        NathanAccess access = (NathanAccess) query.uniqueResult();
        if (access != null && access.getExpiresAt() != null && !access.getExpiresAt().after(new Date())) {
            return null;
        }
        return access;
    }

    public boolean accessExists(Session session, int accessId) {
        return session.get(NathanAccess.class, accessId) != null;
    }

    public void setEnabled(Session session, int accessId, boolean enabled) {
        NathanAccess access = (NathanAccess) session.get(NathanAccess.class, accessId);
        if (access == null) {
            return;
        }
        Transaction transaction = session.beginTransaction();
        try {
            if (enabled && NathanAccess.TYPE_REQUEST.equals(access.getAccessType()) && access.getTokenHash() == null) {
                access.setStatus(NathanAccess.STATUS_PENDING);
            } else {
                access.setStatus(enabled ? NathanAccess.STATUS_ACTIVE : NathanAccess.STATUS_DISABLED);
            }
            session.update(access);
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    public void recordPageView(Session session, int accessId) {
        NathanAccess access = (NathanAccess) session.get(NathanAccess.class, accessId);
        if (access == null) {
            return;
        }
        Date now = new Date();
        Transaction transaction = session.beginTransaction();
        try {
            if (access.getFirstUsedAt() == null) {
                access.setFirstUsedAt(now);
            }
            access.setLastUsedAt(now);
            access.setUseCount(access.getUseCount() + 1);
            session.update(access);
            saveEvent(session, accessId, NathanAccessEvent.TYPE_PAGE_VIEW, null, now);
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    public void recordEvent(Session session, int accessId, String eventType, String contentKey) {
        if (!accessExists(session, accessId)) {
            return;
        }
        Transaction transaction = session.beginTransaction();
        try {
            saveEvent(session, accessId, eventType, emptyToNull(contentKey), new Date());
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public List<NathanAccess> listByStatus(Session session, String status) {
        Query query = session.createQuery("from NathanAccess where status = :status "
                + "order by coalesce(requestedAt, createdAt) desc");
        query.setString("status", status);
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<NathanAccessEvent> listActivity(Session session, int accessId) {
        Query query = session.createQuery("from NathanAccessEvent where nathanAccessId = :accessId "
                + "and eventType <> :pageView order by createdAt desc");
        query.setInteger("accessId", accessId);
        query.setString("pageView", NathanAccessEvent.TYPE_PAGE_VIEW);
        query.setMaxResults(20);
        return query.list();
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void save(Session session, NathanAccess access) {
        Transaction transaction = session.beginTransaction();
        try {
            session.save(access);
            transaction.commit();
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        }
    }

    private static void saveEvent(Session session, int accessId, String eventType, String contentKey, Date createdAt) {
        NathanAccessEvent event = new NathanAccessEvent();
        event.setNathanAccessId(accessId);
        event.setCreatedAt(createdAt);
        event.setEventType(eventType);
        event.setContentKey(contentKey);
        session.save(event);
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }

    public static class CreatedAccess {
        private final NathanAccess access;
        private final String rawToken;

        CreatedAccess(NathanAccess access, String rawToken) {
            this.access = access;
            this.rawToken = rawToken;
        }

        public NathanAccess getAccess() { return access; }
        public String getRawToken() { return rawToken; }
    }
}