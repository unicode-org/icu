/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
 *
 * File CTSTDEP.C
 *
 * Modification History:
 *        Name                     Description
 *    Ram Viswanadha              Creation
 *
 * Test for deprecated formatting APIs.
 *
 * Note: This test tests all deprecated C API which follow the method of application 
 * complication using preporcessor magic suggested by SRL.Compilation of this test 
 * will fail for every release when U_USE_DEPRECATED_FORMAT_API is set, as a reminder  
 * for updating macros for deprecated APIs and eventual removal of those APIs.
 *********************************************************************************
 */

#ifndef U_USE_DEPRECATED_FORMAT_API
#define U_USE_DEPRECATED_FORMAT_API 1
#endif

#include "unicode/unum.h"
#include "unicode/ucol.h"
#include "unicode/udat.h"
#include "unicode/ustring.h"
#include <string.h>
#include "cintltst.h"
#include <stdlib.h>
#include <stdio.h>


static void TestDeprecatedCollationAPI(void);
static void TestDeprecatedNumFmtAPI(void);
static void TestDeprecatedDateFmtAPI(void);
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

void 
addTestDeprecatedAPI(TestNode** root)
{
   addTest(root, &TestDeprecatedCollationAPI, "ctstdep/TestDeprecatedCollationAPI");
   addTest(root, &TestDeprecatedNumFmtAPI, "ctstdep/TestDeprecatedNumFmtAPI");
   addTest(root, &TestDeprecatedDateFmtAPI,  "ctstdep/TestDeprecatedDateFmtAPI");
}

/*
 *TODO: The ucol_openRules method which does not take UParseError as one of its params
 * has been deprecated in 2.0 release.Please remove this API by 10/1/2002
 */
static void 
TestDeprecatedCollationAPI(void)
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
        UCollationElements *iter;
        u_uastrcpy(t1, cnt1[i]);
        u_uastrcpy(t2, cnt1[j]);
        iter = ucol_openElements(coll, t2, u_strlen(t2), &status);
        if (U_FAILURE(status)) {
          log_err("Creation of iterator failed\n");
          break;
        }
        free(iter);
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
        UCollationElements *iter;
        u_uastrcpy(t1, cnt2[i]);
        u_uastrcpy(t2, cnt2[j]);
        iter = ucol_openElements(coll, t2, u_strlen(t2), &status);
        if (U_FAILURE(status)) {
          log_err("Creation of iterator failed\n");
          break;
        }
        free(iter);
      }
    }
  } 

  ucol_close(coll);
}

/*
 *TODO: The unum_open,unum_applyPattern, which does not take UParseError as one of their params
 *and unum_openPattern methods have been deprecated in 2.0 release.Please remove this API by 10/1/2002
 */

static void 
TestDeprecatedNumFmtAPI(void)
{

  int32_t pat_length, i, lneed;
  UNumberFormat *fmt;
  UChar upat[5];
  UChar unewpat[5];
  UChar unum[5];
  UChar *unewp=NULL;
  UChar *str=NULL;
  UErrorCode status = U_ZERO_ERROR;
  const char* pat[]    = { "#.#", "#.", ".#", "#" };
  const char* newpat[] = { "#0.#", "#0.", "#.0", "#" };
  const char* num[]    = { "0",   "0.", ".0", "0" };

  log_verbose("\nTesting different format patterns\n");
  pat_length = sizeof(pat) / sizeof(pat[0]);
  for (i=0; i < pat_length; ++i)
    {
      status = U_ZERO_ERROR;
      u_uastrcpy(upat, pat[i]);
      fmt= unum_openPattern(upat, u_strlen(upat), "en_US", &status);
      if (U_FAILURE(status)) {
        log_err("FAIL: Number format constructor failed for pattern %s\n", pat[i]);
        continue; 
      }
      lneed=0;
      lneed=unum_toPattern(fmt, FALSE, NULL, lneed, &status);
      if(status==U_BUFFER_OVERFLOW_ERROR){
        status= U_ZERO_ERROR;
        unewp=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
        unum_toPattern(fmt, FALSE, unewp, lneed+1, &status);
      }
      if(U_FAILURE(status)){
        log_err("FAIL: Number format extracting the pattern failed for %s\n", pat[i]);
      }
      u_uastrcpy(unewpat, newpat[i]);
      if(u_strcmp(unewp, unewpat) != 0)
        log_err("FAIL: Pattern  %s should be transmute to %s; %s seen instead\n", pat[i], newpat[i],  austrdup(unewp) );

      lneed=0;
      lneed=unum_format(fmt, 0, NULL, lneed, NULL, &status);
      if(status==U_BUFFER_OVERFLOW_ERROR){
        status=U_ZERO_ERROR;
        str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
        unum_format(fmt, 0, str, lneed+1,  NULL, &status);
      }
      if(U_FAILURE(status)) {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
      }
      u_uastrcpy(unum, num[i]);
      if (u_strcmp(str, unum) != 0)
        {
          log_err("FAIL: Pattern %s should format zero as %s; %s Seen instead\n", pat[i], num[i], austrdup(str) );

        }
      free(unewp);
      free(str);
      unum_close(fmt);
    }

    {
      UNumberFormat *currencyFmt;
      UChar *str=NULL, *res=NULL;
      int32_t lneed, i;
      UFieldPosition pos;
      UErrorCode status = U_ZERO_ERROR;
      const char* locale[]={"fr_CA", "de_DE", "fr_FR"};
      const char* result[]={"1,50 $", "1,50 DM", "1,50 F"};
      log_verbose("\nTesting the number format with different currency patterns\n");
      for(i=0; i < 3; i++)
        {
          currencyFmt = unum_open(UNUM_CURRENCY, locale[i], &status);
          if(U_FAILURE(status)){
              log_err("Error in the construction of number format with style currency:\n%s\n",
                      myErrorName(status));
          }
          lneed=0;
          lneed= unum_formatDouble(currencyFmt, 1.50, NULL, lneed, NULL, &status);
          if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
            pos.field = 0;
            unum_formatDouble(currencyFmt, 1.50, str, lneed+1, &pos, &status);
          }
          if(U_FAILURE(status)) {
            log_err("Error in formatting using unum_formatDouble(.....): %s\n", myErrorName(status) );
          }
          res=(UChar*)malloc(sizeof(UChar) * (strlen(result[i])+1) );
          u_uastrcpy(res, result[i]);
          if (u_strcmp(str, res) != 0) log_err("FAIL: Expected %s\n", result[i]);
          unum_close(currencyFmt);
          free(str);
          free(res);
        }
    }
}

/*
 *TODO: udat_open and udat_openPatterns methods have been unified and deprecated in 2.0 release.
 *Please remove this API by 10/1/2002.
 */
static void 
TestDeprecatedDateFmtAPI(void)
{
    UDateFormat *def, *fr, *fr_pat ;
    UErrorCode status = U_ZERO_ERROR;
    UChar temp[30];

    fr = udat_open(UDAT_FULL, UDAT_DEFAULT, "fr_FR", NULL,0, &status);
    if(U_FAILURE(status))
    {
        log_err("FAIL: error in creating the dateformat using full time style with french locale\n %s\n", 
            myErrorName(status) );
    }
    /* this is supposed to open default date format, but later on it treats it like it is "en_US" 
       - very bad if you try to run the tests on machine where default locale is NOT "en_US" */
    /* def = udat_open(UDAT_SHORT, UDAT_SHORT, NULL, NULL, 0, &status); */
    def = udat_open(UDAT_SHORT, UDAT_SHORT, "en_US", NULL, 0, &status);
    if(U_FAILURE(status))
    {
        log_err("FAIL: error in creating the dateformat using short date and time style\n %s\n", 
            myErrorName(status) );
    }

    /*Testing udat_openPattern()  */
    status=U_ZERO_ERROR;
    log_verbose("\nTesting the udat_openPattern with a specified pattern\n");
    /*for french locale */
    fr_pat=udat_openPattern(temp, u_strlen(temp), "fr_FR", &status);
    if(U_FAILURE(status))
    {
        log_err("FAIL: Error in creating a date format using udat_openPattern \n %s\n", 
            myErrorName(status) );
    }
    else
        log_verbose("PASS: creating dateformat using udat_openPattern() succesful\n");

}
