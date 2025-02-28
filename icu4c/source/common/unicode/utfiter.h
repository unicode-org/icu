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

// Some defined behaviors for handling ill-formed Unicode strings.
// TODO: For 8-bit strings, the SURROGATE option does not have an equivalent -- static_assert.
typedef enum UIllFormedBehavior {
    U_BEHAVIOR_NEGATIVE,
    U_BEHAVIOR_FFFD,
    U_BEHAVIOR_SURROGATE
} UIllFormedBehavior;

namespace U_HEADER_ONLY_NAMESPACE {

// Handle ill-formed UTF-16: One unpaired surrogate.
// @internal
template<typename CP32, UIllFormedBehavior behavior>
CP32 uprv_u16Sub(CP32 surrogate) {
    switch (behavior) {
        case U_BEHAVIOR_NEGATIVE: return U_SENTINEL;
        case U_BEHAVIOR_FFFD: return 0xfffd;
        case U_BEHAVIOR_SURROGATE: return surrogate;
    }
}

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

    // TODO: Do we even need the template logic here?
    // Or is it disabled anyway if the code does not compile with a non-pointer?
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

/**
 * Internal base class for public U16Iterator & U16ReverseIterator.
 * Not intended for public subclassing.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @internal
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class U16IteratorBase {
protected:
    // TODO: Maybe std::move() the UnitIters?
    // @internal
    U16IteratorBase(UnitIter start, UnitIter limit) : start(start), limit(limit) {}
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    // @internal
    U16IteratorBase(const U16IteratorBase &other) = default;
    // @internal
    U16IteratorBase &operator=(const U16IteratorBase &other) = default;

    // @internal
    void inc(UnitIter &p) {
        // TODO: assert p != limit -- more precisely: start <= p < limit
        // Very similar to U16_FWD_1().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
        }
    }

    // TODO: unused, remove?
    // @internal
    void dec(UnitIter &p) {
        // TODO: assert p != start -- more precisely: start < p <= limit
        // Very similar to U16_BACK_1().
        UnitIter p1;
        if (U16_IS_TRAIL(*--p) && p != start && (p1 = p, U16_IS_LEAD(*--p1))) {
            p = p1;
        }
    }

    // @internal
    CodeUnits<UnitIter, CP32> readAndInc(UnitIter &p) const {
        // TODO: assert p != limit -- more precisely: start <= p < limit
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
                return {uprv_u16Sub<CP32, behavior>(c), 1, false, p0};
            }
        }
    }

    // @internal
    CodeUnits<UnitIter, CP32> decAndRead(UnitIter &p) const {
        // TODO: assert p != start -- more precisely: start < p <= limit
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
                return {uprv_u16Sub<CP32, behavior>(c), 1, false, p};
            }
        }
    }

    // In a validating iterator, we need start & limit so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    // @internal
    const UnitIter start;
    // @internal
    const UnitIter limit;
};

/**
 * Validating bidirectional iterator over the code points in a Unicode 16-bit string.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior, typename = void>
class U16Iterator : private U16IteratorBase<UnitIter, CP32, behavior> {
    // FYI: We need to qualify all accesses to super class members because of private inheritance.
    using Super = U16IteratorBase<UnitIter, CP32, behavior>;

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
    // TODO: Should these Iterators define value_type etc.?
    //       What about iterator_category depending on the UnitIter??

    U16Iterator(UnitIter start, UnitIter p, UnitIter limit) :
            Super(start, limit), p_(p), units(0, 0, false, p) {}
    // Constructs an iterator start or limit sentinel.
    U16Iterator(UnitIter p) : Super(p, p), p_(p), units(0, 0, false, p) {}

    U16Iterator(const U16Iterator &other) = default;
    U16Iterator &operator=(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const {
        // Compare logical positions.
        UnitIter p1 = state <= 0 ? p_ : units.data();
        UnitIter p2 = other.state <= 0 ? other.p_ : other.units.data();
        return p1 == p2;
    }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    CodeUnits<UnitIter, CP32> operator*() {
        if (state == 0) {
            units = Super::readAndInc(p_);
            state = units.length();
        }
        return units;
    }

    Proxy operator->() {
        if (state == 0) {
            units = Super::readAndInc(p_);
            state = units.length();
        }
        return Proxy(units);
    }

    U16Iterator &operator++() {  // pre-increment
        if (state > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state = 0;
        } else if (state == 0) {
            Super::inc(p_);
        } else /* state < 0 */ {
            // operator--() called decAndRead() so we know how far to skip; max 2 for UTF-16.
            ++p_;
            if (++state != 0) {
                ++p_;
                state = 0;
            }
        }
        return *this;
    }

    U16Iterator operator++(int) {  // post-increment
        if (state > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            U16Iterator result(*this);
            state = 0;
            return result;
        } else if (state == 0) {
            units = Super::readAndInc(p_);
            U16Iterator result(*this);
            result.state = units.length();
            // keep this->state == 0
            return result;
        } else /* state < 0 */ {
            // operator--() called decAndRead() so we know how far to skip; max 2 for UTF-16.
            U16Iterator result(*this);
            ++p_;
            if (++state != 0) {
                ++p_;
                state = 0;
            }
            return result;
        }
    }

    U16Iterator &operator--() {  // pre-decrement
        if (state > 0) {
            // operator*() called readAndInc() so p_ is ahead of the logical position.
            --p_;
            if (--state != 0) {
                --p_;
                state = 0;
            }
        }
        units = Super::decAndRead(p_);
        state = -units.length();
        return *this;
    }

    U16Iterator operator--(int) {  // post-decrement
        U16Iterator result(*this);
        operator--();
        return result;
    }

private:
    UnitIter p_;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // to make it easy for the compiler to optimize.
    CodeUnits<UnitIter, CP32> units;
    // >0: units = readAndInc(), p_ = units limit, state = units.len
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // <0: units = decAndRead(), p_ = units start, state = -units.len
    // TODO: could also set state = -1 & use units.len when needed, but less consistent
    // TODO: could insert state into hidden CodeUnits field to avoid padding,
    //       but mostly irrelevant when inlined?
    int8_t state = 0;
};

#ifndef U_IN_DOXYGEN
// Partial template specialization for single-pass input iterator.
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class U16Iterator<
        UnitIter,
        CP32,
        behavior,
        std::enable_if_t<
            !std::is_base_of_v<
                std::forward_iterator_tag,
                typename std::iterator_traits<UnitIter>::iterator_category>>> {
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
    // TODO: Should these Iterators define value_type etc.?
    //       What about iterator_category depending on the UnitIter??

    // TODO: Does it make sense for the limits to allow having a different type?
    // We only need to be able to compare p_ vs. limit for == and !=.
    // Might allow interesting sentinel types.
    // Would be trouble for the sentinel constructor that inits both iters from the same p.

    U16Iterator(UnitIter p, UnitIter limit) : p_(p), limit(limit) {}
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    // Constructs an iterator start or limit sentinel.
    U16Iterator(UnitIter p) : p_(p), limit(p) {}

    U16Iterator(const U16Iterator &other) = default;
    U16Iterator &operator=(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const {
        return p_ == other.p_ && state == other.state;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot move, or do arithmetic with, a single-pass UnitIter.
    }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    CodeUnits<UnitIter, CP32> operator*() {
        if (state == 0) {
            units = readAndInc(p_);
            state = units.length();
        }
        return units;
    }

    Proxy operator->() {
        if (state == 0) {
            units = readAndInc(p_);
            state = units.length();
        }
        return Proxy(units);
    }

    U16Iterator &operator++() {  // pre-increment
        if (state != 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state = 0;
        } else {
            inc();
        }
        return *this;
    }

    Proxy operator++(int) {  // post-increment
        if (state != 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            state = 0;
        } else {
            units = readAndInc(p_);
            // keep this->state == 0
        }
        return Proxy(units);
    }

private:
    // @internal
    void inc() {
        // TODO: assert p_ != limit -- more precisely: start <= p_ < limit
        // Very similar to U16_FWD_1().
        auto c = *p_;
        ++p_;
        if (U16_IS_LEAD(c) && p_ != limit && U16_IS_TRAIL(*p_)) {
            ++p_;
        }
    }

    // @internal
    CodeUnits<UnitIter, CP32> readAndInc(UnitIter &p) const {
        // TODO: assert p != limit -- more precisely: start <= p < limit
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
                return {uprv_u16Sub<CP32, behavior>(c), 1, false};
            }
        }
    }

    // In a validating iterator, we need the limit so that when we read a code point
    // we can test if there are enough code units.
    UnitIter p_;
    const UnitIter limit;
    // Keep state so that we call readAndInc() only once for both operator*() and ++
    // so that we can use a single-pass input iterator for UnitIter.
    CodeUnits<UnitIter, CP32> units = {0, 0, false};
    // >0: units = readAndInc(), p_ = units limit, state = units.len
    //     which means that p_ is ahead of its logical position
    //  0: initial state
    // TODO: could insert state into hidden CodeUnits field to avoid padding,
    //       but mostly irrelevant when inlined?
    int8_t state = 0;
};
#endif  // U_IN_DOXYGEN

/**
 * Validating reverse iterator over the code points in a Unicode 16-bit string.
 * Not bidirectional, but optimized for reverse iteration.
 *
 * @tparam UnitIter An iterator (often a pointer) that returns a code unit type:
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class U16ReverseIterator : private U16IteratorBase<UnitIter, CP32, behavior> {
    using Super = U16IteratorBase<UnitIter, CP32, behavior>;

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
    U16ReverseIterator(UnitIter start, UnitIter p, UnitIter limit) :
            Super(start, limit), p_(p) {}
    // Constructs an iterator start or limit sentinel.
    U16ReverseIterator(UnitIter p) : Super(p, p), p_(p) {}

    U16ReverseIterator(const U16ReverseIterator &other) = default;
    U16ReverseIterator &operator=(const U16ReverseIterator &other) = default;

    bool operator==(const U16ReverseIterator &other) const { return p_ == other.p_; }
    bool operator!=(const U16ReverseIterator &other) const { return !operator==(other); }

    CodeUnits<UnitIter, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnitIter p = p_;
        return Super::decAndRead(p);
    }

    Proxy operator->() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnitIter p = p_;
        return Proxy(Super::decAndRead(p));
    }

    U16ReverseIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::decAndRead(p_);
        return *this;
    }

    U16ReverseIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        U16ReverseIterator result(*this);
        Super::decAndRead(p_);
        return result;
    }

private:
    UnitIter p_;
};

/**
 * A C++ "range" for validating iteration over all of the code points of a 16-bit Unicode string.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename Unit16, typename CP32, UIllFormedBehavior behavior>
class U16StringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    U16StringCodePoints(std::basic_string_view<Unit16> s) : s(s) {}

    /** @draft ICU 78 */
    U16StringCodePoints(const U16StringCodePoints &other) = default;

    /** @draft ICU 78 */
    U16StringCodePoints &operator=(const U16StringCodePoints &other) = default;

    /** @draft ICU 78 */
    U16Iterator<const Unit16 *, CP32, behavior> begin() const {
        return {s.data(), s.data(), s.data() + s.length()};
    }

    /** @draft ICU 78 */
    U16Iterator<const Unit16 *, CP32, behavior> end() const {
        const Unit16 *limit = s.data() + s.length();
        return {s.data(), limit, limit};
    }

    /** @draft ICU 78 */
    U16ReverseIterator<const Unit16 *, CP32, behavior> rbegin() const {
        const Unit16 *limit = s.data() + s.length();
        return {s.data(), limit, limit};
    }

    /** @draft ICU 78 */
    U16ReverseIterator<const Unit16 *, CP32, behavior> rend() const {
        return {s.data(), s.data(), s.data() + s.length()};
    }

private:
    std::basic_string_view<Unit16> s;
};

// ------------------------------------------------------------------------- ***

/**
 * Internal base class for public U16UnsafeIterator & U16UnsafeReverseIterator.
 * Not intended for public subclassing.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @internal
 */
template<typename Unit16, typename CP32>
class U16UnsafeIteratorBase {
protected:
    // @internal
    U16UnsafeIteratorBase(const Unit16 *p) : p_(p) {}
    // Test pointers for == or != but not < or >.

    // @internal
    U16UnsafeIteratorBase(const U16UnsafeIteratorBase &other) = default;
    // @internal
    U16UnsafeIteratorBase &operator=(const U16UnsafeIteratorBase &other) = default;

    // @internal
    bool operator==(const U16UnsafeIteratorBase &other) const { return p_ == other.p_; }
    // @internal
    bool operator!=(const U16UnsafeIteratorBase &other) const { return !operator==(other); }

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
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit16, typename CP32>
class U16UnsafeIterator : private U16UnsafeIteratorBase<Unit16, CP32> {
    // FYI: We need to qualify all accesses to super class members because of private inheritance.
    using Super = U16UnsafeIteratorBase<Unit16, CP32>;
public:
    U16UnsafeIterator(const Unit16 *p) : Super(p) {}

    U16UnsafeIterator(const U16UnsafeIterator &other) = default;
    U16UnsafeIterator &operator=(const U16UnsafeIterator &other) = default;

    bool operator==(const U16UnsafeIterator &other) const { return Super::operator==(other); }
    bool operator!=(const U16UnsafeIterator &other) const { return !Super::operator==(other); }

    UnsafeCodeUnits<Unit16, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        const Unit16 *p = Super::p_;
        return Super::readAndInc(p);
    }

    U16UnsafeIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::readAndInc(Super::p_);
        return *this;
    }

    // TODO: disable for single-pass input iterator? or return proxy like std::istreambuf_iterator?
    U16UnsafeIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        U16UnsafeIterator result(*this);
        Super::readAndInc(Super::p_);
        return result;
    }

    U16UnsafeIterator &operator--() {  // pre-decrement
        Super::dec();
        return *this;
    }

    U16UnsafeIterator operator--(int) {  // post-decrement
        U16UnsafeIterator result(*this);
        Super::dec();
        return result;
    }
};

/**
 * Non-validating reverse iterator over the code points in a UTF-16 string.
 * Not bidirectional, but optimized for reverse iteration.
 * The string must be well-formed.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit16, typename CP32>
class U16UnsafeReverseIterator : private U16UnsafeIteratorBase<Unit16, CP32> {
    using Super = U16UnsafeIteratorBase<Unit16, CP32>;
public:
    U16UnsafeReverseIterator(const Unit16 *p) : Super(p) {}

    U16UnsafeReverseIterator(const U16UnsafeReverseIterator &other) = default;
    U16UnsafeReverseIterator &operator=(const U16UnsafeReverseIterator &other) = default;

    bool operator==(const U16UnsafeReverseIterator &other) const { return Super::operator==(other); }
    bool operator!=(const U16UnsafeReverseIterator &other) const { return !Super::operator==(other); }

    UnsafeCodeUnits<Unit16, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        const Unit16 *p = Super::p_;
        return Super::decAndRead(p);
    }

    U16UnsafeReverseIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Super::decAndRead(Super::p_);
        return *this;
    }

    U16UnsafeReverseIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        U16UnsafeReverseIterator result(*this);
        Super::decAndRead(Super::p_);
        return result;
    }
};

/**
 * A C++ "range" for non-validating iteration over all of the code points of a UTF-16 string.
 * The string must be well-formed.
 *
 * @tparam Unit16 Code unit type: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @draft ICU 78
 */
template<typename Unit16, typename CP32>
class U16UnsafeStringCodePoints {
public:
    /**
     * Constructs a C++ "range" object over the code points in the string.
     * @draft ICU 78
     */
    U16UnsafeStringCodePoints(std::basic_string_view<Unit16> s) : s(s) {}

    /** @draft ICU 78 */
    U16UnsafeStringCodePoints(const U16UnsafeStringCodePoints &other) = default;
    U16UnsafeStringCodePoints &operator=(const U16UnsafeStringCodePoints &other) = default;

    /** @draft ICU 78 */
    U16UnsafeIterator<Unit16, CP32> begin() const {
        return {s.data()};
    }

    /** @draft ICU 78 */
    U16UnsafeIterator<Unit16, CP32> end() const {
        return {s.data() + s.length()};
    }

    /** @draft ICU 78 */
    U16UnsafeReverseIterator<Unit16, CP32> rbegin() const {
        return {s.data() + s.length()};
    }

    /** @draft ICU 78 */
    U16UnsafeReverseIterator<Unit16, CP32> rend() const {
        return {s.data()};
    }

private:
    std::basic_string_view<Unit16> s;
};

// ------------------------------------------------------------------------- ***

// TODO: UTF-8

// TODO: remove experimental sample code
#ifndef UTYPES_H
int32_t rangeLoop(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   for (auto units : range) {
       sum += units.codePoint();
   }
   return sum;
}

int32_t loopIterPlusPlus(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   auto iter = range.begin();
   auto limit = range.end();
   while (iter != limit) {
       sum += (*iter++).codePoint();
   }
   return sum;
}

int32_t backwardLoop(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   auto start = range.begin();
   auto iter = range.end();
   while (start != iter) {
       sum += (*--iter).codePoint();
   }
   return sum;
}

int32_t reverseLoop(std::u16string_view s) {
   header::U16StringCodePoints<char16_t, UChar32, U_BEHAVIOR_NEGATIVE> range(s);
   int32_t sum = 0;
   for (auto iter = range.rbegin(); iter != range.rend(); ++iter) {
       sum += (*iter).codePoint();
   }
   return sum;
}

int32_t unsafeRangeLoop(std::u16string_view s) {
   header::U16UnsafeStringCodePoints<char16_t, UChar32> range(s);
   int32_t sum = 0;
   for (auto units : range) {
       sum += units.codePoint();
   }
   return sum;
}

int32_t unsafeReverseLoop(std::u16string_view s) {
   header::U16UnsafeStringCodePoints<char16_t, UChar32> range(s);
   int32_t sum = 0;
   for (auto iter = range.rbegin(); iter != range.rend(); ++iter) {
       sum += (*iter).codePoint();
   }
   return sum;
}
#endif

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTF16CPPITER_H__
