#ifndef CPPUTILS_H
#define CPPUTILS_H

#include "utypes.h"

#ifdef XP_CPLUSPLUS

#include "cmemory.h"
#include "unistr.h"

/*===========================================================================*/
/* Array copy utility functions */
/*===========================================================================*/

inline void icu_arrayCopy(const double* src, double* dst, int32_t count)
{ icu_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const double* src, int32_t srcStart,
              double* dst, int32_t dstStart, int32_t count)
{ icu_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int8_t* src, int8_t* dst, int32_t count)
    { icu_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int8_t* src, int32_t srcStart,
              int8_t* dst, int32_t dstStart, int32_t count)
{ icu_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int16_t* src, int16_t* dst, int32_t count)
{ icu_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int16_t* src, int32_t srcStart,
              int16_t* dst, int32_t dstStart, int32_t count)
{ icu_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int32_t* src, int32_t* dst, int32_t count)
{ icu_memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int32_t* src, int32_t srcStart,
              int32_t* dst, int32_t dstStart, int32_t count)
{ icu_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void
icu_arrayCopy(const UChar *src, int32_t srcStart,
        UChar *dst, int32_t dstStart, int32_t count)
{ icu_memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

/******************************************************
 * Simple utility to set output buffer parameters
 ******************************************************/

CAPI void U_EXPORT2 T_fillOutputParams(const UnicodeString* temp,
				       UChar* result, 
				       const int32_t resultLength,
				       int32_t* resultLengthOut, 
				       UErrorCode* status);
#endif




#endif /* _CPPUTILS */
