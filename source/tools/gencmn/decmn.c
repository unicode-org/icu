/*
*******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
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

static uint8_t buffer[100000], buffer2[128*1024];

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
copyFile(FILE *in, int32_t offset, int32_t size, const char *name) {
    FILE *out;
    int32_t length;

    if(0!=fseek(in, offset, SEEK_SET)) {
        fprintf(stderr, "error: cannot seek to position %ld for file \"%s\"\n",
            (long)offset, name);
        return 4;
    }
    out=fopen(name, "wb");
    if(out==NULL) {
        fprintf(stderr, "error: unable to open output file \"%s\"\n", name);
        return 5;
    }

    /* copy the contents into the new, separate file */
    while(size>sizeof(buffer2)) {
        length=(int32_t)fread(buffer2, 1, sizeof(buffer2), in);
        if(length<=0) {
            fprintf(stderr, "error: read error while copying output file \"%s\"\n", name);
            fclose(out);
            return 4;
        }
        if(length!=(int32_t)fwrite(buffer2, 1, length, out)) {
            fprintf(stderr, "error: write error while copying output file \"%s\"\n", name);
            fclose(out);
            return 5;
        }
        size-=length;
    }
    while(size>0) {
        length=(int32_t)fread(buffer2, 1, size, in);
        if(length<=0) {
            fprintf(stderr, "error: read error while copying output file \"%s\"\n", name);
            fclose(out);
            return 4;
        }
        if(length!=(int32_t)fwrite(buffer2, 1, length, out)) {
            fprintf(stderr, "error: write error while copying output file \"%s\"\n", name);
            fclose(out);
            return 5;
        }
        size-=length;
    }

    fclose(out);
    return 0;
}

extern int
main(int argc, const char *argv[]) {
    FILE *in;
    UDataInfo *info;
    uint8_t *base;
    int32_t *p;
    int32_t i, length, count, baseOffset;
    int result;

    if(argc!=2) {
        fprintf(stderr,
                "usage: %s icu-data-filename\n"
                "  unpackages ICU common data files:\n"
                "  reads a memory-mappable ICU data file, outputs the list of data pieces to stdout\n"
                "  and writes the packaged data files as separate files in the current folder.\n",
                argv[0]);
        return 1;
    }

    in=fopen(argv[1], "rb");
    if(in==NULL) {
        fprintf(stderr, "error: unable to open input file \"%s\"\n", argv[1]);
        return 2;
    }

    /* read the beginning of the file */
    length=(int32_t)fread(buffer, 1, sizeof(buffer), in);
    if(length<20) {
        fprintf(stderr, "error: input file too short\n");
        fclose(in);
        return 3;
    }

    /* check the validity of the file */
    if(buffer[2]!=0xda || buffer[3]!=0x27) {
        fprintf(stderr, "error: not an ICU data file\n");
        fclose(in);
        return 3;
    }

    /* check the platform properties for the file */
    info=(UDataInfo *)(buffer+4);
    if(info->isBigEndian!=U_IS_BIG_ENDIAN) {
        fprintf(stderr, "error: the file is in the opposite byte endianness\n");
        fclose(in);
        return 3;
    }
    if(info->charsetFamily!=U_CHARSET_FAMILY) {
        fprintf(stderr, "error: the file is not built for this machine's charset family\n");
        fclose(in);
        return 3;
    }

    /* check that this is a common data file */
    if(info->dataFormat[0]!=0x43 || info->dataFormat[1]!=0x6d || info->dataFormat[2]!=0x6e || info->dataFormat[3]!=0x44) {
        fprintf(stderr, "error: this file is not a common data file\n");
        fclose(in);
        return 3;
    }

    /* check for version 1 */
    if(info->formatVersion[0]!=1) {
        fprintf(stderr, "error: the format version %d.%d.%d.%d is not known\n",
                info->formatVersion[0], info->formatVersion[1], info->formatVersion[2], info->formatVersion[3]);
        fclose(in);
        return 3;
    }

    /* is there a comment? */
    if(*(uint16_t *)buffer>4+info->size) {
        printf("comment=%s\n", buffer+4+info->size);
    }

    /* output all filenames */
    baseOffset=*(uint16_t *)buffer;
    base=buffer+baseOffset;
    p=(int32_t *)base;
    count=*p++;
    printf("files[%ld]\n", (long)count);
    for(i=0; i<count; ++i) {
        printf("file[%ld]=%s\n", (long)i, base+*p);
        p+=2;
    }
    puts("endfiles");

    /* sort all files by their offsets in the common file */
    qsort(base+4, count, 8, compareFiles);

    /* write all files except the last one */
    p=(int32_t *)(base+4);
    --count;
    for(i=0; i<count; ++i) {
        /* the size is the difference between this file's offset and the next one's */
        result=copyFile(in, baseOffset+p[1], p[3]-p[1], (const char *)(base+*p));
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
            fprintf(stderr, "error: unable to seek to the end of the common file\n");
            return 4;
        }
        result=copyFile(in, baseOffset+p[1], (int32_t)ftell(in)-baseOffset-p[1], (const char *)(base+*p));
        if(result!=0) {
            fclose(in);
            return result;
        }
    }

    fclose(in);
    return 0;
}
