// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __SOURCE_NUMBER_MULTIPLIER_H__
#define __SOURCE_NUMBER_MULTIPLIER_H__

#include "numparse_types.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


class MultiplierChain : public MicroPropsGenerator, public UMemory {
  public:
    void setAndChain(const Multiplier& other, const MicroPropsGenerator* parent);

    void processQuantity(DecimalQuantity& quantity, MicroProps& micros,
                         UErrorCode& status) const U_OVERRIDE;

  private:
    Multiplier multiplier;
    const MicroPropsGenerator *parent;
};


} // namespace impl
} // namespace number
U_NAMESPACE_END

#endif //__SOURCE_NUMBER_MULTIPLIER_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
