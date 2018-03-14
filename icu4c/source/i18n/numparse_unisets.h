// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_UNISETS_H__
#define __NUMPARSE_UNISETS_H__

#include "numparse_types.h"
#include "unicode/uniset.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {
namespace unisets {

enum Key {
    EMPTY,

    // Ignorables
            BIDI,
    WHITESPACE,
    DEFAULT_IGNORABLES,
    STRICT_IGNORABLES,

    // Separators
    // Notes:
    // - COMMA is a superset of STRICT_COMMA
    // - PERIOD is a superset of SCRICT_PERIOD
    // - ALL_SEPARATORS is the union of COMMA, PERIOD, and OTHER_GROUPING_SEPARATORS
    // - STRICT_ALL_SEPARATORS is the union of STRICT_COMMA, STRICT_PERIOD, and OTHER_GRP_SEPARATORS
            COMMA,
    PERIOD,
    STRICT_COMMA,
    STRICT_PERIOD,
    OTHER_GROUPING_SEPARATORS,
    ALL_SEPARATORS,
    STRICT_ALL_SEPARATORS,

    // Symbols
    // TODO: NaN?
            MINUS_SIGN,
    PLUS_SIGN,
    PERCENT_SIGN,
    PERMILLE_SIGN,
    INFINITY_KEY, // INFINITY is defined in cmath

    // Other
            DIGITS,
    NAN_LEAD,
    SCIENTIFIC_LEAD,
    CWCF,

    // Combined Separators with Digits (for lead code points)
            DIGITS_OR_ALL_SEPARATORS,
    DIGITS_OR_STRICT_ALL_SEPARATORS,

    // The number of elements in the enum.  Also used to indicate null.
            COUNT
};

const UnicodeSet* get(Key key);

Key chooseFrom(UnicodeString str, Key key1);

Key chooseFrom(UnicodeString str, Key key1, Key key2);

} // namespace unisets
} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_UNISETS_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
