/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  compsamp.c
*   encoding:   ASCII (7-bit)
*
*   created on: 2000may30
*   created by: Steven R. Loomis
*
*   Sample code for the ICU compression routines.
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
#include <string.h>

#include "unicode/utypes.h"   /* Basic ICU data types */
#include "unicode/scsu.h"     
#include "unicode/uchar.h"
#include "unicode/ustring.h"  /* some more string fcns*/
#include "unicode/uloc.h"

/* Some utility functions */

static const UChar kNone[] = { 0x0000 };

/* Print a UChar if possible, in seven characters. */
static void prettyPrintUChar(UChar c)
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


static void printUChars(const char  *name = "?", 
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

static void printBytes(const char  *name = "?", 
                 const uint8_t *uch  = (const uint8_t*)"",
                 int32_t     len   = -1 )
{
  int32_t i;

  if( (len == -1) && (uch) ) {
    len = strlen((const char*)uch);
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
  Very simple C sample to compress the word 'Moscow' in Russian, followed
  by an exclamation mark (!)

 */
UErrorCode compsample_01()
{
  printf("\n\n==============================================\n"
         "Sample 01: C: simple Unicode compression\n");


  // "Moscva!" in cyrillic letters, to be converted to the KOI8-R
  // Russian code page.
  UChar input[] = { 0x041C, 0x043E, 0x0441, 0x043A, 0x0432,
                     0x0430, 0x0021, 0x0000 };
  const UChar *source;
  uint8_t buffer[100];
  uint8_t *target; 
  int32_t targetLen = sizeof(buffer);
  int32_t sourceLen = u_strlen(input);
  target = buffer;
  source = input;
  
  UErrorCode status = U_ZERO_ERROR;

  // **************************** START SAMPLE *******************
  // set up the compressor
  UnicodeCompressor comp;
  scsu_init(&comp);

  target = buffer;
  scsu_compress(&comp, &target, target+targetLen, &source,
                source+sourceLen, &status);
  assert(U_SUCCESS(status));

  fprintf(stderr, "Bytes converted: %d\n", target-buffer);

  // ***************************** END SAMPLE ********************
  
  // Print it out
  printUChars("src", input);
  printf("\n");
  printBytes("targ", buffer, target - buffer);

  return U_ZERO_ERROR;
}

int32_t countOurDataStrings()
{
  return uloc_countAvailable();
}

int32_t getOurDataString(int32_t i, UChar *s, int32_t size)
{
  UErrorCode status = U_ZERO_ERROR;
  int32_t len;
  const char *loc = uloc_getAvailable(i);
  len =  uloc_getDisplayName(loc,loc,s,size,&status);
  if(U_FAILURE(status))
    {
      return 0;
    }
  return len;
}

void compsample_02()
{
  printf("\n\n==============================================\n"
         "Sample 02: C: bulk Unicode compression\n");

#define SAMPLE2BUFFERSIZE 1024  /* larger than our largest data */
  UChar input[SAMPLE2BUFFERSIZE];
  const UChar *source;
  uint8_t output[SAMPLE2BUFFERSIZE];
  uint8_t *target; 
  int32_t sourceLen;
  int32_t count;
  int32_t i;
  int32_t charsIn = 0,bytesOut = 0;
  FILE *f;

  UErrorCode status = U_ZERO_ERROR;

  f = fopen("outdata2.scsu", "w");

  // **************************** START SAMPLE *******************
  // set up the compressor
  UnicodeCompressor comp;
  scsu_init(&comp);

  count = countOurDataStrings();
  for(i=0;i<count;i++)
    {
      sourceLen = getOurDataString(i, input, SAMPLE2BUFFERSIZE);
      charsIn += sourceLen;
      //      printUChars("src", input);

      /* Now, loop and write out all of the data */
      source = input;
      target = output;
      
      while(source < (input+sourceLen))
        {
          scsu_compress(&comp, &target, output+SAMPLE2BUFFERSIZE,
                        &source, input+sourceLen, &status);
          
          if( (status == U_ZERO_ERROR) || (status == U_BUFFER_OVERFLOW_ERROR)) {
            /* got all of it */
//            printBytes("out", output, target-output); // Uncomment for very verbose output..
            
            fwrite(output, 1, target-output, f);
            bytesOut += (target-output);

            target = output; /* reset target to beginning */
            if(status == U_ZERO_ERROR) {
              break; /* Got everything! */
            }
            
            status = U_ZERO_ERROR; /* reset, go get another chunk. */
          }
        }
    }
  
  fclose(f);
  printf("done[02] - %d uchars in, %d bytes written. \n", charsIn, bytesOut);
  /* at this point, call scsu_reset(&comp) if you want ot write out
     a different data stream with the same compressor. */

  /************************* END SAMPLE ************************/
}

int main()
{
  compsample_01();
  compsample_02();

  return 0;
}
