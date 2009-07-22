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

#if !UCONFIG_NO_REGULAR_EXPRESSIONS

#include "itspoof.h"
#include "unicode/uspoof.h"

#define TEST_ASSERT_SUCCESS(status) {if (U_FAILURE(status)) { \
    errcheckln(status, "Failure at file %s, line %d, error = %s", __FILE__, __LINE__, u_errorName(status));}}

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
         case 2:
            name = "TestAreConfusable";
            if (exec) {
                testAreConfusable();
            }
            break;
          case 3:
            name = "TestInvisible";
            if (exec) {
                testInvisible();
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
        TEST_ASSERT_EQ(USPOOF_MIXED_SCRIPT_CONFUSABLE | USPOOF_WHOLE_SCRIPT_CONFUSABLE, checkResults);

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
//                 Test cases chosen for substitutions of various lengths, and 
//                 membership in different mapping tables.
void IntlTestSpoof::testSkeleton() {
    const uint32_t ML = 0;
    const uint32_t SL = USPOOF_SINGLE_SCRIPT_CONFUSABLE;
    const uint32_t MA = USPOOF_ANY_CASE;
    const uint32_t SA = USPOOF_SINGLE_SCRIPT_CONFUSABLE | USPOOF_ANY_CASE;

    TEST_SETUP
        // A long "identifier" that will overflow implementation stack buffers, forcing heap allocations.
        CHECK_SKELETON(SL, " A long 'identifier' that will overflow implementation stack buffers, forcing heap allocations."
                           " A long 'identifier' that will overflow implementation stack buffers, forcing heap allocations."
                           " A long 'identifier' that will overflow implementation stack buffers, forcing heap allocations."
                           " A long 'identifier' that will overflow implementation stack buffers, forcing heap allocations.",

               " A 1ong \\u02b9identifier\\u02b9 that wi11 overf1ow imp1ementation stack buffers, forcing heap a11ocations."
               " A 1ong \\u02b9identifier\\u02b9 that wi11 overf1ow imp1ementation stack buffers, forcing heap a11ocations."
               " A 1ong \\u02b9identifier\\u02b9 that wi11 overf1ow imp1ementation stack buffers, forcing heap a11ocations."
               " A 1ong \\u02b9identifier\\u02b9 that wi11 overf1ow imp1ementation stack buffers, forcing heap a11ocations.")

        // FC5F ;	FE74 0651 ;   ML  #* ARABIC LIGATURE SHADDA WITH KASRATAN ISOLATED FORM to
        //                                ARABIC KASRATAN ISOLATED FORM, ARABIC SHADDA	
        //    This character NFKD normalizes to \u0020 \u064d \u0651, so its confusable mapping 
        //    is never used in creating a skeleton.
        CHECK_SKELETON(SL, "\\uFC5F", " \\u064d\\u0651");

        CHECK_SKELETON(SL, "nochange", "nochange");
        CHECK_SKELETON(MA, "love", "1ove");   // lower case l to digit 1
        CHECK_SKELETON(ML, "OOPS", "OOPS");
        CHECK_SKELETON(MA, "OOPS", "00PS");   // Letter O to digit 0 in any case mode only
        CHECK_SKELETON(SL, "\\u059c", "\\u0301");
        CHECK_SKELETON(SL, "\\u2A74", "\\u003A\\u003A\\u003D");
        CHECK_SKELETON(SL, "\\u247E", "\\u0028\\u0031\\u0031\\u0029");
        CHECK_SKELETON(SL, "\\uFDFB", "\\u062C\\u0644\\u0020\\u062C\\u0644\\u0627\\u0644\\u0647");

        // This mapping exists in the ML and MA tables, does not exist in SL, SA
        //0C83 ;	0C03 ;	ML	# ( ಃ → ః ) KANNADA SIGN VISARGA → TELUGU SIGN VISARGA	# {source:513}
        CHECK_SKELETON(SL, "\\u0C83", "\\u0C83");
        CHECK_SKELETON(SA, "\\u0C83", "\\u0C83");
        CHECK_SKELETON(ML, "\\u0C83", "\\u0C03");
        CHECK_SKELETON(MA, "\\u0C83", "\\u0C03");
        
        // 0391 ; 0041 ; MA # ( Α → A ) GREEK CAPITAL LETTER ALPHA to LATIN CAPITAL LETTER A 
        // This mapping exists only in the MA table.
        CHECK_SKELETON(MA, "\\u0391", "A");
        CHECK_SKELETON(SA, "\\u0391", "\\u0391");
        CHECK_SKELETON(ML, "\\u0391", "\\u0391");
        CHECK_SKELETON(SL, "\\u0391", "\\u0391");

        // 13CF ;  0062 ;  MA  #  CHEROKEE LETTER SI to LATIN SMALL LETTER B  
        // This mapping exists in the ML and MA tables
        CHECK_SKELETON(ML, "\\u13CF", "b");
        CHECK_SKELETON(MA, "\\u13CF", "b");
        CHECK_SKELETON(SL, "\\u13CF", "\\u13CF");
        CHECK_SKELETON(SA, "\\u13CF", "\\u13CF");

        // 0022 ;  02B9 02B9 ;  SA  #*  QUOTATION MARK to MODIFIER LETTER PRIME, MODIFIER LETTER PRIME 
        // all tables.
        CHECK_SKELETON(SL, "\\u0022", "\\u02B9\\u02B9");
        CHECK_SKELETON(SA, "\\u0022", "\\u02B9\\u02B9");
        CHECK_SKELETON(ML, "\\u0022", "\\u02B9\\u02B9");
        CHECK_SKELETON(MA, "\\u0022", "\\u02B9\\u02B9");

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
        errln(UnicodeString(" Actual   Skeleton: \"") + actual + UnicodeString("\"\n") +
              UnicodeString(" Expected Skeleton: \"") + uExpected + UnicodeString("\""));
    }
}

void IntlTestSpoof::testAreConfusable() {
    TEST_SETUP
        UnicodeString s1("A long string that will overflow stack buffers.  A long string that will overflow stack buffers. "
                         "A long string that will overflow stack buffers.  A long string that will overflow stack buffers. ");
        UnicodeString s2("A long string that wi11 overflow stack buffers.  A long string that will overflow stack buffers. "
                         "A long string that wi11 overflow stack buffers.  A long string that will overflow stack buffers. ");
        TEST_ASSERT_EQ(USPOOF_SINGLE_SCRIPT_CONFUSABLE, uspoof_areConfusableUnicodeString(sc, s1, s2, &status));
        TEST_ASSERT_SUCCESS(status);

    TEST_TEARDOWN;
}

void IntlTestSpoof::testInvisible() {
    TEST_SETUP
        UnicodeString  s = UnicodeString("abcd\\u0301ef").unescape();
        int32_t position = -42;
        TEST_ASSERT_EQ(0, uspoof_checkUnicodeString(sc, s, &position, &status));
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT(position == -42);

        UnicodeString  s2 = UnicodeString("abcd\\u0301\\u0302\\u0301ef").unescape();
        TEST_ASSERT_EQ(USPOOF_INVISIBLE, uspoof_checkUnicodeString(sc, s2, &position, &status));
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT_EQ(7, position);

        // Tow acute accents, one from the composed a with acute accent, \u00e1,
        // and one separate.
        position = -42;
        UnicodeString  s3 = UnicodeString("abcd\\u00e1\\u0301xyz").unescape();
        TEST_ASSERT_EQ(USPOOF_INVISIBLE, uspoof_checkUnicodeString(sc, s3, &position, &status));
        TEST_ASSERT_SUCCESS(status);
        TEST_ASSERT_EQ(7, position);
    TEST_TEARDOWN;
}
#endif // UCONFIG_NO_REGULAR_EXPRESSIONS

