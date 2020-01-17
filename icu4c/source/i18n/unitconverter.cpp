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
struct ConvertUnit {
    UnicodeString source;
    UnicodeString target;
    int32_t factorNum;
    int32_t factorDen;
    int32_t offsetNum;
    int32_t offsetDen;
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