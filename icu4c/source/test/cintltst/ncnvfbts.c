/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ***************************************************************************/
/*******************************************************************************
*
* File NCNVCBTS
*
* Modification History:
*      Name              Date                  Description            
* Madhu Katragadda    06/23/2000     Tests for Conveter FallBack API and Functionality
**********************************************************************************
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
#include "ncnvfbts.h"

#define NEW_MAX_BUFFER 999


#define nct_min(x,y)  ((x<y) ? x : y)

static int32_t  gInBufferSize = 0;
static int32_t  gOutBufferSize = 0;
static char     gNuConvTestName[1024];

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
void TestConverterFallBack(void)
{
   TestConvertFallBackWithBufferSizes(10,10);
   TestConvertFallBackWithBufferSizes(2,3);
   TestConvertFallBackWithBufferSizes(3,2);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,1);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,2);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,3);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,4);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,5);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,6);
   TestConvertFallBackWithBufferSizes(1,NEW_MAX_BUFFER);
   TestConvertFallBackWithBufferSizes(2,NEW_MAX_BUFFER);
   TestConvertFallBackWithBufferSizes(3,NEW_MAX_BUFFER);
   TestConvertFallBackWithBufferSizes(4,NEW_MAX_BUFFER);
   TestConvertFallBackWithBufferSizes(5,NEW_MAX_BUFFER);
   TestConvertFallBackWithBufferSizes(NEW_MAX_BUFFER,NEW_MAX_BUFFER);

}





void addTestConverterFallBack(TestNode** root)
{
   addTest(root, &TestConverterFallBack, "tsconv/ncnvfbts/TestConverterFallBack");
 
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


UBool testConvertFromUnicode(const UChar *source, int sourceLen,  const uint8_t *expect, int expectLen, 
                const char *codepage, UBool fallback, int32_t *expectOffsets)
{


    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    uint8_t junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t junokout[NEW_MAX_BUFFER]; /* FIX */
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
    UBool action=FALSE;
    uint8_t *p;


    for(i=0;i<NEW_MAX_BUFFER;i++)
        junkout[i] = 0xF0;
    for(i=0;i<NEW_MAX_BUFFER;i++)
        junokout[i] = 0xFF;
    setNuConvTestName(codepage, "FROM");

    log_verbose("\nTesting========= %s  FROM \n  inputbuffer= %d   outputbuffer= %d\n", codepage, gInBufferSize, 
            gOutBufferSize);

    conv = ucnv_open(codepage, &status);
    if(U_FAILURE(status))
    {
        log_err("Couldn't open converter %s\n",codepage);
        return FALSE;
    }

    log_verbose("Converter opened..\n");
    /*----setting the callback routine----*/
    ucnv_setFallback (conv, fallback);
    action = ucnv_usesFallback(conv);
    if(action != fallback){
        log_err("FAIL: Error is setting fallback. Errocode=%s\n", myErrorName(status));
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
                  (const char *)end,
                  &src,
                  sourceLimit,
                  checkOffsets ? offs : NULL,
                  doFlush, /* flush if we're at the end of the input data */
                  &status);

    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (sourceLimit < realSourceEnd) );

    if(U_FAILURE(status))
    {
        log_err("Problem doing toUnicode, errcode %d %s\n", myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-junkout);
    if(VERBOSITY)
    {
        char junk[9999];
        char offset_str[9999];

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
        log_verbose("Expected %d chars out, got %d %s\n", expectLen, targ-junkout, gNuConvTestName);
        printSeqErr(junkout, targ-junkout);
        printSeqErr(expect, expectLen);
        return FALSE;
    }

    if (checkOffsets && (expectOffsets != 0) )
    {
        log_verbose("\ncomparing %d offsets..\n", targ-junkout);
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t) )){
            log_err("\ndid not get the expected offsets while %s \n", gNuConvTestName);
            log_err("Got  : ");
            printSeqErr(junkout, targ-junkout);
            for(p=junkout;p<targ;p++)
                log_err("%d, ", junokout[p-junkout]); 
            log_err("\nExpected: ");
            for(i=0; i<(targ-junkout); i++)
                log_err("%d,", expectOffsets[i]);
        }
    }

    log_verbose("\n\ncomparing..\n");
    if(!memcmp(junkout, expect, expectLen))
    {
        log_verbose("Matches!\n");
        return TRUE;
    }
    else
    {
        log_err("String does not match. %s\n", gNuConvTestName);
        log_verbose("String does not match. %s\n", gNuConvTestName);
        printSeqErr(junkout, expectLen);
        printSeqErr(expect, expectLen);
        return FALSE;
    }
}

UBool testConvertToUnicode( const uint8_t *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, UBool fallback, int32_t *expectOffsets)
{
    UErrorCode status = U_ZERO_ERROR;
    UConverter *conv = 0;
    UChar   junkout[NEW_MAX_BUFFER]; /* FIX */
    int32_t junokout[NEW_MAX_BUFFER]; /* FIX */
    const uint8_t *src;
    const uint8_t *realSourceEnd;
    const uint8_t *srcLimit;
    UChar *targ;
    UChar *end;
    int32_t *offs;
    int i;
    UBool   checkOffsets = TRUE;
    char junk[9999];
    char offset_str[9999];
    UChar *p;
    UBool action;

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
    /*----setting the fallback routine----*/
    ucnv_setFallback (conv, fallback);
    action = ucnv_usesFallback(conv);
    if(action != fallback){
        log_err("FAIL: Error is setting fallback. Errocode=%s\n", myErrorName(status));
    }
    /*-------------------------------------*/
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



        status = U_ZERO_ERROR;

        ucnv_toUnicode (conv,
                &targ,
                end,
                (const char **)&src,
                (const char *)srcLimit,
                checkOffsets ? offs : NULL,
                (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
                &status);
    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (srcLimit < realSourceEnd) ); /* while we just need another buffer */


    if(U_FAILURE(status))
    {
        log_err("Problem doing toUnicode, errcode %s %s\n", myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done. %d bytes -> %d chars.\nResult :",
        sourcelen, targ-junkout);
    if(VERBOSITY)
    {

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
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t)))
        {
            log_err("\n\ndid not get the expected offsets while %s \n", gNuConvTestName);
            log_err("\nGot  : ");
            for(p=junkout;p<targ;p++)
                log_err("%d, ", junokout[p-junkout]); 
            log_err("\nExpected: ");
            for(i=0; i<(targ-junkout); i++)
                log_err("%d,", expectOffsets[i]);
            log_err("");
            for(i=0; i<(targ-junkout); i++)
                log_err("0x%04X,", junkout[i]);
            log_err("");
            for(i=0; i<(src-source); i++)
                log_err("0x%04X,", (unsigned char)source[i]);
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
        printUSeqErr(junkout, expectlen);
        printf("\n");
        printUSeqErr(expect, expectlen);
        return FALSE;
    }
}



void TestConvertFallBackWithBufferSizes(int32_t outsize, int32_t insize ) 
{

    UChar    SBCSText[] = 
     { 0x0021, 0xFF01, 0x0022, 0xFF02, 0x0023, 0xFF03, 0x003A, 0xFF1A, 0x003B, 0xFF1B, 0x003C, 0xFF1C };
     /* 21, ?, 22, ?, 23, ?, 3a, ?, 3b, ?, 3c, ? SBCS*/
    const uint8_t expectedNative[] = 
     {  0x21, 0x21, 0x22, 0x22, 0x23, 0x23, 0x3a, 0x3a, 0x3b, 0x3b, 0x3c, 0x3c};
    UChar retrievedSBCSText[]=
       { 0x0021, 0x0021, 0x0022, 0x0022, 0x0023, 0x0023, 0x003A, 0x003A, 0x003B, 0x003B, 0x003C, 0x003C };
    int32_t  toNativeOffs    [] = 
     {  0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b};
    int32_t fromNativeoffs []  = 
    {  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
   
    
    UChar    DBCSText[] = 
     { 0x00a1, 0x00ad, 0x2010, 0x00b7, 0x30fb};
    const uint8_t expectedIBM1362[] = 
     {  0xa2, 0xae, 0xa1, 0xa9, 0xa1, 0xa9, 0xa1, 0xa4, 0xa1, 0xa4};
    UChar retrievedDBCSText[]=
        { 0x00a1, 0x2010, 0x2010, 0x30fb, 0x30fb };
    int32_t  toIBM1362Offs    [] = 
        {  0x00, 0x00, 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04};
    int32_t fromIBM1362offs []  = 
    {  0, 2, 4, 6, 8};


    UChar    MBCSText[] = 
     { 0x0001, 0x263a, 0x2013, 0x2014, 0x263b, 0x0002};
    const uint8_t expectedIBM1370[] = 
     {  0x01, 0x01, 0xa1, 0x56, 0xa1, 0x56, 0x02, 0x02};
    UChar retrievedMBCSText[]=
       { 0x0001, 0x0001, 0x2014, 0x2014, 0x0002, 0x0002};
    int32_t  toIBM1370Offs    [] = 
     {  0x00, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x05};
    int32_t fromIBM1370offs []  = 
    {  0, 1, 2, 4, 6, 7};

    UChar    MBCSText1363[] = 
     { 0x0005, 0xffe8, 0x0007, 0x2022, 0x005c, 0x00b7, 0x3016, 0x30fb, 0x9a36};
    const uint8_t expectedIBM1363[] = 
     {  0x05, 0x05, 0x07, 0x07, 0x7f, 0xa1, 0xa4, 0xa1, 0xe0, 0xa1, 0xa4, 0xf5, 0xe2};
    UChar retrievedMBCSText1363[]=
       { 0x0005, 0x0005, 0x0007, 0x0007, 0x001a, 0x30fb, 0x25a1, 0x30fb, 0x9a36};
    int32_t  toIBM1363Offs    [] = 
     {  0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07, 0x07, 0x08, 0x08};
    int32_t fromIBM1363offs []  = 
    {  0, 1, 2, 3, 4, 5, 7, 9, 11};


    
    const char* nativeCodePage[]={
        /*NLCS Mapping*/
        "ibm-367",
        "ibm-1051",
        "ibm-1089",
        "ibm-1250",
        "ibm-1251",
        "ibm-1253",
        "ibm-1254",
        "ibm-1255",
        "ibm-1256",
        "ibm-1257",
        "ibm-1258",
        "ibm-1275",
        "ibm-1276"
    };

    int32_t i=0;
    gInBufferSize = insize;
    gOutBufferSize = outsize;

    for(i=0; i<sizeof(nativeCodePage)/sizeof(nativeCodePage[0]); i++){
        log_verbose("Testing %s\n", nativeCodePage[i]);
        if(!testConvertFromUnicode(SBCSText, sizeof(SBCSText)/sizeof(SBCSText[0]),
            expectedNative, sizeof(expectedNative), nativeCodePage[i], TRUE, toNativeOffs ))
            log_err("u-> %s(SBCS) with FallBack did not match.\n", nativeCodePage[i]);

        if(!testConvertToUnicode(expectedNative, sizeof(expectedNative), 
            retrievedSBCSText, sizeof(retrievedSBCSText)/sizeof(retrievedSBCSText[0]), nativeCodePage[i], TRUE, fromNativeoffs ))
            log_err("%s->u(SBCS) with Fallback did not match.\n", nativeCodePage[i]);
    }
    
    /*DBCS*/
    if(!testConvertFromUnicode(DBCSText, sizeof(DBCSText)/sizeof(DBCSText[0]),
        expectedIBM1362, sizeof(expectedIBM1362), "ibm-1362", TRUE, toIBM1362Offs ))
       log_err("u-> ibm-1362(DBCS) with FallBack did not match.\n");

    if(!testConvertToUnicode(expectedIBM1362, sizeof(expectedIBM1362), 
        retrievedDBCSText, sizeof(retrievedDBCSText)/sizeof(retrievedDBCSText[0]),"ibm-1362", TRUE, fromIBM1362offs ))
        log_err("ibm-1362->u(DBCS) with Fallback did not match.\n");

  
    /*MBCS*/
    if(!testConvertFromUnicode(MBCSText, sizeof(MBCSText)/sizeof(MBCSText[0]),
        expectedIBM1370, sizeof(expectedIBM1370), "ibm-1370", TRUE, toIBM1370Offs ))
       log_err("u-> ibm-1370(MBCS) with FallBack did not match.\n");

    if(!testConvertToUnicode(expectedIBM1370, sizeof(expectedIBM1370), 
        retrievedMBCSText, sizeof(retrievedMBCSText)/sizeof(retrievedMBCSText[0]),"ibm-1370", TRUE, fromIBM1370offs ))
        log_err("ibm-1370->u(MBCS) with Fallback did not match.\n");
    
   /*commented untill data table is available*/
    log_verbose("toUnicode fallback with fallback data for MBCS\n");
    {
        const uint8_t IBM1370input[] =   {  
            0xf4, 0x87, 0xa4, 0x4a, 0xf4, 0x88, 0xa4, 0x4b,
                0xf9, 0x92, 0xdc, 0xb0, };
        UChar expectedUnicodeText[]= { 0x5165, 0x5165, 0x516b, 0x516b, 0x9ef9, 0x9ef9};
        int32_t fromIBM1370offs []  =   {  0, 2, 4, 6, 8, 10};

        UChar expectedFallbackFalse[]= { 0xfffd, 0x5165, 0xfffd, 0x516b, 0xfffd, 0x9ef9};

        if(!testConvertToUnicode(IBM1370input, sizeof(IBM1370input), 
                expectedUnicodeText, sizeof(expectedUnicodeText)/sizeof(expectedUnicodeText[0]),"ibm-1370", TRUE, fromIBM1370offs ))
            log_err("ibm-1370->u(MBCS) with Fallback did not match.\n");
        if(!testConvertToUnicode(IBM1370input, sizeof(IBM1370input), 
                expectedFallbackFalse, sizeof(expectedFallbackFalse)/sizeof(expectedFallbackFalse[0]),"ibm-1370", FALSE, fromIBM1370offs ))
            log_err("ibm-1370->u(MBCS) with Fallback did not match.\n");

    }
    log_verbose("toUnicode fallback with fallback data for euc-tw\n");
    {
        const uint8_t euc_tw_input[] =   {  
            0xA7, 0xCC, 0x8E, 0xA2, 0xA1, 0xAB, 
            0xA8, 0xC7, 0xC8, 0xDE, 
            0xA8, 0xCD, 0x8E, 0xA2, 0xA2, 0xEA,};
        UChar expectedUnicodeText[]= { 0x5C6E, 0x5C6E, 0x81FC, 0x81FC, 0x8278, 0x8278};
        int32_t from_euc_tw_offs []  =   {  0, 2, 6, 8, 10, 12};

        UChar expectedFallbackFalse[]= { 0xfffd, 0x5C6E, 0xfffd, 0x81FC, 0xfffd, 0x8278};

        if(!testConvertToUnicode(euc_tw_input, sizeof(euc_tw_input), 
                expectedUnicodeText, sizeof(expectedUnicodeText)/sizeof(expectedUnicodeText[0]),"euc-tw", TRUE, from_euc_tw_offs ))
            log_err("from euc-tw->u with Fallback did not match.\n");

        if(!testConvertToUnicode(euc_tw_input, sizeof(euc_tw_input), 
                expectedFallbackFalse, sizeof(expectedFallbackFalse)/sizeof(expectedFallbackFalse[0]),"euc-tw", FALSE, from_euc_tw_offs ))
            log_err("from euc-tw->u with Fallback false did not match.\n");


    }
    log_verbose("fromUnicode to euc-tw with fallback data euc-tw\n");
    {
        UChar inputText[]= { 0x0001, 0x008e, 0x203e, 0x2223, 0xff5c, 0x5296, 
                             0x5C6E, 0x5C6E, 0x81FC, 0x81FC, 0x8278, 0x8278, 0xEDEC};
        const uint8_t expected_euc_tw[] =   {  
            0x01, 0xfd, 0xfe, 0xa2, 0xa3, 
            0xa2, 0xde, 0xa2, 0xde, 
            0x8e, 0xa2, 0xe5, 0xb9,
            0x8e, 0xa2, 0xa1, 0xab, 0x8e, 0xa2, 0xa1, 0xab,
            0xc8, 0xde, 0xc8, 0xde,
            0x8e, 0xa2, 0xa2, 0xea, 0x8e, 0xa2, 0xa2, 0xea,
            0x8e, 0xac, 0xc6, 0xf7};
        int32_t to_euc_tw_offs []  =   {  0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5, 5, 6, 6, 
            6, 6, 7, 7, 7, 7, 8, 8, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12};

        if(!testConvertFromUnicode(inputText, sizeof(inputText)/sizeof(inputText[0]),
                expected_euc_tw, sizeof(expected_euc_tw), "euc-tw", TRUE, to_euc_tw_offs ))
            log_err("u-> euc-tw with FallBack did not match.\n");

    }

    /*MBCS 1363*/
    if(!testConvertFromUnicode(MBCSText1363, sizeof(MBCSText1363)/sizeof(MBCSText1363[0]),
        expectedIBM1363, sizeof(expectedIBM1363), "ibm-1363", TRUE, toIBM1363Offs ))
       log_err("u-> ibm-1363(MBCS) with FallBack did not match.\n");

    if(!testConvertToUnicode(expectedIBM1363, sizeof(expectedIBM1363), 
        retrievedMBCSText1363, sizeof(retrievedMBCSText1363)/sizeof(retrievedMBCSText1363[0]),"ibm-1363", TRUE, fromIBM1363offs ))
        log_err("ibm-1363->u(MBCS) with Fallback did not match.\n");




}
