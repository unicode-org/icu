/*
**********************************************************************
* Copyright (C) 2009, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*/
/**
 * IntlTestSpoof tests for USpoofDetector
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_SPOOF_DETECT

#include "itspoof.h"
#include "unicode/uspoof.h"

#define TEST_ASSERT_SUCCESS(status) {if (U_FAILURE(status)) { \
    errln("Failure at file %s, line %d, error = %s\n", __FILE__, __LINE__, u_errorName(status));}}

#define TEST_ASSERT(expr) {if ((expr)==FALSE) { \
    errln("Test Failure at file %s, line %d: \"%s\" is false.\n", __FILE__, __LINE__, #expr);};}

#define TEST_ASSERT_EQ(a, b) { if ((a) != (b)) { \
    errln("Test Failure at file %s, line %d: \"%s\" (%d) != \"%s\" (%d) \n", \
             __FILE__, __LINE__, #a, (a), #b, (b)); }}

#define TEST_ASSERT_NE(a, b) { if ((a) == (b)) { \
    errln("Test Failure at file %s, line %d: \"%s\" (%d) == \"%s\" (%d) \n", \
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
    TEST_ASSERT_SUCCESS(status);   \
    if (U_SUCCESS(status)){

#define TEST_TEARDOWN  \
    }  \
    TEST_ASSERT_SUCCESS(status);  \
    uspoof_close(sc);  \
}




void IntlTestSpoof::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite spoof: ");
    switch (index) {
        case 0:
            name = "TestSpoofAPI"; 
            if (exec) {
                testSpoofAPI();
            }
            break;
         case 1:
            name = "TestSkeleton"; 
            if (exec) {
                testSkeleton();
            }
            break;
        default: name=""; break;
    }
}

void IntlTestSpoof::testSpoofAPI() {

    TEST_SETUP
        UnicodeString s("uvw");
        int32_t position = 666;
        int32_t checkResults = uspoof_checkUnicodeString(sc, s, &position, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT_EQ(0, checkResults);
        TEST_ASSERT_EQ(666, position);
    TEST_TEARDOWN;
    
    TEST_SETUP
        UnicodeString s1("cxs");
        UnicodeString s2 = UnicodeString("\\u0441\\u0445\\u0455").unescape();  // Cyrillic "cxs"
        int32_t checkResults = uspoof_areConfusableUnicodeString(sc, s1, s2, &status);
        TEST_ASSERT_EQ(USPOOF_MIXED_SCRIPT_CONFUSABLE, checkResults);

    TEST_TEARDOWN;

    TEST_SETUP
        UnicodeString s("I1l0O");
        UnicodeString dest;
        UnicodeString &retStr = uspoof_getSkeletonUnicodeString(sc, USPOOF_ANY_CASE, s, dest, &status);
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(UnicodeString("11100") == dest);
        TEST_ASSERT(&dest == &retStr);
    TEST_TEARDOWN;
}


#define CHECK_SKELETON(type, input, expected) { \
    checkSkeleton(sc, type, input, expected, __LINE__); \
    }


// testSkeleton.   Spot check a number of confusable skeleton substitutions from the 
//                 Unicode data file confusables.txt
void IntlTestSpoof::testSkeleton() {
    TEST_SETUP
        CHECK_SKELETON(0, "\\u059c", "\\u0301");
//      CHECK_SKELETON(0, "\\uFC5F", "\\uFE74\\u0651");
        CHECK_SKELETON(0, "\\u2A74", "\\u003A\\u003A\\u003D");
        CHECK_SKELETON(0, "\\u247E", "\\u0028\\u0031\\u0031\\u0029");
        CHECK_SKELETON(0, "\\uFDFB", "\\u062C\\u0644\\u0020\\u062C\\u0644\\u0627\\u0644\\u0647");
    TEST_TEARDOWN;
}


//
//  Run a single confusable skeleton transformation test case.
//
void IntlTestSpoof::checkSkeleton(const USpoofChecker *sc, uint32_t type, 
                                  const char *input, const char *expected, int32_t lineNum) {
    UnicodeString uInput = UnicodeString(input).unescape();
    UnicodeString uExpected = UnicodeString(expected).unescape();
    
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString actual;
    uspoof_getSkeletonUnicodeString(sc, type, uInput, actual, &status);
    if (U_FAILURE(status)) {
        errln("File %s, Line %d, Test case from line %d, status is %s", __FILE__, __LINE__, lineNum,
              u_errorName(status));
        return;
    }
    if (uExpected != actual) {
        errln("File %s, Line %d, Test case from line %d, Actual and Expected skeletons differ.",
               __FILE__, __LINE__, lineNum);
        errln(UnicodeString(" Actual   Skeleton: \"") + actual + UnicodeString("\""));
        errln(UnicodeString(" Expected Skeleton: \"") + uExpected + UnicodeString("\""));
    }
}

#endif /* #if !UCONFIG_NO_SPOOF_DETECT*/
