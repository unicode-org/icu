/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation.       *
 * All Rights Reserved.                                                        *
 *******************************************************************************
 */

#ifndef ICUSERV_H
#define ICUSERV_H

#include "unicode/utypes.h"

#if UCONFIG_NO_SERVICE

U_NAMESPACE_BEGIN

/*
 * Allow the declaration of APIs with pointers to ICUService
 * even when service is removed from the build.
 */
class ICUService;

U_NAMESPACE_END

#else

#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/locid.h"
#include "unicode/ubrk.h"

#include "hash.h"
#include "uvector.h"
#include "icunotif.h"

U_NAMESPACE_BEGIN

class Key;
class Factory;
class SimpleFactory;
class ServiceListener;
class ICUServiceEnumeration;
class ICUService;
class ICUServiceTest;

class DNCache;

/*
 ******************************************************************
 */

 /**
  * Keys are used to communicate with factories to generate an
  * instance of the service.  Keys define how ids are
  * canonicalized, provide both a current id and a current
  * descriptor to use in querying the cache and factories, and
  * determine the fallback strategy.</p>
  *
  * <p>Keys provide both a currentDescriptor and a currentID.
  * The descriptor contains an optional prefix, followed by '/'
  * and the currentID.  Factories that handle complex keys,
  * for example number format factories that generate multiple
  * kinds of formatters for the same locale, use the descriptor 
  * to provide a fully unique identifier for the service object, 
  * while using the currentID (in this case, the locale string),
  * as the visible IDs that can be localized.
  *
  * <p> The default implementation of Key has no fallbacks and
  * has no custom descriptors.</p>
  */
class U_COMMON_API Key : public UObject {
 private: 
  const UnicodeString _id;

 protected:
  static const UChar PREFIX_DELIMITER;

 public:

 /**
  * Construct a key from an id.
  */
  Key(const UnicodeString& id);

  /**
   * Virtual destructor.
   */
  virtual ~Key();

 /**
  * Return the original ID used to construct this key.
  */
  virtual const UnicodeString& getID() const;

 /**
  * Return the canonical version of the original ID.  This implementation
  * appends the original ID to result.  It returns result as a convenience.
  */
  virtual UnicodeString& canonicalID(UnicodeString& result) const;

 /**
  * Return the (canonical) current ID.  This implementation
  * appends the canonical ID to result.  It returns result as a convenience.
  */
  virtual UnicodeString& currentID(UnicodeString& result) const;

 /**
  * Return the current descriptor.  This implementation returns
  * the current ID in result, ignoring any previous contents.
  * Result is returned as a convenience.
  *
  * <p>The current descriptor is used to fully
  * identify an instance of the service in the cache.  A
  * factory may handle all descriptors for an ID, or just a
  * particular descriptor.  The factory can either parse the
  * descriptor or use custom API on the key in order to
  * instantiate the service. 
  */
  virtual UnicodeString& currentDescriptor(UnicodeString& result) const;

 /**
  * If the key has a fallback, modify the key and return true,
  * otherwise return false.  The current ID will change if there
  * is a fallback.  No currentIDs should be repeated, and fallback
  * must eventually return false.  This implmentation has no fallbacks
  * and always returns false.
  */
  virtual UBool fallback();

 /**
  * Return true if a key created from id matches, or would eventually
  * fallback to match, the canonical ID of this key.  
  */
  virtual UBool isFallbackOf(const UnicodeString& id) const;

 /**
  * Append the prefix to result
  */
  virtual UnicodeString& prefix(UnicodeString& result) const;

 /**
  * Parse the prefix out of a descriptor string.
  */
  static UnicodeString& parsePrefix(UnicodeString& result);

  /**
  * Parse the suffix out of a descriptor string.
  */
  static UnicodeString& parseSuffix(UnicodeString& result);

 public:
  /**
   * UObject boilerplate.
   */
  virtual UClassID getDynamicClassID() const {
    return getStaticClassID();
  }

  static UClassID getStaticClassID() { 
    return (UClassID)&fgClassID;
  }

 public:
  virtual UnicodeString& debug(UnicodeString& result) const;
  virtual UnicodeString& debugClass(UnicodeString& result) const;

 private:
    static const char fgClassID;
};

/*
 ******************************************************************
 */

 /**
  * Factories generate the service objects maintained by the
  * service.  A factory generates a service object from a key,
  * updates id->factory mappings, and returns the display name for
  * a supported id.  
  */
class U_COMMON_API Factory : public UObject {
 public:

    /**
     * Create a service object from the key, if this factory
     * supports the key.  Otherwise, return null.  
     *
     * <p>If the factory supports the key, then it can call
     * defaultCreate(const Key& key, const ICUService*)
     * the service's getKey(Key, String[], Factory) method
     * passing itself as the factory to get the object that
     * the service would have created prior to the factory's
     * registration with the service.  This can change the
     * key, so any information required from the key should
     * be extracted before making such a callback.
     */
    virtual UObject* create(const Key& key, const ICUService* service, UErrorCode& status) const = 0;

    /**
     * Update the result IDs (not descriptors) to reflect the IDs
     * this factory handles.  This function and getDisplayName are
     * used to support ICUService.getDisplayNames.  Basically, the
     * factory has to determine which IDs it will permit to be
     * available, and of those, which it will provide localized
     * display names for.  In most cases this reflects the IDs that
     * the factory directly supports.
     */
    virtual void updateVisibleIDs(Hashtable& result, UErrorCode& status) const = 0;

    /**
     * Return the display name for this id in the provided locale.
     * This is an localized id, not a descriptor.  If the id is 
     * not visible or not defined by the factory, return null.
     * If locale is null, return id unchanged.
     */
    virtual UnicodeString& getDisplayName(const UnicodeString& id, const Locale& locale, UnicodeString& result) const = 0;
};

/*
 ******************************************************************
 */

 /**
  * A default implementation of factory.  This provides default
  * implementations for subclasses, and implements a singleton
  * factory that matches a single id  and returns a single
  * (possibly deferred-initialized) instance.  This implements
  * updateVisibleIDs to add a mapping from its ID to itself
  * if visible is true, or to remove any existing mapping
  * for its ID if visible is false.
  */
class U_COMMON_API SimpleFactory : public Factory {
 protected:
  UObject* _instance;
  const UnicodeString _id;
  const UBool _visible;

 public:
  /**
   * Convenience constructor that calls SimpleFactory(Object, String, boolean)
   * with visible true.
   */
  SimpleFactory(UObject* instanceToAdopt, const UnicodeString& id);

  /**
   * Construct a simple factory that maps a single id to a single 
   * service instance.  If visible is true, the id will be visible.
   * Neither the instance nor the id can be null.
   */
  SimpleFactory(UObject* instanceToAdopt, const UnicodeString& id, UBool visible);

  /**
   * Virtual destructor.
   */
  virtual ~SimpleFactory();

  /**
   * Return the service instance if the factory's id is equal to
   * the key's currentID.  Service is ignored.
   */
  UObject* create(const Key& key, const ICUService* service, UErrorCode& status) const;

  /**
   * If visible, adds a mapping from id -> this to the result, 
   * otherwise removes id from result.
   */
  void updateVisibleIDs(Hashtable& result, UErrorCode& status) const;

  /**
   * If this.id equals id, returns id regardless of locale,
   * otherwise returns the empty string.  (This default implementation has
   * no localized id information.)  
   */
  UnicodeString& getDisplayName(const UnicodeString& id, const Locale& locale, UnicodeString& result) const;

 public:

 /**
  * UObject boilerplate.
  */
  virtual UClassID getDynamicClassID() const {
	  return getStaticClassID();
  }

  static UClassID getStaticClassID() { 
	  return (UClassID)&fgClassID;
  }

 public:

  /**
   * For debugging.
   */
  virtual UnicodeString& debug(UnicodeString& toAppendTo) const;
  virtual UnicodeString& debugClass(UnicodeString& toAppendTo) const;

 private:
  static const char fgClassID;
};

/*
 ******************************************************************
 */

/**
 * ServiceListener is the listener that ICUService provides by default.
 * ICUService will notifiy this listener when factories are added to
 * or removed from the service.  Subclasses can provide
 * different listener interfaces that extend EventListener, and modify
 * acceptsListener and notifyListener as appropriate.
 */
class U_COMMON_API ServiceListener : public EventListener {
 public:
  virtual UClassID getDynamicClassID() const {
    return getStaticClassID();
  }

  static UClassID getStaticClassID() {
    return (UClassID)&fgClassID;
  }

  virtual void serviceChanged(const ICUService& service) const = 0;

 private:
  static const char fgClassID;
};

/*
 ******************************************************************
 */

class U_COMMON_API StringPair : public UMemory {
public:
  const UnicodeString displayName;
  const UnicodeString id;

  static StringPair* create(const UnicodeString& displayName, 
                            const UnicodeString& id,
                            UErrorCode& status);

  UBool isBogus() const;

private:
  StringPair(const UnicodeString& displayName, const UnicodeString& id);
};

/**
 * Deleter for StringPairs
 */
U_CAPI void U_EXPORT2
deleteStringPair(void *obj);

/*
 ******************************************************************
 */

 /**
 * <p>A Service provides access to service objects that implement a
 * particular service, e.g. transliterators.  Users provide a String
 * id (for example, a locale string) to the service, and get back an
 * object for that id.  Service objects can be any kind of object.
 * The service object is cached and returned for later queries, so
 * generally it should not be mutable, or the caller should clone the
 * object before modifying it.</p>
 *
 * <p>Services 'canonicalize' the query id and use the canonical id to
 * query for the service.  The service also defines a mechanism to
 * 'fallback' the id multiple times.  Clients can optionally request
 * the actual id that was matched by a query when they use an id to
 * retrieve a service object.</p>
 *
 * <p>Service objects are instantiated by Factory objects registered with 
 * the service.  The service queries each Factory in turn, from most recently
 * registered to earliest registered, until one returns a service object.
 * If none responds with a service object, a fallback id is generated,
 * and the process repeats until a service object is returned or until
 * the id has no further fallbacks.</p>
 *
 * <p>Factories can be dynamically registered and unregistered with the
 * service.  When registered, a Factory is installed at the head of
 * the factory list, and so gets 'first crack' at any keys or fallback
 * keys.  When unregistered, it is removed from the service and can no
 * longer be located through it.  Service objects generated by this
 * factory and held by the client are unaffected.</p>
 *
 * <p>ICUService uses Keys to query factories and perform
 * fallback.  The Key defines the canonical form of the id, and
 * implements the fallback strategy.  Custom Keys can be defined that
 * parse complex IDs into components that Factories can more easily
 * use.  The Key can cache the results of this parsing to save
 * repeated effort.  ICUService provides convenience APIs that
 * take Strings and generate default Keys for use in querying.</p>
 *
 * <p>ICUService provides API to get the list of ids publicly
 * supported by the service (although queries aren't restricted to
 * this list).  This list contains only 'simple' IDs, and not fully
 * unique ids.  Factories are associated with each simple ID and
 * the responsible factory can also return a human-readable localized
 * version of the simple ID, for use in user interfaces.  ICUService
 * can also provide a sorted collection of the all the localized visible 
 * ids.</p>
 *
 * <p>ICUService implements ICUNotifier, so that clients can register
 * to receive notification when factories are added or removed from 
 * the service.  ICUService provides a default EventListener subinterface,
 * ServiceListener, which can be registered with the service.  When
 * the service changes, the ServiceListener's serviceChanged method
 * is called, with the service as the only argument.</p>
 *
 * <p>The ICUService API is both rich and generic, and it is expected
 * that most implementations will statically 'wrap' ICUService to
 * present a more appropriate API-- for example, to declare the type
 * of the objects returned from get, to limit the factories that can
 * be registered with the service, or to define their own listener
 * interface with a custom callback method.  They might also customize
 * ICUService by overriding it, for example, to customize the Key and
 * fallback strategy.  ICULocaleService is a customized service that
 * uses Locale names as ids and uses Keys that implement the standard
 * resource bundle fallback strategy.<p> 
 */
class U_COMMON_API ICUService : public ICUNotifier {
 protected: 
    /**
     * Name useful for debugging.
     */
    const UnicodeString name;

    /**
     * single lock used by this service.
     */
    UMTX lock;

 private:
    /**
     * Timestamp so iterators can be fail-fast.
     */
    uint32_t timestamp;

    /**
     * All the factories registered with this service.
     */
    UVector* factories;

    /**
     * The service cache.
     */
    Hashtable* serviceCache;

    /**
     * The id cache.
     */
    Hashtable* idCache;

    /**
     * The name cache.
     */
    DNCache* dnCache;

    /**
     * Constructor.
     */
 public:
    ICUService();

    /**
     * Construct with a name (useful for debugging).
     */
    ICUService(const UnicodeString& name);

    /**
     * Destructor.
     */
    virtual ~ICUService();

    /**
     * Return the name of this service. This will be the empty string if none was assigned.
     */
    UnicodeString& getName(UnicodeString& result) const;

    /**
     * Convenience override for get(Key&, UnicodeString*). This uses
     * createKey to create a key for the provided descriptor.
     */
    UObject* get(const UnicodeString& descriptor, UErrorCode& status) const;

    /**
     * Convenience override for get(Key&, UnicodeString*).  This uses
     * createKey to create a key from the provided descriptor.
     */
    UObject* get(const UnicodeString& descriptor, UnicodeString* actualReturn, UErrorCode& status) const;

    /**
     * Convenience override for get(Key&, UnicodeString*).
     */
    UObject* getKey(Key& key, UErrorCode& status) const;

    /**
     * <p>Given a key, return a service object, and, if actualReturn
     * is not null, the descriptor with which it was found in the
     * first element of actualReturn.  If no service object matches
     * this key, return null, and leave actualReturn unchanged.</p>
     *
     * <p>This queries the cache using the key's descriptor, and if no
     * object in the cache matches it, tries the key on each
     * registered factory, in order.  If none generates a service
     * object for the key, repeats the process with each fallback of
     * the key, until either one returns a service object, or the key
     * has no fallback.</p> 
     *
     * <p>If key is null, just returns null.</p>
     */
    UObject* getKey(Key& key, UnicodeString* actualReturn, UErrorCode& status) const;

    /**
     * <p>Given a key, return a service object, and, if actualReturn
     * is not null, the descriptor with which it was found in the
     * first element of actualReturn.  If no service object matches
     * this key, return null, and leave actualReturn unchanged.</p>
     */
    UObject* getKey(Key& key, UnicodeString* actualReturn, const Factory* factory, UErrorCode& status) const;

    /**
     * Convenience override for getVisibleIDs(String) that passes null
     * as the fallback, thus returning all visible IDs.
     */
    UVector& getVisibleIDs(UVector& result, UErrorCode& status) const;

    /**
     * <p>Return a snapshot of the visible IDs for this service.  This
     * set will not change as Factories are added or removed, but the
     * supported ids will, so there is no guarantee that all and only
     * the ids in the returned set are visible and supported by the
     * service in subsequent calls.</p>
     *
     * <p>matchID is passed to createKey to create a key.  If the
     * key is not null, it is used to filter out ids that don't have
     * the key as a fallback.
     *
     * <p>The IDs are returned as pointers to UnicodeStrings.  The
     * caller owns the IDs.
     */
    UVector& getVisibleIDs(UVector& result, const UnicodeString* matchID, UErrorCode& status) const;

    /**
     * Convenience override for getDisplayName(String, Locale) that
     * uses the current default locale.
     */
    UnicodeString& getDisplayName(const UnicodeString& id, UnicodeString& result) const;

    /**
     * Given a visible id, return the display name in the requested locale.
     * If there is no directly supported id corresponding to this id, set
     * result to bogus.
     */
    UnicodeString& getDisplayName(const UnicodeString& id, UnicodeString& result, const Locale& locale) const;

    /**
     * Convenience override of getDisplayNames(Locale, String) that 
     * uses the current default Locale as the locale and NULL for
     * the matchID.
     */
    UVector& getDisplayNames(UVector& result, UErrorCode& status) const;

    /**
     * Convenience override of getDisplayNames(Locale, String) that
     */
    UVector& getDisplayNames(UVector& result, const Locale& locale, UErrorCode& status) const;

    /**
     * Return a snapshot of the mapping from display names to visible
     * IDs for this service.  This set will not change as factories
     * are added or removed, but the supported ids will, so there is
     * no guarantee that all and only the ids in the returned map will
     * be visible and supported by the service in subsequent calls,
     * nor is there any guarantee that the current display names match
     * those in the set.  This iterates over StringPair instances.
     */
    UVector& getDisplayNames(UVector& result,
                             const Locale& locale, 
                             const UnicodeString* matchID, 
                             UErrorCode& status) const;

    /**
     * A convenience override of registerObject(Object, String, boolean)
     * that defaults visible to true.  The service adopts the object.
     */
    const Factory* registerObject(UObject* objToAdopt, const UnicodeString& id);

    /**
     * Register an object with the provided id.  The id will be 
     * canonicalized.  The canonicalized ID will be returned by
     * getVisibleIDs if visible is true.  This wraps the object
     * using createSimpleFactory and calls registerFactory.
     */
    virtual const Factory* registerObject(UObject* obj, const UnicodeString& id, UBool visible);

    /**
     * Register a Factory.  Returns the factory if the service accepts
     * the factory, otherwise returns null.  The default implementation
     * accepts all factories.  The service owns the factories. 
     */
    virtual const Factory* registerFactory(Factory* factoryToAdopt);

    /**
     * Unregister a factory.  The first matching registered factory will
     * be deleted and removed from the list.  Returns true if a matching factory was
     * found.  If not found, factory is not deleted, but this is typically an
     * error.
     */
    virtual UBool unregisterFactory(Factory* factory);

    /**
     * Reset the service to the default factories.  The factory
     * lock is acquired and then reInitializeFactories is called.
     */
    virtual void reset();

    /**
     * Return true if the service is in its default state.  The default
     * implementation returns true if there are no factories registered.
     */
    virtual UBool isDefault() const;

    /**
     * Create a key from an id.  This creates a Key instance.
     * Subclasses can override to define more useful keys appropriate
     * to the factories they accept.  If id is null, returns null.
     */
    virtual Key* createKey(const UnicodeString* id) const;

    /**
     * Clone object so that caller can own the copy.  UObject doesn't yet implement
     * clone so we need an instance-aware method that knows how to do this.
     * This is public so factories can call it, but should really be protected.
     */
    virtual UObject* cloneInstance(UObject* instance) const = 0;

 protected:

    /**
     * Create a factory that wraps a singleton object.
     * Default is an instance of SimpleFactory.
     */
    virtual Factory* createSimpleFactory(UObject* objToAdopt, const UnicodeString& id, UBool visible);

    /**
     * Reinitialize the factory list to its default state.  By default
     * this clears the list.  Subclasses can override to provide other
     * default initialization of the factory list.  Subclasses must
     * not call this method directly, as it must only be called while
     * holding write access to the factory list.  
     */
    virtual void reInitializeFactories();

    /**
     * Default handler for this service if no factory in the list
     * handled the key.
     */
    virtual UObject* handleDefault(const Key& key, UnicodeString* actualIDReturn, UErrorCode& status) const;

    /**
     * Clear caches maintained by this service.  Subclasses can
     * override if they implement additional that need to be cleared
     * when the service changes. Subclasses should generally not call
     * this method directly, as it must only be called while
     * synchronized on this.
     */
    virtual void clearCaches();

    /**
     * Clears only the service cache.
     * This can be called by subclasses when a change affects the service
     * cache but not the id caches, e.g., when the default locale changes
     * the resolution of ids changes, but not the visible ids themselves.
     */
    void clearServiceCache();

    /**
     * Return true if the listener is accepted; by default this
     * requires a ServiceListener.  Subclasses can override to accept
     * different listeners.  
     */
    virtual UBool acceptsListener(const EventListener& l) const;

    /**
     * Notify the listener, which by default is a ServiceListener.
     * Subclasses can override to use a different listener.  
     */
    virtual void notifyListener(EventListener& l) const;

    /**
     * Return a map from visible ids to factories.  
     * This should be only be called when the mutex is held.
     */
    const Hashtable* getVisibleIDMap(UErrorCode& status) const;

    /**
     * Allow subclasses to read the time stamp.
     */
    virtual int32_t getTimestamp() const;

 private:

    friend class ICUServiceTest;
    /**
     * Return the number of factories, used for testing.
     */
    int32_t countFactories() const;
};

U_NAMESPACE_END

    /* UCONFIG_NO_SERVICE */
#endif

    /* ICUSERV_H */
#endif

