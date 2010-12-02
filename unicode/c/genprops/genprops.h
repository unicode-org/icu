/*
*******************************************************************************
*
*   Copyright (C) 1999-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec13
*   created by: Markus W. Scherer
*/

#ifndef __GENPROPS_H__
#define __GENPROPS_H__

#include "unicode/utypes.h"
#include "utrie.h"
#include "propsvec.h"

/* file definitions */
#define DATA_NAME "uprops"
#define DATA_TYPE "icu"

/* character properties */
typedef struct {
    uint32_t code;
    int32_t numericValue; /* see numericType */
    uint32_t denominator; /* 0: no value */
    uint8_t generalCategory, numericType, exponent;
} Props;

/* global flags */
U_CFUNC UBool beVerbose, haveCopyright;

U_CFUNC const char *const
genCategoryNames[];

/* properties vectors in props2.cpp */
U_CFUNC UPropsVectors *pv;

/* prototypes */
U_CFUNC void
writeUCDFilename(char *basename, const char *filename, const char *suffix);

U_CFUNC UBool
isToken(const char *token, const char *s);

U_CFUNC int32_t
getTokenIndex(const char *const tokens[], int32_t countTokens, const char *s);

U_CFUNC void
setUnicodeVersion(const char *v);

U_CFUNC void
initStore(void);

U_CFUNC void
exitStore(void);

U_CFUNC uint32_t
makeProps(Props *p);

U_CFUNC void
addProps(uint32_t c, uint32_t props);

U_CFUNC uint32_t
getProps(uint32_t c);

U_CFUNC void
repeatProps(uint32_t first, uint32_t last, uint32_t props);

U_CFUNC void
generateData(const char *dataDir, UBool csource);

/* props2.c */
U_CFUNC void
initAdditionalProperties(void);

U_CFUNC void
exitAdditionalProperties(void);

U_CFUNC void
generateAdditionalProperties(char *filename, const char *suffix, UErrorCode *pErrorCode);

U_CFUNC int32_t
writeAdditionalData(FILE *f, uint8_t *p, int32_t capacity, int32_t indexes[16]);

#endif
