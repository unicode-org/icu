// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <cmath>

#include "cmemory.h"
#include "number_decimalquantity.h"
#include "number_roundingutils.h"
#include "uarrsort.h"
#include "uassert.h"
#include "unicode/fmtable.h"
#include "unicode/localpointer.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "units_complexconverter.h"
#include "units_converter.h"

U_NAMESPACE_BEGIN
namespace units {

ComplexUnitsConverter::ComplexUnitsConverter(const MeasureUnitImpl &inputUnit,
                                             const MeasureUnitImpl &outputUnits,
                                             const ConversionRates &ratesInfo, UErrorCode &status)
    : units_(outputUnits.extractIndividualUnitsWithIndecies(status)) {
    if (U_FAILURE(status)) {
        return;
    }

    U_ASSERT(units_.length() != 0);

    // NOTE:
    //  This comparator is used to sort the units in a descending order. Therefore, we return -1 if
    //  the left is bigger than right and so on.
    auto descendingCompareUnits = [](const void *context, const void *left, const void *right) {
        UErrorCode status = U_ZERO_ERROR;

        const auto *leftPointer =
            static_cast<const std::pair<int32_t, MeasureUnitImpl *> *const *>(left);
        const auto *rightPointer =
            static_cast<const std::pair<int32_t, MeasureUnitImpl *> *const *>(right);

        // Return -ve the result because we are sorting in descending order.
        return -1 * UnitConverter::compareTwoUnits(*((**leftPointer).second) /* left unit*/,       //
                                                   *((**rightPointer).second) /* right unit */,    //
                                                   *static_cast<const ConversionRates *>(context), //
                                                   status);
    };

    uprv_sortArray(units_.getAlias(),                                                                  //
                   units_.length(),                                                                    //
                   sizeof units_[0], /* NOTE: we have already asserted that the units_ is not empty.*/ //
                   descendingCompareUnits,                                                             //
                   &ratesInfo,                                                                         //
                   false,                                                                              //
                   &status                                                                             //
    );

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
    for (int i = 0, n = units_.length(); i < n; i++) {
        if (i == 0) { // first element
            unitConverters_.emplaceBackAndCheckErrorCode(status, inputUnit, *(units_[i]->second), ratesInfo,
                                                         status);
        } else {
            unitConverters_.emplaceBackAndCheckErrorCode(status, *(units_[i - 1]->second), *(units_[i]->second), ratesInfo,
                                                         status);
        }

        if (U_FAILURE(status)) {
            return;
        }
    }
}

UBool ComplexUnitsConverter::greaterThanOrEqual(double quantity, double limit) const {
    U_ASSERT(unitConverters_.length() > 0);

    // First converter converts to the biggest quantity.
    double newQuantity = unitConverters_[0]->convert(quantity);
    return newQuantity >= limit;
}

MaybeStackVector<Measure> ComplexUnitsConverter::convert(double quantity,
                                                         icu::number::impl::RoundingImpl *rounder,
                                                         UErrorCode &status) const {
    // TODO(hugovdm): return an error for "foot-and-foot"?
    MaybeStackVector<Measure> result;
    int sign = 1;
    if (quantity < 0) {
        quantity *= -1;
        sign = -1;
    }

    // For N converters:
    // - the first converter converts from the input unit to the largest unit,
    // - the following N-2 converters convert to bigger units for which we want integers,
    // - the Nth converter (index N-1) converts to the smallest unit, for which
    //   we keep a double.
    MaybeStackArray<int64_t, 5> intValues(unitConverters_.length() - 1, status);
    if (U_FAILURE(status)) {
        return result;
    }
    uprv_memset(intValues.getAlias(), 0, (unitConverters_.length() - 1) * sizeof(int64_t));

    for (int i = 0, n = unitConverters_.length(); i < n; ++i) {
        quantity = (*unitConverters_[i]).convert(quantity);
        if (i < n - 1) {
            // The double type has 15 decimal digits of precision. For choosing
            // whether to use the current unit or the next smaller unit, we
            // therefore nudge up the number with which the thresholding
            // decision is made. However after the thresholding, we use the
            // original values to ensure unbiased accuracy (to the extent of
            // double's capabilities).
            int64_t roundedQuantity = floor(quantity * (1 + DBL_EPSILON));
            intValues[i] = roundedQuantity;

            // Keep the residual of the quantity.
            //   For example: `3.6 feet`, keep only `0.6 feet`
            //
            // When the calculation is near enough +/- DBL_EPSILON, we round to
            // zero. (We also ensure no negative values here.)
            if ((quantity - roundedQuantity) / quantity < DBL_EPSILON) {
                quantity = 0;
            } else {
                quantity -= roundedQuantity;
            }
        } else { // LAST ELEMENT
            if (rounder == nullptr) {
                // Nothing to do for the last element.
                break;
            }

            // Round the last value
            // TODO(ICU-21288): get smarter about precision for mixed units.
            number::impl::DecimalQuantity quant;
            quant.setToDouble(quantity);
            rounder->apply(quant, status);
            if (U_FAILURE(status)) {
                return result;
            }
            quantity = quant.toDouble();
            if (i == 0) {
                // Last element is also the first element, so we're done
                break;
            }

            // Check if there's a carry, and bubble it back up the resulting intValues.
            int64_t carry = floor(unitConverters_[i]->convertInverse(quantity) * (1 + DBL_EPSILON));
            if (carry <= 0) {
                break;
            }
            quantity -= unitConverters_[i]->convert(carry);
            intValues[i - 1] += carry;

            // We don't use the first converter: that one is for the input unit
            for (int32_t j = i - 1; j > 0; j--) {
                carry = floor(unitConverters_[j]->convertInverse(intValues[j]) * (1 + DBL_EPSILON));
                if (carry <= 0) {
                    break;
                }
                intValues[j] -= round(unitConverters_[j]->convert(carry));
                intValues[j - 1] += carry;
            }
        }
    }

    // Package values into Measure instances in unordered_result:
    MaybeStackVector<std::pair<int32_t, Measure> > unordered_result;
    for (int i = 0, n = units_.length(); i < n; ++i) {
        if (i < n - 1) {
            Formattable formattableQuantity(intValues[i] * sign);
            // Measure takes ownership of the MeasureUnit*
            MeasureUnit *type = new MeasureUnit(units_[i]->second->copy(status).build(status));
            if (unordered_result.emplaceBackAndCheckErrorCode(status, std::make_pair(units_[i]->first,  Measure(formattableQuantity, type, status))) ==
                nullptr) {
                // Ownership wasn't taken
                U_ASSERT(U_FAILURE(status));
                delete type;
            }
            if (U_FAILURE(status)) {
                return result;
            }
        } else { // LAST ELEMENT
            // Add the last element, not an integer:
            Formattable formattableQuantity(quantity * sign);
            // Measure takes ownership of the MeasureUnit*
            MeasureUnit *type = new MeasureUnit((units_[i])->second->copy(status).build(status));
            if (unordered_result.emplaceBackAndCheckErrorCode(status, std::make_pair(units_[i]->first, Measure( formattableQuantity, type, status))) ==
                nullptr) {
                // Ownership wasn't taken
                U_ASSERT(U_FAILURE(status));
                delete type;
            }
            if (U_FAILURE(status)) {
                return result;
            }
            U_ASSERT(unordered_result.length() == i + 1);
            U_ASSERT(unordered_result[i] != nullptr);
        }
    }

    // Sort the unordered_result
    
    // NOTE:
    //  This comparator is used to sort the units in ascending order according to their indices. 
    auto ascendingOrderByIndexComparator = [](const void *, const void *left, const void *right) {
        const auto *leftPointer = static_cast<const std::pair<int32_t, MeasureUnitImpl> *const *>(left);
        const auto *rightPointer = static_cast<const std::pair<int32_t, MeasureUnitImpl> *const *>(right);

       int32_t diff = (*leftPointer)->first -(*rightPointer)->first;
       if (diff == 0) { return 0;}
       return diff > 0? 1 : -1;
    };

    uprv_sortArray(unordered_result.getAlias(), //
                   unordered_result.length(),   //
                   sizeof unordered_result[0],  //
                   ascendingOrderByIndexComparator,      //
                   nullptr,                     //
                   false,                       //
                   &status                      //
    );

    for(int32_t i = 0, n = unordered_result.length(); i < n; ++i) {
        result.emplaceBackAndCheckErrorCode(status, std::move( unordered_result[i]->second));
        if(U_FAILURE(status)) {
            return result;
        }
    }

    return result;
}

} // namespace units
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
