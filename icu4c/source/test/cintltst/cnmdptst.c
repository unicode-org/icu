/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*******************************************************************************
*
* File CNMDPTST.C
*
*  Madhu Katragadda                       Creation
* Modification History:
*
*   Date        Name        Description
*   06/24/99    helena      Integrated Alan's NF enhancements and Java2 bug fixes
*******************************************************************************
*/

/* C DEPTH TEST FOR NUMBER FORMAT */

#include "unicode/uloc.h"
#include "unicode/utypes.h"
#include "unicode/unum.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include "cnmdptst.h"
#include <stdio.h>
#include <string.h>

#define CHECK(status,str) if (U_FAILURE(status)) { log_err("FAIL: %s\n", str); return; }

void addNumFrDepTest(TestNode** root)
{
  addTest(root, &TestPatterns, "tsformat/cnmdptst/TestPatterns");
  addTest(root, &TestQuotes, "tsformat/cnmdptst/TestQuotes");
  addTest(root, &TestExponential, "tsformat/cnmdptst/TestExponential");
  addTest(root, &TestCurrencySign, "tsformat/cnmdptst/TestCurrencySign");
  addTest(root, &TestCurrency,  "tsformat/cnmdptst/TestCurrency");
  addTest(root, &TestRounding487, "tsformat/cnmdptst/TestRounding487");
  addTest(root, &TestDoubleAttribute, "tsformat/cnmdptst/TestDoubleAttribute");
  addTest(root, &TestSecondaryGrouping, "tsformat/cnmdptst/TestSecondaryGrouping");

}
/*Test Various format patterns*/
void TestPatterns(void)
{
  int32_t pat_length, i, lneed;
  UNumberFormat *fmt;
  UFieldPosition pos;
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
      lneed=unum_format(fmt, 0, NULL, lneed, &pos, &status);
      if(status==U_BUFFER_OVERFLOW_ERROR){
	status=U_ZERO_ERROR;
	str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
	unum_format(fmt, 0, str, lneed+1,  &pos, &status);
      }
      if(U_FAILURE(status))	{
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
}
/* Test the handling of quotes*/
void TestQuotes(void)
{
  int32_t lneed;
  UErrorCode status;
  UFieldPosition pos;
  UChar pat[15];
  UChar res[15];
  UChar *str=NULL;
  UNumberFormat *fmt;
  status=U_ZERO_ERROR;
  log_verbose("\nTestting the handling of quotes in number format\n");
  u_uastrcpy(pat, "a'fo''o'b#");
  fmt =unum_openPattern(pat, u_strlen(pat), "en_US", &status);
  if(U_FAILURE(status)){
    log_err("Error in number format costruction using pattern \"a'fo''o'b#\"\n");
  }
  lneed=0;
  lneed=unum_format(fmt, 123, NULL, lneed, &pos, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
    unum_format(fmt, 123, str, lneed+1,  &pos, &status);
  }
  if(U_FAILURE(status))	{
    log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
  }
  log_verbose("Pattern \"%s\" \n", austrdup(pat) );
  log_verbose("Format 123 -> %s\n", austrdup(str) );
  u_uastrcpy(res, "afo'ob123");
  if(u_strcmp(str, res) != 0)
    log_err("FAIL: Expected afo'ob123");
    
  free(str);
  unum_close(fmt);
    
	
  u_uastrcpy(pat, "");
  u_uastrcpy(pat, "a''b#");
    
	
  fmt =unum_openPattern(pat, u_strlen(pat), "en_US", &status);
  if(U_FAILURE(status)){
    log_err("Error in number format costruction using pattern \"a''b#\"\n");
  }
  lneed=0;
  lneed=unum_format(fmt, 123, NULL, lneed, &pos, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
    unum_format(fmt, 123, str, lneed+1,  &pos, &status);
  }
  if(U_FAILURE(status))	{
    log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
  }
  log_verbose("Pattern \"%s\" \n", austrdup(pat) );
  log_verbose("Format 123 -> %s\n", austrdup(str) );
  u_uastrcpy(res, "");
  u_uastrcpy(res, "a'b123");
  if(u_strcmp(str, res) != 0)
    log_err("FAIL: Expected a'b123\n");
	
  free(str);
  unum_close(fmt);
}

/* Test exponential pattern*/
void TestExponential(void)
{
  int32_t pat_length, val_length, lval_length;
  int32_t ival, ilval, p, v, lneed;
  UNumberFormat *fmt;
  UFieldPosition pos;
  int32_t ppos;
  UChar *upat;
  UChar pattern[20];
  UChar *str=NULL;
  UChar uvalfor[20], ulvalfor[20];
  double a;
  UErrorCode status = U_ZERO_ERROR;
#ifdef OS390
  double val[] = { 0.01234, 123456789, 1.23e75, -3.141592653e-78 };
#else
  double val[] = { 0.01234, 123456789, 1.23e300, -3.141592653e-271 };
#endif
  char* pat[] = { "0.####E0", "00.000E00", "##0.######E000", "0.###E0;[0.###E0]"  };
  int32_t lval[] = { 0, -1, 1, 123456789 };
	
  char* valFormat[] =
  {
    "1.234E-2", "1.2346E8", "1.23E300", "-3.1416E-271",
    "12.340E-03", "12.346E07", "12.300E299", "-31.416E-272",
    "12.34E-003", "123.4568E006", "1.23E300", "-314.1593E-273",
    "1.234E-2", "1.235E8", "1.23E300", "[3.142E-271]"
  };
  char* lvalFormat[] =
  {
    "0E0", "-1E0", "1E0", "1.2346E8",
    "00.000E00", "-10.000E-01", "10.000E-01", "12.346E07",
    "0E000", "-1E000", "1E000", "123.4568E006",
    "0E0", "[1E0]", "1E0", "1.235E8"
  };
  double valParse[] =
  {
#ifdef OS390
    0.01234, 123460000, 1.23E75, -3.1416E-78,
    0.01234, 123460000, 1.23E75, -3.1416E-78,
    0.01234, 123456800, 1.23E75, -3.141593E-78,
    0.01234, 123500000, 1.23E75, -3.142E-78
#else
    0.01234, 123460000, 1.23E300, -3.1416E-271,
    0.01234, 123460000, 1.23E300, -3.1416E-271,
    0.01234, 123456800, 1.23E300, -3.141593E-271,
    0.01234, 123500000, 1.23E300, -3.142E-271
#endif
  };
  int32_t lvalParse[] =
  {
    0, -1, 1, 123460000,
    0, -1, 1, 123460000,
    0, -1, 1, 123456800,
    0, -1, 1, 123500000
  };


  pat_length = sizeof(pat) / sizeof(pat[0]);
  val_length = sizeof(val) / sizeof(val[0]);
  lval_length = sizeof(lval) / sizeof(lval[0]);
  ival = 0;
  ilval = 0;
  for (p=0; p < pat_length; ++p)
    {
      upat=(UChar*)malloc(sizeof(UChar) * (strlen(pat[p])+1) );
      u_uastrcpy(upat, pat[p]);
      fmt=unum_openPattern(upat, u_strlen(upat), "en_US", &status);
      if (U_FAILURE(status)) { 
	log_err("FAIL: Bad status returned by Number format construction with pattern %s\n, pat[i]"); 
	continue; 
      }
      lneed= u_strlen(upat) + 1;
      unum_toPattern(fmt, FALSE, pattern, lneed, &status);
      log_verbose("Pattern \" %s \" -toPattern-> \" %s \" \n", upat, austrdup(pattern) );
      for (v=0; v<val_length; ++v)
        {
	  /*format*/
	  lneed=0; 
	  lneed=unum_formatDouble(fmt, val[v], NULL, lneed, &pos, &status);
	  if(status==U_BUFFER_OVERFLOW_ERROR){
	    status=U_ZERO_ERROR;
	    str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
	    unum_formatDouble(fmt, val[v], str, lneed+1,  &pos, &status);
	  }
	  if(U_FAILURE(status))	{
	    log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
	  }
	  
			
			
	  u_uastrcpy(uvalfor, valFormat[v+ival]);
	  if(u_strcmp(str, uvalfor) != 0)
	    log_verbose("FAIL: Expected %s ( %s )\n", valFormat[v+ival], austrdup(uvalfor) );

	  /*parsing*/
	  ppos=0;
	  a=unum_parseDouble(fmt, str, u_strlen(str), &ppos, &status);
	  if (ppos== u_strlen(str)) {
	     if (a != valParse[v+ival])
	      printf("FAIL: Expected : %e\n", valParse[v+ival]);
	  }
	  else
	    printf(" FAIL: Partial parse (  %d  chars ) ->  %e\n",  ppos, a);
        
	  free(str);
	}
      for (v=0; v<lval_length; ++v)
        {
	  /*format*/
	  lneed=0; 
	  lneed=unum_formatDouble(fmt, lval[v], NULL, lneed, &pos, &status);
	  if(status==U_BUFFER_OVERFLOW_ERROR){
	    status=U_ZERO_ERROR;
	    str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
	    unum_formatDouble(fmt, lval[v], str, lneed+1,  &pos, &status);
	  }
	  if(U_FAILURE(status))	{
	    log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
	  }
	  /*printf(" Format %e -> %s\n",  lval[v], austrdup(str) );*/
	  u_uastrcpy(ulvalfor, lvalFormat[v+ilval]);
	  if(u_strcmp(str, ulvalfor) != 0)
	    log_err("FAIL: Expected %s ( %s )\n", valFormat[v+ilval], austrdup(ulvalfor) );

	  /*parsing*/
	  ppos=0;
	  a=unum_parseDouble(fmt, str, u_strlen(str), &ppos, &status);
	  if (ppos== u_strlen(str)) {
	    /*printf(" Parse -> %e\n",  a);*/
	    if (a != lvalParse[v+ilval])
	      printf("FAIL: Expected : %e\n", valParse[v+ival]);
	  }
	  else
	    printf(" FAIL: Partial parse (  %d  chars ) ->  %e\n",  ppos, a);
        
	  free(str);

        }
      ival += val_length;
      ilval += lval_length;
      unum_close(fmt);
      free(upat);
    }
}

/**
 * Test the handling of the currency symbol in patterns.
 */
void TestCurrencySign(void)
{
  int32_t lneed;
  UNumberFormat *fmt;
  UChar *pattern=NULL;
  UChar *str=NULL;
  UChar *pat=NULL;
  UChar *res=NULL;
  UFieldPosition pos;
  UErrorCode status = U_ZERO_ERROR;
  pattern=(UChar*)malloc(sizeof(UChar) * (strlen("*#,##0.00;-*#,##0.00") + 1) );
  u_uastrcpy(pattern, "*#,##0.00;-*#,##0.00");
  pattern[0]=pattern[11]=0xa4; /* insert latin-1 currency symbol */
  fmt = unum_openPattern(pattern, u_strlen(pattern), "en_US", &status);
  if(U_FAILURE(status)){
    log_err("Error in number format construction with pattern  \"\\xA4#,##0.00;-\\xA4#,##0.00\\\" \n");
  }
  lneed=0; 
  lneed=unum_formatDouble(fmt, 1234.56, NULL, lneed, &pos, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
    unum_formatDouble(fmt, 1234.56, str, lneed+1,  &pos, &status);
  }
  if(U_FAILURE(status))	{
    log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
  }
  lneed=0;
  lneed=unum_toPattern(fmt, FALSE, NULL, lneed, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    pat=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
    unum_formatDouble(fmt, FALSE, pat, lneed+1,  &pos, &status);
  }
  log_verbose("Pattern \" %s \" \n", austrdup(pat));
  log_verbose("Format 1234.56 -> %s\n", austrdup(str) );
	
  res=(UChar*)malloc(sizeof(UChar) * (strlen("$1,234.56")+1) );
  u_uastrcpy(res, "$1,234.56");
  if (u_strcmp(str, res) !=0) log_err("FAIL: Expected $1,234.56\n");
  free(str);
  free(res);
  free(pat);

  lneed=0; 
  lneed=unum_formatDouble(fmt, -1234.56, NULL, lneed, &pos, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
    unum_formatDouble(fmt, -1234.56, str, lneed+1,  &pos, &status);
  }
  if(U_FAILURE(status))	{
    log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
  }
  res=(UChar*)malloc(sizeof(UChar) * (strlen("-$1,234.56")+1) );
  u_uastrcpy(res, "-$1,234.56");
  if (u_strcmp(str, res) != 0) log_err("FAIL: Expected -$1,234.56\n");
  free(str);
  free(res);

  unum_close(fmt);  
  free(pattern);
}
/**
 * Test localized currency patterns.
 */
void TestCurrency(void)
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
      if(U_FAILURE(status)){log_err("Error in the construction of number format with style currency:\n%s\n",
				  myErrorName(status));
      }
      lneed=0;
      lneed= unum_formatDouble(currencyFmt, 1.50, NULL, lneed, &pos, &status);
      if(status==U_BUFFER_OVERFLOW_ERROR){
	status=U_ZERO_ERROR;
	str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
	unum_formatDouble(currencyFmt, 1.50, str, lneed+1, &pos, &status);
      }
      if(U_FAILURE(status))	{
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
/**
 * Test proper rounding by the format method.
 */
void TestRounding487(void)
{
  UNumberFormat *nnf;
  UErrorCode status = U_ZERO_ERROR;
  /* this is supposed to open default date format, but later on it treats it like it is "en_US" 
     - very bad if you try to run the tests on machine where default locale is NOT "en_US" */
  /* nnf = unum_open(UNUM_DEFAULT, NULL, &status); */
  nnf = unum_open(UNUM_DEFAULT, "en_US", &status);
  if(U_FAILURE(status)){
    log_err("FAIL: failure in the construction of number format: %s\n", myErrorName(status));
  }
  roundingTest(nnf, 0.00159999, 4, "0.0016");
  roundingTest(nnf, 0.00995, 4, "0.01");

  roundingTest(nnf, 12.3995, 3, "12.4");

  roundingTest(nnf, 12.4999, 0, "12");
  roundingTest(nnf, - 19.5, 0, "-20");
  unum_close(nnf);
}
 
/*-------------------------------------*/
 
void roundingTest(UNumberFormat* nf, double x, int32_t maxFractionDigits, const char* expected)
{
  UChar *out = NULL;
  UChar *res;
  UFieldPosition pos;
  UErrorCode status;
  int32_t lneed;
  status=U_ZERO_ERROR;
  unum_setAttribute(nf, UNUM_MAX_FRACTION_DIGITS, maxFractionDigits);
  lneed=0;
  lneed=unum_formatDouble(nf, x, NULL, lneed, &pos, &status);
  if(status==U_BUFFER_OVERFLOW_ERROR){
    status=U_ZERO_ERROR;
    out=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
    unum_formatDouble(nf, x, out, lneed+1, &pos, &status);
  }
  if(U_FAILURE(status))	{
    log_err("Error in formatting using unum_formatDouble(.....): %s\n", myErrorName(status) );
  }
  /*Need to use log_verbose here. Problem with the float*/
  /*printf("%f format with %d fraction digits to %s\n", x, maxFractionDigits, austrdup(out) );*/
  res=(UChar*)malloc(sizeof(UChar) * (strlen(expected)+1) );
  u_uastrcpy(res, expected);
  if (u_strcmp(out, res) != 0) log_err("FAIL: Expected: %s or %s\n", expected, austrdup(res) );
  free(res);
  if(out != NULL) {
    free(out);
  }
}
/*
 * Testing unum_getDoubleAttribute and  unum_setDoubleAttribute() 
 */
void TestDoubleAttribute(void)
{
	double mydata[] = { 1.11, 22.22, 333.33, 4444.44, 55555.55, 666666.66, 7777777.77, 88888888.88, 999999999.99};
	double dvalue;
	int i;
	UErrorCode status=U_ZERO_ERROR;
	UNumberFormatAttribute attr;
	UNumberFormatStyle style= UNUM_DEFAULT;
	UNumberFormat *def;
	def=unum_open(style, NULL, &status);
    log_verbose("\nTesting get and set DoubleAttributes\n");
    attr=UNUM_ROUNDING_INCREMENT;
    dvalue=unum_getDoubleAttribute(def, attr);
    for (i = 0; i<9 ; i++)
	{
		dvalue = mydata[i]; 
		unum_setDoubleAttribute(def, attr, dvalue);
		if(unum_getDoubleAttribute(def,attr)!=mydata[i])
			log_err("Fail: error in setting and getting double attributes for UNUM_ROUNDING_INCREMENT\n");
		else
			log_verbose("Pass: setting and getting double attributes for UNUM_ROUNDING_INCREMENT works fine\n");
  	} 
	unum_close(def);
}

/**
 * Test the functioning of the secondary grouping value.
 */
void TestSecondaryGrouping(void) {
    UErrorCode status = U_ZERO_ERROR;
    UNumberFormat *f = NULL, *g= NULL;
    UNumberFormat *us = unum_open(UNUM_DECIMAL, "en_US", &status);
    UNumberFormatSymbols usSymbols;
    UFieldPosition pos;
    UChar resultBuffer[512];
    int32_t l = 1876543210L;
    UBool ok = TRUE;
    UChar buffer[512];
    int32_t i;
    UBool expectGroup = FALSE, isGroup = FALSE;

    unum_getSymbols(us, &usSymbols);
    CHECK(status, "DecimalFormatSymbols ct");

    u_uastrcpy(buffer, "#,##,###");
    f = unum_openPattern(buffer, -1, "en_US", &status);
    CHECK(status, "DecimalFormat ct");

    unum_format(f, (int32_t)123456789L, resultBuffer, 512 , &pos, &status);
    u_uastrcpy(buffer, "12,34,56,789");
    if ((u_strcmp(resultBuffer, buffer) != 0) || U_FAILURE(status))
    {
        log_err("Fail: Formatting \"#,##,###\" pattern with 123456789 got %s, expected %s\n", resultBuffer, "12,34,56,789");
    }
    memset(resultBuffer,0, sizeof(UChar)*512);
    unum_toPattern(f, FALSE, resultBuffer, 512, &status);
    u_uastrcpy(buffer, "#,##,###");
    if ((u_strcmp(resultBuffer, buffer) != 0) || U_FAILURE(status))
    {
        log_err("Fail: toPattern() got %s, expected %s\n", resultBuffer, "#,##,###");
    }
    memset(resultBuffer,0, sizeof(UChar)*512);
    u_uastrcpy(buffer, "#,###");
    unum_applyPattern(f, FALSE, buffer, -1);
    if (U_FAILURE(status))
    {
        log_err("Fail: applyPattern call failed\n");
    }
    unum_setAttribute(f, UNUM_SECONDARY_GROUPING_SIZE, 4);
    unum_format(f, (int32_t)123456789L, resultBuffer, 512 , &pos, &status);
    u_uastrcpy(buffer, "12,3456,789");
    if ((u_strcmp(resultBuffer, buffer) != 0) || U_FAILURE(status))
    {
        log_err("Fail: Formatting \"#,###\" pattern with 123456789 got %s, expected %s\n", resultBuffer, "12,3456,789");
    }
    memset(resultBuffer,0, sizeof(UChar)*512);
    unum_toPattern(f, FALSE, resultBuffer, 512, &status);
    u_uastrcpy(buffer, "#,####,###");
    if ((u_strcmp(resultBuffer, buffer) != 0) || U_FAILURE(status))
    {
        log_err("Fail: toPattern() got %s, expected %s\n", resultBuffer, "#,####,###");
    }
    memset(resultBuffer,0, sizeof(UChar)*512);
    g = unum_open(UNUM_DECIMAL, "hi_IN", &status);
    if (U_FAILURE(status))
    {
        log_err("Fail: Cannot create UNumberFormat for \"hi_IN\" locale.\n");
    }

    unum_format(g, l, resultBuffer, 512, &pos, &status);
    unum_close(g);
    /* expect "1,87,65,43,210", but with Hindi digits */
    /*         01234567890123                         */
    if (u_strlen(resultBuffer) != 14) {
        ok = FALSE;
    } else {
        for (i=0; i<u_strlen(resultBuffer); ++i) {
            expectGroup = FALSE;
            switch (i) {
            case 1:
            case 4:
            case 7:
            case 10:
                expectGroup = TRUE;
                break;
            }
            /* Later -- fix this to get the actual grouping */
            /* character from the resource bundle.          */
            isGroup = (resultBuffer[i] == 0x002C);
            if (isGroup != expectGroup) {
                ok = FALSE;
                break;
            }
        }
    }
    if (!ok) {
        log_err("FAIL  Expected %s x hi_IN -> \"1,87,65,43,210\" (with Hindi digits), got %s\n", "1876543210L", resultBuffer);
    } 
    unum_close(f);
    unum_close(us);
}
