/*
*******************************************************************************
*
*   Copyright (C) 1999-2004, International Business Machines
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
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "utrie.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "uprops.h"
#include "genprops.h"

#define DO_DEBUG_OUT 0

/* Unicode character properties file format ------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 3 .

Data contents:

The contents is a parsed, binary form of several Unicode character
database files, most prominently UnicodeData.txt.

Any Unicode code point from 0 to 0x10ffff can be looked up to get
the properties, if any, for that code point. This means that the input
to the lookup are 21-bit unsigned integers, with not all of the
21-bit range used.

It is assumed that client code keeps a uint32_t pointer
to the beginning of the data:

    const uint32_t *p32;

Formally, the file contains the following structures:

    const int32_t indexes[16] with values i0..i15:

    i0 propsIndex; -- 32-bit unit index to the table of 32-bit properties words
    i1 exceptionsIndex;  -- 32-bit unit index to the table of 32-bit exception words
    i2 exceptionsTopIndex; -- 32-bit unit index to the array of UChars for special mappings

    i3 additionalTrieIndex; -- 32-bit unit index to the additional trie for more properties
    i4 additionalVectorsIndex; -- 32-bit unit index to the table of properties vectors
    i5 additionalVectorsColumns; -- number of 32-bit words per properties vector

    i6 reservedItemIndex; -- 32-bit unit index to the top of the properties vectors table
    i7..i9 reservedIndexes; -- reserved values; 0 for now

    i10 maxValues; -- maximum code values for vector word 0, see uprops.h (format version 3.1+)
    i11 maxValues2; -- maximum code values for vector word 2, see uprops.h (format version 3.2)
    i12..i15 reservedIndexes; -- reserved values; 0 for now

    PT serialized properties trie, see utrie.h (byte size: 4*(i0-16))

    P  const uint32_t props32[i1-i0];
    E  const uint32_t exceptions[i2-i1];
    U  const UChar uchars[2*(i3-i2)];

    AT serialized trie for additional properties (byte size: 4*(i4-i3))
    PV const uint32_t propsVectors[(i6-i4)/i5][i5]==uint32_t propsVectors[i6-i4];

Trie lookup and properties:

In order to condense the data for the 21-bit code space, several properties of
the Unicode code assignment are exploited:
- The code space is sparse.
- There are several 10k of consecutive codes with the same properties.
- Characters and scripts are allocated in groups of 16 code points.
- Inside blocks for scripts the properties are often repetitive.
- The 21-bit space is not fully used for Unicode.

The lookup of properties for a given code point is done with a trie lookup,
using the UTrie implementation.
The trie lookup result is a 16-bit index in the props32[] table where the
actual 32-bit properties word is stored. This is done to save space.

(There are thousands of 16-bit entries in the trie data table, but
only a few hundred unique 32-bit properties words.
If the trie data table contained 32-bit words directly, then that would be
larger because the length of the table would be the same as now but the
width would be 32 bits instead of 16. This saves more than 10kB.)

With a given Unicode code point

    UChar32 c;

and 0<=c<0x110000, the lookup is done like this:

    uint16_t i;
    UTRIE_GET16(c, i);
    uint32_t props=p32[i];

For some characters, not all of the properties can be efficiently encoded
using 32 bits. For them, the 32-bit word contains an index into the exceptions[]
array:

    if(props&EXCEPTION_BIT)) {
        uint16_t e=(uint16_t)(props>>VALUE_SHIFT);
        ...
    }

The exception values are a variable number of uint32_t starting at

    const uint32_t *pe=p32+exceptionsIndex+e;

The first uint32_t there contains flags about what values actually follow it.
Some of the exception values are UChar32 code points for the case mappings,
others are numeric values etc.

32-bit properties sets:

Each 32-bit properties word contains:

 0.. 4  general category
 5      has exception values
 6..10  BiDi category
11      is mirrored
12..14  numericType:
            0 no numeric value
            1 decimal digit value
            2 digit value
            3 numeric value
            ### TODO: type 4 for Han digits & numbers?!
15..19  reserved
20..31  value according to bits 0..5:
        if(has exception) {
            exception index;
        } else switch(general category) {
        case Ll: delta to uppercase; -- same as titlecase
        case Lu: -delta to lowercase; -- titlecase is same as c
        case Lt: -delta to lowercase; -- uppercase is same as c
        default:
            if(is mirrored) {
                delta to mirror;
            } else if(numericType!=0) {
                numericValue;
            } else {
                0;
            };
        }

Exception values:

In the first uint32_t exception word for a code point,
bits
31..16  reserved
15..0   flags that indicate which values follow:

bit
 0      has uppercase mapping
 1      has lowercase mapping
 2      has titlecase mapping
 3      unused
 4      has numeric value (numerator)
            if numericValue=0x7fffff00+x then numericValue=10^x
 5      has denominator value
 6      has a mirror-image Unicode code point
 7      has SpecialCasing.txt entries
 8      has CaseFolding.txt entries

According to the flags in this word, one or more uint32_t words follow it
in the sequence of the bit flags in the flags word; if a flag is not set,
then the value is missing or 0:

For the case mappings and the mirror-image Unicode code point,
one uint32_t or UChar32 each is the code point.
If the titlecase mapping is missing, then it is the same as the uppercase mapping.

For the digit values, bits 31..16 contain the decimal digit value, and
bits 15..0 contain the digit value. A value of -1 indicates that
this value is missing.

For the numeric/numerator value, an int32_t word contains the value directly,
except for when there is no numerator but a denominator, then the numerator
is implicitly 1. This means:
    numerator denominator result
    none      none        none
    x         none        x
    none      y           1/y
    x         y           x/y

If the numerator value is 0x7fffff00+x then it is replaced with 10^x.

For the denominator value, a uint32_t word contains the value directly.

For special casing mappings, the 32-bit exception word contains:
31      if set, this character has complex, conditional mappings
        that are not stored;
        otherwise, the mappings are stored according to the following bits
30..24  number of UChars used for mappings
23..16  reserved
15.. 0  UChar offset from the beginning of the UChars array where the
        UChars for the special case mappings are stored in the following format:

Format of special casing UChars:
One UChar value with lengths as follows:
14..10  number of UChars for titlecase mapping
 9.. 5  number of UChars for uppercase mapping
 4.. 0  number of UChars for lowercase mapping

Followed by the UChars for lowercase, uppercase, titlecase mappings in this order.

For case folding mappings, the 32-bit exception word contains:
31..24  number of UChars used for the full mapping
23..16  reserved
15.. 0  UChar offset from the beginning of the UChars array where the
        UChars for the special case mappings are stored in the following format:

Format of case folding UChars:
Two UChars contain the simple mapping as follows:
    0,  0   no simple mapping
    BMP,0   a simple mapping to a BMP code point
    s1, s2  a simple mapping to a supplementary code point stored as two surrogates
This is followed by the UChars for the full case folding mappings.

Example:
U+2160, ROMAN NUMERAL ONE, needs an exception because it has a lowercase
mapping and a numeric value.
Its exception values would be stored as 3 uint32_t words:

- flags=0x0a (see above) with combining class 0
- lowercase mapping 0x2170
- numeric value=1

--- Additional properties (new in format version 2.1) ---

The second trie for additional properties (AT) is also a UTrie with 16-bit data.
The data words consist of 32-bit unit indexes (not row indexes!) into the
table of unique properties vectors (PV).
Each vector contains a set of properties.
The width of a vector (number of uint32_t per row) may change
with the formatVersion, it is stored in i5.

Current properties: see icu/source/common/uprops.h

--- Changes in format version 3.1 ---

See i10 maxValues above, contains only UBLOCK_COUNT and USCRIPT_CODE_LIMIT.

--- Changes in format version 3.2 ---

- The tries use linear Latin-1 ranges.
- The additional properties bits store full properties XYZ instead
  of partial Other_XYZ, so that changes in the derivation formulas
  need not be tracked in runtime library code.
- Joining Type and Line Break are also stored completely, so that uprops.c
  needs no runtime formulas for enumerated properties either.
- Store the case-sensitive flag in the main properties word.
- i10 also contains U_LB_COUNT and U_EA_COUNT.
- i11 contains maxValues2 for vector word 2.

----------------------------------------------------------------------------- */

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x55, 0x50, 0x72, 0x6f },                 /* dataFormat="UPro" */
    { 3, 2, UTRIE_SHIFT, UTRIE_INDEX_SHIFT },   /* formatVersion */
    { 4, 0, 1, 0 }                              /* dataVersion */
};

/* definitions of expected data size limits */
enum {
    MAX_PROPS_COUNT=25000,
    MAX_UCHAR_COUNT=10000
};

static UNewTrie *pTrie=NULL;

/* props32[] contains unique properties words after compacting the array of properties */
static uint32_t props32[MAX_PROPS_COUNT];

/* context pointer for compareProps() - temporarily holds a pointer to the trie data */
static uint32_t *props;

/* length of props32[] after compaction */
static int32_t propsTop;

/* exceptions values */
static uint32_t exceptions[UPROPS_MAX_EXCEPTIONS_COUNT+20];
static uint16_t exceptionsTop=0;

/* Unicode characters, e.g. for special casing or decomposition */
static UChar uchars[MAX_UCHAR_COUNT+20];
static uint32_t ucharsTop=0;

/* statistics */
static uint16_t exceptionsCount=0;

/* prototypes --------------------------------------------------------------- */

static int
compareProps(const void *l, const void *r);

static uint32_t
addUChars(const UChar *s, uint32_t length);

/* -------------------------------------------------------------------------- */

extern void
setUnicodeVersion(const char *v) {
    UVersionInfo version;
    u_versionFromString(version, v);
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

extern void
initStore() {
    pTrie=utrie_open(NULL, NULL, MAX_PROPS_COUNT, 0, 0, TRUE);
    if(pTrie==NULL) {
        fprintf(stderr, "error: unable to create a UNewTrie\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }

    uprv_memset(props32, 0, sizeof(props32));
    initAdditionalProperties();
}

/* store a character's properties ------------------------------------------- */

extern uint32_t
makeProps(Props *p) {
    uint32_t x;
    int32_t value;
    uint16_t count;
    UBool isNumber;

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
     * For the canonical categories, there are only few actually used
     * most of the time.
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

    /* count the case mappings and other values competing for the value bit field */
    x=0;
    value=0;
    count=0;
    isNumber= (UBool)(genCategoryNames[p->generalCategory][0]=='N');

    if(p->upperCase!=0) {
        /* verify that no numbers and no Mn have case mappings */
        if(p->generalCategory==U_LOWERCASE_LETTER) {
            value=(int32_t)p->code-(int32_t)p->upperCase;
        } else {
            x=UPROPS_EXCEPTION_BIT;
        }
        ++count;
    }
    if(p->lowerCase!=0) {
        /* verify that no numbers and no Mn have case mappings */
        if(p->generalCategory==U_UPPERCASE_LETTER || p->generalCategory==U_TITLECASE_LETTER) {
            value=(int32_t)p->lowerCase-(int32_t)p->code;
        } else {
            x=UPROPS_EXCEPTION_BIT;
        }
        ++count;
    }
    if(p->upperCase!=p->titleCase) {
        x=UPROPS_EXCEPTION_BIT;
        ++count;
    }
    if(p->numericType!=0) {
        value=p->numericValue;
        ++count;
    }
    if(p->denominator!=0) {
        x=UPROPS_EXCEPTION_BIT;
        ++count;
    }
    if(p->isMirrored) {
        if(p->mirrorMapping!=0) {
            value=(int32_t)p->mirrorMapping-(int32_t)p->code;
        }
        ++count;
    }
    if(p->specialCasing!=NULL) {
        x=UPROPS_EXCEPTION_BIT;
        ++count;
    }
    if(p->caseFolding!=NULL) {
        x=UPROPS_EXCEPTION_BIT;
        ++count;
    }

    /* handle exceptions */
    if(count>1 || x!=0 || value<UPROPS_MIN_VALUE || UPROPS_MAX_VALUE<value) {
        /* this code point needs exception values */
        if(beVerbose) {
            if(x!=0) {
                /* do not print - many code points because of SpecialCasing & CaseFolding
                printf("*** code 0x%06x needs an exception because it is irregular\n", p->code);
                */
            } else if(value<UPROPS_MIN_VALUE || UPROPS_MAX_VALUE<value) {
                printf("*** U+%04x needs an exception because its value is out-of-bounds at %ld (not [%ld..%ld]\n",
                    (int)p->code, (long)value, (long)UPROPS_MIN_VALUE, (long)UPROPS_MAX_VALUE);
            } else {
                printf("*** U+%04x needs an exception because it has %u values\n",
                    (int)p->code, count);
            }
        }

        ++exceptionsCount;
        x=UPROPS_EXCEPTION_BIT;

        /* allocate and create exception values */
        value=exceptionsTop;
        if(value>=UPROPS_MAX_EXCEPTIONS_COUNT) {
            fprintf(stderr, "genprops: out of exceptions memory at U+%06x. (%d exceeds allocated space)\n",
                    (int)p->code, (int)value);
            exit(U_MEMORY_ALLOCATION_ERROR);
        } else {
            uint32_t first=0;
            uint16_t length=1;

            if(p->upperCase!=0) {
                first|=1;
                exceptions[value+length++]=p->upperCase;
            }
            if(p->lowerCase!=0) {
                first|=2;
                exceptions[value+length++]=p->lowerCase;
            }
            if(p->upperCase!=p->titleCase) {
                first|=4;
                if(p->titleCase!=0) {
                    exceptions[value+length++]=p->titleCase;
                } else {
                    exceptions[value+length++]=p->code;
                }
            }
            if(p->numericType!=0) {
                if(p->denominator==0) {
                    first|=0x10;
                    exceptions[value+length++]=(uint32_t)p->numericValue;
                } else {
                    if(p->numericValue!=1) {
                        first|=0x10;
                        exceptions[value+length++]=(uint32_t)p->numericValue;
                    }
                    first|=0x20;
                    exceptions[value+length++]=p->denominator;
                }
            }
            if(p->isMirrored) {
                first|=0x40;
                exceptions[value+length++]=p->mirrorMapping;
            }
            if(p->specialCasing!=NULL) {
                first|=0x80;
                if(p->specialCasing->isComplex) {
                    /* complex special casing */
                    exceptions[value+length++]=0x80000000;
                } else {
                    /* unconditional special casing */
                    UChar u[128];
                    uint32_t i;
                    uint16_t j, entry;

                    i=1;
                    entry=0;
                    j=p->specialCasing->lowerCase[0];
                    if(j>0) {
                        uprv_memcpy(u+1, p->specialCasing->lowerCase+1, 2*j);
                        i+=j;
                        entry=j;
                    }
                    j=p->specialCasing->upperCase[0];
                    if(j>0) {
                        uprv_memcpy(u+i, p->specialCasing->upperCase+1, 2*j);
                        i+=j;
                        entry|=j<<5;
                    }
                    j=p->specialCasing->titleCase[0];
                    if(j>0) {
                        uprv_memcpy(u+i, p->specialCasing->titleCase+1, 2*j);
                        i+=j;
                        entry|=j<<10;
                    }
                    u[0]=entry;

                    exceptions[value+length++]=(i<<24)|addUChars(u, i);
                }
            }
            if(p->caseFolding!=NULL) {
                first|=0x100;
                if(p->caseFolding->simple==0 && p->caseFolding->full[0]==0) {
                    /* special case folding, store only a marker */
                    exceptions[value+length++]=0;
                } else {
                    /* normal case folding with a simple and a full mapping */
                    UChar u[128];
                    uint16_t i;

                    /* store the simple mapping into the first two UChars */
                    i=0;
                    u[1]=0;
                    UTF_APPEND_CHAR_UNSAFE(u, i, p->caseFolding->simple);

                    /* store the full mapping after that */
                    i=p->caseFolding->full[0];
                    if(i>0) {
                        uprv_memcpy(u+2, p->caseFolding->full+1, 2*i);
                    }

                    exceptions[value+length++]=(i<<24)|addUChars(u, 2+i);
                }
            }
            exceptions[value]=first;
            exceptionsTop+=length;
        }
    }

    /* put together the 32-bit word of encoded properties */
    x|=
        (uint32_t)p->generalCategory |
        (uint32_t)p->bidi<<UPROPS_BIDI_SHIFT |
        (uint32_t)p->isMirrored<<UPROPS_MIRROR_SHIFT |
        (uint32_t)p->numericType<<UPROPS_NUMERIC_TYPE_SHIFT |
        (uint32_t)value<<UPROPS_VALUE_SHIFT;

    return x;

    /*
     * "Higher-hanging fruit" (not implemented):
     *
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

extern void
addProps(uint32_t c, uint32_t x) {
    if(!utrie_set32(pTrie, (UChar32)c, x)) {
        fprintf(stderr, "error: too many entries for the properties trie\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }
}

extern void
addCaseSensitive(UChar32 first, UChar32 last) {
    uint32_t x, cs;

    cs=U_MASK(UPROPS_CASE_SENSITIVE_SHIFT);
    while(first<=last) {
        x=utrie_get32(pTrie, first, NULL);
        if(!utrie_set32(pTrie, first, x|cs)) {
            fprintf(stderr, "error: too many entries for the properties trie\n");
            exit(U_BUFFER_OVERFLOW_ERROR);
        }
        ++first;
    }
}

extern uint32_t
getProps(uint32_t c) {
    return utrie_get32(pTrie, (UChar32)c, NULL);
}

/* areas of same properties ------------------------------------------------- */

extern void
repeatProps(uint32_t first, uint32_t last, uint32_t x) {
    if(!utrie_setRange32(pTrie, (UChar32)first, (UChar32)(last+1), x, FALSE)) {
        fprintf(stderr, "error: too many entries for the properties trie\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }
}

/* compacting --------------------------------------------------------------- */

static void
compactProps(void) {
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
    uint32_t x;
    int32_t i, oldIndex, newIndex;

    static uint16_t map[MAX_PROPS_COUNT];

#if DO_DEBUG_OUT
    {
        /* debug output */
        uint16_t i1, i2, i3;
        uint32_t c;
        for(c=0; c<0xffff; c+=307) {
            printf("properties(0x%06x)=0x%06x\n", c, getProps(c, &i1, &i2, &i3));
        }
    }
#endif

    props=utrie_getData(pTrie, &propsTop);

    /* build the index table */
    for(i=propsTop; i>0;) {
        --i;
        map[i]=(uint16_t)i;
    }

    /* reorder */
    qsort(map, propsTop, 2, compareProps);

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
        props[oldIndex]=newIndex;

        /* set the following same properties only in stage3 */
        while(++i<propsTop && x==props[map[i]]) {
            props[map[i]]=newIndex;
        }

        ++newIndex;
    }

    /* we saved some space */
    if(beVerbose) {
        printf("compactProps() reduced propsTop from %u to %u\n",
            (int)propsTop, (int)newIndex);
    }
    propsTop=newIndex;

#if DO_DEBUG_OUT
    {
        /* debug output */
        uint16_t i1, i2, i3, i4;
        uint32_t c;
        for(c=0; c<0xffff; c+=307) {
            printf("properties(0x%06x)=0x%06x\n", c, getProps2(c, &i1, &i2, &i3, &i4));
        }
    }
#endif
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

/* folding value: just store the offset (16 bits) if there is any non-0 entry */
U_CFUNC uint32_t U_EXPORT2
getFoldedPropsValue(UNewTrie *trie, UChar32 start, int32_t offset) {
    uint32_t value;
    UChar32 limit;
    UBool inBlockZero;

    limit=start+0x400;
    while(start<limit) {
        value=utrie_get32(trie, start, &inBlockZero);
        if(inBlockZero) {
            start+=UTRIE_DATA_BLOCK_LENGTH;
        } else if(value!=0) {
            return (uint32_t)(offset|0x8000);
        } else {
            ++start;
        }
    }
    return 0;
}

extern void
generateData(const char *dataDir) {
    static int32_t indexes[UPROPS_INDEX_COUNT]={
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0
    };
    static uint8_t trieBlock[40000];
    static uint8_t additionalProps[120000];

    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t size;
    int32_t trieSize, additionalPropsSize, offset;
    long dataLength;

    compactProps();

    trieSize=utrie_serialize(pTrie, trieBlock, sizeof(trieBlock), getFoldedPropsValue, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error: utrie_serialize failed: %s (length %ld)\n", u_errorName(errorCode), (long)trieSize);
        exit(errorCode);
    }

    offset=sizeof(indexes)/4;               /* uint32_t offset to the properties trie */

    /* round up trie size to 4-alignement */
    trieSize=(trieSize+3)&~3;
    offset+=trieSize>>2;
    indexes[UPROPS_PROPS32_INDEX]=offset;   /* uint32_t offset to props[] */

    offset+=propsTop;
    indexes[UPROPS_EXCEPTIONS_INDEX]=offset;/* uint32_t offset to exceptions[] */

    offset+=exceptionsTop;                  /* uint32_t offset to the first unit after exceptions[] */
    indexes[UPROPS_EXCEPTIONS_TOP_INDEX]=offset;

    /* round up UChar count to 4-alignement */
    ucharsTop=(ucharsTop+1)&~1;
    offset+=(uint16_t)(ucharsTop/2);        /* uint32_t offset to the first unit after uchars[] */
    indexes[UPROPS_ADDITIONAL_TRIE_INDEX]=offset;

    if(beVerbose) {
        printf("trie size in bytes:                    %5u\n", (int)trieSize);
        printf("number of unique properties values:    %5u\n", (int)propsTop);
        printf("number of code points with exceptions: %5u\n", exceptionsCount);
        printf("size in bytes of exceptions:           %5u\n", 4*exceptionsTop);
        printf("number of UChars for special mappings: %5u\n", (int)ucharsTop);
    }

    additionalPropsSize=writeAdditionalData(additionalProps, sizeof(additionalProps), indexes);

    size=4*offset+additionalPropsSize;      /* total size of data */
    if(beVerbose) {
        printf("data size:                            %6lu\n", (unsigned long)size);
    }

    /* write the data */
    pData=udata_create(dataDir, DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: unable to create data memory, %s\n", u_errorName(errorCode));
        exit(errorCode);
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, props32, 4*propsTop);
    udata_writeBlock(pData, exceptions, 4*exceptionsTop);
    udata_writeBlock(pData, uchars, 2*ucharsTop);
    udata_writeBlock(pData, additionalProps, additionalPropsSize);

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=(long)size) {
        fprintf(stderr, "genprops: data length %ld != calculated size %lu\n",
            dataLength, (unsigned long)size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }

    utrie_close(pTrie);
}

/* helpers ------------------------------------------------------------------ */

static uint32_t
addUChars(const UChar *s, uint32_t length) {
    uint32_t top=(uint16_t)(ucharsTop+length);
    UChar *p;

    if(top>=MAX_UCHAR_COUNT) {
        fprintf(stderr, "genprops: out of UChars memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=uchars+ucharsTop;
    uprv_memcpy(p, s, 2*length);
    ucharsTop=top;
    return (uint32_t)(p-uchars);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
