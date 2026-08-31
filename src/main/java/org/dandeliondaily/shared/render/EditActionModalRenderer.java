package org.dandeliondaily.shared.render;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.ProjectContact;
import org.openimmunizationsoftware.pt.model.WebUser;

public class EditActionModalRenderer {

        public static class Config {
                public String overlayId;
                public String overlayClass;
                public String overlayOnClick;
                public String modalClass;
                public String closeOnClick;
                public String saveOnClick;
                public String saveAndStartOnClick;
                public String cancelOnClick;
                public String deleteOnClick;
                /**
                 * When set, a "Complete" button and the backdated completion row are rendered.
                 */
                public String completeOnClick;
                public String completeSubmitOnClick;
        }

        public void render(PrintWriter out, AppReq appReq, Config config) {
                out.println("<div id=\"" + n(config.overlayId) + "\" class=\"" + n(config.overlayClass)
                                + "\" onclick=\"" + n(config.overlayOnClick) + "\">");
                out.println("  <div class=\"" + n(config.modalClass) + "\" onclick=\"event.stopPropagation()\">");
                out.println("    <div class=\"ea-modal-head\">");
                out.println("      <h3 class=\"ea-modal-title\">Edit Action</h3>");
                out.println("      <button type=\"button\" class=\"ea-modal-close\" onclick=\"" + n(config.closeOnClick)
                                + "\">&times;</button>");
                out.println("    </div>");
                out.println("    <div class=\"ea-modal-body\">");
                out.println("      <input type=\"hidden\" id=\"eaEditActionId\" value=\"\" />");
                out.println("      <input type=\"hidden\" id=\"eaEditActionDateOriginal\" value=\"\" />");
                out.println("      <div class=\"ea-modal-field\">");
                out.println("        <label for=\"eaEditActionDate\">When</label>");
                out.println("        <input type=\"text\" id=\"eaEditActionDate\" />");
                out.println("      </div>");

                out.println("      <div id=\"eaEditActionTypeSection\" class=\"ea-modal-field\">");
                out.println("        <label>Action Type</label>");
                out.println("        <div class=\"ea-action-type-chips\">");
                out.println(
                                "          <button type=\"button\" data-action-type=\"WILL\" onclick=\"eaSetActionType('WILL')\" class=\"ea-chip ea-action-type-btn\">will</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"MIGHT\" onclick=\"eaSetActionType('MIGHT')\" class=\"ea-chip ea-action-type-btn\">might</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"WOULD_LIKE_TO\" onclick=\"eaSetActionType('WOULD_LIKE_TO')\" class=\"ea-chip ea-action-type-btn\">would like to</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"WILL_CONTACT\" onclick=\"eaSetActionType('WILL_CONTACT')\" class=\"ea-chip ea-action-type-btn\">will contact</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"WILL_MEET\" onclick=\"eaSetActionType('WILL_MEET')\" class=\"ea-chip ea-action-type-btn\">will meet</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"REVIEW\" onclick=\"eaSetActionType('REVIEW')\" class=\"ea-chip ea-action-type-btn\">will review</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"DOCUMENT\" onclick=\"eaSetActionType('DOCUMENT')\" class=\"ea-chip ea-action-type-btn\">will document</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"WILL_FOLLOW_UP\" onclick=\"eaSetActionType('WILL_FOLLOW_UP')\" class=\"ea-chip ea-action-type-btn\">will follow up</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"COMMITTED_TO\" onclick=\"eaSetActionType('COMMITTED_TO')\" class=\"ea-chip ea-action-type-btn\">committed</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"GOAL\" onclick=\"eaSetActionType('GOAL')\" class=\"ea-chip ea-action-type-btn\">set goal</button>");
                out.println(
                                "          <button type=\"button\" data-action-type=\"WAITING\" onclick=\"eaSetActionType('WAITING')\" class=\"ea-chip ea-action-type-btn\">waiting</button>");
                out.println("          <input type=\"hidden\" id=\"eaEditActionType\" value=\"\" />");
                out.println("        </div>");
                out.println("      </div>");

                out.println("      <div id=\"eaEditActionSlotSection\" class=\"ea-modal-field\" style=\"display:none\">");
                out.println("        <label for=\"eaEditActionTimeSlot\">Time of Day</label>");
                out.println("        <select id=\"eaEditActionTimeSlot\">");
                out.println("          <option value=\"WAKE\">Wake</option>");
                out.println("          <option value=\"MORNING\">Morning</option>");
                out.println("          <option value=\"AFTERNOON\">Afternoon</option>");
                out.println("          <option value=\"EVENING\">Evening</option>");
                out.println("        </select>");
                out.println("      </div>");

                out.println("      <div class=\"ea-modal-field\">");
                out.println("        <label for=\"eaEditActionDescription\">Description</label>");
                out.println("        <textarea id=\"eaEditActionDescription\" rows=\"2\"></textarea>");
                out.println("      </div>");

                out.println("      <div id=\"eaEditActionContactSection\" class=\"ea-modal-field\">");
                out.println("        <label for=\"eaEditActionContact\" class=\"ea-contact-label\">Who</label>");
                out.println("        <select id=\"eaEditActionContact\">");
                out.println("          <option value=\"\">none</option>");

                Session dataSession = appReq.getDataSession();
                WebUser webUser = appReq.getWebUser();
                int currentContactId = webUser.getProjectContact().getContactId();
                Query contactQuery = dataSession.createQuery(
                                "from ProjectContact where workspaceId = :workspaceId order by nameLast, nameFirst");
                contactQuery.setParameter("workspaceId", appReq.getActiveWorkspaceId());
                @SuppressWarnings("unchecked")
                List<ProjectContact> contacts = (List<ProjectContact>) contactQuery.list();
                for (ProjectContact contact : contacts) {
                        if (contact.getContactId() != currentContactId) {
                                out.println("          <option value=\"" + contact.getContactId() + "\">"
                                                + escapeHtml(contact.getName()) + "</option>");
                        }
                }
                out.println("        </select>");
                out.println("      </div>");

                out.println("      <div id=\"eaEditActionTimeSection\" class=\"ea-modal-field\">");
                out.println("        <label for=\"eaEditActionTime\">Time Estimate (mins)</label>");
                out.println("        <input type=\"number\" id=\"eaEditActionTime\" min=\"0\" step=\"1\" />");
                out.println("      </div>");

                out.println("      <div class=\"ea-modal-field\">");
                out.println("        <label for=\"eaEditActionLink\">Link URL</label>");
                out.println("        <input type=\"text\" id=\"eaEditActionLink\" />");
                out.println("      </div>");

                out.println("      <details id=\"eaEditAdvancedSection\" class=\"ea-advanced-section\">");
                out.println("        <summary class=\"ea-advanced-toggle\">More options</summary>");
                out.println("        <div class=\"ea-advanced-body\">");
                out.println("          <div class=\"ea-modal-field\">");
                out.println("            <label for=\"eaEditActionTarget\">Target Date</label>");
                out.println("            <input type=\"date\" id=\"eaEditActionTarget\" />");
                out.println("          </div>");
                out.println("          <div class=\"ea-modal-field\">");
                out.println("            <label for=\"eaEditActionDeadline\">Deadline</label>");
                out.println("            <input type=\"date\" id=\"eaEditActionDeadline\" />");
                out.println("          </div>");
                out.println("          <div class=\"ea-modal-field\">");
                out.println("            <label for=\"eaEditActionNote\">Note</label>");
                out.println("            <textarea id=\"eaEditActionNote\" rows=\"2\"></textarea>");
                out.println("          </div>");
                out.println("        </div>");
                out.println("      </details>");

                out.print(buildModalActionsHtml(config));
                out.println("    </div>");
                out.println("  </div>");
                out.println("</div>");

                if (isCompleteConfigured(config)) {
                        printCompleteScript(out);
                }
        }

        private boolean isCompleteConfigured(Config config) {
                return config.completeOnClick != null && config.completeSubmitOnClick != null;
        }

        /**
         * Renders the (DB-independent) complete section plus the modal's action
         * button row. Split out from {@link #render} so it can be exercised
         * directly in tests without a Hibernate session.
         */
        String buildModalActionsHtml(Config config) {
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw);
                boolean showComplete = isCompleteConfigured(config);
                if (showComplete) {
                        printCompleteSection(out, config);
                }

                out.println("      <div class=\"ea-modal-actions\">");
                out.println("        <button type=\"button\" class=\"ea-primary-btn\" onclick=\""
                                + n(config.saveOnClick)
                                + "\">Save Action</button>");
                out.println(
                                "        <button type=\"button\" class=\"ea-primary-btn\" onclick=\""
                                                + n(config.saveAndStartOnClick)
                                                + "\">Save and Start</button>");
                if (showComplete) {
                        out.println("        <button type=\"button\" class=\"ea-primary-btn\" id=\"eaCompleteToggleBtn\" onclick=\""
                                        + n(config.completeOnClick) + "\">Complete</button>");
                }
                out.println("        <button type=\"button\" class=\"ea-secondary-btn\" onclick=\""
                                + n(config.cancelOnClick)
                                + "\">Cancel</button>");
                out.println("        <button type=\"button\" class=\"ea-danger-btn\" onclick=\""
                                + n(config.deleteOnClick)
                                + "\">Delete</button>");
                out.println("      </div>");
                out.flush();
                return sw.toString();
        }

        private void printCompleteSection(PrintWriter out, Config config) {
                out.println("      <div id=\"eaCompleteSection\" class=\"ea-complete-section\" style=\"display:none\">");
                out.println("        <div class=\"ea-modal-field\">");
                out.println("          <label for=\"eaCompleteDescription\">What was done (optional)</label>");
                out.println("          <input type=\"text\" id=\"eaCompleteDescription\" />");
                out.println("        </div>");
                out.println("        <div class=\"ea-complete-row\">");
                out.println("          <div class=\"ea-modal-field\">");
                out.println("            <label for=\"eaCompleteDate\">Date</label>");
                out.println("            <input type=\"date\" id=\"eaCompleteDate\" />");
                out.println("          </div>");
                out.println("          <div class=\"ea-modal-field\">");
                out.println("            <label for=\"eaCompleteTime\">Start Time</label>");
                out.println("            <input type=\"time\" id=\"eaCompleteTime\" />");
                out.println("          </div>");
                out.println("          <div class=\"ea-modal-field\">");
                out.println("            <label for=\"eaCompleteDuration\">Minutes Spent</label>");
                out.println("            <input type=\"number\" id=\"eaCompleteDuration\" min=\"0\" step=\"1\" value=\"0\" />");
                out.println("          </div>");
                out.println("        </div>");
                out.println("        <div id=\"eaCompleteError\" class=\"ea-complete-error\" style=\"display:none\"></div>");
                out.println("        <div class=\"ea-complete-actions\">");
                out.println("          <button type=\"button\" class=\"ea-primary-btn\" onclick=\""
                                + n(config.completeSubmitOnClick) + "\">Complete</button>");
                out.println("        </div>");
                out.println("      </div>");
        }

        /**
         * Shared helpers for the completion row; assigned to window so inline onclick
         * can reach them.
         */
        private void printCompleteScript(PrintWriter out) {
                out.println("<script>");
                out.println("  window.eaResetCompleteSection = function(){");
                out.println("    var section = document.getElementById('eaCompleteSection');");
                out.println("    if (section) { section.style.display = 'none'; }");
                out.println("    var now = new Date();");
                out.println("    var pad = function(v){ return (v < 10 ? '0' : '') + v; };");
                out.println("    var dateEl = document.getElementById('eaCompleteDate');");
                out.println(
                                "    if (dateEl) { dateEl.value = now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()); }");
                out.println("    var timeEl = document.getElementById('eaCompleteTime');");
                out.println("    if (timeEl) { timeEl.value = pad(now.getHours()) + ':' + pad(now.getMinutes()); }");
                out.println("    var durationEl = document.getElementById('eaCompleteDuration');");
                out.println("    if (durationEl) { durationEl.value = '0'; }");
                out.println("    var descEl = document.getElementById('eaCompleteDescription');");
                out.println("    if (descEl) { descEl.value = ''; }");
                out.println("    window.eaClearCompleteError();");
                out.println("  };");
                out.println("  window.eaToggleCompleteSection = function(){");
                out.println("    var section = document.getElementById('eaCompleteSection');");
                out.println("    if (!section) { return; }");
                out.println("    if (section.style.display === 'none') {");
                out.println("      section.style.display = '';");
                out.println("      var descEl = document.getElementById('eaCompleteDescription');");
                out.println("      if (descEl) { descEl.focus(); }");
                out.println("    } else {");
                out.println("      section.style.display = 'none';");
                out.println("    }");
                out.println("  };");
                out.println("  window.eaClearCompleteError = function(){");
                out.println("    var errorEl = document.getElementById('eaCompleteError');");
                out.println("    if (errorEl) { errorEl.textContent = ''; errorEl.style.display = 'none'; }");
                out.println("  };");
                out.println("  window.eaShowCompleteError = function(message){");
                out.println("    var errorEl = document.getElementById('eaCompleteError');");
                out.println("    if (!errorEl) { alert(message); return; }");
                out.println("    var section = document.getElementById('eaCompleteSection');");
                out.println("    if (section) { section.style.display = ''; }");
                out.println("    errorEl.textContent = message || 'Unable to complete action.';");
                out.println("    errorEl.style.display = '';");
                out.println("  };");
                out.println("  window.eaReadCompleteValue = function(id){");
                out.println("    var el = document.getElementById(id);");
                out.println("    return el ? (el.value || '') : '';");
                out.println("  };");
                out.println("</script>");
        }

        public void printSharedStyles(PrintWriter out) {
                out.println(
                                "  .ea-modal-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid #d8e0d6; }");
                out.println("  .ea-modal-title { margin: 0; font-size: 18px; color: #243624; }");
                out.println(
                                "  .ea-modal-close { border: 1px solid #8ca08c; background: #f5faf4; color: #274127; width: 34px; height: 34px; border-radius: 7px; font-size: 24px; line-height: 28px; cursor: pointer; }");
                out.println("  .ea-modal-body { padding: 14px; }");
                out.println("  .ea-modal-field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }");
                out.println(
                                "  .ea-modal-field label { font-size: 12px; font-weight: 700; color: #314631; letter-spacing: .02em; text-transform: uppercase; }");
                out.println(
                                "  .ea-modal-field input, .ea-modal-field textarea, .ea-modal-field select { width: 100%; box-sizing: border-box; border: 1px solid #bfcdbb; border-radius: 6px; padding: 8px 9px; font-family: inherit; font-size: 14px; background: #fcfffb; }");
                out.println("  .ea-modal-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }");
                out.println(
                                "  .ea-primary-btn { border: 1px solid #7a9b7a; background: #d6ecd6; color: #1f3a1f; padding: 8px 12px; cursor: pointer; border-radius: 6px; font-weight: 700; }");
                out.println(
                                "  .ea-secondary-btn { border: 1px solid #9aa89a; background: #f2f6f2; color: #2b3e2b; padding: 8px 12px; cursor: pointer; border-radius: 6px; }");
                out.println(
                                "  .ea-danger-btn { border: 1px solid #d0a0a0; background: #f7ecec; color: #7b2323; padding: 8px 12px; cursor: pointer; border-radius: 6px; }");
                out.println(
                                "  .ea-chip { display: inline-flex; align-items: center; justify-content: center; padding: 4px 9px; border: 1px solid #b9c9b7; border-radius: 14px; background: #f7fbf6; color: #314631; font-size: 12px; cursor: pointer; }");
                out.println("  .ea-action-type-chips { display: flex; flex-wrap: wrap; gap: 6px; }");
                out.println("  .ea-chip-active { background: #5b735c; color: #fffdf8; border-color: #445a45; }");
                out.println("  .ea-contact-emphasized .ea-contact-label { color: #5b3a00; font-weight: bold; }");
                out.println("  .ea-contact-emphasized select { border-color: #8b6020; box-shadow: 0 0 0 2px #f5e0a0; }");
                out.println("  .ea-advanced-section { margin-bottom: 12px; border: 1px solid #d4c9b8; border-radius: 4px; }");
                out.println(
                                "  .ea-advanced-toggle { padding: 6px 10px; cursor: pointer; font-size: 13px; font-weight: bold; color: #5b735c; list-style: none; user-select: none; }");
                out.println("  .ea-advanced-toggle::-webkit-details-marker { display: none; }");
                out.println("  .ea-advanced-toggle::before { content: '\\25B6 '; font-size: 10px; }");
                out.println("  details.ea-advanced-section[open] .ea-advanced-toggle::before { content: '\\25BC '; }");
                out.println("  .ea-advanced-body { padding: 6px 10px 2px; }");
                out.println(
                                "  .ea-complete-section { border: 1px solid #b8c9b6; border-radius: 6px; background: #f4faf3; padding: 10px 10px 4px; margin-bottom: 10px; }");
                out.println("  .ea-complete-row { display: flex; gap: 8px; flex-wrap: wrap; }");
                out.println("  .ea-complete-row .ea-modal-field { flex: 1 1 120px; }");
                out.println(
                                "  .ea-complete-error { color: #7b2323; background: #f9ecec; border: 1px solid #d0a0a0; border-radius: 6px; padding: 6px 8px; font-size: 13px; margin-bottom: 8px; }");
                out.println("  .ea-complete-actions { display: flex; gap: 8px; margin-bottom: 8px; }");
        }

        private String n(String value) {
                return value == null ? "" : value;
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
