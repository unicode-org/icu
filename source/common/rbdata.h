/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File rbdata.h
*
* Modification History:
*
*   Date        Name        Description
*   06/11/99    stephen     Creation. (Moved here from resbund.cpp)
*******************************************************************************
*/

#ifndef RBDATA_H
#define RBDATA_H 1

#include "unicode/utypes.h"
#include "hash.h"
#include "unicode/unistr.h"

/**
 * Abstract base class for data stored in resource bundles.  These
 * objects are kept in hashtables, indexed by strings.  We never need
 * to copy or clone these objects, since they are created once and
 * never deleted.  
 */
class ResourceBundleData
{
public:
  virtual ~ResourceBundleData() {}
  virtual UClassID getDynamicClassID(void) const = 0;
};

/** Concrete data class representing a list of strings.  */
class StringList : public ResourceBundleData
{
public:
  StringList();
  StringList(UnicodeString* adopted, int32_t count);
  virtual ~StringList();
  const UnicodeString& operator[](int32_t i) const;
  virtual UClassID getDynamicClassID(void) const;
  static UClassID getStaticClassID(void);

  static UClassID  fgClassID;
  int32_t         fCount;
  UnicodeString   *fStrings;
};

/** Concrete data class representing a 2-dimensional list of strings. */
class String2dList : public ResourceBundleData
{
public:
  String2dList();
  String2dList(UnicodeString** adopted, int32_t rowCount, int32_t colCount);
  virtual ~String2dList();
  const UnicodeString& getString(int32_t rowIndex, int32_t colIndex);
  virtual UClassID getDynamicClassID(void) const;
  static UClassID getStaticClassID(void);

  static UClassID  fgClassID;
  int32_t         fRowCount;
  int32_t         fColCount;
  UnicodeString   **fStrings;
};

/**
 * Concrete data class representing a tagged list of strings.  This is
 * implemented using a hash table.  
 */
class TaggedList : public ResourceBundleData
{
    Hashtable *hash;

public:
    TaggedList();
    virtual ~TaggedList();
    
    void put(const UnicodeString& tag, const UnicodeString& data);
    const UnicodeString* get(const UnicodeString& tag) const;
    bool_t nextElement(const UnicodeString*& key,
                       const UnicodeString*& value,
                       int32_t& pos) const;
    int32_t count() const;

    virtual UClassID getDynamicClassID(void) const;
    static UClassID getStaticClassID(void);
    
    static UClassID  fgClassID;
};

#endif
