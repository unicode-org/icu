// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING
#ifndef __SOURCE_NUMBER_UTYPES_H__
#define __SOURCE_NUMBER_UTYPES_H__

#include "unicode/numberformatter.h"
#include "number_types.h"
#include "number_decimalquantity.h"
#include "number_stringbuilder.h"
#include "capi_helper.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


/**
 * Implementation class for UNumberFormatter. Wraps a LocalizedNumberFormatter.
 */
struct UNumberFormatterData : public UMemory,
        // Magic number as ASCII == "NFR" (NumberFormatteR)
        public IcuCApiHelper<UNumberFormatter, UNumberFormatterData, 0x4E465200> {
    LocalizedNumberFormatter fFormatter;
};


/**
 * Implementation class for UFormattedNumber.
 *
 * This struct is also held internally by the C++ version FormattedNumber since the member types are not
 * declared in the public header file.
 *
 * The DecimalQuantity is not currently being used by FormattedNumber, but at some point it could be used
 * to add a toDecNumber() or similar method.
 */
struct UFormattedNumberData : public UMemory,
        // Magic number as ASCII == "FDN" (FormatteDNumber)
        public IcuCApiHelper<UFormattedNumber, UFormattedNumberData, 0x46444E00> {
    DecimalQuantity quantity;
    NumberStringBuilder string;
};


} // namespace impl
} // namespace number
U_NAMESPACE_END

#endif //__SOURCE_NUMBER_UTYPES_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
