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
 * possible directly on a serialized set.  See below.
 */
struct USerializedSet {
    const uint16_t *array;
    int32_t bmpLength, length;
    uint16_t staticArray[USET_SERIALIZED_STATIC_ARRAY_CAPACITY];
};
typedef struct USerializedSet USerializedSet;


/*********************************************************************
 * USet API
 *********************************************************************/

/**
 * Creates a USet object that contains the range of characters
 * start..end, inclusive.
 * @param start first character of the range, inclusive
 * @param end last character of the range, inclusive
 * @return a newly created USet.  The caller must call uset_close() on
 * it when done.
 */
U_CAPI USet* U_EXPORT2
uset_open(UChar32 start, UChar32 end);

/**
 * Creates a set from the given pattern.  See the UnicodeSet class
 * description for the syntax of the pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param patternLength the length of the pattern, or -1 if null
 * terminated
 * @param ec the error code
 */
U_CAPI USet* U_EXPORT2
uset_openPattern(const UChar* pattern, int32_t patternLength,
                 UErrorCode* ec);

/**
 * Disposes of the storage used by a USet object.  This function should
 * be called exactly once for objects returned by uset_open().
 * @param set the object to dispose of
 */
U_CAPI void U_EXPORT2
uset_close(USet* set);

/**
 * Returns a string representation of this set.  If the result of
 * calling this function is passed to a uset_openPattern(), it
 * will produce another set that is equal to this one.
 * @param set the set
 * @param result the string to receive the rules, may be NULL
 * @param resultCapacity the capacity of result, may be 0 if result is NULL
 * @param escapeUnprintable if TRUE then convert unprintable
 * character to their hex escape representations, \uxxxx or
 * \Uxxxxxxxx.  Unprintable characters are those other than
 * U+000A, U+0020..U+007E.
 * @param ec error code.
 * @return length of string, possibly larger than resultCapacity
 */
U_CAPI int32_t U_EXPORT2
uset_toPattern(const USet* set,
               UChar* result, int32_t resultCapacity,
               UBool escapeUnprintable,
               UErrorCode* ec);

/**
 * Adds the given character to the given USet.  After this call,
 * uset_contains(set, c) will return TRUE.
 * @param set the object to which to add the character
 * @param c the character to add
 */
U_CAPI void U_EXPORT2
uset_add(USet* set, UChar32 c);

/**
 * Adds the given range of characters to the given USet.  After this call,
 * uset_contains(set, start, end) will return TRUE.
 * @param set the object to which to add the character
 * @param start the first character of the range to add, inclusive
 * @param end the last character of the range to add, inclusive
 * @draft ICU 2.2
 */
U_CAPI void U_EXPORT2
uset_addRange(USet* set, UChar32 start, UChar32 end);

/**
 * Adds the given string to the given USet.  After this call,
 * uset_containsString(set, str, strLen) will return TRUE.
 * @param set the object to which to add the character
 * @param str the string to add
 * @param strLen the length of the string or -1 if null terminated.
 */
U_CAPI void U_EXPORT2
uset_addString(USet* set, const UChar* str, int32_t strLen);

/**
 * Removes the given character from the given USet.  After this call,
 * uset_contains(set, c) will return FALSE.
 * @param set the object from which to remove the character
 * @param c the character to remove
 */
U_CAPI void U_EXPORT2
uset_remove(USet* set, UChar32 c);

/**
 * Removes the given range of characters from the given USet.  After this call,
 * uset_contains(set, start, end) will return FALSE.
 * @param set the object to which to add the character
 * @param start the first character of the range to remove, inclusive
 * @param end the last character of the range to remove, inclusive
 * @draft ICU 2.2
 */
U_CAPI void U_EXPORT2
uset_removeRange(USet* set, UChar32 start, UChar32 end);

/**
 * Removes the given string to the given USet.  After this call,
 * uset_containsString(set, str, strLen) will return FALSE.
 * @param set the object to which to add the character
 * @param str the string to remove
 * @param strLen the length of the string or -1 if null terminated.
 */
U_CAPI void U_EXPORT2
uset_removeString(USet* set, const UChar* str, int32_t strLen);

/**
 * Inverts this set.  This operation modifies this set so that
 * its value is its complement.  This operation does not affect
 * the multicharacter strings, if any.
 * @param set the set
 */
U_CAPI void U_EXPORT2
uset_complement(USet* set);

/**
 * Removes all of the elements from this set.  This set will be
 * empty after this call returns.
 * @param set the set
 */
U_CAPI void U_EXPORT2
uset_clear(USet* set);

/**
 * Returns TRUE if the given USet contains no characters and no
 * strings.
 * @param set the set
 * @return true if set is empty
 */
U_CAPI UBool U_EXPORT2
uset_isEmpty(const USet* set);

/**
 * Returns TRUE if the given USet contains the given character.
 * @param set the set
 * @return true if set contains c
 */
U_CAPI UBool U_EXPORT2
uset_contains(const USet* set, UChar32 c);

/**
 * Returns TRUE if the given USet contains all characters c
 * where start <= c && c <= end.
 * @param start the first character of the range to test, inclusive
 * @param end the last character of the range to test, inclusive
 * @return TRUE if set contains the range
 * @draft ICU 2.2
 */
U_CAPI UBool U_EXPORT2
uset_containsRange(const USet* set, UChar32 start, UChar32 end);

/**
 * Returns TRUE if the given USet contains the given string.
 * @param set the set
 * @param str the string
 * @param strLen the length of the string or -1 if null terminated.
 * @return true if set contains str
 */
U_CAPI UBool U_EXPORT2
uset_containsString(const USet* set, const UChar* str, int32_t strLen);

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
 * Returns the number of items in this set.  An item is either a range
 * of characters or a single multicharacter string.
 * @param set the set
 * @return a non-negative integer counting the character ranges
 * and/or strings contained in set
 */
U_CAPI int32_t U_EXPORT2
uset_getItemCount(const USet* set);

/**
 * Returns an item of this set.  An item is either a range of
 * characters or a single multicharacter string.
 * @param set the set
 * @param itemIndex a non-negative integer in the range 0..
 * uset_getItemCount(set)-1
 * @param start pointer to variable to receive first character
 * in range, inclusive
 * @param end pointer to variable to receive last character in range,
 * inclusive
 * @param str buffer to receive the string, may be NULL
 * @param strCapacity capacity of str, or 0 if str is NULL
 * @param ec error code
 * @return the length of the string (>= 2), or 0 if the item is a
 * range, in which case it is the range *start..*end, or -1 if
 * itemIndex is out of range
 */
U_CAPI int32_t U_EXPORT2
uset_getItem(const USet* set, int32_t itemIndex,
             UChar32* start, UChar32* end,
             UChar* str, int32_t strCapacity,
             UErrorCode* ec);

/*********************************************************************
 * Serialized set API
 *********************************************************************/

/**
 * Serializes this set into an array of 16-bit integers.  Serialization
 * (currently) only records the characters in the set; multicharacter
 * strings are ignored.
 *
 * The array
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
uset_serialize(const USet* set, uint16_t* dest, int32_t destCapacity, UErrorCode* pErrorCode);

/**
 * Given a serialized array, fill in the given serialized set object.
 * @param fillSet pointer to result
 * @param src pointer to start of array
 * @param srcLength length of array
 * @return true if the given array is valid, otherwise false
 */
U_CAPI UBool U_EXPORT2
uset_getSerializedSet(USerializedSet* fillSet, const uint16_t* src, int32_t srcLength);

/**
 * Set the USerializedSet to contain the given character (and nothing
 * else).
 */
U_CAPI void U_EXPORT2
uset_setSerializedToOne(USerializedSet* fillSet, UChar32 c);

/**
 * Returns TRUE if the given USerializedSet contains the given
 * character.
 * @param set the serialized set
 * @return true if set contains c
 */
U_CAPI UBool U_EXPORT2
uset_serializedContains(const USerializedSet* set, UChar32 c);

/**
 * Returns the number of disjoint ranges of characters contained in
 * the given serialized set.  Ignores any strings contained in the
 * set.
 * @param set the serialized set
 * @return a non-negative integer counting the character ranges
 * contained in set
 */
U_CAPI int32_t U_EXPORT2
uset_getSerializedRangeCount(const USerializedSet* set);

/**
 * Returns a range of characters contained in the given serialized
 * set.
 * @param set the serialized set
 * @param rangeIndex a non-negative integer in the range 0..
 * uset_getSerializedRangeCount(set)-1
 * @param pStart pointer to variable to receive first character
 * in range, inclusive
 * @param pEnd pointer to variable to receive last character in range,
 * inclusive
 * @return true if rangeIndex is valid, otherwise false
 */
U_CAPI UBool U_EXPORT2
uset_getSerializedRange(const USerializedSet* set, int32_t rangeIndex,
                        UChar32* pStart, UChar32* pEnd);

#endif
