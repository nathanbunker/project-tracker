package org.openimmunizationsoftware.pt.report;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.manager.MailManager;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.ReportSchedule;
import org.openimmunizationsoftware.pt.model.WebUser;
import org.openimmunizationsoftware.pt.report.definition.ReportDefinition;
import org.openimmunizationsoftware.pt.report.definition.ReportGenerator;
import org.openimmunizationsoftware.pt.report.definition.ReportParameter;

public class GenerateReport {

  private Session dataSession = null;

  public GenerateReport(Session dataSession) {
    this.dataSession = dataSession;
  }

  public void execute() {
    Query query =
        dataSession.createQuery("from reportSchedule where status = ? AND period NOT LIKE 'C%'");
    query.setParameter(0, ReportSchedule.STATUS_QUEUED);

    List<ReportSchedule> reportSchedules = query.list();

    for (ReportSchedule schedule : reportSchedules) {
      if (schedule.canRun()) {
        try {
          generateReport(schedule);
        } catch (Exception e) {
          // TODO
        }
      }
    }
  }

  public void generateReport(ReportSchedule schedule) throws Exception {
    String originalStatus = schedule.getStatus();
    Transaction trans = dataSession.beginTransaction();
    try {
      schedule.setStatus(ReportSchedule.STATUS_RUNNING);
      dataSession.update(schedule);
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
      Date todayDate = new Date();
      String today = sdf.format(todayDate);
      int profileId = schedule.getProfileId();
      ReportProfile reportProfile =
          (ReportProfile) dataSession.get(ReportProfile.class, schedule.getProfileId());
      WebUser webuser = (WebUser) dataSession.get(WebUser.class, reportProfile.getUsername());
      ReportBatch reportBatch = new ReportBatch();
      reportBatch.setRunDate(today);
      reportBatch
          .setPeriod(schedule.getPeriod().length() > 1 ? schedule.getPeriod().substring(0, 1) : "");
      populateStartEndDates(reportBatch);
      reportBatch.setPreviousRunDate(schedule.getDateStart());
      reportBatch.setReportProfile(reportProfile);

      Map<String, String> paramaterValues = reportBatch.getParameterValues();
      ReportDefinition reportDefinition = reportProfile.getReportDefinition();
      List<ReportParameter> reportParameterList = reportDefinition.getReportParameters();
      for (ReportParameter reportParameter : reportParameterList) {
        String value = reportParameter.getValue(reportProfile, dataSession);
        paramaterValues.put(reportParameter.getName().toUpperCase(), value);
      }

      boolean sent = false;
      StringBuffer sbuf = new StringBuffer();
      Date reportDate = new Date();
      SimpleDateFormat sdfForFilename = new SimpleDateFormat("MM-dd-yyyy");
      String filename = reportProfile.getProfileLabel() + sdf.format(reportDate) + ".html";
      String dateDescription = "";
      if (schedule.isMethodEmail()) {
        StringWriter reportStringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(reportStringWriter);
        doHeader(out, reportProfile);
        ReportGenerator.generateReport(out, sbuf, paramaterValues, reportProfile.getReportText(),
            dataSession);
        dateDescription = "Emailed to " + schedule.getLocation();
        doFooter(out, reportProfile, dateDescription);

        MailManager mailManager = new MailManager(dataSession);

        mailManager.sendEmail(schedule.getName(), sbuf.toString(), schedule.getLocation());
      }
      sent = true;

      if (sent) {
        schedule.setStatus(originalStatus);
        schedule.setDateStart(todayDate);
      } else {
        schedule.setStatus(ReportSchedule.STATUS_STOPPED);
      }
      dataSession.update(schedule);

    } finally {
      trans.commit();
    }
  }

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

  public static void doFooter(PrintWriter out, ReportProfile reportProfile, String method) {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
    out.println("    <cite>");
    out.println("      " + reportProfile.getProfileLabel());
    out.println("      - " + method + " " + sdf.format(new Date()) + " ");
    out.println("    </cite>");
    out.println("  </body>");
    out.println("</html>");

  }

  public static void populateStartEndDates(ReportBatch reportBatch) throws Exception {
    SimpleDateFormat sdf = ReportBatch.createSimpleDateFormat();
    SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM yyyy");
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");

    Date runDate = sdf.parse(reportBatch.getRunDate());
    Calendar startDate = Calendar.getInstance();
    startDate.setTime(runDate);
    Calendar endDate = Calendar.getInstance();
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
      Calendar tempEndDate = Calendar.getInstance();
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
