/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
*   uconv_cnv.h:
*   defines all the low level conversion functions
*   T_UnicodeConverter_{to,from}Unicode_$ConversionType
*
* Modification History:
*
*   Date        Name        Description
*   05/09/00    helena      Added implementation to handle fallback mappings.
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#ifndef UCNV_CNV_H
#define UCNV_CNV_H

#include "unicode/utypes.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnvmbcs.h"

union UConverterTable
  {
    UConverterMBCSTable mbcs;
  };


U_CDECL_BEGIN

/* this is used in fromUnicode DBCS tables as an "unassigned" marker */
#define missingCharMarker 0xFFFF

/*
 * #define missingUCharMarker 0xfffe
 *
 * there are actually two values used in toUnicode tables:
 * U+fffe "unassigned"
 * U+ffff "illegal"
 */



typedef void (*UConverterLoad) (UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode);
typedef void (*UConverterUnload) (UConverterSharedData *sharedData);

typedef void (*UConverterOpen) (UConverter *cnv, const char *name, const char *locale,uint32_t options, UErrorCode *pErrorCode);
typedef void (*UConverterClose) (UConverter *cnv);

typedef enum UConverterResetChoice {
    UCNV_RESET_BOTH,
    UCNV_RESET_TO_UNICODE,
    UCNV_RESET_FROM_UNICODE
} UConverterResetChoice;

typedef void (*UConverterReset) (UConverter *cnv, UConverterResetChoice choice);

typedef void (*T_ToUnicodeFunction) (UConverterToUnicodeArgs *, UErrorCode *);

typedef void (*T_FromUnicodeFunction) (UConverterFromUnicodeArgs *, UErrorCode *);

typedef UChar32 (*T_GetNextUCharFunction) (UConverterToUnicodeArgs *, UErrorCode *);

typedef void (*UConverterGetStarters)(const UConverter* converter,
                                      UBool starters[256],
                                      UErrorCode *pErrorCode);

/* If this function pointer is null or if the function returns null
 * the name field in static data struct should be returned by 
 * ucnv_getName() API function
 */
typedef const char * (*UConverterGetName) (const UConverter *cnv);

/**
 * Write the codepage substitution character.
 * If this function is not set, then ucnv_cbFromUWriteSub() writes
 * the substitution character from UConverter.
 * For stateful converters, it is typically necessary to handle this
 * specificially for the converter in order to properly maintain the state.
 */
typedef void (*UConverterWriteSub) (UConverterFromUnicodeArgs *pArgs, int32_t offsetIndex, UErrorCode *pErrorCode);

/**
 * For converter-specific safeClone processing
 * If this function is not set, then ucnv_safeClone assumes that the converter has no private data that changes
 * after the converter is done opening.
 */
typedef UConverter * (*UConverterSafeClone) (const UConverter   *cnv, 
                                             void               *stackBuffer,
                                             int32_t            *pBufferSize, 
                                             UErrorCode         *status);

UBool CONVERSION_U_SUCCESS (UErrorCode err);

void flushInternalUnicodeBuffer (UConverter * _this,
                                 UChar * myTarget,
                                 int32_t * myTargetIndex,
                                 int32_t targetLength,
                                 int32_t** offsets,
                                 UErrorCode * err);

void flushInternalCharBuffer (UConverter * _this,
                              char *myTarget,
                              int32_t * myTargetIndex,
                              int32_t targetLength,
                              int32_t** offsets,
                              UErrorCode * err);

/**
 * UConverterImpl contains all the data and functions for a converter type.
 * Its function pointers work much like a C++ vtable.
 * Many converter types need to define only a subset of the functions;
 * when a function pointer is NULL, then a default action will be performed.
 *
 * Every converter type must implement toUnicode, fromUnicode, and getNextUChar,
 * otherwise the converter may crash.
 * Every converter type that has variable-length codepage sequences should
 * also implement toUnicodeWithOffsets and fromUnicodeWithOffsets for
 * correct offset handling.
 * All other functions may or may not be implemented - it depends only on
 * whether the converter type needs them.
 *
 * When open() fails, then close() will be called, if present.
 */
struct UConverterImpl {
    UConverterType type;

    UConverterLoad load;
    UConverterUnload unload;

    UConverterOpen open;
    UConverterClose close;
    UConverterReset reset;

    T_ToUnicodeFunction toUnicode;
    T_ToUnicodeFunction toUnicodeWithOffsets;
    T_FromUnicodeFunction fromUnicode;
    T_FromUnicodeFunction fromUnicodeWithOffsets;
    T_GetNextUCharFunction getNextUChar;

    UConverterGetStarters getStarters;
    UConverterGetName getName;
    UConverterWriteSub writeSub;
    UConverterSafeClone safeClone;
};

extern const UConverterSharedData
    _MBCSData, _Latin1Data,
    _UTF8Data, _UTF16BEData, _UTF16LEData, _UTF32BEData, _UTF32LEData,
    _ISO2022Data, 
    _LMBCSData1,_LMBCSData2, _LMBCSData3, _LMBCSData4, _LMBCSData5, _LMBCSData6,
    _LMBCSData8,_LMBCSData11,_LMBCSData16,_LMBCSData17,_LMBCSData18,_LMBCSData19,
    _HZData,_ISCIIData, _SCSUData, _ASCIIData, _UTF7Data;

U_CDECL_END

/**
 * This function is useful for implementations of getNextUChar().
 * After a call to a callback function or to toUnicode(), an output buffer
 * begins with a Unicode code point that needs to be returned as UChar32,
 * and all following code units must be prepended to the - potentially
 * prefilled - overflow buffer in the UConverter.
 * The buffer should be at least of capacity UTF_MAX_CHAR_LENGTH so that a
 * complete UChar32's UChars fit into it.
 *
 * @param cnv    The converter that will get remaining UChars copied to its overflow area.
 * @param buffer An array of UChars that was passed into a callback function
 *               or a toUnicode() function.
 * @param length The number of code units (UChars) that are actually in the buffer.
 *               This must be >0.
 * @return The code point from the first UChars in the buffer.
 */
U_CFUNC UChar32
ucnv_getUChar32KeepOverflow(UConverter *cnv, const UChar *buffer, int32_t length);

/**
 * This helper function updates the offsets array after a callback function call.
 * It adds the sourceIndex to each offsets item, or sets each of them to -1 if
 * sourceIndex==-1.
 *
 * @param offsets The pointer to offsets entry that corresponds to the first target
 *                unit that the callback wrote.
 * @param length  The number of output units that the callback wrote.
 * @param sourceIndex The sourceIndex of the input sequence that the callback
 *                    function was called for.
 * @return offsets+length if offsets!=NULL, otherwise NULL
 */
U_CFUNC int32_t *
ucnv_updateCallbackOffsets(int32_t *offsets, int32_t length, int32_t sourceIndex);

/** Always use fallbacks from codepage to Unicode */
#define TO_U_USE_FALLBACK(useFallback) TRUE
#define UCNV_TO_U_USE_FALLBACK(cnv) TRUE

/** Use fallbacks from Unicode to codepage when cnv->useFallback or for private-use code points */
#define IS_PRIVATE_USE(c) ((uint32_t)((c)-0xe000)<0x1900 || (uint32_t)((c)-0xf0000)<0x20000)
#define FROM_U_USE_FALLBACK(useFallback, c) ((useFallback) || IS_PRIVATE_USE(c))
#define UCNV_FROM_U_USE_FALLBACK(cnv, c) FROM_U_USE_FALLBACK((cnv)->useFallback, c)

/**
 * This is a simple implementation of ucnv_getNextUChar() that uses the
 * converter's toUnicode() function.
 *
 * \par
 * A surrogate pair from a single byte sequence is always
 * combined to a supplementary code point.
 * A surrogate pair from consecutive byte sequences is only combined
 * if collectPairs is set. This is necessary for SCSU
 * but not allowed for most legacy codepages.
 *
 * @param pArgs The argument structure supplied by ucnv_getNextUChar()
 * @param toU   A function pointer to the converter's toUnicode() function
 * @param collectPairs indicates whether separate surrogate results from
 *                     consecutive byte sequences should be combined into
 *                     a single code point
 * @param pErrorCode An ICU error code parameter
 * @return The Unicode code point as a result of a conversion of a minimal
 *         number of input bytes
 */
U_CFUNC UChar32
ucnv_getNextUCharFromToUImpl(UConverterToUnicodeArgs *pArgs,
                             T_ToUnicodeFunction toU,
                             UBool collectPairs,
                             UErrorCode *pErrorCode);

#endif /* UCNV_CNV */
