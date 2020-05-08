// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "number_decimalquantity.h"
#include "cstring.h"
#include "resource.h"
#include "unitsdata.h"
#include "uresimp.h"
#include "util.h"

U_NAMESPACE_BEGIN

namespace {

using icu::number::impl::DecimalQuantity;

void trimSpaces(CharString& factor, UErrorCode& status){
   CharString trimmed;
   for (int i = 0 ; i < factor.length(); i++) {
       if (factor[i] == ' ') continue;

       trimmed.append(factor[i], status);
   }

   factor = std::move(trimmed);
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

// WIP/FIXME: partially due to my skepticism of using the ResourceSink design
// for units resources, this class is currently unused code (dead?) -
// collectUnitPrefs() has all the features we need whereas this doesn't handle
// fallback to usage="default" and region="001" yet. If we want to fix that
// here, this code will get quite a bit more complicated.
class UnitPreferencesSink : public ResourceSink {
  public:
    explicit UnitPreferencesSink(MaybeStackVector<UnitPreference> &out) : outVector(out) {}

    void put(const char *key, ResourceValue &value, UBool /*noFallback*/, UErrorCode &status) {
        if (U_FAILURE(status)) { return; }
        int32_t prefLen;
        ResourceArray unitPrefs = value.getArray(status);
        if (U_FAILURE(status)) { return; }
        prefLen = unitPrefs.getSize();
        for (int32_t i = 0; unitPrefs.getValue(i, value); i++) {
            UnitPreference *up = outVector.emplaceBack();
            if (!up) {
                status = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            ResourceTable unitPref = value.getTable(status);
            if (U_FAILURE(status)) { return; }
            for (int32_t i = 0; unitPref.getKeyAndValue(i, key, value); ++i) {
                if (uprv_strcmp(key, "unit") == 0) {
                    int32_t length;
                    const UChar *u = value.getString(length, status);
                    up->unit.appendInvariantChars(u, length, status);
                } else if (uprv_strcmp(key, "geq") == 0) {
                    int32_t length;
                    const UChar *g = value.getString(length, status);
                    CharString geq;
                    geq.appendInvariantChars(g, length, status);
                    DecimalQuantity dq;
                    dq.setToDecNumber(geq.data(), status);
                    up->geq = dq.toDouble();
                } else if (uprv_strcmp(key, "skeleton") == 0) {
                    int32_t length;
                    const UChar *s = value.getString(length, status);
                    up->skeleton.appendInvariantChars(s, length, status);
                }
            }
        }
    }

  private:
    MaybeStackVector<UnitPreference> &outVector;
};

/**
 * Collects unit preference information from a set of preferences.
 * @param usageData This should be a resource bundle containing a vector of
 * preferences - i.e. the unitPreferenceData tree resources already narrowed
 * down to a particular usage and region (example:
 * "unitPreferenceData/length/road/GB").
 */
void collectUnitPrefs(UResourceBundle *usageData, MaybeStackVector<UnitPreference> &outVector,
                      UErrorCode &status) {
    if (U_FAILURE(status)) return;
    StackUResourceBundle prefBundle;

    int32_t numPrefs = ures_getSize(usageData);
    for (int32_t i = 0; i < numPrefs; i++) {
        ures_getByIndex(usageData, i, prefBundle.getAlias(), &status);

        // Add and populate a new UnitPreference
        int32_t strLen;

        // unit
        const UChar *unitIdent = ures_getStringByKey(prefBundle.getAlias(), "unit", &strLen, &status);
        if (U_FAILURE(status)) return;
        UnitPreference *up = outVector.emplaceBack();
        if (!up) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        up->unit.appendInvariantChars(unitIdent, strLen, status);

        // geq
        const UChar *geq = ures_getStringByKey(prefBundle.getAlias(), "geq", &strLen, &status);
        if (U_SUCCESS(status)) {
            // If we don't mind up->geq having a bad value when
            // U_FAILURE(status), we could extract a function and do a one-liner:
            // up->geq = UCharsToDouble(geq, status);
            CharString cGeq;
            cGeq.appendInvariantChars(geq, strLen, status);
            DecimalQuantity dq;
            dq.setToDecNumber(StringPiece(cGeq.data()), status);
            if (U_FAILURE(status)) return;
            up->geq = dq.toDouble();
        } else if (status == U_MISSING_RESOURCE_ERROR) {
            // We don't mind if geq is missing
            status = U_ZERO_ERROR;
        } else {
            return;
        }

        // skeleton
        const UChar *skel = ures_getStringByKey(prefBundle.getAlias(), "skeleton", &strLen, &status);
        if (U_SUCCESS(status)) {
            up->skeleton.appendInvariantChars(skel, strLen, status);
        } else if (status == U_MISSING_RESOURCE_ERROR) {
            // We don't mind if geq is missing
            status = U_ZERO_ERROR;
        } else {
            return;
        }
    }
}

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

/**
 * Fetches the units data that would be needed for the given usage.
 *
 * @param inputUnit the unit for which input is expected. (NOTE/WIP: If this is
 * known to be a base unit already, we could strip some logic here.)
 */
void getUnitsData(const char *outputRegion, const char *usage, const MeasureUnit &inputUnit,
                  CharString &category, MeasureUnit &baseUnit,
                  MaybeStackVector<ConversionRateInfo> &conversionRates,
                  MaybeStackVector<UnitPreference> &unitPreferences, UErrorCode &status) {
    // This first fetches all conversion info. Next it fetches the category and
    // unit preferences for the given usage and region.

    // In this function we use LocalUResourceBundlePointers for resource bundles
    // that don't change, and StackUResourceBundles for structures we use as
    // fillin.

    getAllConversionRates(conversionRates, status);
    if (U_FAILURE(status)) return;
    if (conversionRates.length() < 1) {
        // This is defensive programming, because this shouldn't happen: if
        // convertSink succeeds, there should be at least one item in
        // conversionRates.
        status = U_MISSING_RESOURCE_ERROR;
        return;
    }
    // TODO(hugovdm): this is broken. We fetch all conversion rates now, the
    // first is nothing special.
    const char *baseIdentifier = conversionRates[0]->baseUnit.data();
    baseUnit = MeasureUnit::forIdentifier(baseIdentifier, status);

    // find category
    LocalUResourceBundlePointer unitsBundle(ures_openDirect(NULL, "units", &status));
    LocalUResourceBundlePointer unitQuantities(
        ures_getByKey(unitsBundle.getAlias(), "unitQuantities", NULL, &status));
    int32_t categoryLength;
    const UChar *uCategory =
        ures_getStringByKey(unitQuantities.getAlias(), baseIdentifier, &categoryLength, &status);
    category.appendInvariantChars(uCategory, categoryLength, status);

    // Find the right unit preference bundle
    StackUResourceBundle stackBundle; // Reused as we climb the tree
    ures_getByKey(unitsBundle.getAlias(), "unitPreferenceData", stackBundle.getAlias(), &status);
    ures_getByKey(stackBundle.getAlias(), category.data(), stackBundle.getAlias(), &status);
    if (U_FAILURE(status)) { return; }
    ures_getByKey(stackBundle.getAlias(), usage, stackBundle.getAlias(), &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        // Requested usage does not exist, so we use "default".
        status = U_ZERO_ERROR;
        ures_getByKey(stackBundle.getAlias(), "default", stackBundle.getAlias(), &status);
    }
    ures_getByKey(stackBundle.getAlias(), outputRegion, stackBundle.getAlias(), &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        // Requested region does not exist, so we use "001".
        status = U_ZERO_ERROR;
        ures_getByKey(stackBundle.getAlias(), "001", stackBundle.getAlias(), &status);
    }

    // Collect all the preferences into unitPreferences
    collectUnitPrefs(stackBundle.getAlias(), unitPreferences, status);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
