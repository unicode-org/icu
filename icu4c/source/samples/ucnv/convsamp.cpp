/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  convsamp.c
*   encoding:   ASCII (7-bit)
*
*   created on: 2000may30
*   created by: Steven R. Loomis
*
*   Sample code for the ICU conversion routines.
*
* Note: Nothing special is needed to build this sample. Link with
*       the icu UC and icu I18N libraries. 
* 
*       I use 'assert' for error checking, you probably will want
*       something more flexible.  '***BEGIN SAMPLE***' and 
*       '***END SAMPLE***' mark pieces suitable for stand alone
*       code snippets.
*
*
*  Each test can define it's own BUFFERSIZE 
*
*/

#include <stdio.h>
#include <ctype.h>            /* for isspace, etc.    */
#include <assert.h>
#include <string.h>
#include <stdlib.h>  /* malloc */

#include "unicode/utypes.h"   /* Basic ICU data types */
#include "unicode/ucnv.h"     /* C   Converter API    */
#include "unicode/convert.h"  /* C++ Converter API    */
#include "unicode/ustring.h"  /* some more string fcns*/
#include "unicode/uloc.h"


#include "flagcb.h"

/* Some utility functions */

static const UChar kNone[] = { 0x0000 };

#define U_ASSERT(x)  { if(U_FAILURE(x)) {fflush(stdout);fflush(stderr); fprintf(stderr, #x " == %s\n", u_errorName(x)); assert(U_SUCCESS(x)); }}

/* Print a UChar if possible, in seven characters. */
void prettyPrintUChar(UChar c)
{
  if(  (c <= 0x007F) &&
       (isgraph(c))  ) {
    printf("  '%c'  ", (char)(0x00FF&c));
  } else if ( c > 0x007F ) {
    char buf[1000];
    UErrorCode status = U_ZERO_ERROR;
    UTextOffset o;
    
    o = u_charName(c, U_UNICODE_CHAR_NAME, buf, 1000, &status);
    if(U_SUCCESS(status) && (o>0) ) {
      buf[6] = 0;
      printf("%7s", buf);
    } else {
      o = u_charName(c, U_UNICODE_10_CHAR_NAME, buf, 1000, &status);
      if(U_SUCCESS(status) && (o>0)) {
        buf[5] = 0;
        printf("~%6s", buf);
      }
      else {
        printf("??????? ");
      }
    }
  } else {
    switch((char)(c & 0x007F)) {
    case ' ':
      printf("  ' '  ");
      break;
    case '\t':
      printf("  \\t   ");
      break;
    case '\n':
      printf("  \\n   ");
      break;
    default:
      printf("   _   ");
      break;
    }
  }
}


void printUChars(const char  *name = "?", 
                 const UChar *uch  = kNone,
                 int32_t     len   = -1 )
{
  int32_t i;

  if( (len == -1) && (uch) ) {
    len = u_strlen(uch);
  }

  printf("%5s:", name);
  for( i = 0; i <len; i++) {
    printf("%-6d ", i);
  }
  printf("\n");

  printf("%5s: ", "uni");
  for( i = 0; i <len; i++) {
    printf("\\u%04X ", (int)uch[i]);
  }
  printf("\n");

  printf("%5s: ", "ch");
  for( i = 0; i <len; i++) {
    prettyPrintUChar(uch[i]);
  }
  printf("\n");
}

void printString(const char *name, const UnicodeString& string)
{
  UChar *uch;
  int32_t len = string.size();
  uch = (UChar*)malloc(sizeof(UChar)*(len+1));
  string.extract(0,len,uch,0);
  uch[len]=0;
  printUChars(name, uch, -1);
  delete(uch);
}

void printBytes(const char  *name = "?", 
                 const char *uch  = "",
                 int32_t     len   = -1 )
{
  int32_t i;

  if( (len == -1) && (uch) ) {
    len = strlen(uch);
  }

  printf("%5s:", name);
  for( i = 0; i <len; i++) {
    printf(" %- 4d", i);
  }
  printf("\n");

  printf("%5s: ", "uni");
  for( i = 0; i <len; i++) {
    printf("\\x%02X ", 0x00FF & (int)uch[i]);
  }
  printf("\n");

  printf("%5s: ", "ch");
  for( i = 0; i <len; i++) {
    if(isgraph(uch[i])) {
      printf(" '%c' ", (char)uch[i]);
    } else {
      printf("     ");
    }
  }
  printf("\n");
}

void printUChar(UChar32 ch32)
{
    if(ch32 > 0xFFFF) {
      printf("ch: U+%06X\n", ch32);
    }
    else {
      UChar ch = ch32;
      printUChars("C", &ch, 1);
    }
}

/*******************************************************************
  Very simple C++ sample to convert the word 'Moscow' in Russian, followed
  by an exclamation mark (!) into the KOI8-R Russian code page.

  This example first creates a UnicodeString out of the Unicode chars.

  targetSize must be set to the amount of space available in the target
  buffer. After UnicodeConverter::fromUnicodeString() is called, 
  targetSize will contain the number of bytes in target[] which were
  used in the resulting codepage.  In this case, there is a 1:1 mapping
  between the input and output characters. The exclamation mark has the
  same value in both KOI8-R and Unicode.

  src: 0      1      2      3      4      5      6     
  uni: \u041C \u043E \u0441 \u043A \u0432 \u0430 \u0021 
   ch: CYRILL CYRILL CYRILL CYRILL CYRILL CYRILL   '!'  

 targ:  0    1    2    3    4    5    6  
  uni: \xED \xCF \xD3 \xCB \xD7 \xC1 \x21 
   ch:                                '!' 


 */
UErrorCode convsample_01()
{
  printf("\n\n==============================================\n"
         "Sample 01: C++: simple Unicode -> koi8-r conversion\n");


  // **************************** START SAMPLE *******************
  // "Moscva!" in cyrillic letters, to be converted to the KOI8-R
  // Russian code page.
  UChar source[] = { 0x041C, 0x043E, 0x0441, 0x043A, 0x0432,
                     0x0430, 0x0021, 0x0000 };
  char target[100];
  int32_t targetSize = sizeof(target);
  UnicodeString myString(source);
  UErrorCode status = U_ZERO_ERROR;

  // set up the converter
  UnicodeConverter conv("koi8-r", status);
  U_ASSERT(status);

  // convert to KOI8-R
  conv.fromUnicodeString(target, targetSize, myString, status);
  U_ASSERT(status);

  // ***************************** END SAMPLE ********************
  
  // Print it out
  printUChars("src", source);
  printf("\n");
  printBytes("targ", target, targetSize);

  return U_ZERO_ERROR;
}


/******************************************************
  Similar sample to the preceding one. 
  You must call ucnv_close to clean up the memory used by the
  converter.

  'len' returns the number of OUTPUT bytes resulting from the 
  conversion.
 */

UErrorCode convsample_02()
{
  printf("\n\n==============================================\n"
         "Sample 02: C: simple Unicode -> koi8-r conversion\n");


  // **************************** START SAMPLE *******************
  // "cat<cat>OK"
  UChar source[] = { 0x041C, 0x043E, 0x0441, 0x043A, 0x0432,
                     0x0430, 0x0021, 0x0000 };
  char target[100];
  UErrorCode status = U_ZERO_ERROR;
  UConverter *conv;
  int32_t     len;

  // set up the converter
  conv = ucnv_open("koi8-r", &status);
  assert(U_SUCCESS(status));

  // convert to koi8-r
  len = ucnv_fromUChars(conv, target, 100, source, -1, &status);
  assert(U_SUCCESS(status));

  // close the converter
  ucnv_close(conv);

  // ***************************** END SAMPLE ********************
  
  // Print it out
  printUChars("src", source);
  printf("\n");
  printBytes("targ", target, len);

  return U_ZERO_ERROR;
}


UErrorCode convsample_03()
{
  printf("\n\n==============================================\n"
         "Sample 03: C: print out all converters\n");

  int32_t count;
  int32_t i;

  // **************************** START SAMPLE *******************
  count = ucnv_countAvailable();
  printf("Available converters: %d\n", count);
  
  for(i=0;i<count;i++) 
  {
    printf("%s ", ucnv_getAvailableName(i));
  }

  // ***************************** END SAMPLE ********************
  
  printf("\n");

  return U_ZERO_ERROR;
}



#define BUFFERSIZE 17 /* make it interesting :) */

/*
  Converting from a codepage to Unicode in bulk..
  What is the best way to determine the buffer size?

     The 'buffersize' is in bytes of input.
    For a given converter, divinding this by the minimum char size
    give you the maximum number of Unicode characters that could be
    expected for a given number of input bytes.
     see: ucnv_getMinCharSize()

     For example, a single byte codepage like 'Latin-3' has a 
    minimum char size of 1. (It takes at least 1 byte to represent
    each Unicode char.) So the unicode buffer has the same number of
    UChars as the input buffer has bytes.

     In a strictly double byte codepage such as cp1362 (Windows
    Korean), the minimum char size is 2. So, only half as many Unicode
    chars as bytes are needed.

     This work to calculate the buffer size is an optimization. Any
    size of input and output buffer can be used, as long as the
    program handles the following cases: If the input buffer is empty,
    the source pointer will be equal to sourceLimit.  If the output
    buffer has overflowed, U_BUFFER_OVERFLOW_ERROR will be returned. 
 */

UErrorCode convsample_05()
{
  printf("\n\n==============================================\n"
         "Sample 05: C: count the number of letters in a UTF-8 document\n");

  FILE *f;
  int32_t count;
  char inBuf[BUFFERSIZE];
  const char *source;
  const char *sourceLimit;
  UChar *uBuf;
  UChar *target;
  UChar *targetLimit;
  UChar *p;
  int32_t uBufSize = 0;
  UConverter *conv;
  UErrorCode status = U_ZERO_ERROR;
  uint32_t letters=0, total=0;

  f = fopen("data01.ut8", "r");
  if(!f)
  {
    fprintf(stderr, "Couldn't open file 'data01.ut8' (UTF-8 data file).\n");
    return U_FILE_ACCESS_ERROR;
  }

  // **************************** START SAMPLE *******************
  conv = ucnv_open("utf-8", &status);
  assert(U_SUCCESS(status));

  uBufSize = (BUFFERSIZE/ucnv_getMinCharSize(conv));
  printf("input bytes %d / min chars %d = %d UChars\n",
         BUFFERSIZE, ucnv_getMinCharSize(conv), uBufSize);
  uBuf = (UChar*)malloc(uBufSize * sizeof(UChar));
  assert(uBuf!=NULL);

  // grab another buffer's worth
  while((!feof(f)) && 
        ((count=fread(inBuf, 1, BUFFERSIZE , f)) > 0) )
  {
    // Convert bytes to unicode
    source = inBuf;
    sourceLimit = inBuf + count;
    
    do
    {
        target = uBuf;
        targetLimit = uBuf + uBufSize;
        
        ucnv_toUnicode(conv, &target, targetLimit, 
                       &source, sourceLimit, NULL,
                       feof(f)?TRUE:FALSE,         /* pass 'flush' when eof */
                                   /* is true (when no more data will come) */
                       &status);
      
        if(status == U_BUFFER_OVERFLOW_ERROR)
        {
          // simply ran out of space - we'll reset the target ptr the next
          // time through the loop.
          status = U_ZERO_ERROR;
        }
        else
        {
          //  Check other errors here.
          assert(U_SUCCESS(status));
          // Break out of the loop (by force)
        }

        // Process the Unicode
        // Todo: handle UTF-16/surrogates

        for(p = uBuf; p<target; p++)
        {
          if(u_isalpha(*p))
            letters++;
          total++;
        }
    } while (source < sourceLimit); // while simply out of space
  }

  printf("%d letters out of %d total UChars.\n", letters, total);
  
  // ***************************** END SAMPLE ********************
  ucnv_close(conv);

  printf("\n");

  return U_ZERO_ERROR;
}
#undef BUFFERSIZE


/*******************************************************************
  Very simple C++ sample to convert a string into Unicode from SJIS

  This example creates a UnicodeString out of the chars.

 */
UErrorCode convsample_11()
{
  printf("\n\n==============================================\n"
         "Sample 11: C++: simple sjis -> Unicode conversion\n");


  // **************************** START SAMPLE *******************

  char source[] = { 0x63, 0x61, 0x74, 0x94, 0x4C, 0x82, 0x6E, 0x82, 0x6A, 0x00 };
  int32_t sourceSize = sizeof(source);
  UnicodeString target;
  UErrorCode status = U_ZERO_ERROR;

  // set up the converter
  UnicodeConverter conv("shift_jis", status);
  assert(U_SUCCESS(status));

  // convert from JIS
  conv.toUnicodeString(target, source, sourceSize, status);
  assert(U_SUCCESS(status));

  // ***************************** END SAMPLE ********************
  
  // Print it out
  printBytes("src", source, sourceSize);
  printf("\n");
  printString("targ", target );
  printf("\n");

  return U_ZERO_ERROR;
}


/******************************************************
  Similar sample to the preceding one. 
  You must call ucnv_close to clean up the memory used by the
  converter.

  'len' returns the number of OUTPUT bytes resulting from the 
  conversion.
 */

UErrorCode convsample_12()
{
  printf("\n\n==============================================\n"
         "Sample 12: C: simple sjis -> unicode conversion\n");


  // **************************** START SAMPLE *******************

  char source[] = { 0x63, 0x61, 0x74, 0x94, 0x4C, 0x82, 0x6E, 0x82, 0x6A, 0x00 };
  UChar target[100];
  UErrorCode status = U_ZERO_ERROR;
  UConverter *conv;
  int32_t     len;

  // set up the converter
  conv = ucnv_open("shift_jis", &status);
  assert(U_SUCCESS(status));

  // convert to Unicode
  // Note: we can use strlen, we know it's an 8 bit null terminated codepage
  target[6] = 0xFDCA;
  len = ucnv_toUChars(conv, target, 100, source, strlen(source), &status);
  U_ASSERT(status);
  // close the converter
  ucnv_close(conv);

  // ***************************** END SAMPLE ********************
  
  // Print it out
  printBytes("src", source, strlen(source) );
  printf("\n");
  printUChars("targ", target, len);

  return U_ZERO_ERROR;
}


/******************************************************************
   C: Convert from codepage to Unicode one at a time. 
*/
  
UErrorCode convsample_13()
{
  printf("\n\n==============================================\n"
         "Sample 13: C: simple Big5 -> unicode conversion, char at a time\n");


  const char sourceChars[] = { 0x7a, 0x68, 0x3d, 0xa4, 0xa4, 0xa4, 0xe5, 0x2e };
  //  const char sourceChars[] = { 0x7a, 0x68, 0x3d, 0xe4, 0xb8, 0xad, 0xe6, 0x96, 0x87, 0x2e };
  const char *source, *sourceLimit;
  UChar32 target;
  UErrorCode status = U_ZERO_ERROR;
  UConverter *conv = NULL;
  int32_t srcCount=0;
  int32_t dstCount=0;
  
  srcCount = sizeof(sourceChars);

  conv = ucnv_open("Big5", &status);
  U_ASSERT(status);

  source = sourceChars;
  sourceLimit = sourceChars + sizeof(sourceChars);

  // **************************** START SAMPLE *******************


  printBytes("src",source,sourceLimit-source);

  while(source < sourceLimit)
  {
    puts("");
    target = ucnv_getNextUChar (conv,
                                &source,
                                sourceLimit,
                                &status);
    
    //    printBytes("src",source,sourceLimit-source);
    U_ASSERT(status);
    printUChar(target);
    dstCount++;
  }
  
  
  // ************************** END SAMPLE *************************
  
  printf("src=%d bytes, dst=%d uchars\n", srcCount, dstCount);
  ucnv_close(conv);

  return U_ZERO_ERROR;
}




UBool convsample_20_didSubstitute(const char *source)
{
  UChar uchars[100];
  char bytes[100];
  UConverter *conv = NULL;
  UErrorCode status = U_ZERO_ERROR;
  uint32_t len, len2;
  
  FromUFLAGContext context;

  printf("\n\n==============================================\n"
         "Sample 20: C: Test for substitution using callbacks\n");

  /* print out the original source */
  printBytes("src", source);
  printf("\n");

  /* First, convert from UTF8 to unicode */
  conv = ucnv_open("utf-8", &status);
  U_ASSERT(status);

  len = ucnv_toUChars(conv, uchars, 100, source, strlen(source), &status);
  U_ASSERT(status);
 
  printUChars("uch", uchars, len);
  printf("\n");

  /* Now, close the converter */
  ucnv_close(conv);

  /* Now, convert to windows-1252 */
  conv = ucnv_open("windows-1252", &status);
  U_ASSERT(status);

  /* Converter starts out with the SUBSTITUTE callback set. */

  /* initialize our callback */
  context.subCallback = NULL;
  context.subContext  = NULL;
  context.flag        = FALSE;

  /* Set our special callback */
  ucnv_setFromUCallBack(conv,
                        UCNV_FROM_U_CALLBACK_FLAG,
                        &context,
                        &context.subCallback,
                        &context.subContext,
                        &status);
  U_ASSERT(status);

  len2 = ucnv_fromUChars(conv, bytes, 100, uchars, len, &status);
  U_ASSERT(status);

  ucnv_close(conv);

  /* print out the original source */
  printBytes("bytes", bytes, len2);

  return context.flag; /* true if callback was called */
}

UErrorCode convsample_20()
{
  const char *sample1 = "abc\xdf\xbf";
  const char *sample2 = "abc_def";


  if(convsample_20_didSubstitute(sample1))
  {
    printf("DID substitute.\n******\n");
  }
  else
  {
    printf("Did NOT substitute.\n*****\n");
  }

  if(convsample_20_didSubstitute(sample2))
  {
    printf("DID substitute.\n******\n");
  }
  else
  {
    printf("Did NOT substitute.\n*****\n");
  }

  return U_ZERO_ERROR;
}

//  40-  C, cp37 -> UTF16 [data02.bin -> data40.utf16]

#define BUFFERSIZE 17 /* make it interesting :) */

UErrorCode convsample_40()
{
  printf("\n\n==============================================\n"
    "Sample 40: C: convert data02.bin from cp37 to UTF16 [data40.utf16]\n");

  FILE *f;
  FILE *out;
  int32_t count;
  char inBuf[BUFFERSIZE];
  const char *source;
  const char *sourceLimit;
  UChar *uBuf;
  UChar *target;
  UChar *targetLimit;
  int32_t uBufSize = 0;
  UConverter *conv = NULL;
  UErrorCode status = U_ZERO_ERROR;
  uint32_t inbytes=0, total=0;

  f = fopen("data02.bin", "rb");
  if(!f)
  {
    fprintf(stderr, "Couldn't open file 'data02.bin' (cp37 data file).\n");
    return U_FILE_ACCESS_ERROR;
  }

  out = fopen("data40.utf16", "wb");
  if(!out)
  {
    fprintf(stderr, "Couldn't create file 'data40.utf16'.\n");
    return U_FILE_ACCESS_ERROR;
  }

  // **************************** START SAMPLE *******************
  conv = ucnv_openCCSID(37, UCNV_IBM, &status);
  assert(U_SUCCESS(status));

  uBufSize = (BUFFERSIZE/ucnv_getMinCharSize(conv));
  printf("input bytes %d / min chars %d = %d UChars\n",
         BUFFERSIZE, ucnv_getMinCharSize(conv), uBufSize);
  uBuf = (UChar*)malloc(uBufSize * sizeof(UChar));
  assert(uBuf!=NULL);

  // grab another buffer's worth
  while((!feof(f)) && 
        ((count=fread(inBuf, 1, BUFFERSIZE , f)) > 0) )
  {
    inbytes += count;

    // Convert bytes to unicode
    source = inBuf;
    sourceLimit = inBuf + count;
    
    do
    {
        target = uBuf;
        targetLimit = uBuf + uBufSize;
        
        ucnv_toUnicode( conv, &target, targetLimit, 
                       &source, sourceLimit, NULL,
                       feof(f)?TRUE:FALSE,         /* pass 'flush' when eof */
                                   /* is true (when no more data will come) */
                         &status);
      
        if(status == U_BUFFER_OVERFLOW_ERROR)
        {
          // simply ran out of space - we'll reset the target ptr the next
          // time through the loop.
          status = U_ZERO_ERROR;
        }
        else
        {
          //  Check other errors here.
          assert(U_SUCCESS(status));
          // Break out of the loop (by force)
        }

        // Process the Unicode
        // Todo: handle UTF-16/surrogates
        assert(fwrite(uBuf, sizeof(uBuf[0]), (target-uBuf), out) ==
               (size_t)(target-uBuf));
        total += (target-uBuf);
    } while (source < sourceLimit); // while simply out of space
  }

  printf("%d bytes in,  %d UChars out.\n", inbytes, total);
  
  // ***************************** END SAMPLE ********************
  ucnv_close(conv);

  fclose(f);
  fclose(out);
  printf("\n");

  return U_ZERO_ERROR;
}
#undef BUFFERSIZE

//        convsample_41();  // C++, cp37 -> UTF16 [data02.bin -> data41.utf16]

#define BUFFERSIZE 17 /* make it interesting :) */

UErrorCode convsample_41()
{
  printf("\n\n==============================================\n"
         "Sample 41: C++: convert data02.bin from cp37 to UTF16 [data41.utf16]\n");

  FILE *f;
  FILE *out;
  int32_t count;
  char inBuf[BUFFERSIZE];
  const char *source;
  const char *sourceLimit;
  UChar *uBuf;
  UChar *target;
  UChar *targetLimit;
  int32_t uBufSize = 0;
  UnicodeConverter *conv;
  UErrorCode status = U_ZERO_ERROR;
  uint32_t inbytes=0, total=0;

  f = fopen("data02.bin", "rb");
  if(!f)
  {
    fprintf(stderr, "Couldn't open file 'data02.bin' (cp37 data file).\n");
    return U_FILE_ACCESS_ERROR;
  }

  out = fopen("data41.utf16", "wb");
  if(!out)
  {
    fprintf(stderr, "Couldn't create file 'data41.utf16'.\n");
    return U_FILE_ACCESS_ERROR;
  }

  // **************************** START SAMPLE *******************
  conv = new UnicodeConverter(37, UCNV_IBM, status);
  assert(U_SUCCESS(status));

  uBufSize = (BUFFERSIZE/conv->getMinBytesPerChar());
  printf("input bytes %d / min chars %d = %d UChars\n",
         BUFFERSIZE, conv->getMinBytesPerChar(), uBufSize);
  uBuf = (UChar*)malloc(uBufSize * sizeof(UChar));
  assert(uBuf!=NULL);

  // grab another buffer's worth
  while((!feof(f)) && 
        ((count=fread(inBuf, 1, BUFFERSIZE , f)) > 0) )
  {
    inbytes += count;

    // Convert bytes to unicode
    source = inBuf;
    sourceLimit = inBuf + count;
    
    do
    {
        target = uBuf;
        targetLimit = uBuf + uBufSize;
        
        conv->toUnicode( target, targetLimit, 
                       source, sourceLimit, NULL,
                       feof(f)?TRUE:FALSE,         /* pass 'flush' when eof */
                                   /* is true (when no more data will come) */
                         status);
      
        if(status == U_BUFFER_OVERFLOW_ERROR)
        {
          // simply ran out of space - we'll reset the target ptr the next
          // time through the loop.
          status = U_ZERO_ERROR;
        }
        else
        {
          //  Check other errors here.
          assert(U_SUCCESS(status));
          // Break out of the loop (by force)
        }

        // Process the Unicode
        // Todo: handle UTF-16/surrogates
        assert(fwrite(uBuf, sizeof(uBuf[0]), (target-uBuf), out) ==
               (size_t)(target-uBuf));
        total += (target-uBuf);
    } while (source < sourceLimit); // while simply out of space
  }

  printf("%d bytes in,  %d UChars out.\n", inbytes, total);
  
  // ***************************** END SAMPLE ********************
  delete conv;

  fclose(f);
  fclose(out);
  printf("\n");

  return U_ZERO_ERROR;
}
#undef BUFFERSIZE



//  46-  C, UTF16 -> latin2 [data41.utf16 -> data46.out]

#define BUFFERSIZE 23 /* make it interesting :) */

UErrorCode convsample_46()
{
  printf("\n\n==============================================\n"
    "Sample 46: C: convert data41.utf16 from UTF16 to latin2 [data46.out]\n");

  FILE *f;
  FILE *out;
  int32_t count;
  UChar inBuf[BUFFERSIZE];
  const UChar *source;
  const UChar *sourceLimit;
  char *buf;
  char *target;
  char *targetLimit;

  int32_t bufSize = 0;
  UConverter *conv = NULL;
  UErrorCode status = U_ZERO_ERROR;
  uint32_t inchars=0, total=0;

  f = fopen("data41.utf16", "rb");
  if(!f)
  {
    fprintf(stderr, "Couldn't open file 'data41.utf16' (did you run convsample_41() ?)\n");
    return U_FILE_ACCESS_ERROR;
  }

  out = fopen("data46.out", "wb");
  if(!out)
  {
    fprintf(stderr, "Couldn't create file 'data46.out'.\n");
    return U_FILE_ACCESS_ERROR;
  }

  // **************************** START SAMPLE *******************
  conv = ucnv_open( "iso-8859-2", &status);
  assert(U_SUCCESS(status));

  bufSize = (BUFFERSIZE*ucnv_getMaxCharSize(conv));
  printf("input UChars[16] %d * max charsize %d = %d bytes output buffer\n",
         BUFFERSIZE, ucnv_getMaxCharSize(conv), bufSize);
  buf = (char*)malloc(bufSize * sizeof(char));
  assert(buf!=NULL);

  // grab another buffer's worth
  while((!feof(f)) && 
        ((count=fread(inBuf, sizeof(UChar), BUFFERSIZE , f)) > 0) )
  {
    inchars += count;

    // Convert bytes to unicode
    source = inBuf;
    sourceLimit = inBuf + count;
    
    do
    {
        target = buf;
        targetLimit = buf + bufSize;
        
        ucnv_fromUnicode( conv, &target, targetLimit, 
                       &source, sourceLimit, NULL,
                       feof(f)?TRUE:FALSE,         /* pass 'flush' when eof */
                                   /* is true (when no more data will come) */
                         &status);
      
        if(status == U_BUFFER_OVERFLOW_ERROR)
        {
          // simply ran out of space - we'll reset the target ptr the next
          // time through the loop.
          status = U_ZERO_ERROR;
        }
        else
        {
          //  Check other errors here.
          assert(U_SUCCESS(status));
          // Break out of the loop (by force)
        }

        // Process the Unicode
        assert(fwrite(buf, sizeof(buf[0]), (target-buf), out) ==
               (size_t)(target-buf));
        total += (target-buf);
    } while (source < sourceLimit); // while simply out of space
  }

  printf("%d Uchars (%d bytes) in, %d chars out.\n", inchars, inchars * sizeof(UChar), total);
  
  // ***************************** END SAMPLE ********************
  ucnv_close(conv);

  fclose(f);
  fclose(out);
  printf("\n");

  return U_ZERO_ERROR;
}
#undef BUFFERSIZE

#define BUFFERSIZE 219

UErrorCode convsample_47()
{
  printf("\n\n==============================================\n"
         "Sample 47: C++: convert data40.utf16 from UTF16 to latin2 [data47.out]\n");

  FILE *f;
  FILE *out;
  int32_t count;
  UChar inBuf[BUFFERSIZE];
  const UChar *source;
  const UChar *sourceLimit;
  char *buf;
  char *target;
  char *targetLimit;

  int32_t bufSize = 0;
  UnicodeConverter *conv = NULL;
  UErrorCode status = U_ZERO_ERROR;
  uint32_t inchars=0, total=0;

  f = fopen("data40.utf16", "rb");
  if(!f)
  {
    fprintf(stderr, "Couldn't open file 'data40.utf16' (Did you run convsample_40() ?)\n");
    return U_FILE_ACCESS_ERROR;
  }

  out = fopen("data47.out", "wb");
  if(!out)
  {
    fprintf(stderr, "Couldn't create file 'data47.out'.\n");
    return U_FILE_ACCESS_ERROR;
  }


  // **************************** START SAMPLE *******************
  conv = new UnicodeConverter( "iso-8859-2", status);
  assert(U_SUCCESS(status));

  bufSize = (BUFFERSIZE*conv->getMaxBytesPerChar());
  printf("input UChars[16] %d * max charsize %d = %d bytes output buffer\n",
         BUFFERSIZE, conv->getMaxBytesPerChar(), bufSize);
  buf = (char*)malloc(bufSize * sizeof(char));
  assert(buf!=NULL);

  // grab another buffer's worth
  while((!feof(f)) && 
        ((count=fread(inBuf, sizeof(UChar), BUFFERSIZE , f)) > 0) )
  {
    inchars += count;

    // Convert bytes to unicode
    source = inBuf;
    sourceLimit = inBuf + count;
    
    do
    {
        target = buf;
        targetLimit = buf + bufSize;
        
        conv->fromUnicode( target, targetLimit, 
                source, sourceLimit, NULL,
                           feof(f)?TRUE:FALSE,         /* pass 'flush' when eof */
                           /* is true (when no more data will come) */
                           status);
        
        if(status == U_BUFFER_OVERFLOW_ERROR)
        {
          // simply ran out of space - we'll reset the target ptr the next
          // time through the loop.
          status = U_ZERO_ERROR;
        }
        else
        {
          //  Check other errors here.
          assert(U_SUCCESS(status));
          // Break out of the loop (by force)
        }

        // Process the Unicode
        assert(fwrite(buf, sizeof(buf[0]), (target-buf), out) ==
               (size_t)(target-buf));
        total += (target-buf);
    } while (source < sourceLimit); // while simply out of space
  }

  printf("%d Uchars (%d bytes) in, %d chars out.\n", inchars, inchars * sizeof(UChar), total);
  
  // ***************************** END SAMPLE ********************
  delete conv;

  fclose(f);
  fclose(out);
  printf("\n");

  return U_ZERO_ERROR;
}
#undef BUFFERSIZE


/* main */

int main()
{

  printf("Default Converter=%s\n", ucnv_getDefaultName() );
  
  convsample_01();  // C++, u->koi8r, conv
  convsample_02();  // C  , u->koi8r, conv
  convsample_03();  // C,   iterate
  //    //  convsample_04();  /* not written yet */
  convsample_05();  // C,  utf8->u, getNextUChar
  convsample_11();  // C++, sjis->u, conv
  convsample_12();  // C,  sjis->u, conv
  convsample_13();  // C,  big5->u, getNextU
  
  convsample_20();  // C, callback
  
  convsample_40();  // C,   cp37 -> UTF16 [data02.bin -> data40.utf16]
  convsample_41();  // C++, cp37 -> UTF16 [data02.bin -> data41.utf16]
  
  convsample_46();  // C,  UTF16 -> latin3 [data41.utf16 -> data46.out]
  convsample_47();  // C++,UTF16 -> latin3 [data40.utf16 -> data47.out]
        
   return 0;
}
