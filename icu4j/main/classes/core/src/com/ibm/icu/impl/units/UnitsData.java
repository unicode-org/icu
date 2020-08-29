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
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;

/**
 * Responsible for all units data operations (retriever, analysis, extraction certain data ... etc.).
 */
public class UnitsData {
    private static String[] simpleUnits = null;
    private ConversionRates conversionRates;
    private UnitPreferences unitPreferences;
    /**
     * Pairs of categories and the corresponding base units.
     */
    private Categories categories;

    public UnitsData() {
        this.conversionRates = new ConversionRates();
        this.unitPreferences = UnitPreferences.getUnitPreferences();
        this.categories = new Categories();
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

    public static class SimpleUnitsSink extends UResource.Sink {
        String[] simpleUnits = null;

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            Assert.assrt(key.toString().equals(Constants.CONVERSION_UNIT_TABLE_NAME));
            Assert.assrt(value.getType() == UResourceBundle.TABLE);

            UResource.Table simpleUnitsTable = value.getTable();
            ArrayList<String> simpleUnits = new ArrayList<>();
            for (int i = 0; simpleUnitsTable.getKeyAndValue(i, key, value); i++) {
                simpleUnits.add(key.toString());
            }

            this.simpleUnits = simpleUnits.toArray(new String[0]);
        }
    }

    /**
     * Contains all the needed constants.
     */
    public static class Constants {
        // Trie value offset for simple units, e.g. "gram", "nautical-mile",
        // "fluid-ounce-imperial".
        public static final int kSimpleUnitOffset = 512;

        // Trie value offset for powers like "square-", "cubic-", "pow2-" etc.
        public static final int kPowerPartOffset = 256;


        // Trie value offset for "per-".
        public final static int kInitialCompoundPartOffset = 192;

        // Trie value offset for compound parts, e.g. "-per-", "-", "-and-".
        public final static int kCompoundPartOffset = 128;

        // Trie value offset for SI Prefixes. This is big enough to ensure we only
        // insert positive integers into the trie.
        public static final int kSIPrefixOffset = 64;


        /* Tables Names*/
        public static final String CONVERSION_UNIT_TABLE_NAME = "convertUnits";
        public static final String UNIT_PREFERENCE_TABLE_NAME = "unitPreferenceData";
        public static final String CATEGORY_TABLE_NAME = "unitQuantities";
        public static final String DEFAULT_REGION = "001";
        public static final String DEFAULT_USAGE = "default";
    }
}
