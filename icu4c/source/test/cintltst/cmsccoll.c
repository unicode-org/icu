/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File cmsccoll.C
*
*********************************************************************************/
/**
 * CollationGermanTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "cdetst.h"
#include "ccolltst.h"
#include "callcoll.h"
#include "unicode/ustring.h"
#include "string.h"

static UCollator *myCollation;
const static UChar rules[MAX_TOKEN_LEN] =
/*" & 0 < 1,\u2461<a,A"*/
{ 0x0026, 0x0030, 0x003C, 0x0031, 0x002C, 0x2460, 0x003C, 0x0061, 0x002C, 0x0041, 0x0000 };

const static UChar testCase[][MAX_TOKEN_LEN] =
{
    /*0*/ {0x0031 /*'1'*/, 0x0061/*'a'*/, 0x0000},     
    /*1*/ {0x0031 /*'1'*/, 0x0041/*'A'*/, 0x0000},     
    /*2*/ {0x2460 /*circ'1'*/, 0x0061/*'a'*/, 0x0000},     
    /*3*/ {0x2460 /*circ'1'*/, 0x0041/*'A'*/, 0x0000}
};

const static UCollationResult caseTestResults[][9] =
{
        { UCOL_LESS, UCOL_LESS, UCOL_LESS, 0, UCOL_LESS, UCOL_LESS, 0, 0, UCOL_LESS },        
        { UCOL_GREATER, UCOL_LESS, UCOL_LESS, 0, UCOL_LESS, UCOL_LESS, 0, 0, UCOL_GREATER },
        { UCOL_LESS, UCOL_LESS, UCOL_LESS, 0, UCOL_GREATER, UCOL_LESS, 0, 0, UCOL_LESS },        
        { UCOL_GREATER, UCOL_LESS, UCOL_GREATER, 0, UCOL_LESS, UCOL_LESS, 0, 0, UCOL_GREATER }

};

const static UColAttributeValue caseTestAttributes[][2] =
{
        { UCOL_LOWER_FIRST, UCOL_OFF},        
        { UCOL_UPPER_FIRST, UCOL_OFF},        
        { UCOL_LOWER_FIRST, UCOL_ON},        
        { UCOL_UPPER_FIRST, UCOL_ON}

};

static void TestCase( )
{
    
    int32_t i,j,k;
    UErrorCode status = U_ZERO_ERROR;
    myCollation = ucol_open(NULL, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
	return;
    }
    log_verbose("Testing different case settings\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);

    for(k = 0; k<4; k++) {
      ucol_setAttribute(myCollation, UCOL_CASE_FIRST, caseTestAttributes[k][0], &status);
      ucol_setAttribute(myCollation, UCOL_CASE_LEVEL, caseTestAttributes[k][1], &status);
      for (i = 0; i < 3 ; i++) {
        for(j = i+1; j<4; j++) {
          doTest(myCollation, testCase[i], testCase[j], caseTestResults[k][3*i+j-1]);
        }
      }
    }
    ucol_close(myCollation);    
    
    myCollation = ucol_openRules(rules, u_strlen(rules), UNORM_NONE, UCOL_TERTIARY, &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
	return;
    }
    log_verbose("Testing different case settings with custom rules\n");
    ucol_setStrength(myCollation, UCOL_TERTIARY);

    for(k = 0; k<4; k++) {
      ucol_setAttribute(myCollation, UCOL_CASE_FIRST, caseTestAttributes[k][0], &status);
      ucol_setAttribute(myCollation, UCOL_CASE_LEVEL, caseTestAttributes[k][1], &status);
      for (i = 0; i < 3 ; i++) {
        for(j = i+1; j<4; j++) {
          doTest(myCollation, testCase[i], testCase[j], caseTestResults[k][3*i+j-1]);
        }
      }
    }
    ucol_close(myCollation);    

}



void addMiscCollTest(TestNode** root)
{
    

    addTest(root, &TestCase, "tscoll/cmsccoll/TestCase");
     


}
