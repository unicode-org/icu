// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <utility>

#include "cmemory.h"
#include "cstring.h"
#include "number_decimalquantity.h"
#include "resource.h"
#include "unitsrouter.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

namespace {
/* Internal Data */
// // Preference of a single unit.
// struct UnitPreference {
//     StringPiece identifier;

//     // Represents the limit of the largest unit in the identifier that the quantity must be greater than
//     // or equal.
//     // e.g. geq: 0.3 for a unit "foot-and-inch"
//     double limit;
// };

// MaybeStackVector<UnitPreference> extractUnitPreferences(StringPiece locale, StringPiece usage,
//                                                         StringPiece category) {
//     MaybeStackVector<UnitPreference> result;

//     // TODO(hugovdm): extract from the database all the UnitPreference for the `locale`, `category` and
//     // `usage` in order.

//     return result;
// }

// StringPiece extractUnitCategory(MeasureUnit unit) {
//     StringPiece result;

//     // TODO(hugovdm): extract the category of a unit from their MeasureUnits.

//     return result;
// }

using icu::number::impl::DecimalQuantity;

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
            } else if (uprv_strcmp(key, "offset") == 0) {
                int32_t length;
                const UChar *o = value.getString(length, status);
                cr->offset.appendInvariantChars(o, length, status);
            } else if (uprv_strcmp(key, "target") == 0) {
                int32_t length;
                const UChar *t = value.getString(length, status);
                cr->target.appendInvariantChars(t, length, status);
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

void putUnitPref(UResourceBundle *usageData, MaybeStackVector<UnitPreference> &outVector,
                 UErrorCode &status) {
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
            // fprintf(stderr, "status: %s, geq: %s, dq.toDouble(): %f\n", u_errorName(status),
            // cGeq.data(),
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

    StackUResourceBundle convertUnitsBundle;
    // CharString has initial capacity 40. Key appending only gets slow when we
    // go beyond. TODO(hugovdm): look at how often this might happen though?
    // Each append could be a re-allocation.
    CharString key;
    // key.append("convertUnits/", status);
    key.append(inputBase.getIdentifier(), status);
    ConvertUnitsSink convertSink(conversionInfo);
    ures_getByKey(unitsBundle.getAlias(), "convertUnits", convertUnitsBundle.getAlias(), &status);
    ures_getAllItemsWithFallback(convertUnitsBundle.getAlias(), key.data(), convertSink, status);
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
    StackUResourceBundle stackBundle;
    ures_getByKey(unitsBundle.getAlias(), "unitPreferenceData", stackBundle.getAlias(), &status);
    ures_getByKey(stackBundle.getAlias(), category.data(), stackBundle.getAlias(), &status);
    if (U_FAILURE(status)) { return; }
    ures_getByKey(stackBundle.getAlias(), usage, stackBundle.getAlias(), &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        // Requested usage does not exist, use "default".
        status = U_ZERO_ERROR;
        ures_getByKey(stackBundle.getAlias(), "default", stackBundle.getAlias(), &status);
    }
    // if (U_FAILURE(status)) fprintf(stderr, "failed getting usage %s: %s\n", usage,
    // u_errorName(status));
    ures_getByKey(stackBundle.getAlias(), outputRegion, stackBundle.getAlias(), &status);
    if (status == U_MISSING_RESOURCE_ERROR) {
        // Requested region does not exist, use "001".
        status = U_ZERO_ERROR;
        ures_getByKey(stackBundle.getAlias(), "001", stackBundle.getAlias(), &status);
    }
    // if (U_FAILURE(status)) fprintf(stderr, "failed getting region %s: %s\n", outputRegion,
    // u_errorName(status));
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

    // Load ConversionRateInfo for each of the units in unitPreferences.
    //
    // WIP/FIXME: this currently adds plenty of duplicates. hugovdm will soon
    // adapt the code to skip dupes (or add conversion info for units with SI
    // prefixes?)
    for (int32_t i = 0; i < unitPreferences.length(); i++) {
        UnitPreference *up = unitPreferences[i];
        MeasureUnit prefUnitBase = MeasureUnit::forIdentifier(up->unit.data(), status)
                                       .withSIPrefix(UMEASURE_SI_PREFIX_ONE, status);
        ures_getAllItemsWithFallback(convertUnitsBundle.getAlias(), prefUnitBase.getIdentifier(), convertSink, status);
    }
}

UnitsRouter::UnitsRouter(MeasureUnit inputUnit, StringPiece locale, StringPiece usage,
                         UErrorCode &status) {
    // StringPiece unitCategory = extractUnitCategory(inputUnit);
    // MaybeStackVector<UnitPreference> preferences = extractUnitPreferences(locale, usage, unitCategory);
    const char *region = "001"; // FIXME extract from locale.
    CharString category;
    MeasureUnit baseUnit;
    MaybeStackVector<ConversionRateInfo> conversionInfo;
    MaybeStackVector<UnitPreference> unitPreferences;
    getUnitsData(region, usage.data(), inputUnit, category, baseUnit, conversionInfo, unitPreferences,
                 status);

    for (int i = 0, n = unitPreferences.length(); i < n; ++i) {
        const auto &preference = *unitPreferences[i];
        MeasureUnit complexTargetUnit = MeasureUnit::forIdentifier(preference.unit.data(), status);
        // TODO(younies): Find a way to emplaceBack `ConverterPreference`
        // converterPreferences_.emplaceBack(
        //     std::move(ConverterPreference(inputUnit, complexTargetUnit, preference.geq, status)));
    }
}

MaybeStackVector<Measure> UnitsRouter::route(double quantity, UErrorCode &status) {
    for (int i = 0, n = converterPreferences_.length() - 1; i < n; i++) {
        const auto &converterPreference = *converterPreferences_[i];

        if (converterPreference.converter.greaterThanOrEqual(quantity, converterPreference.limit)) {
            return converterPreference.converter.convert(quantity, status);
        }
    }

    const auto &converterPreference =
        *converterPreferences_[converterPreferences_.length() - 1]; // Last Element
    return converterPreference.converter.convert(quantity, status);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
