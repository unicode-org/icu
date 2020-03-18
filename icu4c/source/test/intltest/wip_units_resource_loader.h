#ifndef WIP_UNITS_RESOURCE_LOADER_H
#define WIP_UNITS_RESOURCE_LOADER_H

#include "charstr.h" // CharString
#include "unicode/measunit.h" // MeasureUnit

using icu::CharString;
using icu::MaybeStackVector;
using icu::MeasureUnit;

struct ConversionRateInfo {
    CharString source;
    CharString target;
    CharString factor;
    CharString offset;

    const UChar *factorUChar;
    const UChar *offsetUChar;
    // WIP: This is a UChar* so that it can point at the resource. We could
    // convert it to a CharString and own it ourselves, or if we can trust
    // another owner's lifetime management we can make it a char*.
    const UChar *targetUChar;

    bool reciprocal = false;
};

struct UnitPreference {
    UnitPreference() : geq(0) {}
    CharString unit;
    double geq;
    CharString skeleton;
};

void getUnitsData(const char *outputRegion, const char *usage, const MeasureUnit &inputUnit,
                  CharString &category, MeasureUnit &baseUnit,
                  MaybeStackVector<ConversionRateInfo> &conversionInfo,
                  MaybeStackVector<UnitPreference> &unitPreferences, UErrorCode &status);

#endif // WIP_UNITS_RESOURCE_LOADER_H
