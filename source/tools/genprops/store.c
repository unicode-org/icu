/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  store.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec11
*   created by: Markus W. Scherer
*
*   Store Unicode character properties efficiently for
*   random access.
*/

#include <stdio.h>
#include <stdlib.h>
#include "utypes.h"
#include "uchar.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "udata.h"
#include "unewdata.h"
#include "genprops.h"

/* Unicode character properties file format ------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

The contents is a parsed, binary form of several Unicode character
database files, mose prominently UnicodeData.txt.

Any Unicode code point from 0 to 0x10ffff can be looked up to get
the properties, if any, for that code point. This means that the input
to the lookup are 21-bit unsigned integers, with not all of the
21-bit range used.

It is assumed that client code keeps a uint16_t pointer
to the beginning of the data:

    const uint16 *p16;

Some indexes assume 32-bit units; although client code should only
cast the above pointer to (const uint32_t *), it is easier here
to talk about the result of the indexing with the definition of
another pointer variable for this:

    const uint32_t *p32=(const uint32_t *)p16;

Formally, the file contains the following structures:

    A const uint16_t exceptionsIndex; -- 32-bit index
    B const uint16_t ucharsIndex; -- 32-bit index
    C const uint16_t reservedIndex;
    D const uint16_t reservedIndex;

    E const uint16_t stage1[0x440]; -- 0x440=0x110000>>10
    F const uint16_t stage2[variable*2*64];
    G const uint16_t stage3[variable];
      (possible 1*uint16_t for padding to 4-alignment)

    H const uint32_t props32[variable];
    I const uint16_t exceptions[variable];
      (possible 1*uint16_t for padding to 4-alignment)

    J const UChar uchars[variable];

3-stage lookup and properties:

In order to condense the data for the 21-bit code space, several properties of
the Unicode code assignment are exploited:
- The code space is sparse.
- There are several 10k consecutive codes with the same properties.
- Characters and scripts are allocated in groups of 16 code points.
- Inside blocks for scripts the properties are often repetitive.
- The 21-bit space is not fully used for Unicode.

The three-stage lookup organizes code points in groups of 16 in stage 3.
64 such groups are grouped again, resulting in blocks of 1k in stage 2.
The first stage is limited according to all code points being <0x110000.
Each stage contains indexes to groups or blocks of the next stage
in an n:1 manner, i.e., multiple entries of one stage index the same
group or block in the next one.
In the third stage, groups of 16 may partially or completely overlap to save
space with repetitive properties.
In the properties table, only unique 32-bit words are stored to exploit
non-adjacent overlapping. This is why the third stage does not directly
contain the 32-bit properties words but only indexes to them.

The indexes in each stage take the offset in the data of the next block into
account to save additional arithmetic in the access.

The second stage also contains properties for groups of characters:
Each set of 64 indexes to stage 3 groups is followed by 64 group properties
words of a uint16_t each. This is used for the script ID, since scripts
are allocated with multiples of 16 code points each.

With a given Unicode code point

    uint32_t c;

and 0<=c<0x110000, the lookup uses the three stage tables to
arrive at an index into the props32[] table containing the character
properties for c.
For some characters, not all of the properties can be efficiently encoded
using 32 bits. For them, the 32-bit word contains an index into the exceptions[]
array. Some exception entries, in turn, may contain indexes into the uchars[]
array of Unicode strings, especially for non-1:1 case mappings.

The first stage consumes the 11 most significant bits of the 21-bit code point
and results in an index into the second stage:

    uint16_t i2=p16[4+c>>10];

The second stage consumes bits 9 to 4 of c and results in an index into the
third stage:

    uint16_t i3=p16[i2+((c>>4)&0x3f)];

The third stage consumes bits 3 to 0 of c and results in a code point-
specific value, which itself is only an index into the props32[] table:

    uint16_t i=p16[i3+(c&0xf)];

There is finally the 32-bit encoded set of properties for c:

    uint32_t props=p32[i];

For some characters, this contains an index into the exceptions array:

    if(props&0x20) {
        uint16_t e=(uint16_t)(props>>20);
        ...
    }

The exception values are a variable number of uint16_t starting at

    const uint16_t *pe=p16+2*p16[0]+e;

The first uint16_t there contains flags about what values actually follow it.
Some of those may be indexes for case mappings or similar and point to strings
(zero-terminated) in the uchars[] array:

    ...
    uint16_t u=pe[depends on pe[0]];
    const UChar *pu=(const UChar *)(p32+p16[1])+u;

32-bit properties sets:

Each 32-bit properties word contains:

 0.. 4  general category
 5      has exception values
 6.. 9  BiDi category (the 5 explicit codes stored as one)
10      is mirrored
11..19  reserved
20..31  value according to bits 0..5:
        if(has exception) {
            exception index;
        } else switch(general category) {
        case Ll: delta to uppercase; -- same as titlecase
        case Lu: delta to lowercase; -- titlecase is same as c
        case Lt: delta to lowercase; -- uppercase is same as c
        case Mn: canonical category;
        case N*: numeric value;
        default: *;
        }

----------------------------------------------------------------------------- */

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    0x55, 0x50, 0x72, 0x6f,     /* dataFormat="UPro" */
    1, 0, 0, 0,                 /* formatVersion */
    3, 0, 0, 0                  /* dataVersion */
};

/* definitions and arrays for the 3-stage lookup */
enum {
    STAGE_1_BITS=11, STAGE_2_BITS=6, STAGE_3_BITS=4,

    STAGE_1_BLOCK=0x110000>>(STAGE_2_BITS+STAGE_3_BITS),
    STAGE_2_BLOCK=1<<STAGE_2_BITS,
    STAGE_3_BLOCK=1<<STAGE_3_BITS,

    MAX_PROPS_COUNT=20000,
    MAX_UCHAR_COUNT=10000,
    MAX_EXCEPTIONS_COUNT=4096,
    MAX_STAGE_2_COUNT=MAX_PROPS_COUNT/8
};

static uint16_t stage1[STAGE_1_BLOCK], stage2[MAX_STAGE_2_COUNT],
                stage3[MAX_PROPS_COUNT], map[MAX_PROPS_COUNT];

/* stage1Top=STAGE_1_BLOCK never changes */
static uint16_t stage2Top=0, stage3Top;

/* props[] is used before, props32[] after compacting the array of properties */
static uint32_t props[MAX_PROPS_COUNT], props32[MAX_PROPS_COUNT];
static uint16_t propsTop=STAGE_3_BLOCK; /* the first props[] are always empty */

/* exceptions values */
static uint16_t exceptions[MAX_EXCEPTIONS_COUNT+20];
static uint16_t exceptionsTop=0;

/* Unicode characters, e.g. for special casing or decomposition */

static UChar uchars[MAX_UCHAR_COUNT+20];
static uint16_t ucharsTop=0;

/* prototypes --------------------------------------------------------------- */

static int
compareProps(const void *l, const void *r);

static uint16_t
addUChars(const UChar *s, uint16_t length);

/* -------------------------------------------------------------------------- */

extern void
initStore() {
    icu_memset(stage1, 0, sizeof(stage1));
    icu_memset(stage2, 0, sizeof(stage2));
    icu_memset(stage3, 0, sizeof(stage3));
    icu_memset(map, 0, sizeof(map));
    icu_memset(props32, 0, sizeof(props32));
}

/* store a character's properties ------------------------------------------- */

extern void
addProps(Props *p) {
    uint16_t count;

    /*
     * Simple ideas for reducing the number of bits for one character's
     * properties:
     *
     * Some fields are only used for characters of certain
     * general categories:
     * - casing fields for letters and others, not for
     *     numbers & Mn
     *   + uppercase not for uppercase letters
     *   + lowercase not for lowercase letters
     *   + titlecase not for titlecase letters
     *
     *   * most of the time, uppercase=titlecase
     * - numeric fields for various digit & other types
     * - canonical combining classes for non-spacing marks (Mn)
     * * the above is not always true, for all three cases
     *
     * Using the same bits for alternate fields saves some space.
     *
     * For the canonical categories, there are only few actually used.
     * They can be stored using 5 bits.
     *
     * In the BiDi categories, the 5 explicit codes are only ever
     * assigned 1:1 to 5 well-known code points. Storing only one
     * value for all "explicit codes" gets this down to 4 bits.
     * Client code then needs to check for this special value
     * and replace it by the real one using a 5-element table.
     *
     * The general categories Mn & Me, non-spacing & enclosing marks,
     * are always NSM, and NSM are always of those categories.
     *
     * Digit values can often be derived from the code point value
     * itself in a simple way.
     *
     */

    /* count the case mappings */
    count=0;
    if(p->upperCase!=0) {
        ++count;
    }
    if(p->lowerCase!=0) {
        ++count;
    }
    if(p->upperCase!=p->titleCase) {
        ++count;
    }

    /* verify that only Mn has a canonical combining class */
    if(p->generalCategory!=U_NON_SPACING_MARK && p->canonicalCombining>0) {
        printf("*** code 0x%06x: canonical combining class does not fit expected range ***\n", p->code);
    }

    /* verify that only numeric categories have numeric values */
    if(genCategoryNames[p->generalCategory][0]!='N' && p->numericValue!=0) {
        printf("*** code 0x%06x: non-numeric category but numeric value\n", p->code);
    }

    /* verify that no numbers and no Mn have case mappings */
    /* this is not 100% true either (see 0345;COMBINING GREEK YPOGEGRAMMENI) */
    if( (genCategoryNames[p->generalCategory][0]=='N' ||
         p->generalCategory==U_NON_SPACING_MARK) &&
        count>0
    ) {
        printf("*** code 0x%06x: number category or Mn but case mapping\n", p->code);
    } else if(count>1) {
        /* see for which characters there are two case mappings */
        /* there are some, but few (12) */
        printf("*** code 0x%06x: more than one case mapping\n", p->code);
    }

    /* verify that { Mn, Me } if and only if NSM */
    if( (p->generalCategory==U_NON_SPACING_MARK ||
         p->generalCategory==U_ENCLOSING_MARK)
        ^
        (p->bidi==U_DIR_NON_SPACING_MARK)) {
        printf("*** code 0x%06x: bidi class does not fit expected range ***\n", p->code);
    }

    /*
     * "Higher-hanging fruit":
     * For some sets of fields, there are fewer sets of values
     * than the product of the numbers of values per field.
     * This means that storing one single value for more than
     * one field and later looking up both field values in a table
     * saves space.
     * Examples:
     * - general category & BiDi
     *
     * There are only few common displacements between a code point
     * and its case mappings. Store deltas. Store codes for few
     * occuring deltas.
     */
}

/* compacting --------------------------------------------------------------- */

extern void
compactStage3() {
    /*
     * At this point, all the propsTop properties are in props[].
     * propsTop is a multiple of 16, and there are always 16 props[] entries
     * per stage 2 entry which do not overlap.
     * The first 16 props[] are always the empty ones.
     * We make them overlap appropriately here and fill every 16th entry in
     * map[] with the mapping from old to new properties indexes
     * in order to adjust the stage 2 tables.
     * This simple algorithm does not find arbitrary overlaps, but only those
     * where the last i properties of the previous group and the first i of the
     * current one all have the same value.
     * This seems reasonable and yields linear performance.
     */
    uint16_t i, start, prevEnd, newStart;
    uint32_t x;

    map[0]=0;
    newStart=STAGE_3_BLOCK;
    for(start=newStart; start<propsTop;) {
        prevEnd=newStart-1;
        x=props[start];
        if(x==props[prevEnd]) {
            /* overlap by at least one */
            for(i=1; i<STAGE_3_BLOCK && x==props[start+i] && x==props[prevEnd-i]; ++i) {}

            /* overlap by i */
            map[start]=newStart-i;

            /* move the non-overlapping properties to their new positions */
            start+=i;
            for(i=STAGE_3_BLOCK-i; i>0; --i) {
                props[newStart++]=props[start++];
            }
        } else if(newStart<start) {
            /* move the properties to their new positions */
            map[start]=newStart;
            for(i=STAGE_3_BLOCK; i>0; --i) {
                props[newStart++]=props[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start]=start;
            newStart+=STAGE_3_BLOCK;
            start=newStart;
        }
    }

    /* we saved some space */
    if(beVerbose) {
        printf("compactStage3() reduced propsTop from %u to %u\n", propsTop, propsTop-(start-newStart));
    }
    propsTop-=(start-newStart);

    /* now adjust the stage 2 tables, skipping the data parts in them */
    for(start=0; start<stage2Top; start+=STAGE_2_BLOCK) {
        for(i=STAGE_2_BLOCK; i>0; --i) {
            stage2[start]=map[stage2[start]];
            ++start;
        }
    }
}

extern void
compactProps() {
    /*
     * At this point, all the propsTop properties are in props[], but they
     * are not all unique.
     * Now we sort them, reduce them to unique ones in props32[], and
     * build an index in stage3[] from the old to the new indexes.
     * (The quick sort averages at N*log(N) with N=propsTop. The inverting
     * yields linear performance.)
     */

    /*
     * We are going to sort only an index table in map[] because we need this
     * index table anyway and qsort() does not allow to sort two tables together
     * directly. This will thus also reduce the amount of data moved around.
     */
    uint16_t i, oldIndex, newIndex;
    uint32_t x;

    /* build the index table */
    for(i=propsTop; i>0;) {
        --i;
        map[i]=i;
    }

    /* do not reorder the first, empty entries */
    qsort(map+STAGE_3_BLOCK, propsTop-STAGE_3_BLOCK, 2, compareProps);

    /*
     * Now invert the reordered table and compact it in the same step.
     * The result will be props32[] having only unique properties words
     * and stage3[] having indexes to them.
     */
    newIndex=0;
    for(i=0; i<propsTop;) {
        /* set the first of a possible series of the same properties */
        oldIndex=map[i];
        props32[newIndex]=x=props[oldIndex];
        stage3[oldIndex]=newIndex;

        /* set the following same properties only in stage3 */
        while(++i<propsTop && x==props[map[i]]) {
            stage3[map[i]]=newIndex;
        }

        ++newIndex;
    }

    /* we saved some space */
    stage3Top=propsTop;
    propsTop=newIndex;
    if(beVerbose) {
        printf("compactProps() reduced propsTop from %u to %u\n", stage3Top, propsTop);
    }
}

static int
compareProps(const void *l, const void *r) {
    uint32_t left=props[*(const uint16_t *)l], right=props[*(const uint16_t *)r];

    /* compare general categories first */
    int rc=(int)(left&0x1f)-(int)(right&0x1f);
    if(rc==0 && left!=right) {
        rc= left<right ? -1 : 1;
    }
    return rc;
}

/* generate output data ----------------------------------------------------- */

extern void
generateData() {
    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t size;
    long dataLength;

    pData=udata_create(DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: unable to create data memory, error %d\n", errorCode);
        exit(errorCode);
    }

    /* ### */
    size=0;

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=(long)size) {
        fprintf(stderr, "genprops: data length %ld != calculated size %lu\n", dataLength, size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

/* helpers ------------------------------------------------------------------ */

static uint16_t
addUChars(const UChar *s, uint16_t length) {
    uint16_t top=ucharsTop+length+1;
    UChar *p;

    if(top>=MAX_UCHAR_COUNT) {
        fprintf(stderr, "genprops: out of UChars memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=uchars+ucharsTop;
    icu_memcpy(p, s, length);
    p[length]=0;
    ucharsTop=top;
    return (uint16_t)(p-uchars);
}
