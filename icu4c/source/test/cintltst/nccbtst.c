/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File NCCBTST.C
*
* Modification History:
*        Name                            Description
*    Madhu Katragadda     7/21/1999      Testing error callback routines
**************************************************************************************
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
#include "nccbtst.h"
#define NEW_MAX_BUFFER 999

#define nct_min(x,y)  ((x<y) ? x : y)
#define ARRAY_LENGTH(array) (sizeof(array)/sizeof((array)[0]))

static int32_t  gInBufferSize = 0;
static int32_t  gOutBufferSize = 0;
static char     gNuConvTestName[1024];

static void printSeq(const uint8_t* a, int len)
{
    int i=0;
    log_verbose("\n{");
    while (i<len)
        log_verbose("0x%02X, ", a[i++]);
    log_verbose("}\n");
}

static void printUSeq(const UChar* a, int len)
{
    int i=0;
    log_verbose("{");
    while (i<len)
        log_verbose("  0x%04x, ", a[i++]);
    log_verbose("}\n");
}

static void printSeqErr(const uint8_t* a, int len)
{
    int i=0;
    fprintf(stderr, "{");
    while (i<len)
        fprintf(stderr, "  0x%02x, ", a[i++]);
    fprintf(stderr, "}\n");
}

static void printUSeqErr(const UChar* a, int len)
{
    int i=0;
    fprintf(stderr, "{");
    while (i<len)
        fprintf(stderr, "0x%04x, ", a[i++]);
    fprintf(stderr,"}\n");
}

static void setNuConvTestName(const char *codepage, const char *direction)
{
    sprintf(gNuConvTestName, "[testing %s %s Unicode, InputBufSiz=%d, OutputBufSiz=%d]",
            codepage,
            direction,
            gInBufferSize,
            gOutBufferSize);
}



void addTestConvertErrorCallBack(TestNode** root)
{
    addTest(root, &TestSkipCallBack,  "tsconv/nccbtst/TestSkipCallBack");
    addTest(root, &TestStopCallBack,  "tsconv/nccbtst/TestStopCallBack");
    addTest(root, &TestSubCallBack,   "tsconv/nccbtst/TestSubCallBack");
    addTest(root, &TestSubWithValueCallBack, "tsconv/nccbtst/TestSubWithValueCallBack");
    addTest(root, &TestLegalAndOtherCallBack,  "tsconv/nccbtst/TestLegalAndOtherCallBack");
    addTest(root, &TestSingleByteCallBack,  "tsconv/nccbtst/TestSingleByteCallBack");
}

static void TestSkipCallBack()
{
    TestSkip(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
    TestSkip(1,NEW_MAX_BUFFER);
    TestSkip(1,1);
    TestSkip(NEW_MAX_BUFFER, 1);
}

static void TestStopCallBack()
{
    TestStop(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
    TestStop(1,NEW_MAX_BUFFER);
    TestStop(1,1);
    TestStop(NEW_MAX_BUFFER, 1);
}

static void TestSubCallBack()
{
    TestSub(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
    TestSub(1,NEW_MAX_BUFFER);
    TestSub(1,1);
    TestSub(NEW_MAX_BUFFER, 1);
    TestEBCDIC_STATEFUL_Sub(1, 1);
    TestEBCDIC_STATEFUL_Sub(1, NEW_MAX_BUFFER);
    TestEBCDIC_STATEFUL_Sub(NEW_MAX_BUFFER, 1);
    TestEBCDIC_STATEFUL_Sub(NEW_MAX_BUFFER, NEW_MAX_BUFFER);


}

static void TestSubWithValueCallBack()
{
    TestSubWithValue(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
    TestSubWithValue(1,NEW_MAX_BUFFER);
    TestSubWithValue(1,1);
    TestSubWithValue(NEW_MAX_BUFFER, 1);
}

static void TestLegalAndOtherCallBack()
{
    TestLegalAndOthers(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
    TestLegalAndOthers(1,NEW_MAX_BUFFER);
    TestLegalAndOthers(1,1);
    TestLegalAndOthers(NEW_MAX_BUFFER, 1);
}

static void TestSingleByteCallBack()
{
    TestSingleByte(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
    TestSingleByte(1,NEW_MAX_BUFFER);
    TestSingleByte(1,1);
    TestSingleByte(NEW_MAX_BUFFER, 1);
}

static void TestSkip(int32_t inputsize, int32_t outputsize)
{
    UChar   sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
    UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };

    const uint8_t expskipIBM_949[]= { 
        0x00, 0xb0, 0xa1, 0xb0, 0xa2, 0xc8, 0xd3 };

    const uint8_t expskipIBM_943[] = { 
        0x9f, 0xaf, 0x9f, 0xb1, 0x89, 0x59 };

    const uint8_t expskipIBM_930[] = { 
        0x0e, 0x5d, 0x5f, 0x5d, 0x63, 0x46, 0x6b };

    UChar IBM_949skiptoUnicode[]= {0x0000, 0xAC00, 0xAC01, 0xD700 };
    UChar IBM_943skiptoUnicode[]= { 0x6D63, 0x6D64, 0x6D66 };
    UChar IBM_930skiptoUnicode[]= { 0x6D63, 0x6D64, 0x6D66 };


    int32_t  toIBM949Offsskip [] = { 0, 1, 1, 2, 2, 4, 4};
    int32_t  toIBM943Offsskip [] = { 0, 0, 1, 1, 3, 3};
    int32_t  toIBM930Offsskip [] = { 0, 0, 0, 1, 1, 3, 3};

    int32_t  fromIBM949Offs [] = { 0, 1, 3, 5};
    int32_t  fromIBM943Offs [] = { 0, 2, 4};
    int32_t  fromIBM930Offs [] = { 1, 3, 5};

    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;

    /*From Unicode*/
    log_verbose("Testing fromUnicode with UCNV_FROM_U_CALLBACK_SKIP  \n");

    if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expskipIBM_949, sizeof(expskipIBM_949), "ibm-949",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, toIBM949Offsskip, NULL, 0 ))
        log_err("u-> ibm-949 with skip did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expskipIBM_943, sizeof(expskipIBM_943), "ibm-943",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, toIBM943Offsskip, NULL, 0 ))
        log_err("u-> ibm-943 with skip did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expskipIBM_930, sizeof(expskipIBM_930), "ibm-930",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, toIBM930Offsskip , NULL, 0))
        log_err("u-> ibm-930 with skip did not match.\n");

    {
        static const UChar fromU[] = { 0x61, 0xff5e, 0x62, 0x6d63, 0xff5e, 0x6d64, 0x63, 0xff5e, 0x6d66 };
        static const uint8_t fromUBytes[] = { 0x62, 0x63, 0x0e, 0x5d, 0x5f, 0x5d, 0x63, 0x0f, 0x64, 0x0e, 0x46, 0x6b };
        static const int32_t fromUOffsets[] = { 0, 2, 3, 3, 3, 5, 5, 6, 6, 8, 8, 8 };

        /* test ibm-930 (EBCDIC_STATEFUL) with fallbacks that are not taken to check correct state transitions */
        if(!testConvertFromUnicode(fromU, sizeof(fromU)/U_SIZEOF_UCHAR,
                                   fromUBytes, sizeof(fromUBytes),
                                   "ibm-930",
                                   UCNV_FROM_U_CALLBACK_SKIP, fromUOffsets,
                                   NULL, 0)
        ) {
            log_err("u->ibm-930 with skip with untaken fallbacks did not match.\n");
        }
    }

    {
        static const UChar usasciiFromU[] = { 0x61, 0x80, 0x4e00, 0x31, 0xd800, 0xdfff, 0x39 };
        static const uint8_t usasciiFromUBytes[] = { 0x61, 0x31, 0x39 };
        static const int32_t usasciiFromUOffsets[] = { 0, 3, 6 };

        static const UChar latin1FromU[] = { 0x61, 0xa0, 0x4e00, 0x31, 0xd800, 0xdfff, 0x39 };
        static const uint8_t latin1FromUBytes[] = { 0x61, 0xa0, 0x31, 0x39 };
        static const int32_t latin1FromUOffsets[] = { 0, 1, 3, 6 };

        /* US-ASCII */
        if(!testConvertFromUnicode(usasciiFromU, sizeof(usasciiFromU)/U_SIZEOF_UCHAR,
                                   usasciiFromUBytes, sizeof(usasciiFromUBytes),
                                   "US-ASCII",
                                   UCNV_FROM_U_CALLBACK_SKIP, usasciiFromUOffsets,
                                   NULL, 0)
        ) {
            log_err("u->US-ASCII with skip did not match.\n");
        }

        /* SBCS NLTC codepage 367 for US-ASCII */
        if(!testConvertFromUnicode(usasciiFromU, sizeof(usasciiFromU)/U_SIZEOF_UCHAR,
                                   usasciiFromUBytes, sizeof(usasciiFromUBytes),
                                   "ibm-367",
                                   UCNV_FROM_U_CALLBACK_SKIP, usasciiFromUOffsets,
                                   NULL, 0)
        ) {
            log_err("u->ibm-367 with skip did not match.\n");
        }

        /* ISO-Latin-1 */
        if(!testConvertFromUnicode(latin1FromU, sizeof(latin1FromU)/U_SIZEOF_UCHAR,
                                   latin1FromUBytes, sizeof(latin1FromUBytes),
                                   "LATIN_1",
                                   UCNV_FROM_U_CALLBACK_SKIP, latin1FromUOffsets,
                                   NULL, 0)
        ) {
            log_err("u->LATIN_1 with skip did not match.\n");
        }

        /* windows-1252 */
        if(!testConvertFromUnicode(latin1FromU, sizeof(latin1FromU)/U_SIZEOF_UCHAR,
                                   latin1FromUBytes, sizeof(latin1FromUBytes),
                                   "windows-1252",
                                   UCNV_FROM_U_CALLBACK_SKIP, latin1FromUOffsets,
                                   NULL, 0)
        ) {
            log_err("u->windows-1252 with skip did not match.\n");
        }
    }

    {
        UChar inputTest[] = { 0x0061, 0xd801, 0xdc01, 0xd801, 0x0061 };
        const uint8_t toIBM943[]= { 0x61, 0x61 };
        int32_t offset[]= {0, 4};

         /* EUC_JP*/
        UChar euc_jp_inputText[]={ 0x0061, 0x4edd, 0x5bec, 0xd801, 0xdc01, 0xd801, 0x0061, 0x00a2 };
        const uint8_t to_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0x61, 0x8e, 0xe0,
        };
        int32_t fromEUC_JPOffs [] ={ 0, 1, 1, 2, 2, 2, 6, 7, 7};

        /*EUC_TW*/
        UChar euc_tw_inputText[]={ 0x0061, 0x2295, 0x5BF2, 0xd801, 0xdc01, 0xd801, 0x0061, 0x8706, 0x8a, };
        const uint8_t to_euc_tw[]={ 
            0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0x61, 0xe6, 0xca, 0x8a,
        };
        int32_t from_euc_twOffs [] ={ 0, 1, 1, 2, 2, 2, 2, 6, 7, 7, 8,};

        /*ISO-2022-JP*/
        UChar iso_2022_jp_inputText[]={0x0041, 0x00E9/*unassigned*/,0x0042, };
        const uint8_t to_iso_2022_jp[]={ 
            0x41,       
            0x42,

        };
        int32_t from_iso_2022_jpOffs [] ={0,2};

        UChar iso_2022_jp_inputText1[]={0x3000, 0x00E9, 0x3001, };
        const uint8_t to_iso_2022_jp1[]={ 
            0x1b,   0x24,   0x42,   0x21, 0x21,       
            0x21,   0x22,

        };
        int32_t from_iso_2022_jpOffs1 [] ={0,0,0,0,0,2,2,};

        /*ISO-2022-JP*/
        UChar iso_2022_jp_inputText2[]={0x0041, 0x00E9/*unassigned*/,0x43,0xd800/*illegal*/,0x0042, };
        const uint8_t to_iso_2022_jp2[]={ 
            0x41,       
            0x43,

        };
        int32_t from_iso_2022_jpOffs2 [] ={0,2};

        /*ISO-2022-cn*/
        UChar iso_2022_cn_inputText[]={ 0x0041, 0x3712/*unassigned*/, 0x0042, };
        const uint8_t to_iso_2022_cn[]={  
            0x0F,   0x41,   
            0x0F,   0x42, 
        };
        int32_t from_iso_2022_cnOffs [] ={ 
            0,0,
            2,2,
        };
        
        /*ISO-2022-CN*/
        UChar iso_2022_cn_inputText1[]={0x0041, 0x3712/*unassigned*/,0x43,0xd800/*illegal*/,0x0042, };
        const uint8_t to_iso_2022_cn1[]={ 
            0x0F,   0x41,   
            0x0F,   0x43, 

        };
        int32_t from_iso_2022_cnOffs1 [] ={0,0,2,2};

        /*ISO-2022-kr*/
        UChar iso_2022_kr_inputText[]={ 0x0041, 0x03A0,0x3712/*unassigned*/,0x03A0, 0x0042, };
        const uint8_t to_iso_2022_kr[]={  
            0x1b,   0x24,   0x29,   0x43,   
            0x41,   
            0x0e,   0x25,   0x50,   
            0x25,   0x50, 
            0x0f,   0x42, 
        };
        int32_t from_iso_2022_krOffs [] ={ 
            -1,-1,-1,-1,
            0,
            1,1,1,
            3,3,
            4,4
        };

        /*ISO-2022-kr*/
        UChar iso_2022_kr_inputText1[]={ 0x0041, 0x03A0,0x3712/*unassigned*/,0x03A0,0xd801/*illegal*/, 0x0042, };
        const uint8_t to_iso_2022_kr1[]={  
            0x1b,   0x24,   0x29,   0x43,   
            0x41,   
            0x0e,   0x25,   0x50,   
            0x25,   0x50, 
 
        };
        int32_t from_iso_2022_krOffs1 [] ={ 
            -1,-1,-1,-1,
            0,
            1,1,1,
            3,3,

        };
        /* HZ encoding */       
        UChar hz_inputText[]={ 0x0041, 0x03A0,0x0662/*unassigned*/,0x03A0, 0x0042, };

        const uint8_t to_hz[]={    
            0x7e,   0x7d,   0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
            0x26,   0x30,
            0x7e,   0x7d,   0x42, 
           
        };
        int32_t from_hzOffs [] ={ 
            0,0,0,
            1,1,1,1,
            3,3,
            4,4,4,4
        };

        UChar hz_inputText1[]={ 0x0041, 0x03A0,0x0662/*unassigned*/,0x03A0,0xd801/*illegal*/, 0x0042, };

        const uint8_t to_hz1[]={    
            0x7e,   0x7d,   0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
            0x26,   0x30,

           
        };
        int32_t from_hzOffs1 [] ={ 
            0,0,0,
            1,1,1,1,
            3,3,

        };


        UChar SCSU_inputText[]={ 0x0041, 0xd801/*illegal*/, 0x0042, };

        const uint8_t to_SCSU[]={    
            0x41,   
            0x42

           
        };
        int32_t from_SCSUOffs [] ={ 
            0,
            2,

        };

        if(!testConvertFromUnicode(inputTest, sizeof(inputTest)/sizeof(inputTest[0]),
                toIBM943, sizeof(toIBM943), "ibm-943",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, offset, NULL, 0 ))
            log_err("u-> ibm-943 with skip did not match.\n");

        if(!testConvertFromUnicode(euc_jp_inputText, sizeof(euc_jp_inputText)/sizeof(euc_jp_inputText[0]),
                to_euc_jp, sizeof(to_euc_jp), "euc-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, fromEUC_JPOffs, NULL, 0 ))
            log_err("u-> euc-jp with skip did not match.\n");

        if(!testConvertFromUnicode(euc_tw_inputText, sizeof(euc_tw_inputText)/sizeof(euc_tw_inputText[0]),
                to_euc_tw, sizeof(to_euc_tw), "euc-tw",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_euc_twOffs, NULL, 0 ))
            log_err("u-> euc-tw with skip did not match.\n");  
        
        /*iso_2022_jp*/
        if(!testConvertFromUnicode(iso_2022_jp_inputText, sizeof(iso_2022_jp_inputText)/sizeof(iso_2022_jp_inputText[0]),
                to_iso_2022_jp, sizeof(to_iso_2022_jp), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_jpOffs, NULL, 0 ))
            log_err("u-> iso-2022-jp with skip did not match.\n"); 
        
        if(!testConvertFromUnicode(iso_2022_jp_inputText1, sizeof(iso_2022_jp_inputText1)/sizeof(iso_2022_jp_inputText1[0]),
                to_iso_2022_jp1, sizeof(to_iso_2022_jp1), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_jpOffs1, NULL, 0 ))
            log_err("u-> iso-2022-jp with skip did not match.\n"); 
        /* with context */
        if(!testConvertFromUnicodeWithContext(iso_2022_jp_inputText2, sizeof(iso_2022_jp_inputText2)/sizeof(iso_2022_jp_inputText2[0]),
                to_iso_2022_jp2, sizeof(to_iso_2022_jp2), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_jpOffs2, NULL, 0,UCNV_SKIP_STOP_ON_ILLEGAL,U_ILLEGAL_CHAR_FOUND ))
            log_err("u-> iso-2022-jp with skip & UCNV_SKIP_STOP_ON_ILLEGAL did not match.\n"); 
    
        /*iso_2022_cn*/
        if(!testConvertFromUnicode(iso_2022_cn_inputText, sizeof(iso_2022_cn_inputText)/sizeof(iso_2022_cn_inputText[0]),
                to_iso_2022_cn, sizeof(to_iso_2022_cn), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_cnOffs, NULL, 0 ))
            log_err("u-> iso-2022-cn with skip did not match.\n"); 
        /*with context*/
        if(!testConvertFromUnicodeWithContext(iso_2022_cn_inputText1, sizeof(iso_2022_cn_inputText1)/sizeof(iso_2022_cn_inputText1[0]),
                to_iso_2022_cn1, sizeof(to_iso_2022_cn1), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_cnOffs1, NULL, 0,UCNV_SKIP_STOP_ON_ILLEGAL,U_ILLEGAL_CHAR_FOUND ))
            log_err("u-> iso-2022-cn with skip & UCNV_SKIP_STOP_ON_ILLEGAL did not match.\n"); 

        /*iso_2022_kr*/
        if(!testConvertFromUnicode(iso_2022_kr_inputText, sizeof(iso_2022_kr_inputText)/sizeof(iso_2022_kr_inputText[0]),
                to_iso_2022_kr, sizeof(to_iso_2022_kr), "iso-2022-kr",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_krOffs, NULL, 0 ))
            log_err("u-> iso-2022-kr with skip did not match.\n"); 
          /*with context*/
        if(!testConvertFromUnicodeWithContext(iso_2022_kr_inputText1, sizeof(iso_2022_kr_inputText1)/sizeof(iso_2022_kr_inputText1[0]),
                to_iso_2022_kr1, sizeof(to_iso_2022_kr1), "iso-2022-kr",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_iso_2022_krOffs1, NULL, 0,UCNV_SKIP_STOP_ON_ILLEGAL,U_ILLEGAL_CHAR_FOUND ))
            log_err("u-> iso-2022-kr with skip & UCNV_SKIP_STOP_ON_ILLEGAL did not match.\n"); 

        /*hz*/
        if(!testConvertFromUnicode(hz_inputText, sizeof(hz_inputText)/sizeof(hz_inputText[0]),
                to_hz, sizeof(to_hz), "HZ",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_hzOffs, NULL, 0 ))
            log_err("u-> HZ with skip did not match.\n"); 
          /*with context*/
        if(!testConvertFromUnicodeWithContext(hz_inputText1, sizeof(hz_inputText1)/sizeof(hz_inputText1[0]),
                to_hz1, sizeof(to_hz1), "hz",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_hzOffs1, NULL, 0,UCNV_SKIP_STOP_ON_ILLEGAL,U_ILLEGAL_CHAR_FOUND ))
            log_err("u-> hz with skip & UCNV_SKIP_STOP_ON_ILLEGAL did not match.\n"); 
        
        /*SCSU*/
        if(!testConvertFromUnicode(SCSU_inputText, sizeof(SCSU_inputText)/sizeof(SCSU_inputText[0]),
                to_SCSU, sizeof(to_SCSU), "SCSU",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, from_SCSUOffs, NULL, 0 ))
            log_err("u-> SCSU with skip did not match.\n");


      
    }

    /*to Unicode*/
    log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_SKIP  \n");

    if(!testConvertToUnicode(expskipIBM_949, sizeof(expskipIBM_949),
             IBM_949skiptoUnicode, sizeof(IBM_949skiptoUnicode)/sizeof(IBM_949skiptoUnicode),"ibm-949",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM949Offs, NULL, 0 ))
        log_err("ibm-949->u with skip did not match.\n");
    if(!testConvertToUnicode(expskipIBM_943, sizeof(expskipIBM_943),
             IBM_943skiptoUnicode, sizeof(IBM_943skiptoUnicode)/sizeof(IBM_943skiptoUnicode[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM943Offs, NULL, 0 ))
        log_err("ibm-943->u with skip did not match.\n");

    if(!testConvertToUnicode(expskipIBM_930, sizeof(expskipIBM_930),
             IBM_930skiptoUnicode, sizeof(IBM_930skiptoUnicode)/sizeof(IBM_930skiptoUnicode[0]),"ibm-930",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM930Offs, NULL, 0 ))
        log_err("ibm-930->u with skip did not match.\n");

    {
        static const uint8_t usasciiToUBytes[] = { 0x61, 0x80, 0x31 };
        static const UChar usasciiToU[] = { 0x61, 0x31 };
        static const int32_t usasciiToUOffsets[] = { 0, 2 };

        static const uint8_t latin1ToUBytes[] = { 0x61, 0xa0, 0x31 };
        static const UChar latin1ToU[] = { 0x61, 0xa0, 0x31 };
        static const int32_t latin1ToUOffsets[] = { 0, 1, 2 };

        /* US-ASCII */
        if(!testConvertToUnicode(usasciiToUBytes, sizeof(usasciiToUBytes),
                                 usasciiToU, sizeof(usasciiToU)/U_SIZEOF_UCHAR,
                                 "US-ASCII",
                                 UCNV_TO_U_CALLBACK_SKIP, usasciiToUOffsets,
                                 NULL, 0)
        ) {
            log_err("US-ASCII->u with skip did not match.\n");
        }

        /* SBCS NLTC codepage 367 for US-ASCII */
        if(!testConvertToUnicode(usasciiToUBytes, sizeof(usasciiToUBytes),
                                 usasciiToU, sizeof(usasciiToU)/U_SIZEOF_UCHAR,
                                 "ibm-367",
                                 UCNV_TO_U_CALLBACK_SKIP, usasciiToUOffsets,
                                 NULL, 0)
        ) {
            log_err("ibm-367->u with skip did not match.\n");
        }

        /* ISO-Latin-1 */
        if(!testConvertToUnicode(latin1ToUBytes, sizeof(latin1ToUBytes),
                                 latin1ToU, sizeof(latin1ToU)/U_SIZEOF_UCHAR,
                                 "LATIN_1",
                                 UCNV_TO_U_CALLBACK_SKIP, latin1ToUOffsets,
                                 NULL, 0)
        ) {
            log_err("LATIN_1->u with skip did not match.\n");
        }

        /* windows-1252 */
        if(!testConvertToUnicode(latin1ToUBytes, sizeof(latin1ToUBytes),
                                 latin1ToU, sizeof(latin1ToU)/U_SIZEOF_UCHAR,
                                 "windows-1252",
                                 UCNV_TO_U_CALLBACK_SKIP, latin1ToUOffsets,
                                 NULL, 0)
        ) {
            log_err("windows-1252->u with skip did not match.\n");
        }
    }

    {
        const uint8_t sampleTxtEBCIDIC_STATEFUL [] ={
            0x0e, 0x5d, 0x5f , 0x41, 0x79, 0x41, 0x44
        };
        UChar EBCIDIC_STATEFUL_toUnicode[] ={  0x6d63, 0x03b4 
        };
        int32_t from_EBCIDIC_STATEFULOffsets []={ 1, 5};
       

         /* euc-jp*/
        const uint8_t sampleTxt_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0x8f, 0xda, 0xa1,  /*unassigned*/
           0x8e, 0xe0,
        };
        UChar euc_jptoUnicode[]={ 0x0061, 0x4edd, 0x5bec, 0x00a2};
        int32_t from_euc_jpOffs [] ={ 0, 1, 3, 9};

         /*EUC_TW*/
        const uint8_t sampleTxt_euc_tw[]={ 0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0x8e, 0xaa, 0xbb, 0xcc,/*unassigned*/
           0xe6, 0xca, 0x8a,
        };
        UChar euc_twtoUnicode[]={ 0x0061, 0x2295, 0x5BF2, 0x8706, 0x8a, };
        int32_t from_euc_twOffs [] ={ 0, 1, 3, 11, 13};
                /*iso-2022-jp*/
        const uint8_t sampleTxt_iso_2022_jp[]={ 
            0x41,
            0x1b,   0x24,   0x42,   0x2A, 0x44, /*unassigned*/
             0x1b,   0x28,   0x42,   0x42,
            
        };
        UChar iso_2022_jptoUnicode[]={    0x41,0x42 };
        int32_t from_iso_2022_jpOffs [] ={  0,9   };
        
        /*iso-2022-cn*/
        const uint8_t sampleTxt_iso_2022_cn[]={ 
            0x0f,   0x41,   0x44,
            0x1B,   0x24,   0x29,   0x47, 
            0x0E,   0x40,   0x6c, /*unassigned*/
            0x0f,   0x42,
            
        };

        UChar iso_2022_cntoUnicode[]={    0x41, 0x44,0x42 };
        int32_t from_iso_2022_cnOffs [] ={  1,   2,   11   };

        /*iso-2022-kr*/
        const uint8_t sampleTxt_iso_2022_kr[]={ 
          0x1b, 0x24, 0x29,  0x43,
          0x41,
          0x0E, 0x7f, 0x1E,
          0x0e, 0x25, 0x50, 
          0x0f, 0x51,
          0x42, 0x43,
            
        };
        UChar iso_2022_krtoUnicode[]={     0x41,0x03A0,0x51, 0x42,0x43};
        int32_t from_iso_2022_krOffs [] ={  4,    9,    12,   13  , 14 };
        
        /*hz*/
        const uint8_t sampleTxt_hz[]={ 
            0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
            0x7f,   0x1E, /*unassigned*/ 
            0x26,   0x30,
            0x7e,   0x7d,   0x42, 
            0x7e,   0x7b,   0x7f,   0x1E,/*unassigned*/ 
            0x7e,   0x7d,   0x42,
        };
        UChar hztoUnicode[]={  
            0x41,
            0x03a0,
            0x03A0, 
            0x42,
            0x42,};

        int32_t from_hzOffs [] ={0,3,7,11,18,  };

        /*LMBCS*/
        const uint8_t sampleTxtLMBCS[]={ 0x12, 0xc9, 0x50, 
            0x12, 0x92, 0xa0, /*unassigned*/
            0x12, 0x92, 0xA1,
        };
        UChar LMBCSToUnicode[]={ 0x4e2e, 0xe5c4};
        int32_t fromLMBCS[] = {0, 6};
 
        if(!testConvertToUnicode(sampleTxtEBCIDIC_STATEFUL, sizeof(sampleTxtEBCIDIC_STATEFUL),
             EBCIDIC_STATEFUL_toUnicode, sizeof(EBCIDIC_STATEFUL_toUnicode)/sizeof(EBCIDIC_STATEFUL_toUnicode[0]),"ibm-930",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_EBCIDIC_STATEFULOffsets, NULL, 0 ))
        log_err("EBCIDIC_STATEFUL->u with skip did not match.\n");



        if(!testConvertToUnicode(sampleTxt_euc_jp, sizeof(sampleTxt_euc_jp),
                 euc_jptoUnicode, sizeof(euc_jptoUnicode)/sizeof(euc_jptoUnicode[0]),"euc-jp",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_euc_jpOffs , NULL, 0))
            log_err("euc-jp->u with skip did not match.\n");



        if(!testConvertToUnicode(sampleTxt_euc_tw, sizeof(sampleTxt_euc_tw),
                 euc_twtoUnicode, sizeof(euc_twtoUnicode)/sizeof(euc_twtoUnicode[0]),"euc-tw",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_euc_twOffs , NULL, 0))
            log_err("euc-tw->u with skip did not match.\n");

        
        if(!testConvertToUnicode(sampleTxt_iso_2022_jp, sizeof(sampleTxt_iso_2022_jp),
                 iso_2022_jptoUnicode, sizeof(iso_2022_jptoUnicode)/sizeof(iso_2022_jptoUnicode[0]),"iso-2022-jp",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_iso_2022_jpOffs , NULL, 0))
            log_err("iso-2022-jp->u with skip did not match.\n");
        
        if(!testConvertToUnicode(sampleTxt_iso_2022_cn, sizeof(sampleTxt_iso_2022_cn),
                 iso_2022_cntoUnicode, sizeof(iso_2022_cntoUnicode)/sizeof(iso_2022_cntoUnicode[0]),"iso-2022-cn",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_iso_2022_cnOffs , NULL, 0))
            log_err("iso-2022-cn->u with skip did not match.\n");

        if(!testConvertToUnicode(sampleTxt_iso_2022_kr, sizeof(sampleTxt_iso_2022_kr),
                 iso_2022_krtoUnicode, sizeof(iso_2022_krtoUnicode)/sizeof(iso_2022_krtoUnicode[0]),"iso-2022-kr",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_iso_2022_krOffs , NULL, 0))
            log_err("iso-2022-kr->u with skip did not match.\n");

        if(!testConvertToUnicode(sampleTxt_hz, sizeof(sampleTxt_hz),
                 hztoUnicode, sizeof(hztoUnicode)/sizeof(hztoUnicode[0]),"HZ",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, from_hzOffs , NULL, 0))
            log_err("HZ->u with skip did not match.\n");

        if(/* broken for icu 1.6 and 1.6.0.1, do not test */uprv_strcmp("1.7", U_ICU_VERSION) != 0 && !testConvertToUnicode(sampleTxtLMBCS, sizeof(sampleTxtLMBCS),
                LMBCSToUnicode, sizeof(LMBCSToUnicode)/sizeof(LMBCSToUnicode[0]),"LMBCS-1",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromLMBCS , NULL, 0))
            log_err("LMBCS->u with skip did not match.\n");

    }
    log_verbose("Testing to Unicode for UTF-8 with UCNV_TO_U_CALLBACK_SKIP \n");
    {
        const uint8_t sampleText1[] = { 0x31, 0xe4, 0xba, 0x8c, 
            0xe0, 0x80,  0x61,};
        UChar    expected1[] = {  0x0031, 0x4e8c, 0x0061};
        int32_t offsets1[] = {   0x0000, 0x0001, 0x0006};

        if(!testConvertToUnicode(sampleText1, sizeof(sampleText1),
                 expected1, sizeof(expected1)/sizeof(expected1[0]),"utf8",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, offsets1, NULL, 0 ))
            log_err("utf8->u with skip did not match.\n");;
    }

    log_verbose("Testing toUnicode for SCSU with UCNV_TO_U_CALLBACK_SKIP \n");
    {
        const uint8_t sampleText1[] = {  0xba, 0x8c,0xF8, 0x61,0x0c, 0x0c,};
        UChar    expected1[] = {  0x00ba,  0x008c,  0x00f8,  0x0061,0xfffe,0xfffe};
        int32_t offsets1[] = {   0x0000, 0x0001,0x0002,0x0003,4,5};

        if(!testConvertToUnicode(sampleText1, sizeof(sampleText1),
                 expected1, sizeof(expected1)/sizeof(expected1[0]),"SCSU",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, offsets1, NULL, 0 ))
            log_err("scsu->u with stop did not match.\n");;
    }
}

static void TestStop(int32_t inputsize, int32_t outputsize)
{
    UChar   sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
    UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };

    const uint8_t expstopIBM_949[]= { 
        0x00, 0xb0, 0xa1, 0xb0, 0xa2};

    const uint8_t expstopIBM_943[] = { 
        0x9f, 0xaf, 0x9f, 0xb1};

    const uint8_t expstopIBM_930[] = { 
        0x0e, 0x5d, 0x5f, 0x5d, 0x63};

    UChar IBM_949stoptoUnicode[]= {0x0000, 0xAC00, 0xAC01};
    UChar IBM_943stoptoUnicode[]= { 0x6D63, 0x6D64};
    UChar IBM_930stoptoUnicode[]= { 0x6D63, 0x6D64};


    int32_t  toIBM949Offsstop [] = { 0, 1, 1, 2, 2};
    int32_t  toIBM943Offsstop [] = { 0, 0, 1, 1};
    int32_t  toIBM930Offsstop [] = { 0, 0, 0, 1, 1};

    int32_t  fromIBM949Offs [] = { 0, 1, 3};
    int32_t  fromIBM943Offs [] = { 0, 2};
    int32_t  fromIBM930Offs [] = { 1, 3};

    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;
    /*From Unicode*/
    if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expstopIBM_949, sizeof(expstopIBM_949), "ibm-949",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, toIBM949Offsstop, NULL, 0 ))
        log_err("u-> ibm-949 with stop did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expstopIBM_943, sizeof(expstopIBM_943), "ibm-943",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, toIBM943Offsstop , NULL, 0))
        log_err("u-> ibm-943 with stop did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expstopIBM_930, sizeof(expstopIBM_930), "ibm-930",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, toIBM930Offsstop, NULL, 0 ))
        log_err("u-> ibm-930 with stop did not match.\n");

    log_verbose("Testing fromUnicode with UCNV_FROM_U_CALLBACK_STOP  \n");
    {
        UChar inputTest[] = { 0x0061, 0xd801, 0xdc01, 0xd801, 0x0061 };
        const uint8_t toIBM943[]= { 0x61,};
        int32_t offset[]= {0,} ;

         /*EUC_JP*/
        UChar euc_jp_inputText[]={ 0x0061, 0x4edd, 0x5bec, 0xd801, 0xdc01, 0xd801, 0x0061, 0x00a2 };
        const uint8_t to_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,};
        int32_t fromEUC_JPOffs [] ={ 0, 1, 1, 2, 2, 2,};

        /*EUC_TW*/
        UChar euc_tw_inputText[]={ 0x0061, 0x2295, 0x5BF2, 0xd801, 0xdc01, 0xd801, 0x0061, 0x8706, 0x8a, };
        const uint8_t to_euc_tw[]={ 
            0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,};
        int32_t from_euc_twOffs [] ={ 0, 1, 1, 2, 2, 2, 2,};
        
        /*ISO-2022-JP*/
        UChar iso_2022_jp_inputText[]={0x0041, 0x00E9, 0x0042, };
        const uint8_t to_iso_2022_jp[]={ 
             0x41,   

        };
        int32_t from_iso_2022_jpOffs [] ={0,};

        /*ISO-2022-cn*/
        UChar iso_2022_cn_inputText[]={ 0x0041, 0x3712, 0x0042, };
        const uint8_t to_iso_2022_cn[]={  
            0x0F,   0x41,   

        };
        int32_t from_iso_2022_cnOffs [] ={ 
            0,0,
            2,2,
        };
        
        /*ISO-2022-kr*/
        UChar iso_2022_kr_inputText[]={ 0x0041, 0x03A0,0x3712/*unassigned*/,0x03A0, 0x0042, };
        const uint8_t to_iso_2022_kr[]={  
            0x1b,   0x24,   0x29,   0x43,   
            0x41,   
            0x0e,   0x25,   0x50,   
        };
        int32_t from_iso_2022_krOffs [] ={ 
            -1,-1,-1,-1,
             0,
            1,1,1,
        };

        /* HZ encoding */       
        UChar hz_inputText[]={ 0x0041, 0x03A0,0x0662/*unassigned*/,0x03A0, 0x0042, };

        const uint8_t to_hz[]={    
            0x7e,   0x7d, 0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
           
        };
        int32_t from_hzOffs [] ={ 
            0, 0,0,
            1,1,1,1,
        };


        if(!testConvertFromUnicode(inputTest, sizeof(inputTest)/sizeof(inputTest[0]),
                toIBM943, sizeof(toIBM943), "ibm-943",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, offset, NULL, 0 ))
            log_err("u-> ibm-943 with stop did not match.\n");

        if(!testConvertFromUnicode(euc_jp_inputText, sizeof(euc_jp_inputText)/sizeof(euc_jp_inputText[0]),
                to_euc_jp, sizeof(to_euc_jp), "euc-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, fromEUC_JPOffs, NULL, 0 ))
            log_err("u-> euc-jp with stop did not match.\n");

        if(!testConvertFromUnicode(euc_tw_inputText, sizeof(euc_tw_inputText)/sizeof(euc_tw_inputText[0]),
                to_euc_tw, sizeof(to_euc_tw), "euc-tw",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_euc_twOffs, NULL, 0 ))
            log_err("u-> euc-tw with stop did not match.\n");  

        if(!testConvertFromUnicode(iso_2022_jp_inputText, sizeof(iso_2022_jp_inputText)/sizeof(iso_2022_jp_inputText[0]),
                to_iso_2022_jp, sizeof(to_iso_2022_jp), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_iso_2022_jpOffs, NULL, 0 ))
            log_err("u-> iso-2022-jp with stop did not match.\n");  

        if(!testConvertFromUnicode(iso_2022_jp_inputText, sizeof(iso_2022_jp_inputText)/sizeof(iso_2022_jp_inputText[0]),
                to_iso_2022_jp, sizeof(to_iso_2022_jp), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_iso_2022_jpOffs, NULL, 0 ))
            log_err("u-> iso-2022-jp with stop did not match.\n");  
        
        if(!testConvertFromUnicode(iso_2022_cn_inputText, sizeof(iso_2022_cn_inputText)/sizeof(iso_2022_cn_inputText[0]),
                to_iso_2022_cn, sizeof(to_iso_2022_cn), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_iso_2022_cnOffs, NULL, 0 ))
            log_err("u-> iso-2022-cn with stop did not match.\n");  

        if(!testConvertFromUnicode(iso_2022_kr_inputText, sizeof(iso_2022_kr_inputText)/sizeof(iso_2022_kr_inputText[0]),
                to_iso_2022_kr, sizeof(to_iso_2022_kr), "iso-2022-kr",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_iso_2022_krOffs, NULL, 0 ))
            log_err("u-> iso-2022-kr with stop did not match.\n");  
        
        if(!testConvertFromUnicode(hz_inputText, sizeof(hz_inputText)/sizeof(hz_inputText[0]),
                to_hz, sizeof(to_hz), "HZ",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_hzOffs, NULL, 0 ))
            log_err("u-> HZ with stop did not match.\n");  


    }
    log_verbose("Testing fromUnicode for UTF-8 with UCNV_FROM_U_CALLBACK_STOP \n");
    {
        const UChar testinput[]={ 0x20ac, 0xd801, 0xdc01, 0xdc01, 0xd801, 0xffff, 0x0061,};
        const uint8_t expectedUTF8[]= { 0xe2, 0x82, 0xac, 
                           0xf0, 0x90, 0x90, 0x81, 
                           0xed, 0xb0, 0x81, 0xed, 0xa0, 0x81,
                           0xef, 0xbf, 0xbf, 0x61,
                           
        };
        int32_t offsets[]={ 0, 0, 0, 1, 1, 1, 1, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6 };
        if(!testConvertFromUnicode(testinput, sizeof(testinput)/sizeof(testinput[0]),
                expectedUTF8, sizeof(expectedUTF8), "utf8",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, offsets, NULL, 0 ))
            log_err("u-> utf8 with stop did not match.\n");
    }
    log_verbose("Testing fromUnicode for SCSU with UCNV_FROM_U_CALLBACK_STOP \n");
    {
        UChar SCSU_inputText[]={ 0x0041, 0xd801/*illegal*/, 0x0042, };

        const uint8_t to_SCSU[]={    
            0x41,   
           
        };
        int32_t from_SCSUOffs [] ={ 
            0,

        };
        if(!testConvertFromUnicode(SCSU_inputText, sizeof(SCSU_inputText)/sizeof(SCSU_inputText[0]),
                to_SCSU, sizeof(to_SCSU), "SCSU",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, from_SCSUOffs, NULL, 0 ))
            log_err("u-> SCSU with skip did not match.\n");
    
    }
    /*to Unicode*/
    if(!testConvertToUnicode(expstopIBM_949, sizeof(expstopIBM_949),
             IBM_949stoptoUnicode, sizeof(IBM_949stoptoUnicode)/sizeof(IBM_949stoptoUnicode[0]),"ibm-949",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM949Offs, NULL, 0 ))
        log_err("ibm-949->u with stop did not match.\n");
    if(!testConvertToUnicode(expstopIBM_943, sizeof(expstopIBM_943),
             IBM_943stoptoUnicode, sizeof(IBM_943stoptoUnicode)/sizeof(IBM_943stoptoUnicode[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM943Offs, NULL, 0 ))
        log_err("ibm-943->u with stop did not match.\n");
    if(!testConvertToUnicode(expstopIBM_930, sizeof(expstopIBM_930),
             IBM_930stoptoUnicode, sizeof(IBM_930stoptoUnicode)/sizeof(IBM_930stoptoUnicode[0]),"ibm-930",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM930Offs, NULL, 0 ))
        log_err("ibm-930->u with stop did not match.\n");

    log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_STOP \n");
    {

        const uint8_t sampleTxtEBCIDIC_STATEFUL [] ={
            0x0e, 0x5d, 0x5f , 0x41, 0x79, 0x41, 0x44
        };
        UChar EBCIDIC_STATEFUL_toUnicode[] ={  0x6d63 };
        int32_t from_EBCIDIC_STATEFULOffsets []={ 1};


         /*EUC-JP*/
        const uint8_t sampleTxt_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0x8f, 0xda, 0xa1,  /*unassigned*/
           0x8e, 0xe0,
        };
        UChar euc_jptoUnicode[]={ 0x0061, 0x4edd, 0x5bec};
        int32_t from_euc_jpOffs [] ={ 0, 1, 3};

          /*EUC_TW*/
        const uint8_t sampleTxt_euc_tw[]={ 0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0x8e, 0xaa, 0xbb, 0xcc,/*unassigned*/
           0xe6, 0xca, 0x8a,
        };
        UChar euc_twtoUnicode[]={ 0x0061, 0x2295, 0x5BF2};
        int32_t from_euc_twOffs [] ={ 0, 1, 3};



         if(!testConvertToUnicode(sampleTxtEBCIDIC_STATEFUL, sizeof(sampleTxtEBCIDIC_STATEFUL),
             EBCIDIC_STATEFUL_toUnicode, sizeof(EBCIDIC_STATEFUL_toUnicode)/sizeof(EBCIDIC_STATEFUL_toUnicode[0]),"ibm-930",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, from_EBCIDIC_STATEFULOffsets, NULL, 0 ))
        log_err("EBCIDIC_STATEFUL->u with stop did not match.\n");

        if(!testConvertToUnicode(sampleTxt_euc_jp, sizeof(sampleTxt_euc_jp),
             euc_jptoUnicode, sizeof(euc_jptoUnicode)/sizeof(euc_jptoUnicode[0]),"euc-jp",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, from_euc_jpOffs , NULL, 0))
        log_err("euc-jp->u with stop did not match.\n");

        if(!testConvertToUnicode(sampleTxt_euc_tw, sizeof(sampleTxt_euc_tw),
                 euc_twtoUnicode, sizeof(euc_twtoUnicode)/sizeof(euc_twtoUnicode[0]),"euc-tw",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, from_euc_twOffs, NULL, 0 ))
            log_err("euc-tw->u with stop did not match.\n");
    }
    log_verbose("Testing toUnicode for UTF-8 with UCNV_TO_U_CALLBACK_STOP \n");
    {
        const uint8_t sampleText1[] = { 0x31, 0xe4, 0xba, 0x8c, 
            0xe0, 0x80,  0x61,};
        UChar    expected1[] = {  0x0031, 0x4e8c,};
        int32_t offsets1[] = {   0x0000, 0x0001};

        if(!testConvertToUnicode(sampleText1, sizeof(sampleText1),
                 expected1, sizeof(expected1)/sizeof(expected1[0]),"utf8",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, offsets1, NULL, 0 ))
            log_err("utf8->u with stop did not match.\n");;
    }
    log_verbose("Testing toUnicode for SCSU with UCNV_TO_U_CALLBACK_STOP \n");
    {
        const uint8_t sampleText1[] = {  0xba, 0x8c,0xF8, 0x61,0x0c, 0x0c,0x04};
        UChar    expected1[] = {  0x00ba,  0x008c,  0x00f8,  0x0061};
        int32_t offsets1[] = {   0x0000, 0x0001,0x0002,0x0003};

        if(!testConvertToUnicode(sampleText1, sizeof(sampleText1),
                 expected1, sizeof(expected1)/sizeof(expected1[0]),"SCSU",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, offsets1, NULL, 0 ))
            log_err("scsu->u with stop did not match.\n");;
    }

}

static void TestSub(int32_t inputsize, int32_t outputsize)
{
    UChar   sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
    UChar sampleText2[]=    { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };
    
    const uint8_t expsubIBM_949[] = 
     { 0x00, 0xb0, 0xa1, 0xb0, 0xa2, 0xaf, 0xfe, 0xc8, 0xd3 };

    const uint8_t expsubIBM_943[] = { 
        0x9f, 0xaf, 0x9f, 0xb1, 0xfc, 0xfc, 0x89, 0x59 };

    const uint8_t expsubIBM_930[] = { 
        0x0e, 0x5d, 0x5f, 0x5d, 0x63, 0xfe, 0xfe, 0x46, 0x6b };

    UChar IBM_949subtoUnicode[]= {0x0000, 0xAC00, 0xAC01, 0xfffd, 0xD700 };
    UChar IBM_943subtoUnicode[]= {0x6D63, 0x6D64, 0xfffd, 0x6D66 };
    UChar IBM_930subtoUnicode[]= {0x6D63, 0x6D64, 0xfffd, 0x6D66 };

    int32_t toIBM949Offssub [] ={ 0, 1, 1, 2, 2, 3, 3, 4, 4};
    int32_t toIBM943Offssub [] ={ 0, 0, 1, 1, 2, 2, 3, 3};
    int32_t toIBM930Offssub [] ={ 0, 0, 0, 1, 1, 2, 2, 3, 3};

    int32_t  fromIBM949Offs [] = { 0, 1, 3, 5, 7};
    int32_t  fromIBM943Offs [] = { 0, 2, 4, 6};
    int32_t  fromIBM930Offs [] = { 1, 3, 5, 7};

    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;

    /*from unicode*/
    if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expsubIBM_949, sizeof(expsubIBM_949), "ibm-949", 
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, toIBM949Offssub, NULL, 0 ))
        log_err("u-> ibm-949 with subst did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expsubIBM_943, sizeof(expsubIBM_943), "ibm-943",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, toIBM943Offssub , NULL, 0))
        log_err("u-> ibm-943 with subst did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expsubIBM_930, sizeof(expsubIBM_930), "ibm-930", 
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, toIBM930Offssub, NULL, 0 ))
        log_err("u-> ibm-930 with subst did not match.\n");

    log_verbose("Testing fromUnicode with UCNV_FROM_U_CALLBACK_SUBSTITUTE  \n");
    {
        UChar inputTest[] = { 0x0061, 0xd801, 0xdc01, 0xd801, 0x0061 };
        const uint8_t toIBM943[]= { 0x61, 0xfc, 0xfc, 0xfc, 0xfc, 0x61 };
        int32_t offset[]= {0, 1, 1, 3, 3, 4};


        /* EUC_JP*/
        UChar euc_jp_inputText[]={ 0x0061, 0x4edd, 0x5bec, 0xd801, 0xdc01, 0xd801, 0x0061, 0x00a2 };
        const uint8_t to_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0xf4, 0xfe, 0xf4, 0xfe, 
            0x61, 0x8e, 0xe0,
        };
        int32_t fromEUC_JPOffs [] ={ 0, 1, 1, 2, 2, 2, 3, 3, 5, 5, 6, 7, 7};

        /*EUC_TW*/
        UChar euc_tw_inputText[]={ 0x0061, 0x2295, 0x5BF2, 0xd801, 0xdc01, 0xd801, 0x0061, 0x8706, 0x8a, };
        const uint8_t to_euc_tw[]={ 
            0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0xfd, 0xfe, 0xfd, 0xfe,
            0x61, 0xe6, 0xca, 0x8a,
        };
        int32_t from_euc_twOffs [] ={ 0, 1, 1, 2, 2, 2, 2, 3, 3, 5, 5, 6, 7, 7, 8,};

        if(!testConvertFromUnicode(inputTest, sizeof(inputTest)/sizeof(inputTest[0]),
                toIBM943, sizeof(toIBM943), "ibm-943",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, offset, NULL, 0 ))
            log_err("u-> ibm-943 with substitute did not match.\n");

        if(!testConvertFromUnicode(euc_jp_inputText, sizeof(euc_jp_inputText)/sizeof(euc_jp_inputText[0]),
                to_euc_jp, sizeof(to_euc_jp), "euc-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, fromEUC_JPOffs, NULL, 0 ))
            log_err("u-> euc-jp with substitute did not match.\n");

        if(!testConvertFromUnicode(euc_tw_inputText, sizeof(euc_tw_inputText)/sizeof(euc_tw_inputText[0]),
                to_euc_tw, sizeof(to_euc_tw), "euc-tw",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, from_euc_twOffs, NULL, 0 ))
            log_err("u-> euc-tw with substitute did not match.\n");  

    }
    log_verbose("Testing fromUnicode for SCSU with UCNV_FROM_U_CALLBACK_SUBSTITUTE \n");
    {
        UChar SCSU_inputText[]={ 0x0041, 0xd801/*illegal*/, 0x0042, };

        const uint8_t to_SCSU[]={    
            0x41,
            0x0e, 0xff,0xfd,
            0x42

           
        };
        int32_t from_SCSUOffs [] ={ 
            0,
            1,1,1,
            2,

        };
        if(!testConvertFromUnicode(SCSU_inputText, sizeof(SCSU_inputText)/sizeof(SCSU_inputText[0]),
                to_SCSU, sizeof(to_SCSU), "SCSU",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, from_SCSUOffs, NULL, 0 ))
            log_err("u-> SCSU with skip did not match.\n");

    
    }
    
    /*to unicode*/
    if(!testConvertToUnicode(expsubIBM_949, sizeof(expsubIBM_949),
             IBM_949subtoUnicode, sizeof(IBM_949subtoUnicode)/sizeof(IBM_949subtoUnicode[0]),"ibm-949",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM949Offs, NULL, 0 ))
        log_err("ibm-949->u with substitute did not match.\n");
    if(!testConvertToUnicode(expsubIBM_943, sizeof(expsubIBM_943),
             IBM_943subtoUnicode, sizeof(IBM_943subtoUnicode)/sizeof(IBM_943subtoUnicode[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM943Offs, NULL, 0 ))
        log_err("ibm-943->u with substitute did not match.\n");
    if(!testConvertToUnicode(expsubIBM_930, sizeof(expsubIBM_930),
             IBM_930subtoUnicode, sizeof(IBM_930subtoUnicode)/sizeof(IBM_930subtoUnicode[0]),"ibm-930",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM930Offs, NULL, 0 ))
        log_err("ibm-930->u with substitute did not match.\n");
 
    log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_SUBSTITUTE \n");
    {

        const uint8_t sampleTxtEBCIDIC_STATEFUL [] ={
            0x0e, 0x5d, 0x5f , 0x41, 0x79, 0x41, 0x44
        };
        UChar EBCIDIC_STATEFUL_toUnicode[] ={  0x6d63, 0xfffd, 0x03b4 
        };
        int32_t from_EBCIDIC_STATEFULOffsets []={ 1, 3, 5};


        /* EUC_JP*/
        const uint8_t sampleTxt_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0x8f, 0xda, 0xa1,  /*unassigned*/
           0x8e, 0xe0, 0x8a
        };
        UChar euc_jptoUnicode[]={ 0x0061, 0x4edd, 0x5bec, 0xfffd, 0x00a2, 0x008a };
        int32_t from_euc_jpOffs [] ={ 0, 1, 3, 6,  9, 11 };

        /*EUC_TW*/
        const uint8_t sampleTxt_euc_tw[]={ 
            0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0x8e, 0xaa, 0xbb, 0xcc,/*unassigned*/
            0xe6, 0xca, 0x8a,
        };
        UChar euc_twtoUnicode[]={ 0x0061, 0x2295, 0x5BF2, 0xfffd, 0x8706, 0x8a, };
        int32_t from_euc_twOffs [] ={ 0, 1, 3, 7, 11, 13};

       
        if(!testConvertToUnicode(sampleTxtEBCIDIC_STATEFUL, sizeof(sampleTxtEBCIDIC_STATEFUL),
           EBCIDIC_STATEFUL_toUnicode, sizeof(EBCIDIC_STATEFUL_toUnicode)/sizeof(EBCIDIC_STATEFUL_toUnicode[0]),"ibm-930",
          (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, from_EBCIDIC_STATEFULOffsets, NULL, 0 ))
            log_err("EBCIDIC_STATEFUL->u with substitute did not match.\n");


        if(!testConvertToUnicode(sampleTxt_euc_jp, sizeof(sampleTxt_euc_jp),
           euc_jptoUnicode, sizeof(euc_jptoUnicode)/sizeof(euc_jptoUnicode[0]),"euc-jp",
          (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, from_euc_jpOffs, NULL, 0 ))
            log_err("euc-jp->u with substitute did not match.\n");

        
        if(!testConvertToUnicode(sampleTxt_euc_tw, sizeof(sampleTxt_euc_tw),
           euc_twtoUnicode, sizeof(euc_twtoUnicode)/sizeof(euc_twtoUnicode[0]),"euc-tw",
          (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, from_euc_twOffs, NULL, 0 ))
            log_err("euc-tw->u with substitute  did not match.\n");


    }
    log_verbose("Testing toUnicode for UTF-8 with UCNV_TO_U_CALLBACK_SUBSTITUTE \n");
    {
        const uint8_t sampleText1[] = { 0x31, 0xe4, 0xba, 0x8c, 
            0xe0, 0x80,  0x61,};
        UChar    expected1[] = {  0x0031, 0x4e8c, 0xfffd, 0x0061};
        int32_t offsets1[] = {   0x0000, 0x0001, 0x0004, 0x0006};

        if(!testConvertToUnicode(sampleText1, sizeof(sampleText1),
                 expected1, sizeof(expected1)/sizeof(expected1[0]),"utf8",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, offsets1, NULL, 0 ))
            log_err("utf8->u with substitute did not match.\n");;
    }
    log_verbose("Testing toUnicode for SCSU with UCNV_TO_U_CALLBACK_SUBSTITUTE \n");
    {
        const uint8_t sampleText1[] = {  0xba, 0x8c,0xF8, 0x61,0x0c, 0x0c,};
        UChar    expected1[] = {  0x00ba,  0x008c,  0x00f8,  0x0061,0xfffd,0xfffd};
        int32_t offsets1[] = {   0x0000, 0x0001,0x0002,0x0003,4,5};

        if(!testConvertToUnicode(sampleText1, sizeof(sampleText1),
                 expected1, sizeof(expected1)/sizeof(expected1[0]),"SCSU",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, offsets1, NULL, 0 ))
            log_err("scsu->u with stop did not match.\n");;
    }

    log_verbose("Testing ibm-930 subchar/subchar1\n");
    {
        static const UChar u1[]={         0x6d63,           0x6d64,     0x6d65,     0x6d66,     0xdf };
        static const uint8_t s1[]={       0x0e, 0x5d, 0x5f, 0x5d, 0x63, 0xfe, 0xfe, 0x46, 0x6b, 0x0f, 0x3f };
        static const int32_t offsets1[]={ 0,    0,    0,    1,    1,    2,    2,    3,    3,    4,    4 };

        static const UChar u2[]={         0x6d63,           0x6d64,     0xfffd,     0x6d66,     0x1a };
        static const uint8_t s2[]={       0x0e, 0x5d, 0x5f, 0x5d, 0x63, 0xfc, 0xfc, 0x46, 0x6b, 0x0f, 0x57 };
        static const int32_t offsets2[]={ 1,                3,          5,          7,          10 };

        if(!testConvertFromUnicode(u1, ARRAY_LENGTH(u1), s1, ARRAY_LENGTH(s1), "ibm-930", 
                                   (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, offsets1, NULL, 0)
        ) {
            log_err("u->ibm-930 subchar/subchar1 did not match.\n");
        }

        if(!testConvertToUnicode(s2, ARRAY_LENGTH(s2), u2, ARRAY_LENGTH(u2), "ibm-930", 
                                 (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, offsets2, NULL, 0)
        ) {
            log_err("ibm-930->u subchar/subchar1 did not match.\n");
        }
    }

    log_verbose("Testing GB 18030 with substitute callbacks\n");
    {
        static const UChar u1[]={
            0x24, 0x7f, 0x80,                   0x1f9,      0x20ac,     0x4e00,     0x9fa6,                 0xffff,                 0xd800, 0xdc00,         0xdbff, 0xdfff };
        static const uint8_t gb1[]={
            0x24, 0x7f, 0x81, 0x30, 0x81, 0x30, 0xa8, 0xbf, 0xa2, 0xe3, 0xd2, 0xbb, 0x82, 0x35, 0x8f, 0x33, 0x84, 0x31, 0xa4, 0x39, 0x90, 0x30, 0x81, 0x30, 0xe3, 0x32, 0x9a, 0x35 };
        static const int32_t offsets1[]={
            0, 1, 2, 2, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 10, 10, 10, 10 };

        static const UChar u2[]={
            0x24, 0x7f, 0x80,                   0x1f9,      0x20ac,     0x4e00,     0x9fa6,                 0xffff,                 0xd800, 0xdc00,         0xfffd,                 0xdbff, 0xdfff };
        static const uint8_t gb2[]={
            0x24, 0x7f, 0x81, 0x30, 0x81, 0x30, 0xa8, 0xbf, 0xa2, 0xe3, 0xd2, 0xbb, 0x82, 0x35, 0x8f, 0x33, 0x84, 0x31, 0xa4, 0x39, 0x90, 0x30, 0x81, 0x30, 0xe3, 0x32, 0x9a, 0x36, 0xe3, 0x32, 0x9a, 0x35 };
        static const int32_t offsets2[]={
            0, 1, 2, 6, 8, 10, 12, 16, 20, 20, 24, 28, 28 };

        if(!testConvertFromUnicode(u1, ARRAY_LENGTH(u1), gb1, ARRAY_LENGTH(gb1), "gb18030", 
                                   (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, offsets1, NULL, 0)
        ) {
            log_err("u->gb18030 with substitute did not match.\n");
        }

        if(!testConvertToUnicode(gb2, ARRAY_LENGTH(gb2), u2, ARRAY_LENGTH(u2), "gb18030", 
                                 (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, offsets2, NULL, 0)
        ) {
            log_err("gb18030->u with substitute did not match.\n");
        }
    }

    log_verbose("Testing UTF-7 with substitute callbacks\n");
    {
        static const uint8_t utf7[]={
         /* a~            a+AB~                         a+AB\x0c                      a+AB-                         a+AB.                         a+. */
            0x61, 0x7e,   0x61, 0x2b, 0x41, 0x42, 0x7e, 0x61, 0x2b, 0x41, 0x42, 0x0c, 0x61, 0x2b, 0x41, 0x42, 0x2d, 0x61, 0x2b, 0x41, 0x42, 0x2e, 0x61, 0x2b, 0x2e
        };
        static const UChar unicode[]={
            0x61, 0xfffd, 0x61,       0xfffd,           0x61,       0xfffd,           0x61,       0xfffd,           0x61,       0xfffd,           0x61, 0xfffd
        };
        static const int32_t offsets[]={
            0,    1,      2,          4,                7,          9,                12,         14,               17,         19,               22,   23
        };

        if(!testConvertToUnicode(utf7, ARRAY_LENGTH(utf7), unicode, ARRAY_LENGTH(unicode), "UTF-7", 
                                 (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, offsets, NULL, 0)
        ) {
            log_err("UTF-7->u with substitute did not match.\n");
        }
    }
}

static void TestSubWithValue(int32_t inputsize, int32_t outputsize)
{
    UChar   sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
    UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };

    const uint8_t expsubwvalIBM_949[]= { 
        0x00, 0xb0, 0xa1, 0xb0, 0xa2,
        0x25, 0x55, 0x45, 0x46, 0x36, 0x37, 0xc8, 0xd3 }; 

    const uint8_t expsubwvalIBM_943[]= { 
        0x9f, 0xaf, 0x9f, 0xb1,
        0x25, 0x55, 0x36, 0x44, 0x36, 0x35, 0x89, 0x59 };

    const uint8_t expsubwvalIBM_930[] = {
        0x0e, 0x5d, 0x5f, 0x5d, 0x63, 0x0f, 0x6c, 0xe4, 0xf6, 0xc4, 0xf6, 0xf5, 0x0e, 0x46, 0x6b };

    int32_t toIBM949Offs [] ={ 0, 1, 1, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4};
    int32_t toIBM943Offs [] = { 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3};
    int32_t toIBM930Offs [] = { 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3}; /* last item: 3,3,3 because there's a shift plus a doublebyter .. */

    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;

    /*from Unicode*/
    if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
            expsubwvalIBM_949, sizeof(expsubwvalIBM_949), "ibm-949", 
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, toIBM949Offs, NULL, 0 ))
        log_err("u-> ibm-949 with subst with value did not match.\n");

    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expsubwvalIBM_943, sizeof(expsubwvalIBM_943), "ibm-943",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, toIBM943Offs, NULL, 0 ))
        log_err("u-> ibm-943 with sub with value did not match.\n");

    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
            expsubwvalIBM_930, sizeof(expsubwvalIBM_930), "ibm-930", 
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, toIBM930Offs, NULL, 0 ))
        log_err("u-> ibm-930 with subst with value did not match.\n");


    log_verbose("Testing fromUnicode with UCNV_FROM_U_CALLBACK_ESCAPE  \n");
    {
        UChar inputTest[] = { 0x0061, 0xd801, 0xdc01, 0xd801, 0x0061 };
        const uint8_t toIBM943[]= { 0x61, 
            0x25, 0x55, 0x44, 0x38, 0x30, 0x31,
            0x25, 0x55, 0x44, 0x43, 0x30, 0x31,
            0x25, 0x55, 0x44, 0x38, 0x30, 0x31,
            0x61 };
        int32_t offset[]= {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 4};


         /* EUC_JP*/
        UChar euc_jp_inputText[]={ 0x0061, 0x4edd, 0x5bec, 0xd801, 0xdc01, 0xd801, 0x0061, 0x00a2, };
        const uint8_t to_euc_jp[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0x25, 0x55, 0x44, 0x38, 0x30, 0x31,
            0x25, 0x55, 0x44, 0x43, 0x30, 0x31,
            0x25, 0x55, 0x44, 0x38, 0x30, 0x31,
            0x61, 0x8e, 0xe0,
        };
        int32_t fromEUC_JPOffs [] ={ 0, 1, 1, 2, 2, 2, 
            3, 3, 3, 3, 3, 3, 
            3, 3, 3, 3, 3, 3,
            5, 5, 5, 5, 5, 5,
            6, 7, 7,
        };

        /*EUC_TW*/
        UChar euc_tw_inputText[]={ 0x0061, 0x2295, 0x5BF2, 0xd801, 0xdc01, 0xd801, 0x0061, 0x8706, 0x8a, };
        const uint8_t to_euc_tw[]={ 
            0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0x25, 0x55, 0x44, 0x38, 0x30, 0x31,
            0x25, 0x55, 0x44, 0x43, 0x30, 0x31,
            0x25, 0x55, 0x44, 0x38, 0x30, 0x31,
            0x61, 0xe6, 0xca, 0x8a,
        };
        int32_t from_euc_twOffs [] ={ 0, 1, 1, 2, 2, 2, 2, 
             3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 5, 5,
             6, 7, 7, 8,
        };
        /*ISO-2022-JP*/
        UChar iso_2022_jp_inputText[]={ 0x0041, 0x00E9, 0x0042,0x00E9,0x3000 };
        const uint8_t to_iso_2022_jp[]={  
               0x41,   
               0x25,  0x55,   0x30,   0x30,   0x45,   0x39,   
               0x42,
               0x25,  0x55,   0x30,   0x30,   0x45,   0x39,
               0x1b,  0x24,   0x42,   0x21,   0x21, 
        };

        int32_t from_iso_2022_jpOffs [] ={ 
            0,
            1,1,1,1,1,1,
            2,
            3,3,3,3,3,3,
            4,4,4,4,4
        };
        UChar iso_2022_jp_inputText1[]={ 0x3000, 0x00E9, 0x3001,0x00E9, 0x0042} ;
        const uint8_t to_iso_2022_jp1[]={  
            0x1b,   0x24,   0x42,   0x21, 0x21,   
            0x1b,   0x28,   0x42,   0x25, 0x55,   0x30,   0x30,   0x45,   0x39,   
            0x1b,   0x24,   0x42,   0x21, 0x22,
            0x1b,   0x28,   0x42,   0x25, 0x55,   0x30,   0x30,   0x45,   0x39, 
            0x42,
        };

        int32_t from_iso_2022_jpOffs1 [] ={ 
            0,0,0,0,0,
            1,1,1,1,1,1,1,1,1,
            2,2,2,2,2,
            3,3,3,3,3,3,3,3,3,
            4,
        };
        /* surrogate pair*/
        UChar iso_2022_jp_inputText2[]={ 0x3000, 0xD84D, 0xDC56, 0x3001,0xD84D,0xDC56, 0x0042} ;
        const uint8_t to_iso_2022_jp2[]={  
                                0x1b,   0x24,   0x42,   0x21,   0x21,   
                                0x1b,   0x28,   0x42,   0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
                                0x25,   0x55,   0x44,   0x43,   0x35,   0x36,   
                                0x1b,   0x24,   0x42,   0x21,   0x22,
                                0x1b,   0x28,   0x42,   0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
                                0x25,   0x55,   0x44,   0x43,   0x35,   0x36,   
                                0x42,
                                };
        int32_t from_iso_2022_jpOffs2 [] ={ 
            0,0,0,0,0,
            1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,
            3,3,3,3,3,
            4,4,4,4,4,4,4,4,4,
            4,4,4,4,4,4,
            6,
        };

        /*ISO-2022-cn*/
        UChar iso_2022_cn_inputText[]={ 0x0041, 0x3712, 0x0042, };
        const uint8_t to_iso_2022_cn[]={  
            0x0F,   0x41,   
            0x0f,   0x25, 0x55,   0x33,   0x37,   0x31,   0x32,   
            0x0F,   0x42, 
        };
        int32_t from_iso_2022_cnOffs [] ={ 
            0,0,
            1,1,1,1,1,1,1,
            2,2,
        };
        UChar iso_2022_cn_inputText1[]={ 0x4e00, 0x3712, 0x4e01, };
        const uint8_t to_iso_2022_cn1[]={  
                        0x1b,   0x24,   0x29,   0x41,   0x0e,   0x52,   0x3b,   
                        0x0f,   0x25,   0x55,   0x33,   0x37,   0x31,   0x32, 
                        0x1b,   0x24,   0x29,   0x41,   0x0e,   0x36,   0x21,
        };
        int32_t from_iso_2022_cnOffs1 [] ={ 
                0, 0, 0, 0, 0, 0, 0, 
                1, 1, 1, 1, 1, 1, 1, 
                2, 2, 2, 2, 2, 2, 2,
        };
        UChar iso_2022_cn_inputText3[]={ 0x3000, 0x3712, 0x3001, };
        const uint8_t to_iso_2022_cn3[]={  
               0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x21,   
              0x0f,   0x25,   0x55,   0x33,   0x37,   0x31,   0x32,   
             0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x22, 
        };
        int32_t from_iso_2022_cnOffs3 [] ={ 
            0,0,0,0,0,0,0,
            1,1,1,1,1,1,1,
            2,2,2,2,2,2,2
        };
        UChar iso_2022_cn_inputText2[]={ 0x0041, 0x3712, 0x4e00, };
        const uint8_t to_iso_2022_cn2[]={  
            0x0F,   0x41,   
            0x0f,   0x25,   0x55,   0x33,   0x37,   0x31,   0x32,   
            0x1b,   0x24,   0x29,   0x41,   0x0e,   0x52,   0x3b,
        };
        int32_t from_iso_2022_cnOffs2 [] ={ 
            0,0,
            1,1,1,1,1,1,1,
            2,2,2,2,2,2,2
        };
        
        UChar iso_2022_cn_inputText4[]={ 0x3000, 0xD84D, 0xDC56, 0x3001,0xD84D,0xDC56, 0x0042};

        const uint8_t to_iso_2022_cn4[]={  
                             0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x21,   
                             0x0f,   0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
                             0x25,   0x55,   0x44,   0x43,   0x35,   0x36,   
                             0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x22,   
                             0x0f,   0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
                             0x25,   0x55,   0x44,   0x43,   0x35,   0x36,   
                             0x0f,   0x42, 
                             };
        int32_t from_iso_2022_cnOffs4 [] ={ 
            0,0,0,0,0,0,0,
            1,1,1,1,1,1,1,
            1,1,1,1,1,1,
            3,3,3,3,3,3,3,
            4,4,4,4,4,4,4,
            4,4,4,4,4,4,
            6, 6

        };

        /*ISO-2022-kr*/
        UChar iso_2022_kr_inputText2[]={ 0x0041, 0x03A0,0xD84D, 0xDC56/*unassigned*/,0x03A0, 0x0042,0xD84D, 0xDC56/*unassigned*/,0x43 };
        const uint8_t to_iso_2022_kr2[]={  
            0x1b,   0x24,   0x29,   0x43,   
            0x41,   
            0x0e,   0x25,   0x50,   
            0x0f,   0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
            0x25,   0x55,   0x44,   0x43,   0x35,   0x36,  
            0x0e,   0x25,   0x50, 
            0x0f,   0x42, 
            0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
            0x25,   0x55,   0x44,   0x43,   0x35,   0x36,  
            0x43
        };
        int32_t from_iso_2022_krOffs2 [] ={ 
            -1,-1,-1,-1,
             0,
            1,1,1,
            2,2,2,2,2,2,2,
            2,2,2,2,2,2,
            4,4,4,
            5,5,
            6,6,6,6,6,6,
            6,6,6,6,6,6,
            8,
        };

        UChar iso_2022_kr_inputText[]={ 0x0041, 0x03A0,0x3712/*unassigned*/,0x03A0, 0x0042,0x3712/*unassigned*/,0x43 };
        const uint8_t to_iso_2022_kr[]={  
            0x1b,   0x24,   0x29,   0x43,   
            0x41,   
            0x0e,   0x25,   0x50,   
            0x0f,   0x25,   0x55,   0x33,   0x37,   0x31,   0x32,  /*unassigned*/ 
            0x0e,   0x25,   0x50, 
            0x0f,   0x42, 
            0x25,   0x55,   0x33,   0x37,   0x31,   0x32,  /*unassigned*/ 
            0x43
        };
     

        int32_t from_iso_2022_krOffs [] ={ 
            -1,-1,-1,-1,
             0,
            1,1,1,
            2,2,2,2,2,2,2,
            3,3,3,
            4,4,
            5,5,5,5,5,5,
            6,
        };
        /* HZ encoding */       
        UChar hz_inputText[]={ 0x0041, 0x03A0,0x0662/*unassigned*/,0x03A0, 0x0042, };

        const uint8_t to_hz[]={    
            0x7e,   0x7d,   0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
            0x7e,   0x7d,   0x25,   0x55,   0x30,   0x36,   0x36,   0x32,  /*unassigned*/ 
            0x7e,   0x7b,   0x26,   0x30,
            0x7e,   0x7d,   0x42, 
           
        };
        int32_t from_hzOffs [] ={ 
            0,0,0,
            1,1,1,1,
            2,2,2,2,2,2,2,2,
            3,3,3,3,
            4,4,4
        };

        UChar hz_inputText2[]={ 0x0041, 0x03A0,0xD84D, 0xDC56/*unassigned*/,0x03A0, 0x0042,0xD84D, 0xDC56/*unassigned*/,0x43 };
        const uint8_t to_hz2[]={   
            0x7e,   0x7d,   0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
            0x7e,   0x7d,   0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
            0x25,   0x55,   0x44,   0x43,   0x35,   0x36,  
            0x7e,   0x7b,   0x26,   0x30,
            0x7e,   0x7d,   0x42, 
            0x25,   0x55,   0x44,   0x38,   0x34,   0x44,   
            0x25,   0x55,   0x44,   0x43,   0x35,   0x36,  
            0x43
        };
        int32_t from_hzOffs2 [] ={ 
            0,0,0,
            1,1,1,1,
            2,2,2,2,2,2,2,2,
            2,2,2,2,2,2,
            4,4,4,4,
            5,5,5,
            6,6,6,6,6,6,
            6,6,6,6,6,6,
            8,
        };

        if(!testConvertFromUnicode(inputTest, sizeof(inputTest)/sizeof(inputTest[0]),
                toIBM943, sizeof(toIBM943), "ibm-943",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, offset, NULL, 0 ))
            log_err("u-> ibm-943 with subst with value did not match.\n");

        if(!testConvertFromUnicode(euc_jp_inputText, sizeof(euc_jp_inputText)/sizeof(euc_jp_inputText[0]),
                to_euc_jp, sizeof(to_euc_jp), "euc-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, fromEUC_JPOffs, NULL, 0 ))
            log_err("u-> euc-jp with subst with value did not match.\n");

        if(!testConvertFromUnicode(euc_tw_inputText, sizeof(euc_tw_inputText)/sizeof(euc_tw_inputText[0]),
                to_euc_tw, sizeof(to_euc_tw), "euc-tw",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_euc_twOffs, NULL, 0 ))
            log_err("u-> euc-tw with subst with value did not match.\n");  
        
        if(!testConvertFromUnicode(iso_2022_jp_inputText, sizeof(iso_2022_jp_inputText)/sizeof(iso_2022_jp_inputText[0]),
                to_iso_2022_jp, sizeof(to_iso_2022_jp), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs, NULL, 0 ))
            log_err("u-> iso_2022_jp with subst with value did not match.\n"); 
     
        if(!testConvertFromUnicode(iso_2022_jp_inputText1, sizeof(iso_2022_jp_inputText1)/sizeof(iso_2022_jp_inputText1[0]),
                to_iso_2022_jp1, sizeof(to_iso_2022_jp1), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs1, NULL, 0 ))
            log_err("u-> iso_2022_jp with subst with value did not match.\n"); 
        
        if(!testConvertFromUnicode(iso_2022_jp_inputText1, sizeof(iso_2022_jp_inputText1)/sizeof(iso_2022_jp_inputText1[0]),
                to_iso_2022_jp1, sizeof(to_iso_2022_jp1), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs1, NULL, 0 ))
            log_err("u-> iso_2022_jp with subst with value did not match.\n"); 
        
        if(!testConvertFromUnicode(iso_2022_jp_inputText2, sizeof(iso_2022_jp_inputText2)/sizeof(iso_2022_jp_inputText2[0]),
                to_iso_2022_jp2, sizeof(to_iso_2022_jp2), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs2, NULL, 0 ))
            log_err("u-> iso_2022_jp with subst with value did not match.\n");
        /*ESCAPE OPTIONS*/
        {
            const uint8_t to_iso_2022_jp2[]={  
                    0x1b,   0x24,   0x42,   0x21,   0x21,   
                    0x1b,   0x28,   0x42,   0x26,   0x23,   0x31,  0x34,   0x34,   0x34,   0x37, 0x30,   
                      
                    0x1b,   0x24,   0x42,   0x21,   0x22,
                    0x1b,   0x28,   0x42,   0x26,   0x23,  0x31,  0x34,   0x34,   0x34,   0x37, 0x30,  
                    
                    0x42,
                    };

            int32_t from_iso_2022_jpOffs2 [] ={ 
                0,0,0,0,0,
                1,1,1,1,1,1,1,1,1,1,1,

                3,3,3,3,3,
                4,4,4,4,4,4,4,4,4,4,4,

                6,
            };

            if(!testConvertFromUnicodeWithContext(iso_2022_jp_inputText2, sizeof(iso_2022_jp_inputText2)/sizeof(iso_2022_jp_inputText2[0]),
                    to_iso_2022_jp2, sizeof(to_iso_2022_jp2), "iso-2022-jp",
                    (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs2, NULL, 0,UCNV_ESCAPE_XML_DEC,U_ZERO_ERROR ))
                log_err("u-> iso-2022-jp with skip & UCNV_ESCAPE_XML_DEC did not match.\n"); 
        }
        {
             const uint8_t to_iso_2022_jp2[]={  
                    0x1b,   0x24,   0x42,   0x21,   0x21,   
                    0x1b,   0x28,   0x42,   0x26,   0x23,   0x78,  0x32,   0x33,   0x34,   0x35, 0x36,   
                      
                    0x1b,   0x24,   0x42,   0x21,   0x22,
                    0x1b,   0x28,   0x42,   0x26,   0x23,   0x78,  0x32,   0x33,   0x34,   0x35, 0x36,  
                    
                    0x42,
                    };

            int32_t from_iso_2022_jpOffs2 [] ={ 
                0,0,0,0,0,
                1,1,1,1,1,1,1,1,1,1,1,

                3,3,3,3,3,
                4,4,4,4,4,4,4,4,4,4,4,

                6,
            };
            if(!testConvertFromUnicodeWithContext(iso_2022_jp_inputText2, sizeof(iso_2022_jp_inputText2)/sizeof(iso_2022_jp_inputText2[0]),
                to_iso_2022_jp2, sizeof(to_iso_2022_jp2), "iso-2022-jp",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs2, NULL, 0,UCNV_ESCAPE_XML_HEX,U_ZERO_ERROR ))
                log_err("u-> iso-2022-jp with skip & UCNV_ESCAPE_XML_HEX did not match.\n"); 

        }
        {             
            const uint8_t to_iso_2022_cn4[]={  
                             0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x21,   
                             0x0f,   0x5c,   0x75,   0x44,   0x38,   0x34,   0x44,   
                             0x5c,   0x75,   0x44,   0x43,   0x35,   0x36,   
                             0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x22,   
                             0x0f,   0x5c,   0x75,   0x44,   0x38,   0x34,   0x44,   
                             0x5c,   0x75,   0x44,   0x43,   0x35,   0x36,   
                             0x0f,   0x42, 
                             };
            int32_t from_iso_2022_cnOffs4 [] ={ 
                0,0,0,0,0,0,0,
                1,1,1,1,1,1,1,
                1,1,1,1,1,1,
                3,3,3,3,3,3,3,
                4,4,4,4,4,4,4,
                4,4,4,4,4,4,
                6, 6

            };
            if(!testConvertFromUnicodeWithContext(iso_2022_cn_inputText4, sizeof(iso_2022_cn_inputText4)/sizeof(iso_2022_cn_inputText4[0]),
                to_iso_2022_cn4, sizeof(to_iso_2022_cn4), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs4, NULL, 0,UCNV_ESCAPE_JAVA,U_ZERO_ERROR ))
                log_err("u-> iso-2022-cn with skip & UCNV_ESCAPE_JAVA did not match.\n"); 

        }
        {
            const uint8_t to_iso_2022_cn4[]={  
                            0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x21,   
                            0x0f,   0x5c,   0x75,   0x30,   0x30,   0x30,   0x32,   0x33,   0x34,   0x35,   0x36,   
                            0x1b,   0x24,   0x29,   0x41,   0x0e,   0x21,   0x22,
                            0x0f,   0x5c,   0x75,   0x30,   0x30,   0x30,   0x32,   0x33,   0x34,   0x35,   0x36, 
                            0x0f,   0x42 
                             };


            int32_t from_iso_2022_cnOffs4 [] ={ 
                0,0,0,0,0,0,0,
                1,1,1,1,1,1,1,1,1,1,1,

                3,3,3,3,3,3,3,
                4,4,4,4,4,4,4,4,4,4,4,

                6,6

            };
            if(!testConvertFromUnicodeWithContext(iso_2022_cn_inputText4, sizeof(iso_2022_cn_inputText4)/sizeof(iso_2022_cn_inputText4[0]),
                to_iso_2022_cn4, sizeof(to_iso_2022_cn4), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs4, NULL, 0,UCNV_ESCAPE_C,U_ZERO_ERROR ))
                log_err("u-> iso-2022-cn with skip & UCNV_ESCAPE_C did not match.\n"); 
        }
        if(!testConvertFromUnicode(iso_2022_cn_inputText, sizeof(iso_2022_cn_inputText)/sizeof(iso_2022_cn_inputText[0]),
                to_iso_2022_cn, sizeof(to_iso_2022_cn), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs, NULL, 0 ))
            log_err("u-> iso_2022_cn with subst with value did not match.\n");

        if(!testConvertFromUnicode(iso_2022_cn_inputText1, sizeof(iso_2022_cn_inputText1)/sizeof(iso_2022_cn_inputText1[0]),
                to_iso_2022_cn1, sizeof(to_iso_2022_cn1), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs1, NULL, 0 ))
            log_err("u-> iso_2022_cn with subst with value did not match.\n"); 
        if(!testConvertFromUnicode(iso_2022_cn_inputText2, sizeof(iso_2022_cn_inputText2)/sizeof(iso_2022_cn_inputText2[0]),
                to_iso_2022_cn2, sizeof(to_iso_2022_cn2), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs2, NULL, 0 ))
            log_err("u-> iso_2022_cn with subst with value did not match.\n");
        if(!testConvertFromUnicode(iso_2022_cn_inputText3, sizeof(iso_2022_cn_inputText3)/sizeof(iso_2022_cn_inputText3[0]),
                to_iso_2022_cn3, sizeof(to_iso_2022_cn3), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs3, NULL, 0 ))
            log_err("u-> iso_2022_cn with subst with value did not match.\n");
        if(!testConvertFromUnicode(iso_2022_cn_inputText4, sizeof(iso_2022_cn_inputText4)/sizeof(iso_2022_cn_inputText4[0]),
                to_iso_2022_cn4, sizeof(to_iso_2022_cn4), "iso-2022-cn",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs4, NULL, 0 ))
            log_err("u-> iso_2022_cn with subst with value did not match.\n");
        if(!testConvertFromUnicode(iso_2022_kr_inputText, sizeof(iso_2022_kr_inputText)/sizeof(iso_2022_kr_inputText[0]),
                to_iso_2022_kr, sizeof(to_iso_2022_kr), "iso-2022-kr",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_krOffs, NULL, 0 ))
            log_err("u-> iso_2022_kr with subst with value did not match.\n");
        if(!testConvertFromUnicode(iso_2022_kr_inputText2, sizeof(iso_2022_kr_inputText2)/sizeof(iso_2022_kr_inputText2[0]),
                to_iso_2022_kr2, sizeof(to_iso_2022_kr2), "iso-2022-kr",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_iso_2022_krOffs2, NULL, 0 ))
            log_err("u-> iso_2022_kr2 with subst with value did not match.\n");
        if(!testConvertFromUnicode(hz_inputText, sizeof(hz_inputText)/sizeof(hz_inputText[0]),
                to_hz, sizeof(to_hz), "HZ",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_hzOffs, NULL, 0 ))
            log_err("u-> hz with subst with value did not match.\n");
         if(!testConvertFromUnicode(hz_inputText2, sizeof(hz_inputText2)/sizeof(hz_inputText2[0]),
                to_hz2, sizeof(to_hz2), "HZ",
                (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, from_hzOffs2, NULL, 0 ))
            log_err("u-> hz with subst with value did not match.\n");
    }


    log_verbose("Testing toUnicode with UCNV_FROM_U_CALLBACK_ESCAPE \n");
    /*to Unicode*/
    {
        const uint8_t sampleTxtToU[]= { 0x00, 0x9f, 0xaf, 
            0x81, 0xad, /*unassigned*/
            0x89, 0xd3 };
        UChar IBM_943toUnicode[] = { 0x0000, 0x6D63, 
            0x25, 0x58, 0x38, 0x31, 0x25, 0x58, 0x41, 0x44,
            0x7B87};
        int32_t  fromIBM943Offs [] =    { 0, 1, 3, 3, 3, 3, 3, 3, 3, 3, 5};

        /* EUC_JP*/
        const uint8_t sampleTxt_EUC_JP[]={ 0x61, 0xa1, 0xb8, 0x8f, 0xf4, 0xae,
            0x8f, 0xda, 0xa1,  /*unassigned*/
           0x8e, 0xe0,
        };
        UChar EUC_JPtoUnicode[]={ 0x0061, 0x4edd, 0x5bec,
            0x25, 0x58, 0x38, 0x46, 0x25, 0x58, 0x44, 0x41, 0x25, 0x58, 0x41, 0x31, 
            0x00a2 };
        int32_t fromEUC_JPOffs [] ={ 0, 1, 3, 
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            9,
        };

        /*EUC_TW*/
        const uint8_t sampleTxt_euc_tw[]={ 
            0x61, 0xa2, 0xd3, 0x8e, 0xa2, 0xdc, 0xe5,
            0x8e, 0xaa, 0xbb, 0xcc,/*unassigned*/
            0xe6, 0xca, 0x8a,
        };
        UChar euc_twtoUnicode[]={ 0x0061, 0x2295, 0x5BF2, 
             0x25, 0x58, 0x38, 0x45, 0x25, 0x58, 0x41, 0x41, 0x25, 0x58, 0x42, 0x42, 0x25, 0x58, 0x43, 0x43, 
             0x8706, 0x8a, };
        int32_t from_euc_twOffs [] ={ 0, 1, 3, 
             7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
             11, 13};
        
        /*iso-2022-jp*/
        const uint8_t sampleTxt_iso_2022_jp[]={ 
            0x1b,   0x28,   0x42,   0x41,
            0x1b,   0x24,   0x42,   0x2A, 0x44, /*unassigned*/
            0x1b,   0x28,   0x42,   0x42,
            
        };
        UChar iso_2022_jptoUnicode[]={    0x41,0x25,0x58,0x32,0x41,0x25,0x58,0x34,0x34, 0x42 };
        int32_t from_iso_2022_jpOffs [] ={  3,   7,   7,   7,   7,   7,   7,   7,   7,    12   };
        
        /*iso-2022-cn*/
        const uint8_t sampleTxt_iso_2022_cn[]={ 
            0x0f,   0x41,   0x44,
            0x1B,   0x24,   0x29,   0x47, 
            0x0E,   0x40,   0x6c, /*unassigned*/
            0x0f,   0x42,
            
        };
        UChar iso_2022_cntoUnicode[]={    0x41, 0x44,0x25,0x58,0x34,0x30,0x25,0x58,0x36,0x43,0x42 };
        int32_t from_iso_2022_cnOffs [] ={  1,   2,   8,   8,   8,   8,   8,   8,   8,  8,    11   };

        /*iso-2022-kr*/
        const uint8_t sampleTxt_iso_2022_kr[]={ 
          0x1b, 0x24, 0x29,  0x43,
          0x41,
          0x0E, 0x7f, 0x1E,
          0x0e, 0x25, 0x50, 
          0x0f, 0x51,
          0x42, 0x43,
            
        };
        UChar iso_2022_krtoUnicode[]={     0x41,0x25,0x58,0x37,0x46,0x25,0x58,0x31,0x45,0x03A0,0x51, 0x42,0x43};
        int32_t from_iso_2022_krOffs [] ={  4,   6,   6,   6,   6,   6,   6,   6,   6,    9,    12,   13  , 14 };
        
        /*hz*/
        const uint8_t sampleTxt_hz[]={ 
            0x41,   
            0x7e,   0x7b,   0x26,   0x30,   
            0x7f,   0x1E, /*unassigned*/ 
            0x26,   0x30,
            0x7e,   0x7d,   0x42, 
            0x7e,   0x7b,   0x7f,   0x1E,/*unassigned*/ 
            0x7e,   0x7d,   0x42,
        };
        UChar hztoUnicode[]={  
            0x41,
            0x03a0,
            0x25,0x58,0x37,0x46,0x25,0x58,0x31,0x45,
            0x03A0, 
            0x42,
            0x25,0x58,0x37,0x46,0x25,0x58,0x31,0x45,
            0x42,};

        int32_t from_hzOffs [] ={0,3,5,5,5,5,5,5,5,5,7,11,14,14,14,14,14,14,14,14,18,  };

        /*LMBCS*/
        const uint8_t sampleTxtLMBCS[]={ 0x12, 0xc9, 0x50, 
            0x12, 0x92, 0xa0, /*unassigned*/
            0x12, 0x92, 0xa1,
        };
        UChar LMBCSToUnicode[]={ 0x4e2e, 
            0x25, 0x58, 0x31, 0x32, 0x25, 0x58, 0x39, 0x32, 0x25, 0x58, 0x41, 0x30, 
            0xe5c4, };
        int32_t fromLMBCS[] = {0, 
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            6, };
       
        
        if(!testConvertToUnicode(sampleTxtToU, sizeof(sampleTxtToU),
                 IBM_943toUnicode, sizeof(IBM_943toUnicode)/sizeof(IBM_943toUnicode[0]),"ibm-943",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromIBM943Offs, NULL, 0 ))
            log_err("ibm-943->u with substitute with value did not match.\n");

        if(!testConvertToUnicode(sampleTxt_EUC_JP, sizeof(sampleTxt_EUC_JP),
                 EUC_JPtoUnicode, sizeof(EUC_JPtoUnicode)/sizeof(EUC_JPtoUnicode[0]),"euc-jp",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromEUC_JPOffs, NULL, 0))
            log_err("euc-jp->u with substitute with value did not match.\n");

        if(!testConvertToUnicode(sampleTxt_euc_tw, sizeof(sampleTxt_euc_tw),
                 euc_twtoUnicode, sizeof(euc_twtoUnicode)/sizeof(euc_twtoUnicode[0]),"euc-tw",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, from_euc_twOffs, NULL, 0))
            log_err("euc-tw->u with substitute with value did not match.\n");
            
        if(!testConvertToUnicode(sampleTxt_iso_2022_jp, sizeof(sampleTxt_iso_2022_jp),
                 iso_2022_jptoUnicode, sizeof(iso_2022_jptoUnicode)/sizeof(iso_2022_jptoUnicode[0]),"iso-2022-jp",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, from_iso_2022_jpOffs, NULL, 0))
            log_err("iso-2022-jp->u with substitute with value did not match.\n");
        
        if(!testConvertToUnicode(sampleTxt_iso_2022_cn, sizeof(sampleTxt_iso_2022_cn),
                 iso_2022_cntoUnicode, sizeof(iso_2022_cntoUnicode)/sizeof(iso_2022_cntoUnicode[0]),"iso-2022-cn",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, from_iso_2022_cnOffs, NULL, 0))
            log_err("iso-2022-cn->u with substitute with value did not match.\n");
        
        if(!testConvertToUnicode(sampleTxt_iso_2022_kr, sizeof(sampleTxt_iso_2022_kr),
                 iso_2022_krtoUnicode, sizeof(iso_2022_krtoUnicode)/sizeof(iso_2022_krtoUnicode[0]),"iso-2022-kr",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, from_iso_2022_krOffs, NULL, 0))
            log_err("iso-2022-kr->u with substitute with value did not match.\n");

         if(!testConvertToUnicode(sampleTxt_hz, sizeof(sampleTxt_hz),
                 hztoUnicode, sizeof(hztoUnicode)/sizeof(hztoUnicode[0]),"HZ",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, from_hzOffs, NULL, 0))
            log_err("iso-2022-kr->u with substitute with value did not match.\n");

        /*got to confirm this*/
        if(/* broken for icu 1.6.0.1, do not test */uprv_strcmp("1.7", U_ICU_VERSION) != 0 && !testConvertToUnicode(sampleTxtLMBCS, sizeof(sampleTxtLMBCS),
                LMBCSToUnicode, sizeof(LMBCSToUnicode)/sizeof(LMBCSToUnicode[0]),"LMBCS",
                (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromLMBCS, NULL, 0))
            log_err("LMBCS->u with substitute with value did not match.\n"); 
    }
}

static void TestLegalAndOthers(int32_t inputsize, int32_t outputsize)
{
    UChar    legalText[] =  { 0x0000, 0xAC00, 0xAC01, 0xD700 };
    const uint8_t templegal949[] ={ 0x00, 0xb0, 0xa1, 0xb0, 0xa2, 0xc8, 0xd3 };
    int32_t  to949legal[] = {0, 1, 1, 2, 2, 3, 3};


    const uint8_t text943[] = {
        0x82, 0xa9, 0x82, 0x20, /*0xc8,*/  0x61, 0x8a, 0xbf, 0x8e, 0x9a };
        UChar toUnicode943sub[] = { 0x304b, 0xfffd, /*0xff88,*/ 0x0061, 0x6f22,  0x5b57};
        UChar toUnicode943skip[]= { 0x304b, /*0xff88,*/ 0x0061, 0x6f22,  0x5b57};
        UChar toUnicode943stop[]= { 0x304b};

    int32_t  fromIBM943Offssub[]  = {0, 2, 4, 5, 7};
    int32_t  fromIBM943Offsskip[] = { 0, 4, 5, 7};
    int32_t  fromIBM943Offsstop[] = { 0};

    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;
    /*checking with a legal value*/
    if(!testConvertFromUnicode(legalText, sizeof(legalText)/sizeof(legalText[0]),
            templegal949, sizeof(templegal949), "ibm-949",
            (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, to949legal, NULL, 0 ))
        log_err("u-> ibm-949 with skip did not match.\n");

    /*checking illegal value for ibm-943 with substitute*/ 
    if(!testConvertToUnicode(text943, sizeof(text943),
             toUnicode943sub, sizeof(toUnicode943sub)/sizeof(toUnicode943sub[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM943Offssub, NULL, 0 ))
        log_err("ibm-943->u with subst did not match.\n");
    /*checking illegal value for ibm-943 with skip */
    if(!testConvertToUnicode(text943, sizeof(text943),
             toUnicode943skip, sizeof(toUnicode943skip)/sizeof(toUnicode943skip[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM943Offsskip, NULL, 0 ))
        log_err("ibm-943->u with skip did not match.\n");

    /*checking illegal value for ibm-943 with stop */
    if(!testConvertToUnicode(text943, sizeof(text943),
             toUnicode943stop, sizeof(toUnicode943stop)/sizeof(toUnicode943stop[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM943Offsstop, NULL, 0 ))
        log_err("ibm-943->u with stop did not match.\n");

}

static void TestSingleByte(int32_t inputsize, int32_t outputsize)
{
    const uint8_t sampleText[] = {
        0x82, 0xa9, 0x61, 0x62, 0x63 , 0x82,
        0xff, /*0x82, 0xa9,*/ 0x32, 0x33};
    UChar toUnicode943sub[] = {0x304b, 0x0061, 0x0062, 0x0063,  0xfffd,/*0x304b,*/ 0x0032, 0x0033};
    int32_t  fromIBM943Offssub[]  = {0, 2, 3, 4, 5, 7, 8};
    /*checking illegal value for ibm-943 with substitute*/ 
    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;

    if(!testConvertToUnicode(sampleText, sizeof(sampleText),
             toUnicode943sub, sizeof(toUnicode943sub)/sizeof(toUnicode943sub[0]),"ibm-943",
            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM943Offssub, NULL, 0 ))
        log_err("ibm-943->u with subst did not match.\n");
}

static void TestEBCDIC_STATEFUL_Sub(int32_t inputsize, int32_t outputsize)
{
    /*EBCDIC_STATEFUL*/
    UChar ebcdic_inputTest[] = { 0x0061, 0x6d64, 0x0061, 0x00A2, 0x6d65, 0x0061 };
    const uint8_t toIBM930[]= { 0x62, 0x0e, 0x5d, 0x63, 0x0f, 0x62, 0xb1, 0x0e, 0xfe, 0xfe, 0x0f, 0x62 };
    int32_t offset_930[]=     { 0,    1,    1,    1,    2,    2,    3,    4,    4,    4,    5,    5    };
/*                              s     SO    doubl       SI    sng   s     SO    fe    fe    SI    s    */

    /*EBCDIC_STATEFUL with subChar=3f*/
    const uint8_t toIBM930_subvaried[]= { 0x62, 0x0e, 0x5d, 0x63, 0x0f, 0x62, 0xb1, 0x3f, 0x62 };
    int32_t offset_930_subvaried[]=     { 0,    1,    1,    1,    2,    2,    3,    4,    5    };
    const char mySubChar[]={ 0x3f};

    gInBufferSize = inputsize;
    gOutBufferSize = outputsize;

    if(!testConvertFromUnicode(ebcdic_inputTest, sizeof(ebcdic_inputTest)/sizeof(ebcdic_inputTest[0]),
        toIBM930, sizeof(toIBM930), "ibm-930",
        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, offset_930, NULL, 0 ))
            log_err("u-> ibm-930(EBCDIC_STATEFUL) with subst did not match.\n");
    
    if(!testConvertFromUnicode(ebcdic_inputTest, sizeof(ebcdic_inputTest)/sizeof(ebcdic_inputTest[0]),
        toIBM930_subvaried, sizeof(toIBM930_subvaried), "ibm-930",
        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, offset_930_subvaried, mySubChar, 1 ))
            log_err("u-> ibm-930(EBCDIC_STATEFUL) with subst(setSubChar=0x3f) did not match.\n");
}



UBool testConvertFromUnicode(const UChar *source, int sourceLen,  const uint8_t *expect, int expectLen, 
                const char *codepage, UConverterFromUCallback callback , const int32_t *expectOffsets, 
                const char *mySubChar, int8_t len)
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
    int32_t  realBufferSize;
    uint8_t *realBufferEnd;
    const UChar *realSourceEnd;
    const UChar *sourceLimit;
    UBool checkOffsets = TRUE;
    UBool doFlush;
    char junk[9999];
    char offset_str[9999];
    uint8_t *p;
    UConverterFromUCallback oldAction = NULL;
    void* oldContext = NULL;


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
    ucnv_setFromUCallBack (conv, callback, NULL, &oldAction, &oldContext, &status);
    if (U_FAILURE(status)) 
    { 
        log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));
    }
    /*------------------------*/
    /*setting the subChar*/
    if(mySubChar != NULL){
        ucnv_setSubstChars(conv, mySubChar, len, &status);
        if (U_FAILURE(status))  { 
            log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));
        }
    }
    /*------------*/

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
    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (U_SUCCESS(status) && (sourceLimit < realSourceEnd)) );

    /* allow failure codes for the stop callback */
    if(U_FAILURE(status) &&
       (callback != UCNV_FROM_U_CALLBACK_STOP || (status != U_INVALID_CHAR_FOUND && status != U_ILLEGAL_CHAR_FOUND)))
    {
        log_err("Problem in fromUnicode, errcode %s %s\n", myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-junkout);
    if(VERBOSITY)
    {

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
        log_verbose("comparing %d offsets..\n", targ-junkout);
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t) )){
            log_err("did not get the expected offsets while %s \n", gNuConvTestName);
            log_err("Got Output : ");
            printSeqErr(junkout, targ-junkout);
            log_err("Got Offsets:      ");
            for(p=junkout;p<targ;p++)
                log_err("%d,", junokout[p-junkout]); 
            log_err("\n");
            log_err("Expected Offsets: ");
            for(i=0; i<(targ-junkout); i++)
                log_err("%d,", expectOffsets[i]);
            log_err("\n");
            return FALSE;
        }
    }

    if(!memcmp(junkout, expect, expectLen))
    {
        log_verbose("String matches! %s\n", gNuConvTestName);
        return TRUE;
    }
    else
    {
        log_err("String does not match. %s\n", gNuConvTestName);
        log_err("source: ");
        printUSeqErr(source, sourceLen);
        log_err("Got:      ");
        printSeqErr(junkout, expectLen);
        log_err("Expected: ");
        printSeqErr(expect, expectLen);
        return FALSE;
    }
}
UBool testConvertToUnicode( const uint8_t *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, UConverterToUCallback callback, const int32_t *expectOffsets,
               const char *mySubChar, int8_t len)
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
    UConverterToUCallback oldAction = NULL;
    void* oldContext = NULL;

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
    /*----setting the callback routine----*/
    ucnv_setToUCallBack (conv, callback, NULL, &oldAction, &oldContext, &status);
    if (U_FAILURE(status)) 
    { 
        log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));  
    }
    /*-------------------------------------*/
    /*setting the subChar*/
    if(mySubChar != NULL){
        ucnv_setSubstChars(conv, mySubChar, len, &status);
        if (U_FAILURE(status))  { 
            log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status)); 
        }
    }
    /*------------*/


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
                (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of the source data */
                &status);
    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (U_SUCCESS(status) && (srcLimit < realSourceEnd)) ); /* while we just need another buffer */

    /* allow failure codes for the stop callback */
    if(U_FAILURE(status) &&
       (callback != UCNV_TO_U_CALLBACK_STOP || (status != U_INVALID_CHAR_FOUND && status != U_ILLEGAL_CHAR_FOUND && status != U_TRUNCATED_CHAR_FOUND)))
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
            log_err("did not get the expected offsets while %s \n", gNuConvTestName);
            log_err("Got offsets:      ");
            for(p=junkout;p<targ;p++)
                log_err("  %2d,", junokout[p-junkout]); 
            log_err("\n");
            log_err("Expected offsets: ");
            for(i=0; i<(targ-junkout); i++)
                log_err("  %2d,", expectOffsets[i]);
            log_err("\n");
            log_err("Got output:       ");
            for(i=0; i<(targ-junkout); i++)
                log_err("0x%04x,", junkout[i]);
            log_err("\n");
            log_err("From source:      ");
            for(i=0; i<(src-source); i++)
                log_err("  0x%02x,", (unsigned char)source[i]);
            log_err("\n");
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
        log_err("Got:      ");
        printUSeqErr(junkout, expectlen);
        log_err("Expected: ");
        printUSeqErr(expect, expectlen);
        log_err("\n");
        return FALSE;
    }
}

UBool testConvertFromUnicodeWithContext(const UChar *source, int sourceLen,  const uint8_t *expect, int expectLen, 
                const char *codepage, UConverterFromUCallback callback , const int32_t *expectOffsets, 
                const char *mySubChar, int8_t len, void* context, UErrorCode expectedError)
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
    int32_t  realBufferSize;
    uint8_t *realBufferEnd;
    const UChar *realSourceEnd;
    const UChar *sourceLimit;
    UBool checkOffsets = TRUE;
    UBool doFlush;
    char junk[9999];
    char offset_str[9999];
    uint8_t *p;
    UConverterFromUCallback oldAction = NULL;
    void* oldContext = NULL;


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
    ucnv_setFromUCallBack (conv, callback, context, &oldAction, &oldContext, &status);
    if (U_FAILURE(status)) 
    { 
        log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));
    }
    /*------------------------*/
    /*setting the subChar*/
    if(mySubChar != NULL){
        ucnv_setSubstChars(conv, mySubChar, len, &status);
        if (U_FAILURE(status))  { 
            log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));
        }
    }
    /*------------*/

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
    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (U_SUCCESS(status) && (sourceLimit < realSourceEnd)) );

    /* allow failure codes for the stop callback */
    if(U_FAILURE(status) && status != expectedError)
    {
        log_err("Problem in fromUnicode, errcode %s %s\n", myErrorName(status), gNuConvTestName);
        return FALSE;
    }

    log_verbose("\nConversion done [%d uchars in -> %d chars out]. \nResult :",
        sourceLen, targ-junkout);
    if(VERBOSITY)
    {

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
        log_verbose("comparing %d offsets..\n", targ-junkout);
        if(memcmp(junokout,expectOffsets,(targ-junkout) * sizeof(int32_t) )){
            log_err("did not get the expected offsets while %s \n", gNuConvTestName);
            log_err("Got Output : ");
            printSeqErr(junkout, targ-junkout);
            log_err("Got Offsets:      ");
            for(p=junkout;p<targ;p++)
                log_err("%d,", junokout[p-junkout]); 
            log_err("\n");
            log_err("Expected Offsets: ");
            for(i=0; i<(targ-junkout); i++)
                log_err("%d,", expectOffsets[i]);
            log_err("\n");
            return FALSE;
        }
    }

    if(!memcmp(junkout, expect, expectLen))
    {
        log_verbose("String matches! %s\n", gNuConvTestName);
        return TRUE;
    }
    else
    {
        log_err("String does not match. %s\n", gNuConvTestName);
        log_err("source: ");
        printUSeqErr(source, sourceLen);
        log_err("Got:      ");
        printSeqErr(junkout, expectLen);
        log_err("Expected: ");
        printSeqErr(expect, expectLen);
        return FALSE;
    }
}
UBool testConvertToUnicodeWithContext( const uint8_t *source, int sourcelen, const UChar *expect, int expectlen, 
               const char *codepage, UConverterToUCallback callback, const int32_t *expectOffsets,
               const char *mySubChar, int8_t len, void* context, UErrorCode expectedError)
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
    UConverterToUCallback oldAction = NULL;
    void* oldContext = NULL;

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
    /*----setting the callback routine----*/
    ucnv_setToUCallBack (conv, callback, context, &oldAction, &oldContext, &status);
    if (U_FAILURE(status)) 
    { 
        log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));  
    }
    /*-------------------------------------*/
    /*setting the subChar*/
    if(mySubChar != NULL){
        ucnv_setSubstChars(conv, mySubChar, len, &status);
        if (U_FAILURE(status))  { 
            log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status)); 
        }
    }
    /*------------*/


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
                (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of the source data */
                &status);
    } while ( (status == U_BUFFER_OVERFLOW_ERROR) || (U_SUCCESS(status) && (srcLimit < realSourceEnd)) ); /* while we just need another buffer */

    /* allow failure codes for the stop callback */
    if(U_FAILURE(status) && status!=expectedError)
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
            log_err("did not get the expected offsets while %s \n", gNuConvTestName);
            log_err("Got offsets:      ");
            for(p=junkout;p<targ;p++)
                log_err("  %2d,", junokout[p-junkout]); 
            log_err("\n");
            log_err("Expected offsets: ");
            for(i=0; i<(targ-junkout); i++)
                log_err("  %2d,", expectOffsets[i]);
            log_err("\n");
            log_err("Got output:       ");
            for(i=0; i<(targ-junkout); i++)
                log_err("0x%04x,", junkout[i]);
            log_err("\n");
            log_err("From source:      ");
            for(i=0; i<(src-source); i++)
                log_err("  0x%02x,", (unsigned char)source[i]);
            log_err("\n");
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
        log_err("Got:      ");
        printUSeqErr(junkout, expectlen);
        log_err("Expected: ");
        printUSeqErr(expect, expectlen);
        log_err("\n");
        return FALSE;
    }
}
