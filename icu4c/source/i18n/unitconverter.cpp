// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <cmath>

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
// Helpers

typedef enum Signal {
    NEGATIVE = -1,
    POSITIVE = 1,
} Signal;

double strToDouble(StringPiece strNum) {
    char charNum[strNum.length()];
    for (int i = 0; i < strNum.length(); i++) {
        charNum[i] = strNum.data()[i];
    }

    char *end;
    return std::strtod(charNum, &end);
}

/* Represents a conversion factor */
struct Factor {
    double factorNum = 1;
    double factorDen = 1;
    double offset = 0;
    bool reciprocal = false;
    int32_t constants[CONSTANTS_COUNT] = {};

    void multiplyBy(const Factor &rhs, UErrorCode &status) {
        factorNum *= rhs.factorNum;
        factorDen *= rhs.factorDen;
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] += rhs.constants[i];

        offset += rhs.offset;
    }

    void divideBy(const Factor &rhs, UErrorCode &status) {
        factorNum *= rhs.factorDen;
        factorDen *= rhs.factorNum;
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] -= rhs.constants[i];

        offset += rhs.offset;
    }

    // apply the power to the factor.
    void power(int32_t power, UErrorCode &status) {
        // multiply all the constant by the power.
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] *= power;

        bool shouldFlip = power < 0; // This means that after applying the absolute power, we should flip
                                     // the Numerator and Denomerator.
        int32_t absPower = std::abs(power);

        factorNum = std::pow(factorNum, std::abs(power));
        factorDen = std::pow(factorDen, std::abs(power));

        if (shouldFlip) {
            // Flip Numerator and Denomirator.
            std::swap(factorNum, factorDen);
        }
    }

    // Flip the `Factor`, for example, factor= 2/3, flippedFactor = 3/2
    void flip(UErrorCode &status) {
        std::swap(factorNum, factorDen);

        for (int i = 0; i < CONSTANTS_COUNT; i++) {
            constants[i] *= -1;
        }
    }

    // Apply SI prefix to the `Factor`
    void applySiPrefix(UMeasureSIPrefix siPrefix, UErrorCode &status) {
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
        if (U_FAILURE(status)) {
            return;
        }

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

            if (U_FAILURE(status)) {
                return;
            }
        }
    }

  private:
    Factor *conversionFactor;
};

/*/
 * Add single factor element to the `Factor`. e.g "ft3m", "2.333" or "cup2m3". But not "cup2m3^3".
 */
void addSingleFactorConstant(Factor &factor, StringPiece baseStr, int32_t power, Signal signal,
                             UErrorCode &status) {
    if (baseStr == "ft2m") {
        factor.constants[CONSTANT_FT2M] += power * signal;
    } else if (baseStr == "G") {
        factor.constants[CONSTANT_G] += power * signal;
    } else if (baseStr == "gravity") {
        factor.constants[CONSTANT_GRAVITY] += power * signal;
    } else if (baseStr == "lb2kg") {
        factor.constants[CONSTANT_LB2KG] += power * signal;
    } else if (baseStr == "cup2m3") {
        factor.constants[CONSTANT_CUP2M3] += power * signal;
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

    addSingleFactorConstant(factor, baseStr, power, signal, status);
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
    for (const auto &entry : temporarily::dataEntries) {
        if (entry.source == source) {
            extractFactor(factor, entry.factor, status);
            factor.offset = strToDouble(entry.offset);
            factor.reciprocal = factor.reciprocal;

            return;
        }
    }

    status = UErrorCode::U_ARGUMENT_TYPE_MISMATCH;
}

// Load Factor for compound source
// TODO(younies): handle `one-per-second` case
void loadCompoundFactor(Factor &factor, StringPiece source, UErrorCode &status) {
    icu::MeasureUnit compoundSourceUnit = icu::MeasureUnit::forIdentifier(source, status);
    auto singleUnits = compoundSourceUnit.splitToSingleUnits(status);
    for (int i = 0; i < singleUnits.length(); i++) {
        Factor singleFactor;
        auto singleUnit = TempSingleUnit::forMeasureUnit(singleUnits[i], status);

        loadSingleFactor(singleFactor, singleUnit.identifier, status);

        // You must apply SiPrefix before the power, because the power may be will flip the factor.
        singleFactor.applySiPrefix(singleUnit.siPrefix, status);

        singleFactor.power(singleUnit.dimensionality, status);

        factor.multiplyBy(singleFactor, status);
    }
}

void substituteSingleConstant(Factor &factor, int32_t constantPower,
                              double constantValue /* constant actual value, e.g. G= 9.88888 */,
                              UErrorCode &status) {
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
    constantsValues[CONSTANT_CUP2M3] = 0.000236588;
    constantsValues[CONSTANT_LB2KG] = 0.45359237;

    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        if (factor.constants[i] == 0) continue;

        substituteSingleConstant(factor, factor.constants[i], constantsValues[i], status);
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
                        UErrorCode &status) {

    // LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    // CharString key;
    // key.append("convertUnit/", status);
    // key.append(source, status);

    Factor finalFactor;
    Factor SourcetoMiddle;
    Factor TargettoMiddle;

    // Load needed factors (TODO(younies): illustrate more)
    loadCompoundFactor(SourcetoMiddle, source, status);
    loadCompoundFactor(TargettoMiddle, target, status);

    // Merger Factors
    finalFactor.multiplyBy(SourcetoMiddle, status);
    finalFactor.divideBy(TargettoMiddle, status);

    // Substitute constants
    substituteConstants(finalFactor, status);

    conversionRate.source = source;
    conversionRate.target = target;

    conversionRate.factorNum = finalFactor.factorNum;
    conversionRate.factorDen = finalFactor.factorDen;

    if (checkSingularUnits(source, status) && checkSingularUnits(target, status)) {
        conversionRate.sourceOffset =
            SourcetoMiddle.offset * SourcetoMiddle.factorDen / SourcetoMiddle.factorNum;
        conversionRate.targetOffset =
            TargettoMiddle.offset * TargettoMiddle.factorDen / TargettoMiddle.factorNum;
    }

    // TODO(younies): use the database.
    // UnitConversionRatesSink sink(&conversionFactor);
    //  ures_getAllItemsWithFallback(rb.getAlias(), key.data(), sink, status);
}

} // namespace

UnitConverter::UnitConverter(MeasureUnit source, MeasureUnit target, UErrorCode status) {
    loadConversionRate(conversionRate_, source.getIdentifier(), target.getIdentifier(), status);
}

double UnitConverter::convert(double inputValue, UErrorCode status) {
    double result = inputValue + conversionRate_.sourceOffset;
    result *= conversionRate_.factorNum / conversionRate_.factorDen;
    result -= conversionRate_.targetOffset;

    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */