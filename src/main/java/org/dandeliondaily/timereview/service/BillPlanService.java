package org.dandeliondaily.timereview.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.pt.doa.BillPlanDao;
import org.openimmunizationsoftware.pt.doa.BillPlanTargetDao;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanStatus;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;

public class BillPlanService {

    private final BillPlanDao billPlanDao;
    private final BillPlanTargetDao billPlanTargetDao;

    public BillPlanService() {
        this(new BillPlanDao(), new BillPlanTargetDao());
    }

    public BillPlanService(BillPlanDao billPlanDao, BillPlanTargetDao billPlanTargetDao) {
        this.billPlanDao = billPlanDao;
        this.billPlanTargetDao = billPlanTargetDao;
    }

    public BillPlan createDraftPlan(BillPlan billPlan) {
        billPlan.setPlanStatus(BillPlanStatus.DRAFT.getCode());
        return billPlanDao.save(billPlan);
    }

    public void updateDraftPlan(BillPlan billPlan) {
        billPlanDao.update(billPlan);
    }

    /**
     * Copies a plan and its targets into a new draft numbered after the highest
     * existing version of the plan code. The source keeps its status until the
     * draft is approved, so the current plan stays in effect meanwhile.
     */
    public BillPlan createRevision(BillPlan source, Date today) {
        BillPlan revision = new BillPlan();
        revision.setWorkspaceId(source.getWorkspaceId());
        revision.setWebUserId(source.getWebUserId());
        revision.setBillPlanCode(source.getBillPlanCode());
        revision.setPlanLabel(source.getPlanLabel());
        revision.setFiscalStartDate(source.getFiscalStartDate());
        revision.setFiscalEndDate(source.getFiscalEndDate());
        revision.setVersionNum(billPlanDao.findMaxVersionNum(source.getWorkspaceId(), source.getWebUserId(),
                source.getBillPlanCode()) + 1);
        boolean todayInFiscalPeriod = today != null && source.getFiscalStartDate() != null
                && source.getFiscalEndDate() != null && !today.before(source.getFiscalStartDate())
                && !today.after(source.getFiscalEndDate());
        revision.setEffectiveDate(todayInFiscalPeriod ? today : source.getEffectiveDate());
        revision.setPercentBasis(source.getPercentBasis());
        revision.setPlanStatus(BillPlanStatus.DRAFT.getCode());
        revision.setSupersedesBillPlanId(Integer.valueOf(source.getBillPlanId()));
        revision.setChangeNote(null);
        billPlanDao.save(revision);

        for (BillPlanTarget sourceTarget : billPlanTargetDao.listTargetsForPlan(source.getBillPlanId())) {
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
            billPlanTargetDao.save(target);
        }
        return revision;
    }

    public void approvePlan(BillPlan billPlan) {
        BillPlan prior = null;
        if (billPlan.getSupersedesBillPlanId() != null
                && billPlan.getSupersedesBillPlanId().intValue() != billPlan.getBillPlanId()) {
            BillPlan source = billPlanDao.getBillPlan(billPlan.getSupersedesBillPlanId().intValue());
            if (source != null && BillPlanStatus.APPROVED.getCode().equalsIgnoreCase(source.getPlanStatus())) {
                prior = source;
            }
        }
        if (prior == null) {
            prior = billPlanDao.findLatestApprovedPlanBefore(billPlan.getWorkspaceId(), billPlan.getWebUserId(),
                    billPlan.getBillPlanCode(), billPlan.getEffectiveDate(),
                    Integer.valueOf(billPlan.getBillPlanId()));
        }
        if (prior != null) {
            billPlan.setSupersedesBillPlanId(Integer.valueOf(prior.getBillPlanId()));
            prior.setPlanStatus(BillPlanStatus.SUPERSEDED.getCode());
            billPlanDao.update(prior);
        }
        billPlan.setPlanStatus(BillPlanStatus.APPROVED.getCode());
        billPlanDao.update(billPlan);
    }

    public BillPlan getPlanById(int billPlanId) {
        return billPlanDao.getBillPlan(billPlanId);
    }

    public List<BillPlan> listPlansForUserAndFiscalPeriod(int workspaceId, int webUserId, Date fiscalStartDate,
            Date fiscalEndDate) {
        return billPlanDao.listPlansForUserAndFiscalPeriod(workspaceId, webUserId, fiscalStartDate, fiscalEndDate);
    }

    public BillPlan getActiveApprovedPlan(int workspaceId, int webUserId, Date requestedDate) {
        return billPlanDao.findActiveApprovedPlan(workspaceId, webUserId, requestedDate);
    }

    public BillPlan selectActiveApprovedPlan(List<BillPlan> plans, Date requestedDate) {
        if (plans == null || requestedDate == null) {
            return null;
        }
        // Mirrors BillPlanDao.findActiveApprovedPlan: a superseded plan still applies
        // until its successor's effective date.
        List<BillPlan> inForce = new ArrayList<BillPlan>();
        for (BillPlan plan : plans) {
            if (plan == null || plan.getEffectiveDate() == null || plan.getPlanStatus() == null) {
                continue;
            }
            if (!BillPlanStatus.APPROVED.getCode().equalsIgnoreCase(plan.getPlanStatus())
                    && !BillPlanStatus.SUPERSEDED.getCode().equalsIgnoreCase(plan.getPlanStatus())) {
                continue;
            }
            if (plan.getEffectiveDate().after(requestedDate)) {
                continue;
            }
            inForce.add(plan);
        }
        List<BillPlan> candidates = new ArrayList<BillPlan>();
        for (BillPlan plan : inForce) {
            boolean replaced = false;
            for (BillPlan other : inForce) {
                if (other.getSupersedesBillPlanId() != null
                        && other.getSupersedesBillPlanId().intValue() == plan.getBillPlanId()
                        && other != plan) {
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                candidates.add(plan);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.sort(candidates, new Comparator<BillPlan>() {
            @Override
            public int compare(BillPlan left, BillPlan right) {
                int effectiveDateCompare = right.getEffectiveDate().compareTo(left.getEffectiveDate());
                if (effectiveDateCompare != 0) {
                    return effectiveDateCompare;
                }
                return right.getVersionNum() - left.getVersionNum();
            }
        });
        return candidates.get(0);
    }

    public List<BillPlanTarget> getTargetsForPlan(int billPlanId) {
        return billPlanTargetDao.listTargetsForPlan(billPlanId);
    }
}