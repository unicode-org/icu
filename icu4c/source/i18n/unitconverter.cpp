// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <cmath>

#include "charstr.h"
#include "double-conversion.h"
#include "measunit_impl.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/stringpiece.h"
#include "unitconverter.h"

U_NAMESPACE_BEGIN

namespace {

/* Internal Structure */

enum Constants {
    CONSTANT_FT2M,    // ft2m stands for foot to meter.
    CONSTANT_PI,      // PI
    CONSTANT_GRAVITY, // Gravity
    CONSTANT_G,
    CONSTANT_GAL_IMP2M3, // Gallon imp to m3
    CONSTANT_LB2KG,      // Pound to Kilogram

    // Must be the last element.
    CONSTANTS_COUNT
};

typedef enum SigNum {
    NEGATIVE = -1,
    POSITIVE = 1,
} SigNum;

/* Represents a conversion factor */
struct Factor {
    double factorNum = 1;
    double factorDen = 1;
    double offset = 0;
    bool reciprocal = false;
    int32_t constants[CONSTANTS_COUNT] = {};

    void multiplyBy(const Factor &rhs) {
        factorNum *= rhs.factorNum;
        factorDen *= rhs.factorDen;
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] += rhs.constants[i];

        offset += rhs.offset;
    }

    void divideBy(const Factor &rhs) {
        factorNum *= rhs.factorDen;
        factorDen *= rhs.factorNum;
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] -= rhs.constants[i];

        offset += rhs.offset;
    }

    // Apply the power to the factor.
    void power(int32_t power) {
        // multiply all the constant by the power.
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] *= power;

        bool shouldFlip = power < 0; // This means that after applying the absolute power, we should flip
                                     // the Numerator and Denomerator.

        factorNum = std::pow(factorNum, std::abs(power));
        factorDen = std::pow(factorDen, std::abs(power));

        if (shouldFlip) {
            // Flip Numerator and Denomirator.
            std::swap(factorNum, factorDen);
        }
    }

    // Flip the `Factor`, for example, factor= 2/3, flippedFactor = 3/2
    void flip() {
        std::swap(factorNum, factorDen);

        for (int i = 0; i < CONSTANTS_COUNT; i++) {
            constants[i] *= -1;
        }
    }

    // Apply SI prefix to the `Factor`
    void applySiPrefix(UMeasureSIPrefix siPrefix) {
        if (siPrefix == UMeasureSIPrefix::UMEASURE_SI_PREFIX_ONE) return; // No need to do anything

        double siApplied = std::pow(10.0, std::abs(siPrefix));

        if (siPrefix < 0) {
            factorDen *= siApplied;
            return;
        }

        factorNum *= siApplied;
    }
};

/* Helpers */

using icu::double_conversion::StringToDoubleConverter;

// Returns `double` from a scientific number(i.e. "1", "2.01" or "3.09E+4")
double strToDouble(StringPiece strNum) {
    // We are processing well-formed input, so we don't need any special options to
    // StringToDoubleConverter.
    StringToDoubleConverter converter(0, 0, 0, "", "");
    int32_t count;
    return converter.StringToDouble(strNum.data(), strNum.length(), &count);
}

// Returns `double` from a scientific number that could has a division sign (i.e. "1", "2.01", "3.09E+4"
// or "2E+2/3")
double strHasDivideSignToDouble(StringPiece strWithDivide) {
    int divisionSignInd = -1;
    for (int i = 0, n = strWithDivide.length(); i < n; ++i) {
        if (strWithDivide.data()[i] == '/') {
            divisionSignInd = i;
            break;
        }
    }

    if (divisionSignInd >= 0) {
        return strToDouble(strWithDivide.substr(0, divisionSignInd)) /
               strToDouble(strWithDivide.substr(divisionSignInd + 1));
    }

    return strToDouble(strWithDivide);
}

const ConversionRateInfo &
extractConversionInfo(StringPiece source,
                      const MaybeStackVector<ConversionRateInfo> &conversionRateInfoList,
                      UErrorCode &status) {
    for (size_t i = 0, n = conversionRateInfoList.length(); i < n; ++i) {
        if (conversionRateInfoList[i]->sourceUnit.toStringPiece() == source)
            return *(conversionRateInfoList[i]);
    }

    status = U_INTERNAL_PROGRAM_ERROR;
    return ConversionRateInfo();
}

MeasureUnit extractBaseUnit(const MeasureUnit &source,
                            const MaybeStackVector<ConversionRateInfo> &conversionRateInfoList,
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
        const auto &rateInfo =
            extractConversionInfo(singleUnitImpl.identifier, conversionRateInfoList, status);
        if (U_FAILURE(status)) return result;

        // Multiply the power of the singleUnit by the power of the baseUnit. For example, square-hectare
        // must be p4-meter. (NOTE: hectare --> square-meter)
        auto baseUnit = MeasureUnit::forIdentifier(rateInfo.baseUnit.toStringPiece(), status);
        auto singleBaseUnit = SingleUnitImpl::forMeasureUnit(baseUnit, status);
        singleBaseUnit.dimensionality *= singleUnit.getDimensionality(status);

        result = result.product(singleBaseUnit.build(status), status);
    }

    return result;
}

/*
 * Adds a single factor element to the `Factor`. e.g "ft3m", "2.333" or "cup2m3". But not "cup2m3^3".
 */
void addSingleFactorConstant(StringPiece baseStr, int32_t power, SigNum sigNum, Factor &factor) {

    if (baseStr == "ft_to_m") {
        factor.constants[CONSTANT_FT2M] += power * sigNum;
    } else if (baseStr == "ft2_to_m2") {
        factor.constants[CONSTANT_FT2M] += 2 * power * sigNum;
    } else if (baseStr == "ft3_to_m3") {
        factor.constants[CONSTANT_FT2M] += 3 * power * sigNum;
    } else if (baseStr == "in3_to_m3") {
        factor.constants[CONSTANT_FT2M] += 3 * power * sigNum;
        factor.factorDen *= 12 * 12 * 12;
    } else if (baseStr == "gal_to_m3") {
        factor.factorNum *= 231;
        factor.constants[CONSTANT_FT2M] += 3 * power * sigNum;
        factor.factorDen *= 12 * 12 * 12;
    } else if (baseStr == "gal_imp_to_m3") {
        factor.constants[CONSTANT_GAL_IMP2M3] += power * sigNum;
    } else if (baseStr == "G") {
        factor.constants[CONSTANT_G] += power * sigNum;
    } else if (baseStr == "gravity") {
        factor.constants[CONSTANT_GRAVITY] += power * sigNum;
    } else if (baseStr == "lb_to_kg") {
        factor.constants[CONSTANT_LB2KG] += power * sigNum;
    } else if (baseStr == "PI") {
        factor.constants[CONSTANT_PI] += power * sigNum;
    } else {
        if (sigNum == SigNum::NEGATIVE) {
            factor.factorDen *= std::pow(strToDouble(baseStr), power);
        } else {
            factor.factorNum *= std::pow(strToDouble(baseStr), power);
        }
    }
}

/*
  Adds single factor to a `Factor` object. Single factor means "23^2", "23.3333", "ft2m^3" ...etc.
  However, complex factor are not included, such as "ft2m^3*200/3"
*/
void addFactorElement(Factor &factor, StringPiece elementStr, SigNum sigNum) {
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

        power = static_cast<int32_t>(strToDouble(powerStr));
    } else {
        baseStr = elementStr;
    }

    addSingleFactorConstant(baseStr, power, sigNum, factor);
}

/*
 * Extracts `Factor` from a complete string factor. e.g. "ft2m^3*1007/cup2m3*3"
 */
Factor extractFactorConversions(StringPiece stringFactor, UErrorCode &status) {
    Factor result;
    SigNum sigNum = SigNum::POSITIVE;
    auto factorData = stringFactor.data();
    for (int32_t i = 0, start = 0, n = stringFactor.length(); i < n; i++) {
        if (factorData[i] == '*' || factorData[i] == '/') {
            StringPiece factorElement = stringFactor.substr(start, i - start);
            addFactorElement(result, factorElement, sigNum);

            start = i + 1; // Set `start` to point to the start of the new element.
        } else if (i == n - 1) {
            // Last element
            addFactorElement(result, stringFactor.substr(start, i + 1), sigNum);
        }

        if (factorData[i] == '/')
            sigNum = SigNum::NEGATIVE; // Change the sigNum because we reached the Denominator.
    }

    return result;
}

// Load factor for a single source
Factor loadSingleFactor(StringPiece source, const MaybeStackVector<ConversionRateInfo> &ratesInfo,
                        UErrorCode &status) {
    const auto &conversionUnit = extractConversionInfo(source, ratesInfo, status);
    if (U_FAILURE(status)) return Factor();

    auto result = extractFactorConversions(conversionUnit.factor.toStringPiece(), status);
    result.offset = strHasDivideSignToDouble(conversionUnit.offset.toStringPiece());

    // TODO: `reciprocal` should be added to the `ConversionRateInfo`.
    // result.reciprocal = conversionUnit.reciprocal

    return result;
}

// Load Factor for compound source
Factor loadCompoundFactor(const MeasureUnit &source,
                          const MaybeStackVector<ConversionRateInfo> &ratesInfo, UErrorCode &status) {

    Factor result;
    auto compoundSourceUnit = MeasureUnitImpl::forMeasureUnitMaybeCopy(source, status);
    if (U_FAILURE(status)) return result;

    for (int32_t i = 0, n = compoundSourceUnit.units.length(); i < n; i++) {
        auto singleUnit = *compoundSourceUnit.units[i]; // a TempSingleUnit

        Factor singleFactor = loadSingleFactor(singleUnit.identifier, ratesInfo, status);

        // Apply SiPrefix before the power, because the power may be will flip the factor.
        singleFactor.applySiPrefix(singleUnit.siPrefix);

        // Apply the power of the `dimensionality`
        singleFactor.power(singleUnit.dimensionality);

        result.multiplyBy(singleFactor);
    }

    return result;
}

void substituteSingleConstant(int32_t constantPower,
                              double constantValue /* constant actual value, e.g. G= 9.88888 */,
                              Factor &factor) {
    constantValue = std::pow(constantValue, std::abs(constantPower));

    if (constantPower < 0) {
        factor.factorDen *= constantValue;
    } else {
        factor.factorNum *= constantValue;
    }
}

void substituteConstants(Factor &factor, UErrorCode &status) {
    double constantsValues[CONSTANTS_COUNT];

    constantsValues[CONSTANT_FT2M] = 0.3048;
    constantsValues[CONSTANT_PI] = 411557987.0 / 131002976.0;
    constantsValues[CONSTANT_GRAVITY] = 9.80665;
    constantsValues[CONSTANT_G] = 6.67408E-11;
    constantsValues[CONSTANT_LB2KG] = 0.45359237;
    constantsValues[CONSTANT_GAL_IMP2M3] = 0.00454609;

    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        if (factor.constants[i] == 0) continue;

        substituteSingleConstant(factor.constants[i], constantsValues[i], factor);
        factor.constants[i] = 0;
    }
}

/**
 * Checks if the source unit and the target unit are simple. For example celsius or fahrenheit. But not
 * square-celsius or square-fahrenheit.
 */
UBool checkSimpleUnit(const MeasureUnit &unit, UErrorCode &status) {
    auto compoundSourceUnit = MeasureUnitImpl::forMeasureUnitMaybeCopy(unit, status);

    if (U_FAILURE(status)) return false;

    if (compoundSourceUnit.complexity != UMEASURE_UNIT_SINGLE) { return false; }

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
                        const MeasureUnit &target, UnitsMatchingState unitsState,
                        const MaybeStackVector<ConversionRateInfo> &ratesInfo, UErrorCode &status) {
    // Represents the conversion factor from the source to the target.
    Factor finalFactor;

    // Represents the conversion factor from the source to the base unit that specified in the conversion
    // data which is considered as the root of the source and the target.
    Factor sourceToBase = loadCompoundFactor(source, ratesInfo, status);
    Factor targetToBase = loadCompoundFactor(target, ratesInfo, status);

    // Merger Factors
    finalFactor.multiplyBy(sourceToBase);
    if (unitsState == UnitsMatchingState::CONVERTIBLE) {
        finalFactor.divideBy(targetToBase);
    } else if (unitsState == UnitsMatchingState::RECIPROCAL) {
        finalFactor.multiplyBy(targetToBase);
    } else {
        status = UErrorCode::U_ARGUMENT_TYPE_MISMATCH;
        return;
    }

    // Substitute constants
    substituteConstants(finalFactor, status);

    conversionRate.factorNum = finalFactor.factorNum;
    conversionRate.factorDen = finalFactor.factorDen;

    // In case of simple units (such as: celsius or fahrenheit), offsets are considered.
    if (checkSimpleUnit(source, status) && checkSimpleUnit(target, status)) {
        conversionRate.sourceOffset =
            sourceToBase.offset * sourceToBase.factorDen / sourceToBase.factorNum;
        conversionRate.targetOffset =
            targetToBase.offset * targetToBase.factorDen / targetToBase.factorNum;
    }

    conversionRate.reciprocal = unitsState == UnitsMatchingState::RECIPROCAL;
}

} // namespace

UnitsMatchingState U_I18N_API
checkUnitsState(const MeasureUnit &source, const MeasureUnit &target,
                const MaybeStackVector<ConversionRateInfo> &conversionRateInfoList, UErrorCode &status) {
    auto sourceBaseUnit = extractBaseUnit(source, conversionRateInfoList, status);
    auto targetBaseUnit = extractBaseUnit(target, conversionRateInfoList, status);

    if (U_FAILURE(status)) return UNCONVERTIBLE;

    if (sourceBaseUnit == targetBaseUnit) return CONVERTIBLE;
    if (sourceBaseUnit == targetBaseUnit.reciprocal(status)) return RECIPROCAL;

    return UNCONVERTIBLE;
}

UnitConverter::UnitConverter(MeasureUnit source, MeasureUnit target,
                             const MaybeStackVector<ConversionRateInfo> &ratesInfo, UErrorCode &status) {
    UnitsMatchingState unitsState = checkUnitsState(source, target, ratesInfo, status);
    if (U_FAILURE(status)) return;
    if (unitsState == UnitsMatchingState::UNCONVERTIBLE) {
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
    if (conversionRate_.reciprocal) { result = 1.0 / result; }
    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */