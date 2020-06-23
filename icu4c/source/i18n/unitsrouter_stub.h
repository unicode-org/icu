// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// TODO(units): delete this file! Use the proper UnitsRouter instead. Since this
// is a temporary stubbed out version, I've not given it its own .cpp file: the
// actual code is tresspassing in number_fluent.cpp.

// #include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITSROUTER_H__
#define __UNITSROUTER_H__

#include <limits>

#include "charstr.h" // CharString
#include "cmemory.h"
// #include "complexunitsconverter.h"
#include "unicode/errorcode.h"
#include "unicode/locid.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"
#include "unitsdata.h"

U_NAMESPACE_BEGIN

class U_I18N_API StubUnitsRouter {
  public:
    // As found in UnitsRouter
    StubUnitsRouter(MeasureUnit inputUnit, StringPiece region, StringPiece usage,
                    UErrorCode &status);

    // TODO(units): consider this possible improvement for the constructor:
    // passing "Locale" instead of "StringPiece region"? :
    StubUnitsRouter(MeasureUnit inputUnit, Locale locale, StringPiece usage, UErrorCode &status);

    // TODO(units): API under reconsideration: going via Measure may be
    // excessive when we need a double; MaybeStackVector<Measure> might also not
    // be what we want for dealing with Mixed, given that a MeasureUnit can, on
    // its own, represent mixed units. (i.e. do we want to have Measure support
    // multi-valued mixed-units?)
    MaybeStackVector<Measure> route(double quantity, UErrorCode &status) const;

    MaybeStackVector<MeasureUnit> getOutputUnits() const;

  private:
    // Stubbed out:
    // MaybeStackVector<ConverterPreference> converterPreferences_;

    // Locale fStubLocale;
    CharString fRegion;
};

U_NAMESPACE_END

#endif //__UNITSROUTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
