#include "wip_units_resource_loader.h"
#include "cstring.h" // uprv_strcmp
#include "cmemory.h" // MaybeStackVector
#include "number_decimalquantity.h" // DecimalQuantity
#include "uresimp.h" // ures_getAllItemsWithFallback

using namespace icu;
using icu::number::impl::DecimalQuantity;

namespace {

// Resources. It should be migrated to a permanent location with updated API,
// once we know what that will look like and where that will be.

class ConvertUnitsSink : public ResourceSink {
  public:
    explicit ConvertUnitsSink(MaybeStackVector<ConversionRateInfo> &out) : outVector(out) {}

    // WIP: look into noFallback
    void put(const char *key, ResourceValue &value, UBool /*noFallback*/, UErrorCode &status) {
        ResourceTable conversionRateTable = value.getTable(status);
        if (U_FAILURE(status)) {
            // fprintf(stderr, "%s: getTable failed\n", u_errorName(status));
            return;
        }

        ConversionRateInfo *cr = outVector.emplaceBack();
        if (!cr) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }

        int32_t length = uprv_strlen(key);
        cr->source.append(key, length, status);
        if (U_FAILURE(status)) {
            // fprintf(stderr, "%s: source.append failed\n", u_errorName(status));
            return;
        }
        for (int32_t i = 0; conversionRateTable.getKeyAndValue(i, key, value); ++i) {
            if (uprv_strcmp(key, "factor") == 0) {
                int32_t length;
                const UChar *f = value.getString(length, status);
                cr->factor.appendInvariantChars(f, length, status);
                cr->factorUChar = f;
            } else if (uprv_strcmp(key, "offset") == 0) {
                int32_t length;
                const UChar *o = value.getString(length, status);
                cr->offset.appendInvariantChars(o, length, status);
                cr->offsetUChar = o;
            } else if (uprv_strcmp(key, "target") == 0) {
                int32_t length;
                const UChar *t = value.getString(length, status);
                cr->target.appendInvariantChars(t, length, status);
                cr->targetUChar = t;
            }
        }
    }
  private:
    MaybeStackVector<ConversionRateInfo> &outVector;
};

class UnitPreferencesSink : public ResourceSink {
  public:
    explicit UnitPreferencesSink(MaybeStackVector<UnitPreference> &out) : outVector(out) {}

    // WIP: look into noFallback
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

void putUnitPref(UResourceBundle *usageData,
                 MaybeStackVector<UnitPreference> &outVector, UErrorCode &status) {
    if (U_FAILURE(status)) { return; }

    UResourceBundle *prefBundle = NULL;
    int32_t numPrefs = ures_getSize(usageData);
    for (int32_t i = 0; i < numPrefs; i++) {
        prefBundle = ures_getByIndex(usageData, i, prefBundle, &status);
        if (U_FAILURE(status)) {
            // fprintf(stderr, "failed getting index %d/%d: %s\n", i, numPrefs, u_errorName(status));
            status = U_ZERO_ERROR;
            break;
        }
        int32_t len;
        const UChar *unitIdent = ures_getStringByKey(prefBundle, "unit", &len, &status);
        if (U_FAILURE(status)) {
            // fprintf(stderr, "open unit failed: %s\n", u_errorName(status));
            break;
        }

        UnitPreference *up = outVector.emplaceBack();
        if (!up) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        up->unit.appendInvariantChars(unitIdent, len, status);
        if (U_FAILURE(status)) {
            // fprintf(stderr, "failed appending unitIdent: %s\n", u_errorName(status));
            status = U_ZERO_ERROR;
            break;
        }
        const UChar *geq = ures_getStringByKey(prefBundle, "geq", &len, &status);
        if (U_SUCCESS(status)) {
            CharString cGeq;
            cGeq.appendInvariantChars(geq, len, status);
            DecimalQuantity dq;
            dq.setToDecNumber(StringPiece(cGeq.data()), status);
            // fprintf(stderr, "status: %s, geq: %s, dq.toDouble(): %f\n", u_errorName(status), cGeq.data(),
            //         dq.toDouble());
            up->geq = dq.toDouble();
        } else if (status == U_MISSING_RESOURCE_ERROR) {
            status = U_ZERO_ERROR;
        }
        if (U_FAILURE(status)) {
            // fprintf(stderr, "failed appending geq: %s\n", u_errorName(status));
            break;
        }
        const UChar *skel = ures_getStringByKey(prefBundle, "skeleton", &len, &status);
        if (U_SUCCESS(status)) {
            up->skeleton.appendInvariantChars(skel, len, status);
        } else if (status == U_MISSING_RESOURCE_ERROR) {
            status = U_ZERO_ERROR;
        }
    }
    ures_close(prefBundle);
}

} // namespace

/**
 * Fetches required data FIXME.
 *
 * @param inputUnit the unit for which input is expected. (NOTE/WIP: If this is
 * known to be a base unit already, we could strip some logic here.)
 */
void getUnitsData(const char *outputRegion, const char *usage, const MeasureUnit &inputUnit,
                  CharString &category, MeasureUnit &baseUnit,
                  MaybeStackVector<ConversionRateInfo> &conversionInfo,
                  MaybeStackVector<UnitPreference> &unitPreferences, UErrorCode &status) {
    // One can also use a StackUResourceBundle as a fill-in.
    LocalUResourceBundlePointer unitsBundle(ures_openDirect(NULL, "units", &status));
    if (U_FAILURE(status)) {
        // fprintf(stderr, "%s: ures_openDirect %s\n", u_errorName(status), "units");
        return;
    }

    MeasureUnit inputBase = inputUnit.withSIPrefix(UMEASURE_SI_PREFIX_ONE, status);
    if (uprv_strcmp(inputBase.getIdentifier(), "gram") == 0) { inputBase = MeasureUnit::getKilogram(); }
    // if (U_FAILURE(status)) fprintf(stderr, "failed getting inputBase: %s\n", u_errorName(status));

    StackUResourceBundle stackBundle;
    // CharString has initial capacity 40. Key appending only gets slow when we
    // go beyond. TODO(hugovdm): look at how often this might happen though?
    // Each append could be a re-allocation.
    CharString key;
    // key.append("convertUnits/", status);
    key.append(inputBase.getIdentifier(), status);
    ConvertUnitsSink convertSink(conversionInfo);
    ures_getByKey(unitsBundle.getAlias(), "convertUnits", stackBundle.getAlias(), &status);
    ures_getAllItemsWithFallback(stackBundle.getAlias(), key.data(), convertSink, status);
    const CharString &baseIdentifier = conversionInfo[0]->target;
    baseUnit = MeasureUnit::forIdentifier(baseIdentifier.data(), status);

    // key.clear();
    // key.append("unitQuantities/", status);
    // key.append(baseIdentifier, status);
    // ures_findSubResource(unitsBundle.getAlias(), key.data(), fillIn, &status);
    // Now we still need to convert to string.
    LocalUResourceBundlePointer unitQuantities(
        ures_getByKey(unitsBundle.getAlias(), "unitQuantities", NULL, &status));
    int32_t categoryLength;
    const UChar *uCategory =
        ures_getStringByKey(unitQuantities.getAlias(), baseIdentifier.data(), &categoryLength, &status);
    category.appendInvariantChars(uCategory, categoryLength, status);

    // We load the region-specific unit preferences into stackBundle, reusing it
    // for fill-in every step of the way:
    ures_getByKey(unitsBundle.getAlias(), "unitPreferenceData", stackBundle.getAlias(), &status);
    ures_getByKey(stackBundle.getAlias(), category.data(), stackBundle.getAlias(), &status);
    if (U_FAILURE(status)) { return; }
    ures_getByKey(stackBundle.getAlias(), usage, stackBundle.getAlias(), &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        // Requested usage does not exist, use "default".
        status = U_ZERO_ERROR;
        ures_getByKey(stackBundle.getAlias(), "default", stackBundle.getAlias(), &status);
    }
    // if (U_FAILURE(status)) fprintf(stderr, "failed getting usage %s: %s\n", usage, u_errorName(status));
    ures_getByKey(stackBundle.getAlias(), outputRegion, stackBundle.getAlias(), &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        // Requested region does not exist, use "001".
        status = U_ZERO_ERROR;
        ures_getByKey(stackBundle.getAlias(), "001", stackBundle.getAlias(), &status);
    }
    // if (U_FAILURE(status)) fprintf(stderr, "failed getting region %s: %s\n", outputRegion, u_errorName(status));
    putUnitPref(stackBundle.getAlias(), unitPreferences, status);
    // if (U_FAILURE(status)) fprintf(stderr, "putUnitPref failed: %s\n", u_errorName(status));

    // An alterantive for the above "We load ..." block, I don't think this is neater:
    // key.clear();
    // key.append("unitPreferenceData/", status);
    // key.append(category, status).append("/", status);
    // key.append(usage, status).append("/", status); // FIXME: fall back to "default"
    // key.append(outputRegion, status); // FIXME: fall back to "001"
    // UnitPreferencesSink prefsSink(unitPreferences);
    // ures_getAllItemsWithFallback(unitsBundle.getAlias(), key.data(), prefsSink, status);
}
