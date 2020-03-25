// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __GETUNITSDATA_H__
#define __GETUNITSDATA_H__

#include "charstr.h" // CharString
#include "cmemory.h"
#include "unicode/errorcode.h"
#include "unicode/measunit.h"
#include "unicode/measure.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN

// Encapsulates "convertUnits" information from units resources, specifying how
// to convert from one unit to another.
class U_I18N_API ConversionRateInfo {
  public:
    ConversionRateInfo() {};
    ConversionRateInfo(StringPiece source, StringPiece target, StringPiece factor, StringPiece offset,
                       UErrorCode &status)
        : source(), target(), factor(), offset() {
        this->source.append(source, status);
        this->target.append(target, status);
        this->factor.append(factor, status);
        this->offset.append(offset, status);
    };
    CharString source;
    CharString target; // FIXME/WIP: baseUnit
    CharString factor;
    CharString offset;
    bool reciprocal = false;
};

// Encapsulates unitPreferenceData information from units resources, specifying
// a sequence of output unit preferences.
struct U_I18N_API UnitPreference {
    UnitPreference() : geq(1) {}
    CharString unit;
    double geq;
    CharString skeleton;
};

/**
 * Collects and returns ConversionRateInfo needed to convert from source to
 * target.
 * 
 * @param source The source unit (the unit type converted from).
 * @param target The target unit (the unit type converted to).
 * @param baseCompoundUnit Output parameter: if not NULL, it will be set to the
 * base unit type used as pivot for converting from source to target.
 * @param status Receives status.
 */
MaybeStackVector<ConversionRateInfo> U_I18N_API getConversionRatesInfo(MeasureUnit source,
                                                                       MeasureUnit target,
                                                                       MeasureUnit *baseCompoundUnit,
                                                                       UErrorCode &status);

/**
 * Collects the data needed for converting the inputUnit type to output units
 * for the given region and usage.
 *
 * WARNING: This function only supports simple units presently.
 * WIP/TODO(hugovdm): add support for UMEASURE_UNIT_SEQUENCE and
 * UMEASURE_UNIT_COMPOUND complexities.
 *
 * @param outputRegion The region code for which output preferences are desired
 * (e.g. US or 001).
 * @param usage The "usage" parameter, such as "person", "road", or "fluid".
 * Unrecognised usages are treated as "default".
 * @param inputUnit The input unit type: the type of the units that will be
 * converted.
 * @param category Output parameter, this will be set to the category associated
 * with the inputUnit. TODO(hugovdm): this might get specified instead of
 * requested. Or it may be unnecessary to return it.
 * @param baseUnit Output parameter, this will be set to the base unit through
 * which conversions are made (e.g. "kilogram", "meter", or "year").
 * TODO(hugovdm): find out if this is needed/useful.
 * @param conversionInfo Output parameter, a vector of ConversionRateInfo
 * instances needed to be able to convert from inputUnit to all the output units
 * found in unitPreferences.
 * @param unitPreferences Output parameter, a vector of all the output
 * preferences for the given region, usage, and input unit type (which
 * determines the category).
 * @param status Receives status.
 */
void U_I18N_API getUnitsData(const char *outputRegion, const char *usage, const MeasureUnit &inputUnit,
                             CharString &category, MeasureUnit &baseUnit,
                             MaybeStackVector<ConversionRateInfo> &conversionInfo,
                             MaybeStackVector<UnitPreference> &unitPreferences, UErrorCode &status);

// // TODO(hugo): Implement
// // Compound units as source and target, conversion rates for each piece.
// MaybeStackVector<ConversionRateInfo>
//     U_I18N_API getConversionRatesInfo(MeasureUnit source, MeasureUnit target, UErrorCode &status);

U_NAMESPACE_END

#endif //__GETUNITSDATA_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
