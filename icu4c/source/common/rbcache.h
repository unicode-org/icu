/*
*****************************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*
* File rbcache.h
*
* Modification History:
*
*   Date        Name        Description
*   03/20/97    aliu        Creation.
*   04/29/97    aliu        Convert to use new Hashtable protocol.
*   04/15/99    damiba      plugged in new C hashtable
*****************************************************************************************
*/

#include "hash.h"
#include "unicode/unistr.h"

/* Disable these warnings:
d:\icu\source\common\rbcache.h(31) : warning C4251: 'hash' : class 'Hashtable' needs to have dll-interface to be used by clients of class 'ResourceBundleCache'
d:\icu\source\common\rbcache.h(57) : warning C4251: 'hash' : class 'Hashtable' needs to have dll-interface to be used by clients of class 'VisitedFileCache'
*/
#ifdef _WIN32
#pragma warning( disable : 4251 )
#endif

/**
 * A class that maps UnicodeString keys to UHashtable objects.  It
 * owns the UHashtable objects passed to put() and will eventually
 * close and delete them.
 */
class U_COMMON_API ResourceBundleCache { // Not really external; just making the compiler happy
private:
    Hashtable hash;
    static void U_CALLCONV deleteUHashtable(void* value);
public:
    ResourceBundleCache();
    inline ~ResourceBundleCache() {}
    inline void put(const UnicodeString& key, UHashtable* adoptedValue);
    inline const UHashtable* get(const UnicodeString& key) const;
};

inline void ResourceBundleCache::put(const UnicodeString& key, UHashtable* adoptedValue) {
    UErrorCode status = U_ZERO_ERROR;
    hash.put(key, adoptedValue, status);
}

inline const UHashtable* ResourceBundleCache::get(const UnicodeString& key) const {
    return (const UHashtable*) hash.get(key);
}

/**
 * A class that records whether a filename has been seen before or
 * not.  Call markAsVisited() to mark a filename as seen.  Call
 * wasVisited() to see if markAsVisited() has been called with that
 * filename or not.
 */
class U_COMMON_API VisitedFileCache { // Not really external; just making the compiler happy
private:
    Hashtable hash;
public:
    inline VisitedFileCache() {}
    inline ~VisitedFileCache() {}
    inline UBool wasVisited(const UnicodeString& filename) const;
    inline void markAsVisited(const UnicodeString& filename);
};

inline UBool VisitedFileCache::wasVisited(const UnicodeString& filename) const {
    return (hash.get(filename) != 0);
}

inline void VisitedFileCache::markAsVisited(const UnicodeString& filename) {
    UErrorCode status = U_ZERO_ERROR;
    hash.put(filename, (void*)1, status);
}

//eof
