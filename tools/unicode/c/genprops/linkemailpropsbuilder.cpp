// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// linkemailpropsbuilder.cpp
// created: 2025 for UTS #58 / Unicode 17.0

#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/ucptrie.h"
#include "unicode/udata.h"
#include "unicode/umutablecptrie.h"
#include "charstr.h"
#include "cmemory.h"
#include "genprops.h"
#include "linkemailprops.h"
#include "uassert.h"
#include "unewdata.h"
#include "uparse.h"

/* Link_Email properties file format -------------------------------------------

The file format is identical in structure to ulinkterm.icu (see
linktermpropsbuilder.cpp), with these differences:

    The dataFormat tag is 'L','n','k','E' instead of 'L','n','k','T'.

    The UCPTrie stores uint8_t values where 0 = No (default for all unlisted
    code points) and 1 = Yes (code point is allowed in an email local part).

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

    { 'L', 'n', 'k', 'E' },  // dataFormat
    { 1, 0, 0, 0 },           // formatVersion
    { 0, 0, 0, 0 }            // dataVersion (filled from ppucd Unicode version)
};

class LinkEmailPropsBuilder : public PropsBuilder {
public:
    LinkEmailPropsBuilder(UErrorCode &errorCode);
    ~LinkEmailPropsBuilder() override;

    void setUnicodeVersion(const UVersionInfo version) override;
    void parseUnidataFiles(const char *unidataPath, UErrorCode &errorCode) override;
    void build(UErrorCode &errorCode) override;
    void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode) override;

private:
    UMutableCPTrie *mutableCPTrie = nullptr;
    UCPTrie        *cpTrie        = nullptr;

    static constexpr int32_t TRIE_BLOCK_CAPACITY = 100000;
    uint8_t trieBlock[TRIE_BLOCK_CAPACITY];
    int32_t trieSize = 0;
};

LinkEmailPropsBuilder::LinkEmailPropsBuilder(UErrorCode &errorCode) {
    // Default value 0 = No; error value 0 = No (same).
    mutableCPTrie = umutablecptrie_open(0, 0, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/linkemail error: umutablecptrie_open() failed: %s\n",
                u_errorName(errorCode));
    }
}

LinkEmailPropsBuilder::~LinkEmailPropsBuilder() {
    umutablecptrie_close(mutableCPTrie);
    ucptrie_close(cpTrie);
}

void
LinkEmailPropsBuilder::setUnicodeVersion(const UVersionInfo version) {
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}

void
LinkEmailPropsBuilder::parseUnidataFiles(const char *unidataPath, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    CharString path(unidataPath, errorCode);
    path.ensureEndsWithFileSeparator(errorCode);
    path.append("LinkEmail.txt", errorCode);
    if (U_FAILURE(errorCode)) { return; }

    // LinkEmail.txt has one field per data line (just a code point range);
    // all listed code points have Link_Email=Yes.  Parse the file directly
    // rather than through u_parseDelimitedFile to avoid ambiguity with the
    // single-field format.
    FILE *f = fopen(path.data(), "r");
    if (f == nullptr) {
        fprintf(stderr, "genprops/linkemail error: cannot open %s\n", path.data());
        errorCode = U_FILE_ACCESS_ERROR;
        return;
    }

    char line[300];
    int lineNum = 0;
    while (fgets(line, (int)sizeof(line), f) != nullptr) {
        ++lineNum;
        // Skip leading whitespace, blank lines, and comment lines.
        char *p = line;
        while (*p == ' ' || *p == '\t') { ++p; }
        if (*p == '#' || *p == '\r' || *p == '\n' || *p == '\0') { continue; }
        // Strip trailing inline comment.
        char *hash = strchr(p, '#');
        if (hash != nullptr) { *hash = '\0'; }

        uint32_t start, end;
        u_parseCodePointRange(p, &start, &end, &errorCode);
        if (U_FAILURE(errorCode)) {
            fprintf(stderr, "genprops/linkemail error: bad code point range on line %d of %s: %s\n",
                    lineNum, path.data(), u_errorName(errorCode));
            fclose(f);
            return;
        }

        if (start == end) {
            umutablecptrie_set(mutableCPTrie, start, 1, &errorCode);
        } else {
            umutablecptrie_setRange(mutableCPTrie, start, end, 1, &errorCode);
        }
        if (U_FAILURE(errorCode)) {
            fprintf(stderr,
                    "genprops/linkemail error: umutablecptrie_set(U+%04X..U+%04X) failed: %s\n",
                    start, end, u_errorName(errorCode));
            fclose(f);
            return;
        }
    }
    fclose(f);
}

void
LinkEmailPropsBuilder::build(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    if (!beQuiet) { puts("* ulinkemail.icu stats *"); }

    cpTrie = umutablecptrie_buildImmutable(
        mutableCPTrie, UCPTRIE_TYPE_FAST, UCPTRIE_VALUE_BITS_8, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkemail error: umutablecptrie_buildImmutable() failed: %s\n",
                u_errorName(errorCode));
        return;
    }

    trieSize = ucptrie_toBinary(cpTrie, trieBlock, TRIE_BLOCK_CAPACITY, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkemail error: ucptrie_toBinary() failed: %s (length %d)\n",
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
               (int)(LinkEmailProps::IX_LINK_EMAIL_COUNT * 4 + trieSize));
    }
}

void
LinkEmailPropsBuilder::writeBinaryData(const char *path, UBool withCopyright,
                                       UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    UNewDataMemory *pData = udata_create(
        path, LinkEmailProps::DATA_TYPE, LinkEmailProps::DATA_NAME, &dataInfo,
        withCopyright ? U_COPYRIGHT_STRING : nullptr, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops/linkemail error: udata_create(%s, ulinkemail.icu) failed: %s\n",
                path, u_errorName(errorCode));
        return;
    }

    int32_t indexes[LinkEmailProps::IX_LINK_EMAIL_COUNT] = {};
    indexes[LinkEmailProps::IX_COUNT] = LinkEmailProps::IX_LINK_EMAIL_COUNT;
    int32_t top = LinkEmailProps::IX_LINK_EMAIL_COUNT * 4;
    indexes[LinkEmailProps::IX_CPTRIE_TOP] = (top += trieSize);
    indexes[LinkEmailProps::IX_TRIE2_TOP]  = top;
    indexes[LinkEmailProps::IX_TRIE3_TOP]  = top;
    indexes[LinkEmailProps::IX_TOTAL_SIZE] = top;

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, trieBlock, trieSize);

    long dataLength = udata_finish(pData, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops/linkemail error: %s writing the output file\n",
                u_errorName(errorCode));
        return;
    }

    if (dataLength != (long)top) {
        fprintf(stderr,
                "udata_finish(ulinkemail.icu) reports %ld bytes written but should be %ld\n",
                dataLength, (long)top);
        errorCode = U_INTERNAL_PROGRAM_ERROR;
    }
}

PropsBuilder *
createLinkEmailPropsBuilder(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return nullptr; }
    PropsBuilder *pb = new LinkEmailPropsBuilder(errorCode);
    if (pb == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return pb;
}
