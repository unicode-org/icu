/*
*******************************************************************************
* Copyright (C) 2013, International Business Machines Corporation and         
* others. All Rights Reserved.                                                
*******************************************************************************
*                                                                             
* File LRUCACHE.H                                                             
*******************************************************************************
*/

#ifndef __LRU_CACHE_H__
#define __LRU_CACHE_H__

#include "unicode/uobject.h"
#include "umutex.h"
#include "sharedptr.h"

struct UHashtable;

U_NAMESPACE_BEGIN

/**
 * LRUCache keyed by locale ID.
 */

class CacheEntry2;

class U_COMMON_API LRUCache : public UObject {
  public:
    template<typename T>
    void get(const char *localeId, SharedPtr<T> &ptr, UErrorCode &status) {
        SharedPtr<UObject> p;
        _get(localeId, p, status);
        if (U_FAILURE(status)) {
            return;
        }
        ptr = p;
    }
    UBool contains(const char *localeId) const;
    virtual ~LRUCache();
  protected:
    virtual UObject *create(const char *localeId, UErrorCode &status)=0;
    LRUCache(int32_t maxSize, UMutex *mutex, UErrorCode &status);
  private:
    LRUCache();
    LRUCache(const LRUCache &other);
    LRUCache &operator=(const LRUCache &other);
    UObject *safeCreate(const char *localeId, UErrorCode &status);
    CacheEntry2 *mostRecentlyUsedMarker;
    CacheEntry2 *leastRecentlyUsedMarker;
    UHashtable *localeIdToEntries;
    int32_t maxSize;
    UMutex *mutex;

    void moveToMostRecent(CacheEntry2 *cacheEntry);
    UBool init(const char *localeId, CacheEntry2 *cacheEntry);
    void _get(const char *localeId, SharedPtr<UObject> &ptr, UErrorCode &status);
};

typedef UObject *(*CreateFunc)(const char *localeId, UErrorCode &status);

class U_COMMON_API SimpleLRUCache : public LRUCache {
public:
    SimpleLRUCache(
        int32_t maxSize,
        UMutex *mutex,
        CreateFunc cf,
        UErrorCode &status) :
            LRUCache(maxSize, mutex, status), createFunc(cf) {
    }
    virtual ~SimpleLRUCache();
protected:
    virtual UObject *create(const char *localeId, UErrorCode &status);
private:
    CreateFunc createFunc;
};
    
U_NAMESPACE_END

#endif
