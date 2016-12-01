// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2002-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  strcase.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002mar12
*   created by: Markus W. Scherer
*
*   Test file for string casing C++ API functions.
*/

#include "unicode/std_string.h"
#include "unicode/uchar.h"
#include "unicode/ures.h"
#include "unicode/uloc.h"
#include "unicode/locid.h"
#include "unicode/ubrk.h"
#include "unicode/unistr.h"
#include "unicode/ucasemap.h"
#include "ucase.h"
#include "ustrtest.h"
#include "unicode/tstdtmod.h"
#include "cmemory.h"

StringCaseTest::StringCaseTest() : GREEK_LOCALE_("el") {}

StringCaseTest::~StringCaseTest() {}

void
StringCaseTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite StringCaseTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestCaseConversion);
#if !UCONFIG_NO_BREAK_ITERATION && !UCONFIG_NO_FILE_IO && !UCONFIG_NO_LEGACY_CONVERSION
    TESTCASE_AUTO(TestCasing);
#endif
    TESTCASE_AUTO(TestFullCaseFoldingIterator);
    TESTCASE_AUTO(TestGreekUpper);
    TESTCASE_AUTO(TestLongUpper);
    TESTCASE_AUTO(TestMalformedUTF8);
    TESTCASE_AUTO(TestBufferOverflow);
    TESTCASE_AUTO_END;
}

void
StringCaseTest::TestCaseConversion()
{
    static const UChar uppercaseGreek[] =
        { 0x399, 0x395, 0x3a3, 0x3a5, 0x3a3, 0x20, 0x03a7, 0x3a1, 0x399, 0x3a3, 0x3a4,
        0x39f, 0x3a3, 0 };
        // "IESUS CHRISTOS"

    static const UChar lowercaseGreek[] = 
        { 0x3b9, 0x3b5, 0x3c3, 0x3c5, 0x3c2, 0x20, 0x03c7, 0x3c1, 0x3b9, 0x3c3, 0x3c4,
        0x3bf, 0x3c2, 0 };
        // "iesus christos"

    static const UChar lowercaseTurkish[] = 
        { 0x69, 0x73, 0x74, 0x61, 0x6e, 0x62, 0x75, 0x6c, 0x2c, 0x20, 0x6e, 0x6f, 0x74, 0x20, 0x63, 0x6f, 
        0x6e, 0x73, 0x74, 0x61, 0x6e, 0x74, 0x0131, 0x6e, 0x6f, 0x70, 0x6c, 0x65, 0x21, 0 };

    static const UChar uppercaseTurkish[] = 
        { 0x54, 0x4f, 0x50, 0x4b, 0x41, 0x50, 0x49, 0x20, 0x50, 0x41, 0x4c, 0x41, 0x43, 0x45, 0x2c, 0x20,
        0x0130, 0x53, 0x54, 0x41, 0x4e, 0x42, 0x55, 0x4c, 0 };
    
    UnicodeString expectedResult;
    UnicodeString   test3;

    test3 += (UChar32)0x0130;
    test3 += "STANBUL, NOT CONSTANTINOPLE!";

    UnicodeString   test4(test3);
    test4.toLower(Locale(""));
    expectedResult = UnicodeString("i\\u0307stanbul, not constantinople!", "").unescape();
    if (test4 != expectedResult)
        errln("1. toLower failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test4 = test3;
    test4.toLower(Locale("tr", "TR"));
    expectedResult = lowercaseTurkish;
    if (test4 != expectedResult)
        errln("2. toLower failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test3 = "topkap";
    test3 += (UChar32)0x0131;
    test3 += " palace, istanbul";
    test4 = test3;

    test4.toUpper(Locale(""));
    expectedResult = "TOPKAPI PALACE, ISTANBUL";
    if (test4 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test4 = test3;
    test4.toUpper(Locale("tr", "TR"));
    expectedResult = uppercaseTurkish;
    if (test4 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test3 = CharsToUnicodeString("S\\u00FC\\u00DFmayrstra\\u00DFe");

    test3.toUpper(Locale("de", "DE"));
    expectedResult = CharsToUnicodeString("S\\u00DCSSMAYRSTRASSE");
    if (test3 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test3 + "\".");
    
    test4.replace(0, test4.length(), uppercaseGreek);

    test4.toLower(Locale("el", "GR"));
    expectedResult = lowercaseGreek;
    if (test4 != expectedResult)
        errln("toLower failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");
    
    test4.replace(0, test4.length(), lowercaseGreek);

    test4.toUpper();
    expectedResult = uppercaseGreek;
    if (test4 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    // more string case mapping tests with the new implementation
    {
        static const UChar

        beforeLower[]= { 0x61, 0x42, 0x49,  0x3a3, 0xdf, 0x3a3, 0x2f, 0xd93f, 0xdfff },
        lowerRoot[]=   { 0x61, 0x62, 0x69,  0x3c3, 0xdf, 0x3c2, 0x2f, 0xd93f, 0xdfff },
        lowerTurkish[]={ 0x61, 0x62, 0x131, 0x3c3, 0xdf, 0x3c2, 0x2f, 0xd93f, 0xdfff },

        beforeUpper[]= { 0x61, 0x42, 0x69,  0x3c2, 0xdf,       0x3c3, 0x2f, 0xfb03,           0xfb03,           0xfb03,           0xd93f, 0xdfff },
        upperRoot[]=   { 0x41, 0x42, 0x49,  0x3a3, 0x53, 0x53, 0x3a3, 0x2f, 0x46, 0x46, 0x49, 0x46, 0x46, 0x49, 0x46, 0x46, 0x49, 0xd93f, 0xdfff },
        upperTurkish[]={ 0x41, 0x42, 0x130, 0x3a3, 0x53, 0x53, 0x3a3, 0x2f, 0x46, 0x46, 0x49, 0x46, 0x46, 0x49, 0x46, 0x46, 0x49, 0xd93f, 0xdfff },

        beforeMiniUpper[]=  { 0xdf, 0x61 },
        miniUpper[]=        { 0x53, 0x53, 0x41 };

        UnicodeString s;

        /* lowercase with root locale */
        s=UnicodeString(FALSE, beforeLower, UPRV_LENGTHOF(beforeLower));
        s.toLower("");
        if( s.length()!=UPRV_LENGTHOF(lowerRoot) ||
            s!=UnicodeString(FALSE, lowerRoot, s.length())
        ) {
            errln("error in toLower(root locale)=\"" + s + "\" expected \"" + UnicodeString(FALSE, lowerRoot, UPRV_LENGTHOF(lowerRoot)) + "\"");
        }

        /* lowercase with turkish locale */
        s=UnicodeString(FALSE, beforeLower, UPRV_LENGTHOF(beforeLower));
        s.setCharAt(0, beforeLower[0]).toLower(Locale("tr"));
        if( s.length()!=UPRV_LENGTHOF(lowerTurkish) ||
            s!=UnicodeString(FALSE, lowerTurkish, s.length())
        ) {
            errln("error in toLower(turkish locale)=\"" + s + "\" expected \"" + UnicodeString(FALSE, lowerTurkish, UPRV_LENGTHOF(lowerTurkish)) + "\"");
        }

        /* uppercase with root locale */
        s=UnicodeString(FALSE, beforeUpper, UPRV_LENGTHOF(beforeUpper));
        s.setCharAt(0, beforeUpper[0]).toUpper(Locale(""));
        if( s.length()!=UPRV_LENGTHOF(upperRoot) ||
            s!=UnicodeString(FALSE, upperRoot, s.length())
        ) {
            errln("error in toUpper(root locale)=\"" + s + "\" expected \"" + UnicodeString(FALSE, upperRoot, UPRV_LENGTHOF(upperRoot)) + "\"");
        }

        /* uppercase with turkish locale */
        s=UnicodeString(FALSE, beforeUpper, UPRV_LENGTHOF(beforeUpper));
        s.toUpper(Locale("tr"));
        if( s.length()!=UPRV_LENGTHOF(upperTurkish) ||
            s!=UnicodeString(FALSE, upperTurkish, s.length())
        ) {
            errln("error in toUpper(turkish locale)=\"" + s + "\" expected \"" + UnicodeString(FALSE, upperTurkish, UPRV_LENGTHOF(upperTurkish)) + "\"");
        }

        /* uppercase a short string with root locale */
        s=UnicodeString(FALSE, beforeMiniUpper, UPRV_LENGTHOF(beforeMiniUpper));
        s.setCharAt(0, beforeMiniUpper[0]).toUpper("");
        if( s.length()!=UPRV_LENGTHOF(miniUpper) ||
            s!=UnicodeString(FALSE, miniUpper, s.length())
        ) {
            errln("error in toUpper(root locale)=\"" + s + "\" expected \"" + UnicodeString(FALSE, miniUpper, UPRV_LENGTHOF(miniUpper)) + "\"");
        }
    }

    // test some supplementary characters (>= Unicode 3.1)
    {
        UnicodeString t;

        UnicodeString
            deseretInput=UnicodeString("\\U0001043C\\U00010414", "").unescape(),
            deseretLower=UnicodeString("\\U0001043C\\U0001043C", "").unescape(),
            deseretUpper=UnicodeString("\\U00010414\\U00010414", "").unescape();
        (t=deseretInput).toLower();
        if(t!=deseretLower) {
            errln("error lowercasing Deseret (plane 1) characters");
        }
        (t=deseretInput).toUpper();
        if(t!=deseretUpper) {
            errln("error uppercasing Deseret (plane 1) characters");
        }
    }

    // test some more cases that looked like problems
    {
        UnicodeString t;

        UnicodeString
            ljInput=UnicodeString("ab'cD \\uFB00i\\u0131I\\u0130 \\u01C7\\u01C8\\u01C9 \\U0001043C\\U00010414", "").unescape(),
            ljLower=UnicodeString("ab'cd \\uFB00i\\u0131ii\\u0307 \\u01C9\\u01C9\\u01C9 \\U0001043C\\U0001043C", "").unescape(),
            ljUpper=UnicodeString("AB'CD FFIII\\u0130 \\u01C7\\u01C7\\u01C7 \\U00010414\\U00010414", "").unescape();
        (t=ljInput).toLower("en");
        if(t!=ljLower) {
            errln("error lowercasing LJ characters");
        }
        (t=ljInput).toUpper("en");
        if(t!=ljUpper) {
            errln("error uppercasing LJ characters");
        }
    }

#if !UCONFIG_NO_NORMALIZATION
    // some context-sensitive casing depends on normalization data being present

    // Unicode 3.1.1 SpecialCasing tests
    {
        UnicodeString t;

        // sigmas preceded and/or followed by cased letters
        UnicodeString
            sigmas=UnicodeString("i\\u0307\\u03a3\\u0308j \\u0307\\u03a3\\u0308j i\\u00ad\\u03a3\\u0308 \\u0307\\u03a3\\u0308 ", "").unescape(),
            sigmasLower=UnicodeString("i\\u0307\\u03c3\\u0308j \\u0307\\u03c3\\u0308j i\\u00ad\\u03c2\\u0308 \\u0307\\u03c3\\u0308 ", "").unescape(),
            sigmasUpper=UnicodeString("I\\u0307\\u03a3\\u0308J \\u0307\\u03a3\\u0308J I\\u00ad\\u03a3\\u0308 \\u0307\\u03a3\\u0308 ", "").unescape();

        (t=sigmas).toLower();
        if(t!=sigmasLower) {
            errln("error in sigmas.toLower()=\"" + t + "\" expected \"" + sigmasLower + "\"");
        }

        (t=sigmas).toUpper(Locale(""));
        if(t!=sigmasUpper) {
            errln("error in sigmas.toUpper()=\"" + t + "\" expected \"" + sigmasUpper + "\"");
        }

        // turkish & azerbaijani dotless i & dotted I
        // remove dot above if there was a capital I before and there are no more accents above
        UnicodeString
            dots=UnicodeString("I \\u0130 I\\u0307 I\\u0327\\u0307 I\\u0301\\u0307 I\\u0327\\u0307\\u0301", "").unescape(),
            dotsTurkish=UnicodeString("\\u0131 i i i\\u0327 \\u0131\\u0301\\u0307 i\\u0327\\u0301", "").unescape(),
            dotsDefault=UnicodeString("i i\\u0307 i\\u0307 i\\u0327\\u0307 i\\u0301\\u0307 i\\u0327\\u0307\\u0301", "").unescape();

        (t=dots).toLower("tr");
        if(t!=dotsTurkish) {
            errln("error in dots.toLower(tr)=\"" + t + "\" expected \"" + dotsTurkish + "\"");
        }

        (t=dots).toLower("de");
        if(t!=dotsDefault) {
            errln("error in dots.toLower(de)=\"" + t + "\" expected \"" + dotsDefault + "\"");
        }
    }

    // more Unicode 3.1.1 tests
    {
        UnicodeString t;

        // lithuanian dot above in uppercasing
        UnicodeString
            dots=UnicodeString("a\\u0307 \\u0307 i\\u0307 j\\u0327\\u0307 j\\u0301\\u0307", "").unescape(),
            dotsLithuanian=UnicodeString("A\\u0307 \\u0307 I J\\u0327 J\\u0301\\u0307", "").unescape(),
            dotsDefault=UnicodeString("A\\u0307 \\u0307 I\\u0307 J\\u0327\\u0307 J\\u0301\\u0307", "").unescape();

        (t=dots).toUpper("lt");
        if(t!=dotsLithuanian) {
            errln("error in dots.toUpper(lt)=\"" + t + "\" expected \"" + dotsLithuanian + "\"");
        }

        (t=dots).toUpper("de");
        if(t!=dotsDefault) {
            errln("error in dots.toUpper(de)=\"" + t + "\" expected \"" + dotsDefault + "\"");
        }

        // lithuanian adds dot above to i in lowercasing if there are more above accents
        UnicodeString
            i=UnicodeString("I I\\u0301 J J\\u0301 \\u012e \\u012e\\u0301 \\u00cc\\u00cd\\u0128", "").unescape(),
            iLithuanian=UnicodeString("i i\\u0307\\u0301 j j\\u0307\\u0301 \\u012f \\u012f\\u0307\\u0301 i\\u0307\\u0300i\\u0307\\u0301i\\u0307\\u0303", "").unescape(),
            iDefault=UnicodeString("i i\\u0301 j j\\u0301 \\u012f \\u012f\\u0301 \\u00ec\\u00ed\\u0129", "").unescape();

        (t=i).toLower("lt");
        if(t!=iLithuanian) {
            errln("error in i.toLower(lt)=\"" + t + "\" expected \"" + iLithuanian + "\"");
        }

        (t=i).toLower("de");
        if(t!=iDefault) {
            errln("error in i.toLower(de)=\"" + t + "\" expected \"" + iDefault + "\"");
        }
    }

#endif

    // test case folding
    {
        UnicodeString
            s=UnicodeString("A\\u00df\\u00b5\\ufb03\\U0001040c\\u0130\\u0131", "").unescape(),
            f=UnicodeString("ass\\u03bcffi\\U00010434i\\u0307\\u0131", "").unescape(),
            g=UnicodeString("ass\\u03bcffi\\U00010434i\\u0131", "").unescape(),
            t;

        (t=s).foldCase();
        if(f!=t) {
            errln("error in foldCase(\"" + s + "\", default)=\"" + t + "\" but expected \"" + f + "\"");
        }

        // alternate handling for dotted I/dotless i (U+0130, U+0131)
        (t=s).foldCase(U_FOLD_CASE_EXCLUDE_SPECIAL_I);
        if(g!=t) {
            errln("error in foldCase(\"" + s + "\", U_FOLD_CASE_EXCLUDE_SPECIAL_I)=\"" + t + "\" but expected \"" + g + "\"");
        }
    }
}

// data-driven case mapping tests ------------------------------------------ ***

enum {
    TEST_LOWER,
    TEST_UPPER,
    TEST_TITLE,
    TEST_FOLD,
    TEST_COUNT
};

// names of TestData children in casing.txt
static const char *const dataNames[TEST_COUNT+1]={
    "lowercasing",
    "uppercasing",
    "titlecasing",
    "casefolding",
    ""
};

void
StringCaseTest::TestCasingImpl(const UnicodeString &input,
                               const UnicodeString &output,
                               int32_t whichCase,
                               void *iter, const char *localeID, uint32_t options) {
    // UnicodeString
    UnicodeString result;
    const char *name;
    Locale locale(localeID);

    result=input;
    switch(whichCase) {
    case TEST_LOWER:
        name="toLower";
        result.toLower(locale);
        break;
    case TEST_UPPER:
        name="toUpper";
        result.toUpper(locale);
        break;
#if !UCONFIG_NO_BREAK_ITERATION
    case TEST_TITLE:
        name="toTitle";
        result.toTitle((BreakIterator *)iter, locale, options);
        break;
#endif
    case TEST_FOLD:
        name="foldCase";
        result.foldCase(options);
        break;
    default:
        name="";
        break; // won't happen
    }
    if(result!=output) {
        dataerrln("error: UnicodeString.%s() got a wrong result for a test case from casing.res", name);
    }
#if !UCONFIG_NO_BREAK_ITERATION
    if(whichCase==TEST_TITLE && options==0) {
        result=input;
        result.toTitle((BreakIterator *)iter, locale);
        if(result!=output) {
            dataerrln("error: UnicodeString.toTitle(options=0) got a wrong result for a test case from casing.res");
        }
    }
#endif

    // UTF-8
    char utf8In[100], utf8Out[100];
    int32_t utf8InLength, utf8OutLength, resultLength;
    UChar *buffer;

    IcuTestErrorCode errorCode(*this, "TestCasingImpl");
    LocalUCaseMapPointer csm(ucasemap_open(localeID, options, errorCode));
#if !UCONFIG_NO_BREAK_ITERATION
    if(iter!=NULL) {
        // Clone the break iterator so that the UCaseMap can safely adopt it.
        UBreakIterator *clone=ubrk_safeClone((UBreakIterator *)iter, NULL, NULL, errorCode);
        ucasemap_setBreakIterator(csm.getAlias(), clone, errorCode);
    }
#endif

    u_strToUTF8(utf8In, (int32_t)sizeof(utf8In), &utf8InLength, input.getBuffer(), input.length(), errorCode);
    switch(whichCase) {
    case TEST_LOWER:
        name="ucasemap_utf8ToLower";
        utf8OutLength=ucasemap_utf8ToLower(csm.getAlias(),
                    utf8Out, (int32_t)sizeof(utf8Out),
                    utf8In, utf8InLength, errorCode);
        break;
    case TEST_UPPER:
        name="ucasemap_utf8ToUpper";
        utf8OutLength=ucasemap_utf8ToUpper(csm.getAlias(),
                    utf8Out, (int32_t)sizeof(utf8Out),
                    utf8In, utf8InLength, errorCode);
        break;
#if !UCONFIG_NO_BREAK_ITERATION
    case TEST_TITLE:
        name="ucasemap_utf8ToTitle";
        utf8OutLength=ucasemap_utf8ToTitle(csm.getAlias(),
                    utf8Out, (int32_t)sizeof(utf8Out),
                    utf8In, utf8InLength, errorCode);
        break;
#endif
    case TEST_FOLD:
        name="ucasemap_utf8FoldCase";
        utf8OutLength=ucasemap_utf8FoldCase(csm.getAlias(),
                    utf8Out, (int32_t)sizeof(utf8Out),
                    utf8In, utf8InLength, errorCode);
        break;
    default:
        name="";
        utf8OutLength=0;
        break; // won't happen
    }
    buffer=result.getBuffer(utf8OutLength);
    u_strFromUTF8(buffer, result.getCapacity(), &resultLength, utf8Out, utf8OutLength, errorCode);
    result.releaseBuffer(errorCode.isSuccess() ? resultLength : 0);

    if(errorCode.isFailure()) {
        errcheckln(errorCode, "error: %s() got an error for a test case from casing.res - %s", name, u_errorName(errorCode));
        errorCode.reset();
    } else if(result!=output) {
        errln("error: %s() got a wrong result for a test case from casing.res", name);
        errln("expected \"" + output + "\" got \"" + result + "\"" );
    }
}

void
StringCaseTest::TestCasing() {
    UErrorCode status = U_ZERO_ERROR;
#if !UCONFIG_NO_BREAK_ITERATION
    LocalUBreakIteratorPointer iter;
#endif
    char cLocaleID[100];
    UnicodeString locale, input, output, optionsString, result;
    uint32_t options;
    int32_t whichCase, type;
    LocalPointer<TestDataModule> driver(TestDataModule::getTestDataModule("casing", *this, status));
    if(U_SUCCESS(status)) {
        for(whichCase=0; whichCase<TEST_COUNT; ++whichCase) {
#if UCONFIG_NO_BREAK_ITERATION
            if(whichCase==TEST_TITLE) {
                continue;
            }
#endif
            LocalPointer<TestData> casingTest(driver->createTestData(dataNames[whichCase], status));
            if(U_FAILURE(status)) {
                errln("TestCasing failed to createTestData(%s) - %s", dataNames[whichCase], u_errorName(status));
                break;
            }
            const DataMap *myCase = NULL;
            while(casingTest->nextCase(myCase, status)) {
                input = myCase->getString("Input", status);
                output = myCase->getString("Output", status);

                if(whichCase!=TEST_FOLD) {
                    locale = myCase->getString("Locale", status);
                }
                locale.extract(0, 0x7fffffff, cLocaleID, sizeof(cLocaleID), "");

#if !UCONFIG_NO_BREAK_ITERATION
                if(whichCase==TEST_TITLE) {
                    type = myCase->getInt("Type", status);
                    if(type>=0) {
                        iter.adoptInstead(ubrk_open((UBreakIteratorType)type, cLocaleID, NULL, 0, &status));
                    } else if(type==-2) {
                        // Open a trivial break iterator that only delivers { 0, length }
                        // or even just { 0 } as boundaries.
                        static const UChar rules[] = { 0x2e, 0x2a, 0x3b };  // ".*;"
                        UParseError parseError;
                        iter.adoptInstead(ubrk_openRules(rules, UPRV_LENGTHOF(rules), NULL, 0, &parseError, &status));
                    }
                }
#endif
                options = 0;
                if(whichCase==TEST_TITLE || whichCase==TEST_FOLD) {
                    optionsString = myCase->getString("Options", status);
                    if(optionsString.indexOf((UChar)0x54)>=0) {  // T
                        options|=U_FOLD_CASE_EXCLUDE_SPECIAL_I;
                    }
                    if(optionsString.indexOf((UChar)0x4c)>=0) {  // L
                        options|=U_TITLECASE_NO_LOWERCASE;
                    }
                    if(optionsString.indexOf((UChar)0x41)>=0) {  // A
                        options|=U_TITLECASE_NO_BREAK_ADJUSTMENT;
                    }
                }

                if(U_FAILURE(status)) {
                    dataerrln("error: TestCasing() setup failed for %s test case from casing.res: %s", dataNames[whichCase],  u_errorName(status));
                    status = U_ZERO_ERROR;
                } else {
#if UCONFIG_NO_BREAK_ITERATION
                    LocalPointer<UMemory> iter;
#endif
                    TestCasingImpl(input, output, whichCase, iter.getAlias(), cLocaleID, options);
                }

#if !UCONFIG_NO_BREAK_ITERATION
                iter.adoptInstead(NULL);
#endif
            }
        }
    }

#if !UCONFIG_NO_BREAK_ITERATION
    // more tests for API coverage
    status=U_ZERO_ERROR;
    input=UNICODE_STRING_SIMPLE("sTrA\\u00dfE").unescape();
    (result=input).toTitle(NULL);
    if(result!=UNICODE_STRING_SIMPLE("Stra\\u00dfe").unescape()) {
        dataerrln("UnicodeString::toTitle(NULL) failed.");
    }
#endif
}

void
StringCaseTest::TestFullCaseFoldingIterator() {
    UnicodeString ffi=UNICODE_STRING_SIMPLE("ffi");
    UnicodeString ss=UNICODE_STRING_SIMPLE("ss");
    FullCaseFoldingIterator iter;
    int32_t count=0;
    int32_t countSpecific=0;
    UChar32 c;
    UnicodeString full;
    while((c=iter.next(full))>=0) {
        ++count;
        // Check that the full Case_Folding has more than 1 code point.
        if(!full.hasMoreChar32Than(0, 0x7fffffff, 1)) {
            errln("error: FullCaseFoldingIterator.next()=U+%04lX full Case_Folding has at most 1 code point", (long)c);
            continue;
        }
        // Check that full == Case_Folding(c).
        UnicodeString cf(c);
        cf.foldCase();
        if(full!=cf) {
            errln("error: FullCaseFoldingIterator.next()=U+%04lX full Case_Folding != cf(c)", (long)c);
            continue;
        }
        // Spot-check a couple of specific cases.
        if((full==ffi && c==0xfb03) || (full==ss && (c==0xdf || c==0x1e9e))) {
            ++countSpecific;
        }
    }
    if(countSpecific!=3) {
        errln("error: FullCaseFoldingIterator did not yield exactly the expected specific cases");
    }
    if(count<70) {
        errln("error: FullCaseFoldingIterator yielded only %d (cp, full) pairs", (int)count);
    }
}

void
StringCaseTest::assertGreekUpper(const char *s, const char *expected) {
    UnicodeString s16 = UnicodeString(s).unescape();
    UnicodeString expected16 = UnicodeString(expected).unescape();
    UnicodeString msg = UnicodeString("UnicodeString::toUpper/Greek(\"") + s16 + "\")";
    UnicodeString result16(s16);
    result16.toUpper(GREEK_LOCALE_);
    assertEquals(msg, expected16, result16);

    msg = UnicodeString("u_strToUpper/Greek(\"") + s16 + "\") cap=";
    int32_t length = expected16.length();
    int32_t capacities[] = {
        // Keep in sync with the UTF-8 capacities near the bottom of this function.
        0, length / 2, length - 1, length, length + 1
    };
    for (int32_t i = 0; i < UPRV_LENGTHOF(capacities); ++i) {
        int32_t cap = capacities[i];
        UChar *dest16 = result16.getBuffer(expected16.length() + 1);
        u_memset(dest16, 0x55AA, result16.getCapacity());
        UErrorCode errorCode = U_ZERO_ERROR;
        length = u_strToUpper(dest16, cap, s16.getBuffer(), s16.length(), "el", &errorCode);
        assertEquals(msg + cap, expected16.length(), length);
        UErrorCode expectedErrorCode;
        if (cap < expected16.length()) {
            expectedErrorCode = U_BUFFER_OVERFLOW_ERROR;
        } else if (cap == expected16.length()) {
            expectedErrorCode = U_STRING_NOT_TERMINATED_WARNING;
        } else {
            expectedErrorCode = U_ZERO_ERROR;
            assertEquals(msg + cap + " NUL", 0, dest16[length]);
        }
        assertEquals(msg + cap + " errorCode", expectedErrorCode, errorCode);
        result16.releaseBuffer(length);
        if (cap >= expected16.length()) {
            assertEquals(msg + cap, expected16, result16);
        }
    }

#if U_HAVE_STD_STRING
    UErrorCode errorCode = U_ZERO_ERROR;
    LocalUCaseMapPointer csm(ucasemap_open("el", 0, &errorCode));
    assertSuccess("ucasemap_open", errorCode);
    std::string s8;
    s16.toUTF8String(s8);
    msg = UnicodeString("ucasemap_utf8ToUpper/Greek(\"") + s16 + "\")";
    char dest8[1000];
    length = ucasemap_utf8ToUpper(csm.getAlias(), dest8, UPRV_LENGTHOF(dest8),
                                  s8.data(), s8.length(), &errorCode);
    assertSuccess("ucasemap_utf8ToUpper", errorCode);
    StringPiece result8(dest8, length);
    UnicodeString result16From8 = UnicodeString::fromUTF8(result8);
    assertEquals(msg, expected16, result16From8);

    msg += " cap=";
    capacities[1] = length / 2;
    capacities[2] = length - 1;
    capacities[3] = length;
    capacities[4] = length + 1;
    char dest8b[1000];
    int32_t expected8Length = length;  // Assuming the previous call worked.
    for (int32_t i = 0; i < UPRV_LENGTHOF(capacities); ++i) {
        int32_t cap = capacities[i];
        memset(dest8b, 0x5A, UPRV_LENGTHOF(dest8b));
        UErrorCode errorCode = U_ZERO_ERROR;
        length = ucasemap_utf8ToUpper(csm.getAlias(), dest8b, cap,
                                      s8.data(), s8.length(), &errorCode);
        assertEquals(msg + cap, expected8Length, length);
        UErrorCode expectedErrorCode;
        if (cap < expected8Length) {
            expectedErrorCode = U_BUFFER_OVERFLOW_ERROR;
        } else if (cap == expected8Length) {
            expectedErrorCode = U_STRING_NOT_TERMINATED_WARNING;
        } else {
            expectedErrorCode = U_ZERO_ERROR;
            assertEquals(msg + cap + " NUL", 0, dest8b[length]);
        }
        assertEquals(msg + cap + " errorCode", expectedErrorCode, errorCode);
        if (cap >= expected8Length) {
            assertEquals(msg + cap + " (memcmp)", 0, memcmp(dest8, dest8b, expected8Length));
        }
    }
#endif
}

void
StringCaseTest::TestGreekUpper() {
    // See UCharacterCaseTest.java for human-readable strings.

    // http://bugs.icu-project.org/trac/ticket/5456
    assertGreekUpper("\\u03AC\\u03B4\\u03B9\\u03BA\\u03BF\\u03C2, "
                     "\\u03BA\\u03B5\\u03AF\\u03BC\\u03B5\\u03BD\\u03BF, "
                     "\\u03AF\\u03C1\\u03B9\\u03B4\\u03B1",
                     "\\u0391\\u0394\\u0399\\u039A\\u039F\\u03A3, "
                     "\\u039A\\u0395\\u0399\\u039C\\u0395\\u039D\\u039F, "
                     "\\u0399\\u03A1\\u0399\\u0394\\u0391");
    // https://bugzilla.mozilla.org/show_bug.cgi?id=307039
    // https://bug307039.bmoattachments.org/attachment.cgi?id=194893
    assertGreekUpper("\\u03A0\\u03B1\\u03C4\\u03AC\\u03C4\\u03B1",
                     "\\u03A0\\u0391\\u03A4\\u0391\\u03A4\\u0391");
    assertGreekUpper("\\u0391\\u03AD\\u03C1\\u03B1\\u03C2, "
                     "\\u039C\\u03C5\\u03C3\\u03C4\\u03AE\\u03C1\\u03B9\\u03BF, "
                     "\\u03A9\\u03C1\\u03B1\\u03AF\\u03BF",
                     "\\u0391\\u0395\\u03A1\\u0391\\u03A3, "
                     "\\u039C\\u03A5\\u03A3\\u03A4\\u0397\\u03A1\\u0399\\u039F, "
                     "\\u03A9\\u03A1\\u0391\\u0399\\u039F");
    assertGreekUpper("\\u039C\\u03B1\\u0390\\u03BF\\u03C5, \\u03A0\\u03CC\\u03C1\\u03BF\\u03C2, "
                     "\\u03A1\\u03CD\\u03B8\\u03BC\\u03B9\\u03C3\\u03B7",
                     "\\u039C\\u0391\\u03AA\\u039F\\u03A5, \\u03A0\\u039F\\u03A1\\u039F\\u03A3, "
                     "\\u03A1\\u03A5\\u0398\\u039C\\u0399\\u03A3\\u0397");
    assertGreekUpper("\\u03B0, \\u03A4\\u03B7\\u03C1\\u03CE, \\u039C\\u03AC\\u03B9\\u03BF\\u03C2",
                     "\\u03AB, \\u03A4\\u0397\\u03A1\\u03A9, \\u039C\\u0391\\u03AA\\u039F\\u03A3");
    assertGreekUpper("\\u03AC\\u03C5\\u03BB\\u03BF\\u03C2",
                     "\\u0391\\u03AB\\u039B\\u039F\\u03A3");
    assertGreekUpper("\\u0391\\u03AB\\u039B\\u039F\\u03A3",
                     "\\u0391\\u03AB\\u039B\\u039F\\u03A3");
    assertGreekUpper("\\u0386\\u03BA\\u03BB\\u03B9\\u03C4\\u03B1 "
                     "\\u03C1\\u03AE\\u03BC\\u03B1\\u03C4\\u03B1 \\u03AE "
                     "\\u03AC\\u03BA\\u03BB\\u03B9\\u03C4\\u03B5\\u03C2 "
                     "\\u03BC\\u03B5\\u03C4\\u03BF\\u03C7\\u03AD\\u03C2",
                     "\\u0391\\u039A\\u039B\\u0399\\u03A4\\u0391 "
                     "\\u03A1\\u0397\\u039C\\u0391\\u03A4\\u0391 \\u0397\\u0301 "
                     "\\u0391\\u039A\\u039B\\u0399\\u03A4\\u0395\\u03A3 "
                     "\\u039C\\u0395\\u03A4\\u039F\\u03A7\\u0395\\u03A3");
    // http://www.unicode.org/udhr/d/udhr_ell_monotonic.html
    assertGreekUpper("\\u0395\\u03C0\\u03B5\\u03B9\\u03B4\\u03AE \\u03B7 "
                     "\\u03B1\\u03BD\\u03B1\\u03B3\\u03BD\\u03CE\\u03C1\\u03B9\\u03C3\\u03B7 "
                     "\\u03C4\\u03B7\\u03C2 \\u03B1\\u03BE\\u03B9\\u03BF\\u03C0\\u03C1\\u03AD"
                     "\\u03C0\\u03B5\\u03B9\\u03B1\\u03C2",
                     "\\u0395\\u03A0\\u0395\\u0399\\u0394\\u0397 \\u0397 "
                     "\\u0391\\u039D\\u0391\\u0393\\u039D\\u03A9\\u03A1\\u0399\\u03A3\\u0397 "
                     "\\u03A4\\u0397\\u03A3 \\u0391\\u039E\\u0399\\u039F\\u03A0\\u03A1\\u0395"
                     "\\u03A0\\u0395\\u0399\\u0391\\u03A3");
    assertGreekUpper("\\u03BD\\u03BF\\u03BC\\u03B9\\u03BA\\u03BF\\u03CD \\u03AE "
                     "\\u03B4\\u03B9\\u03B5\\u03B8\\u03BD\\u03BF\\u03CD\\u03C2",
                     "\\u039D\\u039F\\u039C\\u0399\\u039A\\u039F\\u03A5 \\u0397\\u0301 "
                     "\\u0394\\u0399\\u0395\\u0398\\u039D\\u039F\\u03A5\\u03A3");
    // http://unicode.org/udhr/d/udhr_ell_polytonic.html
    assertGreekUpper("\\u1F18\\u03C0\\u03B5\\u03B9\\u03B4\\u1F74 \\u1F21 "
                     "\\u1F00\\u03BD\\u03B1\\u03B3\\u03BD\\u1F7D\\u03C1\\u03B9\\u03C3\\u03B7",
                     "\\u0395\\u03A0\\u0395\\u0399\\u0394\\u0397 \\u0397 "
                     "\\u0391\\u039D\\u0391\\u0393\\u039D\\u03A9\\u03A1\\u0399\\u03A3\\u0397");
    assertGreekUpper("\\u03BD\\u03BF\\u03BC\\u03B9\\u03BA\\u03BF\\u1FE6 \\u1F22 "
                     "\\u03B4\\u03B9\\u03B5\\u03B8\\u03BD\\u03BF\\u1FE6\\u03C2",
                     "\\u039D\\u039F\\u039C\\u0399\\u039A\\u039F\\u03A5 \\u0397\\u0301 "
                     "\\u0394\\u0399\\u0395\\u0398\\u039D\\u039F\\u03A5\\u03A3");
    // From Google bug report
    assertGreekUpper("\\u039D\\u03AD\\u03BF, "
                     "\\u0394\\u03B7\\u03BC\\u03B9\\u03BF\\u03C5\\u03C1\\u03B3\\u03AF\\u03B1",
                     "\\u039D\\u0395\\u039F, "
                     "\\u0394\\u0397\\u039C\\u0399\\u039F\\u03A5\\u03A1\\u0393\\u0399\\u0391");
    // http://crbug.com/234797
    assertGreekUpper("\\u0395\\u03BB\\u03AC\\u03C4\\u03B5 \\u03BD\\u03B1 \\u03C6\\u03AC\\u03C4\\u03B5 "
                     "\\u03C4\\u03B1 \\u03BA\\u03B1\\u03BB\\u03CD\\u03C4\\u03B5\\u03C1\\u03B1 "
                     "\\u03C0\\u03B1\\u03CA\\u03B4\\u03AC\\u03BA\\u03B9\\u03B1!",
                     "\\u0395\\u039B\\u0391\\u03A4\\u0395 \\u039D\\u0391 \\u03A6\\u0391\\u03A4\\u0395 "
                     "\\u03A4\\u0391 \\u039A\\u0391\\u039B\\u03A5\\u03A4\\u0395\\u03A1\\u0391 "
                     "\\u03A0\\u0391\\u03AA\\u0394\\u0391\\u039A\\u0399\\u0391!");
    assertGreekUpper("\\u039C\\u03B1\\u0390\\u03BF\\u03C5, \\u03C4\\u03C1\\u03CC\\u03BB\\u03B5\\u03CA",
                     "\\u039C\\u0391\\u03AA\\u039F\\u03A5, \\u03A4\\u03A1\\u039F\\u039B\\u0395\\u03AA");
    assertGreekUpper("\\u03A4\\u03BF \\u03AD\\u03BD\\u03B1 \\u03AE \\u03C4\\u03BF "
                     "\\u03AC\\u03BB\\u03BB\\u03BF.",
                     "\\u03A4\\u039F \\u0395\\u039D\\u0391 \\u0397\\u0301 \\u03A4\\u039F "
                     "\\u0391\\u039B\\u039B\\u039F.");
    // http://multilingualtypesetting.co.uk/blog/greek-typesetting-tips/
    assertGreekUpper("\\u03C1\\u03C9\\u03BC\\u03AD\\u03B9\\u03BA\\u03B1",
                     "\\u03A1\\u03A9\\u039C\\u0395\\u03AA\\u039A\\u0391");
}

void
StringCaseTest::TestLongUpper() {
    if (quick) {
        logln("not exhaustive mode: skipping this test");
        return;
    }
    // Ticket #12663, crash with an extremely long string where
    // U+0390 maps to 0399 0308 0301 so that the result is three times as long
    // and overflows an int32_t.
    int32_t length = 0x40000004;  // more than 1G UChars
    UnicodeString s(length, (UChar32)0x390, length);
    UnicodeString result;
    UChar *dest = result.getBuffer(length + 1);
    if (s.isBogus() || dest == NULL) {
        logln("Out of memory, unable to run this test on this machine.");
        return;
    }
    IcuTestErrorCode errorCode(*this, "TestLongUpper");
    int32_t destLength = u_strToUpper(dest, result.getCapacity(),
                                      s.getBuffer(), s.length(), "", errorCode);
    result.releaseBuffer(destLength);
    if (errorCode.reset() != U_INDEX_OUTOFBOUNDS_ERROR) {
        errln("expected U_INDEX_OUTOFBOUNDS_ERROR, got %s (destLength is undefined, got %ld)",
              errorCode.errorName(), (long)destLength);
    }
}

void StringCaseTest::TestMalformedUTF8() {
    // ticket #12639
    IcuTestErrorCode errorCode(*this, "TestMalformedUTF8");
    LocalUCaseMapPointer csm(ucasemap_open("en", U_TITLECASE_NO_BREAK_ADJUSTMENT, errorCode));
    if (errorCode.isFailure()) {
        errln("ucasemap_open(English) failed - %s", errorCode.errorName());
        return;
    }
    char src[1] = { (char)0x85 };  // malformed UTF-8
    char dest[3] = { 0, 0, 0 };
    int32_t destLength = ucasemap_utf8ToTitle(csm.getAlias(), dest, 3, src, 1, errorCode);
    if (errorCode.isFailure() || destLength != 1 || dest[0] != src[0]) {
        errln("ucasemap_utf8ToTitle(\\x85) failed: %s destLength=%d dest[0]=0x%02x",
              errorCode.errorName(), (int)destLength, dest[0]);
    }

    errorCode.reset();
    dest[0] = 0;
    destLength = ucasemap_utf8ToLower(csm.getAlias(), dest, 3, src, 1, errorCode);
    if (errorCode.isFailure() || destLength != 1 || dest[0] != src[0]) {
        errln("ucasemap_utf8ToLower(\\x85) failed: %s destLength=%d dest[0]=0x%02x",
              errorCode.errorName(), (int)destLength, dest[0]);
    }

    errorCode.reset();
    dest[0] = 0;
    destLength = ucasemap_utf8ToUpper(csm.getAlias(), dest, 3, src, 1, errorCode);
    if (errorCode.isFailure() || destLength != 1 || dest[0] != src[0]) {
        errln("ucasemap_utf8ToUpper(\\x85) failed: %s destLength=%d dest[0]=0x%02x",
              errorCode.errorName(), (int)destLength, dest[0]);
    }

    errorCode.reset();
    dest[0] = 0;
    destLength = ucasemap_utf8FoldCase(csm.getAlias(), dest, 3, src, 1, errorCode);
    if (errorCode.isFailure() || destLength != 1 || dest[0] != src[0]) {
        errln("ucasemap_utf8FoldCase(\\x85) failed: %s destLength=%d dest[0]=0x%02x",
              errorCode.errorName(), (int)destLength, dest[0]);
    }
}

void StringCaseTest::TestBufferOverflow() {
    // Ticket #12849, incorrect result from Title Case preflight operation, 
    // when buffer overflow error is expected.
    IcuTestErrorCode errorCode(*this, "TestBufferOverflow");
    LocalUCaseMapPointer csm(ucasemap_open("en", 0, errorCode));
    if (errorCode.isFailure()) {
        errln("ucasemap_open(English) failed - %s", errorCode.errorName());
        return;
    }

    UnicodeString data("hello world");
    int32_t result = ucasemap_toTitle(csm.getAlias(), NULL, 0, data.getBuffer(), data.length(), errorCode);
    if (errorCode.get() != U_BUFFER_OVERFLOW_ERROR || result != data.length()) {
        errln("%s:%d ucasemap_toTitle(\"hello world\") failed: "
              "expected (U_BUFFER_OVERFLOW_ERROR, %d), got (%s, %d)",
              __FILE__, __LINE__, data.length(), errorCode.errorName(), result);
    }
    errorCode.reset();

#if U_HAVE_STD_STRING
    std::string data_utf8;
    data.toUTF8String(data_utf8);
    result = ucasemap_utf8ToTitle(csm.getAlias(), NULL, 0, data_utf8.c_str(), data_utf8.length(), errorCode);
    if (errorCode.get() != U_BUFFER_OVERFLOW_ERROR || result != (int32_t)data_utf8.length()) {
        errln("%s:%d ucasemap_toTitle(\"hello world\") failed: "
              "expected (U_BUFFER_OVERFLOW_ERROR, %d), got (%s, %d)",
              __FILE__, __LINE__, data_utf8.length(), errorCode.errorName(), result);
    }
    errorCode.reset();
#endif  // U_HAVE_STD_STRING
}
