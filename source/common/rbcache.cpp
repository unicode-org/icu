/*
 **********************************************************************
 *   Copyright (C) 1997-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*
* File rbcache.cpp
*
* Modification History:
*
*   Date        Name        Description
*   03/20/97    aliu        Creation.
*   04/29/97    aliu        Convert to use new Hashtable protocol.
*   04/15/99    damiba      plugged in C hash table.
*****************************************************************************************
*/

#include "rbcache.h"

ResourceBundleCache::ResourceBundleCache() : hash() {
    hash.setValueDeleter(deleteUHashtable);
}

void ResourceBundleCache::deleteUHashtable(void* value) {
    uhash_close((UHashtable*)value);
}

//eof




