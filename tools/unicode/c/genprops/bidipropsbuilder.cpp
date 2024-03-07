// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2004-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  bidipropsbuilder.cpp (was genbidi/store.c)
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004dec30
*   created by: Markus W. Scherer
*
*   Store Unicode bidi/shaping properties efficiently for
*   random access.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "cmemory.h"
#include "cstring.h"
#include "ppucd.h"
#include "uarrsort.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "utrie2.h"
#include "writesrc.h"
#include "ubidi_props.h"
#include "genprops.h"

/* Unicode bidi/shaping properties file format ---------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 2.2 .

The file contains the following structures:

    const int32_t indexes[i0] with values i0, i1, ...:
    (see UBIDI_IX_... constants for names of indexes)

    i0 indexLength; -- length of indexes[] (UBIDI_IX_TOP)
    i1 dataLength; -- length in bytes of the post-header data (incl. indexes[])
    i2 trieSize; -- size in bytes of the bidi/shaping properties trie
    i3 mirrorLength; -- length in uint32_t of the bidi mirroring array

    i4 jgStart; -- first code point with Joining_Group data
    i5 jgLimit; -- limit code point for Joining_Group data

    -- i6, i7 new in format version 2.2:
    i6 jgStart2; -- first code point with Joining_Group data, second range
    i7 jgLimit2; -- limit code point for Joining_Group data, second range

    i8..i14 reservedIndexes; -- reserved values; 0 for now

    i15 maxValues; -- maximum code values for enumerated properties
                      bits 23..16 contain the max value for Joining_Group,
                      otherwise the bits are used like enum fields in the trie word

    Serialized trie, see utrie2.h;

    const uint32_t mirrors[mirrorLength];

    const uint8_t jgArray[i5-i4]; -- (i5-i4)+(i7-i6) is always a multiple of 4
    const uint8_t jgArray2[i7-i6]; -- new in format version 2.2

Trie data word:
Bits
15..13  signed delta to bidi mirroring code point
        (add delta to input code point)
        0 no such code point (source maps to itself)
        -3..-1, 1..3 delta
        -4 look in mirrors table
    12  is mirrored
    11  Bidi_Control
    10  Join_Control
 9.. 8  Bidi_Paired_Bracket_Type(bpt) -- new in format version 2.1
 7.. 5  Joining_Type
 4.. 0  BiDi category


Mirrors:
Stores some of the bidi mirroring data, where each code point maps to
at most one other.
Most code points do not have a mirroring code point; most that do have a signed
delta stored in the trie data value. Only those where the delta does not fit
into the trie data are stored in this table.

Logically, this is a two-column table with source and mirror code points.

Physically, the table is compressed by taking advantage of the fact that each
mirror code point is also a source code point
(each of them is a mirror of the other).
Therefore, both logical columns contain the same set of code points, which needs
to be stored only once.

The table stores source code points, and also for each the index of its mirror
code point in the same table, in a simple array of uint32_t.
Bits
31..21  index to mirror code point (unsigned)
20.. 0  source code point

The table is sorted by source code points.


Joining_Group array:
The Joining_Group values do not fit into the 16-bit trie, but the data is also
limited to a small range of code points (Arabic and Syriac) and not
well compressible.

The start and limit code points for the range are stored in the indexes[]
array, and the jgArray[] stores a byte for each of these code points,
containing the Joining_Group value.

All code points outside of this range have No_Joining_Group (0).

ICU 54 adds jgArray2[] for a second range.

--- Changes in format version 2.2 ---

Addition of second range for Joining_Group values (i6, i7),
for 10800..10FFF, including Unicode 7.0 10AC0..10AFF Manichaean.

--- Changes in format version 2.1 ---

Addition of Bidi_Paired_Bracket_Type(bpt) values.
(Trie data bits 9..8 were reserved.)

--- Changes in format version 2 ---

Change from UTrie to UTrie2.

----------------------------------------------------------------------------- */

U_NAMESPACE_USE

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    /* dataFormat="BiDi" */
    { UBIDI_FMT_0, UBIDI_FMT_1, UBIDI_FMT_2, UBIDI_FMT_3 },
    { 2, 2, 0, 0 },                             /* formatVersion */
    { 6, 0, 0, 0 }                              /* dataVersion */
};

/* -------------------------------------------------------------------------- */

class BiDiPropsBuilder : public PropsBuilder {
public:
    BiDiPropsBuilder(UErrorCode &errorCode);
    virtual ~BiDiPropsBuilder();

    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void build(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);

private:
    int32_t encodeBidiMirroringGlyph(UChar32 src, UChar32 end, UChar32 mirror, UErrorCode &errorCode);
    void makeMirror(UErrorCode &errorCode);

    static const UChar32 MIN_JG_START=0x600;
    static const UChar32 MAX_JG_LIMIT=0x8ff+1;
    static const UChar32 MIN_JG_START2=0x10800;
    static const UChar32 MAX_JG_LIMIT2=0x10fff+1;

    UnicodeSet relevantProps;
    UTrie2 *pTrie;
    uint8_t jgArray[MAX_JG_LIMIT-MIN_JG_START];
    uint8_t jgArray2[MAX_JG_LIMIT2-MIN_JG_START2];
    uint32_t mirrors[UBIDI_MAX_MIRROR_INDEX+1][2];
    int32_t mirrorTop;
};

BiDiPropsBuilder::BiDiPropsBuilder(UErrorCode &errorCode)
        : pTrie(nullptr),
          mirrorTop(0) {
    // This builder encodes the following properties.
    relevantProps.
        add(UCHAR_BIDI_CONTROL).
        add(UCHAR_BIDI_MIRRORED).
        add(UCHAR_BIDI_CLASS).
        add(UCHAR_BIDI_MIRRORING_GLYPH).
        add(UCHAR_JOIN_CONTROL).
        add(UCHAR_JOINING_GROUP).
        add(UCHAR_JOINING_TYPE);
    pTrie=utrie2_open(0, 0, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: bidipropsbuilder utrie2_open() failed - %s\n",
                u_errorName(errorCode));
    }
    uprv_memset(jgArray, U_JG_NO_JOINING_GROUP, sizeof(jgArray));
    uprv_memset(jgArray2, U_JG_NO_JOINING_GROUP, sizeof(jgArray2));
}

BiDiPropsBuilder::~BiDiPropsBuilder() {
    utrie2_close(pTrie);
}

void
BiDiPropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

/* bidi mirroring table ----------------------------------------------------- */

int32_t
BiDiPropsBuilder::encodeBidiMirroringGlyph(UChar32 src, UChar32 end, UChar32 mirror,
                                           UErrorCode &errorCode) {
    if(U_FAILURE(errorCode) || mirror<0) {
        return 0;
    }
    if(src!=end) {
        fprintf(stderr,
                "genprops error: range U+%04lX..U+%04lX all with the same "
                "Bidi_Mirroring_Glyph U+%04lX\n",
                (long)src, (long)end, (long)mirror);
        errorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    int32_t delta=mirror-src;
    if(delta==0) {
        return 0; /* mapping to self=no mapping */
    }

    if(delta<UBIDI_MIN_MIRROR_DELTA || UBIDI_MAX_MIRROR_DELTA<delta) {
        /* delta does not fit into the trie properties value, store in the mirrors[] table */
        if(mirrorTop==LENGTHOF(mirrors)) {
            fprintf(stderr,
                    "genprops error: too many long-distance Bidi_Mirroring_Glyph mappings; "
                    "UBIDI_MAX_MIRROR_INDEX can only be increased by "
                    "redesigning the ubidi.icu data bit fields\n");
            errorCode=U_BUFFER_OVERFLOW_ERROR;
            return 0;
        }

        /* possible: search the table so far and see if src is already listed */

        mirrors[mirrorTop][0]=(uint32_t)src;
        mirrors[mirrorTop][1]=(uint32_t)mirror;
        ++mirrorTop;

        /* set an escape marker in src's properties */
        delta=UBIDI_ESC_MIRROR_DELTA;
    }
    return delta;
}

void
BiDiPropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                           UErrorCode &errorCode) {
    if(U_FAILURE(errorCode) || newValues.containsNone(relevantProps)) { return; }

    UChar32 start=props.start;
    UChar32 end=props.end;

    // The runtime code relies on this invariant for returning both bmg and bpb
    // from the same data.
    int32_t bpt=props.getIntProp(UCHAR_BIDI_PAIRED_BRACKET_TYPE);
    if(!(bpt==0 ? props.bpb==U_SENTINEL : props.bpb==props.bmg)) {
        fprintf(stderr,
                "genprops error: invariant not true: "
                "if(bpt==None) then bpb=<none> else bpb=bmg\n");
        return;
    }
    int32_t delta=encodeBidiMirroringGlyph(start, end, props.bmg, errorCode);
    uint32_t value=(uint32_t)delta<<UBIDI_MIRROR_DELTA_SHIFT;
    if(props.binProps[UCHAR_BIDI_MIRRORED]) {
        value|=U_MASK(UBIDI_IS_MIRRORED_SHIFT);
    }
    if(props.binProps[UCHAR_BIDI_CONTROL]) {
        value|=U_MASK(UBIDI_BIDI_CONTROL_SHIFT);
    }
    if(props.binProps[UCHAR_JOIN_CONTROL]) {
        value|=U_MASK(UBIDI_JOIN_CONTROL_SHIFT);
    }
    value|=(uint32_t)bpt<<UBIDI_BPT_SHIFT;
    value|=(uint32_t)props.getIntProp(UCHAR_JOINING_TYPE)<<UBIDI_JT_SHIFT;
    value|=(uint32_t)props.getIntProp(UCHAR_BIDI_CLASS);
    utrie2_setRange32(pTrie, start, end, value, true, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: BiDiPropsBuilder utrie2_setRange32() failed - %s\n",
                u_errorName(errorCode));
        return;
    }

    // Store Joining_Group values from vector column 1 in simple byte arrays.
    int32_t jg=props.getIntProp(UCHAR_JOINING_GROUP);
    for(UChar32 c=start; c<=end; ++c) {
        if(MIN_JG_START<=c && c<MAX_JG_LIMIT) {
            jgArray[c-MIN_JG_START]=(uint8_t)jg;
        } else if(MIN_JG_START2<=c && c<MAX_JG_LIMIT2) {
            jgArray2[c-MIN_JG_START2]=(uint8_t)jg;
        } else if(jg!=U_JG_NO_JOINING_GROUP) {
            fprintf(stderr, "genprops error: Joining_Group for out-of-range code points U+%04lx..U+%04lx\n",
                    (long)start, (long)end);
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    }
}

/* generate output data ----------------------------------------------------- */

static int32_t U_CALLCONV
compareMirror(const void *context, const void *left, const void *right) {
    UChar32 l, r;

    l=UBIDI_GET_MIRROR_CODE_POINT(((const uint32_t *)left)[0]);
    r=UBIDI_GET_MIRROR_CODE_POINT(((const uint32_t *)right)[0]);
    return l-r;
}

void
BiDiPropsBuilder::makeMirror(UErrorCode &errorCode) {
    /* sort the mirroring table by source code points */
    uprv_sortArray(mirrors, mirrorTop, 8,
                   compareMirror, nullptr, false, &errorCode);
    if(U_FAILURE(errorCode)) { return; }

    /*
     * reduce the 2-column table to a single column
     * by putting the index to the mirror entry into the source entry
     *
     * first:
     * find each mirror code point in the source column and set each other's indexes
     *
     * second:
     * reduce the table, combine the source code points with their indexes
     * and store as a simple array of uint32_t
     */
    for(int32_t i=0; i<mirrorTop; ++i) {
        uint32_t c=mirrors[i][1]; /* mirror code point */
        if(c>0x1fffff) {
            continue; /* this entry already has an index */
        }

        /* search for the mirror code point in the source column */
        int32_t start, limit, step;
        if(c<mirrors[i][0]) {
            /* search before i */
            start=i-1;
            limit=-1;
            step=-1;
        } else {
            start=i+1;
            limit=mirrorTop;
            step=1;
        }

        for(int32_t j=start;; j+=step) {
            if(j==limit) {
                fprintf(stderr,
                        "genprops error: bidi mirror does not roundtrip - %04lx->%04lx->?\n",
                        (long)mirrors[i][0], (long)mirrors[i][1]);
                errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            }
            if(c==mirrors[j][0]) {
                /*
                 * found the mirror code point c in the source column,
                 * set both entries' indexes to each other
                 */
                if(UBIDI_GET_MIRROR_CODE_POINT(mirrors[i][0])!=UBIDI_GET_MIRROR_CODE_POINT(mirrors[j][1])) {
                    /* roundtrip check fails */
                    fprintf(stderr,
                            "genprops error: bidi mirrors do not roundtrip - %04lx->%04lx->%04lx\n",
                            (long)mirrors[i][0], (long)mirrors[i][1], (long)mirrors[j][1]);
                    errorCode=U_ILLEGAL_ARGUMENT_ERROR;
                } else {
                    mirrors[i][1]|=(uint32_t)j<<UBIDI_MIRROR_INDEX_SHIFT;
                    mirrors[j][1]|=(uint32_t)i<<UBIDI_MIRROR_INDEX_SHIFT;
                }
                break;
            }
        }
    }

    /* now the second step, the actual reduction of the table */
    uint32_t *reducedMirror=mirrors[0];
    for(int32_t i=0; i<mirrorTop; ++i) {
        reducedMirror[i]=mirrors[i][0]|(mirrors[i][1]&~0x1fffff);
    }
}

static int32_t indexes[UBIDI_IX_TOP]={
    UBIDI_IX_TOP, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0
};

static uint8_t trieBlock[40000];
static int32_t trieSize;

void
BiDiPropsBuilder::build(UErrorCode &errorCode) {
    makeMirror(errorCode);
    if(U_FAILURE(errorCode)) { return; }

    utrie2_freeze(pTrie, UTRIE2_16_VALUE_BITS, &errorCode);
    trieSize=utrie2_serialize(pTrie, trieBlock, sizeof(trieBlock), &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: utrie2_freeze()+utrie2_serialize() failed: %s (length %ld)\n",
                u_errorName(errorCode), (long)trieSize);
        return;
    }

    // Finish jgArray & jgArray2.
    UChar32 jgStart;  // First code point with a Joining_Group, first range.
    UChar32 jgLimit;  // One past the last one.
    // Find the end of the range first, so that if it's empty we
    // get jgStart=jgLimit=MIN_JG_START.
    for(jgLimit=MAX_JG_LIMIT;
        MIN_JG_START<jgLimit && jgArray[jgLimit-MIN_JG_START-1]==U_JG_NO_JOINING_GROUP;
        --jgLimit) {}
    for(jgStart=MIN_JG_START;
        jgStart<jgLimit && jgArray[jgStart-MIN_JG_START]==U_JG_NO_JOINING_GROUP;
        ++jgStart) {}

    UChar32 jgStart2;  // First code point with a Joining_Group, second range.
    UChar32 jgLimit2;  // One past the last one.
    for(jgLimit2=MAX_JG_LIMIT2;
        MIN_JG_START2<jgLimit2 && jgArray2[jgLimit2-MIN_JG_START2-1]==U_JG_NO_JOINING_GROUP;
        --jgLimit2) {}
    for(jgStart2=MIN_JG_START2;
        jgStart2<jgLimit2 && jgArray2[jgStart2-MIN_JG_START2]==U_JG_NO_JOINING_GROUP;
        ++jgStart2) {}

    // Pad the total Joining_Group arrays length to a multiple of 4.
    // Prefer rounding down starts before rounding up limits
    // so that we are guaranteed not to increase the limits beyond
    // the end of the arrays' code point ranges.
    int32_t jgLength=jgLimit-jgStart+jgLimit2-jgStart2;
    while(jgLength&3) {
        if((jgStart<jgLimit) && (jgStart&3)) {
            --jgStart;
        } else if((jgStart2<jgLimit2) && (jgStart2&3)) {
            --jgStart2;
        } else if(jgStart<jgLimit) {
            ++jgLimit;
        } else {
            ++jgLimit2;
        }
        ++jgLength;
    }
    indexes[UBIDI_IX_JG_START]=jgStart;
    indexes[UBIDI_IX_JG_LIMIT]=jgLimit;
    indexes[UBIDI_IX_JG_START2]=jgStart2;
    indexes[UBIDI_IX_JG_LIMIT2]=jgLimit2;

    indexes[UBIDI_IX_TRIE_SIZE]=trieSize;
    indexes[UBIDI_IX_MIRROR_LENGTH]=mirrorTop;
    indexes[UBIDI_IX_LENGTH]=
        (int32_t)sizeof(indexes)+
        trieSize+
        4*mirrorTop+
        jgLength;

    if(!beQuiet) {
        puts("* ubidi.icu stats *");
        printf("trie size in bytes:                    %5d\n", (int)trieSize);
        printf("size in bytes of mirroring table:      %5d\n", (int)(4*mirrorTop));
        printf("length of Joining_Group array:         %5d (U+%04x..U+%04x)\n",
               (int)(jgLimit-jgStart), (int)jgStart, (int)(jgLimit-1));
        printf("length of Joining_Group array 2:       %5d (U+%04x..U+%04x)\n",
               (int)(jgLimit2-jgStart2), (int)jgStart2, (int)(jgLimit2-1));
        printf("data size:                             %5d\n", (int)indexes[UBIDI_IX_LENGTH]);
    }

    indexes[UBIDI_MAX_VALUES_INDEX]=
        ((int32_t)U_CHAR_DIRECTION_COUNT-1)|
        (((int32_t)U_JT_COUNT-1)<<UBIDI_JT_SHIFT)|
        (((int32_t)U_BPT_COUNT-1)<<UBIDI_BPT_SHIFT)|
        (((int32_t)U_JG_COUNT-1)<<UBIDI_MAX_JG_SHIFT);
}

void
BiDiPropsBuilder::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    FILE *f=usrc_create(path, "ubidi_props_data.h", 2016,
                        "icu/tools/unicode/c/genprops/bidipropsbuilder.cpp");
    if(f==nullptr) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifdef INCLUDED_FROM_UBIDI_PROPS_C\n\n", f);
    usrc_writeArray(f,
        "static const UVersionInfo ubidi_props_dataVersion={",
        dataInfo.dataVersion, 8, 4,
        "",
        "};\n\n");
    usrc_writeArray(f,
        "static const int32_t ubidi_props_indexes[UBIDI_IX_TOP]={",
        indexes, 32, UBIDI_IX_TOP,
        "",
        "};\n\n");
    usrc_writeUTrie2Arrays(f,
        "static const uint16_t ubidi_props_trieIndex[%ld]={\n", nullptr,
        pTrie,
        "\n};\n\n");
    usrc_writeArray(f,
        "static const uint32_t ubidi_props_mirrors[%ld]={\n",
        mirrors, 32, mirrorTop,
        "",
        "\n};\n\n");
    UChar32 jgStart=indexes[UBIDI_IX_JG_START];
    UChar32 jgLimit=indexes[UBIDI_IX_JG_LIMIT];
    usrc_writeArray(f,
        "static const uint8_t ubidi_props_jgArray[%ld]={\n",
        jgArray+(jgStart-MIN_JG_START), 8, jgLimit-jgStart,
        "",
        "\n};\n\n");
    UChar32 jgStart2=indexes[UBIDI_IX_JG_START2];
    UChar32 jgLimit2=indexes[UBIDI_IX_JG_LIMIT2];
    usrc_writeArray(f,
        "static const uint8_t ubidi_props_jgArray2[%ld]={\n",
        jgArray2+(jgStart2-MIN_JG_START2), 8, jgLimit2-jgStart2,
        "",
        "\n};\n\n");
    fputs(
        "static const UBiDiProps ubidi_props_singleton={\n"
        "  nullptr,\n"
        "  ubidi_props_indexes,\n"
        "  ubidi_props_mirrors,\n"
        "  ubidi_props_jgArray,\n"
        "  ubidi_props_jgArray2,\n",
        f);
    usrc_writeUTrie2Struct(f,
        "  {\n",
        pTrie, "ubidi_props_trieIndex", nullptr,
        "  },\n");
    usrc_writeArray(f, "  { ", dataInfo.formatVersion, 8, 4, "", " }\n");
    fputs("};\n\n"
          "#endif  // INCLUDED_FROM_UBIDI_PROPS_C\n", f);
    fclose(f);
}

void
BiDiPropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData=udata_create(path, UBIDI_DATA_TYPE, UBIDI_DATA_NAME, &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, ubidi.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, mirrors, 4*mirrorTop);
    UChar32 jgStart=indexes[UBIDI_IX_JG_START];
    UChar32 jgLimit=indexes[UBIDI_IX_JG_LIMIT];
    udata_writeBlock(pData, jgArray+(jgStart-MIN_JG_START), jgLimit-jgStart);
    UChar32 jgStart2=indexes[UBIDI_IX_JG_START2];
    UChar32 jgLimit2=indexes[UBIDI_IX_JG_LIMIT2];
    udata_writeBlock(pData, jgArray2+(jgStart2-MIN_JG_START2), jgLimit2-jgStart2);

    long dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: bidipropsbuilder %d writing the output file\n", errorCode);
        return;
    }

    if(dataLength!=indexes[UBIDI_IX_LENGTH]) {
        fprintf(stderr,
                "udata_finish(ubidi.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)indexes[UBIDI_IX_LENGTH]);
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createBiDiPropsBuilder(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return nullptr; }
    PropsBuilder *pb=new BiDiPropsBuilder(errorCode);
    if(pb==nullptr) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
