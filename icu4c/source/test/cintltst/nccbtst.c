/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File NCCBTST.C
*
* Modification History:
*        Name							 Description            
*    Madhu Katragadda     7/21/1999      Testing error callback routines
**************************************************************************************
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
#include "nccbtst.h"
#define NEW_MAX_BUFFER 999

#define nct_min(x,y)  ((x<y) ? x : y)

static int32_t  gInBufferSize = 0;
static int32_t  gOutBufferSize = 0;
static char     gNuConvTestName[1024];

void printSeq(const char* a, int len)
{
	int i=0;
	log_verbose("\n{");
	while (i<len) log_verbose("%02X, ", (unsigned char) a[i++]);
	log_verbose("}\n");
}
void printUSeq(const UChar* a, int len)
{
	int i=0;
	log_verbose("{");
	while (i<len) log_verbose("  %4x, ", (unsigned char) a[i++]);
	log_verbose("}\n");
}

void printSeqErr(const char* a, int len)
{
	int i=0;
	fprintf(stderr, "{");
	while (i<len)  fprintf(stderr, "  %2x, ", (unsigned char) a[i++]);
	fprintf(stderr, "}\n");
}
void printUSeqErr(const UChar* a, int len)
{
	int i=0;
	fprintf(stderr, "{");
	while (i<len) fprintf(stderr, "%4x, ", a[i++]);
	fprintf(stderr,"}\n");
}
void setNuConvTestName(const char *codepage, const char *direction)
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

void TestSkipCallBack()
{
	 TestSkip(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
     TestSkip(1,NEW_MAX_BUFFER);
     TestSkip(1,1);
     TestSkip(NEW_MAX_BUFFER, 1);
}
void TestStopCallBack()
{
	 TestStop(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
     TestStop(1,NEW_MAX_BUFFER);
     TestStop(1,1);
     TestStop(NEW_MAX_BUFFER, 1);
}
void TestSubCallBack()
{
	 TestSub(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
     TestSub(1,NEW_MAX_BUFFER);
     TestSub(1,1);
     TestSub(NEW_MAX_BUFFER, 1);
}
void TestSubWithValueCallBack()
{
	 TestSubWithValue(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
     TestSubWithValue(1,NEW_MAX_BUFFER);
     TestSubWithValue(1,1);
     TestSubWithValue(NEW_MAX_BUFFER, 1);
}
void TestLegalAndOtherCallBack()
{
	 TestLegalAndOthers(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
     TestLegalAndOthers(1,NEW_MAX_BUFFER);
     TestLegalAndOthers(1,1);
     TestLegalAndOthers(NEW_MAX_BUFFER, 1);
}
void TestSingleByteCallBack()
{
	 TestSingleByte(NEW_MAX_BUFFER, NEW_MAX_BUFFER);
     TestSingleByte(1,NEW_MAX_BUFFER);
     TestSingleByte(1,1);
     TestSingleByte(NEW_MAX_BUFFER, 1);
}
void TestSkip(int32_t inputsize, int32_t outputsize)
{
	UChar	sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
	UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };

	const char expskipIBM_949[]= { 
		(char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2, (char)0xc8, (char)0xd3 };
	
	const char expskipIBM_943[] = { 
		(char)0x9f, (char)0xaf, (char)0x9f, (char)0xb1, (char)0x89, (char)0x59 };
	
	const char expskipIBM_930[] = { 
		(char)0x0e, (char)0x5d, (char)0x5f, (char)0x5d, (char)0x63, (char)0x46, (char)0x6b };
	
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
	if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
			expskipIBM_949, sizeof(expskipIBM_949), "ibm-949",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, toIBM949Offsskip ))
		log_err("u-> ibm-949 with skip did not match.\n");
	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expskipIBM_943, sizeof(expskipIBM_943), "ibm-943",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, toIBM943Offsskip ))
		log_err("u-> ibm-943 with skip did not match.\n");
	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expskipIBM_930, sizeof(expskipIBM_930), "ibm-930",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, toIBM930Offsskip ))
		log_err("u-> ibm-930 with skip did not match.\n");

	/*to Unicode*/
	if(!testConvertToUnicode(expskipIBM_949, sizeof(expskipIBM_949),
			 IBM_949skiptoUnicode, sizeof(IBM_949skiptoUnicode)/sizeof(IBM_949skiptoUnicode),"ibm-949",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM949Offs ))
		log_err("ibm-949->u with skip did not match.\n");
    if(!testConvertToUnicode(expskipIBM_943, sizeof(expskipIBM_943),
			 IBM_943skiptoUnicode, sizeof(IBM_943skiptoUnicode)/sizeof(IBM_943skiptoUnicode[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM943Offs ))
		log_err("ibm-943->u with skip did not match.\n");
    
    if(!testConvertToUnicode(expskipIBM_930, sizeof(expskipIBM_930),
			 IBM_930skiptoUnicode, sizeof(IBM_930skiptoUnicode)/sizeof(IBM_930skiptoUnicode[0]),"ibm-930",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM930Offs ))
		log_err("ibm-930->u with skip did not match.\n");

   log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_SKIP \n");
    {
         /*ibm-954*/
        const char sampleTxt_UC_JP[]={ (char)0x61, (char)0xa1, (char)0xb8, (char)0x8f, (char)0xf4, (char)0xae,
            (char)0x8f, (char)0xda, (char)0xa1,  /*unassigned*/
           (char)0x8e, (char)0xe0,
        };
        UChar UC_JPtoUnicode[]={ 0x0061, 0x4edd, 0x5bec, 0x00a2};
        int32_t fromUC_JPOffs [] ={ 0, 1, 3, 9};

        /*LMBCS*/
        const char sampleTxtLMBCS[]={ (char)0x12, (char)0xc9, (char)0x50, 
            (char)0x12, (char)0x92, (char)0xa0, /*unassigned*/
            (char)0x12, (char)0x92, (char)0xA1,
        };
        UChar LMBCSToUnicode[]={ 0x4e2e, 0xe5c4};
        int32_t fromLMBCS[] = {0, 6};


        if(!testConvertToUnicode(sampleTxt_UC_JP, sizeof(sampleTxt_UC_JP),
			     UC_JPtoUnicode, sizeof(UC_JPtoUnicode)/sizeof(UC_JPtoUnicode[0]),"ibm-954",
	            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromUC_JPOffs ))
		    log_err("ibm-954->u with skip did not match.\n");

        if(!testConvertToUnicode(sampleTxtLMBCS, sizeof(sampleTxtLMBCS),
			    LMBCSToUnicode, sizeof(LMBCSToUnicode)/sizeof(LMBCSToUnicode[0]),"LMBCS-1",
	            (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromLMBCS ))
		    log_err("LMBCS->u with skip did not match.\n");

    }

}
void TestStop(int32_t inputsize, int32_t outputsize)
{
	UChar	sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
	UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };

	const char expstopIBM_949[]= { 
		(char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2};
	
	const char expstopIBM_943[] = { 
		(char)0x9f, (char)0xaf, (char)0x9f, (char)0xb1};
	
	const char expstopIBM_930[] = { 
		(char)0x0e, (char)0x5d, (char)0x5f, (char)0x5d, (char)0x63};
	
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
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, toIBM949Offsstop ))
		log_err("u-> ibm-949 with stop did not match.\n");
	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expstopIBM_943, sizeof(expstopIBM_943), "ibm-943",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, toIBM943Offsstop ))
		log_err("u-> ibm-943 with stop did not match.\n");
	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expstopIBM_930, sizeof(expstopIBM_930), "ibm-930",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP, toIBM930Offsstop ))
		log_err("u-> ibm-930 with stop did not match.\n");

	/*to Unicode*/
	if(!testConvertToUnicode(expstopIBM_949, sizeof(expstopIBM_949),
			 IBM_949stoptoUnicode, sizeof(IBM_949stoptoUnicode)/sizeof(IBM_949stoptoUnicode[0]),"ibm-949",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM949Offs ))
		log_err("ibm-949->u with stop did not match.\n");
    if(!testConvertToUnicode(expstopIBM_943, sizeof(expstopIBM_943),
			 IBM_943stoptoUnicode, sizeof(IBM_943stoptoUnicode)/sizeof(IBM_943stoptoUnicode[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM943Offs ))
		log_err("ibm-943->u with stop did not match.\n");
    if(!testConvertToUnicode(expstopIBM_930, sizeof(expstopIBM_930),
			 IBM_930stoptoUnicode, sizeof(IBM_930stoptoUnicode)/sizeof(IBM_930stoptoUnicode[0]),"ibm-930",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM930Offs ))
		log_err("ibm-930->u with stop did not match.\n");

     log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_STOP \n");
    {
         /*ibm-954*/
        const char sampleTxt_UC_JP[]={ (char)0x61, (char)0xa1, (char)0xb8, (char)0x8f, (char)0xf4, (char)0xae,
            (char)0x8f, (char)0xda, (char)0xa1,  /*unassigned*/
           (char)0x8e, (char)0xe0,
        };
        UChar UC_JPtoUnicode[]={ 0x0061, 0x4edd, 0x5bec};
        int32_t fromUC_JPOffs [] ={ 0, 1, 3};

        if(!testConvertToUnicode(sampleTxt_UC_JP, sizeof(sampleTxt_UC_JP),
			 UC_JPtoUnicode, sizeof(UC_JPtoUnicode)/sizeof(UC_JPtoUnicode[0]),"ibm-954",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromUC_JPOffs ))
		log_err("ibm-954->u with stop did not match.\n");
    }

}
void TestSub(int32_t inputsize, int32_t outputsize)
{
    UChar	sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
	UChar sampleText2[]=    { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };
    
    const char expsubIBM_949[] = 
	 { (char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2, (char)0xaf, (char)0xfe, (char)0xc8, (char)0xd3 };
	
    const char expsubIBM_943[] = { 
        (char)0x9f, (char)0xaf, (char)0x9f, (char)0xb1, (char)0xfc, (char)0xfc, (char)0x89, (char)0x59 };
	
    const char expsubIBM_930[] = { 
        (char)0x0e, (char)0x5d, (char)0x5f, (char)0x5d, (char)0x63, (char)0xfe, (char)0xfe, (char)0x46, (char)0x6b };
		
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
			(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, toIBM949Offssub ))
		log_err("u-> ibm-949 with subst did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubIBM_943, sizeof(expsubIBM_943), "ibm-943",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, toIBM943Offssub ))
		log_err("u-> ibm-943 with subst did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubIBM_930, sizeof(expsubIBM_930), "ibm-930", 
			(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE, toIBM930Offssub ))
		log_err("u-> ibm-930 with subst did not match.\n");
    
    /*to unicode*/
	if(!testConvertToUnicode(expsubIBM_949, sizeof(expsubIBM_949),
			 IBM_949subtoUnicode, sizeof(IBM_949subtoUnicode)/sizeof(IBM_949subtoUnicode[0]),"ibm-949",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM949Offs ))
		log_err("ibm-949->u with substitute did not match.\n");
	if(!testConvertToUnicode(expsubIBM_943, sizeof(expsubIBM_943),
			 IBM_943subtoUnicode, sizeof(IBM_943subtoUnicode)/sizeof(IBM_943subtoUnicode[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM943Offs ))
		log_err("ibm-943->u with substitute did not match.\n");
	if(!testConvertToUnicode(expsubIBM_930, sizeof(expsubIBM_930),
			 IBM_930subtoUnicode, sizeof(IBM_930subtoUnicode)/sizeof(IBM_930subtoUnicode[0]),"ibm-930",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM930Offs ))
		log_err("ibm-930->u with substitute did not match.\n");

    log_verbose("Testing toUnicode with UCNV_TO_U_CALLBACK_SUBSTITUTE \n");
    {
        /*ibm-954*/
        const char sampleTxt_UC_JP[]={ (char)0x61, (char)0xa1, (char)0xb8, (char)0x8f, (char)0xf4, (char)0xae,
            (char)0x8f, (char)0xda, (char)0xa1,  /*unassigned*/
           (char)0x8e, (char)0xe0,
        };
        UChar UC_JPtoUnicode[]={ 0x0061, 0x4edd, 0x5bec, 0xfffd, 0x00a2 };
        int32_t fromUC_JPOffs [] ={ 0, 1, 3, 6,  9,
        };
        if(!testConvertToUnicode(sampleTxt_UC_JP, sizeof(sampleTxt_UC_JP),
			 UC_JPtoUnicode, sizeof(UC_JPtoUnicode)/sizeof(UC_JPtoUnicode[0]),"ibm-954",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromUC_JPOffs ))
		log_err("ibm-954->u with substitute with value did not match.\n");

    }


}

void TestSubWithValue(int32_t inputsize, int32_t outputsize)
{
    UChar	sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
	UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };
	
	const char expsubwvalIBM_949[]=	{ 
        (char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2,
        (char)0x25, (char)0x55, (char)0x45, (char)0x46, (char)0x36, (char)0x37, (char)0xc8, (char)0xd3 }; 
    
    const char expsubwvalIBM_943[]=	{ 
        (char)0x9f, (char)0xaf, (char)0x9f, (char)0xb1,
        (char)0x25, (char)0x55, (char)0x36, (char)0x44, (char)0x36, (char)0x35, (char)0x89, (char)0x59 };
	
    const char expsubwvalIBM_930[] = {
        (char)0x0e, (char)0x5d, (char)0x5f, (char)0x5d, (char)0x63, (char)0x0f, (char)0x6c, (char)0xe4, (char)0xf6, (char)0xc4, (char)0xf6, (char)0xf5, (char)0x0e, (char)0x46, (char)0x6b };

	int32_t toIBM949Offs [] ={ 0, 1, 1, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4};
	int32_t toIBM943Offs [] = { 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3};
	int32_t toIBM930Offs [] = { 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3};

	gInBufferSize = inputsize;
	gOutBufferSize = outputsize;

    /*from Unicode*/
	if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
			expsubwvalIBM_949, sizeof(expsubwvalIBM_949), "ibm-949", 
			(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, toIBM949Offs ))
		log_err("u-> ibm-949 with subst with value did not match.\n");

	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubwvalIBM_943, sizeof(expsubwvalIBM_943), "ibm-943",
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, toIBM943Offs ))
		log_err("u-> ibm-943 with sub with value did not match.\n");

	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubwvalIBM_930, sizeof(expsubwvalIBM_930), "ibm-930", 
			(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_ESCAPE, toIBM930Offs ))
		log_err("u-> ibm-930 with subst with value did not match.\n(needs fix for j344 and general callback cleanup.)\n");
  
	log_verbose("Testing toUnicode with UCNV_FROM_U_CALLBACK_ESCAPE \n");
    /*to Unicode*/
    {
        const char sampleTxtToU[]= { (char)0x00, (char)0x9f, (char)0xaf, 
            (char)0x81, (char)0xad, /*unassigned*/
            (char)0x89, (char)0xd3 };
	    UChar IBM_943toUnicode[] = { 0x0000, 0x6D63, 
            0x25, 0x58, 0x38, 0x31, 0x25, 0x58, 0x41, 0x44,
            0x7B87};
        int32_t  fromIBM943Offs [] = 	{ 0, 1, 3, 3, 3, 3, 3, 3, 3, 3, 5};

        /*ibm-954*/
        const char sampleTxt_EUC_JP[]={ (char)0x61, (char)0xa1, (char)0xb8, (char)0x8f, (char)0xf4, (char)0xae,
            (char)0x8f, (char)0xda, (char)0xa1,  /*unassigned*/
           (char)0x8e, (char)0xe0,
        };
        UChar EUC_JPtoUnicode[]={ 0x0061, 0x4edd, 0x5bec,
            0x25, 0x58, 0x38, 0x46, 0x25, 0x58, 0x44, 0x41, 0x25, 0x58, 0x41, 0x31, 
            0x00a2 };
        int32_t fromEUC_JPOffs [] ={ 0, 1, 3, 
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
            9,
        };
       
        /*LMBCS*/
        const char sampleTxtLMBCS[]={ (char)0x12, (char)0xc9, (char)0x50, 
            (char)0x12, (char)0x92, (char)0xa0, /*unassigned*/
            (char)0x12, (char)0x92, (char)0xa1,
        };
        UChar LMBCSToUnicode[]={ 0x4e2e, 
            0x25, 0x58, 0x31, 0x32, 0x25, 0x58, 0x39, 0x32, 0x25, 0x58, 0x41, 0x30, 
            0xe5c4, };
        int32_t fromLMBCS[] = {0, 
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            6, };
       
        
        if(!testConvertToUnicode(sampleTxtToU, sizeof(sampleTxtToU),
		         IBM_943toUnicode, sizeof(IBM_943toUnicode)/sizeof(IBM_943toUnicode[0]),"ibm-943",
	            (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromIBM943Offs ))
		    log_err("ibm-943->u with substitute with value did not match.\n");

        if(!testConvertToUnicode(sampleTxt_EUC_JP, sizeof(sampleTxt_EUC_JP),
		         EUC_JPtoUnicode, sizeof(EUC_JPtoUnicode)/sizeof(EUC_JPtoUnicode[0]),"euc-jp",
	            (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromEUC_JPOffs))
		    log_err("euc-jp->u with substitute with value did not match.\n");

        /*got to confirm this*/
        
        if(!testConvertToUnicode(sampleTxtLMBCS, sizeof(sampleTxtLMBCS),
			    LMBCSToUnicode, sizeof(LMBCSToUnicode)/sizeof(LMBCSToUnicode[0]),"LMBCS",
	            (UConverterToUCallback)UCNV_TO_U_CALLBACK_ESCAPE, fromLMBCS ))
		    log_err("LMBCS->u with substitute with value did not match.\n"); 
            



    }
}
void TestLegalAndOthers(int32_t inputsize, int32_t outputsize)
{
    UChar    legalText[] =	{ 0x0000, 0xAC00, 0xAC01, 0xD700 };
    const char templegal949[] ={ (char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2, (char)0xc8, (char)0xd3 };
	int32_t  to949legal[] = {0, 1, 1, 2, 2, 3, 3};
    

    const char text943[] = {
        (char)0x82, (char)0xa9, (char)0x82, (char)0x20, /*(char)0xc8,*/  (char)0x61, (char)0x8a, (char)0xbf, (char)0x8e, (char)0x9a };
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
	        (UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SKIP, to949legal ))
		log_err("u-> ibm-949 with skip did not match.\n");
    
    /*checking illegal value for ibm-943 with substitute*/ 
    if(!testConvertToUnicode(text943, sizeof(text943),
			 toUnicode943sub, sizeof(toUnicode943sub)/sizeof(toUnicode943sub[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM943Offssub ))
		log_err("ibm-943->u with subst did not match.\n");
	/*checking illegal value for ibm-943 with skip */
	if(!testConvertToUnicode(text943, sizeof(text943),
			 toUnicode943skip, sizeof(toUnicode943skip)/sizeof(toUnicode943skip[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SKIP, fromIBM943Offsskip ))
		log_err("ibm-943->u with skip did not match.\n");

	/*checking illegal value for ibm-943 with stop */
	if(!testConvertToUnicode(text943, sizeof(text943),
			 toUnicode943stop, sizeof(toUnicode943stop)/sizeof(toUnicode943stop[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP, fromIBM943Offsstop ))
		log_err("ibm-943->u with stop did not match.\n");

}
void TestSingleByte(int32_t inputsize, int32_t outputsize)
{
	const char sampleText[] = {
        (char)0x82, (char)0xa9, (char)0x61, (char)0x62, (char)0x63 , (char)0x82,
        (char)0xff, /*(char)0x82, (char)0xa9,*/ (char)0x32, (char)0x33};
	UChar toUnicode943sub[] = {0x304b, 0x0061, 0x0062, 0x0063,  0xfffd,/*0x304b,*/ 0x0032, 0x0033};
	int32_t  fromIBM943Offssub[]  = {0, 2, 3, 4, 5, 7, 8};
	/*checking illegal value for ibm-943 with substitute*/ 
	gInBufferSize = inputsize;
	gOutBufferSize = outputsize;
	
    if(!testConvertToUnicode(sampleText, sizeof(sampleText),
			 toUnicode943sub, sizeof(toUnicode943sub)/sizeof(toUnicode943sub[0]),"ibm-943",
	        (UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE, fromIBM943Offssub ))
		log_err("ibm-943->u with subst did not match.\n");
}

UBool testConvertFromUnicode(const UChar *source, int sourceLen,  const char *expect, int expectLen, 
			    const char *codepage, UConverterFromUCallback callback , int32_t *expectOffsets)
{
	
		
	UErrorCode status = U_ZERO_ERROR;
	UConverter *conv = 0;
	char	junkout[NEW_MAX_BUFFER]; /* FIX */
	int32_t	junokout[NEW_MAX_BUFFER]; /* FIX */
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
	char junk[9999];
	char offset_str[9999];
	char *p;
    UConverterFromUCallback oldAction = NULL;
    void* oldContext = NULL;
	
	
	for(i=0;i<NEW_MAX_BUFFER;i++)
		junkout[i] = (char)0xF0;
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

      /*check for an INVALID character for testing the call back function STOP*/
	 if(status == U_INVALID_CHAR_FOUND || status == U_ILLEGAL_CHAR_FOUND  )
     {
		junk[0] = 0;
		offset_str[0] = 0;
        for(p = junkout;p<targ;p++)
		  sprintf(junk + strlen(junk), "0x%02x, ", (0xFF) & (unsigned int)*p);
		/*		printSeqErr(junkout, expectlen);*/
		if(!memcmp(junkout, expect, expectLen))
		{
		log_verbose("Matches!\n");
    	ucnv_close(conv);
		return TRUE;
		}
		else
		{	
		log_err("String does not match. %s\n", gNuConvTestName);
		log_verbose("String does not match. %s\n", gNuConvTestName);
		printSeq(junkout, expectLen);
		printSeq(expect, expectLen);
    	ucnv_close(conv);
		return FALSE;
		}
		
	} 
	} while ( (status == U_INDEX_OUTOFBOUNDS_ERROR) || (sourceLimit < realSourceEnd) );
	
	if(U_FAILURE(status))
	{
		log_err("Problem doing toUnicode, errcode %s %s\n", myErrorName(status), gNuConvTestName);
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
        printUSeqErr(source, sourceLen);
		printSeqErr(junkout, expectLen);
		printSeqErr(expect, expectLen);
		return FALSE;
	}
}

UBool testConvertToUnicode( const char *source, int sourcelen, const UChar *expect, int expectlen, 
		       const char *codepage, UConverterToUCallback callback, int32_t *expectOffsets)
{
	UErrorCode status = U_ZERO_ERROR;
	UConverter *conv = 0;
	UChar	junkout[NEW_MAX_BUFFER]; /* FIX */
	int32_t	junokout[NEW_MAX_BUFFER]; /* FIX */
	const char *src;
	const char *realSourceEnd;
	const char *srcLimit;
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
			    &src,
			    srcLimit,
			    checkOffsets ? offs : NULL,
			    (UBool)(srcLimit == realSourceEnd), /* flush if we're at the end of the source data */
			    &status);
        
         /*check for an INVALID character for testing the call back function STOP*/
	if(status == U_INVALID_CHAR_FOUND || status == U_ILLEGAL_CHAR_FOUND )
	{
		junk[0] = 0;
		offset_str[0] = 0;
        for(p = junkout;p<targ;p++)
		  sprintf(junk + strlen(junk), "0x%04x, ", (0xFFFF) & (unsigned int)*p);
		/*		printUSeqErr(junkout, expectlen);*/
		if(!memcmp(junkout, expect, expectlen*2))
		{
		log_verbose("Matches!\n");
    	ucnv_close(conv);
		return TRUE;
		}
		else
		{	
		log_err("String does not match. %s\n", gNuConvTestName);
		log_verbose("String does not match. %s\n", gNuConvTestName);
		printUSeqErr(junkout, expectlen);
		printUSeqErr(expect, expectlen);
    	ucnv_close(conv);
		return FALSE;
		}
    }
    else if (status == U_INDEX_OUTOFBOUNDS_ERROR)
    {  /* Jim Snyder-Grant: testing for UCharErrorBuffer. Only has contents if output 
       from last source char crosses TargetLimit. As of 2000-07-11, this happens only 
       for escape procesing. */
        UChar errChars [UCNV_MAX_SUBCHAR_LEN];
        int8_t len = UCNV_MAX_SUBCHAR_LEN;
        UErrorCode localStatus = U_ZERO_ERROR;
        ucnv_getInvalidUChars (conv, errChars, &len, &localStatus);
        if (U_FAILURE(localStatus))
        {
            log_err("Error from ucnv_getInvalidUChars\n");
        }
        else
        {
           int targIndex = targ - junkout;
           if ((len != 0) && memcmp(errChars, expect+targIndex, len*sizeof(UChar)))
           {
              log_err("ucharErrorBuffer uchars do not match expected in %s\n", gNuConvTestName);
              
              printUSeqErr(errChars, len); 
              printUSeqErr(expect+targIndex, len);
           }
        }
     }
  } while ( (status == U_INDEX_OUTOFBOUNDS_ERROR) || (U_SUCCESS(status) && (srcLimit < realSourceEnd)) ); /* while we just need another buffer */

    
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
            log_err("\nGot offsets:      ");
		    for(p=junkout;p<targ;p++)
		      log_err("  %2d,", junokout[p-junkout]); 
		    log_err("\nExpected offsets: ");
		    for(i=0; i<(targ-junkout); i++)
		      log_err("  %2d,", expectOffsets[i]);
		    log_err("\nGot output:       ");
		    for(i=0; i<(targ-junkout); i++)
		      log_err("%4x,", junkout[i]);
		    log_err("\nFrom source:      ");
		    for(i=0; i<(src-source); i++)
		      log_err("  %2x,", (unsigned char)source[i]);
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
		printUSeqErr(junkout, expectlen);
		printUSeqErr(expect, expectlen);
		return FALSE;
	}
}
