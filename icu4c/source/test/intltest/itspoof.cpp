/*
**********************************************************************
* Copyright (C) 2009, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*/
/**
 * IntlTestSpoof is the medium level test class for USpoofDetector
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
                TestSpoofAPI();
            }
            break;

        default: name=""; break;
    }
}

void IntlTestSpoof::TestSpoofAPI() {

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
        int32_t checkResults = uspoof_areConfusableUnicodeString(sc, s1, s2, NULL, &status);
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


#endif /* #if !UCONFIG_NO_SPOOF_DETECT*/
