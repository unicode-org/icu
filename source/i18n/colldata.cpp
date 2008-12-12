/*
 ******************************************************************************
 *   Copyright (C) 1996-2008, International Business Machines                 *
 *   Corporation and others.  All Rights Reserved.                            *
 ******************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/unistr.h"
#include "unicode/putil.h"
#include "unicode/usearch.h"

#include "cmemory.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/ucoleitr.h"

#include "unicode/regex.h"        // TODO: make conditional on regexp being built.

#include "unicode/uniset.h"
#include "unicode/uset.h"
#include "unicode/ustring.h"
#include "hash.h"
#include "uhash.h"
#include "ucln_in.h"
#include "ucol_imp.h"
#include "umutex.h"

#include "unicode/colldata.h"

U_NAMESPACE_BEGIN

#define NEW_ARRAY(type, count) (type *) uprv_malloc((count) * sizeof(type))
#define DELETE_ARRAY(array) uprv_free((void *) (array))
#define ARRAY_COPY(dst, src, count) uprv_memcpy((void *) (dst), (void *) (src), (count) * sizeof (src)[0])

static inline USet *uset_openEmpty()
{
    return uset_open(1, 0);
}

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(CEList)

#ifdef INSTRUMENT_CELIST
int32_t CEList::_active = 0;
int32_t CEList::_histogram[10] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
#endif

CEList::CEList(UCollator *coll, const UnicodeString &string)
    : ces(NULL), listMax(CELIST_BUFFER_SIZE), listSize(0)
{
    UErrorCode status = U_ZERO_ERROR;
    UCollationElements *elems = ucol_openElements(coll, string.getBuffer(), string.length(), &status);
    uint32_t strengthMask = 0;
    int32_t order;

    switch (ucol_getStrength(coll)) 
    {
    default:
        strengthMask |= UCOL_TERTIARYORDERMASK;
        /* fall through */

    case UCOL_SECONDARY:
        strengthMask |= UCOL_SECONDARYORDERMASK;
        /* fall through */

    case UCOL_PRIMARY:
        strengthMask |= UCOL_PRIMARYORDERMASK;
    }

#ifdef INSTRUMENT_CELIST
    _active += 1;
    _histogram[0] += 1;
#endif

    ces = ceBuffer;

    while ((order = ucol_next(elems, &status)) != UCOL_NULLORDER) {
        order &= strengthMask;

        if (order == UCOL_IGNORABLE) {
            continue;
        }

        add(order);
    }

    ucol_closeElements(elems);
}

CEList::~CEList()
{
#ifdef INSTRUMENT_CELIST
    _active -= 1;
#endif

    if (ces != ceBuffer) {
        DELETE_ARRAY(ces);
    }
}

void CEList::add(int32_t ce)
{
    if (listSize >= listMax) {
      //listMax *= 2;
        listMax += CELIST_BUFFER_SIZE;

#ifdef INSTRUMENT_CELIST
        _histogram[listSize / CELIST_BUFFER_SIZE] += 1;
#endif

        int32_t *newCEs = NEW_ARRAY(int32_t, listMax);

        uprv_memcpy(newCEs, ces, listSize * sizeof(int32_t));

        if (ces != ceBuffer) {
            DELETE_ARRAY(ces);
        }

        ces = newCEs;
    }

    ces[listSize++] = ce;
}

int32_t CEList::get(int32_t index) const
{
    if (index >= 0 && index < listSize) {
        return ces[index];
    }

    return -1;
}

int32_t &CEList::operator[](int32_t index) const
{
    return ces[index];
}

UBool CEList::matchesAt(int32_t offset, const CEList *other) const
{
    if (listSize - offset < other->size()) {
        return FALSE;
    }

    for (int32_t i = offset, j = 0; j < other->size(); i += 1, j += 1) {
        if (ces[i] != other->get(j)) {
            return FALSE;
        }
    }

    return TRUE;
}

int32_t CEList::size() const
{
    return listSize;
}

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(StringList)

#ifdef INSTRUMENT_STRING_LIST
int32_t StringList::_lists = 0;
int32_t StringList::_strings = 0;
int32_t StringList::_histogram[101] = {0};
#endif

StringList::StringList()
    : strings(NULL), listMax(STRING_LIST_BUFFER_SIZE), listSize(0)
{
    strings = new UnicodeString [listMax];

#ifdef INSTRUMENT_STRING_LIST
    _lists += 1;
    _histogram[0] += 1;
#endif
}

StringList::~StringList()
{
    delete[] strings;
}

void StringList::add(const UnicodeString *string)
{
#ifdef INSTRUMENT_STRING_LIST
    _strings += 1;
#endif

    if (listSize >= listMax) {
        listMax += STRING_LIST_BUFFER_SIZE;

        UnicodeString *newStrings = new UnicodeString[listMax];

        uprv_memcpy(newStrings, strings, listSize * sizeof(UnicodeString));

#ifdef INSTRUMENT_STRING_LIST
        int32_t _h = listSize / STRING_LIST_BUFFER_SIZE;

        if (_h > 100) {
            _h = 100;
        }

        _histogram[_h] += 1;
#endif

        delete[] strings;
        strings = newStrings;
    }

    // The ctor initialized all the strings in
    // the array to empty strings, so this
    // is the same as copying the source string.
    strings[listSize++].append(*string);
}

void StringList::add(const UChar *chars, int32_t count)
{
    const UnicodeString string(chars, count);

    add(&string);
}

const UnicodeString *StringList::get(int32_t index) const
{
    if (index >= 0 && index < listSize) {
        return &strings[index];
    }

    return NULL;
}

int32_t StringList::size() const
{
    return listSize;
}


U_CFUNC void deleteStringList(void *obj);

class CEToStringsMap : public UMemory
{
public:

    CEToStringsMap();
    ~CEToStringsMap();

    void put(int32_t ce, UnicodeString *string);
    StringList *getStringList(int32_t ce) const;

private:
 
    void putStringList(int32_t ce, StringList *stringList);
    UHashtable *map;
};

CEToStringsMap::CEToStringsMap()
{
    UErrorCode status = U_ZERO_ERROR;

    map = uhash_open(uhash_hashLong, uhash_compareLong,
                     uhash_compareCaselessUnicodeString,
                     &status);

    uhash_setValueDeleter(map, deleteStringList);
}

CEToStringsMap::~CEToStringsMap()
{
    uhash_close(map);
}

void CEToStringsMap::put(int32_t ce, UnicodeString *string)
{
    StringList *strings = getStringList(ce);

    if (strings == NULL) {
        strings = new StringList();
        putStringList(ce, strings);
    }

    strings->add(string);
}

StringList *CEToStringsMap::getStringList(int32_t ce) const
{
    return (StringList *) uhash_iget(map, ce);
}

void CEToStringsMap::putStringList(int32_t ce, StringList *stringList)
{
    UErrorCode status = U_ZERO_ERROR;

    uhash_iput(map, ce, (void *) stringList, &status);
}

U_CFUNC void deleteStringList(void *obj)
{
    StringList *strings = (StringList *) obj;

    delete strings;
}

U_CFUNC void deleteCEList(void *obj);
U_CFUNC void deleteUnicodeStringKey(void *obj);

class StringToCEsMap : public UMemory
{
public:
    StringToCEsMap();
    ~StringToCEsMap();

    void put(const UnicodeString *string, const CEList *ces);
    const CEList *get(const UnicodeString *string);
    void free(const CEList *list);

private:


    UHashtable *map;
};

StringToCEsMap::StringToCEsMap()
{
    UErrorCode status = U_ZERO_ERROR;

    map = uhash_open(uhash_hashUnicodeString,
                     uhash_compareUnicodeString,
                     uhash_compareLong,
                     &status);

    uhash_setValueDeleter(map, deleteCEList);
    uhash_setKeyDeleter(map, deleteUnicodeStringKey);
}

StringToCEsMap::~StringToCEsMap()
{
    uhash_close(map);
}

void StringToCEsMap::put(const UnicodeString *string, const CEList *ces)
{
    UErrorCode status = U_ZERO_ERROR;

    uhash_put(map, (void *) string, (void *) ces, &status);
}

const CEList *StringToCEsMap::get(const UnicodeString *string)
{
    return (const CEList *) uhash_get(map, string);
}

U_CFUNC void deleteCEList(void *obj)
{
    CEList *list = (CEList *) obj;

    delete list;
}

U_CFUNC void deleteUnicodeStringKey(void *obj)
{
    UnicodeString *key = (UnicodeString *) obj;

    delete key;
}

class CollDataCacheEntry : public UMemory
{
public:
    CollDataCacheEntry(CollData *theData);
    ~CollDataCacheEntry();

    CollData *data;
    int32_t   refCount;
};

CollDataCacheEntry::CollDataCacheEntry(CollData *theData)
    : data(theData), refCount(1)
{
    // nothing else to do
}

CollDataCacheEntry::~CollDataCacheEntry()
{
    // check refCount?
    delete data;
}

class CollDataCache : public UMemory
{
public:
    CollDataCache();
    ~CollDataCache();

    CollData *get(UCollator *collator);
    void unref(CollData *collData);

private:
    static char *getKey(UCollator *collator, char *keyBuffer, int32_t *charBufferLength);
    static void deleteKey(char *key);

    UMTX lock;
    UHashtable *cache;
};

U_CFUNC void deleteChars(void *obj)
{
    char *chars = (char *) obj;

    // All the key strings are owned by the 
    // CollData objects and don't need to
    // be freed here.
  //DELETE_ARRAY(chars);
}

U_CFUNC void deleteCollDataCacheEntry(void *obj)
{
    CollDataCacheEntry *entry = (CollDataCacheEntry *) obj;

    delete entry;
}

CollDataCache::CollDataCache()
    : lock(0), cache(NULL)
{
    UErrorCode status = U_ZERO_ERROR;

    umtx_init(&lock);

    cache = uhash_open(uhash_hashChars, uhash_compareChars, uhash_compareLong, &status);

    uhash_setValueDeleter(cache, deleteCollDataCacheEntry);
    uhash_setKeyDeleter(cache, deleteChars);
}

CollDataCache::~CollDataCache()
{
    umtx_lock(&lock);
    uhash_close(cache);
    cache = NULL;
    umtx_unlock(&lock);

    umtx_destroy(&lock);
}

CollData *CollDataCache::get(UCollator *collator)
{
    UErrorCode status = U_ZERO_ERROR;
    char keyBuffer[KEY_BUFFER_SIZE];
    int32_t keyLength = KEY_BUFFER_SIZE;
    char *key = getKey(collator, keyBuffer, &keyLength);
    CollData *result = NULL, *newData = NULL;
    CollDataCacheEntry *entry = NULL, *newEntry = NULL;

    umtx_lock(&lock);
    entry = (CollDataCacheEntry *) uhash_get(cache, key);

    if (entry == NULL) {
        umtx_unlock(&lock);

        newData = new CollData(collator, key, keyLength);
        newEntry = new CollDataCacheEntry(newData);

        umtx_lock(&lock);
        entry = (CollDataCacheEntry *) uhash_get(cache, key);

        if (entry == NULL) {
            uhash_put(cache, newData->key, newEntry, &status);
            umtx_unlock(&lock);

            return newData;
        }
    }

    result = entry->data;
    entry->refCount += 1;
    umtx_unlock(&lock);

    if (key != keyBuffer) {
        deleteKey(key);
    }

    if (newEntry != NULL) {
        delete newEntry;
        delete newData;
    }

    return result;
}

void CollDataCache::unref(CollData *collData)
{
    CollDataCacheEntry *entry = NULL;
    
    umtx_lock(&lock);
    entry = (CollDataCacheEntry *) uhash_get(cache, collData->key);

    if (entry != NULL) {
        entry->refCount -= 1;
    }
    umtx_unlock(&lock);
}

char *CollDataCache::getKey(UCollator *collator, char *keyBuffer, int32_t *keyBufferLength)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t len = ucol_getShortDefinitionString(collator, NULL, keyBuffer, *keyBufferLength, &status);

    if (len >= *keyBufferLength) {
        *keyBufferLength = (len + 2) & ~1;  // round to even length, leaving room for terminating null
        keyBuffer = NEW_ARRAY(char, *keyBufferLength);
        status = U_ZERO_ERROR;

        len = ucol_getShortDefinitionString(collator, NULL, keyBuffer, *keyBufferLength, &status);
    }

    keyBuffer[len] = '\0';

    return keyBuffer;
}

void CollDataCache::deleteKey(char *key)
{
    DELETE_ARRAY(key);
}

U_CDECL_BEGIN
static UBool coll_data_cleanup(void) {
    CollData::freeCollDataCache();
  return TRUE;
}
U_CDECL_END

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(CollData)

CollData::CollData()
{
    // nothing
}

#define CLONE_COLLATOR
//#define CACHE_CELISTS
CollData::CollData(UCollator *collator, char *cacheKey, int32_t cacheKeyLength)
{
    UErrorCode status = U_ZERO_ERROR;

#if 0
    U_STRING_DECL(test_pattern, "[[:assigned:]-[:ideographic:]-[:hangul:]-[:c:]]", 47);
    U_STRING_INIT(test_pattern, "[[:assigned:]-[:ideographic:]-[:hangul:]-[:c:]]", 47);
    USet *charsToTest  = uset_openPattern(test_pattern, 47, &status);
#else
    U_STRING_DECL(test_pattern, "[[:assigned:]-[:c:]]", 20);
    U_STRING_INIT(test_pattern, "[[:assigned:]-[:c:]]", 20);
    USet *charsToTest  = uset_openPattern(test_pattern, 20, &status);
#endif

    USet *expansions   = uset_openEmpty();
    USet *contractions = uset_openEmpty();
    int32_t itemCount;

#ifdef CACHE_CELISTS
    charsToCEList = new StringToCEsMap();
#else
    charsToCEList = NULL;
#endif

    ceToCharsStartingWith = new CEToStringsMap();
    
    if (cacheKeyLength > KEY_BUFFER_SIZE) {
        key = NEW_ARRAY(char, cacheKeyLength);
    } else {
        key = keyBuffer;
    }

    ARRAY_COPY(key, cacheKey, cacheKeyLength);

#ifdef CLONE_COLLATOR
    coll = ucol_safeClone(collator, NULL, NULL, &status);
#else
    coll = collator;
#endif

    ucol_getContractionsAndExpansions(coll, contractions, expansions, FALSE, &status);

    uset_addAll(charsToTest, contractions);
    uset_addAll(charsToTest, expansions);

    itemCount = uset_getItemCount(charsToTest);
    for(int32_t item = 0; item < itemCount; item += 1) {
        UChar32 start = 0, end = 0;
        UChar buffer[16];
        int32_t len = uset_getItem(charsToTest, item, &start, &end,
                                   buffer, 16, &status);

        if (len == 0) {
            for (UChar32 ch = start; ch <= end; ch += 1) {
                UnicodeString *st = new UnicodeString(ch);
                CEList *ceList = new CEList(coll, *st);

                ceToCharsStartingWith->put(ceList->get(0), st);

#ifdef CACHE_CELISTS
                charsToCEList->put(st, ceList);
#else
                delete ceList;
#endif
            }
        } else if (len > 0) {
            UnicodeString *st = new UnicodeString(buffer, len);
            CEList *ceList = new CEList(coll, *st);

            ceToCharsStartingWith->put(ceList->get(0), st);

#ifdef CACHE_CELISTS
            charsToCEList->put(st, ceList);
#else
            delete ceList;
#endif
        } else {
            // shouldn't happen...
        }
    }

    uset_close(contractions);
    uset_close(expansions);
    uset_close(charsToTest);
}

CollData::~CollData()
{
#ifdef CLONE_COLLATOR
   ucol_close(coll);
#endif

   if (key != keyBuffer) {
       DELETE_ARRAY(key);
   }

   delete ceToCharsStartingWith;

#ifdef CACHE_CELISTS
   delete charsToCEList;
#endif
}

UCollator *CollData::getCollator() const
{
    return coll;
}

const StringList *CollData::getStringList(int32_t ce) const
{
    return ceToCharsStartingWith->getStringList(ce);
}

const CEList *CollData::getCEList(const UnicodeString *string) const
{
#ifdef CACHE_CELISTS
    return charsToCEList->get(string);
#else
    return new CEList(coll, *string);
#endif
}

void CollData::freeCEList(const CEList *list)
{
#ifndef CACHE_CELISTS
    delete list;
#endif
}

int32_t CollData::minLengthInChars(const CEList *ceList, int32_t offset, int32_t *history) const
{
    // find out shortest string for the longest sequence of ces.
    // this can probably be folded with the minLengthCache...

    if (history[offset] >= 0) {
        return history[offset];
    }

    int32_t ce = ceList->get(offset);
    int32_t maxOffset = ceList->size();
    int32_t shortestLength = INT32_MAX;
    const StringList *strings = ceToCharsStartingWith->getStringList(ce);

    if (strings != NULL) {
        int32_t stringCount = strings->size();
      
        for (int32_t s = 0; s < stringCount; s += 1) {
            const UnicodeString *string = strings->get(s);
#ifdef CACHE_CELISTS
            const CEList *ceList2 = charsToCEList->get(string);
#else
            const CEList *ceList2 = new CEList(coll, *string);
#endif

            if (ceList->matchesAt(offset, ceList2)) {
                int32_t clength = ceList2->size();
                int32_t slength = string->length();
                int32_t roffset = offset + clength;
                int32_t rlength = 0;
                
                if (roffset < maxOffset) {
                    rlength = minLengthInChars(ceList, roffset, history);

                    if (rlength <= 0) {
                        // ignore any dead ends
                        continue;
                    }
                }

                if (shortestLength > slength + rlength) {
                    shortestLength = slength + rlength;
                }
            }

#ifndef CACHE_CELISTS
            delete ceList2;
#endif
        }
    }

    if (shortestLength == INT32_MAX) {
        // no matching strings at this offset - just move on.
      //shortestLength = offset < (maxOffset - 1)? minLengthInChars(ceList, offset + 1, history) : 0;
        return -1;
    }

    history[offset] = shortestLength;

    return shortestLength;
}

int32_t CollData::minLengthInChars(const CEList *ceList, int32_t offset) const
{
    int32_t clength = ceList->size();
    int32_t *history = NEW_ARRAY(int32_t, clength);

    for (int32_t i = 0; i < clength; i += 1) {
        history[i] = -1;
    }

    int32_t minLength = minLengthInChars(ceList, offset, history);

    DELETE_ARRAY(history);

    return minLength;
}

CollData *CollData::open(UCollator *collator)
{
    UErrorCode status = U_ZERO_ERROR;
    CollDataCache *cache = getCollDataCache();
        
    return cache->get(collator);
}

void CollData::close(CollData *collData)
{
    CollDataCache *cache = getCollDataCache();

    cache->unref(collData);
}

CollDataCache *CollData::collDataCache = NULL;

CollDataCache *CollData::getCollDataCache()
{
    CollDataCache *cache = NULL;

    UMTX_CHECK(NULL, collDataCache, cache);

    if (cache == NULL) {
        cache = new CollDataCache();
        umtx_lock(NULL);
        if (collDataCache == NULL) {
            collDataCache = cache;

            ucln_i18n_registerCleanup(UCLN_I18N_COLL_DATA, coll_data_cleanup);
        }
        umtx_unlock(NULL);

        if (collDataCache != cache) {
            delete cache;
        }
    }

    return collDataCache;
}

void CollData::freeCollDataCache()
{
    CollDataCache *cache = NULL;

    UMTX_CHECK(NULL, collDataCache, cache);

    if (cache != NULL) {
        umtx_lock(NULL);
        if (collDataCache != NULL) {
            collDataCache = NULL;
        } else {
            cache = NULL;
        }
        umtx_unlock(NULL);

        delete cache;
    }
}

U_NAMESPACE_END

#endif // #if !UCONFIG_NO_COLLATION
