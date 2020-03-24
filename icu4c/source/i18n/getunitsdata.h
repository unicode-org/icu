// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __GETUNITSDATA_H__
#define __GETUNITSDATA_H__

#include "charstr.h" // CharString
#include "cmemory.h"
#include "complexunitsconverter.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN

struct ConversionRateInfo {
    CharString source;
    CharString target;
    CharString factor;
    CharString offset;
};

struct UnitPreference {
    UnitPreference() : geq(1) {}
    CharString unit;
    double geq;
    CharString skeleton;
};

// TODO(hugo): Add a comment.
void U_I18N_API getUnitsData(const char *outputRegion, const char *usage, const MeasureUnit &inputUnit,
                             CharString &category, MeasureUnit &baseUnit,
                             MaybeStackVector<ConversionRateInfo> &conversionInfo,
                             MaybeStackVector<UnitPreference> &unitPreferences, UErrorCode &status);

U_NAMESPACE_END

#endif //__GETUNITSDATA_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
