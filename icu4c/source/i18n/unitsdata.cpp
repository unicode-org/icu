// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <utility>

#include "cstring.h"
#include "resource.h"
#include "unitsdata.h"
#include "uresimp.h"

U_NAMESPACE_BEGIN

namespace {

/**
 * A ResourceSink that collects conversion rate information.
 *
 * This class is for use by ures_getAllItemsWithFallback. Example code for
 * collecting conversion info for "mile" and "foot" into conversionInfoOutput:
 *
 *     UErrorCode status = U_ZERO_ERROR;
 *     ures_getByKey(unitsBundle, "convertUnits", &fillIn, &status);
 *     MaybeStackVector<ConversionRateInfo> conversionInfoOutput;
 *     ConversionRateDataSink convertSink(conversionInfoOutput);
 *     ures_getAllItemsWithFallback(fillIn, "mile", convertSink, status);
 *     ures_getAllItemsWithFallback(fillIn, "foot", convertSink, status);
 */
class ConversionRateDataSink : public ResourceSink {
  public:
    /**
     * Constructor.
     * @param out The vector to which ConversionRateInfo instances are to be
     * added.
     */
    explicit ConversionRateDataSink(MaybeStackVector<ConversionRateInfo> &out) : outVector(out) {}

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
        ResourceTable conversionRateTable = value.getTable(status);
        if (U_FAILURE(status)) return;

        // Collect base unit, factor and offset from the resource.
        int32_t lenSource = uprv_strlen(source);
        const UChar *baseUnit = NULL, *factor = NULL, *offset = NULL;
        int32_t lenBaseUnit, lenFactor, lenOffset;
        const char *key;
        for (int32_t i = 0; conversionRateTable.getKeyAndValue(i, key, value); ++i) {
            if (uprv_strcmp(key, "target") == 0) {
                baseUnit = value.getString(lenBaseUnit, status);
            } else if (uprv_strcmp(key, "factor") == 0) {
                factor = value.getString(lenFactor, status);
            } else if (uprv_strcmp(key, "offset") == 0) {
                offset = value.getString(lenOffset, status);
            }
        }
        if (baseUnit == NULL || factor == NULL) {
            status = U_MISSING_RESOURCE_ERROR;
            return;
        }

        // Check if we already have the conversion rate in question.
        //
        // TODO(revieW): We could do this skip-check *before* we fetch
        // baseUnit/factor/offset based only on the source unit, but only if
        // we're certain we'll never get two different baseUnits for a given
        // source. This should be the case, since convertUnit entries in CLDR's
        // units.xml should all point at a defined base unit for the unit
        // category. I should make this code more efficient after
        // double-checking we're fine with relying on such a detail from the
        // CLDR spec?
        fLastBaseUnit.clear();
        fLastBaseUnit.appendInvariantChars(baseUnit, lenBaseUnit, status);
        if (U_FAILURE(status)) return;
        for (int32_t i = 0, len = outVector.length(); i < len; i++) {
            if (strcmp(outVector[i]->sourceUnit.data(), source) == 0 &&
                strcmp(outVector[i]->baseUnit.data(), fLastBaseUnit.data()) == 0) {
                return;
            }
        }
        if (U_FAILURE(status)) return;

        // We don't have this ConversionRateInfo yet: add it.
        ConversionRateInfo *cr = outVector.emplaceBack();
        if (!cr) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return;
        } else {
            cr->sourceUnit.append(source, lenSource, status);
            cr->baseUnit.append(fLastBaseUnit.data(), fLastBaseUnit.length(), status);
            cr->factor.appendInvariantChars(factor, lenFactor, status);
            if (offset != NULL) cr->offset.appendInvariantChars(offset, lenOffset, status);
        }
    }

    /**
     * Returns the MeasureUnit that was the conversion base unit of the most
     * recent call to put() - typically meaning the most recent call to
     * ures_getAllItemsWithFallback().
     */
    MeasureUnit getLastBaseUnit(UErrorCode &status) {
        return MeasureUnit::forIdentifier(fLastBaseUnit.data(), status);
    }

  private:
    MaybeStackVector<ConversionRateInfo> &outVector;

    // TODO(review): felt like a hack: provides easy access to the most recent
    // baseUnit. This hack is another point making me wonder if doing this
    // ResourceSink thing is worthwhile. Functional style is not more verbose,
    // and IMHO more readable than this object-based approach where the output
    // seems/feels like a side-effect.
    CharString fLastBaseUnit;
};

// The input unit needs to be simple, but can have dimensionality != 1.
void processSingleUnit(const MeasureUnit &unit, const UResourceBundle *convertUnitsBundle,
                       ConversionRateDataSink &convertSink, MeasureUnit *baseSingleUnit,
                       UErrorCode &status) {
    int32_t dimensionality = unit.getDimensionality(status);

    MeasureUnit simple = unit;
    if (dimensionality != 1 || simple.getSIPrefix(status) != UMEASURE_SI_PREFIX_ONE) {
        simple = unit.withDimensionality(1, status).withSIPrefix(UMEASURE_SI_PREFIX_ONE, status);
    }
    ures_getAllItemsWithFallback(convertUnitsBundle, simple.getIdentifier(), convertSink, status);

    if (baseSingleUnit != NULL) {
        MeasureUnit baseUnit = convertSink.getLastBaseUnit(status);

        if (dimensionality == 1) {
            *baseSingleUnit = baseUnit;
        } else if (baseUnit.getComplexity(status) == UMEASURE_UNIT_SINGLE) {
            // TODO(hugovdm): find examples where we're converting a *-per-* to
            // a square-*? Does one ever square frequency? What about
            // squared-speed in the case of mv^2? Or F=ma^2?
            //
            // baseUnit might also have dimensionality, e.g. cubic-meter -
            // retain this instead of overriding with input unit dimensionality:
            dimensionality *= baseUnit.getDimensionality(status);
            *baseSingleUnit = baseUnit.withDimensionality(dimensionality, status);
        } else {
            // We only support higher dimensionality input units if they map to
            // simple base units, such that that base unit can have the
            // dimensionality easily applied.
            //
            // TODO(hugovdm): produce succeeding examples of simple input unit
            // mapped to a different simple target/base unit.
            //
            // TODO(hugovdm): produce failing examples of higher-dimensionality
            // or inverted input units that map to compound output units.
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    }
}

} // namespace

MaybeStackVector<ConversionRateInfo> getConversionRatesInfo(const MeasureUnit source, const MeasureUnit target,
                                                            MeasureUnit *baseCompoundUnit,
                                                            UErrorCode &status) {
    MaybeStackVector<ConversionRateInfo> result;

    int32_t sourceUnitsLength, targetUnitsLength;
    LocalArray<MeasureUnit> sourceUnits = source.splitToSingleUnits(sourceUnitsLength, status);
    LocalArray<MeasureUnit> targetUnits = target.splitToSingleUnits(targetUnitsLength, status);

    LocalUResourceBundlePointer unitsBundle(ures_openDirect(NULL, "units", &status));
    StackUResourceBundle convertUnitsBundle;
    ures_getByKey(unitsBundle.getAlias(), "convertUnits", convertUnitsBundle.getAlias(), &status);

    ConversionRateDataSink convertSink(result);
    MeasureUnit sourceBaseUnit;
    for (int i = 0; i < sourceUnitsLength; i++) {
        MeasureUnit baseUnit;
        processSingleUnit(sourceUnits[i], convertUnitsBundle.getAlias(), convertSink, &baseUnit, status);
        if (source.getComplexity(status) == UMEASURE_UNIT_SEQUENCE) {
            if (i == 0) {
                sourceBaseUnit = baseUnit;
            } else {
                if (baseUnit != sourceBaseUnit) {
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return result;
                }
            }
        } else {
            sourceBaseUnit = sourceBaseUnit.product(baseUnit, status);
        }
    }
    MeasureUnit targetBaseUnit;
    for (int i = 0; i < targetUnitsLength; i++) {
        MeasureUnit baseUnit;
        processSingleUnit(targetUnits[i], convertUnitsBundle.getAlias(), convertSink, &baseUnit, status);
        if (target.getComplexity(status) == UMEASURE_UNIT_SEQUENCE) {
            // WIP/TODO(hugovdm): add consistency checks.
            if (baseUnit != sourceBaseUnit) {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return result;
            }
            targetBaseUnit = baseUnit;
        } else {
            // WIP/FIXME(hugovdm): I think I found a bug in targetBaseUnit.product():
            // Target Base: <kilogram-square-meter-per-square-second> x <one-per-meter> => <meter>
            //
            // fprintf(stderr, "Target Base: <%s> x <%s> => ", targetBaseUnit.getIdentifier(),
            //         baseUnit.getIdentifier());
            targetBaseUnit = targetBaseUnit.product(baseUnit, status);
            // fprintf(stderr, "<%s>\n", targetBaseUnit.getIdentifier());
            // fprintf(stderr, "Status: %s\n", u_errorName(status));
        }
    }
    if (targetBaseUnit != sourceBaseUnit) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return result;
    }
    if (baseCompoundUnit != NULL) { *baseCompoundUnit = sourceBaseUnit; }
    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
