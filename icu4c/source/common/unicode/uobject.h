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

/* TODO undefine this symbol except for tests! See uobject.cpp */
// #define U_CPP_MEMORY_TEST
#define U_OVERRIDE_CXX_ALLOCATION 1

/**
 * UObject is the common ICU base class.
 * All other ICU C++ classes are derived from UObject (starting with ICU 2.2).
 *
 * This is primarily to make it possible and simple to override the
 * C++ memory management by adding new/delete operators to this base class.
 * ICU itself will not declare and implement these new/delete operators, but
 * users of ICU can modify the ICU code for the base class.
 *
 * UObject contains pure virtual methods to allow derived classes like Format
 * (which used to be base classes themselves before UObject was introduced)
 * to have pure virtual methods.
 *
 * It is likely that a future ICU release will split UObject to
 * separate out a new "more base" class for only the
 * memory management customization, with UObject subclassing that new
 * class and adding virtual methods for "boilerplate" functions.
 * This will simplify the maintenance of ICU.
 *
 * @draft ICU 2.2
 */
class U_COMMON_API UObject {
public:
    /**
     * Destructor.
     *
     * @draft ICU 2.2
     */
    virtual inline ~UObject() {}


#ifdef U_OVERRIDE_CXX_ALLOCATION
    /**
     * Overrides for ICU4C C++ memory management.
     * simple, non-class types are allocated using the macros in common/cmemory.h
     * (uprv_malloc(), uprv_free(), uprv_realloc());
     * they or something else could be used here to implement C++ new/delete
     * for ICU4C C++ classes
     * @draft ICU 2.2
     */
    void *operator new(size_t size);
    /**
     * Overrides for ICU4C C++ memory management.
     * simple, non-class types are allocated using the macros in common/cmemory.h
     * (uprv_malloc(), uprv_free(), uprv_realloc());
     * they or something else could be used here to implement C++ new/delete
     * for ICU4C C++ classes
     * @draft ICU 2.2
     */
    void operator delete(void *p);
#if U_CXX_MEMORY_TEST
    /* Sun Forte really dislikes these functions. */
    void *operator new[](size_t size);
    void operator delete[](void *p);
    void operator delete(void *p, size_t size);
    void operator delete[](void *p, size_t size);
#endif
#endif

    /**
     * ICU4C "poor man's RTTI", returns a UClassID for the actual ICU class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const = 0;

protected:
    // the following functions are protected to prevent instantiation and
    // direct use of UObject itself

    // default constructor
    // commented out because UObject is abstract (see getDynamicClassID)
    // inline UObject() {}

    // copy constructor
    // commented out because UObject is abstract (see getDynamicClassID)
    // inline UObject(const UObject &other) {}

#if U_ICU_VERSION_MAJOR_NUM>2 || (U_ICU_VERSION_MAJOR_NUM==2 && U_ICU_VERSION_MINOR_NUM>2)
    // TODO post ICU 2.2
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
#endif
};

U_NAMESPACE_END

#endif
