// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <math.h>
#include <utility>

#include "cmemory.h"
#include "complexunitsconverter.h"
#include "uassert.h"
#include "unicode/fmtable.h"
#include "unitconverter.h"

U_NAMESPACE_BEGIN

#define EPSILON_DEN 1000000000000000000.0

ComplexUnitsConverter::ComplexUnitsConverter(const MeasureUnit inputUnit, const MeasureUnit outputUnits,
                                             const ConversionRates &ratesInfo, UErrorCode &status) {
    int32_t length;
    auto singleUnits = outputUnits.splitToSingleUnits(length, status);
    MaybeStackVector<MeasureUnit> singleUnitsInOrder;
    for (int i = 0; i < length; ++i) {
        // TODO(younies): ensure units being in order in phase 2. Now, the units in order by default.
        singleUnitsInOrder.emplaceBack(singleUnits[i]);
    }

    if (singleUnitsInOrder.length() == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    for (int i = 0, n = singleUnitsInOrder.length(); i < n; i++) {
        if (i == 0) { // first element
            unitConverters_.emplaceBack(inputUnit, *singleUnitsInOrder[i], ratesInfo, status);
        } else {
            unitConverters_.emplaceBack(*singleUnitsInOrder[i - 1], *singleUnitsInOrder[i], ratesInfo,
                                        status);
        }

        if (U_FAILURE(status)) break;
    }

    if (U_FAILURE(status)) return;

    units_.appendAll(singleUnitsInOrder, status);
}

UBool ComplexUnitsConverter::greaterThanOrEqual(double quantity, double limit) const {
    // TODO(younies): this assert fails for the first constructor above:
    U_ASSERT(unitConverters_.length() > 0);

    // first quantity is the biggest one.
    double newQuantity = unitConverters_[0]->convert(quantity);
    newQuantity = roundl(newQuantity * EPSILON_DEN) / EPSILON_DEN; // ROUND

    return newQuantity >= limit;
}

MaybeStackVector<Measure> ComplexUnitsConverter::convert(double quantity, UErrorCode &status) const {
    MaybeStackVector<Measure> result;

    for (int i = 0, n = unitConverters_.length(); i < n; ++i) {
        quantity = (*unitConverters_[i]).convert(quantity);
        if (i < n - 1) { // not last element
            // round to the nearest EPSILON
            quantity = roundl(quantity * EPSILON_DEN) / EPSILON_DEN;
            int64_t newQuantity = quantity;
            Formattable formattableNewQuantity(newQuantity);
            // Measure wants to own its MeasureUnit. For now, this copies it.
            // TODO(younies): consider whether ownership transfer would be
            // reasonable? (If not, just delete this comment?)
            result.emplaceBack(formattableNewQuantity, new MeasureUnit(*units_[i]), status);

            quantity -= newQuantity;
        } else { // Last element
            Formattable formattableQuantity(quantity);
            // Measure wants to own its MeasureUnit. For now, this copies it.
            //  TODO(younies): consider whether ownership transfer would be
            //  reasonable? (If not, just delete this comment?)
            result.emplaceBack(formattableQuantity, new MeasureUnit(*units_[i]), status);
        }
    }

    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
