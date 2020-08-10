package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.math.BigDecimal;

public class ConversionRate {

    public ConversionRate(String simpleUnit, String target, String conversionRate, String offset) {
        this.simpleUnit = simpleUnit;
        this.target = target;
        this.conversionRate = conversionRate;
        this.offset = forNumberWithDivision(offset);
    }

    /**
     * @return the base unit.
     * <p>
     * For example:
     * ("meter", "foot", "inch", "mile" ... etc.) have "meter" as a base/root unit.
     */
    public String getTarget() {
        return this.target;
    }

    /**
     *
     * @return The offset from this unit to the base unit.
     */
    public BigDecimal getOffset() {
        return this.offset;
    }

    /**
     *
     * @return The conversion rate from this unit to the base unit.
     */
    public String getConversionRate() {
        return conversionRate;
    }

    private final String simpleUnit;
    private final String target;
    private final String conversionRate;
    private final BigDecimal offset;

    private static BigDecimal forNumberWithDivision(String numberWithDivision) {
        String[] numbers = numberWithDivision.split("/");
        Assert.assrt(numbers.length <= 2);

        if (numbers.length == 1) {
            return new BigDecimal(numbers[0]);
        }

        return new BigDecimal(numbers[0]).divide(new BigDecimal(numbers[1]));
    }


}
