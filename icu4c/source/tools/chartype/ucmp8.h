
/*
 ********************************************************************
 *
 *   Copyright (C) 1996-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 ********************************************************************
 */



#ifndef UCMP8_H
#define UCMP8_H


#include "utypes.h"

/*====================================*/
/* class CompactByteArray
 * Provides a compact way to store information that is indexed by Unicode values,
 * such as character properties, types, keyboard values, etc.
 * The ATypes are used by value, so should be small, integers or pointers.
 *====================================
 */

U_CAPI  const int32_t UCMP8_kUnicodeCount;
U_CAPI  const int32_t UCMP8_kBlockShift;
U_CAPI  const int32_t UCMP8_kBlockCount;
U_CAPI  const int32_t UCMP8_kIndexShift;
U_CAPI  const int32_t UCMP8_kIndexCount;
U_CAPI  const uint32_t UCMP8_kBlockMask;

U_CAPI int32_t ucmp8_getkUnicodeCount(void);
U_CAPI int32_t ucmp8_getkBlockCount(void);
U_CAPI int32_t ucmp8_getkIndexCount(void);
typedef struct{
  int8_t* fArray;
  uint16_t* fIndex;
  int32_t fCount;
  bool_t fCompact; 
  bool_t fBogus;
} CompactByteArray;

U_CAPI  CompactByteArray* ucmp8_open(int8_t defaultValue);
U_CAPI  CompactByteArray* ucmp8_openAdopt(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);
U_CAPI  void ucmp8_close(CompactByteArray* array);
U_CAPI  bool_t isBogus(const CompactByteArray* array);


U_CAPI int8_t ucmp8_get(CompactByteArray* array, uint16_t index); 
U_CAPI uint8_t ucmp8_getu(CompactByteArray* array, uint16_t index);

U_CAPI  void ucmp8_set(CompactByteArray* array,
                 UChar index,
                 int8_t value);

U_CAPI  void ucmp8_setRange(CompactByteArray* array, 
                  UChar start,
                  UChar end, 
                  int8_t value);

U_CAPI  int32_t ucmp8_getCount(const CompactByteArray* array);
U_CAPI  const int8_t* ucmp8_getArray(const CompactByteArray* array);
U_CAPI  const uint16_t* ucmp8_getIndex(const CompactByteArray* array);

/* Compact the array.
   The value of cycle determines how large the overlap can be.
   A cycle of 1 is the most compacted, but takes the most time to do.
   If values stored in the array tend to repeat in cycles of, say, 16,
   then using that will be faster than cycle = 1, and get almost the
   same compression.
*/
U_CAPI  void ucmp8_compact(CompactByteArray* array, 
                 uint32_t cycle);

/* Expanded takes the array back to a 65536 element array*/
U_CAPI  void ucmp8_expand(CompactByteArray* array);



#endif



