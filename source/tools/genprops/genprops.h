/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
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

/* file definitions */
#define DATA_NAME "uprops"
#define DATA_TYPE "dat"

/* special casing data */
typedef struct {
    uint32_t code;
    UBool isComplex;
    UChar lowerCase[32], upperCase[32], titleCase[32];
} SpecialCasing;

/* case folding data */
typedef struct {
    uint32_t code, simple;
    char status;
    UChar full[32];
} CaseFolding;

/* character properties */
typedef struct {
    uint32_t code, lowerCase, upperCase, titleCase, mirrorMapping;
    int16_t decimalDigitValue, digitValue; /* -1: no value */
    int32_t numericValue; /* see hasNumericValue */
    uint32_t denominator; /* 0: no value */
    uint8_t generalCategory, canonicalCombining, bidi, isMirrored, hasNumericValue;
    SpecialCasing *specialCasing;
    CaseFolding *caseFolding;
} Props;

/* global flags */
extern UBool beVerbose, haveCopyright;

/* name tables */
extern const char *const
bidiNames[];

extern const char *const
genCategoryNames[];

/* prototypes */
extern void
setUnicodeVersion(const char *v);

extern void
initStore(void);

extern uint32_t
makeProps(Props *p);

extern void
addProps(uint32_t c, uint32_t props);

extern void
repeatProps(uint32_t first, uint32_t last, uint32_t props);

extern void
compactStage2(void);

extern void
compactStage3(void);

extern void
compactProps(void);

extern void
generateData(const char *dataDir);

#endif

