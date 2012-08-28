/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * A cache containing data by locale for {@link CompactDecimalFormat}
 *
 * @author Travis Keep
 */
class CompactDecimalDataCache {

    private static final int MAX_DIGITS = 15;
    private final ICUCache<ULocale, Data> cache = new SimpleCache<ULocale, Data>();

    /**
     * Data contains the compact decimal data for a particular locale. Data consists
     * of three arrays. The index of each array corresponds to log10 of the number
     * being formatted, so when formatting 12,345, the 4th index of the arrays should
     * be used. Divisors contain the number to divide by before doing formatting.
     * In the case of english, <code>divisors[4]</code> is 1000.  So to format
     * 12,345, divide by 1000 to get 12. prefix and suffix contain the prefix and
     * suffix to use, for english, <code>suffix[4]</code> is "K" So ultimately,
     * 12,345 is formatted as 12K.
     *
     * Each array in data is 15 in length, and every index is filled.
     *
     * @author Travis Keep
     *      *
     */
    static class Data {
        long[] divisors;
        String[] prefixes;
        String[] suffixes;

        Data(long[] divisors, String[] prefixes, String[] suffixes) {
            this.divisors = divisors;
            this.prefixes = prefixes;
            this.suffixes = suffixes;
        }
    }

    /**
     * Fetch data for a particular locale.
     */
    Data get(ULocale locale) {
        Data result = cache.get(locale);
        if (result == null) {
            result = load(locale);
            cache.put(locale, result);
        }
        return result;
    }

    private static Data load(ULocale ulocale) {
        NumberingSystem ns = NumberingSystem.getInstance(ulocale);
        ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ulocale);
        r = r.getWithFallback("NumberElements/" + ns.getName() + "/patternsShort/decimalFormat");
        int size = r.getSize();
        Data result = new Data(
                new long[MAX_DIGITS], new String[MAX_DIGITS], new String[MAX_DIGITS]);
        for (int i = 0; i < size; i++) {
            populateData((ICUResourceBundle) r.get(i), result);
        }
        fillInMissingAndFixRedundant(result);
        return result;
    }

    /**
     * Populates Data object with data for a particular divisor from resource bundle.
     */
    private static void populateData(ICUResourceBundle divisorData, Data result) {
        long divisor = Long.parseLong(divisorData.getKey());
        int thisIndex = (int) Math.log10(divisor);
        // Silently ignore divisors that are too big.
        if (thisIndex >= MAX_DIGITS) {
            return;
        }
        ICUResourceBundle other = (ICUResourceBundle) divisorData.get("other");
        populatePrefixSuffix(other.getString(), thisIndex, result);
        result.divisors[thisIndex] = divisor;
    }

    /**
     * Extracts the prefix and suffix from the template and places them in the
     * Data object.
     * @param template the number template, e.g 000K
     * @param idx the index to store the extracted prefix and suffix
     * @param result Data object modified in-place here.
     */
    private static void populatePrefixSuffix(String template, int idx, Data result) {
        int firstIdx = template.indexOf("0");
        int lastIdx = template.lastIndexOf("0");
        result.prefixes[idx] = template.substring(0, firstIdx);
        result.suffixes[idx] = template.substring(lastIdx + 1);
    }

    /**
     * After reading information from resource bundle into a Data object, there
     * is no guarantee that every index of the arrays will be filled. Moreover,
     * the resource bundle may contain redundant information. After reading
     * the resource for english, we may end up with:
     * <pre>
     * index divisor prefix suffix
     * 0     <none>  <none> <none>
     * 1     <none>  <none> <none>
     * 2     <none>  <none> <none>
     * 3     1000           K
     * 4     10000          K
     * 5     100000         K
     * 6     1000000        M
     * ...
     * </pre>
     * The 10000 and 100000 are redundant. In fact, this data will cause CompactDecimalFormatter
     * to format 12345 as 1.2K instead of 12K.  Moreover, no data was populated for
     * indexes 0, 1, or 2. What we want is something like this:
     * <pre>
     * index divisor prefix suffix
     * 0     1
     * 1     1
     * 2     1
     * 3     1000           K
     * 4     1000           K
     * 5     1000           K
     * 6     1000000        M
     * ...
     * </pre>
     * This function walks through the arrays filling in missing data and replacing
     * redundant data.  If prefix is missing for an index, we fill in that index
     * with the values from the previous index. If prefix and suffix for an index
     * are the same as the previous index, we consider that redundant data and we
     * replace the divisor with the one from the previous index.
     *
     * @param result this instance is fixed in-place.
     */
    private static void fillInMissingAndFixRedundant(Data result) {
        // Initially we assume that previous divisor is 1 with no prefix or suffix.
        long lastDivisor = 1L;
        String lastPrefix = "";
        String lastSuffix = "";
        for (int i = 0; i < result.divisors.length; i++) {
            if (result.prefixes[i] == null) {
                result.divisors[i] = lastDivisor;
                result.prefixes[i] = lastPrefix;
                result.suffixes[i] = lastSuffix;
            } else if (result.prefixes[i].equals(lastPrefix) && result.suffixes[i].equals(lastSuffix)) {
                result.divisors[i] = lastDivisor;
            } else {
                lastDivisor = result.divisors[i];
                lastPrefix = result.prefixes[i];
                lastSuffix = result.suffixes[i];
            }
        }
    }
}
