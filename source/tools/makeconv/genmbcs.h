/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genmbcs.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul10
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unewdata.h"

/* exports from makeconv.c */
U_CFUNC UBool VERBOSE;

/* exports from genmbcs.c */
struct MBCSData;
typedef struct MBCSData MBCSData;

U_CFUNC  MBCSData *
MBCSOpen(uint8_t maxCharLength);

U_CFUNC  void
MBCSClose(MBCSData *mbcsData);

U_CFUNC  UBool
MBCSAddState(MBCSData *mbcsData, const char *s);

U_CFUNC  UBool
MBCSProcessStates(MBCSData *mbcsData);

U_CFUNC  UBool
MBCSAddToUnicode(MBCSData *mbcsData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c,
                 UBool isFallback);

U_CFUNC  UBool
MBCSAddFromUnicode(MBCSData *mbcsData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c,
                   UBool isFallback);

U_CFUNC  void
MBCSPostprocess(MBCSData *mbcsData);

U_CFUNC  uint32_t
MBCSWrite(MBCSData *mbcsData, UNewDataMemory *pData);



