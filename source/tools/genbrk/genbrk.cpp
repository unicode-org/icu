/*
**********************************************************************
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File genbrk.c
*/

//--------------------------------------------------------------------
//
//   Tool for generating RuleBasedBreakIterator data files (.brk files).
//   .brk files contain the precompiled rules for standard types
//   of iterators - word, line, sentence, etc.
//
//   Usage:  genbrk [options] -r rule-file.txt  -o output-file.brk
//
//       options:   -v         verbose
//                  -? or -h   help
//
//   The input rule file is a plain text file containing break rules
//    in the input format accepted by RuleBasedBreakIterators.  The
//    file can be encoded as utf-8, or utf-16 (either endian), or
//    in the default code page (platform dependent.).  utf encoded
//    files must include a BOM.
//
//--------------------------------------------------------------------

#include <stdio.h>
#include <stdlib.h>

#include "unicode/ucnv.h"
#include "unicode/unistr.h"
#include "unicode/rbbi.h"
#include "unicode/uclean.h"
#include "unicode/udata.h"

#include "uoptions.h"
#include "ucmndata.h"

static char *progName;
static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    { "rules", NULL, NULL, NULL, 'r', UOPT_REQUIRES_ARG, 0 },
    { "out",   NULL, NULL, NULL, 'o', UOPT_REQUIRES_ARG, 0 }
};

void usageAndDie(int retCode) {
        printf("Usage: %s [-v] -r rule-file -o output-file\n", progName);
        exit (retCode);
}

//----------------------------------------------------------------------------
//
//  main      for genbrk
//
//----------------------------------------------------------------------------
int  main(int argc, char **argv) {
    UErrorCode  status = U_ZERO_ERROR;
    const char *ruleFileName;
    const char *outFileName;

    //
    // Pick up and check the command line arguments,
    //    using the standard ICU tool utils option handling.
    //
    progName = argv[0];
    U_MAIN_INIT_ARGS(argc, argv);
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);
    if(argc<0) {
        // Unrecognized option
        fprintf(stderr, "error in command line argument \"%s\"\n", argv[-argc]);
        usageAndDie(U_ILLEGAL_ARGUMENT_ERROR);
    }

    if(options[0].doesOccur || options[1].doesOccur) {
        //  -? or -h for help.
        usageAndDie(0);
    }

    if (!(options[3].doesOccur && options[4].doesOccur)) {
        fprintf(stderr, "rule file and output file must both be specified.\n");
        usageAndDie(U_ILLEGAL_ARGUMENT_ERROR);
    }
    ruleFileName = options[3].value;
    outFileName  = options[4].value;

    //
    //  Read in the rule source file
    //
    int         result;
    long        ruleFileSize;
    FILE        *file;
    char        *ruleBufferC;

    file = fopen(ruleFileName, "rb");
    if( file == 0 ) {
        fprintf(stderr, "Could not open file \"%s\"\n", ruleFileName);
        exit(-1);
    }
    fseek(file, 0, SEEK_END);
    ruleFileSize = ftell(file);
    fseek(file, 0, SEEK_SET);
    ruleBufferC = new char[ruleFileSize+10];

    result = fread(ruleBufferC, 1, ruleFileSize, file);
    if (result != ruleFileSize)  {
        fprintf(stderr, "Error reading file \"%s\"\n", ruleFileName);
        exit (-1);
    }
    ruleBufferC[ruleFileSize]=0;
    fclose(file);

    //
    // Look for a Unicode Signature (BOM) on the rule file
    //
    int32_t        signatureLength;
    const char *   ruleSourceC = ruleBufferC;
    const char*    encoding = ucnv_detectUnicodeSignature(
                           ruleSourceC, ruleFileSize, &signatureLength, &status);
    if (U_FAILURE(status)) {
        exit(status);
    }
    if(encoding!=NULL ){
        ruleSourceC  += signatureLength;
        ruleFileSize -= signatureLength;
    }

    //
    // Open a converter to take the rule file to UTF-16
    //
    UConverter* conv;
    conv = ucnv_open(encoding, &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "ucnv_open: ICU Error \"%s\"\n", u_errorName(status));
        exit(status);
    }

    //
    // Convert the rules to UChar.
    //  Preflight first to determine required buffer size.
    //
    uint32_t destCap = ucnv_toUChars(conv,
                       NULL,           //  dest,
                       0,              //  destCapacity,
                       ruleSourceC,
                       ruleFileSize,
                       &status);
    if (status != U_BUFFER_OVERFLOW_ERROR) {
        fprintf(stderr, "ucnv_toUChars: ICU Error \"%s\"\n", u_errorName(status));
        exit(status);
    };

    status = U_ZERO_ERROR;
    UChar *ruleSourceU = new UChar[destCap+1];
    ucnv_toUChars(conv,
                  ruleSourceU,     //  dest,
                  destCap+1,
                  ruleSourceC,
                  ruleFileSize,
                  &status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "ucnv_toUChars: ICU Error \"%s\"\n", u_errorName(status));
        exit(status);
    };
    ucnv_close(conv);


    //
    //  Put the source rules into a UnicodeString
    //
    UnicodeString ruleSourceS(FALSE, ruleSourceU, destCap);

    //
    //  Create the break iterator from the rules
    //     This will compile the rules.
    //
    UParseError parseError;
    RuleBasedBreakIterator *bi = new RuleBasedBreakIterator(ruleSourceS, parseError, status);
    if (U_FAILURE(status)) {
        fprintf(stderr, "createRuleBasedBreakIterator: ICU Error \"%s\"  at line %d, column %d\n",
                u_errorName(status), parseError.line, parseError.offset);
        exit(status);
    };


    //
    //  Get the compiled rule data from the break iterator.
    //
    uint32_t        outDataSize;
    const uint8_t  *outData;
    outData = bi->getFlattenedData(&outDataSize);


    //
    //  Create the output file
    //
    size_t bytesWritten;
    file = fopen(outFileName, "wb");
    if (file == 0) {
        fprintf(stderr, "Could not open output file \"%s\"\n", outFileName);
        exit(-1);
    }


    //
    //  Set up the ICU data header, defined in ucmndata.h
    //
    DataHeader dh ={
        {sizeof(DataHeader),           // Struct MappedData
            0xda,
            0x27},

        {                               // struct UDataInfo
            sizeof(UDataInfo),          //     size
            0,                          //     reserved
            U_IS_BIG_ENDIAN,
            U_CHARSET_FAMILY,
            U_SIZEOF_UCHAR,
            0,                          //     reserved

        { 0x42, 0x72, 0x6b, 0x20 },     //     dataFormat="Brk "
        { 2, 1, 0, 0 },                 //     formatVersion
            { 3, 1, 0, 0 }                //   dataVersion (Unicode version)
        }};
    bytesWritten = fwrite(&dh, 1, sizeof(DataHeader), file);

    //
    //  Write the data itself.
    //
    bytesWritten = fwrite(outData, 1, outDataSize, file);
    if (bytesWritten != outDataSize) {
        fprintf(stderr, "Error writing to output file \"%s\"\n", outFileName);
        exit(-1);
    }

    fclose(file);
    delete bi;
    delete ruleSourceU;
    delete ruleBufferC;
    u_cleanup();


    printf("genbrk: tool completed successfully.\n");
    return 0;
}
