/*
*******************************************************************************
* Copyright (C) 1997-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File TXTBDRY.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Added DONE.
*   01/13/2000  helena      Added UErrorCode parameter to createXXXInstance methods.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file BreakIterator.java
// *****************************************************************************

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/dbbi.h"
#include "unicode/brkiter.h"
#include "unicode/udata.h"
#include "unicode/resbund.h"
#include "cstring.h"
#include "mutex.h"
#include "iculserv.h"
#include "locbased.h"

// *****************************************************************************
// class BreakIterator
// This class implements methods for finding the location of boundaries in text.
// Instances of BreakIterator maintain a current position and scan over text
// returning the index of characters where boundaries occur.
// *****************************************************************************

U_NAMESPACE_BEGIN

const int32_t BreakIterator::DONE = (int32_t)-1;

// -------------------------------------

// Creates a break iterator for word breaks.
BreakIterator*
BreakIterator::createWordInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_WORD, status);
}

BreakIterator*
BreakIterator::makeWordInstance(const Locale& key, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    const char* filename = "word";

    if (U_FAILURE(status))
        return NULL;

    if (!uprv_strcmp(key.getLanguage(), "th"))
    {
        filename = "word_th";
    }

    UDataMemory* file = udata_open(NULL, "brk", filename, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    // The UDataMemory is adopted by the break iterator.

    if(!uprv_strcmp(filename, "word_th")) {
        filename = "thaidict.brk";
        result = new DictionaryBasedBreakIterator(file, filename, status);
    }
    else {
        result = new RuleBasedBreakIterator(file, status);
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        if (result != NULL) {
            delete result;
        }
        return NULL;
    }
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    
    return result;
}

// -------------------------------------

// Creates a break iterator  for line breaks.
BreakIterator*
BreakIterator::createLineInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_LINE, status);
}

BreakIterator*
BreakIterator::makeLineInstance(const Locale& key, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    const char* filename = "line";

    if (U_FAILURE(status))
        return NULL;

    if (!uprv_strcmp(key.getLanguage(), "th"))
    {
        filename = "line_th";
    }

    UDataMemory* file = udata_open(NULL, "brk", filename, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    // The UDataMemory is adopted by the break iterator.

    if (!uprv_strcmp(key.getLanguage(), "th")) {
        filename = "thaidict.brk";
        result = new DictionaryBasedBreakIterator(file, filename, status);
    }
    else {
        result = new RuleBasedBreakIterator(file, status);
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        if (result != NULL) {
            delete result;
        }
        return NULL;
    }
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

// -------------------------------------

// Creates a break iterator  for character breaks.
BreakIterator*
BreakIterator::createCharacterInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_CHARACTER, status);
}

BreakIterator*
BreakIterator::makeCharacterInstance(const Locale& /* key */, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    static const char filename[] = "char";

    if (U_FAILURE(status))
        return NULL;
    UDataMemory* file = udata_open(NULL, "brk", filename, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    // The UDataMemory is adopted by the break iterator.

    result = new RuleBasedBreakIterator(file, status);
    
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        if (result != NULL) {
            delete result;
        }
        return NULL;
    }
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

// -------------------------------------

// Creates a break iterator  for sentence breaks.
BreakIterator*
BreakIterator::createSentenceInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_SENTENCE, status);
}

BreakIterator*
BreakIterator::makeSentenceInstance(const Locale& /*key */, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files and the alternate rules files for Thai.  This function
    // will have to be made fully general at some time in the future!
    BreakIterator* result = NULL;
    static const char filename[] = "sent";

    if (U_FAILURE(status))
        return NULL;
    UDataMemory* file = udata_open(NULL, "brk", filename, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    // The UDataMemory is adopted by the break iterator.

    result = new RuleBasedBreakIterator(file, status);
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        if (result != NULL) {
            delete result;
        }
        return NULL;
    }
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }

    return result;
}

// -------------------------------------

// Creates a break iterator for title casing breaks.
BreakIterator*
BreakIterator::createTitleInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_TITLE, status);
}

BreakIterator*
BreakIterator::makeTitleInstance(const Locale& /* key */, UErrorCode& status)
{
    // WARNING: This routine is currently written specifically to handle only the
    // default rules files.  This function will have to be made fully general
    // at some time in the future!
    BreakIterator* result = NULL;
    static const char filename[] = "title";

    if (U_FAILURE(status))
        return NULL;
    UDataMemory* file = udata_open(NULL, "brk", filename, &status);
    if (U_FAILURE(status)) {
        return NULL;
    }
    // The UDataMemory is adopted by the break iterator.

    result = new RuleBasedBreakIterator(file, status);
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        if (result != NULL) {
            delete result;
        }
        return NULL;
    }
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }

    return result;
}

// -------------------------------------

// Gets all the available locales that has localized text boundary data.
const Locale*
BreakIterator::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}

// -------------------------------------
// Gets the objectLocale display name in the default locale language.
UnicodeString&
BreakIterator::getDisplayName(const Locale& objectLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(name);
}

// -------------------------------------
// Gets the objectLocale display name in the displayLocale language.
UnicodeString&
BreakIterator::getDisplayName(const Locale& objectLocale,
                             const Locale& displayLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(displayLocale, name);
}

// ------------------------------------------
//
// Default constructor and destructor
//
//-------------------------------------------

BreakIterator::BreakIterator()
{
    fBufferClone = FALSE;
    *validLocale = *actualLocale = 0;
}

BreakIterator::~BreakIterator()
{
}

// ------------------------------------------
//
// Registration
//
//-------------------------------------------

static ICULocaleService* gService = NULL;

// -------------------------------------

class ICUBreakIteratorFactory : public ICUResourceBundleFactory {
protected:
    virtual UObject* handleCreate(const Locale& loc, int32_t kind, const ICUService* /*service*/, UErrorCode& status) const {
        return BreakIterator::makeInstance(loc, kind, status);
    }
};

// -------------------------------------

class ICUBreakIteratorService : public ICULocaleService {
public:
    ICUBreakIteratorService()
        : ICULocaleService("Break Iterator")
    {
        UErrorCode status = U_ZERO_ERROR;
        registerFactory(new ICUBreakIteratorFactory(), status);
    }
    
    virtual UObject* cloneInstance(UObject* instance) const {
        return ((BreakIterator*)instance)->clone();
    }
    
    virtual UObject* handleDefault(const ICUServiceKey& key, UnicodeString* /*actualID*/, UErrorCode& status) const {
        LocaleKey& lkey = (LocaleKey&)key;
        int32_t kind = lkey.kind();
        Locale loc;
        lkey.currentLocale(loc);
        return BreakIterator::makeInstance(loc, kind, status);
    }
    
    virtual UBool isDefault() const {
        return countFactories() == 1;
    }
};

// -------------------------------------

static ICULocaleService* 
getService(void)
{
    UBool needsInit;
    umtx_lock(NULL);
    needsInit = (UBool)(gService == NULL);
    umtx_unlock(NULL);
    
    if (needsInit) {
        ICULocaleService  *tService = new ICUBreakIteratorService();
        umtx_lock(NULL);
        if (gService == NULL) {
            gService = tService;
            tService = NULL;
        }
        umtx_unlock(NULL);
        delete tService;
    }
    return gService;
}

// -------------------------------------

static UBool
hasService(void) 
{
    Mutex mutex;
    return gService != NULL;
}

// -------------------------------------

BreakIterator*
BreakIterator::createInstance(const Locale& loc, UBreakIteratorType kind, UErrorCode& status)
{
    if (U_FAILURE(status)) {
        return NULL;
    }
    
    u_init(&status);
    if (hasService()) {
        Locale actualLoc;
        BreakIterator *result = (BreakIterator*)gService->get(loc, kind, &actualLoc, status);
        // TODO: The way the service code works in ICU 2.8 is that if
        // there is a real registered break iterator, the actualLoc
        // will be populated, but if the handleDefault path is taken
        // (because nothing is registered that can handle the
        // requested locale) then the actualLoc comes back empty.  In
        // that case, the returned object already has its actual/valid
        // locale data populated (by makeInstance, which is what
        // handleDefault calls), so we don't touch it.  YES, A COMMENT
        // THIS LONG is a sign of bad code -- so the action item is to
        // revisit this in ICU 3.0 and clean it up/fix it/remove it.
        if (U_SUCCESS(status) && (result != NULL) && *actualLoc.getName() != 0) {
            U_LOCALE_BASED(locBased, *result);
            locBased.setLocaleIDs(actualLoc.getName(), actualLoc.getName());
        }
        return result;
    } else {
        return makeInstance(loc, kind, status);
    }
}

// -------------------------------------

URegistryKey
BreakIterator::registerInstance(BreakIterator* toAdopt, const Locale& locale, UBreakIteratorType kind, UErrorCode& status) 
{
    return getService()->registerInstance(toAdopt, locale, kind, status);
}

// -------------------------------------

UBool 
BreakIterator::unregister(URegistryKey key, UErrorCode& status) 
{
    if (U_SUCCESS(status)) {
        if (hasService()) {
            return gService->unregister(key, status);
        }
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    return FALSE;
}

// -------------------------------------

StringEnumeration* 
BreakIterator::getAvailableLocales(void)
{
  return getService()->getAvailableLocales();
}

// -------------------------------------

BreakIterator* 
BreakIterator::makeInstance(const Locale& loc, int32_t kind, UErrorCode& status)
{

	if (U_FAILURE(status)) {
		return NULL;
	}

    BreakIterator *result = NULL;
    switch (kind) {
    case UBRK_CHARACTER: 
        result = BreakIterator::makeCharacterInstance(loc, status);
        break;
    case UBRK_WORD:
        result = BreakIterator::makeWordInstance(loc, status);
        break;
    case UBRK_LINE:
        result = BreakIterator::makeLineInstance(loc, status);
        break;
    case UBRK_SENTENCE:
        result = BreakIterator::makeSentenceInstance(loc, status);
        break;
    case UBRK_TITLE:
        result = BreakIterator::makeTitleInstance(loc, status);
        break;
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }

	if (U_FAILURE(status)) {
		return NULL;
	}

    // this is more of a placeholder. All the break iterators have the same actual locale: root
    // except the Thai one
    ResourceBundle res(NULL, loc, status);
    U_LOCALE_BASED(locBased, *result);
    locBased.setLocaleIDs(res.getLocale(ULOC_VALID_LOCALE, status).getName(),
                          (uprv_strcmp(loc.getLanguage(), "th") == 0) ?
                          "th" : "root");
    return result;
}

Locale 
BreakIterator::getLocale(ULocDataLocaleType type, UErrorCode& status) const {
    U_LOCALE_BASED(locBased, *this);
    return locBased.getLocale(type, status);
}

const char *
BreakIterator::getLocaleID(ULocDataLocaleType type, UErrorCode& status) const {
    U_LOCALE_BASED(locBased, *this);
    return locBased.getLocaleID(type, status);
}

U_NAMESPACE_END

// defined in ucln_cmn.h

/**
 * Release all static memory held by breakiterator.  
 */
U_CFUNC UBool breakiterator_cleanup(void) {
    if (gService) {
        delete gService;
        gService = NULL;
    }
    return TRUE;
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

//eof
