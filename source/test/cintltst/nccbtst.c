/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
*
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
#include "uloc.h"
#include "ucnv.h"
#include "ucnv_err.h"
#include "cintltst.h"
#include "utypes.h"
#include "ustring.h"
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
	log_verbose("\n{");
	while (i<len) log_verbose("%04X, ", (unsigned char) a[i++]);
	log_verbose("}\n");
}

void printSeqErr(const char* a, int len)
{
	int i=0;
	fprintf(stderr, "\n{");
	while (i<len)  fprintf(stderr, "%02X, ", (unsigned char) a[i++]);
	fprintf(stderr, "}\n");
}
void printUSeqErr(const UChar* a, int len)
{
	int i=0;
	fprintf(stderr, "\n{");
	while (i<len) fprintf(stderr, "%04X, ", (unsigned char) a[i++]);
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
	        (UCNV_FromUCallBack)MissingUnicodeAction_SKIP, toIBM949Offsskip ))
		log_err("u-> ibm-949 with skip did not match.\n");
	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expskipIBM_943, sizeof(expskipIBM_943), "ibm-943",
	        (UCNV_FromUCallBack)MissingUnicodeAction_SKIP, toIBM943Offsskip ))
		log_err("u-> ibm-943 with skip did not match.\n");
	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expskipIBM_930, sizeof(expskipIBM_930), "ibm-930",
	        (UCNV_FromUCallBack)MissingUnicodeAction_SKIP, toIBM930Offsskip ))
		log_err("u-> ibm-930 with skip did not match.\n");

	/*to Unicode*/
	if(!testConvertToUnicode(expskipIBM_949, sizeof(expskipIBM_949),
			 IBM_949skiptoUnicode, sizeof(IBM_949skiptoUnicode)/sizeof(IBM_949skiptoUnicode),"ibm-949",
	        (UCNV_ToUCallBack)MissingCharAction_SKIP, fromIBM949Offs ))
		log_err("ibm-949->u with skip did not match.\n");
    if(!testConvertToUnicode(expskipIBM_943, sizeof(expskipIBM_943),
			 IBM_943skiptoUnicode, sizeof(IBM_943skiptoUnicode)/sizeof(IBM_943skiptoUnicode[0]),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_SKIP, fromIBM943Offs ))
		log_err("ibm-943->u with skip did not match.\n");
    
    if(!testConvertToUnicode(expskipIBM_930, sizeof(expskipIBM_930),
			 IBM_930skiptoUnicode, sizeof(IBM_930skiptoUnicode)/sizeof(IBM_930skiptoUnicode[0]),"ibm-930",
	        (UCNV_ToUCallBack)MissingCharAction_SKIP, fromIBM930Offs ))
		log_err("ibm-930->u with skip did not match.\n");

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
			(UCNV_FromUCallBack)MissingUnicodeAction_SUBSTITUTE, toIBM949Offssub ))
		log_err("u-> ibm-949 with subst did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubIBM_943, sizeof(expsubIBM_943), "ibm-943",
	        (UCNV_FromUCallBack)MissingUnicodeAction_SUBSTITUTE, toIBM943Offssub ))
		log_err("u-> ibm-943 with subst did not match.\n");
    if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubIBM_930, sizeof(expsubIBM_930), "ibm-930", 
			(UCNV_FromUCallBack)MissingUnicodeAction_SUBSTITUTE, toIBM930Offssub ))
		log_err("u-> ibm-930 with subst did not match.\n");
    
    /*to unicode*/
	if(!testConvertToUnicode(expsubIBM_949, sizeof(expsubIBM_949),
			 IBM_949subtoUnicode, sizeof(IBM_949subtoUnicode)/sizeof(IBM_949subtoUnicode[0]),"ibm-949",
	        (UCNV_ToUCallBack)MissingCharAction_SUBSTITUTE, fromIBM949Offs ))
		log_err("ibm-949->u with substitute did not match.\n");
	if(!testConvertToUnicode(expsubIBM_943, sizeof(expsubIBM_943),
			 IBM_943subtoUnicode, sizeof(IBM_943subtoUnicode)/sizeof(IBM_943subtoUnicode[0]),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_SUBSTITUTE, fromIBM943Offs ))
		log_err("ibm-943->u with substitute did not match.\n");
	if(!testConvertToUnicode(expsubIBM_930, sizeof(expsubIBM_930),
			 IBM_930subtoUnicode, sizeof(IBM_930subtoUnicode)/sizeof(IBM_930subtoUnicode[0]),"ibm-930",
	        (UCNV_ToUCallBack)MissingCharAction_SUBSTITUTE, fromIBM930Offs ))
		log_err("ibm-930->u with substitute did not match.\n");

}

void TestSubWithValue(int32_t inputsize, int32_t outputsize)
{
    UChar	sampleText[] =  { 0x0000, 0xAC00, 0xAC01, 0xEF67, 0xD700 };
	UChar  sampleText2[] =  { 0x6D63, 0x6D64, 0x6D65, 0x6D66 };
	
	const char sampleTxtToU[]= { (char)0x00, (char)0x9f, (char)0xaf, (char)0xff, (char)0x89, (char)0xd3 };
	
	UChar IBM_943toUnicode[] = { 0x0000, 0x6D63, '%', 'X', 'F', 'F', 0x6D66};
	
	int32_t  fromIBM943Offs [] = 	{ 0, 1, 3, 3, 3, 3, 4};
    
	
	const char expsubwvalIBM_949[]=	{ 
        (char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2, '%', 'U', 'E', 'F', '6', '7', (char)0xc8, (char)0xd3 }; 
    
    const char expsubwvalIBM_943[]=	{ 
        (char)0x9f, (char)0xaf, (char)0x9f, (char)0xb1, '%', 'U', '6', 'D', '6', '5', (char)0x89, (char)0x59 };
	
    const char expsubwvalIBM_930[] = {
        (char)0x0e, (char)0x5d, (char)0x5f, (char)0x5d, (char)0x63, (char)0x0f, (char)0x6c, (char)0xe4, (char)0xf6, (char)0xc4, (char)0xf6, (char)0xf5, (char)0x46, (char)0x6b };

    
	int32_t toIBM949Offs [] ={ 0, 1, 1, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4};
	int32_t toIBM943Offs [] = { 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3};
	int32_t toIBM930Offs [] = { 0, 0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3};

	gInBufferSize = inputsize;
	gOutBufferSize = outputsize;

    /*from Unicode*/
	if(!testConvertFromUnicode(sampleText, sizeof(sampleText)/sizeof(sampleText[0]),
			expsubwvalIBM_949, sizeof(expsubwvalIBM_949), "ibm-949", 
			(UCNV_FromUCallBack)MissingUnicodeAction_SUBSTITUTEwithValue, toIBM949Offs ))
		log_err("u-> ibm-949 with subst with value did not match.\n");

	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubwvalIBM_943, sizeof(expsubwvalIBM_943), "ibm-943",
	        (UCNV_FromUCallBack)MissingUnicodeAction_SUBSTITUTEwithValue, toIBM943Offs ))
		log_err("u-> ibm-943 with sub with value did not match.\n");

	if(!testConvertFromUnicode(sampleText2, sizeof(sampleText2)/sizeof(sampleText2[0]),
			expsubwvalIBM_930, sizeof(expsubwvalIBM_930), "ibm-930", 
			(UCNV_FromUCallBack)MissingUnicodeAction_SUBSTITUTEwithValue, toIBM930Offs ))
		log_err("u-> ibm-930 with subst with value did not match.\n");
  
	/*to Unicode*/
	if(!testConvertToUnicode(sampleTxtToU, sizeof(sampleTxtToU),
			 IBM_943toUnicode, sizeof(IBM_943toUnicode)/sizeof(IBM_943toUnicode),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_SUBSTITUTEwithValue, fromIBM943Offs ))
		log_err("ibm-943->u with substitute did not match.\n");
}
void TestLegalAndOthers(int32_t inputsize, int32_t outputsize)
{
    UChar    legalText[] =	{ 0x0000, 0xAC00, 0xAC01, 0xD700 };
    const char templegal949[] ={ (char)0x00, (char)0xb0, (char)0xa1, (char)0xb0, (char)0xa2, (char)0xc8, (char)0xd3 };
	int32_t  to949legal[] = {0, 1, 1, 2, 2, 3, 3};
    

    const char text943[] = {
        (char)0x82, (char)0xa9, (char)0x82, (char)0x20, /*(char)0xc8,*/  'a', (char)0x8a, (char)0xbf, (char)0x8e, (char)0x9a };
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
	        (UCNV_FromUCallBack)MissingUnicodeAction_SKIP, to949legal ))
		log_err("u-> ibm-949 with skip did not match.\n");
    
    /*checking illegal value for ibm-943 with substitute*/ 
    if(!testConvertToUnicode(text943, sizeof(text943),
			 toUnicode943sub, sizeof(toUnicode943sub)/sizeof(toUnicode943sub[0]),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_SUBSTITUTE, fromIBM943Offssub ))
		log_err("ibm-943->u with subst did not match.\n");
	/*checking illegal value for ibm-943 with skip */
	if(!testConvertToUnicode(text943, sizeof(text943),
			 toUnicode943skip, sizeof(toUnicode943skip)/sizeof(toUnicode943skip[0]),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_SKIP, fromIBM943Offsskip ))
		log_err("ibm-943->u with skip did not match.\n");

	/*checking illegal value for ibm-943 with stop */
	if(!testConvertToUnicode(text943, sizeof(text943),
			 toUnicode943stop, sizeof(toUnicode943stop)/sizeof(toUnicode943stop[0]),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_STOP, fromIBM943Offsstop ))
		log_err("ibm-943->u with stop did not match.\n");

}
void TestSingleByte(int32_t inputsize, int32_t outputsize)
{
	const char sampleText[] = {(char)0x82, (char)0xa9, 'a', 'b', 'c' , (char)0x82, (char)0xff, /*(char)0x82, (char)0xa9,*/ '2', '3'};
	UChar toUnicode943sub[] = {0x304b, 0x0061, 0x0062, 0x0063,  0xfffd,/*0x304b,*/ 0x0032, 0x0033};
	int32_t  fromIBM943Offssub[]  = {0, 2, 3, 4, 5, 7, 8};
	/*checking illegal value for ibm-943 with substitute*/ 
	gInBufferSize = inputsize;
	gOutBufferSize = outputsize;
	
    if(!testConvertToUnicode(sampleText, sizeof(sampleText),
			 toUnicode943sub, sizeof(toUnicode943sub)/sizeof(toUnicode943sub[0]),"ibm-943",
	        (UCNV_ToUCallBack)MissingCharAction_SUBSTITUTE, fromIBM943Offssub ))
		log_err("ibm-943->u with subst did not match.\n");
}

bool_t testConvertFromUnicode(const UChar *source, int sourceLen,  const char *expect, int expectLen, 
			    const char *codepage, UCNV_FromUCallBack callback , int32_t *expectOffsets)
{
	
		
	UErrorCode status = ZERO_ERROR;
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
	bool_t checkOffsets = TRUE;
	bool_t doFlush;
	UCNV_FromUCallBack action;
	char junk[9999];
	char offset_str[9999];
	char expOff[9999];
	char *p;
	
	
	for(i=0;i<NEW_MAX_BUFFER;i++)
		junkout[i] = 0xF0;
	for(i=0;i<NEW_MAX_BUFFER;i++)
		junokout[i] = 0xFF;
	setNuConvTestName(codepage, "FROM");

	log_verbose("\nTesting========= %s  FROM \n  inputbuffer= %d   outputbuffer= %d\n", codepage, gInBufferSize, 
		    gOutBufferSize);

	conv = ucnv_open(codepage, &status);
	if(FAILURE(status))
	{
		log_err("Couldn't open converter %s\n",codepage);	
		return FALSE;
	}

	log_verbose("Converter opened..\n");
	/*----setting the callback routine----*/
	   
    
	ucnv_setFromUCallBack (conv,  /*action*/callback, &status);
	if (FAILURE(status)) 
    { 
		log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));  
	}
	action=ucnv_getFromUCallBack(conv);
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
	    

	    status = ZERO_ERROR;
 
	    ucnv_fromUnicode (conv,
			      &targ,
			      end,
			      &src,
			      sourceLimit,
			      checkOffsets ? offs : NULL,
			      doFlush, /* flush if we're at the end of the input data */
			      &status);
	
	  } while ( (status == INDEX_OUTOFBOUNDS_ERROR) || (sourceLimit < realSourceEnd) );
	    
	 if(FAILURE(status))
	  {
	    log_err("Problem tdoing fromUnicode, errcode %d %s\n", myErrorName(status), gNuConvTestName);
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
		printSeqErr(junkout, expectLen);
		printSeqErr(expect, expectLen);
		return FALSE;
	}
}

bool_t testConvertToUnicode( const char *source, int sourcelen, const UChar *expect, int expectlen, 
		       const char *codepage, UCNV_ToUCallBack callback, int32_t *expectOffsets)
{
	UErrorCode status = ZERO_ERROR;
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
	bool_t   checkOffsets = TRUE;
	char junk[9999];
	char offset_str[9999];
	UChar *p;
	UCNV_ToUCallBack action;	

	int32_t   realBufferSize;
	UChar *realBufferEnd;
	

	for(i=0;i<NEW_MAX_BUFFER;i++)
		junkout[i] = 0xFFFE;

	for(i=0;i<NEW_MAX_BUFFER;i++)
		junokout[i] = -1;
    
	setNuConvTestName(codepage, "TO");

	log_verbose("\n=========  %s\n", gNuConvTestName);

	conv = ucnv_open(codepage, &status);
	if(FAILURE(status))
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
	
       
	ucnv_setToUCallBack (conv, callback, &status);
	if (FAILURE(status)) 
    { 
		log_err("FAILURE in setting the callback Function! %s\n", myErrorName(status));  
	}

    action = ucnv_getToUCallBack(conv);
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

	  

	    status = ZERO_ERROR;

	    ucnv_toUnicode (conv,
			    &targ,
			    end,
			    &src,
			    srcLimit,
			    checkOffsets ? offs : NULL,
			    (srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
			    &status);

	   	   

	  } while ( (status == INDEX_OUTOFBOUNDS_ERROR) || (srcLimit < realSourceEnd) ); /* while we just need another buffer */
	/*check for an INVALID character for testing the call back function STOP*/
	if(status == INVALID_CHAR_FOUND || status == ILLEGAL_CHAR_FOUND )
	{
		for(p = junkout;p<targ;p++)
		  sprintf(junk + strlen(junk), "0x%04x, ", (0xFFFF) & (unsigned int)*p);
		/*		printUSeqErr(junkout, expectlen);*/
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
	if(FAILURE(status))
	{
		log_err("Problem doing toUnicode, errcode %d %s\n", myErrorName(status), gNuConvTestName);
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
		log_err("\n\ndid not get the expected offsets while %s \n", gNuConvTestName);			log_err("\nGot  : ");

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

