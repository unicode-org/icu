/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utrace.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003aug06
*   created by: Markus W. Scherer
*
*   Definitions for ICU tracing/logging.
*
*   Public API header.
*
*   This is currently a design document.
*   Latest Version: http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/utrace.h
*   CVS: http://oss.software.ibm.com/cvs/icu/icuhtml/design/utrace.h
*
*   Various notes:
*   - using a trace level variable to only call trace functions
*     when the level is sufficient
*   - using the same variable for tracing on/off to never make a function
*     call when off
*   - the function number is put into a local variable by the entry macro
*     and used implicitly to avoid copy&paste/typing mistakes by the developer
*   - the application must call utrace_setFunctions() and pass in
*     implementations for the trace functions
*   - ICU trace macros call ICU functions that route through the function
*     pointers if they have been set;
*     this avoids an indirection at the call site
*     (which would cost more code for another check and for the indirection)
*
*   ### TODO Issues:
*   - Verify that va_list is portable among compilers for the same platform.
*     va_list should be portable because printf() would fail otherwise!
*   - Should enum values like UTraceLevel be passed into int32_t-type arguments,
*     or should enum types be used?
*     We use enum types in all public APIs, so they should be safe and more
*     descriptive/type-safe. (Search case-insensitive for "level".)
*/

#ifndef __UTRACE_H__
#define __UTRACE_H__

#include <stdarg.h>
#include "unicode/utypes.h"

U_CDECL_BEGIN

/* Trace severity levels */
enum UTraceLevel {
    UTRACE_OFF=-1,
    UTRACE_ERROR,
    UTRACE_WARNING,
    UTRACE_INFO,
    UTRACE_VERBOSE
};
typedef enum UTraceLevel UTraceLevel;

/**
 * \var utrace_level
 * Trace level variable. Negative for "off".
 * Use only via UTRACE_ macros.
 * @internal
 */
U_COMMON_API int32_t
utrace_level; 

/**
 * Boolean expression to see if ICU tracing is turned on.
 * @draft ICU 2.8
 */
#define UTRACE_IS_ON (utrace_level>=UTRACE_ERROR)

/**
 * Boolean expression to see if ICU tracing is turned on
 * to at least the specified level.
 * @draft ICU 2.8
 */
#define UTRACE_LEVEL(level) (utrace_level>=(level))

/**
 * Setter for the trace level.
 * @param traceLevel A UTraceLevel value.
 * @draft ICU 2.8
 */
U_CAPI void U_EXPORT2
utrace_setLevel(int32_t traceLevel);

/**
 * Trace statement for the entry point of a function.
 * Stores the function number in a local variable.
 * In C code, must be placed immediately after the last variable declaration.
 * Must be matched with UTRACE_EXIT() at all function exit points.
 *
 * Tracing should start with UTRACE_ENTRY after checking for
 * U_FAILURE at function entry, so that if a function returns immediately
 * because of a pre-existing error condition, it does not show up in the trace,
 * consistent with ICU's error handling model.
 *
 * @param fnNumber The UTraceFunctionNumber for the current function.
 * @draft ICU 2.8
 */
#define UTRACE_ENTRY(fnNumber) \
    int32_t utraceFnNumber=(fnNumber); \
    if(UTRACE_IS_ON) { \
        utrace_entry(fnNumber); \
    }

/* Function Exit return types.  Internal 
 *   Bits 0-3:  The function return type.  First variable param.
 *   Bit    4:  Flag for presence of U_ErrorCode status param.
 */
enum UTraceExitVal {
    UTRACE_EXITV_NONE   = 0,
    UTRACE_EXITV_I32    = 1,
    UTRACE_EXITV_PTR    = 2,
    UTRACE_EXITV_BOOL   = 3,
    UTRACE_EXITV_MASK   = 0xf,
    UTRACE_EXITV_STATUS = 0x10
};
typedef enum UTraceExitVal UTraceExitVal;

/**
 * Trace statement for each exit point of a function that has a UTRACE_ENTRY()
 * statement.
 *
 * @param errorCode The function's ICU UErrorCode value at function exit,
 *                  or U_ZERO_ERROR if the function does not use a UErrorCode.
 *                  0==U_ZERO_ERROR indicates success,
 *                  positive values an error (see u_errorName()),
 *                  negative values an informational status.
 *
 * @draft ICU 2.8
 */
#define UTRACE_EXIT() \
    if(UTRACE_IS_ON) { \
        utrace_exit(utraceFnNumber, UTRACE_EXITV_NONE); \
    }

/**
 * Trace statement for each exit point of a function that has a UTRACE_ENTRY()
 * statement, and that returns a value.
 *
 * @param val       The function's return value, int32_t or comatible type.
 *
 * @draft ICU 2.8
 */
#define UTRACE_EXIT_D(val) \
    if(UTRACE_IS_ON) { \
        utrace_exit(utraceFnNumber, UTRACE_EXITV_I32, val); \
    }

#define UTRACE_EXIT_S(status) \
    if(UTRACE_IS_ON) { \
        utrace_exit(utraceFnNumber, UTRACE_EXITV_STATUS, status); \
    }


/**
 * Trace function for the entry point of a function.
 * Do not use directly, use UTRACE_ENTRY instead.
 * @param fnNumber The UTraceFunctionNumber for the current function.
 * @internal
 */
U_CAPI void U_EXPORT2
utrace_entry(int32_t fnNumber);

/**
 * Trace function for each exit point of a function.
 * Do not use directly, use UTRACE_EXIT* instead.
 * @param fnNumber The UTraceFunctionNumber for the current function.
 * @param returnType The type of the value returned by the function.
 * @param errorCode The UErrorCode value at function exit. See UTRACE_EXIT.
 * @internal
 */
U_CAPI void U_EXPORT2
utrace_exit(int32_t fnNumber, UTraceExitVal returnType, ...);


/**
 * Trace function used inside functions that have a UTRACE_ENTRY() statement.
 * Do not use directly, use UTRACE_DATAX() macros instead.
 *
 * ICU trace format string syntax
 *
 * Goals
 * - basic data output
 * - easy to use for trace programmer
 * - sufficient provision for data types for trace output readability
 * - well-defined types and binary portable APIs
 *
 * Non-goals
 * - printf compatibility
 * - fancy formatting
 * - argument reordering and other internationalization features
 *
 * ICU trace format strings contain plain text with argument inserts,
 * much like standard printf format strings.
 * Each insert begins with a '%', then optionally contains a 'v',
 * then exactly one type character.
 * Two '%' in a row represent a '%' instead of an insert.
 * If the 'v' is not specified, then one item of the specified type
 * is passed in.
 * If the 'v' (for "vector") is specified, then a vector of items of the
 * specified type is passed in, via a pointer to the first item
 * and an int32_t value for the length of the vector.
 *
 * The trace format strings need not have \n at the end.
 *
 * The ICU trace macros and functions that are used in ICU source code take
 * a variable number of arguments and pass them into the application trace
 * functions as va_list.
 *
 * Type characters:
 * - c A char character in the default codepage.
 * - s A NUL-terminated char * string in the default codepage.
 * - b A byte (8-bit integer).
 * - h A 16-bit integer.
 * - d A 32-bit integer.
 * - l A 64-bit integer.
 * - p A data pointer.
 *
 * Examples:
 * - the precise formatting is up to the application!
 * - the examples use type casts for arguments only to _show_ the types of
 *   arguments without needing variable declarations in the examples;
 *   the type casts will not be necessary in actual code
 *
 * UTRACE_DATA2(UTRACE_ERROR,
 *              "There is a character %c in the string %s.",
 *              (char)c, (const char *)s);
 * -> Error: There is a character 0x42 'B' in the string "Bravo".
 *
 * UTRACE_DATA4(UTRACE_WARNING,
 *              "Vector of bytes %vb vector of chars %vc",
 *              (const uint8_t *)bytes, (int32_t)bytesLength,
 *              (const char *)chars, (int32_t)charsLength);
 * -> Warning: Vector of bytes
 *      42 63 64 3f [4]
 *    vector of chars
 *      "Bcd?"[4]
 *
 * UTRACE_DATA3(UTRACE_INFO,
 *              "An int32_t %d and a whole bunch of them %vd",
 *              (int32_t)-5, (const int32_t *)ints, (int32_t)intsLength);
 * -> Info: An int32_t -5=0xfffffffb and a whole bunch of them
 *      fffffffb 00000005 0000010a [3]
 *
 * @param utraceFnNumber The number of the current function, from the local
 *        variable of the same name.
 * @param level The trace level for this message.
 * @param fmt The trace format string.
 *
 * @internal
 */
U_CAPI void U_EXPORT2
utrace_data(int32_t utraceFnNumber, int32_t level, const char *fmt, ...);

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes no data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA0(level, fmt) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes one data argument.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA1(level, fmt, a) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes two data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA2(level, fmt, a, b) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes three data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA3(level, fmt, a, b, c) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes four data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA4(level, fmt, a, b, c, d) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c), (d)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes five data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA5(level, fmt, a, b, c, d, e) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c), (d), (e)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes six data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA6(level, fmt, a, b, c, d, e, f) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c), (d), (e), (f)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes seven data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA7(level, fmt, a, b, c, d, e, f, g) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c), (d), (e), (f), (g)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes eight data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA8(level, fmt, a, b, c, d, e, f, g, h) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c), (d), (e), (f), (g), (h)); \
    }

/**
 * Trace statement used inside functions that have a UTRACE_ENTRY() statement.
 * Takes nine data arguments.
 * The number of arguments for this macro must match the number of inserts
 * in the format string. Vector inserts count as two arguments.
 * Calls utrace_data() if the level is high enough.
 * @draft ICU 2.8
 */
#define UTRACE_DATA9(level, fmt, a, b, c, d, e, f, g, h, i) \
    if(UTRACE_LEVEL(level)) { \
        utrace_data(utraceFnNumber, (level), (fmt), (a), (b), (c), (d), (e), (f), (g), (h), (i)); \
    }

/* Trace function pointers from the application ----------------------------- */

typedef void U_CALLCONV
UTraceEntry(const void *context, int32_t fnNumber);

typedef void U_CALLCONV
UTraceExit(const void *context, int32_t fnNumber, 
           UTraceExitVal retType, va_list args);

typedef void U_CALLCONV
UTraceData(const void *context, int32_t fnNumber, int32_t level,
           const char *fmt, va_list args);

U_CAPI void U_EXPORT2
utrace_setFunctions(const void *context,
                    UTraceEntry *e, UTraceExit *x, UTraceData *d,
                    int32_t traceLevel,
                    UErrorCode *pErrorCode);


/**
  *  Trace output   Formatter.  Application tracing functions may call
  *  back to this   function to format the trace output.
  *  @param outBuf  pointer to a buffer to receive the formatted output.  Output
  *                 will be null terminated if there is space in the buffer -
  *                 if the length of the requested output < the output buffer size.
  *  @param capacity  Length of the output buffer.
  *  @param indent  Number of spaces to indent the output.  Intended to allow
  *                 data displayed from nested functions to be indented for readability.
  *  @param fmt     Format specification for the data to output
  *  @param args    Data to be formatted.
  *  @return        Length of formatted output, including the terminating NULL if present.
  */
U_CAPI int32_t U_EXPORT2
utrace_format(char *outBuf, int32_t capacity,
              int32_t indent, const char *fmt,  va_list args);


/* Trace function numbers --------------------------------------------------- */

/**
 * Get the name of a function from its trace function number.
 *
 * @param fnNumber The trace number for an ICU function.
 * @return The name string for the function.
 *
 * @see UTraceFunctionNumber
 * @draft ICU 2.8
 */
U_CAPI const char * U_EXPORT2
utrace_functionName(int32_t fnNumber);

enum UTraceFunctionNumber {
    UTRACE_FUNCTION_START=0,
    UTRACE_U_INIT=UTRACE_FUNCTION_START,
    UTRACE_U_CLEANUP,
    UTRACE_FUNCTION_LIMIT,

    UTRACE_CONVERSION_START=0x1000,
    UTRACE_UCNV_OPEN=UTRACE_CONVERSION_START,
    UTRACE_UCNV_CLOSE,
    UTRACE_UCNV_FLUSH_CACHE,
    UTRACE_CONVERSION_LIMIT,

    UTRACE_COLLATION_START=0x2000,
    UTRACE_UCOL_OPEN=UTRACE_COLLATION_START,
    UTRACE_UCOL_CLOSE,
    UTRACE_UCOL_STRCOLL,
    UTRACE_UCOL_GET_SORTKEY,
    UTRACE_COLLATION_LIMIT,
    UTRACE_COLLATION_GETLOCALE
};
typedef enum UTraceFunctionNumber UTraceFunctionNumber;

U_CDECL_END

#endif
