/*  
******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  ubidiimp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999aug06
*   created by: Markus W. Scherer
*/

#ifndef UBIDIIMP_H
#define UBIDIIMP_H

/* set import/export definitions */
#ifdef U_COMMON_IMPLEMENTATION

#include "unicode/utypes.h"
#include "unicode/uchar.h"

/* miscellaneous definitions ---------------------------------------------- */

typedef uint8_t DirProp;
typedef uint32_t Flags;

/*  Comparing the description of the BiDi algorithm with this implementation
    is easier with the same names for the BiDi types in the code as there.
    See UCharDirection in uchar.h .
*/
enum { 
    L=  U_LEFT_TO_RIGHT,
    R=  U_RIGHT_TO_LEFT,
    EN= U_EUROPEAN_NUMBER,
    ES= U_EUROPEAN_NUMBER_SEPARATOR,
    ET= U_EUROPEAN_NUMBER_TERMINATOR,
    AN= U_ARABIC_NUMBER,
    CS= U_COMMON_NUMBER_SEPARATOR,
    B=  U_BLOCK_SEPARATOR,
    S=  U_SEGMENT_SEPARATOR,
    WS= U_WHITE_SPACE_NEUTRAL,
    ON= U_OTHER_NEUTRAL,
    LRE=U_LEFT_TO_RIGHT_EMBEDDING,
    LRO=U_LEFT_TO_RIGHT_OVERRIDE,
    AL= U_RIGHT_TO_LEFT_ARABIC,
    RLE=U_RIGHT_TO_LEFT_EMBEDDING,
    RLO=U_RIGHT_TO_LEFT_OVERRIDE,
    PDF=U_POP_DIRECTIONAL_FORMAT,
    NSM=U_DIR_NON_SPACING_MARK,
    BN= U_BOUNDARY_NEUTRAL,
    dirPropCount
};

/*
 * Sometimes, bit values are more appropriate
 * to deal with directionality properties.
 * Abbreviations in these macro names refer to names
 * used in the BiDi algorithm.
 */
#define DIRPROP_FLAG(dir) (1UL<<(dir))

/* special flag for multiple runs from explicit embedding codes */
#define DIRPROP_FLAG_MULTI_RUNS (1UL<<31)

/* are there any characters that are LTR or RTL? */
#define MASK_LTR (DIRPROP_FLAG(L)|DIRPROP_FLAG(EN)|DIRPROP_FLAG(AN)|DIRPROP_FLAG(LRE)|DIRPROP_FLAG(LRO))
#define MASK_RTL (DIRPROP_FLAG(R)|DIRPROP_FLAG(AL)|DIRPROP_FLAG(RLE)|DIRPROP_FLAG(RLO))

/* explicit embedding codes */
#define MASK_LRX (DIRPROP_FLAG(LRE)|DIRPROP_FLAG(LRO))
#define MASK_RLX (DIRPROP_FLAG(RLE)|DIRPROP_FLAG(RLO))
#define MASK_OVERRIDE (DIRPROP_FLAG(LRO)|DIRPROP_FLAG(RLO))

#define MASK_EXPLICIT (MASK_LRX|MASK_RLX|DIRPROP_FLAG(PDF))
#define MASK_BN_EXPLICIT (DIRPROP_FLAG(BN)|MASK_EXPLICIT)

/* paragraph and segment separators */
#define MASK_B_S (DIRPROP_FLAG(B)|DIRPROP_FLAG(S))

/* all types that are counted as White Space or Neutral in some steps */
#define MASK_WS (MASK_B_S|DIRPROP_FLAG(WS)|MASK_BN_EXPLICIT)
#define MASK_N (DIRPROP_FLAG(ON)|MASK_WS)

/* all types that are included in a sequence of European Terminators for (W5) */
#define MASK_ET_NSM_BN (DIRPROP_FLAG(ET)|DIRPROP_FLAG(NSM)|MASK_BN_EXPLICIT)

/* types that are neutrals or could becomes neutrals in (Wn) */
#define MASK_POSSIBLE_N (DIRPROP_FLAG(CS)|DIRPROP_FLAG(ES)|DIRPROP_FLAG(ET)|MASK_N)

/*
 * These types may be changed to "e",
 * the embedding type (L or R) of the run,
 * in the BiDi algorithm (N2)
 */
#define MASK_EMBEDDING (DIRPROP_FLAG(NSM)|MASK_POSSIBLE_N)

/* the dirProp's L and R are defined to 0 and 1 values in UCharDirection */
#define GET_LR_FROM_LEVEL(level) ((DirProp)((level)&1))

#define IS_DEFAULT_LEVEL(level) (((level)&0xfe)==0xfe)

/* Run structure for reordering --------------------------------------------- */

typedef struct Run {
    int32_t logicalStart,   /* first character of the run; b31 indicates even/odd level */
                visualLimit;    /* last visual position of the run +1 */
} Run;

/* in a Run, logicalStart will get this bit set if the run level is odd */
#define INDEX_ODD_BIT (1UL<<31)

#define MAKE_INDEX_ODD_PAIR(index, level) (index|((int32_t)level<<31))
#define ADD_ODD_BIT_FROM_LEVEL(x, level)  ((x)|=((int32_t)level<<31))
#define REMOVE_ODD_BIT(x)                 ((x)&=~INDEX_ODD_BIT)

#define GET_INDEX(x)   (x&~INDEX_ODD_BIT)
#define GET_ODD_BIT(x) ((uint32_t)x>>31)
#define IS_ODD_RUN(x)  ((x&INDEX_ODD_BIT)!=0)
#define IS_EVEN_RUN(x) ((x&INDEX_ODD_BIT)==0)

U_CFUNC UBool
ubidi_getRuns(UBiDi *pBiDi);

/* UBiDi structure ----------------------------------------------------------- */

struct UBiDi {
    /* alias pointer to the current text */
    const UChar *text;

    /* length of the current text */
    int32_t length;

    /* memory sizes in bytes */
    int32_t dirPropsSize, levelsSize, runsSize;

    /* allocated memory */
    DirProp *dirPropsMemory;
    UBiDiLevel *levelsMemory;
    Run *runsMemory;

    /* indicators for whether memory may be allocated after ubidi_open() */
    UBool mayAllocateText, mayAllocateRuns;

    /* arrays with one value per text-character */
    const DirProp *dirProps;
    UBiDiLevel *levels;

    /* are we performing an approximation of the "inverse BiDi" algorithm? */
    UBool isInverse;

    /* the paragraph level */
    UBiDiLevel paraLevel;

    /* the overall paragraph or line directionality - see UBiDiDirection */
    UBiDiDirection direction;

    /* flags is a bit set for which directional properties are in the text */
    Flags flags;

    /* characters after trailingWSStart are WS and are */
    /* implicitly at the paraLevel (rule (L1)) - levels may not reflect that */
    int32_t trailingWSStart;

    /* fields for line reordering */
    int32_t runCount;     /* ==-1: runs not set up yet */
    Run *runs;

    /* for non-mixed text, we only need a tiny array of runs (no malloc()) */
    Run simpleRuns[1];
};

/* helper function to (re)allocate memory if allowed */
U_CFUNC UBool
ubidi_getMemory(void **pMemory, int32_t *pSize, UBool mayAllocate, int32_t sizeNeeded);

/* helper macros for each allocated array in UBiDi */
#define getDirPropsMemory(pBiDi, length) \
        ubidi_getMemory((void **)&(pBiDi)->dirPropsMemory, &(pBiDi)->dirPropsSize, \
                        (pBiDi)->mayAllocateText, (length))

#define getLevelsMemory(pBiDi, length) \
        ubidi_getMemory((void **)&(pBiDi)->levelsMemory, &(pBiDi)->levelsSize, \
                        (pBiDi)->mayAllocateText, (length))

#define getRunsMemory(pBiDi, length) \
        ubidi_getMemory((void **)&(pBiDi)->runsMemory, &(pBiDi)->runsSize, \
                        (pBiDi)->mayAllocateRuns, (length)*sizeof(Run))

/* additional macros used by ubidi_open() - always allow allocation */
#define getInitialDirPropsMemory(pBiDi, length) \
        ubidi_getMemory((void **)&(pBiDi)->dirPropsMemory, &(pBiDi)->dirPropsSize, \
                        TRUE, (length))

#define getInitialLevelsMemory(pBiDi, length) \
        ubidi_getMemory((void **)&(pBiDi)->levelsMemory, &(pBiDi)->levelsSize, \
                        TRUE, (length))

#define getInitialRunsMemory(pBiDi, length) \
        ubidi_getMemory((void **)&(pBiDi)->runsMemory, &(pBiDi)->runsSize, \
                        TRUE, (length)*sizeof(Run))

#endif

#endif
