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

    // TODO(younies): hugovdm added this to resolve "git merge" issues. The API
    // should be improved to make this unnecessary.
    const MaybeStackVector<ConversionRateInfo> *getInternalList() const { return &conversionInfo_; };

  private:
    MaybeStackVector<ConversionRateInfo> conversionInfo_;
};

U_NAMESPACE_END

#endif //__GETUNITSDATA_H__

#endif /* #if !UCONFIG_NO_FORMATTING */
