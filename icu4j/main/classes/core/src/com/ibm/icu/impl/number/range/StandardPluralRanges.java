// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.range;

import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author sffc
 *
 */
public class StandardPluralRanges {

    StandardPlural[] flatTriples;
    int numTriples = 0;

    ////////////////////

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
            ULocale locale,
            StandardPluralRanges out) {
        StringBuilder sb = new StringBuilder();
        ICUResourceBundle resource;
        resource = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "pluralRanges");
        sb.append("locales/");
        sb.append(locale.getLanguage());
        String key = sb.toString();
        String set;
        try {
            set = resource.getStringWithFallback(key);
        } catch (MissingResourceException e) {
            // Not all languages are covered: fail gracefully
            return;
        }

        sb.setLength(0);
        sb.append("rules/");
        sb.append(set);
        key = sb.toString();
        PluralRangesDataSink sink = new PluralRangesDataSink(out);
        resource.getAllItemsWithFallback(key, sink);
    }

    ////////////////////

    public StandardPluralRanges(ULocale locale) {
        getPluralRangesData(locale, this);
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
