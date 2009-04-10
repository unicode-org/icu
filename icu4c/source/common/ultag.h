/*
**********************************************************************
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#ifndef ULTAG_H
#define ULTAG_H

#include "unicode/utypes.h"

typedef struct ULanguageTag ULanguageTag;

U_CFUNC ULanguageTag*
ultag_parse(const char* tag, int32_t tagLen, int32_t* parsedLen, UErrorCode* status);

U_CFUNC void
ultag_close(ULanguageTag* langtag);

U_CFUNC const char*
ultag_getLanguage(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getJDKLanguage(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getExtlang(const ULanguageTag* langtag, int32_t idx);

U_CFUNC int32_t
ultag_getExtlangSize(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getScript(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getRegion(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getVariant(const ULanguageTag* langtag, int32_t idx);

U_CFUNC int32_t
ultag_getVariantsSize(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getExtensionKey(const ULanguageTag* langtag, int32_t idx);

U_CFUNC const char*
ultag_getExtensionValue(const ULanguageTag* langtag, int32_t idx);

U_CFUNC int32_t
ultag_getExtensionsSize(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getPrivateUse(const ULanguageTag* langtag);

U_CFUNC const char*
ultag_getGrandfathered(const ULanguageTag* langtag);

U_CFUNC int32_t
ultag_languageTagToLocale(const char* langtag,
                          char* localeID,
                          int32_t localeIDCapacity,
                          int32_t* parsedLength,
                          UErrorCode* status);

U_CFUNC int32_t
ultag_localeToLanguageTag(const char* localeID,
                          char* langtag,
                          int32_t langtagCapacity,
                          UBool strict,
                          UErrorCode* status);


#endif /* ULTAG_H */
