/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/* Created by weiv 05/09/2002 */

#ifndef INTLTST_DATAMAP
#define INTLTST_DATAMAP

#include "hash.h"
#include "unicode/ures.h"

/** Holder of test data and settings. Allows addressing of items by name.
 *  For test cases, names are defined in the "Headers" section. For settings
 *  and info data, names are keys in data. Currently, we return scalar strings
 *  and integers and arrays of strings and integers. Arrays should be deposited
 *  of by the user. 
 */
class DataMap {
public:
  virtual ~DataMap() {};

protected:
  DataMap() {};
  int32_t utoi(const UnicodeString &s) const;


public:
  /** get the string from the DataMap. Addressed by name
   *  @param key name of the data field.
   *  @return a string containing the data
   */
  virtual const UnicodeString getString(const char* key, UErrorCode &status) const = 0;

  /** get the string from the DataMap. Addressed by name
   *  @param key name of the data field.
   *  @return an integer containing the data
   */
  virtual int32_t getInt(const char* key, UErrorCode &status) const = 0;
  
  /** get an array of strings from the DataMap. Addressed by name.
   *  The user must dispose of it after usage.
   *  @param key name of the data field.
   *  @return a string array containing the data
   */
  virtual const UnicodeString* getStringArray(int32_t& count, const char* key, UErrorCode &status) const = 0;

  /** get an array of integers from the DataMap. Addressed by name.
   *  The user must dispose of it after usage.
   *  @param key name of the data field.
   *  @return an integer array containing the data
   */
  virtual const int32_t* getIntArray(int32_t& count, const char* key, UErrorCode &status) const = 0;

  // ... etc ...
};

// This one is already concrete - it is going to be instantiated from 
// concrete data by TestData children...
class RBDataMap : public DataMap{
private:
  Hashtable *fData;

public:
  virtual ~RBDataMap();

public:
  RBDataMap();

  RBDataMap(UResourceBundle *data, UErrorCode &status);
  RBDataMap(UResourceBundle *headers, UResourceBundle *data, UErrorCode &status);

public:
  void init(UResourceBundle *data, UErrorCode &status);
  void init(UResourceBundle *headers, UResourceBundle *data, UErrorCode &status);
  
  virtual const UnicodeString getString(const char* key, UErrorCode &status) const;
  virtual int32_t getInt(const char* key, UErrorCode &status) const;
  
  virtual const UnicodeString* getStringArray(int32_t& count, const char* key, UErrorCode &status) const;
  virtual const int32_t* getIntArray(int32_t& count, const char* key, UErrorCode &status) const;

  // ... etc ...
};

#endif

