/*
*******************************************************************************
*
*   Copyright (C) 2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.h
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

U_CDECL_BEGIN

/* file definitions --------------------------------------------------------- */

#define UCASE_DATA_NAME "ucase"
#define UCASE_DATA_TYPE "icu"

/* format "cAsE" */
#define UCASE_FMT_0 0x63
#define UCASE_FMT_1 0x41
#define UCASE_FMT_2 0x53
#define UCASE_FMT_3 0x45

/* indexes into indexes[] */
enum {
    UCASE_IX_INDEX_TOP,
    UCASE_IX_LENGTH,
    UCASE_IX_TRIE_SIZE,
    UCASE_IX_EXC_LENGTH,

    UCASE_IX_TOP=16
};

/* definitions for 16-bit case properties word ------------------------------ */

/* 2-bit constants for types of cased characters */
#define UCASE_TYPE_MASK     3
enum {
    UCASE_NONE,
    UCASE_LOWER,
    UCASE_UPPER,
    UCASE_TITLE
};

#define UCASE_SENSITIVE     4
#define UCASE_EXCEPTION     8

#define UCASE_DOT_MASK      0x30
enum {
    UCASE_NO_DOT=0,
    UCASE_SOFT_DOTTED=0x10,
    UCASE_ABOVE=0x20,       /* "above" accents with cc=230 */
    UCASE_OTHER_ACCENT=0x30 /* other character (0<cc!=230) */
};

/* no exception: bits 15..6 are a 10-bit signed case mapping delta */
#define UCASE_DELTA_SHIFT   6
#define UCASE_DELTA_MASK    0xffc0
#define UCASE_MAX_DELTA     0x1ff
#define UCASE_MIN_DELTA     (-UCASE_MAX_DELTA-1)

#define UCASE_GET_DELTA(props) ((int16_t)(props)>>UCASE_DELTA_SHIFT)

/* exception: bits 15..4 are an unsigned 12-bit index into the exceptions array */
#define UCASE_EXC_SHIFT     4
#define UCASE_EXC_MASK      0xfff0
#define UCASE_MAX_EXCEPTIONS 0x1000

/* definitions for 16-bit main exceptions word ------------------------------ */

/* first 8 bits indicate values in optional slots */
enum {
    UCASE_EXC_LOWER,
    UCASE_EXC_FOLD,
    UCASE_EXC_UPPER,
    UCASE_EXC_TITLE,
    UCASE_EXC_4,            /* reserved */
    UCASE_EXC_5,            /* reserved */
    UCASE_EXC_6,            /* reserved */
    UCASE_EXC_FULL_MAPPINGS
};

/* each slot is 2 uint16_t instead of 1 */
#define UCASE_EXC_DOUBLE_SLOTS      0x100

/* reserved: exception bits 11..9 */

/* UCASE_EXC_DOT_MASK=UCASE_DOT_MASK<<UCASE_EXC_DOT_SHIFT */
#define UCASE_EXC_DOT_SHIFT     8

/* normally stored in the main word, but pushed out for larger exception indexes */
#define UCASE_EXC_DOT_MASK      0x3000
enum {
    UCASE_EXC_NO_DOT=0,
    UCASE_EXC_SOFT_DOTTED=0x1000,
    UCASE_EXC_ABOVE=0x2000,         /* "above" accents with cc=230 */
    UCASE_EXC_OTHER_ACCENT=0x3000   /* other character (0<cc!=230) */
};

/* complex/conditional mappings */
#define UCASE_EXC_CONDITIONAL_SPECIAL   0x4000
#define UCASE_EXC_CONDITIONAL_FOLD      0x8000

/* definitions for lengths word for full case mappings */
#define UCASE_FULL_LOWER    0xf
#define UCASE_FULL_FOLDING  0xf0
#define UCASE_FULL_UPPER    0xf00
#define UCASE_FULL_TITLE    0xf000

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
    uint8_t cc;
    UBool isTitle;
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

