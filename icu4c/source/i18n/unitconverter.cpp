// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
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

decNumber UnitConverter::convert(double quantity, UErrorCode status) {
    DecNum result;
    result.setTo(quantity, status);

    result.multiplyBy(this->conversion_rate_.factorNum, status);
    result.divideBy(this->conversion_rate_.factorDen, status);
     
    if (conversion_rate_.reciprocal) { 
        DecNum reciprocalResult;
        reciprocalResult.setTo(1, status);
        reciprocalResult.divideBy(result, status);

        return *(reciprocalResult.getRawDecNumber());
    }

    return *(result.getRawDecNumber());
}

//////////////////////////
/// BEGIN DATA LOADING ///
//////////////////////////

class UnitConversionRatesSink : public ResourceSink {
  public:
    explicit UnitConversionRatesSink(ConversionRate *conversionRate) : conversionRate(conversionRate) {}

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
    ConversionRate *conversionRate;
};

/*/
 * Add single factor element to the `Factor`. e.g "ft3m", "2.333" or "cup2m3". But not "cup2m3^3".
 */
void addSingleFactorConstant(Factor &factor, StringPiece baseStr, number::impl::DecNum &power,
                             int32_t signal, UErrorCode &status) {
    if (baseStr == "ft2m") {
        // factor.constants[CONSTANT_FT2M] +=  power;
    } else if (baseStr == "G") {
        // factor.constants[CONSTANT_G] += power;
    } else if (baseStr == "cup2m3") {
        // factor.constants[CONSTANT_CUP2M3] += power;
    } else if (baseStr == "pi") {
        // factor.constants[CONSTANT_PI] += power;
    } else {
        if (status != U_ZERO_ERROR)
            return;

        number::impl::DecNum factorNumber;
        factorNumber.setTo(baseStr.data(), status);

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

    power.multiplyBy(signalDecNum, status);
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
            addFactorElement(factor, stringFactor.substr(start, i), signal, status);

            start = i + 1; // Set `start` to point to the start of the new element.
        } else if (i == n - 1) {
            // Last element
            addFactorElement(factor, stringFactor.substr(start, i + 1), signal, status);
        }

        if (factorData[i] == '/')
            signal = -1; // Change the signal because we reached the Denominator.
    }
}

void loadConversionRate(ConversionRate &conversionRate, StringPiece source, StringPiece target,
                        UErrorCode &status) {
    LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    CharString key;
    key.append("convertUnit/", status);
    key.append(source, status);

    UnitConversionRatesSink sink(&conversionRate);

    ures_getAllItemsWithFallback(rb.getAlias(), key.data(), sink, status);
}

} // namespace

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */