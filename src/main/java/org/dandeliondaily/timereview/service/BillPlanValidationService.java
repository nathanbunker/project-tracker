package org.dandeliondaily.timereview.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openimmunizationsoftware.pt.model.BillBudget;
import org.openimmunizationsoftware.pt.model.BillCode;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanStatus;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;
import org.openimmunizationsoftware.pt.model.BillPlanTargetMode;

public class BillPlanValidationService {

    public static final int MAX_BASIS_POINTS = 10000;

    public BillPlanValidationResult validatePlan(BillPlan billPlan) {
        BillPlanValidationResult result = new BillPlanValidationResult();
        if (billPlan == null) {
            result.addError("Bill plan is required.");
            return result;
        }
        if (billPlan.getWorkspaceId() <= 0) {
            result.addError("Workspace is required.");
        }
        if (billPlan.getWebUserId() <= 0) {
            result.addError("User is required.");
        }
        if (isBlank(billPlan.getBillPlanCode())) {
            result.addError("Plan code is required.");
        }
        if (isBlank(billPlan.getPlanLabel())) {
            result.addError("Plan label is required.");
        }
        if (billPlan.getFiscalStartDate() == null || billPlan.getFiscalEndDate() == null) {
            result.addError("Fiscal start and end dates are required.");
        } else if (billPlan.getFiscalEndDate().before(billPlan.getFiscalStartDate())) {
            result.addError("Fiscal end date cannot be before fiscal start date.");
        }
        if (billPlan.getEffectiveDate() == null) {
            result.addError("Effective date is required.");
        } else if (billPlan.getFiscalStartDate() != null && billPlan.getFiscalEndDate() != null
                && (billPlan.getEffectiveDate().before(billPlan.getFiscalStartDate())
                        || billPlan.getEffectiveDate().after(billPlan.getFiscalEndDate()))) {
            result.addError("Effective date must be within the fiscal period.");
        }
        if (billPlan.getVersionNum() <= 0) {
            result.addError("Version number must be positive.");
        }
        return result;
    }

    public BillPlanValidationResult validateTargets(BillPlan billPlan, List<BillPlanTarget> targets,
            List<BillCode> validBillCodes, List<BillBudget> validBudgets, boolean approving) {
        BillPlanValidationResult result = validatePlan(billPlan);
        Set<String> billCodes = new HashSet<String>();
        int totalBasisPoints = 0;
        if (targets == null || targets.isEmpty()) {
            if (approving) {
                result.addError("Approved plans require at least one target.");
            }
            return result;
        }
        for (BillPlanTarget target : targets) {
            if (target == null) {
                result.addError("Plan target is required.");
                continue;
            }
            if (isBlank(target.getBillCode())) {
                result.addError("Plan target billing code is required.");
                continue;
            }
            String normalizedCode = target.getBillCode().trim();
            if (!billCodes.add(normalizedCode)) {
                result.addError("Duplicate billing code in plan targets: " + normalizedCode);
            }
            if (!containsBillCode(validBillCodes, normalizedCode, billPlan == null ? 0 : billPlan.getWorkspaceId())) {
                result.addError("Billing code does not exist in the plan workspace: " + normalizedCode);
            }
            validateBasisPoints(target.getAnnualTargetBps(), "Annual target", result);
            validateBasisPoints(target.getSteeringTargetBps(), "Steering target", result);
            BillPlanTargetMode mode = BillPlanTargetMode.fromCode(target.getTargetMode());
            if (mode == null) {
                result.addError("Target mode is required for billing code: " + normalizedCode);
            } else {
                boolean hasPercent = target.getAnnualTargetBps() != null || target.getSteeringTargetBps() != null;
                boolean hasBudget = target.getBillBudgetId() != null;
                if (mode == BillPlanTargetMode.PERCENT && !hasPercent) {
                    result.addError(
                            "PERCENT target requires at least one percentage for billing code: " + normalizedCode);
                }
                if (mode == BillPlanTargetMode.FIXED_HOURS && !hasBudget) {
                    result.addError("FIXED_HOURS target requires a budget for billing code: " + normalizedCode);
                }
                if (mode == BillPlanTargetMode.BOTH && (!hasBudget || !hasPercent)) {
                    result.addError("BOTH target requires a budget and at least one percentage for billing code: "
                            + normalizedCode);
                }
            }
            if (target.getBillBudgetId() != null) {
                BillBudget budget = findBudget(validBudgets, target.getBillBudgetId().intValue());
                if (budget == null) {
                    result.addError("Budget not found for billing code: " + normalizedCode);
                } else if (budget.getBillCode() == null || budget.getBillCode().getBillCode() == null
                        || !normalizedCode.equals(budget.getBillCode().getBillCode())) {
                    result.addError("Budget billing code must match target billing code: " + normalizedCode);
                }
            }
            totalBasisPoints += target.getAnnualTargetBps() == null ? 0 : target.getAnnualTargetBps().intValue();
        }
        if (approving) {
            if (totalBasisPoints != MAX_BASIS_POINTS) {
                result.addError("Approved plans require annual target percentages totaling exactly 100%.");
            }
        } else if (totalBasisPoints != MAX_BASIS_POINTS) {
            result.addWarning("Draft plan annual target percentages do not total 100%.");
        }
        if (approving && billPlan != null
                && !BillPlanStatus.APPROVED.getCode().equalsIgnoreCase(billPlan.getPlanStatus())) {
            result.addWarning("Approving plan with non-APPROVED status code.");
        }
        return result;
    }

    private void validateBasisPoints(Integer basisPoints, String label, BillPlanValidationResult result) {
        if (basisPoints == null) {
            return;
        }
        if (basisPoints.intValue() < 0 || basisPoints.intValue() > MAX_BASIS_POINTS) {
            result.addError(label + " must be between 0 and 10000 basis points.");
        }
    }

    private boolean containsBillCode(List<BillCode> validBillCodes, String billCode, int workspaceId) {
        if (validBillCodes == null) {
            return false;
        }
        for (BillCode candidate : validBillCodes) {
            if (candidate != null && candidate.getWorkspaceId() != null
                    && candidate.getWorkspaceId().intValue() == workspaceId
                    && billCode.equals(candidate.getBillCode())) {
                return true;
            }
        }
        return false;
    }

    private BillBudget findBudget(List<BillBudget> validBudgets, int billBudgetId) {
        if (validBudgets == null) {
            return null;
        }
        for (BillBudget budget : validBudgets) {
            if (budget != null && budget.getBillBudgetId() == billBudgetId) {
                return budget;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}