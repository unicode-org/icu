/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
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

#include "utypes.h"
#include "ctest.h"

C_FUNC void addAllTests(TestNode** root);

/**
 *complete a relative path to a full pathname, and convert to platform-specific syntax. 
 * The character seperating directories for the relative path is '|'.
 * @fullname the full path name
 * @maxsize the maximum size of the string
 * @relPath the relative path name
 */
C_FUNC void ctest_pathnameInContext(char* fullname, int32_t maxsize, const char* relPath ) ;
/**
 *Fetches the current path name of the test directory
 *returns the path of the test directory
 */
C_FUNC const char* ctest_getTestDirectory(void);
/**
 *
 * sets the path of the test directory
 * @param newDir the test directory to be set
 */

C_FUNC void ctest_setTestDirectory(const char* newDir); 
/**
 * Convert a const UChar* into a char*
 * Caller owns storage, but in practice this function
 * LEAKS so be aware of that.
 * @param unichars UChars (null terminated) to be converted
 * @return new char* to the unichars in host format
 */
 
C_FUNC char *austrdup(const UChar* unichars);


/**
 * function used to specify the error
 * converts the errorcode to an error descriptive string(const char*)
 * @param status the error code
 */
C_FUNC const char* myErrorName(UErrorCode status);


#endif
