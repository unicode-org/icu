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
#include "unicode/ures.h"
#include "ucln_cmn.h"
#include "cstring.h"
#include "mutex.h"
#include "iculserv.h"
#include "locbased.h"
#include "uresimp.h"

// *****************************************************************************
// class BreakIterator
// This class implements methods for finding the location of boundaries in text.
// Instances of BreakIterator maintain a current position and scan over text
// returning the index of characters where boundaries occur.
// *****************************************************************************

U_NAMESPACE_BEGIN

const int32_t BreakIterator::DONE = (int32_t)-1;

// -------------------------------------

BreakIterator*
BreakIterator::buildInstance(const Locale& loc, const char *type, UBool dict, UErrorCode &status)
{
    char fnbuff[256];
    char actualLocale[ULOC_FULLNAME_CAPACITY];
    int32_t size;
    const UChar* brkfname = NULL;
    UResourceBundle brkRulesStack, brkNameStack;
    UResourceBundle *brkRules = &brkRulesStack, *brkName = &brkNameStack;
    BreakIterator *result = NULL;
    
    if (U_FAILURE(status))
        return NULL;

    ures_initStackObject(brkRules);
    ures_initStackObject(brkName);

    // Get the locale
    UResourceBundle *b = ures_open(NULL, loc.getName(), &status);

    // Get the "boundaries" array.
    if (U_SUCCESS(status)) {
        brkRules = ures_getByKeyWithFallback(b, "boundaries", brkRules, &status);
        // Get the string object naming the rules file
        brkName = ures_getByKeyWithFallback(brkRules, type, brkName, &status);
        // Get the actual string
        brkfname = ures_getString(brkName, &size, &status);

        // Use the string if we found it
        if (U_SUCCESS(status) && brkfname) {
            uprv_strncpy(actualLocale,
                ures_getLocale(brkName, &status),
                sizeof(actualLocale)/sizeof(actualLocale[0]));
            u_UCharsToChars(brkfname, fnbuff, size+1);
        }
    }

    ures_close(brkRules);
    ures_close(brkName);
    
    UDataMemory* file = udata_open(NULL, "brk", fnbuff, &status);
    if (U_FAILURE(status)) {
        ures_close(b);
        return NULL;
    }

    // We found the break rules; now see if a dictionary is needed
    if (dict)
    {
        UErrorCode localStatus = U_ZERO_ERROR;
        brkName = &brkNameStack;
        ures_initStackObject(brkName);
        brkName = ures_getByKeyWithFallback(b, "BreakDictionaryData", brkName, &localStatus);
#if 0
        if (U_SUCCESS(localStatus)) {
            brkfname = ures_getString(&brkname, &size, &localStatus);
        }
#endif
        if (U_SUCCESS(localStatus)) {
#if 0
            u_UCharsToChars(brkfname, fnbuff, size);
            fnbuff[size] = '\0';
#endif
            result = new DictionaryBasedBreakIterator(file, "thaidict.brk", status);
        }
        ures_close(brkName);
    }
    
    // If there is still no result but we haven't had an error, no dictionary,
    // so make a non-dictionary break iterator
    if (U_SUCCESS(status) && result == NULL) {
        result = new RuleBasedBreakIterator(file, status);
    }

    // If there is a result, set the valid locale and actual locale
    if (U_SUCCESS(status) && result != NULL) {
        U_LOCALE_BASED(locBased, *result);
        locBased.setLocaleIDs(ures_getLocaleByType(b, ULOC_VALID_LOCALE, &status), actualLocale);
    }

    ures_close(b);
    
    if (U_FAILURE(status) && result != NULL) {  // Sometimes redundant check, but simple
        delete result;
        return NULL;
    }

    if (result == NULL) {
        udata_close(file);
        if (U_SUCCESS(status)) {
            status = U_MEMORY_ALLOCATION_ERROR;
        }
    }

    return result;
}

// Creates a break iterator for word breaks.
BreakIterator* U_EXPORT2
BreakIterator::createWordInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_WORD, status);
}

// -------------------------------------

// Creates a break iterator  for line breaks.
BreakIterator* U_EXPORT2
BreakIterator::createLineInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_LINE, status);
}

// -------------------------------------

// Creates a break iterator  for character breaks.
BreakIterator* U_EXPORT2
BreakIterator::createCharacterInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_CHARACTER, status);
}

// -------------------------------------

// Creates a break iterator  for sentence breaks.
BreakIterator* U_EXPORT2
BreakIterator::createSentenceInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_SENTENCE, status);
}

// -------------------------------------

// Creates a break iterator for title casing breaks.
BreakIterator* U_EXPORT2
BreakIterator::createTitleInstance(const Locale& key, UErrorCode& status)
{
    return createInstance(key, UBRK_TITLE, status);
}

// -------------------------------------

// Gets all the available locales that has localized text boundary data.
const Locale* U_EXPORT2
BreakIterator::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}

// -------------------------------------
// Gets the objectLocale display name in the default locale language.
UnicodeString& U_EXPORT2
BreakIterator::getDisplayName(const Locale& objectLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(name);
}

// -------------------------------------
// Gets the objectLocale display name in the displayLocale language.
UnicodeString& U_EXPORT2
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
#if !UCONFIG_NO_SERVICE

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
        : ICULocaleService(UNICODE_STRING("Break Iterator", 14))
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

U_NAMESPACE_END

// defined in ucln_cmn.h

/**
 * Release all static memory held by breakiterator.  
 */
U_CDECL_BEGIN
static UBool U_CALLCONV breakiterator_cleanup(void) {
#if !UCONFIG_NO_SERVICE
    if (gService) {
        delete gService;
        gService = NULL;
    }
#endif
    return TRUE;
}
U_CDECL_END
U_NAMESPACE_BEGIN

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
            ucln_common_registerCleanup(UCLN_COMMON_BREAKITERATOR, breakiterator_cleanup);
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

URegistryKey U_EXPORT2
BreakIterator::registerInstance(BreakIterator* toAdopt, const Locale& locale, UBreakIteratorType kind, UErrorCode& status) 
{
    return getService()->registerInstance(toAdopt, locale, kind, status);
}

// -------------------------------------

UBool U_EXPORT2
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

StringEnumeration* U_EXPORT2
BreakIterator::getAvailableLocales(void)
{
  return getService()->getAvailableLocales();
}
#endif /* UCONFIG_NO_SERVICE */

// -------------------------------------

BreakIterator*
BreakIterator::createInstance(const Locale& loc, UBreakIteratorType kind, UErrorCode& status)
{
    if (U_FAILURE(status)) {
        return NULL;
    }
    
    u_init(&status);
#if !UCONFIG_NO_SERVICE
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
    }
    else
#endif
    {
        return makeInstance(loc, kind, status);
    }
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
        result = BreakIterator::buildInstance(loc, "grapheme", FALSE, status);
        break;
    case UBRK_WORD:
        result = BreakIterator::buildInstance(loc, "word", TRUE, status);
        break;
    case UBRK_LINE:
        result = BreakIterator::buildInstance(loc, "line", TRUE, status);
        break;
    case UBRK_SENTENCE:
        result = BreakIterator::buildInstance(loc, "sentence", FALSE, status);
        break;
    case UBRK_TITLE:
        result = BreakIterator::buildInstance(loc, "title", FALSE, status);
        break;
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }

    if (U_FAILURE(status)) {
        return NULL;
    }

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

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

//eof
