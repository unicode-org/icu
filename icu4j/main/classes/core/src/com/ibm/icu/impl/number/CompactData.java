// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Datatype for compact notation data. Includes logic for data loading.
 */
public class CompactData implements MultiplierProducer {

    public enum CompactType {
        DECIMAL, CURRENCY
    }

    // A dummy object used when a "0" compact decimal entry is encountered. This is necessary
    // in order to prevent falling back to root. Object equality ("==") is intended.
    private static final String USE_FALLBACK = "<USE FALLBACK>";

    private final String[] patterns;
    private final byte[] multipliers;
    private byte largestMagnitude;
    private boolean isEmpty;

    private static final int COMPACT_MAX_DIGITS = 20;

    public CompactData() {
        patterns = new String[(CompactData.COMPACT_MAX_DIGITS + 1) * StandardPlural.COUNT];
        multipliers = new byte[CompactData.COMPACT_MAX_DIGITS + 1];
        largestMagnitude = 0;
        isEmpty = true;
    }

    public void populate(
            ULocale locale,
            String nsName,
            CompactStyle compactStyle,
            CompactType compactType) {
        assert isEmpty;
        CompactDataSink sink = new CompactDataSink(this);
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle
                .getBundleInstance(ICUData.ICU_BASE_NAME, locale);

        boolean nsIsLatn = nsName.equals("latn");
        boolean compactIsShort = compactStyle == CompactStyle.SHORT;

        // Fall back to latn numbering system and/or short compact style.
        StringBuilder resourceKey = new StringBuilder();
        getResourceBundleKey(nsName, compactStyle, compactType, resourceKey);
        rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        if (isEmpty && !nsIsLatn) {
            getResourceBundleKey("latn", compactStyle, compactType, resourceKey);
            rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        }
        if (isEmpty && !compactIsShort) {
            getResourceBundleKey(nsName, CompactStyle.SHORT, compactType, resourceKey);
            rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        }
        if (isEmpty && !nsIsLatn && !compactIsShort) {
            getResourceBundleKey("latn", CompactStyle.SHORT, compactType, resourceKey);
            rb.getAllItemsWithFallbackNoFail(resourceKey.toString(), sink);
        }

        // The last fallback should be guaranteed to return data.
        if (isEmpty) {
            throw new ICUException("Could not load compact decimal data for locale " + locale);
        }
    }

    /** Produces a string like "NumberElements/latn/patternsShort/decimalFormat". */
    private static void getResourceBundleKey(
            String nsName,
            CompactStyle compactStyle,
            CompactType compactType,
            StringBuilder sb) {
        sb.setLength(0);
        sb.append("NumberElements/");
        sb.append(nsName);
        sb.append(compactStyle == CompactStyle.SHORT ? "/patternsShort" : "/patternsLong");
        sb.append(compactType == CompactType.DECIMAL ? "/decimalFormat" : "/currencyFormat");
    }

    /** Java-only method used by CLDR tooling. */
    public void populate(Map<String, Map<String, String>> powersToPluralsToPatterns) {
        assert isEmpty;
        for (Map.Entry<String, Map<String, String>> magnitudeEntry : powersToPluralsToPatterns
                .entrySet()) {
            byte magnitude = (byte) (magnitudeEntry.getKey().length() - 1);
            for (Map.Entry<String, String> pluralEntry : magnitudeEntry.getValue().entrySet()) {
                String pluralString = pluralEntry.getKey().toString();
                StandardPlural plural = StandardPlural.fromString(pluralString);
                String patternString = pluralEntry.getValue().toString();
                patterns[getIndex(magnitude, plural)] = patternString;
                int numZeros = countZeros(patternString);
                if (numZeros > 0) { // numZeros==0 in certain cases, like Somali "Kun"
                    // Save the multiplier.
                    multipliers[magnitude] = (byte) (numZeros - magnitude - 1);
                    if (magnitude > largestMagnitude) {
                        largestMagnitude = magnitude;
                    }
                    isEmpty = false;
                }
            }
        }
    }

    @Override
    public int getMultiplier(int magnitude) {
        if (magnitude < 0) {
            return 0;
        }
        if (magnitude > largestMagnitude) {
            magnitude = largestMagnitude;
        }
        return multipliers[magnitude];
    }

    public String getPattern(int magnitude, PluralRules rules, DecimalQuantity dq) {
        if (magnitude < 0) {
            return null;
        }
        if (magnitude > largestMagnitude) {
            magnitude = largestMagnitude;
        }
        String patternString = null;
        if (dq.isHasIntegerValue()) {
            long i = dq.toLong(true);
            if (i == 0) {
                patternString = patterns[getIndex(magnitude, StandardPlural.EQ_0)];
            } else if (i == 1) {
                patternString = patterns[getIndex(magnitude, StandardPlural.EQ_1)];
            }
            if (patternString != null) {
                return patternString;
            }
        }
        StandardPlural plural = dq.getStandardPlural(rules);
        patternString = patterns[getIndex(magnitude, plural)];
        if (patternString == null && plural != StandardPlural.OTHER) {
            // Fall back to "other" plural variant
            patternString = patterns[getIndex(magnitude, StandardPlural.OTHER)];
        }
        if (patternString == USE_FALLBACK) { // == is intended
            // Return null if USE_FALLBACK is present
            patternString = null;
        }
        return patternString;
    }

    public void getUniquePatterns(Set<String> output) {
        assert output.isEmpty();
        // NOTE: In C++, this is done more manually with a UVector.
        // In Java, we can take advantage of JDK HashSet.
        output.addAll(Arrays.asList(patterns));
        output.remove(USE_FALLBACK);
        output.remove(null);
    }

    private static final class CompactDataSink extends UResource.Sink {

        CompactData data;

        public CompactDataSink(CompactData data) {
            this.data = data;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean isRoot) {
            // traverse into the table of powers of ten
            UResource.Table powersOfTenTable = value.getTable();
            for (int i3 = 0; powersOfTenTable.getKeyAndValue(i3, key, value); ++i3) {

                // Assumes that the keys are always of the form "10000" where the magnitude is the
                // length of the key minus one. We expect magnitudes to be less than MAX_DIGITS.
                byte magnitude = (byte) (key.length() - 1);
                if (magnitude >= COMPACT_MAX_DIGITS) {
                    continue;
                }
                byte multiplier = data.multipliers[magnitude];

                // Iterate over the plural variants ("one", "other", etc)
                UResource.Table pluralVariantsTable = value.getTable();
                for (int i4 = 0; pluralVariantsTable.getKeyAndValue(i4, key, value); ++i4) {
                    // Skip this magnitude/plural if we already have it from a child locale.
                    // Note: This also skips USE_FALLBACK entries.
                    StandardPlural plural = StandardPlural.fromString(key.toString());
                    if (data.patterns[getIndex(magnitude, plural)] != null) {
                        continue;
                    }

                    // The value "0" means that we need to use the default pattern and not fall back
                    // to parent locales. Example locale where this is relevant: 'it'.
                    String patternString = value.toString();
                    if (patternString.equals("0")) {
                        patternString = USE_FALLBACK;
                    }

                    // Save the pattern string. We will parse it lazily.
                    data.patterns[getIndex(magnitude, plural)] = patternString;

                    // If necessary, compute the multiplier: the difference between the magnitude
                    // and the number of zeros in the pattern.
                    if (multiplier == 0) {
                        int numZeros = countZeros(patternString);
                        if (numZeros > 0) { // numZeros==0 in certain cases, like Somali "Kun"
                            multiplier = (byte) (numZeros - magnitude - 1);
                        }
                    }
                }

                // Save the multiplier.
                if (data.multipliers[magnitude] == 0) {
                    data.multipliers[magnitude] = multiplier;
                    if (magnitude > data.largestMagnitude) {
                        data.largestMagnitude = magnitude;
                    }
                    data.isEmpty = false;
                } else {
                    assert data.multipliers[magnitude] == multiplier;
                }
            }
        }
    }

    private static final int getIndex(int magnitude, StandardPlural plural) {
        return magnitude * StandardPlural.COUNT + plural.ordinal();
    }

    private static final int countZeros(String patternString) {
        // NOTE: This strategy for computing the number of zeros is a hack for efficiency.
        // It could break if there are any 0s that aren't part of the main pattern.
        int numZeros = 0;
        for (int i = 0; i < patternString.length(); i++) {
            if (patternString.charAt(i) == '0') {
                numZeros++;
            } else if (numZeros > 0) {
                break; // zeros should always be contiguous
            }
        }
        return numZeros;
    }
}
