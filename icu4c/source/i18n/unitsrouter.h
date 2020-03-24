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

U_NAMESPACE_END

#endif //__UNITSROUTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
