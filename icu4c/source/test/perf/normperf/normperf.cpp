/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/
/** 
 * This Program tests the performance of ICU's Normalization engine against Windows
 * to run it use the command like
 *
 * c:\normperf.exe -s C:\work\ICUCupertinoRep\icu4c\collation-perf-data  -i 10 -p 15 -f TestNames_Asian.txt -u -e UTF-8  -l
 */
#include "normperf.h"

UPerfFunction* NormalizerPerformanceTest::runIndexedTest(int32_t index, UBool exec,const char* &name, char* par) {
    switch (index) {
        TESTCASE(0,TestICU_NFC_NFD_Text);
        TESTCASE(1,TestICU_NFC_NFC_Text);
        TESTCASE(2,TestICU_NFC_Orig_Text);

        TESTCASE(3,TestICU_NFD_NFD_Text);
        TESTCASE(4,TestICU_NFD_NFC_Text);
        TESTCASE(5,TestICU_NFD_Orig_Text);

        TESTCASE(6,TestICU_FCD_NFD_Text);
        TESTCASE(7,TestICU_FCD_NFC_Text);
        TESTCASE(8,TestICU_FCD_Orig_Text);

        TESTCASE(9,TestWin_NFC_NFD_Text);
        TESTCASE(10,TestWin_NFC_NFC_Text);
        TESTCASE(11,TestWin_NFC_Orig_Text);

        TESTCASE(12,TestWin_NFD_NFD_Text);
        TESTCASE(13,TestWin_NFD_NFC_Text);
        TESTCASE(14,TestWin_NFD_Orig_Text);

        TESTCASE(15,TestQC_NFC_NFD_Text);
        TESTCASE(16,TestQC_NFC_NFC_Text);
        TESTCASE(17,TestQC_NFC_Orig_Text);

        TESTCASE(18,TestQC_NFD_NFD_Text);
        TESTCASE(19,TestQC_NFD_NFC_Text);
        TESTCASE(20,TestQC_NFD_Orig_Text);

        TESTCASE(21,TestQC_FCD_NFD_Text);
        TESTCASE(22,TestQC_FCD_NFC_Text);
        TESTCASE(23,TestQC_FCD_Orig_Text);

        TESTCASE(24,TestIsNormalized_NFC_NFD_Text);
        TESTCASE(25,TestIsNormalized_NFC_NFC_Text);
        TESTCASE(26,TestIsNormalized_NFC_Orig_Text);

        TESTCASE(27,TestIsNormalized_NFD_NFD_Text);
        TESTCASE(28,TestIsNormalized_NFD_NFC_Text);
        TESTCASE(29,TestIsNormalized_NFD_Orig_Text);

        TESTCASE(30,TestIsNormalized_FCD_NFD_Text);
        TESTCASE(31,TestIsNormalized_FCD_NFC_Text);
        TESTCASE(32,TestIsNormalized_FCD_Orig_Text);

        default: 
            name = ""; 
            return NULL;
    }
    return NULL;

}

void NormalizerPerformanceTest::normalizeInput(ULine* dest,const UChar* src ,int32_t srcLen,UNormalizationMode mode){
    int32_t reqLen = 0;
    UErrorCode status = U_ZERO_ERROR;
    for(;;){
        /* pure pre-flight */
        reqLen=unorm_normalize(src,srcLen,mode,0,NULL,0,&status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            dest->name = new UChar[reqLen+1];
            reqLen= unorm_normalize(src,srcLen,mode,0,dest->name,reqLen+1,&status);
            dest->len=reqLen;
            break;
        }else if(U_FAILURE(status)){
            printf("Could not normalize input. Error: %s", u_errorName(status));
        }
    }
}
UChar* NormalizerPerformanceTest::normalizeInput(int32_t& len, const UChar* src ,int32_t srcLen,UNormalizationMode mode){
    int32_t reqLen = 0;
    UErrorCode status = U_ZERO_ERROR;
    UChar* dest = NULL;
    for(;;){
        /* pure pre-flight */
        reqLen=unorm_normalize(src,srcLen,mode,0,NULL,0,&status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            dest = new UChar[reqLen+1];
            reqLen= unorm_normalize(src,srcLen,mode,0,dest,reqLen+1,&status);
            len=reqLen;
            break;
        }else if(U_FAILURE(status)){
            printf("Could not normalize input. Error: %s", u_errorName(status));
        }
    }
    return dest;
}

NormalizerPerformanceTest::NormalizerPerformanceTest(int32_t argc, const char* argv[], UErrorCode& status)
: UPerfTest(argc,argv,status){
    NFDBuffer = NULL;
    NFCBuffer = NULL;
    NFDBufferLen = 0;
    NFCBufferLen = 0;
    NFDFileLines = NULL;
    NFCFileLines = NULL;

    if(status== U_ILLEGAL_ARGUMENT_ERROR){
       fprintf(stderr,gUsageString, "normperf");
       return;
    }

    if(U_FAILURE(status)){
        fprintf(stderr, "FAILED to create UPerfTest object. Error: %s\n", u_errorName(status));
        return;
    }

    if(line_mode){
        ULine* filelines = getLines(status);
        if(U_FAILURE(status)){
            fprintf(stderr, "FAILED to read lines from file and create UPerfTest object. Error: %s\n", u_errorName(status));
            return;
        }
        NFDFileLines = new ULine[numLines];
        NFCFileLines = new ULine[numLines];
    
        for(int32_t i=0;i<numLines;i++){
            normalizeInput(&NFDFileLines[i],filelines[i].name,filelines[i].len,UNORM_NFD);
            normalizeInput(&NFCFileLines[i],filelines[i].name,filelines[i].len,UNORM_NFC);

        }
    }else if(bulk_mode){
        int32_t srcLen = 0;
        const UChar* src = getBuffer(srcLen,status);
        NFDBufferLen = 0;
        NFCBufferLen = 0;

        if(U_FAILURE(status)){
            fprintf(stderr, "FAILED to read buffer from file and create UPerfTest object. Error: %s\n", u_errorName(status));
            return;
        }
         
        NFCBuffer = normalizeInput(NFDBufferLen,src,srcLen,UNORM_NFD);
        NFDBuffer = normalizeInput(NFCBufferLen,src,srcLen,UNORM_NFC);
    }
    
}

NormalizerPerformanceTest::~NormalizerPerformanceTest(){
    delete[] NFDFileLines;
    delete[] NFCFileLines;
    delete[] NFDBuffer;
    delete[] NFCBuffer;
}

// Test NFC Performance
UPerfFunction* NormalizerPerformanceTest::TestICU_NFC_NFD_Text(){
    if(line_mode){
        NormPerfFunction* func= new NormPerfFunction(ICUNormNFC,NFDFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func= new NormPerfFunction(ICUNormNFC,NFDBuffer, NFDBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestICU_NFC_NFC_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFC,NFCFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func= new NormPerfFunction(ICUNormNFC,NFCBuffer, NFCBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestICU_NFC_Orig_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFC,lines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFC,buffer, bufferLen, uselen);
        return func;
    }
}

// Test NFD Performance
UPerfFunction* NormalizerPerformanceTest::TestICU_NFD_NFD_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFD,NFDFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFD,NFDBuffer,NFDBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestICU_NFD_NFC_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFD,NFCFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFD,NFCBuffer,NFCBufferLen, uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestICU_NFD_Orig_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFD,lines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormNFD,buffer,bufferLen, uselen);
        return func;
    }
}

// Test FCD Performance
UPerfFunction* NormalizerPerformanceTest::TestICU_FCD_NFD_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormFCD,NFDFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormFCD,NFDBuffer,NFDBufferLen, uselen);
        return func;        
    }

}
UPerfFunction* NormalizerPerformanceTest::TestICU_FCD_NFC_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormFCD,NFCFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormFCD,NFCBuffer,NFCBufferLen, uselen);
        return func;   
    }
}
UPerfFunction* NormalizerPerformanceTest::TestICU_FCD_Orig_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(ICUNormFCD,lines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(ICUNormFCD,buffer,bufferLen, uselen);
        return func;   
    }
}

// Test Win NFC Performance
UPerfFunction* NormalizerPerformanceTest::TestWin_NFC_NFD_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(WinNormNFC,NFDFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(WinNormNFC,NFDBuffer,NFDBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestWin_NFC_NFC_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(WinNormNFC,NFCFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(WinNormNFC,NFCBuffer,NFCBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestWin_NFC_Orig_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(WinNormNFC,lines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(WinNormNFC,buffer,bufferLen, uselen);
        return func;
    }
}

// Test Win NFD Performance
UPerfFunction* NormalizerPerformanceTest::TestWin_NFD_NFD_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(WinNormNFD,NFDFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(WinNormNFD,NFDBuffer,NFDBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestWin_NFD_NFC_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(WinNormNFD,NFCFileLines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(WinNormNFD,NFCBuffer,NFCBufferLen, uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestWin_NFD_Orig_Text(){
    if(line_mode){
        NormPerfFunction* func = new NormPerfFunction(WinNormNFD,lines,numLines, uselen);
        return func;
    }else{
        NormPerfFunction* func = new NormPerfFunction(WinNormNFD,buffer,bufferLen, uselen);
        return func;  
    }
}

// Test Quick Check Performance
UPerfFunction* NormalizerPerformanceTest::TestQC_NFC_NFD_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFDFileLines, numLines, UNORM_NFC,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFDBuffer, NFDBufferLen, UNORM_NFC,uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestQC_NFC_NFC_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFCFileLines, numLines, UNORM_NFC,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFCBuffer, NFCBufferLen, UNORM_NFC,uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestQC_NFC_Orig_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,lines, numLines, UNORM_NFC,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,buffer, bufferLen, UNORM_NFC,uselen);
        return func;
    }
}

UPerfFunction* NormalizerPerformanceTest::TestQC_NFD_NFD_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFDFileLines, numLines, UNORM_NFD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFDBuffer, NFDBufferLen, UNORM_NFD,uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestQC_NFD_NFC_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFCFileLines, numLines, UNORM_NFD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFCBuffer, NFCBufferLen, UNORM_NFD,uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestQC_NFD_Orig_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,lines, numLines, UNORM_NFD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,buffer, bufferLen, UNORM_NFD,uselen);
        return func;
    }
}

UPerfFunction* NormalizerPerformanceTest::TestQC_FCD_NFD_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFDFileLines, numLines, UNORM_FCD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFDBuffer, NFDBufferLen, UNORM_FCD,uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestQC_FCD_NFC_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFCFileLines, numLines, UNORM_FCD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,NFCBuffer, NFCBufferLen, UNORM_FCD,uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestQC_FCD_Orig_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,lines, numLines, UNORM_FCD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUQuickCheck,buffer, bufferLen, UNORM_FCD,uselen);
        return func;
    }
}

// Test isNormalized Performance
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_NFC_NFD_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFDFileLines, numLines, UNORM_NFC,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFDBuffer, NFDBufferLen, UNORM_NFC,uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_NFC_NFC_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFCFileLines, numLines, UNORM_NFC,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFCBuffer, NFCBufferLen, UNORM_NFC,uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_NFC_Orig_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,lines, numLines, UNORM_NFC,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,buffer, bufferLen, UNORM_NFC,uselen);
        return func;
    }
}

UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_NFD_NFD_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFDFileLines, numLines, UNORM_NFD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFDBuffer, NFDBufferLen, UNORM_NFD,uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_NFD_NFC_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFCFileLines, numLines, UNORM_NFD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFCBuffer, NFCBufferLen, UNORM_NFD,uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_NFD_Orig_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,lines, numLines, UNORM_NFD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,buffer, bufferLen, UNORM_NFD,uselen);
        return func;
    }
}

UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_FCD_NFD_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFDFileLines, numLines, UNORM_FCD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFDBuffer, NFDBufferLen, UNORM_FCD,uselen);
        return func;
    }
}
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_FCD_NFC_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFCFileLines, numLines, UNORM_FCD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,NFCBuffer, NFCBufferLen, UNORM_FCD,uselen);
        return func; 
    }
}
UPerfFunction* NormalizerPerformanceTest::TestIsNormalized_FCD_Orig_Text(){
    if(line_mode){
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,lines, numLines, UNORM_FCD,uselen);
        return func;
    }else{
        QuickCheckPerfFunction* func = new QuickCheckPerfFunction(ICUIsNormalized,buffer, bufferLen, UNORM_FCD,uselen);
        return func;
    }
}

int main(int argc, const char* argv[]){
    UErrorCode status = U_ZERO_ERROR;
    NormalizerPerformanceTest test(argc, argv, status);
    if(U_FAILURE(status)){
        return status;
    }
    if(test.run()==FALSE){
        fprintf(stderr,"FAILED: Tests could not be run please check the arguments.\n");
        return -1;
    }
    return 0;
}
