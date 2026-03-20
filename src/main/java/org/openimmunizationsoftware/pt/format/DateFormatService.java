package org.openimmunizationsoftware.pt.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public interface DateFormatService {

    String PATTERN_DATE_SHORT = "MM/dd/yyyy";
    String PATTERN_DATE_SHORT_EU = "dd/MM/yyyy";
    String PATTERN_DATE_LONG = "EEEE, MMM d, yyyy";
    String PATTERN_DATE_TIME_SHORT = "MM/dd/yyyy h:mm a";
    String PATTERN_MONTH_YEAR = "MMM yyyy";
    String PATTERN_WEEKDAY_SHORT = "EEE";
    String PATTERN_WEEKDAY_LONG = "EEEE";
    String PATTERN_TRANSPORT_DATE = "yyyy-MM-dd";
    String PATTERN_TIME_12H = "hh:mm aaa";
    String PATTERN_TIME_24H = "HH:mm";

    SimpleDateFormat createLegacyFormatter(String pattern, TimeZone timeZone);

    String formatPattern(Date value, String pattern, TimeZone timeZone);

    String formatDate(Date value, TimeZone timeZone);

    String formatDateLong(Date value, TimeZone timeZone);

    String formatDateTime(Date value, TimeZone timeZone);

    String formatMonthYear(Date value, TimeZone timeZone);

    String formatWeekdayShort(Date value, TimeZone timeZone);

    String formatWeekdayLong(Date value, TimeZone timeZone);

    String formatTransportDate(Date value, TimeZone timeZone);

    Date parseUserDate(String value, TimeZone timeZone);

    Date parseUserDate(String value, TimeZone timeZone, String preferredDatePattern);

    Date parseUserDateTime(String value, TimeZone timeZone);

    Date parseUserDateTime(String value, TimeZone timeZone, String preferredDatePattern,
            String preferredTimePattern);

    Date parseTransportDate(String value, TimeZone timeZone);

    Date parseApiDateTimeLenient(String value, TimeZone timeZone);
}