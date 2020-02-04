// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITCONVERTER_H__
#define __UNITCONVERTER_H__

#include "number_decnum.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

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

/* Represents a conversion factor */
struct Factor {
    number::impl::DecNum factorNum;
    number::impl::DecNum factorDen;
    int8_t constants[CONSTANTS_COUNT] = {};

    Factor(UErrorCode &status) {
        factorNum.setTo(1.0, status);
        factorDen.setTo(1.0, status);
    }
};

/**
 * Represents the conversion rate between `source` and `destincation`.
 */
struct ConversionRate {
    StringPiece source;
    StringPiece target;
    number::impl::DecNum factorNum;
    number::impl::DecNum factorDen;
    bool reciprocal;

    ConversionRate(UErrorCode &status) {
        factorNum.setTo(1.0, status);
        factorDen.setTo(1.0, status);
    }
};

/**
 * Converts from a source `MeasureUnit` to a target `MeasureUnit`.
 */
class UnitConverter {
  public:
    /**
     * Constructor of `UnitConverter`.
     * NOTE:
     *   - source and target must be under the same category
     *      - e.g. meter to mile --> both of them are length units.
     *
     * @param source represents the source unit.
     * @param target represents the target unit.
     * @param status
     */
    UnitConverter(MeasureUnit source, MeasureUnit target, UErrorCode status);

    /**
     * Convert a value in the source unit to another value in the target unit.
     *
     * @param input_value the value that needs to be converted.
     * @param output_value the value that holds the result of the conversion.
     * @param status
     */
    void convert(const number::impl::DecNum &input_value, number::impl::DecNum &output_value,
                 UErrorCode status);

  private:
    ConversionRate conversion_rate_;
};

U_NAMESPACE_END

#endif //__UNITCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
