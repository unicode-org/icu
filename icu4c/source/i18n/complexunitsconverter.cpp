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
                                             const ConversionRates &ratesInfo, UErrorCode &status) {
    int32_t length;
    auto singleUnits = outputUnits.splitToSingleUnits(length, status);
    MaybeStackVector<MeasureUnit> singleUnitsInOrder;
    for (int i = 0; i < length; ++i) {
        // TODO(younies): ensure units being in order in phase 2. Now, the units in order by default.
        singleUnitsInOrder.emplaceBack(singleUnits[i]);
    }

    *this = ComplexUnitsConverter(inputUnit, std::move(singleUnitsInOrder),
                                  ratesInfo, status);

    // TODO(younies): question from Hugo: is this check appropriate? The
    // U_ASSERT in greaterThanOrEqual suggests this should be an invariant for
    // ComplexUnitConverter.
    if (unitConverters_.length() == 0) {
        status = U_INTERNAL_PROGRAM_ERROR;
    }
}

ComplexUnitsConverter::ComplexUnitsConverter(const MeasureUnit inputUnit,
                                             const MaybeStackVector<MeasureUnit> outputUnits,
                                             const ConversionRates &ratesInfo, UErrorCode &status) {
    if (outputUnits.length() == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    for (int i = 0, n = outputUnits.length(); i < n; i++) {
      if (i == 0) { // first element
        unitConverters_.emplaceBack(inputUnit, *outputUnits[i], ratesInfo,
                                    status);

      } else {
        unitConverters_.emplaceBack(*outputUnits[i - 1], *outputUnits[i],
                                    ratesInfo, status);
      }

        if (U_FAILURE(status)) break;
    }

    if (U_FAILURE(status)) return;

    units_.appendAll(outputUnits, status);
}

UBool ComplexUnitsConverter::greaterThanOrEqual(double quantity, double limit) const {
    // TODO(younies): this assert fails for the first constructor above:
    U_ASSERT(unitConverters_.length() > 0);

    // first quantity is the biggest one.
    double newQuantity = unitConverters_[0]->convert(quantity);

    return newQuantity >= limit;
}

MaybeStackVector<Measure> ComplexUnitsConverter::convert(double quantity, UErrorCode &status) const {
    MaybeStackVector<Measure> result;

    for (int i = 0, n = unitConverters_.length(); i < n; ++i) {
        quantity = (*unitConverters_[i]).convert(quantity);
        if (i < n - 1) { // not last element
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
