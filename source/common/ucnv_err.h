/*
 ********************************************************************************
 *                                                                              *
 * COPYRIGHT:                                                                   *
 *   (C) Copyright International Business Machines Corporation, 1999            *
 *   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
 *   US Government Users Restricted Rights - Use, duplication, or disclosure    *
 *   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
 *                                                                              *
 ********************************************************************************
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
 *        UErrorCode err = ZERO_ERROR;
 *        UConverter* myConverter = T_UConverter_create("ibm-949", &err);
 *
 *        if (SUCCESS(err))
 *        {
 *       T_UConverter_setMissingUnicodeAction(myConverter, (MissingUnicodeAction)MissingUnicodeAction_STOP, &err);
 *       T_UConverter_setMissingCharAction(myConverter, (MissingCharAction)MissingCharAction_SUBSTITUTE, &err);
 *        }
 *        ...
 *
 *   The code above tells "myConverter" to stop when it encounters a ILLEGAL/TRUNCATED/INVALID sequences when it is used to
 *   convert from Unicode -> Codepage.
 *   and to substitute with a codepage specific substitutions sequence when converting from Codepage -> Unicode
 */


#ifndef UCNV_ERR_H
#define UCNV_ERR_H


#include "utypes.h"


/*Functor STOPS at the ILLEGAL_SEQUENCE */
CAPI void U_EXPORT2 MissingUnicodeAction_STOP (UConverter * _this,
				     char **target,
				     const char *targetLimit,
				     const UChar ** source,
				     const UChar * sourceLimit,
				     int32_t* offsets,
				     bool_t flush,
				     UErrorCode * err);


/*Functor STOPS at the ILLEGAL_SEQUENCE */
CAPI void U_EXPORT2 MissingCharAction_STOP (UConverter * _this,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);




/*Functor SKIPs the ILLEGAL_SEQUENCE */
CAPI void U_EXPORT2 MissingUnicodeAction_SKIP (UConverter * _this,
				     char **target,
				     const char *targetLimit,
				     const UChar ** source,
				     const UChar * sourceLimit,
				     int32_t* offsets,
				     bool_t flush,
				     UErrorCode * err);

/* Functor Substitute the ILLEGAL SEQUENCE with the current substitution string assiciated with _this,
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
 */

CAPI void U_EXPORT2 MissingUnicodeAction_SUBSTITUTE (UConverter * _this,
					   char **target,
					   const char *targetLimit,
					   const UChar ** source,
					   const UChar * sourceLimit,
					   int32_t* offsets,
					   bool_t flush,
					   UErrorCode * err);

/* Functor Substitute the ILLEGAL SEQUENCE with a sequence escaped codepoints corresponding to the ILLEGAL
 * SEQUENCE (format  %UXXXX, e.g. "%uFFFE%u00AC%uC8FE"). In the Event the Converter doesn't support the
 * characters {u,%}[A-F][0-9], it will substitute  the illegal sequence with the substitution characters
 * (it will behave like the above functor).
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
 */

CAPI void U_EXPORT2 MissingUnicodeAction_SUBSTITUTEwithValue (UConverter * _this,
						    char **target,
						    const char *targetLimit,
						    const UChar ** source,
						  const UChar * sourceLimit,
						    int32_t* offsets,
						    bool_t flush,
						    UErrorCode * err);


/*Functor SKIPs the ILLEGAL_SEQUENCE */
CAPI void U_EXPORT2 MissingCharAction_SKIP (UConverter * _this,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);


/* Functor Substitute the ILLEGAL SEQUENCE with the current substitution string assiciated with _this,
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
 */
CAPI void U_EXPORT2 MissingCharAction_SUBSTITUTE (UConverter * _this,
					UChar ** target,
					const UChar * targetLimit,
					const char **source,
					const char *sourceLimit,
					int32_t* offsets,
					bool_t flush,
					UErrorCode * err);

/* Functor Substitute the ILLEGAL SEQUENCE with a sequence escaped codepoints corresponding to the
 * ILLEGAL SEQUENCE (format  %XNN, e.g. "%XFF%X0A%XC8%X03").
 * in the event target buffer is too small, it will store the extra info in the UConverter, and err
 * will be set to INDEX_OUTOFBOUNDS_ERROR. The next time T_UConverter_fromUnicode is called, it will
 * store the left over data in target, before transcoding the "source Stream"
 */

CAPI void U_EXPORT2 MissingCharAction_SUBSTITUTEwithValue (UConverter * _this,
						 UChar ** target,
						 const UChar * targetLimit,
						 const char **source,
						 const char *sourceLimit,
						 int32_t* offsets,
						 bool_t flush,
						 UErrorCode * err);


#endif/*UCNV_ERR_H*/ 
