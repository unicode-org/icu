// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// layoutpropsbuilder.cpp
// created: 2018aug30 Markus W. Scherer

#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ucptrie.h"
#include "unicode/udata.h"
#include "unicode/umutablecptrie.h"
#include "unicode/uniset.h"
#include "cmemory.h"
#include "genprops.h"
#include "ppucd.h"
#include "uassert.h"
#include "ulayout_props.h"
#include "unewdata.h"

/* Unicode layout properties file format ---------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 1.0 .

The file contains the following structures:

    const int32_t indexes[i0] with values i0, i1, ...:
    (see ULAYOUT_IX_... constants for names of indexes)

    i0 indexesLength; -- length of indexes[] (ULAYOUT_IX_COUNT)
    i1 inpcTop; -- limit byte offset of the InPC trie
    i2 inscTop; -- limit byte offset of the InSC trie
    i3 voTop; -- limit byte offset of the vo trie
    i4..i7 -- reserved, same as the last limit byte offset
    i8 -- reserved, 0

    i9 maxValues; -- max values of the InPC, InSC, vo properties
        (8 bits each; lowest 8 bits reserved, 0)
    i10..i11 -- reserved, 0

    After the indexes array follow consecutive, serialized,
    single-property code point tries for the following properties,
    each built "small" or "fast",
    each padded to a multiple of 16 bytes:
    - InPC
    - InSC
    - vo

----------------------------------------------------------------------------- */

U_NAMESPACE_USE

// UDataInfo cf. udata.h
static UDataInfo dataInfo = {
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    // dataFormat="Layo"
    { ULAYOUT_FMT_0, ULAYOUT_FMT_1, ULAYOUT_FMT_2, ULAYOUT_FMT_3 },
    { 1, 0, 0, 0 },  // formatVersion
    { 12, 0, 0, 0 }  // dataVersion
};

class LayoutPropsBuilder : public PropsBuilder {
public:
    LayoutPropsBuilder(UErrorCode &errorCode);
    virtual ~LayoutPropsBuilder() U_OVERRIDE;

    virtual void setUnicodeVersion(const UVersionInfo version) U_OVERRIDE;
    virtual void setProps(const UniProps &props, const UnicodeSet &newValues, UErrorCode &errorCode) U_OVERRIDE;
    virtual void build(UErrorCode &errorCode) U_OVERRIDE;
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) U_OVERRIDE;

private:
    void setIntProp(const UniProps &, const UnicodeSet &newValues,
                    UProperty prop, UMutableCPTrie *trie,
                    UErrorCode &errorCode);
    int32_t getMaxIntValue(UProperty prop) const {
        return maxIntValues[prop - UCHAR_INT_START];
    }
    void checkMaxIntValue(UProperty prop, int32_t maxMax, UErrorCode &errorCode) const;

    int32_t maxIntValues[UCHAR_INT_LIMIT - UCHAR_INT_START];
    UMutableCPTrie *inpcMutableTrie;
    UMutableCPTrie *inscMutableTrie;
    UMutableCPTrie *voMutableTrie;

    UCPTrie *inpcTrie;
    UCPTrie *inscTrie;
    UCPTrie *voTrie;
};

LayoutPropsBuilder::LayoutPropsBuilder(UErrorCode &errorCode) :
        inpcTrie(nullptr), inscTrie(nullptr), voTrie(nullptr) {
    memset(maxIntValues, 0, sizeof(maxIntValues));
    inpcMutableTrie = umutablecptrie_open(0, 0, &errorCode);
    inscMutableTrie = umutablecptrie_open(0, 0, &errorCode);
    voMutableTrie = umutablecptrie_open(0, 0, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: layoutpropsbuilder umutablecptrie_open() failed - %s\n",
                u_errorName(errorCode));
    }
}

LayoutPropsBuilder::~LayoutPropsBuilder() {
    umutablecptrie_close(inpcMutableTrie);
    umutablecptrie_close(inscMutableTrie);
    umutablecptrie_close(voMutableTrie);
    ucptrie_close(inpcTrie);
    ucptrie_close(inscTrie);
    ucptrie_close(voTrie);
}

void
LayoutPropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

void
LayoutPropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                             UErrorCode &errorCode) {
    setIntProp(props, newValues, UCHAR_INDIC_POSITIONAL_CATEGORY, inpcMutableTrie, errorCode);
    setIntProp(props, newValues, UCHAR_INDIC_SYLLABIC_CATEGORY, inscMutableTrie, errorCode);
    setIntProp(props, newValues, UCHAR_VERTICAL_ORIENTATION, voMutableTrie, errorCode);
}

void LayoutPropsBuilder::setIntProp(const UniProps &props, const UnicodeSet &newValues,
                                    UProperty prop, UMutableCPTrie *trie,
                                    UErrorCode &errorCode) {
    if (U_SUCCESS(errorCode) && newValues.contains(prop)) {
        UChar32 start=props.start;
        UChar32 end=props.end;
        int32_t value = props.getIntProp(prop);
        if (value < 0) {
            fprintf(stderr, "error: unencodable negative value for property 0x%x %04lX..%04lX=%ld\n",
                    (int)prop, (long)start, (long)end, (long)value);
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        if (value > maxIntValues[prop - UCHAR_INT_START]) {
            maxIntValues[prop - UCHAR_INT_START] = value;
        }
        if (start == end) {
            umutablecptrie_set(trie, start, value, &errorCode);
        } else {
            umutablecptrie_setRange(trie, start, end, value, &errorCode);
        }
        if (U_FAILURE(errorCode)) {
            fprintf(stderr, "error: umutablecptrie_set(prop 0x%x trie %04lX..%04lX) failed - %s\n",
                    (int)prop, (long)start, (long)end, u_errorName(errorCode));
        }
    }
}

namespace {

UCPTrie *buildUCPTrie(const char *name, UMutableCPTrie *mutableTrie,
                      UCPTrieType type, UCPTrieValueWidth valueWidth, UErrorCode &errorCode) {
    UCPTrie *trie = umutablecptrie_buildImmutable(mutableTrie, type, valueWidth, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: %s trie buildImmutable() failed: %s\n",
                name, u_errorName(errorCode));
        return trie;
    }
    if (!beQuiet) {
        UErrorCode overflow = U_ZERO_ERROR;
        int32_t length = ucptrie_toBinary(trie, nullptr, 0, &overflow);
        printf("%11s trie size in bytes:        %5u\n", name, (int)length);
    }
    return trie;
}

constexpr int32_t TRIE_BLOCK_CAPACITY = 100000;

uint8_t inpcBytes[TRIE_BLOCK_CAPACITY];
uint8_t inscBytes[TRIE_BLOCK_CAPACITY];
uint8_t voBytes[TRIE_BLOCK_CAPACITY];

int32_t inpcLength = 0;
int32_t inscLength = 0;
int32_t voLength = 0;

int32_t writeTrieBytes(const UCPTrie *trie, uint8_t block[], UErrorCode &errorCode) {
    int32_t length = ucptrie_toBinary(trie, block, TRIE_BLOCK_CAPACITY, &errorCode);
    while ((length & 0xf) != 0) {
        block[length++] = 0xaa;
    }
    return length;
}

}  // namespace

void
LayoutPropsBuilder::build(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    if (!beQuiet) {
        puts("* text layout properties stats *");
    }

    checkMaxIntValue(UCHAR_INDIC_POSITIONAL_CATEGORY, 0xff, errorCode);
    checkMaxIntValue(UCHAR_INDIC_SYLLABIC_CATEGORY, 0xff, errorCode);
    checkMaxIntValue(UCHAR_VERTICAL_ORIENTATION, 0xff, errorCode);
    inpcTrie = buildUCPTrie("inpc", inpcMutableTrie,
                            UCPTRIE_TYPE_SMALL, UCPTRIE_VALUE_BITS_8, errorCode);
    inscTrie = buildUCPTrie("insc", inscMutableTrie,
                            UCPTRIE_TYPE_SMALL, UCPTRIE_VALUE_BITS_8, errorCode);
    voTrie = buildUCPTrie("vo", voMutableTrie,
                          UCPTRIE_TYPE_SMALL, UCPTRIE_VALUE_BITS_8, errorCode);

    inpcLength = writeTrieBytes(inpcTrie, inpcBytes, errorCode);
    inscLength = writeTrieBytes(inscTrie, inscBytes, errorCode);
    voLength = writeTrieBytes(voTrie, voBytes, errorCode);

    if (!beQuiet) {
        int32_t size = ULAYOUT_IX_COUNT * 4 + inpcLength + inscLength + voLength;
        printf("data size:                             %5d\n", (int)size);
    }
}

void LayoutPropsBuilder::checkMaxIntValue(UProperty prop, int32_t maxMax,
                                          UErrorCode &errorCode) const {
    int32_t max = getMaxIntValue(prop);
    if (max > maxMax) {
        fprintf(stderr, "genprops error: 0x%x max value = %d overflow\n", (int)prop, (int)max);
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

// In ICU 63, we had functions writeCSourceFile() and writeJavaSourceFile().
// For Java, each serialized trie was written as a String constant with
// one byte per char and an optimization for byte 0,
// to optimize for Java .class file size.
// (See ICU 63 if we need to resurrect some of that code.)
// Since ICU 64, we write a binary ulayout.icu file for use in both C++ & Java.

void
LayoutPropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData = udata_create(
        path, ULAYOUT_DATA_TYPE, ULAYOUT_DATA_NAME, &dataInfo,
        withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: udata_create(%s, ulayout.icu) failed - %s\n",
                path, u_errorName(errorCode));
        return;
    }

    int32_t indexes[ULAYOUT_IX_COUNT] = { ULAYOUT_IX_COUNT };
    int32_t top = ULAYOUT_IX_COUNT * 4;

    indexes[ULAYOUT_IX_INPC_TRIE_TOP] = (top += inpcLength);
    indexes[ULAYOUT_IX_INSC_TRIE_TOP] = (top += inscLength);
    indexes[ULAYOUT_IX_VO_TRIE_TOP] = (top += voLength);

    // Set reserved trie-top values to the top of the last trie
    // so that they look empty until a later file format version
    // uses one or more of these slots.
    for (int32_t i = ULAYOUT_IX_RESERVED_TOP; i <= ULAYOUT_IX_TRIES_TOP; ++i) {
        indexes[i] = top;
    }

    indexes[ULAYOUT_IX_MAX_VALUES] =
        ((getMaxIntValue(UCHAR_INDIC_POSITIONAL_CATEGORY)) << ULAYOUT_MAX_INPC_SHIFT) |
        ((getMaxIntValue(UCHAR_INDIC_SYLLABIC_CATEGORY)) << ULAYOUT_MAX_INSC_SHIFT) |
        ((getMaxIntValue(UCHAR_VERTICAL_ORIENTATION)) << ULAYOUT_MAX_VO_SHIFT);

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, inpcBytes, inpcLength);
    udata_writeBlock(pData, inscBytes, inscLength);
    udata_writeBlock(pData, voBytes, voLength);

    long dataLength = udata_finish(pData, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: error %s writing the output file\n", u_errorName(errorCode));
        return;
    }

    if (dataLength != (long)top) {
        fprintf(stderr,
                "udata_finish(ulayout.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)top);
        errorCode = U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createLayoutPropsBuilder(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return nullptr; }
    PropsBuilder *pb=new LayoutPropsBuilder(errorCode);
    if(pb==nullptr) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}
