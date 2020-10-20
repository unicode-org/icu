// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import com.ibm.icu.impl.CacheBase;
import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.SoftCache;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.SingleUnitImpl;
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

    //////////////////////////////////////////////////////////////////////////////////////////
    // NOTE: For examples of how to add a new stem to the number skeleton parser, see:      //
    // https://github.com/unicode-org/icu/commit/a2a7982216b2348070dc71093775ac7195793d73   //
    // and                                                                                  //
    // https://github.com/unicode-org/icu/commit/6fe86f3934a8a5701034f648a8f7c5087e84aa28   //
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * While parsing a skeleton, this enum records what type of option we expect to find next.
     */
    static enum ParseState {
        // Section 0: We expect whitespace or a stem, but not an option:
        STATE_NULL,

        // Section 1: We might accept an option, but it is not required:
        STATE_SCIENTIFIC,
        STATE_FRACTION_PRECISION,

        // Section 2: An option is required:
        STATE_INCREMENT_PRECISION,
        STATE_MEASURE_UNIT,
        STATE_PER_MEASURE_UNIT,
        STATE_IDENTIFIER_UNIT,
        STATE_UNIT_USAGE,
        STATE_CURRENCY_UNIT,
        STATE_INTEGER_WIDTH,
        STATE_NUMBERING_SYSTEM,
        STATE_SCALE,
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
        STEM_PERCENT_100, // concise-only
        STEM_PRECISION_INTEGER,
        STEM_PRECISION_UNLIMITED,
        STEM_PRECISION_CURRENCY_STANDARD,
        STEM_PRECISION_CURRENCY_CASH,
        STEM_ROUNDING_MODE_CEILING,
        STEM_ROUNDING_MODE_FLOOR,
        STEM_ROUNDING_MODE_DOWN,
        STEM_ROUNDING_MODE_UP,
        STEM_ROUNDING_MODE_HALF_EVEN,
        STEM_ROUNDING_MODE_HALF_DOWN,
        STEM_ROUNDING_MODE_HALF_UP,
        STEM_ROUNDING_MODE_UNNECESSARY,
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
        STEM_UNIT_WIDTH_FORMAL,
        STEM_UNIT_WIDTH_VARIANT,
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
        STEM_PRECISION_INCREMENT,
        STEM_MEASURE_UNIT,
        STEM_PER_MEASURE_UNIT,
        STEM_UNIT,
        STEM_UNIT_USAGE,
        STEM_CURRENCY,
        STEM_INTEGER_WIDTH,
        STEM_NUMBERING_SYSTEM,
        STEM_SCALE,
    };

    /** Default wildcard char, accepted on input and printed in output */
    static final char WILDCARD_CHAR = '*';

    /** Alternative wildcard char, accept on input but not printed in output */
    static final char ALT_WILDCARD_CHAR = '+';

    /** Checks whether the char is a wildcard on input */
    static boolean isWildcardChar(char c) {
        return c == WILDCARD_CHAR || c == ALT_WILDCARD_CHAR;
    }

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
        b.add("precision-integer", StemEnum.STEM_PRECISION_INTEGER.ordinal());
        b.add("precision-unlimited", StemEnum.STEM_PRECISION_UNLIMITED.ordinal());
        b.add("precision-currency-standard", StemEnum.STEM_PRECISION_CURRENCY_STANDARD.ordinal());
        b.add("precision-currency-cash", StemEnum.STEM_PRECISION_CURRENCY_CASH.ordinal());
        b.add("rounding-mode-ceiling", StemEnum.STEM_ROUNDING_MODE_CEILING.ordinal());
        b.add("rounding-mode-floor", StemEnum.STEM_ROUNDING_MODE_FLOOR.ordinal());
        b.add("rounding-mode-down", StemEnum.STEM_ROUNDING_MODE_DOWN.ordinal());
        b.add("rounding-mode-up", StemEnum.STEM_ROUNDING_MODE_UP.ordinal());
        b.add("rounding-mode-half-even", StemEnum.STEM_ROUNDING_MODE_HALF_EVEN.ordinal());
        b.add("rounding-mode-half-down", StemEnum.STEM_ROUNDING_MODE_HALF_DOWN.ordinal());
        b.add("rounding-mode-half-up", StemEnum.STEM_ROUNDING_MODE_HALF_UP.ordinal());
        b.add("rounding-mode-unnecessary", StemEnum.STEM_ROUNDING_MODE_UNNECESSARY.ordinal());
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
        b.add("unit-width-formal", StemEnum.STEM_UNIT_WIDTH_FORMAL.ordinal());
        b.add("unit-width-variant", StemEnum.STEM_UNIT_WIDTH_VARIANT.ordinal());
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
        b.add("precision-increment", StemEnum.STEM_PRECISION_INCREMENT.ordinal());
        b.add("measure-unit", StemEnum.STEM_MEASURE_UNIT.ordinal());
        b.add("per-measure-unit", StemEnum.STEM_PER_MEASURE_UNIT.ordinal());
        b.add("unit", StemEnum.STEM_UNIT.ordinal());
        b.add("usage", StemEnum.STEM_UNIT_USAGE.ordinal());
        b.add("currency", StemEnum.STEM_CURRENCY.ordinal());
        b.add("integer-width", StemEnum.STEM_INTEGER_WIDTH.ordinal());
        b.add("numbering-system", StemEnum.STEM_NUMBERING_SYSTEM.ordinal());
        b.add("scale", StemEnum.STEM_SCALE.ordinal());

        // Section 3 (concise tokens):
        b.add("K", StemEnum.STEM_COMPACT_SHORT.ordinal());
        b.add("KK", StemEnum.STEM_COMPACT_LONG.ordinal());
        b.add("%", StemEnum.STEM_PERCENT.ordinal());
        b.add("%x100", StemEnum.STEM_PERCENT_100.ordinal());
        b.add(",_", StemEnum.STEM_GROUP_OFF.ordinal());
        b.add(",?", StemEnum.STEM_GROUP_MIN2.ordinal());
        b.add(",!", StemEnum.STEM_GROUP_ON_ALIGNED.ordinal());
        b.add("+!", StemEnum.STEM_SIGN_ALWAYS.ordinal());
        b.add("+_", StemEnum.STEM_SIGN_NEVER.ordinal());
        b.add("()", StemEnum.STEM_SIGN_ACCOUNTING.ordinal());
        b.add("()!", StemEnum.STEM_SIGN_ACCOUNTING_ALWAYS.ordinal());
        b.add("+?", StemEnum.STEM_SIGN_EXCEPT_ZERO.ordinal());
        b.add("()?", StemEnum.STEM_SIGN_ACCOUNTING_EXCEPT_ZERO.ordinal());

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

        private static Precision precision(StemEnum stem) {
            switch (stem) {
            case STEM_PRECISION_INTEGER:
                return Precision.integer();
            case STEM_PRECISION_UNLIMITED:
                return Precision.unlimited();
            case STEM_PRECISION_CURRENCY_STANDARD:
                return Precision.currency(CurrencyUsage.STANDARD);
            case STEM_PRECISION_CURRENCY_CASH:
                return Precision.currency(CurrencyUsage.CASH);
            default:
                throw new AssertionError();
            }
        }

        private static RoundingMode roundingMode(StemEnum stem) {
            switch (stem) {
            case STEM_ROUNDING_MODE_CEILING:
                return RoundingMode.CEILING;
            case STEM_ROUNDING_MODE_FLOOR:
                return RoundingMode.FLOOR;
            case STEM_ROUNDING_MODE_DOWN:
                return RoundingMode.DOWN;
            case STEM_ROUNDING_MODE_UP:
                return RoundingMode.UP;
            case STEM_ROUNDING_MODE_HALF_EVEN:
                return RoundingMode.HALF_EVEN;
            case STEM_ROUNDING_MODE_HALF_DOWN:
                return RoundingMode.HALF_DOWN;
            case STEM_ROUNDING_MODE_HALF_UP:
                return RoundingMode.HALF_UP;
            case STEM_ROUNDING_MODE_UNNECESSARY:
                return RoundingMode.UNNECESSARY;
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
            case STEM_UNIT_WIDTH_FORMAL:
                return UnitWidth.FORMAL;
            case STEM_UNIT_WIDTH_VARIANT:
                return UnitWidth.VARIANT;
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

        private static void roundingMode(RoundingMode value, StringBuilder sb) {
            switch (value) {
            case CEILING:
                sb.append("rounding-mode-ceiling");
                break;
            case FLOOR:
                sb.append("rounding-mode-floor");
                break;
            case DOWN:
                sb.append("rounding-mode-down");
                break;
            case UP:
                sb.append("rounding-mode-up");
                break;
            case HALF_EVEN:
                sb.append("rounding-mode-half-even");
                break;
            case HALF_DOWN:
                sb.append("rounding-mode-half-down");
                break;
            case HALF_UP:
                sb.append("rounding-mode-half-up");
                break;
            case UNNECESSARY:
                sb.append("rounding-mode-unnecessary");
                break;
            default:
                throw new AssertionError();
            }
        }

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
            case FORMAL:
                sb.append("unit-width-formal");
                break;
            case VARIANT:
                sb.append("unit-width-variant");
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
                case STATE_INCREMENT_PRECISION:
                case STATE_MEASURE_UNIT:
                case STATE_PER_MEASURE_UNIT:
                case STATE_IDENTIFIER_UNIT:
                case STATE_UNIT_USAGE:
                case STATE_CURRENCY_UNIT:
                case STATE_INTEGER_WIDTH:
                case STATE_NUMBERING_SYSTEM:
                case STATE_SCALE:
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
     * Given that the current segment represents a stem, parse it and save the result.
     *
     * @return The next state after parsing this stem, corresponding to what subset of options to expect.
     */
    private static ParseState parseStem(StringSegment segment, CharsTrie stemTrie, MacroProps macros) {
        // First check for "blueprint" stems, which start with a "signal char"
        switch (segment.charAt(0)) {
        case '.':
            checkNull(macros.precision, segment);
            BlueprintHelpers.parseFractionStem(segment, macros);
            return ParseState.STATE_FRACTION_PRECISION;
        case '@':
            checkNull(macros.precision, segment);
            BlueprintHelpers.parseDigitsStem(segment, macros);
            return ParseState.STATE_NULL;
        case 'E':
            checkNull(macros.notation, segment);
            BlueprintHelpers.parseScientificStem(segment, macros);
            return ParseState.STATE_NULL;
        case '0':
            checkNull(macros.integerWidth, segment);
            BlueprintHelpers.parseIntegerStem(segment, macros);
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

        case STEM_PERCENT_100:
            checkNull(macros.scale, segment);
            checkNull(macros.unit, segment);
            macros.scale = Scale.powerOfTen(2);
            macros.unit = NoUnit.PERCENT;
            return ParseState.STATE_NULL;

        case STEM_PRECISION_INTEGER:
        case STEM_PRECISION_UNLIMITED:
        case STEM_PRECISION_CURRENCY_STANDARD:
        case STEM_PRECISION_CURRENCY_CASH:
            checkNull(macros.precision, segment);
            macros.precision = StemToObject.precision(stem);
            switch (stem) {
            case STEM_PRECISION_INTEGER:
                return ParseState.STATE_FRACTION_PRECISION; // allows for "precision-integer/@##"
            default:
                return ParseState.STATE_NULL;
            }

        case STEM_ROUNDING_MODE_CEILING:
        case STEM_ROUNDING_MODE_FLOOR:
        case STEM_ROUNDING_MODE_DOWN:
        case STEM_ROUNDING_MODE_UP:
        case STEM_ROUNDING_MODE_HALF_EVEN:
        case STEM_ROUNDING_MODE_HALF_DOWN:
        case STEM_ROUNDING_MODE_HALF_UP:
        case STEM_ROUNDING_MODE_UNNECESSARY:
            checkNull(macros.roundingMode, segment);
            macros.roundingMode = StemToObject.roundingMode(stem);
            return ParseState.STATE_NULL;

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
        case STEM_UNIT_WIDTH_FORMAL:
        case STEM_UNIT_WIDTH_VARIANT:
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

        case STEM_PRECISION_INCREMENT:
            checkNull(macros.precision, segment);
            return ParseState.STATE_INCREMENT_PRECISION;

        case STEM_MEASURE_UNIT:
            checkNull(macros.unit, segment);
            return ParseState.STATE_MEASURE_UNIT;

        case STEM_PER_MEASURE_UNIT:
            // In C++, STEM_CURRENCY's checks mark perUnit as "seen". Here we do
            // the inverse: checking that macros.unit is not set to a currency.
            if (macros.unit instanceof Currency) {
                throw new SkeletonSyntaxException("Duplicated setting", segment);
            }
            checkNull(macros.perUnit, segment);
            return ParseState.STATE_PER_MEASURE_UNIT;

        case STEM_UNIT:
            checkNull(macros.unit, segment);
            checkNull(macros.perUnit, segment);
            return ParseState.STATE_IDENTIFIER_UNIT;

        case STEM_UNIT_USAGE:
            checkNull(macros.usage, segment);
            return ParseState.STATE_UNIT_USAGE;

        case STEM_CURRENCY:
            checkNull(macros.unit, segment);
            checkNull(macros.perUnit, segment);
            return ParseState.STATE_CURRENCY_UNIT;

        case STEM_INTEGER_WIDTH:
            checkNull(macros.integerWidth, segment);
            return ParseState.STATE_INTEGER_WIDTH;

        case STEM_NUMBERING_SYSTEM:
            checkNull(macros.symbols, segment);
            return ParseState.STATE_NUMBERING_SYSTEM;

        case STEM_SCALE:
            checkNull(macros.scale, segment);
            return ParseState.STATE_SCALE;

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
        case STATE_IDENTIFIER_UNIT:
            BlueprintHelpers.parseIdentifierUnitOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_UNIT_USAGE:
            BlueprintHelpers.parseUnitUsageOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_INCREMENT_PRECISION:
            BlueprintHelpers.parseIncrementOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_INTEGER_WIDTH:
            BlueprintHelpers.parseIntegerWidthOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_NUMBERING_SYSTEM:
            BlueprintHelpers.parseNumberingSystemOption(segment, macros);
            return ParseState.STATE_NULL;
        case STATE_SCALE:
            BlueprintHelpers.parseScaleOption(segment, macros);
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
        case STATE_FRACTION_PRECISION:
            if (BlueprintHelpers.parseFracSigOption(segment, macros)) {
                return ParseState.STATE_NULL;
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
        if (macros.usage != null && GeneratorHelpers.usage(macros, sb)) {
            sb.append(' ');
        }
        if (macros.precision != null && GeneratorHelpers.precision(macros, sb)) {
            sb.append(' ');
        }
        if (macros.roundingMode != null && GeneratorHelpers.roundingMode(macros, sb)) {
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
        if (macros.scale != null && GeneratorHelpers.scale(macros, sb)) {
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
            if (!isWildcardChar(segment.charAt(0))) {
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
            sb.append(WILDCARD_CHAR);
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

        // "measure-unit/" is deprecated in favour of "unit/".
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

        // "per-measure-unit/" is deprecated in favour of "unit/".
        private static void parseMeasurePerUnitOption(StringSegment segment, MacroProps macros) {
            // A little bit of a hack: save the current unit (numerator), call the main measure unit
            // parsing code, put back the numerator unit, and put the new unit into per-unit.
            MeasureUnit numerator = macros.unit;
            parseMeasureUnitOption(segment, macros);
            macros.perUnit = macros.unit;
            macros.unit = numerator;
        }

        /**
         * Parses unit identifiers like "meter-per-second" and "foot-and-inch", as
         * specified via a "unit/" concise skeleton.
         */
        private static void parseIdentifierUnitOption(StringSegment segment, MacroProps macros) {
            MeasureUnitImpl fullUnit;
            try {
                fullUnit = MeasureUnitImpl.forIdentifier(segment.asString());
            } catch (IllegalArgumentException e) {
                throw new SkeletonSyntaxException("Invalid unit stem", segment);
            }

            // Mixed units can only be represented by full MeasureUnit instances, so we
            // don't split the denominator into macros.perUnit.
            if (fullUnit.getComplexity() == MeasureUnit.Complexity.MIXED) {
                macros.unit = fullUnit.build();
                return;
            }

            // When we have a built-in unit (e.g. meter-per-second), we don't split it up
            MeasureUnit testBuiltin = fullUnit.build();
            if (testBuiltin.getType() != null) {
                macros.unit = testBuiltin;
                return;
            }

            // TODO(ICU-20941): Clean this up.
            for (SingleUnitImpl subUnit : fullUnit.getSingleUnits()) {
                if (subUnit.getDimensionality() > 0) {
                    if (macros.unit == null) {
                        macros.unit = subUnit.build();
                    } else {
                        macros.unit = macros.unit.product(subUnit.build());
                    }
                } else {
                    // It's okay to mutate fullUnit, we're throwing it away after this:
                    subUnit.setDimensionality(subUnit.getDimensionality() * -1);
                    if (macros.perUnit == null) {
                        macros.perUnit = subUnit.build();
                    } else {
                        macros.perUnit = macros.perUnit.product(subUnit.build());
                    }
                }
            }
        }

        private static void parseUnitUsageOption(StringSegment segment, MacroProps macros) {
            macros.usage = segment.asString();
            // We do not do any validation of the usage string: it depends on the
            // unitPreferenceData in the units resources.
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
                if (isWildcardChar(segment.charAt(offset))) {
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
                if (minFrac == 0) {
                    macros.precision = Precision.unlimited();
                } else {
                    macros.precision = Precision.minFraction(minFrac);
                }
            } else {
                macros.precision = Precision.minMaxFraction(minFrac, maxFrac);
            }
        }

        private static void generateFractionStem(int minFrac, int maxFrac, StringBuilder sb) {
            if (minFrac == 0 && maxFrac == 0) {
                sb.append("precision-integer");
                return;
            }
            sb.append('.');
            appendMultiple(sb, '0', minFrac);
            if (maxFrac == -1) {
                sb.append(WILDCARD_CHAR);
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
                if (isWildcardChar(segment.charAt(offset))) {
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
                macros.precision = Precision.minSignificantDigits(minSig);
            } else {
                macros.precision = Precision.minMaxSignificantDigits(minSig, maxSig);
            }
        }

        private static void generateDigitsStem(int minSig, int maxSig, StringBuilder sb) {
            appendMultiple(sb, '@', minSig);
            if (maxSig == -1) {
                sb.append(WILDCARD_CHAR);
            } else {
                appendMultiple(sb, '#', maxSig - minSig);
            }
        }

        private static void parseScientificStem(StringSegment segment, MacroProps macros) {
            assert(segment.charAt(0) == 'E');
            block:
            {
                int offset = 1;
                if (segment.length() == offset) {
                    break block;
                }
                boolean isEngineering = false;
                if (segment.charAt(offset) == 'E') {
                    isEngineering = true;
                    offset++;
                    if (segment.length() == offset) {
                        break block;
                    }
                }
                SignDisplay signDisplay = SignDisplay.AUTO;
                if (segment.charAt(offset) == '+') {
                    offset++;
                    if (segment.length() == offset) {
                        break block;
                    }
                    if (segment.charAt(offset) == '!') {
                        signDisplay = SignDisplay.ALWAYS;
                    } else if (segment.charAt(offset) == '?') {
                        signDisplay = SignDisplay.EXCEPT_ZERO;
                    } else {
                        break block;
                    }
                    offset++;
                    if (segment.length() == offset) {
                        break block;
                    }
                }
                int minDigits = 0;
                for (; offset < segment.length(); offset++) {
                    if (segment.charAt(offset) != '0') {
                        break block;
                    }
                    minDigits++;
                }
                macros.notation = (isEngineering ? Notation.engineering() : Notation.scientific())
                    .withExponentSignDisplay(signDisplay)
                    .withMinExponentDigits(minDigits);
                return;
            }
            throw new SkeletonSyntaxException("Invalid scientific stem", segment);
        }

        private static void parseIntegerStem(StringSegment segment, MacroProps macros) {
            assert(segment.charAt(0) == '0');
            int offset = 1;
            for (; offset < segment.length(); offset++) {
                if (segment.charAt(offset) != '0') {
                    offset--;
                    break;
                }
            }
            if (offset < segment.length()) {
                 throw new SkeletonSyntaxException("Invalid integer stem", segment);
            }
            macros.integerWidth = IntegerWidth.zeroFillTo(offset);
            return;
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
                if (isWildcardChar(segment.charAt(offset))) {
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

            FractionPrecision oldRounder = (FractionPrecision) macros.precision;
            if (maxSig == -1) {
                macros.precision = oldRounder.withMinDigits(minSig);
            } else {
                macros.precision = oldRounder.withMaxDigits(maxSig);
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
            macros.precision = Precision.increment(increment);
        }

        private static void generateIncrementOption(BigDecimal increment, StringBuilder sb) {
            sb.append(increment.toPlainString());
        }

        private static void parseIntegerWidthOption(StringSegment segment, MacroProps macros) {
            int offset = 0;
            int minInt = 0;
            int maxInt;
            if (isWildcardChar(segment.charAt(0))) {
                maxInt = -1;
                offset++;
            } else {
                maxInt = 0;
            }
            for (; offset < segment.length(); offset++) {
                if (maxInt != -1 && segment.charAt(offset) == '#') {
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
                sb.append(WILDCARD_CHAR);
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

        private static void parseScaleOption(StringSegment segment, MacroProps macros) {
            // Call segment.subSequence() because segment.toString() doesn't create a clean string.
            String str = segment.subSequence(0, segment.length()).toString();
            BigDecimal bd;
            try {
                bd = new BigDecimal(str);
            } catch (NumberFormatException e) {
                throw new SkeletonSyntaxException("Invalid scale", segment, e);
            }
            // NOTE: If bd is a power of ten, the Scale API optimizes it for us.
            macros.scale = Scale.byBigDecimal(bd);
        }

        private static void generateScaleOption(Scale scale, StringBuilder sb) {
            BigDecimal bd = scale.arbitrary;
            if (bd == null) {
                bd = BigDecimal.ONE;
            }
            bd = bd.scaleByPowerOfTen(scale.magnitude);
            sb.append(bd.toPlainString());
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
            MeasureUnit unit = macros.unit;
            if (macros.perUnit != null) {
                if (macros.unit instanceof Currency || macros.perUnit instanceof Currency) {
                    throw new UnsupportedOperationException(
                        "Cannot generate number skeleton with currency unit and per-unit");
                }
                unit = unit.product(macros.perUnit.reciprocal());
            }
            if (unit instanceof Currency) {
                sb.append("currency/");
                BlueprintHelpers.generateCurrencyOption((Currency)unit, sb);
                return true;
            } else if (unit.equals(MeasureUnit.PERCENT)) {
                sb.append("percent");
                return true;
            } else if (unit.equals(MeasureUnit.PERMILLE)) {
                sb.append("permille");
                return true;
            } else {
                sb.append("unit/");
                sb.append(unit.getIdentifier());
                return true;
            }
        }

        private static boolean usage(MacroProps macros, StringBuilder sb) {
            if (macros.usage != null  && macros.usage.length() > 0) {
                sb.append("usage/");
                sb.append(macros.usage);

                return true;
            }
            return false;
        }

        private static boolean precision(MacroProps macros, StringBuilder sb) {
            if (macros.precision instanceof Precision.InfiniteRounderImpl) {
                sb.append("precision-unlimited");
            } else if (macros.precision instanceof Precision.FractionRounderImpl) {
                Precision.FractionRounderImpl impl = (Precision.FractionRounderImpl) macros.precision;
                BlueprintHelpers.generateFractionStem(impl.minFrac, impl.maxFrac, sb);
            } else if (macros.precision instanceof Precision.SignificantRounderImpl) {
                Precision.SignificantRounderImpl impl = (Precision.SignificantRounderImpl) macros.precision;
                BlueprintHelpers.generateDigitsStem(impl.minSig, impl.maxSig, sb);
            } else if (macros.precision instanceof Precision.FracSigRounderImpl) {
                Precision.FracSigRounderImpl impl = (Precision.FracSigRounderImpl) macros.precision;
                BlueprintHelpers.generateFractionStem(impl.minFrac, impl.maxFrac, sb);
                sb.append('/');
                if (impl.minSig == -1) {
                    BlueprintHelpers.generateDigitsStem(1, impl.maxSig, sb);
                } else {
                    BlueprintHelpers.generateDigitsStem(impl.minSig, -1, sb);
                }
            } else if (macros.precision instanceof Precision.IncrementRounderImpl) {
                Precision.IncrementRounderImpl impl = (Precision.IncrementRounderImpl) macros.precision;
                sb.append("precision-increment/");
                BlueprintHelpers.generateIncrementOption(impl.increment, sb);
            } else {
                assert macros.precision instanceof Precision.CurrencyRounderImpl;
                Precision.CurrencyRounderImpl impl = (Precision.CurrencyRounderImpl) macros.precision;
                if (impl.usage == CurrencyUsage.STANDARD) {
                    sb.append("precision-currency-standard");
                } else {
                    sb.append("precision-currency-cash");
                }
            }

            // NOTE: Always return true for rounding because the default value depends on other options.
            return true;
        }

        private static boolean roundingMode(MacroProps macros, StringBuilder sb) {
            if (macros.roundingMode == RoundingUtils.DEFAULT_ROUNDING_MODE) {
                return false; // Default value
            }
            EnumToStemString.roundingMode(macros.roundingMode, sb);
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

        private static boolean scale(MacroProps macros, StringBuilder sb) {
            if (!macros.scale.isValid()) {
                return false; // Default value
            }
            sb.append("scale/");
            BlueprintHelpers.generateScaleOption(macros.scale, sb);
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
