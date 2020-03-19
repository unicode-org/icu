// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#include <utility>

#include "cmemory.h"
#include "complexunitsconverter.h"
#include "uassert.h"
#include "unicode/fmtable.h"
#include "unitconverter.h"

U_NAMESPACE_BEGIN

ComplexUnitsConverter::ComplexUnitsConverter(const MeasureUnit inputUnit, const MeasureUnit outputUnits,
                                             UErrorCode &status) {
    auto singleUnits = outputUnits.splitToSingleUnits(status);
    MaybeStackVector<MeasureUnit> singleUnitsInOrder;
    for (int i = 0, n = singleUnits.length(); i < n; ++i) {
        // TODO(younies): ensure units being in order in phase 2. Now, the units in order by default.
        singleUnitsInOrder.emplaceBack(singleUnits[i]);
    }

    ComplexUnitsConverter(inputUnit, std::move(singleUnitsInOrder), status);
}

ComplexUnitsConverter::ComplexUnitsConverter(const MeasureUnit inputUnit,
                                             const MaybeStackVector<MeasureUnit> outputUnits,
                                             UErrorCode &status) {
    if (outputUnits.length() == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    MaybeStackVector<UnitConverter> converters;
    for (int i = 0, n = outputUnits.length(); i < n; i++) {
        if (i == 0) { // first element
            converters.emplaceBack(UnitConverter(inputUnit, *outputUnits[i], status));

        } else {
            converters.emplaceBack(UnitConverter(*outputUnits[i - 1], *outputUnits[i], status));
        }

        if (U_FAILURE(status)) break;
    }

    if (U_FAILURE(status)) return;

    unitConverters_.appendAll(converters, status);
    units_.appendAll(outputUnits, status);
}

UBool ComplexUnitsConverter::greaterThanOrEqual(double quantity, double limit) const {
    U_ASSERT(unitConverters_.length() > 0);

    // first quantity is the biggest one.
    double newQuantity = (*unitConverters_[0]).convert(quantity);

    return newQuantity >= limit;
}

MaybeStackVector<Measure> ComplexUnitsConverter::convert(double quantity, UErrorCode &status) const {
    MaybeStackVector<Measure> result;

    for (int i = 0, n = unitConverters_.length(); i < n; ++i) {
        quantity = (*unitConverters_[i]).convert(quantity);
        if (i < n - 1) { // not last element
            int64_t newQuantity = quantity;
            Formattable formattableNewQuantity(newQuantity);
            result.emplaceBack(Measure(formattableNewQuantity, units_[i], status));

            quantity -= newQuantity;
        } else { // Last element
            Formattable formattableQuantity(quantity);
            result.emplaceBack(Measure(formattableQuantity, units_[i], status));
        }
    }

    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */