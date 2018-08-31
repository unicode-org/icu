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
#include "writesrc.h"

U_NAMESPACE_USE

class LayoutPropsBuilder : public PropsBuilder {
public:
    LayoutPropsBuilder(UErrorCode &errorCode);
    virtual ~LayoutPropsBuilder();

    virtual void setProps(const UniProps &props, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void build(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);

private:
    void setIntProp(const UniProps &, const UnicodeSet &newValues,
                    UProperty prop, UMutableCPTrie *trie,
                    UErrorCode &errorCode);
    int32_t getMaxIntValue(UProperty prop) const {
        return maxIntValues[prop - UCHAR_INT_START];
    }
    void checkMaxIntValue(UProperty prop, int32_t maxMax, UErrorCode &errorCode) const;

    int32_t maxIntValues[UCHAR_INT_LIMIT - UCHAR_INT_START];
    UMutableCPTrie *inpcTrie;
    UMutableCPTrie *inscTrie;
    UMutableCPTrie *voTrie;
};

LayoutPropsBuilder::LayoutPropsBuilder(UErrorCode &errorCode) {
    memset(maxIntValues, 0, sizeof(maxIntValues));
    inpcTrie = umutablecptrie_open(0, 0, &errorCode);
    inscTrie = umutablecptrie_open(0, 0, &errorCode);
    voTrie = umutablecptrie_open(0, 0, &errorCode);
    if (U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: layoutpropsbuilder umutablecptrie_open() failed - %s\n",
                u_errorName(errorCode));
    }
}

LayoutPropsBuilder::~LayoutPropsBuilder() {
    umutablecptrie_close(inpcTrie);
    umutablecptrie_close(inscTrie);
    umutablecptrie_close(voTrie);
}

void
LayoutPropsBuilder::setProps(const UniProps &props, const UnicodeSet &newValues,
                             UErrorCode &errorCode) {
    setIntProp(props, newValues, UCHAR_INDIC_POSITIONAL_CATEGORY, inpcTrie, errorCode);
    setIntProp(props, newValues, UCHAR_INDIC_SYLLABIC_CATEGORY, inscTrie, errorCode);
    setIntProp(props, newValues, UCHAR_VERTICAL_ORIENTATION, voTrie, errorCode);
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

uint8_t trieBlock[100000];
int32_t trieSize;

void writeUCPTrie(FILE *f, const char *name, UMutableCPTrie *mutableTrie,
                  UCPTrieType type, UCPTrieValueWidth valueWidth, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    LocalUCPTriePointer trie(
        umutablecptrie_buildImmutable(mutableTrie, type, valueWidth, &errorCode));
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: %s trie buildImmutable() failed: %s\n",
                name, u_errorName(errorCode));
        return;
    }
    usrc_writeUCPTrie(f, name, trie.getAlias());
    if (!beQuiet) {
        int32_t size = ucptrie_toBinary(trie.getAlias(),
                                        trieBlock, UPRV_LENGTHOF(trieBlock), &errorCode);
        printf("%11s trie size in bytes:        %5u\n", name, (int)size);
    }
}

}  // namespace

void
LayoutPropsBuilder::build(UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    checkMaxIntValue(UCHAR_INDIC_POSITIONAL_CATEGORY, 0xff, errorCode);
    checkMaxIntValue(UCHAR_INDIC_SYLLABIC_CATEGORY, 0xff, errorCode);
    checkMaxIntValue(UCHAR_VERTICAL_ORIENTATION, 0xff, errorCode);
}

void LayoutPropsBuilder::checkMaxIntValue(UProperty prop, int32_t maxMax,
                                          UErrorCode &errorCode) const {
    int32_t max = getMaxIntValue(prop);
    if (max > maxMax) {
        fprintf(stderr, "genprops error: 0x%x max value = %d overflow\n", (int)prop, (int)max);
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

void
LayoutPropsBuilder::writeCSourceFile(const char *path, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    if (!beQuiet) {
        puts("* text layout properties stats *");
    }

    FILE *f = usrc_create(path, "ulayout_props_data.h", 2018,
                          "icu/tools/unicode/c/genprops/layoutpropsbuilder.cpp");
    if (f == nullptr) {
        errorCode = U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifdef INCLUDED_FROM_UPROPS_CPP\n\n", f);

    fprintf(f, "static const int32_t maxInPCValue = %ld;\n\n",
            (long)getMaxIntValue(UCHAR_INDIC_POSITIONAL_CATEGORY));
    writeUCPTrie(f, "inpc", inpcTrie, UCPTRIE_TYPE_SMALL, UCPTRIE_VALUE_BITS_8, errorCode);

    fprintf(f, "static const int32_t maxInSCValue = %ld;\n\n",
            (long)getMaxIntValue(UCHAR_INDIC_SYLLABIC_CATEGORY));
    writeUCPTrie(f, "insc", inscTrie, UCPTRIE_TYPE_SMALL, UCPTRIE_VALUE_BITS_8, errorCode);

    fprintf(f, "static const int32_t maxVoValue = %ld;\n\n",
            (long)getMaxIntValue(UCHAR_VERTICAL_ORIENTATION));
    writeUCPTrie(f, "vo", voTrie, UCPTRIE_TYPE_SMALL, UCPTRIE_VALUE_BITS_8, errorCode);

    fputs("#endif  // INCLUDED_FROM_UPROPS_CPP\n", f);
    fclose(f);
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
