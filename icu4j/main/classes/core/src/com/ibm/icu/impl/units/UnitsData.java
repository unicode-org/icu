package com.ibm.icu.impl.units;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.util.UResourceBundle;

import java.util.Iterator;
import java.util.Map;

public class UnitsData {

    public UnitsData() {
        this.conversionRates = new ConversionRates();
        this.unitPreferences = UnitPreferences.getUnitPreferences();
        this.categories = new Categories();
    }

    public ConversionRates getConversionRates() {
        return conversionRates;
    }

    public UnitPreferences getUnitPreferences() {
        return unitPreferences;
    }

    /**
     * TODO: add comment
     *
     * @param measureUnit
     * @return
     */
    public String getCategory(MeasureUnitImpl measureUnit) {
        MeasureUnitImpl baseMeasureUnit
                = this.getConversionRates().getBasicMeasureUnitImplWithoutSIPrefix(measureUnit);
       String baseUnitIdentifier = baseMeasureUnit.getIdentifier();

       if (baseUnitIdentifier.equals("meter-per-cubic-meter")) {
        // TODO(CLDR-13787,hugovdm): special-casing the consumption-inverse
        // case. Once CLDR-13787 is clarified, this should be generalised (or
        // possibly removed):

            return "consumption-inverse";
        }

       return this.categories.mapFromUnitToCategory.get(baseUnitIdentifier);
    }

    public UnitPreference[] getPreferencesFor(String category, String usage, String region) {
        return this.unitPreferences.getPreferencesFor(category, usage, region);
    }

    public static String[] getSimpleUnits() {
        if (simpleUnits != null) {
            return simpleUnits;
        }

        // Read simple units
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        SimpleUnitsSink sink = new SimpleUnitsSink();
        resource.getAllItemsWithFallback("convertUnits", sink);
        simpleUnits = sink.simpleUnits;

        return simpleUnits;
    }


    private ConversionRates conversionRates;
    private UnitPreferences unitPreferences;

    /**
     * Pairs of categories and the corresponding base units.
     */
    private Categories categories;
    private static String[] simpleUnits = null;
}
