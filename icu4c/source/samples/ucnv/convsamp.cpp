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
    char buf[1000];
    UErrorCode status = U_ZERO_ERROR;
    UTextOffset o;
    
    o = u_charName(c, U_UNICODE_CHAR_NAME, buf, 1000, &status);
    if(U_SUCCESS(status) && (o>0) ) {
      buf[6] = 0;
      printf("%- 7s", buf);
    } else {
      o = u_charName(c, U_UNICODE_10_CHAR_NAME, buf, 1000, &status);
      if(U_SUCCESS(status) && (o>0)) {
        buf[5] = 0;
        printf("~%- 6s", buf);
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



/*******************************************************************
  Very simple C++ sample to convert a string into Unicode from SJIS

  This example creates a UnicodeString out of the chars.

 */
UErrorCode convsample_11()
{
  printf("\n\n==============================================\n"
         "Sample 01: C++: simple Unicode -> koi8-r conversion\n");


  // **************************** START SAMPLE *******************

  char source[] = { 0x63, 0x61, 0x74, 0x94, 0x4C, 0x82, 0x6E, 0x82, 0x6A, 0x00 };
  int32_t sourceSize = sizeof(source);
  UnicodeString target;
  UErrorCode status = U_ZERO_ERROR;

  // set up the converter
  UnicodeConverterCPP conv("shift_jis", status);
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
#if 0
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
#endif
  return U_ZERO_ERROR;
}

/* main */
const char *scriptName(UCharScript script)
{
  switch(script)
    {
case U_BASIC_LATIN: return ("BASIC_LATIN"); return;
case U_LATIN_1_SUPPLEMENT: return ("LATIN_1_SUPPLEMENT"); return;
case U_LATIN_EXTENDED_A: return ("LATIN_EXTENDED_A"); return;
case U_LATIN_EXTENDED_B: return ("LATIN_EXTENDED_B"); return;
case U_IPA_EXTENSIONS: return ("IPA_EXTENSIONS"); return;
case U_SPACING_MODIFIER_LETTERS: return ("SPACING_MODIFIER_LETTERS"); return;
case U_COMBINING_DIACRITICAL_MARKS: return ("COMBINING_DIACRITICAL_MARKS"); return;
case U_GREEK: return ("GREEK"); return;
case U_CYRILLIC: return ("CYRILLIC"); return;
case U_ARMENIAN: return ("ARMENIAN"); return;
case U_HEBREW: return ("HEBREW"); return;
case U_ARABIC: return ("ARABIC"); return;
case U_SYRIAC: return ("SYRIAC"); return;
case U_THAANA: return ("THAANA"); return;
case U_DEVANAGARI: return ("DEVANAGARI"); return;
case U_BENGALI: return ("BENGALI"); return;
case U_GURMUKHI: return ("GURMUKHI"); return;
case U_GUJARATI: return ("GUJARATI"); return;
case U_ORIYA: return ("ORIYA"); return;
case U_TAMIL: return ("TAMIL"); return;
case U_TELUGU: return ("TELUGU"); return;
case U_KANNADA: return ("KANNADA"); return;
case U_MALAYALAM: return ("MALAYALAM"); return;
case U_SINHALA: return ("SINHALA"); return;
case U_THAI: return ("THAI"); return;
case U_LAO: return ("LAO"); return;
case U_TIBETAN: return ("TIBETAN"); return;
case U_MYANMAR: return ("MYANMAR"); return;
case U_GEORGIAN: return ("GEORGIAN"); return;
case U_HANGUL_JAMO: return ("HANGUL_JAMO"); return;
case U_ETHIOPIC: return ("ETHIOPIC"); return;
case U_CHEROKEE: return ("CHEROKEE"); return;
case U_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS: return ("UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS"); return;
case U_OGHAM: return ("OGHAM"); return;
case U_RUNIC: return ("RUNIC"); return;
case U_KHMER: return ("KHMER"); return;
case U_MONGOLIAN: return ("MONGOLIAN"); return;
case U_LATIN_EXTENDED_ADDITIONAL: return ("LATIN_EXTENDED_ADDITIONAL"); return;
case U_GREEK_EXTENDED: return ("GREEK_EXTENDED"); return;
case U_GENERAL_PUNCTUATION: return ("GENERAL_PUNCTUATION"); return;
case U_SUPERSCRIPTS_AND_SUBSCRIPTS: return ("SUPERSCRIPTS_AND_SUBSCRIPTS"); return;
case U_CURRENCY_SYMBOLS: return ("CURRENCY_SYMBOLS"); return;
case U_COMBINING_MARKS_FOR_SYMBOLS: return ("COMBINING_MARKS_FOR_SYMBOLS"); return;
case U_LETTERLIKE_SYMBOLS: return ("LETTERLIKE_SYMBOLS"); return;
case U_NUMBER_FORMS: return ("NUMBER_FORMS"); return;
case U_ARROWS: return ("ARROWS"); return;
case U_MATHEMATICAL_OPERATORS: return ("MATHEMATICAL_OPERATORS"); return;
case U_MISCELLANEOUS_TECHNICAL: return ("MISCELLANEOUS_TECHNICAL"); return;
case U_CONTROL_PICTURES: return ("CONTROL_PICTURES"); return;
case U_OPTICAL_CHARACTER_RECOGNITION: return ("OPTICAL_CHARACTER_RECOGNITION"); return;
case U_ENCLOSED_ALPHANUMERICS: return ("ENCLOSED_ALPHANUMERICS"); return;
case U_BOX_DRAWING: return ("BOX_DRAWING"); return;
case U_BLOCK_ELEMENTS: return ("BLOCK_ELEMENTS"); return;
case U_GEOMETRIC_SHAPES: return ("GEOMETRIC_SHAPES"); return;
case U_MISCELLANEOUS_SYMBOLS: return ("MISCELLANEOUS_SYMBOLS"); return;
case U_DINGBATS: return ("DINGBATS"); return;
case U_BRAILLE_PATTERNS: return ("BRAILLE_PATTERNS"); return;
case U_CJK_RADICALS_SUPPLEMENT: return ("CJK_RADICALS_SUPPLEMENT"); return;
case U_KANGXI_RADICALS: return ("KANGXI_RADICALS"); return;
case U_IDEOGRAPHIC_DESCRIPTION_CHARACTERS: return ("IDEOGRAPHIC_DESCRIPTION_CHARACTERS"); return;
case U_CJK_SYMBOLS_AND_PUNCTUATION: return ("CJK_SYMBOLS_AND_PUNCTUATION"); return;
case U_HIRAGANA: return ("HIRAGANA"); return;
case U_KATAKANA: return ("KATAKANA"); return;
case U_BOPOMOFO: return ("BOPOMOFO"); return;
case U_HANGUL_COMPATIBILITY_JAMO: return ("HANGUL_COMPATIBILITY_JAMO"); return;
case U_KANBUN: return ("KANBUN"); return;
case U_BOPOMOFO_EXTENDED: return ("BOPOMOFO_EXTENDED"); return;
case U_ENCLOSED_CJK_LETTERS_AND_MONTHS: return ("ENCLOSED_CJK_LETTERS_AND_MONTHS"); return;
case U_CJK_COMPATIBILITY: return ("CJK_COMPATIBILITY"); return;
case U_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A: return ("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A"); return;
case U_CJK_UNIFIED_IDEOGRAPHS: return ("CJK_UNIFIED_IDEOGRAPHS"); return;
case U_YI_SYLLABLES: return ("YI_SYLLABLES"); return;
case U_YI_RADICALS: return ("YI_RADICALS"); return;
case U_HANGUL_SYLLABLES: return ("HANGUL_SYLLABLES"); return;
case U_HIGH_SURROGATES: return ("HIGH_SURROGATES"); return;
case U_HIGH_PRIVATE_USE_SURROGATES: return ("HIGH_PRIVATE_USE_SURROGATES"); return;
case U_LOW_SURROGATES: return ("LOW_SURROGATES"); return;
case U_PRIVATE_USE_AREA /* PRIVATE_USE */: return ("PRIVATE_USE_AREA /* PRIVATE_USE */"); return;
case U_CJK_COMPATIBILITY_IDEOGRAPHS: return ("CJK_COMPATIBILITY_IDEOGRAPHS"); return;
case U_ALPHABETIC_PRESENTATION_FORMS: return ("ALPHABETIC_PRESENTATION_FORMS"); return;
case U_ARABIC_PRESENTATION_FORMS_A: return ("ARABIC_PRESENTATION_FORMS_A"); return;
case U_COMBINING_HALF_MARKS: return ("COMBINING_HALF_MARKS"); return;
case U_CJK_COMPATIBILITY_FORMS: return ("CJK_COMPATIBILITY_FORMS"); return;
case U_SMALL_FORM_VARIANTS: return ("SMALL_FORM_VARIANTS"); return;
case U_ARABIC_PRESENTATION_FORMS_B: return ("ARABIC_PRESENTATION_FORMS_B"); return;
case U_SPECIALS: return ("SPECIALS"); return;
case U_HALFWIDTH_AND_FULLWIDTH_FORMS: return ("HALFWIDTH_AND_FULLWIDTH_FORMS"); return;
  /*case U_CHAR_SCRIPT_COUNT: return ("SCRIPT_COUNT"); return; */
case U_NO_SCRIPT: return ("NO_SCRIPT"); return;

    default: return("Unknown??????");
    }
}

void dumpTest()
{
    char buf[1000];
    UErrorCode status = U_ZERO_ERROR;
    UTextOffset o;
    int32_t n;
    UChar c;
    FILE *q;

    q = fopen("nt.txt", "w");

    for(n=0;n<0x10000;n++)
      {
        c = (UChar)n;
        status = U_ZERO_ERROR;
        o = u_charName(c, U_UNICODE_10_CHAR_NAME, buf, 1000, &status);
        if(U_SUCCESS(status) && (o>0) ) {
          fprintf(q, "%06X:% 8s:%s\n", n, scriptName(u_charScript(c)), buf);
        }
        else {
          fprintf(q, "%06X:% 8s:_______________\n", n, scriptName(u_charScript(c)));
        }
      }
    fclose(q);
}

int main()
{
  //  dumpTest();
  //  return 0;

  printf("DEFCONV=%s, DEVLOC=%s\n", ucnv_getDefaultName(), uloc_getDefault());
  
  convsample_01();
  convsample_02();
  convsample_03();
//convsample_04();  /* not written yet */
  convsample_05();
  convsample_11();
  convsample_12();
}
