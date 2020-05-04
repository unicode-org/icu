// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "cstring.h"
#include "resource.h"
#include "unitsdata.h"
#include "uresimp.h"
#include "util.h"

U_NAMESPACE_BEGIN

namespace {

void trimSpaces(CharString& factor, UErrorCode& status){
   CharString trimmed;
   for (int i = 0 ; i < factor.length(); i++) {
       if (factor[i] == ' ') continue;

       trimmed.append(factor[i], status);
   }

   factor.clear();
   factor.append(trimmed, status);
}

/**
 * A ResourceSink that collects conversion rate information.
 *
 * This class is for use by ures_getAllItemsWithFallback.
 */
class ConversionRateDataSink : public ResourceSink {
  public:
    /**
     * Constructor.
     * @param out The vector to which ConversionRateInfo instances are to be
     * added. This vector must outlive the use of the ResourceSink.
     */
    explicit ConversionRateDataSink(MaybeStackVector<ConversionRateInfo> *out) : outVector(out) {}

    /**
     * Adds the conversion rate information found in value to the output vector.
     *
     * Each call to put() collects a ConversionRateInfo instance for the
     * specified source unit identifier into the vector passed to the
     * constructor, but only if an identical instance isn't already present.
     *
     * @param source The source unit identifier.
     * @param value A resource containing conversion rate info (the base unit
     * and factor, and possibly an offset).
     * @param noFallback Ignored.
     * @param status The standard ICU error code output parameter.
     */
    void put(const char *source, ResourceValue &value, UBool /*noFallback*/, UErrorCode &status) {
        if (U_FAILURE(status)) return;
        if (uprv_strcmp(source, "convertUnits") != 0) {
            // This is very strict, however it is the cheapest way to be sure
            // that with `value`, we're looking at the convertUnits table.
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        ResourceTable conversionRateTable = value.getTable(status);
        const char *srcUnit;
        // We're reusing `value`, which seems to be a common pattern:
        for (int32_t unit = 0; conversionRateTable.getKeyAndValue(unit, srcUnit, value); unit++) {
            ResourceTable unitTable = value.getTable(status);
            const char *key;
            UnicodeString baseUnit = ICU_Utility::makeBogusString();
            UnicodeString factor = ICU_Utility::makeBogusString();
            UnicodeString offset = ICU_Utility::makeBogusString();
            for (int32_t i = 0; unitTable.getKeyAndValue(i, key, value); i++) {
                if (uprv_strcmp(key, "target") == 0) {
                    baseUnit = value.getUnicodeString(status);
                } else if (uprv_strcmp(key, "factor") == 0) {
                    factor = value.getUnicodeString(status);
                } else if (uprv_strcmp(key, "offset") == 0) {
                    offset = value.getUnicodeString(status);
                }
            }
            if (U_FAILURE(status)) return;
            if (baseUnit.isBogus() || factor.isBogus()) {
                // We could not find a usable conversion rate: bad resource.
                status = U_MISSING_RESOURCE_ERROR;
                return;
            }

            // We don't have this ConversionRateInfo yet: add it.
            ConversionRateInfo *cr = outVector->emplaceBack();
            if (!cr) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            } else {
                cr->sourceUnit.append(srcUnit, status);
                cr->baseUnit.appendInvariantChars(baseUnit, status);
                cr->factor.appendInvariantChars(factor, status);
                trimSpaces(cr->factor, status);
                if (!offset.isBogus()) cr->offset.appendInvariantChars(offset, status);
            }
        }
        return;
    }

  private:
    MaybeStackVector<ConversionRateInfo> *outVector;
};

} // namespace

void U_I18N_API getAllConversionRates(MaybeStackVector<ConversionRateInfo> &result, UErrorCode &status) {
    LocalUResourceBundlePointer unitsBundle(ures_openDirect(NULL, "units", &status));
    ConversionRateDataSink sink(&result);
    ures_getAllItemsWithFallback(unitsBundle.getAlias(), "convertUnits", sink, status);
}

const ConversionRateInfo *ConversionRates::extractConversionInfo(StringPiece source,
                                                                 UErrorCode &status) const {
    for (size_t i = 0, n = conversionInfo_.length(); i < n; ++i) {
        if (conversionInfo_[i]->sourceUnit.toStringPiece() == source) return conversionInfo_[i];
    }

    status = U_INTERNAL_PROGRAM_ERROR;
    return nullptr;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
