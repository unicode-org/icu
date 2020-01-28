// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITCONVERTER_H__
#define __UNITCONVERTER_H__

#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"

U_NAMESPACE_BEGIN

// Data Skeleton.
enum Constants {
    CONSTANT_FT2M, // ft2m stands for foot to meter.
    CONSTANT_PI,
    CONSTANT_G,      // G stands for Gravity.
    CONSTANT_CUP2M3, // CUP2M3 stands for cup to cubic meter.

    // Must be the last element.
    CONSTANTS_COUNT
};

struct Factor {
    int64_t factorNum;
    int64_t factorDen;
    int8_t constants[CONSTANTS_COUNT] = {};
};

struct ConversionRate {
    UnicodeString source;
    UnicodeString target;
    Factor factor;
    bool reciprocal;
};

/**
 * Converts from a source `MeasureUnit` to a target `MeasureUnit`.
 */
class UnitConverter {
  public:
    /*
     * Constructor for `UnitConverter`.
     * NOTE:
     *   - source and target must be under the same category
     *      - e.g. meter to mile --> both of them are length units.
     */
    UnitConverter(MeasureUnit source, MeasureUnit target, UErrorCode status);

    icu::Measure convert(icu::Measure quantity, UErrorCode status);

  private:
    ConversionRate conversion_rate_;
};

U_NAMESPACE_END

#endif //__UNITCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
