package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.UResourceBundle;

import java.util.TreeMap;

public class UnitPreferences {

    public static String formMapKey(String category, String usage, String region) {
        return category + "++" + usage + "++" + region;
    }

    public UnitPreferences() {
        // Read unit preferences
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        UnitPreferencesSink sink = new UnitPreferencesSink();
        resource.getAllItemsWithFallback(Constants.unitPreferenceTableName, sink);
        this.mapToUnitPreferences = sink.getMapToUnitPreferences();
    }


    public UnitPreference[] getPreferencesFor(String category, String usage, String region) {
        if (mapToUnitPreferences.containsKey(formMapKey(category, usage, region))) {
            return mapToUnitPreferences.get(formMapKey(category, usage, region));
        } else if ( mapToUnitPreferences.containsKey(formMapKey(category, usage, Constants.defaultRegion))) {
            return mapToUnitPreferences.get(formMapKey(category, usage, Constants.defaultRegion));
        }else if ( mapToUnitPreferences.containsKey(formMapKey(category, usage, Constants.defaultRegion))) {
            return mapToUnitPreferences.get(formMapKey(category, Constants.defaultUsage, Constants.defaultRegion));
        }

        Assert.fail("At least the category must be exist");
        return null;
    }


    private TreeMap<String, UnitPreference[]> mapToUnitPreferences = new TreeMap<>();

}
