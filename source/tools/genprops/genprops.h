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

/* character properties */
typedef struct {
    uint32_t code, lowerCase, upperCase, titleCase, mirrorMapping;
    /* ### uint32_t decomp[16]; */
    uint32_t numericValue, denominator;
    uint8_t generalCategory, canonicalCombining, bidi, isMirrored;
} Props;

/* global flags */
extern bool_t beVerbose, haveCopyright;

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

extern void
addProps(Props *p);

extern void
repeatProps(void);

extern void
compactStage2(void);

extern void
compactStage3(void);

extern void
compactProps(void);

extern void
generateData(const char *dataDir);

#endif

