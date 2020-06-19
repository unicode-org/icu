// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#if !UCONFIG_NO_FORMATTING

#include "number_usageprefs.h"
#include "number_decimalquantity.h"
#include "number_microprops.h"

using namespace icu::number::impl;

UsagePrefsHandler::UsagePrefsHandler(const Locale &locale, const MeasureUnit inputUnit,
                                     const StringPiece usage, const MicroPropsGenerator *parent,
                                     UErrorCode &status)
    : fUnitsRouter(inputUnit, locale, usage, status), fParent(parent) {}

void UsagePrefsHandler::processQuantity(DecimalQuantity &quantity, MicroProps &micros,
                                        UErrorCode &status) const {
    fParent->processQuantity(quantity, micros, status);
    // if (U_FAILURE(status)) { return; }
    // FIXME: DO COOL STUFF!

    quantity.roundToInfinity();
    auto fixme = fUnitsRouter.route(quantity.toDouble(), status);
    // fprintf(stderr, "setting outputUnit to %s\n", fixme[0]->getUnit().getIdentifier());
    micros.helpers.outputUnit = fixme[0]->getUnit();
    quantity.setToDouble(fixme[0]->getNumber().getDouble());
    // micros.SOMETHING = fixme[0]->getUnit();
}

#endif /* #if !UCONFIG_NO_FORMATTING */
