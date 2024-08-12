// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utf16cppiter.h
// created: 2024aug12 Markus W. Scherer

#ifndef __UTF16CPPITER_H__
#define __UTF16CPPITER_H__

#include <string_view>

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API

#include "unicode/utf16.h"
#include "unicode/uversion.h"

/**
 * \file
 * \brief C++ API: C++ iterators over Unicode 16-bit strings (=UTF-16 if well-formed).
 */

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

// Validating iterator over the code points in a Unicode 16-bit string.
// TODO: all @draft ICU 76
template<typename Unit16, U16IllFormedBehavior behavior>
class U16Iterator {
public:
    // TODO: make private, make friends
    U16Iterator(const Unit16 *start, const Unit16 *p, const Unit16 *limit) :
            start(start), p(p), limit(limit) {
        if (p != limit) {
            readOneForward();
        }
    }
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    U16Iterator(const U16Iterator &other) = default;
    U16Iterator(U16Iterator &&other) noexcept = default;

    bool operator==(const U16Iterator &other) const { return p == other.p; }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    UChar32 operator*() const {
        return c;
    }

    // TODO: good function names?
    // It would be nice to avoid a prefix like "current", "one", "cp",
    // but just length() on the iterator could be confusing.
    int32_t currentLength() const { return len; }

    std::basic_string_view<Unit16> currentView() const {
        return std::basic_string_view<Unit16>(p, len);
    }

    bool currentIsWellFormed() const { return ok; }

    U16Iterator &operator++() {  // pre-increment
        // TODO: think about switching directions etc.
        // Assume that readOneForward() was called and set `len`.
        // Skip the current code point, then read the next one.
        p += len;
        if (p != limit) {
            readOneForward();
        }
        return *this;
    }

    U16Iterator operator++(int) {  // post-increment
        U16Iterator result(*this);
        // TODO: think about switching directions etc.
        // Assume that readOneForward() was called and set `len`.
        // Skip the current code point, then read the next one.
        p += len;
        if (p != limit) {
            readOneForward();
        }
        return result;
    }

private:
    void readOneForward() {
        // see U16_NEXT_OR_FFFD()
        c = *p;
        len = 1;
        ok = true;
        if (U16_IS_SURROGATE(c)) {
            uint16_t c2;
            if (U16_IS_SURROGATE_LEAD(c) && (p + 1) != limit && U16_IS_TRAIL(c2 = p[1])) {
                c = U16_GET_SUPPLEMENTARY(c, c2);
                len = 2;
            } else {
                // TODO: U16IllFormedBehavior
                c = 0xfffd;
                ok = false;
            }
        }
    }

    // In a validating iterator, we need start & limit so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    const Unit16 *start;
    const Unit16 *p;
    const Unit16 *limit;
    UChar32 c = 0;
    int8_t len = 0;
    bool ok = false;
};

// ------------------------------------------------------------------------- ***

// TODO: Non-validating iterator over the code points in a Unicode 16-bit string.
// Assumes well-formed UTF-16. Otherwise the behavior is undefined.
// TODO: all @draft ICU 76
// template<typename Unit16>
// class U16UnsafeIterator
// TODO: only p, no start, no limit
// TODO: can/should we read the code point only in operator*()?
// if we read it in the constructor, then we would still need start/limit...

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_SHOW_CPLUSPLUS_API
#endif  // __UTF16CPPITER_H__
