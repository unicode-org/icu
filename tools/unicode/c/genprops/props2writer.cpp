/*
*******************************************************************************
*
*   Copyright (C) 2002-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  props2writer.cpp (was props2.cpp)
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb24
*   created by: Markus W. Scherer
*
*   Parse more Unicode Character Database files and store
*   additional Unicode character properties in bit set vectors.
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "unicode/usetiter.h"
#include "unicode/uscript.h"
#include "cstring.h"
#include "genprops.h"
#include "propsvec.h"
#include "uassert.h"
#include "unewdata.h"
#include "uprops.h"
#include "utrie2.h"
#include "writesrc.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

U_NAMESPACE_USE

static UTrie2 *newTrie=NULL;
static UPropsVectors *pv=NULL;

static UnicodeString *scriptExtensions=NULL;

class Props2Writer : public PropsWriter {
public:
    Props2Writer(UErrorCode &errorCode);
    virtual ~Props2Writer();

    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
};

Props2Writer::Props2Writer(UErrorCode &errorCode) {
    pv=upvec_open(UPROPS_VECTOR_WORDS, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: props2writer upvec_open() failed - %s\n",
                u_errorName(errorCode));
    }
    scriptExtensions=new UnicodeString();
}

Props2Writer::~Props2Writer() {
    utrie2_close(newTrie);
    upvec_close(pv);
    delete scriptExtensions;
}

struct PropToBinary {
    int32_t prop;  // UProperty
    int32_t vecWord, vecShift;
};

static const PropToBinary
propToBinaries[]={
    { UCHAR_WHITE_SPACE,                    1, UPROPS_WHITE_SPACE },
    { UCHAR_DASH,                           1, UPROPS_DASH },
    // Note: The Hyphen property is stabilized since Unicode 4.0
    // and deprecated since Unicode 6.0.
    { UCHAR_HYPHEN,                         1, UPROPS_HYPHEN },
    { UCHAR_QUOTATION_MARK,                 1, UPROPS_QUOTATION_MARK },
    { UCHAR_TERMINAL_PUNCTUATION,           1, UPROPS_TERMINAL_PUNCTUATION },
    // Note: The Hex_Digit and ASCII_Hex_Digit properties are probably stable enough
    // so that they could be hardcoded.
    { UCHAR_HEX_DIGIT,                      1, UPROPS_HEX_DIGIT },
    { UCHAR_ASCII_HEX_DIGIT,                1, UPROPS_ASCII_HEX_DIGIT },
    { UCHAR_IDEOGRAPHIC,                    1, UPROPS_IDEOGRAPHIC },
    { UCHAR_DIACRITIC,                      1, UPROPS_DIACRITIC },
    { UCHAR_EXTENDER,                       1, UPROPS_EXTENDER },
    // Note: The Noncharacter_Code_Point property is probably stable enough
    // so that it could be hardcoded.
    { UCHAR_NONCHARACTER_CODE_POINT,        1, UPROPS_NONCHARACTER_CODE_POINT },
    // Note: The Grapheme_Link property is deprecated since Unicode 5.0
    // because it is a "Duplication of ccc=9" (UAX #44).
    { UCHAR_GRAPHEME_LINK,                  1, UPROPS_GRAPHEME_LINK },
    { UCHAR_IDS_BINARY_OPERATOR,            1, UPROPS_IDS_BINARY_OPERATOR },
    { UCHAR_IDS_TRINARY_OPERATOR,           1, UPROPS_IDS_TRINARY_OPERATOR },
    { UCHAR_RADICAL,                        1, UPROPS_RADICAL },
    { UCHAR_UNIFIED_IDEOGRAPH,              1, UPROPS_UNIFIED_IDEOGRAPH },
    { UCHAR_DEPRECATED,                     1, UPROPS_DEPRECATED },
    { UCHAR_LOGICAL_ORDER_EXCEPTION,        1, UPROPS_LOGICAL_ORDER_EXCEPTION },
    { UCHAR_S_TERM,                         1, UPROPS_S_TERM },
    { UCHAR_VARIATION_SELECTOR,             1, UPROPS_VARIATION_SELECTOR },
    // Note: Pattern_Syntax & Pattern_White_Space are available via
    // the internal PatternProps class and need not be stored here any more.
    { UCHAR_PATTERN_SYNTAX,                 1, UPROPS_PATTERN_SYNTAX },
    { UCHAR_PATTERN_WHITE_SPACE,            1, UPROPS_PATTERN_WHITE_SPACE },
    { UCHAR_XID_START,                      1, UPROPS_XID_START },
    { UCHAR_XID_CONTINUE,                   1, UPROPS_XID_CONTINUE },
    { UCHAR_MATH,                           1, UPROPS_MATH },
    { UCHAR_ALPHABETIC,                     1, UPROPS_ALPHABETIC },
    { UCHAR_GRAPHEME_EXTEND,                1, UPROPS_GRAPHEME_EXTEND },
    { UCHAR_DEFAULT_IGNORABLE_CODE_POINT,   1, UPROPS_DEFAULT_IGNORABLE_CODE_POINT },
    { UCHAR_ID_START,                       1, UPROPS_ID_START },
    { UCHAR_ID_CONTINUE,                    1, UPROPS_ID_CONTINUE },
    { UCHAR_GRAPHEME_BASE,                  1, UPROPS_GRAPHEME_BASE },
};

struct PropToEnum {
    int32_t prop;  // UProperty
    int32_t vecWord, vecShift;
    uint32_t vecMask;
};

static const PropToEnum
propToEnums[]={
    // Use UPROPS_SCRIPT_X_MASK not UPROPS_SCRIPT_MASK:
    // When writing a Script code, remove Script_Extensions bits as well.
    // If needed, they will get written again.
    { UCHAR_SCRIPT,                     0, 0, UPROPS_SCRIPT_X_MASK },
    { UCHAR_BLOCK,                      0, UPROPS_BLOCK_SHIFT, UPROPS_BLOCK_MASK },
    { UCHAR_EAST_ASIAN_WIDTH,           0, UPROPS_EA_SHIFT, UPROPS_EA_MASK },
    { UCHAR_DECOMPOSITION_TYPE,         2, 0, UPROPS_DT_MASK },
    { UCHAR_GRAPHEME_CLUSTER_BREAK,     2, UPROPS_GCB_SHIFT, UPROPS_GCB_MASK },
    { UCHAR_WORD_BREAK,                 2, UPROPS_WB_SHIFT, UPROPS_WB_MASK },
    { UCHAR_SENTENCE_BREAK,             2, UPROPS_SB_SHIFT, UPROPS_SB_MASK },
    { UCHAR_LINE_BREAK,                 2, UPROPS_LB_SHIFT, UPROPS_LB_MASK },
};

void
Props2Writer::setProps(const UniProps &props, const UnicodeSet &newValues, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }
    UChar32 start=props.start;
    UChar32 end=props.end;
    if(start==0 && end==0x10ffff) {
        // Also set bits for initialValue and errorValue.
        end=UPVEC_MAX_CP;
    }
    if(newValues.containsSome(0, UCHAR_BINARY_LIMIT-1)) {
        for(int32_t i=0; i<LENGTHOF(propToBinaries); ++i) {
            const PropToBinary &p2b=propToBinaries[i];
            U_ASSERT(p2b.vecShift<32);
            if(newValues.contains(p2b.prop)) {
                uint32_t mask=U_MASK(p2b.vecShift);
                uint32_t value= props.binProps[p2b.prop] ? mask : 0;
                upvec_setValue(pv, start, end, p2b.vecWord, value, mask, &errorCode);
            }
        }
    }
    if(newValues.containsSome(UCHAR_INT_START, UCHAR_INT_LIMIT-1)) {
        for(int32_t i=0; i<LENGTHOF(propToEnums); ++i) {
            const PropToEnum &p2e=propToEnums[i];
            U_ASSERT(p2e.vecShift<32);
            if(newValues.contains(p2e.prop)) {
                uint32_t mask=p2e.vecMask;
                uint32_t value=(uint32_t)(props.getIntProp(p2e.prop)<<p2e.vecShift);
                U_ASSERT((value&mask)==value);
                upvec_setValue(pv, start, end, p2e.vecWord, value, mask, &errorCode);
            }
        }
    }
    if(newValues.contains(UCHAR_AGE)) {
        if(props.age[0]>15 || props.age[1]>15 || props.age[2]!=0 || props.age[3]!=0) {
            char buffer[U_MAX_VERSION_STRING_LENGTH];
            u_versionToString(props.age, buffer);
            fprintf(stderr, "genprops error: age %s cannot be encoded\n", buffer);
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        uint32_t version=(props.age[0]<<4)|props.age[1];
        upvec_setValue(pv, start, end,
                       0, version<<UPROPS_AGE_SHIFT, UPROPS_AGE_MASK,
                       &errorCode);
    }
    // Write a new (Script, Script_Extensions) value if there are Script_Extensions
    // and either Script or Script_Extensions are new on the current line.
    // (If only Script is new, then it just clobbered the relevant bits.)
    if( !props.scx.isEmpty() &&
        (newValues.contains(UCHAR_SCRIPT) || newValues.contains(UCHAR_SCRIPT_EXTENSIONS))
    ) {
        UnicodeString codes;  // vector of 16-bit UScriptCode values
        UnicodeSetIterator iter(props.scx);
        while(iter.next()) { codes.append((UChar)iter.getCodepoint()); }

        // Set bit 15 on the last script code, for termination.
        int32_t length=codes.length();
        codes.setCharAt(length-1, (UChar)(codes[length-1]|0x8000));
        // Find this list of codes in the Script_Extensions data so far, or add this list.
        int32_t index=scriptExtensions->indexOf(codes);
        if(index<0) {
            index=scriptExtensions->length();
            scriptExtensions->append(codes);
        }

        // Encode the (Script, Script_Extensions index) pair.
        int32_t script=props.getIntProp(UCHAR_SCRIPT);
        uint32_t scriptX;
        if(script==USCRIPT_COMMON) {
            scriptX=UPROPS_SCRIPT_X_WITH_COMMON|(uint32_t)index;
        } else if(script==USCRIPT_INHERITED) {
            scriptX=UPROPS_SCRIPT_X_WITH_INHERITED|(uint32_t)index;
        } else {
            // Store an additional pair of 16-bit units for an unusual main Script code
            // together with the Script_Extensions index.
            UnicodeString codeIndexPair;
            codeIndexPair.append((UChar)script).append((UChar)index);
            index=scriptExtensions->indexOf(codeIndexPair);
            if(index<0) {
                index=scriptExtensions->length();
                scriptExtensions->append(codeIndexPair);
            }
            scriptX=UPROPS_SCRIPT_X_WITH_OTHER|(uint32_t)index;
        }
        if(index>UPROPS_SCRIPT_MASK) {
            fprintf(stderr, "genprops: Script_Extensions indexes overflow bit field\n");
            errorCode=U_BUFFER_OVERFLOW_ERROR;
            return;
        }
        upvec_setValue(pv, start, end, 0, scriptX, UPROPS_SCRIPT_X_MASK, &errorCode);
    }
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to set props2 values for %04lX..%04lX: %s\n",
                (long)start, (long)end, u_errorName(errorCode));
    }
}

static uint8_t trieBlock[100000];
static int32_t trieSize;

int32_t
props2FinalizeData(int32_t indexes[UPROPS_INDEX_COUNT], UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return 0; }

    newTrie=upvec_compactToUTrie2WithRowIndexes(pv, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to build trie for additional properties: %s\n",
                u_errorName(errorCode));
        return 0;
    }

    trieSize=utrie2_serialize(newTrie, trieBlock, (int32_t)sizeof(trieBlock), &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops error: utrie2_freeze(additional properties)+utrie2_serialize() failed: %s\n",
                u_errorName(errorCode));
        return 0;
    }

    int32_t pvRows;
    const uint32_t *pvArray=upvec_getArray(pv, &pvRows, NULL);
    int32_t pvCount=pvRows*UPROPS_VECTOR_WORDS;

    /* round up scriptExtensions to multiple of 4 bytes */
    if(scriptExtensions->length()&1) {
        scriptExtensions->append((UChar)0);
    }

    /* set indexes */
    indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]=
        indexes[UPROPS_ADDITIONAL_TRIE_INDEX]+trieSize/4;
    indexes[UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX]=UPROPS_VECTOR_WORDS;
    indexes[UPROPS_SCRIPT_EXTENSIONS_INDEX]=
        indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]+pvCount;
    indexes[UPROPS_RESERVED_INDEX_7]=
        indexes[UPROPS_SCRIPT_EXTENSIONS_INDEX]+scriptExtensions->length()/2;
    indexes[UPROPS_RESERVED_INDEX_8]=indexes[UPROPS_RESERVED_INDEX_7];
    indexes[UPROPS_DATA_TOP_INDEX]=indexes[UPROPS_RESERVED_INDEX_8];

    indexes[UPROPS_MAX_VALUES_INDEX]=
        (((int32_t)U_EA_COUNT-1)<<UPROPS_EA_SHIFT)|
        (((int32_t)UBLOCK_COUNT-1)<<UPROPS_BLOCK_SHIFT)|
        (((int32_t)USCRIPT_CODE_LIMIT-1)&UPROPS_SCRIPT_MASK);
    indexes[UPROPS_MAX_VALUES_2_INDEX]=
        (((int32_t)U_LB_COUNT-1)<<UPROPS_LB_SHIFT)|
        (((int32_t)U_SB_COUNT-1)<<UPROPS_SB_SHIFT)|
        (((int32_t)U_WB_COUNT-1)<<UPROPS_WB_SHIFT)|
        (((int32_t)U_GCB_COUNT-1)<<UPROPS_GCB_SHIFT)|
        ((int32_t)U_DT_COUNT-1);

    if(beVerbose) {
        printf("size in bytes of additional props trie:%5u\n", (int)trieSize);
        printf("number of additional props vectors:    %5u\n", (int)pvRows);
        printf("number of 32-bit words per vector:     %5u\n", UPROPS_VECTOR_WORDS);
        printf("number of 16-bit scriptExtensions:     %5u\n", (int)scriptExtensions->length());
    }

    return 4*(indexes[UPROPS_DATA_TOP_INDEX]-indexes[UPROPS_ADDITIONAL_TRIE_INDEX]);
}

void
props2AppendToCSourceFile(FILE *f, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    int32_t pvRows;
    const uint32_t *pvArray=upvec_getArray(pv, &pvRows, NULL);
    int32_t pvCount=pvRows*UPROPS_VECTOR_WORDS;

    usrc_writeUTrie2Arrays(f,
        "static const uint16_t propsVectorsTrie_index[%ld]={\n", NULL,
        newTrie,
        "\n};\n\n");
    usrc_writeUTrie2Struct(f,
        "static const UTrie2 propsVectorsTrie={\n",
        newTrie, "propsVectorsTrie_index", NULL,
        "};\n\n");

    usrc_writeArray(f,
        "static const uint32_t propsVectors[%ld]={\n",
        pvArray, 32, pvCount,
        "};\n\n");
    fprintf(f, "static const int32_t countPropsVectors=%ld;\n", (long)pvCount);
    fprintf(f, "static const int32_t propsVectorsColumns=%ld;\n", (long)UPROPS_VECTOR_WORDS);

    usrc_writeArray(f,
        "static const uint16_t scriptExtensions[%ld]={\n",
        scriptExtensions->getBuffer(), 16, scriptExtensions->length(),
        "};\n\n");
}

void
props2AppendToBinaryFile(UNewDataMemory *pData, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return; }

    int32_t pvRows;
    const uint32_t *pvArray=upvec_getArray(pv, &pvRows, NULL);
    int32_t pvCount=pvRows*UPROPS_VECTOR_WORDS;

    udata_writeBlock(pData, trieBlock, trieSize);
    udata_writeBlock(pData, pvArray, pvCount*4);
    udata_writeBlock(pData, scriptExtensions->getBuffer(), scriptExtensions->length()*2);
}

PropsWriter *
createProps2Writer(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    PropsWriter *pw=new Props2Writer(errorCode);
    if(pw==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pw;
}
