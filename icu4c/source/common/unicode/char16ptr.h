// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// char16ptr.h
// created: 2017feb28 Markus W. Scherer

#ifndef __CHAR16PTR_H__
#define __CHAR16PTR_H__

#include <cstddef>
#include "unicode/utypes.h"

/**
 * \file
 * \brief C++ API: char16_t pointer wrappers with
 *        implicit conversion to/from bit-compatible raw pointer types.
 */

U_NAMESPACE_BEGIN

/**
 * \def U_ALIASING_BARRIER
 * Barrier for pointer anti-aliasing optimizations even across function boundaries.
 * @internal
 */
#ifdef U_ALIASING_BARRIER
    // Use the predefined value.
#elif 0 // TODO: only some versions of clang?? defined(__clang__) || defined(__GNUC__)
#   define U_ALIASING_BARRIER(ptr) asm volatile("" : "+rm"(ptr))
#endif

/**
 * char16_t * wrapper with implicit conversion from/to distinct but bit-compatible pointer types.
 * @draft ICU 59
 */
class U_COMMON_API Char16Ptr final {
public:
    /**
     * Copies the pointer.
     * TODO: @param p ...
     * @draft ICU 59
     */
    inline Char16Ptr(char16_t *p);
    /**
     * Converts the pointer to char16_t *.
     * @draft ICU 59
     */
    inline Char16Ptr(uint16_t *p);
#if U_SIZEOF_WCHAR_T==2 || defined(U_IN_DOXYGEN)
    /**
     * Converts the pointer to char16_t *.
     * (Only defined if U_SIZEOF_WCHAR_T==2.)
     * @draft ICU 59
     */
    inline Char16Ptr(wchar_t *p);
#endif
    /**
     * nullptr constructor.
     * @draft ICU 59
     */
    inline Char16Ptr(std::nullptr_t p);
    /**
     * Destructor.
     * @draft ICU 59
     */
    inline ~Char16Ptr();

    /**
     * Pointer access.
     * TODO @return ...
     * @draft ICU 59
     */
    inline char16_t *get() const;
    /**
     * char16_t pointer access via type conversion (e.g., static_cast).
     * @draft ICU 59
     */
    operator char16_t *() const { return get(); }
    // TODO: do we need output conversion and other operator overloads
    // if we do not change return values to pointer wrappers?
    /**
     * uint16_t pointer access via type conversion (e.g., static_cast).
     * @draft ICU 59
     */
    inline operator uint16_t *() const;
#if U_SIZEOF_WCHAR_T==2 || defined(U_IN_DOXYGEN)
    /**
     * wchar_t pointer access via type conversion (e.g., static_cast).
     * @draft ICU 59
     */
    inline operator wchar_t *() const;
#endif
    operator void *() const { return get(); }

    char16_t operator[](std::ptrdiff_t offset) const { return get()[offset]; }

    UBool operator==(const Char16Ptr &other) const { return get() == other.get(); }
    UBool operator!=(const Char16Ptr &other) const { return !operator==(other); }
    UBool operator==(const char16_t *other) const { return get() == other; }
    UBool operator!=(const char16_t *other) const { return !operator==(other); }
    UBool operator==(const uint16_t *other) const { return static_cast<uint16_t *>(*this) == other; }
    UBool operator!=(const uint16_t *other) const { return !operator==(other); }
#if U_SIZEOF_WCHAR_T==2 || defined(U_IN_DOXYGEN)
    UBool operator==(const wchar_t *other) const { return static_cast<wchar_t *>(*this) == other; }
    UBool operator!=(const wchar_t *other) const { return !operator==(other); }
#endif
    UBool operator==(const std::nullptr_t null) const { return get() == null; }
    UBool operator!=(const std::nullptr_t null) const { return !operator==(null); }
    /**
     * Comparison with 0.
     * @return TRUE if the pointer is nullptr and zero==0
     * @draft ICU 59
     */
    UBool operator==(int zero) const { return get() == nullptr && zero == 0; }
    /**
     * Comparison with 0.
     * @return TRUE if the pointer is not nullptr and zero==0
     * @draft ICU 59
     */
    UBool operator!=(int zero) const { return get() != nullptr && zero == 0; }

    Char16Ptr operator+(std::ptrdiff_t offset) const { return Char16Ptr(get() + offset); }

private:
    Char16Ptr() = delete;

#ifdef U_ALIASING_BARRIER
    template<typename T> static char16_t *cast(T *t) {
        U_ALIASING_BARRIER(t);
        return reinterpret_cast<char16_t *>(t);
    }

    char16_t *p;
#else
    union {
        char16_t *cp;
        uint16_t *up;
        wchar_t *wp;
    } u;
#endif
};

#ifdef U_ALIASING_BARRIER

Char16Ptr::Char16Ptr(char16_t *p) : p(p) {}
Char16Ptr::Char16Ptr(uint16_t *p) : p(cast(p)) {}
#if U_SIZEOF_WCHAR_T==2
Char16Ptr::Char16Ptr(wchar_t *p) : p(cast(p)) {}
#endif
Char16Ptr::Char16Ptr(std::nullptr_t p) : p(p) {}
Char16Ptr::~Char16Ptr() {
    U_ALIASING_BARRIER(p);
}

char16_t *Char16Ptr::get() const { return p; }

Char16Ptr::operator uint16_t *() const {
    U_ALIASING_BARRIER(p);
    return reinterpret_cast<uint16_t *>(p);
}
#if U_SIZEOF_WCHAR_T==2
Char16Ptr::operator wchar_t *() const {
    U_ALIASING_BARRIER(p);
    return reinterpret_cast<wchar_t *>(p);
}
#endif

#else

Char16Ptr::Char16Ptr(char16_t *p) { u.cp = p; }
Char16Ptr::Char16Ptr(uint16_t *p) { u.up = p; }
#if U_SIZEOF_WCHAR_T==2
Char16Ptr::Char16Ptr(wchar_t *p) { u.wp = p; }
#endif
Char16Ptr::Char16Ptr(std::nullptr_t p) { u.cp = p; }
Char16Ptr::~Char16Ptr() {}

char16_t *Char16Ptr::get() const { return u.cp; }

Char16Ptr::operator uint16_t *() const {
    return u.up;
}
#if U_SIZEOF_WCHAR_T==2
Char16Ptr::operator wchar_t *() const {
    return u.wp;
}
#endif

#endif

/**
 * const char16_t * wrapper with implicit conversion from/to distinct but bit-compatible pointer types.
 * @draft ICU 59
 */
class U_COMMON_API ConstChar16Ptr final {
public:
    /**
     * Copies the pointer.
     * @draft ICU 59
     */
    inline ConstChar16Ptr(const char16_t *p);
    /**
     * Converts the pointer to char16_t *.
     * @draft ICU 59
     */
    inline ConstChar16Ptr(const uint16_t *p);
#if U_SIZEOF_WCHAR_T==2 || defined(U_IN_DOXYGEN)
    /**
     * Converts the pointer to char16_t *.
     * (Only defined if U_SIZEOF_WCHAR_T==2.)
     * @draft ICU 59
     */
    inline ConstChar16Ptr(const wchar_t *p);
#endif
    /**
     * nullptr constructor.
     * @draft ICU 59
     */
    inline ConstChar16Ptr(const std::nullptr_t p);
    /**
     * Destructor.
     * @draft ICU 59
     */
    inline ~ConstChar16Ptr();

    /**
     * Pointer access.
     * @draft ICU 59
     */
    inline const char16_t *get() const;
    /**
     * char16_t pointer access via type conversion (e.g., static_cast).
     * @draft ICU 59
     */
    operator const char16_t *() const { return get(); }
    /**
     * uint16_t pointer access via type conversion (e.g., static_cast).
     * @draft ICU 59
     */
    inline operator const uint16_t *() const;
#if U_SIZEOF_WCHAR_T==2 || defined(U_IN_DOXYGEN)
    /**
     * wchar_t pointer access via type conversion (e.g., static_cast).
     * @draft ICU 59
     */
    inline operator const wchar_t *() const;
#endif
    operator const void *() const { return get(); }

    char16_t operator[](std::ptrdiff_t offset) const { return get()[offset]; }

    UBool operator==(const ConstChar16Ptr &other) const { return get() == other.get(); }
    UBool operator!=(const ConstChar16Ptr &other) const { return !operator==(other); }
    UBool operator==(const char16_t *other) const { return get() == other; }
    UBool operator!=(const char16_t *other) const { return !operator==(other); }
    UBool operator==(const uint16_t *other) const { return static_cast<const uint16_t *>(*this) == other; }
    UBool operator!=(const uint16_t *other) const { return !operator==(other); }
#if U_SIZEOF_WCHAR_T==2 || defined(U_IN_DOXYGEN)
    UBool operator==(const wchar_t *other) const { return static_cast<const wchar_t *>(*this) == other; }
    UBool operator!=(const wchar_t *other) const { return !operator==(other); }
#endif
    UBool operator==(const std::nullptr_t null) const { return get() == null; }
    UBool operator!=(const std::nullptr_t null) const { return !operator==(null); }
    UBool operator==(int zero) const { return get() == nullptr && zero == 0; }
    UBool operator!=(int zero) const { return get() != nullptr && zero == 0; }

    ConstChar16Ptr operator+(std::ptrdiff_t offset) { return ConstChar16Ptr(get() + offset); }

private:
    ConstChar16Ptr() = delete;

#ifdef U_ALIASING_BARRIER
    template<typename T> static const char16_t *cast(const T *t) {
        U_ALIASING_BARRIER(t);
        return reinterpret_cast<const char16_t *>(t);
    }

    const char16_t *p;
#else
    union {
        const char16_t *cp;
        const uint16_t *up;
        const wchar_t *wp;
    } u;
#endif
};

#ifdef U_ALIASING_BARRIER

ConstChar16Ptr::ConstChar16Ptr(const char16_t *p) : p(p) {}
ConstChar16Ptr::ConstChar16Ptr(const uint16_t *p) : p(cast(p)) {}
#if U_SIZEOF_WCHAR_T==2
ConstChar16Ptr::ConstChar16Ptr(const wchar_t *p) : p(cast(p)) {}
#endif
ConstChar16Ptr::ConstChar16Ptr(const std::nullptr_t p) : p(p) {}
ConstChar16Ptr::~ConstChar16Ptr() {
    U_ALIASING_BARRIER(p);
}

const char16_t *ConstChar16Ptr::get() const { return p; }

ConstChar16Ptr::operator const uint16_t *() const {
    U_ALIASING_BARRIER(p);
    return reinterpret_cast<const uint16_t *>(p);
}
#if U_SIZEOF_WCHAR_T==2
ConstChar16Ptr::operator const wchar_t *() const {
    U_ALIASING_BARRIER(p);
    return reinterpret_cast<const wchar_t *>(p);
}
#endif

#else

ConstChar16Ptr::ConstChar16Ptr(const char16_t *p) { u.cp = p; }
ConstChar16Ptr::ConstChar16Ptr(const uint16_t *p) { u.up = p; }
#if U_SIZEOF_WCHAR_T==2
ConstChar16Ptr::ConstChar16Ptr(const wchar_t *p) { u.wp = p; }
#endif
ConstChar16Ptr::ConstChar16Ptr(const std::nullptr_t p) { u.cp = p; }
ConstChar16Ptr::~ConstChar16Ptr() {}

const char16_t *ConstChar16Ptr::get() const { return u.cp; }

ConstChar16Ptr::operator const uint16_t *() const {
    return u.up;
}
#if U_SIZEOF_WCHAR_T==2
ConstChar16Ptr::operator const wchar_t *() const {
    return u.wp;
}
#endif

#endif

U_NAMESPACE_END

#endif  // __CHAR16PTR_H__
