// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfiterator.h
// created: 2024aug12 Markus W. Scherer

#ifndef __UTFITERATOR_H__
#define __UTFITERATOR_H__

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API || !defined(UTYPES_H)

#include <iterator>
#include <string>
#include <string_view>
#include <type_traits>
#include "unicode/utf16.h"
#include "unicode/utf8.h"
#include "unicode/uversion.h"

/**
 * \file
 * \brief C++ header-only API: C++ iterators over Unicode strings (=UTF-8/16/32 if well-formed).
 *
 * Sample code:
 * \code
 * #include <string_view>
 * #include <iostream>
 * #include "unicode/utypes.h"
 * #include "unicode/utfiterator.h"
 *
 * using U_HEADER_ONLY_NAMESPACE::utfIterator;
 * using U_HEADER_ONLY_NAMESPACE::utfStringCodePoints;
 * using U_HEADER_ONLY_NAMESPACE::unsafeUTFIterator;
 * using U_HEADER_ONLY_NAMESPACE::unsafeUTFStringCodePoints;
 *
 * int32_t rangeLoop16(std::u16string_view s) {
 *     // We are just adding up the code points for minimal-code demonstration purposes.
 *     int32_t sum = 0;
 *     for (auto units : utfStringCodePoints<UChar32, UTF_BEHAVIOR_NEGATIVE>(s)) {
 *         sum += units.codePoint();  // < 0 if ill-formed
 *     }
 *     return sum;
 * }
 *
 * int32_t loopIterPlusPlus16(std::u16string_view s) {
 *     auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
 *     int32_t sum = 0;
 *     for (auto iter = range.begin(), limit = range.end(); iter != limit;) {
 *         sum += (*iter++).codePoint();  // U+FFFD if ill-formed
 *     }
 *     return sum;
 * }
 *
 * int32_t backwardLoop16(std::u16string_view s) {
 *     auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_SURROGATE>(s);
 *     int32_t sum = 0;
 *     for (auto start = range.begin(), iter = range.end(); start != iter;) {
 *         sum += (*--iter).codePoint();  // surrogate code point if unpaired / ill-formed
 *     }
 *     return sum;
 * }
 *
 * int32_t reverseLoop8(std::string_view s) {
 *     auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
 *     int32_t sum = 0;
 *     for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
 *         sum += iter->codePoint();  // U+FFFD if ill-formed
 *     }
 *     return sum;
 * }
 *
 * int32_t countCodePoints16(std::u16string_view s) {
 *     auto range = utfStringCodePoints<UChar32, UTF_BEHAVIOR_SURROGATE>(s);
 *     return std::distance(range.begin(), range.end());
 * }
 *
 * int32_t unsafeRangeLoop16(std::u16string_view s) {
 *     int32_t sum = 0;
 *     for (auto units : unsafeUTFStringCodePoints<UChar32>(s)) {
 *         sum += units.codePoint();
 *     }
 *     return sum;
 * }
 *
 * int32_t unsafeReverseLoop8(std::string_view s) {
 *     auto range = unsafeUTFStringCodePoints<UChar32>(s);
 *     int32_t sum = 0;
 *     for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
 *         sum += iter->codePoint();
 *     }
 *     return sum;
 * }
 *
 * char32_t firstCodePointOrFFFD16(std::u16string_view s) {
 *     if (s.empty()) { return 0xfffd; }
 *     auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
 *     return range.begin()->codePoint();
 * }
 *
 * std::string_view firstSequence8(std::string_view s) {
 *     if (s.empty()) { return {}; }
 *     auto range = utfStringCodePoints<char32_t, UTF_BEHAVIOR_FFFD>(s);
 *     auto units = *(range.begin());
 *     if (units.wellFormed()) {
 *         return units.stringView();
 *     } else {
 *         return {};
 *     }
 * }
 *
 * template<typename InputStream>  // some istream or streambuf
 * std::u32string cpFromInput(InputStream &in) {
 *     // This is a single-pass input_iterator.
 *     std::istreambuf_iterator bufIter(in);
 *     std::istreambuf_iterator<typename InputStream::char_type> bufLimit;
 *     auto iter = utfIterator<char32_t, UTF_BEHAVIOR_FFFD>(bufIter);
 *     auto limit = utfIterator<char32_t, UTF_BEHAVIOR_FFFD>(bufLimit);
 *     std::u32string s32;
 *     for (; iter != limit; ++iter) {
 *         s32.push_back(iter->codePoint());
 *     }
 *     return s32;
 * }
 *
 * std::u32string cpFromStdin() { return cpFromInput(std::cin); }
 * std::u32string cpFromWideStdin() { return cpFromInput(std::wcin); }
 * \endcode
 */

#ifndef U_HIDE_DRAFT_API

/**
 * Some defined behaviors for handling ill-formed Unicode strings.
 * This is a template parameter for UTFIterator and related classes.
 *
 * When a validating UTFIterator encounters an ill-formed code unit sequence,
 * then CodeUnits.codePoint() is a value according to this parameter.
 *
 * @draft ICU 78
 * @see CodeUnits
 * @see UTFIterator
 * @see UTFStringCodePoints
 */
typedef enum UTFIllFormedBehavior {
    /**
     * Returns a negative value (-1=U_SENTINEL) instead of a code point.
     * If the CP32 template parameter for the relevant classes is an unsigned type,
     * then the negative value becomes 0xffffffff=UINT32_MAX.
     *
     * @draft ICU 78
     */
    UTF_BEHAVIOR_NEGATIVE,
    /** Returns U+FFFD Replacement Character. @draft ICU 78 */
    UTF_BEHAVIOR_FFFD,
    /**
     * UTF-8: Not allowed;
     * UTF-16: returns the unpaired surrogate;
     * UTF-32: returns the surrogate code point, or U+FFFD if out of range.
     *
     * @draft ICU 78
     */
    UTF_BEHAVIOR_SURROGATE
} UTFIllFormedBehavior;

namespace U_HEADER_ONLY_NAMESPACE {

/**
 * Result of decoding a code unit sequence for one code point.
 * Returned from non-validating Unicode string code point iterators.
 * Base class for class CodeUnits which is returned from validating iterators.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @see UnsafeUTFIterator
 * @see UnsafeUTFStringCodePoints
 * @draft ICU 78
 */
template<typename CP32, typename UnitIter, typename = void>
class UnsafeCodeUnits {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Unit = typename std::iterator_traits<UnitIter>::value_type;
public:
    /** @internal */
    UnsafeCodeUnits(CP32 codePoint, uint8_t length, UnitIter start, UnitIter limit) :
            c_(codePoint), len_(length), start_(start), limit_(limit) {}

    /** Copy constructor. @draft ICU 78 */
    UnsafeCodeUnits(const UnsafeCodeUnits &other) = default;
    /** Copy assignment operator. @draft ICU 78 */
    UnsafeCodeUnits &operator=(const UnsafeCodeUnits &other) = default;

    /**
     * @return the Unicode code point decoded from the code unit sequence.
     *     If the sequence is ill-formed and the iterator validates,
     *     then this is a replacement value according to the iterator‘s
     *     UTFIllFormedBehavior template parameter.
     * @draft ICU 78
     */
    UChar32 codePoint() const { return c_; }

    /**
     * @return the start of the code unit sequence for one code point.
     * Only enabled if UnitIter is a (multi-pass) forward_iterator or better.
     * @draft ICU 78
     */
    UnitIter begin() const { return start_; }

    /**
     * @return the limit (exclusive end) of the code unit sequence for one code point.
     * Only enabled if UnitIter is a (multi-pass) forward_iterator or better.
     * @draft ICU 78
     */
    UnitIter end() const { return limit_; }

    /**
     * @return the length of the code unit sequence for one code point.
     * @draft ICU 78
     */
    uint8_t length() const { return len_; }

    // C++17: There is no test for contiguous_iterator, so we just work with pointers
    // and with string and string_view iterators.
    /**
     * @return a string_view of the code unit sequence for one code point.
     * Only enabled if UnitIter is a pointer, a string_view::iterator, or a string::iterator.
     * @draft ICU 78
     */
    template<typename Iter = UnitIter>
    std::enable_if_t<
        std::is_pointer_v<Iter> ||
            std::is_same_v<Iter, typename std::basic_string<Unit>::iterator> ||
            std::is_same_v<Iter, typename std::basic_string_view<Unit>::iterator>,
        std::basic_string_view<Unit>>
    stringView() const {
        // C++20:
        // - require https://en.cppreference.com/w/cpp/iterator/contiguous_iterator
        // - return string_view(begin(), end())
        return std::basic_string_view<Unit>(&*start_, len_);
    }

private:
    // Order of fields with padding and access frequency in mind.
    CP32 c_;
    uint8_t len_;
    UnitIter start_;
    UnitIter limit_;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
// No UnitIter field, no getter for it, no stringView().
template<typename CP32, typename UnitIter>
class UnsafeCodeUnits<
        CP32,
        UnitIter,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
public:
    UnsafeCodeUnits(CP32 codePoint, uint8_t length) : c_(codePoint), len_(length) {}

    UnsafeCodeUnits(const UnsafeCodeUnits &other) = default;
    UnsafeCodeUnits &operator=(const UnsafeCodeUnits &other) = default;

    UChar32 codePoint() const { return c_; }

    uint8_t length() const { return len_; }

private:
    // Order of fields with padding and access frequency in mind.
    CP32 c_;
    uint8_t len_;
};
#endif  // U_IN_DOXYGEN

/**
 * Result of validating and decoding a code unit sequence for one code point.
 * Returned from validating Unicode string code point iterators.
 * Adds function wellFormed() to base class UnsafeCodeUnits.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @see UTFIterator
 * @see UTFStringCodePoints
 * @draft ICU 78
 */
template<typename CP32, typename UnitIter, typename = void>
class CodeUnits : public UnsafeCodeUnits<CP32, UnitIter> {
public:
    /** @internal */
    CodeUnits(CP32 codePoint, uint8_t length, bool wellFormed, UnitIter start, UnitIter limit) :
            UnsafeCodeUnits<CP32, UnitIter>(codePoint, length, start, limit), ok_(wellFormed) {}

    /** Copy constructor. @draft ICU 78 */
    CodeUnits(const CodeUnits &other) = default;
    /** Copy assignment operator. @draft ICU 78 */
    CodeUnits &operator=(const CodeUnits &other) = default;

    /**
     * @return true if the decoded code unit sequence is well-formed.
     * @draft ICU 78
     */
    bool wellFormed() const { return ok_; }

private:
    bool ok_;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
// No UnitIter field, no getter for it, no stringView().
template<typename CP32, typename UnitIter>
class CodeUnits<
        CP32,
        UnitIter,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> :
            public UnsafeCodeUnits<CP32, UnitIter> {
public:
    CodeUnits(CP32 codePoint, uint8_t length, bool wellFormed) :
            UnsafeCodeUnits<CP32, UnitIter>(codePoint, length), ok_(wellFormed) {}

    CodeUnits(const CodeUnits &other) = default;
    CodeUnits &operator=(const CodeUnits &other) = default;

    bool wellFormed() const { return ok_; }

private:
    bool ok_;
};
#endif  // U_IN_DOXYGEN

// Validating implementations ---------------------------------------------- ***

#ifndef U_IN_DOXYGEN
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter, typename = void>
class UTFImpl;

// Note: readAndInc() functions take both a p0 and a p iterator.
// They must have the same value.
// For a multi-pass UnitIter, the caller must copy its p into a local variable p0,
// and readAndInc() copies p0 and the incremented p into the CodeUnits.
// For a single-pass UnitIter, which may not be default-constructible nor coypable,
// the caller can pass p into both references, and readAndInc() does not use p0
// and constructs CodeUnits without them.
// Moving the p0 variable into the call site avoids having to declare it inside readAndInc()
// which may not be possible for a single-pass iterator.

// UTF-8
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
class UTFImpl<
        CP32,
        behavior,
        UnitIter,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 1>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    static_assert(behavior != UTF_BEHAVIOR_SURROGATE,
                  "For 8-bit strings, the SURROGATE option does not have an equivalent.");
public:
    // Handle ill-formed UTF-8
    U_FORCE_INLINE static CP32 sub() {
        switch (behavior) {
            case UTF_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case UTF_BEHAVIOR_FFFD: return 0xfffd;
        }
    }

    U_FORCE_INLINE static void inc(UnitIter &p, const UnitIter &limit) {
        // Very similar to U8_FWD_1().
        uint8_t b = *p;
        ++p;
        if (U8_IS_LEAD(b) && p != limit) {
            uint8_t t1 = *p;
            if ((0xe0 <= b && b < 0xf0)) {
                if (U8_IS_VALID_LEAD3_AND_T1(b, t1) &&
                        ++p != limit && U8_IS_TRAIL(*p)) {
                    ++p;
                }
            } else if (b < 0xe0) {
                if (U8_IS_TRAIL(t1)) {
                    ++p;
                }
            } else /* b >= 0xf0 */ {
                if (U8_IS_VALID_LEAD4_AND_T1(b, t1) &&
                        ++p != limit && U8_IS_TRAIL(*p) &&
                        ++p != limit && U8_IS_TRAIL(*p)) {
                    ++p;
                }
            }
        }
    }

    U_FORCE_INLINE static void dec(UnitIter start, UnitIter &p) {
        // Very similar to U8_BACK_1().
        uint8_t c = *--p;
        if (U8_IS_TRAIL(c) && p != start) {
            UnitIter p1 = p;
            uint8_t b1 = *--p1;
            if (U8_IS_LEAD(b1)) {
                if (b1 < 0xe0 ||
                        (b1 < 0xf0 ?
                            U8_IS_VALID_LEAD3_AND_T1(b1, c) :
                            U8_IS_VALID_LEAD4_AND_T1(b1, c))) {
                    p = p1;
                    return;
                }
            } else if (U8_IS_TRAIL(b1) && p1 != start) {
                uint8_t b2 = *--p1;
                if (0xe0 <= b2 && b2 <= 0xf4) {
                    if (b2 < 0xf0 ?
                            U8_IS_VALID_LEAD3_AND_T1(b2, b1) :
                            U8_IS_VALID_LEAD4_AND_T1(b2, b1)) {
                        p = p1;
                        return;
                    }
                } else if (U8_IS_TRAIL(b2) && p1 != start) {
                    uint8_t b3 = *--p1;
                    if (0xf0 <= b3 && b3 <= 0xf4 && U8_IS_VALID_LEAD4_AND_T1(b3, b2)) {
                        p = p1;
                        return;
                    }
                }
            }
        }
    }

    U_FORCE_INLINE static CodeUnits<CP32, UnitIter> readAndInc(
            UnitIter &p0, UnitIter &p, const UnitIter &limit) {
        constexpr bool isMultiPass =
            std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>;
        // Very similar to U8_NEXT_OR_FFFD().
        CP32 c = uint8_t(*p);
        ++p;
        if (U8_IS_SINGLE(c)) {
            if constexpr (isMultiPass) {
                return {c, 1, true, p0, p};
            } else {
                return {c, 1, true};
            }
        }
        uint8_t length = 1;
        uint8_t t = 0;
        if (p != limit &&
                // fetch/validate/assemble all but last trail byte
                (c >= 0xe0 ?
                    (c < 0xf0 ?  // U+0800..U+FFFF except surrogates
                        U8_LEAD3_T1_BITS[c &= 0xf] & (1 << ((t = *p) >> 5)) &&
                        (t &= 0x3f, 1)
                    :  // U+10000..U+10FFFF
                        (c -= 0xf0) <= 4 &&
                        U8_LEAD4_T1_BITS[(t = *p) >> 4] & (1 << c) &&
                        (c = (c << 6) | (t & 0x3f), ++length, ++p != limit) &&
                        (t = *p - 0x80) <= 0x3f) &&
                    // valid second-to-last trail byte
                    (c = (c << 6) | t, ++length, ++p != limit)
                :  // U+0080..U+07FF
                    c >= 0xc2 && (c &= 0x1f, 1)) &&
                // last trail byte
                (t = *p - 0x80) <= 0x3f) {
            c = (c << 6) | t;
            ++length;
            ++p;
            if constexpr (isMultiPass) {
                return {c, length, true, p0, p};
            } else {
                return {c, length, true};
            }
        }
        if constexpr (isMultiPass) {
            return {sub(), length, false, p0, p};
        } else {
            return {sub(), length, false};
        }
    }

    U_FORCE_INLINE static CodeUnits<CP32, UnitIter> decAndRead(UnitIter start, UnitIter &p) {
        // Very similar to U8_PREV_OR_FFFD().
        UnitIter p0 = p;
        CP32 c = uint8_t(*--p);
        if (U8_IS_SINGLE(c)) {
            return {c, 1, true, p, p0};
        }
        if (U8_IS_TRAIL(c) && p != start) {
            UnitIter p1 = p;
            uint8_t b1 = *--p1;
            if (U8_IS_LEAD(b1)) {
                if (b1 < 0xe0) {
                    p = p1;
                    c = ((b1 - 0xc0) << 6) | (c & 0x3f);
                    return {c, 2, true, p, p0};
                } else if (b1 < 0xf0 ?
                            U8_IS_VALID_LEAD3_AND_T1(b1, c) :
                            U8_IS_VALID_LEAD4_AND_T1(b1, c)) {
                    // Truncated 3- or 4-byte sequence.
                    p = p1;
                    return {sub(), 2, false, p, p0};
                }
            } else if (U8_IS_TRAIL(b1) && p1 != start) {
                // Extract the value bits from the last trail byte.
                c &= 0x3f;
                uint8_t b2 = *--p1;
                if (0xe0 <= b2 && b2 <= 0xf4) {
                    if (b2 < 0xf0) {
                        b2 &= 0xf;
                        if (U8_IS_VALID_LEAD3_AND_T1(b2, b1)) {
                            p = p1;
                            c = (b2 << 12) | ((b1 & 0x3f) << 6) | c;
                            return {c, 3, true, p, p0};
                        }
                    } else if (U8_IS_VALID_LEAD4_AND_T1(b2, b1)) {
                        // Truncated 4-byte sequence.
                        p = p1;
                        return {sub(), 3, false, p, p0};
                    }
                } else if (U8_IS_TRAIL(b2) && p1 != start) {
                    uint8_t b3 = *--p1;
                    if (0xf0 <= b3 && b3 <= 0xf4) {
                        b3 &= 7;
                        if (U8_IS_VALID_LEAD4_AND_T1(b3, b2)) {
                            p = p1;
                            c = (b3 << 18) | ((b2 & 0x3f) << 12) | ((b1 & 0x3f) << 6) | c;
                            return {c, 4, true, p, p0};
                        }
                    }
                }
            }
        }
        return {sub(), 1, false, p, p0};
    }
};

// UTF-16
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
class UTFImpl<
        CP32,
        behavior,
        UnitIter,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 2>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
public:
    // Handle ill-formed UTF-16: One unpaired surrogate.
    U_FORCE_INLINE static CP32 sub(CP32 surrogate) {
        switch (behavior) {
            case UTF_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case UTF_BEHAVIOR_FFFD: return 0xfffd;
            case UTF_BEHAVIOR_SURROGATE: return surrogate;
        }
    }

    U_FORCE_INLINE static void inc(UnitIter &p, const UnitIter &limit) {
        // Very similar to U16_FWD_1().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
        }
    }

    U_FORCE_INLINE static void dec(UnitIter start, UnitIter &p) {
        // Very similar to U16_BACK_1().
        UnitIter p1;
        if (U16_IS_TRAIL(*--p) && p != start && (p1 = p, U16_IS_LEAD(*--p1))) {
            p = p1;
        }
    }

    U_FORCE_INLINE static CodeUnits<CP32, UnitIter> readAndInc(
            UnitIter &p0, UnitIter &p, const UnitIter &limit) {
        constexpr bool isMultiPass =
            std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>;
        // Very similar to U16_NEXT_OR_FFFD().
        CP32 c = *p;
        ++p;
        if (!U16_IS_SURROGATE(c)) {
            if constexpr (isMultiPass) {
                return {c, 1, true, p0, p};
            } else {
                return {c, 1, true};
            }
        } else {
            uint16_t c2;
            if (U16_IS_SURROGATE_LEAD(c) && p != limit && U16_IS_TRAIL(c2 = *p)) {
                ++p;
                c = U16_GET_SUPPLEMENTARY(c, c2);
                if constexpr (isMultiPass) {
                    return {c, 2, true, p0, p};
                } else {
                    return {c, 2, true};
                }
            } else {
                if constexpr (isMultiPass) {
                    return {sub(c), 1, false, p0, p};
                } else {
                    return {sub(c), 1, false};
                }
            }
        }
    }

    U_FORCE_INLINE static CodeUnits<CP32, UnitIter> decAndRead(UnitIter start, UnitIter &p) {
        // Very similar to U16_PREV_OR_FFFD().
        UnitIter p0 = p;
        CP32 c = *--p;
        if (!U16_IS_SURROGATE(c)) {
            return {c, 1, true, p, p0};
        } else {
            UnitIter p1;
            uint16_t c2;
            if (U16_IS_SURROGATE_TRAIL(c) && p != start && (p1 = p, U16_IS_LEAD(c2 = *--p1))) {
                p = p1;
                c = U16_GET_SUPPLEMENTARY(c2, c);
                return {c, 2, true, p, p0};
            } else {
                return {sub(c), 1, false, p, p0};
            }
        }
    }
};

// UTF-32: trivial, but still validating
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
class UTFImpl<
        CP32,
        behavior,
        UnitIter,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 4>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
public:
    // Handle ill-formed UTF-32
    U_FORCE_INLINE static CP32 sub(bool forSurrogate, CP32 surrogate) {
        switch (behavior) {
            case UTF_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case UTF_BEHAVIOR_FFFD: return 0xfffd;
            case UTF_BEHAVIOR_SURROGATE: return forSurrogate ? surrogate : 0xfffd;
        }
    }

    U_FORCE_INLINE static void inc(UnitIter &p, const UnitIter &/*limit*/) {
        ++p;
    }

    U_FORCE_INLINE static void dec(UnitIter /*start*/, UnitIter &p) {
        --p;
    }

    U_FORCE_INLINE static CodeUnits<CP32, UnitIter> readAndInc(
            UnitIter &p0, UnitIter &p, const UnitIter &/*limit*/) {
        constexpr bool isMultiPass =
            std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>;
        uint32_t uc = *p;
        CP32 c = uc;
        ++p;
        if (uc < 0xd800 || (0xe000 <= uc && uc <= 0x10ffff)) {
            if constexpr (isMultiPass) {
                return {c, 1, true, p0, p};
            } else {
                return {c, 1, true};
            }
        } else {
            if constexpr (isMultiPass) {
                return {sub(uc < 0xe000, c), 1, false, p0, p};
            } else {
                return {sub(uc < 0xe000, c), 1, false};
            }
        }
    }

    U_FORCE_INLINE static CodeUnits<CP32, UnitIter> decAndRead(UnitIter /*start*/, UnitIter &p) {
        UnitIter p0 = p;
        uint32_t uc = *--p;
        CP32 c = uc;
        if (uc < 0xd800 || (0xe000 <= uc && uc <= 0x10ffff)) {
            return {c, 1, true, p, p0};
        } else {
            return {sub(uc < 0xe000, c), 1, false, p, p0};
        }
    }
};

// Non-validating implementations ------------------------------------------ ***

template<typename CP32, typename UnitIter, typename = void>
class UnsafeUTFImpl;

// UTF-8
template<typename CP32, typename UnitIter>
class UnsafeUTFImpl<
        CP32,
        UnitIter,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 1>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
public:
    U_FORCE_INLINE static void inc(UnitIter &p) {
        // Very similar to U8_FWD_1_UNSAFE().
        uint8_t b = *p;
        std::advance(p, 1 + U8_COUNT_TRAIL_BYTES_UNSAFE(b));
    }

    U_FORCE_INLINE static void dec(UnitIter &p) {
        // Very similar to U8_BACK_1_UNSAFE().
        while (U8_IS_TRAIL(*--p)) {}
    }

    U_FORCE_INLINE static UnsafeCodeUnits<CP32, UnitIter> readAndInc(UnitIter &p0, UnitIter &p) {
        constexpr bool isMultiPass =
            std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>;
        // Very similar to U8_NEXT_UNSAFE().
        CP32 c = uint8_t(*p);
        ++p;
        if (U8_IS_SINGLE(c)) {
            if constexpr (isMultiPass) {
                return {c, 1, p0, p};
            } else {
                return {c, 1};
            }
        } else if (c < 0xe0) {
            c = ((c & 0x1f) << 6) | (*p & 0x3f);
            ++p;
            if constexpr (isMultiPass) {
                return {c, 2, p0, p};
            } else {
                return {c, 2};
            }
        } else if (c < 0xf0) {
            // No need for (c&0xf) because the upper bits are truncated
            // after <<12 in the cast to uint16_t.
            c = uint16_t(c << 12) | ((*p & 0x3f) << 6);
            ++p;
            c |= *p & 0x3f;
            ++p;
            if constexpr (isMultiPass) {
                return {c, 3, p0, p};
            } else {
                return {c, 3};
            }
        } else {
            c = ((c & 7) << 18) | ((*p & 0x3f) << 12);
            ++p;
            c |= (*p & 0x3f) << 6;
            ++p;
            c |= *p & 0x3f;
            ++p;
            if constexpr (isMultiPass) {
                return {c, 4, p0, p};
            } else {
                return {c, 4};
            }
        }
    }

    U_FORCE_INLINE static UnsafeCodeUnits<CP32, UnitIter> decAndRead(UnitIter &p) {
        // Very similar to U8_PREV_UNSAFE().
        UnitIter p0 = p;
        CP32 c = uint8_t(*--p);
        if (U8_IS_SINGLE(c)) {
            return {c, 1, p, p0};
        }
        // U8_IS_TRAIL(c) if well-formed
        c &= 0x3f;
        uint8_t count = 1;
        for (uint8_t shift = 6;;) {
            uint8_t b = *--p;
            if (b >= 0xc0) {
                U8_MASK_LEAD_BYTE(b, count);
                c |= uint32_t{b} << shift;
                break;
            } else {
                c |= (uint32_t{b} & 0x3f) << shift;
                ++count;
                shift += 6;
            }
        }
        ++count;
        return {c, count, p, p0};
    }
};

// UTF-16
template<typename CP32, typename UnitIter>
class UnsafeUTFImpl<
        CP32,
        UnitIter,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 2>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
public:
    U_FORCE_INLINE static void inc(UnitIter &p) {
        // Very similar to U16_FWD_1_UNSAFE().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c)) {
            ++p;
        }
    }

    U_FORCE_INLINE static void dec(UnitIter &p) {
        // Very similar to U16_BACK_1_UNSAFE().
        if (U16_IS_TRAIL(*--p)) {
            --p;
        }
    }

    U_FORCE_INLINE static UnsafeCodeUnits<CP32, UnitIter> readAndInc(UnitIter &p0, UnitIter &p) {
        constexpr bool isMultiPass =
            std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>;
        // Very similar to U16_NEXT_UNSAFE().
        CP32 c = *p;
        ++p;
        if (!U16_IS_LEAD(c)) {
            if constexpr (isMultiPass) {
                return {c, 1, p0, p};
            } else {
                return {c, 1};
            }
        } else {
            uint16_t c2 = *p;
            ++p;
            c = U16_GET_SUPPLEMENTARY(c, c2);
            if constexpr (isMultiPass) {
                return {c, 2, p0, p};
            } else {
                return {c, 2};
            }
        }
    }

    U_FORCE_INLINE static UnsafeCodeUnits<CP32, UnitIter> decAndRead(UnitIter &p) {
        // Very similar to U16_PREV_UNSAFE().
        UnitIter p0 = p;
        CP32 c = *--p;
        if (!U16_IS_TRAIL(c)) {
            return {c, 1, p, p0};
        } else {
            uint16_t c2 = *--p;
            c = U16_GET_SUPPLEMENTARY(c2, c);
            return {c, 2, p, p0};
        }
    }
};

// UTF-32: trivial
template<typename CP32, typename UnitIter>
class UnsafeUTFImpl<
        CP32,
        UnitIter,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 4>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
public:
    U_FORCE_INLINE static void inc(UnitIter &p) {
        ++p;
    }

    U_FORCE_INLINE static void dec(UnitIter &p) {
        --p;
    }

    U_FORCE_INLINE static UnsafeCodeUnits<CP32, UnitIter> readAndInc(UnitIter &p0, UnitIter &p) {
        constexpr bool isMultiPass =
            std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>;
        CP32 c = *p;
        ++p;
        if constexpr (isMultiPass) {
            return {c, 1, p0, p};
        } else {
            return {c, 1};
        }
    }

    U_FORCE_INLINE static UnsafeCodeUnits<CP32, UnitIter> decAndRead(UnitIter &p) {
        UnitIter p0 = p;
        CP32 c = *--p;
        return {c, 1, p, p0};
    }
};

#endif

// Validating iterators ---------------------------------------------------- ***

/**
 * Validating iterator over the code points in a Unicode string.
 *
 * The UnitIter can be
 * an input_iterator, a forward_iterator, or a bidirectional_iterator (including a pointer).
 * The UTFIterator will have the corresponding iterator_category.
 *
 * Call utfIterator() to have the compiler deduce the UnitIter type.
 *
 * For reverse iteration, either use this iterator directly as in <code>*--iter</code>
 * or wrap it using std::make_reverse_iterator(iter).
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @draft ICU 78
 * @see utfIterator
 */
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter, typename = void>
class UTFIterator {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Impl = UTFImpl<CP32, behavior, UnitIter>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        explicit Proxy(CodeUnits<CP32, UnitIter> &units) : units_(units) {}
        CodeUnits<CP32, UnitIter> &operator*() { return units_; }
        CodeUnits<CP32, UnitIter> *operator->() { return &units_; }
    private:
        CodeUnits<CP32, UnitIter> units_;
    };

public:
    /** C++ iterator boilerplate @internal */
    using value_type = CodeUnits<CP32, UnitIter>;
    /** C++ iterator boilerplate @internal */
    using reference = value_type;
    /** C++ iterator boilerplate @internal */
    using pointer = Proxy;
    /** C++ iterator boilerplate @internal */
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    /** C++ iterator boilerplate @internal */
    using iterator_category = std::conditional_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<UnitIter>::iterator_category>,
        std::bidirectional_iterator_tag,
        std::forward_iterator_tag>;

    /**
     * Constructor with start <= p < limit.
     * All of these iterators/pointers should be at code point boundaries.
     * Only enabled if UnitIter is a (multi-pass) forward_iterator or better.
     *
     * @param start Start of the range
     * @param p Initial position inside the range
     * @param limit Limit (exclusive end) of the range
     * @draft ICU 78
     */
    U_FORCE_INLINE UTFIterator(UnitIter start, UnitIter p, UnitIter limit) :
            p_(p), start_(start), limit_(limit), units_(0, 0, false, p, p) {}
    /**
     * Constructor with start == p < limit.
     * All of these iterators/pointers should be at code point boundaries.
     *
     * @param p Start of the range, and the initial position
     * @param limit Limit (exclusive end) of the range
     * @draft ICU 78
     */
    U_FORCE_INLINE UTFIterator(UnitIter p, UnitIter limit) :
            p_(p), start_(p), limit_(limit), units_(0, 0, false, p, p) {}
    /**
     * Constructs an iterator start or limit sentinel.
     * The iterator/pointer should be at a code point boundary.
     * Requires UnitIter to be copyable.
     *
     * @param p Range start or limit
     * @draft ICU 78
     */
    U_FORCE_INLINE explicit UTFIterator(UnitIter p) : p_(p), start_(p), limit_(p), units_(0, 0, false, p, p) {}
    /**
     * Default constructor. Makes a non-functional iterator.
     *
     * @draft ICU 78
     */
    U_FORCE_INLINE UTFIterator() : p_{}, start_{}, limit_{}, units_(0, 0, false, p_, p_) {}

    /** Move constructor. @draft ICU 78 */
    U_FORCE_INLINE UTFIterator(UTFIterator &&src) noexcept = default;
    /** Move assignment operator. @draft ICU 78 */
    U_FORCE_INLINE UTFIterator &operator=(UTFIterator &&src) noexcept = default;

    /** Copy constructor. @draft ICU 78 */
    U_FORCE_INLINE UTFIterator(const UTFIterator &other) = default;
    /** Copy assignment operator. @draft ICU 78 */
    U_FORCE_INLINE UTFIterator &operator=(const UTFIterator &other) = default;

    /**
     * @param other Another iterator
     * @return true if this operator is at the same position as the other one
     * @draft ICU 78
     */
    U_FORCE_INLINE bool operator==(const UTFIterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    /**
     * @param other Another iterator
     * @return true if this operator is not at the same position as the other one
     * @draft ICU 78
     */
    U_FORCE_INLINE bool operator!=(const UTFIterator &other) const { return !operator==(other); }

    /**
     * Decodes the code unit sequence at the current position.
     *
     * @return CodeUnits with the decoded code point etc.
     * @draft ICU 78
     */
    U_FORCE_INLINE CodeUnits<CP32, UnitIter> operator*() const {
        if (state_ == 0) {
            UnitIter p0 = p_;
            units_ = Impl::readAndInc(p0, p_, limit_);
            state_ = 1;
        }
        return units_;
    }

    /**
     * Decodes the code unit sequence at the current position.
     * Used like <code>iter->codePoint()</code> or <code>iter->stringView()</code> etc.
     *
     * @return CodeUnits with the decoded code point etc., wrapped into
     *     an opaque proxy object so that <code>iter->codePoint()</code> etc. works.
     * @draft ICU 78
     */
    U_FORCE_INLINE Proxy operator->() const {
        if (state_ == 0) {
            UnitIter p0 = p_;
            units_ = Impl::readAndInc(p0, p_, limit_);
            state_ = 1;
        }
        return Proxy(units_);
    }

    /**
     * Pre-increment operator.
     *
     * @return this iterator
     * @draft ICU 78
     */
    U_FORCE_INLINE UTFIterator &operator++() {  // pre-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::inc(p_, limit_);
        } else /* state_ < 0 */ {
            // operator--() called decAndRead() so we know how far to skip.
            p_ = units_.end();
            state_ = 0;
        }
        return *this;
    }

    /**
     * Post-increment operator.
     *
     * @return a copy of this iterator from before the increment.
     *     If UnitIter is a single-pass input_iterator, then this function
     *     returns an opaque proxy object so that <code>*iter++</code> still works.
     * @draft ICU 78
     */
    U_FORCE_INLINE UTFIterator operator++(int) {  // post-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            UTFIterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            UnitIter p0 = p_;
            units_ = Impl::readAndInc(p0, p_, limit_);
            UTFIterator result(*this);
            result.state_ = 1;
            // keep this->state_ == 0
            return result;
        } else /* state_ < 0 */ {
            UTFIterator result(*this);
            // operator--() called decAndRead() so we know how far to skip.
            p_ = units_.end();
            state_ = 0;
            return result;
        }
    }

    /**
     * Pre-decrement operator.
     * Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
     *
     * @return this iterator
     * @draft ICU 78
     */
    template<typename Iter = UnitIter>
    U_FORCE_INLINE
    std::enable_if_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<Iter>::iterator_category>,
        UTFIterator &>
    operator--() {  // pre-decrement
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is ahead of the logical position.
            p_ = units_.begin();
        }
        units_ = Impl::decAndRead(start_, p_);
        state_ = -1;
        return *this;
    }

    /**
     * Post-decrement operator.
     * Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
     *
     * @return a copy of this iterator from before the decrement.
     * @draft ICU 78
     */
    template<typename Iter = UnitIter>
    U_FORCE_INLINE
    std::enable_if_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<Iter>::iterator_category>,
        UTFIterator>
    operator--(int) {  // post-decrement
        UTFIterator result(*this);
        operator--();
        return result;
    }

private:
    friend class std::reverse_iterator<UTFIterator<CP32, behavior, UnitIter>>;

    U_FORCE_INLINE UnitIter getLogicalPosition() const {
        return state_ <= 0 ? p_ : units_.begin();
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need start_ & limit_ so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    UnitIter start_;
    UnitIter limit_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable CodeUnits<CP32, UnitIter> units_;
    // >0: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start
    mutable int8_t state_ = 0;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
class UTFIterator<
        CP32,
        behavior,
        UnitIter,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Impl = UTFImpl<CP32, behavior, UnitIter>;

    // Proxy type for post-increment return value, to make *iter++ work.
    // Also for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        explicit Proxy(CodeUnits<CP32, UnitIter> &units) : units_(units) {}
        CodeUnits<CP32, UnitIter> &operator*() { return units_; }
        CodeUnits<CP32, UnitIter> *operator->() { return &units_; }
    private:
        CodeUnits<CP32, UnitIter> units_;
    };

public:
    using value_type = CodeUnits<CP32, UnitIter>;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::input_iterator_tag;

    U_FORCE_INLINE UTFIterator(UnitIter p, UnitIter limit) : p_(std::move(p)), limit_(std::move(limit)) {}

    // Constructs an iterator start or limit sentinel.
    // Requires p to be copyable.
    U_FORCE_INLINE explicit UTFIterator(UnitIter p) : p_(std::move(p)), limit_(p_) {}

    U_FORCE_INLINE UTFIterator(UTFIterator &&src) noexcept = default;
    U_FORCE_INLINE UTFIterator &operator=(UTFIterator &&src) noexcept = default;

    U_FORCE_INLINE UTFIterator(const UTFIterator &other) = default;
    U_FORCE_INLINE UTFIterator &operator=(const UTFIterator &other) = default;

    U_FORCE_INLINE bool operator==(const UTFIterator &other) const {
        return p_ == other.p_ && ahead_ == other.ahead_;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot advance, or do arithmetic with, a single-pass UnitIter.
    }
    U_FORCE_INLINE bool operator!=(const UTFIterator &other) const { return !operator==(other); }

    U_FORCE_INLINE CodeUnits<CP32, UnitIter> operator*() const {
        if (!ahead_) {
            units_ = Impl::readAndInc(p_, p_, limit_);
            ahead_ = true;
        }
        return units_;
    }

    U_FORCE_INLINE Proxy operator->() const {
        if (!ahead_) {
            units_ = Impl::readAndInc(p_, p_, limit_);
            ahead_ = true;
        }
        return Proxy(units_);
    }

    U_FORCE_INLINE UTFIterator &operator++() {  // pre-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            Impl::inc(p_, limit_);
        }
        return *this;
    }

    U_FORCE_INLINE Proxy operator++(int) {  // post-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            units_ = Impl::readAndInc(p_, p_, limit_);
            // keep this->ahead_ == false
        }
        return Proxy(units_);
    }

private:
    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need limit_ so that when we read a code point
    // we can test if there are enough code units.
    UnitIter limit_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // so that we can use a single-pass input iterator for UnitIter.
    mutable CodeUnits<CP32, UnitIter> units_ = {0, 0, false};
    // true: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    // false: initial state
    mutable bool ahead_ = false;
};
#endif  // U_IN_DOXYGEN

}  // namespace U_HEADER_ONLY_NAMESPACE

#ifndef U_IN_DOXYGEN
// Bespoke specialization of reverse_iterator.
// The default implementation implements reverse operator*() and ++ in a way
// that does most of the same work twice for reading variable-length sequences.
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
class std::reverse_iterator<U_HEADER_ONLY_NAMESPACE::UTFIterator<CP32, behavior, UnitIter>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Impl = U_HEADER_ONLY_NAMESPACE::UTFImpl<CP32, behavior, UnitIter>;
    using CodeUnits_ = U_HEADER_ONLY_NAMESPACE::CodeUnits<CP32, UnitIter>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        explicit Proxy(CodeUnits_ units) : units_(units) {}
        CodeUnits_ &operator*() { return units_; }
        CodeUnits_ *operator->() { return &units_; }
    private:
        CodeUnits_ units_;
    };

public:
    using value_type = CodeUnits_;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::bidirectional_iterator_tag;

    U_FORCE_INLINE explicit reverse_iterator(U_HEADER_ONLY_NAMESPACE::UTFIterator<CP32, behavior, UnitIter> iter) :
            p_(iter.getLogicalPosition()), start_(iter.start_), limit_(iter.limit_),
            units_(0, 0, false, p_, p_) {}
    U_FORCE_INLINE reverse_iterator() : p_{}, start_{}, limit_{}, units_(0, 0, false, p_, p_) {}

    U_FORCE_INLINE reverse_iterator(reverse_iterator &&src) noexcept = default;
    U_FORCE_INLINE reverse_iterator &operator=(reverse_iterator &&src) noexcept = default;

    U_FORCE_INLINE reverse_iterator(const reverse_iterator &other) = default;
    U_FORCE_INLINE reverse_iterator &operator=(const reverse_iterator &other) = default;

    U_FORCE_INLINE bool operator==(const reverse_iterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    U_FORCE_INLINE bool operator!=(const reverse_iterator &other) const { return !operator==(other); }

    U_FORCE_INLINE CodeUnits_ operator*() const {
        if (state_ == 0) {
            units_ = Impl::decAndRead(start_, p_);
            state_ = -1;
        }
        return units_;
    }

    U_FORCE_INLINE Proxy operator->() const {
        if (state_ == 0) {
            units_ = Impl::decAndRead(start_, p_);
            state_ = -1;
        }
        return Proxy(units_);
    }

    U_FORCE_INLINE reverse_iterator &operator++() {  // pre-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::dec(start_, p_);
        } else /* state_ > 0 */ {
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.begin();
            state_ = 0;
        }
        return *this;
    }

    U_FORCE_INLINE reverse_iterator operator++(int) {  // post-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            reverse_iterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            units_ = Impl::decAndRead(start_, p_);
            reverse_iterator result(*this);
            result.state_ = -1;
            // keep this->state_ == 0
            return result;
        } else /* state_ > 0 */ {
            reverse_iterator result(*this);
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.begin();
            state_ = 0;
            return result;
        }
    }

    U_FORCE_INLINE reverse_iterator &operator--() {  // pre-decrement
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is behind the logical position.
            p_ = units_.end();
        }
        UnitIter p0 = p_;
        units_ = Impl::readAndInc(p0, p_, limit_);
        state_ = 1;
        return *this;
    }

    U_FORCE_INLINE reverse_iterator operator--(int) {  // post-decrement
        reverse_iterator result(*this);
        operator--();
        return result;
    }

private:
    U_FORCE_INLINE UnitIter getLogicalPosition() const {
        return state_ >= 0 ? p_ : units_.end();
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need start_ & limit_ so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    UnitIter start_;
    UnitIter limit_;
    // Keep state so that we call decAndRead() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable CodeUnits_ units_;
    // >0: units_ = readAndInc(), p_ = units limit
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start
    //     which means that p_ is behind its logical position
    mutable int8_t state_ = 0;
};
#endif  // U_IN_DOXYGEN

namespace U_HEADER_ONLY_NAMESPACE {

/**
 * A C++ "range" for validating iteration over all of the code points of a Unicode string.
 *
 * Call utfStringCodePoints() to have the compiler deduce the Unit type.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @tparam Unit Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @draft ICU 78
 * @see utfStringCodePoints
 */
template<typename CP32, UTFIllFormedBehavior behavior, typename Unit>
class UTFStringCodePoints {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using UnitIter = typename std::basic_string_view<Unit>::iterator;
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    explicit UTFStringCodePoints(std::basic_string_view<Unit> s) : s(s) {}

    /** Copy constructor. @draft ICU 78 */
    UTFStringCodePoints(const UTFStringCodePoints &other) = default;

    /** Copy assignment operator. @draft ICU 78 */
    UTFStringCodePoints &operator=(const UTFStringCodePoints &other) = default;

    /**
     * @return the range start iterator
     * @draft ICU 78
     */
    auto begin() const {
        return UTFIterator<CP32, behavior, UnitIter>(s.begin(), s.begin(), s.end());
    }

    /**
     * @return the range limit (exclusive end) iterator
     * @draft ICU 78
     */
    auto end() const {
        return UTFIterator<CP32, behavior, UnitIter>(s.begin(), s.end(), s.end());
    }

    /**
     * @return std::reverse_iterator(end())
     * @draft ICU 78
     */
    auto rbegin() const {
        return std::make_reverse_iterator(end());
    }

    /**
     * @return std::reverse_iterator(begin())
     * @draft ICU 78
     */
    auto rend() const {
        return std::make_reverse_iterator(begin());
    }

private:
    std::basic_string_view<Unit> s;
};

/**
 * UTFIterator factory function for start <= p < limit.
 * Deduces the UnitIter template parameter from the inputs.
 * Only enabled if UnitIter is a (multi-pass) forward_iterator or better.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam behavior How to handle ill-formed Unicode strings
 * @tparam UnitIter Can usually be omitted/deduced:
 *     An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @param start start code unit iterator
 * @param p current-position code unit iterator
 * @param limit limit (exclusive-end) code unit iterator
 * @return a UTFIterator&lt;CP32, behavior, UnitIter&gt;
 *     for the given code unit iterators or character pointers
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
auto utfIterator(UnitIter start, UnitIter p, UnitIter limit) {
    return UTFIterator<CP32, behavior, UnitIter>(std::move(start), std::move(p), std::move(limit));
}

/**
 * UTFIterator factory function for start = p < limit.
 * Deduces the UnitIter template parameter from the inputs.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam behavior How to handle ill-formed Unicode strings
 * @tparam UnitIter Can usually be omitted/deduced:
 *     An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @param p start and current-position code unit iterator
 * @param limit limit (exclusive-end) code unit iterator
 * @return a UTFIterator&lt;CP32, behavior, UnitIter&gt;
 *     for the given code unit iterators or character pointers
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
auto utfIterator(UnitIter p, UnitIter limit) {
    return UTFIterator<CP32, behavior, UnitIter>(std::move(p), std::move(limit));
}

// Note: We should only enable the following factory function for a copyable UnitIter.
// In C++17, we would have to partially specialize with enable_if_t testing for forward_iterator,
// but a function template partial specialization is not allowed.
// In C++20, we might be able to require the std::copyable concept.

/**
 * UTFIterator factory function for a start or limit sentinel.
 * Deduces the UnitIter template parameter from the input.
 * Requires UnitIter to be copyable.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam behavior How to handle ill-formed Unicode strings
 * @tparam UnitIter Can usually be omitted/deduced:
 *     An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @param p code unit iterator
 * @return a UTFIterator&lt;CP32, behavior, UnitIter&gt;
 *     for the given code unit iterator or character pointer
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior, typename UnitIter>
auto utfIterator(UnitIter p) {
    return UTFIterator<CP32, behavior, UnitIter>(std::move(p));
}

/**
 * UTFStringCodePoints factory function for a "range" of code points in a string,
 * which validates while decoding.
 * Avoids having to explicitly specify the Unit template parameter for the UTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param s input string
 * @return a UTFStringCodePoints&lt;CP32, behavior, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior>
auto utfStringCodePoints(std::string_view s) {
    return UTFStringCodePoints<CP32, behavior, decltype(s)::value_type>(s);
}

/**
 * UTFStringCodePoints factory function for a "range" of code points in a string,
 * which validates while decoding.
 * Avoids having to explicitly specify the Unit template parameter for the UTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param s input string
 * @return a UTFStringCodePoints&lt;CP32, behavior, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior>
auto utfStringCodePoints(std::u16string_view s) {
    return UTFStringCodePoints<CP32, behavior, decltype(s)::value_type>(s);
}

/**
 * UTFStringCodePoints factory function for a "range" of code points in a string,
 * which validates while decoding.
 * Avoids having to explicitly specify the Unit template parameter for the UTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param s input string
 * @return a UTFStringCodePoints&lt;CP32, behavior, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior>
auto utfStringCodePoints(std::u32string_view s) {
    return UTFStringCodePoints<CP32, behavior, decltype(s)::value_type>(s);
}

#if U_CPLUSPLUS_VERSION >= 20
// The new type char8_t is distinct from char. u8"literals" are now char8_t literals.
/**
 * UTFStringCodePoints factory function for a "range" of code points in a string,
 * which validates while decoding.
 * Avoids having to explicitly specify the Unit template parameter for the UTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param s input string
 * @return a UTFStringCodePoints&lt;CP32, behavior, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior>
auto utfStringCodePoints(std::u8string_view s) {
    return UTFStringCodePoints<CP32, behavior, decltype(s)::value_type>(s);
}
#endif

/**
 * UTFStringCodePoints factory function for a "range" of code points in a string,
 * which validates while decoding.
 * Avoids having to explicitly specify the Unit template parameter for the UTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if UTF_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param s input string
 * @return a UTFStringCodePoints&lt;CP32, behavior, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32, UTFIllFormedBehavior behavior>
auto utfStringCodePoints(std::wstring_view s) {
    return UTFStringCodePoints<CP32, behavior, decltype(s)::value_type>(s);
}

// Non-validating iterators ------------------------------------------------ ***

/**
 * Non-validating iterator over the code points in a Unicode string.
 * The string must be well-formed.
 *
 * The UnitIter can be
 * an input_iterator, a forward_iterator, or a bidirectional_iterator (including a pointer).
 * The UTFIterator will have the corresponding iterator_category.
 *
 * Call unsafeUTFIterator() to have the compiler deduce the UnitIter type.
 *
 * For reverse iteration, either use this iterator directly as in <code>*--iter</code>
 * or wrap it using std::make_reverse_iterator(iter).
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @draft ICU 78
 * @see unsafeUTFIterator
 */
template<typename CP32, typename UnitIter, typename = void>
class UnsafeUTFIterator {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Impl = UnsafeUTFImpl<CP32, UnitIter>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning UnsafeCodeUnits.
    class Proxy {
    public:
        explicit Proxy(UnsafeCodeUnits<CP32, UnitIter> &units) : units_(units) {}
        UnsafeCodeUnits<CP32, UnitIter> &operator*() { return units_; }
        UnsafeCodeUnits<CP32, UnitIter> *operator->() { return &units_; }
    private:
        UnsafeCodeUnits<CP32, UnitIter> units_;
    };

public:
    /** C++ iterator boilerplate @internal */
    using value_type = UnsafeCodeUnits<CP32, UnitIter>;
    /** C++ iterator boilerplate @internal */
    using reference = value_type;
    /** C++ iterator boilerplate @internal */
    using pointer = Proxy;
    /** C++ iterator boilerplate @internal */
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    /** C++ iterator boilerplate @internal */
    using iterator_category = std::conditional_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<UnitIter>::iterator_category>,
        std::bidirectional_iterator_tag,
        std::forward_iterator_tag>;

    /**
     * Constructor; the iterator/pointer should be at a code point boundary.
     *
     * @param p Initial position inside the range, or a range sentinel
     * @draft ICU 78
     */
    U_FORCE_INLINE explicit UnsafeUTFIterator(UnitIter p) : p_(p), units_(0, 0, p, p) {}
    /**
     * Default constructor. Makes a non-functional iterator.
     *
     * @draft ICU 78
     */
    U_FORCE_INLINE UnsafeUTFIterator() : p_{}, units_(0, 0, p_, p_) {}

    /** Move constructor. @draft ICU 78 */
    U_FORCE_INLINE UnsafeUTFIterator(UnsafeUTFIterator &&src) noexcept = default;
    /** Move assignment operator. @draft ICU 78 */
    U_FORCE_INLINE UnsafeUTFIterator &operator=(UnsafeUTFIterator &&src) noexcept = default;

    /** Copy constructor. @draft ICU 78 */
    U_FORCE_INLINE UnsafeUTFIterator(const UnsafeUTFIterator &other) = default;
    /** Copy assignment operator. @draft ICU 78 */
    U_FORCE_INLINE UnsafeUTFIterator &operator=(const UnsafeUTFIterator &other) = default;

    /**
     * @param other Another iterator
     * @return true if this operator is at the same position as the other one
     * @draft ICU 78
     */
    U_FORCE_INLINE bool operator==(const UnsafeUTFIterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    /**
     * @param other Another iterator
     * @return true if this operator is not at the same position as the other one
     * @draft ICU 78
     */
    U_FORCE_INLINE bool operator!=(const UnsafeUTFIterator &other) const { return !operator==(other); }

    /**
     * Decodes the code unit sequence at the current position.
     *
     * @return CodeUnits with the decoded code point etc.
     * @draft ICU 78
     */
    U_FORCE_INLINE UnsafeCodeUnits<CP32, UnitIter> operator*() const {
        if (state_ == 0) {
            UnitIter p0 = p_;
            units_ = Impl::readAndInc(p0, p_);
            state_ = 1;
        }
        return units_;
    }

    /**
     * Decodes the code unit sequence at the current position.
     * Used like <code>iter->codePoint()</code> or <code>iter->stringView()</code> etc.
     *
     * @return CodeUnits with the decoded code point etc., wrapped into
     *     an opaque proxy object so that <code>iter->codePoint()</code> etc. works.
     * @draft ICU 78
     */
    U_FORCE_INLINE Proxy operator->() const {
        if (state_ == 0) {
            UnitIter p0 = p_;
            units_ = Impl::readAndInc(p0, p_);
            state_ = 1;
        }
        return Proxy(units_);
    }

    /**
     * Pre-increment operator.
     *
     * @return this iterator
     * @draft ICU 78
     */
    U_FORCE_INLINE UnsafeUTFIterator &operator++() {  // pre-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::inc(p_);
        } else /* state_ < 0 */ {
            // operator--() called decAndRead() so we know how far to skip.
            p_ = units_.end();
            state_ = 0;
        }
        return *this;
    }

    /**
     * Post-increment operator.
     *
     * @return a copy of this iterator from before the increment.
     *     If UnitIter is a single-pass input_iterator, then this function
     *     returns an opaque proxy object so that <code>*iter++</code> still works.
     * @draft ICU 78
     */
    U_FORCE_INLINE UnsafeUTFIterator operator++(int) {  // post-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            UnsafeUTFIterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            UnitIter p0 = p_;
            units_ = Impl::readAndInc(p0, p_);
            UnsafeUTFIterator result(*this);
            result.state_ = 1;
            // keep this->state_ == 0
            return result;
        } else /* state_ < 0 */ {
            UnsafeUTFIterator result(*this);
            // operator--() called decAndRead() so we know how far to skip.
            p_ = units_.end();
            state_ = 0;
            return result;
        }
    }

    /**
     * Pre-decrement operator.
     * Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
     *
     * @return this iterator
     * @draft ICU 78
     */
    template<typename Iter = UnitIter>
    U_FORCE_INLINE
    std::enable_if_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<Iter>::iterator_category>,
        UnsafeUTFIterator &>
    operator--() {  // pre-decrement
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is ahead of the logical position.
            p_ = units_.begin();
        }
        units_ = Impl::decAndRead(p_);
        state_ = -1;
        return *this;
    }

    /**
     * Post-decrement operator.
     * Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
     *
     * @return a copy of this iterator from before the decrement.
     * @draft ICU 78
     */
    template<typename Iter = UnitIter>
    U_FORCE_INLINE
    std::enable_if_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<Iter>::iterator_category>,
        UnsafeUTFIterator>
    operator--(int) {  // post-decrement
        UnsafeUTFIterator result(*this);
        operator--();
        return result;
    }

private:
    friend class std::reverse_iterator<UnsafeUTFIterator<CP32, UnitIter>>;

    U_FORCE_INLINE UnitIter getLogicalPosition() const {
        return state_ <= 0 ? p_ : units_.begin();
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable UnsafeCodeUnits<CP32, UnitIter> units_;
    // >0: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start
    mutable int8_t state_ = 0;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
template<typename CP32, typename UnitIter>
class UnsafeUTFIterator<
        CP32,
        UnitIter,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Impl = UnsafeUTFImpl<CP32, UnitIter>;

    // Proxy type for post-increment return value, to make *iter++ work.
    // Also for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning UnsafeCodeUnits.
    class Proxy {
    public:
        explicit Proxy(UnsafeCodeUnits<CP32, UnitIter> &units) : units_(units) {}
        UnsafeCodeUnits<CP32, UnitIter> &operator*() { return units_; }
        UnsafeCodeUnits<CP32, UnitIter> *operator->() { return &units_; }
    private:
        UnsafeCodeUnits<CP32, UnitIter> units_;
    };

public:
    using value_type = UnsafeCodeUnits<CP32, UnitIter>;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::input_iterator_tag;

    U_FORCE_INLINE explicit UnsafeUTFIterator(UnitIter p) : p_(std::move(p)) {}

    U_FORCE_INLINE UnsafeUTFIterator(UnsafeUTFIterator &&src) noexcept = default;
    U_FORCE_INLINE UnsafeUTFIterator &operator=(UnsafeUTFIterator &&src) noexcept = default;

    U_FORCE_INLINE UnsafeUTFIterator(const UnsafeUTFIterator &other) = default;
    U_FORCE_INLINE UnsafeUTFIterator &operator=(const UnsafeUTFIterator &other) = default;

    U_FORCE_INLINE bool operator==(const UnsafeUTFIterator &other) const {
        return p_ == other.p_ && ahead_ == other.ahead_;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot advance, or do arithmetic with, a single-pass UnitIter.
    }
    U_FORCE_INLINE bool operator!=(const UnsafeUTFIterator &other) const { return !operator==(other); }

    U_FORCE_INLINE UnsafeCodeUnits<CP32, UnitIter> operator*() const {
        if (!ahead_) {
            units_ = Impl::readAndInc(p_, p_);
            ahead_ = true;
        }
        return units_;
    }

    U_FORCE_INLINE Proxy operator->() const {
        if (!ahead_) {
            units_ = Impl::readAndInc(p_, p_);
            ahead_ = true;
        }
        return Proxy(units_);
    }

    U_FORCE_INLINE UnsafeUTFIterator &operator++() {  // pre-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            Impl::inc(p_);
        }
        return *this;
    }

    U_FORCE_INLINE Proxy operator++(int) {  // post-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            units_ = Impl::readAndInc(p_, p_);
            // keep this->ahead_ == false
        }
        return Proxy(units_);
    }

private:
    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // so that we can use a single-pass input iterator for UnitIter.
    mutable UnsafeCodeUnits<CP32, UnitIter> units_ = {0, 0};
    // true: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    // false: initial state
    mutable bool ahead_ = false;
};
#endif  // U_IN_DOXYGEN

}  // namespace U_HEADER_ONLY_NAMESPACE

#ifndef U_IN_DOXYGEN
// Bespoke specialization of reverse_iterator.
// The default implementation implements reverse operator*() and ++ in a way
// that does most of the same work twice for reading variable-length sequences.
template<typename CP32, typename UnitIter>
class std::reverse_iterator<U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator<CP32, UnitIter>> {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using Impl = U_HEADER_ONLY_NAMESPACE::UnsafeUTFImpl<CP32, UnitIter>;
    using UnsafeCodeUnits_ = U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits<CP32, UnitIter>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning UnsafeCodeUnits.
    class Proxy {
    public:
        explicit Proxy(UnsafeCodeUnits_ units) : units_(units) {}
        UnsafeCodeUnits_ &operator*() { return units_; }
        UnsafeCodeUnits_ *operator->() { return &units_; }
    private:
        UnsafeCodeUnits_ units_;
    };

public:
    using value_type = UnsafeCodeUnits_;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::bidirectional_iterator_tag;

    U_FORCE_INLINE explicit reverse_iterator(U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator<CP32, UnitIter> iter) :
            p_(iter.getLogicalPosition()), units_(0, 0, p_, p_) {}
    U_FORCE_INLINE reverse_iterator() : p_{}, units_(0, 0, p_, p_) {}

    U_FORCE_INLINE reverse_iterator(reverse_iterator &&src) noexcept = default;
    U_FORCE_INLINE reverse_iterator &operator=(reverse_iterator &&src) noexcept = default;

    U_FORCE_INLINE reverse_iterator(const reverse_iterator &other) = default;
    U_FORCE_INLINE reverse_iterator &operator=(const reverse_iterator &other) = default;

    U_FORCE_INLINE bool operator==(const reverse_iterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    U_FORCE_INLINE bool operator!=(const reverse_iterator &other) const { return !operator==(other); }

    U_FORCE_INLINE UnsafeCodeUnits_ operator*() const {
        if (state_ == 0) {
            units_ = Impl::decAndRead(p_);
            state_ = -1;
        }
        return units_;
    }

    U_FORCE_INLINE Proxy operator->() const {
        if (state_ == 0) {
            units_ = Impl::decAndRead(p_);
            state_ = -1;
        }
        return Proxy(units_);
    }

    U_FORCE_INLINE reverse_iterator &operator++() {  // pre-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::dec(p_);
        } else /* state_ > 0 */ {
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.begin();
            state_ = 0;
        }
        return *this;
    }

    U_FORCE_INLINE reverse_iterator operator++(int) {  // post-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            reverse_iterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            units_ = Impl::decAndRead(p_);
            reverse_iterator result(*this);
            result.state_ = -1;
            // keep this->state_ == 0
            return result;
        } else /* state_ > 0 */ {
            reverse_iterator result(*this);
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.begin();
            state_ = 0;
            return result;
        }
    }

    U_FORCE_INLINE reverse_iterator &operator--() {  // pre-decrement
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is behind the logical position.
            p_ = units_.end();
        }
        UnitIter p0 = p_;
        units_ = Impl::readAndInc(p0, p_);
        state_ = 1;
        return *this;
    }

    U_FORCE_INLINE reverse_iterator operator--(int) {  // post-decrement
        reverse_iterator result(*this);
        operator--();
        return result;
    }

private:
    U_FORCE_INLINE UnitIter getLogicalPosition() const {
        return state_ >= 0 ? p_ : units_.end();
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // Keep state so that we call decAndRead() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable UnsafeCodeUnits_ units_;
    // >0: units_ = readAndInc(), p_ = units limit
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start
    //     which means that p_ is behind its logical position
    mutable int8_t state_ = 0;
};
#endif  // U_IN_DOXYGEN

namespace U_HEADER_ONLY_NAMESPACE {

/**
 * A C++ "range" for non-validating iteration over all of the code points of a Unicode string.
 * The string must be well-formed.
 *
 * Call unsafeUTFStringCodePoints() to have the compiler deduce the Unit type.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam Unit Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @draft ICU 78
 * @see unsafeUTFStringCodePoints
 */
template<typename CP32, typename Unit>
class UnsafeUTFStringCodePoints {
    static_assert(sizeof(CP32) == 4, "CP32 must be a 32-bit type to hold a code point");
    using UnitIter = typename std::basic_string_view<Unit>::iterator;
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    explicit UnsafeUTFStringCodePoints(std::basic_string_view<Unit> s) : s(s) {}

    /** Copy constructor. @draft ICU 78 */
    UnsafeUTFStringCodePoints(const UnsafeUTFStringCodePoints &other) = default;

    /** Copy assignment operator. @draft ICU 78 */
    UnsafeUTFStringCodePoints &operator=(const UnsafeUTFStringCodePoints &other) = default;

    /**
     * @return the range start iterator
     * @draft ICU 78
     */
    auto begin() const {
        return UnsafeUTFIterator<CP32, UnitIter>(s.begin());
    }

    /**
     * @return the range limit (exclusive end) iterator
     * @draft ICU 78
     */
    auto end() const {
        return UnsafeUTFIterator<CP32, UnitIter>(s.end());
    }

    /**
     * @return std::reverse_iterator(end())
     * @draft ICU 78
     */
    auto rbegin() const {
        return std::make_reverse_iterator(end());
    }

    /**
     * @return std::reverse_iterator(begin())
     * @draft ICU 78
     */
    auto rend() const {
        return std::make_reverse_iterator(begin());
    }

private:
    std::basic_string_view<Unit> s;
};

/**
 * UnsafeUTFIterator factory function.
 * Deduces the UnitIter template parameter from the input.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam UnitIter Can usually be omitted/deduced:
 *     An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @param iter code unit iterator
 * @return an UnsafeUTFIterator&lt;CP32, UnitIter&gt;
 *     for the given code unit iterator or character pointer
 * @draft ICU 78
 */
template<typename CP32, typename UnitIter>
auto unsafeUTFIterator(UnitIter iter) {
    return UnsafeUTFIterator<CP32, UnitIter>(std::move(iter));
}

/**
 * UnsafeUTFStringCodePoints factory function for a "range" of code points in a string.
 * The string must be well-formed.
 * Avoids having to explicitly specify the Unit template parameter
 * for the UnsafeUTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param s input string
 * @return an UnsafeUTFStringCodePoints&lt;CP32, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32>
auto unsafeUTFStringCodePoints(std::string_view s) {
    return UnsafeUTFStringCodePoints<CP32, decltype(s)::value_type>(s);
}

/**
 * UnsafeUTFStringCodePoints factory function for a "range" of code points in a string.
 * The string must be well-formed.
 * Avoids having to explicitly specify the Unit template parameter
 * for the UnsafeUTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param s input string
 * @return an UnsafeUTFStringCodePoints&lt;CP32, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32>
auto unsafeUTFStringCodePoints(std::u16string_view s) {
    return UnsafeUTFStringCodePoints<CP32, decltype(s)::value_type>(s);
}

/**
 * UnsafeUTFStringCodePoints factory function for a "range" of code points in a string.
 * The string must be well-formed.
 * Avoids having to explicitly specify the Unit template parameter
 * for the UnsafeUTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param s input string
 * @return an UnsafeUTFStringCodePoints&lt;CP32, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32>
auto unsafeUTFStringCodePoints(std::u32string_view s) {
    return UnsafeUTFStringCodePoints<CP32, decltype(s)::value_type>(s);
}

#if U_CPLUSPLUS_VERSION >= 20
// The new type char8_t is distinct from char. u8"literals" are now char8_t literals.
/**
 * UnsafeUTFStringCodePoints factory function for a "range" of code points in a string.
 * The string must be well-formed.
 * Avoids having to explicitly specify the Unit template parameter
 * for the UnsafeUTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param s input string
 * @return an UnsafeUTFStringCodePoints&lt;CP32, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32>
auto unsafeUTFStringCodePoints(std::u8string_view s) {
    return UnsafeUTFStringCodePoints<CP32, decltype(s)::value_type>(s);
}
#endif

/**
 * UnsafeUTFStringCodePoints factory function for a "range" of code points in a string.
 * The string must be well-formed.
 * Avoids having to explicitly specify the Unit template parameter
 * for the UnsafeUTFStringCodePoints.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param s input string
 * @return an UnsafeUTFStringCodePoints&lt;CP32, Unit&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;
 * @draft ICU 78
 */
template<typename CP32>
auto unsafeUTFStringCodePoints(std::wstring_view s) {
    return UnsafeUTFStringCodePoints<CP32, decltype(s)::value_type>(s);
}

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTFITERATOR_H__
