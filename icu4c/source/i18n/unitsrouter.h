// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITSROUTER_H__
#define __UNITSROUTER_H__

#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN

class UnitConverter;

class U_I18N_API UnitsRouter {
  public:
    UnitsRouter(MeasureUnit inputUnit, Locale locale, StringPiece usage);

    LocalArray<Measure> route(double quantity, UErrorCode &status);

  private:
    LocalArray<UnitConverter> unitsConverters;
};

U_NAMESPACE_END

#endif //__UNITSROUTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
