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

#ifndef U_IN_DOXYGEN
// @internal
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior, typename = void>
class UTFImpl;

// UTF-16
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class UTFImpl<
        UnitIter,
        CP32,
        behavior,
        std::enable_if_t<
            sizeof(typename std::iterator_traits<UnitIter>::value_type) == 2>> {
public:
    static void inc(UnitIter &p, UnitIter limit) {
        // TODO: assert p != limit -- more precisely: start <= p < limit
        // Very similar to U16_FWD_1().
        auto c = *p;
        ++p;
        if (U16_IS_LEAD(c) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
        }
    }

    static CodeUnits<UnitIter, CP32> readAndInc(UnitIter &p, UnitIter limit) {
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

    static CodeUnits<UnitIter, CP32> singlePassReadAndInc(UnitIter &p, UnitIter limit) {
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

    static CodeUnits<UnitIter, CP32> decAndRead(UnitIter start, UnitIter &p) {
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

    static void moveToReadAndIncStart(UnitIter &p, int8_t &state) {
        // state > 0 after readAndInc(); max 2 for UTF-16
        --p;
        if (--state != 0) {
            --p;
            state = 0;
        }
    }

    static void moveToDecAndReadLimit(UnitIter &p, int8_t &state) {
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
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior, typename = void>
class U16Iterator {
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
    // TODO: Should these Iterators define value_type etc.?
    //       What about iterator_category depending on the UnitIter??

    // TODO: Maybe std::move() the UnitIters?
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.
    U16Iterator(UnitIter start, UnitIter p, UnitIter limit) :
            p_(p), start_(start), limit_(limit), units_(0, 0, false, p) {}
    // TODO: add constructor with just start-or-p and limit: start=p
    // Constructs an iterator start or limit sentinel.
    U16Iterator(UnitIter p) : p_(p), start_(p), limit_(p), units_(0, 0, false, p) {}

    U16Iterator(const U16Iterator &other) = default;
    U16Iterator &operator=(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const {
        // Compare logical positions.
        UnitIter p1 = state_ <= 0 ? p_ : units_.data();
        UnitIter p2 = other.state_ <= 0 ? other.p_ : other.units_.data();
        return p1 == p2;
    }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    CodeUnits<UnitIter, CP32> operator*() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            state_ = units_.length();
        }
        return units_;
    }

    Proxy operator->() const {
        if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            state_ = units_.length();
        }
        return Proxy(units_);
    }

    U16Iterator &operator++() {  // pre-increment
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

    U16Iterator operator++(int) {  // post-increment
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is already ahead.
            U16Iterator result(*this);
            state_ = 0;
            return result;
        } else if (state_ == 0) {
            units_ = Impl::readAndInc(p_, limit_);
            U16Iterator result(*this);
            result.state_ = units_.length();
            // keep this->state_ == 0
            return result;
        } else /* state_ < 0 */ {
            U16Iterator result(*this);
            // operator--() called decAndRead() so we know how far to skip.
            Impl::moveToDecAndReadLimit(p_, state_);
            return result;
        }
    }

    U16Iterator &operator--() {  // pre-decrement
        if (state_ > 0) {
            // operator*() called readAndInc() so p_ is ahead of the logical position.
            Impl::moveToReadAndIncStart(p_, state_);
        }
        units_ = Impl::decAndRead(start_, p_);
        state_ = -units_.length();
        return *this;
    }

    U16Iterator operator--(int) {  // post-decrement
        U16Iterator result(*this);
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
class U16Iterator<
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
    // TODO: Should these Iterators define value_type etc.?
    //       What about iterator_category depending on the UnitIter??

    // TODO: Does it make sense for the limits to allow having a different type?
    // We only need to be able to compare p_ vs. limit_ for == and !=.
    // Might allow interesting sentinel types.
    // Would be trouble for the sentinel constructor that inits both iters from the same p.

    U16Iterator(UnitIter p, UnitIter limit) : p_(p), limit_(limit) {}
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    // Constructs an iterator start or limit sentinel.
    U16Iterator(UnitIter p) : p_(p), limit_(p) {}

    U16Iterator(const U16Iterator &other) = default;
    U16Iterator &operator=(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const {
        return p_ == other.p_ && ahead_ == other.ahead_;
        // Strictly speaking, we should check if the logical position is the same.
        // However, we cannot move, or do arithmetic with, a single-pass UnitIter.
    }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    CodeUnits<UnitIter, CP32> operator*() const {
        if (!ahead_) {
            units_ = Impl::singlePassReadAndInc(p_, limit_);
            ahead_ = true;
        }
        return units_;
    }

    Proxy operator->() const {
        if (!ahead_) {
            units_ = Impl::singlePassReadAndInc(p_, limit_);
            ahead_ = true;
        }
        return Proxy(units_);
    }

    U16Iterator &operator++() {  // pre-increment
        if (ahead_) {
            // operator*() called readAndInc() so p_ is already ahead.
            ahead_ = false;
        } else {
            Impl::inc(p_, limit_);
        }
        return *this;
    }

    Proxy operator++(int) {  // post-increment
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
 *     UTF-16: char16_t or uint16_t or (on Windows) wchar_t
 * @tparam CP32 Code point type: UChar32 (=int32_t) or char32_t or uint32_t;
 *              should be signed if U_BEHAVIOR_NEGATIVE
 * @tparam UIllFormedBehavior TODO
 * @draft ICU 78
 */
template<typename UnitIter, typename CP32, UIllFormedBehavior behavior>
class U16ReverseIterator {
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
    U16ReverseIterator(UnitIter start, UnitIter p) : p_(p), start_(start) {}
    // Constructs an iterator start or limit sentinel.
    U16ReverseIterator(UnitIter p) : p_(p), start_(p) {}

    U16ReverseIterator(const U16ReverseIterator &other) = default;
    U16ReverseIterator &operator=(const U16ReverseIterator &other) = default;

    bool operator==(const U16ReverseIterator &other) const { return p_ == other.p_; }
    bool operator!=(const U16ReverseIterator &other) const { return !operator==(other); }

    CodeUnits<UnitIter, CP32> operator*() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnitIter p = p_;
        return Impl::decAndRead(start_, p);
    }

    Proxy operator->() const {
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        UnitIter p = p_;
        return Proxy(Impl::decAndRead(start_, p));
    }

    U16ReverseIterator &operator++() {  // pre-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        Impl::decAndRead(start_, p_);
        return *this;
    }

    U16ReverseIterator operator++(int) {  // post-increment
        // Call the same function in both operator*() and operator++() so that an
        // optimizing compiler can easily eliminate redundant work when alternating between the two.
        U16ReverseIterator result(*this);
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
        return {s.data(), s.data() + s.length()};
    }

    /** @draft ICU 78 */
    U16ReverseIterator<const Unit16 *, CP32, behavior> rend() const {
        return {s.data(), s.data()};
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
