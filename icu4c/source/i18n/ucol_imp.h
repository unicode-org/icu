/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* Private implementation header for C collation
*   file name:  ucol_imp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000dec11
*   created by: Vladimir Weinstein
*
* Modification history
* Date        Name      Comments
* 02/16/2001  synwee    Added UCOL_GETPREVCE for the use in ucoleitr
* 02/27/2001  synwee    Added getMaxExpansion data structure in UCollator
* 03/02/2001  synwee    Added UCOL_IMPLICIT_CE
* 03/12/2001  synwee    Added pointer start to collIterate.
*/

#ifndef UCOL_IMP_H
#define UCOL_IMP_H

#include "unicode/ucol.h"
#include "ucmpe32.h"
#include "unicode/ures.h"
#include "unicode/udata.h"

/* This is the size of the stack allocated buffer for sortkey generation and similar operations */
/* if it is too small, heap allocation will occur.*/
/* you can change this value if you need memory - it will affect the performance, though, since we're going to malloc */
#define UCOL_MAX_BUFFER 128
#define UCOL_PRIMARY_MAX_BUFFER 8*UCOL_MAX_BUFFER
#define UCOL_SECONDARY_MAX_BUFFER UCOL_MAX_BUFFER
#define UCOL_TERTIARY_MAX_BUFFER UCOL_MAX_BUFFER
#define UCOL_CASE_MAX_BUFFER UCOL_MAX_BUFFER/4
#define UCOL_QUAD_MAX_BUFFER UCOL_MAX_BUFFER

#define UCOL_NORMALIZATION_GROWTH 2
#define UCOL_NORMALIZATION_MAX_BUFFER UCOL_MAX_BUFFER*UCOL_NORMALIZATION_GROWTH

/* This writable buffer is used if we encounter Thai and need to reorder the string on the fly */
/* Sometimes we already have a writable buffer (like in case of normalized strings). */
/*
you can change this value to any value >= 3 if you need memory -
it will affect the performance, though, since we're going to malloc.
Note 3 is the minimum value for Thai collation to work correctly.
*/
#define UCOL_WRITABLE_BUFFER_SIZE 256

/* This is the size of the buffer for expansion CE's */
/* In reality we should not have to deal with expm sequences longer then 16 */
/* you can change this value if you need memory */
/* WARNING THIS BUFFER DOES NOT HAVE MALLOC FALLBACK. If you make it too small, you'll get in trouble */
/* Reasonable small value is around 10, if you don't do Arabic or other funky collations that have long expansion sequence */
/* This is the longest expansion sequence we can handle without bombing out */
#define UCOL_EXPAND_CE_BUFFER_SIZE 512 /* synwee :TODO revert back 64*/


/* Unsafe UChar hash table table size.                                           */
/*  size is 32 bytes for 1 bit for each latin 1 char + some power of two for     */
/*  hashing the rest of the chars.   Size in bytes                               */
#define UCOL_UNSAFECP_TABLE_SIZE 1056
                                    /* mask value down to "some power of two"-1  */
                                    /*  number of bits, not num of bytes.        */
#define UCOL_UNSAFECP_TABLE_MASK 0x1fff


/* flags bits for collIterate.flags       */
/*                                        */
/*  NORM - set for incremental normalize of source string */
#define UCOL_ITER_NORM  1

#define UCOL_ITER_HASLEN 2

                              /* UCOL_ITER_INNORMBUF - set if the "pos" is in          */
                              /*               the writable side buffer, handling      */
                              /*               incrementally normalized characters.    */
#define UCOL_ITER_INNORMBUF 4

                              /* UCOL_ITER_ALLOCATED - set if this iterator has        */
                              /*    malloced storage to expand a buffer.               */
#define UCOL_ITER_ALLOCATED 8

#define NFC_ZERO_CC_BLOCK_LIMIT_  0x300

struct collIterate {
  UChar *string; /* Original string */
  /* UChar *start;  Pointer to the start of the source string. Either points to string
                    or to writableBuffer */
  UChar *endp;   /* string end ptr.  Is undefined for null terminated strings */
  UChar *pos; /* This is position in the string.  Can be to original or writable buf */

  uint32_t *toReturn; /* This is the CE from CEs buffer that should be returned */
  uint32_t *CEpos; /* This is the position to which we have stored processed CEs */
  UChar *writableBuffer;
  uint32_t writableBufSize;
  UChar *fcdPosition; /* Position in the original string to continue FCD check from. */
  const UCollator *coll;
  uint8_t   flags;
  uint8_t   origFlags;
  uint32_t CEs[UCOL_EXPAND_CE_BUFFER_SIZE]; /* This is where we store CEs */
  UChar stackWritableBuffer[UCOL_WRITABLE_BUFFER_SIZE]; /* A writable buffer. */
};

/* 
struct used internally in getSpecial*CE.
data similar to collIterate.
*/
struct collIterateState {
    UChar    *pos; /* This is position in the string.  Can be to original or writable buf */
    UChar    *fcdPosition; /* Position in the original string to continue FCD check from. */
    UChar    *bufferaddress; /* address of the normalization buffer */
    uint32_t  buffersize;
    uint8_t   flags;
    uint8_t   origFlags;
};

U_CAPI void init_collIterate(const UCollator *collator, const UChar *sourceString, int32_t sourceLen, collIterate *s);


struct UCollationElements
{
  /**
  * Struct wrapper for source data
  */
        collIterate        iteratordata_;
  /**
  * Indicates if this data has been reset.
  */
        UBool              reset_;
  /**
  * Indicates if the data should be deleted.
  */
        UBool              isWritable;
};


#define UCOL_LEVELTERMINATOR 1

/* mask off anything but primary order */
#define UCOL_PRIMARYORDERMASK 0xffff0000
/* mask off anything but secondary order */
#define UCOL_SECONDARYORDERMASK 0x0000ff00
/* mask off anything but tertiary order */
#define UCOL_TERTIARYORDERMASK 0x000000ff
/* primary order shift */
#define UCOL_PRIMARYORDERSHIFT 16
/* secondary order shift */
#define UCOL_SECONDARYORDERSHIFT 8

#define UCOL_BYTE_SIZE_MASK 0xFF

#define UCOL_CASE_BYTE_START 0x80
#define UCOL_CASE_SHIFT_START 7

#define UCOL_IGNORABLE 0

/* get weights from a CE */
#define UCOL_PRIMARYORDER(order) (((order) & UCOL_PRIMARYORDERMASK)>> UCOL_PRIMARYORDERSHIFT)
#define UCOL_SECONDARYORDER(order) (((order) & UCOL_SECONDARYORDERMASK)>> UCOL_SECONDARYORDERSHIFT)
#define UCOL_TERTIARYORDER(order) ((order) & UCOL_TERTIARYORDERMASK)

/**
 * Determine if a character is a Thai vowel (which sorts after
 * its base consonant).
 */
#define UCOL_ISTHAIPREVOWEL(ch) ((((uint32_t)(ch) - 0xe40) <= (0xe44 - 0xe40)) || \
                                 (((uint32_t)(ch) - 0xec0) <= (0xec4 - 0xec0)))

/**
 * Determine if a character is a Thai base consonant
 */
#define UCOL_ISTHAIBASECONSONANT(ch) ((uint32_t)(ch) - 0xe01) <= (0xe2e - 0xe01)

#define UCOL_ISJAMO(ch) ((((uint32_t)(ch) - 0x1100) <= (0x1112 - 0x1100)) || \
                         (((uint32_t)(ch) - 0x1161) <= (0x1175 - 0x1161)) || \
                         (((uint32_t)(ch) - 0x11A8) <= (0x11C2 - 0x11A8)))



#if 0
/* initializes collIterate structure */
/* made as macro to speed up things */
#define init_collIterate(collator, sourceString, sourceLen, s) { \
    (s)->start = (s)->string = (s)->pos = (UChar *)(sourceString); \
    (s)->endp  = (sourceLen) == -1 ? NULL :(UChar *)(sourceString)+(sourceLen); \
    (s)->CEpos = (s)->toReturn = (s)->CEs; \
    (s)->isThai = TRUE; \
    (s)->writableBuffer = (s)->stackWritableBuffer; \
    (s)->writableBufSize = UCOL_WRITABLE_BUFFER_SIZE; \
    (s)->coll = (collator); \
    (s)->fcdPosition = 0;   \
    (s)->flags = 0; \
    if(((collator)->normalizationMode == UCOL_ON)) (s)->flags |= UCOL_ITER_NORM; \
}
#endif



/*
* Macro to get the maximum size of an expansion ending with the argument ce.
* Used in the Boyer Moore algorithm.
* Note for tailoring, the UCA maxexpansion table has been merged.
* Hence we only have to search the tailored collator only.
* @param coll const UCollator pointer
* @param order last collation element of the expansion sequence
* @param result size of the longest expansion with argument collation element
*        as the last element
*/
#define UCOL_GETMAXEXPANSION(coll, order, result) {                          \
  const uint32_t *start;                                                     \
  const uint32_t *limit;                                                     \
  const uint32_t *mid;                                                       \
  start = (coll)->endExpansionCE;                                            \
  limit = (coll)->lastEndExpansionCE;                                        \
  while (start < limit - 1) {                                                \
    mid = start + ((limit - start) >> 1);                                    \
    if ((order) <= *mid) {                                                   \
      limit = mid;                                                           \
    }                                                                        \
    else {                                                                   \
      start = mid;                                                           \
    }                                                                        \
  }                                                                          \
  if (*start == order) {                                                     \
    result = *((coll)->expansionCESize + (start - (coll)->endExpansionCE));  \
  }                                                                          \
  else if (*limit == order) {                                                \
         result = *(coll->expansionCESize + (limit - coll->endExpansionCE)); \
       }                                                                     \
       else if ((order & 0xFFFF) == 0x00C0) {                                \
              result = 2;                                                    \
            }                                                                \
            else {                                                           \
              result = 1;                                                    \
            }                                                                \
}

uint32_t getSpecialCE(const UCollator *coll, UChar ch, uint32_t CE, collIterate *source, UErrorCode *status);
uint32_t getSpecialPrevCE(const UCollator *coll, UChar ch, uint32_t CE,
                          collIterate *source, UErrorCode *status);
U_CAPI uint32_t U_EXPORT2 ucol_getNextCE(const UCollator *coll, collIterate *collationSource, UErrorCode *status);
U_CAPI uint32_t U_EXPORT2 ucol_getPrevCE(const UCollator *coll,
                                         collIterate *collationSource,
                                         UErrorCode *status);
/* function used by C++ getCollationKey to prevent restarting the calculation */
U_CFUNC uint8_t *ucol_getSortKeyWithAllocation(const UCollator *coll,
        const    UChar        *source,
        int32_t            sourceLength, int32_t *resultLen);

/* get some memory */
void *ucol_getABuffer(const UCollator *coll, uint32_t size);

/* worker function for generating sortkeys */
int32_t
ucol_calcSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status);

int32_t
ucol_calcSortKeySimpleTertiary(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status);

/**
 * Makes a copy of the Collator's rule data. The format is
 * that of .col files.
 *
 * @param length returns the length of the data, in bytes.
 * @param status the error status
 * @return memory, owned by the caller, of size 'length' bytes.
 * @internal INTERNAL USE ONLY
 */
U_CAPI uint8_t *
ucol_cloneRuleData(const UCollator *coll, int32_t *length, UErrorCode *status);

#define UCOL_SPECIAL_FLAG 0xF0000000
#define UCOL_TAG_SHIFT 24
#define UCOL_TAG_MASK 0x0F000000
#define INIT_EXP_TABLE_SIZE 1024
#define UCOL_NOT_FOUND 0xF0000000
#define UCOL_EXPANSION 0xF1000000
#define UCOL_CONTRACTION 0xF2000000
#define UCOL_THAI 0xF3000000
#define UCOL_UNMARKED 0x03
#define UCOL_NEW_TERTIARYORDERMASK 0x0000003f

/* Bit mask for primary collation strength. */
#define UCOL_PRIMARYMASK    0xFFFF0000

/* Bit mask for secondary collation strength. */
#define UCOL_SECONDARYMASK  0x0000FF00

/* Bit mask for tertiary collation strength. */
#define UCOL_TERTIARYMASK   0x000000FF

/**
 * Internal.
 * This indicates the last element in a UCollationElements has been consumed.
 * Compare with the UCOL_NULLORDER, UCOL_NULLORDER is returned if error occurs.
 */
#define UCOL_NO_MORE_CES            0x00010101
#define UCOL_NO_MORE_CES_PRIMARY    0x00010000
#define UCOL_NO_MORE_CES_SECONDARY  0x00000100
#define UCOL_NO_MORE_CES_TERTIARY   0x00000001

#define isSpecial(CE) ((((CE)&UCOL_SPECIAL_FLAG)>>28)==0xF)

#define UCOL_UPPER_CASE 0x80
#define UCOL_MIXED_CASE 0x40
#define UCOL_LOWER_CASE 0x00

#define UCOL_CONTINUATION_MARKER 0xC0
#define UCOL_REMOVE_CONTINUATION 0xFFFFFF3F

#define isContinuation(CE) (((CE) & UCOL_CONTINUATION_MARKER) == UCOL_CONTINUATION_MARKER)
#define isFlagged(CE) (((CE) & 0x80) == 0x80)
#define isLongPrimary(CE) (((CE) & 0xC0) == 0xC0)

#define getCETag(CE) (((CE)&UCOL_TAG_MASK)>>UCOL_TAG_SHIFT)
#define isContraction(CE) (isSpecial((CE)) && (getCETag((CE)) == CONTRACTION_TAG))
#define constructContractCE(CE) (UCOL_SPECIAL_FLAG | (CONTRACTION_TAG<<UCOL_TAG_SHIFT) | ((CE))&0xFFFFFF)
#define getContractOffset(CE) ((CE)&0xFFFFFF)
#define getExpansionOffset(CE) (((CE)&0x00FFFFF0)>>4)
#define getExpansionCount(CE) ((CE)&0xF)
#define isCEIgnorable(CE) (((CE) & 0xFFFFFFBF) == 0)

/* StringSearch internal use */
#define inNormBuf(coleiter) ((coleiter)->iteratordata_.flags & UCOL_ITER_INNORMBUF)
#define isFCDPointerNull(coleiter) ((coleiter)->iteratordata_.fcdPosition == NULL)
#define hasExpansion(coleiter) ((coleiter)->iteratordata_.CEpos != (coleiter)->iteratordata_.CEs)
#define getExpansionPrefix(coleiter) ((coleiter)->iteratordata_.toReturn - (coleiter)->iteratordata_.CEs)
#define setExpansionPrefix(coleiter, offset) ((coleiter)->iteratordata_.CEs + offset)
#define getExpansionSuffix(coleiter) ((coleiter)->iteratordata_.CEpos - (coleiter)->iteratordata_.toReturn)
#define setExpansionSuffix(coleiter, offset) ((coleiter)->iteratordata_.toReturn = (coleiter)->iteratordata_.CEpos - leftoverces)

#define UCA_DATA_TYPE "dat"
#define UCA_DATA_NAME "ucadata"
#define INVC_DATA_TYPE "dat"
#define INVC_DATA_NAME "invuca"

/* This is an enum that lists magic special byte values from the fractional UCA */
/* TODO: all the #defines that refer to special byte values from the UCA should be changed to point here */

enum {
    UCOL_BYTE_ZERO = 0x00,
    UCOL_BYTE_LEVEL_SEPARATOR = 0x01,
    UCOL_BYTE_SORTKEY_GLUE = 0x02,
    UCOL_BYTE_SHIFT_PREFIX = 0x03,
    UCOL_BYTE_UNSHIFTED_MIN = UCOL_BYTE_SHIFT_PREFIX,
    UCOL_BYTE_FIRST_TAILORED = 0x04,
    UCOL_BYTE_COMMON = 0x05,
    UCOL_BYTE_FIRST_UCA = UCOL_BYTE_COMMON,
    UCOL_BYTE_LAST_LATIN_PRIMARY = 0x4C,
    UCOL_BYTE_FIRST_NON_LATIN_PRIMARY = 0x4D,
    UCOL_BYTE_UNSHIFTED_MAX = 0xFF
}; 

#define UCOL_RESET_TOP_VALUE 0x9F000303
#define UCOL_NEXT_TOP_VALUE  0xE8960303
#define PRIMARY_IMPLICIT_MIN 0xE8000000
#define PRIMARY_IMPLICIT_MAX 0xF0000000

/* These constants can be changed - sortkey size is affected by them */
#define UCOL_PROPORTION2 0.5
#define UCOL_PROPORTION3 0.667

/* These values come from the UCA */
#define UCOL_COMMON_BOT2 UCOL_BYTE_COMMON
#define UCOL_COMMON_TOP2 0x86
#define UCOL_TOTAL2 (UCOL_COMMON_TOP2-UCOL_COMMON_BOT2-1) 

#define UCOL_FLAG_BIT_MASK_CASE_SW_OFF 0x80
#define UCOL_FLAG_BIT_MASK_CASE_SW_ON 0x40
#define UCOL_COMMON_TOP3_CASE_SW_OFF 0x85
#define UCOL_COMMON_TOP3_CASE_SW_LOWER 0x45
#define UCOL_COMMON_TOP3_CASE_SW_UPPER 0xC5

/* These values come from the UCA */
#define UCOL_COMMON_BOT3 0x05

#define UCOL_COMMON_BOTTOM3_CASE_SW_UPPER 0x86;
#define UCOL_COMMON_BOTTOM3_CASE_SW_LOWER UCOL_COMMON_BOT3;

#define UCOL_TOP_COUNT2  (UCOL_PROPORTION2*UCOL_TOTAL2)
#define UCOL_BOT_COUNT2  (UCOL_TOTAL2-UCOL_TOP_COUNT2)


#define UCOL_COMMON2 UCOL_COMMON_BOT2
#define UCOL_COMMON3_UPPERFIRST 0xC5
#define UCOL_COMMON3_NORMAL UCOL_COMMON_BOT3

#define UCOL_COMMON4 0xFF

/* constants for case level/case first handling */
/* used to instantiate UCollators fields in ucol_updateInternalState */
#define UCOL_CASE_SWITCH      0xC0
#define UCOL_NO_CASE_SWITCH   0x00

#define UCOL_REMOVE_CASE      0x3F
#define UCOL_KEEP_CASE        0xFF

#define UCOL_CASE_BIT_MASK    0xC0

#define UCOL_TERT_CASE_MASK   0xFF

typedef enum {
    NOT_FOUND_TAG = 0,
    EXPANSION_TAG = 1,       /* This code point results in an expansion */
    CONTRACTION_TAG = 2,     /* Start of a contraction */
    THAI_TAG = 3,            /* Thai character - do the reordering */
    CHARSET_TAG = 4,         /* Charset processing, not yet implemented */
    SURROGATE_TAG = 5,       /* Lead surrogate that is tailored and doesn't start a contraction */
    HANGUL_SYLLABLE_TAG = 6, /* AC00-D7AF*/
    LEAD_SURROGATE_TAG = 7,  /* D800-DBFF*/
    TRAIL_SURROGATE_TAG = 8,     /* DC00-DFFF*/
    CJK_IMPLICIT_TAG = 9,    /* 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D*/
    IMPLICIT_TAG = 10,
    CE_TAGS_COUNT
} UColCETags;

/*
 *****************************************************************************************
 * set to zero
 * NON_CHARACTER FDD0 - FDEF, FFFE, FFFF, 1FFFE, 1FFFF, 2FFFE, 2FFFF,...e.g. **FFFE, **FFFF
 ******************************************************************************************
 */

typedef struct {
      uint32_t variableTopValue;
      UColAttributeValue frenchCollation;
      UColAttributeValue alternateHandling; /* attribute for handling variable elements*/
      UColAttributeValue caseFirst;         /* who goes first, lower case or uppercase */
      UColAttributeValue caseLevel;         /* do we have an extra case level */
      UColAttributeValue normalizationMode; /* attribute for normalization */
      UColAttributeValue strength;          /* attribute for strength */
} UColOptionSet;

typedef struct {
      int32_t size;
      /* all the offsets are in bytes */
      /* to get the address add to the header address and cast properly */
      uint32_t options; /* these are the default options for the collator */
      uint32_t contractionUCACombos;        /* this one is needed only for UCA, to copy the appropriate contractions */
      uint32_t unusedReserved1;         /* reserved for future use */
      uint32_t mappingPosition;  /* const uint8_t *mappingPosition; */
      uint32_t expansion;        /* uint32_t *expansion;            */
      uint32_t contractionIndex; /* UChar *contractionIndex;        */
      uint32_t contractionCEs;   /* uint32_t *contractionCEs;       */
      uint32_t contractionSize;  /* needed for various closures */
      uint32_t latinOneMapping;  /* fast track to latin1 chars      */

      uint32_t endExpansionCE;      /* array of last collation element in
                                       expansion */
      uint32_t expansionCESize;     /* array of maximum expansion size
                                       corresponding to the expansion
                                       collation elements with last element
                                       in endExpansionCE*/
      int32_t  endExpansionCECount; /* size of endExpansionCE */
      uint32_t unsafeCP;            /* hash table of unsafe code points */
      uint32_t contrEndCP;          /* hash table of final code points  */
                                    /*   in contractions.               */

      int32_t CEcount;
      UBool jamoSpecial;                    /* is jamoSpecial */
      uint8_t padding[3];                   /* for guaranteed alignment */
      UVersionInfo version;
      char charsetName[32];                 /* for charset CEs */
      uint8_t reserved[64];                 /* for future use */
} UCATableHeader;

#define U_UNKNOWN_STATE 0
#define U_COLLATOR_STATE 0x01
#define U_STATE_LIMIT 0x02

/* This is the first structure in a state */
/* it should be machine independent */
typedef struct {
  /* this structure is supposed to be readable on all the platforms.*/
  /* first 2 fields hold the size of the structure in a platform independent way */
  uint8_t sizeLo;
  uint8_t sizeHi;
  /* identifying the writing platform */
  uint8_t isBigEndian;
  /* see U_CHARSET_FAMILY values in utypes.h */
  uint8_t charsetFamily;
  /* version of ICU this state structure comes from */
  uint8_t icuVersion[4];
  /* What is the data following this state */
  uint8_t type;
  /* more stuff to come, keep it on 16 byte boundary */
  uint8_t reserved[7];
} UStateStruct;

/* This structure follows UStatusStruct */
/* and contains data specific for the collators */
/* Endianess needs to be decided before accessing this structure */
/* However, it's size IS endianess independent */
typedef struct {
  /* size of this structure */
  uint8_t sizeLo;
  uint8_t sizeHi;
  /* This state is followed by the frozen tailoring */
  uint8_t containsTailoring;
  /* This state is followed by the frozen UCA */
  uint8_t containsUCA;
  /* Version info - the same one */
  uint8_t versionInfo[4];

  /* for charset CEs */
  uint8_t charsetName[32];                 
  /* this is the resolved locale name*/
  uint8_t locale[32];                      

  /* Attributes. Open ended */
  /* all the following will be moved to uint32_t because of portability */
  /* variable top value */
  uint32_t variableTopValue;
  /* attribute for handling variable elements*/
  uint32_t /*UColAttributeValue*/ alternateHandling; 
  /* how to handle secondary weights */
  uint32_t /*UColAttributeValue*/ frenchCollation;
  /* who goes first, lower case or uppercase */
  uint32_t /*UColAttributeValue*/ caseFirst;         
  /* do we have an extra case level */
  uint32_t /*UColAttributeValue*/ caseLevel;         
  /* attribute for normalization */
  uint32_t /*UColAttributeValue*/ normalizationMode; 
  /* attribute for strength */
  uint32_t /*UColAttributeValue*/ strength;
  /* to be immediately 16 byte aligned */
  uint8_t reserved[12];
} UColStateStruct;

#define UCOL_INV_SIZEMASK 0xFFF00000
#define UCOL_INV_OFFSETMASK 0x000FFFFF
#define UCOL_INV_SHIFTVALUE 20

typedef struct {
  uint32_t byteSize;
  uint32_t tableSize;
  uint32_t contsSize;
  uint32_t table;
  uint32_t conts;
} InverseTableHeader;

typedef int32_t
SortKeyGenerator(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status);

struct UCollator {
    UBool freeOptionsOnClose;
    UColOptionSet *options;
    SortKeyGenerator *sortKeyGen;
    UBool freeOnClose;
    UResourceBundle *rb;
    const UCATableHeader *image;
    CompactEIntArray *mapping;
    const uint32_t *latinOneMapping;
    const uint32_t *expansion;
    const UChar *contractionIndex;
    const uint32_t *contractionCEs;
    const uint8_t *scriptOrder;
    uint8_t caseSwitch;
    uint8_t tertiaryCommon;
    uint8_t tertiaryMask;
    int32_t tertiaryAddition; /* when switching case, we need to add or subtract different values */
    uint8_t tertiaryTop; /* Upper range when compressing */
    uint8_t tertiaryBottom; /* Upper range when compressing */
    uint8_t tertiaryTopCount;
    uint8_t tertiaryBottomCount;

    uint32_t variableTopValue;
    UColAttributeValue frenchCollation;
    UColAttributeValue alternateHandling; /* attribute for handling variable elements*/
    UColAttributeValue caseFirst;         /* who goes first, lower case or uppercase */
    UColAttributeValue caseLevel;         /* do we have an extra case level */
    UColAttributeValue normalizationMode; /* attribute for normalization */
    UColAttributeValue strength;          /* attribute for strength */
    UBool variableTopValueisDefault;
    UBool frenchCollationisDefault;
    UBool alternateHandlingisDefault; /* attribute for handling variable elements*/
    UBool caseFirstisDefault;         /* who goes first, lower case or uppercase */
    UBool caseLevelisDefault;         /* do we have an extra case level */
    UBool normalizationModeisDefault; /* attribute for normalization */
    UBool strengthisDefault;          /* attribute for strength */
    UBool hasRealData;                /* some collators have only options, like French, no rules */
                                      /* to speed up things, we use the UCA image, but we don't want it */
                                      /* to run around */
    const UChar *rules;
    UBool freeRulesOnClose;
    UChar zero;
    UDataInfo dataInfo;               /* Data info of UCA table */
    UErrorCode errorCode;             /* internal error code */

    const uint32_t *endExpansionCE;    /* array of last ces in an expansion ce.
                                          corresponds to expansionCESize */
    const uint32_t *lastEndExpansionCE;/* pointer to the last element in endExpansionCE */
    const uint8_t  *expansionCESize;   /* array of the maximum size of a
                                         expansion ce with the last ce
                                         corresponding to endExpansionCE,
                                         terminated with a null */
    const uint8_t *unsafeCP;           /* unsafe code points hashtable */
    const uint8_t *contrEndCP;         /* Contraction ending chars hash table */
    UChar          minUnsafeCP;        /* Smallest unsafe Code Point. */
    UChar          minContrEndCP;      /* Smallest code point at end of a contraction */
};

/* various internal functions */
int32_t ucol_getIncrementalCE(const UCollator *coll, incrementalContext *ctx, UErrorCode *status);
void ucol_initUCA(UErrorCode *status);

UCollator* ucol_initCollator(const UCATableHeader *image, UCollator *fillIn, UErrorCode *status);
void ucol_setOptionsFromHeader(UCollator* result, UColOptionSet * opts, UErrorCode *status);
void ucol_putOptionsToHeader(UCollator* result, UColOptionSet * opts, UErrorCode *status);

void ucol_updateInternalState(UCollator *coll);

U_CAPI uint32_t U_EXPORT2 ucol_getFirstCE(const UCollator *coll, UChar u, UErrorCode *status);
U_CAPI char U_EXPORT2 *ucol_sortKeyToString(const UCollator *coll, const uint8_t *sortkey, char *buffer, uint32_t *len);
U_CAPI UBool U_EXPORT2 isTailored(const UCollator *coll, const UChar u, UErrorCode *status);

U_CAPI const U_EXPORT2 InverseTableHeader *ucol_initInverseUCA(UErrorCode *status);
U_CAPI int32_t U_EXPORT2 ucol_inv_getNextCE(uint32_t CE, uint32_t contCE,
                                            uint32_t *nextCE, uint32_t *nextContCE,
                                            uint32_t strength);
U_CAPI int32_t U_EXPORT2 ucol_inv_getPrevCE(uint32_t CE, uint32_t contCE,
                                            uint32_t *prevCE, uint32_t *prevContCE,
                                            uint32_t strength);



/*
 *  Test whether a character is potentially "unsafe" for use as a collation
 *  starting point.  Unsafe chars are those with combining class != 0 plus
 *  those that are the 2nd thru nth character in a contraction sequence.
 *
 *  Function is in header file because it's used in both collation and string search,
 *  and needs to be inline for performance.
 */
 /* __inline */ static UBool ucol_unsafeCP(UChar c, const UCollator *coll) {
    int32_t  hash;
    uint8_t  htbyte;

    if (c < coll->minUnsafeCP) {
        return FALSE;
    }

    hash = c;
    if (hash >= UCOL_UNSAFECP_TABLE_SIZE*8) {
      if(UTF_IS_TRAIL(c)) {
            /*  Trail surrogate                     */
            /*  These are always considered unsafe. */
            return TRUE;
        }
        hash = (hash & UCOL_UNSAFECP_TABLE_MASK) + 256;
    }
    htbyte = coll->unsafeCP[hash>>3];
    return (((htbyte >> (hash & 7)) & 1) == 1);
}

#endif

