package com.ibm.icu.impl.units;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.util.UResourceBundle;

public class UnitsData {

    public UnitsData() {
        this.conversionRates = new ConversionRates();
        this.unitPreferences = new UnitPreferences();
        // TODO: read categories
    }

    public ConversionRates getConversionRates() {
        return conversionRates;
    }

    public UnitPreferences getUnitPreferences() {
        return unitPreferences;
    }

    public String getCategory(MeasureUnitImpl measureUnit) {
        for (int i = 0; i < this.categories.length; i++) {
            MeasureUnitImpl categoryMeasureUnit = UnitsParser.parseForIdentifier(this.categories[i].second);
            if (UnitConverter.extractConvertibility(measureUnit, categoryMeasureUnit, this.conversionRates)
                    == Convertibility.CONVERTIBLE) {
                return this.categories[i].first;
            }
        }

        return "";
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
    private Pair<String, String>[] categories;
    private static String[] simpleUnits = null;
}
