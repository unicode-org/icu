// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// linktermpropsbuilder.cpp
// created: 2025 for UTS #58 / Unicode 17.0

#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/ucptrie.h"
#include "unicode/udata.h"
#include "unicode/umutablecptrie.h"
#include "unicode/uniset.h"
#include "charstr.h"
#include "cmemory.h"
#include "genprops.h"
#include "linktermprops.h"
#include "uassert.h"
#include "unewdata.h"
#include "uparse.h"

/* Link termination properties file format ------------------------------------

The file format prepared and written here contains several data
structures that store indexes or data.

Before the data contents described below, there are the headers required by
the udata API for loading ICU data. Especially, a UDataInfo structure
precedes the actual data. It contains platform properties values and the
file format version.

The following is a description of format version 1.0 .

The file contains the following structures:

    const int32_t indexes[IX_LINK_TERM_COUNT] with values i0, i1, ...:
    (see LinkTermProps::IX_... constants for names of indexes)

    i0  IX_COUNT:       length of indexes[] (LinkTermProps::IX_LINK_TERM_COUNT = 8)
    i1  IX_CPTRIE_TOP:  limit byte offset of the Link_Term UCPTrie
    i2  IX_TRIE2_TOP:   reserved; same as IX_CPTRIE_TOP until a second trie is added
    i3  IX_TRIE3_TOP:   reserved; same as IX_TRIE2_TOP  until a third  trie is added
    i4  IX_TOTAL_SIZE:  total data size (same as the limit of the last trie)
    i5..i7              reserved, 0

    Byte offsets are from the start of the indexes[] array.

    After the indexes array follows a UCPTrie=CodePointTrie (type=fast, valueWidth=8)
    storing ULinkTerm values; see the ULINK_TERM_... enum in linktermprops.h.
    The default value ULINK_TERM_HARD=0 covers all unlisted code points.
    The trie is padded to a multiple of 16 bytes.

    Slots i2 and i3 are reserved for two additional property tries anticipated
    in a near future version of this file format.  When added, they will follow
    the Link_Term trie in the same layout (fast or small UCPTrie, 16-byte-padded).

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

    { 'L', 'n', 'k', 'T' },  // dataFormat
    { 1, 0, 0, 0 },           // formatVersion
    { 0, 0, 0, 0 }            // dataVersion (filled from ppucd Unicode version)
};

class LinkTermPropsBuilder : public PropsBuilder {
public:
    LinkTermPropsBuilder(UErrorCode &errorCode);
    ~LinkTermPropsBuilder() override;

    void setUnicodeVersion(const UVersionInfo version) override;
    void parseUnidataFiles(const char *unidataPath, UErrorCode &errorCode) override;
    void build(UErrorCode &errorCode) override;
    void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) override;

    // visible for C callback adapter
    void handleLine(char *fields[][2], UErrorCode &errorCode);

private:
    UMutableCPTrie *mutableCPTrie = nullptr;
    UCPTrie        *cpTrie        = nullptr;

    static constexpr int32_t TRIE_BLOCK_CAPACITY = 100000;
    uint8_t trieBlock[TRIE_BLOCK_CAPACITY];
    int32_t trieSize = 0;
};

LinkTermPropsBuilder::LinkTermPropsBuilder(UErrorCode &errorCode) {
    // Default: INCLUDE for most unlisted code points; HARD is applied below
    // for characters that should always terminate links.
    mutableCPTrie = umutablecptrie_open(ULINK_TERM_INCLUDE, ULINK_TERM_HARD, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/linkterm error: umutablecptrie_open() failed: %s\n",
                u_errorName(errorCode));
        return;
    }

    // Compute the set of code points that are HARD by default:
    //   White_Space | Noncharacter_Code_Point | Deprecated |
    //   [C - Cf]  (i.e. Cc + Cs + Co + Cn, but NOT format chars)
    // LinkTerm.txt values take precedence and are applied afterwards in
    // parseUnidataFiles().
    UnicodeSet hardDefaults(
        UNICODE_STRING_SIMPLE(
            "[[:White_Space:][:Noncharacter_Code_Point:][:Deprecated:]"
            "[:Cc:][:Cs:][:Co:][:Cn:]]"),
        errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/linkterm error: UnicodeSet for hard defaults failed: %s\n",
                u_errorName(errorCode));
        return;
    }
    for (int32_t i = 0; i < hardDefaults.getRangeCount(); ++i) {
        umutablecptrie_setRange(mutableCPTrie,
                                hardDefaults.getRangeStart(i),
                                hardDefaults.getRangeEnd(i),
                                ULINK_TERM_HARD, &errorCode);
        if (U_FAILURE(errorCode)) {
            fprintf(stderr,
                    "genprops/linkterm error: umutablecptrie_setRange(U+%04X..U+%04X) failed: %s\n",
                    hardDefaults.getRangeStart(i), hardDefaults.getRangeEnd(i),
                    u_errorName(errorCode));
            return;
        }
    }
}

LinkTermPropsBuilder::~LinkTermPropsBuilder() {
    umutablecptrie_close(mutableCPTrie);
    ucptrie_close(cpTrie);
}

void
LinkTermPropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

namespace {

void U_CALLCONV
parseLinkTermLineFn(void *context, char *fields[][2], int32_t, UErrorCode *pErrorCode) {
    reinterpret_cast<LinkTermPropsBuilder *>(context)->handleLine(fields, *pErrorCode);
}

}  // namespace

void
LinkTermPropsBuilder::parseUnidataFiles(const char *unidataPath, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    CharString path(unidataPath, errorCode);
    path.ensureEndsWithFileSeparator(errorCode);
    path.append("LinkTerm.txt", errorCode);
    if (U_FAILURE(errorCode)) { return; }

    char *fields[2][2];
    u_parseDelimitedFile(path.data(), ';', fields, 2, parseLinkTermLineFn, this, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/linkterm error: parsing %s failed: %s\n",
                path.data(), u_errorName(errorCode));
    }
}

void
LinkTermPropsBuilder::handleLine(char *fields[][2], UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    // Field 0: code point or range, e.g. "0029" or "0021..0022".
    //          Already NUL-terminated by u_parseDelimitedFile at the ';'.
    // Field 1: value name, possibly followed by " # comment".
    //          fields[1][1] points to the end of the line; NUL-terminate it.
    *fields[1][1] = 0;
    char *value = const_cast<char *>(u_skipWhitespace(fields[1][0]));
    // Strip any inline # comment before trimming.
    char *hash = strchr(value, '#');
    if (hash != nullptr) { *hash = 0; }
    u_rtrim(value);

    uint32_t v;
    if      (strcmp(value, "Hard")    == 0) { v = ULINK_TERM_HARD; }
    else if (strcmp(value, "Include") == 0) { v = ULINK_TERM_INCLUDE; }
    else if (strcmp(value, "Soft")    == 0) { v = ULINK_TERM_SOFT; }
    else if (strcmp(value, "Close")   == 0) { v = ULINK_TERM_CLOSE; }
    else if (strcmp(value, "Open")    == 0) { v = ULINK_TERM_OPEN; }
    else {
        fprintf(stderr, "genprops/linkterm error: unknown Link_Term value \"%s\"\n", value);
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    uint32_t start, end;
    u_parseCodePointRange(fields[0][0], &start, &end, &errorCode);
    if (U_FAILURE(errorCode)) { return; }

    if (start == end) {
        umutablecptrie_set(mutableCPTrie, start, v, &errorCode);
    } else {
        umutablecptrie_setRange(mutableCPTrie, start, end, v, &errorCode);
    }
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkterm error: umutablecptrie_set(U+%04X..U+%04X, %u) failed: %s\n",
                start, end, v, u_errorName(errorCode));
    }
}

void
LinkTermPropsBuilder::build(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    if (!beQuiet) { puts("* ulinkterm.icu stats *"); }

    cpTrie = umutablecptrie_buildImmutable(
        mutableCPTrie, UCPTRIE_TYPE_FAST, UCPTRIE_VALUE_BITS_8, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkterm error: umutablecptrie_buildImmutable() failed: %s\n",
                u_errorName(errorCode));
        return;
    }

    trieSize = ucptrie_toBinary(cpTrie, trieBlock, TRIE_BLOCK_CAPACITY, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkterm error: ucptrie_toBinary() failed: %s (length %d)\n",
                u_errorName(errorCode), (int)trieSize);
        return;
    }

    // Pad to a multiple of 16 bytes for alignment of any subsequent trie.
    while ((trieSize & 0xf) != 0) {
        trieBlock[trieSize++] = 0xaa;
    }

    if (!beQuiet) {
        printf("UCPTrie size in bytes: %5d\n", (int)trieSize);
        printf("data size:             %5d\n",
               (int)(LinkTermProps::IX_LINK_TERM_COUNT * 4 + trieSize));
    }
}

void
LinkTermPropsBuilder::writeBinaryData(const char *path, UBool withCopyright,
                                      UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData = udata_create(
        path, LinkTermProps::DATA_TYPE, LinkTermProps::DATA_NAME, &dataInfo,
        withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkterm error: udata_create(%s, ulinkterm.icu) failed: %s\n",
                path, u_errorName(errorCode));
        return;
    }

    int32_t indexes[LinkTermProps::IX_LINK_TERM_COUNT] = {};
    indexes[LinkTermProps::IX_COUNT] = LinkTermProps::IX_LINK_TERM_COUNT;
    // Offsets are from the start of the indexes[] array.
    int32_t top = LinkTermProps::IX_LINK_TERM_COUNT * 4;
    indexes[LinkTermProps::IX_CPTRIE_TOP] = (top += trieSize);
    // Reserved trie slots are set to the same offset as the previous trie's limit,
    // so they look empty until a future format version uses them.
    indexes[LinkTermProps::IX_TRIE2_TOP]  = top;
    indexes[LinkTermProps::IX_TRIE3_TOP]  = top;
    indexes[LinkTermProps::IX_TOTAL_SIZE] = top;

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);

    long dataLength = udata_finish(pData, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/linkterm error: %s writing the output file\n",
                u_errorName(errorCode));
        return;
    }

    if (dataLength != (long)top) {
        fprintf(stderr,
                "udata_finish(ulinkterm.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)top);
        errorCode = U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createLinkTermPropsBuilder(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return nullptr; }
    PropsBuilder *pb = new LinkTermPropsBuilder(errorCode);
    if (pb == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}
