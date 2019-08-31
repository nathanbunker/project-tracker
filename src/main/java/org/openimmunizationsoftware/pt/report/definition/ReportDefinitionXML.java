package org.openimmunizationsoftware.pt.report.definition;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openimmunizationsoftware.pt.model.ReportProfile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReportDefinitionXML implements ReportDefinition {
  private String reportText;
  private String selectorText;
  private ReportProfile reportProfile;

  public ReportDefinitionXML(ReportProfile reportProfile) {
    this.reportProfile = reportProfile;
    if (reportProfile.getDefinition() == null) {
      selectorText = "";
      reportText = "";
    } else {
      selectorText = getXmlSection(SELECTOR_TAG_NAME, new String(reportProfile.getDefinition()));
      reportText = getXmlSection(REPORT_TAG_NAME, new String(reportProfile.getDefinition()));
    }
  }

  private static final String SELECTOR_TAG_NAME = "selector";
  private static final String REPORT_TAG_NAME = "report";

  private static String getXmlSection(String tagName, String xml) {
    int startPos = xml.indexOf("<" + tagName);
    if (startPos == -1) {
      throw new IllegalArgumentException("Unable to find start tag '" + tagName + "'");
    }
    int endPos = xml.indexOf("</" + tagName + ">", startPos);
    if (endPos == -1) {
      throw new IllegalArgumentException("Unable to find end tag '" + tagName + "'");
    }
    endPos += tagName.length() + 3;
    return xml.substring(startPos, endPos);
  }

  public ReportProfile getReportProfile() {
    return reportProfile;
  }

  public String getReportText() {
    return reportText;
  }

  public String getSelectorText() {
    return selectorText;
  }

  public List<ReportParameter> getReportParameters() throws Exception {
    if (selectorText.length() > 0) {
      return getParameters(new ByteArrayInputStream(selectorText.getBytes("UTF8")));
    }
    return new ArrayList<ReportParameter>();
  }

  private static List<ReportParameter> getParameters(InputStream xml) throws Exception {

    List<ReportParameter> parameters = new ArrayList<ReportParameter>();

    DocumentBuilderFactory factory;
    DocumentBuilder builder;
    factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setNamespaceAware(true);
    builder = factory.newDocumentBuilder();
    Document document = builder.parse(xml);
    Node selectorNode = document.getFirstChild();
    if (selectorNode == null || !selectorNode.getNodeName().equals("selector")) {
      throw new Exception("No selector node found");
    }
    NodeList parameterNodes = selectorNode.getChildNodes();
    for (int pi = 0; pi < parameterNodes.getLength(); pi++) {
      Node parameterNode = parameterNodes.item(pi);
      if (parameterNode.getNodeName().equals("parameter")) {
        ReportParameter parameter = new ReportParameter();
        Element eElement = (Element) parameterNode;
        parameter.setLabel(eElement.getAttribute("label"));
        parameter.setName(eElement.getAttribute("name"));
        parameter.setType(eElement.getAttribute("type").toUpperCase());
        parameter.setHelp(eElement.getAttribute("help"));
        String maxLength = eElement.getAttribute("maxLength");
        if (maxLength != null && !maxLength.equals("")) {
          parameter.setMaxLength(Integer.parseInt(maxLength));
        }
        String displayLength = eElement.getAttribute("displayLength");
        if (displayLength != null && !displayLength.equals("")) {
          parameter.setDisplayLength(Integer.parseInt(displayLength));
        }
        parameter.setDefaultValue(eElement.getAttribute("defaultValue"));
        parameter.setDropdownLink(eElement.getAttribute("dropdownLink"));
        parameters.add(parameter);
      }
    }
    return parameters;
  }

}
