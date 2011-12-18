/*
*******************************************************************************
*
*   Copyright (C) 1999-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec08
*   created by: Markus W. Scherer
*
*   This program reads several of the Unicode character database text files,
*   parses them, and extracts most of the properties for each character.
*   It then writes a binary file containing the properties
*   that is designed to be used directly for random-access to
*   the properties of each Unicode character.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/putil.h"
#include "unicode/uchar.h"
#include "unicode/uclean.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "charstr.h"
#include "cmemory.h"
#include "cstring.h"
#include "genprops.h"
#include "propsvec.h"
#include "ppucd.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "uparse.h"
#include "uprops.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

// TODO: remove
#define USE_NEW 1

U_NAMESPACE_USE

UBool beVerbose=FALSE;

PropsWriter::~PropsWriter() {}
void PropsWriter::setUnicodeVersion(const UVersionInfo) {}
void PropsWriter::setProps(const UniProps &, const UnicodeSet &, UErrorCode &) {}
void PropsWriter::finalizeData(UErrorCode &) {}
void PropsWriter::writeCSourceFile(const char *, UErrorCode &) {}
void PropsWriter::writeBinaryData(const char *, UBool, UErrorCode &) {}

/* prototypes --------------------------------------------------------------- */

static void
parseDB(const char *filename, UErrorCode *pErrorCode);

/* -------------------------------------------------------------------------- */

enum
{
    HELP_H,
    HELP_QUESTION_MARK,
    VERBOSE,
    COPYRIGHT,
    SOURCEDIR,
    ICUDATADIR
};

/* Keep these values in sync with the above enums */
static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT,
    UOPTION_SOURCEDIR,
    UOPTION_ICUDATADIR
};

extern int
main(int argc, char* argv[]) {
    char filename[300];
    const char *srcDir=NULL;
    char *basename=NULL;

    U_MAIN_INIT_ARGS(argc, argv);

    /* preset then read command line options */
    options[SOURCEDIR].value="";
    options[ICUDATADIR].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, LENGTHOF(options), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<2 || options[HELP_H].doesOccur || options[HELP_QUESTION_MARK].doesOccur) {
        /*
         * Broken into chunks because the C89 standard says the minimum
         * required supported string length is 509 bytes.
         */
        fprintf(stderr,
            "Usage: %s [-options] path/to/ICU/src/root\n"
            "\n"
            "Reads the preparsed UCD file path/to/ICU/src/root/source/data/unidata/ppucd.txt and\n"
            "writes source and binary data files with the character properties.\n"
            "(UCD=Unicode Character Database)\n"
            "\n",
            argv[0]);
        fprintf(stderr,
            "Options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-v or --verbose     verbose output\n"
            "\t-c or --copyright   include a copyright notice\n");
        fprintf(stderr,
            "\t-s or --sourcedir   source directory, followed by the path\n"
            "\t-i or --icudatadir  directory for locating any needed intermediate data files,\n"
            "\t                    followed by path, defaults to %s\n",
            u_getDataDirectory());
        return argc<2 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    /* get the options values */
    beVerbose=options[VERBOSE].doesOccur;
    srcDir=options[SOURCEDIR].value;

    /* initialize */
    IcuToolErrorCode errorCode("genprops");
    LocalPointer<PropsWriter> corePropsWriter(createCorePropsWriter(errorCode));
    LocalPointer<PropsWriter> props2Writer(createProps2Writer(errorCode));
    if(errorCode.isFailure()) {
        fprintf(stderr, "genprops: unable to create PropsWriters - %s\n", errorCode.errorName());
        return errorCode.reset();
    }

    CharString icuSrcRoot(argv[1], errorCode);

    CharString icuSource(icuSrcRoot, errorCode);
    icuSource.appendPathPart("source", errorCode);

    CharString icuSourceData(icuSource, errorCode);
    icuSourceData.appendPathPart("data", errorCode);

    CharString ppucdPath(icuSourceData, errorCode);
    ppucdPath.appendPathPart("unidata", errorCode);
    ppucdPath.appendPathPart("ppucd.txt", errorCode);

    PreparsedUCD ppucd(ppucdPath.data(), errorCode);
    if(errorCode.isFailure()) {
        fprintf(stderr, "genprops: unable to open %s - %s\n",
                ppucdPath.data(), errorCode.errorName());
        return errorCode.reset();
    }
    PreparsedUCD::LineType lineType;
    UnicodeSet newValues;
    while((lineType=ppucd.readLine(errorCode))!=PreparsedUCD::NO_LINE) {
        if(ppucd.lineHasPropertyValues()) {
            const UniProps *props=ppucd.getProps(newValues, errorCode);
            props2Writer->setProps(*props, newValues, errorCode);
        } else if(lineType==PreparsedUCD::UNICODE_VERSION_LINE) {
            const UVersionInfo &version=ppucd.getUnicodeVersion();
            corePropsWriter->setUnicodeVersion(version);
        }
        if(errorCode.isFailure()) {
            fprintf(stderr,
                    "genprops: error parsing or setting values from ppucd.txt line %ld - %s\n",
                    (long)ppucd.getLineNumber(), errorCode.errorName());
            return errorCode.reset();
        }
    }

    if (options[ICUDATADIR].doesOccur) {
        u_setDataDirectory(options[ICUDATADIR].value);
    }

    /* prepare the filename beginning with the source dir */
    uprv_strcpy(filename, srcDir);
    basename=filename+uprv_strlen(filename);
    if(basename>filename && *(basename-1)!=U_FILE_SEP_CHAR) {
        *basename++=U_FILE_SEP_CHAR;
    }

    /* process UnicodeData.txt */
    writeUCDFilename(basename, "UnicodeData", NULL);
    parseDB(filename, errorCode);

    /* process additional properties files */
    *basename=0;
    generateAdditionalProperties(filename, NULL, errorCode);

    corePropsWriter->finalizeData(errorCode);
    if(errorCode.isFailure()) {
        fprintf(stderr, "genprops error: failure finalizing the data - %s\n",
                errorCode.errorName());
        return errorCode.reset();
    }

    // Write the files with the generated data.
    CharString sourceCommon(icuSource, errorCode);
    sourceCommon.appendPathPart("common", errorCode);

    CharString sourceDataIn(icuSourceData, errorCode);
    sourceDataIn.appendPathPart("in", errorCode);

    UBool withCopyright=options[COPYRIGHT].doesOccur;

    corePropsWriter->writeCSourceFile(sourceCommon.data(), errorCode);
    corePropsWriter->writeBinaryData(sourceDataIn.data(), withCopyright, errorCode);

    return errorCode;
}

U_CFUNC void
writeUCDFilename(char *basename, const char *filename, const char *suffix) {
    int32_t length=(int32_t)uprv_strlen(filename);
    uprv_strcpy(basename, filename);
    if(suffix!=NULL) {
        basename[length++]='-';
        uprv_strcpy(basename+length, suffix);
        length+=(int32_t)uprv_strlen(suffix);
    }
    uprv_strcpy(basename+length, ".txt");
}

U_CFUNC UBool
isToken(const char *token, const char *s) {
    const char *z;
    int32_t j;

    s=u_skipWhitespace(s);
    for(j=0;; ++j) {
        if(token[j]!=0) {
            if(s[j]!=token[j]) {
                break;
            }
        } else {
            z=u_skipWhitespace(s+j);
            if(*z==';' || *z==0) {
                return TRUE;
            } else {
                break;
            }
        }
    }

    return FALSE;
}

U_CFUNC int32_t
getTokenIndex(const char *const tokens[], int32_t countTokens, const char *s) {
    const char *t, *z;
    int32_t i, j;

    s=u_skipWhitespace(s);
    for(i=0; i<countTokens; ++i) {
        t=tokens[i];
        if(t!=NULL) {
            for(j=0;; ++j) {
                if(t[j]!=0) {
                    if(s[j]!=t[j]) {
                        break;
                    }
                } else {
                    z=u_skipWhitespace(s+j);
                    if(*z==';' || *z==0 || *z=='#' || *z=='\r' || *z=='\n') {
                        return i;
                    } else {
                        break;
                    }
                }
            }
        }
    }
    return -1;
}

/* parser for UnicodeData.txt ----------------------------------------------- */

/* general categories */
const char *const
genCategoryNames[U_CHAR_CATEGORY_COUNT]={
    "Cn",
    "Lu", "Ll", "Lt", "Lm", "Lo", "Mn", "Me",
    "Mc", "Nd", "Nl", "No",
    "Zs", "Zl", "Zp",
    "Cc", "Cf", "Co", "Cs",
    "Pd", "Ps", "Pe", "Pc", "Po",
    "Sm", "Sc", "Sk", "So",
    "Pi", "Pf"
};

static struct {
    uint32_t first, last, props;
    char name[80];
} unicodeAreas[32];

static int32_t unicodeAreaIndex=0;

static void U_CALLCONV
unicodeDataLineFn(void *context,
                  char *fields[][2], int32_t fieldCount,
                  UErrorCode *pErrorCode) {
    Props p;
    char *end;
    static uint32_t prevCode=0;
    uint32_t value;
    int32_t i;

    /* reset the properties */
    uprv_memset(&p, 0, sizeof(Props));

    /* get the character code, field 0 */
    p.code=(uint32_t)uprv_strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "genprops: syntax error in field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* get general category, field 2 */
    i=getTokenIndex(genCategoryNames, U_CHAR_CATEGORY_COUNT, fields[2][0]);
    if(i>=0) {
        p.generalCategory=(uint8_t)i;
    } else {
        fprintf(stderr, "genprops: unknown general category \"%s\" at code 0x%lx\n",
            fields[2][0], (unsigned long)p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* decimal digit value, field 6 */
    if(fields[6][0]<fields[6][1]) {
        value=(uint32_t)uprv_strtoul(fields[6][0], &end, 10);
        if(end!=fields[6][1] || value>0x7fff) {
            fprintf(stderr, "genprops: syntax error in field 6 at code 0x%lx\n",
                (unsigned long)p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
        p.numericValue=(int32_t)value;
        p.numericType=1;
    }

    /* digit value, field 7 */
    if(fields[7][0]<fields[7][1]) {
        value=(uint32_t)uprv_strtoul(fields[7][0], &end, 10);
        if(end!=fields[7][1] || value>0x7fff) {
            fprintf(stderr, "genprops: syntax error in field 7 at code 0x%lx\n",
                (unsigned long)p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
        if(p.numericType==0) {
            p.numericValue=(int32_t)value;
            p.numericType=2;
        } else if((int32_t)value!=p.numericValue) {
            fprintf(stderr, "genprops error: numeric values in fields 6 & 7 different at code 0x%lx\n",
                (unsigned long)p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
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

        value=(uint32_t)uprv_strtoul(s, &end, 10);
        if(value>0 && *end=='/') {
            /* field 8 may contain a fractional value, get the denominator */
            if(p.numericType>0) {
                fprintf(stderr, "genprops error: numeric values in fields 6..8 different at code 0x%lx\n",
                    (unsigned long)p.code);
                *pErrorCode=U_PARSE_ERROR;
                exit(U_PARSE_ERROR);
            }

            p.denominator=(uint32_t)uprv_strtoul(end+1, &end, 10);
            if(p.denominator==0) {
                fprintf(stderr, "genprops: denominator is 0 in field 8 at code 0x%lx\n",
                    (unsigned long)p.code);
                *pErrorCode=U_PARSE_ERROR;
                exit(U_PARSE_ERROR);
            }
        }
        if(end!=fields[8][1] || value>0x7fffffff) {
            fprintf(stderr, "genprops: syntax error in field 8 at code 0x%lx\n",
                (unsigned long)p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }

        if(p.numericType==0) {
            if(isNegative) {
                p.numericValue=-(int32_t)value;
            } else {
                p.numericValue=(int32_t)value;
            }
            p.numericType=3;
        } else if((int32_t)value!=p.numericValue) {
            fprintf(stderr, "genprops error: numeric values in fields 6..8 different at code 0x%lx\n",
                (unsigned long)p.code);
            *pErrorCode=U_PARSE_ERROR;
            exit(U_PARSE_ERROR);
        }
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
                        (unsigned long)unicodeAreas[unicodeAreaIndex].first,
                        (unsigned long)unicodeAreas[unicodeAreaIndex].last,
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

    /* check for non-character code points */
    if((p.code&0xfffe)==0xfffe || (uint32_t)(p.code-0xfdd0)<0x20) {
        fprintf(stderr, "genprops: error - properties for non-character code point U+%04lx\n",
                (unsigned long)p.code);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    /* check that the code points (p.code) are in ascending order */
    if(p.code<=prevCode && p.code>0) {
        fprintf(stderr, "genprops: error - UnicodeData entries out of order, U+%04lx after U+%04lx\n",
                (unsigned long)p.code, (unsigned long)prevCode);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }
    prevCode=p.code;

    /* properties for a single code point */
    addProps(p.code, value);
}

/* set repeated properties for the areas */
static void
repeatAreaProps() {
    uint32_t puaProps;
    int32_t i;
    UBool hasPlane15PUA, hasPlane16PUA;
    UErrorCode errorCode;

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
            (unsigned long)unicodeAreas[unicodeAreaIndex].first);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    repeatAreaProps();

    if(U_FAILURE(*pErrorCode)) {
        return;
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
