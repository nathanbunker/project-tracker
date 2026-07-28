package org.dandeliondaily.timereview.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dandeliondaily.timereview.model.AllocationActualMinutes;
import org.dandeliondaily.timereview.model.AllocationCalculationResult;
import org.dandeliondaily.timereview.model.AllocationCalculationTarget;
import org.openimmunizationsoftware.pt.model.BillExpected;
import org.openimmunizationsoftware.pt.model.BillPlan;
import org.openimmunizationsoftware.pt.model.BillPlanTarget;

public class AllocationCalculationService {

    private final AllocationPercentSupport percentSupport = new AllocationPercentSupport();

    public AllocationCalculationResult calculate(BillPlan plan, List<BillPlanTarget> targets,
            List<AllocationActualMinutes> actualMinutes, List<BillExpected> expectedRows, Date asOfDate) {
        AllocationCalculationResult result = new AllocationCalculationResult();
        result.setAsOfDate(asOfDate);
        result.setFiscalStartDate(plan == null ? null : plan.getFiscalStartDate());
        result.setFiscalEndDate(plan == null ? null : plan.getFiscalEndDate());

        int expectedYearToDate = sumExpectedMinutes(expectedRows, plan == null ? null : plan.getFiscalStartDate(),
                asOfDate);
        int expectedFullPeriod = sumExpectedMinutes(expectedRows, plan == null ? null : plan.getFiscalStartDate(),
                plan == null ? null : plan.getFiscalEndDate());
        int remainingExpected = Math.max(0, expectedFullPeriod - expectedYearToDate);
        result.setExpectedWorkedMinutesYearToDate(expectedYearToDate);
        result.setExpectedWorkedMinutesRemaining(remainingExpected);

        List<AllocationCalculationTarget> calculatedTargets = new ArrayList<AllocationCalculationTarget>();
        if (targets != null) {
            for (BillPlanTarget target : targets) {
                AllocationCalculationTarget calculated = calculateTarget(target, actualMinutes, expectedYearToDate,
                        expectedFullPeriod, remainingExpected);
                calculatedTargets.add(calculated);
            }
        }
        result.setTargets(calculatedTargets);
        return result;
    }

    private AllocationCalculationTarget calculateTarget(BillPlanTarget target,
            List<AllocationActualMinutes> actualMinutes,
            int expectedYearToDate, int expectedFullPeriod, int remainingExpected) {
        AllocationCalculationTarget calculated = new AllocationCalculationTarget();
        calculated.setBillCode(target.getBillCode());
        calculated.setAnnualTargetPercent(toPercent(target.getAnnualTargetBps()));
        calculated.setSteeringTargetPercent(toPercent(target.getSteeringTargetBps()));

        int actualYtdMinutes = sumActualMinutes(actualMinutes, target.getBillCode());
        calculated.setActualYearToDateMinutes(Integer.valueOf(actualYtdMinutes));
        calculated
                .setActualYearToDatePercent(percentSupport.ratioToPercent(percentSupport.divideMinutes(actualYtdMinutes,
                        expectedYearToDate)));

        Integer targetYtdMinutes = null;
        if (target.getAnnualTargetBps() != null) {
            targetYtdMinutes = Integer.valueOf(new BigDecimal(expectedYearToDate)
                    .multiply(new BigDecimal(target.getAnnualTargetBps()))
                    .divide(new BigDecimal("10000"), 0, RoundingMode.HALF_UP)
                    .intValue());
        }
        calculated.setTargetYearToDateMinutes(targetYtdMinutes);

        if (targetYtdMinutes != null) {
            int varianceMinutes = actualYtdMinutes - targetYtdMinutes.intValue();
            calculated.setVarianceMinutes(Integer.valueOf(varianceMinutes));
            calculated.setVariancePercentagePoints(percentSupport.ratioToPercent(
                    percentSupport.divideMinutes(varianceMinutes, expectedYearToDate)));
        }

        calculated.setRemainingExpectedWorkMinutes(Integer.valueOf(remainingExpected));
        int projectedYearEndMinutes = actualYtdMinutes;
        if (remainingExpected > 0 && target.getSteeringTargetBps() != null) {
            projectedYearEndMinutes += new BigDecimal(remainingExpected)
                    .multiply(new BigDecimal(target.getSteeringTargetBps()))
                    .divide(new BigDecimal("10000"), 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        calculated.setProjectedYearEndMinutes(Integer.valueOf(projectedYearEndMinutes));
        calculated.setProjectedYearEndPercent(percentSupport.ratioToPercent(
                percentSupport.divideMinutes(projectedYearEndMinutes, expectedFullPeriod)));

        if (target.getAnnualTargetBps() != null && remainingExpected > 0) {
            int targetYearEndMinutes = new BigDecimal(expectedFullPeriod)
                    .multiply(new BigDecimal(target.getAnnualTargetBps()))
                    .divide(new BigDecimal("10000"), 0, RoundingMode.HALF_UP)
                    .intValue();
            int remainingMinutesNeeded = targetYearEndMinutes - actualYtdMinutes;
            calculated.setRequiredRemainingPeriodPercent(percentSupport.ratioToPercent(
                    percentSupport.divideMinutes(remainingMinutesNeeded, remainingExpected)));
        } else {
            calculated.setRequiredRemainingPeriodPercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return calculated;
    }

    private BigDecimal toPercent(Integer basisPoints) {
        if (basisPoints == null) {
            return null;
        }
        return percentSupport.basisPointsToPercent(basisPoints.intValue());
    }

    private int sumActualMinutes(List<AllocationActualMinutes> actualMinutes, String billCode) {
        if (actualMinutes == null || billCode == null) {
            return 0;
        }
        int total = 0;
        for (AllocationActualMinutes item : actualMinutes) {
            if (item != null && billCode.equals(item.getBillCode())) {
                total += item.getTotalMinutes();
            }
        }
        return total;
    }

    private int sumExpectedMinutes(List<BillExpected> rows, Date startDateInclusive, Date endDateInclusive) {
        if (rows == null || startDateInclusive == null || endDateInclusive == null) {
            return 0;
        }
        int total = 0;
        for (BillExpected row : rows) {
            if (row == null || row.getId() == null || row.getId().getBillDate() == null) {
                continue;
            }
            Date billDate = row.getId().getBillDate();
            if (billDate.before(startDateInclusive) || billDate.after(endDateInclusive)) {
                continue;
            }
            if (!isWorkedStatus(row.getWorkStatus())) {
                continue;
            }
            total += Math.max(0, row.getBillMins());
        }
        return total;
    }

    boolean isWorkedStatus(String workStatus) {
        if (workStatus == null || workStatus.trim().length() == 0) {
            return true;
        }
        return !"N".equalsIgnoreCase(workStatus.trim());
    }
}