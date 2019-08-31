package org.openimmunizationsoftware.pt.report.definition;

import java.util.ArrayList;
import java.util.List;
import org.openimmunizationsoftware.pt.model.ReportProfile;

public class ReportDefinitionPro implements ReportDefinition {

  private ReportProfile reportProfile = null;
  private List<ReportParameter> parameters = null;

  public ReportDefinitionPro(ReportProfile reportProfile) {
    this.reportProfile = reportProfile;
  }

  public ReportProfile getReportProfile() {
    return reportProfile;
  }

  public String getReportText() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getSelectorText() {
    // TODO Auto-generated method stub
    return null;
  }

  public List<ReportParameter> getReportParameters() throws Exception {
    if (parameters == null) {
      parameters = new ArrayList<ReportParameter>();
      ReportParameter parameter = new ReportParameter();
      parameter.setName("billBudgetId");
      parameter.setType(ReportParameter.TYPE_DROPDOWN);
      parameter.setHelp("Bill Budget id");
      parameter.setDefaultValue("0");
      parameter.setLabel("Bill Budget");
      parameter.setDropdownLink(ReportParameter.DROPDOWN_TRACK_BILL_BUDGET_ID);
      parameters.add(parameter);

    }
    return parameters;
  }

}
