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

/** 
 * NOTE: This array is specifically implemented to support surrogates
 * in the collation framework. It's interface is minimal and usage model
 * is far from the flexible. Use at your own risk outside of collation.
 * Risk is also present in the collation framework, but there is hardly
 * anything you can do about it, save reimplementig the framework
 */

#ifndef UCMPE32_H
#define UCMPE32_H

#define ICU_UCMPE32_VERSION 0x01260000

#include "unicode/utypes.h"

#include "filestrm.h"
#include "umemstrm.h"


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

typedef struct CompactEIntArray{
  uint32_t fStructSize;
  UBool fCompact;    
  UBool fAlias;
  UBool fBogus;

  uint16_t *stage1;
  int32_t *stage2;
  int32_t stage1Top;
  int32_t stage2Top;
  int32_t fDefaultValue;
  int32_t fSurrogateValue;
} CompactEIntArray;

    
/**
 * Construct an empty CompactEIntArray.
 *
 * @param defaultValue the default value for all characters not explicitly in the array
 */
U_CAPI CompactEIntArray* U_EXPORT2 
ucmpe32_open(int32_t defaultValue, int32_t surrogateValue, UErrorCode *status);

/**
 * Opens a compacted read-only array from
 * a block in memory.
 */
U_CAPI  CompactEIntArray* U_EXPORT2 
ucmpe32_openFromData( const uint8_t **source, UErrorCode *status);

/**
 * Clones an array. It can be either compacted or expanded
 */
U_CAPI CompactEIntArray* U_EXPORT2 
ucmpe32_clone(CompactEIntArray* orig, UErrorCode *status);

/**
 * Free up any allocated memory associated with this compact array.
 * The memory that is uprv_free'd depends on how the array was initialized
 * or opened.
 * 
 * @param array The compact array to close
 */
U_CAPI void U_EXPORT2 ucmpe32_close(CompactEIntArray* array);

/**
 * Get the mapped value of a Unicode character.
 *
 * @param index the character to get the mapped value with
 * @return the mapped value of the given character
 */
#define ucmpe32_get(this_obj, index) (this_obj->stage2[(this_obj->stage1[(index >> _UCMPE32_TRIE_SHIFT)] )+ \
                           (index & _UCMPE32_STAGE_2_MASK)])

/** 
 * Get the mapped value of a confirmed surrogate. First value already comes 
 * from the trie and is combined with the following value in order to get
 * the value. THIS CAN BE ONLY USED ON A COMPACTED TRIE. You will get wrong
 * results if you try it on the expanded one
 * NO ERROR CHECKING IS PERFORMED! PREPARE YOUR DATA CAREFULLY!
 * @param leadValue32 the mapping of the leading surrogate.
 * @param trail the trailing surrogate
 * @return the mapped value of the given character
 */

#define ucmpe32_getSurrogate(this_obj, leadValue32, trail) ucmpe32_get(this_obj, \
                           ((leadValue32 & 0xffc00) | (trail & 0x3ff)))

/**
 * This is a slow function that takes lead and trail surrogate and gets
 * the mapping regardless of the compaction status. 
 */
U_CAPI int32_t U_EXPORT2 
ucmpe32_getSurrogateEx(CompactEIntArray *array, UChar lead, UChar trail);

/**
 * Set a new value for a Unicode character.
 * Do not set if the array is compacted - nothing will happen.
 * @param character the character to set the mapped value with
 * @param value the new mapped value
 */
U_CAPI  void U_EXPORT2 ucmpe32_set32(CompactEIntArray *array,
                  UChar32 character,
                  int32_t value);

/** 
 * alias for compatibility
 */
#define ucmpe32_set(array, character, value) ucmpe32_set32((array), (UChar32)(character), (value))


/**
 * Set a new value for a surrogate character.
 * Do not set if the array is compacted - nothing will happen.
 * Set automatically expands the array if it is compacted.
 * Alternatively you can put the surrogate code point together 
 * yourself and use set32.
 * @param lead leading surrogate unit
 * @param trail trailing surrogate unit
 * @param value the new mapped value
 */
U_CAPI void  U_EXPORT2 
ucmpe32_setSurrogate(CompactEIntArray* this_obj, UChar lead, 
                           UChar trail, int32_t value);
/** 
 * compacts the array. 
 * This folds the surrogates and compacts the array.
 * no setting will succeed after the array is compacted.
 * Array have to be compacted in order to be flattened.
 */
U_CAPI  void U_EXPORT2 
ucmpe32_compact(CompactEIntArray* this_object);

/** 
 * Flattens the array to an memory stream.
 * Array has to be compacted beforehand.
 * @param MS memory stream to flatten to
 * @return number of bytes written.
 */
U_CAPI  uint32_t U_EXPORT2 
ucmpe32_flattenMem(const CompactEIntArray* this_object, UMemoryStream *MS);

#endif

