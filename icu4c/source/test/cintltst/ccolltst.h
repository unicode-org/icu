/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
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
                         UCollationResult incResult,
                         UCollationResult expectedResult );

UChar* appendCompareResult(UCollationResult result, UChar* target);


UChar* CharsToUChars(const char* chars);

void addCollAPITest(TestNode**);
void addCurrencyTest(TestNode**);
void addNormTest(TestNode**);
void addDanishCollTest(TestNode**);
void addGermanCollTest(TestNode**);
void addSpanishCollTest(TestNode**);
void addFrenchCollTest(TestNode**);
void addKannaCollTest(TestNode**);
void addTurkishCollTest(TestNode**);
void addEnglishCollTest(TestNode**);
void addFinnishCollTest(TestNode**);

void addRuleBasedCollTest(TestNode**);
void addCollIterTest(TestNode**);
void addAllCollTest(TestNode**);
void addMiscCollTest(TestNode**);
void addSearchTest(TestNode**);

#endif
