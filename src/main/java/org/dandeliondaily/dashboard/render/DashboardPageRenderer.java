package org.dandeliondaily.dashboard.render;

import java.io.PrintWriter;

import org.openimmunizationsoftware.pt.AppReq;

public class DashboardPageRenderer {

    public void render(AppReq appReq) {
        PrintWriter out = appReq.getOut();

        printStyles(out);

        out.println("<div class=\"dd-dashboard-page\">");
        out.println("  <div class=\"dd-dashboard-intro\">");
        out.println("    <p><a href=\"HomeServlet\">Dandelion</a> &raquo; Dandelion Dashboard</p>");
        out.println("    <h1>Dandelion Dashboard</h1>");
        out.println(
                "    <p>This bootstrap page is a visual starting point for the future dashboard replacement path.</p>");
        out.println("  </div>");

        out.println("  <div class=\"dd-dashboard-shell\">");
        out.println("    <div class=\"dd-dashboard-header\">");
        out.println("      <div class=\"dd-header-cell dd-header-now\">Now</div>");
        out.println("      <div class=\"dd-header-cell dd-header-today\">Today</div>");
        out.println("      <div class=\"dd-header-cell dd-header-next\">Next</div>");
        out.println("    </div>");

        out.println("    <div class=\"dd-dashboard-columns\">");
        out.println("      <div class=\"dd-dashboard-column dd-dashboard-column-now\">");
        out.println("        <!-- Bootstrap placeholder section: future location for real data wiring. -->");
        printNowColumn(out);
        out.println("      </div>");
        out.println("      <div class=\"dd-dashboard-column dd-dashboard-column-today\">");
        out.println(
                "        <!-- Bootstrap placeholder section: future replacement path for ProjectActionServlet logic. -->");
        printTodayColumn(out);
        out.println("      </div>");
        out.println("      <div class=\"dd-dashboard-column dd-dashboard-column-next\">");
        out.println("        <!-- Bootstrap placeholder section: future location for planning and summary data. -->");
        printNextColumn(out);
        out.println("      </div>");
        out.println("    </div>");
        out.println("  </div>");
        out.println("</div>");
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
        out.println("  .dd-section {");
        out.println("    margin-bottom: 16px;");
        out.println("    padding: 14px 14px 10px 14px;");
        out.println("    border: 1px solid #cfbea6;");
        out.println("    background: #fffdf8;");
        out.println("    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);");
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

    private void printNowColumn(PrintWriter out) {
        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Current Action</h2>");
        out.println("  <span class=\"dd-emphasis\">Review project plan</span>");
        out.println("  <p><span class=\"dd-status-chip\">Placeholder</span></p>");
        out.println("  <p>Current action: Review project plan and confirm the next milestone note.</p>");
        out.println("</div>");

        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Current Project</h2>");
        out.println("  <p>Project: Dandelion Dashboard Bootstrap</p>");
        out.println("  <p>Focus: establish structure and visual layout before real workflow wiring.</p>");
        out.println("</div>");

        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Project Notes / Backlog</h2>");
        out.println("  <ul>");
        out.println("    <li>Keep this page independent from legacy action workflow logic.</li>");
        out.println("    <li>Preserve direct HTML servlet rendering for now.</li>");
        out.println("    <li>Replace placeholder cards with real data in later slices.</li>");
        out.println("  </ul>");
        out.println("</div>");
    }

    private void printTodayColumn(PrintWriter out) {
        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Quick Capture</h2>");
        out.println("  <div class=\"dd-capture-box\">");
        out.println("    Placeholder capture area for quick notes, action intake, or summary drafting.");
        out.println("  </div>");
        out.println("</div>");

        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Today&#39;s Actions</h2>");
        out.println("  <p><strong>Today total:</strong> 6h 30m planned</p>");
        out.println("  <ul>");
        out.println("    <li>9:00 AM - Review project plan</li>");
        out.println("    <li>10:30 AM - Draft stakeholder update</li>");
        out.println("    <li>1:00 PM - Prepare implementation notes</li>");
        out.println("    <li>3:30 PM - Outline next few days</li>");
        out.println("  </ul>");
        out.println("</div>");

        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Completed Today</h2>");
        out.println("  <ul>");
        out.println("    <li>Checked deployment notes</li>");
        out.println("    <li>Validated dashboard bootstrap scope</li>");
        out.println("    <li>Captured follow-up ideas for later wiring</li>");
        out.println("  </ul>");
        out.println("</div>");
    }

    private void printNextColumn(PrintWriter out) {
        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Next Few Days Summary</h2>");
        out.println("  <table class=\"dd-planning-table\">");
        out.println("    <tr><th>Day</th><th>Focus</th><th>Planned</th></tr>");
        out.println("    <tr><td>Thu</td><td>Layout review</td><td>3h 00m</td></tr>");
        out.println("    <tr><td>Fri</td><td>Data wiring outline</td><td>4h 15m</td></tr>");
        out.println("    <tr><td>Mon</td><td>Legacy mapping review</td><td>2h 30m</td></tr>");
        out.println("  </table>");
        out.println("</div>");

        out.println("<div class=\"dd-section\">");
        out.println("  <h2>Selected Day Details</h2>");
        out.println("  <p>Friday is currently selected for placeholder planning detail.</p>");
        out.println("  <ul>");
        out.println("    <li>Capture real dashboard section requirements</li>");
        out.println("    <li>Identify minimal first data sources</li>");
        out.println("    <li>Keep replacement steps incremental</li>");
        out.println("  </ul>");
        out.println("</div>");
    }
}