/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uiter.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002jan18
*   created by: Markus W. Scherer
*/

#ifndef __UITER_H__
#define __UITER_H__

/**
 * \file
 * \brief C API: Unicode Character Iteration
 *
 * @see UCharIterator
 */

#include "unicode/utypes.h"

#ifdef XP_CPLUSPLUS
    U_NAMESPACE_BEGIN

    class CharacterIterator;
    class Replaceable;

    U_NAMESPACE_END
#endif

U_CDECL_BEGIN

struct UCharIterator;
typedef struct UCharIterator UCharIterator;

/**
 * Origin constants for UCharIterator.getIndex() and UCharIterator.move().
 * @see UCharIteratorMove
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef enum UCharIteratorOrigin {
    UITER_START, UITER_CURRENT, UITER_LIMIT, UITER_ZERO, UITER_LENGTH
} UCharIteratorOrigin;

/**
 * Function type declaration for UCharIterator.getIndex().
 *
 * Gets the current position, or the start or limit of the
 * iteration range.
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @param origin get the 0, start, limit, length, or current index
 * @return the requested index, or -1 in an error condition
 *
 * @see UCharIteratorOrigin
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef int32_t U_CALLCONV
UCharIteratorGetIndex(UCharIterator *iter, UCharIteratorOrigin origin);

/**
 * Function type declaration for UCharIterator.move().
 *
 * Use iter->move(iter, index, UITER_ZERO) like CharacterIterator::setIndex(index).
 *
 * Moves the current position relative to the start or limit of the
 * iteration range, or relative to the current position itself.
 * The movement is expressed in numbers of code units forward
 * or backward by specifying a positive or negative delta.
 * Out of bounds movement will be pinned to the start or limit.
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @param delta can be positive, zero, or negative
 * @param origin move relative to the 0, start, limit, length, or current index
 * @return the new index, or -1 on an error condition.
 *
 * @see UCharIteratorOrigin
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef int32_t U_CALLCONV
UCharIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin);

/**
 * Function type declaration for UCharIterator.hasNext().
 *
 * Check if current() and next() can still
 * return another code unit.
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return boolean value for whether current() and next() can still return another code unit
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef UBool U_CALLCONV
UCharIteratorHasNext(UCharIterator *iter);

/**
 * Function type declaration for UCharIterator.hasPrevious().
 *
 * Check if previous() can still return another code unit.
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return boolean value for whether previous() can still return another code unit
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef UBool U_CALLCONV
UCharIteratorHasPrevious(UCharIterator *iter);
 
/**
 * Function type declaration for UCharIterator.current().
 *
 * Return the code unit at the current position,
 * or -1 if there is none (index is at the limit).
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return the current code unit
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef int32_t U_CALLCONV
UCharIteratorCurrent(UCharIterator *iter);

/**
 * Function type declaration for UCharIterator.next().
 *
 * Return the code unit at the current index and increment
 * the index (post-increment, like s[i++]),
 * or return -1 if there is none (index is at the limit).
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return the current code unit (and post-increment the current index)
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef int32_t U_CALLCONV
UCharIteratorNext(UCharIterator *iter);

/**
 * Function type declaration for UCharIterator.previous().
 *
 * Decrement the index and return the code unit from there
 * (pre-decrement, like s[--i]),
 * or return -1 if there is none (index is at the start).
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return the previous code unit (after pre-decrementing the current index)
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef int32_t U_CALLCONV
UCharIteratorPrevious(UCharIterator *iter);

/**
 * Function type declaration for UCharIterator.reservedFn().
 * Reserved for future use.
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @param something some integer argument
 * @return some integer
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
typedef int32_t U_CALLCONV
UCharIteratorReserved(UCharIterator *iter, int32_t something);


/**
 * C API for code unit iteration.
 * This can be used as a C wrapper around
 * CharacterIterator, Replaceable, or implemented using simple strings, etc.
 *
 * There are two roles for using UCharIterator:
 *
 * A "provider" sets the necessary function pointers and controls the "protected"
 * fields of the UCharIterator structure. A "provider" passes a UCharIterator
 * into C APIs that need a UCharIterator as an abstract, flexible string interface.
 *
 * Implementations of such C APIs are "callers" of UCharIterator functions;
 * they only use the "public" function pointers and never access the "protected"
 * fields directly.
 *
 * UCharIterator functions return code unit values 0..0xffff,
 * or -1 if the iteration bounds are reached.
 * Therefore, the return type is int32_t.
 *
 * @draft ICU 2.1
 */
struct UCharIterator {
    /**
     * (protected) Pointer to string or wrapped object or similar.
     * Not used by caller.
     */
    const void *context;

    /**
     * (protected) Length of string or similar.
     * Not used by caller.
     */
    int32_t length;

    /**
     * (protected) Start index or similar.
     * Not used by caller.
     */
    int32_t start;

    /**
     * (protected) Current index or similar.
     * Not used by caller.
     */
    int32_t index;

    /**
     * (protected) Limit index or similar.
     * Not used by caller.
     */
    int32_t limit;

    /**
     * (protected) Not currently used by any instance.
     */
    int32_t reservedField;

    /**
     * (public) Returns the current position or the
     * start or limit index of the iteration range.
     *
     * @see UCharIteratorGetIndex
     */
    UCharIteratorGetIndex *getIndex;

    /**
     * (public) Moves the current position relative to the start or limit of the
     * iteration range, or relative to the current position itself.
     * The movement is expressed in numbers of code units forward
     * or backward by specifying a positive or negative delta.
     *
     * @see UCharIteratorMove
     */
    UCharIteratorMove *move;

    /**
     * (public) Check if current() and next() can still
     * return another code unit.
     *
     * @see UCharIteratorHasNext
     */
    UCharIteratorHasNext *hasNext;

    /**
     * (public) Check if previous() can still return another code unit.
     *
     * @see UCharIteratorHasPrevious
     */
    UCharIteratorHasPrevious *hasPrevious;

    /**
     * (public) Return the code unit at the current position,
     * or -1 if there is none (index is at the limit).
     *
     * @see UCharIteratorCurrent
     */
    UCharIteratorCurrent *current;

    /**
     * (public) Return the code unit at the current index and increment
     * the index (post-increment, like s[i++]),
     * or return -1 if there is none (index is at the limit).
     *
     * @see UCharIteratorNext
     */
    UCharIteratorNext *next;

    /**
     * (public) Decrement the index and return the code unit from there
     * (pre-decrement, like s[--i]),
     * or return -1 if there is none (index is at the start).
     *
     * @see UCharIteratorPrevious
     */
    UCharIteratorPrevious *previous;

    /**
     * (public) Reserved for future use. Currently NULL.
     *
     * @see UCharIteratorReserved
     */
    UCharIteratorReserved *reservedFn;
};

/**
 * Helper function for UCharIterator to get the code point
 * at the current index.
 *
 * Return the code point that includes the code unit at the current position,
 * or -1 if there is none (index is at the limit).
 * If the current code unit is a lead or trail surrogate,
 * then the following or preceding surrogate is used to form
 * the code point value.
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return the current code point
 *
 * @see UCharIterator
 * @see UTF_GET_CHAR
 * @see UnicodeString::char32At()
 * @draft ICU 2.1
 */
U_CAPI int32_t U_EXPORT2
uiter_current32(UCharIterator *iter);

/**
 * Helper function for UCharIterator to get the next code point.
 *
 * Return the code point at the current index and increment
 * the index (post-increment, like s[i++]),
 * or return -1 if there is none (index is at the limit).
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return the current code point (and post-increment the current index)
 *
 * @see UCharIterator
 * @see UTF_NEXT_CHAR
 * @draft ICU 2.1
 */
U_CAPI int32_t U_EXPORT2
uiter_next32(UCharIterator *iter);

/**
 * Helper function for UCharIterator to get the previous code point.
 *
 * Decrement the index and return the code point from there
 * (pre-decrement, like s[--i]),
 * or return -1 if there is none (index is at the start).
 *
 * @param iter the UCharIterator structure ("this pointer")
 * @return the previous code point (after pre-decrementing the current index)
 *
 * @see UCharIterator
 * @see UTF_PREV_CHAR
 * @draft ICU 2.1
 */
U_CAPI int32_t U_EXPORT2
uiter_previous32(UCharIterator *iter);

/**
 * Set up a UCharIterator to iterate over a string.
 *
 * Sets the UCharIterator function pointers for iteration over the string s
 * with iteration boundaries start=index=0 and length=limit=string length.
 * The "provider" may set the start, index, and limit values at any time
 * within the range 0..length.
 * The length field will be ignored.
 *
 * The string pointer s is set into UCharIterator.context without copying
 * or reallocating the string contents.
 *
 * @param iter UCharIterator structure to be set for iteration
 * @param s String to iterate over
 * @param length Length of s, or -1 if NUL-terminated
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
U_CAPI void U_EXPORT2
uiter_setString(UCharIterator *iter, const UChar *s, int32_t length);

#ifdef XP_CPLUSPLUS

/**
 * Set up a UCharIterator to wrap around a C++ CharacterIterator.
 *
 * Sets the UCharIterator function pointers for iteration using the
 * CharacterIterator charIter.
 *
 * The CharacterIterator pointer charIter is set into UCharIterator.context
 * without copying or cloning the CharacterIterator object.
 * The other "protected" UCharIterator fields are set to 0 and will be ignored.
 * The iteration index and boundaries are controlled by the CharacterIterator.
 *
 * @param iter UCharIterator structure to be set for iteration
 * @param charIter CharacterIterator to wrap
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
U_CAPI void U_EXPORT2
uiter_setCharacterIterator(UCharIterator *iter, CharacterIterator *charIter);

/**
 * Set up a UCharIterator to iterate over a C++ Replaceable.
 *
 * Sets the UCharIterator function pointers for iteration over the
 * Replaceable rep with iteration boundaries start=index=0 and
 * length=limit=rep->length().
 * The "provider" may set the start, index, and limit values at any time
 * within the range 0..length=rep->length().
 * The length field will be ignored.
 *
 * The Replaceable pointer rep is set into UCharIterator.context without copying
 * or cloning/reallocating the Replaceable object.
 *
 * @param iter UCharIterator structure to be set for iteration
 * @param rep Replaceable to iterate over
 *
 * @see UCharIterator
 * @draft ICU 2.1
 */
U_CAPI void U_EXPORT2
uiter_setReplaceable(UCharIterator *iter, const Replaceable *rep);

#endif

U_CDECL_END

#endif
