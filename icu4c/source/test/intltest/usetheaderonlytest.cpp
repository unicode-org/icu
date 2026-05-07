// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// usetheaderonlytest.cpp
// created: 2024dec11 Markus W. Scherer

#include <memory>
#include <string>

// Test header-only ICU C++ APIs. Do not use other ICU C++ APIs.
// Non-default configuration:
#define U_SHOW_CPLUSPLUS_API 0
// Default configuration:
// #define U_SHOW_CPLUSPLUS_HEADER_API 1

#include "unicode/utypes.h"
#include "unicode/uset.h"
// for commented-out sample code: #include "unicode/ustring.h"
#include "unicode/utf.h"
#include "unicode/utf16.h"
#include "intltest.h"

class USetHeaderOnlyTest : public IntlTest {
public:
    USetHeaderOnlyTest() = default;

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=nullptr) override;

    void TestUSetCodePointIterator();
    void TestUSetRangeIterator();
    void TestUSetStringIterator();
    void TestUSetElementIterator();
};

extern IntlTest *createUSetHeaderOnlyTest() {
    return new USetHeaderOnlyTest();
}

void USetHeaderOnlyTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite USetHeaderOnlyTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestUSetCodePointIterator);
    TESTCASE_AUTO(TestUSetRangeIterator);
    TESTCASE_AUTO(TestUSetStringIterator);
    TESTCASE_AUTO(TestUSetElementIterator);
    TESTCASE_AUTO_END;
}

std::u16string cpString(UChar32 c) {
    if (U_IS_BMP(c)) {
        return {static_cast<char16_t>(c)};
    } else {
        return {U16_LEAD(c), U16_TRAIL(c)};
    }
}

void USetHeaderOnlyTest::TestUSetCodePointIterator() {
    IcuTestErrorCode errorCode(*this, "TestUSetCodePointIterator");
    using U_HEADER_NESTED_NAMESPACE::USetCodePoints;
    std::unique_ptr<USet, decltype(&uset_close)> uset(
        uset_openPattern(u"[abcçカ🚴]", -1, errorCode), &uset_close);
    std::u16string result;
    for (UChar32 c : USetCodePoints(uset.get())) {
        result.append(u" ").append(cpString(c));
        // Commented-out sample code for pasting into the API docs.
        // printf("uset.codePoint U+%04lx\n", (long)c);
    }
    assertEquals(WHERE, u" a b c ç カ 🚴", result);

    USetCodePoints range1(uset.get());
    auto range2(range1);  // copy constructor
    auto iter = range1.begin();
    auto limit = range2.end();
    // operator* with pre- and post-increment
    assertEquals(WHERE, u'a', *iter);
    ++iter;
    assertEquals(WHERE, u'b', *iter);
    assertEquals(WHERE, u'c', *++iter);
    auto iter2(iter);  // copy constructor
    assertEquals(WHERE, u'c', *iter2++);
    assertEquals(WHERE, u'ç', *iter2++);
    assertEquals(WHERE, u'カ', *iter2);
    assertTrue(WHERE, ++iter2 != limit);
    auto iter3(iter2++);
    assertEquals(WHERE, U'🚴', *iter3);
    assertTrue(WHERE, iter2 == limit);
}

void USetHeaderOnlyTest::TestUSetRangeIterator() {
    IcuTestErrorCode errorCode(*this, "TestUSetRangeIterator");
    using U_HEADER_NESTED_NAMESPACE::USetRanges;
    using U_HEADER_NESTED_NAMESPACE::CodePointRange;
    std::unique_ptr<USet, decltype(&uset_close)> uset(
        uset_openPattern(u"[abcçカ🚴]", -1, errorCode), &uset_close);
    std::u16string result;
    for (auto [start, end] : USetRanges(uset.get())) {
        result.append(u" ").append(cpString(start)).append(u"-").append(cpString(end));
        // Commented-out sample code for pasting into the API docs.
        // printf("uset.range U+%04lx..U+%04lx\n", (long)start, (long)end);
    }
    assertEquals(WHERE, u" a-c ç-ç カ-カ 🚴-🚴", result);
    result.clear();
    for (auto range : USetRanges(uset.get())) {
        for (UChar32 c : range) {
            result.append(u" ").append(cpString(c));
            // Commented-out sample code for pasting into the API docs.
            // printf("uset.range.c U+%04lx\n", (long)c);
        }
        result.append(u" |");
    }
    assertEquals(WHERE, u" a b c | ç | カ | 🚴 |", result);

    USetRanges range1(uset.get());
    auto range2(range1);  // copy constructor
    auto iter = range1.begin();
    auto limit = range2.end();
    // operator* with pre- and post-increment
    {
        auto cpRange = *iter;
        assertEquals(WHERE, u'a', cpRange.rangeStart);
        assertEquals(WHERE, u'c', cpRange.rangeEnd);
        assertEquals(WHERE, 3, cpRange.size());
        auto cpRange2(cpRange);
        auto cpIter = cpRange.begin();
        auto cpLimit = cpRange2.end();
        assertEquals(WHERE, u'a', *cpIter++);
        assertEquals(WHERE, u'b', *cpIter);
        assertTrue(WHERE, cpIter != cpLimit);
        CodePointRange::iterator cpIter2(u'b');  // public constructor
        assertTrue(WHERE, cpIter == cpIter2);
        assertEquals(WHERE, u'c', *++cpIter);
        assertTrue(WHERE, cpIter != cpIter2);
        assertTrue(WHERE, ++cpIter == cpLimit);
    }
    ++iter;
    auto iter2(iter);  // copy constructor
    assertEquals(WHERE, u'ç', (*iter2).rangeStart);
    assertEquals(WHERE, u'ç', (*iter2).rangeEnd);
    assertEquals(WHERE, 1, (*iter2).size());
    assertEquals(WHERE, u'ç', (*iter2++).rangeStart);
    assertEquals(WHERE, u'カ', (*iter2).rangeStart);
    assertTrue(WHERE, ++iter2 != limit);
    auto iter3(iter2++);
    assertEquals(WHERE, U'🚴', (*iter3).rangeStart);
    assertTrue(WHERE, iter2 == limit);

    {
        CodePointRange cpRange(u'h', u'k');  // public constructor
        // FYI: currently no operator==
        assertEquals(WHERE, u'h', cpRange.rangeStart);
        assertEquals(WHERE, u'k', cpRange.rangeEnd);
        assertEquals(WHERE, 4, cpRange.size());
        assertEquals(WHERE, u'i', *++(cpRange.begin()));
    }
}

void USetHeaderOnlyTest::TestUSetStringIterator() {
    IcuTestErrorCode errorCode(*this, "TestUSetStringIterator");
    using U_HEADER_NESTED_NAMESPACE::USetStrings;
    std::unique_ptr<USet, decltype(&uset_close)> uset(
        uset_openPattern(u"[abcçカ🚴{}{abc}{de}]", -1, errorCode), &uset_close);
    std::u16string result;
    for (auto s : USetStrings(uset.get())) {
        result.append(u" \"").append(s).append(u"\"");
        // Commented-out sample code for pasting into the API docs.
        // int32_t len32 = s.length();
        // char utf8[200];
        // u_strToUTF8WithSub(utf8, int32_t{sizeof(utf8) - 1}, nullptr,
        //                    s.data(), len32, 0xFFFD, nullptr, errorCode);
        // printf("uset.string length %ld \"%s\"\n", long{len32}, utf8);
    }
    assertEquals(WHERE, uR"( "" "abc" "de")", result);

    USetStrings range1(uset.get());
    auto range2(range1);  // copy constructor
    auto iter = range1.begin();
    auto limit = range2.end();
    // operator* with pre- and post-increment
    assertEquals(WHERE, u"", *iter);
    assertEquals(WHERE, u"abc", *++iter);
    auto iter2(iter);  // copy constructor
    assertEquals(WHERE, u"abc", *iter2++);
    assertTrue(WHERE, iter2 != limit);
    auto iter3(iter2++);
    assertEquals(WHERE, u"de", *iter3);
    assertTrue(WHERE, iter2 == limit);
}

void USetHeaderOnlyTest::TestUSetElementIterator() {
    IcuTestErrorCode errorCode(*this, "TestUSetElementIterator");
    using U_HEADER_NESTED_NAMESPACE::USetElements;
    std::unique_ptr<USet, decltype(&uset_close)> uset(
        uset_openPattern(u"[abcçカ🚴{}{abc}{de}]", -1, errorCode), &uset_close);
    std::u16string result;
    for (auto el : USetElements(uset.get())) {
        result.append(u" \"").append(el).append(u"\"");
        // Commented-out sample code for pasting into the API docs.
        // int32_t len32 = el.length();
        // char utf8[200];
        // u_strToUTF8WithSub(utf8, int32_t{sizeof(utf8) - 1}, nullptr,
        //                    el.data(), len32, 0xFFFD, nullptr, errorCode);
        // printf("uset.element length %ld \"%s\"\n", long{len32}, utf8);
    }
    assertEquals(WHERE, uR"( "a" "b" "c" "ç" "カ" "🚴" "" "abc" "de")", result);

    USetElements range1(uset.get());
    auto range2(range1);  // copy constructor
    auto iter = range1.begin();
    auto limit = range2.end();
    // operator* with pre- and post-increment
    assertEquals(WHERE, u"a", *iter);
    ++iter;
    assertEquals(WHERE, u"b", *iter);
    assertEquals(WHERE, u"c", *++iter);
    auto iter2(iter);  // copy constructor
    assertEquals(WHERE, u"c", *iter2++);
    // skip çカ🚴
    ++++++iter2;
    assertEquals(WHERE, u"", *iter2++);
    assertEquals(WHERE, u"abc", *iter2);
    assertTrue(WHERE, ++iter2 != limit);
    auto iter3(iter2++);
    assertEquals(WHERE, u"de", *iter3);
    assertTrue(WHERE, iter2 == limit);
}
