/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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
#include "utypes.h"
#include "uchar.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "udata.h"
#include "unewdata.h"

#define DATA_NAME "uprops"
#define DATA_TYPE "dat"

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    0x55, 0x50, 0x72, 0x6f,     /* dataFormat="UPro" */
    1, 0, 0, 0,                 /* formatVersion */
    3, 0, 0, 0                  /* dataVersion */
};

static bool_t beVerbose=FALSE, haveCopyright=TRUE;

/*
 * Definitions and arrays for the 3-stage lookup.
 */
enum {
    STAGE_1_BITS=11, STAGE_2_BITS=6, STAGE_3_BITS=4,

    STAGE_1_COUNT=0x110000>>(STAGE_2_BITS+STAGE_3_BITS),
    STAGE_2_COUNT=1<<STAGE_2_BITS,
    STAGE_3_COUNT=1<<STAGE_3_BITS,

    MAX_PROPS_COUNT=20000,
    MAX_UCHAR_COUNT=10000,
    MAX_STAGE_2_COUNT=MAX_PROPS_COUNT/10,
    MAX_STAGES_1_2_COUNT=STAGE_1_COUNT+MAX_STAGE_2_COUNT
};

static uint16_t stages1_2[MAX_STAGES_1_2_COUNT];

static uint16_t stage2Top=STAGE_1_COUNT;

/* character properties */
typedef struct {
    uint32_t code, lowerCase, upperCase, titleCase;
    uint32_t numericValue, denominator;
    /* special casing? */
    /* decomposition mappping? */
    uint8_t generalCategory, canonicalCombining, bidi, isMirrored;
} Props;

static Props props[MAX_PROPS_COUNT];

/* Unicode characters, e.g., for special casing or decomposition */

static UChar uchars[MAX_UCHAR_COUNT];
static uint16_t ucharsTop=0;

/* general categories */

static const char *const genCategoryNames[U_CHAR_CATEGORY_COUNT]={
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

static const char *const bidiNames[U_CHAR_DIRECTION_COUNT]={
	"L", "R", "EN", "ES", "ET", "AN", "CS", "B", "S",
    "WS", "ON", "LRE", "LRO", "AL", "RLE", "RLO", "PDF", "NSM", "BN"
};

/* prototypes --------------------------------------------------------------- */

static void
init();

static void
parseDB(FileStream *in);

static int16_t
getField(char *line, int16_t start, int16_t limit);

static void
checkLineIndex(uint32_t code, int16_t limit, int16_t length);

static void
addProps(Props *p);

static void
compress();

static void
generateData();

static uint16_t
addUChars(const UChar *s, uint16_t length);

/* -------------------------------------------------------------------------- */

extern int
main(int argc, char *argv[]) {
    FileStream *in;
    char *arg, *filename=NULL;
    int i;

    if(argc<=1) {
        fprintf(stderr,
            "usage: %s [-1[+|-]] [-v[+|-]] [-c[+|-]] filename\n"
            "\tread the UnicodeData.txt file and \n"
            "\tcreate a binary file " DATA_NAME "." DATA_TYPE " with the character properties\n"
            "\toptions:\n"
            "\t\t-v[+|-]  verbose output\n"
            "\t\t-c[+|-]  do (not) include a copyright notice\n"
            "\t\tfilename  absolute path/filename for the\n"
            "\t\t\tUnicode database text file (default: standard input)\n",
            argv[0]);
    }

    for(i=1; i<argc; ++i) {
        arg=argv[i];
        if(arg[0]=='-') {
            switch(arg[1]) {
            case 'v':
                beVerbose= arg[2]=='+';
                break;
            case 'c':
                haveCopyright= arg[2]=='+';
                break;
            default:
                break;
            }
        } else {
            filename=arg;
        }
    }

    if(filename==NULL) {
        in=T_FileStream_stdin();
    } else {
        in=T_FileStream_open(filename, "r");
        if(in==NULL) {
            fprintf(stderr, "genprops: unable to open input file %s\n", filename);
            exit(U_FILE_ACCESS_ERROR);
        }
    }

    init();
    parseDB(in);
    compress();
    generateData();

    if(in!=T_FileStream_stdin()) {
        T_FileStream_close(in);
    }

    return 0;
}

static void
init() {
}

/* parsing ------------------------------------------------------------------ */

static void
parseDB(FileStream *in) {
    char line[300];
    char *end;
    Props p;
    uint32_t value;
    int16_t start, limit, length, i;
    bool_t hasNumericValue;

    while(T_FileStream_readLine(in, line, sizeof(line))!=NULL) {
        length=icu_strlen(line);

        /* remove trailing newline characters */
        while(length>0 && (line[length-1]=='\r' || line[length-1]=='\n')) {
            line[--length]=0;
        }

        /* reset the properties */
        icu_memset(&p, 0, sizeof(p));
        hasNumericValue=FALSE;

        /* get the character code, field 0 */
        p.code=icu_strtoul(line, &end, 16);
        limit=end-line;
        if(limit<1 || *end!=';') {
            fprintf(stderr, "genprops: syntax error in field 0 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* skip character name, field 1 */
        checkLineIndex(p.code, ++limit, length);
        limit=getField(line, limit, length);

        /* get general category, field 2 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        limit=getField(line, start, length);
        line[limit]=0;
        for(i=1;;) {
            if(icu_strcmp(line+start, genCategoryNames[i])==0) {
                p.generalCategory=(uint8_t)i;
                break;
            }
            if(++i==U_CHAR_CATEGORY_COUNT) {
                fprintf(stderr, "genprops: unknown general category \"%s\" at code 0x%lx\n", line+start, p.code);
                exit(U_PARSE_ERROR);
            }
        }

        /* get canonical combining class, field 3 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        p.canonicalCombining=(uint8_t)icu_strtoul(line+start, &end, 10);
        limit=end-line;
        if(start>=limit || *end!=';') {
            fprintf(stderr, "genprops: syntax error in field 3 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* get BiDi category, field 4 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        limit=getField(line, start, length);
        line[limit]=0;
        for(i=0;;) {
            if(icu_strcmp(line+start, bidiNames[i])==0) {
                p.bidi=(uint8_t)i;
                break;
            }
            if(++i==U_CHAR_DIRECTION_COUNT) {
                fprintf(stderr, "genprops: unknown BiDi category \"%s\" at code 0x%lx\n", line+start, p.code);
                exit(U_PARSE_ERROR);
            }
        }

        /* character decomposition mapping, field 5 */
        /* ### skip for now */
        checkLineIndex(p.code, ++limit, length);
        limit=getField(line, limit, length);

        /* decimal digit value, field 6 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        value=icu_strtoul(line+start, &end, 10);
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 6 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }
        limit=end-line;
        if(start<limit) {
            p.numericValue=value;
            hasNumericValue=TRUE;
        }

        /* digit value, field 7 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        value=icu_strtoul(line+start, &end, 10);
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 7 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }
        limit=end-line;
        if(start<limit) {
            if(hasNumericValue) {
                if(p.numericValue!=value) {
                    fprintf(stderr, "genprops: more than one numeric value at code 0x%lx\n", p.code);
                    exit(U_PARSE_ERROR);
                }
            } else {
                p.numericValue=value;
                hasNumericValue=TRUE;
            }
        }

        /* numeric value, field 8 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        value=icu_strtoul(line+start, &end, 10);
        if(value>0 && *end=='/') {
            p.denominator=icu_strtoul(end+1, &end, 10);
        }
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 8 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }
        limit=end-line;
        if(start<limit) {
            if(hasNumericValue) {
                if(p.numericValue!=value) {
                    fprintf(stderr, "genprops: more than one numeric value at code 0x%lx\n", p.code);
                    exit(U_PARSE_ERROR);
                }
            } else {
                p.numericValue=value;
                hasNumericValue=TRUE;
            }
        }

        /* get Mirrored flag, field 9 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        limit=getField(line, start, length);
        if(line[start]=='Y') {
            p.isMirrored=1;
        } else if(limit-start!=1 || line[start]!='N') {
            fprintf(stderr, "genprops: syntax error in field 9 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* skip Unicode 1.0 character name, field 10 */
        checkLineIndex(p.code, ++limit, length);
        limit=getField(line, limit, length);

        /* skip comment, field 11 */
        checkLineIndex(p.code, ++limit, length);
        limit=getField(line, limit, length);

        /* get uppercase mapping, field 12 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        p.upperCase=icu_strtoul(line+start, &end, 16);
        limit=end-line;
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 12 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* get lowercase mapping, field 13 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        p.lowerCase=icu_strtoul(line+start, &end, 16);
        limit=end-line;
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 13 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* get titlecase mapping, field 14 */
        start=limit+1;
        if(start<length) {
            /* this is the last field */
            p.titleCase=icu_strtoul(line+start, &end, 16);
            if(*end!=';' && *end!=0) {
                fprintf(stderr, "genprops: syntax error in field 14 at code 0x%lx\n", p.code);
                exit(U_PARSE_ERROR);
            }
        }

        /* ### debug output */
        printf(
            "0x%06lx "
            "%s(%2d) "
            "comb=%3d "
            "bidi=%3s(%2d) "
            "num=%7d/%7d "
            "mirr=%d "
            "u%06lx l%06lx t%06lx"
            "\n",
            p.code,
            genCategoryNames[p.generalCategory], p.generalCategory,
            p.canonicalCombining,
            bidiNames[p.bidi], p.bidi,
            p.numericValue, p.denominator,
            p.isMirrored,
            p.upperCase, p.lowerCase, p.titleCase);

        addProps(&p);
    }
}

static int16_t
getField(char *line, int16_t start, int16_t limit) {
    while(start<limit && line[start]!=';') {
        ++start;
    }
    return start;
}

static void
checkLineIndex(uint32_t code, int16_t index, int16_t length) {
    if(index>=length) {
        fprintf(stderr, "genprops: too few fields at code 0x%lx\n", code);
        exit(U_PARSE_ERROR);
    }
}

/* store a character's properties ------------------------------------------- */

static void
addProps(Props *p) {
    uint16_t count;

    /*
     * Simple ideas for reducing the number of bits for one character's
     * properties:
     *
     * Some fields are only used for characters of certain
     * general categories:
     * - casing fields for letters and others, not for
     *     numbers & Mn
     *   + uppercase not for uppercase letters
     *   + lowercase not for lowercase letters
     *   + titlecase not for titlecase letters
     *
     *   * most of the time, uppercase=titlecase
     * - numeric fields for various digit & other types
     * - canonical combining classes for non-spacing marks (Mn)
     * * the above is not always true, for all three cases
     *
     * Using the same bits for alternate fields saves some space.
     *
     * For the canonical categories, there are only few actually used.
     * They can be stored using 5 bits.
     *
     * In the BiDi categories, the 5 explicit codes are only ever
     * assigned 1:1 to 5 well-known code points. Storing only one
     * value for all "explicit codes" gets this down to 4 bits.
     * Client code then needs to check for this special value
     * and replace it by the real one using a 5-element table.
     *
     * The general categories Mn & Me, non-spacing & enclosing marks,
     * are always NSM, and NSM are always of those categories.
     *
     * Digit values can often be derived from the code point value
     * itself in a simple way.
     *
     */

    /* count the case mappings */
    count=0;
    if(p->upperCase!=0) {
        ++count;
    }
    if(p->lowerCase!=0) {
        ++count;
    }
    if(p->upperCase!=p->titleCase) {
        ++count;
    }

    /* verify that only Mn has a canonical combining class */
    if(p->generalCategory!=U_NON_SPACING_MARK && p->canonicalCombining>0) {
        printf("*** code 0x%06x: canonical combining class does not fit expected range ***\n", p->code);
    }

    /* verify that only numeric categories have numeric values */
    if(genCategoryNames[p->generalCategory][0]!='N' && p->numericValue!=0) {
        printf("*** code 0x%06x: non-numeric category but numeric value\n", p->code);
    }

    /* verify that no numbers and no Mn have case mappings */
    /* this is not 100% true either (see 0345;COMBINING GREEK YPOGEGRAMMENI) */
    if( (genCategoryNames[p->generalCategory][0]=='N' ||
         p->generalCategory==U_NON_SPACING_MARK) &&
        count>0
    ) {
        printf("*** code 0x%06x: number category or Mn but case mapping\n", p->code);
    } else if(count>1) {
        /* see for which characters there are two case mappings */
        /* there are some, but few (12) */
        printf("*** code 0x%06x: more than one case mapping\n", p->code);
    }

    /* verify that { Mn, Me } if and only if NSM */
    if( (p->generalCategory==U_NON_SPACING_MARK ||
         p->generalCategory==U_ENCLOSING_MARK)
        ^
        (p->bidi==U_DIR_NON_SPACING_MARK)) {
        printf("*** code 0x%06x: bidi class does not fit expected range ***\n", p->code);
    }

    /*
     * "Higher-hanging fruit":
     * For some sets of fields, there are fewer sets of values
     * than the product of the numbers of values per field.
     * This means that storing one single value for more than
     * one field and later looking up both field values in a table
     * saves space.
     * Examples:
     * - general category & BiDi
     *
     * There are only few common displacements between a code point
     * and its case mappings. Store deltas. Store codes for few
     * occuring deltas.
     */
}

/* compressing -------------------------------------------------------------- */

static void
compress() {
}

/* generate output data ----------------------------------------------------- */

static void
generateData() {
    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t size;
    long dataLength;

    pData=udata_create(DATA_TYPE, DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: unable to create data memory, error %d\n", errorCode);
        exit(errorCode);
    }

    /* ### */
    size=0;

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genprops: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=(long)size) {
        fprintf(stderr, "genprops: data length %ld != calculated size %lu\n", dataLength, size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

/* helpers ------------------------------------------------------------------ */

static uint16_t
addUChars(const UChar *s, uint16_t length) {
    uint16_t top=ucharsTop+length+1;
    UChar *p;

    if(top>=MAX_UCHAR_COUNT) {
        fprintf(stderr, "genprops: out of memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=uchars+ucharsTop;
    icu_memcpy(p, s, length);
    p[length]=0;
    ucharsTop=top;
    return (uint16_t)(p-uchars);
}
