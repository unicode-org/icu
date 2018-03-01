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
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.NumberingSystem;
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
        skeletonData.put(StemType.COMPACT_NOTATION, "compact-short", Notation.compactShort());
        skeletonData.put(StemType.COMPACT_NOTATION, "compact-long", Notation.compactLong());
        skeletonData.put(StemType.SCIENTIFIC_NOTATION, "scientific", Notation.scientific());
        skeletonData.put(StemType.SCIENTIFIC_NOTATION, "engineering", Notation.engineering());
        skeletonData.put(StemType.SIMPLE_NOTATION, "simple-notation", Notation.simple());

        skeletonData.put(StemType.NO_UNIT, "base-unit", NoUnit.BASE);
        skeletonData.put(StemType.NO_UNIT, "percent", NoUnit.PERCENT);
        skeletonData.put(StemType.NO_UNIT, "permille", NoUnit.PERMILLE);

        skeletonData.put(StemType.ROUNDER, "round-integer", Rounder.integer());
        skeletonData.put(StemType.ROUNDER, "round-unlimited", Rounder.unlimited());
        skeletonData.put(StemType.ROUNDER,
                "round-currency-standard",
                Rounder.currency(CurrencyUsage.STANDARD));
        skeletonData.put(StemType.ROUNDER, "round-currency-cash", Rounder.currency(CurrencyUsage.CASH));

        skeletonData.put(StemType.GROUPING, "group-off", GroupingStrategy.OFF);
        skeletonData.put(StemType.GROUPING, "group-min2", GroupingStrategy.MIN2);
        skeletonData.put(StemType.GROUPING, "group-auto", GroupingStrategy.AUTO);
        skeletonData.put(StemType.GROUPING, "group-on-aligned", GroupingStrategy.ON_ALIGNED);
        skeletonData.put(StemType.GROUPING, "group-thousands", GroupingStrategy.THOUSANDS);

        skeletonData.put(StemType.LATIN, "latin", NumberingSystem.LATIN);

        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-narrow", UnitWidth.NARROW);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-short", UnitWidth.SHORT);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-full-name", UnitWidth.FULL_NAME);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-iso-code", UnitWidth.ISO_CODE);
        skeletonData.put(StemType.UNIT_WIDTH, "unit-width-hidden", UnitWidth.HIDDEN);

        skeletonData.put(StemType.SIGN_DISPLAY, "sign-auto", SignDisplay.AUTO);
        skeletonData.put(StemType.SIGN_DISPLAY, "sign-always", SignDisplay.ALWAYS);
        skeletonData.put(StemType.SIGN_DISPLAY, "sign-never", SignDisplay.NEVER);
        skeletonData.put(StemType.SIGN_DISPLAY, "sign-accounting", SignDisplay.ACCOUNTING);
        skeletonData.put(StemType.SIGN_DISPLAY, "sign-accounting-always", SignDisplay.ACCOUNTING_ALWAYS);
        skeletonData.put(StemType.SIGN_DISPLAY, "sign-except-zero", SignDisplay.EXCEPT_ZERO);
        skeletonData.put(StemType.SIGN_DISPLAY,
                "sign-accounting-except-zero",
                SignDisplay.ACCOUNTING_EXCEPT_ZERO);

        skeletonData.put(StemType.DECIMAL_DISPLAY, "decimal-auto", DecimalSeparatorDisplay.AUTO);
        skeletonData.put(StemType.DECIMAL_DISPLAY, "decimal-always", DecimalSeparatorDisplay.ALWAYS);
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

    private static StemType parseStem(CharSequence content, MacroProps macros) {
        // First try: exact match with a literal stem
        StemType stem = skeletonData.stemToType(content);
        if (stem != null) {
            Object value = skeletonData.stemToValue(content);
            switch (stem) {
            case COMPACT_NOTATION:
            case SCIENTIFIC_NOTATION:
            case SIMPLE_NOTATION:
                checkNull(macros.notation, content);
                macros.notation = (Notation) value;
                break;
            case NO_UNIT:
                checkNull(macros.unit, content);
                macros.unit = (NoUnit) value;
                break;
            case ROUNDER:
                checkNull(macros.rounder, content);
                macros.rounder = (Rounder) value;
                break;
            case GROUPING:
                checkNull(macros.grouping, content);
                macros.grouping = value;
                break;
            case LATIN:
                checkNull(macros.symbols, content);
                macros.symbols = value;
                break;
            case UNIT_WIDTH:
                checkNull(macros.unitWidth, content);
                macros.unitWidth = (UnitWidth) value;
                break;
            case SIGN_DISPLAY:
                checkNull(macros.sign, content);
                macros.sign = (SignDisplay) value;
                break;
            case DECIMAL_DISPLAY:
                checkNull(macros.decimal, content);
                macros.decimal = (DecimalSeparatorDisplay) value;
                break;
            default:
                assert false;
            }
            return stem;
        }

        // Second try: literal stems that require an option
        if (content.equals("round-increment")) {
            checkNull(macros.rounder, content);
            return StemType.MAYBE_INCREMENT_ROUNDER;
        } else if (content.equals("measure-unit")) {
            checkNull(macros.unit, content);
            return StemType.MEASURE_UNIT;
        } else if (content.equals("per-measure-unit")) {
            checkNull(macros.perUnit, content);
            return StemType.PER_MEASURE_UNIT;
        } else if (content.equals("currency")) {
            checkNull(macros.unit, content);
            return StemType.CURRENCY;
        } else if (content.equals("integer-width")) {
            checkNull(macros.integerWidth, content);
            return StemType.INTEGER_WIDTH;
        } else if (content.equals("numbering-system")) {
            checkNull(macros.symbols, content);
            return StemType.NUMBERING_SYSTEM;
        }

        // Third try: stem "blueprint" syntax
        switch (content.charAt(0)) {
        case '.':
            stem = StemType.FRACTION_ROUNDER;
            checkNull(macros.rounder, content);
            parseFractionStem(content, macros);
            break;
        case '@':
            stem = StemType.ROUNDER;
            checkNull(macros.rounder, content);
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

        ///// Required options: /////

        switch (stem) {
        case CURRENCY:
            parseCurrencyOption(content, macros);
            return StemType.OTHER;
        case MEASURE_UNIT:
            parseMeasureUnitOption(content, macros);
            return StemType.OTHER;
        case PER_MEASURE_UNIT:
            parseMeasurePerUnitOption(content, macros);
            return StemType.OTHER;
        case MAYBE_INCREMENT_ROUNDER:
            parseIncrementOption(content, macros);
            return StemType.ROUNDER;
        case INTEGER_WIDTH:
            parseIntegerWidthOption(content, macros);
            return StemType.OTHER;
        case NUMBERING_SYSTEM:
            parseNumberingSystemOption(content, macros);
            return StemType.OTHER;
        }

        ///// Non-required options: /////

        // Scientific options
        switch (stem) {
        case SCIENTIFIC_NOTATION:
            if (parseExponentWidthOption(content, macros)) {
                return StemType.SCIENTIFIC_NOTATION;
            }
            if (parseExponentSignOption(content, macros)) {
                return StemType.SCIENTIFIC_NOTATION;
            }
        }

        // Frac-sig option
        switch (stem) {
        case FRACTION_ROUNDER:
            if (parseFracSigOption(content, macros)) {
                return StemType.ROUNDER;
            }
        }

        // Rounding mode option
        switch (stem) {
        case ROUNDER:
        case FRACTION_ROUNDER:
        case CURRENCY_ROUNDER:
            if (parseRoundingModeOption(content, macros)) {
                return StemType.ROUNDER;
            }
        }

        // Unknown option
        throw new SkeletonSyntaxException("Unknown option", content);
    }

    private static void generateSkeleton(MacroProps macros, StringBuilder sb) {
        if (macros.notation != null) {
            generateNotationValue(macros, sb);
            sb.append(' ');
        }
        if (macros.unit != null) {
            generateUnitValue(macros, sb);
            sb.append(' ');
        }
        if (macros.perUnit != null) {
            generatePerUnitValue(macros, sb);
            sb.append(' ');
        }
        if (macros.rounder != null) {
            generateRoundingValue(macros, sb);
            sb.append(' ');
        }
        if (macros.grouping != null) {
            generateGroupingValue(macros, sb);
            sb.append(' ');
        }
        if (macros.integerWidth != null) {
            generateIntegerWidthValue(macros, sb);
            sb.append(' ');
        }
        if (macros.symbols != null) {
            generateSymbolsValue(macros, sb);
            sb.append(' ');
        }
        if (macros.unitWidth != null) {
            generateUnitWidthValue(macros, sb);
            sb.append(' ');
        }
        if (macros.sign != null) {
            generateSignValue(macros, sb);
            sb.append(' ');
        }
        if (macros.decimal != null) {
            generateDecimalValue(macros, sb);
            sb.append(' ');
        }

        // Remove the trailing space
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
    }

    /////

    private static boolean parseExponentWidthOption(CharSequence content, MacroProps macros) {
        if (content.charAt(0) != '+') {
            return false;
        }
        int offset = 1;
        int minExp = 0;
        for (; offset < content.length(); offset++) {
            if (content.charAt(offset) == 'e') {
                minExp++;
            } else {
                break;
            }
        }
        if (offset < content.length()) {
            return false;
        }
        // Use the public APIs to enforce bounds checking
        macros.notation = ((ScientificNotation) macros.notation).withMinExponentDigits(minExp);
        return true;
    }

    private static void generateExponentWidthOption(int minInt, int maxInt, StringBuilder sb) {
        sb.append('+');
        appendMultiple(sb, 'e', minInt);
    }

    private static boolean parseExponentSignOption(CharSequence content, MacroProps macros) {
        Object value = skeletonData.stemToValue(content);
        if (value != null && value instanceof SignDisplay) {
            macros.notation = ((ScientificNotation) macros.notation)
                    .withExponentSignDisplay((SignDisplay) value);
            return true;
        }
        return false;
    }

    private static void generateCurrencyOption(Currency currency, StringBuilder sb) {
        sb.append(currency.getCurrencyCode());
    }

    private static void parseCurrencyOption(CharSequence content, MacroProps macros) {
        String currencyCode = content.subSequence(0, content.length()).toString();
        try {
            macros.unit = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new SkeletonSyntaxException("Invalid currency", content, e);
        }
    }

    private static void parseMeasureUnitOption(CharSequence content, MacroProps macros) {
        // NOTE: The category (type) of the unit is guaranteed to be a valid subtag (alphanumeric)
        // http://unicode.org/reports/tr35/#Validity_Data
        int firstHyphen = 0;
        while (firstHyphen < content.length() && content.charAt(firstHyphen) != '-') {
            firstHyphen++;
        }
        if (firstHyphen == content.length()) {
            throw new SkeletonSyntaxException("Invalid measure unit option", content);
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
        throw new SkeletonSyntaxException("Unknown measure unit", content);
    }

    private static void generateMeasureUnitOption(MeasureUnit unit, StringBuilder sb) {
        sb.append(unit.getType() + "-" + unit.getSubtype());
    }

    private static void parseMeasurePerUnitOption(CharSequence content, MacroProps macros) {
        // A little bit of a hack: safe the current unit (numerator), call the main measure unit parsing
        // code, put back the numerator unit, and put the new unit into per-unit.
        MeasureUnit numerator = macros.unit;
        parseMeasureUnitOption(content, macros);
        macros.perUnit = macros.unit;
        macros.unit = numerator;
    }

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
        // the new Rounder saved into the macros.
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

    private static void parseIntegerWidthOption(CharSequence content, MacroProps macros) {
        int offset = 0;
        int minInt = 0;
        int maxInt;
        if (content.charAt(0) == '+') {
            maxInt = -1;
            offset++;
        } else {
            maxInt = 0;
        }
        for (; offset < content.length(); offset++) {
            if (content.charAt(offset) == '#') {
                maxInt++;
            } else {
                break;
            }
        }
        if (offset < content.length()) {
            for (; offset < content.length(); offset++) {
                if (content.charAt(offset) == '0') {
                    minInt++;
                } else {
                    break;
                }
            }
        }
        if (maxInt != -1) {
            maxInt += minInt;
        }
        if (offset < content.length()) {
            throw new SkeletonSyntaxException("Invalid integer width stem", content);
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

    private static void parseNumberingSystemOption(CharSequence content, MacroProps macros) {
        String nsName = content.subSequence(0, content.length()).toString();
        NumberingSystem ns = NumberingSystem.getInstanceByName(nsName);
        if (ns == null) {
            throw new SkeletonSyntaxException("Unknown numbering system", content);
        }
        macros.symbols = ns;
    }

    private static void generateNumberingSystemOption(NumberingSystem ns, StringBuilder sb) {
        sb.append(ns.getName());
    }

    /////

    private static void generateNotationValue(MacroProps macros, StringBuilder sb) {
        // Check for literals
        String literal = skeletonData.valueToStem(macros.notation);
        if (literal != null) {
            sb.append(literal);
            return;
        }

        // Generate the stem
        if (macros.notation instanceof CompactNotation) {
            // Compact notation generated from custom data (not supported in skeleton)
            // The other compact notations are literals
        } else if (macros.notation instanceof ScientificNotation) {
            ScientificNotation impl = (ScientificNotation) macros.notation;
            if (impl.engineeringInterval == 3) {
                sb.append("engineering");
            } else {
                sb.append("scientific");
            }
            if (impl.minExponentDigits > 1) {
                sb.append('/');
                generateExponentWidthOption(impl.minExponentDigits, -1, sb);
            }
            if (impl.exponentSignDisplay != SignDisplay.AUTO) {
                sb.append('/');
                sb.append(skeletonData.valueToStem(impl.exponentSignDisplay));
            }
        } else {
            assert macros.notation instanceof SimpleNotation;
            sb.append("notation-simple");
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
            sb.append("currency/");
            generateCurrencyOption((Currency) macros.unit, sb);
        } else if (macros.unit instanceof NoUnit) {
            // This should be taken care of by the literals.
            assert false;
        } else {
            sb.append("measure-unit/");
            generateMeasureUnitOption(macros.unit, sb);
        }
    }

    private static void generatePerUnitValue(MacroProps macros, StringBuilder sb) {
        // Per-units are currently expected to be only MeasureUnits.
        if (macros.unit instanceof Currency || macros.unit instanceof NoUnit) {
            assert false;
        } else {
            sb.append("per-measure-unit/");
            generateMeasureUnitOption(macros.perUnit, sb);
        }
    }

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

    private static void generateGroupingValue(MacroProps macros, StringBuilder sb) {
        appendExpectedLiteral(macros.grouping, sb);
    }

    private static void generateIntegerWidthValue(MacroProps macros, StringBuilder sb) {
        sb.append("integer-width/");
        generateIntegerWidthOption(macros.integerWidth.minInt, macros.integerWidth.maxInt, sb);
    }

    private static void generateSymbolsValue(MacroProps macros, StringBuilder sb) {
        if (macros.symbols instanceof NumberingSystem) {
            NumberingSystem ns = (NumberingSystem) macros.symbols;
            if (ns.getName().equals("latn")) {
                sb.append("latin");
            } else {
                sb.append("numbering-system/");
                generateNumberingSystemOption(ns, sb);
            }
        } else {
            // DecimalFormatSymbols (not supported in skeleton)
        }
    }

    private static void generateUnitWidthValue(MacroProps macros, StringBuilder sb) {
        appendExpectedLiteral(macros.unitWidth, sb);
    }

    private static void generateSignValue(MacroProps macros, StringBuilder sb) {
        appendExpectedLiteral(macros.sign, sb);
    }

    private static void generateDecimalValue(MacroProps macros, StringBuilder sb) {
        appendExpectedLiteral(macros.decimal, sb);
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
