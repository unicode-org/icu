// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;

/**
 * @author sffc
 *
 */
class NumberSkeletonImpl {

    static enum StemType {
        OTHER, ROUNDER, FRACTION_ROUNDER, MAYBE_INCREMENT_ROUNDER, CURRENCY_ROUNDER, MEASURE_UNIT, UNIT_WIDTH
    }

    static class SkeletonDataStructure {
        final Map<String, StemType> stemsToTypes;
        final Map<String, Object> stemsToValues;
        final Map<Object, String> valuesToStems;

        SkeletonDataStructure() {
            stemsToTypes = new HashMap<String, StemType>();
            stemsToValues = new HashMap<String, Object>();
            valuesToStems = new HashMap<Object, String>();
        }

        public void put(StemType stemType, String content, Object value) {
            stemsToTypes.put(content, stemType);
            stemsToValues.put(content, value);
            valuesToStems.put(value, content);
        }

        public StemType stemToType(CharSequence content) {
            return stemsToTypes.get(content);
        }

        public Object stemToValue(CharSequence content) {
            return stemsToValues.get(content);
        }

        public String valueToStem(Object value) {
            return valuesToStems.get(value);
        }
    }

    static final SkeletonDataStructure skeletonData = new SkeletonDataStructure();

    static {
        skeletonData.put(StemType.ROUNDER, "round-integer", Rounder.integer());
        skeletonData.put(StemType.ROUNDER, "round-unlimited", Rounder.unlimited());
        skeletonData.put(StemType.ROUNDER,
                "round-currency-standard",
                Rounder.currency(CurrencyUsage.STANDARD));
        skeletonData.put(StemType.ROUNDER, "round-currency-cash", Rounder.currency(CurrencyUsage.CASH));

        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-narrow", UnitWidth.NARROW);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-short", UnitWidth.SHORT);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-full-name", UnitWidth.FULL_NAME);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-iso-code", UnitWidth.ISO_CODE);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-hidden", UnitWidth.HIDDEN);
    }

    private static final Map<String, UnlocalizedNumberFormatter> cache = new ConcurrentHashMap<String, UnlocalizedNumberFormatter>();

    /**
     * Gets the number formatter for the given number skeleton string from the cache, creating it if it
     * does not exist in the cache.
     *
     * @param skeletonString
     *            A number skeleton string, possibly not in its shortest form.
     * @return An UnlocalizedNumberFormatter with behavior defined by the given skeleton string.
     */
    public static UnlocalizedNumberFormatter getOrCreate(String skeletonString) {
        String unNormalized = skeletonString; // more appropriate variable name for the implementation

        // First try: look up the un-normalized skeleton.
        UnlocalizedNumberFormatter formatter = cache.get(unNormalized);
        if (formatter != null) {
            return formatter;
        }

        // Second try: normalize the skeleton, and then access the cache.
        // Store the un-normalized form for a faster lookup next time.
        // Synchronize because we need a transaction with multiple queries to the cache.
        String normalized = normalizeSkeleton(unNormalized);
        if (cache.containsKey(normalized)) {
            synchronized (cache) {
                formatter = cache.get(normalized);
                if (formatter != null) {
                    cache.putIfAbsent(unNormalized, formatter);
                }
            }
        }
        if (formatter != null) {
            return formatter;
        }

        // Third try: create the formatter, store it in the cache, and return it.
        formatter = create(normalized);

        // Synchronize because we need a transaction with multiple queries to the cache.
        synchronized (cache) {
            if (cache.containsKey(normalized)) {
                formatter = cache.get(normalized);
            } else {
                cache.put(normalized, formatter);
            }
            cache.putIfAbsent(unNormalized, formatter);
        }
        return formatter;
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

    public static String generate(MacroProps macros) {
        StringBuilder sb = new StringBuilder();
        generateSkeleton(macros, sb);
        return sb.toString();
    }

    /**
     * Normalizes a number skeleton string to the shortest equivalent form.
     *
     * @param skeletonString
     *            A number skeleton string, possibly not in its shortest form.
     * @return An equivalent and possibly simplified skeleton string.
     */
    public static String normalizeSkeleton(String skeletonString) {
        // FIXME
        return skeletonString;
    }

    /////

    private static MacroProps parseSkeleton(String skeletonString) {
        MacroProps macros = new MacroProps();
        StringSegment segment = new StringSegment(skeletonString + " ", false);
        StemType stem = null;
        int offset = 0;
        while (offset < segment.length()) {
            int cp = segment.codePointAt(offset);
            boolean isWhiteSpace = PatternProps.isWhiteSpace(cp);
            if (offset > 0 && (isWhiteSpace || cp == '/')) {
                segment.setLength(offset);
                if (stem == null) {
                    stem = parseStem(segment, macros);
                } else {
                    stem = parseOption(stem, segment, macros);
                }
                segment.resetLength();
                segment.adjustOffset(offset + 1);
                offset = 0;
            } else {
                offset += Character.charCount(cp);
            }
            if (isWhiteSpace && stem != null) {
                // Check for stems that require an option
                switch (stem) {
                case MAYBE_INCREMENT_ROUNDER:
                case MEASURE_UNIT:
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

    private static StemType parseStem(CharSequence content, MacroProps macros) {
        // First try: exact match with a literal stem
        StemType stem = skeletonData.stemToType(content);
        if (stem != null) {
            Object value = skeletonData.stemToValue(content);
            switch (stem) {
            case ROUNDER:
                checkNull(macros.rounder, content);
                macros.rounder = (Rounder) value;
                break;
            case UNIT_WIDTH:
                checkNull(macros.unitWidth, content);
                macros.unitWidth = (UnitWidth) value;
                break;
            default:
                assert false;
            }
            return stem;
        }

        // Second try: literal stems that require an option
        if (content.equals("round-increment")) {
            return StemType.MAYBE_INCREMENT_ROUNDER;
        } else if (content.equals("measure-unit")) {
            return StemType.MEASURE_UNIT;
        }

        // Second try: stem "blueprint" syntax
        switch (content.charAt(0)) {
        case '.':
            stem = StemType.FRACTION_ROUNDER;
            parseFractionStem(content, macros);
            break;
        case '@':
            stem = StemType.ROUNDER;
            parseDigitsStem(content, macros);
            break;
        }
        if (stem != null) {
            return stem;
        }

        // Still no hits: throw an exception
        throw new SkeletonSyntaxException("Unknown stem", content);
    }

    private static StemType parseOption(StemType stem, CharSequence content, MacroProps macros) {
        // Frac-sig option
        switch (stem) {
        case FRACTION_ROUNDER:
            if (parseFracSigOption(content, macros)) {
                return StemType.ROUNDER;
            }
        }

        // Increment option
        switch (stem) {
        case MAYBE_INCREMENT_ROUNDER:
            // The increment option is required.
            parseIncrementOption(content, macros);
            return StemType.ROUNDER;
        }

        // Rounding mode option
        switch (stem) {
        case ROUNDER:
        case FRACTION_ROUNDER:
        case CURRENCY_ROUNDER:
            if (parseRoundingModeOption(content, macros)) {
                break;
            }
        }

        // Measure unit option
        switch (stem) {
        case MEASURE_UNIT:
            // The measure unit option is required.
            parseMeasureUnitOption(content, macros);
            return StemType.OTHER;
        }

        // Unknown option
        throw new SkeletonSyntaxException("Unknown option", content);
    }

    /////

    private static void generateSkeleton(MacroProps macros, StringBuilder sb) {
        if (macros.rounder != null) {
            generateRoundingValue(macros, sb);
            sb.append(' ');
        }
        if (macros.unit != null) {
            generateUnitValue(macros, sb);
            sb.append(' ');
        }
        if (macros.unitWidth != null) {
            generateUnitWidthValue(macros, sb);
            sb.append(' ');
        }

        // Remove the trailing space
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
    }

    /////

    private static void parseFractionStem(CharSequence content, MacroProps macros) {
        assert content.charAt(0) == '.';
        int offset = 1;
        int minFrac = 0;
        int maxFrac;
        for (; offset < content.length(); offset++) {
            if (content.charAt(offset) == '0') {
                minFrac++;
            } else {
                break;
            }
        }
        if (offset < content.length()) {
            if (content.charAt(offset) == '+') {
                maxFrac = -1;
                offset++;
            } else {
                maxFrac = minFrac;
                for (; offset < content.length(); offset++) {
                    if (content.charAt(offset) == '#') {
                        maxFrac++;
                    } else {
                        break;
                    }
                }
            }
        } else {
            maxFrac = minFrac;
        }
        if (offset < content.length()) {
            throw new SkeletonSyntaxException("Invalid fraction stem", content);
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

    private static void parseDigitsStem(CharSequence content, MacroProps macros) {
        assert content.charAt(0) == '@';
        int offset = 0;
        int minSig = 0;
        int maxSig;
        for (; offset < content.length(); offset++) {
            if (content.charAt(offset) == '@') {
                minSig++;
            } else {
                break;
            }
        }
        if (offset < content.length()) {
            if (content.charAt(offset) == '+') {
                maxSig = -1;
                offset++;
            } else {
                maxSig = minSig;
                for (; offset < content.length(); offset++) {
                    if (content.charAt(offset) == '#') {
                        maxSig++;
                    } else {
                        break;
                    }
                }
            }
        } else {
            maxSig = minSig;
        }
        if (offset < content.length()) {
            throw new SkeletonSyntaxException("Invalid significant digits stem", content);
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

    private static boolean parseFracSigOption(CharSequence content, MacroProps macros) {
        if (content.charAt(0) != '@') {
            return false;
        }
        FractionRounder oldRounder = (FractionRounder) macros.rounder;
        // A little bit of a hack: parse the option as a digits stem, and extract the min/max sig from
        // the new Rounder saved into the macros
        parseDigitsStem(content, macros);
        Rounder.SignificantRounderImpl intermediate = (Rounder.SignificantRounderImpl) macros.rounder;
        if (intermediate.maxSig == -1) {
            macros.rounder = oldRounder.withMinDigits(intermediate.minSig);
        } else {
            macros.rounder = oldRounder.withMaxDigits(intermediate.maxSig);
        }
        return true;
    }

    private static void parseIncrementOption(CharSequence content, MacroProps macros) {
        // Call content.subSequence() because content.toString() doesn't create a clean string.
        String str = content.subSequence(0, content.length()).toString();
        BigDecimal increment;
        try {
            increment = new BigDecimal(str);
        } catch (NumberFormatException e) {
            throw new SkeletonSyntaxException("Invalid rounding increment", content, e);
        }
        macros.rounder = Rounder.increment(increment);
    }

    private static void generateIncrementOption(BigDecimal increment, StringBuilder sb) {
        sb.append(increment.toPlainString());
    }

    private static boolean parseRoundingModeOption(CharSequence content, MacroProps macros) {
        // Iterate over int modes instead of enum modes for performance
        for (int rm = 0; rm <= BigDecimal.ROUND_UNNECESSARY; rm++) {
            RoundingMode mode = RoundingMode.valueOf(rm);
            if (content.equals(mode.toString())) {
                macros.rounder = macros.rounder.withMode(mode);
                return true;
            }
        }
        return false;
    }

    private static void generateRoundingModeOption(RoundingMode mode, StringBuilder sb) {
        sb.append(mode.toString());
    }

    private static void parseMeasureUnitOption(CharSequence content, MacroProps macros) {
        // NOTE: The category (type) of the unit is guaranteed to be a valid subtag (alphanumeric)
        // http://unicode.org/reports/tr35/#Validity_Data
        int firstHyphen = 0;
        while (firstHyphen < content.length() && content.charAt(firstHyphen) != '-') {
            firstHyphen++;
        }
        String type = content.subSequence(0, firstHyphen).toString();
        String subType = content.subSequence(firstHyphen + 1, content.length()).toString();
        Set<MeasureUnit> units = MeasureUnit.getAvailable(type);
        for (MeasureUnit unit : units) {
            if (subType.equals(unit.getSubtype())) {
                macros.unit = unit;
                return;
            }
        }
        throw new SkeletonSyntaxException("Unknown unit", content);
    }

    private static void generateMeasureUnitOption(MeasureUnit unit, StringBuilder sb) {
        sb.append(unit.getType() + "-" + unit.getSubtype());
    }

    /////

    private static void generateRoundingValue(MacroProps macros, StringBuilder sb) {
        // Check for literals
        String literal = skeletonData.valueToStem(macros.rounder);
        if (literal != null) {
            sb.append(literal);
            return;
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
    }

    private static void generateUnitValue(MacroProps macros, StringBuilder sb) {
        // Check for literals
        String literal = skeletonData.valueToStem(macros.unit);
        if (literal != null) {
            sb.append(literal);
            return;
        }

        // Generate the stem
        if (macros.unit instanceof Currency) {
            // TODO
        } else if (macros.unit instanceof NoUnit) {
            // TODO
        } else {
            sb.append("measure-unit/");
            generateMeasureUnitOption(macros.unit, sb);
        }
    }

    private static void generateUnitWidthValue(MacroProps macros, StringBuilder sb) {
        // There should be a literal.
        String literal = skeletonData.valueToStem(macros.unitWidth);
        assert literal != null;
        sb.append(literal);
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
}
