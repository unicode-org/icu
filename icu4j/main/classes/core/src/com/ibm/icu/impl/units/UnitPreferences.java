// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.units;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class UnitPreferences {
    private static final Map<String, String> measurementSystem;

    static {
        Map tempMS = new HashMap<String, String>();
        tempMS.put("metric", "001");
        tempMS.put("ussystem", "US");
        tempMS.put("uksystem", "GB");
        measurementSystem = Collections.unmodifiableMap(tempMS);
    }


    private HashMap<String, HashMap<String, UnitPreference[]>> mapToUnitPreferences = new HashMap<>();

    public UnitPreferences() {
        // Read unit preferences
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        UnitPreferencesSink sink = new UnitPreferencesSink();
        resource.getAllItemsWithFallback(UnitsData.Constants.UNIT_PREFERENCE_TABLE_NAME, sink);
        this.mapToUnitPreferences = sink.getMapToUnitPreferences();
    }

    public static String formMapKey(String category, String usage) {
        return category + "++" + usage;
    }

    /**
     * Extracts all the sub-usages from a usage including the default one in the end.
     * The usages will be in order starting with the longest matching one.
     * For example:
     * if usage                :   "person-height-child"
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

    public UnitPreference[] getPreferencesFor(String category, String usage, ULocale locale, UnitsData data) {
        // TODO: remove this condition when all the categories are allowed.
        if (category.equals("temperature")) {
            String localeUnit = locale.getKeywordValue("mu");
            String localeUnitCategory;
            try {
                localeUnitCategory = localeUnit == null ? null : data.getCategory(MeasureUnitImpl.forIdentifier(localeUnit));
            } catch (Exception e) {
                localeUnitCategory = null;
            }

            if (localeUnitCategory != null && category.equals(localeUnitCategory)) {
                UnitPreference[] preferences = {new UnitPreference(localeUnit, null, null)};
                return preferences;
            }
        }

        String region = locale.getCountry();

        // Check the locale system tag, e.g `ms=metric`.
        String localeSystem = locale.getKeywordValue("measure");
        boolean isLocaleSystem = false;
        if (measurementSystem.containsKey(localeSystem)) {
            isLocaleSystem = true;
            region = measurementSystem.get(localeSystem);
        }

        // Check the region tag, e.g. `rg=uszzz`.
        if (!isLocaleSystem) {
            String localeRegion = locale.getKeywordValue("rg");
            if (localeRegion != null && localeRegion.length() >= 3) {
                if (localeRegion.equals("default")) {
                    region = localeRegion;
                } else if (Character.isDigit(localeRegion.charAt(0))) {
                    region = localeRegion.substring(0, 3); // e.g. 001
                } else {
                    // Capitalize the first two character of the region, e.g. ukzzzz or usca
                    region = localeRegion.substring(0, 2).toUpperCase(Locale.ROOT);
                }
            }
        }

        String[] subUsages = getAllUsages(usage);
        UnitPreference[] result = null;
        for (String subUsage :
                subUsages) {
            result = getUnitPreferences(category, subUsage, region);
            if (result != null) break;
        }

        // TODO: if a category is missing, we get an assertion failure, or we
        // return null, causing a NullPointerException. In C++, we return an
        // U_MISSING_RESOURCE_ERROR error.
        assert (result != null) : "At least the category must be exist";
        return result;
    }

    /**
     * @param category
     * @param usage
     * @param region
     * @return null if there is no entry associated to the category and usage. O.W. returns the corresponding UnitPreference[]
     */
    private UnitPreference[] getUnitPreferences(String category, String usage, String region) {
        String key = formMapKey(category, usage);
        if (this.mapToUnitPreferences.containsKey(key)) {
            HashMap<String, UnitPreference[]> unitPreferencesMap = this.mapToUnitPreferences.get(key);
            UnitPreference[] result =
                    unitPreferencesMap.containsKey(region) ?
                            unitPreferencesMap.get(region) :
                            unitPreferencesMap.get(UnitsData.Constants.DEFAULT_REGION);

            assert (result != null);
            return result;
        }

        return null;
    }

    public static class UnitPreference {
        private final String unit;
        private final BigDecimal geq;
        private final String skeleton;


        public UnitPreference(String unit, String geq, String skeleton) {
            this.unit = unit;
            this.geq = geq == null ? BigDecimal.valueOf( Double.MIN_VALUE) /* -inf */ :  new BigDecimal(geq);
            this.skeleton = skeleton == null? "" : skeleton;
        }

        public String getUnit() {
            return this.unit;
        }

        public BigDecimal getGeq() {
            return geq;
        }

        public String getSkeleton() {
            return skeleton;
        }
    }

    public static class UnitPreferencesSink extends UResource.Sink {

        private HashMap<String, HashMap<String, UnitPreference[]>> mapToUnitPreferences;

        public UnitPreferencesSink() {
            this.mapToUnitPreferences = new HashMap<>();
        }

        public HashMap<String, HashMap<String, UnitPreference[]>> getMapToUnitPreferences() {
            return mapToUnitPreferences;
        }

        /**
         * The unitPreferenceData structure (see icu4c/source/data/misc/units.txt) contains a
         * hierarchy of category/usage/region, within which are a set of
         * preferences. Hence three for-loops and another loop for the
         * preferences themselves.
         */
        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            assert (UnitsData.Constants.UNIT_PREFERENCE_TABLE_NAME.equals(key.toString()));

            UResource.Table categoryTable = value.getTable();
            for (int i = 0; categoryTable.getKeyAndValue(i, key, value); i++) {
                assert (value.getType() == UResourceBundle.TABLE);

                String category = key.toString();
                UResource.Table usageTable = value.getTable();
                for (int j = 0; usageTable.getKeyAndValue(j, key, value); j++) {
                    assert (value.getType() == UResourceBundle.TABLE);

                    String usage = key.toString();
                    UResource.Table regionTable = value.getTable();
                    for (int k = 0; regionTable.getKeyAndValue(k, key, value); k++) {
                        assert (value.getType() == UResourceBundle.ARRAY);

                        String region = key.toString();
                        UResource.Array preferencesTable = value.getArray();
                        ArrayList<UnitPreference> unitPreferences = new ArrayList<>();
                        for (int l = 0; preferencesTable.getValue(l, value); l++) {
                            assert (value.getType() == UResourceBundle.TABLE);

                            UResource.Table singlePrefTable = value.getTable();
                            // TODO collect the data
                            String unit = null;
                            String geq = "1";
                            String skeleton = "";
                            for (int m = 0; singlePrefTable.getKeyAndValue(m, key, value); m++) {
                                assert (value.getType() == UResourceBundle.STRING);
                                String keyString = key.toString();
                                if ("unit".equals(keyString)) {
                                    unit = value.getString();
                                } else if ("geq".equals(keyString)) {
                                    geq = value.getString();
                                } else if ("skeleton".equals(keyString)) {
                                    skeleton = value.getString();
                                } else {
                                    assert false : "key must be unit, geq or skeleton";
                                }
                            }
                            assert (unit != null);
                            unitPreferences.add(new UnitPreference(unit, geq, skeleton));
                        }

                        assert (!unitPreferences.isEmpty());
                        this.insertUnitPreferences(
                                category,
                                usage,
                                region,
                                unitPreferences.toArray(new UnitPreference[0])
                        );
                    }
                }
            }
        }

        private void insertUnitPreferences(String category, String usage, String region, UnitPreference[] unitPreferences) {
            String key = formMapKey(category, usage);
            HashMap<String, UnitPreference[]> shouldInsert;
            if (this.mapToUnitPreferences.containsKey(key)) {
                shouldInsert = this.mapToUnitPreferences.get(key);
            } else {
                shouldInsert = new HashMap<>();
                this.mapToUnitPreferences.put(key, shouldInsert);
            }

            shouldInsert.put(region, unitPreferences);
        }
    }
}
