
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

U_CAPI int32_t U_EXPORT2 ucmp8_getkUnicodeCount(void);
U_CAPI int32_t U_EXPORT2 ucmp8_getkBlockCount(void);

typedef struct CompactByteArray {
  int8_t* fArray;
  uint16_t* fIndex;
  int32_t fCount;
  bool_t fCompact; 
  bool_t fBogus;
} CompactByteArray;

U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_open(int8_t defaultValue);
U_CAPI  CompactByteArray* U_EXPORT2 ucmp8_openAdopt(uint16_t* indexArray, 
                               int8_t* newValues,
                               int32_t count);
U_CAPI  void U_EXPORT2 ucmp8_close(CompactByteArray* array);
U_CAPI  bool_t U_EXPORT2 isBogus(const CompactByteArray* array);


U_CAPI int8_t U_EXPORT2 ucmp8_get(CompactByteArray* array, uint16_t index); 
U_CAPI uint8_t U_EXPORT2 ucmp8_getu(CompactByteArray* array, uint16_t index);

U_CAPI  void U_EXPORT2 ucmp8_set(CompactByteArray* array,
                 UChar index,
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



#endif



