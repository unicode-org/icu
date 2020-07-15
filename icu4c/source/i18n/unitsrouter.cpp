// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "cmemory.h"
#include "unicode/measure.h"
#include "unitconverter.h"
#include "unitsdata.h"
#include "unitsrouter.h"

U_NAMESPACE_BEGIN
namespace units {

UnitsRouter::UnitsRouter(MeasureUnit inputUnit, StringPiece region, StringPiece usage,
                         UErrorCode &status) {
    // TODO: do we want to pass in ConversionRates and UnitPreferences instead
    // of loading in each UnitsRouter instance? (Or make global?)
    ConversionRates conversionRates(status);
    UnitPreferences prefs(status);

    MeasureUnit baseUnit = extractCompoundBaseUnit(inputUnit, conversionRates, status);
    CharString category = getUnitCategory(baseUnit.getIdentifier(), status);

    const UnitPreference *const *unitPreferences;
    int32_t preferencesCount;
    prefs.getPreferencesFor(category.data(), usage, region, unitPreferences, preferencesCount, status);

    for (int i = 0; i < preferencesCount; ++i) {
        const auto &preference = *unitPreferences[i];

        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.unit.data(), status);
        if (U_FAILURE(status)) {
            return;
        }

        outputUnits_.emplaceBackAndCheckErrorCode(status, complexTargetUnit);
        converterPreferences_.emplaceBackAndCheckErrorCode(status, inputUnit, complexTargetUnit,
                                                           preference.geq, conversionRates, status);
        if (U_FAILURE(status)) {
            return;
        }
    }
}

MaybeStackVector<Measure> UnitsRouter::route(double quantity, UErrorCode &status) const {
    for (int i = 0, n = converterPreferences_.length(); i < n; i++) {
        const auto &converterPreference = *converterPreferences_[i];

        if (converterPreference.converter.greaterThanOrEqual(quantity, converterPreference.limit)) {
            return converterPreference.converter.convert(quantity, status);
        }
    }

    // In case of the `quantity` does not fit in any converter limit, use the last converter.
    const auto &lastConverter = (*converterPreferences_[converterPreferences_.length() - 1]).converter;
    return lastConverter.convert(quantity, status);
}

const MaybeStackVector<MeasureUnit> *UnitsRouter::getOutputUnits() const {
    return &outputUnits_;
}

} // namespace units
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
