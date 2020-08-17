package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;
import java.util.TreeMap;

public class ConversionRates {

    public ConversionRates() {
        // Read the conversion rates from the data (units.txt).
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        ConversionRatesSink sink = new ConversionRatesSink();
        resource.getAllItemsWithFallback(Constants.conversionUnitTableName, sink);
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
        UMeasureSIPrefix siPrefix = singleUnit.getSiPrefix();
        Factor result = Factor.precessFactor(mapToConversionRate.get(singleUnit.getSimpleUnit()).getConversionRate());

        return result.power(power).applySiPrefix(siPrefix);
    }

    public Factor getFactorToBase(MeasureUnitImpl measureUnit) {
        Factor result = new Factor();
        for (SingleUnitImpl singleUnit :
                measureUnit.getSingleUnits()) {
            result = result.multiply(getFactorToBase(singleUnit));
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
     * Map from any simple unit (i.e. "meter", "foot", "inch") to its basic/root conversion rate info.
     */
    private TreeMap<String, ConversionRate> mapToConversionRate;


}
