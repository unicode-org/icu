/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uiter.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002jan18
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/chariter.h"
#include "unicode/rep.h"
#include "unicode/uiter.h"
#include "cstring.h"

U_CDECL_BEGIN

/* No-Op UCharIterator implementation for illegal input --------------------- */

static int32_t U_CALLCONV
noopGetIndex(UCharIterator * /*iter*/, UCharIteratorOrigin /*origin*/) {
    return 0;
}

static int32_t U_CALLCONV
noopMove(UCharIterator * /*iter*/, int32_t /*delta*/, UCharIteratorOrigin /*origin*/) {
    return 0;
}

static UBool U_CALLCONV
noopHasNext(UCharIterator * /*iter*/) {
    return FALSE;
}

static UChar32 U_CALLCONV
noopCurrent(UCharIterator * /*iter*/) {
    return U_SENTINEL;
}

static const UCharIterator noopIterator={
    0, 0, 0, 0, 0, 0,
    noopGetIndex,
    noopMove,
    noopHasNext,
    noopHasNext,
    noopCurrent,
    noopCurrent,
    noopCurrent,
    0
};

/* UCharIterator implementation for simple strings -------------------------- */

/*
 * This is an implementation of a code unit (UChar) iterator
 * for UChar * strings.
 *
 * The UCharIterator.context field holds a pointer to the string.
 */

static int32_t U_CALLCONV
stringIteratorGetIndex(UCharIterator *iter, UCharIteratorOrigin origin) {
    switch(origin) {
    case UITER_ZERO:
        return 0;
    case UITER_START:
        return iter->start;
    case UITER_CURRENT:
        return iter->index;
    case UITER_LIMIT:
        return iter->limit;
    case UITER_LENGTH:
        return iter->length;
    default:
        /* not a valid origin */
        /* Should never get here! */
        return -1;
    }
}

static int32_t U_CALLCONV
stringIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    int32_t pos;

    switch(origin) {
    case UITER_ZERO:
        pos=delta;
        break;
    case UITER_START:
        pos=iter->start+delta;
        break;
    case UITER_CURRENT:
        pos=iter->index+delta;
        break;
    case UITER_LIMIT:
        pos=iter->limit+delta;
        break;
    case UITER_LENGTH:
        pos=iter->length+delta;
        break;
    default:
        return -1;  /* Error */
    }

    if(pos<iter->start) {
        pos=iter->start;
    } else if(pos>iter->limit) {
        pos=iter->limit;
    }

    return iter->index=pos;
}

static UBool U_CALLCONV
stringIteratorHasNext(UCharIterator *iter) {
    return iter->index<iter->limit;
}

static UBool U_CALLCONV
stringIteratorHasPrevious(UCharIterator *iter) {
    return iter->index>iter->start;
}

static UChar32 U_CALLCONV
stringIteratorCurrent(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((const UChar *)(iter->context))[iter->index];
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
stringIteratorNext(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((const UChar *)(iter->context))[iter->index++];
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
stringIteratorPrevious(UCharIterator *iter) {
    if(iter->index>iter->start) {
        return ((const UChar *)(iter->context))[--iter->index];
    } else {
        return U_SENTINEL;
    }
}

static const UCharIterator stringIterator={
    0, 0, 0, 0, 0, 0,
    stringIteratorGetIndex,
    stringIteratorMove,
    stringIteratorHasNext,
    stringIteratorHasPrevious,
    stringIteratorCurrent,
    stringIteratorNext,
    stringIteratorPrevious,
    0
};

U_CAPI void U_EXPORT2
uiter_setString(UCharIterator *iter, const UChar *s, int32_t length) {
    if(iter!=0) {
        if(s!=0 && length>=-1) {
            *iter=stringIterator;
            iter->context=s;
            if(length>=0) {
                iter->length=length;
            } else {
                iter->length=u_strlen(s);
            }
            iter->limit=iter->length;
        } else {
            *iter=noopIterator;
        }
    }
}

/* UCharIterator implementation for UTF-16BE strings ------------------------ */

/*
 * This is an implementation of a code unit (UChar) iterator
 * for UTF-16BE strings, i.e., strings in byte-vectors where
 * each UChar is stored as a big-endian pair of bytes.
 *
 * The UCharIterator.context field holds a pointer to the string.
 * Everything works just like with a normal UChar iterator (uiter_setString),
 * except that UChars are assembled from byte pairs.
 */

static UChar32 U_CALLCONV
utf16BEIteratorCurrent(UCharIterator *iter) {
    int32_t index;

    if((index=iter->index)<iter->limit) {
        const uint8_t *p=(const uint8_t *)iter->context;
        return ((UChar)p[2*index]<<8)|(UChar)p[2*index+1];
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
utf16BEIteratorNext(UCharIterator *iter) {
    int32_t index;

    if((index=iter->index)<iter->limit) {
        const uint8_t *p=(const uint8_t *)iter->context;
        iter->index=index+1;
        return ((UChar)p[2*index]<<8)|(UChar)p[2*index+1];
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
utf16BEIteratorPrevious(UCharIterator *iter) {
    int32_t index;

    if((index=iter->index)>iter->start) {
        const uint8_t *p=(const uint8_t *)iter->context;
        iter->index=--index;
        return ((UChar)p[2*index]<<8)|(UChar)p[2*index+1];
    } else {
        return U_SENTINEL;
    }
}

static const UCharIterator utf16BEIterator={
    0, 0, 0, 0, 0, 0,
    stringIteratorGetIndex,
    stringIteratorMove,
    stringIteratorHasNext,
    stringIteratorHasPrevious,
    utf16BEIteratorCurrent,
    utf16BEIteratorNext,
    utf16BEIteratorPrevious,
    0
};

/*
 * Count the number of UChars in a UTF-16BE string before a terminating UChar NUL,
 * i.e., before a pair of 0 bytes where the first 0 byte is at an even
 * offset from s.
 */
static int32_t
utf16BE_strlen(const char *s) {
    if(((int32_t)s&1)==0) {
        /*
         * even-aligned, call u_strlen(s)
         * we are probably on a little-endian machine, but searching for UChar NUL
         * does not care about endianness
         */
        return u_strlen((const UChar *)s);
    } else {
        /* odd-aligned, search for pair of 0 bytes */
        const char *p=s;

        while(!(*p==0 && p[1]==0)) {
            p+=2;
        }
        return (int32_t)((p-s)/2);
    }
}

U_CAPI void U_EXPORT2
uiter_setUTF16BE(UCharIterator *iter, const char *s, int32_t length) {
    if(iter!=0) {
        /* allow only even-length strings (the input length counts bytes) */
        if(s!=0 && length==-1 || (length>=0 && (length&1)==0)) {
            if(U_IS_BIG_ENDIAN && ((int32_t)s&1)==0) {
                /* big-endian machine and 2-aligned UTF-16BE string: use normal UChar iterator */
                uiter_setString(iter, (const UChar *)s, length/2);
                return;
            }

            *iter=utf16BEIterator;
            iter->context=s;
            if(length>=0) {
                iter->length=length/2;
            } else {
                iter->length=utf16BE_strlen(s);
            }
            iter->limit=iter->length;
        } else {
            *iter=noopIterator;
        }
    }
}

/* UCharIterator wrapper around CharacterIterator --------------------------- */

/*
 * This is wrapper code around a C++ CharacterIterator to
 * look like a C UCharIterator.
 *
 * The UCharIterator.context field holds a pointer to the CharacterIterator.
 */

static int32_t U_CALLCONV
characterIteratorGetIndex(UCharIterator *iter, UCharIteratorOrigin origin) {
    switch(origin) {
    case UITER_ZERO:
        return 0;
    case UITER_START:
        return ((CharacterIterator *)(iter->context))->startIndex();
    case UITER_CURRENT:
        return ((CharacterIterator *)(iter->context))->getIndex();
    case UITER_LIMIT:
        return ((CharacterIterator *)(iter->context))->endIndex();
    case UITER_LENGTH:
        return ((CharacterIterator *)(iter->context))->getLength();
    default:
        /* not a valid origin */
        /* Should never get here! */
        return -1;
    }
}

static int32_t U_CALLCONV
characterIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    switch(origin) {
    case UITER_ZERO:
        ((CharacterIterator *)(iter->context))->setIndex(delta);
        return ((CharacterIterator *)(iter->context))->getIndex();
    case UITER_START:
    case UITER_CURRENT:
    case UITER_LIMIT:
        return ((CharacterIterator *)(iter->context))->move(delta, (CharacterIterator::EOrigin)origin);
    case UITER_LENGTH:
        ((CharacterIterator *)(iter->context))->setIndex(((CharacterIterator *)(iter->context))->getLength()+delta);
        return ((CharacterIterator *)(iter->context))->getIndex();
    default:
        /* not a valid origin */
        /* Should never get here! */
        return -1;
    }
}

static UBool U_CALLCONV
characterIteratorHasNext(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->hasNext();
}

static UBool U_CALLCONV
characterIteratorHasPrevious(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->hasPrevious();
}

static UChar32 U_CALLCONV
characterIteratorCurrent(UCharIterator *iter) {
    UChar32 c;

    c=((CharacterIterator *)(iter->context))->current();
    if(c!=0xffff || ((CharacterIterator *)(iter->context))->hasNext()) {
        return c;
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
characterIteratorNext(UCharIterator *iter) {
    if(((CharacterIterator *)(iter->context))->hasNext()) {
        return ((CharacterIterator *)(iter->context))->nextPostInc();
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
characterIteratorPrevious(UCharIterator *iter) {
    if(((CharacterIterator *)(iter->context))->hasPrevious()) {
        return ((CharacterIterator *)(iter->context))->previous();
    } else {
        return U_SENTINEL;
    }
}

static const UCharIterator characterIteratorWrapper={
    0, 0, 0, 0, 0, 0,
    characterIteratorGetIndex,
    characterIteratorMove,
    characterIteratorHasNext,
    characterIteratorHasPrevious,
    characterIteratorCurrent,
    characterIteratorNext,
    characterIteratorPrevious,
    0
};

U_CAPI void U_EXPORT2
uiter_setCharacterIterator(UCharIterator *iter, CharacterIterator *charIter) {
    if(iter!=0) {
        if(charIter!=0) {
            *iter=characterIteratorWrapper;
            iter->context=charIter;
        } else {
            *iter=noopIterator;
        }
    }
}

/* UCharIterator wrapper around Replaceable --------------------------------- */

/*
 * This is an implementation of a code unit (UChar) iterator
 * based on a Replaceable object.
 *
 * The UCharIterator.context field holds a pointer to the Replaceable.
 * UCharIterator.length and UCharIterator.index hold Replaceable.length()
 * and the iteration index.
 */

static UChar32 U_CALLCONV
replaceableIteratorCurrent(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((Replaceable *)(iter->context))->charAt(iter->index);
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
replaceableIteratorNext(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((Replaceable *)(iter->context))->charAt(iter->index++);
    } else {
        return U_SENTINEL;
    }
}

static UChar32 U_CALLCONV
replaceableIteratorPrevious(UCharIterator *iter) {
    if(iter->index>iter->start) {
        return ((Replaceable *)(iter->context))->charAt(--iter->index);
    } else {
        return U_SENTINEL;
    }
}

static const UCharIterator replaceableIterator={
    0, 0, 0, 0, 0, 0,
    stringIteratorGetIndex,
    stringIteratorMove,
    stringIteratorHasNext,
    stringIteratorHasPrevious,
    replaceableIteratorCurrent,
    replaceableIteratorNext,
    replaceableIteratorPrevious,
    0
};

U_CAPI void U_EXPORT2
uiter_setReplaceable(UCharIterator *iter, const Replaceable *rep) {
    if(iter!=0) {
        if(rep!=0) {
            *iter=replaceableIterator;
            iter->context=rep;
            iter->limit=iter->length=rep->length();
        } else {
            *iter=noopIterator;
        }
    }
}

/* UCharIterator implementation for UTF-8 strings --------------------------- */

/*
 * Possible, probably necessary only for an implementation for arbitrary
 * converters:
 * Maintain a buffer (ring buffer?) for a piece of converted 16-bit text.
 * This would require to turn reservedFn into a close function and
 * to introduce a uiter_close(iter).
 */

#define UITER_CNV_CAPACITY 16

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
utf8IteratorGetIndex(UCharIterator *iter, UCharIteratorOrigin origin) {
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
                U8_NEXT(s, i, limit, c);
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
utf8IteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
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
        pos=utf8IteratorGetIndex(iter, UITER_LENGTH)+delta;
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

    /* minimize the number of U8_NEXT/PREV operations */
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
            U8_NEXT(s, i, limit, c);
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
            U8_PREV(s, 0, i, c);
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
utf8IteratorHasNext(UCharIterator *iter) {
    return iter->reservedField!=0 || iter->start<iter->limit;
}

static UBool U_CALLCONV
utf8IteratorHasPrevious(UCharIterator *iter) {
    return iter->index>0;
}

static UChar32 U_CALLCONV
utf8IteratorCurrent(UCharIterator *iter) {
    if(iter->reservedField!=0) {
        return U16_TRAIL(iter->reservedField);
    } else if(iter->start<iter->limit) {
        const uint8_t *s=(const uint8_t *)iter->context;
        UChar32 c;
        int32_t i=iter->start;

        U8_NEXT(s, i, iter->limit, c);
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
utf8IteratorNext(UCharIterator *iter) {
    if(iter->reservedField!=0) {
        UChar trail=U16_TRAIL(iter->reservedField);
        iter->reservedField=0;
        ++iter->index;
        return trail;
    } else if(iter->start<iter->limit) {
        const uint8_t *s=(const uint8_t *)iter->context;
        UChar32 c;

        U8_NEXT(s, iter->start, iter->limit, c);
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
utf8IteratorPrevious(UCharIterator *iter) {
    if(iter->reservedField!=0) {
        UChar lead=U16_LEAD(iter->reservedField);
        iter->reservedField=0;
        iter->start-=4; /* we stayed behind the supplementary code point; go before it now */
        --iter->index;
        return lead;
    } else if(iter->start>0) {
        const uint8_t *s=(const uint8_t *)iter->context;
        UChar32 c;

        U8_PREV(s, 0, iter->start, c);
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

static const UCharIterator utf8Iterator={
    0, 0, 0, 0, 0, 0,
    utf8IteratorGetIndex,
    utf8IteratorMove,
    utf8IteratorHasNext,
    utf8IteratorHasPrevious,
    utf8IteratorCurrent,
    utf8IteratorNext,
    utf8IteratorPrevious,
    0
};

U_CAPI void U_EXPORT2
uiter_setUTF8(UCharIterator *iter, const char *s, int32_t length) {
    if(iter!=0) {
        if(s!=0 && length>=-1) {
            *iter=utf8Iterator;
            iter->context=s;
            if(length>=0) {
                iter->limit=length;
            } else {
                iter->limit=uprv_strlen(s);
            }
            iter->length= iter->limit==0 ? 0 : -1;
        } else {
            *iter=noopIterator;
        }
    }
}

/* Helper functions --------------------------------------------------------- */

U_CAPI UChar32 U_EXPORT2
uiter_current32(UCharIterator *iter) {
    UChar32 c, c2;

    c=iter->current(iter);
    if(UTF_IS_SURROGATE(c)) {
        if(UTF_IS_SURROGATE_FIRST(c)) {
            /*
             * go to the next code unit
             * we know that we are not at the limit because c!=U_SENTINEL
             */
            iter->move(iter, 1, UITER_CURRENT);
            if(UTF_IS_SECOND_SURROGATE(c2=iter->current(iter))) {
                c=UTF16_GET_PAIR_VALUE(c, c2);
            }

            /* undo index movement */
            iter->move(iter, -1, UITER_CURRENT);
        } else {
            if(UTF_IS_FIRST_SURROGATE(c2=iter->previous(iter))) {
                c=UTF16_GET_PAIR_VALUE(c2, c);
            }
            if(c2>=0) {
                /* undo index movement */
                iter->move(iter, 1, UITER_CURRENT);
            }
        }
    }
    return c;
}

U_CAPI UChar32 U_EXPORT2
uiter_next32(UCharIterator *iter) {
    UChar32 c, c2;

    c=iter->next(iter);
    if(UTF_IS_FIRST_SURROGATE(c)) {
        if(UTF_IS_SECOND_SURROGATE(c2=iter->next(iter))) {
            c=UTF16_GET_PAIR_VALUE(c, c2);
        } else if(c2>=0) {
            /* unmatched first surrogate, undo index movement */
            iter->move(iter, -1, UITER_CURRENT);
        }
    }
    return c;
}

U_CAPI UChar32 U_EXPORT2
uiter_previous32(UCharIterator *iter) {
    UChar32 c, c2;

    c=iter->previous(iter);
    if(UTF_IS_SECOND_SURROGATE(c)) {
        if(UTF_IS_FIRST_SURROGATE(c2=iter->previous(iter))) {
            c=UTF16_GET_PAIR_VALUE(c2, c);
        } else if(c2>=0) {
            /* unmatched second surrogate, undo index movement */
            iter->move(iter, 1, UITER_CURRENT);
        }
    }
    return c;
}

U_CDECL_END
