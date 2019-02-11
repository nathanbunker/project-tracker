package org.openimmunizationsoftware.pt.report.definition;

import java.util.List;

import org.openimmunizationsoftware.pt.model.ReportProfile;

public interface ReportDefinition
{
  public ReportProfile getReportProfile();

  public String getReportText();

  public String getSelectorText();

  public List<ReportParameter> getReportParameters() throws Exception;

}
