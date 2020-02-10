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
    number::impl::DecNum factorNum;
    number::impl::DecNum factorDen;
    number::impl::DecNum offset;
    bool reciprocal;

    ConversionRate(UErrorCode &status) {
        factorNum.setTo(1.0, status);
        factorDen.setTo(1.0, status);
        offset.setTo(0.0, status);
        reciprocal = false;
    }
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
    {"kilogram", "kilogram", "", "", false},
    {"candela", "candela", "", "", false},
    {"meter", "meter", "", "", false},
    {"second", "second", "", "", false},
    {"year", "year", "", "", false},
    {"ampere", "ampere", "", "", false},
    {"kelvin", "kelvin", "", "", false},
    {"revolution", "revolution", "", "", false},
    {"item", "item", "", "", false},
    {"portion", "portion", "", "", false},
    {"bit", "bit", "", "", false},
    {"pixel", "pixel", "", "", false},
    {"em", "em", "", "", false},

    // Unit conversions Rates
    {"atmosphere", "kilogram-per-meter-square-second", "101325", "", false},
    {"byte", "bit", "8", "", false},
    {"day", "second", "86400", "", false},
    {"day-person", "second", "86400", "", false},
    {"hour", "second", "3600", "", false},
    {"minute", "second", "60", "", false},
    {"week", "second", "604800", "", false},
    {"week-person", "second", "604800", "", false},
    {"ohm", "kilogram-square-meter-per-cubic-second-square-ampere", "1", "", false},
    {"volt", "kilogram-square-meter-per-cubic-second-ampere", "1", "", false},
    {"light-year", "meter", "9460730000000000", "", false},
    {"parsec", "meter", "30856780000000000", "", false},
    {"g-force", "meter-per-square-second", "gravity", "", false},
    {"degree", "revolution", "1/360", "", false},
    {"arc-minute", "revolution", "1/360*60", "", false},
    {"arc-second", "revolution", "1/360*60*60", "", false},
    {"radian", "revolution", "1/2*PI", "", false},
    {"mole", "item", "602214076000000000000000", "", false},
    {"percent", "portion", "1/100", "", false},
    {"permille", "portion", "1/1000", "", false},
    {"permyriad", "portion", "1/10000", "", false},
    {"calorie", "kilogram-square-meter-per-square-second", "4.184", "", false},
    {"electronvolt", "kilogram-square-meter-per-square-second", "0.0000000000000000001602177", "",
     false},
    {"foodcalorie", "kilogram-square-meter-per-square-second", "4184", "", false},
    {"hertz", "revolution-per-second", "1", "", false},
    {"astronomical-unit", "meter", "149597900000", "", false},
    {"acre", "square-meter", "ft2m^2*43560", "", false},
    {"therm-us", "kilogram-square-meter-per-square-second", "105506000", "", false},
    {"pound-force", "kilogram-meter-per-square-second", "lb2kg*gravity", "", false},
    {"fathom", "meter", "ft2m*6", "", false},
    {"foot", "meter", "ft2m", "", false},
    {"furlong", "meter", "ft2m*660", "", false},
    {"inch", "meter", "ft2m/12", "", false},
    {"mile", "meter", "ft2m*5280", "", false},
    {"nautical-mile", "meter", "1852", "", false},
    {"yard", "meter", "ft2m*3", "", false},
    {"100-kilometer", "meter", "100000", "", false},
    {"ounce", "kilogram", "lb2kg/16", "", false},
    {"ounce-troy", "kilogram", "0.03110348", "", false},
    {"pound", "kilogram", "lb2kg", "", false},
    {"stone", "kilogram", "lb2kg*14", "", false},
    {"ton", "kilogram", "lb2kg*2000", "", false},
    {"horsepower", "kilogram-square-meter-per-cubic-second", "ft2m*lb2kg*gravity*550", "", false},
    {"inch-hg", "kilogram-per-meter-square-second", "3386.389", "", false},
    {"knot", "meter-per-second", "1852/3600", "", false},
    {"fahrenheit", "kelvin", "5/9", "2298.35/9", false},
    {"barrel", "cubic-meter", "cup2m3*672", "", false},
    {"bushel", "cubic-meter", "0.03523907", "", false},
    {"cup", "cubic-meter", "cup2m3", "", false},
    {"fluid-ounce", "cubic-meter", "cup2m3/8", "", false},
    {"gallon", "cubic-meter", "cup2m3*16", "", false},
    {"tablespoon", "cubic-meter", "cup2m3/16", "", false},
    {"teaspoon", "cubic-meter", "cup2m3/48", "", false},
    {"karat", "portion", "1/24", "", false},
    {"pint", "cubic-meter", "cup2m3*2", "", false},
    {"quart", "cubic-meter", "cup2m3*4", "", false},
    {"fluid-ounce-imperial", "cubic-meter", "0.00002841306", "", false},
    {"gallon-imperial", "cubic-meter", "0.00454609", "", false},
    {"dunam", "square-meter", "1000", "", false},
    {"mile-scandinavian", "meter", "10000", "", false},
    {"hectare", "square-meter", "10000", "", false},
    {"joule", "kilogram-square-meter-per-square-second", "1", "", false},
    {"newton", "kilogram-meter-per-square-second", "1", "", false},
    {"carat", "kilogram", "0.0002", "", false},
    {"gram", "kilogram", "0.001", "", false},
    {"metric-ton", "kilogram", "1000", "", false},
    {"watt", "kilogram-square-meter-per-cubic-second", "1", "", false},
    {"bar", "kilogram-per-meter-square-second", "100000", "", false},
    {"pascal", "kilogram-per-meter-square-second", "1", "", false},
    {"celsius", "kelvin", "1", "-273.15", false},
    {"cup-metric", "cubic-meter", "0.00025", "", false},
    {"liter", "cubic-meter", "0.001", "", false},
    {"pint-metric", "cubic-meter", "0.0005", "", false},
    {"centimeter", "meter", "1/100", "", false},
    {"century", "year", "100", "", false},
    {"decade", "year", "10", "", false},
    {"dot", "pixel", "100", "", false},
    {"month", "year", "12", "", false},
    {"month-person", "year", "12", "", false},
    {"solar-luminosity", "kilogram-square-meter-per-cubic-second", "382800000000000000000000000", "",
     false},
    {"solar-radius", "meter", "132712440000000000000/G", "", false},
    {"earth-radius", "meter", "6378100", "", false},
    {"solar-mass", "kilogram", "19884700000000000000000000000000", "", false},
    {"earth-mass", "kilogram", "5972200000000000000000000", "", false},
    {"year-person", "year", "1", "", false},
    {"part-per-million", "portion", "1/1000000", "", false},
    {"millimeter-of-mercury", "kilogram-per-meter-square-second", "10132500/760000", "", false},
    {"british-thermal-unit", "kilogram-square-meter-per-square-second", "1055.06", "", false},
    {"point", "meter", "ft2m/864", "", false},
    {"dalton", "kilogram-square-meter-per-square-second", "0.000000000149241808560", "", false},
    {"lux", "candela-square-meter-per-square-meter", "1", "", false},
};

} // namespace temporarily



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
