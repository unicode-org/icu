// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "number_mapper.h"
#include "number_patternstring.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;


UnlocalizedNumberFormatter NumberPropertyMapper::create(const DecimalFormatProperties& properties,
                                                        const DecimalFormatSymbols& symbols,
                                                        DecimalFormatWarehouse& warehouse,
                                                        UErrorCode& status) {
    return NumberFormatter::with().macros(oldToNew(properties, symbols, warehouse, nullptr, status));
}

UnlocalizedNumberFormatter NumberPropertyMapper::create(const DecimalFormatProperties& properties,
                                                        const DecimalFormatSymbols& symbols,
                                                        DecimalFormatWarehouse& warehouse,
                                                        DecimalFormatProperties& exportedProperties,
                                                        UErrorCode& status) {
    return NumberFormatter::with().macros(
            oldToNew(
                    properties, symbols, warehouse, &exportedProperties, status));
}

MacroProps NumberPropertyMapper::oldToNew(const DecimalFormatProperties& properties,
                                          const DecimalFormatSymbols& symbols,
                                          DecimalFormatWarehouse& warehouse,
                                          DecimalFormatProperties* exportedProperties,
                                          UErrorCode& status) {
    MacroProps macros;
    Locale locale = symbols.getLocale();

    /////////////
    // SYMBOLS //
    /////////////

    macros.symbols.setTo(symbols);

    //////////////////
    // PLURAL RULES //
    //////////////////

    // TODO

    /////////////
    // AFFIXES //
    /////////////

    AffixPatternProvider* affixProvider;
    if (properties.currencyPluralInfo.fPtr.isNull()) {
        warehouse.currencyPluralInfoAPP.setToBogus();
        warehouse.propertiesAPP.setTo(properties);
        affixProvider = &warehouse.propertiesAPP;
    } else {
        warehouse.currencyPluralInfoAPP.setTo(*properties.currencyPluralInfo.fPtr);
        warehouse.propertiesAPP.setToBogus();
        affixProvider = &warehouse.currencyPluralInfoAPP;
    }
    macros.affixProvider = affixProvider;

    ///////////
    // UNITS //
    ///////////

    bool useCurrency = (
            !properties.currency.isNull() || !properties.currencyPluralInfo.fPtr.isNull() ||
            !properties.currencyUsage.isNull() || affixProvider->hasCurrencySign());
    // TODO: CustomSymbolCurrency
    CurrencyUnit currency = {u"USD", status};
    UCurrencyUsage currencyUsage = properties.currencyUsage.getOrDefault(UCURR_USAGE_STANDARD);
    if (useCurrency) {
        // NOTE: Slicing is OK.
        macros.unit = currency; // NOLINT
    }

    ///////////////////////
    // ROUNDING STRATEGY //
    ///////////////////////

    int32_t maxInt = properties.maximumIntegerDigits;
    int32_t minInt = properties.minimumIntegerDigits;
    int32_t maxFrac = properties.maximumFractionDigits;
    int32_t minFrac = properties.minimumFractionDigits;
    int32_t minSig = properties.minimumSignificantDigits;
    int32_t maxSig = properties.maximumSignificantDigits;
    double roundingIncrement = properties.roundingIncrement;
    RoundingMode roundingMode = properties.roundingMode.getOrDefault(UNUM_ROUND_HALFEVEN);
    bool explicitMinMaxFrac = minFrac != -1 || maxFrac != -1;
    bool explicitMinMaxSig = minSig != -1 || maxSig != -1;
    // Resolve min/max frac for currencies, required for the validation logic and for when minFrac or
    // maxFrac was
    // set (but not both) on a currency instance.
    // NOTE: Increments are handled in "Rounder.constructCurrency()".
    if (useCurrency && (minFrac == -1 || maxFrac == -1)) {
        int32_t digits = ucurr_getDefaultFractionDigitsForUsage(
                currency.getISOCurrency(), currencyUsage, &status);
        if (minFrac == -1 && maxFrac == -1) {
            minFrac = digits;
            maxFrac = digits;
        } else if (minFrac == -1) {
            minFrac = std::min(maxFrac, digits);
        } else /* if (maxFrac == -1) */ {
            maxFrac = std::max(minFrac, digits);
        }
    }
    // Validate min/max int/frac.
    // For backwards compatibility, minimum overrides maximum if the two conflict.
    // The following logic ensures that there is always a minimum of at least one digit.
    if (minInt == 0 && maxFrac != 0) {
        // Force a digit after the decimal point.
        minFrac = minFrac <= 0 ? 1 : minFrac;
        maxFrac = maxFrac < 0 ? -1 : maxFrac < minFrac ? minFrac : maxFrac;
        minInt = 0;
        maxInt = maxInt < 0 ? -1 : maxInt > kMaxIntFracSig ? -1 : maxInt;
    } else {
        // Force a digit before the decimal point.
        minFrac = minFrac < 0 ? 0 : minFrac;
        maxFrac = maxFrac < 0 ? -1 : maxFrac < minFrac ? minFrac : maxFrac;
        minInt = minInt <= 0 ? 1 : minInt > kMaxIntFracSig ? 1 : minInt;
        maxInt = maxInt < 0 ? -1 : maxInt < minInt ? minInt : maxInt > kMaxIntFracSig ? -1 : maxInt;
    }
    Rounder rounding;
    if (!properties.currencyUsage.isNull()) {
        rounding = Rounder::constructCurrency(currencyUsage).withCurrency(currency);
    } else if (roundingIncrement != 0.0) {
        rounding = Rounder::constructIncrement(roundingIncrement, minFrac);
    } else if (explicitMinMaxSig) {
        minSig = minSig < 1 ? 1 : minSig > kMaxIntFracSig ? kMaxIntFracSig : minSig;
        maxSig = maxSig < 0 ? kMaxIntFracSig : maxSig < minSig ? minSig : maxSig > kMaxIntFracSig
                                                                          ? kMaxIntFracSig : maxSig;
        rounding = Rounder::constructSignificant(minSig, maxSig);
    } else if (explicitMinMaxFrac) {
        rounding = Rounder::constructFraction(minFrac, maxFrac);
    } else if (useCurrency) {
        rounding = Rounder::constructCurrency(currencyUsage);
    }
    if (!rounding.isBogus()) {
        rounding = rounding.withMode(roundingMode);
        macros.rounder = rounding;
    }

    ///////////////////
    // INTEGER WIDTH //
    ///////////////////

    macros.integerWidth = IntegerWidth::zeroFillTo(minInt).truncateAt(maxInt);

    ///////////////////////
    // GROUPING STRATEGY //
    ///////////////////////

    macros.grouper = Grouper::forProperties(properties);

    /////////////
    // PADDING //
    /////////////

    if (properties.formatWidth != -1) {
        macros.padder = Padder::forProperties(properties);
    }

    ///////////////////////////////
    // DECIMAL MARK ALWAYS SHOWN //
    ///////////////////////////////

    macros.decimal = properties.decimalSeparatorAlwaysShown ? UNUM_DECIMAL_SEPARATOR_ALWAYS
                                                            : UNUM_DECIMAL_SEPARATOR_AUTO;

    ///////////////////////
    // SIGN ALWAYS SHOWN //
    ///////////////////////

    macros.sign = properties.signAlwaysShown ? UNUM_SIGN_ALWAYS : UNUM_SIGN_AUTO;

    /////////////////////////
    // SCIENTIFIC NOTATION //
    /////////////////////////

    if (properties.minimumExponentDigits != -1) {
        // Scientific notation is required.
        // This whole section feels like a hack, but it is needed for regression tests.
        // The mapping from property bag to scientific notation is nontrivial due to LDML rules.
        if (maxInt > 8) {
            // But #13110: The maximum of 8 digits has unknown origins and is not in the spec.
            // If maxInt is greater than 8, it is set to minInt, even if minInt is greater than 8.
            maxInt = minInt;
            macros.integerWidth = IntegerWidth::zeroFillTo(minInt).truncateAt(maxInt);
        } else if (maxInt > minInt && minInt > 1) {
            // Bug #13289: if maxInt > minInt > 1, then minInt should be 1.
            minInt = 1;
            macros.integerWidth = IntegerWidth::zeroFillTo(minInt).truncateAt(maxInt);
        }
        int engineering = maxInt < 0 ? -1 : maxInt;
        macros.notation = ScientificNotation(
                // Engineering interval:
                static_cast<int8_t>(engineering),
                // Enforce minimum integer digits (for patterns like "000.00E0"):
                (engineering == minInt),
                // Minimum exponent digits:
                static_cast<digits_t>(properties.minimumExponentDigits),
                // Exponent sign always shown:
                properties.exponentSignAlwaysShown ? UNUM_SIGN_ALWAYS : UNUM_SIGN_AUTO);
        // Scientific notation also involves overriding the rounding mode.
        // TODO: Overriding here is a bit of a hack. Should this logic go earlier?
        if (macros.rounder.fType == Rounder::RounderType::RND_FRACTION) {
            // For the purposes of rounding, get the original min/max int/frac, since the local
            // variables
            // have been manipulated for display purposes.
            int minInt_ = properties.minimumIntegerDigits;
            int minFrac_ = properties.minimumFractionDigits;
            int maxFrac_ = properties.maximumFractionDigits;
            if (minInt_ == 0 && maxFrac_ == 0) {
                // Patterns like "#E0" and "##E0", which mean no rounding!
                macros.rounder = Rounder::unlimited().withMode(roundingMode);
            } else if (minInt_ == 0 && minFrac_ == 0) {
                // Patterns like "#.##E0" (no zeros in the mantissa), which mean round to maxFrac+1
                macros.rounder = Rounder::constructSignificant(1, maxFrac_ + 1).withMode(roundingMode);
            } else {
                // All other scientific patterns, which mean round to minInt+maxFrac
                macros.rounder = Rounder::constructSignificant(minInt_ + minFrac_, minInt_ + maxFrac_)
                        .withMode(roundingMode);
            }
        }
    }

    //////////////////////
    // COMPACT NOTATION //
    //////////////////////

    if (!properties.compactStyle.isNull()) {
        if (properties.compactStyle.getNoError() == UNumberCompactStyle::UNUM_LONG) {
            macros.notation = Notation::compactLong();
        } else {
            macros.notation = Notation::compactShort();
        }
        // Do not forward the affix provider.
        macros.affixProvider = nullptr;
    }

    /////////////////
    // MULTIPLIERS //
    /////////////////

    if (properties.magnitudeMultiplier != 0) {
        macros.multiplier = MultiplierImpl::magnitude(properties.magnitudeMultiplier);
    } else if (properties.multiplier != 1) {
        macros.multiplier = MultiplierImpl::integer(properties.multiplier);
    }

    //////////////////////
    // PROPERTY EXPORTS //
    //////////////////////

    if (exportedProperties != nullptr) {

        exportedProperties->roundingMode = roundingMode;
        exportedProperties->minimumIntegerDigits = minInt;
        exportedProperties->maximumIntegerDigits = maxInt == -1 ? INT32_MAX : maxInt;

        Rounder rounding_;
        if (rounding.fType == Rounder::RounderType::RND_CURRENCY) {
            rounding_ = rounding.withCurrency(currency, status);
        } else {
            rounding_ = rounding;
        }
        int minFrac_ = minFrac;
        int maxFrac_ = maxFrac;
        int minSig_ = minSig;
        int maxSig_ = maxSig;
        double increment_ = 0.0;
        if (rounding_.fType == Rounder::RounderType::RND_FRACTION) {
            minFrac_ = rounding_.fUnion.fracSig.fMinFrac;
            maxFrac_ = rounding_.fUnion.fracSig.fMaxFrac;
        } else if (rounding_.fType == Rounder::RounderType::RND_INCREMENT) {
            increment_ = rounding_.fUnion.increment.fIncrement;
            minFrac_ = rounding_.fUnion.increment.fMinFrac;
            maxFrac_ = rounding_.fUnion.increment.fMinFrac;
        } else if (rounding_.fType == Rounder::RounderType::RND_SIGNIFICANT) {
            minSig_ = rounding_.fUnion.fracSig.fMinSig;
            maxSig_ = rounding_.fUnion.fracSig.fMaxSig;
        }

        exportedProperties->minimumFractionDigits = minFrac_;
        exportedProperties->maximumFractionDigits = maxFrac_;
        exportedProperties->minimumSignificantDigits = minSig_;
        exportedProperties->maximumSignificantDigits = maxSig_;
        exportedProperties->roundingIncrement = increment_;
    }

    return macros;
}


void PropertiesAffixPatternProvider::setTo(const DecimalFormatProperties& properties) {
    // TODO
}

bool PropertiesAffixPatternProvider::hasBody() const {
    return true;
}


void CurrencyPluralInfoAffixProvider::setTo(const CurrencyPluralInfo& cpi) {
    // TODO
}

bool CurrencyPluralInfoAffixProvider::hasBody() const {
    return affixesByPlural[StandardPlural::OTHER].hasBody();
}


#endif /* #if !UCONFIG_NO_FORMATTING */
