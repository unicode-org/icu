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
*    Steven R. Loomis     7/8/1999      Adding input buffer test
*********************************************************************************
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "cstring.h"
#include "unicode/uloc.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "cintltst.h"
#include "unicode/utypes.h"
#include "unicode/ustring.h"

static void printSeq(const unsigned char* a, int len);
static void printUSeq(const UChar* a, int len);
static void TestNextUChar(UConverter* cnv, const char* source, const char* limit, const uint32_t results[], const char* message);
static void TestNextUCharError(UConverter* cnv, const char* source, const char* limit, UErrorCode expected, const char* message);

void TestNewConvertWithBufferSizes(int32_t osize, int32_t isize) ;
void TestConverterTypesAndStarters(void);
void TestAmbiguous(void);
void TestUTF8(void);
void TestUTF16BE(void);
void TestUTF16LE(void);
void TestLATIN1(void);
void TestSBCS(void);
void TestDBCS(void);
void TestMBCS(void);
void TestISO_2022(void);
void TestISO_2022_JP(void);
void TestEBCDIC_STATEFUL(void);
void TestLMBCS(void);
void TestJitterbug255(void);
void TestEBCDICUS4XML(void);

#define NEW_MAX_BUFFER 999

static int32_t  gInBufferSize = 0;
static int32_t  gOutBufferSize = 0;
static char     gNuConvTestName[1024];

#define nct_min(x,y)  ((x<y) ? x : y)

void printSeq(const unsigned char* a, int len)
{
    int i=0;
    log_verbose("{");
    while (i<len) log_verbose("0x%02x ", a[i++]);
    log_verbose("}\n");
}
void printUSeq(const UChar* a, int len)
{
    int i=0;
    log_verbose("{U+");
    while (i<len) log_verbose("0x%04x ", a[i++]);
    log_verbose("}\n");
}

void printSeqErr(const unsigned char* a, int len)
{
    int i=0;
    fprintf(stderr, "{");
    while (i<len)  fprintf(stderr, "0x%02x ", a[i++]);
    fprintf(stderr, "}\n");
}
void printUSeqErr(const UChar* a, int len)
{
    int i=0;
    fprintf(stderr, "{U+");
    while (i<len) fprintf(stderr, "0x%04x ", a[i++]);
    fprintf(stderr,"}\n");
}
void 
TestNextUChar(UConverter* cnv, const char* source, const char* limit, const uint32_t results[], const char* message)
{
     const char* s0;
     const char* s=(char*)source;
     const uint32_t *r=results;
     UErrorCode errorCode=U_ZERO_ERROR;
     uint32_t c;

     while(s<limit) {
        s0=s;
        c=ucnv_getNextUChar(cnv, &s, limit, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("%s ucnv_getNextUChar() failed: %s\n", message, u_errorName(errorCode));
            break;
        } else if((uint32_t)(s-s0)!=*r || c!=(UChar32)*(r+1)) {
            log_err("%s ucnv_getNextUChar() result %lx from %d bytes, should have been %lx from %d bytes.\n",
                message, c, (s-s0), *(r+1), *r);
            break;
        }
        r+=2;
    }
}
void 
TestNextUCharError(UConverter* cnv, const char* source, const char* limit, UErrorCode expected, const char* message)
{
     const char* s=(char*)source;
     UErrorCode errorCode=U_ZERO_ERROR;
     uint32_t c;
     c=ucnv_getNextUChar(cnv, &s, limit, &errorCode);
     if(errorCode != expected){
        log_err("FAIL: Expected:%s when %s-----Got:%s\n", myErrorName(expected), message, myErrorName(errorCode));
     }
     if(c != 0xFFFD && c != 0xffff){
        log_err("FAIL: Expected return value of 0xfffd or 0xffff when %s-----Got 0x%lx\n", message, c);
     }
     
}   
void TestInBufSizes(void)
{
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,1);
#if 1
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,2);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,3);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,4);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,5);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,6);
  TestNewConvertWithBufferSizes(1,1);
  TestNewConvertWithBufferSizes(2,3);
  TestNewConvertWithBufferSizes(3,2);
#endif
}

void TestOutBufSizes(void)
{
#if 1
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(1,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(2,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(3,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(4,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(5,NEW_MAX_BUFFER);
  
#endif
}


void addTestNewConvert(TestNode** root)
{
   addTest(root, &TestInBufSizes, "tsconv/nucnvtst/TestInBufSizes");
   addTest(root, &TestOutBufSizes, "tsconv/nucnvtst/TestOutBufSizes");
   addTest(root, &TestConverterTypesAndStarters, "tsconv/nucnvtst/TestConverterTypesAndStarters");
   addTest(root, &TestAmbiguous, "tsconv/nucnvtst/TestAmbiguous");
   addTest(root, &TestUTF8, "tsconv/nucnvtst/TestUTF8");
   addTest(root, &TestUTF16BE, "tsconv/nucnvtst/TestUTF16BE");
   addTest(root, &TestUTF16LE, "tsconv/nucnvtst/TestUTF16LE");
   addTest(root, &TestLATIN1, "tsconv/nucnvtst/TestLATIN1");
   addTest(root, &TestSBCS, "tsconv/nucnvtst/TestSBCS");
   addTest(root, &TestDBCS, "tsconv/nucnvtst/TestDBCS");
   addTest(root, &TestMBCS, "tsconv/nucnvtst/TestMBCS");
   addTest(root, &TestISO_2022, "tsconv/nucnvtst/TestISO_2022");
   addTest(root, &TestISO_2022_JP, "tsconv/nucnvtst/TestISO_2022_JP");
   addTest(root, &TestEBCDIC_STATEFUL, "tsconv/nucnvtst/TestEBCDIC_STATEFUL");
   addTest(root, &TestLMBCS, "tsconv/nucnvtst/TestLMBCS");
   addTest(root, &TestJitterbug255, "tsconv/nucnvtst/TestJitterbug255");
   addTest(root, &TestEBCDICUS4XML, "tsconv/nucnvtst/TestEBCDICUS4XML");
}


/* Note that this test already makes use of statics, so it's not really 
   multithread safe. 
   This convenience function lets us make the error messages actually useful.
*/

void setNuConvTestName(const char *codepage, const char *direction)
{
  sprintf(gNuConvTestName, "[Testing %s %s Unicode, InputBufSiz=%d, OutputBufSiz=%d]",
      codepage,
      direction,
      gInBufferSize,
      gOutBufferSize);
}

UBool testConvertFromU( const UChar *source, int sourceLen,  const uint8_t *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    uint8_t    junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t    junokout[NEW_MAX_BUFFER]; /* FIX */
    uint8_t *p;
    const UChar *src;
    uint8_t *end;
    uint8_t *targ;
    int32_t *offs;
    int i;
    int32_t   realBufferSize;
    uint8_t *realBufferEnd;
    const UChar *realSourceEnd;
    const UChar *sourceLimit;
    UBool checkOffsets = TRUE;
    UBool doFlush;

    for(i=0;i<NEW_MAX_BUFFER;i++)
        junkout[i] = 0xF0;
    for(i=0;i<NEW_MAX_BUFFER;i++)
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

    src = source;
    targ = junkout;
    offs = junokout;

    realBufferSize = (sizeof(junkout)/sizeof(junkout[0]));
    realBufferEnd = junkout + realBufferSize;
    realSourceEnd = source + sourceLen;

    if ( gOutBufferSize != realBufferSize )
        checkOffsets = FALSE;
 
    if( gInBufferSize != NEW_MAX_BUFFER )
        checkOffsets = FALSE;

    do
    {
        end = nct_min(targ + gOutBufferSize, realBufferEnd);
        sourceLimit = nct_min(src + gInBufferSize, realSourceEnd);

        doFlush = (UBool)(sourceLimit == realSourceEnd);

        if(targ == realBufferEnd)
          {
        log_err("Error, overflowed the real buffer while about to call fromUnicode! targ=%08lx %s", targ, gNuConvTestName);
        return FALSE;
          }
        log_verbose("calling fromUnicode @ SOURCE:%08lx to %08lx  TARGET: %08lx to %08lx, flush=%s\n", src,sourceLimit, targ,end, doFlush?"TRUE":"FALSE");
        

        status = U_ZERO_ERROR;
 
        ucnv_fromUnicode (conv,
                  (char **)&targ,
                  (const int8_t*)end,
                  &src,
                  sourceLimit,
                  checkOffsets ? offs : NULL,
                  doFlush, /* flush if we're at the end of the input data */
                  &status);
    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (U_SUCCESS(status) && sourceLimit < realSourceEnd) );

    if(U_FAILURE(status))
    {
        log_err("Problem doing fromUnicode to %s, errcode %s %s\n", codepage, myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-junkout);
    if(VERBOSITY)
    {
        char junk[9999];
        char offset_str[9999];
        uint8_t *p;

        junk[0] = 0;
        offset_str[0] = 0;
        for(p = junkout;p<targ;p++)
        {
            sprintf(junk + strlen(junk), "0x%02x, ", (int)(0xFF & *p));
            sprintf(offset_str + strlen(offset_str), "0x%02x, ", (int)(0xFF & junokout[p-junkout]));
        }

        log_verbose(junk);
        printSeq((const uint8_t *)expect, expectLen);
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
        printSeqErr((const unsigned char*)junkout, targ-junkout);
        printf("\nExpected:");
        printSeqErr((const unsigned char*)expect, expectLen);
        return FALSE;
    }

    if (checkOffsets && (expectOffsets != 0) )
    {
        log_verbose("comparing %d offsets..\n", targ-junkout);
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t) )){
            log_err("did not get the expected offsets. %s", gNuConvTestName);
            log_err("Got  : ");
            printSeqErr((const unsigned char*)junkout, targ-junkout);
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

UBool testConvertToU( const uint8_t *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    UChar    junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t    junokout[NEW_MAX_BUFFER]; /* FIX */
    const uint8_t *src;
    const uint8_t *realSourceEnd;
    const uint8_t *srcLimit;
    UChar *p;
    UChar *targ;
    UChar *end;
    int32_t *offs;
    int i;
    UBool   checkOffsets = TRUE;
    
    int32_t   realBufferSize;
    UChar *realBufferEnd;
    

    for(i=0;i<NEW_MAX_BUFFER;i++)
        junkout[i] = 0xFFFE;

    for(i=0;i<NEW_MAX_BUFFER;i++)
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

    src = source;
    targ = junkout;
    offs = junokout;
    
    realBufferSize = (sizeof(junkout)/sizeof(junkout[0]));
    realBufferEnd = junkout + realBufferSize;
    realSourceEnd = src + sourcelen;

    if ( gOutBufferSize != realBufferSize )
      checkOffsets = FALSE;

    if( gInBufferSize != NEW_MAX_BUFFER )
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
                (const char **)&src,
                (const char *)srcLimit,
                checkOffsets ? offs : NULL,
                (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
                &status);

        /*        offs += (targ-oldTarg); */

      } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (U_SUCCESS(status) && (srcLimit < realSourceEnd)) ); /* while we just need another buffer */

    if(U_FAILURE(status))
    {
        log_err("Problem doing %s toUnicode, errcode %s %s\n", codepage, myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done. %d bytes -> %d chars.\nResult :",
        sourcelen, targ-junkout);
    if(VERBOSITY)
    {
        char junk[9999];
        char offset_str[9999];
    
        UChar *p;
        
        junk[0] = 0;
        offset_str[0] = 0;

        for(p = junkout;p<targ;p++)
        {
            sprintf(junk + strlen(junk), "0x%04x, ", (0xFFFF) & (unsigned int)*p);
            sprintf(offset_str + strlen(offset_str), "0x%04x, ", (0xFFFF) & (unsigned int)junokout[p-junkout]);
        }
        
        log_verbose(junk);
        printUSeq(expect, expectlen);
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
        printUSeqErr(junkout, expectlen);
        printf("\nExpected:");
        printUSeqErr(expect, expectlen); 
        return FALSE;
    }
}


void TestNewConvertWithBufferSizes(int32_t outsize, int32_t insize ) 
{
/** test chars #1 */
    /*  1 2 3  1Han 2Han 3Han .  */
    UChar    sampleText[] = 
     { 0x0031, 0x0032, 0x0033, 0x0000, 0x4e00, 0x4e8c, 0x4e09,  0x002E  };

    
    const uint8_t expectedUTF8[] = 
     { 0x31, 0x32, 0x33, 0x00, 0xe4, 0xb8, 0x80, 0xe4, 0xba, 0x8c, 0xe4, 0xb8, 0x89, 0x2E };
    int32_t  toUTF8Offs[] = 
     { 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x07};
    int32_t fmUTF8Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0007, 0x000a, 0x000d };
    
    /* Same as UTF8, but with ^[%B preceeding */
    const uint8_t expectedISO2022[] = 
     { 0x1b, 0x25, 0x42, 0x31, 0x32, 0x33, 0x00, 0xe4, 0xb8, 0x80, 0xe4, 0xba, 0x8c, 0xe4, 0xb8, 0x89, 0x2E };
    int32_t  toISO2022Offs[]     = 
     { -1, -1, -1, 0x00, 0x01, 0x02, 0x03, 0x04, 0x04,  
       0x04, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x07 }; /* right? */
    int32_t fmISO2022Offs[] = 
     { 0x0003, 0x0004, 0x0005, 0x0006, 0x0007, 0x000a, 0x000d, 0x0010 }; /* is this right? */
   
    /*  1 2 3 0, <SO> h1 h2 h3 <SI> . EBCDIC_STATEFUL */
    const uint8_t expectedIBM930[] = 
     { 0xF1, 0xF2, 0xF3, 0x00, 0x0E, 0x45, 0x41, 0x45, 0x42, 0x45, 0x43, 0x0F, 0x4B };
    int32_t  toIBM930Offs[] = 
     { 0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07, 0x07, };
    int32_t fmIBM930Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0005, 0x0007, 0x0009, 0x000c};
    
    /* 1 2 3 0 h1 h2 h3 . MBCS*/
    const uint8_t expectedIBM943[] = 
     {  0x31, 0x32, 0x33, 0x00, 0x88, 0xea, 0x93, 0xf1, 0x8e, 0x4f, 0x2e };
    int32_t  toIBM943Offs    [] = 
     {  0x00, 0x01, 0x02, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07 };
    int32_t fmIBM943Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0006, 0x0008, 0x000a};
    
    /* 1 2 3 0 h1 h2 h3 . DBCS*/
    const uint8_t expectedIBM835[] = 
     {  0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0xfe, 0x4c, 0x41, 0x4c, 0x48, 0x4c, 0x55, 0xfe, 0xfe};
    int32_t  toIBM835Offs    [] = 
     {  0x00, 0x00, 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07, 0x07};
   
     /* 1 2 3 0 <?> <?> <?> . SBCS*/
    const uint8_t expectedIBM920[] = 
     {  0x31, 0x32, 0x33, 0x00, 0x1a, 0x1a, 0x1a, 0x2e };
    int32_t  toIBM920Offs    [] = 
     {  0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };
   
    /* 1 2 3 0 <?> <?> <?> . SBCS*/
    const uint8_t expectedISO88593[] = 
     { 0x31, 0x32, 0x33, 0x00, 0x1a, 0x1a, 0x1a, 0x2E };
    int32_t  toISO88593Offs[]     = 
     { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

    /* 1 2 3 0 <?> <?> <?> . LATIN_1*/
    const uint8_t expectedLATIN1[] = 
     { 0x31, 0x32, 0x33, 0x00, 0x1a, 0x1a, 0x1a, 0x2E };
    int32_t  toLATIN1Offs[]     = 
     { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    
   
    /*  etc */
    const uint8_t expectedUTF16LE[] = 
     { 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x00, 0x00, 0x00, 0x4e, 0x8c, 0x4e, 0x09, 0x4e, 0x2e, 0x00 };
    int32_t      toUTF16LEOffs[]=  
     { 0x00, 0x00, 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06,  0x07, 0x07};
    int32_t fmUTF16LEOffs[] = 
     { 0x0000, 0x0002, 0x0004, 0x0006, 0x0008, 0x000a, 0x000c,  0x000e }; 

    const uint8_t expectedUTF16BE[] = 
     { 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x00, 0x4e, 0x00, 0x4e, 0x8c, 0x4e, 0x09, 0x00, 0x2e };
    int32_t      toUTF16BEOffs[]=  
     { 0x00, 0x00, 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07, 0x07};
    int32_t fmUTF16BEOffs[] = 
     { 0x0000, 0x0002, 0x0004, 0x0006, 0x0008, 0x000a, 0x000c,  0x000e }; 
    
  
    


/** Test chars #2 **/

    /* Sahha [health],  slashed h's */
    const UChar malteseUChars[] = { 0x0053, 0x0061, 0x0127, 0x0127, 0x0061 };
    const uint8_t expectedMaltese913[] = { 0x53, 0x61, 0xB1, 0xB1, 0x61 };

    /* LMBCS */
    const UChar LMBCSUChars[]  = { 0x0027, 0x010A, 0x0000, 0x0127, 0x2666 };
    const uint8_t expectedLMBCS[] = { 0x27, 0x06, 0x04, 0x00, 0x01, 0x73, 0x01, 0x04 };
    int32_t toLMBCSOffs[]   = { 0x00, 0x01, 0x01, 0x02, 0x03, 0x03, 0x04, 0x04 };
    int32_t fmLMBCSOffs[]   = { 0x0000, 0x0001, 0x0003, 0x0004, 0x0006};
    /*********************************** START OF CODE finally *************/

  gInBufferSize = insize;
  gOutBufferSize = outsize;

  log_verbose("\n\n\nTesting conversions with InputBufferSize = %d, OutputBufferSize = %d\n", gInBufferSize, gOutBufferSize);

    
#if 1
    /*UTF-8*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF8, sizeof(expectedUTF8), "UTF8", toUTF8Offs ))
        log_err("u-> UTF8 did not match.\n");
    
    log_verbose("Test surrogate behaviour for UTF8\n");
    {
        const UChar testinput[]={ 0x20ac, 0xd801, 0xdc01, 0xdc01, 0xd801};
        const uint8_t expectedUTF8[]= { 0xe2, 0x82, 0xac, 
                           0xf0, 0x90, 0x90, 0x81, 
                           0xed, 0xb0, 0x81, 0xed, 0xa0, 0x81 
        };
        int32_t offsets[]={ 0, 0, 0, 1, 1, 1, 1, 3, 3, 3, 4, 4, 4 };
        if(!testConvertFromU(testinput, sizeof(testinput)/sizeof(testinput[0]),
            expectedUTF8, sizeof(expectedUTF8), "UTF8", offsets ))
        log_err("u-> UTF8 did not match.\n");  

    }
    /*ISO-2022*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedISO2022, sizeof(expectedISO2022), "ISO_2022", toISO2022Offs ))
        log_err("u-> iso-2022 did not match.\n");
    /*UTF16 LE*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF16LE, sizeof(expectedUTF16LE), "utf-16le", toUTF16LEOffs ))
        log_err("u-> utf-16le did not match.\n");
    /*UTF16 BE*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF16BE, sizeof(expectedUTF16BE), "utf-16be", toUTF16BEOffs ))
        log_err("u-> utf-16be did not match.\n");
    /*LATIN_1*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedLATIN1, sizeof(expectedLATIN1), "LATIN_1", toLATIN1Offs ))
        log_err("u-> LATIN_1 did not match.\n");
    /*EBCDIC_STATEFUL*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedIBM930, sizeof(expectedIBM930), "ibm-930", toIBM930Offs ))
        log_err("u-> ibm-930 did not match.\n");

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedISO88593, sizeof(expectedISO88593), "iso-8859-3", toISO88593Offs ))
        log_err("u-> iso-8859-3 did not match.\n");

    /*MBCS*/

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedIBM943, sizeof(expectedIBM943), "ibm-943", toIBM943Offs ))
        log_err("u-> ibm-943 [UCNV_MBCS] not match.\n");
    /*DBCS*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedIBM835, sizeof(expectedIBM835), "ibm-835", toIBM835Offs ))
        log_err("u-> ibm-835 [UCNV_DBCS] not match.\n");
    /*SBCS*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedIBM920, sizeof(expectedIBM920), "ibm-920", toIBM920Offs ))
        log_err("u-> ibm-920 [UCNV_SBCS] not match.\n");
    /*SBCS*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedISO88593, sizeof(expectedISO88593), "iso-8859-3", toISO88593Offs ))
        log_err("u-> iso-8859-3 did not match.\n");

 
/****/
#endif

#if 1
    /*UTF-8*/
    if(!testConvertToU(expectedUTF8, sizeof(expectedUTF8),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf8", fmUTF8Offs ))
      log_err("utf8 -> u did not match\n");
    /*ISO-2022*/
    if(!testConvertToU(expectedISO2022, sizeof(expectedISO2022),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ISO_2022", fmISO2022Offs ))
      log_err("iso-2022  -> u  did not match.\n");
    /*UTF16 LE*/
    if(!testConvertToU(expectedUTF16LE, sizeof(expectedUTF16LE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16le", fmUTF16LEOffs ))
      log_err("utf-16le -> u  did not match.\n");
    /*UTF16 BE*/
    if(!testConvertToU(expectedUTF16BE, sizeof(expectedUTF16BE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16be", fmUTF16BEOffs ))
      log_err("utf-16be -> u  did not match.\n");
    /*EBCDIC_STATEFUL*/
    if(!testConvertToU(expectedIBM930, sizeof(expectedIBM930),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ibm-930", fmIBM930Offs ))
      log_err("ibm-930  -> u  did not match.\n");
    /*MBCS*/
    if(!testConvertToU(expectedIBM943, sizeof(expectedIBM943),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ibm-943", fmIBM943Offs ))
      log_err("ibm-943 -> u  did not match.\n");

    if(!testConvertToU(expectedUTF16LE, sizeof(expectedUTF16LE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16le", fmUTF16LEOffs ))
      log_err("utf-16le -> u  did not match.\n");

    if(!testConvertToU(expectedMaltese913, sizeof(expectedMaltese913),
               malteseUChars, sizeof(malteseUChars)/sizeof(malteseUChars[0]), "latin3", NULL))
      log_err("latin3[813] -> u did not match\n");

    if(!testConvertFromU(malteseUChars, sizeof(malteseUChars)/sizeof(malteseUChars[0]),
            expectedMaltese913, sizeof(expectedMaltese913), "iso-8859-3", NULL ))
        log_err("u-> latin3[813] did not match.\n");  

   /*LMBCS*/
    if(!testConvertFromU(LMBCSUChars, sizeof(LMBCSUChars)/sizeof(LMBCSUChars[0]),
            expectedLMBCS, sizeof(expectedLMBCS), "LMBCS-1", toLMBCSOffs ))
        log_err("u-> LMBCS-1 did not match.\n");
    if(!testConvertToU(expectedLMBCS, sizeof(expectedLMBCS),
               LMBCSUChars, sizeof(LMBCSUChars)/sizeof(LMBCSUChars[0]), "LMBCS-1", fmLMBCSOffs ))
      log_err("LMBCS-1 -> u  did not match.\n");


    /*some more test to increase the code coverage in MBCS.  Create an test converter from test1.ucm
      which is test file for MBCS conversion with single-byte codepage data.*/
    {
        
        /* MBCS with single byte codepage data test1.ucm*/
        const UChar unicodeInput[]    = { 0x20ac, 0x0005, 0x0006, 0xdbc4, 0xde34, 0x0003};
        const uint8_t expectedtest1[] = { 0x00, 0x05, 0xff, 0x07, 0xff,};
        int32_t  totest1Offs[]        = { 0, 1, 2, 3, 5, }; 

        const uint8_t test1input[]    = { 0x00, 0x05, 0x06, 0x07, 0x08, 0x09};
        const UChar expectedUnicode[] = { 0x20ac, 0x0005, 0xfffd, 0xdbc4, 0xde34, 0xfffd, 0xfffd};
        int32_t fromtest1Offs[]       = { 0, 1, 2, 3, 3, 4, 5};

        /*from Unicode*/
        if(!testConvertFromU(unicodeInput, sizeof(unicodeInput)/sizeof(unicodeInput[0]),
                expectedtest1, sizeof(expectedtest1), "test1", totest1Offs ))
            log_err("u-> test1(MBCS conversion with single-byte) did not match.\n");
        
        /*to Unicode*/
        if(!testConvertToU(test1input, sizeof(test1input),
               expectedUnicode, sizeof(expectedUnicode)/sizeof(expectedUnicode[0]), "test1", fromtest1Offs ))
            log_err("test1(MBCS conversion with single-byte) -> u  did not match.\n");

    }

    /*some more test to increase the code coverage in MBCS.  Create an test converter from test3.ucm
      which is test file for MBCS conversion with three-byte codepage data.*/
    {
        
        /* MBCS with three byte codepage data test3.ucm*/
        const UChar unicodeInput[]    = { 0x20ac, 0x0005, 0x0006, 0x000b, 0xdbc4, 0xde34, 0xd84d, 0xdc56, 0x000e};
        const uint8_t expectedtest3[] = { 0x00, 0x05, 0xff, 0x01, 0x02, 0x0b,  0x07,  0x01, 0x02, 0x0a,  0xff,};
        int32_t  totest3Offs[]        = { 0, 1, 2, 3, 3, 3, 4, 6, 6, 6, 8}; 

        const uint8_t test3input[]    = { 0x00, 0x05, 0x06, 0x01, 0x02, 0x0b,  0x07,  0x01, 0x02, 0x0a, 0x01, 0x02, 0x0c,};
        const UChar expectedUnicode[] = { 0x20ac, 0x0005, 0xfffd, 0x000b, 0xdbc4, 0xde34, 0xd84d, 0xdc56, 0xfffd};
        int32_t fromtest3Offs[]       = { 0, 1, 2, 3, 6, 6, 7, 7, 10 };

        /*from Unicode*/
        if(!testConvertFromU(unicodeInput, sizeof(unicodeInput)/sizeof(unicodeInput[0]),
                expectedtest3, sizeof(expectedtest3), "test3", totest3Offs ))
            log_err("u-> test3(MBCS conversion with three-byte) did not match.\n");
        
        /*to Unicode*/
        if(!testConvertToU(test3input, sizeof(test3input),
               expectedUnicode, sizeof(expectedUnicode)/sizeof(expectedUnicode[0]), "test3", fromtest3Offs ))
            log_err("test3(MBCS conversion with three-byte) -> u  did not match.\n");

    }
    
    /*some more test to increase the code coverage in MBCS.  Create an test converter from test4.ucm
      which is test file for MBCS conversion with four-byte codepage data.*/
    {
        
        /* MBCS with three byte codepage data test4.ucm*/
        const UChar unicodeInput[]    = { 0x20ac, 0x0005, 0x0006, 0x000b, 0xdbc4, 0xde34, 0xd84d, 0xdc56, 0x000e};
        const uint8_t expectedtest4[] = { 0x00, 0x05, 0xff, 0x01, 0x02, 0x03, 0x0b,  0x07,  0x01, 0x02, 0x03, 0x0a,  0xff,};
        int32_t  totest4Offs[]        = { 0, 1, 2, 3, 3, 3, 3, 4, 6, 6, 6, 6, 8,}; 

        const uint8_t test4input[]    = { 0x00, 0x05, 0x06, 0x01, 0x02, 0x03, 0x0b,  0x07,  0x01, 0x02, 0x03, 0x0a, 0x01, 0x02, 0x03, 0x0c,};
        const UChar expectedUnicode[] = { 0x20ac, 0x0005, 0xfffd, 0x000b, 0xdbc4, 0xde34, 0xd84d, 0xdc56, 0xfffd};
        int32_t fromtest4Offs[]       = { 0, 1, 2, 3, 7, 7, 8, 8, 12,};

        /*from Unicode*/
        if(!testConvertFromU(unicodeInput, sizeof(unicodeInput)/sizeof(unicodeInput[0]),
                expectedtest4, sizeof(expectedtest4), "test4", totest4Offs ))
            log_err("u-> test4(MBCS conversion with four-byte) did not match.\n");
        
        /*to Unicode*/
        if(!testConvertToU(test4input, sizeof(test4input),
               expectedUnicode, sizeof(expectedUnicode)/sizeof(expectedUnicode[0]), "test4", fromtest4Offs ))
            log_err("test4(MBCS conversion with four-byte) -> u  did not match.\n");

    }



 
}  
     

void TestConverterTypesAndStarters()
{
    UConverter* myConverter[3];
    UErrorCode err = U_ZERO_ERROR;
    UBool mystarters[256];
    
/*    const UBool expectedKSCstarters[256] = {
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE,
        FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE,
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE};*/


  log_verbose("Testing KSC, ibm-930, ibm-878  for starters and their conversion types.");

    myConverter[0] = ucnv_open("ksc", &err);
    if (U_FAILURE(err))
      log_err("Failed to create an ibm-ksc converter\n");
    else
      {
        if (ucnv_getType(myConverter[0])!=UCNV_MBCS) log_err("ucnv_getType Failed for ibm-949\n");
        else log_verbose("ucnv_getType ibm-949 ok\n");

        if(myConverter[0]!=NULL)
          ucnv_getStarters(myConverter[0], mystarters, &err);

        /*if (memcmp(expectedKSCstarters, mystarters, sizeof(expectedKSCstarters)))
          log_err("Failed ucnv_getStarters for ksc\n");
          else
          log_verbose("ucnv_getStarters ok\n");*/
        
      }

    myConverter[1] = ucnv_open("ibm-930", &err);
    if (U_FAILURE(err))
      log_err("Failed to create an ibm-930 converter\n");
    else
      {
        if (ucnv_getType(myConverter[1])!=UCNV_EBCDIC_STATEFUL) log_err("ucnv_getType Failed for ibm-930\n");
        else log_verbose("ucnv_getType ibm-930 ok\n");
      }

    myConverter[2] = ucnv_open("ibm-878", &err);
    if (U_FAILURE(err))
      log_err("Failed to create an ibm-815 converter\n");
    else
      {
        if (ucnv_getType(myConverter[2])!=UCNV_SBCS) log_err("ucnv_getType Failed for ibm-815\n");
        else log_verbose("ucnv_getType ibm-815 ok\n");
      }

    
    ucnv_close(myConverter[0]);
    ucnv_close(myConverter[1]);
    ucnv_close(myConverter[2]);
}
void TestAmbiguous()
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *ascii_cnv = 0, *sjis_cnv = 0;
    const char target[] = {
        /* "\\usr\\local\\share\\data\\icutest.txt" */
        0x5c, 0x75, 0x73, 0x72,
        0x5c, 0x6c, 0x6f, 0x63, 0x61, 0x6c,
        0x5c, 0x73, 0x68, 0x61, 0x72, 0x65,
        0x5c, 0x64, 0x61, 0x74, 0x61,
        0x5c, 0x69, 0x63, 0x75, 0x74, 0x65, 0x73, 0x74, 0x2e, 0x74, 0x78, 0x74,
        0
    };
    UChar *asciiResult = 0, *sjisResult = 0;
    int32_t asciiLength = 0, sjisLength = 0;
    
    sjis_cnv = ucnv_open("SJIS", &status);
    if (U_FAILURE(status))
    {
        log_err("Failed to create a SJIS converter\n");
        return;
    }
    ascii_cnv = ucnv_open("LATIN-1", &status);
    if (U_FAILURE(status))
    {
        log_err("Failed to create a SJIS converter\n");
        ucnv_close(sjis_cnv);
        return;
    }
    /* convert target from SJIS to Unicode */
    sjisLength = ucnv_toUChars(sjis_cnv, sjisResult, 0, target, strlen(target), &status);
    status = U_ZERO_ERROR;
    sjisResult = (UChar*)malloc(sizeof(UChar)* sjisLength);
    ucnv_toUChars(sjis_cnv, sjisResult, sjisLength, target, strlen(target), &status);
    if (U_FAILURE(status))
    {
        log_err("Failed to convert the SJIS string.\n");
        ucnv_close(sjis_cnv);
        ucnv_close(ascii_cnv);
        return;
    }
    /* convert target from Latin-1 to Unicode */
    asciiLength = ucnv_toUChars(ascii_cnv, asciiResult, 0, target, strlen(target), &status);
    status = U_ZERO_ERROR;
    asciiResult = (UChar*)malloc(sizeof(UChar)* asciiLength);
    ucnv_toUChars(ascii_cnv, asciiResult, asciiLength, target, strlen(target), &status);
    if (U_FAILURE(status))
    {
        log_err("Failed to convert the Latin-1 string.\n");
        free(sjisResult);
        ucnv_close(sjis_cnv);
        ucnv_close(ascii_cnv);
        return;
    }    
    if (!ucnv_isAmbiguous(sjis_cnv))
    {
        log_err("SJIS converter should contain ambiguous character mappings.\n");
        free(sjisResult);
        free(asciiResult);
        ucnv_close(sjis_cnv);
        ucnv_close(ascii_cnv);
        return;
    }
    if (u_strcmp(sjisResult, asciiResult) == 0)
    {
        log_err("File separators for SJIS don't need to be fixed.\n");
    }
    ucnv_fixFileSeparator(sjis_cnv, sjisResult, sjisLength);
    if (u_strcmp(sjisResult, asciiResult) != 0)
    {
        log_err("Fixing file separator for SJIS failed.\n");
    }
    free(sjisResult);
    free(asciiResult);
    ucnv_close(sjis_cnv);
    ucnv_close(ascii_cnv);
}
  
void
TestUTF8() {
    /* test input */
    static const uint8_t in[]={
        0x61,
        0xc0, 0x80,
        0xe0, 0x80, 0x80,
        0xf0, 0x80, 0x80, 0x80,
        0xf4, 0x84, 0x8c, 0xa1,
        0xf0, 0x90, 0x90, 0x81,
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x61,
        2, 0,
        3, 0,
        4, 0,
        4, 0x104321,
        4, 0x10401,
    };

    const char *source=(const char *)in,*limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("UTF-8", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a UTF-8 converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "UTF-8");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    ucnv_close(cnv);
}
void
TestUTF16BE() {
    /* test input */
    static const uint8_t in[]={
        0x00, 0x61, 
        0x00, 0xc0, 
        0x00, 0x31, 
        0x00, 0xf4, 
        0xce, 0xfe,
        0xd8, 0x01, 0xdc, 0x01,
                
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        2, 0x61,
        2, 0xc0,
        2, 0x31, 
        2, 0xf4,
        2, 0xcefe,
        4, 0x10401,
        
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("utf-16be", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a UTF16-BE converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "UTF-16BE");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for the condition where there is an invalid character*/
    {
        static const uint8_t source2[]={0x61};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_TRUNCATED_CHAR_FOUND, "an invalid character");
    }
    /*Test for the condition where there is a surrogate pair*/
    {
        const uint8_t source2[]={0xd8, 0x01};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_TRUNCATED_CHAR_FOUND, "an truncated surrogate character"); 
    }
    ucnv_close(cnv);
}
void
TestUTF16LE() {
    /* test input */
    static const uint8_t in[]={
        0x61, 0x00,
        0x31, 0x00,
        0x4e, 0x2e, 
        0x4e, 0x00,
        0x01, 0xd8, 0x01, 0xdc,
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        2, 0x61,
        2, 0x31,
        2, 0x2e4e,
        2, 0x4e,
        4, 0x10401,
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("utf-16le", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a UTF16-LE converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "UTF-16LE");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for the condition where there is an invalid character*/
    {
        static const uint8_t source2[]={0x61};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_TRUNCATED_CHAR_FOUND, "an invalid character");
    }
    /*Test for the condition where there is a surrogate character*/
    {
        static const uint8_t source2[]={0x01, 0xd8};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_TRUNCATED_CHAR_FOUND, "an truncated surrogate character");
    }
   
    ucnv_close(cnv);
}
void
TestLATIN1() {
    /* test input */
    static const uint8_t in[]={ 
       0x61,
       0x31,
       0x32,
       0xc0, 
       0xf0,
       0xf4, 
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x61,
        1, 0x31,
        1, 0x32,
        1, 0xc0,
        1, 0xf0,
        1, 0xf4,
    };

    const char *source=(const char *)in;
    const char *limit=(const char *)in+sizeof(in);

    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("LATIN_1", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a LATIN_1 converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "LATIN_1");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    ucnv_close(cnv);
}

void
TestSBCS() {
    /* test input */
    static const uint8_t in[]={ 0x61, 0xc0, 0x80, 0xe0, 0xf0, 0xf4};
    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x61,
        1, 0xbf,
        1, 0xc4,
        1, 0x2021,
        1, 0xf8fe,
        1, 0x00d9
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("ibm-1281", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a SBCS(ibm-1281) converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "SBCS(ibm-1281)");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for Illegal character*//*
    {
    static const uint8_t input1[]={ 0xA1 };
    const char* illegalsource=(const char*)input1;
    TestNextUCharError(cnv, illegalsource, illegalsource+sizeof(illegalsource), U_INVALID_CHAR_FOUND, "source has a illegal characte");
    }
   */
    ucnv_close(cnv);
}

void
TestDBCS() {
    /* test input */
    static const uint8_t in[]={
        0x44, 0x6a,
        0xc4, 0x9c,
        0x7a, 0x74,
        0x46, 0xab,
        0x42, 0x5b,
       
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        2, 0x00a7,
        2, 0xe1d2,
        2, 0x6962,
        2, 0xf842,
        2, 0xffe5, 
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    
    UConverter *cnv=ucnv_open("ibm-9027", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a DBCS(ibm-9027) converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "DBCS(ibm-9027)");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for the condition where we have a truncated char*/
    {
        static const uint8_t source1[]={0xc4};
        TestNextUCharError(cnv, (const char*)source1, (const char*)source1+sizeof(source1), U_TRUNCATED_CHAR_FOUND, "a character is truncated");
    }
    /*Test for the condition where there is an invalid character*/
    {
        static const uint8_t source2[]={0x1a, 0x1b};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_ZERO_ERROR, "an invalid character");
    }
    ucnv_close(cnv);
}
void
TestMBCS() {
    /* test input */
    static const uint8_t in[]={
        0x01,
        0xa6, 0xa3,
        0x00,
        0xa6, 0xa1,
        0x08,
        0xc2, 0x76,
        0xc2, 0x78,  
       
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x0001,
        2, 0x250c,
        1, 0x0000,
        2, 0x2500,
        1, 0x0008,  
        2, 0xd60c,
        2, 0xd60e,
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    
    UConverter *cnv=ucnv_open("ibm-1363", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a MBCS(ibm-1363) converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "MBCS(ibm-1363)");
    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for the condition where we have a truncated char*/
    {
        static const uint8_t source1[]={0xc4};
        TestNextUCharError(cnv, (const char*)source1, (const char*)source1+sizeof(source1), U_TRUNCATED_CHAR_FOUND, "a character is truncated");
    }
    /*Test for the condition where there is an invalid character*/
    {
        static const uint8_t source2[]={0xa1, 0x01};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_ZERO_ERROR, "an invalid character");
    }
    ucnv_close(cnv);

}
void
TestISO_2022() {
    /* test input */
    static const uint8_t in[]={
        0x1b, 0x25, 0x42, 0x31,
        0x32,
        0x61,
        0xc0, 0x80,
        0xe0, 0x80, 0x80,
        0xf0, 0x80, 0x80, 0x80
      
    };



    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        4, 0x0031,
        1, 0x0032,
        1, 0x61,
        2, 0,
        3, 0,
        4, 0, 
           
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv;

    cnv=ucnv_open("ISO_2022", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a iso-2022 converter: %s\n", u_errorName(errorCode));
        return;
    }
    TestNextUChar(cnv, source, limit, results, "ISO_2022");

    /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source-1, U_ILLEGAL_ARGUMENT_ERROR, "sourceLimit < source");
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for the condition where we have a truncated char*/
    {
        static const uint8_t source1[]={0xc4};
        TestNextUCharError(cnv, (const char*)source1, (const char*)source1+sizeof(source1), U_TRUNCATED_CHAR_FOUND, "a character is truncated");
    }
    /*Test for the condition where there is an invalid character*/
    {
        static const uint8_t source2[]={0xa1, 0x01};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_ZERO_ERROR, "an invalid character");
    }
    ucnv_close(cnv);
}
void
TestISO_2022_JP() {
    /* test input */
    static const uint16_t in[]={
		0x4E00,  0x4E20,  0x000A,  0x000D,  0x4E30,  0x4E40,  0x4E71,  0x4E73,  0x4E38,  0x000A,  
		0x000D,  0x4E15,  0x4EC5,  0x4EF3,  0x4EF1,  0x4EB1,  0x4E56,  0x4E14,  0x4E12,  0x000A,  
		0x000D,  0x4E01,  0x4E01,  0x4E12,  0x4E56,  0x4E86,  0x4E69,  0x4E46,  0x4E72,  0x4E21,
		0xAC13,  0xACA8,  0x000A,  0x000D,  0x4E01,  0x4E33,  0x4EA9,  0x4EA1,  0xF9D9,  0xF978,  
        0xF978,  0xF983,  0xF9D2,  0xF990,  0xF931,  0xF937,  0xF9B9,  0xF9D7,  0xF9D3,  0x000A,  
        0x000D,  0xFE40,  0xFF15,  0xFF2D,  0xFF0E,  0xFE33,  0xFE30,  0xFF21,  0xFF26,  0xFF19,  
        0xFE40,  0xFE44,  0xFE61,  0x000A,  0x000D,  0xF9B7,  0xF9EB,  0xF98C,  0xF962,  0xF912,  
        0xF911,  0xF9D1,  0x3053,  0x30A4,  0x30B9,  0x307C,  0x3055,  0x3093,  0x3109,  0x3109,  
        0x000A,  0x000D,  0x30CB,  0x315B,  0x317B,  0x3177,  0x3172,  0x318B,  0x313B,  0x30EB,  
        0x30CA,  0x30B6,  0x3127,  0x3168,  0x000A,  0x000D,  0x3155,  0x3167,  0x3145,  0x3181,  
        0x3173,  0x3204,  0x3207,  0x3228,  0x320B,  0x320F,  0x3221,  0x000A,  0x000D
      };
	const UChar* uSource;
	const UChar* uSourceLimit;
	const char* cSource;
	const char* cSourceLimit;
	UChar *uTargetLimit =NULL;
	UChar *uTarget;
	char *cTarget;
	const char *cTargetLimit;
	char *cBuf; 
	UChar *uBuf,*test;
    int32_t uBufSize = 120;
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv;

    cnv=ucnv_open("ISO_2022,locale=jp", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a iso-2022 converter: %s\n", u_errorName(errorCode));
        return;
    }

	uBuf =  (UChar*)malloc(uBufSize * sizeof(UChar)*5);
	cBuf =(char*)malloc(uBufSize * sizeof(char) * 5);
	uSource = &in[0];
	uSourceLimit=&in[sizeof(in)/2];
	cTarget = cBuf;
	cTargetLimit = cBuf +uBufSize*5;
	uTarget = uBuf;
	uTargetLimit = uBuf+ uBufSize*5;
	ucnv_fromUnicode( cnv , &cTarget, cTargetLimit,&uSource,uSourceLimit,NULL,TRUE, &errorCode);
    if(U_FAILURE(errorCode)){
        log_err("ucnv_fromUnicode conversion failed reason %s\n", u_errorName(errorCode));
        return;
    }
    cSource = cBuf;
	cSourceLimit =cTarget;
	test =uBuf;
	ucnv_toUnicode(cnv,&uTarget,uTargetLimit,&cSource,cSourceLimit,NULL,TRUE,&errorCode);
	if(U_FAILURE(errorCode)){
        log_err("ucnv_fromUnicode conversion failed reason %s\n", u_errorName(errorCode));
        return;
    }
    uSource = &in[0];
	while(*uSource){
		if(*test!=*uSource){
			log_err("Expected : \\u%04X \t Got: \\u%04X\n",*uSource,(int)*test) ;
		}
		*uSource++;
		*test++;
	}

	ucnv_close(cnv);
	free(uBuf);
	free(cBuf);
}

void
TestEBCDIC_STATEFUL() {
    /* test input */
    static const uint8_t in[]={
        0x61,
        0x1a,
        0x0f, 0x4b,
        0x42,
        0x40, 
        0x36,
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x002f,
        1, 0x0092,
        2, 0x002e,
        1, 0xff62,
        1, 0x0020, 
        1, 0x0096,
           
    };
    static const uint8_t in2[]={
        0x0f,
        0xa1,
        0x01
    };

    /* expected test results */
    static const uint32_t results2[]={
        /* number of bytes read, code point */
        2, 0x203E,
        1, 0x0001,
    };

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("ibm-930", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a EBCDIC_STATEFUL(ibm-930) converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "EBCDIC_STATEFUL(ibm-930)");
    ucnv_reset(cnv);
     /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    ucnv_reset(cnv);
    /*Test for the condition where source > sourcelimit after consuming the shift chracter */
    {
        static const uint8_t source1[]={0x0f};
        TestNextUCharError(cnv, (const char*)source1, (const char*)source1+sizeof(source1), U_INDEX_OUTOFBOUNDS_ERROR, "a character is truncated");
    }
    /*Test for the condition where there is an invalid character*/
    ucnv_reset(cnv);
    {
        static const uint8_t source2[]={0x0e, 0x7F, 0xFF};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_ZERO_ERROR, "an invalid character [EBCDIC STATEFUL]");
    }
    ucnv_reset(cnv);
    source=(const char*)in2;
    limit=(const char*)in2+sizeof(in2);
    TestNextUChar(cnv,source,limit,results2,"EBCDIC_STATEFUL(ibm-930),seq#2");
    ucnv_close(cnv);

}
void
TestLMBCS() {
    /* LMBCS-1 string */
    static const uint8_t pszLMBCS[]={
        0x61,
        0x01, 0x29,
        0x81,
        0xA0,
        0x0F, 0x27,
        0x0F, 0x91,
        0x14, 0x0a, 0x74,
        0x14, 0xF6, 0x02, 
        0x14, 0xd8, 0x4d, 0x14, 0xdc, 0x56, /* UTF-16 surrogate pair */
        0x10, 0x88, 0xA0,
    };

    /* Unicode UChar32 equivalents */
    static const UChar32 pszUnicode32[]={
        /* code point */
        0x00000061,
        0x00002013,
        0x000000FC,
        0x000000E1,
        0x00000007,
        0x00000091,
        0x00000a74,
        0x00000200,
        0x00023456, /* code point for surrogate pair */
        0x00005516
    };

/* Unicode UChar equivalents */
    static const UChar pszUnicode[]={
        /* code point */
        0x0061,
        0x2013,
        0x00FC,
        0x00E1,
        0x0007,
        0x0091,
        0x0a74,
        0x0200,
        0xD84D, /* low surrogate */
        0xDC56, /* high surrogate */
        0x5516
    };

/* expected test results */
    static const int offsets32[]={
        /* number of bytes read, code point */
        0, 
        1, 
        3, 
        4, 
        5, 
        7, 
        9, 
        12, 
        15, 
        21,
        24
    };

/* expected test results */
    static const int offsets[]={
        /* number of bytes read, code point */
        0, 
        1, 
        3, 
        4, 
        5, 
        7, 
        9, 
        12, 
        15, 
        18,
        21, 
        24 
    };


    UConverter *cnv; 

#define NAME_LMBCS_1 "LMBCS-1"
#define NAME_LMBCS_2 "LMBCS-2"


   /* Some basic open/close/property tests on some LMBCS converters */
    {

      char expected_subchars[] = {0x3F};   /* ANSI Question Mark */
      char new_subchars [] = {0x7F};       /* subst char used by SmartSuite..*/
      char get_subchars [1];
      const char * get_name;
      UConverter *cnv1;
      UConverter *cnv2;

      int8_t len = sizeof(get_subchars);

      UErrorCode errorCode=U_ZERO_ERROR;

      /* Open */
      cnv1=ucnv_open(NAME_LMBCS_1, &errorCode);
      if(U_FAILURE(errorCode)) {
         log_err("Unable to open a LMBCS-1 converter: %s\n", u_errorName(errorCode));
      }
      cnv2=ucnv_open(NAME_LMBCS_2, &errorCode);
      if(U_FAILURE(errorCode)) {
         log_err("Unable to open a LMBCS-2 converter: %s\n", u_errorName(errorCode));
      }

      /* Name */
      get_name = ucnv_getName (cnv1, &errorCode);
      if (strcmp(NAME_LMBCS_1,get_name)){
         log_err("Unexpected converter name: %s\n", get_name);
      }
      get_name = ucnv_getName (cnv2, &errorCode);
      if (strcmp(NAME_LMBCS_2,get_name)){
         log_err("Unexpected converter name: %s\n", get_name);
      }

      /* substitution chars */
      ucnv_getSubstChars (cnv1, get_subchars, &len, &errorCode);
      if(U_FAILURE(errorCode)) {
         log_err("Failure on get subst chars: %s\n", u_errorName(errorCode));
      }
      if (len!=1){
         log_err("Unexpected length of sub chars\n");
      }
      if (get_subchars[0] != expected_subchars[0]){
           log_err("Unexpected value of sub chars\n");
      }
      ucnv_setSubstChars (cnv2,new_subchars, len, &errorCode);
      if(U_FAILURE(errorCode)) {
         log_err("Failure on set subst chars: %s\n", u_errorName(errorCode));
      }
      ucnv_getSubstChars (cnv2, get_subchars, &len, &errorCode);
      if(U_FAILURE(errorCode)) {
         log_err("Failure on get subst chars: %s\n", u_errorName(errorCode));
      }
      if (len!=1){
         log_err("Unexpected length of sub chars\n");
      }
      if (get_subchars[0] != new_subchars[0]){
           log_err("Unexpected value of sub chars\n");
      }
      ucnv_close(cnv1);
      ucnv_close(cnv2);

    }

    /* LMBCS to Unicode - offsets */
    {
       UErrorCode errorCode=U_ZERO_ERROR;
       
       const uint8_t * pSource = pszLMBCS;
       const uint8_t * sourceLimit = pszLMBCS + sizeof(pszLMBCS);
       
       UChar Out [sizeof(pszUnicode)];
       UChar * pOut = Out;
       UChar * OutLimit = Out + sizeof(Out);

       int32_t off [sizeof(offsets)];

      /* last 'offset' in expected results is just the final size. 
         (Makes other tests easier). Compensate here: */

       off[(sizeof(offsets)/sizeof(offsets[0]))-1] = sizeof(pszLMBCS);



      cnv=ucnv_open("lmbcs", &errorCode); /* use generic name for LMBCS-1 */
      if(U_FAILURE(errorCode)) {
           log_err("Unable to open a LMBCS converter: %s\n", u_errorName(errorCode));
      }



      ucnv_toUnicode (cnv,
                      &pOut,
                      OutLimit,
                      (const char **)&pSource,
                      (const char *)sourceLimit, 
                      off,
                      TRUE,
                      &errorCode);


       if (memcmp(off,offsets,sizeof(offsets)))
       {
         log_err("LMBCS->Uni: Calculated offsets do not match expected results\n");
       }
       if (memcmp(Out,pszUnicode,sizeof(pszUnicode)))
       {
         log_err("LMBCS->Uni: Calculated codepoints do not match expected results\n");
       }
       ucnv_close(cnv);
    }
    {
   /* LMBCS to Unicode - getNextUChar */
      const char * sourceStart;
      const char *source=(const char *)pszLMBCS;
      const char *limit=(const char *)pszLMBCS+sizeof(pszLMBCS);
      const UChar32 *results= pszUnicode32;
      const int *off = offsets32;

      UErrorCode errorCode=U_ZERO_ERROR;
      uint32_t uniChar;

      cnv=ucnv_open("LMBCS-1", &errorCode);
      if(U_FAILURE(errorCode)) {
           log_err("Unable to open a LMBCS-1 converter: %s\n", u_errorName(errorCode));
      }
      else
      {

         while(source<limit) {
            sourceStart=source;
            uniChar=ucnv_getNextUChar(cnv, &source, source + (off[1] - off[0]), &errorCode);
            if(U_FAILURE(errorCode)) {
                  log_err("LMBCS-1 ucnv_getNextUChar() failed: %s\n", u_errorName(errorCode));
                  break;
            } else if(source-sourceStart != off[1] - off[0] || uniChar != *results) {
               log_err("LMBCS-1 ucnv_getNextUChar() result %lx from %d bytes, should have been %lx from %d bytes.\n",
                   uniChar, (source-sourceStart), *results, *off);
               break;
            }
            results++;
            off++;
         }
       }
       ucnv_close(cnv);
    }
    { /* test locale & optimization group operations: Unicode to LMBCS */
    
      UErrorCode errorCode=U_ZERO_ERROR;
      UConverter *cnv16he = ucnv_open("LMBCS-16,locale=he", &errorCode);
      UConverter *cnv16jp = ucnv_open("LMBCS-16,locale=ja_JP", &errorCode);
      UConverter *cnv01us = ucnv_open("LMBCS-1,locale=us_EN", &errorCode);
      UChar uniString [] = {0x0192}; /* Latin Small letter f with hook */
      const UChar * pUniOut = uniString;
      UChar * pUniIn = uniString;
      uint8_t lmbcsString [4];
      const uint8_t * pLMBCSOut = lmbcsString;
      uint8_t * pLMBCSIn = lmbcsString;

      /* 0192 (hook) converts to both group 3 & group 1. input locale should differentiate */
      ucnv_fromUnicode (cnv16he, 
                        (char **)&pLMBCSIn, (const char *)(pLMBCSIn + sizeof(lmbcsString)/sizeof(lmbcsString[0])), 
                        &pUniOut, pUniOut + sizeof(uniString)/sizeof(uniString[0]), 
                        NULL, 1, &errorCode);

      if (lmbcsString[0] != 0x3 || lmbcsString[1] != 0x83)
      {
         log_err("LMBCS-16,locale=he gives unexpected translation\n");
      }

      pLMBCSIn=lmbcsString;
      pUniOut = uniString;
      ucnv_fromUnicode (cnv01us, 
                        (char **)&pLMBCSIn, (const char *)(lmbcsString + sizeof(lmbcsString)/sizeof(lmbcsString[0])), 
                        &pUniOut, pUniOut + sizeof(uniString)/sizeof(uniString[0]),
                        NULL, 1, &errorCode);
      
      if (lmbcsString[0] != 0x9F)
      {
         log_err("LMBCS-1,locale=US gives unexpected translation\n");
      }

      /* single byte char from mbcs char set */
      lmbcsString[0] = 0xAE;  /* 1/2 width katakana letter small Yo */
      pLMBCSOut = lmbcsString;
      pUniIn = uniString;
      ucnv_toUnicode (cnv16jp, 
                        &pUniIn, pUniIn + 1,
                        (const char **)&pLMBCSOut, (const char *)(pLMBCSOut + 1), 
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCSOut != lmbcsString+1 || pUniIn != uniString+1 || uniString[0] != 0xFF6E)
      {
           log_err("Unexpected results from LMBCS-16 single byte char\n");
      }
      /* convert to group 1: should be 3 bytes */
      pLMBCSIn = lmbcsString;
      pUniOut = uniString;
      ucnv_fromUnicode (cnv01us, 
                        (char **)&pLMBCSIn, (const char *)(pLMBCSIn + 3), 
                        &pUniOut, pUniOut + 1,
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCSIn != lmbcsString+3 || pUniOut != uniString+1 
         || lmbcsString[0] != 0x10 || lmbcsString[1] != 0x10 || lmbcsString[2] != 0xAE)
      {
           log_err("Unexpected results to LMBCS-1 single byte mbcs char\n");
      }
      pLMBCSOut = lmbcsString;
      pUniIn = uniString;
      ucnv_toUnicode (cnv01us, 
                        &pUniIn, pUniIn + 1,
                        (const char **)&pLMBCSOut, (const char *)(pLMBCSOut + 3), 
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCSOut != lmbcsString+3 || pUniIn != uniString+1 || uniString[0] != 0xFF6E)
      {
           log_err("Unexpected results from LMBCS-1 single byte mbcs char\n");
      }
      pLMBCSIn = lmbcsString;
      pUniOut = uniString;
      ucnv_fromUnicode (cnv16jp, 
                        (char **)&pLMBCSIn, (const char *)(pLMBCSIn + 1), 
                        &pUniOut, pUniOut + 1,
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCSIn != lmbcsString+1 || pUniOut != uniString+1 || lmbcsString[0] != 0xAE)
      {
           log_err("Unexpected results to LMBCS-16 single byte mbcs char\n");
      }
      ucnv_close(cnv16he);
      ucnv_close(cnv16jp);
      ucnv_close(cnv01us);
    }
    {
       /* Small source buffer testing, LMBCS -> Unicode */

       UErrorCode errorCode=U_ZERO_ERROR;
       
       const uint8_t * pSource = pszLMBCS;
       const uint8_t * sourceLimit = pszLMBCS + sizeof(pszLMBCS);
       int codepointCount = 0;

       UChar Out [sizeof(pszUnicode)];
       UChar * pOut = Out;
       UChar * OutLimit = Out + sizeof(Out);


      cnv = ucnv_open(NAME_LMBCS_1, &errorCode);
      if(U_FAILURE(errorCode)) {
         log_err("Unable to open a LMBCS-1 converter: %s\n", u_errorName(errorCode));
      }


       while ((pSource < sourceLimit) && U_SUCCESS (errorCode))
       {
                  
         ucnv_toUnicode (cnv,
                      &pOut,
                      OutLimit,
                      (const char **)&pSource,
                      (const char *)(pSource+1), /* claim that this is a 1- byte buffer */
                      NULL,
                      FALSE,    /* FALSE means there might be more chars in the next buffer */
                      &errorCode);

         if (U_SUCCESS (errorCode))
         {
            if ((pSource - (const uint8_t *)pszLMBCS) == offsets [codepointCount+1])
            {
               /* we are on to the next code point: check value */
                           
               if (Out[0] != pszUnicode[codepointCount]){
                  log_err("LMBCS->Uni result %lx should have been %lx \n",
                   Out[0], pszUnicode[codepointCount]);
               }
            
               pOut = Out; /* reset for accumulating next code point */
               codepointCount++;
            }
         }
         else
         {
            log_err("Unexpected Error on toUnicode: %s\n", u_errorName(errorCode));
         }
       }
       {
         /* limits & surrogate error testing */
         UErrorCode errorCode=U_ZERO_ERROR;
       
         uint8_t LIn [sizeof(pszLMBCS)];
         const uint8_t * pLIn = LIn;
                
         char LOut [sizeof(pszLMBCS)];
         char * pLOut = LOut;
                
         UChar UOut [sizeof(pszUnicode)];
         UChar * pUOut = UOut;
         
         UChar UIn [sizeof(pszUnicode)];
         const UChar * pUIn = UIn;
         
         int32_t off [sizeof(offsets)];
         UChar32 uniChar;

          /* negative source request should always return U_ILLEGAL_ARGUMENT_ERROR */
         ucnv_fromUnicode(cnv, &pLOut,pLOut+1,&pUIn,pUIn-1,off,FALSE, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on negative source request to ucnv_fromUnicode: %s\n", u_errorName(errorCode));
         }
         errorCode=U_ZERO_ERROR;
         ucnv_toUnicode(cnv, &pUOut,pUOut+1,(const char **)&pLIn,(const char *)(pLIn-1),off,FALSE, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on negative source request to ucnv_toUnicode: %s\n", u_errorName(errorCode));
         }
         errorCode=U_ZERO_ERROR;
         
         uniChar = ucnv_getNextUChar(cnv, (const char **)&pLIn, (const char *)(pLIn-1), &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on negative source request to ucnv_getNextUChar: %s\n", u_errorName(errorCode));
         }
         errorCode=U_ZERO_ERROR;

         /* 0 byte source request - no error, no pointer movement */
         ucnv_toUnicode(cnv, &pUOut,pUOut+1,(const char **)&pLIn,(const char *)pLIn,off,FALSE, &errorCode);
         ucnv_fromUnicode(cnv, &pLOut,pLOut+1,&pUIn,pUIn,off,FALSE, &errorCode);
         if(U_FAILURE(errorCode)) {
            log_err("0 byte source request: unexpected error: %s\n", u_errorName(errorCode));
         }
         if ((pUOut != UOut) || (pUIn != UIn) || (pLOut != LOut) || (pLIn != LIn))
         {
              log_err("Unexpected pointer move in 0 byte source request \n");
         }
         /*0 byte source request - GetNextUChar : error & value == fffe or ffff */
         uniChar = ucnv_getNextUChar(cnv, (const char **)&pLIn, (const char *)pLIn, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on 0-byte source request to ucnv_getnextUChar: %s\n", u_errorName(errorCode));
         }
         if (((uint32_t)uniChar - 0xfffe) > 1) /* not 0xfffe<=uniChar<=0xffff */
         {
            log_err("Unexpected value on 0-byte source request to ucnv_getnextUChar \n");
         }
         errorCode = U_ZERO_ERROR;

         /* running out of target room : U_BUFFER_OVERFLOW_ERROR */

         pUIn = pszUnicode;
         ucnv_fromUnicode(cnv, &pLOut,pLOut+offsets[4],&pUIn,pUIn+sizeof(pszUnicode),off,FALSE, &errorCode);
         if (errorCode != U_BUFFER_OVERFLOW_ERROR || pLOut != LOut + offsets[4] || pUIn != pszUnicode+4 )
         {
            log_err("Unexpected results on out of target room to ucnv_fromUnicode\n");
         }

         errorCode = U_ZERO_ERROR;

         pLIn = pszLMBCS;
         ucnv_toUnicode(cnv, &pUOut,pUOut+4,(const char **)&pLIn,(const char *)(pLIn+sizeof(pszLMBCS)),off,FALSE, &errorCode);
         if (errorCode != U_BUFFER_OVERFLOW_ERROR || pUOut != UOut + 4 || pLIn != (const uint8_t *)pszLMBCS+offsets[4])
         {
            log_err("Unexpected results on out of target room to ucnv_toUnicode\n");
         }
         
         /* unpaired or chopped LMBCS surrogates */

         /* OK high surrogate, Low surrogate is chopped */
         LIn [0] = 0x14; 
         LIn [1] = 0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x14; 
         LIn [4] = 0xDC; 
         pLIn = LIn;
         errorCode = U_ZERO_ERROR;
         pUOut = UOut;
            
         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),(const char **)&pLIn,(const char *)(pLIn+5),off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || errorCode != U_TRUNCATED_CHAR_FOUND || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results on chopped low surrogate\n");
         }
         
         /* chopped at surrogate boundary */
         LIn [0] = 0x14; 
         LIn [1] = 0xD8; 
         LIn [2] = 0x01; 
         pLIn = LIn;
         errorCode = U_ZERO_ERROR;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),(const char **)&pLIn,(const char *)(pLIn+3),off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || U_FAILURE(errorCode) || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results on chopped at surrogate boundary \n");
         }

         /* unpaired surrogate plus valid Unichar */
         LIn [0] = 0x14; 
         LIn [1] = 0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x14; 
         LIn [4] = 0xC9; 
         LIn [5] = 0xD0;
         pLIn = LIn;
         errorCode = U_ZERO_ERROR;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),(const char **)&pLIn,(const char *)(pLIn+6),off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || UOut[1] != 0xC9D0 || U_FAILURE(errorCode) || pUOut != UOut + 2 || pLIn != LIn + 6)
         {
            log_err("Unexpected results after unpaired surrogate plus valid Unichar \n");
         }

      /* unpaired surrogate plus chopped Unichar */
         LIn [0] = 0x14; 
         LIn [1] = 0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x14; 
         LIn [4] = 0xC9; 
         
         pLIn = LIn;
         errorCode = U_ZERO_ERROR;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),(const char **)&pLIn,(const char *)(pLIn+5),off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || errorCode != U_TRUNCATED_CHAR_FOUND || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results after unpaired surrogate plus chopped Unichar \n");
         }

         /* unpaired surrogate plus valid non-Unichar */
         LIn [0] = 0x14; 
         LIn [1] = 0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x0F; 
         LIn [4] = 0x3B; 
         
         pLIn = LIn;
         errorCode = U_ZERO_ERROR;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),(const char **)&pLIn,(const char *)(pLIn+5),off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || UOut[1] != 0x1B || U_FAILURE(errorCode) || pUOut != UOut + 2 || pLIn != LIn + 5)
         {
            log_err("Unexpected results after unpaired surrogate plus valid non-Unichar\n");
         }

         /* unpaired surrogate plus chopped non-Unichar */
         LIn [0] = 0x14; 
         LIn [1] = 0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x0F; 
                  
         pLIn = LIn;
         errorCode = U_ZERO_ERROR;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),(const char **)&pLIn,(const char *)(pLIn+4),off,TRUE, &errorCode);

         if (UOut[0] != 0xD801 || errorCode != U_TRUNCATED_CHAR_FOUND || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results after unpaired surrogate plus chopped non-Unichar\n");
         }
       }
    }
   ucnv_close(cnv);  /* final cleanup */
}


void TestJitterbug255()
{
    const uint8_t testBytes[] = { 0x95, 0xcf, 0x8a, 0xb7, 0x0d, 0x0a, 0x00 };
    const uint8_t *testBuffer = testBytes;
    const uint8_t *testEnd = testBytes + sizeof(testBytes);
    UErrorCode status = U_ZERO_ERROR;
    UChar32 result;
    UConverter *cnv = 0;

    cnv = ucnv_open("shift-jis", &status); 
    if (U_FAILURE(status) || cnv == 0) {
        log_err("Failed to open the converter for SJIS.\n");
                return;
    }
    while (testBuffer != testEnd)
    {
        result = ucnv_getNextUChar (cnv, (const char **)&testBuffer, (const char *)testEnd , &status);
        if (U_FAILURE(status))
        {
            log_err("Failed to convert the next UChar for SJIS.\n");
            break;
        }
    }
    ucnv_close(cnv);
}

void TestEBCDICUS4XML()
{
    UChar unicodes_x[] = {0x0000, 0x0000, 0x0000, 0x0000};
    const UChar toUnicodeMaps_x[] = {0x000A, 0x000A, 0x000D, 0x0000};
    const char fromUnicodeMaps_x[] = {0x25, 0x25, 0x0D, 0x00};
    const char newLines_x[] = {0x25, 0x15, 0x0D, 0x00};
    char target_x[] = {0x00, 0x00, 0x00, 0x00};
    UChar *unicodes = unicodes_x;
    const UChar *toUnicodeMaps = toUnicodeMaps_x;
    char *target = target_x;
    const char* fromUnicodeMaps = fromUnicodeMaps_x, *newLines = newLines_x;
    UErrorCode status = U_ZERO_ERROR;
    UConverter *cnv = 0;

    cnv = ucnv_open("ebcdic-xml-us", &status);
    if (U_FAILURE(status) || cnv == 0) {
        log_err("Failed to open the converter for EBCDIC-XML-US.\n");
                return;
    }
    ucnv_toUnicode(cnv, &unicodes, unicodes+3, (const char**)&newLines, newLines+3, NULL, TRUE, &status);
    if (U_FAILURE(status) || memcmp(unicodes_x, toUnicodeMaps, sizeof(UChar)*3) != 0) {
        log_err("To Unicode conversion failed in EBCDICUS4XML test.\n");
    }
    ucnv_fromUnicode(cnv, &target, target+3, (const UChar**)&toUnicodeMaps, toUnicodeMaps+3, NULL, TRUE, &status);
    if (U_FAILURE(status) || memcmp(target_x, fromUnicodeMaps, sizeof(char)*3) != 0) {
        log_err("From Unicode conversion failed in EBCDICUS4XML test.\n");
    }
    ucnv_close(cnv);
}


#endif
