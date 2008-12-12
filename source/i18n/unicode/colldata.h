/*
 ******************************************************************************
 *   Copyright (C) 1996-2008, International Business Machines                 *
 *   Corporation and others.  All Rights Reserved.                            *
 ******************************************************************************
 */

/**
 * \file 
 * \brief C++ API: Collation data used to compute minLengthInChars.
 */
 
#ifndef COLL_DATA_H
#define COLL_DATA_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/uobject.h"
#include "unicode/ucol.h"

U_NAMESPACE_BEGIN

#define KEY_BUFFER_SIZE 64

#define CELIST_BUFFER_SIZE 4
#define INSTRUMENT_CELIST

#define STRING_LIST_BUFFER_SIZE 16
#define INSTRUMENT_STRING_LIST

class U_I18N_API CEList : public UObject
{
public:
    CEList(UCollator *coll, const UnicodeString &string);
    ~CEList();

    int32_t size() const;
    int32_t get(int32_t index) const;
    UBool matchesAt(int32_t offset, const CEList *other) const; 

    int32_t &operator[](int32_t index) const;

    virtual UClassID getDynamicClassID() const;
    static UClassID getStaticClassID();

private:
    void add(int32_t ce);

    int32_t ceBuffer[CELIST_BUFFER_SIZE];
    int32_t *ces;
    int32_t listMax;
    int32_t listSize;

#ifdef INSTRUMENT_CELIST
    static int32_t _active;
    static int32_t _histogram[10];
#endif
};

class U_I18N_API StringList : public UObject
{
public:
    StringList();
    ~StringList();

    void add(const UnicodeString *string);
    void add(const UChar *chars, int32_t count);
    const UnicodeString *get(int32_t index) const;
    int32_t size() const;

    virtual UClassID getDynamicClassID() const;
    static UClassID getStaticClassID();

private:
    UnicodeString *strings;
    int32_t listMax;
    int32_t listSize;

#ifdef INSTRUMENT_STRING_LIST
    static int32_t _lists;
    static int32_t _strings;
    static int32_t _histogram[101];
#endif
};

class StringToCEsMap;
class CEToStringsMap;
class CollDataCache;

class U_I18N_API CollData : public UObject
{
public:
    static CollData *open(UCollator *collator);
    static void close(CollData *collData);

    UCollator *getCollator() const;

    const StringList *getStringList(int32_t ce) const;

    const CEList *getCEList(const UnicodeString *string) const;
    void freeCEList(const CEList *list);

    int32_t minLengthInChars(const CEList *ces, int32_t offset) const;

    int32_t minLengthInChars(const CEList *ces, int32_t offset, int32_t *history) const;

    virtual UClassID getDynamicClassID() const;
    static UClassID getStaticClassID();

    static void freeCollDataCache();

private:
    friend class CollDataCache;
    friend class CollDataCacheEntry;

    CollData(UCollator *collator, char *cacheKey, int32_t cachekeyLength);
    ~CollData();

    CollData();

    static char *getCollatorKey(UCollator *collator, char *buffer, int32_t bufferLength);

    static CollDataCache *getCollDataCache();

    UCollator      *coll;
    StringToCEsMap *charsToCEList;
    CEToStringsMap *ceToCharsStartingWith;

    char keyBuffer[KEY_BUFFER_SIZE];
    char *key;

    static CollDataCache *collDataCache;
};

U_NAMESPACE_END

#endif // #if !UCONFIG_NO_COLLATION
#endif // #ifndef COLL_DATA_H
