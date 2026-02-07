package org.openimmunizationsoftware.pt.api.common;

import org.hibernate.Session;

public final class HibernateRequestContext {

    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<Session>();

    private HibernateRequestContext() {
    }

    static void set(Session session) {
        CURRENT.set(session);
    }

    public static Session getCurrentSession() {
        Session session = CURRENT.get();
        if (session == null) {
            throw new IllegalStateException("No Hibernate Session bound to current thread.");
        }
        return session;
    }

    static void clear() {
        CURRENT.remove();
    }
}
