/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
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
#include "cmemory.h"

void addNumForTest(TestNode** root);

void addNumForTest(TestNode** root)
{
    addTest(root, &TestNumberFormat, "tsformat/cnumtst/TestNumberFormat");
    addTest(root, &TestNumberFormatPadding, "tsformat/cnumtst/TestNumberFormatPadding");
}

/* test Number Format API */
static void TestNumberFormat()
{
    UChar *result=NULL;
    UChar temp1[512];
    UChar temp2[512];

    UChar temp[5];

    UChar prefix[5];
    UChar suffix[5];
    UChar symbol[20];
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
    int32_t i;

    UNumberFormatAttribute attr;
    UNumberFormatSymbol symType = UNUM_DECIMAL_SEPARATOR_SYMBOL;
    int32_t newvalue;
    UErrorCode status=U_ZERO_ERROR;
    UNumberFormatStyle style= UNUM_DEFAULT;
    UNumberFormat *pattern;
    UNumberFormat *def, *fr, *cur_def, *cur_fr, *per_def, *per_fr,
                  *cur_frpattern, *myclone;
    /* Testing unum_open() with various Numberformat styles and locales*/
    status = U_ZERO_ERROR;
    log_verbose("Testing  unum_open() with default style and locale\n");
    def=unum_open(style, NULL,0,NULL, NULL,&status);
    if(U_FAILURE(status))
        log_err("Error in creating NumberFormat default using unum_open(): %s\n", myErrorName(status));

    log_verbose("\nTesting unum_open() with french locale and default style(decimal)\n");
    fr=unum_open(style,NULL,0, "fr_FR",NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat (french): %s\n", myErrorName(status));

    log_verbose("\nTesting unum_open(currency,NULL,status)\n");
    style=UNUM_CURRENCY;
    /* Can't hardcode the result to assume the default locale is "en_US". */
    cur_def=unum_open(style, NULL,0,"en_US", NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using \n unum_open(currency, NULL, &status) %s\n",
                        myErrorName(status) );

    log_verbose("\nTesting unum_open(currency, frenchlocale, status)\n");
    cur_fr=unum_open(style,NULL,0, "fr_FR", NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(currency, french, &status): %s\n", 
                myErrorName(status));

    log_verbose("\nTesting unum_open(percent, NULL, status)\n");
    style=UNUM_PERCENT;
    per_def=unum_open(style,NULL,0, NULL,NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(percent, NULL, &status): %s\n", myErrorName(status));

    log_verbose("\nTesting unum_open(percent,frenchlocale, status)\n");
    per_fr=unum_open(style, NULL,0,"fr_FR", NULL,&status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(percent, french, &status): %s\n", myErrorName(status));

    /*
    log_verbose("\nTesting unum_open(spellout, NULL, status)");
    style=UNUM_SPELLOUT;
    spellout_def=unum_open(style, NULL, &status);
    if(U_FAILURE(status))
        log_err("Error: could not create NumberFormat using unum_open(spellout, NULL, &status): %s\n", myErrorName(status));
    */
    /* Testing unum_clone(..) */
    log_verbose("\nTesting unum_clone(fmt, status)");
    status = U_ZERO_ERROR;
    myclone = unum_clone(def,&status);
    if(U_FAILURE(status))
        log_err("Error: could not clone unum_clone(def, &status): %s\n", myErrorName(status));
    else
    {
        log_verbose("unum_clone() successful\n");
    }

    /*Testing unum_getAvailable() and unum_countAvailable()*/
    log_verbose("\nTesting getAvailableLocales and countAvailable()\n");
    numlocales=unum_countAvailable();
    if(numlocales < 0)
        log_err("error in countAvailable");
    else{
        log_verbose("unum_countAvialable() successful\n");
        log_verbose("The no: of locales where number formattting is applicable is %d\n", numlocales);
    }
    for(i=0;i<numlocales;i++)
    {
        log_verbose("%s\n", unum_getAvailable(i));
        if (unum_getAvailable(i) == 0)
            log_err("No locale for which number formatting patterns are applicable\n");
        else
            log_verbose("A locale %s for which number formatting patterns are applicable\n",unum_getAvailable(i));
    }


    /*Testing unum_format() and unum_formatdouble()*/
    u_uastrcpy(temp1, "$100,000,000.00");

    log_verbose("\nTesting unum_format() \n");
    resultlength=0;
    pos1.field = 0; /* Integer Section */
    resultlengthneeded=unum_format(cur_def, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
/*        for (i = 0; i < 100000; i++) */
        {
            unum_format(cur_def, l, result, resultlength, &pos1, &status);
        }
    }

    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
    }
    if(u_strcmp(result, temp1)==0)
        log_verbose("Pass: Number formatting using unum_format() successful\n");
    else
        log_err("Fail: Error in number Formatting using unum_format()\n");
    if(pos1.beginIndex == 1 && pos1.endIndex == 12)
        log_verbose("Pass: Complete number formatting using unum_format() successful\n");
    else
        log_err("Fail: Error in complete number Formatting using unum_format()\nGot: b=%d end=%d\nExpected: b=1 end=12\n",
                pos1.beginIndex, pos1.endIndex);

uprv_free(result);
    result = 0;

    log_verbose("\nTesting unum_formatDouble()\n");
    u_uastrcpy(temp1, "($10,456.37)");
    resultlength=0;
    pos2.field = 1; /* Fractional Section */
    resultlengthneeded=unum_formatDouble(cur_def, d, NULL, resultlength, &pos2, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
/*        for (i = 0; i < 100000; i++) */
        {
            unum_formatDouble(cur_def, d, result, resultlength, &pos2, &status);
        }
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_formatDouble(.....): %s\n", myErrorName(status));
    }
    if(u_strcmp(result, temp1)==0)
        log_verbose("Pass: Number Formatting using unum_formatDouble() Successful\n");
    else
        log_err("FAIL: Error in number formatting using unum_formatDouble()\n");
    if(pos2.beginIndex == 9 && pos2.endIndex == 11)
        log_verbose("Pass: Complete number formatting using unum_format() successful\n");
    else
        log_err("Fail: Error in complete number Formatting using unum_formatDouble()\nGot: b=%d end=%d\nExpected: b=9 end=11",
                pos1.beginIndex, pos1.endIndex);


    /* Testing unum_parse() and unum_parseDouble() */
    log_verbose("\nTesting unum_parseDouble()\n");
/*    for (i = 0; i < 100000; i++)*/
    {
        parsepos=0;
        d1=unum_parseDouble(cur_def, result, u_strlen(result), &parsepos, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("parse failed. The error is  : %s\n", myErrorName(status));
    }

    if(d1!=d)
        log_err("Fail: Error in parsing\n");
    else
        log_verbose("Pass: parsing successful\n");

/* performance testing */
    u_uastrcpy(temp1, "$462.12345");
    resultlength=u_strlen(temp1);
/*    for (i = 0; i < 100000; i++) */
    {
        parsepos=0;
        d1=unum_parseDouble(cur_def, temp1, resultlength, &parsepos, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("parse failed. The error is  : %s\n", myErrorName(status));
    }

    if(d1!=462.12345)
        log_err("Fail: Error in parsing\n");
    else
        log_verbose("Pass: parsing successful\n");

uprv_free(result);

    u_uastrcpy(temp1, "($10,456.3E1])");
    parsepos=0;
    d1=unum_parseDouble(cur_def, temp1, u_strlen(temp1), &parsepos, &status);
    if(U_SUCCESS(status))
    {
        log_err("Error in unum_parseDouble(..., %s, ...): %s\n", temp1, myErrorName(status));
    }


    log_verbose("\nTesting unum_format()\n");
    status=U_ZERO_ERROR;
    resultlength=0;
    parsepos=0;
    resultlengthneeded=unum_format(per_fr, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
/*        for (i = 0; i < 100000; i++)*/
        {
            unum_format(per_fr, l, result, resultlength, &pos1, &status);
        }
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }


    log_verbose("\nTesting unum_parse()\n");
/*    for (i = 0; i < 100000; i++) */
    {
        parsepos=0;
        l1=unum_parse(per_fr, result, u_strlen(result), &parsepos, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("parse failed. The error is  : %s\n", myErrorName(status));
    }

    if(l1!=l)
        log_err("Fail: Error in parsing\n");
    else
        log_verbose("Pass: parsing successful\n");

uprv_free(result);

    /* create a number format using unum_openPattern(....)*/
    log_verbose("\nTesting unum_openPattern()\n");
    u_uastrcpy(temp1, "#,##0.0#;(#,##0.0#)");
    pattern=unum_open(UNUM_IGNORE,temp1, u_strlen(temp1), NULL, NULL,&status);
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
        result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
        unum_toPattern(pattern, FALSE, result, resultlength, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("error in extracting the pattern from UNumberFormat: %s\n", myErrorName(status));
    }
    else
    {
        if(u_strcmp(result, temp1)!=0)
            log_err("FAIL: Error in extracting the pattern using unum_toPattern()\n");
        else
            log_verbose("Pass: extracted the pattern correctly using unum_toPattern()\n");
uprv_free(result);
    }

    /*Testing unum_getSymbols() and unum_setSymbols()*/
    log_verbose("\nTesting unum_getSymbols and unum_setSymbols()\n");
    /*when we try to change the symbols of french to default we need to apply the pattern as well to fetch correct results */
    resultlength=0;
    resultlengthneeded=unum_toPattern(cur_def, FALSE, NULL, resultlength, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
        unum_toPattern(cur_def, FALSE, result, resultlength, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("error in extracting the pattern from UNumberFormat: %s\n", myErrorName(status));
    }

    status=U_ZERO_ERROR;
    cur_frpattern=unum_open(UNUM_IGNORE,result, u_strlen(result), "fr_FR",NULL, &status);
    if(U_FAILURE(status))
    {
        log_err("error in unum_openPattern(): %s\n", myErrorName(status));
    }

uprv_free(result);

    /*getting the symbols of cur_def */
    /*set the symbols of cur_frpattern to cur_def */
    for (symType = UNUM_DECIMAL_SEPARATOR_SYMBOL; symType < UNUM_FORMAT_SYMBOL_COUNT; symType++) {
        status=U_ZERO_ERROR;
        unum_getSymbol(cur_def, symType, temp1, sizeof(temp1), &status);
        unum_setSymbol(cur_frpattern, symType, temp1, -1, &status);
        if(U_FAILURE(status))
        {
            log_err("Error in get/set symbols: %s\n", myErrorName(status));
        }
    }

    /*format to check the result */
    resultlength=0;
    resultlengthneeded=unum_format(cur_def, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
        unum_format(cur_def, l, result, resultlength, &pos1, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }

    if(U_FAILURE(status)){
        log_err("Fail: error in unum_setSymbols: %s\n", myErrorName(status));
    }
    unum_applyPattern(cur_frpattern, FALSE, result, u_strlen(result),NULL,NULL);

    for (symType = UNUM_DECIMAL_SEPARATOR_SYMBOL; symType < UNUM_FORMAT_SYMBOL_COUNT; symType++) {
        status=U_ZERO_ERROR;
        unum_getSymbol(cur_def, symType, temp1, sizeof(temp1), &status);
        unum_getSymbol(cur_frpattern, symType, temp2, sizeof(temp2), &status);
        if(U_FAILURE(status) || u_strcmp(temp1, temp2) != 0)
        {
            log_err("Fail: error in getting symbols\n");
        }
        else
            log_verbose("Pass: get and set symbols successful\n");
    }

    /*format and check with the previous result */

    resultlength=0;
    resultlengthneeded=unum_format(cur_frpattern, l, NULL, resultlength, &pos1, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        unum_format(cur_frpattern, l, temp1, resultlength, &pos1, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status));
    }
    /* TODO: 
     * This test fails because we have not called unum_applyPattern().
     * Currently, such an applyPattern() does not exist on the C API, and
     * we have jitterbug 411 for it.
     * Since it is close to the 1.5 release, I (markus) am disabling this test just
     * for this release (I added the test itself only last week).
     * For the next release, we need to fix this.
     * Then, remove the uprv_strcmp("1.5", ...) and this comment, and the include "cstring.h" at the beginning of this file.
     */
    if(u_strcmp(result, temp1) != 0) {
        log_err("Formatting failed after setting symbols. result=%s temp1=%s\n", result, temp1);
    }


    /*----------- */

uprv_free(result);

    /* Testing unum_get/setSymbol() */
    for(i = 0; i < UNUM_FORMAT_SYMBOL_COUNT; ++i) {
        symbol[0] = (UChar)(0x41 + i);
        symbol[1] = (UChar)(0x61 + i);
        unum_setSymbol(cur_frpattern, (UNumberFormatSymbol)i, symbol, 2, &status);
        if(U_FAILURE(status)) {
            log_err("Error from unum_setSymbol(%d): %s\n", i, myErrorName(status));
            return;
        }
    }
    for(i = 0; i < UNUM_FORMAT_SYMBOL_COUNT; ++i) {
        resultlength = unum_getSymbol(cur_frpattern, (UNumberFormatSymbol)i, symbol, sizeof(symbol)/U_SIZEOF_UCHAR, &status);
        if(U_FAILURE(status)) {
            log_err("Error from unum_getSymbol(%d): %s\n", i, myErrorName(status));
            return;
        }
        if(resultlength != 2 || symbol[0] != 0x41 + i || symbol[1] != 0x61 + i) {
            log_err("Failure in unum_getSymbol(%d): got unexpected symbol\n", i);
        }
    }
    /*try getting from a bogus symbol*/
    unum_getSymbol(cur_frpattern, (UNumberFormatSymbol)i, symbol, sizeof(symbol)/U_SIZEOF_UCHAR, &status);
    if(U_SUCCESS(status)){
        log_err("Error : Expected U_ILLEGAL_ARGUMENT_ERROR for bogus symbol");
    }
    if(U_FAILURE(status)){
        if(status != U_ILLEGAL_ARGUMENT_ERROR){
            log_err("Error: Expected U_ILLEGAL_ARGUMENT_ERROR for bogus symbol, Got %s\n", myErrorName(status));
        }
    }
    status=U_ZERO_ERROR;

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

    u_uastrcpy(suffix, "++");
    unum_setTextAttribute(def, UNUM_POSITIVE_SUFFIX, suffix, u_strlen(suffix) , &status);
    if(U_FAILURE(status))
    {
        log_err("error in setting the text attributes: %s\n", myErrorName(status));
    }

    unum_getTextAttribute(def, UNUM_POSITIVE_SUFFIX, temp, resultlength, &status);
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
    for(attr=UNUM_PARSE_INT_ONLY; attr<= UNUM_PADDING_POSITION; attr=(UNumberFormatAttribute)((int32_t)attr + 1) )
    {
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
    unum_close(myclone);

}

static void TestNumberFormatPadding()
{
    UChar *result=NULL;
    UChar temp1[512];

    UErrorCode status=U_ZERO_ERROR;
    int32_t resultlength;
    int32_t resultlengthneeded;
    UNumberFormat *pattern;
    double d1;
    double d = -10456.37;
    UFieldPosition pos1;
    int32_t parsepos;

    /* create a number format using unum_openPattern(....)*/
    log_verbose("\nTesting unum_openPattern() with padding\n");
    u_uastrcpy(temp1, "*#,##0.0#*;(#,##0.0#)");
    status=U_ZERO_ERROR;
    pattern=unum_open(UNUM_IGNORE,temp1, u_strlen(temp1), NULL, NULL,&status);
    if(U_SUCCESS(status))
    {
        log_err("error in unum_openPattern(%s): %s\n", temp1, myErrorName(status) );;
    }
    else
    {
        unum_close(pattern);
    }

/*    u_uastrcpy(temp1, "*x#,###,###,##0.0#;(*x#,###,###,##0.0#)"); */
    u_uastrcpy(temp1, "*x#,###,###,##0.0#;*x(###,###,##0.0#)");
    status=U_ZERO_ERROR;
    pattern=unum_open(UNUM_IGNORE,temp1, u_strlen(temp1), "en_US",NULL, &status);
    if(U_FAILURE(status))
    {
        log_err("error in padding unum_openPattern(%s): %s\n", temp1, myErrorName(status) );;
    }
    else {
        log_verbose("Pass: padding unum_openPattern() works fine\n");

        /*test for unum_toPattern()*/
        log_verbose("\nTesting padding unum_toPattern()\n");
        resultlength=0;
        resultlengthneeded=unum_toPattern(pattern, FALSE, NULL, resultlength, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resultlength=resultlengthneeded+1;
            result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
            unum_toPattern(pattern, FALSE, result, resultlength, &status);
        }
        if(U_FAILURE(status))
        {
            log_err("error in extracting the padding pattern from UNumberFormat: %s\n", myErrorName(status));
        }
        else
        {
            if(u_strcmp(result, temp1)!=0)
                log_err("FAIL: Error in extracting the padding pattern using unum_toPattern()\n");
            else
                log_verbose("Pass: extracted the padding pattern correctly using unum_toPattern()\n");
uprv_free(result);
        }
/*        u_uastrcpy(temp1, "(xxxxxxx10,456.37)"); */
        u_uastrcpy(temp1, "xxxxx(10,456.37)");
        resultlength=0;
        pos1.field = 1; /* Fraction field */
        resultlengthneeded=unum_formatDouble(pattern, d, NULL, resultlength, &pos1, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resultlength=resultlengthneeded+1;
            result=(UChar*)uprv_malloc(sizeof(UChar) * resultlength);
            unum_formatDouble(pattern, d, result, resultlength, NULL, &status);
        }
        if(U_FAILURE(status))
        {
            log_err("Error in formatting using unum_formatDouble(.....) with padding : %s\n", myErrorName(status));
        }
        else
        {
            if(u_strcmp(result, temp1)==0)
                log_verbose("Pass: Number Formatting using unum_formatDouble() padding Successful\n");
            else
                log_err("FAIL: Error in number formatting using unum_formatDouble() with padding\n");
            if(pos1.beginIndex == 13 && pos1.endIndex == 15)
                log_verbose("Pass: Complete number formatting using unum_formatDouble() successful\n");
            else
                log_err("Fail: Error in complete number Formatting using unum_formatDouble()\nGot: b=%d end=%d\nExpected: b=13 end=15\n",
                        pos1.beginIndex, pos1.endIndex);


            /* Testing unum_parse() and unum_parseDouble() */
            log_verbose("\nTesting padding unum_parseDouble()\n");
            parsepos=0;
            d1=unum_parseDouble(pattern, result, u_strlen(result), &parsepos, &status);
            if(U_FAILURE(status))
            {
                log_err("padding parse failed. The error is : %s\n", myErrorName(status));
            }

            if(d1!=d)
                log_err("Fail: Error in padding parsing\n");
            else
                log_verbose("Pass: padding parsing successful\n");
uprv_free(result);
        }
    }

    unum_close(pattern);
}

