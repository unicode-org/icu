/*
**********************************************************************
*   Copyright (C) 1999 Alan Liu and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   10/20/99    alan        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "usettest.h"
#include "unicode/uniset.h"

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break;

void
UnicodeSetTest::runIndexedTest(int32_t index, bool_t exec,
                               char* &name, char* par) {
    // if (exec) logln((UnicodeString)"TestSuite UnicodeSetTest");
    switch (index) {
        CASE(0,TestPatterns)
        CASE(1,TestAddRemove)

        default: name = ""; break;
    }
}

void
UnicodeSetTest::TestPatterns(void) {
    UnicodeSet set;
    expectPattern(set, "[[a-m]&[d-z]&[k-y]]",  "km");
    expectPattern(set, "[[a-z]-[m-y]-[d-r]]",  "aczz");
    expectPattern(set, "[a\\-z]",  "--aazz");
    expectPattern(set, "[-az]",  "--aazz");
    expectPattern(set, "[az-]",  "--aazz");
    expectPattern(set, "[[[a-z]-[aeiou]i]]", "bdfnptvz");

    // Throw in a test of complement
    set.complement();
    UnicodeString exp;
    exp.append((UChar)0x0000).append("aeeoouu").append((UChar)('z'+1)).append((UChar)0xFFFF);
    expectPairs(set, exp);
}

void
UnicodeSetTest::TestAddRemove(void) {
	UErrorCode status = U_ZERO_ERROR;

    UnicodeSet set; // Construct empty set
    set.add('a', 'z');
    expectPairs(set, "az");
    set.remove('m', 'p');
    expectPairs(set, "alqz");
    set.remove('e', 'g');
    expectPairs(set, "adhlqz");
    set.remove('d', 'i');
    expectPairs(set, "acjlqz");
    set.remove('c', 'r');
    expectPairs(set, "absz");
    set.add('f', 'q');
    expectPairs(set, "abfqsz");
    set.remove('a', 'g');
    expectPairs(set, "hqsz");
    set.remove('a', 'z');
    expectPairs(set, "");

    // Try removing an entire set from another set
    expectPattern(set, "[c-x]", "cx");
    UnicodeSet set2;
    expectPattern(set2, "[f-ky-za-bc[vw]]", "acfkvwyz");
    set.removeAll(set2);
    expectPairs(set, "deluxx");

    // Try adding an entire set to another set
    expectPattern(set, "[jackiemclean]", "aacceein");
    expectPattern(set2, "[hitoshinamekatajamesanderson]", "aadehkmort");
    set.addAll(set2);
    expectPairs(set, "aacehort");

    // Test commutativity
    expectPattern(set, "[hitoshinamekatajamesanderson]", "aadehkmort");
    expectPattern(set2, "[jackiemclean]", "aacceein");
    set.addAll(set2);
    expectPairs(set, "aacehort");
}

void
UnicodeSetTest::expectPattern(UnicodeSet& set,
                              const UnicodeString& pattern,
                              const UnicodeString& expectedPairs) {
    UErrorCode status = U_ZERO_ERROR;
    set.applyPattern(pattern, status);
	if (U_FAILURE(status)) {
		errln(UnicodeString("FAIL: applyPattern(\"") + pattern +
              "\") failed");
		return;
	} else {
        if (set.getPairs() != expectedPairs) {
            errln(UnicodeString("FAIL: applyPattern(\"") + pattern +
                  "\") => pairs \"" +
                  escape(set.getPairs()) + "\", expected \"" +
                  escape(expectedPairs) + "\"");
        } else {
            logln(UnicodeString("Ok:   applyPattern(\"") + pattern +
                  "\") => pairs \"" +
                  escape(set.getPairs()) + "\"");
        }
    }
}

void
UnicodeSetTest::expectPairs(const UnicodeSet& set, const UnicodeString& expectedPairs) {
    if (set.getPairs() != expectedPairs) {
        errln(UnicodeString("FAIL: Expected pair list \"") +
              escape(expectedPairs) + "\", got \"" +
              escape(set.getPairs()) + "\"");
    }
}

static UChar toHexString(int32_t i) { return i + (i < 10 ? '0' : ('A' - 10)); }

UnicodeString
UnicodeSetTest::escape(const UnicodeString& s) {
    UnicodeString buf;
    for (int32_t i=0; i<s.length(); ++i)
    {
        UChar c = s[(UTextOffset)i];
        if (' ' <= c && c <= (UChar)0x7F) {
            buf += c;
        } else {
            buf += '\\'; buf += 'u';
            buf += toHexString((c & 0xF000) >> 12);
            buf += toHexString((c & 0x0F00) >> 8);
            buf += toHexString((c & 0x00F0) >> 4);
            buf += toHexString(c & 0x000F);
        }
    }
    return buf;
}
