/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
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
