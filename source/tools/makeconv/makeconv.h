/*
*******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  makeconv.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000nov01
*   created by: Markus W. Scherer
*/

#ifndef __MAKECONV_H__
#define __MAKECONV_H__

#include "unicode/utypes.h"
#include "ucnv_bld.h"
#include "unewdata.h"

/* exports from makeconv.c */
U_CFUNC UBool VERBOSE;

/* abstract converter generator struct, C++ - style */
struct NewConverter;
typedef struct NewConverter NewConverter;

struct NewConverter {
    void
    (*close)(NewConverter *cnvData);

    UBool
    (*startMappings)(NewConverter *cnvData);

    /** is this byte sequence valid? */
    UBool
    (*isValid)(NewConverter *cnvData,
               const uint8_t *bytes, int32_t length,
               uint32_t b);

    UBool
    (*addToUnicode)(NewConverter *cnvData,
                    const uint8_t *bytes, int32_t length,
                    UChar32 c, uint32_t b,
                    int8_t isFallback);

    UBool
    (*addFromUnicode)(NewConverter *cnvData,
                      const uint8_t *bytes, int32_t length,
                      UChar32 c, uint32_t b,
                      int8_t isFallback);

    void
    (*finishMappings)(NewConverter *cnvData, const UConverterStaticData *staticData);

    uint32_t
    (*write)(NewConverter *cnvData, const UConverterStaticData *staticData, UNewDataMemory *pData);
};

#endif
