// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfiteratortest.cpp
// created: 2024aug12 Markus W. Scherer

#include <algorithm>
#include <array>
#include <iterator>
#include <forward_list>
#include <list>
#if defined(__cpp_lib_ranges)
#include <ranges>
#endif
#include <streambuf>
#include <sstream>
#include <string>
#include <string_view>
#include <vector>

// Test header-only ICU C++ APIs. Do not use other ICU C++ APIs.
// Non-default configuration:
#define U_SHOW_CPLUSPLUS_API 0
// Default configuration:
// #define U_SHOW_CPLUSPLUS_HEADER_API 1

#include "unicode/utypes.h"
#include "unicode/utf8.h"
#include "unicode/utf16.h"
#include "unicode/utfiterator.h"
#include "intltest.h"

// Makes u"literal"sv std::u16string_view literals possible.
// https://en.cppreference.com/w/cpp/string/basic_string_view/operator%22%22sv
using namespace std::string_view_literals;

using icu::header::UTFIterator;
using icu::header::utfIterator;
using icu::header::UTFStringCodePoints;
using icu::header::utfStringCodePoints;
using icu::header::UnsafeUTFIterator;
using icu::header::unsafeUTFIterator;
using icu::header::UnsafeUTFStringCodePoints;
using icu::header::unsafeUTFStringCodePoints;

namespace {

// Sentinel for NUL-terminated strings.
class Nul {};

template<typename Unit>
class SinglePassIter;

// Shared state for one or more copies of single-pass iterators.
// Similar to https://en.cppreference.com/w/cpp/iterator/istreambuf_iterator
// but the iterators only implement LegacyIterator (* and ++) without post-increment.
template<typename Unit>
class SinglePassSource {
public:
    explicit SinglePassSource(std::basic_string_view<Unit> s) :
            p(s.data()), limit(s.data() + s.length()) {}
    SinglePassSource(const Unit *s, const Nul &) :
            p(s), limit(nullptr) {}

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

    // Asymmetric equality & nonequality with a sentinel type.
    friend bool operator==(const SinglePassIter &iter, const Nul &) {
        return iter.atNul();
    }
#if U_CPLUSPLUS_VERSION < 20
    // C++17: Need to define all four combinations of == / != vs. parameter order.
    // Once we require C++20, we could remove all but the first == because
    // the compiler would generate the rest.
    friend bool operator==(const Nul &, const SinglePassIter &iter) {
        return iter.atNul();
    }
    friend bool operator!=(const SinglePassIter &iter, const Nul &nul) { return !(iter == nul); }
    friend bool operator!=(const Nul &nul, const SinglePassIter &iter) { return !(iter == nul); }
#endif  // C++17

    Unit operator*() const { return *(src->p); }
    SinglePassIter &operator++() {  // pre-increment
        ++(src->p);
        return *this;
    }
    // *no* post-increment

private:
    bool isDone() const { return src == nullptr || src->p == src->limit; }
    bool atNul() const { return *src->p == 0; }

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
    FwdIter() = default;

    bool operator==(const FwdIter &other) const { return p == other.p; }
    bool operator!=(const FwdIter &other) const { return !operator==(other); }

    // Asymmetric equality & nonequality with a sentinel type.
    friend bool operator==(const FwdIter &iter, const Nul &) {
        return *iter.p == 0;
    }
#if U_CPLUSPLUS_VERSION < 20
    // C++17: Need to define all four combinations of == / != vs. parameter order.
    // Once we require C++20, we could remove all but the first == because
    // the compiler would generate the rest.
    friend bool operator==(const Nul &, const FwdIter &iter) {
        return *iter.p == 0;
    }
    friend bool operator!=(const FwdIter &iter, const Nul &nul) { return !(iter == nul); }
    friend bool operator!=(const Nul &nul, const FwdIter &iter) { return !(iter == nul); }
#endif  // C++17

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

template<typename T>
T reverseCopy(T x) {
    T result{x};
    std::reverse(result.begin(), result.end());
    return result;
}

// Use SAFE when we don't care about ILL_FORMED vs. WELL_FORMED.
enum TestMode { SAFE, ILL_FORMED, WELL_FORMED, UNSAFE };
enum IterType { INPUT, FWD, CONTIG };

// Use this don't-care behavior value for unsafe iterators that do not use the behavior tparam.
constexpr auto ANY_B = UTF_BEHAVIOR_FFFD;

template<typename Unit>
struct ImplTest {
    std::basic_string<Unit> str;
    std::vector<std::basic_string<Unit>> parts;
    std::u32string codePoints;

    ImplTest reverseParts() const {
        ImplTest result;
        // We cannot just reverse the string.
        // We need to keep the code units for each subsequence in order.
        // We could join the reversed parts, but so far we don't need the reversed string.
        result.parts = reverseCopy(parts);
        // result.str = join<Unit>(result.parts);
        result.codePoints = reverseCopy(codePoints);
        return result;
    }
};

}  // namespace

class UTFIteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) override {
        if (exec) { logln("TestSuite UTFIteratorTest: "); }
        TESTCASE_AUTO_BEGIN;

        TESTCASE_AUTO(testSafe16Good);
        TESTCASE_AUTO(testSafe16Negative);
        TESTCASE_AUTO(testSafe16FFFD);
        TESTCASE_AUTO(testSafe16Surrogate);
        TESTCASE_AUTO(testUnsafe16);

        TESTCASE_AUTO(testSafe16SinglePassIterGood);
        TESTCASE_AUTO(testSafe16SinglePassIterNegative);
        TESTCASE_AUTO(testUnsafe16SinglePassIter);
        TESTCASE_AUTO(testSafe16SinglePassIterNulGood);

        TESTCASE_AUTO(testSafe16FwdIter);
        TESTCASE_AUTO(testUnsafe16FwdIter);

        TESTCASE_AUTO(testSafe8Good);
        TESTCASE_AUTO(testSafe8Negative);
        TESTCASE_AUTO(testSafe8FFFD);
        TESTCASE_AUTO(testUnsafe8);

        TESTCASE_AUTO(testSafe8SinglePassIterGood);
        TESTCASE_AUTO(testSafe8SinglePassIterFFFD);
        TESTCASE_AUTO(testUnsafe8SinglePassIter);
        TESTCASE_AUTO(testUnsafe8SinglePassIterNul);

        TESTCASE_AUTO(testSafe8FwdIter);
        TESTCASE_AUTO(testUnsafe8FwdIter);
        TESTCASE_AUTO(testSafe8FwdIterNul);

        TESTCASE_AUTO(testSafe32Good);
        TESTCASE_AUTO(testSafe32Negative);
        TESTCASE_AUTO(testSafe32FFFD);
        TESTCASE_AUTO(testSafe32Surrogate);
        TESTCASE_AUTO(testUnsafe32);

        TESTCASE_AUTO(testSafe32SinglePassIterGood);
        TESTCASE_AUTO(testSafe32SinglePassIterSurrogate);
        TESTCASE_AUTO(testUnsafe32SinglePassIter);

        TESTCASE_AUTO(testSafe32FwdIter);
        TESTCASE_AUTO(testUnsafe32FwdIter);
        TESTCASE_AUTO(testUnsafe32FwdIterNul);

        TESTCASE_AUTO(testSafe16LongLinearContig);
        TESTCASE_AUTO(testSafe8LongLinearContig);
        TESTCASE_AUTO(testSafe32LongLinearContig);

        TESTCASE_AUTO(testUnsafe16LongLinearContig);
        TESTCASE_AUTO(testUnsafe8LongLinearContig);
        TESTCASE_AUTO(testUnsafe32LongLinearContig);

        TESTCASE_AUTO(testSafe16LongLinearInput);
        TESTCASE_AUTO(testSafe8LongLinearInput);
        TESTCASE_AUTO(testSafe32LongLinearInput);

        TESTCASE_AUTO(testUnsafe16LongLinearInput);
        TESTCASE_AUTO(testUnsafe8LongLinearInput);
        TESTCASE_AUTO(testUnsafe32LongLinearInput);

        TESTCASE_AUTO(testSafe16LongLinearFwd);
        TESTCASE_AUTO(testSafe8LongLinearFwd);
        TESTCASE_AUTO(testSafe32LongLinearFwd);

        TESTCASE_AUTO(testUnsafe16LongLinearFwd);
        TESTCASE_AUTO(testUnsafe8LongLinearFwd);
        TESTCASE_AUTO(testUnsafe32LongLinearFwd);

        TESTCASE_AUTO(testSafe16LongBackward);
        TESTCASE_AUTO(testSafe8LongBackward);
        TESTCASE_AUTO(testSafe32LongBackward);

        TESTCASE_AUTO(testUnsafe16LongBackward);
        TESTCASE_AUTO(testUnsafe8LongBackward);
        TESTCASE_AUTO(testUnsafe32LongBackward);

        TESTCASE_AUTO(testSafe16LongReverse);
        TESTCASE_AUTO(testSafe8LongReverse);
        TESTCASE_AUTO(testSafe32LongReverse);

        TESTCASE_AUTO(testUnsafe16LongReverse);
        TESTCASE_AUTO(testUnsafe8LongReverse);
        TESTCASE_AUTO(testUnsafe32LongReverse);

        TESTCASE_AUTO(testSafe16Zigzag);
        TESTCASE_AUTO(testSafe8Zigzag);
        TESTCASE_AUTO(testSafe32Zigzag);

        TESTCASE_AUTO(testUnsafe16Zigzag);
        TESTCASE_AUTO(testUnsafe8Zigzag);
        TESTCASE_AUTO(testUnsafe32Zigzag);

        TESTCASE_AUTO(testSafe16ZigzagReverse);
        TESTCASE_AUTO(testSafe8ZigzagReverse);
        TESTCASE_AUTO(testSafe32ZigzagReverse);

        TESTCASE_AUTO(testUnsafe16ZigzagReverse);
        TESTCASE_AUTO(testUnsafe8ZigzagReverse);
        TESTCASE_AUTO(testUnsafe32ZigzagReverse);

        TESTCASE_AUTO(testOwnership);

        // C++20 ranges with all 2021 defect reports.  There is no separate
        // feature test macro value for https://wg21.link/P2210R2, but 2021'10
        // gets us https://wg21.link/P2415R2 as well as
        // https://wg21.link/P2325R3, and in practice implementations that have
        // both also have P2210R2, see
        // https://en.cppreference.com/w/cpp/compiler_support.html.
        // 2021'06, which guarantees P2325R3, is not good enough: GCC 11.3 and
        // MSVC 19.30 have P2325R3 but not P2210R2 (we need GCC 12 and MSVC
        // 19.31).
#if defined(__cpp_lib_ranges) && __cpp_lib_ranges >= 2021'10
        TESTCASE_AUTO(testUncommonInputRange);
        TESTCASE_AUTO(testUncommonForwardRange);
        TESTCASE_AUTO(testUncommonBidirectionalRange);
        TESTCASE_AUTO(testCommonForwardRange);
        TESTCASE_AUTO(testCommonBidirectionalRange);
        TESTCASE_AUTO(testCommonContiguousRange);
#endif

        TESTCASE_AUTO(testAllCodePoints);
        TESTCASE_AUTO(testAllScalarValues);

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

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testBidiIter(StringView piped);

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior,
             typename StringView, typename CodePoints>
    void testBidiIter(StringView sv, const std::vector<StringView> &parts, CodePoints range);

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testSinglePassIter(StringView piped);

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testSinglePassIterNul(StringView piped);

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior,
             typename StringView, typename Iter, typename Sentinel>
    void testSinglePassIter(const std::vector<StringView> &parts,
                            Iter &iter, const Sentinel &rangeLimit);

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testFwdIter(StringView piped);

    template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
    void testFwdIterNul(StringView piped);

    template<TestMode mode, typename StringView,
             typename LimitIter, typename Iter, typename Sentinel>
    void testFwdIter(const std::vector<StringView> &parts, LimitIter goodLimit,
                     Iter iter, Sentinel rangeLimit);

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
        testBidiIter<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(good16);
    }
    void testSafe16Negative() {
        testBidiIter<ILL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(bad16);
    }
    void testSafe16FFFD() {
        testBidiIter<ILL_FORMED, char32_t, UTF_BEHAVIOR_FFFD>(bad16);
    }
    void testSafe16Surrogate() {
        testBidiIter<ILL_FORMED, uint32_t, UTF_BEHAVIOR_SURROGATE>(bad16);
    }
    void testUnsafe16() {
        testBidiIter<UNSAFE, UChar32, ANY_B>(good16);
    }

    void testSafe16SinglePassIterGood() {
        testSinglePassIter<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(good16);
    }
    void testSafe16SinglePassIterNegative() {
        testSinglePassIter<ILL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(bad16);
    }
    void testUnsafe16SinglePassIter() {
        testSinglePassIter<UNSAFE, UChar32, ANY_B>(good16);
    }
    void testSafe16SinglePassIterNulGood() {
        testSinglePassIterNul<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(good16);
    }

    void testSafe16FwdIter() {
        testFwdIter<SAFE, UChar32, UTF_BEHAVIOR_NEGATIVE>(good16);
    }
    void testUnsafe16FwdIter() {
        testFwdIter<UNSAFE, UChar32, ANY_B>(good16);
    }

    void testSafe8Good() {
        testBidiIter<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(std::string_view{good8Chars});
    }
    void testSafe8Negative() {
        testBidiIter<ILL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(
                std::string_view(string8FromBytes(badChars8, std::size(badChars8))));
    }
    void testSafe8FFFD() {
        testBidiIter<ILL_FORMED, char32_t, UTF_BEHAVIOR_FFFD>(
                std::string_view(string8FromBytes(badChars8, std::size(badChars8))));
    }
    void testUnsafe8() {
        testBidiIter<UNSAFE, UChar32, ANY_B>(std::string_view{good8Chars});
    }

    void testSafe8SinglePassIterGood() {
        testSinglePassIter<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(
            std::string_view{good8Chars});
    }
    void testSafe8SinglePassIterFFFD() {
        testSinglePassIter<ILL_FORMED, char32_t, UTF_BEHAVIOR_FFFD>(
            std::string_view(string8FromBytes(badChars8, std::size(badChars8))));
    }
    void testUnsafe8SinglePassIter() {
        testSinglePassIter<UNSAFE, UChar32, ANY_B>(std::string_view{good8Chars});
    }
    void testUnsafe8SinglePassIterNul() {
        testSinglePassIterNul<UNSAFE, UChar32, ANY_B>(std::string_view{good8Chars});
    }

    void testSafe8FwdIter() {
        testFwdIter<SAFE, UChar32, UTF_BEHAVIOR_NEGATIVE>(std::string_view{good8Chars});
    }
    void testUnsafe8FwdIter() {
        testFwdIter<UNSAFE, UChar32, ANY_B>(std::string_view{good8Chars});
    }
    void testSafe8FwdIterNul() {
        testFwdIterNul<SAFE, UChar32, UTF_BEHAVIOR_NEGATIVE>(std::string_view{good8Chars});
    }

    void testSafe32Good() {
        testBidiIter<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(good32);
    }
    void testSafe32Negative() {
        testBidiIter<ILL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(bad32);
    }
    void testSafe32FFFD() {
        testBidiIter<ILL_FORMED, char32_t, UTF_BEHAVIOR_FFFD>(bad32);
    }
    void testSafe32Surrogate() {
        testBidiIter<ILL_FORMED, uint32_t, UTF_BEHAVIOR_SURROGATE>(bad32);
    }
    void testUnsafe32() {
        testBidiIter<UNSAFE, UChar32, ANY_B>(good32);
    }

    void testSafe32SinglePassIterGood() {
        testSinglePassIter<WELL_FORMED, UChar32, UTF_BEHAVIOR_NEGATIVE>(good32);
    }
    void testSafe32SinglePassIterSurrogate() {
        testSinglePassIter<ILL_FORMED, uint32_t, UTF_BEHAVIOR_SURROGATE>(bad32);
    }
    void testUnsafe32SinglePassIter() {
        testSinglePassIter<UNSAFE, UChar32, ANY_B>(good32);
    }

    void testSafe32FwdIter() {
        testFwdIter<SAFE, UChar32, UTF_BEHAVIOR_NEGATIVE>(good32);
    }
    void testUnsafe32FwdIter() {
        testFwdIter<UNSAFE, UChar32, ANY_B>(good32);
    }
    void testUnsafe32FwdIterNul() {
        testFwdIterNul<UNSAFE, UChar32, ANY_B>(good32);
    }

#if defined(__cpp_lib_ranges) && __cpp_lib_ranges >= 2021'10 // See above for the value.
    void testUncommonInputRange() {
        auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
        constexpr char source[] = "D808 DC2D D808 DEBA D808 DE40 200B D808 DF60 D808 DEA9";
        // Reads a sequence of space-separated hex code units from a stream.
        auto streamCodeUnits = [](std::stringstream&& s) {
            return std::views::istream<uint16_t>(s >> std::hex);
        };
        using CodeUnitRange = decltype(streamCodeUnits(std::stringstream(source)));
        // This range has a sentinel.
        static_assert(!std::ranges::common_range<CodeUnitRange>);
        static_assert(std::ranges::input_range<CodeUnitRange>);
        static_assert(!std::ranges::forward_range<CodeUnitRange>);
        {
            using CodePoints =
                decltype(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(streamCodeUnits({})));
            static_assert(!std::ranges::common_range<CodePoints>);
            static_assert(std::ranges::input_range<CodePoints>);
            static_assert(!std::ranges::forward_range<CodePoints>);
            assertTrue("uncommon input streamed range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(
                                              streamCodeUnits(std::stringstream(source))) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒀭𒊺𒉀\u200B𒍠𒊩")));
            assertFalse("uncommon input streamed range: find",
                        std::ranges::find_if(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(
                                                 streamCodeUnits(std::stringstream(source)))
                                                 .begin(),
                                             std::default_sentinel, [](auto u) {
                                                 return u.codePoint() == U'𒊩';
                                             }) == std::default_sentinel);
        }
        {
            using UnsafeCodePoints = decltype(unsafeUTFStringCodePoints<char32_t>(streamCodeUnits({})));
            static_assert(!std::ranges::common_range<UnsafeCodePoints>);
            static_assert(std::ranges::input_range<UnsafeCodePoints>);
            static_assert(!std::ranges::forward_range<UnsafeCodePoints>);
            assertTrue("unsafe uncommon input streamed range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(
                                              streamCodeUnits(std::stringstream(source))) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒀭𒊺𒉀\u200B𒍠𒊩")));
            assertFalse("unsafe uncommon input streamed range: find",
                        std::ranges::find_if(unsafeUTFStringCodePoints<char32_t>(
                                                 streamCodeUnits(std::stringstream(source)))
                                                 .begin(),
                                             std::default_sentinel, [](auto u) {
                                                 return u.codePoint() == U'𒊩';
                                             }) == std::default_sentinel);
        }
    }

    void testUncommonForwardRange() {
        auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
        const std::u16string text = u"𒌉 𒂍𒁾𒁀𒀀 𒌓 𒌌𒆷𒀀𒀭 𒈨𒂠 𒉌𒁺𒉈𒂗\n"
                                        u"𒂍𒁾𒁀𒀀𒂠 𒉌𒁺𒉈𒂗\n"
                                        u"𒂍𒁾𒁀𒀀 𒀀𒈾𒀀𒀭 𒉌𒀝\n"
                                        u"𒁾𒈬 𒉌𒋃 𒃻𒅗𒁺𒈬 𒉌𒅥\n";
        // Code units from the third line in `text`.
        auto codeUnits = *(text | std::ranges::views::lazy_split(u'\n') | std::views::drop(2)).begin();
        using CodeUnitRange = decltype(codeUnits);
        // This range has a sentinel.
        static_assert(!std::ranges::common_range<CodeUnitRange>);
        // Even though `source` is contiguous, the lazy split makes this forward-only.
        static_assert(std::ranges::forward_range<CodeUnitRange>);
        static_assert(!std::ranges::bidirectional_range<CodeUnitRange>);
        {
            using CodePoints = decltype(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits));
            static_assert(!std::ranges::common_range<CodePoints>);
            static_assert(std::ranges::forward_range<CodePoints>);
            static_assert(!std::ranges::bidirectional_range<CodePoints>);
            assertTrue("uncommon forward lazily split range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒂍𒁾𒁀𒀀 𒀀𒈾𒀀𒀭 𒉌𒀝")));
        }
        {
            using UnsafeCodePoints = decltype(unsafeUTFStringCodePoints<char32_t>(codeUnits));
            static_assert(!std::ranges::common_range<UnsafeCodePoints>);
            static_assert(std::ranges::forward_range<UnsafeCodePoints>);
            static_assert(!std::ranges::bidirectional_range<UnsafeCodePoints>);
            assertTrue("unsafe uncommon forward lazily split range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒂍𒁾𒁀𒀀 𒀀𒈾𒀀𒀭 𒉌𒀝")));
        }
    }

    void testUncommonBidirectionalRange() {
        auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
        const std::u8string text = u8"𒀭𒊺𒉀 𒍠𒊩  # ᵈnisaba za₃-mim";
        // Code units before the #.
        auto codeUnits = text | std::ranges::views::take_while([](char8_t c) { return c != u8'#'; });
        using CodeUnitRange = decltype(codeUnits);
        // This range has a sentinel.
        static_assert(!std::ranges::common_range<CodeUnitRange>);
        static_assert(std::ranges::contiguous_range<CodeUnitRange>);
        {
            using CodePoints = decltype(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits));
            static_assert(!std::ranges::common_range<CodePoints>);
            static_assert(std::ranges::bidirectional_range<CodePoints>);
            static_assert(!std::ranges::random_access_range<CodePoints>);
            assertTrue("uncommon bidirectional prefix range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒀭𒊺𒉀 𒍠𒊩  ")));
            assertTrue("reversed uncommon bidirectional prefix range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                                              std::ranges::views::reverse |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"  𒊩𒍠 𒉀𒊺𒀭")));
        }
        {
            using UnsafeCodePoints = decltype(unsafeUTFStringCodePoints<char32_t>(codeUnits));
            static_assert(!std::ranges::common_range<UnsafeCodePoints>);
            static_assert(std::ranges::bidirectional_range<UnsafeCodePoints>);
            static_assert(!std::ranges::random_access_range<UnsafeCodePoints>);
            assertTrue("unsafe uncommon bidirectional prefix range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒀭𒊺𒉀 𒍠𒊩  ")));
            assertTrue("reversed unsafe uncommon bidirectional prefix range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(codeUnits) |
                                              std::ranges::views::reverse |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"  𒊩𒍠 𒉀𒊺𒀭")));
        }
    }

    void testCommonForwardRange() {
        auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
        const std::forward_list<std::u16string_view> words{u"𒌉",
                                                           u"𒂍𒁾𒁀𒀀",
                                                           u"𒌓",
                                                           u"𒌌𒆷𒀀𒀭",
                                                           u"𒈨𒂠",
                                                           u"𒉌𒁺𒉈𒂗"};
        // Code units from the concatenated words.
        auto codeUnits = words | std::ranges::views::join;
        using CodeUnitRange = decltype(codeUnits);
        // This range does not have a sentinel.
        static_assert(std::ranges::common_range<CodeUnitRange>);
        // Forward-only because we started from a forward list.
        static_assert(std::ranges::forward_range<CodeUnitRange>);
        static_assert(!std::ranges::bidirectional_range<CodeUnitRange>);
        {
            using CodePoints = decltype(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits));
            static_assert(std::ranges::common_range<CodePoints>);
            static_assert(std::ranges::forward_range<CodePoints>);
            static_assert(!std::ranges::bidirectional_range<CodePoints>);
            assertTrue("common forward concatenated range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒌉𒂍𒁾𒁀𒀀𒌓𒌌𒆷𒀀𒀭𒈨𒂠𒉌𒁺𒉈𒂗")));
        }
        {
            using UnsafeCodePoints = decltype(unsafeUTFStringCodePoints<char32_t>(codeUnits));
            static_assert(std::ranges::common_range<UnsafeCodePoints>);
            static_assert(std::ranges::forward_range<UnsafeCodePoints>);
            static_assert(!std::ranges::bidirectional_range<UnsafeCodePoints>);
            assertTrue("unsafe common forward concatenated range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒌉𒂍𒁾𒁀𒀀𒌓𒌌𒆷𒀀𒀭𒈨𒂠𒉌𒁺𒉈𒂗")));
        }
    }

    void testCommonBidirectionalRange() {
        auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
        const std::u8string card{0xF0, 0x92, 0xFF, 0x89, 0xFF, 0xFF, 0xAD};
        // Read code units from `card`, skipping any bytes set to FF.
        auto codeUnits = card | std::ranges::views::filter([](char8_t c) { return c != 0xFF; });
        using CodeUnitRange = decltype(codeUnits);
        // This range does not have a sentinel.
        static_assert(std::ranges::common_range<CodeUnitRange>);
        // Bidirectional but not contiguous (there are holes where the bytes are FF).
        static_assert(std::ranges::bidirectional_range<CodeUnitRange>);
        static_assert(!std::ranges::contiguous_range<CodeUnitRange>);
        {
            using CodePoints = decltype(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits));
            static_assert(std::ranges::common_range<CodePoints>);
            static_assert(std::ranges::forward_range<CodePoints>);
            static_assert(std::ranges::bidirectional_range<CodePoints>);
            assertTrue("common bidirectional filtered range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒉭")));
            assertTrue("reversed common bidirectional filtered range",
                       std::ranges::equal(utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits) |
                                              std::ranges::views::reverse |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒉭")));
#if U_CPLUSPLUS_VERSION >= 23 && __cpp_lib_ranges >= 2022'02
            assertTrue("reversed common bidirectional filtered range: one big pipeline",
                       std::ranges::equal(
                           card
                           | std::ranges::views::filter([](char8_t c) { return c != 0xFF; })
                           | utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>
                           | std::ranges::views::reverse
                           | std::ranges::views::transform(codePoint),
                           std::u32string_view(U"𒉭")));
#endif
        }
        {
            using UnsafeCodePoints = decltype(unsafeUTFStringCodePoints<char32_t>(codeUnits));
            static_assert(std::ranges::common_range<UnsafeCodePoints>);
            static_assert(std::ranges::forward_range<UnsafeCodePoints>);
            static_assert(std::ranges::bidirectional_range<UnsafeCodePoints>);
            assertTrue("unsafe common bidirectional filtered range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(codeUnits) |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒉭")));
            assertTrue("reversed unsafe common bidirectional filtered range",
                       std::ranges::equal(unsafeUTFStringCodePoints<char32_t>(codeUnits) |
                                              std::ranges::views::reverse |
                                              std::ranges::views::transform(codePoint),
                                          std::u32string_view(U"𒉭")));
#if U_CPLUSPLUS_VERSION >= 23 && __cpp_lib_ranges >= 2022'02
            assertTrue("reversed unsafe common bidirectional filtered range: one big pipeline",
                       std::ranges::equal(
                           card
                           | std::ranges::views::filter([](char8_t c) { return c != 0xFF; })
                           | unsafeUTFStringCodePoints<char32_t>
                           | std::ranges::views::reverse
                           | std::ranges::views::transform(codePoint),
                           std::u32string_view(U"𒉭")));
#endif
        }
    }

    void testCommonContiguousRange() {
        auto codePoint = [](const auto &codeUnits) { return codeUnits.codePoint(); };
        const std::u16string codeUnits = u"𒀭𒊺𒉀​𒍠𒊩";
        static_assert(std::ranges::contiguous_range<decltype(codeUnits)>);
        auto codePoints = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(codeUnits);
        static_assert(std::is_same_v<decltype(codePoints),
                                     UTFStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD,
                                                         std::ranges::ref_view<const std::u16string>>>);
        static_assert(std::ranges::common_range<decltype(codePoints)>);
        static_assert(std::ranges::bidirectional_range<decltype(codePoints)>);
        static_assert(!std::ranges::random_access_range<decltype(codePoints)>);
        auto unsafeCodePoints = unsafeUTFStringCodePoints<char32_t>(codeUnits);
        static_assert(std::is_same_v<
                      decltype(unsafeCodePoints),
                      UnsafeUTFStringCodePoints<char32_t, std::ranges::ref_view<const std::u16string>>>);
        static_assert(std::ranges::common_range<decltype(unsafeCodePoints)>);
        static_assert(std::ranges::bidirectional_range<decltype(unsafeCodePoints)>);
        static_assert(!std::ranges::random_access_range<decltype(unsafeCodePoints)>);
        assertTrue("safe contiguous range by reference",
                   std::ranges::equal(codePoints | std::ranges::views::transform(codePoint),
                                      std::u32string_view(U"𒀭𒊺𒉀​𒍠𒊩")));
        assertTrue("unsafe contiguous range by reference",
                   std::ranges::equal(unsafeCodePoints | std::ranges::views::transform(codePoint),
                                      std::u32string_view(U"𒀭𒊺𒉀​𒍠𒊩")));
        auto ownedCodePoints =
            utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(std::u16string(u"𒀭𒊺𒉀​𒍠𒊩"));
        static_assert(std::is_same_v<
                      decltype(ownedCodePoints),
                           UTFStringCodePoints<
                               char32_t, UTF_BEHAVIOR_FFFD, std::ranges::owning_view<std::u16string>>>);
        auto unsafeOwnedCodePoints =
            unsafeUTFStringCodePoints<char32_t>(std::u16string(u"𒀭𒊺𒉀​𒍠𒊩"));
        static_assert(std::is_same_v<
                      decltype(unsafeOwnedCodePoints),
                      UnsafeUTFStringCodePoints<char32_t, std::ranges::owning_view<std::u16string>>>);
        assertTrue("safe owned contiguous range",
                   std::ranges::equal(ownedCodePoints | std::ranges::views::transform(codePoint),
                                      std::u32string_view(U"𒀭𒊺𒉀​𒍠𒊩")));
        assertTrue("unsafe owned contiguous range",
                   std::ranges::equal(unsafeOwnedCodePoints | std::ranges::views::transform(codePoint),
                                      std::u32string_view(U"𒀭𒊺𒉀​𒍠𒊩")));
    }
#endif

    // implementation code coverage ---------------------------------------- ***

    void initLong();

    template<TestMode mode, UTFIllFormedBehavior behavior,
             IterType type, typename Unit, typename Units>
    void checkUnits(const Units &units, std::basic_string_view<Unit> part, UChar32 expectedCP);

    template<TestMode mode, UTFIllFormedBehavior behavior, IterType type, typename Unit, typename Iter>
    void testLongLinear(const ImplTest<Unit> &test, Iter begin, Iter end) {
        for (size_t i = 0; begin != end; ++i, ++begin) {
            checkUnits<mode, behavior, type, Unit>(*begin, test.parts[i], test.codePoints[i]);
        }
    }

    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testLongLinearContig(const ImplTest<Unit> &test) {
        initLong();
        if constexpr (mode == UNSAFE) {
            auto range = unsafeUTFStringCodePoints<UChar32>(test.str);
            testLongLinear<mode, behavior, CONTIG, Unit>(test, range.begin(), range.end());
        } else {
            auto range = utfStringCodePoints<UChar32, behavior>(test.str);
            testLongLinear<mode, behavior, CONTIG, Unit>(test, range.begin(), range.end());
        }
    }

    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testLongLinearInput(const ImplTest<Unit> &test) {
        initLong();
        SinglePassSource<Unit> src(test.str);
        if constexpr (mode == UNSAFE) {
            testLongLinear<mode, behavior, INPUT, Unit>(
                test,
                unsafeUTFIterator<UChar32>(src.begin()),
                unsafeUTFIterator<UChar32>(src.end()));
        } else {
            testLongLinear<mode, behavior, INPUT, Unit>(
                test,
                utfIterator<UChar32, behavior>(src.begin(), src.end()),
                utfIterator<UChar32, behavior>(src.end(), src.end()));
        }
    }

    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testLongLinearFwd(const ImplTest<Unit> &test) {
        initLong();
        FwdIter<Unit> srcBegin(test.str.data());
        FwdIter<Unit> srcLimit(test.str.data() + test.str.length());
        if constexpr (mode == UNSAFE) {
            testLongLinear<mode, behavior, FWD, Unit>(
                test,
                unsafeUTFIterator<UChar32>(srcBegin),
                unsafeUTFIterator<UChar32>(srcLimit));
        } else {
            testLongLinear<mode, behavior, FWD, Unit>(
                test,
                utfIterator<UChar32, behavior>(srcBegin, srcLimit),
                utfIterator<UChar32, behavior>(srcLimit));
        }
    }

    // backward: from end to begin with *--iter
    template<TestMode mode, UTFIllFormedBehavior behavior,
             IterType type, typename Unit, typename Iter>
    void testLongBackward(const ImplTest<Unit> &test, Iter begin, Iter end) {
        for (size_t i = test.codePoints.length(); begin != end;) {
            --i;
            checkUnits<mode, behavior, type, Unit>(*--end, test.parts[i], test.codePoints[i]);
        }
    }

    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testLongBackward(const ImplTest<Unit> &test) {
        initLong();
        if constexpr (mode == UNSAFE) {
            auto range = unsafeUTFStringCodePoints<UChar32>(test.str);
            testLongBackward<mode, behavior, CONTIG, Unit>(test, range.begin(), range.end());
        } else {
            auto range = utfStringCodePoints<UChar32, behavior>(test.str);
            testLongBackward<mode, behavior, CONTIG, Unit>(test, range.begin(), range.end());
        }
    }

    // reverse: from rbegin() to rend(), uses the reverse_iterator
    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testLongReverse(const ImplTest<Unit> &test) {
        initLong();
        auto reverse = test.reverseParts();
        if constexpr (mode == UNSAFE) {
            auto range = unsafeUTFStringCodePoints<UChar32>(test.str);
            testLongLinear<mode, behavior, CONTIG, Unit>(reverse, range.rbegin(), range.rend());
        } else {
            auto range = utfStringCodePoints<UChar32, behavior>(test.str);
            testLongLinear<mode, behavior, CONTIG, Unit>(reverse, range.rbegin(), range.rend());
        }
    }

    // Test state keeping in a bidirectional_iterator:
    // Change directions, increment/decrement without reading, etc.
    template<TestMode mode, UTFIllFormedBehavior behavior,
             IterType type, typename Unit, typename Iter>
    void zigzag(const ImplTest<Unit> &test, size_t i,
                const Iter &begin, Iter iter, const Iter &end);

    template<TestMode mode, UTFIllFormedBehavior behavior, IterType type, typename Unit, typename Iter>
    void testZigzag(const ImplTest<Unit> &test, Iter begin, Iter end) {
        size_t i = 0;
        for (Iter iter = begin; iter != end; ++i, ++iter) {
            zigzag<mode, behavior, type, Unit>(test, i, begin, iter, end);
        }
    }

    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testZigzag(const ImplTest<Unit> &test) {
        initLong();
        if constexpr (mode == UNSAFE) {
            auto range = unsafeUTFStringCodePoints<UChar32>(test.str);
            testZigzag<mode, behavior, CONTIG, Unit>(test, range.begin(), range.end());
        } else {
            auto range = utfStringCodePoints<UChar32, behavior>(test.str);
            testZigzag<mode, behavior, CONTIG, Unit>(test, range.begin(), range.end());
        }
    }

    // Exercise the reverse_iterator as well.
    template<TestMode mode, UTFIllFormedBehavior behavior, typename Unit>
    void testZigzagReverse(const ImplTest<Unit> &test) {
        initLong();
        auto reverse = test.reverseParts();
        if constexpr (mode == UNSAFE) {
            auto range = unsafeUTFStringCodePoints<UChar32>(test.str);
            testZigzag<mode, behavior, CONTIG, Unit>(reverse, range.rbegin(), range.rend());
        } else {
            auto range = utfStringCodePoints<UChar32, behavior>(test.str);
            testZigzag<mode, behavior, CONTIG, Unit>(reverse, range.rbegin(), range.rend());
        }
    }

    void testSafe16LongLinearContig() {
        testLongLinearContig<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8LongLinearContig() {
        testLongLinearContig<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32LongLinearContig() {
        testLongLinearContig<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16LongLinearContig() {
        testLongLinearContig<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8LongLinearContig() {
        testLongLinearContig<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32LongLinearContig() {
        testLongLinearContig<UNSAFE, ANY_B, char32_t>(longGood32);
    }

    void testSafe16LongLinearInput() {
        testLongLinearInput<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8LongLinearInput() {
        testLongLinearInput<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32LongLinearInput() {
        testLongLinearInput<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16LongLinearInput() {
        testLongLinearInput<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8LongLinearInput() {
        testLongLinearInput<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32LongLinearInput() {
        testLongLinearInput<UNSAFE, ANY_B, char32_t>(longGood32);
    }

    void testSafe16LongLinearFwd() {
        testLongLinearFwd<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8LongLinearFwd() {
        testLongLinearFwd<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32LongLinearFwd() {
        testLongLinearFwd<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16LongLinearFwd() {
        testLongLinearFwd<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8LongLinearFwd() {
        testLongLinearFwd<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32LongLinearFwd() {
        testLongLinearFwd<UNSAFE, ANY_B, char32_t>(longGood32);
    }

    void testSafe16LongBackward() {
        testLongBackward<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8LongBackward() {
        testLongBackward<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32LongBackward() {
        testLongBackward<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16LongBackward() {
        testLongBackward<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8LongBackward() {
        testLongBackward<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32LongBackward() {
        testLongBackward<UNSAFE, ANY_B, char32_t>(longGood32);
    }

    void testSafe16LongReverse() {
        testLongReverse<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8LongReverse() {
        testLongReverse<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32LongReverse() {
        testLongReverse<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16LongReverse() {
        testLongReverse<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8LongReverse() {
        testLongReverse<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32LongReverse() {
        testLongReverse<UNSAFE, ANY_B, char32_t>(longGood32);
    }

    void testSafe16Zigzag() {
        testZigzag<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8Zigzag() {
        testZigzag<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32Zigzag() {
        testZigzag<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16Zigzag() {
        testZigzag<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8Zigzag() {
        testZigzag<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32Zigzag() {
        testZigzag<UNSAFE, ANY_B, char32_t>(longGood32);
    }

    void testSafe16ZigzagReverse() {
        testZigzagReverse<SAFE, UTF_BEHAVIOR_SURROGATE, char16_t>(longBad16);
    }
    void testSafe8ZigzagReverse() {
        testZigzagReverse<SAFE, UTF_BEHAVIOR_NEGATIVE, char>(longBad8);
    }
    void testSafe32ZigzagReverse() {
        testZigzagReverse<SAFE, UTF_BEHAVIOR_SURROGATE, char32_t>(longBad32);
    }

    void testUnsafe16ZigzagReverse() {
        testZigzagReverse<UNSAFE, ANY_B, char16_t>(longGood16);
    }
    void testUnsafe8ZigzagReverse() {
        testZigzagReverse<UNSAFE, ANY_B, char>(longGood8);
    }
    void testUnsafe32ZigzagReverse() {
        testZigzagReverse<UNSAFE, ANY_B, char32_t>(longGood32);
    }
    void testOwnership() {
        class NonCopyableString : public std::u16string {
          public:
            NonCopyableString(std::u16string s) : std::u16string(std::move(s)) {}
            NonCopyableString(NonCopyableString const&) = delete;
            NonCopyableString &operator=(NonCopyableString const&) = delete;
            NonCopyableString(NonCopyableString&&) = default;
            NonCopyableString &operator=(NonCopyableString&&) = default;
        };
        const NonCopyableString referenced(u"𒀭𒊺𒉀 𒍠𒊩");
        {
            auto referencingRange = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(referenced);
            auto owningRange =
                utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(NonCopyableString(u"𒀭𒊺𒉀 𒍠𒊩"));
            auto itr = referencingRange.begin();
            auto ito = owningRange.begin();
            for (; itr != referencingRange.end() && ito != owningRange.end(); ++itr, ++ito) {
                assertEquals("Referenced and owned iteration", itr->codePoint(), ito->codePoint());
            }
        }
        {
            auto unsafeReferencingRange = unsafeUTFStringCodePoints<char32_t>(referenced);
            auto unsafeOwningRange = unsafeUTFStringCodePoints<char32_t>(NonCopyableString(u"𒀭𒊺𒉀 𒍠𒊩"));
            auto itr = unsafeReferencingRange.begin();
            auto ito = unsafeOwningRange.begin();
            for (; itr != unsafeReferencingRange.end() && ito != unsafeOwningRange.end(); ++itr, ++ito) {
                assertEquals("Referenced and owned unsafe iteration", itr->codePoint(),
                             ito->codePoint());
            }
        }
    }

    void testAllCodePoints();
    void testAllScalarValues();

    ImplTest<char> longGood8;
    ImplTest<char16_t> longGood16;
    ImplTest<char32_t> longGood32;
    ImplTest<char> longBad8;
    ImplTest<char16_t> longBad16;
    ImplTest<char32_t> longBad32;
};

const char *UTFIteratorTest::good8Chars = reinterpret_cast<const char *>(u8"a|b|ç|カ|🚴");

extern IntlTest *createUTFIteratorTest() {
    return new UTFIteratorTest();
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testBidiIter(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    StringView sv(joined);
    // "abçカ🚴"
    // or
    // "a?ç?🚴" where the ? sequences are ill-formed
    if constexpr (mode == UNSAFE) {
        auto range = unsafeUTFStringCodePoints<CP32>(sv);
        testBidiIter<mode, CP32, behavior>(sv, parts, range);
    } else {
        auto range = utfStringCodePoints<CP32, behavior>(sv);
        testBidiIter<mode, CP32, behavior>(sv, parts, range);
    }
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior,
         typename StringView, typename CodePoints>
void UTFIteratorTest::testBidiIter(
        StringView sv, const std::vector<StringView> &parts, CodePoints range) {
    constexpr bool isWellFormed = mode != ILL_FORMED;
    auto last = parts[4];
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
    CP32 expectedCP = isWellFormed ? u'b' : sub<CP32, behavior>(parts[1]);
    assertEquals("iter[1] * codePoint", expectedCP, units.codePoint());
    assertEquals("iter[1] * length", parts[1].length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertEquals("iter[1] * wellFormed", isWellFormed, units.wellFormed());
    }
    assertTrue("iter[1] * stringView()", units.stringView() == parts[1]);
    auto unitsIter = units.begin();
    for (auto c : parts[1]) {
        assertEquals("iter[1] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[1] * end()[0]", *units.end() == parts[2][0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    units = *iter++;  // post-increment
    expectedCP = isWellFormed ? u'カ' : sub<CP32, behavior>(parts[3]);
    assertEquals("iter[3] * codePoint", expectedCP, units.codePoint());
    if constexpr (mode != UNSAFE) {
        assertEquals("iter[3] * wellFormed", isWellFormed, units.wellFormed());
    }
    // Fetch the current code point twice.
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;  // post-increment
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", last.length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertTrue("iter[4] * wellFormed", units.wellFormed());
    }
    assertTrue("iter[4] * stringView()", units.stringView() == last);
    unitsIter = units.begin();
    for (auto c : last) {
        assertEquals("iter[back 4] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[4] * end() == endIter", units.end() == sv.end());
    assertTrue("iter == endIter", iter == range.end());
    // backwards
    units = *--iter;  // pre-decrement
    assertEquals("iter[back 4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[back 4] * length", last.length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertTrue("iter[back 4] * wellFormed", units.wellFormed());
    }
    assertTrue("iter[back 4] * stringView()", units.stringView() == last);
    unitsIter = units.begin();
    for (auto c : last) {
        assertEquals("iter[back 4] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[back 4] * end() == endIter", units.end() == sv.end());
    --iter;
    if constexpr (mode != UNSAFE) {
        assertEquals("iter[back 3] -> wellFormed", isWellFormed, iter->wellFormed());
    }
    assertEquals("iter[back 3] * codePoint", expectedCP, (*iter--).codePoint());  // post-decrement
    assertEquals("iter[back 2] * codePoint", u'ç', (*iter).codePoint());
    assertEquals("iter[back 2] -> length", parts[2].length(), iter->length());
    if constexpr (mode != UNSAFE) {
        assertTrue("iter[back 2] -> wellFormed", iter->wellFormed());
    }
    units = *--iter;
    expectedCP = isWellFormed ? u'b' : sub<CP32, behavior>(parts[1]);
    assertEquals("iter[back 1] * codePoint", expectedCP, units.codePoint());
    if constexpr (mode != UNSAFE) {
        assertEquals("iter[back 1] * wellFormed", isWellFormed, units.wellFormed());
    }
    assertTrue("iter[back 1] * stringView()", units.stringView() == parts[1]);
    --iter;
    assertEquals("iter[back 0] -> codePoint", u'a', iter->codePoint());
    assertTrue("iter[back 0] -> begin() == beginIter", iter->begin() == sv.begin());
    assertTrue("iter == beginIter", iter == range.begin());
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testSinglePassIter(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    SinglePassSource<Unit> good(joined);
    // "abçカ🚴"
    // or
    // "a?ç?🚴" where the ? sequences are ill-formed
    if constexpr (mode == UNSAFE) {
        auto iter = unsafeUTFIterator<CP32>(good.begin());
        auto rangeLimit = unsafeUTFIterator<CP32>(good.end());
        testSinglePassIter<mode, CP32, behavior>(parts, iter, rangeLimit);
    } else {
        auto iter = utfIterator<CP32, behavior>(good.begin(), good.end());
        auto rangeLimit = utfIterator<CP32, behavior>(good.end(), good.end());
        testSinglePassIter<mode, CP32, behavior>(parts, iter, rangeLimit);
    }
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testSinglePassIterNul(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    Nul sentinel;
    SinglePassSource<Unit> good(joined.c_str(), sentinel);
    // "abçカ🚴"
    // or
    // "a?ç?🚴" where the ? sequences are ill-formed
    if constexpr (mode == UNSAFE) {
        auto iter = unsafeUTFIterator<CP32>(good.begin());
        testSinglePassIter<mode, CP32, behavior>(parts, iter, sentinel);
    } else {
        auto iter = utfIterator<CP32, behavior>(good.begin(), sentinel);
        testSinglePassIter<mode, CP32, behavior>(parts, iter, sentinel);
    }
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior,
         typename StringView, typename Iter, typename Sentinel>
void UTFIteratorTest::testSinglePassIter(
        const std::vector<StringView> &parts, Iter &iter, const Sentinel &rangeLimit) {
    constexpr bool isWellFormed = mode != ILL_FORMED;
    assertTrue(
        "input_iterator_tag",
        std::is_same_v<
            typename std::iterator_traits<std::remove_reference_t<decltype(iter)>>::
                iterator_category,
            std::input_iterator_tag>);
    assertEquals("iter[0] * codePoint", u'a', (*iter).codePoint());
    assertEquals("iter[0] -> codePoint", u'a', iter->codePoint());
    ++iter;  // pre-increment
    auto units = *iter;
    CP32 expectedCP = isWellFormed ? u'b' : sub<CP32, behavior>(parts[1]);
    assertEquals("iter[1] * codePoint", expectedCP, units.codePoint());
    assertEquals("iter[1] * length", parts[1].length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertEquals("iter[1] * wellFormed", isWellFormed, units.wellFormed());
    }
    // No units.stringView() when the unit iterator is not a pointer.
    // No begin() for a single-pass unit iterator.
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    expectedCP = isWellFormed ? u'カ' : sub<CP32, behavior>(parts[3]);
    assertEquals("iter[3] -> codePoint", expectedCP, iter->codePoint());
    ++iter;
    // Fetch the current code point twice.
    assertFalse("iter != endIter", iter == rangeLimit);
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", parts[4].length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertTrue("iter[4] * wellFormed", units.wellFormed());
    }
    assertTrue("iter == endIter", iter == rangeLimit);
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testFwdIter(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    // "abçカ🚴"
    FwdIter<Unit> goodBegin(joined.data());
    FwdIter<Unit> goodLimit(joined.data() + joined.length());
    if constexpr (mode == UNSAFE) {
        auto iter = unsafeUTFIterator<CP32>(goodBegin);
        auto rangeLimit = unsafeUTFIterator<CP32>(goodLimit);
        testFwdIter<mode, StringView>(parts, goodLimit, iter, rangeLimit);
    } else {
        auto iter = utfIterator<CP32, behavior>(goodBegin, goodLimit);
        auto rangeLimit = utfIterator<CP32, behavior>(goodLimit);
        testFwdIter<mode, StringView>(parts, goodLimit, iter, rangeLimit);
    }
}

template<TestMode mode, typename CP32, UTFIllFormedBehavior behavior, typename StringView>
void UTFIteratorTest::testFwdIterNul(StringView piped) {
    using Unit = typename StringView::value_type;
    auto parts = split(piped);
    auto joined = join<Unit>(parts);
    // "abçカ🚴"
    FwdIter<Unit> goodBegin(joined.data());
    Nul sentinel;
    if constexpr (mode == UNSAFE) {
        auto iter = unsafeUTFIterator<CP32>(goodBegin);
        testFwdIter<mode, StringView>(parts, sentinel, iter, sentinel);
    } else {
        auto iter = utfIterator<CP32, behavior>(goodBegin, sentinel);
        testFwdIter<mode, StringView>(parts, sentinel, iter, sentinel);
    }
}

template<TestMode mode, typename StringView,
         typename LimitIter, typename Iter, typename Sentinel>
void UTFIteratorTest::testFwdIter(const std::vector<StringView> &parts, LimitIter goodLimit,
                                  Iter iter, Sentinel rangeLimit) {
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
    assertEquals("iter[1] * length", parts[1].length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertTrue("iter[1] * wellFormed", units.wellFormed());
    }
    // No units.stringView() when the unit iterator is not a pointer.
    auto unitsIter = units.begin();
    for (auto c : parts[1]) {
        assertEquals("iter[1] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[1] * end()[0]", *units.end() == parts[2][0]);
    ++iter;
    assertEquals("iter[2] * codePoint", u'ç', (*iter++).codePoint());  // post-increment
    assertEquals("iter[3] -> codePoint", u'カ', iter->codePoint());
    assertFalse("iter[3] * end() != endIter", units.end() == goodLimit);
    ++iter;
    // Fetch the current code point twice.
    assertFalse("iter != endIter", iter == rangeLimit);
    assertEquals("iter[4.0] * codePoint", U'🚴', (*iter).codePoint());
    units = *iter++;
    assertEquals("iter[4] * codePoint", U'🚴', units.codePoint());
    assertEquals("iter[4] * length", parts[4].length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertTrue("iter[4] * wellFormed", units.wellFormed());
    }
    unitsIter = units.begin();
    for (auto c : parts[4]) {
        assertEquals("iter[back 4] * begin()[i]",
                     static_cast<UChar32>(c), static_cast<UChar32>(*unitsIter++));
    }
    assertTrue("iter[4] * end() == endIter", units.end() == goodLimit);
    assertTrue("iter == endIter", iter == rangeLimit);
}

namespace {

enum PartType { GOOD, BAD8, BAD16, BAD32 };

struct Part {
    constexpr Part(char32_t c) : type_(GOOD), len_(0), c_(c) {}
    constexpr Part(PartType t, int32_t u0) : type_(t), len_(1), u0_(u0) {}
    constexpr Part(PartType t, int32_t u0, int32_t u1) : type_(t), len_(2), u0_(u0), u1_(u1) {}
    constexpr Part(PartType t, int32_t u0, int32_t u1, int32_t u2) :
            type_(t), len_(3), u0_(u0), u1_(u1), u2_(u2) {}

    PartType type_;
    uint8_t len_;
    char32_t c_ = U'?';
    int32_t u0_ = 0;
    int32_t u1_ = 0;
    int32_t u2_ = 0;
};

// Careful: We test with the reverse order of parts as well.
// For that to yield self-contained results, parts must not
// continue sequences across part boundaries in either order.
constexpr Part testParts[] = {
    // "abçカ🚴"
    u'a',
    0x7f,
    0x80,
    Part(BAD8, 0xc0),
    Part(BAD8, 0x80),
    Part(BAD8, 0xc1),
    0,
    Part(BAD8, 0xe0),
    Part(BAD8, 0xe0, 0xa0),
    Part(BAD8, 0xe0, 0xbf),
    Part(BAD8, 0xed, 0x9f),
    // ED A0 xx .. ED BF xx would be surrogate code points
    Part(BAD8, 0xed),
    Part(BAD8, 0xa0),
    Part(BAD8, 0xed),
    Part(BAD8, 0xbf),
    u'ç',
    Part(BAD8, 0xbf),  // extra trail byte
    u'カ',
    Part(BAD8, 0xee, 0x80),
    Part(BAD8, 0xef, 0xbf),
    Part(BAD8, 0xf0),
    Part(BAD8, 0x8f),
    u'b',
    Part(BAD8, 0xf0),
    Part(BAD8, 0xf0, 0x90),
    Part(BAD8, 0xf0, 0x90, 0x80),
    Part(BAD8, 0xf4),
    Part(BAD8, 0xf4, 0x8f),
    Part(BAD8, 0xf4, 0x8f, 0xbf),
    Part(BAD8, 0xf5),
    Part(BAD8, 0xbf),
    U'🚴',
    Part(BAD8, 0x80),  // extra trail byte
    0x7ff,
    0x800,
    0xfff,
    0x1000,
    0xd7ff,
    Part(BAD16, 0xd800),
    Part(BAD16, 0xdbff),
    u'カ',
    Part(BAD16, 0xdc00),
    Part(BAD16, 0xdfff),
    0xe000,
    0xfffd,
    0xffff,
    0x10000,
    0x10ffff,
    Part(BAD32, 0x110000),
    Part(BAD32, -1)
};

}  // namespace

void UTFIteratorTest::initLong() {
    if (!longGood32.str.empty()) { return; }
    for (auto part : testParts) {
        switch (part.type_) {
        case GOOD: {
            char u8[4];
            size_t len8 = 0;
            U8_APPEND_UNSAFE(u8, len8, part.c_);
            longGood8.str.append(u8, len8);
            longGood8.parts.push_back({u8, len8});
            longBad8.str.append(u8, len8);
            longBad8.parts.push_back({u8, len8});
            longBad8.codePoints.push_back(part.c_);

            char16_t u16[2];
            size_t len16 = 0;
            U16_APPEND_UNSAFE(u16, len16, part.c_);
            longGood16.str.append(u16, len16);
            longGood16.parts.push_back({u16, len16});
            longBad16.str.append(u16, len16);
            longBad16.parts.push_back({u16, len16});
            longBad16.codePoints.push_back(part.c_);

            longGood32.str.push_back(part.c_);
            longGood32.parts.push_back({&part.c_, 1});
            longBad32.str.push_back(part.c_);
            longBad32.parts.push_back({&part.c_, 1});
            longBad32.codePoints.push_back(part.c_);
            break;
        }
        case BAD8: {
            char u8[3] = {
                static_cast<char>(part.u0_),
                static_cast<char>(part.u1_),
                static_cast<char>(part.u2_)
            };
            longBad8.str.append(u8, part.len_);
            longBad8.parts.push_back({u8, part.len_});
            longBad8.codePoints.push_back(U'?');
            break;
        }
        case BAD16: {  // surrogate code unit / code point
            char16_t u16 = part.u0_;
            longBad16.str.push_back(u16);
            longBad16.parts.push_back({&u16, 1});
            longBad16.codePoints.push_back(U'?');
            char32_t u32 = part.u0_;
            longBad32.str.push_back(u32);
            longBad32.parts.push_back({&u32, 1});
            longBad32.codePoints.push_back(U'?');
            break;
        }
        case BAD32: {
            char32_t u32 = part.u0_;
            longBad32.str.push_back(u32);
            longBad32.parts.push_back({&u32, 1});
            longBad32.codePoints.push_back(U'?');
            break;
        }
        }
    }
    longGood8.codePoints = longGood16.codePoints = longGood32.codePoints = longGood32.str;
}

template<TestMode mode, UTFIllFormedBehavior behavior, IterType type, typename Unit, typename Units>
void UTFIteratorTest::checkUnits(
        const Units &units, std::basic_string_view<Unit> part, UChar32 expectedCP) {
    bool expectedWellFormed = true;
    if (expectedCP == u'?') {
        expectedCP = sub<UChar32, behavior>(part);
        expectedWellFormed = false;
    }
    assertEquals("cp[i]", expectedCP, units.codePoint());
    assertEquals("length[i]", part.length(), units.length());
    if constexpr (mode != UNSAFE) {
        assertEquals("wellFormed[i]", expectedWellFormed, units.wellFormed());
    }
    if constexpr (type >= FWD) {
        int32_t j = 0;
        for (Unit unit : units) {  // begin()..end()
            assertEquals("units.iter[i][j]",
                         static_cast<UChar32>(part[j]), static_cast<UChar32>(unit));
            ++j;
        }
        assertEquals("units.iter.length[i]", part.length(), j);
    }
    if constexpr (type >= CONTIG) {
        assertTrue("stringView[i]", part == units.stringView());
    }
}

template<TestMode mode, UTFIllFormedBehavior behavior, IterType type, typename Unit, typename Iter>
void UTFIteratorTest::zigzag(const ImplTest<Unit> &test, size_t i,
                             const Iter &begin, Iter iter, const Iter &end) {
    static constexpr const char *path = "**+*+--*PPp++*p--+P+pP-*-*";
    size_t iLimit = test.codePoints.length();
    for (const char *p = path; *p != 0; ++p) {
        switch(*p) {
        case '*':
            if (i < iLimit) {
                checkUnits<mode, behavior, type, Unit>(
                    *iter, test.parts[i], test.codePoints[i]);
            }
            break;
        case '+':  // pre-increment
            if (i < iLimit) {
                ++i;
                ++iter;
            } else {
                assertTrue("at limit", iter == end);
            }
            break;
        case '-':  // pre-decrement
            if (i > 0) {
                --i;
                --iter;
            } else {
                assertTrue("at start", iter == begin);
            }
            break;
        case 'P':  // post-increment
            if (i < iLimit) {
                checkUnits<mode, behavior, type, Unit>(
                    *iter++, test.parts[i], test.codePoints[i]);
                ++i;
            }
            break;
        case 'p':  // post-decrement
            if (0 < i && i < iLimit) {
                checkUnits<mode, behavior, type, Unit>(
                    *iter--, test.parts[i], test.codePoints[i]);
                --i;
            }
            break;
        default:
            break;
        }
    }
}

#if defined(__cpp_lib_concepts) && __cpp_lib_concepts >= 2020'02 // Test against C++20 concepts.
namespace {
template <typename Iterator>
using CodePointIterator = UTFIterator<char32_t, UTF_BEHAVIOR_FFFD, Iterator>;
template <typename Iterator>
using UnsafeCodePointIterator = UnsafeUTFIterator<char32_t, Iterator>;

// Check that the iterators satisfy the right concepts.
namespace input {
using CodeUnitIterator = std::istreambuf_iterator<char16_t>;
static_assert(std::input_iterator<CodeUnitIterator>);
static_assert(!std::forward_iterator<CodeUnitIterator>);
static_assert(std::input_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(!std::forward_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(std::input_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
static_assert(!std::forward_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
} // namespace input
namespace forward {
using CodeUnitIterator = std::forward_list<char16_t>::iterator;
static_assert(std::forward_iterator<CodeUnitIterator>);
static_assert(!std::bidirectional_iterator<CodeUnitIterator>);
static_assert(std::forward_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(!std::bidirectional_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(std::forward_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
static_assert(!std::bidirectional_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
} // namespace forward
namespace bidirectional {
using CodeUnitIterator = std::list<char16_t>::iterator;
static_assert(std::bidirectional_iterator<CodeUnitIterator>);
static_assert(!std::random_access_iterator<CodeUnitIterator>);
static_assert(std::bidirectional_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(!std::random_access_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(std::bidirectional_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
static_assert(!std::random_access_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
} // namespace bidirectional
namespace contiguous {
using CodeUnitIterator = std::u16string::iterator;
static_assert(std::contiguous_iterator<CodeUnitIterator>);
static_assert(std::bidirectional_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(!std::random_access_iterator<CodePointIterator<CodeUnitIterator>>);
static_assert(std::bidirectional_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
static_assert(!std::random_access_iterator<UnsafeCodePointIterator<CodeUnitIterator>>);
} // namespace contiguous

} // namespace
#endif

void UTFIteratorTest::testAllCodePoints() {
    int32_t count = 0;
    UChar32 previous = -1;
    for (UChar32 c : icu::header::AllCodePoints<UChar32>()) {
        // Not assertTrue() / assertEquals() because they are slow for this many code points.
        if (!U_IS_CODE_POINT(c)) {
            errln("!U_IS_CODE_POINT(U+%04lx)", static_cast<long>(c));
        }
        if (c != (previous + 1)) {
            errln("expected U+%04lx = U+%04lx + 1",
                  static_cast<long>(c), static_cast<long>(previous));
        }
        previous = c;
        ++count;
    }
    assertEquals("count", 0x110000, count);
}

void UTFIteratorTest::testAllScalarValues() {
    int32_t count = 0;
    UChar32 previous = -1;
    for (UChar32 c : icu::header::AllScalarValues<UChar32>()) {
        // Not assertTrue() / assertEquals() because they are slow for this many code points.
        if (!U_IS_SCALAR_VALUE(c)) {
            errln("!U_IS_SCALAR_VALUE(U+%04lx)", static_cast<long>(c));
        }
        if (previous == 0xd7ff) {
            previous = 0xdfff;
        }
        if (c != (previous + 1)) {
            errln("expected U+%04lx = U+%04lx + 1",
                  static_cast<long>(c), static_cast<long>(previous));
        }
        previous = c;
        ++count;
    }
    assertEquals("count", 0x110000 - 0x800, count);
}
