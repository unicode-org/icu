/*
************************************************************************
* Copyright (c) 1997-2000, International Business Machines
* Corporation and others.  All Rights Reserved.
************************************************************************
*/

#ifndef _NORMCONF
#define _NORMCONF

#include "unicode/utypes.h"
#include "unicode/normlzr.h"
#include "intltest.h"

#define TEST_SUITE_DIR  "unidata"
#define TEST_SUITE_FILE "Draft-TestSuite.txt"

class NormalizerConformanceTest : public IntlTest {
    Normalizer normalizer;

 public:
    NormalizerConformanceTest();
    ~NormalizerConformanceTest();

    void runIndexedTest(int32_t index, UBool exec, char* &name, char* par=NULL);

    /**
     * Test the conformance of Normalizer to
     * http://www.unicode.org/unicode/reports/tr15/conformance/Draft-TestSuite.txt.
     * This file must be located at the path specified as TEST_SUITE_FILE.
     */
    void TestConformance(void);

    // Specific tests for debugging.  These are generally failures taken from
    // the conformance file, but culled out to make debugging easier.
    void TestCase6(void);

 private:
    /**
     * Verify the conformance of the given line of the Unicode
     * normalization (UTR 15) test suite file.  For each line,
     * there are five columns, corresponding to field[0]..field[4].
     *
     * The following invariants must be true for all conformant implementations
     *  c2 == NFC(c1) == NFC(c2) == NFC(c3)
     *  c3 == NFD(c1) == NFD(c2) == NFD(c3)
     *  c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)
     *  c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)
     *
     * @param field the 5 columns
     * @param line the source line from the test suite file
     * @return true if the test passes
     */
    UBool checkConformance(const UnicodeString* field,
                           const UnicodeString& line);

    void iterativeNorm(const UnicodeString& str,
                       Normalizer::EMode mode,
                       UnicodeString& result,
                       int8_t dir);

    /**
     * @param op name of normalization form, e.g., "KC"
     * @param s string being normalized
     * @param got value received
     * @param exp expected value
     * @param msg description of this test
     * @param return true if got == exp
     */
    UBool assertEqual(const UnicodeString& op,
                      const UnicodeString& s,
                      const UnicodeString& got,
                      const UnicodeString& exp,
                      const UnicodeString& msg);

    /**
     * Split a string into pieces based on the given delimiter
     * character.  Then, parse the resultant fields from hex into
     * characters.  That is, "0040 0400;0C00;0899" -> new String[] {
     * "\u0040\u0400", "\u0C00", "\u0899" }.  The output is assumed to
     * be of the proper length already, and exactly output.length
     * fields are parsed.  If there are too few an exception is
     * thrown.  If there are too many the extras are ignored.
     *
     * @param buf scratch buffer
     * @return FALSE upon failure
     */
    UBool hexsplit(const UnicodeString& s, UChar delimiter,
                   UnicodeString* output, int32_t outputLength,
                   UnicodeString& buf);

    void _testOneLine(const UnicodeString& line);
};

#endif
