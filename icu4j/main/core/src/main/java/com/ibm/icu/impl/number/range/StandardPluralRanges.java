// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.range;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;

/**
 * @author sffc
 *
 */
public class StandardPluralRanges {

    StandardPlural[] flatTriples;
    int numTriples = 0;

    /**
     * An immutable map from language codes to set IDs.
     * Pre-computed and cached in Java since it is used as a cache key for PluralRules.
     */
    private static volatile Map<String, String> languageToSet;

    /** An empty StandardPluralRanges instance. */
    public static final StandardPluralRanges DEFAULT = new StandardPluralRanges();

    ////////////////////

    private static final class PluralRangeSetsDataSink extends UResource.Sink {

        Map<String, String> output;

        PluralRangeSetsDataSink(Map<String, String> output) {
            this.output = output;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); ++i) {
                // The data has only languages; no regions/scripts. If this changes, this
                // code and languageToSet will need to change.
                assert key.toString().equals(new ULocale(key.toString()).getLanguage());
                output.put(key.toString(), value.toString());
            }
        }
    }

    private static Map<String, String> getLanguageToSet() {
        Map<String, String> candidate = languageToSet;
        if (candidate == null) {
            Map<String, String> map = new HashMap<String, String>();
            PluralRangeSetsDataSink sink = new PluralRangeSetsDataSink(map);
            ICUResourceBundle resource = (ICUResourceBundle)
                UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "pluralRanges");
            resource.getAllItemsWithFallback("locales", sink);
            candidate = Collections.unmodifiableMap(map);
        }
        // Check if another thread set languageToSet in the mean time
        if (languageToSet == null) {
            languageToSet = candidate;
        }
        return languageToSet;
    }

    private static final class PluralRangesDataSink extends UResource.Sink {

        StandardPluralRanges output;

        PluralRangesDataSink(StandardPluralRanges output) {
            this.output = output;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            UResource.Array entriesArray = value.getArray();
            output.setCapacity(entriesArray.getSize());
            for (int i = 0; entriesArray.getValue(i, value); ++i) {
                UResource.Array pluralFormsArray = value.getArray();
                if (pluralFormsArray.getSize() != 3) {
                    throw new UResourceTypeMismatchException(
                        "Expected 3 elements in pluralRanges.txt array");
                }
                pluralFormsArray.getValue(0, value);
                StandardPlural first = StandardPlural.fromString(value.getString());
                pluralFormsArray.getValue(1, value);
                StandardPlural second = StandardPlural.fromString(value.getString());
                pluralFormsArray.getValue(2, value);
                StandardPlural result = StandardPlural.fromString(value.getString());
                output.addPluralRange(first, second, result);
            }
        }
    }

    private static void getPluralRangesData(
            String set,
            StandardPluralRanges out) {
        StringBuilder sb = new StringBuilder();
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "pluralRanges");
        sb.setLength(0);
        sb.append("rules/");
        sb.append(set);
        String key = sb.toString();
        PluralRangesDataSink sink = new PluralRangesDataSink(out);
        resource.getAllItemsWithFallback(key, sink);
    }

    ////////////////////

    /** Create a StandardPluralRanges based on locale. */
    public static StandardPluralRanges forLocale(ULocale locale) {
        return forSet(getSetForLocale(locale));
    }

    /** Create a StandardPluralRanges based on set name. */
    public static StandardPluralRanges forSet(String set) {
        StandardPluralRanges result = new StandardPluralRanges();
        if (set == null) {
            // Not all languages are covered: fail gracefully
            return DEFAULT;
        }
        getPluralRangesData(set, result);
        return result;
    }

    /** Get the set name from the locale. */
    public static String getSetForLocale(ULocale locale) {
        return getLanguageToSet().get(locale.getLanguage());
    }

    private StandardPluralRanges() {
    }

    /** Used for data loading. */
    private void addPluralRange(StandardPlural first, StandardPlural second, StandardPlural result) {
        flatTriples[3 * numTriples] = first;
        flatTriples[3 * numTriples + 1] = second;
        flatTriples[3 * numTriples + 2] = result;
        numTriples++;
    }

    /** Used for data loading. */
    private void setCapacity(int length) {
        flatTriples = new StandardPlural[length*3];
    }

    public StandardPlural resolve(StandardPlural first, StandardPlural second) {
        for (int i = 0; i < numTriples; i++) {
            if (first == flatTriples[3 * i] && second == flatTriples[3 * i + 1]) {
                return flatTriples[3 * i + 2];
            }
        }
        // Default fallback
        return StandardPlural.OTHER;
    }

}
