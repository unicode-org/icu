/*
*******************************************************************************
*   Copyright (C) 2004-2006, International Business Machines
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

#ifndef UINTRNAL_H
#define UINTRNAL_H

#ifdef U_HIDE_INTERNAL_API

#    if U_DISABLE_RENAMING
#        define RegexPatternDump RegexPatternDump_INTERNAL_API_DO_NOT_USE
#        define ucol_collatorToIdentifier ucol_collatorToIdentifier_INTERNAL_API_DO_NOT_USE
#        define ucol_equals ucol_equals_INTERNAL_API_DO_NOT_USE
#        define ucol_forgetUCA ucol_forgetUCA_INTERNAL_API_DO_NOT_USE
#        define ucol_getAttributeOrDefault ucol_getAttributeOrDefault_INTERNAL_API_DO_NOT_USE
#        define ucol_getUnsafeSet ucol_getUnsafeSet_INTERNAL_API_DO_NOT_USE
#        define ucol_identifierToShortString ucol_identifierToShortString_INTERNAL_API_DO_NOT_USE
#        define ucol_openFromIdentifier ucol_openFromIdentifier_INTERNAL_API_DO_NOT_USE
#        define ucol_prepareShortStringOpen ucol_prepareShortStringOpen_INTERNAL_API_DO_NOT_USE
#        define ucol_shortStringToIdentifier ucol_shortStringToIdentifier_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultCodepage uprv_getDefaultCodepage_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultLocaleID uprv_getDefaultLocaleID_INTERNAL_API_DO_NOT_USE
#        define ures_openFillIn ures_openFillIn_INTERNAL_API_DO_NOT_USE
#        define utf8_appendCharSafeBody utf8_appendCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_back1SafeBody utf8_back1SafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_countTrailBytes utf8_countTrailBytes_INTERNAL_API_DO_NOT_USE
#        define utf8_nextCharSafeBody utf8_nextCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_prevCharSafeBody utf8_prevCharSafeBody_INTERNAL_API_DO_NOT_USE
#    else
#        define RegexPatternDump_3_6 RegexPatternDump_INTERNAL_API_DO_NOT_USE
#        define ucol_collatorToIdentifier_3_6 ucol_collatorToIdentifier_INTERNAL_API_DO_NOT_USE
#        define ucol_equals_3_6 ucol_equals_INTERNAL_API_DO_NOT_USE
#        define ucol_forgetUCA_3_6 ucol_forgetUCA_INTERNAL_API_DO_NOT_USE
#        define ucol_getAttributeOrDefault_3_6 ucol_getAttributeOrDefault_INTERNAL_API_DO_NOT_USE
#        define ucol_getUnsafeSet_3_6 ucol_getUnsafeSet_INTERNAL_API_DO_NOT_USE
#        define ucol_identifierToShortString_3_6 ucol_identifierToShortString_INTERNAL_API_DO_NOT_USE
#        define ucol_openFromIdentifier_3_6 ucol_openFromIdentifier_INTERNAL_API_DO_NOT_USE
#        define ucol_prepareShortStringOpen_3_6 ucol_prepareShortStringOpen_INTERNAL_API_DO_NOT_USE
#        define ucol_shortStringToIdentifier_3_6 ucol_shortStringToIdentifier_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultCodepage_3_6 uprv_getDefaultCodepage_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultLocaleID_3_6 uprv_getDefaultLocaleID_INTERNAL_API_DO_NOT_USE
#        define ures_openFillIn_3_6 ures_openFillIn_INTERNAL_API_DO_NOT_USE
#        define utf8_appendCharSafeBody_3_6 utf8_appendCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_back1SafeBody_3_6 utf8_back1SafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_countTrailBytes_3_6 utf8_countTrailBytes_INTERNAL_API_DO_NOT_USE
#        define utf8_nextCharSafeBody_3_6 utf8_nextCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_prevCharSafeBody_3_6 utf8_prevCharSafeBody_INTERNAL_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_INTERNAL_API */
#endif /* UINTRNAL_H */

