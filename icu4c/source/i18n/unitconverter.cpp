// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "measunit_impl.h"
#include "number_decnum.h"
#include "resource.h"
#include "unicode/stringpiece.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"
#include "unitconverter.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

namespace {

using number::impl::DecNum;

/* Represents a conversion factor */
struct Factor {
    StringPiece source;
    StringPiece target;
    number::impl::DecNum factorNum;
    number::impl::DecNum factorDen;
    number::impl::DecNum offset;

    bool reciprocal = false;

    int32_t constants[CONSTANTS_COUNT] = {};

    Factor(UErrorCode &status) {
        factorNum.setTo(1.0, status);
        factorDen.setTo(1.0, status);
        offset.setTo(0.0, status);
    }

    void multiplyBy(const Factor &rhs, UErrorCode &status) {
        factorNum.multiplyBy(rhs.factorNum, status);
        factorDen.multiplyBy(rhs.factorDen, status);
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] += rhs.constants[i];

        offset.add(rhs.offset, status);
    }

    void divideBy(const Factor &rhs, UErrorCode &status) {
        factorNum.multiplyBy(rhs.factorDen, status);
        factorDen.multiplyBy(rhs.factorNum, status);
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] -= rhs.constants[i];

        offset.add(rhs.offset, status);
    }

    // apply the power to the factor.
    void power(int32_t power, UErrorCode &status) {
        // multiply all the constant by the power.
        for (int i = 0; i < CONSTANTS_COUNT; i++)
            constants[i] *= power;

        bool shouldFlip = power < 0; // This means that after applying the absolute power, we should flip
                                     // the Numerator and Denomerator.
        int32_t absPower = std::abs(power);

        factorNum.power(absPower, status);
        factorDen.power(absPower, status);

        if (shouldFlip) {
            // Flip Numerator and Denomirator.
            DecNum temp(factorNum, status);
            factorNum.setTo(factorDen, status);
            factorDen.setTo(temp, status);
        }
    }

    // Flip the `Factor`, for example, factor= 2/3, flippedFactor = 3/2
    void flip(UErrorCode &status) {
        DecNum temp;
        temp.setTo(factorNum, status);
        factorNum.setTo(factorDen, status);
        factorDen.setTo(temp, status);

        for (int i = 0; i < CONSTANTS_COUNT; i++) {
            constants[i] *= -1;
        }
    }

    // Apply SI prefix to the `Factor`
    void applySiPrefix(UMeasureSIPrefix siPrefix, UErrorCode &status) {
        DecNum siPrefixDecNum;
        siPrefixDecNum.setTo(10, status);
        siPrefixDecNum.power(siPrefix, status);

        factorNum.multiplyBy(siPrefixDecNum, status);
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
void addSingleFactorConstant(Factor &factor, StringPiece baseStr, number::impl::DecNum &power,
                             int32_t signal, UErrorCode &status) {
    if (baseStr == "ft2m") {
        factor.constants[CONSTANT_FT2M] += power.toInt32();
    } else if (baseStr == "G") {
        factor.constants[CONSTANT_G] += power.toInt32();
    } else if (baseStr == "gravity") {
        factor.constants[CONSTANT_GRAVITY] += power.toInt32();
    } else if (baseStr == "lb2kg") {
        factor.constants[CONSTANT_LB2KG] += power.toInt32();
    } else if (baseStr == "cup2m3") {
        factor.constants[CONSTANT_CUP2M3] += power.toInt32();
    } else if (baseStr == "PI") {
        factor.constants[CONSTANT_PI] += power.toInt32();
    } else {
        if (U_FAILURE(status)) return;

        number::impl::DecNum factorNumber;
        factorNumber.setTo(baseStr, status);
        // TODO(younies): do not use the signal, use the power.
        if (signal < 0) { // negative number means add the factor to the denominator.
            factor.factorDen.multiplyBy(factorNumber, status);
        } else {
            factor.factorNum.multiplyBy(factorNumber, status);
        }
    }
}

/*
  Adds single factor for a `Factor` object. Single factor means "23^2", "23.3333", "ft2m^3" ...etc.
  However, complext factor are not included, such as "ft2m^3*200/3"
*/
void addFactorElement(Factor &factor, StringPiece elementStr, int32_t signal, UErrorCode &status) {
    StringPiece baseStr;
    StringPiece powerStr;
    number::impl::DecNum power;
    power.setTo(1.0, status);

    number::impl::DecNum signalDecNum;
    signalDecNum.setTo(signal, status);

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

        power.setTo(powerStr, status);
    } else {
        baseStr = elementStr;
    }

    power.multiplyBy(signalDecNum, status); // The power needs to take the same sign as `signal`.
    addSingleFactorConstant(factor, baseStr, power, signal, status);
}

/*
 * Extracts `Factor` from a complete string factor. e.g. "ft2m^3*1007/cup2m3*3"
 */
void extractFactor(Factor &factor, StringPiece stringFactor, UErrorCode &status) {
    // Set factor to `1`
    factor.factorNum.setTo("1", status);
    factor.factorDen.setTo("1", status);

    int32_t signal = 1;
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

        if (factorData[i] == '/') signal = -1; // Change the signal because we reached the Denominator.
    }
}

// Load factor for a single source
void loadSingleFactor(Factor &factor, StringPiece source, UErrorCode &status) {
    factor.source = source;

    for (const auto &entry : temporarily::dataEntries) {
        if (entry.source == factor.source) {
            factor.target = entry.target;
            extractFactor(factor, entry.factor, status);
            factor.offset.setTo(entry.offset, status);
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
        Factor singleFactor(status);
        auto singleUnit = TempSingleUnit::forMeasureUnit(singleUnits[i], status);

        loadSingleFactor(singleFactor, singleUnit.identifier, status);

        // You must apply SiPrefix before the power, because the power may be will flip the factor.
        singleFactor.applySiPrefix(singleUnit.siPrefix, status);

        singleFactor.power(singleUnit.dimensionality, status);

        factor.multiplyBy(singleFactor, status);
    }
}

void substituteSingleConstant(Factor &factor, int32_t constValue,
                              const DecNum &constSub /* constant actual value, e.g. G= 9.88888 */,
                              UErrorCode &status) {
    bool positive = constValue >= 0;
    int32_t absConstValue = std::abs(constValue);

    double testConst = constSub.toDouble();
    DecNum finalConstSub;
    finalConstSub.setTo(constSub, status);
    finalConstSub.power(absConstValue, status);

    double testfinalconst = finalConstSub.toDouble();

    if (positive) {
        double testfactNum = factor.factorNum.toDouble();

        factor.factorNum.multiplyBy(finalConstSub, status);
        
        testfactNum = factor.factorNum.toDouble();
    } else {
        factor.factorDen.multiplyBy(finalConstSub, status);
    }
}

void substituteConstants(Factor &factor, UErrorCode &status) {

    DecNum constSubs[CONSTANTS_COUNT];

    constSubs[CONSTANT_FT2M].setTo(0.3048, status);
    constSubs[CONSTANT_PI].setTo(3.14159265359, status);
    constSubs[CONSTANT_GRAVITY].setTo(9.80665, status);
    constSubs[CONSTANT_G].setTo("6.67408E-11", status);
    constSubs[CONSTANT_CUP2M3].setTo("0.000236588", status);
    constSubs[CONSTANT_LB2KG].setTo("0.45359237", status);

    for (int i = 0; i < CONSTANTS_COUNT; i++) {
        double test1 = factor.factorNum.toDouble();
        double test2 = factor.factorDen.toDouble();

        if (factor.constants[i] == 0) continue;

        substituteSingleConstant(factor, factor.constants[i], constSubs[i], status);
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

    LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    CharString key;
    key.append("convertUnit/", status);
    key.append(source, status);

    Factor finalFactor(status);
    Factor SourcetoMiddle(status);
    Factor TargettoMiddle(status);

    loadCompoundFactor(SourcetoMiddle, source, status);
    loadCompoundFactor(TargettoMiddle, target, status);

    double testing00 = finalFactor.factorNum.toDouble();
    double testing0 = finalFactor.factorDen.toDouble();

    double testing1 = SourcetoMiddle.factorNum.toDouble();
    double testing2 = SourcetoMiddle.factorDen.toDouble();
    double testing3 = TargettoMiddle.factorNum.toDouble();
    double testing4 = TargettoMiddle.factorDen.toDouble();

    finalFactor.multiplyBy(SourcetoMiddle, status);
    finalFactor.divideBy(TargettoMiddle, status);

    double testing5 = finalFactor.factorNum.toDouble();
    double testing6 = finalFactor.factorDen.toDouble();

    substituteConstants(finalFactor, status);

    double testing8 = finalFactor.factorNum.toDouble();
    double testing9 = finalFactor.factorDen.toDouble();

    conversionRate.source = source;
    conversionRate.target = target;

    conversionRate.factorNum.setTo(finalFactor.factorNum, status);
    conversionRate.factorDen.setTo(finalFactor.factorDen, status);

    if (checkSingularUnits(source, status) && checkSingularUnits(target, status)) {

        double num1 = SourcetoMiddle.factorNum.toDouble();
        double den1 = SourcetoMiddle.factorDen.toDouble();
        double newOffset1 = SourcetoMiddle.offset.toDouble() * den1 / num1;
        conversionRate.sourceOffset.setTo(newOffset1, status);

        double num2 = TargettoMiddle.factorNum.toDouble();
        double den2 = TargettoMiddle.factorDen.toDouble();
        double newOffset2 = TargettoMiddle.offset.toDouble() * den2 / num2;
        conversionRate.targetOffset.setTo(newOffset2, status);
    }

    // TODO(younies): use the database.
    // UnitConversionRatesSink sink(&conversionFactor);
    //  ures_getAllItemsWithFallback(rb.getAlias(), key.data(), sink, status);
}

} // namespace

UnitConverter::UnitConverter(MeasureUnit source, MeasureUnit target, UErrorCode status)
    : conversion_rate_(status) {
    // TODO(younies):: add the test of non-compound units here.
    // Deal with non-compound units only.
    // if (source.getCompoundUnits(status).length() > 1 || target.getCompoundUnits(status).length() > 1
    // ||
    //     U_FAILURE(status)) {
    //     status = UErrorCode::U_ILLEGAL_ARGUMENT_ERROR;
    //     return;
    // }

    loadConversionRate(conversion_rate_, source.getIdentifier(), target.getIdentifier(), status);
}

void UnitConverter::convert(const DecNum &input_value, DecNum &output_value, UErrorCode status) {

    /*
    DecNum result(input_value, status);
    result.multiplyBy(conversion_rate_.factorNum, status);
    result.divideBy(conversion_rate_.factorDen, status);
    // TODO(younies): result.add(conversion_rate_.offset, status);

    if (U_FAILURE(status)) return;

    if (conversion_rate_.reciprocal) {
        DecNum reciprocalResult;
        reciprocalResult.setTo(1, status);
        reciprocalResult.divideBy(result, status);

        output_value.setTo(result, status);
    } else {
        output_value.setTo(result, status);
    }*/

    double result = input_value.toDouble() + conversion_rate_.sourceOffset.toDouble();
    double num = conversion_rate_.factorNum.toDouble();
    double den = conversion_rate_.factorDen.toDouble();
    result *= num / den;

    result -= conversion_rate_.targetOffset.toDouble();

    output_value.setTo(result, status);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */