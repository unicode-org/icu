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
* File CNUMTST.C
*
*     Madhu Katragadda              Creation
*
* Modification History:
*
*   Date        Name        Description
*   06/24/99    helena      Integrated Alan's NF enhancements and Java2 bug fixes
*   07/15/99    helena      Ported to HPUX 10/11 CC.
*********************************************************************************
*/

/* C API TEST FOR NUMBER FORMAT */

#include "unicode/uloc.h"
#include "unicode/utypes.h"
#include "unicode/unum.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include "cnumtst.h"
#include<stdio.h>

void addNumForTest(TestNode** root)
{
    addTest(root, &TestNumberFormat, "tsformat/cnumtst/TestNumberFormat");
}
/* test Number Format API */
void TestNumberFormat()
{
    UChar *result;
    UChar *temp1;

    UChar temp[5];
    
    UChar prefix[5];
    UChar suffix[5];
    int32_t resultlength;
    int32_t resultlengthneeded;
    int32_t parsepos;
    double d1;
    int32_t l1;
    double d = -10456.37;
    int32_t l = 100000000;
    UFieldPosition pos1;
    UFieldPosition pos2;
    int32_t numlocales;

    UNumberFormatAttribute attr;
    UNumberFormatSymbols symbols1, symbols2;
    int32_t newvalue;  
    UErrorCode status=U_ZERO_ERROR;
    UNumberFormatStyle style= UNUM_DEFAULT;
    UNumberFormat *pattern;
    UNumberFormat *def, *fr, *cur_def, *cur_fr, *per_def, *per_fr, *spellout_def, *cur_frpattern;
    /* Testing unum_open() with various Numberformat styles and locales*/
    status = U_ZERO_ERROR;
    log_verbose("Testing  unum_open() with default style and locale\n");
    def=unum_open(style, NULL, &status);
    if(U_FAILURE(status))
        log_err("Error in creating NumberFormat default using unum_open(): %s\n", myErrorName(status));

    log_verbose("\nTesting unum_open() with french locale and default style(decimal)\n");
    fr=unum_open(style, "fr_FR", &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat (french): %s\n", myErrorName(status));

    log_verbose("\nTesting unum_open(currency,NULL,status)\n");
    style=UNUM_CURRENCY;
    /* Can't hardcode the result to assume the default locale is "en_US". */
	cur_def=unum_open(style, "en_US", &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using \n unum_open(currency, NULL, &status) %s\n",
                        myErrorName(status) );

    log_verbose("\nTesting unum_open(currency, frenchlocale, status)\n");
    cur_fr=unum_open(style, "fr_FR", &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(currency, french, &status): %s\n", 
                myErrorName(status));

    log_verbose("\nTesting unum_open(percent, NULL, status)\n");
    style=UNUM_PERCENT;
    per_def=unum_open(style, NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(percent, NULL, &status): %s\n", myErrorName(status));

    log_verbose("\nTesting unum_open(percent,frenchlocale, status)\n");
    per_fr=unum_open(style, "fr_FR", &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(percent, french, &status): %s\n", myErrorName(status));

    /*
    log_verbose("\nTesting unum_open(spellout, NULL, status)");
    style=UNUM_SPELLOUT;
    spellout_def=unum_open(style, NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(spellout, NULL, &status): %s\n", myErrorName(status));
    */
    
    
    /*Testing unum_getAvailable() and unum_countAvailable()*/ 
    log_verbose("\nTesting getAvailableLocales and countAvailable()\n");
    numlocales=unum_countAvailable();
    /* use something sensible w/o hardcoding the count */
    if(numlocales < 0)
        log_err("error in countAvailable");
    else{
        log_verbose("unum_countAvialable() successful\n");
        log_verbose("The no: of locales where number formattting is applicable is %d\n", numlocales);
    }
    /*for(i=0;i<numlocales;i++)
        log_verbose("%s\n", uloc_getName(unum_getAvailable(i))); */

    
    /*Testing unum_format() and unum_formatdouble()*/
    temp1=(UChar*)malloc(sizeof(UChar) * 25);
    u_uastrcpy(temp1, "$100,000,000.00");
    
    log_verbose("\nTesting unum_format() \n");
    resultlength=0;
    resultlengthneeded=unum_format(cur_def, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_format(cur_def, l, result, resultlength, &pos1, &status);
    }

    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
    }
    if(u_strcmp(result, temp1)==0)
        log_verbose("Pass: Number formatting using unum_foramt() successful\n");
    else
        log_err("Fail: Error in number Formatting using unum_foramt()\n");
    
free(result);    
    result = 0;

    log_verbose("\nTesting unum_formatDouble()\n");
    u_uastrcpy(temp1, "($10,456.37)");
    resultlength=0;
    resultlengthneeded=unum_formatDouble(cur_def, d, NULL, resultlength, &pos2, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_formatDouble(cur_def, d, result, resultlength, &pos2, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }
    if(u_strcmp(result, temp1)==0)
        log_verbose("Pass: Number Formatting using unum_formatDouble() Successful\n");
    else
        log_err("FAIL: Error in munber formatting using unum_formatDouble()\n");


    /* Testing unum_parse() and unum_parseDouble() */
    parsepos=0;
    log_verbose("\nTesting unum_parseDouble()\n");
    d1=unum_parseDouble(cur_def, result, u_strlen(result), &parsepos, &status);
    if(U_FAILURE(status))
    {
        log_err("parse failed. The error is  : %s\n", myErrorName(status));
    }

    if(d1!=d)
        log_err("Fail: Error in parsing\n");
    else
        log_verbose("Pass: parsing successful\n");

free(result);

    log_verbose("\nTesting unum_parse()\n");
    resultlength=0;
    parsepos=0;
    resultlengthneeded=unum_format(per_fr, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_format(per_fr, l, result, resultlength, &pos1, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }
    
    
    l1=unum_parse(per_fr, result, u_strlen(result), &parsepos, &status);
    if(U_FAILURE(status))
    {
        log_err("parse failed. The error is  : %s\n", myErrorName(status));
    }
    
    if(l1!=l)
        log_err("Fail: Error in parsing\n");
    else
        log_verbose("Pass: parsing successful\n");
    
free(result);    

    /* create a number format using unum_openPattern(....)*/
    log_verbose("\nTesting unum_openPattern()\n");
    u_uastrcpy(temp1, "#,##0.0#;(#,##0.0#)");
    pattern=unum_openPattern(temp1, u_strlen(temp1), NULL, &status);
    if(U_FAILURE(status))
    {
    log_err("error in unum_openPattern(): %s\n", myErrorName(status) );;
    }
    else
        log_verbose("Pass: unum_openPattern() works fine\n");
    
    /*test for unum_toPattern()*/
    log_verbose("\nTesting unum_toPattern()\n");
    resultlength=0;
    resultlengthneeded=unum_toPattern(pattern, FALSE, NULL, resultlength, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_toPattern(pattern, FALSE, result, resultlength, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("error in extracting the pattern from UNumberFormat: %s\n", myErrorName(status));
    }
    if(u_strcmp(result, temp1)!=0)
        log_err("FAIL: Error in extracting the pattern using unum_toPattern()\n");
    else
        log_verbose("Pass: extracted the pattern correctly using unum_toPattern()\n");

    

free(result);
free(temp1);
    
    /*Testing unum_getSymbols() and unum_setSymbols()*/
    log_verbose("\nTesting unum_getSymbols and unum_setSymbols()\n");
    /*when we try to change the symbols of french to default we need to apply the pattern as well to fetch correct results */
    resultlength=0;
    resultlengthneeded=unum_toPattern(cur_def, FALSE, NULL, resultlength, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_toPattern(cur_def, FALSE, result, resultlength, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("error in extracting the pattern from UNumberFormat: %s\n", myErrorName(status));
    }

    cur_frpattern=unum_openPattern(result, u_strlen(result), "fr_FR", &status);
    if(U_FAILURE(status))
    {
        log_err("error in unum_openPattern(): %s\n", myErrorName(status));
    }
    
free(result);

    /*getting the symbols of cur_def */
    unum_getSymbols(cur_def, &symbols1);
    
    /*format to check the result */
    resultlength=0;
    resultlengthneeded=unum_format(cur_def, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_format(cur_def, l, result, resultlength, &pos1, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }
    

    /*set the symbols of cur_frpattern to cur_def */
    unum_setSymbols(cur_frpattern, &symbols1, &status);
    if(U_FAILURE(status)){
        log_err("Fail: error in unum_setSymbols: %s\n", myErrorName(status));
    }

    unum_getSymbols(cur_frpattern, &symbols2);
    if((symbols1.decimalSeparator != symbols2.decimalSeparator) ||
       (symbols1.groupingSeparator != symbols2.groupingSeparator) ||
       (symbols1.patternSeparator != symbols2.patternSeparator) ||
       (symbols1.percent != symbols2.percent) ||
       (symbols1.zeroDigit != symbols2.zeroDigit) ||
       (symbols1.digit != symbols2.digit) ||
       (symbols1.minusSign != symbols2.minusSign) ||
       (symbols1.plusSign != symbols2.plusSign) ||
       (u_strcmp(symbols1.currency,symbols2.currency)!=0) ||
       (u_strcmp(symbols1.intlCurrency,symbols2.intlCurrency)!=0) ||
       (symbols1.monetarySeparator != symbols2.monetarySeparator) ||
       (symbols1.exponential != symbols2.exponential) ||
       (symbols1.perMill != symbols2.perMill) ||
       (symbols1.padEscape != symbols2.padEscape) ||
       (u_strcmp(symbols1.infinity,symbols2.infinity)!=0) ||
       (u_strcmp(symbols1.naN,symbols2.naN)!=0))
        log_err("Fail: error in setting and getting symbols\n");
    else
        log_verbose("Pass: get and set symbols successful\n");
    
    /*format and check with the previous result */

    resultlength=0;
    resultlengthneeded=unum_format(cur_frpattern, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        temp1=(UChar*)malloc(sizeof(UChar) * resultlength);
        unum_format(cur_frpattern, l, temp1, resultlength, &pos1, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }
    
    /*----------- */
    
free(result);
free(temp1);    
    
    /* Testing unum_getTextAttribute() and unum_setTextAttribute()*/
    log_verbose("\nTesting getting and setting text attributes\n");
    resultlength=5;
    unum_getTextAttribute(cur_fr, UNUM_NEGATIVE_SUFFIX, temp, resultlength, &status);
    if(U_FAILURE(status))
    {
        log_err("Failure in gettting the Text attributes of number format: %s\n", myErrorName(status));
    }
    unum_setTextAttribute(cur_def, UNUM_NEGATIVE_SUFFIX, temp, u_strlen(temp), &status);
    if(U_FAILURE(status))
    {
        log_err("Failure in gettting the Text attributes of number format: %s\n", myErrorName(status));
    }
    unum_getTextAttribute(cur_def, UNUM_NEGATIVE_SUFFIX, suffix, resultlength, &status);
    if(U_FAILURE(status))
    {
        log_err("Failure in gettting the Text attributes of number format: %s\n", myErrorName(status));
    }
    if(u_strcmp(suffix,temp)!=0)
        log_err("Fail:Error in setTextAttribute or getTextAttribute in setting and getting suffix\n");
    else
        log_verbose("Pass: setting and getting suffix works fine\n");
    /*set it back to normal */
    u_uastrcpy(temp,"$");
    unum_setTextAttribute(cur_def, UNUM_NEGATIVE_SUFFIX, temp, u_strlen(temp), &status);
    
    /*checking some more text setter conditions */
    u_uastrcpy(prefix, "+");
    unum_setTextAttribute(def, UNUM_POSITIVE_PREFIX, prefix, u_strlen(prefix) , &status);
    if(U_FAILURE(status))
    {
        log_err("error in setting the text attributes : %s\n", myErrorName(status));
    }
    unum_getTextAttribute(def, UNUM_POSITIVE_PREFIX, temp, resultlength, &status);
    if(U_FAILURE(status))
    {
        log_err("error in getting the text attributes : %s\n", myErrorName(status));
    }
    
    if(u_strcmp(prefix, temp)!=0) 
        log_err("ERROR: get and setTextAttributes with positive prefix failed\n");
    else
        log_verbose("Pass: get and setTextAttributes with positive prefix works fine\n");
    
    u_uastrcpy(prefix, "+");
    unum_setTextAttribute(def, UNUM_NEGATIVE_PREFIX, prefix, u_strlen(prefix), &status);
    if(U_FAILURE(status))
    {
        log_err("error in setting the text attributes : %s\n", myErrorName(status));
    }
    unum_getTextAttribute(def, UNUM_NEGATIVE_PREFIX, temp, resultlength, &status);
    if(U_FAILURE(status))
    {
        log_err("error in getting the text attributes : %s\n", myErrorName(status));
    }
    if(u_strcmp(prefix, temp)!=0) 
        log_err("ERROR: get and setTextAttributes with negative prefix failed\n");
    else
        log_verbose("Pass: get and setTextAttributes with negative prefix works fine\n");
 
    u_uastrcpy(suffix, "+");
    unum_setTextAttribute(def, UNUM_NEGATIVE_SUFFIX, suffix, u_strlen(suffix) , &status);
    if(U_FAILURE(status))
    {
        log_err("error in setting the text attributes: %s\n", myErrorName(status));
    }
    
    unum_getTextAttribute(def, UNUM_NEGATIVE_SUFFIX, temp, resultlength, &status);
    if(U_FAILURE(status))
    {
        log_err("error in getting the text attributes : %s\n", myErrorName(status));
    }
    if(u_strcmp(suffix, temp)!=0) 
        log_err("ERROR: get and setTextAttributes with negative suffix failed\n");
    else
        log_verbose("Pass: get and settextAttributes with negative suffix works fine\n");
    
    
    

    /*Testing unum_getAttribute and  unum_setAttribute() */
    log_verbose("\nTesting get and set Attributes\n");
    attr=UNUM_GROUPING_SIZE;
    newvalue=unum_getAttribute(def, attr);
    newvalue=2;
    unum_setAttribute(def, attr, newvalue);
    if(unum_getAttribute(def,attr)!=2)
        log_err("Fail: error in setting and getting attributes for UNUM_GROUPING_SIZE\n");
    else
        log_verbose("Pass: setting and getting attributes for UNUM_GROUPING_SIZE works fine\n");

    attr=UNUM_MULTIPLIER;
    newvalue=unum_getAttribute(def, attr);
    newvalue=8;
    unum_setAttribute(def, attr, newvalue);
    if(unum_getAttribute(def,attr) != 8)
        log_err("error in setting and getting attributes for UNUM_MULTIPLIER\n");
    else
        log_verbose("Pass:setting and getting attributes for UNUM_MULTIPLIER works fine\n");
    
    /*testing set and get Attributes extensively */
    log_verbose("\nTesting get and set attributes extensively\n");
    for(attr=UNUM_PARSE_INT_ONLY; attr<= UNUM_GROUPING_SIZE; attr=(UNumberFormatAttribute)((int32_t)attr + 1) ){
    newvalue=unum_getAttribute(fr, attr);
    unum_setAttribute(def, attr, newvalue);
    if(unum_getAttribute(def,attr)!=unum_getAttribute(fr, attr))
        log_err("error in setting and getting attributes\n");
    else
        log_verbose("Pass: attributes set and retrieved successfully\n");
    }

    
    /*closing the NumberFormat() using unum_close(UNumberFormat*)")*/
    unum_close(def);
    unum_close(fr);
    unum_close(cur_def);
    unum_close(cur_fr);
    unum_close(per_def);
    unum_close(per_fr);
    /*unum_close(spellout_def);*/
    unum_close(pattern);
    unum_close(cur_frpattern);
    
}
