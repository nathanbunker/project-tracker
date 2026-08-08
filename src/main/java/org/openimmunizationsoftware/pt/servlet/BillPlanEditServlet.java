package org.openimmunizationsoftware.pt.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dandeliondaily.timereview.service.BillPlanService;
import org.dandeliondaily.timereview.service.BillPlanValidationResult;
import org.dandeliondaily.timereview.service.BillPlanValidationService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openimmunizationsoftware.pt.AppReq;
import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanPercentBasis;
import org.openimmunizationsoftware.pt.model.BillPlanStatus;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;
import org.openimmunizationsoftware.pt.model.BillPlanTargetMode;
import org.openimmunizationsoftware.pt.model.BillPlanVariancePolicy;

public class BillPlanEditServlet extends ClientServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AppReq appReq = new AppReq(request, response);
        try {
            if (appReq.isLoggedOut() || !appReq.isAdmin()) {
                forwardToHome(request, response);
                return;
            }
            Integer activeWorkspaceId = appReq.getActiveWorkspaceId();
            if (activeWorkspaceId == null) {
                forwardToHome(request, response);
                return;
            }

            Session dataSession = appReq.getDataSession();
            BillPlan billPlan = resolveBillPlan(request, dataSession, activeWorkspaceId.intValue());
            if (billPlan == null) {
                billPlan = new BillPlan();
                billPlan.setWorkspaceId(activeWorkspaceId.intValue());
                billPlan.setVersionNum(1);
                billPlan.setPercentBasis(BillPlanPercentBasis.ALL_WORKED_TIME.getCode());
                billPlan.setPlanStatus(BillPlanStatus.DRAFT.getCode());
            }

            Query codeQuery = dataSession.createQuery(
                    "from BillCode where workspaceId = :workspaceId and visible = 'Y' order by billLabel");
            codeQuery.setInteger("workspaceId", activeWorkspaceId.intValue());
            @SuppressWarnings("unchecked")
            List<BillCode> billCodeList = codeQuery.list();

            Query budgetQuery = dataSession.createQuery(
                    "from BillBudget where workspaceId = :workspaceId order by billBudgetCode");
            budgetQuery.setInteger("workspaceId", activeWorkspaceId.intValue());
            @SuppressWarnings("unchecked")
            List<BillBudget> billBudgetList = budgetQuery.list();

            List<BillPlanTarget> targets = getTargets(dataSession, billPlan);
            if (targets.isEmpty()) {
                targets.add(new BillPlanTarget());
            }

            String action = appReq.getAction();
            if (action != null && action.length() > 0) {
                if (isReadOnlyStatus(billPlan)) {
                    appReq.setMessageProblem(
                            "Approved, superseded, and inactive plans are read-only. Create a revision to make changes.");
                } else {
                    applyPlanFields(request, appReq, billPlan);
                    boolean editingTargets = "Add Target".equals(action) || action.startsWith("Remove Target ");
                    targets = readTargetsFromRequest(request, editingTargets);
                    if ("Add Target".equals(action)) {
                        targets.add(new BillPlanTarget());
                    } else if (action.startsWith("Remove Target ")) {
                        Integer targetIndex = parseInteger(action.substring("Remove Target ".length()));
                        if (targetIndex != null && targetIndex.intValue() >= 0
                                && targetIndex.intValue() < targets.size()) {
                            targets.remove(targetIndex.intValue());
                        }
                        if (targets.isEmpty()) {
                            targets.add(new BillPlanTarget());
                        }
                    } else {
                        String validationMessage = validateAndSave(action, dataSession, billPlan, targets, billCodeList,
                                billBudgetList);
                        if (validationMessage == null) {
                            response.sendRedirect("BillPlanEditServlet?billPlanId=" + billPlan.getBillPlanId());
                            return;
                        }
                        appReq.setMessageProblem(validationMessage);
                    }
                }
            }

            appReq.setTitle("Allocation Plan");
            printHtmlHead(appReq);
            PrintWriter out = appReq.getOut();
            printDandelionLocation(out, "Billing / Allocation Plans");
            printBillingAdminNav(out, "Allocation Plans");

            out.println("<form method=\"POST\" action=\"BillPlanEditServlet\">");
            out.println("<input type=\"hidden\" name=\"billPlanId\" value=\"" + billPlan.getBillPlanId() + "\">");
            out.println("<input type=\"hidden\" name=\"targetCount\" value=\"" + targets.size() + "\">");
            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"2\">Edit Allocation Plan</th></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Plan Code</th><td class=\"boxed\"><input type=\"text\" name=\"billPlanCode\" value=\""
                            + escapeHtmlAttribute(n(billPlan.getBillPlanCode())) + "\" size=\"30\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Plan Label</th><td class=\"boxed\"><input type=\"text\" name=\"planLabel\" value=\""
                            + escapeHtmlAttribute(n(billPlan.getPlanLabel())) + "\" size=\"40\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Fiscal Start Date</th><td class=\"boxed\"><input type=\"text\" name=\"fiscalStartDate\" value=\""
                            + formatDate(appReq, billPlan.getFiscalStartDate()) + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Fiscal End Date</th><td class=\"boxed\"><input type=\"text\" name=\"fiscalEndDate\" value=\""
                            + formatDate(appReq, billPlan.getFiscalEndDate()) + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Version</th><td class=\"boxed\"><input type=\"text\" name=\"versionNum\" value=\""
                            + billPlan.getVersionNum() + "\" size=\"4\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Effective Date</th><td class=\"boxed\"><input type=\"text\" name=\"effectiveDate\" value=\""
                            + formatDate(appReq, billPlan.getEffectiveDate()) + "\" size=\"10\"></td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Percent Basis</th><td class=\"boxed\"><select name=\"percentBasis\">");
            for (BillPlanPercentBasis basis : BillPlanPercentBasis.values()) {
                out.println("<option value=\"" + basis.getCode() + "\""
                        + (basis.getCode().equalsIgnoreCase(n(billPlan.getPercentBasis())) ? " selected" : "") + ">"
                        + basis.getCode() + "</option>");
            }
            out.println("</select></td></tr>");
            out.println("  <tr class=\"boxed\"><th class=\"boxed\">Status</th><td class=\"boxed\">"
                    + n(billPlan.getPlanStatus()) + "</td></tr>");
            out.println(
                    "  <tr class=\"boxed\"><th class=\"boxed\">Change Note</th><td class=\"boxed\"><textarea name=\"changeNote\" rows=\"3\" cols=\"60\">"
                            + n(billPlan.getChangeNote()) + "</textarea></td></tr>");
            out.println("</table><br/>");

            boolean targetsReadOnly = isReadOnlyStatus(billPlan);
            out.println("<table class=\"boxed\">");
            out.println("  <tr><th class=\"title\" colspan=\"" + (targetsReadOnly ? 8 : 9) + "\">Targets</th></tr>");
            out.println("  <tr class=\"boxed\">");
            out.println("    <th class=\"boxed\">Bill Code</th>");
            out.println("    <th class=\"boxed\">Mode</th>");
            out.println("    <th class=\"boxed\">Annual BPS</th>");
            out.println("    <th class=\"boxed\">Steering BPS</th>");
            out.println("    <th class=\"boxed\">Budget</th>");
            out.println("    <th class=\"boxed\">Variance Policy</th>");
            out.println("    <th class=\"boxed\">Display Order</th>");
            out.println("    <th class=\"boxed\">Note</th>");
            if (!targetsReadOnly) {
                out.println("    <th class=\"boxed\">Action</th>");
            }
            out.println("  </tr>");
            for (int i = 0; i < targets.size(); i++) {
                BillPlanTarget target = targets.get(i);
                out.println("  <tr class=\"boxed\">");
                out.println("    <td class=\"boxed\"><select name=\"targetBillCode_" + i
                        + "\"><option value=\"\"></option>");
                for (BillCode billCode : billCodeList) {
                    out.println("<option value=\"" + n(billCode.getBillCode()) + "\""
                            + (n(billCode.getBillCode()).equals(n(target.getBillCode())) ? " selected" : "") + ">"
                            + n(billCode.getBillCode()) + "</option>");
                }
                out.println("</select></td>");
                out.println("    <td class=\"boxed\"><select name=\"targetMode_" + i + "\">");
                for (BillPlanTargetMode mode : BillPlanTargetMode.values()) {
                    out.println("<option value=\"" + mode.getCode() + "\""
                            + (mode.getCode().equalsIgnoreCase(n(target.getTargetMode())) ? " selected" : "") + ">"
                            + mode.getCode() + "</option>");
                }
                out.println("</select></td>");
                out.println("    <td class=\"boxed\"><input type=\"text\" name=\"annualTargetBps_" + i + "\" value=\""
                        + n(target.getAnnualTargetBps()) + "\" size=\"6\"></td>");
                out.println("    <td class=\"boxed\"><input type=\"text\" name=\"steeringTargetBps_" + i + "\" value=\""
                        + n(target.getSteeringTargetBps()) + "\" size=\"6\"></td>");
                out.println(
                        "    <td class=\"boxed\"><select name=\"billBudgetId_" + i + "\"><option value=\"\"></option>");
                for (BillBudget billBudget : billBudgetList) {
                    String budgetId = String.valueOf(billBudget.getBillBudgetId());
                    out.println("<option value=\"" + budgetId + "\""
                            + (budgetId.equals(n(target.getBillBudgetId())) ? " selected" : "") + ">"
                            + n(billBudget.getBillBudgetCode()) + "</option>");
                }
                out.println("</select></td>");
                out.println("    <td class=\"boxed\"><select name=\"variancePolicy_" + i + "\">");
                for (BillPlanVariancePolicy policy : BillPlanVariancePolicy.values()) {
                    out.println("<option value=\"" + policy.getCode() + "\""
                            + (policy.getCode().equalsIgnoreCase(n(target.getVariancePolicy())) ? " selected" : "")
                            + ">"
                            + policy.getCode() + "</option>");
                }
                out.println("</select></td>");
                out.println("    <td class=\"boxed\"><input type=\"text\" name=\"displayOrder_" + i + "\" value=\""
                        + target.getDisplayOrder() + "\" size=\"4\"></td>");
                out.println("    <td class=\"boxed\"><input type=\"text\" name=\"targetNote_" + i + "\" value=\""
                        + escapeHtmlAttribute(n(target.getTargetNote())) + "\" size=\"20\"></td>");
                if (!targetsReadOnly) {
                    out.println("    <td class=\"boxed\"><button type=\"submit\" name=\"action\" value=\"Remove Target "
                            + i + "\">Remove</button></td>");
                }
                out.println("  </tr>");
            }
            out.println("</table><br/>");

            if (!isReadOnlyStatus(billPlan)) {
                out.println("<input type=\"submit\" name=\"action\" value=\"Add Target\">");
                out.println("<input type=\"submit\" name=\"action\" value=\"Save Draft\">");
                out.println("<input type=\"submit\" name=\"action\" value=\"Approve\">");
            }
            if (billPlan.getBillPlanId() > 0) {
                out.println("<input type=\"submit\" name=\"action\" value=\"Create Revision\">");
            }
            out.println("</form>");

            printHtmlFoot(appReq);
        } finally {
            appReq.close();
        }
    }

    private BillPlan resolveBillPlan(HttpServletRequest request, Session dataSession, int workspaceId) {
        String id = request.getParameter("billPlanId");
        if (id == null || id.trim().equals("")) {
            return null;
        }
        BillPlan billPlan = (BillPlan) dataSession.get(BillPlan.class, Integer.parseInt(id));
        if (billPlan == null || billPlan.getWorkspaceId() != workspaceId) {
            return null;
        }
        return billPlan;
    }

    private List<BillPlanTarget> getTargets(Session dataSession, BillPlan billPlan) {
        List<BillPlanTarget> targets = new ArrayList<BillPlanTarget>();
        if (billPlan.getBillPlanId() > 0) {
            Query query = dataSession.createQuery(
                    "from BillPlanTarget where billPlan.billPlanId = :billPlanId order by displayOrder, billCode");
            query.setInteger("billPlanId", billPlan.getBillPlanId());
            @SuppressWarnings("unchecked")
            List<BillPlanTarget> dbTargets = query.list();
            targets.addAll(dbTargets);
        }
        return targets;
    }

    private void applyPlanFields(HttpServletRequest request, AppReq appReq, BillPlan billPlan) {
        billPlan.setBillPlanCode(trim(request.getParameter("billPlanCode"), 30));
        billPlan.setPlanLabel(trim(request.getParameter("planLabel"), 150));
        billPlan.setWebUserId(appReq.getWebUser().getWebUserId());
        billPlan.setFiscalStartDate(appReq.getWebUser().parseDate(request.getParameter("fiscalStartDate")));
        billPlan.setFiscalEndDate(appReq.getWebUser().parseDate(request.getParameter("fiscalEndDate")));
        billPlan.setVersionNum(parseInt(request.getParameter("versionNum")));
        billPlan.setEffectiveDate(appReq.getWebUser().parseDate(request.getParameter("effectiveDate")));
        billPlan.setPercentBasis(trim(request.getParameter("percentBasis"), 30));
        billPlan.setChangeNote(trim(request.getParameter("changeNote"), 1000));
    }

    private List<BillPlanTarget> readTargetsFromRequest(HttpServletRequest request, boolean includeBlankTargets) {
        int targetCount = parseInt(request.getParameter("targetCount"));
        List<BillPlanTarget> targets = new ArrayList<BillPlanTarget>();
        for (int i = 0; i < targetCount; i++) {
            BillPlanTarget target = new BillPlanTarget();
            target.setBillCode(trim(request.getParameter("targetBillCode_" + i), 15));
            target.setTargetMode(trim(request.getParameter("targetMode_" + i), 15));
            target.setAnnualTargetBps(parseInteger(request.getParameter("annualTargetBps_" + i)));
            target.setSteeringTargetBps(parseInteger(request.getParameter("steeringTargetBps_" + i)));
            target.setBillBudgetId(parseInteger(request.getParameter("billBudgetId_" + i)));
            target.setVariancePolicy(trim(request.getParameter("variancePolicy_" + i), 20));
            target.setDisplayOrder(parseInt(request.getParameter("displayOrder_" + i)));
            target.setTargetNote(trim(request.getParameter("targetNote_" + i), 500));
            if (includeBlankTargets || target.getBillCode().length() > 0) {
                targets.add(target);
            }
        }
        return targets;
    }

    private String validateAndSave(String action, Session dataSession, BillPlan billPlan,
            List<BillPlanTarget> targets, List<BillCode> billCodeList, List<BillBudget> billBudgetList) {
        boolean approving = "Approve".equals(action);
        BillPlanValidationService validationService = new BillPlanValidationService();
        BillPlanValidationResult result = validationService.validateTargets(billPlan, targets, billCodeList,
                billBudgetList,
                approving);
        if (!result.getErrors().isEmpty()) {
            return joinMessages(result.getErrors());
        }

        Transaction trans = dataSession.beginTransaction();
        try {
            if (billPlan.getBillPlanId() == 0) {
                dataSession.save(billPlan);
            } else {
                dataSession.update(billPlan);
            }

            Query deleteQuery = dataSession
                    .createQuery("delete from BillPlanTarget where billPlan.billPlanId = :billPlanId");
            deleteQuery.setInteger("billPlanId", billPlan.getBillPlanId());
            deleteQuery.executeUpdate();

            for (BillPlanTarget target : targets) {
                target.setBillPlan(billPlan);
                dataSession.save(target);
            }

            if (approving) {
                billPlan.setPlanStatus(BillPlanStatus.APPROVED.getCode());
                BillPlanService service = new BillPlanService();
                service.approvePlan(billPlan);
            } else if ("Save Draft".equals(action)) {
                billPlan.setPlanStatus(BillPlanStatus.DRAFT.getCode());
                dataSession.update(billPlan);
            } else if ("Create Revision".equals(action)) {
                createRevision(dataSession, billPlan, targets);
            }

            trans.commit();
        } catch (Exception e) {
            trans.rollback();
            return "Unable to save allocation plan: " + e.getMessage();
        }
        return null;
    }

    private void createRevision(Session dataSession, BillPlan sourcePlan, List<BillPlanTarget> sourceTargets) {
        BillPlan revision = new BillPlan();
        revision.setWorkspaceId(sourcePlan.getWorkspaceId());
        revision.setWebUserId(sourcePlan.getWebUserId());
        revision.setBillPlanCode(sourcePlan.getBillPlanCode());
        revision.setPlanLabel(sourcePlan.getPlanLabel());
        revision.setFiscalStartDate(sourcePlan.getFiscalStartDate());
        revision.setFiscalEndDate(sourcePlan.getFiscalEndDate());
        revision.setVersionNum(sourcePlan.getVersionNum() + 1);
        revision.setEffectiveDate(sourcePlan.getEffectiveDate());
        revision.setPercentBasis(sourcePlan.getPercentBasis());
        revision.setPlanStatus(BillPlanStatus.DRAFT.getCode());
        revision.setSupersedesBillPlanId(Integer.valueOf(sourcePlan.getBillPlanId()));
        revision.setChangeNote(sourcePlan.getChangeNote());
        dataSession.save(revision);

        for (BillPlanTarget sourceTarget : sourceTargets) {
            BillPlanTarget target = new BillPlanTarget();
            target.setBillPlan(revision);
            target.setBillCode(sourceTarget.getBillCode());
            target.setTargetMode(sourceTarget.getTargetMode());
            target.setAnnualTargetBps(sourceTarget.getAnnualTargetBps());
            target.setSteeringTargetBps(sourceTarget.getSteeringTargetBps());
            target.setBillBudgetId(sourceTarget.getBillBudgetId());
            target.setVariancePolicy(sourceTarget.getVariancePolicy());
            target.setDisplayOrder(sourceTarget.getDisplayOrder());
            target.setTargetNote(sourceTarget.getTargetNote());
            dataSession.save(target);
        }

        if (BillPlanStatus.APPROVED.getCode().equalsIgnoreCase(n(sourcePlan.getPlanStatus()))) {
            sourcePlan.setPlanStatus(BillPlanStatus.SUPERSEDED.getCode());
            dataSession.update(sourcePlan);
        }
    }

    private boolean isReadOnlyStatus(BillPlan billPlan) {
        if (billPlan == null) {
            return false;
        }
        String status = n(billPlan.getPlanStatus()).toUpperCase();
        return BillPlanStatus.APPROVED.getCode().equals(status)
                || BillPlanStatus.SUPERSEDED.getCode().equals(status)
                || BillPlanStatus.INACTIVE.getCode().equals(status);
    }

    private String formatDate(AppReq appReq, java.util.Date value) {
        return value == null ? "" : appReq.getWebUser().getDateFormat().format(value);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(n(value).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private Integer parseInteger(String value) {
        String s = n(value).trim();
        if (s.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(s));
        } catch (Exception e) {
            return null;
        }
    }

    private String joinMessages(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(messages.get(i));
        }
        return sb.toString();
    }
}
