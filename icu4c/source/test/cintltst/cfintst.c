/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CFINTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API
*********************************************************************************
/**
 * CollationFinnishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "cfintst.h"
#include "unicode/ustring.h"
#include "string.h"

static UCollator *myCollation;
const static UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {0x0077/*'w'*/, 0x0061/*'a'*/, 0x0074/*'t'*/, 0x0000},
    {0x0076/*'v'*/, 0x0061/*'a'*/, 0x0074/*'t'*/, 0x0000},
    {0x0061/*'a'*/, 0x00FC, 0x0062/*'b'*/, 0x0065/*'e'*/, 0x0063/*'c'*/, 0x006b/*'k'*/, 0x0000},
    {0x004c/*'L'*/, 0x00E5, 0x0076/*'v'*/, 0x0069/*'i'*/, 0x0000},
    {0x0077/*'w'*/, 0x0061/*'a'*/, 0x0074/*'t'*/, 0x0000}
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {0x0076/*'v'*/, 0x0061/*'a'*/, 0x0074/*'t'*/, 0x0000},
    {0x0077/*'w'*/, 0x0061/*'a'*/, 0x0079/*'y'*/, 0x0000},
    {0x0061/*'a'*/, 0x0078/*'x'*/, 0x0062/*'b'*/, 0x0065/*'e'*/, 0x0063/*'c'*/, 0x006b/*'k'*/, 0x0000},
    {0x004c/*'L'*/, 0x00E4, 0x0077/*'w'*/, 0x0065/*'e'*/, 0x0000},
    {0x0076/*'v'*/, 0x0061/*'a'*/, 0x0074/*'t'*/, 0x0000}
};

const static UCollationResult results[] = {
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_LESS,
    /* test primary > 4*/
    UCOL_EQUAL
};



void addFinnishCollTest(TestNode** root)
{
    
    
    addTest(root, &TestPrimary, "tscoll/cficoll/TestPrimary");
    addTest(root, &TestTertiary, "tscoll/cficoll/TestTertiary");
    


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
    myCollation = ucol_open("fi_FI", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Finnish Collation with Tertiary strength\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 4 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

void TestPrimary()
{
    
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("fi_FI", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Finnish Collation with Tertiary strength\n");
    ucol_setStrength(myCollation, UCOL_PRIMARY);
    for (i = 4; i < 4; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

