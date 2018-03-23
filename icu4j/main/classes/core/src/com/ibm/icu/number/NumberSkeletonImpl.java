// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.impl.CacheBase;
import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.SoftCache;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.CharsTrie;
import com.ibm.icu.util.CharsTrieBuilder;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.StringTrieBuilder;

/**
 * @author sffc
 *
 */
class NumberSkeletonImpl {

    static enum StemType {
        OTHER,
        COMPACT_NOTATION,
        SCIENTIFIC_NOTATION,
        SIMPLE_NOTATION,
        NO_UNIT,
        CURRENCY,
        MEASURE_UNIT,
        PER_MEASURE_UNIT,
        ROUNDER,
        FRACTION_ROUNDER,
        MAYBE_INCREMENT_ROUNDER,
        CURRENCY_ROUNDER,
        GROUPING,
        INTEGER_WIDTH,
        LATIN,
        NUMBERING_SYSTEM,
        UNIT_WIDTH,
        SIGN_DISPLAY,
        DECIMAL_DISPLAY
    }

    static enum ActualStem {
        // Section 1: Stems that do not require an option:
        STEM_COMPACT_SHORT,
        STEM_COMPACT_LONG,
        STEM_SCIENTIFIC,
        STEM_ENGINEERING,
        STEM_NOTATION_SIMPLE,
        STEM_BASE_UNIT,
        STEM_PERCENT,
        STEM_PERMILLE,
        STEM_ROUND_INTEGER,
        STEM_ROUND_UNLIMITED,
        STEM_ROUND_CURRENCY_STANDARD,
        STEM_ROUND_CURRENCY_CASH,
        STEM_GROUP_OFF,
        STEM_GROUP_MIN2,
        STEM_GROUP_AUTO,
        STEM_GROUP_ON_ALIGNED,
        STEM_GROUP_THOUSANDS,
        STEM_LATIN,
        STEM_UNIT_WIDTH_NARROW,
        STEM_UNIT_WIDTH_SHORT,
        STEM_UNIT_WIDTH_FULL_NAME,
        STEM_UNIT_WIDTH_ISO_CODE,
        STEM_UNIT_WIDTH_HIDDEN,
        STEM_SIGN_AUTO,
        STEM_SIGN_ALWAYS,
        STEM_SIGN_NEVER,
        STEM_SIGN_ACCOUNTING,
        STEM_SIGN_ACCOUNTING_ALWAYS,
        STEM_SIGN_EXCEPT_ZERO,
        STEM_SIGN_ACCOUNTING_EXCEPT_ZERO,
        STEM_DECIMAL_AUTO,
        STEM_DECIMAL_ALWAYS,

        // Section 2: Stems that DO require an option:
        STEM_ROUND_INCREMENT,
        STEM_MEASURE_UNIT,
        STEM_PER_MEASURE_UNIT,
        STEM_CURRENCY,
        STEM_INTEGER_WIDTH,
        STEM_NUMBERING_SYSTEM,
    };

    static final ActualStem[] ACTUAL_STEM_VALUES = ActualStem.values();

    private static Notation stemToNotation(ActualStem stem) {
        switch (stem) {
        case STEM_COMPACT_SHORT:
            return Notation.compactShort();
        case STEM_COMPACT_LONG:
            return Notation.compactLong();
        case STEM_SCIENTIFIC:
            return Notation.scientific();
        case STEM_ENGINEERING:
            return Notation.engineering();
        case STEM_NOTATION_SIMPLE:
            return Notation.simple();
        default:
            return null;
        }
    }

    private static MeasureUnit stemToUnit(ActualStem stem) {
        switch (stem) {
        case STEM_BASE_UNIT:
            return NoUnit.BASE;
        case STEM_PERCENT:
            return NoUnit.PERCENT;
        case STEM_PERMILLE:
            return NoUnit.PERMILLE;
        default:
            return null;
        }
    }

    private static Rounder stemToRounder(ActualStem stem) {
        switch (stem) {
        case STEM_ROUND_INTEGER:
            return Rounder.integer();
        case STEM_ROUND_UNLIMITED:
            return Rounder.unlimited();
        case STEM_ROUND_CURRENCY_STANDARD:
            return Rounder.currency(CurrencyUsage.STANDARD);
        case STEM_ROUND_CURRENCY_CASH:
            return Rounder.currency(CurrencyUsage.CASH);
        default:
            return null;
        }
    }

    private static GroupingStrategy stemToGrouper(ActualStem stem) {
        switch (stem) {
        case STEM_GROUP_OFF:
            return GroupingStrategy.OFF;
        case STEM_GROUP_MIN2:
            return GroupingStrategy.MIN2;
        case STEM_GROUP_AUTO:
            return GroupingStrategy.AUTO;
        case STEM_GROUP_ON_ALIGNED:
            return GroupingStrategy.ON_ALIGNED;
        case STEM_GROUP_THOUSANDS:
            return GroupingStrategy.THOUSANDS;
        default:
            return null;
        }
    }

    private static UnitWidth stemToUnitWidth(ActualStem stem) {
        switch (stem) {
        case STEM_UNIT_WIDTH_NARROW:
            return UnitWidth.NARROW;
        case STEM_UNIT_WIDTH_SHORT:
            return UnitWidth.SHORT;
        case STEM_UNIT_WIDTH_FULL_NAME:
            return UnitWidth.FULL_NAME;
        case STEM_UNIT_WIDTH_ISO_CODE:
            return UnitWidth.ISO_CODE;
        case STEM_UNIT_WIDTH_HIDDEN:
            return UnitWidth.HIDDEN;
        default:
            return null;
        }
    }

    private static SignDisplay stemToSignDisplay(ActualStem stem) {
        switch (stem) {
        case STEM_SIGN_AUTO:
            return SignDisplay.AUTO;
        case STEM_SIGN_ALWAYS:
            return SignDisplay.ALWAYS;
        case STEM_SIGN_NEVER:
            return SignDisplay.NEVER;
        case STEM_SIGN_ACCOUNTING:
            return SignDisplay.ACCOUNTING;
        case STEM_SIGN_ACCOUNTING_ALWAYS:
            return SignDisplay.ACCOUNTING_ALWAYS;
        case STEM_SIGN_EXCEPT_ZERO:
            return SignDisplay.EXCEPT_ZERO;
        case STEM_SIGN_ACCOUNTING_EXCEPT_ZERO:
            return SignDisplay.ACCOUNTING_EXCEPT_ZERO;
        default:
            return null;
        }
    }

    private static DecimalSeparatorDisplay stemToDecimalSeparatorDisplay(ActualStem stem) {
        switch (stem) {
        case STEM_DECIMAL_AUTO:
            return DecimalSeparatorDisplay.AUTO;
        case STEM_DECIMAL_ALWAYS:
            return DecimalSeparatorDisplay.ALWAYS;
        default:
            return null;
        }
    }

    static final String SERIALIZED_STEM_TRIE = buildStemTrie();

    static String buildStemTrie() {
        CharsTrieBuilder b = new CharsTrieBuilder();

        // Section 1:
        b.add("compact-short", ActualStem.STEM_COMPACT_SHORT.ordinal());
        b.add("compact-long", ActualStem.STEM_COMPACT_LONG.ordinal());
        b.add("scientific", ActualStem.STEM_SCIENTIFIC.ordinal());
        b.add("engineering", ActualStem.STEM_ENGINEERING.ordinal());
        b.add("notation-simple", ActualStem.STEM_NOTATION_SIMPLE.ordinal());
        b.add("base-unit", ActualStem.STEM_BASE_UNIT.ordinal());
        b.add("percent", ActualStem.STEM_PERCENT.ordinal());
        b.add("permille", ActualStem.STEM_PERMILLE.ordinal());
        b.add("round-integer", ActualStem.STEM_ROUND_INTEGER.ordinal());
        b.add("round-unlimited", ActualStem.STEM_ROUND_UNLIMITED.ordinal());
        b.add("round-currency-standard", ActualStem.STEM_ROUND_CURRENCY_STANDARD.ordinal());
        b.add("round-currency-cash", ActualStem.STEM_ROUND_CURRENCY_CASH.ordinal());
        b.add("group-off", ActualStem.STEM_GROUP_OFF.ordinal());
        b.add("group-min2", ActualStem.STEM_GROUP_MIN2.ordinal());
        b.add("group-auto", ActualStem.STEM_GROUP_AUTO.ordinal());
        b.add("group-on-aligned", ActualStem.STEM_GROUP_ON_ALIGNED.ordinal());
        b.add("group-thousands", ActualStem.STEM_GROUP_THOUSANDS.ordinal());
        b.add("latin", ActualStem.STEM_LATIN.ordinal());
        b.add("unit-width-narrow", ActualStem.STEM_UNIT_WIDTH_NARROW.ordinal());
        b.add("unit-width-short", ActualStem.STEM_UNIT_WIDTH_SHORT.ordinal());
        b.add("unit-width-full-name", ActualStem.STEM_UNIT_WIDTH_FULL_NAME.ordinal());
        b.add("unit-width-iso-code", ActualStem.STEM_UNIT_WIDTH_ISO_CODE.ordinal());
        b.add("unit-width-hidden", ActualStem.STEM_UNIT_WIDTH_HIDDEN.ordinal());
        b.add("sign-auto", ActualStem.STEM_SIGN_AUTO.ordinal());
        b.add("sign-always", ActualStem.STEM_SIGN_ALWAYS.ordinal());
        b.add("sign-never", ActualStem.STEM_SIGN_NEVER.ordinal());
        b.add("sign-accounting", ActualStem.STEM_SIGN_ACCOUNTING.ordinal());
        b.add("sign-accounting-always", ActualStem.STEM_SIGN_ACCOUNTING_ALWAYS.ordinal());
        b.add("sign-except-zero", ActualStem.STEM_SIGN_EXCEPT_ZERO.ordinal());
        b.add("sign-accounting-except-zero", ActualStem.STEM_SIGN_ACCOUNTING_EXCEPT_ZERO.ordinal());
        b.add("decimal-auto", ActualStem.STEM_DECIMAL_AUTO.ordinal());
        b.add("decimal-always", ActualStem.STEM_DECIMAL_ALWAYS.ordinal());

        // Section 2:
        b.add("round-increment", ActualStem.STEM_ROUND_INCREMENT.ordinal());
        b.add("measure-unit", ActualStem.STEM_MEASURE_UNIT.ordinal());
        b.add("per-measure-unit", ActualStem.STEM_PER_MEASURE_UNIT.ordinal());
        b.add("currency", ActualStem.STEM_CURRENCY.ordinal());
        b.add("integer-width", ActualStem.STEM_INTEGER_WIDTH.ordinal());
        b.add("numbering-system", ActualStem.STEM_NUMBERING_SYSTEM.ordinal());

        // TODO: Use SLOW or FAST here?
        return b.buildCharSequence(StringTrieBuilder.Option.FAST).toString();
    }

    static class SkeletonDataStructure {
        final Map<Object, String> valuesToStems;

        SkeletonDataStructure() {
            valuesToStems = new HashMap<Object, String>();
        }

        public void put(StemType stemType, String content, Object value) {
            valuesToStems.put(value, content);
        }

        public String valueToStem(Object value) {
            return valuesToStems.get(value);
        }
    }

    static final SkeletonDataStructure skeletonData = new SkeletonDataStructure();

    static {
        SkeletonDataStructure d = skeletonData; // abbreviate for shorter lines
        d.put(StemType.COMPACT_NOTATION, "compact-short", Notation.compactShort());
        d.put(StemType.COMPACT_NOTATION, "compact-long", Notation.compactLong());
        d.put(StemType.SCIENTIFIC_NOTATION, "scientific", Notation.scientific());
        d.put(StemType.SCIENTIFIC_NOTATION, "engineering", Notation.engineering());
        d.put(StemType.SIMPLE_NOTATION, "notation-simple", Notation.simple());

        d.put(StemType.NO_UNIT, "base-unit", NoUnit.BASE);
        d.put(StemType.NO_UNIT, "percent", NoUnit.PERCENT);
        d.put(StemType.NO_UNIT, "permille", NoUnit.PERMILLE);

        d.put(StemType.ROUNDER, "round-integer", Rounder.integer());
        d.put(StemType.ROUNDER, "round-unlimited", Rounder.unlimited());
        d.put(StemType.ROUNDER, "round-currency-standard", Rounder.currency(CurrencyUsage.STANDARD));
        d.put(StemType.ROUNDER, "round-currency-cash", Rounder.currency(CurrencyUsage.CASH));

        d.put(StemType.GROUPING, "group-off", GroupingStrategy.OFF);
        d.put(StemType.GROUPING, "group-min2", GroupingStrategy.MIN2);
        d.put(StemType.GROUPING, "group-auto", GroupingStrategy.AUTO);
        d.put(StemType.GROUPING, "group-on-aligned", GroupingStrategy.ON_ALIGNED);
        d.put(StemType.GROUPING, "group-thousands", GroupingStrategy.THOUSANDS);

        d.put(StemType.LATIN, "latin", NumberingSystem.LATIN);

        d.put(StemType.UNIT_WIDTH, "unit-width-narrow", UnitWidth.NARROW);
        d.put(StemType.UNIT_WIDTH, "unit-width-short", UnitWidth.SHORT);
        d.put(StemType.UNIT_WIDTH, "unit-width-full-name", UnitWidth.FULL_NAME);
        d.put(StemType.UNIT_WIDTH, "unit-width-iso-code", UnitWidth.ISO_CODE);
        d.put(StemType.UNIT_WIDTH, "unit-width-hidden", UnitWidth.HIDDEN);

        d.put(StemType.SIGN_DISPLAY, "sign-auto", SignDisplay.AUTO);
        d.put(StemType.SIGN_DISPLAY, "sign-always", SignDisplay.ALWAYS);
        d.put(StemType.SIGN_DISPLAY, "sign-never", SignDisplay.NEVER);
        d.put(StemType.SIGN_DISPLAY, "sign-accounting", SignDisplay.ACCOUNTING);
        d.put(StemType.SIGN_DISPLAY, "sign-accounting-always", SignDisplay.ACCOUNTING_ALWAYS);
        d.put(StemType.SIGN_DISPLAY, "sign-except-zero", SignDisplay.EXCEPT_ZERO);
        d.put(StemType.SIGN_DISPLAY, "sign-accounting-except-zero", SignDisplay.ACCOUNTING_EXCEPT_ZERO);

        d.put(StemType.DECIMAL_DISPLAY, "decimal-auto", DecimalSeparatorDisplay.AUTO);
        d.put(StemType.DECIMAL_DISPLAY, "decimal-always", DecimalSeparatorDisplay.ALWAYS);
    }

    static final String[] ROUNDING_MODE_STRINGS = {
            "up",
            "down",
            "ceiling",
            "floor",
            "half-up",
            "half-down",
            "half-even",
            "unnecessary" };

    private static final CacheBase<String, UnlocalizedNumberFormatter, Void> cache = new SoftCache<String, UnlocalizedNumberFormatter, Void>() {
        @Override
        protected UnlocalizedNumberFormatter createInstance(String skeletonString, Void unused) {
            return create(skeletonString);
        }
    };

    /**
     * Gets the number formatter for the given number skeleton string from the cache, creating it if it
     * does not exist in the cache.
     *
     * @param skeletonString
     *            A number skeleton string, possibly not in its shortest form.
     * @return An UnlocalizedNumberFormatter with behavior defined by the given skeleton string.
     */
    public static UnlocalizedNumberFormatter getOrCreate(String skeletonString) {
        // TODO: This does not currently check the cache for the normalized form of the skeleton.
        // A new cache implementation would be required for that to work.
        return cache.getInstance(skeletonString, null);
    }

    /**
     * Creates a NumberFormatter corresponding to the given skeleton string.
     *
     * @param skeletonString
     *            A number skeleton string, possibly not in its shortest form.
     * @return An UnlocalizedNumberFormatter with behavior defined by the given skeleton string.
     */
    public static UnlocalizedNumberFormatter create(String skeletonString) {
        MacroProps macros = parseSkeleton(skeletonString);
        return NumberFormatter.with().macros(macros);
    }

    /**
     * Create a skeleton string corresponding to the given NumberFormatter.
     *
     * @param macros
     *            The NumberFormatter options object.
     * @return A skeleton string in normalized form.
     */
    public static String generate(MacroProps macros) {
        StringBuilder sb = new StringBuilder();
        generateSkeleton(macros, sb);
        return sb.toString();
    }

    /////

    private static MacroProps parseSkeleton(String skeletonString) {
        // Add a trailing whitespace to the end of the skeleton string to make code cleaner.
        skeletonString += " ";

        MacroProps macros = new MacroProps();
        StringSegment segment = new StringSegment(skeletonString, false);
        CharsTrie stemTrie = new CharsTrie(SERIALIZED_STEM_TRIE, 0);
        StemType stem = null;
        int offset = 0;
        while (offset < segment.length()) {
            int cp = segment.codePointAt(offset);
            boolean isTokenSeparator = PatternProps.isWhiteSpace(cp);
            boolean isOptionSeparator = (cp == '/');

            if (!isTokenSeparator && !isOptionSeparator) {
                // Non-separator token; consume it.
                offset += Character.charCount(cp);
                if (stem == null) {
                    // We are currently consuming a stem.
                    // Go to the next state in the stem trie.
                    stemTrie.nextForCodePoint(cp);
                }
                continue;
            }

            // We are looking at a token or option separator.
            // If the segment is nonempty, parse it and reset the segment.
            // Otherwise, make sure it is a valid repeating separator.
            if (offset != 0) {
                segment.setLength(offset);
                if (stem == null) {
                    // The first separator after the start of a token. Parse it as a stem.
                    stem = parseStem2(segment, stemTrie, macros);
                    stemTrie.reset();
                } else {
                    // A separator after the first separator of a token. Parse it as an option.
                    stem = parseOption(stem, segment, macros);
                }
                segment.resetLength();
                segment.adjustOffset(offset + 1);
                offset = 0;

            } else if (stem != null) {
                // A separator ('/' or whitespace) following an option separator ('/')
                throw new SkeletonSyntaxException("Unexpected separator character", segment);

            } else {
                // Two spaces in a row; this is OK.
                segment.adjustOffset(Character.charCount(cp));
            }

            // Make sure we aren't in a state requiring an option, and then reset the state.
            if (isTokenSeparator && stem != null) {
                switch (stem) {
                case MAYBE_INCREMENT_ROUNDER:
                case MEASURE_UNIT:
                case PER_MEASURE_UNIT:
                case CURRENCY:
                case INTEGER_WIDTH:
                case NUMBERING_SYSTEM:
                    throw new SkeletonSyntaxException("Stem requires an option", segment);
                default:
                    break;
                }
                stem = null;
            }
        }
        assert stem == null;
        return macros;
    }

    private static StemType parseStem2(StringSegment segment, CharsTrie stemTrie, MacroProps macros) {
        // First check for "blueprint" stems, which start with a "signal char"
        switch (segment.charAt(0)) {
        case '.':
            checkNull(macros.rounder, segment);
            parseFractionStem(segment, macros);
            return StemType.FRACTION_ROUNDER;
        case '@':
            checkNull(macros.rounder, segment);
            parseDigitsStem(segment, macros);
            return StemType.OTHER;
        }

        // Now look at the stemsTrie, which is already be pointing at our stem.
        BytesTrie.Result stemResult = stemTrie.current();

        if (stemResult != BytesTrie.Result.INTERMEDIATE_VALUE
                && stemResult != BytesTrie.Result.FINAL_VALUE) {
            throw new SkeletonSyntaxException("Unknown stem", segment);
        }

        ActualStem stemEnum = ACTUAL_STEM_VALUES[stemTrie.getValue()];
        switch (stemEnum) {

        // Stems with meaning on their own, not requiring an option:

        case STEM_COMPACT_SHORT:
        case STEM_COMPACT_LONG:
        case STEM_SCIENTIFIC:
        case STEM_ENGINEERING:
        case STEM_NOTATION_SIMPLE:
            checkNull(macros.notation, segment);
            macros.notation = stemToNotation(stemEnum);
            switch (stemEnum) {
            case STEM_SCIENTIFIC:
            case STEM_ENGINEERING:
                return StemType.SCIENTIFIC_NOTATION; // allows for scientific options
            default:
                return StemType.OTHER;
            }

        case STEM_BASE_UNIT:
        case STEM_PERCENT:
        case STEM_PERMILLE:
            checkNull(macros.unit, segment);
            macros.unit = stemToUnit(stemEnum);
            return StemType.OTHER;

        case STEM_ROUND_INTEGER:
        case STEM_ROUND_UNLIMITED:
        case STEM_ROUND_CURRENCY_STANDARD:
        case STEM_ROUND_CURRENCY_CASH:
            checkNull(macros.rounder, segment);
            macros.rounder = stemToRounder(stemEnum);
            switch (stemEnum) {
            case STEM_ROUND_INTEGER:
                return StemType.FRACTION_ROUNDER; // allows for "round-integer/@##"
            default:
                return StemType.ROUNDER; // allows for rounding mode options
            }

        case STEM_GROUP_OFF:
        case STEM_GROUP_MIN2:
        case STEM_GROUP_AUTO:
        case STEM_GROUP_ON_ALIGNED:
        case STEM_GROUP_THOUSANDS:
            checkNull(macros.grouping, segment);
            macros.grouping = stemToGrouper(stemEnum);
            return StemType.OTHER;

        case STEM_LATIN:
            checkNull(macros.symbols, segment);
            macros.symbols = NumberingSystem.LATIN;
            return StemType.OTHER;

        case STEM_UNIT_WIDTH_NARROW:
        case STEM_UNIT_WIDTH_SHORT:
        case STEM_UNIT_WIDTH_FULL_NAME:
        case STEM_UNIT_WIDTH_ISO_CODE:
        case STEM_UNIT_WIDTH_HIDDEN:
            checkNull(macros.unitWidth, segment);
            macros.unitWidth = stemToUnitWidth(stemEnum);
            return StemType.OTHER;

        case STEM_SIGN_AUTO:
        case STEM_SIGN_ALWAYS:
        case STEM_SIGN_NEVER:
        case STEM_SIGN_ACCOUNTING:
        case STEM_SIGN_ACCOUNTING_ALWAYS:
        case STEM_SIGN_EXCEPT_ZERO:
        case STEM_SIGN_ACCOUNTING_EXCEPT_ZERO:
            checkNull(macros.sign, segment);
            macros.sign = stemToSignDisplay(stemEnum);
            return StemType.OTHER;

        case STEM_DECIMAL_AUTO:
        case STEM_DECIMAL_ALWAYS:
            checkNull(macros.decimal, segment);
            macros.decimal = stemToDecimalSeparatorDisplay(stemEnum);
            return StemType.OTHER;

        // Stems requiring an option:

        case STEM_ROUND_INCREMENT:
            checkNull(macros.rounder, segment);
            return StemType.MAYBE_INCREMENT_ROUNDER;

        case STEM_MEASURE_UNIT:
            checkNull(macros.unit, segment);
            return StemType.MEASURE_UNIT;

        case STEM_PER_MEASURE_UNIT:
            checkNull(macros.perUnit, segment);
            return StemType.PER_MEASURE_UNIT;

        case STEM_CURRENCY:
            checkNull(macros.unit, segment);
            return StemType.CURRENCY;

        case STEM_INTEGER_WIDTH:
            checkNull(macros.integerWidth, segment);
            return StemType.INTEGER_WIDTH;

        case STEM_NUMBERING_SYSTEM:
            checkNull(macros.symbols, segment);
            return StemType.NUMBERING_SYSTEM;

        default:
            throw new AssertionError();
        }
    }

    private static StemType parseOption(StemType stem, StringSegment segment, MacroProps macros) {

        ///// Required options: /////

        switch (stem) {
        case CURRENCY:
            parseCurrencyOption(segment, macros);
            return StemType.OTHER;
        case MEASURE_UNIT:
            parseMeasureUnitOption(segment, macros);
            return StemType.OTHER;
        case PER_MEASURE_UNIT:
            parseMeasurePerUnitOption(segment, macros);
            return StemType.OTHER;
        case MAYBE_INCREMENT_ROUNDER:
            parseIncrementOption(segment, macros);
            return StemType.ROUNDER;
        case INTEGER_WIDTH:
            parseIntegerWidthOption(segment, macros);
            return StemType.OTHER;
        case NUMBERING_SYSTEM:
            parseNumberingSystemOption(segment, macros);
            return StemType.OTHER;
        }

        ///// Non-required options: /////

        // Scientific options
        switch (stem) {
        case SCIENTIFIC_NOTATION:
            if (parseExponentWidthOption(segment, macros)) {
                return StemType.SCIENTIFIC_NOTATION;
            }
            if (parseExponentSignOption(segment, macros)) {
                return StemType.SCIENTIFIC_NOTATION;
            }
        }

        // Frac-sig option
        switch (stem) {
        case FRACTION_ROUNDER:
            if (parseFracSigOption(segment, macros)) {
                return StemType.ROUNDER;
            }
        }

        // Rounding mode option
        switch (stem) {
        case ROUNDER:
        case FRACTION_ROUNDER:
        case CURRENCY_ROUNDER:
            if (parseRoundingModeOption(segment, macros)) {
                return StemType.ROUNDER;
            }
        }

        // Unknown option
        throw new SkeletonSyntaxException("Invalid option", segment);
    }

    private static void generateSkeleton(MacroProps macros, StringBuilder sb) {
        // Supported options
        if (macros.notation != null && generateNotationValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.unit != null && generateUnitValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.perUnit != null && generatePerUnitValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.rounder != null && generateRoundingValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.grouping != null && generateGroupingValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.integerWidth != null && generateIntegerWidthValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.symbols != null && generateSymbolsValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.unitWidth != null && generateUnitWidthValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.sign != null && generateSignValue(macros, sb)) {
            sb.append(' ');
        }
        if (macros.decimal != null && generateDecimalValue(macros, sb)) {
            sb.append(' ');
        }

        // Unsupported options
        if (macros.padder != null) {
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom padder");
        }
        if (macros.affixProvider != null) {
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom affix provider");
        }
        if (macros.multiplier != null) {
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom multiplier");
        }
        if (macros.rules != null) {
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom plural rules");
        }

        // Remove the trailing space
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
    }

    /////

    private static boolean parseExponentWidthOption(StringSegment segment, MacroProps macros) {
        if (segment.charAt(0) != '+') {
            return false;
        }
        int offset = 1;
        int minExp = 0;
        for (; offset < segment.length(); offset++) {
            if (segment.charAt(offset) == 'e') {
                minExp++;
            } else {
                break;
            }
        }
        if (offset < segment.length()) {
            return false;
        }
        // Use the public APIs to enforce bounds checking
        macros.notation = ((ScientificNotation) macros.notation).withMinExponentDigits(minExp);
        return true;
    }

    private static void generateExponentWidthOption(int minExponentDigits, StringBuilder sb) {
        sb.append('+');
        appendMultiple(sb, 'e', minExponentDigits);
    }

    private static boolean parseExponentSignOption(StringSegment segment, MacroProps macros) {
        // Get the sign display type out of the CharsTrie data structure.
        // TODO: Make this more efficient (avoid object allocation)? It shouldn't be very hot code.
        CharsTrie tempStemTrie = new CharsTrie(SERIALIZED_STEM_TRIE, 0);
        BytesTrie.Result result = tempStemTrie.next(segment, 0, segment.length());
        if (result != BytesTrie.Result.INTERMEDIATE_VALUE && result != BytesTrie.Result.FINAL_VALUE) {
            return false;
        }
        SignDisplay sign = stemToSignDisplay(ACTUAL_STEM_VALUES[tempStemTrie.getValue()]);
        if (sign == null) {
            return false;
        }
        macros.notation = ((ScientificNotation) macros.notation).withExponentSignDisplay(sign);
        return true;
    }

    private static void generateCurrencyOption(Currency currency, StringBuilder sb) {
        sb.append(currency.getCurrencyCode());
    }

    private static void parseCurrencyOption(StringSegment segment, MacroProps macros) {
        String currencyCode = segment.subSequence(0, segment.length()).toString();
        try {
            macros.unit = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new SkeletonSyntaxException("Invalid currency", segment, e);
        }
    }

    private static void parseMeasureUnitOption(StringSegment segment, MacroProps macros) {
        // NOTE: The category (type) of the unit is guaranteed to be a valid subtag (alphanumeric)
        // http://unicode.org/reports/tr35/#Validity_Data
        int firstHyphen = 0;
        while (firstHyphen < segment.length() && segment.charAt(firstHyphen) != '-') {
            firstHyphen++;
        }
        if (firstHyphen == segment.length()) {
            throw new SkeletonSyntaxException("Invalid measure unit option", segment);
        }
        String type = segment.subSequence(0, firstHyphen).toString();
        String subType = segment.subSequence(firstHyphen + 1, segment.length()).toString();
        Set<MeasureUnit> units = MeasureUnit.getAvailable(type);
        for (MeasureUnit unit : units) {
            if (subType.equals(unit.getSubtype())) {
                macros.unit = unit;
                return;
            }
        }
        throw new SkeletonSyntaxException("Unknown measure unit", segment);
    }

    private static void generateMeasureUnitOption(MeasureUnit unit, StringBuilder sb) {
        sb.append(unit.getType() + "-" + unit.getSubtype());
    }

    private static void parseMeasurePerUnitOption(StringSegment segment, MacroProps macros) {
        // A little bit of a hack: safe the current unit (numerator), call the main measure unit parsing
        // code, put back the numerator unit, and put the new unit into per-unit.
        MeasureUnit numerator = macros.unit;
        parseMeasureUnitOption(segment, macros);
        macros.perUnit = macros.unit;
        macros.unit = numerator;
    }

    private static void parseFractionStem(StringSegment segment, MacroProps macros) {
        assert segment.charAt(0) == '.';
        int offset = 1;
        int minFrac = 0;
        int maxFrac;
        for (; offset < segment.length(); offset++) {
            if (segment.charAt(offset) == '0') {
                minFrac++;
            } else {
                break;
            }
        }
        if (offset < segment.length()) {
            if (segment.charAt(offset) == '+') {
                maxFrac = -1;
                offset++;
            } else {
                maxFrac = minFrac;
                for (; offset < segment.length(); offset++) {
                    if (segment.charAt(offset) == '#') {
                        maxFrac++;
                    } else {
                        break;
                    }
                }
            }
        } else {
            maxFrac = minFrac;
        }
        if (offset < segment.length()) {
            throw new SkeletonSyntaxException("Invalid fraction stem", segment);
        }
        // Use the public APIs to enforce bounds checking
        if (maxFrac == -1) {
            macros.rounder = Rounder.minFraction(minFrac);
        } else {
            macros.rounder = Rounder.minMaxFraction(minFrac, maxFrac);
        }
    }

    private static void generateFractionStem(int minFrac, int maxFrac, StringBuilder sb) {
        if (minFrac == 0 && maxFrac == 0) {
            sb.append("round-integer");
            return;
        }
        sb.append('.');
        appendMultiple(sb, '0', minFrac);
        if (maxFrac == -1) {
            sb.append('+');
        } else {
            appendMultiple(sb, '#', maxFrac - minFrac);
        }
    }

    private static void parseDigitsStem(StringSegment segment, MacroProps macros) {
        assert segment.charAt(0) == '@';
        int offset = 0;
        int minSig = 0;
        int maxSig;
        for (; offset < segment.length(); offset++) {
            if (segment.charAt(offset) == '@') {
                minSig++;
            } else {
                break;
            }
        }
        if (offset < segment.length()) {
            if (segment.charAt(offset) == '+') {
                maxSig = -1;
                offset++;
            } else {
                maxSig = minSig;
                for (; offset < segment.length(); offset++) {
                    if (segment.charAt(offset) == '#') {
                        maxSig++;
                    } else {
                        break;
                    }
                }
            }
        } else {
            maxSig = minSig;
        }
        if (offset < segment.length()) {
            throw new SkeletonSyntaxException("Invalid significant digits stem", segment);
        }
        // Use the public APIs to enforce bounds checking
        if (maxSig == -1) {
            macros.rounder = Rounder.minDigits(minSig);
        } else {
            macros.rounder = Rounder.minMaxDigits(minSig, maxSig);
        }
    }

    private static void generateDigitsStem(int minSig, int maxSig, StringBuilder sb) {
        appendMultiple(sb, '@', minSig);
        if (maxSig == -1) {
            sb.append('+');
        } else {
            appendMultiple(sb, '#', maxSig - minSig);
        }
    }

    private static boolean parseFracSigOption(StringSegment segment, MacroProps macros) {
        if (segment.charAt(0) != '@') {
            return false;
        }
        int offset = 0;
        int minSig = 0;
        int maxSig;
        for (; offset < segment.length(); offset++) {
            if (segment.charAt(offset) == '@') {
                minSig++;
            } else {
                break;
            }
        }
        // For the frac-sig option, there must be minSig or maxSig but not both.
        // Valid: @+, @@+, @@@+
        // Valid: @#, @##, @###
        // Invalid: @, @@, @@@
        // Invalid: @@#, @@##, @@@#
        if (offset < segment.length()) {
            if (segment.charAt(offset) == '+') {
                maxSig = -1;
                offset++;
            } else if (minSig > 1) {
                // @@#, @@##, @@@#
                throw new SkeletonSyntaxException("Invalid digits option for fraction rounder", segment);
            } else {
                maxSig = minSig;
                for (; offset < segment.length(); offset++) {
                    if (segment.charAt(offset) == '#') {
                        maxSig++;
                    } else {
                        break;
                    }
                }
            }
        } else {
            // @, @@, @@@
            throw new SkeletonSyntaxException("Invalid digits option for fraction rounder", segment);
        }
        if (offset < segment.length()) {
            throw new SkeletonSyntaxException("Invalid digits option for fraction rounder", segment);
        }

        FractionRounder oldRounder = (FractionRounder) macros.rounder;
        if (maxSig == -1) {
            macros.rounder = oldRounder.withMinDigits(minSig);
        } else {
            macros.rounder = oldRounder.withMaxDigits(maxSig);
        }
        return true;
    }

    private static void parseIncrementOption(StringSegment segment, MacroProps macros) {
        // Call segment.subSequence() because segment.toString() doesn't create a clean string.
        String str = segment.subSequence(0, segment.length()).toString();
        BigDecimal increment;
        try {
            increment = new BigDecimal(str);
        } catch (NumberFormatException e) {
            throw new SkeletonSyntaxException("Invalid rounding increment", segment, e);
        }
        macros.rounder = Rounder.increment(increment);
    }

    private static void generateIncrementOption(BigDecimal increment, StringBuilder sb) {
        sb.append(increment.toPlainString());
    }

    private static boolean parseRoundingModeOption(StringSegment segment, MacroProps macros) {
        for (int rm = 0; rm < ROUNDING_MODE_STRINGS.length; rm++) {
            if (segment.equals(ROUNDING_MODE_STRINGS[rm])) {
                macros.rounder = macros.rounder.withMode(RoundingMode.valueOf(rm));
                return true;
            }
        }
        return false;
    }

    private static void generateRoundingModeOption(RoundingMode mode, StringBuilder sb) {
        String option = ROUNDING_MODE_STRINGS[mode.ordinal()];
        sb.append(option);
    }

    private static void parseIntegerWidthOption(StringSegment segment, MacroProps macros) {
        int offset = 0;
        int minInt = 0;
        int maxInt;
        if (segment.charAt(0) == '+') {
            maxInt = -1;
            offset++;
        } else {
            maxInt = 0;
        }
        for (; offset < segment.length(); offset++) {
            if (segment.charAt(offset) == '#') {
                maxInt++;
            } else {
                break;
            }
        }
        if (offset < segment.length()) {
            for (; offset < segment.length(); offset++) {
                if (segment.charAt(offset) == '0') {
                    minInt++;
                } else {
                    break;
                }
            }
        }
        if (maxInt != -1) {
            maxInt += minInt;
        }
        if (offset < segment.length()) {
            throw new SkeletonSyntaxException("Invalid integer width stem", segment);
        }
        // Use the public APIs to enforce bounds checking
        if (maxInt == -1) {
            macros.integerWidth = IntegerWidth.zeroFillTo(minInt);
        } else {
            macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(maxInt);
        }
    }

    private static void generateIntegerWidthOption(int minInt, int maxInt, StringBuilder sb) {
        if (maxInt == -1) {
            sb.append('+');
        } else {
            appendMultiple(sb, '#', maxInt - minInt);
        }
        appendMultiple(sb, '0', minInt);
    }

    private static void parseNumberingSystemOption(StringSegment segment, MacroProps macros) {
        String nsName = segment.subSequence(0, segment.length()).toString();
        NumberingSystem ns = NumberingSystem.getInstanceByName(nsName);
        if (ns == null) {
            throw new SkeletonSyntaxException("Unknown numbering system", segment);
        }
        macros.symbols = ns;
    }

    private static void generateNumberingSystemOption(NumberingSystem ns, StringBuilder sb) {
        sb.append(ns.getName());
    }

    /////

    private static boolean generateNotationValue(MacroProps macros, StringBuilder sb) {
        // Check for literals
        String literal = skeletonData.valueToStem(macros.notation);
        if ("notation-simple".equals(literal)) {
            return false; // Default value
        } else if (literal != null) {
            sb.append(literal);
            return true;
        }

        // Generate the stem
        if (macros.notation instanceof CompactNotation) {
            // Compact notation generated from custom data (not supported in skeleton)
            // The other compact notations are literals
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom compact data");
        } else if (macros.notation instanceof ScientificNotation) {
            ScientificNotation impl = (ScientificNotation) macros.notation;
            if (impl.engineeringInterval == 3) {
                sb.append("engineering");
            } else {
                sb.append("scientific");
            }
            if (impl.minExponentDigits > 1) {
                sb.append('/');
                generateExponentWidthOption(impl.minExponentDigits, sb);
            }
            if (impl.exponentSignDisplay != SignDisplay.AUTO) {
                sb.append('/');
                sb.append(skeletonData.valueToStem(impl.exponentSignDisplay));
            }
            return true;
        } else {
            // SimpleNotation should be handled by a literal
            throw new AssertionError();
        }
    }

    private static boolean generateUnitValue(MacroProps macros, StringBuilder sb) {
        // Check for literals
        String literal = skeletonData.valueToStem(macros.unit);
        if ("base-unit".equals(literal)) {
            return false; // Default value
        } else if (literal != null) {
            sb.append(literal);
            return true;
        }

        // Generate the stem
        if (macros.unit instanceof Currency) {
            sb.append("currency/");
            generateCurrencyOption((Currency) macros.unit, sb);
            return true;
        } else if (macros.unit instanceof NoUnit) {
            // This should be taken care of by the literals.
            throw new AssertionError();
        } else {
            sb.append("measure-unit/");
            generateMeasureUnitOption(macros.unit, sb);
            return true;
        }
    }

    private static boolean generatePerUnitValue(MacroProps macros, StringBuilder sb) {
        // Per-units are currently expected to be only MeasureUnits.
        if (macros.unit instanceof Currency || macros.unit instanceof NoUnit) {
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with per-unit that is not a standard measure unit");
        } else {
            sb.append("per-measure-unit/");
            generateMeasureUnitOption(macros.perUnit, sb);
            return true;
        }
    }

    private static boolean generateRoundingValue(MacroProps macros, StringBuilder sb) {
        // Check for literals
        String literal = skeletonData.valueToStem(macros.rounder);
        if (literal != null) {
            sb.append(literal);
            return true;
        }

        // Generate the stem
        if (macros.rounder instanceof Rounder.InfiniteRounderImpl) {
            sb.append("round-unlimited");
        } else if (macros.rounder instanceof Rounder.FractionRounderImpl) {
            Rounder.FractionRounderImpl impl = (Rounder.FractionRounderImpl) macros.rounder;
            generateFractionStem(impl.minFrac, impl.maxFrac, sb);
        } else if (macros.rounder instanceof Rounder.SignificantRounderImpl) {
            Rounder.SignificantRounderImpl impl = (Rounder.SignificantRounderImpl) macros.rounder;
            generateDigitsStem(impl.minSig, impl.maxSig, sb);
        } else if (macros.rounder instanceof Rounder.FracSigRounderImpl) {
            Rounder.FracSigRounderImpl impl = (Rounder.FracSigRounderImpl) macros.rounder;
            generateFractionStem(impl.minFrac, impl.maxFrac, sb);
            sb.append('/');
            if (impl.minSig == -1) {
                generateDigitsStem(1, impl.maxSig, sb);
            } else {
                generateDigitsStem(impl.minSig, -1, sb);
            }
        } else if (macros.rounder instanceof Rounder.IncrementRounderImpl) {
            Rounder.IncrementRounderImpl impl = (Rounder.IncrementRounderImpl) macros.rounder;
            sb.append("round-increment/");
            generateIncrementOption(impl.increment, sb);
        } else {
            assert macros.rounder instanceof Rounder.CurrencyRounderImpl;
            Rounder.CurrencyRounderImpl impl = (Rounder.CurrencyRounderImpl) macros.rounder;
            if (impl.usage == CurrencyUsage.STANDARD) {
                sb.append("round-currency-standard");
            } else {
                sb.append("round-currency-cash");
            }
        }

        // Generate the options
        if (macros.rounder.mathContext != Rounder.DEFAULT_MATH_CONTEXT) {
            sb.append('/');
            generateRoundingModeOption(macros.rounder.mathContext.getRoundingMode(), sb);
        }

        // NOTE: Always return true for rounding because the default value depends on other options.
        return true;
    }

    private static boolean generateGroupingValue(MacroProps macros, StringBuilder sb) {
        if (macros.grouping instanceof GroupingStrategy) {
            if (macros.grouping == GroupingStrategy.AUTO) {
                return false; // Default value
            }
            appendExpectedLiteral(macros.grouping, sb);
            return true;
        } else {
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom Grouper");
        }
    }

    private static boolean generateIntegerWidthValue(MacroProps macros, StringBuilder sb) {
        if (macros.integerWidth.equals(IntegerWidth.DEFAULT)) {
            return false; // Default
        }
        sb.append("integer-width/");
        generateIntegerWidthOption(macros.integerWidth.minInt, macros.integerWidth.maxInt, sb);
        return true;
    }

    private static boolean generateSymbolsValue(MacroProps macros, StringBuilder sb) {
        if (macros.symbols instanceof NumberingSystem) {
            NumberingSystem ns = (NumberingSystem) macros.symbols;
            if (ns.getName().equals("latn")) {
                sb.append("latin");
            } else {
                sb.append("numbering-system/");
                generateNumberingSystemOption(ns, sb);
            }
            return true;
        } else {
            assert macros.symbols instanceof DecimalFormatSymbols;
            throw new UnsupportedOperationException(
                    "Cannot generate number skeleton with custom DecimalFormatSymbols");
        }
    }

    private static boolean generateUnitWidthValue(MacroProps macros, StringBuilder sb) {
        if (macros.unitWidth == UnitWidth.SHORT) {
            return false; // Default value
        }
        appendExpectedLiteral(macros.unitWidth, sb);
        return true;
    }

    private static boolean generateSignValue(MacroProps macros, StringBuilder sb) {
        if (macros.sign == SignDisplay.AUTO) {
            return false; // Default value
        }
        appendExpectedLiteral(macros.sign, sb);
        return true;
    }

    private static boolean generateDecimalValue(MacroProps macros, StringBuilder sb) {
        if (macros.decimal == DecimalSeparatorDisplay.AUTO) {
            return false; // Default value
        }
        appendExpectedLiteral(macros.decimal, sb);
        return true;
    }

    /////

    private static void checkNull(Object value, CharSequence content) {
        if (value != null) {
            throw new SkeletonSyntaxException("Duplicated setting", content);
        }
    }

    private static void appendMultiple(StringBuilder sb, int cp, int count) {
        for (int i = 0; i < count; i++) {
            sb.appendCodePoint(cp);
        }
    }

    private static void appendExpectedLiteral(Object value, StringBuilder sb) {
        String literal = skeletonData.valueToStem(value);
        assert literal != null;
        sb.append(literal);
    }
}
