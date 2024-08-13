// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfcppitertest.cpp
// created: 2024aug12 Markus W. Scherer

#include <string_view>

#include "unicode/utypes.h"
#include "unicode/utf16cppiter.h"
#include "intltest.h"

// Makes u"literal"sv std::u16string_view literals possible.
// https://en.cppreference.com/w/cpp/string/basic_string_view/operator%22%22sv
using namespace std::string_view_literals;

using U_HEADER_ONLY_NAMESPACE::U16_BEHAVIOR_NEGATIVE;
using U_HEADER_ONLY_NAMESPACE::U16Iterator;
using U_HEADER_ONLY_NAMESPACE::U16OneSeq;

class U16IteratorTest : public IntlTest {
public:
    U16IteratorTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=nullptr) override;

    void testExperiment();
};

extern IntlTest *createU16IteratorTest() {
    return new U16IteratorTest();
}

void U16IteratorTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite U16IteratorTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testExperiment);
    TESTCASE_AUTO_END;
}

void U16IteratorTest::testExperiment() {
    IcuTestErrorCode errorCode(*this, "testExperiment");
    std::u16string_view good(u"abçカ🚴"sv);
    const char16_t *goodLimit = good.data() + good.length();
    U16Iterator<char16_t, U16_BEHAVIOR_NEGATIVE> goodIter(good.data(), good.data(), goodLimit);
    assertEquals("goodIter[0] * codePoint()", u'a', (*goodIter).codePoint());
    ++goodIter;  // pre-increment
    assertEquals("goodIter[1] * codePoint()", u'b', (*goodIter).codePoint());
    ++goodIter;
    assertEquals("goodIter[2] * codePoint()", u'ç', (*goodIter++).codePoint());  // post-increment
    assertEquals("goodIter[3] * codePoint()", u'カ', (*goodIter).codePoint());
    ++goodIter;
    const U16OneSeq<char16_t> &seq = *goodIter++;
    assertEquals("goodIter[4] * codePoint()", U'🚴', seq.codePoint());
    assertEquals("goodIter[4] * length()", 2, seq.length());
    assertTrue("goodIter[4] * stringView()", seq.stringView() == u"🚴"sv);
    U16Iterator<char16_t, U16_BEHAVIOR_NEGATIVE> goodEndIter(good.data(), goodLimit, goodLimit);
    assertTrue("goodIter == goodEndIter", goodIter == goodEndIter);

    // TODO: test ill-formed, and much more...
}
