/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  gennames.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999nov01
*   created by: Markus W. Scherer
*
*   This program reads a binary file and creates a C source code file
*   with a byte array that contains the data of the binary file.
*
*   12/09/1999  weiv    Added multiple file handling
*/

#include <stdio.h>
#include <stdlib.h>
#include "utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"

static uint16_t column=0xffff;

/* prototypes --------------------------------------------------------------- */

static void
writeCCode(const char *filename);

static void
getOutFilename(const char *inFilename, char *outFilename, char *entryName);

static void
write8(FileStream *out, uint8_t byte);

/* -------------------------------------------------------------------------- */

extern int
main(int argc, char *argv[]) {
    if(argc<=1) {
        fprintf(stderr,
            "usage: %s filename1 filename2 ...\n"
            "\tread the binary input file and \n"
            "\tcreate a .c file with a byte array that contains the input file's data\n",
            argv[0]);
    } else {
        while (--argc) {
            fprintf(stdout, "Generating C code for %s\n", getLongPathname(argv[argc]));
            column=0xffff;
            writeCCode(getLongPathname(argv[argc]));
        }
    }

    return 0;
}

static void
writeCCode(const char *filename) {
    char buffer[4096], entry[40];
    FileStream *in, *out;
    size_t i, length;

    in=T_FileStream_open(filename, "rb");
    if(in==NULL) {
        fprintf(stderr, "genccode: unable to open input file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    getOutFilename(filename, buffer, entry);
    out=T_FileStream_open(buffer, "w");
    if(out==NULL) {
        fprintf(stderr, "genccode: unable to open output file %s\n", buffer);
        exit(U_FILE_ACCESS_ERROR);
    }

    T_FileStream_writeLine(out, "#include \"utypes.h\"\nU_CAPI const uint8_t U_EXPORT2 ");

    for(i=0;i<strlen(entry);i++)
      {
	if(entry[i]=='-')
        {
	  entry[i]='_';
        }
      }

    T_FileStream_writeLine(out, entry);
    T_FileStream_writeLine(out, "[]={\n");

    for(;;) {
        length=T_FileStream_read(in, buffer, sizeof(buffer));
        if(length==0) {
            break;
        }
        for(i=0; i<length; ++i) {
            write8(out, (uint8_t)buffer[i]);
        }
    }

    T_FileStream_writeLine(out, "\n};\n");

    if(T_FileStream_error(in)) {
        fprintf(stderr, "genccode: file read error while generating from file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    if(T_FileStream_error(out)) {
        fprintf(stderr, "genccode: file write error while generating from file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    T_FileStream_close(out);
    T_FileStream_close(in);
}

static void
getOutFilename(const char *inFilename, char *outFilename, char *entryName) {
    const char *basename=findBasename(inFilename), *suffix=icu_strrchr(basename, '.');

    /* copy path */
    while(inFilename<basename) {
        *outFilename++=*inFilename++;
    }

    if(suffix==NULL) {
        /* the filename does not have a suffix */
        icu_strcpy(entryName, inFilename);
        icu_strcpy(outFilename, inFilename);
        icu_strcat(outFilename, ".c");
    } else {
        /* copy basename */
        while(inFilename<suffix) {
            *outFilename++=*entryName++=*inFilename++;
        }

        /* replace '.' by '_' */
        *outFilename++=*entryName++='_';
        ++inFilename;

        /* copy suffix */
        while(*inFilename!=0) {
            *outFilename++=*entryName++=*inFilename++;
        }

        *entryName=0;

        /* add ".c" */
        *outFilename++='.';
        *outFilename++='c';
        *outFilename=0;
    }
}

static void
write8(FileStream *out, uint8_t byte) {
    char s[4];
    int i=0;

    /* convert the byte value to a string */
    if(byte>=100) {
        s[i++]='0'+byte/100;
        byte%=100;
    }
    if(i>0 || byte>=10) {
        s[i++]='0'+byte/10;
        byte%=10;
    }
    s[i++]='0'+byte;
    s[i]=0;

    /* write the value, possibly with comma and newline */
    if(column==0xffff) {
        /* first byte */
        column=1;
    } else if(column<16) {
        T_FileStream_writeLine(out, ",");
        ++column;
    } else {
        T_FileStream_writeLine(out, ",\n");
        column=1;
    }
    T_FileStream_writeLine(out, s);
}
