/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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

#include "utypes.h"
#include "uhash.h"
#include "unistr.h"

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
public:
  TaggedList();
  virtual ~TaggedList();
  void put(const UnicodeString& tag, const UnicodeString& data);
  const UnicodeString* get(const UnicodeString& tag) const;
  virtual UClassID getDynamicClassID(void) const;
  static UClassID getStaticClassID(void);

  static void deleteValue(void* value);

  static UClassID  fgClassID;
  UHashtable      *fHashtableValues;
  UHashtable      *fHashtableKeys;
};

#endif
