/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   12/09/99    aliu        Ported from Java.
**********************************************************************
*/

#include "thcoll.h"
#include "unicode/coll.h"
#include "unicode/sortkey.h"
#include "cstring.h"
#include "filestrm.h"

/**
 * The TestDictionary test expects a file of this name, with this
 * encoding, to be present in the directory $ICU/source/test/testdata.
 */
#define TEST_FILE           "th18057.txt"
#define TEST_FILE_ENCODING  "UTF8"

/**
 * This is the most failures we show in TestDictionary.  If this number
 * is < 0, we show all failures.
 */
#define MAX_FAILURES_TO_SHOW 8

#define CASE(id,test)                 \
    case id:                          \
        name = #test;                 \
        if (exec) {                   \
            logln(#test "---");       \
            logln((UnicodeString)""); \
            test();                   \
        }                             \
        break;

CollationThaiTest::CollationThaiTest() {
    UErrorCode status = U_ZERO_ERROR;
    coll = Collator::createInstance(Locale("th", "TH", ""), status);
    if (coll && U_SUCCESS(status)) {
        coll->setStrength(Collator::TERTIARY);
    } else {
        delete coll;
        coll = 0;
    }
}

CollationThaiTest::~CollationThaiTest() {
    delete coll;
}

void CollationThaiTest::runIndexedTest(int32_t index, bool_t exec, char* &name,
                                       char* par) {
    switch (index) {
        CASE(0,TestDictionary)
        CASE(1,TestCornerCases)
        default: name = ""; break;
    }
}

/**
 * Read the external dictionary file, which is already in proper
 * sorted order, and confirm that the collator compares each line as
 * preceding the following line.
 */
void CollationThaiTest::TestDictionary(void) {
    if (coll == 0) {
        errln("Error: could not construct Thai collator");
        return;
    }

    // Read in a dictionary of Thai words
    char buffer[1024];
    uprv_strcpy(buffer, IntlTest::getTestDirectory());
    uprv_strcat(buffer, TEST_FILE);

    FileStream *in = T_FileStream_open(buffer, "r");
    if (in == 0) {
        errln((UnicodeString)"Error: could not open test file " + buffer);
        return;        
    }

    //
    // Loop through each word in the dictionary and compare it to the previous
    // word.  They should be in sorted order.
    //
    UnicodeString lastWord;
    int32_t line = 0;
    int32_t failed = 0;
    while (T_FileStream_readLine(in, buffer, sizeof(buffer)) != 0) {
        UnicodeString word(buffer, TEST_FILE_ENCODING);
        line++;

        if (word.charAt(0) == 0x23) {
            // Skip comments
            continue;
        }

        // Trim line termination characters from the end
        int32_t i = word.length()-1;
        while (i>=0 &&
               (word.charAt(i) == (UChar)13 ||
                word.charAt(i) == (UChar)10)) {
            --i;
        }
        word.truncate(i+1);

        // Skip blank lines
        if (word.length() == 0) {
            continue;
        }

        if (lastWord.length() > 0) {
            int32_t result = coll->compare(lastWord, word);

            if (result >= 0) {
                failed++;
                if (MAX_FAILURES_TO_SHOW < 0 || failed <= MAX_FAILURES_TO_SHOW) {
                    UnicodeString str;
                    UnicodeString msg =
                        UnicodeString("--------------------------------------------\n")
                        + line
                        + " compare(" + prettify(lastWord, str);
                    msg += UnicodeString(", ")
                        + prettify(word, str) + ") returned " + result
                        + ", expected -1\n";
                    UErrorCode status = U_ZERO_ERROR;
                    CollationKey k1, k2;
                    coll->getCollationKey(lastWord, k1, status);
                    coll->getCollationKey(word, k2, status);
                    if (U_FAILURE(status)) {
                        errln((UnicodeString)"Fail: getCollationKey returned " + status);
                        return;
                    }
                    msg.append("key1: ").append(prettify(k1, str)).append("\n");
                    msg.append("key2: ").append(prettify(k2, str));
                    errln(msg);
                }
            }
        }
        lastWord = word;
    }

    if (failed != 0) {
        if (failed > MAX_FAILURES_TO_SHOW) {
            errln((UnicodeString)"Too many failures; only the first " +
                  MAX_FAILURES_TO_SHOW + " failures were shown");
        }
        errln((UnicodeString)"Summary: " + failed + " of " + (line - 1) +
              " comparisons failed");
    }
}

/**
 * Odd corner conditions taken from "How to Sort Thai Without Rewriting Sort",
 * by Doug Cooper, http://seasrc.th.net/paper/thaisort.zip
 */
void CollationThaiTest::TestCornerCases(void) {
    const char* TESTS[] = {
        // Shorter words precede longer
        "\\u0e01",                               "<",    "\\u0e01\\u0e01",

        // Tone marks are considered after letters (i.e. are primary ignorable)
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e49\\u0e32",

        // ditto for other over-marks
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e32\\u0e4c",

        // commonly used mark-in-context order.
        // In effect, marks are sorted after each syllable.
        "\\u0e01\\u0e32\\u0e01\\u0e49\\u0e32",   "<",    "\\u0e01\\u0e48\\u0e32\\u0e01\\u0e49\\u0e32",

        // Hyphens and other punctuation follow whitespace but come before letters
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e32-",
        "\\u0e01\\u0e32-",                       "<",    "\\u0e01\\u0e32\\u0e01\\u0e32",

        // Doubler follows an indentical word without the doubler
        "\\u0e01\\u0e32",                        "<",    "\\u0e01\\u0e32\\u0e46",
        "\\u0e01\\u0e32\\u0e46",                 "<",    "\\u0e01\\u0e32\\u0e01\\u0e32",


        // \\u0e45 after either \\u0e24 or \\u0e26 is treated as a single
        // combining character, similar to "c < ch" in traditional spanish.
        // TODO: beef up this case
        "\\u0e24\\u0e29\\u0e35",                 "<",    "\\u0e24\\u0e45\\u0e29\\u0e35",
        "\\u0e26\\u0e29\\u0e35",                 "<",    "\\u0e26\\u0e45\\u0e29\\u0e35",

        // Vowels reorder, should compare \\u0e2d and \\u0e34
        "\\u0e40\\u0e01\\u0e2d",                 "<",    "\\u0e40\\u0e01\\u0e34",

        // Tones are compared after the rest of the word (e.g. primary ignorable)
        "\\u0e01\\u0e32\\u0e01\\u0e48\\u0e32",   "<",    "\\u0e01\\u0e49\\u0e32\\u0e01\\u0e32",

        // Periods are ignored entirely
        "\\u0e01.\\u0e01.",                      "<",    "\\u0e01\\u0e32",
    };
    const int32_t TESTS_length = sizeof(TESTS)/sizeof(TESTS[0]);

    if (coll == 0) {
        errln("Error: could not construct Thai collator");
        return;
    }
    compareArray(*coll, TESTS, TESTS_length);
}

//------------------------------------------------------------------------
// Internal utilities
//------------------------------------------------------------------------

void CollationThaiTest::compareArray(const Collator& c, const char* tests[],
                                     int32_t testsLength) {
    UErrorCode status = U_ZERO_ERROR;
    for (int32_t i = 0; i < testsLength; i += 3) {

        int32_t expect = 0;
        if (tests[i+1][0] == '<') {
            expect = -1;
        } else if (tests[i+1][0] == '>') {
            expect = 1;
        } else if (tests[i+1][0] == '=') {
            expect = 0;
        } else {
            // expect = Integer.decode(tests[i+1]).intValue();
            errln((UnicodeString)"Error: unknown operator " + tests[i+1]);
            return;
        }

        UnicodeString s1, s2;
        parseChars(s1, tests[i]);
        parseChars(s2, tests[i+2]);

        int32_t result = c.compare(s1, s2);
        if (sign(result) != sign(expect))
        {
            UnicodeString t1, t2;
            errln(UnicodeString("") +
                  i/3 + ": compare(" + prettify(s1, t1)
                  + " , " + prettify(s2, t2)
                  + ") got " + result + "; expected " + expect);

            CollationKey k1, k2;
            c.getCollationKey(s1, k1, status);
            c.getCollationKey(s2, k2, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"Fail: getCollationKey returned " + status);
                return;
            }
            errln((UnicodeString)"  key1: " + prettify(k1, t1) );
            errln((UnicodeString)"  key2: " + prettify(k2, t2) );
        }
        else
        {
            // Collator.compare worked OK; now try the collation keys
            CollationKey k1, k2;
            c.getCollationKey(s1, k1, status);
            c.getCollationKey(s2, k2, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"Fail: getCollationKey returned " + status);
                return;
            }

            result = k1.compareTo(k2);
            if (sign(result) != sign(expect)) {
                UnicodeString t1, t2;
                errln(UnicodeString("") +
                      i/3 + ": key(" + prettify(s1, t1)
                      + ").compareTo(key(" + prettify(s2, t2)
                      + ")) got " + result + "; expected " + expect);
                
                errln((UnicodeString)"  " + prettify(k1, t1) + " vs. " + prettify(k2, t2));
            }
        }
    }
}

int8_t CollationThaiTest::sign(int32_t i) {
    if (i < 0) return -1;
    if (i > 0) return 1;
    return 0;
}

/**
 * Set a UnicodeString corresponding to the given string.  Use
 * UnicodeString and the default converter, unless we see the sequence
 * "\\u", in which case we interpret the subsequent escape.
 */
UnicodeString& CollationThaiTest::parseChars(UnicodeString& result,
                                             const char* chars) {
    return result = CharsToUnicodeString(chars);
}
