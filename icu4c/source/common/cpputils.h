/*
******************************************************************************
*
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  cpputils.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*/

#ifndef CPPUTILS_H
#define CPPUTILS_H

#include "unicode/utypes.h"
#include "cmemory.h"

// forward declaration
class UnicodeString;

/*==========================================================================*/
/* Array copy utility functions */
/*==========================================================================*/

inline void uprv_arrayCopy(const double* src, double* dst, int32_t count)
{ uprv_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const double* src, int32_t srcStart,
              double* dst, int32_t dstStart, int32_t count)
{ uprv_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const int8_t* src, int8_t* dst, int32_t count)
    { uprv_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const int8_t* src, int32_t srcStart,
              int8_t* dst, int32_t dstStart, int32_t count)
{ uprv_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const int16_t* src, int16_t* dst, int32_t count)
{ uprv_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const int16_t* src, int32_t srcStart,
              int16_t* dst, int32_t dstStart, int32_t count)
{ uprv_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const int32_t* src, int32_t* dst, int32_t count)
{ uprv_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void uprv_arrayCopy(const int32_t* src, int32_t srcStart,
              int32_t* dst, int32_t dstStart, int32_t count)
{ uprv_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void
uprv_arrayCopy(const UChar *src, int32_t srcStart,
        UChar *dst, int32_t dstStart, int32_t count)
{ uprv_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

/** Simple utility to fill a UChar array from a UnicodeString */
U_CAPI int32_t U_EXPORT2
uprv_fillOutputString(const UnicodeString &temp,
                      UChar *dest, 
                      int32_t destCapacity,
                      UErrorCode *status);

#endif /* _CPPUTILS */
