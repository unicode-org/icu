
/*
 ********************************************************************
 * COPYRIGHT: 
 * (C) Copyright Taligent, Inc., 1996
 * (C) Copyright International Business Machines Corporation, 1996 - 1998
 * Licensed Material - Program-Property of IBM - All Rights Reserved. 
 * US Government Users Restricted Rights - Use, duplication, or disclosure 
 * restricted by GSA ADP Schedule Contract with IBM Corp. 
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

CAPI  const int32_t UCMP8_kUnicodeCount;
CAPI  const int32_t UCMP8_kBlockShift;
CAPI  const int32_t UCMP8_kBlockCount;
CAPI  const int32_t UCMP8_kIndexShift;
CAPI  const int32_t UCMP8_kIndexCount;
CAPI  const uint32_t UCMP8_kBlockMask;

CAPI int32_t ucmp8_getkUnicodeCount(void);
CAPI int32_t ucmp8_getkBlockCount(void);
CAPI int32_t ucmp8_getkIndexCount(void);
typedef struct{
  int8_t* fArray;
  uint16_t* fIndex;
  int32_t fCount;
  bool_t fCompact; 
  bool_t fBogus;
} CompactByteArray;

CAPI  CompactByteArray* ucmp8_open(int8_t defaultValue);
CAPI  CompactByteArray* ucmp8_openAdopt(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);
CAPI  void ucmp8_close(CompactByteArray* array);
CAPI  bool_t isBogus(const CompactByteArray* array);


CAPI int8_t ucmp8_get(CompactByteArray* array, uint16_t index); 
CAPI uint8_t ucmp8_getu(CompactByteArray* array, uint16_t index);

CAPI  void ucmp8_set(CompactByteArray* array,
                 UChar index,
                 int8_t value);

CAPI  void ucmp8_setRange(CompactByteArray* array, 
                  UChar start,
                  UChar end, 
                  int8_t value);

CAPI  int32_t ucmp8_getCount(const CompactByteArray* array);
CAPI  const int8_t* ucmp8_getArray(const CompactByteArray* array);
CAPI  const uint16_t* ucmp8_getIndex(const CompactByteArray* array);

/* Compact the array.
   The value of cycle determines how large the overlap can be.
   A cycle of 1 is the most compacted, but takes the most time to do.
   If values stored in the array tend to repeat in cycles of, say, 16,
   then using that will be faster than cycle = 1, and get almost the
   same compression.
*/
CAPI  void ucmp8_compact(CompactByteArray* array, 
                 uint32_t cycle);

/* Expanded takes the array back to a 65536 element array*/
CAPI  void ucmp8_expand(CompactByteArray* array);



#endif



