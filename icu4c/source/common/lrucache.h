/*
******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and         
* others. All Rights Reserved.                                                
******************************************************************************
*                                                                             
* File LRUCACHE.H                                                             
******************************************************************************
*/

#ifndef __LRU_CACHE_H__
#define __LRU_CACHE_H__

#include "unicode/uobject.h"
#include "sharedobject.h"

struct UHashtable;

U_NAMESPACE_BEGIN

/**
 * A cache of SharedObjects keyed by locale ID.
 *
 * LRUCache has one main method, get(), which fetches a SharedObject by
 * locale ID. If no such SharedObject is cached, get() creates the new
 * SharedObject and caches it behind the scenes.
 *
 * Each LRUCache has a maximum size. Whenever adding a new item to the cache
 * would exceed this maximum size, LRUCache evicts the SharedObject that was
 * least recently fetched via the get() method.
 *
 * LRUCache is designed to be subclassed. Subclasses must override the create()
 * method to create a new SharedObject by localeId. If only locale ID is
 * needed to create the SharedObject, a client can use SimpleLRUCache.
 */
class U_COMMON_API LRUCache : public UObject {
public:
    /**
     * Fetches a SharedObject by locale ID. On success, get() makes ptr point
     * to the fetched SharedObject while automatically updating reference
     * counts; on failure, get() leaves ptr unchanged and sets status.
     * When get() is called, ptr must either be NULL or be included in the
     * reference count of what it points to. After get() returns successfully,
     * caller must eventually call removeRef() on ptr to avoid memory leaks.
     *
     * T must be a subclass of SharedObject.
     */ 
    template<typename T>
    void get(const char *localeId, const T *&ptr, UErrorCode &status) {
        const T *value = (const T *) _get(localeId, status);
        if (U_FAILURE(status)) {
            return;
        }
        SharedObject::copyPtr(value, ptr);
    }
    /**
     * Returns TRUE if a SharedObject for given ID is cached. Used
     * primarily for testing purposes.
     */
    UBool contains(const char *localeId) const;
    virtual ~LRUCache();
protected:
    /**
     * Subclasses override to create a new SharedObject for given localeID.
     * get() calls this to resolve cache misses. create() must either return
     * a SharedObject with 0 reference count and no error in status or return
     * NULL and set an error in status.
     */
    virtual SharedObject *create(const char *localeId, UErrorCode &status)=0;

    /**
     * Constructor.
     * @param maxSize the maximum size of the LRUCache
     * @param status any error is set here.
     */
    LRUCache(int32_t maxSize, UErrorCode &status);
private:
    class CacheEntry : public UMemory {
    public:
        CacheEntry *moreRecent;
        CacheEntry *lessRecent;
        char *localeId;
        const SharedObject *cachedData;
        UErrorCode status;  // This is the error if any from creating
                            // cachedData.
        CacheEntry();
        ~CacheEntry();

        void unlink();
        void reset();
        void init(
               char *adoptedLocId, SharedObject *dataToAdopt, UErrorCode err);
    private:
        CacheEntry(const CacheEntry& other);
        CacheEntry &operator=(const CacheEntry& other);
    };
    LRUCache();
    LRUCache(const LRUCache &other);
    LRUCache &operator=(const LRUCache &other);

    // TODO (Travis Keep): Consider replacing both of these end nodes with a
    // single sentinel.
    CacheEntry *mostRecentlyUsedMarker;
    CacheEntry *leastRecentlyUsedMarker;
    UHashtable *localeIdToEntries;
    int32_t maxSize;

    void moveToMostRecent(CacheEntry *cacheEntry);
    void init(char *localeId, CacheEntry *cacheEntry);
    const SharedObject *_get(const char *localeId, UErrorCode &status);
};

/**
 * A function type that creates a SharedObject from a locale ID. Functions of
 * this type must return a SharedObject with 0 reference count and no error in
 * status or return NULL and set an error in status.
 */
typedef SharedObject *CreateFunc(const char *localeId, UErrorCode &status);

/**
 * A concrete subclass of LRUCache that creates SharedObjects using a
 * function of type CreateFunc.
 */
class U_COMMON_API SimpleLRUCache : public LRUCache {
public:
    /**
     * Constructor.
     * @param maxSize the maximum cache size.
     * @param cf creates SharedObject on cache miss.
     * @param status error reported here.
     */
    SimpleLRUCache(
        int32_t maxSize,
        CreateFunc cf,
        UErrorCode &status) :
            LRUCache(maxSize, status), createFunc(cf) {
    }
    virtual ~SimpleLRUCache();
protected:
    virtual SharedObject *create(const char *localeId, UErrorCode &status);
private:
    CreateFunc *createFunc;
};
    
U_NAMESPACE_END

#endif
