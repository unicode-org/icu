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
