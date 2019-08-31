package org.openimmunizationsoftware.pt.report;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.WebUser;

public class GenerateReport {

  public static final String PARAM_MESSAGE_SUBJECT = "messageSubject";
  public static final String PARAM_MESSAGE_TEXT = "messageText";
  public static final String PARAM_MESSAGE_TYPE = "messageType";
  public static final String PARAM_FILE_NAME = "fileName";
  public static final String PARAM_FILE_CONTENTS = "fileContents";
  public static final String PARAM_FILE_TYPE = "fileType";
  public static final String PARAM_COUNT_TOTAL = "countTotal";
  public static final String PARAM_COUNT_ERROR = "countError";
  public static final String PARAM_ITEM_ID = "itemId";
  public static final String PARAM_PROVIDER_ID = "providerId";
  public static final String PARAM_PROVIDER_NAME = "providerName";
  public static final String PARAM_PROFILE_ID = "profileId";
  public static final String PARAM_PROFILE_NAME = "profileLabel";
  public static final String PARAM_WEBUSER_USERNAME = "webuser.username";
  public static final String PARAM_WEBUSER_NAMEFIRST = "webuser.namefirst";
  public static final String PARAM_WEBUSER_NAMELAST = "webuser.namelast";
  public static final String PARAM_WEBUSER_USERID = "webuser.userid";

  public static void doHeader(PrintWriter out, ReportProfile reportProfile) {
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>" + reportProfile.getProfileLabel() + "</title>");
    out.println(
        "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"/>");
    //    out.println("    <style>");
    //    out.println("       <!--");
    //    out.println(reportProfile.getReportCSS());
    //    out.println("       -->");
    //    out.println("    </style>");
    out.println("  </head>");
    out.println("  <body>");
  }

  public static void doFooter(PrintWriter out, ReportProfile reportProfile, String method, WebUser webUser) {
    SimpleDateFormat sdf = webUser.getDateFormat("MM/dd/yyyy hh:mm:ss a");
    out.println("    <cite>");
    out.println("      " + reportProfile.getProfileLabel());
    out.println("      - " + method + " " + sdf.format(new Date()) + " ");
    out.println("    </cite>");
    out.println("  </body>");
    out.println("</html>");

  }

  public static void populateStartEndDates(ReportBatch reportBatch, WebUser webUser) throws Exception {
    SimpleDateFormat sdf = webUser.getDateFormat("MM/dd/yyyy hh:mm:ss a");
    SimpleDateFormat sdfDate = webUser.getDateFormat();
    SimpleDateFormat sdfMonth = webUser.getMonthFormat();
    SimpleDateFormat sdfYear = webUser.getDateFormat("yyyy");

    Date runDate = sdf.parse(reportBatch.getRunDate());
    Calendar startDate = webUser.getCalendar();
    startDate.setTime(runDate);
    Calendar endDate = webUser.getCalendar();
    startDate.setTime(runDate);
    startDate.set(Calendar.HOUR, 0);
    startDate.set(Calendar.MINUTE, 0);
    startDate.set(Calendar.SECOND, 0);
    endDate.set(Calendar.HOUR, 0);
    endDate.set(Calendar.MINUTE, 0);
    endDate.set(Calendar.SECOND, 0);
    if (reportBatch.isPeriodDaily()) {
      startDate.add(Calendar.DAY_OF_MONTH, -1);
      reportBatch.setDateRangeLabel(sdfDate.format(startDate.getTime()));
    } else if (reportBatch.isPeriodWeekly()) {
      startDate.add(Calendar.DAY_OF_MONTH, -7);
      Calendar tempEndDate = webUser.getCalendar();
      tempEndDate.setTime(endDate.getTime());
      tempEndDate.add(Calendar.DAY_OF_MONTH, -1);
      reportBatch.setDateRangeLabel(
          sdfDate.format(startDate.getTime()) + " - " + sdfDate.format(tempEndDate.getTime()));
    } else if (reportBatch.isPeriodMonthly()) {
      endDate.set(Calendar.DAY_OF_MONTH, 1);
      startDate.add(Calendar.MONTH, -1);
      startDate.set(Calendar.DAY_OF_MONTH, 1);
      reportBatch.setDateRangeLabel(sdfMonth.format(startDate.getTime()));
    } else if (reportBatch.isPeriodYearly()) {
      endDate.set(Calendar.DAY_OF_MONTH, 1);
      startDate.add(Calendar.YEAR, -1);
      startDate.set(Calendar.DAY_OF_MONTH, 1);
      if (endDate.get(Calendar.MONTH) == Calendar.JANUARY) {
        reportBatch.setDateRangeLabel(sdfYear.format(startDate.getTime()));
      } else {
        Calendar tempEndDate = Calendar.getInstance();
        tempEndDate.setTime(endDate.getTime());
        tempEndDate.add(Calendar.DAY_OF_MONTH, -1);
        reportBatch.setDateRangeLabel(
            sdfMonth.format(startDate.getTime()) + " - " + sdfMonth.format(tempEndDate.getTime()));
      }
    }
    reportBatch.setStartDate(sdfDate.format(startDate.getTime()));
    reportBatch.setEndDate(sdfDate.format(endDate.getTime()));

  }

}
