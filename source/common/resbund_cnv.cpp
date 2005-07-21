/*
*******************************************************************************
*
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  resbund_cnv.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004aug25
*   created by: Markus W. Scherer
*
*   Character conversion functions moved here from resbund.cpp
*/

#include "unicode/utypes.h"
#include "unicode/resbund.h"
#include "uinvchar.h"

U_NAMESPACE_BEGIN

ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                const Locale&           locale,
                                UErrorCode&              error)
                                :UObject(), fLocale(NULL)
{
    constructForLocale(path, locale, error);
}

ResourceBundle::ResourceBundle( const UnicodeString&    path,
                                UErrorCode&              error)
                                :UObject(), fLocale(NULL)
{
    constructForLocale(path, Locale::getDefault(), error);
}

void 
ResourceBundle::constructForLocale(const UnicodeString& path,
                                   const Locale& locale,
                                   UErrorCode& error)
{
    char name[300];

    if(path.length() >= (int32_t)sizeof(name)) {
        fResource = NULL;
        error = U_ILLEGAL_ARGUMENT_ERROR;
    } else if(!path.isEmpty()) {
        if(uprv_isInvariantUString(path.getBuffer(), path.length())) {
            // the invariant converter is sufficient for package and tree names
            // and is more efficient
            path.extract(0, INT32_MAX, name, (int32_t)sizeof(name), US_INV);
        } else {
#if !UCONFIG_NO_CONVERSION
            // use the default converter to support variant-character paths
            path.extract(name, sizeof(name), 0, error);
#else
            // the default converter is not available
            fResource = NULL;
            error = U_UNSUPPORTED_ERROR;
            return;
#endif
        }
        fResource = ures_open(name, locale.getName(), &error);
    } else {
        fResource = ures_open(0, locale.getName(), &error);
    }
}

U_NAMESPACE_END
