// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __SOURCE_NUMBER_MULTIPLIER_H__
#define __SOURCE_NUMBER_MULTIPLIER_H__

#include "numparse_types.h"
#include "number_decimfmtprops.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


/**
 * Wraps a {@link Multiplier} for use in the number formatting pipeline.
 */
class MultiplierFormatHandler : public MicroPropsGenerator, public UMemory {
  public:
    void setAndChain(const Multiplier& multiplier, const MicroPropsGenerator* parent);

    void processQuantity(DecimalQuantity& quantity, MicroProps& micros,
                         UErrorCode& status) const U_OVERRIDE;

  private:
    Multiplier multiplier;
    const MicroPropsGenerator *parent;
};


/** Gets a Multiplier from a DecimalFormatProperties. In Java, defined in RoundingUtils.java */
static inline Multiplier multiplierFromProperties(const DecimalFormatProperties& properties) {
    if (properties.magnitudeMultiplier != 0) {
        return Multiplier::powerOfTen(properties.magnitudeMultiplier);
    } else if (properties.multiplier != 1) {
        return Multiplier::arbitraryDouble(properties.multiplier);
    } else {
        return Multiplier::none();
    }
}


} // namespace impl
} // namespace number
U_NAMESPACE_END

#endif //__SOURCE_NUMBER_MULTIPLIER_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
