/**********************************************************************
* Copyright (C) 1998-2000, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************/
#include "ucmpwrit.h"
#include <stdio.h>

/*
   UCMP8 format:
   
   offset   size                    what
   ---------------------------------------------
   0        4                       ICU_UCMP8_VERSION
   4        4                       count
   8        512*2 = 1024            fIndex [uint16's] (UCMP8_kIndexCount*2)
   1032     1*fCount                fArray [int8_t's]
   +        padding (to extend fCount to the nearest multiple of 4)
*/

/* Sanity check. */
#if (UCMP8_kIndexCount != 512)
# error UCMP8_kIndexCount - changed size. Check to see if different pading needed.
#endif

U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp8 (UNewDataMemory *pData, const CompactByteArray* array)
{
  int32_t size;

    udata_write32(pData, ICU_UCMP8_VERSION);
    size += 4;

    udata_write32(pData, array->fCount);
    size += 4;

    udata_writeBlock(pData, array->fIndex, sizeof(array->fIndex[0])*UCMP8_kIndexCount);
    size += sizeof(array->fIndex[0])*UCMP8_kIndexCount;

    udata_writeBlock(pData, array->fArray, sizeof(array->fArray[0])*array->fCount);
    size += sizeof(array->fArray[0])*array->fCount;

    while(size%4) /* end padding */
    {
        udata_writePadding(pData, 1); /* Pad total so far to even size */
        size += 1;
    }

    return size;
}

/* internal constants*/
#define UCMP16_kMaxUnicode_int 65535
#define UCMP16_kUnicodeCount_int (UCMP16_kMaxUnicode_int + 1)
#define UCMP16_kBlockShift_int 7
#define UCMP16_kBlockCount_int (1 << UCMP16_kBlockShift_int)
#define UCMP16_kBlockBytes_int (UCMP16_kBlockCount_int * sizeof(int16_t))
#define UCMP16_kIndexShift_int (16 - UCMP16_kBlockShift_int)
#define UCMP16_kIndexCount_int (1 << UCMP16_kIndexShift_int)
#define UCMP16_kBlockMask_int (UCMP16_kBlockCount_int - 1)


const int32_t UCMP16_kMaxUnicode = UCMP16_kMaxUnicode_int;
const int32_t UCMP16_kUnicodeCount = UCMP16_kUnicodeCount_int;
const int32_t UCMP16_kBlockShift = UCMP16_kBlockShift_int;
const int32_t UCMP16_kBlockCount = UCMP16_kBlockCount_int;
const int32_t UCMP16_kBlockBytes = UCMP16_kBlockBytes_int;
const int32_t UCMP16_kIndexShift = UCMP16_kIndexShift_int;
const int32_t UCMP16_kIndexCount = UCMP16_kIndexCount_int;
const uint32_t UCMP16_kBlockMask = UCMP16_kBlockMask_int;

/*
   UCMP16 format:
   
   offset   size                    what
   ---------------------------------------------
   0        4                       ICU_UCMP16_VERSION
   4        4                       count
   8        4                       blockShift
   12       4                       blockMask
   16       512*2 = 1024            fIndex [uint16's] (UCMP16_kIndexCount*2)
   1032     1*fCount                fArray [int16's]
   +        padding (to extend fCount to the nearest multiple of 4)
  
 */

#if (UCMP16_kIndexCount_int != 512)
# error UCMP16_kIndexCount - changed size. Check to see if different pading needed.
#endif

U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp16 (UNewDataMemory *pData, const CompactShortArray* array)
{
  int32_t size;

  udata_write32(pData, ICU_UCMP16_VERSION);
  size += 4;
  
  udata_write32(pData, array->fCount);
  size += 4;
  
  udata_write32(pData, array->kBlockShift);
  size += 4;
  
  udata_write32(pData, array->kBlockMask);
  size += 4;
  
  udata_writeBlock(pData, array->fIndex, sizeof(array->fIndex[0])*UCMP16_kIndexCount);
  size += sizeof(array->fIndex[0])*UCMP16_kIndexCount;
  
  udata_writeBlock(pData, array->fArray, sizeof(array->fArray[0])*array->fCount);
  size += sizeof(array->fArray[0])*array->fCount;
  
  while(size%4) /* end padding */
    {
      udata_writePadding(pData, 1); /* Pad total so far to even size */
      size += 1;
    }

  return size;
}








