package org.dandeliondaily.shared.render;

import java.io.PrintWriter;

import org.dandeliondaily.dashboard.service.ActionRecoveryService;
import org.dandeliondaily.dashboard.service.ActionRecoveryService.RecentAction;
import org.openimmunizationsoftware.pt.AppReq;

public class ActionRecoveryRenderer {

    private final ActionRecoveryService actionRecoveryService = new ActionRecoveryService();

    public void render(AppReq appReq) {
        RecentAction recentAction = actionRecoveryService.getRecentAction(appReq);
        if (recentAction == null) {
            return;
        }

        long remainingMillis = Math.max(1L, recentAction.getExpiresAtMillis() - System.currentTimeMillis());
        PrintWriter out = appReq.getOut();
        out.println("<style>");
        out.println(
                "  .ar-chip { position: fixed; right: 18px; bottom: 18px; z-index: 1400; width: min(420px, calc(100vw - 36px)); min-height: 58px; display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 14px; padding: 11px 14px; border: 1px solid #506453; border-radius: 8px; background: #1f2d22; color: #f7fbf7; box-shadow: 0 10px 28px rgba(20, 34, 23, 0.28); cursor: pointer; text-align: left; font: inherit; }");
        out.println(
                "  .ar-chip:hover, .ar-chip:focus-visible { background: #2b3c2e; outline: 3px solid rgba(92, 145, 101, 0.35); outline-offset: 2px; }");
        out.println("  .ar-copy { min-width: 0; display: block; }");
        out.println(
                "  .ar-status { display: block; margin-bottom: 3px; color: #c6d8c8; font-size: 12px; font-weight: 700; text-transform: uppercase; }");
        out.println(
                "  .ar-description { display: block; overflow: hidden; color: #ffffff; font-size: 14px; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }");
        out.println("  .ar-restore { color: #d7f2dc; font-size: 13px; font-weight: 700; white-space: nowrap; }");
        out.println("</style>");
        out.println("<button id=\"arRecentAction\" type=\"button\" class=\"ar-chip\" onclick=\"arRestoreRecentAction("
                + recentAction.getActionNextId() + ")\" aria-label=\"Restore recent action\">");
        out.println("  <span class=\"ar-copy\">");
        out.println("    <span class=\"ar-status\">" + escapeHtml(recentAction.getStatusLabel()) + "</span>");
        out.println("    <span class=\"ar-description\">" + escapeHtml(recentAction.getDescription()) + "</span>");
        out.println("  </span>");
        out.println("  <span class=\"ar-restore\">Restore</span>");
        out.println("</button>");
        out.println("<script>");
        out.println("  function arRestoreRecentAction(actionNextId) {");
        out.println("    var chip = document.getElementById('arRecentAction');");
        out.println("    if (chip) { chip.disabled = true; }");
        out.println("    var body = 'action=restoreRecentAction&actionNextId=' + encodeURIComponent(actionNextId);");
        out.println(
                "    fetch('DandelionDashboardServlet', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }, body: body })");
        out.println("      .then(function(response) { return response.json(); })");
        out.println("      .then(function(data) {");
        out.println("        if (data && data.success) { window.location.reload(); return; }");
        out.println("        if (chip) { chip.disabled = false; }");
        out.println("        alert((data && data.message) ? data.message : 'Unable to restore action.');");
        out.println("      })");
        out.println(
                "      .catch(function() { if (chip) { chip.disabled = false; } alert('Unable to restore action.'); });");
        out.println("  }");
        out.println("  window.setTimeout(function() {");
        out.println("    var chip = document.getElementById('arRecentAction');");
        out.println("    if (chip) { chip.remove(); }");
        out.println("  }, " + remainingMillis + ");");
        out.println("</script>");
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