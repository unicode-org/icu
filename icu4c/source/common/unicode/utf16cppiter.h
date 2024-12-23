// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// utf16cppiter.h
// created: 2024aug12 Markus W. Scherer

#ifndef __UTF16CPPITER_H__
#define __UTF16CPPITER_H__

#include <string_view>

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API

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

/**
 * A code unit sequence for one code point returned by U16Iterator.
 *
 * TODO: check doxygen syntax for template parameters
 * @param Unit16 char16_t or uint16_t or (on Windows) wchar_t
 * @draft ICU 77
 */
template<typename Unit16>
class U16OneSeq {
public:
    U16OneSeq(const U16OneSeq &other) = default;

    const Unit16 *data() { return p; }
    int32_t length() const { return len; }

    std::basic_string_view<Unit16> stringView() const {
        return std::basic_string_view<Unit16>(p, len);
    }

    bool isWellFormed() const { return ok; }

    UChar32 codePoint() const { return c; }

    // TODO: std::optional<UChar32> maybeCodePoint() const ? (nullopt if !ok)

private:
    // TODO: Why can't we just use Unit16 here?
    // error: declaration of 'Unit16' shadows template parameter
    template<typename SomeOtherUnit16, U16IllFormedBehavior behavior>
    friend class U16Iterator;

    U16OneSeq(const Unit16 *p) : p(p) {}

    void fwd1() { p += len; }

    void readOneForward(const Unit16 *limit) {
        if (p == limit) {
            len = 0;
            return;
        }
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

    const Unit16 *p;
    UChar32 c = 0;
    int8_t len = 0;
    bool ok = false;
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
            start(start), limit(limit), seq(p) {
        seq.readOneForward(limit);
    }
    // TODO: We might try to support limit==nullptr, similar to U16_ macros supporting length<0.
    // Test pointers for == or != but not < or >.

    U16Iterator(const U16Iterator &other) = default;

    bool operator==(const U16Iterator &other) const { return seq.p == other.seq.p; }
    bool operator!=(const U16Iterator &other) const { return !operator==(other); }

    const U16OneSeq<Unit16> &operator*() const {
        return seq;
    }

    U16Iterator &operator++() {  // pre-increment
        // TODO: think about switching directions etc.
        // Assume that readOneForward() was called and set seq.len.
        // Skip the current code point, then read the next one.
        seq.fwd1();
        seq.readOneForward(limit);
        return *this;
    }

    U16Iterator operator++(int) {  // post-increment
        U16Iterator result(*this);
        // TODO: think about switching directions etc.
        // Assume that readOneForward() was called and set seq.len.
        // Skip the current code point, then read the next one.
        seq.fwd1();
        seq.readOneForward(limit);
        return result;
    }

private:
    // In a validating iterator, we need start & limit so that when we read a code point
    // (forward or backward) we can test if there are enough code units.
    const Unit16 *start;
    const Unit16 *limit;
    U16OneSeq<Unit16> seq;
};

// ------------------------------------------------------------------------- ***

// TODO: Non-validating iterator over the code points in a Unicode 16-bit string.
// Assumes well-formed UTF-16. Otherwise the behavior is undefined.
// TODO: all @draft ICU 77
// template<typename Unit16>
// class U16UnsafeIterator
// TODO: only p, no start, no limit
// TODO: can/should we read the code point only in operator*()?
// if we read it in the constructor, then we would still need start/limit...

}  // namespace U_HEADER_ONLY_NAMESPACE

#endif  // U_HIDE_DRAFT_API
#endif  // U_SHOW_CPLUSPLUS_API || U_SHOW_CPLUSPLUS_HEADER_API
#endif  // __UTF16CPPITER_H__
