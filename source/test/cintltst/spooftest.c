/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2009, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File spooftest.c
*
*********************************************************************************/
/*C API TEST for the uspoof Unicode Indentifier Spoofing and Security API */
/**
*   This is an API test for ICU spoof detection in plain C.  It doesn't test very many cases, and doesn't
*   try to test the full functionality.  It just calls each function and verifies that it
*   works on a basic level.
*
*   More complete testing of spoof detection functionality is done with the C++ tests.
**/

#include "unicode/utypes.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "unicode/uspoof.h"
#include "unicode/ustring.h"
#include "cintltst.h"

#define TEST_ASSERT_SUCCESS(status) {if (U_FAILURE(status)) { \
    log_err("Failure at file %s, line %d, error = %s\n", __FILE__, __LINE__, u_errorName(status));}}

#define TEST_CHECK_SUCCESS(status) {if (U_FAILURE(status)) { \
    log_err("Failure at file %s, line %d, error = %s\n", __FILE__, __LINE__, u_errorName(status)); \
    goto bailout;} \
}

#define TEST_ASSERT_TRUE(expr) {if ((expr)==FALSE) { \
log_err("Test Failure at file %s, line %d: \"%s\" is false.\n", __FILE__, __LINE__, #expr);}}

#define TEST_ASSERT_EQ(a, b) { if ((a) != (b)) { \
    log_err("Test Failure at file %s, line %d: \"%s\" (%d) != \"%s\" (%d) \n", \
             __FILE__, __LINE__, #a, (a), #b, (b)); }}

#define TEST_ASSERT_NE(a, b) { if ((a) == (b)) { \
    log_err("Test Failure at file %s, line %d: \"%s\" (%d) == \"%s\" (%d) \n", \
             __FILE__, __LINE__, #a, (a), #b, (b)); }}


/*
 *   TEST_SETUP and TEST_TEARDOWN
 *         macros to handle the boilerplate around setting up test case.
 *         Put arbitrary test code between SETUP and TEARDOWN.
 *         "sc" is the ready-to-go  SpoofChecker for use in the tests.
 */
#define TEST_SETUP {  \
    UErrorCode status = U_ZERO_ERROR; \
    USpoofChecker *sc;     \
    sc = uspoof_open(&status);  \
    TEST_CHECK_SUCCESS(status);   \
    {

#define TEST_TEARDOWN  \
    }  \
    TEST_ASSERT_SUCCESS(status);  \
 bailout: \
    uspoof_close(sc);  \
}


static void test_assert_string(const char *expected, const UChar *actual, UBool nulTerm, const char *file, int line) {
     char     buf_inside_macro[120];
     int32_t  len = (int32_t)strlen(expected);
     UBool    success;
     if (nulTerm) {
         u_austrncpy(buf_inside_macro, (actual), len+1);
         buf_inside_macro[len+2] = 0;
         success = (strcmp((expected), buf_inside_macro) == 0);
     } else {
         u_austrncpy(buf_inside_macro, (actual), len);
         buf_inside_macro[len+1] = 0;
         success = (strncmp((expected), buf_inside_macro, len) == 0);
     }
     if (success == FALSE) {
         log_err("Failure at file %s, line %d, expected \"%s\", got \"%s\"\n",
             file, line, (expected), buf_inside_macro);
     }
}

#define TEST_ASSERT_STRING(expected, actual, nulTerm) test_assert_string(expected, actual, nulTerm, __FILE__, __LINE__)



static void TestUSpoofCAPI(void);

void addUSpoofTest(TestNode** root);

void addUSpoofTest(TestNode** root)
{
    addTest(root, &TestUSpoofCAPI, "uspoof/TestUSpoofCAPI");
}


/*
 *   Spoof Detction C API Tests
 */
static void TestUSpoofCAPI(void) {

    TEST_SETUP
    const char *dataSrcDir;
    char       *fileName;
    char       *confusables;
    int         confusablesLength;
    char       *confusablesWholeScript;
    int         confusablesWholeScriptLength;
    FILE       *f;
    UParseError pe;
    int32_t     errType;
    USpoofChecker *rsc;
    
    dataSrcDir = ctest_dataSrcDir();
    fileName = malloc(strlen(dataSrcDir) + 100);
    strcpy(fileName, dataSrcDir);
    strcat(fileName, U_FILE_SEP_STRING "unidata" U_FILE_SEP_STRING "confusables.txt");
    f = fopen(fileName, "r");
    TEST_ASSERT_NE(f, NULL);
    confusables = malloc(3000000);
    confusablesLength = fread(confusables, 1, 3000000, f);
    fclose(f);

    
    strcpy(fileName, dataSrcDir);
    strcat(fileName, U_FILE_SEP_STRING "unidata" U_FILE_SEP_STRING "confusablesWholeScript.txt");
    f = fopen(fileName, "r");
    TEST_ASSERT_NE(f, NULL);
    confusablesWholeScript = malloc(1000000);
    confusablesWholeScriptLength = fread(confusablesWholeScript, 1, 1000000, f);
    fclose(f);

    rsc = uspoof_openFromSource(confusables, confusablesLength,
                                              confusablesWholeScript, confusablesWholeScriptLength,
                                              &errType, &pe, &status);
    TEST_ASSERT_SUCCESS(status);

    free(confusablesWholeScript);
    free(confusables);
    free(fileName);
    uspoof_close(rsc);
    /*  printf("ParseError Line is %d\n", pe.line);  */
    TEST_TEARDOWN;
    
}

