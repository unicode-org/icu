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

U_CDECL_BEGIN

/* No-Op UCharIterator implementation for illegal input --------------------- */

static int32_t U_CALLCONV
noopMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    return 0;
}

static UBool U_CALLCONV
noopHasNext(UCharIterator *iter) {
    return FALSE;
}

static UChar U_CALLCONV
noopCurrent(UCharIterator *iter) {
    return 0xffff;
}

static const UCharIterator noopIterator={
    0, 0, 0, 0, 0,
    noopMove,
    noopHasNext,
    noopHasNext,
    noopCurrent,
    noopCurrent,
    noopCurrent
};

/* UCharIterator implementation for simple strings -------------------------- */

/*
 * This is an implementation of a code unit (UChar) iterator
 * for UChar * strings.
 *
 * The UCharIterator.context field holds a pointer to the string.
 */

static int32_t U_CALLCONV
stringIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    int32_t pos;

    switch(origin) {
    case UITERATOR_START:
        pos=iter->start+delta;
        break;
    case UITERATOR_CURRENT:
        pos=iter->index+delta;
        break;
    case UITERATOR_END:
        pos=iter->limit+delta;
        break;
    default:
        /* not a valid origin, no move */
        /* Should never get here! */
        pos = iter->start;
        break;
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

static UChar U_CALLCONV
stringIteratorCurrent(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((const UChar *)(iter->context))[iter->index];
    } else {
        return 0xffff;
    }
}

static UChar U_CALLCONV
stringIteratorNext(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((const UChar *)(iter->context))[iter->index++];
    } else {
        return 0xffff;
    }
}

static UChar U_CALLCONV
stringIteratorPrevious(UCharIterator *iter) {
    if(iter->index>iter->start) {
        return ((const UChar *)(iter->context))[--iter->index];
    } else {
        return 0xffff;
    }
}

static const UCharIterator stringIterator={
    0, 0, 0, 0, 0,
    stringIteratorMove,
    stringIteratorHasNext,
    stringIteratorHasPrevious,
    stringIteratorCurrent,
    stringIteratorNext,
    stringIteratorPrevious
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

/* UCharIterator wrapper around CharacterIterator --------------------------- */

/*
 * This is wrapper code around a C++ CharacterIterator to
 * look like a C UCharIterator.
 *
 * The UCharIterator.context field holds a pointer to the CharacterIterator.
 */

static int32_t U_CALLCONV
characterIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    return ((CharacterIterator *)(iter->context))->move(delta, (CharacterIterator::EOrigin)origin);
}

static UBool U_CALLCONV
characterIteratorHasNext(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->hasNext();
}

static UBool U_CALLCONV
characterIteratorHasPrevious(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->hasPrevious();
}

static UChar U_CALLCONV
characterIteratorCurrent(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->current();
}

static UChar U_CALLCONV
characterIteratorNext(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->nextPostInc();
}

static UChar U_CALLCONV
characterIteratorPrevious(UCharIterator *iter) {
    return ((CharacterIterator *)(iter->context))->previous();
}

static const UCharIterator characterIteratorWrapper={
    0, 0, 0, 0, 0,
    characterIteratorMove,
    characterIteratorHasNext,
    characterIteratorHasPrevious,
    characterIteratorCurrent,
    characterIteratorNext,
    characterIteratorPrevious
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

static UChar U_CALLCONV
replaceableIteratorCurrent(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((Replaceable *)(iter->context))->charAt(iter->index);
    } else {
        return 0xffff;
    }
}

static UChar U_CALLCONV
replaceableIteratorNext(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((Replaceable *)(iter->context))->charAt(iter->index++);
    } else {
        return 0xffff;
    }
}

static UChar U_CALLCONV
replaceableIteratorPrevious(UCharIterator *iter) {
    if(iter->index>iter->start) {
        return ((Replaceable *)(iter->context))->charAt(--iter->index);
    } else {
        return 0xffff;
    }
}

static const UCharIterator replaceableIterator={
    0, 0, 0, 0, 0,
    stringIteratorMove,
    stringIteratorHasNext,
    stringIteratorHasPrevious,
    replaceableIteratorCurrent,
    replaceableIteratorNext,
    replaceableIteratorPrevious
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

U_CDECL_END
