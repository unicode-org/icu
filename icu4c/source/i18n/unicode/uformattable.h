/*
********************************************************************************
* Copyright (C) 2013, International Business Machines Corporation and others.
* All Rights Reserved.
********************************************************************************
*
* File UFORMATTABLE.H
*
* Modification History:
*
*   Date        Name        Description
*   2013 Jun 7  srl         New
********************************************************************************
*/

/**
 * \file
 * \brief C API: UFormattable is a thin wrapper for primitive types used for formatting and parsing.
 *
 * This is a C interface to the icu::Formattable class. Static functions on this class convert
 * to and from this interface (via reinterpret_cast).  Note that Formattables (and thus UFormattables)
 * are mutable, and many operations (even getters) may actually modify the internal state. For this
 * reason, UFormattables are not thread safe, and should not be shared between threads.
 */

#ifndef FORMATTABLE_H
#define FORMATTABLE_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/localpointer.h"

/**
 * Enum designating the type of a UFormattable instance.
 * Practically, this indicates which of the getters would return without conversion
 * or error.
 * @see icu::Formattable::Type
 * @draft ICU 52
 */
typedef enum UFormattableType {
  UFMT_DATE = 0, /**< ufmt_getDate() will return without conversion. */
  UFMT_DOUBLE,   /**< ufmt_getDouble() will return without conversion. */
  UFMT_LONG,     /**< ufmt_getLong() will return without conversion. */
  UFMT_INT64,    /**< ufmt_getInt64() will return without conversion. */
  UFMT_OBJECT,   /**< ufmt_getObject() will return without conversion. */
  UFMT_STRING,   /**< ufmt_getUChars() will return without conversion. */
  UFMT_ARRAY,    /**< ufmt_countArray() and ufmt_getArray() will return the value. */
  UFMT_COUNT     /**< Count of defined UFormattableType values */
} UFormattableType;


/**
 * Opaque type representing various types of data which may be used for formatting
 * and parsing operations.
 * @see icu::Formattable
 * @draft ICU 52
 */
typedef void *UFormattable;

/**
 * Initialize a UFormattable, to type UNUM_LONG, value 0
 * may return error if memory allocation failed.
 * parameter status error code.
 * @draft ICU 52
 * @return the new UFormattable
 */
U_DRAFT UFormattable* U_EXPORT2
ufmt_open(UErrorCode* status);

/**
 * Cleanup any additional memory allocated by this UFormattable.
 * @draft ICU 52
 */
U_DRAFT void U_EXPORT2
ufmt_close(UFormattable* fmt);

#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * \class LocalUFormattablePointer
 * "Smart pointer" class, closes a UFormattable via ufmt_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @draft ICU 52
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUFormattablePointer, UFormattable, ufmt_close);

U_NAMESPACE_END

#endif

/**
 * Return the type of this object
 * @param fmt the UFormattable object
 * @status status code - U_ILLEGAL_ARGUMENT_ERROR is returned if the UFormattable contains data not supported by
 * the API
 * @return the value as a UFormattableType
 * @draft ICU 52
 */
U_DRAFT UFormattableType U_EXPORT2
ufmt_getType(UFormattable* fmt, UErrorCode *status);

/**
 * Return whether the object is numeric.
 * @param fmt the UFormattable object
 * @return true if the object is a double, long, or int64 value.
 * @draft ICU 52
 */
U_DRAFT UBool U_EXPORT2
ufmt_isNumeric(UFormattable* fmt);

/**
 * Get the value as a date, converting if need be.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors
 * @return the value
 * @draft ICU 52
 */
U_DRAFT UDate U_EXPORT2
ufmt_getDate(UFormattable* fmt, UErrorCode *status);

/**
 * Get the value as a double, converting if need be.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors
 * @return the value
 * @draft ICU 52
 */
U_DRAFT double U_EXPORT2
ufmt_getDouble(UFormattable* fmt, UErrorCode *status);

/**
 * Get the value as a int32_t, converting if need be.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors
 * @return the value
 * @draft ICU 52
 */
U_DRAFT int32_t U_EXPORT2
ufmt_getLong(UFormattable* fmt, UErrorCode *status);


/**
 * Get the value as a int64_t, converting if need be.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors
 * @return the value
 * @draft ICU 52
 */
U_DRAFT int64_t U_EXPORT2
ufmt_getInt64(UFormattable* fmt, UErrorCode *status);

/**
 * Get the value as an object.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors
 * @return the value as a const void*. It is a polymorphic C++ object.
 * @draft ICU 52
 */
U_DRAFT const void *U_EXPORT2
ufmt_getObject(UFormattable* fmt, UErrorCode *status);

/**
 * Get the value as UChar string, converting if need be.
 * This function is not thread safe and may modify the UFormattable if need be to terminate the buffer.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors
 * @param len if non null, contains the string length on return
 * @return the null terminated string value - must not be referenced after any other functions are called on this UFormattable.
 * @draft ICU 52
 */
U_DRAFT const UChar* U_EXPORT2
ufmt_getUChars(UFormattable* fmt, int32_t *len, UErrorCode *status);

/**
 * Get the number of array objects contained. Invalid if the object is not an array type.
 * @param fmt the UFormattable object
 * @param status the error code - any conversion or format errors. U_ILLEGAL_ARGUMENT_ERROR if not an array type.
 * @return the number of array objects
 * @draft ICU 52
 */
U_DRAFT int32_t U_EXPORT2
ufmt_getArrayLength(UFormattable* fmt, UErrorCode *status);

/**
 * Get the specified value from the array of UFormattables. Invalid if the object is not an array type.
 * @param fmt the UFormattable object
 * #param n the number of the array to return (0 based).
 * @param status the error code - any conversion or format errors. Returns an error if n is out of bounds.
 * @return the nth array value, only valid while the containing UFormattable is valid
 * @draft ICU 52
 */
U_DRAFT UFormattable * U_EXPORT2
ufmt_getArrayItemByIndex(UFormattable* fmt, int32_t n, UErrorCode *status);

/**
 * Get the value as a C String, if it is a numeric type (isNumeric is true), or if is an Object
 * with a numeric value. The returned string is not valid if any other function calls are made on this
 * object, or if it is destroyed.
 * @param fmt the UFormattable object
 * @param len if non-null, on exit contains the string length (not including the terminating null)
 * @param status the error code
 * @return the character buffer, which is owned by the object and must not be accessed if any other functions are called on this object.
 * @draft ICU 52
 */
U_DRAFT const char * U_EXPORT2
ufmt_getDecNumChars(UFormattable *fmt, int32_t *len, UErrorCode *status);

#endif

#endif
