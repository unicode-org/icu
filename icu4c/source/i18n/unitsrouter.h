// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITSROUTER_H__
#define __UNITSROUTER_H__

#include "charstr.h" // CharString
#include "cmemory.h"
#include "complexunitsconverter.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN

using icu::CharString;
using icu::MaybeStackVector;
using icu::MeasureUnit;

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

struct ConverterPreference {
    ComplexUnitsConverter converter;
    double limit;

    ConverterPreference(MeasureUnit source, MeasureUnit complexTarget, double limit, UErrorCode &status)
        : converter(source, complexTarget, status), limit(limit) {}
};

class U_I18N_API UnitsRouter {
  public:
    UnitsRouter(MeasureUnit inputUnit, StringPiece locale, StringPiece usage, UErrorCode &status);

    MaybeStackVector<Measure> route(double quantity, UErrorCode &status);

  private:
    MaybeStackVector<ConverterPreference> converterPreferences_;
};

// TODO(hugo): Add a comment.
void U_I18N_API getUnitsData(const char *outputRegion, const char *usage, const MeasureUnit &inputUnit,
                CharString &category, MeasureUnit &baseUnit,
                MaybeStackVector<ConversionRateInfo> &conversionInfo,
                MaybeStackVector<UnitPreference> &unitPreferences, UErrorCode &status);

U_NAMESPACE_END

#endif //__UNITSROUTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
