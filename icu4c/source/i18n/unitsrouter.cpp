// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <utility>

#include "cmemory.h"
#include "cstring.h"
#include "number_decimalquantity.h"
#include "resource.h"
#include "unitsrouter.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

// TODO: unused parameter 'locale' [-Wunused-parameter]
UnitsRouter::UnitsRouter(MeasureUnit inputUnit, StringPiece locale, StringPiece usage,
                         UErrorCode &status) {
    // StringPiece unitCategory = extractUnitCategory(inputUnit);
    // MaybeStackVector<UnitPreference> preferences = extractUnitPreferences(locale, usage,
    // unitCategory);
    const char *region = "001"; // FIXME extract from locale.
    const char *category = "length"; // FIXME(hugovdm) extract from inputUnit.
    MeasureUnit baseUnit;
    ConversionRates conversionRates(status);
    UnitPreferences prefs(status);

    const UnitPreference *const *unitPreferences;
    int32_t preferencesCount;
    prefs.getPreferencesFor(category, usage.data(), region, unitPreferences, preferencesCount, status);

    for (int i = 0; i < preferencesCount; ++i) {
        const auto &preference = *unitPreferences[i];
        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.unit.data(), status);

        converterPreferences_.emplaceBack(inputUnit, complexTargetUnit, preference.geq, conversionRates,
                                          status);
    }
}

MaybeStackVector<Measure> UnitsRouter::route(double quantity, UErrorCode &status) {
    for (int i = 0, n = converterPreferences_.length() - 1; i < n; i++) {
        const auto &converterPreference = *converterPreferences_[i];

        if (converterPreference.converter.greaterThanOrEqual(quantity, converterPreference.limit)) {
            return converterPreference.converter.convert(quantity, status);
        }
    }

    const auto &converterPreference =
        *converterPreferences_[converterPreferences_.length() - 1]; // Last Element
    return converterPreference.converter.convert(quantity, status);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
