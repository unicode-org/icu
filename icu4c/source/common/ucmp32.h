/*
 **********************************************************************
 *   Copyright (C) 1995-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 * and others. All Rights Reserved.
 * @version 1.0 23/10/96
 * @author  Helena Shih
 * Based on Taligent international support for java
 * Modification History : 
 *
 *  Date        Name        Description
 *  2/5/97      aliu        Added CompactIntArray streamIn and streamOut methods.
 * 05/07/97     helena      Added isBogus()
 * 04/26/99     Madhu       Ported to C for C Implementation
 * 11/21/99     srl         macroized ucmp32_get()
 */

#ifndef UCMP32_H
#define UCMP32_H


#include "unicode/utypes.h"

#include "filestrm.h"
#include "umemstrm.h"

/* INTERNAL CONSTANTS */
#define UCMP32_kBlockShift  7
#define UCMP32_kBlockCount  (1<<UCMP32_kBlockShift)
#define UCMP32_kIndexShift  (16-UCMP32_kBlockShift)
#define UCMP32_kIndexCount  (1<<UCMP32_kIndexShift)
#define UCMP32_kBlockMask   (UCMP32_kBlockCount-1)
#define UCMP32_kUnicodeCount  65536

 /**
 * class CompactATypeArray : use only on primitive data types
 * Provides a compact way to store information that is indexed by Unicode
 * values, such as character properties, types, keyboard values, etc.This
 * is very useful when you have a block of Unicode data that contains
 * significant values while the rest of the Unicode data is unused in the
 * application or when you have a lot of redundance, such as where all 21,000
 * Han ideographs have the same value.  However, lookup is much faster than a
 * hash table.
 * <P>
 * A compact array of any primitive data type serves two purposes:
 * <UL type = round>
 *     <LI>Fast access of the indexed values.
 *     <LI>Smaller memory footprint.
 * </UL>
 * <P>
 * The index array always points into particular parts of the data array
 * it is initially set up to point at regular block boundaries
 * The following example uses blocks of 4 for simplicity
 * <PRE>
 * Example: Expanded
 * BLOCK  0   1   2   3   4
 * INDEX  0   4   8   12  16 ...
 * ARRAY  abcdeababcdezyabcdea...
 *        |   |   |   |   |   |...
 * </PRE>
 * <P>
 * After compression, the index will point to various places in the data array
 * wherever there is a runs of the same elements as in the original
 * <PRE>
 * Example: Compressed
 * BLOCK  0   1   2   3   4
 * INDEX  0   4   1   8   2 ...
 * ARRAY  abcdeabazyabc...
 * </PRE>
 * <P>
 * If you look at the example, index number 2 in the expanded version points
 * to data position number 8, which has elements "bcde". In the compressed
 * version, index number 2 points to data position 1, which also has "bcde"
 * @see                CompactByteArray
 * @see                CompactIntArray
 * @see                CompactCharArray
 * @see                CompactStringArray
 * @version            $Revision: 1.11 $ 8/25/98
 * @author             Helena Shih
 */
/*====================================*/
/*CompactIntArray
 * Provides a compact way to store information that is indexed by Unicode values,
 * such as character properties, types, keyboard values, etc.
 * The ATypes are used by value, so should be small, integers or pointers.
 *====================================
 */
typedef struct{
    int32_t* fArray;
    uint16_t* fIndex;
    int32_t fCount;
    UBool fCompact;    
    UBool fBogus;
} CompactIntArray;

    U_CAPI int32_t U_EXPORT2 ucmp32_getkUnicodeCount(void);
    U_CAPI int32_t U_EXPORT2 ucmp32_getkBlockCount(void);

    
/**
 *
 * Construct an empty CompactIntArray.
 * @param defaultValue the default value for all characters not explicitly in the array
 */
U_CAPI CompactIntArray* U_EXPORT2 ucmp32_open(int32_t defaultValue);

 /**
  * Construct a CompactIntArray from a pre-computed index and values array. The values
  * will be adopted by the CompactIntArray. Memory is allocated with uprv_malloc.
  * Note: for speed, the compact method will only re-use blocks in the values array
  * that are on a block boundary. The pre-computed arrays passed in to this constructor
  * may re-use blocks at any position in the values array.
  * @param indexArray the index array to be adopted
  * @param newValues the value array to be adopted
  * @param count the number of entries in the value array
  * @see compact
  */
U_CAPI CompactIntArray* U_EXPORT2 ucmp32_openAdopt(uint16_t *indexArray,
                          int32_t *newValues,
                          int32_t count);

 /**
  * Initialize a CompactIntArray from a pre-computed index and values array. The values
  * will be adopted by the CompactIntArray. No memory is allocated. Note: for speed,
  * the compact method will only re-use blocks in the values array that are on a block
  * boundary. The pre-computed arrays passed in to this constructor may re-use blocks
  * at any position in the values array.
  * @param indexArray the index array to be adopted
  * @param newValues the value array to be adopted
  * @param count the number of entries in the value array
  * @see compact
  */
U_CAPI CompactIntArray* U_EXPORT2 ucmp32_initAdopt(CompactIntArray *this_obj,
                          uint16_t *indexArray,
                          int32_t *newValues,
                          int32_t count);

U_CAPI void U_EXPORT2 ucmp32_close(CompactIntArray* array);

/**
  * Returns TRUE if the creation of the compact array fails.
  */

U_CAPI  UBool U_EXPORT2 ucmp32_isBogus(const CompactIntArray* array);
/**
  *
  * Get the mapped value of a Unicode character.
  * @param index the character to get the mapped value with
  * @return the mapped value of the given character
  */

#define ucmp32_get(array, index) (array->fArray[(array->fIndex[(index >> UCMP32_kBlockShift)] )+ \
                           (index & UCMP32_kBlockMask)])

#define ucmp32_getu(array, index) (uint16_t)ucmp32_get(array, index)

/**
  * Set a new value for a Unicode character.
  * Set automatically expands the array if it is compacted.
  * @param character the character to set the mapped value with
  * @param value the new mapped value
  */
U_CAPI  void U_EXPORT2 ucmp32_set(CompactIntArray *array,
                  UChar character,
                  int32_t value);

 /**
  *
  * Set new values for a range of Unicode character.
  * @param start the starting offset of the range
  * @param end the ending offset of the range
  * @param value the new mapped value
  */
U_CAPI  void U_EXPORT2 ucmp32_setRange(CompactIntArray* array,
                   UChar start,
                   UChar end, 
                   int32_t value);

/**
 * Compact the array. The value of cycle determines how large the overlap can be.
 * A cycle of 1 is the most compacted, but takes the most time to do.
 * If values stored in the array tend to repeat in cycles of, say, 16,
 * then using that will be faster than cycle = 1, and get almost the
 * same compression.
 */
U_CAPI  void U_EXPORT2 ucmp32_compact(CompactIntArray* array, int32_t cycle);
/**
 * Expands the compacted array.
 * Takes the array back to a 65536 element array
 */

U_CAPI  void U_EXPORT2 ucmp32_expand(CompactIntArray* array);
/**
 *
 * Get the number of elements in the value array.
 * @return the number of elements in the value array.
 */
U_CAPI  uint32_t U_EXPORT2 ucmp32_getCount(const CompactIntArray* array);

/**
 *
 * Get the address of the value array.
 * @return the address of the value array
 */
U_CAPI  const int32_t* U_EXPORT2 ucmp32_getArray(const CompactIntArray* array);

/**
 *
 * Get the address of the index array.
 * @return the address of the index array
 */
U_CAPI  const uint16_t* U_EXPORT2 ucmp32_getIndex(const CompactIntArray* array);

U_CAPI void U_EXPORT2 ucmp32_streamIn( CompactIntArray* array, FileStream* is);
U_CAPI void U_EXPORT2 ucmp32_streamOut(CompactIntArray* array, FileStream* os);

U_CAPI void U_EXPORT2 ucmp32_streamMemIn( CompactIntArray* array, UMemoryStream* is);
U_CAPI void U_EXPORT2 ucmp32_streamMemOut(CompactIntArray* array, UMemoryStream* os);


#endif






