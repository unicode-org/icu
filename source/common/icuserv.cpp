 /**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation.       *
 * All Rights Reserved.                                                        *
 *******************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_SERVICE

#include "icuserv.h"

#undef SERVICE_REFCOUNT

// in case we use the refcount stuff

U_NAMESPACE_BEGIN

// A reference counted wrapper for an object.  Creation and access is
// through RefHandle.

#ifdef SERVICE_REFCOUNT

#include "unicode/strenum.h"

/*
 ******************************************************************
 */

class RefCounted {
private:
  int32_t _count;
  UObject* _obj;

  friend class RefHandle;

  RefCounted(UObject* objectToAdopt) : _count(1), _obj(objectToAdopt) {}
  ~RefCounted() { delete _obj; }
  void ref() { umtx_atomic_inc(&_count); }
  void unref() { if (umtx_atomic_dec(&_count) == 0) { delete this; }}
};

/*
 ******************************************************************
 */

// Reference counted handle for an object
class RefHandle {
  RefCounted* _ref;
  
public:
  RefHandle() : _ref(NULL) {}
  RefHandle(UObject* obj) : _ref(new RefCounted(obj)) {}
  RefHandle(const RefHandle& rhs) : _ref(NULL) { operator=(rhs); }
  ~RefHandle() { if (_ref) _ref->unref(); }
  RefHandle& operator=(const RefHandle& rhs) {
    if (rhs._ref) rhs._ref->ref();
    if (_ref) _ref->unref();
    _ref = rhs._ref;
  }
  const UObject* get() const { return _ref ? _ref->_obj : NULL; }
};

/*
 ******************************************************************
 */

// Generic enumeration class with fail-fast behavior.

class MapEnumeration : public UObject, public StringEnumeration
{
    private:
    UChar* _buffer;
    int _buflen;

    protected:
    const ICUService* _service;
    uint32_t _timestamp;
    RefHandle _table;
    Key* _filter;
    int32_t _position;
    int32_t _count;

    protected:
    MapEnumeration(ICUService* service, int32_t timestamp, RefHandle& table, Key* filter = NULL)
        : _buffer(NULL)
        , _buflen(0)
        , _service(service)
        , _timestamp(timestamp)
        , _table(table)
        , _filter(filter)
        , _position(0)
        , _count(((const Hashtable*)table.get())->count())
    {
    }
   
    virtual ~MapEnumeration()
    {
        delete _filter;
    }

    int32_t count(UErrorCode& status) const
    {
        return U_SUCCESS(status) ? _count : 0;
    }

    const char* next(UErrorCode& status) {
        const UnicodeString* us = snext(status);
        if (us) {
            int newlen;
            for (newlen = us->extract((char*)_buffer, _buflen / sizeof(char), NULL, status);
                 status == U_STRING_NOT_TERMINATED_WARNING || status == U_BUFFER_OVERFLOW_ERROR;
                 resizeBuffer((newlen + 1) * sizeof(char)));
      
            if (U_SUCCESS(status)) {
                ((char*)_buffer)[newlen] = 0;
                return (const char*)_buffer;
            }
        }
        return NULL;
    }

    const UChar* unext(UErrorCode& status) {
        const UnicodeString* us = snext(status);
        if (us) {
            int newlen;
            for (newlen = us->extract((UChar*)_buffer, _buflen / sizeof(UChar), NULL, status);
                 status == U_STRING_NOT_TERMINATED_WARNING || status == U_BUFFER_OVERFLOW_ERROR;
                 resizeBuffer((newlen + 1) * sizeof(UChar)));
      
            if (U_SUCCESS(status)) {
                ((UChar*)_buffer)[newlen] = 0;
                return (const UChar*)_buffer;
            }
        }
        return NULL;
    }

    const UnicodeString* snext(UErrorCode& status) 
    {
        if (U_SUCCESS(status)) {
            if (_timestamp != _service->_timestamp) {
                status = U_ENUM_OUT_OF_SYNCH_ERROR;
            } else {
                return internalNext((Hashtable*)_table.get());
            }
        }
        return NULL;
    }

    void reset(UErrorCode& status)
    {
        if (U_SUCCESS(status)) {
            service->reset(this);
        }
    }

    protected:
    virtual const UnicodeString* internalNext(Hashtable* table) = 0;

    private:
    void reset(RefHandle& table, int32_t timestamp)
    {
        _table = table;
        _timestamp = timestamp;
        _position = 0;
        _count = ((const Hashtable*)table.get())->count();
    }

    friend class ICUService;
};

/*
 ******************************************************************
 */

// An enumeration over the visible ids in a service.  The ids
// are in the hashtable, which is refcounted, so it will not
// disappear as long as the enumeration exists even if the
// service itself unrefs it.  For "fail-fast" behavior the
// enumeration checks the timestamp of the service, but this
// is not a guarantee that the result the client receives will
// still be valid once the function returns.

class IDEnumeration : public MapEnumeration {
public:
  IDEnumeration(ICUService* service, int32_t timestamp, RefHandle& table, Key* filter = NULL)
    : MapEnumeration(service, timestamp, table, filter)
  {
  }

protected:
  const UnicodeString* internalNext(Hashtable* table) {
    while (TRUE) {
      const UnicodeString* elem = (const UnicodeString*)(table->nextElement(_position).key.pointer);
      if (elem == NULL ||
          _filter == NULL ||
          _filter->isFallbackOf(*elem)) {
        return elem;
      }
    }
    return NULL;
  }
};

/*
 ******************************************************************
 */

class DisplayEnumeration : public MapEnumeration {
private:
  Locale _locale;
  UnicodeString _cache;

public:
  DisplayEnumeration(ICUService* service, int32_t timestamp, RefHandle& table, Locale& locale, Key* filter = NULL)
    : MapEnumeration(service, timestamp, table, filter), _locale(locale)
  {
  }

protected:
  const UnicodeString* internalNext(Hashtable* table) {
    while (TRUE) {
      UHashElement* elem = table->nextElement(_position);
      if (elem == NULL) {
        return NULL;
      }
      const UnicodeString* id = (const UnicodeString*)elem->key.pointer;
      const Factory* factory = (const Factory*)elem->value.pointer;
      if (_filter == NULL || _filter->isFallbackOf(*id)) {
        factory->getDisplayName(*id, cache, locale);
        return &cache;
      }
    }
    return NULL;
  }
};

/* SERVICE_REFCOUNT */
#endif

/*
 ******************************************************************
 */

const UChar Key::PREFIX_DELIMITER = '/';

Key::Key(const UnicodeString& id) 
  : _id(id) {
}

Key::~Key() 
{
}

const UnicodeString& 
Key::getID() const 
{
  return _id;
}

UnicodeString& 
Key::canonicalID(UnicodeString& result) const 
{
  return result.append(_id);
}

UnicodeString& 
Key::currentID(UnicodeString& result) const 
{
  return canonicalID(result);
}

UnicodeString& 
Key::currentDescriptor(UnicodeString& result) const 
{
  prefix(result);
  result.append(PREFIX_DELIMITER);
  return currentID(result);
}

UBool 
Key::fallback() 
{
  return FALSE;
}

UBool 
Key::isFallbackOf(const UnicodeString& id) const 
{
  return id == _id;
}

UnicodeString& 
Key::prefix(UnicodeString& result) const 
{
  return result;
}

UnicodeString& 
Key::parsePrefix(UnicodeString& result) 
{
  int32_t n = result.indexOf(PREFIX_DELIMITER);
  if (n < 0) {
    n = 0;
  }
  result.remove(n);
  return result;
}

UnicodeString& 
Key::parseSuffix(UnicodeString& result) 
{
  int32_t n = result.indexOf(PREFIX_DELIMITER);
  if (n >= 0) {
    result.remove(0, n+1);
  }
  return result;
}

UnicodeString& 
Key::debug(UnicodeString& result) const 
{
  debugClass(result);
  result.append(" id: ");
  result.append(_id);
  return result;
}

UnicodeString& 
Key::debugClass(UnicodeString& result) const 
{
  return result.append("Key");
}

const char Key::fgClassID = '\0';

/*
 ******************************************************************
 */

SimpleFactory::SimpleFactory(UObject* instanceToAdopt, const UnicodeString& id) 
  : _instance(instanceToAdopt), _id(id), _visible(TRUE)
{
}

SimpleFactory::SimpleFactory(UObject* instanceToAdopt, const UnicodeString& id, UBool visible) 
  : _instance(instanceToAdopt), _id(id), _visible(visible)
{
}

SimpleFactory::~SimpleFactory() 
{
  delete _instance;
}

UObject* 
SimpleFactory::create(const Key& key, const ICUService* service, UErrorCode& status) const 
{
  if (U_SUCCESS(status)) {
    UnicodeString temp;
    if (_id == key.currentID(temp)) {
      return service->cloneInstance(_instance); 
    }
  }
  return NULL;
}

void 
SimpleFactory::updateVisibleIDs(Hashtable& result, UErrorCode& status) const 
{
  if (_visible) {
    result.put(_id, (void*)this, status); // cast away const
  } else {
    result.remove(_id);
  }
}

UnicodeString& 
SimpleFactory::getDisplayName(const UnicodeString& id, const Locale& locale, UnicodeString& result) const 
{
  if (_visible && _id == id) {
    result = _id;
  } else {
    result.setToBogus();
  }
  return result;
}

UnicodeString& 
SimpleFactory::debug(UnicodeString& toAppendTo) const 
{
  debugClass(toAppendTo);
  toAppendTo.append(" id: ");
  toAppendTo.append(_id);
  toAppendTo.append(", visible: ");
  toAppendTo.append(_visible ? "T" : "F");
  return toAppendTo;
}

UnicodeString& 
SimpleFactory::debugClass(UnicodeString& toAppendTo) const 
{
  return toAppendTo.append("SimpleFactory");
}

const char SimpleFactory::fgClassID = '\0';

/*
 ******************************************************************
 */

const char ServiceListener::fgClassID = '\0';

/*
 ******************************************************************
 */

 // Record the actual id for this service in the cache, so we can return it
 // even if we succeed later with a different id.
class CacheEntry {
private:
  int32_t refcount;

public:
  UnicodeString actualDescriptor;
  UObject* service;

  /**
   * Releases a reference to the shared resource.
   */
  ~CacheEntry() {
    delete service;
  }

  CacheEntry(const UnicodeString& _actualDescriptor, UObject* _service) 
    : refcount(1), actualDescriptor(_actualDescriptor), service(_service) {
  }

  /**
   * Instantiation creates an initial reference, so don't call this
   * unless you're creating a new pointer to this.  Management of
   * that pointer will have to know how to deal with refcounts.  
   * Return true if the resource has not already been released.
   */
  CacheEntry* ref() {
    ++refcount;
    return this;
  }

  /**
   * Destructions removes a reference, so don't call this unless
   * you're removing pointer to this somewhere.  Management of that
   * pointer will have to know how to deal with refcounts.  Once
   * the refcount drops to zero, the resource is released.  Return
   * false if the resouce has been released.
   */
  CacheEntry* unref() {
    if ((--refcount) == 0) {
      delete this;
      return NULL;
    }
    return this;
  }

  /**
   * Return TRUE if there is at least one reference to this and the
   * resource has not been released.
   */
  UBool isShared() const {
    return refcount > 1;
  }
};

// UObjectDeleter for serviceCache

U_CAPI void U_EXPORT2
cacheDeleter(void* obj) {
  U_NAMESPACE_USE
    ((CacheEntry*)obj)->unref();
}

/**
 * Deleter for UObjects
 */
U_CAPI void U_EXPORT2
deleteUObject(void *obj) {
  U_NAMESPACE_USE
    delete (UObject*) obj;
}

/*
 ******************************************************************
 */

class DNCache {
public:
  Hashtable cache;
  const Locale locale;

  DNCache(const Locale& _locale) 
    : cache(FALSE), locale(_locale) 
  {
    // cache.setKeyDeleter(uhash_deleteUnicodeString);
  }
};


/*
 ******************************************************************
 */

StringPair* 
StringPair::create(const UnicodeString& displayName, 
                   const UnicodeString& id,
                   UErrorCode& status)
{
  if (U_SUCCESS(status)) {
    StringPair* sp = new StringPair(displayName, id);
    if (sp == NULL || sp->isBogus()) {
      status = U_MEMORY_ALLOCATION_ERROR;
      delete sp;
      return NULL;
    }
    return sp;
  }
  return NULL;
}

UBool 
StringPair::isBogus() const {
  return displayName.isBogus() || id.isBogus();
}

StringPair::StringPair(const UnicodeString& _displayName, 
                       const UnicodeString& _id)
  : displayName(_displayName)
  , id(_id)
{
}

U_CAPI void U_EXPORT2
deleteStringPair(void *obj) {
  U_NAMESPACE_USE
    delete (StringPair*) obj;
}

/*
 ******************************************************************
 */

ICUService::ICUService()
  : name()
  , lock(0)
  , timestamp(0)
  , factories(NULL)
  , serviceCache(NULL)
  , idCache(NULL)
  , dnCache(NULL)
{
}

ICUService::ICUService(const UnicodeString& name) 
  : name(name)
  , lock(0)
  , timestamp(0)
  , factories(NULL)
  , serviceCache(NULL)
  , idCache(NULL)
  , dnCache(NULL) {
}

ICUService::~ICUService()
 {
  Mutex mutex(&lock);
  clearCaches();
  delete factories;
  factories = NULL;
}

UObject* 
ICUService::get(const UnicodeString& descriptor, UErrorCode& status) const 
{
  return get(descriptor, NULL, status);
}

UObject* 
ICUService::get(const UnicodeString& descriptor, UnicodeString* actualReturn, UErrorCode& status) const 
{
  UObject* result = NULL;
  if (U_SUCCESS(status)) {
    Key* key = createKey(&descriptor);
    if (key) {
      result = getKey(*key, actualReturn, NULL, status);
      delete key;
    }
  }
  return result;
}

UObject* 
ICUService::getKey(Key& key, UErrorCode& status) const 
{
  return getKey(key, NULL, NULL, status);
}

UObject* 
ICUService::getKey(Key& key, UnicodeString* actualReturn, UErrorCode& status) const 
{
  return getKey(key, actualReturn, NULL, status);
}

UObject* 
ICUService::getKey(Key& key, UnicodeString* actualReturn, const Factory* factory, UErrorCode& status) const 
{
  if (U_FAILURE(status)) {
    return NULL;
  }

  if (isDefault()) {
    return handleDefault(key, actualReturn, status);
  }

#ifdef DEBUG_SERVICE
  fprintf(stderr, "Service: " + name + " key: " + key);
#endif
   
  ICUService* ncthis = (ICUService*)this; // cast away semantic const

  CacheEntry* result = NULL;
  {
    // The factory list can't be modified until we're done, 
    // otherwise we might update the cache with an invalid result.
    // The cache has to stay in synch with the factory list.
    // ICU doesn't have monitors so we can't use rw locks, so 
    // we single-thread everything using this service, for now.

    Mutex mutex(&ncthis->lock);

    if (serviceCache == NULL) {
      ncthis->serviceCache = new Hashtable(FALSE, status);
      if (U_FAILURE(status)) {
        delete serviceCache;
        return NULL;
      }
      serviceCache->setValueDeleter(cacheDeleter);
    }

    UnicodeString currentDescriptor;
    UVector* cacheDescriptorList = NULL;
    UBool putInCache = FALSE;

#ifdef DEBUG_SERVICE
    int32_t NDebug = 0;
#endif

    int32_t startIndex = 0;
    int32_t limit = factories->size();
    UBool cacheResult = TRUE;

    if (factory != NULL) {
      for (int32_t i = 0; i < limit; ++i) {
        if (factory == (const Factory*)factories->elementAt(i)) {
          startIndex = i + 1;
          break;
        }
      }
      if (startIndex == 0) {
        // throw new InternalError("Factory " + factory + "not registered with service: " + this);
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
      }
      cacheResult = FALSE;
    }

    do {
      currentDescriptor.remove();
      key.currentDescriptor(currentDescriptor);
#ifdef DEBUG_SERVICE
      fprintf(stderr, name + "[" + NDebug++ + "] looking for: " + currentDescriptor);
#endif
      result = (CacheEntry*)serviceCache->get(currentDescriptor);
      if (result != NULL) {
#ifdef DEBUG_SERVICE
        fprintf(stderr, name + " found with descriptor: " + currentDescriptor);
#endif
        break;
      } else {
#ifdef DEBUG_SERVICE
        fprintf(stderr, "did not find: " + currentDescriptor + " in cache");
#endif
      }

      // first test of cache failed, so we'll have to update
      // the cache if we eventually succeed-- that is, if we're 
      // going to update the cache at all.
      putInCache = cacheResult;

      int32_t n = 0;
      int32_t index = startIndex;
      while (index < limit) {
        Factory* f = (Factory*)factories->elementAt(index++);
#ifdef DEBUG_SERVICE
        fprintf("trying factory[" + (index-1) + "] " + f.toString());
#endif
        UObject* service = f->create(key, this, status);
        if (U_FAILURE(status)) {
          delete cacheDescriptorList;
          return NULL;
        }
        if (service != NULL) {
          result = new CacheEntry(currentDescriptor, service);
          if (result == NULL) {
            delete service;
            delete cacheDescriptorList;
            status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
          }

#ifdef DEBUG_SERVICE
          fprintf(stderr, name + " factory supported: " + currentDescriptor + ", caching");
#endif
          goto outerEnd;
        } else {
#ifdef DEBUG_SERVICE
          fprintf(stderr, "factory did not support: " + currentDescriptor);
#endif
        }
      }

      // prepare to load the cache with all additional ids that 
      // will resolve to result, assuming we'll succeed.  We
      // don't want to keep querying on an id that's going to
      // fallback to the one that succeeded, we want to hit the
      // cache the first time next goaround.
      if (cacheDescriptorList == NULL) {
        cacheDescriptorList = new UVector(uhash_deleteUnicodeString, NULL, 5, status);
        if (U_FAILURE(status)) {
          return NULL;
        }
      }
      UnicodeString* idToCache = new UnicodeString(currentDescriptor);
      if (idToCache == NULL || idToCache->isBogus()) {
        delete cacheDescriptorList;
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
      }

      cacheDescriptorList->addElement(idToCache, status);
      if (U_FAILURE(status)) {
        delete cacheDescriptorList;
        return NULL;
      }
    } while (key.fallback());
  outerEnd:

    if (result != NULL) {
      if (putInCache) {
        serviceCache->put(result->actualDescriptor, result, status);
        if (U_FAILURE(status)) {
          delete result;
          delete cacheDescriptorList;
          return NULL;
        }

        if (cacheDescriptorList != NULL) {
          for (int32_t i = cacheDescriptorList->size(); --i >= 0;) {
            UnicodeString* desc = (UnicodeString*)cacheDescriptorList->elementAt(i);
#ifdef DEBUG_SERVICE
            fprintf(stderr, name + " adding descriptor: '" + desc + "' for actual: '" + result.actualDescriptor + "'");
#endif
            serviceCache->put(*desc, result, status);
            if (U_FAILURE(status)) {
              delete result;
              delete cacheDescriptorList;
              return NULL;
            }

            result->ref();
            cacheDescriptorList->removeElementAt(i);
          }

          delete cacheDescriptorList;
          cacheDescriptorList = NULL;
        }
      }

      if (actualReturn != NULL) {
        // strip null prefix
        if (result->actualDescriptor.indexOf("/") == 0) {
          actualReturn->remove();
          actualReturn->append(result->actualDescriptor, 
                               1, 
                               result->actualDescriptor.length() - 1);
        } else {
          *actualReturn = result->actualDescriptor;
        }

        if (actualReturn->isBogus()) {
          status = U_MEMORY_ALLOCATION_ERROR;
          return NULL;
        }
      }

#ifdef DEBUG_SERVICE
      fprintf(stderr, "found in service: " + name);
#endif
      return cloneInstance(result->service);
    }
  }
 
#ifdef DEBUG_SERVICE
  fprintf(stderr, "not found in service: " + name);
#endif

  return handleDefault(key, actualReturn, status);
}

UObject* 
ICUService::handleDefault(const Key& key, UnicodeString* actualIDReturn, UErrorCode& status) const 
{
  return NULL;
}

  
UVector& 
ICUService::getVisibleIDs(UVector& result, UErrorCode& status) const {
  return getVisibleIDs(result, NULL, status);
}

UVector& 
ICUService::getVisibleIDs(UVector& result, const UnicodeString* matchID, UErrorCode& status) const 
{
  result.removeAllElements();

  if (U_FAILURE(status)) {
    return result;
  }

  ICUService * ncthis = (ICUService*)this; // cast away semantic const
  {
    Mutex mutex(&ncthis->lock);
    const Hashtable* map = getVisibleIDMap(status);
    if (map != NULL) {
      Key* fallbackKey = createKey(matchID);

      for (int32_t pos = 0;;) {
        const UHashElement* e = map->nextElement(pos);
        if (e == NULL) {
          break;
        }

        const UnicodeString* id = (const UnicodeString*)e->key.pointer;
        if (fallbackKey != NULL) {
          if (!fallbackKey->isFallbackOf(*id)) {
            continue;
          }
        }

        UnicodeString* idClone = new UnicodeString(*id);
        if (idClone == NULL || idClone->isBogus()) {
          delete idClone;
          status = U_MEMORY_ALLOCATION_ERROR;
          break;
        }
        result.addElement(idClone, status);
        if (U_FAILURE(status)) {
          delete idClone;
          break;
        }
      }
      delete fallbackKey;
    }
  }
  if (U_FAILURE(status)) {
    result.removeAllElements();
  }
  return result;
}

const Hashtable* 
ICUService::getVisibleIDMap(UErrorCode& status) const {
  if (U_SUCCESS(status) && idCache == NULL) {
    ICUService* ncthis = (ICUService*)this; // cast away semantic const
    ncthis->idCache = new Hashtable();
    if (idCache == NULL) {
      status = U_MEMORY_ALLOCATION_ERROR;
    } else if (factories != NULL) {
      for (int32_t pos = factories->size(); --pos >= 0;) {
        Factory* f = (Factory*)factories->elementAt(pos);
        f->updateVisibleIDs(*idCache, status);
      }
      if (U_FAILURE(status)) {
        delete idCache;
        ncthis->idCache = NULL;
      }
    }
  }

  return idCache;
}

 
UnicodeString& 
ICUService::getDisplayName(const UnicodeString& id, UnicodeString& result) const 
{
	return getDisplayName(id, result, Locale::getDefault());
}

UnicodeString& 
ICUService::getDisplayName(const UnicodeString& id, UnicodeString& result, const Locale& locale) const 
{
  result.setToBogus();
  {
    ICUService* ncthis = (ICUService*)this; // cast away semantic const
    UErrorCode status = U_ZERO_ERROR;
    Mutex mutex(&ncthis->lock);
    const Hashtable* map = getVisibleIDMap(status);
    if (map != NULL) {
      Factory* f = (Factory*)map->get(id);
      if (f != NULL) {
        f->getDisplayName(id, locale, result);
      }
    }
  }
  return result;
}

UVector& 
ICUService::getDisplayNames(UVector& result, UErrorCode& status) const 
{
	return getDisplayNames(result, Locale::getDefault(), NULL, status);
}

UVector& 
ICUService::getDisplayNames(UVector& result, const Locale& locale, UErrorCode& status) const 
{
  return getDisplayNames(result, locale, NULL, status);
}

UVector& 
ICUService::getDisplayNames(UVector& result, 
                            const Locale& locale, 
                            const UnicodeString* matchID, 
                            UErrorCode& status) const 
{
  result.removeAllElements();
  if (U_SUCCESS(status)) {
    ICUService* ncthis = (ICUService*)this; // cast away semantic const
    Mutex mutex(&ncthis->lock);

    if (dnCache != NULL && dnCache->locale != locale) {
      delete dnCache;
      ncthis->dnCache = NULL;
    }

    if (dnCache == NULL) {
      const Hashtable* m = getVisibleIDMap(status);
      if (m != NULL) {
        ncthis->dnCache = new DNCache(locale); 
        if (dnCache == NULL) {
          status = U_MEMORY_ALLOCATION_ERROR;
          return result;
        }

        int32_t pos = 0;
        const UHashElement* entry = NULL;
        while (entry = m->nextElement(pos)) {
          const UnicodeString* id = (const UnicodeString*)entry->key.pointer;
          Factory* f = (Factory*)entry->value.pointer;
          UnicodeString name;
          f->getDisplayName(*id, locale, name);
          if (name.isBogus()) {
            status = U_MEMORY_ALLOCATION_ERROR;
          } else {
            dnCache->cache.put(name, (void*)id, status); // share pointer with visibleIDMap
            if (U_SUCCESS(status)) {
              continue;
            }
          }
          delete dnCache;
          ncthis->dnCache = NULL;
          return result;
        }
      }
    }
  }

  Key* matchKey = createKey(matchID);
  int32_t pos = 0;
  const UHashElement *entry = NULL;
  while (entry = dnCache->cache.nextElement(pos)) {
    const UnicodeString* id = (const UnicodeString*)entry->value.pointer;
    if (matchKey != NULL && !matchKey->isFallbackOf(*id)) {
      continue;
    }
    const UnicodeString* dn = (const UnicodeString*)entry->key.pointer;
    StringPair* sp = StringPair::create(*id, *dn, status);
    result.addElement(sp, status);
    if (U_FAILURE(status)) {
      result.removeAllElements();
      break;
    }
  }
  delete matchKey;

  return result;
}

const Factory* 
ICUService::registerObject(UObject* objToAdopt, const UnicodeString& id) 
{
  return registerObject(objToAdopt, id, TRUE);
}

const Factory* 
ICUService::registerObject(UObject* objToAdopt, const UnicodeString& id, UBool visible) 
{
  Key* key = createKey(&id);
  if (key != NULL) {
    UnicodeString canonicalID;
    key->canonicalID(canonicalID);
    delete key;

    Factory* f = createSimpleFactory(objToAdopt, canonicalID, visible);
    if (f != NULL) {
      return registerFactory(f);
    }
  }
  delete objToAdopt;
  return NULL;
}

Factory* 
ICUService::createSimpleFactory(UObject* objToAdopt, const UnicodeString& id, UBool visible)
{
  return new SimpleFactory(objToAdopt, id, visible);
}

const Factory* 
ICUService::registerFactory(Factory* factoryToAdopt) 
{
  if (factoryToAdopt != NULL) {
    Mutex mutex(&lock);

    UErrorCode status = U_ZERO_ERROR;
    if (factories == NULL) {
      factories = new UVector(deleteUObject, NULL, status);
      if (U_FAILURE(status)) {
        delete factories;
        return NULL;
      }
    }
    factories->insertElementAt(factoryToAdopt, 0, status);
    if (U_SUCCESS(status)) {
      clearCaches();
    } else {
      delete factoryToAdopt;
      factoryToAdopt = NULL;
    }
  }

  if (factoryToAdopt != NULL) {
      notifyChanged();
  }

  return factoryToAdopt;
}

UBool 
ICUService::unregisterFactory(Factory* factory) 
{
  UBool result = FALSE;
  if (factory != NULL && factories != NULL) {
    Mutex mutex(&lock);

    if (factories->removeElement(factory)) {
      clearCaches();
      result = TRUE;
    }
  }
  if (result) {
    notifyChanged();
  }
  return result;
}

void 
ICUService::reset() 
{
  {
    Mutex mutex(&lock);
    reInitializeFactories();
    clearCaches();
  }
  notifyChanged();
}

void 
ICUService::reInitializeFactories() 
{
  if (factories != NULL) {
    factories->removeAllElements();
  }
}

UBool 
ICUService::isDefault() const 
{
  return factories == NULL || factories->size() == 0;
}

Key* 
ICUService::createKey(const UnicodeString* id) const 
{
  return id == NULL ? NULL : new Key(*id);
}

void 
ICUService::clearCaches() 
{
  // callers synchronize before use
  ++timestamp;
  delete dnCache; dnCache = NULL;
  delete idCache; idCache = NULL;
  delete serviceCache; serviceCache = NULL;
}

void 
ICUService::clearServiceCache() 
{
  // callers synchronize before use
  delete serviceCache; serviceCache = NULL;
}

UBool 
ICUService::acceptsListener(const EventListener& l) const 
{
  return l.getDynamicClassID() == ServiceListener::getStaticClassID();
}

void 
ICUService::notifyListener(EventListener& l) const 
{
  ((ServiceListener&)l).serviceChanged(*this);
}

UnicodeString&
ICUService::getName(UnicodeString& result) const 
{
  return result.append(name);
}

int32_t 
ICUService::countFactories() const 
{
  return factories == NULL ? 0 : factories->size();
}

int32_t
ICUService::getTimestamp() const
{
  return timestamp;
}

U_NAMESPACE_END

/* UCONFIG_NO_SERVICE */
#endif
