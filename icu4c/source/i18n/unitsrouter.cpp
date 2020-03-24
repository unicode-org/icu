// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <utility>

#include "cmemory.h"
#include "cstring.h"
#include "getunitsdata.h"
#include "number_decimalquantity.h"
#include "resource.h"
#include "unitsrouter.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

UnitsRouter::UnitsRouter(MeasureUnit inputUnit, StringPiece locale, StringPiece usage,
                         UErrorCode &status) {
    // StringPiece unitCategory = extractUnitCategory(inputUnit);
    // MaybeStackVector<UnitPreference> preferences = extractUnitPreferences(locale, usage,
    // unitCategory);
    const char *region = "001"; // FIXME extract from locale.
    CharString category;
    MeasureUnit baseUnit;
    MaybeStackVector<ConversionRateInfo> conversionRates;
    MaybeStackVector<UnitPreference> unitPreferences;
    getUnitsData(region, usage.data(), inputUnit, category, baseUnit, conversionRates, unitPreferences,
                 status);

    for (int i = 0, n = unitPreferences.length(); i < n; ++i) {
        const auto &preference = *unitPreferences[i];
        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.unit.data(), status);
        // TODO(younies): Find a way to emplaceBack `ConverterPreference`
        // converterPreferences_.emplaceBack(
        //     std::move(ConverterPreference(inputUnit, complexTargetUnit, preference.geq, status)));
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
