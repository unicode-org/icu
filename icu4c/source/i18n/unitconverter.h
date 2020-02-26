// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __UNITCONVERTER_H__
#define __UNITCONVERTER_H__

#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN
// Data Skeleton.

enum Constants {
    CONSTANT_FT2M, // ft2m stands for foot to meter.
    CONSTANT_PI,
    CONSTANT_GRAVITY,
    CONSTANT_G,
    CONSTANT_CUP2M3, // CUP2M3 stands for cup to cubic meter.
    CONSTANT_LB2KG,

    // Must be the last element.
    CONSTANTS_COUNT
};

/**
 * Represents the conversion rate between `source` and `destincation`.
 */
struct ConversionRate {
    StringPiece source;
    StringPiece target;
    double factorNum = 1;
    double factorDen = 1;
    double sourceOffset = 0;
    double targetOffset = 0;
    bool reciprocal;
};

// The data in this namespace are temporary, it is just for testing
namespace temporarily {

struct entry {
    StringPiece source;
    StringPiece target;
    StringPiece factor;
    StringPiece offset;
    bool reciprocal;
} dataEntries[] = {
    // Base Units
    {"kilogram", "kilogram", "1", "0", false},
    {"candela", "candela", "1", "0", false},
    {"meter", "meter", "1", "0", false},
    {"second", "second", "1", "0", false},
    {"year", "year", "1", "0", false},
    {"ampere", "ampere", "1", "0", false},
    {"kelvin", "kelvin", "1", "0", false},
    {"revolution", "revolution", "1", "0", false},
    {"item", "item", "1", "0", false},
    {"portion", "portion", "1", "0", false},
    {"bit", "bit", "1", "0", false},
    {"pixel", "pixel", "1", "0", false},
    {"em", "em", "1", "0", false},

    // Unit conversions Rates
    {"atmosphere", "kilogram-per-meter-square-second", "101325", "0", false},
    {"byte", "bit", "8", "0", false},
    {"day", "second", "86400", "0", false},
    {"day-person", "second", "86400", "0", false},
    {"hour", "second", "3600", "0", false},
    {"minute", "second", "60", "0", false},
    {"week", "second", "604800", "0", false},
    {"week-person", "second", "604800", "0", false},
    {"ohm", "kilogram-square-meter-per-cubic-second-square-ampere", "1", "0", false},
    {"volt", "kilogram-square-meter-per-cubic-second-ampere", "1", "0", false},
    {"light-year", "meter", "9460730000000000", "0", false},
    {"parsec", "meter", "30856780000000000", "0", false},
    {"g-force", "meter-per-square-second", "gravity", "0", false},
    {"degree", "revolution", "1/360", "0", false},
    {"arc-minute", "revolution", "1/360*60", "0", false},
    {"arc-second", "revolution", "1/360*60*60", "0", false},
    {"radian", "revolution", "1/2*PI", "0", false},
    {"mole", "item", "602214076000000000000000", "0", false},
    {"percent", "portion", "1/100", "0", false},
    {"permille", "portion", "1/1000", "0", false},
    {"permyriad", "portion", "1/10000", "0", false},
    {"calorie", "kilogram-square-meter-per-square-second", "4.184", "0", false},
    {"electronvolt", "kilogram-square-meter-per-square-second", "0.0000000000000000001602177", "0",
     false},
    {"foodcalorie", "kilogram-square-meter-per-square-second", "4184", "0", false},
    {"hertz", "revolution-per-second", "1", "0", false},
    {"astronomical-unit", "meter", "149597900000", "0", false},
    {"acre", "square-meter", "ft2m^2*43560", "0", false},
    {"therm-us", "kilogram-square-meter-per-square-second", "105506000", "0", false},
    {"pound-force", "kilogram-meter-per-square-second", "lb2kg*gravity", "0", false},
    {"fathom", "meter", "ft2m*6", "0", false},
    {"foot", "meter", "ft2m", "0", false},
    {"furlong", "meter", "ft2m*660", "0", false},
    {"inch", "meter", "ft2m/12", "0", false},
    {"mile", "meter", "ft2m*5280", "0", false},
    {"nautical-mile", "meter", "1852", "0", false},
    {"yard", "meter", "ft2m*3", "0", false},
    {"100-kilometer", "meter", "100000", "0", false},
    {"ounce", "kilogram", "lb2kg/16", "0", false},
    {"ounce-troy", "kilogram", "0.03110348", "0", false},
    {"pound", "kilogram", "lb2kg", "0", false},
    {"stone", "kilogram", "lb2kg*14", "0", false},
    {"ton", "kilogram", "lb2kg*2000", "0", false},
    {"horsepower", "kilogram-square-meter-per-cubic-second", "ft2m*lb2kg*gravity*550", "0", false},
    {"inch-hg", "kilogram-per-meter-square-second", "3386.389", "0", false},
    {"knot", "meter-per-second", "1852/3600", "0", false},
    {"fahrenheit", "kelvin", "5/9", "255.372222222", false}, // 2298.35/9
    {"barrel", "cubic-meter", "cup2m3*672", "0", false},
    {"bushel", "cubic-meter", "0.03523907", "0", false},
    {"cup", "cubic-meter", "cup2m3", "0", false},
    {"fluid-ounce", "cubic-meter", "cup2m3/8", "0", false},
    {"gallon", "cubic-meter", "cup2m3*16", "0", false},
    {"tablespoon", "cubic-meter", "cup2m3/16", "0", false},
    {"teaspoon", "cubic-meter", "cup2m3/48", "0", false},
    {"karat", "portion", "1/24", "0", false},
    {"pint", "cubic-meter", "cup2m3*2", "0", false},
    {"quart", "cubic-meter", "cup2m3*4", "0", false},
    {"fluid-ounce-imperial", "cubic-meter", "0.00002841306", "0", false},
    {"gallon-imperial", "cubic-meter", "0.00454609", "0", false},
    {"dunam", "square-meter", "1000", "0", false},
    {"mile-scandinavian", "meter", "10000", "0", false},
    {"hectare", "square-meter", "10000", "0", false},
    {"joule", "kilogram-square-meter-per-square-second", "1", "0", false},
    {"newton", "kilogram-meter-per-square-second", "1", "0", false},
    {"carat", "kilogram", "0.0002", "0", false},
    {"gram", "kilogram", "0.001", "0", false},
    {"metric-ton", "kilogram", "1000", "0", false},
    {"watt", "kilogram-square-meter-per-cubic-second", "1", "0", false},
    {"bar", "kilogram-per-meter-square-second", "100000", "0", false},
    {"pascal", "kilogram-per-meter-square-second", "1", "0", false},
    {"celsius", "kelvin", "1", "273.15", false},
    {"cup-metric", "cubic-meter", "0.00025", "0", false},
    {"liter", "cubic-meter", "0.001", "0", false},
    {"pint-metric", "cubic-meter", "0.0005", "0", false},
    {"centimeter", "meter", "1/100", "0", false},
    {"century", "year", "100", "0", false},
    {"decade", "year", "10", "0", false},
    {"dot", "pixel", "1", "0", false},
    {"month", "year", "1/12", "0", false},
    {"month-person", "year", "1/12", "0", false},
    {"solar-luminosity", "kilogram-square-meter-per-cubic-second", "382800000000000000000000000", "0",
     false},
    {"solar-radius", "meter", "695700000", "0",
     false}, // 132712440000000000000 TODO(younies): fill bug about StringPiece handling this string ??
    {"earth-radius", "meter", "6378100", "0", false},
    {"solar-mass", "kilogram", "1.98847E+30", "0", false},
    {"earth-mass", "kilogram", "5972200000000000000000000", "0", false},
    {"year-person", "year", "1", "0", false},
    {"part-per-million", "portion", "1/1000000", "0", false},
    {"millimeter-of-mercury", "kilogram-per-meter-square-second", "10132500/760000", "0", false},
    {"british-thermal-unit", "kilogram-square-meter-per-square-second", "1055.06", "0", false},
    {"point", "meter", "ft2m/864", "0", false},
    {"dalton", "kilogram-square-meter-per-square-second", "0.000000000149241808560", "0", false},
    {"lux", "candela-square-meter-per-square-meter", "1", "0", false},
};

} // namespace temporarily

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
    UnitConverter(MeasureUnit source, MeasureUnit target, UErrorCode status);

    /**
     * Convert a value in the source unit to another value in the target unit.
     *
     * @param input_value the value that needs to be converted.
     * @param output_value the value that holds the result of the conversion.
     * @param status
     */
    double convert(double inputValue, UErrorCode status);

  private:
    ConversionRate conversionRate_;
};

U_NAMESPACE_END

#endif //__UNITCONVERTER_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
