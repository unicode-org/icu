// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utf16cppiter.h
// created: 2024aug12 Markus W. Scherer

#ifndef __UTF16CPPITER_H__
#define __UTF16CPPITER_H__

// TODO: For experimentation outside of ICU, comment out this include.
// Experimentally conditional code below checks for UTYPES_H and
// otherwise uses copies of bits of ICU.
#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API || !defined(UTYPES_H)

#include <string_view>
#ifdef UTYPES_H
#include "unicode/utf16.h"
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
#endif

/**
 * \file
 * \brief C++ header-only API: C++ iterators over Unicode 16-bit strings (=UTF-16 if well-formed).
 */

#ifndef U_HIDE_DRAFT_API

namespace U_HEADER_ONLY_NAMESPACE {

// Some defined behaviors for handling ill-formed 16-bit strings.
// TODO: Maybe share with 8-bit strings, but the SURROGATE option does not have an equivalent there.
//
// TODO: A possible alternative to an enum might be some kind of function template
// which would be fully customizable.
enum U16IllFormedBehavior {
    U16_BEHAVIOR_NEGATIVE,
    U16_BEHAVIOR_FFFD,
    U16_BEHAVIOR_SURROGATE
};

/**
 * A code unit sequence for one code point returned by U16Iterator.
 * TODO: Share with UTF-8?
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U16_BEHAVIOR_NEGATIVE
 * @draft ICU 77
 */
template<typename Unit16, typename CP32>
struct U16OneSeq {
    // Order of fields with padding and access frequency in mind.
    CP32 codePoint = 0;
    uint8_t length = 0;
    bool isWellFormed = false;
    const Unit16 *data;

    std::basic_string_view<Unit16> stringView() const {
        return std::basic_string_view<Unit16>(data, length);
    }

    // TODO: std::optional<CP32> maybeCodePoint() const ? (nullopt if !isWellFormed)
};

/**
 * Internal base class for public U16Iterator & U16ReverseIterator.
 * Not intended for public subclassing.
 * @internal
 */
template<typename Unit16, typename CP32, U16IllFormedBehavior behavior>
class U16IteratorBase {
protected:
    // @internal
    U16IteratorBase(const Unit16 *start, const Unit16 *p, const Unit16 *limit) :
            start(start), current(p), limit(limit) {}
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    // @internal
    bool operator==(const U16IteratorBase &other) const { return current == other.current; }
    // @internal
    bool operator!=(const U16IteratorBase &other) const { return !operator==(other); }

    // @internal
    U16OneSeq<Unit16, CP32> readAndInc(const Unit16 *&p) const {
        // TODO: assert p != limit -- more precisely: start <= p < limit
        // Very similar to U16_NEXT_OR_FFFD().
        const Unit16 *p0 = p;
        CP32 c = *p++;
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

    // @internal
    U16OneSeq<Unit16, CP32> decAndRead(const Unit16 *&p) const {
        // TODO: assert p != limit -- more precisely: start <= p < limit
        // Very similar to U16_PREV_OR_FFFD().
        CP32 c = *--p;
        if (!U16_IS_SURROGATE(c)) {
            return {c, 1, true, p};
        } else {
            uint16_t c2;
            if (U16_IS_SURROGATE_TRAIL(c) && p != start && U16_IS_LEAD(c2 = *(p - 1))) {
                --p;
                c = U16_GET_SUPPLEMENTARY(c2, c);
                return {c, 2, true, p};
            } else {
                return {sub(c), 1, false, p};
            }
        }
    }

    // Handle ill-formed UTF-16: One unpaired surrogate.
    // @internal
    CP32 sub(CP32 surrogate) const {
        switch (behavior) {
            case U16_BEHAVIOR_NEGATIVE: return U_SENTINEL;
            case U16_BEHAVIOR_FFFD: return 0xfffd;
            case U16_BEHAVIOR_SURROGATE: return surrogate;
        }
    }

    // In a validating iterator, we need start & limit so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    // @internal
    const Unit16 *const start;
    // @internal
    const Unit16 *current;
    // @internal
    const Unit16 *const limit;
};

/**
 * Validating bidirectional iterator over the code points in a Unicode 16-bit string.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U16_BEHAVIOR_NEGATIVE
 * @tparam U16IllFormedBehavior TODO
 * @draft ICU 77
 */
template<typename Unit16, typename CP32, U16IllFormedBehavior behavior>
class U16Iterator : private U16IteratorBase<Unit16, CP32, behavior> {
    // FYI: We need to qualify all accesses to super class members because of private inheritance.
    using Super = U16IteratorBase<Unit16, CP32, behavior>;
public:
    // TODO: make private, make friends
    U16Iterator(const Unit16 *start, const Unit16 *p, const Unit16 *limit) :
            Super(start, p, limit) {}

    U16Iterator(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const { return Super::operator==(other); }
    bool operator!=(const U16Iterator &other) const { return !Super::operator==(other); }

    U16OneSeq<Unit16, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        const Unit16 *p = Super::current;
        return Super::readAndInc(p);
    }

    U16Iterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::readAndInc(Super::current);
        return *this;
    }

    U16Iterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        U16Iterator result(*this);
        Super::readAndInc(Super::current);
        return result;
    }

    U16Iterator &operator--() {  // pre-decrement
        return Super::dec();
    }

    U16Iterator operator--(int) {  // post-decrement
        U16Iterator result(*this);
        Super::dec();
        return result;
    }
};

/**
 * Validating reverse iterator over the code points in a Unicode 16-bit string.
 * Not bidirectional, but optimized for reverse iteration.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U16_BEHAVIOR_NEGATIVE
 * @tparam U16IllFormedBehavior TODO
 * @draft ICU 77
 */
template<typename Unit16, typename CP32, U16IllFormedBehavior behavior>
class U16ReverseIterator : private U16IteratorBase<Unit16, CP32, behavior> {
    using Super = U16IteratorBase<Unit16, CP32, behavior>;
public:
    // TODO: make private, make friends
    U16ReverseIterator(const Unit16 *start, const Unit16 *p, const Unit16 *limit) :
            Super(start, p, limit) {}

    U16ReverseIterator(const U16ReverseIterator &other) = default;

    bool operator==(const U16ReverseIterator &other) const { return Super::operator==(other); }
    bool operator!=(const U16ReverseIterator &other) const { return !Super::operator==(other); }

    U16OneSeq<Unit16, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        const Unit16 *p = Super::current;
        return Super::decAndRead(p);
    }

    U16ReverseIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::decAndRead(Super::current);
        return *this;
    }

    U16ReverseIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        U16ReverseIterator result(*this);
        Super::decAndRead(Super::current);
        return result;
    }
};

/**
 * A C++ "range" for iterating over all of the code points of a 16-bit Unicode string.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U16_BEHAVIOR_NEGATIVE
 * @tparam U16IllFormedBehavior TODO
 * @draft ICU 77
 */
template<typename Unit16, typename CP32, U16IllFormedBehavior behavior>
class U16StringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 77
     */
    U16StringCodePoints(std::basic_string_view<Unit16> s) : s(s) {}

    /** @draft ICU 77 */
    U16StringCodePoints(const U16StringCodePoints &other) = default;

    /** @draft ICU 77 */
    U16Iterator<Unit16, CP32, behavior> begin() const {
        return {s.data(), s.data(), s.data() + s.length()};
    }

    /** @draft ICU 77 */
    U16Iterator<Unit16, CP32, behavior> end() const {
        const Unit16 *limit = s.data() + s.length();
        return {s.data(), limit, limit};
    }

    /** @draft ICU 77 */
    U16ReverseIterator<Unit16, CP32, behavior> rbegin() const {
        const Unit16 *limit = s.data() + s.length();
        return {s.data(), limit, limit};
    }

    /** @draft ICU 77 */
    U16ReverseIterator<Unit16, CP32, behavior> rend() const {
        return {s.data(), s.data(), s.data() + s.length()};
    }

private:
    std::basic_string_view<Unit16> s;
};

// ------------------------------------------------------------------------- ***

// TODO: Non-validating iterator over the code points in a Unicode 16-bit string.
// Assumes well-formed UTF-16. Otherwise the behavior is undefined.
// template<typename Unit16>
// class U16UnsafeIterator
// TODO: only p, no start, no limit

// TODO: remove experimental sample code
#ifndef UTYPES_H
int32_t rangeLoop(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, header::U16_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   for (auto seq : range) {
       sum += seq.codePoint;
   }
   return sum;
}

int32_t loopIterPlusPlus(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, header::U16_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   auto iter = range.begin();
   auto limit = range.end();
   while (iter != limit) {
       sum += (*iter++).codePoint;
   }
   return sum;
}

int32_t reverseLoop(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, header::U16_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   for (auto iter = range.rbegin(); iter != range.rend(); ++iter) {
       sum += (*iter).codePoint;
   }
   return sum;
}
#endif

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTF16CPPITER_H__
