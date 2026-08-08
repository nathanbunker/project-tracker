package org.openimmunizationsoftware.pt.servlet;

import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Base64;
import java.util.TimeZone;

import org.dandeliondaily.weeklyreport.model.WeeklyReportViewModel;
import org.dandeliondaily.weeklyreport.render.WeeklyReportRenderer;
import org.dandeliondaily.weeklyreport.service.WeeklyReportDataService;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.model.WeeklyReport;

final class WeeklyReportSupport {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter WEEK_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private WeeklyReportSupport() {
    }

    static String generateAccessKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashAccessKey(String accessKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(accessKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(64);
            for (byte value : digest) {
                hash.append(String.format("%02x", value & 0xff));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    static LocalDate latestCompletedWeekSunday(LocalDate today) {
        LocalDate lastCompletedSaturday = today.minusDays(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        return lastCompletedSaturday.minusDays(6);
    }

    static LocalDate normalizeWeek(String weekValue, LocalDate today) {
        LocalDate latest = latestCompletedWeekSunday(today);
        if (weekValue == null || weekValue.trim().length() == 0) {
            return latest;
        }
        try {
            LocalDate requested = LocalDate.parse(weekValue, WEEK_FORMAT)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            return requested.isAfter(latest) ? latest : requested;
        } catch (DateTimeParseException e) {
            return latest;
        }
    }

    static WebUser loadOwner(Session dataSession, WeeklyReport report) {
        WebUser owner = (WebUser) dataSession.get(WebUser.class, report.getOwnerUserId());
        if (owner != null) {
            owner.setProjectContact((ProjectContact) dataSession.get(ProjectContact.class, owner.getContactId()));
            owner.setTimeZone(TimeZone.getTimeZone(TrackerKeysManager.getKeyValue(
                    TrackerKeysManager.KEY_TIME_ZONE, WebUser.AMERICA_DENVER, owner, dataSession)));
        }
        return owner;
    }

    static void printReportBody(PrintWriter out, Session dataSession, WeeklyReport report, WebUser owner,
            LocalDate weekSunday, String route, String accessKey) {
        LocalDate latest = latestCompletedWeekSunday(owner.getLocalDateToday());
        WeeklyReportViewModel model = new WeeklyReportDataService().load(dataSession, report, owner, weekSunday);
        new WeeklyReportRenderer().render(out, model, owner, latest, route, accessKey);
    }
}