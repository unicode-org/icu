/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 *
 *
 *   ucnv_err.h:
 */
/**
 * \file
 * \brief C UConverter predefined error callbacks
 *
 *  <h2> Error Behaviour Fnctions </h2>
 *  Defines some error behaviour functions called by ucnv_{from,to}Unicode
 *  These are provided as part of ICU and many are stable, but they
 *  can also be considered only as an example of what can be done with
 *  callbacks.  You may of course write your own.
 *
 *   These Functions, although public, should NEVER be called directly, they should be used as parameters to
 *   the ucnv_setFromUCallback and ucnv_setToUCallback functions, to
 *    set the behaviour of a converter
 *   when it encounters ILLEGAL/UNMAPPED/INVALID sequences.
 *
 *   usage example:  'STOP' doesn't need any context, but newContext
 *    could be set to something other than 'NULL' if needed.
 *
 *  \code
 *    UErrorCode err = U_ZERO_ERROR;
 *    UConverter* myConverter = ucnv_open("ibm-949", &err);
 *  const void *newContext = NULL;
 *  const void *oldContext;
 *  UConverterFromUCallback oldAction;
 *
 *
 *    if (U_SUCCESS(err))
 *    {
 *  ucnv_setFromUCallBack(myConverter,
 *                       UCNV_FROM_U_CALLBACK_STOP,
 *                       newContext,
 *                       &oldAction,
 *                       &oldContext,
 *                      &status);
 *    }
 *  \endcode
 *
 *   The code above tells "myConverter" to stop when it encounters a ILLEGAL/TRUNCATED/INVALID sequences when it is used to
 *   convert from Unicode -> Codepage.
 *   The behavior from Codepage to Unicode is not changed.
 */

/* This file isn't designed to be included all by itself. */
#ifndef UCNV_H
# include "unicode/ucnv.h"
 /* and the rest of this file will be ignored. */
#endif

#ifndef UCNV_ERR_H
#define UCNV_ERR_H

#include "unicode/utypes.h"


/**
 * FROM_U, TO_U options for sub callback
 * @stable ICU 2.0
 */
#define UCNV_SUB_STOP_ON_ILLEGAL "i"

/**
 * FROM_U, TO_U options for skip callback
 * @stable ICU 2.0
 */
#define UCNV_SKIP_STOP_ON_ILLEGAL "i"

/**
 * FROM_U_CALLBACK_ESCAPE option to escape the code unit according to ICU (%UXXXX) 
 * @stable ICU 2.0
 */
#define UCNV_ESCAPE_ICU       NULL
/**
 * FROM_U_CALLBACK_ESCAPE option to escape the code unit according to JAVA (\uXXXX)
 * @stable ICU 2.0
 */
#define UCNV_ESCAPE_JAVA      "J"
/**
 * FROM_U_CALLBACK_ESCAPE option to escape the code unit according to C (\uXXXX \UXXXXXXXX)
 * TO_U_CALLBACK_ESCAPE option to escape the character value accoding to C (\xXXXX)
 * @stable ICU 2.0
 */
#define UCNV_ESCAPE_C         "C"
/**
 * FROM_U_CALLBACK_ESCAPE option to escape the code unit according to XML Decimal escape (&#DDDD)
 * TO_U_CALLBACK_ESCAPE option to escape the character value accoding to XML Decimal escape (&#DDDD)
 * @stable ICU 2.0
 */
#define UCNV_ESCAPE_XML_DEC   "D"
/**
 * FROM_U_CALLBACK_ESCAPE option to escape the code unit according to XML Hex escape (&#xXXXX)
 * TO_U_CALLBACK_ESCAPE option to escape the character value accoding to XML Hex escape (&#xXXXX)
 * @stable ICU 2.0
 */
#define UCNV_ESCAPE_XML_HEX   "X"
/**
 * FROM_U_CALLBACK_ESCAPE option to escape teh code unit according to Unicode (U+XXXXX)
 * @stable ICU 2.0
 */
#define UCNV_ESCAPE_UNICODE   "U"

/** 
 * The process condition code to be used with the callbacks.  
 * Codes which are greater than UCNV_IRREGULAR should be 
 * passed on to any chained callbacks.
 * @stable ICU 2.0
 */
typedef enum {
    UCNV_UNASSIGNED = 0,  /**< The code point is unassigned.
                             The error code U_INVALID_CHAR_FOUND will be set. */
    UCNV_ILLEGAL = 1,     /**< The code point is illegal. For example, 
                             \x81\x2E is illegal in SJIS because \x2E
                             is not a valid trail byte for the \x81 
                             lead byte.
                             Also, starting with Unicode 3.0.1, non-shortest byte sequences
                             in UTF-8 (like \xC1\xA1 instead of \x61 for U+0061)
                             are also illegal, not just irregular.
                             The error code U_ILLEGAL_CHAR_FOUND will be set. */
    UCNV_IRREGULAR = 2,   /**< The codepoint is not a regular sequence in 
                             the encoding. For example, \xED\xA0\x80..\xED\xBF\xBF
                             are irregular UTF-8 byte sequences for single surrogate
                             code points.
                             The error code U_INVALID_CHAR_FOUND will be set. */
    UCNV_RESET = 3,       /**< The callback is called with this reason when a
                             'reset' has occured. Callback should reset all
                             state. */
    UCNV_CLOSE = 4,        /**< Called when the converter is closed. The
                             callback should release any allocated memory.*/
    UCNV_CLONE = 5         /**< Called when ucnv_safeClone() is called on the
                              converter. the pointer available as the
                              'context' is an alias to the original converters'
                              context pointer. If the context must be owned
                              by the new converter, the callback must clone 
                              the data and call ucnv_setFromUCallback 
                              (or setToUCallback) with the correct pointer.
                              @draft ICU 2.2
                           */
} UConverterCallbackReason;


/**
 * The structure for the fromUnicode callback function parameter.
 * @stable ICU 2.0
 */
typedef struct {
    uint16_t size;              /**< The size of this struct. @stable ICU 2.0 */
    UBool flush;                /**< The internal state of converter will be reset and data flushed if set to TRUE. @stable ICU 2.0    */
    UConverter *converter;      /**< Pointer to the converter that is opened and to which this struct is passed as an argument. @stable ICU 2.0  */
    const UChar *source;        /**< Pointer to the source source buffer. @stable ICU 2.0    */
    const UChar *sourceLimit;   /**< Pointer to the limit (end + 1) of source buffer. @stable ICU 2.0    */
    char *target;               /**< Pointer to the target buffer. @stable ICU 2.0    */
    const char *targetLimit;    /**< Pointer to the limit (end + 1) of target buffer. @stable ICU 2.0     */
    int32_t *offsets;           /**< Pointer to the buffer that recieves the offsets. *offset = blah ; offset++;. @stable ICU 2.0  */
} UConverterFromUnicodeArgs;


/**
 * The structure for the toUnicode callback function parameter.
 * @stable ICU 2.0
 */
typedef struct {
    uint16_t size;              /**< The size of this struct    */
    UBool flush;                /**< The internal state of converter will be reset and data flushed if set to TRUE. @stable ICU 2.0   */
    UConverter *converter;      /**< Pointer to the converter that is opened and to which this struct is passed as an argument. @stable ICU 2.0 */
    const char *source;         /**< Pointer to the source source buffer. @stable ICU 2.0    */
    const char *sourceLimit;    /**< Pointer to the limit (end + 1) of source buffer. @stable ICU 2.0    */
    UChar *target;              /**< Pointer to the target buffer. @stable ICU 2.0    */
    const UChar *targetLimit;   /**< Pointer to the limit (end + 1) of target buffer. @stable ICU 2.0     */
    int32_t *offsets;           /**< Pointer to the buffer that recieves the offsets. *offset = blah ; offset++;. @stable ICU 2.0  */
} UConverterToUnicodeArgs;


/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback STOPS at the ILLEGAL_SEQUENCE,
 * returning the error code back to the caller immediately.
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_STOP (
                  const void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
                  UErrorCode * err);



/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This To Unicode callback STOPS at the ILLEGAL_SEQUENCE,
 * returning the error code back to the caller immediately.
 * 
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_STOP (
                  const void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
                  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback skips any ILLEGAL_SEQUENCE, or
 * skips only UNASSINGED_SEQUENCE depending on the context parameter
 * simply ignoring those characters. 
 * @param context: the function currently recognizes the callback options:
 *                 UCNV_SKIP_STOP_ON_ILLEGAL: STOPS at the ILLEGAL_SEQUENCE,
 *                      returning the error code back to the caller immediately.
 *                 NULL: Skips any ILLEGAL_SEQUENCE
 *                     
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_SKIP (
                  const void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
                  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback will Substitute the ILLEGAL SEQUENCE, or 
 * UNASSIGNED_SEQUENCE depending on context parameter, with the
 * current substitution string for the converter. This is the default
 * callback.
 * @param context: the function currently recognizes the callback options:
 *                 UCNV_SUB_STOP_ON_ILLEGAL: STOPS at the ILLEGAL_SEQUENCE,
 *                      returning the error code back to the caller immediately.
 *                 NULL: Substitutes any ILLEGAL_SEQUENCE
 * @see ucnv_setSubstChars
 * @stable ICU 2.0
 */

U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_SUBSTITUTE (
                  const void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
                  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback will Substitute the ILLEGAL SEQUENCE with the
 * hexadecimal representation of the illegal codepoints
 *
 * @param context: the function currently recognizes the callback options:
 *        
 *        UCNV_ESCAPE_ICU: Substitues the  ILLEGAL SEQUENCE with the hexadecimal 
 *          representation in the format  %UXXXX, e.g. "%uFFFE%u00AC%uC8FE"). 
 *          In the Event the converter doesn't support the characters {%,U}[A-F][0-9], 
 *          it will  substitute  the illegal sequence with the substitution characters.
 *          Note that  codeUnit(32bit int eg: unit of a surrogate pair) is represented as
 *          %UD84D%UDC56
 *        UCNV_ESCAPE_JAVA: Substitues the  ILLEGAL SEQUENCE with the hexadecimal 
 *          representation in the format  \uXXXX, e.g. "\uFFFE\u00AC\uC8FE"). 
 *          In the Event the converter doesn't support the characters {\,u}[A-F][0-9], 
 *          it will  substitute  the illegal sequence with the substitution characters.
 *          Note that  codeUnit(32bit int eg: unit of a surrogate pair) is represented as
 *          \uD84D\uDC56
 *        UCNV_ESCAPE_C: Substitues the  ILLEGAL SEQUENCE with the hexadecimal 
 *          representation in the format  \uXXXX, e.g. "\uFFFE\u00AC\uC8FE"). 
 *          In the Event the converter doesn't support the characters {\,u,U}[A-F][0-9], 
 *          it will  substitute  the illegal sequence with the substitution characters.
 *          Note that  codeUnit(32bit int eg: unit of a surrogate pair) is represented as
 *          \U00023456
 *        UCNV_ESCAPE_XML_DEC: Substitues the  ILLEGAL SEQUENCE with the decimal 
 *          representation in the format  &#DDDDDDDD, e.g. "&#65534&#172&#51454"). 
 *          In the Event the converter doesn't support the characters {&,#}[0-9], 
 *          it will  substitute  the illegal sequence with the substitution characters.
 *          Note that  codeUnit(32bit int eg: unit of a surrogate pair) is represented as
 *          &#144470 and Zero padding is ignored.
 *        UCNV_ESCAPE_XML_HEX:Substitues the  ILLEGAL SEQUENCE with the decimal 
 *          representation in the format  &#xXXXX, e.g. "&#xFFFE&#x00AC&#xC8FE"). 
 *          In the Event the converter doesn't support the characters {&,#,x}[0-9], 
 *          it will  substitute  the illegal sequence with the substitution characters.
 *          Note that  codeUnit(32bit int eg: unit of a surrogate pair) is represented as
 *          &#x23456
 * @stable ICU 2.0
 */

U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_ESCAPE (
                  const void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
                  UErrorCode * err);


/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This To Unicode callback skips any ILLEGAL_SEQUENCE, or
 * skips only UNASSINGED_SEQUENCE depending on the context parameter
 * simply ignoring those characters. 
 * @param context: the function currently recognizes the callback options:
 *                 UCNV_SKIP_STOP_ON_ILLEGAL: STOPS at the ILLEGAL_SEQUENCE,
 *                      returning the error code back to the caller immediately.
 *                 NULL: Skips any ILLEGAL_SEQUENCE
 *                     
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_SKIP (
                  const void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
                  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This To Unicode callback will Substitute the ILLEGAL SEQUENCE,or 
 * UNASSIGNED_SEQUENCE depending on context parameter,  with the
 * Unicode substitution character, U+FFFD.
 * @param context: the function currently recognizes the callback options:
 *                 UCNV_SUB_STOP_ON_ILLEGAL: STOPS at the ILLEGAL_SEQUENCE,
 *                      returning the error code back to the caller immediately.
 *                 NULL: Substitutes any ILLEGAL_SEQUENCE
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_SUBSTITUTE (
                  const void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
                  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This To Unicode callback will Substitute the ILLEGAL SEQUENCE with the
 * hexadecimal representation of the illegal bytes
 *  (in the format  %XNN, e.g. "%XFF%X0A%XC8%X03").
 * @stable ICU 2.0
 */

U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_ESCAPE (
                  const void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
                  UErrorCode * err);

#endif

/*UCNV_ERR_H*/ 
