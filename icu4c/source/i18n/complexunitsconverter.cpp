// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <math.h>

#include "cmemory.h"
#include "complexunitsconverter.h"
#include "uassert.h"
#include "unicode/fmtable.h"
#include "unicode/localpointer.h"
#include "unicode/measure.h"
#include "unitconverter.h"

U_NAMESPACE_BEGIN
namespace units {

ComplexUnitsConverter::ComplexUnitsConverter(const MeasureUnit &inputUnit,
                                             const MeasureUnit &outputUnits,
                                             const ConversionRates &ratesInfo, UErrorCode &status) {

    if (outputUnits.getComplexity(status) != UMeasureUnitComplexity::UMEASURE_UNIT_MIXED) {
        unitConverters_.emplaceBackAndCheckErrorCode(status, inputUnit, outputUnits, ratesInfo, status);
        if (U_FAILURE(status)) {
            return;
        }

        units_.emplaceBackAndCheckErrorCode(status, outputUnits);
        return;
    }

    // In case the `outputUnits` are `UMEASURE_UNIT_MIXED` such as `foot+inch`. In this case we need more
    // converters to convert from the `inputUnit` to the first unit in the `outputUnits`. Then, a
    // converter from the first unit in the `outputUnits` to the second unit and so on.
    //      For Example:
    //          - inputUnit is `meter`
    //          - outputUnits is `foot+inch`
    //              - Therefore, we need to have two converters:
    //                      1. a converter from `meter` to `foot`
    //                      2. a converter from `foot` to `inch`
    //          - Therefore, if the input is `2 meter`:
    //              1. convert `meter` to `foot` --> 2 meter to 6.56168 feet
    //              2. convert the residual of 6.56168 feet (0.56168) to inches, which will be (6.74016
    //              inches)
    //              3. then, the final result will be (6 feet and 6.74016 inches)
    int32_t length;
    auto singleUnits = outputUnits.splitToSingleUnits(length, status);
    MaybeStackVector<MeasureUnit> singleUnitsInOrder;
    for (int i = 0; i < length; ++i) {
        /**
         *  TODO(younies): ensure units being in order by the biggest unit at first.
         * 
         * HINT:
         *  MaybeStackVector<SingleUnitImpl> singleUnitsInOrder =  MeasureUnitImpl::forMeasureUnitMaybeCopy(outputUnits, status).units;
         *      uprv_sortArray(
         *      singleUnitsInOrder.getAlias(),
         *      singleUnitsInOrder.length(),
         *      sizeof(singleUnitsInOrder[0]),
         *      compareSingleUnits,
         *      nullptr,
         *      false,
         *      &status);
         */ 
        singleUnitsInOrder.emplaceBackAndCheckErrorCode(status, singleUnits[i]);
    }

    if (singleUnitsInOrder.length() == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    for (int i = 0, n = singleUnitsInOrder.length(); i < n; i++) {
        if (i == 0) { // first element
            unitConverters_.emplaceBackAndCheckErrorCode(status, inputUnit, *singleUnitsInOrder[i],
                                                         ratesInfo, status);
        } else {
            unitConverters_.emplaceBackAndCheckErrorCode(status, *singleUnitsInOrder[i - 1],
                                                         *singleUnitsInOrder[i], ratesInfo, status);
        }

        if (U_FAILURE(status)) {
            return;
        }
    }

    units_.appendAll(singleUnitsInOrder, status);
}

UBool ComplexUnitsConverter::greaterThanOrEqual(double quantity, double limit) const {
    U_ASSERT(unitConverters_.length() > 0);

    // First converter converts to the biggest quantity.
    double newQuantity = unitConverters_[0]->convert(quantity);
    return newQuantity >= limit;
}

MaybeStackVector<Measure> ComplexUnitsConverter::convert(double quantity, UErrorCode &status) const {
    MaybeStackVector<Measure> result;

    for (int i = 0, n = unitConverters_.length(); i < n; ++i) {
        quantity = (*unitConverters_[i]).convert(quantity);
        if (i < n - 1) {
            int64_t newQuantity = floor(quantity);
            Formattable formattableNewQuantity(newQuantity);

            // NOTE: Measure would own its MeasureUnit.
            result.emplaceBackAndCheckErrorCode(status, formattableNewQuantity,
                                                new MeasureUnit(*units_[i]), status);

            // Keep the residual of the quantity.
            //   For example: `3.6 feet`, keep only `0.6 feet`
            quantity -= newQuantity;
        } else { // LAST ELEMENT
            Formattable formattableQuantity(quantity);

            // NOTE: Measure would own its MeasureUnit.
            result.emplaceBackAndCheckErrorCode(status, formattableQuantity, new MeasureUnit(*units_[i]),
                                                status);
        }
    }

    return result;
}

} // namespace units
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
