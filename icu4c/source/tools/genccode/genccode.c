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

#ifdef WIN32
#include <windows.h>
#include <time.h>
#endif

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
writeObjectCode(const char *filename, const char *destdir);

static void
getOutFilename(const char *inFilename, const char *destdir, char *outFilename, char *entryName, const char *newSuffix);

static void
write8(FileStream *out, uint8_t byte);

/* -------------------------------------------------------------------------- */

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_DESTDIR,
    UOPTION_DEF("object", 'o', UOPT_NO_ARG),
    UOPTION_DEF("name", 'n', UOPT_REQUIRES_ARG)
};

char symPrefix[100];

extern int
main(int argc, char* argv[]) {
    /* read command line options */
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    if(options[4].doesOccur)
    {
      uprv_strcpy(symPrefix, options[4].value);
      uprv_strcat(symPrefix, "_");
    }
    else
    {
      symPrefix[0] = 0;
    }

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
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-o or --object      write a .obj file instead of .c\n",
            argv[0]);
    } else {
        const char *message, *filename;
        void (*writeCode)(const char *filename, const char *destdir);
        if(options[3].doesOccur) {
            message="Generating object code for %s\n";
            writeCode=&writeObjectCode;
        } else {
            message="Generating C code for %s\n";
            writeCode=&writeCCode;
        }
        while(--argc) {
            filename=getLongPathname(argv[argc]);
            fprintf(stdout, message, filename);
            column=0xffff;
            writeCode(filename, options[2].value);
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

    getOutFilename(filename, destdir, buffer, entry, ".c");
    out=T_FileStream_open(buffer, "w");
    if(out==NULL) {
        fprintf(stderr, "genccode: unable to open output file %s\n", buffer);
        exit(U_FILE_ACCESS_ERROR);
    }

    /* turn dashes in the entry name into underscores */
    length=uprv_strlen(entry);
    for(i=0; i<length; ++i) {
        if(entry[i]=='-') {
            entry[i]='_';
        }
    }

    sprintf(buffer,
        "#include \"unicode/utypes.h\"\n"
        "U_CDECL_BEGIN\n"
        "const struct {\n"
        "    double bogus;\n"
        "    uint8_t bytes[%ld]; \n"
        "} %s%s={ 0, {\n",
        T_FileStream_size(in), symPrefix, entry);
    T_FileStream_writeLine(out, buffer);

    for(;;) {
        length=T_FileStream_read(in, buffer, sizeof(buffer));
        if(length==0) {
            break;
        }
        for(i=0; i<length; ++i) {
            write8(out, (uint8_t)buffer[i]);
        }
    }

    T_FileStream_writeLine(out, "\n}\n};\nU_CDECL_END\n");

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
writeObjectCode(const char *filename, const char *destdir) {
#ifdef WIN32
    char buffer[4096], entry[40];
    struct {
        IMAGE_FILE_HEADER fileHeader;
        IMAGE_SECTION_HEADER sections[2];
        char linkerOptions[100];
    } objHeader;
    IMAGE_SYMBOL symbols[1];
    struct {
        DWORD sizeofLongNames;
        char longNames[100];
    } symbolNames;
    FileStream *in, *out;
    size_t i, entryLength, length, size;

    in=T_FileStream_open(filename, "rb");
    if(in==NULL) {
        fprintf(stderr, "genccode: unable to open input file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    /* entry have a leading '_' */
    entry[0]='_';
    getOutFilename(filename, destdir, buffer, entry+1, ".obj");

    /* turn dashes in the entry name into underscores */
    entryLength=uprv_strlen(entry);
    for(i=0; i<entryLength; ++i) {
        if(entry[i]=='-') {
            entry[i]='_';
        }
    }

    /* open the output file */
    out=T_FileStream_open(buffer, "wb");
    if(out==NULL) {
        fprintf(stderr, "genccode: unable to open output file %s\n", buffer);
        exit(U_FILE_ACCESS_ERROR);
    }

    /* populate the .obj headers */
    uprv_memset(&objHeader, 0, sizeof(objHeader));
    uprv_memset(&symbols, 0, sizeof(symbols));
    uprv_memset(&symbolNames, 0, sizeof(symbolNames));
    size=T_FileStream_size(in);

    /* write the linker export directive */
    uprv_strcpy(objHeader.linkerOptions, "-export:");
    length=8;
    uprv_strcpy(objHeader.linkerOptions+length, entry);
    length+=entryLength;
    uprv_strcpy(objHeader.linkerOptions+length, ",data ");
    length+=6;

    /* set the file header */
    objHeader.fileHeader.Machine=IMAGE_FILE_MACHINE_I386;
    objHeader.fileHeader.NumberOfSections=2;
    objHeader.fileHeader.TimeDateStamp=time(NULL);
    objHeader.fileHeader.PointerToSymbolTable=IMAGE_SIZEOF_FILE_HEADER+2*IMAGE_SIZEOF_SECTION_HEADER+length+size; /* start of symbol table */
    objHeader.fileHeader.NumberOfSymbols=1;

    /* set the section for the linker options */
    uprv_strncpy((char *)objHeader.sections[0].Name, ".drectve", 8);
    objHeader.sections[0].SizeOfRawData=length;
    objHeader.sections[0].PointerToRawData=IMAGE_SIZEOF_FILE_HEADER+2*IMAGE_SIZEOF_SECTION_HEADER;
    objHeader.sections[0].Characteristics=IMAGE_SCN_LNK_INFO|IMAGE_SCN_LNK_REMOVE|IMAGE_SCN_ALIGN_1BYTES;

    /* set the data section */
    uprv_strncpy((char *)objHeader.sections[1].Name, ".rdata", 6);
    objHeader.sections[1].SizeOfRawData=size;
    objHeader.sections[1].PointerToRawData=IMAGE_SIZEOF_FILE_HEADER+2*IMAGE_SIZEOF_SECTION_HEADER+length;
    objHeader.sections[1].Characteristics=IMAGE_SCN_CNT_INITIALIZED_DATA|IMAGE_SCN_ALIGN_16BYTES|IMAGE_SCN_MEM_READ;

    /* set the symbol table */
    if(entryLength<=8) {
        uprv_strncpy((char *)symbols[0].N.ShortName, entry, entryLength);
        symbolNames.sizeofLongNames=4;
    } else {
        symbols[0].N.Name.Short=0;
        symbols[0].N.Name.Long=4;
        symbolNames.sizeofLongNames=4+entryLength+1;
        uprv_strcpy(symbolNames.longNames, entry);
    }
    symbols[0].SectionNumber=2;
    symbols[0].StorageClass=IMAGE_SYM_CLASS_EXTERNAL;

    /* write the file header and the linker options section */
    T_FileStream_write(out, &objHeader, objHeader.sections[1].PointerToRawData);

    /* copy the data file into section 2 */
    for(;;) {
        length=T_FileStream_read(in, buffer, sizeof(buffer));
        if(length==0) {
            break;
        }
        T_FileStream_write(out, buffer, length);
    }

    /* write the symbol table */
    T_FileStream_write(out, symbols, IMAGE_SIZEOF_SYMBOL);
    T_FileStream_write(out, &symbolNames, symbolNames.sizeofLongNames);

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
#endif
}

static void
getOutFilename(const char *inFilename, const char *destdir, char *outFilename, char *entryName, const char *newSuffix) {
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
        uprv_strcat(outFilename, newSuffix);
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
        uprv_strcpy(outFilename, newSuffix);
    }
}

static void
write8(FileStream *out, uint8_t byte) {
    char s[4];
    int i=0;

    /* convert the byte value to a string */
    if(byte>=100) {
        s[i++]=(char)('0'+byte/100);
        byte%=100;
    }
    if(i>0 || byte>=10) {
        s[i++]=(char)('0'+byte/10);
        byte%=10;
    }
    s[i++]=(char)('0'+byte);
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
