/*
*******************************************************************************
*
*   Copyright (C) 2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gencase.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004aug28
*   created by: Markus W. Scherer
*/

#ifndef __GENCASE_H__
#define __GENCASE_H__

#include "unicode/utypes.h"
#include "utrie.h"
#include "ucase.h"

U_CDECL_BEGIN

/* gencase ------------------------------------------------------------------ */

#define UGENCASE_EXC_SHIFT     16
#define UGENCASE_EXC_MASK      0xffff0000

/* special casing data */
typedef struct {
    UChar32 code;
    UBool isComplex;
    UChar lowerCase[32], upperCase[32], titleCase[32];
} SpecialCasing;

/* case folding data */
typedef struct {
    UChar32 code, simple;
    char status;
    UChar full[32];
} CaseFolding;

/* case mapping properties */
typedef struct {
    UChar32 code, lowerCase, upperCase, titleCase;
    SpecialCasing *specialCasing;
    CaseFolding *caseFolding;
    uint8_t gc, cc;
} Props;

/* global flags */
extern UBool beVerbose, haveCopyright;

/* properties vectors in gencase.c */
extern uint32_t *pv;

/* prototypes */
U_CFUNC void
writeUCDFilename(char *basename, const char *filename, const char *suffix);

U_CFUNC UBool
isToken(const char *token, const char *s);

extern void
setUnicodeVersion(const char *v);

extern void
setProps(Props *p);

U_CFUNC uint32_t U_EXPORT2
getFoldedPropsValue(UNewTrie *trie, UChar32 start, int32_t offset);

extern void
addCaseSensitive(UChar32 first, UChar32 last);

extern void
makeCaseClosure(void);

extern void
makeExceptions(void);

extern void
generateData(const char *dataDir);

U_CDECL_END

#endif
