/*
*****************************************************************
* Copyright (c) 2002, International Business Machines Corporation
* and others.  All Rights Reserved.
*****************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/anytrans.cpp,v $ 
* $Revision: 1.1 $
*****************************************************************
* Date        Name        Description
* 06/06/2002  aliu        Creation.
*****************************************************************
*/
#include "anytrans.h"
#include "uvector.h"
#include "unicode/nultrans.h"
#include "unicode/uscript.h"

//------------------------------------------------------------
// Constants

static const UChar HYPHEN = 45; // '-'
static const UChar ANY[] = {65,110,121,45,0}; // "Any-"

//------------------------------------------------------------
// AnyTransliterator

U_NAMESPACE_BEGIN

/**
 * Try to create a transliterator with the given ID, which should be
 * of the form "Any-X".  The "X" will be pulled off and passed to
 * createInstance().
 */
Transliterator* AnyTransliterator::_create(const UnicodeString& ID, Token /*context*/) {
    UnicodeString target(ID);
    int32_t i = target.indexOf(HYPHEN);
    if (i >= 0) {
        target.remove(0, i+1);
    }
    return AnyTransliterator::createInstance(target, TRUE, TRUE);
}

/**
 * Registers standard variants with the system.  Called by
 * Transliterator during initialization.
 */
void AnyTransliterator::registerIDs() {
    Token t = integerToken(0);

    // Register Any-Latin and make its inverse Null
    Transliterator::_registerFactory("Any-Latin", _create, t);
    Transliterator::_registerSpecialInverse("Latin", "Null", FALSE);
}

/**
 * Return the script code for a given name, or -1 if not found.
 */
int32_t AnyTransliterator::scriptNameToCode(const UnicodeString& name) {
    char buf[128];
    UScriptCode code;
    UErrorCode ec = U_ZERO_ERROR;

    name.extract(0, 128, buf, 128, "");
    if (uscript_getCode(buf, &code, 1, &ec) != 1 ||
        U_FAILURE(ec)) {
        code = (UScriptCode) -1;
    }
    return (int32_t) code;
}

/**
 * Factory method to create an Any-X transliterator.  Relies on
 * registered transliterators at the time of the call to build the
 * Any-X transliterator.  If there are no registered transliterators
 * of the form Y-X, then the logical result is Any-Null.  If there is
 * exactly one transliterator of the form Y-X, then the logical result
 * is Y-X, a degenerate result.  If there are 2 or more
 * transliterators of the form Y-X, then an AnyTransliterator is
 * instantiated and returned. 
 * @param allowNull if true, then return Any-Null if there are no
 * transliterator to the given script; otherwise return NULL
 * @param allowDegenerate if true, then return a transliterator of the
 * form X-Y if there is only one such transliterator
 * the given script; otherwise return NULL
 */
Transliterator* AnyTransliterator::createInstance(const UnicodeString& toTarget,
                                                  UBool allowNull,
                                                  UBool allowDegenerate) {
    UErrorCode ec = U_ZERO_ERROR;
    UVector translits(ec);
    if (U_FAILURE(ec)) {
        return NULL;
    }

    // Count transliterators _to_ the given target.  This is
    // inconvenient since we have to iterate over all sources.
    int32_t sourceCount = Transliterator::countAvailableSources();
    for (int32_t s=0; s<sourceCount; ++s) {
        UnicodeString source;
        Transliterator::getAvailableSource(s, source);
        int32_t targetCount = Transliterator::countAvailableTargets(source);
        for (int32_t t=0; t<targetCount; ++t) {
            UnicodeString target;
            Transliterator::getAvailableTarget(t, source, target);
            if (target.caseCompare(toTarget, 0 /*U_FOLD_CASE_DEFAULT*/) == 0) {
                // We have a source match.  It must also be a script
                // or we can't use it.
                int32_t code = scriptNameToCode(source);
                if (code < 0) {
                    continue;
                }

                // Try to instantiate the given transliterator
                UnicodeString id(source);
                id.append(HYPHEN).append(toTarget);
                Transliterator* t = Transliterator::createInstance(
                                         id, UTRANS_FORWARD, ec);
                if (U_FAILURE(ec) || t == NULL) {
                    delete t;
                    continue;
                }

                // We have a script code and a transliterator; save
                // them.
                translits.addElement(new Elem((UScriptCode) code, t), ec);
            }
        }
    }

    switch (translits.size()) {
    case 0:
        // There is nothing registered going to the requested target,
        // so return Any-Null, if allowed
        return allowNull ? new NullTransliterator() : NULL;
    case 1:
        // Exactly one transliterator goes to the requested target, so
        // return it, if allowed
        {
            Transliterator* t = NULL;
            if (allowDegenerate) {
                Elem *e = (Elem*) translits.orphanElementAt(0);
                t = e->translit;
                delete e;
            }
            return t;
        }
    }

    // We have 2 or more script-toTarget transliterators.  Assemble an
    // AnyTransliterator and return it.
    UnicodeString id(ANY);
    id.append(toTarget);
    return new AnyTransliterator(id, translits);
}

//|/**
//| * Factory method to create an Any-X transliterator.  Convenience
//| * function that takes a script code.
//| */
//|Transliterator* AnyTransliterator::createInstance(UScriptCode target,
//|                                                  UBool allowNull,
//|                                                  UBool allowDegenerate) {
//|    UnicodeString name(uscript_getName(target), "");
//|    return createInstance(name, allowNull, allowDegenerate);
//|}

/**
 * Constructs aa transliterator with the given ID.  The vector should
 * contain Elem objects.  Each will be removed from the vector and
 * ownership taken of its storage, including the contained
 * transliterator.  Upon return the vector will be empty.
 */
AnyTransliterator::AnyTransliterator(const UnicodeString& id, UVector& vec) :
    Transliterator(id, NULL)
{
    count = vec.size();
    elems = new Elem[count];
    for (int32_t i=count-1; i>=0; --i) {
        Elem* e = (Elem*) vec.orphanElementAt(i);
        elems[i] = *e;
        delete e;
    }
}

AnyTransliterator::~AnyTransliterator() {
    for (int32_t i=0; i<count; ++i) {
        delete elems[i].translit;
    }
    delete[] elems;
}

/**
 * Copy constructor.
 */
AnyTransliterator::AnyTransliterator(const AnyTransliterator& o) :
    Transliterator(o)
{
    count = o.count;
    elems = new Elem[count];
    for (int32_t i=0; i<count; ++i) {
        elems[i] = o.elems[i];
        elems[i].translit = elems[i].translit->clone();
    }
}

/**
 * Transliterator API.
 */
Transliterator* AnyTransliterator::clone() const {
    return new AnyTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void AnyTransliterator::handleTransliterate(Replaceable& text, UTransPosition& pos,
                                            UBool isIncremental) const {

    // Compute indices relative to contextStart
    int32_t start = pos.start - pos.contextStart;
    int32_t limit = pos.limit - pos.contextStart;
    int32_t contextLimit = pos.contextLimit - pos.contextStart;

    if (start == limit) return; // Short circuit

    // Extract contextStart..contextLimit
    UnicodeString ustext;
    text.extractBetween(pos.contextStart, pos.contextLimit, ustext);

    // Work directly on the buffer.  We don't need to release the
    // buffer since the UnicodeString is automatic scope.
    UChar* utext = ustext.getBuffer(-1);

    UErrorCode ec = U_ZERO_ERROR;
    UScriptRun* run = uscript_openRun(utext, contextLimit, &ec);
    if (U_FAILURE(ec)) {
        pos.start = pos.limit; // we're done
        uscript_closeRun(run);
        return;
    }

    int32_t origLimit = pos.limit; // save original limit
    int32_t delta = 0; // cumulative change in length

    // Iterate over runs
    int32_t runStart, runLimit;
    UScriptCode runScript;

    // We're done if we've entered the post context or when there are
    // no more script runs (which should only happen when we call
    // nextRun _after_ runLimit has been returned at contextLimit).
    runLimit = 0;
    while (runLimit < limit &&
           uscript_nextRun(run, &runStart, &runLimit, &runScript)) {

        // Do nothing if we're still in the ante context
        if (runLimit <= start) continue;

        // See if we have a transliterator for this run
        Transliterator* t = NULL;
        for (int32_t i=0; i<count; ++i) {
            if (elems[i].script == runScript) {
                t = elems[i].translit;
                break;
            }
        }

        // Transliterate max(start, runStart) to min(limit, runLimit).
        // Adjust indices to text-relative ones
        pos.start = uprv_max(start, runStart) + pos.contextStart + delta;
        pos.limit = uprv_min(limit, runLimit) + pos.contextStart + delta;
        
        // If we don't have a transliterator for this script, then
        // leave the text unchanged.
        if (t == NULL) {
            pos.start = pos.limit;
        }

        else {
            // If the run end is before the transliteration limit, do
            // a non-incremental transliteration.  Otherwise do an
            // incremental one.
            UBool incremental = isIncremental && (runLimit >= limit);
            
            // Transliterate and record change in length
            int32_t l = pos.limit;
            t->filteredTransliterate(text, pos, incremental);
            delta += pos.limit - l;
        }
    }

    uscript_closeRun(run);

    // pos.start can stay where the last transliterator left it.  pos.limit
    // needs to be adjusted for changes in length.
    pos.limit = origLimit + delta;
}

U_NAMESPACE_END

//eof
