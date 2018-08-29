// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __SOURCE_NUMRANGE_TYPES_H__
#define __SOURCE_NUMRANGE_TYPES_H__

#include "unicode/numberformatter.h"
#include "unicode/numberrangeformatter.h"
#include "number_types.h"
#include "number_decimalquantity.h"
#include "number_stringbuilder.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


/**
 * Implementation class for UFormattedNumber with magic number for safety.
 *
 * This struct is also held internally by the C++ version FormattedNumber since the member types are not
 * declared in the public header file.
 *
 * The DecimalQuantity is not currently being used by FormattedNumber, but at some point it could be used
 * to add a toDecNumber() or similar method.
 */
struct UFormattedNumberRangeData : public UMemory {
    // The magic number to identify incoming objects.
    // Reads in ASCII as "FDR" (FormatteDnumberRange with room at the end)
    static constexpr int32_t kMagic = 0x46445200;

    // Data members:
    int32_t fMagic = kMagic;
    DecimalQuantity quantity1;
    DecimalQuantity quantity2;
    NumberStringBuilder string;
    UNumberRangeIdentityType identityType = UNUM_IDENTITY_TYPE_EQUAL_BEFORE_ROUNDING;

    // No C conversion methods (no C API yet)
};


} // namespace impl
} // namespace number
U_NAMESPACE_END

#endif //__SOURCE_NUMRANGE_TYPES_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
