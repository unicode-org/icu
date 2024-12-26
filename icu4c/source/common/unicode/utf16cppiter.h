// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utf16cppiter.h
// created: 2024aug12 Markus W. Scherer

#ifndef __UTF16CPPITER_H__
#define __UTF16CPPITER_H__

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API

#include <string_view>
#include "unicode/utf16.h"
#include "unicode/uversion.h"

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
// The operator*() return value might then want to be a template parameter as well.
// For example, for a well-formed sequence, the return value could be
// a tuple of (code point, well-formed), or a string view, or...
// (And then the caller could choose between UChar32 and char32_t.)
// However, all of that would make the API more complex and daunting.
enum U16IllFormedBehavior {
    U16_BEHAVIOR_NEGATIVE,
    U16_BEHAVIOR_FFFD,
    U16_BEHAVIOR_SURROGATE
};

// TODO: Consider a template parameter for UChar32 vs. char32_t vs. uint32_t.

/**
 * A code unit sequence for one code point returned by U16Iterator.
 *
 * TODO: check doxygen syntax for template parameters
 * @param Unit16 char16_t or uint16_t or (on Windows) wchar_t
 * @draft ICU 77
 */
template<typename Unit16>
struct U16OneSeq {
    // Order of fields with padding and access frequency in mind.
    UChar32 codePoint = 0;
    uint8_t length = 0;
    bool isWellFormed = false;
    const Unit16 *data;

    std::basic_string_view<Unit16> stringView() const {
        return std::basic_string_view<Unit16>(data, length);
    }

    // TODO: std::optional<UChar32> maybeCodePoint() const ? (nullopt if !isWellFormed)
};

/**
 * Validating iterator over the code points in a Unicode 16-bit string.
 *
 * TODO: check doxygen syntax for template parameters
 * @param Unit16 char16_t or uint16_t or (on Windows) wchar_t
 * @param U16IllFormedBehavior TODO
 * @draft ICU 77
 */
template<typename Unit16, U16IllFormedBehavior behavior>
class U16Iterator {
public:
    // TODO: make private, make friends
    U16Iterator(const Unit16 *start, const Unit16 *p, const Unit16 *limit) :
            start(start), p(p), limit(limit) {}
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    U16Iterator(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const { return p == other.p; }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    const U16OneSeq<Unit16> operator*() const {
        // TODO: assert p != limit -- more precisely: start <= p < limit
        // Similar to U16_NEXT_OR_FFFD().
        UChar32 c = *p;
        if (!U16_IS_SURROGATE(c)) {
            return {c, 1, true, p};
        } else {
            uint16_t c2;
            if (U16_IS_SURROGATE_LEAD(c) && (p + 1) != limit && U16_IS_TRAIL(c2 = p[1])) {
                c = U16_GET_SUPPLEMENTARY(c, c2);
                return {c, 2, true, p};
            } else {
                // TODO: U16IllFormedBehavior
                return {0xfffd, 1, false, p};
            }
        }
    }

    U16Iterator &operator++() {  // pre-increment
        // TODO: assert p != limit -- more precisely: start <= p < limit
        // Similar to U16_FWD_1().
        if (U16_IS_LEAD(*p++) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
        }
        return *this;
    }

    U16Iterator operator++(int) {  // post-increment
        // TODO: assert p != limit -- more precisely: start <= p < limit
        U16Iterator result(*this);
        // More similar to U16_NEXT_OR_FFFD() than U16_FWD_1() to try to help the compiler
        // amortize work between operator*() and operator++(int) in typical *it++ usage.
        // Otherwise this is slightly less efficient because it tests a lead surrogate twice.
        UChar32 c = *p++;
        if (U16_IS_SURROGATE(c) &&
                U16_IS_SURROGATE_LEAD(c) && p != limit && U16_IS_TRAIL(*p)) {
            ++p;
        }
        return result;
    }

private:
    // In a validating iterator, we need start & limit so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    const Unit16 *const start;
    const Unit16 *p;
    const Unit16 *const limit;
};

// ------------------------------------------------------------------------- ***

// TODO: Non-validating iterator over the code points in a Unicode 16-bit string.
// Assumes well-formed UTF-16. Otherwise the behavior is undefined.
// template<typename Unit16>
// class U16UnsafeIterator
// TODO: only p, no start, no limit
// TODO: can/should we read the code point only in operator*()?
// if we read it in the constructor, then we would still need start/limit...

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTF16CPPITER_H__
