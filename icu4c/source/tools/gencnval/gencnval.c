/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gencnval.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999nov05
*   created by: Markus W. Scherer
*
*   This program reads convrtrs.txt and writes a memory-mappable
*   converter name alias table to cnvalias.dat .
*
*   This program currently writes version 2.1 of the data format. See
*   ucnv_io.c for more details on the format. Note that version 2.1
*   is written in such a way that a 2.0 reader will be able to use it,
*   and a 2.1 reader will be able to read 2.0.
*/

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "ucnv_io.h" /* charsetNameCmp */
#include "filestrm.h"
#include "unewdata.h"
#include "uoptions.h"

#define STRING_STORE_SIZE 100000
#define MAX_ALIAS_COUNT 2000

#define TAG_STORE_SIZE 20000
#define MAX_TAG_COUNT 200

#define DATA_NAME "cnvalias"
#define DATA_TYPE "dat"

/* UDataInfo cf. udata.h */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x43, 0x76, 0x41, 0x6c,     /* dataFormat="CvAl" */
    2, 1, 0, 0,                 /* formatVersion */
    1, 4, 2, 0                  /* dataVersion */
};

typedef struct {
    char *store;
    uint32_t top;
    uint32_t max;
} StringBlock;

static char stringStore[STRING_STORE_SIZE];
static StringBlock stringBlock = { stringStore, 0, STRING_STORE_SIZE };
 
typedef struct {
    const char *alias;
    uint16_t converter;
} Alias;

static Alias aliases[MAX_ALIAS_COUNT];
static uint16_t aliasCount=0;

typedef struct {
    const char *converter;
    uint16_t aliasCount;
} Converter;

static Converter converters[MAX_ALIAS_COUNT];
static uint16_t converterCount=0;

static char tagStore[TAG_STORE_SIZE];
static StringBlock tagBlock = { tagStore, 0, TAG_STORE_SIZE };

typedef struct {
    const char *tag;
    const char *aliases[MAX_ALIAS_COUNT];
} Tag;

static Tag tags[MAX_TAG_COUNT];
static uint16_t tagCount = 0;

/* prototypes --------------------------------------------------------------- */

static void
parseLine(const char *line);

static uint16_t
addAlias(const char *alias, uint16_t converter);

static uint16_t
addConverter(const char *converter);

static char *
allocString(StringBlock *block, uint32_t length);

static int
compareAliases(const void *alias1, const void *alias2);

static int
compareConverters(const void *converter1, const void *converter2);

static uint16_t
getTagNumber(const char *tag, uint16_t tagLen);

static void
addTaggedAlias(uint16_t tag, const char *alias, uint16_t converter);

/* -------------------------------------------------------------------------- */

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_COPYRIGHT,
    UOPTION_DESTDIR,
    UOPTION_SOURCEDIR
};

extern int
main(int argc, const char *argv[]) {
    char line[512];
    const char *path;
    FileStream *in;
    UNewDataMemory *out;
    char *s;
    UErrorCode errorCode=U_ZERO_ERROR;
    int i;
    uint16_t tagOffset, stringOffset;

    /* preset then read command line options */
    options[3].value=options[4].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] [convrtrs.txt]\n"
            "\tread convrtrs.txt and create " DATA_NAME "." DATA_TYPE "\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-c or --copyright   include a copyright notice\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-s or --sourcedir   source directory, followed by the path\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(argc>=2) {
        path=argv[1];
    } else {
        path=options[4].value;
        if(path!=NULL && *path!=0) {
            uprv_strcpy(line, path);
            path=line+uprv_strlen(line);
            if(*(path-1)!=U_FILE_SEP_CHAR) {
                *((char *)path)=U_FILE_SEP_CHAR;
                ++path;
            }
            uprv_strcpy((char *)path, "convrtrs.txt");
            path=line;
        } else {
            path="convrtrs.txt";
        }
    }
    in=T_FileStream_open(path, "r");
    if(in==NULL) {
        fprintf(stderr, "gencnval: unable to open input file convrtrs.txt\n");
        exit(U_FILE_ACCESS_ERROR);
    }

    /* read the list of aliases */
    while(T_FileStream_readLine(in, line, sizeof(line))!=NULL) {
        /* remove trailing newline characters */
        s=line;
        while(*s!=0) {
            if(*s=='\r' || *s=='\n') {
                *s=0;
                break;
            }
            ++s;
        }

        parseLine(line);
    }

    T_FileStream_close(in);

    /* sort the aliases */
    qsort(aliases, aliasCount, sizeof(Alias), compareAliases);

    /* create the output file */
    out=udata_create(options[3].value, DATA_TYPE, DATA_NAME, &dataInfo,
                     options[2].doesOccur ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gencnval: unable to open output file - error %s\n", u_errorName(errorCode));
        exit(errorCode);
    }

    /* determine the length of tables for the data offset of the strings */
    tagOffset = 2 + 4 * aliasCount + 2 + 4 * converterCount;
    stringOffset = tagOffset + 2 + (2 * tagCount) * converterCount + tagBlock.top;

    /* write the table of aliases */
    udata_write16(out, aliasCount);
    for(i=0; i<aliasCount; ++i) {
        udata_write16(out, (uint16_t)(stringOffset+(aliases[i].alias-stringBlock.store)));
    }
    for(i=0; i<aliasCount; ++i) {
        udata_write16(out, aliases[i].converter);
    }

    /* write the table of converters */
    udata_write16(out, converterCount);
    for(i=0; i<converterCount; ++i) {
        udata_write16(out, (uint16_t)(stringOffset+(converters[i].converter-stringBlock.store)));
        udata_write16(out, converters[i].aliasCount);
    }

    /* write the table of tags */
    udata_write16(out, tagCount);

    for (i = 0; i < tagCount; ++i) {
        int j;

        /* write the aliases offsets */
        for (j = 0; j < converterCount; ++j) {
            if (tags[i].aliases[j]) {
                udata_write16(out, (uint16_t) (stringOffset + tags[i].aliases[j] - stringBlock.store));
            } else {
                udata_write16(out, 0);
            }
        }
    }

    /* write the tags strings */
    udata_writeString(out, tagBlock.store, tagBlock.top);

    /* write the aliases strings */
    udata_writeString(out, stringBlock.store, stringBlock.top);

    /* finish */
    udata_finish(out, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gencnval: error finishing output file - %s\n", u_errorName(errorCode));
        exit(errorCode);
    }

    return 0;
}

static void
parseLine(const char *line) {
    uint16_t pos=0, start, limit, length, cnv;
    char *converter, *alias;

    /* skip leading white space */
    while(line[pos]!=0 && isspace((unsigned char)line[pos])) {
        ++pos;
    }

    /* is there only a comment on this line? */
    if(line[pos]==0 || line[pos]=='#') {
        return;
    }

    /* get the converter name */
    start=pos;
    while(line[pos]!=0 && line[pos]!='#' && !isspace((unsigned char)line[pos])) {
        ++pos;
    }
    limit=pos;

    /* store the converter name */
    length=limit-start;
    converter=allocString(&stringBlock, length+1);
    uprv_memcpy(converter, line+start, length);
    converter[length]=0;

    /* add the converter to the converter table */
    cnv=addConverter(converter);

    /* add the converter as its own alias to the alias table */
    addAlias(converter, cnv);

    /* get all the real aliases */
    for(;;) {
        /* skip white space */
        while(line[pos]!=0 && isspace((unsigned char)line[pos])) {
            ++pos;
        }

        /* is there no more alias name on this line? */
        if(line[pos]==0 || line[pos]=='#') {
            break;
        }

        /* get an alias name */
        start=pos;
        while(line[pos]!=0 && line[pos]!='{' && line[pos]!='#' && !isspace((unsigned char)line[pos])) {
            ++pos;
        }
        limit=pos;

        /* store the alias name */
        length=limit-start;
        alias=allocString(&stringBlock, length+1);
        uprv_memcpy(alias, line+start, length);
        alias[length]=0;

        /* add the alias/converter pair to the alias table */
        addAlias(alias, cnv);

        /* skip whitespace */
        while (line[pos] && isspace((unsigned char)line[pos])) {
            ++pos;
        }

        /* handle tags if they are present */
        if (line[pos] == '{') {
            ++pos;
            do {
                start = pos;
                while (line[pos] && line[pos] != '}' && line[pos] != '#' && !isspace((unsigned char) line[pos])) {
                    ++pos;
                }
                limit = pos;

                if (start != limit) {
                    uint16_t tag;

                    /* add the tag to the tag table */
                    tag = getTagNumber(line + start, limit - start);
                    addTaggedAlias(tag, alias, cnv);
                }

                while (line[pos] && isspace((unsigned char)line[pos])) {
                    ++pos;
                }
            } while (line[pos] && line[pos] != '}' && line[pos] != '#');

            if (line[pos] == '}') {
                ++pos;
            } else {
                fprintf(stderr, "unterminated tag list in: %s\n", line);
                exit(U_PARSE_ERROR);
            }
        }
    }
}

static uint16_t
getTagNumber(const char *tag, uint16_t tagLen) {
    char *atag;
    uint16_t t;

    if (tagCount == MAX_TAG_COUNT) {
        fprintf(stderr, "gencnval: too many tags\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    for (t = 0; t < tagCount; ++t) {
        if (uprv_strlen(tags[t].tag) == tagLen && !uprv_strnicmp(tags[t].tag, tag, tagLen)) {
            return t;
        }
    }

    /* we need to add this tag */

    if (tagCount == MAX_TAG_COUNT) {
        fprintf(stderr, "gencnval: too many tags\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    /* allocate a new entry in the tag table */

    atag = allocString(&tagBlock, tagLen + 1);
    uprv_memcpy(atag, tag, tagLen);
    atag[tagLen] = 0;

    /* add the tag to the tag table */
    tags[tagCount].tag = atag;
    uprv_memset(&tags[tagCount].aliases, 0, sizeof(tags[tagCount].aliases));

    return tagCount++;
}

static void
addTaggedAlias(uint16_t tag, const char *alias, uint16_t converter) {
    tags[tag].aliases[converter] = alias;
}

static uint16_t
addAlias(const char *alias, uint16_t converter) {
    if(aliasCount==MAX_ALIAS_COUNT) {
        fprintf(stderr, "gencnval: too many aliases\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    aliases[aliasCount].alias=alias;
    aliases[aliasCount].converter=converter;

    ++converters[converter].aliasCount;

    return aliasCount++;
}

static uint16_t
addConverter(const char *converter) {
    if(converterCount==MAX_ALIAS_COUNT) {
        fprintf(stderr, "gencnval: too many converters\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    converters[converterCount].converter=converter;
    converters[converterCount].aliasCount=0;

    return converterCount++;
}

static char *
allocString(StringBlock *block, uint32_t length) {
    uint32_t top=block->top+length;
    char *p;

    if(top>block->max) {
        fprintf(stderr, "gencnval: out of memory\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    p=block->store+block->top;
    block->top=top;
    return p;
}

static int
compareAliases(const void *alias1, const void *alias2) {
    return charsetNameCmp(((Alias*)alias1)->alias, ((Alias*)alias2)->alias);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */

