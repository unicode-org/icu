/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File uscanf.c
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/uchar.h"

#include "uscanf.h"
#include "uscanf_p.h"
#include "uscanset.h"
#include "unicode/ustdio.h"
#include "ufile.h"
#include "unicode/ustring.h"
#include "locbund.h"
#include "unicode/unum.h"
#include "unicode/udat.h"

#include "cmemory.h"
#include "ustr_imp.h"

/* --- Prototypes ---------------------------- */

int32_t 
u_scanf_simple_percent_handler(UFILE            *stream,
                   const u_scanf_spec_info     *info,
                   ufmt_args             *args,
                   const UChar        *fmt,
                   int32_t            *consumed);

int32_t
u_scanf_ustring_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_scanf_count_handler(UFILE             *stream,
              const u_scanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed);

int32_t
u_scanf_integer_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_scanf_uinteger_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_scanf_double_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed);

int32_t
u_scanf_scientific_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args           *args,
               const UChar            *fmt,
               int32_t            *consumed);

int32_t
u_scanf_scidbl_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args        *args,
               const UChar        *fmt,
               int32_t            *consumed);

int32_t
u_scanf_currency_handler(UFILE                 *stream,
             const u_scanf_spec_info     *info,
             ufmt_args            *args,
             const UChar            *fmt,
             int32_t            *consumed);

int32_t
u_scanf_percent_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_scanf_date_handler(UFILE             *stream,
             const u_scanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed);

int32_t
u_scanf_time_handler(UFILE             *stream,
             const u_scanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed);

int32_t
u_scanf_char_handler(UFILE             *stream,
             const u_scanf_spec_info     *info,
             ufmt_args        *args,
             const UChar        *fmt,
             int32_t            *consumed);

int32_t
u_scanf_uchar_handler(UFILE             *stream,
              const u_scanf_spec_info     *info,
              ufmt_args        *args,
              const UChar        *fmt,
              int32_t            *consumed);

int32_t
u_scanf_spellout_handler(UFILE                 *stream,
             const u_scanf_spec_info     *info,
             ufmt_args             *args,
             const UChar            *fmt,
             int32_t            *consumed);

int32_t
u_scanf_hex_handler(UFILE             *stream,
            const u_scanf_spec_info     *info,
            ufmt_args            *args,
            const UChar            *fmt,
            int32_t            *consumed);

int32_t
u_scanf_octal_handler(UFILE             *stream,
              const u_scanf_spec_info     *info,
              ufmt_args         *args,
              const UChar        *fmt,
              int32_t            *consumed);

int32_t
u_scanf_pointer_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

int32_t
u_scanf_string_handler(UFILE             *stream,
               const u_scanf_spec_info     *info,
               ufmt_args             *args,
               const UChar        *fmt,
               int32_t            *consumed);

int32_t
u_scanf_scanset_handler(UFILE             *stream,
            const u_scanf_spec_info *info,
            ufmt_args        *args,
            const UChar        *fmt,
            int32_t            *consumed);

/* ANSI style formatting */
/* Use US-ASCII characters only for formatting */

/* % */
#define UFMT_SIMPLE_PERCENT {ufmt_simple_percent, u_scanf_simple_percent_handler}
/* s */
#define UFMT_STRING         {ufmt_string, u_scanf_string_handler}
/* c */
#define UFMT_CHAR           {ufmt_string, u_scanf_char_handler}
/* d, i */
#define UFMT_INT            {ufmt_int, u_scanf_integer_handler}
/* u */
#define UFMT_UINT           {ufmt_int, u_scanf_uinteger_handler}
/* o */
#define UFMT_OCTAL          {ufmt_int, u_scanf_octal_handler}
/* x, X */
#define UFMT_HEX            {ufmt_int, u_scanf_hex_handler}
/* f */
#define UFMT_DOUBLE         {ufmt_double, u_scanf_double_handler}
/* e, E */
#define UFMT_SCIENTIFIC     {ufmt_double, u_scanf_scientific_handler}
/* g, G */
#define UFMT_SCIDBL         {ufmt_double, u_scanf_scidbl_handler}
/* n */
#define UFMT_COUNT          {ufmt_count, u_scanf_count_handler}
/* [ */
#define UFMT_SCANSET        {ufmt_string, u_scanf_scanset_handler} /* TODO: Is this also suppose to be ufmt_ustring */

/* non-ANSI extensions */
/* Use US-ASCII characters only for formatting */

/* p */
#define UFMT_POINTER        {ufmt_pointer, u_scanf_pointer_handler}
/* D */
#define UFMT_DATE           {ufmt_date, u_scanf_date_handler}
/* T */
#define UFMT_TIME           {ufmt_date, u_scanf_time_handler}
/* V */
#define UFMT_SPELLOUT       {ufmt_double, u_scanf_spellout_handler}
/* P */
#define UFMT_PERCENT        {ufmt_double, u_scanf_percent_handler}
/* M */
#define UFMT_CURRENCY       {ufmt_double, u_scanf_currency_handler}
/* K */
#define UFMT_UCHAR          {ufmt_uchar, u_scanf_uchar_handler}
/* U */
#define UFMT_USTRING        {ufmt_ustring, u_scanf_ustring_handler}


#define UFMT_EMPTY {ufmt_empty, NULL}

struct u_scanf_info {
    ufmt_type_info info;
    u_scanf_handler handler;
};
typedef struct u_scanf_info u_scanf_info;

/* Use US-ASCII characters only for formatting. Most codepages have
 characters 20-7F from Unicode. Using any other codepage specific
 characters will make it very difficult to format the string on
 non-Unicode machines */
static const u_scanf_info g_u_scanf_infos[108] = {
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

#define USCANF_NUM_FMT_HANDLERS sizeof(g_u_scanf_infos)

/* We do not use handlers for 0-0x1f */
#define USCANF_BASE_FMT_HANDLERS 0x20

int32_t 
u_fscanf(UFILE        *f,
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
u_fscanf_u(UFILE        *f,
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

U_CAPI int32_t  U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_vfscanf(UFILE        *f,
          const char    *patternSpecification,
          va_list        ap)
{
    int32_t converted;
    UChar *pattern;
    UChar buffer[UFMT_DEFAULT_BUFFER_SIZE];
    int32_t size = (int32_t)strlen(patternSpecification) + 1;

    /* convert from the default codepage to Unicode */
    if (size >= MAX_UCHAR_BUFFER_SIZE(buffer)) {
        pattern = (UChar *)uprv_malloc(size * sizeof(UChar));
        if(pattern == 0) {
            return 0;
        }
    }
    else {
        pattern = buffer;
    }
    ufmt_defaultCPToUnicode(patternSpecification, size, pattern, size);

    /* do the work */
    converted = u_vfscanf_u(f, pattern, ap);

    /* clean up */
    if (pattern != buffer) {
        uprv_free(pattern);
    }

    return converted;
}

static int32_t
u_scanf_skip_leading_ws(UFILE     *stream,
                        UChar     pad)
{
    UChar     c;
    int32_t    count = 0;

    /* skip all leading ws in the stream */
    while( ((c = u_fgetc(stream)) != U_EOF) && (c == pad || u_isWhitespace(c)) )
        ++count;

    /* put the final character back on the stream */
    if(c != U_EOF)
        u_fungetc(c, stream);

    return count;
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
    UErrorCode     status     = U_ZERO_ERROR;
    char         *arg     = (char*)(args[0].ptrValue);
    char         *alias     = arg;
    char         *limit;

    /* skip all ws in the stream */
    u_scanf_skip_leading_ws(stream, info->fPadChar);

    /* get the string one character at a time, truncating to the width */
    count = 0;

    /* open the default converter */
    conv = u_getDefaultConverter(&status);

    if(U_FAILURE(status))
        return -1;

    while( ((c = u_fgetc(stream)) != U_EOF) &&
        (c != info->fPadChar && !u_isWhitespace(c)) &&
        (info->fWidth == -1 || count < info->fWidth) )
    {

        /* put the character from the stream onto the target */
        source = &c;
        /* Since we do this one character at a time, do it this way. */
        limit = alias + ucnv_getMaxCharSize(conv);

        /* convert the character to the default codepage */
        ucnv_fromUnicode(conv, &alias, limit, &source, source + 1,
            NULL, TRUE, &status);

        if(U_FAILURE(status)) {
            /* clean up */
            u_releaseDefaultConverter(conv);
            return -1;
        }

        /* increment the count */
        ++count;
    }

    /* put the final character we read back on the stream */
    if(c != U_EOF)
        u_fungetc(c, stream);

    /* add the terminator */
    *alias = 0x00;

    /* clean up */
    u_releaseDefaultConverter(conv);

    /* we converted 1 arg */
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

    while( ((c = u_fgetc(stream)) != U_EOF) &&
        (c != info->fPadChar && ! u_isWhitespace(c)) &&
        (info->fWidth == -1 || count < info->fWidth) ) {

        /* put the character from the stream onto the target */
        *alias++ = c;

        /* increment the count */
        ++count;
    }

    /* put the final character we read back on the stream */
    if(c != U_EOF)
        u_fungetc(c, stream);

    /* add the terminator */
    *alias = 0x0000;

    /* we converted 1 arg */
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
    UErrorCode         status         = U_ZERO_ERROR;


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
        *num &= UINT16_MAX;
    else if(! info->fIsLong || ! info->fIsLongLong)
        *num &= UINT32_MAX;

    /* update the stream's position to reflect consumed data */
    stream->fUCPos += parsePos;

    /* we converted 1 arg */
    return 1;
}

int32_t
u_scanf_uinteger_handler(UFILE             *stream,
                         const u_scanf_spec_info *info,
                         ufmt_args        *args,
                         const UChar        *fmt,
                         int32_t            *consumed)
{
    ufmt_args uint_args;
    int32_t converted_args;
    uint32_t            *num         = (uint32_t*) (args[0].ptrValue);
    double currDouble;

    uint_args.ptrValue = &currDouble;
    converted_args = u_scanf_double_handler(stream, info, &uint_args, fmt, consumed);

    *num = (uint32_t)currDouble;

    return converted_args;
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
    UErrorCode         status         = U_ZERO_ERROR;


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
    UErrorCode         status         = U_ZERO_ERROR;


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
u_scanf_scidbl_handler(UFILE             *stream,
                       const u_scanf_spec_info     *info,
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
    UBool         useScientific;


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
    useScientific = (UBool)(scientificParsePos > genericParsePos);

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
    UErrorCode         status         = U_ZERO_ERROR;


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
    UErrorCode         status         = U_ZERO_ERROR;


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
    UErrorCode         status         = U_ZERO_ERROR;


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
    UErrorCode         status         = U_ZERO_ERROR;


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
u_scanf_char_handler(UFILE             *stream,
                     const u_scanf_spec_info     *info,
                     ufmt_args        *args,
                     const UChar        *fmt,
                     int32_t            *consumed)
{
    UChar uc = 0;
    char *result;
    char *c = (char*)(args[0].ptrValue);

    /* skip all ws in the stream */
    u_scanf_skip_leading_ws(stream, info->fPadChar);

    /* get the character from the stream, truncating to the width */
    if(info->fWidth == -1 || info->fWidth > 1)
        uc = u_fgetc(stream);

    /* handle EOF */
    if(uc == U_EOF)
        return -1;

    /* convert the character to the default codepage */
    result = ufmt_unicodeToDefaultCP(&uc, 1);
    *c = result[0];

    /* clean up */
    uprv_free(result);

    /* we converted 1 arg */
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
    if(*c == U_EOF)
        return -1;

    /* we converted 1 arg */
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
    UErrorCode         status         = U_ZERO_ERROR;


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
        *num &= UINT16_MAX;
    else if(! info->fIsLong || ! info->fIsLongLong)
        *num &= UINT32_MAX;

    /* we converted 1 arg */
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
        *num &= UINT16_MAX;
    else if(! info->fIsLong || ! info->fIsLongLong)
        *num &= UINT32_MAX;

    /* we converted 1 arg */
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
u_scanf_scanset_handler(UFILE             *stream,
                        const u_scanf_spec_info *info,
                        ufmt_args        *args,
                        const UChar        *fmt,
                        int32_t            *consumed)
{
    u_scanf_scanset scanset;
    int32_t         len;
    UBool           success;
    UChar           c;
    UChar           *s     = (UChar*) (args[0].ptrValue);
    UChar           *alias, *limit;


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

    /* verify that the parse was successful and the converter opened */
    if(! success)
        return -1;

    /* grab characters one at a time and make sure they are in the scanset */
    while( (c = u_fgetc(stream)) != U_EOF && alias < limit) {
        if(u_scanf_scanset_in(&scanset, c)) {
            *(alias++) = c;
        }
        else {
            /* if the character's not in the scanset, break out */
            break;
        }
    }

    /* put the final character we read back on the stream */
    if(c != U_EOF)
        u_fungetc(c, stream);

    /* if we didn't match at least 1 character, fail */
    if(alias == s)
        return -1;
    /* otherwise, add the terminator */
    else
        *alias = 0x00;

    /* we converted 1 arg */
    return 1;
}


#define UP_PERCENT 0x0025

U_CAPI int32_t  U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_vfscanf_u(UFILE        *f,
            const UChar    *patternSpecification,
            va_list        ap)
{
    u_scanf_spec         spec;
    const UChar         *alias;
    int32_t         count, converted, temp;
    uint16_t      handlerNum;

    ufmt_args       args;

    ufmt_type_info    info;
    u_scanf_handler handler;

    /* alias the pattern */
    alias = patternSpecification;

    /* haven't converted anything yet */
    converted = 0;

    /* iterate through the pattern */
    for(;;) {

        /* match any characters up to the next '%' */
        while(*alias != UP_PERCENT && *alias != 0x0000 && u_fgetc(f) == *alias) {
            alias++;
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
            args.ptrValue = va_arg(ap, int*);

        handlerNum = (uint16_t)(spec.fInfo.fSpec - USCANF_BASE_FMT_HANDLERS);
        if (handlerNum < USCANF_NUM_FMT_HANDLERS) {
            /* query the info function for argument information */
            info = g_u_scanf_infos[ handlerNum ].info;
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
            handler = g_u_scanf_infos[ handlerNum ].handler;
            if(handler != 0) {

                /* reset count */
                count = 0;

                temp = (*handler)(f, &spec.fInfo, &args, alias, &count);

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

#endif /* #if !UCONFIG_NO_FORMATTING */
