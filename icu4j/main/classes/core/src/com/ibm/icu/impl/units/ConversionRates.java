package com.ibm.icu.impl.units;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.UResourceBundle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.TreeMap;

public class ConversionRates {

    public ConversionRates() {
        // Read the conversion rates from the data (units.txt).
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        ConversionRatesSink sink = new ConversionRatesSink();
        resource.getAllItemsWithFallback(Constants.CONVERSION_UNIT_TABLE_NAME, sink);
        this.mapToConversionRate = sink.getMapToConversionRate();
    }

    /**
     * Extracts the factor from a `SingleUnitImpl` to its Basic Unit.
     *
     * @param singleUnit
     * @return
     */
    private Factor getFactorToBase(SingleUnitImpl singleUnit) {
        int power = singleUnit.getDimensionality();
        SIPrefix siPrefix = singleUnit.getSiPrefix();
        Factor result = Factor.precessFactor(mapToConversionRate.get(singleUnit.getSimpleUnit()).getConversionRate());

        return result.applySiPrefix(siPrefix).power(power); // NOTE: you must apply the SI prefixes before the power.
    }

    public Factor getFactorToBase(MeasureUnitImpl measureUnit) {
        Factor result = new Factor();
        for (SingleUnitImpl singleUnit :
                measureUnit.getSingleUnits()) {
            result = result.multiply(getFactorToBase(singleUnit));
        }

        return result;
    }

    protected BigDecimal getOffset(MeasureUnitImpl source, MeasureUnitImpl target, Factor
            sourceToBase, Factor targetToBase, Convertibility convertibility) {
        if (convertibility != Convertibility.CONVERTIBLE) return BigDecimal.valueOf(0);
        if (!(checkSimpleUnit(source) && checkSimpleUnit(target))) return BigDecimal.valueOf(0);

        String sourceSimpleIdentifier = source.getSingleUnits().get(0).getSimpleUnit();
        String targetSimpleIdentifier = target.getSingleUnits().get(0).getSimpleUnit();

        BigDecimal sourceOffset = this.mapToConversionRate.get(sourceSimpleIdentifier).getOffset();
        BigDecimal targetOffset = this.mapToConversionRate.get(targetSimpleIdentifier).getOffset();
        return sourceOffset
                .subtract(targetOffset)
                .divide(targetToBase.getConversionRate(), MathContext.DECIMAL128);


    }

    public MeasureUnitImpl getBasicMeasureUnitImplWithoutSIPrefix(MeasureUnitImpl measureUnit) {
        ArrayList<SingleUnitImpl> baseUnits =  this.getBasicUnitsWithoutSIPrefix(measureUnit);

        MeasureUnitImpl result = new MeasureUnitImpl();
        for (SingleUnitImpl baseUnit :
                baseUnits) {
            result.appendSingleUnit(baseUnit);
        }

        return result;
    }


    public ArrayList<SingleUnitImpl> getBasicUnitsWithoutSIPrefix(MeasureUnitImpl measureUnitImpl) {
        ArrayList<SingleUnitImpl> result = new ArrayList<>();
        ArrayList<SingleUnitImpl> singleUnits = measureUnitImpl.getSingleUnits();
        for (SingleUnitImpl singleUnit :
                singleUnits) {
            result.addAll(getBasicUnitsWithoutSIPrefix(singleUnit));
        }

        return result;
    }

    /**
     * @param singleUnit
     * @return The bese units in the `SingleUnitImpl` with applying the dimensionality only and not the SI prefix.
     * <p>
     * NOTE:
     * This method is helpful when checking the convertibility because no need to check convertibility.
     */
    public ArrayList<SingleUnitImpl> getBasicUnitsWithoutSIPrefix(SingleUnitImpl singleUnit) {
        String target = mapToConversionRate.get(singleUnit.getSimpleUnit()).getTarget();
        MeasureUnitImpl targetImpl = UnitsParser.parseForIdentifier(target);

        // Each unit must be powered by the same dimension
        targetImpl.applyDimensionality(singleUnit.getDimensionality());

        // NOTE: we do not apply SI prefixes.

        return targetImpl.getSingleUnits();
    }

    /**
     * Checks if the `MeasureUnitImpl` is simple or not.
     *
     * @param measureUnitImpl
     * @return true if the `MeasureUnitImpl` is simple, false otherwise.
     */
    private boolean checkSimpleUnit(MeasureUnitImpl measureUnitImpl) {
        if (measureUnitImpl.getComplexity() != Complexity.SINGLE) return false;
        SingleUnitImpl singleUnit = measureUnitImpl.getSingleUnits().get(0);

        if (singleUnit.getSiPrefix() != SIPrefix.SI_PREFIX_ONE) return false;
        if (singleUnit.getDimensionality() != 1) return false;

        return true;
    }

    /**
     * Map from any simple unit (i.e. "meter", "foot", "inch") to its basic/root conversion rate info.
     */
    private TreeMap<String, ConversionRate> mapToConversionRate;
}
