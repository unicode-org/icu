// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <stdio.h>
#include <utility>

#include "cmemory.h"
#include "cstring.h"
#include "number_decimalquantity.h"
#include "resource.h"
#include "unitconverter.h" // for extractCompoundBaseUnit
#include "unitsdata.h"     // for getUnitCategory
#include "unitsrouter.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

UnitsRouter::UnitsRouter(MeasureUnit inputUnit, StringPiece region, StringPiece usage,
                         UErrorCode &status) {
    // TODO: do we want to pass in ConversionRates and UnitPreferences instead
    // of loading in each UnitsRouter instance? (Or make global?)
    ConversionRates conversionRates(status);
    UnitPreferences prefs(status);

    MeasureUnit baseUnit = extractCompoundBaseUnit(inputUnit, conversionRates, status);
    CharString category = getUnitCategory(baseUnit.getIdentifier(), status);

    // TODO: deal correctly with StringPiece / null-terminated string incompatibility...
    const UnitPreference *const *unitPreferences;
    int32_t preferencesCount;
    prefs.getPreferencesFor(category.data(), usage, region, unitPreferences, preferencesCount, status);

    for (int i = 0; i < preferencesCount; ++i) {
        const auto &preference = *unitPreferences[i];

        fprintf(stderr, "testtestt   %s,     %f  \n", preference.unit.data(), preference.geq);

        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.unit.data(), status);

        if (U_FAILURE(status)) { return; }

        converterPreferences_.emplaceBack(inputUnit, complexTargetUnit, preference.geq, conversionRates,
                                          status);
        if (U_FAILURE(status)) {
            fprintf(
                stderr,
                "FAILED: converterPreferences_.emplaceBack(<%s>, <%s>, %f, conversionRates, status)\n",
                inputUnit.getIdentifier(), complexTargetUnit.getIdentifier(), preference.geq);
            return;
        }
    }
}

MaybeStackVector<Measure> UnitsRouter::route(double quantity, UErrorCode &status) {
    for (int i = 0, n = converterPreferences_.length(); i < n; i++) {

        const auto &converterPreference = *converterPreferences_[i];

        if (i == n - 1) { // Last element
            return converterPreference.converter.convert(quantity, status);
        }

        if (converterPreference.converter.greaterThanOrEqual(quantity, converterPreference.limit)) {
            return converterPreference.converter.convert(quantity, status);
        }
    }
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
