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
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "uoptions.h"

static uint16_t column=0xffff;

/* prototypes --------------------------------------------------------------- */

static void
writeCCode(const char *filename, const char *destdir);

static void
getOutFilename(const char *inFilename, const char *destdir, char *outFilename, char *entryName);

static void
write8(FileStream *out, uint8_t byte);

/* -------------------------------------------------------------------------- */

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_DESTDIR
};

extern int
main(int argc, const char *argv[]) {
    /* read command line options */
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] filename1 filename2 ...\n"
            "\tread each binary input file and \n"
            "\tcreate a .c file with a byte array that contains the input file's data\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n",
            argv[0]);
    } else {
        while (--argc) {
            fprintf(stdout, "Generating C code for %s\n", getLongPathname(argv[argc]));
            column=0xffff;
            writeCCode(getLongPathname(argv[argc]), options[2].value);
        }
    }

    return 0;
}

static void
writeCCode(const char *filename, const char *destdir) {
    char buffer[4096], entry[40];
    FileStream *in, *out;
    size_t i, length;

    in=T_FileStream_open(filename, "rb");
    if(in==NULL) {
        fprintf(stderr, "genccode: unable to open input file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    getOutFilename(filename, destdir, buffer, entry);
    out=T_FileStream_open(buffer, "w");
    if(out==NULL) {
        fprintf(stderr, "genccode: unable to open output file %s\n", buffer);
        exit(U_FILE_ACCESS_ERROR);
    }

    T_FileStream_writeLine(out, "#include \"unicode/utypes.h\"\nU_CAPI const struct U_EXPORT2 {\n    double bogus;\n    uint8_t bytes ");

    T_FileStream_writeLine(out, "[");
    sprintf(buffer, "%d",  T_FileStream_size(in) );
    T_FileStream_writeLine(out, buffer);
    T_FileStream_writeLine(out, "]; \n} ");
    for(i=0;i<strlen(entry);i++)
      {
	if(entry[i]=='-')
        {
	  entry[i]='_';
        }
      }

    T_FileStream_writeLine(out, entry); 
    T_FileStream_writeLine(out,"={ 0, {\n");

    for(;;) {
        length=T_FileStream_read(in, buffer, sizeof(buffer));
        if(length==0) {
            break;
        }
        for(i=0; i<length; ++i) {
            write8(out, (uint8_t)buffer[i]);
        }
    }

    T_FileStream_writeLine(out, "\n}\n};\n");

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
getOutFilename(const char *inFilename, const char *destdir, char *outFilename, char *entryName) {
    const char *basename=findBasename(inFilename), *suffix=uprv_strrchr(basename, '.');

    /* copy path */
    if(destdir!=NULL && *destdir!=0) {
        do {
            *outFilename++=*destdir++;
        } while(*destdir!=0);
        if(*(outFilename-1)!=U_FILE_SEP_CHAR) {
            *outFilename++=U_FILE_SEP_CHAR;
        }
        inFilename=basename;
    } else {
        while(inFilename<basename) {
            *outFilename++=*inFilename++;
        }
    }

    if(suffix==NULL) {
        /* the filename does not have a suffix */
        uprv_strcpy(entryName, inFilename);
        uprv_strcpy(outFilename, inFilename);
        uprv_strcat(outFilename, ".c");
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
