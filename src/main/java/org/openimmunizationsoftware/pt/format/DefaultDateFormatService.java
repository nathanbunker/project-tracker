package org.openimmunizationsoftware.pt.format;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DefaultDateFormatService implements DateFormatService {

    @Override
    public SimpleDateFormat createLegacyFormatter(String pattern, TimeZone timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        sdf.setTimeZone(resolveTimeZone(timeZone));
        return sdf;
    }

    @Override
    public String formatPattern(Date value, String pattern, TimeZone timeZone) {
        if (value == null) {
            return "";
        }
        return createLegacyFormatter(pattern, timeZone).format(value);
    }

    @Override
    public String formatDate(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_DATE_SHORT, timeZone);
    }

    @Override
    public String formatDateLong(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_DATE_LONG, timeZone);
    }

    @Override
    public String formatDateTime(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_DATE_TIME_SHORT, timeZone);
    }

    @Override
    public String formatMonthYear(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_MONTH_YEAR, timeZone);
    }

    @Override
    public String formatWeekdayShort(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_WEEKDAY_SHORT, timeZone);
    }

    @Override
    public String formatWeekdayLong(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_WEEKDAY_LONG, timeZone);
    }

    @Override
    public String formatTransportDate(Date value, TimeZone timeZone) {
        return formatPattern(value, PATTERN_TRANSPORT_DATE, timeZone);
    }

    @Override
    public Date parseUserDate(String value, TimeZone timeZone) {
        return parseUserDate(value, timeZone, PATTERN_DATE_SHORT);
    }

    @Override
    public Date parseUserDate(String value, TimeZone timeZone, String preferredDatePattern) {
        List<String> patterns = new ArrayList<String>();
        addUnique(patterns, normalizeDatePattern(preferredDatePattern));
        addUnique(patterns, PATTERN_DATE_SHORT);
        addUnique(patterns, PATTERN_DATE_SHORT_EU);
        addUnique(patterns, PATTERN_TRANSPORT_DATE);
        return parseRequired(value, timeZone, patterns.toArray(new String[patterns.size()]));
    }

    @Override
    public Date parseUserDateTime(String value, TimeZone timeZone) {
        return parseUserDateTime(value, timeZone, PATTERN_DATE_SHORT, PATTERN_TIME_12H);
    }

    @Override
    public Date parseUserDateTime(String value, TimeZone timeZone, String preferredDatePattern,
            String preferredTimePattern) {
        List<String> patterns = new ArrayList<String>();
        String[] datePatterns = new String[] {
                normalizeDatePattern(preferredDatePattern),
                PATTERN_DATE_SHORT,
                PATTERN_DATE_SHORT_EU,
                PATTERN_TRANSPORT_DATE };

        String normalizedTimePattern = normalizeTimePattern(preferredTimePattern);
        for (String datePattern : datePatterns) {
            appendDateTimePatterns(patterns, datePattern, normalizedTimePattern);
            if (PATTERN_TIME_24H.equals(normalizedTimePattern)) {
                appendDateTimePatterns(patterns, datePattern, PATTERN_TIME_12H);
            } else {
                appendDateTimePatterns(patterns, datePattern, PATTERN_TIME_24H);
            }
        }

        addUnique(patterns, "MM/dd/yyyy h:mm:ss a");
        addUnique(patterns, "MM/dd/yyyy hh:mm:ss a");
        addUnique(patterns, "MM/dd/yyyy h:mm a");
        addUnique(patterns, "MM/dd/yyyy hh:mm a");
        addUnique(patterns, "MM/dd/yyyy hh:mm aaa");

        return parseRequired(value, timeZone, patterns.toArray(new String[patterns.size()]));
    }

    @Override
    public Date parseTransportDate(String value, TimeZone timeZone) {
        return parseRequired(value, timeZone, new String[] { "yyyy-MM-dd" });
    }

    @Override
    public Date parseApiDateTimeLenient(String value, TimeZone timeZone) {
        String trimmed = normalize(value);

        try {
            return Date.from(Instant.parse(trimmed));
        } catch (DateTimeParseException ex) {
            // Continue trying supported fallbacks.
        }

        try {
            OffsetDateTime parsed = OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Date.from(parsed.toInstant());
        } catch (DateTimeParseException ex) {
            // Continue trying supported fallbacks.
        }

        try {
            LocalDateTime parsed = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(parsed.atZone(resolveTimeZone(timeZone).toZoneId()).toInstant());
        } catch (DateTimeParseException ex) {
            // Continue trying supported fallbacks.
        }

        return parseRequired(trimmed, timeZone,
                new String[] { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" });
    }

    private Date parseRequired(String value, TimeZone timeZone, String[] patterns) {
        String trimmed = normalize(value);
        TimeZone resolvedTimeZone = resolveTimeZone(timeZone);
        for (String pattern : patterns) {
            Date parsed = tryParse(trimmed, pattern, resolvedTimeZone);
            if (parsed != null) {
                return parsed;
            }
        }
        throw new IllegalArgumentException("Unsupported date format: " + value);
    }

    private Date tryParse(String value, String pattern, TimeZone timeZone) {
        SimpleDateFormat sdf = createLegacyFormatter(pattern, timeZone);
        ParsePosition position = new ParsePosition(0);
        Date parsed = sdf.parse(value, position);
        if (parsed != null && position.getIndex() == value.length()) {
            return parsed;
        }

        if ("yyyy-MM-dd".equals(pattern)) {
            try {
                LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                ZoneId zoneId = timeZone.toZoneId();
                return Date.from(localDate.atStartOfDay(zoneId).toInstant());
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Date value is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new IllegalArgumentException("Date value is required.");
        }
        return trimmed;
    }

    private TimeZone resolveTimeZone(TimeZone timeZone) {
        return timeZone == null ? TimeZone.getDefault() : timeZone;
    }

    private void addUnique(List<String> patterns, String pattern) {
        if (pattern == null || pattern.length() == 0) {
            return;
        }
        if (!patterns.contains(pattern)) {
            patterns.add(pattern);
        }
    }

    private String normalizeDatePattern(String datePattern) {
        if (PATTERN_DATE_SHORT_EU.equals(datePattern)) {
            return PATTERN_DATE_SHORT_EU;
        }
        if (PATTERN_TRANSPORT_DATE.equals(datePattern)) {
            return PATTERN_TRANSPORT_DATE;
        }
        return PATTERN_DATE_SHORT;
    }

    private String normalizeTimePattern(String timePattern) {
        if (PATTERN_TIME_24H.equals(timePattern)) {
            return PATTERN_TIME_24H;
        }
        return PATTERN_TIME_12H;
    }

    private void appendDateTimePatterns(List<String> patterns, String datePattern, String timePattern) {
        if (PATTERN_TIME_24H.equals(timePattern)) {
            addUnique(patterns, datePattern + " HH:mm:ss");
            addUnique(patterns, datePattern + " HH:mm");
        } else {
            addUnique(patterns, datePattern + " h:mm:ss a");
            addUnique(patterns, datePattern + " hh:mm:ss a");
            addUnique(patterns, datePattern + " h:mm a");
            addUnique(patterns, datePattern + " hh:mm a");
            addUnique(patterns, datePattern + " hh:mm aaa");
        }
    }
}