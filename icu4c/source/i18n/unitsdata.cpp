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
        if (U_FAILURE(status)) return;
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
        if (U_FAILURE(status)) return;
        if (baseUnit == NULL || factor == NULL) {
            // We could not find a usable conversion rate.
            status = U_MISSING_RESOURCE_ERROR;
            return;
        }

        // Check if we already have the conversion rate in question.
        //
        // TODO(review): We could do this skip-check *before* we fetch
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

/**
 * Collects conversion information for a "single unit" (a unit whose complexity
 * is UMEASURE_UNIT_SINGLE).
 *
 * This function currently only supports higher-dimensionality input units if
 * they map to "single unit" output units. This means it don't support
 * square-bar, one-per-bar, square-joule or one-per-joule. (Some unit types in
 * this class: volume, consumption, torque, force, pressure, speed,
 * acceleration, and more).
 *
 * TODO(hugovdm): maybe find and share (in documentation) a solid argument for
 * why these kinds of input units won't be needed with higher dimensionality? Or
 * start supporting them... Also: add unit tests demonstrating the
 * U_ILLEGAL_ARGUMENT_ERROR returned for such units.
 *
 * @param unit The input unit. Its complexity must be UMEASURE_UNIT_SINGLE, but
 * it may have a dimensionality != 1.
 * @param converUnitsBundle A UResourceBundle instance for the convertUnits
 * resource.
 * @param convertSink The ConversionRateDataSink through which
 * ConversionRateInfo instances are to be collected.
 * @param baseSingleUnit Output parameter: if not NULL, the base unit through
 * which conversion rates pivot to other similar units will be returned through
 * this pointer.
 * @param status The standard ICU error code output parameter.
 */
void processSingleUnit(const MeasureUnit &unit, const UResourceBundle *convertUnitsBundle,
                       ConversionRateDataSink &convertSink, MeasureUnit *baseSingleUnit,
                       UErrorCode &status) {
    if (U_FAILURE(status)) return;
    int32_t dimensionality = unit.getDimensionality(status);

    // Fetch the relevant entry in convertUnits.
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
            // The baseUnit is a single unit, so can be raised to the
            // dimensionality of the input unit.
            dimensionality *= baseUnit.getDimensionality(status);
            *baseSingleUnit = baseUnit.withDimensionality(dimensionality, status);
        } else {
            // We only support higher dimensionality input units if they map to
            // simple base units, such that that base unit can have the
            // dimensionality easily applied.
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    }
}

} // namespace

MaybeStackVector<ConversionRateInfo> getConversionRatesInfo(const MeasureUnit source,
                                                            const MeasureUnit target,
                                                            MeasureUnit *baseUnit,
                                                            UErrorCode &status) {
    MaybeStackVector<ConversionRateInfo> result;
    if (U_FAILURE(status)) return result;

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
            // WIP/FIXME(hugovdm): ensure this gets fixed, then remove this
            // comment: I think I found a bug in targetBaseUnit.product(). First
            // symptom was an unexpected product, further exploration resulted
            // in AddressSanitizer errors.
            //
            // The product was:
            //
            // <kilogram-square-meter-per-square-second> * <one-per-meter> => <meter>
            //
            // as output by a printf:
            //
            // fprintf(stderr, "<%s> x <%s> => ",
            //         targetBaseUnit.getIdentifier(),
            //         baseUnit.getIdentifier());
            targetBaseUnit = targetBaseUnit.product(baseUnit, status);
            // fprintf(stderr, "<%s> - Status: %s\n",
            //         targetBaseUnit.getIdentifier(), u_errorName(status));
        }
    }
    if (targetBaseUnit != sourceBaseUnit) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return result;
    }
    if (baseUnit != NULL) { *baseUnit = sourceBaseUnit; }
    return result;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
