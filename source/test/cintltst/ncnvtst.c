/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CCONVTST.C
*
* Modification History:
*        Name                     Description            
*   Madhu Katragadda              7/7/2000        Converter Tests for extended code coverage
*********************************************************************************
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "unicode/uloc.h"
#include "unicode/ucnv.h"
#include "cintltst.h"
#include "unicode/utypes.h"
#include "unicode/ustring.h"

#define MAX_LENGTH 99
#ifdef TEST_BUFFER_SIZE
#undef TEST_BUFFER_SIZE
#endif
#define TEST_BUFFER_SIZE 5555555

#define UNICODE_LIMIT 0x10FFFF

static int32_t  gInBufferSize = 0;
static int32_t  gOutBufferSize = 0;
static char     gNuConvTestName[1024];

#define nct_min(x,y)  ((x<y) ? x : y)

static void printSeq(const unsigned char* a, int len);
static void printSeqErr(const unsigned char* a, int len);
static void printUSeq(const UChar* a, int len);
static void printUSeqErr(const UChar* a, int len);
static UBool convertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets, UBool doFlush, UErrorCode expectedStatus);
static UBool convertToU( const char *source, int sourceLen, const UChar *expect, int expectLen, 
               const char *codepage, int32_t *expectOffsets, UBool doFlush, UErrorCode expectedStatus);

static UBool testConvertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, UConverterFromUCallback callback, int32_t *expectOffsets);
static UBool testConvertToU( const char *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, UConverterToUCallback callback, int32_t *expectOffsets);

static void setNuConvTestName(const char *codepage, const char *direction)
{
  sprintf(gNuConvTestName, "[Testing %s %s Unicode, InputBufSiz=%d, OutputBufSiz=%d]",
      codepage,
      direction,
      gInBufferSize,
      gOutBufferSize);
}


static void TestSurrogateBehaviour();
static void TestErrorBehaviour();
static void TestToUnicodeErrorBehaviour();
static void TestGetNextErrorBehaviour();
static void TestRegression();
static void TestAvailableConverters();
static void TestFlushInternalBuffer();  /*for improved code coverage in ucnv_cnv.c*/

static void TestWithBufferSize(int32_t osize, int32_t isize);


void printSeq(const unsigned char* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len) log_verbose("0x%02X ", a[i++]);
    log_verbose("}\n");
}
static void printUSeq(const UChar* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len) log_verbose("%0x04X ", a[i++]);
    log_verbose("}\n");
}

void printSeqErr(const unsigned char* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len)  fprintf(stderr, "0x%02X ", a[i++]);
    fprintf(stderr, "}\n");
}
static void printUSeqErr(const UChar* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len) fprintf(stderr, "0x%04X ", a[i++]);
    fprintf(stderr,"}\n");
}

void addExtraTests(TestNode** root)
{
     addTest(root, &TestSurrogateBehaviour,         "tsconv/ncnvtst/TestSurrogateBehaviour");
     addTest(root, &TestErrorBehaviour,             "tsconv/ncnvtst/TestErrorBehaviour");
     addTest(root, &TestToUnicodeErrorBehaviour,    "tsconv/ncnvtst/ToUnicodeErrorBehaviour");
     addTest(root, &TestGetNextErrorBehaviour,      "tsconv/ncnvtst/TestGetNextErrorBehaviour");
     addTest(root, &TestRegression,                 "tsconv/ncnvtst/TestRegression");
     addTest(root, &TestAvailableConverters,        "tsconv/ncnvtst/TestAvailableConverters");
     addTest(root, &TestFlushInternalBuffer,        "tsconv/ncnvtst/TestFlushInternalBuffer");

}
/*test surrogate behaviour*/
void TestSurrogateBehaviour(){
    log_verbose("Testing for SBCS and LATIN_1\n");
    {
        UChar    sampleText[] =   { 0x0031, 0xd801, 0xdc01, 0x0032};
        const char expected[] = 
            {  (char)0x31, (char)0x1a, (char)0x32};
        /*SBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-920", 0 , TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-920 [UCNV_SBCS] not match.\n");
        
        /*LATIN_1*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "LATIN_1", 0, TRUE, U_ZERO_ERROR ))
            log_err("u-> LATIN_1 not match.\n");
       
    }
    log_verbose("Testing for DBCS and MBCS\n");
    {
        UChar    sampleText[] =   { 0x00a1, 0xd801, 0xdc01, 0x00a4};
        const char expected[] = 
            {  (char)0xa2, (char)0xae, (char)0xa1, (char)0xe0, (char)0xa2, (char)0xb4};
        int32_t offsets[]={(char)0x00, (char)0x00, (char)0x01, (char)0x01, (char)0x03 };
       
        /*DBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1362", 0 , TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1362", offsets , TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] not match.\n");
        /*MBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1363", 0 , TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_MBCS] not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1363", offsets , TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_MBCS] not match.\n");
    }
    /*UTF-8*/
     log_verbose("Testing for UTF8\n");
    {
        UChar    sampleText[] =   { 0x4e00, 0x0701, 0x0031, 0xbfc1, 0xd801, 0xdc01, 0x0032};
        int32_t offsets[]={(char)0x00, (char)0x00, (char)0x00, (char)0x01, (char)0x01, (char)0x02,
                           (char)0x03, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x04,
                           (char)0x04, (char)0x06 };
        const char expected[] = 
            {  (char)0xe4, (char)0xb8, (char)0x80,  (char)0xdc, (char)0x81,  (char)0x31, 
            (char)0xeb, (char)0xbf, (char)0x81,  (char)0xF0, (char)0x90, (char)0x90, (char)0x81,  (char)0x32};
        
         
        int32_t fromOffsets[] = { 0x0000, 0x0003, 0x0005, 0x0006, 0x0009, 0x0009, 0x000D }; 
        /*UTF-8*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", offsets, TRUE, U_ZERO_ERROR ))
            log_err("u-> UTF8 with offsets and flush true did not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", 0, TRUE, U_ZERO_ERROR ))
            log_err("u-> UTF8 with offsets and flush true did not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", offsets, FALSE, U_ZERO_ERROR ))
            log_err("u-> UTF8 with offsets and flush true did not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", 0, FALSE, U_ZERO_ERROR ))
            log_err("u-> UTF8 with offsets and flush true did not match.\n");
       
        if(!convertToU(expected, sizeof(expected), 
            sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "UTF8", 0, TRUE, U_ZERO_ERROR ))
            log_err("UTF8 -> did not match.\n");
        if(!convertToU(expected, sizeof(expected), 
            sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "UTF8", 0, FALSE, U_ZERO_ERROR ))
            log_err("UTF8 -> did not match.\n");
        if(!convertToU(expected, sizeof(expected), 
            sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "UTF8", fromOffsets, TRUE, U_ZERO_ERROR ))
            log_err("UTF8 -> did not match.\n");
        if(!convertToU(expected, sizeof(expected), 
            sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "UTF8", fromOffsets, FALSE, U_ZERO_ERROR ))
            log_err("UTF8 -> did not match.\n");
        
    }
    


}
/*test various error behaviours*/
void TestErrorBehaviour(){
     log_verbose("Testing for SBCS and LATIN_1\n");
    {
        UChar    sampleText[] =   { 0x0031, 0xd801};
        UChar    sampleText2[] =   { 0x0031, 0xd801, 0x0032};
        const char expectedFlushTrue[] = 
            {  (char)0x31, (char)0x1a,};
        const char expected[] = 
            {  (char)0x31, };
        const char expected2[] = 
            {  (char)0x31, (char)0x1a, (char)0x32};
           
#if 0        
          /*commented untill further modifications in the source*/
        /*SBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expectedFlushTrue, sizeof(expectedFlushTrue), "ibm-920", 0, TRUE, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> ibm-920 [UCNV_SBCS] \n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-920", 0, FALSE, U_ZERO_ERROR))
            log_err("u-> ibm-920 [UCNV_SBCS] \n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-920", 0, TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-920 [UCNV_SBCS] did not match\n");
        
        
        /*LATIN_1*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expectedFlushTrue, sizeof(expectedFlushTrue), "LATIN_1", 0, TRUE, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> LATIN_1 is supposed to fail\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "LATIN_1", 0, FALSE, U_ZERO_ERROR))
            log_err("u-> LATIN_1 is supposed to fail\n");
       
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "LATIN_1", 0, TRUE, U_ZERO_ERROR))
            log_err("u-> LATIN_1 did not match\n");
#endif       
      
    }
     log_verbose("Testing for DBCS and MBCS\n");
    {
        UChar    sampleText[] =   { 0x00a1, 0xd801};
        const char expected[]=
        {  (char)0xa2, (char)0xae};
        int32_t offsets[]={(char)0x00, (char)0x01, (char)0x02};
       
        UChar    sampleText2[] =   { 0x00a1, 0xd801, 0x00a4};
        const char expected2[] = 
             {  (char)0xa2, (char)0xae, (char)0xa1, (char)0xe0, (char)0xa2, (char)0xb4};

         UChar    sampleText3MBCS[] =   { 0x0001, 0x00a4, 0xdc01};
        const char expected3MBCS[] = 
        {   (char)0x01, (char)0xa2, (char)0xb4, (char)0xa1, (char)0xe0};
        int32_t offsets3MBCS[] = { (char)0x00, (char)0x01, (char)0x02 };

        UChar    sampleText4MBCS[] =   { 0x0061, 0x00a6, 0xdc01};
        const char expected4MBCS[] = 
        {   (char)0x61, (char)0x8f, (char)0xa2, (char)0xc3, (char)0xf4, (char)0xfe};
        int32_t offsets4MBCS[] = { (char)0x00, (char)0x01, (char)0x02 };

       

        
       
#if 0      
        /*commented untill further modifications in the source*/
        /*DBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1362", 0, TRUE, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> ibm-1362 [UCNV_DBCS] is supposed to fail\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1362", 0, FALSE, U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] is supposed to fail\n");
       
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1362", 0, TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] did not match \n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1362", offsets, TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] did not match \n");
#endif       
        
        /*MBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1363", 0, TRUE, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> ibm-1363 [UCNV_MBCS] \n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1363", 0, FALSE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_MBCS] \n");
      
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1363", 0, TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_DBCS] did not match\n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1363", 0, FALSE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_DBCS] did not match\n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1363", offsets, FALSE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_DBCS] did not match\n");

        if(!convertFromU(sampleText3MBCS, sizeof(sampleText3MBCS)/sizeof(sampleText3MBCS[0]),
                expected3MBCS, sizeof(expected3MBCS), "ibm-1363", offsets3MBCS, TRUE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_MBCS] \n");
        if(!convertFromU(sampleText3MBCS, sizeof(sampleText3MBCS)/sizeof(sampleText3MBCS[0]),
                expected3MBCS, sizeof(expected3MBCS), "ibm-1363", offsets3MBCS, FALSE, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_MBCS] \n");

        if(!convertFromU(sampleText4MBCS, sizeof(sampleText4MBCS)/sizeof(sampleText4MBCS[0]),
                expected4MBCS, sizeof(expected4MBCS), "euc-jp", offsets4MBCS, TRUE, U_ZERO_ERROR))
            log_err("u-> euc-jp [UCNV_MBCS] \n");
        if(!convertFromU(sampleText4MBCS, sizeof(sampleText4MBCS)/sizeof(sampleText4MBCS[0]),
                expected4MBCS, sizeof(expected4MBCS), "euc-jp", offsets4MBCS, FALSE, U_ZERO_ERROR))
            log_err("u-> euc-jp [UCNV_MBCS] \n");
    
    }
    
     
}
/*test different convertToUnicode error behaviours*/
void TestToUnicodeErrorBehaviour()
{
    log_verbose("Testing error conditions for DBCS\n");
    {
        char sampleText[] = { (char)0xa2, (char)0xae, (char)0x03, (char)0x04};
        const UChar expected[] = { 0x00a1 };
        
        char sampleText2[] = { (char)0xa2, (char)0xae, (char)0xa2};
        const UChar expected2[] = { 0x00a1 };

        if(!convertToU(sampleText, sizeof(sampleText), 
                expected, sizeof(expected)/sizeof(expected[0]), "ibm-1362", 0, TRUE, U_ZERO_ERROR ))
            log_err("DBCS (ibm-1362)->Unicode  did not match.\n");
        if(!convertToU(sampleText, sizeof(sampleText), 
                expected, sizeof(expected)/sizeof(expected[0]), "ibm-1362", 0, FALSE, U_ZERO_ERROR ))
            log_err("DBCS (ibm-1362)->Unicode  with flush = false did not match.\n");
       
        if(!convertToU(sampleText2, sizeof(sampleText2), 
                expected2, sizeof(expected2)/sizeof(expected2[0]), "ibm-1362", 0, TRUE, U_TRUNCATED_CHAR_FOUND ))
            log_err("DBCS (ibm-1362)->Unicode with TRUNCATED CHARACTER  did not match.\n");
       
    
    }
     log_verbose("Testing error conditions for SBCS\n");
    {
        char sampleText[] = { (char)0xa2, (char)0xFF};
        const UChar expected[] = { 0x00c2 };

        char sampleText2[] = { (char)0xa2, (char)0x70 };
        const UChar expected2[] = { 0x0073 };
        
        if(!convertToU(sampleText, sizeof(sampleText), 
                expected, sizeof(expected)/sizeof(expected[0]), "ibm-1051", 0, TRUE, U_ZERO_ERROR ))
            log_err("SBCS (ibm-1051)->Unicode  did not match.\n");
        if(!convertToU(sampleText, sizeof(sampleText), 
                expected, sizeof(expected)/sizeof(expected[0]), "ibm-1051", 0, FALSE, U_ZERO_ERROR ))
            log_err("SBCS (ibm-1051)->Unicode  with flush = false did not match.\n");

     }
     log_verbose("Testing error conditions for UTF8\n");
    {
        const char sampleText[] = { (char)0x31, (char)0xe4, (char)0xba, (char)0x8c, (char)0xe4, (char)0xb8, };
        UChar    expectedUTF8[] = {  0x0031, 0x4e8c};
        int32_t offsets[] = {   0x0000, 0x0001};

        const char sampleText2[] = { (char)0x31, (char)0xff, (char)0xe4, (char)0xba, (char)0x8c, 
            (char)0xe0, (char)0x80, (char)0x61,};
        UChar    expected2UTF8[] = {  0x0031, 0xfffd, 0x4e8c, 0xfffd, 0x0061};
        int32_t offsets2[] = {   0x0000, 0x0001, 0x0002, 0x0005, 0x0007};

        const char sampleText3[] = { (char)0x31, (char)0xfb, (char)0xbf, (char)0xbf, (char)0xbf, (char)0xbf, 
            (char)0x61,};
        UChar    expected3UTF8[] = {  0x0031, 0xfffd, 0x0061};
        int32_t offsets3[] = {   0x0000, 0x0001, 0x0006};

        if(!convertToU(sampleText, sizeof(sampleText), 
                expectedUTF8, sizeof(expectedUTF8)/sizeof(expectedUTF8[0]), "utf-8", 0, TRUE, U_TRUNCATED_CHAR_FOUND ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText, sizeof(sampleText), 
                expectedUTF8, sizeof(expectedUTF8)/sizeof(expectedUTF8[0]), "utf-8", 0, FALSE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText, sizeof(sampleText), 
                expectedUTF8, sizeof(expectedUTF8)/sizeof(expectedUTF8[0]), "utf-8", offsets, TRUE, U_TRUNCATED_CHAR_FOUND ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText, sizeof(sampleText), 
                expectedUTF8, sizeof(expectedUTF8)/sizeof(expectedUTF8[0]), "utf-8", offsets, FALSE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");

        if(!convertToU(sampleText2, sizeof(sampleText2), 
                expected2UTF8, sizeof(expected2UTF8)/sizeof(expected2UTF8[0]), "utf-8", 0, TRUE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText2, sizeof(sampleText2), 
                expected2UTF8, sizeof(expected2UTF8)/sizeof(expected2UTF8[0]), "utf-8", 0, FALSE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText2, sizeof(sampleText2), 
                expected2UTF8, sizeof(expected2UTF8)/sizeof(expected2UTF8[0]), "utf-8", offsets2, TRUE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText2, sizeof(sampleText2), 
                expected2UTF8, sizeof(expected2UTF8)/sizeof(expected2UTF8[0]), "utf-8", offsets2, FALSE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");

        if(!convertToU(sampleText3, sizeof(sampleText3), 
                expected3UTF8, sizeof(expected3UTF8)/sizeof(expected3UTF8[0]), "utf-8", offsets3, TRUE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match.\n");
        if(!convertToU(sampleText3, sizeof(sampleText3), 
                expected3UTF8, sizeof(expected3UTF8)/sizeof(expected3UTF8[0]), "utf-8", offsets3, FALSE, U_ZERO_ERROR ))
            log_err("utf-8->Unicode  did not match with flush false.\n");

    }
    
 
}
void TestGetNextErrorBehaviour(){
   /*Test for unassigned character*/
    static const char input1[]={ (char)0x70 };
    const char* source=(const char*)input1;
    UErrorCode err=U_ZERO_ERROR;
    UChar32 c=0;
    UConverter *cnv=ucnv_open("ibm-1159", &err);
    if(U_FAILURE(err)) {
        log_err("Unable to open a SBCS(ibm-1159) converter: %s\n", u_errorName(err));
        return;
    }
    c=ucnv_getNextUChar(cnv, &source, source+sizeof(source), &err);
    if(err != U_INVALID_CHAR_FOUND && c!=0xfffd){
        log_err("FAIL in TestGetNextErrorBehaviour(unassigned): Expected: U_INVALID_CHAR_ERROR or 0xfffd ----Got:%s and 0x%lx\n",  myErrorName(err), c);
    }
    ucnv_close(cnv);
    
    
            
}

/*Regression test for utf8 converter*/
void TestRegression(){
    char *buffer=0;
    UChar32 c;
    char *targ;
    char *targetLimit;
    UChar *source;
    const UChar *src=0;
    const UChar *sourceLimit=0;
    UChar *extractedTargetBuffer=0;
    UChar *extractedTarget=0;
    UChar *target=0;
    UChar *limit=0;
    int32_t count=0;
  
    int32_t offset=0;
    int32_t sourceLength=0, i=0;
    UErrorCode status=U_ZERO_ERROR;
    UConverter *conv=ucnv_open("utf8", &status);
    if(U_FAILURE(status)) {
        log_err("Unable to open a utf-8 converter: %s\n", u_errorName(status));
    }
    source=(UChar*)malloc(sizeof(UChar) * TEST_BUFFER_SIZE);
    extractedTargetBuffer=(UChar*)malloc(sizeof(UChar) * TEST_BUFFER_SIZE);
    buffer=(char*)malloc(sizeof(char) * TEST_BUFFER_SIZE);

    for(i =0; i< TEST_BUFFER_SIZE; i++){
        buffer[i]=(char)0xF0;
        source[i]=(UChar)0xFFFE;
        extractedTargetBuffer[i]=(UChar)0xFFFE;
    }
    for(c=0x0000; c <= UNICODE_LIMIT; c++){
        UTF16_APPEND_CHAR_SAFE(source, offset, TEST_BUFFER_SIZE, c);
        count++;
    }
    log_verbose("\nThe the No: of code units=%ld,  The no: of Code Points=%d\n", offset, count);
    
    src=source;
    sourceLimit=src+(offset);
    targ=buffer;
    targetLimit=targ+(TEST_BUFFER_SIZE);
    ucnv_fromUnicode (conv,
                  &targ,
                  targetLimit,
                  &src,
                  sourceLimit,
                  NULL,
                  TRUE, 
                  &status);
    if(U_FAILURE(status)){
        log_err("FAILED: error= %s at offset=0x%04X and target=0x%02X\n", myErrorName(status), (UNICODE_LIMIT-(sourceLimit-src)/sizeof(UChar)) , *targ);
    }

    log_verbose("The No: of bytes in target buffer =%ld\n", targ-buffer);
    
     targetLimit=targ;
     targ=buffer;

    extractedTarget=extractedTargetBuffer;
    limit=extractedTarget+TEST_BUFFER_SIZE;
    ucnv_toUnicode(conv, 
                   &extractedTarget, 
                   limit, 
                   (const char **) &targ, 
                   targetLimit, 
                   NULL, 
                   TRUE, 
                   &status);

    /*if(memcmp(source, extractedTarget, extractedTarget-extractedTargetBuffer) != 0){
        log_err("FAILED\n");
    }*/
    src=source;
    target=extractedTargetBuffer;
    while(target < extractedTarget){
        if(*src != *target){
            log_err("FAILED: comparision failed at source=0x%04X, extracted=0x%04X\n", *src, *target);
            break;
        }
        src++;
        target++;

    }
    if((extractedTarget-extractedTargetBuffer) != (sourceLimit-source)){
        log_err("The conversion didn't go through the whole range: Expected= %d, Got=%d\n", (sourceLimit-source), (extractedTarget-extractedTargetBuffer));

    }
    ucnv_close(conv);
    free(source);
    free(extractedTargetBuffer);
    free(buffer);

}
/*Walk through the available converters*/
void TestAvailableConverters(){
    UErrorCode status=U_ZERO_ERROR;
    UConverter *conv=NULL;
    int32_t i=0;
    for(i=0; i < ucnv_countAvailable(); i++){
        status=U_ZERO_ERROR;
        conv=ucnv_open(ucnv_getAvailableName(i), &status);
        if(U_FAILURE(status)){
            log_err("ERROR: converter creation failed. Failure in alias table or the data table for \n converter=%s. Error=%s\n", 
                        ucnv_getAvailableName(i), myErrorName(status));
            continue;
        }
        ucnv_close(conv);
    }

}

void TestFlushInternalBuffer(){
    TestWithBufferSize(MAX_LENGTH, 1);
    TestWithBufferSize(1, 1);
    TestWithBufferSize(1, MAX_LENGTH);
    TestWithBufferSize(MAX_LENGTH, MAX_LENGTH);
}

void TestWithBufferSize(int32_t insize, int32_t outsize){

    gInBufferSize =insize;
    gOutBufferSize = outsize;

     log_verbose("Testing fromUnicode for UTF-8 with UCNV_TO_U_CALLBACK_SUBSTITUTE \n");
    {
        UChar    sampleText[] = 
            { 0x0031, 0x0032, 0x0033, 0x0000, 0x4e00, 0x4e8c, 0x4e09,  0x002E  };
        const char expectedUTF8[] = 
            { (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0xe4, (char)0xb8, (char)0x80, (char)0xe4, (char)0xba, (char)0x8c, (char)0xe4, (char)0xb8, (char)0x89, (char)0x2E };
        int32_t  toUTF8Offs[] = 
            { (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x06, (char)0x07};
        int32_t fmUTF8Offs[] = 
            { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0007, 0x000a, 0x000d };
        
        /*UTF-8*/
        if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF8, sizeof(expectedUTF8), "UTF8", UCNV_FROM_U_CALLBACK_SUBSTITUTE, toUTF8Offs ))
             log_err("u-> UTF8 did not match.\n");
    }
     
     log_verbose("Testing fromUnicode with UCNV_FROM_U_CALLBACK_ESCAPE  \n");
    {
        UChar inputTest[] = { 0x0061, 0xd801, 0xdc01, 0xd801, 0x0061 };
        const char toIBM943[]= { (char)0x61, 
            (char)0x25, (char)0x55, (char)0x44, (char)0x38, (char)0x30, (char)0x31,
            (char)0x25, (char)0x55, (char)0x44, (char)0x43, (char)0x30, (char)0x31,
            (char)0x25, (char)0x55, (char)0x44, (char)0x38, (char)0x30, (char)0x31,
            (char)0x61 };
        int32_t offset[]= {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 4};

        if(!testConvertFromU(inputTest, sizeof(inputTest)/sizeof(inputTest[0]),
			    toIBM943, sizeof(toIBM943), "ibm-943",
	            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, offset))
		    log_err("u-> ibm-943 with subst with value did not match.\n");
    }

     log_verbose("Testing fromUnicode for UTF-8 with UCNV_TO_U_CALLBACK_SUBSTITUTE \n");
    {
        const char sampleText1[] = { (char)0x31, (char)0xe4, (char)0xba, (char)0x8c, 
            (char)0xe0, (char)0x80,  (char)0x61,};
        UChar    expected1[] = {  0x0031, 0x4e8c, 0xfffd, 0x0061};
        int32_t offsets1[] = {   0x0000, 0x0001, 0x0004, 0x0006};

        if(!testConvertToU(sampleText1, sizeof(sampleText1),
			     expected1, sizeof(expected1)/sizeof(expected1[0]),"utf8", UCNV_TO_U_CALLBACK_SUBSTITUTE, offsets1))
		    log_err("utf8->u with substitute did not match.\n");;
    }

    
    log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_ESCAPE \n");
    /*to Unicode*/
    {
        const char sampleTxtToU[]= { (char)0x00, (char)0x9f, (char)0xaf, 
            (char)0x81, (char)0xad, /*unassigned*/
            (char)0x89, (char)0xd3 };
	    UChar IBM_943toUnicode[] = { 0x0000, 0x6D63, 
            0x25, 0x58, 0x38, 0x31, 0x25, 0x58, 0x41, 0x44,
            0x7B87};
        int32_t  fromIBM943Offs [] = 	{ 0, 1, 3, 3, 3, 3, 3, 3, 3, 3, 5};

        if(!testConvertToU(sampleTxtToU, sizeof(sampleTxtToU),
		         IBM_943toUnicode, sizeof(IBM_943toUnicode)/sizeof(IBM_943toUnicode[0]),"ibm-943",
	            (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromIBM943Offs))
		    log_err("ibm-943->u with substitute with value did not match.\n");

    }
    

}

UBool convertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets, UBool doFlush, UErrorCode expectedStatus)
{
   
    int32_t i=0;
    char *p=0;
    const UChar *src;
    char buffer[MAX_LENGTH];
    int32_t offsetBuffer[MAX_LENGTH];
    int32_t *offs=0;
    char *targ;
    char *targetLimit;
    UChar *sourceLimit=0;
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    conv = ucnv_open(codepage, &status);
    if(U_FAILURE(status))
    {
        log_err("Couldn't open converter %s\n",codepage);    
        return FALSE;
    }
    log_verbose("Converter %s opened..\n", ucnv_getName(conv, &status));
    
    for(i=0; i<MAX_LENGTH; i++){
        buffer[i]=(char)0xF0;
        offsetBuffer[i]=0xFF;
    }

    src=source;
    sourceLimit=(UChar*)src+(sourceLen);
    targ=buffer;
    targetLimit=targ+MAX_LENGTH;
    offs=offsetBuffer;
    ucnv_fromUnicode (conv,
                  &targ,
                  targetLimit,
                  &src,
                  sourceLimit,
                  expectOffsets ? offs : NULL,
                  doFlush, 
                  &status);
    ucnv_close(conv);
    if(status != expectedStatus){
          log_err("ucnv_fromUnicode() failed for codepage=%s. Error =%s Expected=%s\n", codepage, myErrorName(status), myErrorName(expectedStatus));
          return FALSE;
    } 
    
    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-buffer);
  
    if(expectLen != targ-buffer)
    {
        log_err("Expected %d chars out, got %d FROM Unicode to %s\n", expectLen, targ-buffer, codepage);
        log_verbose("Expected %d chars out, got %d FROM Unicode to %s\n", expectLen, targ-buffer, codepage);
		printSeqErr((const unsigned char *)buffer, targ-buffer);
        printSeqErr(expect, expectLen);
        return FALSE;
    }
    
    if(!memcmp(buffer, expect, expectLen)){
        log_verbose("Matches!\n");
        return TRUE;
    }
    else {    
        log_err("String does not match. FROM Unicode to codePage%s\n", codepage);
        printf("\nGot:");
        printSeqErr((const unsigned char *)buffer, expectLen);
        printf("\nExpected:");
        printSeqErr((const unsigned char *)expect, expectLen);
        return FALSE;
    }

   if (expectOffsets != 0){
        log_verbose("comparing %d offsets..\n", targ-buffer);
        if(memcmp(offsetBuffer,expectOffsets,(targ-buffer) * sizeof(int32_t) )){
            log_err("did not get the expected offsets. for FROM Unicode to %s\n", codepage);
            printf("\nGot  : ");
			printSeqErr(buffer, targ-buffer);
			for(p=buffer;p<targ;p++)
				printf("%d, ", offsetBuffer[p-buffer]); 
			printf("\nExpected: ");
			for(i=0; i< (targ-buffer); i++)
				printf("%d,", expectOffsets[i]);
        }
    }
    
}


UBool convertToU( const char *source, int sourceLen, const UChar *expect, int expectLen, 
               const char *codepage, int32_t *expectOffsets, UBool doFlush, UErrorCode expectedStatus)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    int32_t i=0;
    UChar *p=0;
    const char* src;
    UChar buffer[MAX_LENGTH];
    int32_t offsetBuffer[MAX_LENGTH];
    int32_t *offs=0;
    UChar *targ;
    UChar *targetLimit;
    char *sourceLimit=0;
    
       

    conv = ucnv_open(codepage, &status);
    if(U_FAILURE(status))
    {
        log_err("Couldn't open converter %s\n",codepage);    
        return FALSE;
    }
    log_verbose("Converter %s opened..\n", ucnv_getName(conv, &status));
    


    for(i=0; i<MAX_LENGTH; i++){
        buffer[i]=0xFFFE;
        offsetBuffer[i]=-1;
    }
   
    src=source;
    sourceLimit=(char*)src+(sourceLen);
    targ=buffer;
    targetLimit=targ+MAX_LENGTH;
    offs=offsetBuffer;
    
   
   
    ucnv_toUnicode (conv,
                &targ,
                targetLimit,
                &src,
                sourceLimit,
                expectOffsets ? offs : NULL,
                doFlush,
                &status);
   
    ucnv_close(conv);
    if(status != expectedStatus){
          log_err("ucnv_fromUnicode() failed for codepage=%s. Error =%s Expected=%s\n", codepage, myErrorName(status), myErrorName(expectedStatus));
          return FALSE;
    } 
    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-buffer);
          
   
  

    log_verbose("comparing %d uchars (%d bytes)..\n",expectLen,expectLen*2);

    if (expectOffsets != 0)
    {
        if(memcmp(offsetBuffer, expectOffsets, (targ-buffer) * sizeof(int32_t))){
            
            log_err("did not get the expected offsets from %s To UNICODE\n", codepage);
            printf("\nGot : ");
            for(p=buffer;p<targ;p++)
		      printf("%d, ", offsetBuffer[p-buffer]); 
		    printf("\nExpected: ");
		    for(i=0; i<(targ-buffer); i++)
		      printf("%d, ", expectOffsets[i]);
		    printf("\nGot result:");
		    for(i=0; i<(targ-buffer); i++)
		      printf("0x%04X,", buffer[i]);
		    printf("\nFrom Input:");
		    for(i=0; i<(src-source); i++)
		      printf("0x%02X,", (unsigned char)source[i]);
            puts("\n");
		}
    }
    if(!memcmp(buffer, expect, expectLen*2)){
        log_verbose("Matches!\n");
        return TRUE;
    }
    else {    
        log_err("String does not match. from codePage %s TO Unicode\n", codepage);
        printf("\nGot:");
        printUSeqErr(buffer, expectLen);
        printf("\nExpected:");
        printUSeqErr(expect, expectLen);
        return FALSE;
    }
   
}


UBool testConvertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, UConverterFromUCallback callback , int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    char    junkout[MAX_LENGTH]; /* FIX */
    int32_t    junokout[MAX_LENGTH]; /* FIX */
    char *p;
    const UChar *src;
    char *end;
    char *targ;
    int32_t *offs;
    int i;
    int32_t   realBufferSize;
    char *realBufferEnd;
    const UChar *realSourceEnd;
    const UChar *sourceLimit;
    UBool checkOffsets = TRUE;
    UBool doFlush;

    UConverterFromUCallback oldAction = NULL;
    void* oldContext = NULL;

    for(i=0;i<MAX_LENGTH;i++)
        junkout[i] = (char)0xF0;
    for(i=0;i<MAX_LENGTH;i++)
        junokout[i] = 0xFF;

    setNuConvTestName(codepage, "FROM");

    log_verbose("\n=========  %s\n", gNuConvTestName);

    conv = ucnv_open(codepage, &status);
    if(U_FAILURE(status))
    {
        log_err("Couldn't open converter %s\n",codepage);    
        return FALSE;
    }

    log_verbose("Converter opened..\n");
    /*----setting the callback routine----*/
	ucnv_setFromUCallBack (conv, callback, NULL, &oldAction, &oldContext, &status);
	if (U_FAILURE(status)) { 
		log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));  
	}
    /*------------------------*/

    src = source;
    targ = junkout;
    offs = junokout;

    realBufferSize = (sizeof(junkout)/sizeof(junkout[0]));
    realBufferEnd = junkout + realBufferSize;
    realSourceEnd = source + sourceLen;

    if ( gOutBufferSize != realBufferSize )
      checkOffsets = FALSE;
 
    if( gInBufferSize != MAX_LENGTH )
      checkOffsets = FALSE;

    do
      {
        end = nct_min(targ + gOutBufferSize, realBufferEnd);
        sourceLimit = nct_min(src + gInBufferSize, realSourceEnd);

        doFlush = (sourceLimit == realSourceEnd);

        if(targ == realBufferEnd)
          {
        log_err("Error, overflowed the real buffer while about to call fromUnicode! targ=%08lx %s", targ, gNuConvTestName);
        return FALSE;
          }
        log_verbose("calling fromUnicode @ SOURCE:%08lx to %08lx  TARGET: %08lx to %08lx, flush=%s\n", src,sourceLimit, targ,end, doFlush?"TRUE":"FALSE");
        

        status = U_ZERO_ERROR;
 
        ucnv_fromUnicode (conv,
                  &targ,
                  end,
                  &src,
                  sourceLimit,
                  offs,
                  doFlush, /* flush if we're at the end of the input data */
                  &status);
      } while ( (status == U_INDEX_OUTOFBOUNDS_ERROR) || (U_SUCCESS(status) && sourceLimit < realSourceEnd) );
        
    if(U_FAILURE(status)) {
        log_err("Problem doing fromUnicode to %s, errcode %s %s\n", codepage, myErrorName(status), gNuConvTestName);
        return FALSE;
      }

    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-junkout);
    if(VERBOSITY)
    {
        char junk[999];
        char offset_str[999];
        char *p;
        
        junk[0] = 0;
        offset_str[0] = 0;
        for(p = junkout;p<targ;p++)
        {
            sprintf(junk + strlen(junk), "0x%02x, ", (0xFF) & (unsigned int)*p);
            sprintf(offset_str + strlen(offset_str), "0x%02x, ", (0xFF) & (unsigned int)junokout[p-junkout]);
        }
        
        log_verbose(junk);
        printSeq((const unsigned char *)expect, expectLen);
        if ( checkOffsets )
          {
            log_verbose("\nOffsets:");
            log_verbose(offset_str);
          }
        log_verbose("\n");
    }
    ucnv_close(conv);


    if(expectLen != targ-junkout)
    {
        log_err("Expected %d chars out, got %d %s\n", expectLen, targ-junkout, gNuConvTestName);
        log_verbose("Expected %d chars out, got %d %s\n", expectLen, targ-junkout, gNuConvTestName);
        printf("\nGot:");
		printSeqErr(junkout, targ-junkout);
        printf("\nExpected:");
		printSeqErr(expect, expectLen);
        return FALSE;
    }

    if (checkOffsets && (expectOffsets != 0) )
    {
        log_verbose("comparing %d offsets..\n", targ-junkout);
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t) )){
            log_err("did not get the expected offsets. %s", gNuConvTestName);
            log_err("Got  : ");
			printSeqErr(junkout, targ-junkout);
			for(p=junkout;p<targ;p++)
				log_err("%d, ", junokout[p-junkout]); 
			log_err("\nExpected: ");
			for(i=0; i<(targ-junkout); i++)
				log_err("%d,", expectOffsets[i]);
        }
    }

    log_verbose("comparing..\n");
    if(!memcmp(junkout, expect, expectLen))
    {
        log_verbose("Matches!\n");
        return TRUE;
    }
    else
    {    
        log_err("String does not match. %s\n", gNuConvTestName);
        printUSeqErr(source, sourceLen);
        printf("\nGot:");
        printSeqErr((const unsigned char *)junkout, expectLen);
        printf("\nExpected:");
        printSeqErr((const unsigned char *)expect, expectLen);
        
        return FALSE;
    }
}

UBool testConvertToU( const char *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, UConverterToUCallback callback, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    UChar    junkout[MAX_LENGTH]; /* FIX */
    int32_t    junokout[MAX_LENGTH]; /* FIX */
    const char *src;
    const char *realSourceEnd;
    const char *srcLimit;
    UChar *p;
    UChar *targ;
    UChar *end;
    int32_t *offs;
    int i;
    UBool   checkOffsets = TRUE;
    int32_t   realBufferSize;
    UChar *realBufferEnd;

    UConverterToUCallback oldAction = NULL;
    void* oldContext = NULL;
    

    for(i=0;i<MAX_LENGTH;i++)
        junkout[i] = 0xFFFE;

    for(i=0;i<MAX_LENGTH;i++)
        junokout[i] = -1;

    setNuConvTestName(codepage, "TO");

    log_verbose("\n=========  %s\n", gNuConvTestName);

    conv = ucnv_open(codepage, &status);
    if(U_FAILURE(status))
    {
        log_err("Couldn't open converter %s\n",gNuConvTestName);
        return FALSE;
    }

    log_verbose("Converter opened..\n");
     /*----setting the callback routine----*/
	ucnv_setToUCallBack (conv, callback, NULL, &oldAction, &oldContext, &status);
	if (U_FAILURE(status)) { 
		log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));  
	}
	/*-------------------------------------*/

    src = source;
    targ = junkout;
    offs = junokout;
    
    realBufferSize = (sizeof(junkout)/sizeof(junkout[0]));
    realBufferEnd = junkout + realBufferSize;
    realSourceEnd = src + sourcelen;

    if ( gOutBufferSize != realBufferSize )
      checkOffsets = FALSE;

    if( gInBufferSize != MAX_LENGTH )
      checkOffsets = FALSE;

    do
      {
        end = nct_min( targ + gOutBufferSize, realBufferEnd);
        srcLimit = nct_min(realSourceEnd, src + gInBufferSize);

        if(targ == realBufferEnd)
          {
        log_err("Error, the end would overflow the real output buffer while about to call toUnicode! tarjey=%08lx %s",targ,gNuConvTestName);
        return FALSE;
          }
        log_verbose("calling toUnicode @ %08lx to %08lx\n", targ,end);

        /* oldTarg = targ; */

        status = U_ZERO_ERROR;

        ucnv_toUnicode (conv,
                &targ,
                end,
                &src,
                srcLimit,
                offs,
                (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
                &status);

        /*        offs += (targ-oldTarg); */

      } while ( (status == U_INDEX_OUTOFBOUNDS_ERROR) || (U_SUCCESS(status) && (srcLimit < realSourceEnd)) ); /* while we just need another buffer */

    if(U_FAILURE(status))
    {
        log_err("Problem doing %s toUnicode, errcode %s %s\n", codepage, myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done. %d bytes -> %d chars.\nResult :",
        sourcelen, targ-junkout);
    if(VERBOSITY)
    {
        char junk[999];
        char offset_str[999];
    
        UChar *p;
        
        junk[0] = 0;
        offset_str[0] = 0;

        for(p = junkout;p<targ;p++)
        {
            sprintf(junk + strlen(junk), "0x%04x, ", (0xFFFF) & (unsigned int)*p);
            sprintf(offset_str + strlen(offset_str), "0x%04x, ", (0xFFFF) & (unsigned int)junokout[p-junkout]);
        }
        
        log_verbose(junk);

        if ( checkOffsets )
          {
            log_verbose("\nOffsets:");
            log_verbose(offset_str);
          }
        log_verbose("\n");
    }
    ucnv_close(conv);

    log_verbose("comparing %d uchars (%d bytes)..\n",expectlen,expectlen*2);

    if (checkOffsets && (expectOffsets != 0))
    {
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t))){
            
            log_err("did not get the expected offsets. %s",gNuConvTestName);
            for(p=junkout;p<targ;p++)
		      log_err("%d, ", junokout[p-junkout]); 
		    log_err("\nExpected: ");
		    for(i=0; i<(targ-junkout); i++)
		      log_err("%d,", expectOffsets[i]);
		    log_err("");
		    for(i=0; i<(targ-junkout); i++)
		      log_err("%X,", junkout[i]);
		    log_err("");
		    for(i=0; i<(src-source); i++)
		      log_err("%X,", (unsigned char)source[i]);
		}
    }

    if(!memcmp(junkout, expect, expectlen*2))
    {
        log_verbose("Matches!\n");
        return TRUE;
    }
    else
    {    
        log_err("String does not match. %s\n", gNuConvTestName);
        log_verbose("String does not match. %s\n", gNuConvTestName);
        printf("\nGot:");
		printUSeq(junkout, expectlen);
        printf("\nExpected:");
        printUSeq(expect, expectlen); 
        return FALSE;
    }
}
