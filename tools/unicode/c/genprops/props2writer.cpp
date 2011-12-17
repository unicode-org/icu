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
#include "unicode/unistr.h"
#include "unicode/uscript.h"
#include "cstring.h"
#include "cmemory.h"
#include "utrie2.h"
#include "uprops.h"
#include "propsvec.h"
#include "uassert.h"
#include "uparse.h"
#include "writesrc.h"
#include "genprops.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

U_NAMESPACE_USE

/* data --------------------------------------------------------------------- */

static UTrie2 *newTrie;
static UPropsVectors *pv;

static UnicodeString *scriptExtensions;

/* miscellaneous ------------------------------------------------------------ */

static char *
trimTerminateField(char *s, char *limit) {
    /* trim leading whitespace */
    s=(char *)u_skipWhitespace(s);

    /* trim trailing whitespace */
    while(s<limit && U_IS_INV_WHITESPACE(*(limit-1))) {
        --limit;
    }
    *limit=0;

    return s;
}

static void
parseTwoFieldFile(char *filename, char *basename,
                  const char *ucdFile, const char *suffix,
                  UParseLineFn *lineFn,
                  UErrorCode *pErrorCode) {
    char *fields[2][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    writeUCDFilename(basename, ucdFile, suffix);

    u_parseDelimitedFile(filename, ';', fields, 2, lineFn, NULL, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "error parsing %s.txt: %s\n", ucdFile, u_errorName(*pErrorCode));
    }
}

static void U_CALLCONV
scriptExtensionsLineFn(void *context,
                       char *fields[][2], int32_t fieldCount,
                       UErrorCode *pErrorCode);

static void
parseMultiFieldFile(char *filename, char *basename,
                    const char *ucdFile, const char *suffix,
                    int32_t fieldCount,
                    UParseLineFn *lineFn,
                    UErrorCode *pErrorCode) {
    char *fields[20][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    writeUCDFilename(basename, ucdFile, suffix);

    u_parseDelimitedFile(filename, ';', fields, fieldCount, lineFn, NULL, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "error parsing %s.txt: %s\n", ucdFile, u_errorName(*pErrorCode));
    }
}

static void U_CALLCONV
numericLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode);

/* parse files with single enumerated properties ---------------------------- */

struct SingleEnum {
    const char *ucdFile, *propName;
    UProperty prop;
    int32_t vecWord, vecShift;
    uint32_t vecMask;
};
typedef struct SingleEnum SingleEnum;

static void
parseSingleEnumFile(char *filename, char *basename, const char *suffix,
                    const SingleEnum *sen,
                    UErrorCode *pErrorCode);

static const SingleEnum scriptSingleEnum={ 
    "Scripts", "script", 
    UCHAR_SCRIPT, 
    0, 0, UPROPS_SCRIPT_MASK
};

static const SingleEnum blockSingleEnum={
    "Blocks", "block",
    UCHAR_BLOCK,
    0, UPROPS_BLOCK_SHIFT, UPROPS_BLOCK_MASK
};

static const SingleEnum graphemeClusterBreakSingleEnum={
    "GraphemeBreakProperty", "Grapheme_Cluster_Break",
    UCHAR_GRAPHEME_CLUSTER_BREAK,
    2, UPROPS_GCB_SHIFT, UPROPS_GCB_MASK
};

static const SingleEnum wordBreakSingleEnum={
    "WordBreakProperty", "Word_Break",
    UCHAR_WORD_BREAK,
    2, UPROPS_WB_SHIFT, UPROPS_WB_MASK
};

static const SingleEnum sentenceBreakSingleEnum={
    "SentenceBreakProperty", "Sentence_Break",
    UCHAR_SENTENCE_BREAK,
    2, UPROPS_SB_SHIFT, UPROPS_SB_MASK
};

static const SingleEnum lineBreakSingleEnum={
    "LineBreak", "line break",
    UCHAR_LINE_BREAK,
    2, UPROPS_LB_SHIFT, UPROPS_LB_MASK
};

static const SingleEnum eawSingleEnum={
    "EastAsianWidth", "east asian width",
    UCHAR_EAST_ASIAN_WIDTH,
    0, UPROPS_EA_SHIFT, UPROPS_EA_MASK
};

static void U_CALLCONV
singleEnumLineFn(void *context,
                 char *fields[][2], int32_t fieldCount,
                 UErrorCode *pErrorCode) {
    const SingleEnum *sen;
    char *s;
    uint32_t start, end, uv;
    int32_t value;

    sen=(const SingleEnum *)context;

    u_parseCodePointRange(fields[0][0], &start, &end, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in %s.txt field 0 at %s\n", sen->ucdFile, fields[0][0]);
        exit(*pErrorCode);
    }

    /* parse property alias */
    s=trimTerminateField(fields[1][0], fields[1][1]);
    value=u_getPropertyValueEnum(sen->prop, s);
    if(value<0) {
        if(sen->prop==UCHAR_BLOCK) {
            if(isToken("Greek", s)) {
                value=UBLOCK_GREEK; /* Unicode 3.2 renames this to "Greek and Coptic" */
            } else if(isToken("Combining Marks for Symbols", s)) {
                value=UBLOCK_COMBINING_MARKS_FOR_SYMBOLS; /* Unicode 3.2 renames this to "Combining Diacritical Marks for Symbols" */
            } else if(isToken("Private Use", s)) {
                value=UBLOCK_PRIVATE_USE; /* Unicode 3.2 renames this to "Private Use Area" */
            }
        }
    }
    if(value<0) {
        fprintf(stderr, "genprops error: unknown %s name in %s.txt field 1 at %s\n",
                        sen->propName, sen->ucdFile, s);
        exit(U_PARSE_ERROR);
    }

    uv=(uint32_t)(value<<sen->vecShift);
    if((uv&sen->vecMask)!=uv) {
        fprintf(stderr, "genprops error: %s value overflow (0x%x) at %s\n",
                        sen->propName, (int)uv, s);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }

    if(start==0 && end==0x10ffff) {
        /* Also set bits for initialValue and errorValue. */
        end=UPVEC_MAX_CP;
    }
    upvec_setValue(pv, start, end, sen->vecWord, uv, sen->vecMask, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set %s code: %s\n",
                        sen->propName, u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

static void
parseSingleEnumFile(char *filename, char *basename, const char *suffix,
                    const SingleEnum *sen,
                    UErrorCode *pErrorCode) {
    char *fields[2][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    writeUCDFilename(basename, sen->ucdFile, suffix);

    u_parseDelimitedFile(filename, ';', fields, 2, singleEnumLineFn, (void *)sen, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "error parsing %s.txt: %s\n", sen->ucdFile, u_errorName(*pErrorCode));
    }
}

/* -------------------------------------------------------------------------- */

static void
initAdditionalProperties() {
    UErrorCode errorCode=U_ZERO_ERROR;
    pv=upvec_open(UPROPS_VECTOR_WORDS, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: props2writer upvec_open() failed - %s\n",
                u_errorName(errorCode));
        exit(errorCode);
    }
    scriptExtensions=new UnicodeString;
}

static void
exitAdditionalProperties() {
    utrie2_close(newTrie);
    upvec_close(pv);
    delete scriptExtensions;
}

U_CFUNC void
generateAdditionalProperties(char *filename, const char *suffix, UErrorCode *pErrorCode) {
    char *basename;

    basename=filename+uprv_strlen(filename);

    /* process various UCD .txt files */

    /* add Han numeric types & values */
    parseMultiFieldFile(filename, basename, "DerivedNumericValues", suffix, 2, numericLineFn, pErrorCode);

    parseSingleEnumFile(filename, basename, suffix, &scriptSingleEnum, pErrorCode);

    parseTwoFieldFile(filename, basename, "ScriptExtensions", suffix, scriptExtensionsLineFn, pErrorCode);

    parseSingleEnumFile(filename, basename, suffix, &blockSingleEnum, pErrorCode);

    parseSingleEnumFile(filename, basename, suffix, &graphemeClusterBreakSingleEnum, pErrorCode);

    parseSingleEnumFile(filename, basename, suffix, &wordBreakSingleEnum, pErrorCode);

    parseSingleEnumFile(filename, basename, suffix, &sentenceBreakSingleEnum, pErrorCode);

    /*
     * LineBreak-4.0.0.txt:
     *  - All code points, assigned and unassigned, that are not listed 
     *         explicitly are given the value "XX".
     *
     * XX==U_LB_UNKNOWN==0 - nothing to do
     */
    parseSingleEnumFile(filename, basename, suffix, &lineBreakSingleEnum, pErrorCode);

    /*
     * Preset East Asian Width defaults:
     *
     * http://www.unicode.org/reports/tr11/#Unassigned
     * 7.1 Unassigned and Private Use characters
     *
     * All unassigned characters are by default classified as non-East Asian neutral,
     * except for the range U+20000 to U+2FFFD,
     * since all code positions from U+20000 to U+2FFFD are intended for CJK ideographs (W).
     * All Private use characters are by default classified as ambiguous,
     * since their definition depends on context.
     *
     * N for all ==0 - nothing to do
     * A for Private Use
     * W for plane 2
     */
    *pErrorCode=U_ZERO_ERROR;
    upvec_setValue(pv, 0xe000, 0xf8ff, 0, (uint32_t)(U_EA_AMBIGUOUS<<UPROPS_EA_SHIFT), UPROPS_EA_MASK, pErrorCode);
    upvec_setValue(pv, 0xf0000, 0xffffd, 0, (uint32_t)(U_EA_AMBIGUOUS<<UPROPS_EA_SHIFT), UPROPS_EA_MASK, pErrorCode);
    upvec_setValue(pv, 0x100000, 0x10fffd, 0, (uint32_t)(U_EA_AMBIGUOUS<<UPROPS_EA_SHIFT), UPROPS_EA_MASK, pErrorCode);
    upvec_setValue(pv, 0x20000, 0x2fffd, 0, (uint32_t)(U_EA_WIDE<<UPROPS_EA_SHIFT), UPROPS_EA_MASK, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: unable to set default East Asian Widths: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }

    /* parse EastAsianWidth.txt */
    parseSingleEnumFile(filename, basename, suffix, &eawSingleEnum, pErrorCode);

    newTrie=upvec_compactToUTrie2WithRowIndexes(pv, pErrorCode);
// TODO: remove
#if 0
const uint32_t *pvArray;
int32_t pvRows;
pvArray=upvec_getArray(pv, &pvRows, NULL);
for(int32_t c=0; c<=0x10ffff; ++c) {
  uint16_t ri=utrie2_get32(newTrie, c);
  uint32_t v2=pvArray[ri+2];
  int32_t dt=v2&UPROPS_DT_MASK;
  if(dt!=0) {
    printf("%04x %d\n", c, dt);
  }
}
#endif
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops error: unable to build trie for additional properties: %s\n",
                u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* ScriptExtensions.txt ----------------------------------------------------- */

static void U_CALLCONV
scriptExtensionsLineFn(void *context,
                       char *fields[][2], int32_t fieldCount,
                       UErrorCode *pErrorCode) {
    uint32_t start, end;
    u_parseCodePointRange(fields[0][0], &start, &end, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in ScriptExtensions.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }

    /* ignore "<script>" on the @missing line */
    if(*u_skipWhitespace(fields[1][0])=='<') {
        return;
    }

    /* parse list of script codes */
    UnicodeString codes;  // vector of 16-bit UScriptCode values
    char *s=fields[1][0];
    for(;;) {
        // skip whitespace before each token
        s=(char *)u_skipWhitespace(s);
        if(*s==0 || *s==';') {
            break;
        }
        // skip non-whitespace, non-terminator characters to find the token limit
        char *limit=s;
        char c;
        do {
            c=*++limit;
        } while(!U_IS_INV_WHITESPACE(c) && c!=0 && c!=';');
        // NUL-terminated this token
        *limit=0;
        // convert the token (script property value alias) into a UScriptCode value
        int32_t value=u_getPropertyValueEnum(UCHAR_SCRIPT, s);
        if(value<0) {
            fprintf(stderr, "genprops: syntax error in ScriptExtensions.txt field 1 at %s\n", s);
            exit(U_INVALID_FORMAT_ERROR);
        }
        // Insertion sort into the list of script codes.
        for(int32_t i=0;; ++i) {
            if(i<codes.length()) {
                if(value<codes[i]) {
                    codes.insert(i, (UChar)value);
                    break;
                } else if(value==codes[i]) {
                    fprintf(stderr,
                            "genprops: duplicate script code in ScriptExtensions.txt field 1 at %s "
                            "for U+%04lx..U+%04lx\n",
                            s, (long)start, (long)end);
                    exit(U_INVALID_FORMAT_ERROR);
                }
                // continue while value>codes[i]
            } else {
                codes.append((UChar)value);
                break;
            }
        }
        if(c==0 || c==';') {
            // the token ended at a terminator
            break;
        } else {
            // the token ended at U_IS_INV_WHITESPACE(c), continue after c
            s=limit+1;
        }
    }
    int32_t length=codes.length();
    if(length==0) {
        fprintf(stderr,
                "genprops: missing values in ScriptExtensions.txt field 1 "
                "for U+%04lx..U+%04lx\n",
                (long)start, (long)end);
        exit(U_INVALID_FORMAT_ERROR);
    }
    // Set bit 15 on the last script code, for termination.
    codes.setCharAt(length-1, (UChar)(codes[length-1]|0x8000));
    // Find this list of codes in the Script_Extensions data so far, or add this list.
    int32_t index=scriptExtensions->indexOf(codes);
    if(index<0) {
        index=scriptExtensions->length();
        scriptExtensions->append(codes);
    }
    // Modify the Script data for each of the start..end code points
    // to include the Script_Extensions index.
    do {
        uint32_t scriptX=upvec_getValue(pv, (UChar32)start, 0)&UPROPS_SCRIPT_X_MASK;
        // Find the next code point that has a different script value.
        // We want to add the Script_Extensions index to the code point range start..next-1.
        UChar32 next;
        for(next=(UChar32)start+1;
            next<=(UChar32)end && scriptX==(upvec_getValue(pv, next, 0)&UPROPS_SCRIPT_X_MASK);
            ++next) {}
        if(scriptX>=UPROPS_SCRIPT_X_WITH_COMMON) {
            fprintf(stderr,
                    "genprops: ScriptExtensions.txt has values for U+%04lx..U+%04lx "
                    "which overlaps with a range including U+%04lx..U+%04lx\n",
                    (long)start, (long)end, (long)start, (long)(next-1));
            exit(U_INVALID_FORMAT_ERROR);
        }
        // Encode the (Script, Script_Extensions index) pair.
        if(scriptX==USCRIPT_COMMON) {
            scriptX=UPROPS_SCRIPT_X_WITH_COMMON|(uint32_t)index;
        } else if(scriptX==USCRIPT_INHERITED) {
            scriptX=UPROPS_SCRIPT_X_WITH_INHERITED|(uint32_t)index;
        } else {
            // Store an additional pair of 16-bit units for an unusual main Script code
            // together with the Script_Extensions index.
            UnicodeString codeIndexPair;
            codeIndexPair.append((UChar)scriptX).append((UChar)index);
            index=scriptExtensions->indexOf(codeIndexPair);
            if(index<0) {
                index=scriptExtensions->length();
                scriptExtensions->append(codeIndexPair);
            }
            scriptX=UPROPS_SCRIPT_X_WITH_OTHER|(uint32_t)index;
        }
        if(index>UPROPS_SCRIPT_MASK) {
            fprintf(stderr, "genprops: Script_Extensions indexes overflow bit field\n");
            exit(U_BUFFER_OVERFLOW_ERROR);
        }
        // Write the (Script, Script_Extensions index) pair into
        // the properties vector for start..next-1.
        upvec_setValue(pv, (UChar32)start, (UChar32)(next-1),
                        0, scriptX, UPROPS_SCRIPT_X_MASK, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            fprintf(stderr, "genprops error: unable to set Script_Extensions: %s\n", u_errorName(*pErrorCode));
            exit(*pErrorCode);
        }
        start=next;
    } while(start<=end);
}

/* DerivedNumericValues.txt ------------------------------------------------- */

static void U_CALLCONV
numericLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode) {
    Props newProps={ 0 };
    char *s, *numberLimit;
    uint32_t start, end, value, oldProps32;
    char c;
    UBool isFraction;

    /* get the code point range */
    u_parseCodePointRange(fields[0][0], &start, &end, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in DerivedNumericValues.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }

    /*
     * Ignore the
     * # @missing: 0000..10FFFF; NaN
     * line from Unicode 5.1's DerivedNumericValues.txt:
     * The following code cannot parse "NaN", and we don't want to overwrite
     * the numeric values for all characters after reading most
     * from UnicodeData.txt already.
     */
    if(start==0 && end==0x10ffff) {
        return;
    }

    /* check if the numeric value is a fraction (this code does not handle any) */
    isFraction=FALSE;
    s=uprv_strchr(fields[1][0], '.');
    if(s!=NULL) {
        numberLimit=s+1;
        while('0'<=(c=*numberLimit++) && c<='9') {
            if(c!='0') {
                isFraction=TRUE;
                break;
            }
        }
    }

    if(isFraction) {
        value=0;
    } else {
        /* parse numeric value */
        s=(char *)u_skipWhitespace(fields[1][0]);

        /* try large, single-significant-digit numbers, may otherwise overflow strtoul() */
        if('1'<=s[0] && s[0]<='9' && s[1]=='0' && s[2]=='0') {
            /* large integers are encoded in a special way, see store.c */
            uint8_t exp=0;

            value=s[0]-'0';
            numberLimit=s;
            while(*(++numberLimit)=='0') {
                ++exp;
            }
            newProps.exponent=exp;
        } else {
            /* normal number parsing */
            value=(uint32_t)uprv_strtoul(s, &numberLimit, 10);
        }
        if(numberLimit<=s || (*numberLimit!='.' && u_skipWhitespace(numberLimit)!=fields[1][1]) || value>=0x80000000) {
            fprintf(stderr, "genprops: syntax error in DerivedNumericValues.txt field 1 at %s\n", fields[0][0]);
            exit(U_PARSE_ERROR);
        }
    }

    /*
     * Unicode 4.0.1 removes the third column that used to list the numeric type.
     * Assume that either the data is the same as in UnicodeData.txt,
     * or else that the numeric type is "numeric".
     * This should work because we only expect to add numeric values for
     * Han characters; for those, UnicodeData.txt lists only ranges without
     * specific properties for single characters.
     */

    /* set the new numeric value */
    newProps.code=start;
    newProps.numericValue=(int32_t)value;       /* newly parsed numeric value */
    /* the exponent may have been set above */

    for(; start<=end; ++start) {
        uint32_t newProps32;
        int32_t oldNtv;
        oldProps32=getProps(start);
        oldNtv=(int32_t)GET_NUMERIC_TYPE_VALUE(oldProps32);

        if(isFraction) {
            if(UPROPS_NTV_FRACTION_START<=oldNtv && oldNtv<UPROPS_NTV_LARGE_START) {
                /* this code point was already listed with its numeric value in UnicodeData.txt */
                continue;
            } else {
                fprintf(stderr, "genprops: not prepared for new fractions in DerivedNumericValues.txt field 1 at %s\n", fields[1][0]);
                exit(U_PARSE_ERROR);
            }
        }

        /*
         * For simplicity, and because we only expect to set numeric values for Han characters,
         * for now we only allow to set these values for Lo characters.
         */
        if(oldNtv==UPROPS_NTV_NONE && GET_CATEGORY(oldProps32)!=U_OTHER_LETTER) {
            fprintf(stderr, "genprops error: new numeric value for a character other than Lo in DerivedNumericValues.txt at %s\n", fields[0][0]);
            exit(U_PARSE_ERROR);
        }

        /* verify that we do not change an existing value (fractions were excluded above) */
        if(oldNtv!=UPROPS_NTV_NONE) {
            /* the code point already has a value stored */
            newProps.numericType=UPROPS_NTV_GET_TYPE(oldNtv);
            newProps32=makeProps(&newProps);
            if(oldNtv!=GET_NUMERIC_TYPE_VALUE(newProps32)) {
                fprintf(stderr, "genprops error: new numeric value differs from old one for U+%04lx\n", (long)start);
                exit(U_PARSE_ERROR);
            }
            /* same value, continue */
        } else {
            /* the code point is getting a new numeric value */
            newProps.numericType=(uint8_t)U_NT_NUMERIC; /* assumed numeric type, see Unicode 4.0.1 comment */
            newProps32=makeProps(&newProps);
            if(beVerbose) {
                printf("adding U+%04x numeric type %d encoded-numeric-type-value 0x%03x from %s\n",
                       (int)start, U_NT_NUMERIC, (int)GET_NUMERIC_TYPE_VALUE(newProps32), fields[0][0]);
            }

            addProps(start, newProps32|GET_CATEGORY(oldProps32));
        }
    }
}

/* data serialization ------------------------------------------------------- */

U_CFUNC int32_t
writeAdditionalData(FILE *f, uint8_t *p, int32_t capacity, int32_t indexes[UPROPS_INDEX_COUNT]) {
    const uint32_t *pvArray;
    int32_t pvRows, pvCount;
    int32_t length;
    UErrorCode errorCode;

    pvArray=upvec_getArray(pv, &pvRows, NULL);
    pvCount=pvRows*UPROPS_VECTOR_WORDS;

    errorCode=U_ZERO_ERROR;
    length=utrie2_serialize(newTrie, p, capacity, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr,
                "genprops error: utrie2_freeze(additional properties)+utrie2_serialize() failed: %s\n",
                u_errorName(errorCode));
        exit(errorCode);
    }

    /* round up scriptExtensions to multiple of 4 bytes */
    if(scriptExtensions->length()&1) {
        scriptExtensions->append((UChar)0);
    }

    /* set indexes */
    indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]=
        indexes[UPROPS_ADDITIONAL_TRIE_INDEX]+length/4;
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

    int32_t additionalPropsSize=4*(indexes[UPROPS_DATA_TOP_INDEX]-indexes[UPROPS_ADDITIONAL_TRIE_INDEX]);
    if(p!=NULL && additionalPropsSize<=capacity) {
        if(beVerbose) {
            printf("size in bytes of additional props trie:%5u\n", (int)length);
        }
        if(f!=NULL) {
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
            fprintf(f, "static const int32_t propsVectorsColumns=%ld;\n", (long)indexes[UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX]);

            usrc_writeArray(f,
                "static const uint16_t scriptExtensions[%ld]={\n",
                scriptExtensions->getBuffer(), 16, scriptExtensions->length(),
                "};\n\n");
        } else {
            p+=length;
            length=pvCount*4;
            uprv_memcpy(p, pvArray, length);

            p+=length;
            length=scriptExtensions->length()*2;
            uprv_memcpy(p, scriptExtensions->getBuffer(), length);
        }
        if(beVerbose) {
            printf("number of additional props vectors:    %5u\n", (int)pvRows);
            printf("number of 32-bit words per vector:     %5u\n", UPROPS_VECTOR_WORDS);
            printf("number of 16-bit scriptExtensions:     %5u\n", (int)scriptExtensions->length());
        }
    }

    return additionalPropsSize;
}

class Props2Writer : public PropsWriter {
public:
    Props2Writer() { initAdditionalProperties(); }
    virtual ~Props2Writer() { exitAdditionalProperties(); }

    virtual void setProps(const UniProps &, const UnicodeSet &newValues, UErrorCode &errorCode);
};

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
    // Note: The Grapheme_Link property is deprecated since Unicode 5.0.
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
    if(newValues.contains(UCHAR_DECOMPOSITION_TYPE)) {
        upvec_setValue(pv, start, end,
                       2, (uint32_t)props.getIntProp(UCHAR_DECOMPOSITION_TYPE), UPROPS_DT_MASK,
                       &errorCode);
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
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to set values for %04lX..%04lX: %s\n",
                (long)start, (long)end, u_errorName(errorCode));
    }
}

PropsWriter *
createProps2Writer(UErrorCode &errorCode) {
    if(U_FAILURE(errorCode)) { return NULL; }
    PropsWriter *pw=new Props2Writer();
    if(pw==NULL) {
        errorCode=U_MEMORY_ALLOCATION_ERROR;
    }
    return pw;
}
