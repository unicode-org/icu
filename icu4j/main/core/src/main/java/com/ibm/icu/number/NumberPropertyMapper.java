// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.MathContext;

import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.CustomSymbolCurrency;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.Padder;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringUtils;
import com.ibm.icu.impl.number.PropertiesAffixPatternProvider;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.Precision.FractionRounderImpl;
import com.ibm.icu.number.Precision.IncrementRounderImpl;
import com.ibm.icu.number.Precision.SignificantRounderImpl;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.ULocale;

/**
 * <p>
 * This class, as well as NumberFormatterImpl, could go into the impl package, but they depend on too
 * many package-private members of the public APIs.
 */
final class NumberPropertyMapper {

    /** Convenience method to create a NumberFormatter directly from Properties. */
    public static UnlocalizedNumberFormatter create(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols) {
        MacroProps macros = oldToNew(properties, symbols, null);
        return NumberFormatter.with().macros(macros);
    }

    /** Convenience method to create a NumberFormatter directly from Properties. */
    public static UnlocalizedNumberFormatter create(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols,
            DecimalFormatProperties exportedProperties) {
        MacroProps macros = oldToNew(properties, symbols, exportedProperties);
        return NumberFormatter.with().macros(macros);
    }

    /**
     * Convenience method to create a NumberFormatter directly from a pattern string. Something like this
     * could become public API if there is demand.
     *
     * NOTE: This appears to be dead code.
     */
    public static UnlocalizedNumberFormatter create(String pattern, DecimalFormatSymbols symbols) {
        DecimalFormatProperties properties = PatternStringParser.parseToProperties(pattern);
        return create(properties, symbols);
    }

    /**
     * Creates a new {@link MacroProps} object based on the content of a {@link DecimalFormatProperties}
     * object. In other words, maps Properties to MacroProps. This function is used by the
     * JDK-compatibility API to call into the ICU 60 fluent number formatting pipeline.
     *
     * @param properties
     *            The property bag to be mapped.
     * @param symbols
     *            The symbols associated with the property bag.
     * @param exportedProperties
     *            A property bag in which to store validated properties. Used by some DecimalFormat
     *            getters.
     * @return A new MacroProps containing all of the information in the Properties.
     */
    public static MacroProps oldToNew(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols,
            DecimalFormatProperties exportedProperties) {
        MacroProps macros = new MacroProps();
        ULocale locale = symbols.getULocale();

        /////////////
        // SYMBOLS //
        /////////////

        macros.symbols = symbols;

        //////////////////
        // PLURAL RULES //
        //////////////////

        PluralRules rules = properties.getPluralRules();
        if (rules == null && properties.getCurrencyPluralInfo() != null) {
            rules = properties.getCurrencyPluralInfo().getPluralRules();
        }
        macros.rules = rules;

        /////////////
        // AFFIXES //
        /////////////

        AffixPatternProvider affixProvider = PropertiesAffixPatternProvider.forProperties(properties);
        macros.affixProvider = affixProvider;

        ///////////
        // UNITS //
        ///////////

        boolean useCurrency = ((properties.getCurrency() != null)
                || properties.getCurrencyPluralInfo() != null
                || properties.getCurrencyUsage() != null
                || affixProvider.hasCurrencySign());
        Currency currency = CustomSymbolCurrency.resolve(properties.getCurrency(), locale, symbols);
        CurrencyUsage currencyUsage = properties.getCurrencyUsage();
        boolean explicitCurrencyUsage = currencyUsage != null;
        if (!explicitCurrencyUsage) {
            currencyUsage = CurrencyUsage.STANDARD;
        }
        if (useCurrency) {
            macros.unit = currency;
        }

        ///////////////////////
        // ROUNDING STRATEGY //
        ///////////////////////

        int maxInt = properties.getMaximumIntegerDigits();
        int minInt = properties.getMinimumIntegerDigits();
        int maxFrac = properties.getMaximumFractionDigits();
        int minFrac = properties.getMinimumFractionDigits();
        int minSig = properties.getMinimumSignificantDigits();
        int maxSig = properties.getMaximumSignificantDigits();
        BigDecimal roundingIncrement = properties.getRoundingIncrement();
        MathContext mathContext = RoundingUtils.getMathContextOrUnlimited(properties);
        boolean explicitMinMaxFrac = minFrac != -1 || maxFrac != -1;
        boolean explicitMinMaxSig = minSig != -1 || maxSig != -1;
        // Resolve min/max frac for currencies, required for the validation logic and for when minFrac or
        // maxFrac was set (but not both) on a currency instance.
        // NOTE: Increments are handled in "Rounder.constructCurrency()".
        if (useCurrency) {
            if (minFrac == -1 && maxFrac == -1) {
                minFrac = currency.getDefaultFractionDigits(currencyUsage);
                maxFrac = currency.getDefaultFractionDigits(currencyUsage);
            } else if (minFrac == -1) {
                minFrac = Math.min(maxFrac, currency.getDefaultFractionDigits(currencyUsage));
            } else if (maxFrac == -1) {
                maxFrac = Math.max(minFrac, currency.getDefaultFractionDigits(currencyUsage));
            } else {
                // No-op: user override for both minFrac and maxFrac
            }
        }
        // Validate min/max int/frac.
        // For backwards compatibility, minimum overrides maximum if the two conflict.
        if (minInt == 0 && maxFrac != 0) {
            minFrac = (minFrac < 0 || (minFrac == 0 && maxInt == 0)) ? 1 : minFrac;
            maxFrac = maxFrac < 0 ? -1 : maxFrac < minFrac ? minFrac : maxFrac;
            minInt = 0;
            maxInt = maxInt < 0 ? -1 : maxInt > RoundingUtils.MAX_INT_FRAC_SIG ? -1 : maxInt;
        } else {
            // Force a digit before the decimal point.
            minFrac = minFrac < 0 ? 0 : minFrac;
            maxFrac = maxFrac < 0 ? -1 : maxFrac < minFrac ? minFrac : maxFrac;
            minInt = minInt <= 0 ? 1 : minInt > RoundingUtils.MAX_INT_FRAC_SIG ? 1 : minInt;
            maxInt = maxInt < 0 ? -1
                    : maxInt < minInt ? minInt : maxInt > RoundingUtils.MAX_INT_FRAC_SIG ? -1 : maxInt;
        }
        Precision rounding = null;
        if (explicitCurrencyUsage) {
            rounding = Precision.constructCurrency(currencyUsage).withCurrency(currency);
        } else if (roundingIncrement != null) {
            if (PatternStringUtils.ignoreRoundingIncrement(roundingIncrement, maxFrac)) {
                rounding = Precision.constructFraction(minFrac, maxFrac);
            } else {
                if (minFrac > roundingIncrement.scale()) {
                    roundingIncrement = roundingIncrement.setScale(minFrac);
                }
                rounding = Precision.constructIncrement(roundingIncrement);
            }
        } else if (explicitMinMaxSig) {
            minSig = minSig < 1 ? 1
                    : minSig > RoundingUtils.MAX_INT_FRAC_SIG ? RoundingUtils.MAX_INT_FRAC_SIG : minSig;
            maxSig = maxSig < 0 ? RoundingUtils.MAX_INT_FRAC_SIG
                    : maxSig < minSig ? minSig
                            : maxSig > RoundingUtils.MAX_INT_FRAC_SIG ? RoundingUtils.MAX_INT_FRAC_SIG
                                    : maxSig;
            rounding = Precision.constructSignificant(minSig, maxSig);
        } else if (explicitMinMaxFrac) {
            rounding = Precision.constructFraction(minFrac, maxFrac);
        } else if (useCurrency) {
            rounding = Precision.constructCurrency(currencyUsage);
        }
        if (rounding != null) {
            rounding = rounding.withMode(mathContext);
            macros.precision = rounding;
        }

        ///////////////////
        // INTEGER WIDTH //
        ///////////////////

        macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(maxInt);

        ///////////////////////
        // GROUPING STRATEGY //
        ///////////////////////

        macros.grouping = Grouper.forProperties(properties);

        /////////////
        // PADDING //
        /////////////

        if (properties.getFormatWidth() > 0) {
            macros.padder = Padder.forProperties(properties);
        }

        ///////////////////////////////
        // DECIMAL MARK ALWAYS SHOWN //
        ///////////////////////////////

        macros.decimal = properties.getDecimalSeparatorAlwaysShown() ? DecimalSeparatorDisplay.ALWAYS
                : DecimalSeparatorDisplay.AUTO;

        ///////////////////////
        // SIGN ALWAYS SHOWN //
        ///////////////////////

        macros.sign = properties.getSignAlwaysShown() ? SignDisplay.ALWAYS : SignDisplay.AUTO;

        /////////////////////////
        // SCIENTIFIC NOTATION //
        /////////////////////////

        if (properties.getMinimumExponentDigits() != -1) {
            // Scientific notation is required.
            // This whole section feels like a hack, but it is needed for regression tests.
            // The mapping from property bag to scientific notation is nontrivial due to LDML rules.
            if (maxInt > 8) {
                // But #13110: The maximum of 8 digits has unknown origins and is not in the spec.
                // If maxInt is greater than 8, it is set to minInt, even if minInt is greater than 8.
                maxInt = minInt;
                macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(maxInt);
            } else if (maxInt > minInt && minInt > 1) {
                // Bug #13289: if maxInt > minInt > 1, then minInt should be 1.
                minInt = 1;
                macros.integerWidth = IntegerWidth.zeroFillTo(minInt).truncateAt(maxInt);
            }
            int engineering = maxInt < 0 ? -1 : maxInt;
            macros.notation = new ScientificNotation(
                    // Engineering interval:
                    engineering,
                    // Enforce minimum integer digits (for patterns like "000.00E0"):
                    (engineering == minInt),
                    // Minimum exponent digits:
                    properties.getMinimumExponentDigits(),
                    // Exponent sign always shown:
                    properties.getExponentSignAlwaysShown() ? SignDisplay.ALWAYS : SignDisplay.AUTO);
            // Scientific notation also involves overriding the rounding mode.
            // TODO: Overriding here is a bit of a hack. Should this logic go earlier?
            if (macros.precision instanceof FractionPrecision) {
                // For the purposes of rounding, get the original min/max int/frac, since the local
                // variables have been manipulated for display purposes.
                int maxInt_ = properties.getMaximumIntegerDigits();
                int minInt_ = properties.getMinimumIntegerDigits();
                int minFrac_ = properties.getMinimumFractionDigits();
                int maxFrac_ = properties.getMaximumFractionDigits();
                if (minInt_ == 0 && maxFrac_ == 0) {
                    // Patterns like "#E0" and "##E0", which mean no rounding!
                    macros.precision = Precision.constructInfinite().withMode(mathContext);
                } else if (minInt_ == 0 && minFrac_ == 0) {
                    // Patterns like "#.##E0" (no zeros in the mantissa), which mean round to maxFrac+1
                    macros.precision = Precision.constructSignificant(1, maxFrac_ + 1).withMode(mathContext);
                } else {
                    int maxSig_ = minInt_ + maxFrac_;
                    // Bug #20058: if maxInt_ > minInt_ > 1, then minInt_ should be 1.
                    if (maxInt_ > minInt_ && minInt_ > 1) {
                        minInt_ = 1;
                    }
                    int minSig_ = minInt_ + minFrac_;
                    // To avoid regression, maxSig is not reset when minInt_ set to 1.
                    // TODO: Reset maxSig_ = 1 + minFrac_ to follow the spec.
                    macros.precision = Precision.constructSignificant(minSig_, maxSig_)
                            .withMode(mathContext);
                }
            }
        }

        //////////////////////
        // COMPACT NOTATION //
        //////////////////////

        if (properties.getCompactStyle() != null) {
            if (properties.getCompactCustomData() != null) {
                macros.notation = new CompactNotation(properties.getCompactCustomData());
            } else if (properties.getCompactStyle() == CompactStyle.LONG) {
                macros.notation = Notation.compactLong();
            } else {
                macros.notation = Notation.compactShort();
            }
        }

        /////////////////
        // MULTIPLIERS //
        /////////////////

        macros.scale = RoundingUtils.scaleFromProperties(properties);

        //////////////////////
        // PROPERTY EXPORTS //
        //////////////////////

        if (exportedProperties != null) {

            exportedProperties.setCurrency(currency);
            exportedProperties.setMathContext(mathContext);
            exportedProperties.setRoundingMode(mathContext.getRoundingMode());
            exportedProperties.setMinimumIntegerDigits(minInt);
            exportedProperties.setMaximumIntegerDigits(maxInt == -1 ? Integer.MAX_VALUE : maxInt);

            Precision rounding_;
            if (rounding instanceof CurrencyPrecision) {
                rounding_ = ((CurrencyPrecision) rounding).withCurrency(currency);
            } else {
                rounding_ = rounding;
            }
            int minFrac_ = minFrac;
            int maxFrac_ = maxFrac;
            int minSig_ = minSig;
            int maxSig_ = maxSig;
            BigDecimal increment_ = null;
            if (rounding_ instanceof FractionRounderImpl) {
                minFrac_ = ((FractionRounderImpl) rounding_).minFrac;
                maxFrac_ = ((FractionRounderImpl) rounding_).maxFrac;
            } else if (rounding_ instanceof IncrementRounderImpl) {
                increment_ = ((IncrementRounderImpl) rounding_).increment;
                minFrac_ = increment_.scale();
                maxFrac_ = increment_.scale();
            } else if (rounding_ instanceof SignificantRounderImpl) {
                minSig_ = ((SignificantRounderImpl) rounding_).minSig;
                maxSig_ = ((SignificantRounderImpl) rounding_).maxSig;
            }

            exportedProperties.setMinimumFractionDigits(minFrac_);
            exportedProperties.setMaximumFractionDigits(maxFrac_);
            exportedProperties.setMinimumSignificantDigits(minSig_);
            exportedProperties.setMaximumSignificantDigits(maxSig_);
            exportedProperties.setRoundingIncrement(increment_);
        }

        return macros;
    }
}
