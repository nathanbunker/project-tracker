package org.dandeliondaily.dashboard.render;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.dandeliondaily.dashboard.model.DashboardNowColumnModel;
import org.dandeliondaily.dashboard.model.DashboardNextColumnModel;
import org.dandeliondaily.dashboard.model.DashboardTodayColumnModel;
import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.dandeliondaily.dashboard.model.TimeGaugeState;
import org.dandeliondaily.dashboard.model.TimeGaugeVariant;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ProjectNextActionType;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

public class DashboardPageRenderer {

    // Temporary layout aid: keep true while iterating dashboard structure.
    // Set to false to disable all developer panel labels in one place.
    private static final boolean DEV_LABELS_ENABLED = true;
    private static final int TODAY_GAUGE_WARNING_PERCENT = 85;
    private static final String TODAY_SECTION_PARAM = "todaySection";
    private static final String TODAY_SECTION_ALL = "all";
    private static final String TODAY_SECTION_SESSION_KEY = "DASHBOARD_TODAY_SECTION_FILTER";

    private final TimeGaugeRenderer timeGaugeRenderer = new TimeGaugeRenderer();

    public void render(AppReq appReq, DashboardNowColumnModel nowColumnModel,
            DashboardTodayColumnModel todayColumnModel,
            DashboardNextColumnModel nextColumnModel,
            TimeGaugeModel nowGaugeModel,
            TimeGaugeModel todayGaugeModel) {
        PrintWriter out = appReq.getOut();
        String rootClass = "dd-dashboard-page" + (DEV_LABELS_ENABLED ? " dd-dashboard-dev-labels-enabled" : "");

        printStyles(out);

        out.println("<div class=\"" + rootClass + "\">");
        out.println("  <div class=\"dd-dashboard-intro\">");
        out.println("    <h1>Dandelion Dashboard</h1>");
        String overClass = todayColumnModel.getTotals().isOverCommitted() ? " dd-over-under-over" : "";
        out.println("    <p class=\"dd-over-under" + overClass + "\">"
                + escapeHtml(todayColumnModel.getTotals().getGuidanceMessage()) + "</p>");
        out.println("  </div>");

        out.println("  <div class=\"dd-dashboard-shell\">");
        out.println("    <div class=\"dd-dashboard-header\">");
        out.println("      <div class=\"dd-header-cell dd-header-now dd-panel dd-panel-open\">");
        printDevLabel(out, "NOW HEADER");
        out.println("        <div class=\"dd-header-main\">");
        out.println("          <div class=\"dd-header-text\">");
        out.println("            <div class=\"dd-header-label\">" + escapeHtml(buildNowHeaderLine(nowColumnModel))
                + "</div>");
        out.println("            <div class=\"dd-header-subtitle\">" + escapeHtml(buildNowHeaderProject(nowColumnModel))
                + "</div>");
        out.println("          </div>");
        out.println("          <div class=\"dd-header-gauge-wrap\">");
        out.println("            <div id=\"ddNowHeaderGauge\">");
        timeGaugeRenderer.render(out, nowGaugeModel);
        out.println("            </div>");
        out.println("          </div>");
        out.println("        </div>");
        out.println("      </div>");
        out.println("      <div class=\"dd-header-cell dd-header-today dd-panel dd-panel-open\">");
        printDevLabel(out, "TODAY HEADER");
        out.println("        <div class=\"dd-header-main\">");
        out.println("          <div class=\"dd-header-text\">");
        out.println(
                "            <div class=\"dd-header-label\">" + escapeHtml(buildTodayHeaderLabel(appReq)) + "</div>");
        out.println("            <div class=\"dd-header-subtitle\" id=\"ddTodayHeaderCurrentTime\">"
                + escapeHtml(buildTodayHeaderCurrentTime(appReq)) + "</div>");
        out.println("          </div>");
        out.println("          <div class=\"dd-header-gauge-wrap\">");
        out.println("            <div id=\"ddTodayHeaderGauge\">");
        timeGaugeRenderer.render(out, todayGaugeModel);
        out.println("            </div>");
        out.println("          </div>");
        out.println("        </div>");
        out.println("      </div>");
        out.println("      <div class=\"dd-header-cell dd-header-next dd-panel dd-panel-open\">");
        printDevLabel(out, "NEXT HEADER");
        String nextHeaderLabel = nextColumnModel.getSelectedDay().getDayKey().length() > 0
                ? nextColumnModel.getSelectedDay().getFullDateLabel()
                : "Next";
        out.println("        <div class=\"dd-header-main\">");
        out.println("          <div class=\"dd-header-text\">");
        out.println("            <div class=\"dd-header-label\">"
                + escapeHtml(nextHeaderLabel) + "</div>");
        String nextHeaderSubtitle = nextColumnModel.getSelectedDay().getDayKey().length() > 0
                ? nextColumnModel.getSelectedDay().getPlannedDisplay() + " planned"
                : "No planned day selected";
        out.println("            <div class=\"dd-header-subtitle\">" + escapeHtml(nextHeaderSubtitle) + "</div>");
        out.println("          </div>");
        out.println("          <div class=\"dd-header-gauge-wrap\">");
        out.println("            <div id=\"ddNextHeaderGauge\">");
        timeGaugeRenderer.render(out, nextColumnModel.getSelectedDay().getHeaderGauge());
        out.println("            </div>");
        out.println("          </div>");
        out.println("        </div>");
        out.println("      </div>");
        out.println("    </div>");

        out.println("    <div class=\"dd-dashboard-columns\">");
        out.println("      <div class=\"dd-dashboard-column dd-dashboard-column-now\">");
        out.println("        <!-- Real data wiring starts here for the dashboard now column. -->");
        printNowColumn(out, nowColumnModel);
        out.println("      </div>");
        out.println("      <div class=\"dd-dashboard-column dd-dashboard-column-today\">");
        out.println("        <!-- Real data wiring starts here for the dashboard today column. -->");
        printTodayColumn(out, todayColumnModel, appReq);
        out.println("      </div>");
        out.println("      <div class=\"dd-dashboard-column dd-dashboard-column-next\">");
        out.println("        <!-- Real data wiring starts here for the dashboard next column. -->");
        printNextColumn(out, nextColumnModel);
        out.println("      </div>");
        out.println("    </div>");
        out.println("  </div>");
        printHeaderGaugeAutoRefreshScript(out);
        out.println("</div>");
    }

    private void printHeaderGaugeAutoRefreshScript(PrintWriter out) {
        out.println("<script>");
        out.println("  (function() {");
        out.println("    function ddRefreshHeaderGauges() {");
        out.println("      var body = 'action=refreshHeaderGauges';");
        out.println("      fetch('DandelionDashboardServlet', {");
        out.println("        method: 'POST',");
        out.println("        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },");
        out.println("        body: body");
        out.println("      })");
        out.println("      .then(function(response) { return response.json(); })");
        out.println("      .then(function(data) {");
        out.println("        if (!data || !data.success) {");
        out.println("          return;");
        out.println("        }");
        out.println("        var nowContainer = document.getElementById('ddNowHeaderGauge');");
        out.println("        if (nowContainer && data.nowGaugeHtml) {");
        out.println("          nowContainer.innerHTML = data.nowGaugeHtml;");
        out.println("        }");
        out.println("        var todayContainer = document.getElementById('ddTodayHeaderGauge');");
        out.println("        if (todayContainer && data.todayGaugeHtml) {");
        out.println("          todayContainer.innerHTML = data.todayGaugeHtml;");
        out.println("        }");
        out.println("        var todayTime = document.getElementById('ddTodayHeaderCurrentTime');");
        out.println("        if (todayTime && data.todayCurrentTime) {");
        out.println("          todayTime.textContent = data.todayCurrentTime;");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(function(err) {");
        out.println("        console.log('Header gauge refresh failed:', err);");
        out.println("      });");
        out.println("    }");
        out.println("    // Keep gauges and active billing minutes current without full page refresh.");
        out.println("    window.setInterval(ddRefreshHeaderGauges, 60000);");
        out.println("  })();");
        out.println("</script>");
    }

    private void printStyles(PrintWriter out) {
        out.println("<style>");
        out.println("  .dd-dashboard-page {");
        out.println("    padding: 12px 18px 24px 18px;");
        out.println("    background: linear-gradient(180deg, #f4f0e8 0%, #efe7db 40%, #f8f6f1 100%);");
        out.println("    min-height: calc(100vh - 70px);");
        out.println("    box-sizing: border-box;");
        out.println("  }");
        out.println("  .dd-dashboard-intro {");
        out.println("    margin-bottom: 12px;");
        out.println("  }");
        out.println("  .dd-dashboard-intro h1 {");
        out.println("    margin: 0 0 6px 0;");
        out.println("    font-size: 28px;");
        out.println("    color: #2d3a2d;");
        out.println("  }");
        out.println("  .dd-dashboard-intro p {");
        out.println("    margin: 4px 0;");
        out.println("  }");
        out.println("  .dd-dashboard-shell {");
        out.println("    border: 1px solid #cbbda7;");
        out.println("    background: rgba(255, 252, 247, 0.92);");
        out.println("    box-shadow: 0 10px 30px rgba(94, 77, 58, 0.12);");
        out.println("  }");
        // Developer labels are temporary and intentionally easy to disable.
        out.println("  .dd-panel {");
        out.println("    position: relative;");
        out.println("  }");
        out.println("  .dd-dashboard-dev-label {");
        out.println("    position: absolute;");
        out.println("    top: 4px;");
        out.println("    right: 6px;");
        out.println("    font-size: 9px;");
        out.println("    line-height: 1;");
        out.println("    font-weight: normal;");
        out.println("    letter-spacing: 0.08em;");
        out.println("    text-transform: uppercase;");
        out.println("    color: rgba(80, 80, 80, 0.52);");
        out.println("    pointer-events: none;");
        out.println("    z-index: 2;");
        out.println("  }");
        out.println("  .dd-panel-open {");
        out.println("    border: none;");
        out.println("    background: transparent;");
        out.println("    box-shadow: none;");
        out.println("  }");
        out.println("  .dd-panel-boxed {");
        out.println("    border: 1px solid #cfbea6;");
        out.println("    background: #fffdf8;");
        out.println("    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);");
        out.println("  }");
        out.println("  .dd-dashboard-header {");
        out.println("    position: sticky;");
        out.println("    top: 0;");
        out.println("    z-index: 20;");
        out.println("    display: grid;");
        out.println("    grid-template-columns: 25% 45% 30%;");
        out.println("    background: linear-gradient(90deg, #3c5341 0%, #5b735c 45%, #7a8f70 100%);");
        out.println("    color: #fffdf8;");
        out.println("    border-bottom: 1px solid #b49f82;");
        out.println("  }");
        out.println("  .dd-header-cell {");
        out.println("    padding: 16px 18px;");
        out.println("    font-size: 20px;");
        out.println("    font-weight: bold;");
        out.println("    letter-spacing: 0.04em;");
        out.println("    text-transform: uppercase;");
        out.println("    display: block;");
        out.println("  }");
        out.println("  .dd-header-main {");
        out.println("    display: flex;");
        out.println("    align-items: flex-start;");
        out.println("    gap: 12px;");
        out.println("  }");
        out.println("  .dd-header-text {");
        out.println("    display: flex;");
        out.println("    flex-direction: column;");
        out.println("    gap: 4px;");
        out.println("    min-width: 0;");
        out.println("    flex: 1;");
        out.println("  }");
        out.println("  .dd-header-label {");
        out.println("    line-height: 1;");
        out.println("  }");
        out.println("  .dd-header-now .dd-header-label {");
        out.println("    text-transform: none;");
        out.println("    letter-spacing: normal;");
        out.println("    font-size: 16px;");
        out.println("  }");
        out.println("  .dd-header-today .dd-header-label {");
        out.println("    text-transform: none;");
        out.println("    letter-spacing: normal;");
        out.println("    font-size: 16px;");
        out.println("  }");
        out.println("  .dd-header-next .dd-header-label {");
        out.println("    text-transform: none;");
        out.println("    letter-spacing: normal;");
        out.println("    font-size: 16px;");
        out.println("  }");
        out.println("  .dd-header-subtitle {");
        out.println("    font-size: 12px;");
        out.println("    font-weight: normal;");
        out.println("    text-transform: none;");
        out.println("    letter-spacing: normal;");
        out.println("    opacity: 0.9;");
        out.println("  }");
        out.println("  .dd-header-gauge-wrap {");
        out.println("    width: 210px;");
        out.println("    flex: 0 0 210px;");
        out.println("  }");
        out.println("  .dd-header-today .dd-header-gauge-wrap {");
        out.println("    flex: 1;");
        out.println("    width: auto;");
        out.println("    min-width: 0;");
        out.println("    overflow: hidden;");
        out.println("  }");
        out.println("  .dd-dashboard-columns {");
        out.println("    display: grid;");
        out.println("    grid-template-columns: 25% 45% 30%;");
        out.println("    align-items: start;");
        out.println("  }");
        out.println("  .dd-dashboard-column {");
        out.println("    min-height: 700px;");
        out.println("    padding: 16px;");
        out.println("    border-right: 1px solid #ddd0bd;");
        out.println("    box-sizing: border-box;");
        out.println("  }");
        out.println("  .dd-dashboard-column-next {");
        out.println("    border-right: none;");
        out.println("  }");
        out.println("  .dd-now-action-wrap {");
        out.println("    margin-bottom: 16px;");
        out.println("    padding: 0;");
        out.println("  }");
        // Top-level section boxes are intentionally reduced to lower visual noise while
        // layout structure is refined.
        out.println("  .dd-section {");
        out.println("    margin-bottom: 16px;");
        out.println("    padding: 4px 0 0 0;");
        out.println("  }");
        out.println("  .dd-section h2 {");
        out.println("    margin: 0 0 10px 0;");
        out.println("    font-size: 18px;");
        out.println("    color: #334235;");
        out.println("  }");
        out.println("  .dd-section p {");
        out.println("    margin: 0 0 10px 0;");
        out.println("    line-height: 1.4;");
        out.println("  }");
        out.println("  .dd-section ul {");
        out.println("    margin: 0;");
        out.println("    padding-left: 18px;");
        out.println("  }");
        out.println("  .dd-section li {");
        out.println("    margin-bottom: 8px;");
        out.println("  }");
        out.println("  .dd-emphasis {");
        out.println("    display: block;");
        out.println("    font-size: 22px;");
        out.println("    color: #3d4f41;");
        out.println("    margin-bottom: 6px;");
        out.println("  }");
        out.println("  .dd-capture-box {");
        out.println("    min-height: 90px;");
        out.println("    padding: 12px;");
        out.println("    border: 1px dashed #b89a72;");
        out.println("    background: #fcf7ee;");
        out.println("    color: #6a5a42;");
        out.println("  }");
        out.println("  .dd-planning-table {");
        out.println("    width: 100%;");
        out.println("    border-collapse: collapse;");
        out.println("    margin-top: 8px;");
        out.println("  }");
        out.println("  .dd-planning-table th,");
        out.println("  .dd-planning-table td {");
        out.println("    border: 1px solid #d9ccb8;");
        out.println("    padding: 8px;");
        out.println("    text-align: left;");
        out.println("  }");
        out.println("  .dd-planning-table th {");
        out.println("    background: #f2eadf;");
        out.println("  }");
        out.println("  .dd-status-chip {");
        out.println("    display: inline-block;");
        out.println("    padding: 2px 8px;");
        out.println("    border-radius: 12px;");
        out.println("    background: #dde9d8;");
        out.println("    color: #29402c;");
        out.println("    font-size: 12px;");
        out.println("    font-weight: bold;");
        out.println("  }");
        out.println("  .dd-meta-row {");
        out.println("    display: flex;");
        out.println("    flex-wrap: wrap;");
        out.println("    gap: 8px;");
        out.println("    margin-bottom: 10px;");
        out.println("  }");
        out.println("  .dd-meta-pill {");
        out.println("    display: inline-block;");
        out.println("    padding: 4px 8px;");
        out.println("    border: 1px solid #d6c7b1;");
        out.println("    background: #f6efe4;");
        out.println("    color: #5d4b34;");
        out.println("    font-size: 12px;");
        out.println("  }");
        out.println("  .dd-subtle {");
        out.println("    color: #6b6256;");
        out.println("  }");
        out.println("  .dd-backlog-list {");
        out.println("    list-style: none;");
        out.println("    padding-left: 0;");
        out.println("    margin: 0;");
        out.println("  }");
        out.println("  .dd-backlog-item {");
        out.println("    margin-bottom: 10px;");
        out.println("    padding: 10px;");
        out.println("    border: 1px solid #e0d4c3;");
        out.println("    background: #fcfaf6;");
        out.println("  }");
        out.println("  .dd-backlog-item-current {");
        out.println("    border-color: #9ab288;");
        out.println("    background: #f5faef;");
        out.println("  }");
        out.println("  .dd-backlog-item p {");
        out.println("    margin: 0 0 6px 0;");
        out.println("  }");
        out.println("  .dd-backlog-section-title {");
        out.println("    margin: 14px 0 4px 0;");
        out.println("    font-size: 12px;");
        out.println("    text-transform: uppercase;");
        out.println("    letter-spacing: 0.04em;");
        out.println("    color: #4e5d50;");
        out.println("  }");
        out.println("  .dd-backlog-table {");
        out.println("    margin-bottom: 8px;");
        out.println("  }");
        out.println("  .dd-backlog-empty {");
        out.println("    margin: 2px 0 10px 0;");
        out.println("    font-size: 12px;");
        out.println("  }");
        out.println("  .dd-backlog-col-date {");
        out.println("    width: 90px;");
        out.println("    white-space: nowrap;");
        out.println("  }");
        out.println("  .dd-backlog-col-desc {");
        out.println("  }");
        out.println("  .dd-backlog-col-action {");
        out.println("    width: 28px;");
        out.println("    text-align: center;");
        out.println("  }");
        out.println("  .dd-backlog-row-current td {");
        out.println("    background: #f0f6eb;");
        out.println("  }");
        out.println("  tr.dd-row-clickable:hover td {");
        out.println("    background-color: #eee8da;");
        out.println("    cursor: pointer;");
        out.println("  }");
        out.println("  tr.dd-row-clickable.dd-backlog-row-current:hover td {");
        out.println("    background-color: #ddebd0;");
        out.println("  }");
        out.println("  .dd-capture-form input[type='text'] {");
        out.println("    width: 100%;");
        out.println("    box-sizing: border-box;");
        out.println("    padding: 8px; ");
        out.println("    margin-bottom: 0;");
        out.println("    border: 1px solid #d3c2aa;");
        out.println("    background: #fffaf1;");
        out.println("  }");
        out.println("  .dd-capture-form {");
        out.println("    margin: 0;");
        out.println("  }");
        out.println("  .dd-capture-row {");
        out.println("    display: flex;");
        out.println("    align-items: center;");
        out.println("    gap: 8px;");
        out.println("  }");
        out.println("  .dd-capture-input-container {");
        out.println("    position: relative;");
        out.println("    flex: 1;");
        out.println("  }");
        out.println("  .dd-capture-suggestions {");
        out.println("    position: absolute;");
        out.println("    left: 0;");
        out.println("    right: 0;");
        out.println("    top: calc(100% + 2px);");
        out.println("    z-index: 40;");
        out.println("    display: none;");
        out.println("    background: #fffdf8;");
        out.println("    border: 1px solid #d9ccb8;");
        out.println("    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);");
        out.println("    max-height: 220px;");
        out.println("    overflow-y: auto;");
        out.println("  }");
        out.println("  .dd-capture-suggestions div {");
        out.println("    padding: 6px 8px;");
        out.println("    cursor: pointer;");
        out.println("  }");
        out.println("  .dd-capture-suggestions div:hover {");
        out.println("    background: #efe7db;");
        out.println("  }");
        out.println("  .dd-capture-actions {");
        out.println("    display: flex;");
        out.println("    gap: 8px;");
        out.println("    flex-wrap: nowrap;");
        out.println("    margin: 0;");
        out.println("  }");
        out.println("  .dd-capture-actions input[type='submit'] {");
        out.println("    border: 1px solid #6d7f63;");
        out.println("    background: #eef5ea;");
        out.println("    padding: 6px 10px;");
        out.println("    cursor: pointer;");
        out.println("  }");
        out.println("  .dd-group-title {");
        out.println("    margin: 0 0 8px 0;");
        out.println("    font-size: 14px;");
        out.println("    text-transform: uppercase;");
        out.println("    letter-spacing: 0.03em;");
        out.println("    color: #56664d;");
        out.println("  }");
        out.println("  .dd-action-list {");
        out.println("    list-style: none;");
        out.println("    margin: 0;");
        out.println("    padding-left: 0;");
        out.println("  }");
        out.println("  .dd-action-item {");
        out.println("    border: 1px solid #e2d7c8;");
        out.println("    background: #fffdf9;");
        out.println("    padding: 8px;");
        out.println("    margin-bottom: 8px;");
        out.println("  }");
        out.println("  .dd-action-project {");
        out.println("    font-weight: bold;");
        out.println("    margin-bottom: 3px;");
        out.println("  }");
        out.println("  .dd-action-desc {");
        out.println("    margin-bottom: 6px;");
        out.println("  }");
        out.println("  .dd-totals-grid {");
        out.println("    display: grid;");
        out.println("    grid-template-columns: repeat(2, minmax(120px, 1fr));");
        out.println("    gap: 6px;");
        out.println("    margin-bottom: 8px;");
        out.println("  }");
        out.println("  .dd-totals-cell {");
        out.println("    border: 1px solid #ddceb7;");
        out.println("    background: #f8f2e8;");
        out.println("    padding: 6px 8px;");
        out.println("  }");
        out.println("  .dd-today-chips {");
        out.println("    display: grid;");
        out.println("    grid-template-columns: repeat(3, minmax(0, 1fr));");
        out.println("    gap: 6px;");
        out.println("    margin: 4px 0 14px 0;");
        out.println("  }");
        out.println("  .dd-today-chip {");
        out.println("    display: flex;");
        out.println("    justify-content: space-between;");
        out.println("    align-items: center;");
        out.println("    gap: 8px;");
        out.println("    border: 1px solid #d7ccb9;");
        out.println("    background: #faf6ef;");
        out.println("    color: #3d4f41;");
        out.println("    text-decoration: none;");
        out.println("    font-size: 12px;");
        out.println("    padding: 6px 8px;");
        out.println("    border-radius: 4px;");
        out.println("  }");
        out.println("  .dd-today-chip:hover {");
        out.println("    background: #f0e8dd;");
        out.println("  }");
        out.println("  .dd-today-chip-empty {");
        out.println("    color: #8e897f;");
        out.println("    background: #f5f2ed;");
        out.println("    border-color: #e0d8cc;");
        out.println("  }");
        out.println("  .dd-today-chip-alert {");
        out.println("    color: #fff8f5;");
        out.println("    background: #b5483f;");
        out.println("    border-color: #8f2f2f;");
        out.println("  }");
        out.println("  .dd-today-chip-alert:hover {");
        out.println("    background: #9f3d35;");
        out.println("  }");
        out.println("  .dd-today-chip-count {");
        out.println("    font-weight: bold;");
        out.println("    font-size: 11px;");
        out.println("    min-width: 18px;");
        out.println("    text-align: right;");
        out.println("  }");
        out.println("  .dd-today-show-all-wrap {");
        out.println("    margin: 8px 0 2px 0;");
        out.println("  }");
        out.println("  .dd-today-show-all-link {");
        out.println("    color: #2d4a2f;");
        out.println("    text-decoration: none;");
        out.println("    font-size: 12px;");
        out.println("    font-weight: bold;");
        out.println("  }");
        out.println("  .dd-today-show-all-link:hover {");
        out.println("    text-decoration: underline;");
        out.println("  }");
        out.println("  .dd-next-day-chips {");
        out.println("    display: grid;");
        out.println("    grid-template-columns: repeat(4, minmax(0, 1fr));");
        out.println("    gap: 6px;");
        out.println("    margin: 4px 0 14px 0;");
        out.println("  }");
        out.println("  .dd-next-day-chip {");
        out.println("    display: flex;");
        out.println("    flex-direction: column;");
        out.println("    gap: 2px;");
        out.println("    border: 1px solid #d7ccb9;");
        out.println("    background: #faf6ef;");
        out.println("    color: #3d4f41;");
        out.println("    text-decoration: none;");
        out.println("    padding: 6px 8px;");
        out.println("    border-radius: 4px;");
        out.println("    min-height: 42px;");
        out.println("  }");
        out.println("  .dd-next-day-chip:hover {");
        out.println("    background: #f0e8dd;");
        out.println("  }");
        out.println("  .dd-next-day-chip-selected {");
        out.println("    border-color: #6b8c72;");
        out.println("    background: #ecf3ed;");
        out.println("  }");
        out.println("  .dd-next-day-chip-title {");
        out.println("    font-size: 13px;");
        out.println("    font-weight: bold;");
        out.println("    line-height: 1.2;");
        out.println("  }");
        out.println("  .dd-next-day-chip-date {");
        out.println("    font-size: 11px;");
        out.println("    color: #7a7060;");
        out.println("  }");
        out.println("  .dd-today-section {");
        out.println("    margin: 0 0 18px 0;");
        out.println("    padding: 0;");
        out.println("  }");
        out.println("  .dd-today-section-title {");
        out.println("    margin: 0 0 6px 0;");
        out.println("    font-size: 13px;");
        out.println("    text-transform: uppercase;");
        out.println("    letter-spacing: 0.04em;");
        out.println("    color: #4e5d50;");
        out.println("  }");
        out.println("  .dd-today-section-count {");
        out.println("    color: #8d8374;");
        out.println("    font-size: 11px;");
        out.println("    margin-left: 6px;");
        out.println("  }");
        out.println("  .dd-today-table {");
        out.println("    width: 100%;");
        out.println("    border-collapse: collapse;");
        out.println("    table-layout: fixed;");
        out.println("  }");
        out.println("  .dd-today-table th {");
        out.println("    text-align: left;");
        out.println("    font-size: 11px;");
        out.println("    text-transform: uppercase;");
        out.println("    letter-spacing: 0.03em;");
        out.println("    color: #6f6659;");
        out.println("    padding: 4px 6px;");
        out.println("    border-bottom: 1px solid #d8cdbb;");
        out.println("  }");
        out.println("  .dd-today-table td {");
        out.println("    padding: 4px 6px;");
        out.println("    border-bottom: 1px solid #ebe2d4;");
        out.println("    vertical-align: top;");
        out.println("    font-size: 12px;");
        out.println("  }");
        out.println("  .dd-today-cell-action-main {");
        out.println("    width: 42%;");
        out.println("  }");
        out.println("  .dd-today-cell-project {");
        out.println("    width: 20%;");
        out.println("    color: #756b5e;");
        out.println("  }");
        out.println("  .dd-today-cell-gauge {");
        out.println("    width: 30%;");
        out.println("  }");
        out.println("  .dd-today-cell-time-slot {");
        out.println("    width: 30%;");
        out.println("    color: #6f6659;");
        out.println("    white-space: nowrap;");
        out.println("  }");
        out.println("  .dd-today-cell-action-placeholder {");
        out.println("    width: 8%;");
        out.println("  }");
        out.println("  .dd-row-actions {");
        out.println("    display: inline-flex;");
        out.println("    align-items: center;");
        out.println("    gap: 4px;");
        out.println("    white-space: nowrap;");
        out.println("  }");
        out.println("  .dd-row-action-btn {");
        out.println("    background: none;");
        out.println("    border: none;");
        out.println("    padding: 0;");
        out.println("    margin: 0;");
        out.println("    font-size: 12px;");
        out.println("    line-height: 1;");
        out.println("    cursor: pointer;");
        out.println("    opacity: 0.8;");
        out.println("  }");
        out.println("  .dd-row-action-btn:hover {");
        out.println("    opacity: 1;");
        out.println("  }");
        out.println("  .dd-modal-overlay {");
        out.println("    position: fixed;");
        out.println("    inset: 0;");
        out.println("    background: rgba(0, 0, 0, 0.45);");
        out.println("    display: none;");
        out.println("    align-items: center;");
        out.println("    justify-content: center;");
        out.println("    z-index: 12000;");
        out.println("  }");
        out.println("  .dd-modal-overlay.dd-modal-open {");
        out.println("    display: flex;");
        out.println("  }");
        out.println("  .dd-modal {");
        out.println("    width: min(420px, calc(100vw - 40px));");
        out.println("    background: #ffffff;");
        out.println("    border-radius: 6px;");
        out.println("    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.2);");
        out.println("    border: 1px solid #e4dbcd;");
        out.println("    padding: 12px 14px 14px 14px;");
        out.println("  }");
        out.println("  .dd-modal-head {");
        out.println("    display: flex;");
        out.println("    align-items: center;");
        out.println("    justify-content: space-between;");
        out.println("    margin-bottom: 8px;");
        out.println("  }");
        out.println("  .dd-modal-title {");
        out.println("    margin: 0;");
        out.println("    font-size: 16px;");
        out.println("    color: #2f3a2f;");
        out.println("  }");
        out.println("  .dd-modal-close {");
        out.println("    background: none;");
        out.println("    border: none;");
        out.println("    font-size: 20px;");
        out.println("    line-height: 1;");
        out.println("    cursor: pointer;");
        out.println("    color: #6f6659;");
        out.println("  }");
        out.println("  .dd-modal-body {");
        out.println("    margin: 0;");
        out.println("    color: #4f4a42;");
        out.println("    font-size: 13px;");
        out.println("  }");
        out.println("  .dd-today-empty-row {");
        out.println("    color: #8b8377;");
        out.println("  }");
        out.println("  .dd-over-under {");
        out.println("    font-weight: bold;");
        out.println("  }");
        out.println("  .dd-over-under-over {");
        out.println("    color: #8f2f2f;");
        out.println("  }");
        out.println("  .dd-next-summary-table {");
        out.println("    width: 100%;");
        out.println("    border-collapse: collapse;");
        out.println("    margin-top: 8px;");
        out.println("  }");
        out.println("  .dd-next-summary-table th,");
        out.println("  .dd-next-summary-table td {");
        out.println("    border: 1px solid #d9ccb8;");
        out.println("    padding: 8px;");
        out.println("    vertical-align: top;");
        out.println("  }");
        out.println("  .dd-next-summary-table th {");
        out.println("    background: #f2eadf;");
        out.println("  }");
        out.println("  .dd-next-summary-row-selected {");
        out.println("    background: #f1f8ea;");
        out.println("  }");
        out.println("  .dd-next-day-link {");
        out.println("    font-weight: bold;");
        out.println("    color: #2d4a2f;");
        out.println("    text-decoration: none;");
        out.println("  }");
        out.println("  .dd-next-day-link:hover {");
        out.println("    text-decoration: underline;");
        out.println("  }");
        out.println("  .dd-next-section-title {");
        out.println("    margin: 12px 0 8px 0;");
        out.println("    font-size: 14px;");
        out.println("    text-transform: uppercase;");
        out.println("    letter-spacing: 0.03em;");
        out.println("    color: #56664d;");
        out.println("  }");
        out.println("  .dd-next-table-desc { width: 64%; }");
        out.println("  .dd-next-table-time { width: 18%; white-space: nowrap; }");
        out.println("  .dd-next-table-action { width: 18%; white-space: nowrap; text-align: center; }");
        out.println("  .dd-next-desc-link { color: #2d4a2f; text-decoration: none; }");
        out.println("  .dd-next-desc-link:hover { text-decoration: underline; }");
        out.println("  .dd-task-detail-grid { display: grid; grid-template-columns: 110px 1fr; gap: 8px 10px; }");
        out.println("  .dd-task-detail-label { font-weight: bold; color: #2d3a2d; }");
        out.println("  .dd-task-detail-value { color: #3e3a33; white-space: pre-wrap; word-break: break-word; }");
        out.println("  .dd-task-detail-empty { color: #8a8174; }");
        // Future: inline variant can be reused in planning rows/future-day summaries.
        // Future: stacked or inline gauges can be reused for selected future day
        // column.
        timeGaugeRenderer.printStyles(out);
        out.println("  @media (max-width: 1200px) {");
        out.println("    .dd-dashboard-header,");
        out.println("    .dd-dashboard-columns {");
        out.println("      grid-template-columns: 1fr;");
        out.println("    }");
        out.println("    .dd-dashboard-column {");
        out.println("      min-height: auto;");
        out.println("      border-right: none;");
        out.println("      border-bottom: 1px solid #ddd0bd;");
        out.println("    }");
        out.println("    .dd-dashboard-column-next {");
        out.println("      border-bottom: none;");
        out.println("    }");
        out.println("  }");
        out.println("</style>");
    }

    private void printNowColumn(PrintWriter out, DashboardNowColumnModel nowColumnModel) {
        // Future refinement point for project backlog logic lives in the service/model
        // layer so this renderer stays focused on layout.
        out.println("<div class=\"dd-now-action-wrap dd-panel dd-panel-open\">");
        printDevLabel(out, "CURRENT ACTION");
        if (nowColumnModel.getCurrentAction().isAvailable()) {
            out.println("  <form method=\"POST\" action=\"DandelionDashboardServlet\">");
            if (nowColumnModel.getCurrentAction().getNotes().isEmpty()) {
                out.println(
                        "    <p><a href=\"javascript:void(0);\" class=\"edit-link\" onclick=\"ddOpenCurrentActionNoteModal(); return false;\">Add Note</a></p>");
            } else {
                out.println("    <ul>");
                for (String note : nowColumnModel.getCurrentAction().getNotes()) {
                    out.println("      <li>" + escapeHtml(note) + "</li>");
                }
                out.println("    </ul>");
            }
            if (nowColumnModel.getCurrentAction().getLinkUrl() != null
                    && nowColumnModel.getCurrentAction().getLinkUrl().trim().length() > 0) {
                String fullUrl = nowColumnModel.getCurrentAction().getLinkUrl().trim();
                String displayUrl = fullUrl.length() > 30 ? fullUrl.substring(0, 30) + "..." : fullUrl;
                out.println("    <p>Link: <a href=\"" + escapeHtml(fullUrl)
                        + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + escapeHtml(displayUrl)
                        + "</a></p>");
            }
            if (nowColumnModel.getCurrentAction().getDeadlineDisplay() != null
                    && nowColumnModel.getCurrentAction().getDeadlineDisplay().trim().length() > 0) {
                out.println("    <p>Deadline: " + escapeHtml(nowColumnModel.getCurrentAction().getDeadlineDisplay())
                        + "</p>");
            }
            out.println("    <p>What action was taken:</p>");
            out.println("    <input type=\"text\" id=\"workProgressInput\" name=\"nextSummary\" value=\""
                    + escapeHtml(nowColumnModel.getCurrentAction().getSummary())
                    + "\" style=\"width: 100%;\" autofocus/>");
            out.println("    <div style=\"margin-top: 8px;\">");
            out.println(
                    "      <label><input type=\"radio\" name=\"workStatus\" value=\"IN_PROGRESS\"/> In Progress</label>");
            out.println(
                    "      <label style=\"margin-left: 12px;\"><input type=\"radio\" name=\"workStatus\" value=\"COMPLETE\" checked/> Complete</label>");
            out.println(
                    "      <label style=\"margin-left: 12px;\"><input type=\"radio\" name=\"workStatus\" value=\"DELETE\"/> Delete</label>");
            out.println(
                    "      <label style=\"margin-left: 12px;\"><input type=\"radio\" name=\"workStatus\" value=\"BLOCKED\"/> Blocked</label>");
            out.println("    </div>");
            out.println("    <div style=\"margin-top: 8px;\">");
            out.println("      <span id=\"workFollowUpPrefix\">Next</span>");
            out.println(
                    "      <input type=\"text\" id=\"workFollowUpInput\" name=\"workFollowUp\" style=\"width: 70%; margin-left: 8px;\" autocomplete=\"off\"/>");
            out.println(
                    "      <button type=\"submit\" name=\"action\" value=\"WorkNext\" style=\"margin-left: 8px;\">Next</button>");
            out.println("    </div>");
            out.println("    <input type=\"hidden\" name=\"completingActionNextId\" value=\""
                    + nowColumnModel.getCurrentAction().getActionNextId() + "\"/>");
            out.println("    <input type=\"hidden\" name=\"editActionNextId\" value=\""
                    + nowColumnModel.getCurrentAction().getActionNextId() + "\"/>");
            out.println("  </form>");

            if (nowColumnModel.getCurrentAction().getNotes().isEmpty()) {
                out.println(
                        "  <div id=\"ddCurrentActionNoteModal\" class=\"dd-modal-overlay\" onclick=\"ddCloseCurrentActionNoteModal(event)\">");
                out.println("    <div class=\"dd-modal\" onclick=\"event.stopPropagation()\">");
                out.println("      <div class=\"dd-modal-head\">");
                out.println("        <h3 class=\"dd-modal-title\">Add Note</h3>");
                out.println(
                        "        <button class=\"dd-modal-close\" onclick=\"ddCloseCurrentActionNoteModal(event)\">&times;</button>");
                out.println("      </div>");
                out.println("      <div style=\"padding: 16px;\">");
                out.println(
                        "        <textarea id=\"ddCurrentActionNoteInput\" rows=\"5\" style=\"width: 100%; box-sizing: border-box;\"></textarea>");
                out.println("        <div style=\"margin-top: 12px; display: flex; gap: 8px;\">");
                out.println(
                        "          <button class=\"dd-btn dd-btn-primary\" onclick=\"ddSubmitCurrentActionNote(event, "
                                + nowColumnModel.getCurrentAction().getActionNextId() + ")\">Add Note</button>");
                out.println(
                        "          <button class=\"dd-btn dd-btn-secondary\" onclick=\"ddCloseCurrentActionNoteModal(event)\">Cancel</button>");
                out.println("        </div>");
                out.println("      </div>");
                out.println("    </div>");
                out.println("  </div>");
                out.println("  <script>");
                out.println("    function ddOpenCurrentActionNoteModal() {");
                out.println("      var modal = document.getElementById('ddCurrentActionNoteModal');");
                out.println("      if (modal) { modal.classList.add('dd-modal-open'); }");
                out.println("      var input = document.getElementById('ddCurrentActionNoteInput');");
                out.println("      if (input) { input.focus(); }");
                out.println("    }");
                out.println("    function ddCloseCurrentActionNoteModal(evt) {");
                out.println("      if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
                out.println("      var modal = document.getElementById('ddCurrentActionNoteModal');");
                out.println("      if (modal) { modal.classList.remove('dd-modal-open'); }");
                out.println("    }");
                out.println("    function ddSubmitCurrentActionNote(evt, actionNextId) {");
                out.println("      evt.preventDefault();");
                out.println("      var input = document.getElementById('ddCurrentActionNoteInput');");
                out.println("      if (!input || !input.value || input.value.trim().length === 0) { return; }");
                out.println("      var formData = new FormData();");
                out.println("      formData.append('action', 'addCurrentActionNote');");
                out.println("      formData.append('actionNextId', actionNextId);");
                out.println("      formData.append('nextNote', input.value.trim());");
                out.println("      fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
                out.println("        .then(function(response) { return response.json(); })");
                out.println("        .then(function(data) {");
                out.println("          if (data && data.success) {");
                out.println("            window.location.reload();");
                out.println("          } else {");
                out.println("            alert('Unable to add note.');");
                out.println("          }");
                out.println("        })");
                out.println("        .catch(function() { alert('Unable to add note.'); });");
                out.println("    }");
                out.println("  </script>");
            }
        } else {
            out.println("  <p class=\"dd-subtle\">" + nowColumnModel.getCurrentAction().getFallbackMessage() + "</p>");
        }
        out.println("</div>");

        out.println("<div class=\"dd-section dd-panel dd-panel-open\">");
        printDevLabel(out, "CURRENT PROJECT");
        if (nowColumnModel.getCurrentProject().isAvailable()) {
            out.println("  <span class=\"dd-emphasis\">" + nowColumnModel.getCurrentProject().getName() + "</span>");
            out.println("  <p>" + nowColumnModel.getCurrentProject().getDescription() + "</p>");
        } else {
            out.println("  <p class=\"dd-subtle\">" + nowColumnModel.getCurrentProject().getFallbackMessage() + "</p>");
        }
        out.println("</div>");

        out.println("<div class=\"dd-section dd-panel dd-panel-open\">");
        printDevLabel(out, "PROJECT BACKLOG");
        printBacklogScheduled(out, nowColumnModel.getScheduledActions());
        printBacklogUnscheduled(out, nowColumnModel.getUnscheduledActions());
        printBacklogTemplated(out, nowColumnModel.getTemplatedActions());
        printBacklogRecentCompleted(out, nowColumnModel.getRecentCompleted());
        out.println("</div>");
    }

    private void printBacklogScheduled(PrintWriter out,
            List<DashboardNowColumnModel.ScheduledActionItem> items) {
        out.println("  <h3 class=\"dd-backlog-section-title\">Scheduled Actions</h3>");
        if (items.isEmpty()) {
            out.println("  <p class=\"dd-subtle dd-backlog-empty\">No scheduled actions.</p>");
            return;
        }
        out.println("  <table class=\"dd-today-table dd-backlog-table\">");
        out.println("    <tr>");
        out.println("      <th class=\"dd-backlog-col-desc\">Description</th>");
        out.println("      <th class=\"dd-backlog-col-date\">Date</th>");
        out.println("    </tr>");
        for (DashboardNowColumnModel.ScheduledActionItem item : items) {
            String rowClass = item.isCurrentSelection() ? "dd-backlog-row-current" : "";
            out.println("    <tr" + (rowClass.length() > 0 ? " class=\"" + rowClass + "\"" : "") + ">");
            out.println(
                    "      <td class=\"dd-backlog-col-desc\"><a href=\"javascript:void(0);\" class=\"dd-next-desc-link\" onclick=\"ddOpenNextTaskDetails("
                            + item.getActionNextId() + ", event)\">" + item.getDescriptionHtml() + "</a></td>");
            out.println(
                    "      <td class=\"dd-backlog-col-date\"><a href=\"javascript:void(0);\" class=\"dd-next-desc-link\" onclick=\"ddOpenActionModal('rescheduleModal',"
                            + item.getActionNextId() + ",event)\">" + escapeHtml(item.getDateLabel()) + "</a></td>");
            out.println("    </tr>");
        }
        out.println("  </table>");
    }

    private void printBacklogUnscheduled(PrintWriter out,
            List<DashboardNowColumnModel.UnscheduledActionItem> items) {
        out.println("  <h3 class=\"dd-backlog-section-title\">Un-Scheduled Actions</h3>");
        if (items.isEmpty()) {
            out.println("  <p class=\"dd-subtle dd-backlog-empty\">No un-scheduled actions.</p>");
            return;
        }
        out.println("  <table class=\"dd-today-table dd-backlog-table\">");
        out.println("    <tr>");
        out.println("      <th class=\"dd-backlog-col-desc\">Description</th>");
        out.println("    </tr>");
        for (DashboardNowColumnModel.UnscheduledActionItem item : items) {
            String rowClass = item.isCurrentSelection() ? "dd-backlog-row-current" : "";
            out.println("    <tr" + (rowClass.length() > 0 ? " class=\"" + rowClass + "\"" : "") + ">");
            out.println(
                    "      <td class=\"dd-backlog-col-desc\"><a href=\"javascript:void(0);\" class=\"dd-next-desc-link\" onclick=\"ddOpenNextTaskDetails("
                            + item.getActionNextId() + ", event)\">" + item.getDescriptionHtml() + "</a></td>");
            out.println("    </tr>");
        }
        out.println("  </table>");
    }

    private void printBacklogTemplated(PrintWriter out,
            List<DashboardNowColumnModel.TemplatedActionItem> items) {
        out.println("  <h3 class=\"dd-backlog-section-title\">Templated Actions</h3>");
        if (items.isEmpty()) {
            out.println("  <p class=\"dd-subtle dd-backlog-empty\">No templated actions.</p>");
            return;
        }
        out.println("  <table class=\"dd-today-table dd-backlog-table\">");
        out.println("    <tr>");
        out.println("      <th class=\"dd-backlog-col-desc\">Description</th>");
        out.println("    </tr>");
        for (DashboardNowColumnModel.TemplatedActionItem item : items) {
            out.println("    <tr>");
            out.println(
                    "      <td class=\"dd-backlog-col-desc\"><a href=\"javascript:void(0);\" class=\"dd-next-desc-link\" onclick=\"ddOpenNextTaskDetails("
                            + item.getActionNextId() + ", event)\">" + item.getDescriptionHtml() + "</a></td>");
            out.println("    </tr>");
        }
        out.println("  </table>");
    }

    private void printBacklogRecentCompleted(PrintWriter out,
            List<DashboardNowColumnModel.RecentCompletedItem> items) {
        out.println("  <h3 class=\"dd-backlog-section-title\">Recent Completed Actions</h3>");
        if (items.isEmpty()) {
            out.println("  <p class=\"dd-subtle dd-backlog-empty\">No completed actions in the last 90 days.</p>");
            return;
        }
        out.println("  <table class=\"dd-today-table dd-backlog-table\">");
        out.println("    <tr>");
        out.println("      <th class=\"dd-backlog-col-desc\">What Happened</th>");
        out.println("      <th class=\"dd-backlog-col-date\">Date</th>");
        out.println("    </tr>");
        for (DashboardNowColumnModel.RecentCompletedItem item : items) {
            out.println("    <tr>");
            out.println(
                    "      <td class=\"dd-backlog-col-desc\"><a href=\"javascript:void(0);\" class=\"dd-next-desc-link\" onclick=\"ddOpenNextTaskDetails("
                            + item.getActionNextId() + ", event)\">" + escapeHtml(item.getWhatHappened())
                            + "</a></td>");
            out.println("      <td class=\"dd-backlog-col-date\">" + escapeHtml(item.getDateLabel()) + "</td>");
            out.println("    </tr>");
        }
        out.println("  </table>");
    }

    private void printTodayColumn(PrintWriter out, DashboardTodayColumnModel todayColumnModel, AppReq appReq) {
        // Quick capture submit handling intentionally posts to
        // DandelionDashboardServlet
        // so the dashboard stays independent of legacy servlet routing.
        out.println("<div class=\"dd-section dd-panel dd-panel-open\">");
        printDevLabel(out, "QUICK CAPTURE");
        out.println("  <h2>Quick Capture</h2>");
        out.println("  <form class=\"dd-capture-form\" method=\"POST\" action=\""
                + todayColumnModel.getQuickCapture().getFormAction() + "\">");
        out.println("    <div class=\"dd-capture-row\">");
        out.println("      <div class=\"dd-capture-input-container\">");
        out.println("        <input type=\"text\" id=\"sentenceInput\" name=\""
                + todayColumnModel.getQuickCapture().getSentenceInputName()
                + "\" value=\"" + escapeHtml(todayColumnModel.getQuickCapture().getSentenceValue())
                + "\" placeholder=\"" + escapeHtml(todayColumnModel.getQuickCapture().getPlaceholder())
                + "\" autocomplete=\"off\"/>");
        out.println("        <div id=\"suggestions\" class=\"dd-capture-suggestions\"></div>");
        out.println("      </div>");
        out.println("      <div class=\"dd-capture-actions\">");
        out.println("        <input type=\"submit\" name=\"" + todayColumnModel.getQuickCapture().getActionParamName()
                + "\" value=\"" + todayColumnModel.getQuickCapture().getScheduleActionValue() + "\"/> ");
        out.println("        <input type=\"submit\" name=\"" + todayColumnModel.getQuickCapture().getActionParamName()
                + "\" value=\"" + todayColumnModel.getQuickCapture().getScheduleAndStartActionValue() + "\"/> ");
        out.println("      </div>");
        out.println("    </div>");
        out.println("  </form>");
        printQuickCaptureScript(out, todayColumnModel);
        out.println("</div>");

        out.println("<div class=\"dd-section dd-panel dd-panel-open\">");
        printDevLabel(out, "TODAY CHIPS");

        List<TodaySectionRenderModel> sections = buildTodaySections(todayColumnModel);
        String selectedTodaySectionId = resolveSelectedTodaySectionId(appReq, sections);
        boolean filteredView = selectedTodaySectionId.length() > 0;
        out.println("  <div class=\"dd-today-chips\">");
        for (TodaySectionRenderModel section : sections) {
            String chipClass = "dd-today-chip" + (section.getItems().isEmpty() ? " dd-today-chip-empty" : "");
            if ("overdue".equals(section.getId()) && section.getItems().size() > 1) {
                chipClass += " dd-today-chip-alert";
            }
            if (section.getItems().isEmpty()) {
                out.println("    <span class=\"" + chipClass + "\">");
                out.println("      <span>" + escapeHtml(section.getTitle()) + "</span>");
                out.println("      <span class=\"dd-today-chip-count\">" + section.getItems().size() + "</span>");
                out.println("    </span>");
            } else {
                String chipLink = "DandelionDashboardServlet?" + TODAY_SECTION_PARAM + "="
                        + escapeHtml(section.getId());
                out.println("    <a class=\"" + chipClass + "\" href=\"" + chipLink + "\">");
                out.println("      <span>" + escapeHtml(section.getTitle()) + "</span>");
                out.println("      <span class=\"dd-today-chip-count\">" + section.getItems().size() + "</span>");
                out.println("    </a>");
            }
        }
        out.println("  </div>");
        out.println("</div>");

        for (TodaySectionRenderModel section : sections) {
            if (!section.getItems().isEmpty()) {
                if (filteredView && !selectedTodaySectionId.equals(section.getId())) {
                    continue;
                }
                boolean completedSection = "completed".equals(section.getId());
                boolean personalSection = "personal-morning".equals(section.getId())
                        || "personal-afternoon-evening".equals(section.getId());
                out.println("<div id=\"dd-today-section-" + section.getId()
                        + "\" class=\"dd-today-section dd-panel dd-panel-open\">");
                printDevLabel(out, "TODAY SECTION " + section.getTitle().toUpperCase());
                out.println("  <h3 class=\"dd-today-section-title\">" + escapeHtml(section.getTitle())
                        + "<span class=\"dd-today-section-count\">(" + section.getItems().size() + ")</span></h3>");
                out.println("  <div class=\"dd-panel dd-panel-open\">");
                printDevLabel(out, "TODAY TABLE");
                out.println("    <table class=\"dd-today-table\">");
                out.println("      <tr>");
                out.println("        <th class=\"dd-today-cell-action-main\">Description</th>");
                out.println("        <th class=\"dd-today-cell-project\">Project</th>");
                if (completedSection) {
                    out.println("        <th class=\"dd-today-cell-action-placeholder\">Action</th>");
                } else if (personalSection) {
                    out.println("        <th class=\"dd-today-cell-time-slot\">Time Slot</th>");
                    out.println("        <th class=\"dd-today-cell-action-placeholder\">Action</th>");
                } else {
                    out.println("        <th class=\"dd-today-cell-gauge\">Time Gauge</th>");
                    out.println("        <th class=\"dd-today-cell-action-placeholder\">Action</th>");
                }
                out.println("      </tr>");
                for (DashboardTodayColumnModel.TodayActionItemModel item : section.getItems()) {
                    String todayRowOnclick = " onclick=\"window.location='DandelionDashboardServlet?action=SelectAction&completingActionNextId="
                            + item.getActionNextId() + "'\"";
                    out.println("      <tr class=\"dd-row-clickable\"" + todayRowOnclick + ">");
                    out.println(
                            "        <td class=\"dd-today-cell-action-main\">" + item.getDescriptionHtml() + "</td>");
                    out.println("        <td class=\"dd-today-cell-project\">" + escapeHtml(item.getProjectName())
                            + "</td>");
                    if (completedSection) {
                        printTodayRowActionsCell(out, item.getActionNextId());
                    } else if (personalSection) {
                        out.println(
                                "        <td class=\"dd-today-cell-time-slot\">" + escapeHtml(item.getContextLabel())
                                        + "</td>");
                        printTodayRowActionsCell(out, item.getActionNextId());
                    } else {
                        out.println("        <td class=\"dd-today-cell-gauge\">");
                        timeGaugeRenderer.render(out, buildInlineTodayGauge(item));
                        out.println("        </td>");
                        printTodayRowActionsCell(out, item.getActionNextId());
                    }
                    out.println("      </tr>");
                }
                out.println("    </table>");
                if (filteredView) {
                    out.println(
                            "    <p class=\"dd-today-show-all-wrap\"><a class=\"dd-today-show-all-link\" href=\"DandelionDashboardServlet?"
                                    + TODAY_SECTION_PARAM + "=" + TODAY_SECTION_ALL
                                    + "\">Show all</a></p>");
                }
                out.println("  </div>");
                out.println("</div>");
            }
        }

        printTodayActionModalScaffolding(out, appReq);
    }

    private void printTodayRowActionsCell(PrintWriter out, int actionNextId) {
        out.println("        <td class=\"dd-today-cell-action-placeholder\">");
        out.println("          <span class=\"dd-row-actions\">");
        out.println(
                "            <button class=\"dd-row-action-btn\" title=\"Reprioritize for today\" onclick=\"ddOpenActionModal('reprioritizeModal',"
                        + actionNextId + ",event)\">↕️</button>");
        out.println(
                "            <button class=\"dd-row-action-btn\" title=\"Reschedule\" onclick=\"ddOpenActionModal('rescheduleModal',"
                        + actionNextId + ",event)\">📅</button>");
        out.println(
                "            <button class=\"dd-row-action-btn\" title=\"Edit action\" onclick=\"ddOpenActionModal('editActionModal',"
                        + actionNextId + ",event)\">✏️</button>");
        out.println("          </span>");
        out.println("        </td>");
    }

    private void printTodayActionModalScaffolding(PrintWriter out, AppReq appReq) {
        printTodayReprioritizeModal(out, appReq);
        printTodayRescheduleModal(out, appReq);
        printTodayEditActionModal(out, appReq);

        out.println("<script>");
        out.println("  window.ddSelectedActionId = null;");
        out.println("  window.ddActionDataCache = {};");
        out.println("  function ddOpenActionModal(modalId, actionId, evt) {");
        out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
        out.println("    window.ddSelectedActionId = actionId;");
        out.println("    var modal = document.getElementById(modalId);");
        out.println(
                "    if (modal) { modal.classList.add('dd-modal-open'); modal.setAttribute('data-action-id', actionId); }");
        out.println("    if (modalId === 'editActionModal') {");
        out.println("      ddLoadEditFormData(actionId);");
        out.println("    } else if (modalId === 'reprioritizeModal') {");
        out.println("      ddLoadReprioritizeData(actionId);");
        out.println("    } else if (modalId === 'rescheduleModal') {");
        out.println("      ddLoadRescheduleData(actionId);");
        out.println("    }");
        out.println("    return false;");
        out.println("  }");
        out.println("  function ddCloseActionModal(modalId, evt) {");
        out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
        out.println("    var modal = document.getElementById(modalId);");
        out.println("    if (modal) { modal.classList.remove('dd-modal-open'); }");
        out.println("    return false;");
        out.println("  }");
        out.println("  function ddOverlayClose(evt, overlay) {");
        out.println("    if (evt && evt.target === overlay) { overlay.classList.remove('dd-modal-open'); }");
        out.println("  }");
        out.println("  function ddCloseAllActionModals() {");
        out.println("    var modals = document.querySelectorAll('.dd-modal-overlay.dd-modal-open');");
        out.println("    for (var i = 0; i < modals.length; i++) { modals[i].classList.remove('dd-modal-open'); }");
        out.println("  }");
        out.println("  if (window.addEventListener) {");
        out.println(
                "    window.addEventListener('keydown', function(e) { if (e.key === 'Escape') { ddCloseAllActionModals(); } });");
        out.println("  }");
        out.println("</script>");
    }

    private void printTodayReprioritizeModal(PrintWriter out, AppReq appReq) {
        out.println("<div id=\"reprioritizeModal\" class=\"dd-modal-overlay\" onclick=\"ddOverlayClose(event,this)\">");
        out.println("  <div class=\"dd-modal dd-reprioritize-modal\" onclick=\"event.stopPropagation()\">");
        out.println("    <div class=\"dd-modal-head\">");
        out.println("      <h3 class=\"dd-modal-title\">Reprioritize for Today</h3>");
        out.println(
                "      <button class=\"dd-modal-close\" onclick=\"ddCloseActionModal('reprioritizeModal',event)\">&times;</button>");
        out.println("    </div>");
        out.println("    <div class=\"dd-reprioritize-body\">");
        out.println("      <div class=\"dd-reprioritize-current\">");
        out.println("        <h4>Current Action</h4>");
        out.println("        <p id=\"ddReprioritizeCurrentAction\" class=\"dd-action-title\"></p>");
        out.println("      </div>");
        out.println("      <div class=\"dd-reprioritize-quick-moves\">");
        out.println("        <h4>Quick Moves</h4>");
        out.println("        <div class=\"dd-quick-move-buttons\">");
        out.println(
                "          <button class=\"dd-btn dd-btn-quick\" onclick=\"ddMoveToFirst(event)\">⬆️ First</button>");
        out.println("          <button class=\"dd-btn dd-btn-quick\" onclick=\"ddMoveUp(event)\">↑ Up</button>");
        out.println("          <button class=\"dd-btn dd-btn-quick\" onclick=\"ddMoveDown(event)\">↓ Down</button>");
        out.println("          <button class=\"dd-btn dd-btn-quick\" onclick=\"ddMoveToLast(event)\">⬇️ Last</button>");
        out.println("        </div>");
        out.println("      </div>");
        out.println("      <div class=\"dd-reprioritize-list\">");
        out.println("        <h4>Move Before...</h4>");
        out.println("        <div id=\"ddReprioritizeActionList\" class=\"dd-action-list-compact\"></div>");
        out.println("      </div>");
        out.println("    </div>");
        out.println("  </div>");
        out.println("</div>");

        out.println("<style>");
        out.println(
                "  .dd-reprioritize-modal { max-width: 400px; max-height: 85vh; overflow-y: auto; display: flex; flex-direction: column; }");
        out.println(
                "  .dd-reprioritize-body { padding: 16px; flex: 1; overflow-y: auto; display: flex; flex-direction: column; }");
        out.println(
                "  .dd-reprioritize-current { margin-bottom: 16px; padding: 12px; background: #f0ebe0; border-radius: 3px; border: 1px solid #cfbea6; }");
        out.println(
                "  .dd-reprioritize-current h4 { margin: 0 0 8px 0; color: #3c5341; font-size: 12px; font-weight: bold; text-transform: uppercase; }");
        out.println("  .dd-action-title { margin: 0; color: #2d3a2d; font-size: 13px; font-weight: 500; }");
        out.println("  .dd-reprioritize-quick-moves { margin-bottom: 16px; }");
        out.println(
                "  .dd-reprioritize-quick-moves h4 { margin: 0 0 8px 0; color: #3c5341; font-size: 12px; font-weight: bold; text-transform: uppercase; }");
        out.println("  .dd-quick-move-buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }");
        out.println(
                "  .dd-btn-quick { padding: 8px 12px; background: #5b735c; color: #fffdf8; border: 1px solid #445a45; border-radius: 3px; cursor: pointer; font-size: 12px; font-weight: bold; transition: all 0.2s; }");
        out.println("  .dd-btn-quick:hover { background: #485a46; }");
        out.println("  .dd-reprioritize-list { flex: 1; overflow-y: auto; }");
        out.println(
                "  .dd-reprioritize-list h4 { margin: 0 0 8px 0; color: #3c5341; font-size: 12px; font-weight: bold; text-transform: uppercase; }");
        out.println("  .dd-action-list-compact { display: flex; flex-direction: column; gap: 4px; }");
        out.println(
                "  .dd-action-item-button { display: block; width: 100%; text-align: left; padding: 8px 10px; background: #f0ebe0; border: 1px solid #cfbea6; border-radius: 3px; color: #2d3a2d; cursor: pointer; font-size: 12px; text-decoration: none; transition: all 0.2s; font-family: inherit; }");
        out.println("  .dd-action-item-button:hover { background: #e8e1d3; border-color: #bfa982; }");
        out.println("</style>");

        out.println("<script>");
        out.println("  window.ddReprioritizeCurrentActionId = null;");
        out.println("  function ddLoadReprioritizeData(actionId) {");
        out.println("    window.ddReprioritizeCurrentActionId = actionId;");
        out.println("    var formData = new FormData();");
        out.println("    formData.append('action', 'loadReprioritizeData');");
        out.println("    formData.append('actionNextId', actionId);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(response => response.json())");
        out.println("      .then(data => {");
        out.println("        if (data.success) {");
        out.println(
                "          document.getElementById('ddReprioritizeCurrentAction').textContent = (data.description || 'Action').substring(0, 80) + (data.description && data.description.length > 80 ? '...' : '');");
        out.println("          var listDiv = document.getElementById('ddReprioritizeActionList');");
        out.println("          listDiv.innerHTML = '';");
        out.println("          if (data.actions && data.actions.length > 0) {");
        out.println("            data.actions.forEach(function(action) {");
        out.println("              var btn = document.createElement('button');");
        out.println("              btn.className = 'dd-action-item-button';");
        out.println("              btn.type = 'button';");
        out.println(
                "              btn.textContent = (action.description || 'Action').substring(0, 60) + (action.description && action.description.length > 60 ? '...' : '');");
        out.println("              btn.onclick = function(e) { ddMoveBeforeAction(e, action.id); };");
        out.println("              listDiv.appendChild(btn);");
        out.println("            });");
        out.println("          }");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(err => console.log('Error loading reprioritize data:', err));");
        out.println("  }");
        out.println("  function ddPerformMove(moveType) {");
        out.println("    var actionId = window.ddReprioritizeCurrentActionId;");
        out.println("    if (!actionId) return;");
        out.println("    var formData = new FormData();");
        out.println("    formData.append('action', 'reprioritizeAction');");
        out.println("    formData.append('actionNextId', actionId);");
        out.println("    formData.append('moveType', moveType);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(response => response.json())");
        out.println("      .then(data => {");
        out.println("        if (data.success) {");
        out.println("          ddCloseActionModal('reprioritizeModal');");
        out.println("          window.location.reload();");
        out.println("        } else {");
        out.println("          alert('Error: ' + (data.message || 'Could not move action'));");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(err => {");
        out.println("        alert('Error moving action. Please try again.');");
        out.println("        console.log('Move error:', err);");
        out.println("      });");
        out.println("  }");
        out.println("  function ddMoveToFirst(evt) { evt.preventDefault(); ddPerformMove('first'); }");
        out.println("  function ddMoveUp(evt) { evt.preventDefault(); ddPerformMove('up'); }");
        out.println("  function ddMoveDown(evt) { evt.preventDefault(); ddPerformMove('down'); }");
        out.println("  function ddMoveToLast(evt) { evt.preventDefault(); ddPerformMove('last'); }");
        out.println("  function ddMoveBeforeAction(evt, targetActionId) { evt.preventDefault(); ");
        out.println("    var formData = new FormData();");
        out.println("    formData.append('action', 'reprioritizeAction');");
        out.println("    formData.append('actionNextId', window.ddReprioritizeCurrentActionId);");
        out.println("    formData.append('moveType', 'before');");
        out.println("    formData.append('targetActionId', targetActionId);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(response => response.json())");
        out.println("      .then(data => {");
        out.println("        if (data.success) {");
        out.println("          ddCloseActionModal('reprioritizeModal');");
        out.println("          window.location.reload();");
        out.println("        } else {");
        out.println("          alert('Error: ' + (data.message || 'Could not move action'));");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(err => {");
        out.println("        alert('Error moving action. Please try again.');");
        out.println("        console.log('Move error:', err);");
        out.println("      });");
        out.println("  }");
        out.println("</script>");
    }

    private void printTodayRescheduleModal(PrintWriter out, AppReq appReq) {
        // Modal for rescheduling an action to a new date (today or future)
        // Designed for reuse on future-day items as well as today items
        out.println("<div id=\"rescheduleModal\" class=\"dd-modal-overlay\" onclick=\"ddOverlayClose(event,this)\">");
        out.println("  <div class=\"dd-modal dd-reschedule-modal\" onclick=\"event.stopPropagation()\">");
        out.println("    <div class=\"dd-modal-head\">");
        out.println("      <h3 class=\"dd-modal-title\">Reschedule Action</h3>");
        out.println(
                "      <button class=\"dd-modal-close\" onclick=\"ddCloseActionModal('rescheduleModal',event)\">&times;</button>");
        out.println("    </div>");
        out.println("    <div class=\"dd-reschedule-body\">");
        out.println("      <div class=\"dd-reschedule-action\">");
        out.println("        <p class=\"dd-reschedule-label\">Current Action</p>");
        out.println("        <p id=\"ddRescheduleActionDesc\" class=\"dd-reschedule-action-desc\"></p>");
        out.println("      </div>");
        out.println("      <div class=\"dd-reschedule-date-section\">");
        out.println("        <label class=\"dd-reschedule-date-label\">New Date (today or future):</label>");
        out.println(
                "        <input type=\"date\" id=\"ddRescheduleNewDate\" class=\"dd-reschedule-date-input\" min=\"\" />");
        out.println("        <div id=\"ddRescheduleCalendar\" class=\"dd-reschedule-calendar\"></div>");
        out.println("      </div>");
        out.println("      <div class=\"dd-reschedule-actions\">");
        out.println(
                "        <button class=\"dd-btn dd-btn-primary\" onclick=\"ddSubmitReschedule(event)\">Apply</button>");
        out.println(
                "        <button class=\"dd-btn dd-btn-secondary\" onclick=\"ddCloseActionModal('rescheduleModal',event)\">Cancel</button>");
        out.println("      </div>");
        out.println("    </div>");
        out.println("  </div>");
        out.println("</div>");

        out.println("<style>");
        out.println("  .dd-reschedule-modal { max-width: 400px; }");
        out.println("  .dd-reschedule-body { padding: 16px; }");
        out.println(
                "  .dd-reschedule-action { margin-bottom: 20px; padding: 12px; background: #f0ebe0; border-radius: 3px; border: 1px solid #cfbea6; }");
        out.println(
                "  .dd-reschedule-label { margin: 0; color: #3c5341; font-size: 11px; font-weight: bold; text-transform: uppercase; }");
        out.println(
                "  .dd-reschedule-action-desc { margin: 8px 0 0 0; color: #2d3a2d; font-size: 13px; font-weight: 500; }");
        out.println("  .dd-reschedule-date-section { margin-bottom: 20px; }");
        out.println(
                "  .dd-reschedule-date-label { display: block; margin-bottom: 8px; color: #2d3a2d; font-weight: bold; font-size: 13px; }");
        out.println(
                "  .dd-reschedule-date-input { width: 100%; padding: 8px; border: 1px solid #cfbea6; border-radius: 3px; font-size: 13px; box-sizing: border-box; }");
        out.println(
                "  .dd-reschedule-date-input:focus { outline: none; border-color: #5b735c; box-shadow: 0 0 0 2px rgba(91, 115, 92, 0.1); }");
        out.println(
                "  .dd-reschedule-calendar { margin-top: 12px; border: 1px solid #d6c8b3; border-radius: 4px; overflow: hidden; background: #fcfaf6; }");
        out.println(
                "  .dd-reschedule-calendar table { width: 100%; border-collapse: collapse; table-layout: fixed; }");
        out.println(
                "  .dd-reschedule-calendar th { padding: 6px 0; font-size: 11px; text-transform: uppercase; color: #6e6559; background: #f2eadf; border-bottom: 1px solid #d6c8b3; }");
        out.println(
                "  .dd-reschedule-calendar td { border: 1px solid #ece2d4; height: 34px; padding: 0; text-align: center; vertical-align: middle; }");
        out.println(
                "  .dd-reschedule-calendar-day { display: block; width: 100%; height: 100%; padding: 7px 0; background: transparent; border: none; color: #2d3a2d; cursor: pointer; font-size: 12px; }");
        out.println(
                "  .dd-reschedule-calendar-day:hover { background: #ece6d9; }");
        out.println(
                "  .dd-reschedule-calendar-day-selected { background: #5b735c; color: #fffdf8; font-weight: bold; }");
        out.println(
                "  .dd-reschedule-calendar-day-today { box-shadow: inset 0 0 0 1px #5b735c; font-weight: bold; }");
        out.println(
                "  .dd-reschedule-calendar-day-disabled { color: #b2a79a; background: #f7f2eb; cursor: default; }");
        out.println(
                "  .dd-reschedule-calendar-day-other-month { color: #8f877c; }");
        out.println(
                "  .dd-reschedule-actions { display: flex; gap: 8px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #cfbea6; }");
        out.println("</style>");

        out.println("<script>");
        out.println("  // Helper to get today's date in ISO format (YYYY-MM-DD)");
        out.println("  function ddGetTodayISO() {");
        out.println("    var today = new Date();");
        out.println("    var year = today.getFullYear();");
        out.println("    var month = (today.getMonth() + 1).toString().padStart(2, '0');");
        out.println("    var day = today.getDate().toString().padStart(2, '0');");
        out.println("    return year + '-' + month + '-' + day;");
        out.println("  }");
        out.println("  function ddStartOfWeek(date) {");
        out.println("    var start = new Date(date.getFullYear(), date.getMonth(), date.getDate());");
        out.println("    start.setDate(start.getDate() - start.getDay());");
        out.println("    return start;");
        out.println("  }");
        out.println("  function ddFormatISO(date) {");
        out.println("    var year = date.getFullYear();");
        out.println("    var month = (date.getMonth() + 1).toString().padStart(2, '0');");
        out.println("    var day = date.getDate().toString().padStart(2, '0');");
        out.println("    return year + '-' + month + '-' + day;");
        out.println("  }");
        out.println("  function ddRenderRescheduleCalendar(selectedIso) {");
        out.println("    var container = document.getElementById('ddRescheduleCalendar');");
        out.println("    if (!container) { return; }");
        out.println("    var today = new Date();");
        out.println("    today = new Date(today.getFullYear(), today.getMonth(), today.getDate());");
        out.println("    var start = ddStartOfWeek(today);");
        out.println("    var currentMonth = today.getMonth();");
        out.println("    var weekdayLabels = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];");
        out.println("    var html = '<table><tr>';");
        out.println(
                "    for (var h = 0; h < weekdayLabels.length; h++) { html += '<th>' + weekdayLabels[h] + '</th>'; }");
        out.println("    html += '</tr>';");
        out.println("    for (var week = 0; week < 6; week++) {");
        out.println("      html += '<tr>'; ");
        out.println("      for (var day = 0; day < 7; day++) {");
        out.println(
                "        var cellDate = new Date(start.getFullYear(), start.getMonth(), start.getDate() + (week * 7) + day);");
        out.println("        var iso = ddFormatISO(cellDate);");
        out.println("        var classes = ['dd-reschedule-calendar-day'];");
        out.println(
                "        if (cellDate.getMonth() !== currentMonth && week === 0) { classes.push('dd-reschedule-calendar-day-other-month'); }");
        out.println(
                "        if (cellDate.getTime() === today.getTime()) { classes.push('dd-reschedule-calendar-day-today'); }");
        out.println(
                "        if (selectedIso && iso === selectedIso) { classes.push('dd-reschedule-calendar-day-selected'); }");
        out.println("        if (cellDate < today) {");
        out.println("          classes.push('dd-reschedule-calendar-day-disabled');");
        out.println(
                "          html += '<td><span class=\"' + classes.join(' ') + '\">' + cellDate.getDate() + '</span></td>'; ");
        out.println("        } else {");
        out.println(
                "          html += '<td><button type=\"button\" class=\"' + classes.join(' ') + '\" onclick=\"ddSelectRescheduleDate(\\'' + iso + '\\')\">' + cellDate.getDate() + '</button></td>'; ");
        out.println("        }");
        out.println("      }");
        out.println("      html += '</tr>'; ");
        out.println("    }");
        out.println("    html += '</table>'; ");
        out.println("    container.innerHTML = html;");
        out.println("  }");
        out.println("  function ddSelectRescheduleDate(isoDate) {");
        out.println("    var input = document.getElementById('ddRescheduleNewDate');");
        out.println("    if (!input) { return; }");
        out.println("    input.value = isoDate;");
        out.println("    ddRenderRescheduleCalendar(isoDate);");
        out.println("  }");
        out.println("  // Load reschedule data when modal opens");
        out.println("  function ddLoadRescheduleData(actionId) {");
        out.println("    var todayISO = ddGetTodayISO();");
        out.println("    // Set min attribute to block past dates");
        out.println("    document.getElementById('ddRescheduleNewDate').setAttribute('min', todayISO);");
        out.println("    var formData = new FormData();");
        out.println("    formData.append('action', 'loadRescheduleData');");
        out.println("    formData.append('actionNextId', actionId);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(response => response.json())");
        out.println("      .then(data => {");
        out.println("        if (data.success) {");
        out.println(
                "          document.getElementById('ddRescheduleActionDesc').textContent = (data.description || 'Action').substring(0, 100) + (data.description && data.description.length > 100 ? '...' : '');");
        out.println("          // Convert MM/dd/yyyy from server to YYYY-MM-DD for date input");
        out.println("          if (data.nextActionDate) {");
        out.println("            var parts = data.nextActionDate.split('/');");
        out.println("            if (parts.length === 3) {");
        out.println("              var isoDate = parts[2] + '-' + parts[0] + '-' + parts[1];");
        out.println("              document.getElementById('ddRescheduleNewDate').value = isoDate;");
        out.println("              ddRenderRescheduleCalendar(isoDate);");
        out.println("            }");
        out.println("          } else {");
        out.println("            document.getElementById('ddRescheduleNewDate').value = todayISO;");
        out.println("            ddRenderRescheduleCalendar(todayISO);");
        out.println("          }");
        out.println("          // Store action ID for submission");
        out.println(
                "          document.getElementById('ddRescheduleNewDate').setAttribute('data-action-id', actionId);");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(err => console.log('Error loading reschedule data:', err));");
        out.println("  }");
        out.println("  document.addEventListener('change', function(evt) {");
        out.println("    if (evt && evt.target && evt.target.id === 'ddRescheduleNewDate') {");
        out.println("      ddRenderRescheduleCalendar(evt.target.value);");
        out.println("    }");
        out.println("  });");
        out.println("  // Submit reschedule");
        out.println("  function ddSubmitReschedule(evt) {");
        out.println("    evt.preventDefault();");
        out.println("    var dateInput = document.getElementById('ddRescheduleNewDate');");
        out.println("    var actionId = dateInput.getAttribute('data-action-id');");
        out.println("    var selectedDate = dateInput.value;");
        out.println("    if (!actionId || !selectedDate) {");
        out.println("      alert('Please select a date');");
        out.println("      return;");
        out.println("    }");
        out.println("    // Convert YYYY-MM-DD to MM/dd/yyyy for server");
        out.println("    var parts = selectedDate.split('-');");
        out.println("    if (parts.length === 3) {");
        out.println("      var formattedDate = parts[1] + '/' + parts[2] + '/' + parts[0];");
        out.println("      var formData = new FormData();");
        out.println("      formData.append('action', 'rescheduleAction');");
        out.println("      formData.append('actionNextId', actionId);");
        out.println("      formData.append('nextActionDate', formattedDate);");
        out.println("      fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("        .then(response => response.json())");
        out.println("        .then(data => {");
        out.println("          if (data.success) {");
        out.println("            ddCloseActionModal('rescheduleModal');");
        out.println("            window.location.reload();");
        out.println("          } else {");
        out.println("            alert('Error: ' + (data.message || 'Could not reschedule action'));");
        out.println("          }");
        out.println("        })");
        out.println("        .catch(err => {");
        out.println("          alert('Error rescheduling action. Please try again.');");
        out.println("          console.log('Reschedule error:', err);");
        out.println("        });");
        out.println("    }");
        out.println("  }");
        out.println("</script>");
    }

    private void printTodayEditActionModal(PrintWriter out, AppReq appReq) {
        out.println("<div id=\"editActionModal\" class=\"dd-modal-overlay\" onclick=\"ddOverlayClose(event,this)\">");
        out.println("  <div class=\"dd-modal dd-edit-action-modal\" onclick=\"event.stopPropagation()\">");
        out.println("    <div class=\"dd-modal-head\">");
        out.println("      <h3 class=\"dd-modal-title\">Edit Action</h3>");
        out.println(
                "      <button class=\"dd-modal-close\" onclick=\"ddCloseActionModal('editActionModal',event)\">&times;</button>");
        out.println("    </div>");
        out.println(
                "    <form id=\"ddEditActionForm\" class=\"dd-edit-form\" method=\"POST\" action=\"DandelionDashboardServlet\" onsubmit=\"return ddSubmitEditActionForm(event)\">");
        out.println("      <input type=\"hidden\" name=\"action\" value=\"editAction\">");
        out.println("      <input type=\"hidden\" name=\"actionNextId\" id=\"ddEditActionId\" value=\"\">");
        out.println("      <input type=\"hidden\" id=\"ddEditActionDateOriginal\" value=\"\">");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">When:</label>");
        out.println("        <div class=\"dd-form-control\">");
        out.println(
                "          <input type=\"text\" name=\"nextActionDate\" id=\"ddEditActionDate\" size=\"10\" class=\"dd-form-input\">");
        out.println("          <div id=\"ddEditActionDateQuickButtons\" class=\"dd-quick-selectors\"></div>");
        out.println("        </div>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Action Type:</label>");
        out.println("        <div class=\"dd-form-control\">");
        out.println("          <div class=\"dd-quick-selectors dd-action-type-selectors\">");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WILL
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WILL
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">will</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.MIGHT
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.MIGHT
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">might</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WILL_CONTACT
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WILL_CONTACT
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">will contact</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WILL_MEET
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WILL_MEET
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">will meet</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WILL_REVIEW
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WILL_REVIEW
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">will review</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WILL_DOCUMENT
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WILL_DOCUMENT
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">will document</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WILL_FOLLOW_UP
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WILL_FOLLOW_UP
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">will follow up</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.COMMITTED_TO
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.COMMITTED_TO
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">committed</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.GOAL
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.GOAL
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">set goal</button>");
        out.println("            <button type=\"button\" data-action-type=\"" + ProjectNextActionType.WAITING
                + "\" onclick=\"ddSetActionType('" + ProjectNextActionType.WAITING
                + "')\" class=\"dd-quick-btn dd-action-type-btn\">waiting</button>");
        out.println("          </div>");
        out.println("          <input type=\"hidden\" name=\"nextActionType\" id=\"ddEditActionType\" value=\"\">");
        out.println("        </div>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Who:</label>");
        out.println("        <select name=\"nextContactId\" id=\"ddEditActionContact\" class=\"dd-form-input\">");
        out.println("          <option value=\"\">none</option>");

        Session dataSession = appReq.getDataSession();
        WebUser webUser = appReq.getWebUser();
        int currentContactId = webUser.getProjectContact().getContactId();

        Query query = dataSession.createQuery(
                "from ProjectContact where provider = :provider order by nameLast, nameFirst");
        query.setParameter("provider", webUser.getProvider());
        @SuppressWarnings("unchecked")
        List<ProjectContact> contacts = query.list();
        for (ProjectContact contact : contacts) {
            if (contact.getContactId() != currentContactId) {
                out.println("          <option value=\"" + contact.getContactId() + "\">"
                        + escapeHtml(contact.getName()) + "</option>");
            }
        }
        out.println("        </select>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Description:</label>");
        out.println(
                "        <textarea name=\"nextDescription\" id=\"ddEditActionDescription\" class=\"dd-form-textarea\" rows=\"2\"></textarea>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Time Estimate:</label>");
        out.println("        <div class=\"dd-form-control\">");
        out.println(
                "          <input type=\"text\" name=\"nextTimeEstimate\" id=\"ddEditActionTime\" size=\"3\" class=\"dd-form-input-small\" value=\"0\"> mins");
        out.println("          <div class=\"dd-quick-selectors\">");
        out.println("            <a href=\"javascript:ddSetTimeEstimate('5')\" class=\"dd-quick-btn\">5m</a>");
        out.println("            <a href=\"javascript:ddSetTimeEstimate('10')\" class=\"dd-quick-btn\">10m</a>");
        out.println("            <a href=\"javascript:ddSetTimeEstimate('20')\" class=\"dd-quick-btn\">20m</a>");
        out.println("            <a href=\"javascript:ddSetTimeEstimate('30')\" class=\"dd-quick-btn\">30m</a>");
        out.println("            <a href=\"javascript:ddSetTimeEstimate('60')\" class=\"dd-quick-btn\">1h</a>");
        out.println("            <a href=\"javascript:ddSetTimeEstimate('120')\" class=\"dd-quick-btn\">2h</a>");
        out.println("          </div>");
        out.println("        </div>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Target Date:</label>");
        out.println("        <div class=\"dd-form-control\">");
        out.println(
                "          <input type=\"text\" name=\"nextTargetDate\" id=\"ddEditActionTarget\" size=\"10\" class=\"dd-form-input\">");
        out.println("          <div id=\"ddEditActionTargetQuickButtons\" class=\"dd-quick-selectors\"></div>");
        out.println("        </div>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Deadline:</label>");
        out.println("        <div class=\"dd-form-control\">");
        out.println(
                "          <input type=\"text\" name=\"nextDeadlineDate\" id=\"ddEditActionDeadline\" size=\"10\" class=\"dd-form-input\">");
        out.println("          <div id=\"ddEditActionDeadlineQuickButtons\" class=\"dd-quick-selectors\"></div>");
        out.println("        </div>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Link URL:</label>");
        out.println(
                "        <input type=\"text\" name=\"linkUrl\" id=\"ddEditActionLink\" size=\"40\" class=\"dd-form-input\">");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-field\">");
        out.println("        <label class=\"dd-form-label\">Note:</label>");
        out.println(
                "        <textarea name=\"nextNote\" id=\"ddEditActionNote\" class=\"dd-form-textarea\" rows=\"2\"></textarea>");
        out.println("      </div>");

        out.println("      <div class=\"dd-form-actions\">");
        out.println("        <button type=\"submit\" class=\"dd-btn dd-btn-primary\">Save Action</button>");
        out.println(
                "        <button type=\"button\" class=\"dd-btn dd-btn-secondary\" onclick=\"ddCloseActionModal('editActionModal')\">Cancel</button>");
        out.println("      </div>");
        out.println("    </form>");
        out.println("  </div>");
        out.println("</div>");

        out.println("<style>");
        out.println("  .dd-edit-action-modal { max-width: 600px; max-height: 85vh; overflow-y: auto; }");
        out.println("  .dd-edit-form { padding: 16px; }");
        out.println("  .dd-form-field { margin-bottom: 16px; }");
        out.println(
                "  .dd-form-label { display: block; margin-bottom: 6px; font-weight: bold; color: #2d3a2d; font-size: 13px; }");
        out.println(
                "  .dd-form-input, .dd-form-input-small, .dd-form-textarea { padding: 6px 8px; border: 1px solid #cfbea6; border-radius: 3px; font-family: inherit; font-size: 13px; }");
        out.println("  .dd-form-input { width: 100%; box-sizing: border-box; }");
        out.println("  .dd-form-input-small { width: 60px; }");
        out.println("  .dd-form-textarea { width: 100%; box-sizing: border-box; }");
        out.println("  .dd-quick-selectors { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }");
        out.println("  .dd-action-type-selectors { align-items: flex-start; }");
        out.println(
                "  .dd-quick-btn { display: inline-block; padding: 4px 10px; background: #f0ebe0; border: 1px solid #cfbea6; border-radius: 3px; color: #2d3a2d; text-decoration: none; font-size: 12px; cursor: pointer; }");
        out.println("  .dd-quick-btn:hover { background: #e8e1d3; border-color: #bfa982; }");
        out.println(
                "  .dd-quick-btn.dd-action-type-btn-active { background: #5b735c; color: #fffdf8; border-color: #445a45; }");
        out.println("  .dd-form-control { display: flex; flex-direction: column; align-items: stretch; gap: 8px; }");
        out.println(
                "  .dd-form-actions { display: flex; gap: 8px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #cfbea6; }");
        out.println(
                "  .dd-btn { padding: 8px 16px; border: 1px solid #cfbea6; border-radius: 3px; background: #f0ebe0; color: #2d3a2d; cursor: pointer; font-size: 13px; font-weight: bold; transition: all 0.2s; }");
        out.println("  .dd-btn:hover { background: #e8e1d3; border-color: #bfa982; }");
        out.println("  .dd-btn-primary { background: #5b735c; color: #fffdf8; border-color: #445a45; }");
        out.println("  .dd-btn-primary:hover { background: #485a46; }");
        out.println("  .dd-btn-secondary { background: #f0ebe0; }");
        out.println("</style>");

        out.println("<script>");
        out.println("  function ddFormatEditDate(date) {");
        out.println("    var month = (date.getMonth() + 1).toString().padStart(2, '0');");
        out.println("    var day = date.getDate().toString().padStart(2, '0');");
        out.println("    return month + '/' + day + '/' + date.getFullYear();");
        out.println("  }");
        out.println("  function ddAppendDateQuickButton(container, label, fieldId, dateValue) {");
        out.println("    if (!container) { return; }");
        out.println("    var button = document.createElement('button');");
        out.println("    button.type = 'button';");
        out.println("    button.className = 'dd-quick-btn';");
        out.println("    button.textContent = label;");
        out.println("    button.addEventListener('click', function() { ddSetDateField(fieldId, dateValue); });");
        out.println("    container.appendChild(button);");
        out.println("  }");
        out.println(
                "  function ddRenderLegacyDateButtons(containerId, fieldId, startOffset, count, includeToday, includeTomorrow, includeEoy) {");
        out.println("    var container = document.getElementById(containerId);");
        out.println("    if (!container) { return; }");
        out.println("    container.innerHTML = '';");
        out.println("    var today = new Date();");
        out.println("    today = new Date(today.getFullYear(), today.getMonth(), today.getDate());");
        out.println("    if (includeToday) {");
        out.println("      ddAppendDateQuickButton(container, 'Today', fieldId, ddFormatEditDate(today));");
        out.println("    }");
        out.println("    var cursor = new Date(today.getFullYear(), today.getMonth(), today.getDate());");
        out.println("    if (startOffset > 0) { cursor.setDate(cursor.getDate() + startOffset); }");
        out.println("    if (includeTomorrow) {");
        out.println("      var tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);");
        out.println("      ddAppendDateQuickButton(container, 'Tomorrow', fieldId, ddFormatEditDate(tomorrow));");
        out.println("      cursor = new Date(tomorrow.getFullYear(), tomorrow.getMonth(), tomorrow.getDate() + 1);");
        out.println("    }");
        out.println("    var nextWeek = false;");
        out.println("    for (var i = 0; i < count; i++) {");
        out.println("      var label = cursor.toLocaleDateString('en-US', { weekday: 'short' });");
        out.println("      if (nextWeek) { label = 'Next-' + label; }");
        out.println("      ddAppendDateQuickButton(container, label, fieldId, ddFormatEditDate(cursor));");
        out.println("      if (cursor.getDay() === 0) { nextWeek = true; }");
        out.println("      cursor = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() + 1);");
        out.println("    }");
        out.println("    if (includeEoy) {");
        out.println("      var eoy = new Date(today.getFullYear(), 11, 31);");
        out.println("      ddAppendDateQuickButton(container, 'EOY', fieldId, ddFormatEditDate(eoy));");
        out.println("    }");
        out.println("  }");
        out.println("  function ddRenderEditDateButtons() {");
        out.println(
                "    ddRenderLegacyDateButtons('ddEditActionDateQuickButtons', 'ddEditActionDate', 0, 6, true, true, true);");
        out.println(
                "    ddRenderLegacyDateButtons('ddEditActionTargetQuickButtons', 'ddEditActionTarget', 2, 8, false, false, false);");
        out.println(
                "    ddRenderLegacyDateButtons('ddEditActionDeadlineQuickButtons', 'ddEditActionDeadline', 2, 8, false, false, false);");
        out.println("  }");
        out.println("  function ddSetDateField(fieldId, dateValue) {");
        out.println("    var field = document.getElementById(fieldId);");
        out.println("    if (field) { field.value = dateValue; }");
        out.println("  }");
        out.println("  function ddUpdateActionTypeButtons(selectedType) {");
        out.println("    var buttons = document.querySelectorAll('.dd-action-type-btn');");
        out.println("    for (var i = 0; i < buttons.length; i++) {");
        out.println("      var button = buttons[i];");
        out.println("      if (button.getAttribute('data-action-type') === selectedType) {");
        out.println("        button.classList.add('dd-action-type-btn-active');");
        out.println("      } else {");
        out.println("        button.classList.remove('dd-action-type-btn-active');");
        out.println("      }");
        out.println("    }");
        out.println("  }");
        out.println("  function ddLoadEditFormData(actionId) {");
        out.println("    document.getElementById('ddEditActionId').value = actionId;");
        out.println("    var formData = new FormData();");
        out.println("    formData.append('action', 'loadActionData');");
        out.println("    formData.append('actionNextId', actionId);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(response => response.json())");
        out.println("      .then(data => {");
        out.println("        if (data.success) {");
        out.println("          var loadedDate = data.nextActionDate || '';");
        out.println("          document.getElementById('ddEditActionDate').value = loadedDate;");
        out.println("          document.getElementById('ddEditActionDateOriginal').value = loadedDate;");
        out.println("          document.getElementById('ddEditActionType').value = data.nextActionType || '';");
        out.println("          ddUpdateActionTypeButtons(data.nextActionType || '');");
        out.println("          document.getElementById('ddEditActionContact').value = data.nextContactId || '';");
        out.println("          document.getElementById('ddEditActionDescription').value = data.nextDescription || '';");
        out.println("          document.getElementById('ddEditActionTime').value = data.nextTimeEstimate || '0';");
        out.println("          document.getElementById('ddEditActionTarget').value = data.nextTargetDate || '';");
        out.println("          document.getElementById('ddEditActionDeadline').value = data.nextDeadlineDate || '';");
        out.println("          document.getElementById('ddEditActionLink').value = data.linkUrl || '';");
        out.println("          document.getElementById('ddEditActionNote').value = data.nextNote || '';");
        out.println("        } else {");
        out.println("          console.log('Error loading action data:', data.message || 'Unknown error');");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(err => console.log('Error loading action data:', err));");
        out.println("  }");
        out.println("  function ddSetActionType(type) {");
        out.println("    document.getElementById('ddEditActionType').value = type;");
        out.println("    ddUpdateActionTypeButtons(type);");
        out.println("  }");
        out.println("  function ddSetTimeEstimate(minutes) {");
        out.println("    document.getElementById('ddEditActionTime').value = minutes;");
        out.println("  }");
        out.println("  function ddSubmitEditActionForm(evt) {");
        out.println("    evt.preventDefault();");
        out.println("    var form = document.getElementById('ddEditActionForm');");
        out.println("    var dateField = document.getElementById('ddEditActionDate');");
        out.println("    var originalDateField = document.getElementById('ddEditActionDateOriginal');");
        out.println(
                "    if (dateField && (!dateField.value || dateField.value.trim().length === 0) && originalDateField && originalDateField.value) {");
        out.println("      dateField.value = originalDateField.value;");
        out.println("    }");
        out.println("    var formData = new FormData(form);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(response => response.json())");
        out.println("      .then(data => {");
        out.println("        if (data.success) {");
        out.println("          ddCloseActionModal('editActionModal');");
        out.println("          window.location.reload();");
        out.println("        } else {");
        out.println("          alert('Error saving action: ' + (data.message || 'Unknown error'));");
        out.println("        }");
        out.println("      })");
        out.println("      .catch(err => {");
        out.println("        alert('Error submitting form. Please try refreshing the page.');");
        out.println("        console.log('Form submission error:', err);");
        out.println("      });");
        out.println("    return false;");
        out.println("  }");
        out.println("  ddRenderEditDateButtons();");
        out.println("</script>");
    }

    private String resolveSelectedTodaySectionId(AppReq appReq, List<TodaySectionRenderModel> sections) {
        List<String> availableIds = new ArrayList<String>();
        for (TodaySectionRenderModel section : sections) {
            if (!section.getItems().isEmpty()) {
                availableIds.add(section.getId());
            }
        }

        String selected = appReq.getRequest().getParameter(TODAY_SECTION_PARAM);
        if (selected != null) {
            selected = selected.trim();
        }

        if (selected != null && selected.length() > 0) {
            if (TODAY_SECTION_ALL.equals(selected)) {
                appReq.getWebSession().removeAttribute(TODAY_SECTION_SESSION_KEY);
                return "";
            }
            if (availableIds.contains(selected)) {
                appReq.getWebSession().setAttribute(TODAY_SECTION_SESSION_KEY, selected);
                return selected;
            }
        }

        Object sessionValue = appReq.getWebSession().getAttribute(TODAY_SECTION_SESSION_KEY);
        if (sessionValue instanceof String) {
            String sessionSelected = ((String) sessionValue).trim();
            if (availableIds.contains(sessionSelected)) {
                return sessionSelected;
            }
            appReq.getWebSession().removeAttribute(TODAY_SECTION_SESSION_KEY);
        }

        return "";
    }

    private void printTodayTotals(PrintWriter out, DashboardTodayColumnModel.TodayTotalsModel totals) {
        out.println("  <div class=\"dd-totals-grid\">");
        out.println("    <div class=\"dd-totals-cell\"><strong>Completed</strong><br/>" + totals.getCompletedDisplay()
                + "</div>");
        out.println("    <div class=\"dd-totals-cell\"><strong>Committed</strong><br/>" + totals.getCommittedDisplay()
                + "</div>");
        out.println("    <div class=\"dd-totals-cell\"><strong>Will</strong><br/>" + totals.getWillDisplay()
                + "</div>");
        out.println("    <div class=\"dd-totals-cell\"><strong>Will Meet</strong><br/>" + totals.getWillMeetDisplay()
                + "</div>");
        out.println("    <div class=\"dd-totals-cell\"><strong>Today Planned</strong><br/>"
                + totals.getTotalPlannedDisplay() + "</div>");
        out.println("  </div>");
        String overClass = totals.isOverCommitted() ? " dd-over-under-over" : "";
        out.println("  <p class=\"dd-over-under" + overClass + "\">" + escapeHtml(totals.getGuidanceMessage())
                + "</p>");
    }

    private void printTodayActionList(PrintWriter out, List<DashboardTodayColumnModel.TodayActionItemModel> items) {
        out.println("  <ul class=\"dd-action-list\">");
        for (DashboardTodayColumnModel.TodayActionItemModel item : items) {
            out.println("    <li class=\"dd-action-item\">");
            if (item.getProjectName() != null && item.getProjectName().length() > 0) {
                out.println("      <div class=\"dd-action-project\">" + escapeHtml(item.getProjectName()) + "</div>");
            }
            out.println("      <div class=\"dd-action-desc\">" + item.getDescriptionHtml() + "</div>");
            out.println("      <div class=\"dd-meta-row\">");
            out.println(
                    "        <span class=\"dd-meta-pill\">Est: " + escapeHtml(item.getEstimateDisplay()) + "</span>");
            out.println("        <span class=\"dd-meta-pill\">Act: " + escapeHtml(item.getActualDisplay()) + "</span>");
            out.println("        <span class=\"dd-meta-pill\">" + escapeHtml(item.getStatusLabel()) + "</span>");
            out.println("      </div>");
            out.println("    </li>");
        }
        out.println("  </ul>");
    }

    private TimeGaugeModel buildInlineTodayGauge(DashboardTodayColumnModel.TodayActionItemModel item) {
        TimeGaugeModel model = new TimeGaugeModel();
        model.setVariant(TimeGaugeVariant.INLINE_BAR_LONG);
        model.setShowTitle(false);
        model.setShowStatus(false);
        int currentMinutes = Math.max(0, item.getActualMinutes());
        int targetMinutes = Math.max(0, item.getEstimateMinutes());

        TimeGaugeState state = TimeGaugeState.UNKNOWN;
        if (targetMinutes > 0) {
            if (currentMinutes > targetMinutes) {
                state = TimeGaugeState.OVER;
            } else {
                int percent = (int) Math.round((currentMinutes * 100.0) / targetMinutes);
                state = percent >= TODAY_GAUGE_WARNING_PERCENT ? TimeGaugeState.WARNING : TimeGaugeState.NORMAL;
            }
        }

        TimeGaugeModel.GaugeRow row = new TimeGaugeModel.GaugeRow(null, currentMinutes, targetMinutes);
        row.setState(state);
        model.addRow(row);

        return model;
    }

    private List<TodaySectionRenderModel> buildTodaySections(DashboardTodayColumnModel todayColumnModel) {
        Map<String, TodaySectionRenderModel> sections = new LinkedHashMap<String, TodaySectionRenderModel>();
        sections.put("committed", new TodaySectionRenderModel("committed", "Committed"));
        sections.put("meetings", new TodaySectionRenderModel("meetings", "Meetings"));
        sections.put("will", new TodaySectionRenderModel("will", "Will"));
        sections.put("start-of-work-day", new TodaySectionRenderModel("start-of-work-day", "Start of Work Day"));
        sections.put("completed", new TodaySectionRenderModel("completed", "Completed"));
        sections.put("end-of-work-day", new TodaySectionRenderModel("end-of-work-day", "End of Work Day"));
        sections.put("overdue", new TodaySectionRenderModel("overdue", "Overdue"));
        sections.put("waiting", new TodaySectionRenderModel("waiting", "Waiting"));
        sections.put("might", new TodaySectionRenderModel("might", "Might"));
        sections.put("personal-morning", new TodaySectionRenderModel("personal-morning", "Personal (Morning)"));
        sections.put("personal-afternoon-evening",
                new TodaySectionRenderModel("personal-afternoon-evening", "Personal (Afternoon/Evening)"));
        sections.put("other", new TodaySectionRenderModel("other", "Other"));

        for (DashboardTodayColumnModel.TodayActionGroupModel group : todayColumnModel.getActionGroups()) {
            String title = group.getTitle();
            String sectionId = "";
            if ("Committed".equals(title)) {
                sectionId = "committed";
            } else if ("Will Meet".equals(title)) {
                sectionId = "meetings";
            } else if ("Will".equals(title)) {
                sectionId = "will";
            } else if ("Start of Work Day".equals(title)) {
                sectionId = "start-of-work-day";
            } else if ("End of Work Day".equals(title)) {
                sectionId = "end-of-work-day";
            } else if ("Overdue".equals(title)) {
                sectionId = "overdue";
            } else if ("Waiting".equals(title)) {
                sectionId = "waiting";
            } else if ("Might".equals(title)) {
                sectionId = "might";
            } else if ("Personal (Wake)".equals(title) || "Personal (Morning)".equals(title)) {
                sectionId = "personal-morning";
            } else if ("Personal (Afternoon & Evening)".equals(title)) {
                sectionId = "personal-afternoon-evening";
            } else if ("Other".equals(title)) {
                sectionId = "other";
            }

            TodaySectionRenderModel section = sections.get(sectionId);
            if (section != null) {
                section.getItems().addAll(group.getItems());
            }
        }

        sections.get("completed").getItems().addAll(todayColumnModel.getCompletedToday());

        return new ArrayList<TodaySectionRenderModel>(sections.values());
    }

    private static class TodaySectionRenderModel {
        private final String id;
        private final String title;
        private final List<DashboardTodayColumnModel.TodayActionItemModel> items = new ArrayList<DashboardTodayColumnModel.TodayActionItemModel>();

        private TodaySectionRenderModel(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public List<DashboardTodayColumnModel.TodayActionItemModel> getItems() {
            return items;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String buildNowHeaderLine(DashboardNowColumnModel nowColumnModel) {
        if (nowColumnModel.getCurrentAction() == null || !nowColumnModel.getCurrentAction().isAvailable()) {
            return "No current action";
        }
        String actionText = safe(nowColumnModel.getCurrentAction().getTitle());
        if (actionText.length() == 0) {
            return "No current action";
        }
        String lower = actionText.toLowerCase();
        if (lower.startsWith("i ")) {
            return actionText;
        }

        String actionType = safe(nowColumnModel.getCurrentAction().getActionTypeLabel());
        if (ProjectNextActionType.COMMITTED_TO.equals(actionType)) {
            return "I have committed to " + actionText;
        }
        if (ProjectNextActionType.MIGHT.equals(actionType)) {
            return "I might " + actionText;
        }
        if (ProjectNextActionType.WILL_MEET.equals(actionType)) {
            return "I will meet " + actionText;
        }
        if (ProjectNextActionType.WILL_CONTACT.equals(actionType)) {
            return "I will contact about " + actionText;
        }
        if (ProjectNextActionType.WILL_REVIEW.equals(actionType)) {
            return "I will review " + actionText;
        }
        if (ProjectNextActionType.WILL_DOCUMENT.equals(actionType)) {
            return "I will document " + actionText;
        }
        if (ProjectNextActionType.WAITING.equals(actionType)) {
            return "I am waiting for " + actionText;
        }
        if (ProjectNextActionType.OVERDUE_TO.equals(actionType)) {
            return "I am overdue to " + actionText;
        }
        if (ProjectNextActionType.GOAL.equals(actionType)) {
            return "I have set a goal to " + actionText;
        }
        if (ProjectNextActionType.WILL_FOLLOW_UP.equals(actionType)) {
            return "I will follow up " + actionText;
        }
        return "I will " + actionText;
    }

    private String buildNowHeaderProject(DashboardNowColumnModel nowColumnModel) {
        if (nowColumnModel.getCurrentProject() == null || !nowColumnModel.getCurrentProject().isAvailable()) {
            return "No project selected";
        }
        return safe(nowColumnModel.getCurrentProject().getName());
    }

    private String buildTodayHeaderLabel(AppReq appReq) {
        return appReq.getWebUser().getDateFormatService().formatPattern(new java.util.Date(), "EEEE dd MMM yyyy",
                appReq.getWebUser().getTimeZone());
    }

    private String buildTodayHeaderCurrentTime(AppReq appReq) {
        return appReq.getWebUser().getDateFormatService().formatPattern(new java.util.Date(), "hh:mm:ss aaa z",
                appReq.getWebUser().getTimeZone());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private void printQuickCaptureScript(PrintWriter out, DashboardTodayColumnModel todayColumnModel) {
        out.println("  <script>");
        out.print("    const projectNames = [");
        List<String> projectNames = todayColumnModel.getQuickCapture().getProjectNames();
        for (int i = 0; i < projectNames.size(); i++) {
            out.print("\"" + escapeJs(projectNames.get(i)) + "\"");
            if (i < projectNames.size() - 1) {
                out.print(", ");
            }
        }
        out.println("];");
        out.println(
                "    const actionVerbs = [\"I will\", \"I have committed\", \"I might\", \"I will meet\", \"I have set goal to\", \"I am waiting\"];");
        out.println("    const input = document.getElementById('sentenceInput');");
        out.println("    const suggestionsBox = document.getElementById('suggestions');");
        out.println("    if (input && suggestionsBox) {");
        out.println("      let currentSuggestions = [];\n      let selectedIndex = -1;");
        out.println("      input.addEventListener('input', function() {");
        out.println("        const text = input.value;\n        const colonIndex = text.indexOf(':');");
        out.println("        let suggestions = [];\n        if (colonIndex === -1) {");
        out.println(
                "          suggestions = projectNames.filter(function(name) { return name.toLowerCase().startsWith(text.toLowerCase()); });");
        out.println("          currentSuggestions = suggestions;");
        out.println("        } else {");
        out.println(
                "          const beforeColon = text.substring(0, colonIndex).trim();\n          const afterColon = text.substring(colonIndex + 1).trim();");
        out.println("          if (projectNames.indexOf(beforeColon) === -1) {");
        out.println(
                "            suggestions = projectNames.filter(function(name) { return name.toLowerCase().indexOf(beforeColon.toLowerCase()) >= 0; });");
        out.println("          } else if (afterColon.length === 0) {");
        out.println("            suggestions = actionVerbs;");
        out.println("          } else {");
        out.println(
                "            suggestions = actionVerbs.filter(function(verb) { return verb.toLowerCase().startsWith(afterColon.toLowerCase()); });");
        out.println("          }");
        out.println("          currentSuggestions = suggestions;");
        out.println("        }");
        out.println("        selectedIndex = -1;\n        showSuggestions(suggestions, text);");
        out.println("      });");
        out.println("      function showSuggestions(suggestions, text) {");
        out.println(
                "        suggestionsBox.innerHTML = '';\n        suggestionsBox.style.display = suggestions.length ? 'block' : 'none';");
        out.println("        suggestions.forEach(function(suggestion, i) {");
        out.println("          const div = document.createElement('div');\n          div.textContent = suggestion;");
        out.println("          if (i === selectedIndex) { div.style.backgroundColor = '#e0e0e0'; }");
        out.println(
                "          div.addEventListener('click', function() {\n            input.value = suggestion;\n            acceptSuggestion(suggestion, text);\n          });");
        out.println("          suggestionsBox.appendChild(div);");
        out.println("        });");
        out.println("      }");
        out.println("      function acceptSuggestion(suggestion, text) {");
        out.println("        if (text.indexOf(':') === -1) {");
        out.println("          input.value = suggestion + ': ';");
        out.println("        } else {");
        out.println(
                "          const beforeColon = text.split(':')[0];\n          input.value = beforeColon.trim() + ': ' + suggestion + ' ';");
        out.println("        }");
        out.println("        suggestionsBox.style.display = 'none';\n        selectedIndex = -1;");
        out.println("      }");
        out.println("      input.addEventListener('keydown', function(e) {");
        out.println("        const visible = suggestionsBox.style.display === 'block';");
        out.println("        if (visible && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {");
        out.println(
                "          e.preventDefault();\n          const count = currentSuggestions.length;\n          if (count === 0) { return; }");
        out.println(
                "          if (e.key === 'ArrowDown') { selectedIndex = (selectedIndex + 1) % count; } else { selectedIndex = (selectedIndex - 1 + count) % count; }");
        out.println("          showSuggestions(currentSuggestions, input.value);");
        out.println("        }");
        out.println("        if (visible && (e.key === 'Enter' || e.key === 'Tab')) {");
        out.println("          if (selectedIndex < 0) { selectedIndex = 0; }");
        out.println(
                "          if (selectedIndex < currentSuggestions.length) { e.preventDefault(); acceptSuggestion(currentSuggestions[selectedIndex], input.value); }");
        out.println("        }");
        out.println("        if (e.key === 'Escape') { suggestionsBox.style.display = 'none'; selectedIndex = -1; }");
        out.println("      });");
        out.println(
                "      input.addEventListener('blur', function() { setTimeout(function() { suggestionsBox.style.display = 'none'; }, 150); });");
        out.println("    }");
        out.println("  </script>");
    }

    private String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void printNextColumn(PrintWriter out, DashboardNextColumnModel nextColumnModel) {
        out.println("<div class=\"dd-section dd-panel dd-panel-open\">");
        printDevLabel(out, "NEXT DAY CHIPS");
        if (nextColumnModel.getDaySummaries().isEmpty()) {
            out.println("  <p class=\"dd-subtle\">No planned days found in the next 2 weeks.</p>");
        } else {
            out.println("  <div class=\"dd-next-day-chips\">");
            for (DashboardNextColumnModel.NextDaySummaryModel daySummary : nextColumnModel.getDaySummaries()) {
                String chipClass = "dd-next-day-chip" + (daySummary.isSelected() ? " dd-next-day-chip-selected" : "");
                String dayLink = "DandelionDashboardServlet?" + DashboardNextColumnModel.getSelectedDayParam()
                        + "=" + escapeHtml(daySummary.getDayKey());
                out.println("    <a class=\"" + chipClass + "\" href=\"" + dayLink + "\">");
                out.println("      <span class=\"dd-next-day-chip-title\">"
                        + escapeHtml(daySummary.getDayShortLabel()) + "</span>");
                out.println("      <span class=\"dd-next-day-chip-date\">"
                        + escapeHtml(daySummary.getPlannedDisplay()) + " planned</span>");
                out.println("    </a>");
            }
            out.println("  </div>");
        }
        out.println("</div>");

        out.println("<div id=\"dd-next-day-detail\" class=\"dd-section dd-panel dd-panel-open\">");
        printDevLabel(out, "SELECTED DAY DETAILS");
        if (nextColumnModel.getSelectedDay().getDayKey() == null
                || nextColumnModel.getSelectedDay().getDayKey().length() == 0) {
            out.println("  <p class=\"dd-subtle\">Select a day above to see its actions.</p>");
        } else {
            if (nextColumnModel.getSelectedDay().getSections().isEmpty()) {
                out.println("  <p class=\"dd-subtle\">No actions found for the selected day.</p>");
            } else {
                for (DashboardNextColumnModel.SelectedDaySectionModel section : nextColumnModel.getSelectedDay()
                        .getSections()) {
                    out.println("  <h3 class=\"dd-next-section-title\">"
                            + escapeHtml(section.getTitle()) + "</h3>");
                    out.println("  <table class=\"dd-today-table\">");
                    out.println("    <tr>");
                    out.println("      <th class=\"dd-next-table-desc\">Description</th>");
                    out.println("      <th class=\"dd-next-table-time\">Time</th>");
                    out.println("      <th class=\"dd-next-table-action\">Action</th>");
                    out.println("    </tr>");
                    for (DashboardNextColumnModel.SelectedDayActionItemModel item : section.getItems()) {
                        out.println("    <tr>");
                        out.println(
                                "      <td class=\"dd-next-table-desc\"><a href=\"javascript:void(0);\" class=\"dd-next-desc-link\" onclick=\"ddOpenNextTaskDetails("
                                        + item.getActionNextId() + ", event)\">"
                                        + escapeHtml(truncateForTable(item.getDescriptionPlain(), 60)) + "</a></td>");
                        out.println("      <td class=\"dd-next-table-time\">" + escapeHtml(item.getEstimateDisplay())
                                + "</td>");
                        out.println("      <td class=\"dd-next-table-action\">"
                                + "<span class=\"dd-row-actions\">"
                                + "<button class=\"dd-row-action-btn\" title=\"Reschedule\" onclick=\"ddOpenActionModal('rescheduleModal',"
                                + item.getActionNextId() + ",event)\">📅</button>"
                                + "<button class=\"dd-row-action-btn\" title=\"Edit action\" onclick=\"ddOpenActionModal('editActionModal',"
                                + item.getActionNextId() + ",event)\">✏️</button>"
                                + "</span></td>");
                        out.println("    </tr>");
                    }
                    out.println("  </table>");
                }
            }
        }
        out.println("</div>");

        out.println(
                "<div id=\"ddNextTaskDetailModal\" class=\"dd-modal-overlay\" onclick=\"ddOverlayClose(event,this)\">");
        out.println("  <div class=\"dd-modal\" onclick=\"event.stopPropagation()\">");
        out.println("    <div class=\"dd-modal-head\">");
        out.println("      <h3 class=\"dd-modal-title\">Task Details</h3>");
        out.println(
                "      <button class=\"dd-modal-close\" onclick=\"ddCloseActionModal('ddNextTaskDetailModal',event)\">&times;</button>");
        out.println("    </div>");
        out.println("    <div class=\"dd-modal-body\">");
        out.println("      <div class=\"dd-task-detail-grid\">");
        out.println(
                "        <div class=\"dd-task-detail-label\">Description</div><div id=\"ddNextTaskDetailDescription\" class=\"dd-task-detail-value\"></div>");
        out.println(
                "        <div class=\"dd-task-detail-label\">Project</div><div id=\"ddNextTaskDetailProject\" class=\"dd-task-detail-value\"></div>");
        out.println(
                "        <div class=\"dd-task-detail-label\">Date Due</div><div id=\"ddNextTaskDetailDateDue\" class=\"dd-task-detail-value\"></div>");
        out.println(
                "        <div class=\"dd-task-detail-label\">Notes</div><div id=\"ddNextTaskDetailNotes\" class=\"dd-task-detail-value\"></div>");
        out.println(
                "        <div class=\"dd-task-detail-label\">Links</div><div id=\"ddNextTaskDetailLinks\" class=\"dd-task-detail-value\"></div>");
        out.println(
                "        <div class=\"dd-task-detail-label\">Deadline Date</div><div id=\"ddNextTaskDetailDeadline\" class=\"dd-task-detail-value\"></div>");
        out.println("      </div>");
        out.println("      <div class=\"dd-form-actions\" style=\"margin-top:12px;\">");
        out.println(
                "        <button type=\"button\" class=\"dd-btn dd-btn-primary\" onclick=\"ddOpenEditFromTaskDetail(event)\">Edit</button>");
        out.println("      </div>");
        out.println("    </div>");
        out.println("  </div>");
        out.println("</div>");

        out.println("<script>");
        out.println("  window.ddNextTaskDetailActionId = null;");
        out.println("  function ddDetailText(id, value) {");
        out.println("    var el = document.getElementById(id);");
        out.println("    if (!el) { return; }");
        out.println("    el.classList.remove('dd-task-detail-empty');");
        out.println("    el.textContent = value && value.length > 0 ? value : '-';");
        out.println("    if (!value || value.length === 0) { el.classList.add('dd-task-detail-empty'); }");
        out.println("  }");
        out.println("  function ddOpenNextTaskDetails(actionId, evt) {");
        out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
        out.println("    window.ddNextTaskDetailActionId = actionId;");
        out.println("    var formData = new FormData();");
        out.println("    formData.append('action', 'loadActionData');");
        out.println("    formData.append('actionNextId', actionId);");
        out.println("    fetch('DandelionDashboardServlet', { method: 'POST', body: formData })");
        out.println("      .then(function(response) { return response.json(); })");
        out.println("      .then(function(data) {");
        out.println("        if (!data || !data.success) { alert('Unable to load details'); return; }");
        out.println("        ddDetailText('ddNextTaskDetailDescription', data.nextDescription || '');");
        out.println("        ddDetailText('ddNextTaskDetailProject', data.projectName || '');");
        out.println("        ddDetailText('ddNextTaskDetailDateDue', data.nextActionDate || '');");
        out.println("        ddDetailText('ddNextTaskDetailNotes', data.nextNote || '');");
        out.println("        ddDetailText('ddNextTaskDetailDeadline', data.nextDeadlineDate || '');");
        out.println("        var linksEl = document.getElementById('ddNextTaskDetailLinks');");
        out.println("        if (linksEl) {");
        out.println("          linksEl.classList.remove('dd-task-detail-empty');");
        out.println("          linksEl.innerHTML = '';");
        out.println("          if (data.linkUrl && data.linkUrl.length > 0) {");
        out.println("            var a = document.createElement('a');");
        out.println("            a.href = data.linkUrl;");
        out.println("            a.target = '_blank';");
        out.println("            a.rel = 'noopener noreferrer';");
        out.println("            a.textContent = data.linkUrl;");
        out.println("            linksEl.appendChild(a);");
        out.println("          } else {");
        out.println("            linksEl.textContent = '-';");
        out.println("            linksEl.classList.add('dd-task-detail-empty');");
        out.println("          }");
        out.println("        }");
        out.println("        ddOpenActionModal('ddNextTaskDetailModal', actionId, evt);");
        out.println("      })");
        out.println("      .catch(function() { alert('Unable to load details'); });");
        out.println("    return false;");
        out.println("  }");
        out.println("  function ddOpenEditFromTaskDetail(evt) {");
        out.println("    if (evt) { evt.preventDefault(); evt.stopPropagation(); }");
        out.println("    if (!window.ddNextTaskDetailActionId) { return false; }");
        out.println("    ddCloseActionModal('ddNextTaskDetailModal', evt);");
        out.println("    ddOpenActionModal('editActionModal', window.ddNextTaskDetailActionId, evt);");
        out.println("    return false;");
        out.println("  }");
        out.println("</script>");
    }

    private String truncateForTable(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    private void printDevLabel(PrintWriter out, String label) {
        if (!DEV_LABELS_ENABLED) {
            return;
        }
        out.println("  <span class=\"dd-dashboard-dev-label\">" + escapeHtml(label) + "</span>");
    }
}