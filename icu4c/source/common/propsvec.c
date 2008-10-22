/*
*******************************************************************************
*
*   Copyright (C) 2002-2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  propsvec.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb22
*   created by: Markus W. Scherer
*
*   Store additional Unicode character properties in bit set vectors.
*/

#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "utrie.h"
#include "utrie2.h"
#include "uarrsort.h"
#include "propsvec.h"

static uint32_t *
_findRow(uint32_t *pv, UChar32 rangeStart) {
    uint32_t *row;
    int32_t *hdr;
    int32_t columns, i, start, limit, prevRow, rows;

    hdr=(int32_t *)pv;
    columns=hdr[UPVEC_COLUMNS];
    limit=hdr[UPVEC_ROWS];
    prevRow=hdr[UPVEC_PREV_ROW];
    rows=hdr[UPVEC_ROWS];
    pv+=UPVEC_HEADER_LENGTH;

    /* check the vicinity of the last-seen row */
    if(prevRow<rows) {
        row=pv+prevRow*columns;
        if(rangeStart>=(UChar32)row[0]) {
            if(rangeStart<(UChar32)row[1]) {
                /* same row as last seen */
                return row;
            } else if(
                ++prevRow<rows &&
                rangeStart>=(UChar32)(row+=columns)[0] && rangeStart<(UChar32)row[1]
            ) {
                /* next row after the last one */
                hdr[UPVEC_PREV_ROW]=prevRow;
                return row;
            }
        }
    }

    /* do a binary search for the start of the range */
    start=0;
    while(start<limit-1) {
        i=(start+limit)/2;
        row=pv+i*columns;
        if(rangeStart<(UChar32)row[0]) {
            limit=i;
        } else if(rangeStart<(UChar32)row[1]) {
            hdr[UPVEC_PREV_ROW]=i;
            return row;
        } else {
            start=i;
        }
    }

    /* must be found because all ranges together always cover all of Unicode */
    hdr[UPVEC_PREV_ROW]=start;
    return pv+start*columns;
}

U_CAPI uint32_t * U_EXPORT2
upvec_open(int32_t columns, int32_t maxRows) {
    uint32_t *pv, *row;
    uint32_t cp;
    int32_t length;

    if(columns<1 || maxRows<1) {
        return NULL;
    }

    columns+=2; /* count range start and limit columns */
    length=UPVEC_HEADER_LENGTH+maxRows*columns;
    pv=(uint32_t *)uprv_malloc(length*4);
    if(pv!=NULL) {
        /* set header */
        pv[UPVEC_COLUMNS]=(uint32_t)columns;
        pv[UPVEC_MAXROWS]=(uint32_t)maxRows;
        pv[UPVEC_ROWS]=2+(UPVEC_MAX_CP-UPVEC_FIRST_SPECIAL_CP);
        pv[UPVEC_PREV_ROW]=0;

        /* set the all-Unicode row and the special-value rows */
        row=pv+UPVEC_HEADER_LENGTH;
        uprv_memset(row, 0, pv[UPVEC_ROWS]*columns*4);
        row[0]=0;
        row[1]=0x110000;
        row+=columns;
        for(cp=UPVEC_FIRST_SPECIAL_CP; cp<=UPVEC_MAX_CP; ++cp) {
            row[0]=cp;
            row[1]=cp+1;
            row+=columns;
        }
    }
    return pv;
}

U_CAPI void U_EXPORT2
upvec_close(uint32_t *pv) {
    if(pv!=NULL) {
        uprv_free(pv);
    }
}

U_CAPI UBool U_EXPORT2
upvec_setValue(uint32_t *pv,
               UChar32 start, UChar32 end,
               int32_t column,
               uint32_t value, uint32_t mask,
               UErrorCode *pErrorCode) {
    uint32_t *firstRow, *lastRow;
    int32_t columns;
    UChar32 limit;
    UBool splitFirstRow, splitLastRow;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    if( pv==NULL ||
        start<0 || start>end || end>UPVEC_MAX_CP ||
        column<0 || (uint32_t)(column+1)>=pv[UPVEC_COLUMNS]
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return FALSE;
    }
    limit=end+1;

    /* initialize */
    columns=(int32_t)pv[UPVEC_COLUMNS];
    column+=2; /* skip range start and limit columns */
    value&=mask;

    /* find the rows whose ranges overlap with the input range */

    /* find the first row, always successful */
    firstRow=_findRow(pv, start);

    /* find the last row, always successful */
    lastRow=firstRow;
    /*
     * Start searching with an unrolled loop:
     * start and limit are often in a single range, or in adjacent ranges.
     */
    if(limit>(UChar32)lastRow[1]) {
        lastRow+=columns;
        if(limit>(UChar32)lastRow[1]) {
            lastRow+=columns;
            if(limit>(UChar32)lastRow[1]) {
                if((limit-(UChar32)lastRow[1])<10) {
                    /* we are close, continue looping */
                    do {
                        lastRow+=columns;
                    } while(limit>(UChar32)lastRow[1]);
                } else {
                    lastRow=_findRow(pv, limit-1);
                }
            }
        }
    }

    /*
     * Rows need to be split if they partially overlap with the
     * input range (only possible for the first and last rows)
     * and if their value differs from the input value.
     */
    splitFirstRow= (UBool)(start!=(UChar32)firstRow[0] && value!=(firstRow[column]&mask));
    splitLastRow= (UBool)(limit!=(UChar32)lastRow[1] && value!=(lastRow[column]&mask));

    /* split first/last rows if necessary */
    if(splitFirstRow || splitLastRow) {
        int32_t count, rows;

        rows=(int32_t)pv[UPVEC_ROWS];
        if((rows+splitFirstRow+splitLastRow)>(int32_t)pv[UPVEC_MAXROWS]) {
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return FALSE;
        }

        /* count the number of row cells to move after the last row, and move them */
        count = (int32_t)((pv+UPVEC_HEADER_LENGTH+rows*columns)-(lastRow+columns));
        if(count>0) {
            uprv_memmove(
                lastRow+(1+splitFirstRow+splitLastRow)*columns,
                lastRow+columns,
                count*4);
        }
        pv[UPVEC_ROWS]=rows+splitFirstRow+splitLastRow;

        /* split the first row, and move the firstRow pointer to the second part */
        if(splitFirstRow) {
            /* copy all affected rows up one and move the lastRow pointer */
            count = (int32_t)((lastRow-firstRow)+columns);
            uprv_memmove(firstRow+columns, firstRow, count*4);
            lastRow+=columns;

            /* split the range and move the firstRow pointer */
            firstRow[1]=firstRow[columns]=(uint32_t)start;
            firstRow+=columns;
        }

        /* split the last row */
        if(splitLastRow) {
            /* copy the last row data */
            uprv_memcpy(lastRow+columns, lastRow, columns*4);

            /* split the range and move the firstRow pointer */
            lastRow[1]=lastRow[columns]=(uint32_t)limit;
        }
    }

    /* set the "row last seen" to the last row for the range */
    pv[UPVEC_PREV_ROW]=(uint32_t)((lastRow-(pv+UPVEC_HEADER_LENGTH))/columns);

    /* set the input value in all remaining rows */
    firstRow+=column;
    lastRow+=column;
    mask=~mask;
    for(;;) {
        *firstRow=(*firstRow&mask)|value;
        if(firstRow==lastRow) {
            break;
        }
        firstRow+=columns;
    }
    return TRUE;
}

U_CAPI uint32_t U_EXPORT2
upvec_getValue(uint32_t *pv, UChar32 c, int32_t column) {
    uint32_t *row;

    if(pv==NULL || c<0 || c>UPVEC_MAX_CP) {
        return 0;
    }
    row=_findRow(pv, c);
    return row[2+column];
}

U_CAPI uint32_t * U_EXPORT2
upvec_getRow(uint32_t *pv, int32_t rowIndex,
             UChar32 *pRangeStart, UChar32 *pRangeEnd) {
    uint32_t *row;
    int32_t columns;

    if(pv==NULL || rowIndex<0 || rowIndex>=(int32_t)pv[UPVEC_ROWS]) {
        return NULL;
    }

    columns=(int32_t)pv[UPVEC_COLUMNS];
    row=pv+UPVEC_HEADER_LENGTH+rowIndex*columns;
    if(pRangeStart!=NULL) {
        *pRangeStart=row[0];
    }
    if(pRangeEnd!=NULL) {
        *pRangeEnd=row[1]-1;
    }
    return row+2;
}

static int32_t U_CALLCONV
upvec_compareRows(const void *context, const void *l, const void *r) {
    const uint32_t *left=(const uint32_t *)l, *right=(const uint32_t *)r;
    const uint32_t *pv=(const uint32_t *)context;
    int32_t i, count, columns;

    count=columns=(int32_t)pv[UPVEC_COLUMNS]; /* includes start/limit columns */

    /* start comparing after start/limit but wrap around to them */
    i=2;
    do {
        if(left[i]!=right[i]) {
            return left[i]<right[i] ? -1 : 1;
        }
        if(++i==columns) {
            i=0;
        }
    } while(--count>0);

    return 0;
}

U_CAPI int32_t U_EXPORT2
upvec_compact(uint32_t *pv, UPVecCompactHandler *handler, void *context, UErrorCode *pErrorCode) {
    uint32_t *row;
    int32_t i, columns, valueColumns, rows, count;
    UChar32 start, limit;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(pv==NULL || handler==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    rows=(int32_t)pv[UPVEC_ROWS];
    if(rows==0) {
        return 0;
    }

    row=pv+UPVEC_HEADER_LENGTH;
    columns=(int32_t)pv[UPVEC_COLUMNS];
    valueColumns=columns-2; /* not counting start & limit */

    /* sort the properties vectors to find unique vector values */
    if(rows>1) {
        uprv_sortArray(row, rows, columns*4,
                       upvec_compareRows, pv, FALSE, pErrorCode);
    }
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /*
     * Find and set the special values.
     * This has to do almost the same work as the compaction below,
     * to find the indexes where the special-value rows will move.
     */
    count=-valueColumns;
    for(i=0; i<rows; ++i) {
        start=(UChar32)row[0];

        /* count a new values vector if it is different from the current one */
        if(count<0 || 0!=uprv_memcmp(row+2, row-valueColumns, valueColumns*4)) {
            count+=valueColumns;
        }

        if(start>=UPVEC_FIRST_SPECIAL_CP) {
            handler(context, start, start, count, row+2, valueColumns, pErrorCode);
            if(U_FAILURE(*pErrorCode)) {
                return 0;
            }
        }

        row+=columns;
    }

    /* count is at the beginning of the last vector, add valueColumns to include that last vector */
    count+=valueColumns;

    /* Call the handler once more to signal the start of delivering real values. */
    handler(context, UPVEC_START_REAL_VALUES_CP, UPVEC_START_REAL_VALUES_CP,
            count, row-valueColumns, valueColumns, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /*
     * Move vector contents up to a contiguous array with only unique
     * vector values, and call the handler function for each vector.
     *
     * This destroys the Properties Vector structure and replaces it
     * with an array of just vector values.
     */
    row=pv+UPVEC_HEADER_LENGTH;
    count=-valueColumns;
    for(i=0; i<rows; ++i) {
        /* fetch these first before memmove() may overwrite them */
        start=(UChar32)row[0];
        limit=(UChar32)row[1];

        /* add a new values vector if it is different from the current one */
        if(count<0 || 0!=uprv_memcmp(row+2, pv+count, valueColumns*4)) {
            count+=valueColumns;
            uprv_memmove(pv+count, row+2, valueColumns*4);
        }

        if(start<UPVEC_FIRST_SPECIAL_CP) {
            handler(context, start, limit-1, count, pv+count, valueColumns, pErrorCode);
            if(U_FAILURE(*pErrorCode)) {
                return 0;
            }
        }

        row+=columns;
    }

    /* count is at the beginning of the last vector, add valueColumns to include that last vector */
    return count+valueColumns;
}

/*
 * TODO(markus): Add upvec_compactToUTrie2WithRowIndexes() function that returns
 * a UTrie2 and does not require the caller to pass in a callback function.
 *
 * Add upvec_16BitsToUTrie2() function that enumerates all rows, extracts
 * some 16-bit field and builds and returns a UTrie2.
 */

U_CAPI void U_CALLCONV
upvec_compactToUTrieHandler(void *context,
                            UChar32 start, UChar32 end,
                            int32_t rowIndex, uint32_t *row, int32_t columns,
                            UErrorCode *pErrorCode) {
    UPVecToUTrieContext *toUTrie=(UPVecToUTrieContext *)context;
    if(start<UPVEC_FIRST_SPECIAL_CP) {
        if(!utrie_setRange32(toUTrie->newTrie, start, end+1, (uint32_t)rowIndex, TRUE)) {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    } else {
        switch(start) {
        case UPVEC_INITIAL_VALUE_CP:
            toUTrie->initialValue=rowIndex;
            break;
        case UPVEC_START_REAL_VALUES_CP:
            if(rowIndex>0xffff) {
                /* too many rows for a 16-bit trie */
                *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            } else {
                toUTrie->newTrie=utrie_open(NULL, NULL, toUTrie->capacity,
                                            toUTrie->initialValue, toUTrie->initialValue,
                                            toUTrie->latin1Linear);
                if(toUTrie->newTrie==NULL) {
                    *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                }
            }
            break;
        default:
            break;
        }
    }
}

U_CAPI void U_CALLCONV
upvec_compactToUTrie2Handler(void *context,
                             UChar32 start, UChar32 end,
                             int32_t rowIndex, uint32_t *row, int32_t columns,
                             UErrorCode *pErrorCode) {
    UPVecToUTrie2Context *toUTrie2=(UPVecToUTrie2Context *)context;
    if(start<UPVEC_FIRST_SPECIAL_CP) {
        utrie2_setRange32(toUTrie2->trie, start, end, (uint32_t)rowIndex, TRUE, pErrorCode);
    } else {
        switch(start) {
        case UPVEC_INITIAL_VALUE_CP:
            toUTrie2->initialValue=rowIndex;
            break;
        case UPVEC_ERROR_VALUE_CP:
            toUTrie2->errorValue=rowIndex;
            break;
        case UPVEC_START_REAL_VALUES_CP:
            toUTrie2->maxValue=rowIndex;
            if(rowIndex>0xffff) {
                /* too many rows for a 16-bit trie */
                *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            } else {
                toUTrie2->trie=utrie2_open(toUTrie2->initialValue,
                                           toUTrie2->errorValue, pErrorCode);
            }
            break;
        default:
            break;
        }
    }
}
