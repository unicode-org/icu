// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html


package com.ibm.icu.impl.units;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.UResourceBundle;

public class ConversionRates {

    /**
     * Map from any simple unit (i.e. "meter", "foot", "inch") to its basic/root conversion rate info.
     */
    private HashMap<String, ConversionRateInfo> mapToConversionRate;

    public ConversionRates() {
        // Read the conversion rates from the data (units.txt).
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        ConversionRatesSink sink = new ConversionRatesSink();
        resource.getAllItemsWithFallback(UnitsData.Constants.CONVERSION_UNIT_TABLE_NAME, sink);
        this.mapToConversionRate = sink.getMapToConversionRate();
    }

    /**
     * Extracts the factor from a {@code SingleUnitImpl} to its Basic Unit.
     *
     * @param singleUnit
     * @return
     */
    // In ICU4C, this is called loadCompoundFactor().
    private UnitsConverter.Factor getFactorToBase(SingleUnitImpl singleUnit) {
        int power = singleUnit.getDimensionality();
        MeasureUnit.MeasurePrefix unitPrefix = singleUnit.getPrefix();
        UnitsConverter.Factor result = UnitsConverter.Factor.processFactor(mapToConversionRate.get(singleUnit.getSimpleUnitID()).getConversionRate());

        // Prefix before power, because:
        // - square-kilometer to square-meter: (1000)^2
        // - square-kilometer to square-foot (approximate): (3.28*1000)^2
        return result.applyPrefix(unitPrefix).power(power);
    }

    public UnitsConverter.Factor getFactorToBase(MeasureUnitImpl measureUnit) {
        UnitsConverter.Factor result = new UnitsConverter.Factor();
        for (SingleUnitImpl singleUnit :
                measureUnit.getSingleUnits()) {
            result = result.multiply(getFactorToBase(singleUnit));
        }

        return result;
    }

    // In ICU4C, this functionality is found in loadConversionRate().
    protected BigDecimal getOffset(MeasureUnitImpl source, MeasureUnitImpl target, UnitsConverter.Factor
            sourceToBase, UnitsConverter.Factor targetToBase, UnitsConverter.Convertibility convertibility) {
        if (convertibility != UnitsConverter.Convertibility.CONVERTIBLE) return BigDecimal.valueOf(0);
        if (!(checkSimpleUnit(source) && checkSimpleUnit(target))) return BigDecimal.valueOf(0);

        String sourceSimpleIdentifier = source.getSingleUnits().get(0).getSimpleUnitID();
        String targetSimpleIdentifier = target.getSingleUnits().get(0).getSimpleUnitID();

        BigDecimal sourceOffset = this.mapToConversionRate.get(sourceSimpleIdentifier).getOffset();
        BigDecimal targetOffset = this.mapToConversionRate.get(targetSimpleIdentifier).getOffset();
        return sourceOffset
                .subtract(targetOffset)
                .divide(targetToBase.getConversionRate(), MathContext.DECIMAL128);


    }

    public MeasureUnitImpl extractCompoundBaseUnit(MeasureUnitImpl measureUnit) {
        ArrayList<SingleUnitImpl> baseUnits = this.extractBaseUnits(measureUnit);

        MeasureUnitImpl result = new MeasureUnitImpl();
        for (SingleUnitImpl baseUnit :
                baseUnits) {
            result.appendSingleUnit(baseUnit);
        }

        return result;
    }

    public ArrayList<SingleUnitImpl> extractBaseUnits(MeasureUnitImpl measureUnitImpl) {
        ArrayList<SingleUnitImpl> result = new ArrayList<>();
        ArrayList<SingleUnitImpl> singleUnits = measureUnitImpl.getSingleUnits();
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            result.addAll(extractBaseUnits(singleUnit));
        }

        return result;
    }

    /**
     * @param singleUnit An instance of SingleUnitImpl.
     * @return The base units in the {@code SingleUnitImpl} with applying the dimensionality only and not the SI prefix.
     * <p>
     * NOTE:
     * This method is helpful when checking the convertibility because no need to check convertibility.
     */
    public ArrayList<SingleUnitImpl> extractBaseUnits(SingleUnitImpl singleUnit) {
        String target = mapToConversionRate.get(singleUnit.getSimpleUnitID()).getTarget();
        MeasureUnitImpl targetImpl = MeasureUnitImpl.UnitsParser.parseForIdentifier(target);

        // Each unit must be powered by the same dimension
        targetImpl.applyDimensionality(singleUnit.getDimensionality());

        // NOTE: we do not apply SI prefixes.

        return targetImpl.getSingleUnits();
    }

    /**
     * Checks if the {@code MeasureUnitImpl} is simple or not.
     *
     * @param measureUnitImpl
     * @return true if the {@code MeasureUnitImpl} is simple, false otherwise.
     */
    private boolean checkSimpleUnit(MeasureUnitImpl measureUnitImpl) {
        if (measureUnitImpl.getComplexity() != MeasureUnit.Complexity.SINGLE) return false;
        SingleUnitImpl singleUnit = measureUnitImpl.getSingleUnits().get(0);

        if (singleUnit.getPrefix() != MeasureUnit.MeasurePrefix.ONE) return false;
        if (singleUnit.getDimensionality() != 1) return false;

        return true;
    }

    public static class ConversionRatesSink extends UResource.Sink {
        /**
         * Map from any simple unit (i.e. "meter", "foot", "inch") to its basic/root conversion rate info.
         */
        private HashMap<String, ConversionRateInfo> mapToConversionRate = new HashMap<>();

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            assert (UnitsData.Constants.CONVERSION_UNIT_TABLE_NAME.equals(key.toString()));

            UResource.Table conversionRateTable = value.getTable();
            for (int i = 0; conversionRateTable.getKeyAndValue(i, key, value); i++) {
                assert (value.getType() == UResourceBundle.TABLE);

                String simpleUnit = key.toString();

                UResource.Table simpleUnitConversionInfo = value.getTable();
                String target = null;
                String factor = null;
                String offset = "0";
                for (int j = 0; simpleUnitConversionInfo.getKeyAndValue(j, key, value); j++) {
                    assert (value.getType() == UResourceBundle.STRING);


                    String keyString = key.toString();
                    String valueString = value.toString().replaceAll(" ", "");
                    if ("target".equals(keyString)) {
                        target = valueString;
                    } else if ("factor".equals(keyString)) {
                        factor = valueString;
                    } else if ("offset".equals(keyString)) {
                        offset = valueString;
                    } else if ("systems".equals(keyString)) {
                        // just ignore for time being
                    } else {
                        assert false : "The key must be target, factor, systems or offset";
                    }
                }

                // HERE a single conversion rate data should be loaded
                assert (target != null);
                assert (factor != null);

                mapToConversionRate.put(simpleUnit, new ConversionRateInfo(simpleUnit, target, factor, offset));
            }


        }

        public HashMap<String, ConversionRateInfo> getMapToConversionRate() {
            return mapToConversionRate;
        }
    }

    public static class ConversionRateInfo {

        @SuppressWarnings("unused")
        private final String simpleUnit;
        private final String target;
        private final String conversionRate;
        private final BigDecimal offset;

        public ConversionRateInfo(String simpleUnit, String target, String conversionRate, String offset) {
            this.simpleUnit = simpleUnit;
            this.target = target;
            this.conversionRate = conversionRate;
            this.offset = forNumberWithDivision(offset);
        }

        private static BigDecimal forNumberWithDivision(String numberWithDivision) {
            String[] numbers = numberWithDivision.split("/");
            assert (numbers.length <= 2);

            if (numbers.length == 1) {
                return new BigDecimal(numbers[0]);
            }

            return new BigDecimal(numbers[0]).divide(new BigDecimal(numbers[1]), MathContext.DECIMAL128);
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
         * @return The offset from this unit to the base unit.
         */
        public BigDecimal getOffset() {
            return this.offset;
        }

        /**
         * @return The conversion rate from this unit to the base unit.
         */
        public String getConversionRate() {
            return conversionRate;
        }
    }
}
