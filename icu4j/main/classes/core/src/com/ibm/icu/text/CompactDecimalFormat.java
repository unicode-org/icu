/*
 *******************************************************************************
 * Copyright (C) 1996-2012, Google, International Business Machines Corporation and
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.text.CompactDecimalDataCache.Data;
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
 * @draft ICU 49
 * @provisional This API might change or be removed in a future release.
 */
public class CompactDecimalFormat extends DecimalFormat {

    private static final long serialVersionUID = 4716293295276629682L;

    private static final int POSITIVE_PREFIX = 0, POSITIVE_SUFFIX = 1, AFFIX_SIZE = 2;
    private static final CompactDecimalDataCache cache = new CompactDecimalDataCache();

    private final Map<String, DecimalFormat.Unit[]> units;
    private final long[] divisor;
    private final String[] currencyAffixes;

    // null if created internally using explicit prefixes and suffixes.
    private final PluralRules pluralRules;

    /**
     * Style parameter for CompactDecimalFormat.
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public enum CompactStyle {
        /**
         * Short version, like "1.2T"
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        SHORT,
        /**
         * Longer version, like "1.2 trillion", if available. May return same result as SHORT if not.
         * @draft ICU 50
         * @provisional This API might change or be removed in a future release.
         */
        LONG
    }

    /**
     * Create a CompactDecimalFormat appropriate for a locale. The result may
     * be affected by the number system in the locale, such as ar-u-nu-latn.
     *
     * @param locale the desired locale
     * @param style the compact style
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public static CompactDecimalFormat getInstance(ULocale locale, CompactStyle style) {
        return new CompactDecimalFormat(locale, style);
    }

    /**
     * Create a CompactDecimalFormat appropriate for a locale. The result may
     * be affected by the number system in the locale, such as ar-u-nu-latn.
     *
     * @param locale the desired locale
     * @param style the compact style
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    public static CompactDecimalFormat getInstance(Locale locale, CompactStyle style) {
        return new CompactDecimalFormat(ULocale.forLocale(locale), style);
    }

    /**
     * The public mechanism is CompactDecimalFormat.getInstance().
     *
     * @param locale
     *            the desired locale
     * @param style
     *            the compact style
     */
    CompactDecimalFormat(ULocale locale, CompactStyle style) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
        CompactDecimalDataCache.Data data = getData(locale, style);
        this.units = data.units;
        this.divisor = data.divisors;
        applyPattern(format.toPattern());
        setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        setMaximumSignificantDigits(3); // default significant digits
        setSignificantDigitsUsed(true);
        setGroupingUsed(false);
        this.pluralRules = PluralRules.forLocale(locale);

        DecimalFormat currencyFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        currencyAffixes = new String[AFFIX_SIZE];
        currencyAffixes[CompactDecimalFormat.POSITIVE_PREFIX] = currencyFormat.getPositivePrefix();
        currencyAffixes[CompactDecimalFormat.POSITIVE_SUFFIX] = currencyFormat.getPositiveSuffix();
        setCurrency(null);
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
        if (prefix.length < CompactDecimalDataCache.MAX_DIGITS) {
            recordError(debugCreationErrors, "Must have at least " + CompactDecimalDataCache.MAX_DIGITS + " prefix items.");
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

        this.units = otherPluralVariant(prefix, suffix);
        this.divisor = divisor.clone();
        applyPattern(pattern);
        setDecimalFormatSymbols(formatSymbols);
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);
        setGroupingUsed(false);
        this.currencyAffixes = currencyAffixes.clone();
        this.pluralRules = null;
        setCurrency(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false; // super does class check
        CompactDecimalFormat other = (CompactDecimalFormat) obj;
        return mapsAreEqual(units, other.units)
                && Arrays.equals(divisor, other.divisor)
                && Arrays.equals(currencyAffixes, other.currencyAffixes)
                && pluralRules.equals(other.pluralRules);
    }

    private boolean mapsAreEqual(
        Map<String, DecimalFormat.Unit[]> lhs, Map<String, DecimalFormat.Unit[]> rhs) {
        if (lhs.size() != rhs.size()) {
            return false;
        }
        // For each MapEntry in lhs, see if there is a matching one in rhs.
        for (Map.Entry<String, DecimalFormat.Unit[]> entry : lhs.entrySet()) {
            DecimalFormat.Unit[] value = rhs.get(entry.getKey());
            if (value == null || !Arrays.equals(entry.getValue(), value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        Amount amount = toAmount(number);
        Unit unit = amount.getUnit();
        unit.writePrefix(toAppendTo);
        super.format(amount.getQty(), toAppendTo, pos);
        unit.writeSuffix(toAppendTo);
        return toAppendTo;
    }

    /**
     * {@inheritDoc}
     * @draft ICU 50
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException();
        }
        Number number = (Number) obj;
        Amount amount = toAmount(number.doubleValue());
        return super.formatToCharacterIterator(amount.getQty(), amount.getUnit());
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((double) number, toAppendTo, pos);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuffer format(com.ibm.icu.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    /**
     * Parsing is currently unsupported, and throws an UnsupportedOperationException.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    // DISALLOW Serialization, at least while draft

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        throw new NotSerializableException();
    }

    /* INTERNALS */


    private Amount toAmount(double number) {
        // We do this here so that the prefix or suffix we choose is always consistent
        // with the rounding we do. This way, 999999 -> 1M instead of 1000K.
        boolean negative = isNumberNegative(number);
        number = adjustNumberAsInFormatting(number);
        int base = number <= 1.0d ? 0 : (int) Math.log10(number);
        if (base >= CompactDecimalDataCache.MAX_DIGITS) {
            base = CompactDecimalDataCache.MAX_DIGITS - 1;
        }
        number /= divisor[base];
        String pluralVariant = getPluralForm(number);
        if (negative) {
            number = -number;
        }
        return new Amount(
                number,
                CompactDecimalDataCache.getUnit(units, pluralVariant, base));

    }

    private void recordError(Collection<String> creationErrors, String errorMessage) {
        if (creationErrors == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        creationErrors.add(errorMessage);
    }

    private Map<String, DecimalFormat.Unit[]> otherPluralVariant(String[] prefix, String[] suffix) {
        Map<String, DecimalFormat.Unit[]> result = new HashMap<String, DecimalFormat.Unit[]>();
        DecimalFormat.Unit[] units = new DecimalFormat.Unit[prefix.length];
        for (int i = 0; i < units.length; i++) {
            units[i] = new DecimalFormat.Unit(prefix[i], suffix[i]);
        }
        result.put(CompactDecimalDataCache.OTHER, units);
        return result;
    }

    private String getPluralForm(double number) {
        if (pluralRules == null) {
            return CompactDecimalDataCache.OTHER;
        }
        return pluralRules.select(number);
    }

    /**
     * Gets the data for a particular locale and style. If style is unrecognized,
     * we just return data for CompactStyle.SHORT.
     * @param locale The locale.
     * @param style The style.
     * @return The data which must not be modified.
     */
    private Data getData(ULocale locale, CompactStyle style) {
        CompactDecimalDataCache.DataBundle bundle = cache.get(locale);
        switch (style) {
        case SHORT:
            return bundle.shortData;
        case LONG:
            return bundle.longData;
        default:
            return bundle.shortData;
        }
    }

    private static class Amount {
        private final double qty;
        private final Unit unit;

        public Amount(double qty, Unit unit) {
            this.qty = qty;
            this.unit = unit;
        }

        public double getQty() {
            return qty;
        }

        public Unit getUnit() {
            return unit;
        }
    }
}
