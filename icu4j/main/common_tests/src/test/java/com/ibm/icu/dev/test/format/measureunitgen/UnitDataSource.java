// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Source of unit identifiers and ISO currency codes for {@link MeasureUnitGenerator}.
 *
 * <p>Today {@link IcuResourceUnitDataSource} reads ICU's resource bundles. Once CLDR's unit data
 * table (CLDR-19895) is approved, an implementation that reads {@code units_info.tsv} can plug in
 * here without changing the rest of the generator.
 */
public interface UnitDataSource {

    /** Reads all non-currency unit subtypes grouped by type, plus ISO currency codes. */
    Data readData();

    /** Immutable snapshot of unit types, subtypes, and currency codes. */
    final class Data {
        private final Map<String, Set<String>> unitsByType;
        private final Set<String> currencyCodes;

        public Data(
                Map<String, ? extends Collection<String>> unitsByType,
                Collection<String> currencyCodes) {
            TreeMap<String, Set<String>> sortedTypes = new TreeMap<>();
            for (Map.Entry<String, ? extends Collection<String>> entry : unitsByType.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    sortedTypes.put(
                            entry.getKey(),
                            Collections.unmodifiableSet(new TreeSet<>(entry.getValue())));
                }
            }
            this.unitsByType = Collections.unmodifiableMap(sortedTypes);
            this.currencyCodes = Collections.unmodifiableSet(new TreeSet<>(currencyCodes));
        }

        public Map<String, Set<String>> getUnitsByType() {
            return unitsByType;
        }

        public Set<String> getCurrencyCodes() {
            return currencyCodes;
        }
    }
}
