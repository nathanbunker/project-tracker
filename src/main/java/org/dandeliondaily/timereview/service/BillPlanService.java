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

    public void approvePlan(BillPlan billPlan) {
        BillPlan prior = billPlanDao.findLatestApprovedPlanBefore(billPlan.getWorkspaceId(), billPlan.getWebUserId(),
                billPlan.getBillPlanCode(), billPlan.getEffectiveDate(), Integer.valueOf(billPlan.getBillPlanId()));
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
        List<BillPlan> candidates = new ArrayList<BillPlan>();
        for (BillPlan plan : plans) {
            if (plan == null || plan.getEffectiveDate() == null || plan.getPlanStatus() == null) {
                continue;
            }
            if (!BillPlanStatus.APPROVED.getCode().equalsIgnoreCase(plan.getPlanStatus())) {
                continue;
            }
            if (plan.getEffectiveDate().after(requestedDate)) {
                continue;
            }
            candidates.add(plan);
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