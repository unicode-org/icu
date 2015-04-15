/*
**********************************************************************
*   Copyright (C) 2008-2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/11/2008  Andy Heninger  Port from Java
**********************************************************************
*/

#include "unicode/utypes.h"

#if  !UCONFIG_NO_TRANSLITERATION && !UCONFIG_NO_BREAK_ITERATION

#include "unicode/unifilt.h"
#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "unicode/brkiter.h"
#include "brktrans.h"
#include "unicode/uchar.h"
#include "cmemory.h"
#include "uprops.h"
#include "uinvchar.h"
#include "umutex.h"
#include "util.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(BreakTransliterator)

static const UChar SPACE       = 32;  // ' '


/**
 * Constructs a transliterator with the default delimiters '{' and
 * '}'.
 */
BreakTransliterator::BreakTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(UNICODE_STRING("Any-BreakInternal", 17), adoptedFilter),
    fInsertion(SPACE) {
        cachedBI = NULL;
        cachedBoundaries = NULL;
    }


/**
 * Destructor.
 */
BreakTransliterator::~BreakTransliterator() {
    delete cachedBI;
    cachedBI = NULL;
    delete cachedBoundaries;
    cachedBoundaries = NULL;
}

/**
 * Copy constructor.
 */
BreakTransliterator::BreakTransliterator(const BreakTransliterator& o) :
        Transliterator(o) {
    cachedBI = NULL;
    cachedBoundaries = NULL;
    fInsertion = o.fInsertion;
}


/**
 * Transliterator API.
 */
Transliterator* BreakTransliterator::clone(void) const {
    return new BreakTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void BreakTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                    UBool isIncremental ) const {

        UErrorCode status = U_ZERO_ERROR;
        BreakIterator *bi = NULL;
        UVector32 *boundaries = NULL;

        umtx_lock(NULL);
        if (cachedBI) {
            bi = cachedBI;
            boundaries = cachedBoundaries;
            BreakTransliterator *nonConstThis = const_cast<BreakTransliterator *>(this);
            nonConstThis->cachedBI = NULL;
            nonConstThis->cachedBoundaries = NULL;
        }
        umtx_unlock(NULL);
        if (bi == NULL) {
            boundaries = new UVector32(status);
            bi = BreakIterator::createWordInstance(Locale::getEnglish(), status);
        }    

        if (bi == NULL || boundaries == NULL || U_FAILURE(status)) {
            return;
        }
            
        boundaries->removeAllElements();
        UnicodeString sText = replaceableAsString(text);
        bi->setText(sText);
        bi->preceding(offsets.start);

        // To make things much easier, we will stack the boundaries, and then insert at the end.
        // generally, we won't need too many, since we will be filtered.

        int32_t boundary;
        for(boundary = bi->next(); boundary != UBRK_DONE && boundary < offsets.limit; boundary = bi->next()) {
            if (boundary == 0) continue;
            // HACK: Check to see that preceeding item was a letter

            UChar32 cp = sText.char32At(boundary-1);
            int type = u_charType(cp);
            //System.out.println(Integer.toString(cp,16) + " (before): " + type);
            if ((U_MASK(type) & (U_GC_L_MASK | U_GC_M_MASK)) == 0) continue;

            cp = sText.char32At(boundary);
            type = u_charType(cp);
            //System.out.println(Integer.toString(cp,16) + " (after): " + type);
            if ((U_MASK(type) & (U_GC_L_MASK | U_GC_M_MASK)) == 0) continue;

            boundaries->addElement(boundary, status);
            // printf("Boundary at %d\n", boundary);
        }

        int delta = 0;
        int lastBoundary = 0;

        if (boundaries->size() != 0) { // if we found something, adjust
            delta = boundaries->size() * fInsertion.length();
            lastBoundary = boundaries->lastElementi();

            // we do this from the end backwards, so that we don't have to keep updating.

            while (boundaries->size() > 0) {
                boundary = boundaries->popi();
                text.handleReplaceBetween(boundary, boundary, fInsertion);
            }
        }

        // Now fix up the return values
        offsets.contextLimit += delta;
        offsets.limit += delta;
        offsets.start = isIncremental ? lastBoundary + delta : offsets.limit;

        // Return break iterator & boundaries vector to the cache.
        umtx_lock(NULL);
        BreakTransliterator *nonConstThis = const_cast<BreakTransliterator *>(this);
        if (nonConstThis->cachedBI == NULL) {
            nonConstThis->cachedBI = bi;
            bi = NULL;
        }
        if (nonConstThis->cachedBoundaries == NULL) {
            nonConstThis->cachedBoundaries = boundaries;
            boundaries = NULL;
        }
        umtx_unlock(NULL);
        if (bi) {
            delete bi;
        }
        if (boundaries) {
            delete boundaries;
        }

        // TODO:  do something with U_FAILURE(status);
        //        (need to look at transliterators overall, not just here.)
}

//
//  getInsertion()
//
const UnicodeString &BreakTransliterator::getInsertion() const {
    return fInsertion;
}

//
//  setInsertion()
//
void BreakTransliterator::setInsertion(const UnicodeString &insertion) {
    this->fInsertion = insertion;
}

//
//   replaceableAsString   Hack to let break iterators work
//                         on the replaceable text from transliterators.
//                         In practice, the only real Replaceable type that we
//                         will be seeing is UnicodeString, so this function
//                         will normally be efficient.
//
UnicodeString BreakTransliterator::replaceableAsString(Replaceable &r) {
    UnicodeString s;
    UnicodeString *rs = dynamic_cast<UnicodeString *>(&r);
    if (rs != NULL) {
        s = *rs;
    } else {
        r.extractBetween(0, r.length(), s);
    }
    return s;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
