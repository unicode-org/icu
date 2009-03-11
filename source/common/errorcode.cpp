/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  errorcode.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009mar10
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/errorcode.h"

U_NAMESPACE_BEGIN

ErrorCode::~ErrorCode() {}
/*  Logically
        if(isFailure()) {
            handleFailure(kDestructor);
        }
    but in the destructor, even if it's virtual, this does not call
    the subclass' handleFailure(), and our own handleFailure()
    does not do anything.
    The subclass must have this code.
*/

UErrorCode ErrorCode::reset() {
    UErrorCode code = errorCode;
    errorCode = U_ZERO_ERROR;
    return code;
}

void ErrorCode::check() const {
    if(isFailure()) { handleFailure(kCheck); }
}

U_NAMESPACE_END
