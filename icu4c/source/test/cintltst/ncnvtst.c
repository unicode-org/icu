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
*    Madhu Katragadda              7/7/2000        Converter Tests for extended code coverage
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
UBool convertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets, UErrorCode expectedStatus);

static void TestSurrogateBehaviour();
static void TestErrorBehaviour();

void printSeq(const unsigned char* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len) log_verbose("%X", a[i++]);
    log_verbose("}\n");
}
/*static void printUSeq(const UChar* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len) log_verbose("%4X", a[i++]);
    log_verbose("}\n");
}
*/
void printSeqErr(const unsigned char* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len)  fprintf(stderr, "%X", a[i++]);
    fprintf(stderr, "}\n");
}
/*static void printUSeqErr(const UChar* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len) fprintf(stderr, "%4X", a[i++]);
    fprintf(stderr,"}\n");
}
*/
void addExtraTests(TestNode** root)
{
     addTest(root, &TestSurrogateBehaviour, "tsconv/ncnvtst/TestSurrogateBehaviour");
     addTest(root, &TestErrorBehaviour, "tsconv/ncnvtst/TestErrorBehaviour");

}

void TestSurrogateBehaviour(){
    log_verbose("Testing for SBCS and LATIN_1\n");
    {
        UChar    sampleText[] =   { 0x0031, 0xd801, 0xdc01, 0x0032};
        const char expected[] = 
            {  (char)0x31, (char)0x1a, (char)0x32};
        /*SBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-920", 0 , U_ZERO_ERROR))
            log_err("u-> ibm-920 [UCNV_SBCS] not match.\n");
        
        /*LATIN_1*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "LATIN_1", 0, U_ZERO_ERROR ))
            log_err("u-> LATIN_1 not match.\n");
       
    }
    log_verbose("Testing for DBCS and MBCS\n");
    {
        UChar    sampleText[] =   { 0x00a1, 0xd801, 0xdc01, 0x00a4};
        const char expected[] = 
            {  (char)0xa2, (char)0xae, (char)0xa1, (char)0xe0, (char)0xa2, (char)0xb4};
    
        /*DBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1362", 0 , U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] not match.\n");
      
        /*MBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1363", 0 , U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_MBCS] not match.\n");
    }
    /*UTF-8*/
     log_verbose("Testing for UTF8\n");
    {
        UChar    sampleText[] =   { 0x0031, 0x0701, 0x7f81, 0xbfc1, 0x0032};
        const char expected[] = 
            {  (char)0x31, (char)0xdc, (char)0x81, (char)0xe7, (char)0xbe, (char)0x81, (char)0xeb, (char)0xbf, (char)0x81, (char)0x32};
        /*UTF-8*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expected, sizeof(expected), "UTF8", 0, U_ZERO_ERROR ))
            log_err("u-> UTF8 did not match.\n");
    }

}
void TestErrorBehaviour(){
     log_verbose("Testing for SBCS and LATIN_1\n");
    {
        UChar    sampleText[] =   { 0x0031, 0xd801};
        UChar    sampleText2[] =   { 0x0031, 0xd801, 0x0032};
        const char expected[] = 
            {  (char)0x31, (char)0x1a };
        const char expected2[] = 
            {  (char)0x31, (char)0x1a, (char)0x32};
        
        /*SBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-920", 0, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> ibm-920 [UCNV_SBCS] \n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-920", 0, U_ZERO_ERROR))
            log_err("u-> ibm-920 [UCNV_SBCS] did not match\n");
        
        
        /*LATIN_1*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "LATIN_1", 0, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> LATIN_1 is supposed to fail\n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "LATIN_1", 0, U_ZERO_ERROR))
            log_err("u-> LATIN_1 did not match\n");


    }
     log_verbose("Testing for DBCS and MBCS\n");
    {
        UChar    sampleText[] =   { 0x00a1, 0xd801};
        UChar    sampleText2[] =   { 0x00a1, 0xd801, 0x00a4};
        const char expected[] = 
            {  (char)0xa2, (char)0xae, (char)0xa1, (char)0xe0};
        const char expected2[] = 
             {  (char)0xa2, (char)0xae, (char)0xa1, (char)0xe0, (char)0xa2, (char)0xb4};
        
        /*DBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1362", 0, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> ibm-1362 [UCNV_DBCS] is supposed to fail\n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1362", 0, U_ZERO_ERROR))
            log_err("u-> ibm-1362 [UCNV_DBCS] did not match \n");
        
        
        /*MBCS*/
        if(!convertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
                expected, sizeof(expected), "ibm-1363", 0, U_TRUNCATED_CHAR_FOUND))
            log_err("u-> ibm-1363 [UCNV_MBCS] \n");
        if(!convertFromU(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
                expected2, sizeof(expected2), "ibm-1363", 0, U_ZERO_ERROR))
            log_err("u-> ibm-1363 [UCNV_DBCS] did not match\n");


    }
    
     
}

UBool convertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets, UErrorCode expectedStatus)
{
   
    int32_t i=0;
    const UChar *src;
    char buffer[MAX_LENGTH];
    /*int32_t offsetBuffer[MAX_LENGTH];
     int32_t *offs=0;*/
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
    for(i=0; i<MAX_LENGTH; i++){
        buffer[i]=(char)0xF0;
       /* offsetBuffer[i]=0xFF;*/
    }

    src=source;
    sourceLimit=(UChar*)src+(sourceLen);
    targ=buffer;
    targetLimit=targ+MAX_LENGTH;
   /* offs=offsetBuffer;*/
    ucnv_fromUnicode (conv,
                  &targ,
                  targetLimit,
                  &src,
                  sourceLimit,
                  NULL,/*offs,*/
                  TRUE, /* flush if we're at the end of the input data */
                  &status);
    ucnv_close(conv);
    if(U_FAILURE(status)){
        if(status == expectedStatus){
            return TRUE;
        } else {
            log_err("ucnv_fromUnicode() failed for codepage=%s. Error =%s Expected=%s\n", codepage, myErrorName(status), myErrorName(expectedStatus));
            return FALSE;
        }
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



    
}