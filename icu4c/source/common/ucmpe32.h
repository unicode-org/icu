/*
 **********************************************************************
 *   Copyright (C) 2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 *   file name:  ucmpe32.h
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2001aug03
 *   created by: Vladimir Weinstein
 *
 *   This is basically a rip-off of trie developed by Markus for 
 *   normalization data, but using a reduced ucmp interface
 *   Interface is implemented as much as required by the collation
 *   framework.
 *   This table is slow on data addition, but should support surrogates
 *   nicely.
 */

#ifndef UCMPE32_H
#define UCMPE32_H

#define ICU_UCMPE32_VERSION 0x01260000

#include "unicode/utypes.h"

#include "filestrm.h"
#include "umemstrm.h"

/* INTERNAL CONSTANTS */
#define UCMPE32_kBlockShift    7

#define UCMPE32_kBlockCount    (1<<UCMPE32_kBlockShift)
#define UCMPE32_kBlockMask     (UCMPE32_kBlockCount-1)

#define UCMPE32_kSurrogateBlockBits (10 - UCMPE32_kBlockShift)
#define UCMPE32_kSurrogateBlockCount (1<<UCMPE32_kSurrogateBlockBits)

#define UCMPE32_kIndexShift    (21-UCMPE32_kBlockShift)
/*#define UCMPE32_kIndexCount    (1<<UCMPE32_kIndexShift)*/
#define UCMPE32_kIndexCount (0x110000>>UCMPE32_kBlockShift)

/*#define UCMPE32_kIndexBMPCount (1<<(16-UCMPE32_kBlockShift))*/
#define UCMPE32_kIndexBMPCount (0x10000>>UCMPE32_kBlockShift)


#define UCMPE32_kUnicodeCount  0x110000

/* trie constants */
enum {
    /*
     * must be <=10:
     * above 10, a lead surrogate's block is smaller than a stage 2 block
     */
    _UCMPE32_TRIE_SHIFT=5,

    _UCMPE32_STAGE_2_BLOCK_COUNT=1<<_UCMPE32_TRIE_SHIFT,
    _UCMPE32_STAGE_2_MASK=_UCMPE32_STAGE_2_BLOCK_COUNT-1,

    _UCMPE32_STAGE_1_BMP_COUNT=(1<<(16-_UCMPE32_TRIE_SHIFT)),

    _UCMPE32_SURROGATE_BLOCK_BITS=10-_UCMPE32_TRIE_SHIFT,
    _UCMPE32_SURROGATE_BLOCK_COUNT=(1<<_UCMPE32_SURROGATE_BLOCK_BITS),

    _UCMPE32_EXTRA_SHIFT=16,               /* 16 bits for the index to UChars and other extra data */
    _UCMPE32_EXTRA_INDEX_TOP=0xfc00,       /* start of surrogate specials after shift */

    _UCMPE32_EXTRA_SURROGATE_MASK=0x3ff,
    _UCMPE32_EXTRA_SURROGATE_TOP=0x3f0    /* hangul etc. */
};

/* this may be >0xffff and may not work as an enum */
#define _UCMPE32_STAGE_1_MAX_COUNT (0x110000>>_UCMPE32_TRIE_SHIFT)

typedef struct UToolMemory {
    char name[64];
    uint32_t count, size, index;
    uint32_t array[1];
} UToolMemory;

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
 * @see                CompactEIntArray
 * @see                CompactCharArray
 * @see                CompactStringArray
 * @version            $Revision: 1.1 $ 8/25/98
 * @author             Helena Shih
 */
/*====================================
 *CompactEIntArray
 * Provides a compact way to store information that is indexed by Unicode values,
 * such as character properties, types, keyboard values, etc.
 * The ATypes are used by value, so should be small, integers or pointers.
 *====================================
 */

typedef struct CompactEIntArray{
    uint32_t fStructSize;
    int32_t* fArray;
    uint16_t* fIndex;
    int32_t fCount;
    UBool fCompact;    
    UBool fBogus;
    UBool fAlias;
    UBool fIAmOwned; /* don't free CBA on close */

  UToolMemory *stage2Mem;
  uint16_t stage1[_UCMPE32_STAGE_1_MAX_COUNT];
  uint32_t *stage2;
  uint16_t stage1Top;

} CompactEIntArray;

    U_CAPI int32_t U_EXPORT2 ucmpe32_getkUnicodeCount(void);
    U_CAPI int32_t U_EXPORT2 ucmpe32_getkBlockCount(void);

    
/**
 * Construct an empty CompactEIntArray.
 *
 * @param defaultValue the default value for all characters not explicitly in the array
 */
U_CAPI CompactEIntArray* U_EXPORT2 ucmpe32_open(int32_t defaultValue);

/**
 * Construct a CompactEIntArray from a pre-computed index and values array. The values
 * will be adopted by the CompactEIntArray. Memory is allocated with uprv_malloc.
 * Note: for speed, the compact method will only re-use blocks in the values array
 * that are on a block boundary. The pre-computed arrays passed in to this constructor
 * may re-use blocks at any position in the values array. The indexArray and
 * newValues will be uprv_free'd when ucmp16_close() is called.
 *
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @see compact
 */
U_CAPI CompactEIntArray* U_EXPORT2 ucmpe32_openAdopt(uint16_t *indexArray,
                          int32_t *newValues,
                          int32_t count);

/**
 * Construct a CompactEIntArray from a pre-computed index and values array. The values
 * will be aliased by the CompactEIntArray. Memory is allocated with uprv_malloc.
 * Note: for speed, the compact method will only re-use blocks in the values array
 * that are on a block boundary. The pre-computed arrays passed in to this constructor
 * may re-use blocks at any position in the values array.
 *
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @see compact
 */
U_CAPI CompactEIntArray* U_EXPORT2 ucmpe32_openAlias(uint16_t *indexArray,
                          int32_t *newValues,
                          int32_t count);

/**
 * Initialize a CompactEIntArray from a pre-computed index and values array. The values
 * will be adopted by the CompactEIntArray. No memory is allocated. Note: for speed,
 * the compact method will only re-use blocks in the values array that are on a block
 * boundary. The pre-computed arrays passed in to this constructor may re-use blocks
 * at any position in the values array. The indexArray and
 * newValues will be uprv_free'd when ucmp16_close() is called.
 *
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @see compact
 */
U_CAPI CompactEIntArray* U_EXPORT2 ucmpe32_initAdopt(CompactEIntArray *this_obj,
                          uint16_t *indexArray,
                          int32_t *newValues,
                          int32_t count);

/**
 * Initialize a CompactEIntArray from a pre-computed index and values array. The values
 * will be aliased by the CompactEIntArray. No memory is allocated. Note: for speed,
 * the compact method will only re-use blocks in the values array that are on a block
 * boundary. The pre-computed arrays passed in to this constructor may re-use blocks
 * at any position in the values array.
 *
 * @param indexArray the index array to be adopted
 * @param newValues the value array to be adopted
 * @param count the number of entries in the value array
 * @see compact
 */
U_CAPI CompactEIntArray* U_EXPORT2 ucmpe32_initAlias(CompactEIntArray *this_obj,
                          uint16_t *indexArray,
                          int32_t *newValues,
                          int32_t count);

/**
 * Free up any allocated memory associated with this compact array.
 * The memory that is uprv_free'd depends on how the array was initialized
 * or opened.
 * 
 * @param array The compact array to close
 */
U_CAPI void U_EXPORT2 ucmpe32_close(CompactEIntArray* array);

/**
 * Returns TRUE if the creation of the compact array fails.
 */
U_CAPI  UBool U_EXPORT2 ucmpe32_isBogus(const CompactEIntArray* array);

/**
 * Get the mapped value of a Unicode character.
 *
 * @param index the character to get the mapped value with
 * @return the mapped value of the given character
 */

#if 0
#define ucmpe32_get(array, index) (array->fArray[(array->fIndex[(index >> UCMPE32_kBlockShift)<< UCMPE32_kBlockShift] )+ \
                           (index & UCMPE32_kBlockMask)])
#endif

U_CAPI int32_t U_EXPORT2 ucmpe32_get32(CompactEIntArray *array, UChar32 index);
#define ucmpe32_get(array, index) ucmpe32_get32((array), (UChar32)(index))
#define ucmpe32_getu(array, index) (uint16_t)ucmpe32_get(array, index)


U_CAPI int32_t ucmpe32_getSurrogate(CompactEIntArray *array, UChar lead, UChar trail);

 /**
 * Set a new value for a Unicode character.
 * Set automatically expands the array if it is compacted.
 * @param character the character to set the mapped value with
 * @param value the new mapped value
 */
U_CAPI  void U_EXPORT2 ucmpe32_set32(CompactEIntArray *array,
                  UChar32 character,
                  int32_t value);

#define ucmpe32_set(array, character, value) ucmpe32_set32((array), (UChar32)(character), (value))

U_CAPI void  U_EXPORT2 ucmpe32_setSurrogate(CompactEIntArray* this_obj, UChar lead, 
                           UChar trail, int32_t value);

/**
 *
 * Set new values for a range of Unicode character.
 * @param start the starting offset of the range
 * @param end the ending offset of the range
 * @param value the new mapped value
 */
U_CAPI  void U_EXPORT2 ucmpe32_setRange(CompactEIntArray* array,
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
U_CAPI  void U_EXPORT2 ucmpe32_compact(CompactEIntArray* array, int32_t cycle);

/**
 * Expands the compacted array.
 * Takes the array back to a 65536 element array
 */
U_CAPI  void U_EXPORT2 ucmpe32_expand(CompactEIntArray* array);

/**
 * Get the number of elements in the value array.
 *
 * @return the number of elements in the value array.
 */
U_CAPI  uint32_t U_EXPORT2 ucmpe32_getCount(const CompactEIntArray* array);

/**
 * Get the address of the value array.
 *
 * @return the address of the value array
 */
U_CAPI  const int32_t* U_EXPORT2 ucmpe32_getArray(const CompactEIntArray* array);

/**
 * Get the address of the index array.
 *
 * @return the address of the index array
 */
U_CAPI  const uint16_t* U_EXPORT2 ucmpe32_getIndex(const CompactEIntArray* array);

U_CAPI void U_EXPORT2 ucmpe32_streamIn( CompactEIntArray* array, FileStream* is);
U_CAPI void U_EXPORT2 ucmpe32_streamOut(CompactEIntArray* array, FileStream* os);

U_CAPI void U_EXPORT2 ucmpe32_streamMemIn( CompactEIntArray* array, UMemoryStream* is);
U_CAPI void U_EXPORT2 ucmpe32_streamMemOut(CompactEIntArray* array, UMemoryStream* os);

U_CAPI  uint32_t U_EXPORT2 ucmpe32_flattenMem(const CompactEIntArray* array, UMemoryStream *MS);

U_CAPI  CompactEIntArray* U_EXPORT2 ucmpe32_openFromData( const uint8_t **source, UErrorCode *status);
U_CAPI  void U_EXPORT2 ucmpe32_initFromData(CompactEIntArray *this_obj, const uint8_t **source, UErrorCode *status);

#endif

