// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_MACROS_H
#define MESSAGEFORMAT2_MACROS_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

// Tokens for parser and serializer
 
// Syntactically significant characters
#define LEFT_CURLY_BRACE ((UChar32)0x007B)
#define RIGHT_CURLY_BRACE ((UChar32)0x007D)
#define SPACE ((UChar32)0x0020)
#define HTAB ((UChar32)0x0009)
#define CR ((UChar32)0x000D)
#define LF ((UChar32)0x000A)
#define BACKSLASH ((UChar32)0x005C)
#define PIPE ((UChar32)0x007C)
#define EQUALS ((UChar32)0x003D)
#define DOLLAR ((UChar32)0x0024)
#define COLON ((UChar32)0x003A)
#define PLUS ((UChar32)0x002B)
#define HYPHEN ((UChar32)0x002D)
#define PERIOD ((UChar32)0x002E)
#define UNDERSCORE ((UChar32)0x005F)

// Both used (in a `key` context) and reserved (in an annotation context)
#define ASTERISK ((UChar32)0x002A)

// Reserved sigils
#define BANG ((UChar32)0x0021)
#define AT ((UChar32)0x0040)
#define POUND ((UChar32)0x0023)
#define PERCENT ((UChar32)0x0025)
#define CARET ((UChar32)0x005E)
#define AMPERSAND ((UChar32)0x0026)
#define LESS_THAN ((UChar32)0x003C)
#define GREATER_THAN ((UChar32)0x003E)
#define QUESTION ((UChar32)0x003F)
#define TILDE ((UChar32)0x007E)

// MessageFormat2 uses three keywords: `let`, `when`, and `match`.

static constexpr UChar32 ID_LET[] = {
    0x6C, 0x65, 0x74, 0 /* "let" */
};

static constexpr UChar32 ID_WHEN[] = {
    0x77, 0x68, 0x65, 0x6E, 0 /* "when" */
};

static constexpr UChar32 ID_MATCH[] = {
    0x6D, 0x61, 0x74, 0x63, 0x68, 0 /* "match" */
};

// See `s` in the MessageFormat 2 grammar
inline bool isWhitespace(UChar32 c) {
    switch (c) {
    case SPACE:
    case HTAB:
    case CR:
    case LF:
        return true;
    default:
        return false;
    }
}
 
// Returns immediately if `errorCode` indicates failure
#define CHECK_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return;                                                                                         \
    }

// Returns immediately if `errorCode` indicates failure
#define NULL_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return nullptr;                                                                                         \
    }
 
// Returns immediately if `errorCode` indicates failure
#define THIS_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return *this; \
    }

// Returns immediately if `errorCode` indicates failure
#define FALSE_ON_ERROR(errorCode)                                                                          \
    if (U_FAILURE(errorCode)) {                                                                         \
        return false; \
    }

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_MACROS_H

#endif // U_HIDE_DEPRECATED_API
// eof
