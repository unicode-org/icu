// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_MACROS_H
#define MESSAGEFORMAT2_MACROS_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/format.h"
#include "unicode/unistr.h"
#include "plurrule_impl.h"
#include "ubidiimp.h"

U_NAMESPACE_BEGIN

namespace message2 {

using namespace pluralimpl;

// Tokens for parser and serializer

// Syntactically significant characters
inline constexpr UChar32 MF2_LEFT_CURLY_BRACE = 0x007B;
inline constexpr UChar32 MF2_RIGHT_CURLY_BRACE = 0x007D;
inline constexpr UChar32 MF2_HTAB = 0x0009;
inline constexpr UChar32 MF2_IDEOGRAPHIC_SPACE = 0x3000;

inline constexpr UChar32 MF2_PIPE = 0x007C;
inline constexpr UChar32 MF2_EQUALS = 0x003D;
inline constexpr UChar32 MF2_DOLLAR = 0x0024;
inline constexpr UChar32 MF2_COLON = 0x003A;
inline constexpr UChar32 MF2_PLUS = 0x002B;
inline constexpr UChar32 MF2_HYPHEN = 0x002D;
inline constexpr UChar32 MF2_PERIOD = 0x002E;
inline constexpr UChar32 MF2_UNDERSCORE = 0x005F;

inline constexpr UChar32 MF2_LOWERCASE_E = 0x0065;
inline constexpr UChar32 MF2_UPPERCASE_E = 0x0045;

// Reserved sigils
inline constexpr UChar32 MF2_BANG = 0x0021;
inline constexpr UChar32 MF2_AT = 0x0040;
inline constexpr UChar32 MF2_PERCENT = 0x0025;
inline constexpr UChar32 MF2_CARET = 0x005E;
inline constexpr UChar32 MF2_AMPERSAND = 0x0026;
inline constexpr UChar32 MF2_LESS_THAN = 0x003C;
inline constexpr UChar32 MF2_GREATER_THAN = 0x003E;
inline constexpr UChar32 MF2_QUESTION = 0x003F;
inline constexpr UChar32 MF2_TILDE = 0x007E;

// Fallback
inline constexpr UChar32 MF2_REPLACEMENT = 0xFFFD;

// MessageFormat2 uses three keywords: `.input`, `.local`, and `.match`.

static constexpr std::u16string_view ID_INPUT = u".input";
static constexpr std::u16string_view ID_LOCAL = u".local";
static constexpr std::u16string_view ID_MATCH = u".match";

// Returns immediately if `errorCode` indicates failure
#define MF2_CHECK_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return;                                                                                         \
    }

// Returns immediately if `errorCode` indicates failure
#define MF2_NULL_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return nullptr;                                                                                         \
    }

// Returns immediately if `errorCode` indicates failure
#define MF2_THIS_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return *this; \
    }

// Returns immediately if `errorCode` indicates failure
#define MF2_EMPTY_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return {}; \
    }

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_MACROS_H

#endif // U_HIDE_DEPRECATED_API
// eof
