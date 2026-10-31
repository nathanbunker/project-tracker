package org.dandeliondaily.shared.render;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dandeliondaily.dashboard.service.DashboardTodayColumnService;
import org.openimmunizationsoftware.pt.SoftwareVersion;

/**
 * The one Quick Capture box, used by every page. Prints the form and loads
 * {@code css/quick-capture.css} and {@code js/quick-capture.js} (project and verb suggestions).
 * The form posts {@code sentenceInput} with {@code action} set to "Schedule" (Enter) or
 * "Schedule and Edit", which the page passes to
 * {@link DashboardTodayColumnService#handleQuickCapture}; for "Schedule and Edit" the page then
 * renders the {@link ActionEditorRenderer} box open on the saved action.
 */
public class QuickCaptureRenderer {

        public static class Config {
                private final String formAction;
                private final Map<String, String> hiddenFields = new LinkedHashMap<String, String>();
                private String sentenceValue = "";
                private List<String> projectNames = Collections.emptyList();
                private boolean focusRequested;
                private String boxClass = "";

                public Config(String formAction) {
                        this.formAction = formAction;
                }

                /** A hidden field the page needs posted back, such as Plan Ahead's window start. */
                public Config hidden(String name, String value) {
                        hiddenFields.put(name, value);
                        return this;
                }

                public Config sentenceValue(String sentenceValue) {
                        this.sentenceValue = sentenceValue == null ? "" : sentenceValue;
                        return this;
                }

                public Config projectNames(List<String> projectNames) {
                        this.projectNames = projectNames == null ? Collections.<String>emptyList() : projectNames;
                        return this;
                }

                public Config focusRequested(boolean focusRequested) {
                        this.focusRequested = focusRequested;
                        return this;
                }

                /** Extra class on the box, for the page's placement (width, position). */
                public Config boxClass(String boxClass) {
                        this.boxClass = boxClass == null ? "" : boxClass;
                        return this;
                }
        }

        public void render(PrintWriter out, Config config) {
                String version = escapeHtml(SoftwareVersion.VERSION);
                out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/quick-capture.css?v=" + version + "\" />");
                out.println("<div class=\"qc-box" + (config.boxClass.length() == 0 ? "" : " " + escapeHtml(config.boxClass))
                                + "\">");
                out.println("  <div class=\"qc-title\">Quick Capture</div>");
                out.println("  <form class=\"qc-form\" method=\"POST\" action=\"" + escapeHtml(config.formAction) + "\">");
                for (Map.Entry<String, String> field : config.hiddenFields.entrySet()) {
                        out.println("    <input type=\"hidden\" name=\"" + escapeHtml(field.getKey()) + "\" value=\""
                                        + escapeHtml(field.getValue()) + "\" />");
                }
                out.println("    <div class=\"qc-row\">");
                out.println("      <div class=\"qc-input-wrap\">");
                out.println("        <input type=\"text\" id=\"sentenceInput\" class=\"qc-input\" name=\"sentenceInput\" value=\""
                                + escapeHtml(config.sentenceValue)
                                + "\" placeholder=\"Project Handle: I will action details\" autocomplete=\"off\""
                                + (config.focusRequested ? " autofocus=\"autofocus\"" : "") + " />");
                out.println("        <div id=\"qcSuggestions\" class=\"qc-suggestions\"></div>");
                out.println("      </div>");
                out.println("      <div class=\"qc-actions\">");
                out.println("        <button type=\"submit\" name=\"action\" value=\"Schedule\" class=\"qc-btn qc-btn-primary\""
                                + " title=\"Save it (Enter)\">Schedule</button>");
                out.println("        <button type=\"submit\" name=\"action\" value=\""
                                + DashboardTodayColumnService.ACTION_SCHEDULE_AND_EDIT + "\" class=\"qc-btn\""
                                + " title=\"Save it, then add a note, dates or other details\">Edit Action</button>");
                out.println("      </div>");
                out.println("    </div>");
                out.println("  </form>");
                out.print("  <script type=\"application/json\" id=\"qcProjectNames\">[");
                for (int i = 0; i < config.projectNames.size(); i++) {
                        out.print((i == 0 ? "" : ",") + jsonString(config.projectNames.get(i)));
                }
                out.println("]</script>");
                out.println("</div>");
                out.println("<script src=\"js/quick-capture.js?v=" + version + "\"></script>");
        }

        private String jsonString(String value) {
                StringBuilder sb = new StringBuilder("\"");
                String s = value == null ? "" : value;
                for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        if (c == '"' || c == '\\') {
                                sb.append('\\').append(c);
                        } else if (c == '<') {
                                sb.append("\\u003c");
                        } else if (c < 0x20) {
                                sb.append(String.format("\\u%04x", (int) c));
                        } else {
                                sb.append(c);
                        }
                }
                return sb.append('"').toString();
        }

        private String escapeHtml(String value) {
                if (value == null) {
                        return "";
                }
                return value.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }
}
