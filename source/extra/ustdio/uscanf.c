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
* File uscanf.c
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
*******************************************************************************
*/

#include "uchar.h"

#include "uscanf.h"
#include "uscanf_p.h"
#include "uscanset.h"
#include "ustdio.h"
#include "ufile.h"
#include "ustring.h"
#include "locbund.h"
#include "umutex.h"
#include "unum.h"
#include "udat.h"

#include <string.h>
#include <stdlib.h>
#include <float.h>
#include <limits.h>
#include <wchar.h>


u_scanf_handler g_u_scanf_handlers     [256];
u_scanf_info       g_u_scanf_infos     [256];
bool_t        g_u_scanf_inited    = FALSE;

int32_t 
u_fscanf(    UFILE        *f,
        const char    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t converted;
  
  va_start(ap, patternSpecification);
  converted = u_vfscanf(f, patternSpecification, ap);
  va_end(ap);
  
  return converted;
}

int32_t 
u_fscanf_u(    UFILE        *f,
        const UChar    *patternSpecification,
        ... )
{
  va_list ap;
  int32_t converted;
  
  va_start(ap, patternSpecification);
  converted = u_vfscanf_u(f, patternSpecification, ap);
  va_end(ap);
  
  return converted;
}

int32_t 
u_vfscanf(    UFILE        *f,
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
  converted = u_vfscanf_u(f, pattern, ap);

  /* clean up */
  free(pattern);
  
  return converted;
}

int32_t
u_scanf_register_handler (UChar            spec, 
              u_scanf_info         info,
              u_scanf_handler     handler)
{
  /* lock the cache */
  umtx_lock(0);

  /* add to our list of function pointers */
  g_u_scanf_infos[ (unsigned char) spec ]     = info;
  g_u_scanf_handlers[ (unsigned char) spec ]     = handler;

  /* unlock the cache */
  umtx_unlock(0);
  return 0;
}

int32_t
u_scanf_skip_leading_ws(UFILE     *stream,
            UChar     pad)
{
  UChar     c;
  int32_t    count = 0;
  
  /* skip all leading ws in the stream */
  while( ((c = u_fgetc(stream)) != 0xFFFF) && (c == pad || ufmt_isws(c)) )
    ++count;
  
  /* put the final character back on the stream */  
  if(c != 0xFFFF)
    u_fungetc(c, stream);

  return count;
}

int32_t 
u_scanf_simple_percent_info(const u_scanf_spec_info    *info,
                int32_t             *argtypes,
                int32_t             n)
{
  /* we don't need any arguments */
  return 0;
}

int32_t 
u_scanf_simple_percent_handler(UFILE            *stream,
                   const u_scanf_spec_info     *info,
                   ufmt_args             *args,
                   const UChar        *fmt,
                   int32_t            *consumed)
{
  /* make sure the next character in the stream is a percent */
  if(u_fgetc(stream) != 0x0025)
    return -1;
  else
    return 0;
}

int32_t 
u_scanf_string_info(const u_scanf_spec_info     *info,
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
u_scanf_string_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args             *args,
               const UChar        *fmt,
               int32_t            *consumed)
{
  UChar     c;
  int32_t     count;
  const UChar     *source;
  UConverter     *conv;
  UErrorCode     status     = ZERO_ERROR;
  char         *arg     = (char*)(args[0].ptrValue);
  char         *alias     = arg;
  char         *limit;
  
  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);
  
  /* get the string one character at a time, truncating to the width */
  count = 0;

  /* open the default converter */
  conv = ucnv_open(ucnv_getDefaultName(), &status);

  if(FAILURE(status))
    return -1;

  /* since there is no real limit, just use a reasonable value */
  limit = alias + 2048;

  while( ((c = u_fgetc(stream)) != 0xFFFF) && 
     (c != info->fPadChar && ! ufmt_isws(c)) &&
     (info->fWidth == -1 || count < info->fWidth) ) {
    
    /* put the character from the stream onto the target */
    source = &c;

    /* convert the character to the default codepage */
    ucnv_fromUnicode(conv, &alias, limit, &source, source + 1,
             NULL, TRUE, &status);
    
    if(FAILURE(status))
      return -1;
    
    /* increment the count */
    ++count;
  }

  /* put the final character we read back on the stream */  
  if(c != 0xFFFF)
    u_fungetc(c, stream);
  
  /* add the terminator */
  *alias = 0x00;

  /* clean up */
  ucnv_close(conv);

  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_ustring_info(const u_scanf_spec_info     *info,
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
u_scanf_ustring_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  UChar     c;
  int32_t     count;
  UChar     *arg     = (UChar*)(args[0].ptrValue);
  UChar     *alias     = arg;
  
  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);
  
  /* get the string one character at a time, truncating to the width */
  count = 0;

  while( ((c = u_fgetc(stream)) != 0xFFFF) && 
     (c != info->fPadChar && ! ufmt_isws(c)) &&
     (info->fWidth == -1 || count < info->fWidth) ) {
    
    /* put the character from the stream onto the target */
    *alias++ = c;
    
    /* increment the count */
    ++count;
  }

  /* put the final character we read back on the stream */  
  if(c != 0xFFFF)
    u_fungetc(c, stream);
  
  /* add the terminator */
  *alias = 0x0000;

  /* we converted 1 arg */
  return 1;
}


int32_t 
u_scanf_count_info(const u_scanf_spec_info     *info,
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
u_scanf_count_handler(UFILE             *stream,
              const u_scanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed)
{
  int *converted = (int*)(args[0].ptrValue);
  
  /* in the special case of count, the u_scanf_spec_info's width */
  /* will contain the # of items converted thus far */
  *converted = info->fWidth;

  /* we converted 0 args */
  return 0;
}

int32_t 
u_scanf_integer_info(const u_scanf_spec_info     *info,
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
u_scanf_integer_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  int32_t        len;
  long            *num         = (long*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getNumberFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parse(format, stream->fUCPos, len, &parsePos, &status);

  /* mask off any necessary bits */
  if(info->fIsShort)
    *num &= SHRT_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    *num &= INT_MAX;

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_double_info(const u_scanf_spec_info     *info,
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
u_scanf_double_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getNumberFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, stream->fUCPos, len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_scientific_info(const u_scanf_spec_info     *info,
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
u_scanf_scientific_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args           *args,
               const UChar            *fmt,
               int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getScientificFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, stream->fUCPos, len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_scidbl_info(const u_scanf_spec_info     *info,
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
u_scanf_scidbl_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *scientificFormat, *genericFormat;
  int32_t        scientificResult, genericResult;
  int32_t        scientificParsePos = 0, genericParsePos = 0;
  UErrorCode         scientificStatus = ZERO_ERROR;
  UErrorCode         genericStatus = ZERO_ERROR;
  bool_t        useScientific;


  /* since we can't determine by scanning the characters whether */
  /* a number was formatted in the 'f' or 'g' styles, parse the */
  /* string with both formatters, and assume whichever one */
  /* parsed the most is the correct formatter to use */


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatters */
  scientificFormat = u_locbund_getScientificFormat(stream->fBundle);
  genericFormat = u_locbund_getNumberFormat(stream->fBundle);
  
  /* handle error */
  if(scientificFormat == 0 || genericFormat == 0)
    return 0;
  
  /* parse the number using each format*/

  scientificResult = unum_parseDouble(scientificFormat, stream->fUCPos, len,
                      &scientificParsePos, &scientificStatus);

  genericResult = unum_parseDouble(genericFormat, stream->fUCPos, len,
                   &genericParsePos, &genericStatus);

  /* determine which parse made it farther */
  useScientific = scientificParsePos > genericParsePos;
  
  /* stash the result in num */
  *num = useScientific ? scientificResult : genericResult;
  
  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += useScientific ? scientificParsePos : genericParsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_currency_info(const u_scanf_spec_info     *info,
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
u_scanf_currency_handler(UFILE                 *stream,
             const u_scanf_spec_info     *info,
             ufmt_args            *args,
             const UChar            *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getCurrencyFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, stream->fUCPos, len, &parsePos, &status);
  
  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_percent_info(const u_scanf_spec_info     *info,
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
u_scanf_percent_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getPercentFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, stream->fUCPos, len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_date_info(const u_scanf_spec_info     *info,
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
u_scanf_date_handler(UFILE             *stream,
             const u_scanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  UDate            *date         = (UDate*) (args[0].ptrValue);
  UDateFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getDateFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *date = udat_parse(format, stream->fUCPos, len, &parsePos, &status);
  
  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_time_info(const u_scanf_spec_info     *info,
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
u_scanf_time_handler(UFILE             *stream,
             const u_scanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  UDate            *time         = (UDate*) (args[0].ptrValue);
  UDateFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getTimeFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *time = udat_parse(format, stream->fUCPos, len, &parsePos, &status);
  
  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_char_info(const u_scanf_spec_info     *info,
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
u_scanf_char_handler(UFILE             *stream,
             const u_scanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed)
{
  UChar uc;
  char *result;
  char *c = (char*)(args[0].ptrValue);
  
  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);
  
  /* get the character from the stream, truncating to the width */
  if(info->fWidth == -1 || info->fWidth > 1)
    uc = u_fgetc(stream);

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
u_scanf_uchar_info(const u_scanf_spec_info     *info,
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
u_scanf_uchar_handler(UFILE             *stream,
              const u_scanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed)
{
  UChar *c = (UChar*)(args[0].ptrValue);
  
  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);
  
  /* get the character from the stream, truncating to the width */
  if(info->fWidth == -1 || info->fWidth > 1)
    *c = u_fgetc(stream);

  /* handle EOF */
  if(*c == 0xFFFF)
    return -1;

  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_spellout_info(const u_scanf_spec_info     *info,
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
u_scanf_spellout_handler(UFILE                 *stream,
             const u_scanf_spec_info     *info,
             ufmt_args             *args,
             const UChar            *fmt,
             int32_t            *consumed)
{
  int32_t        len;
  double        *num         = (double*) (args[0].ptrValue);
  UNumberFormat        *format;
  int32_t        parsePos     = 0;
  UErrorCode         status         = ZERO_ERROR;


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* get the formatter */
  format = u_locbund_getSpelloutFormat(stream->fBundle);
  
  /* handle error */
  if(format == 0)
    return 0;
  
  /* parse the number */
  *num = unum_parseDouble(format, stream->fUCPos, len, &parsePos, &status);

  /* mask off any necessary bits */
  /*  if(! info->fIsLong_double)
      num &= DBL_MAX;*/

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += parsePos;
  
  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_hex_info(const u_scanf_spec_info     *info,
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
u_scanf_hex_handler(UFILE             *stream,
            const u_scanf_spec_info     *info,
            ufmt_args            *args,
            const UChar            *fmt,
            int32_t            *consumed)
{
  int32_t        len;
  long            *num         = (long*) (args[0].ptrValue);


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* check for alternate form */
  if( *(stream->fUCPos) == 0x0030 && 
      (*(stream->fUCPos + 1) == 0x0078 || *(stream->fUCPos + 1) == 0x0058) ) {

    /* skip the '0' and 'x' or 'X' if present */
    stream->fUCPos += 2;
    len -= 2;
  }

  /* parse the number */
  *num = ufmt_utol(stream->fUCPos, &len, 16);

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += len;

  /* mask off any necessary bits */
  if(info->fIsShort)
    *num &= SHRT_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    *num &= INT_MAX;

  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_octal_info(const u_scanf_spec_info    *info,
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
u_scanf_octal_handler(UFILE             *stream,
              const u_scanf_spec_info     *info,
              ufmt_args         *args,
              const UChar        *fmt,
              int32_t            *consumed)
{
  int32_t        len;
  long            *num         = (long*) (args[0].ptrValue);


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* parse the number */
  *num = ufmt_utol(stream->fUCPos, &len, 8);

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += len;

  /* mask off any necessary bits */
  if(info->fIsShort)
    *num &= SHRT_MAX;
  else if(! info->fIsLong || ! info->fIsLongLong)
    *num &= INT_MAX;

  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_pointer_info(const u_scanf_spec_info     *info,
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
u_scanf_pointer_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  int32_t    len;
  void        *p     = (void*)(args[0].ptrValue);


  /* skip all ws in the stream */
  u_scanf_skip_leading_ws(stream, info->fPadChar);

  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

  /* truncate to the width, if specified */
  if(info->fWidth != -1)
    len = ufmt_min(len, info->fWidth);

  /* parse the pointer - cast to void** to assign to *p */
  *(void**)p = (void*) ufmt_utol(stream->fUCPos, &len, 16);

  /* update the stream's position to reflect consumed data */
  stream->fUCPos += len;

  /* we converted 1 arg */
  return 1;
}

int32_t 
u_scanf_scanset_info(const u_scanf_spec_info     *info,
             int32_t             *argtypes,
             int32_t             n)
{
  /* handle error */
  if(n < 1)
    return 0;

  /* we need 1 argument of type char* */
  argtypes[0] = ufmt_string;
  return 1;
}

int32_t
u_scanf_scanset_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed)
{
  u_scanf_scanset    scanset;
  int32_t        len;
  bool_t        success;
  UChar            c;
  const UChar         *source;
  UConverter         *conv;
  UErrorCode         status     = ZERO_ERROR;
  char            *s     = (char*) (args[0].ptrValue);
  char             *alias, *limit;


  /* fill the stream's internal buffer */
  ufile_fill_uchar_buffer(stream);

  /* determine the size of the stream's buffer */
  len = stream->fUCLimit - stream->fUCPos;

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
  if(! success || FAILURE(status))
    return -1;

  /* grab characters one at a time and make sure they are in the scanset */
  while( (c = u_fgetc(stream)) != 0xFFFF && alias < limit) {
    if(u_scanf_scanset_in(&scanset, c)) {
      source = &c;
      /* convert the character to the default codepage */
      ucnv_fromUnicode(conv, &alias, limit, &source, source + 1,
               NULL, TRUE, &status);
      
      if(FAILURE(status))
    return -1;
    }
    /* if the character's not in the scanset, break out */
    else {
      break;
    }
  }

  /* put the final character we read back on the stream */  
  if(c != 0xFFFF)
    u_fungetc(c, stream);

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

void
u_scanf_init()
{
  int32_t i;
  /*Mutex *lock;*/
  
  /* if we're already inited, do nothing */
  if(g_u_scanf_inited)
    return;

  /* lock the cache */
  umtx_lock(0);

  /* if we're already inited, do nothing */
  if(g_u_scanf_inited) {
    umtx_unlock(0);
    return;
  }

  /* initialize all handlers and infos to 0 */
  for(i = 0; i < 256; ++i) {
    g_u_scanf_infos[i]         = 0;
    g_u_scanf_handlers[i]     = 0;
  }

  /* register the handlers for standard specifiers */
  /* don't use u_scanf_register_handler to avoid mutex creation */

  /* handle '%' */
  g_u_scanf_infos[ 0x0025 ]     = u_scanf_simple_percent_info;
  g_u_scanf_handlers[ 0x0025 ]     = u_scanf_simple_percent_handler;

  /* handle 's' */
  g_u_scanf_infos[ 0x0073 ]     = u_scanf_string_info;
  g_u_scanf_handlers[ 0x0073 ]     = u_scanf_string_handler;

  /* handle 'U' */
  g_u_scanf_infos[ 0x0055 ]     = u_scanf_ustring_info;
  g_u_scanf_handlers[ 0x0055 ]     = u_scanf_ustring_handler;

  /* handle 'n' */
  g_u_scanf_infos[ 0x006E ]     = u_scanf_count_info;
  g_u_scanf_handlers[ 0x006E ]     = u_scanf_count_handler;

  /* handle 'd' */
  g_u_scanf_infos[ 0x0064 ]     = u_scanf_integer_info;
  g_u_scanf_handlers[ 0x0064 ]     = u_scanf_integer_handler;

  /* handle 'i' */
  g_u_scanf_infos[ 0x0069 ]     = u_scanf_integer_info;
  g_u_scanf_handlers[ 0x0069 ]     = u_scanf_integer_handler;

  /* handle 'u' */
  g_u_scanf_infos[ 0x0075 ]     = u_scanf_integer_info;
  g_u_scanf_handlers[ 0x0075 ]     = u_scanf_integer_handler;

  /* handle 'f' */
  g_u_scanf_infos[ 0x0066 ]     = u_scanf_double_info;
  g_u_scanf_handlers[ 0x0066 ]     = u_scanf_double_handler;

  /* handle 'e' */
  g_u_scanf_infos[ 0x0065 ]     = u_scanf_scientific_info;
  g_u_scanf_handlers[ 0x0065 ]     = u_scanf_scientific_handler;

  /* handle 'E' */
  g_u_scanf_infos[ 0x0045 ]     = u_scanf_scientific_info;
  g_u_scanf_handlers[ 0x0045 ]     = u_scanf_scientific_handler;

  /* handle 'g' */
  g_u_scanf_infos[ 0x0067 ]     = u_scanf_scidbl_info;
  g_u_scanf_handlers[ 0x0067 ]     = u_scanf_scidbl_handler;

  /* handle 'G' */
  g_u_scanf_infos[ 0x0047 ]     = u_scanf_scidbl_info;
  g_u_scanf_handlers[ 0x0047 ]     = u_scanf_scidbl_handler;

  /* handle 'M' */
  g_u_scanf_infos[ 0x004D ]     = u_scanf_currency_info;
  g_u_scanf_handlers[ 0x004D ]     = u_scanf_currency_handler;

  /* handle 'P' */
  g_u_scanf_infos[ 0x0050 ]     = u_scanf_percent_info;
  g_u_scanf_handlers[ 0x0050 ]     = u_scanf_percent_handler;

  /* handle 'D' */
  g_u_scanf_infos[ 0x0044 ]     = u_scanf_date_info;
  g_u_scanf_handlers[ 0x0044 ]     = u_scanf_date_handler;

  /* handle 'T' */
  g_u_scanf_infos[ 0x0054 ]     = u_scanf_time_info;
  g_u_scanf_handlers[ 0x0054 ]     = u_scanf_time_handler;

  /* handle 'c' */
  g_u_scanf_infos[ 0x0063 ]     = u_scanf_char_info;
  g_u_scanf_handlers[ 0x0063 ]     = u_scanf_char_handler;

  /* handle 'K' */
  g_u_scanf_infos[ 0x004B ]     = u_scanf_uchar_info;
  g_u_scanf_handlers[ 0x004B ]     = u_scanf_uchar_handler;

  /* handle 'V' */
  g_u_scanf_infos[ 0x0056 ]     = u_scanf_spellout_info;
  g_u_scanf_handlers[ 0x0056 ]     = u_scanf_spellout_handler;

  /* handle 'x' */
  g_u_scanf_infos[ 0x0078 ]     = u_scanf_hex_info;
  g_u_scanf_handlers[ 0x0078 ]     = u_scanf_hex_handler;

  /* handle 'X' */
  g_u_scanf_infos[ 0x0058 ]     = u_scanf_hex_info;
  g_u_scanf_handlers[ 0x0058 ]     = u_scanf_hex_handler;

  /* handle 'o' */
  g_u_scanf_infos[ 0x006F ]     = u_scanf_octal_info;
  g_u_scanf_handlers[ 0x006F ]     = u_scanf_octal_handler;

  /* handle 'p' */
  g_u_scanf_infos[ 0x0070 ]     = u_scanf_pointer_info;
  g_u_scanf_handlers[ 0x0070 ]     = u_scanf_pointer_handler;

  /* handle '[' */
  g_u_scanf_infos[ 0x005B ]     = u_scanf_scanset_info;
  g_u_scanf_handlers[ 0x005B ]     = u_scanf_scanset_handler;

  /* we're finished */
  g_u_scanf_inited = TRUE;

  /* unlock the cache */
  umtx_unlock(0);
}


#define U_SCANF_MAX_ARGS 32
#define UP_PERCENT 0x0025

int32_t 
u_vfscanf_u(    UFILE        *f,
        const UChar    *patternSpecification,
        va_list        ap)
{
  u_scanf_spec         spec;
  const UChar         *alias;
  int32_t         count, converted, temp;

  int32_t         num_args_wanted;
  int32_t         ufmt_types     [U_SCANF_MAX_ARGS];
  ufmt_args       args         [U_SCANF_MAX_ARGS];

  u_scanf_info        info;
  u_scanf_handler    handler;

  int32_t         cur_arg;


  /* init our function tables */
  if(! g_u_scanf_inited)
    u_scanf_init();

  /* alias the pattern */
  alias = patternSpecification;
  
  /* haven't converted anything yet */
  converted = 0;

  /* iterate through the pattern */
  for(;;) {
    
    /* match any characters up to the next '%' */
    while(*alias != UP_PERCENT && *alias != 0x0000 && u_fgetc(f) == *alias) {
      *alias++;
    }

    /* if we aren't at a '%', or if we're at end of string, break*/
    if(*alias != UP_PERCENT || *alias == 0x0000)
      break;
    
    /* parse the specifier */
    count = u_scanf_parse_spec(alias, &spec);
    
    /* update the pointer in pattern */
    alias += count;
    
    /* skip the argument, if necessary */
    if(spec.fSkipArg)
      va_arg(ap, int);
    
    /* query the info function for argument information */
    info = g_u_scanf_infos[ (unsigned char) spec.fInfo.fSpec ];
    if(info != 0) { 
      num_args_wanted = (*info)(&spec.fInfo, 
                ufmt_types,
                U_SCANF_MAX_ARGS);
    }
    else
      num_args_wanted = 0;

    /* fill in the requested arguments */
    for(cur_arg = 0; 
    cur_arg < num_args_wanted && cur_arg < U_SCANF_MAX_ARGS; 
    ++cur_arg) {
      
      switch(ufmt_types[cur_arg]) {

      case ufmt_count:
    args[cur_arg].intValue = va_arg(ap, int);
    /* set the spec's width to the # of items converted */
    spec.fInfo.fWidth = converted;
    break;

      case ufmt_int:
    args[cur_arg].ptrValue = va_arg(ap, int*);
    break;
    
      case ufmt_char:
    args[cur_arg].ptrValue = va_arg(ap, int*);
    break;
    
      case ufmt_wchar:
    args[cur_arg].ptrValue = va_arg(ap, wchar_t*);
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
    args[cur_arg].ptrValue = va_arg(ap, float*);
    break;
    
      case ufmt_double:
    args[cur_arg].ptrValue = va_arg(ap, double*);
    break;

      case ufmt_date:
    args[cur_arg].ptrValue = va_arg(ap, UDate*);
    break;

      case ufmt_ustring:
    args[cur_arg].ptrValue = va_arg(ap, UChar*);
    break;

      case ufmt_uchar:
    args[cur_arg].ptrValue = va_arg(ap, int*);
    break;
      }
    }
    
    /* call the handler function */
    handler = g_u_scanf_handlers[ (unsigned char) spec.fInfo.fSpec ];
    if(handler != 0) {

      /* reset count */
      count = 0;

      temp = (*handler)(f, &spec.fInfo, args, alias, &count);

      /* if the handler encountered an error condition, break */
      if(temp == -1)
    break;

      /* add to the # of items converted */
      converted += temp;

      /* update the pointer in pattern */
      alias += count;
    }

    /* just ignore unknown tags */

  }

  /* return # of items converted */
  return converted;
}

