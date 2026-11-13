// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.UResourceBundle;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Loads units and currencies using ICU4J's resource-bundle loader, matching the data read by {@code
 * MeasureUnit.populateCache()}.
 *
 * <p>Reading the resource bundles directly rather than calling {@code MeasureUnit.getAvailable()}
 * keeps ICU's locale fallback and alias resolution while avoiding static constants already declared
 * on {@code MeasureUnit}, so the generator can detect when CLDR removes a unit.
 */
public final class IcuResourceUnitDataSource implements UnitDataSource {

    @Override
    public Data readData() {
        Map<String, Set<String>> unitsByType = new TreeMap<>();
        ICUResourceBundle unitBundle =
                (ICUResourceBundle)
                        UResourceBundle.getBundleInstance(ICUData.ICU_UNIT_BASE_NAME, "en");
        unitBundle.getAllItemsWithFallback("units", new UnitSink(unitsByType));

        Set<String> currencyCodes = new TreeSet<>();
        ICUResourceBundle currencyBundle =
                (ICUResourceBundle)
                        UResourceBundle.getBundleInstance(
                                ICUData.ICU_BASE_NAME,
                                "currencyNumericCodes",
                                ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        currencyBundle.getAllItemsWithFallback("codeMap", new CurrencySink(currencyCodes));

        if (unitsByType.isEmpty()) {
            throw new IllegalStateException("No units found in ICU unit resource bundle.");
        }
        if (currencyCodes.isEmpty()) {
            throw new IllegalStateException("No currencies found in currencyNumericCodes.");
        }
        return new Data(unitsByType, currencyCodes);
    }

    private static final class UnitSink extends UResource.Sink {
        private final Map<String, Set<String>> unitsByType;

        UnitSink(Map<String, Set<String>> unitsByType) {
            this.unitsByType = unitsByType;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table typesTable = value.getTable();
            for (int i = 0; typesTable.getKeyAndValue(i, key, value); ++i) {
                if (key.contentEquals("compound") || key.contentEquals("coordinate")) {
                    continue;
                }
                String type = key.toString();
                Set<String> subtypes = unitsByType.computeIfAbsent(type, k -> new TreeSet<>());
                UResource.Table subtypesTable = value.getTable();
                for (int j = 0; subtypesTable.getKeyAndValue(j, key, value); ++j) {
                    subtypes.add(key.toString());
                }
            }
        }
    }

    private static final class CurrencySink extends UResource.Sink {
        private final Set<String> currencyCodes;

        CurrencySink(Set<String> currencyCodes) {
            this.currencyCodes = currencyCodes;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table codesTable = value.getTable();
            for (int i = 0; codesTable.getKeyAndValue(i, key, value); ++i) {
                currencyCodes.add(key.toString());
            }
        }
    }
}
