/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  propsvec.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb22
*   created by: Markus W. Scherer
*
*   Store additional Unicode character properties in bit set vectors.
*/

#ifndef __UPROPSVEC_H__
#define __UPROPSVEC_H__

#include "unicode/utypes.h"
#include "utrie.h"

/*
 * Unicode Properties Vectors associated with code point ranges.
 * Stored in an array of uint32_t.
 *
 * The array starts with a header, then rows of integers store
 * the range limits and the properties vectors.
 *
 * In each row, row[0] contains the start code point and
 * row[1] contains the limit code point,
 * which is the start of the next range.
 *
 * Initially, there is only one range [0..0x110000[ with values 0.
 *
 * It would be possible to store only one range boundary per row,
 * but self-contained rows allow to later sort them by contents.
 */
enum {
    UPVEC_COLUMNS,
    UPVEC_MAXROWS,
    UPVEC_ROWS,
    UPVEC_RESERVED,
    UPVEC_HEADER_LENGTH
};

U_CFUNC uint32_t *
upvec_open(int32_t columns, int32_t maxRows);

U_CFUNC void
upvec_close(uint32_t *pv);

U_CFUNC UBool
upvec_setValue(uint32_t *pv,
               uint32_t start, uint32_t limit,
               int32_t column,
               uint32_t value, uint32_t mask,
               UErrorCode *pErrorCode);

U_CFUNC uint32_t *
upvec_getRow(uint32_t *pv, int32_t rowIndex,
             uint32_t *pRangeStart, uint32_t *pRangeLimit);

U_CFUNC int32_t
upvec_toTrie(uint32_t *pv, UNewTrie *trie, UErrorCode *pErrorCode);

#endif
