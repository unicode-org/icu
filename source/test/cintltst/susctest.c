/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File test.c
*
* Modification History:
*
*   Date        Name        Description
*   05/17/99    stephen	    Creation (ported from java)
*   09/24/99    stephen     Added new test for data split on decompression.
*******************************************************************************
*/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <signal.h>
#include <string.h>

#include "scsu.h"
#include "ustring.h"
#include "utypes.h"
#include "cintltst.h"

#ifdef MIN
#  undef MIN
#endif

#define MIN(a,b) (a < b ? a : b)

#ifdef MAX
#  undef MAX
#endif

#define MAX(a,b) (a > b ? a : b)


/* Compression modes */
#define SINGLEBYTEMODE 0
#define UNICODEMODE 1


/* Single-byte mode tags */
#define SDEFINEX 0x0B
/* 0x0C is a reserved value*/
#define SRESERVED 0x0C
#define SQUOTEU 0x0E
#define SCHANGEU 0x0F

#define SQUOTE0 0x01
#define SQUOTE1 0x02
#define SQUOTE2 0x03
#define SQUOTE3 0x04
#define SQUOTE4 0x05
#define SQUOTE5 0x06
#define SQUOTE6 0x07
#define SQUOTE7 0x08

#define SCHANGE0 0x10
#define SCHANGE1 0x11
#define SCHANGE2 0x12
#define SCHANGE3 0x13
#define SCHANGE4 0x14
#define SCHANGE5 0x15
#define SCHANGE6 0x16
#define SCHANGE7 0x17

#define SDEFINE0 0x18
#define SDEFINE1 0x19
#define SDEFINE2 0x1A
#define SDEFINE3 0x1B
#define SDEFINE4 0x1C
#define SDEFINE5 0x1D
#define SDEFINE6 0x1E
#define SDEFINE7 0x1F

/* Unicode mode tags */
#define UCHANGE0 0xE0
#define UCHANGE1 0xE1
#define UCHANGE2 0xE2
#define UCHANGE3 0xE3
#define UCHANGE4 0xE4
#define UCHANGE5 0xE5
#define UCHANGE6 0xE6
#define UCHANGE7 0xE7

#define UDEFINE0 0xE8
#define UDEFINE1 0xE9
#define UDEFINE2 0xEA
#define UDEFINE3 0xEB
#define UDEFINE4 0xEC
#define UDEFINE5 0xED
#define UDEFINE6 0xEE
#define UDEFINE7 0xEF

#define UQUOTEU 0xF0
#define UDEFINEX 0xF1

static int32_t 
digitvalue(char c)
{
  return c - 0x30 - (c >= 0x41 ? (c >= 0x61 ? 39 : 7) : 0);
}

static UChar* 
unescape(const char *s)
{
  UChar *retval;
  UChar *alias;

  retval = (UChar*) calloc(strlen(s) + 1, sizeof(UChar));
  if(retval == 0) {
    printf("calloc error at line %d.\n", __LINE__);
    exit(1);
  }

  alias = retval;

  while(*s != '\0') {
    if(*s == '\\') {
      int32_t value;
      ++s; /* skip '\' */
      value = digitvalue(*s++);
      value *= 16;
      value += digitvalue(*s++);
      value *= 16;
      value += digitvalue(*s++);
      value *= 16;
      value += digitvalue(*s++);

      *alias++ = value;
    }
    else
      *alias++ = *s++;
  }

  *alias = 0x0000;

  return retval;
}

static void
printChars(const UChar *chars, 
	   int32_t len)
{
  int32_t i;

  for(i = 0; i < len; i++) {
    printf("%#x ", chars[i]);
  }
  puts("");
}


static void 
printChars2(const UChar *chars, 
	    int32_t len)
{
  int32_t i;
  
  for(i = 0; i < len; i++) {
    if(chars[i] < 0x0020 || chars[i] > 0x007E)
      printf("[%#x]", chars[i]);
    else
      printf("%c", chars[i]);
  }
  puts("");
}


static void 
printBytes(const uint8_t *byteBuffer, 
	   int32_t len)
{
  int32_t curByteIndex = 0;
  int32_t byteBufferLimit = len;
  int32_t mode = SINGLEBYTEMODE;
  int32_t aByte = 0x00;
  
  while(curByteIndex < byteBufferLimit) {
    switch(mode) {  
    case SINGLEBYTEMODE:
      while(curByteIndex < byteBufferLimit && mode == SINGLEBYTEMODE) {
	aByte = byteBuffer[curByteIndex++] & 0xFF;
	switch(aByte) {
	default:
	  printf("%#x ", aByte);
	  break;
	  
	  
	  /* quote unicode*/
	case SQUOTEU:
	  printf("SQUOTEU ");
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	  /* switch to Unicode mode*/
	case SCHANGEU:
	  printf("SCHANGEU ");
	  mode = UNICODEMODE;
	  break;
	  
	  /* handle all quote tags*/
	case SQUOTE0: 	case SQUOTE1: 	case SQUOTE2:	case SQUOTE3:
	case SQUOTE4: 	case SQUOTE5: 	case SQUOTE6:	case SQUOTE7:
	  printf("SQUOTE%d ", aByte - SQUOTE0);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	  /* handle all switch tags*/
	case SCHANGE0: 	case SCHANGE1: 	case SCHANGE2:	case SCHANGE3:
	case SCHANGE4: 	case SCHANGE5: 	case SCHANGE6:	case SCHANGE7:
	  printf("SCHANGE%d ", aByte - SCHANGE0);
	  break;
	  
	  /* handle all define tags*/
	case SDEFINE0: 	case SDEFINE1: 	case SDEFINE2:	case SDEFINE3:
	case SDEFINE4: 	case SDEFINE5: 	case SDEFINE6:	case SDEFINE7:
	  printf("SDEFINE%d ", aByte - SDEFINE0);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	  /* handle define extended tag*/
	case SDEFINEX:
	  printf("SDEFINEX ");
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	} /* end switch*/
      } /* end while*/
      break;
      
    case UNICODEMODE:
      while(curByteIndex < byteBufferLimit && mode == UNICODEMODE) {

	aByte = byteBuffer[curByteIndex++] & 0xFF;

	switch(aByte) {
	  /* handle all define tags*/
	case UDEFINE0: 	case UDEFINE1: 	case UDEFINE2:	case UDEFINE3:
	case UDEFINE4: 	case UDEFINE5: 	case UDEFINE6:	case UDEFINE7:
	  printf("UDEFINE%d ", aByte - UDEFINE0);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  mode = SINGLEBYTEMODE;
	  break;
	  
	  /* handle define extended tag*/
	case UDEFINEX:
	  printf("UDEFINEX ");
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	  /* handle all switch tags*/
	case UCHANGE0: 	case UCHANGE1: 	case UCHANGE2:	case UCHANGE3:
	case UCHANGE4: 	case UCHANGE5: 	case UCHANGE6:	case UCHANGE7:
	  printf("UCHANGE%d ", aByte - UCHANGE0);
	  mode = SINGLEBYTEMODE;
	  break;
	  
	  /* quote unicode*/
	case UQUOTEU:
	  printf("UQUOTEU ");
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	default:
	  printf("%#x ", aByte);
	  if(curByteIndex < byteBufferLimit)
	    printf("%#x ", byteBuffer[curByteIndex++]);
	  break;
	  
	} /* end switch*/
      } /* end while*/
      break;
      
    } /* end switch( mode )*/
  } /* end while*/
  
  puts("");
}

static bool_t
printDiffs(const UChar *s1, 
	   int32_t s1len, 
	   const UChar *s2, 
	   int32_t s2len)
{
  bool_t result  = FALSE;
  int32_t len;
  int32_t i;

  if(s1len != s2len) {
    puts("====================");
    printf("Length doesn't match: expected %d, got %d\n", s1len, s2len);
    puts("Expected:");
    printChars(s1, s1len);
    puts("Got:");
    printChars(s2, s2len);
    result = TRUE;
  }
  
  len = (s1len < s2len ? s1len : s2len);
  for(i = 0; i < len; ++i) {
    if(s1[i] != s2[i]) {
      if(result == FALSE)
	puts("====================");
      printf("First difference at char %d\n", i);
      printf("Exp. char: %#x\n", s1[i]);
      printf("Got char : %#x\n", s2[i]);
      puts("Expected:");
      printChars(s1, s1len);
      puts("Got:");
      printChars(s2, s2len);
      result = TRUE;
      break;
    }
  }
  
  return result;
}

/* generate a run of characters in a "window" */
static void
randomRun(UChar *target, 
	  int32_t pos, 
	  int32_t len)
{
  int32_t offset = (int32_t)(0xFFFF * (double)(rand()/(double)RAND_MAX));
  int32_t i;

  /* don't overflow 16 bits*/
  if(offset > 0xFF80)
    offset = 0xFF80;
  
  for(i = pos; i < pos + len; i++) {
    target[i] = (UChar) (offset + (int32_t)(0x7F * (double)(rand()/(double)RAND_MAX)));
  }
}

/* generate a string of characters, with simulated runs of characters */
static UChar* 
randomChars(int32_t len)
{
  UChar *result = 0;
  int32_t runLen = 0;
  int32_t used = 0;

  result = (UChar*) calloc(len, sizeof(UChar));
  if(result == 0) {
    printf("calloc error at line %d.\n", __LINE__);
    exit(1);
  }

  while(used < len) {
    runLen = (int32_t)(30 * (double)(rand()/(double)RAND_MAX));
    if(used + runLen >= len)
      runLen = len - used;
    randomRun(result, used, runLen);
    used += runLen;
  }
  
  return result;
}

static void 
myTest(const UChar *chars, 
       int32_t len)
{
  UnicodeCompressor myCompressor;

  /* compression variables */
  uint8_t *myCompressed  = 0;
  uint8_t *myCTarget = 0;
  int32_t myCTargetSize = MAX(512, 3*len);
  const UChar *myCSource = chars;

  /* decompression variables */
  UChar *myDecompressed = 0;
  UChar *myDTarget = 0;
  int32_t myDTargetSize = MAX(2*len, 2);
  const uint8_t *myDSource = 0;
  
  /* variables for my compressor */
  int32_t myByteCount = 0;
  int32_t myCharCount = 0;

  /* error code */
  UErrorCode status = U_ZERO_ERROR;


  /* allocate memory */
  myCompressed = (uint8_t*) calloc(myCTargetSize, sizeof(uint8_t));
  myDecompressed = (UChar*) calloc(myDTargetSize, sizeof(UChar));
  
  if(myCompressed == 0 || myDecompressed == 0) {
    printf("calloc error at line %d.\n", __LINE__);
    exit(1);
  }
  
  /* init compressor */
  scsu_init(&myCompressor);

  /* compress */
  myCTarget = myCompressed;
  scsu_compress(&myCompressor,
		&myCTarget,
		myCTarget + myCTargetSize,
		&myCSource,
		myCSource + len,
		&status);

  if(U_FAILURE(status)) {
    printf("Failing status code at line %d.\n", __LINE__);
    exit(1);
  }

  myByteCount = (myCTarget - myCompressed);

  /* reset */
  scsu_reset(&myCompressor);

  /* decompress */
  myDTarget = myDecompressed;
  myDSource = myCompressed;
  scsu_decompress(&myCompressor,
		  &myDTarget,
		  myDTarget + myDTargetSize,
		  &myDSource,
		  myDSource + myByteCount,
		  &status);

  if(U_FAILURE(status)) {
    printf("Failing status code at line %d.\n", __LINE__);
    exit(1);
  }
  
  myCharCount = (myDTarget - myDecompressed);

  /* find differences */
  if( printDiffs(chars, len, myDecompressed, myCharCount) == FALSE) {
    /*printf("%d chars ===> %d bytes ===> %d chars (%f)\n", len, 
      myByteCount, myCharCount, (double)(myByteCount/(myCharCount*2.0)));*/
  }
  else {
    puts("Compressed:");
    printBytes(myCompressed, myByteCount);
  }

  /* clean up */
  free(myCompressed);
  free(myDecompressed);
}

/* tweak these; COMPRESSIONBUFFERSIZE must not be less than 4, and
   DECOMPRESSIONBUFFERSIZE must not be less than 2 */
#define COMPRESSIONBUFFERSIZE 4
#define DECOMPRESSIONBUFFERSIZE 2

static void 
myMultipassTest(const UChar *chars, 
		int32_t len)
{
  UnicodeCompressor myCompressor;

  /* compression variables */
  uint8_t myCompressionBuffer [COMPRESSIONBUFFERSIZE];
  uint8_t *myCompressed  = 0;
  uint8_t *myCTarget = 0;
  int32_t myCTargetSize = MAX(512, 3 * len);
  const UChar *myCSource = chars;
  const UChar *myCSourceAlias = 0;

  /* decompression variables */
  UChar myDecompressionBuffer [DECOMPRESSIONBUFFERSIZE];
  UChar *myDecompressed = 0;
  UChar *myDTarget = 0;
  int32_t myDTargetSize = MAX(2 * len, 2);
  const uint8_t *myDSource = 0;
  const uint8_t *myDSourceAlias = 0;
  
  /* counts */
  int32_t totalCharsCompressed    = 0;
  int32_t totalBytesWritten       = 0;
  
  int32_t totalBytesDecompressed  = 0;
  int32_t totalCharsWritten       = 0;
 
  /* error code */
  UErrorCode status = U_ZERO_ERROR;
  
  /* allocate memory */
  myCompressed = (uint8_t*) calloc(myCTargetSize, sizeof(uint8_t));
  myDecompressed = (UChar*) calloc(myDTargetSize, sizeof(UChar));
  
  if(myCompressed == 0 || myDecompressed == 0) {
    printf("calloc error at line %d.\n", __LINE__);
    exit(1);
  }
  
  /* init compressor */
  scsu_init(&myCompressor);
  
  /* perform the compression in a loop */
  do {      
    status = U_ZERO_ERROR;
    myCTarget = myCompressionBuffer;    
    myCSourceAlias = myCSource;
    
    scsu_compress(&myCompressor,
		  &myCTarget,
		  myCTarget + COMPRESSIONBUFFERSIZE,
		  &myCSource,
		  chars + len,
		  &status);
    
    if(status != U_INDEX_OUTOFBOUNDS_ERROR && U_FAILURE(status)) {
      printf("Failing status code at line %d.\n", __LINE__);
      exit(1);
    }
    
    /* copy the newly-compressed chunk to the target */
    memcpy(myCompressed + totalBytesWritten,
	   myCompressionBuffer,
	   sizeof(uint8_t) * (myCTarget - myCompressionBuffer));
    
    /*      printf("Compression pass complete.  Compressed %d chars into %d bytes\n",
	    (myCSource - myCSourceAlias), (myCTarget - myCompressionBuffer));*/
    
    /* update pointers */
    totalCharsCompressed = (myCSource - chars);
    
    totalBytesWritten += (myCTarget - myCompressionBuffer);
    
  } while(status == U_INDEX_OUTOFBOUNDS_ERROR/*totalCharsCompressed < len*/);
  
  /* reset */
  scsu_reset(&myCompressor);
  
  /* set up decompression params */
  myDSource = myCompressed;

  /* perform the decompression in a loop */
  do {
    status = U_ZERO_ERROR;
    myDTarget = myDecompressionBuffer;
    myDSourceAlias = myDSource;
    
    scsu_decompress(&myCompressor,
		    &myDTarget,
		    myDTarget + DECOMPRESSIONBUFFERSIZE,
		    &myDSource,
		    myCompressed + totalBytesWritten,
		    &status);
    
    if(status != U_INDEX_OUTOFBOUNDS_ERROR && U_FAILURE(status)) {
      printf("Failing status code at line %d.\n", __LINE__);
      exit(1);
    }
    
    /* copy the newly-decompressed chunk to the target */
    memcpy(myDecompressed + totalCharsWritten,
	   myDecompressionBuffer,
	   sizeof(UChar) * (myDTarget - myDecompressionBuffer));

    /*    printf("Decompression pass complete.  Decompressed %d bytes into %d chars\n",
	  (myDSource - myDSourceAlias), (myDTarget - myDecompressionBuffer));*/
    
    /* update pointers */
    totalBytesDecompressed = (myDSource - myCompressed);

    totalCharsWritten += (myDTarget - myDecompressionBuffer);
  
  } while(status == U_INDEX_OUTOFBOUNDS_ERROR/*totalBytesDecompressed < totalBytesWritten*/);
  
  /* find differences */
  if( printDiffs(chars, len, myDecompressed, totalCharsWritten) == FALSE) {
    /*printf("%d chars ===> %d bytes ===> %d chars (%f) (MP)\n", len, 
	   totalBytesWritten, totalCharsWritten,
	   (double)(totalBytesWritten/(totalCharsWritten*2.0)));*/
  }
  else {
    puts("Compressed:");
    printBytes(myCompressed, totalBytesWritten);
  }

  /* clean up */
  free(myCompressed);
  free(myDecompressed);
}

static char *fTestCases [] = {
  "Hello \\9292 \\9192 World!",
  "Hell\\0429o \\9292 \\9192 W\\00e4rld!",
  "Hell\\0429o \\9292 \\9292W\\00e4rld!",
  
  "\\0648\\06c8", /* catch missing reset*/
  "\\0648\\06c8",
  
  "\\4444\\E001", /* lowest quotable*/
  "\\4444\\f2FF", /* highest quotable*/
  "\\4444\\f188\\4444",
  "\\4444\\f188\\f288",
  "\\4444\\f188abc\0429\\f288",
  "\\9292\\2222",
  "Hell\\0429\\04230o \\9292 \\9292W\\00e4\\0192rld!",
  "Hell\\0429o \\9292 \\9292W\\00e4rld!",
  "Hello World!123456",
  "Hello W\\0081\\011f\\0082!", /* Latin 1 run*/
  
  "abc\\0301\\0302",  /* uses SQn for u301 u302*/
  "abc\\4411d",      /* uses SQU*/
  "abc\\4411\\4412d",/* uses SCU*/
  "abc\\0401\\0402\\047f\\00a5\\0405", /* uses SQn for ua5*/
  "\\9191\\9191\\3041\\9191\\3041\\3041\\3000", /* SJIS like data*/
  "\\9292\\2222",
  "\\9191\\9191\\3041\\9191\\3041\\3041\\3000",
  "\\9999\\3051\\300c\\9999\\9999\\3060\\9999\\3065\\3065\\3065\\300c",
  "\\3000\\266a\\30ea\\30f3\\30b4\\53ef\\611b\\3044\\3084\\53ef\\611b\\3044\\3084\\30ea\\30f3\\30b4\\3002",
  
  "", /* empty input*/
  "\\0000", /* smallest BMP character*/
  "\\FFFF", /* largest BMP character*/
  
  "\\d800\\dc00", /* smallest surrogate*/
  "\\d8ff\\dcff", /* largest surrogate pair*/
  
  /* regression tests*/
  "\\6441\\b413\\a733\\f8fe\\eedb\\587f\\195f\\4899\\f23d\\49fd\\0aac\\5792\\fc22\\fc3c\\fc46\\00aa",
  "\\00df\\01df\\f000\\dbff\\dfff\\000d\n\\0041\\00df\\0401\\015f\\00df\\01df\\f000\\dbff\\dfff",
  "\\30f9\\8321\\05e5\\181c\\d72b\\2019\\99c9\\2f2f\\c10c\\82e1\\2c4d\\1ebc\\6013\\66dc\\bbde\\94a5\\4726\\74af\\3083\\55b9\\000c",
  "\\0041\\00df\\0401\\015f",
  "\\9066\\2123abc",
  "\\d266\\43d7\\\\e386\\c9c0\\4a6b\\9222\\901f\\7410\\a63f\\539b\\9596\\482e\\9d47\\cfe4\\7b71\\c280\\f26a\\982f\\862a\\4edd\\f513\\fda6\\869d\\2ee0\\a216\\3ff6\\3c70\\89c0\\9576\\d5ec\\bfda\\6cca\\5bb3\\bcea\\554c\\914e\\fa4a\\ede3\\2990\\d2f5\\2729\\5141\\0f26\\ccd8\\5413\\d196\\bbe2\\51b9\\9b48\\0dc8\\2195\\21a2\\21e9\\00e4\\9d92\\0bc0\\06c5",
  "\\f95b\\2458\\2468\\0e20\\f51b\\e36e\\bfc1\\0080\\02dd\\f1b5\\0cf3\\6059\\7489",
  0
  
};

static unsigned long gTotalChars;

/* unused unless this is run as a main in an infinite loop */
void
signal_handler(int signal)
{
  printf("total chars compressed = %llu\n", gTotalChars);
  exit(0);
}


/* Decompress the two segments */
static UChar*
segment_test(uint8_t *segment1,
	     int32_t seg1Len,
	     uint8_t *segment2,
	     int32_t seg2Len)
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeCompressor myDecompressor;

    const uint8_t *seg1 = segment1;
    const uint8_t *seg2 = segment2;

    int32_t charBufferCap = 2*(seg1Len + seg2Len);
    UChar *charBuffer = (UChar*) malloc(sizeof(UChar) * charBufferCap);

    UChar *target = charBuffer;
    int32_t outCount = 0, count1 = 0, count2 = 0;


    scsu_init(&myDecompressor);

    scsu_decompress(&myDecompressor, &target, charBuffer + charBufferCap,
		    &seg1, segment1 + seg1Len, &status);

    count1 = seg1 - segment1;

    /*    println("Segment 1 (" + segment1.length + " bytes) " +
	  "decompressed into " + count1  + " chars");
	  println("Bytes consumed: " + bytesRead[0]);
    
	  print("Got chars: ");
	  println(System.out, charBuffer, 0, count1);*/

    /*s.append(charBuffer, 0, count1);*/

    scsu_decompress(&myDecompressor, &target,
		    charBuffer + charBufferCap,
		    &seg2, segment2 + seg2Len, &status);

    count2 = seg2 - segment2;

    outCount = (target - charBuffer);

    /*    println("Segment 2 (" + segment2.length + " bytes) " +
	  "decompressed into " + count2  + " chars");
	  println("Bytes consumed: " + bytesRead[0]);
	  
	  print("Got chars: ");
	  println(System.out, charBuffer, count1, count2);*/
    
    /*s.append(charBuffer, count1, count2);*/
    
    /*print("Result: ");
      println(System.out, charBuffer, 0, count1 + count2);
      println("====================");*/
    
    charBuffer [ outCount ] = 0x0000;
    return charBuffer;
}


int
TestSCSU() 
{
  UChar *chars = 0;
  int32_t len = 0;
  int32_t i;

  /* multi-segment test data */

  /* compressed segment breaking on a define window sequence */
  /*                       B     o     o     t     h     SD1  */
  uint8_t segment1a [] = { 0x42, 0x6f, 0x6f, 0x74, 0x68, 0x19 };
  /*                       IDX   ,           S     .          */
  uint8_t segment1b [] = { 0x01, 0x2c, 0x20, 0x53, 0x2e };
  /* expected result */
  UChar result1 [] = { 0x0042, 0x006f, 0x006f, 0x0074, 0x0068, 
		       0x002c, 0x0020, 0x0053, 0x002e, 0x0000 };

  /* compressed segment breaking on a quote unicode sequence */
  /*                       B     o     o     t     SQU        */
  uint8_t segment2a [] = { 0x42, 0x6f, 0x6f, 0x74, 0x0e, 0x00 };

  /*                       h     ,           S     .          */
  uint8_t segment2b [] = { 0x68, 0x2c, 0x20, 0x53, 0x2e };
  /* expected result */
  UChar result2 [] = { 0x0042, 0x006f, 0x006f, 0x0074, 0x0068, 
		       0x002c, 0x0020, 0x0053, 0x002e, 0x0000 };

  /* compressed segment breaking on a quote unicode sequence */
  /*                       SCU   UQU                         */
  uint8_t segment3a [] = { 0x0f, 0xf0, 0x00 };
    
  /*                       B                                 */
  uint8_t segment3b [] = { 0x42 };
  /* expected result */
  UChar result3 [] = { 0x0042, 0x0000 };


  chars = segment_test(segment1a, 6, segment1b, 5);
  if(u_strcmp(chars, result1)) {
    log_err("Failure in multisegment 1\n");
  }
  free(chars);

  chars = segment_test(segment2a, 6, segment2b, 5);
  if(u_strcmp(chars, result2)) {
    log_err("Failure in multisegment 2\n");
  }
  free(chars);

  chars = segment_test(segment3a, 3, segment3b, 1);
  if(u_strcmp(chars, result3)) {
    log_err("Failure in multisegment 3\n");
  }
  free(chars);

  /* register to handle interrupts */
  /*signal(SIGHUP, signal_handler);*/
  /*signal(SIGINT, signal_handler);*/

  /* initialize char count */
  gTotalChars = 0;

  /* initialize random number generator */
  srand(time(0));
  
  for(i = 0; fTestCases[i] != 0; i++) {
    
    chars = unescape(fTestCases[i]);
    len = u_strlen(chars);

    /*printChars2(chars, len);*/

    myTest(chars, len);
    myMultipassTest(chars, len);

    
    free(chars);
    gTotalChars += len;
  }

  /*puts("==============================");*/
    i=0;
  while(i<=1000) {
    len = (int32_t)(1000 * (double)(rand()/(double)RAND_MAX));
    if(len == 0) /* 0-length malloc will fail */
      len = 10;
    chars = randomChars(len);
    myTest(chars, len);
    myMultipassTest(chars, len);
    free(chars);
    gTotalChars += len;
	i++;
  }

  return 0;
}

void
addSUSCTest(TestNode** root)
{
  addTest(root, &TestSCSU, "scsutest/TestSCSU");
}
