package org.openimmunizationsoftware.pt.report;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.ReportSchedule;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ReportBatch {

  public static final String PARAMETER_PROVIDER_ID = "REPORT_PROVIDER_ID";
  public static final String PARAMETER_USER_ID = "REPORT_USER_ID";
  public static final String PARAMETER_PREVIOUS_RUN_DATE = "REPORT_PREVIOUS_RUN_DATE";
  public static final String PARAMETER_RUN_DATE = "REPORT_RUN_DATE";
  public static final String PARAMETER_START_DATE = "REPORT_START_DATE";
  public static final String PARAMETER_END_DATE = "REPORT_END_DATE";
  public static final String PARAMETER_PERIOD = "REPORT_PERIOD";
  public static final String PARAMETER_PERIOD_LABEL = "REPORT_PERIOD_LABEL";
  public static final String PARAMETER_DATE_RANGE_LABEL = "DATE_RANGE_LABEL";

  private int profileId = 0;
  private Map<String, String> parameterValues = new HashMap<String, String>();
  private WebUser webUser;

  public ReportBatch(WebUser webUser) {
    this.webUser = webUser;
    parameterValues.put(PARAMETER_RUN_DATE,
        webUser.getDateFormat("MM/dd/yyyy hh:mm:ss a").format(new Date()));
  }

  public void setReportProfile(ReportProfile reportProfile) {
    this.profileId = reportProfile.getProfileId();
    setProviderId(reportProfile.getProvider().getProviderId());
    setUserId(reportProfile.getUserId());
  }

  public String getEndDate() {
    return get(PARAMETER_END_DATE);
  }

  public void setEndDate(String endDate) {
    parameterValues.put(PARAMETER_END_DATE, endDate);
  }

  public String getPeriod() {
    return get(PARAMETER_PERIOD);
  }

  public void setPeriod(String period) {
    parameterValues.put(PARAMETER_PERIOD, period);
    if (isPeriodDaily()) {
      parameterValues.put(PARAMETER_PERIOD_LABEL, "Daily");
    } else if (isPeriodWeekly()) {
      parameterValues.put(PARAMETER_PERIOD_LABEL, "Weekly");
    } else if (isPeriodMonthly()) {
      parameterValues.put(PARAMETER_PERIOD_LABEL, "Monthly");
    } else if (isPeriodYearly()) {
      parameterValues.put(PARAMETER_PERIOD_LABEL, "Yearly");
    }
  }

  public int getProfileId() {
    return profileId;
  }

  public void setProfileId(int profileId) {
    this.profileId = profileId;
  }

  public String getProviderId() {
    return get(PARAMETER_PROVIDER_ID);
  }

  public void setProviderId(String providerId) {
    parameterValues.put(PARAMETER_PROVIDER_ID, providerId);
  }

  public String getRunDate() {
    return get(PARAMETER_RUN_DATE);
  }

  public void setRunDate(String runEndDate) {
    parameterValues.put(PARAMETER_RUN_DATE, runEndDate);
  }

  public String getPreviousRunDate() {
    return get(PARAMETER_PREVIOUS_RUN_DATE);
  }

  public void setPreviousRunDate(Date previousRunDate) {
    setPreviousRunDate(webUser.getDateFormat("MM/dd/yyyy hh:mm:ss a").format(previousRunDate));
  }

  public void setPreviousRunDate(String previousRunDate) {
    parameterValues.put(PARAMETER_PREVIOUS_RUN_DATE, previousRunDate);
  }

  public String getUserId() {
    return get(PARAMETER_USER_ID);
  }

  public void setUserId(String userId) {
    parameterValues.put(PARAMETER_USER_ID, userId);
  }

  public String getStartDate() {
    return get(PARAMETER_START_DATE);
  }

  public void setStartDate(String startDate) {
    parameterValues.put(PARAMETER_START_DATE, startDate);
  }

  public Map<String, String> getParameterValues() {
    return parameterValues;
  }

  private static String cleanNull(Object o) {
    if (o == null) {
      return "";
    }
    return (String) o;
  }

  private String get(String param) {
    return cleanNull(parameterValues.get(param));
  }

  public boolean isPeriodDaily() {
    return ReportSchedule.REPORT_DAILY.equals(getPeriod());
  }

  public boolean isPeriodWeekly() {
    return ReportSchedule.REPORT_WEEKLY.equals(getPeriod());
  }

  public boolean isPeriodMonthly() {
    return ReportSchedule.REPORT_MONTHLY.equals(getPeriod());
  }

  public boolean isPeriodYearly() {
    return ReportSchedule.REPORT_YEARLY.equals(getPeriod());
  }

  public void setDateRangeLabel(String dateRangeLabel) {
    parameterValues.put(PARAMETER_DATE_RANGE_LABEL, dateRangeLabel);
  }

}
