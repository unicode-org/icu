// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#if !UCONFIG_NO_FORMATTING

#include "number_usageprefs.h"
#include "number_decimalquantity.h"
#include "number_microprops.h"
#include "unicode/numberformatter.h"

using namespace icu::number::impl;

UsagePrefsHandler::UsagePrefsHandler(const Locale &locale,
                                     const MeasureUnit inputUnit,
                                     const StringPiece usage,
                                     const MicroPropsGenerator *parent,
                                     UErrorCode &status)
    : fUnitsRouter(inputUnit, StringPiece(locale.getCountry()), usage, status),
      fParent(parent) {
}
// Is this not better? :
// : fUnitsRouter(inputUnit, locale, usage, status), fParent(parent) {}

void UsagePrefsHandler::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                        UErrorCode &status) const {
    fParent->processQuantity(quantity, micros, status);
    if (U_FAILURE(status)) {
        return;
    }

    quantity.roundToInfinity(); // Enables toDouble
    auto routed = fUnitsRouter.route(quantity.toDouble(), status);
    micros.helpers.outputUnit = routed[0]->getUnit();
    quantity.setToDouble(routed[0]->getNumber().getDouble());

    // TODO(units): here we are always overriding Precision. (1) get precision
    // from fUnitsRouter, (2) ensure we use the UnitPreference skeleton's
    // precision only when there isn't an explicit override we prefer to use.
    // This needs to be handled within
    // NumberFormatterImpl::macrosToMicroGenerator in number_formatimpl.cpp
    Precision precision = Precision::integer().withMinDigits(2);
    UNumberFormatRoundingMode roundingMode;
    // Temporary until ICU 64?
    roundingMode = precision.fRoundingMode;
    CurrencyUnit currency(u"", status);
    micros.rounder = {precision, roundingMode, currency, status};
    if (U_FAILURE(status)) {
        return;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
