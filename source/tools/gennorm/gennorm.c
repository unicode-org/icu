/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gennorm.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001may25
*   created by: Markus W. Scherer
*
*   This program reads the Unicode character database text file,
*   parses it, and extracts the data for normalization.
*   It then preprocesses it and writes a binary file for efficient use
*   in various Unicode text normalization processes.
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
#include "unormimp.h"

U_CDECL_BEGIN
#include "gennorm.h"
U_CDECL_END

#ifdef WIN32
#   pragma warning(disable: 4100)
#endif

UBool beVerbose=FALSE, haveCopyright=TRUE;

/* prototypes --------------------------------------------------------------- */

static void
parseDerivedNormalizationProperties(const char *filename, UErrorCode *pErrorCode);

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
            "\t\t                    'gennorm new' will read UnicodeData-new.txt etc.\n",
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

    /* process DerivedNormalizationProperties.txt (quick check flags) */
    if(suffix==NULL) {
        uprv_strcpy(basename, "DerivedNormalizationProperties.txt");
    } else {
        uprv_strcpy(basename, "DerivedNormalizationProperties");
        basename[30]='-';
        uprv_strcpy(basename+31, suffix);
        uprv_strcat(basename+31, ".txt");
    }
    parseDerivedNormalizationProperties(filename, &errorCode);

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
        processData();

        /* write the properties data file */
        generateData(destDir);
    }

    return errorCode;
}

/* parsing helpers ---------------------------------------------------------- */

static const char *
skipWhitespace(const char *s) {
    while(*s==' ' || *s=='\t') {
        ++s;
    }
    return s;
}

/*
 * parse a list of code points
 * store them as a UTF-32 string in dest[destCapacity] with the string length in dest[0]
 * set the first code point in *pFirst
 * return the number of code points
 */
static int32_t
parseCodePoints(const char *s,
                uint32_t *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    char *end;
    uint32_t value;
    int32_t count;

    count=0;
    for(;;) {
        s=skipWhitespace(s);
        if(*s==';' || *s==0) {
            return count;
        }

        /* read one code point */
        value=(uint32_t)uprv_strtoul(s, &end, 16);
        if(end<=s || (*end!=' ' && *end!='\t' && *end!=';') || value>=0x110000) {
            fprintf(stderr, "gennorm: syntax error parsing code point at %s\n", s);
            *pErrorCode=U_PARSE_ERROR;
            return -1;
        }

        /* overflow? */
        if(count>=destCapacity) {
            fprintf(stderr, "gennorm: code point sequence too long at at %s\n", s);
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return -1;
        }

        /* append it to the destination array */
        dest[count++]=value;

        /* go to the following characters */
        s=end;
    }
}

/* read a range like start or start..end */
static int32_t
parseCodePointRange(const char *s,
                    uint32_t *pStart, uint32_t *pEnd,
                    UErrorCode *pErrorCode) {
    char *end;
    uint32_t value;

    s=skipWhitespace(s);
    if(*s==';' || *s==0) {
        fprintf(stderr, "gennorm: syntax error parsing range at %s - empty field\n", s);
        *pErrorCode=U_PARSE_ERROR;
        return -1;
    }

    /* read the start code point */
    value=(uint32_t)uprv_strtoul(s, &end, 16);
    if(end<=s || (*end!=' ' && *end!='\t' && *end!='.' && *end!=';') || value>=0x110000) {
        fprintf(stderr, "gennorm: syntax error parsing range start code point at %s\n", s);
        *pErrorCode=U_PARSE_ERROR;
        return -1;
    }
    *pStart=*pEnd=value;

    /* is there a "..end"? */
    s=skipWhitespace(end);
    if(*s==';' || *s==0) {
        return 1;
    }

    if(*s!='.' || s[1]!='.') {
        fprintf(stderr, "gennorm: syntax error parsing range at %s\n", s);
        *pErrorCode=U_PARSE_ERROR;
        return -1;
    }
    s+=2;

    /* read the end code point */
    value=(uint32_t)uprv_strtoul(s, &end, 16);
    if(end<=s || (*end!=' ' && *end!='\t' && *end!=';') || value>=0x110000) {
        fprintf(stderr, "gennorm: syntax error parsing range end code point at %s\n", s);
        *pErrorCode=U_PARSE_ERROR;
        return -1;
    }
    *pEnd=value;

    /* is this a valid range? */
    if(value<*pStart) {
        fprintf(stderr, "gennorm: syntax error parsing range at %s - not a valid range\n", s);
        *pErrorCode=U_PARSE_ERROR;
        return -1;
    }

    /* no garbage after that? */
    s=skipWhitespace(end);
    if(*s==';' || *s==0) {
        return value-*pStart+1;
    } else {
        fprintf(stderr, "gennorm: syntax error parsing range at %s\n", s);
        *pErrorCode=U_PARSE_ERROR;
        return -1;
    }
}

/* parser for DerivedNormalizationProperties.txt ---------------------------- */

static void
derivedNormalizationPropertiesLineFn(void *context,
                                     char *fields[][2], int32_t fieldCount,
                                     UErrorCode *pErrorCode) {
    char *s;
    uint32_t start, end;
    int32_t count;
    uint8_t qcFlags;

    /* get code point range */
    count=parseCodePointRange(fields[0][0], &start, &end, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "gennorm: error parsing DerivedNormalizationProperties.txt mapping at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }

    /* ignore hangul - handle explicitly */
    if(start==0xac00) {
        return;
    }

    /* get property - ignore unrecognized ones */
    s=(char *)skipWhitespace(fields[1][0]);
    if(*s=='N' && s[1]=='F') {
        qcFlags=0x11;
        s+=2;
        if(*s=='K') {
            qcFlags<<=1;
            ++s;
        }

        if(*s=='C' && s[1]=='_') {
            s+=2;
        } else if(*s=='D' && s[1]=='_') {
            qcFlags<<=2;
            s+=2;
        } else {
            return;
        }

        if(0==uprv_memcmp(s, "NO", 2)) {
            qcFlags&=0xf;
        } else if(0==uprv_memcmp(s, "MAYBE", 5)) {
            qcFlags&=0x30;
        } else {
            return;
        }

        /* set this flag for all code points in this range */
        while(start<=end) {
            setQCFlags(start++, qcFlags);
        }
    } else if(0==uprv_memcmp(s, "Comp_Ex", 7)) {
        while(start<=end) {
            setCompositionExclusion(start++);
        }
    }
}

static void
parseDerivedNormalizationProperties(const char *filename, UErrorCode *pErrorCode) {
    char *fields[2][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 2, derivedNormalizationPropertiesLineFn, NULL, pErrorCode);
}

/* parser for UnicodeData.txt ----------------------------------------------- */

static void
unicodeDataLineFn(void *context,
                  char *fields[][2], int32_t fieldCount,
                  UErrorCode *pErrorCode) {
    uint32_t decomp[40];
    Norm norm;
    const char *s;
    char *end;
    uint32_t code, value;
    int32_t length;
    UBool isCompat, something=FALSE;

    /* ignore First and Last entries for ranges */
    if( *fields[1][0]=='<' &&
        (length=(fields[1][1]-fields[1][0]))>=9 &&
        (0==uprv_memcmp(", First>", fields[1][1]-8, 8) || 0==uprv_memcmp(", Last>", fields[1][1]-7, 7))
    ) {
        return;
    }

    /* reset the properties */
    uprv_memset(&norm, 0, sizeof(Norm));

    /* get the character code, field 0 */
    code=(uint32_t)uprv_strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "gennorm: syntax error in field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* get canonical combining class, field 3 */
    value=(uint32_t)uprv_strtoul(fields[3][0], &end, 10);
    if(end<=fields[3][0] || end!=fields[3][1] || value>0xff) {
        fprintf(stderr, "gennorm: syntax error in field 3 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    if(value>0) {
        norm.udataCC=(uint8_t)value;
        something=TRUE;
    }

    /* get the decomposition, field 5 */
    if(fields[5][0]<fields[5][1]) {
        if(*(s=fields[5][0])=='<') {
            ++s;
            isCompat=TRUE;

            /* skip and ignore the compatibility type name */
            do {
                if(s==fields[5][1]) {
                    /* missing '>' */
                    fprintf(stderr, "gennorm: syntax error in field 5 at %s\n", fields[0][0]);
                    *pErrorCode=U_PARSE_ERROR;
                    exit(U_PARSE_ERROR);
                }
            } while(*s++!='>');
        } else {
            isCompat=FALSE;
        }

        /* parse the decomposition string */
        length=parseCodePoints(s, decomp, sizeof(decomp)/4, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            exit(*pErrorCode);
        }

        /* store the string */
        if(length>0) {
            something=TRUE;
            if(isCompat) {
                norm.lenNFKD=(uint8_t)length;
                norm.nfkd=decomp;
            } else {
                if(length>2) {
                    fprintf(stderr, "gennorm: error - length of NFD(U+%04lx) = %ld >2 in UnicodeData - illegal\n",
                            code, length);
                    *pErrorCode=U_PARSE_ERROR;
                    exit(U_PARSE_ERROR);
                }
                norm.lenNFD=(uint8_t)length;
                norm.nfd=decomp;
            }
        }
    }

    /* check for non-character code points */
    if((code&0xfffe)==0xfffe || (uint32_t)(code-0xfdd0)<0x20 || code>0x10ffff) {
        fprintf(stderr, "gennorm: error - properties for non-character code point U+%04lx\n",
                code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    if(something) {
        /* there are normalization values, so store them */
        if(beVerbose) {
            printf("store values for U+%04lx: cc=%d, lenNFD=%ld, lenNFKD=%ld\n",
                   code, norm.udataCC, norm.lenNFD, norm.lenNFKD);
        }
        storeNorm(code, &norm);
    }
}

static void
parseDB(const char *filename, UErrorCode *pErrorCode) {
    char *fields[15][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 15, unicodeDataLineFn, NULL, pErrorCode);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
