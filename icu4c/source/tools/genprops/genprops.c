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
#include "genprops.h"

extern bool_t beVerbose=FALSE, haveCopyright=TRUE;

/* prototypes --------------------------------------------------------------- */

static void
init(void);

static void
parseMirror(const char *filename, UErrorCode *pErrorCode);

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
main(int argc, const char *argv[]) {
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
        *basename=U_FILE_SEP_CHAR;
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
        repeatProps();
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

/* parser for Mirror.txt ---------------------------------------------------- */

#define MAX_MIRROR_COUNT 2000

static uint32_t mirrorMappings[MAX_MIRROR_COUNT][2];
static int32_t mirrorCount=0;

static void
MirrorCode(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    char *end;

    mirrorMappings[mirrorCount][fieldNr]=uprv_strtoul(start, &end, 16);
    if((end-start)<1 || end!=limit) {
        fprintf(stderr, "genprops: syntax error in Mirror.txt field %d at %s\n", fieldNr, start);
        exit(U_PARSE_ERROR);
    }
}

static void
MirrorFinish(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    if(++mirrorCount==MAX_MIRROR_COUNT) {
        fprintf(stderr, "genprops: too many mirror mappings\n");
        exit(U_INDEX_OUTOFBOUNDS_ERROR);
    }
}

static UParseFieldFn *mirrorFields[4]={
    NULL,
    MirrorCode,
    MirrorCode,
    MirrorFinish
};

static void
parseMirror(const char *filename, UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', mirrorFields, 2, NULL, pErrorCode);
}

/* parser for UnicodeData.txt ----------------------------------------------- */

#define NO_NUMERIC_VALUE ((uint32_t)15821005)

/* general categories */
extern const char *const
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

extern const char *const
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

static void
UnicodeDataInit(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;

    /* reset the properties */
    uprv_memset(p, 0, sizeof(Props));
    p->numericValue=NO_NUMERIC_VALUE;
}

static void
UnicodeDataCode(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;
    char *end;

    /* get the character code, field 0 */
    p->code=uprv_strtoul(start, &end, 16);
    if((end-start)<1 || end!=limit) {
        fprintf(stderr, "genprops: syntax error in field 0 at %s\n", start);
        exit(U_PARSE_ERROR);
    }
}

static void
UnicodeDataCategory(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;
    int i;
    char c;

    /* get general category, field 2 */
    c=*limit;
    *limit=0;
    for(i=1;;) {
        if(uprv_strcmp(start, genCategoryNames[i])==0) {
            p->generalCategory=(uint8_t)i;
            break;
        }
        if(++i==U_CHAR_CATEGORY_COUNT) {
            fprintf(stderr, "genprops: unknown general category \"%s\" at code 0x%lx\n", start, p->code);
            exit(U_PARSE_ERROR);
        }
    }
    *limit=c;
}

static void
UnicodeDataCombining(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;
    char *end;

    /* get canonical combining class, field 3 */
    p->canonicalCombining=(uint8_t)uprv_strtoul(start, &end, 10);
    if(start>=end || end!=limit) {
        fprintf(stderr, "genprops: syntax error in field 3 at code 0x%lx\n", p->code);
        exit(U_PARSE_ERROR);
    }
}

static void
UnicodeDataBiDi(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;
    int i;
    char c;

    /* get BiDi category, field 4 */
    c=*limit;
    *limit=0;
    for(i=0;;) {
        if(uprv_strcmp(start, bidiNames[i])==0) {
            p->bidi=(uint8_t)i;
            break;
        }
        if(++i==U_CHAR_DIRECTION_COUNT) {
            fprintf(stderr, "genprops: unknown BiDi category \"%s\" at code 0x%lx\n", start, p->code);
            exit(U_PARSE_ERROR);
        }
    }
    *limit=c;
}

static void
UnicodeDataNumeric(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;
    uint32_t value;
    char *end;

    /* decimal digit value, field 6 */
    /* digit value, field 7 */
    /* numeric value, field 8 */
    value=uprv_strtoul(start, &end, 10);
    if(fieldNr==8 && value>0 && *end=='/') {
        /* field 8 may contain a fractional value, get the denominator */
        p->denominator=uprv_strtoul(end+1, &end, 10);
    }
    if(end!=limit) {
        fprintf(stderr, "genprops: syntax error in field 6 at code 0x%lx\n", p->code);
        exit(U_PARSE_ERROR);
    }
    if(start<end) {
        if(p->numericValue!=NO_NUMERIC_VALUE && p->numericValue!=value) {
            fprintf(stderr, "genprops: more than one numeric value at code 0x%lx\n", p->code);
            exit(U_PARSE_ERROR);
        }
        p->numericValue=value;
    }
}

static void
UnicodeDataMirrored(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;

    /* get Mirrored flag, field 9 */
    if(*start=='Y') {
        p->isMirrored=1;
    } else if(limit-start!=1 || *start!='N') {
        fprintf(stderr, "genprops: syntax error in field 9 at code 0x%lx\n", p->code);
        exit(U_PARSE_ERROR);
    }
}

static void
UnicodeDataCase(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    Props *p=(Props *)context;
    char *end;
    uint32_t mapping;

    /* get uppercase mapping, field 12 */
    /* get lowercase mapping, field 13 */
    /* get titlecase mapping, field 14 */
    mapping=uprv_strtoul(start, &end, 16);
    if(end!=limit) {
        fprintf(stderr, "genprops: syntax error in field %d at code 0x%lx\n", fieldNr, p->code);
        exit(U_PARSE_ERROR);
    }
    switch(fieldNr) {
    case 12:
        p->upperCase=mapping;
        break;
    case 13:
        p->lowerCase=mapping;
        break;
    case 14:
        p->titleCase=mapping;
        break;
    }
}

static void
UnicodeDataFinish(void *context, char *start, char *limit, int32_t fieldNr, UErrorCode *pErrorCode) {
    static int32_t mirrorIndex=0;
    Props *p=(Props *)context;
    int16_t i;

    if(p->numericValue==NO_NUMERIC_VALUE) {
        p->numericValue=0;
    }

    /* override properties for some common control characters */
    if(p->generalCategory==U_CONTROL_CHAR) {
        for(i=0; i<sizeof(controlProps)/sizeof(controlProps[0]); ++i) {
            if(controlProps[i].code==p->code) {
                p->generalCategory=controlProps[i].generalCategory;
            }
        }
    }

    /* set additional properties from previously parsed files */
    if(mirrorIndex<mirrorCount && p->code==mirrorMappings[mirrorIndex][0]) {
        p->mirrorMapping=mirrorMappings[mirrorIndex++][1];
    }

    addProps(p);
}

static UParseFieldFn *unicodeDBFields[17]={
    UnicodeDataInit,

    UnicodeDataCode,
    NULL,                   /* 1: character name */
    UnicodeDataCategory,
    UnicodeDataCombining,
    UnicodeDataBiDi,
    NULL,                   /* 5: character decomposition mapping */
    UnicodeDataNumeric,
    UnicodeDataNumeric,
    UnicodeDataNumeric,
    UnicodeDataMirrored,
    NULL,                   /* 10: Unicode 1.0 character name */
    NULL,                   /* 11: comment */
    UnicodeDataCase,
    UnicodeDataCase,
    UnicodeDataCase,

    UnicodeDataFinish
};

static void
parseDB(const char *filename, UErrorCode *pErrorCode) {
    Props p;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', unicodeDBFields, 15, &p, pErrorCode);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
