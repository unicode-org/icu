/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec08
*   created by: Markus W. Scherer
*
*   This program reads the Unicode character database text file,
*   parses it, and extracts most of the properties for each character.
*   It then writes a binary file containing the properties
*   that is designed to be used directly for random-access to
*   the properties of each Unicode character.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "uoptions.h"
#include "uparse.h"

U_CDECL_BEGIN
#include "genprops.h"
U_CDECL_END

UBool beVerbose=FALSE, haveCopyright=TRUE;

/* prototypes --------------------------------------------------------------- */

static void
init(void);

static void
parseMirror(const char *filename, UErrorCode *pErrorCode);

static void
parseSpecialCasing(const char *filename, UErrorCode *pErrorCode);

static void
parseCaseFolding(const char *filename, UErrorCode *pErrorCode);

static void
parseDB(const char *filename, UErrorCode *pErrorCode);

/* -------------------------------------------------------------------------- */

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT,
    UOPTION_DESTDIR,
    UOPTION_SOURCEDIR,
    { "unicode", NULL, NULL, NULL, 'u', UOPT_REQUIRES_ARG, 0 }
};

extern int
main(int argc, char* argv[]) {
    char filename[300];
    const char *srcDir=NULL, *destDir=NULL, *suffix=NULL;
    char *basename=NULL;
    UErrorCode errorCode=U_ZERO_ERROR;

    /* preset then read command line options */
    options[4].value=u_getDataDirectory();
    options[5].value="";
    options[6].value="3.0.0";
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] [suffix]\n"
            "\tread the UnicodeData.txt file and other Unicode properties files and\n"
            "\tcreate a binary file " DATA_NAME "." DATA_TYPE " with the character properties\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-v or --verbose     verbose output\n"
            "\t\t-c or --copyright   include a copyright notice\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-s or --sourcedir   source directory, followed by the path\n"
            "\t\t-u or --unicode     Unicode version, followed by the version like 3.0.0\n"
            "\t\tsuffix              suffix that is to be appended with a '-'\n"
            "\t\t                    to the source file basenames before opening;\n"
            "\t\t                    'genprops new' will read UnicodeData-new.txt etc.\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    /* get the options values */
    beVerbose=options[2].doesOccur;
    haveCopyright=options[3].doesOccur;
    srcDir=options[5].value;
    destDir=options[4].value;

    if(argc>=2) {
        suffix=argv[1];
    } else {
        suffix=NULL;
    }

    setUnicodeVersion(options[6].value);

    /* prepare the filename beginning with the source dir */
    uprv_strcpy(filename, srcDir);
    basename=filename+uprv_strlen(filename);
    if(basename>filename && *(basename-1)!=U_FILE_SEP_CHAR) {
        *basename++=U_FILE_SEP_CHAR;
    }

    /* initialize */
    init();
    initStore();

    /* process Mirror.txt */
    if(suffix==NULL) {
        uprv_strcpy(basename, "Mirror.txt");
    } else {
        uprv_strcpy(basename, "Mirror");
        basename[6]='-';
        uprv_strcpy(basename+7, suffix);
        uprv_strcat(basename+7, ".txt");
    }
    parseMirror(filename, &errorCode);

    /* process SpecialCasing.txt */
    if(suffix==NULL) {
        uprv_strcpy(basename, "SpecialCasing.txt");
    } else {
        uprv_strcpy(basename, "SpecialCasing");
        basename[13]='-';
        uprv_strcpy(basename+14, suffix);
        uprv_strcat(basename+14, ".txt");
    }
    parseSpecialCasing(filename, &errorCode);

    /* process CaseFolding.txt */
    if(suffix==NULL) {
        uprv_strcpy(basename, "CaseFolding.txt");
    } else {
        uprv_strcpy(basename, "CaseFolding");
        basename[11]='-';
        uprv_strcpy(basename+12, suffix);
        uprv_strcat(basename+12, ".txt");
    }
    parseCaseFolding(filename, &errorCode);

    /* process UnicodeData.txt */
    if(suffix==NULL) {
        uprv_strcpy(basename, "UnicodeData.txt");
    } else {
        uprv_strcpy(basename, "UnicodeData");
        basename[11]='-';
        uprv_strcpy(basename+12, suffix);
        uprv_strcat(basename+12, ".txt");
    }
    parseDB(filename, &errorCode);

    /* process parsed data */
    if(U_SUCCESS(errorCode)) {
        compactProps();
        compactStage3();
        compactStage2();

        /* write the properties data file */
        generateData(destDir);
    }

    return errorCode;
}

static void
init(void) {
}

static const char *
skipWhitespace(const char *s) {
    while(*s==' ' || *s=='\t') {
        ++s;
    }
    return s;
}

/*
 * parse a list of code points
 * store them as a string in dest[destSize] with the string length in dest[0]
 * set the first code point in *pFirst
 * return the number of code points
 */
static int32_t
parseCodePoints(const char *s,
                UChar *dest, int32_t destSize,
                uint32_t *pFirst,
                UErrorCode *pErrorCode) {
    char *end;
    uint32_t value;
    int32_t i, count;

    if(pFirst!=NULL) {
        *pFirst=0xffff;
    }

    count=0;
    i=1; /* leave dest[0] for the length value */
    for(;;) {
        s=skipWhitespace(s);
        if(*s==';' || *s==0) {
            dest[0]=(UChar)(i-1);
            return count;
        }

        /* read one code point */
        value=uprv_strtoul(s, &end, 16);
        if(end<=s || (*end!=' ' && *end!='\t' && *end!=';') || value>=0x110000) {
            fprintf(stderr, "genprops: syntax error parsing code point at %s\n", s);
            *pErrorCode=U_PARSE_ERROR;
            return -1;
        }

        /* store the first code point */
        if(++count==1 && pFirst!=NULL) {
            *pFirst=value;
        }

        /* append it to the destination array */
        UTF_APPEND_CHAR(dest, i, destSize, value);

        /* overflow? */
        if(i>=destSize) {
            fprintf(stderr, "genprops: code point sequence too long at at %s\n", s);
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return -1;
        }

        /* go to the following characters */
        s=end;
    }
}

/* parser for Mirror.txt ---------------------------------------------------- */

#define MAX_MIRROR_COUNT 2000

static uint32_t mirrorMappings[MAX_MIRROR_COUNT][2];
static int32_t mirrorCount=0;

static void
mirrorLineFn(void *context,
             char *fields[][2], int32_t fieldCount,
             UErrorCode *pErrorCode) {
    char *end;

    mirrorMappings[mirrorCount][0]=uprv_strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "genprops: syntax error in Mirror.txt field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    mirrorMappings[mirrorCount][1]=uprv_strtoul(fields[1][0], &end, 16);
    if(end<=fields[1][0] || end!=fields[1][1]) {
        fprintf(stderr, "genprops: syntax error in Mirror.txt field 1 at %s\n", fields[1][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    if(++mirrorCount==MAX_MIRROR_COUNT) {
        fprintf(stderr, "genprops: too many mirror mappings\n");
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        exit(U_INDEX_OUTOFBOUNDS_ERROR);
    }
}

static void
parseMirror(const char *filename, UErrorCode *pErrorCode) {
    char *fields[2][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 2, mirrorLineFn, NULL, pErrorCode);
}

/* parser for SpecialCasing.txt --------------------------------------------- */

#define MAX_SPECIAL_CASING_COUNT 500

static SpecialCasing specialCasings[MAX_SPECIAL_CASING_COUNT];
static int32_t specialCasingCount=0;

static void
specialCasingLineFn(void *context,
                    char *fields[][2], int32_t fieldCount,
                    UErrorCode *pErrorCode) {
    char *end;

    /* get code point */
    specialCasings[specialCasingCount].code=uprv_strtoul(skipWhitespace(fields[0][0]), &end, 16);
    end=(char *)skipWhitespace(end);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "genprops: syntax error in SpecialCasing.txt field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* is this a complex mapping? */
    if(*skipWhitespace(fields[4][0])!=0) {
        /* there is some condition text in the fifth field */
        specialCasings[specialCasingCount].isComplex=TRUE;

        /* do not store any actual mappings for this */
        specialCasings[specialCasingCount].lowerCase[0]=0;
        specialCasings[specialCasingCount].upperCase[0]=0;
        specialCasings[specialCasingCount].titleCase[0]=0;
    } else {
        /* just set the "complex" flag and get the case mappings */
        specialCasings[specialCasingCount].isComplex=FALSE;
        parseCodePoints(fields[1][0], specialCasings[specialCasingCount].lowerCase, 32, NULL, pErrorCode);
        parseCodePoints(fields[3][0], specialCasings[specialCasingCount].upperCase, 32, NULL, pErrorCode);
        parseCodePoints(fields[2][0], specialCasings[specialCasingCount].titleCase, 32, NULL, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            fprintf(stderr, "genprops: error parsing special casing at %s\n", fields[0][0]);
            exit(*pErrorCode);
        }
    }

    if(++specialCasingCount==MAX_SPECIAL_CASING_COUNT) {
        fprintf(stderr, "genprops: too many special casing mappings\n");
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        exit(U_INDEX_OUTOFBOUNDS_ERROR);
    }
}

static int
compareSpecialCasings(const void *left, const void *right) {
    return ((const SpecialCasing *)left)->code-((const SpecialCasing *)right)->code;
}

static void
parseSpecialCasing(const char *filename, UErrorCode *pErrorCode) {
    char *fields[5][2];
    int32_t i, j;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 5, specialCasingLineFn, NULL, pErrorCode);

    /* sort the special casing entries by code point */
    if(specialCasingCount>0) {
        qsort(specialCasings, specialCasingCount, sizeof(SpecialCasing), compareSpecialCasings);
    }

    /* replace multiple entries for any code point by one "complex" one */
    j=0;
    for(i=1; i<specialCasingCount; ++i) {
        if(specialCasings[i-1].code==specialCasings[i].code) {
            /* there is a duplicate code point */
            specialCasings[i-1].code=0x7fffffff;    /* remove this entry in the following qsort */
            specialCasings[i].isComplex=TRUE;       /* make the following one complex */
            specialCasings[i].lowerCase[0]=0;
            specialCasings[i].upperCase[0]=0;
            specialCasings[i].titleCase[0]=0;
            ++j;
        }
    }

    /* if some entries just were removed, then re-sort */
    if(j>0) {
        qsort(specialCasings, specialCasingCount, sizeof(SpecialCasing), compareSpecialCasings);
        specialCasingCount-=j;
    }
}

/* parser for CaseFolding.txt ----------------------------------------------- */

#define MAX_CASE_FOLDING_COUNT 500

static CaseFolding caseFoldings[MAX_CASE_FOLDING_COUNT];
static int32_t caseFoldingCount=0;

static void
caseFoldingLineFn(void *context,
                  char *fields[][2], int32_t fieldCount,
                  UErrorCode *pErrorCode) {
    char *end;
    int32_t count;
    char status;

    /* get code point */
    caseFoldings[caseFoldingCount].code=uprv_strtoul(skipWhitespace(fields[0][0]), &end, 16);
    end=(char *)skipWhitespace(end);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "genprops: syntax error in CaseFolding.txt field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* get the status of this mapping */
    caseFoldings[caseFoldingCount].status=status=*skipWhitespace(fields[1][0]);
    if(status!='L' && status!='E' && status!='C' && status!='S' && status!='F' && status!='I') {
        fprintf(stderr, "genprops: unrecognized status field in CaseFolding.txt at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* ignore all case folding mappings that are the same as the UnicodeData.txt lowercase mappings */
    if(status=='L') {
        return;
    }

    /* get the mapping */
    count=parseCodePoints(fields[2][0], caseFoldings[caseFoldingCount].full, 32, &caseFoldings[caseFoldingCount].simple, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genprops: error parsing CaseFolding.txt mapping at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }

    /* there is a simple mapping only if there is exactly one code point */
    if(count!=1) {
        caseFoldings[caseFoldingCount].simple=0;
    }

    /* check the status */
    if(status=='S') {
        /* check if there was a full mapping for this code point before */
        if( caseFoldingCount>0 &&
            caseFoldings[caseFoldingCount-1].code==caseFoldings[caseFoldingCount].code &&
            caseFoldings[caseFoldingCount-1].status=='F'
        ) {
            /* merge the two entries */
            caseFoldings[caseFoldingCount-1].simple=caseFoldings[caseFoldingCount].simple;
            return;
        }
    } else if(status=='F') {
        /* check if there was a simple mapping for this code point before */
        if( caseFoldingCount>0 &&
            caseFoldings[caseFoldingCount-1].code==caseFoldings[caseFoldingCount].code &&
            caseFoldings[caseFoldingCount-1].status=='S'
        ) {
            /* merge the two entries */
            uprv_memcpy(caseFoldings[caseFoldingCount-1].full, caseFoldings[caseFoldingCount].full, 32*U_SIZEOF_UCHAR);
            return;
        }
    } else if(status=='I') {
        /* store only a marker for special handling for cases like dotless i */
        caseFoldings[caseFoldingCount].simple=0;
        caseFoldings[caseFoldingCount].full[0]=0;
    }

    if(++caseFoldingCount==MAX_CASE_FOLDING_COUNT) {
        fprintf(stderr, "genprops: too many case folding mappings\n");
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        exit(U_INDEX_OUTOFBOUNDS_ERROR);
    }
}

static void
parseCaseFolding(const char *filename, UErrorCode *pErrorCode) {
    char *fields[3][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 3, caseFoldingLineFn, NULL, pErrorCode);
}

/* parser for UnicodeData.txt ----------------------------------------------- */

/* general categories */
const char *const
genCategoryNames[U_CHAR_CATEGORY_COUNT]={
    NULL,
    "Lu", "Ll", "Lt", "Lm", "Lo", "Mn", "Me",
    "Mc", "Nd", "Nl", "No",
    "Zs", "Zl", "Zp",
    "Cc", "Cf", "Co", "Cs",
    "Pd", "Ps", "Pe", "Pc", "Po",
    "Sm", "Sc", "Sk", "So",
    "Pi", "Pf",
    "Cn"
};

const char *const
bidiNames[U_CHAR_DIRECTION_COUNT]={
    "L", "R", "EN", "ES", "ET", "AN", "CS", "B", "S",
    "WS", "ON", "LRE", "LRO", "AL", "RLE", "RLO", "PDF", "NSM", "BN"
};

/* control code properties */
static const struct {
    uint32_t code;
    uint8_t generalCategory;
} controlProps[]={
    /* TAB */   0x9, U_SPACE_SEPARATOR,
    /* VT */    0xb, U_SPACE_SEPARATOR,
    /* LF */    0xa, U_PARAGRAPH_SEPARATOR,
    /* FF */    0xc, U_LINE_SEPARATOR,
    /* CR */    0xd, U_PARAGRAPH_SEPARATOR,
    /* FS */    0x1c, U_PARAGRAPH_SEPARATOR,
    /* GS */    0x1d, U_PARAGRAPH_SEPARATOR,
    /* RS */    0x1e, U_PARAGRAPH_SEPARATOR,
    /* US */    0x1f, U_SPACE_SEPARATOR,
    /* NL */    0x85, U_PARAGRAPH_SEPARATOR
};

static struct {
    uint32_t first, last, props;
    char name[80];
} unicodeAreas[32];

static int32_t unicodeAreaIndex=0;

static void
unicodeDataLineFn(void *context,
                  char *fields[][2], int32_t fieldCount,
                  UErrorCode *pErrorCode) {
    static int32_t mirrorIndex=0, specialCasingIndex=0, caseFoldingIndex=0;
    Props p;
    char *end;
    uint32_t value;
    int i;

    /* reset the properties */
    uprv_memset(&p, 0, sizeof(Props));
    p.decimalDigitValue=p.digitValue=-1;
    p.numericValue=0x80000000;

    /* get the character code, field 0 */
    p.code=uprv_strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "genprops: syntax error in field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* get general category, field 2 */
    *fields[2][1]=0;
    for(i=1;;) {
        if(uprv_strcmp(fields[2][0], genCategoryNames[i])==0) {
            p.generalCategory=(uint8_t)i;
            break;
        }
        if(++i==U_CHAR_CATEGORY_COUNT) {
            fprintf(stderr, "genprops: unknown general category \"%s\" at code 0x%lx\n", fields[2][0], p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
    }

    /* get canonical combining class, field 3 */
    p.canonicalCombining=(uint8_t)uprv_strtoul(fields[3][0], &end, 10);
    if(end<=fields[3][0] || end!=fields[3][1]) {
        fprintf(stderr, "genprops: syntax error in field 3 at code 0x%lx\n", p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* get BiDi category, field 4 */
    *fields[4][1]=0;
    for(i=0;;) {
        if(uprv_strcmp(fields[4][0], bidiNames[i])==0) {
            p.bidi=(uint8_t)i;
            break;
        }
        if(++i==U_CHAR_DIRECTION_COUNT) {
            fprintf(stderr, "genprops: unknown BiDi category \"%s\" at code 0x%lx\n", fields[4][0], p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
    }

    /* decimal digit value, field 6 */
    if(fields[6][0]<fields[6][1]) {
        value=uprv_strtoul(fields[6][0], &end, 10);
        if(end!=fields[6][1] || value>0x7fff) {
            fprintf(stderr, "genprops: syntax error in field 6 at code 0x%lx\n", p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
        p.decimalDigitValue=(int16_t)value;
    }

    /* digit value, field 7 */
    if(fields[7][0]<fields[7][1]) {
        value=uprv_strtoul(fields[7][0], &end, 10);
        if(end!=fields[7][1] || value>0x7fff) {
            fprintf(stderr, "genprops: syntax error in field 7 at code 0x%lx\n", p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
        p.digitValue=(int16_t)value;
    }

    /* numeric value, field 8 */
    if(fields[8][0]<fields[8][1]) {
        char *s=fields[8][0];
        UBool isNegative;

        /* get a possible minus sign */
        if(*s=='-') {
            isNegative=TRUE;
            ++s;
        } else {
            isNegative=FALSE;
        }

        value=uprv_strtoul(s, &end, 10);
        if(value>0 && *end=='/') {
            /* field 8 may contain a fractional value, get the denominator */
            p.denominator=uprv_strtoul(end+1, &end, 10);
            if(p.denominator==0) {
                fprintf(stderr, "genprops: denominator is 0 in field 8 at code 0x%lx\n", p.code);
                *pErrorCode=U_PARSE_ERROR;
                exit(U_PARSE_ERROR);
            }
        }
        if(end!=fields[8][1] || value>0x7fffffff) {
            fprintf(stderr, "genprops: syntax error in field 8 at code 0x%lx\n", p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }

        if(isNegative) {
            p.numericValue=-(int32_t)value;
        } else {
            p.numericValue=(int32_t)value;
        }
        p.hasNumericValue=TRUE;
    }

    /* get Mirrored flag, field 9 */
    if(*fields[9][0]=='Y') {
        p.isMirrored=1;
    } else if(fields[9][1]-fields[9][0]!=1 || *fields[9][0]!='N') {
        fprintf(stderr, "genprops: syntax error in field 9 at code 0x%lx\n", p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* get uppercase mapping, field 12 */
    value=uprv_strtoul(fields[12][0], &end, 16);
    if(end!=fields[12][1]) {
        fprintf(stderr, "genprops: syntax error in field 12 at code 0x%lx\n", p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    p.upperCase=value;

    /* get lowercase value, field 13 */
    value=uprv_strtoul(fields[13][0], &end, 16);
    if(end!=fields[13][1]) {
        fprintf(stderr, "genprops: syntax error in field 13 at code 0x%lx\n", p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    p.lowerCase=value;

    /* get titlecase value, field 14 */
    value=uprv_strtoul(fields[14][0], &end, 16);
    if(end!=fields[14][1]) {
        fprintf(stderr, "genprops: syntax error in field 14 at code 0x%lx\n", p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    p.titleCase=value;

    /* override properties for some common control characters */
    if(p.generalCategory==U_CONTROL_CHAR) {
        for(i=0; i<sizeof(controlProps)/sizeof(controlProps[0]); ++i) {
            if(controlProps[i].code==p.code) {
                p.generalCategory=controlProps[i].generalCategory;
            }
        }
    }

    /* set additional properties from previously parsed files */
    if(mirrorIndex<mirrorCount && p.code==mirrorMappings[mirrorIndex][0]) {
        p.mirrorMapping=mirrorMappings[mirrorIndex++][1];
    }
    if(specialCasingIndex<specialCasingCount && p.code==specialCasings[specialCasingIndex].code) {
        p.specialCasing=specialCasings+specialCasingIndex++;
    } else {
        p.specialCasing=NULL;
    }
    if(caseFoldingIndex<caseFoldingCount && p.code==caseFoldings[caseFoldingIndex].code) {
        p.caseFolding=caseFoldings+caseFoldingIndex++;

        /* ignore "Common" mappings (simple==full) that map to the same code point as the regular lowercase mapping */
        if( p.caseFolding->status=='C' &&
            p.caseFolding->simple==p.lowerCase
        ) {
            p.caseFolding=NULL;
        }
    } else {
        p.caseFolding=NULL;
    }

    value=makeProps(&p);

    if(*fields[1][0]=='<') {
        /* first or last entry of a Unicode area */
        size_t length=fields[1][1]-fields[1][0];

        if(length<9) {
            /* name too short for an area name */
        } else if(0==uprv_memcmp(", First>", fields[1][1]-8, 8)) {
            /* set the current area */
            if(unicodeAreas[unicodeAreaIndex].first==0xffffffff) {
                length-=9;
                unicodeAreas[unicodeAreaIndex].first=p.code;
                unicodeAreas[unicodeAreaIndex].props=value;
                uprv_memcpy(unicodeAreas[unicodeAreaIndex].name, fields[1][0]+1, length);
                unicodeAreas[unicodeAreaIndex].name[length]=0;
            } else {
                /* error: a previous area is incomplete */
                fprintf(stderr, "genprops: error - area \"%s\" is incomplete\n", unicodeAreas[unicodeAreaIndex].name);
                *pErrorCode=U_PARSE_ERROR;
                exit(U_PARSE_ERROR);
            }
            return;
        } else if(0==uprv_memcmp(", Last>", fields[1][1]-7, 7)) {
            /* check that the current area matches, and complete it with the last code point */
            length-=8;
            if( unicodeAreas[unicodeAreaIndex].props==value &&
                0==uprv_memcmp(unicodeAreas[unicodeAreaIndex].name, fields[1][0]+1, length) &&
                unicodeAreas[unicodeAreaIndex].name[length]==0 &&
                unicodeAreas[unicodeAreaIndex].first<p.code
            ) {
                unicodeAreas[unicodeAreaIndex].last=p.code;
                if(beVerbose) {
                    printf("Unicode area U+%04lx..U+%04lx \"%s\"\n",
                        unicodeAreas[unicodeAreaIndex].first,
                        unicodeAreas[unicodeAreaIndex].last,
                        unicodeAreas[unicodeAreaIndex].name);
                }
                unicodeAreas[++unicodeAreaIndex].first=0xffffffff;
            } else {
                /* error: different properties between first & last, different area name, first>=last */
                fprintf(stderr, "genprops: error - Last of area \"%s\" is incorrect\n", unicodeAreas[unicodeAreaIndex].name);
                *pErrorCode=U_PARSE_ERROR;
                exit(U_PARSE_ERROR);
            }
            return;
        } else {
            /* not an area name */
        }
    }

    /* properties for a single code point */
    /* ### TODO: check that the code points (p.code) are in ascending order */
    addProps(p.code, value);
}

/* set repeated properties for the areas */
static void
repeatAreaProps() {
    uint32_t puaProps;
    int32_t i;
    UBool hasPlane15PUA, hasPlane16PUA;

    /*
     * UnicodeData.txt before 3.0.1 did not contain the PUAs on
     * planes 15 and 16.
     * If that is the case, then we add them here, using the properties
     * from the BMP PUA.
     */
    puaProps=0;
    hasPlane15PUA=hasPlane16PUA=FALSE;

    for(i=0; i<unicodeAreaIndex; ++i) {
        repeatProps(unicodeAreas[i].first,
                    unicodeAreas[i].last,
                    unicodeAreas[i].props);
        if(unicodeAreas[i].first==0xe000) {
            puaProps=unicodeAreas[i].props;
        } else if(unicodeAreas[i].first==0xf0000) {
            hasPlane15PUA=TRUE;
        } else if(unicodeAreas[i].first==0x100000) {
            hasPlane16PUA=TRUE;
        }
    }

    if(puaProps!=0) {
        if(!hasPlane15PUA) {
            repeatProps(0xf0000, 0xffffd, puaProps);
        }
        if(!hasPlane16PUA) {
            repeatProps(0x100000, 0x10fffd, puaProps);
        }
    }
}

static void
parseDB(const char *filename, UErrorCode *pErrorCode) {
    char *fields[15][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    /* while unicodeAreas[unicodeAreaIndex] is unused, set its first to a bogus value */
    unicodeAreas[0].first=0xffffffff;

    u_parseDelimitedFile(filename, ';', fields, 15, unicodeDataLineFn, NULL, pErrorCode);

    if(unicodeAreas[unicodeAreaIndex].first!=0xffffffff) {
        fprintf(stderr, "genprops: error - the last area \"%s\" from U+%04lx is incomplete\n",
            unicodeAreas[unicodeAreaIndex].name,
            unicodeAreas[unicodeAreaIndex].first);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    repeatAreaProps();
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
