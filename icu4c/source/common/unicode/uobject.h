/*
******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  uobject.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002jun26
*   created by: Markus W. Scherer
*/

#ifndef __UOBJECT_H__
#define __UOBJECT_H__

#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

/**
 * \file
 * \brief C++ API: Common ICU base class UObject.
 */

/**
 * UObject is the common ICU base class.
 * All other ICU C++ classes are derived from UObject (starting with ICU 2.2).
 *
 * This is primarily to make it possible and simple to override the
 * C++ memory management by adding new/delete operators to this base class.
 * ICU itself will not declare and implement these new/delete operators, but
 * users of ICU can modify the ICU code for the base class.
 */
class U_COMMON_API UObject {
public:
    // destructor
    virtual inline ~UObject() {}

    // possible overrides for ICU4C C++ memory management,
    // not provided by ICU itself;
    // simple, non-class types are allocated using the macros in common/cmemory.h
    // (uprv_malloc(), uprv_free(), uprv_realloc());
    // they or something else could be used here to implement C++ new/delete
    // for ICU4C C++ classes
    // void *operator new(size_t size);
    // void *operator new[](size_t size);
    // void operator delete(void *p, size_t size);
    // void operator delete[](void *p, size_t size);

    // ICU4C "poor man's RTTI"
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }
    virtual inline UClassID getDynamicClassID() { return getStaticClassID(); }

protected:
    // the following functions are protected to prevent instantiation and
    // direct use of UObject itself

    // default constructor
    inline UObject() {}

    // copy constructor
    inline UObject(const UObject &other) {}

    // some or all of the following "boilerplate" functions may be made public
    // in a future ICU4C release when all subclasses implement them

    // assignment operator
    // (not virtual, see "Taligent's Guide to Designing Programs" pp.73..74)
    // commented out because the implementation is the same as a compiler's default
    // UObject &operator=(const UObject &other) { return *this; }

    // comparison operators
    virtual inline UBool operator==(const UObject &other) const { return this==&other; }
    inline UBool operator!=(const UObject &other) const { return !operator==(other); }

    // clone() commented out from the base class:
    // some compilers do not support co-variant return types
    // (i.e., subclasses would have to return UObject& as well, instead of SubClass&)
    // virtual UObject *clone() const;

private:
    // for ICU4C "poor man's RTTI"
    static const char fgClassID;
};

U_NAMESPACE_END

#endif
