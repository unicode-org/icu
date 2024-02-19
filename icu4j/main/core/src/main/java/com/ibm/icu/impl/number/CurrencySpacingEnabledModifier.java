// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;

/** Identical to {@link ConstantMultiFieldModifier}, but supports currency spacing. */
public class CurrencySpacingEnabledModifier extends ConstantMultiFieldModifier {

    // These are the default currency spacing UnicodeSets in CLDR.
    // Pre-compute them for performance.
    // The unit test testCurrencySpacingPatternStability() will start failing if these change in CLDR.
    private static final UnicodeSet UNISET_DIGIT = new UnicodeSet("[:digit:]").freeze();
    private static final UnicodeSet UNISET_NOTSZ = new UnicodeSet("[[:^S:]&[:^Z:]]").freeze();

    // Constants for better readability. Types are for compiler checking.
    static final byte PREFIX = 0;
    static final byte SUFFIX = 1;
    static final short IN_CURRENCY = 0;
    static final short IN_NUMBER = 1;

    private final UnicodeSet afterPrefixUnicodeSet;
    private final String afterPrefixInsert;
    private final UnicodeSet beforeSuffixUnicodeSet;
    private final String beforeSuffixInsert;

    /** Safe code path */
    public CurrencySpacingEnabledModifier(
            FormattedStringBuilder prefix,
            FormattedStringBuilder suffix,
            boolean overwrite,
            boolean strong,
            DecimalFormatSymbols symbols) {
        super(prefix, suffix, overwrite, strong);

        // Check for currency spacing. Do not build the UnicodeSets unless there is
        // a currency code point at a boundary.
        if (prefix.length() > 0 && prefix.fieldAt(prefix.length() - 1) == NumberFormat.Field.CURRENCY) {
            int prefixCp = prefix.getLastCodePoint();
            UnicodeSet prefixUnicodeSet = getUnicodeSet(symbols, IN_CURRENCY, PREFIX);
            if (prefixUnicodeSet.contains(prefixCp)) {
                afterPrefixUnicodeSet = getUnicodeSet(symbols, IN_NUMBER, PREFIX);
                afterPrefixUnicodeSet.freeze(); // no-op if set is already frozen
                afterPrefixInsert = getInsertString(symbols, PREFIX);
            } else {
                afterPrefixUnicodeSet = null;
                afterPrefixInsert = null;
            }
        } else {
            afterPrefixUnicodeSet = null;
            afterPrefixInsert = null;
        }
        if (suffix.length() > 0 && suffix.fieldAt(0) == NumberFormat.Field.CURRENCY) {
            int suffixCp = suffix.getFirstCodePoint();
            UnicodeSet suffixUnicodeSet = getUnicodeSet(symbols, IN_CURRENCY, SUFFIX);
            if (suffixUnicodeSet.contains(suffixCp)) {
                beforeSuffixUnicodeSet = getUnicodeSet(symbols, IN_NUMBER, SUFFIX);
                beforeSuffixUnicodeSet.freeze(); // no-op if set is already frozen
                beforeSuffixInsert = getInsertString(symbols, SUFFIX);
            } else {
                beforeSuffixUnicodeSet = null;
                beforeSuffixInsert = null;
            }
        } else {
            beforeSuffixUnicodeSet = null;
            beforeSuffixInsert = null;
        }
    }

    /** Safe code path */
    @Override
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex) {
        // Currency spacing logic
        int length = 0;
        if (rightIndex - leftIndex > 0
                && afterPrefixUnicodeSet != null
                && afterPrefixUnicodeSet.contains(output.codePointAt(leftIndex))) {
            // TODO: Should we use the CURRENCY field here?
            length += output.insert(leftIndex, afterPrefixInsert, null);
        }
        if (rightIndex - leftIndex > 0
                && beforeSuffixUnicodeSet != null
                && beforeSuffixUnicodeSet.contains(output.codePointBefore(rightIndex))) {
            // TODO: Should we use the CURRENCY field here?
            length += output.insert(rightIndex + length, beforeSuffixInsert, null);
        }

        // Call super for the remaining logic
        length += super.apply(output, leftIndex, rightIndex + length);
        return length;
    }

    /** Unsafe code path */
    public static int applyCurrencySpacing(
            FormattedStringBuilder output,
            int prefixStart,
            int prefixLen,
            int suffixStart,
            int suffixLen,
            DecimalFormatSymbols symbols) {
        int length = 0;
        boolean hasPrefix = (prefixLen > 0);
        boolean hasSuffix = (suffixLen > 0);
        boolean hasNumber = (suffixStart - prefixStart - prefixLen > 0); // could be empty string
        if (hasPrefix && hasNumber) {
            length += applyCurrencySpacingAffix(output, prefixStart + prefixLen, PREFIX, symbols);
        }
        if (hasSuffix && hasNumber) {
            length += applyCurrencySpacingAffix(output, suffixStart + length, SUFFIX, symbols);
        }
        return length;
    }

    /** Unsafe code path */
    private static int applyCurrencySpacingAffix(
            FormattedStringBuilder output,
            int index,
            byte affix,
            DecimalFormatSymbols symbols) {
        // NOTE: For prefix, output.fieldAt(index-1) gets the last field type in the prefix.
        // This works even if the last code point in the prefix is 2 code units because the
        // field value gets populated to both indices in the field array.
        Object affixField = (affix == PREFIX) ? output.fieldAt(index - 1)
                : output.fieldAt(index);
        if (affixField != NumberFormat.Field.CURRENCY) {
            return 0;
        }
        int affixCp = (affix == PREFIX) ? output.codePointBefore(index) : output.codePointAt(index);
        UnicodeSet affixUniset = getUnicodeSet(symbols, IN_CURRENCY, affix);
        if (!affixUniset.contains(affixCp)) {
            return 0;
        }
        int numberCp = (affix == PREFIX) ? output.codePointAt(index) : output.codePointBefore(index);
        UnicodeSet numberUniset = getUnicodeSet(symbols, IN_NUMBER, affix);
        if (!numberUniset.contains(numberCp)) {
            return 0;
        }
        String spacingString = getInsertString(symbols, affix);

        // NOTE: This next line *inserts* the spacing string, triggering an arraycopy.
        // It would be more efficient if this could be done before affixes were attached,
        // so that it could be prepended/appended instead of inserted.
        // However, the build code path is more efficient, and this is the most natural
        // place to put currency spacing in the non-build code path.
        // TODO: Should we use the CURRENCY field here?
        return output.insert(index, spacingString, null);
    }

    private static UnicodeSet getUnicodeSet(DecimalFormatSymbols symbols, short position, byte affix) {
        String pattern = symbols
                .getPatternForCurrencySpacing(
                        position == IN_CURRENCY ? DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH
                                : DecimalFormatSymbols.CURRENCY_SPC_SURROUNDING_MATCH,
                        affix == SUFFIX);
        if (pattern.equals("[:digit:]")) {
            return UNISET_DIGIT;
        } else if (pattern.equals("[[:^S:]&[:^Z:]]")) {
            return UNISET_NOTSZ;
        } else {
            return new UnicodeSet(pattern);
        }
    }

    private static String getInsertString(DecimalFormatSymbols symbols, byte affix) {
        return symbols.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT,
                affix == SUFFIX);
    }
}
