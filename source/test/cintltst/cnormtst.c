/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CNORMTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API
*     synwee                      added test for quick check
*********************************************************************************/
/*tests for u_normalization*/
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "cnormtst.h"
#include "ccolltst.h"
#include "unicode/ustring.h"

#define ARRAY_LENGTH(array) (sizeof (array) / sizeof (*array))



static UCollator *myCollation;
const static char* canonTests[][3] = {
    /* Input*/                    /*Decomposed*/                /*Composed*/
    { "cat",                    "cat",                        "cat"                    },
    { "\\u00e0ardvark",            "a\\u0300ardvark",            "\\u00e0ardvark",        },

    { "\\u1e0a",                "D\\u0307",                    "\\u1e0a"                }, /* D-dot_above*/
    { "D\\u0307",                "D\\u0307",                    "\\u1e0a"                }, /* D dot_above*/
    
    { "\\u1e0c\\u0307",            "D\\u0323\\u0307",            "\\u1e0c\\u0307"        }, /* D-dot_below dot_above*/
    { "\\u1e0a\\u0323",            "D\\u0323\\u0307",            "\\u1e0c\\u0307"        }, /* D-dot_above dot_below */
    { "D\\u0307\\u0323",        "D\\u0323\\u0307",            "\\u1e0c\\u0307"        }, /* D dot_below dot_above */
    
    { "\\u1e10\\u0307\\u0323",    "D\\u0327\\u0323\\u0307",    "\\u1e10\\u0323\\u0307"    }, /*D dot_below cedilla dot_above*/
    { "D\\u0307\\u0328\\u0323",    "D\\u0328\\u0323\\u0307",    "\\u1e0c\\u0328\\u0307"    }, /* D dot_above ogonek dot_below*/

    { "\\u1E14",                "E\\u0304\\u0300",            "\\u1E14"                }, /* E-macron-grave*/
    { "\\u0112\\u0300",            "E\\u0304\\u0300",            "\\u1E14"                }, /* E-macron + grave*/
    { "\\u00c8\\u0304",            "E\\u0300\\u0304",            "\\u00c8\\u0304"        }, /* E-grave + macron*/
    
    { "\\u212b",                "A\\u030a",                    "\\u00c5"                }, /* angstrom_sign*/
    { "\\u00c5",                "A\\u030a",                    "\\u00c5"                }, /* A-ring*/
    
    { "\\u00C4ffin",            "A\\u0308ffin",                "\\u00C4ffin"                    },
    { "\\u00C4\\uFB03n",        "A\\u0308\\uFB03n",            "\\u00C4\\uFB03n"                },

    { "Henry IV",                "Henry IV",                    "Henry IV"                },
    { "Henry \\u2163",            "Henry \\u2163",            "Henry \\u2163"            },

    { "\\u30AC",                "\\u30AB\\u3099",            "\\u30AC"                }, /* ga (Katakana)*/
    { "\\u30AB\\u3099",            "\\u30AB\\u3099",            "\\u30AC"                }, /*ka + ten*/
    { "\\uFF76\\uFF9E",            "\\uFF76\\uFF9E",            "\\uFF76\\uFF9E"        }, /* hw_ka + hw_ten*/
    { "\\u30AB\\uFF9E",            "\\u30AB\\uFF9E",            "\\u30AB\\uFF9E"        }, /* ka + hw_ten*/
    { "\\uFF76\\u3099",            "\\uFF76\\u3099",            "\\uFF76\\u3099"        },  /* hw_ka + ten*/
    { "A\\u0300\\u0316",           "A\\u0316\\u0300",           "\\u00C0\\u0316"        }  /* hw_ka + ten*/
};

const static char* compatTests[][3] = {
    /* Input*/                        /*Decomposed    */                /*Composed*/
    { "cat",                        "cat",                            "cat"                },

    { "\\uFB4f",                    "\\u05D0\\u05DC",                "\\u05D0\\u05DC"    }, /* Alef-Lamed vs. Alef, Lamed*/

    { "\\u00C4ffin",                "A\\u0308ffin",                    "\\u00C4ffin"             },
    { "\\u00C4\\uFB03n",            "A\\u0308ffin",                    "\\u00C4ffin"                }, /* ffi ligature -> f + f + i*/

    { "Henry IV",                    "Henry IV",                        "Henry IV"            },
    { "Henry \\u2163",                "Henry IV",                        "Henry IV"            },

    { "\\u30AC",                    "\\u30AB\\u3099",                "\\u30AC"            }, /* ga (Katakana)*/
    { "\\u30AB\\u3099",                "\\u30AB\\u3099",                "\\u30AC"            }, /*ka + ten*/
    
    { "\\uFF76\\u3099",                "\\u30AB\\u3099",                "\\u30AC"            }, /* hw_ka + ten*/

    /*These two are broken in Unicode 2.1.2 but fixed in 2.1.5 and later*/
    { "\\uFF76\\uFF9E",                "\\u30AB\\u3099",                "\\u30AC"            }, /* hw_ka + hw_ten*/
    { "\\u30AB\\uFF9E",                "\\u30AB\\u3099",                "\\u30AC"            } /* ka + hw_ten*/
    
};


void addNormTest(TestNode** root)
{
    
    addTest(root, &TestDecomp, "tscoll/cnormtst/TestDecomp");
    addTest(root, &TestCompatDecomp, "tscoll/cnormtst/TestCompatDecomp");
    addTest(root, &TestCanonDecompCompose, "tscoll/cnormtst/TestCanonDecompCompose");
    addTest(root, &TestCompatDecompCompose, "tscoll/cnormtst/CompatDecompCompose");
    addTest(root, &TestNull, "tscoll/cnormtst/TestNull");
    addTest(root, &TestQuickCheck, "tscoll/cnormtst/TestQuickCheck");
}

void TestDecomp() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source=NULL, *result=NULL; 
    status = U_ZERO_ERROR;
    myCollation = ucol_open("en_US", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    resLen=0;
    log_verbose("Testing u_normalize with  Decomp canonical\n");
    for(x=0; x < ARRAY_LENGTH(canonTests); x++)
    {
        source=CharsToUChars(canonTests[x][0]);
        neededLen= u_normalize(source, u_strlen(source), UCOL_DECOMP_CAN, UCOL_IGNORE_HANGUL, NULL, 0, &status); 
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resLen=neededLen+1;
            result=(UChar*)malloc(sizeof(UChar*) * resLen);
            u_normalize(source, u_strlen(source), UCOL_DECOMP_CAN, UCOL_IGNORE_HANGUL, result, resLen, &status); 
        }
        if(U_FAILURE(status)){
            log_err("ERROR in u_normalize at %s:  %s\n", austrdup(source), myErrorName(status) );
        }
        assertEqual(result, canonTests[x][1], x);
        free(result);
        free(source);
    }
    ucol_close(myCollation);
}

void TestCompatDecomp() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source=NULL, *result=NULL; 
    status = U_ZERO_ERROR;
    myCollation = ucol_open("en_US", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    resLen=0;
    log_verbose("Testing u_normalize with  Decomp compat\n");
    for(x=0; x < ARRAY_LENGTH(compatTests); x++)
    {
        source=CharsToUChars(compatTests[x][0]);
        neededLen= u_normalize(source, u_strlen(source), UCOL_DECOMP_COMPAT, UCOL_IGNORE_HANGUL, NULL, 0, &status); 
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resLen=neededLen+1;
            result=(UChar*)malloc(sizeof(UChar*) * resLen);
            u_normalize(source, u_strlen(source), UCOL_DECOMP_COMPAT,UCOL_IGNORE_HANGUL, result, resLen, &status); 
        }
        if(U_FAILURE(status)){
            log_err("ERROR in u_normalize at %s:  %s\n", austrdup(source), myErrorName(status) );
        }
        assertEqual(result, compatTests[x][1], x);
        free(result);
        free(source);
    }
    ucol_close(myCollation);            
}

void TestCanonDecompCompose() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source=NULL, *result=NULL; 
    status = U_ZERO_ERROR;
    myCollation = ucol_open("en_US", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    resLen=0;
    log_verbose("Testing u_normalize with Decomp can compose compat\n");
    for(x=0; x < ARRAY_LENGTH(canonTests); x++)
    {
        source=CharsToUChars(canonTests[x][0]);
        neededLen= u_normalize(source, u_strlen(source), UCOL_DECOMP_CAN_COMP_COMPAT, UCOL_IGNORE_HANGUL, NULL, 0, &status); 
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resLen=neededLen+1;
            result=(UChar*)malloc(sizeof(UChar*) * resLen);
            u_normalize(source, u_strlen(source), UCOL_DECOMP_CAN_COMP_COMPAT, UCOL_IGNORE_HANGUL, result, resLen, &status); 
        }
        if(U_FAILURE(status)){
            log_err("ERROR in u_normalize at %s:  %s\n", austrdup(source),myErrorName(status) );
        }
        assertEqual(result, canonTests[x][2], x);
        free(result);
        free(source);
    }
    ucol_close(myCollation);            
}

void TestCompatDecompCompose() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source=NULL, *result=NULL;
    status = U_ZERO_ERROR;
    myCollation = ucol_open("en_US", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of rule based collator: %s\n", myErrorName(status));
    }
    resLen=0;
    log_verbose("Testing u_normalize with compat decomp compose can\n");
    for(x=0; x < ARRAY_LENGTH(compatTests); x++)
    {
        source=CharsToUChars(compatTests[x][0]);
        neededLen= u_normalize(source, u_strlen(source), UCOL_DECOMP_COMPAT_COMP_CAN, UCOL_IGNORE_HANGUL, NULL, 0, &status); 
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resLen=neededLen+1;
            result=(UChar*)malloc(sizeof(UChar*) * resLen);
            u_normalize(source, u_strlen(source), UCOL_DECOMP_COMPAT_COMP_CAN, UCOL_IGNORE_HANGUL, result, resLen, &status); 
        }
        if(U_FAILURE(status)){
            log_err("ERROR in u_normalize at %s:  %s\n", austrdup(source), myErrorName(status) );
        }
        assertEqual(result, compatTests[x][2], x);
        free(result);
        free(source);
    }
    ucol_close(myCollation);            
}


/*
static void assertEqual(const UChar* result, const UChar* expected, int32_t index)
{
    if(u_strcmp(result, expected)!=0){
        log_err("ERROR in decomposition at index = %d. EXPECTED: %s , GOT: %s\n", index, austrdup(expected),
            austrdup(result) );
    }
}
*/

static void assertEqual(const UChar* result, const char* expected, int32_t index)
{
    UChar *expectedUni = CharsToUChars(expected);
    if(u_strcmp(result, expectedUni)!=0){
        log_err("ERROR in decomposition at index = %d. EXPECTED: %s , GOT: %s\n", index, expected,
            austrdup(result) );
    }
    free(expectedUni);
}

static void TestNull_check(UChar *src, int32_t srcLen, 
                    UChar *exp, int32_t expLen,
                    UNormalizationMode mode,
                    const char *name)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t len, i;

    UChar   result[50];


    status = U_ZERO_ERROR;

    for(i=0;i<50;i++)
      {
        result[i] = 0xFFFD;
      }

    len = u_normalize(src, srcLen, mode, 0, result, 50, &status); 

    if(U_FAILURE(status)) {
      log_err("u_normalize(%s) with 0x0000 failed: %s\n", name, u_errorName(status));
    } else if (len != expLen) {
      log_err("u_normalize(%s) with 0x0000 failed: Expected len %d, got %d\n", name, expLen, len);
    } 

    {
      for(i=0;i<len;i++){
        if(exp[i] != result[i]) {
          log_err("u_normalize(%s): @%d, expected \\u%04X got \\u%04X\n",
                  name,
                  i,
                  exp[i],
                  result[i]);
          return;
        }
        log_verbose("     %d: \\u%04X\n", i, result[i]);
      }
    }
    
    log_verbose("u_normalize(%s) with 0x0000: OK\n", name);
}

void TestNull() 
{

    UChar   source_comp[] = { 0x0061, 0x0000, 0x0044, 0x0307 };
    int32_t source_comp_len = 4;
    UChar   expect_comp[] = { 0x0061, 0x0000, 0x1e0a };
    int32_t expect_comp_len = 3;

    UChar   source_dcmp[] = { 0x1e0A, 0x0000, 0x0929 };
    int32_t source_dcmp_len = 3;
    UChar   expect_dcmp[] = { 0x0044, 0x0307, 0x0000, 0x0928, 0x093C };
    int32_t expect_dcmp_len = 5;
    
    TestNull_check(source_comp,
                   source_comp_len,
                   expect_comp,
                   expect_comp_len,
                   UCOL_DECOMP_CAN_COMP_COMPAT,
                   "UCOL_DECOMP_CAN_COMP_COMPAT");

    TestNull_check(source_dcmp,
                   source_dcmp_len,
                   expect_dcmp,
                   expect_dcmp_len,
                   UCOL_DECOMP_CAN,
                   "UCOL_DECOMP_CAN");

    TestNull_check(source_comp,
                   source_comp_len,
                   expect_comp,
                   expect_comp_len,
                   UCOL_DECOMP_COMPAT_COMP_CAN,
                   "UCOL_DECOMP_COMPAT_COMP_CAN");


}

void TestQuickCheckResultNO() 
{
  const UChar CPNFD[] = {0x00C5, 0x0407, 0x1E00, 0x1F57, 0x220C, 
                         0x30AE, 0xAC00, 0xD7A3, 0xFB36, 0xFB4E};
  const UChar CPNFC[] = {0x0340, 0x0F93, 0x1F77, 0x1FBB, 0x1FEB, 
                          0x2000, 0x232A, 0xF900, 0xFA1E, 0xFB4E};
  const UChar CPNFKD[] = {0x00A0, 0x02E4, 0x1FDB, 0x24EA, 0x32FE, 
                           0xAC00, 0xFB4E, 0xFA10, 0xFF3F, 0xFA2D};
  const UChar CPNFKC[] = {0x00A0, 0x017F, 0x2000, 0x24EA, 0x32FE, 
                           0x33FE, 0xFB4E, 0xFA10, 0xFF3F, 0xFA2D};


  const int SIZE = 10;

  int count = 0;
  UErrorCode error = U_ZERO_ERROR;

  for (; count < SIZE; count ++)
  {
    if (u_quickCheck(&(CPNFD[count]), 1, UCOL_DECOMP_CAN, &error) != 
                                                              UQUICK_CHECK_NO)
    {
      log_err("ERROR in NFD quick check at code point %d", CPNFD[count]);
      return;
    }
    if (u_quickCheck(&(CPNFC[count]), 1, UCOL_DECOMP_CAN_COMP_COMPAT, &error) != 
                                                              UQUICK_CHECK_NO)
    {
      log_err("ERROR in NFC quick check at code point %d", CPNFC[count]);
      return;
    }
    if (u_quickCheck(&(CPNFKD[count]), 1, UCOL_DECOMP_COMPAT, &error) != 
                                                              UQUICK_CHECK_NO)
    {
      log_err("ERROR in NFKD quick check at code point %d", CPNFKD[count]);
      return;
    }
    if (u_quickCheck(&(CPNFKC[count]), 1, UCOL_DECOMP_COMPAT_COMP_CAN, &error) != 
                                                              UQUICK_CHECK_NO)
    {
      log_err("ERROR in NFKC quick check at code point %d", CPNFKC[count]);
      return;
    }
  }
}

 
void TestQuickCheckResultYES() 
{
  const UChar CPNFD[] = {0x00C6, 0x017F, 0x0F74, 0x1000, 0x1E9A, 
                         0x2261, 0x3075, 0x4000, 0x5000, 0xF000};
  const UChar CPNFC[] = {0x0400, 0x0540, 0x0901, 0x1000, 0x1500, 
                         0x1E9A, 0x3000, 0x4000, 0x5000, 0xF000};
  const UChar CPNFKD[] = {0x00AB, 0x02A0, 0x1000, 0x1027, 0x2FFB, 
                          0x3FFF, 0x4FFF, 0xA000, 0xF000, 0xFA27};
  const UChar CPNFKC[] = {0x00B0, 0x0100, 0x0200, 0x0A02, 0x1000, 
                          0x2010, 0x3030, 0x4000, 0xA000, 0xFA0E};

  const int SIZE = 10;
  int count = 0;
  UErrorCode error = U_ZERO_ERROR;

  UChar cp = 0;
  while (cp < 0xA0)
  {
    if (u_quickCheck(&cp, 1, UCOL_DECOMP_CAN, &error) != UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFD quick check at code point %d", cp);
      return;
    }
    if (u_quickCheck(&cp, 1, UCOL_DECOMP_CAN_COMP_COMPAT, &error) != 
                                                             UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFC quick check at code point %d", cp);
      return;
    }
    if (u_quickCheck(&cp, 1, UCOL_DECOMP_COMPAT, &error) != UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFKD quick check at code point %d", cp);
      return;
    }
    if (u_quickCheck(&cp, 1, UCOL_DECOMP_COMPAT_COMP_CAN, &error) != 
                                                             UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFKC quick check at code point %d", cp);
      return;
    }
    cp ++;
  }

  for (; count < SIZE; count ++)
  {
    if (u_quickCheck(&(CPNFD[count]), 1, UCOL_DECOMP_CAN, &error) != 
                                                             UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFD quick check at code point %d", CPNFD[count]);
      return;
    }
    if (u_quickCheck(&(CPNFC[count]), 1, UCOL_DECOMP_CAN_COMP_COMPAT, &error) 
                                                          != UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFC quick check at code point %d", CPNFC[count]);
      return;
    }
    if (u_quickCheck(&(CPNFKD[count]), 1, UCOL_DECOMP_COMPAT, &error) != 
                                                             UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFKD quick check at code point %d", CPNFKD[count]);
      return;
    }
    if (u_quickCheck(&(CPNFKC[count]), 1, UCOL_DECOMP_COMPAT_COMP_CAN, &error) != 
                                                             UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFKC quick check at code point %d", CPNFKC[count]);
      return;
    }
  }
}

void TestQuickCheckResultMAYBE() 
{
  const UChar CPNFD[] = {0x0220, 0x03F4, 0x0530, 0x0DE3, 0x1FFF, 
                         0x23AA, 0x3130, 0x9FA7, 0xD7A6, 0xFF00};
  const UChar CPNFC[] = {0x0306, 0x05B4, 0x0BBE, 0x102E, 0x1FFF, 
                         0x1175, 0x23AA, 0x3099, 0xD7A6, 0xFF00};
  const UChar CPNFKD[] = {0x0220, 0x03F4, 0x0530, 0x0DE3, 0x1FFF, 
                          0x23AA, 0x3130, 0x9FA7, 0xD7A6, 0xFF00};
  const UChar CPNFKC[] = {0x0300, 0x05B4, 0x0655, 0x09D7, 0x1FFF, 
                          0x23AA, 0x3099, 0x9FA7, 0xD7A6, 0xFF00};


  const int SIZE = 10;

  int count = 0;
  UErrorCode error = U_ZERO_ERROR;

  for (; count < SIZE; count ++)
  {
    if (u_quickCheck(&(CPNFD[count]), 1, UCOL_DECOMP_CAN, &error) != 
                                                           UQUICK_CHECK_MAYBE)
    {
      log_err("ERROR in NFD quick check at code point %d", CPNFD[count]);
      return;
    }
    if (u_quickCheck(&(CPNFC[count]), 1, UCOL_DECOMP_CAN_COMP_COMPAT, &error) != 
                                                           UQUICK_CHECK_MAYBE)
    {
      log_err("ERROR in NFC quick check at code point %d", CPNFC[count]);
      return;
    }
    if (u_quickCheck(&(CPNFKD[count]), 1, UCOL_DECOMP_COMPAT, &error) != 
                                                           UQUICK_CHECK_MAYBE)
    {
      log_err("ERROR in NFKD quick check at code point %d", CPNFKD[count]);
      return;
    }
    if (u_quickCheck(&(CPNFKC[count]), 1, UCOL_DECOMP_COMPAT_COMP_CAN, &error) != 
                                                           UQUICK_CHECK_MAYBE)
    {
      log_err("ERROR in NFKC quick check at code point %d", CPNFKC[count]);
      return;
    }
  }
}

void TestQuickCheckStringResult() 
{
  int count;
  UChar *d = NULL;
  UChar *c = NULL;
  UErrorCode error = U_ZERO_ERROR;

  for (count = 0; count < ARRAY_LENGTH(canonTests); count ++)
  {
    d = CharsToUChars(canonTests[count][1]);
    c = CharsToUChars(canonTests[count][2]);
    if (u_quickCheck(d, u_strlen(d), UCOL_DECOMP_CAN, &error) != 
                                                            UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFD quick check for string at count %d", count);
      return;
    }

    if (u_quickCheck(c, u_strlen(c), UCOL_DECOMP_CAN_COMP_COMPAT, &error) == 
                                                            UQUICK_CHECK_NO)
    {
      log_err("ERROR in NFC quick check for string at count %d", count);
      return;
    }

    free(d);
    free(c);
  }

  for (count = 0; count < ARRAY_LENGTH(compatTests); count ++)
  {
    d = CharsToUChars(compatTests[count][1]);
    c = CharsToUChars(compatTests[count][2]);
    if (u_quickCheck(d, u_strlen(d), UCOL_DECOMP_COMPAT, &error) != 
                                                            UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFKD quick check for string at count %d", count);
      return;
    }

    if (u_quickCheck(c, u_strlen(c), UCOL_DECOMP_COMPAT_COMP_CAN, &error) != 
                                                            UQUICK_CHECK_YES)
    {
      log_err("ERROR in NFKC quick check for string at count %d", count);
      return;
    }

    free(d);
    free(c);
  }  
}

void TestQuickCheck() 
{
  TestQuickCheckResultNO();
  TestQuickCheckResultYES();
  TestQuickCheckResultMAYBE();
  TestQuickCheckStringResult(); 
}

