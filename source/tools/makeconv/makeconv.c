/*
 ********************************************************************************
 *
 *   Copyright (C) 1998-2004, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 ********************************************************************************
 *
 *
 *  makeconv.c:
 *  tool creating a binary (compressed) representation of the conversion mapping
 *  table (IBM NLTC ucmap format).
 *
 *  05/04/2000    helena     Added fallback mapping into the picture...
 *  06/29/2000  helena      Major rewrite of the callback APIs.
 */

#include <stdio.h>
#include "unicode/putil.h"
#include "ucnv_io.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnv_imp.h"
#include "ucnv_cnv.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"
#include "toolutil.h"
#include "uoptions.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "uparse.h"
#include "ucm.h"
#include "makeconv.h"
#include "genmbcs.h"

#define DEBUG 0

typedef struct ConvData {
    UCMFile *ucm;
    NewConverter *cnvData, *extData;
    UConverterSharedData sharedData;
    UConverterStaticData staticData;
} ConvData;

static void
initConvData(ConvData *data) {
    uprv_memset(data, 0, sizeof(ConvData));
    data->sharedData.structSize=sizeof(UConverterSharedData);
    data->staticData.structSize=sizeof(UConverterStaticData);
    data->sharedData.staticData=&data->staticData;
}

static void
cleanupConvData(ConvData *data) {
    if(data!=NULL) {
        if(data->cnvData!=NULL) {
            data->cnvData->close(data->cnvData);
            data->cnvData=NULL;
        }
        if(data->extData!=NULL) {
            data->extData->close(data->extData);
            data->extData=NULL;
        }
        ucm_close(data->ucm);
        data->ucm=NULL;
    }
}

/*
 * from ucnvstat.c - static prototypes of data-based converters
 */
extern const UConverterStaticData * ucnv_converterStaticData[UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES];

/*
 * Global - verbosity
 */
UBool VERBOSE = FALSE;
UBool TOUCHFILE = FALSE;

static void
createConverter(ConvData *data, const char* converterName, UErrorCode *pErrorCode);

/*
 * Set up the UNewData and write the converter..
 */
static void
writeConverterData(ConvData *data, const char *cnvName, const char *cnvDir, UErrorCode *status);

UBool haveCopyright=TRUE;

static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x63, 0x6e, 0x76, 0x74},     /* dataFormat="cnvt" */
    {6, 2, 0, 0},                 /* formatVersion */
    {0, 0, 0, 0}                  /* dataVersion (calculated at runtime) */
};

static void
writeConverterData(ConvData *data, const char *cnvName, const char *cnvDir, UErrorCode *status)
{
    UNewDataMemory *mem = NULL;
    uint32_t sz2;
    uint32_t size = 0;
    int32_t tableType;

    if(U_FAILURE(*status))
      {
        return;
      }

    tableType=TABLE_NONE;
    if(data->cnvData!=NULL) {
        tableType|=TABLE_BASE;
    }
    if(data->extData!=NULL) {
        tableType|=TABLE_EXT;
    }

    mem = udata_create(cnvDir, "cnv", cnvName, &dataInfo, haveCopyright ? U_COPYRIGHT_STRING : NULL, status);

    if(U_FAILURE(*status))
      {
        fprintf(stderr, "Couldn't create the udata %s.%s: %s\n",
                cnvName,
                "cnv",
                u_errorName(*status));
        return;
      }

    if(VERBOSE)
      {
        fprintf(stderr, "- Opened udata %s.%s\n", cnvName, "cnv");
      }


    /* all read only, clean, platform independent data.  Mmmm. :)  */
    udata_writeBlock(mem, &data->staticData, sizeof(UConverterStaticData));
    size += sizeof(UConverterStaticData); /* Is 4-aligned  - by size */
    /* Now, write the table */
    if(tableType&TABLE_BASE) {
        size += data->cnvData->write(data->cnvData, &data->staticData, mem, tableType);
    }
    if(tableType&TABLE_EXT) {
        size += data->extData->write(data->extData, &data->staticData, mem, tableType);
    }

    sz2 = udata_finish(mem, status);
    if(size != sz2)
    {
        fprintf(stderr, "error: wrote %u bytes to the .cnv file but counted %u bytes\n", (int)sz2, (int)size);
        *status=U_INTERNAL_PROGRAM_ERROR;
    }
    if(VERBOSE)
    {
      fprintf(stderr, "- Wrote %u bytes to the udata.\n", (int)sz2);
    }
}

static UOption options[]={
    UOPTION_HELP_H,              /* 0  Numbers for those who*/
    UOPTION_HELP_QUESTION_MARK,  /* 1   can't count. */
    UOPTION_COPYRIGHT,           /* 2 */
    UOPTION_VERSION,             /* 3 */
    UOPTION_DESTDIR,             /* 4 */
    UOPTION_VERBOSE,             /* 5 */
    UOPTION_PACKAGE_NAME,        /* 6 */
	UOPTION_DEF( "touchfile", 't', UOPT_NO_ARG) /* 7 */
};

int main(int argc, char* argv[])
{
    ConvData data;
    UErrorCode err = U_ZERO_ERROR, localError;
    char outFileName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
    char touchFileName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
    const char* destdir, *arg;
    const char *pkgName = NULL;
    size_t destdirlen;
    char* dot = NULL, *outBasename;
    char cnvName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
    char cnvNameWithPkg[UCNV_MAX_FULL_FILE_NAME_LENGTH];
    UVersionInfo icuVersion;
    UBool printFilename;

    err = U_ZERO_ERROR;

    U_MAIN_INIT_ARGS(argc, argv);

    /* Set up the ICU version number */
    u_getVersion(icuVersion);
    uprv_memcpy(&dataInfo.dataVersion, &icuVersion, sizeof(UVersionInfo));

    /* preset then read command line options */
    options[4].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    } else if(argc<2) {
        argc=-1;
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] files...\n"
            "\tread .ucm codepage mapping files and write .cnv files\n"
            "options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-V or --version     show a version message\n"
            "\t-c or --copyright   include a copyright notice\n"
            "\t-d or --destdir     destination directory, followed by the path\n"
            "\t-v or --verbose     Turn on verbose output\n",
            argv[0]);
        fprintf(stderr,
            "\t-p or --pkgname     sets the 'package' name for output files.\n"
            "\t                    If name is ICUDATA, then the default icu package\n"
            "\t                    name will be used.\n"
            "\t-t or --touchfile   Generate additional small file without packagename, for nmake\n");
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[3].doesOccur) {
      fprintf(stderr,"makeconv version %hu.%hu, ICU tool to read .ucm codepage mapping files and write .cnv files\n",
            dataInfo.formatVersion[0], dataInfo.formatVersion[1]);
      fprintf(stderr, "Copyright (C) 1998-2000, International Business Machines\n");
      fprintf(stderr,"Corporation and others.  All Rights Reserved.\n");
        exit(0);
    }

   TOUCHFILE = options[7].doesOccur;

   if(!options[6].doesOccur)
    {
      pkgName=NULL;
    }
    else
    {
        pkgName =options[6].value;
        if(!strcmp(pkgName, "ICUDATA"))
        {
            pkgName = U_ICUDATA_NAME;
        }
        if(pkgName[0] == 0)
        {
            pkgName = NULL;

            if(TOUCHFILE)
            {
                fprintf(stderr, "%s: Don't use touchfile option with an empty packagename.\n",
                        argv[0]);
                exit(1);
            }
        }
    }

    /* get the options values */
    haveCopyright = options[2].doesOccur;
    destdir = options[4].value;
    VERBOSE = options[5].doesOccur;

    if (destdir != NULL && *destdir != 0) {
        uprv_strcpy(outFileName, destdir);
        destdirlen = uprv_strlen(destdir);
        outBasename = outFileName + destdirlen;
        if (*(outBasename - 1) != U_FILE_SEP_CHAR) {
            *outBasename++ = U_FILE_SEP_CHAR;
            ++destdirlen;
        }
    } else {
        destdirlen = 0;
        outBasename = outFileName;
    }

#if DEBUG
    {
      int i;
      printf("makeconv: processing %d files...\n", argc - 1);
      for(i=1; i<argc; ++i) {
        printf("%s ", argv[i]);
      }
      printf("\n");
      fflush(stdout);
    }
#endif

    err = U_ZERO_ERROR;
    printFilename = (UBool) (argc > 2 || VERBOSE);
    for (++argv; --argc; ++argv)
    {
        arg = getLongPathname(*argv);

        /*produces the right destination path for display*/
        if (destdirlen != 0)
        {
            const char *basename;

            /* find the last file sepator */
            basename = findBasename(arg);
            uprv_strcpy(outBasename, basename);
        }
        else
        {
            uprv_strcpy(outFileName, arg);
        }

        /*removes the extension if any is found*/
        dot = uprv_strrchr(outBasename, '.');
        if (dot)
        {
            *dot = '\0';
        }

        /* the basename without extension is the converter name */
        uprv_strcpy(cnvName, outBasename);

        if(TOUCHFILE)
        {
            uprv_strcpy(touchFileName, outBasename);
            uprv_strcat(touchFileName, ".cnv");
        }

        if(pkgName != NULL)
        {
            /* changes both basename and filename */
            uprv_strcpy(outBasename, pkgName);
            uprv_strcat(outBasename, "_");
            uprv_strcat(outBasename, cnvName);
        }


        /*Adds the target extension*/
        uprv_strcat(outBasename, CONVERTER_FILE_EXTENSION);

#if DEBUG
        printf("makeconv: processing %s  ...\n", arg);
        fflush(stdout);
#endif
        localError = U_ZERO_ERROR;
        initConvData(&data);
        createConverter(&data, arg, &localError);

        if (U_FAILURE(localError))
        {
            /* if an error is found, print out an error msg and keep going */
            fprintf(stderr, "Error creating converter for \"%s\" file for \"%s\" (%s)\n", outFileName, arg,
                u_errorName(localError));
            if(U_SUCCESS(err)) {
                err = localError;
            }
        }
        else
        {
            /* Make the static data name equal to the file name */
            if( /*VERBOSE &&  */ uprv_stricmp(cnvName,data.staticData.name))
            {
                fprintf(stderr, "Warning: %s%s claims to be '%s'\n",
                    cnvName,
                    CONVERTER_FILE_EXTENSION,
                    data.staticData.name);
            }

            uprv_strcpy((char*)data.staticData.name, cnvName);

            if(!uprv_isInvariantString((char*)data.staticData.name, -1)) {
                fprintf(stderr,
                    "Error: A converter name must contain only invariant characters.\n"
                    "%s is not a valid converter name.\n",
                    data.staticData.name);
                if(U_SUCCESS(err)) {
                    err = U_INVALID_TABLE_FORMAT;
                }
            }

            if(pkgName == NULL)
            {
                uprv_strcpy(cnvNameWithPkg, cnvName);
            }
            else
            {
                uprv_strcpy(cnvNameWithPkg, pkgName);
                uprv_strcat(cnvNameWithPkg, "_");
                uprv_strcat(cnvNameWithPkg, cnvName);
            }

            localError = U_ZERO_ERROR;
            writeConverterData(&data, cnvNameWithPkg, destdir, &localError);
            if(TOUCHFILE)
            {
                FileStream *q;
                char msg[1024];

                sprintf(msg, "This empty file tells nmake that %s in package %s has been updated.\n",
                    cnvName, pkgName);

                q = T_FileStream_open(touchFileName, "w");
                if(q == NULL)
                {
                    fprintf(stderr, "Error writing touchfile \"%s\"\n", touchFileName);
                    localError = U_FILE_ACCESS_ERROR;
                }

                else
                {
                    T_FileStream_write(q, msg, (int32_t)uprv_strlen(msg));
                    T_FileStream_close(q);
                }
            }

            if(U_FAILURE(localError))
            {
                /* if an error is found, print out an error msg and keep going*/
                fprintf(stderr, "Error writing \"%s\" file for \"%s\" (%s)\n", outFileName, arg,
                    u_errorName(localError));
                if(U_SUCCESS(err)) {
                    err = localError;
                }
            }
            else if (printFilename)
            {
                puts(outFileName);
            }
        }
        fflush(stdout);
        fflush(stderr);

        cleanupConvData(&data);
    }

    return err;
}

static void
getPlatformAndCCSIDFromName(const char *name, int8_t *pPlatform, int32_t *pCCSID) {
    if( (name[0]=='i' || name[0]=='I') &&
        (name[1]=='b' || name[1]=='B') &&
        (name[2]=='m' || name[2]=='M')
    ) {
        name+=3;
        if(*name=='-') {
            ++name;
        }
        *pPlatform=UCNV_IBM;
        *pCCSID=(int32_t)uprv_strtoul(name, NULL, 10);
    } else {
        *pPlatform=UCNV_UNKNOWN;
        *pCCSID=0;
    }
}

static void
readHeader(ConvData *data,
           FileStream* convFile,
           const char* converterName,
           UErrorCode *pErrorCode) {
    char line[200];
    char *s, *key, *value;
    const UConverterStaticData *prototype;
    UConverterStaticData *staticData;

    if(U_FAILURE(*pErrorCode)) {
        return;
    }

    staticData=&data->staticData;
    staticData->platform=UCNV_IBM;
    staticData->subCharLen=0;

    while(T_FileStream_readLine(convFile, line, sizeof(line))) {
        /* basic parsing and handling of state-related items */
        if(ucm_parseHeaderLine(data->ucm, line, &key, &value)) {
            continue;
        }

        /* stop at the beginning of the mapping section */
        if(uprv_strcmp(line, "CHARMAP")==0) {
            break;
        }

        /* collect the information from the header field, ignore unknown keys */
        if(uprv_strcmp(key, "code_set_name")==0) {
            if(*value!=0) {
                uprv_strcpy((char *)staticData->name, value);
                getPlatformAndCCSIDFromName(value, &staticData->platform, &staticData->codepage);
            }
        } else if(uprv_strcmp(key, "subchar")==0) {
            uint8_t bytes[UCNV_EXT_MAX_BYTES];
            int8_t length;

            s=value;
            length=ucm_parseBytes(bytes, line, (const char **)&s);
            if(1<=length && length<=4 && *s==0) {
                staticData->subCharLen=length;
                uprv_memcpy(staticData->subChar, bytes, length);
            } else {
                fprintf(stderr, "error: illegal <subchar> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        } else if(uprv_strcmp(key, "subchar1")==0) {
            uint8_t bytes[UCNV_EXT_MAX_BYTES];

            s=value;
            if(1==ucm_parseBytes(bytes, line, (const char **)&s) && *s==0) {
                staticData->subChar1=bytes[0];
            } else {
                fprintf(stderr, "error: illegal <subchar1> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        }
    }

    /* copy values from the UCMFile to the static data */
    staticData->maxBytesPerChar=(int8_t)data->ucm->states.maxCharLength;
    staticData->minBytesPerChar=(int8_t)data->ucm->states.minCharLength;
    staticData->conversionType=data->ucm->states.conversionType;

    if(staticData->conversionType==UCNV_UNSUPPORTED_CONVERTER) {
        fprintf(stderr, "ucm error: missing conversion type (<uconv_class>)\n");
        *pErrorCode=U_INVALID_TABLE_FORMAT;
        return;
    }

    /*
     * Now that we know the type, copy any 'default' values from the table.
     * We need not check the type any further because the parser only
     * recognizes what we have prototypes for.
     *
     * For delta (extension-only) tables, copy values from the base file
     * instead, see createConverter().
     */
    if(data->ucm->baseName[0]==0) {
        prototype=ucnv_converterStaticData[staticData->conversionType];
        if(prototype!=NULL) {
            if(staticData->name[0]==0) {
                uprv_strcpy((char *)staticData->name, prototype->name);
            }

            if(staticData->codepage==0) {
                staticData->codepage=prototype->codepage;
            }

            if(staticData->platform==0) {
                staticData->platform=prototype->platform;
            }

            if(staticData->minBytesPerChar==0) {
                staticData->minBytesPerChar=prototype->minBytesPerChar;
            }

            if(staticData->maxBytesPerChar==0) {
                staticData->maxBytesPerChar=prototype->maxBytesPerChar;
            }

            if(staticData->subCharLen==0) {
                staticData->subCharLen=prototype->subCharLen;
                if(prototype->subCharLen>0) {
                    uprv_memcpy(staticData->subChar, prototype->subChar, prototype->subCharLen);
                }
            }
        }
    }

    if(data->ucm->states.outputType<0) {
        data->ucm->states.outputType=(int8_t)data->ucm->states.maxCharLength-1;
    }

    if( staticData->subChar1!=0 &&
            (staticData->minBytesPerChar>1 ||
                (staticData->conversionType!=UCNV_MBCS &&
                 staticData->conversionType!=UCNV_EBCDIC_STATEFUL))
    ) {
        fprintf(stderr, "error: <subchar1> defined for a type other than MBCS or EBCDIC_STATEFUL\n");
        *pErrorCode=U_INVALID_TABLE_FORMAT;
    }
}

/* return TRUE if a base table was read, FALSE for an extension table */
static UBool
readFile(ConvData *data, const char* converterName,
         UErrorCode *pErrorCode) {
    char line[200];
    char *end;
    FileStream *convFile;

    UCMStates *baseStates;
    UBool dataIsBase;

    if(U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    data->ucm=ucm_open();

    convFile=T_FileStream_open(converterName, "r");
    if(convFile==NULL) {
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return FALSE;
    }

    readHeader(data, convFile, converterName, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    if(data->ucm->baseName[0]==0) {
        dataIsBase=TRUE;
        baseStates=&data->ucm->states;
        ucm_processStates(baseStates);
    } else {
        dataIsBase=FALSE;
        baseStates=NULL;
    }

    /* read the base table */
    ucm_readTable(data->ucm, convFile, dataIsBase, baseStates, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    /* read an extension table if there is one */
    while(T_FileStream_readLine(convFile, line, sizeof(line))) {
        end=uprv_strchr(line, 0);
        while(line<end &&
              (*(end-1)=='\n' || *(end-1)=='\r' || *(end-1)==' ' || *(end-1)=='\t')) {
            --end;
        }
        *end=0;

        if(line[0]=='#' || u_skipWhitespace(line)==end) {
            continue; /* ignore empty and comment lines */
        }

        if(0==uprv_strcmp(line, "CHARMAP")) {
            /* read the extension table */
            ucm_readTable(data->ucm, convFile, FALSE, baseStates, pErrorCode);
        } else {
            fprintf(stderr, "unexpected text after the base mapping table\n");
        }
        break;
    }

    T_FileStream_close(convFile);

    if(data->ucm->base->flagsType==UCM_FLAGS_MIXED || data->ucm->ext->flagsType==UCM_FLAGS_MIXED) {
        fprintf(stderr, "error: some entries have the mapping precision (with '|'), some do not\n");
        *pErrorCode=U_INVALID_TABLE_FORMAT;
    }

    return dataIsBase;
}

static void
createConverter(ConvData *data, const char *converterName, UErrorCode *pErrorCode) {
    ConvData baseData;
    UBool dataIsBase;

    UConverterStaticData *staticData;
    UCMStates *states, *baseStates;

    if(U_FAILURE(*pErrorCode)) {
        return;
    }

    initConvData(data);

    dataIsBase=readFile(data, converterName, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return;
    }

    staticData=&data->staticData;
    states=&data->ucm->states;

    if(dataIsBase) {
        data->cnvData=MBCSOpen(data->ucm);
        if(data->cnvData==NULL) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;

        } else if(!data->cnvData->isValid(data->cnvData,
                            staticData->subChar, staticData->subCharLen)
        ) {
            fprintf(stderr, "       the substitution character byte sequence is illegal in this codepage structure!\n");
            *pErrorCode=U_INVALID_TABLE_FORMAT;

        } else if(staticData->subChar1!=0 &&
                    !data->cnvData->isValid(data->cnvData, &staticData->subChar1, 1)
        ) {
            fprintf(stderr, "       the subchar1 byte is illegal in this codepage structure!\n");
            *pErrorCode=U_INVALID_TABLE_FORMAT;

        } else if(data->ucm->ext->mappingsLength>0) {
            /* prepare the extension table, if there is one */
            data->extData=CnvExtOpen(data->ucm);
            if(data->extData==NULL) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;

            } else if(
                !ucm_checkBaseExt(states, data->ucm->base, data->ucm->ext, data->ucm->ext, FALSE) ||
                !data->extData->addTable(data->extData, data->ucm->ext, &data->staticData)
            ) {
                *pErrorCode=U_INVALID_TABLE_FORMAT;
            }
        }

        /* add the base table after ucm_checkBaseExt()! */
        if( U_SUCCESS(*pErrorCode) &&
            !data->cnvData->addTable(data->cnvData, data->ucm->base, &data->staticData)
        ) {
            *pErrorCode=U_INVALID_TABLE_FORMAT;
        }
    } else {
        char baseFilename[500];
        char *basename;

        initConvData(&baseData);

        /* assemble a path/filename for data->ucm->baseName */
        uprv_strcpy(baseFilename, converterName);
        basename=(char *)findBasename(baseFilename);
        uprv_strcpy(basename, data->ucm->baseName);
        uprv_strcat(basename, ".ucm");

        /* read the base table */
        dataIsBase=readFile(&baseData, baseFilename, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return;
        } else if(!dataIsBase) {
            fprintf(stderr, "error: the <icu:base> file \"%s\" is not a base table file\n", baseFilename);
            *pErrorCode=U_INVALID_TABLE_FORMAT;
        } else {
            /* prepare the extension table */
            data->extData=CnvExtOpen(data->ucm);
            if(data->extData==NULL) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;

            } else {
                /* fill in gaps in extension file header fields */
                UCMapping *m, *mLimit;
                uint8_t fallbackFlags;

                baseStates=&baseData.ucm->states;
                if(states->conversionType==UCNV_DBCS) {
                    staticData->minBytesPerChar=(int8_t)(states->minCharLength=2);
                } else if(states->minCharLength==0) {
                    staticData->minBytesPerChar=(int8_t)(states->minCharLength=baseStates->minCharLength);
                }
                if(states->maxCharLength<states->minCharLength) {
                    staticData->maxBytesPerChar=(int8_t)(states->maxCharLength=baseStates->maxCharLength);
                }

                if(staticData->subCharLen==0) {
                    uprv_memcpy(staticData->subChar, baseData.staticData.subChar, 4);
                    staticData->subCharLen=baseData.staticData.subCharLen;
                }
                /*
                 * do not copy subChar1 -
                 * only use what is explicitly specified
                 * because it cannot be unset in the extension file header
                 */

                /* get the fallback flags */
                fallbackFlags=0;
                for(m=baseData.ucm->base->mappings, mLimit=m+baseData.ucm->base->mappingsLength;
                    m<mLimit && fallbackFlags!=3;
                    ++m
                ) {
                    if(m->f==1) {
                        fallbackFlags|=1;
                    } else if(m->f==3) {
                        fallbackFlags|=2;
                    }
                }
                for(m=data->ucm->base->mappings, mLimit=m+data->ucm->base->mappingsLength;
                    m<mLimit && fallbackFlags!=3;
                    ++m
                ) {
                    if(m->f==1) {
                        fallbackFlags|=1;
                    } else if(m->f==3) {
                        fallbackFlags|=2;
                    }
                }

                if(fallbackFlags&1) {
                    staticData->hasFromUnicodeFallback=TRUE;
                }
                if(fallbackFlags&2) {
                    staticData->hasToUnicodeFallback=TRUE;
                }

                if(1!=ucm_countChars(baseStates, staticData->subChar, staticData->subCharLen)) {
                    fprintf(stderr, "       the substitution character byte sequence is illegal in this codepage structure!\n");
                    *pErrorCode=U_INVALID_TABLE_FORMAT;

                } else if(1!=ucm_countChars(baseStates, &staticData->subChar1, 1)) {
                    fprintf(stderr, "       the subchar1 byte is illegal in this codepage structure!\n");
                    *pErrorCode=U_INVALID_TABLE_FORMAT;

                } else if(
                    !ucm_checkValidity(data->ucm->ext, baseStates) ||
                    !ucm_checkBaseExt(baseStates, baseData.ucm->base, data->ucm->ext, data->ucm->ext, FALSE) ||
                    !data->extData->addTable(data->extData, data->ucm->ext, &data->staticData)
                ) {
                    *pErrorCode=U_INVALID_TABLE_FORMAT;
                }
            }
        }

        cleanupConvData(&baseData);
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
