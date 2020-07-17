// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __COMPLEXUNITSCONVERTER_H__
#define __COMPLEXUNITSCONVERTER_H__

#include "cmemory.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unitconverter.h"
#include "unitsdata.h"

U_NAMESPACE_BEGIN

namespace units {

/**
 *  Converts from single or compound unit to single, compound or mixed units.
 * For example, from `meter` to `foot+inch`.
 *
 *  DESIGN:
 *    This class uses `UnitConverter` in order to perform the single converter (i.e. converters from a
 *    single unit to another single unit). Therefore, `ComplexUnitsConverter` class contains multiple
 *    instances of the `UnitConverter` to perform the conversion.
 */
class U_I18N_API ComplexUnitsConverter {
  public:
    /**
     * Constructor of `ComplexUnitsConverter`.
     * NOTE:
     *   - inputUnit and outputUnits must be under the same category
     *      - e.g. meter to feet and inches --> all of them are length units.
     *
     * @param inputUnit represents the source unit. (should be single or compound unit).
     * @param outputUnits represents the output unit. could be any type. (single, compound or mixed).
     * @param status
     */
    ComplexUnitsConverter(const MeasureUnit &inputUnit, const MeasureUnit &outputUnits,
                          const ConversionRates &ratesInfo, UErrorCode &status);

    // Returns true if the specified `quantity` of the `inputUnit`, expressed in terms of the biggest
    // unit in the MeasureUnit `outputUnit`, is greater than or equal to `limit`.
    //    For example, if the input unit is `meter` and the target unit is `foot+inch`. Therefore, this
    //    function will convert the `quantity` from `meter` to `foot`, then, it will compare the value in
    //    `foot` with the `limit`.
    UBool greaterThanOrEqual(double quantity, double limit) const;

    // Returns outputMeasures which is an array with the corresponding values.
    //    - E.g. converting meters to feet and inches.
    //                  1 meter --> 3 feet, 3.3701 inches
    //         NOTE:
    //           the smallest element is the only element that could have fractional values. And all
    //           other elements are floored to the nearest integer
    MaybeStackVector<Measure> convert(double quantity, UErrorCode &status) const;

  private:
    MaybeStackVector<UnitConverter> unitConverters_;
    MaybeStackVector<MeasureUnit> units_;
};

} // namespace units
U_NAMESPACE_END

#endif //__COMPLEXUNITSCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
