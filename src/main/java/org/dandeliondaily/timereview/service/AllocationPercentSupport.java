package org.dandeliondaily.timereview.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AllocationPercentSupport {

    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    public static final BigDecimal BASIS_POINTS_PER_PERCENT = new BigDecimal("100");
    public static final BigDecimal BASIS_POINTS_DENOMINATOR = new BigDecimal("10000");

    public int percentToBasisPoints(BigDecimal percent) {
        if (percent == null) {
            throw new IllegalArgumentException("Percent is required.");
        }
        BigDecimal basisPoints = percent.multiply(BASIS_POINTS_PER_PERCENT);
        return basisPoints.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
    }

    public BigDecimal basisPointsToPercent(int basisPoints) {
        return new BigDecimal(basisPoints).divide(BASIS_POINTS_PER_PERCENT, 2, RoundingMode.UNNECESSARY);
    }

    public BigDecimal basisPointsToRatio(Integer basisPoints) {
        if (basisPoints == null) {
            return null;
        }
        return new BigDecimal(basisPoints).divide(BASIS_POINTS_DENOMINATOR, 8, RoundingMode.HALF_UP);
    }

    public BigDecimal divideMinutes(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), 8, RoundingMode.HALF_UP);
    }

    public BigDecimal ratioToPercent(BigDecimal ratio) {
        if (ratio == null) {
            return null;
        }
        return ratio.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }
}