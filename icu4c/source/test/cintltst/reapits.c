/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File reapits.c
*
*********************************************************************************/
/*C API TEST FOR Regular Expressions */
/**
*   This is an API test for ICU regular expressions in C.  It doesn't test very many cases, and doesn't
*   try to test the full functionality.  It just calls each function and verifies that it
*   works on a basic level.
*
*   More complete testing of regular expression functionality is done with the C++ tests.
**/

#include "unicode/utypes.h"

#if !UCONFIG_NO_REGULAR_EXPRESSIONS

#include <stdlib.h>
#include <string.h>
#include "unicode/uloc.h"
#include "unicode/uregex.h"
#include "unicode/ustring.h"
#include "cintltst.h"

#define TEST_ASSERT_SUCCESS(status) {if (U_FAILURE(status)) { \
log_err("Failure at file %s, line %d, error = %s\n", __FILE__, __LINE__, u_errorName(status));}}

#define TEST_ASSERT(expr) {if ((expr)==FALSE) { \
log_err("Test Failure at file %s, line %d\n", __FILE__, __LINE__);}}

static void TestRegexCAPI(void);

void addURegexTest(TestNode** root);

void addURegexTest(TestNode** root)
{
    addTest(root, &TestRegexCAPI, "regex/TestRegexCAPI");
/*  addTest(root, &TestBreakIteratorSafeClone, "tstxtbd/cbiapts/TestBreakIteratorSafeClone"); */
}


void TestRegexCAPI(void) {
    UErrorCode           status = U_ZERO_ERROR;
    URegularExpression  *re;
    UChar                pat[200];

    /* Mimimalist open/close */
    u_uastrncpy(pat, "abc*", sizeof(pat)/2);
    re = uregex_open(pat, -1, 0, 0, &status);
    TEST_ASSERT_SUCCESS(status);
    uregex_close(re);

    /* Open with all flag values set */
    status = U_ZERO_ERROR;
    re = uregex_open(pat, -1, 
        UREGEX_CASE_INSENSITIVE | UREGEX_COMMENTS | UREGEX_DOTALL | UREGEX_MULTILINE | UREGEX_UWORD,
        0, &status);
    TEST_ASSERT_SUCCESS(status);
    uregex_close(re);

    /* Open with an invalid flag */
    status = U_ZERO_ERROR;
    re = uregex_open(pat, -1, 0x40000000, 0, &status);
    TEST_ASSERT(status == U_REGEX_INVALID_FLAG);
    uregex_close(re);


    /* openC   open from a C string */
    {
        const UChar   *p;
        int32_t  len;
        status = U_ZERO_ERROR;
        re = uregex_openC("abc*", 0, 0, &status);
        TEST_ASSERT_SUCCESS(status);
        p = uregex_pattern(re, &len, &status);
        TEST_ASSERT_SUCCESS(status);
        u_uastrncpy(pat, "abc*", sizeof(pat)/2);
        TEST_ASSERT(u_strcmp(pat, p) == 0);
        TEST_ASSERT(len==(int32_t)strlen("abc*"));

        uregex_close(re);
    }

    /*
     *  clone
     */
    {
        URegularExpression *clone1;
        URegularExpression *clone2;
        URegularExpression *clone3;
        UChar  testString1[30];
        UChar  testString2[30];
        UBool  result;


        status = U_ZERO_ERROR;
        re = uregex_openC("abc*", 0, 0, &status);
        TEST_ASSERT_SUCCESS(status);
        clone1 = uregex_clone(re, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(clone1 != NULL);

        status = U_ZERO_ERROR;
        clone2 = uregex_clone(re, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(clone2 != NULL);
        uregex_close(re);

        status = U_ZERO_ERROR;
        clone3 = uregex_clone(clone2, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(clone3 != NULL);

        u_uastrncpy(testString1, "abcccd", sizeof(pat)/2);
        u_uastrncpy(testString2, "xxxabcccd", sizeof(pat)/2);

        status = U_ZERO_ERROR;
        uregex_setText(clone1, testString1, -1, &status);
        TEST_ASSERT_SUCCESS(status);
        result = uregex_lookingAt(clone1, 0, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(result==TRUE);
        
        status = U_ZERO_ERROR;
        uregex_setText(clone2, testString2, -1, &status);
        TEST_ASSERT_SUCCESS(status);
        result = uregex_lookingAt(clone2, 0, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(result==FALSE);
        result = uregex_find(clone2, 0, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(result==TRUE);

        uregex_close(clone1);
        uregex_close(clone2);
        uregex_close(clone3);

    }

    /*
     *  pattern()
    */
    {
        const UChar  *resultPat;
        int32_t       resultLen;
        u_uastrncpy(pat, "hello", sizeof(pat)/2);
        status = U_ZERO_ERROR;
        re = uregex_open(pat, -1, 0, NULL, &status);
        resultPat = uregex_pattern(re, &resultLen, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(resultLen == -1);
        TEST_ASSERT(u_strcmp(resultPat, pat) == 0);
        uregex_close(re);

        status = U_ZERO_ERROR;
        re = uregex_open(pat, 3, 0, NULL, &status);
        resultPat = uregex_pattern(re, &resultLen, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(resultLen == 3);
        TEST_ASSERT(u_strncmp(resultPat, pat, 3) == 0);
        TEST_ASSERT(u_strlen(resultPat) == 3);
        uregex_close(re);


    }
    

}

#endif   /*  !UCONFIG_NO_REGULAR_EXPRESSIONS */
