/*
******************************************************************************
*
*   Copyright (C) 1998-2004, International Business Machines
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

#include "unicode/ustdio.h"
#include "unicode/ustring.h"
#include "unicode/unum.h"
#include "unicode/udat.h"
#include "unicode/uset.h"
#include "uscanf.h"
#include "ufile.h"
#include "uscanf_p.h"
#include "locbund.h"

#include "cmemory.h"
#include "ustr_imp.h"

#define UP_PERCENT 0x0025


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
/* V */
#define UFMT_SPELLOUT       {ufmt_double, u_scanf_spellout_handler}
/* P */
#define UFMT_PERCENT        {ufmt_double, u_scanf_percent_handler}
/* C  K is old format */
#define UFMT_UCHAR          {ufmt_uchar, u_scanf_uchar_handler}
/* S  U is old format */
#define UFMT_USTRING        {ufmt_ustring, u_scanf_ustring_handler}


#define UFMT_EMPTY {ufmt_empty, NULL}

/**
 * A u_scanf handler function.  
 * A u_scanf handler is responsible for handling a single u_scanf 
 * format specification, for example 'd' or 's'.
 * @param stream The UFILE to which to write output.
 * @param info A pointer to a <TT>u_scanf_spec_info</TT> struct containing
 * information on the format specification.
 * @param args A pointer to the argument data
 * @param fmt A pointer to the first character in the format string
 * following the spec.
 * @param consumed On output, set to the number of characters consumed
 * in <TT>fmt</TT>.
 * @return The number of arguments converted and assigned, or -1 if an
 * error occurred.
 */
typedef int32_t (*u_scanf_handler) (UFILE  *stream,
                   const u_scanf_spec_info     *info,
                   ufmt_args  *args,
                   const UChar            *fmt,
                   int32_t            *consumed);

typedef struct u_scanf_info {
    ufmt_type_info info;
    u_scanf_handler handler;
} u_scanf_info;

#define USCANF_NUM_FMT_HANDLERS 108

/* We do not use handlers for 0-0x1f */
#define USCANF_BASE_FMT_HANDLERS 0x20


static int32_t
u_scanf_skip_leading_ws(UFILE     *input,
                        UChar     pad)
{
    UChar     c;
    int32_t    count = 0;

    /* skip all leading ws in the input */
    while( ((c = u_fgetc(input)) != U_EOF) && (c == pad || u_isWhitespace(c)) )
    {
        count++;
    }

    /* put the final character back on the input */
    if(c != U_EOF)
        u_fungetc(c, input);

    return count;
}

static int32_t 
u_scanf_simple_percent_handler(UFILE            *input,
                               const u_scanf_spec_info     *info,
                               ufmt_args             *args,
                               const UChar        *fmt,
                               int32_t            *consumed)
{
    /* make sure the next character in the input is a percent */
    if(u_fgetc(input) != 0x0025) {
        return -1;
    }
    return 0;
}

static int32_t
u_scanf_string_handler(UFILE             *input,
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

    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* get the string one character at a time, truncating to the width */
    count = 0;

    /* open the default converter */
    conv = u_getDefaultConverter(&status);

    if(U_FAILURE(status))
        return -1;

    while( ((c = u_fgetc(input)) != U_EOF)
        && (c != info->fPadChar && !u_isWhitespace(c))
        && (info->fWidth == -1 || count < info->fWidth) )
    {

        /* put the character from the input onto the target */
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

    /* put the final character we read back on the input */
    if(c != U_EOF)
        u_fungetc(c, input);

    /* add the terminator */
    *alias = 0x00;

    /* clean up */
    u_releaseDefaultConverter(conv);

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_ustring_handler(UFILE             *input,
                        const u_scanf_spec_info *info,
                        ufmt_args        *args,
                        const UChar        *fmt,
                        int32_t            *consumed)
{
    UChar     c;
    int32_t     count;
    UChar     *arg     = (UChar*)(args[0].ptrValue);
    UChar     *alias     = arg;

    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* get the string one character at a time, truncating to the width */
    count = 0;

    while( ((c = u_fgetc(input)) != U_EOF)
        && (c != info->fPadChar && ! u_isWhitespace(c))
        && (info->fWidth == -1 || count < info->fWidth) )
    {

        /* put the character from the input onto the target */
        *alias++ = c;

        /* increment the count */
        ++count;
    }

    /* put the final character we read back on the input */
    if(c != U_EOF)
        u_fungetc(c, input);

    /* add the terminator */
    *alias = 0x0000;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_count_handler(UFILE             *input,
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

static int32_t
u_scanf_double_handler(UFILE             *input,
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


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* get the formatter */
    format = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_DECIMAL);

    /* handle error */
    if(format == 0)
        return 0;

    /* parse the number */
    *num = unum_parseDouble(format, input->str.fPos, len, &parsePos, &status);

    /* mask off any necessary bits */
    /*  if(! info->fIsLong_double)
    num &= DBL_MAX;*/

    /* update the input's position to reflect consumed data */
    input->str.fPos += parsePos;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_scientific_handler(UFILE             *input,
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


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* get the formatter */
    format = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_SCIENTIFIC);

    /* handle error */
    if(format == 0)
        return 0;

    /* parse the number */
    *num = unum_parseDouble(format, input->str.fPos, len, &parsePos, &status);

    /* mask off any necessary bits */
    /*  if(! info->fIsLong_double)
    num &= DBL_MAX;*/

    /* update the input's position to reflect consumed data */
    input->str.fPos += parsePos;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_scidbl_handler(UFILE             *input,
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


    /* since we can't determine by scanning the characters whether */
    /* a number was formatted in the 'f' or 'g' styles, parse the */
    /* string with both formatters, and assume whichever one */
    /* parsed the most is the correct formatter to use */


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* get the formatters */
    scientificFormat = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_SCIENTIFIC);
    genericFormat = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_DECIMAL);

    /* handle error */
    if(scientificFormat == 0 || genericFormat == 0)
        return 0;

    /* parse the number using each format*/

    scientificResult = unum_parseDouble(scientificFormat, input->str.fPos, len,
        &scientificParsePos, &scientificStatus);

    genericResult = unum_parseDouble(genericFormat, input->str.fPos, len,
        &genericParsePos, &genericStatus);

    /* determine which parse made it farther */
    if(scientificParsePos > genericParsePos) {
        /* stash the result in num */
        *num = scientificResult;
        /* update the input's position to reflect consumed data */
        input->str.fPos += scientificParsePos;
    }
    else {
        /* stash the result in num */
        *num = genericResult;
        /* update the input's position to reflect consumed data */
        input->str.fPos += genericParsePos;
    }

    /* mask off any necessary bits */
    /*  if(! info->fIsLong_double)
    num &= DBL_MAX;*/


    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_integer_handler(UFILE             *input,
                        const u_scanf_spec_info *info,
                        ufmt_args        *args,
                        const UChar        *fmt,
                        int32_t            *consumed)
{
    int32_t        len;
    void            *num         = (void*) (args[0].ptrValue);
    UNumberFormat   *format;
    int32_t         parsePos     = 0;
    UErrorCode      status         = U_ZERO_ERROR;
    int64_t         result;


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* get the formatter */
    format = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_DECIMAL);

    /* handle error */
    if(format == 0)
        return 0;

    /* parse the number */
    result = unum_parseInt64(format, input->str.fPos, len, &parsePos, &status);

    /* mask off any necessary bits */
    if (info->fIsShort)
        *(int16_t*)num = (int16_t)(UINT16_MAX & result);
    else if (info->fIsLongLong)
        *(int64_t*)num = result;
    else
        *(int32_t*)num = (int32_t)(UINT32_MAX & result);

    /* update the input's position to reflect consumed data */
    input->str.fPos += parsePos;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_uinteger_handler(UFILE             *input,
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
    converted_args = u_scanf_double_handler(input, info, &uint_args, fmt, consumed);

    *num = (uint32_t)currDouble;

    return converted_args;
}

static int32_t
u_scanf_percent_handler(UFILE             *input,
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


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* get the formatter */
    format = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_PERCENT);

    /* handle error */
    if(format == 0)
        return 0;

    /* parse the number */
    *num = unum_parseDouble(format, input->str.fPos, len, &parsePos, &status);

    /* mask off any necessary bits */
    /*  if(! info->fIsLong_double)
    num &= DBL_MAX;*/

    /* update the input's position to reflect consumed data */
    input->str.fPos += parsePos;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_char_handler(UFILE             *input,
                     const u_scanf_spec_info     *info,
                     ufmt_args        *args,
                     const UChar        *fmt,
                     int32_t            *consumed)
{
    UChar uc = 0;
    char *result;
    char *c = (char*)(args[0].ptrValue);

    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* get the character from the input, truncating to the width */
    if(info->fWidth == -1 || info->fWidth > 1)
        uc = u_fgetc(input);

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

static int32_t
u_scanf_uchar_handler(UFILE             *input,
                      const u_scanf_spec_info     *info,
                      ufmt_args        *args,
                      const UChar        *fmt,
                      int32_t            *consumed)
{
    UChar *c = (UChar*)(args[0].ptrValue);

    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* get the character from the input, truncating to the width */
    if(info->fWidth == -1 || info->fWidth > 1)
        *c = u_fgetc(input);

    /* handle EOF */
    if(*c == U_EOF)
        return -1;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_spellout_handler(UFILE                 *input,
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


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* get the formatter */
    format = u_locbund_getNumberFormat(&input->str.fBundle, UNUM_SPELLOUT);

    /* handle error */
    if(format == 0)
        return 0;

    /* parse the number */
    *num = unum_parseDouble(format, input->str.fPos, len, &parsePos, &status);

    /* mask off any necessary bits */
    /*  if(! info->fIsLong_double)
    num &= DBL_MAX;*/

    /* update the input's position to reflect consumed data */
    input->str.fPos += parsePos;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_hex_handler(UFILE             *input,
                    const u_scanf_spec_info     *info,
                    ufmt_args            *args,
                    const UChar            *fmt,
                    int32_t            *consumed)
{
    int32_t        len;
    void           *num         = (void*) (args[0].ptrValue);
    int64_t        result;

    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* check for alternate form */
    if( *(input->str.fPos) == 0x0030 &&
        (*(input->str.fPos + 1) == 0x0078 || *(input->str.fPos + 1) == 0x0058) ) {

        /* skip the '0' and 'x' or 'X' if present */
        input->str.fPos += 2;
        len -= 2;
    }

    /* parse the number */
    result = ufmt_uto64(input->str.fPos, &len, 16);

    /* update the input's position to reflect consumed data */
    input->str.fPos += len;

    /* mask off any necessary bits */
    if (info->fIsShort)
        *(int16_t*)num = (int16_t)(UINT16_MAX & result);
    else if (info->fIsLongLong)
        *(int64_t*)num = result;
    else
        *(int32_t*)num = (int32_t)(UINT32_MAX & result);

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_octal_handler(UFILE             *input,
                      const u_scanf_spec_info     *info,
                      ufmt_args         *args,
                      const UChar        *fmt,
                      int32_t            *consumed)
{
    int32_t        len;
    void            *num         = (void*) (args[0].ptrValue);
    int64_t         result;

    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* parse the number */
    result = ufmt_uto64(input->str.fPos, &len, 8);

    /* update the input's position to reflect consumed data */
    input->str.fPos += len;

    /* mask off any necessary bits */
    if (info->fIsShort)
        *(int16_t*)num = (int16_t)(UINT16_MAX & result);
    else if (info->fIsLongLong)
        *(int64_t*)num = result;
    else
        *(int32_t*)num = (int32_t)(UINT32_MAX & result);

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_pointer_handler(UFILE             *input,
                        const u_scanf_spec_info *info,
                        ufmt_args        *args,
                        const UChar        *fmt,
                        int32_t            *consumed)
{
    int32_t    len;
    void        *p     = (void*)(args[0].ptrValue);


    /* skip all ws in the input */
    u_scanf_skip_leading_ws(input, info->fPadChar);

    /* fill the input's internal buffer */
    ufile_fill_uchar_buffer(input);

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* parse the pointer - cast to void** to assign to *p */
    *(void**)p = (void*) ufmt_uto64(input->str.fPos, &len, 16);

    /* update the input's position to reflect consumed data */
    input->str.fPos += len;

    /* we converted 1 arg */
    return 1;
}

static int32_t
u_scanf_scanset_handler(UFILE             *input,
                        const u_scanf_spec_info *info,
                        ufmt_args        *args,
                        const UChar        *fmt,
                        int32_t            *consumed)
{
    USet            *scanset;
    int32_t         len;
    UErrorCode      status = U_ZERO_ERROR;
    UChar32         c;
    UChar           *s     = (UChar*) (args[0].ptrValue);
    UChar           *alias, *limit;


    /* Create an empty set */
    scanset = uset_open(0, -1);

    /* Back up one to get the [ */
    fmt--;

    /* determine the size of the input's buffer */
    len = input->str.fLimit - input->str.fPos;

    /* truncate to the width, if specified */
    if(info->fWidth != -1)
        len = ufmt_min(len, info->fWidth);

    /* alias the target */
    alias = s;
    limit = alias + len;

    /* parse the scanset from the fmt string */
    *consumed = uset_applyPattern(scanset, fmt, u_strlen(fmt), 0, &status);

    /* verify that the parse was successful */
    if (U_SUCCESS(status)) {

        /* grab characters one at a time and make sure they are in the scanset */
        while(alias < limit) {
            if((c = u_fgetcx(input)) != U_EOF && uset_contains(scanset, c)) {
                int32_t idx = 0;
                UBool isError = FALSE;
                int32_t capacity = (int32_t)(1 + (limit - alias));

                U16_APPEND(alias, idx, capacity, c, isError);
                alias += idx;
                if (isError) {
                    break;
                }
            }
            else {
                /* if the character's not in the scanset, break out */
                break;
            }
        }

        /* put the final character we read back on the input */
        if(c != U_EOF) {
            u_fungetc(c, input);
        }
    }

    uset_close(scanset);

    /* if we didn't match at least 1 character, fail */
    if(alias == s)
        return -1;
    /* otherwise, add the terminator */
    else
        *alias = 0x00;

    /* we converted 1 arg */
    return 1;
}

U_CAPI int32_t U_EXPORT2
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

U_CAPI int32_t U_EXPORT2
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
    UChar patBuffer[UFMT_DEFAULT_BUFFER_SIZE];
    int32_t size = (int32_t)strlen(patternSpecification) + 1;

    /* convert from the default codepage to Unicode */
    if (size >= MAX_UCHAR_BUFFER_SIZE(patBuffer)) {
        pattern = (UChar *)uprv_malloc(size * sizeof(UChar));
        if(pattern == 0) {
            return 0;
        }
    }
    else {
        pattern = patBuffer;
    }
    u_charsToUChars(patternSpecification, pattern, size);

    /* do the work */
    converted = u_vfscanf_u(f, pattern, ap);

    /* clean up */
    if (pattern != patBuffer) {
        uprv_free(pattern);
    }

    return converted;
}

/* Use US-ASCII characters only for formatting. Most codepages have
 characters 20-7F from Unicode. Using any other codepage specific
 characters will make it very difficult to format the string on
 non-Unicode machines */
static const u_scanf_info g_u_scanf_infos[USCANF_NUM_FMT_HANDLERS] = {
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
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_UCHAR,
    UFMT_EMPTY,         UFMT_SCIENTIFIC,    UFMT_EMPTY,         UFMT_SCIDBL,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_UCHAR/*deprecated*/,
    UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,         UFMT_EMPTY,

/* 0x50 */
    UFMT_PERCENT,       UFMT_EMPTY,         UFMT_EMPTY,         UFMT_USTRING,
    UFMT_EMPTY,         UFMT_USTRING/*deprecated*/,UFMT_SPELLOUT,      UFMT_EMPTY,
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

U_CAPI int32_t  U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_vfscanf_u(UFILE       *f,
            const UChar *patternSpecification,
            va_list     ap)
{
    const UChar     *alias;
    int32_t         count, converted, temp;
    uint16_t        handlerNum;

    ufmt_args       args;
    u_scanf_spec    spec;
    ufmt_type_info  info;
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

                case ufmt_char:
                case ufmt_uchar:
                case ufmt_int:
                case ufmt_string:
                case ufmt_ustring:
                case ufmt_pointer:
                case ufmt_float:
                case ufmt_double:
                    args.ptrValue = va_arg(ap, void*);
                    break;

                case ufmt_count:
                    args.int64Value = va_arg(ap, int);
                    /* set the spec's width to the # of items converted */
                    spec.fInfo.fWidth = converted;
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

