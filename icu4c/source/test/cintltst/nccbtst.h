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
#ifndef _NUCNVTST
#define _NUCNVTST
/* C API TEST FOR CALL BACK ROUTINES OF CODESET CONVERSION COMPONENT */
#include "cintltst.h"
#include "unicode/utypes.h"


static void TestSkipCallBack(void);
static void TestSubCallBack(void);
static void TestSubWithValueCallBack(void);
static void TestLegalAndOtherCallBack(void);
static void TestSingleByteCallBack(void);

static void TestSkip(int32_t inputsize, int32_t outputsize);

static void TestSub(int32_t inputsize, int32_t outputsize);

static void TestSubWithValue(int32_t inputsize, int32_t outputsize);

static void TestLegalAndOthers(int32_t inputsize, int32_t outputsize);
static void TestSingleByte(int32_t inputsize, int32_t outputsize);

UBool testConvertFromUnicode(const UChar *source, int sourceLen,  const char *expect, int expectLen, 
			    const char *codepage, UConverterFromUCallback callback, int32_t *expectOffsets);


UBool testConvertToUnicode( const char *source, int sourcelen, const UChar *expect, int expectlen, 
		       const char *codepage, UConverterToUCallback callback, int32_t *expectOffsets);


static void printSeq(const char* a, int len);
static void printUSeq(const UChar* a, int len);
static void printSeqErr(const char* a, int len);
static void printUSeqErr(const UChar* a, int len);
static void setNuConvTestName(const char *codepage, const char *direction);


#endif
