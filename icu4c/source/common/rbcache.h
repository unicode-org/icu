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

#include "uhash.h"
#include "unicode/unistr.h"

/**
 * A class which represents an ordinary Hashtable which deletes its contents when it
 * is destroyed.  This class stores UnicodeStringKeys as its keys, and
 * ResourceBundleData objects as its values.
 */
class U_COMMON_API ResourceBundleCache  // Not really external; just making the compiler happy
{
 public:
  ResourceBundleCache();
  ~ResourceBundleCache();
  UHashtable* hashTable;
 private:
  static void U_CALLCONV deleteValue(void* value);
};

/**
 * A hashtable which owns its keys and values and deletes them when it is destroyed.
 * This class stored UnicodeStringKeys as its keys, and the value 1 as its objects.
 * in other words, the objects are just (void*)1.  The only real information is
 * whether or not a key is present.  Semantically, if a key is present, it means
 * that the corresponding filename has been visited already.
 */
class U_COMMON_API VisitedFileCache // Not really external; just making the compiler happy
{
 public:
  
  VisitedFileCache();
  ~VisitedFileCache();
  UHashtable* hashTable;
  inline bool_t wasVisited(const UnicodeString& filename) const;
  inline void markAsVisited(const UnicodeString& filename);
};

inline bool_t VisitedFileCache::wasVisited(const UnicodeString& filename) const
{
  return (uhash_get(hashTable, uhash_hashUString(filename.getUChars())) != 0);
}

inline void VisitedFileCache::markAsVisited(const UnicodeString& filename)
{
  UErrorCode err = U_ZERO_ERROR;
  uhash_putKey(hashTable, uhash_hashUString(filename.getUChars()), (void*)TRUE, &err);
}


//eof
