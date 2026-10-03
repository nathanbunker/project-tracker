package org.dandeliondaily.mcp.service;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

public class McpWebUserSupportTest {

    @Test
    public void hydratesMissingProjectContactFromContactId() {
        ProjectContact contact = new ProjectContact();
        contact.setContactId(42);
        AtomicInteger gets = new AtomicInteger();
        WebUser owner = new WebUser();
        owner.setContactId(42);

        McpWebUserSupport.hydrateProjectContact(sessionReturning(contact, gets), owner);

        Assert.assertSame(contact, owner.getProjectContact());
        Assert.assertEquals(1, gets.get());
    }

    @Test
    public void leavesExistingProjectContactAlone() {
        ProjectContact existing = new ProjectContact();
        existing.setContactId(42);
        AtomicInteger gets = new AtomicInteger();
        WebUser owner = new WebUser();
        owner.setContactId(42);
        owner.setProjectContact(existing);

        McpWebUserSupport.hydrateProjectContact(sessionReturning(new ProjectContact(), gets), owner);

        Assert.assertSame(existing, owner.getProjectContact());
        Assert.assertEquals(0, gets.get());
    }

    private static Session sessionReturning(final ProjectContact contact, final AtomicInteger gets) {
        return (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[] { Session.class },
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args.length == 2 && args[0] == ProjectContact.class
                            && Integer.valueOf(42).equals(args[1])) {
                        gets.incrementAndGet();
                        return contact;
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestSession";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
