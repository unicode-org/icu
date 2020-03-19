// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <utility>

#include "cmemory.h"
#include "unitsrouter.h"

U_NAMESPACE_BEGIN

namespace {
/* Internal Data */
// Preference of a single unit.
struct UnitPreference {
    StringPiece identifier;

    // Represents the limit of the largest unit in the identifier that the quantity must be greater than
    // or equal.
    // e.g. geq: 0.3 for a unit "foot-and-inch"
    double limit;
};

MaybeStackVector<UnitPreference> extractUnitPreferences(StringPiece locale, StringPiece usage,
                                                        StringPiece category) {
    MaybeStackVector<UnitPreference> result;

    // TODO(hugovdm): extract from the database all the UnitPreference for the `locale`, `category` and
    // `usage` in order.

    return result;
}

StringPiece extractUnitCategory(MeasureUnit unit) {
    StringPiece result;

    // TODO(hugovdm): extract the category of a unit from their MeasureUnits.

    return result;
}

} // namespace

UnitsRouter::UnitsRouter(MeasureUnit inputUnit, StringPiece locale, StringPiece usage,
                         UErrorCode &status) {
    StringPiece unitCategory = extractUnitCategory(inputUnit);
    MaybeStackVector<UnitPreference> preferences = extractUnitPreferences(locale, usage, unitCategory);

    for (int i = 0, n = preferences.length(); i < n; ++i) {
        const auto &preference = *preferences[i];
        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.identifier, status);
        // TODO(younies): Find a way to emplaceBack `ConverterPreference`
        // converterPreferences_.emplaceBack(
        //     std::move(ConverterPreference(inputUnit, complexTargetUnit, preference.limit, status)));
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