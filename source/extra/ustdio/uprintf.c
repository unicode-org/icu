/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File uprintf.c
*
* Modification History:
*
*   Date        Name        Description
*   11/19/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*                           Added conversion from default codepage.
******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "uprintf.h"
#include "uprntf_p.h"
#include "unicode/ustdio.h"
#include "ufile.h"
#include "unicode/ustring.h"
#include "locbund.h"
#include "unicode/unum.h"
#include "unicode/udat.h"

#include "cmemory.h"


/* --- Prototypes ---------------------------- */

int32_t
u_printf_simple_percent_handler(UFILE                 *stream,
                const u_printf_spec_info     *info,
                const ufmt_args            *args);

int32_t
u_printf_string_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_printf_date_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args         *args);

int32_t
u_printf_scientific_handler(UFILE             *stream,
                const u_printf_spec_info     *info,
                const ufmt_args            *args);

int32_t
u_printf_scidbl_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_printf_uchar_handler(UFILE                 *stream,
               const u_printf_spec_info     *info,
               const ufmt_args            *args);

int32_t
u_printf_currency_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args            *args);

int32_t
u_printf_ustring_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args         *args);

int32_t
u_printf_percent_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_printf_time_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args         *args);

int32_t
u_printf_spellout_handler(UFILE             *stream,
              const u_printf_spec_info     *info,
              const ufmt_args            *args);

int32_t
u_printf_hex_handler(UFILE             *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_printf_char_handler(UFILE                 *stream,
              const u_printf_spec_info         *info,
              const ufmt_args              *args);

int32_t
u_printf_integer_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_printf_uinteger_handler(UFILE                 *stream,
             const u_printf_spec_info     *info,
             const ufmt_args            *args);

int32_t
u_printf_double_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_printf_count_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_printf_octal_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args);

int32_t
u_printf_pointer_handler(UFILE                 *stream,
            const u_printf_spec_info     *info,
            const ufmt_args            *args);

/* ANSI style formatting */
/* Use US-ASCII characters only for formatting */

/* % */
#define UFMT_SIMPLE_PERCENT {ufmt_simple_percent, u_printf_simple_percent_handler}
/* s */
#define UFMT_STRING         {ufmt_string, u_printf_string_handler}
/* c */
#define UFMT_CHAR           {ufmt_char, u_printf_char_handler}
/* d, i */
#define UFMT_INT            {ufmt_int, u_printf_integer_handler}
/* u */
#define UFMT_UINT           {ufmt_int, u_printf_uinteger_handler}
/* o */
#define UFMT_OCTAL          {ufmt_int, u_printf_octal_handler}
/* x, X */
#define UFMT_HEX            {ufmt_int, u_printf_hex_handler}
/* f */
#define UFMT_DOUBLE         {ufmt_double, u_printf_double_handler}
/* e, E */
#define UFMT_SCIENTIFIC     {ufmt_double, u_printf_scientific_handler}
/* g, G */
#define UFMT_SCIDBL         {ufmt_double, u_printf_scidbl_handler}
/* n */
#define UFMT_COUNT          {ufmt_count, u_printf_count_handler}

/* non-ANSI extensions */
/* Use US-ASCII characters only for formatting */

/* p */
#define UFMT_POINTER        {ufmt_pointer, u_printf_pointer_handler}
/* D */
#define UFMT_DATE           {ufmt_date, u_printf_date_handler}
/* T */
#define UFMT_TIME           {ufmt_date, u_printf_time_handler}
/* V */
#define UFMT_SPELLOUT       {ufmt_double, u_printf_spellout_handler}
/* P */
#define UFMT_PERCENT        {ufmt_double, u_printf_percent_handler}
/* M */
#define UFMT_CURRENCY       {ufmt_double, u_printf_currency_handler}
/* K */
#define UFMT_UCHAR          {ufmt_uchar, u_printf_uchar_handler}
/* U */
#define UFMT_USTRING        {ufmt_ustring, u_printf_ustring_handler}


#define UFMT_EMPTY {ufmt_empty, NULL}

struct u_printf_info {
    ufmt_type_info info;
    u_printf_handler handler;
};
typedef struct u_printf_info u_printf_info;

/* Use US-ASCII characters only for formatting. Most codepages have
 characters 20-7F from Unicode. Using any other codepage specific
 characters will make it very difficult to format the string on
 non-Unicode machines */
static const u_printf_info g_u_printf_infos[108] = {
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

#define UPRINTF_NUM_FMT_HANDLERS sizeof(g_u_printf_infos)

/* We do not use handlers for 0-0x1f */
#define UPRINTF_BASE_FMT_HANDLERS 0x20

/* buffer size for formatting */
#define UFPRINTF_BUFFER_SIZE 1024
#define UFPRINTF_SYMBOL_BUFFER_SIZE 8

static const UChar gNullStr[] = {0x28, 0x6E, 0x75, 0x6C, 0x6C, 0x29, 0}; /* "(null)" */
static const UChar gSpaceStr[] = {0x20, 0}; /* " " */

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

U_CAPI int32_t  U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_vfprintf(    UFILE        *f,
           const char    *patternSpecification,
           va_list        ap)
{
    int32_t count;
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
    count = u_vfprintf_u(f, pattern, ap);

    /* clean up */
    if (pattern != buffer) {
        uprv_free(pattern);
    }

    return count;
}

static int32_t
u_printf_pad_and_justify(UFILE              *stream,
                         const u_printf_spec_info    *info,
                         const UChar                 *result,
                         int32_t                     resultLen)
{
    int32_t written, i;

    /* pad and justify, if needed */
    if(info->fWidth != -1 && resultLen < info->fWidth) {
        /* left justify */
        if(info->fLeft) {
            written = u_file_write(result, resultLen, stream);
            for(i = 0; i < info->fWidth - resultLen; ++i) {
                written += u_file_write(&info->fPadChar, 1, stream);
            }
        }
        /* right justify */
        else {
            written = 0;
            for(i = 0; i < info->fWidth - resultLen; ++i) {
                written += u_file_write(&info->fPadChar, 1, stream);
            }
            written += u_file_write(result, resultLen, stream);
        }
    }
    /* just write the formatted output */
    else
        written = u_file_write(result, resultLen, stream);

    return written;
}

/* Sets the sign of a format based on u_sprintf_spec_info */
/* TODO: Is setting the prefix symbol to a positive sign a good idea in all locales? */
static void
u_printf_set_sign(UNumberFormat        *format,
                   const u_printf_spec_info     *info,
                   UErrorCode *status)
{
    if(info->fShowSign) {
        if (info->fSpace) {
            /* Setting UNUM_PLUS_SIGN_SYMBOL affects the exponent too. */
            /* unum_setSymbol(format, UNUM_PLUS_SIGN_SYMBOL, gSpaceStr, 1, &status); */
            unum_setTextAttribute(format, UNUM_POSITIVE_PREFIX, gSpaceStr, 1, status);
        }
        else {
            UChar plusSymbol[UFPRINTF_SYMBOL_BUFFER_SIZE];
            int32_t symbolLen;

            symbolLen = unum_getSymbol(format,
                UNUM_PLUS_SIGN_SYMBOL,
                plusSymbol,
                sizeof(plusSymbol)/sizeof(*plusSymbol),
                status);
            unum_setTextAttribute(format,
                UNUM_POSITIVE_PREFIX,
                plusSymbol,
                symbolLen,
                status);
        }
    }
}

/* handle a '%' */

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
u_printf_string_handler(UFILE                 *stream,
                        const u_printf_spec_info     *info,
                        const ufmt_args            *args)
{
    UChar *s;
    UChar buffer[UFMT_DEFAULT_BUFFER_SIZE];
    int32_t len, written;
    int32_t argSize;
    const char *arg = (const char*)(args[0].ptrValue);

    /* convert from the default codepage to Unicode */
    if (arg) {
        argSize = (int32_t)strlen(arg) + 1;
        if (argSize >= MAX_UCHAR_BUFFER_SIZE(buffer)) {
            s = ufmt_defaultCPToUnicode(arg, argSize,
                    (UChar *)uprv_malloc(MAX_UCHAR_BUFFER_NEEDED(argSize)),
                    MAX_UCHAR_BUFFER_NEEDED(argSize));
            if(s == NULL) {
                return 0;
            }
        }
        else {
            s = ufmt_defaultCPToUnicode(arg, argSize, buffer,
                    sizeof(buffer)/sizeof(UChar));
        }
    }
    else {
        s = (UChar *)gNullStr;
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
    else {
        written = u_printf_pad_and_justify(stream, info, s, len);
    }

    /* clean up */
    if (gNullStr != s && buffer != s) {
        uprv_free(s);
    }

    return written;
}

/* HSYS */
int32_t
u_printf_integer_handler(UFILE                 *stream,
                         const u_printf_spec_info     *info,
                         const ufmt_args            *args)
{
    long            num         = (long) (args[0].intValue);
    UNumberFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    int32_t        minDigits     = -1;
    UErrorCode        status        = U_ZERO_ERROR;

    /* mask off any necessary bits */
    if(info->fIsShort)
        num &= UINT16_MAX;
    else if(! info->fIsLong || ! info->fIsLongLong)
        num &= UINT32_MAX;

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

        u_printf_set_sign(format, info, &status);
    }

    /* format the number */
    unum_format(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    /* restore the number format */
    if(minDigits != -1)
        unum_setAttribute(format, UNUM_MIN_INTEGER_DIGITS, minDigits);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}

int32_t
u_printf_hex_handler(UFILE             *stream,
                     const u_printf_spec_info     *info,
                     const ufmt_args            *args)
{
    long            num         = (long) (args[0].intValue);
    UChar           result[UFPRINTF_BUFFER_SIZE];
    int32_t         len        = UFPRINTF_BUFFER_SIZE;


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
    if(num != 0 && info->fAlt && len < UFPRINTF_BUFFER_SIZE - 2) {
        /* shift the formatted string right by 2 chars */
        memmove(result + 2, result, len * sizeof(UChar));
        result[0] = 0x0030;
        result[1] = info->fSpec;
        len += 2;
    }

    return u_printf_pad_and_justify(stream, info, result, len);
}

int32_t
u_printf_octal_handler(UFILE                 *stream,
                       const u_printf_spec_info     *info,
                       const ufmt_args            *args)
{
    long            num         = (long) (args[0].intValue);
    UChar           result[UFPRINTF_BUFFER_SIZE];
    int32_t         len        = UFPRINTF_BUFFER_SIZE;


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
    if(info->fAlt && result[0] != 0x0030 && len < UFPRINTF_BUFFER_SIZE - 1) {
        /* shift the formatted string right by 1 char */
        memmove(result + 1, result, len * sizeof(UChar));
        result[0] = 0x0030;
        len += 1;
    }

    return u_printf_pad_and_justify(stream, info, result, len);
}


int32_t
u_printf_uinteger_handler(UFILE                 *stream,
                          const u_printf_spec_info     *info,
                          const ufmt_args            *args)
{
    u_printf_spec_info uint_info;
    ufmt_args uint_args;

    memcpy(&uint_info, info, sizeof(u_printf_spec_info));
    memcpy(&uint_args, args, sizeof(ufmt_args));

    uint_info.fPrecision = 0;
    uint_info.fAlt  = FALSE;

    /* Get around int32_t limitations */
    uint_args.doubleValue = ((double) ((uint32_t) (uint_args.intValue)));

    return u_printf_double_handler(stream, &uint_info, &uint_args);
}

int32_t
u_printf_double_handler(UFILE                 *stream,
                        const u_printf_spec_info     *info,
                        const ufmt_args            *args)
{
    double        num         = (double) (args[0].doubleValue);
    UNumberFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    int32_t        minDecimalDigits;
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
    u_printf_set_sign(format, info, &status);

    /* format the number */
    unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    /* restore the number format */
    unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
    unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}


int32_t
u_printf_char_handler(UFILE                 *stream,
                      const u_printf_spec_info         *info,
                      const ufmt_args              *args)
{
    UChar s[UTF_MAX_CHAR_LENGTH+1];
    int32_t len = 1, written;
    unsigned char arg = (unsigned char)(args[0].intValue);

    /* convert from default codepage to Unicode */
    ufmt_defaultCPToUnicode((const char *)&arg, 2, s, sizeof(s)/sizeof(UChar));

    /* Remember that this may be a surrogate pair */
    if (arg != 0) {
        len = u_strlen(s);
    }

    /* width = minimum # of characters to write */
    /* precision = maximum # of characters to write */

    /* precision takes precedence over width */
    /* determine if the string should be truncated */
    if(info->fPrecision != -1 && len > info->fPrecision) {
        written = u_file_write(s, info->fPrecision, stream);
    }
    else {
        /* determine if the string should be padded */
        written = u_printf_pad_and_justify(stream, info, s, len);
    }

    return written;
}


int32_t
u_printf_pointer_handler(UFILE                 *stream,
                         const u_printf_spec_info     *info,
                         const ufmt_args            *args)
{
    long            num         = (long) (args[0].intValue);
    UChar           result[UFPRINTF_BUFFER_SIZE];
    int32_t         len        = UFPRINTF_BUFFER_SIZE;


    /* format the pointer in hex */
    ufmt_ltou(result, &len, num, 16, TRUE, info->fPrecision);

    return u_printf_pad_and_justify(stream, info, result, len);
}


int32_t
u_printf_scientific_handler(UFILE             *stream,
                            const u_printf_spec_info     *info,
                            const ufmt_args            *args)
{
    double        num         = (double) (args[0].doubleValue);
    UNumberFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    int32_t        minDecimalDigits;
    int32_t        maxDecimalDigits;
    UErrorCode        status        = U_ZERO_ERROR;
    UChar srcExpBuf[UFPRINTF_SYMBOL_BUFFER_SIZE];
    int32_t srcLen, expLen;
    UChar expBuf[UFPRINTF_SYMBOL_BUFFER_SIZE];


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
        stream->fOwnBundle  = TRUE;
        format              = u_locbund_getScientificFormat(stream->fBundle);
    }

    srcLen = unum_getSymbol(format,
        UNUM_EXPONENTIAL_SYMBOL,
        srcExpBuf,
        sizeof(srcExpBuf),
        &status);

    /* Upper/lower case the e */
    if (info->fSpec == (UChar)0x65 /* e */) {
        expLen = u_strToLower(expBuf, (int32_t)sizeof(expBuf),
            srcExpBuf, srcLen,
            stream->fBundle->fLocale,
            &status);
    }
    else {
        expLen = u_strToUpper(expBuf, (int32_t)sizeof(expBuf),
            srcExpBuf, srcLen,
            stream->fBundle->fLocale,
            &status);
    }

    unum_setSymbol(format,
        UNUM_EXPONENTIAL_SYMBOL,
        expBuf,
        expLen,
        &status);

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
    u_printf_set_sign(format, info, &status);

    /* format the number */
    unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    /* restore the number format */
    unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
    unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

    /* Since we clone the fBundle and we're only using the scientific
       format, we don't need to save the old exponent value. */
    /*unum_setSymbol(format,
        UNUM_EXPONENTIAL_SYMBOL,
        srcExpBuf,
        srcLen,
        &status);*/

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}

int32_t
u_printf_date_handler(UFILE             *stream,
                      const u_printf_spec_info     *info,
                      const ufmt_args         *args)
{
    UDate            num         = (UDate) (args[0].dateValue);
    UDateFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    UErrorCode        status        = U_ZERO_ERROR;


    /* get the formatter */
    format = u_locbund_getDateFormat(stream->fBundle);

    /* handle error */
    if(format == 0)
        return 0;

    /* format the date */
    udat_format(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}

int32_t
u_printf_time_handler(UFILE             *stream,
                      const u_printf_spec_info     *info,
                      const ufmt_args         *args)
{
    UDate            num         = (UDate) (args[0].dateValue);
    UDateFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    UErrorCode        status        = U_ZERO_ERROR;


    /* get the formatter */
    format = u_locbund_getTimeFormat(stream->fBundle);

    /* handle error */
    if(format == 0)
        return 0;

    /* format the time */
    udat_format(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}


int32_t
u_printf_percent_handler(UFILE                 *stream,
                         const u_printf_spec_info     *info,
                         const ufmt_args            *args)
{
    double        num         = (double) (args[0].doubleValue);
    UNumberFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    int32_t        minDecimalDigits;
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
    u_printf_set_sign(format, info, &status);

    /* format the number */
    unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    /* restore the number format */
    unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
    unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}


int32_t
u_printf_currency_handler(UFILE             *stream,
                          const u_printf_spec_info     *info,
                          const ufmt_args            *args)
{
    double        num         = (double) (args[0].doubleValue);
    UNumberFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    int32_t        minDecimalDigits;
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
        unum_setAttribute(format, UNUM_FRACTION_DIGITS, 2);
    }
    else {
        /* # of decimal digits is 2 if precision not specified, 2 is typical */
        unum_setAttribute(format, UNUM_FRACTION_DIGITS, 2);
    }

    /* set whether to show the sign */
    u_printf_set_sign(format, info, &status);

    /* format the number */
    unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    /* restore the number format */
    unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
    unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}

int32_t
u_printf_ustring_handler(UFILE                 *stream,
                         const u_printf_spec_info     *info,
                         const ufmt_args         *args)
{
    int32_t len = 0, written;
    const UChar *arg = (const UChar*)(args[0].ptrValue);

    /* allocate enough space for the buffer */
    if (arg == NULL) {
        arg = gNullStr;
    }
    len = u_strlen(arg);

    /* width = minimum # of characters to write */
    /* precision = maximum # of characters to write */

    /* precision takes precedence over width */
    /* determine if the string should be truncated */
    if(info->fPrecision != -1 && len > info->fPrecision) {
        written = u_file_write(arg, info->fPrecision, stream);
    }
    else {
        /* determine if the string should be padded */
        written = u_printf_pad_and_justify(stream, info, arg, len);
    }

    return written;
}



int32_t
u_printf_uchar_handler(UFILE                 *stream,
                       const u_printf_spec_info     *info,
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
        written = u_printf_pad_and_justify(stream, info, &arg, 1);
    }

    return written;
}

int32_t
u_printf_scidbl_handler(UFILE                 *stream,
                        const u_printf_spec_info     *info,
                        const ufmt_args            *args)
{
    u_printf_spec_info scidbl_info;
    double      num = args[0].doubleValue;

    memcpy(&scidbl_info, info, sizeof(u_printf_spec_info));

    /* determine whether to use 'd', 'e' or 'f' notation */
    if (scidbl_info.fPrecision == -1 && num == uprv_trunc(num))
    {
        /* use 'f' notation */
        scidbl_info.fSpec = 0x0066;
        scidbl_info.fPrecision = 0;
        /* call the double handler */
        return u_printf_double_handler(stream, &scidbl_info, args);
    }
    else if(num < 0.0001 || (scidbl_info.fPrecision < 1 && 1000000.0 <= num)
        || (scidbl_info.fPrecision != -1 && num > uprv_pow10(scidbl_info.fPrecision)))
    {
        /* use 'e' or 'E' notation */
        scidbl_info.fSpec = scidbl_info.fSpec - 2;
        /* call the scientific handler */
        return u_printf_scientific_handler(stream, &scidbl_info, args);
    }
    else {
        /* use 'f' notation */
        scidbl_info.fSpec = 0x0066;
        /* call the double handler */
        return u_printf_double_handler(stream, &scidbl_info, args);
    }
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
u_printf_spellout_handler(UFILE             *stream,
                          const u_printf_spec_info     *info,
                          const ufmt_args            *args)
{
    double        num         = (double) (args[0].doubleValue);
    UNumberFormat        *format;
    UChar            result        [UFPRINTF_BUFFER_SIZE];
    int32_t        minDecimalDigits;
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
    u_printf_set_sign(format, info, &status);

    /* format the number */
    unum_formatDouble(format, num, result, UFPRINTF_BUFFER_SIZE, 0, &status);

    /* restore the number format */
    unum_setAttribute(format, UNUM_MIN_FRACTION_DIGITS, minDecimalDigits);
    unum_setAttribute(format, UNUM_MAX_FRACTION_DIGITS, maxDecimalDigits);

    return u_printf_pad_and_justify(stream, info, result, u_strlen(result));
}

#define UP_PERCENT 0x0025

U_CAPI int32_t  U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_vfprintf_u(    UFILE        *f,
             const UChar    *patternSpecification,
             va_list        ap)
{
    u_printf_spec   spec;
    const UChar     *alias;
    int32_t         patCount, written;
    uint16_t      handlerNum;

    ufmt_args       args;

    ufmt_type_info    info;
    u_printf_handler handler;

    /* alias the pattern */
    alias = patternSpecification;

    /* haven't written anything yet */
    written = 0;

    /* iterate through the pattern */
    for(;;) {

        /* find the next '%' */
        patCount = 0;
        while(*alias != UP_PERCENT && *alias != 0x0000) {
            alias++;
            ++patCount;
        }

        /* write any characters before the '%' */
        if(patCount > 0)
            written += u_file_write(alias - patCount, patCount, f);

        /* break if at end of string */
        if(*alias == 0x0000)
            break;

        /* parse the specifier */
        patCount = u_printf_parse_spec(alias, &spec);

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

        handlerNum = (uint16_t)(spec.fInfo.fSpec - UPRINTF_BASE_FMT_HANDLERS);
        if (handlerNum < UPRINTF_NUM_FMT_HANDLERS) {
            /* query the info function for argument information */
            info = g_u_printf_infos[ handlerNum ].info;
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
            handler = g_u_printf_infos[ handlerNum ].handler;
            if(handler != 0) {
                written += (*handler)(f, &spec.fInfo, &args);
            }
            else {
                /* just echo unknown tags */
                written += u_file_write(alias, patCount, f);
            }
        }
        else {
            /* just echo unknown tags */
            written += u_file_write(alias, patCount, f);
        }

        /* update the pointer in pattern and continue */
        alias += patCount;
    }

    /* return # of UChars written */
    return written;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
