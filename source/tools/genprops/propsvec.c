/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
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
#include "uprops.h"
#include "propsvec.h"

static uint32_t *
_findRow(uint32_t *pv, uint32_t rangeStart) {
    uint32_t *row;
    int32_t columns, i, start, limit;

    columns=(int32_t)pv[UPVEC_COLUMNS];
    limit=(int32_t)pv[UPVEC_ROWS];
    pv+=UPVEC_HEADER_LENGTH;

    /* do a binary search for the start of the range */
    start=0;
    while(start<limit-1) {
        i=(start+limit)/2;
        row=pv+i*columns;
        if(rangeStart<row[0]) {
            limit=i;
        } else if(rangeStart<row[1]) {
            return row;
        } else {
            start=i;
        }
    }

    /* must be found because all ranges together always cover all of Unicode */
    return pv+start*columns;
}

U_CFUNC uint32_t *
upvec_open(int32_t columns, int32_t maxRows) {
    uint32_t *pv, *row;
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
        pv[UPVEC_ROWS]=1;
        pv[UPVEC_RESERVED]=0;

        /* set initial row */
        row=pv+UPVEC_HEADER_LENGTH;
        *row++=0;
        *row++=0x110000;
        columns-=2;
        do {
            *row++=0;
        } while(--columns>0);
    }
    return pv;
}

U_CFUNC void
upvec_close(uint32_t *pv) {
    if(pv==NULL) {
        uprv_free(pv);
    }
}

U_CFUNC UBool
upvec_setValue(uint32_t *pv,
               uint32_t start, uint32_t limit,
               int32_t column,
               uint32_t value, uint32_t mask,
               UErrorCode *pErrorCode) {
    uint32_t *firstRow, *lastRow;
    int32_t columns;
    UBool splitFirstRow, splitLastRow;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return FALSE;
    }

    if( pv==NULL ||
        start>limit || limit>0x110000 ||
        column<0 || (uint32_t)(column+1)>=pv[UPVEC_COLUMNS]
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return FALSE;
    }
    if(start==limit) {
        /* empty range, nothing to do */
        return TRUE;
    }

    /* initialize */
    columns=pv[UPVEC_COLUMNS];
    column+=2; /* skip range start and limit columns */
    value&=mask;

    /* find the rows whose ranges overlap with the input range */

    /* find the first row, always successful */
    firstRow=_findRow(pv, start);

    /* find the last row, always successful */
    lastRow=firstRow;
    while(limit>lastRow[1]) {
        lastRow+=columns;
    }

    /*
     * Rows need to be split if they partially overlap with the
     * input range (only possible for the first and last rows)
     * and if their value differs from the input value.
     */
    splitFirstRow= (UBool)(start!=firstRow[0] && value!=(firstRow[column]&mask));
    splitLastRow= (UBool)(limit!=lastRow[1] && value!=(lastRow[column]&mask));

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
            firstRow[1]=firstRow[columns]=start;
            firstRow+=columns;
        }

        /* split the last row */
        if(splitLastRow) {
            /* copy the last row data */
            uprv_memcpy(lastRow+columns, lastRow, columns*4);

            /* split the range and move the firstRow pointer */
            lastRow[1]=lastRow[columns]=limit;
        }
    }

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

U_CFUNC uint32_t *
upvec_getRow(uint32_t *pv, int32_t rowIndex,
             uint32_t *pRangeStart, uint32_t *pRangeLimit) {
    uint32_t *row;
    int32_t columns;

    if(pv==NULL || rowIndex<0 || rowIndex>=(int32_t)pv[UPVEC_ROWS]) {
        return NULL;
    }

    columns=pv[UPVEC_COLUMNS];
    row=pv+UPVEC_HEADER_LENGTH+rowIndex*columns;
    if(pRangeStart!=NULL) {
        *pRangeStart=row[0];
    }
    if(pRangeLimit!=NULL) {
        *pRangeLimit=row[1];
    }
    return row+2;
}

static int
upvec_compareRows(const void *l, const void *r) {
    const uint32_t *left=(const uint32_t *)l, *right=(const uint32_t *)r;
    int32_t i, count, columns;

    count=columns=2+UPROPS_VECTOR_WORDS;

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

U_CFUNC int32_t
upvec_toTrie(uint32_t *pv, UNewTrie *trie, UErrorCode *pErrorCode) {
    uint32_t *row;
    int32_t columns, valueColumns, rows, count;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(pv==NULL || trie==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    row=pv+UPVEC_HEADER_LENGTH;
    columns=(int32_t)pv[UPVEC_COLUMNS];
    rows=(int32_t)pv[UPVEC_ROWS];

    /* sort the properties vectors to find unique vector values */
    if(rows>1) {
        qsort(pv+UPVEC_HEADER_LENGTH, rows, columns*4, upvec_compareRows);
    }

    /*
     * Move vector contents up to a contiguous array with only unique
     * vector values, and set indexes to those values into the trie.
     *
     * This destroys the Properties Vector structure and replaces it
     * with an array of just vector values.
     */
    valueColumns=columns-2; /* not counting start & limit */
    count=-valueColumns;

    do {
        /* add a new values vector if it is different from the current one */
        if(count<0 || 0!=uprv_memcmp(row+2, pv+count, valueColumns*4)) {
            count+=valueColumns;
            uprv_memmove(pv+count, row+2, valueColumns*4);
        }

        if(count>0 && !utrie_setRange32(trie, (UChar32)row[0], (UChar32)row[1], (uint32_t)count, FALSE)) {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            return 0;
        }

        row+=columns;
    } while(--rows>0);

    return count+valueColumns;
}
