/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#ifndef STRENUM_H
#define STRENUM_H

#include "uobject.h"

U_NAMESPACE_BEGIN

class UnicodeString;

/**
 * Base class for 'pure' C++ implementations.  Adds method that
 * returns the next UnicodeString since in C++ this might be a
 * common storage model for strings.
 */
class U_COMMON_API StringEnumeration : public UMemory {
 public:
  virtual ~StringEnumeration();
  virtual int32_t count(UErrorCode& status) const = 0;

  virtual const char* next(UErrorCode& status) = 0;
  virtual const UChar* unext(UErrorCode& status) = 0;
  virtual const UnicodeString* snext(UErrorCode& status) = 0;

  virtual void reset(UErrorCode& status) = 0;
};

inline StringEnumeration::~StringEnumeration() {
}

U_NAMESPACE_END

/* STRENUM_H */
#endif
