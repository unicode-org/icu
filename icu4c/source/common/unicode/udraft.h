/*
*******************************************************************************
*   Copyright (C) 2004-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  
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

#ifndef UDRAFT_H
#define UDRAFT_H

#ifdef U_HIDE_DRAFT_API

#    if U_DISABLE_RENAMING
#        define u_compareVersions u_compareVersions_DRAFT_API_DO_NOT_USE
#        define u_strFromUTF32WithSub u_strFromUTF32WithSub_DRAFT_API_DO_NOT_USE
#        define u_strToUTF32WithSub u_strToUTF32WithSub_DRAFT_API_DO_NOT_USE
#        define u_versionFromUString u_versionFromUString_DRAFT_API_DO_NOT_USE
#        define ucal_getKeywordValuesForLocale ucal_getKeywordValuesForLocale_DRAFT_API_DO_NOT_USE
#        define ucal_getType ucal_getType_DRAFT_API_DO_NOT_USE
#        define ucnvsel_close ucnvsel_close_DRAFT_API_DO_NOT_USE
#        define ucnvsel_open ucnvsel_open_DRAFT_API_DO_NOT_USE
#        define ucnvsel_openFromSerialized ucnvsel_openFromSerialized_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForString ucnvsel_selectForString_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForUTF8 ucnvsel_selectForUTF8_DRAFT_API_DO_NOT_USE
#        define ucnvsel_serialize ucnvsel_serialize_DRAFT_API_DO_NOT_USE
#        define ucol_getKeywordValuesForLocale ucol_getKeywordValuesForLocale_DRAFT_API_DO_NOT_USE
#        define ucurr_getKeywordValuesForLocale ucurr_getKeywordValuesForLocale_DRAFT_API_DO_NOT_USE
#        define ucurr_getPluralName ucurr_getPluralName_DRAFT_API_DO_NOT_USE
#        define uloc_forLanguageTag uloc_forLanguageTag_DRAFT_API_DO_NOT_USE
#        define uloc_toLanguageTag uloc_toLanguageTag_DRAFT_API_DO_NOT_USE
#        define ulocdata_getCLDRVersion ulocdata_getCLDRVersion_DRAFT_API_DO_NOT_USE
#        define ulocdata_getLocaleDisplayPattern ulocdata_getLocaleDisplayPattern_DRAFT_API_DO_NOT_USE
#        define ulocdata_getLocaleSeparator ulocdata_getLocaleSeparator_DRAFT_API_DO_NOT_USE
#        define uset_closeOver uset_closeOver_DRAFT_API_DO_NOT_USE
#        define uset_openEmpty uset_openEmpty_DRAFT_API_DO_NOT_USE
#        define uset_removeAllStrings uset_removeAllStrings_DRAFT_API_DO_NOT_USE
#        define uspoof_areConfusable uspoof_areConfusable_DRAFT_API_DO_NOT_USE
#        define uspoof_areConfusableUTF8 uspoof_areConfusableUTF8_DRAFT_API_DO_NOT_USE
#        define uspoof_check uspoof_check_DRAFT_API_DO_NOT_USE
#        define uspoof_checkUTF8 uspoof_checkUTF8_DRAFT_API_DO_NOT_USE
#        define uspoof_clone uspoof_clone_DRAFT_API_DO_NOT_USE
#        define uspoof_close uspoof_close_DRAFT_API_DO_NOT_USE
#        define uspoof_getAllowedChars uspoof_getAllowedChars_DRAFT_API_DO_NOT_USE
#        define uspoof_getAllowedLocales uspoof_getAllowedLocales_DRAFT_API_DO_NOT_USE
#        define uspoof_getChecks uspoof_getChecks_DRAFT_API_DO_NOT_USE
#        define uspoof_getSkeleton uspoof_getSkeleton_DRAFT_API_DO_NOT_USE
#        define uspoof_getSkeletonUTF8 uspoof_getSkeletonUTF8_DRAFT_API_DO_NOT_USE
#        define uspoof_open uspoof_open_DRAFT_API_DO_NOT_USE
#        define uspoof_openFromSerialized uspoof_openFromSerialized_DRAFT_API_DO_NOT_USE
#        define uspoof_openFromSource uspoof_openFromSource_DRAFT_API_DO_NOT_USE
#        define uspoof_serialize uspoof_serialize_DRAFT_API_DO_NOT_USE
#        define uspoof_setAllowedChars uspoof_setAllowedChars_DRAFT_API_DO_NOT_USE
#        define uspoof_setAllowedLocales uspoof_setAllowedLocales_DRAFT_API_DO_NOT_USE
#        define uspoof_setChecks uspoof_setChecks_DRAFT_API_DO_NOT_USE
#        define usprep_openByType usprep_openByType_DRAFT_API_DO_NOT_USE
#    else
#        define u_compareVersions_4_2 u_compareVersions_DRAFT_API_DO_NOT_USE
#        define u_strFromUTF32WithSub_4_2 u_strFromUTF32WithSub_DRAFT_API_DO_NOT_USE
#        define u_strToUTF32WithSub_4_2 u_strToUTF32WithSub_DRAFT_API_DO_NOT_USE
#        define u_versionFromUString_4_2 u_versionFromUString_DRAFT_API_DO_NOT_USE
#        define ucal_getKeywordValuesForLocale_4_2 ucal_getKeywordValuesForLocale_DRAFT_API_DO_NOT_USE
#        define ucal_getType_4_2 ucal_getType_DRAFT_API_DO_NOT_USE
#        define ucnvsel_close_4_2 ucnvsel_close_DRAFT_API_DO_NOT_USE
#        define ucnvsel_openFromSerialized_4_2 ucnvsel_openFromSerialized_DRAFT_API_DO_NOT_USE
#        define ucnvsel_open_4_2 ucnvsel_open_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForString_4_2 ucnvsel_selectForString_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForUTF8_4_2 ucnvsel_selectForUTF8_DRAFT_API_DO_NOT_USE
#        define ucnvsel_serialize_4_2 ucnvsel_serialize_DRAFT_API_DO_NOT_USE
#        define ucol_getKeywordValuesForLocale_4_2 ucol_getKeywordValuesForLocale_DRAFT_API_DO_NOT_USE
#        define ucurr_getKeywordValuesForLocale_4_2 ucurr_getKeywordValuesForLocale_DRAFT_API_DO_NOT_USE
#        define ucurr_getPluralName_4_2 ucurr_getPluralName_DRAFT_API_DO_NOT_USE
#        define uloc_forLanguageTag_4_2 uloc_forLanguageTag_DRAFT_API_DO_NOT_USE
#        define uloc_toLanguageTag_4_2 uloc_toLanguageTag_DRAFT_API_DO_NOT_USE
#        define ulocdata_getCLDRVersion_4_2 ulocdata_getCLDRVersion_DRAFT_API_DO_NOT_USE
#        define ulocdata_getLocaleDisplayPattern_4_2 ulocdata_getLocaleDisplayPattern_DRAFT_API_DO_NOT_USE
#        define ulocdata_getLocaleSeparator_4_2 ulocdata_getLocaleSeparator_DRAFT_API_DO_NOT_USE
#        define uset_closeOver_4_2 uset_closeOver_DRAFT_API_DO_NOT_USE
#        define uset_openEmpty_4_2 uset_openEmpty_DRAFT_API_DO_NOT_USE
#        define uset_removeAllStrings_4_2 uset_removeAllStrings_DRAFT_API_DO_NOT_USE
#        define uspoof_areConfusableUTF8_4_2 uspoof_areConfusableUTF8_DRAFT_API_DO_NOT_USE
#        define uspoof_areConfusable_4_2 uspoof_areConfusable_DRAFT_API_DO_NOT_USE
#        define uspoof_checkUTF8_4_2 uspoof_checkUTF8_DRAFT_API_DO_NOT_USE
#        define uspoof_check_4_2 uspoof_check_DRAFT_API_DO_NOT_USE
#        define uspoof_clone_4_2 uspoof_clone_DRAFT_API_DO_NOT_USE
#        define uspoof_close_4_2 uspoof_close_DRAFT_API_DO_NOT_USE
#        define uspoof_getAllowedChars_4_2 uspoof_getAllowedChars_DRAFT_API_DO_NOT_USE
#        define uspoof_getAllowedLocales_4_2 uspoof_getAllowedLocales_DRAFT_API_DO_NOT_USE
#        define uspoof_getChecks_4_2 uspoof_getChecks_DRAFT_API_DO_NOT_USE
#        define uspoof_getSkeletonUTF8_4_2 uspoof_getSkeletonUTF8_DRAFT_API_DO_NOT_USE
#        define uspoof_getSkeleton_4_2 uspoof_getSkeleton_DRAFT_API_DO_NOT_USE
#        define uspoof_openFromSerialized_4_2 uspoof_openFromSerialized_DRAFT_API_DO_NOT_USE
#        define uspoof_openFromSource_4_2 uspoof_openFromSource_DRAFT_API_DO_NOT_USE
#        define uspoof_open_4_2 uspoof_open_DRAFT_API_DO_NOT_USE
#        define uspoof_serialize_4_2 uspoof_serialize_DRAFT_API_DO_NOT_USE
#        define uspoof_setAllowedChars_4_2 uspoof_setAllowedChars_DRAFT_API_DO_NOT_USE
#        define uspoof_setAllowedLocales_4_2 uspoof_setAllowedLocales_DRAFT_API_DO_NOT_USE
#        define uspoof_setChecks_4_2 uspoof_setChecks_DRAFT_API_DO_NOT_USE
#        define usprep_openByType_4_2 usprep_openByType_DRAFT_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_DRAFT_API */
#endif /* UDRAFT_H */

