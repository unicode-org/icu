/**
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and others. *
 * All Rights Reserved.                                                        *
 *******************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "brkeng.h"
#include "dictbe.h"
#include "triedict.h"
#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "unicode/chariter.h"
#include "unicode/ures.h"
#include "unicode/udata.h"
#include "unicode/putil.h"
#include "unicode/ustring.h"
#include "uvector.h"
#include "mutex.h"
#include "uresimp.h"
#include "ubrkimpl.h"

U_NAMESPACE_BEGIN

/*
 ******************************************************************
 */

LanguageBreakEngine::LanguageBreakEngine() {
}

LanguageBreakEngine::~LanguageBreakEngine() {
}

/*
 ******************************************************************
 */

LanguageBreakFactory::LanguageBreakFactory() {
}

LanguageBreakFactory::~LanguageBreakFactory() {
}

/*
 ******************************************************************
 */

UnhandledEngine::UnhandledEngine(UErrorCode &/*status*/) {
    for (int32_t i = 0; i < (int32_t)(sizeof(fHandled)/sizeof(fHandled[0])); ++i) {
        fHandled[i] = 0;
    }
}

UnhandledEngine::~UnhandledEngine() {
    for (int32_t i = 0; i < (int32_t)(sizeof(fHandled)/sizeof(fHandled[0])); ++i) {
        if (fHandled[i] != 0) {
            delete fHandled[i];
        }
    }
}

UBool
UnhandledEngine::handles(UChar32 c, int32_t breakType) const {
    return (breakType >= 0 && breakType < (int32_t)(sizeof(fHandled)/sizeof(fHandled[0]))
        && fHandled[breakType] != 0 && fHandled[breakType]->contains(c));
}

int32_t
UnhandledEngine::findBreaks( UText *text,
                                 int32_t startPos,
                                 int32_t endPos,
                                 UBool reverse,
                                 int32_t breakType,
                                 UStack &/*foundBreaks*/ ) const {
    if (breakType >= 0 && breakType < (int32_t)(sizeof(fHandled)/sizeof(fHandled[0]))) {
        UChar32 c = utext_current32(text); 
        if (reverse) {
            while((int32_t)utext_getNativeIndex(text) > startPos && fHandled[breakType]->contains(c)) {
                c = utext_previous32(text);
            }
        }
        else {
            while((int32_t)utext_getNativeIndex(text) < endPos && fHandled[breakType]->contains(c)) {
                utext_next32(text);            // TODO:  recast loop to work with post-increment operations.
                c = utext_current32(text);
            }
        }
    }
    return 0;
}

void
UnhandledEngine::handleCharacter(UChar32 c, int32_t breakType) {
    if (breakType >= 0 && breakType < (int32_t)(sizeof(fHandled)/sizeof(fHandled[0]))) {
        if (fHandled[breakType] == 0) {
            fHandled[breakType] = new UnicodeSet();
            if (fHandled[breakType] == 0) {
                return;
            }
        }
        if (!fHandled[breakType]->contains(c)) {
            UErrorCode status = U_ZERO_ERROR;
            // Apply the entire script of the character.
            int32_t script = u_getIntPropertyValue(c, UCHAR_SCRIPT);
            fHandled[breakType]->applyIntPropertyValue(UCHAR_SCRIPT, script, status);
        }
    }
}

/*
 ******************************************************************
 */

ICULanguageBreakFactory::ICULanguageBreakFactory(UErrorCode &/*status*/) {
    fEngines = 0;
}

ICULanguageBreakFactory::~ICULanguageBreakFactory() {
    if (fEngines != 0) {
        delete fEngines;
    }
}

U_NAMESPACE_END
U_CDECL_BEGIN
static void U_CALLCONV _deleteEngine(void *obj) {
    delete (const LanguageBreakEngine *) obj;
}
U_CDECL_END
U_NAMESPACE_BEGIN

const LanguageBreakEngine *
ICULanguageBreakFactory::getEngineFor(UChar32 c, int32_t breakType) {
    UBool       needsInit;
    UErrorCode  status = U_ZERO_ERROR;
    umtx_lock(NULL);
    needsInit = (UBool)(fEngines == NULL);
    umtx_unlock(NULL);
    
    if (needsInit) {
        UStack  *engines = new UStack(_deleteEngine, NULL, status);
        if (U_SUCCESS(status) && engines == NULL) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
        // TODO: add locale parameter, check "dictionaries" in locale
        // TODO: generalize once we can figure out how to parameterize engines
        // instead of having different subclasses. Right now it needs to check
        // for the key of each particular subclass.

        // Open root from brkitr tree.
        char dictnbuff[256];
        char ext[4]={'\0'};

        UResourceBundle *b = ures_open(U_ICUDATA_BRKITR, "", &status);
        b = ures_getByKeyWithFallback(b, "dictionaries", b, &status);
        b = ures_getByKeyWithFallback(b, "Thai", b, &status);
        int32_t dictnlength = 0;
        const UChar *dictfname = ures_getString(b, &dictnlength, &status);
        if (U_SUCCESS(status) && (size_t)dictnlength >= sizeof(dictnbuff)) {
            dictnlength = 0;
            status = U_BUFFER_OVERFLOW_ERROR;
        }
        if (U_SUCCESS(status) && dictfname) {
            UChar* extStart=u_strchr(dictfname, 0x002e);
            int len = 0;
            if(extStart!=NULL){
                len = extStart-dictfname;
                u_UCharsToChars(extStart+1, ext, sizeof(ext)); // nul terminates the buff
                u_UCharsToChars(dictfname, dictnbuff, len);
            }
            dictnbuff[len]=0; // nul terminate
        }
        ures_close(b);
        UDataMemory *file = udata_open(U_ICUDATA_BRKITR, ext, dictnbuff, &status);
        if (U_SUCCESS(status)) {
            const CompactTrieDictionary *dict = new CompactTrieDictionary(
                file, status);
            if (U_SUCCESS(status) && dict == NULL) {
                status = U_MEMORY_ALLOCATION_ERROR;
            }
            if (U_FAILURE(status)) {
                delete dict;
                dict = NULL;
            }
            const ThaiBreakEngine *thai = new ThaiBreakEngine(dict, status);
            if (thai == NULL) {
            	delete dict;
            	if (U_SUCCESS(status)) {
                	status = U_MEMORY_ALLOCATION_ERROR;
                }
            }
            if (U_SUCCESS(status)) {
                engines->push((void *)thai, status);
            }
            else {
                delete thai;
            }
        }
        umtx_lock(NULL);
        if (fEngines == NULL) {
            fEngines = engines;
            engines = NULL;
        }
        umtx_unlock(NULL);
        delete engines;
    }
    
    if (fEngines == NULL) {
        return NULL;
    }
    int32_t i = fEngines->size();
    const LanguageBreakEngine *lbe = NULL;
    while (--i >= 0) {
        lbe = (const LanguageBreakEngine *)(fEngines->elementAt(i));
        if (lbe != NULL && lbe->handles(c, breakType)) {
            break;
        }
        lbe = NULL;
    }
    return lbe;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
