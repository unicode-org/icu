/*
 ********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 */



#ifndef UCMP8_H
#define UCMP8_H

/* 32-bits.
  Bump this whenever the internal structure changes.
*/
#define ICU_UCMP8_VERSION 0x01260000

#include "unicode/utypes.h"

/*====================================*/
/* class CompactByteArray
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


U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_open(int8_t defaultValue);

U_CAPI  void U_EXPORT2 ucmp8_init(CompactByteArray* array, int8_t defaultValue);
U_CAPI  void U_EXPORT2 ucmp8_initBogus(CompactByteArray* array);

U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_openAdopt(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_openAlias(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);
U_CAPI  void U_EXPORT2 ucmp8_close(CompactByteArray* array);
U_CAPI  UBool U_EXPORT2 isBogus(const CompactByteArray* array);

#define ucmp8_get(array, index)  (array->fArray[(array->fIndex[index >> UCMP8_kBlockShift] & 0xFFFF) + (index & UCMP8_kBlockMask)])

#define ucmp8_getu(array,index) (uint8_t)ucmp8_get(array,index)


U_CAPI  void U_EXPORT2 ucmp8_set(CompactByteArray* array,
                 UChar character,
                 int8_t value);

U_CAPI  void U_EXPORT2 ucmp8_setRange(CompactByteArray* array, 
                  UChar start,
                  UChar end, 
                  int8_t value);

U_CAPI  int32_t U_EXPORT2 ucmp8_getCount(const CompactByteArray* array);
U_CAPI  const int8_t* U_EXPORT2 ucmp8_getArray(const CompactByteArray* array);
U_CAPI  const uint16_t* U_EXPORT2 ucmp8_getIndex(const CompactByteArray* array);

/* Compact the array.
   The value of cycle determines how large the overlap can be.
   A cycle of 1 is the most compacted, but takes the most time to do.
   If values stored in the array tend to repeat in cycles of, say, 16,
   then using that will be faster than cycle = 1, and get almost the
   same compression.
*/
U_CAPI  void U_EXPORT2 ucmp8_compact(CompactByteArray* array, 
                 uint32_t cycle);

/* Expanded takes the array back to a 65536 element array*/
U_CAPI  void U_EXPORT2 ucmp8_expand(CompactByteArray* array);

/** (more) INTERNAL USE ONLY **/
/* initializes an existing CBA from memory.  Will cause ucmp8_close() to not deallocate anything. */
U_CAPI  void U_EXPORT2 ucmp8_initFromData(CompactByteArray* array, const uint8_t **source, UErrorCode *status);

#endif



