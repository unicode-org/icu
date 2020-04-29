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
 * Contains all the supported conversion rates.
 */
class U_I18N_API ConversionRates {
  public:
    /**
     * Constructor
     *
     * @param status Receives status.
     */
    ConversionRates(UErrorCode &status) { getAllConversionRates(conversionInfo_, status); }

    /**
     * Returns a pointer to the conversion rate info that match the `source`.
     *
     * @param source Contains the source.
     * @param status Receives status.
     */
    const ConversionRateInfo *extractConversionInfo(StringPiece source, UErrorCode &status) const;

  private:
    MaybeStackVector<ConversionRateInfo> conversionInfo_;
};

// Encapsulates unitPreferenceData information from units resources, specifying
// a sequence of output unit preferences.
struct U_I18N_API UnitPreference {
    UnitPreference() : geq(1) {}
    CharString unit;
    double geq;
    CharString skeleton;
};

namespace {

// UnitPreferenceMetadata lives in the anonymous namespace, because it should
// only be useful to internal code and unit testing code.
struct U_I18N_API UnitPreferenceMetadata {
    CharString category;
    CharString usage;
    CharString region;
    int32_t prefsOffset;
    int32_t prefsCount;
};

} // namespace

/**
 * Unit Preferences information for various locales and usages.
 */
class U_I18N_API UnitPreferences {
  public:
    /**
     * Constructor that loads data.
     *
     * @param status Receives status.
     */
    UnitPreferences(UErrorCode &status);

    /**
     * FIXME/WIP document me!
     *
     * If region can't be found, falls back to global (001). If usage can't be found, falls back to
     * "default".
     *
     * Copies the preferences structures. Consider returning pointers (references) instead?
     */
    void getPreferencesFor(const char *category, const char *usage, const char *region,
                           MaybeStackVector<UnitPreference> *outPreferences, UErrorCode &status);

  protected:
    int32_t binarySearch(const char *category, const char *usage, const char *region);

    MaybeStackVector<UnitPreferenceMetadata> metadata_;
    MaybeStackVector<UnitPreference> unitPrefs_;
};

U_NAMESPACE_END

#endif //__GETUNITSDATA_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
