// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITCONVERTER_H__
#define __UNITCONVERTER_H__

#include "cmemory.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unitconverter.h"
#include "unitsdata.h"

U_NAMESPACE_BEGIN

enum U_I18N_API UnitsConvertibilityState {
    RECIPROCAL,
    CONVERTIBLE,
    UNCONVERTIBLE,
};

UnitsConvertibilityState U_I18N_API checkConvertibility(const MeasureUnit &source,
                                                        const MeasureUnit &target,
                                                        const ConversionRates &conversionRates,
                                                        UErrorCode &status);

U_NAMESPACE_END

#endif //__UNITCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
