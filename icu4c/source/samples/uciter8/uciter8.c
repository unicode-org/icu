/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uciter8.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003jan10
*   created by: Markus W. Scherer
*
*   This file contains sample code that illustrates reading
*   8-bit Unicode text leniently, accepting a mix of UTF-8 and CESU-8
*   and also accepting single surrogates.
*   UTF-8-style macros are defined as well as a UCharIterator.
*   The macros are incomplete (do not assemble code points from pairs of
*   surrogates, see comment below)
*   but sufficient for the iterator.
*/

#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/uiter.h"

#define log_err printf

/* lenient UTF-8/CESU-8 macros ---------------------------------------------- */

/*
 * This code leniently reads 8-bit Unicode strings,
 * which could contain a mix of UTF-8 and CESU-8.
 * More precisely:
 * - supplementary code points may be encoded with dedicated 4-byte sequences
 *   (UTF-8 style)
 * - supplementary code points may be encoded with
 *   pairs of 3-byte sequences, one for each surrogate of the UTF-16 form
 *   (CESU-8 style)
 * - single surrogates are allowed, encoded with their "natural" 3-byte sequences
 *
 * Limitation:
 * Right now, the macros do not attempt to assemble code points from pairs of
 * separately encoded surrogates.
 * This would not be sufficient for processing based on these macros,
 * but it is sufficient for a UCharIterator that returns only UChars anyway.
 *
 * The code is copied and modified from utf_impl.c and utf8.h.
 * The "strict" argument in the implementation functions is completely removed,
 * using the "<0" branch from the original code.
 * Checks for surrogate code points are removed for the leniency
 * described above.
 */

static const UChar32
lenient8_minLegal[4]={ 0, 0x80, 0x800, 0x10000 };

static UChar32
lenient8_nextCharSafeBody(const uint8_t *s, int32_t *pi, int32_t length, UChar32 c) {
    int32_t i=*pi;
    uint8_t count=U8_COUNT_TRAIL_BYTES(c);
    if((i)+count<=(length)) {
        uint8_t trail, illegal=0;

        U8_MASK_LEAD_BYTE((c), count);
        /* count==0 for illegally leading trail bytes and the illegal bytes 0xfe and 0xff */
        switch(count) {
        /* each branch falls through to the next one */
        case 5:
        case 4:
            /* count>=4 is always illegal: no more than 3 trail bytes in Unicode's UTF-8 */
            illegal=1;
            break;
        case 3:
            trail=s[(i)++];
            (c)=((c)<<6)|(trail&0x3f);
            if(c<0x110) {
                illegal|=(trail&0xc0)^0x80;
            } else {
                /* code point>0x10ffff, outside Unicode */
                illegal=1;
                break;
            }
        case 2:
            trail=s[(i)++];
            (c)=((c)<<6)|(trail&0x3f);
            illegal|=(trail&0xc0)^0x80;
        case 1:
            trail=s[(i)++];
            (c)=((c)<<6)|(trail&0x3f);
            illegal|=(trail&0xc0)^0x80;
            break;
        case 0:
            return U_SENTINEL;
        /* no default branch to optimize switch()  - all values are covered */
        }

        /* correct sequence - all trail bytes have (b7..b6)==(10)? */
        /* illegal is also set if count>=4 */
        if(illegal || (c)<lenient8_minLegal[count]) {
            /* error handling */
            uint8_t errorCount=count;
            /* don't go beyond this sequence */
            i=*pi;
            while(count>0 && U8_IS_TRAIL(s[i])) {
                ++(i);
                --count;
            }
            c=U_SENTINEL;
        }
    } else /* too few bytes left */ {
        /* error handling */
        int32_t i0=i;
        /* don't just set (i)=(length) in case there is an illegal sequence */
        while((i)<(length) && U8_IS_TRAIL(s[i])) {
            ++(i);
        }
        c=U_SENTINEL;
    }
    *pi=i;
    return c;
}

static UChar32
lenient8_prevCharSafeBody(const uint8_t *s, int32_t start, int32_t *pi, UChar32 c) {
    int32_t i=*pi;
    uint8_t b, count=1, shift=6;

    /* extract value bits from the last trail byte */
    c&=0x3f;

    for(;;) {
        if(i<=start) {
            /* no lead byte at all */
            return U_SENTINEL;
        }

        /* read another previous byte */
        b=s[--i];
        if((uint8_t)(b-0x80)<0x7e) { /* 0x80<=b<0xfe */
            if(b&0x40) {
                /* lead byte, this will always end the loop */
                uint8_t shouldCount=U8_COUNT_TRAIL_BYTES(b);

                if(count==shouldCount) {
                    /* set the new position */
                    *pi=i;
                    U8_MASK_LEAD_BYTE(b, count);
                    c|=(UChar32)b<<shift;
                    if(count>=4 || c>0x10ffff || c<lenient8_minLegal[count]) {
                        /* illegal sequence */
                        if(count>=4) {
                            count=3;
                        }
                        c=U_SENTINEL;
                    } else {
                        /* exit with correct c */
                    }
                } else {
                    /* the lead byte does not match the number of trail bytes */
                    /* only set the position to the lead byte if it would
                       include the trail byte that we started with */
                    if(count<shouldCount) {
                        *pi=i;
                    }
                    c=U_SENTINEL;
                }
                break;
            } else if(count<5) {
                /* trail byte */
                c|=(UChar32)(b&0x3f)<<shift;
                ++count;
                shift+=6;
            } else {
                /* more than 5 trail bytes is illegal */
                c=U_SENTINEL;
                break;
            }
        } else {
            /* single-byte character precedes trailing bytes */
            c=U_SENTINEL;
            break;
        }
    }
    return c;
}

#define L8_NEXT(s, i, length, c) { \
    (c)=(s)[(i)++]; \
    if((c)>=0x80) { \
        if(U8_IS_LEAD(c)) { \
            (c)=lenient8_nextCharSafeBody(s, &(i), (int32_t)(length), c); \
        } else { \
            (c)=U_SENTINEL; \
        } \
    } \
}

#define L8_PREV(s, start, i, c) { \
    (c)=(s)[--(i)]; \
    if((c)>=0x80) { \
        if((c)<=0xbf) { \
            (c)=lenient8_prevCharSafeBody(s, start, &(i), c); \
        } else { \
            (c)=U_SENTINEL; \
        } \
    } \
}

/* lenient-8 UCharIterator -------------------------------------------------- */

/*
 * This is a copy of the UTF-8 UCharIterator in uiter.cpp,
 * except that it uses the lenient-8-bit-Unicode macros above.
 */

/*
 * Minimal implementation:
 * Maintain a single-UChar buffer for an additional surrogate.
 * The caller must not modify start and limit because they are used internally.
 *
 * Use UCharIterator fields as follows:
 *   context        pointer to UTF-8 string
 *   length         UTF-16 length of the string; -1 until lazy evaluation
 *   start          current UTF-8 index
 *   index          current UTF-16 index
 *   limit          UTF-8 length of the string
 *   reservedField  supplementary code point
 *
 * Since UCharIterator delivers 16-bit code units, the iteration can be
 * currently in the middle of the byte sequence for a supplementary code point.
 * In this case, reservedField will contain that code point and start will
 * point to after the corresponding byte sequence.
 * Otherwise, reservedField will be 0.
 */

/*
 * Possible optimization for NUL-terminated UTF-8 and UTF-16 strings:
 * Add implementations that do not call strlen() for iteration but check for NUL.
 */

static int32_t U_CALLCONV
lenient8IteratorGetIndex(UCharIterator *iter, UCharIteratorOrigin origin) {
    switch(origin) {
    case UITER_ZERO:
    case UITER_START:
        return 0;
    case UITER_CURRENT:
        return iter->index;
    case UITER_LIMIT:
    case UITER_LENGTH:
        if(iter->length<0) {
            const uint8_t *s;
            UChar32 c;
            int32_t i, limit, length;

            s=(const uint8_t *)iter->context;
            i=iter->start;
            limit=iter->limit;
            length=iter->index;
            if(iter->reservedField!=0) {
                iter->reservedField=0;
                ++length;
            }
            while(i<limit) {
                L8_NEXT(s, i, limit, c);
                if(c<=0xffff) {
                    ++length;
                } else {
                    length+=2;
                }
            }
            iter->length=length;
        }
        return iter->length;
    default:
        /* not a valid origin */
        /* Should never get here! */
        return -1;
    }
}

static int32_t U_CALLCONV
lenient8IteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    const uint8_t *s;
    UChar32 c;
    int32_t pos; /* requested UTF-16 index */
    int32_t i, limit; /* UTF-8 index & length */

    /* calculate the requested UTF-16 position */
    switch(origin) {
    case UITER_ZERO:
    case UITER_START:
        pos=delta;
        break;
    case UITER_CURRENT:
        pos=iter->index+delta;
        break;
    case UITER_LIMIT:
    case UITER_LENGTH:
        pos=lenient8IteratorGetIndex(iter, UITER_LENGTH)+delta;
        break;
    default:
        return -1;  /* Error */
    }

    /* shortcuts: pinning to the edges of the string */
    if(pos<=0) {
        iter->index=iter->start=iter->reservedField=0;
        return 0;
    } else if(iter->length>=0 && pos>=iter->length) {
        iter->index=iter->length;
        iter->start=iter->limit;
        iter->reservedField=0;
        return iter->index;
    }

    /* minimize the number of L8_NEXT/PREV operations */
    if(pos<iter->index/2) {
        /* go forward from the start instead of backward from the current index */
        iter->index=iter->start=iter->reservedField=0;
    } else if(iter->length>=0 && (iter->length-pos)<(pos-iter->index)) {
        /*
         * if we have the UTF-16 length and the new position is
         * closer to the end than the current index,
         * then go backward from the end instead of forward from the current index
         */
        iter->index=iter->length;
        iter->start=iter->limit;
        iter->reservedField=0;
    }

    delta=pos-iter->index;
    if(delta==0) {
        return iter->index; /* nothing to do */
    }

    /* move towards the requested position if possible */
    s=(const uint8_t *)iter->context;
    pos=iter->index;
    i=iter->start;
    limit=iter->limit;
    if(delta>0) {
        /* go forward */
        if(iter->reservedField!=0) {
            iter->reservedField=0;
            ++pos;
            --delta;
        }
        while(delta>0 && i<limit) {
            L8_NEXT(s, i, limit, c);
            if(c<0xffff) {
                ++pos;
                --delta;
            } else if(delta>=2) {
                pos+=2;
                delta-=2;
            } else /* delta==1 */ {
                /* stop in the middle of a supplementary code point */
                iter->reservedField=c;
                ++pos;
                break; /* delta=0; */
            }
        }
        if(i==limit && iter->length<0) {
            iter->length=pos;
        }
    } else /* delta<0 */ {
        /* go backward */
        if(iter->reservedField!=0) {
            iter->reservedField=0;
            --pos;
            ++delta;
        }
        while(delta<0 && i>0) {
            L8_PREV(s, 0, i, c);
            if(c<0xffff) {
                --pos;
                ++delta;
            } else if(delta<=-2) {
                pos-=2;
                delta+=2;
            } else /* delta==-1 */ {
                /* stop in the middle of a supplementary code point */
                iter->reservedField=c;
                --pos;
                break; /* delta=0; */
            }
        }
    }

    iter->start=i;
    return iter->index=pos;
}

static UBool U_CALLCONV
lenient8IteratorHasNext(UCharIterator *iter) {
    return iter->reservedField!=0 || iter->start<iter->limit;
}

static UBool U_CALLCONV
lenient8IteratorHasPrevious(UCharIterator *iter) {
    return iter->index>0;
}

static UChar32 U_CALLCONV
lenient8IteratorCurrent(UCharIterator *iter) {
    if(iter->reservedField!=0) {
        return U16_TRAIL(iter->reservedField);
    } else if(iter->start<iter->limit) {
        const uint8_t *s=(const uint8_t *)iter->context;
        UChar32 c;
        int32_t i=iter->start;

        L8_NEXT(s, i, iter->limit, c);
        if(c<0) {
            return 0xfffd;
        } else if(c<=0xffff) {
            return c;
        } else {
            return U16_LEAD(c);
        }
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
lenient8IteratorNext(UCharIterator *iter) {
    if(iter->reservedField!=0) {
        UChar trail=U16_TRAIL(iter->reservedField);
        iter->reservedField=0;
        ++iter->index;
        return trail;
    } else if(iter->start<iter->limit) {
        const uint8_t *s=(const uint8_t *)iter->context;
        UChar32 c;

        L8_NEXT(s, iter->start, iter->limit, c);
        ++iter->index;
        if(iter->length<0 && iter->start==iter->limit) {
            iter->length= c<=0xffff ? iter->index : iter->index+1;
        }
        if(c<0) {
            return 0xfffd;
        } else if(c<=0xffff) {
            return c;
        } else {
            iter->reservedField=c;
            return U16_LEAD(c);
        }
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
lenient8IteratorPrevious(UCharIterator *iter) {
    if(iter->reservedField!=0) {
        UChar lead=U16_LEAD(iter->reservedField);
        iter->reservedField=0;
        iter->start-=4; /* we stayed behind the supplementary code point; go before it now */
        --iter->index;
        return lead;
    } else if(iter->start>0) {
        const uint8_t *s=(const uint8_t *)iter->context;
        UChar32 c;

        L8_PREV(s, 0, iter->start, c);
        --iter->index;
        if(c<0) {
            return 0xfffd;
        } else if(c<=0xffff) {
            return c;
        } else {
            iter->start+=4; /* back to behind this supplementary code point for consistent state */
            iter->reservedField=c;
            return U16_TRAIL(c);
        }
    } else {
        return U_SENTINEL;
    }
}

static const UCharIterator lenient8Iterator={
    0, 0, 0, 0, 0, 0,
    lenient8IteratorGetIndex,
    lenient8IteratorMove,
    lenient8IteratorHasNext,
    lenient8IteratorHasPrevious,
    lenient8IteratorCurrent,
    lenient8IteratorNext,
    lenient8IteratorPrevious,
    0
};

static void
uiter_setLenient8(UCharIterator *iter, const char *s, int32_t length) {
    if(iter!=0) {
        if(s!=0 && length>=-1) {
            *iter=lenient8Iterator;
            iter->context=s;
            if(length>=0) {
                iter->limit=length;
            } else {
                iter->limit=strlen(s);
            }
            iter->length= iter->limit==0 ? 0 : -1;
        } else {
            /* set no-op iterator */
            uiter_setUTF8(iter, NULL, 0);
        }
    }
}

/* UCharIterator test ------------------------------------------------------- */

/*
 * The following code is a copy of the UCharIterator test code in
 * source/test/cintltst/custrtst.c,
 * testing the lenient-8 iterator instead of the UTF-8 one.
 */

/*
 * Compare results from two iterators, should be same.
 * Assume that the text is not empty and that
 * iteration start==0 and iteration limit==length.
 */
static void
compareIterators(UCharIterator *iter1, const char *n1,
                 UCharIterator *iter2, const char *n2) {
    int32_t i, pos1, pos2, middle, length;
    UChar32 c1, c2;

    /* compare lengths */
    length=iter1->getIndex(iter1, UITER_LENGTH);
    pos2=iter2->getIndex(iter2, UITER_LENGTH);
    if(length!=pos2) {
        log_err("%s->getIndex(length)=%d != %d=%s->getIndex(length)\n", n1, length, pos2, n2);
        return;
    }

    /* set into the middle */
    middle=length/2;

    pos1=iter1->move(iter1, middle, UITER_ZERO);
    if(pos1!=middle) {
        log_err("%s->move(from 0 to middle %d)=%d does not move to the middle\n", n1, middle, pos1);
        return;
    }

    pos2=iter2->move(iter2, middle, UITER_ZERO);
    if(pos2!=middle) {
        log_err("%s->move(from 0 to middle %d)=%d does not move to the middle\n", n2, middle, pos2);
        return;
    }

    /* test current() */
    c1=iter1->current(iter1);
    c2=iter2->current(iter2);
    if(c1!=c2) {
        log_err("%s->current()=U+%04x != U+%04x=%s->current() at middle=%d\n", n1, c1, c2, n2, middle);
        return;
    }

    /* move forward 3 UChars */
    for(i=0; i<3; ++i) {
        c1=iter1->next(iter1);
        c2=iter2->next(iter2);
        if(c1!=c2) {
            log_err("%s->next()=U+%04x != U+%04x=%s->next() at %d (started in middle)\n", n1, c1, c2, n2, iter1->getIndex(iter1, UITER_CURRENT));
            return;
        }
    }

    /* move backward 5 UChars */
    for(i=0; i<5; ++i) {
        c1=iter1->previous(iter1);
        c2=iter2->previous(iter2);
        if(c1!=c2) {
            log_err("%s->previous()=U+%04x != U+%04x=%s->previous() at %d (started in middle)\n", n1, c1, c2, n2, iter1->getIndex(iter1, UITER_CURRENT));
            return;
        }
    }

    /* iterate forward from the beginning */
    pos1=iter1->move(iter1, 0, UITER_START);
    if(pos1<0) {
        log_err("%s->move(start) failed\n", n1);
        return;
    }
    if(!iter1->hasNext(iter1)) {
        log_err("%s->hasNext() at the start returns FALSE\n", n1);
        return;
    }

    pos2=iter2->move(iter2, 0, UITER_START);
    if(pos2<0) {
        log_err("%s->move(start) failed\n", n2);
        return;
    }
    if(!iter2->hasNext(iter2)) {
        log_err("%s->hasNext() at the start returns FALSE\n", n2);
        return;
    }

    do {
        c1=iter1->next(iter1);
        c2=iter2->next(iter2);
        if(c1!=c2) {
            log_err("%s->next()=U+%04x != U+%04x=%s->next() at %d\n", n1, c1, c2, n2, iter1->getIndex(iter1, UITER_CURRENT));
            return;
        }
    } while(c1>=0);

    if(iter1->hasNext(iter1)) {
        log_err("%s->hasNext() at the end returns TRUE\n", n1);
        return;
    }
    if(iter2->hasNext(iter2)) {
        log_err("%s->hasNext() at the end returns TRUE\n", n2);
        return;
    }

    /* back to the middle */
    pos1=iter1->move(iter1, middle, UITER_ZERO);
    if(pos1!=middle) {
        log_err("%s->move(from end to middle %d)=%d does not move to the middle\n", n1, middle, pos1);
        return;
    }

    pos2=iter2->move(iter2, middle, UITER_ZERO);
    if(pos2!=middle) {
        log_err("%s->move(from end to middle %d)=%d does not move to the middle\n", n2, middle, pos2);
        return;
    }

    /* move to index 1 */
    pos1=iter1->move(iter1, 1, UITER_ZERO);
    if(pos1!=1) {
        log_err("%s->move(from middle %d to 1)=%d does not move to 1\n", n1, middle, pos1);
        return;
    }

    pos2=iter2->move(iter2, 1, UITER_ZERO);
    if(pos2!=1) {
        log_err("%s->move(from middle %d to 1)=%d does not move to 1\n", n2, middle, pos2);
        return;
    }

    /* iterate backward from the end */
    pos1=iter1->move(iter1, 0, UITER_LIMIT);
    if(pos1<0) {
        log_err("%s->move(limit) failed\n", n1);
        return;
    }
    if(!iter1->hasPrevious(iter1)) {
        log_err("%s->hasPrevious() at the end returns FALSE\n", n1);
        return;
    }

    pos2=iter2->move(iter2, 0, UITER_LIMIT);
    if(pos2<0) {
        log_err("%s->move(limit) failed\n", n2);
        return;
    }
    if(!iter2->hasPrevious(iter2)) {
        log_err("%s->hasPrevious() at the end returns FALSE\n", n2);
        return;
    }

    do {
        c1=iter1->previous(iter1);
        c2=iter2->previous(iter2);
        if(c1!=c2) {
            log_err("%s->previous()=U+%04x != U+%04x=%s->previous() at %d\n", n1, c1, c2, n2, iter1->getIndex(iter1, UITER_CURRENT));
            return;
        }
    } while(c1>=0);

    if(iter1->hasPrevious(iter1)) {
        log_err("%s->hasPrevious() at the start returns TRUE\n", n1);
        return;
    }
    if(iter2->hasPrevious(iter2)) {
        log_err("%s->hasPrevious() at the start returns TRUE\n", n2);
        return;
    }
}

static void
TestLenient8Iterator() {
    static const UChar text[]={
        0x61, 0x62, 0x63,
        /* dffd 107fd             d801    dffd - in UTF-16, U+107fd=<d801 dffd> */
        0xdffd, 0xd801, 0xdffd, 0xd801, 0xdffd, 
        0x78, 0x79, 0x7a, 0
    };
    static const uint8_t bytes[]={
        0x61, 0x62, 0x63,
        /* dffd            107fd                    d801               dffd - mixture */
        0xed, 0xbf, 0xbd,  0xf0, 0x90, 0x9f, 0xbd,  0xed, 0xa0, 0x81,  0xed, 0xbf, 0xbd,
        0x78, 0x79, 0x7a, 0
    };

    UCharIterator iter1, iter2;
    UChar32 c1, c2;

    puts("test a UCharIterator for lenient 8-bit Unicode (accept single surrogates)");

    /* compare the same string between UTF-16 and lenient-8 UCharIterators */
    uiter_setString(&iter1, text, -1);
    uiter_setLenient8(&iter2, (const char *)bytes, sizeof(bytes)-1);
    compareIterators(&iter1, "UTF16Iterator", &iter2, "Lenient8Iterator");

    /* try again with length=-1 */
    uiter_setLenient8(&iter2, (const char *)bytes, -1);
    compareIterators(&iter1, "UTF16Iterator", &iter2, "Lenient8Iterator_1");

    puts("no output so far means that the lenient-8 iterator works fine");

    puts("iterate forward:\nUTF-16\tlenient-8");
    iter1.move(&iter1, 0, UITER_START);
    iter2.move(&iter2, 0, UITER_START);
    for(;;) {
        c1=iter1.next(&iter1);
        c2=iter2.next(&iter2);
        if(c1<0 && c2<0) {
            break;
        }
        if(c1<0) {
            printf("\t%04x\n", c2);
        } else if(c2<0) {
            printf("%04x\n", c1);
        } else {
            printf("%04x\t%04x\n", c1, c2);
        }
    }
}

extern int
main(int argc, const char *argv[]) {
    TestLenient8Iterator();
    return 0;
}
