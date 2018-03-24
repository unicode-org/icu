// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * While parsing a skeleton, this enum records what type of option we expect to find next.
     */
    static enum ParseState {
        // Section 0: We expect whitespace or a stem, but not an option:
        STATE_NULL,

        // Section 1: We might accept an option, but it is not required:
        STATE_SCIENTIFIC,
        STATE_ROUNDER,
        STATE_FRACTION_ROUNDER,

        // Section 2: An option is required:
        STATE_INCREMENT_ROUNDER,
        STATE_MEASURE_UNIT,
        STATE_PER_MEASURE_UNIT,
        STATE_CURRENCY_UNIT,
        STATE_INTEGER_WIDTH,
        STATE_NUMBERING_SYSTEM,
    }

    /**
     * All possible stem literals have an entry in the StemEnum. The enum name is the kebab case stem
     * string literal written in upper snake case.
     *
     * @see StemToObject
     * @see #SERIALIZED_STEM_TRIE
     */
    static enum StemEnum {
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

    /** For mapping from ordinal back to StemEnum in Java. */
    static final StemEnum[] STEM_ENUM_VALUES = StemEnum.values();

    /** A data structure for mapping from stem strings to the stem enum. Built at startup. */
    static final String SERIALIZED_STEM_TRIE = buildStemTrie();

    static String buildStemTrie() {
        CharsTrieBuilder b = new CharsTrieBuilder();

        // Section 1:
        b.add("compact-short", StemEnum.STEM_COMPACT_SHORT.ordinal());
        b.add("compact-long", StemEnum.STEM_COMPACT_LONG.ordinal());
        b.add("scientific", StemEnum.STEM_SCIENTIFIC.ordinal());
        b.add("engineering", StemEnum.STEM_ENGINEERING.ordinal());
        b.add("notation-simple", StemEnum.STEM_NOTATION_SIMPLE.ordinal());
        b.add("base-unit", StemEnum.STEM_BASE_UNIT.ordinal());
        b.add("percent", StemEnum.STEM_PERCENT.ordinal());
        b.add("permille", StemEnum.STEM_PERMILLE.ordinal());
        b.add("round-integer", StemEnum.STEM_ROUND_INTEGER.ordinal());
        b.add("round-unlimited", StemEnum.STEM_ROUND_UNLIMITED.ordinal());
        b.add("round-currency-standard", StemEnum.STEM_ROUND_CURRENCY_STANDARD.ordinal());
        b.add("round-currency-cash", StemEnum.STEM_ROUND_CURRENCY_CASH.ordinal());
        b.add("group-off", StemEnum.STEM_GROUP_OFF.ordinal());
        b.add("group-min2", StemEnum.STEM_GROUP_MIN2.ordinal());
        b.add("group-auto", StemEnum.STEM_GROUP_AUTO.ordinal());
        b.add("group-on-aligned", StemEnum.STEM_GROUP_ON_ALIGNED.ordinal());
        b.add("group-thousands", StemEnum.STEM_GROUP_THOUSANDS.ordinal());
        b.add("latin", StemEnum.STEM_LATIN.ordinal());
        b.add("unit-width-narrow", StemEnum.STEM_UNIT_WIDTH_NARROW.ordinal());
        b.add("unit-width-short", StemEnum.STEM_UNIT_WIDTH_SHORT.ordinal());
        b.add("unit-width-full-name", StemEnum.STEM_UNIT_WIDTH_FULL_NAME.ordinal());
        b.add("unit-width-iso-code", StemEnum.STEM_UNIT_WIDTH_ISO_CODE.ordinal());
        b.add("unit-width-hidden", StemEnum.STEM_UNIT_WIDTH_HIDDEN.ordinal());
        b.add("sign-auto", StemEnum.STEM_SIGN_AUTO.ordinal());
        b.add("sign-always", StemEnum.STEM_SIGN_ALWAYS.ordinal());
        b.add("sign-never", StemEnum.STEM_SIGN_NEVER.ordinal());
        b.add("sign-accounting", StemEnum.STEM_SIGN_ACCOUNTING.ordinal());
        b.add("sign-accounting-always", StemEnum.STEM_SIGN_ACCOUNTING_ALWAYS.ordinal());
        b.add("sign-except-zero", StemEnum.STEM_SIGN_EXCEPT_ZERO.ordinal());
        b.add("sign-accounting-except-zero", StemEnum.STEM_SIGN_ACCOUNTING_EXCEPT_ZERO.ordinal());
        b.add("decimal-auto", StemEnum.STEM_DECIMAL_AUTO.ordinal());
        b.add("decimal-always", StemEnum.STEM_DECIMAL_ALWAYS.ordinal());

        // Section 2:
        b.add("round-increment", StemEnum.STEM_ROUND_INCREMENT.ordinal());
        b.add("measure-unit", StemEnum.STEM_MEASURE_UNIT.ordinal());
        b.add("per-measure-unit", StemEnum.STEM_PER_MEASURE_UNIT.ordinal());
        b.add("currency", StemEnum.STEM_CURRENCY.ordinal());
        b.add("integer-width", StemEnum.STEM_INTEGER_WIDTH.ordinal());
        b.add("numbering-system", StemEnum.STEM_NUMBERING_SYSTEM.ordinal());

        // Build the CharsTrie
        // TODO: Use SLOW or FAST here?
        return b.buildCharSequence(StringTrieBuilder.Option.FAST).toString();
    }

    /**
     * Utility class for methods that convert from StemEnum to corresponding objects or enums. This
     * applies to only the "Section 1" stems, those that are well-defined without an option.
     */
    static final class StemToObject {

        private static Notation notation(StemEnum stem) {
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
                throw new AssertionError();
            }
        }

        private static MeasureUnit unit(StemEnum stem) {
            switch (stem) {
            case STEM_BASE_UNIT:
                return NoUnit.BASE;
            case STEM_PERCENT:
                return NoUnit.PERCENT;
            case STEM_PERMILLE:
                return NoUnit.PERMILLE;
            default:
                throw new AssertionError();
            }
        }

        private static Rounder rounder(StemEnum stem) {
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
                throw new AssertionError();
            }
        }

        private static GroupingStrategy groupingStrategy(StemEnum stem) {
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
                return null; // for objects, throw; for enums, return null
            }
        }

        private static UnitWidth unitWidth(StemEnum stem) {
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
                return null; // for objects, throw; for enums, return null
            }
        }

        private static SignDisplay signDisplay(StemEnum stem) {
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
                return null; // for objects, throw; for enums, return null
            }
        }

        private static DecimalSeparatorDisplay decimalSeparatorDisplay(StemEnum stem) {
            switch (stem) {
            case STEM_DECIMAL_AUTO:
                return DecimalSeparatorDisplay.AUTO;
            case STEM_DECIMAL_ALWAYS:
                return DecimalSeparatorDisplay.ALWAYS;
            default:
                return null; // for objects, throw; for enums, return null
            }
        }
    }

    /**
     * Utility class for methods that convert from enums to stem strings. More complex object conversions
     * take place in ObjectToStemString.
     */
    static final class EnumToStemString {

        private static void groupingStrategy(GroupingStrategy value, StringBuilder sb) {
            switch (value) {
            case OFF:
                sb.append("group-off");
                break;
            case MIN2:
                sb.append("group-min2");
                break;
            case AUTO:
                sb.append("group-auto");
                break;
            case ON_ALIGNED:
                sb.append("group-on-aligned");
                break;
            case THOUSANDS:
                sb.append("group-thousands");
                break;
            default:
                throw new AssertionError();
            }
        }

        private static void unitWidth(UnitWidth value, StringBuilder sb) {
            switch (value) {
            case NARROW:
                sb.append("unit-width-narrow");
                break;
            case SHORT:
                sb.append("unit-width-short");
                break;
            case FULL_NAME:
                sb.append("unit-width-full-name");
                break;
            case ISO_CODE:
                sb.append("unit-width-iso-code");
                break;
            case HIDDEN:
                sb.append("unit-width-hidden");
                break;
            default:
                throw new AssertionError();
            }
        }

        private static void signDisplay(SignDisplay value, StringBuilder sb) {
            switch (value) {
            case AUTO:
                sb.append("sign-auto");
                break;
            case ALWAYS:
                sb.append("sign-always");
                break;
            case NEVER:
                sb.append("sign-never");
                break;
            case ACCOUNTING:
                sb.append("sign-accounting");
                break;
            case ACCOUNTING_ALWAYS:
                sb.append("sign-accounting-always");
                break;
            case EXCEPT_ZERO:
                sb.append("sign-except-zero");
                break;
            case ACCOUNTING_EXCEPT_ZERO:
                sb.append("sign-accounting-except-zero");
                break;
            default:
                throw new AssertionError();
            }
        }

        private static void decimalSeparatorDisplay(DecimalSeparatorDisplay value, StringBuilder sb) {
            switch (value) {
            case AUTO:
                sb.append("decimal-auto");
                break;
            case ALWAYS:
                sb.append("decimal-always");
                break;
            default:
                throw new AssertionError();
            }
        }
    }

    /** Kebab case versions of the rounding mode enum. */
    static final String[] ROUNDING_MODE_STRINGS = {
            "up",
            "down",
            "ceiling",
            "floor",
            "half-up",
            "half-down",
            "half-even",
            "unnecessary" };

    ///// ENTRYPOINT FUNCTIONS /////

    /** Cache for parsed skeleton strings. */
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

    ///// MAIN PARSING FUNCTIONS /////

    /**
     * Converts from a skeleton string to a MacroProps. This method contains the primary parse loop.
     */
    private static MacroProps parseSkeleton(String skeletonString) {
        // Add a trailing whitespace to the end of the skeleton string to make code cleaner.
        skeletonString += " ";

        MacroProps macros = new MacroProps();
        StringSegment segment = new StringSegment(skeletonString, false);
        CharsTrie stemTrie = new CharsTrie(SERIALIZED_STEM_TRIE, 0);
        ParseState stem = ParseState.STATE_NULL;
        int offset = 0;

        // Primary skeleton parse loop:
        while (offset < segment.length()) {
            int cp = segment.codePointAt(offset);
            boolean isTokenSeparator = PatternProps.isWhiteSpace(cp);
            boolean isOptionSeparator = (cp == '/');

            if (!isTokenSeparator && !isOptionSeparator) {
                // Non-separator token; consume it.
                offset += Character.charCount(cp);
                if (stem == ParseState.STATE_NULL) {
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
                if (stem == ParseState.STATE_NULL) {
                    // The first separator after the start of a token. Parse it as a stem.
                    stem = parseStem(segment, stemTrie, macros);
                    stemTrie.reset();
                } else {
                    // A separator after the first separator of a token. Parse it as an option.
                    stem = parseOption(stem, segment, macros);
                }
                segment.resetLength();

                // Consume the segment:
                segment.adjustOffset(offset);
                offset = 0;

            } else if (stem != ParseState.STATE_NULL) {
                // A separator ('/' or whitespace) following an option separator ('/')
                segment.setLength(Character.charCount(cp)); // for error message
                throw new SkeletonSyntaxException("Unexpected separator character", segment);

            } else {
                // Two spaces in a row; this is OK.
            }

            // Does the current stem forbid options?
            if (isOptionSeparator && stem == ParseState.STATE_NULL) {
                segment.setLength(Character.charCount(cp)); // for error message
                throw new SkeletonSyntaxException("Unexpected option separator", segment);
            }

            // Does the current stem require an option?
            if (isTokenSeparator && stem != ParseState.STATE_NULL) {
                switch (stem) {
                case STATE_INCREMENT_ROUNDER:
                case STATE_MEASURE_UNIT:
                case STATE_PER_MEASURE_UNIT:
                case STATE_CURRENCY_UNIT:
                case STATE_INTEGER_WIDTH:
                case STATE_NUMBERING_SYSTEM:
                    segment.setLength(Character.charCount(cp)); // for error message
                    throw new SkeletonSyntaxException("Stem requires an option", segment);
                default:
                    break;
                }
                stem = ParseState.STATE_NULL;
            }

            // Consume the separator:
            segment.adjustOffset(Character.charCount(cp));
        }
        assert stem == ParseState.STATE_NULL;
        return macros;
    }

    /**
     * Given that the current segment represents an stem, parse it and save the result.
     *
     * @return The next state after parsing this stem, corresponding to what subset of options to expect.
     */
    private static ParseState parseStem(StringSegment segment, CharsTrie stemTrie, MacroProps macros) {
        // First check for "blueprint" stems, which start with a "signal char"
        switch (segment.charAt(0)) {
        case '.':
            checkNull(macros.rounder, segment);
            BlueprintHelpers.parseFractionStem(segment, macros);
            return ParseState.STATE_FRACTION_ROUNDER;
        case '@':
            checkNull(macros.rounder, segment);
            BlueprintHelpers.parseDigitsStem(segment, macros);
            return ParseState.STATE_NULL;
        }

        // Now look at the stemsTrie, which is already be pointing at our stem.
        BytesTrie.Result stemResult = stemTrie.current();

        if (stemResult != BytesTrie.Result.INTERMEDIATE_VALUE
                && stemResult != BytesTrie.Result.FINAL_VALUE) {
            throw new SkeletonSyntaxException("Unknown stem", segment);
        }

        StemEnum stem = STEM_ENUM_VALUES[stemTrie.getValue()];
        switch (stem) {

        // Stems with meaning on their own, not requiring an option:

        case STEM_COMPACT_SHORT:
        case STEM_COMPACT_LONG:
        case STEM_SCIENTIFIC:
        case STEM_ENGINEERING:
        case STEM_NOTATION_SIMPLE:
            checkNull(macros.notation, segment);
            macros.notation = StemToObject.notation(stem);
            switch (stem) {
            case STEM_SCIENTIFIC:
            case STEM_ENGINEERING:
                return ParseState.STATE_SCIENTIFIC; // allows for scientific options
            default:
                return ParseState.STATE_NULL;
            }

        case STEM_BASE_UNIT:
        case STEM_PERCENT:
        case STEM_PERMILLE:
            checkNull(macros.unit, segment);
            macros.unit = StemToObject.unit(stem);
            return ParseState.STATE_NULL;

        case STEM_ROUND_INTEGER:
        case STEM_ROUND_UNLIMITED:
        case STEM_ROUND_CURRENCY_STANDARD:
        case STEM_ROUND_CURRENCY_CASH:
            checkNull(macros.rounder, segment);
            macros.rounder = StemToObject.rounder(stem);
            switch (stem) {
            case STEM_ROUND_INTEGER:
                return ParseState.STATE_FRACTION_ROUNDER; // allows for "round-integer/@##"
            default:
                return ParseState.STATE_ROUNDER; // allows for rounding mode options
            }

        case STEM_GROUP_OFF:
        case STEM_GROUP_MIN2:
        case STEM_GROUP_AUTO:
        case STEM_GROUP_ON_ALIGNED:
        case STEM_GROUP_THOUSANDS:
            checkNull(macros.grouping, segment);
            macros.grouping = StemToObject.groupingStrategy(stem);
            return ParseState.STATE_NULL;

        case STEM_LATIN:
            checkNull(macros.symbols, segment);
            macros.symbols = NumberingSystem.LATIN;
            return ParseState.STATE_NULL;

        case STEM_UNIT_WIDTH_NARROW:
        case STEM_UNIT_WIDTH_SHORT:
        case STEM_UNIT_WIDTH_FULL_NAME:
        case STEM_UNIT_WIDTH_ISO_CODE:
        case STEM_UNIT_WIDTH_HIDDEN:
            checkNull(macros.unitWidth, segment);
            macros.unitWidth = StemToObject.unitWidth(stem);
            return ParseState.STATE_NULL;

        case STEM_SIGN_AUTO:
        case STEM_SIGN_ALWAYS:
        case STEM_SIGN_NEVER:
        case STEM_SIGN_ACCOUNTING:
        case STEM_SIGN_ACCOUNTING_ALWAYS:
        case STEM_SIGN_EXCEPT_ZERO:
        case STEM_SIGN_ACCOUNTING_EXCEPT_ZERO:
            checkNull(macros.sign, segment);
            macros.sign = StemToObject.signDisplay(stem);
            return ParseState.STATE_NULL;

        case STEM_DECIMAL_AUTO:
        case STEM_DECIMAL_ALWAYS:
            checkNull(macros.decimal, segment);
            macros.decimal = StemToObject.decimalSeparatorDisplay(stem);
            return ParseState.STATE_NULL;

        // Stems requiring an option:

        case STEM_ROUND_INCREMENT:
            checkNull(macros.rounder, segment);
            return ParseState.STATE_INCREMENT_ROUNDER;

        case STEM_MEASURE_UNIT:
            checkNull(macros.unit, segment);
            return ParseState.STATE_MEASURE_UNIT;

        case STEM_PER_MEASURE_UNIT:
            checkNull(macros.perUnit, segment);
            return ParseState.STATE_PER_MEASURE_UNIT;

        case STEM_CURRENCY:
            checkNull(macros.unit, segment);
            return ParseState.STATE_CURRENCY_UNIT;

        case STEM_INTEGER_WIDTH:
            checkNull(macros.integerWidth, segment);
            return ParseState.STATE_INTEGER_WIDTH;

        case STEM_NUMBERING_SYSTEM:
            checkNull(macros.symbols, segment);
            return ParseState.STATE_NUMBERING_SYSTEM;

        default:
            throw new AssertionError();
        }
    }

    /**
     * Given that the current segment represents an option, parse it and save the result.
     *
     * @return The next state after parsing this option, corresponding to what subset of options to
     *         expect next.
     */
    private static ParseState parseOption(ParseState stem, StringSegment segment, MacroProps macros) {

        ///// Required options: /////

        switch (stem) {
        case STATE_CURRENCY_UNIT:
            BlueprintHelpers.parseCurrencyOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_MEASURE_UNIT:
            BlueprintHelpers.parseMeasureUnitOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_PER_MEASURE_UNIT:
            BlueprintHelpers.parseMeasurePerUnitOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_INCREMENT_ROUNDER:
            BlueprintHelpers.parseIncrementOption(segment, macros);
            return ParseState.STATE_ROUNDER;
        case STATE_INTEGER_WIDTH:
            BlueprintHelpers.parseIntegerWidthOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_NUMBERING_SYSTEM:
            BlueprintHelpers.parseNumberingSystemOption(segment, macros);
            return ParseState.STATE_NULL;
        default:
            break;
        }

        ///// Non-required options: /////

        // Scientific options
        switch (stem) {
        case STATE_SCIENTIFIC:
            if (BlueprintHelpers.parseExponentWidthOption(segment, macros)) {
                return ParseState.STATE_SCIENTIFIC;
            }
            if (BlueprintHelpers.parseExponentSignOption(segment, macros)) {
                return ParseState.STATE_SCIENTIFIC;
            }
            break;
        default:
            break;
        }

        // Frac-sig option
        switch (stem) {
        case STATE_FRACTION_ROUNDER:
            if (BlueprintHelpers.parseFracSigOption(segment, macros)) {
                return ParseState.STATE_ROUNDER;
            }
            break;
        default:
            break;
        }

        // Rounding mode option
        switch (stem) {
        case STATE_ROUNDER:
        case STATE_FRACTION_ROUNDER:
            if (BlueprintHelpers.parseRoundingModeOption(segment, macros)) {
                return ParseState.STATE_ROUNDER;
            }
            break;
        default:
            break;
        }

        // Unknown option
        throw new SkeletonSyntaxException("Invalid option", segment);
    }

    ///// MAIN SKELETON GENERATION FUNCTION /////

    /**
     * Main skeleton generator function. Appends the normalized skeleton for the MacroProps to the given
     * StringBuilder.
     */
    private static void generateSkeleton(MacroProps macros, StringBuilder sb) {
        // Supported options
        if (macros.notation != null && GeneratorHelpers.notation(macros, sb)) {
            sb.append(' ');
        }
        if (macros.unit != null && GeneratorHelpers.unit(macros, sb)) {
            sb.append(' ');
        }
        if (macros.perUnit != null && GeneratorHelpers.perUnit(macros, sb)) {
            sb.append(' ');
        }
        if (macros.rounder != null && GeneratorHelpers.rounding(macros, sb)) {
            sb.append(' ');
        }
        if (macros.grouping != null && GeneratorHelpers.grouping(macros, sb)) {
            sb.append(' ');
        }
        if (macros.integerWidth != null && GeneratorHelpers.integerWidth(macros, sb)) {
            sb.append(' ');
        }
        if (macros.symbols != null && GeneratorHelpers.symbols(macros, sb)) {
            sb.append(' ');
        }
        if (macros.unitWidth != null && GeneratorHelpers.unitWidth(macros, sb)) {
            sb.append(' ');
        }
        if (macros.sign != null && GeneratorHelpers.sign(macros, sb)) {
            sb.append(' ');
        }
        if (macros.decimal != null && GeneratorHelpers.decimal(macros, sb)) {
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

    ///// BLUEPRINT HELPER FUNCTIONS /////

    /**
     * Utility class for methods for processing stems and options that cannot be interpreted literally.
     */
    static final class BlueprintHelpers {

        /** @return Whether we successfully found and parsed an exponent width option. */
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

        /** @return Whether we successfully found and parsed an exponent sign option. */
        private static boolean parseExponentSignOption(StringSegment segment, MacroProps macros) {
            // Get the sign display type out of the CharsTrie data structure.
            // TODO: Make this more efficient (avoid object allocation)? It shouldn't be very hot code.
            CharsTrie tempStemTrie = new CharsTrie(SERIALIZED_STEM_TRIE, 0);
            BytesTrie.Result result = tempStemTrie.next(segment, 0, segment.length());
            if (result != BytesTrie.Result.INTERMEDIATE_VALUE
                    && result != BytesTrie.Result.FINAL_VALUE) {
                return false;
            }
            SignDisplay sign = StemToObject.signDisplay(STEM_ENUM_VALUES[tempStemTrie.getValue()]);
            if (sign == null) {
                return false;
            }
            macros.notation = ((ScientificNotation) macros.notation).withExponentSignDisplay(sign);
            return true;
        }

        private static void parseCurrencyOption(StringSegment segment, MacroProps macros) {
            String currencyCode = segment.subSequence(0, segment.length()).toString();
            Currency currency;
            try {
                currency = Currency.getInstance(currencyCode);
            } catch (IllegalArgumentException e) {
                // Not 3 ascii chars
                throw new SkeletonSyntaxException("Invalid currency", segment, e);
            }
            macros.unit = currency;
        }

        private static void generateCurrencyOption(Currency currency, StringBuilder sb) {
            sb.append(currency.getCurrencyCode());
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
            sb.append(unit.getType());
            sb.append("-");
            sb.append(unit.getSubtype());
        }

        private static void parseMeasurePerUnitOption(StringSegment segment, MacroProps macros) {
            // A little bit of a hack: safe the current unit (numerator), call the main measure unit
            // parsing code, put back the numerator unit, and put the new unit into per-unit.
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

        /** @return Whether we successfully found and parsed a frac-sig option. */
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
                    throw new SkeletonSyntaxException("Invalid digits option for fraction rounder",
                            segment);
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

        /** @return Whether we successfully found and parsed a rounding mode. */
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
            sb.append(ROUNDING_MODE_STRINGS[mode.ordinal()]);
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
    }

    ///// STEM GENERATION HELPER FUNCTIONS /////

    /**
     * Utility class for methods for generating a token corresponding to each macro-prop. Each method
     * returns whether or not a token was written to the string builder.
     */
    static final class GeneratorHelpers {

        private static boolean notation(MacroProps macros, StringBuilder sb) {
            if (macros.notation instanceof CompactNotation) {
                if (macros.notation == Notation.compactLong()) {
                    sb.append("compact-long");
                    return true;
                } else if (macros.notation == Notation.compactShort()) {
                    sb.append("compact-short");
                    return true;
                } else {
                    // Compact notation generated from custom data (not supported in skeleton)
                    // The other compact notations are literals
                    throw new UnsupportedOperationException(
                            "Cannot generate number skeleton with custom compact data");
                }
            } else if (macros.notation instanceof ScientificNotation) {
                ScientificNotation impl = (ScientificNotation) macros.notation;
                if (impl.engineeringInterval == 3) {
                    sb.append("engineering");
                } else {
                    sb.append("scientific");
                }
                if (impl.minExponentDigits > 1) {
                    sb.append('/');
                    BlueprintHelpers.generateExponentWidthOption(impl.minExponentDigits, sb);
                }
                if (impl.exponentSignDisplay != SignDisplay.AUTO) {
                    sb.append('/');
                    EnumToStemString.signDisplay(impl.exponentSignDisplay, sb);
                }
                return true;
            } else {
                assert macros.notation instanceof SimpleNotation;
                // Default value is not shown in normalized form
                return false;
            }
        }

        private static boolean unit(MacroProps macros, StringBuilder sb) {
            if (macros.unit instanceof Currency) {
                sb.append("currency/");
                BlueprintHelpers.generateCurrencyOption((Currency) macros.unit, sb);
                return true;
            } else if (macros.unit instanceof NoUnit) {
                if (macros.unit == NoUnit.PERCENT) {
                    sb.append("percent");
                    return true;
                } else if (macros.unit == NoUnit.PERMILLE) {
                    sb.append("permille");
                    return true;
                } else {
                    assert macros.unit == NoUnit.BASE;
                    // Default value is not shown in normalized form
                    return false;
                }
            } else {
                sb.append("measure-unit/");
                BlueprintHelpers.generateMeasureUnitOption(macros.unit, sb);
                return true;
            }
        }

        private static boolean perUnit(MacroProps macros, StringBuilder sb) {
            // Per-units are currently expected to be only MeasureUnits.
            if (macros.perUnit instanceof Currency || macros.perUnit instanceof NoUnit) {
                throw new UnsupportedOperationException(
                        "Cannot generate number skeleton with per-unit that is not a standard measure unit");
            } else {
                sb.append("per-measure-unit/");
                BlueprintHelpers.generateMeasureUnitOption(macros.perUnit, sb);
                return true;
            }
        }

        private static boolean rounding(MacroProps macros, StringBuilder sb) {
            if (macros.rounder instanceof Rounder.InfiniteRounderImpl) {
                sb.append("round-unlimited");
            } else if (macros.rounder instanceof Rounder.FractionRounderImpl) {
                Rounder.FractionRounderImpl impl = (Rounder.FractionRounderImpl) macros.rounder;
                BlueprintHelpers.generateFractionStem(impl.minFrac, impl.maxFrac, sb);
            } else if (macros.rounder instanceof Rounder.SignificantRounderImpl) {
                Rounder.SignificantRounderImpl impl = (Rounder.SignificantRounderImpl) macros.rounder;
                BlueprintHelpers.generateDigitsStem(impl.minSig, impl.maxSig, sb);
            } else if (macros.rounder instanceof Rounder.FracSigRounderImpl) {
                Rounder.FracSigRounderImpl impl = (Rounder.FracSigRounderImpl) macros.rounder;
                BlueprintHelpers.generateFractionStem(impl.minFrac, impl.maxFrac, sb);
                sb.append('/');
                if (impl.minSig == -1) {
                    BlueprintHelpers.generateDigitsStem(1, impl.maxSig, sb);
                } else {
                    BlueprintHelpers.generateDigitsStem(impl.minSig, -1, sb);
                }
            } else if (macros.rounder instanceof Rounder.IncrementRounderImpl) {
                Rounder.IncrementRounderImpl impl = (Rounder.IncrementRounderImpl) macros.rounder;
                sb.append("round-increment/");
                BlueprintHelpers.generateIncrementOption(impl.increment, sb);
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
                BlueprintHelpers.generateRoundingModeOption(macros.rounder.mathContext.getRoundingMode(),
                        sb);
            }

            // NOTE: Always return true for rounding because the default value depends on other options.
            return true;
        }

        private static boolean grouping(MacroProps macros, StringBuilder sb) {
            if (macros.grouping instanceof GroupingStrategy) {
                if (macros.grouping == GroupingStrategy.AUTO) {
                    return false; // Default value
                }
                EnumToStemString.groupingStrategy((GroupingStrategy) macros.grouping, sb);
                return true;
            } else {
                throw new UnsupportedOperationException(
                        "Cannot generate number skeleton with custom Grouper");
            }
        }

        private static boolean integerWidth(MacroProps macros, StringBuilder sb) {
            if (macros.integerWidth.equals(IntegerWidth.DEFAULT)) {
                return false; // Default
            }
            sb.append("integer-width/");
            BlueprintHelpers.generateIntegerWidthOption(macros.integerWidth.minInt,
                    macros.integerWidth.maxInt,
                    sb);
            return true;
        }

        private static boolean symbols(MacroProps macros, StringBuilder sb) {
            if (macros.symbols instanceof NumberingSystem) {
                NumberingSystem ns = (NumberingSystem) macros.symbols;
                if (ns.getName().equals("latn")) {
                    sb.append("latin");
                } else {
                    sb.append("numbering-system/");
                    BlueprintHelpers.generateNumberingSystemOption(ns, sb);
                }
                return true;
            } else {
                assert macros.symbols instanceof DecimalFormatSymbols;
                throw new UnsupportedOperationException(
                        "Cannot generate number skeleton with custom DecimalFormatSymbols");
            }
        }

        private static boolean unitWidth(MacroProps macros, StringBuilder sb) {
            if (macros.unitWidth == UnitWidth.SHORT) {
                return false; // Default value
            }
            EnumToStemString.unitWidth(macros.unitWidth, sb);
            return true;
        }

        private static boolean sign(MacroProps macros, StringBuilder sb) {
            if (macros.sign == SignDisplay.AUTO) {
                return false; // Default value
            }
            EnumToStemString.signDisplay(macros.sign, sb);
            return true;
        }

        private static boolean decimal(MacroProps macros, StringBuilder sb) {
            if (macros.decimal == DecimalSeparatorDisplay.AUTO) {
                return false; // Default value
            }
            EnumToStemString.decimalSeparatorDisplay(macros.decimal, sb);
            return true;
        }

    }

    ///// OTHER UTILITY FUNCTIONS /////

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
}
