/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 *
 *
 *   ucnv_err.h:
 *   defines error behaviour functions called by T_UConverter_{from,to}Unicode
 *
 *   These Functions, although public, should NEVER be called directly, they should be used as parameters to
 *   the T_UConverter_setMissing{Char,Unicode}Action API, to set the behaviour of a converter
 *   when it encounters ILLEGAL/UNMAPPED/INVALID sequences.
 *
 *   usage example:
 *
 *        ...
 *        UErrorCode err = U_ZERO_ERROR;
 *        UConverter* myConverter = T_UConverter_create("ibm-949", &err);
 *
 *        if (U_SUCCESS(err))
 *        {
 *       T_UConverter_setMissingUnicodeAction(myConverter, (MissingUnicodeAction)UCNV_FROM_U_CALLBACK_STOP, &err);
 *       T_UConverter_setMissingCharAction(myConverter, (MissingCharAction)UCNV_TO_U_CALLBACK_SUBSTITUTE, &err);
 *        }
 *        ...
 *
 *   The code above tells "myConverter" to stop when it encounters a ILLEGAL/TRUNCATED/INVALID sequences when it is used to
 *   convert from Unicode -> Codepage.
 *   and to substitute with a codepage specific substitutions sequence when converting from Codepage -> Unicode
 */

/* This file isn't designed to be included by itself. */
#ifndef UCNV_H
# include "unicode/ucnv.h"
 /* and the rest of this file will be ignored. */
#endif

#ifndef UCNV_ERR_H
#define UCNV_ERR_H

#include "unicode/utypes.h"

/** 
 * The process condition code to be used with the callbacks.  
 * UCNV_UNASSIGNED : the code point is unassigned.
 * UCNV_ILLEGAL : The code point is illegal.  For example,\x81\x2E is illegal 
 * because \x2E is not a valid trail byte for the \x81 lead byte in SJIS.
 * UCNV_IRREGULAR : The code point is not a regular sequence in the encoding.
 * For example,\xC0\E1 is irregular because the same character can be represented
 * as \x61.
 * UCNV_RESET : Whether the conversion operation has been reset.
 * UCNV_CLOSE : Whether the conversion operation has ended.
 */
typedef enum {
    UCNV_UNASSIGNED = 0,
    UCNV_ILLEGAL = 1,
    UCNV_IRREGULAR = 2,
    UCNV_RESET = 3,
    UCNV_CLOSE = 4
} UConverterCallbackReason;


/**
 * The structure for the fromUnicode callback function parameter.
 */
typedef struct {
	uint16_t size;
	UBool flush;
	UConverter *converter;
    const UChar *sourceStart;
	const UChar **pSource;
	const UChar *sourceLimit;
	char **pTarget;
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
    const char *sourceStart;
	const char **pSource;
	const char *sourceLimit;
	UChar **pTarget;
	const UChar *targetLimit;
	int32_t *offsets;
} UConverterToUnicodeArgs;


/**
 * Functor STOPS at the ILLEGAL_SEQUENCE 
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
 * Functor STOPS at the ILLEGAL_SEQUENCE 
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
 * Functor SKIPs the ILLEGAL_SEQUENCE 
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
 * Functor Substitute the ILLEGAL SEQUENCE with the current substitution string assiciated with _this,
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to U_INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
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
 * Functor Substitute the ILLEGAL SEQUENCE with a sequence escaped codepoints corresponding to the ILLEGAL
 * SEQUENCE (format  %UXXXX, e.g. "%uFFFE%u00AC%uC8FE"). In the Event the Converter doesn't support the
 * characters {u,%}[A-F][0-9], it will substitute  the illegal sequence with the substitution characters
 * (it will behave like the above functor).
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to U_INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
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
 * Functor SKIPs the ILLEGAL_SEQUENCE 
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
 * Functor Substitute the ILLEGAL SEQUENCE with the current substitution string assiciated with _this,
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to U_INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
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
 * Functor Substitute the ILLEGAL SEQUENCE with a sequence escaped codepoints corresponding to the
 * ILLEGAL SEQUENCE (format  %XNN, e.g. "%XFF%X0A%XC8%X03").
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to U_INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
 * @stable
 */

U_CAPI void U_EXPORT2 UCNV_TO_U_CALLBACK_ESCAPE (
                  void *context,
                  UConverterToUnicodeArgs *fromUArgs,
                  const char* codeUnits,
                  int32_t length,
                  UConverterCallbackReason reason,
				  UErrorCode * err);

#endif/*UCNV_ERR_H*/ 
