/*
*******************************************************************************
*
*   Copyright (C) 1999-2004, International Business Machines
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
#   define VC_EXTRALEAN
#   define WIN32_LEAN_AND_MEAN
#   define NOGDI
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>
#include <time.h>

/* _M_IA64 should be defined in windows.h */
#if defined(_M_IA64)
#   define ICU_OBJECT_MACHINE_TYPE IMAGE_FILE_MACHINE_IA64
#   define ICU_ENTRY_OFFSET 0
#elif defined(_M_AMD64)
#   define ICU_OBJECT_MACHINE_TYPE IMAGE_FILE_MACHINE_AMD64
#   define ICU_ENTRY_OFFSET 0
#else
#   define ICU_OBJECT_MACHINE_TYPE IMAGE_FILE_MACHINE_I386
#   define ICU_ENTRY_OFFSET 1
#endif

#endif

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unicode/uclean.h"
#include "uoptions.h"

#define MAX_COLUMN ((uint32_t)(0xFFFFFFFFU))

static uint32_t column=MAX_COLUMN;

#ifdef WIN32
#define CAN_GENERATE_OBJECTS
#endif

/* prototypes --------------------------------------------------------------- */

static void
writeCCode(const char *filename, const char *destdir);

static void
writeAssemblyCode(const char *filename, const char *destdir);

#ifdef CAN_GENERATE_OBJECTS
static void
writeObjectCode(const char *filename, const char *destdir);
#endif

static void
getOutFilename(const char *inFilename, const char *destdir, char *outFilename, char *entryName, const char *newSuffix);

static void
write8(FileStream *out, uint8_t byte);

static void
write32(FileStream *out, uint32_t byte);

#ifdef OS400
static void
write8str(FileStream *out, uint8_t byte);
#endif
/* -------------------------------------------------------------------------- */

enum { 
  kOptHelpH = 0,
  kOptHelpQuestionMark,
  kOptDestDir,
  kOptName,
  kOptEntryPoint,
#ifdef CAN_GENERATE_OBJECTS
  kOptObject,
#endif
  kOptFilename,
  kOptAssembly
};

/*
Creating Template Files for New Platforms

Let the cc compiler help you get started.
Compile this program
    const unsigned int x[5] = {1, 2, 0xdeadbeef, 0xffffffff, 16};
with the -S option to produce assembly output.

For example, this will generate array.s:
gcc -S array.c

This will produce a .s file that may look like this:

    .file   "array.c"
    .version        "01.01"
gcc2_compiled.:
    .globl x
    .section        .rodata
    .align 4
    .type    x,@object
    .size    x,20
x:
    .long   1
    .long   2
    .long   -559038737
    .long   -1
    .long   16
    .ident  "GCC: (GNU) 2.96 20000731 (Red Hat Linux 7.1 2.96-85)"

which gives a starting point that will compile, and can be transformed
to become the template, generally with some consulting of as docs and
some experimentation.

If you want ICU to automatically use this assembly, you should
specify "GENCCODE_ASSEMBLY=-a name" in the specific config/mh-* file,
where the name is the compiler or platform that you used in this
assemblyHeader data structure.
*/
static const struct AssemblyType {
    const char *name;
    const char *header;
    const char *beginLine;
} assemblyHeader[] = {
    {"gcc",
        ".globl %s\n"
        "\t.section .rodata\n"
        "\t.align 8\n" /* Either align 8 bytes or 2^8 (256) bytes. 8 bytes is needed. */
        "%s:\n\n",

        ".long "
    },
    {"gcc-darwin",
        /*"\t.section __TEXT,__text,regular,pure_instructions\n"
        "\t.section __TEXT,__picsymbolstub1,symbol_stubs,pure_instructions,32\n"*/
        ".globl _%s\n"
        "\t.data\n"
        "\t.const\n"
        "\t.align 4\n"  /* 1<<4 = 16 */
        "_%s:\n\n",

        ".long "
    },
    {"gcc-cygwin",
        ".globl _%s\n"
        "\t.section .rodata\n"
        "\t.align 8\n" /* Either align 8 bytes or 2^8 (256) bytes. 8 bytes is needed. */
        "_%s:\n\n",

        ".long "
    },
    {"sun",
        "\t.section \".rodata\"\n"
        "\t.align   8\n"
        ".globl     %s\n"
        "%s:\n",

        ".word "
    },
    {"xlc",
        ".globl %s{RO}\n"
        "\t.toc\n"
        "%s:\n"
        "\t.csect %s{RO}, 4\n",

        ".long "
    },
    {"aCC",
        "\t.SPACE  $TEXT$\n"
        "\t.SUBSPA $LIT$\n"
        "%s\n"
        "\t.EXPORT %s\n"
        "\t.ALIGN  16\n",

        ".WORD "
    }
};

static int32_t assemblyHeaderIndex = -1;

static UOption options[]={
/*0*/UOPTION_HELP_H,
     UOPTION_HELP_QUESTION_MARK,
     UOPTION_DESTDIR,
     UOPTION_DEF("name", 'n', UOPT_REQUIRES_ARG),
     UOPTION_DEF("entrypoint", 'e', UOPT_REQUIRES_ARG),
#ifdef CAN_GENERATE_OBJECTS
/*5*/UOPTION_DEF("object", 'o', UOPT_NO_ARG),
#endif
     UOPTION_DEF("filename", 'f', UOPT_REQUIRES_ARG),
     UOPTION_DEF("assembly", 'a', UOPT_REQUIRES_ARG)
};

extern int
main(int argc, char* argv[]) {
    UBool verbose = TRUE;
    int32_t idx;

    U_MAIN_INIT_ARGS(argc, argv);

    options[kOptDestDir].value = ".";

    /* read command line options */
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);
    
    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[kOptHelpH].doesOccur || options[kOptHelpQuestionMark].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] filename1 filename2 ...\n"
            "\tread each binary input file and \n"
            "\tcreate a .c file with a byte array that contains the input file's data\n"
            "options:\n"
            "\t-h or -? or --help  this usage text\n"
            "\t-d or --destdir     destination directory, followed by the path\n"
            "\t-n or --name        symbol prefix, followed by the prefix\n"
            "\t-e or --entrypoint  entry point name, followed by the name\n"
            "\t-r or --revision    Specify a version\n"
#ifdef CAN_GENERATE_OBJECTS
            "\t-o or --object      write a .obj file instead of .c\n"
#endif
            "\t-f or --filename    Specify an alternate base filename. (default: symbolname_typ)\n"
            , argv[0]);
        fprintf(stderr,
            "\t-a or --assembly    Create assembly file. (possible values are: ");

        fprintf(stderr, "%s", assemblyHeader[0].name);
        for (idx = 1; idx < (int32_t)(sizeof(assemblyHeader)/sizeof(assemblyHeader[0])); idx++) {
            fprintf(stderr, ", %s", assemblyHeader[idx].name);
        }
        fprintf(stderr,
            ")\n");
    } else {
        const char *message, *filename;
        void (*writeCode)(const char *, const char *);

        if(options[kOptAssembly].doesOccur) {
            message="generating assembly code for %s\n";
            writeCode=&writeAssemblyCode;
            for (idx = 0; idx < (int32_t)(sizeof(assemblyHeader)/sizeof(assemblyHeader[0])); idx++) {
                if (uprv_strcmp(options[kOptAssembly].value, assemblyHeader[idx].name) == 0) {
                    assemblyHeaderIndex = idx;
                    break;
                }
            }
            if (assemblyHeaderIndex < 0) {
                fprintf(stderr,
                    "Assembly type \"%s\" is unknown.\n", options[kOptAssembly].value);
                return -1;
            }
        }
#ifdef CAN_GENERATE_OBJECTS
        else if(options[kOptObject].doesOccur) {
            message="generating object code for %s\n";
            writeCode=&writeObjectCode;
        }
#endif
        else
        {
            message="generating C code for %s\n";
            writeCode=&writeCCode;
        }
        while(--argc) {
            filename=getLongPathname(argv[argc]);
            if (verbose) {
                fprintf(stdout, message, filename);
            }
            column=MAX_COLUMN;
            writeCode(filename, options[kOptDestDir].value);
        }
    }

    return 0;
}

static void
writeAssemblyCode(const char *filename, const char *destdir) {
    char entry[64];
    uint32_t buffer[1024];
    char *bufferStr = (char *)buffer;
    FileStream *in, *out;
    size_t i, length;

    in=T_FileStream_open(filename, "rb");
    if(in==NULL) {
        fprintf(stderr, "genccode: unable to open input file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    getOutFilename(filename, destdir, bufferStr, entry, ".s");
    out=T_FileStream_open(bufferStr, "w");
    if(out==NULL) {
        fprintf(stderr, "genccode: unable to open output file %s\n", bufferStr);
        exit(U_FILE_ACCESS_ERROR);
    }

    if(options[kOptEntryPoint].doesOccur) {
        uprv_strcpy(entry, options[kOptEntryPoint].value);
        uprv_strcat(entry, "_dat");
    }

    /* turn dashes or dots in the entry name into underscores */
    length=uprv_strlen(entry);
    for(i=0; i<length; ++i) {
        if(entry[i]=='-' || entry[i]=='.') {
            entry[i]='_';
        }
    }

    sprintf(bufferStr, assemblyHeader[assemblyHeaderIndex].header,
        entry, entry, entry, entry,
        entry, entry, entry, entry);
    T_FileStream_writeLine(out, bufferStr);
    T_FileStream_writeLine(out, assemblyHeader[assemblyHeaderIndex].beginLine);

    for(;;) {
        length=T_FileStream_read(in, buffer, sizeof(buffer));
        if(length==0) {
            break;
        }
        if (length != sizeof(buffer)) {
            /* pad with extra 0's when at the end of the file */
            for(i=0; i < (length % sizeof(uint32_t)); ++i) {
                buffer[length+i] = 0;
            }
        }
        for(i=0; i<(length/sizeof(buffer[0])); i++) {
            write32(out, buffer[i]);
        }
    }

    T_FileStream_writeLine(out, "\n");

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
writeCCode(const char *filename, const char *destdir) {
    char buffer[4096], entry[64];
    FileStream *in, *out;
    size_t i, length;

    in=T_FileStream_open(filename, "rb");
    if(in==NULL) {
        fprintf(stderr, "genccode: unable to open input file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    if(options[kOptName].doesOccur) { /* prepend  'icudt28_' */
      strcpy(entry, options[kOptName].value);
      strcat(entry, "_");
    } else {
      entry[0] = 0;
    }

    getOutFilename(filename, destdir, buffer, entry+uprv_strlen(entry), ".c");
    out=T_FileStream_open(buffer, "w");
    if(out==NULL) {
        fprintf(stderr, "genccode: unable to open output file %s\n", buffer);
        exit(U_FILE_ACCESS_ERROR);
    }

    /* turn dashes or dots in the entry name into underscores */
    length=uprv_strlen(entry);
    for(i=0; i<length; ++i) {
        if(entry[i]=='-' || entry[i]=='.') {
            entry[i]='_';
        }
    }
    
#ifdef OS400
    /*
    TODO: Fix this once the compiler implements this feature. Keep in sync with udatamem.c

    This is here because this platform can't currently put
    const data into the read-only pages of an object or
    shared library (service program). Only strings are allowed in read-only
    pages, so we use char * strings to store the data.

    In order to prevent the beginning of the data from ever matching the
    magic numbers we must still use the initial double.
    [grhoten 4/24/2003]
    */
    sprintf(buffer,
        "#define U_DISABLE_RENAMING 1\n"
        "#include \"unicode/umachine.h\"\n"
        "U_CDECL_BEGIN\n"
        "const struct {\n"
        "    double bogus;\n"
        "    const char *bytes; \n"
        "} %s={ 0.0, \n",
        entry);
    T_FileStream_writeLine(out, buffer);

    for(;;) {
        length=T_FileStream_read(in, buffer, sizeof(buffer));
        if(length==0) {
            break;
        }
        for(i=0; i<length; ++i) {
            write8str(out, (uint8_t)buffer[i]);
        }
    }

    T_FileStream_writeLine(out, "\"\n};\nU_CDECL_END\n");
#else
    /* Function renaming shouldn't be done in data */
    sprintf(buffer,
        "#define U_DISABLE_RENAMING 1\n"
        "#include \"unicode/umachine.h\"\n"
        "U_CDECL_BEGIN\n"
        "const struct {\n"
        "    double bogus;\n"
        "    uint8_t bytes[%ld]; \n"
        "} %s={ 0.0, {\n",
        (long)T_FileStream_size(in), entry);
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
#endif

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

#ifdef CAN_GENERATE_OBJECTS
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
    DWORD i, entryLength, length, size;

    in=T_FileStream_open(filename, "rb");
    if(in==NULL) {
        fprintf(stderr, "genccode: unable to open input file %s\n", filename);
        exit(U_FILE_ACCESS_ERROR);
    }

    /* entry have a leading '_' */
    entry[0]='_';
    getOutFilename(filename, destdir, buffer, entry+ICU_ENTRY_OFFSET, ".obj");

    if(options[kOptEntryPoint].doesOccur) {
        uprv_strcpy(entry+ICU_ENTRY_OFFSET, options[kOptEntryPoint].value);
        uprv_strcat(entry, "_dat");
    }
    /* turn dashes in the entry name into underscores */
    entryLength=(int32_t)uprv_strlen(entry);
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
    objHeader.fileHeader.Machine=ICU_OBJECT_MACHINE_TYPE;
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
        T_FileStream_write(out, buffer, (int32_t)length);
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
#endif

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
        if(options[kOptFilename].doesOccur) {
          uprv_strcpy(outFilename, options[kOptFilename].value);
        } else {
          uprv_strcpy(outFilename, inFilename);
        }
        uprv_strcat(outFilename, newSuffix);
    } else {
        char *saveOutFilename = outFilename;
        /* copy basename */
        while(inFilename<suffix) {
            if(*inFilename=='-') {
                /* iSeries cannot have '-' in the .o objects. */
                *outFilename++=*entryName++='_';
                inFilename++;
            }
            else {
                *outFilename++=*entryName++=*inFilename++;
            }
        }

        /* replace '.' by '_' */
        *outFilename++=*entryName++='_';
        ++inFilename;

        /* copy suffix */
        while(*inFilename!=0) {
            *outFilename++=*entryName++=*inFilename++;
        }

        *entryName=0;

        if(options[kOptFilename].doesOccur) {
            uprv_strcpy(saveOutFilename, options[kOptFilename].value);
            uprv_strcat(saveOutFilename, newSuffix); 
        } else {
            /* add ".c" */
            uprv_strcpy(outFilename, newSuffix);
        }
    }
}

static void
write32(FileStream *out, uint32_t bitField) {
    int32_t i;
    char bitFieldStr[64]; /* This is more bits than needed for a 32-bit number */
    char *s = bitFieldStr;
    uint8_t *ptrIdx = (uint8_t *)&bitField;
    static const char hexToStr[16] = {
        '0','1','2','3',
        '4','5','6','7',
        '8','9','A','B',
        'C','D','E','F'
    };

    /* write the value, possibly with comma and newline */
    if(column==MAX_COLUMN) {
        /* first byte */
        column=1;
    } else if(column<32) {
        *(s++)=',';
        ++column;
    } else {
        *(s++)='\n';
        uprv_strcpy(s, assemblyHeader[assemblyHeaderIndex].beginLine);
        s+=uprv_strlen(s);
        column=1;
    }

    if (bitField < 10) {
        /* It's a small number. Don't waste the space for 0x */
        *(s++)=hexToStr[bitField];
    }
    else {
        int seenNonZero = 0; /* This is used to remove leading zeros */

        *(s++)='0';
        *(s++)='x';

        /* This creates a 32-bit field */
#if U_IS_BIG_ENDIAN
        for (i = 0; i < sizeof(uint32_t); i++)
#else
        for (i = sizeof(uint32_t)-1; i >= 0 ; i--)
#endif
        {
            uint8_t value = ptrIdx[i];
            if (value || seenNonZero) {
                *(s++)=hexToStr[value>>4];
                *(s++)=hexToStr[value&0xF];
                seenNonZero = 1;
            }
        }
    }

    *(s++)=0;
    T_FileStream_writeLine(out, bitFieldStr);
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
    if(column==MAX_COLUMN) {
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

#ifdef OS400
static void
write8str(FileStream *out, uint8_t byte) {
    char s[8];

    if (byte > 7)
        sprintf(s, "\\x%X", byte);
    else
        sprintf(s, "\\%X", byte);

    /* write the value, possibly with comma and newline */
    if(column==MAX_COLUMN) {
        /* first byte */
        column=1;
        T_FileStream_writeLine(out, "\"");
    } else if(column<24) {
        ++column;
    } else {
        T_FileStream_writeLine(out, "\"\n\"");
        column=1;
    }
    T_FileStream_writeLine(out, s);
}
#endif

