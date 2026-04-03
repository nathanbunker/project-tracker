package org.dandeliondaily.dashboard.render;

import java.io.PrintWriter;

import org.dandeliondaily.dashboard.model.TimeGaugeModel;
import org.dandeliondaily.dashboard.model.TimeGaugeVariant;

public class TimeGaugeRenderer {

    public void printStyles(PrintWriter out) {
        out.println("  .dd-time-gauge {");
        out.println("    background: #ffffff;");
        out.println("    border: 1px solid #d8e0d4;");
        out.println("    border-radius: 8px;");
        out.println("    padding: 8px;");
        out.println("    color: #2f4234;");
        out.println("    box-shadow: 0 1px 0 rgba(255, 255, 255, 0.9) inset;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-gauge-inline {");
        out.println("    display: inline-flex;");
        out.println("    align-items: center;");
        out.println("    gap: 8px;");
        out.println("    padding: 4px 8px;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-gauge-inline-bar-long {");
        out.println("    display: flex;");
        out.println("    flex-direction: column;");
        out.println("    padding: 6px;");
        out.println("    gap: 0;");
        out.println("    border: none;");
        out.println("    background: transparent;");
        out.println("    box-shadow: none;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-gauge-today-header {");
        out.println("    display: flex;");
        out.println("    flex-direction: column;");
        out.println("    padding: 8px;");
        out.println("    gap: 0;");
        out.println("  }");
        out.println("  .dd-time-gauge-title {");
        out.println("    margin: 0 0 3px 0;");
        out.println("    font-size: 10px;");
        out.println("    font-weight: bold;");
        out.println("    letter-spacing: 0.04em;");
        out.println("    text-transform: uppercase;");
        out.println("    color: #556a59;");
        out.println("  }");
        out.println("  .dd-time-gauge-ratio {");
        out.println("    font-size: 12px;");
        out.println("    font-weight: bold;");
        out.println("    margin: 0 0 6px 0;");
        out.println("  }");
        out.println("  .dd-time-gauge-bar-wrap {");
        out.println("    height: 8px;");
        out.println("    border-radius: 5px;");
        out.println("    background: #eef2ec;");
        out.println("    overflow: hidden;");
        out.println("  }");
        out.println("  .dd-time-gauge-bar {");
        out.println("    height: 100%;");
        out.println("    width: 0%;");
        out.println("    border-radius: 5px;");
        out.println("    background: #4f8b63;");
        out.println("  }");
        out.println("  .dd-time-gauge-status {");
        out.println("    margin-top: 5px;");
        out.println("    font-size: 11px;");
        out.println("    color: #58725f;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-state-warning .dd-time-gauge-bar {");
        out.println("    background: #c78b34;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-state-warning .dd-time-gauge-status {");
        out.println("    color: #8a5d15;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-state-over .dd-time-gauge-bar {");
        out.println("    background: #b24a4a;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-state-over .dd-time-gauge-status {");
        out.println("    color: #8f2f2f;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-state-unknown .dd-time-gauge-bar {");
        out.println("    background: #92a59b;");
        out.println("  }");
        out.println("  .dd-time-gauge.dd-state-unknown .dd-time-gauge-status {");
        out.println("    color: #5f6f67;");
        out.println("  }");
        out.println("  .dd-gauge-inline-bar-long .dd-time-gauge-row {");
        out.println("    display: flex;");
        out.println("    align-items: center;");
        out.println("    gap: 6px;");
        out.println("    margin-bottom: 4px;");
        out.println("    font-size: 11px;");
        out.println("  }");
        out.println("  .dd-gauge-inline-bar-long .dd-time-gauge-row:last-child {");
        out.println("    margin-bottom: 0;");
        out.println("  }");
        out.println("  .dd-gauge-inline-bar-long .dd-time-gauge-ratio {");
        out.println("    min-width: 70px;");
        out.println("    font-size: 11px;");
        out.println("    font-weight: bold;");
        out.println("    margin: 0;");
        out.println("  }");
        out.println("  .dd-gauge-inline-bar-long .dd-time-gauge-bar-wrap {");
        out.println("    flex: 1;");
        out.println("    height: 6px;");
        out.println("  }");
        out.println("  .dd-gauge-today-header .dd-time-gauge-row {");
        out.println("    display: flex;");
        out.println("    align-items: center;");
        out.println("    gap: 8px;");
        out.println("    margin-bottom: 8px;");
        out.println("    font-size: 12px;");
        out.println("  }");
        out.println("  .dd-gauge-today-header .dd-time-gauge-row:last-child {");
        out.println("    margin-bottom: 0;");
        out.println("  }");
        out.println("  .dd-gauge-today-header .dd-time-gauge-row-label {");
        out.println("    flex: 0 0 58px;");
        out.println("    font-weight: bold;");
        out.println("    color: #3d4f41;");
        out.println("    text-align: right;");
        out.println("  }");
        out.println("  .dd-gauge-today-header .dd-time-gauge-ratio {");
        out.println("    flex: 0 0 88px;");
        out.println("    font-size: 12px;");
        out.println("    font-weight: bold;");
        out.println("    margin: 0;");
        out.println("  }");
        out.println("  .dd-gauge-today-header .dd-time-gauge-bar-wrap {");
        out.println("    flex: 1;");
        out.println("    height: 8px;");
        out.println("    min-width: 40px;");
        out.println("  }");
    }

    public void render(PrintWriter out, TimeGaugeModel model) {
        if (model.getVariant() == TimeGaugeVariant.INLINE_BAR_LONG) {
            renderInlineBarLong(out, model);
        } else if (model.getVariant() == TimeGaugeVariant.TODAY_HEADER) {
            renderTodayHeader(out, model);
        } else {
            renderStandardGauge(out, model);
        }
    }

    private void renderTodayHeader(PrintWriter out, TimeGaugeModel model) {
        out.println("<div class=\"dd-time-gauge dd-gauge-today-header\">");
        for (TimeGaugeModel.GaugeRow row : model.getRows()) {
            String stateClass = "dd-state-" + row.getState().name().toLowerCase();
            out.println("  <div class=\"dd-time-gauge-row " + stateClass + "\">");
            if (row.getLabel() != null && row.getLabel().length() > 0) {
                out.println("    <span class=\"dd-time-gauge-row-label\">" + escapeHtml(row.getLabel()) + "</span>");
            }
            String ratioText = model.isShowTargetRange()
                    ? formatDuration(row.getCurrentMinutes()) + " / " + formatTarget(row.getTargetMinutes())
                    : formatDuration(row.getCurrentMinutes());
            out.println("    <span class=\"dd-time-gauge-ratio\">" + ratioText + "</span>");
            int fillPercent = calculateFillPercent(row.getCurrentMinutes(), row.getTargetMinutes());
            out.println("    <div class=\"dd-time-gauge-bar-wrap\">\n"
                    + "      <div class=\"dd-time-gauge-bar\" style=\"width: " + fillPercent + "%;\"></div>\n"
                    + "    </div>");
            out.println("  </div>");
        }
        out.println("</div>");
    }

    private void renderInlineBarLong(PrintWriter out, TimeGaugeModel model) {
        if (model.getRows().isEmpty()) {
            return;
        }
        out.println("<div class=\"dd-time-gauge dd-gauge-inline-bar-long\">");
        for (TimeGaugeModel.GaugeRow row : model.getRows()) {
            String stateClass = "dd-state-" + row.getState().name().toLowerCase();
            out.println("  <div class=\"dd-time-gauge-row " + stateClass + "\">");
            String ratioText = model.isShowTargetRange()
                    ? formatDuration(row.getCurrentMinutes()) + " / " + formatTarget(row.getTargetMinutes())
                    : formatDuration(row.getCurrentMinutes());
            out.println("    <span class=\"dd-time-gauge-ratio\">" + ratioText + "</span>");
            int fillPercent = calculateFillPercent(row.getCurrentMinutes(), row.getTargetMinutes());
            out.println("    <div class=\"dd-time-gauge-bar-wrap\">\n"
                    + "      <div class=\"dd-time-gauge-bar\" style=\"width: " + fillPercent + "%;\"></div>\n"
                    + "    </div>");
            out.println("  </div>");
        }
        out.println("</div>");
    }

    private void renderStandardGauge(PrintWriter out, TimeGaugeModel model) {
        if (model.getTargetMinutes() <= 0) {
            return;
        }
        String variantClass = model.getVariant() == TimeGaugeVariant.INLINE ? "dd-gauge-inline" : "dd-gauge-stacked";
        String stateClass = "dd-state-" + model.getState().name().toLowerCase();
        out.println("<div class=\"dd-time-gauge " + variantClass + " " + stateClass + "\">");

        if (model.isShowTitle() && model.hasTitle()) {
            out.println("  <div class=\"dd-time-gauge-title\">" + escapeHtml(model.getTitle()) + "</div>");
        }

        String ratioText = model.isShowTargetRange()
                ? formatDuration(model.getCurrentMinutes()) + " / " + formatTarget(model.getTargetMinutes())
                : formatDuration(model.getCurrentMinutes());
        out.println("  <div class=\"dd-time-gauge-ratio\">" + ratioText + "</div>");

        int fillPercent = calculateFillPercent(model.getCurrentMinutes(), model.getTargetMinutes());
        out.println("  <div class=\"dd-time-gauge-bar-wrap\">\n"
                + "    <div class=\"dd-time-gauge-bar\" style=\"width: " + fillPercent + "%;\"></div>\n"
                + "  </div>");

        if (model.isShowStatus() && model.getStatusText() != null && model.getStatusText().trim().length() > 0) {
            out.println("  <div class=\"dd-time-gauge-status\">" + escapeHtml(model.getStatusText()) + "</div>");
        }

        out.println("</div>");
    }

    private int calculateFillPercent(int currentMinutes, int targetMinutes) {
        if (targetMinutes <= 0) {
            return 100;
        }
        int percent = (int) Math.round((currentMinutes * 100.0) / targetMinutes);
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        return percent;
    }

    private String formatTarget(int targetMinutes) {
        if (targetMinutes <= 0) {
            return "?";
        }
        return formatDuration(targetMinutes);
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "0m";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "m";
        }
        if (minutes == 0) {
            return hours + "h";
        }
        return hours + "h " + minutes + "m";
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
}