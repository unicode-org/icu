/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File sprintf.c
*
* Modification History:
*
*   Date        Name        Description
*   02/08/00    george      Creation. Copied from uprintf.c
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "sprintf.h"
#include "sprntf_p.h"
#include "unicode/ustdio.h"
#include "unicode/ustring.h"
#include "locbund.h"
#include "loccache.h"
#include "unicode/unum.h"
#include "unicode/udat.h"
#include "unicode/uloc.h"

#include <string.h>
#include <stdlib.h>


/* --- Prototypes ---------------------------- */

int32_t
u_sprintf_simple_percent_handler(u_localized_string *output,
                const u_sprintf_spec_info     *info,
                const ufmt_args            *args);

int32_t
u_sprintf_string_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_sprintf_date_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args         *args);

int32_t
u_sprintf_scientific_handler(u_localized_string *output,
                const u_sprintf_spec_info     *info,
                const ufmt_args            *args);

int32_t
u_sprintf_scidbl_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_sprintf_uchar_handler(u_localized_string *output,
               const u_sprintf_spec_info     *info,
               const ufmt_args            *args);

int32_t
u_sprintf_currency_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args            *args);

int32_t
u_sprintf_ustring_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args         *args);

int32_t
u_sprintf_percent_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_sprintf_time_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args         *args);

int32_t
u_sprintf_spellout_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args            *args);

int32_t
u_sprintf_hex_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_sprintf_char_handler(u_localized_string *output,
              const u_sprintf_spec_info         *info,
              const ufmt_args              *args);

int32_t
u_sprintf_integer_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_sprintf_uinteger_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_sprintf_double_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_sprintf_count_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_sprintf_octal_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_sprintf_pointer_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args);

/* ANSI style formatting */
/* Use US-ASCII characters only for formatting */

/* % */
#define UFMT_SIMPLE_PERCENT {ufmt_simple_percent, u_sprintf_simple_percent_handler}
/* s */
#define UFMT_STRING         {ufmt_string, u_sprintf_string_handler}
/* c */
#define UFMT_CHAR           {ufmt_char, u_sprintf_char_handler}
/* d, i */
#define UFMT_INT            {ufmt_int, u_sprintf_integer_handler}
/* u */
#define UFMT_UINT           {ufmt_int, u_sprintf_uinteger_handler}
/* o */
#define UFMT_OCTAL          {ufmt_int, u_sprintf_octal_handler}
/* x, X */
#define UFMT_HEX            {ufmt_int, u_sprintf_hex_handler}
/* f */
#define UFMT_DOUBLE         {ufmt_double, u_sprintf_double_handler}
/* e, E */
#define UFMT_SCIENTIFIC     {ufmt_double, u_sprintf_scientific_handler}
/* g, G */
#define UFMT_SCIDBL         {ufmt_double, u_sprintf_scidbl_handler}
/* n */
#define UFMT_COUNT          {ufmt_count, u_sprintf_count_handler}

/* non-ANSI extensions */
/* Use US-ASCII characters only for formatting */

/* p */
#define UFMT_POINTER        {ufmt_pointer, u_sprintf_pointer_handler}
/* D */
#define UFMT_DATE           {ufmt_date, u_sprintf_date_handler}
/* T */
#define UFMT_TIME           {ufmt_date, u_sprintf_time_handler}
/* V */
#define UFMT_SPELLOUT       {ufmt_double, u_sprintf_spellout_handler}
/* P */
#define UFMT_PERCENT        {ufmt_double, u_sprintf_percent_handler}
/* M */
#define UFMT_CURRENCY       {ufmt_double, u_sprintf_currency_handler}
/* K */
#define UFMT_UCHAR          {ufmt_uchar, u_sprintf_uchar_handler}
/* U */
#define UFMT_USTRING        {ufmt_ustring, u_sprintf_ustring_handler}


#define UFMT_EMPTY {ufmt_empty, NULL}

struct u_sprintf_info {
    enum ufmt_type_info info;
    u_sprintf_handler handler;
};
typedef struct u_sprintf_info u_sprintf_info;

/* Use US-ASCII characters only for formatting. Most codepages have
 characters 20-7F from Unicode. Using any other codepage specific
 characters will make it very difficult to format the string on
 non-Unicode machines */
static const u_sprintf_info g_u_sprintf_infos[108] = {
/* 0x20 */
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_SIMPLE_PERCENT,UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,

/* 0x30 */
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,

/* 0x40 */
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_DATE,          UFMT_SCIENTIFIC,    UFMT_EMPTY,         UFMT_SCIDBL,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_UCHAR,
    UFMT_EMPTY,         UFMT_CURRENCY,      UFMT_EMPTY,         UFMT_EMPTY,

/* 0x50 */
    UFMT_PERCENT,       UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_TIME,          UFMT_USTRING,       UFMT_SPELLOUT,      UFMT_EMPTY,
    UFMT_HEX,           UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,

/* 0x60 */
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_CHAR,
    UFMT_INT,           UFMT_SCIENTIFIC,    UFMT_DOUBLE,        UFMT_SCIDBL,
    UFMT_EMPTY,         UFMT_INT,           UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_COUNT,         UFMT_OCTAL,

/* 0x70 */
    UFMT_POINTER,       UFMT_EMPTY,         UFMT_EMPTY,         UFMT_STRING,
    UFMT_EMPTY,         UFMT_UINT,          UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_HEX,           UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,
};

#define USPRINTF_NUM_FMT_HANDLERS sizeof(g_u_sprintf_infos)

/* We do not use handlers for 0-0x1f */
#define USPRINTF_BASE_FMT_HANDLERS 0x20

/* buffer size for formatting */
#define USPRINTF_BUFFER_SIZE 1024

int32_t 
u_sprintf(UChar       *buffer,
        const char    *locale,
        const char    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t written;
  
  va_start(ap, patternSpecification);
  written = u_vsnprintf(buffer, INT32_MAX, locale, patternSpecification, ap);
  va_end(ap);
  
  return written;
}

int32_t 
u_sprintf_u(UChar     *buffer,
        const char     *locale,
        const UChar    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t written;
  
  va_start(ap, patternSpecification);
  written = u_vsnprintf_u(buffer, INT32_MAX, locale, patternSpecification, ap);
  va_end(ap);
  
  return written;
}

int32_t 
u_vsprintf(UChar       *buffer,
        const char     *locale,
        const char     *patternSpecification,
        va_list         ap)
{
  return u_vsnprintf(buffer, INT32_MAX, locale, patternSpecification, ap);
}

int32_t 
u_snprintf(UChar       *buffer,
        int32_t         count,
        const char    *locale,
        const char    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t written;
  
  va_start(ap, patternSpecification);
  written = u_vsnprintf(buffer, count, locale, patternSpecification, ap);
  va_end(ap);
  
  return written;
}

int32_t 
u_snprintf_u(UChar     *buffer,
        int32_t        count,
        const char     *locale,
        const UChar    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t written;
  
  va_start(ap, patternSpecification);
  written = u_vsnprintf_u(buffer, count, locale, patternSpecification, ap);
  va_end(ap);
  
  return written;
}

int32_t 
u_vsnprintf(UChar       *buffer,
        int32_t         count,
        const char     *locale,
        const char     *patternSpecification,
        va_list         ap)
{
  int32_t written;
  UChar *pattern;
  
  /* convert from the default codepage to Unicode */
  pattern = ufmt_defaultCPToUnicode(patternSpecification, 
                    strlen(patternSpecification));
  if(pattern == 0) {
    return 0;
  }

  /* do the work */
  written = u_vsnprintf_u(buffer, count, locale, pattern, ap);

  /* clean up */
  free(pattern);
  
  return written;
}

static UChar *
u_strset(UChar *str, int32_t count, UChar c) {
    int32_t idx;
    for(idx = 0; idx < count; ++idx) {
         str[idx] = c;
    }
    return str;
}

/* copies the minimum number of code units of (count or output->available) */
static int32_t
u_minstrncpy(u_localized_string *output, const UChar *str, int32_t count) {
    int32_t size = ufmt_min(count, output->available);

    u_strncpy(output->str + (output->len - output->available), str, size);
    output->available -= size;
    return size;
}

static int32_t
u_sprintf_pad_and_justify(u_localized_string *output,
                const u_sprintf_spec_info    *info,
                const UChar                 *result,
                int32_t                     resultLen)
{
    int32_t written;

    resultLen = ufmt_min(resultLen, output->available);
    written = resultLen;

    /* pad and justify, if needed */
    if(info->fWidth != -1 && resultLen < info->fWidth) {
        int32_t paddingLeft = info->fWidth - resultLen;
        int32_t outputPos = output->len - output->available;

        if (paddingLeft + resultLen > output->available) {
            paddingLeft = output->available - resultLen;
        }
        written += paddingLeft;

        /* left justify */
        if(info->fLeft) {
            written += u_minstrncpy(output, result, resultLen);
            u_strset(&output->str[outputPos + resultLen], paddingLeft, info->fPadChar);
        }
        /* right justify */
        else {
            u_strset(&output->str[outputPos + resultLen], paddingLeft, info->fPadChar);
            written += u_minstrncpy(output, result, resultLen);
        }
    }
    /* just write the formatted output */
    else {
        written += u_minstrncpy(output, result, resultLen);
    }

    output->available -= written;
    return written;
}

/* handle a '%' */

int32_t
u_sprintf_simple_percent_handler(u_localized_string *output,
                const u_sprintf_spec_info     *info,
                const ufmt_args            *args)
{
    /* put a single '%' on the stream */
    if (output->available >= 1) {
        output->str[output->len - output->available--] = 0x0025;
        /* we wrote one character */
        return 1;
    }
    return 0;
}

/* handle 's' */

int32_t
u_sprintf_string_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args)
{
  UChar *s;
  int32_t len, written;
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
    written = u_minstrncpy(output, s, info->fPrecision);
  }
  /* determine if the string should be padded */
  else {
    written = u_sprintf_pad_and_justify(output, info, s, len);
  }

  /* clean up */
  free(s);
  
  return written;
}

/* HSYS */
int32_t
u_sprintf_integer_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args)
{
  long            num         = (long) (args[0].intValue);
  UNumberFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  int32_t        minDigits     = -1;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  if(info->fIsShort)
    num &= UINT16_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    num &= UINT32_MAX;

  /* get the formatter */
  format = u_locbund_getNumberFormat(output->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* set the minimum integer digits */
  if(info->fPrecision != -1) {
    /* clone the stream's bundle if it isn't owned */
    if(! output->fOwnBundle) {
      output->fBundle     = u_locbund_clone(output->fBundle);
      output->fOwnBundle = TRUE;
      format           = u_locbund_getNumberFormat(output->fBundle);
    }

    /* set the minimum # of digits */
    minDigits = unum_getAttribute(format, UNUM_MIN_INTEGER_DIGITS);
    unum_setAttribute(format, UNUM_MIN_INTEGER_DIGITS, info->fPrecision);
  }

  /* set whether to show the sign */
  if(info->fShowSign) {
    /* clone the stream's bundle if it isn't owned */
    if(! output->fOwnBundle) {
      output->fBundle     = u_locbund_clone(output->fBundle);
      output->fOwnBundle = TRUE;
      format           = u_locbund_getNumberFormat(output->fBundle);
    }

    /* set whether to show the sign*/
    /* {sfb} TODO */
  }

  /* format the number */
  unum_format(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);
  
  /* restore the number format */
  if(minDigits != -1)
    unum_setAttribute(format, UNUM_MIN_INTEGER_DIGITS, minDigits);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}

int32_t
u_sprintf_hex_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args)
{
  long            num         = (long) (args[0].intValue);
  UChar            result         [USPRINTF_BUFFER_SIZE];
  int32_t         len        = USPRINTF_BUFFER_SIZE;


  /* mask off any necessary bits */
  if(info->fIsShort)
    num &= UINT16_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    num &= UINT32_MAX;

  /* format the number, preserving the minimum # of digits */
  ufmt_ltou(result, &len, num, 16,
        (UBool)(info->fSpec == 0x0078),
        (info->fPrecision == -1 && info->fZero) ? info->fWidth : info->fPrecision);

  /* convert to alt form, if desired */
  if(num != 0 && info->fAlt && len < USPRINTF_BUFFER_SIZE - 2) {
    /* shift the formatted string right by 2 chars */
    memmove(result + 2, result, len * sizeof(UChar));
    result[0] = 0x0030;
    result[1] = info->fSpec;
    len += 2;
  }

  return u_sprintf_pad_and_justify(output, info, result, len);
}

int32_t
u_sprintf_octal_handler(u_localized_string *output,
               const u_sprintf_spec_info     *info,
               const ufmt_args            *args)
{
  long            num         = (long) (args[0].intValue);
  UChar            result         [USPRINTF_BUFFER_SIZE];
  int32_t         len        = USPRINTF_BUFFER_SIZE;


  /* mask off any necessary bits */
  if(info->fIsShort)
    num &= UINT16_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    num &= UINT32_MAX;

  /* format the number, preserving the minimum # of digits */
  ufmt_ltou(result, &len, num, 8,
        FALSE, /* doesn't matter for octal */
        info->fPrecision == -1 && info->fZero ? info->fWidth : info->fPrecision);

  /* convert to alt form, if desired */
  if(info->fAlt && result[0] != 0x0030 && len < USPRINTF_BUFFER_SIZE - 1) {
    /* shift the formatted string right by 1 char */
    memmove(result + 1, result, len * sizeof(UChar));
    result[0] = 0x0030;
    len += 1;
  }

  return u_sprintf_pad_and_justify(output, info, result, len);
}


int32_t
u_sprintf_uinteger_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args)
{
    u_sprintf_spec_info uint_info;
    ufmt_args uint_args;

    memcpy(&uint_info, info, sizeof(u_sprintf_spec_info));
    memcpy(&uint_args, args, sizeof(ufmt_args));

    uint_info.fPrecision = 0;
    uint_info.fAlt  = FALSE;

    /* Get around int32_t limitations */
    uint_args.doubleValue = ((double) ((uint32_t) (uint_args.intValue)));

    return u_sprintf_double_handler(output, &uint_info, &uint_args);
}

int32_t
u_sprintf_double_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args)
{
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  int32_t        minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;

  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getNumberFormat(output->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! output->fOwnBundle) {
    output->fBundle     = u_locbund_clone(output->fBundle);
    output->fOwnBundle     = TRUE;
    format           = u_locbund_getNumberFormat(output->fBundle);
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
    /* {sfb} TODO */
  }

  /* format the number */
  unum_formatDouble(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);
  
  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}


int32_t
u_sprintf_char_handler(u_localized_string *output,
              const u_sprintf_spec_info         *info,
              const ufmt_args              *args)
{
  UChar *s;
  int32_t len, written;
  unsigned char arg = (unsigned char)(args[0].intValue);

  /* convert from default codepage to Unicode */
  s = ufmt_defaultCPToUnicode((const char *)&arg, 1);
  if(s == 0) {
    return 0;
  }

  /* Remember that this may be a surrogate pair */
  len = u_strlen(s);
  
  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the string should be truncated */
  if(info->fPrecision != -1 && len > info->fPrecision) {
    written = u_minstrncpy(output, s, info->fPrecision);
  }
  else {
    /* determine if the string should be padded */
    written = u_sprintf_pad_and_justify(output, info, s, len);
  }

  /* clean up */
  free(s);

  return written;
}


int32_t
u_sprintf_pointer_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args)
{
  long            num         = (long) (args[0].intValue);
  UChar            result         [USPRINTF_BUFFER_SIZE];
  int32_t         len        = USPRINTF_BUFFER_SIZE;


  /* format the pointer in hex */
  ufmt_ltou(result, &len, num, 16, TRUE, info->fPrecision);

  return u_sprintf_pad_and_justify(output, info, result, len);
}


int32_t
u_sprintf_scientific_handler(u_localized_string *output,
                const u_sprintf_spec_info     *info,
                const ufmt_args            *args)
{
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  int32_t        minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;
  

  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getScientificFormat(output->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! output->fOwnBundle) {
    output->fBundle     = u_locbund_clone(output->fBundle);
    output->fOwnBundle     = TRUE;
    format           = u_locbund_getScientificFormat(output->fBundle);
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
    /* {sfb} TODO */
  }

  /* format the number */
  unum_formatDouble(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);

  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}

int32_t
u_sprintf_date_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args         *args)
{
  UDate            num         = (UDate) (args[0].dateValue);
  UDateFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  UErrorCode        status        = U_ZERO_ERROR;


  /* get the formatter */
  format = u_locbund_getDateFormat(output->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;

  /* format the date */
  udat_format(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}

int32_t
u_sprintf_time_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args         *args)
{
  UDate            num         = (UDate) (args[0].dateValue);
  UDateFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  UErrorCode        status        = U_ZERO_ERROR;


  /* get the formatter */
  format = u_locbund_getTimeFormat(output->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;

  /* format the time */
  udat_format(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}


int32_t
u_sprintf_percent_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args            *args)
{
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  int32_t        minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getPercentFormat(output->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! output->fOwnBundle) {
    output->fBundle     = u_locbund_clone(output->fBundle);
    output->fOwnBundle     = TRUE;
    format           = u_locbund_getPercentFormat(output->fBundle);
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
    /* {sfb} TODO */
  }

  /* format the number */
  unum_formatDouble(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);

  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}


int32_t
u_sprintf_currency_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args            *args)
{
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  int32_t        minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getCurrencyFormat(output->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! output->fOwnBundle) {
    output->fBundle     = u_locbund_clone(output->fBundle);
    output->fOwnBundle     = TRUE;
    format           = u_locbund_getCurrencyFormat(output->fBundle);
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
    /* {sfb} TODO */
  }

  /* format the number */
  unum_formatDouble(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);

  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}

int32_t
u_sprintf_ustring_handler(u_localized_string *output,
             const u_sprintf_spec_info     *info,
             const ufmt_args         *args)
{
  int32_t len, written;
  const UChar *arg = (const UChar*)(args[0].ptrValue);

  /* allocate enough space for the buffer */
  len = u_strlen(arg);

  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the string should be truncated */
  if(info->fPrecision != -1 && len > info->fPrecision) {
    written = u_minstrncpy(output, arg, info->fPrecision);
  }
  else {
    /* determine if the string should be padded */
    written = u_sprintf_pad_and_justify(output, info, arg, len);
  }

  return written;
}



int32_t
u_sprintf_uchar_handler(u_localized_string *output,
               const u_sprintf_spec_info     *info,
               const ufmt_args            *args)
{
  int32_t written = 0;
  UChar arg = (UChar)(args[0].intValue);
  

  /* width = minimum # of characters to write */
  /* precision = maximum # of characters to write */

  /* precision takes precedence over width */
  /* determine if the char should be printed */
  if(info->fPrecision != -1 && info->fPrecision < 1) {
    /* write nothing */
    written = 0;
  }
  else {
    /* determine if the string should be padded */
    written = u_sprintf_pad_and_justify(output, info, &arg, 1);
  }

  return written;
}

int32_t
u_sprintf_scidbl_handler(u_localized_string *output,
            const u_sprintf_spec_info     *info,
            const ufmt_args            *args)
{
  double     num = (double)(args[0].doubleValue);
  UBool     useE;

  /* a precision of 0 is taken as 1 */
  if(info->fPrecision == 0)
    ((u_sprintf_spec_info*)info)->fPrecision = 1;

  /* determine whether to use 'e' or 'f' */
  useE = (UBool)(num < 0.0001
       || (info->fPrecision != -1 && num > uprv_pow10(info->fPrecision)));
  
  /* use 'e' */
  if(useE) {
    /* adjust the specifier */
    ((u_sprintf_spec_info*)info)->fSpec = 0x0065;
    /* call the scientific handler */
    return u_sprintf_scientific_handler(output, info, args);
  }
  /* use 'f' */
  else {
    /* adjust the specifier */
    ((u_sprintf_spec_info*)info)->fSpec = 0x0066;
    /* call the double handler */
    return u_sprintf_double_handler(output, info, args);
  }
}


int32_t
u_sprintf_count_handler(u_localized_string *output,
               const u_sprintf_spec_info     *info,
               const ufmt_args            *args)
{
  int *count = (int*)(args[0].ptrValue);

  /* in the special case of count, the u_printf_spec_info's width */
  /* will contain the # of chars written thus far */
  *count = info->fWidth;

  return 0;
}


int32_t
u_sprintf_spellout_handler(u_localized_string *output,
              const u_sprintf_spec_info     *info,
              const ufmt_args            *args)
{
  double        num         = (double) (args[0].doubleValue);
  UNumberFormat        *format;
  UChar            result        [USPRINTF_BUFFER_SIZE];
  int32_t        minDecimalDigits;
  int32_t        maxDecimalDigits;
  UErrorCode        status        = U_ZERO_ERROR;


  /* mask off any necessary bits */
  /*  if(! info->fIsLongDouble)
      num &= DBL_MAX;*/

  /* get the formatter */
  format = u_locbund_getSpelloutFormat(output->fBundle);

  /* handle error */
  if(format == 0)
    return 0;

  /* set the appropriate flags on the formatter */

  /* clone the stream's bundle if it isn't owned */
  if(! output->fOwnBundle) {
    output->fBundle     = u_locbund_clone(output->fBundle);
    output->fOwnBundle     = TRUE;
    format           = u_locbund_getSpelloutFormat(output->fBundle);
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
    /* {sfb} TODO */
  }

  /* format the number */
  unum_formatDouble(format, num, result, USPRINTF_BUFFER_SIZE, 0, &status);

  /* restore the number format */
  unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
  unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

  return u_sprintf_pad_and_justify(output, info, result, u_strlen(result));
}

#define UP_PERCENT 0x0025

int32_t 
u_vsnprintf_u(UChar    *buffer,
        int32_t        count,
        const char     *locale,
        const UChar    *patternSpecification,
        va_list        ap)
{
  const UChar       *alias = patternSpecification;
  const UChar       *lastAlias;
  int32_t           patCount;
  int32_t           written = 0;
  uint16_t          handlerNum;

  ufmt_args         args;  
  u_localized_string outStr;
  u_sprintf_spec    spec;
  ufmt_type_info    info;
  u_sprintf_handler handler;

  if (count < 0) {
    count = INT32_MAX;
  }

  outStr.str = buffer;
  outStr.len = count;
  outStr.available = count;

  /* if locale is 0, use the default */
  if(locale == 0) {
    locale = uloc_getDefault();
  }
  outStr.fBundle = u_loccache_get(locale);

  if(outStr.fBundle == 0) {
    return 0;
  }
  outStr.fOwnBundle     = FALSE;

  /* iterate through the pattern */
  while(outStr.available > 0) {

    /* find the next '%' */
    lastAlias = alias;
    while(*alias != UP_PERCENT && *alias != 0x0000) {
      alias++;
    }

    /* write any characters before the '%' */
    if(alias > lastAlias) {
      written += u_minstrncpy(&outStr, lastAlias, (int32_t)(alias - lastAlias));
    }

    /* break if at end of string */
    if(*alias == 0x0000)
      break;
    
    /* parse the specifier */
    patCount = u_sprintf_parse_spec(alias, &spec);

    /* fill in the precision and width, if specified out of line */

    /* width specified out of line */
    if(spec.fInfo.fWidth == -2) {
      if(spec.fWidthPos == -1) {
        /* read the width from the argument list */
        spec.fInfo.fWidth = va_arg(ap, int);
      }
      else {
        /* handle positional parameter */
      }
    
      /* if it's negative, take the absolute value and set left alignment */
      if(spec.fInfo.fWidth < 0) {
        spec.fInfo.fWidth     *= -1;
        spec.fInfo.fLeft     = TRUE;
      }
    }

    /* precision specified out of line */
    if(spec.fInfo.fPrecision == -2) {
      if(spec.fPrecisionPos == -1) {
        /* read the precision from the argument list */
        spec.fInfo.fPrecision = va_arg(ap, int);
      }
      else {
        /* handle positional parameter */
      }
      
      /* if it's negative, set it to zero */
      if(spec.fInfo.fPrecision < 0)
        spec.fInfo.fPrecision = 0;
    }
    
    handlerNum = (uint16_t)(spec.fInfo.fSpec - USPRINTF_BASE_FMT_HANDLERS);
    if (handlerNum < USPRINTF_NUM_FMT_HANDLERS) {
        /* query the info function for argument information */
        info = g_u_sprintf_infos[ handlerNum ].info;
        if(info > ufmt_simple_percent) { 
          switch(info) {

          case ufmt_count:
            /* set the spec's width to the # of chars written */
            spec.fInfo.fWidth = written;

          case ufmt_char:
          case ufmt_uchar:
          case ufmt_int:
            args.intValue = va_arg(ap, int);
            break;

          case ufmt_wchar:
            args.wcharValue = va_arg(ap, wchar_t);
            break;

          case ufmt_string:
            args.ptrValue = va_arg(ap, char*);
            break;
    
          case ufmt_wstring:
            args.ptrValue = va_arg(ap, wchar_t*);
            break;
    
          case ufmt_ustring:
            args.ptrValue = va_arg(ap, UChar*);
            break;

          case ufmt_pointer:
            args.ptrValue = va_arg(ap, void*);
            break;
    
          case ufmt_float:
            args.floatValue = (float) va_arg(ap, double);
            break;
    
          case ufmt_double:
            args.doubleValue = va_arg(ap, double);
            break;

          case ufmt_date:
            args.dateValue = va_arg(ap, UDate);
            break;

          default:
            break;  /* Should never get here */
          }
        }

        /* call the handler function */
        handler = g_u_sprintf_infos[ handlerNum ].handler;
        if(handler != 0) {
           written += (*handler)(&outStr, &spec.fInfo, &args);
        }
        else {
          /* just echo unknown tags */
          written += u_minstrncpy(&outStr, lastAlias, (int32_t)(alias - lastAlias));
        }
    }
    else {
      /* just echo unknown tags */
      written += u_minstrncpy(&outStr, lastAlias, (int32_t)(alias - lastAlias));
    }
    
    /* update the pointer in pattern and continue */
    alias += patCount;
  }
  
  /* return # of UChars written */
  return written;
}

