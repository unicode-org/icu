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
*/

#include <stdio.h>
#include <ctype.h>            /* for isspace, etc.    */
#include <assert.h>

#include "unicode/utypes.h"   /* Basic ICU data types */
#include "unicode/ucnv.h"     /* C   Converter API    */
#include "unicode/convert.h"  /* C++ Converter API    */
#include "unicode/ustring.h"  /* some more string fcns*/

/* Some utility functions */

static const UChar kNone[] = { 0x0000 };

/* Print a UChar if possible, in seven characters. */
void prettyPrintUChar(UChar c)
{
  if(  (c <= 0x007F) &&
       (isgraph(c))  ) {
    printf("  '%c'  ", (char)(0x00FF&c));
  } else if ( c > 0x007F ) {
    char buf[100];
    UErrorCode status = U_ZERO_ERROR;
    UTextOffset o;
    
    o = u_charName(c, U_UNICODE_CHAR_NAME, buf, 100, &status);
    if(U_SUCCESS(status)) {
      buf[6] = 0;
      printf("%- 7s", buf);
    } else {
      printf("??????? ");
    }
  } else {
    switch((char)(c & 0x007F)) {
    case ' ':
      printf("  ' '  ");
      break;
    case '\t':
      printf("  \t   ");
      break;
    case '\n':
      printf("  \n   ");
      break;
    default:
      printf("       ");
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

  printf("% 5s:", name);
  for( i = 0; i <len; i++) {
    printf("%- 6d ", i);
  }
  printf("\n");

  printf("% 5s: ", "uni");
  for( i = 0; i <len; i++) {
    printf("\\u%04X ", (int)uch[i]);
  }
  printf("\n");

  printf("% 5s: ", "ch");
  for( i = 0; i <len; i++) {
    prettyPrintUChar(uch[i]);
  }
  printf("\n");
}

void printBytes(const char  *name = "?", 
                 const char *uch  = "",
                 int32_t     len   = -1 )
{
  int32_t i;

  if( (len == -1) && (uch) ) {
    len = strlen(uch);
  }

  printf("% 5s:", name);
  for( i = 0; i <len; i++) {
    printf(" %- 4d", i);
  }
  printf("\n");

  printf("% 5s: ", "uni");
  for( i = 0; i <len; i++) {
    printf("\\x%02X ", 0x00FF & (int)uch[i]);
  }
  printf("\n");

  printf("% 5s: ", "ch");
  for( i = 0; i <len; i++) {
    if(isgraph(uch[i])) {
      printf(" '%c' ", (char)uch[i]);
    } else {
      printf("     ");
    }
  }
  printf("\n");
}


/*******************************************************************
  Very simple C++ sample to convert the word 'Moscow' in Russian, followed
  by an exclamation mark (!) into the KOI8-R Russian code page.

  This example first creates a UnicodeString out of the Unicode chars.

  targetSize must be set to the amount of space available in the target
  buffer. After UnicodeConverterCPP::fromUnicodeString() is called, 
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
  UnicodeConverterCPP conv("koi8-r", status);
  assert(U_SUCCESS(status));

  // convert to KOI8-R
  conv.fromUnicodeString(target, targetSize, myString, status);
  assert(U_SUCCESS(status));

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
  conversion. In this case, it will be 9 even though there are
  only 6 unicode characters going in. This is because the 
  letters 'cat' each only take up one byte, but the remaining
  three UChars each take up 2 bytes in the output codepage.

  src: 0      1      2      3      4      5     
  uni: \u0063 \u0061 \u0074 \u732B \uFF2F \uFF2B 
   ch:   'c'    'a'    't'  CJK UN               

 targ:  0    1    2    3    4    5    6    7    8  
  uni: \x63 \x61 \x74 \x94 \x4C \x82 \x6E \x82 \x6A 
   ch:  'c'  'a'  't'   [not readable here........]

 */

UErrorCode convsample_02()
{
  printf("\n\n==============================================\n"
         "Sample 02: C: simple Unicode -> Shift_Jis conversion\n");


  // **************************** START SAMPLE *******************
  // "cat<cat>OK"
  UChar source[] = {   0x0063, 0x0061, 0x0074, 0x732B, 0xFF2F, 0xFF2B,
                     0x0000 };
  char target[100];
  UErrorCode status = U_ZERO_ERROR;
  UConverter *conv;
  int32_t     len;

  // set up the converter
  conv = ucnv_open("shift_jis", &status);
  assert(U_SUCCESS(status));

  // convert to shift-jis
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
    buffer is empty, U_INDEX_OUTOFBOUNDS_ERROR will be returned. 
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
      
        if(status != U_INDEX_OUTOFBOUNDS_ERROR)
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

/* main */

int main()
{
  convsample_01();
  convsample_02();
  convsample_03();
//convsample_04();  /* not written yet */
  convsample_05();
}
