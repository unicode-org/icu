/*
*******************************************************************************
*
*   Copyright (C) 1999-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gennorm.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001may25
*   created by: Markus W. Scherer
*/

#ifndef __GENPROPS_H__
#define __GENPROPS_H__

#include "unicode/utypes.h"

/*
 * data structure that holds the normalization properties for one or more
 * code point(s) at build time
 */
typedef struct Norm {
    uint8_t udataCC, lenNFD, lenNFKD;
    uint32_t *nfd, *nfkd;
} Norm;

/* global flags */
extern UBool beVerbose;

/* prototypes */
extern void
init(void);

extern void
storeNorm(uint32_t code, Norm *norm);

extern void
setCompositionExclusion(uint32_t code);

extern void
writeNorm2(const char *dataDir);

extern void
cleanUpData(void);

#endif
