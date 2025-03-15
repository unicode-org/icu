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
using U_HEADER_ONLY_NAMESPACE::utfIterator;
using U_HEADER_ONLY_NAMESPACE::UTFStringCodePoints;
using U_HEADER_ONLY_NAMESPACE::utfStringCodePoints;

#if 0
// Sample code for API docs etc. Compile when changing samples or APIs.
using U_HEADER_ONLY_NAMESPACE::unsafeUTFIterator;
using U_HEADER_ONLY_NAMESPACE::unsafeUTFStringCodePoints;

int32_t rangeLoop16(std::u16string_view s) {
    // We are just adding up the code points for minimal-code demonstration purposes.
    int32_t sum = 0;
    for (auto units : utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(s)) {
        sum += units.codePoint();  // < 0 if ill-formed
    }
    return sum;
}

int32_t loopIterPlusPlus16(std::u16string_view s) {
    auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
    int32_t sum = 0;
    for (auto iter = range.begin(), limit = range.end(); iter != limit;) {
        sum += (*iter++).codePoint();  // U+FFFD if ill-formed
    }
    return sum;
}

int32_t backwardLoop16(std::u16string_view s) {
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_SURROGATE>(s);
    int32_t sum = 0;
    for (auto start = range.begin(), iter = range.end(); start != iter;) {
        sum += (*--iter).codePoint();  // surrogate code point if unpaired / ill-formed
    }
    return sum;
}

int32_t reverseLoop8(std::string_view s) {
    auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
        sum += iter->codePoint();  // U+FFFD if ill-formed
    }
    return sum;
}

int32_t countCodePoints16(std::u16string_view s) {
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_SURROGATE>(s);
    return std::distance(range.begin(), range.end());
}

int32_t unsafeRangeLoop16(std::u16string_view s) {
    int32_t sum = 0;
    for (auto units : unsafeUTFStringCodePoints<UChar32>(s)) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t unsafeReverseLoop8(std::string_view s) {
    auto range = unsafeUTFStringCodePoints<UChar32>(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
        sum += iter->codePoint();
    }
    return sum;
}

char32_t firstCodePointOrFFFD16(std::u16string_view s) {
    if (s.empty()) { return 0xfffd; }
    auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
    return range.begin()->codePoint();
}

std::string_view firstSequence8(std::string_view s) {
    if (s.empty()) { return {}; }
    auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
    auto units = *(range.begin());
    if (units.wellFormed()) {
        return units.stringView();
    } else {
        return {};
    }
}

#endif  // SAMPLE_CODE

template<typename Unit>
class SinglePassIter;

// Shared state for one or more copies of single-pass iterators.
// Similar to https://en.cppreference.com/w/cpp/iterator/istreambuf_iterator
// but the iterators only implement LegacyIterator (* and ++) without post-increment.
template<typename Unit>
class SinglePassSource {
public:
    SinglePassSource(std::basic_string_view<Unit> s) : p(s.data()), limit(s.data() + s.length()) {}

    SinglePassIter<Unit> begin() { return SinglePassIter<Unit>(*this); }
    SinglePassIter<Unit> end() { return SinglePassIter<Unit>(); }

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
    typedef std::ptrdiff_t difference_type;
    // This is a LegacyIterator but there is no specific category for that,
    // so we claim it to be a LegacyInputIterator. It *is* single-pass.
    typedef std::input_iterator_tag iterator_category;

    SinglePassIter(SinglePassSource<Unit> &src) : src(&src) {}
    // limit sentinel
    SinglePassIter() : src(nullptr) {}

    // movable
    SinglePassIter(SinglePassIter &&src) noexcept = default;
    SinglePassIter &operator=(SinglePassIter &&src) noexcept = default;

    // not copyable
    SinglePassIter(const SinglePassIter &other) = delete;
    SinglePassIter &operator=(const SinglePassIter &other) = delete;

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
    typedef std::ptrdiff_t difference_type;
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
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(good);
    // TODO: Try to un-hardcode the iterator types in these checks via declspec.
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, char16_t *>>::iterator_category,
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
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(bad);
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
    // TODO: test units.begin()
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
    auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(bad);
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
    auto range = utfStringCodePoints<uint32_t, UTF_BEHAVIOR_SURROGATE>(bad);
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
    auto iter = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(good.begin(), good.end());
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(good.end(), good.end());
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, SinglePassIter<char16_t>>>::iterator_category,
            std::input_iterator_tag>);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    // No begin() for a single-pass unit iterator.
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
    auto rangeBegin = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodBegin, goodLimit);
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodLimit);
    // TODO: UTFStringCodePoints<FwdIter, UChar32, UTF_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, FwdIter<char16_t>>>::iterator_category,
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
    assertTrue("iter[1] * begin()[0]", *units.begin() == u'b');
    assertTrue("iter[1] * end()[0]", *units.end() == u'ç');
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
    FwdIter<char16_t> data = units.begin();
    assertTrue("iter[4] * begin()[0]", *data++ == u"🚴"[0]);
    assertTrue("iter[4] * begin()[1]", *data == u"🚴"[1]);
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
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
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(good);
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, char *>>::iterator_category,
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
    auto iter = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(good.begin(), good.end());
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(good.end(), good.end());
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, SinglePassIter<char>>>::iterator_category,
            std::input_iterator_tag>);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    // No begin() for a single-pass unit iterator.
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
    auto rangeBegin = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodBegin, goodLimit);
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodLimit);
    // TODO: UTFStringCodePoints<FwdIter, UChar32, UTF_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, FwdIter<char>>>::iterator_category,
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
    assertTrue("iter[1] * begin()[0]", *units.begin() == u8'b');
    assertTrue("iter[1] * end()[0]",
               static_cast<uint8_t>(*units.end()) == static_cast<uint8_t>(u8"ç"[0]));
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
    FwdIter<char> data = units.begin();
    assertTrue("iter[4] * begin()[0]",
               static_cast<uint8_t>(*data++) == static_cast<uint8_t>(u8"🚴"[0]));
    assertTrue("iter[4] * begin()[1]",
               static_cast<uint8_t>(*data++) == static_cast<uint8_t>(u8"🚴"[1]));
    assertTrue("iter[4] * begin()[2]",
               static_cast<uint8_t>(*data++) == static_cast<uint8_t>(u8"🚴"[2]));
    assertTrue("iter[4] * begin()[3]",
               static_cast<uint8_t>(*data) == static_cast<uint8_t>(u8"🚴"[3]));
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
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
    auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(good);
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, char32_t *>>::iterator_category,
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
    auto iter = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(good.begin(), good.end());
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(good.end(), good.end());
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, SinglePassIter<char32_t>>>::iterator_category,
            std::input_iterator_tag>);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", 1, units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    // No units.stringView() when the unit iterator is not a pointer.
    // No begin() for a single-pass unit iterator.
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
    auto rangeBegin = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodBegin, goodLimit);
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodLimit);
    // TODO: UTFStringCodePoints<FwdIter, UChar32, UTF_BEHAVIOR_NEGATIVE> range(good);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<
                UTFIterator<UChar32, UTF_BEHAVIOR_NEGATIVE, FwdIter<char32_t>>>::iterator_category,
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
    assertTrue("iter[1] * begin()[0]", *units.begin() == u'b');
    assertTrue("iter[1] * end()[0]", *units.end() == u'ç');
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
    FwdIter<char32_t> data = units.begin();
    assertTrue("iter[4] * begin()[0]", *data == U"🚴"[0]);
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
    assertTrue("iter == endIter", iter == rangeLimit);
}
