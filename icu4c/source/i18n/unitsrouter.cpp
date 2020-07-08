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

    const UnitPreference *const *unitPreferences;
    int32_t preferencesCount;
    prefs.getPreferencesFor(category.data(), usage, region, unitPreferences, preferencesCount, status);

    for (int i = 0; i < preferencesCount; ++i) {
        const auto &preference = *unitPreferences[i];

        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.unit.data(), status);
        if (U_FAILURE(status)) {
            return;
        }

        number::impl::MacroProps macroProps;
        int32_t errOffset;
        if (preference.skeleton.length() == 0) {
            UnicodeString skeletonUniStr("precision-increment/1");
            macroProps = number::impl::skeleton::parseSkeleton(skeletonUniStr, errOffset, status);
            if (U_FAILURE(status)) {
                return;
            }
        } else {
            // TODO: check that the skeleton is in the following format "precision-increment/d*.d*"
            UnicodeString skeletonUniStr(preference.skeleton.data());
            macroProps = number::impl::skeleton::parseSkeleton(skeletonUniStr, errOffset, status);
            if (U_FAILURE(status)) {
                return;
            }
        }

        converterPreferences_.emplaceBackAndCheckErrorCode(status, inputUnit, complexTargetUnit,
                                                           preference.geq, macroProps.precision,
                                                           conversionRates, status);

        if (U_FAILURE(status)) {
            return;
        }
    }
}

RouteResult UnitsRouter::route(double quantity, UErrorCode &status) const {
    for (int i = 0, n = converterPreferences_.length(); i < n; i++) {
        const auto &converterPreference = *converterPreferences_[i];

        if (converterPreference.converter.greaterThanOrEqual(quantity, converterPreference.limit)) {
            return RouteResult{
                converterPreference.converter.convert(quantity, status), //
                converterPreference.precision                            //
            };
        }
    }

    // In case of the `quantity` does not fit in any converter limit, use the last converter.
    const auto &lastConverterPreference = (*converterPreferences_[converterPreferences_.length() - 1]);
    return RouteResult{
        lastConverterPreference.converter.convert(quantity, status), //
        lastConverterPreference.precision                            //
    };
}

const MaybeStackVector<MeasureUnit> *UnitsRouter::getOutputUnits() const {
    return &outputUnits_;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
