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
* File CESTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API
*********************************************************************************
/**
 * CollationSpanishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "cestst.h"
#include "ccolltst.h"
#include "unicode/ustring.h"
#include "string.h"

static UCollator *myCollation;
const static UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {0x0062/*'a'*/, 0x006c/*'l'*/, 0x0069/*'i'*/, 0x0061/*'a'*/, 0x0073/*'s'*/, 0x0000},
    {0x0045/*'E'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x0069/*'i'*/, 0x006f/*'o'*/, 0x0074/*'t'*/, 0x0000},
    {0x0048/*'H'*/, 0x0065/*'e'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x006f/*'o'*/, 0x0000},
    {0x0061/*'a'*/, 0x0063/*'c'*/, 0x0048/*'H'*/, 0x0063/*'c'*/, 0x0000},
    {0x0061/*'a'*/, 0x0063/*'c'*/, 0x0063/*'c'*/, 0x0000},
    {0x0061/*'a'*/, 0x006c/*'l'*/, 0x0069/*'i'*/, 0x0061/*'a'*/, 0x0073/*'s'*/, 0x0000},
    {0x0061/*'a'*/, 0x0063/*'c'*/, 0x0048/*'H'*/, 0x0063/*'c'*/, 0x0000},
    {0x0061/*'a'*/, 0x0063/*'c'*/, 0x0063/*'c'*/, 0x0000},
    {0x0048/*'H'*/, 0x0065/*'e'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x006f/*'o'*/, 0x0000},
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {0x0062/*'a'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x0069/*'i'*/, 0x0061/*'a'*/, 0x0073/*'s'*/, 0x0000},
    {0x0045/*'E'*/, 0x006d/*'m'*/, 0x0069/*'i'*/, 0x006f/*'o'*/, 0x0074/*'t'*/, 0x0000},
    {0x0068/*'h'*/, 0x0065/*'e'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x006f/*'O'*/, 0x0000},
    {0x0061/*'a'*/, 0x0043/*'C'*/, 0x0048/*'H'*/, 0x0063/*'c'*/, 0x0000},
    {0x0061/*'a'*/, 0x0043/*'C'*/, 0x0048/*'H'*/, 0x0063/*'c'*/, 0x0000},
    {0x0062/*'a'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x0069/*'i'*/, 0x0061/*'a'*/, 0x0073/*'s'*/, 0x0000},
    {0x0061/*'a'*/, 0x0043/*'C'*/, 0x0048/*'H'*/, 0x0063/*'c'*/, 0x0000},
    {0x0061/*'a'*/, 0x0043/*'C'*/, 0x0048/*'H'*/, 0x0063/*'c'*/, 0x0000},
    {0x0068/*'h'*/, 0x0065/*'e'*/, 0x006c/*'l'*/, 0x006c/*'l'*/, 0x006f/*'O'*/, 0x0000},
};

const static UCollationResult results[] = {
    UCOL_LESS,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_LESS,
    /* test primary > 5*/
    UCOL_LESS,
    UCOL_EQUAL,
    UCOL_LESS,
    UCOL_EQUAL
};


void addSpanishCollTest(TestNode** root)
{
    
    
    addTest(root, &TestPrimary, "tscoll/cestst/TestPrimary");
    addTest(root, &TestTertiary, "tscoll/cestst/TestTertiary");

}
void doTest(UCollator* myCollation, const UChar source[], const UChar target[], UCollationResult result)
{
    int32_t sortklen, temp;
    UCollationResult compareResult, keyResult;
    uint8_t *sortKey1, *sortKey2;
    
    compareResult = ucol_strcoll(myCollation, source, u_strlen(source), target, u_strlen(target));
    
    sortklen=ucol_getSortKey(myCollation, source, u_strlen(source),  NULL, 0);
    sortKey1=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    ucol_getSortKey(myCollation, source, u_strlen(source), sortKey1, sortklen+1);
    
    sortklen=ucol_getSortKey(myCollation, target, u_strlen(target),  NULL, 0);
    sortKey2=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    ucol_getSortKey(myCollation, target, u_strlen(target), sortKey2, sortklen+1);
    

    temp= memcmp(sortKey1, sortKey2, sortklen);
    if(temp < 0) keyResult=UCOL_LESS;
    else if(temp > 0) keyResult= UCOL_GREATER;
    else keyResult = UCOL_EQUAL;
    reportCResult( source, target, sortKey1, sortKey2, compareResult, keyResult, result );
}

void TestTertiary( )
{
    
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("es_ES", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Spanish Collation with Tertiary strength\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 5 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

void TestPrimary()
{
    
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open("es_ES", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Spanish Collation with Primary strength\n");
    ucol_setStrength(myCollation, UCOL_PRIMARY);
    for (i = 5; i < 9; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

