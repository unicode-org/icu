// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITCONVERTER_H__
#define __UNITCONVERTER_H__

#include "cmemory.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN
// Data Skeleton.

// Represents a raw convert unit.
struct ConvertUnit {
    StringPiece source;
    StringPiece target;

    // NOTE
    //  If the source and target are equal, the factor will be "1"
    StringPiece factor = "1";

    // NOTE
    //  If the offset is not exist, its value will be "0"
    StringPiece offset = "0";

    // NOTE
    //  If reciprocal is not exits, its value will be `false`
    bool reciprocal = false;
};

/**
 * Represents the conversion rate between `source` and `target`.
 */
struct ConversionRate {
    MeasureUnit source;
    MeasureUnit target;
    double factorNum = 1;
    double factorDen = 1;
    double sourceOffset = 0;
    double targetOffset = 0;
    bool reciprocal = false;
};

/**
 * Converts from a source `MeasureUnit` to a target `MeasureUnit`.
 */
class U_I18N_API UnitConverter {
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
    UnitConverter(MeasureUnit source, MeasureUnit target, const MaybeStackVector<ConvertUnit>& convertUnits,
                  UErrorCode &status);

    /**
     * Convert a value in the source unit to another value in the target unit.
     *
     * @param input_value the value that needs to be converted.
     * @param output_value the value that holds the result of the conversion.
     * @param status
     */
    double convert(double inputValue);

  private:
    ConversionRate conversionRate_;
};

U_NAMESPACE_END

#endif //__UNITCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
