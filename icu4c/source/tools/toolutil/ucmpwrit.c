#include "ucmpwrit.h"
#include <stdio.h>

U_CAPI  void U_EXPORT2 udata_write_ucmp8 (UNewDataMemory *pData, const CompactByteArray* array)
{
    udata_writeBlock(pData, array, sizeof(*array));
    udata_writeBlock(pData, array->fArray, sizeof(array->fArray[0])*array->fCount);
    udata_writeBlock(pData, array->fIndex, sizeof(array->fIndex[0])*UCMP8_kIndexCount);
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


U_CAPI  void U_EXPORT2 udata_write_ucmp16 (UNewDataMemory *pData, const CompactShortArray* array)
{
    udata_writeBlock(pData, array, sizeof(*array));
    udata_writeBlock(pData, array->fArray, sizeof(array->fArray[0])*array->fCount);
    udata_writeBlock(pData, array->fIndex, sizeof(array->fIndex[0])*UCMP16_kIndexCount);
}








