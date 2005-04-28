/*
*******************************************************************************
*
*   Copyright (C) 2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  writesrc.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005apr23
*   created by: Markus W. Scherer
*
*   Helper functions for writing source code for data.
*/

#ifndef __WRITESRC_H__
#define __WRITESRC_H__

#include <stdio.h>
#include "unicode/utypes.h"
#include "utrie.h"

U_CAPI FILE * U_EXPORT2
usrc_create(const char *path, const char *filename);

U_CAPI void U_EXPORT2
usrc_writeArray(FILE *f,
                const char *prefix,
                const void *p, int32_t width, int32_t length,
                const char *postfix);

U_CAPI void U_EXPORT2
usrc_writeUTrieArrays(FILE *f,
                      const char *indexPrefix, const char *dataPrefix,
                      const UTrie *pTrie,
                      const char *postfix);

U_CAPI void U_EXPORT2
usrc_writeUTrieStruct(FILE *f,
                      const char *prefix,
                      const UTrie *pTrie,
                      const char *indexName, const char *dataName,
                      const char *getFoldingOffsetName,
                      const char *postfix);

#endif
