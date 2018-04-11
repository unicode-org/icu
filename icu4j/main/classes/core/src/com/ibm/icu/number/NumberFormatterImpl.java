// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.CurrencyData.CurrencyFormatInfo;
import com.ibm.icu.impl.number.CompactData.CompactType;
import com.ibm.icu.impl.number.ConstantAffixModifier;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.LongNameHandler;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.impl.number.MicroPropsGenerator;
import com.ibm.icu.impl.number.MultiplierFormatHandler;
import com.ibm.icu.impl.number.MutablePatternModifier;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Padder;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;

/**
 * This is the "brain" of the number formatting pipeline. It ties all the pieces together, taking in a
 * MacroProps and a DecimalQuantity and outputting a properly formatted number string.
 *
 * <p>
 * This class, as well as NumberPropertyMapper, could go into the impl package, but they depend on too
 * many package-private members of the public APIs.
 */
class NumberFormatterImpl {

    /** Builds a "safe" MicroPropsGenerator, which is thread-safe and can be used repeatedly. */
    public static NumberFormatterImpl fromMacros(MacroProps macros) {
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, true);
        return new NumberFormatterImpl(microPropsGenerator);
    }

    /**
     * Builds and evaluates an "unsafe" MicroPropsGenerator, which is cheaper but can be used only once.
     */
    public static void applyStatic(
            MacroProps macros,
            DecimalQuantity inValue,
            NumberStringBuilder outString) {
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, false);
        MicroProps micros = microPropsGenerator.processQuantity(inValue);
        microsToString(micros, inValue, outString);
    }

    /**
     * Prints only the prefix and suffix; used for DecimalFormat getters.
     *
     * @return The index into the output at which the prefix ends and the suffix starts; in other words,
     *         the prefix length.
     */
    public static int getPrefixSuffix(
            MacroProps macros,
            DecimalQuantity inValue,
            NumberStringBuilder output) {
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, false);
        MicroProps micros = microPropsGenerator.processQuantity(inValue);
        // #13453: DecimalFormat wants the affixes from the pattern only (modMiddle).
        micros.modMiddle.apply(output, 0, 0);
        return micros.modMiddle.getPrefixLength();
    }

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");

    final MicroPropsGenerator microPropsGenerator;

    private NumberFormatterImpl(MicroPropsGenerator microPropsGenerator) {
        this.microPropsGenerator = microPropsGenerator;
    }

    public void apply(DecimalQuantity inValue, NumberStringBuilder outString) {
        MicroProps micros = microPropsGenerator.processQuantity(inValue);
        microsToString(micros, inValue, outString);
    }

    //////////

    private static boolean unitIsCurrency(MeasureUnit unit) {
        // TODO: Check using "instanceof" operator instead?
        return unit != null && "currency".equals(unit.getType());
    }

    private static boolean unitIsNoUnit(MeasureUnit unit) {
        // NOTE: In ICU4C, units cannot be null, and the default unit is a NoUnit.
        // In ICU4J, return TRUE for a null unit from this method.
        return unit == null || "none".equals(unit.getType());
    }

    private static boolean unitIsPercent(MeasureUnit unit) {
        return unit != null && "percent".equals(unit.getSubtype());
    }

    private static boolean unitIsPermille(MeasureUnit unit) {
        return unit != null && "permille".equals(unit.getSubtype());
    }

    /**
     * Synthesizes the MacroProps into a MicroPropsGenerator. All information, including the locale, is
     * encoded into the MicroPropsGenerator, except for the quantity itself, which is left abstract and
     * must be provided to the returned MicroPropsGenerator instance.
     *
     * @see MicroPropsGenerator
     * @param macros
     *            The {@link MacroProps} to consume. This method does not mutate the MacroProps instance.
     * @param safe
     *            If true, the returned MicroPropsGenerator will be thread-safe. If false, the returned
     *            value will <em>not</em> be thread-safe, intended for a single "one-shot" use only.
     *            Building the thread-safe object is more expensive.
     */
    private static MicroPropsGenerator macrosToMicroGenerator(MacroProps macros, boolean safe) {
        MicroProps micros = new MicroProps(safe);
        MicroPropsGenerator chain = micros;

        // TODO: Normalize the currency (accept symbols from DecimalFormatSymbols)?
        // currency = CustomSymbolCurrency.resolve(currency, input.loc, micros.symbols);

        // Pre-compute a few values for efficiency.
        boolean isCurrency = unitIsCurrency(macros.unit);
        boolean isNoUnit = unitIsNoUnit(macros.unit);
        boolean isPercent = isNoUnit && unitIsPercent(macros.unit);
        boolean isPermille = isNoUnit && unitIsPermille(macros.unit);
        boolean isCldrUnit = !isCurrency && !isNoUnit;
        boolean isAccounting = macros.sign == SignDisplay.ACCOUNTING
                || macros.sign == SignDisplay.ACCOUNTING_ALWAYS
                || macros.sign == SignDisplay.ACCOUNTING_EXCEPT_ZERO;
        Currency currency = isCurrency ? (Currency) macros.unit : DEFAULT_CURRENCY;
        UnitWidth unitWidth = UnitWidth.SHORT;
        if (macros.unitWidth != null) {
            unitWidth = macros.unitWidth;
        }
        PluralRules rules = macros.rules;

        // Select the numbering system.
        NumberingSystem ns;
        if (macros.symbols instanceof NumberingSystem) {
            ns = (NumberingSystem) macros.symbols;
        } else {
            // TODO: Is there a way to avoid creating the NumberingSystem object?
            ns = NumberingSystem.getInstance(macros.loc);
        }
        String nsName = ns.getName();

        // Resolve the symbols. Do this here because currency may need to customize them.
        if (macros.symbols instanceof DecimalFormatSymbols) {
            micros.symbols = (DecimalFormatSymbols) macros.symbols;
        } else {
            micros.symbols = DecimalFormatSymbols.forNumberingSystem(macros.loc, ns);
        }

        // Load and parse the pattern string. It is used for grouping sizes and affixes only.
        // If we are formatting currency, check for a currency-specific pattern.
        String pattern = null;
        if (isCurrency) {
            CurrencyFormatInfo info = CurrencyData.provider.getInstance(macros.loc, true)
                    .getFormatInfo(currency.getCurrencyCode());
            if (info != null) {
                pattern = info.currencyPattern;
                // It's clunky to clone an object here, but this code is not frequently executed.
                micros.symbols = (DecimalFormatSymbols) micros.symbols.clone();
                micros.symbols.setMonetaryDecimalSeparatorString(info.monetaryDecimalSeparator);
                micros.symbols.setMonetaryGroupingSeparatorString(info.monetaryGroupingSeparator);
            }
        }
        if (pattern == null) {
            int patternStyle;
            if (isPercent || isPermille) {
                patternStyle = NumberFormat.PERCENTSTYLE;
            } else if (!isCurrency || unitWidth == UnitWidth.FULL_NAME) {
                patternStyle = NumberFormat.NUMBERSTYLE;
            } else if (isAccounting) {
                // NOTE: Although ACCOUNTING and ACCOUNTING_ALWAYS are only supported in currencies
                // right now, the API contract allows us to add support to other units in the future.
                patternStyle = NumberFormat.ACCOUNTINGCURRENCYSTYLE;
            } else {
                patternStyle = NumberFormat.CURRENCYSTYLE;
            }
            pattern = NumberFormat
                    .getPatternForStyleAndNumberingSystem(macros.loc, nsName, patternStyle);
        }
        ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(pattern);

        /////////////////////////////////////////////////////////////////////////////////////
        /// START POPULATING THE DEFAULT MICROPROPS AND BUILDING THE MICROPROPS GENERATOR ///
        /////////////////////////////////////////////////////////////////////////////////////

        // Multiplier (compatibility mode value).
        if (macros.multiplier != null) {
            chain = new MultiplierFormatHandler(macros.multiplier, chain);
        }

        // Rounding strategy
        if (macros.rounder != null) {
            micros.rounding = macros.rounder;
        } else if (macros.notation instanceof CompactNotation) {
            micros.rounding = Rounder.COMPACT_STRATEGY;
        } else if (isCurrency) {
            micros.rounding = Rounder.MONETARY_STANDARD;
        } else {
            micros.rounding = Rounder.DEFAULT_MAX_FRAC_6;
        }
        micros.rounding = micros.rounding.withLocaleData(currency);

        // Grouping strategy
        if (macros.grouping instanceof Grouper) {
            micros.grouping = (Grouper) macros.grouping;
        } else if (macros.grouping instanceof GroupingStrategy) {
            micros.grouping = Grouper.forStrategy((GroupingStrategy) macros.grouping);
        } else if (macros.notation instanceof CompactNotation) {
            // Compact notation uses minGrouping by default since ICU 59
            micros.grouping = Grouper.forStrategy(GroupingStrategy.MIN2);
        } else {
            micros.grouping = Grouper.forStrategy(GroupingStrategy.AUTO);
        }
        micros.grouping = micros.grouping.withLocaleData(macros.loc, patternInfo);

        // Padding strategy
        if (macros.padder != null) {
            micros.padding = macros.padder;
        } else {
            micros.padding = Padder.NONE;
        }

        // Integer width
        if (macros.integerWidth != null) {
            micros.integerWidth = macros.integerWidth;
        } else {
            micros.integerWidth = IntegerWidth.DEFAULT;
        }

        // Sign display
        if (macros.sign != null) {
            micros.sign = macros.sign;
        } else {
            micros.sign = SignDisplay.AUTO;
        }

        // Decimal mark display
        if (macros.decimal != null) {
            micros.decimal = macros.decimal;
        } else {
            micros.decimal = DecimalSeparatorDisplay.AUTO;
        }

        // Use monetary separator symbols
        micros.useCurrency = isCurrency;

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
        patternMod.setPatternAttributes(micros.sign, isPermille);
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
        if (isCldrUnit) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(macros.loc);
            }
            chain = LongNameHandler
                    .forMeasureUnit(macros.loc, macros.unit, macros.perUnit, unitWidth, rules, chain);
        } else if (isCurrency && unitWidth == UnitWidth.FULL_NAME) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(macros.loc);
            }
            chain = LongNameHandler.forCurrencyLongNames(macros.loc, currency, rules, chain);
        } else {
            // No outer modifier required
            micros.modOuter = ConstantAffixModifier.EMPTY;
        }

        // Compact notation
        // NOTE: Compact notation can (but might not) override the middle modifier and rounding.
        // It therefore needs to go at the end of the chain.
        if (macros.notation instanceof CompactNotation) {
            if (rules == null) {
                // Lazily create PluralRules
                rules = PluralRules.forLocale(macros.loc);
            }
            CompactType compactType = (macros.unit instanceof Currency
                    && macros.unitWidth != UnitWidth.FULL_NAME) ? CompactType.CURRENCY
                            : CompactType.DECIMAL;
            chain = ((CompactNotation) macros.notation).withLocaleData(macros.loc,
                    nsName,
                    compactType,
                    rules,
                    safe ? patternMod : null,
                    chain);
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
    private static void microsToString(
            MicroProps micros,
            DecimalQuantity quantity,
            NumberStringBuilder string) {
        micros.rounding.apply(quantity);
        if (micros.integerWidth.maxInt == -1) {
            quantity.setIntegerLength(micros.integerWidth.minInt, Integer.MAX_VALUE);
        } else {
            quantity.setIntegerLength(micros.integerWidth.minInt, micros.integerWidth.maxInt);
        }
        int length = writeNumber(micros, quantity, string);
        // NOTE: When range formatting is added, these modifiers can bubble up.
        // For now, apply them all here at once.
        // Always apply the inner modifier (which is "strong").
        length += micros.modInner.apply(string, 0, length);
        if (micros.padding.isValid()) {
            micros.padding.padAndApply(micros.modMiddle, micros.modOuter, string, 0, length);
        } else {
            length += micros.modMiddle.apply(string, 0, length);
            length += micros.modOuter.apply(string, 0, length);
        }
    }

    private static int writeNumber(
            MicroProps micros,
            DecimalQuantity quantity,
            NumberStringBuilder string) {
        int length = 0;
        if (quantity.isInfinite()) {
            length += string.insert(length, micros.symbols.getInfinity(), NumberFormat.Field.INTEGER);

        } else if (quantity.isNaN()) {
            length += string.insert(length, micros.symbols.getNaN(), NumberFormat.Field.INTEGER);

        } else {
            // Add the integer digits
            length += writeIntegerDigits(micros, quantity, string);

            // Add the decimal point
            if (quantity.getLowerDisplayMagnitude() < 0
                    || micros.decimal == DecimalSeparatorDisplay.ALWAYS) {
                length += string.insert(length,
                        micros.useCurrency ? micros.symbols.getMonetaryDecimalSeparatorString()
                                : micros.symbols.getDecimalSeparatorString(),
                        NumberFormat.Field.DECIMAL_SEPARATOR);
            }

            // Add the fraction digits
            length += writeFractionDigits(micros, quantity, string);
        }

        return length;
    }

    private static int writeIntegerDigits(
            MicroProps micros,
            DecimalQuantity quantity,
            NumberStringBuilder string) {
        int length = 0;
        int integerCount = quantity.getUpperDisplayMagnitude() + 1;
        for (int i = 0; i < integerCount; i++) {
            // Add grouping separator
            if (micros.grouping.groupAtPosition(i, quantity)) {
                length += string.insert(0,
                        micros.useCurrency ? micros.symbols.getMonetaryGroupingSeparatorString()
                                : micros.symbols.getGroupingSeparatorString(),
                        NumberFormat.Field.GROUPING_SEPARATOR);
            }

            // Get and append the next digit value
            byte nextDigit = quantity.getDigit(i);
            if (micros.symbols.getCodePointZero() != -1) {
                length += string.insertCodePoint(0,
                        micros.symbols.getCodePointZero() + nextDigit,
                        NumberFormat.Field.INTEGER);
            } else {
                length += string.insert(0,
                        micros.symbols.getDigitStringsLocal()[nextDigit],
                        NumberFormat.Field.INTEGER);
            }
        }
        return length;
    }

    private static int writeFractionDigits(
            MicroProps micros,
            DecimalQuantity quantity,
            NumberStringBuilder string) {
        int length = 0;
        int fractionCount = -quantity.getLowerDisplayMagnitude();
        for (int i = 0; i < fractionCount; i++) {
            // Get and append the next digit value
            byte nextDigit = quantity.getDigit(-i - 1);
            if (micros.symbols.getCodePointZero() != -1) {
                length += string.appendCodePoint(micros.symbols.getCodePointZero() + nextDigit,
                        NumberFormat.Field.FRACTION);
            } else {
                length += string.append(micros.symbols.getDigitStringsLocal()[nextDigit],
                        NumberFormat.Field.FRACTION);
            }
        }
        return length;
    }
}
