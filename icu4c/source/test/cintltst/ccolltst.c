/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CCOLLTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "cintltst.h"
#include "ccolltst.h"
#include "unicode/ucol.h"
#include "unicode/ustring.h"

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
void addAllCollTest(TestNode**);

void addRuleBasedCollTest(TestNode**);
void addCollIterTest(TestNode**);
void addMiscCollTest(TestNode** root);


void addCollTest(TestNode** root)
{
    addCollAPITest(root);
    addCurrencyTest(root);
    addNormTest(root);
    addDanishCollTest(root);
    addGermanCollTest(root);
    addSpanishCollTest(root);
    addFrenchCollTest(root);
    addKannaCollTest(root);
    addTurkishCollTest(root);
    addEnglishCollTest(root);
    
    /* WEIVTODO: return tests here */
    addRuleBasedCollTest(root);
    addCollIterTest(root);
    addAllCollTest(root);
    addMiscCollTest(root);

}



/*Internal functions used*/

void reportCResult( const UChar source[], const UChar target[], 
                         uint8_t *sourceKey, uint8_t *targetKey,
                         UCollationResult compareResult,
                         UCollationResult keyResult,
                         UCollationResult incResult,
                         UCollationResult expectedResult )
{
    UChar *sResult, *sExpect;
    sResult=(UChar*)malloc(sizeof(UChar) * 10);
    sExpect=(UChar*)malloc(sizeof(UChar) * 10);
    if (expectedResult < -1 || expectedResult > 1)
    {
        log_err("***** invalid call to reportCResult ****\n");
        return;
    }

    if (compareResult != expectedResult)
    {
        
        appendCompareResult(compareResult, sResult);
        appendCompareResult(expectedResult, sExpect);
        log_err("Compare(%s , %s) returned: %s expected: %s\n", austrdup(source), austrdup(target),
            austrdup(sResult), austrdup(sExpect) );
    }

    if (incResult != expectedResult)
    {
        
        appendCompareResult(incResult, sResult);
        appendCompareResult(expectedResult, sExpect);
        log_err("incCompare(%s , %s) returned: %s expected: %s\n", austrdup(source), austrdup(target),
            austrdup(sResult), austrdup(sExpect) );
    }

    if (keyResult != expectedResult)
    {
    
        appendCompareResult(keyResult, sResult);
        appendCompareResult(expectedResult, sExpect);

        log_err("KeyCompare(%s , %s) returned: %s expected: %s\n", austrdup(source), austrdup(target), 
            austrdup(sResult), austrdup(sExpect) );

    
    }

    if (keyResult != compareResult)
    {
    
        appendCompareResult(keyResult, sResult);
        appendCompareResult(compareResult, sExpect);

        log_err("difference between sortkey and compare result for (%s , %s) Keys: %s compare %s\n", austrdup(source), austrdup(target), 
            austrdup(sResult), austrdup(sExpect) );

    
    }
    free(sExpect);
    free(sResult);
}

UChar* appendCompareResult(UCollationResult result, UChar* target)
{
    if (result == UCOL_LESS)
    {
        u_uastrcpy(target, "LESS");
    }
    else if (result == UCOL_EQUAL)
    {
        u_uastrcpy(target, "EQUAL");
    }
    else if (result == UCOL_GREATER)
    {
        u_uastrcpy(target, "GREATER");
    }
    else
    {
        u_uastrcpy(target, "huh???");
            
    }

    return target;
}

UChar* CharsToUChars(const char* str) {
    /* Might be faster to just use uprv_strlen() as the preflight len - liu */
    int32_t len = u_unescape(str, 0, 0); /* preflight */
    UChar *buf = (UChar*) malloc(sizeof(UChar) * len);
    u_unescape(str, buf, len);
    return buf;
}


/* Support for testing incremental strcoll */
typedef struct {
    const UChar *start;
    const UChar *end;
} testContext;

UChar testInc(void *context) {
    testContext *s = (testContext *)context;
    if(s->start == s->end) {
        return 0xFFFF;
    } else {
        return *(s->start++);
    }
}


/* This is test for incremental */
UCollationResult
ctst_strcollTestIncremental(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
   testContext tcSource, tcTarget;

    if(sourceLength == -1) {
        sourceLength = u_strlen(source);
    }
    if(targetLength == -1) {
        targetLength = u_strlen(target
            );
    }
   tcSource.start = source;
   tcSource.end = source+sourceLength;
   tcTarget.start = target;
   tcTarget.end = target + targetLength;

   return ucol_strcollinc(coll, testInc, &tcSource, testInc, &tcTarget);

}
