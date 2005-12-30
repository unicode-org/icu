/*  
**********************************************************************
*   Copyright (C) 2002-2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  utfperf.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005Nov17
*   created by: Raymond Yang
*
*   Ported from utfper.c created by Markus W. Scherer
*   Performance test program for Unicode converters
*/

#include <stdio.h>
#include "unicode/uperf.h"


/* definitions and text buffers */

#define INPUT_CAPACITY (1024*1024)
#define INTERMEDIATE_CAPACITY 4096
#define INTERMEDIATE_SMALL_CAPACITY 20
#define OUTPUT_CAPACITY INPUT_CAPACITY

static UChar input[INPUT_CAPACITY];
static UChar output[OUTPUT_CAPACITY];
static char intermediate[INTERMEDIATE_CAPACITY];

static int32_t inputLength, encodedLength, outputLength, countInputCodePoints;


class Command : public UPerfFunction {
private:
    Command(const char * name, int32_t buf_cap):name(name),buf_cap(buf_cap){
        errorCode=U_ZERO_ERROR;
        cnv=ucnv_open(name, &errorCode);
    }
public:
    static UPerfFunction* get(const char * name, int32_t buf_cap){
        Command * t = new Command(name, buf_cap);
        if (U_SUCCESS(t->errorCode)){
            return t;
        } else {
            //fprintf(stderr, "error opening converter for \"%s\" - %s\n", name, u_errorName(errorCode));
            delete t;
            return NULL;
        }
    }
    virtual ~Command(){
        if(U_SUCCESS(errorCode)) {
            ucnv_close(cnv);
        }
    }
    virtual void call(UErrorCode* pErrorCode){
        const UChar *pIn, *pInLimit;
        UChar *pOut, *pOutLimit;
        char *pInter, *pInterLimit;
        const char *p;
        UBool flush;

        ucnv_reset(cnv);

        pIn=input;
        pInLimit=input+inputLength;

        pOut=output;
        pOutLimit=output+OUTPUT_CAPACITY;

        pInterLimit=intermediate+buf_cap;

        encodedLength=outputLength=0;
        flush=FALSE;

        while(pIn<pInLimit || !flush) {
            /* convert a block of [pIn..pInLimit[ to the encoding in intermediate[] */
            pInter=intermediate;
            flush=(UBool)(pIn==pInLimit);
            ucnv_fromUnicode(cnv, &pInter, pInterLimit, &pIn, pInLimit, NULL, flush, pErrorCode);
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
            ucnv_toUnicode(cnv, &pOut, pOutLimit,&p, pInter,NULL, flush,pErrorCode);
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
    virtual long getOperationsPerIteration(){
        return countInputCodePoints;
    }
    const char * name;
    int32_t buf_cap;
    UErrorCode errorCode;
    UConverter *cnv;
};

class  UtfPerformanceTest : public UPerfTest{
public:
    UtfPerformanceTest(int32_t argc, const char *argv[], UErrorCode &status) :UPerfTest(argc,argv,status){
        getBuffer(inputLength, status);
        u_strncpy(input, buffer, inputLength);
        countInputCodePoints = u_countChar32(input, inputLength);
    }

    virtual UPerfFunction* runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ){
        switch (index) {
            case 0: name = "UTF_8";     if (exec) return Command::get("UTF-8", INTERMEDIATE_CAPACITY); break;
            case 1: name = "UTF_8_SB";  if (exec) return Command::get("UTF-8",INTERMEDIATE_SMALL_CAPACITY); break;
            case 2: name = "SCSU";      if (exec) return Command::get("SCSU", INTERMEDIATE_CAPACITY); break;
            case 3: name = "SCSU_SB";   if (exec) return Command::get("SCSU", INTERMEDIATE_SMALL_CAPACITY); break;
            case 4: name = "BOCU_1";    if (exec) return Command::get("BOCU-1", INTERMEDIATE_CAPACITY); break;
            case 5: name = "BOCU_1_SB"; if (exec) return Command::get("BOCU-1",INTERMEDIATE_SMALL_CAPACITY); break;
            default: name = ""; break;
        }
        return NULL;
    }
};


int main(int argc, const char *argv[])
{
    UErrorCode status = U_ZERO_ERROR;
    UtfPerformanceTest test(argc, argv, status);

	if (U_FAILURE(status)){
        printf("The error is %s\n", u_errorName(status));
        return status;
    }
        
    if (test.run() == FALSE){
        fprintf(stderr, "FAILED: Tests could not be run please check the "
			            "arguments.\n");
        return -1;
    }
    return 0;
}
