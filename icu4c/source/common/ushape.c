/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ushape.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jun29
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "unicode/ushape.h"

#if UTF_SIZE<16
    /*
     * This implementation assumes that the internal encoding is UTF-16
     * or UTF-32, not UTF-8.
     * The main assumption is that the Arabic characters and their
     * presentation forms each fit into a single UChar.
     * With UTF-8, they occupy 2 or 3 bytes, and more than the ASCII
     * characters.
     */
#   error This implementation assumes UTF-16 or UTF-32 (check UTF_SIZE)
#endif

/*
 * This function shapes European digits to Arabic-Indic digits
 * in-place, writing over the input characters.
 * Since we know that we are only looking for BMP code points,
 * we can safely just work with code units (again, at least UTF-16).
 */
static void
_shapeToArabicDigitsWithContext(UChar *s, int32_t length,
                                UChar digitBase,
                                UBool isLogical, UBool lastStrongWasAL) {
    int32_t i;
    UChar c;

    digitBase-=0x30;

    /* the iteration direction depends on the type of input */
    if(isLogical) {
        for(i=0; i<length; ++i) {
            c=s[i];
            switch(u_charDirection(c)) {
            case U_LEFT_TO_RIGHT: /* L */
            case U_RIGHT_TO_LEFT: /* R */
                lastStrongWasAL=FALSE;
                break;
            case U_RIGHT_TO_LEFT_ARABIC: /* AL */
                lastStrongWasAL=TRUE;
                break;
            case U_EUROPEAN_NUMBER: /* EN */
                if(lastStrongWasAL && (uint32_t)(c-0x30)<10) {
                    s[i]=digitBase+c; /* digitBase+(c-0x30) - digitBase was modified above */
                }
                break;
            default :
                break;
            }
        }
    } else {
        for(i=length; i>0; /* pre-decrement in the body */) {
            c=s[--i];
            switch(u_charDirection(c)) {
            case U_LEFT_TO_RIGHT: /* L */
            case U_RIGHT_TO_LEFT: /* R */
                lastStrongWasAL=FALSE;
                break;
            case U_RIGHT_TO_LEFT_ARABIC: /* AL */
                lastStrongWasAL=TRUE;
                break;
            case U_EUROPEAN_NUMBER: /* EN */
                if(lastStrongWasAL && (uint32_t)(c-0x30)<10) {
                    s[i]=digitBase+c; /* digitBase+(c-0x30) - digitBase was modified above */
                }
                break;
            default :
                break;
            }
        }
    }
}

U_CAPI int32_t U_EXPORT2
u_shapeArabic(const UChar *source, int32_t sourceLength,
              UChar *dest, int32_t destSize,
              uint32_t options,
              UErrorCode *pErrorCode) {
    /* usual error checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /* also make sure that no reserved options values are used */
    if( source==NULL || sourceLength<-1 || dest==NULL || destSize<0 ||
        options>=U_SHAPE_DIGIT_TYPE_RESERVED ||
        (options&U_SHAPE_LENGTH_MASK)==U_SHAPE_LENGTH_RESERVED ||
        (options&U_SHAPE_LETTERS_MASK)==U_SHAPE_LETTERS_RESERVED ||
        (options&U_SHAPE_DIGITS_MASK)>=U_SHAPE_DIGITS_RESERVED
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* determine the source length */
    if(sourceLength==-1) {
        sourceLength=u_strlen(source);
    }
    if(sourceLength==0) {
        return 0;
    }

    /* check that source and destination do not overlap */
    if( source<=dest && dest<source+sourceLength ||
        dest<=source && source<dest+destSize
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if((options&U_SHAPE_LETTERS_MASK)!=U_SHAPE_LETTERS_NOOP) {
        /* currently, only number shaping is supported */
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return 0;
    } else {
        /*
         * No letter shaping:
         * just make sure the destination is large enough and copy the string.
         */
        if(destSize<sourceLength) {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            return sourceLength;
        }
        uprv_memcpy(dest, source, sourceLength*U_SIZEOF_UCHAR);
        destSize=sourceLength;
    }

    /*
     * Perform number shaping.
     * With UTF-16 or UTF-32, the length of the string is constant.
     * The easiest way to do this is to operate on the destination and
     * "shape" the digits in-place.
     */
    if((options&U_SHAPE_DIGITS_MASK)!=U_SHAPE_DIGITS_NOOP) {
        UChar digitBase;
        int32_t i;

        /* select the requested digit group */
        switch(options&U_SHAPE_DIGIT_TYPE_MASK) {
        case U_SHAPE_DIGIT_TYPE_AN:
            digitBase=0x660; /* Unicode: "Arabic-Indic digits" */
            break;
        case U_SHAPE_DIGIT_TYPE_AN_EXTENDED:
            digitBase=0x6f0; /* Unicode: "Eastern Arabic-Indic digits (Persian and Urdu)" */
            break;
        default:
            /* will never occur because of validity checks above */
            break;
        }

        /* perform the requested operation */
        switch(options&U_SHAPE_DIGITS_MASK) {
        case U_SHAPE_DIGITS_EN2AN:
            /* add (digitBase-'0') to each European (ASCII) digit code point */
            digitBase-=0x30;
            for(i=0; i<destSize; ++i) {
                if(((uint32_t)dest[i]-0x30)<10) {
                    dest[i]+=digitBase;
                }
            }
            break;
        case U_SHAPE_DIGITS_AN2EN:
            /* subtract (digitBase-'0') from each Arabic digit code point */
            for(i=0; i<destSize; ++i) {
                if(((uint32_t)dest[i]-(uint32_t)digitBase)<10) {
                    dest[i]-=digitBase-0x30;
                }
            }
            break;
        case U_SHAPE_DIGITS_ALEN2AN_INIT_LR:
            _shapeToArabicDigitsWithContext(dest, destSize,
                                            digitBase,
                                            (UBool)((options&U_SHAPE_TEXT_DIRECTION_MASK)==U_SHAPE_TEXT_DIRECTION_LOGICAL),
                                            FALSE);
            break;
        case U_SHAPE_DIGITS_ALEN2AN_INIT_AL:
            _shapeToArabicDigitsWithContext(dest, destSize,
                                            digitBase,
                                            (UBool)((options&U_SHAPE_TEXT_DIRECTION_MASK)==U_SHAPE_TEXT_DIRECTION_LOGICAL),
                                            TRUE);
            break;
        default:
            /* will never occur because of validity checks above */
            break;
        }
    }

    return destSize;
}
