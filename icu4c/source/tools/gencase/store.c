/*
*******************************************************************************
*
*   Copyright (C) 2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  store.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004aug28
*   created by: Markus W. Scherer
*
*   Store Unicode case mapping properties efficiently for
*   random access.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "utrie.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "propsvec.h"
#include "gencase.h"

/* Unicode case mapping properties file format ---------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 1 .

The file contains the following structures:

    const int32_t indexes[i0] with values i0, i1, ...:
    (see UCASE_IX_... constants for names of indexes)

    i0 indexLength; -- length of indexes[] (UCASE_IX_TOP)
    i1 dataLength; -- length in bytes of the post-header data (incl. indexes[])
    i2 trieSize; -- size in bytes of the case mapping properties trie
    i3 exceptionsLength; -- length in uint16_t of the exceptions array

    i4..i14 reservedIndexes; -- reserved values; 0 for now

    i15 maxFullLength; -- maximum length of a full case mapping/folding string


    Serizalied trie, see utrie.h;

    const uint16_t exceptions[exceptionsLength];


Trie data word:
Bits
if(exception) {
    15..4   unsigned exception index
} else {
    if(not uncased) {
        15..6   signed delta to simple case mapping code point
                (add delta to input code point)
    } else {
            6   the code point is case-ignorable
                (U+0307 is also case-ignorable but has an exception)
    }
     5..4   0 normal character with cc=0
            1 soft-dotted character
            2 cc=230
            3 other cc
}
    3   exception
    2   case sensitive
 1..0   0 uncased
        1 lowercase
        2 uppercase
        3 titlecase


Exceptions:
A sub-array of the exceptions array is indexed by the exception index in a
trie word.
The sub-array consists of the following fields:
    uint16_t excWord;
    uint16_t optional values [];
    UTF-16 strings for full (string) mappings for lowercase, case folding, uppercase, titlecase

excWord: (see UCASE_EXC_...)
Bits
    15  conditional case folding
    14  conditional special casing
13..12  same as non-exception trie data bits 5..4
        moved here because the exception index needs more bits than the delta
        0 normal character with cc=0
        1 soft-dotted character
        2 cc=230
        3 other cc
11.. 9  reserved
     8  if set, then for each optional-value slot there are 2 uint16_t values
        (high and low parts of 32-bit values)
        instead of single ones
 7.. 0  bits for which optional value is present

Optional-value slots:
0   lowercase mapping (code point)
1   case folding (code point)
2   uppercase mapping (code point)
3   titlecase mapping (code point)
4..6 reserved
7   there is at least one full (string) case mapping
    the length of each is encoded in a nibble of this optional value,
    and the strings follow this optional value in the same order:
    lower/fold/upper/title

For space saving, some values are not stored. Lookups are as follows:
- If special casing is conditional, then no full lower/upper/title mapping
  strings are stored.
- If case folding is conditional, then no simple or full case foldings are
  stored.
- Fall back in this order:
    full (string) mapping -- if full mappings are used
    simple (code point) mapping of the same type
    simple fold->simple lower
    simple title->simple upper
    finally, the original code point (no mapping)

----------------------------------------------------------------------------- */

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    /* dataFormat="cAsE" */
    { UCASE_FMT_0, UCASE_FMT_1, UCASE_FMT_2, UCASE_FMT_3 },
    { 1, 0, UTRIE_SHIFT, UTRIE_INDEX_SHIFT },   /* formatVersion */
    { 4, 0, 1, 0 }                              /* dataVersion */
};

enum {
    /* maximum number of exceptions expected */
    MAX_EXC_COUNT=1000
};

/* exceptions values */
static uint16_t exceptions[UCASE_MAX_EXCEPTIONS+100];
static uint16_t exceptionsTop=0;
static Props excProps[MAX_EXC_COUNT];
static uint16_t exceptionsCount=0;

/* becomes indexes[UCASE_IX_MAX_FULL_LENGTH] */
static int32_t maxFullLength=U16_MAX_LENGTH;

/* -------------------------------------------------------------------------- */

extern void
setUnicodeVersion(const char *v) {
    UVersionInfo version;
    u_versionFromString(version, v);
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

/* store a character's properties ------------------------------------------- */

extern void
setProps(Props *p) {
    UErrorCode errorCode;
    uint32_t value, oldValue;
    int32_t delta;

    /* get the non-UnicodeData.txt properties */
    value=oldValue=upvec_getValue(pv, p->code, 0);

    /* default: map to self */
    delta=0;

    if(p->gc==U_TITLECASE_LETTER) {
        /* the Titlecase property is read late, from UnicodeData.txt */
        value|=UCASE_TITLE;
    }

    if(p->upperCase!=0) {
        /* uppercase mapping as delta if the character is lowercase */
        if((value&UCASE_TYPE_MASK)==UCASE_LOWER) {
            delta=p->upperCase-p->code;
        } else {
            value|=UCASE_EXCEPTION;
        }
    }
    if(p->lowerCase!=0) {
        /* lowercase mapping as delta if the character is uppercase or titlecase */
        if((value&UCASE_TYPE_MASK)>=UCASE_UPPER) {
            delta=p->lowerCase-p->code;
        } else {
            value|=UCASE_EXCEPTION;
        }
    }
    if(p->upperCase!=p->titleCase) {
        value|=UCASE_EXCEPTION;
    }
    if(p->specialCasing!=NULL) {
        value|=UCASE_EXCEPTION;
    }
    if(p->caseFolding!=NULL) {
        value|=UCASE_EXCEPTION;
    }

    if(delta<UCASE_MIN_DELTA || UCASE_MAX_DELTA<delta) {
        value|=UCASE_EXCEPTION;
    }

    if(p->cc!=0) {
        if(value&UCASE_DOT_MASK) {
            fprintf(stderr, "gencase: a soft-dotted character has cc!=0\n");
            exit(U_INTERNAL_PROGRAM_ERROR);
        }
        if(p->cc==230) {
            value|=UCASE_ABOVE;
        } else {
            value|=UCASE_OTHER_ACCENT;
        }
    }

    /* encode case-ignorable as delta==1 on uncased characters */
    if(
        (value&UCASE_TYPE_MASK)==UCASE_NONE &&
        p->code!=0x307 &&
        ((U_MASK(p->gc)&(U_GC_MN_MASK|U_GC_ME_MASK|U_GC_CF_MASK|U_GC_LM_MASK|U_GC_SK_MASK))!=0 ||
            p->code==0x27 || p->code==0xad || p->code==0x2019)
    ) {
        /*
         * We use one of the delta/exception bits, which works because we only
         * store the case-ignorable flag for uncased characters.
         * There is no delta for uncased characters (see checks above).
         * If there is an exception for an uncased, case-ignorable character
         * (although there should not be any case mappings if it's uncased)
         * then we have a problem.
         * There is one character which is case-ignorable but has an exception:
         * U+0307 is uncased, Mn, has conditional special casing and
         * is therefore handled in code instead.
         */
        if(value&UCASE_EXCEPTION) {
            fprintf(stderr, "gencase error: unable to encode case-ignorable for U+%04lx with exceptions\n",
                            (unsigned long)p->code);
            exit(U_INTERNAL_PROGRAM_ERROR);
        }

        delta=1;
    }

    /* handle exceptions */
    if(value&UCASE_EXCEPTION) {
        /* simply store exceptions for later processing and encoding */
        value|=(uint32_t)exceptionsCount<<UGENCASE_EXC_SHIFT;
        uprv_memcpy(excProps+exceptionsCount, p, sizeof(*p));
        if(++exceptionsCount==MAX_EXC_COUNT) {
            fprintf(stderr, "gencase: too many exceptions\n");
            exit(U_INDEX_OUTOFBOUNDS_ERROR);
        }
    } else {
        /* store the simple case mapping delta */
        value|=((uint32_t)delta<<UCASE_DELTA_SHIFT)&UCASE_DELTA_MASK;
    }

    errorCode=U_ZERO_ERROR;
    if( value!=oldValue &&
        !upvec_setValue(pv, p->code, p->code+1, 0, value, 0xffffffff, &errorCode)
    ) {
        fprintf(stderr, "gencase error: unable to set case mapping values, code: %s\n",
                        u_errorName(errorCode));
        exit(errorCode);
    }
}

extern void
addCaseSensitive(UChar32 first, UChar32 last) {
    UErrorCode errorCode=U_ZERO_ERROR;
    if(!upvec_setValue(pv, first, last+1, 0, UCASE_SENSITIVE, UCASE_SENSITIVE, &errorCode)) {
        fprintf(stderr, "gencase error: unable to set UCASE_SENSITIVE, code: %s\n",
                        u_errorName(errorCode));
        exit(errorCode);
    }
}

extern void
makeCaseClosure() {
    /* TODO */
}

/* exceptions --------------------------------------------------------------- */

static UBool
fullMappingEqualsSimple(const UChar *s, UChar32 simple, UChar32 c) {
    int32_t i, length;
    UChar32 full;

    length=*s++;
    if(length==0 || length>U16_MAX_LENGTH) {
        return FALSE;
    }
    i=0;
    U16_NEXT(s, i, length, full);

    if(simple==0) {
        simple=c; /* UCD has no simple mapping if it's the same as the code point itself */
    }
    return (UBool)(i==length && full==simple);
}

static uint16_t
makeException(uint32_t value, Props *p) {
    uint32_t slots[8];
    uint32_t slotBits;
    uint16_t excWord, excIndex, excTop, i, count, length, fullLengths;
    UBool doubleSlots;

    /* excIndex will be returned for storing in the trie word */
    excIndex=exceptionsTop;
    if(excIndex>=UCASE_MAX_EXCEPTIONS) {
        fprintf(stderr, "gencase error: too many exceptions words\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    excTop=excIndex+1; /* +1 for excWord which will be stored at excIndex */

    /* copy and shift the soft-dotted bits */
    excWord=((uint16_t)value&UCASE_DOT_MASK)<<UCASE_EXC_DOT_SHIFT;

    /* update maxFullLength */
    if(p->specialCasing!=NULL) {
        length=p->specialCasing->lowerCase[0];
        if(length>maxFullLength) {
            maxFullLength=length;
        }
        length=p->specialCasing->upperCase[0];
        if(length>maxFullLength) {
            maxFullLength=length;
        }
        length=p->specialCasing->titleCase[0];
        if(length>maxFullLength) {
            maxFullLength=length;
        }
    }
    if(p->caseFolding!=NULL) {
        length=p->caseFolding->full[0];
        if(length>maxFullLength) {
            maxFullLength=length;
        }
    }

    /* set the bits for conditional mappings */
    if(p->specialCasing!=NULL && p->specialCasing->isComplex) {
        excWord|=UCASE_EXC_CONDITIONAL_SPECIAL;
        p->specialCasing=NULL;
    }
    if(p->caseFolding!=NULL && p->caseFolding->simple==0 && p->caseFolding->full[0]==0) {
        excWord|=UCASE_EXC_CONDITIONAL_FOLD;
        p->caseFolding=NULL;
    }

    /*
     * Note:
     * UCD stores no simple mappings when they are the same as the code point itself.
     * SpecialCasing and CaseFolding do store simple mappings even if they are
     * the same as the code point itself.
     * Comparisons between simple regular mappings and simple special/folding
     * mappings need to compensate for the difference by comparing with the
     * original code point if a simple UCD mapping is missing (0).
     */

    /* remove redundant data */
    if(p->specialCasing!=NULL) {
        /* do not store full mappings if they are the same as the simple ones */
        if(fullMappingEqualsSimple(p->specialCasing->lowerCase, p->lowerCase, p->code)) {
            p->specialCasing->lowerCase[0]=0;
        }
        if(fullMappingEqualsSimple(p->specialCasing->upperCase, p->upperCase, p->code)) {
            p->specialCasing->upperCase[0]=0;
        }
        if(fullMappingEqualsSimple(p->specialCasing->titleCase, p->titleCase, p->code)) {
            p->specialCasing->titleCase[0]=0;
        }
    }
    if( p->caseFolding!=NULL &&
        fullMappingEqualsSimple(p->caseFolding->full, p->caseFolding->simple, p->code)
    ) {
        p->caseFolding->full[0]=0;
    }

    /* write the optional slots */
    slotBits=0;
    count=0;

    if(p->lowerCase!=0) {
        slots[count]=(uint32_t)p->lowerCase;
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_LOWER);
    }
    if( p->caseFolding!=NULL &&
        p->caseFolding->simple!=0 &&
        (p->lowerCase!=0 ?
            p->caseFolding->simple!=p->lowerCase :
            p->caseFolding->simple!=p->code)
    ) {
        slots[count]=(uint32_t)p->caseFolding->simple;
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_FOLD);
    }
    if(p->upperCase!=0) {
        slots[count]=(uint32_t)p->upperCase;
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_UPPER);
    }
    if(p->upperCase!=p->titleCase) {
        if(p->titleCase!=0) {
            slots[count]=(uint32_t)p->titleCase;
        } else {
            slots[count]=(uint32_t)p->code;
        }
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_TITLE);
    }

    /* lengths of full case mapping strings, stored in the last slot */
    fullLengths=0;
    if(p->specialCasing!=NULL) {
        fullLengths=p->specialCasing->lowerCase[0];
        fullLengths|=p->specialCasing->upperCase[0]<<8;
        fullLengths|=p->specialCasing->titleCase[0]<<12;
    }
    if(p->caseFolding!=NULL) {
        fullLengths|=p->caseFolding->full[0]<<4;
    }
    if(fullLengths!=0) {
        slots[count]=fullLengths;
        slotBits|=slots[count];
        ++count;
        excWord|=U_MASK(UCASE_EXC_FULL_MAPPINGS);
    }

    /* write slots */
    doubleSlots=(UBool)(slotBits>0xffff);
    if(!doubleSlots) {
        for(i=0; i<count; ++i) {
            exceptions[excTop++]=(uint16_t)slots[i];
        }
    } else {
        excWord|=UCASE_EXC_DOUBLE_SLOTS;
        for(i=0; i<count; ++i) {
            exceptions[excTop++]=(uint16_t)(slots[i]>>16);
            exceptions[excTop++]=(uint16_t)slots[i];
        }
    }

    /* write the full case mapping strings */
    if(p->specialCasing!=NULL) {
        length=(uint16_t)p->specialCasing->lowerCase[0];
        u_memcpy((UChar *)exceptions+excTop, p->specialCasing->lowerCase+1, length);
        excTop+=length;
    }
    if(p->caseFolding!=NULL) {
        length=(uint16_t)p->caseFolding->full[0];
        u_memcpy((UChar *)exceptions+excTop, p->caseFolding->full+1, length);
        excTop+=length;
    }
    if(p->specialCasing!=NULL) {
        length=(uint16_t)p->specialCasing->upperCase[0];
        u_memcpy((UChar *)exceptions+excTop, p->specialCasing->upperCase+1, length);
        excTop+=length;

        length=(uint16_t)p->specialCasing->titleCase[0];
        u_memcpy((UChar *)exceptions+excTop, p->specialCasing->titleCase+1, length);
        excTop+=length;
    }

    exceptionsTop=excTop;

    /* write the main exceptions word */
    exceptions[excIndex]=excWord;

    return excIndex;
}

extern void
makeExceptions() {
    uint32_t *row;
    uint32_t value;
    int32_t i;
    uint16_t excIndex;

    i=0;
    while((row=upvec_getRow(pv, i, NULL, NULL))!=NULL) {
        value=*row;
        if(value&UCASE_EXCEPTION) {
            excIndex=makeException(value, excProps+(value>>UGENCASE_EXC_SHIFT));
            *row=(value&~(UGENCASE_EXC_MASK|UCASE_EXC_MASK))|(excIndex<<UCASE_EXC_SHIFT);
        }
        ++i;
    }
}

/* generate output data ----------------------------------------------------- */

extern void
generateData(const char *dataDir) {
    static int32_t indexes[UCASE_IX_TOP]={
        UCASE_IX_TOP
    };
    static uint8_t trieBlock[40000];

    const uint32_t *row;
    UChar32 start, limit;
    int32_t i;

    UNewDataMemory *pData;
    UNewTrie *pTrie;
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t trieSize;
    long dataLength;

    pTrie=utrie_open(NULL, NULL, 20000, 0, 0, TRUE);
    if(pTrie==NULL) {
        fprintf(stderr, "gencase error: unable to create a UNewTrie\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    for(i=0; (row=upvec_getRow(pv, i, &start, &limit))!=NULL; ++i) {
        if(!utrie_setRange32(pTrie, start, limit, *row, TRUE)) {
            fprintf(stderr, "gencase error: unable to set trie value (overflow)\n");
            exit(U_BUFFER_OVERFLOW_ERROR);
        }
    }

    trieSize=utrie_serialize(pTrie, trieBlock, sizeof(trieBlock), NULL, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error: utrie_serialize failed: %s (length %ld)\n", u_errorName(errorCode), (long)trieSize);
        exit(errorCode);
    }

    indexes[UCASE_IX_EXC_LENGTH]=exceptionsTop;
    indexes[UCASE_IX_TRIE_SIZE]=trieSize;
    indexes[UCASE_IX_LENGTH]=(int32_t)sizeof(indexes)+trieSize+2*exceptionsTop;

    indexes[UCASE_IX_MAX_FULL_LENGTH]=maxFullLength;

    if(beVerbose) {
        printf("trie size in bytes:                    %5d\n", (int)trieSize);
        printf("number of code points with exceptions: %5d\n", exceptionsCount);
        printf("size in bytes of exceptions:           %5d\n", 2*exceptionsTop);
        printf("data size:                             %5d\n", (int)indexes[UCASE_IX_LENGTH]);
    }

    /* write the data */
    pData=udata_create(dataDir, UCASE_DATA_TYPE, UCASE_DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gencase: unable to create data memory, %s\n", u_errorName(errorCode));
        exit(errorCode);
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, exceptions, 2*exceptionsTop);

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gencase: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=indexes[UCASE_IX_LENGTH]) {
        fprintf(stderr, "gencase: data length %ld != calculated size %d\n",
            dataLength, (int)indexes[UCASE_IX_LENGTH]);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }

    utrie_close(pTrie);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
