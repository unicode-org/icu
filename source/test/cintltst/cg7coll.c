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
* File CG7COLL.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for CAPI
*********************************************************************************
/**
 * G7CollationTest is a third level test class.  This test performs the examples 
 * mentioned on the Taligent international demos web site.  
 * Sample Rules: & Z < p , P 
 * Effect :  Making P sort after Z.
 *
 * Sample Rules: & c < ch , cH, Ch, CH 
 * Effect : As well as adding sequences of characters that act as a single character (this is
 * known as contraction), you can also add characters that act like a sequence of
 * characters (this is known as expansion).  
 * 
 * Sample Rules: & Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&' 
 * Effect : Expansion and contraction can actually be combined.  
 * 
 * Sample Rules: & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-'
 * Effect : sorted sequence as the following,
 * aardvark  
 * a-rdvark  
 * abbot  
 * coop  
 * co-p  
 * cop 
 */
#include "utypes.h"
#include "ucol.h"
#include "uloc.h"
#include "cintltst.h"
#include "cg7coll.h"
#include "ccolltst.h"
#include "ustring.h"
#include <string.h>
#include <stdio.h>
#include <memory.h>


const char* locales[8] = {
        "en_US",
        "en_GB",
        "en_CA",
        "fr_FR",
        "fr_CA",
        "de_DE",
        "it_IT",
        "ja_JP"
};



const static UChar testCases[][MAX_TOKEN_LEN] = {
    { 0x0062 /*'b'*/, 0x006c /*'l'*/, 0x0061 /*'a'*/, 0x0062 /*'c'*/, 0x006b /*'k'*/, 
        0x002d /*'-'*/,  0x0062 /*'b'*/, 0x0069 /*'i'*/, 0x0072 /*'r'*/, 0x0064 /*'d'*/, 0x0073/*'s'*/, 0x0000},  /* 0 */
    { 0x0050 /*'P'*/, 0x0061 /*'a'*/, 0x0074/*'t'*/, 0x0000},                                                    /* 1 */
    { 0x0070 /*'p'*/, 0x00E9, 0x0063 /*'c'*/, 0x0068 /*'h'*/, 0x00E9, 0x0000},                                    /* 2 */
    { 0x0070 /*'p'*/, 0x00EA, 0x0063 /*'c'*/, 0x0068 /*'h'*/, 0x0065 /*'e'*/, 0x0000},                           /* 3 */
    { 0x0070 /*'p'*/, 0x00E9, 0x0063 /*'c'*/, 0x0068 /*'h'*/, 0x0065 /*'e'*/, 0x0072 /*'r'*/, 0x0000},            /* 4 */
    { 0x0070 /*'p'*/, 0x00EA, 0x0063 /*'c'*/, 0x0068 /*'h'*/, 0x0065 /*'e'*/, 0x0072 /*'r'*/, 0x0000},            /* 5 */
    { 0x0054 /*'T'*/, 0x006f /*'o'*/, 0x0064 /*'d'*/, 0x0000},                                                    /* 6 */
    { 0x0054 /*'T'*/, 0x00F6, 0x006e /*'n'*/, 0x0065 /*'e'*/, 0x0000},                                            /* 7 */
    { 0x0054 /*'T'*/, 0x006f /*'o'*/, 0x0066 /*'f'*/, 0x0075 /*'u'*/, 0x0000},                                   /* 8 */
    {  0x0062 /*'b'*/, 0x006c /*'l'*/, 0x0061 /*'a'*/, 0x0062 /*'c'*/, 0x006b /*'k'*/, 
        0x0062 /*'b'*/, 0x0069 /*'i'*/, 0x0072 /*'r'*/, 0x0064 /*'d'*/, 0x0073 /*'s'*/, 0x0000},                    /* 9 */
    { 0x0054 /*'T'*/, 0x006f /*'o'*/, 0x006e /*'n'*/, 0x0000},                                                    /* 10 */
    { 0x0050  /*'P'*/, 0x0041 /*'A'*/, 0x0054 /*'T'*/, 0x0000},                                                    /* 11 */
    { 0x0062 /*'b'*/, 0x006c /*'l'*/, 0x0061 /*'a'*/, 0x0062 /*'c'*/, 0x006b /*'k'*/, 
      0x0062  /*'b'*/, 0x0069 /*'i'*/, 0x0072 /*'r'*/, 0x0064 /*'d'*/, 0x0000},                                    /* 12 */
    { 0x0062 /*'b'*/, 0x006c /*'l'*/, 0x0061 /*'a'*/, 0x0062 /*'c'*/, 0x006b /*'k'*/, 
        0x002d /*'-'*/,  0x0062 /*'b'*/, 0x0069 /*'i'*/, 0x0072 /*'r'*/, 0x0064 /*'d'*/, 0x0000},                /* 13 */
    {0x0070 /*'p'*/, 0x0061 /*'a'*/, 0x0074 /*'t'*/, 0x0000},                                                    /* 14 */
    /* Additional tests */
    { 0x0063 /*'c'*/, 0x007a /*'z'*/, 0x0061 /*'a'*/, 0x0072 /*'r'*/, 0x0000 },                                 /* 15 */
    { 0x0063 /*'c'*/, 0x0068 /*'h'*/, 0x0075 /*'u'*/, 0x0072 /*'r'*/, 0x006f /*'o'*/, 0x0000 },                  /* 16 */
    { 0x0063 /*'c'*/, 0x0061 /*'a'*/, 0x0074 /*'t'*/, 0x000 },                                                    /* 17 */ 
    { 0x0064 /*'d'*/, 0x0061 /*'a'*/, 0x0072 /*'r'*/, 0x006e /*'n'*/, 0x0000 },                                 /* 18 */
    { 0x003f /*'?'*/, 0x0000 },                                                                                /* 19 */
    { 0x0071 /*'q'*/, 0x0075 /*'u'*/, 0x0069 /*'i'*/, 0x0063 /*'c'*/, 0x006b /*'k'*/, 0x0000 },                  /* 20 */
    { 0x0023 /*'#'*/, 0x0000 },                                                                                /* 21 */
    { 0x0026 /*'&'*/, 0x0000 },                                                                                /* 22 */
    { 0x0061 /*'a'*/, 0x0061 /*'a'*/, 0x0072 /*'r'*/, 0x0064 /*'d'*/, 0x0076 /*'v'*/, 0x0061 /*'a'*/, 
                0x0072/*'r'*/, 0x006b/*'k'*/, 0x0000},                                                        /* 23 */
    {  0x0061 /*'a'*/, 0x002d /*'-'*/, 0x0072 /*'r'*/, 0x0064 /*'d'*/, 0x0076 /*'v'*/, 0x0061 /*'a'*/, 
                0x0072/*'r'*/, 0x006b/*'k'*/, 0x0000},                                                        /* 24 */
    { 0x0061 /*'a'*/, 0x0062 /*'b'*/, 0x0062 /*'b'*/, 0x006f /*'o'*/, 0x0074 /*'t'*/, 0x0000},                   /* 25 */
    { 0x0063 /*'c'*/, 0x006f /*'o'*/, 0x006f /*'o'*/, 0x0070 /*'p'*/, 0x0000},                                 /* 26 */
    { 0x0063 /*'c'*/, 0x006f /*'o'*/, 0x002d /*'-'*/, 0x0070 /*'p'*/, 0x0000},                                 /* 27 */
    { 0x0063 /*'c'*/, 0x006f  /*'o'*/, 0x0070 /*'p'*/, 0x0000},                                                /* 28 */
    { 0x007a /*'z'*/, 0x0065  /*'e'*/, 0x0062 /*'b'*/, 0x0072 /*'r'*/, 0x0061 /*'a'*/, 0x0000}                    /* 29 */
};

const static int32_t results[TESTLOCALES][TOTALTESTSET] = {
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* en_US */
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* en_GB */
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* en_CA */
    { 12, 13, 9, 0, 14, 1, 11, 3, 2, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* fr_FR */
    { 12, 13, 9, 0, 14, 1, 11, 3, 2, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* fr_CA */
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* de_DE */
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* it_IT */
    { 12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, /* ja_JP */
    /* new table collation with rules "& Z < p, P"  loop to FIXEDTESTSET */
    { 12, 13, 9, 0, 6, 8, 10, 7, 14, 1, 11, 2, 3, 4, 5, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, 
    /* new table collation with rules "& C < ch , cH, Ch, CH " loop to TOTALTESTSET */
    { 19, 22, 21, 23, 25, 24, 12, 13, 9, 0, 17, 26, 28, 27, 15, 16, 18, 14, 1, 11, 2, 3, 4, 5, 20, 6, 8, 10, 7, 29 },
    /* new table collation with rules "& Question-mark ; ? & Hash-mark ; # & Ampersand ; '&'  " loop to TOTALTESTSET */
    { 23, 25, 22, 24, 12, 13, 9, 0, 17, 16, 26, 28, 27, 15, 18, 21, 14, 1, 11, 2, 3, 4, 5, 19, 20, 6, 8, 10, 7, 29 },
    /* analogous to Japanese rules " & aa ; a- & ee ; e- & ii ; i- & oo ; o- & uu ; u- " */  /* loop to TOTALTESTSET */
    { 19, 22, 21, 23, 24, 25, 12, 13, 9, 0, 17, 16, 26, 27, 28, 15, 18, 14, 1, 11, 2, 3, 4, 5, 20, 6, 8, 10, 7, 29 }
};
UChar* 
my_strncpy(UChar     *dst, 
     const UChar     *src, 
     int32_t     n) 
{
  UChar *anchor = dst;     /* save the start of result string */
  
  if (!n) return dst;
  while(n--)
      *dst++ = *src++;
  *dst = 0x0000;
  return anchor;
}

UChar* 
my_strcat(UChar     *dst, 
     const UChar     *src,
     int32_t n)
{
  UChar *anchor = dst;       /* save a pointer to start of dst */
  
  if (!n) return dst;

  dst += n;
  while (*src != 0x0000) 
      *dst++ = *src++;
  *dst = 0x0000;
  
  return anchor;
}
void addRuleBasedCollTest(TestNode** root)
{
    addTest(root, &TestG7Locales, "tscoll/cg7coll/TestG7Locales");
    addTest(root, &TestDemo1, "tscoll/cg7coll/TestDemo1");
    addTest(root, &TestDemo2, "tscoll/cg7coll/TestDemo2");
    addTest(root, &TestDemo3, "tscoll/cg7coll/TestDemo3");
    addTest(root, &TestDemo4, "tscoll/cg7coll/TestDemo4");

    
}
void doTest(UCollator *myCollation, const UChar source[], const UChar target[], UCollationResult result)
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

void TestG7Locales()
{
    UCollator *myCollation, *tblColl1;
    UErrorCode status = ZERO_ERROR;
    const UChar *defRules;
    int32_t i, rlen, j, n;
    log_verbose("Testing  ucol_openRules for all the locales\n");
    for (i = 0; i < 8; i++)
    {
        status = ZERO_ERROR;
        myCollation = ucol_open(locales[i], &status);
        if (FAILURE(status))
        {
            log_err("Error in creating collator in %s:  %s\n", locales[i], myErrorName(status));
            continue;
        }

        defRules = ucol_getRules(myCollation, &rlen);
        status = ZERO_ERROR;
        tblColl1 = ucol_openRules(defRules, rlen, UCOL_NO_NORMALIZATION, 
                   UCOL_DEFAULT_STRENGTH, &status);
        if (FAILURE(status))
        {
            
            log_err("Error in creating collator in %s:  %s\n", locales[i], myErrorName(status));
            continue;
        }

        
        log_verbose("Locale  %s\n", locales[i]);
        log_verbose("  tests start...\n");

        j = 0;
        n = 0;
        for (j = 0; j < FIXEDTESTSET; j++)
        {
            for (n = j+1; n < FIXEDTESTSET; n++)
            {
                doTest(tblColl1, testCases[results[i][j]], testCases[results[i][n]], UCOL_LESS);
            }
        }

        ucol_close(myCollation);
        ucol_close(tblColl1);
    }
}

void TestDemo1()
{
    UCollator *col, *myCollation;
    const UChar *baseRules;
    UChar *newRules, *temp;
    int32_t len, rlen, j, n;

    UErrorCode status = ZERO_ERROR;
    log_verbose("Demo Test 1 : Create a new table collation with rules \" & Z < p, P \" \n");
    status = ZERO_ERROR;
    col = ucol_open(NULL, &status);
    if(FAILURE(status)){
        log_err("Error in creation of Collator in Demo1  :%s\n", myErrorName(status));
        return;
    }

    baseRules = ucol_getRules(col, &rlen);
    temp=(UChar*)malloc(sizeof(UChar) * 15);
    u_uastrcpy(temp, "& Z < p, P");

    len=rlen + u_strlen(temp);
    newRules=(UChar*)malloc(sizeof(UChar) * (len+1));
    my_strncpy(newRules, baseRules, rlen);
    my_strcat(newRules, temp, rlen);
    myCollation = ucol_openRules(newRules, len, UCOL_NO_NORMALIZATION, 
                                                    UCOL_DEFAULT_STRENGTH, &status);

    if (FAILURE(status))
    {
        log_err( "Demo Test 1 Rule collation object creation failed. : %s\n", myErrorName(status));
        return;
    }

    j = 0;
    n = 0;
    for (j = 0; j < FIXEDTESTSET; j++)
    {
        for (n = j+1; n < FIXEDTESTSET; n++)
        {
            doTest(myCollation, testCases[results[8][j]], testCases[results[8][n]], UCOL_LESS);
        }
    }

    ucol_close(myCollation); 
    ucol_close(col);
    free(newRules);
}
void TestDemo2()
{
    UCollator *col, *myCollation;
    UErrorCode status = ZERO_ERROR;
    const UChar *baseRules;
    UChar *newRules, *temp;
    int32_t len, rlen, j, n;

    log_verbose("Demo Test 2 : Create a new table collation with rules \"& C < ch , cH, Ch, CH\"");
    status = ZERO_ERROR;
    col = ucol_open(NULL, &status);
    if(FAILURE(status)){
        log_err("Error in creation of Collator in Demo1  : %s\n", myErrorName(status));
        return;
    }
    baseRules = ucol_getRules(col, &rlen);
    temp=(UChar*)malloc(sizeof(UChar) * 70);
    u_uastrcpy(temp, "& C < ch , cH, Ch, CH");

    len=rlen + u_strlen(temp);
    newRules=(UChar*)malloc(sizeof(UChar) * (len+1));
    my_strncpy(newRules, baseRules, rlen);
    my_strcat(newRules, temp, rlen);
    myCollation = ucol_openRules(newRules, len, UCOL_NO_NORMALIZATION, 
                                                    UCOL_DEFAULT_STRENGTH, &status);

    if (FAILURE(status))
    {
        log_err( "Demo Test 2 Rule collation object creation failed.: %s\n", myErrorName(status));
        return;
    }
    j = 0;
    for (j; j < TOTALTESTSET; j++)
    {
        for (n = j+1; n < TOTALTESTSET; n++)
        {
            doTest(myCollation, testCases[results[9][j]], testCases[results[9][n]], UCOL_LESS);
        }
    }
    ucol_close(myCollation); 
    ucol_close(col);
    free(newRules);
    
}

void TestDemo3()
{
    UCollator *col, *myCollation;
    UErrorCode status = ZERO_ERROR;
    const UChar *baseRules;
    UChar *newRules, *temp;
    int32_t rlen, j, n, len;
    
    log_verbose("Demo Test 3 : Create a new table collation with rules \"& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'\" \n");
    status = ZERO_ERROR;
    col = ucol_open(NULL, &status);
    if(FAILURE(status)){
        log_err("Error in creation of Collator in Demo3  : %s\n", myErrorName(status));
        return;
    }
    baseRules = ucol_getRules(col, &rlen);
    temp=(UChar*)malloc(sizeof(UChar) * 70);
    u_uastrcpy(temp, "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'");
    
    len=rlen + u_strlen(temp);
    newRules=(UChar*)malloc(sizeof(UChar) * (len+1));
    my_strncpy(newRules, baseRules, rlen);
    my_strcat(newRules, temp, rlen);
    myCollation = ucol_openRules(newRules, len, UCOL_NO_NORMALIZATION, 
                                                    UCOL_DEFAULT_STRENGTH, &status);

    if (FAILURE(status))
    {
        log_err( "Demo Test 3 Rule collation object creation failed.: %s\n", myErrorName(status));
        return;
    }

    j = 0;
    for (j = 0; j < TOTALTESTSET; j++)
    {
        for (n = j+1; n < TOTALTESTSET; n++)
        {
            doTest(myCollation, testCases[results[10][j]], testCases[results[10][n]], UCOL_LESS);
        }
    }
    ucol_close(myCollation); 
    ucol_close(col);
    free(temp);
    free(newRules);
    
}

void TestDemo4()
{
    UCollator *col, *myCollation;
    UErrorCode status = ZERO_ERROR;
    const UChar *baseRules;
    UChar *newRules, *temp;
    int32_t rlen, j, n, len;
    
    log_verbose("Demo Test 4 : Create a new table collation with rules \" & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' \"\n");
    status = ZERO_ERROR;
    col = ucol_open(NULL, &status);
    if(FAILURE(status)){
        log_err("Error in creation of Collator in Demo1  : %s\n", myErrorName(status));
        return;
    }
    baseRules = ucol_getRules(col, &rlen);
    temp=(UChar*)malloc(sizeof(UChar) * 90);
    u_uastrcpy(temp, " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' ");
    
    len=rlen + u_strlen(temp);
    newRules=(UChar*)malloc(sizeof(UChar) * (len+1));
    my_strncpy(newRules, baseRules, rlen);
    my_strcat(newRules, temp, rlen);
    myCollation = ucol_openRules(newRules, len, UCOL_NO_NORMALIZATION, 
                                                    UCOL_DEFAULT_STRENGTH, &status);

    if (FAILURE(status))
    {
        log_err( "Demo Test 4 Rule collation object creation failed.: %s\n", myErrorName(status));
        return;
    }
    j;
    for (j = 0; j < TOTALTESTSET; j++)
    {
        for (n = j+1; n < TOTALTESTSET; n++)
        {
            doTest(myCollation, testCases[results[11][j]], testCases[results[11][n]], UCOL_LESS);
        }
    }
    ucol_close(myCollation); 
    ucol_close(col);
    free(temp);
    free(newRules);
}
