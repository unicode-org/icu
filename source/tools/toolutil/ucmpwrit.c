/**********************************************************************
* Copyright (C) 1998-2000, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************/
#include "ucmpwrit.h"
#include <stdio.h>

U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp8 (UNewDataMemory *pData, const CompactByteArray* array)
{
    uint32_t size = sizeof(*array);

    udata_writeBlock(pData, array, sizeof(*array));
    udata_writeBlock(pData, array->fArray, sizeof(array->fArray[0])*array->fCount);
    size += sizeof(array->fArray[0])*array->fCount;

    if((sizeof(*array)+(sizeof(array->fArray[0])*array->fCount))&1)
    {
        udata_writePadding(pData, 1); /* Pad total so far to even size */
        size += 1;
    }

    udata_writeBlock(pData, array->fIndex, sizeof(array->fIndex[0])*UCMP8_kIndexCount);
    size +=  sizeof(array->fIndex[0])*UCMP8_kIndexCount;

    
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


U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp16 (UNewDataMemory *pData, const CompactShortArray* array)
{
    uint32_t size = sizeof(*array);

    udata_writeBlock(pData, array, sizeof(*array));

    if(sizeof(*array)&1)
    {
        udata_writePadding(pData, 1); /* Pad to even size */
        size++;
    }
    
    udata_writeBlock(pData, array->fArray, sizeof(array->fArray[0])*array->fCount);
    size +=  sizeof(array->fArray[0])*array->fCount;
    udata_writeBlock(pData, array->fIndex, sizeof(array->fIndex[0])*UCMP16_kIndexCount);
    size += sizeof(array->fIndex[0])*UCMP16_kIndexCount;
    
    while(size%4) /* end padding */
    {
        udata_writePadding(pData, 1); /* Pad total so far to even size */
        size += 1;
    }

    return size;
}








