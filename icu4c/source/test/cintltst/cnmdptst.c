/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1996                                       *
*   (C) Copyright International Business Machines Corporation,  1999	      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
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

#include "uloc.h"
#include "utypes.h"
#include "unum.h"
#include "ustring.h"
#include "cintltst.h"
#include "cnmdptst.h"
#include <stdio.h>
#include <string.h>

void addNumFrDepTest(TestNode** root)
{
  addTest(root, &TestPatterns, "tsformat/cnmdptst/TestPatterns");
  addTest(root, &TestQuotes, "tsformat/cnmdptst/TestQuotes");
  addTest(root, &TestExponential, "tsformat/cnmdptst/TestExponential");
  addTest(root, &TestCurrencySign, "tsformat/cnmdptst/TestCurrencySign");
  addTest(root, &TestCurrency,  "tsformat/cnmdptst/TestCurrency");
  addTest(root, &TestRounding487, "tsformat/cnmdptst/TestRounding487");


}
/*Test Various format patterns*/
void TestPatterns()
{
  int32_t pat_length, i, lneed;
  UNumberFormat *fmt;
  UFieldPosition pos;
  UChar upat[5];
  UChar unewpat[5];
  UChar unum[5];
  UChar *unewp;
  UChar *str;
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
    }
}
/* Test the handling of quotes*/
void TestQuotes()
{
  int32_t lneed;
  UErrorCode status;
  UFieldPosition pos;
  UChar pat[15];
  UChar res[15];
  UChar *str;
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
void TestExponential()
{
  int32_t pat_length, val_length, lval_length;
  int32_t ival, ilval, p, v, lneed;
  UNumberFormat *fmt;
  UFieldPosition pos;
  int32_t ppos;
  UChar *upat;
  UChar pattern[20];
  UChar *str;
  UChar uvalfor[20], ulvalfor[20];
  double a;
  UErrorCode status = U_ZERO_ERROR;
  double val[] = { 0.01234, 123456789, 1.23e300, -3.141592653e-271 };
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
    0.01234, 123460000, 1.23E300, -3.1416E-271,
    0.01234, 123460000, 1.23E300, -3.1416E-271,
    0.01234, 123456800, 1.23E300, -3.141593E-271,
    0.01234, 123500000, 1.23E300, -3.142E-271,
  };
  int32_t lvalParse[] =
  {
    0, -1, 1, 123460000,
    0, -1, 1, 123460000,
    0, -1, 1, 123456800,
    0, -1, 1, 123500000,
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
void TestCurrencySign()
{
  int32_t lneed;
  UNumberFormat *fmt;
  UChar *pattern;
  UChar *str;
  UChar *pat;
  UChar *res;
  UFieldPosition pos;
  UErrorCode status = U_ZERO_ERROR;
  pattern=(UChar*)malloc(sizeof(UChar) * (strlen("\xA4#,##0.00;-\xA4#,##0.00") + 1) );
  u_uastrcpy(pattern, "\xA4#,##0.00;-\xA4#,##0.00");
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
void TestCurrency()
{
  UNumberFormat *currencyFmt;
  UChar *str, *res;
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
    }
}
/**
 * Test proper rounding by the format method.
 */
void TestRounding487()
{
  UNumberFormat *nnf;
  UErrorCode status = U_ZERO_ERROR;
  nnf = unum_open(UNUM_DEFAULT, NULL, &status);
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
  UChar *out;
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
}
