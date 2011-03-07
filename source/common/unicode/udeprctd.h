/*
*******************************************************************************
*   Copyright (C) 2004-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  udeprctd.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   Created by: genheaders.pl, a perl script written by Ram Viswanadha
*
*  Contains data for commenting out APIs.
*  Gets included by umachine.h
*
*  THIS FILE IS MACHINE-GENERATED, DON'T PLAY WITH IT IF YOU DON'T KNOW WHAT
*  YOU ARE DOING, OTHERWISE VERY BAD THINGS WILL HAPPEN!
*/

#ifndef UDEPRCTD_H
#define UDEPRCTD_H

#ifdef U_HIDE_DEPRECATED_API

#    if U_DISABLE_RENAMING
#        define ucol_getContractions(const UCollator *coll, USet *conts, UErrorCode *status) ucol_getContractions(const UCollator *coll, USet *conts, UErrorCode *status)_DEPRECATED_API_DO_NOT_USE
#        define ucol_getLocale(const UCollator *coll, ULocDataLocaleType type, UErrorCode *status) ucol_getLocale(const UCollator *coll, ULocDataLocaleType type, UErrorCode *status)_DEPRECATED_API_DO_NOT_USE
#        define ures_countArrayItems(const UResourceBundle *resourceBundle, const char *resourceKey, UErrorCode *err) ures_countArrayItems(const UResourceBundle *resourceBundle, const char *resourceKey, UErrorCode *err)_DEPRECATED_API_DO_NOT_USE
#        define ures_getLocale(const UResourceBundle *resourceBundle, UErrorCode *status) ures_getLocale(const UResourceBundle *resourceBundle, UErrorCode *status)_DEPRECATED_API_DO_NOT_USE
#        define ures_getVersionNumber(const UResourceBundle *resourceBundle) ures_getVersionNumber(const UResourceBundle *resourceBundle)_DEPRECATED_API_DO_NOT_USE
#        define utrans_getAvailableID(int32_t index, char *buf, int32_t bufCapacity) utrans_getAvailableID(int32_t index, char *buf, int32_t bufCapacity)_DEPRECATED_API_DO_NOT_USE
#        define utrans_getID(const UTransliterator *trans, char *buf, int32_t bufCapacity) utrans_getID(const UTransliterator *trans, char *buf, int32_t bufCapacity)_DEPRECATED_API_DO_NOT_USE
#        define utrans_unregister(const char *id) utrans_unregister(const char *id)_DEPRECATED_API_DO_NOT_USE
#    else
#        define ucol_getContractions(const UCollator *coll, USet *conts, UErrorCode *status)_4_7 ucol_getContractions(const UCollator *coll, USet *conts, UErrorCode *status)_DEPRECATED_API_DO_NOT_USE
#        define ucol_getLocale(const UCollator *coll, ULocDataLocaleType type, UErrorCode *status)_4_7 ucol_getLocale(const UCollator *coll, ULocDataLocaleType type, UErrorCode *status)_DEPRECATED_API_DO_NOT_USE
#        define ures_countArrayItems(const UResourceBundle *resourceBundle, const char *resourceKey, UErrorCode *err)_4_7 ures_countArrayItems(const UResourceBundle *resourceBundle, const char *resourceKey, UErrorCode *err)_DEPRECATED_API_DO_NOT_USE
#        define ures_getLocale(const UResourceBundle *resourceBundle, UErrorCode *status)_4_7 ures_getLocale(const UResourceBundle *resourceBundle, UErrorCode *status)_DEPRECATED_API_DO_NOT_USE
#        define ures_getVersionNumber(const UResourceBundle *resourceBundle)_4_7 ures_getVersionNumber(const UResourceBundle *resourceBundle)_DEPRECATED_API_DO_NOT_USE
#        define utrans_getAvailableID(int32_t index, char *buf, int32_t bufCapacity)_4_7 utrans_getAvailableID(int32_t index, char *buf, int32_t bufCapacity)_DEPRECATED_API_DO_NOT_USE
#        define utrans_getID(const UTransliterator *trans, char *buf, int32_t bufCapacity)_4_7 utrans_getID(const UTransliterator *trans, char *buf, int32_t bufCapacity)_DEPRECATED_API_DO_NOT_USE
#        define utrans_unregister(const char *id)_4_7 utrans_unregister(const char *id)_DEPRECATED_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_DEPRECATED_API */
#endif /* UDEPRCTD_H */

