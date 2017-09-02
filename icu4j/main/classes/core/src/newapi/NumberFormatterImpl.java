// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.text.CompactDecimalFormat.CompactType;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;
import newapi.impl.LongNameHandler;
import newapi.impl.MacroProps;
import newapi.impl.MicroProps;
import newapi.impl.MicroPropsGenerator;
import newapi.impl.MutablePatternModifier;
import newapi.impl.Padder;

/**
 * This is the "brain" of the number formatting pipeline. It ties all the pieces together, taking in a MacroProps and a
 * DecimalQuantity and outputting a properly formatted number string.
 *
 * <p>
 * This class, as well as NumberPropertyMapper, could go into the impl package, but they depend on too many
 * package-private members of the public APIs.
 */
class NumberFormatterImpl {

    public static NumberFormatterImpl fromMacros(MacroProps macros) {
        // Build a "safe" MicroPropsGenerator, which is thread-safe and can be used repeatedly.
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, true);
        return new NumberFormatterImpl(microPropsGenerator);
    }

    public static MicroProps applyStatic(MacroProps macros, DecimalQuantity inValue, NumberStringBuilder outString) {
        // Build an "unsafe" MicroPropsGenerator, which is cheaper but can be used only once.
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, false);
        MicroProps micros = microPropsGenerator.processQuantity(inValue);
        microsToString(micros, inValue, outString);
        return micros;
    }

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");

    final MicroPropsGenerator microPropsGenerator;

    private NumberFormatterImpl(MicroPropsGenerator microsGenerator) {
        this.microPropsGenerator = microsGenerator;
    }

    public MicroProps apply(DecimalQuantity inValue, NumberStringBuilder outString) {
        MicroProps micros = microPropsGenerator.processQuantity(inValue);
        microsToString(micros, inValue, outString);
        return micros;
    }

    //////////

    /**
     * Synthesizes the MacroProps into a MicroPropsGenerator. All information, including the locale, is encoded into the
     * MicroPropsGenerator, except for the quantity itself, which is left abstract and must be provided to the returned
     * MicroPropsGenerator instance.
     *
     * @see MicroPropsGenerator
     * @param macros
     *            The {@link MacroProps} to consume. This method does not mutate the MacroProps instance.
     * @param safe
     *            If true, the returned MicroPropsGenerator will be thread-safe. If false, the returned value will
     *            <em>not</em> be thread-safe, intended for a single "one-shot" use only. Building the thread-safe
     *            object is more expensive.
     * @return
     */
    private static MicroPropsGenerator macrosToMicroGenerator(MacroProps macros, boolean safe) {

        String innerPattern = null;
        LongNameHandler longNames = null;
        Rounder defaultRounding = Rounder.unlimited();
        Currency currency = DEFAULT_CURRENCY;
        UnitWidth unitWidth = null;
        boolean perMille = false;
        PluralRules rules = macros.rules;

        MicroProps micros = new MicroProps(safe);
        MicroPropsGenerator chain = micros;

        // Copy over the simple settings
        micros.sign = macros.sign == null ? SignDisplay.AUTO : macros.sign;
        micros.decimal = macros.decimal == null ? DecimalMarkDisplay.AUTO : macros.decimal;
        micros.multiplier = 0;
        micros.integerWidth = macros.integerWidth == null ? IntegerWidth.zeroFillTo(1) : macros.integerWidth;

        if (macros.unit == null || macros.unit == NoUnit.BASE) {
            // No units; default format
            innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.NUMBERSTYLE);
        } else if (macros.unit == NoUnit.PERCENT) {
            // Percent
            innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.PERCENTSTYLE);
            micros.multiplier += 2;
        } else if (macros.unit == NoUnit.PERMILLE) {
            // Permille
            innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.PERCENTSTYLE);
            micros.multiplier += 3;
            perMille = true;
        } else if (macros.unit instanceof Currency && macros.unitWidth != UnitWidth.FULL_NAME) {
            // Narrow, short, or ISO currency.
            // TODO: Although ACCOUNTING and ACCOUNTING_ALWAYS are only supported in currencies right now,
            // the API contract allows us to add support to other units.
            if (macros.sign == SignDisplay.ACCOUNTING || macros.sign == SignDisplay.ACCOUNTING_ALWAYS) {
                innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.ACCOUNTINGCURRENCYSTYLE);
            } else {
                innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.CURRENCYSTYLE);
            }
            defaultRounding = Rounder.currency(CurrencyUsage.STANDARD);
            currency = (Currency) macros.unit;
            micros.useCurrency = true;
            unitWidth = (macros.unitWidth == null) ? UnitWidth.SHORT : macros.unitWidth;
        } else if (macros.unit instanceof Currency) {
            // Currency long name
            innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.NUMBERSTYLE);
            longNames = LongNameHandler.getCurrencyLongNameModifiers(macros.loc, (Currency) macros.unit);
            defaultRounding = Rounder.currency(CurrencyUsage.STANDARD);
            currency = (Currency) macros.unit;
            micros.useCurrency = true;
            unitWidth = UnitWidth.FULL_NAME;
        } else {
            // MeasureUnit
            innerPattern = NumberFormat.getPatternForStyle(macros.loc, NumberFormat.NUMBERSTYLE);
            unitWidth = (macros.unitWidth == null) ? UnitWidth.SHORT : macros.unitWidth;
            longNames = LongNameHandler.getMeasureUnitModifiers(macros.loc, macros.unit, unitWidth);
        }

        // Parse the pattern, which is used for grouping and affixes only.
        ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(innerPattern);

        // Symbols
        if (macros.symbols == null) {
            micros.symbols = DecimalFormatSymbols.getInstance(macros.loc);
        } else if (macros.symbols instanceof DecimalFormatSymbols) {
            micros.symbols = (DecimalFormatSymbols) macros.symbols;
        } else if (macros.symbols instanceof NumberingSystem) {
            // TODO: Do this more efficiently. Will require modifying DecimalFormatSymbols.
            NumberingSystem ns = (NumberingSystem) macros.symbols;
            ULocale temp = macros.loc.setKeywordValue("numbers", ns.getName());
            micros.symbols = DecimalFormatSymbols.getInstance(temp);
        } else {
            throw new AssertionError();
        }

        // TODO: Normalize the currency (accept symbols from DecimalFormatSymbols)?
        // currency = CustomSymbolCurrency.resolve(currency, input.loc, micros.symbols);

        // Multiplier (compatibility mode value).
        // An int magnitude multiplier is used when not in compatibility mode to
        // reduce object creations.
        if (macros.multiplier != null) {
            chain = macros.multiplier.copyAndChain(chain);
        }

        // Rounding strategy
        if (macros.rounder != null) {
            micros.rounding = Rounder.normalizeType(macros.rounder, currency);
        } else if (macros.notation instanceof CompactNotation) {
            micros.rounding = Rounder.COMPACT_STRATEGY;
        } else {
            micros.rounding = Rounder.normalizeType(defaultRounding, currency);
        }

        // Grouping strategy
        if (macros.grouper != null) {
            micros.grouping = Grouper.normalizeType(macros.grouper, patternInfo);
        } else if (macros.notation instanceof CompactNotation) {
            // Compact notation uses minGrouping by default since ICU 59
            micros.grouping = Grouper.normalizeType(Grouper.min2(), patternInfo);
        } else {
            micros.grouping = Grouper.normalizeType(Grouper.defaults(), patternInfo);
        }

        // Inner modifier (scientific notation)
        if (macros.notation instanceof ScientificNotation) {
            chain = ((ScientificNotation) macros.notation).withLocaleData(micros.symbols, safe, chain);
        } else {
            // No inner modifier required
            micros.modInner = ConstantAffixModifier.EMPTY;
        }

        // Middle modifier (patterns, positive/negative, currency symbols, percent)
        // The default middle modifier is weak (thus the false argument).
        MutablePatternModifier patternMod = new MutablePatternModifier(false);
        patternMod.setPatternInfo((macros.affixProvider != null) ? macros.affixProvider : patternInfo);
        patternMod.setPatternAttributes(micros.sign, perMille);
        if (patternMod.needsPlurals()) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(macros.loc);
            }
            patternMod.setSymbols(micros.symbols, currency, unitWidth, rules);
        } else {
            patternMod.setSymbols(micros.symbols, currency, unitWidth, null);
        }
        if (safe) {
            chain = patternMod.createImmutableAndChain(chain);
        } else {
            chain = patternMod.addToChain(chain);
        }

        // Outer modifier (CLDR units and currency long names)
        if (longNames != null) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(macros.loc);
            }
            chain = longNames.withLocaleData(rules, safe, chain);
        } else {
            // No outer modifier required
            micros.modOuter = ConstantAffixModifier.EMPTY;
        }

        // Padding strategy
        if (macros.padder != null) {
            micros.padding = macros.padder;
        } else {
            micros.padding = Padder.none();
        }

        // Compact notation
        // NOTE: Compact notation can (but might not) override the middle modifier and rounding.
        // It therefore needs to go at the end of the chain.
        if (macros.notation instanceof CompactNotation) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(macros.loc);
            }
            CompactType compactType = (macros.unit instanceof Currency) ? CompactType.CURRENCY : CompactType.DECIMAL;
            chain = ((CompactNotation) macros.notation).withLocaleData(macros.loc, compactType, rules,
                    safe ? patternMod : null, chain);
        }

        return chain;
    }

    //////////

    /**
     * Synthesizes the output string from a MicroProps and DecimalQuantity.
     *
     * @param micros
     *            The MicroProps after the quantity has been consumed. Will not be mutated.
     * @param quantity
     *            The DecimalQuantity to be rendered. May be mutated.
     * @param string
     *            The output string. Will be mutated.
     */
    private static void microsToString(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        quantity.adjustMagnitude(micros.multiplier);
        micros.rounding.apply(quantity);
        if (micros.integerWidth.maxInt == -1) {
            quantity.setIntegerLength(micros.integerWidth.minInt, Integer.MAX_VALUE);
        } else {
            quantity.setIntegerLength(micros.integerWidth.minInt, micros.integerWidth.maxInt);
        }
        int length = writeNumber(micros, quantity, string);
        // NOTE: When range formatting is added, these modifiers can bubble up.
        // For now, apply them all here at once.
        length += micros.padding.applyModsAndMaybePad(micros, string, 0, length);
    }

    private static int writeNumber(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        int length = 0;
        if (quantity.isInfinite()) {
            length += string.insert(length, micros.symbols.getInfinity(), NumberFormat.Field.INTEGER);

        } else if (quantity.isNaN()) {
            length += string.insert(length, micros.symbols.getNaN(), NumberFormat.Field.INTEGER);

        } else {
            // Add the integer digits
            length += writeIntegerDigits(micros, quantity, string);

            // Add the decimal point
            if (quantity.getLowerDisplayMagnitude() < 0 || micros.decimal == DecimalMarkDisplay.ALWAYS) {
                length += string.insert(length, micros.useCurrency ? micros.symbols.getMonetaryDecimalSeparatorString()
                        : micros.symbols.getDecimalSeparatorString(), NumberFormat.Field.DECIMAL_SEPARATOR);
            }

            // Add the fraction digits
            length += writeFractionDigits(micros, quantity, string);
        }

        return length;
    }

    private static int writeIntegerDigits(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        int length = 0;
        int integerCount = quantity.getUpperDisplayMagnitude() + 1;
        for (int i = 0; i < integerCount; i++) {
            // Add grouping separator
            if (micros.grouping.groupAtPosition(i, quantity)) {
                length += string.insert(0, micros.useCurrency ? micros.symbols.getMonetaryGroupingSeparatorString()
                        : micros.symbols.getGroupingSeparatorString(), NumberFormat.Field.GROUPING_SEPARATOR);
            }

            // Get and append the next digit value
            byte nextDigit = quantity.getDigit(i);
            if (micros.symbols.getCodePointZero() != -1) {
                length += string.insertCodePoint(0, micros.symbols.getCodePointZero() + nextDigit,
                        NumberFormat.Field.INTEGER);
            } else {
                length += string.insert(0, micros.symbols.getDigitStringsLocal()[nextDigit],
                        NumberFormat.Field.INTEGER);
            }
        }
        return length;
    }

    private static int writeFractionDigits(MicroProps micros, DecimalQuantity quantity, NumberStringBuilder string) {
        int length = 0;
        int fractionCount = -quantity.getLowerDisplayMagnitude();
        for (int i = 0; i < fractionCount; i++) {
            // Get and append the next digit value
            byte nextDigit = quantity.getDigit(-i - 1);
            if (micros.symbols.getCodePointZero() != -1) {
                length += string.appendCodePoint(micros.symbols.getCodePointZero() + nextDigit,
                        NumberFormat.Field.FRACTION);
            } else {
                length += string.append(micros.symbols.getDigitStringsLocal()[nextDigit], NumberFormat.Field.FRACTION);
            }
        }
        return length;
    }
}
