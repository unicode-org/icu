/*  
*******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_wgt.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001mar08
*   created by: Markus W. Scherer
*
*   This file contains code for allocating n collation element weights
*   between two exclusive limits.
*   It is used only internally by ucol_bld.
*/

#ifdef UCOL_DEBUG
#   include <stdio.h>
#endif

/* we are using qsort() */
#include <stdlib.h>

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucol_wgt.h"

#if defined(UCOL_DEBUG) && defined(WIN32)
    /* turn off "unreferenced formal parameter" */
#   pragma warning(disable: 4100)
#endif

/* collation element weight allocation -------------------------------------- */

/* helper functions for CE weights */

static int32_t
lengthOfWeight(uint32_t weight) {
    if((weight&0xffffff)==0) {
        return 1;
    } else if((weight&0xffff)==0) {
        return 2;
    } else if((weight&0xff)==0) {
        return 3;
    } else {
        return 4;
    }
}

static uint32_t
getWeightTrail(uint32_t weight, int32_t length) {
    return (uint32_t)(weight>>(8*(4-length)))&0xff;
}

static uint32_t
setWeightTrail(uint32_t weight, int32_t length, uint32_t trail) {
    length=8*(4-length);
    return (uint32_t)((weight&(0xffffff00<<length))|(trail<<length));
}

static uint32_t
getWeightByte(uint32_t weight, int32_t index) {
    return getWeightTrail(weight, index); /* same calculation */
}

static uint32_t
setWeightByte(uint32_t weight, int32_t index, uint32_t byte) {
    uint32_t mask; /* 0xffffffff except a 00 "hole" for the index-th byte */

    index*=8;
    mask=0xffffffff>>index;
    index=32-index;
    mask|=0xffffff00<<index;
    return (uint32_t)((weight&mask)|(byte<<index));
}

static uint32_t
truncateWeight(uint32_t weight, int32_t length) {
    return (uint32_t)(weight&(0xffffffff<<(8*(4-length))));
}

static uint32_t
incWeightTrail(uint32_t weight, int32_t length) {
    return (uint32_t)(weight+(1UL<<(8*(4-length))));
}

static uint32_t
decWeightTrail(uint32_t weight, int32_t length) {
    return (uint32_t)(weight-(1UL<<(8*(4-length))));
}

static uint32_t
incWeight(uint32_t weight, int32_t length) {
    uint32_t byte;

    for(;;) {
        byte=getWeightByte(weight, length);
        if(byte<0xff) {
            return setWeightByte(weight, length, byte+1);
        } else {
            /* roll over, set this byte to 2 and increment the previous one */
            weight=setWeightByte(weight, length, 2);
            --length;
        }
    }
}

static int32_t
lengthenRange(WeightRange *range) {
    int32_t length;

    length=range->length2+1;
    range->start=setWeightTrail(range->start, length, 2);
    range->end=setWeightTrail(range->end, length, 0xff);
    range->count2*=254;
    range->length2=length;
    return length;
}

/* for qsort: sort ranges in weight order */
static int
compareRanges(const void *left, const void *right) {
    uint32_t l, r;

    l=((const WeightRange *)left)->start;
    r=((const WeightRange *)right)->start;
    if(l<r) {
        return -1;
    } else if(l>r) {
        return 1;
    } else {
        return 0;
    }
}

/*
 * take two CE weights and calculate the
 * possible ranges of weights between the two limits, excluding them
 * for weights with up to 4 bytes there are up to 2*4-1=7 ranges
 */
static int32_t
getWeightRanges(uint32_t lowerLimit, uint32_t upperLimit, WeightRange ranges[7]) {
    WeightRange lower[5], middle, upper[5]; /* [0] and [1] are not used - this simplifies indexing */
    uint32_t weight, trail;
    int32_t length, lowerLength, upperLength, rangeCount;

    /* assume that both lowerLimit & upperLimit are not 0 */

    /* get the lengths of the limits */
    lowerLength=lengthOfWeight(lowerLimit);
    upperLength=lengthOfWeight(upperLimit);

#ifdef UCOL_DEBUG
    printf("length of lower limit 0x%08lx is %ld\n", lowerLimit, lowerLength);
    printf("length of upper limit 0x%08lx is %ld\n", upperLimit, upperLength);
#endif

    if(lowerLimit>=upperLimit) {
#ifdef UCOL_DEBUG
        printf("error: no space between lower & upper limits\n");
#endif
        return 0;
    }

    /* check that neither is a prefix of the other */
    if(lowerLength<upperLength) {
        if(lowerLimit==truncateWeight(upperLimit, lowerLength)) {
#ifdef UCOL_DEBUG
            printf("error: lower limit 0x%08lx is a prefix of upper limit 0x%08lx\n", lowerLimit, upperLimit);
#endif
            return 0;
        }
    }
    /* if the upper limit is a prefix of the lower limit then the earlier test lowerLimit>=upperLimit has caught it */

    /* reset local variables */
    uprv_memset(lower, 0, sizeof(lower));
    uprv_memset(&middle, 0, sizeof(middle));
    uprv_memset(upper, 0, sizeof(upper));

    /*
     * With the limit lengths of 1..4, there are up to 7 ranges for allocation:
     * range     minimum length
     * lower[4]  4
     * lower[3]  3
     * lower[2]  2
     * middle    1
     * upper[2]  2
     * upper[3]  3
     * upper[4]  4
     *
     * We are now going to calculate up to 7 ranges.
     * Some of them will typically overlap, so we will then have to merge and eliminate ranges.
     */
    weight=lowerLimit;
    for(length=lowerLength; length>=2; --length) {
        trail=getWeightTrail(weight, length);
        if(trail<0xff) {
            lower[length].start=incWeightTrail(weight, length);
            lower[length].end=setWeightTrail(weight, length, 0xff);
            lower[length].length=length;
            lower[length].count=0xff-trail;
        }
        weight=truncateWeight(weight, length-1);
    }
    middle.start=incWeightTrail(weight, 1);

    weight=upperLimit;
    for(length=upperLength; length>=2; --length) {
        trail=getWeightTrail(weight, length);
        if(trail>2) {
            upper[length].start=setWeightTrail(weight, length, 2);
            upper[length].end=decWeightTrail(weight, length);
            upper[length].length=length;
            upper[length].count=trail-2;
        }
        weight=truncateWeight(weight, length-1);
    }
    middle.end=decWeightTrail(weight, 1);

    /* set the middle range */
    middle.length=1;
    if(middle.end>=middle.start) {
        middle.count=(int32_t)((middle.end-middle.start)>>24)+1;
    } else {
        /* eliminate overlaps */
        uint32_t start, end;

        /* remove the middle range */
        middle.count=0;

        /* reduweight or remove the lower ranges that go beyond upperLimit */
        for(length=4; length>=2; --length) {
            if(lower[length].count>0 && upper[length].count>0) {
                start=upper[length].start;
                end=lower[length].end;

                if(end>=start || incWeight(end, length)==start) {
                    /* lower and upper ranges collide or are directly adjacent: merge these two and remove all shorter ranges */
                    start=lower[length].start;
                    end=lower[length].end=upper[length].end;
                    /* merging directly adjacent ranges needs to subtract the 0/1 gaps in between; it may result in a range with count>254 */
                    lower[length].count=
                        (int32_t)(getWeightTrail(end, length)-getWeightTrail(start, length)+1+
                                  254*(getWeightByte(end, length-1)-getWeightByte(start, length-1)));
                    upper[length].count=0;
                    while(--length>=2) {
                        lower[length].count=upper[length].count=0;
                    }
                    break;
                }
            }
        }
    }

#ifdef UCOL_DEBUG
    /* print ranges */
    for(length=4; length>=2; --length) {
        if(lower[length].count>0) {
            printf("lower[%ld] .start=0x%08lx .end=0x%08lx .count=%ld\n", length, lower[length].start, lower[length].end, lower[length].count);
        }
    }
    if(middle.count>0) {
        printf("middle   .start=0x%08lx .end=0x%08lx .count=%ld\n", middle.start, middle.end, middle.count);
    }
    for(length=2; length<=4; ++length) {
        if(upper[length].count>0) {
            printf("upper[%ld] .start=0x%08lx .end=0x%08lx .count=%ld\n", length, upper[length].start, upper[length].end, upper[length].count);
        }
    }
#endif

    /* copy the ranges, shortest first, into the result array */
    rangeCount=0;
    if(middle.count>0) {
        uprv_memcpy(ranges, &middle, sizeof(WeightRange));
        rangeCount=1;
    }
    for(length=2; length<=4; ++length) {
        /* copy upper first so that later the middle range is more likely the first one to use */
        if(upper[length].count>0) {
            uprv_memcpy(ranges+rangeCount, upper+length, sizeof(WeightRange));
            ++rangeCount;
        }
        if(lower[length].count>0) {
            uprv_memcpy(ranges+rangeCount, lower+length, sizeof(WeightRange));
            ++rangeCount;
        }
    }
    return rangeCount;
}

/*
 * call getWeightRanges and then determine heuristically
 * which ranges to use for a given number of weights between (excluding)
 * two limits
 */
U_CFUNC int32_t
ucol_allocWeights(uint32_t lowerLimit, uint32_t upperLimit, uint32_t n, WeightRange ranges[7]) {
    /* 254 to the power of index */
    static const uint32_t powers254[5]={
        1, 254, 254*254, 254UL*254*254, 254UL*254*254*254
    };

    uint32_t lengthCounts[6]; /* [0] unused, [5] to make index checks unnecessary */
    uint32_t maxCount;
    int32_t i, rangeCount, minLength, maxLength;

#ifdef UCOL_DEBUG
    puts("");
#endif

    rangeCount=getWeightRanges(lowerLimit, upperLimit, ranges);
    if(rangeCount<=0) {
#ifdef UCOL_DEBUG
        printf("error: unable to get Weight ranges\n");
#endif
        return 0;
    }

    /* what is the maximum number of weights with these ranges? */
    maxCount=0;
    for(i=0; i<rangeCount; ++i) {
        maxCount+=(uint32_t)ranges[i].count*powers254[4-ranges[i].length];
    }
    if(maxCount>=n) {
#ifdef UCOL_DEBUG
        printf("the maximum number of %lu weights is sufficient for n=%lu\n", maxCount, n);
#endif
    } else {
#ifdef UCOL_DEBUG
        printf("error: the maximum number of %lu weights is insufficient for n=%lu\n", maxCount, n);
#endif
        return 0;
    }

    /* set the length2 and count2 fields */
    for(i=0; i<rangeCount; ++i) {
        ranges[i].length2=ranges[i].length;
        ranges[i].count2=(uint32_t)ranges[i].count;
    }

    /* try until we find suitably large ranges */
    for(;;) {
        /* get the smallest number of bytes in a range */
        minLength=ranges[0].length2;

        /* sum up the number of elements that fit into ranges of each byte length */
        uprv_memset(lengthCounts, 0, sizeof(lengthCounts));
        for(i=0; i<rangeCount; ++i) {
            lengthCounts[ranges[i].length2]+=ranges[i].count2;
        }

        /* now try to allocate n elements in the available short ranges */
        if(n<=(lengthCounts[minLength]+lengthCounts[minLength+1])) {
            /* trivial cases, use the first few ranges */
            maxCount=0;
            rangeCount=0;
            do {
                maxCount+=ranges[rangeCount].count2;
                ++rangeCount;
            } while(n>maxCount);
#ifdef UCOL_DEBUG
            printf("take first %ld ranges\n", rangeCount);
#endif
            break;
        } else if(n<=ranges[0].count2*254) {
            /* easy case, just make this one range large enough by lengthening it once more, possibly split it */
            uint32_t count1, count2, power254_1, power254;

            rangeCount=1;
            maxLength=minLength+1;

            /* calculate how to split the range between maxLength-1 (count1) and maxLength (count2) */
            power254_1=powers254[minLength-ranges[0].length];
            power254=power254_1*254;
            count2=(n+power254-1)/power254;
            count1=ranges[0].count-count2;

            /* split the range */
#ifdef UCOL_DEBUG
            printf("split the first range %ld:%ld\n", count1, count2);
#endif
            if(count1<1) {
                /* lengthen the entire range to maxLength */
                lengthenRange(ranges);
            } else {
                /* really split the range */
                uint32_t byte;

                /* create a new range with the end and initial and current length of the old one */
                rangeCount=2;
                ranges[1].end=ranges[0].end;
                ranges[1].length=ranges[0].length;
                ranges[1].length2=minLength;

                /* set the end of the first range according to count1 */
                i=ranges[0].length;
                byte=getWeightByte(ranges[0].start, i)+count1-1;

                /* ranges[0].count and count1 may be >254 from merging adjacent ranges; byte>0xff is possible */
                if(byte<=0xff) {
                    ranges[0].end=setWeightByte(ranges[0].start, i, byte);
                } else /* byte>0xff */ {
                    ranges[0].end=setWeightByte(incWeight(ranges[0].start, i-1), i, byte-254);
                }

                /* set the bytes in the end weight at length+1..length2 to 0xff */
                ranges[0].end|=(0xffffffff>>(8*i))&(0xffffffff<<(8*(4-minLength)));

                /* set the start of the second range to immediately follow the end of the first one */
                ranges[1].start=incWeight(ranges[0].end, minLength);

                /* set the count values (informational) */
                ranges[0].count=count1;
                ranges[1].count=count2;

                ranges[0].count2=count1*power254_1;
                ranges[1].count2=count2*power254_1; /* will be *254 when lengthened */

                /* lengthen the second range to maxLength */
                lengthenRange(ranges+1);
            }
            break;
        }

        /* no good match, lengthen all minLength ranges and iterate */
#ifdef UCOL_DEBUG
        printf("lengthen the short ranges from %ld bytes to %ld and iterate\n", minLength, minLength+1);
#endif
        for(i=0; ranges[i].length2==minLength; ++i) {
            lengthenRange(ranges+i);
        }
    }

    if(rangeCount>1) {
        /* sort the ranges by weight values */
        qsort(ranges, rangeCount, sizeof(WeightRange), compareRanges);
    }

#ifdef UCOL_DEBUG
    puts("final ranges:");
    for(i=0; i<rangeCount; ++i) {
        printf("ranges[%ld] .start=0x%08lx .end=0x%08lx .length=%ld .length2=%ld .count=%ld .count2=%lu\n",
               i, ranges[i].start, ranges[i].end, ranges[i].length, ranges[i].length2, ranges[i].count, ranges[i].count2);
    }
#endif

    return rangeCount;
}

/*
 * given a set of ranges calculated by ucol_allocWeights(),
 * iterate through the weights
 */
U_CFUNC uint32_t
ucol_nextWeight(WeightRange ranges[], int32_t *pRangeCount) {
    if(*pRangeCount<=0) {
        return 0xffffffff;
    } else {
        uint32_t weight;

        /* get the next weight */
        weight=ranges[0].start;
        if(weight==ranges[0].end) {
            /* this range is finished, remove it and move the following ones up */
            if(--*pRangeCount>0) {
                uprv_memmove(ranges, ranges+1, *pRangeCount*sizeof(WeightRange));
            }
        } else {
            /* increment the weight for the next value */
            ranges[0].start=incWeight(weight, ranges[0].length2);
        }

        return weight;
    }
}

#ifdef UCOL_DEBUG

static void
testAlloc(uint32_t lowerLimit, uint32_t upperLimit, uint32_t n, UBool enumerate) {
    WeightRange ranges[8];
    int32_t rangeCount;

    rangeCount=ucol_allocWeights(lowerLimit, upperLimit, n, ranges);
    if(enumerate) {
        uint32_t weight;

        while(n>0) {
            weight=ucol_nextWeight(ranges, &rangeCount);
            if(weight==0xffffffff) {
                printf("error: 0xffffffff with %lu more weights to go\n", n);
                break;
            }
            printf("    0x%08lx\n", weight);
            --n;
        }
    }
}

extern int
main(int argc, const char *argv[]) {
#if 0
#endif
    testAlloc(0x364214fc, 0x44b87d23, 5, FALSE);
    testAlloc(0x36421500, 0x44b87d23, 5, FALSE);
    testAlloc(0x36421500, 0x44b87d23, 20, FALSE);
    testAlloc(0x36421500, 0x44b87d23, 13700, FALSE);
    testAlloc(0x36421500, 0x38b87d23, 1, FALSE);
    testAlloc(0x36421500, 0x38b87d23, 20, FALSE);
    testAlloc(0x36421500, 0x38b87d23, 200, TRUE);
    testAlloc(0x36421500, 0x38b87d23, 13700, FALSE);
    testAlloc(0x36421500, 0x37b87d23, 13700, FALSE);
    testAlloc(0x36ef1500, 0x37b87d23, 13700, FALSE);
    testAlloc(0x36421500, 0x36b87d23, 13700, FALSE);
    testAlloc(0x36b87122, 0x36b87d23, 13700, FALSE);
    testAlloc(0x49000000, 0x4a600000, 13700, FALSE);
    testAlloc(0x9fffffff, 0xd0000000, 13700, FALSE);
    testAlloc(0x9fffffff, 0xd0000000, 67400, FALSE);
    testAlloc(0x9fffffff, 0xa0030000, 67400, FALSE);
    testAlloc(0x9fffffff, 0xa0030000, 40000, FALSE);
    testAlloc(0xa0000000, 0xa0030000, 40000, FALSE);
    testAlloc(0xa0031100, 0xa0030000, 40000, FALSE);
#if 0
#endif
    return 0;
}

#endif
