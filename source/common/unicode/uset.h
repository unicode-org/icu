/*
*******************************************************************************
*
*   Copyright (C) 2002-2004, International Business Machines
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


/**
 * \file
 * \brief C API: Unicode Set
 *
 * <p>This is a C wrapper around the C++ UnicodeSet class.</p>
 */

#ifndef __USET_H__
#define __USET_H__

#include "unicode/utypes.h"

#ifndef UCNV_H
struct USet;
/**
 * A UnicodeSet.  Use the uset_* API to manipulate.  Create with
 * uset_open*, and destroy with uset_close.
 * @stable ICU 2.4
 */
typedef struct USet USet;
#endif

/**
 * Bitmask values to be passed to uset_openPatternOptions() or
 * uset_applyPattern() taking an option parameter.
 * @stable ICU 2.4
 */
enum {
    /**
     * Ignore white space within patterns unless quoted or escaped.
     * @stable ICU 2.4
     */
    USET_IGNORE_SPACE = 1,  

    /**
     * Enable case insensitive matching.  E.g., "[ab]" with this flag
     * will match 'a', 'A', 'b', and 'B'.  "[^ab]" with this flag will
     * match all except 'a', 'A', 'b', and 'B'.
     * @stable ICU 2.4
     */
    USET_CASE_INSENSITIVE = 2,  

    /**
     * Bitmask for UnicodeSet::closeOver() indicating letter case.
     * This may be ORed together with other selectors.
     * @internal
     */
    USET_CASE = 2,
    /**
     * Enough for any single-code point set
     * @internal
     */
    USET_SERIALIZED_STATIC_ARRAY_CAPACITY=8
};

/**
 * A serialized form of a Unicode set.  Limited manipulations are
 * possible directly on a serialized set.  See below.
 * @stable ICU 2.4
 */
typedef struct USerializedSet {
    /**
     * The serialized Unicode Set.
     * @stable ICU 2.4
     */
    const uint16_t *array;
    /**
     * The length of the array that contains BMP characters.
     * @stable ICU 2.4
     */
    int32_t bmpLength;
    /**
     * The total length of the array.
     * @stable ICU 2.4
     */
    int32_t length;
    /**
     * A small buffer for the array to reduce memory allocations.
     * @stable ICU 2.4
     */
    uint16_t staticArray[USET_SERIALIZED_STATIC_ARRAY_CAPACITY];
} USerializedSet;

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
 * @stable ICU 2.4
 */
U_STABLE USet* U_EXPORT2
uset_open(UChar32 start, UChar32 end);

/**
 * Creates a set from the given pattern.  See the UnicodeSet class
 * description for the syntax of the pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param patternLength the length of the pattern, or -1 if null
 * terminated
 * @param ec the error code
 * @stable ICU 2.4
 */
U_STABLE USet* U_EXPORT2
uset_openPattern(const UChar* pattern, int32_t patternLength,
                 UErrorCode* ec);

/**
 * Creates a set from the given pattern.  See the UnicodeSet class
 * description for the syntax of the pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param patternLength the length of the pattern, or -1 if null
 * terminated
 * @param options bitmask for options to apply to the pattern.
 * Valid options are USET_IGNORE_SPACE and USET_CASE_INSENSITIVE.
 * @param ec the error code
 * @stable ICU 2.4
 */
U_STABLE USet* U_EXPORT2
uset_openPatternOptions(const UChar* pattern, int32_t patternLength,
                 uint32_t options,
                 UErrorCode* ec);

/**
 * Disposes of the storage used by a USet object.  This function should
 * be called exactly once for objects returned by uset_open().
 * @param set the object to dispose of
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_close(USet* set);

/**
 * Modifies the set to represent the set specified by the given
 * pattern. See the UnicodeSet class description for the syntax of 
 * the pattern language. See also the User Guide chapter about UnicodeSet.
 * <em>Empties the set passed before applying the pattern.</em>
 * @param set               The set to which the pattern is to be applied. 
 * @param pattern           A pointer to UChar string specifying what characters are in the set.
 *                          The character at pattern[0] must be a '['.
 * @param patternLength     The length of the UChar string. -1 if NUL terminated.
 * @param options           A bitmask for options to apply to the pattern.
 *                          Valid options are USET_IGNORE_SPACE and USET_CASE_INSENSITIVE.
 * @param status            Returns an error if the pattern cannot be parsed.
 * @return                  Upon successful parse, the value is either
 *                          the index of the character after the closing ']' 
 *                          of the parsed pattern.
 *                          If the status code indicates failure, then the return value 
 *                          is the index of the error in the source.
 *                                  
 * @draft ICU 2.8
 */
U_DRAFT int32_t U_EXPORT2 
uset_applyPattern(USet *set,
                  const UChar *pattern, int32_t patternLength,
                  uint32_t options,
                  UErrorCode *status);

/**
 * Returns a string representation of this set.  If the result of
 * calling this function is passed to a uset_openPattern(), it
 * will produce another set that is equal to this one.
 * @param set the set
 * @param result the string to receive the rules, may be NULL
 * @param resultCapacity the capacity of result, may be 0 if result is NULL
 * @param escapeUnprintable if TRUE then convert unprintable
 * character to their hex escape representations, \\uxxxx or
 * \\Uxxxxxxxx.  Unprintable characters are those other than
 * U+000A, U+0020..U+007E.
 * @param ec error code.
 * @return length of string, possibly larger than resultCapacity
 * @stable ICU 2.4
 */
U_STABLE int32_t U_EXPORT2
uset_toPattern(const USet* set,
               UChar* result, int32_t resultCapacity,
               UBool escapeUnprintable,
               UErrorCode* ec);

/**
 * Adds the given character to the given USet.  After this call,
 * uset_contains(set, c) will return TRUE.
 * @param set the object to which to add the character
 * @param c the character to add
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_add(USet* set, UChar32 c);

/**
 * Adds all of the elements in the specified set to this set if
 * they're not already present.  This operation effectively
 * modifies this set so that its value is the <i>union</i> of the two
 * sets.  The behavior of this operation is unspecified if the specified
 * collection is modified while the operation is in progress.
 *
 * @param set the object to which to add the set
 * @param additionalSet the source set whose elements are to be added to this set.
 * @draft ICU 2.6
 */
U_DRAFT void U_EXPORT2
uset_addAll(USet* set, const USet *additionalSet);

/**
 * Adds the given range of characters to the given USet.  After this call,
 * uset_contains(set, start, end) will return TRUE.
 * @param set the object to which to add the character
 * @param start the first character of the range to add, inclusive
 * @param end the last character of the range to add, inclusive
 * @stable ICU 2.2
 */
U_STABLE void U_EXPORT2
uset_addRange(USet* set, UChar32 start, UChar32 end);

/**
 * Adds the given string to the given USet.  After this call,
 * uset_containsString(set, str, strLen) will return TRUE.
 * @param set the object to which to add the character
 * @param str the string to add
 * @param strLen the length of the string or -1 if null terminated.
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_addString(USet* set, const UChar* str, int32_t strLen);

/**
 * Removes the given character from the given USet.  After this call,
 * uset_contains(set, c) will return FALSE.
 * @param set the object from which to remove the character
 * @param c the character to remove
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_remove(USet* set, UChar32 c);

/**
 * Removes the given range of characters from the given USet.  After this call,
 * uset_contains(set, start, end) will return FALSE.
 * @param set the object to which to add the character
 * @param start the first character of the range to remove, inclusive
 * @param end the last character of the range to remove, inclusive
 * @stable ICU 2.2
 */
U_STABLE void U_EXPORT2
uset_removeRange(USet* set, UChar32 start, UChar32 end);

/**
 * Removes the given string to the given USet.  After this call,
 * uset_containsString(set, str, strLen) will return FALSE.
 * @param set the object to which to add the character
 * @param str the string to remove
 * @param strLen the length of the string or -1 if null terminated.
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_removeString(USet* set, const UChar* str, int32_t strLen);

/**
 * Inverts this set.  This operation modifies this set so that
 * its value is its complement.  This operation does not affect
 * the multicharacter strings, if any.
 * @param set the set
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_complement(USet* set);

/**
 * Removes all of the elements from this set.  This set will be
 * empty after this call returns.
 * @param set the set
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_clear(USet* set);

/**
 * Returns TRUE if the given USet contains no characters and no
 * strings.
 * @param set the set
 * @return true if set is empty
 * @stable ICU 2.4
 */
U_STABLE UBool U_EXPORT2
uset_isEmpty(const USet* set);

/**
 * Returns TRUE if the given USet contains the given character.
 * @param set the set
 * @param c The codepoint to check for within the set
 * @return true if set contains c
 * @stable ICU 2.4
 */
U_STABLE UBool U_EXPORT2
uset_contains(const USet* set, UChar32 c);

/**
 * Returns TRUE if the given USet contains all characters c
 * where start <= c && c <= end.
 * @param set the set
 * @param start the first character of the range to test, inclusive
 * @param end the last character of the range to test, inclusive
 * @return TRUE if set contains the range
 * @stable ICU 2.2
 */
U_STABLE UBool U_EXPORT2
uset_containsRange(const USet* set, UChar32 start, UChar32 end);

/**
 * Returns TRUE if the given USet contains the given string.
 * @param set the set
 * @param str the string
 * @param strLen the length of the string or -1 if null terminated.
 * @return true if set contains str
 * @stable ICU 2.4
 */
U_STABLE UBool U_EXPORT2
uset_containsString(const USet* set, const UChar* str, int32_t strLen);

/**
 * Returns the number of characters and strings contained in the given
 * USet.
 * @param set the set
 * @return a non-negative integer counting the characters and strings
 * contained in set
 * @stable ICU 2.4
 */
U_STABLE int32_t U_EXPORT2
uset_size(const USet* set);

/**
 * Returns the number of items in this set.  An item is either a range
 * of characters or a single multicharacter string.
 * @param set the set
 * @return a non-negative integer counting the character ranges
 * and/or strings contained in set
 * @stable ICU 2.4
 */
U_STABLE int32_t U_EXPORT2
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
 * @stable ICU 2.4
 */
U_STABLE int32_t U_EXPORT2
uset_getItem(const USet* set, int32_t itemIndex,
             UChar32* start, UChar32* end,
             UChar* str, int32_t strCapacity,
             UErrorCode* ec);

/* TODO: propose the following to the list and make them public */

/**
 * @internal
 */
U_INTERNAL UBool U_EXPORT2
uset_containsAll(const USet* set1, const USet* set2);

/**
 * @internal
 */
U_INTERNAL UBool U_EXPORT2
uset_containsNone(const USet* set1, const USet* set2);

/**
 * @internal
 */
U_INTERNAL UBool U_EXPORT2
uset_equals(const USet* set1, const USet* set2);

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
 * @stable ICU 2.4
 */
U_STABLE int32_t U_EXPORT2
uset_serialize(const USet* set, uint16_t* dest, int32_t destCapacity, UErrorCode* pErrorCode);

/**
 * Given a serialized array, fill in the given serialized set object.
 * @param fillSet pointer to result
 * @param src pointer to start of array
 * @param srcLength length of array
 * @return true if the given array is valid, otherwise false
 * @stable ICU 2.4
 */
U_STABLE UBool U_EXPORT2
uset_getSerializedSet(USerializedSet* fillSet, const uint16_t* src, int32_t srcLength);

/**
 * Set the USerializedSet to contain the given character (and nothing
 * else).
 * @param fillSet pointer to result
 * @param c The codepoint to set
 * @stable ICU 2.4
 */
U_STABLE void U_EXPORT2
uset_setSerializedToOne(USerializedSet* fillSet, UChar32 c);

/**
 * Returns TRUE if the given USerializedSet contains the given
 * character.
 * @param set the serialized set
 * @param c The codepoint to check for within the set
 * @return true if set contains c
 * @stable ICU 2.4
 */
U_STABLE UBool U_EXPORT2
uset_serializedContains(const USerializedSet* set, UChar32 c);

/**
 * Returns the number of disjoint ranges of characters contained in
 * the given serialized set.  Ignores any strings contained in the
 * set.
 * @param set the serialized set
 * @return a non-negative integer counting the character ranges
 * contained in set
 * @stable ICU 2.4
 */
U_STABLE int32_t U_EXPORT2
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
 * @stable ICU 2.4
 */
U_STABLE UBool U_EXPORT2
uset_getSerializedRange(const USerializedSet* set, int32_t rangeIndex,
                        UChar32* pStart, UChar32* pEnd);

#endif
