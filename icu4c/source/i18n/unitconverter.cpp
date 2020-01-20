// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "resource.h"
#include "unicode/stringpiece.h"
#include "unicode/unistr.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

namespace {

//////////////////////////
/// BEGIN DATA LOADING ///
//////////////////////////

// Loaded Data Skeleton.
enum Constants {
    CONSTANT_FT2M, // ft2m stand for foot to meter.
    CONSTANT_PI,
    CONSTANT_G,      // G stands for Gravity.
    CONSTANT_CUP2M3, // CUP2M3 stand for cup to cubic meter.

    // Must be the last element.
    CONSTANTS_COUNT
};

struct Factor {
    int64_t factorNum;
    int64_t factorDen;
    int8_t constants[CONSTANTS_COUNT] = {};
};

struct ConvertUnit {
    UnicodeString source;
    UnicodeString target;
    Factor factor;
    bool reciprocal;
};

class UnitConversionRatesSink : public ResourceSink {
  public:
    explicit UnitConversionRatesSink(ConvertUnit *convertUnit) : convertUnit(convertUnit) {}

    void put(const char *key, ResourceValue &value, UBool /*noFallback*/,
             UErrorCode &status) U_OVERRIDE {
        ResourceTable convertUnitTable = value.getTable(status);
        if (U_FAILURE(status)) {
            return;
        }

        for (int32_t i = 0; convertUnitTable.getKeyAndValue(i, key, value); ++i) {
            StringPiece keySP(key);

            if (keySP == "factor") {
                value.getUnicodeString(status);

            }

            else if (keySP == "offset") {
                value.getUnicodeString(status);
            }

            else if (keySP == "target") {
                convertUnit->target = value.getUnicodeString(status);
            }

            if (U_FAILURE(status)) {
                return;
            }
        }
    }

  private:
    ConvertUnit *convertUnit;
};

ConvertUnit loadConvertUnit(StringPiece source, UErrorCode &status) {
    LocalUResourceBundlePointer rb(ures_openDirect(nullptr, "units", &status));
    CharString key;
    key.append("convertUnits/", status);
    key.append(source, status);

    ConvertUnit convertUnit;
    UnitConversionRatesSink sink(&convertUnit);

    ures_getAllItemsWithFallback(rb.getAlias(), key.data(), sink, status);

    return convertUnit;
}

} // namespace

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */