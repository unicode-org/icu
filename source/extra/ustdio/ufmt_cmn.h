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
* File ufmt_cmn.h
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*   03/15/99    stephen     Added defaultCPToUnicode, unicodeToDefaultCP
*******************************************************************************
*/

#ifndef UFMT_CMN_H
#define UFMT_CMN_H

#include "utypes.h"

/** 
 * Enum representing the possible argument types for uprintf/uscanf
 */
enum
{
  ufmt_count,      /* special flag for count */
  ufmt_int,        /* int */
  ufmt_char,       /* int, cast to char */
  ufmt_wchar,      /* wchar_t */
  ufmt_string,     /* char* */
  ufmt_wstring,    /* wchar_t* */
  ufmt_pointer,    /* void* */
  ufmt_float,      /* float */
  ufmt_double,     /* double */
  ufmt_date,       /* Date */
  ufmt_uchar,      /* int, cast to UChar */
  ufmt_ustring,    /* UChar* */
  ufmt_last
};

/**
 * Union representing a uprintf/uscanf argument
 */
union ufmt_args {
  int     intValue;      /* int, UChar */
  float   floatValue;    /* float */
  double  doubleValue;   /* double */
  void    *ptrValue;     /* any pointer - void*, char*, wchar_t*, UChar* */
  wchar_t wcharValue;    /* wchar_t */
  UDate   dateValue;     /* Date */
};
typedef union ufmt_args ufmt_args;

/**
 * Macro for determining the minimum of two numbers.
 * @param a An integer
 * @param b An integer
 * @return <TT>a</TT> if </TT>a < b</TT>, <TT>b</TT> otherwise
 */
#define ufmt_min(a,b) (a) < (b) ? (a) : (b)

/**
 * Convert a UChar in a some radix to an integer value.
 * @param c The UChar to convert.
 * @return The integer value of <TT>c</TT>.
 */
int
ufmt_digitvalue(UChar c);

/**
 * Determine if a UChar is a digit for a specified radix.
 * @param c The UChar to check.
 * @param radix The desired radix.
 * @return TRUE if <TT>c</TT> is a digit in <TT>radix</TT>, FALSE otherwise.
 */
bool_t
ufmt_isdigit(UChar     c,
         int32_t     radix);

/**
 * Convert a long to a UChar* in a specified radix
 * @param buffer The target buffer
 * @param len On input, the size of <TT>buffer</TT>.  On output,
 * the number of UChars written to <TT>buffer</TT>.
 * @param value The value to be converted
 * @param radix The desired radix
 * @param uselower TRUE means lower case will be used, FALSE means upper case
 * @param minDigits The minimum number of digits for for the formatted number,
 * which will be padded with zeroes. -1 means do not pad.
 */
void 
ufmt_ltou(UChar     *buffer, 
      int32_t     *len,
      long         value, 
      int32_t     radix,
      bool_t    uselower,
      int32_t    minDigits);

/**
 * Convert a UChar* in a specified radix to a long.
 * @param buffer The target buffer
 * @param len On input, the size of <TT>buffer</TT>.  On output,
 * the number of UChars read from <TT>buffer</TT>.
 * @param radix The desired radix
 * @return The numeric value.
 */
long
ufmt_utol(const UChar     *buffer, 
      int32_t     *len,
      int32_t     radix);

/**
 * Determine if a UChar is a whitespace character.
 * @param c The UChar to test.
 * @return TRUE if the UChar is a space (U+0020), tab (U+0009), 
 * carriage-return (U+000D), newline (U+000A), vertical-tab (U+000B),
 * form-feed (U+000C), or any other Unicode-defined space, line, or paragraph
 * separator.
 */
bool_t
ufmt_isws(UChar c);

/**
 * Convert a string from the default codepage to Unicode.
 * @param s The string to convert, in the default codepage.
 * @param len The number of characters in s.
 * @return A pointer to a newly allocated converted version of s, or 0 
 * on error.
 */
UChar*
ufmt_defaultCPToUnicode(const char *s,
            int32_t len);


/**
 * Convert a string from the Unicode to the default codepage.
 * @param s The string to convert.
 * @param len The number of characters in s.
 * @return A pointer to a newly allocated converted version of s, or 0 
 * on error.
 */
char*
ufmt_unicodeToDefaultCP(const UChar *s,
            int32_t len);

#endif




