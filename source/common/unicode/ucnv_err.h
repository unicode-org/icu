/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
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
 *  void *newContext = NULL;
 *  void *oldContext;
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
 * The process condition code to be used with the callbacks.  
 */
typedef enum {
    UCNV_UNASSIGNED = 0,  /* The code point is unassigned */
    UCNV_ILLEGAL = 1,     /* The code point is illegal, for example, 
                             \x81\x2E is illegal in SJIS because \x2E
                             is not a valid trail byte for the \x81 
                             lead byte. */
    UCNV_IRREGULAR = 2,   /* The codepoint is not a regular sequence in 
                             the encoding. For example, \xC0\xE1 would
                             be irregular in UTF-8 because \x61 represents
                             the same character. */
    UCNV_RESET = 3,       /* The callback is called with this reason when a
                             'reset' has occured. Callback should reset all
                             state. */
    UCNV_CLOSE = 4        /* Called when the converter is closed. The
                             callback should release any allocated memory.*/
} UConverterCallbackReason;


/**
 * The structure for the fromUnicode callback function parameter.
 */
typedef struct {
	uint16_t size;
	UBool flush;
	UConverter *converter;
	const UChar *source;
	const UChar *sourceLimit;
	char *target;
	const char *targetLimit;
	int32_t *offsets;  /* *offset = blah ; offset++; */
} UConverterFromUnicodeArgs;


/**
 * The structure for the toUnicode callback function parameter.
 */
typedef struct {
	uint16_t size;
	UBool flush;
	UConverter *converter;
	const char *source;
	const char *sourceLimit;
	UChar *target;
	const UChar *targetLimit;
	int32_t *offsets;
} UConverterToUnicodeArgs;


/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback STOPS at the ILLEGAL_SEQUENCE,
 * returning the error code back to the caller immediately.
 * @stable
 */
U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_STOP (
                  void *context,
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
 * @stable
 */
U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_STOP (
                  void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
				  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback skips any illegal sequence, 
 * simply ignoring those characters. 
 * @stable
 */
U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_SKIP (
                  void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
				  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This From Unicode callback will Substitute the ILLEGAL SEQUENCE with the
 * current substitution string for the converter. This is the default
 * callback.
 * @see ucnv_setSubstChars
 * @stable
 */

U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_SUBSTITUTE (
                  void *context,
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
 *  (in the format  %UXXXX, e.g. "%uFFFE%u00AC%uC8FE"). In the Event the
 * converter doesn't support the characters {u,%}[A-F][0-9], it will 
 * substitute  the illegal sequence with the substitution characters.
 * Note that percent (%) was chosen because backslash (\) does not exist
 * on many converters.
 * @stable
 */

U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_ESCAPE (
                  void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
				  UErrorCode * err);


/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This To Unicode callback skips any illegal sequence, 
 * simply ignoring those characters. 
 * @stable
 */
U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_SKIP (
                  void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
				  UErrorCode * err);

/**
 * DO NOT CALL THIS FUNCTION DIRECTLY!
 * This To Unicode callback will Substitute the ILLEGAL SEQUENCE with the
 * Unicode substitution character, U+FFFD.
 * @stable
 */
U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_SUBSTITUTE (
                  void *context,
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
 * @stable
 */

U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_ESCAPE (
                  void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
				  UErrorCode * err);

#endif

/*UCNV_ERR_H*/ 
