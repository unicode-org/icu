#ifndef CPPUTILS_H
#define CPPUTILS_H

#include "utypes.h"

#ifdef XP_CPLUSPLUS

#include "unistr.h"



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
