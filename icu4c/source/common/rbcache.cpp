/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
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
  UErrorCode err = ZERO_ERROR;
  hashTable = uhash_open((UHashFunction)uhash_hashUString, &err);
  uhash_setValueDeleter(hashTable, deleteValue);
}

void ResourceBundleCache::deleteValue(void* value)
{
  uhash_close((UHashtable*)value);
}

//----------------------------------------------------------------------------------

VisitedFileCache::VisitedFileCache()
{
  UErrorCode err = ZERO_ERROR;
  hashTable = uhash_open((UHashFunction)uhash_hashUString, &err);
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




