// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfitertest.cpp
// created: 2024aug12 Markus W. Scherer

#include <string_view>

// Test header-only ICU C++ APIs. Do not use other ICU C++ APIs.
// Non-default configuration:
#define U_SHOW_CPLUSPLUS_API 0
// Default configuration:
// #define U_SHOW_CPLUSPLUS_HEADER_API 1

#include "unicode/utypes.h"
#include "unicode/utfiter.h"
#include "intltest.h"

// Makes u"literal"sv std::u16string_view literals possible.
// https://en.cppreference.com/w/cpp/string/basic_string_view/operator%22%22sv
using namespace std::string_view_literals;

using U_HEADER_ONLY_NAMESPACE::U16Iterator;
using U_HEADER_ONLY_NAMESPACE::U16StringCodePoints;

template<typename Unit>
class FwdIter {
public:
    typedef Unit value_type;

    FwdIter(const Unit *data) : p(data) {}

    bool operator==(const FwdIter &other) const { return p == other.p; }
    bool operator!=(const FwdIter &other) const { return !operator==(other); }

    Unit operator*() const { return *p; }
    FwdIter &operator++() {  // pre-increment
        ++p;
        return *this;
    }
    FwdIter operator++(int) {  // post-increment
        FwdIter result(*this);
        ++p;
        return result;
    }

private:
    const Unit *p;
};

class U16IteratorTest : public IntlTest {
public:
    U16IteratorTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=nullptr) override;

    void testGood();
    void testNegative();
    void testFFFD();
    void testSurrogate();
    void testFwdIter();
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
    TESTCASE_AUTO(testFwdIter);
    TESTCASE_AUTO_END;
}

void U16IteratorTest::testGood() {
    IcuTestErrorCode errorCode(*this, "testGood");
    std::u16string_view good(u"abçカ🚴"sv);
    U16StringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    assertTrue("iter[1] * stringView()", units.stringView() == u"b"sv);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] * codePoint", u'カ', (*iter).codePoint());
    ++iter;
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 2, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter[4] * stringView()", units.stringView() == u"🚴"sv);
    assertTrue("iter == endIter", iter == range.end());
}

void U16IteratorTest::testNegative() {
    IcuTestErrorCode errorCode(*this, "testNegative");
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    U16StringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", -1, units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertFalse("iter[1] * wellFormed", units.wellFormed());
    auto sv = units.stringView();
    assertEquals("iter[1] * stringView().length()", 1, sv.length());
    assertEquals("iter[1] * stringView()[0]", 0xd900, sv[0]);
    // TODO: test units.data()
    ++iter;
    assertEquals("iter[2] * codePoint", u'b', (*iter++).codePoint());  // post-increment
    units = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", -1, units.codePoint());
    assertFalse("iter[3] * wellFormed", units.wellFormed());
    assertEquals("iter[4] * stringView()", u"ç", (*iter++).stringView());  // post-increment
    assertTrue("iter == endIter", iter == range.end());
}

void U16IteratorTest::testFFFD() {
    IcuTestErrorCode errorCode(*this, "testFFFD");
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    U16StringCodePoints<char16_t, char32_t, U_BEHAVIOR_FFFD> range(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", 0xfffd, units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertFalse("iter[1] * wellFormed", units.wellFormed());
    auto sv = units.stringView();
    assertEquals("iter[1] * stringView().length()", 1, sv.length());
    assertEquals("iter[1] * stringView()[0]", 0xd900, sv[0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'b', (*iter++).codePoint());  // post-increment
    units = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", 0xfffd, units.codePoint());
    assertFalse("iter[3] * wellFormed", units.wellFormed());
    assertEquals("iter[4] * stringView()", u"ç", (*iter++).stringView());  // post-increment
    assertTrue("iter == endIter", iter == range.end());
}

void U16IteratorTest::testSurrogate() {
    IcuTestErrorCode errorCode(*this, "testSurrogate");
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    U16StringCodePoints<char16_t, uint32_t, U_BEHAVIOR_SURROGATE> range(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", 0xd900, units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertFalse("iter[1] * wellFormed", units.wellFormed());
    auto sv = units.stringView();
    assertEquals("iter[1] * stringView().length()", 1, sv.length());
    assertEquals("iter[1] * stringView()[0]", 0xd900, sv[0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'b', (*iter++).codePoint());  // post-increment
    units = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", 0xdc05, units.codePoint());
    assertFalse("iter[3] * wellFormed", units.wellFormed());
    assertEquals("iter[4] * stringView()", u"ç", (*iter++).stringView());  // post-increment
    assertTrue("iter == endIter", iter == range.end());
}

void U16IteratorTest::testFwdIter() {
    IcuTestErrorCode errorCode(*this, "testFwdIter");
    std::u16string_view good(u"abçカ🚴"sv);
    FwdIter<char16_t> goodBegin(good.data());
    FwdIter<char16_t> goodLimit(good.data() + good.length());
    U16Iterator<FwdIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(
        goodBegin, goodBegin, goodLimit);
    U16Iterator<FwdIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(
        goodBegin, goodLimit, goodLimit);
    // TODO: U16StringCodePoints<FwdIter, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    assertTrue("iter[1] * data()[0]", *units.data() == u'b');
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] * codePoint", u'カ', (*iter).codePoint());
    ++iter;
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 2, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    FwdIter<char16_t> data = units.data();
    assertTrue("iter[4] * data()[0]", *data++ == u"🚴"[0]);
    assertTrue("iter[4] * data()[1]", *data == u"🚴"[1]);
    assertTrue("iter == endIter", iter == rangeLimit);
}
