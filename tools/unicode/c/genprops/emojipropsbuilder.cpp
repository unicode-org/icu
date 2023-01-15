// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// emojipropsbuilder.cpp
// created: 2021sep03 Markus W. Scherer

#include <stdio.h>
#include <string.h>
#include <set>
#include <string>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ucharstriebuilder.h"
#include "unicode/ucptrie.h"
#include "unicode/udata.h"
#include "unicode/umutablecptrie.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "charstr.h"
#include "cmemory.h"
#include "emojiprops.h"
#include "genprops.h"
#include "uassert.h"
#include "unewdata.h"
#include "uparse.h"

/* Emoji properties file format ------------------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 1.0 .

The file contains the following structures:

    const int32_t indexes[] with values i0, i1, ...:
    (see EmojiProps::IX_... constants for names of indexes)

    The length of the indexes[] array is indexes[IX_CPTRIE_OFFSET]/4;

    The first 14 indexes are byte offsets in ascending order.
    Each byte offset marks the start of a part in the data file,
    and the limit (exclusive end) of the previous one.
    When two consecutive byte offsets are the same, then the corresponding part is empty.
    Byte offsets are offsets from after the header, that is, from the beginning of the indexes[].
    Each part starts at an offset with proper alignment for its data.
    If necessary, the previous part may include padding bytes to achieve this alignment.

    i0        offset of cpTrie (and the limit offset of the indexes[] array)
    i1..i3    reserved, same as the limit offset of the previous part
    i4        offset of Basic_Emoji string trie
    i5        offset of Emoji_Keycap_Sequence string trie
    i6        offset of RGI_Emoji_Modifier_Sequence string trie
    i7        offset of RGI_Emoji_Flag_Sequence string trie
    i8        offset of RGI_Emoji_Tag_Sequence string trie
    i9        offset of RGI_Emoji_ZWJ_Sequence string trie
    i10..i12  reserved, same as the limit offset of the previous part
    i13       totalSize -- same as the limit offset of the previous part
    i14..i15  reserved, 0

    After the indexes array follows a UCPTrie=CodePointTrie (type=fast, valueWidth=8)
    "cpTrie" with one bit each for multiple binary properties;
    see EmojiProps::BIT_... constants.

    After that follow consecutive, serialized,
    single-property UCharsTrie=CharsTrie string tries for multiple properties of strings;
    see EmojiProps::IX_.._TRIE_OFFSET constants.

    The Basic_Emoji property contains both single code points and multi-character strings.
    Its data is in both the code point trie and in one of the string tries.

----------------------------------------------------------------------------- */

U_NAMESPACE_USE

// UDataInfo cf. udata.h
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { u'E', u'm', u'o', u'j' },                 // dataFormat="Emoj"
    { 1, 0, 0, 0 },                             // formatVersion
    { 14, 0, 0, 0 }                             // dataVersion
};

class EmojiPropsBuilder : public PropsBuilder {
public:
    EmojiPropsBuilder(UErrorCode &errorCode);
    ~EmojiPropsBuilder() override;

    void setUnicodeVersion(const UVersionInfo version) override;
    void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode) override;
    void parseUnidataFiles(const char *unidataPath, UErrorCode &errorCode) override;
    void build(UErrorCode &errorCode) override;
    void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) override;

    // visible for C adapter
    void parsePropsOfStringsLine(char *fields[][2], UErrorCode &errorCode);

private:
    void setBit(UChar32 start, UChar32 end, int32_t shift, bool on, UErrorCode &errorCode);
    void setBits(UChar32 start, UChar32 end, uint32_t value, uint32_t mask, UErrorCode &errorCode);
    void parsePropsOfStringsFile(const char *path, UErrorCode &errorCode);

    static int32_t getTrieIndex(int32_t index) {
        U_ASSERT(TRIE_IX_START <= index);
        U_ASSERT(index < TRIE_IX_LIMIT);
        return index - TRIE_IX_START;
    }
    UCharsTrieBuilder &getTrieBuilder(int32_t index) {
        index = getTrieIndex(index);
        U_ASSERT(trieBuilders[index] != nullptr);
        return *trieBuilders[index];
    }
    UnicodeString &getTrieString(int32_t index) {
        index = getTrieIndex(index);
        return trieStrings[index];
    }
    int32_t &getNumStrings(int32_t index) {
        index = getTrieIndex(index);
        return numStrings[index];
    }

    static constexpr int32_t TRIE_IX_START = EmojiProps::IX_BASIC_EMOJI_TRIE_OFFSET;
    static constexpr int32_t TRIE_IX_LIMIT = EmojiProps::IX_RESERVED10;

    UMutableCPTrie *mutableCPTrie = nullptr;
    UCPTrie *cpTrie = nullptr;
    std::set<std::string> unrecognized;
    UCharsTrieBuilder *trieBuilders[TRIE_IX_LIMIT - TRIE_IX_START] = {
        nullptr, nullptr, nullptr, nullptr, nullptr, nullptr
    };
    UnicodeString trieStrings[TRIE_IX_LIMIT - TRIE_IX_START];
    int32_t numStrings[TRIE_IX_LIMIT - TRIE_IX_START];
    int32_t indexes[EmojiProps::IX_COUNT] = {
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0
    };
    uint8_t trieBlock[100000];
    int32_t trieSize = 0;
};

EmojiPropsBuilder::EmojiPropsBuilder(UErrorCode &errorCode) {
    mutableCPTrie = umutablecptrie_open(0, 0, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/emoji error: umutablecptrie_open() failed: %s\n",
                u_errorName(errorCode));
    }
    bool isNull = false;
    for (auto &ptr : trieBuilders) {
        ptr = new UCharsTrieBuilder(errorCode);
        if (ptr == nullptr) {
            isNull = true;
        }
    }
    if (isNull && U_SUCCESS(errorCode)) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/emoji error: new UCharsTrieBuilder() failed: %s\n",
                u_errorName(errorCode));
    }
    for (auto &num : numStrings) {
        num = 0;
    }
}

EmojiPropsBuilder::~EmojiPropsBuilder() {
    umutablecptrie_close(mutableCPTrie);
    ucptrie_close(cpTrie);
    for (auto ptr : trieBuilders) {
        delete ptr;
    }
}

void
EmojiPropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

namespace {

struct PropToBinary {
    UProperty prop;
    int32_t shift;
};

constexpr PropToBinary propToBinaries[] = {
    { UCHAR_EMOJI,                      EmojiProps::BIT_EMOJI },
    { UCHAR_EMOJI_PRESENTATION,         EmojiProps::BIT_EMOJI_PRESENTATION },
    { UCHAR_EMOJI_MODIFIER,             EmojiProps::BIT_EMOJI_MODIFIER },
    { UCHAR_EMOJI_MODIFIER_BASE,        EmojiProps::BIT_EMOJI_MODIFIER_BASE },
    { UCHAR_EMOJI_COMPONENT,            EmojiProps::BIT_EMOJI_COMPONENT },
    { UCHAR_EXTENDED_PICTOGRAPHIC,      EmojiProps::BIT_EXTENDED_PICTOGRAPHIC },
};

struct PropNameToIndex {
    const char *propName;
    int32_t emojiPropsIndex;
};

constexpr PropNameToIndex propNameToIndex[] = {
    { "Basic_Emoji",                    EmojiProps::IX_BASIC_EMOJI_TRIE_OFFSET },
    { "Emoji_Keycap_Sequence",          EmojiProps::IX_EMOJI_KEYCAP_SEQUENCE_TRIE_OFFSET },
    { "RGI_Emoji_Modifier_Sequence",    EmojiProps::IX_RGI_EMOJI_MODIFIER_SEQUENCE_TRIE_OFFSET },
    { "RGI_Emoji_Flag_Sequence",        EmojiProps::IX_RGI_EMOJI_FLAG_SEQUENCE_TRIE_OFFSET },
    { "RGI_Emoji_Tag_Sequence",         EmojiProps::IX_RGI_EMOJI_TAG_SEQUENCE_TRIE_OFFSET },
    { "RGI_Emoji_ZWJ_Sequence",         EmojiProps::IX_RGI_EMOJI_ZWJ_SEQUENCE_TRIE_OFFSET },
};

}  // namespace

void
EmojiPropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                            UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    if (newValues.containsSome(0, UCHAR_BINARY_LIMIT-1)) {
        for (const auto &p2b : propToBinaries) {
            U_ASSERT(p2b.shift < 8);
            if (newValues.contains(p2b.prop)) {
                setBit(props.start, props.end, p2b.shift, props.binProps[p2b.prop], errorCode);
            }
        }
    }
}

void
EmojiPropsBuilder::setBit(UChar32 start, UChar32 end, int32_t shift, bool on,
                          UErrorCode &errorCode) {
    uint32_t mask = U_MASK(shift);
    uint32_t value = on ? mask : 0;
    setBits(start, end, value, mask, errorCode);
}

void
EmojiPropsBuilder::setBits(UChar32 start, UChar32 end, uint32_t value, uint32_t mask,
                           UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    if (start == end) {
        uint32_t oldValue = umutablecptrie_get(mutableCPTrie, start);
        uint32_t newValue = (oldValue & ~mask) | value;
        if (newValue != oldValue) {
            umutablecptrie_set(mutableCPTrie, start, newValue, &errorCode);
        }
        return;
    }
    while (start <= end && U_SUCCESS(errorCode)) {
        uint32_t oldValue;
        UChar32 rangeEnd = umutablecptrie_getRange(
            mutableCPTrie, start, UCPMAP_RANGE_NORMAL, 0, nullptr, nullptr, &oldValue);
        if (rangeEnd > end) {
            rangeEnd = end;
        }
        uint32_t newValue = (oldValue & ~mask) | value;
        if (newValue != oldValue) {
            umutablecptrie_setRange(mutableCPTrie, start, rangeEnd, newValue, &errorCode);
        }
        start = rangeEnd + 1;
    }
}

namespace {

void U_CALLCONV
parsePropsOfStringsLineFn(
        void *context,
        char *fields[][2], int32_t /* fieldCount */,
        UErrorCode *pErrorCode) {
    reinterpret_cast<EmojiPropsBuilder *>(context)->parsePropsOfStringsLine(fields, *pErrorCode);
}

}  // namespace

void
EmojiPropsBuilder::parseUnidataFiles(const char *unidataPath, UErrorCode &errorCode) {
    CharString path(unidataPath, errorCode);
    path.ensureEndsWithFileSeparator(errorCode);
    if (U_FAILURE(errorCode)) { return; }
    int32_t pathLength = path.length();
    path.append("emoji-sequences.txt", errorCode);
    parsePropsOfStringsFile(path.data(), errorCode);
    if (U_FAILURE(errorCode)) { return; }
    path.truncate(pathLength);
    path.append("emoji-zwj-sequences.txt", errorCode);
    parsePropsOfStringsFile(path.data(), errorCode);

    if (U_SUCCESS(errorCode) && !unrecognized.empty()) {
        puts("\n*** genprops/emoji warning: sample of unrecognized property names:");
        int32_t i = 0;
        for (const auto &s : unrecognized) {
            printf("    \"%s\"\n", s.c_str());
            if (++i == 10) { break; }
        }
    }
}

void
EmojiPropsBuilder::parsePropsOfStringsFile(const char *path, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    char *fields[3][2];
    u_parseDelimitedFile(path, ';', fields, 3, parsePropsOfStringsLineFn, this, &errorCode);
}

void EmojiPropsBuilder::parsePropsOfStringsLine(char *fields[][2], UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    // Format:
    //   code_point(s) ; type_field ; description # comments
    *fields[1][1] = 0;  // NUL-terminate the name field
    char *propName = const_cast<char *>(u_skipWhitespace(fields[1][0]));
    u_rtrim(propName);
    int32_t index = -1;
    for (const PropNameToIndex &pn2i : propNameToIndex) {
        if (strcmp(pn2i.propName, propName) == 0) {
            index = pn2i.emojiPropsIndex;
            break;
        }
    }
    if (index < 0) {
        // not a supported property
        unrecognized.insert(propName);
        return;
    }

    const char *rangeOrString = fields[0][0];
    if (strstr(rangeOrString, "..") != nullptr) {
        // Code point range:
        // 231A..231B    ; Basic_Emoji                  ; watch
        if (index != EmojiProps::IX_BASIC_EMOJI_TRIE_OFFSET) {
            fprintf(stderr,
                    "genprops/emoji error: single code points %s for %s\n", rangeOrString, propName);
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        uint32_t start, end;
        u_parseCodePointRange(rangeOrString, &start, &end, &errorCode);
        setBit(start, end, EmojiProps::BIT_BASIC_EMOJI, true, errorCode);
    } else {
        // Code point or string:
        // 23F0          ; Basic_Emoji                  ; alarm clock
        // 23F1 FE0F     ; Basic_Emoji                  ; stopwatch
        uint32_t first;
        UChar s[100];
        int32_t length = u_parseString(rangeOrString, s, UPRV_LENGTHOF(s), &first, &errorCode);
        if (U_FAILURE(errorCode)) { return; }
        if (length == 0) {
            fprintf(stderr,
                    "genprops/emoji error: empty string on line\n    %s ; %s ; %s\n",
                    rangeOrString, propName, fields[2][0]);
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        if (length == U16_LENGTH(first)) {
            // single code point
            if (index != EmojiProps::IX_BASIC_EMOJI_TRIE_OFFSET) {
                fprintf(stderr,
                        "genprops/emoji error: single code point %s for %s\n", rangeOrString, propName);
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
            setBit(first, first, EmojiProps::BIT_BASIC_EMOJI, true, errorCode);
        } else {
            // more than one code point
            UnicodeString us(false, s, length);
            getTrieBuilder(index).add(us, 0, errorCode);
            ++getNumStrings(index);
        }
    }
}

void
EmojiPropsBuilder::build(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    cpTrie = umutablecptrie_buildImmutable(
        mutableCPTrie, UCPTRIE_TYPE_FAST, UCPTRIE_VALUE_BITS_8, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/emoji error: umutablecptrie_buildImmutable() failed: %s\n",
                u_errorName(errorCode));
        return;
    }
    trieSize = ucptrie_toBinary(cpTrie, trieBlock, sizeof(trieBlock), &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/emoji error: ucptrie_toBinary() failed: %s (length %ld)\n",
                u_errorName(errorCode), (long)trieSize);
        return;
    }
    U_ASSERT((trieSize & 3) == 0);  // multiple of 4 bytes

    for (int32_t index = TRIE_IX_START; index < TRIE_IX_LIMIT; ++index) {
        if (getNumStrings(index) == 0) {
            fprintf(stderr, "genprops/emoji error: no strings for property index %d\n", (int)index);
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        UCharsTrieBuilder &builder = getTrieBuilder(index);
        UnicodeString &result = getTrieString(index);
        builder.buildUnicodeString(USTRINGTRIE_BUILD_SMALL, result, errorCode);
        if (U_FAILURE(errorCode)) {
            fprintf(stderr,
                    "genprops/emoji error: UCharsTrieBuilder[%d].buildUnicodeString() failed: %s\n",
                    (int)index, u_errorName(errorCode));
            return;
        }
    }

    // Set indexes.
    int32_t length = sizeof(indexes);
    U_ASSERT(length == EmojiProps::IX_COUNT * 4);
    int32_t offset = length;
    indexes[EmojiProps::IX_CPTRIE_OFFSET] = offset;
    if (!beQuiet) {
        puts("* uemoji.icu stats *");
        printf("UCPTrie size in bytes:                                 %5u\n", (int)trieSize);
    }
    offset += trieSize;

    indexes[EmojiProps::IX_RESERVED1] = offset;
    indexes[EmojiProps::IX_RESERVED2] = offset;
    indexes[EmojiProps::IX_RESERVED3] = offset;

    int32_t index = EmojiProps::IX_BASIC_EMOJI_TRIE_OFFSET;
    indexes[index] = offset;
    length = getTrieString(index).length() * 2;
    if (!beQuiet) {
        printf("UCharsTrie size in bytes: Basic_Emoji                  %5u  num strings: %5u\n",
               (int)length, (int)getNumStrings(index));
    }
    offset += length;

    index = EmojiProps::IX_EMOJI_KEYCAP_SEQUENCE_TRIE_OFFSET;
    indexes[index] = offset;
    length = getTrieString(index).length() * 2;
    if (!beQuiet) {
        printf("UCharsTrie size in bytes: Emoji_Keycap_Sequence        %5u  num strings: %5u\n",
               (int)length, (int)getNumStrings(index));
    }
    offset += length;

    index = EmojiProps::IX_RGI_EMOJI_MODIFIER_SEQUENCE_TRIE_OFFSET;
    indexes[index] = offset;
    length = getTrieString(index).length() * 2;
    if (!beQuiet) {
        printf("UCharsTrie size in bytes: RGI_Emoji_Modifier_Sequence  %5u  num strings: %5u\n",
               (int)length, (int)getNumStrings(index));
    }
    offset += length;

    index = EmojiProps::IX_RGI_EMOJI_FLAG_SEQUENCE_TRIE_OFFSET;
    indexes[index] = offset;
    length = getTrieString(index).length() * 2;
    if (!beQuiet) {
        printf("UCharsTrie size in bytes: RGI_Emoji_Flag_Sequence      %5u  num strings: %5u\n",
               (int)length, (int)getNumStrings(index));
    }
    offset += length;

    index = EmojiProps::IX_RGI_EMOJI_TAG_SEQUENCE_TRIE_OFFSET;
    indexes[index] = offset;
    length = getTrieString(index).length() * 2;
    if (!beQuiet) {
        printf("UCharsTrie size in bytes: RGI_Emoji_Tag_Sequence       %5u  num strings: %5u\n",
               (int)length, (int)getNumStrings(index));
    }
    offset += length;

    index = EmojiProps::IX_RGI_EMOJI_ZWJ_SEQUENCE_TRIE_OFFSET;
    indexes[index] = offset;
    length = getTrieString(index).length() * 2;
    if (!beQuiet) {
        printf("UCharsTrie size in bytes: RGI_Emoji_ZWJ_Sequence       %5u  num strings: %5u\n",
               (int)length, (int)getNumStrings(index));
    }
    offset += length;

    indexes[EmojiProps::IX_RESERVED10] = offset;
    indexes[EmojiProps::IX_RESERVED11] = offset;
    indexes[EmojiProps::IX_RESERVED12] = offset;
    indexes[EmojiProps::IX_TOTAL_SIZE] = offset;

    if (!beQuiet) {
        printf("data size:                                            %6ld\n", (long)offset);
    }
}

namespace {

void writeTrieBlock(UNewDataMemory *pData, const UnicodeString &s) {
    udata_writeBlock(pData, s.getBuffer(), s.length() * 2);
}

}  // namespace

void
EmojiPropsBuilder::writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData = udata_create(path, "icu", "uemoji", &dataInfo,
                                         withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/emoji error: udata_create(%s, uemoji.icu) failed: %s\n",
                path, u_errorName(errorCode));
        return;
    }

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);
    writeTrieBlock(pData, getTrieString(EmojiProps::IX_BASIC_EMOJI_TRIE_OFFSET));
    writeTrieBlock(pData, getTrieString(EmojiProps::IX_EMOJI_KEYCAP_SEQUENCE_TRIE_OFFSET));
    writeTrieBlock(pData, getTrieString(EmojiProps::IX_RGI_EMOJI_MODIFIER_SEQUENCE_TRIE_OFFSET));
    writeTrieBlock(pData, getTrieString(EmojiProps::IX_RGI_EMOJI_FLAG_SEQUENCE_TRIE_OFFSET));
    writeTrieBlock(pData, getTrieString(EmojiProps::IX_RGI_EMOJI_TAG_SEQUENCE_TRIE_OFFSET));
    writeTrieBlock(pData, getTrieString(EmojiProps::IX_RGI_EMOJI_ZWJ_SEQUENCE_TRIE_OFFSET));

    long dataLength = udata_finish(pData, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/emoji error: error %s writing the output file\n",
                u_errorName(errorCode));
        return;
    }

    int32_t totalSize = indexes[EmojiProps::IX_TOTAL_SIZE];
    if (dataLength != (long)totalSize) {
        fprintf(stderr,
                "udata_finish(uemoji.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)totalSize);
        errorCode = U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createEmojiPropsBuilder(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return nullptr; }
    PropsBuilder *pb = new EmojiPropsBuilder(errorCode);
    if (pb == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
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
