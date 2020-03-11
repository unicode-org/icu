// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <cmath>
#include <memory>
#include <vector>

#include "charstr.h"
#include "measunit_impl.h"
#include "resource.h"
#include "unicode/stringpiece.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "unitconverter.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

namespace {
/* Internal Structure */

// Represents a raw convert unit.
struct ConvertUnit {
    StringPiece source;
    StringPiece target;

    // NOTE
    //  If the source and target are equal, the factor will be "1"
    StringPiece factor = "1";

    // NOTE
    //  If the offset is not exist, its value will be "0"
    StringPiece offset = "0";

    // NOTE
    //  If reciprocal is not exits, its value will be `false`
    bool reciprocal = false;
};

typedef enum Signal {
    NEGATIVE = -1,
    POSITIVE = 1,
} Signal;

enum UnitsCase {
    RECIPROCAL,
    CONVERTIBLE,
    UNCONVERTIBLE,
};

/* Helpers */

// Returns `double` from a scientific number(i.e. "1", "2.01" or "3.09E+4")
double strToDouble(StringPiece strNum) {
    std::string charNum;
    for (int i = 0; i < strNum.length(); i++) {
        charNum += strNum.data()[i];
    }

    char *end;
    return std::strtod(charNum.c_str(), &end);
}

// Returns `double` from a scientific number that could has a division sign (i.e. "1", "2.01", "3.09E+4"
// or "2E+2/3")
double strHasDivideSignToDouble(StringPiece strWithDivide, UErrorCode &status) {
    CharString charNum(strWithDivide, status);
    if (U_FAILURE(status)) return 0.0;

    int divisionSignInd = -1;
    for (int i = 0, n = charNum[i]; i < n; ++i) {
        if (charNum[i] == '/') {
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

//////////////////////////
/// BEGIN DATA LOADING ///
//////////////////////////

class UnitConversionRatesSink : public ResourceSink {
  public:
    explicit UnitConversionRatesSink(Factor *conversionFactor) : conversionFactor(conversionFactor) {}

    void put(const char *key, ResourceValue &value, UBool /*noFallback*/,
             UErrorCode &status) U_OVERRIDE {
        ResourceTable conversionRateTable = value.getTable(status);
        if (U_FAILURE(status)) { return; }

        for (int32_t i = 0; conversionRateTable.getKeyAndValue(i, key, value); ++i) {
            StringPiece keySP(key);

            if (keySP == "factor") {
                value.getUnicodeString(status);

            }

            else if (keySP == "offset") {
                value.getUnicodeString(status);
            }

            else if (keySP == "target") {
                // TODO(younies): find a way to convert UnicodeStirng to StringPiece
                // conversionRate->target.set(value.getUnicodeString(status));
            }

            if (U_FAILURE(status)) { return; }
        }
    }

  private:
    Factor *conversionFactor;
};

ConvertUnit extractConvertUnit(StringPiece source, UErrorCode &status) {
    ConvertUnit result;
    // TODO(hugovdm): implement.
    //   NOTE: use the database.
    //         UnitConversionRatesSink sink(&conversionFactor);
    //         ures_getAllItemsWithFallback(rb.getAlias(), key.data(), sink, status);
    //
    //   NOTE:
    //     LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    //     CharString key;
    //     key.append("convertUnit/", status);
    //     key.append(source, status);
    return result;
}

/*/
 * Add single factor element to the `Factor`. e.g "ft3m", "2.333" or "cup2m3". But not "cup2m3^3".
 */
void addSingleFactorConstant(Factor &factor, StringPiece baseStr, int32_t power, Signal signal) {
    if (baseStr == "ft_to_m") {
        factor.constants[CONSTANT_FT2M] += power * signal;
    } else if (baseStr == "ft2_to_m2") {
        factor.constants[CONSTANT_FT2M] += 2 * power * signal;
    } else if (baseStr == "ft3_to_m3") {
        factor.constants[CONSTANT_FT2M] += 3 * power * signal;
    } else if (baseStr == "in3_to_m3") {
        factor.constants[CONSTANT_FT2M] += 3 * power * signal;
        factor.factorDen *= 12 * 12 * 12;
    } else if (baseStr == "gal_to_m3") {
        factor.factorNum *= 231;
        factor.constants[CONSTANT_FT2M] += 3 * power * signal;
        factor.factorDen *= 12 * 12 * 12;
    } else if (baseStr == "G") {
        factor.constants[CONSTANT_G] += power * signal;
    } else if (baseStr == "gravity") {
        factor.constants[CONSTANT_GRAVITY] += power * signal;
    } else if (baseStr == "lb_to_kg") {
        factor.constants[CONSTANT_LB2KG] += power * signal;
    } else if (baseStr == "PI") {
        factor.constants[CONSTANT_PI] += power * signal;
    } else {
        if (signal == Signal::NEGATIVE) {
            factor.factorDen *= std::pow(strToDouble(baseStr), power);
        } else {
            factor.factorNum *= std::pow(strToDouble(baseStr), power);
        }
    }
}

/*
  Adds single factor for a `Factor` object. Single factor means "23^2", "23.3333", "ft2m^3" ...etc.
  However, complext factor are not included, such as "ft2m^3*200/3"
*/
void addFactorElement(Factor &factor, StringPiece elementStr, Signal signal, UErrorCode &status) {
    StringPiece baseStr;
    StringPiece powerStr;
    int32_t power =
        1; // In case the power is not written, then, the power is equal 1 ==> `ft2m^1` == `ft2m`

    // Search for the power part
    int32_t powerInd = -1;
    CharString charStr(elementStr, status);
    for (int32_t i = 0, n = charStr.length(); i < n; ++i) {
        if (charStr[i] == '^') {
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

    addSingleFactorConstant(factor, baseStr, power, signal);
}

/*
 * Extracts `Factor` from a complete string factor. e.g. "ft2m^3*1007/cup2m3*3"
 */
void extractFactor(Factor &factor, StringPiece stringFactor, UErrorCode &status) {
    // Set factor to `1`
    factor.factorNum = 1;
    factor.factorDen = 1;

    Signal signal = Signal::POSITIVE;
    auto factorData = stringFactor.data();
    for (int32_t i = 0, start = 0, n = stringFactor.length(); i < n; i++) {
        if (factorData[i] == '*' || factorData[i] == '/') {
            StringPiece factorElement = stringFactor.substr(start, i - start);
            addFactorElement(factor, factorElement, signal, status);

            start = i + 1; // Set `start` to point to the start of the new element.
        } else if (i == n - 1) {
            // Last element
            addFactorElement(factor, stringFactor.substr(start, i + 1), signal, status);
        }

        if (factorData[i] == '/')
            signal = Signal::NEGATIVE; // Change the signal because we reached the Denominator.
    }
}

// Load factor for a single source
void loadSingleFactor(Factor &factor, StringPiece source, UErrorCode &status) {
    auto conversionUnit = extractConvertUnit(source, status);
    if (U_FAILURE(status)) return;

    extractFactor(factor, conversionUnit.factor, status);
    factor.offset = strHasDivideSignToDouble(conversionUnit.offset, status);
    factor.reciprocal = factor.reciprocal;
}

// Load Factor for compound source
void loadCompoundFactor(Factor &factor, StringPiece source, UErrorCode &status) {
    icu::MeasureUnit compoundSourceUnit = icu::MeasureUnit::forIdentifier(source, status);
    auto singleUnits = compoundSourceUnit.splitToSingleUnits(status);
    for (int i = 0; i < singleUnits.length(); i++) {
        Factor singleFactor;
        auto singleUnit = TempSingleUnit::forMeasureUnit(singleUnits[i], status);

        loadSingleFactor(singleFactor, singleUnit.identifier, status);

        // You must apply SiPrefix before the power, because the power may be will flip the factor.
        singleFactor.applySiPrefix(singleUnit.siPrefix);

        singleFactor.power(singleUnit.dimensionality);

        factor.multiplyBy(singleFactor);
    }
}

void substituteSingleConstant(Factor &factor, int32_t constantPower,
                              double constantValue /* constant actual value, e.g. G= 9.88888 */) {
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
    constantsValues[CONSTANT_PI] = 3.14159265359;
    constantsValues[CONSTANT_GRAVITY] = 9.80665;
    constantsValues[CONSTANT_G] = 6.67408E-11;
    constantsValues[CONSTANT_LB2KG] = 0.45359237;

    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        if (factor.constants[i] == 0) continue;

        substituteSingleConstant(factor, factor.constants[i], constantsValues[i]);
        factor.constants[i] = 0;
    }
}

/**
 * Checks if the source unit and the target unit are singular. For example celsius or fahrenheit. But not
 * square-celsius or square-fahrenheit.
 */
UBool checkSingularUnits(StringPiece source, UErrorCode &status) {
    icu::MeasureUnit compoundSourceUnit = icu::MeasureUnit::forIdentifier(source, status);

    auto singleUnits = compoundSourceUnit.splitToSingleUnits(status);
    if (singleUnits.length() > 1) return false; // Singular unit contains only a single unit.

    auto singleUnit = TempSingleUnit::forMeasureUnit(singleUnits[0], status);

    if (singleUnit.dimensionality != 1) return false;
    if (singleUnit.siPrefix == UMeasureSIPrefix::UMEASURE_SI_PREFIX_ONE) return true;
    return false;
}

/**
 *  Extract conversion rate from `source` to `target`
 */
void loadConversionRate(ConversionRate &conversionRate, StringPiece source, StringPiece target,
                        UnitsCase unitsCase, UErrorCode &status) {
    // Represents the conversion factor from the source to the target.
    Factor finalFactor;

    // Represents the conversion factor from the source to the target that specified in the conversion
    // data which is considered as the root of the source and the target.
    Factor SourceToRoot;
    Factor TargetToRoot;

    /* Load needed factors. */
    // Load the conversion factor from the source to the target in the  which is considered as the Root
    // between
    loadCompoundFactor(SourceToRoot, source, status);
    loadCompoundFactor(TargetToRoot, target, status);

    // Merger Factors
    finalFactor.multiplyBy(SourceToRoot);
    if (unitsCase == UnitsCase::CONVERTIBLE) {
        finalFactor.divideBy(TargetToRoot);
    } else if (unitsCase == UnitsCase::RECIPROCAL) {
        finalFactor.multiplyBy(TargetToRoot);
    } else {
        status = UErrorCode::U_ARGUMENT_TYPE_MISMATCH;
        return;
    }

    // Substitute constants
    substituteConstants(finalFactor, status);

    conversionRate.source = source;
    conversionRate.target = target;

    conversionRate.factorNum = finalFactor.factorNum;
    conversionRate.factorDen = finalFactor.factorDen;

    if (checkSingularUnits(source, status) && checkSingularUnits(target, status)) {
        conversionRate.sourceOffset =
            SourceToRoot.offset * SourceToRoot.factorDen / SourceToRoot.factorNum;
        conversionRate.targetOffset =
            TargetToRoot.offset * TargetToRoot.factorDen / TargetToRoot.factorNum;
    }

    conversionRate.reciprocal = unitsCase == UnitsCase::RECIPROCAL;
}

StringPiece getTarget(StringPiece source, UErrorCode &status) {
    auto convertUnit = extractConvertUnit(source, status);
    if (U_FAILURE(status)) return StringPiece("");
    return convertUnit.target;
}

// Remove this once we moved to c++14
template <typename T, typename... Args> std::unique_ptr<T> make_unique(Args &&... args) {
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}

std::vector<std::unique_ptr<MeasureUnit>> extractTargets(MeasureUnit source, UErrorCode &status) {
    std::vector<std::unique_ptr<MeasureUnit>> result;
    auto singleUnits = source.splitToSingleUnits(status);
    for (int i = 0; i < singleUnits.length(); i++) {
        const auto &singleUnit = singleUnits[i];
        StringPiece target = getTarget(singleUnit.getIdentifier(), status);

        if (U_FAILURE(status)) return result;

        MeasureUnit targetUnit = MeasureUnit::forIdentifier(target, status);
        auto tempTargetUnit = TempSingleUnit::forMeasureUnit(targetUnit, status);
        tempTargetUnit.siPrefix = singleUnit.getSIPrefix(status);
        tempTargetUnit.dimensionality = singleUnit.getDimensionality(status);
        if (U_FAILURE(status)) return result;

        auto targetUnits = tempTargetUnit.build(status).splitToSingleUnits(status);
        if (U_FAILURE(status)) return result;

        for (int j = 0; j < targetUnits.length(); j++) {
            result.push_back(make_unique<MeasureUnit>(targetUnits[j]));
            if (U_FAILURE(status)) return result;
        }
    }

    return result;
}

UnitsCase checkUnitsCase(MeasureUnit source, MeasureUnit target, UErrorCode &status) {
    std::vector<std::unique_ptr<MeasureUnit>> sourceTargetUnits = extractTargets(source, status);
    std::vector<std::unique_ptr<MeasureUnit>> targetTargetUnits = extractTargets(target, status);

    const int32_t sourceUnitsSize = sourceTargetUnits.size();
    const int32_t targetUnitsSize = targetTargetUnits.size();

    if (sourceUnitsSize != targetUnitsSize || sourceUnitsSize == 0) return UnitsCase::UNCONVERTIBLE;
    std::vector<bool> targetCheckersMatch(targetUnitsSize, false);
    std::vector<bool> targetCheckersReciprocal(targetUnitsSize, false);

    for (const auto &singleSourceTarget : sourceTargetUnits) {
        // Check for Match
        for (int i = 0; i < targetUnitsSize; i++) {
            if (!targetCheckersMatch[i] && *singleSourceTarget == *targetTargetUnits[i]) {
                targetCheckersMatch[i] = true;
                break;
            }
        }

        // Check for Reciprocal
        for (int i = 0; i < targetUnitsSize; i++) {
            if (!targetCheckersReciprocal[i] &&
                singleSourceTarget->reciprocal(status) == *targetTargetUnits[i]) {
                targetCheckersReciprocal[i] = true;
                break;
            }
        }
    }

    if (targetCheckersMatch == std::vector<bool>(targetUnitsSize, true)) return UnitsCase::CONVERTIBLE;
    if (targetCheckersReciprocal == std::vector<bool>(targetUnitsSize, true))
        return UnitsCase::RECIPROCAL;

    return UnitsCase::UNCONVERTIBLE;
}

} // namespace

UnitConverter::UnitConverter(MeasureUnit source, MeasureUnit target, UErrorCode &status) {
    UnitsCase unitsCase = checkUnitsCase(source, target, status);
    if (U_FAILURE(status)) return;
    loadConversionRate(conversionRate_, source.getIdentifier(), target.getIdentifier(), unitsCase,
                       status);
}

double UnitConverter::convert(double inputValue) {
    double result =
        inputValue + conversionRate_.sourceOffset; // Reset the input to the target zero index.
    // Convert the quantity to from the source scale to the target scale.
    result *= conversionRate_.factorNum / conversionRate_.factorDen;

    result -= conversionRate_.targetOffset; // Set the result to its index.

    if (conversionRate_.reciprocal && result != 0 /* TODO(younies): address zero issue*/)
        result = 1 / result;
    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */