/*
*******************************************************************************
*
*   Copyright (C) 2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  listformattertest.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2012aug27
*   created by: Umesh P. Nair
*/

#include "listformattertest.h"
#include <string.h>

ListFormatterTest::ListFormatterTest() :
        prefix("Prefix: ", -1, US_INV),
        one("Alice", -1, US_INV), two("Bob", -1, US_INV),
        three("Charlie", -1, US_INV), four("Delta", -1, US_INV) {
}

void ListFormatterTest::CheckFormatting(const ListFormatter* formatter, UnicodeString data[], int32_t dataSize,
                                        const UnicodeString& expected_result) {
    UnicodeString actualResult(prefix);
    UErrorCode errorCode = U_ZERO_ERROR;
    formatter->format(data, dataSize, actualResult, errorCode);
    UnicodeString expectedStringWithPrefix = prefix + expected_result;
    if (expectedStringWithPrefix != actualResult) {
        errln(UnicodeString("Expected: |") + expectedStringWithPrefix +  "|, Actual: |" + actualResult + "|");
    }
}

void ListFormatterTest::CheckFourCases(const char* locale_string, UnicodeString one, UnicodeString two,
        UnicodeString three, UnicodeString four, UnicodeString results[4]) {
    UErrorCode errorCode = U_ZERO_ERROR;
    ListFormatter* formatter = ListFormatter::createInstance(Locale(locale_string), errorCode);
    if (formatter == NULL ||  U_FAILURE(errorCode)) {
        errln("Allocation problem\n");
        return;
    }
    UnicodeString input1[] = {one};
    CheckFormatting(formatter, input1, 1, results[0]);

    UnicodeString input2[] = {one, two};
    CheckFormatting(formatter, input2, 2, results[1]);

    UnicodeString input3[] = {one, two, three};
    CheckFormatting(formatter, input3, 3, results[2]);

    UnicodeString input4[] = {one, two, three, four};
    CheckFormatting(formatter, input4, 4, results[3]);

    delete formatter;
}


void ListFormatterTest::TestLocaleFallback() {
    const char* testData[][4] = {
        {"en_US", "en", "", ""},    // ULocale.getFallback("") should return ""
        {"EN_us_Var", "en_US", "en", ""},   // Case is always normalized
        {"de_DE@collation=phonebook", "de@collation=phonebook", "@collation=phonebook", "@collation=phonebook"},    // Keyword is preserved
        {"en__POSIX", "en", "", ""},    // Trailing empty segment should be truncated
        {"_US_POSIX", "_US", "", ""},   // Same as above
        {"root", "", "", ""},               // No canonicalization
    };
    for (int i = 0; i < 6; ++i) {
        for(int j = 1; j < 4; ++j) {
            Locale in(testData[i][j-1]);
            Locale out;
            UErrorCode errorCode = U_ZERO_ERROR;
            ListFormatter::getFallbackLocale(in, out, errorCode);
            if (U_FAILURE(errorCode)) {
                errln("Error in getLocaleFallback: %s", u_errorName(errorCode));
            }

            if (::strcmp(testData[i][j], out.getName())) {
                errln("Expected: |%s|, Actual: |%s|\n", testData[i][j], out.getName());
            }
        }
    }
}

void ListFormatterTest::TestRoot() {
    UnicodeString results[4] = {
        one,
        one + ", " + two,
        one + ", " + two + ", " + three,
        one + ", " + two + ", " + three + ", " + four
    };

    CheckFourCases("", one, two, three, four, results);
}

// Bogus locale should fallback to root.
void ListFormatterTest::TestBogus() {
    UnicodeString results[4] = {
        one,
        one + ", " + two,
        one + ", " + two + ", " + three,
        one + ", " + two + ", " + three + ", " + four
    };

    CheckFourCases("ex_PY", one, two, three, four, results);
}

// Formatting in English.
// "and" is used before the last element, and all elements up to (and including) the penultimate are followed by a comma.
void ListFormatterTest::TestEnglish() {
    UnicodeString results[4] = {
        one,
        one + " and " + two,
        one + ", " + two + ", and " + three,
        one + ", " + two + ", " + three + ", and " + four
    };

    CheckFourCases("en", one, two, three, four, results);
}

void ListFormatterTest::TestEnglishUS() {
    UnicodeString results[4] = {
        one,
        one + " and " + two,
        one + ", " + two + ", and " + three,
        one + ", " + two + ", " + three + ", and " + four
    };

    CheckFourCases("en_US", one, two, three, four, results);
}

// Formatting in Russian.
// "\\u0438" is used before the last element, and all elements up to (but not including) the penultimate are followed by a comma.
void ListFormatterTest::TestRussian() {
    UnicodeString and_string = UnicodeString(" \\u0438 ", -1, US_INV).unescape();
    UnicodeString results[4] = {
        one,
        one + and_string + two,
        one + ", " + two + and_string + three,
        one + ", " + two + ", " + three + and_string + four
    };

    CheckFourCases("ru", one, two, three, four, results);
}

// Formatting in Malayalam.
// For two elements, "\\u0d15\\u0d42\\u0d1f\\u0d3e\\u0d24\\u0d46" is inserted in between.
// For more than two elements, comma is inserted between all elements up to (and including) the penultimate,
// and the word \\u0d0e\\u0d28\\u0d4d\\u0d28\\u0d3f\\u0d35 is inserted in the end.
void ListFormatterTest::TestMalayalam() {
    UnicodeString pair_string = UnicodeString(" \\u0d15\\u0d42\\u0d1f\\u0d3e\\u0d24\\u0d46 ", -1, US_INV).unescape();
    UnicodeString total_string = UnicodeString(" \\u0d0e\\u0d28\\u0d4d\\u0d28\\u0d3f\\u0d35", -1, US_INV).unescape();
    UnicodeString results[4] = {
        one,
        one + pair_string + two,
        one + ", " + two + ", " + three + total_string,
        one + ", " + two + ", " + three + ", " + four + total_string
    };

    CheckFourCases("ml", one, two, three, four, results);
}

// Formatting in Zulu.
// "and" is used before the last element, and all elements up to (and including) the penultimate are followed by a comma.
void ListFormatterTest::TestZulu() {
    UnicodeString results[4] = {
        one,
        "I-" + one + " ne-" + two,
        one + ", " + two + ", no-" + three,
        one + ", " + two + ", " + three + ", no-" + four
    };

    CheckFourCases("zu", one, two, three, four, results);
}

void ListFormatterTest::TestOutOfOrderPatterns() {
    UnicodeString results[4] = {
        one,
        two + " after " + one,
        three + " in the last after " + two + " after the first " + one,
        four + " in the last after " + three + " after " + two + " after the first " + one
    };

    UErrorCode errorCode = U_ZERO_ERROR;
    ListFormatData data("{1} after {0}", "{1} after the first {0}",
                        "{1} after {0}", "{1} in the last after {0}");
    ListFormatter formatter(data);

    UnicodeString input1[] = {one};
    CheckFormatting(&formatter, input1, 1, results[0]);

    UnicodeString input2[] = {one, two};
    CheckFormatting(&formatter, input2, 2, results[1]);

    UnicodeString input3[] = {one, two, three};
    CheckFormatting(&formatter, input3, 3, results[2]);

    UnicodeString input4[] = {one, two, three, four};
    CheckFormatting(&formatter, input4, 4, results[3]);
}

void ListFormatterTest::runIndexedTest(int32_t index, UBool exec,
                                       const char* &name, char* /*par */) {
    switch(index) {
        case 0: name = "TestRoot"; if (exec) TestRoot(); break;
        case 1: name = "TestBogus"; if (exec) TestBogus(); break;
        case 2: name = "TestEnglish"; if (exec) TestEnglish(); break;
        case 3: name = "TestEnglishUS"; if (exec) TestEnglishUS(); break;
        case 4: name = "TestRussian"; if (exec) TestRussian(); break;
        case 5: name = "TestMalayalam"; if (exec) TestMalayalam(); break;
        case 6: name = "TestZulu"; if (exec) TestZulu(); break;
        case 7: name = "TestLocaleFallback"; if (exec) TestLocaleFallback(); break;
        case 8: name = "TestOutOfOrderPatterns"; if (exec) TestLocaleFallback(); break;

        default: name = ""; break;
    }
}
