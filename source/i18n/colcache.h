/*
*****************************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*/
//===============================================================================
//
// File colcache.h
//
// CollationCache implements a persistent in-memory cache for
// TableCollationData objects.  The goal of CollationCache is to improve
// the memory footprint of a process which may have multiple threads
// loading up the same TableCollation object.  Performance improvement is
// strictly a secondary goal.
//
// Created by: Alan Liu
//
// Modification History:
//
//  Date        Name        Description
//  2/11/97     aliu        Creation.
//  2/12/97     aliu        Modified to work with TableCollationData.
//  8/18/97     helena      Added internal API documentation.
//
//===============================================================================

#ifndef COLCACHE_H
#define COLCACHE_H

#include "hash.h"
#include "unicode/unistr.h"

class Hashtable;
class TableCollationData;

// Tell the VC++ compiler not to warn about DLL interface
#ifdef _WIN32
#pragma warning( disable : 4251 )
#endif

//-------------------------------------------------------------------------------
/**
 * CollationCache implements a simple cache for TableCollationData objects.
 * TableCollationData objects may be added to the cache, and looked up in the
 * cache.  When the cache is destroyed, all the TableCollationData objects are
 * deleted.
 */

class CollationCache
{
public:
    /**
     * Default constructor.
     */
    CollationCache();

    /**
     * Destructor.
     */
    inline ~CollationCache() {}

    /** 
     * ::Add and ::Find use a UnicodeString as the key to Collation objects in the
     * cache.  If Add is called twice with equivalent keys, but different
     * collation objects, the first collation object will be deleted when the
     * second one is added.  In general, this is undesirable; objects in the
     * cache are usually pointed to by various clients in the system.  For this
     * reason, clients should call Find to ensure a Collation object does not
     * already exist in the cache for the given key before calling Add.
     * @param key the unique key.
     * @param data the collation data object.
     * @return the found collation data object
     */
    void                Add(const UnicodeString& key, TableCollationData* adoptedData);
    TableCollationData* Find(const UnicodeString& key);

private:
    Hashtable   fHashtable;
};

inline void CollationCache::Add(const UnicodeString& key, TableCollationData* adoptedValue) {
    UErrorCode status = U_ZERO_ERROR;
    fHashtable.put(key, adoptedValue, status);
}

inline TableCollationData* CollationCache::Find(const UnicodeString& keyString) {
    return (TableCollationData*) fHashtable.get(keyString);
}

#endif
//eof
