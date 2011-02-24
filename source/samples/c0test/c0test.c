/*
**********************************************************************
* Copyright (C) 1998-2011, International Business Machines Corporation
* and others.  All Rights Reserved.
**********************************************************************
*
* File date.c
*
* Modification History:
*
*   Date        Name        Description
*   2011-Jan-16 srl         Created.
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uloc.h"
#include "unicode/unorm2.h"
#include "unicode/unorm.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_cb.h"

#include <stdio.h>
#define log_data_err printf
#define log_verbose printf
#define log_err printf
#define ERRMSG printf("%s:%d: error=%s\n", __FILE__,__LINE__,u_errorName(error))
#if UCONFIG_NO_NORMALIZATION
#error UCONFIG_NO_NORMALIZATION set
#endif

#ifndef LENGTHOF
#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))
#endif

static const char* res2str(UNormalizationCheckResult res) {
  switch(res) {
  case UNORM_YES: return "YES";
  case UNORM_NO: return "NO";
  case UNORM_MAYBE: return "MAYBE";
  default: return "?";
  }
}


static void TestQuickCheckResultNO() 
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
    /* if (unorm_quickCheck(&(CPNFD[count]), 1, UNORM_NFD, &error) !=  */
    /*                                                           UNORM_NO) */
    /* { */
    /*   log_err("ERROR in NFD quick check at U+%04x\n", CPNFD[count]); */
    /*   return; */
    /* } */
    if (unorm_quickCheck(&(CPNFC[count]), 1, UNORM_NFC, &error) != 
                                                              UNORM_NO)
    {
      log_err("ERROR in NFC quick check at U+%04x\n", CPNFC[count]);
      return;
    }
    /* if (unorm_quickCheck(&(CPNFKD[count]), 1, UNORM_NFKD, &error) !=  */
    /*                                                           UNORM_NO) */
    /* { */
    /*   log_err("ERROR in NFKD quick check at U+%04x\n", CPNFKD[count]); */
    /*   return; */
    /* } */
    /* if (unorm_quickCheck(&(CPNFKC[count]), 1, UNORM_NFKC, &error) !=  */
    /*                                                           UNORM_NO) */
    /* { */
    /*   log_err("ERROR in NFKC quick check at U+%04x\n", CPNFKC[count]); */
    /*   return; */
    /* } */
  }
}

 
static void TestQuickCheckResultYES() 
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
    /* if (unorm_quickCheck(&cp, 1, UNORM_NFD, &error) != UNORM_YES) */
    /* { */
    /*   ERRMSG; */
    /*   log_data_err("ERROR in NFD quick check at U+%04x - (Are you missing data?)\n", cp); */
    /*   return; */
    /* } */
    if (unorm_quickCheck(&cp, 1, UNORM_NFC, &error) != 
                                                             UNORM_YES)
    {
      ERRMSG;
      log_err("ERROR in NFC quick check at U+%04x\n", cp);
      return;
    }
    /* if (unorm_quickCheck(&cp, 1, UNORM_NFKD, &error) != UNORM_YES) */
    /* { */
    /*   ERRMSG; */
    /*   log_err("ERROR in NFKD quick check at U+%04x\n", cp); */
    /*   return; */
    /* } */
    /* if (unorm_quickCheck(&cp, 1, UNORM_NFKC, &error) !=  */
    /*                                                          UNORM_YES) */
    /* { */
    /*   ERRMSG; */
    /*   log_err("ERROR in NFKC quick check at U+%04x\n", cp); */
    /*   return; */
    /* } */
    cp ++;
  }

  for (; count < SIZE; count ++)
  {
    /* if (unorm_quickCheck(&(CPNFD[count]), 1, UNORM_NFD, &error) !=  */
    /*                                                          UNORM_YES) */
    /* { */
    /*   ERRMSG; */
    /*   log_err("ERROR in NFD quick check at U+%04x\n", CPNFD[count]); */
    /*   return; */
    /* } */
    if (unorm_quickCheck(&(CPNFC[count]), 1, UNORM_NFC, &error) 
                                                          != UNORM_YES)
    {
      ERRMSG;
      log_err("ERROR in NFC quick check at U+%04x\n", CPNFC[count]);
      return;
    }
    /* if (unorm_quickCheck(&(CPNFKD[count]), 1, UNORM_NFKD, &error) !=  */
    /*                                                          UNORM_YES) */
    /* { */
    /*   ERRMSG; */
    /*   log_err("ERROR in NFKD quick check at U+%04x\n", CPNFKD[count]); */
    /*   return; */
    /* } */
    /* if (unorm_quickCheck(&(CPNFKC[count]), 1, UNORM_NFKC, &error) !=  */
    /*                                                          UNORM_YES) */
    /* { */
    /*   ERRMSG; */
    /*   log_err("ERROR in NFKC quick check at U+%04x\n", CPNFKC[count]); */
    /*   return; */
    /* } */
  }
}

static void TestQuickCheckResultMAYBE() 
{
  const UChar CPNFC[] = {0x0306, 0x0654, 0x0BBE, 0x102E, 0x1161, 
                         0x116A, 0x1173, 0x1175, 0x3099, 0x309A};
  const UChar CPNFKC[] = {0x0300, 0x0654, 0x0655, 0x09D7, 0x0B3E, 
                          0x0DCF, 0xDDF, 0x102E, 0x11A8, 0x3099};


  const int SIZE = 10;

  int count = 0;
  UErrorCode error = U_ZERO_ERROR;

  /* NFD and NFKD does not have any MAYBE codepoints */
  for (; count < SIZE; count ++)
  {
    UNormalizationCheckResult res;
    if ((res=unorm_quickCheck(&(CPNFC[count]), 1, UNORM_NFC, &error)) != 
                                                           UNORM_MAYBE)
    {
      ERRMSG;
      log_data_err("ERROR in NFC quick check at U+%04x - %s wanted MAYBE\n", CPNFC[count], res2str(res));
           /* return; */
    }
    /* if (unorm_quickCheck(&(CPNFKC[count]), 1, UNORM_NFKC, &error) !=  */
    /*                                                        UNORM_MAYBE) */
    /* { */
    /*   ERRMSG; */
    /*   log_err("ERROR in NFKC quick check at U+%04x\n", CPNFKC[count]); */
    /*   return; */
    /* } */
  }
}

UChar* CharsToUChars(const char* str) {
    /* Might be faster to just use uprv_strlen() as the preflight len - liu */
    int32_t len = u_unescape(str, 0, 0); /* preflight */
    /* Do NOT use malloc() - we are supposed to be acting like user code! */
    UChar *buf = (UChar*) malloc(sizeof(UChar) * (len + 1));
    u_unescape(str, buf, len + 1);
    return buf;
}

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

char *austrdup(const UChar* unichars)
{
    int   length;
    char *newString;

    length    = u_strlen ( unichars );
    /*newString = (char*)malloc  ( sizeof( char ) * 4 * ( length + 1 ) );*/ /* this leaks for now */
    newString = (char*)malloc  ( sizeof( char ) * 4 * ( length + 1 ) ); /* this shouldn't */

    if ( newString == NULL )
        return NULL;

    u_austrcpy ( newString, unichars );

    return newString;
}

char *aescstrdup(const UChar* unichars,int32_t length){
    char *newString,*targetLimit,*target;
    UConverterFromUCallback cb;
    const void *p;
    UErrorCode errorCode = U_ZERO_ERROR;
#if U_CHARSET_FAMILY==U_EBCDIC_FAMILY
#   ifdef OS390
        static const char convName[] = "ibm-1047";
#   else
        static const char convName[] = "ibm-37";
#   endif
#else
    static const char convName[] = "US-ASCII";
#endif
    UConverter* conv = ucnv_open(convName, &errorCode);
    if(length==-1){
        length = u_strlen( unichars);
    }
    newString = (char*)malloc ( sizeof(char) * 8 * (length +1));
    target = newString;
    targetLimit = newString+sizeof(char) * 8 * (length +1);
    ucnv_setFromUCallBack(conv, UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_C, &cb, &p, &errorCode);
    ucnv_fromUnicode(conv,&target,targetLimit, &unichars, (UChar*)(unichars+length),NULL,TRUE,&errorCode);
    ucnv_close(conv);
    *target = '\0';
    return newString;
}

static void assertEqual(const UChar* result, const char* expected, int32_t index)
{
    UChar *expectedUni = CharsToUChars(expected);
    if(u_strcmp(result, expectedUni)!=0){
        log_err("ERROR in index = %d. EXPECTED: %s , GOT: %s\n", index, expected,
            austrdup(result) );
    }
    free(expectedUni);
}

void TestCanonDecompCompose() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source=NULL, *result=NULL; 
    status = U_ZERO_ERROR;
    resLen=0;
    log_verbose("Testing unorm_normalize with Decomp can compose compat\n");
    for(x=0; x < LENGTHOF(canonTests); x++)
    {
        source=CharsToUChars(canonTests[x][0]);
        neededLen= unorm_normalize(source, u_strlen(source), UNORM_NFC, 0, NULL, 0, &status); 
        if(status==U_BUFFER_OVERFLOW_ERROR)
        {
            status=U_ZERO_ERROR;
            resLen=neededLen+1;
            result=(UChar*)malloc(sizeof(UChar*) * resLen);
            unorm_normalize(source, u_strlen(source), UNORM_NFC, 0, result, resLen, &status); 
            if(U_FAILURE(status)){
              log_data_err("ERROR in unorm_normalize at %s:  %s - (Are you missing data?)\n", austrdup(source),u_errorName(status) );
            } else {
              assertEqual(result, canonTests[x][2], x);
            }
            free(result);
        } else {
          log_data_err("ERROR in normalization of %s: status was %s should be U_BUFFER_OVERFLOW_ERROR, neededLen %d, len was %d\n", 
                       austrdup(source), u_errorName(status), neededLen, u_strlen(source));
        }
        free(source);
    }
}


static void TestQuickCheckStringResult() 
{
#if 1
  int count;
  UChar *d = NULL;
  UChar *c = NULL;
  UErrorCode error = U_ZERO_ERROR;

  for (count = 0; count < LENGTHOF(canonTests); count ++)
  {
    UNormalizationCheckResult res;

    d = CharsToUChars(canonTests[count][1]);
    c = CharsToUChars(canonTests[count][2]);
    /* if (unorm_quickCheck(d, u_strlen(d), UNORM_NFD, &error) !=  */
    /*                                                         UNORM_YES) */
    /* { */
    /*   log_data_err("ERROR in NFD quick check for string at count %d - (Are you missing data?)\n", count); */
    /*   return; */
    /* } */

    if ((res=unorm_quickCheck(c, u_strlen(c), UNORM_NFC, &error)) == 
                                                            UNORM_NO)
    {
      log_err("ERROR in NFC quick check for string at count %d, got %s wanted %s\n", count, res2str(res), res2str(UNORM_NO));
      return;
    }

    free(d);
    free(c);
    /* printf("OK: %s = NO \n", canonTests[count][2]); */
  }

  /* for (count = 0; count < LENGTHOF(compatTests); count ++) */
  /* { */
  /*   d = CharsToUChars(compatTests[count][1]); */
  /*   c = CharsToUChars(compatTests[count][2]); */
  /*   if (unorm_quickCheck(d, u_strlen(d), UNORM_NFKD, &error) !=  */
  /*                                                           UNORM_YES) */
  /*   { */
  /*     log_err("ERROR in NFKD quick check for string at count %d\n", count); */
  /*     return; */
  /*   } */

  /*   if (unorm_quickCheck(c, u_strlen(c), UNORM_NFKC, &error) !=  */
  /*                                                           UNORM_YES) */
  /*   { */
  /*     log_err("ERROR in NFKC quick check for string at count %d\n", count); */
  /*     return; */
  /*   } */

  /*   free(d); */
  /*   free(c); */
  /* }   */
#endif
}

void TestQuickCheck() 
{
  TestQuickCheckResultNO();
  TestQuickCheckResultYES();
  TestQuickCheckResultMAYBE();
  TestQuickCheckStringResult(); 
}


int main()
{
  char *dl = NULL;
  UErrorCode status = U_ZERO_ERROR;

#if 0
  dl = uloc_getDefault();

  printf("Default Name: %s\n", dl!=NULL?dl:"<NULL>");
  printf("u_iscntrl(U+%04X)=%d\n", 0x0009, u_iscntrl(0x0009));
  printf("u_iscntrl(U+%04X)=%d\n", 0x0020, u_iscntrl(0x0020));
  printf("u_tolower(U+%04X)=U+%04X\n", 0x2C1F, u_tolower(0x2C1F));
  printf("u_tolower(U+%04X)=U+%04X\n", 0xA65C, u_tolower(0xA65C));

  printf("Testing QuickCheck..");
  fflush(stdout);
  TestQuickCheck();

  printf("Testing norm instance..");
  fflush(stdout);
  /* norm */
  {
    UErrorCode errorCode;
    const UNormalizer2 *norm2;
    int length;
    UChar buffer16[300];
    UChar source[50];
    
    /*
     * Test for an example that unorm_getCanonStartSet() delivers
     * all characters that compose from the input one,
     * even in multiple steps.
     * For example, the set for "I" (0049) should contain both
     * I-diaeresis (00CF) and I-diaeresis-acute (1E2E).
     * In general, the set for the middle such character should be a subset
     * of the set for the first.
     */
    errorCode=U_ZERO_ERROR;
    norm2=unorm2_getInstance(NULL, "nfc", UNORM2_COMPOSE, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_data_err("unorm2_getInstance(NFC) failed - %s\n", u_errorName(errorCode));
        return -1;
    }

    length=unorm2_normalize(norm2, source, 1, buffer16, LENGTHOF(buffer16), &errorCode);

    unorm2_close(norm2);
  }

#endif

  TestCanonDecompCompose();

#if 0
  {
    UNormalizationCheckResult res;
    UChar cpnfc = 0x0306;
    UErrorCode error = U_ZERO_ERROR;
    
    res = unorm_quickCheck(&cpnfc, 1, UNORM_NFC, &error);
    printf("CH: U+%04X, res=%s, err=%s\n", cpnfc, res2str(res), u_errorName(error));
  }
#endif

  printf("Pure C test OK: %s\n", u_errorName(status));
  fflush(stdout);
  return status;
}
