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
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "genprops.h"

#define DO_DEBUG_OUT 0

/* Unicode character properties file format ------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 1.0 .


Data contents:

The contents is a parsed, binary form of several Unicode character
database files, most prominently UnicodeData.txt.

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

    A0 const uint16_t STAGE_2_BITS(=6);
    A1 const uint16_t STAGE_3_BITS(=4);
      (STAGE_1_BITS(=11) not stored, implicitly=21-(STAGE_2_BITS+STAGE_3_BITS))
    A2 const uint16_t exceptionsIndex;  -- 32-bit unit index
    A3 const uint16_t reservedIndex;
    A4 const uint16_t reservedIndex;
    A5 const uint16_t reservedIndex;
    A6 const uint16_t reservedIndex;
    A7 const uint16_t reservedIndex;

    S1 const uint16_t stage1[0x440];    -- 0x440=0x110000>>10
    S2 const uint16_t stage2[variable size];
    S3 const uint16_t stage3[variable size];
       (possible 1*uint16_t for padding to 4-alignment)

    P  const uint32_t props32[variable size];
    E  const uint32_t exceptions[variable size];

3-stage lookup and properties:

In order to condense the data for the 21-bit code space, several properties of
the Unicode code assignment are exploited:
- The code space is sparse.
- There are several 10k of consecutive codes with the same properties.
- Characters and scripts are allocated in groups of 16 code points.
- Inside blocks for scripts the properties are often repetitive.
- The 21-bit space is not fully used for Unicode.

The three-stage lookup organizes code points in groups of 16 in stage 3.
64 such groups are grouped again, resulting in blocks of 64 indexes
for a total of 1k code points in stage 2.
The first stage is limited according to all code points being <0x110000.
Each stage contains indexes to groups or blocks of the next stage
in an n:1 manner, i.e., multiple entries of one stage may index the same
group or block in the next one.
In the second and third stages, groups of 64 or 16 may partially or completely
overlap to save space with repetitive properties.
In the properties table, only unique 32-bit words are stored to exploit
non-adjacent overlapping. This is why the third stage does not directly
contain the 32-bit properties words but only indexes to them.

The indexes in each stage take the offset in the data of the next block into
account to save additional arithmetic in the access.

With a given Unicode code point

    uint32_t c;

and 0<=c<0x110000, the lookup uses the three stage tables to
arrive at an index into the props32[] table containing the character
properties for c.
For some characters, not all of the properties can be efficiently encoded
using 32 bits. For them, the 32-bit word contains an index into the exceptions[]
array.

The first stage consumes the 11 most significant bits of the 21-bit code point
and results in an index into the second stage:

    uint16_t i2=p16[8+c>>10];

The second stage consumes bits 9 to 4 of c and results in an index into the
third stage:

    uint16_t i3=p16[i2+((c>>4)&0x3f)];

The third stage consumes bits 3 to 0 of c and results in a code point-
specific value, which itself is only an index into the props32[] table:

    uint16_t i=p16[i3+(c&0xf)];

Note that the bit numbers and shifts actually depend on the STAGE_2/3_BITS
in p16[0..1].

There is finally the 32-bit encoded set of properties for c:

    uint32_t props=p32[i];

For some characters, this contains an index into the exceptions array:

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
12..19  reserved
20..31  value according to bits 0..5:
        if(has exception) {
            exception index;
        } else switch(general category) {
        case Ll: delta to uppercase; -- same as titlecase
        case Lu: -delta to lowercase; -- titlecase is same as c
        case Lt: -delta to lowercase; -- uppercase is same as c
        case Mn: combining class;
        case Nd: value=numeric value==decimal digit value=digit value;
        case Nl:
        case No: value=numeric value - but decimal digit value and digit value are not defined;
        default:
            if(is mirrored) {
                delta to mirror
            } else {
                0
            };
        }

Exception values:

In the first uint32_t exception word for a code point,
bits
31..24  reserved
23..16  combining class
15..0   flags that indicate which values follow:

bit
 0      has uppercase mapping
 1      has lowercase mapping
 2      has titlecase mapping
 3      has digit value(s)
 4      has numeric value (numerator)
 5      has denominator value
 6      has a mirror-image Unicode code point

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

For the denominator value, a uint32_t word contains the value directly.

Example:
U+2160, ROMAN NUMERAL ONE, needs an exception because it has a lowercase
mapping and a numeric value.
Its exception values would be stored as 3 uint32_t words:

- flags=0x0a (see above) with combining class 0
- lowercase mapping 0x2170
- numeric value=1

----------------------------------------------------------------------------- */

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
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
    STAGE_2_BITS=6, STAGE_3_BITS=4,
    STAGE_1_BITS=21-(STAGE_2_BITS+STAGE_3_BITS),

    STAGE_2_SHIFT=STAGE_3_BITS,
    STAGE_1_SHIFT=(STAGE_2_SHIFT+STAGE_2_BITS),

    /* number of entries per sub-table in each stage */
    STAGE_1_BLOCK=0x110000>>STAGE_1_SHIFT,
    STAGE_2_BLOCK=1<<STAGE_2_BITS,
    STAGE_3_BLOCK=1<<STAGE_3_BITS,

    /* number of code points per stage 1 index */
    STAGE_2_3_AREA=1<<STAGE_1_SHIFT,

    MAX_PROPS_COUNT=25000,
    MAX_UCHAR_COUNT=10000,
    MAX_EXCEPTIONS_COUNT=4096,
    MAX_STAGE_2_COUNT=MAX_PROPS_COUNT
};

/* definitions for the properties words */
enum {
    EXCEPTION_SHIFT=5,
    BIDI_SHIFT,
    MIRROR_SHIFT=BIDI_SHIFT+5,
    VALUE_SHIFT=20,

    EXCEPTION_BIT=1UL<<EXCEPTION_SHIFT,
    VALUE_BITS=32-VALUE_SHIFT
};

static const int32_t MAX_VALUE=(1L<<(VALUE_BITS-1))-1;
static const int32_t MIN_VALUE=-(1L<<(VALUE_BITS-1));

static uint16_t stage1[STAGE_1_BLOCK], stage2[MAX_STAGE_2_COUNT],
                stage3[MAX_PROPS_COUNT], map[MAX_PROPS_COUNT];

/* stage1Top=STAGE_1_BLOCK never changes, stage2Top starts after the empty-properties-group */
static uint16_t stage2Top=STAGE_2_BLOCK, stage3Top;

/* props[] is used before, props32[] after compacting the array of properties */
static uint32_t props[MAX_PROPS_COUNT], props32[MAX_PROPS_COUNT];
static uint16_t propsTop=STAGE_3_BLOCK; /* the first props[] are always empty */

/* exceptions values */
static uint32_t exceptions[MAX_EXCEPTIONS_COUNT+20];
static uint16_t exceptionsTop=0;

/* Unicode characters, e.g. for special casing or decomposition */
static UChar uchars[MAX_UCHAR_COUNT+20];
static uint16_t ucharsTop=0;

/* statistics */
static uint16_t exceptionsCount=0;

/* prototypes --------------------------------------------------------------- */

static uint16_t
repeatFromStage2(uint16_t i2, uint16_t i2Limit, uint16_t i3Repeat, uint32_t x);

static void
repeatFromStage3(uint16_t i2, uint16_t j3, uint32_t x);

static uint16_t
compactStage(uint16_t *stage, uint16_t stageTop, uint16_t blockSize,
             uint16_t *parent, uint16_t parentTop);

static int
compareProps(const void *l, const void *r);

static uint32_t
getProps2(uint32_t c, uint16_t *pI1, uint16_t *pI2, uint16_t *pI3, uint16_t *pI4);

static uint32_t
getProps(uint32_t c, uint16_t *pI1, uint16_t *pI2, uint16_t *pI3);

static void
setProps(uint32_t c, uint32_t x, uint16_t *pI1, uint16_t *pI2, uint16_t *pI3);

static uint16_t
allocStage2(void);

static uint16_t
allocProps(void);

static uint16_t
addUChars(const UChar *s, uint16_t length);

/* -------------------------------------------------------------------------- */

/* ### this must become public in putil.c */
static void
__versionFromString(UVersionInfo versionArray, const char *versionString) {
    char *end;
    uint16_t part=0;

    if(versionArray==NULL) {
        return;
    }

    if(versionString!=NULL) {
        for(;;) {
            versionArray[part]=(uint8_t)uprv_strtoul(versionString, &end, 10);
            if(end==versionString || ++part==U_MAX_VERSION_LENGTH || *end!=U_VERSION_DELIMITER) {
                break;
            }
            versionString=end+1;
        }
    }

    while(part<U_MAX_VERSION_LENGTH) {
        versionArray[part++]=0;
    }
}

extern void
setUnicodeVersion(const char *v) {
    UVersionInfo version;
    __versionFromString(version, v);
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

extern void
initStore() {
    uprv_memset(stage1, 0, sizeof(stage1));
    uprv_memset(stage2, 0, sizeof(stage2));
    uprv_memset(stage3, 0, sizeof(stage3));
    uprv_memset(map, 0, sizeof(map));
    uprv_memset(props, 0, sizeof(props));
    uprv_memset(props32, 0, sizeof(props32));
}

/* store a character's properties ------------------------------------------- */

extern void
addProps(Props *p) {
    uint32_t x;
    int32_t value;
    uint16_t count;
    bool_t isNumber;

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
    isNumber= genCategoryNames[p->generalCategory][0]=='N';

    if(p->upperCase!=0) {
        /* verify that no numbers and no Mn have case mappings */
        if(p->generalCategory==U_LOWERCASE_LETTER) {
            value=(int32_t)p->code-(int32_t)p->upperCase;
        } else {
            x=EXCEPTION_BIT;
        }
        ++count;
    }
    if(p->lowerCase!=0) {
        /* verify that no numbers and no Mn have case mappings */
        if(p->generalCategory==U_UPPERCASE_LETTER || p->generalCategory==U_TITLECASE_LETTER) {
            value=(int32_t)p->lowerCase-(int32_t)p->code;
        } else {
            x=EXCEPTION_BIT;
        }
        ++count;
    }
    if(p->upperCase!=p->titleCase) {
        x=EXCEPTION_BIT;
        ++count;
    }
    if(p->canonicalCombining>0) {
        /* verify that only Mn has a canonical combining class */
        if(p->generalCategory==U_NON_SPACING_MARK) {
            value=p->canonicalCombining;
        } else {
            x=EXCEPTION_BIT;
        }
        ++count;
    }
    if(p->generalCategory==U_DECIMAL_DIGIT_NUMBER) {
        /* verify that all numeric fields contain the same value */
        if(p->decimalDigitValue!=-1 && p->digitValue==p->decimalDigitValue &&
           p->hasNumericValue && p->numericValue==p->decimalDigitValue &&
           p->denominator==0
        ) {
            value=p->decimalDigitValue;
        } else {
            x=EXCEPTION_BIT;
        }
        ++count;
    } else if(p->generalCategory==U_LETTER_NUMBER || p->generalCategory==U_OTHER_NUMBER) {
        /* verify that only the numeric value field itself contains a value */
        if(p->decimalDigitValue==-1 && p->digitValue==-1 && p->hasNumericValue) {
            value=p->numericValue;
        } else {
            x=EXCEPTION_BIT;
        }
        ++count;
    } else if(p->decimalDigitValue!=-1 || p->digitValue!=-1 || p->hasNumericValue) {
        /* verify that only numeric categories have numeric values */
        x=EXCEPTION_BIT;
        ++count;
    }
    if(p->denominator!=0) {
        /* verification for numeric category covered by the above */
        x=EXCEPTION_BIT;
        ++count;
    }
    if(p->isMirrored) {
        if(p->mirrorMapping!=0) {
            value=(int32_t)p->mirrorMapping-(int32_t)p->code;
        }
        ++count;
    }

    /* handle exceptions */
    if(count>1 || x!=0 || value<MIN_VALUE || MAX_VALUE<value) {
        /* this code point needs exception values */
        if(beVerbose) {
            if(x!=0) {
                printf("*** code 0x%06x needs an exception because it is irregular\n", p->code);
            } else if(count==1) {
                printf("*** code 0x%06x needs an exception because its value would be %ld\n", p->code, value);
            } else if(value<MIN_VALUE || MAX_VALUE<value) {
                printf("*** code 0x%06x needs an exception because its value is out-of-bounds at %ld (not [%ld..%ld]\n", p->code, value, MIN_VALUE,MAX_VALUE);
            } else {
                printf("*** code 0x%06x needs an exception because it has %u values\n", p->code, count);
            }
        }

        ++exceptionsCount;
        x=EXCEPTION_BIT;

        /* allocate and create exception values */
        value=exceptionsTop;
        if(value>=4096) {
            fprintf(stderr, "genprops: out of exceptions memory at U+%06x. (%d exceeds allocated space)\n",
                    p->code, value);
            exit(U_MEMORY_ALLOCATION_ERROR);
        } else {
            uint32_t first=(uint32_t)p->canonicalCombining<<16;
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
            if(p->decimalDigitValue!=-1 || p->digitValue!=-1) {
                first|=8;
                exceptions[value+length++]=
                    (uint32_t)p->decimalDigitValue<<16|
                    (uint16_t)p->digitValue;
            }
            if(p->hasNumericValue) {
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

            exceptions[value]=first;
            exceptionsTop+=length;
        }
    }

    /* put together the 32-bit word of encoded properties */
    x|=
        (uint32_t)p->generalCategory |
        (uint32_t)p->bidi<<BIDI_SHIFT |
        (uint32_t)p->isMirrored<<MIRROR_SHIFT |
        (uint32_t)value<<VALUE_SHIFT;

    setProps(p->code, x, &count, &count, &count);

    if(beVerbose && p->code<=0x9f) {
        if(p->code==0) {
            printf("static uint32_t staticProps32Table[0xa0]={\n");
        }
        if(x&EXCEPTION_BIT) {
            /* do something more intelligent if there is an exception */
            printf("    /* 0x%02lx */ 0x%lx, /* has exception */\n", p->code, x&~EXCEPTION_BIT);
        } else {
            printf("    /* 0x%02lx */ 0x%lx,\n", p->code, x);
        }
        if(p->code==0x9f) {
            printf("};\n");
        }
    }

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

/* areas of same properties ------------------------------------------------- */

extern void
repeatProps(void) {
    /* first and last code points in repetitive areas */
    static const uint32_t areas[][2]={
        { 0x3400, 0x4db5 },     /* CJK ext. A */
        { 0x4e00, 0x9fa5 },     /* CJK */
        { 0xac00, 0xd7a3 },     /* Hangul */
        { 0xd800, 0xdb7f },     /* High Surrogates */
        { 0xdb80, 0xdbff },     /* Private Use High Surrogates */
        { 0xdc00, 0xdfff },     /* Low Surrogates */
        { 0xe000, 0xf8ff },     /* Private Use */
        { 0xf0000, 0x10ffff }   /* Private Use */
    };

    /*
     * Set the repetitive properties for the big, known areas of all the same
     * character properties. Most of those will share the same stage 2 and 3
     * tables.
     *
     * Assumptions:
     * - each area starts at a code point that is a multiple of 16
     * - for each area, except the plane 15/16 private use one,
     *   the first and last code points are defined in UnicodeData.txt
     *   and thus already set
     * - there may be some properties already stored for some code points,
     *   especially in the Private Use areas
     */

    uint16_t i1, i2, i3, j3, i1Limit, i2Repeat, i3Repeat, area;
    uint32_t x, start, limit;

    /* set the properties for the plane 15/16 Private Use area if necessary */
    if(getProps(0xf0000, &i1, &i2, &i3)==0) {
        setProps(0xf0000, getProps(0xe000, &i1, &i2, &i3), &i1, &i2, &i3);
    }

    /* fill in the repetitive properties */
    for(area=0; area<sizeof(areas)/sizeof(areas[0]); ++area) {
        start=areas[area][0];
        limit=areas[area][1]+1;

        /* get the properties from the preset first code point */
        x=getProps(start, &i1, &i2, &i3);

        /* i1, i2, and i3 are set for the start code point */
        i1Limit=(uint16_t)(limit>>STAGE_1_SHIFT);

        /* assume that i3 is the beginning of a stage 3 block (see assumptions above) */

        /* is this stage 3 block suitable for setting it everywhere? (set i3Repeat) */
        for(j3=1;;) {
            if(!(props[i3+j3]==0 || props[i3+j3]==x)) {
                /* this block contains different properties, we need a new one */
                i3Repeat=allocProps();
                break;
            }
            if(++j3==STAGE_3_BLOCK) {
                /* this block is good */
                i3Repeat=i3;
                break;
            }
        }

        /* fill the repetitive block */
        for(j3=0; j3<STAGE_3_BLOCK; ++j3) {
            props[i3Repeat+j3]=x;
        }

        /*
         * now there are up to three sub-areas:
         * - a range of code points before the first full block for
         *   one stage 1 index
         * - a (big) range of code points within full blocks for
         *   stage 1 indexes
         * - a range of code points after the last full block for
         *   one stage 1 index
         */

        if((start&(STAGE_2_3_AREA-1))!=0) {
            /*
             * fill the area before the first full block;
             * assume that the start value is set and therefore
             * at least stage 2 is allocated
             */

            /* fill stages 2 & 3 */
            if(i1<i1Limit) {
                i2=repeatFromStage2(i2, (uint16_t)((i2+STAGE_2_BLOCK)&~(STAGE_2_BLOCK-1)), i3Repeat, x);
            } else {
                /* there is no full block at all - fill stages 2 & 3 only inside this block */
                i2=repeatFromStage2(i2, (uint16_t)(stage1[i1]+((limit>>STAGE_2_SHIFT)&(STAGE_2_BLOCK-1))), i3Repeat, x);

                /* does this area end in an incomplete stage 3 block? */
                repeatFromStage3(i2, (uint16_t)(limit&(STAGE_3_BLOCK-1)), x);
                return;
            }

            /* this stage 2 block will not be suitable for repetition */
            i2Repeat=0;

            /* advance i1 to the first full block */
            ++i1;
        } else {
            uint16_t j2;

            /* is this stage 2 block suitable for setting it everywhere? (set i2Repeat) */
            for(j2=0;;) {
                if(!(stage2[i2+j2]==0 || stage2[i2+j2]==i3Repeat)) {
                    /* this block contains different indexes, we will need a new one */
                    i2Repeat=0;
                    break;
                }
                if(++j2==STAGE_2_BLOCK) {
                    /* this block is good, set and fill it */
                    i2Repeat=i2;
                    for(j2=0; j2<STAGE_2_BLOCK; ++j2) {
                        stage2[i2Repeat+j2]=i3Repeat;
                    }
                    break;
                }
            }
        }

        if(i1<i1Limit) {
            /* fill whole blocks for stage 1 indexes */

            /* fill all the stages 1 to 3 */
            do {
                i2=stage1[i1];
                if(i2==0) {
                    /* set the index for common repeat block for stage 2 */
                    if(i2Repeat==0) {
                        /* allocate and fill a stage 2 block for this */
                        uint16_t j2;

                        i2Repeat=allocStage2();
                        for(j2=0; j2<STAGE_2_BLOCK; ++j2) {
                            stage2[i2Repeat+j2]=i3Repeat;
                        }
                    }
                    stage1[i1]=i2Repeat;
                } else {
                    /* some properties are set in this stage 2 block */
                    repeatFromStage2(i2, (uint16_t)(i2+STAGE_2_BLOCK), i3Repeat, x);
                }
            } while(++i1<i1Limit);
        }

        if((limit&(STAGE_2_3_AREA-1))!=0) {
            /* fill the area after the last full block */
            i2=stage1[i1];
            if(i2==0) {
                i2=allocStage2();
            }
            i2=repeatFromStage2(i2, (uint16_t)(i2+((limit>>STAGE_2_SHIFT)&(STAGE_2_BLOCK-1))), i3Repeat, x);

            /* does this area end in an incomplete stage 3 block? */
            repeatFromStage3(i2, (uint16_t)(limit&(STAGE_3_BLOCK-1)), x);
        }
    }
}

static uint16_t
repeatFromStage2(uint16_t i2, uint16_t i2Limit, uint16_t i3Repeat, uint32_t x) {
    /* set a section of a stage 2 table and its properties to x */
    uint16_t i3, j3;

    while(i2<i2Limit) {
        i3=stage2[i2];
        if(i3==0) {
            stage2[i2]=i3Repeat;
        } else {
            /* some properties are set in this stage 3 block */
            j3=STAGE_3_BLOCK;
            do {
                if(props[i3]==0) {
                    props[i3]=x;
                }
                ++i3;
            } while(--j3>0);
        }
        ++i2;
    }
    return i2;
}

static void
repeatFromStage3(uint16_t i2, uint16_t j3, uint32_t x) {
    if(j3!=0) {
        /* fill in properties in a last, incomplete stage 3 block */
        uint16_t i3=stage2[i2];
        if(i3==0) {
            stage2[i2]=i3=allocProps();
        }

        /* some properties may be set in this stage 3 block */
        do {
            if(props[i3]==0) {
                props[i3]=x;
            }
            ++i3;
        } while(--j3>0);
    }
}

/* compacting --------------------------------------------------------------- */

extern void
compactStage2(void) {
    uint16_t newTop=compactStage(stage2, stage2Top, STAGE_2_BLOCK, stage1, STAGE_1_BLOCK);

    /* we saved some space */
    if(beVerbose) {
        printf("compactStage2() reduced stage2Top from %u to %u\n", stage2Top, newTop);
    }
    stage2Top=newTop;

    if(DO_DEBUG_OUT) {
        /* debug output */
        uint16_t i1, i2, i3, i4;
        uint32_t c;
        for(c=0; c<0xffff; c+=307) {
            printf("properties(0x%06x)=0x%06x\n", c, getProps2(c, &i1, &i2, &i3, &i4));
        }
    }
}

extern void
compactStage3(void) {
    uint16_t newTop=compactStage(stage3, stage3Top, STAGE_3_BLOCK, stage2, stage2Top);

    /* we saved some space */
    if(beVerbose) {
        printf("compactStage3() reduced stage3Top from %u to %u\n", stage3Top, newTop);
    }
    stage3Top=newTop;

    if(DO_DEBUG_OUT) {
        /* debug output */
        uint16_t i1, i2, i3, i4;
        uint32_t c;
        for(c=0; c<0xffff; c+=307) {
            printf("properties(0x%06x)=0x%06x\n", c, getProps2(c, &i1, &i2, &i3, &i4));
        }
    }
}

static uint16_t
compactStage(uint16_t *stage, uint16_t stageTop, uint16_t blockSize,
             uint16_t *parent, uint16_t parentTop) {
    /*
     * This function is the common implementation for compacting
     * a stage table.
     * There are stageTop entries (indexes) in stage[].
     * stageTop is a multiple of blockSize, and there are always blockSize stage[] entries
     * per parent stage entry which do not overlap - yet.
     * The first blockSize stage[] entries are always the empty ones.
     * We make the blocks overlap appropriately here and fill every blockSize-th entry in
     * map[] with the mapping from old to new properties indexes
     * in order to adjust the parent stage tables.
     * This simple algorithm does not find arbitrary overlaps, but only those
     * where the last i entries of the previous block and the first i of the
     * current one all have the same value.
     * This seems reasonable and yields linear performance.
     */
    uint16_t i, start, prevEnd, newStart, x;

    map[0]=0;
    newStart=blockSize;
    for(start=newStart; start<stageTop;) {
        prevEnd=newStart-1;
        x=stage[start];
        if(x==stage[prevEnd]) {
            /* overlap by at least one */
            for(i=1; i<blockSize && x==stage[start+i] && x==stage[prevEnd-i]; ++i) {}

            /* overlap by i */
            map[start]=newStart-i;

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=blockSize-i; i>0; --i) {
                stage[newStart++]=stage[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start]=newStart;
            for(i=blockSize; i>0; --i) {
                stage[newStart++]=stage[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start]=start;
            newStart+=blockSize;
            start=newStart;
        }
    }

    /* now adjust the parent stage table */
    for(i=0; i<parentTop; ++i) {
        parent[i]=map[parent[i]];
    }

    /* we saved some space */
    return stageTop-(start-newStart);
}

extern void
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
    uint16_t i, oldIndex, newIndex;
    uint32_t x;
    if(DO_DEBUG_OUT) {
        /* debug output */
        uint16_t i1, i2, i3;
        uint32_t c;
        for(c=0; c<0xffff; c+=307) {
            printf("properties(0x%06x)=0x%06x\n", c, getProps(c, &i1, &i2, &i3));
        }
    }

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
    if(DO_DEBUG_OUT) {
        /* debug output */
        uint16_t i1, i2, i3, i4;
        uint32_t c;
        for(c=0; c<0xffff; c+=307) {
            printf("properties(0x%06x)=0x%06x\n", c, getProps2(c, &i1, &i2, &i3, &i4));
        }
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
generateData(const char *dataDir) {
    static uint16_t indexes[8]={
        STAGE_2_BITS, STAGE_3_BITS,
        0, 0,
        0, 0, 0, 0
    };

    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t size;
    long dataLength;
    uint16_t i, offset;

    /* fix up the indexes in the stage tables to include the table offsets in the data */
    offset=8+STAGE_1_BLOCK;         /* uint16_t offset to stage2[] */
    for(i=0; i<STAGE_1_BLOCK; ++i) {
        stage1[i]+=offset;
    }

    offset+=stage2Top;              /* uint16_t offset to stage3[] */
    for(i=0; i<stage2Top; ++i) {
        stage2[i]+=offset;
    }

    offset=(offset+stage3Top+1)/2;  /* uint32_t offset to props[], include padding */
    for(i=0; i<stage3Top; ++i) {
        stage3[i]+=offset;
    }

    indexes[2]=offset+=propsTop;            /* uint32_t offset to exceptions[] */

    size=4*(offset+exceptionsTop);          /* total size of data */

    if(beVerbose) {
        printf("number of stage 2 entries:              %5u\n", stage2Top);
        printf("number of stage 3 entries:              %5u\n", stage3Top);
        printf("number of unique properties values:     %5u\n", propsTop);
        printf("number of code points with exceptions:  %5u\n", exceptionsCount);
        printf("size in bytes of exceptions:            %5u\n", 4*exceptionsTop);
        printf("data size:                             %6lu\n", size);
    }

    /* write the data */
    pData=udata_create(dataDir, DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: unable to create data memory, error %d\n", errorCode);
        exit(errorCode);
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, stage1, sizeof(stage1));
    udata_writeBlock(pData, stage2, 2*stage2Top);
    udata_writeBlock(pData, stage3, 2*stage3Top);
    udata_writePadding(pData, (stage2Top+stage3Top)&1);
    udata_writeBlock(pData, props32, 4*propsTop);
    udata_writeBlock(pData, exceptions, 4*exceptionsTop);

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

/* get properties after compacting them */
static uint32_t
getProps2(uint32_t c, uint16_t *pI1, uint16_t *pI2, uint16_t *pI3, uint16_t *pI4) {
    uint16_t i1, i2, i3, i4;

    *pI1=i1=(uint16_t)(c>>STAGE_1_SHIFT);
    *pI2=i2=stage1[i1]+(uint16_t)((c>>STAGE_2_SHIFT)&(STAGE_2_BLOCK-1));
    *pI3=i3=stage2[i2]+(uint16_t)(c&(STAGE_3_BLOCK-1));
    *pI4=i4=stage3[i3];
    return props32[i4];
}

/* get properties before compacting them */
static uint32_t
getProps(uint32_t c, uint16_t *pI1, uint16_t *pI2, uint16_t *pI3) {
    uint16_t i1, i2, i3;

    *pI1=i1=(uint16_t)(c>>STAGE_1_SHIFT);
    *pI2=i2=stage1[i1]+(uint16_t)((c>>STAGE_2_SHIFT)&(STAGE_2_BLOCK-1));
    *pI3=i3=stage2[i2]+(uint16_t)(c&(STAGE_3_BLOCK-1));
    return props[i3];
}

/* set properties before compacting them */
static void
setProps(uint32_t c, uint32_t x, uint16_t *pI1, uint16_t *pI2, uint16_t *pI3) {
    uint16_t i1, i2, i3;

    *pI1=i1=(uint16_t)(c>>STAGE_1_SHIFT);

    i2=stage1[i1];
    if(i2==0) {
        stage1[i1]=i2=allocStage2();
    }
    *pI2=i2+=(uint16_t)((c>>STAGE_2_SHIFT)&(STAGE_2_BLOCK-1));

    i3=stage2[i2];
    if(i3==0) {
        stage2[i2]=i3=allocProps();
    }
    *pI3=i3+=(uint16_t)(c&(STAGE_3_BLOCK-1));

    props[i3]=x;
}

static uint16_t
allocStage2(void) {
    uint16_t i=stage2Top;
    stage2Top+=STAGE_2_BLOCK;
    if(stage2Top>=MAX_STAGE_2_COUNT) {
        fprintf(stderr, "genprops: stage 2 overflow\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    return i;
}

static uint16_t
allocProps(void) {
    uint16_t i=propsTop;
    propsTop+=STAGE_3_BLOCK;
    if(propsTop>=MAX_PROPS_COUNT) {
        fprintf(stderr, "genprops: properties overflow\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    return i;
}

static uint16_t
addUChars(const UChar *s, uint16_t length) {
    uint16_t top=ucharsTop+length+1;
    UChar *p;

    if(top>=MAX_UCHAR_COUNT) {
        fprintf(stderr, "genprops: out of UChars memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=uchars+ucharsTop;
    uprv_memcpy(p, s, length);
    p[length]=0;
    ucharsTop=top;
    return (uint16_t)(p-uchars);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
