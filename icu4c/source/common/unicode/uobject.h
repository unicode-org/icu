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

/**  U_OVERRIDE_CXX_ALLOCATION - Define this to override operator new and
 *                               delete in UMemory. Enabled by default for ICU.
 *
 *         Enabling forces all allocation of ICU object types to use ICU's
 *         memory allocation. On Windows, this allows the ICU DLL to be used by
 *         applications that statically link the C Runtime library, meaning that
 *         the app and ICU will be using different heaps.
 *
 * @draft ICU 2.2
 */                              
#ifndef U_OVERRIDE_CXX_ALLOCATION
#define U_OVERRIDE_CXX_ALLOCATION 1
#endif

/**  U_HAVE_PLACEMENT_NEW - Define this to define the placement new and
 *                          delete in UMemory for STL.
 *
 * @draft ICU 2.6
 */                              
#ifndef U_HAVE_PLACEMENT_NEW
#define U_HAVE_PLACEMENT_NEW 1
#endif

/**
 * UMemory is the common ICU base class.
 * All other ICU C++ classes are derived from UMemory (starting with ICU 2.4).
 *
 * This is primarily to make it possible and simple to override the
 * C++ memory management by adding new/delete operators to this base class.
 *
 * To override ALL ICU memory management, including that from plain C code,
 * replace the allocation functions declared in cmemory.h
 *
 * UMemory does not contain any virtual functions.
 * Common "boilerplate" functions are defined in UObject.
 *
 * @draft ICU 2.4
 */
class U_COMMON_API UMemory {
public:

#if U_OVERRIDE_CXX_ALLOCATION
    /**
     * Override for ICU4C C++ memory management.
     * simple, non-class types are allocated using the macros in common/cmemory.h
     * (uprv_malloc(), uprv_free(), uprv_realloc());
     * they or something else could be used here to implement C++ new/delete
     * for ICU4C C++ classes
     * @draft ICU 2.4
     */
    static void *operator new(size_t size);

    /**
     * Override for ICU4C C++ memory management.
     * See new().
     * @draft ICU 2.4
     */
    static void *operator new[](size_t size);

    /**
     * Override for ICU4C C++ memory management.
     * simple, non-class types are allocated using the macros in common/cmemory.h
     * (uprv_malloc(), uprv_free(), uprv_realloc());
     * they or something else could be used here to implement C++ new/delete
     * for ICU4C C++ classes
     * @draft ICU 2.4
     */
    static void operator delete(void *p);

    /**
     * Override for ICU4C C++ memory management.
     * See delete().
     * @draft ICU 2.4
     */
    static void operator delete[](void *p);

#if U_HAVE_PLACEMENT_NEW
    /**
     * Override for ICU4C C++ memory management for STL.
     * See new().
     * @draft ICU 2.6
     */
    static inline void * operator new(size_t, void *ptr) { return ptr; }

    /**
     * Override for ICU4C C++ memory management for STL.
     * See delete().
     * @draft ICU 2.6
     */
    static inline void operator delete(void *, void *) {}
#endif /* U_HAVE_PLACEMENT_NEW */
#endif /* U_OVERRIDE_CXX_ALLOCATION */
};

/**
 * UObject is the common ICU "boilerplate" class.
 * UObject inherits UMemory (starting with ICU 2.4),
 * and all other public ICU C++ classes
 * are derived from UObject (starting with ICU 2.2).
 *
 * UObject contains common virtual functions like for ICU's "poor man's RTTI".
 * It does not contain default implementations of virtual methods
 * like getDynamicClassID to allow derived classes such as Format
 * to declare these as pure virtual.
 *
 * @draft ICU 2.2
 */
class U_COMMON_API UObject : public UMemory {
public:
    /**
     * Destructor.
     *
     * @draft ICU 2.2
     */
    virtual inline ~UObject() {}

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

#if U_ICU_VERSION_MAJOR_NUM>2 || (U_ICU_VERSION_MAJOR_NUM==2 && U_ICU_VERSION_MINOR_NUM>6)
    // TODO post ICU 2.4  (This comment inserted in 2.2)
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
    // (i.e., subclasses would have to return UObject * as well, instead of SubClass *)
    // virtual UObject *clone() const;
#endif
};

U_NAMESPACE_END

#endif
