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
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "genprops.h"

extern bool_t beVerbose=FALSE, haveCopyright=TRUE;

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

/* prototypes --------------------------------------------------------------- */

static void
init(void);

static void
parseDB(FileStream *in);

static int16_t
getField(char *line, int16_t start, int16_t limit);

static void
checkLineIndex(uint32_t code, int16_t limit, int16_t length);

/* -------------------------------------------------------------------------- */

extern int
main(int argc, char *argv[]) {
    FileStream *in;
    const char *destdir = 0;
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

    if (!destdir) {
        destdir = u_getDataDir();
    }

    init();
    initStore();
    parseDB(in);
    repeatProps();
    compactProps();
    compactStage3();
    compactStage2();
    generateData(destdir);

    if(in!=T_FileStream_stdin()) {
        T_FileStream_close(in);
    }

    return 0;
}

static void
init(void) {
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
        length=uprv_strlen(line);

        /* remove trailing newline characters */
        while(length>0 && (line[length-1]=='\r' || line[length-1]=='\n')) {
            line[--length]=0;
        }

        /* reset the properties */
        uprv_memset(&p, 0, sizeof(p));
        hasNumericValue=FALSE;

        /* get the character code, field 0 */
        p.code=uprv_strtoul(line, &end, 16);
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
            if(uprv_strcmp(line+start, genCategoryNames[i])==0) {
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
        p.canonicalCombining=(uint8_t)uprv_strtoul(line+start, &end, 10);
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
            if(uprv_strcmp(line+start, bidiNames[i])==0) {
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
        value=uprv_strtoul(line+start, &end, 10);
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
        value=uprv_strtoul(line+start, &end, 10);
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
        value=uprv_strtoul(line+start, &end, 10);
        if(value>0 && *end=='/') {
            p.denominator=uprv_strtoul(end+1, &end, 10);
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
        p.upperCase=uprv_strtoul(line+start, &end, 16);
        limit=end-line;
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 12 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* get lowercase mapping, field 13 */
        start=limit+1;
        checkLineIndex(p.code, start, length);
        p.lowerCase=uprv_strtoul(line+start, &end, 16);
        limit=end-line;
        if(*end!=';') {
            fprintf(stderr, "genprops: syntax error in field 13 at code 0x%lx\n", p.code);
            exit(U_PARSE_ERROR);
        }

        /* get titlecase mapping, field 14 */
        start=limit+1;
        if(start<length) {
            /* this is the last field */
            p.titleCase=uprv_strtoul(line+start, &end, 16);
            if(*end!=';' && *end!=0) {
                fprintf(stderr, "genprops: syntax error in field 14 at code 0x%lx\n", p.code);
                exit(U_PARSE_ERROR);
            }
        }

#if 0
        /* debug output */
        if(beVerbose) {
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
        }
#endif

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

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
