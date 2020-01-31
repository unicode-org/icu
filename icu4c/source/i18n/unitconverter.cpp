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
                conversionRate->target = value.getUnicodeString(status);
            }

            if (U_FAILURE(status)) {
                return;
            }
        }
    }

  private:
    ConversionRate *conversionRate;
};

/*
  Adds single factor for a `Factor` object. Single factor means "23^2", "23.3333", "ft2m^3" ...etc.
  However, complext factor are not included, such as "ft2m^3*200/3"
*/
void addSingleFactorConstant(Factor &factor, StringPiece elementStr, int32_t signal,
                             UErrorCode &status) {
    StringPiece baseStr;
    StringPiece powerStr;
    number::impl::DecNum power;
    power.setTo(1.0, status);

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
    }
}

void addFactorElement(Factor &factor, const UnicodeString &stringFactor, int32_t start, int32_t length,
                      int32_t signal, UErrorCode &status) {
    if (stringFactor.compare(start, length, u"ft2m")) {
        factor.constants[CONSTANT_FT2M] += signal;
    } else if (stringFactor.compare(start, length, u"G")) {
        factor.constants[CONSTANT_G] += signal;
    } else if (stringFactor.compare(start, length, u"cup2m3")) {
        factor.constants[CONSTANT_CUP2M3] += signal;
    } else if (stringFactor.compare(start, length, u"pi")) {
        factor.constants[CONSTANT_PI] += signal;
    } else {
        CharString tempUTF8;
        tempUTF8.appendInvariantChars(stringFactor.tempSubString(start, length), status);

        if (status != U_ZERO_ERROR)
            return;

        number::impl::DecNum decNum;
        decNum.setTo(tempUTF8.data(), status);
        if (status != U_ZERO_ERROR)
            return;
    }
}

Factor extractFactor(UnicodeString stringFactor, UErrorCode &status) {
    Factor result;

    int32_t signal = 1;
    for (int32_t i = 0, start = 0, length = stringFactor.length(); i < length; i++) {
        if (stringFactor[i] == '*' || stringFactor[i] == '/') {
            addFactorElement(result, stringFactor, start, i - start, signal, status);

            start = i + 1; // Set `start` to point to the new start point.
        } else if (i == length - 1) {
            // Last element
            addFactorElement(result, stringFactor, start, i - start + 1, signal, status);
        }

        if (stringFactor[i] == '/')
            signal = -1; // Change the signal because we reached the Denominator.
    }

    return result;
}

ConversionRate loadConversionRate(StringPiece source, UErrorCode &status) {
    LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    CharString key;
    key.append("convertUnit/", status);
    key.append(source, status);

    ConversionRate conversionRate;
    UnitConversionRatesSink sink(&conversionRate);

    ures_getAllItemsWithFallback(rb.getAlias(), key.data(), sink, status);

    return conversionRate;
}

} // namespace

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */