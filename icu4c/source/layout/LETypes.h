
/*
 * @(#)LETypes.h	1.2 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __LETYPES_H
#define __LETYPES_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "cmemory.h"

U_NAMESPACE_BEGIN

/**
 * A type used for signed, 32-bit integers.
 *
 * @stable ICU 2.4
 */
typedef int32_t le_int32;

/**
 * A type used for unsigned, 32-bit integers.
 *
 * @stable ICU 2.4
 */
typedef uint32_t le_uint32;

/**
 * A type used for signed, 16-bit integers.
 *
 * @stable ICU 2.4
 */
typedef int16_t le_int16;

/**
 * A type used for unsigned, 16-bit integers.
 *
 * @stable ICU 2.4
 */
typedef uint16_t le_uint16;

/**
 * A type used for signed, 8-bit integers.
 *
 * @stable ICU 2.4
 */
typedef int8_t le_int8;

/**
 * A type used for unsigned, 8-bit integers.
 *
 * @stable ICU 2.4
 */
typedef uint8_t le_uint8;


/**
 * A type used for boolean values.
 *
 * @stable ICU 2.4
 */
typedef UBool le_bool;

#ifndef true
/**
 * Used for <code>le_bool</code> values which are <code>true</code>.
 *
 * @stable ICU 2.4
 */
#define true 1
#endif

#ifndef false
/**
 * Used for <code>le_bool</code> values which are <code>false</code>.
 *
 * @stable ICU 2.4
 */
#define false 0
#endif

#ifndef NULL
/**
 * Used to represent empty pointers.
 *
 * @stable ICU 2.4
 */
#define NULL 0
#endif

/**
 * Used for four character tags.
 *
 * @stable ICU 2.4
 */
typedef le_uint32 LETag;

/**
 * Used for glyph indices.
 *
 * (NOTE: this will likely change to le_uint32)
 *
 * @draft ICU 2.4
 */
typedef le_uint16 LEGlyphID;

/**
 * Used to represent 16-bit Unicode code points.
 *
 * @stable ICU 2.4
 */
typedef UChar LEUnicode16;

/**
 * Used to represent 32-bit Unicode code points.
 *
 * @stable ICU 2.4
 */
typedef UChar32 LEUnicode32;

/**
 * Used to represent 16-bit Unicode code points.
 *
 * @deprecated since ICU 2.4. Use LEUnicode16 instead
 */
typedef UChar LEUnicode;

/**
 * Used to hold a pair of (x, y) values which represent a point.
 *
 * @stable ICU 2.4
 */
struct LEPoint
{
    /**
     * The x coordinate of the point.
     *
     * @stable ICU 2.4
     */
    float fX;

    /**
     * The y coordinate of the point.
     *
     * @stable ICU 2.4
     */
    float fY;
};

#ifndef XP_CPLUSPLUS
/**
 * Used to hold a pair of (x, y) values which represent a point.
 *
 * @stable ICU 2.4
 */
typedef struct LEPoint LEPoint;
#endif

/**
 * A convenience macro for copying an array.
 *
 * @stable ICU 2.4
 */
#define LE_ARRAY_COPY(dst, src, count) memcpy(dst, src, (count) * sizeof (src)[0])

/**
 * Allocate an array of basic types. This is used to isolate the rest of
 * the LayoutEngine code from cmemory.h.
 *
 * @draft ICU 2.6
 */
#define LE_NEW_ARRAY(type, count) (type *) uprv_malloc((count) * sizeof(type))

/**
 * Free an array of basic types. This is used to isolate the rest of
 * the LayoutEngine code from cmemory.h.
 *
 * @draft ICU 2.6
 */
#define LE_DELETE_ARRAY(array) uprv_free(array)

/**
 * Error codes returned by the LayoutEngine.
 *
 * @stable ICU 2.4
 */
enum LEErrorCode {
    /* informational */
    // none right now...

    /* success */
    LE_NO_ERROR                     = U_ZERO_ERROR,

    /* failures */
    LE_ILLEGAL_ARGUMENT_ERROR       = U_ILLEGAL_ARGUMENT_ERROR,
    LE_MEMORY_ALLOCATION_ERROR      = U_MEMORY_ALLOCATION_ERROR,
    LE_INDEX_OUT_OF_BOUNDS_ERROR    = U_INDEX_OUTOFBOUNDS_ERROR,
    LE_NO_LAYOUT_ERROR              = U_UNSUPPORTED_ERROR,
    LE_INTERNAL_ERROR               = U_INTERNAL_PROGRAM_ERROR
};

#ifndef XP_CPLUSPLUS
/**
 * Error codes returned by the LayoutEngine.
 *
 * @stable ICU 2.4
 */
typedef enum LEErrorCode LEErrorCode;
#endif

/**
 * A convenience macro to test for the success of a LayoutEngine call.
 *
 * @stable ICU 2.4
 */
#define LE_SUCCESS(code) (U_SUCCESS((UErrorCode)code))

/**
 * A convenience macro to test for the failure of a LayoutEngine call.
 *
 * @stable ICU 2.4
 */
#define LE_FAILURE(code) (U_FAILURE((UErrorCode)code))

U_NAMESPACE_END
#endif


