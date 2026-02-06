package org.openimmunizationsoftware.pt.report.definition;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.manager.TrackerKeysManager;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.ProjectCategory;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.openimmunizationsoftware.pt.model.WebUser;

public class ReportParameter {
  public static final String TYPE_STRING = "STRING";
  public static final String TYPE_INT = "INT";
  public static final String TYPE_DATE = "DATE";
  public static final String TYPE_CHECKBOX = "CHECKBOX";
  public static final String TYPE_DROPDOWN = "DROPDOWN";

  public static final String DROPDOWN_PROJECT_BILLCODE = "Project billCodes";
  public static final String DROPDOWN_PROJECT_CATEGORY = "Project projectCategory";
  public static final String DROPDOWN_TRACK_BILL_BUDGET_ID = "Track billBudgetId";

  private String label = "";
  private String name = "";

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHelp() {
    return help;
  }

  public void setHelp(String help) {
    this.help = help;
  }

  public boolean isPutLabelInValue() {
    return putLabelInValue;
  }

  public void setPutLabelInValue(boolean putLabelInValue) {
    this.putLabelInValue = putLabelInValue;
  }

  private String type = "";
  private String help = "";
  private int maxLength = 0;
  private int displayLength = 0;
  private String defaultValue = "";
  private String dropdownLink = "";
  private boolean putLabelInValue = false;

  public String getType() {
    return type;
  }

  public void setType(String dataType) {
    this.type = dataType;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public int getDisplayLength() {
    return displayLength;
  }

  public void setDisplayLength(int displayLength) {
    this.displayLength = displayLength;
  }

  public String getDropdownLink() {
    return dropdownLink;
  }

  public void setDropdownLink(String dropdownLink) {
    this.dropdownLink = dropdownLink;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  public String getValue(ReportProfile profile, Session dataSession) {
    return TrackerKeysManager.getReportKeyValue(name, defaultValue, profile, dataSession);
  }

  public String toHtml(ReportProfile profile, AppReq appReq) throws Exception {
    WebUser webUser = appReq.getWebUser();
    Session dataSession = appReq.getDataSession();

    StringBuilder sbuf = new StringBuilder();
    if (type.equals(TYPE_INT)) {
      int value = TrackerKeysManager.getReportKeyValueInt(name, defaultValue, profile, dataSession);
      return createTextInput("" + value);
    } else if (type.equals(TYPE_CHECKBOX)) {
      boolean value = TrackerKeysManager.getReportKeyValueBoolean(name, defaultValue, profile, dataSession);
      return "<input type=\"checkbox\" name=\"" + name + "\" value=\"T\""
          + (value ? " checked" : "") + "/>";
    } else if (type.equals(TYPE_DATE)) {
      SimpleDateFormat sdf = webUser.getTimeFormat();
      Date value = TrackerKeysManager.getReportKeyValueDateTime(name, defaultValue, profile, dataSession);
      return createTextInput(sdf.format(value));
    } else if (type.equals(TYPE_DROPDOWN)) {
      String value = TrackerKeysManager.getReportKeyValue(name, defaultValue, profile, dataSession);
      if (dropdownLink.equals(DROPDOWN_PROJECT_BILLCODE)) {
        sbuf.append("<select name=\"" + name + "\">");
        Query query = dataSession.createQuery(
            "from BillCode where provider = :provider and visible = 'Y' order by billLabel");
        query.setParameter("provider", webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<BillCode> billCodeList = query.list();
        for (BillCode billCode : billCodeList) {
          if (billCode.getBillCode().equals(value)) {
            sbuf.append("<option value=\"" + billCode.getBillCode() + "\" selected>"
                + billCode.getBillLabel() + "</option>");
          } else {
            sbuf.append("<option value=\"" + billCode.getBillCode() + "\">"
                + billCode.getBillLabel() + "</option>");
          }
        }
        sbuf.append("</select>");
      } else if (dropdownLink.equals(DROPDOWN_PROJECT_CATEGORY)) {
        sbuf.append("<select name=\"" + name + "\">");
        Query query = dataSession.createQuery(
            "from ProjectCategory where provider = provider order by sortOrder, clientName");
        query.setParameter("provider", webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<ProjectCategory> projectCategoryList = query.list();
        for (ProjectCategory projectCategory : projectCategoryList) {
          if (projectCategory.getCategoryCode().equals(value)) {
            sbuf.append("<option value=\"" + projectCategory.getCategoryCode() + "\" selected>"
                + projectCategory.getClientNameForDropdown() + "</option>");
          } else {
            sbuf.append("<option value=\"" + projectCategory.getCategoryCode() + "\">"
                + projectCategory.getClientNameForDropdown() + "</option>");
          }
        }
        sbuf.append("</select>");
      } else if (dropdownLink.equals(DROPDOWN_TRACK_BILL_BUDGET_ID)) {
        sbuf.append("<select name=\"" + name + "\">");
        Query query = dataSession.createQuery(
            "from BillBudget where billCode.provider = :provider and billCode.visible = 'Y' "
                + "order by billCode.billLabel, billBudgetCode");
        query.setParameter("provider", webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<BillBudget> billBudgetList = query.list();
        for (BillBudget billBudget : billBudgetList) {
          if (String.valueOf(billBudget.getBillBudgetId()).equals(value)) {
            sbuf.append("<option value=\"" + billBudget.getBillBudgetId() + "\" selected>"
                + billBudget.getBillCode().getBillLabel() + ": " + billBudget.getBillBudgetCode()
                + "</option>");
          } else {
            sbuf.append("<option value=\"" + billBudget.getBillBudgetId() + "\">"
                + billBudget.getBillCode().getBillLabel() + ": " + billBudget.getBillBudgetCode()
                + "</option>");
          }
        }
        sbuf.append("</select>");
      } else {
        return createTextInput(value) + " [dropDown '" + dropdownLink + "' not recognized]";
      }

      sbuf.append("</select>");
      return sbuf.toString();
    } else {
      String value = TrackerKeysManager.getReportKeyValue(name, defaultValue, profile, dataSession);
      return createTextInput(value);
    }

  }

  private String createTextInput(String value) {
    String input = "<input type=\"text\" name=\"" + name + "\" value=\"" + value + "\"";
    if (displayLength > 0) {
      input += " size=\"" + displayLength + "\"";
    }
    if (maxLength > 0) {
      input += " maxlength=\"" + maxLength + "\"";
    }
    return input + "/>";
  }

}
