// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "measunit_impl.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/stringpiece.h"
#include "unitconverter.h"

U_NAMESPACE_BEGIN

namespace {
/**
 * Extracts the compound base unit of a compound unit (`source`). For example, if the source unit is
 * `square-mile-per-hour`, the compound base unit will be `square-meter-per-second`
 */
MeasureUnit extractCompoundBaseUnit(const MeasureUnit &source, const ConversionRates &conversionRates,
                                    UErrorCode &status) {
    MeasureUnit result;
    int32_t count;
    const auto singleUnits = source.splitToSingleUnits(count, status);
    if (U_FAILURE(status)) return result;

    for (int i = 0; i < count; ++i) {
        const auto &singleUnit = singleUnits[i];
        // Extract `ConversionRateInfo` using the absolute unit. For example: in case of `square-meter`,
        // we will use `meter`
        const auto singleUnitImpl = SingleUnitImpl::forMeasureUnit(singleUnit, status);
        const auto rateInfo = conversionRates.extractConversionInfo(singleUnitImpl.getSimpleUnitID(), status);
        if (U_FAILURE(status)) return result;
        if (rateInfo == nullptr) {
            status = U_INTERNAL_PROGRAM_ERROR;
            return result;
        }

        // Multiply the power of the singleUnit by the power of the baseUnit. For example, square-hectare
        // must be p4-meter. (NOTE: hectare --> square-meter)
        auto compoundBaseUnit = MeasureUnit::forIdentifier(rateInfo->baseUnit.toStringPiece(), status);

        int32_t baseUnitsCount;
        auto baseUnits = compoundBaseUnit.splitToSingleUnits(baseUnitsCount, status);
        for (int j = 0; j < baseUnitsCount; j++) {
            int8_t newDimensionality =
                baseUnits[j].getDimensionality(status) * singleUnit.getDimensionality(status);
            result = result.product(baseUnits[j].withDimensionality(newDimensionality, status), status);

            if (U_FAILURE(status)) { return result; }
        }
    }

    return result;
}

} // namespace

UnitsConvertibilityState U_I18N_API checkConvertibility(const MeasureUnit &source,
                                                        const MeasureUnit &target,
                                                        const ConversionRates &conversionRates,
                                                        UErrorCode &status) {
    auto sourceBaseUnit = extractCompoundBaseUnit(source, conversionRates, status);
    auto targetBaseUnit = extractCompoundBaseUnit(target, conversionRates, status);

    if (U_FAILURE(status)) return UNCONVERTIBLE;

    if (sourceBaseUnit == targetBaseUnit) return CONVERTIBLE;
    if (sourceBaseUnit == targetBaseUnit.reciprocal(status)) return RECIPROCAL;

    return UNCONVERTIBLE;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
