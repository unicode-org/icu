/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utf_impl.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep13
*   created by: Markus W. Scherer
*
*   This file provides implementation functions for macros in the utfXX.h
*   that would otherwise be too long as macros.
*/

#include "unicode/umachine.h"
#include "unicode/utf.h"

/*
 * This table could be replaced on many machines by
 * a few lines of assembler code using an
 * "index of first 0-bit from msb" instruction and
 * one or two more integer instructions.
 *
 * For example, on an i386, do something like
 * - MOV AL, leadByte
 * - NOT AL         (8-bit, leave b15..b8==0..0, reverse only b7..b0)
 * - MOV AH, 0
 * - BSR BX, AX     (16-bit)
 * - MOV AX, 6      (result)
 * - JZ finish      (ZF==1 if leadByte==0xff)
 * - SUB AX, BX (result)
 * -finish:
 * (BSR: Bit Scan Reverse, scans for a 1-bit, starting from the MSB)
 */
uint8_t U_EXPORT2
utf8_countTrailBytes[256]={
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,

    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    3, 3, 3, 3, 3, 3, 3, 3,
    4, 4, 4, 4,
    5, 5,
    0, 0    /* illegal bytes 0xfe and 0xff */
};

static UChar32
utf8_minRegular[4]={ 0, 0x80, 0x800, 0x10000 };

static UChar32
utf8_errorValue[6]={
    UTF8_ERROR_VALUE_1, UTF8_ERROR_VALUE_2, UTF_ERROR_VALUE, 0x10ffff,
    0x3ffffff, 0x7fffffff
};

U_CAPI UChar32 U_EXPORT2
utf8_nextCharSafeBody(const uint8_t *s, UTextOffset *pi, UTextOffset length, UChar32 c, UBool strict) {
    UTextOffset i=*pi;
    uint8_t count=UTF8_COUNT_TRAIL_BYTES(c);
    if((i)+count<=(length)) {
        uint8_t trail, illegal=0;

        UTF8_MASK_LEAD_BYTE((c), count);
        /* count==0 for illegally leading trail bytes and the illegal bytes 0xfe and 0xff */
        switch(count) {
        /* each branch falls through to the next one */
        case 5:
            trail=s[(i)++];
            (c)=((c)<<6)|(trail&0x3f);
            illegal|=(trail&0xc0)^0x80;
        case 4:
            trail=s[(i)++];
            (c)=((c)<<6)|(trail&0x3f);
            illegal|=(trail&0xc0)^0x80;
        case 3:
            trail=s[(i)++];
            (c)=((c)<<6)|(trail&0x3f);
            if(c<0x110) {
                illegal|=(trail&0xc0)^0x80;
            } else {
                /* code point>0x10ffff, outside Unicode */
                i+=2;
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
            illegal=1;
        /* no default branch to optimize switch()  - all values are covered */
            break;
        }

        /*
         * All the error handling should return a value
         * that needs count bytes so that UTF8_GET_CHAR_SAFE() works right.
         */

        /* correct sequence - all trail bytes have (b7..b6)==(10)? */
        if(illegal) {
            /* error handling */
            uint8_t errorCount=count;
            /* don't go beyond this sequence */
            (i)-=count;
            while(count>0 && UTF8_IS_TRAIL(s[i])) {
                ++(i);
                --count;
            }
            c=utf8_errorValue[errorCount-count];
        } else if((strict) &&
                  (UTF_IS_SURROGATE(c) ||
                   count>=4 || (c)<utf8_minRegular[count] ||
                   ((c)&0xfffe)==0xfffe)
        ) {
            /* irregular sequence */
            c=utf8_errorValue[count];
        }
    } else /* too few bytes left */ {
        /* error handling */
        UTextOffset i0=i;
        /* don't just set (i)=(length) in case there is an illegal sequence */
        while((i)<(length) && UTF8_IS_TRAIL(s[i])) {
            ++(i);
        }
        c=utf8_errorValue[i-i0];
    }
    *pi=i;
    return c;
}

U_CAPI UTextOffset U_EXPORT2
utf8_appendCharSafeBody(uint8_t *s, UTextOffset i, UTextOffset length, UChar32 c) {
    if((c)<=0x7ff) {
        if((i)+1<(length)) {
            (s)[(i)++]=(uint8_t)((c)>>6)|0xc0;
            (s)[(i)++]=(uint8_t)(c)&0x3f|0x80;
            return i;
        }
    } else if((uint32_t)(c)<=0xffff) {
        if((i)+2<(length)) {
            (s)[(i)++]=(uint8_t)((c)>>12)|0xe0;
            (s)[(i)++]=(uint8_t)((c)>>6)&0x3f|0x80;
            (s)[(i)++]=(uint8_t)(c)&0x3f|0x80;
            return i;
        }
    } else if((uint32_t)(c)<=0x10ffff) {
        if((i)+3<(length)) {
            (s)[(i)++]=(uint8_t)((c)>>18)|0xf0;
            (s)[(i)++]=(uint8_t)((c)>>12)&0x3f|0x80;
            (s)[(i)++]=(uint8_t)((c)>>6)&0x3f|0x80;
            (s)[(i)++]=(uint8_t)(c)&0x3f|0x80;
            return i;
        }
    }
    /* c>0x10ffff or not enough space, write an error value */
    length-=i;
    if(length>0) {
        if(length>3) {
            length=3;
        }
        s+=i;
        c=utf8_errorValue[length-1];
        UTF8_APPEND_CHAR_SAFE(s, i, length, c);
    }
    return i;
}

U_CAPI UChar32 U_EXPORT2
utf8_prevCharSafeBody(const uint8_t *s, UTextOffset start, UTextOffset *pi, UChar32 c, UBool strict) {
    UTextOffset i=*pi;
    uint8_t b, count=1, shift=6;

    /* extract value bits from the last trail byte */
    c&=0x3f;

    for(;;) {
        if(i<=start) {
            /* no lead byte at all */
            c=UTF8_ERROR_VALUE_1;
            break;
        }

        /* read another previous byte */
        b=s[--i];
        if((uint8_t)(b-0x80)<0x7e) { /* 0x80<=b<0xfe */
            if(b&0x40) {
                /* lead byte, this will always end the loop */
                uint8_t shouldCount=UTF8_COUNT_TRAIL_BYTES(b);

                if(count==shouldCount) {
                    /* set the new position */
                    *pi=i;
                    UTF8_MASK_LEAD_BYTE(b, count);
                    c|=(UChar32)b<<shift;
                    if( c>0x10ffff ||
                        (strict) &&
                            (UTF_IS_SURROGATE(c) ||
                             count>=4 || (c)<utf8_minRegular[count] || ((c)&0xfffe)==0xfffe)
                    ) {
                        /* irregular sequence */
                        c=utf8_errorValue[count];
                    } else {
                        /* exit with correct c */
                    }
                } else {
                    /* the lead byte does not match the number of trail bytes */
                    /* only set the position to the lead byte if it would
                       include the trail byte that we started with */
                    if(count<shouldCount) {
                        *pi=i;
                        c=utf8_errorValue[count];
                    } else {
                        c=UTF8_ERROR_VALUE_1;
                    }
                }
                break;
            } else if(count<5) {
                /* trail byte */
                c|=(UChar32)(b&0x3f)<<shift;
                ++count;
                shift+=6;
            } else {
                /* more than 5 trail bytes is illegal */
                c=UTF8_ERROR_VALUE_1;
                break;
            }
        } else {
            /* single-byte character precedes trailing bytes */
            c=UTF8_ERROR_VALUE_1;
            break;
        }
    }
    return c;
}

U_CAPI UTextOffset U_EXPORT2
utf8_back1SafeBody(const uint8_t *s, UTextOffset start, UTextOffset i) {
    /* i had been decremented once before the function call */
    UTextOffset I=i, Z;
    uint8_t b;

    /* read at most the 6 bytes s[Z] to s[i], inclusively */
    if(I-5>start) {
        Z=I-5;
    } else {
        Z=start;
    }

    /* return I if the sequence starting there is long enough to include i */
    for(;;) {
        b=s[I];
        if((uint8_t)(b-0x80)>=0x7e) { /* not 0x80<=b<0xfe */
            break;
        } else if(b>=0xc0) {
            if(UTF8_COUNT_TRAIL_BYTES(b)>=(i-I)) {
                return I;
            } else {
                break;
            }
        } else if(Z<I) {
            --I;
        } else {
            break;
        }
    }

    /* return i itself to be consistent with the FWD_1 macro */
    return i;
}
