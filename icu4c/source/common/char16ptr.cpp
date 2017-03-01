// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// char16ptr.cpp
// created: 2017feb28 Markus W. Scherer

#include "unicode/utypes.h"
#include "unicode/char16ptr.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

#ifdef U_ALIASING_BARRIER

Char16Ptr::Char16Ptr(int null) : p(nullptr) {
    U_ASSERT(null == 0);
    if (null != 0) {
        // Try to provoke a crash.
        p = reinterpret_cast<char16_t *>(1);
    }
}

ConstChar16Ptr::ConstChar16Ptr(int null) : p(nullptr) {
    U_ASSERT(null == 0);
    if (null != 0) {
        // Try to provoke a crash.
        p = reinterpret_cast<char16_t *>(1);
    }
}

#else

Char16Ptr::Char16Ptr(int null) {
    U_ASSERT(null == 0);
    if (null == 0) {
        u.cp = nullptr;
    } else {
        // Try to provoke a crash.
        u.cp = reinterpret_cast<char16_t *>(1);
    }
}

ConstChar16Ptr::ConstChar16Ptr(int null) {
    U_ASSERT(null == 0);
    if (null == 0) {
        u.cp = nullptr;
    } else {
        // Try to provoke a crash.
        u.cp = reinterpret_cast<char16_t *>(1);
    }
}

#endif

U_NAMESPACE_END
