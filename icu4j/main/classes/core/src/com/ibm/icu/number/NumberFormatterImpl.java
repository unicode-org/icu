// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.CurrencyData.CurrencyFormatInfo;
import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.CompactData.CompactType;
import com.ibm.icu.impl.number.ConstantAffixModifier;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.LongNameHandler;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.MicroProps;
import com.ibm.icu.impl.number.MicroPropsGenerator;
import com.ibm.icu.impl.number.MultiplierFormatHandler;
import com.ibm.icu.impl.number.MutablePatternModifier;
import com.ibm.icu.impl.number.Padder;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringParser.ParsedPatternInfo;
import com.ibm.icu.impl.number.RoundingUtils;
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
    public NumberFormatterImpl(MacroProps macros) {
        micros = new MicroProps(true);
        microPropsGenerator = macrosToMicroGenerator(macros, micros, true);
    }

    /**
     * Builds and evaluates an "unsafe" MicroPropsGenerator, which is cheaper but can be used only once.
     */
    public static int formatStatic(
            MacroProps macros,
            DecimalQuantity inValue,
            FormattedStringBuilder outString) {
        MicroProps micros = preProcessUnsafe(macros, inValue);
        int length = writeNumber(micros, inValue, outString, 0);
        length += writeAffixes(micros, outString, 0, length);
        return length;
    }

    /**
     * Prints only the prefix and suffix; used for DecimalFormat getters.
     *
     * @return The index into the output at which the prefix ends and the suffix starts; in other words,
     *         the prefix length.
     */
    public static int getPrefixSuffixStatic(
            MacroProps macros,
            byte signum,
            StandardPlural plural,
            FormattedStringBuilder output) {
        MicroProps micros = new MicroProps(false);
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, micros, false);
        return getPrefixSuffixImpl(microPropsGenerator, signum, output);
    }

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XXX");

    final MicroProps micros;
    final MicroPropsGenerator microPropsGenerator;

    /**
     * Evaluates the "safe" MicroPropsGenerator created by "fromMacros".
     */
    public int format(DecimalQuantity inValue, FormattedStringBuilder outString) {
        MicroProps micros = preProcess(inValue);
        int length = writeNumber(micros, inValue, outString, 0);
        length += writeAffixes(micros, outString, 0, length);
        return length;
    }

    /**
     * Like format(), but saves the result into an output MicroProps without additional processing.
     */
    public MicroProps preProcess(DecimalQuantity inValue) {
        MicroProps micros = microPropsGenerator.processQuantity(inValue);
        micros.rounder.apply(inValue);
        if (micros.integerWidth.maxInt == -1) {
            inValue.setMinInteger(micros.integerWidth.minInt);
        } else {
            inValue.setMinInteger(micros.integerWidth.minInt);
            inValue.applyMaxInteger(micros.integerWidth.maxInt);
        }
        return micros;
    }

    private static MicroProps preProcessUnsafe(MacroProps macros, DecimalQuantity inValue) {
        MicroProps micros = new MicroProps(false);
        MicroPropsGenerator microPropsGenerator = macrosToMicroGenerator(macros, micros, false);
        micros = microPropsGenerator.processQuantity(inValue);
        micros.rounder.apply(inValue);
        if (micros.integerWidth.maxInt == -1) {
            inValue.setMinInteger(micros.integerWidth.minInt);
        } else {
            inValue.setMinInteger(micros.integerWidth.minInt);
            inValue.applyMaxInteger(micros.integerWidth.maxInt);
        }
        return micros;
    }

    public int getPrefixSuffix(byte signum, StandardPlural plural, FormattedStringBuilder output) {
        return getPrefixSuffixImpl(microPropsGenerator, signum, output);
    }

    private static int getPrefixSuffixImpl(MicroPropsGenerator generator, byte signum, FormattedStringBuilder output) {
        // #13453: DecimalFormat wants the affixes from the pattern only (modMiddle).
        // TODO: Clean this up, closer to C++. The pattern modifier is not as accessible as in C++.
        // Right now, ignore the plural form, run the pipeline with number 0, and get the modifier from the result.
        DecimalQuantity_DualStorageBCD quantity = new DecimalQuantity_DualStorageBCD(0);
        if (signum < 0) {
            quantity.negate();
        }
        MicroProps micros = generator.processQuantity(quantity);
        micros.modMiddle.apply(output, 0, 0);
        return micros.modMiddle.getPrefixLength();
    }

    public MicroProps getRawMicroProps() {
        return micros;
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
    private static MicroPropsGenerator macrosToMicroGenerator(MacroProps macros, MicroProps micros, boolean safe) {
        MicroPropsGenerator chain = micros;

        // TODO: Normalize the currency (accept symbols from DecimalFormatSymbols)?
        // currency = CustomSymbolCurrency.resolve(currency, input.loc, micros.symbols);

        // Pre-compute a few values for efficiency.
        boolean isCurrency = unitIsCurrency(macros.unit);
        boolean isNoUnit = unitIsNoUnit(macros.unit);
        boolean isPercent = unitIsPercent(macros.unit);
        boolean isPermille = unitIsPermille(macros.unit);
        boolean isAccounting = macros.sign == SignDisplay.ACCOUNTING
                || macros.sign == SignDisplay.ACCOUNTING_ALWAYS
                || macros.sign == SignDisplay.ACCOUNTING_EXCEPT_ZERO;
        Currency currency = isCurrency ? (Currency) macros.unit : DEFAULT_CURRENCY;
        UnitWidth unitWidth = UnitWidth.SHORT;
        if (macros.unitWidth != null) {
            unitWidth = macros.unitWidth;
        }
        boolean isCldrUnit = !isCurrency && !isNoUnit &&
            (unitWidth == UnitWidth.FULL_NAME || !(isPercent || isPermille));
        PluralRules rules = macros.rules;

        // Select the numbering system.
        NumberingSystem ns;
        if (macros.symbols instanceof NumberingSystem) {
            ns = (NumberingSystem) macros.symbols;
        } else {
            // TODO: Is there a way to avoid creating the NumberingSystem object?
            ns = NumberingSystem.getInstance(macros.loc);
        }
        micros.nsName = ns.getName();

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
            if (isCldrUnit) {
                patternStyle = NumberFormat.NUMBERSTYLE;
            } else if (isPercent || isPermille) {
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
                    .getPatternForStyleAndNumberingSystem(macros.loc, micros.nsName, patternStyle);
        }
        ParsedPatternInfo patternInfo = PatternStringParser.parseToPatternInfo(pattern);

        /////////////////////////////////////////////////////////////////////////////////////
        /// START POPULATING THE DEFAULT MICROPROPS AND BUILDING THE MICROPROPS GENERATOR ///
        /////////////////////////////////////////////////////////////////////////////////////

        // Multiplier
        if (macros.scale != null) {
            chain = new MultiplierFormatHandler(macros.scale, chain);
        }

        // Rounding strategy
        if (macros.precision != null) {
            micros.rounder = macros.precision;
        } else if (macros.notation instanceof CompactNotation) {
            micros.rounder = Precision.COMPACT_STRATEGY;
        } else if (isCurrency) {
            micros.rounder = Precision.MONETARY_STANDARD;
        } else {
            micros.rounder = Precision.DEFAULT_MAX_FRAC_6;
        }
        if (macros.roundingMode != null) {
            micros.rounder = micros.rounder.withMode(
                    RoundingUtils.mathContextUnlimited(macros.roundingMode));
        }
        micros.rounder = micros.rounder.withLocaleData(currency);

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
        patternMod.setPatternInfo((macros.affixProvider != null) ? macros.affixProvider : patternInfo, null);
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
                    micros.nsName,
                    compactType,
                    rules,
                    safe ? patternMod : null,
                    chain);
        }

        return chain;
    }

    //////////

    /**
     * Adds the affixes.  Intended to be called immediately after formatNumber.
     */
    public static int writeAffixes(
            MicroProps micros,
            FormattedStringBuilder string,
            int start,
            int end) {
        // Always apply the inner modifier (which is "strong").
        int length = micros.modInner.apply(string, start, end);
        if (micros.padding.isValid()) {
            micros.padding.padAndApply(micros.modMiddle, micros.modOuter, string, start, end + length);
        } else {
            length += micros.modMiddle.apply(string, start, end + length);
            length += micros.modOuter.apply(string, start, end + length);
        }
        return length;
    }

    /**
     * Synthesizes the output string from a MicroProps and DecimalQuantity.
     * This method formats only the main number, not affixes.
     */
    public static int writeNumber(
            MicroProps micros,
            DecimalQuantity quantity,
            FormattedStringBuilder string,
            int index) {
        int length = 0;
        if (quantity.isInfinite()) {
            length += string.insert(length + index, micros.symbols.getInfinity(), NumberFormat.Field.INTEGER);

        } else if (quantity.isNaN()) {
            length += string.insert(length + index, micros.symbols.getNaN(), NumberFormat.Field.INTEGER);

        } else {
            // Add the integer digits
            length += writeIntegerDigits(micros, quantity, string, length + index);

            // Add the decimal point
            if (quantity.getLowerDisplayMagnitude() < 0
                    || micros.decimal == DecimalSeparatorDisplay.ALWAYS) {
                length += string.insert(length + index,
                        micros.useCurrency ? micros.symbols.getMonetaryDecimalSeparatorString()
                                : micros.symbols.getDecimalSeparatorString(),
                        NumberFormat.Field.DECIMAL_SEPARATOR);
            }

            // Add the fraction digits
            length += writeFractionDigits(micros, quantity, string, length + index);
        }

        return length;
    }

    private static int writeIntegerDigits(
            MicroProps micros,
            DecimalQuantity quantity,
            FormattedStringBuilder string,
            int index) {
        int length = 0;
        int integerCount = quantity.getUpperDisplayMagnitude() + 1;
        for (int i = 0; i < integerCount; i++) {
            // Add grouping separator
            if (micros.grouping.groupAtPosition(i, quantity)) {
                length += string.insert(index,
                        micros.useCurrency ? micros.symbols.getMonetaryGroupingSeparatorString()
                                : micros.symbols.getGroupingSeparatorString(),
                        NumberFormat.Field.GROUPING_SEPARATOR);
            }

            // Get and append the next digit value
            byte nextDigit = quantity.getDigit(i);
            if (micros.symbols.getCodePointZero() != -1) {
                length += string.insertCodePoint(index,
                        micros.symbols.getCodePointZero() + nextDigit,
                        NumberFormat.Field.INTEGER);
            } else {
                length += string.insert(index,
                        micros.symbols.getDigitStringsLocal()[nextDigit],
                        NumberFormat.Field.INTEGER);
            }
        }
        return length;
    }

    private static int writeFractionDigits(
            MicroProps micros,
            DecimalQuantity quantity,
            FormattedStringBuilder string,
            int index) {
        int length = 0;
        int fractionCount = -quantity.getLowerDisplayMagnitude();
        for (int i = 0; i < fractionCount; i++) {
            // Get and append the next digit value
            byte nextDigit = quantity.getDigit(-i - 1);
            if (micros.symbols.getCodePointZero() != -1) {
                length += string.insertCodePoint(length + index, micros.symbols.getCodePointZero() + nextDigit,
                        NumberFormat.Field.FRACTION);
            } else {
                length += string.insert(length + index, micros.symbols.getDigitStringsLocal()[nextDigit],
                        NumberFormat.Field.FRACTION);
            }
        }
        return length;
    }
}
