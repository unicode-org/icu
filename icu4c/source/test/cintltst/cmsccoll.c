/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File cmsccoll.C
*
*********************************************************************************/
/**
 * These are the tests specific to ICU 1.8 and above, that I didn't know where to 
 * fit.
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

const static char cnt1[][10] = {
  "AA",
  "AC",
  "AZ",
  "AQ",
  "AB",
  "ABZ",
  "ABQ",
  "Z",
  "ABC",
  "Q",
  "B"
};

const static char cnt2[][10] = {
  "DA",
  "DAD",
  "DAZ",
  "MAR",
  "Z",
  "DAVIS",
  "MARK",
  "DAV",
  "DAVI"
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

static void IncompleteCntTest( )
{
  UErrorCode status = U_ZERO_ERROR;
  UChar *temp=(UChar*)malloc(sizeof(UChar) * 90);
  UChar *t1 =(UChar*)malloc(sizeof(UChar) * 90);
  UChar *t2 =(UChar*)malloc(sizeof(UChar) * 90);

  UCollator *coll =  NULL;
  uint32_t i = 0, j = 0;
  uint32_t size = 0;
  u_uastrcpy(temp, " & Z < ABC < Q < B");

  coll = ucol_openRules(temp, u_strlen(temp), UCOL_NO_NORMALIZATION, 
                                                UCOL_DEFAULT_STRENGTH, &status);
  if(U_SUCCESS(status)) {
    size = sizeof(cnt1)/sizeof(cnt1[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        u_uastrcpy(t1, cnt1[i]);
        u_uastrcpy(t2, cnt1[j]);
        doTest(coll, t1, t2, UCOL_LESS);
      }
    }
  } 

  ucol_close(coll);


  u_uastrcpy(temp, " & Z < DAVIS < MARK <DAV");
  coll = ucol_openRules(temp, u_strlen(temp), UCOL_NO_NORMALIZATION, 
                                                UCOL_DEFAULT_STRENGTH, &status);
  if(U_SUCCESS(status)) {
    size = sizeof(cnt2)/sizeof(cnt2[0]);
    for(i = 0; i < size-1; i++) {
      for(j = i+1; j < size; j++) {
        u_uastrcpy(t1, cnt2[i]);
        u_uastrcpy(t2, cnt2[j]);
        doTest(coll, t1, t2, UCOL_LESS);
      }
    }
  } 

  ucol_close(coll);


}

void addMiscCollTest(TestNode** root)
{ 
    addTest(root, &TestCase, "tscoll/cmsccoll/TestCase");
    addTest(root, &IncompleteCntTest, "tscoll/cmsccoll/IncompleteCntTest");
}
