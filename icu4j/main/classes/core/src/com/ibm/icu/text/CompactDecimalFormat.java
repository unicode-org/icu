/*
 *******************************************************************************
 * Copyright (C) 1996-2012, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.util.ULocale;

/**
 * The CompactDecimalFormat produces abbreviated numbers, suitable for display in environments will limited real estate.
 * For example, 'Hits: 1.2B' instead of 'Hits: 1,200,000,000'. The format will be appropriate for the given language,
 * such as "1,2 Mrd." for German.
 * <p>
 * For numbers under 1000 trillion (under 10^15, such as 123,456,789,012,345), the result will be short for supported
 * languages. However, the result may sometimes exceed 7 characters, such as when there are combining marks or thin
 * characters. In such cases, the visual width in fonts should still be short.
 * <p>
 * By default, there are 2 significant digits. After creation, if more than three significant digits are set (with
 * setMaximumSignificantDigits), or if a fixed number of digits are set (with setMaximumIntegerDigits or
 * setMaximumFractionDigits), then result may be wider.
 * <p>
 * At this time, negative numbers and parsing are not supported, and will produce an UnsupportedOperationException.
 * Resetting the pattern prefixes or suffixes is not supported; the method calls are ignored.
 * <p>
 * Note that important methods, like setting the number of decimals, will be moved up from DecimalFormat to
 * NumberFormat.
 * 
 * @author markdavis
 * @internal
 */
public class CompactDecimalFormat extends DecimalFormat {
    private static final long serialVersionUID = 4716293295276629682L;

    private static final int MINIMUM_ARRAY_LENGTH = 15;
    private static final int POSITIVE_PREFIX = 0, POSITIVE_SUFFIX = 1, AFFIX_SIZE = 2;

    private final String[] prefix;
    private final String[] suffix;
    private final long[] divisor;
    private final String[] currencyAffixes;

    /**
     * The normal mechanism is to use NumberFormat.getCompactDecimalInstance(). This is intended only for testing.
     * 
     * @param locale
     *            the desired locale
     * @param style
     *            the compact style
     * @internal
     */
    public CompactDecimalFormat(ULocale locale, CompactStyle style) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
        Data data;
        while (true) {
            data = localeToData.get(locale);
            if (data != null) {
                break;
            }
            locale = locale.equals(zhTW) ? ULocale.TRADITIONAL_CHINESE : locale.getFallback();
        }
        this.prefix = data.prefixes;
        this.suffix = data.suffixes;
        this.divisor = data.divisors;
        applyPattern(format.toPattern());
        setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);
        setGroupingUsed(false);

        DecimalFormat currencyFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        currencyAffixes = new String[AFFIX_SIZE];
        currencyAffixes[CompactDecimalFormat.POSITIVE_PREFIX] = currencyFormat.getPositivePrefix();
        currencyAffixes[CompactDecimalFormat.POSITIVE_SUFFIX] = currencyFormat.getPositiveSuffix();
        // TODO fix to get right symbol for the count
    }

    /**
     * Create a short number "from scratch". Intended for internal use. The prefix, suffix, and divisor arrays are
     * parallel, and provide the information for each power of 10. When formatting a value, the correct power of 10 is
     * found, then the value is divided by the divisor, and the prefix and suffix are set (using
     * setPositivePrefix/Suffix).
     * 
     * @param pattern
     *            A number format pattern. Note that the prefix and suffix are discarded, and the decimals are
     *            overridden by default.
     * @param formatSymbols
     *            Decimal format symbols, typically from a locale.
     * @param prefix
     *            An array of prefix values, one for each power of 10 from 0 to 14
     * @param suffix
     *            An array of prefix values, one for each power of 10 from 0 to 14
     * @param divisor
     *            An array of prefix values, one for each power of 10 from 0 to 14
     * @param debugCreationErrors
     *            A collection of strings for debugging. If null on input, then any errors found will be added to that
     *            collection instead of throwing exceptions.
     * @internal
     */
    public CompactDecimalFormat(String pattern, DecimalFormatSymbols formatSymbols, String[] prefix, String[] suffix,
            long[] divisor, Collection<String> debugCreationErrors, CompactStyle style, String[] currencyAffixes) {
        if (prefix.length < MINIMUM_ARRAY_LENGTH) {
            recordError(debugCreationErrors, "Must have at least " + MINIMUM_ARRAY_LENGTH + " prefix items.");
        }
        if (prefix.length != suffix.length || prefix.length != divisor.length) {
            recordError(debugCreationErrors, "Prefix, suffix, and divisor arrays must have the same length.");
        }
        long oldDivisor = 0;
        Map<String, Integer> seen = new HashMap<String, Integer>();
        for (int i = 0; i < prefix.length; ++i) {
            if (prefix[i] == null || suffix[i] == null) {
                recordError(debugCreationErrors, "Prefix or suffix is null for " + i);
            }

            // divisor must be a power of 10, and must be less than or equal to 10^i
            int log = (int) Math.log10(divisor[i]);
            if (log > i) {
                recordError(debugCreationErrors, "Divisor[" + i + "] must be less than or equal to 10^" + i
                        + ", but is: " + divisor[i]);
            }
            long roundTrip = (long) Math.pow(10.0d, log);
            if (roundTrip != divisor[i]) {
                recordError(debugCreationErrors, "Divisor[" + i + "] must be a power of 10, but is: " + divisor[i]);
            }

            // we can't have two different indexes with the same display
            String key = prefix[i] + "\uFFFF" + suffix[i] + "\uFFFF" + (i - log);
            Integer old = seen.get(key);
            if (old != null) {
                recordError(debugCreationErrors, "Collision between values for " + i + " and " + old
                        + " for [prefix/suffix/index-log(divisor)" + key.replace('\uFFFF', ';'));
            } else {
                seen.put(key, i);
            }
            if (divisor[i] < oldDivisor) {
                recordError(debugCreationErrors, "Bad divisor, the divisor for 10E" + i + "(" + divisor[i]
                        + ") is less than the divisor for the divisor for 10E" + (i - 1) + "(" + oldDivisor + ")");
            }
            oldDivisor = divisor[i];
        }

        this.prefix = prefix.clone();
        this.suffix = suffix.clone();
        this.divisor = divisor.clone();
        applyPattern(pattern);
        setDecimalFormatSymbols(formatSymbols);
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);
        setGroupingUsed(false);
        this.currencyAffixes = currencyAffixes.clone();
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number < 0.0d) {
            throw new UnsupportedOperationException("CompactDecimalFormat doesn't handle negative numbers yet.");
        }
        int integerCount = (int) Math.log10(number);
        int base = integerCount > 14 ? 14 : integerCount;
        number = number / divisor[base];
        setPositivePrefix(prefix[base]);
        setPositiveSuffix(suffix[base]);
        setCurrency(null);
        return super.format(number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((double) number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(com.ibm.icu.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    /**
     * Parsing is currently unsupported, and throws an UnsupportedOperationException.
     */
    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    /* INTERNALS */

    private static ULocale zhTW = new ULocale("zh_TW");

    private void recordError(Collection<String> creationErrors, String errorMessage) {
        if (creationErrors == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        creationErrors.add(errorMessage);
    }

    /** JUST FOR DEVELOPMENT */
    // For use with the hard-coded data
    static class Data {
        public Data(long[] divisors, String[] prefixes, String[] suffixes) {
            this.divisors = divisors;
            this.prefixes = prefixes;
            this.suffixes = suffixes;
        }

        long[] divisors;
        String[] prefixes;
        String[] suffixes;
    }

    static Map<ULocale, Data> localeToData = new HashMap<ULocale, Data>();

    static void add(String locale, long[] ls, String[] prefixes, String[] suffixes) {
        localeToData.put(new ULocale(locale), new Data(ls, prefixes, suffixes));
    }

    static {
        CompactDecimalFormatData.load();
    }

}
