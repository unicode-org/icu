
/*
 * @(#)LETypes.h	1.2 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __LETYPES_H
#define __LETYPES_H

#include "unicode/utypes.h"

typedef int32_t le_int32;
typedef uint32_t le_uint32;
typedef int16_t le_int16;
typedef uint16_t le_uint16;
typedef int8_t le_int8;
typedef uint8_t le_uint8;

typedef bool_t le_bool;

#ifndef true
#define true 1
#endif

#ifndef false
#define false 0
#endif

#ifndef NULL
#define NULL 0
#endif

typedef le_uint32 LETag;

typedef le_uint16 LEGlyphID;

typedef UChar LEUnicode16;
typedef UChar32 LEUnicode32;
typedef UChar LEUnicode;	// FIXME: we should depricate this type in favor of LEUnicode16...

struct LEPoint
{
    float fX;
    float fY;
};

#ifndef XP_CPLUSPLUS
typedef struct LEPoint LEPoint;
#endif

#define LE_ARRAY_COPY(dst, src, count) memcpy(dst, src, (count) * sizeof (src)[0])

enum LEErrorCode {
	/* informational */
	// none right now...

	/* success */
	LE_NO_ERROR						= U_ZERO_ERROR,

	/* failures */
	LE_ILLEGAL_ARGUMENT_ERROR		= U_ILLEGAL_ARGUMENT_ERROR,
	LE_MEMORY_ALLOCATION_ERROR		= U_MEMORY_ALLOCATION_ERROR,
	LE_INDEX_OUT_OF_BOUNDS_ERROR	= U_INDEX_OUTOFBOUNDS_ERROR,
	LE_NO_LAYOUT_ERROR				= U_UNSUPPORTED_ERROR,
	LE_INTERNAL_ERROR				= U_INTERNAL_PROGRAM_ERROR
};

#ifndef XP_CPLUSPLUS
typedef enum LEErrorCode LEErrorCode;
#endif

#define LE_SUCCESS(code) (U_SUCCESS((UErrorCode)code))
#define LE_FAILURE(code) (U_FAILURE((UErrorCode)code))

#endif


