/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File sscanf.c
*
* Modification History:
*
*   Date        Name        Description
*   02/08/00    george      Creation. Copied from uscanf.c
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"

#include "sscanf.h"
#include "sscanf_p.h"
#include "uscanset.h"
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
u_sscanf_simple_percent_handler(u_localized_string    *input,
                   const u_sscanf_spec_info     *info,
                   ufmt_args             *args,
                   const UChar        *fmt,
                   int32_t            *consumed);

int32_t
u_sscanf_ustring_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_sscanf_count_handler(u_localized_string    *input,
              const u_sscanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed);

int32_t
u_sscanf_integer_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_sscanf_uinteger_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_sscanf_double_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed);

int32_t
u_sscanf_scientific_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args           *args,
               const UChar            *fmt,
               int32_t            *consumed);

int32_t
u_sscanf_scidbl_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed);

int32_t
u_sscanf_currency_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args            *args,
             const UChar            *fmt,
             int32_t            *consumed);

int32_t
u_sscanf_percent_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_sscanf_date_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed);

int32_t
u_sscanf_time_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed);

int32_t
u_sscanf_char_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed);

int32_t
u_sscanf_uchar_handler(u_localized_string    *input,
              const u_sscanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed);

int32_t
u_sscanf_spellout_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args             *args,
             const UChar            *fmt,
             int32_t            *consumed);

int32_t
u_sscanf_hex_handler(u_localized_string    *input,
            const u_sscanf_spec_info     *info,
            ufmt_args            *args,
            const UChar            *fmt,
            int32_t            *consumed);

int32_t
u_sscanf_octal_handler(u_localized_string    *input,
              const u_sscanf_spec_info     *info,
              ufmt_args         *args,
              const UChar        *fmt,
              int32_t            *consumed);

int32_t
u_sscanf_pointer_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_sscanf_string_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args             *args,
               const UChar        *fmt,
               int32_t            *consumed);

int32_t
u_sscanf_scanset_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

/* ANSI style formatting */
/* Use US-ASCII characters only for formatting */

/* % */
#define UFMT_SIMPLE_PERCENT {ufmt_simple_percent, u_sscanf_simple_percent_handler}
/* s */
#define UFMT_STRING         {ufmt_string, u_sscanf_string_handler}
/* c */
#define UFMT_CHAR           {ufmt_string, u_sscanf_char_handler}
/* d, i */
#define UFMT_INT            {ufmt_int, u_sscanf_integer_handler}
/* u */
#define UFMT_UINT           {ufmt_int, u_sscanf_uinteger_handler}
/* o */
#define UFMT_OCTAL          {ufmt_int, u_sscanf_octal_handler}
/* x, X */
#define UFMT_HEX            {ufmt_int, u_sscanf_hex_handler}
/* f */
#define UFMT_DOUBLE         {ufmt_double, u_sscanf_double_handler}
/* e, E */
#define UFMT_SCIENTIFIC     {ufmt_double, u_sscanf_scientific_handler}
/* g, G */
#define UFMT_SCIDBL         {ufmt_double, u_sscanf_scidbl_handler}
/* n */
#define UFMT_COUNT          {ufmt_count, u_sscanf_count_handler}
/* [ */
#define UFMT_SCANSET        {ufmt_string, u_sscanf_scanset_handler} /* TODO: Is this also suppose to be ufmt_ustring */

/* non-ANSI extensions */
/* Use US-ASCII characters only for formatting */

/* p */
#define UFMT_POINTER        {ufmt_pointer, u_sscanf_pointer_handler}
/* D */
#define UFMT_DATE           {ufmt_date, u_sscanf_date_handler}
/* T */
#define UFMT_TIME           {ufmt_date, u_sscanf_time_handler}
/* V */
#define UFMT_SPELLOUT       {ufmt_double, u_sscanf_spellout_handler}
/* P */
#define UFMT_PERCENT        {ufmt_double, u_sscanf_percent_handler}
/* M */
#define UFMT_CURRENCY       {ufmt_double, u_sscanf_currency_handler}
/* K */
#define UFMT_UCHAR          {ufmt_uchar, u_sscanf_uchar_handler}
/* U */
#define UFMT_USTRING        {ufmt_ustring, u_sscanf_ustring_handler}


#define UFMT_EMPTY {ufmt_empty, NULL}

struct u_sscanf_info {
    enum ufmt_type_info info;
    u_sscanf_handler handler;
};
typedef struct u_sscanf_info u_sscanf_info;

/* Use US-ASCII characters only for formatting. Most codepages have
 characters 20-7F from Unicode. Using any other codepage specific
 characters will make it very difficult to format the string on
 non-Unicode machines */
static const u_sscanf_info g_u_sscanf_infos[108] = {
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
    UFMT_HEX,           UFMT_EMPTY,         UFMT_EMPTY,         UFMT_SCANSET,
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

#define USCANF_NUM_FMT_HANDLERS sizeof(g_u_sscanf_infos)

/* We do not use handlers for 0-0x1f */
#define USCANF_BASE_FMT_HANDLERS 0x20

int32_t 
u_sscanf(UChar         *buffer,
        const char     *locale,
        const char    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t converted;
  
  va_start(ap, patternSpecification);
  converted = u_vsscanf(buffer, locale, patternSpecification, ap);
  va_end(ap);
  
  return converted;
}

int32_t 
u_sscanf_u(UChar         *buffer,
        const char     *locale,
        const UChar    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t converted;
  
  va_start(ap, patternSpecification);
  converted = u_vsscanf_u(buffer, locale, patternSpecification, ap);
  va_end(ap);
  
  return converted;
}

int32_t 
u_vsscanf(UChar         *buffer,
        const char     *locale,
        const char    *patternSpecification,
        va_list        ap)
{
  int32_t converted;
  UChar *pattern;
  
  /* convert from the default codepage to Unicode */
  pattern = ufmt_defaultCPToUnicode(patternSpecification, 
                    strlen(patternSpecification));
  if(pattern == 0) {
    return 0;
  }
  
  /* do the work */
  converted = u_vsscanf_u(buffer, locale, pattern, ap);

  /* clean up */
  free(pattern);
  
  return converted;
}

static int32_t
u_sscanf_skip_leading_ws(u_localized_string    *input,
            UChar     pad)
{
  UChar     c;
  int32_t   count = input->pos;
  int32_t   skipped;
  
  /* skip all leading ws in the stream */
  while( ((c = input->str[count]) != 0xFFFF) && (c == pad || ufmt_isws(c)) )
    count++;

  if(c == 0xFFFF)
    count++;

  skipped = count - input->pos;
  input->pos = count;
  return skipped;
}

int32_t 
u_sscanf_simple_percent_handler(u_localized_string    *input,
                   const u_sscanf_spec_info     *info,
                   ufmt_args             *args,
                   const UChar        *fmt,
                   int32_t            *consumed)
{
  /* make sure the next character in the stream is a percent */
  if(input->str[input->pos++] != 0x0025) {
    return -1;
  }

  return 0;
}

int32_t
u_sscanf_string_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args             *args,
               const UChar        *fmt,
               int32_t            *consumed)
{
  UChar     c;
  int32_t     count;
  const UChar     *source;
  UConverter     *conv;
  UErrorCode     status     = U_ZERO_ERROR;
  char         *arg     = (char*)(args[0].ptrValue);
  char         *alias     = arg;
  char         *limit;

  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);
  
  /* get the string one character at a time, truncating to the width */
  count = 0;

  /* open the default converter */
  conv = ucnv_open(ucnv_getDefaultName(), &status);

  if(U_FAILURE(status))
    return -1;

  /* since there is no real limit, just use a reasonable value */
  limit = alias + 2048; /* TODO: Specify a real limit! */

  while( ((c = input->str[input->pos++]) != 0xFFFF) && 
     (c != info->fPadChar && ! ufmt_isws(c)) &&
     (info->fWidth == -1 || count < info->fWidth) ) {
    
    /* put the character from the stream onto the target */
    source = &c;

    /* convert the character to the default codepage */
    ucnv_fromUnicode(conv, &alias, limit, &source, source + 1,
             NULL, TRUE, &status);
    
    if(U_FAILURE(status))
      return -1;
    
    /* increment the count */
    ++count;
  }

  /* put the final character we read back on the stream */  
  if(c != 0xFFFF)
    input->pos--;
  
  /* add the terminator */
  *alias = 0x00;

  /* clean up */
  ucnv_close(conv);

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_ustring_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  UChar     c;
  int32_t     count;
  UChar     *arg     = (UChar*)(args[0].ptrValue);
  UChar     *alias     = arg;
  
  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);
  
  /* get the string one character at a time, truncating to the width */
  count = 0;

  while( ((c = input->str[input->pos++]) != 0xFFFF) && 
     (c != info->fPadChar && ! ufmt_isws(c)) &&
     (info->fWidth == -1 || count < info->fWidth) ) {
    
    /* put the character from the stream onto the target */
    *alias++ = c;
    
    /* increment the count */
    ++count;
  }

  /* put the final character we read back on the stream */  
  if(c != 0xFFFF)
    input->pos--;
  
  /* add the terminator */
  *alias = 0x0000;

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_count_handler(u_localized_string    *input,
              const u_sscanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed)
{
  int *converted = (int*)(args[0].ptrValue);
  
  /* in the special case of count, the u_sscanf_spec_info's width */
  /* will contain the # of items converted thus far */
  *converted = info->fWidth;

  /* we converted 0 args */
  return 0;
}

int32_t
u_sscanf_integer_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  int32_t        len;
  long            *num         = (long*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getNumberFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parse(format, &(input->str[input->pos]), len, &parsePos, &status);

  /* mask off any necessary bits */
  if(info->fIsShort)
    *num &= UINT16_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    *num &= UINT32_MAX;

  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_uinteger_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
    ufmt_args uint_args;
    int32_t converted_args;
    uint32_t            *num         = (uint32_t*) (args[0].ptrValue);
    double currDouble;

    memcpy(&uint_args, args, sizeof(ufmt_args));
    uint_args.ptrValue = &currDouble;
    converted_args = u_sscanf_double_handler(input, info, &uint_args, fmt, consumed);

    *num = (uint32_t)currDouble;

    return converted_args;
}

int32_t
u_sscanf_double_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getNumberFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, &(input->str[input->pos]), len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_scientific_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args           *args,
               const UChar            *fmt,
               int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getScientificFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, &(input->str[input->pos]), len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_scidbl_handler(u_localized_string    *input,
               const u_sscanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat *scientificFormat, *genericFormat;
  /*int32_t       scientificResult, genericResult;*/
  double        scientificResult, genericResult;
  int32_t       scientificParsePos = 0, genericParsePos = 0;
  UErrorCode    scientificStatus = U_ZERO_ERROR;
  UErrorCode    genericStatus = U_ZERO_ERROR;


  /* since we can't determine by scanning the characters whether */
  /* a number was formatted in the 'f' or 'g' styles, parse the */
  /* string with both formatters, and assume whichever one */
  /* parsed the most is the correct formatter to use */


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatters */
  scientificFormat = u_locbund_getScientificFormat(input->fBundle);
  genericFormat = u_locbund_getNumberFormat(input->fBundle);
  
  /* handle error */
  if(scientificFormat == 0 || genericFormat == 0)
    return 0;
  
  /* parse the number using each format*/

  scientificResult = unum_parseDouble(scientificFormat, &(input->str[input->pos]), len,
                      &scientificParsePos, &scientificStatus);

  genericResult = unum_parseDouble(genericFormat, &(input->str[input->pos]), len,
                   &genericParsePos, &genericStatus);

  /* determine which parse made it farther */
  if(scientificParsePos > genericParsePos) {
    /* stash the result in num */
    *num = scientificResult;
    /* update the stream's position to reflect consumed data */
    input->pos += scientificParsePos;
  }
  else {
    /* stash the result in num */
    *num = genericResult;
    /* update the stream's position to reflect consumed data */
    input->pos += genericParsePos;
  }
  
  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_currency_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args            *args,
             const UChar            *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getCurrencyFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, &(input->str[input->pos]), len, &parsePos, &status);
  
  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_percent_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getPercentFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, &(input->str[input->pos]), len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_date_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  UDate            *date         = (UDate*) (args[0].ptrValue);
  UDateFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getDateFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *date = udat_parse(format, &(input->str[input->pos]), len, &parsePos, &status);
  
  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_time_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  UDate            *time         = (UDate*) (args[0].ptrValue);
  UDateFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getTimeFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *time = udat_parse(format, &(input->str[input->pos]), len, &parsePos, &status);
  
  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_char_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed)
{
  UChar uc = 0;
  char *result;
  char *c = (char*)(args[0].ptrValue);
  
  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);
  
  /* get the character from the stream, truncating to the width */
  if(info->fWidth == -1 || info->fWidth > 1)
    uc = input->str[input->pos++];

  /* handle EOF */
  if(uc == 0xFFFF)
    return -1;

  /* convert the character to the default codepage */
  result = ufmt_unicodeToDefaultCP(&uc, 1);
  *c = result[0];

  /* clean up */
  free(result);
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_uchar_handler(u_localized_string    *input,
              const u_sscanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed)
{
  UChar *c = (UChar*)(args[0].ptrValue);
  
  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);
  
  /* get the character from the stream, truncating to the width */
  if(info->fWidth == -1 || info->fWidth > 1)
    *c = input->str[input->pos];

  /* handle EOF */
  if(*c == 0xFFFF)
    return -1;

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_spellout_handler(u_localized_string    *input,
             const u_sscanf_spec_info     *info,
             ufmt_args             *args,
             const UChar            *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = U_ZERO_ERROR;


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getSpelloutFormat(input->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, &(input->str[input->pos]), len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  input->pos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_hex_handler(u_localized_string    *input,
            const u_sscanf_spec_info     *info,
            ufmt_args            *args,
            const UChar            *fmt,
            int32_t            *consumed)
{
  int32_t        len;
  long            *num         = (long*) (args[0].ptrValue);


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* check for alternate form */
  if( input->str[input->pos] == 0x0030 && 
      (input->str[input->pos + 1] == 0x0078 || input->str[input->pos + 1] == 0x0058) ) {

    /* skip the '0' and 'x' or 'X' if present */
    input->pos += 2;
    len -= 2;
  }

  /* parse the number */
  *num = ufmt_utol(&(input->str[input->pos]), &len, 16);

  /* update the stream's position to reflect consumed data */
  input->pos += len;

  /* mask off any necessary bits */
  if(info->fIsShort)
    *num &= UINT16_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    *num &= UINT32_MAX;

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_octal_handler(u_localized_string    *input,
              const u_sscanf_spec_info     *info,
              ufmt_args         *args,
              const UChar        *fmt,
              int32_t            *consumed)
{
  int32_t        len;
  long            *num         = (long*) (args[0].ptrValue);


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* parse the number */
  *num = ufmt_utol(&(input->str[input->pos]), &len, 8);

  /* update the stream's position to reflect consumed data */
  input->pos += len;

  /* mask off any necessary bits */
  if(info->fIsShort)
    *num &= UINT16_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    *num &= UINT32_MAX;

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_pointer_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  int32_t    len;
  void        *p     = (void*)(args[0].ptrValue);


  /* skip all ws in the stream */
  u_sscanf_skip_leading_ws(input, info->fPadChar);

  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* parse the pointer - cast to void** to assign to *p */
  *(void**)p = (void*) ufmt_utol(&(input->str[input->pos]), &len, 16);

  /* update the stream's position to reflect consumed data */
  input->pos += len;

  /* we converted 1 arg */
  return 1;
}

int32_t
u_sscanf_scanset_handler(u_localized_string    *input,
            const u_sscanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  u_scanf_scanset    scanset;
  int32_t        len;
  UBool        success;
  UChar            c;
  const UChar         *source;
  UConverter         *conv;
  UErrorCode         status     = U_ZERO_ERROR;
  char            *s     = (char*) (args[0].ptrValue);
  char             *alias, *limit;


  /* determine the size of the stream's buffer */
  len = input->len - input->pos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* alias the target */
  alias = s;
  limit = alias + len;

  /* parse the scanset from the fmt string */
  *consumed = u_strlen(fmt);
  success = u_scanf_scanset_init(&scanset, fmt, consumed);

  /* increment consumed by one to eat the final ']' */
  ++(*consumed);

  /* open the default converter */
  conv = ucnv_open(ucnv_getDefaultName(), &status);

  /* verify that the parse was successful and the converter opened */
  if(! success || U_FAILURE(status))
    return -1;

  /* grab characters one at a time and make sure they are in the scanset */
  while( (c = input->str[input->pos++]) != 0xFFFF && alias < limit) {
    if(u_scanf_scanset_in(&scanset, c)) {
      source = &c;
      /* convert the character to the default codepage */
      ucnv_fromUnicode(conv, &alias, limit, &source, source + 1,
               NULL, TRUE, &status);
      
      if(U_FAILURE(status))
    return -1;
    }
    /* if the character's not in the scanset, break out */
    else {
      break;
    }
  }

  /* put the final character we read back on the stream */  
  if(c != 0xFFFF)
    input->str[input->pos--];

  /* if we didn't match at least 1 character, fail */
  if(alias == s)
    return -1;
  /* otherwise, add the terminator */
  else
    *alias = 0x00;

  /* clean up */
  ucnv_close(conv);

  /* we converted 1 arg */
  return 1;
}


#define UP_PERCENT 0x0025

int32_t 
u_vsscanf_u(UChar         *buffer,
        const char     *locale,
        const UChar    *patternSpecification,
        va_list        ap)
{
  const UChar         *alias;
  int32_t         count, converted, temp;
  uint16_t      handlerNum;

  ufmt_args       args;
  u_localized_string inStr;
  u_sscanf_spec         spec;
  ufmt_type_info    info;
  u_sscanf_handler handler;

  /* alias the pattern */
  alias = patternSpecification;

  inStr.str = buffer;
  inStr.len = u_strlen(buffer);
  inStr.pos = 0;

  /* haven't converted anything yet */
  converted = 0;

  /* if locale is 0, use the default */
  if(locale == 0) {
    locale = uloc_getDefault();
  }
  inStr.fBundle = u_loccache_get(locale);

  if(inStr.fBundle == 0) {
    return 0;
  }
  inStr.fOwnBundle     = FALSE;

  /* iterate through the pattern */
  for(;;) {
    
    /* match any characters up to the next '%' */
    while(*alias != UP_PERCENT && *alias != 0x0000 && inStr.str[inStr.pos++] == *alias) {
      alias++;
    }

    /* if we aren't at a '%', or if we're at end of string, break*/
    if(*alias != UP_PERCENT || *alias == 0x0000)
      break;

    /* parse the specifier */
    count = u_sscanf_parse_spec(alias, &spec);

    /* update the pointer in pattern */
    alias += count;

    /* skip the argument, if necessary */
    if(spec.fSkipArg)
      va_arg(ap, int);

    handlerNum = (uint16_t)(spec.fInfo.fSpec - USCANF_BASE_FMT_HANDLERS);
    if (handlerNum < USCANF_NUM_FMT_HANDLERS) {
      /* query the info function for argument information */
      info = g_u_sscanf_infos[ handlerNum ].info;
      if(info > ufmt_simple_percent) { 
          switch(info) {

          case ufmt_count:
            args.intValue = va_arg(ap, int);
            /* set the spec's width to the # of items converted */
            spec.fInfo.fWidth = converted;
            break;

          case ufmt_char:
          case ufmt_uchar:
          case ufmt_int:
            args.ptrValue = va_arg(ap, int*);
            break;

          case ufmt_wchar:
            args.ptrValue = va_arg(ap, wchar_t*);
            break;

          case ufmt_string:
            args.ptrValue = va_arg(ap, char*);
            break;

          case ufmt_wstring:
            args.ptrValue = va_arg(ap, wchar_t*);
            break;

          case ufmt_pointer:
            args.ptrValue = va_arg(ap, void*);
            break;

          case ufmt_float:
            args.ptrValue = va_arg(ap, float*);
            break;

          case ufmt_double:
            args.ptrValue = va_arg(ap, double*);
            break;

          case ufmt_date:
            args.ptrValue = va_arg(ap, UDate*);
            break;

          case ufmt_ustring:
            args.ptrValue = va_arg(ap, UChar*);
            break;

          default:
            break;  /* Should never get here */
          }
        }
        /* call the handler function */
        handler = g_u_sscanf_infos[ handlerNum ].handler;
        if(handler != 0) {

          /* reset count */
          count = 0;

          temp = (*handler)(&inStr, &spec.fInfo, &args, alias, &count);

          /* if the handler encountered an error condition, break */
          if(temp == -1)
            break;

          /* add to the # of items converted */
          converted += temp;

          /* update the pointer in pattern */
          alias += count;
        }
        /* else do nothing */
    }
    /* else do nothing */

    /* just ignore unknown tags */

  }

  /* return # of items converted */
  return converted;
}

