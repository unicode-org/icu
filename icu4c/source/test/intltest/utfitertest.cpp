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

using U_HEADER_ONLY_NAMESPACE::UTFIterator;
using U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints;

// Shared state for one or more copies of single-pass iterators.
// Similar to https://en.cppreference.com/w/cpp/iterator/istreambuf_iterator
// but the iterators only implement LegacyIterator (* and ++) without post-increment.
template<typename Unit>
class SinglePassSource {
public:
    SinglePassSource(std::basic_string_view<Unit> s) : p(s.data()), limit(s.data() + s.length()) {}

private:
    template<typename U>
    friend class SinglePassIter;

    const Unit *p;  // incremented by iterators
    const Unit *limit;
};

template<typename Unit>
class SinglePassIter {
public:
    typedef Unit value_type;
    typedef Unit &reference;
    typedef Unit *pointer;
    typedef ssize_t difference_type;
    // This is a LegacyIterator but there is no specific category for that,
    // so we claim it to be a LegacyInputIterator. It *is* single-pass.
    typedef std::input_iterator_tag iterator_category;

    SinglePassIter(SinglePassSource<Unit> &src) : src(&src) {}
    // limit sentinel
    SinglePassIter() : src(nullptr) {}

    // TODO: try to delete the copy constructor/assignment?

    bool operator==(const SinglePassIter &other) const {
        bool done = isDone();
        bool otherDone = other.isDone();
        return done ? otherDone : (!otherDone && src->p == other.src->p);
    }
    bool operator!=(const SinglePassIter &other) const { return !operator==(other); }

    Unit operator*() const { return *(src->p); }
    SinglePassIter &operator++() {  // pre-increment
        ++(src->p);
        return *this;
    }
    // *no* post-increment

private:
    bool isDone() const { return src == nullptr || src->p == src->limit; }

    SinglePassSource<Unit> *src;
};

// TODO: still needed once we test with both SinglePassIter and a bidirectional iter?
template<typename Unit>
class FwdIter {
public:
    typedef Unit value_type;
    typedef Unit &reference;
    typedef Unit *pointer;
    typedef ssize_t difference_type;
    // https://en.cppreference.com/w/cpp/named_req/ForwardIterator#Multi-pass_guarantee
    typedef std::forward_iterator_tag iterator_category;

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
    void testSinglePassIter();
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
    TESTCASE_AUTO(testSinglePassIter);
    TESTCASE_AUTO(testFwdIter);
    TESTCASE_AUTO_END;
}

void U16IteratorTest::testGood() {
    std::u16string_view good(u"abçカ🚴"sv);
    UTFStringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    // TODO: Try to un-hardcode the iterator types in these checks via declspec.
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<char16_t *, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::bidirectional_iterator_tag>);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    assertTrue("iter[1] * stringView()", units.stringView() == u"b"sv);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 2, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter[4] * stringView()", units.stringView() == u"🚴"sv);
    assertTrue("iter == endIter", iter == range.end());
}

void U16IteratorTest::testNegative() {
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    UTFStringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
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
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    UTFStringCodePoints<char16_t, char32_t, U_BEHAVIOR_FFFD> range(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
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
    static const char16_t badChars[] = { u'a', 0xd900, u'b', 0xdc05, u'ç' };
    std::u16string_view bad(badChars, 5);
    UTFStringCodePoints<char16_t, uint32_t, U_BEHAVIOR_SURROGATE> range(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
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

void U16IteratorTest::testSinglePassIter() {
    SinglePassSource<char16_t> good(u"abçカ🚴"sv);
    SinglePassIter<char16_t> goodBegin(good);
    SinglePassIter<char16_t> goodLimit{};
    UTFIterator<SinglePassIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(
        goodBegin, goodLimit);
    UTFIterator<SinglePassIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(goodLimit);
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<SinglePassIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::input_iterator_tag>);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    // No data() for a single-pass unit iterator.
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 2, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter == endIter", iter == rangeLimit);
}

void U16IteratorTest::testFwdIter() {
    std::u16string_view good(u"abçカ🚴"sv);
    FwdIter<char16_t> goodBegin(good.data());
    FwdIter<char16_t> goodLimit(good.data() + good.length());
    UTFIterator<FwdIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(goodBegin, goodLimit);
    UTFIterator<FwdIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(goodLimit);
    // TODO: UTFStringCodePoints<FwdIter, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<FwdIter<char16_t>, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::forward_iterator_tag>);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    assertTrue("iter[1] * data()[0]", *units.data() == u'b');
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 2, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    FwdIter<char16_t> data = units.data();
    assertTrue("iter[4] * data()[0]", *data++ == u"🚴"[0]);
    assertTrue("iter[4] * data()[1]", *data == u"🚴"[1]);
    assertTrue("iter == endIter", iter == rangeLimit);
}

// TODO: test back & forth with bidirectional iterator (not random access, not contiguous)

class U8IteratorTest : public IntlTest {
public:
    U8IteratorTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=nullptr) override;

    void testGood();
    void testNegative();
    void testFFFD();
    void testSinglePassIter();
    void testFwdIter();
};

extern IntlTest *createU8IteratorTest() {
    return new U8IteratorTest();
}

void U8IteratorTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite U8IteratorTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGood);
    // TODO: TESTCASE_AUTO(testNegative);
    // TODO: TESTCASE_AUTO(testFFFD);
    TESTCASE_AUTO(testSinglePassIter);
    TESTCASE_AUTO(testFwdIter);
    TESTCASE_AUTO_END;
}

void U8IteratorTest::testGood() {
    std::string_view good(reinterpret_cast<const char*>(u8"abçカ🚴"));
    UTFStringCodePoints<char, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<char *, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::bidirectional_iterator_tag>);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    assertTrue("iter[1] * stringView()",
               units.stringView() == std::string_view(reinterpret_cast<const char*>(u8"b")));
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 4, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter[4] * stringView()",
               units.stringView() == std::string_view(reinterpret_cast<const char*>(u8"🚴")));
    assertTrue("iter == endIter", iter == range.end());
}

void U8IteratorTest::testSinglePassIter() {
    SinglePassSource<char> good(reinterpret_cast<const char*>(u8"abçカ🚴"));
    SinglePassIter<char> goodBegin(good);
    SinglePassIter<char> goodLimit{};
    UTFIterator<SinglePassIter<char>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(
        goodBegin, goodLimit);
    UTFIterator<SinglePassIter<char>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(goodLimit);
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<SinglePassIter<char>, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::input_iterator_tag>);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    // No data() for a single-pass unit iterator.
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 4, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter == endIter", iter == rangeLimit);
}

void U8IteratorTest::testFwdIter() {
    std::string_view good(reinterpret_cast<const char*>(u8"abçカ🚴"));
    FwdIter<char> goodBegin(good.data());
    FwdIter<char> goodLimit(good.data() + good.length());
    UTFIterator<FwdIter<char>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(goodBegin, goodLimit);
    UTFIterator<FwdIter<char>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(goodLimit);
    // TODO: UTFStringCodePoints<FwdIter, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<FwdIter<char>, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::forward_iterator_tag>);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    assertTrue("iter[1] * data()[0]", *units.data() == u8'b');
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 4, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    FwdIter<char> data = units.data();
    assertTrue("iter[4] * data()[0]", *data++ == u8"🚴"[0]);
    assertTrue("iter[4] * data()[1]", *data++ == u8"🚴"[1]);
    assertTrue("iter[4] * data()[2]", *data++ == u8"🚴"[2]);
    assertTrue("iter[4] * data()[3]", *data == u8"🚴"[3]);
    assertTrue("iter == endIter", iter == rangeLimit);
}

class U32IteratorTest : public IntlTest {
public:
    U32IteratorTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=nullptr) override;

    void testGood();
    void testNegative();
    void testFFFD();
    void testSinglePassIter();
    void testFwdIter();
};

extern IntlTest *createU32IteratorTest() {
    return new U32IteratorTest();
}

void U32IteratorTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if(exec) {
        logln("TestSuite U32IteratorTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGood);
    // TODO: TESTCASE_AUTO(testNegative);
    // TODO: TESTCASE_AUTO(testFFFD);
    TESTCASE_AUTO(testSinglePassIter);
    TESTCASE_AUTO(testFwdIter);
    TESTCASE_AUTO_END;
}

void U32IteratorTest::testGood() {
    std::u32string_view good(U"abçカ🚴"sv);
    UTFStringCodePoints<char32_t, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<char32_t *, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::bidirectional_iterator_tag>);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    assertTrue("iter[1] * stringView()", units.stringView() == U"b"sv);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 1, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter[4] * stringView()", units.stringView() == U"🚴"sv);
    assertTrue("iter == endIter", iter == range.end());
}

void U32IteratorTest::testSinglePassIter() {
    SinglePassSource<char32_t> good(U"abçカ🚴"sv);
    SinglePassIter<char32_t> goodBegin(good);
    SinglePassIter<char32_t> goodLimit{};
    UTFIterator<SinglePassIter<char32_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(
        goodBegin, goodLimit);
    UTFIterator<SinglePassIter<char32_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(goodLimit);
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<SinglePassIter<char32_t>, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::input_iterator_tag>);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    // No data() for a single-pass unit iterator.
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 1, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter == endIter", iter == rangeLimit);
}

void U32IteratorTest::testFwdIter() {
    std::u32string_view good(U"abçカ🚴"sv);
    FwdIter<char32_t> goodBegin(good.data());
    FwdIter<char32_t> goodLimit(good.data() + good.length());
    UTFIterator<FwdIter<char32_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeBegin(goodBegin, goodLimit);
    UTFIterator<FwdIter<char32_t>, UChar32, U_BEHAVIOR_NEGATIVE> rangeLimit(goodLimit);
    // TODO: UTFStringCodePoints<FwdIter, UChar32, U_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<FwdIter<char32_t>, UChar32, U_BEHAVIOR_NEGATIVE>>::iterator_category,
            std::forward_iterator_tag>);
    auto iter = rangeBegin;
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    assertTrue("iter[1] * data()[0]", *units.data() == u'b');
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", 1, units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    FwdIter<char32_t> data = units.data();
    assertTrue("iter[4] * data()[0]", *data == U"🚴"[0]);
    assertTrue("iter == endIter", iter == rangeLimit);
}
