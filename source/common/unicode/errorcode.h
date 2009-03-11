/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  errorcode.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009mar10
*   created by: Markus W. Scherer
*/

#ifndef __ERRORCODE_H__
#define __ERRORCODE_H__

/**
 * \file 
 * \brief C++ API: ErrorCode class intended to make it easier to use
 *                 ICU C and C++ APIs from C++ user code.
 */

#include "unicode/utypes.h"
#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

/**
 * Wrapper class for UErrorCode, with conversion operators for direct use
 * in ICU C and C++ APIs.
 * Intended to be used as a base class, where a subclass overrides
 * the handleFailure() function so that it throws an exception,
 * does an assert(), logs an error, etc.
 * This is not an abstract base class. This class can be used and instantiated
 * by itself, although it will be more useful when subclassed.
 *
 * Features:
 * - The constructor initializes the internal UErrorCode to U_ZERO_ERROR,
 *   removing one common source of errors.
 * - Same use in C APIs taking a UErrorCode * (pointer)
 *   and C++ taking UErrorCode & (reference) via conversion operators.
 * - Automatic checking for success when it goes out of scope.
 *
 * Code sample, using an appropriate IcuErrorCode subclass:
 * \code
 *   IcuErrorCode error_code;
 *   UConverter *cnv = ucnv_open("Shift-JIS", error_code);
 *   length = ucnv_fromUChars(dest, capacity, src, length, error_code);
 *   ucnv_close(cnv);
 *   // IcuErrorCode destructor checks for success.
 * \endcode
 *
 * @draft ICU 4.2
 */
class U_COMMON_API ErrorCode: public UMemory {
public:
    /**
     * Default constructor. Initializes its UErrorCode to U_ZERO_ERROR.
     * @draft ICU 4.2
     */
    ErrorCode() : errorCode(U_ZERO_ERROR) {}
    /**
     * Destructor, does nothing.
     * A subclass destructor should do
     *   if(isFailure()) { handleFailure(kDestructor); }
     * @draft ICU 4.2
     */
    virtual ~ErrorCode();
    /** Conversion operator, returns a reference. @draft ICU 4.2 */
    operator UErrorCode & () { return errorCode; }
    /** Conversion operator, returns a pointer. @draft ICU 4.2 */
    operator UErrorCode * () { return &errorCode; }
    /** Tests for U_SUCCESS(). @draft ICU 4.2 */
    UBool isSuccess() const { return U_SUCCESS(errorCode); }
    /** Tests for U_FAILURE(). @draft ICU 4.2 */
    UBool isFailure() const { return U_FAILURE(errorCode); }
    /** Returns the UErrorCode value. @draft ICU 4.2 */
    UErrorCode get() const { return errorCode; }
    /** Sets the UErrorCode value. @draft ICU 4.2 */
    void set(UErrorCode value) { errorCode=value; }
    /** Returns the UErrorCode value and resets it to U_ZERO_ERROR. @draft ICU 4.2 */
    UErrorCode reset();
    /**
     * Checks for a failure code:
     * if(isFailure()) { handleFailure(kCheck); }
     * @draft ICU 4.2
     */
    void check() const;

protected:
    UErrorCode errorCode;
    enum EOrigin { kCheck, kDestructor };
    // Note: A C++ class destructor must not throw an exception.
    // Use the origin parameter to avoid this if necessary.
    virtual void handleFailure(EOrigin origin) const {}
};

U_NAMESPACE_END

#endif  // __ERRORCODE_H__
