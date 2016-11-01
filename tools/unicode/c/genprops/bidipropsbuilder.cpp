/*
*******************************************************************************
*
*   Copyright (C) 2004-2012, International Business Machines
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

The following is a description of format version 2.0 .

The file contains the following structures:

    const int32_t indexes[i0] with values i0, i1, ...:
    (see UBIDI_IX_... constants for names of indexes)

    i0 indexLength; -- length of indexes[] (UBIDI_IX_TOP)
    i1 dataLength; -- length in bytes of the post-header data (incl. indexes[])
    i2 trieSize; -- size in bytes of the bidi/shaping properties trie
    i3 mirrorLength; -- length in uint32_t of the bidi mirroring array

    i4 jgStart; -- first code point with Joining_Group data
    i5 jgLimit; -- limit code point for Joining_Group data

    i6..i14 reservedIndexes; -- reserved values; 0 for now

    i15 maxValues; -- maximum code values for enumerated properties
                      bits 23..16 contain the max value for Joining_Group,
                      otherwise the bits are used like enum fields in the trie word

    Serialized trie, see utrie2.h;

    const uint32_t mirrors[mirrorLength];

    const uint8_t jgArray[i5-i4]; -- (i5-i4) is always a multiple of 4

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
 9.. 8  reserved (set to 0)
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
    { 2, 0, 0, 0 },                             /* formatVersion */
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

    UnicodeSet relevantProps;
    UTrie2 *pTrie;
    uint8_t jgArray[0x300]; /* at most for U+0600..U+08FF */
    UChar32 jgStart;  // First code point with a Joining_Group.
    UChar32 jgLimit;  // One past the last one.
    uint32_t mirrors[UBIDI_MAX_MIRROR_INDEX+1][2];
    int32_t mirrorTop;
};

BiDiPropsBuilder::BiDiPropsBuilder(UErrorCode &errorCode)
        : pTrie(NULL),
          jgStart(0), jgLimit(0),
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
    value|=(uint32_t)props.getIntProp(UCHAR_JOINING_TYPE)<<UBIDI_JT_SHIFT;
    value|=(uint32_t)props.getIntProp(UCHAR_BIDI_CLASS);
    utrie2_setRange32(pTrie, start, end, value, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: BiDiPropsBuilder utrie2_setRange32() failed - %s\n",
                u_errorName(errorCode));
        return;
    }

    /* store Joining_Group values from vector column 1 in a simple byte array */
    int32_t jg=props.getIntProp(UCHAR_JOINING_GROUP);
    if(jg!=U_JG_NO_JOINING_GROUP) {
        if(start<0x600 || 0x8ff<end) {
            fprintf(stderr, "genprops error: Joining_Group for out-of-range code points U+%04lx..U+%04lx\n",
                    (long)start, (long)end);
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        if(start<jgStart || jgStart==0) { jgStart=start; }
        if(end>=jgLimit || jgLimit==0) { jgLimit=end+1; }
    }
    // On the off-chance that a block is defined with a Joining_Group
    // that is then overridden by No_Joining_Group,
    // we set that too, but only inside U+0600..U+08FF.
    if(end>=0x600 && start<=0x8ff) {
        if(start<0x600) { start=0x600; }
        if(end>0x8ff) { end=0x8ff; }

        /* set Joining_Group value for start..end */
        for(UChar32 c=start; c<=end; ++c) {
            jgArray[c-0x600]=(uint8_t)jg;
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
                   compareMirror, NULL, FALSE, &errorCode);
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

    /* finish jgArray, pad to multiple of 4 */
    while((jgLimit-jgStart)&3) {
        // Prefer rounding down jgStart before rounding up jgLimit
        // so that we are guaranteed not to increase jgLimit beyond 0x900
        // which (after the offset) is the end of the jgArray.
        if(jgStart&3) { --jgStart; } else { ++jgLimit; }
    }
    indexes[UBIDI_IX_JG_START]=jgStart;
    indexes[UBIDI_IX_JG_LIMIT]=jgLimit;

    indexes[UBIDI_IX_TRIE_SIZE]=trieSize;
    indexes[UBIDI_IX_MIRROR_LENGTH]=mirrorTop;
    indexes[UBIDI_IX_LENGTH]=
        (int32_t)sizeof(indexes)+
        trieSize+
        4*mirrorTop+
        (jgLimit-jgStart);

    if(!beQuiet) {
        puts("* ubidi.icu stats *");
        printf("trie size in bytes:                    %5d\n", (int)trieSize);
        printf("size in bytes of mirroring table:      %5d\n", (int)(4*mirrorTop));
        printf("length of Joining_Group array:         %5d (U+%04x..U+%04x)\n", (int)(jgLimit-jgStart), (int)jgStart, (int)(jgLimit-1));
        printf("data size:                             %5d\n", (int)indexes[UBIDI_IX_LENGTH]);
    }

    indexes[UBIDI_MAX_VALUES_INDEX]=
        ((int32_t)U_CHAR_DIRECTION_COUNT-1)|
        (((int32_t)U_JT_COUNT-1)<<UBIDI_JT_SHIFT)|
        (((int32_t)U_JG_COUNT-1)<<UBIDI_MAX_JG_SHIFT);
}

void
BiDiPropsBuilder::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    FILE *f=usrc_create(path, "ubidi_props_data.h",
                        "icu/tools/unicode/c/genprops/bidipropsbuilder.cpp");
    if(f==NULL) {
        errorCode=U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifndef INCLUDED_FROM_UBIDI_PROPS_C\n"
          "#   error This file must be #included from ubidi_props.c only.\n"
          "#endif\n\n", f);
    usrc_writeArray(f,
        "static const UVersionInfo ubidi_props_dataVersion={",
        dataInfo.dataVersion, 8, 4,
        "};\n\n");
    usrc_writeArray(f,
        "static const int32_t ubidi_props_indexes[UBIDI_IX_TOP]={",
        indexes, 32, UBIDI_IX_TOP,
        "};\n\n");
    usrc_writeUTrie2Arrays(f,
        "static const uint16_t ubidi_props_trieIndex[%ld]={\n", NULL,
        pTrie,
        "\n};\n\n");
    usrc_writeArray(f,
        "static const uint32_t ubidi_props_mirrors[%ld]={\n",
        mirrors, 32, mirrorTop,
        "\n};\n\n");
    usrc_writeArray(f,
        "static const uint8_t ubidi_props_jgArray[%ld]={\n",
        jgArray+(jgStart-0x600), 8, jgLimit-jgStart,
        "\n};\n\n");
    fputs(
        "static const UBiDiProps ubidi_props_singleton={\n"
        "  NULL,\n"
        "  ubidi_props_indexes,\n"
        "  ubidi_props_mirrors,\n"
        "  ubidi_props_jgArray,\n",
        f);
    usrc_writeUTrie2Struct(f,
        "  {\n",
        pTrie, "ubidi_props_trieIndex", NULL,
        "  },\n");
    usrc_writeArray(f, "  { ", dataInfo.formatVersion, 8, 4, " }\n");
    fputs("};\n", f);
    fclose(f);
}

void
BiDiPropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData=udata_create(path, UBIDI_DATA_TYPE, UBIDI_DATA_NAME, &dataInfo,
                                       withCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, ubidi.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, mirrors, 4*mirrorTop);
    udata_writeBlock(pData, jgArray+(jgStart-0x600), jgLimit-jgStart);

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
    if(U_FAILURE(errorCode)) { return NULL; }
    PropsBuilder *pb=new BiDiPropsBuilder(errorCode);
    if(pb==NULL) {
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
