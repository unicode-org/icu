/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CINTLTST.H
*
*     Madhu Katragadda               Creation
* Modification History:
*   Date        Name        Description            
*   07/13/99    helena      HPUX 11 CC port.
*********************************************************************************

The main root for C API tests
*/

#ifndef _CINTLTST
#define _CINTLTST

#include "unicode/utypes.h"
#include "unicode/ctest.h"

#ifndef U_USE_DEPRECATED_API
#define U_USE_DEPRECATED_API 1
#endif

U_CFUNC void addAllTests(TestNode** root);

/**
 * Return the path to the icu/source/data/out  directory 
 */
U_CFUNC const char* ctest_dataOutDir(void);

/**
 * Return the path to the icu/source/data/  directory 
 * for out of source builds too returns the source directory
 */
U_CFUNC const char* ctest_dataSrcDir(void);

/**
 *complete a relative path to a full pathname, and convert to platform-specific syntax. 
 * The character seperating directories for the relative path is '|'.
 * @fullname the full path name
 * @maxsize the maximum size of the string
 * @relPath the relative path name
 */
U_CFUNC void ctest_pathnameInContext(char* fullname, int32_t maxsize, const char* relPath ) ;

U_CFUNC UChar* CharsToUChars(const char* chars);

/**
 * Convert a const UChar* into a char*
 * Caller owns storage, but in practice this function
 * LEAKS so be aware of that.
 * @param unichars UChars (null terminated) to be converted
 * @return new char* to the unichars in host format
 */
 
U_CFUNC char *austrdup(const UChar* unichars);
U_CFUNC char *aescstrdup(const UChar* unichars, int32_t length);
U_CFUNC void *ctst_malloc(size_t size);
U_CFUNC void ctst_freeAll(void);

U_CFUNC const char* loadTestData(UErrorCode* err);

/**
 * function used to specify the error
 * converts the errorcode to an error descriptive string(const char*)
 * @param status the error code
 */
#define myErrorName(errorCode) u_errorName(errorCode)


#endif
