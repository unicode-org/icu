// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.LdmlPatternInfo;
import com.ibm.icu.impl.number.LdmlPatternInfo.PatternParseResult;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.text.CompactDecimalFormat.CompactType;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.Dimensionless;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.impl.MacroProps;
import newapi.impl.MicroProps;
import newapi.impl.Padder;
import newapi.impl.QuantityChain;

public class Worker1 {

    public static Worker1 fromMacros(MacroProps macros) {
        return new Worker1(make(macros, true));
    }

    public static MicroProps applyStatic(MacroProps macros, FormatQuantity inValue, NumberStringBuilder outString) {
        MicroProps micros = make(macros, false).withQuantity(inValue);
        applyStatic(micros, inValue, outString);
        return micros;
    }

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");

    final QuantityChain microsGenerator;

    private Worker1(QuantityChain microsGenerator) {
        this.microsGenerator = microsGenerator;
    }

    public MicroProps apply(FormatQuantity inValue, NumberStringBuilder outString) {
        MicroProps micros = microsGenerator.withQuantity(inValue);
        applyStatic(micros, inValue, outString);
        return micros;
    }

    //////////

    private static QuantityChain make(MacroProps input, boolean build) {

        String innerPattern = null;
        MurkyLongNameHandler longNames = null;
        Rounder defaultRounding = Rounder.none();
        Currency currency = DEFAULT_CURRENCY;
        FormatWidth unitWidth = null;
        boolean perMille = false;
        PluralRules rules = input.rules;

        MicroProps micros = new MicroProps(build);
        QuantityChain chain = micros;

        // Copy over the simple settings
        micros.sign = input.sign == null ? SignDisplay.AUTO : input.sign;
        micros.decimal = input.decimal == null ? DecimalMarkDisplay.AUTO : input.decimal;
        micros.multiplier = 0;
        micros.integerWidth = input.integerWidth == null ? IntegerWidth.zeroFillTo(1) : input.integerWidth;

        if (input.unit == null || input.unit == Dimensionless.BASE) {
            // No units; default format
            innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.NUMBERSTYLE);
        } else if (input.unit == Dimensionless.PERCENT) {
            // Percent
            innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.PERCENTSTYLE);
            micros.multiplier += 2;
        } else if (input.unit == Dimensionless.PERMILLE) {
            // Permille
            innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.PERCENTSTYLE);
            micros.multiplier += 3;
            perMille = true;
        } else if (input.unit instanceof Currency && input.unitWidth != FormatWidth.WIDE) {
            // Narrow, short, or ISO currency.
            // TODO: Accounting style?
            innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.CURRENCYSTYLE);
            defaultRounding = Rounder.currency(CurrencyUsage.STANDARD);
            currency = (Currency) input.unit;
            micros.useCurrency = true;
            unitWidth = (input.unitWidth == null) ? FormatWidth.NARROW : input.unitWidth;
        } else if (input.unit instanceof Currency) {
            // Currency long name
            innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.NUMBERSTYLE);
            longNames = MurkyLongNameHandler.getCurrencyLongNameModifiers(input.loc, (Currency) input.unit);
            defaultRounding = Rounder.currency(CurrencyUsage.STANDARD);
            currency = (Currency) input.unit;
            micros.useCurrency = true;
            unitWidth = input.unitWidth = FormatWidth.WIDE;
        } else {
            // MeasureUnit
            innerPattern = NumberFormat.getPatternForStyle(input.loc, NumberFormat.NUMBERSTYLE);
            unitWidth = (input.unitWidth == null) ? FormatWidth.SHORT : input.unitWidth;
            longNames = MurkyLongNameHandler.getMeasureUnitModifiers(input.loc, input.unit, unitWidth);
        }

        // Parse the pattern, which is used for grouping and affixes only.
        PatternParseResult patternInfo = LdmlPatternInfo.parse(innerPattern);

        // Symbols
        if (input.symbols == null) {
            micros.symbols = DecimalFormatSymbols.getInstance(input.loc);
        } else if (input.symbols instanceof DecimalFormatSymbols) {
            micros.symbols = (DecimalFormatSymbols) input.symbols;
        } else if (input.symbols instanceof NumberingSystem) {
            // TODO: Do this more efficiently. Will require modifying DecimalFormatSymbols.
            NumberingSystem ns = (NumberingSystem) input.symbols;
            ULocale temp = input.loc.setKeywordValue("numbers", ns.getName());
            micros.symbols = DecimalFormatSymbols.getInstance(temp);
        } else {
            throw new AssertionError();
        }

        // TODO: Normalize the currency (accept symbols from DecimalFormatSymbols)?
        // currency = CustomSymbolCurrency.resolve(currency, input.loc, micros.symbols);

        // Multiplier (compatibility mode value).
        // An int magnitude multiplier is used when not in compatibility mode to
        // reduce object creations.
        if (input.multiplier != null) {
            chain = input.multiplier.copyAndChain(chain);
        }

        // Rounding strategy
        if (input.rounder != null) {
            micros.rounding = Rounder.normalizeType(input.rounder, currency);
        } else if (input.notation instanceof CompactNotation) {
            micros.rounding = Rounder.COMPACT_STRATEGY;
        } else {
            micros.rounding = Rounder.normalizeType(defaultRounding, currency);
        }

        // Grouping strategy
        if (input.grouper != null) {
            micros.grouping = Grouper.normalizeType(input.grouper, patternInfo);
        } else if (input.notation instanceof CompactNotation) {
            // Compact notation uses minGrouping by default since ICU 59
            micros.grouping = Grouper.normalizeType(Grouper.min2(), patternInfo);
        } else {
            micros.grouping = Grouper.normalizeType(Grouper.defaults(), patternInfo);
        }

        // Inner modifier (scientific notation)
        if (input.notation instanceof ScientificNotation) {
            chain = ((ScientificNotation) input.notation).withLocaleData(micros.symbols, build, chain);
        } else {
            // No inner modifier required
            micros.modInner = ConstantAffixModifier.EMPTY;
        }

        // Middle modifier (patterns, positive/negative, currency symbols, percent)
        // The default middle modifier is weak (thus the false argument).
        MurkyModifier murkyMod = new MurkyModifier(false);
        murkyMod.setPatternInfo((input.affixProvider != null) ? input.affixProvider : patternInfo);
        murkyMod.setPatternAttributes(micros.sign, perMille);
        if (murkyMod.needsPlurals()) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(input.loc);
            }
            murkyMod.setSymbols(micros.symbols, currency, unitWidth, rules);
        } else {
            murkyMod.setSymbols(micros.symbols, currency, unitWidth, null);
        }
        if (build) {
            chain = murkyMod.createImmutableAndChain(chain);
        } else {
            chain = murkyMod.addToChain(chain);
        }

        // Outer modifier (CLDR units and currency long names)
        if (longNames != null) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(input.loc);
            }
            chain = longNames.withLocaleData(rules, build, chain);
        } else {
            // No outer modifier required
            micros.modOuter = ConstantAffixModifier.EMPTY;
        }

        // Padding strategy
        if (input.padder != null) {
            micros.padding = input.padder;
        } else {
            micros.padding = Padder.none();
        }

        // Compact notation
        // NOTE: Compact notation can (but might not) override the middle modifier and rounding.
        // It therefore needs to go at the end of the chain.
        if (input.notation instanceof CompactNotation) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(input.loc);
            }
            CompactType compactType = (input.unit instanceof Currency) ? CompactType.CURRENCY : CompactType.DECIMAL;
            chain = ((CompactNotation) input.notation).withLocaleData(input.loc, compactType, rules,
                    build ? murkyMod : null, chain);
        }

        return chain;
    }

    //////////

    private static int applyStatic(MicroProps micros, FormatQuantity inValue, NumberStringBuilder outString) {
        inValue.adjustMagnitude(micros.multiplier);
        micros.rounding.apply(inValue);
        if (micros.integerWidth.maxInt == -1) {
            inValue.setIntegerLength(micros.integerWidth.minInt, Integer.MAX_VALUE);
        } else {
            inValue.setIntegerLength(micros.integerWidth.minInt, micros.integerWidth.maxInt);
        }
        int length = writeNumber(micros, inValue, outString);
        // NOTE: When range formatting is added, these modifiers can bubble up.
        // For now, apply them all here at once.
        length += micros.padding.applyModsAndMaybePad(micros, outString, 0, length);
        return length;
    }

    private static int writeNumber(MicroProps micros, FormatQuantity input, NumberStringBuilder string) {
        int length = 0;
        if (input.isInfinite()) {
            length += string.insert(length, micros.symbols.getInfinity(), NumberFormat.Field.INTEGER);

        } else if (input.isNaN()) {
            length += string.insert(length, micros.symbols.getNaN(), NumberFormat.Field.INTEGER);

        } else {
            // Add the integer digits
            length += writeIntegerDigits(micros, input, string);

            // Add the decimal point
            if (input.getLowerDisplayMagnitude() < 0 || micros.decimal == DecimalMarkDisplay.ALWAYS) {
                length += string.insert(length, micros.useCurrency ? micros.symbols.getMonetaryDecimalSeparatorString()
                        : micros.symbols.getDecimalSeparatorString(), NumberFormat.Field.DECIMAL_SEPARATOR);
            }

            // Add the fraction digits
            length += writeFractionDigits(micros, input, string);
        }

        return length;
    }

    private static int writeIntegerDigits(MicroProps micros, FormatQuantity input, NumberStringBuilder string) {
        int length = 0;
        int integerCount = input.getUpperDisplayMagnitude() + 1;
        for (int i = 0; i < integerCount; i++) {
            // Add grouping separator
            if (micros.grouping.groupAtPosition(i, input)) {
                length += string.insert(0, micros.useCurrency ? micros.symbols.getMonetaryGroupingSeparatorString()
                        : micros.symbols.getGroupingSeparatorString(), NumberFormat.Field.GROUPING_SEPARATOR);
            }

            // Get and append the next digit value
            byte nextDigit = input.getDigit(i);
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

    private static int writeFractionDigits(MicroProps micros, FormatQuantity input, NumberStringBuilder string) {
        int length = 0;
        int fractionCount = -input.getLowerDisplayMagnitude();
        for (int i = 0; i < fractionCount; i++) {
            // Get and append the next digit value
            byte nextDigit = input.getDigit(-i - 1);
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
