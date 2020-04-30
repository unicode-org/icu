// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITCONVERTER_H__
#define __UNITCONVERTER_H__

#include "unicode/measunit.h"
#include "unicode/stringpiece.h"
#include "unicode/uobject.h"
#include "unitsdata.h"

U_NAMESPACE_BEGIN
namespace units {

/* Internal Structure */

enum Constants {
    CONSTANT_FT2M,    // ft2m stands for foot to meter.
    CONSTANT_PI,      // PI
    CONSTANT_GRAVITY, // Gravity
    CONSTANT_G,
    CONSTANT_GAL_IMP2M3, // Gallon imp to m3
    CONSTANT_LB2KG,      // Pound to Kilogram

    // Must be the last element.
    CONSTANTS_COUNT
};

typedef enum Signum {
    NEGATIVE = -1,
    POSITIVE = 1,
} Signum;

/* Represents a conversion factor */
struct U_I18N_API Factor {
    double factorNum = 1;
    double factorDen = 1;
    double offset = 0;
    bool reciprocal = false;
    int32_t constants[CONSTANTS_COUNT] = {};

    void multiplyBy(const Factor &rhs);
    void divideBy(const Factor &rhs);

    // Apply the power to the factor.
    void power(int32_t power);

    // Flip the `Factor`, for example, factor= 2/3, flippedFactor = 3/2
    void flip();

    // Apply SI prefix to the `Factor`
    void applySiPrefix(UMeasureSIPrefix siPrefix);
    void substituteConstants();
};

/*
 * Adds a single factor element to the `Factor`. e.g "ft3m", "2.333" or "cup2m3". But not "cup2m3^3".
 */
void U_I18N_API addSingleFactorConstant(StringPiece baseStr, int32_t power, Signum sigNum,
                                        Factor &factor, UErrorCode &status);

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

enum U_I18N_API UnitsConvertibilityState {
    RECIPROCAL,
    CONVERTIBLE,
    UNCONVERTIBLE,
};

MeasureUnit U_I18N_API extractCompoundBaseUnit(const MeasureUnit &source,
                                               const ConversionRates &conversionRates,
                                               UErrorCode &status);

/**
 * Check if the convertibility between `source` and `target`.
 * For example:
 *    `meter` and `foot` are `CONVERTIBLE`.
 *    `meter-per-second` and `second-per-meter` are `RECIPROCAL`.
 *    `meter` and `pound` are `UNCONVERTIBLE`.
 *
 * NOTE:
 *    Only works with SINGLE and COMPOUND units. If one of the units is a
 *    MIXED unit, an error will occur. For more information, see UMeasureUnitComplexity.
 */
UnitsConvertibilityState U_I18N_API checkConvertibility(const MeasureUnit &source,
                                                        const MeasureUnit &target,
                                                        const ConversionRates &conversionRates,
                                                        UErrorCode &status);

/**
 * Converts from a source `MeasureUnit` to a target `MeasureUnit`.
 *
 * NOTE:
 *    Only works with SINGLE and COMPOUND units. If one of the units is a
 *    MIXED unit, an error will occur. For more information, see UMeasureUnitComplexity.
 */
class U_I18N_API UnitConverter : public UMemory {
  public:
    /**
     * Constructor of `UnitConverter`.
     * NOTE:
     *   - source and target must be under the same category
     *      - e.g. meter to mile --> both of them are length units.
     *
     * @param source represents the source unit.
     * @param target represents the target unit.
     * @param ratesInfo Contains all the needed conversion rates.
     * @param status
     */
    UnitConverter(MeasureUnit source, MeasureUnit target, const ConversionRates &ratesInfo,
                  UErrorCode &status);

    /**
     * Convert a value in the source unit to another value in the target unit.
     *
     * @param input_value the value that needs to be converted.
     * @param output_value the value that holds the result of the conversion.
     * @param status
     */
    double convert(double inputValue) const;

  private:
    ConversionRate conversionRate_;
};

} // namespace units
U_NAMESPACE_END

#endif //__UNITCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
