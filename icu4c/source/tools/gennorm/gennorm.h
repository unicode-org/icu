/*
*******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
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
#define DATA_NAME "unorm"
#define DATA_TYPE "dat"

/*
 * data structure that holds the normalization properties for one or more
 * code point(s) at build time
 */
typedef struct Norm {
    uint8_t udataCC, lenNFD, lenNFKD;
    uint8_t qcFlags, combiningFlags;
    uint16_t canonBothCCs, compatBothCCs, combiningIndex, specialTag;
    uint32_t *nfd, *nfkd;
} Norm;

/* global flags */
extern UBool beVerbose, haveCopyright;

/* prototypes */
extern void
setUnicodeVersion(const char *v);

extern void
init(void);

extern void
storeNorm(uint32_t code, Norm *norm);

extern void
setQCFlags(uint32_t code, uint8_t qcFlags);

extern void
setCompositionExclusion(uint32_t code);

extern void
processData(void);

extern void
generateData(const char *dataDir);

#endif

