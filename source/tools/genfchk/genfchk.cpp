/*
*******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
* created on: April 03 2001
* created by: Syn Wee Quek
*
* This program reads the FCDCheck text file, parses it and builds compact 
* binary tables for random-access lookup in a checkFCD() API function.
*
* fcdcheck.dat file format (after UDataInfo header etc. - see udata.c)
* (all data is static const)
*
* UDataInfo fields:
*   dataFormat "fchk"
*   formatVersion 1.0
*   dataVersion = Unicode version from -u or --unicode command line option, 
*   defaults to 3.0.0
*
* Data generated is a trie of normalization form corresponding to the index 
* code point.
* Hence codepoint 0xABCD will have normalization form 
* <code>
*    fcdcheck[codepoint] = 
*              STAGE_3_[STAGE_2_[STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
*              ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
*              (codepoint & STAGE_3_MASK_)];
* </code>
* value is 2 byte containing 2 sets of 8 bits information.<br>
* 1st byte : combining class of the first character in the NFD form of the 
*            codepoint
* 2nd byte : combining class of the last character in the NFD form of the 
*            codepoint
*
* Output file format
* - Header
* - Stage 1 index in memory set of uint16_t
* - Stage 2 index in memory set of uint16_t
* - Stage 3 index in memory set of uint16_t
* - Stage 1
* - Stage 2
* - Stage 3
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "unewdata.h"
#include "uoptions.h"
#include "filestrm.h"

#define INPUT_FILE_NAME_  "FCDCheck.txt"
#define DATA_NAME_        "fchk"
#define DATA_TYPE_        "dat"
#define DATA_BUFFER_SIZE_ 100
#define VERSION_STRING_   "fchk"

/* UDataInfo cf. udata.h */
static UDataInfo DATA_INFO_ = {
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x66, 0x63, 0x68, 0x6b},     /* dataFormat="qchk" */
    {1, 0, 0, 0},                 /* formatVersion */
    {3, 0, 0, 0}                  /* dataVersion */
};

static UBool BE_VERBOSE_     = FALSE, 
             BE_QUIET_       = FALSE, 
             HAVE_COPYRIGHT_ =TRUE;

static UOption OPTIONS_[] = {
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_QUIET,
    UOPTION_COPYRIGHT,
    UOPTION_DESTDIR,
    UOPTION_SOURCEDIR,
    { "unicode", NULL, NULL, NULL, 'u', UOPT_REQUIRES_ARG, 0 }
};

/* Stage 1 values for Trie */
static uint16_t STAGE_1_[0x800];
static uint16_t STAGE_1_SIZE_;

/* Stage 2 values for Trie */
static uint16_t STAGE_2_[0xFFFF];
static uint16_t STAGE_2_SIZE_;

/* Stage 3 values for Trie */
static uint16_t STAGE_3_[0xFFFF];
static uint16_t STAGE_3_SIZE_;

/* generate output data ----------------------------------------------------- */

static UBool
parseTrieStage(char *pline, UBool *passflag, uint16_t *pstage, 
               uint16_t *psize, UErrorCode *perror)
{
    char     *pend;

    /* gets the first block of code points */
    while (!(*passflag) && *pline != '{' && *pline != 0) {
        ++ pline;
    }

    /* error in a field function? */
    if (*pline == '\n') {
        *perror = U_PARSE_ERROR;
        return FALSE;
    }
    
    /* first line is just declarations */
    if (!(*passflag)) {
        *passflag = TRUE;
        return TRUE;
    }

    /* proceeding with the real block of data */
    while (*pline != '\n') {
        if (*pline == '}') {
            return FALSE;
        }

        /* read one value by the default base*/
        pstage[*psize] = (uint16_t)uprv_strtoul(pline, &pend, 0);
        
        (*psize) ++;

        if (*pend == '\n')
            return TRUE;

        if (pend <= pline || (*pend != ',')) {
            fprintf(stderr, "genqchk: syntax error parsing trie at %s\n", 
                    pline);
            *perror = U_PARSE_ERROR;
            return FALSE;
        }

        pline = pend + 1;
        /* getting rid of space */
        while (*pline == ' ') {
            pline ++;
        }
    }

    return TRUE;
}

static void
parseDB(const char *filename) {
    char        line[DATA_BUFFER_SIZE_];
    UErrorCode  error      = U_ZERO_ERROR;
    FileStream *file       = T_FileStream_open(filename, "r");
    UBool       stage1     = TRUE;
    UBool       stage2     = TRUE;
    UBool       stage3     = TRUE;
    UBool       stage1pass = FALSE;
    UBool       stage2pass = FALSE;
    UBool       stage3pass = FALSE;
    
    if (file == NULL) {
        fprintf(stderr, "*** unable to open input file %s ***\n", filename);
        error = U_FILE_ACCESS_ERROR;
        return;
    }

    /* initializing variables */
    STAGE_1_SIZE_    = 0;
    STAGE_2_SIZE_    = 0;
    STAGE_3_SIZE_    = 0;

    while (T_FileStream_readLine(file, line, sizeof(line)) != NULL) {
        /* skip this line if it is empty or a comment or is a return value */
        if(line[0] == 0 || line[0] == '#' || line[0] == '\n') {
            continue;
        }

        if (stage1) {
            stage1 = parseTrieStage(line, &stage1pass, STAGE_1_, 
                                    &STAGE_1_SIZE_, &error);
        }
        else if (stage2) {
            stage2 = parseTrieStage(line, &stage2pass, STAGE_2_, 
                                    &STAGE_2_SIZE_, &error);
        }
        else if (stage3) {
            stage3 = parseTrieStage(line, &stage3pass, STAGE_3_, 
                                    &STAGE_3_SIZE_, &error);
        }
    }

    if (filename != NULL) {
        T_FileStream_close(file);
    }
}

static void
generateData(const char *dataDir) {
    UNewDataMemory *pData;
    UErrorCode      error = U_ZERO_ERROR;
    uint16_t        index = 0;

    pData=udata_create(dataDir, DATA_TYPE_, DATA_NAME_, &DATA_INFO_,
                       HAVE_COPYRIGHT_ ? U_COPYRIGHT_STRING : NULL, &error);
    if(U_FAILURE(error)) {
        fprintf(stderr, 
                "genfchk: unable to create data memory, error %d\n", 
                error);
        exit(error);
    }

    /* stage bit size */
    udata_write16(pData, 6);
    udata_write16(pData, 4);
    /* offsets in number of uint16_t*/
    /* stage 1 */
    index = 0;
    udata_write16(pData, index);
    /* stage 2 */
    index += STAGE_1_SIZE_;
    udata_write16(pData, index);
    /* stage 3 */
    index += STAGE_2_SIZE_;
    udata_write16(pData, index);
    udata_write16(pData, 0);
    udata_write16(pData, 0);
    udata_write16(pData, 0);
    
    udata_writeBlock(pData, STAGE_1_, STAGE_1_SIZE_ * sizeof(uint16_t));
    udata_writeBlock(pData, STAGE_2_, STAGE_2_SIZE_ * sizeof(uint16_t));
    udata_writeBlock(pData, STAGE_3_, STAGE_3_SIZE_ * sizeof(uint16_t));
    
    udata_finish(pData, &error);
    if (U_FAILURE(error)) {
        fprintf(stderr, "genfchk: error %d writing the output file\n", 
                error);
        exit(error);
    }
}


extern int
main(int argc, char* argv[]) {
          UVersionInfo  version;
          char          filename[300];
    const char         *srcDir        = NULL, 
                       *destDir       = NULL;
          char         *basename      = NULL;
    
    /* preset then read command line OPTIONS_ */
    OPTIONS_[5].value = u_getDataDirectory();
    OPTIONS_[6].value="";
    OPTIONS_[7].value="3.0.0";

    argc = u_parseArgs(argc, argv, sizeof(OPTIONS_) / sizeof(OPTIONS_[0]), 
                       OPTIONS_);

    /* error handling, printing usage message */
    if (argc < 0) {
        fprintf(stderr, "error in command line argument \"%s\"\n", 
                argv[-argc]);
    } 
    
    if (argc < 0 || OPTIONS_[0].doesOccur || OPTIONS_[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-1[+|-]] [-v[+|-]] [-c[+|-]] filename\n"
            "\tread the FCDCheck.txt file and \n"
            "\tcreate a binary file " DATA_NAME_ "." DATA_TYPE_ "\n" 
            "\t\tfilename  absolute path/filename for the\n" 
            "\t\t\tQuickCheck text file (default: standard input)\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-v or --verbose     verbose output\n" 
            "\t\t-q or --quiet       no output\n" 
            "\t\t-c or --copyright   include a copyright notice\n" 
            "\t\t-d or --destdir     destination directory, followed by the path\n" 
            "\t\t-s or --sourcedir   source directory, followed by the path\n" 
            "\t\t-u or --unicode     Unicode version, followed by the version like 3.0.0\n",
            argv[0]);
        fprintf(stderr, argv[0]);
        return argc < 0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    /* get the OPTIONS_ values */
    BE_VERBOSE_     = OPTIONS_[2].doesOccur;
    BE_QUIET_       = OPTIONS_[3].doesOccur;
    HAVE_COPYRIGHT_ = OPTIONS_[4].doesOccur;
    destDir         = OPTIONS_[5].value;
    srcDir          = OPTIONS_[6].value;
    
    /* set the Unicode version */
    u_versionFromString(version, OPTIONS_[7].value);
    uprv_memcpy(DATA_INFO_.dataVersion, version, 4);

    /* prepare the filename beginning with the source dir */
    uprv_strcpy(filename, srcDir);
    basename = filename + uprv_strlen(filename);
    if (basename > filename && *(basename - 1) != U_FILE_SEP_CHAR) {
        *basename ++ = U_FILE_SEP_CHAR;
    }

    uprv_strcpy(basename, INPUT_FILE_NAME_);
    
    parseDB(filename);
    generateData(OPTIONS_[5].value);

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

