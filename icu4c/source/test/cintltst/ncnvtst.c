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

#define MAX_LENGTH 50

static void printSeq(const unsigned char* a, int len);
static void printSeqErr(const unsigned char* a, int len);
static void printUSeq(const UChar* a, int len);
static void printUSeqErr(const UChar* a, int len);
UBool convertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets, UBool doFlush, UErrorCode expectedStatus);
UBool convertToU( const char *source, int sourceLen, const UChar *expect, int expectLen, 
               const char *codepage, int32_t *expectOffsets, UBool doFlush, UErrorCode expectedStatus);


static void TestSurrogateBehaviour();
static void TestErrorBehaviour();
static void TestToUnicodeErrorBehaviour();
static void TestGetNextErrorBehaviour();

void printSeq(const unsigned char* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len) log_verbose("%X", a[i++]);
    log_verbose("}\n");
}
static void printUSeq(const UChar* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len) log_verbose("%4X", a[i++]);
    log_verbose("}\n");
}

void printSeqErr(const unsigned char* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len)  fprintf(stderr, "%X", a[i++]);
    fprintf(stderr, "}\n");
}
static void printUSeqErr(const UChar* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len) fprintf(stderr, "%4X", a[i++]);
    fprintf(stderr,"}\n");
}

void addExtraTests(TestNode** root)
{
     addTest(root, &TestSurrogateBehaviour, "tsconv/ncnvtst/TestSurrogateBehaviour");
     addTest(root, &TestErrorBehaviour, "tsconv/ncnvtst/TestErrorBehaviour");
     addTest(root, &TestToUnicodeErrorBehaviour, "tsconv/ncnvtst/ToUnicodeErrorBehaviour");
     addTest(root, &TestGetNextErrorBehaviour, "tsconv/ncnvtst/TestGetNextErrorBehaviour");

}

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
                           (char)0x05, (char)0x05, (char)0x05,  (char)0x06 };
        const char expected[] = 
            {  (char)0xe4, (char)0xb8, (char)0x80, (char)0xdc, (char)0x81, (char)0x31,  
               (char)0xeb, (char)0xbf, (char)0x81, (char)0xed, (char)0xa0, (char)0x81,
               (char)0xed, (char)0xb0, (char)0x81, (char)0x32};
        const char expectedFlushFalse[] = 
            {  (char)0xe4, (char)0xb8, (char)0x80, (char)0xdc, (char)0x81, (char)0x31, (char)0xeb, (char)0xbf, 
               (char)0x81, (char)0xF0, (char)0xF0, (char)0x90, (char)0x81, (char)0x32};
        
        /*int32_t fromOffsets[] = { 0x0000, 0x0002, 0x0005, 0x0006, 0x0009, 0x000C, 0x000F }; */
        /*UTF-8*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", offsets, TRUE, U_ZERO_ERROR ))
            log_err("u-> UTF8 with offsets did not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedFlushFalse, sizeof(expectedFlushFalse), "UTF8", offsets, FALSE, U_ZERO_ERROR ))
            log_err("u-> UTF8 with offsets did not match.\n");
         /*UTF-8*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", 0, TRUE, U_ZERO_ERROR ))
            log_err("u-> UTF8 did not match.\n");
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedFlushFalse, sizeof(expectedFlushFalse), "UTF8", 0, FALSE, U_ZERO_ERROR ))
            log_err("u-> UTF8 did not match.\n");


        if(!convertToU(expected, sizeof(expected), 
            sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "UTF8", 0, TRUE, U_ZERO_ERROR ))
            log_err("UTF8 -> did not match.\n");
        if(!convertToU(expected, sizeof(expected), 
            sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "UTF8", 0, FALSE, U_ZERO_ERROR ))
            log_err("UTF8 -> did not match.\n");
        
    }

}
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
        UChar    sampleText2[] =   { 0x00a1, 0xd801, 0x00a4};
        const char expected[]=
        {  (char)0xa2, (char)0xae};
       
       
        const char expected2[] = 
             {  (char)0xa2, (char)0xae, (char)0xa1, (char)0xe0, (char)0xa2, (char)0xb4};

        int32_t offsets[]={(char)0x00, (char)0x01, (char)0x02};
       
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
    
    }
    
     
}
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
        printSeqErr((const unsigned char *)buffer, expectLen);
        printSeqErr((const unsigned char *)expect, expectLen);
        return FALSE;
    }

   if (expectOffsets != 0){
        log_verbose("comparing %d offsets..\n", targ-buffer);
        if(memcmp(offsetBuffer,expectOffsets,(targ-buffer) * sizeof(int32_t) )){
            log_err("did not get the expected offsets. for FROM Unicode to %s\n", codepage);
            puts("Got  : ");
			printSeqErr(buffer, targ-buffer);
			for(p=buffer;p<targ;p++)
				printf("%d, ", offsetBuffer[p-buffer]); 
			puts("\nExpected: ");
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
            puts("\nGot : ");
            for(p=buffer;p<targ;p++)
		      printf("%d, ", offsetBuffer[p-buffer]); 
		    puts("\nExpected: ");
		    for(i=0; i<(targ-buffer); i++)
		      printf("%d, ", expectOffsets[i]);
		    puts("\n");
		    for(i=0; i<(targ-buffer); i++)
		      printf("%X,", buffer[i]);
		    puts("\n");
		    for(i=0; i<(src-source); i++)
		      printf("%X,", (unsigned char)source[i]);
		}
    }
    if(!memcmp(buffer, expect, expectLen*2)){
        log_verbose("Matches!\n");
        return TRUE;
    }
    else {    
        log_err("String does not match. FROM Unicode to codePage%s\n", codepage);
        printUSeq(buffer, expectLen);
        printUSeq(expect, expectLen);
        return FALSE;
    }
   
}
