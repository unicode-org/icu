/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  udataswp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003jun05
*   created by: Markus W. Scherer
*
*   Definitions for ICU data transformations for different platforms,
*   changing between big- and little-endian data and/or between
*   charset families (ASCII<->EBCDIC).
*/

#ifndef __UDATASWP_H__
#define __UDATASWP_H__

#include <stdarg.h>
#include "unicode/utypes.h"

/* forward declaration */
struct UDataSwapper;
typedef struct UDataSwapper UDataSwapper;

/*
 * ### TODO Issues
 *
 * - should each swap function check all input parameters?
 * - see if inData==outData can be allowed without major problems
 * - cnvalias.dat needs to sort outData char * strings with
 *   the output charset family version of ucnv_io.c/ucnv_compareNames()
 * - resource bundle swapping (in common) needs to call collation swapping
 *   (in i18n) for collation binaries
 */

/**
 * Function type for data transformation.
 * Transforms data, or just returns the length of the data if
 * the input length is -1.
 * Swap functions assume that their data pointers are aligned properly.
 *
 * @param ds Pointer to UDataSwapper containing global data about the
 *           transformation and function pointers for handling primitive
 *           types.
 * @param inData Pointer to the input data to be transformed or examined.
 * @param length Length of the data, counting bytes. May be -1 for preflighting.
 *               If length>=0, then transform the data.
 *               If length==-1, then only determine the length of the data.
 *               The length cannot be determined from the data itself for all
 *               types of data (e.g., not for simple arrays of integers).
 * @param outData Pointer to the output data buffer.
 *                If length>=0 (transformation), then the output buffer must
 *                have a capacity of at least length.
 *                If length==-1, then outData will not be used and can be NULL.
 * @param pErrorCode ICU UErrorCode parameter, must not be NULL and must
 *                   fulfill U_SUCCESS on input.
 * @return The actual length of the data.
 *
 * @see UDataSwapper
 * @draft ICU 2.8
 */
typedef int32_t U_CALLCONV
UDataSwapFn(const UDataSwapper *ds,
            const void *inData, int32_t length, void *outData,
            UErrorCode *pErrorCode);

/**
 * Convert one uint16_t from input to platform endianness.
 * @draft ICU 2.8
 */
typedef uint16_t U_CALLCONV
UDataReadUInt16(uint16_t x);

/**
 * Convert one uint32_t from input to platform endianness.
 * @draft ICU 2.8
 */
typedef uint32_t U_CALLCONV
UDataReadUInt32(uint32_t x);

/**
 * Compare invariant-character strings, one in the input data and the
 * other one caller-provided in Unicode.
 * You can use -1 for the length parameters of NUL-terminated strings as usual.
 * Returns Unicode code point order for invariant characters.
 * @draft ICU 2.8
 */
typedef int32_t U_CALLCONV
UDataCompareInvChars(const UDataSwapper *ds,
                     const char *inString, int32_t inLength,
                     const UChar *localString, int32_t localLength);

/**
 * Function for message output when an error occurs during data swapping.
 * A format string and variable number of arguments are passed
 * like for vprintf().
 *
 * @param context A function-specific context pointer.
 * @param fmt The format string.
 * @param args The arguments for format string inserts.
 *
 * @draft ICU 2.8
 */
typedef void U_CALLCONV
UDataPrintError(void *context, const char *fmt, va_list args);

struct UDataSwapper {
    /** Input endianness. @draft ICU 2.8 */
    UBool inIsBigEndian;
    /** Input charset family. @see U_CHARSET_FAMILY @draft ICU 2.8 */
    uint8_t inCharset;
    /** Output endianness. @draft ICU 2.8 */
    UBool outIsBigEndian;
    /** Output charset family. @see U_CHARSET_FAMILY @draft ICU 2.8 */
    uint8_t outCharset;

    /* basic functions for reading data values */

    /** Convert one uint16_t from input to platform endianness. @draft ICU 2.8 */
    UDataReadUInt16 *readUInt16;
    /** Convert one uint32_t from input to platform endianness. @draft ICU 2.8 */
    UDataReadUInt32 *readUInt32;
    /** Compare invariant-character strings. @draft ICU 2.8 */
    UDataCompareInvChars *compareInvChars;

    /* basic functions for data transformations */

    /** Transform an array of 16-bit integers. @draft ICU 2.8 */
    UDataSwapFn *swapArray16;
    /** Transform an array of 32-bit integers. @draft ICU 2.8 */
    UDataSwapFn *swapArray32;
    /** Transform an invariant-character string. @draft ICU 2.8 */
    UDataSwapFn *swapInvChars;

    /**
     * Function for message output when an error occurs during data swapping.
     * Can be NULL.
     * @draft ICU 2.8
     */
    UDataPrintError *printError;
    /** Context pointer for printError. @draft ICU 2.8 */
    void *printErrorContext;
};

U_CAPI UDataSwapper * U_EXPORT2
udata_openSwapper(UBool inIsBigEndian, uint8_t inCharset,
                  UBool outIsBigEndian, uint8_t outCharset,
                  UErrorCode *pErrorCode);

/**
 * Open a UDataSwapper for the given input data and the specified output
 * characteristics.
 * Values of -1 for any of the characteristics mean the local platform's
 * characteristics.
 *
 * @see udata_swap
 * @draft ICU 2.8
 */
U_CAPI UDataSwapper * U_EXPORT2
udata_openSwapperForInputData(const void *data, int32_t length,
                              UBool outIsBigEndian, uint8_t outCharset,
                              UErrorCode *pErrorCode);

U_CAPI void U_EXPORT2
udata_closeSwapper(UDataSwapper *ds);

/**
 * Read the beginning of an ICU data piece, recognize magic bytes,
 * swap the structure.
 * Set a U_UNSUPPORTED_ERROR if it does not look like an ICU data piece.
 *
 * @return The size of the data header, in bytes.
 *
 * @draft ICU 2.8
 */
U_CAPI int32_t U_EXPORT2
udata_swapDataHeader(const UDataSwapper *ds,
                     const void *inData, int32_t length, void *outData,
                     UErrorCode *pErrorCode);

U_CAPI int16_t U_EXPORT2
udata_readInt16(const UDataSwapper *ds, int16_t x);

U_CAPI int32_t U_EXPORT2
udata_readInt32(const UDataSwapper *ds, int32_t x);

U_CAPI void U_EXPORT2
udata_printError(const UDataSwapper *ds,
                 const char *fmt,
                 ...);

/* internal exports from putil.c -------------------------------------------- */

/* declared here to keep them out of the public putil.h */

/**
 * Swap invariant char * strings ASCII->EBCDIC.
 * @internal
 */
U_CFUNC int32_t
uprv_ebcdicFromAscii(const UDataSwapper *ds,
                     const void *inData, int32_t length, void *outData,
                     UErrorCode *pErrorCode);

/**
 * Swap invariant char * strings EBCDIC->ASCII.
 * @internal
 */
U_CFUNC int32_t
uprv_asciiFromEbcdic(const UDataSwapper *ds,
                     const void *inData, int32_t length, void *outData,
                     UErrorCode *pErrorCode);

/**
 * Compare ASCII invariant char * with Unicode invariant UChar *
 * @internal
 */
U_CFUNC int32_t
uprv_compareInvAscii(const UDataSwapper *ds,
                     const char *inString, int32_t inLength,
                     const UChar *localString, int32_t localLength);

/**
 * Compare EBCDIC invariant char * with Unicode invariant UChar *
 * @internal
 */
U_CFUNC int32_t
uprv_compareInvEbcdic(const UDataSwapper *ds,
                      const char *inString, int32_t inLength,
                      const UChar *localString, int32_t localLength);

/* material... -------------------------------------------------------------- */

#if 0

/* udata.h */

/**
 * Public API function in udata.c
 *
 * Same as udata_openChoice() but automatically swaps the data.
 * isAcceptable, if not NULL, may accept data with endianness and charset family
 * different from the current platform's properties.
 * If the data is acceptable and the platform properties do not match, then
 * the swap function is called to swap an allocated version of the data.
 * Preflighting may or may not be performed depending on whether the size of
 * the loaded data item is known.
 *
 * @param isAcceptable Same as for udata_openChoice(). May be NULL.
 *
 * @draft ICU 2.8
 */
U_CAPI UDataMemory * U_EXPORT2
udata_openSwap(const char *path, const char *type, const char *name,
               UDataMemoryIsAcceptable *isAcceptable, void *isAcceptableContext,
               UDataSwapFn *swap,
               UDataPrintError *printError, void *printErrorContext,
               UErrorCode *pErrorCode);

#endif

#endif
