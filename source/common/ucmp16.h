
/*
**********************************************************************
*   Copyright (C) 1995-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 * @version 1.0 23/10/96
 * @author  Helena Shih
 * Based on Taligent international support for java
 * Modification History : 
 *
 * 05/07/97     helena      Added isBogus()
 * 07/15/98        erm            Synched with Java 1.2 CompactShortArray.java.
 * 07/30/98        erm            Added 07/29/98 code review changes.
 * 04/21/99     Damiba     Port to C/New API faster ucmp16_get
 */

#ifndef UCMP16_H
#define UCMP16_H


#include "unicode/utypes.h"



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
 * @version            $Revision: 1.8 $ 8/25/98
 * @author             Helena Shih
 */

typedef struct CompactShortArray {
  int32_t fStructSize;
  int16_t* fArray;
  uint16_t* fIndex;
  int32_t* fHashes;
  int32_t fCount;
  int16_t fDefaultValue;
  bool_t fCompact;    
  bool_t fBogus;
  bool_t fAlias;
  int32_t kBlockShift;
  int32_t kBlockMask;
} CompactShortArray;


U_CAPI int32_t U_EXPORT2 ucmp16_getkUnicodeCount(void);
U_CAPI int32_t U_EXPORT2 ucmp16_getkBlockCount(void);

/**
 * Construct an empty CompactShortArray.
 * @param defaultValue the default value for all characters not explicitly in the array
 */
U_CAPI  CompactShortArray* U_EXPORT2 ucmp16_open(int16_t defaultValue);

 /**
  * Construct a CompactShortArray from a pre-computed index and values array. The values
  * will be adobped by the CompactShortArray. Note: for speed, the compact method will
  * only re-use blocks in the values array that are on a block boundary. The pre-computed
  * arrays passed in to this constructor may re-use blocks at any position in the values
  * array.
  * @param indexArray the index array to be adopted
  * @param newValues the value array to be adobptd
  * @param count the number of entries in the value array
  * @param defaultValue the default value for all characters not explicitly in the array
  * @see compact
  */
U_CAPI  CompactShortArray* U_EXPORT2 ucmp16_openAdopt(uint16_t *indexArray,
						    int16_t *newValues,
						    int32_t count,
						    int16_t defaultValue );

U_CAPI  CompactShortArray* U_EXPORT2 ucmp16_openAdoptWithBlockShift(uint16_t *indexArray,
								  int16_t *newValues,
								  int32_t count,
								  int16_t defaultValue,
								  int32_t blockShift);


U_CAPI  CompactShortArray* U_EXPORT2 ucmp16_openAlias(uint16_t *indexArray,
                                                      int16_t *newValues,
                                                      int32_t count,
                                                      int16_t defaultValue );

U_CAPI  void U_EXPORT2 ucmp16_close(CompactShortArray* array);
 /**
  * Returns TRUE if the creation of the compact array fails.
  */

U_CAPI  bool_t U_EXPORT2 ucmp16_isBogus(const CompactShortArray* array);

/**
 *
 * Get the mapped value of a Unicode character.
 * @param index the character to get the mapped value with
 * @return the mapped value of the given character
 */

#define ucmp16_get(array, index) (array->fArray[(array->fIndex[(index >> array->kBlockShift)] )+ \
                           (index & array->kBlockMask)])

#define ucmp16_getu(array, index) (uint16_t)ucmp16_get(array, index)


/**
  * Set a new value for a Unicode character.
  * Set automatically expands the array if it is compacted.
  * @param character the character to set the mapped value with
  * @param value the new mapped value
  */
U_CAPI  void U_EXPORT2 ucmp16_set(CompactShortArray *array,
				UChar character,
				int16_t value);


 /**
  *
  * Set new values for a range of Unicode character.
  * @param start the starting offset of the range
  * @param end the ending offset of the range
  * @param value the new mapped value
  */
U_CAPI  void U_EXPORT2 ucmp16_setRange(CompactShortArray* array,
				     UChar start,
				     UChar end, 
				     int16_t value);


/**
 * Compact the array. For efficency, this method will only re-use
 * blocks in the values array that are on a block bounday. If you
 * want better compaction, you can do your own compaction and use
 * the constructor that lets you pass in the pre-computed arrays.
 */
U_CAPI  void U_EXPORT2 ucmp16_compact(CompactShortArray* array);

/**
 * Get the default value.
 */
U_CAPI  int16_t U_EXPORT2 ucmp16_getDefaultValue(const CompactShortArray* array);

/**
 *
 * Get the number of elements in the value array.
 * @return the number of elements in the value array.
 */
U_CAPI  uint32_t U_EXPORT2 ucmp16_getCount(const CompactShortArray* array);

/**
 *
 * Get the address of the value array.
 * @return the address of the value array
 */
U_CAPI  const int16_t* U_EXPORT2 ucmp16_getArray(const CompactShortArray* array);

/**
 *
 * Get the address of the index array.
 * @return the address of the index array
 */
U_CAPI  const uint16_t* U_EXPORT2 ucmp16_getIndex(const CompactShortArray* array);


/** INTERNAL USE ONLY **/
U_CAPI  CompactShortArray * U_EXPORT2 ucmp16_cloneFromData(const uint8_t **source,  UErrorCode *status);

#endif



