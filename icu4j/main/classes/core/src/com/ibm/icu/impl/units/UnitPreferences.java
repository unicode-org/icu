/*
 *******************************************************************************
 * Copyright (C) 2004-2020, Google Inc, International Business Machines
 * Corporation and others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.impl.units;

import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;
import java.util.TreeMap;

public class UnitPreferences {

    public static UnitPreferences getUnitPreferences() {
        // Read unit preferences
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        UnitPreferencesSink sink = new UnitPreferencesSink(new UnitPreferences());
        resource.getAllItemsWithFallback(UnitsData.Constants.UNIT_PREFERENCE_TABLE_NAME, sink);
        return sink.getUnitPreferences();
    }


    public UnitPreference[] getPreferencesFor(String category, String usage, String region) {
        String[] subUsages = getAllUsages(usage);
        UnitPreference[] result = null;
        for (String subUsage :
                subUsages) {
            result = getUnitPreferences(category, subUsage, region);
            if (result != null) break;
        }

        // At least the category must be exist
        Assert.assrt(result != null);
        return result;
    }

    public void insertUnitPreferences(String category, String usage, String region, UnitPreference[] unitPreferences) {
        String key = formMapKey(category, usage);
        TreeMap<String, UnitPreference[]> shouldInsert;
        if (this.mapToUnitPreferences.containsKey(key)) {
            shouldInsert = this.mapToUnitPreferences.get(key);
        } else {
            shouldInsert = new TreeMap<>();
            this.mapToUnitPreferences.put(key, shouldInsert);
        }

        shouldInsert.put(region, unitPreferences);
    }

    private static String formMapKey(String category, String usage) {
        return category + "++" + usage;
    }

    private UnitPreferences() {
    }

    /**
     * Extracts all the sub-usages from a usage including the default one in the end.
     * The usages will be in order starting with the longest matching one.
     * For example: if usage:                   "person-height-child"
     * the function will return:   "person-height-child"
     * "person-height"
     * "person"
     * "default"
     *
     * @param usage
     * @return
     */
    private static String[] getAllUsages(String usage) {
        ArrayList<String> result = new ArrayList<>();
        result.add(usage);
        for (int i = usage.length() - 1; i >= 0; --i) {
            if (usage.charAt(i) == '-') {
                result.add(usage.substring(0, i));
            }
        }

        if (!usage.equals(UnitsData.Constants.DEFAULT_USAGE)) { // Do not add default usage twice.
            result.add(UnitsData.Constants.DEFAULT_USAGE);
        }
        return result.toArray(new String[0]);
    }

    /**
     * @param category
     * @param usage
     * @param region
     * @return null if there is no entry associated to the category and usage. O.W. returns the corresponding UnitPreference[]
     * @throws error if the category and usage exist, but the region or the default region are not.
     */
    private UnitPreference[] getUnitPreferences(String category, String usage, String region) {
        String key = formMapKey(category, usage);
        if (this.mapToUnitPreferences.containsKey(key)) {
            TreeMap<String, UnitPreference[]> unitPreferencesMap = this.mapToUnitPreferences.get(key);
            UnitPreference[] result =
                    unitPreferencesMap.containsKey(region) ?
                            unitPreferencesMap.get(region) :
                            unitPreferencesMap.get(UnitsData.Constants.DEFAULT_REGION);

            Assert.assrt(result != null);
            return result;
        }

        return null;
    }

    private TreeMap<String, TreeMap<String, UnitPreference[]>> mapToUnitPreferences = new TreeMap<>();
}
