// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html


package com.ibm.icu.impl.units;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;

/**
 * Responsible for all units data operations (retriever, analysis, extraction certain data ... etc.).
 */
class UnitsData {
    private volatile static String[] simpleUnits = null;

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
}
