#ifndef UCOL_BLD_H
#define UCOL_BLD_H
#include <stdio.h>
#include <stdlib.h>

#include "ucol_imp.h"
#include "ucol_tok.h"
#include "ucol_elm.h"
#include "ucol_wgt.h"

#include "uhash.h"
#include "ucmp16.h"
#include "umutex.h"
#include "cpputils.h"

#include "unicode/ustring.h"
#include "unicode/unistr.h"
#include "unicode/normlzr.h"


const InverseTableHeader *ucol_initInverseUCA(UErrorCode *status);
UCATableHeader *ucol_assembleTailoringTable(UColTokenParser *src, UErrorCode *status);

typedef struct {
  WeightRange ranges[7];
  int32_t noOfRanges;
  uint32_t byteSize; uint32_t start; uint32_t limit;
  int32_t maxCount;
  int32_t count;
  uint32_t current;
  uint32_t fLow; /*forbidden Low */
  uint32_t fHigh; /*forbidden High */
} ucolCEGenerator;

#endif