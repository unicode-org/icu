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

void TestNewConvertWithBufferSizes(int32_t osize, int32_t isize) ;
void TestConverterTypesAndStarters(void);
void TestAmbiguous(void);
void TestUTF8(void);
void TestLMBCS(void);
void TestJitterbug255(void);

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

void TestInBufSizes(void)
{
  TestNewConvertWithBufferSizes(NEW_MAX_BUFFER,1);
#if 0
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
#if 0
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
   addTest(root, &TestLMBCS, "tsconv/nucnvtst/TestLMBCS");
   addTest(root, &TestJitterbug255, "tsconv/nucnvtst/TestJitterbug255");
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

bool_t testConvertFromU( const UChar *source, int sourceLen,  const char *expect, int expectLen, 
                const char *codepage, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    char    junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t    junokout[NEW_MAX_BUFFER]; /* FIX */
    const UChar *src;
    char *end;
    char *targ;
    int32_t *offs;
    int i;
    int32_t   realBufferSize;
    char *realBufferEnd;
    const UChar *realSourceEnd;
    const UChar *sourceLimit;
    bool_t checkOffsets = TRUE;
    bool_t doFlush;

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
        printSeq(expect, expectLen);
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
        return FALSE;
    }

    if (checkOffsets && (expectOffsets != 0) )
    {
        log_verbose("comparing %d offsets..\n", targ-junkout);
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t) ))
            log_err("did not get the expected offsets. %s", gNuConvTestName);
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
        printSeqErr(junkout, expectLen);
        printSeqErr(expect, expectLen);
        
        return FALSE;
    }
}

bool_t testConvertToU( const char *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    UChar    junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t    junokout[NEW_MAX_BUFFER]; /* FIX */
    const char *src;
    const char *realSourceEnd;
    const char *srcLimit;
    UChar *targ;
    UChar *end;
    int32_t *offs;
    int i;
    bool_t   checkOffsets = TRUE;
    
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
                (bool_t)(srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
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
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t)))
            log_err("did not get the expected offsets. %s",gNuConvTestName);
    }

    if(!memcmp(junkout, expect, expectlen*2))
    {
        log_verbose("Matches!\n");
        return TRUE;
    }
    else
    {    
        log_err("String does not match. %s\n", gNuConvTestName);
        printUSeq(expect, expectlen); 
        return FALSE;
    }
}


void TestNewConvertWithBufferSizes(int32_t outsize, int32_t insize ) 
{
/** test chars #1 */
    /*  1 2 3  1Han 2Han 3Han .  */
    UChar    sampleText[] = 
     { 0x0031, 0x0032, 0x0033, 0x4e00, 0x4e8c, 0x4e09, 0x002E     };

    int32_t fmUTF8Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0006, 0x0009, 0x000c };

    int32_t fmISO2022Offs[] = 
     { 0x03, 0x04, 0x05, 0x06, 0x09, 0x0c, 0x0f }; /* is this right? */

    int32_t fmIBM930Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0004, 0x0006, 0x0008, 0x000b,    };

    int32_t fmIBM943Offs[] = 
     { 0x0000, 0x0001, 0x0002, 0x0003, 0x0005, 0x0007, 0x0009,  };

    int32_t fmUTF16LEOffs[] = 
     { 0x0000, 0x0002, 0x0004, 0x0006, 0x0008, 0x000a, 0x000c,  }; 
    
    const char expectedUTF8[] = 
     { (char)0x31, (char)0x32, (char)0x33, (char)0xe4, (char)0xb8, (char)0x80, (char)0xe4, (char)0xba, (char)0x8c, (char)0xe4, (char)0xb8, (char)0x89, (char)0x2E };

    int32_t  toUTF8Offs[] = 
     { (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x05, (char)0x06   };

    /* Same as UTF8, but with ^[%B preceeding */
    const char expectedISO2022[] = 
     { (char)0x1b, (char)0x25, (char)0x42, (char)0x31, (char)0x32, (char)0x33, (char)0xe4, (char)0xb8, (char)0x80, (char)0xe4, (char)0xba, (char)0x8c, (char)0xe4, (char)0xb8, (char)0x89, (char)0x2E };

    int32_t  toISO2022Offs[]     = 
     { (char)0xff, (char)0xff, (char)0xff, (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x03, (char)0x03, 
       (char)0x04, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x05, (char)0x06 }; /* right? */
    
    /*  1 2 3 <SO> h1 h2 h3 <SI> . */
    const char expectedIBM930[] = 
     { (char)0xF1, (char)0xF2, (char)0xF3, (char)0x0E, (char)0x45, (char)0x41, (char)0x45, (char)0x42, (char)0x45, (char)0x43, (char)0x0F, (char)0x4B };

    int32_t  toIBM930Offs[] = 
     { (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06, (char)0x06, };

    /* 1 2 3 <?> <?> <?> . */
    const char expectedISO88593[] = 
     { (char)0x31, (char)0x32, (char)0x33, (char)0x1a, (char)0x1a, (char)0x1a, (char)0x2E };

    int32_t  toISO88593Offs[]     = 
     {(char) 0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x04, (char)0x05, (char)0x06, };

    /* 1 2 3 h1 h2 h3 . */
    const char expectedIBM943[] = 
     {  (char)0x31, (char)0x32, (char)0x33, (char)0x88, (char)0xea, (char)0x93, (char)0xf1, (char)0x8e, (char)0x4f, (char)0x2e };

    int32_t  toIBM943Offs    [] = 
     {  (char)0x00, (char)0x01, (char)0x02, (char)0x03, (char)0x03, (char)0x04, (char)0x04, (char)0x05, (char)0x05, (char)0x06,  };

    /*  etc */
    const char expectedUTF16LE[] = 
     { (char)0x31, (char)0x00, (char)0x32, (char)0x00, (char)0x33, (char)0x00, (char)0x00, (char)0x4e, (char)0x8c, (char)0x4e, (char)0x09, (char)0x4e, (char)0x2e, (char)0x00 };
    int32_t      toUTF16LEOffs[]=  
     { (char)0x00, (char)0x00, (char)0x02, (char)0x02, (char)0x04, (char)0x04, (char)0x06, (char)0x06, (char)0x08, (char)0x08, (char)0x0a, (char)0x0a, (char)0x0c, (char)0x0c, };


/** Test chars #2  NOT USED YET**/

    /* Sahha [health],  slashed h's */
    const UChar malteseUChars[] = { 0x0053, 0x0061, 0x0127, 0x0127, 0x0061 };
    const char expectedMaltese913[] = { (char)0x53, (char)0x61, (char)0xB1, (char)0xB1, (char)0x61 };
    /*********************************** START OF CODE finally *************/

  gInBufferSize = insize;
  gOutBufferSize = outsize;

  log_verbose("\n\n\nTesting conversions with InputBufferSize = %d, OutputBufferSize = %d\n", gInBufferSize, gOutBufferSize);

    
#if 0
    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF8, sizeof(expectedUTF8), "UTF8", toUTF8Offs ))
        log_err("u-> UTF8 did not match.\n");

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedISO2022, sizeof(expectedISO2022), "iso-2022", toISO2022Offs ))
        log_err("u-> iso-2022 did not match.\n");

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedIBM930, sizeof(expectedIBM930), "ibm-930", toIBM930Offs ))
        log_err("u-> ibm-930 did not match.\n");

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedISO88593, sizeof(expectedISO88593), "iso-8859-3", toISO88593Offs ))
        log_err("u-> iso-8859-3 did not match.\n");

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedIBM943, sizeof(expectedIBM943), "ibm-943", toIBM943Offs ))
        log_err("u-> ibm-943 [UCNV_MBCS] not match.\n");

    if(!testConvertFromU(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expectedUTF16LE, sizeof(expectedUTF16LE), "utf-16le", toUTF16LEOffs ))
        log_err("u-> utf-16le did not match.\n");

/****/
#endif

#if 0
    if(!testConvertToU(expectedUTF8, sizeof(expectedUTF8),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf8", fmUTF8Offs ))
      log_err("utf8 -> u did not match\n");

    if(!testConvertToU(expectedISO2022, sizeof(expectedISO2022),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "iso-2022", fmISO2022Offs ))
    
        log_err("iso-2022  -> u  did not match");

#endif

#if 0
    if(!testConvertToU(expectedIBM930, sizeof(expectedIBM930),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ibm-930", fmIBM930Offs ))
      log_err("ibm-930  -> u  did not match");

    if(!testConvertToU(expectedIBM943, sizeof(expectedIBM943),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "ibm-943", fmIBM943Offs ))
      log_err("ibm-943 -> u  did not match");

    if(!testConvertToU(expectedUTF16LE, sizeof(expectedUTF16LE),
               sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf-16le", fmUTF16LEOffs ))
      log_err("utf-16le -> u  did not match");
#endif

    if(!testConvertToU(expectedMaltese913, sizeof(expectedMaltese913),
               malteseUChars, sizeof(malteseUChars)/sizeof(malteseUChars[0]), "latin3", NULL))
      log_err("latin3[813] -> u did not match\n");

    if(!testConvertFromU(malteseUChars, sizeof(malteseUChars)/sizeof(malteseUChars[0]),
            expectedMaltese913, sizeof(expectedMaltese913), "iso-8859-3", NULL ))
        log_err("u-> latin3[813] did not match.\n");

}

void TestConverterTypesAndStarters()
{
    UConverter* myConverter[3];
    UErrorCode err = U_ZERO_ERROR;
    bool_t mystarters[256];
    
    const bool_t expectedKSCstarters[256] = {
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

    const char *s=(const char *)in, *s0, *limit=(const char *)in+sizeof(in);
    const uint32_t *r=results;

    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t c;

    UConverter *cnv=ucnv_open("UTF-8", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a UTF-8 converter: %s\n", u_errorName(errorCode));
    }

    while(s<limit) {
        s0=s;
        c=ucnv_getNextUChar(cnv, &s, limit, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("UTF-8 ucnv_getNextUChar() failed: %s\n", u_errorName(errorCode));
            break;
        } else if((uint32_t)(s-s0)!=*r || c!=(UChar32)*(r+1)) {
            log_err("UTF-8 ucnv_getNextUChar() result %lx from %d bytes, should have been %lx from %d bytes.\n",
                c, (s-s0), *(r+1), *r);
            break;
        }
        r+=2;
    }

    ucnv_close(cnv);
}

void
TestLMBCS() {
    /* test input */
    static const uint8_t in[]={
        0x61,
        0x01, 0x29,
        0x81,
        0xA0,
        0x0F, 0x27,
        0x0F, 0x91,
        0x14, 0x0a, 0x74,
        0x14, 0xF6, 0x02, 
        0x10, 0x88, 0xA0,
        0x14, 0xd8, 0x4d, /* single UTF-16 high (first) surrogate */
        0x14, 0x01, 0x09, /* followed by a Unicode c^ */
        0x14, 0xd8, 0x4d, 0x14, 0xdc, 0x56 /* UTF-16 surrogate pair */
    };

    /* expected test results */
    static const uint32_t results[]={
        /* number of bytes read, code point */
        1, 0x0061,
        2, 0x2013,
        1, 0x00FC,
        1, 0x00E1,
        2, 0x0007,
        2, 0x0091,
        3, 0x0a74,
        3, 0x0200,
        3, 0x5516,
        3, 0xd84d,
        3, 0x0109,
        6, 0x23456 /* code point for above surrogate pair */
    };

    const char *s=(const char *)in, *s0, *limit=(const char *)in+sizeof(in);
    const uint32_t *r=results;

    UErrorCode errorCode=U_ZERO_ERROR;
    uint32_t c;

    UConverter *cnv=ucnv_open("LMBCS-1", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("Unable to open a LMBCS-1 converter: %s\n", u_errorName(errorCode));
    }
    else
    {

      while(s<limit) {
         s0=s;
         c=ucnv_getNextUChar(cnv, &s, limit, &errorCode);
         if(U_FAILURE(errorCode)) {
               log_err("LMBCS-1 ucnv_getNextUChar() failed: %s\n", u_errorName(errorCode));
               break;
         } else if((uint32_t)(s-s0)!=*r || c!=(UChar32)*(r+1)) {
               log_err("LMBCS-1 ucnv_getNextUChar() result %lx from %d bytes, should have been %lx from %d bytes.\n",
                   c, (s-s0), *(r+1), *r);
               break;
         }
         r+=2;
      }

      ucnv_close(cnv);
    }
}


void TestJitterbug255()
{
    const char testBytes[] = { (char)0x95, (char)0xcf, (char)0x8a, 
                               (char)0xb7, (char)0x0d, (char)0x0a, 0x0000 };
    char *testBuffer = (char*)testBytes, *testEnd = (char*)testBytes+strlen(testBytes)+1;
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
        result = ucnv_getNextUChar (cnv, &testBuffer, testEnd , &status);
        if (U_FAILURE(status))
        {
            log_err("Failed to convert the next UChar for SJIS.\n");
            break;
        }
    }
    ucnv_close(cnv);
}
