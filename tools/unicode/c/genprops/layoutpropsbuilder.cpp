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
    virtual ~LayoutPropsBuilder() U_OVERRIDE;

    virtual void setProps(const UniProps &props, const UnicodeSet &newValues, UErrorCode &errorCode) U_OVERRIDE;
    virtual void build(UErrorCode &errorCode) U_OVERRIDE;
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode) U_OVERRIDE;
    virtual void writeJavaSourceFile(const char *path, UErrorCode &errorCode) U_OVERRIDE;

private:
    void setIntProp(const UniProps &, const UnicodeSet &newValues,
                    UProperty prop, UMutableCPTrie *trie,
                    UErrorCode &errorCode);
    int32_t getMaxIntValue(UProperty prop) const {
        return maxIntValues[prop - UCHAR_INT_START];
    }
    void checkMaxIntValue(UProperty prop, int32_t maxMax, UErrorCode &errorCode) const;
    void writeMaxIntValue(FILE *f, const char *name, UProperty prop) const {
        fprintf(f, "static const int32_t max%sValue = %ld;\n\n", name, (long)getMaxIntValue(prop));
    }

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

    FILE *f = usrc_create(path, "ulayout_props_data.h", 2018,
                          "icu/tools/unicode/c/genprops/layoutpropsbuilder.cpp");
    if (f == nullptr) {
        errorCode = U_FILE_ACCESS_ERROR;
        return;
    }
    fputs("#ifdef INCLUDED_FROM_UPROPS_CPP\n\n", f);

    writeMaxIntValue(f, "InPC", UCHAR_INDIC_POSITIONAL_CATEGORY);
    usrc_writeUCPTrie(f, "inpc", inpcTrie);

    writeMaxIntValue(f, "InSC", UCHAR_INDIC_SYLLABIC_CATEGORY);
    usrc_writeUCPTrie(f, "insc", inscTrie);

    writeMaxIntValue(f, "Vo", UCHAR_VERTICAL_ORIENTATION);
    usrc_writeUCPTrie(f, "vo", voTrie);

    fputs("#endif  // INCLUDED_FROM_UPROPS_CPP\n", f);
    fclose(f);
}

namespace {

// Write one byte as one char in a Java String literal.
// Java class file string literals use one byte per U+0001..U+007F,
// and two bytes per U+0000 or U+0080..U+07FF.
// This is reasonably compact if small byte values are more common, as usual.
// Since 0 is very common but takes two bytes, we swap it with U+007A 'z'.
int32_t appendByte(char *s, int32_t length, uint8_t b) {
    if (b == 0) {
        s[length++] = 'z';
    } else if (b == 0x7a) {
        s[length++] = '\\';
        s[length++] = '0';
    } else {
        // Write all other bytes as octal escapes. (Java does not support \xhh.)
        // We could make the source file smaller by writing ASCII characters
        // directly where possible, but that would not make the class file any smaller,
        // and we would have to be careful to still escape certain characters,
        // and to escape digits after short octal escapes.
        s[length++] = '\\';
        if (b >= 0100) {
            s[length++] = '0' + (b >> 6);
        }
        if (b >= 010) {
            s[length++] = '0' + ((b >> 3) & 7);
        }
        s[length++] = '0' + (b & 7);
    }
    return length;
}

void writeBytesAsJavaString(FILE *f, const uint8_t *bytes, int32_t length) {
    // Quotes, line feed, etc., with up to 16 bytes per line, up to 4 bytes "\377" each.
    char line[80];
    int32_t lineLength = 0;
    for (int32_t i = 0;;) {
        if ((i & 0xf) == 0) {  // start of a line of 16 bytes
            line[0] = '"';
            lineLength = 1;
        }
        if (i < length) {
            lineLength = appendByte(line, lineLength, bytes[i++]);
        }
        if (i == length) {  // end of the string
            line[lineLength++] = '"';
            line[lineLength++] = '\n';
            line[lineLength++] = '\n';
            line[lineLength++] = 0;
            fputs(line, f);
            break;
        }
        if ((i & 0xf) == 0) {  // end of a line of 16 bytes
            line[lineLength++] = '"';
            line[lineLength++] = ' ';
            line[lineLength++] = '+';
            line[lineLength++] = '\n';
            line[lineLength++] = 0;
            fputs(line, f);
        }
    }
}

static uint8_t trieBlock[100000];

void writeUCPTrieAsJavaString(FILE *f, const UCPTrie *trie, UErrorCode &errorCode) {
    int32_t length = ucptrie_toBinary(trie, trieBlock, UPRV_LENGTHOF(trieBlock), &errorCode);
    writeBytesAsJavaString(f, trieBlock, length);
}

}  // namespace

// So far, this writes initializers to be copied into Java code, but not a complete Java file.
// We should probably write a regular, binary ICU data file and read that into Java.
void
LayoutPropsBuilder::writeJavaSourceFile(const char * /*path*/, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }

    FILE *f = usrc_create("/tmp", "ulayout_props_data.txt", 2018,
                          "icu/tools/unicode/c/genprops/layoutpropsbuilder.cpp");
    if (f == nullptr) {
        errorCode = U_FILE_ACCESS_ERROR;
        return;
    }

    writeMaxIntValue(f, "InPC", UCHAR_INDIC_POSITIONAL_CATEGORY);
    writeUCPTrieAsJavaString(f, inpcTrie, errorCode);

    writeMaxIntValue(f, "InSC", UCHAR_INDIC_SYLLABIC_CATEGORY);
    writeUCPTrieAsJavaString(f, inscTrie, errorCode);

    writeMaxIntValue(f, "Vo", UCHAR_VERTICAL_ORIENTATION);
    writeUCPTrieAsJavaString(f, voTrie, errorCode);

    fclose(f);
    puts("  ++ Java initializers written to /tmp/ulayout_props_data.txt");
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
