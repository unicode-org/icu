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
#include "uloc.h"
#include "ucnv.h"
#include "ucnv_err.h"
#include "cintltst.h"
#include "utypes.h"
#include "ustring.h"

static void printSeq(const char* a, int len);
static void printUSeq(const UChar* a, int len);

void TestNewConvertWithBufferSizes(int32_t osize, int32_t isize) ;
void TestConverterTypesAndStarters();

#define NEW_MAX_BUFFER 999

static int32_t  gInBufferSize = 0;
static int32_t  gOutBufferSize = 0;
static char     gNuConvTestName[1024];

#define nct_min(x,y)  ((x<y) ? x : y)

void printSeq(const char* a, int len)
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

void printSeqErr(const char* a, int len)
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

void TestInBufSizes()
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

void TestOutBufSizes()
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
			    (srcLimit == realSourceEnd), /* flush if we're at the end of hte source data */
			    &status);

	    /*	    offs += (targ-oldTarg); */

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
	UChar	sampleText[] = 
	 { 0x0031, 0x0032, 0x0033, 0x4e00, 0x4e8c, 0x4e09, 0x002E	 };

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
	const char  maltesechars[]  = { '\0' };
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
	if(!testConvertToU(expectedUTF8, sizeof(expectedUTF8),
			   sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "utf8", fmUTF8Offs ))
	  log_err("utf8 -> u did not match\n");

	if(!testConvertToU(expectedISO2022, sizeof(expectedISO2022),
			   sampleText, sizeof(sampleText)/sizeof(sampleText[0]), "iso-2022", fmISO2022Offs ))
	
		log_err("iso-2022  -> u  did not match");

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
}

void TestConverterTypesAndStarters()
{
	UConverter* myConverter[3];
	UErrorCode err = U_ZERO_ERROR;
	bool_t mystarters[256];
	int i;
	
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
	if (U_FAILURE(err)) log_err("Failed to create an ibm-949 converter\n");
	myConverter[1] = ucnv_open("ibm-930", &err);
	if (U_FAILURE(err)) log_err("Failed to create an ibm-930 converter\n");
	myConverter[2] = ucnv_open("ibm-878", &err);
	if (U_FAILURE(err)) log_err("Failed to create an ibm-815 converter\n");

	if (ucnv_getType(myConverter[0])!=UCNV_MBCS) log_err("ucnv_getType Failed for ibm-949\n");
	else log_verbose("ucnv_getType ibm-949 ok\n");
	if (ucnv_getType(myConverter[1])!=UCNV_EBCDIC_STATEFUL) log_err("ucnv_getType Failed for ibm-930\n");
	else log_verbose("ucnv_getType ibm-930 ok\n");
	if (ucnv_getType(myConverter[2])!=UCNV_SBCS) log_err("ucnv_getType Failed for ibm-815\n");
	else log_verbose("ucnv_getType ibm-815 ok\n");


	ucnv_getStarters(myConverter[0], mystarters, &err);
	/*if (memcmp(expectedKSCstarters, mystarters, sizeof(expectedKSCstarters)))
		log_err("Failed ucnv_getStarters for ksc\n");
	else
		log_verbose("ucnv_getStarters ok\n");*/
	
	ucnv_close(myConverter[0]);
	ucnv_close(myConverter[1]);
	ucnv_close(myConverter[2]);
}
