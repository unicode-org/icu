/*
*******************************************************************************
* Copyright (C) 1997-2001, International Business Machines Corporation and    *
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
  return createInstance(key, BREAK_WORD, status);
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
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        delete result;
        result = NULL;
    }

    return result;
}

// -------------------------------------

// Creates a break iterator  for line breaks.
BreakIterator*
BreakIterator::createLineInstance(const Locale& key, UErrorCode& status)
{
  return createInstance(key, BREAK_LINE, status);
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
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        delete result;
        result = NULL;
    }
    return result;
}

// -------------------------------------

// Creates a break iterator  for character breaks.
BreakIterator*
BreakIterator::createCharacterInstance(const Locale& key, UErrorCode& status)
{
  return createInstance(key, BREAK_CHARACTER, status);
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
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        delete result;
        result = NULL;
    }
    return result;
}

// -------------------------------------

// Creates a break iterator  for sentence breaks.
BreakIterator*
BreakIterator::createSentenceInstance(const Locale& key, UErrorCode& status)
{
  return createInstance(key, BREAK_SENTENCE, status);
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
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        delete result;
        result = NULL;
    }

    return result;
}

// -------------------------------------

// Creates a break iterator for title casing breaks.
BreakIterator*
BreakIterator::createTitleInstance(const Locale& key, UErrorCode& status)
{
  return createInstance(key, BREAK_TITLE, status);
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
    if (result == NULL) {
        udata_close(file);
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    if (U_FAILURE(status)) {   // Sometimes redundant check, but simple.
        delete result;
        result = NULL;
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
  virtual UObject* handleCreate(const Locale& loc, int32_t kind, const ICUService* service, UErrorCode& status) const {
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

  virtual UObject* handleDefault(const Key& key, UnicodeString* actualID, UErrorCode& status) const {
	LocaleKey& lkey = (LocaleKey&)key;
	int32_t kind = lkey.kind();
	Locale loc;
	lkey.currentLocale(loc);
	return BreakIterator::makeInstance(loc, kind, status); // default to root
  }

  virtual UBool isDefault() const {
	return countFactories() == 1;
  }
};

// -------------------------------------

static UMTX gLock = 0;

static ICULocaleService* 
getService(void)
{
  if (gService == NULL) {
    Mutex mutex(&gLock);
    if (gService == NULL) {
      gService = new ICUBreakIteratorService();
    }
  }
  return gService;
}

// -------------------------------------

BreakIterator*
BreakIterator::createInstance(const Locale& loc, UBreakType kind, UErrorCode& status)
{
  if (gService != NULL) {
    return (BreakIterator*)gService->get(loc, kind, status);
  } else {
    return makeInstance(loc, kind, status);
  }
}

// -------------------------------------

const UObject* 
BreakIterator::registerBreak(BreakIterator* toAdopt, const Locale& locale, UBreakType kind, UErrorCode& status) 
{
  return getService()->registerObject(toAdopt, locale, kind, status);
}

// -------------------------------------

UBool 
BreakIterator::unregisterBreak(const UObject* key, UErrorCode& status) 
{
  if (gService != NULL) {
    return gService->unregisterFactory((Factory*)key, status);
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
    switch (kind) {
    case BREAK_CHARACTER: return BreakIterator::makeCharacterInstance(loc, status);
    case BREAK_WORD: return BreakIterator::makeWordInstance(loc, status);
    case BREAK_LINE: return BreakIterator::makeLineInstance(loc, status);
    case BREAK_SENTENCE: return BreakIterator::makeSentenceInstance(loc, status);
    case BREAK_TITLE: return BreakIterator::makeTitleInstance(loc, status);
    default:
      status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }
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
  umtx_destroy(&gLock);
  return TRUE;
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

//eof
