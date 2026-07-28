package org.dandeliondaily.timereview.model;

import java.math.BigDecimal;

public class AllocationCalculationTarget {

    private String billCode;
    private BigDecimal annualTargetPercent;
    private BigDecimal steeringTargetPercent;
    private BigDecimal actualYearToDatePercent;
    private Integer targetYearToDateMinutes;
    private Integer actualYearToDateMinutes;
    private Integer varianceMinutes;
    private BigDecimal variancePercentagePoints;
    private Integer remainingExpectedWorkMinutes;
    private Integer projectedYearEndMinutes;
    private BigDecimal projectedYearEndPercent;
    private BigDecimal requiredRemainingPeriodPercent;

    public String getBillCode() {
        return billCode;
    }

    public void setBillCode(String billCode) {
        this.billCode = billCode;
    }

    public BigDecimal getAnnualTargetPercent() {
        return annualTargetPercent;
    }

    public void setAnnualTargetPercent(BigDecimal annualTargetPercent) {
        this.annualTargetPercent = annualTargetPercent;
    }

    public BigDecimal getSteeringTargetPercent() {
        return steeringTargetPercent;
    }

    public void setSteeringTargetPercent(BigDecimal steeringTargetPercent) {
        this.steeringTargetPercent = steeringTargetPercent;
    }

    public BigDecimal getActualYearToDatePercent() {
        return actualYearToDatePercent;
    }

    public void setActualYearToDatePercent(BigDecimal actualYearToDatePercent) {
        this.actualYearToDatePercent = actualYearToDatePercent;
    }

    public Integer getTargetYearToDateMinutes() {
        return targetYearToDateMinutes;
    }

    public void setTargetYearToDateMinutes(Integer targetYearToDateMinutes) {
        this.targetYearToDateMinutes = targetYearToDateMinutes;
    }

    public Integer getActualYearToDateMinutes() {
        return actualYearToDateMinutes;
    }

    public void setActualYearToDateMinutes(Integer actualYearToDateMinutes) {
        this.actualYearToDateMinutes = actualYearToDateMinutes;
    }

    public Integer getVarianceMinutes() {
        return varianceMinutes;
    }

    public void setVarianceMinutes(Integer varianceMinutes) {
        this.varianceMinutes = varianceMinutes;
    }

    public BigDecimal getVariancePercentagePoints() {
        return variancePercentagePoints;
    }

    public void setVariancePercentagePoints(BigDecimal variancePercentagePoints) {
        this.variancePercentagePoints = variancePercentagePoints;
    }

    public Integer getRemainingExpectedWorkMinutes() {
        return remainingExpectedWorkMinutes;
    }

    public void setRemainingExpectedWorkMinutes(Integer remainingExpectedWorkMinutes) {
        this.remainingExpectedWorkMinutes = remainingExpectedWorkMinutes;
    }

    public Integer getProjectedYearEndMinutes() {
        return projectedYearEndMinutes;
    }

    public void setProjectedYearEndMinutes(Integer projectedYearEndMinutes) {
        this.projectedYearEndMinutes = projectedYearEndMinutes;
    }

    public BigDecimal getProjectedYearEndPercent() {
        return projectedYearEndPercent;
    }

    public void setProjectedYearEndPercent(BigDecimal projectedYearEndPercent) {
        this.projectedYearEndPercent = projectedYearEndPercent;
    }

    public BigDecimal getRequiredRemainingPeriodPercent() {
        return requiredRemainingPeriodPercent;
    }

    public void setRequiredRemainingPeriodPercent(BigDecimal requiredRemainingPeriodPercent) {
        this.requiredRemainingPeriodPercent = requiredRemainingPeriodPercent;
    }
}