/*
*******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  decmn.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001mar05
*   created by: Markus W. Scherer
*   changes by: Yves Arrouye
*
*   This tool takes an ICU common data file (icuxyz.dat),
*   outputs a list of components,
*   and writes one file per packaged data piece in the common file.
*   This can be used to add, remove, or replace data.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/udata.h"
#include "uoptions.h"
#include "cstring.h"

static uint8_t buffer[100000], buffer2[128*1024];

static const char *pname;

static UOption options[]={
/*0*/ UOPTION_HELP_H,
/*1*/ UOPTION_HELP_QUESTION_MARK,
/*2*/ UOPTION_DESTDIR,
/*3*/ UOPTION_DEF(0, 'n', UOPT_NO_ARG),
/*4*/ UOPTION_DEF("comment", 'C', UOPT_NO_ARG),
};

static int
compareFiles(const void *file1, const void *file2) {
    /* sort by file offset */
    int32_t diff=*((int32_t *)file1+1)-*((int32_t *)file2+1);
    if(diff!=0) {
        return (int)(diff>>15)|1;
    } else {
        return 0;
    }
}

static int
copyFile(FILE *in, int32_t offset, int32_t size, const char *dir, const char *name) {
    FILE *out;
    int32_t length;
    char path[512], *p;

    if(0!=fseek(in, offset, SEEK_SET)) {
        fprintf(stderr, "%s: cannot seek to position %ld for file \"%s\"\n", pname,
            (long)offset, name);
        return 4;
    }

    uprv_strcpy(path, dir);
    p = path + strlen(path);
    if (p[-1] != U_FILE_SEP_CHAR) {
        *p++ = U_FILE_SEP_CHAR;
    }
    uprv_strcpy(p, name);

    out=fopen(path, "wb");
    if(out==NULL) {
        fprintf(stderr, "%s: unable to open output file \"%s\"\n", pname, path);
        return 5;
    }

    /* copy the contents into the new, separate file */
    while(size>sizeof(buffer2)) {
        length=(int32_t)fread(buffer2, 1, sizeof(buffer2), in);
        if(length<=0) {
            fprintf(stderr, "%s: read error while copying output file \"%s\"\n", pname, path);
            fclose(out);
            return 4;
        }
        if(length!=(int32_t)fwrite(buffer2, 1, length, out)) {
            fprintf(stderr, "%s: write error while copying output file \"%s\"\n", pname, path);
            fclose(out);
            return 5;
        }
        size-=length;
    }
    while(size>0) {
        length=(int32_t)fread(buffer2, 1, size, in);
        if(length<=0) {
            fprintf(stderr, "%s: read error while copying output file \"%s\"\n", pname, path);
            fclose(out);
            return 4;
        }
        if(length!=(int32_t)fwrite(buffer2, 1, length, out)) {
            fprintf(stderr, "%s: write error while copying output file \"%s\"\n", pname, path);
            fclose(out);
            return 5;
        }
        size-=length;
    }

    fclose(out);
    return 0;
}

extern int
main(int argc, char *argv[]) {
    FILE *in;
    UDataInfo *info;
    uint8_t *base;
    int32_t *p;
    int32_t i, length, count, baseOffset;
    int result, ishelp = 0;

    U_MAIN_INIT_ARGS(argc, argv);

    pname = uprv_strchr(*argv, U_FILE_SEP_CHAR);
#ifdef WIN32
    if (!pname) {
        pname = uprv_strchr(*argv, '/');
    }
#endif
    if (pname) {
        ++pname;
    } else {
        pname = argv[0];
    }

    options[2].value = ".";

    argc = u_parseArgs(argc, argv, sizeof(options) / sizeof(*options), options);
    ishelp = options[0].doesOccur || options[1].doesOccur;
    if (ishelp || argc != 2) {
        fprintf(stderr,
                "%csage: %s [ -h, -?, --help ] [ -n ] [ -C, --comment ] [ -d, --destdir destination ] archive\n", ishelp ? 'U' : 'u', pname);
        if (ishelp) {
            fprintf(stderr, "\nOptions: -h, -?, --help    print this message and exit\n"
                    "         -n                do not create files\n"
                    "         -C, --comment     print the comment embedded in the file and exit\n"
                    "         -d, --destdir destination    create files in destination\n");
        }

        return ishelp ? 0 : 1;
    }

    in=fopen(argv[1], "rb");
    if(in==NULL) {
        fprintf(stderr, "%s: unable to open input file \"%s\"\n", pname, argv[1]);
        return 2;
    }

    /* read the beginning of the file */
    length=(int32_t)fread(buffer, 1, sizeof(buffer), in);
    if(length<20) {
        fprintf(stderr, "%s: input file too short\n", pname);
        fclose(in);
        return 3;
    }

    /* check the validity of the file */
    if(buffer[2]!=0xda || buffer[3]!=0x27) {
        fprintf(stderr, "%s: not an ICU data file\n", pname);
        fclose(in);
        return 3;
    }

    /* check the platform properties for the file */
    info=(UDataInfo *)(buffer+4);
    if(info->isBigEndian!=U_IS_BIG_ENDIAN) {
        fprintf(stderr, "%s: the file is in the wrong byte endianness\n", pname);
        fclose(in);
        return 3;
    }
    if(info->charsetFamily!=U_CHARSET_FAMILY) {
        fprintf(stderr, "%s: the file is not built for this machine's charset family\n", pname);
        fclose(in);
        return 3;
    }

    /* check that this is a common data file */
    if(info->dataFormat[0]!=0x43 || info->dataFormat[1]!=0x6d || info->dataFormat[2]!=0x6e || info->dataFormat[3]!=0x44) {
        fprintf(stderr, "%s: this file is not a common data (archive) file\n", pname);
        fclose(in);
        return 3;
    }

    /* check for version 1 */
    if(info->formatVersion[0]!=1) {
        fprintf(stderr, "%s: the format version %d.%d.%d.%d is not known\n", pname,
                info->formatVersion[0], info->formatVersion[1], info->formatVersion[2], info->formatVersion[3]);
        fclose(in);
        return 3;
    }

    /* do we want to show the comment, and is there a comment? */
    if (options[4].doesOccur && *(uint16_t *)buffer>4+info->size) {
        printf("%s\n", (const char *)(buffer+4+info->size));
        return 0;
    }

    /* output all filenames */
    baseOffset=*(uint16_t *)buffer;
    base=buffer+baseOffset;
    p=(int32_t *)base;
    count=*p++;
    /* printf("files[%ld]\n", (long)count); */
    for(i=0; i<count; ++i) {
        printf("%s%c%s\n", options[2].value, U_FILE_SEP_CHAR, base+*p);
        p+=2;
    }
    /* puts("endfiles"); */

    if (options[3].doesOccur) { /* Do not extract. */
        return 0;
    }

    /* sort all files by their offsets in the common file */
    qsort(base+4, count, 8, compareFiles);

    /* write all files except the last one */
    p=(int32_t *)(base+4);
    --count;
    for(i=0; i<count; ++i) {
        /* the size is the difference between this file's offset and the next one's */
        result=copyFile(in, baseOffset+p[1], p[3]-p[1], options[2].value, (const char *)(base+*p));
        if(result!=0) {
            fclose(in);
            return result;
        }
        p+=2;
    }

    /* write the last file */
    if(count>=0) {
        /* the size is the number of bytes to the end of the common file */
        if(0!=fseek(in, 0, SEEK_END)) {
            fprintf(stderr, "%s: unable to seek to the end of the common file\n", pname);
            return 4;
        }
        result=copyFile(in, baseOffset+p[1], (int32_t)ftell(in)-baseOffset-p[1], options[2].value, (const char *)(base+*p));
        if(result!=0) {
            fclose(in);
            return result;
        }
    }

    fclose(in);
    return 0;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
