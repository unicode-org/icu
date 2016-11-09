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
 * LRUCache keyed by locale ID.
 */

class U_COMMON_API LRUCache : public UObject {
public:
    template<typename T>
    void get(const char *localeId, const T *&ptr, UErrorCode &status) {
        const T *value = (const T *) _get(localeId, status);
        if (U_FAILURE(status)) {
            return;
        }
        SharedObject::copyPtr(value, ptr);
    }
    UBool contains(const char *localeId) const;
    virtual ~LRUCache();
protected:
    virtual SharedObject *create(const char *localeId, UErrorCode &status)=0;
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

typedef SharedObject *CreateFunc(const char *localeId, UErrorCode &status);

class U_COMMON_API SimpleLRUCache : public LRUCache {
public:
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
