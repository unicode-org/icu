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
* File CJAPTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for CAPI
*********************************************************************************
/**
 * CollationKannaTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include "utypes.h"
#include "ucol.h"
#include "uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "cjaptst.h"
#include "ustring.h"
#include "string.h"
#include <memory.h>
static UCollator *myCollation;
const static UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {0x0041/*'A'*/, 0x0300, 0x0301, 0x0000},
    {0x0041/*'A'*/, 0x0300, 0x0316, 0x0000},
    {0x0041/*'A'*/, 0x0300, 0x0000},
    {0x00C0, 0x0301, 0x0000},
    {0x00C0, 0x0316, 0x0000},
    {0xff9E, 0x0000},
    {0x3042, 0x0000},
    {0x30A2, 0x0000},
    {0x3042, 0x3042, 0x0000},
    {0x30A2, 0x30FC, 0x0000},
    {0x30A2, 0x30FC, 0x30C8, 0x0000}                               /*  11 */
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {0x0041/*'A'*/, 0x0301, 0x0300, 0x0000},
    {0x0041/*'A'*/, 0x0316, 0x0300, 0x0000},
    {0x00C0, 0},
    {0x0041/*'A'*/, 0x0301, 0x0300, 0x0000},
    {0x0041/*'A'*/, 0x0316, 0x0300, 0x0000},
    {0xFF9F, 0x0000},
    {0x30A2, 0x0000},
    {0x3042, 0x3042, 0x0000},
    {0x30A2, 0x30FC, 0x0000},
    {0x30A2, 0x30FC, 0x30C8, 0x0000},
    {0x3042, 0x3042, 0x3089, 0x0000}                              /*  11 */
};

const static UCollationResult results[] = {
    UCOL_GREATER,
    UCOL_EQUAL,
    UCOL_EQUAL,
    UCOL_GREATER,
    UCOL_EQUAL,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS                                          /*  11 */
};


void addKannaCollTest(TestNode** root)
{

    addTest(root, &TestTertiary, "tscoll/cjacoll/TestTertiary");

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
    UErrorCode status = ZERO_ERROR;
    myCollation = ucol_open("ja_JP", &status);
    if(FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Kanna(Japan) Collation with Tertiary strength\n");
    ucol_setNormalization(myCollation, UCOL_DECOMP_COMPAT_COMP_CAN);
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 11 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}



