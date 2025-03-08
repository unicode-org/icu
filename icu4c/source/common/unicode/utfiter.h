// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfiter.h
// created: 2024aug12 Markus W. Scherer
// TODO: rename this header file to utfiterator.h?

#ifndef __UTFITERATOR_H__
#define __UTFITERATOR_H__

// TODO: For experimentation outside of ICU, comment out this include.
// Experimentally conditional code below checks for UTYPES_H and
// otherwise uses copies of bits of ICU.
#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API || !defined(UTYPES_H)

#include <iterator>
#include <string_view>
#include <type_traits>
#ifdef UTYPES_H
#include "unicode/utf16.h"
#include "unicode/utf8.h"
#include "unicode/uversion.h"
#else
// TODO: Remove checks for UTYPES_H and replacement definitions.
// unicode/utypes.h etc.
#include <inttypes.h>
typedef int32_t UChar32;
constexpr UChar32 U_SENTINEL = -1;
// unicode/uversion.h
#define U_HEADER_ONLY_NAMESPACE header
namespace header {}
// unicode/utf.h
#define U_IS_SURROGATE(c) (((c)&0xfffff800)==0xd800)
// unicode/utf16.h
#define U16_IS_LEAD(c) (((c)&0xfffffc00)==0xd800)
#define U16_IS_TRAIL(c) (((c)&0xfffffc00)==0xdc00)
#define U16_IS_SURROGATE(c) U_IS_SURROGATE(c)
#define U16_IS_SURROGATE_LEAD(c) (((c)&0x400)==0)
#define U16_IS_SURROGATE_TRAIL(c) (((c)&0x400)!=0)
#define U16_SURROGATE_OFFSET ((0xd800<<10UL)+0xdc00-0x10000)
#define U16_GET_SUPPLEMENTARY(lead, trail) \
    (((UChar32)(lead)<<10UL)+(UChar32)(trail)-U16_SURROGATE_OFFSET)
// unicode/utf8.h
#define U8_IS_SINGLE(c) (((c)&0x80)==0)
#define U8_IS_LEAD(c) ((uint8_t)((c)-0xc2)<=0x32)
#define U8_IS_TRAIL(c) ((int8_t)(c)<-0x40)
#define U8_LEAD3_T1_BITS "\x20\x30\x30\x30\x30\x30\x30\x30\x30\x30\x30\x30\x30\x10\x30\x30"
#define U8_IS_VALID_LEAD3_AND_T1(lead, t1) (U8_LEAD3_T1_BITS[(lead)&0xf]&(1<<((uint8_t)(t1)>>5)))
#define U8_LEAD4_T1_BITS "\x00\x00\x00\x00\x00\x00\x00\x00\x1E\x0F\x0F\x0F\x00\x00\x00\x00"
#define U8_IS_VALID_LEAD4_AND_T1(lead, t1) (U8_LEAD4_T1_BITS[(uint8_t)(t1)>>4]&(1<<((lead)&7)))
#define U8_NEXT(s, i, length, c) U8_INTERNAL_NEXT_OR_SUB(s, i, length, c, U_SENTINEL)
#define U8_NEXT_OR_FFFD(s, i, length, c) U8_INTERNAL_NEXT_OR_SUB(s, i, length, c, 0xfffd)
#define U8_INTERNAL_NEXT_OR_SUB(s, i, length, c, sub) { \
    (c)=(uint8_t)(s)[(i)++]; \
    if(!U8_IS_SINGLE(c)) { \
        uint8_t __t = 0; \
        if((i)!=(length) && \
            /* fetch/validate/assemble all but last trail byte */ \
            ((c)>=0xe0 ? \
                ((c)<0xf0 ?  /* U+0800..U+FFFF except surrogates */ \
                    U8_LEAD3_T1_BITS[(c)&=0xf]&(1<<((__t=(s)[i])>>5)) && \
                    (__t&=0x3f, 1) \
                :  /* U+10000..U+10FFFF */ \
                    ((c)-=0xf0)<=4 && \
                    U8_LEAD4_T1_BITS[(__t=(s)[i])>>4]&(1<<(c)) && \
                    ((c)=((c)<<6)|(__t&0x3f), ++(i)!=(length)) && \
                    (__t=(s)[i]-0x80)<=0x3f) && \
                /* valid second-to-last trail byte */ \
                ((c)=((c)<<6)|__t, ++(i)!=(length)) \
            :  /* U+0080..U+07FF */ \
                (c)>=0xc2 && ((c)&=0x1f, 1)) && \
            /* last trail byte */ \
            (__t=(s)[i]-0x80)<=0x3f && \
            ((c)=((c)<<6)|__t, ++(i), 1)) { \
        } else { \
            (c)=(sub);  /* ill-formed*/ \
        } \
    } \
}
#define U8_COUNT_TRAIL_BYTES_UNSAFE(leadByte) \
    (((uint8_t)(leadByte)>=0xc2)+((uint8_t)(leadByte)>=0xe0)+((uint8_t)(leadByte)>=0xf0))
#define U8_MASK_LEAD_BYTE(leadByte, countTrailBytes) ((leadByte)&=(1<<(6-(countTrailBytes)))-1)
#endif

/**
 * \file
 * \brief C++ header-only API: C++ iterators over Unicode strings (=UTF-8/16/32 if well-formed).
 */

#ifndef U_HIDE_DRAFT_API

// Some defined behaviors for handling ill-formed Unicode strings.
typedef enum UIllFormedBehavior {
    // Returns a negative value instead of a code point.
    U_BEHAVIOR_NEGATIVE,
    // Returns U+FFFD Replacement Character.
    U_BEHAVIOR_FFFD,
    // UTF-8: Not allowed;
    // UTF-16: returns the unpaired surrogate;
    // UTF-32: returns the surrogate code point, or U+FFFD if out of range.
    U_BEHAVIOR_SURROGATE
} UIllFormedBehavior;

namespace U_HEADER_ONLY_NAMESPACE {

/**
 * Result of decoding a minimal Unicode code unit sequence.
 * Returned from non-validating Unicode string code point iterators.
 * Base class for class CodeUnits which is returned from validating iterators.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @see UnsafeUTFIterator
 * @see UnsafeUTFStringCodePoints
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, typename = void>
class UnsafeCodeUnits {
    using Unit = typename std::iterator_traits<UnitIter>::value_type;
public:
    // @internal
    UnsafeCodeUnits(CP32 codePoint, uint8_t length, UnitIter data) :
            c(codePoint), len(length), p(data) {}

    UnsafeCodeUnits(const UnsafeCodeUnits &other) = default;
    UnsafeCodeUnits &operator=(const UnsafeCodeUnits &other) = default;

    /**
     * @return the Unicode code point decoded from the code unit sequence.
     *     If the sequence is ill-formed and the iterator validates,
     *     then this is a replacement value according to the iterator‘s
     *     UIllFormedBehavior template parameter.
     * @draft ICU 78
     */
    UChar32 codePoint() const { return c; }

    /**
     * @return the start of the minimal Unicode code unit sequence.
     * Not enabled if UnitIter is a single-pass input_iterator.
     * @draft ICU 78
     */
    UnitIter data() const { return p; }

    /**
     * @return the length of the minimal Unicode code unit sequence.
     * @draft ICU 78
     */
    uint8_t length() const { return len; }

    /**
     * @return a string_view of the minimal Unicode code unit sequence.
     * Enabled only if UnitIter is a pointer.
     * @draft ICU 78
     */
    template<typename Iter = UnitIter>
    std::enable_if_t<
        std::is_pointer_v<Iter>,
        std::basic_string_view<Unit>>
    stringView() const {
        return std::basic_string_view<Unit>(p, len);
    }

private:
    // Order of fields with padding and access frequency in mind.
    CP32 c;
    uint8_t len;
    UnitIter p;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
// No UnitIter field, no getter for it, no stringView().
template<typename UnitIter, typename CP32>
class UnsafeCodeUnits<
        UnitIter,
        CP32,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
public:
    // @internal
    UnsafeCodeUnits(CP32 codePoint, uint8_t length) : c(codePoint), len(length) {}

    UnsafeCodeUnits(const UnsafeCodeUnits &other) = default;
    UnsafeCodeUnits &operator=(const UnsafeCodeUnits &other) = default;

    UChar32 codePoint() const { return c; }

    uint8_t length() const { return len; }

private:
    // Order of fields with padding and access frequency in mind.
    CP32 c;
    uint8_t len;
};
#endif  // U_IN_DOXYGEN

/**
 * Result of validating and decoding a minimal Unicode code unit sequence.
 * Returned from validating Unicode string code point iterators.
 * Adds function wellFormed() to base class UnsafeCodeUnits.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @see UTFIterator
 * @see UTFStringCodePoints
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, typename = void>
class CodeUnits : public UnsafeCodeUnits<UnitIter, CP32> {
public:
    // @internal
    CodeUnits(CP32 codePoint, uint8_t length, bool wellFormed, UnitIter data) :
            UnsafeCodeUnits<UnitIter, CP32>(codePoint, length, data), ok(wellFormed) {}

    CodeUnits(const CodeUnits &other) = default;
    CodeUnits &operator=(const CodeUnits &other) = default;

    bool wellFormed() const { return ok; }

private:
    bool ok;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
// No UnitIter field, no getter for it, no stringView().
template<typename UnitIter, typename CP32>
class CodeUnits<
        UnitIter,
        CP32,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> :
            public UnsafeCodeUnits<UnitIter, CP32> {
public:
    // @internal
    CodeUnits(CP32 codePoint, uint8_t length, bool wellFormed) :
            UnsafeCodeUnits<UnitIter, CP32>(codePoint, length), ok(wellFormed) {}

    CodeUnits(const CodeUnits &other) = default;
    CodeUnits &operator=(const CodeUnits &other) = default;

    bool wellFormed() const { return ok; }

private:
    bool ok;
};
#endif  // U_IN_DOXYGEN

// Validating implementations --------------------------------------------- ***

#ifndef U_IN_DOXYGEN
// @internal
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior, typename = void>
class UTFImpl;

// UTF-8
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class UTFImpl<
        UnitIter,
        CP32,
        behavior,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 1>> {
    static_assert(behavior != U_BEHAVIOR_SURROGATE,
                  "For 8-bit strings, the SURROGATE option does not have an equivalent.");
public:
    // Handle ill-formed UTF-8
    static inline CP32 sub() {
        switch (behavior) {
            case U_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case U_BEHAVIOR_FFFD: return 0xfffd;
        }
    }

    static inline void inc(UnitIter &p, const UnitIter &limit) {
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

    static inline void dec(UnitIter start, UnitIter &p) {
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

    static inline CodeUnits<UnitIter, CP32> readAndInc(UnitIter &p, UnitIter limit) {
        // Very similar to U8_NEXT_OR_FFFD().
        UnitIter p0 = p;
        CP32 c = uint8_t(*p);
        ++p;
        if (U8_IS_SINGLE(c)) {
            return {c, 1, true, p0};
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
            return {c, length, true, p0};
        }
        return {sub(), length, false, p0};
    }

    static inline CodeUnits<UnitIter, CP32> singlePassReadAndInc(
            UnitIter &p, const UnitIter &limit) {
        // Very similar to U8_NEXT_OR_FFFD().
        CP32 c = uint8_t(*p);
        ++p;
        if (U8_IS_SINGLE(c)) {
            return {c, 1, true};
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
            return {c, length, true};
        }
        return {sub(), length, false};
    }

    static inline CodeUnits<UnitIter, CP32> decAndRead(UnitIter start, UnitIter &p) {
        // Very similar to U8_PREV_OR_FFFD().
        CP32 c = uint8_t(*--p);
        if (U8_IS_SINGLE(c)) {
            return {c, 1, true, p};
        }
        if (U8_IS_TRAIL(c) && p != start) {
            UnitIter p1 = p;
            uint8_t b1 = *--p1;
            if (U8_IS_LEAD(b1)) {
                if (b1 < 0xe0) {
                    p = p1;
                    c = ((b1 - 0xc0) << 6) | (c & 0x3f);
                    return {c, 2, true, p};
                } else if (b1 < 0xf0 ?
                            U8_IS_VALID_LEAD3_AND_T1(b1, c) :
                            U8_IS_VALID_LEAD4_AND_T1(b1, c)) {
                    // Truncated 3- or 4-byte sequence.
                    p = p1;
                    return {sub(), 2, false, p};
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
                            return {c, 3, true, p};
                        }
                    } else if (U8_IS_VALID_LEAD4_AND_T1(b2, b1)) {
                        // Truncated 4-byte sequence.
                        p = p1;
                        return {sub(), 3, false, p};
                    }
                } else if (U8_IS_TRAIL(b2) && p1 != start) {
                    uint8_t b3 = *--p1;
                    if (0xf0 <= b3 && b3 <= 0xf4) {
                        b3 &= 7;
                        if (U8_IS_VALID_LEAD4_AND_T1(b3, b2)) {
                            p = p1;
                            c = (b3 << 18) | ((b2 & 0x3f) << 12) | ((b1 & 0x3f) << 6) | c;
                            return {c, 4, true, p};
                        }
                    }
                }
            }
        }
        return {sub(), 1, false, p};
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, uint8_t n) {
        std::advance(p, n);
    }
};

// UTF-16
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class UTFImpl<
        UnitIter,
        CP32,
        behavior,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 2>> {
public:
    // Handle ill-formed UTF-16: One unpaired surrogate.
    static inline CP32 sub(CP32 surrogate) {
        switch (behavior) {
            case U_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case U_BEHAVIOR_FFFD: return 0xfffd;
            case U_BEHAVIOR_SURROGATE: return surrogate;
        }
    }

    static inline void inc(UnitIter &p, const UnitIter &limit) {
        // Very similar to U16_FWD_1().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
        }
    }

    static inline void dec(UnitIter start, UnitIter &p) {
        // Very similar to U16_BACK_1().
        UnitIter p1;
        if (U16_IS_TRAIL(*--p) && p != start && (p1 = p, U16_IS_LEAD(*--p1))) {
            p = p1;
        }
    }

    static inline CodeUnits<UnitIter, CP32> readAndInc(UnitIter &p, UnitIter limit) {
        // Very similar to U16_NEXT_OR_FFFD().
        UnitIter p0 = p;
        CP32 c = *p;
        ++p;
        if (!U16_IS_SURROGATE(c)) {
            return {c, 1, true, p0};
        } else {
            uint16_t c2;
            if (U16_IS_SURROGATE_LEAD(c) && p != limit && U16_IS_TRAIL(c2 = *p)) {
                ++p;
                c = U16_GET_SUPPLEMENTARY(c, c2);
                return {c, 2, true, p0};
            } else {
                return {sub(c), 1, false, p0};
            }
        }
    }

    static inline CodeUnits<UnitIter, CP32> singlePassReadAndInc(
            UnitIter &p, const UnitIter &limit) {
        // Very similar to U16_NEXT_OR_FFFD().
        CP32 c = *p;
        ++p;
        if (!U16_IS_SURROGATE(c)) {
            return {c, 1, true};
        } else {
            uint16_t c2;
            if (U16_IS_SURROGATE_LEAD(c) && p != limit && U16_IS_TRAIL(c2 = *p)) {
                ++p;
                c = U16_GET_SUPPLEMENTARY(c, c2);
                return {c, 2, true};
            } else {
                return {sub(c), 1, false};
            }
        }
    }

    static inline CodeUnits<UnitIter, CP32> decAndRead(UnitIter start, UnitIter &p) {
        // Very similar to U16_PREV_OR_FFFD().
        CP32 c = *--p;
        if (!U16_IS_SURROGATE(c)) {
            return {c, 1, true, p};
        } else {
            UnitIter p1;
            uint16_t c2;
            if (U16_IS_SURROGATE_TRAIL(c) && p != start && (p1 = p, U16_IS_LEAD(c2 = *--p1))) {
                p = p1;
                c = U16_GET_SUPPLEMENTARY(c2, c);
                return {c, 2, true, p};
            } else {
                return {sub(c), 1, false, p};
            }
        }
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, uint8_t n) {
        // n = 1 or 2 for UTF-16
        ++p;
        if (n == 2) {
            ++p;
        }
    }
};

// UTF-32: trivial, but still validating
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class UTFImpl<
        UnitIter,
        CP32,
        behavior,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 4>> {
public:
    // Handle ill-formed UTF-32
    static inline CP32 sub(bool forSurrogate, CP32 surrogate) {
        switch (behavior) {
            case U_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case U_BEHAVIOR_FFFD: return 0xfffd;
            case U_BEHAVIOR_SURROGATE: return forSurrogate ? surrogate : 0xfffd;
        }
    }

    static inline void inc(UnitIter &p, const UnitIter &/*limit*/) {
        ++p;
    }

    static inline void dec(UnitIter /*start*/, UnitIter &p) {
        --p;
    }

    static inline CodeUnits<UnitIter, CP32> readAndInc(UnitIter &p, UnitIter /*limit*/) {
        UnitIter p0 = p;
        uint32_t uc = *p;
        CP32 c = uc;
        ++p;
        if (uc < 0xd800 || (0xe000 <= uc && uc <= 0x10ffff)) {
            return {c, 1, true, p0};
        } else {
            return {sub(uc < 0xe000, c), 1, false, p0};
        }
    }

    static inline CodeUnits<UnitIter, CP32> singlePassReadAndInc(
            UnitIter &p, const UnitIter &/*limit*/) {
        uint32_t uc = *p;
        CP32 c = uc;
        ++p;
        if (uc < 0xd800 || (0xe000 <= uc && uc <= 0x10ffff)) {
            return {c, 1, true};
        } else {
            return {sub(uc < 0xe000, c), 1, false};
        }
    }

    static inline CodeUnits<UnitIter, CP32> decAndRead(UnitIter /*start*/, UnitIter &p) {
        uint32_t uc = *--p;
        CP32 c = uc;
        if (uc < 0xd800 || (0xe000 <= uc && uc <= 0x10ffff)) {
            return {c, 1, true, p};
        } else {
            return {sub(uc < 0xe000, c), 1, false, p};
        }
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, uint8_t /*n*/) {
        ++p;
    }
};

// Non-validating implementations ----------------------------------------- ***

// @internal
template<typename UnitIter, typename CP32, typename = void>
class UnsafeUTFImpl;

// UTF-8
template<typename UnitIter, typename CP32>
class UnsafeUTFImpl<
        UnitIter,
        CP32,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 1>> {
public:
    static inline void inc(UnitIter &p) {
        // Very similar to U8_FWD_1_UNSAFE().
        uint8_t b = *p;
        std::advance(p, 1 + U8_COUNT_TRAIL_BYTES_UNSAFE(b));
    }

    static inline void dec(UnitIter &p) {
        // Very similar to U8_BACK_1_UNSAFE().
        while (U8_IS_TRAIL(*--p)) {}
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> readAndInc(UnitIter &p) {
        // Very similar to U8_NEXT_UNSAFE().
        UnitIter p0 = p;
        CP32 c = uint8_t(*p);
        ++p;
        if (U8_IS_SINGLE(c)) {
            return {c, 1, p0};
        } else if (c < 0xe0) {
            c = ((c & 0x1f) << 6) | (*p & 0x3f);
            ++p;
            return {c, 2, p0};
        } else if (c < 0xf0) {
            // No need for (c&0xf) because the upper bits are truncated
            // after <<12 in the cast to uint16_t.
            c = uint16_t(c << 12) | ((*p & 0x3f) << 6);
            ++p;
            c |= *p & 0x3f;
            ++p;
            return {c, 3, p0};
        } else {
            c = ((c & 7) << 18) | ((*p & 0x3f) << 12);
            ++p;
            c |= (*p & 0x3f) << 6;
            ++p;
            c |= *p & 0x3f;
            ++p;
            return {c, 4, p0};
        }
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> singlePassReadAndInc(UnitIter &p) {
        // Very similar to U8_NEXT_UNSAFE().
        CP32 c = uint8_t(*p);
        ++p;
        if (U8_IS_SINGLE(c)) {
            return {c, 1};
        } else if (c < 0xe0) {
            c = ((c & 0x1f) << 6) | (*p & 0x3f);
            ++p;
            return {c, 2};
        } else if (c < 0xf0) {
            // No need for (c&0xf) because the upper bits are truncated
            // after <<12 in the cast to uint16_t.
            c = uint16_t(c << 12) | ((*p & 0x3f) << 6);
            ++p;
            c |= *p & 0x3f;
            ++p;
            return {c, 3};
        } else {
            c = ((c & 7) << 18) | ((*p & 0x3f) << 12);
            ++p;
            c |= (*p & 0x3f) << 6;
            ++p;
            c |= *p & 0x3f;
            ++p;
            return {c, 4};
        }
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> decAndRead(UnitIter &p) {
        // Very similar to U8_PREV_UNSAFE().
        CP32 c = uint8_t(*--p);
        if (U8_IS_SINGLE(c)) {
            return {c, 1, p};
        }
        // U8_IS_TRAIL(c) if well-formed
        c &= 0x3f;
        uint8_t count = 1;
        for (uint8_t shift = 6;;) {
            uint8_t b = *--p;
            if (b >= 0xc0) {
                U8_MASK_LEAD_BYTE(b, count);
                c |= uint32_t(b) << shift;
                break;
            } else {
                c |= uint32_t(b & 0x3f) << shift;
                ++count;
                shift += 6;
            }
        }
        ++count;
        return {c, count, p};
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, uint8_t n) {
        std::advance(p, n);
    }
};

// UTF-16
template<typename UnitIter, typename CP32>
class UnsafeUTFImpl<
        UnitIter,
        CP32,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 2>> {
public:
    static inline void inc(UnitIter &p) {
        // Very similar to U16_FWD_1_UNSAFE().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c)) {
            ++p;
        }
    }

    static inline void dec(UnitIter &p) {
        // Very similar to U16_BACK_1_UNSAFE().
        if (U16_IS_TRAIL(*--p)) {
            --p;
        }
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> readAndInc(UnitIter &p) {
        // Very similar to U16_NEXT_UNSAFE().
        UnitIter p0 = p;
        CP32 c = *p;
        ++p;
        if (!U16_IS_LEAD(c)) {
            return {c, 1, p0};
        } else {
            uint16_t c2 = *p;
            ++p;
            c = U16_GET_SUPPLEMENTARY(c, c2);
            return {c, 2, p0};
        }
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> singlePassReadAndInc(UnitIter &p) {
        // Very similar to U16_NEXT_UNSAFE().
        CP32 c = *p;
        ++p;
        if (!U16_IS_LEAD(c)) {
            return {c, 1};
        } else {
            uint16_t c2 = *p;
            ++p;
            c = U16_GET_SUPPLEMENTARY(c, c2);
            return {c, 2};
        }
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> decAndRead(UnitIter &p) {
        // Very similar to U16_PREV_UNSAFE().
        CP32 c = *--p;
        if (!U16_IS_TRAIL(c)) {
            return {c, 1, p};
        } else {
            uint16_t c2 = *--p;
            c = U16_GET_SUPPLEMENTARY(c2, c);
            return {c, 2, p};
        }
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, uint8_t n) {
        // n = 1 or 2 for UTF-16
        ++p;
        if (n == 2) {
            ++p;
        }
    }
};

// UTF-32: trivial
template<typename UnitIter, typename CP32>
class UnsafeUTFImpl<
        UnitIter,
        CP32,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 4>> {
public:
    static inline void inc(UnitIter &p) {
        ++p;
    }

    static inline void dec(UnitIter &p) {
        --p;
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> readAndInc(UnitIter &p) {
        UnitIter p0 = p;
        CP32 c = *p;
        ++p;
        return {c, 1, p0};
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> singlePassReadAndInc(UnitIter &p) {
        CP32 c = *p;
        ++p;
        return {c, 1};
    }

    static inline UnsafeCodeUnits<UnitIter, CP32> decAndRead(UnitIter &p) {
        CP32 c = *--p;
        return {c, 1, p};
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, uint8_t /*n*/) {
        ++p;
    }
};

#endif

/**
 * Validating iterator over the code points in a Unicode string.
 *
 * The UnitIter can be
 * an input_iterator, a forward_iterator, or a bidirectional_iterator (including a pointer).
 * The UTFIterator will have the corresponding iterator_category.
 *
 * For reverse iteration, either use this iterator directly as in <code>*--iter</code>
 * or wrap it using std::make_reverse_iterator(iter).
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior, typename = void>
class UTFIterator {
    using Impl = UTFImpl<UnitIter, CP32, behavior>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        Proxy(CodeUnits<UnitIter, CP32> &units) : units_(units) {}
        CodeUnits<UnitIter, CP32> &operator*() { return units_; }
        CodeUnits<UnitIter, CP32> *operator->() { return &units_; }
    private:
        CodeUnits<UnitIter, CP32> units_;
    };

public:
    /** C++ iterator boilerplate @internal */
    using value_type = CodeUnits<UnitIter, CP32>;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::conditional_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<UnitIter>::iterator_category>,
        std::bidirectional_iterator_tag,
        std::forward_iterator_tag>;

    // Constructor with start <= p < limit.
    // All of these iterators/pointers should be at code point boundaries.
    inline UTFIterator(UnitIter start, UnitIter p, UnitIter limit) :
            p_(p), start_(start), limit_(limit), units_(0, 0, false, p) {}
    // Constructs an iterator with start=p.
    inline UTFIterator(UnitIter p, UnitIter limit) :
            p_(p), start_(p), limit_(limit), units_(0, 0, false, p) {}
    // Constructs an iterator start or limit sentinel.
    inline UTFIterator(UnitIter p) : p_(p), start_(p), limit_(p), units_(0, 0, false, p) {}

    inline UTFIterator(UTFIterator &&src) noexcept = default;
    inline UTFIterator &operator=(UTFIterator &&src) noexcept = default;

    inline UTFIterator(const UTFIterator &other) = default;
    inline UTFIterator &operator=(const UTFIterator &other) = default;

    inline bool operator==(const UTFIterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    inline bool operator!=(const UTFIterator &other) const { return !operator==(other); }

    inline CodeUnits<UnitIter, CP32> operator*() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            state_ = 1;
        }
        return units_;
    }

    /**
     * @return the current decoded subsequence via an opaque proxy object
     * so that <code>iter->codePoint()</code> etc. works.
     * @draft ICU 78
     */
    inline Proxy operator->() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            state_ = 1;
        }
        return Proxy(units_);
    }

    inline UTFIterator &operator++() {  // pre-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::inc(p_, limit_);
        } else /* state_ < 0 */ {
            // operator--() called decAndRead() so we know how far to skip.
            Impl::moveToDecAndReadLimit(p_, units_.length());
            state_ = 0;
        }
        return *this;
    }

    /**
     * @return a copy of this iterator from before the increment.
     *     If UnitIter is a single-pass input_iterator, then this function
     *     returns an opaque proxy object so that <code>*iter++</code> still works.
     * @draft ICU 78
     */
    inline UTFIterator operator++(int) {  // post-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            UTFIterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            UTFIterator result(*this);
            result.state_ = 1;
            // keep this->state_ == 0
            return result;
        } else /* state_ < 0 */ {
            UTFIterator result(*this);
            // operator--() called decAndRead() so we know how far to skip.
            Impl::moveToDecAndReadLimit(p_, units_.length());
            state_ = 0;
            return result;
        }
    }

    // Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
    template<typename Iter = UnitIter>
    inline
    std::enable_if_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<Iter>::iterator_category>,
        UTFIterator &>
    operator--() {  // pre-decrement
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is ahead of the logical position.
            p_ = units_.data();
        }
        units_ = Impl::decAndRead(start_, p_);
        state_ = -1;
        return *this;
    }

    // Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
    template<typename Iter = UnitIter>
    inline
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
    friend class std::reverse_iterator<UTFIterator<UnitIter, CP32, behavior>>;

    inline UnitIter getLogicalPosition() const {
        return state_ <= 0 ? p_ : units_.data();
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need start_ & limit_ so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    const UnitIter start_;
    const UnitIter limit_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable CodeUnits<UnitIter, CP32> units_;
    // >0: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start
    mutable int8_t state_ = 0;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class UTFIterator<
        UnitIter,
        CP32,
        behavior,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
    using Impl = UTFImpl<UnitIter, CP32, behavior>;

    // Proxy type for post-increment return value, to make *iter++ work.
    // Also for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        Proxy(CodeUnits<UnitIter, CP32> &units) : units_(units) {}
        CodeUnits<UnitIter, CP32> &operator*() { return units_; }
        CodeUnits<UnitIter, CP32> *operator->() { return &units_; }
    private:
        CodeUnits<UnitIter, CP32> units_;
    };

public:
    using value_type = CodeUnits<UnitIter, CP32>;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::input_iterator_tag;

    inline UTFIterator(UnitIter p, UnitIter limit) : p_(std::move(p)), limit_(std::move(limit)) {}

    // Constructs an iterator start or limit sentinel.
    // Requires p to be copyable.
    inline UTFIterator(UnitIter p) : p_(std::move(p)), limit_(p_) {}

    inline UTFIterator(UTFIterator &&src) noexcept = default;
    inline UTFIterator &operator=(UTFIterator &&src) noexcept = default;

    inline UTFIterator(const UTFIterator &other) = default;
    inline UTFIterator &operator=(const UTFIterator &other) = default;

    inline bool operator==(const UTFIterator &other) const {
        return p_ == other.p_ && ahead_ == other.ahead_;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot advance, or do arithmetic with, a single-pass UnitIter.
    }
    inline bool operator!=(const UTFIterator &other) const { return !operator==(other); }

    inline CodeUnits<UnitIter, CP32> operator*() const {
        if (!ahead_) {
            units_ = Impl::singlePassReadAndInc(p_, limit_);
            ahead_ = true;
        }
        return units_;
    }

    inline Proxy operator->() const {
        if (!ahead_) {
            units_ = Impl::singlePassReadAndInc(p_, limit_);
            ahead_ = true;
        }
        return Proxy(units_);
    }

    inline UTFIterator &operator++() {  // pre-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            Impl::inc(p_, limit_);
        }
        return *this;
    }

    inline Proxy operator++(int) {  // post-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            units_ = Impl::singlePassReadAndInc(p_, limit_);
            // keep this->ahead_ == false
        }
        return Proxy(units_);
    }

private:
    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need limit_ so that when we read a code point
    // we can test if there are enough code units.
    const UnitIter limit_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // so that we can use a single-pass input iterator for UnitIter.
    mutable CodeUnits<UnitIter, CP32> units_ = {0, 0, false};
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
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class std::reverse_iterator<U_HEADER_ONLY_NAMESPACE::UTFIterator<UnitIter, CP32, behavior>> {
    using Impl = U_HEADER_ONLY_NAMESPACE::UTFImpl<UnitIter, CP32, behavior>;
    using CodeUnits_ = U_HEADER_ONLY_NAMESPACE::CodeUnits<UnitIter, CP32>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        Proxy(CodeUnits_ units) : units_(units) {}
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

    inline reverse_iterator(U_HEADER_ONLY_NAMESPACE::UTFIterator<UnitIter, CP32, behavior> iter) :
            p_(iter.getLogicalPosition()), start_(iter.start_), limit_(iter.limit_),
            units_(0, 0, false, p_), unitsLimit_(p_) {}

    inline reverse_iterator(reverse_iterator &&src) noexcept = default;
    inline reverse_iterator &operator=(reverse_iterator &&src) noexcept = default;

    inline reverse_iterator(const reverse_iterator &other) = default;
    inline reverse_iterator &operator=(const reverse_iterator &other) = default;

    inline bool operator==(const reverse_iterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    inline bool operator!=(const reverse_iterator &other) const { return !operator==(other); }

    inline CodeUnits_ operator*() const {
        if (state_ == 0) {
            unitsLimit_ = p_;
            units_ = Impl::decAndRead(start_, p_);
            state_ = -1;
        }
        return units_;
    }

    inline Proxy operator->() const {
        if (state_ == 0) {
            unitsLimit_ = p_;
            units_ = Impl::decAndRead(start_, p_);
            state_ = -1;
        }
        return Proxy(units_);
    }

    inline reverse_iterator &operator++() {  // pre-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::dec(start_, p_);
        } else /* state_ > 0 */ {
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.data();
            state_ = 0;
        }
        return *this;
    }

    inline reverse_iterator operator++(int) {  // post-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            reverse_iterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            unitsLimit_ = p_;
            units_ = Impl::decAndRead(start_, p_);
            reverse_iterator result(*this);
            result.state_ = -1;
            // keep this->state_ == 0
            return result;
        } else /* state_ > 0 */ {
            reverse_iterator result(*this);
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.data();
            state_ = 0;
            return result;
        }
    }

    inline reverse_iterator &operator--() {  // pre-decrement
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is behind the logical position.
            p_ = unitsLimit_;
        }
        units_ = Impl::readAndInc(start_, p_);
        state_ = 1;
        return *this;
    }

    inline reverse_iterator operator--(int) {  // post-decrement
        reverse_iterator result(*this);
        operator--();
        return result;
    }

private:
    inline UnitIter getLogicalPosition() const {
        return state_ >= 0 ? p_ : unitsLimit_;
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need start_ & limit_ so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    const UnitIter start_;
    const UnitIter limit_;
    // Keep state so that we call decAndRead() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable CodeUnits_ units_;
    // For fast getLogicalPosition() and operator==().
    mutable UnitIter unitsLimit_;
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
 * @tparam Unit Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @draft ICU 78
 */
template<typename Unit, typename CP32, UIllFormedBehavior behavior>
class UTFStringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    UTFStringCodePoints(std::basic_string_view<Unit> s) : s(s) {}

    /** @draft ICU 78 */
    UTFStringCodePoints(const UTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UTFStringCodePoints &operator=(const UTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UTFIterator<const Unit *, CP32, behavior> begin() const {
        return {s.data(), s.data(), s.data() + s.length()};
    }

    /** @draft ICU 78 */
    UTFIterator<const Unit *, CP32, behavior> end() const {
        const Unit *limit = s.data() + s.length();
        return {s.data(), limit, limit};
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
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param start start code unit iterator
 * @param p current-position code unit iterator
 * @param limit limit (exclusive-end) code unit iterator
 * @return a UTFIterator&lt;UnitIter, CP32, behavior&gt;
 *     for the given code unit iterators or character pointers
 * @draft ICU 78
 */
template<typename CP32, UIllFormedBehavior behavior, typename UnitIter>
auto utfIterator(UnitIter start, UnitIter p, UnitIter limit) {
    return UTFIterator<UnitIter, CP32, behavior>(std::move(start), std::move(p), std::move(limit));
}

/**
 * UTFIterator factory function for start = p < limit.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param p start and current-position code unit iterator
 * @param limit limit (exclusive-end) code unit iterator
 * @return a UTFIterator&lt;UnitIter, CP32, behavior&gt;
 *     for the given code unit iterators or character pointers
 * @draft ICU 78
 */
template<typename CP32, UIllFormedBehavior behavior, typename UnitIter>
auto utfIterator(UnitIter p, UnitIter limit) {
    return UTFIterator<UnitIter, CP32, behavior>(std::move(p), std::move(limit));
}

// Note: We should only enable the following factory function for a copyable UnitIter.
// In C++17, we would have to partially specialize with enable_if_t testing for forward_iterator,
// but a function template partial specialization is not allowed.
// In C++20, we might be able to require the std::copyable concept.

/**
 * UTFIterator factory function for a start or limit sentinel.
 * Requires UnitIter to be copyable.
 *
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param p code unit iterator
 * @return a UTFIterator&lt;UnitIter, CP32, behavior&gt;
 *     for the given code unit iterator or character pointer
 * @draft ICU 78
 */
template<typename CP32, UIllFormedBehavior behavior, typename UnitIter>
auto utfIterator(UnitIter p) {
    return UTFIterator<UnitIter, CP32, behavior>(std::move(p));
}

/**
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam behavior How to handle ill-formed Unicode strings
 * @param s input string_view
 * @return a UTFStringCodePoints&lt;Unit, CP32, behavior&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;,
 *     deducing the Unit character type
 * @draft ICU 78
 */
template<typename CP32, UIllFormedBehavior behavior, typename StringView>
auto utfStringCodePoints(StringView s) {
    return UTFStringCodePoints<typename StringView::value_type, CP32, behavior>(s);
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
 * For reverse iteration, either use this iterator directly as in <code>*--iter</code>
 * or wrap it using std::make_reverse_iterator(iter).
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, typename = void>
class UnsafeUTFIterator {
    using Impl = UnsafeUTFImpl<UnitIter, CP32>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning UnsafeCodeUnits.
    class Proxy {
    public:
        Proxy(UnsafeCodeUnits<UnitIter, CP32> &units) : units_(units) {}
        UnsafeCodeUnits<UnitIter, CP32> &operator*() { return units_; }
        UnsafeCodeUnits<UnitIter, CP32> *operator->() { return &units_; }
    private:
        UnsafeCodeUnits<UnitIter, CP32> units_;
    };

public:
    /** C++ iterator boilerplate @internal */
    using value_type = UnsafeCodeUnits<UnitIter, CP32>;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::conditional_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<UnitIter>::iterator_category>,
        std::bidirectional_iterator_tag,
        std::forward_iterator_tag>;

    inline UnsafeUTFIterator(UnitIter p) : p_(p), units_(0, 0, p) {}

    inline UnsafeUTFIterator(UnsafeUTFIterator &&src) noexcept = default;
    inline UnsafeUTFIterator &operator=(UnsafeUTFIterator &&src) noexcept = default;

    inline UnsafeUTFIterator(const UnsafeUTFIterator &other) = default;
    inline UnsafeUTFIterator &operator=(const UnsafeUTFIterator &other) = default;

    inline bool operator==(const UnsafeUTFIterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    inline bool operator!=(const UnsafeUTFIterator &other) const { return !operator==(other); }

    inline UnsafeCodeUnits<UnitIter, CP32> operator*() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_);
            state_ = 1;
        }
        return units_;
    }

    /**
     * @return the current decoded subsequence via an opaque proxy object
     * so that <code>iter->codePoint()</code> etc. works.
     * @draft ICU 78
     */
    inline Proxy operator->() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_);
            state_ = 1;
        }
        return Proxy(units_);
    }

    inline UnsafeUTFIterator &operator++() {  // pre-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::inc(p_);
        } else /* state_ < 0 */ {
            // operator--() called decAndRead() so we know how far to skip.
            Impl::moveToDecAndReadLimit(p_, units_.length());
            state_ = 0;
        }
        return *this;
    }

    /**
     * @return a copy of this iterator from before the increment.
     *     If UnitIter is a single-pass input_iterator, then this function
     *     returns an opaque proxy object so that <code>*iter++</code> still works.
     * @draft ICU 78
     */
    inline UnsafeUTFIterator operator++(int) {  // post-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            UnsafeUTFIterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            units_ = Impl::readAndInc(p_);
            UnsafeUTFIterator result(*this);
            result.state_ = 1;
            // keep this->state_ == 0
            return result;
        } else /* state_ < 0 */ {
            UnsafeUTFIterator result(*this);
            // operator--() called decAndRead() so we know how far to skip.
            Impl::moveToDecAndReadLimit(p_, units_.length());
            state_ = 0;
            return result;
        }
    }

    // Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
    template<typename Iter = UnitIter>
    inline
    std::enable_if_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<Iter>::iterator_category>,
        UnsafeUTFIterator &>
    operator--() {  // pre-decrement
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is ahead of the logical position.
            p_ = units_.data();
        }
        units_ = Impl::decAndRead(p_);
        state_ = -1;
        return *this;
    }

    // Only enabled if UnitIter is a bidirectional_iterator (including a pointer).
    template<typename Iter = UnitIter>
    inline
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
    friend class std::reverse_iterator<UnsafeUTFIterator<UnitIter, CP32>>;

    inline UnitIter getLogicalPosition() const {
        return state_ <= 0 ? p_ : units_.data();
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable UnsafeCodeUnits<UnitIter, CP32> units_;
    // >0: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start
    mutable int8_t state_ = 0;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
template<typename UnitIter, typename CP32>
class UnsafeUTFIterator<
        UnitIter,
        CP32,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
    using Impl = UnsafeUTFImpl<UnitIter, CP32>;

    // Proxy type for post-increment return value, to make *iter++ work.
    // Also for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning UnsafeCodeUnits.
    class Proxy {
    public:
        Proxy(UnsafeCodeUnits<UnitIter, CP32> &units) : units_(units) {}
        UnsafeCodeUnits<UnitIter, CP32> &operator*() { return units_; }
        UnsafeCodeUnits<UnitIter, CP32> *operator->() { return &units_; }
    private:
        UnsafeCodeUnits<UnitIter, CP32> units_;
    };

public:
    using value_type = UnsafeCodeUnits<UnitIter, CP32>;
    using reference = value_type;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::input_iterator_tag;

    inline UnsafeUTFIterator(UnitIter p) : p_(std::move(p)) {}

    inline UnsafeUTFIterator(UnsafeUTFIterator &&src) noexcept = default;
    inline UnsafeUTFIterator &operator=(UnsafeUTFIterator &&src) noexcept = default;

    inline UnsafeUTFIterator(const UnsafeUTFIterator &other) = default;
    inline UnsafeUTFIterator &operator=(const UnsafeUTFIterator &other) = default;

    inline bool operator==(const UnsafeUTFIterator &other) const {
        return p_ == other.p_ && ahead_ == other.ahead_;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot advance, or do arithmetic with, a single-pass UnitIter.
    }
    inline bool operator!=(const UnsafeUTFIterator &other) const { return !operator==(other); }

    inline UnsafeCodeUnits<UnitIter, CP32> operator*() const {
        if (!ahead_) {
            units_ = Impl::singlePassReadAndInc(p_);
            ahead_ = true;
        }
        return units_;
    }

    inline Proxy operator->() const {
        if (!ahead_) {
            units_ = Impl::singlePassReadAndInc(p_);
            ahead_ = true;
        }
        return Proxy(units_);
    }

    inline UnsafeUTFIterator &operator++() {  // pre-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            Impl::inc(p_);
        }
        return *this;
    }

    inline Proxy operator++(int) {  // post-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            units_ = Impl::singlePassReadAndInc(p_);
            // keep this->ahead_ == false
        }
        return Proxy(units_);
    }

private:
    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // so that we can use a single-pass input iterator for UnitIter.
    mutable UnsafeCodeUnits<UnitIter, CP32> units_ = {0, 0, false};
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
template<typename UnitIter, typename CP32>
class std::reverse_iterator<U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator<UnitIter, CP32>> {
    using Impl = U_HEADER_ONLY_NAMESPACE::UnsafeUTFImpl<UnitIter, CP32>;
    using UnsafeCodeUnits_ = U_HEADER_ONLY_NAMESPACE::UnsafeCodeUnits<UnitIter, CP32>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning UnsafeCodeUnits.
    class Proxy {
    public:
        Proxy(UnsafeCodeUnits_ units) : units_(units) {}
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

    inline reverse_iterator(U_HEADER_ONLY_NAMESPACE::UnsafeUTFIterator<UnitIter, CP32> iter) :
            p_(iter.getLogicalPosition()), units_(0, 0, p_), unitsLimit_(p_) {}

    inline reverse_iterator(reverse_iterator &&src) noexcept = default;
    inline reverse_iterator &operator=(reverse_iterator &&src) noexcept = default;

    inline reverse_iterator(const reverse_iterator &other) = default;
    inline reverse_iterator &operator=(const reverse_iterator &other) = default;

    inline bool operator==(const reverse_iterator &other) const {
        return getLogicalPosition() == other.getLogicalPosition();
    }
    inline bool operator!=(const reverse_iterator &other) const { return !operator==(other); }

    inline UnsafeCodeUnits_ operator*() const {
        if (state_ == 0) {
            unitsLimit_ = p_;
            units_ = Impl::decAndRead(p_);
            state_ = -1;
        }
        return units_;
    }

    inline Proxy operator->() const {
        if (state_ == 0) {
            unitsLimit_ = p_;
            units_ = Impl::decAndRead(p_);
            state_ = -1;
        }
        return Proxy(units_);
    }

    inline reverse_iterator &operator++() {  // pre-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            state_ = 0;
        } else if (state_ == 0) {
            Impl::dec(p_);
        } else /* state_ > 0 */ {
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.data();
            state_ = 0;
        }
        return *this;
    }

    inline reverse_iterator operator++(int) {  // post-increment
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is already behind.
            reverse_iterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            unitsLimit_ = p_;
            units_ = Impl::decAndRead(p_);
            reverse_iterator result(*this);
            result.state_ = -1;
            // keep this->state_ == 0
            return result;
        } else /* state_ > 0 */ {
            reverse_iterator result(*this);
            // operator--() called readAndInc() so we know how far to skip.
            p_ = units_.data();
            state_ = 0;
            return result;
        }
    }

    inline reverse_iterator &operator--() {  // pre-decrement
        if (state_ < 0) {
            // operator*() called decAndRead() so p_ is behind the logical position.
            p_ = unitsLimit_;
        }
        units_ = Impl::readAndInc(p_);
        state_ = 1;
        return *this;
    }

    inline reverse_iterator operator--(int) {  // post-decrement
        reverse_iterator result(*this);
        operator--();
        return result;
    }

private:
    inline UnitIter getLogicalPosition() const {
        return state_ >= 0 ? p_ : unitsLimit_;
    }

    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // Keep state so that we call decAndRead() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable UnsafeCodeUnits_ units_;
    // For fast getLogicalPosition() and operator==().
    mutable UnitIter unitsLimit_;
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
 * @tparam Unit Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @draft ICU 78
 */
template<typename Unit, typename CP32>
class UnsafeUTFStringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    UnsafeUTFStringCodePoints(std::basic_string_view<Unit> s) : s(s) {}

    /** @draft ICU 78 */
    UnsafeUTFStringCodePoints(const UnsafeUTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UnsafeUTFStringCodePoints &operator=(const UnsafeUTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UnsafeUTFIterator<const Unit *, CP32> begin() const {
        return {s.data()};
    }

    /** @draft ICU 78 */
    UnsafeUTFIterator<const Unit *, CP32> end() const {
        return {s.data() + s.length()};
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
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param iter code unit iterator
 * @return an UnsafeUTFIterator&lt;UnitIter, CP32&gt;
 *     for the given code unit iterator or character pointer
 * @draft ICU 78
 */
template<typename CP32, typename UnitIter>
auto unsafeUTFIterator(UnitIter iter) {
    return UnsafeUTFIterator<UnitIter, CP32>(std::move(iter));
}

/**
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t
 * @param s input string_view
 * @return an UnsafeUTFStringCodePoints&lt;Unit, CP32&gt;
 *     for the given std::basic_string_view&lt;Unit&gt;,
 *     deducing the Unit character type
 * @draft ICU 78
 */
template<typename CP32, typename StringView>
auto unsafeUTFStringCodePoints(StringView s) {
    return UnsafeUTFStringCodePoints<typename StringView::value_type, CP32>(s);
}

// ------------------------------------------------------------------------- ***

// TODO: remove experimental sample code
#ifndef UTYPES_H
int32_t rangeLoop16(std::u16string_view s) {
    int32_t sum = 0;
    for (auto units : header::utfStringCodePoints<UChar32, U_BEHAVIOR_NEGATIVE>(s)) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t loopIterPlusPlus16(std::u16string_view s) {
    auto range = header::utfStringCodePoints<UChar32, U_BEHAVIOR_NEGATIVE>(s);
    int32_t sum = 0;
    for (auto iter = range.begin(), limit = range.end(); iter != limit;) {
        sum += (*iter++).codePoint();
    }
    return sum;
}

int32_t backwardLoop16(std::u16string_view s) {
    auto range = header::utfStringCodePoints<UChar32, U_BEHAVIOR_NEGATIVE>(s);
    int32_t sum = 0;
    for (auto start = range.begin(), iter = range.end(); start != iter;) {
        sum += (*--iter).codePoint();
    }
    return sum;
}

int32_t reverseLoop16(std::u16string_view s) {
    auto range = header::utfStringCodePoints<UChar32, U_BEHAVIOR_NEGATIVE>(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
        sum += iter->codePoint();
    }
    return sum;
}

int32_t unsafeRangeLoop16(std::u16string_view s) {
    int32_t sum = 0;
    for (auto units : header::unsafeUTFStringCodePoints<UChar32>(s)) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t unsafeReverseLoop16(std::u16string_view s) {
    auto range = header::unsafeUTFStringCodePoints<UChar32>(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
        sum += iter->codePoint();
    }
    return sum;
}

int32_t rangeLoop8(std::string_view s) {
    int32_t sum = 0;
    for (auto units : header::utfStringCodePoints<UChar32, U_BEHAVIOR_NEGATIVE>(s)) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t reverseLoop8(std::string_view s) {
    auto range = header::utfStringCodePoints<UChar32, U_BEHAVIOR_NEGATIVE>(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
        sum += iter->codePoint();
    }
    return sum;
}

int32_t macroLoop8(std::string_view s) {
    const char *p = s.data();
    int32_t sum = 0;
    for (size_t i = 0, length = s.length(); i < length;) {
        UChar32 c;
        U8_NEXT(p, i, length, c);
        sum += c;
    }
    return sum;
}

int32_t unsafeRangeLoop8(std::string_view s) {
    int32_t sum = 0;
    for (auto units : header::unsafeUTFStringCodePoints<UChar32>(s)) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t unsafeReverseLoop8(std::string_view s) {
    auto range = header::unsafeUTFStringCodePoints<UChar32>(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(), limit = range.rend(); iter != limit; ++iter) {
        sum += iter->codePoint();
    }
    return sum;
}
#endif

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTFITERATOR_H__
