// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html


package com.ibm.icu.impl.units;

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.UResourceBundle;

/**
 * Responsible for all units data operations (retriever, analysis, extraction certain data ... etc.).
 */
public class UnitsData {
    private volatile static String[] simpleUnits = null;
    private ConversionRates conversionRates;
    private UnitPreferences unitPreferences;
    /**
     * Pairs of categories and the corresponding base units.
     */
    private Categories categories;

    public UnitsData() {
        this.conversionRates = new ConversionRates();
        this.unitPreferences = new UnitPreferences();
        this.categories = new Categories();
    }

    public static String[] getSimpleUnits() {
        if (simpleUnits != null) {
            return simpleUnits;
        }

        // Read simple units
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        SimpleUnitIdentifiersSink sink = new SimpleUnitIdentifiersSink();
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
     * @param measureUnit An instance of MeasureUnitImpl.
     * @return the corresponding category.
     */
    public String getCategory(MeasureUnitImpl measureUnit) {
        MeasureUnitImpl baseMeasureUnit
                = this.getConversionRates().extractCompoundBaseUnit(measureUnit);
        String baseUnitIdentifier = MeasureUnit.fromMeasureUnitImpl(baseMeasureUnit).getIdentifier();

        if (baseUnitIdentifier.equals("meter-per-cubic-meter")) {
            // TODO(CLDR-13787,hugovdm): special-casing the consumption-inverse
            // case. Once CLDR-13787 is clarified, this should be generalised (or
            // possibly removed):

            return "consumption-inverse";
        }

        return this.categories.mapFromUnitToCategory.get(baseUnitIdentifier);
    }

    public UnitPreferences.UnitPreference[] getPreferencesFor(String category, String usage, String region) {
        return this.unitPreferences.getPreferencesFor(category, usage, region);
    }

    public static class SimpleUnitIdentifiersSink extends UResource.Sink {
        String[] simpleUnits = null;

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            assert key.toString().equals(Constants.CONVERSION_UNIT_TABLE_NAME);
            assert value.getType() == UResourceBundle.TABLE;

            UResource.Table simpleUnitsTable = value.getTable();
            ArrayList<String> simpleUnits = new ArrayList<>();
            for (int i = 0; simpleUnitsTable.getKeyAndValue(i, key, value); i++) {
                if (key.toString().equals("kilogram")) {

                    // For parsing, we use "gram", the prefixless metric mass unit. We
                    // thus ignore the SI Base Unit of Mass: it exists due to being the
                    // mass conversion target unit, but not needed for MeasureUnit
                    // parsing.
                    continue;
                }

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

    public static class Categories {

        /**
         * Contains the map between units in their base units into their category.
         * For example:  meter-per-second --> "speed"
         */
        HashMap<String, String> mapFromUnitToCategory;


        public Categories() {
            // Read unit Categories
            ICUResourceBundle resource;
            resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
            CategoriesSink sink = new CategoriesSink();
            resource.getAllItemsWithFallback(Constants.CATEGORY_TABLE_NAME, sink);
            this.mapFromUnitToCategory = sink.getMapFromUnitToCategory();
        }
    }

    public static class CategoriesSink extends UResource.Sink {
        /**
         * Contains the map between units in their base units into their category.
         * For example:  meter-per-second --> "speed"
         */
        HashMap<String, String> mapFromUnitToCategory;

        public CategoriesSink() {
            mapFromUnitToCategory = new HashMap<>();
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            assert (key.toString().equals(Constants.CATEGORY_TABLE_NAME));
            assert (value.getType() == UResourceBundle.TABLE);

            UResource.Table categoryTable = value.getTable();
            for (int i = 0; categoryTable.getKeyAndValue(i, key, value); i++) {
                assert (value.getType() == UResourceBundle.STRING);
                mapFromUnitToCategory.put(key.toString(), value.toString());
            }
        }

        public HashMap<String, String> getMapFromUnitToCategory() {
            return mapFromUnitToCategory;
        }
    }
}
