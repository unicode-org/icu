/*
*******************************************************************************
*
*   Copyright (C) 2002-2008, International Business Machines
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
#include "utrie2.h"

U_CDECL_BEGIN

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
    /* stores number of columns, plus two for start & limit values */
    UPVEC_COLUMNS,
    UPVEC_MAXROWS,
    UPVEC_ROWS,
    /* search optimization: remember last row seen */
    UPVEC_PREV_ROW,
    UPVEC_HEADER_LENGTH
};

/*
 * Special pseudo code points for storing the initialValue and the errorValue,
 * which are used to initialize a UTrie2 or similar.
 */
#define UPVEC_FIRST_SPECIAL_CP 0x110000
#define UPVEC_INITIAL_VALUE_CP 0x110000
#define UPVEC_ERROR_VALUE_CP 0x110001
#define UPVEC_MAX_CP 0x110001

/*
 * Special pseudo code point used in upvec_compact() signalling the end of
 * delivering special values and the beginning of delivering real ones.
 * Stable value, unlike UPVEC_MAX_CP which might grow over time.
 */
#define UPVEC_START_REAL_VALUES_CP 0x200000

U_CAPI uint32_t * U_EXPORT2
upvec_open(int32_t columns, int32_t maxRows);

U_CAPI void U_EXPORT2
upvec_close(uint32_t *pv);

U_CAPI UBool U_EXPORT2
upvec_setValue(uint32_t *pv,
               UChar32 start, UChar32 end,
               int32_t column,
               uint32_t value, uint32_t mask,
               UErrorCode *pErrorCode);

U_CAPI uint32_t U_EXPORT2
upvec_getValue(uint32_t *pv, UChar32 c, int32_t column);

/*
 * pRangeStart and pRangeEnd can be NULL.
 * @return NULL if rowIndex out of range and for illegal arguments
 */
U_CAPI uint32_t * U_EXPORT2
upvec_getRow(uint32_t *pv, int32_t rowIndex,
             UChar32 *pRangeStart, UChar32 *pRangeEnd);

/*
 * Compact the vectors:
 * - modify the memory
 * - keep only unique vectors
 * - store them contiguously from the beginning of the memory
 * - for each (non-unique) row, call the handler function
 *
 * The handler's rowIndex is the uint32_t index of the row in the compacted
 * memory block.
 * (Therefore, it starts at 0 increases in increments of the columns value.)
 *
 * In a first phase, only special values are delivered (each exactly once),
 * with start==end both equalling a special pseudo code point.
 * Then the handler is called once more with start==end==UPVEC_START_REAL_VALUES_CP
 * where rowIndex is the length of the compacted array,
 * and the row is arbitrary (but not NULL).
 * Then, in the second phase, the handler is called for each row of real values.
 */

U_CDECL_BEGIN

typedef void U_CALLCONV
UPVecCompactHandler(void *context,
                    UChar32 start, UChar32 end,
                    int32_t rowIndex, uint32_t *row, int32_t columns,
                    UErrorCode *pErrorCode);

U_CDECL_END

U_CAPI int32_t U_EXPORT2
upvec_compact(uint32_t *pv, UPVecCompactHandler *handler, void *context, UErrorCode *pErrorCode);

struct UPVecToUTrieContext {
    UNewTrie *newTrie;
    int32_t capacity;
    int32_t initialValue;
    UBool latin1Linear;
};
typedef struct UPVecToUTrieContext UPVecToUTrieContext;

/* context=UPVecToUTrieContext, creates the trie and stores the rowIndex values */
U_CAPI void U_CALLCONV
upvec_compactToUTrieHandler(void *context,
                            UChar32 start, UChar32 end,
                            int32_t rowIndex, uint32_t *row, int32_t columns,
                            UErrorCode *pErrorCode);

struct UPVecToUTrie2Context {
    UTrie2 *trie;
    int32_t initialValue;
    int32_t errorValue;
    int32_t maxValue;
};
typedef struct UPVecToUTrie2Context UPVecToUTrie2Context;

/* context=UPVecToUTrie2Context, creates the trie and stores the rowIndex values */
U_CAPI void U_CALLCONV
upvec_compactToUTrie2Handler(void *context,
                             UChar32 start, UChar32 end,
                             int32_t rowIndex, uint32_t *row, int32_t columns,
                             UErrorCode *pErrorCode);

U_CDECL_END

#endif
