/*  
**********************************************************************
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  utfperf.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002apr17
*   created by: Markus W. Scherer
*
*   Performance test program for Unicode converters
*   (converters that support all Unicode code points).
*   Takes a UTF-8 file as input.
*/

#include <stdio.h>
#include <string.h>

#include <fcntl.h>	/* for _O_BINARY */
#include <io.h>		/* for _setmode() */

#if defined(WIN32) || defined(_WIN32) || defined(WIN64) || defined(_WIN64)
#   include <windows.h>
#else
#   include <sys/time.h>
    static unsigned long
    timeGetTime() {
        struct timeval t;

        gettimeofday(&t, 0);
        return t.tv_sec*1000+t.tv_usec/1000;
    };
#endif

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"

/* definitions and text buffers */

#define INPUT_CAPACITY (1024*1024)
#define INTERMEDIATE_CAPACITY 4096
#define INTERMEDIATE_SMALL_CAPACITY 20
#define OUTPUT_CAPACITY INPUT_CAPACITY

#define TARGET_MEASURE_TIME_MS 2000

#define PERCENT(a, b) (int)(((a)*200+1)/(2*(b)))

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof((a)[0]))

static UChar input[INPUT_CAPACITY], output[OUTPUT_CAPACITY];
static char intermediate[INTERMEDIATE_CAPACITY];

static int32_t inputLength, encodedLength, outputLength, countInputCodePoints;

static int32_t utf8Length=0;
static double utf8Time=0.;

static const char *const
utfNames[]={
    "UTF-8", /* UTF-8 should always be first to serve as percentage reference */
    "SCSU", "BOCU-1" /*, "CESU-8" *//*, "UTF-16BE", "UTF-16LE"*//*, "GB18030"*/
};

/* functions */

typedef void
RoundtripFn(UConverter *cnv, int32_t intermediateCapacity, UErrorCode *pErrorCode);

static void
roundtrip(UConverter *cnv, int32_t intermediateCapacity, UErrorCode *pErrorCode) {
    const UChar *pIn, *pInLimit;
    UChar *pOut, *pOutLimit;
    char *pInter, *pInterLimit, *p;
    UBool flush;

    ucnv_reset(cnv);

    pIn=input;
    pInLimit=input+inputLength;

    pOut=output;
    pOutLimit=output+OUTPUT_CAPACITY;

    pInterLimit=intermediate+intermediateCapacity;

    encodedLength=outputLength=0;
    flush=FALSE;

    while(pIn<pInLimit || !flush) {
        /* convert a block of [pIn..pInLimit[ to the encoding in intermediate[] */
        pInter=intermediate;
        flush=(UBool)(pIn==pInLimit);
        ucnv_fromUnicode(cnv,
                         &pInter, pInterLimit,
                         &pIn, pInLimit,
                         NULL, flush,
                         pErrorCode);
        encodedLength+=(int32_t)(pInter-intermediate);

        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            /* in case flush was TRUE make sure that we convert once more to really flush */
            flush=FALSE;
            *pErrorCode=U_ZERO_ERROR;
        } else if(U_FAILURE(*pErrorCode)) {
            return;
        }

        /* convert the block [intermediate..pInter[ back to UTF-16 */
        p=intermediate;
        ucnv_toUnicode(cnv,
                       &pOut, pOutLimit,
                       &p, pInter,
                       NULL, flush,
                       pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return;
        }
        /* intermediate must have been consumed (p==pInter) because of the converter semantics */
    }

    outputLength=pOut-output;
    if(inputLength!=outputLength) {
        fprintf(stderr, "error: roundtrip failed, inputLength %d!=outputLength %d\n", inputLength, outputLength);
        *pErrorCode=U_INTERNAL_PROGRAM_ERROR;
    }
}

static void
noop(UConverter *cnv, int32_t intermediateCapacity, UErrorCode *pErrorCode) {
    /* do nothing */
}

static unsigned long
measureRoundtrips(RoundtripFn *fn, UConverter *cnv, const char *encName, int32_t intermediateCapacity, int32_t n) {
    unsigned long _time;
    UErrorCode errorCode;

    _time=timeGetTime();
    errorCode=U_ZERO_ERROR;
    do {
        fn(cnv, intermediateCapacity, &errorCode);
    } while(U_SUCCESS(errorCode) && --n>0);
    _time=timeGetTime()-_time;

    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error in roundtrip conversion (%s): %s\n", encName, u_errorName(errorCode));
        return 0x7fffffff;
    }

    if(0!=u_memcmp(input, output, inputLength)) {
        fprintf(stderr, "error: roundtrip failed, input[]!=output[]\n");
        return 0x7fffffff;
    }

    return _time;
}

static void
perEncAndCapacity(UConverter *cnv, const char *encName, int32_t intermediateCapacity) {
    double rtTime;
    unsigned long _time;
    int32_t n;

    /*printf("test performance for %s with intermediate capacity %d\n", encName, intermediateCapacity);*/

    /* warm up caches and estimate loop time */
    n=10;
    for(;;) {
        _time=measureRoundtrips(roundtrip, cnv, encName, intermediateCapacity, n);
        if(_time<500 && _time<TARGET_MEASURE_TIME_MS/10) {
            n*=10;
        } else {
            break;
        }
    }

    if(_time<TARGET_MEASURE_TIME_MS) {
        n=(n*TARGET_MEASURE_TIME_MS)/_time+1;
    }

    /* run actual measurement with a target test time of 10s */
    _time=measureRoundtrips(roundtrip, cnv, encName, intermediateCapacity, n);

    /* subtract same number of loops over no-operation function */
    _time-=measureRoundtrips(noop, cnv, encName, intermediateCapacity, n);

    rtTime=((double)_time*1000.)/(double)n;

    /* report */
    printf("* performance report for                %8s:\n", encName);
    printf("  intermediate buffer capacity          %8d B\n", intermediateCapacity);
    if(intermediateCapacity==INTERMEDIATE_CAPACITY && utf8Length!=0) {
        printf("  number of encoding bytes              %8d B  (%3d%% of UTF-8)\n", encodedLength, PERCENT(encodedLength, utf8Length));
        printf("  roundtrip conversion time             %8g &#956;s (%3d%% of UTF-8)\n", rtTime, PERCENT(rtTime, utf8Time));
    } else {
        printf("  number of encoding bytes              %8d B\n", encodedLength);
        printf("  roundtrip conversion time             %8g &#956;s\n", rtTime);
    }
    printf("  average bytes/code point              %8g B/cp\n", (double)encodedLength/countInputCodePoints);
    puts("");

    /* set UTF-8 values */
    if(intermediateCapacity==INTERMEDIATE_CAPACITY && 0==strcmp(encName, "UTF-8")) {
        utf8Length=encodedLength;
        utf8Time=rtTime;
    }
}

static void
perEnc(UConverter *cnv, const char *encName) {
    /*printf("test performance for %s\n", encName);*/
    perEncAndCapacity(cnv, encName, INTERMEDIATE_CAPACITY);
    perEncAndCapacity(cnv, encName, INTERMEDIATE_SMALL_CAPACITY);
}

static void
testPerformance() {
    UConverter *cnv;
    UErrorCode errorCode;
    int32_t i;

    printf("number of code points                   %8d cp\n", countInputCodePoints);
    printf("platform endianness:                    %8s-endian\n", U_IS_BIG_ENDIAN ? "big" : "little");
    puts("");
    for(i=0; i<ARRAY_LENGTH(utfNames); ++i) {
        errorCode=U_ZERO_ERROR;
        cnv=ucnv_open(utfNames[i], &errorCode);
        if(U_SUCCESS(errorCode)) {
            perEnc(cnv, utfNames[i]);
            ucnv_close(cnv);
        } else {
            fprintf(stderr, "error opening converter for \"%s\" - %s\n", utfNames[i], u_errorName(errorCode));
        }
    }
}

/* read a complete block from the input file */
static int32_t
readBlock(FILE *in) {
    int length, blockLength;

    blockLength=0;
    while(blockLength<INTERMEDIATE_CAPACITY && !feof(in)) {
        length=fread(intermediate, 1, INTERMEDIATE_CAPACITY-blockLength, in);
        if(length<0 || ferror(in)) {
            return -1;
        }
        blockLength+=length;
    }

    return (int32_t)blockLength;
}

static UBool
readInput(FILE *in, const char *encName) {
    UConverter *cnv;
    UChar *pOut, *pOutLimit;
    const char *p, *limit;
    int32_t length;
    UErrorCode errorCode;

    pOut=input;
    pOutLimit=input+INPUT_CAPACITY;

    errorCode=U_ZERO_ERROR;

    /* read the first block and open the converter */
    length=readBlock(in);
    if(length<0) {
        return FALSE;
    }

    if(encName==NULL) {
        int32_t signatureLength;
        encName=ucnv_detectUnicodeSignature(intermediate, length,
                                            &signatureLength,
                                            &errorCode);
        if(U_FAILURE(errorCode) || encName==NULL) {
            /* default to UTF-8 */
            printf("no Unicode signature - using UTF-8\n");
            encName="UTF-8";
            errorCode=U_ZERO_ERROR;
        } else {
            printf("detected signature for %s (removing %d bytes)\n", encName, signatureLength);
            /* remove signature byte sequence */
            memmove(intermediate, intermediate+signatureLength, length-=signatureLength);
        }
    }

    cnv=ucnv_open(encName, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error: unable to ucnv_open(\"%s\") - %s\n", encName, u_errorName(errorCode));
        return FALSE;
    }

    while(length>0) {
        /* convert the block */
        p=intermediate;
        limit=p+length;

        ucnv_toUnicode(cnv,
                       &pOut, pOutLimit,
                       &p, limit,
                       NULL, FALSE,
                       &errorCode);
        if(U_FAILURE(errorCode)) {
            fprintf(stderr, "error converting input to UTF-16: %s\n", u_errorName(errorCode));
            ucnv_close(cnv);
            return FALSE;
        }

        /* read the next block */
        length=readBlock(in);
        if(length<0) {
            ucnv_close(cnv);
            return FALSE;
        }
    }

    /* flush the converter */
    ucnv_toUnicode(cnv,
                   &pOut, pOutLimit,
                   &p, p,
                   NULL, TRUE,
                   &errorCode);
    ucnv_close(cnv);

    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error converting input to UTF-16: %s\n", u_errorName(errorCode));
        return FALSE;
    }

    inputLength=(int32_t)(pOut-input);
    countInputCodePoints=u_countChar32(input, inputLength);
    if(inputLength<=0) {
        fprintf(stderr, "warning: input is empty\n");
        return FALSE;
    }

    return TRUE;
}

static void
showUsage(const char *myName) {
    fprintf(stderr,
            "Usage:\n"
            "%s [-e encoding-name] filename | '-'\n"
            "    encoding-name must be the name of an encoding supported by ICU\n"
            "    the filename of the input file with text to be used\n"
            "      can be a dash (-) for standard input\n",
            myName);
}

/*
 * Read file using some encoding, convert to 1M UTF-16 input buffer.
 * For each UTF to be tested:
 *   n times:
 *     convert from UTF-16 input buffer to UTF, 4kB buffer
 *     convert from 4kB buffer to 1M UTF-16 output buffer
 *   adjust n so that time elapsed is 10s (#define)
 *     ->divide 10s by time, increase n by that factor, run 2nd time
 *   n times:
 *     empty function
 *   subtract out loop/function overhead
 *   display #code points - #UTF bytes - time per roundtrip
 *
 *   * do the same again with an intermediate buffer size of 20 instead of 4kB
 *
 * Test following UTFs:
 * UTF-16BE, UTF-16LE, UTF-8, SCSU, BOCU-1, CESU-8
 *
 * Command-line arguments:
 * - encoding (default UTF-8, detect BOM)
 * - filename (allow "-")
 */
extern int
main(int argc, const char *argv[]) {
    FILE *in;
    const char *myName, *encName, *filename, *basename;

    myName=argv[0];
    if(argc<2) {
        showUsage(myName);
        return 1;
    }

    /* get encoding name argument */
    if(argv[1][0]=='-' && argv[1][1]=='e') {
        encName=argv[1]+2;
        --argc;
        ++argv;
        if(*encName==0) {
            if(argc<2) {
                showUsage(myName);
                return 1;
            }
            encName=argv[1];
            --argc;
            ++argv;
        }
    } else {
        encName=NULL;
    }

    /* get filename argument */
    if(argc<2) {
        showUsage(myName);
        return 1;
    }
    filename=argv[1];
    if(filename[0]=='-' && filename[1]==0) {
        filename="(standard input)";
        in=stdin;
        /* set stdin to binary mode */
        _setmode(_fileno(stdin), _O_BINARY);
    } else {
        in=fopen(filename, "rb");
        if(in==NULL) {
            fprintf(stderr, "error opening \"%s\"\n", filename);
            showUsage(myName);
            return 2;
        }
    }

    /* read input */
    basename=strrchr(filename, U_FILE_SEP_CHAR);
    if(basename!=NULL) {
        ++basename;
    } else {
        basename=filename;
    }
    printf("# testing converter performance with file \"%s\"\n", basename);
    if(!readInput(in, encName)) {
        fprintf(stderr, "error reading \"%s\" (encoding %s)\n", filename, encName);
        showUsage(myName);
        return 2;
    }
    if(in!=stdin) {
        fclose(in);
    }

    /* test performance */
    testPerformance();
    return 0;
}
