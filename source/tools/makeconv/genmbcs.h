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
extern UBool VERBOSE;

/* exports from genmbcs.c */
struct MBCSData;
typedef struct MBCSData MBCSData;

extern MBCSData *
MBCSOpen(uint8_t maxCharLength);

extern void
MBCSClose(MBCSData *mbcsData);

extern UBool
MBCSAddState(MBCSData *mbcsData, const char *s);

extern UBool
MBCSProcessStates(MBCSData *mbcsData);

extern UBool
MBCSAddToUnicode(MBCSData *mbcsData,
                 const uint8_t *bytes, int32_t length,
                 UChar32 c,
                 UBool isFallback);

extern UBool
MBCSAddFromUnicode(MBCSData *mbcsData,
                   const uint8_t *bytes, int32_t length,
                   UChar32 c,
                   UBool isFallback);

extern void
MBCSPostprocess(MBCSData *mbcsData);

extern uint32_t
MBCSWrite(MBCSData *mbcsData, UNewDataMemory *pData);
