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
    log_verbose("\n{");
    while (i<len) log_verbose("%X", a[i++]);
    log_verbose("}\n");
}
void printUSeq(const UChar* a, int len)
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
void printUSeqErr(const UChar* a, int len)
{
    int i=0;
    fprintf(stderr, "\n{");
    while (i<len) fprintf(stderr, "%4X", a[i++]);
    fprintf(stderr,"}\n");
}
void 
TestNextUChar(UConverter* cnv, const char* source, const char* limit, const uint32_t results[], const char* message)
{
     const char* s0;
     char* s=(char*)source;
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
     char* s=(char*)source;
     UErrorCode errorCode=U_ZERO_ERROR;
     uint32_t c;
     c=ucnv_getNextUChar(cnv, &s, limit, &errorCode);
     if(errorCode != expected){
        log_err("FAIL: Expected:%s when %s-----Got:%s\n", myErrorName(expected), message, myErrorName(errorCode));
     }
     if(c != 0xFFFD){
        log_err("FAIL: Expected return value of 0xFFFD when %s-----Got %lx\n", message, c);
     }
     
}   
void TestInBufSizes(void)
{
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,1);
//#if 0
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,2);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,3);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,4);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,5);
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,6);
  TestNewConvertWithBufferSizes(1,1);
  TestNewConvertWithBufferSizes(2,3);
  TestNewConvertWithBufferSizes(3,2);
//#endif
}

void TestOutBufSizes(void)
{
//#if 0
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(1,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(2,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(3,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(4,NEW_MAX_BUFFER);
  TestNewConvertWithBufferSizes(5,NEW_MAX_BUFFER);
//#endif
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

UBool testConvertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    char    junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t    junokout[NEW_MAX_BUFFER]; /* FIX */
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

    for(i=0;i<NEW_MAX_BUFFER;i++)
        junkout[i] = (char)0xF0;
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
                  checkOffsets ? offs : NULL,
                  doFlush, /* flush if we're at the end of the input data */
                  &status);
    
      } while ( (status == U_INDEX_OUTOFBOUNDS_ERROR) || (sourceLimit < realSourceEnd) );
        
    if(U_FAILURE(status))
      {
        log_err("Problem tdoing fromUnicode, errcode %d %s\n", codepage, status, gNuConvTestName);
        return FALSE;
      }

    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-junkout);
    if(VERBOSITY)
    {
        char junk[9999];
        char offset_str[9999];
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
		printSeqErr(junkout, targ-junkout);
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
        printSeqErr((const unsigned char *)junkout, expectLen);
        printSeqErr((const unsigned char *)expect, expectLen);
        
        return FALSE;
    }
}

UBool testConvertToU( const char *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    UChar    junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t    junokout[NEW_MAX_BUFFER]; /* FIX */
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
                &src,
                srcLimit,
                checkOffsets ? offs : NULL,
                (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
                &status);

        /*        offs += (targ-oldTarg); */

      } while ( (status == U_INDEX_OUTOFBOUNDS_ERROR) || (srcLimit < realSourceEnd) ); /* while we just need another buffer */

    if(U_FAILURE(status))
    {
        log_err("Problem doing toUnicode, errcode %d %s\n", status, gNuConvTestName);
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
		printUSeq(junkout, expectlen);
        printUSeq(expect, expectlen); 
        return FALSE;
    }
}


void TestNewConvertWithBufferSizes(int32_t outsize, int32_t insize ) 
{
/** test chars #1 */
    /*  1 2 3  1Han 2Han 3Han .  */
    UChar    sampleText[] = 
     { 0x0031, 0x0032, 0x0033, 0x0000, 0x4e00, 0x4e8c, 0x4e09,  0x002E  };

    
    const char expectedUTF8[] = 
     { (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0xe4, (char)0xb8, (char)0x80, (char)0xe4, (char)0xba, (char)0x8c, (char)0xe4, (char)0xb8, (char)0x89, (char)0x2E };
    int32_t  toUTF8Offs[] = 
     { (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x06, (char)0x07};
    int32_t fmUTF8Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0007, 0x000a, 0x000d };
    
    /* Same as UTF8, but with ^[%B preceeding */
    const char expectedISO2022[] = 
     { (char)0x1b, (char)0x25, (char)0x42, (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0xe4, (char)0xb8, (char)0x80, (char)0xe4, (char)0xba, (char)0x8c, (char)0xe4, (char)0xb8, (char)0x89, (char)0x2E };
    int32_t  toISO2022Offs[]     = 
     { (char)0xff, (char)0xff, (char)0xff, (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x04,  
       (char)0x04, (char)0x05, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x06, (char)0x07 }; /* right? */
    int32_t fmISO2022Offs[] = 
     { 0x0003, 0x0004, 0x0005, 0x0006, 0x0007, 0x000a, 0x000d, 0x0010 }; /* is this right? */
   
    /*  1 2 3 0, <SO> h1 h2 h3 <SI> . EBCDIC_STATEFUL */
    const char expectedIBM930[] = 
     { (char)0xF1, (char)0xF2, (char)0xF3, (char)0x00, (char)0x0E, (char)0x45, (char)0x41, (char)0x45, (char)0x42, (char)0x45, (char)0x43, (char)0x0F, (char)0x4B };
    int32_t  toIBM930Offs[] = 
     { (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x07, (char)0x07, };
    int32_t fmIBM930Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0005, 0x0007, 0x0009, 0x000c};
    
    /* 1 2 3 0 h1 h2 h3 . MBCS*/
    const char expectedIBM943[] = 
     {  (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0x88, (char)0xea, (char)0x93, (char)0xf1, (char)0x8e, (char)0x4f, (char)0x2e };
    int32_t  toIBM943Offs    [] = 
     {  (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x07 };
    int32_t fmIBM943Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0006, 0x0008, 0x000a};
    
    /* 1 2 3 0 h1 h2 h3 . DBCS*/
    const char expectedIBM835[] = 
     {  (char)0xfe, (char)0xfe, (char)0xfe, (char)0xfe, (char)0xfe, (char)0xfe, (char)0xfe, (char)0xfe, (char)0x4c, (char)0x41, (char)0x4c, (char)0x48, (char)0x4c, (char)0x55, (char)0xfe, (char)0xfe};
    int32_t  toIBM835Offs    [] = 
     {  (char)0x00, (char)0x00, (char)0x01, (char)0x01, (char)0x02, (char)0x02, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x07, (char)0x07};
   
     /* 1 2 3 0 <?> <?> <?> . SBCS*/
    const char expectedIBM920[] = 
     {  (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0x1a, (char)0x1a, (char)0x1a, (char)0x2e };
    int32_t  toIBM920Offs    [] = 
     {  (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x05, (char)0x06, (char)0x07 };
   
    /* 1 2 3 0 <?> <?> <?> . SBCS*/
    const char expectedISO88593[] = 
     { (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0x1a, (char)0x1a, (char)0x1a, (char)0x2E };
    int32_t  toISO88593Offs[]     = 
     {(char) 0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x05, (char)0x06, (char)0x07};

    /* 1 2 3 0 <?> <?> <?> . LATIN_1*/
    const char expectedLATIN1[] = 
     { (char)0x31, (char)0x32, (char)0x33, (char)0x00, (char)0x1a, (char)0x1a, (char)0x1a, (char)0x2E };
    int32_t  toLATIN1Offs[]     = 
     {(char) 0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x05, (char)0x06, (char)0x07};
    
   
    /*  etc */
    const char expectedUTF16LE[] = 
     { (char)0x31, (char)0x00, (char)0x32, (char)0x00, (char)0x33, (char)0x00, (char)0x00, (char)0x00, (char)0x00, (char)0x4e, (char)0x8c, (char)0x4e, (char)0x09, (char)0x4e, (char)0x2e, (char)0x00 };
    int32_t      toUTF16LEOffs[]=  
     { (char)0x00, (char)0x00, (char)0x01, (char)0x01, (char)0x02, (char)0x02, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06, (char)0x06,  (char)0x07, (char)0x07};
    int32_t fmUTF16LEOffs[] = 
     { 0x0000, 0x0002, 0x0004, 0x0006, 0x0008, 0x000a, 0x000c,  0x000e }; 

    const char expectedUTF16BE[] = 
     { (char)0x00, (char)0x31, (char)0x00, (char)0x32, (char)0x00, (char)0x33, (char)0x00, (char)0x00, (char)0x4e, (char)0x00, (char)0x4e, (char)0x8c, (char)0x4e, (char)0x09, (char)0x00, (char)0x2e };
    int32_t      toUTF16BEOffs[]=  
     { (char)0x00, (char)0x00, (char)0x01, (char)0x01, (char)0x02, (char)0x02, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06, (char)0x06, (char)0x07, (char)0x07};
    int32_t fmUTF16BEOffs[] = 
     { 0x0000, 0x0002, 0x0004, 0x0006, 0x0008, 0x000a, 0x000c,  0x000e }; 
    
  
    


/** Test chars #2 **/

    /* Sahha [health],  slashed h's */
    const UChar malteseUChars[] = { 0x0053, 0x0061, 0x0127, 0x0127, 0x0061 };
    const char expectedMaltese913[] = { (char)0x53, (char)0x61, (char)0xB1, (char)0xB1, (char)0x61 };

    /* LMBCS */
    const UChar LMBCSUChars[]  = { 0x0027, 0x010A, 0x0000, 0x0127, 0x2666 };
    const char expectedLMBCS[] = { (char)0x27, (char)0x06, (char)0x04, (char)0x00, (char)0x01, (char)0x73, (char)0x01, (char)0x04 };
    int32_t toLMBCSOffs[]   = { (char)0x00, (char)0x01, (char)0x01, (char)0x02, (char)0x03, (char)0x03, (char)0x04, (char)0x04 };
    int32_t fmLMBCSOffs[]   = { 0x0000, 0x0001, 0x0003, 0x0004, 0x0006};
    /*********************************** START OF CODE finally *************/

  gInBufferSize = insize;
  gOutBufferSize = outsize;

  log_verbose("\n\n\nTesting conversions with InputBufferSize = %d, OutputBufferSize = %d\n", gInBufferSize, gOutBufferSize);

    
//#if 0
    /*UTF-8*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF8, sizeof(expectedUTF8), "UTF8", toUTF8Offs ))
        log_err("u-> UTF8 did not match.\n");
    /*ISO-2022*/
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedISO2022, sizeof(expectedISO2022), "iso-2022", toISO2022Offs ))
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
//#endif

//#if 0
    /*UTF-8*/
    if(!testConvertToU(expectedUTF8, sizeof(expectedUTF8),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf8", fmUTF8Offs ))
      log_err("utf8 -> u did not match\n");
    /*ISO-2022*/
    if(!testConvertToU(expectedISO2022, sizeof(expectedISO2022),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "iso-2022", fmISO2022Offs ))
      log_err("iso-2022  -> u  did not match"); 
    /*UTF16 LE*/
    if(!testConvertToU(expectedUTF16LE, sizeof(expectedUTF16LE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16le", fmUTF16LEOffs ))
      log_err("utf-16le -> u  did not match");
    /*UTF16 BE*/
    if(!testConvertToU(expectedUTF16BE, sizeof(expectedUTF16BE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16be", fmUTF16BEOffs ))
      log_err("utf-16be -> u  did not match");
    /*EBCDIC_STATEFUL*/
    if(!testConvertToU(expectedIBM930, sizeof(expectedIBM930),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ibm-930", fmIBM930Offs ))
      log_err("ibm-930  -> u  did not match");
    /*MBCS*/
    if(!testConvertToU(expectedIBM943, sizeof(expectedIBM943),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ibm-943", fmIBM943Offs ))
      log_err("ibm-943 -> u  did not match");

    if(!testConvertToU(expectedUTF16LE, sizeof(expectedUTF16LE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16le", fmUTF16LEOffs ))
      log_err("utf-16le -> u  did not match");

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
      log_err("LMBCS-1 -> u  did not match");
 
}  
     

void TestConverterTypesAndStarters()
{
    UConverter* myConverter[3];
    UErrorCode err = U_ZERO_ERROR;
    UBool mystarters[256];
    
    const UBool expectedKSCstarters[256] = {
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
        TRUE, TRUE, TRUE, TRUE, TRUE, TRUE};


  log_verbose("Testing KSC, ibm-930, ibm-878  for starters and their conversion types.");

    myConverter[0] = ucnv_open("ksc", &err);
    if (U_FAILURE(err))
      log_err("Failed to create an ibm-949 converter\n");
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
        0xf4, 0x84, 0x8c, 0xa1
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x61,
        2, 0,
        3, 0,
        4, 0,
        4, 0x104321
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
                
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        2, 0x61,
        2, 0xc0,
        2, 0x31, 
        2, 0xf4,
        2, 0xffcefe,
        
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
    //    static const uint8_t source2[]={};
    //    TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_TRUNCATED_CHAR_FOUND, "an invalid character");
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
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        2, 0x61,
        2, 0x31,
        2, 0x2e4e,
        2, 0x4e,
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
    ucnv_close(cnv);
    }
   */
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
        2, 0xd60e
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
    UConverter *cnv=ucnv_open("iso-2022", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a iso-2022 converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "iso-2022");

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

    const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
    UErrorCode errorCode=U_ZERO_ERROR;
    UConverter *cnv=ucnv_open("ibm-930", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a EBCDIC_STATEFUL(ibm-930) converter: %s\n", u_errorName(errorCode));
    }
    TestNextUChar(cnv, source, limit, results, "EBCDIC_STATEFUL(ibm-930)");
     /*Test the condition when source > sourceLimit*/
    TestNextUCharError(cnv, source, source, U_INDEX_OUTOFBOUNDS_ERROR, "sourceLimit < source");
    /*Test for the condition where source > sourcelimit after consuming the shift chracter */
    {
        static const uint8_t source1[]={0x0f};
        TestNextUCharError(cnv, (const char*)source1, (const char*)source1+sizeof(source1), U_INDEX_OUTOFBOUNDS_ERROR, "a character is truncated");
    }
    /*Test for the condition where there is an invalid character*/
    {
        static const uint8_t source2[]={0x0f, 0xa1, 0x01};
        TestNextUCharError(cnv, (const char*)source2, (const char*)source2+sizeof(source2), U_ZERO_ERROR, "an invalid character");
    }
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
    }

    /* LMBCS to Unicode - offsets */
    {
       UErrorCode errorCode=U_ZERO_ERROR;
       
       const char * pSource = pszLMBCS;
       const char * sourceLimit = pszLMBCS + sizeof(pszLMBCS);
       
       UChar Out [sizeof(pszUnicode)];
       UChar * pOut = Out;
       UChar * OutLimit = Out + sizeof(Out);

       int off [sizeof(offsets)];

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
		      &pSource,
		      sourceLimit, 
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
      const uint32_t *results= pszUnicode32;
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
      UChar * pUni = uniString;
      char lmbcsString [4];
      char * pLMBCS = lmbcsString;

      /* 0192 (hook) converts to both group 3 & group 1. input locale should differentiate */
      ucnv_fromUnicode (cnv16he, 
                        &pLMBCS, pLMBCS + sizeof(lmbcsString)/sizeof(lmbcsString[0]), 
                        &pUni, pUni + sizeof(uniString)/sizeof(uniString[0]), 
                        NULL, 1, &errorCode);

      if (lmbcsString[0] != (char)0x3 || lmbcsString[1] != (char)0x83)
      {
         log_err("LMBCS-16,locale=he gives unexpected translation\n");
      }
			 
	  pLMBCS=lmbcsString;
      pUni = uniString;
      ucnv_fromUnicode (cnv01us, 
                        &pLMBCS, lmbcsString + sizeof(lmbcsString)/sizeof(lmbcsString[0]), 
                        &pUni, pUni + sizeof(uniString)/sizeof(uniString[0]),
                        NULL, 1, &errorCode);
      
      if (lmbcsString[0] != (char)0x9F)
      {
         log_err("LMBCS-1,locale=US gives unexpected translation\n");
      }

      /* single byte char from mbcs char set */
      lmbcsString[0] = (char)0xAE;  /* 1/2 width katakana letter small Yo */
      pLMBCS = lmbcsString;
      pUni = uniString;
      ucnv_toUnicode (cnv16jp, 
                        &pUni, pUni + 1,
                        &pLMBCS, pLMBCS + 1, 
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCS != lmbcsString+1 || pUni != uniString+1 || uniString[0] != 0xFF6E)
      {
           log_err("Unexpected results from LMBCS-16 single byte char\n");
      }
      /* convert to group 1: should be 3 bytes */
      pLMBCS = lmbcsString;
      pUni = uniString;
      ucnv_fromUnicode (cnv01us, 
                        &pLMBCS, pLMBCS + 3, 
                        &pUni, pUni + 1,
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCS != lmbcsString+3 || pUni != uniString+1 
         || lmbcsString[0] != 0x10 || lmbcsString[1] != 0x10 || lmbcsString[2] != (char)0xAE)
      {
           log_err("Unexpected results to LMBCS-1 single byte mbcs char\n");
      }
      pLMBCS = lmbcsString;
      pUni = uniString;
      ucnv_toUnicode (cnv01us, 
                        &pUni, pUni + 1,
                        &pLMBCS, pLMBCS + 3, 
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCS != lmbcsString+3 || pUni != uniString+1 || uniString[0] != 0xFF6E)
      {
           log_err("Unexpected results from LMBCS-1 single byte mbcs char\n");
      }
      pLMBCS = lmbcsString;
      pUni = uniString;
      ucnv_fromUnicode (cnv16jp, 
                        &pLMBCS, pLMBCS + 1, 
                        &pUni, pUni + 1,
                        NULL, 1, &errorCode);
      if (U_FAILURE(errorCode) || pLMBCS != lmbcsString+1 || pUni != uniString+1 || lmbcsString[0] != (char)0xAE)
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
       
       const char * pSource = pszLMBCS;
       const char * sourceLimit = pszLMBCS + sizeof(pszLMBCS);
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
		      &pSource,
		      pSource+1, /* claim that this is a 1- byte buffer */
		      NULL,
		      FALSE,    /* FALSE means there might be more chars in the next buffer */
		      &errorCode);

         if (U_SUCCESS (errorCode))
	     {
            if ((pSource - pszLMBCS) == offsets [codepointCount+1])
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
       
         char LIn [sizeof(pszLMBCS)];
         const char * pLIn = LIn;
                
         char LOut [sizeof(pszLMBCS)];
         char * pLOut = LOut;
                
         UChar UOut [sizeof(pszUnicode)];
         UChar * pUOut = UOut;
         
         UChar UIn [sizeof(pszUnicode)];
         const UChar * pUIn = UIn;
         
         int off [sizeof(offsets)];
         UChar32 uniChar;

          /* negative source request should always return U_ILLEGAL_ARGUMENT_ERROR */
         ucnv_fromUnicode(cnv, &pLOut,pLOut+1,&pUIn,pUIn-1,off,FALSE, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on negative source request to ucnv_fromUnicode: %s\n", u_errorName(errorCode));
         }
         errorCode=U_ZERO_ERROR;
         ucnv_toUnicode(cnv, &pUOut,pUOut+1,&pLIn,pLIn-1,off,FALSE, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on negative source request to ucnv_toUnicode: %s\n", u_errorName(errorCode));
         }
         errorCode=U_ZERO_ERROR;
         
         uniChar = ucnv_getNextUChar(cnv, &pLIn, pLIn-1, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on negative source request to ucnv_getNextUChar: %s\n", u_errorName(errorCode));
         }
         errorCode=U_ZERO_ERROR;

         /* 0 byte source request - no error, no pointer movement */
         ucnv_toUnicode(cnv, &pUOut,pUOut+1,&pLIn,pLIn,off,FALSE, &errorCode);
         ucnv_fromUnicode(cnv, &pLOut,pLOut+1,&pUIn,pUIn,off,FALSE, &errorCode);
         if(U_FAILURE(errorCode)) {
            log_err("0 byte source request: unexpected error: %s\n", u_errorName(errorCode));
         }
         if ((pUOut != UOut) || (pUIn != UIn) || (pLOut != LOut) || (pLIn != LIn))
         {
              log_err("Unexpected pointer move in 0 byte source request \n");
         }
         /*0 byte source request - GetNextUChar : error & value == FFFD */
         uniChar = ucnv_getNextUChar(cnv, &pLIn, pLIn, &errorCode);
         if (errorCode != U_ILLEGAL_ARGUMENT_ERROR)
         {
            log_err("Unexpected Error on 0-byte source request to ucnv_getnextUChar: %s\n", u_errorName(errorCode));
         }
         if (uniChar != 0xFFFD) /* would like to use an exported define here */
         {
            log_err("Unexpected value on 0-byte source request to ucnv_getnextUChar \n");
         }
         errorCode = 0;

         /* running out of target room : U_INDEX_OUTOFBOUNDS_ERROR*/

         pUIn = pszUnicode;
         ucnv_fromUnicode(cnv, &pLOut,pLOut+offsets[4],&pUIn,pUIn+sizeof(pszUnicode),off,FALSE, &errorCode);
         if (errorCode != U_INDEX_OUTOFBOUNDS_ERROR || pLOut != LOut + offsets[4] || pUIn != pszUnicode+5 )
         {
            log_err("Unexpected results on out of target room to ucnv_fromUnicode\n");
         }

         errorCode = 0;

         pLIn = pszLMBCS;
         ucnv_toUnicode(cnv, &pUOut,pUOut+4,&pLIn,pLIn+sizeof(pszLMBCS),off,FALSE, &errorCode);
         if (errorCode != U_INDEX_OUTOFBOUNDS_ERROR || pUOut != UOut + 4 || pLIn != pszLMBCS+offsets[4])
         {
            log_err("Unexpected results on out of target room to ucnv_toUnicode\n");
         }
         
         /* unpaired or chopped LMBCS surrogates */

         /* OK high surrogate, Low surrogate is chopped */
         LIn [0] = 0x14; 
         LIn [1] = (char)0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x14; 
         LIn [4] = (char)0xDC; 
         pLIn = LIn;
         errorCode = 0;
         pUOut = UOut;
            
         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),&pLIn,pLIn+5,off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || errorCode != U_TRUNCATED_CHAR_FOUND || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results on chopped low surrogate\n");
         }
         
         /* chopped at surrogate boundary */
         LIn [0] = 0x14; 
         LIn [1] = (char)0xD8; 
         LIn [2] = 0x01; 
         pLIn = LIn;
         errorCode = 0;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),&pLIn,pLIn+3,off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || U_FAILURE(errorCode) || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results on chopped at surrogate boundary \n");
         }

         /* unpaired surrogate plus valid Unichar */
         LIn [0] = 0x14; 
         LIn [1] = (char)0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x14; 
         LIn [4] = (char)0xC9; 
         LIn [5] = (char)0xD0;
         pLIn = LIn;
         errorCode = 0;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),&pLIn,pLIn+6,off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || UOut[1] != 0xC9D0 || U_FAILURE(errorCode) || pUOut != UOut + 2 || pLIn != LIn + 6)
         {
            log_err("Unexpected results after unpaired surrogate plus valid Unichar \n");
         }

      /* unpaired surrogate plus chopped Unichar */
         LIn [0] = 0x14; 
         LIn [1] = (char)0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x14; 
         LIn [4] = (char)0xC9; 
         
         pLIn = LIn;
         errorCode = 0;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),&pLIn,pLIn+5,off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || errorCode != U_TRUNCATED_CHAR_FOUND || pUOut != UOut + 1 || pLIn != LIn + 3)
         {
            log_err("Unexpected results after unpaired surrogate plus chopped Unichar \n");
         }

         /* unpaired surrogate plus valid non-Unichar */
         LIn [0] = 0x14; 
         LIn [1] = (char)0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x0F; 
         LIn [4] = 0x3B; 
         
         pLIn = LIn;
         errorCode = 0;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),&pLIn,pLIn+5,off,TRUE, &errorCode);
         if (UOut[0] != 0xD801 || UOut[1] != 0x1B || U_FAILURE(errorCode) || pUOut != UOut + 2 || pLIn != LIn + 5)
         {
            log_err("Unexpected results after unpaired surrogate plus valid non-Unichar\n");
         }

         /* unpaired surrogate plus chopped non-Unichar */
         LIn [0] = 0x14; 
         LIn [1] = (char)0xD8; 
         LIn [2] = 0x01; 
         LIn [3] = 0x0F; 
                  
         pLIn = LIn;
         errorCode = 0;
         pUOut = UOut;

         ucnv_toUnicode(cnv, &pUOut,pUOut+sizeof(UOut),&pLIn,pLIn+4,off,TRUE, &errorCode);

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
    const char testBytes[] = { (char)0x95, (char)0xcf, (char)0x8a, 
                               (char)0xb7, (char)0x0d, (char)0x0a, 0x0000 };
    const char *testBuffer = testBytes, *testEnd = testBytes+strlen(testBytes)+1;
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
        result = ucnv_getNextUChar (cnv, (const char **)&testBuffer, testEnd , &status);
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


