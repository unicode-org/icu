/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 *******************************************************************************
 */
#include "unicode/utypes.h"

#if !UCONFIG_NO_SERVICE

#include "unicode/resbund.h"
#include "cmemory.h"
#include "iculserv.h"
#include "ustrfmt.h"

U_NAMESPACE_BEGIN

static Hashtable * LocaleUtility_cache = NULL;

#define UNDERSCORE_CHAR ((UChar)0x005f)

/*
 ******************************************************************
 */

UnicodeString&
LocaleUtility::canonicalLocaleString(const UnicodeString* id, UnicodeString& result)
{
  if (id == NULL) {
    result.setToBogus();
  } else {
    result = *id;
    int32_t i = 0;
    int32_t n = result.indexOf(UNDERSCORE_CHAR);
    if (n < 0) {
      n = result.length();
    }
    for (; i < n; ++i) {
      UChar c = result.charAt(i);
      if (c >= 0x0041 && c <= 0x005a) {
        c += 0x20;
        result.setCharAt(i, c);
      }
    }
    for (n = result.length(); i < n; ++i) {
      UChar c = result.charAt(i);
      if (c >= 0x0061 && c <= 0x007a) {
        c -= 0x20;
        result.setCharAt(i, c);
      }
    }
  }
  return result;
}

Locale&
LocaleUtility::initLocaleFromName(const UnicodeString& id, Locale& result)
{
    if (id.isBogus()) {
        result.setToBogus();
    } else {
        const int32_t BUFLEN = 128; // larger than ever needed
        char buffer[BUFLEN];
        int len = id.extract(0, BUFLEN, buffer);
        if (len >= BUFLEN) {
            result.setToBogus();
        } else {
            buffer[len] = '\0';
            result = Locale::createFromName(buffer);
        }
    }
    return result;
}

UnicodeString&
LocaleUtility::initNameFromLocale(const Locale& locale, UnicodeString& result)
{
    if (locale.isBogus()) {
        result.setToBogus();
    } else {
        result.append(locale.getName());
    }
    return result;
}

const Hashtable*
LocaleUtility::getAvailableLocaleNames(const UnicodeString& bundleID)
{
    // have to ignore bundleID for the moment, since we don't have easy C++ api.
    // assume it's the default bundle

    Hashtable* htp;
    umtx_lock(NULL);
    htp = LocaleUtility_cache;
    umtx_unlock(NULL);

    if (htp == NULL) {
        htp = new Hashtable();
        if (htp) {
            UErrorCode status = U_ZERO_ERROR;
            int32_t count = uloc_countAvailable();
            for (int32_t i = 0; i < count; ++i) {
                UnicodeString temp(uloc_getAvailable(i), "");
                htp->put(temp, (void*)htp, status);
                if (U_FAILURE(status)) {
                    delete htp;
                    return NULL;
                }
            }
            umtx_lock(NULL);
            if (LocaleUtility_cache == NULL) {
                LocaleUtility_cache = htp;
                htp = NULL;
            }
            umtx_unlock(NULL);
            delete htp;
        }
    }
    return LocaleUtility_cache;
}

UBool
LocaleUtility::isFallbackOf(const UnicodeString& root, const UnicodeString& child)
{
    return child.indexOf(root) == 0 &&
      (child.length() == root.length() ||
       child.charAt(root.length()) == UNDERSCORE_CHAR);
}

UBool
LocaleUtility::cleanup(void) {
    if (LocaleUtility_cache) {
        delete LocaleUtility_cache;
        LocaleUtility_cache = NULL;
    }
    return TRUE;
}

/*
 ******************************************************************
 */

const int32_t LocaleKey::KIND_ANY = -1;

LocaleKey*
LocaleKey::createWithCanonicalFallback(const UnicodeString* primaryID,
                                       const UnicodeString* canonicalFallbackID,
                                       UErrorCode& status)
{
    return LocaleKey::createWithCanonicalFallback(primaryID, canonicalFallbackID, KIND_ANY, status);
}

LocaleKey*
LocaleKey::createWithCanonicalFallback(const UnicodeString* primaryID,
                                       const UnicodeString* canonicalFallbackID,
                                       int32_t kind,
                                       UErrorCode& status)
{
    if (primaryID == NULL || U_FAILURE(status)) {
        return NULL;
    }
    UnicodeString canonicalPrimaryID;
    LocaleUtility::canonicalLocaleString(primaryID, canonicalPrimaryID);
    return new LocaleKey(*primaryID, canonicalPrimaryID, canonicalFallbackID, kind);
}

LocaleKey::LocaleKey(const UnicodeString& primaryID,
                     const UnicodeString& canonicalPrimaryID,
                     const UnicodeString* canonicalFallbackID,
                     int32_t kind)
  : ICUServiceKey(primaryID)
  , _kind(kind)
  , _primaryID(canonicalPrimaryID)
  , _fallbackID()
  , _currentID()
{
    _fallbackID.setToBogus();
    if (_primaryID.length() != 0) {
        if (canonicalFallbackID != NULL && _primaryID != *canonicalFallbackID) {
            _fallbackID = *canonicalFallbackID;
        }
    }

    _currentID = _primaryID;
}

UnicodeString&
LocaleKey::prefix(UnicodeString& result) const {
    if (_kind != KIND_ANY) {
        UChar buffer[64];
        uprv_itou(buffer, 64, _kind, 10, 0);
        UnicodeString temp(buffer);
        result.append(temp);
    }
    return result;
}

int32_t
LocaleKey::kind() const {
    return _kind;
}

UnicodeString&
LocaleKey::canonicalID(UnicodeString& result) const {
    return result.append(_primaryID);
}

UnicodeString&
LocaleKey::currentID(UnicodeString& result) const {
    if (!_currentID.isBogus()) {
        result.append(_currentID);
    }
    return result;
}

UnicodeString&
LocaleKey::currentDescriptor(UnicodeString& result) const {
    if (!_currentID.isBogus()) {
        prefix(result).append(PREFIX_DELIMITER).append(_currentID);
    } else {
        result.setToBogus();
    }
    return result;
}

Locale&
LocaleKey::canonicalLocale(Locale& result) const {
    return LocaleUtility::initLocaleFromName(_primaryID, result);
}

Locale&
LocaleKey::currentLocale(Locale& result) const {
    return LocaleUtility::initLocaleFromName(_currentID, result);
}

UBool
LocaleKey::fallback() {
    if (!_currentID.isBogus()) {
        int x = _currentID.lastIndexOf(UNDERSCORE_CHAR);
        if (x != -1) {
            _currentID.remove(x); // truncate current or fallback, whichever we're pointing to
            return TRUE;
        }

        if (!_fallbackID.isBogus()) {
            _currentID = _fallbackID;
            _fallbackID.setToBogus();
            return TRUE;
        }

        if (_currentID.length() > 0) {
            _currentID.remove(0); // completely truncate
            return TRUE;
        }

        _currentID.setToBogus();
    }

    return FALSE;
}

UBool
LocaleKey::isFallbackOf(const UnicodeString& id) const {
    UnicodeString temp(id);
    parseSuffix(temp);
    return temp.indexOf(_primaryID) == 0 &&
        (temp.length() == _primaryID.length() ||
        temp.charAt(_primaryID.length()) == UNDERSCORE_CHAR);
}

#ifdef SERVICE_DEBUG
UnicodeString&
LocaleKey::debug(UnicodeString& result) const
{
    ICUServiceKey::debug(result);
    result.append(" kind: ");
    result.append(_kind);
    result.append(" primaryID: ");
    result.append(_primaryID);
    result.append(" fallbackID: ");
    result.append(_fallbackID);
    result.append(" currentID: ");
    result.append(_currentID);
    return result;
}

UnicodeString&
LocaleKey::debugClass(UnicodeString& result) const
{
    return result.append("LocaleKey ");
}
#endif

const char LocaleKey::fgClassID = 0;

/*
 ******************************************************************
 */

LocaleKeyFactory::LocaleKeyFactory(int32_t coverage)
  : _name()
  , _coverage(coverage)
{
}

LocaleKeyFactory::LocaleKeyFactory(int32_t coverage, const UnicodeString& name)
  : _name(name)
  , _coverage(coverage)
{
}

LocaleKeyFactory::~LocaleKeyFactory() {
}

UObject*
LocaleKeyFactory::create(const ICUServiceKey& key, const ICUService* service, UErrorCode& status) const {
    if (handlesKey(key, status)) {
        const LocaleKey& lkey = (const LocaleKey&)key;
        int32_t kind = lkey.kind();
        Locale loc;
        lkey.currentLocale(loc);

        return handleCreate(loc, kind, service, status);
    }
    return NULL;
}

UBool
LocaleKeyFactory::handlesKey(const ICUServiceKey& key, UErrorCode& status) const {
    const Hashtable* supported = getSupportedIDs(status);
    if (supported) {
        UnicodeString id;
        key.currentID(id);
        return supported->get(id) != NULL;
    }
    return FALSE;
}

void
LocaleKeyFactory::updateVisibleIDs(Hashtable& result, UErrorCode& status) const {
    const Hashtable* supported = getSupportedIDs(status);
    if (supported) {
        UBool visible = (_coverage & 0x1) == 0;

        const UHashElement* elem = NULL;
        int32_t pos = 0;
        while (elem = supported->nextElement(pos)) {
            const UnicodeString& id = *((const UnicodeString*)elem->key.pointer);
            if (!visible) {
                result.remove(id);
            } else {
                result.put(id, (void*)this, status); // this is dummy non-void marker used for set semantics
                if (U_FAILURE(status)) {
                    break;
                }
            }
        }
    }
}

UnicodeString&
LocaleKeyFactory::getDisplayName(const UnicodeString& id, const Locale& locale, UnicodeString& result) const {
    if ((_coverage & 0x1) == 0) {
        UErrorCode status = U_ZERO_ERROR;
        if (isSupportedID(id, status)) {
            Locale loc;
            LocaleUtility::initLocaleFromName(id, loc);
            return loc.getDisplayName(locale, result);
        }
    }
    result.setToBogus();
    return result;
}

UObject*
LocaleKeyFactory::handleCreate(const Locale& loc, int32_t kind, const ICUService* service, UErrorCode& status) const {
    return NULL;
}

UBool
LocaleKeyFactory::isSupportedID(const UnicodeString& id, UErrorCode& status) const {
    const Hashtable* ids = getSupportedIDs(status);
    return ids && ids->get(id);
}

const Hashtable*
LocaleKeyFactory::getSupportedIDs(UErrorCode& status) const {
    return NULL;
}

#ifdef SERVICE_DEBUG
UnicodeString&
LocaleKeyFactory::debug(UnicodeString& result) const
{
    debugClass(result);
    result.append(", name: ");
    result.append(_name);
    result.append(", coverage: ");
    result.append(_coverage);
    return result;
}

UnicodeString&
LocaleKeyFactory::debugClass(UnicodeString& result) const
{
  return result.append("LocaleKeyFactory");
}
#endif

const char LocaleKeyFactory::fgClassID = 0;

/*
 ******************************************************************
 */

SimpleLocaleKeyFactory::SimpleLocaleKeyFactory(UObject* objToAdopt,
                                               const UnicodeString& locale,
                                               int32_t kind,
                                               int32_t coverage)
  : LocaleKeyFactory(coverage)
  , _obj(objToAdopt)
  , _id(locale)
  , _kind(kind)
{
}

SimpleLocaleKeyFactory::SimpleLocaleKeyFactory(UObject* objToAdopt,
                                               const Locale& locale,
                                               int32_t kind,
                                               int32_t coverage)
  : LocaleKeyFactory(coverage)
  , _obj(objToAdopt)
  , _id()
  , _kind(kind)
{
	LocaleUtility::initNameFromLocale(locale, _id);
}

SimpleLocaleKeyFactory::~SimpleLocaleKeyFactory()
{
  delete _obj;
  _obj = NULL;
}

UObject*
SimpleLocaleKeyFactory::create(const ICUServiceKey& key, const ICUService* service, UErrorCode& status) const
{
    if (U_SUCCESS(status)) {
        const LocaleKey& lkey = (const LocaleKey&)key;
        if (_kind == LocaleKey::KIND_ANY || _kind == lkey.kind()) {
            UnicodeString keyID;
            lkey.currentID(keyID);
            if (_id == keyID) {
                return service->cloneInstance(_obj);
            }
        }
    }
    return NULL;
}

UBool
SimpleLocaleKeyFactory::isSupportedID(const UnicodeString& id, UErrorCode& status) const
{
    return id == _id;
}

void
SimpleLocaleKeyFactory::updateVisibleIDs(Hashtable& result, UErrorCode& status) const
{
    if (U_SUCCESS(status)) {
        if (_coverage & 0x1) {
            result.remove(_id);
        } else {
            result.put(_id, (void*)this, status);
        }
    }
}

#ifdef SERVICE_DEBUG
UnicodeString&
SimpleLocaleKeyFactory::debug(UnicodeString& result) const
{
    LocaleKeyFactory::debug(result);
    result.append(", id: ");
    result.append(_id);
    result.append(", kind: ");
    result.append(_kind);
    return result;
}

UnicodeString&
SimpleLocaleKeyFactory::debugClass(UnicodeString& result) const
{
    return result.append("SimpleLocaleKeyFactory");
}
#endif

const char SimpleLocaleKeyFactory::fgClassID = 0;

/*
 ******************************************************************
 */

ICUResourceBundleFactory::ICUResourceBundleFactory()
  : LocaleKeyFactory(VISIBLE)
  , _bundleName()
{
}

ICUResourceBundleFactory::ICUResourceBundleFactory(const UnicodeString& bundleName)
  : LocaleKeyFactory(VISIBLE)
  , _bundleName(bundleName)
{
}

const Hashtable*
ICUResourceBundleFactory::getSupportedIDs(UErrorCode& status) const
{
    if (U_SUCCESS(status)) {
        return LocaleUtility::getAvailableLocaleNames(_bundleName);
    }
    return NULL;
}

UObject*
ICUResourceBundleFactory::handleCreate(const Locale& loc, int32_t kind, const ICUService* service, UErrorCode& status) const
{
    if (U_SUCCESS(status)) {
        return new ResourceBundle(_bundleName, loc, status);
    }
    return NULL;
}

#ifdef SERVICE_DEBUG
UnicodeString&
ICUResourceBundleFactory::debug(UnicodeString& result) const
{
    LocaleKeyFactory::debug(result);
    result.append(", bundle: ");
    return result.append(_bundleName);
}

UnicodeString&
ICUResourceBundleFactory::debugClass(UnicodeString& result) const
{
    return result.append("ICUResourceBundleFactory");
}
#endif

const char ICUResourceBundleFactory::fgClassID = '\0';

/*
 ******************************************************************
 */

ICULocaleService::ICULocaleService()
  : fallbackLocale(Locale::getDefault())
  , llock(0)
{
  umtx_init(&llock);
}

ICULocaleService::ICULocaleService(const UnicodeString& dname)
  : ICUService(dname)
  , fallbackLocale(Locale::getDefault())
  , llock(0)
{
  umtx_init(&llock);
}

ICULocaleService::~ICULocaleService()
{
  umtx_destroy(&llock);
}

UObject*
ICULocaleService::get(const Locale& locale, UErrorCode& status) const
{
    return get(locale, LocaleKey::KIND_ANY, NULL, status);
}

UObject*
ICULocaleService::get(const Locale& locale, int32_t kind, UErrorCode& status) const
{
    return get(locale, kind, NULL, status);
}

UObject*
ICULocaleService::get(const Locale& locale, Locale* actualReturn, UErrorCode& status) const
{
    return get(locale, LocaleKey::KIND_ANY, actualReturn, status);
}

UObject*
ICULocaleService::get(const Locale& locale, int32_t kind, Locale* actualReturn, UErrorCode& status) const
{
    UObject* result = NULL;
    if (U_FAILURE(status)) {
        return result;
    }

    UnicodeString locName(locale.getName(), "");
    if (locName.isBogus()) {
        status = U_MEMORY_ALLOCATION_ERROR;
    } else {
        ICUServiceKey* key = createKey(&locName, kind, status);
        if (key) {
            if (actualReturn == NULL) {
                result = getKey(*key, status);
            } else {
                UnicodeString temp;
                result = getKey(*key, &temp, status);

                if (result != NULL) {
                    key->parseSuffix(temp);
                    LocaleUtility::initLocaleFromName(temp, *actualReturn);
                }
            }
            delete key;
        }
    }
    return result;
}


URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const UnicodeString& locale, 
	UBool visible, UErrorCode& status)
{
	Locale loc;
	LocaleUtility::initLocaleFromName(locale, loc);
    return registerInstance(objToAdopt, loc, LocaleKey::KIND_ANY, 
		visible ? LocaleKeyFactory::VISIBLE : LocaleKeyFactory::INVISIBLE, status);
}

URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const Locale& locale, UErrorCode& status)
{
    return registerInstance(objToAdopt, locale, LocaleKey::KIND_ANY, LocaleKeyFactory::VISIBLE, status);
}

URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const Locale& locale, int32_t kind, UErrorCode& status)
{
    return registerInstance(objToAdopt, locale, kind, LocaleKeyFactory::VISIBLE, status);
}

URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const Locale& locale, int32_t kind, int32_t coverage, UErrorCode& status)
{
    ICUServiceFactory * factory = new SimpleLocaleKeyFactory(objToAdopt, locale, kind, coverage);
    if (factory != NULL) {
        return registerFactory(factory, status);
    }
    delete objToAdopt;
    return NULL;
}

#if 0
URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const UnicodeString& locale, UErrorCode& status)
{
    return registerInstance(objToAdopt, locale, LocaleKey::KIND_ANY, LocaleKeyFactory::VISIBLE, status);
}

URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const UnicodeString& locale, UBool visible, UErrorCode& status)
{
    return registerInstance(objToAdopt, locale, LocaleKey::KIND_ANY,
                            visible ? LocaleKeyFactory::VISIBLE : LocaleKeyFactory::INVISIBLE,
                            status);
}

URegistryKey
ICULocaleService::registerInstance(UObject* objToAdopt, const UnicodeString& locale, int32_t kind, int32_t coverage, UErrorCode& status)
{
    ICUServiceFactory * factory = new SimpleLocaleKeyFactory(objToAdopt, locale, kind, coverage);
    if (factory != NULL) {
        return registerFactory(factory, status);
    }
    delete objToAdopt;
    return NULL;
}
#endif

class ServiceEnumeration : public StringEnumeration {
private:
    const ICULocaleService* _service;
    int32_t _timestamp;
    UVector _ids;
    int32_t _pos;
    void* _bufp;
    int32_t _buflen;

private:
    ServiceEnumeration(const ICULocaleService* service, UErrorCode status)
        : _service(service)
        , _timestamp(service->getTimestamp())
        , _ids(uhash_deleteUnicodeString, NULL, status)
        , _pos(0)
        , _bufp(NULL)
        , _buflen(0)
    {
        _service->getVisibleIDs(_ids, status);
    }

public:
    static ServiceEnumeration* create(const ICULocaleService* service) {
        UErrorCode status = U_ZERO_ERROR;
        ServiceEnumeration* result = new ServiceEnumeration(service, status);
        if (U_SUCCESS(status)) {
            return result;
        }
        delete result;
        return NULL;
    }

    virtual ~ServiceEnumeration() {
        uprv_free(_bufp);
    }

    virtual int32_t count(UErrorCode& status) const {
        return upToDate(status) ? _ids.size() : 0;
    }

    const char* next(int32_t* resultLength, UErrorCode& status) {
        const UnicodeString* us = snext(status);
        if (us) {
            while (TRUE) {
                int32_t newlen = us->extract((char*)_bufp, _buflen / sizeof(char), NULL, status);
                if (status == U_STRING_NOT_TERMINATED_WARNING || status == U_BUFFER_OVERFLOW_ERROR) {
                    resizeBuffer((newlen + 1) * sizeof(char));
                    status = U_ZERO_ERROR;
                } else if (U_SUCCESS(status)) {
                    ((char*)_bufp)[newlen] = 0;
                    if (resultLength) {
                        resultLength[0] = newlen;
                    }
                    return (const char*)_bufp;
                } else {
                    break;
                }
            }
        }
        return NULL;
    }

    const UChar* unext(int32_t* resultLength, UErrorCode& status) {
        const UnicodeString* us = snext(status);
        if (us) {
            while (TRUE) {
                int32_t newlen = us->extract((UChar*)_bufp, _buflen / sizeof(UChar), status);
                if (status == U_STRING_NOT_TERMINATED_WARNING || status == U_BUFFER_OVERFLOW_ERROR) {
                    resizeBuffer((newlen + 1) * sizeof(UChar));
                } else if (U_SUCCESS(status)) {
                    ((UChar*)_bufp)[newlen] = 0;
                    if (resultLength) {
                        resultLength[0] = newlen;
                    }
                    return (const UChar*)_bufp;
                } else {
                    break;
                }
            }
        }
        return NULL;
    }

    const UnicodeString* snext(UErrorCode& status) {
        if (upToDate(status) && (_pos < _ids.size())) {
            return (const UnicodeString*)_ids[_pos++];
        }
        return NULL;
    }

    void resizeBuffer(int32_t newlen) {
        if (_bufp) {
            _bufp = uprv_realloc(_bufp, newlen);
        } else {
            _bufp = uprv_malloc(newlen);
        }
        _buflen = newlen;
    }

    UBool upToDate(UErrorCode& status) const {
        if (U_SUCCESS(status)) {
            if (_timestamp == _service->getTimestamp()) {
                return TRUE;
            }
            status = U_ENUM_OUT_OF_SYNC_ERROR;
        }
        return FALSE;
    }

    void reset(UErrorCode& status) {
		if (status == U_ENUM_OUT_OF_SYNC_ERROR) {
			status = U_ZERO_ERROR;
		}
        if (U_SUCCESS(status)) {
            _timestamp = _service->getTimestamp();
            _pos = 0;
            _service->getVisibleIDs(_ids, status);
        }
    }

public:
    virtual UClassID getDynamicClassID(void) const { return getStaticClassID(); }
    static UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }
private:
    static const char fgClassID;
};

const char ServiceEnumeration::fgClassID = '\0';

StringEnumeration*
ICULocaleService::getAvailableLocales(void) const
{
    return ServiceEnumeration::create(this);
}

const UnicodeString&
ICULocaleService::validateFallbackLocale() const
{
    const Locale&     loc    = Locale::getDefault();
    ICULocaleService* ncThis = (ICULocaleService*)this;
    {
        Mutex mutex(&ncThis->llock);
        if (loc != fallbackLocale) {
            ncThis->fallbackLocale = loc;
            LocaleUtility::initNameFromLocale(loc, ncThis->fallbackLocaleName);
            ncThis->clearServiceCache();
        }
    }
    return fallbackLocaleName;
}

ICUServiceKey*
ICULocaleService::createKey(const UnicodeString* id, UErrorCode& status) const
{
    return LocaleKey::createWithCanonicalFallback(id, &validateFallbackLocale(), status);
}

ICUServiceKey*
ICULocaleService::createKey(const UnicodeString* id, int32_t kind, UErrorCode& status) const
{
    return LocaleKey::createWithCanonicalFallback(id, &validateFallbackLocale(), kind, status);
}

U_NAMESPACE_END

// defined in ucln_cmn.h

/**
 * Release all static memory held by Locale Utility.  
 */
U_CFUNC UBool service_cleanup(void) {
  return LocaleUtility::cleanup();
}

/* !UCONFIG_NO_SERVICE */
#endif


