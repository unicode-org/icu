/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  icuswap.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003aug08
*   created by: Markus W. Scherer
*
*   This tool takes an ICU data file and "swaps" it, that is, changes its
*   platform properties between big-/little-endianness and ASCII/EBCDIC charset
*   families.
*   The modified data file is written to a new file.
*   Useful as an install-time tool for shipping only one flavor of ICU data
*   and preparing data files for the target platform.
*   Will not work with data DLLs (shared libraries).
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/udata.h"
#include "udataswp.h"
#include "uoptions.h"

/* swapping implementations in common */

#include "uresdata.h"
#include "ucnv_io.h"
#include "uprops.h"

/* swapping implementations in i18n */

/* definitions */

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_DEF("type", 't', UOPT_REQUIRES_ARG)
};

enum {
    OPT_HELP_H,
    OPT_HELP_QUESTION_MARK,
    OPT_OUT_TYPE
};

static int32_t
fileSize(FILE *f) {
    int32_t size;

    fseek(f, 0, SEEK_END);
    size=(int32_t)ftell(f);
    fseek(f, 0, SEEK_SET);
    return size;
}

/**
 * Identifies and then transforms the ICU data piece in-place, or determines
 * its length. See UDataSwapFn.
 * This function handles .dat data packages as well as single data pieces
 * and internally dispatches to per-type swap functions.
 * Sets a U_UNSUPPORTED_ERROR if the data format is not recognized.
 *
 * @see UDataSwapFn
 * @see udata_openSwapper
 * @see udata_openSwapperForInputData
 * @draft ICU 2.8
 */
static int32_t
udata_swap(const UDataSwapper *ds,
           const void *inData, int32_t length, void *outData,
           UErrorCode *pErrorCode);

static void U_CALLCONV
printError(void *context, const char *fmt, va_list args) {
    vfprintf((FILE *)context, fmt, args);
}

static int
printUsage(const char *pname, UBool ishelp) {
    fprintf(stderr,
            "%csage: %s [ -h, -?, --help ] -tl|-tb|-te|--type=b|... infilename outfilename\n",
            ishelp ? 'U' : 'u', pname);
    if(ishelp) {
        fprintf(stderr,
              "\nOptions: -h, -?, --help    print this message and exit\n"
                "         Read the input file, swap its platform properties according\n"
                "         to the -t or --type option, and write the result to the output file.\n"
                "         -tl               change to little-endian/ASCII charset family\n"
                "         -tb               change to little-endian/ASCII charset family\n"
                "         -te               change to little-endian/EBCDIC charset family\n");
    }

    return !ishelp;
}

extern int
main(int argc, char *argv[]) {
    FILE *in, *out;
    const char *pname;
    char *data;
    int32_t length;
    UBool ishelp;
    int rc;

    UDataSwapper *ds;
    UErrorCode errorCode;
    uint8_t outCharset;
    UBool outIsBigEndian;

    U_MAIN_INIT_ARGS(argc, argv);

    /* get the program basename */
    pname=strrchr(argv[0], U_FILE_SEP_CHAR);
    if(pname==NULL) {
        pname=strrchr(argv[0], '/');
    }
    if(pname!=NULL) {
        ++pname;
    } else {
        pname=argv[0];
    }

    argc=u_parseArgs(argc, argv, LENGTHOF(options), options);
    ishelp=options[OPT_HELP_H].doesOccur || options[OPT_HELP_QUESTION_MARK].doesOccur;
    if(ishelp || argc!=3) {
        return printUsage(pname, ishelp);
    }

    /* parse the output type option */
    data=(char *)options[OPT_OUT_TYPE].value;
    if(data[0]==0 || data[1]!=0) {
        /* the type must be exactly one letter */
        return printUsage(pname, FALSE);
    }
    switch(data[0]) {
    case 'l':
        outIsBigEndian=FALSE;
        outCharset=U_ASCII_FAMILY;
        break;
    case 'b':
        outIsBigEndian=TRUE;
        outCharset=U_ASCII_FAMILY;
        break;
    case 'e':
        outIsBigEndian=TRUE;
        outCharset=U_EBCDIC_FAMILY;
        break;
    default:
        return printUsage(pname, FALSE);
    }

    in=out=NULL;
    data=NULL;

    /* open the input file, get its length, allocate memory for it, read the file */
    in=fopen(argv[1], "rb");
    if(in==NULL) {
        fprintf(stderr, "%s: unable to open input file \"%s\"\n", pname, argv[1]);
        rc=2;
        goto done;
    }

    length=fileSize(in);
    if(length<=0) {
        fprintf(stderr, "%s: empty input file \"%s\"\n", pname, argv[1]);
        rc=2;
        goto done;
    }

    data=(char *)malloc(length);
    if(data==NULL) {
        fprintf(stderr, "%s: error allocating memory for \"%s\"\n", pname, argv[1]);
        rc=2;
        goto done;
    }

    if(length!=(int32_t)fread(data, 1, length, in)) {
        fprintf(stderr, "%s: error reading \"%s\"\n", pname, argv[1]);
        rc=3;
        goto done;
    }

    fclose(in);
    in=NULL;

    /* swap the data in-place */
    errorCode=U_ZERO_ERROR;
    ds=udata_openSwapperForInputData(data, length, outIsBigEndian, outCharset, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "%s: udata_openSwapperForInputData(\"%s\") failed - %s\n",
                pname, argv[1], u_errorName(errorCode));
        rc=4;
        goto done;
    }

    ds->printError=printError;
    ds->printErrorContext=stderr;

    length=udata_swap(ds, data, length, data, &errorCode);
    udata_closeSwapper(ds);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "%s: udata_swap(\"%s\") failed - %s\n",
                pname, argv[1], u_errorName(errorCode));
        rc=4;
        goto done;
    }

    out=fopen(argv[2], "wb");
    if(out==NULL) {
        fprintf(stderr, "%s: unable to open output file \"%s\"\n", pname, argv[2]);
        rc=5;
        goto done;
    }

    if(length!=(int32_t)fwrite(data, 1, length, out)) {
        fprintf(stderr, "%s: error writing \"%s\"\n", pname, argv[2]);
        rc=6;
        goto done;
    }

    fclose(out);
    out=NULL;

    /* all done */
    rc=0;

done:
    if(in!=NULL) {
        fclose(in);
    }
    if(out!=NULL) {
        fclose(out);
    }
    if(data!=NULL) {
        free(data);
    }
    return rc;
}

/* swap the data ------------------------------------------------------------ */

static const struct {
    uint8_t dataFormat[4];
    UDataSwapFn *swapFn;
} swapFns[]={
    { { 0x52, 0x65, 0x73, 0x42 }, ures_swap },          /* dataFormat="ResB" */
    /* insert data formats here, descending by expected frequency of occurrence */
    { { 0x55, 0x50, 0x72, 0x6f }, uprops_swap },        /* dataFormat="UPro" */
    { { 0x43, 0x76, 0x41, 0x6c }, ucnv_swapAliases }    /* dataFormat="CvAl" */
};

static int32_t
udata_swap(const UDataSwapper *ds,
           const void *inData, int32_t length, void *outData,
           UErrorCode *pErrorCode) {
    const UDataInfo *pInfo;
    int32_t headerSize, i;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /*
     * Preflight the header first; checks for illegal arguments, too.
     * Do not swap the header right away because the format-specific swapper
     * will swap it, get the headerSize again, and also use the header
     * information. Otherwise we would have to pass some of the information
     * and not be able to use the UDataSwapFn signature.
     */
    headerSize=udata_swapDataHeader(ds, inData, -1, NULL, pErrorCode);

    /*
     * If we wanted udata_swap() to also handle non-loadable data like a UTrie,
     * then we could check here for further known magic values and structures.
     */
    if(U_FAILURE(*pErrorCode)) {
        return 0; /* the data format was not recognized */
    }

    /* dispatch to the swap function for the dataFormat */
    pInfo=(const UDataInfo *)((const char *)inData+4);
    for(i=0; i<LENGTHOF(swapFns); ++i) {
        if(0==memcmp(swapFns[i].dataFormat, pInfo->dataFormat, 4)) {
            return swapFns[i].swapFn(ds, inData, length, outData, pErrorCode);
        }
    }

    /* the dataFormat was not recognized */
    udata_printError(ds, "udata_swap(): unknown data format %02x.%02x.%02x.%02x\n",
                     pInfo->dataFormat[0], pInfo->dataFormat[1],
                     pInfo->dataFormat[2], pInfo->dataFormat[3]);
    *pErrorCode=U_UNSUPPORTED_ERROR;
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
