/*
**********************************************************************
*   Copyright (c) 2001-2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#ifndef _UPROPSET_H_
#define _UPROPSET_H_

#include "unicode/uniset.h"

U_NAMESPACE_BEGIN

/**
 * INTERNAL CLASS
 *
 * NOTE: This class is almost gone...TODO: Figure out where to put
 * getRuleWhiteSpaceSet(), move it there, and remove this file.
 *
 * @author Alan Liu
 * @internal
 */
class U_COMMON_API UnicodePropertySet /* not : public UObject because all methods are static */ {

 public:

    /**
     * "white space" in the sense of ICU rule parsers
     * @internal
     */
    static UnicodeSet getRuleWhiteSpaceSet(UErrorCode &status);

 private:
    // do not instantiate
    UnicodePropertySet();
};

U_NAMESPACE_END

#endif
