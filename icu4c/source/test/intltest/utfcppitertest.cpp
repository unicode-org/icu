// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfcppitertest.cpp
// created: 2024aug12 Markus W. Scherer

#include <string_view>

// Test header-only ICU C++ APIs. Do not use other ICU C++ APIs.
// Non-default configuration:
#define U_SHOW_CPLUSPLUS_API 0
// Default configuration:
// #define U_SHOW_CPLUSPLUS_HEADER_API 1

#include "unicode/utypes.h"
#include "unicode/utf16cppiter.h"
#include "intltest.h"

// Makes u"literal"sv std::u16string_view literals possible.
// https://en.cppreference.com/w/cpp/string/basic_string_view/operator%22%22sv
using namespace std::string_view_literals;

using U_HEADER_ONLY_NAMESPACE::U16_BEHAVIOR_NEGATIVE;
using U_HEADER_ONLY_NAMESPACE::U16_BEHAVIOR_FFFD;
using U_HEADER_ONLY_NAMESPACE::U16_BEHAVIOR_SURROGATE;
using U_HEADER_ONLY_NAMESPACE::U16Iterator;
using U_HEADER_ONLY_NAMESPACE::U16OneSeq;

class U16IteratorTest : public IntlTest {
public:
    U16IteratorTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=nullptr) override;

    void testGood();
    void testNegative();
    void testFFFD();
    void testSurrogate();
};

extern IntlTest *createU16IteratorTest() {
    return new U16IteratorTest();
}

void U16IteratorTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite U16IteratorTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGood);
    TESTCASE_AUTO(testNegative);
    TESTCASE_AUTO(testFFFD);
    TESTCASE_AUTO(testSurrogate);
    TESTCASE_AUTO_END;
}

void U16IteratorTest::testGood() {
    IcuTestErrorCode errorCode(*this, "testGood");
    std::u16string_view good(u"abçカ🚴"sv);
    const char16_t *limit = good.data() + good.length();
    U16Iterator<char16_t, U16_BEHAVIOR_NEGATIVE> iter(good.data(), good.data(), limit);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint);
    ++iter;  // pre-increment
    U16OneSeq<char16_t> seq = *iter;
    assertEquals("iter[1] * codePoint", u'b', seq.codePoint);
    assertEquals("iter[1] * length", 1, seq.length);
    assertTrue("iter[1] * isWellFormed", seq.isWellFormed);
    assertTrue("iter[1] * stringView()", seq.stringView() == u"b"sv);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint);  // post-increment
    assertEquals("iter[3] * codePoint", u'カ', (*iter).codePoint);
    ++iter;
    seq = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', seq.codePoint);
    assertEquals("iter[4] * length", 2, seq.length);
    assertTrue("iter[4] * isWellFormed", seq.isWellFormed);
    assertTrue("iter[4] * stringView()", seq.stringView() == u"🚴"sv);
    U16Iterator<char16_t, U16_BEHAVIOR_NEGATIVE> endIter(good.data(), limit, limit);
    assertTrue("iter == endIter", iter == endIter);
}

void U16IteratorTest::testNegative() {
    IcuTestErrorCode errorCode(*this, "testNegative");
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    const char16_t *limit = bad.data() + bad.length();
    U16Iterator<char16_t, U16_BEHAVIOR_NEGATIVE> iter(bad.data(), bad.data(), limit);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint);
    ++iter;  // pre-increment
    U16OneSeq<char16_t> seq = *iter;
    assertEquals("iter[1] * codePoint", -1, seq.codePoint);
    assertEquals("iter[1] * length", 1, seq.length);
    assertFalse("iter[1] * isWellFormed", seq.isWellFormed);
    auto sv = seq.stringView();
    assertEquals("iter[1] * stringView().length()", 1, sv.length());
    assertEquals("iter[1] * stringView()[0]", 0xd900, sv[0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'b', (*iter++).codePoint);  // post-increment
    seq = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", -1, seq.codePoint);
    assertFalse("iter[3] * isWellFormed", seq.isWellFormed);
    assertEquals("iter[4] * stringView()", u"ç", (*iter++).stringView());  // post-increment
    U16Iterator<char16_t, U16_BEHAVIOR_NEGATIVE> endIter(bad.data(), limit, limit);
    assertTrue("iter == endIter", iter == endIter);
}

void U16IteratorTest::testFFFD() {
    IcuTestErrorCode errorCode(*this, "testFFFD");
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    const char16_t *limit = bad.data() + bad.length();
    U16Iterator<char16_t, U16_BEHAVIOR_FFFD> iter(bad.data(), bad.data(), limit);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint);
    ++iter;  // pre-increment
    U16OneSeq<char16_t> seq = *iter;
    assertEquals("iter[1] * codePoint", 0xfffd, seq.codePoint);
    assertEquals("iter[1] * length", 1, seq.length);
    assertFalse("iter[1] * isWellFormed", seq.isWellFormed);
    auto sv = seq.stringView();
    assertEquals("iter[1] * stringView().length()", 1, sv.length());
    assertEquals("iter[1] * stringView()[0]", 0xd900, sv[0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'b', (*iter++).codePoint);  // post-increment
    seq = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", 0xfffd, seq.codePoint);
    assertFalse("iter[3] * isWellFormed", seq.isWellFormed);
    assertEquals("iter[4] * stringView()", u"ç", (*iter++).stringView());  // post-increment
    U16Iterator<char16_t, U16_BEHAVIOR_FFFD> endIter(bad.data(), limit, limit);
    assertTrue("iter == endIter", iter == endIter);
}

void U16IteratorTest::testSurrogate() {
    IcuTestErrorCode errorCode(*this, "testSurrogate");
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    const char16_t *limit = bad.data() + bad.length();
    U16Iterator<char16_t, U16_BEHAVIOR_SURROGATE> iter(bad.data(), bad.data(), limit);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint);
    ++iter;  // pre-increment
    U16OneSeq<char16_t> seq = *iter;
    assertEquals("iter[1] * codePoint", 0xd900, seq.codePoint);
    assertEquals("iter[1] * length", 1, seq.length);
    assertFalse("iter[1] * isWellFormed", seq.isWellFormed);
    auto sv = seq.stringView();
    assertEquals("iter[1] * stringView().length()", 1, sv.length());
    assertEquals("iter[1] * stringView()[0]", 0xd900, sv[0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'b', (*iter++).codePoint);  // post-increment
    seq = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", 0xdc05, seq.codePoint);
    assertFalse("iter[3] * isWellFormed", seq.isWellFormed);
    assertEquals("iter[4] * stringView()", u"ç", (*iter++).stringView());  // post-increment
    U16Iterator<char16_t, U16_BEHAVIOR_SURROGATE> endIter(bad.data(), limit, limit);
    assertTrue("iter == endIter", iter == endIter);
}
