/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
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
#include <stdio.h>
#include "cintltst.h"
#include "ccolltst.h"
#include "unicode/ucol.h"
#include "unicode/ustring.h"
#include "cmemory.h"

UChar U_CALLCONV testInc(void *context);

void addCollTest(TestNode** root);

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
    addFinnishCollTest(root);
    
    /* WEIVTODO: return tests here */
    addRuleBasedCollTest(root);
    addCollIterTest(root);
    addAllCollTest(root);
    addMiscCollTest(root);

}



/*Internal functions used*/
static char* dumpSk(uint8_t *sourceKey, char *sk) {
    uint32_t kLen = (uint32_t)strlen((const char *)sourceKey);
    uint32_t i = 0;
    
    *sk = 0;
    
    for(i = 0; i<kLen; i++) {
        sprintf(sk+2*i, "%02X", sourceKey[i]);
    }
    return sk;
}

void reportCResult( const UChar source[], const UChar target[], 
                         uint8_t *sourceKey, uint8_t *targetKey,
                         UCollationResult compareResult,
                         UCollationResult keyResult,
                         UCollationResult incResult,
                         UCollationResult expectedResult )
{
    UChar *sResult, *sExpect;
    sResult=(UChar*)uprv_malloc(sizeof(UChar) * 10);
    sExpect=(UChar*)uprv_malloc(sizeof(UChar) * 10);
    if (expectedResult < -1 || expectedResult > 1)
    {
        log_err("***** invalid call to reportCResult ****\n");
        return;
    }

    if (compareResult != expectedResult)
    {
        
        appendCompareResult(compareResult, sResult);
        appendCompareResult(expectedResult, sExpect);
        log_err("Compare(%s , %s) returned: %s expected: %s\n", aescstrdup(source), aescstrdup(target),
            austrdup(sResult), austrdup(sExpect) );
    }

    if (incResult != expectedResult)
    {
        
        appendCompareResult(incResult, sResult);
        appendCompareResult(expectedResult, sExpect);
        log_err("incCompare(%s , %s) returned: %s expected: %s\n", aescstrdup(source), aescstrdup(target),
            austrdup(sResult), austrdup(sExpect) );
    }

    if (keyResult != expectedResult)
    {
    
        appendCompareResult(keyResult, sResult);
        appendCompareResult(expectedResult, sExpect);

        log_err("KeyCompare(%s , %s) returned: %s expected: %s\n", aescstrdup(source), aescstrdup(target), 
            austrdup(sResult), austrdup(sExpect) );

    
    }

    if (keyResult != compareResult)
    {
    
        appendCompareResult(keyResult, sResult);
        appendCompareResult(compareResult, sExpect);

        log_err("difference between sortkey and compare result for (%s , %s) Keys: %s compare %s\n", aescstrdup(source), aescstrdup(target), 
            austrdup(sResult), austrdup(sExpect) );

    
    }

    if(keyResult != expectedResult || keyResult != compareResult)
    {
      char sk[256];
      log_verbose("SortKey1: %s\n", dumpSk(sourceKey, sk));
      log_verbose("SortKey2: %s\n", dumpSk(targetKey, sk));
    }

    uprv_free(sExpect);
    uprv_free(sResult);
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
    UChar *buf = (UChar*) uprv_malloc(sizeof(UChar) * len);
    u_unescape(str, buf, len);
    return buf;
}


/* Support for testing incremental strcoll */
typedef struct {
    const UChar *start;
    const UChar *end;
} testContext;

UChar U_CALLCONV testInc(void *context) {
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
