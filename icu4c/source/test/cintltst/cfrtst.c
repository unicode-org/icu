/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CFRTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API
*********************************************************************************
/**
 * CollationFrenchTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "cfrtst.h"
#include "unicode/ustring.h"
#include "string.h"

static  UCollator *myCollation;
const static UChar testSourceCases[][MAX_TOKEN_LEN] =
{
    {0x0061/*'a'*/, 0x0062/*'b'*/, 0x0063/*'c'*/, 0x0000},
    {0x0043/*'C'*/, 0x004f/*'O'*/, 0x0054/*'T'*/, 0x0045/*'E'*/, 0x0000},
    {0x0063/*'c'*/, 0x006f/*'o'*/, 0x002d/*'-'*/, 0x006f/*'o'*/, 0x0070/*'p'*/, 0x0000},
    {0x0070/*'p'*/, 0x00EA, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x0065/*'e'*/, 0x0000},
    {0x0070/*'p'*/, 0x00EA, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x0065/*'e'*/, 0x0072/*'r'*/, 0x0000},
    {0x0070/*'p'*/, 0x00E9, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x0065/*'e'*/, 0x0072/*'r'*/, 0x0000},
    {0x0070/*'p'*/, 0x00E9, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x0065/*'e'*/, 0x0072/*'r'*/, 0x0000},
    {0x0048/*'H'*/, 0x0065/*'e'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x006f/*'o'*/, 0x0000},
    {0x01f1, 0x0000},
    {0xfb00, 0x0000},
    {0x01fa, 0x0000},
    {0x0101, 0x0000}
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] =
{
    {0x0041/*'A'*/, 0x0042/*'B'*/, 0x0043/*'C'*/, 0x0000},
    {0x0063/*'c'*/, 0x00f4, 0x0074/*'t'*/, 0x0065/*'e'*/, 0x0000},
    {0x0043/*'C'*/, 0x004f/*'O'*/, 0x004f/*'O'*/, 0x0050/*'P'*/, 0x0000},
    {0x0070/*'p'*/, 0x00E9, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x00E9, 0x0000},
    {0x0070/*'p'*/,  0x00E9, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x00E9, 0x0000},
    {0x0070/*'p'*/, 0x00EA, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x0065/*'e'*/, 0x0000},
    {0x0070/*'p'*/, 0x00EA, 0x0063/*'c'*/, 0x0068/*'h'*/, 0x0065/*'e'*/, 0x0072/*'r'*/, 0x0000},
    {0x0068/*'h'*/, 0x0065/*'e'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x004f/*'O'*/, 0x0000},
    {0x01ee, 0x0000},
    {0x25ca, 0x0000},
    {0x00e0, 0x0000},
    {0x01df, 0x0000}
};

const static UCollationResult results[] =
{
    UCOL_LESS,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_GREATER,
    UCOL_GREATER,
    UCOL_GREATER,
    UCOL_GREATER
};

/* 0x0300 is grave, 0x0301 is acute*/
/* the order of elements in this array must be different than the order in CollationEnglishTest*/
const static UChar testAcute[][MAX_TOKEN_LEN] =
{
    {0x0065/*'e'*/, 0x0065/*'e'*/,  0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0065/*'e'*/,  0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0300, 0x0065/*'e'*/,  0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0065/*'e'*/,  0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0301, 0x0065/*'e'*/,  0x0000},
    {0x0065/*'e'*/, 0x0065/*'e'*/, 0x0301, 0x0000}, 
    {0x0065/*'e'*/, 0x0301, 0x0065/*'e'*/, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0300, 0x0065/*'e'*/, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0065/*'e'*/, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0301, 0x0065/*'e'*/, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0065/*'e'*/, 0x0301, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0065/*'e'*/, 0x0301, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0300, 0x0065/*'e'*/, 0x0301, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0065/*'e'*/, 0x0301, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0301, 0x0065/*'e'*/, 0x0301, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0065/*'e'*/, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0065/*'e'*/, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0300, 0x0065/*'e'*/, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0065/*'e'*/, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0301, 0x0065/*'e'*/, 0x0300, 0x0000},
    {0x0065/*'e'*/, 0x0065/*'e'*/, 0x0300, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0065/*'e'*/, 0x0300, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0301, 0x0300, 0x0065/*'e'*/, 0x0300, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0065/*'e'*/, 0x0300, 0x0301, 0x0000},
    {0x0065/*'e'*/, 0x0300, 0x0301, 0x0065/*'e'*/, 0x0300, 0x0301, 0x0000}
};

const static UChar testBugs[][MAX_TOKEN_LEN] =
{
    {0x0061/*'a'*/, 0x000},
    {0x0041/*'A'*/, 0x000},
    {0x0065/*'e'*/, 0x000},
    {0x0045/*'E'*/, 0x000},
    {0x00e9, 0x000},
    {0x00e8, 0x000},
    {0x00ea, 0x000},
    {0x00eb, 0x000},
    {0x0065/*'e'*/, 0x0061/*'a'*/, 0x000},
    {0x0078/*'x'*/, 0x000}
};




void addFrenchCollTest(TestNode** root)
{
    

    addTest(root, &TestSecondary, "tscoll/cfrtst/TestSecondary");
    addTest(root, &TestTertiary, "tscoll/cfrtst/TestTertiary");
    addTest(root, &TestExtra, "tscoll/cfrtst/TestExtra");
       

}

void doTest(UCollator* myCollation, const UChar source[], const UChar target[], UCollationResult result)
{
    int32_t sortklen1, sortklen2, sortklenmax, sortklenmin;
    int32_t temp;
    UCollationResult compareResult, keyResult;
    uint8_t *sortKey1, *sortKey2;
    
    compareResult = ucol_strcoll(myCollation, source, u_strlen(source), target, u_strlen(target));
    
    sortklen1=ucol_getSortKey(myCollation, source, u_strlen(source),  NULL, 0);
    sortklen2=ucol_getSortKey(myCollation, target, u_strlen(target),  NULL, 0);

    sortklenmax = (sortklen1>sortklen2?sortklen1:sortklen2);
    sortklenmin = (sortklen1<sortklen2?sortklen1:sortklen2);

    sortKey1=(uint8_t*)malloc(sizeof(uint8_t) * (sortklenmax+1));
    ucol_getSortKey(myCollation, source, u_strlen(source), sortKey1, sortklen1+1);
    
    sortKey2=(uint8_t*)malloc(sizeof(uint8_t) * (sortklenmax+1));
    ucol_getSortKey(myCollation, target, u_strlen(target), sortKey2, sortklen2+1);
    

    temp= memcmp(sortKey1, sortKey2, sortklenmin);
    if(temp < 0) keyResult=UCOL_LESS;
    else if(temp > 0) keyResult= UCOL_GREATER;
    else keyResult = UCOL_EQUAL;
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, result );
    free(sortKey1);
    free(sortKey2);
}

void TestTertiary( )
{
    
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("fr_FR", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing French Collation with Tertiary strength\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 12 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
  ucol_close(myCollation);
}

void TestSecondary()
{
    int32_t i,j, testAcuteSize;
    UCollationResult expected=UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("fr_FR", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing French Collation with Secondary strength\n");
    /*test acute and grave ordering (compare to french collation)*/
    testAcuteSize = sizeof(testAcute) / sizeof(testAcute[0]);
    for (i = 0; i < testAcuteSize; i++)
    {
        for (j = 0; j < testAcuteSize; j++)
        {
            if (i <  j) expected = UCOL_LESS;
            if (i == j) expected = UCOL_EQUAL;
            if (i >  j) expected = UCOL_GREATER;
            doTest(myCollation, testAcute[i], testAcute[j], expected );
        }
    }
ucol_close(myCollation);
}

void TestExtra()
{
    int32_t i, j;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("fr_FR", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing French Collation extra with secondary strength\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 9 ; i++)
    {
        for (j = i + 1; j < 10; j += 1)
        {
            doTest(myCollation, testBugs[i], testBugs[j], UCOL_LESS);
        }
    }
ucol_close(myCollation);
}
