/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  props2.c
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
#include "unicode/uscript.h"
#include "cstring.h"
#include "cmemory.h"
#include "utrie.h"
#include "uprops.h"
#include "propsvec.h"
#include "uparse.h"
#include "genprops.h"

#define FLAG(n) ((uint32_t)1<<(n))

/* data --------------------------------------------------------------------- */

static UNewTrie *trie;
static uint32_t *pv;
static int32_t pvCount;

static uint32_t prevStart=0, prevLimit=0, prevValue=0;

/* prototypes --------------------------------------------------------------- */

static void
parseTwoFieldFile(char *filename, char *basename,
                  const char *ucdFile, const char *suffix,
                  UParseLineFn *lineFn,
                  UErrorCode *pErrorCode);

static void
ageLineFn(void *context,
          char *fields[][2], int32_t fieldCount,
          UErrorCode *pErrorCode);

static void
scriptsLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode);

static void
blocksLineFn(void *context,
             char *fields[][2], int32_t fieldCount,
             UErrorCode *pErrorCode);

static void
propListLineFn(void *context,
               char *fields[][2], int32_t fieldCount,
               UErrorCode *pErrorCode);

static void
derivedPropListLineFn(void *context,
                      char *fields[][2], int32_t fieldCount,
                      UErrorCode *pErrorCode);

static void
eaWidthLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode);

/* -------------------------------------------------------------------------- */

U_CFUNC void
initAdditionalProperties() {
    pv=upvec_open(UPROPS_VECTOR_WORDS, 20000);
}

U_CFUNC void
setMainProperties(uint32_t start, uint32_t limit, uint32_t value) {
#if 0
    /* ### TODO: remove this function */
    UErrorCode errorCode=U_ZERO_ERROR;

    if(!upvec_setValue(pv, start, limit, 2, value, 0xffffffff, &errorCode)) {
        fprintf(stderr, "genprops: unable to set main properties: %s\n", u_errorName(errorCode));
        exit(errorCode);
    }
#endif
}

U_CFUNC void
generateAdditionalProperties(char *filename, const char *suffix, UErrorCode *pErrorCode) {
    char *basename;
    UErrorCode errorCode;

    basename=filename+uprv_strlen(filename);

    /* process various UCD .txt files */
    parseTwoFieldFile(filename, basename, "DerivedAge", suffix, ageLineFn, pErrorCode);

    /*
     * UTR 24 says:
     * Section 2:
     *   "Common - For characters that may be used
     *             within multiple scripts,
     *             or any unassigned code points."
     *
     * Section 4:
     *   "The value COMMON is the default value,
     *    given to all code points that are not
     *    explicitly mentioned in the data file."
     */
    if(!upvec_setValue(pv, 0, 0x110000, 0, (uint32_t)USCRIPT_COMMON, UPROPS_SCRIPT_MASK, pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set script code: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
    parseTwoFieldFile(filename, basename, "Scripts", suffix, scriptsLineFn, pErrorCode);

    parseTwoFieldFile(filename, basename, "Blocks", suffix, blocksLineFn, pErrorCode);

    parseTwoFieldFile(filename, basename, "PropList", suffix, propListLineFn, pErrorCode);

    parseTwoFieldFile(filename, basename, "DerivedCoreProperties", suffix, derivedPropListLineFn, pErrorCode);

    /*
     * Preset East Asian Width defaults:
     * N for all
     * A for Private Use
     * W for plane 2
     */
    errorCode=U_ZERO_ERROR;
    if( !upvec_setValue(pv, 0, 0x110000, 0, (uint32_t)(U_EA_NEUTRAL<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode) ||
        !upvec_setValue(pv, 0xe000, 0xf900, 0, (uint32_t)(U_EA_AMBIGUOUS<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode) ||
        !upvec_setValue(pv, 0xf0000, 0xffffe, 0, (uint32_t)(U_EA_AMBIGUOUS<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode) ||
        !upvec_setValue(pv, 0x100000, 0x10fffe, 0, (uint32_t)(U_EA_AMBIGUOUS<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode) ||
        !upvec_setValue(pv, 0x20000, 0x2fffe, 0, (uint32_t)(U_EA_WIDE<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode)
    ) {
        fprintf(stderr, "genprops: unable to set default East Asian Widths: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
    prevStart=prevLimit=prevValue=0;
    /* parse EastAsianWidth.txt */
    parseTwoFieldFile(filename, basename, "EastAsianWidth", suffix, eaWidthLineFn, pErrorCode);
    /* set last range */
    if(!upvec_setValue(pv, prevStart, prevLimit, 0, (uint32_t)(prevValue<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set East Asian Width: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }

    trie=utrie_open(NULL, NULL, 50000, 0, FALSE);
    if(trie==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        upvec_close(pv);
        return;
    }

    pvCount=upvec_toTrie(pv, trie, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops error: unable to build trie for additional properties: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

static void
parseTwoFieldFile(char *filename, char *basename,
                  const char *ucdFile, const char *suffix,
                  UParseLineFn *lineFn,
                  UErrorCode *pErrorCode) {
    char *fields[2][2];

    writeUCDFilename(basename, ucdFile, suffix);

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 2, lineFn, NULL, pErrorCode);
}

/* DerivedAge.txt ----------------------------------------------------------- */

static void
ageLineFn(void *context,
          char *fields[][2], int32_t fieldCount,
          UErrorCode *pErrorCode) {
    char *s, *end;
    uint32_t value, start, limit, version;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in DerivedAge.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse version number */
    s=(char *)u_skipWhitespace(fields[1][0]);
    value=(uint32_t)uprv_strtoul(s, &end, 10);
    if(s==end || value==0 || value>15 || (*end!='.' && *end!=' ' && *end!='\t' && *end!=0)) {
        fprintf(stderr, "genprops: syntax error in DerivedAge.txt field 1 at %s\n", fields[1][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    version=value<<4;

    /* parse minor version number */
    if(*end=='.') {
        s=(char *)u_skipWhitespace(end+1);
        value=(uint32_t)uprv_strtoul(s, &end, 10);
        if(s==end || value>15 || (*end!=' ' && *end!='\t' && *end!=0)) {
            fprintf(stderr, "genprops: syntax error in DerivedAge.txt field 1 at %s\n", fields[1][0]);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
        version|=value;
    }

    if(!upvec_setValue(pv, start, limit, 0, version<<UPROPS_AGE_SHIFT, UPROPS_AGE_MASK, pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set character age: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* Scripts.txt -------------------------------------------------------------- */

static void
scriptsLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode) {
    char *s, *end;
    uint32_t start, limit;
    UScriptCode script;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in Scripts.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse script name */
    s=(char *)u_skipWhitespace(fields[1][0]);

    /* trim trailing whitespace */
    end=fields[1][1];
    while(s<end && (*(end-1)==' ' || *(end-1)=='\t')) {
        --end;
    }
    *end=0;
    if( 1!=uscript_getCode(s, &script, 1, pErrorCode) ||
        U_FAILURE(*pErrorCode) ||
        script<=USCRIPT_INVALID_CODE
    ) {
        fprintf(stderr, "genprops error: unknown script name in Scripts.txt field 1 at %s\n", fields[1][0]);
        if(U_SUCCESS(*pErrorCode)) {
            *pErrorCode=U_PARSE_ERROR;
        }
        exit(*pErrorCode);
    }

    if(!upvec_setValue(pv, start, limit, 0, (uint32_t)script, UPROPS_SCRIPT_MASK, pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set script code: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* Blocks.txt --------------------------------------------------------------- */

/* Blocks.txt block names in the order of the parallel UBlockCode constants */
static const char *const
blockNames[UBLOCK_COUNT]={
    NULL,                                       /* 0 */
    "Basic Latin",
    "Latin-1 Supplement",
    "Latin Extended-A",
    "Latin Extended-B",
    "IPA Extensions",
    "Spacing Modifier Letters",
    "Combining Diacritical Marks",
    "Greek",
    "Cyrillic",
    "Armenian",                                 /* 10 */
    "Hebrew",
    "Arabic",
    "Syriac  ",
    "Thaana",
    "Devanagari",
    "Bengali",
    "Gurmukhi",
    "Gujarati",
    "Oriya",
    "Tamil",                                    /* 20 */
    "Telugu",
    "Kannada",
    "Malayalam",
    "Sinhala",
    "Thai",
    "Lao",
    "Tibetan",
    "Myanmar ",
    "Georgian",
    "Hangul Jamo",                              /* 30 */
    "Ethiopic",
    "Cherokee",
    "Unified Canadian Aboriginal Syllabics",
    "Ogham",
    "Runic",
    "Khmer",
    "Mongolian",
    "Latin Extended Additional",
    "Greek Extended",
    "General Punctuation",                      /* 40 */
    "Superscripts and Subscripts",
    "Currency Symbols",
    "Combining Marks for Symbols",
    "Letterlike Symbols",
    "Number Forms",
    "Arrows",
    "Mathematical Operators",
    "Miscellaneous Technical",
    "Control Pictures",
    "Optical Character Recognition",            /* 50 */
    "Enclosed Alphanumerics",
    "Box Drawing",
    "Block Elements",
    "Geometric Shapes",
    "Miscellaneous Symbols",
    "Dingbats",
    "Braille Patterns",
    "CJK Radicals Supplement",
    "Kangxi Radicals",
    "Ideographic Description Characters",       /* 60 */
    "CJK Symbols and Punctuation",
    "Hiragana",
    "Katakana",
    "Bopomofo",
    "Hangul Compatibility Jamo",
    "Kanbun",
    "Bopomofo Extended",
    "Enclosed CJK Letters and Months",
    "CJK Compatibility",
    "CJK Unified Ideographs Extension A",       /* 70 */
    "CJK Unified Ideographs",
    "Yi Syllables",
    "Yi Radicals",
    "Hangul Syllables",
    "High Surrogates",
    "High Private Use Surrogates",
    "Low Surrogates",
    "Private Use",
    "CJK Compatibility Ideographs",
    "Alphabetic Presentation Forms",            /* 80 */
    "Arabic Presentation Forms-A",
    "Combining Half Marks",
    "CJK Compatibility Forms",
    "Small Form Variants",
    "Arabic Presentation Forms-B",
    "Specials",
    "Halfwidth and Fullwidth Forms",
    "Old Italic",
    "Gothic",
    "Deseret",                                  /* 90 */
    "Byzantine Musical Symbols",
    "Musical Symbols",
    "Mathematical Alphanumeric Symbols",
    "CJK Unified Ideographs Extension B",
    "CJK Compatibility Ideographs Supplement",
    "Tags"
};

static void
blocksLineFn(void *context,
             char *fields[][2], int32_t fieldCount,
             UErrorCode *pErrorCode) {
    uint32_t start, limit;
    int32_t i;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in Blocks.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse block name */
    i=getTokenIndex(blockNames, UBLOCK_COUNT, fields[1][0]);
    if(i<0) {
        fprintf(stderr, "genprops error: unknown block name \"%s\" in Blocks.txt\n", fields[1][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    if(!upvec_setValue(pv, start, limit, 0, (uint32_t)i<<UPROPS_BLOCK_SHIFT, UPROPS_BLOCK_MASK, pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set block code: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* PropList.txt ------------------------------------------------------------- */

/*
 * Keep this list of property names in sync with
 * enums in icu/source/common/uprops.h, see UPROPS_BINARY_1_TOP!
 *
 * Careful: Since UPROPS_ also contain derivedPropListNames[] entries,
 * they would need to be skipped here with NULL entries if new properties
 * are added to PropList.txt.
 */
static const char *const
propListNames[]={
    "White_Space",
    "Bidi_Control",
    "Join_Control",
    "Dash",
    "Hyphen",
    "Quotation_Mark",
    "Terminal_Punctuation",
    "Other_Math",
    "Hex_Digit",
    "ASCII_Hex_Digit",
    "Other_Alphabetic",
    "Ideographic",
    "Diacritic",
    "Extender",
    "Other_Lowercase",
    "Other_Uppercase",
    "Noncharacter_Code_Point",
    "Other_Grapheme_Extend",
    "Grapheme_Link",
    "IDS_Binary_Operator",
    "IDS_Trinary_Operator",
    "Radical",
    "Unified_Ideograph",
    "Other_Default_Ignorable_Code_Point",
    "Deprecated",
    "Soft_Dotted",
    "Logical_Order_Exception"
};

static void
propListLineFn(void *context,
               char *fields[][2], int32_t fieldCount,
               UErrorCode *pErrorCode) {
    uint32_t start, limit;
    int32_t i;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in PropList.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse binary property name */
    i=getTokenIndex(propListNames, sizeof(propListNames)/sizeof(*propListNames), fields[1][0]);
    if(i<0) {
        fprintf(stderr, "genprops warning: unknown binary property name \"%s\" in PropList.txt\n", fields[1][0]);
    } else if(!upvec_setValue(pv, start, limit, 1, FLAG(i), FLAG(i), pErrorCode)) {
        fprintf(stderr, "genprops error: unable to set binary property: %s\n", u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* DerivedCoreProperties ---------------------------------------------------- */

static const char *const
derivedPropListNames[]={
    "XID_Start",
    "XID_Continue"
};

static void
derivedPropListLineFn(void *context,
                      char *fields[][2], int32_t fieldCount,
                      UErrorCode *pErrorCode) {
    uint32_t start, limit;
    int32_t i;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in DerivedCoreProperties.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse derived binary property name, ignore unknown names */
    i=getTokenIndex(derivedPropListNames, sizeof(derivedPropListNames)/sizeof(*derivedPropListNames), fields[1][0]);
    if(i>=0) {
        uint32_t flag=FLAG(UPROPS_XID_START+i);
        if(!upvec_setValue(pv, start, limit, 1, flag, flag, pErrorCode)) {
            fprintf(stderr, "genprops error: unable to set derived binary property: %s\n", u_errorName(*pErrorCode));
            exit(*pErrorCode);
        }
    }
}

/* East Asian Width --------------------------------------------------------- */

/* keep this list in sync with UEAWidthCode in uprops.h or uchar.h */
static const char *const
eaNames[U_EA_TOP]={
    "N",        /* Non-East Asian Neutral, default for unassigned code points */
    "A",        /* Ambiguous, default for Private Use code points */
    "H",        /* Half-width */
    "F",        /* Full-width */
    "Na",       /* Narrow */
    "W"         /* Wide, default for plane 2 */
};

static void
eaWidthLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode) {
    uint32_t start, limit;
    int32_t i;

    u_parseCodePointRange(fields[0][0], &start, &limit, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: syntax error in EastAsianWidth.txt field 0 at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    ++limit;

    /* parse binary property name */
    i=getTokenIndex(eaNames, U_EA_TOP, fields[1][0]);
    if(i<0) {
        fprintf(stderr, "genprops error: unknown width name \"%s\" in EastAsianWidth.txt\n", fields[1][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* collect maximum ranges */
    if(prevLimit==start && (uint32_t)i==prevValue) {
        prevLimit=limit;
    } else {
        if(!upvec_setValue(pv, prevStart, prevLimit, 0, (uint32_t)(prevValue<<UPROPS_EA_WIDTH_SHIFT), UPROPS_EA_WIDTH_MASK, pErrorCode)) {
            fprintf(stderr, "genprops error: unable to set East Asian Width: %s\n", u_errorName(*pErrorCode));
            exit(*pErrorCode);
        }
        prevStart=start;
        prevLimit=limit;
        prevValue=(uint32_t)i;
    }
}

/* data serialization ------------------------------------------------------- */

U_CFUNC int32_t
writeAdditionalData(uint8_t *p, int32_t capacity, int32_t indexes[UPROPS_INDEX_COUNT]) {
    int32_t length;
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    length=utrie_serialize(trie, p, capacity, getFoldedPropsValue, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops error: unable to serialize trie for additional properties: %s\n", u_errorName(errorCode));
        exit(errorCode);
    }
    if(p!=NULL) {
        p+=length;
        capacity-=length;
        if(beVerbose) {
            printf("size in bytes of additional props trie:%5u\n", length);
        }

        /* set indexes */
        indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]=
            indexes[UPROPS_ADDITIONAL_TRIE_INDEX]+length/4;
        indexes[UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX]=UPROPS_VECTOR_WORDS;
        indexes[UPROPS_RESERVED_INDEX]=
            indexes[UPROPS_ADDITIONAL_VECTORS_INDEX]+pvCount;
    }

    if(p!=NULL && (pvCount*4)<=capacity) {
        uprv_memcpy(p, pv, pvCount*4);
        if(beVerbose) {
            printf("number of additional props vectors:    %5u\n", pvCount/UPROPS_VECTOR_WORDS);
            printf("number of 32-bit words per vector:     %5u\n", UPROPS_VECTOR_WORDS);
        }
    }
    length+=pvCount*4;

    if(p!=NULL) {
        utrie_close(trie);
        upvec_close(pv);
    }
    return length;
}
