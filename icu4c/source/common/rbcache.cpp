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

ResourceBundleCache::ResourceBundleCache()
{
  UErrorCode err = U_ZERO_ERROR;
  hashTable = uhash_open((UHashFunction)uhash_OLD_hashUString,
                         uhash_OLD_pointerComparator, &err);
  uhash_setValueDeleter(hashTable, deleteValue);
}

void ResourceBundleCache::deleteValue(void* value)
{
  uhash_close((UHashtable*)value);
}

//----------------------------------------------------------------------------------

VisitedFileCache::VisitedFileCache()
{
  UErrorCode err = U_ZERO_ERROR;
  hashTable = uhash_open((UHashFunction)uhash_OLD_hashUString,
                         uhash_OLD_pointerComparator, &err);
}

VisitedFileCache::~VisitedFileCache()
{
  uhash_close(hashTable);
}

ResourceBundleCache::~ResourceBundleCache()
{
  uhash_close(hashTable);
}


//eof




