/*
 ********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 */



#ifndef UCMP8_H
#define UCMP8_H

/* 32-bits.
  Bump this whenever the internal structure changes.
*/
#define ICU_UCMP8_VERSION 0x01260000

#include "umemstrm.h"
#include "unicode/utypes.h"

/*====================================
 * class CompactByteArray
 * Provides a compact way to store information that is indexed by Unicode values,
 * such as character properties, types, keyboard values, etc.
 * The ATypes are used by value, so should be small, integers or pointers.
 *====================================
 */

U_CAPI int32_t U_EXPORT2 ucmp8_getkUnicodeCount(void);
U_CAPI int32_t U_EXPORT2 ucmp8_getkBlockCount(void);

typedef struct CompactByteArray {
  uint32_t fStructSize;
  int8_t* fArray;
  uint16_t* fIndex;
  int32_t fCount;
  UBool fCompact;
  UBool fBogus;
  UBool fAlias;
  UBool fIAmOwned; /* don't free CBA on close */
} CompactByteArray;

#define UCMP8_kUnicodeCount 65536
#define UCMP8_kBlockShift 7
#define UCMP8_kBlockCount (1<<UCMP8_kBlockShift)
#define UCMP8_kIndexShift (16-UCMP8_kBlockShift)
#define UCMP8_kIndexCount (1<<UCMP8_kIndexShift)
#define UCMP8_kBlockMask (UCMP8_kBlockCount-1)


/**
 * Construct an empty CompactByteArray with uprv_malloc(). Do not call any of the
 * ucmp8_init*() functions after using this function. They will cause a memory
 * leak.
 *
 * @param defaultValue the default value for all characters not explicitly in the array
 * @see ucmp8_init
 * @see ucmp8_initBogus
 * @return The initialized array.
 */
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_open(int8_t defaultValue);

/**
 * Construct a CompactByteArray from a pre-computed index and values array. The values
 * will be adopted by the CompactByteArray. Memory is allocated with uprv_malloc.
 * Note: for speed, the compact method will only re-use blocks in the values array
 * that are on a block boundary. The pre-computed arrays passed in to this constructor
 * may re-use blocks at any position in the values array. The indexArray and newValues
 * will be uprv_free'd when ucmp16_close() is called.
 *
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @return the newly constructed ComapctByteArray
 * @see compact
 */
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_openAdopt(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);

/**
 * Construct a CompactByteArray from a pre-computed index and values array. The values
 * will be aliased by the CompactByteArray. Memory is allocated with uprv_malloc.
 * Note: for speed, the compact method will only re-use blocks in the values array
 * that are on a block boundary. The pre-computed arrays passed in to this constructor
 * may re-use blocks at any position in the values array.
 *
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @return the newly constructed CompactByteArray
 * @see compact
 */
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_openAlias(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);


/**
 * Initialize an empty CompactByteArray. Do not call this function
 * if you created the array with ucmp8_open() because it will cause a memory
 * leak.
 *
 * @param defaultValue the default value for all characters not explicitly in the array
 * @param array An uninitialized CompactByteArray
 * @see ucmp8_open
 */
U_CAPI  void U_EXPORT2 ucmp8_init(CompactByteArray* array, int8_t defaultValue);

/**
 * Initialize an empty CompactByteArray to the bogus value. Do not call this
 * function if you created the array with ucmp8_open() because it will cause
 * a memory leak.
 *
 * @param array An uninitialized CompactByteArray
 * @see ucmp8_open
 * @see ucmp8_isBogus
 */
U_CAPI  void U_EXPORT2 ucmp8_initBogus(CompactByteArray* array);

/**
 * Initialize a CompactByteArray from a pre-computed index and values array. The values
 * will be adopted by the CompactByteArray. Memory is allocated with uprv_malloc.
 * Note: for speed, the compact method will only re-use blocks in the values array
 * that are on a block boundary. The pre-computed arrays passed in to this constructor
 * may re-use blocks at any position in the values array. The indexArray and newValues
 * will be uprv_free'd when ucmp16_close() is called.
 *
 * @param this_obj An uninitialized CompactByteArray
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @return the pointer refers to the CompactByteArray
 * @see compact
 */
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_initAdopt(CompactByteArray *this_obj,
                               uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);

/**
 * Initialize a CompactByteArray from a pre-computed index and values array. The values
 * will be aliased by the CompactByteArray. Memory is allocated with uprv_malloc.
 * Note: for speed, the compact method will only re-use blocks in the values array
 * that are on a block boundary. The pre-computed arrays passed in to this constructor
 * may re-use blocks at any position in the values array.
 *
 * @param this_obj An uninitialized CompactByteArray
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @return the pointer refers to the CompactByteArray
 * @see compact
 */
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_initAlias(CompactByteArray *this_obj,
                               uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);

/**
 * Free up any allocated memory associated with this compact array.
 * The memory that is uprv_free'd depends on how the array was initialized
 * or opened.
 * 
 * @param array The compact array to close
 */
U_CAPI  void U_EXPORT2 ucmp8_close(CompactByteArray* array);

/**
 * Returns TRUE if the creation of the compact array fails.
 * @param array The CompactByteArray to be created.
 * @return TRUE if the creation of the compact array fails.
 */
U_CAPI  UBool U_EXPORT2 ucmp8_isBogus(const CompactByteArray* array);

/**
 * Get the mapped value of a Unicode character.
 *
 * @param index the character to get the mapped value with
 * @return the mapped value of the given character
 */
#define ucmp8_get(array, index)  (array->fArray[(array->fIndex[index >> UCMP8_kBlockShift] & 0xFFFF) + (index & UCMP8_kBlockMask)])

#define ucmp8_getu(array,index) (uint8_t)ucmp8_get(array,index)


/**
 * Set a new value for a Unicode character.
 * Set automatically expands the array if it is compacted.
 *
 * @param array the CompactByteArray to be set
 * @param character the character to set the mapped value with
 * @param value the new mapped value
 */
U_CAPI  void U_EXPORT2 ucmp8_set(CompactByteArray* array,
                 UChar character,
                 int8_t value);

/**
 * Set new values for a range of Unicode character.
 *
 * @param array the CompactByteArray to be set
 * @param start the starting offset of the range
 * @param end the ending offset of the range
 * @param value the new mapped value
 */
U_CAPI  void U_EXPORT2 ucmp8_setRange(CompactByteArray* array, 
                  UChar start,
                  UChar end, 
                  int8_t value);

U_CAPI  int32_t U_EXPORT2 ucmp8_getCount(const CompactByteArray* array);
U_CAPI  const int8_t* U_EXPORT2 ucmp8_getArray(const CompactByteArray* array);
U_CAPI  const uint16_t* U_EXPORT2 ucmp8_getIndex(const CompactByteArray* array);

/**
 * Compact the array.
 * The value of cycle determines how large the overlap can be.
 * A cycle of 1 is the most compacted, but takes the most time to do.
 * If values stored in the array tend to repeat in cycles of, say, 16,
 * then using that will be faster than cycle = 1, and get almost the
 * same compression.
 * @param array The CompactByteArray to be compacted
 * @param cycle The value determines how large the overlap can be.
 */
U_CAPI  void U_EXPORT2 ucmp8_compact(CompactByteArray* array, 
                 uint32_t cycle);

/** Expanded takes the array back to a 65536 element array*/
/*  @param array The CompactByteArray to be expanded*/
U_CAPI  void U_EXPORT2 ucmp8_expand(CompactByteArray* array);

/** (more) INTERNAL USE ONLY **/
U_CAPI  uint32_t U_EXPORT2 ucmp8_flattenMem (const CompactByteArray* array, UMemoryStream *MS);
/* initializes an existing CBA from memory.  Will cause ucmp8_close() to not deallocate anything. */
U_CAPI  void U_EXPORT2 ucmp8_initFromData(CompactByteArray* array, const uint8_t **source, UErrorCode *status);

#endif

