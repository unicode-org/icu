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
* File CCOLLTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
#ifndef _CCOLLTST
#define _CCOLLTST

#include "cintltst.h"
#include "unicode/ucol.h"

/* Internal Functions used*/

void reportCResult( const UChar source[], const UChar target[], 
                         uint8_t *sourceKey, uint8_t *targetKey,
                         UCollationResult compareResult,
                         UCollationResult keyResult,
                         UCollationResult expectedResult );

UChar* appendCompareResult(UCollationResult result, UChar* target);


UChar* CharsToUChars(const char* chars);

#endif
