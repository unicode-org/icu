/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: January 16 2004
* Since: ICU 2.8
**********************************************************************
*/
#ifndef LOCBASED_H
#define LOCBASED_H

#include "unicode/locid.h"

#define U_LOCALE_BASED(varname, objname) \
  LocaleBased varname((objname).validLocale, (objname).actualLocale);

U_NAMESPACE_BEGIN

class U_COMMON_API LocaleBased {
    // Hmm.  This is an internal class (which may very well go away
    // altogether soon) and it is ONLY constructed on the stack.
    // It has no vtable BY DESIGN.  For this reason, I'm not inheriting
    // from UObject or UMemory.  aliu 2004-01-20

 public:

    inline LocaleBased(char* validAlias, char* actualAlias);

    inline LocaleBased(const char* validAlias, const char* actualAlias);

    /**
     * Returns the locale for this break iterator. Two flavors are available: valid and 
     * actual locale. 
     * @draft ICU 2.8
     */
    Locale getLocale(ULocDataLocaleType type, UErrorCode& status) const;

    /**
     * Get the locale for this break iterator object. You can choose between valid and actual locale.
     * @param type type of the locale we're looking for (valid or actual) 
     * @param status error code for the operation
     * @return the locale
     * @internal
     */
    const char* getLocaleID(ULocDataLocaleType type, UErrorCode& status) const;

    /**
     */
    void setLocaleIDs(const char* valid, const char* actual);

 private:

    char* valid;
    
    char* actual;
};

inline LocaleBased::LocaleBased(char* validAlias, char* actualAlias) :
    valid(validAlias), actual(actualAlias) {
}

inline LocaleBased::LocaleBased(const char* validAlias,
                                const char* actualAlias) :
    // ugh: cast away const
    valid((char*)validAlias), actual((char*)actualAlias) {
}

U_NAMESPACE_END

#endif
