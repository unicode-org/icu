/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uset.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002mar07
*   created by: Markus W. Scherer
*
*   C version of UnicodeSet.
*/

#ifndef __USET_H__
#define __USET_H__

#include "unicode/utypes.h"

struct USet;
typedef struct USet USet;

enum {
    USET_SERIALIZED_STATIC_ARRAY_CAPACITY=8     /**< enough for any single-code point set */
};

/**
 * A serialized form of a Unicode set.  Limited manipulations are
 * possible directly on a serialized set.
 */
struct USerializedSet {
    const uint16_t *array;
    int32_t bmpLength, length;
    uint16_t staticArray[USET_SERIALIZED_STATIC_ARRAY_CAPACITY];
};
typedef struct USerializedSet USerializedSet;

/**
 * Creates a USet object that contains the range of characters
 * start..end, inclusive.
 * @param start first character of the range, inclusive
 * @param end last character of the range, inclusive
 * @return a newly created USet.  The caller must call uset_close() on
 * it when done.
 */
U_CAPI USet * U_EXPORT2
uset_open(UChar32 start, UChar32 end);

/**
 * Disposes of the storage used by a USet object.  This function should
 * be called exactly once for objects returned by uset_open().
 * @param set the object to dispose of
 */
U_CAPI void U_EXPORT2
uset_close(USet *set);

/**
 * Adds the given character to the given USet.  After this call,
 * uset_contains(set, c) will return TRUE.
 * @param set the object to which to add the character
 * @param c the character to add
 */
U_CAPI void U_EXPORT2
uset_add(USet *set, UChar32 c);

/**
 * Removes the given character from the given USet.  After this call,
 * uset_contains(set, c) will return FALSE.
 * @param set the object from which to remove the character
 * @param c the character to remove
 */
U_CAPI void U_EXPORT2
uset_remove(USet *set, UChar32 c);

/**
 * Returns TRUE if the given USet contains no characters and no
 * strings.
 * @param set the set
 * @return true if set is empty
 */
U_CAPI UBool U_EXPORT2
uset_isEmpty(const USet *set);

/**
 * Returns TRUE if the given USet contains the given character.
 * @param set the set
 * @return true if set contains c
 */
U_CAPI UBool U_EXPORT2
uset_contains(const USet *set, UChar32 c);

/**
 * Returns the number of characters and strings contained in the given
 * USet.
 * @param set the set
 * @return a non-negative integer counting the characters and strings
 * contained in set
 */
U_CAPI int32_t U_EXPORT2
uset_size(const USet* set);

/**
 * Returns the number of disjoint ranges of characters contained in
 * the given set.  Ignores any strings contained in the set.
 * @param set the set
 * @return a non-negative integer counting the character ranges
 * contained in set
 */
U_CAPI int32_t U_EXPORT2
uset_countRanges(const USet *set);

/**
 * Returns a range of characters contained in the given set.
 * @param set the set
 * @param rangeIndex a non-negative integer in the range 0..
 * uset_countRanges(set)-1
 * @param pStart pointer to variable to receive first character
 * in range, inclusive
 * @param pEnd pointer to variable to receive last character in range,
 * inclusive
 * @return true if rangeIndex is value, otherwise false
 */
U_CAPI UBool U_EXPORT2
uset_getRange(const USet *set, int32_t rangeIndex,
              UChar32 *pStart, UChar32 *pEnd);

/**
 * Serializes this set into an array of 16-bit integers.  The array
 * has following format (each line is one 16-bit integer):
 *
 *  length     = (n+2*m) | (m!=0?0x8000:0)
 *  bmpLength  = n; present if m!=0
 *  bmp[0]
 *  bmp[1]
 *  ...
 *  bmp[n-1]
 *  supp-high[0]
 *  supp-low[0]
 *  supp-high[1]
 *  supp-low[1]
 *  ...
 *  supp-high[m-1]
 *  supp-low[m-1]
 *
 * The array starts with a header.  After the header are n bmp
 * code points, then m supplementary code points.  Either n or m
 * or both may be zero.  n+2*m is always <= 0x7FFF.
 *
 * If there are no supplementary characters (if m==0) then the
 * header is one 16-bit integer, 'length', with value n.
 *
 * If there are supplementary characters (if m!=0) then the header
 * is two 16-bit integers.  The first, 'length', has value
 * (n+2*m)|0x8000.  The second, 'bmpLength', has value n.
 *
 * After the header the code points are stored in ascending order.
 * Supplementary code points are stored as most significant 16
 * bits followed by least significant 16 bits.
 *
 * @param set the set
 * @param dest pointer to buffer of destCapacity 16-bit integers.
 * May be NULL only if destCapacity is zero.
 * @param destCapacity size of dest, or zero.  Must not be negative.
 * @param pErrorCode pointer to the error code.  Will be set to
 * U_INDEX_OUTOFBOUNDS_ERROR if n+2*m > 0x7FFF.  Will be set to
 * U_BUFFER_OVERFLOW_ERROR if n+2*m+(m!=0?2:1) > destCapacity.
 * @return the total length of the serialized format, including
 * the header, that is, n+2*m+(m!=0?2:1), or 0 on error other
 * than U_BUFFER_OVERFLOW_ERROR.
 */
U_CAPI int32_t U_EXPORT2
uset_serialize(const USet *set, uint16_t *dest, int32_t destCapacity, UErrorCode *pErrorCode);

/**
 * Given a serialized array, fill in the given serialized set object.
 * @param fillSet pointer to result
 * @param src pointer to start of array
 * @param srcLength length of array
 * @return true if the given array is valid, otherwise false
 */
U_CAPI UBool U_EXPORT2
uset_getSerializedSet(USerializedSet *fillSet, const uint16_t *src, int32_t srcLength);

/**
 * Set the USerializedSet to contain the given character (and nothing
 * else).
 */
U_CAPI void U_EXPORT2
uset_setSerializedToOne(USerializedSet *fillSet, UChar32 c);

/**
 * Returns TRUE if the given USerializedSet contains the given
 * character.
 * @param set the serialized set
 * @return true if set contains c
 */
U_CAPI UBool U_EXPORT2
uset_serializedContains(const USerializedSet *set, UChar32 c);

/**
 * Returns the number of disjoint ranges of characters contained in
 * the given serialized set.  Ignores any strings contained in the
 * set.
 * @param set the serialized set
 * @return a non-negative integer counting the character ranges
 * contained in set
 */
U_CAPI int32_t U_EXPORT2
uset_countSerializedRanges(const USerializedSet *set);

/**
 * Returns a range of characters contained in the given serialized
 * set.
 * @param set the serialized set
 * @param rangeIndex a non-negative integer in the range 0..
 * uset_countSerializedRanges(set)-1
 * @param pStart pointer to variable to receive first character
 * in range, inclusive
 * @param pEnd pointer to variable to receive last character in range,
 * inclusive
 * @return true if rangeIndex is value, otherwise false
 */
U_CAPI UBool U_EXPORT2
uset_getSerializedRange(const USerializedSet *set, int32_t rangeIndex,
                        UChar32 *pStart, UChar32 *pEnd);

#endif
