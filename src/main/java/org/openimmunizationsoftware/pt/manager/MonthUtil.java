package org.openimmunizationsoftware.pt.manager;

import java.util.Calendar;
import java.util.Date;

public class MonthUtil {
  public static Date getMonthDate(Date d) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(d);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    return calendar.getTime();
  }

  public static Date getNextMonth(Date d) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(d);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.MONTH, 1);
    return calendar.getTime();
  }

  public static Date getPrevMonth(Date d) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(d);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.MONTH, -1);
    return calendar.getTime();
  }

  public static Date getMonthLastYear(Date d) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(d);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.YEAR, -1);
    return calendar.getTime();
  }

  public static Date thisMonth(Date currentMonth, Date d) {
    Calendar monthCal = Calendar.getInstance();
    monthCal.setTime(currentMonth);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(d);
    calendar.set(Calendar.MONTH, monthCal.get(Calendar.MONTH));
    calendar.set(Calendar.YEAR, monthCal.get(Calendar.YEAR));
    if (calendar.get(Calendar.DAY_OF_MONTH) > calendar.getMaximum(Calendar.DAY_OF_MONTH)) {
      calendar.set(Calendar.DAY_OF_MONTH, calendar.getMaximum(Calendar.DAY_OF_MONTH));
    }
    return calendar.getTime();
  }

  public static boolean sameMonthOnly(Date d1, Date d2) {
    Calendar c1 = Calendar.getInstance();
    c1.setTime(d1);

    Calendar c2 = Calendar.getInstance();
    c2.setTime(d2);
    return c2.get(Calendar.MONTH) == c1.get(Calendar.MONTH);
  }

}
