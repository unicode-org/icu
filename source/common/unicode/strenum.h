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

#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

class UnicodeString;

/**
 * Base class for 'pure' C++ implementations of uenum api.  Adds a
 * method that returns the next UnicodeString since in C++ this can
 * be a common storage format for strings.
 *
 * <p>The model is that the enumeration is over strings maintained by
 * a 'service.'  At any point, the service might change, invalidating
 * the enumerator (though this is expected to be rare).  The iterator
 * returns an error if this has occurred.  Lack of the error is no
 * guarantee that the service didn't change immediately after the
 * call, so the returned string still might not be 'valid' on
 * subsequent use.</p>
 *
 * <p>Strings may take the form of const char*, const UChar*, or const
 * UnicodeString*.  The type you get is determine by the variant of
 * 'next' that you call.  In general the StringEnumeration is
 * optimized for one of these types, but all StringEnumerations can
 * return all types.  Returned strings are each terminated with a NUL.
 * Depending on the service data, they might also include embedded NUL
 * characters, so API is provided to optionally return the true
 * length, counting the embedded NULs but not counting the terminating
 * NUL.</p>
 *
 * <p>The pointers returned by next, unext, and snext become invalid
 * upon any subsequent call to the enumeration's destructor, next,
 * unext, snext, or reset.</p>
 *
 * @draft ICU 2.4 
 */
class U_COMMON_API StringEnumeration : public UObject { 
 public:
  /**
   * Destructor.
   * @draft ICU 2.4
   */
  virtual ~StringEnumeration();

  /**
   * <p>Return the number of elements that the iterator traverses.  If
   * the iterator is out of sync with its service, status is set to
   * U_ENUM_OUT_OF_SYNC_ERROR, and the return value is zero.</p>
   *
   * <p>The return value will not change except possibly as a result of
   * a subsequent call to reset, or if the iterator becomes out of sync.</p>
   *
   * <p>This is a convenience function. It can end up being very
   * expensive as all the items might have to be pre-fetched
   * (depending on the storage format of the data being
   * traversed).</p>
   *
   * @param status the error code.
   * @return number of elements in the iterator.
   *
   * @draft ICU 2.4 */
  virtual int32_t count(UErrorCode& status) const = 0;

  /**
   * <p>Returns the next element as a NUL-terminated char*.  If there
   * are no more elements, returns NULL.  If the resultLength pointer
   * is not NULL, the length of the string (not counting the
   * terminating NUL) is returned at that address.  If an error
   * status is returned, the value at resultLength is undefined.</p>
   *
   * <p>The returned pointer is owned by this iterator and must not be
   * deleted by the caller.  The pointer is valid until the next call
   * to next, unext, snext, reset, or the enumerator's destructor.</p>
   *
   * <p>If the iterator is out of sync with its service, status is set
   * to U_ENUM_OUT_OF_SYNC_ERROR and NULL is returned.</p>
   *
   * <p>If the native service string is a UChar* string, it is
   * converted to char* with the invariant converter.  If the
   * conversion fails (because a character cannot be converted) then
   * status is set to U_INVARIANT_CONVERSION_ERROR and the return
   * value is undefined (though not NULL).</p>
   *
   * @param status the error code.
   * @param resultLength a pointer to receive the length, can be NULL.
   * @return a pointer to the string, or NULL.
   *
   * @draft ICU 2.4 
   */
  virtual const char* next(int32_t *resultLength, UErrorCode& status) = 0;

  /**
   * <p>Returns the next element as a NUL-terminated UChar*.  If there
   * are no more elements, returns NULL.  If the resultLength pointer
   * is not NULL, the length of the string (not counting the
   * terminating NUL) is returned at that address.  If an error
   * status is returned, the value at resultLength is undefined.</p>
   *
   * <p>The returned pointer is owned by this iterator and must not be
   * deleted by the caller.  The pointer is valid until the next call
   * to next, unext, snext, reset, or the enumerator's destructor.</p>
   *
   * <p>If the iterator is out of sync with its service, status is set
   * to U_ENUM_OUT_OF_SYNC_ERROR and NULL is returned.</p>
   *
   * @param status the error code.
   * @param resultLength a ponter to receive the length, can be NULL.
   * @return a pointer to the string, or NULL.
   *
   * @draft ICU 2.4 
   */
  virtual const UChar* unext(int32_t *resultLength, UErrorCode& status) = 0;

  /**
   * <p>Returns the next element a UnicodeString*.  If there are no
   * more elements, returns NULL.</p>
   *
   * <p>The returned pointer is owned by this iterator and must not be
   * deleted by the caller.  The pointer is valid until the next call
   * to next, unext, snext, reset, or the enumerator's destructor.</p>
   *
   * <p>If the iterator is out of sync with its service, status is set
   * to U_ENUM_OUT_OF_SYNC_ERROR and NULL is returned.</p>
   *
   * @param status the error code.
   * @return a pointer to the string, or NULL.
   *
   * @draft ICU 2.4 
   */
  virtual const UnicodeString* snext(UErrorCode& status) = 0;

  /**
   * <p>Resets the iterator.  This re-establishes sync with the
   * service and rewinds the iterator to start at the first
   * element.</p>
   *
   * <p>Previous pointers returned by next, unext, or snext become
   * invalid, and the value returned by count might change.</p>
   *
   * @param status the error code.
   *
   * @draft ICU 2.4 
   */
  virtual void reset(UErrorCode& status) = 0;
};

inline StringEnumeration::~StringEnumeration() {
}

U_NAMESPACE_END

/* STRENUM_H */
#endif
