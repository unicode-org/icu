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
* File CTURTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for CAPI
*********************************************************************************
/**
 * CollationTurkishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include "utypes.h"
#include "ucol.h"
#include "uloc.h"
#include "cintltst.h"
#include "ccolltst.h"
#include "cturtst.h"
#include "ustring.h"
#include "string.h"
#include <memory.h>

static UCollator *myCollation;
const static UChar testSourceCases[][MAX_TOKEN_LEN] = {
    {0x0073/*'s'*/, 0x0327, 0x0000},
    {0x0076/*'v'*/, 0x00E4, 0x0074/*'t'*/, 0x0000},
    {0x006f/*'o'*/, 0x006c/*'l'*/, 0x0064/*'d'*/, 0x0000},
    {0x00FC, 0x006f/*'o'*/, 0x0069/*'i'*/, 0x0064/*'d'*/, 0x0000},
    {0x0068/*'h'*/, 0x011E, 0x0061/*'a'*/, 0x006c/*'l'*/, 0x0074/*'t'*/, 0x0000},
    {0x0073/*'s'*/, 0x0074/*'t'*/, 0x0072/*'r'*/, 0x0065/*'e'*/, 0x0073/*'s'*/, 0x015E, 0x0000},
    {0x0076/*'v'*/, 0x006f/*'o'*/, 0x0131, 0x0064/*'d'*/, 0x0000},
    {0x0069/*'i'*/, 0x0064/*'d'*/, 0x0065/*'e'*/, 0x0061/*'a'*/, 0x0000},
    {0x00FC, 0x006f/*'o'*/, 0x0069/*'i'*/, 0x0064 /*d'*/, 0x0000},
    {0x0076/*'v'*/, 0x006f/*'o'*/, 0x0131, 0x0064 /*d'*/, 0x0000},
    {0x0069/*'i'*/, 0x0064/*'d'*/, 0x0065/*'e'*/, 0x0061/*'a'*/, 0x0000},
};

const static UChar testTargetCases[][MAX_TOKEN_LEN] = {
    {0x0075/*'u'*/, 0x0308, 0x0000},
    {0x0076/*'v'*/, 0x0062/*'b'*/, 0x0074/*'t'*/, 0x0000},
    {0x00D6, 0x0061/*'a'*/, 0x0079/*'y'*/, 0x0000},
    {0x0076/*'v'*/, 0x006f/*'o'*/, 0x0069/*'i'*/, 0x0064 /*d'*/, 0x0000},
    {0x0068/*'h'*/, 0x0061/*'a'*/,  0x006c/*'l'*/, 0x0074/*'t'*/, 0x0000},
    {0x015E, 0x0074/*'t'*/, 0x0072/*'r'*/, 0x0065/*'e'*/, 0x015E, 0x0073/*'s'*/, 0x0000},
    {0x0076/*'v'*/, 0x006f/*'o'*/, 0x0069/*'i'*/, 0x0064 /*d'*/, 0x0000},
    {0x0049/*'I'*/, 0x0064/*'d'*/, 0x0065/*'e'*/, 0x0061/*'a'*/, 0x0000},
    {0x0076/*'v'*/, 0x006f/*'o'*/, 0x0069/*'i'*/, 0x0064 /*d'*/, 0x0000},
    {0x0076/*'v'*/, 0x006f/*'o'*/, 0x0069/*'i'*/, 0x0064 /*d'*/, 0x0000},
    {0x0049/*'I'*/, 0x0064/*'d'*/, 0x0065/*'e'*/, 0x0061/*'a'*/, 0x0000},
};

const static UCollationResult results[] = {
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_GREATER,
    UCOL_LESS,
    UCOL_LESS,
    UCOL_GREATER,
    /* test priamry > 8 */
    UCOL_LESS,
    UCOL_EQUAL,
    UCOL_EQUAL
};



void addTurkishCollTest(TestNode** root)
{
    
    addTest(root, &TestPrimary, "tscoll/cturtst/TestPrimary");
    addTest(root, &TestTertiary, "tscoll/cturtst/TestTertiary");


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
    myCollation = ucol_open("tr", &status);
    if(FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Turkish Collation with Tertiary strength\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);
    for (i = 0; i < 8 ; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

void TestPrimary()
{
    
    int32_t i;

    UErrorCode status = ZERO_ERROR;
    myCollation = ucol_open("tr", &status);
    if(FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    log_verbose("Testing Turkish Collation with Primary strength\n");
    ucol_setStrength(myCollation, UCOL_PRIMARY);
    for (i = 8; i < 11; i++)
    {
        doTest(myCollation, testSourceCases[i], testTargetCases[i], results[i]);
    }
    ucol_close(myCollation);
}

