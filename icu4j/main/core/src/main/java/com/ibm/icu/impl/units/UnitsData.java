// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.impl.units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Responsible for all units data operations (retriever, analysis, extraction certain data ... etc.).
 */
public class UnitsData {
    // TODO(icu-units#122): this class can use static initialization to load the
    // data once, and provide access to it via static methods. (Partial change
    // has been done already.)

    // Array of simple unit IDs.
    private static String[] simpleUnits = null;

    // Maps from the value associated with each simple unit ID to a category
    // index number.
    private static int[] simpleUnitCategories = null;

    private ConversionRates conversionRates;
    private UnitPreferences unitPreferences;


    public UnitsData() {
        this.conversionRates = new ConversionRates();
        this.unitPreferences = new UnitPreferences();
    }

    public static String[] getSimpleUnits() {
        return simpleUnits;
    }

    static {
        // Read simple units
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
        SimpleUnitIdentifiersSink sink = new SimpleUnitIdentifiersSink();
        resource.getAllItemsWithFallback("convertUnits", sink);
        simpleUnits = sink.simpleUnits;
        simpleUnitCategories = sink.simpleUnitCategories;
    }

    public ConversionRates getConversionRates() {
        return conversionRates;
    }

    public UnitPreferences getUnitPreferences() {
        return unitPreferences;
    }

    public static int getCategoryIndexOfSimpleUnit(int simpleUnitIndex) {
        return simpleUnitCategories[simpleUnitIndex];
    }

    /**
     * @param measureUnit An instance of MeasureUnitImpl.
     * @return the corresponding category.
     */
    public String getCategory(MeasureUnitImpl measureUnit) {
        MeasureUnitImpl baseMeasureUnitImpl
                = this.getConversionRates().extractCompoundBaseUnit(measureUnit);
        baseMeasureUnitImpl.serialize();
        String identifier = baseMeasureUnitImpl.getIdentifier();


        Integer index = Categories.baseUnitToIndex.get(identifier);

        // In case the base unit identifier did not match any entry.
        if (index == null) {
            baseMeasureUnitImpl.takeReciprocal();
            baseMeasureUnitImpl.serialize();
            identifier = baseMeasureUnitImpl.getIdentifier();
            index = Categories.baseUnitToIndex.get(identifier);
        }

        // In case the reciprocal of the base unit identifier did not match any entry.
        baseMeasureUnitImpl.takeReciprocal(); // return to original form
        MeasureUnitImpl simplifiedUnit = baseMeasureUnitImpl.copyAndSimplify();
        if (index == null) {
            simplifiedUnit.serialize();
            identifier = simplifiedUnit.getIdentifier();
            index = Categories.baseUnitToIndex.get(identifier);
        }

        // In case the simplified base unit identifier did not match any entry.
        if (index == null) {
            simplifiedUnit.takeReciprocal();
            simplifiedUnit.serialize();
            identifier = simplifiedUnit.getIdentifier();
            index = Categories.baseUnitToIndex.get(identifier);
        }

        // If there is no match at all, throw an exception.
        if (index == null) {
            throw new IllegalIcuArgumentException("This unit does not has a category" + measureUnit.getIdentifier());
        }

        return Categories.indexToCategory[index];
    }

    public UnitPreferences.UnitPreference[] getPreferencesFor(String category, String usage, ULocale locale) {
        return this.unitPreferences.getPreferencesFor(category, usage, locale, this);
    }

    public static class SimpleUnitIdentifiersSink extends UResource.Sink {
        String[] simpleUnits = null;
        int[] simpleUnitCategories = null;

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            assert key.toString().equals(Constants.CONVERSION_UNIT_TABLE_NAME);
            assert value.getType() == UResourceBundle.TABLE;

            UResource.Table simpleUnitsTable = value.getTable();
            ArrayList<String> simpleUnits = new ArrayList<>();
            ArrayList<Integer> simpleUnitCategories = new ArrayList<>();
            for (int i = 0; simpleUnitsTable.getKeyAndValue(i, key, value); i++) {
                if (key.toString().equals("kilogram")) {

                    // For parsing, we use "gram", the prefixless metric mass unit. We
                    // thus ignore the SI Base Unit of Mass: it exists due to being the
                    // mass conversion target unit, but not needed for MeasureUnit
                    // parsing.
                    continue;
                }

                // Find the base target unit for this simple unit
                UResource.Table table = value.getTable();
                if (!table.findValue("target", value)) {
                    // TODO: is there a more idiomatic way to deal with Resource
                    // Sink data errors in ICU4J? For now we just assert-fail,
                    // and otherwise skip bad data:
                    assert false : "Could not find \"target\" for simple unit: " + key;
                    continue;
                }
                String target = value.getString();

                simpleUnits.add(key.toString());
                simpleUnitCategories.add(Categories.baseUnitToIndex.get(target));
            }

            this.simpleUnits = simpleUnits.toArray(new String[0]);
            this.simpleUnitCategories = new int[simpleUnitCategories.size()];
            Iterator<Integer> iter = simpleUnitCategories.iterator();
            for (int i = 0; i < this.simpleUnitCategories.length; i++)
            {
                this.simpleUnitCategories[i] = iter.next().intValue();
            }
        }
    }

    /**
     * Contains all the needed constants.
     */
    public static class Constants {
        // TODO: consider moving the Trie-offset-related constants into
        // MeasureUnitImpl.java, the only place they're being used?

        // Trie value offset for simple units, e.g. "gram", "nautical-mile",
        // "fluid-ounce-imperial".
        public static final int kSimpleUnitOffset = 512;

        // Trie value offset for powers like "square-", "cubic-", "pow2-" etc.
        public static final int kPowerPartOffset = 256;


        // Trie value offset for "per-".
        public final static int kInitialCompoundPartOffset = 192;

        // Trie value offset for compound parts, e.g. "-per-", "-", "-and-".
        public final static int kCompoundPartOffset = 128;

        // Trie value offset for SI or binary prefixes. This is big enough to
        // ensure we only insert positive integers into the trie.
        public static final int kPrefixOffset = 64;


        /* Tables Names*/
        public static final String CONVERSION_UNIT_TABLE_NAME = "convertUnits";
        public static final String UNIT_PREFERENCE_TABLE_NAME = "unitPreferenceData";
        public static final String CATEGORY_TABLE_NAME = "unitQuantities";
        public static final String DEFAULT_REGION = "001";
        public static final String DEFAULT_USAGE = "default";
    }

    // Deals with base units and categories, e.g. "meter-per-second" --> "speed".
    public static class Categories {
        /**
         * Maps from base unit to an index value: an index into the
         * indexToCategory array.
         */
        static HashMap<String, Integer> baseUnitToIndex;

        /**
         * Our official array of category strings - categories are identified by
         * indeces into this array.
         */
        static String[] indexToCategory;

        static {
            // Read unit Categories
            ICUResourceBundle resource;
            resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "units");
            CategoriesSink sink = new CategoriesSink();
            resource.getAllItemsWithFallback(Constants.CATEGORY_TABLE_NAME, sink);
            baseUnitToIndex = sink.mapFromUnitToIndex;
            indexToCategory = sink.categories.toArray(new String[0]);
        }
    }

    /**
     * A Resource Sink that collects information from {@code unitQuantities} in the
     * {@code units} resource to provide key->value lookups from base unit to
     * category, as well as preserving ordering information for these
     * categories. See {@code units.txt}.
     *
     * For example: "kilogram" -> "mass", "meter-per-second" -> "speed".
     *
     * In Java unitQuantity values are collected in order into an ArrayList,
     * while unitQuantity key-to-index lookups are handled with a HashMap.
     */
    public static class CategoriesSink extends UResource.Sink {
        /**
         * Contains the map between units in their base units into their category.
         * For example:  meter-per-second --> "speed"
         */
        HashMap<String, Integer> mapFromUnitToIndex;
        ArrayList<String> categories;

        public CategoriesSink() {
            mapFromUnitToIndex = new HashMap<>();
            categories = new ArrayList<>();
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            assert (key.toString().equals(Constants.CATEGORY_TABLE_NAME));
            assert (value.getType() == UResourceBundle.ARRAY);

            UResource.Array categoryArray = value.getArray();
            for (int i=0; categoryArray.getValue(i, value); i++) {
                assert (value.getType() == UResourceBundle.TABLE);
                UResource.Table table = value.getTable();
                assert (table.getSize() == 1)
                    : "expecting single-entry table, got size: " + table.getSize();
                table.getKeyAndValue(0, key, value);
                assert value.getType() == UResourceBundle.STRING : "expecting category string";
                mapFromUnitToIndex.put(key.toString(), categories.size());
                categories.add(value.toString());
            }
        }
    }
}
