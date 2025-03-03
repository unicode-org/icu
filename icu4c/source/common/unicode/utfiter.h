// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utfiter.h
// created: 2024aug12 Markus W. Scherer
// TODO: rename this header file to utfiterator.h?

#ifndef __UTF16CPPITER_H__
#define __UTF16CPPITER_H__

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
#endif

/**
 * \file
 * \brief C++ header-only API: C++ iterators over Unicode 16-bit strings (=UTF-16 if well-formed).
 */

#ifndef U_HIDE_DRAFT_API

// Some defined behaviors for handling ill-formed Unicode strings.
// TODO: For UTF-32, we have basically orthogonal conditions for surrogate vs. out-of-range.
// Maybe make U_BEHAVIOR_SURROGATE return FFFD for out-of-range?
typedef enum UIllFormedBehavior {
    U_BEHAVIOR_NEGATIVE,
    U_BEHAVIOR_FFFD,
    U_BEHAVIOR_SURROGATE
} UIllFormedBehavior;

namespace U_HEADER_ONLY_NAMESPACE {

/**
 * Result of validating and decoding a minimal Unicode code unit sequence.
 * Returned from validating Unicode string code point iterators.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, typename = void>
class CodeUnits {
    using Unit = typename std::iterator_traits<UnitIter>::value_type;
public:
    // @internal
    CodeUnits(CP32 codePoint, uint8_t length, bool wellFormed, UnitIter data) :
            c(codePoint), len(length), ok(wellFormed), p(data) {}

    CodeUnits(const CodeUnits &other) = default;
    CodeUnits &operator=(const CodeUnits &other) = default;

    UChar32 codePoint() const { return c; }

    bool wellFormed() const { return ok; }

    UnitIter data() const { return p; }

    uint8_t length() const { return len; }

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
    bool ok;
    UnitIter p;
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
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
    using Unit = typename std::iterator_traits<UnitIter>::value_type;
public:
    // @internal
    CodeUnits(CP32 codePoint, uint8_t length, bool wellFormed) :
            c(codePoint), len(length), ok(wellFormed) {}

    CodeUnits(const CodeUnits &other) = default;
    CodeUnits &operator=(const CodeUnits &other) = default;

    UChar32 codePoint() const { return c; }

    bool wellFormed() const { return ok; }

    uint8_t length() const { return len; }

private:
    // Order of fields with padding and access frequency in mind.
    CP32 c;
    uint8_t len;
    bool ok;
};
#endif  // U_IN_DOXYGEN

// TODO: switch unsafe code to UnitIter as well
/**
 * Result of decoding a minimal Unicode code unit sequence which must be well-formed.
 * Returned from non-validating Unicode string code point iterators.
 *
 * @tparam Unit Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit, typename CP32>
class UnsafeCodeUnits {
public:
    // @internal
    UnsafeCodeUnits(CP32 codePoint, uint8_t length, const Unit *data) :
            c(codePoint), len(length), p(data) {}

    UnsafeCodeUnits(const UnsafeCodeUnits &other) = default;
    UnsafeCodeUnits &operator=(const UnsafeCodeUnits &other) = default;

    UChar32 codePoint() const { return c; }

    // TODO: disable for single-pass input iterator
    const Unit *data() const { return p; }

    uint8_t length() const { return len; }

    // TODO: disable unless pointer
    std::basic_string_view<Unit> stringView() const {
        return std::basic_string_view<Unit>(p, len);
    }

    // TODO: std::optional<CP32> maybeCodePoint() const ? (nullopt if ill-formed)

private:
    // Order of fields with padding and access frequency in mind.
    CP32 c;
    uint8_t len;
    const Unit *p;
};

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

    static inline void inc(UnitIter &p, UnitIter limit) {
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

    static inline CodeUnits<UnitIter, CP32> singlePassReadAndInc(UnitIter &p, UnitIter limit) {
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

    static inline void moveToReadAndIncStart(UnitIter &p, int8_t &state) {
        // state > 0 after readAndInc()
        do { --p; } while (--state != 0);
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, int8_t &state) {
        // state < 0 after decAndRead()
        do { ++p; } while (++state != 0);
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

    static inline void inc(UnitIter &p, UnitIter limit) {
        // Very similar to U16_FWD_1().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
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

    static inline CodeUnits<UnitIter, CP32> singlePassReadAndInc(UnitIter &p, UnitIter limit) {
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

    static inline void moveToReadAndIncStart(UnitIter &p, int8_t &state) {
        // state > 0 after readAndInc(); max 2 for UTF-16
        --p;
        if (--state != 0) {
            --p;
            state = 0;
        }
    }

    static inline void moveToDecAndReadLimit(UnitIter &p, int8_t &state) {
        // state < 0 after decAndRead(); max 2 for UTF-16
        ++p;
        if (++state != 0) {
            ++p;
            state = 0;
        }
    }
};

#endif

/**
 * Validating bidirectional iterator over the code points in a Unicode 16-bit string.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
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
    using value_type = CodeUnits<UnitIter, CP32>;
    // TODO: review the reference and pointer types. Should pointer be Proxy?
    using reference = value_type &;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::conditional_t<
        std::is_base_of_v<
            std::bidirectional_iterator_tag,
            typename std::iterator_traits<UnitIter>::iterator_category>,
        std::bidirectional_iterator_tag,
        std::forward_iterator_tag>;

    // TODO: Maybe std::move() the UnitIters?
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.
    inline UTFIterator(UnitIter start, UnitIter p, UnitIter limit) :
            p_(p), start_(start), limit_(limit), units_(0, 0, false, p) {}
    // Constructs an iterator with start=p.
    inline UTFIterator(UnitIter p, UnitIter limit) :
            p_(p), start_(p), limit_(limit), units_(0, 0, false, p) {}
    // Constructs an iterator start or limit sentinel.
    inline UTFIterator(UnitIter p) : p_(p), start_(p), limit_(p), units_(0, 0, false, p) {}

    inline UTFIterator(const UTFIterator &other) = default;
    inline UTFIterator &operator=(const UTFIterator &other) = default;

    inline bool operator==(const UTFIterator &other) const {
        // Compare logical positions.
        UnitIter p1 = state_ <= 0 ? p_ : units_.data();
        UnitIter p2 = other.state_ <= 0 ? other.p_ : other.units_.data();
        return p1 == p2;
    }
    inline bool operator!=(const UTFIterator &other) const { return !operator==(other); }

    inline CodeUnits<UnitIter, CP32> operator*() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            state_ = units_.length();
        }
        return units_;
    }

    inline Proxy operator->() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            state_ = units_.length();
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
            Impl::moveToDecAndReadLimit(p_, state_);
        }
        return *this;
    }

    inline UTFIterator operator++(int) {  // post-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            UTFIterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            UTFIterator result(*this);
            result.state_ = units_.length();
            // keep this->state_ == 0
            return result;
        } else /* state_ < 0 */ {
            UTFIterator result(*this);
            // operator--() called decAndRead() so we know how far to skip.
            Impl::moveToDecAndReadLimit(p_, state_);
            return result;
        }
    }

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
            Impl::moveToReadAndIncStart(p_, state_);
        }
        units_ = Impl::decAndRead(start_, p_);
        state_ = -units_.length();
        return *this;
    }

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
    // operator*() etc. are logically const.
    mutable UnitIter p_;
    // In a validating iterator, we need start_ & limit_ so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    const UnitIter start_;
    const UnitIter limit_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    mutable CodeUnits<UnitIter, CP32> units_;
    // >0: units_ = readAndInc(), p_ = units limit, state_ = units_.len
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // <0: units_ = decAndRead(), p_ = units start, state_ = -units_.len
    // TODO: could insert state_ into hidden CodeUnits field to avoid padding,
    //       but mostly irrelevant when inlined?
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
    using reference = value_type &;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::input_iterator_tag;

    // TODO: Does it make sense for the limits to allow having a different type?
    // We only need to be able to compare p_ vs. limit_ for == and !=.
    // Might allow interesting sentinel types.
    // Would be trouble for the sentinel constructor that inits both iters from the same p.

    inline UTFIterator(UnitIter p, UnitIter limit) : p_(p), limit_(limit) {}
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    // Constructs an iterator start or limit sentinel.
    inline UTFIterator(UnitIter p) : p_(p), limit_(p) {}

    inline UTFIterator(const UTFIterator &other) = default;
    inline UTFIterator &operator=(const UTFIterator &other) = default;

    inline bool operator==(const UTFIterator &other) const {
        return p_ == other.p_ && ahead_ == other.ahead_;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot move, or do arithmetic with, a single-pass UnitIter.
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
    // In a validating iterator, we need  limit_ so that when we read a code point
    // we can test if there are enough code units.
    const UnitIter limit_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // so that we can use a single-pass input iterator for UnitIter.
    mutable CodeUnits<UnitIter, CP32> units_ = {0, 0, false};
    // true: units_ = readAndInc(), p_ = units limit
    //     which means that p_ is ahead of its logical position
    // false: initial state
    // TODO: could insert ahead_ into hidden CodeUnits field to avoid padding,
    //       but mostly irrelevant when inlined?
    mutable bool ahead_ = false;
};
#endif  // U_IN_DOXYGEN

/**
 * Validating reverse iterator over the code points in a Unicode 16-bit string.
 * Not bidirectional, but optimized for reverse iteration.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class UTFReverseIterator {
    using Impl = UTFImpl<UnitIter, CP32, behavior>;

    // Proxy type for operator->() (required by LegacyInputIterator)
    // so that we don't promise always returning CodeUnits.
    class Proxy {
    public:
        Proxy(CodeUnits<UnitIter, CP32> units) : units_(units) {}
        CodeUnits<UnitIter, CP32> &operator*() { return units_; }
        CodeUnits<UnitIter, CP32> *operator->() { return &units_; }
    private:
        CodeUnits<UnitIter, CP32> units_;
    };

public:
    using value_type = CodeUnits<UnitIter, CP32>;
    using reference = value_type &;
    using pointer = Proxy;
    using difference_type = typename std::iterator_traits<UnitIter>::difference_type;
    using iterator_category = std::forward_iterator_tag;

    inline UTFReverseIterator(UnitIter start, UnitIter p) : p_(p), start_(start) {}
    // Constructs an iterator start or limit sentinel.
    inline UTFReverseIterator(UnitIter p) : p_(p), start_(p) {}

    inline UTFReverseIterator(const UTFReverseIterator &other) = default;
    inline UTFReverseIterator &operator=(const UTFReverseIterator &other) = default;

    inline bool operator==(const UTFReverseIterator &other) const { return p_ == other.p_; }
    inline bool operator!=(const UTFReverseIterator &other) const { return !operator==(other); }

    inline CodeUnits<UnitIter, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnitIter p = p_;
        return Impl::decAndRead(start_, p);
    }

    inline Proxy operator->() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnitIter p = p_;
        return Proxy(Impl::decAndRead(start_, p));
    }

    inline UTFReverseIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Impl::decAndRead(start_, p_);
        return *this;
    }

    inline UTFReverseIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UTFReverseIterator result(*this);
        Impl::decAndRead(start_, p_);
        return result;
    }

private:
    UnitIter p_;
    // In a validating iterator, we need start_ so that when we read a code point
    // backward we can test if there are enough code units.
    const UnitIter start_;
};

/**
 * A C++ "range" for validating iteration over all of the code points of a 16-bit Unicode string.
 *
 * @tparam Unit16 Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename Unit16, typename CP32, UIllFormedBehavior behavior>
class UTFStringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    UTFStringCodePoints(std::basic_string_view<Unit16> s) : s(s) {}

    /** @draft ICU 78 */
    UTFStringCodePoints(const UTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UTFStringCodePoints &operator=(const UTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UTFIterator<const Unit16 *, CP32, behavior> begin() const {
        return {s.data(), s.data(), s.data() + s.length()};
    }

    /** @draft ICU 78 */
    UTFIterator<const Unit16 *, CP32, behavior> end() const {
        const Unit16 *limit = s.data() + s.length();
        return {s.data(), limit, limit};
    }

#if 0
    /** @draft ICU 78 */
    UTFReverseIterator<const Unit16 *, CP32, behavior> rbegin() const {
        return {s.data(), s.data() + s.length()};
    }

    /** @draft ICU 78 */
    UTFReverseIterator<const Unit16 *, CP32, behavior> rend() const {
        return {s.data(), s.data()};
    }
#else
    auto rbegin() const {
        return std::make_reverse_iterator(end());
    }
    auto rend() const {
        return std::make_reverse_iterator(begin());
    }
#endif

private:
    std::basic_string_view<Unit16> s;
};

// ------------------------------------------------------------------------- ***

/**
 * Internal base class for public UnsafeUTFIterator & UnsafeUTFReverseIterator.
 * Not intended for public subclassing.
 *
 * @tparam Unit16 Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @internal
 */
template<typename Unit16, typename CP32>
class UnsafeUTFIteratorBase {
protected:
    // @internal
    UnsafeUTFIteratorBase(const Unit16 *p) : p_(p) {}
    // Test pointers for == or != but not < or >.

    // @internal
    UnsafeUTFIteratorBase(const UnsafeUTFIteratorBase &other) = default;
    // @internal
    UnsafeUTFIteratorBase &operator=(const UnsafeUTFIteratorBase &other) = default;

    // @internal
    bool operator==(const UnsafeUTFIteratorBase &other) const { return p_ == other.p_; }
    // @internal
    bool operator!=(const UnsafeUTFIteratorBase &other) const { return !operator==(other); }

    // @internal
    void dec() {
        // Very similar to U16_BACK_1_UNSAFE().
        if (U16_IS_TRAIL(*--p_)) {
            --p_;
        }
    }

    // @internal
    UnsafeCodeUnits<Unit16, CP32> readAndInc(const Unit16 *&p) const {
        // Very similar to U16_NEXT_UNSAFE().
        const Unit16 *p0 = p;
        CP32 c = *p;
        ++p;
        if (!U16_IS_LEAD(c)) {
            return {c, 1, p0};
        } else {
            c = U16_GET_SUPPLEMENTARY(c, *p);
            ++p;
            return {c, 2, p0};
        }
    }

    // @internal
    UnsafeCodeUnits<Unit16, CP32> decAndRead(const Unit16 *&p) const {
        // Very similar to U16_PREV_UNSAFE().
        CP32 c = *--p;
        if (!U16_IS_TRAIL(c)) {
            return {c, 1, p};
        } else {
            c = U16_GET_SUPPLEMENTARY(*--p, c);
            return {c, 2, p};
        }
    }

    // @internal
    const Unit16 *p_;
};

// TODO: make this one work single-pass as well
/**
 * Non-validating bidirectional iterator over the code points in a UTF-16 string.
 * The string must be well-formed.
 *
 * @tparam Unit16 Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit16, typename CP32>
class UnsafeUTFIterator : private UnsafeUTFIteratorBase<Unit16, CP32> {
    // FYI: We need to qualify all accesses to super class members because of private inheritance.
    using Super = UnsafeUTFIteratorBase<Unit16, CP32>;
public:
    UnsafeUTFIterator(const Unit16 *p) : Super(p) {}

    UnsafeUTFIterator(const UnsafeUTFIterator &other) = default;
    UnsafeUTFIterator &operator=(const UnsafeUTFIterator &other) = default;

    bool operator==(const UnsafeUTFIterator &other) const { return Super::operator==(other); }
    bool operator!=(const UnsafeUTFIterator &other) const { return !Super::operator==(other); }

    UnsafeCodeUnits<Unit16, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        const Unit16 *p = Super::p_;
        return Super::readAndInc(p);
    }

    UnsafeUTFIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::readAndInc(Super::p_);
        return *this;
    }

    // TODO: disable for single-pass input iterator? or return proxy like std::istreambuf_iterator?
    UnsafeUTFIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnsafeUTFIterator result(*this);
        Super::readAndInc(Super::p_);
        return result;
    }

    UnsafeUTFIterator &operator--() {  // pre-decrement
        Super::dec();
        return *this;
    }

    UnsafeUTFIterator operator--(int) {  // post-decrement
        UnsafeUTFIterator result(*this);
        Super::dec();
        return result;
    }
};

/**
 * Non-validating reverse iterator over the code points in a UTF-16 string.
 * Not bidirectional, but optimized for reverse iteration.
 * The string must be well-formed.
 *
 * @tparam Unit16 Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit16, typename CP32>
class UnsafeUTFReverseIterator : private UnsafeUTFIteratorBase<Unit16, CP32> {
    using Super = UnsafeUTFIteratorBase<Unit16, CP32>;
public:
    UnsafeUTFReverseIterator(const Unit16 *p) : Super(p) {}

    UnsafeUTFReverseIterator(const UnsafeUTFReverseIterator &other) = default;
    UnsafeUTFReverseIterator &operator=(const UnsafeUTFReverseIterator &other) = default;

    bool operator==(const UnsafeUTFReverseIterator &other) const { return Super::operator==(other); }
    bool operator!=(const UnsafeUTFReverseIterator &other) const { return !Super::operator==(other); }

    UnsafeCodeUnits<Unit16, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        const Unit16 *p = Super::p_;
        return Super::decAndRead(p);
    }

    UnsafeUTFReverseIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::decAndRead(Super::p_);
        return *this;
    }

    UnsafeUTFReverseIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnsafeUTFReverseIterator result(*this);
        Super::decAndRead(Super::p_);
        return result;
    }
};

/**
 * A C++ "range" for non-validating iteration over all of the code points of a UTF-16 string.
 * The string must be well-formed.
 *
 * @tparam Unit16 Code unit type:
 *     UTF-8: char or char8_t or uint8_t;
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit16, typename CP32>
class UnsafeUTFStringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    UnsafeUTFStringCodePoints(std::basic_string_view<Unit16> s) : s(s) {}

    /** @draft ICU 78 */
    UnsafeUTFStringCodePoints(const UnsafeUTFStringCodePoints &other) = default;
    UnsafeUTFStringCodePoints &operator=(const UnsafeUTFStringCodePoints &other) = default;

    /** @draft ICU 78 */
    UnsafeUTFIterator<Unit16, CP32> begin() const {
        return {s.data()};
    }

    /** @draft ICU 78 */
    UnsafeUTFIterator<Unit16, CP32> end() const {
        return {s.data() + s.length()};
    }

    /** @draft ICU 78 */
    UnsafeUTFReverseIterator<Unit16, CP32> rbegin() const {
        return {s.data() + s.length()};
    }

    /** @draft ICU 78 */
    UnsafeUTFReverseIterator<Unit16, CP32> rend() const {
        return {s.data()};
    }

private:
    std::basic_string_view<Unit16> s;
};

// ------------------------------------------------------------------------- ***

// TODO: UTF-8

// TODO: remove experimental sample code
#ifndef UTYPES_H
int32_t rangeLoop16(std::u16string_view s) {
    header::UTFStringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
    int32_t sum = 0;
    for (auto units : range) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t loopIterPlusPlus16(std::u16string_view s) {
    header::UTFStringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
    int32_t sum = 0;
    auto iter = range.begin();
    auto limit = range.end();
    while (iter != limit) {
        sum += (*iter++).codePoint();
    }
    return sum;
}

int32_t backwardLoop16(std::u16string_view s) {
    header::UTFStringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
    int32_t sum = 0;
    auto start = range.begin();
    auto iter = range.end();
    while (start != iter) {
        sum += (*--iter).codePoint();
    }
    return sum;
}

int32_t reverseLoop16(std::u16string_view s) {
    header::UTFStringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(); iter != range.rend(); ++iter) {
        sum += iter->codePoint();
    }
    return sum;
}

int32_t unsafeRangeLoop16(std::u16string_view s) {
    header::UnsafeUTFStringCodePoints<char16_t, UChar32> range(s);
    int32_t sum = 0;
    for (auto units : range) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t unsafeReverseLoop16(std::u16string_view s) {
    header::UnsafeUTFStringCodePoints<char16_t, UChar32> range(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(); iter != range.rend(); ++iter) {
        sum += (*iter).codePoint();  // TODO: ->
    }
    return sum;
}

int32_t rangeLoop8(std::string_view s) {
    header::UTFStringCodePoints<char, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
    int32_t sum = 0;
    for (auto units : range) {
        sum += units.codePoint();
    }
    return sum;
}

int32_t reverseLoop8(std::string_view s) {
    header::UTFStringCodePoints<char, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
    int32_t sum = 0;
    for (auto iter = range.rbegin(); iter != range.rend(); ++iter) {
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
#endif

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTF16CPPITER_H__
