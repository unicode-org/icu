/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File NCCBTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda           creation
*********************************************************************************
*/
#ifndef _NCNVFBTS
#define _NCNVFBTS
/* C API TEST FOR FALL BACK ROUTINES OF CODESET CONVERSION COMPONENT */
#include "cintltst.h"
#include "unicode/utypes.h"

void TestConverterFallBack(void);

void TestConvertFallBackWithBufferSizes(int32_t outsize, int32_t insize );


static UBool testConvertFromUnicode(const UChar *source, int sourceLen,  const char *expect, int expectLen, 
			    const char *codepage, UBool fallback, int32_t *expectOffsets);


static UBool testConvertToUnicode( const char *source, int sourcelen, const UChar *expect, int expectlen, 
		       const char *codepage, UBool fallback, int32_t *expectOffsets);


static void printSeq(const char* a, int len);
static void printUSeq(const UChar* a, int len);
static void printSeqErr(const char* a, int len);
static void printUSeqErr(const UChar* a, int len);
static void setNuConvTestName(const char *codepage, const char *direction);


#endif
