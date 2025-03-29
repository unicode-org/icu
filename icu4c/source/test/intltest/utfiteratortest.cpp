// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfiteratortest.cpp
// created: 2024aug12 Markus W. Scherer

#include <string>
#include <string_view>
#include <vector>

// Test header-only ICU C++ APIs. Do not use other ICU C++ APIs.
// Non-default configuration:
#define U_SHOW_CPLUSPLUS_API 0
// Default configuration:
// #define U_SHOW_CPLUSPLUS_HEADER_API 1

#include "unicode/utypes.h"
#include "unicode/utfiterator.h"
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
#include <iostream>
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

template<typename InputStream>  // some istream or streambuf
std::u32string cpFromInput(InputStream &in) {
    // This is a single-pass input_iterator.
    std::istreambuf_iterator bufIter(in);
    std::istreambuf_iterator<typename InputStream::char_type> bufLimit;
    auto iter = utfIterator<char32_t, UTF_BEHAVIOR_FFFD>(bufIter);
    auto limit = utfIterator<char32_t, UTF_BEHAVIOR_FFFD>(bufLimit);
    std::u32string s32;
    for (; iter != limit; ++iter) {
        s32.push_back(iter->codePoint());
    }
    return s32;
}

std::u32string cpFromStdin() { return cpFromInput(std::cin); }
std::u32string cpFromWideStdin() { return cpFromInput(std::wcin); }

#endif  // SAMPLE_CODE

template<typename Unit>
class SinglePassIter;

// Shared state for one or more copies of single-pass iterators.
// Similar to https://en.cppreference.com/w/cpp/iterator/istreambuf_iterator
// but the iterators only implement LegacyIterator (* and ++) without post-increment.
template<typename Unit>
class SinglePassSource {
public:
    explicit SinglePassSource(std::basic_string_view<Unit> s) : p(s.data()), limit(s.data() + s.length()) {}

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

    explicit SinglePassIter(SinglePassSource<Unit> &src) : src(&src) {}
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

template<typename Unit>
class FwdIter {
public:
    typedef Unit value_type;
    typedef Unit &reference;
    typedef Unit *pointer;
    typedef std::ptrdiff_t difference_type;
    // https://en.cppreference.com/w/cpp/named_req/ForwardIterator#Multi-pass_guarantee
    typedef std::forward_iterator_tag iterator_category;

    explicit FwdIter(const Unit *data) : p(data) {}

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

template<typename StringView>
std::vector<StringView> split(StringView s) {
    using Unit = typename StringView::value_type;
    std::vector<StringView> result;
    while (!s.empty()) {
        auto pos = s.find(static_cast<Unit>(u'|'));
        if (pos == StringView::npos) { break; }
        result.push_back(s.substr(0, pos));
        s.remove_prefix(pos + 1);
    }
    result.push_back(s);
    return result;
}

template<typename Unit>
std::basic_string<Unit> join(std::vector<std::basic_string_view<Unit>> parts) {
    std::basic_string<Unit> result;
    for (auto part : parts) {
        result.append(part);
    }
    return result;
}

// Avoids having to cast each byte value to char or uint8_t or similar.
std::string string8FromBytes(const int bytes[], size_t length) {
    std::string result;
    for (size_t i = 0; i < length; ++i) {
        result.push_back(static_cast<char>(bytes[i]));
    }
    return result;
}

class UTFIteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) override {
        if (exec) { logln("TestSuite UTFIteratorTest: "); }
        TESTCASE_AUTO_BEGIN;

        TESTCASE_AUTO(testSafe16Good);
        TESTCASE_AUTO(testSafe16Negative);
        TESTCASE_AUTO(testSafe16FFFD);
        TESTCASE_AUTO(testSafe16Surrogate);
        TESTCASE_AUTO(testSafe16SinglePassIter);
        TESTCASE_AUTO(testSafe16FwdIter);

        TESTCASE_AUTO(testSafe8Good);
        TESTCASE_AUTO(testSafe8Negative);
        TESTCASE_AUTO(testSafe8FFFD);
        TESTCASE_AUTO(testSafe8SinglePassIter);
        TESTCASE_AUTO(testSafe8FwdIter);

        TESTCASE_AUTO(testSafe32Good);
        TESTCASE_AUTO(testSafe32Negative);
        TESTCASE_AUTO(testSafe32FFFD);
        TESTCASE_AUTO(testSafe32Surrogate);
        TESTCASE_AUTO(testSafe32SinglePassIter);
        TESTCASE_AUTO(testSafe32FwdIter);

        TESTCASE_AUTO_END;
    }

    template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    CP32 sub(StringView part) {
        switch (behavior) {
            case UTF_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case UTF_BEHAVIOR_FFFD: return 0xfffd;
            case UTF_BEHAVIOR_SURROGATE: {
                auto c = part[0];
                return U_IS_SURROGATE(c) ? c : 0xfffd;
            }
        }
    }

    template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testSafeGood(StringView piped);

    template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testSafeBad(StringView piped);

    template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testSafeSinglePassIter(StringView piped);

    static constexpr std::u16string_view good16{u"a|b|ç|カ|🚴"};
    static const char *good8Chars;
    static constexpr std::u32string_view good32{U"a|b|ç|カ|🚴"};

    static constexpr char16_t badChars16[] = {
        u'a', u'|', 0xd900, u'|', u'ç', u'|', 0xdc05, u'|', u"🚴"[0], u"🚴"[1]
    };
    static constexpr std::u16string_view bad16{badChars16, std::size(badChars16)};

    static constexpr int badChars8[] = {
        u8'a', u8'|', 0xe0, 0xa0, u8'|', u8"ç"[0], u8"ç"[1], u8'|',
        0xf4, 0x8f, 0xbf, u8'|', u8"🚴"[0], u8"🚴"[1], u8"🚴"[2], u8"🚴"[3]
    };

    static constexpr char32_t badChars32[] = {
        u'a', u'|', 0xd900, u'|', u'ç', u'|', 0x110000, u'|', U'🚴'
    };
    static constexpr std::u32string_view bad32{badChars32, std::size(badChars32)};

    void testSafe16Good() {
        testSafeGood<UChar32, UTF_BEHAVIOR_NEGATIVE>(good16);
    }
    void testSafe16Negative() {
        testSafeBad<UChar32, UTF_BEHAVIOR_NEGATIVE>(bad16);
    }
    void testSafe16FFFD() {
        testSafeBad<char32_t, UTF_BEHAVIOR_FFFD>(bad16);
    }
    void testSafe16Surrogate() {
        testSafeBad<uint32_t, UTF_BEHAVIOR_SURROGATE>(bad16);
    }
    void testSafe16SinglePassIter() {
        testSafeSinglePassIter<UChar32, UTF_BEHAVIOR_NEGATIVE>(good16);
    }
    void testSafe16FwdIter();

    void testSafe8Good() {
        testSafeGood<UChar32, UTF_BEHAVIOR_NEGATIVE>(std::string_view{good8Chars});
    }
    void testSafe8Negative() {
        testSafeBad<UChar32, UTF_BEHAVIOR_NEGATIVE>(
            std::string_view(string8FromBytes(badChars8, std::size(badChars8))));
    }
    void testSafe8FFFD() {
        testSafeBad<char32_t, UTF_BEHAVIOR_FFFD>(
            std::string_view(string8FromBytes(badChars8, std::size(badChars8))));
    }
    void testSafe8SinglePassIter() {
        testSafeSinglePassIter<UChar32, UTF_BEHAVIOR_NEGATIVE>(std::string_view{good8Chars});
    }
    void testSafe8FwdIter();

    void testSafe32Good() {
        testSafeGood<UChar32, UTF_BEHAVIOR_NEGATIVE>(good32);
    }
    void testSafe32Negative() {
        testSafeBad<UChar32, UTF_BEHAVIOR_NEGATIVE>(bad32);
    }
    void testSafe32FFFD() {
        testSafeBad<char32_t, UTF_BEHAVIOR_FFFD>(bad32);
    }
    void testSafe32Surrogate() {
        testSafeBad<uint32_t, UTF_BEHAVIOR_SURROGATE>(bad32);
    }
    void testSafe32SinglePassIter() {
        testSafeSinglePassIter<UChar32, UTF_BEHAVIOR_NEGATIVE>(good32);
    }
    void testSafe32FwdIter();
};

const char *UTFIteratorTest::good8Chars = reinterpret_cast<const char *>(u8"a|b|ç|カ|🚴");

extern IntlTest *createUTFIteratorTest() {
    return new UTFIteratorTest();
}

template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testSafeGood(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    auto last = parts[4];
    StringView good(joined);
    // "abçカ🚴"
    auto range = utfStringCodePoints<CP32, behavior>(good);
    auto iter = range.begin();
    assertTrue(
        "bidirectional_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<decltype(iter)>::iterator_category,
            std::bidirectional_iterator_tag>);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", parts[1].length(), units.length());
    assertTrue("iter[1] * wellFormed", units.wellFormed());
    assertTrue("iter[1] * stringView()", units.stringView() == parts[1]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", last.length(), units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter[4] * stringView()", units.stringView() == last);
    auto unitsIter = units.begin();
    for (auto c : last) {
        assertEquals("iter[back 4] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[4] * end() == endIter", units.end() == good.end());
    assertTrue("iter == endIter", iter == range.end());
    // backwards
    units = *--iter;  // pre-decrement
    assertEquals("iter[back 4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[back 4] * length", last.length(), units.length());
    assertTrue("iter[back 4] * wellFormed", units.wellFormed());
    assertTrue("iter[back 4] * stringView()", units.stringView() == last);
    unitsIter = units.begin();
    for (auto c : last) {
        assertEquals("iter[back 4] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[back 4] * end() == endIter", units.end() == good.end());
    --iter;
    assertEquals("iter[back 3] * codePoint", u'カ', (*iter--).codePoint());  // post-decrement
    assertEquals("iter[back 2] * codePoint", u'ç', (*iter).codePoint());
    assertEquals("iter[back 2] -> length", parts[2].length(), iter->length());
    units = *--iter;
    assertEquals("iter[back 1] * codePoint", u'b', units.codePoint());
    assertTrue("iter[back 1] * wellFormed", units.wellFormed());
    assertTrue("iter[back 1] * stringView()", units.stringView() == parts[1]);
    --iter;
    assertEquals("iter[back 0] -> codePoint", u'a', iter->codePoint());
    assertTrue("iter[back 0] -> begin() == beginIter", iter->begin() == good.begin());
    assertTrue("iter == beginIter", iter == range.begin());
}

template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testSafeBad(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    StringView bad(joined);
    // "a?ç?🚴" where the ? sequences are ill-formed
    auto range = utfStringCodePoints<CP32, behavior>(bad);
    auto iter = range.begin();
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", sub<CP32, behavior>(parts[1]), units.codePoint());
    assertEquals("iter[1] * length", parts[1].length(), units.length());
    assertFalse("iter[1] * wellFormed", units.wellFormed());
    auto sv = units.stringView();
    assertEquals("iter[1] * stringView().length()",
                 static_cast<int32_t>(parts[1].length()), static_cast<int32_t>(sv.length()));
    int32_t i = 0;
    for (auto c : parts[1]) {
        assertEquals("iter[1] * stringView()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(sv[i++]));
    }
    auto unitsIter = units.begin();
    for (auto c : parts[1]) {
        assertEquals("iter[1] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[1] * end()[0]", *units.end() == parts[2][0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    units = *iter++;  // post-increment
    assertEquals("iter[3] * codePoint", sub<CP32, behavior>(parts[3]), units.codePoint());
    assertFalse("iter[3] * wellFormed", units.wellFormed());
    assertTrue("iter[4] * stringView()", (*iter++).stringView() == parts[4]);  // post-increment
    assertTrue("iter == endIter", iter == range.end());
    // backwards
    assertEquals("iter[back 4] * codePoint", U'🚴', (*--iter).codePoint());
    assertTrue("iter[back 4] -> wellFormed", iter->wellFormed());
    assertEquals("iter[back 3] * codePoint", sub<CP32, behavior>(parts[3]), (*--iter).codePoint());
    assertFalse("iter[back 3] -> wellFormed", iter->wellFormed());
    assertEquals("iter[back 2] * codePoint", U'ç', (*--iter).codePoint());
    assertEquals("iter[back 1] * codePoint", sub<CP32, behavior>(parts[1]), (*--iter).codePoint());
    assertEquals("iter[back 0] * codePoint", U'a', (*--iter).codePoint());
    assertTrue("iter[back 0] -> begin() == beginIter", iter->begin() == bad.begin());
    assertTrue("iter == beginIter", iter == range.begin());
}

template<typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testSafeSinglePassIter(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    SinglePassSource<Unit> good(joined);
    // "abçカ🚴"
    auto iter = utfIterator<CP32, behavior>(good.begin(), good.end());
    auto rangeLimit = utfIterator<CP32, behavior>(good.end(), good.end());
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<decltype(iter)>::iterator_category,
            std::input_iterator_tag>);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    assertEquals("iter[1] * codePoint", u'b', units.codePoint());
    assertEquals("iter[1] * length", parts[1].length(), units.length());
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
    assertEquals("iter[4] * length", parts[4].length(), units.length());
    assertTrue("iter[4] * wellFormed", units.wellFormed());
    assertTrue("iter == endIter", iter == rangeLimit);
}

void UTFIteratorTest::testSafe16FwdIter() {
    std::u16string_view good(u"abçカ🚴"sv);
    FwdIter<char16_t> goodBegin(good.data());
    FwdIter<char16_t> goodLimit(good.data() + good.length());
    auto iter = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodBegin, goodLimit);
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodLimit);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<decltype(iter)>::iterator_category,
            std::forward_iterator_tag>);
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
    FwdIter<char16_t> unitsIter = units.begin();
    assertTrue("iter[4] * begin()[0]", *unitsIter++ == u"🚴"[0]);
    assertTrue("iter[4] * begin()[1]", *unitsIter == u"🚴"[1]);
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
    assertTrue("iter == endIter", iter == rangeLimit);
}

void UTFIteratorTest::testSafe8FwdIter() {
    std::string_view good(reinterpret_cast<const char*>(u8"abçカ🚴"));
    FwdIter<char> goodBegin(good.data());
    FwdIter<char> goodLimit(good.data() + good.length());
    auto iter = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodBegin, goodLimit);
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodLimit);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<decltype(iter)>::iterator_category,
            std::forward_iterator_tag>);
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
    FwdIter<char> unitsIter = units.begin();
    assertTrue("iter[4] * begin()[0]",
               static_cast<uint8_t>(*unitsIter++) == static_cast<uint8_t>(u8"🚴"[0]));
    assertTrue("iter[4] * begin()[1]",
               static_cast<uint8_t>(*unitsIter++) == static_cast<uint8_t>(u8"🚴"[1]));
    assertTrue("iter[4] * begin()[2]",
               static_cast<uint8_t>(*unitsIter++) == static_cast<uint8_t>(u8"🚴"[2]));
    assertTrue("iter[4] * begin()[3]",
               static_cast<uint8_t>(*unitsIter) == static_cast<uint8_t>(u8"🚴"[3]));
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
    assertTrue("iter == endIter", iter == rangeLimit);
}

void UTFIteratorTest::testSafe32FwdIter() {
    std::u32string_view good(U"abçカ🚴"sv);
    FwdIter<char32_t> goodBegin(good.data());
    FwdIter<char32_t> goodLimit(good.data() + good.length());
    auto iter = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodBegin, goodLimit);
    auto rangeLimit = utfIterator<UChar32, UTF_BEHAVIOR_NEGATIVE>(goodLimit);
    assertTrue(
        "forward_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<decltype(iter)>::iterator_category,
            std::forward_iterator_tag>);
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
    FwdIter<char32_t> unitsIter = units.begin();
    assertTrue("iter[4] * begin()[0]", *unitsIter == U'🚴');
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
    assertTrue("iter == endIter", iter == rangeLimit);
}
