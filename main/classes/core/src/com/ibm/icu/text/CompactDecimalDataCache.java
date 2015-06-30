/*
 *******************************************************************************
 * Copyright (C) 2012-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

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

    private static final String SHORT_STYLE = "short";
    private static final String LONG_STYLE = "long";
    private static final String NUMBER_ELEMENTS = "NumberElements";
    private static final String PATTERN_LONG_PATH = "patternsLong/decimalFormat";
    private static final String PATTERNS_SHORT_PATH = "patternsShort/decimalFormat";

    static final String OTHER = "other";

    /**
     * We can specify prefixes or suffixes for values with up to 15 digits,
     * less than 10^15.
     */
    static final int MAX_DIGITS = 15;

    private static final String LATIN_NUMBERING_SYSTEM = "latn";

    private final ICUCache<ULocale, DataBundle> cache =
            new SimpleCache<ULocale, DataBundle>();

    /**
     * Data contains the compact decimal data for a particular locale. Data consists
     * of one array and two hashmaps. The index of the divisors array as well
     * as the arrays stored in the values of the two hashmaps correspond
     * to log10 of the number being formatted, so when formatting 12,345, the 4th
     * index of the arrays should be used. Divisors contain the number to divide
     * by before doing formatting. In the case of english, <code>divisors[4]</code>
     * is 1000.  So to format 12,345, divide by 1000 to get 12. Then use
     * PluralRules with the current locale to figure out which of the 6 plural variants
     * 12 matches: "zero", "one", "two", "few", "many", or "other." Prefixes and
     * suffixes are maps whose key is the plural variant and whose values are
     * arrays of strings with indexes corresponding to log10 of the original number.
     * these arrays contain the prefix or suffix to use.
     *
     * Each array in data is 15 in length, and every index is filled.
     *
     * @author Travis Keep
     *
     */
    static class Data {
        long[] divisors;
        Map<String, DecimalFormat.Unit[]> units;

        Data(long[] divisors, Map<String, DecimalFormat.Unit[]> units) {
            this.divisors = divisors;
            this.units = units;
        }
    }

    /**
     * DataBundle contains compact decimal data for all the styles in a particular
     * locale. Currently available styles are short and long.
     *
     * @author Travis Keep
     */
    static class DataBundle {
        Data shortData;
        Data longData;

        DataBundle(Data shortData, Data longData) {
            this.shortData = shortData;
            this.longData = longData;
        }
    }

    private static enum QuoteState {
        OUTSIDE,   // Outside single quote
        INSIDE_EMPTY,  // Just inside single quote
        INSIDE_FULL   // Inside single quote along with characters
    }

//    private static enum DataLocation { // Don't change order
//        LOCAL,  // In local numbering system
//        LATIN,  // In latin numbering system
//        ROOT    // In root locale
//    }

    private static enum UResFlags {
        ANY,  // Any locale will do.
        NOT_ROOT  // Locale cannot be root.
    }


    /**
     * Fetch data for a particular locale. Clients must not modify any part
     * of the returned data. Portions of returned data may be shared so modifying
     * it will have unpredictable results.
     */
    DataBundle get(ULocale locale) {
        DataBundle result = cache.get(locale);
        if (result == null) {
            result = load(locale);
            cache.put(locale, result);
        }
        return result;
    }

    /**
     * Loads the "patternsShort" and "patternsLong" data for a particular locale.
     * We look for both of them in 3 places in this order:<ol>
     * <li>local numbering system no ROOT fallback</li>
     * <li>latin numbering system no ROOT fallback</li>
     * <li>latin numbering system ROOT locale.</li>
     * </ol>
     * If we find "patternsShort" data before finding "patternsLong" data, we
     * make the "patternsLong" data be the same as "patternsShort."
     * @param ulocale the locale for which we are loading the data.
     * @return The returned data, never null.
     */
    private static DataBundle load(ULocale ulocale) {
        NumberingSystem ns = NumberingSystem.getInstance(ulocale);
        ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ulocale);
        r = r.getWithFallback(NUMBER_ELEMENTS);
        String numberingSystemName = ns.getName();

        ICUResourceBundle shortDataBundle = null;
        ICUResourceBundle longDataBundle = null;
        if (!LATIN_NUMBERING_SYSTEM.equals(numberingSystemName)) {
            ICUResourceBundle bundle = findWithFallback(r, numberingSystemName, UResFlags.NOT_ROOT);
            shortDataBundle = findWithFallback(bundle, PATTERNS_SHORT_PATH, UResFlags.NOT_ROOT);
            longDataBundle = findWithFallback(bundle, PATTERN_LONG_PATH, UResFlags.NOT_ROOT);
        }

        // If we haven't found, look in latin numbering system.
        if (shortDataBundle == null) {
            ICUResourceBundle bundle = getWithFallback(r, LATIN_NUMBERING_SYSTEM, UResFlags.ANY);
            shortDataBundle = getWithFallback(bundle, PATTERNS_SHORT_PATH, UResFlags.ANY);
            if (longDataBundle == null) {
                longDataBundle = findWithFallback(bundle, PATTERN_LONG_PATH, UResFlags.ANY);
                if (longDataBundle != null && isRoot(longDataBundle) && !isRoot(shortDataBundle)) {
                    longDataBundle = null;
                }
            }
        }
        Data shortData = loadStyle(shortDataBundle, ulocale, SHORT_STYLE);
        Data longData;
        if (longDataBundle == null) {
            longData = shortData;
        } else {
            longData = loadStyle(longDataBundle, ulocale, LONG_STYLE);
        }
        return new DataBundle(shortData, longData);
    }

    /**
     * findWithFallback finds a sub-resource bundle within r.
     * @param r a resource bundle. It may be null in which case sub-resource bundle
     *   won't be found.
     * @param path the path relative to r
     * @param flags ANY or NOT_ROOT for locale of found sub-resource bundle.
     * @return The sub-resource bundle or NULL if none found.
     */
    private static ICUResourceBundle findWithFallback(
            ICUResourceBundle r, String path, UResFlags flags) {
        if (r == null) {
            return null;
        }
        ICUResourceBundle result = r.findWithFallback(path);
        if (result == null) {
            return null;
        }
        switch (flags) {
        case NOT_ROOT:
            return isRoot(result) ? null : result;
        case ANY:
            return result;
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Like findWithFallback but throws MissingResourceException if no
     * resource found instead of returning null.
     */
    private static ICUResourceBundle getWithFallback(
            ICUResourceBundle r, String path, UResFlags flags) {
        ICUResourceBundle result = findWithFallback(r, path, flags);
        if (result == null) {
            throw new MissingResourceException(
                    "Cannot find " + path,
                    ICUResourceBundle.class.getName(), path);

        }
        return result;
    }

    /**
     * isRoot returns true if r is in root locale or false otherwise.
     */
    private static boolean isRoot(ICUResourceBundle r) {
        ULocale bundleLocale = r.getULocale();
        // Note: bundleLocale for root should be ULocale.ROOT, which is equivalent to new ULocale("").
        // However, resource bundle might be initialized with locale ID "root", which should be
        // actually normalized to "" in ICUResourceBundle. For now, this logic also compare to
        // "root", not just ULocale.ROOT.
        return bundleLocale.equals(ULocale.ROOT) || bundleLocale.toString().equals("root");
    }

    /**
     * Loads the data
     * @param r the main resource bundle.
     * @param numberingSystemName The namespace name.
     * @param allowNullResult If true, returns null if no data can be found
     * for particular locale and style. If false, throws a runtime exception
     * if data cannot be found.
     * @return The loaded data or possibly null if allowNullResult is true.
     */
    private static Data loadStyle(ICUResourceBundle r, ULocale locale, String style) {
        int size = r.getSize();
        Data result = new Data(
                new long[MAX_DIGITS],
                new HashMap<String, DecimalFormat.Unit[]>());
        for (int i = 0; i < size; i++) {
            populateData(r.get(i), locale, style, result);
        }
        fillInMissing(result);
        return result;
    }

    /**
     * Populates Data object with data for a particular divisor from resource bundle.
     * @param divisorData represents the rules for numbers of a particular size.
     * This may look like:
     * <pre>
     *   10000{
     *       few{"00K"}
     *       many{"00K"}
     *       one{"00 xnb"}
     *       other{"00 xnb"}
     *   }
     * </pre>
     * @param locale the locale
     * @param style the style
     * @param result rule stored here.
     *
     */
    private static void populateData(
            UResourceBundle divisorData, ULocale locale, String style, Data result) {
        // This value will always be some even pwoer of 10. e.g 10000.
        long magnitude = Long.parseLong(divisorData.getKey());
        int thisIndex = (int) Math.log10(magnitude);

        // Silently ignore divisors that are too big.
        if (thisIndex >= MAX_DIGITS) {
            return;
        }

        int size = divisorData.getSize();

        // keep track of how many zeros are used in the plural variants.
        // For "00K" this would be 2. This number must be the same for all
        // plural variants. If they differ, we throw a runtime exception as
        // such an anomaly is unrecoverable. We expect at least one zero.
        int numZeros = 0;

        // Keep track if this block defines "other" variant. If a block
        // fails to define the "other" variant, we must immediately throw
        // an exception as it is assumed that "other" variants are always
        // defined.
        boolean otherVariantDefined = false;

        // Loop over all the plural variants. e.g one, other.
        for (int i = 0; i < size; i++) {
            UResourceBundle pluralVariantData = divisorData.get(i);
            String pluralVariant = pluralVariantData.getKey();
            String template = pluralVariantData.getString();
            if (pluralVariant.equals(OTHER)) {
                otherVariantDefined = true;
            }
            int nz = populatePrefixSuffix(
                    pluralVariant, thisIndex, template, locale, style, result);
            if (nz != numZeros) {
                if (numZeros != 0) {
                    throw new IllegalArgumentException(
                        "Plural variant '" + pluralVariant + "' template '" +
                        template + "' for 10^" + thisIndex +
                        " has wrong number of zeros in " + localeAndStyle(locale, style));
                }
                numZeros = nz;
            }
        }

        if (!otherVariantDefined) {
            throw new IllegalArgumentException(
                    "No 'other' plural variant defined for 10^" + thisIndex +
                    "in " +localeAndStyle(locale, style));
        }

        // We craft our divisor such that when we divide by it, we get a
        // number with the same number of digits as zeros found in the
        // plural variant templates. If our magnitude is 10000 and we have
        // two 0's in our plural variants, then we want a divisor of 1000.
        // Note that if we have 43560 which is of same magnitude as 10000.
        // When we divide by 1000 we a quotient which rounds to 44 (2 digits)
        long divisor = magnitude;
        for (int i = 1; i < numZeros; i++) {
            divisor /= 10;
        }
        result.divisors[thisIndex] = divisor;
    }


    /**
     * Populates prefix and suffix information for a particular plural variant
     * and index (log10 value).
     * @param pluralVariant e.g "one", "other"
     * @param idx the index (log10 value of the number) 0 <= idx < MAX_DIGITS
     * @param template e.g "00K"
     * @param locale the locale
     * @param style the style
     * @param result Extracted prefix and suffix stored here.
     * @return number of zeros found before any decimal point in template.
     */
    private static int populatePrefixSuffix(
            String pluralVariant, int idx, String template, ULocale locale, String style,
            Data result) {
        int firstIdx = template.indexOf("0");
        int lastIdx = template.lastIndexOf("0");
        if (firstIdx == -1) {
            throw new IllegalArgumentException(
                "Expect at least one zero in template '" + template +
                "' for variant '" +pluralVariant + "' for 10^" + idx +
                " in " + localeAndStyle(locale, style));
        }
        String prefix = fixQuotes(template.substring(0, firstIdx));
        String suffix = fixQuotes(template.substring(lastIdx + 1));
        saveUnit(new DecimalFormat.Unit(prefix, suffix), pluralVariant, idx, result.units);

        // If there is effectively no prefix or suffix, ignore the actual
        // number of 0's and act as if the number of 0's matches the size
        // of the number
        if (prefix.trim().length() == 0 && suffix.trim().length() == 0) {
          return idx + 1;
        }

        // Calculate number of zeros before decimal point.
        int i = firstIdx + 1;
        while (i <= lastIdx && template.charAt(i) == '0') {
            i++;
        }
        return i - firstIdx;
    }

    private static String fixQuotes(String prefixOrSuffix) {
        StringBuilder result = new StringBuilder();
        int len = prefixOrSuffix.length();
        QuoteState state = QuoteState.OUTSIDE;
        for (int idx = 0; idx < len; idx++) {
            char ch = prefixOrSuffix.charAt(idx);
            if (ch == '\'') {
                if (state == QuoteState.INSIDE_EMPTY) {
                    result.append('\'');
                }
            } else {
                result.append(ch);
            }

            // Update state
            switch (state) {
            case OUTSIDE:
                state = ch == '\'' ? QuoteState.INSIDE_EMPTY : QuoteState.OUTSIDE;
                break;
            case INSIDE_EMPTY:
            case INSIDE_FULL:
                state = ch == '\'' ? QuoteState.OUTSIDE : QuoteState.INSIDE_FULL;
                break;
            default:
                throw new IllegalStateException();
            }
        }
        return result.toString();
    }

    /**
     * Returns locale and style. Used to form useful messages in thrown
     * exceptions.
     * @param locale the locale
     * @param style the style
     */
    private static String localeAndStyle(ULocale locale, String style) {
        return "locale '" + locale + "' style '" + style + "'";
    }

    /**
     * After reading information from resource bundle into a Data object, there
     * is guarantee that it is complete.
     *
     * This method fixes any incomplete data it finds within <code>result</code>.
     * It looks at each log10 value applying the two rules.
     *   <p>
     *   If no prefix is defined for the "other" variant, use the divisor, prefixes and
     *   suffixes for all defined variants from the previous log10. For log10 = 0,
     *   use all empty prefixes and suffixes and a divisor of 1.
     *   </p><p>
     *   Otherwise, examine each plural variant defined for the given log10 value.
     *   If it has no prefix and suffix for a particular variant, use the one from the
     *   "other" variant.
     *   </p>
     *
     * @param result this instance is fixed in-place.
     */
    private static void fillInMissing(Data result) {
        // Initially we assume that previous divisor is 1 with no prefix or suffix.
        long lastDivisor = 1L;
        for (int i = 0; i < result.divisors.length; i++) {
            if (result.units.get(OTHER)[i] == null) {
                result.divisors[i] = lastDivisor;
                copyFromPreviousIndex(i, result.units);
            } else {
                lastDivisor = result.divisors[i];
                propagateOtherToMissing(i, result.units);
            }
        }
    }

    private static void propagateOtherToMissing(
            int idx, Map<String, DecimalFormat.Unit[]> units) {
        DecimalFormat.Unit otherVariantValue = units.get(OTHER)[idx];
        for (DecimalFormat.Unit[] byBase : units.values()) {
            if (byBase[idx] == null) {
                byBase[idx] = otherVariantValue;
            }
        }
    }

    private static void copyFromPreviousIndex(int idx, Map<String, DecimalFormat.Unit[]> units) {
        for (DecimalFormat.Unit[] byBase : units.values()) {
            if (idx == 0) {
                byBase[idx] = DecimalFormat.NULL_UNIT;
            } else {
                byBase[idx] = byBase[idx - 1];
            }
        }
    }

    private static void saveUnit(
            DecimalFormat.Unit unit, String pluralVariant, int idx,
            Map<String, DecimalFormat.Unit[]> units) {
        DecimalFormat.Unit[] byBase = units.get(pluralVariant);
        if (byBase == null) {
            byBase = new DecimalFormat.Unit[MAX_DIGITS];
            units.put(pluralVariant, byBase);
        }
        byBase[idx] = unit;

    }

    /**
     * Fetches a prefix or suffix given a plural variant and log10 value. If it
     * can't find the given variant, it falls back to "other".
     * @param prefixOrSuffix the prefix or suffix map
     * @param variant the plural variant
     * @param base log10 value. 0 <= base < MAX_DIGITS.
     * @return the prefix or suffix.
     */
    static DecimalFormat.Unit getUnit(
            Map<String, DecimalFormat.Unit[]> units, String variant, int base) {
        DecimalFormat.Unit[] byBase = units.get(variant);
        if (byBase == null) {
            byBase = units.get(CompactDecimalDataCache.OTHER);
        }
        return byBase[base];
    }
}
