/*
*******************************************************************************
*
*   Copyright (C) 2009-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  normalizer2impl.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009nov22
*   created by: Markus W. Scherer
*   modified by Steven R. Loomis
*/

#include "utrie2.h"

#ifndef _NORM2IMP
#define _NORM2IMP

typedef struct Normalizer2  {
  void (U_EXPORT2 *close) (struct Normalizer2* n);
  UNormalizationCheckResult (U_EXPORT2 *quickCheck) (struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode);
  int32_t (U_EXPORT2 *normalize) (struct Normalizer2 *n,
                                  const UChar *src, int32_t length,
                                  UChar *dest, int32_t capacity,
                                  UErrorCode *pErrorCode);
  UBool (U_EXPORT2 *isNormalized) (struct Normalizer2 *n,
                                  const UChar *src, int32_t length,
                                  UErrorCode *pErrorCode);
  UNormalizationCheckResult (U_EXPORT2 *getQuickCheck) (struct Normalizer2 *n,
                                                        const UChar32 c);

  /* from normalizer2impl.h */
  UDataMemory *memory;
  UVersionInfo dataVersion;

  /* Code point thresholds for quick check codes. */
  UChar32 minDecompNoCP;
  UChar32 minCompNoMaybeCP;

  /* Norm16 value thresholds for quick check combinations and types of extra data. */
  uint16_t minYesNo;
  uint16_t minNoNo;
  uint16_t limitNoNo;
  uint16_t minMaybeYes;

  UTrie2 *normTrie;
  const uint16_t *maybeYesCompositions;
  const uint16_t *extraData;  /* mappings and/or compositions for yesYes, yesNo & noNo characters */

#if UNORM_ENABLE_FCD
  /* FCD only */
  UTrie2 *newFCDTrie;
  UErrorCode fcdErrorCode;
#endif

  UBool onlyContiguous; /* FCD vs FCC? */


  /* ICU4C0 */
  UNormalization2Mode mode;
} Normalizer2;


typedef struct {
  UChar *start, *reorderStart, *limit;
  int32_t capacity, remainingCapacity;
  uint8_t lastCC;
  Normalizer2 *impl;
  UChar *str;
  UChar *codePointStart, *codePointLimit;
  
} ReorderingBuffer;


    enum {
        MIN_CCC_LCCC_CP=0x300
    };

    enum {
        MIN_YES_YES_WITH_CC=0xff01,
        JAMO_VT=0xff00,
        MIN_NORMAL_MAYBE_YES=0xfe00,
        JAMO_L=1,
        MAX_DELTA=0x40
    };

    enum {
      /* Byte offsets from the start of the data, after the generic header. */
        IX_NORM_TRIE_OFFSET,
        IX_EXTRA_DATA_OFFSET,
        IX_RESERVED2_OFFSET,
        IX_RESERVED3_OFFSET,
        IX_RESERVED4_OFFSET,
        IX_RESERVED5_OFFSET,
        IX_RESERVED6_OFFSET,
        IX_TOTAL_SIZE,

        /* Code point thresholds for quick check codes. */
        IX_MIN_DECOMP_NO_CP,
        IX_MIN_COMP_NO_MAYBE_CP,

        /* Norm16 value thresholds for quick check combinations and types of extra data. */
        IX_MIN_YES_NO,
        IX_MIN_NO_NO,
        IX_LIMIT_NO_NO,
        IX_MIN_MAYBE_YES,

        IX_RESERVED14,
        IX_RESERVED15,
        IX_COUNT
    };

    enum {
        MAPPING_HAS_CCC_LCCC_WORD=0x80,
        MAPPING_PLUS_COMPOSITION_LIST=0x40,
        MAPPING_NO_COMP_BOUNDARY_AFTER=0x20,
        MAPPING_LENGTH_MASK=0x1f
    };

    enum {
        COMP_1_LAST_TUPLE=0x8000,
        COMP_1_TRIPLE=1,
        COMP_1_TRAIL_LIMIT=0x3400,
        COMP_1_TRAIL_MASK=0x7ffe,
        COMP_1_TRAIL_SHIFT=9,  /* 10-1 for the "triple" bit */
        COMP_2_TRAIL_SHIFT=6,
        COMP_2_TRAIL_MASK=0xffc0
    };

    /* Korean Hangul and Jamo constants */
    enum {
        JAMO_L_BASE=0x1100,     /* "lead" jamo */
        JAMO_V_BASE=0x1161,     /* "vowel" jamo */
        JAMO_T_BASE=0x11a7,     /* "trail" jamo */

        HANGUL_BASE=0xac00,

        JAMO_L_COUNT=19,
        JAMO_V_COUNT=21,
        JAMO_T_COUNT=28,

        JAMO_VT_COUNT=JAMO_V_COUNT*JAMO_T_COUNT,

        HANGUL_COUNT=JAMO_L_COUNT*JAMO_V_COUNT*JAMO_T_COUNT,
        HANGUL_LIMIT=HANGUL_BASE+HANGUL_COUNT
    };


#define isHangulChar(c) (HANGUL_BASE<=c && c<HANGUL_LIMIT)
#define isHangul(c) (c==_this->minYesNo)

#define isHangulWithoutJamoTBase(c) (c<HANGUL_COUNT && c%JAMO_T_COUNT==0)
#define isHangulWithoutJamoT(c) isHangulWithoutJamoTBase(c-HANGUL_BASE)
#define isJamoL(c)  ((uint32_t)(c-JAMO_L_BASE)<JAMO_L_COUNT)
#define isJamoV(c)  ((uint32_t)(c-JAMO_V_BASE)<JAMO_V_COUNT)
#define isJamoVT(norm16)  ( (norm16)==JAMO_VT )


/**
 * Get the NF*_QC property for a code point, for u_getIntPropertyValue().
 * @internal
 */
U_CFUNC UNormalizationCheckResult U_EXPORT2
unorm_getQuickCheck(UChar32 c, UNormalizationMode mode);


#endif
