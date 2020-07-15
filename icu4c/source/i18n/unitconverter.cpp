// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "cmemory.h"
#include "double-conversion-string-to-double.h"
#include "measunit_impl.h"
#include "uassert.h"
#include "unicode/localpointer.h"
#include "unicode/measunit.h"
#include "unicode/stringpiece.h"
#include "unitconverter.h"
#include <algorithm>
#include <cmath>
#include <stdlib.h>
#include <utility>

U_NAMESPACE_BEGIN
namespace units {

void U_I18N_API Factor::multiplyBy(const Factor &rhs) {
    factorNum *= rhs.factorNum;
    factorDen *= rhs.factorDen;
    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        constants[i] += rhs.constants[i];
    }

    // NOTE
    //  We need the offset when the source and the target are simple units. e.g. the source is
    //  celsius and the target is Fahrenheit. Therefore, we just keep the value using `std::max`.
    offset = std::max(rhs.offset, offset);
}

void U_I18N_API Factor::divideBy(const Factor &rhs) {
    factorNum *= rhs.factorDen;
    factorDen *= rhs.factorNum;
    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        constants[i] -= rhs.constants[i];
    }

    // NOTE
    //  We need the offset when the source and the target are simple units. e.g. the source is
    //  celsius and the target is Fahrenheit. Therefore, we just keep the value using `std::max`.
    offset = std::max(rhs.offset, offset);
}

void U_I18N_API Factor::power(int32_t power) {
    // multiply all the constant by the power.
    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        constants[i] *= power;
    }

    bool shouldFlip = power < 0; // This means that after applying the absolute power, we should flip
                                 // the Numerator and Denominator.

    factorNum = std::pow(factorNum, std::abs(power));
    factorDen = std::pow(factorDen, std::abs(power));

    if (shouldFlip) {
        // Flip Numerator and Denominator.
        std::swap(factorNum, factorDen);
    }
}

void U_I18N_API Factor::flip() {
    std::swap(factorNum, factorDen);

    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        constants[i] *= -1;
    }
}

void U_I18N_API Factor::applySiPrefix(UMeasureSIPrefix siPrefix) {
    if (siPrefix == UMeasureSIPrefix::UMEASURE_SI_PREFIX_ONE) return; // No need to do anything

    double siApplied = std::pow(10.0, std::abs(siPrefix));

    if (siPrefix < 0) {
        factorDen *= siApplied;
        return;
    }

    factorNum *= siApplied;
}

void U_I18N_API Factor::substituteConstants() {
    // These values are a hard-coded subset of unitConstants in the units
    // resources file. A unit test checks that all constants in the resource
    // file are at least recognised by the code. Derived constants' values or
    // hard-coded derivations are not checked.
    // double constantsValues[CONSTANTS_COUNT];
    static const double constantsValues[CONSTANTS_COUNT] = {
        [CONSTANT_FT2M] = 0.3048,                  //
        [CONSTANT_PI] = 411557987.0 / 131002976.0, //
        [CONSTANT_GRAVITY] = 9.80665,              //
        [CONSTANT_G] = 6.67408E-11,                //
        [CONSTANT_GAL_IMP2M3] = 0.00454609,        //
        [CONSTANT_LB2KG] = 0.45359237,             //
    };

    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        if (this->constants[i] == 0) {
            continue;
        }

        auto absPower = std::abs(this->constants[i]);
        Signum powerSig = this->constants[i] < 0 ? Signum::NEGATIVE : Signum::POSITIVE;
        double absConstantValue = std::pow(constantsValues[i], absPower);

        if (powerSig == Signum::NEGATIVE) {
            this->factorDen *= absConstantValue;
        } else {
            this->factorNum *= absConstantValue;
        }

        this->constants[i] = 0;
    }
}

namespace {

/* Helpers */

using icu::double_conversion::StringToDoubleConverter;

// TODO: Make this a shared-utility function.
// Returns `double` from a scientific number(i.e. "1", "2.01" or "3.09E+4")
double strToDouble(StringPiece strNum, UErrorCode &status) {
    // We are processing well-formed input, so we don't need any special options to
    // StringToDoubleConverter.
    StringToDoubleConverter converter(0, 0, 0, "", "");
    int32_t count;
    double result = converter.StringToDouble(strNum.data(), strNum.length(), &count);
    if (count != strNum.length()) {
        status = U_INVALID_FORMAT_ERROR;
    }

    return result;
}

// Returns `double` from a scientific number that could has a division sign (i.e. "1", "2.01", "3.09E+4"
// or "2E+2/3")
double strHasDivideSignToDouble(StringPiece strWithDivide, UErrorCode &status) {
    int divisionSignInd = -1;
    for (int i = 0, n = strWithDivide.length(); i < n; ++i) {
        if (strWithDivide.data()[i] == '/') {
            divisionSignInd = i;
            break;
        }
    }

    if (divisionSignInd >= 0) {
        return strToDouble(strWithDivide.substr(0, divisionSignInd), status) /
               strToDouble(strWithDivide.substr(divisionSignInd + 1), status);
    }

    return strToDouble(strWithDivide, status);
}

/*
  Adds single factor to a `Factor` object. Single factor means "23^2", "23.3333", "ft2m^3" ...etc.
  However, complex factor are not included, such as "ft2m^3*200/3"
*/
void addFactorElement(Factor &factor, StringPiece elementStr, Signum signum, UErrorCode &status) {
    StringPiece baseStr;
    StringPiece powerStr;
    int32_t power =
        1; // In case the power is not written, then, the power is equal 1 ==> `ft2m^1` == `ft2m`

    // Search for the power part
    int32_t powerInd = -1;
    for (int32_t i = 0, n = elementStr.length(); i < n; ++i) {
        if (elementStr.data()[i] == '^') {
            powerInd = i;
            break;
        }
    }

    if (powerInd > -1) {
        // There is power
        baseStr = elementStr.substr(0, powerInd);
        powerStr = elementStr.substr(powerInd + 1);

        power = static_cast<int32_t>(strToDouble(powerStr, status));
    } else {
        baseStr = elementStr;
    }

    addSingleFactorConstant(baseStr, power, signum, factor, status);
}

/*
 * Extracts `Factor` from a complete string factor. e.g. "ft2m^3*1007/cup2m3*3"
 */
Factor extractFactorConversions(StringPiece stringFactor, UErrorCode &status) {
    Factor result;
    Signum signum = Signum::POSITIVE;
    auto factorData = stringFactor.data();
    for (int32_t i = 0, start = 0, n = stringFactor.length(); i < n; i++) {
        if (factorData[i] == '*' || factorData[i] == '/') {
            StringPiece factorElement = stringFactor.substr(start, i - start);
            addFactorElement(result, factorElement, signum, status);

            start = i + 1; // Set `start` to point to the start of the new element.
        } else if (i == n - 1) {
            // Last element
            addFactorElement(result, stringFactor.substr(start, i + 1), signum, status);
        }

        if (factorData[i] == '/') {
            signum = Signum::NEGATIVE; // Change the signum because we reached the Denominator.
        }
    }

    return result;
}

// Load factor for a single source
Factor loadSingleFactor(StringPiece source, const ConversionRates &ratesInfo, UErrorCode &status) {
    const auto conversionUnit = ratesInfo.extractConversionInfo(source, status);
    if (U_FAILURE(status)) return Factor();
    if (conversionUnit == nullptr) {
        status = U_INTERNAL_PROGRAM_ERROR;
        return Factor();
    }

    Factor result = extractFactorConversions(conversionUnit->factor.toStringPiece(), status);
    result.offset = strHasDivideSignToDouble(conversionUnit->offset.toStringPiece(), status);

    return result;
}

// Load Factor of a compound source unit.
Factor loadCompoundFactor(const MeasureUnit &source, const ConversionRates &ratesInfo,
                          UErrorCode &status) {

    Factor result;
    MeasureUnitImpl memory;
    const auto &compoundSourceUnit = MeasureUnitImpl::forMeasureUnit(source, memory, status);
    if (U_FAILURE(status)) return result;

    for (int32_t i = 0, n = compoundSourceUnit.units.length(); i < n; i++) {
        auto singleUnit = *compoundSourceUnit.units[i]; // a SingleUnitImpl

        Factor singleFactor = loadSingleFactor(singleUnit.getSimpleUnitID(), ratesInfo, status);
        if (U_FAILURE(status)) return result;

        // Apply SiPrefix before the power, because the power may be will flip the factor.
        singleFactor.applySiPrefix(singleUnit.siPrefix);

        // Apply the power of the `dimensionality`
        singleFactor.power(singleUnit.dimensionality);

        result.multiplyBy(singleFactor);
    }

    return result;
}

/**
 * Checks if the source unit and the target unit are simple. For example celsius or fahrenheit. But not
 * square-celsius or square-fahrenheit.
 */
UBool checkSimpleUnit(const MeasureUnit &unit, UErrorCode &status) {
    MeasureUnitImpl memory;
    const auto &compoundSourceUnit = MeasureUnitImpl::forMeasureUnit(unit, memory, status);
    if (U_FAILURE(status)) return false;

    if (compoundSourceUnit.complexity != UMEASURE_UNIT_SINGLE) {
        return false;
    }

    U_ASSERT(compoundSourceUnit.units.length() == 1);
    auto singleUnit = *(compoundSourceUnit.units[0]);

    if (singleUnit.dimensionality != 1 || singleUnit.siPrefix != UMEASURE_SI_PREFIX_ONE) {
        return false;
    }
    return true;
}

/**
 *  Extract conversion rate from `source` to `target`
 */
void loadConversionRate(ConversionRate &conversionRate, const MeasureUnit &source,
                        const MeasureUnit &target, UnitsConvertibilityState unitsState,
                        const ConversionRates &ratesInfo, UErrorCode &status) {
    // Represents the conversion factor from the source to the target.
    Factor finalFactor;

    // Represents the conversion factor from the source to the base unit that specified in the conversion
    // data which is considered as the root of the source and the target.
    Factor sourceToBase = loadCompoundFactor(source, ratesInfo, status);
    Factor targetToBase = loadCompoundFactor(target, ratesInfo, status);

    // Merger Factors
    finalFactor.multiplyBy(sourceToBase);
    if (unitsState == UnitsConvertibilityState::CONVERTIBLE) {
        finalFactor.divideBy(targetToBase);
    } else if (unitsState == UnitsConvertibilityState::RECIPROCAL) {
        finalFactor.multiplyBy(targetToBase);
    } else {
        status = UErrorCode::U_ARGUMENT_TYPE_MISMATCH;
        return;
    }

    finalFactor.substituteConstants();

    conversionRate.factorNum = finalFactor.factorNum;
    conversionRate.factorDen = finalFactor.factorDen;

    // In case of simple units (such as: celsius or fahrenheit), offsets are considered.
    if (checkSimpleUnit(source, status) && checkSimpleUnit(target, status)) {
        conversionRate.sourceOffset =
            sourceToBase.offset * sourceToBase.factorDen / sourceToBase.factorNum;
        conversionRate.targetOffset =
            targetToBase.offset * targetToBase.factorDen / targetToBase.factorNum;
    }

    conversionRate.reciprocal = unitsState == UnitsConvertibilityState::RECIPROCAL;
}

} // namespace

// Conceptually, this modifies factor: factor *= baseStr^(signum*power).
//
// baseStr must be a known constant or a value that strToDouble() is able to
// parse.
void U_I18N_API addSingleFactorConstant(StringPiece baseStr, int32_t power, Signum signum,
                                        Factor &factor, UErrorCode &status) {
    if (baseStr == "ft_to_m") {
        factor.constants[CONSTANT_FT2M] += power * signum;
    } else if (baseStr == "ft2_to_m2") {
        factor.constants[CONSTANT_FT2M] += 2 * power * signum;
    } else if (baseStr == "ft3_to_m3") {
        factor.constants[CONSTANT_FT2M] += 3 * power * signum;
    } else if (baseStr == "in3_to_m3") {
        factor.constants[CONSTANT_FT2M] += 3 * power * signum;
        factor.factorDen *= 12 * 12 * 12;
    } else if (baseStr == "gal_to_m3") {
        factor.factorNum *= 231;
        factor.constants[CONSTANT_FT2M] += 3 * power * signum;
        factor.factorDen *= 12 * 12 * 12;
    } else if (baseStr == "gal_imp_to_m3") {
        factor.constants[CONSTANT_GAL_IMP2M3] += power * signum;
    } else if (baseStr == "G") {
        factor.constants[CONSTANT_G] += power * signum;
    } else if (baseStr == "gravity") {
        factor.constants[CONSTANT_GRAVITY] += power * signum;
    } else if (baseStr == "lb_to_kg") {
        factor.constants[CONSTANT_LB2KG] += power * signum;
    } else if (baseStr == "PI") {
        factor.constants[CONSTANT_PI] += power * signum;
    } else {
        if (signum == Signum::NEGATIVE) {
            factor.factorDen *= std::pow(strToDouble(baseStr, status), power);
        } else {
            factor.factorNum *= std::pow(strToDouble(baseStr, status), power);
        }
    }
}

/**
 * Extracts the compound base unit of a compound unit (`source`). For example, if the source unit is
 * `square-mile-per-hour`, the compound base unit will be `square-meter-per-second`
 */
MeasureUnit U_I18N_API extractCompoundBaseUnit(const MeasureUnit &source,
                                               const ConversionRates &conversionRates,
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
        const auto rateInfo =
            conversionRates.extractConversionInfo(singleUnitImpl.getSimpleUnitID(), status);
        if (U_FAILURE(status)) {
            return result;
        }
        if (rateInfo == nullptr) {
            status = U_INTERNAL_PROGRAM_ERROR;
            return result;
        }

        // Multiply the power of the singleUnit by the power of the baseUnit. For example, square-hectare
        // must be pow4-meter. (NOTE: hectare --> square-meter)
        auto compoundBaseUnit = MeasureUnit::forIdentifier(rateInfo->baseUnit.toStringPiece(), status);

        int32_t baseUnitsCount;
        auto baseUnits = compoundBaseUnit.splitToSingleUnits(baseUnitsCount, status);
        for (int j = 0; j < baseUnitsCount; j++) {
            int8_t newDimensionality =
                baseUnits[j].getDimensionality(status) * singleUnit.getDimensionality(status);
            result = result.product(baseUnits[j].withDimensionality(newDimensionality, status), status);

            if (U_FAILURE(status)) {
                return result;
            }
        }
    }

    return result;
}

UnitsConvertibilityState U_I18N_API checkConvertibility(const MeasureUnit &source,
                                                        const MeasureUnit &target,
                                                        const ConversionRates &conversionRates,
                                                        UErrorCode &status) {
    if (source.getComplexity(status) == UMeasureUnitComplexity::UMEASURE_UNIT_MIXED ||
        target.getComplexity(status) == UMeasureUnitComplexity::UMEASURE_UNIT_MIXED) {
        status = U_INTERNAL_PROGRAM_ERROR;
        return UNCONVERTIBLE;
    }

    auto sourceBaseUnit = extractCompoundBaseUnit(source, conversionRates, status);
    auto targetBaseUnit = extractCompoundBaseUnit(target, conversionRates, status);

    if (U_FAILURE(status)) return UNCONVERTIBLE;

    if (sourceBaseUnit == targetBaseUnit) return CONVERTIBLE;
    if (sourceBaseUnit == targetBaseUnit.reciprocal(status)) return RECIPROCAL;

    auto sourceSimplified = sourceBaseUnit.simplify(status);
    auto targetSimplified = targetBaseUnit.simplify(status);

    if (sourceSimplified == targetSimplified) return CONVERTIBLE;
    if (sourceSimplified == targetSimplified.reciprocal(status)) return RECIPROCAL;

    return UNCONVERTIBLE;
}

UnitConverter::UnitConverter(MeasureUnit source, MeasureUnit target, const ConversionRates &ratesInfo,
                             UErrorCode &status) {

    if (source.getComplexity(status) == UMeasureUnitComplexity::UMEASURE_UNIT_MIXED ||
        target.getComplexity(status) == UMeasureUnitComplexity::UMEASURE_UNIT_MIXED) {
        status = U_INTERNAL_PROGRAM_ERROR;
        return;
    }

    UnitsConvertibilityState unitsState = checkConvertibility(source, target, ratesInfo, status);
    if (U_FAILURE(status)) return;
    if (unitsState == UnitsConvertibilityState::UNCONVERTIBLE) {
        status = U_INTERNAL_PROGRAM_ERROR;
        return;
    }

    conversionRate_.source = source;
    conversionRate_.target = target;

    loadConversionRate(conversionRate_, source, target, unitsState, ratesInfo, status);
}

double UnitConverter::convert(double inputValue) const {
    double result =
        inputValue + conversionRate_.sourceOffset; // Reset the input to the target zero index.
    // Convert the quantity to from the source scale to the target scale.
    result *= conversionRate_.factorNum / conversionRate_.factorDen;

    result -= conversionRate_.targetOffset; // Set the result to its index.

    if (result == 0)
        return 0.0; // If the result is zero, it does not matter if the conversion are reciprocal or not.
    if (conversionRate_.reciprocal) {
        result = 1.0 / result;
    }

    // TODO: remove the multiplication by 1.000,000,000,001 after using `decNumber`

    // Multiply the result by 1.000,000,000,001 to fix the deterioration from using `double` (the
    // deterioration is around 15 to 17 decimal digit).
    return result * 1.000000000001;
}

} // namespace units
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
