/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uprintf.c
*
* Modification History:
*
*   Date        Name        Description
*   11/19/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*                           Added conversion from default codepage.
*******************************************************************************
*/

#include "uprintf.h"
#include "uprntf_p.h"
#include "unicode/ustdio.h"
#include "ufile.h"
#include "unicode/ustring.h"
#include "locbund.h"
#include "umutex.h"
#include "unicode/unum.h"
#include "unicode/udat.h"

#include <string.h>
#include <stdlib.h>
#include <math.h>
#include <float.h>
#include <limits.h>
#include <wchar.h>


u_printf_handler     g_u_printf_handlers     [256];
u_printf_info       g_u_printf_infos     [256];
bool_t            g_u_printf_inited    = FALSE;

/* buffer size for formatting */
#define UFPRINTF_BUFFER_SIZE 1024

int32_t 
u_fprintf(    UFILE        *f,
        const char    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t count;
  
  va_start(ap, patternSpecification);
  count = u_vfprintf(f, patternSpecification, ap);
  va_end(ap);
  
  return count;
}

int32_t 
u_fprintf_u(    UFILE        *f,
        const UChar    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t count;
  
  va_start(ap, patternSpecification);
  count = u_vfprintf_u(f, patternSpecification, ap);
  va_end(ap);
  
  return count;
}

int32_t 
u_vfprintf(    UFILE        *f,
        const char    *patternSpecification,
        va_list        ap)
{
  int32_t count;
  UChar *pattern;
  
  /* convert from the default codepage to Unicode */
  pattern = ufmt_defaultCPToUnicode(patternSpecification, 
                    strlen(patternSpecification));
  if(pattern == 0) {
    return 0;
  }
  
  /* do the work */
  count = u_vfprintf_u(f, pattern, ap);

  /* clean up */
  free(pattern);
  
  return count;
}

int32_t
u_printf_register_handler(UChar            spec, 
              u_printf_info     info,
              u_printf_handler     handler)
{
  /* lock the cache */
  umtx_lock(0);

  /* add to our list of function pointers */
  g_u_printf_infos[ (unsigned char) spec ]     = info;
  g_u_printf_handlers[ (unsigned char) spec ]     = handler;

  /* unlock the cache */
  umtx_unlock(0);
  return 0;
}

/* handle a '%' */

int32_t 
u_printf_simple_percent_info(const u_printf_spec_info     *info,
                 int32_t             *argtypes,
                 int32_t             n)
{
  /* we don't need any arguments */
  return 0;
}

int32_t
u_printf_simple_percent_handler(UFILE                 *stream,
                const u_printf_spec_info     *info,
                const ufmt_args            *args)
{
  /* put a single '%' on the stream */
  u_fputc(0x0025, stream);
  /* we wrote one character */
  return 1;
}

/* handle 's' */

int32_t 
u_printf_string_info(const u_printf_spec_info     *info,
             int32_t             *argtypes,
             int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type string */
  argtypes[0] = ufmt_string;
  return 1;
}

int32_t
u_printf_string_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args)
{
  UChar *s;
  int32_t len, written, i;
  const char *arg = (const char*)(args[0].ptrValue);

  /* convert from the default codepage to Unicode */
  s = ufmt_defaultCPToUnicode(arg, strlen(arg));
  if(s == 0) {
    return 0;
  }
  len = u_strlen(s);
  
  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the string should be truncated */
  if(info->fPrecision != -1 && len > info->fPrecision) {
    written = u_file_write(s, info->fPrecision, stream);
  }
  
  /* determine if the string should be padded */
  else if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(s, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(s, len, stream);
    }
  }

  /* just write the string */
  else 
    written = u_file_write(s, len, stream);

  /* clean up */
  free(s);
  
  return written;
}

int32_t 
u_printf_integer_info(const u_printf_spec_info     *info,
              int32_t             *argtypes,
              int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type int */
  argtypes[0] = ufmt_int;
  return 1;
}
/* HSYS */
int32_t
u_printf_integer_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args)
{
  int32_t         written     = 0;
  int32_t         len;
  long            num         = (long) (args[0].intValue);
  UNumberFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i, minDigits     = -1;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  if(info->fIsShort)
    num &= SHRT_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    num &= INT_MAX;

  /* get the formatter */
  format = u_locbund_getNumberFormat(stream->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* set the minimum integer digits */
  if(info->fPrecision != -1) {
    /* clone the stream's bundle if it isn't owned */
    if(! stream->fOwnBundle) {
      stream->fBundle     = u_locbund_clone(stream->fBundle);
      stream->fOwnBundle = TRUE;
      format           = u_locbund_getNumberFormat(stream->fBundle);
    }

    /* set the minimum # of digits */
    minDigits = unum_getAttribute(format, UNUM_MIN_INTEGER_DIGITS);
    unum_setAttribute(format, UNUM_MIN_INTEGER_DIGITS, info->fPrecision);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* clone the stream's bundle if it isn't owned */
    if(! stream->fOwnBundle) {
      stream->fBundle     = u_locbund_clone(stream->fBundle);
      stream->fOwnBundle = TRUE;
      format           = u_locbund_getNumberFormat(stream->fBundle);
    }

    /* set whether to show the sign*/
    /* {sfb} TBD */
  }

  /* format the number */
  unum_format(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);
  
  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  /* restore the number format */
  if(minDigits != -1)
    unum_setAttribute(format, UNUM_MIN_INTEGER_DIGITS, minDigits);

  return written;
}

int32_t 
u_printf_hex_info(const u_printf_spec_info     *info,
          int32_t             *argtypes,
          int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type int */
  argtypes[0] = ufmt_int;
  return 1;
}

int32_t
u_printf_hex_handler(UFILE             *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args)
{
  int32_t         written     = 0;
  long            num         = (long) (args[0].intValue);
  int32_t        i;
  UChar            result         [UFPRINTF_BUFFER_SIZE];
  int32_t         len        = UFPRINTF_BUFFER_SIZE;


  /* mask off any necessary bits */
  if(info->fIsShort)
    num &= SHRT_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    num &= INT_MAX;

  /* format the number, preserving the minimum # of digits */
  ufmt_ltou(result, &len, num, 16,
        (bool_t)(info->fSpec == 0x0078),
        (info->fPrecision == -1 && info->fZero) ? info->fWidth : info->fPrecision);
  
  /* convert to alt form, if desired */
  if(num != 0 && info->fAlt && len < UFPRINTF_BUFFER_SIZE - 2) {
    /* shift the formatted string right by 2 chars */
    memmove(result + 2, result, len * sizeof(UChar));
    result[0] = 0x0030;
    result[1] = info->fSpec;
    len += 2;
  }

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  return written;
}

int32_t 
u_printf_octal_info(const u_printf_spec_info     *info,
            int32_t             *argtypes,
            int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type int */
  argtypes[0] = ufmt_int;
  return 1;
}

int32_t
u_printf_octal_handler(UFILE                 *stream,
               const u_printf_spec_info     *info,
               const ufmt_args            *args)
{
  int32_t         written     = 0;
  long            num         = (long) (args[0].intValue);
  int32_t        i;
  UChar            result         [UFPRINTF_BUFFER_SIZE];
  int32_t         len        = UFPRINTF_BUFFER_SIZE;


  /* mask off any necessary bits */
  if(info->fIsShort)
    num &= SHRT_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    num &= INT_MAX;

  /* format the number, preserving the minimum # of digits */
  ufmt_ltou(result, &len, num, 8,
        FALSE, /* doesn't matter for octal */
        info->fPrecision == -1 && info->fZero ? info->fWidth : info->fPrecision);

  /* convert to alt form, if desired */
  if(info->fAlt && result[0] != 0x0030 && len < UFPRINTF_BUFFER_SIZE - 1) {
    /* shift the formatted string right by 1 char */
    memmove(result + 1, result, len * sizeof(UChar));
    result[0] = 0x0030;
    len += 1;
  }

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  return written;
}


int32_t 
u_printf_double_info(const u_printf_spec_info     *info,
             int32_t             *argtypes,
             int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type double */
  argtypes[0] = ufmt_double;
  return 1;
}

int32_t
u_printf_double_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args)
{
  int32_t         written     = 0;
  int32_t         len;
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i, minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;

  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getNumberFormat(stream->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! stream->fOwnBundle) {
    stream->fBundle     = u_locbund_clone(stream->fBundle);
    stream->fOwnBundle     = TRUE;
    format           = u_locbund_getNumberFormat(stream->fBundle);
  }

  /* set the number of decimal digits */

  /* save the formatter's state */
  minDecimalDigits = unum_getAttribute(format, UNUM_MIN_FRACTION_DIGITS);
  maxDecimalDigits = unum_getAttribute(format, UNUM_MAX_FRACTION_DIGITS);

  if(info->fPrecision != -1) {
    /* set the # of decimal digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, info->fPrecision);
  }
  else if(info->fPrecision == 0 && ! info->fAlt) {
    /* no decimal point in this case */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 0);
  }
  else if(info->fAlt) {
    /* '#' means always show decimal point */
    /* copy of printf behavior on Solaris - '#' shows 6 digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }
  else {
    /* # of decimal digits is 6 if precision not specified */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* set whether to show the sign*/
    /* {sfb} TBD */
  }

  /* format the number */
  unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return written;
}


int32_t 
u_printf_char_info(const u_printf_spec_info     *info,
           int32_t             *argtypes,
           int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type char */
  argtypes[0] = ufmt_char;
  return 1;
}

int32_t
u_printf_char_handler(UFILE                 *stream,
              const u_printf_spec_info         *info,
              const ufmt_args              *args)
{
  UChar *s;
  int32_t len, written = 0, i;
  unsigned char arg = (unsigned char)(args[0].intValue);

  /* convert from default codepage to Unicode */
  s = ufmt_defaultCPToUnicode(&arg, 1);
  if(s == 0) {
    return 0;
  }
  len = u_strlen(s);
  
  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the string should be truncated */
  if(info->fPrecision != -1 && len > info->fPrecision) {
    written = u_file_write(s, info->fPrecision, stream);
  }
  
  /* determine if the string should be padded */
  else if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(s, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(s, len, stream);
    }
  }

  /* just write the string */
  else 
    written = u_file_write(s, len, stream);

  /* clean up */
  free(s);

  return written;
}


int32_t 
u_printf_pointer_info(const u_printf_spec_info     *info,
              int32_t             *argtypes,
              int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type void* */
  argtypes[0] = ufmt_pointer;
  return 1;
}

int32_t
u_printf_pointer_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args)
{
  int32_t         written     = 0;
  long            num         = (long) (args[0].intValue);
  int32_t        i;
  UChar            result         [UFPRINTF_BUFFER_SIZE];
  int32_t         len        = UFPRINTF_BUFFER_SIZE;


  /* format the pointer in hex */
  ufmt_ltou(result, &len, num, 16, TRUE, info->fPrecision);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  return written;
}


int32_t 
u_printf_scientific_info(const u_printf_spec_info     *info,
             int32_t             *argtypes,
             int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;
  
  /* we need 1 argument of type double */
  argtypes[0] = ufmt_double;
  return 1;
}

int32_t
u_printf_scientific_handler(UFILE             *stream,
                const u_printf_spec_info     *info,
                const ufmt_args            *args)
{
  int32_t         written     = 0;
  int32_t         len;
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i, minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;
  

  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getScientificFormat(stream->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! stream->fOwnBundle) {
    stream->fBundle     = u_locbund_clone(stream->fBundle);
    stream->fOwnBundle     = TRUE;
    format           = u_locbund_getScientificFormat(stream->fBundle);
  }

  /* set the number of decimal digits */

  /* save the formatter's state */
  minDecimalDigits = unum_getAttribute(format, UNUM_MIN_FRACTION_DIGITS);
  maxDecimalDigits = unum_getAttribute(format, UNUM_MAX_FRACTION_DIGITS);

  if(info->fPrecision != -1) {
    /* set the # of decimal digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, info->fPrecision);
  }
  else if(info->fPrecision == 0 && ! info->fAlt) {
    /* no decimal point in this case */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 0);
  }
  else if(info->fAlt) {
    /* '#' means always show decimal point */
    /* copy of printf behavior on Solaris - '#' shows 6 digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }
  else {
    /* # of decimal digits is 6 if precision not specified */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* set whether to show the sign*/
    /* {sfb} TBD */
  }

  /* format the number */
  unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return written;
}

int32_t 
u_printf_date_info(const u_printf_spec_info     *info,
           int32_t             *argtypes,
           int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;
  
  /* we need 1 argument of type Date */
  argtypes[0] = ufmt_date;
  return 1;
}

int32_t
u_printf_date_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args         *args)
{
  int32_t         written     = 0;
  int32_t         len;
  UDate            num         = (UDate) (args[0].dateValue);
  UDateFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i;
  UErrorCode        status        = U_ZERO_ERROR;


  /* get the formatter */
  format = u_locbund_getDateFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;

  /* format the date */
  udat_format(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  return written;
}

int32_t 
u_printf_time_info(const u_printf_spec_info     *info,
           int32_t             *argtypes,
           int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;
  
  /* we need 1 argument of type date */
  argtypes[0] = ufmt_date;
  return 1;
}

int32_t
u_printf_time_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args         *args)
{
  int32_t         written     = 0;
  int32_t         len;
  UDate            num         = (UDate) (args[0].dateValue);
  UDateFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i;
  UErrorCode        status        = U_ZERO_ERROR;


  /* get the formatter */
  format = u_locbund_getTimeFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;

  /* format the time */
  udat_format(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  return written;
}


int32_t 
u_printf_percent_info(const u_printf_spec_info     *info,
              int32_t             *argtypes,
              int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type double */
  argtypes[0] = ufmt_double;
  return 1;
}

int32_t
u_printf_percent_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args)
{
  int32_t         written     = 0;
  int32_t         len;
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i, minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getPercentFormat(stream->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! stream->fOwnBundle) {
    stream->fBundle     = u_locbund_clone(stream->fBundle);
    stream->fOwnBundle     = TRUE;
    format           = u_locbund_getPercentFormat(stream->fBundle);
  }

  /* set the number of decimal digits */

  /* save the formatter's state */
  minDecimalDigits = unum_getAttribute(format, UNUM_MIN_FRACTION_DIGITS);
  maxDecimalDigits = unum_getAttribute(format, UNUM_MAX_FRACTION_DIGITS);

  if(info->fPrecision != -1) {
    /* set the # of decimal digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, info->fPrecision);
  }
  else if(info->fPrecision == 0 && ! info->fAlt) {
    /* no decimal point in this case */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 0);
  }
  else if(info->fAlt) {
    /* '#' means always show decimal point */
    /* copy of printf behavior on Solaris - '#' shows 6 digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }
  else {
    /* # of decimal digits is 6 if precision not specified */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* set whether to show the sign*/
    /* {sfb} TBD */
  }

  /* format the number */
  unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return written;
}


int32_t 
u_printf_currency_info(const u_printf_spec_info     *info,
               int32_t                 *argtypes,
               int32_t                 n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type double */
  argtypes[0] = ufmt_double;
  return 1;
}

int32_t
u_printf_currency_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args            *args)
{
  int32_t         written     = 0;
  int32_t         len;
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i, minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getCurrencyFormat(stream->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! stream->fOwnBundle) {
    stream->fBundle     = u_locbund_clone(stream->fBundle);
    stream->fOwnBundle     = TRUE;
    format           = u_locbund_getCurrencyFormat(stream->fBundle);
  }

  /* set the number of decimal digits */

  /* save the formatter's state */
  minDecimalDigits = unum_getAttribute(format, UNUM_MIN_FRACTION_DIGITS);
  maxDecimalDigits = unum_getAttribute(format, UNUM_MAX_FRACTION_DIGITS);

  if(info->fPrecision != -1) {
    /* set the # of decimal digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, info->fPrecision);
  }
  else if(info->fPrecision == 0 && ! info->fAlt) {
    /* no decimal point in this case */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 0);
  }
  else if(info->fAlt) {
    /* '#' means always show decimal point */
    /* copy of printf behavior on Solaris - '#' shows 6 digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }
  else {
    /* # of decimal digits is 6 if precision not specified */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* set whether to show the sign*/
    /* {sfb} TBD */
  }

  /* format the number */
  unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return written;
}

int32_t 
u_printf_ustring_info(const u_printf_spec_info     *info,
              int32_t             *argtypes,
              int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type ustring */
  argtypes[0] = ufmt_ustring;
  return 1;
}

int32_t
u_printf_ustring_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args         *args)
{
  int32_t len, written, i;
  const UChar *arg = (const UChar*)(args[0].ptrValue);

  /* allocate enough space for the buffer */
  len = u_strlen(arg);

  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the string should be truncated */
  if(info->fPrecision != -1 && len > info->fPrecision) {
    written = u_file_write(arg, info->fPrecision, stream);
  }
  
  /* determine if the string should be padded */
  else if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(arg, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(arg, len, stream);
    }
  }

  /* just write the string */
  else 
    written = u_file_write(arg, len, stream);

  return written;
}



int32_t 
u_printf_uchar_info(const u_printf_spec_info     *info,
            int32_t             *argtypes,
            int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type uchar */
  argtypes[0] = ufmt_uchar;
  return 1;
}

int32_t
u_printf_uchar_handler(UFILE                 *stream,
               const u_printf_spec_info     *info,
               const ufmt_args            *args)
{
  int32_t written = 0, i;
  UChar arg = (UChar)(args[0].intValue);
  

  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the char should be printed */
  if(info->fPrecision != -1 && info->fPrecision < 1) {
    /* write nothing */
    written = 0;
  }
  
  /* determine if the character should be padded */
  else if(info->fWidth != -1 && info->fWidth > 1) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(&arg, 1, stream);
      for(i = 0; i < info->fWidth - 1; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - 1; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(&arg, 1, stream);
    }
  }

  /* just write the character */
  else 
    written = u_file_write(&arg, 1, stream);

  return written;
}

int32_t 
u_printf_scidbl_info(const u_printf_spec_info     *info,
             int32_t             *argtypes,
             int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type double */
  argtypes[0] = ufmt_double;
  return 1;
}

int32_t
u_printf_scidbl_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args)
{
  double     num = (double)(args[0].doubleValue);
  bool_t     useE;

  /* a precision of 0 is taken as 1 */
  if(info->fPrecision == 0)
    ((u_printf_spec_info*)info)->fPrecision = 1;

  /* determine whether to use 'e' or 'f' */
  useE = (num < 0.0001 || (info->fPrecision != -1 && num > pow(10.0, info->fPrecision)));
  
  /* use 'e' */
  if(useE) {
    /* adjust the specifier */
    ((u_printf_spec_info*)info)->fSpec = 0x0065;
    /* call the scientific handler */
    return u_printf_scientific_handler(stream, info, args);
  }
  /* use 'f' */
  else {
    /* adjust the specifier */
    ((u_printf_spec_info*)info)->fSpec = 0x0066;
    /* call the double handler */
    return u_printf_double_handler(stream, info, args);
  }
}


int32_t 
u_printf_count_info(const u_printf_spec_info     *info,
            int32_t             *argtypes,
            int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type count */
  argtypes[0] = ufmt_count;
  return 1;
}

int32_t
u_printf_count_handler(UFILE                 *stream,
               const u_printf_spec_info     *info,
               const ufmt_args            *args)
{
  int *count = (int*)(args[0].ptrValue);

  /* in the special case of count, the u_printf_spec_info's width */
  /* will contain the # of chars written thus far */
  *count = info->fWidth;

  return 0;
}


int32_t 
u_printf_spellout_info(const u_printf_spec_info *info,
               int32_t             *argtypes,
               int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type double */
  argtypes[0] = ufmt_double;
  return 1;
}

int32_t
u_printf_spellout_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args            *args)
{
  int32_t         written     = 0;
  int32_t         len;
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [UFPRINTF_BUFFER_SIZE];
  int32_t        i, minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getSpelloutFormat(stream->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! stream->fOwnBundle) {
    stream->fBundle     = u_locbund_clone(stream->fBundle);
    stream->fOwnBundle     = TRUE;
    format           = u_locbund_getSpelloutFormat(stream->fBundle);
  }

  /* set the number of decimal digits */

  /* save the formatter's state */
  minDecimalDigits = unum_getAttribute(format, UNUM_MIN_FRACTION_DIGITS);
  maxDecimalDigits = unum_getAttribute(format, UNUM_MAX_FRACTION_DIGITS);

  if(info->fPrecision != -1) {
    /* set the # of decimal digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, info->fPrecision);
  }
  else if(info->fPrecision == 0 && ! info->fAlt) {
    /* no decimal point in this case */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 0);
  }
  else if(info->fAlt) {
    /* '#' means always show decimal point */
    /* copy of printf behavior on Solaris - '#' shows 6 digits */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }
  else {
    /* # of decimal digits is 6 if precision not specified */
    unum_setAttribute(format, UNUM_FRACTION_DIGITS, 6);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* set whether to show the sign*/
    /* {sfb} TBD */
  }

  /* format the number */
  unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);
  len = u_strlen(result);

  /* pad and justify, if needed */
  if(info->fWidth != -1 && len < info->fWidth) {
    /* left justify */
    if(info->fLeft) {
      written = u_file_write(result, len, stream);
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
    }
    /* right justify */
    else {
      written = 0;
      for(i = 0; i < info->fWidth - len; ++i)
    written += u_file_write(&info->fPadChar, 1, stream);
      written += u_file_write(result, len, stream);
    }
  }
  /* just write the formatted output */
  else
    written = u_file_write(result, len, stream);
  
  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return written;
}

void
u_printf_init(void)
{
  int32_t i;
  /*Mutex *lock;*/

  /* if we're already inited, do nothing */
  if(g_u_printf_inited)
    return;

  /* lock the cache */
  umtx_lock(0);

  /* if we're already inited, do nothing */
  if(g_u_printf_inited) {
    umtx_unlock(0);
    return;
  }

  /* initialize all handlers and infos to 0 */
  for(i = 0; i < 256; ++i) {
    g_u_printf_infos[i]         = 0;
    g_u_printf_handlers[i]     = 0;
  }

  /* register the handlers for standard specifiers */
  /* don't use u_printf_register_handler to avoid mutex creation */

  /* handle '%' */
  g_u_printf_infos[ 0x0025 ]     = u_printf_simple_percent_info;
  g_u_printf_handlers[ 0x0025 ] = u_printf_simple_percent_handler;

  /* handle 's' */
  g_u_printf_infos[ 0x0073 ]     = u_printf_string_info;
  g_u_printf_handlers[ 0x0073 ] = u_printf_string_handler;

  /* handle 'd' */
  g_u_printf_infos[ 0x0064 ]     = u_printf_integer_info;
  g_u_printf_handlers[ 0x0064 ] = u_printf_integer_handler;

  /* handle 'i' */
  g_u_printf_infos[ 0x0069 ]     = u_printf_integer_info;
  g_u_printf_handlers[ 0x0069 ] = u_printf_integer_handler;

  /* handle 'o' */
  g_u_printf_infos[ 0x006F ]     = u_printf_octal_info;
  g_u_printf_handlers[ 0x006F ] = u_printf_octal_handler;

  /* handle 'u' */
  g_u_printf_infos[ 0x0075 ]     = u_printf_integer_info;
  g_u_printf_handlers[ 0x0075 ] = u_printf_integer_handler;

  /* handle 'x' */
  g_u_printf_infos[ 0x0078 ]     = u_printf_hex_info;
  g_u_printf_handlers[ 0x0078 ] = u_printf_hex_handler;

  /* handle 'X' */
  g_u_printf_infos[ 0x0058 ]     = u_printf_hex_info;
  g_u_printf_handlers[ 0x0058 ] = u_printf_hex_handler;

  /* handle 'f' */
  g_u_printf_infos[ 0x0066 ]     = u_printf_double_info;
  g_u_printf_handlers[ 0x0066 ] = u_printf_double_handler;

  /* handle 'c' */
  g_u_printf_infos[ 0x0063 ]     = u_printf_char_info;
  g_u_printf_handlers[ 0x0063 ] = u_printf_char_handler;

  /* handle 'p' */
  g_u_printf_infos[ 0x0070 ]     = u_printf_pointer_info;
  g_u_printf_handlers[ 0x0070 ] = u_printf_pointer_handler;

  /* handle 'e' */
  g_u_printf_infos[ 0x0065 ]     = u_printf_scientific_info;
  g_u_printf_handlers[ 0x0065 ] = u_printf_scientific_handler;

  /* handle 'E' */
  g_u_printf_infos[ 0x0045 ]     = u_printf_scientific_info;
  g_u_printf_handlers[ 0x0045 ] = u_printf_scientific_handler;

  /* handle 'D' */
  g_u_printf_infos[ 0x0044 ]     = u_printf_date_info;
  g_u_printf_handlers[ 0x0044 ] = u_printf_date_handler;

  /* handle 'P' */
  g_u_printf_infos[ 0x0050 ]     = u_printf_percent_info;
  g_u_printf_handlers[ 0x0050 ] = u_printf_percent_handler;

  /* handle 'M' */
  g_u_printf_infos[ 0x004D ]     = u_printf_currency_info;
  g_u_printf_handlers[ 0x004D ] = u_printf_currency_handler;

  /* handle 'T' */
  g_u_printf_infos[ 0x0054 ]     = u_printf_time_info;
  g_u_printf_handlers[ 0x0054 ] = u_printf_time_handler;

  /* handle 'K' */
  g_u_printf_infos[ 0x004B ]     = u_printf_uchar_info;
  g_u_printf_handlers[ 0x004B ] = u_printf_uchar_handler;

  /* handle 'U' */
  g_u_printf_infos[ 0x0055 ]     = u_printf_ustring_info;
  g_u_printf_handlers[ 0x0055 ] = u_printf_ustring_handler;

  /* handle 'g' */
  g_u_printf_infos[ 0x0067 ]     = u_printf_scidbl_info;
  g_u_printf_handlers[ 0x0067 ] = u_printf_scidbl_handler;

  /* handle 'G' */
  g_u_printf_infos[ 0x0047 ]     = u_printf_scidbl_info;
  g_u_printf_handlers[ 0x0047 ] = u_printf_scidbl_handler;

  /* handle 'n' */
  g_u_printf_infos[ 0x006E ]     = u_printf_count_info;
  g_u_printf_handlers[ 0x006E ] = u_printf_count_handler;

  /* handle 'V' */
  g_u_printf_infos[ 0x0056 ]     = u_printf_spellout_info;
  g_u_printf_handlers[ 0x0056 ] = u_printf_spellout_handler;


  /* we're finished */
  g_u_printf_inited = TRUE;

  /* unlock the cache */
  umtx_unlock(0);
}


#define U_PRINTF_MAX_ARGS 32
#define UP_PERCENT 0x0025

int32_t 
u_vfprintf_u(    UFILE        *f,
        const UChar    *patternSpecification,
        va_list        ap)
{
  u_printf_spec         spec;
  const UChar         *alias;
  int32_t         count, written;

  int32_t         num_args_wanted;
  int32_t         ufmt_types     [U_PRINTF_MAX_ARGS];
  ufmt_args     args[U_PRINTF_MAX_ARGS];  
        
  u_printf_info        info;
  u_printf_handler    handler;

  int32_t         cur_arg;


  /* init our function tables */
  if(! g_u_printf_inited)
    u_printf_init();

  /* alias the pattern */
  alias = patternSpecification;
  
  /* haven't written anything yet */
  written = 0;

  /* iterate through the pattern */
  for(;;) {

    /* find the next '%' */
    count = 0;
    while(*alias != UP_PERCENT && *alias != 0x0000) {
      *alias++;
      ++count;
    }

    /* write any characters before the '%' */
    if(count > 0)
      written += u_file_write(alias - count, count, f);

    /* break if at end of string */
    if(*alias == 0x0000)
      break;
    
    /* parse the specifier */
    count = u_printf_parse_spec(alias, &spec);

    /* fill in the precision and width, if specified out of line */

    /* width specified out of line */
    if(spec.fInfo.fWidth == -2) {
      if(spec.fWidthPos != -1) {
    /* handle positional parameter */
      }
      else {
    /* read the width from the argument list */
    spec.fInfo.fWidth = va_arg(ap, int);
      }
    
      /* if it's negative, take the absolute value and set left alignment */
      if(spec.fInfo.fWidth < 0) {
    spec.fInfo.fWidth     *= -1;
    spec.fInfo.fLeft     = TRUE;
      }
    }

    /* precision specified out of line */
    if(spec.fInfo.fPrecision == -2) {
      if(spec.fPrecisionPos != -1) {
    /* handle positional parameter */
      }
      else {
    /* read the precision from the argument list */
    spec.fInfo.fPrecision = va_arg(ap, int);
      }
      
      /* if it's negative, set it to zero */
      if(spec.fInfo.fPrecision < 0)
    spec.fInfo.fPrecision = 0;
    }
    
    /* query the info function for argument information */
    info = g_u_printf_infos[ (unsigned char) spec.fInfo.fSpec ];
    if(info != 0) { 
      num_args_wanted = (*info)(&spec.fInfo, 
                ufmt_types,
                U_PRINTF_MAX_ARGS);
    }
    else
      num_args_wanted = 0;

    /* fill in the requested arguments */
    for(cur_arg = 0; 
    cur_arg < num_args_wanted && cur_arg < U_PRINTF_MAX_ARGS; 
    ++cur_arg) {
      
      switch(ufmt_types[cur_arg]) {

      case ufmt_count:
    args[cur_arg].intValue = va_arg(ap, int);
    /* set the spec's width to the # of chars written */
    spec.fInfo.fWidth = written;
    break;

      case ufmt_int:
    args[cur_arg].intValue = va_arg(ap, int);
    break;
    
      case ufmt_char:
    args[cur_arg].intValue = va_arg(ap, int);
    break;
    
      case ufmt_wchar:
    args[cur_arg].wcharValue = va_arg(ap, wchar_t);
    break;
    
      case ufmt_string:
    args[cur_arg].ptrValue = va_arg(ap, char*);
    break;
    
      case ufmt_wstring:
    args[cur_arg].ptrValue = va_arg(ap, wchar_t*);
    break;
    
      case ufmt_pointer:
    args[cur_arg].ptrValue = va_arg(ap, void*);
    break;
    
      case ufmt_float:
    args[cur_arg].floatValue = va_arg(ap, float);
    break;
    
      case ufmt_double:
    args[cur_arg].doubleValue = va_arg(ap, double);
    break;

      case ufmt_date:
    args[cur_arg].dateValue = va_arg(ap, UDate);
    break;

      case ufmt_ustring:
    args[cur_arg].ptrValue = va_arg(ap, UChar*);
    break;

      case ufmt_uchar:
    args[cur_arg].intValue = va_arg(ap, int);
    break;
      }
    }
    
    /* call the handler function */
    handler = g_u_printf_handlers[ (unsigned char) spec.fInfo.fSpec ];
    if(handler != 0) {
      written += (*handler)(f, &spec.fInfo, args);
    }
    /* just echo unknown tags */
    else
      written += u_file_write(alias, count, f);
    
    /* update the pointer in pattern and continue */
    alias += count;
  }
  
  /* return # of UChars written */
  return written;
}

