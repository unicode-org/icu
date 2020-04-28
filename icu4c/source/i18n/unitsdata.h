// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __GETUNITSDATA_H__
#define __GETUNITSDATA_H__

#include "charstr.h"
#include "cmemory.h"
#include "unicode/measunit.h"
#include "unicode/stringpiece.h"

U_NAMESPACE_BEGIN

/**
 * Encapsulates "convertUnits" information from units resources, specifying how
 * to convert from one unit to another.
 *
 * Information in this class is still in the form of strings: symbolic constants
 * need to be interpreted. Rationale: symbols can cancel out for higher
 * precision conversion - going from feet to inches should cancel out the
 * `ft_to_m` constant.
 */
class U_I18N_API ConversionRateInfo : public UMemory {
  public:
    ConversionRateInfo(){};
    ConversionRateInfo(StringPiece sourceUnit, StringPiece baseUnit, StringPiece factor,
                       StringPiece offset, UErrorCode &status)
        : sourceUnit(), baseUnit(), factor(), offset() {
        this->sourceUnit.append(sourceUnit, status);
        this->baseUnit.append(baseUnit, status);
        this->factor.append(factor, status);
        this->offset.append(offset, status);
    };
    CharString sourceUnit;
    CharString baseUnit;
    CharString factor;
    CharString offset;
};

/**
 * Returns ConversionRateInfo for all supported conversions.
 *
 * @param result Receives the set of conversion rates.
 * @param status Receives status.
 */
void U_I18N_API getAllConversionRates(MaybeStackVector<ConversionRateInfo> &result, UErrorCode &status);

/**
 * Temporary backward-compatibility function.
 *
 * TODO(hugovdm): ensure this gets removed. Currently
 * https://github.com/sffc/icu/pull/32 is making use of it.
 *
 * @param units Ignored.
 * @return the result of getAllConversionRates.
 */
MaybeStackVector<ConversionRateInfo>
    U_I18N_API getConversionRatesInfo(const MaybeStackVector<MeasureUnit> &units, UErrorCode &status);

// Encapsulates unitPreferenceData information from units resources, specifying
// a sequence of output unit preferences.
struct U_I18N_API UnitPreference {
    UnitPreference() : geq(1) {}
    CharString unit;
    double geq;
    CharString skeleton;
};

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

U_NAMESPACE_END

#endif //__GETUNITSDATA_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
