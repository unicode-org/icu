/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File scsu.h
*
* Modification History:
*
*   Date        Name        Description
*   05/17/99    stephen	    Creation (ported from java UnicodeCompressor.java)
*   09/21/99    stephen     Updated to handle data splits on decompression.
*******************************************************************************
*/

#ifndef SCSU_H
#define SCSU_H 1

#include "utypes.h"

/* Number of windows */
#define NUMWINDOWS 8
#define NUMSTATICWINDOWS 8

/* Maximum value for a window's index */
#define MAXINDEX 0xFF

/** The size of the internal buffer for a UnicodeCompressor */
#define SCSU_BUFSIZE 3

/** The UnicodeCompressor struct */
struct UnicodeCompressor {

    /** Alias to current dynamic window */
    int32_t fCurrentWindow;
    
    /** Dynamic compression window offsets */
    int32_t fOffsets    [ NUMWINDOWS ];
    
    /** Current compression mode */
    int32_t fMode;

    /** Keeps count of times character indices are encountered */
    int32_t fIndexCount [ MAXINDEX + 1 ];
    
    /** The time stamps indicate when a window was last defined */
    int32_t fTimeStamps [ NUMWINDOWS ];
    
    /** The current time stamp */
    int32_t fTimeStamp;

    /** Internal buffer for saving state */
    uint8_t fBuffer [ SCSU_BUFSIZE ];
  
    /** Number of characters in our internal buffer */
    int32_t fBufferLength;
};
typedef struct UnicodeCompressor UnicodeCompressor;

/**
 * Initialize a UnicodeCompressor.
 * Sets all windows to their default values.
 * @see #reset
 */
CAPI void U_EXPORT2 scsu_init(UnicodeCompressor *comp);

/**
 * Reset the compressor to its initial state. 
 * @param comp The UnicodeCompressor to reset.
 */
CAPI void U_EXPORT2 scsu_reset(UnicodeCompressor *comp);

/**
 * Compress a Unicode character array into a byte array.
 *
 * This function is not guaranteed to completely fill the output buffer, nor
 * is it guaranteed to compress the entire input.  
 * If the source data is completely compressed, <TT>status</TT> will be set
 * to <TT>ZERO_ERROR</TT>.  
 * If the source data is not completely compressed, <TT>status</TT> will be
 * set to <TT>INDEX_OUTOFBOUNDS_ERROR</TT>.  If this occurs, larger buffers
 * should be allocated, or data flushed, and the function should be called
 * again with the new buffers.
 *
 * @param comp A pointer to a previously-initialized UnicodeCompressor
 * @param target I/O parameter.  On input, a pointer to a buffer of bytes to
 * receive the compressed data.  On output, points to the byte following
 * the last byte written.  This buffer must be at least 4 bytes.
 * @param targetLimit A pointer to the end of the array <TT>target</TT>.
 * @param source I/O parameter.  On input, a pointer to a buffer of
 * Unicode characters to be compressed.  On output, points to the character
 * following the last character compressed.
 * @param sourceLimit A pointer to the end of the array <TT>source</TT>.
 * @param status A pointer to an UErrorCode to receive any errors.
 *
 * @see #decompress
 */
CAPI void U_EXPORT2 scsu_compress(UnicodeCompressor *comp,
			uint8_t           **target,
			const uint8_t     *targetLimit,
			const UChar       **source,
			const UChar       *sourceLimit,
			UErrorCode        *status);

/**
 * Decompress a byte array into a Unicode character array.
 *
 * This function will either completely fill the output buffer, or
 * consume the entire input.  
 * If the source data is completely compressed, <TT>status</TT> will be set
 * to <TT>ZERO_ERROR</TT>.  
 * If the source data is not completely compressed, <TT>status</TT> will be
 * set to <TT>INDEX_OUTOFBOUNDS_ERROR</TT>.  If this occurs, larger buffers
 * should be allocated, or data flushed, and the function should be called
 * again with the new buffers.
 *
 * @param comp A pointer to a previously-initialized UnicodeDecompressor
 * @param target I/O parameter.  On input, a pointer to a buffer of Unicode
 * characters to receive the compressed data.  On output, points to the 
 * character following the last character written.  This buffer must be
 * at least 2 bytes.
 * @param targetLimit A pointer to the end of the array <TT>target</TT>.
 * @param source I/O parameter.  On input, a pointer to a buffer of
 * bytes to be decompressed.  On output, points to the byte following the
 * last byte decompressed.
 * @param sourceLimit A pointer to the end of the array <TT>source</TT>.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The number of Unicode characters writeten to <TT>target</TT>.
 *
 * @see #compress
 */
CAPI void U_EXPORT2 scsu_decompress(UnicodeCompressor *comp,
			  UChar             **target,
			  const UChar       *targetLimit,
			  const uint8_t     **source,
			  const uint8_t     *sourceLimit,
			  UErrorCode        *status);

#endif
