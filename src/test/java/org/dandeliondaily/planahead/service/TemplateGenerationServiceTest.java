package org.dandeliondaily.planahead.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillExpectedId;

public class TemplateGenerationServiceTest {

    @Test
    public void billedReadyInstanceTodayIsPreserved() {
        Assert.assertTrue(TemplateGenerationService.shouldPreserveBilledToday(true, true));
    }

    @Test
    public void unbilledInstanceTodayCanBeReconciled() {
        Assert.assertFalse(TemplateGenerationService.shouldPreserveBilledToday(true, false));
    }

    @Test
    public void futureInstanceCanBeReconciled() {
        Assert.assertFalse(TemplateGenerationService.shouldPreserveBilledToday(false, true));
    }

    @Test
    public void cleanupPastActualTimesBuildsExpectedBulkUpdate() {
        TemplateGenerationService service = new TemplateGenerationService();
        RecordingSessionHandler sessionHandler = new RecordingSessionHandler();
        Session session = sessionHandler.createSessionProxy();

        LocalDate today = LocalDate.of(2026, 5, 26);
        service.cleanupPastActualTimes(session, 42, 99, today);

        Assert.assertEquals(
                "update ActionNext an set an.nextTimeActual = 0 "
                        + "where an.workspaceId = :workspaceId "
                        + "and (an.contactId = :contactId or an.nextContactId = :contactId) "
                        + "and an.nextActionDate is not null and an.nextActionDate < :today "
                        + "and an.nextTimeActual > 0 "
                        + "and an.nextActionStatusString <> :cancelled "
                        + "and an.nextActionStatusString <> :completed",
                sessionHandler.queryString);
        Assert.assertEquals(Integer.valueOf(42), sessionHandler.parameters.get("workspaceId"));
        Assert.assertEquals(Integer.valueOf(99), sessionHandler.parameters.get("contactId"));
        Assert.assertEquals(java.sql.Date.valueOf(today), sessionHandler.parameters.get("today"));
        Assert.assertEquals("X", sessionHandler.parameters.get("cancelled"));
        Assert.assertEquals("C", sessionHandler.parameters.get("completed"));
        Assert.assertEquals(1, sessionHandler.executeUpdateCount);
    }

    @Test
    public void autoCancelDoesNotGenerateOnNonWorkingDay() {
        TemplateGenerationService service = new TemplateGenerationService();
        LocalDate fridayOff = LocalDate.of(2026, 8, 7);
        TemplateWorkdayCalendar calendar = calendar(
                availability(fridayOff, 0, "N"));

        Set<LocalDate> dates = service.resolveScheduledDates(
                Collections.singletonList(fridayOff), calendar, true,
                "AUTO_CANCEL", fridayOff, fridayOff.plusDays(10));

        Assert.assertTrue(dates.isEmpty());
    }

    @Test
    public void ignoreGeneratesOnScheduledNonWorkingDay() {
        TemplateGenerationService service = new TemplateGenerationService();
        LocalDate fridayOff = LocalDate.of(2026, 8, 7);
        TemplateWorkdayCalendar calendar = calendar(
                availability(fridayOff, 0, "N"));

        Set<LocalDate> dates = service.resolveScheduledDates(
                Collections.singletonList(fridayOff), calendar, true,
                "IGNORE", fridayOff, fridayOff.plusDays(10));

        Assert.assertEquals(Collections.singleton(fridayOff), dates);
    }

    @Test
    public void carryForwardUsesNextWorkingDayBeyondNominalWindow() {
        TemplateGenerationService service = new TemplateGenerationService();
        LocalDate fridayOff = LocalDate.of(2026, 8, 7);
        LocalDate mondayWorking = LocalDate.of(2026, 8, 10);
        TemplateWorkdayCalendar calendar = calendar(
                availability(fridayOff, 0, "N"),
                availability(LocalDate.of(2026, 8, 8), 0, "N"),
                availability(LocalDate.of(2026, 8, 9), 0, "N"),
                availability(mondayWorking, 480, "W"));

        Set<LocalDate> dates = service.resolveScheduledDates(
                Collections.singletonList(fridayOff), calendar, true,
                "CARRY_FORWARD", fridayOff, mondayWorking.plusDays(10));

        Assert.assertEquals(Collections.singleton(mondayWorking), dates);
    }

    @Test
    public void carryForwardRetainsEffectiveDateOnLaterSchedulerRun() {
        TemplateGenerationService service = new TemplateGenerationService();
        LocalDate fridayOff = LocalDate.of(2026, 8, 7);
        LocalDate sundayRun = LocalDate.of(2026, 8, 9);
        LocalDate mondayWorking = LocalDate.of(2026, 8, 10);
        TemplateWorkdayCalendar calendar = calendar(
                availability(fridayOff, 0, "N"),
                availability(LocalDate.of(2026, 8, 8), 0, "N"),
                availability(sundayRun, 0, "N"),
                availability(mondayWorking, 480, "W"));

        Set<LocalDate> dates = service.resolveScheduledDates(
                Collections.singletonList(fridayOff), calendar, true,
                "CARRY_FORWARD", sundayRun, mondayWorking.plusDays(10));

        Assert.assertEquals(Collections.singleton(mondayWorking), dates);
    }

    private TemplateWorkdayCalendar calendar(BillExpected... availability) {
        return new TemplateWorkdayCalendar(Arrays.asList(availability));
    }

    private BillExpected availability(LocalDate date, int minutes, String status) {
        return new BillExpected(new BillExpectedId(7, java.sql.Date.valueOf(date)), minutes, 0, status);
    }

    private static final class RecordingSessionHandler implements InvocationHandler {

        private String queryString;
        private final Map<String, Object> parameters = new HashMap<String, Object>();
        private int executeUpdateCount;

        Session createSessionProxy() {
            return (Session) Proxy.newProxyInstance(
                    Session.class.getClassLoader(),
                    new Class<?>[] { Session.class },
                    this);
        }

        Query createQueryProxy() {
            return (Query) Proxy.newProxyInstance(
                    Query.class.getClassLoader(),
                    new Class<?>[] { Query.class },
                    new RecordingQueryHandler());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("createQuery".equals(methodName)) {
                queryString = (String) args[0];
                return createQueryProxy();
            }
            if ("toString".equals(methodName)) {
                return "RecordingSessionProxy";
            }
            return defaultValue(method.getReturnType());
        }

        private final class RecordingQueryHandler implements InvocationHandler {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                if ("setParameter".equals(methodName)) {
                    parameters.put((String) args[0], args[1]);
                    return proxy;
                }
                if ("executeUpdate".equals(methodName)) {
                    executeUpdateCount++;
                    return Integer.valueOf(1);
                }
                if ("toString".equals(methodName)) {
                    return "RecordingQueryProxy";
                }
                return defaultValue(method.getReturnType());
            }
        }

        private Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (returnType == boolean.class) {
                return Boolean.FALSE;
            }
            if (returnType == byte.class) {
                return Byte.valueOf((byte) 0);
            }
            if (returnType == short.class) {
                return Short.valueOf((short) 0);
            }
            if (returnType == int.class) {
                return Integer.valueOf(0);
            }
            if (returnType == long.class) {
                return Long.valueOf(0L);
            }
            if (returnType == float.class) {
                return Float.valueOf(0F);
            }
            if (returnType == double.class) {
                return Double.valueOf(0D);
            }
            if (returnType == char.class) {
                return Character.valueOf('\0');
            }
            return null;
        }
    }
}