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
* File CNORMTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API
*********************************************************************************
/*tests for u_normalization*/
#include "utypes.h"
#include "ucol.h"
#include "uloc.h"
#include "cintltst.h"
#include "cnormtst.h"
#include "ccolltst.h"
#include "ustring.h"
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
    { "\\uFF76\\u3099",            "\\uFF76\\u3099",            "\\uFF76\\u3099"        }  /* hw_ka + ten*/
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

}

void TestDecomp() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source, *result; 
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
        assertEqual(result, CharsToUChars(canonTests[x][1]), x);
        free(result);
    }
    ucol_close(myCollation);            
}
void TestCompatDecomp() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source, *result; 
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
        assertEqual(result, CharsToUChars(compatTests[x][1]), x);
        free(result);
    }
    ucol_close(myCollation);            
}
void TestCanonDecompCompose() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source, *result; 
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
        assertEqual(result, CharsToUChars(canonTests[x][2]), x);
        free(result);
    }
    ucol_close(myCollation);            
}
void TestCompatDecompCompose() 
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t x, neededLen, resLen;
    UChar *source, *result; 
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
        assertEqual(result, CharsToUChars(compatTests[x][2]), x);
        free(result);
    }
    ucol_close(myCollation);            
}



void assertEqual(const UChar* result, const UChar* expected, int32_t index)
{
    if(u_strcmp(result, expected)!=0){
        log_err("ERROR in decomposition at index = %d. EXPECTED: %s , GOT: %s\n", index, austrdup(expected),
            austrdup(result) );
    }
}
