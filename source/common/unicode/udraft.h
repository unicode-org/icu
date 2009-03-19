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
#        define afkLanguageCode afkLanguageCode_DRAFT_API_DO_NOT_USE
#        define armiScriptCode armiScriptCode_DRAFT_API_DO_NOT_USE
#        define u_fclose u_fclose_DRAFT_API_DO_NOT_USE
#        define u_feof u_feof_DRAFT_API_DO_NOT_USE
#        define u_fflush u_fflush_DRAFT_API_DO_NOT_USE
#        define u_fgetConverter u_fgetConverter_DRAFT_API_DO_NOT_USE
#        define u_fgetc u_fgetc_DRAFT_API_DO_NOT_USE
#        define u_fgetcodepage u_fgetcodepage_DRAFT_API_DO_NOT_USE
#        define u_fgetcx u_fgetcx_DRAFT_API_DO_NOT_USE
#        define u_fgetfile u_fgetfile_DRAFT_API_DO_NOT_USE
#        define u_fgetlocale u_fgetlocale_DRAFT_API_DO_NOT_USE
#        define u_fgets u_fgets_DRAFT_API_DO_NOT_USE
#        define u_file_read u_file_read_DRAFT_API_DO_NOT_USE
#        define u_file_write u_file_write_DRAFT_API_DO_NOT_USE
#        define u_finit u_finit_DRAFT_API_DO_NOT_USE
#        define u_fopen u_fopen_DRAFT_API_DO_NOT_USE
#        define u_fprintf u_fprintf_DRAFT_API_DO_NOT_USE
#        define u_fprintf_u u_fprintf_u_DRAFT_API_DO_NOT_USE
#        define u_fputc u_fputc_DRAFT_API_DO_NOT_USE
#        define u_fputs u_fputs_DRAFT_API_DO_NOT_USE
#        define u_frewind u_frewind_DRAFT_API_DO_NOT_USE
#        define u_fscanf u_fscanf_DRAFT_API_DO_NOT_USE
#        define u_fscanf_u u_fscanf_u_DRAFT_API_DO_NOT_USE
#        define u_fsetcodepage u_fsetcodepage_DRAFT_API_DO_NOT_USE
#        define u_fsetlocale u_fsetlocale_DRAFT_API_DO_NOT_USE
#        define u_fsettransliterator u_fsettransliterator_DRAFT_API_DO_NOT_USE
#        define u_fstropen u_fstropen_DRAFT_API_DO_NOT_USE
#        define u_fungetc u_fungetc_DRAFT_API_DO_NOT_USE
#        define u_snprintf u_snprintf_DRAFT_API_DO_NOT_USE
#        define u_snprintf_u u_snprintf_u_DRAFT_API_DO_NOT_USE
#        define u_sprintf u_sprintf_DRAFT_API_DO_NOT_USE
#        define u_sprintf_u u_sprintf_u_DRAFT_API_DO_NOT_USE
#        define u_sscanf u_sscanf_DRAFT_API_DO_NOT_USE
#        define u_sscanf_u u_sscanf_u_DRAFT_API_DO_NOT_USE
#        define u_vfprintf u_vfprintf_DRAFT_API_DO_NOT_USE
#        define u_vfprintf_u u_vfprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vfscanf u_vfscanf_DRAFT_API_DO_NOT_USE
#        define u_vfscanf_u u_vfscanf_u_DRAFT_API_DO_NOT_USE
#        define u_vsnprintf u_vsnprintf_DRAFT_API_DO_NOT_USE
#        define u_vsnprintf_u u_vsnprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vsprintf u_vsprintf_DRAFT_API_DO_NOT_USE
#        define u_vsprintf_u u_vsprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vsscanf u_vsscanf_DRAFT_API_DO_NOT_USE
#        define u_vsscanf_u u_vsscanf_u_DRAFT_API_DO_NOT_USE
#        define ucal_clone ucal_clone_DRAFT_API_DO_NOT_USE
#        define ucal_getCanonicalTimeZoneID ucal_getCanonicalTimeZoneID_DRAFT_API_DO_NOT_USE
#        define ucal_getType ucal_getType_DRAFT_API_DO_NOT_USE
#        define ucnvsel_close ucnvsel_close_DRAFT_API_DO_NOT_USE
#        define ucnvsel_open ucnvsel_open_DRAFT_API_DO_NOT_USE
#        define ucnvsel_openFromSerialized ucnvsel_openFromSerialized_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForString ucnvsel_selectForString_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForUTF8 ucnvsel_selectForUTF8_DRAFT_API_DO_NOT_USE
#        define ucnvsel_serialize ucnvsel_serialize_DRAFT_API_DO_NOT_USE
#        define ucurr_countCurrencies ucurr_countCurrencies_DRAFT_API_DO_NOT_USE
#        define ucurr_forLocaleAndDate ucurr_forLocaleAndDate_DRAFT_API_DO_NOT_USE
#        define uloc_addLikelySubtags uloc_addLikelySubtags_DRAFT_API_DO_NOT_USE
#        define uloc_getCharacterOrientation uloc_getCharacterOrientation_DRAFT_API_DO_NOT_USE
#        define uloc_getLineOrientation uloc_getLineOrientation_DRAFT_API_DO_NOT_USE
#        define uloc_minimizeSubtags uloc_minimizeSubtags_DRAFT_API_DO_NOT_USE
#        define uregex_getMatchCallback uregex_getMatchCallback_DRAFT_API_DO_NOT_USE
#        define uregex_getStackLimit uregex_getStackLimit_DRAFT_API_DO_NOT_USE
#        define uregex_getTimeLimit uregex_getTimeLimit_DRAFT_API_DO_NOT_USE
#        define uregex_hasAnchoringBounds uregex_hasAnchoringBounds_DRAFT_API_DO_NOT_USE
#        define uregex_hasTransparentBounds uregex_hasTransparentBounds_DRAFT_API_DO_NOT_USE
#        define uregex_hitEnd uregex_hitEnd_DRAFT_API_DO_NOT_USE
#        define uregex_regionEnd uregex_regionEnd_DRAFT_API_DO_NOT_USE
#        define uregex_regionStart uregex_regionStart_DRAFT_API_DO_NOT_USE
#        define uregex_requireEnd uregex_requireEnd_DRAFT_API_DO_NOT_USE
#        define uregex_setMatchCallback uregex_setMatchCallback_DRAFT_API_DO_NOT_USE
#        define uregex_setRegion uregex_setRegion_DRAFT_API_DO_NOT_USE
#        define uregex_setStackLimit uregex_setStackLimit_DRAFT_API_DO_NOT_USE
#        define uregex_setTimeLimit uregex_setTimeLimit_DRAFT_API_DO_NOT_USE
#        define uregex_useAnchoringBounds uregex_useAnchoringBounds_DRAFT_API_DO_NOT_USE
#        define uregex_useTransparentBounds uregex_useTransparentBounds_DRAFT_API_DO_NOT_USE
#    else
#        define afkLanguageCode_4_1.2 afkLanguageCode_DRAFT_API_DO_NOT_USE
#        define armiScriptCode_4_1.2 armiScriptCode_DRAFT_API_DO_NOT_USE
#        define u_fclose_4_1.2 u_fclose_DRAFT_API_DO_NOT_USE
#        define u_feof_4_1.2 u_feof_DRAFT_API_DO_NOT_USE
#        define u_fflush_4_1.2 u_fflush_DRAFT_API_DO_NOT_USE
#        define u_fgetConverter_4_1.2 u_fgetConverter_DRAFT_API_DO_NOT_USE
#        define u_fgetc_4_1.2 u_fgetc_DRAFT_API_DO_NOT_USE
#        define u_fgetcodepage_4_1.2 u_fgetcodepage_DRAFT_API_DO_NOT_USE
#        define u_fgetcx_4_1.2 u_fgetcx_DRAFT_API_DO_NOT_USE
#        define u_fgetfile_4_1.2 u_fgetfile_DRAFT_API_DO_NOT_USE
#        define u_fgetlocale_4_1.2 u_fgetlocale_DRAFT_API_DO_NOT_USE
#        define u_fgets_4_1.2 u_fgets_DRAFT_API_DO_NOT_USE
#        define u_file_read_4_1.2 u_file_read_DRAFT_API_DO_NOT_USE
#        define u_file_write_4_1.2 u_file_write_DRAFT_API_DO_NOT_USE
#        define u_finit_4_1.2 u_finit_DRAFT_API_DO_NOT_USE
#        define u_fopen_4_1.2 u_fopen_DRAFT_API_DO_NOT_USE
#        define u_fprintf_4_1.2 u_fprintf_DRAFT_API_DO_NOT_USE
#        define u_fprintf_u_4_1.2 u_fprintf_u_DRAFT_API_DO_NOT_USE
#        define u_fputc_4_1.2 u_fputc_DRAFT_API_DO_NOT_USE
#        define u_fputs_4_1.2 u_fputs_DRAFT_API_DO_NOT_USE
#        define u_frewind_4_1.2 u_frewind_DRAFT_API_DO_NOT_USE
#        define u_fscanf_4_1.2 u_fscanf_DRAFT_API_DO_NOT_USE
#        define u_fscanf_u_4_1.2 u_fscanf_u_DRAFT_API_DO_NOT_USE
#        define u_fsetcodepage_4_1.2 u_fsetcodepage_DRAFT_API_DO_NOT_USE
#        define u_fsetlocale_4_1.2 u_fsetlocale_DRAFT_API_DO_NOT_USE
#        define u_fsettransliterator_4_1.2 u_fsettransliterator_DRAFT_API_DO_NOT_USE
#        define u_fstropen_4_1.2 u_fstropen_DRAFT_API_DO_NOT_USE
#        define u_fungetc_4_1.2 u_fungetc_DRAFT_API_DO_NOT_USE
#        define u_snprintf_4_1.2 u_snprintf_DRAFT_API_DO_NOT_USE
#        define u_snprintf_u_4_1.2 u_snprintf_u_DRAFT_API_DO_NOT_USE
#        define u_sprintf_4_1.2 u_sprintf_DRAFT_API_DO_NOT_USE
#        define u_sprintf_u_4_1.2 u_sprintf_u_DRAFT_API_DO_NOT_USE
#        define u_sscanf_4_1.2 u_sscanf_DRAFT_API_DO_NOT_USE
#        define u_sscanf_u_4_1.2 u_sscanf_u_DRAFT_API_DO_NOT_USE
#        define u_vfprintf_4_1.2 u_vfprintf_DRAFT_API_DO_NOT_USE
#        define u_vfprintf_u_4_1.2 u_vfprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vfscanf_4_1.2 u_vfscanf_DRAFT_API_DO_NOT_USE
#        define u_vfscanf_u_4_1.2 u_vfscanf_u_DRAFT_API_DO_NOT_USE
#        define u_vsnprintf_4_1.2 u_vsnprintf_DRAFT_API_DO_NOT_USE
#        define u_vsnprintf_u_4_1.2 u_vsnprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vsprintf_4_1.2 u_vsprintf_DRAFT_API_DO_NOT_USE
#        define u_vsprintf_u_4_1.2 u_vsprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vsscanf_4_1.2 u_vsscanf_DRAFT_API_DO_NOT_USE
#        define u_vsscanf_u_4_1.2 u_vsscanf_u_DRAFT_API_DO_NOT_USE
#        define ucal_clone_4_1.2 ucal_clone_DRAFT_API_DO_NOT_USE
#        define ucal_getCanonicalTimeZoneID_4_1.2 ucal_getCanonicalTimeZoneID_DRAFT_API_DO_NOT_USE
#        define ucal_getType_4_1.2 ucal_getType_DRAFT_API_DO_NOT_USE
#        define ucnvsel_close_4_1.2 ucnvsel_close_DRAFT_API_DO_NOT_USE
#        define ucnvsel_openFromSerialized_4_1.2 ucnvsel_openFromSerialized_DRAFT_API_DO_NOT_USE
#        define ucnvsel_open_4_1.2 ucnvsel_open_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForString_4_1.2 ucnvsel_selectForString_DRAFT_API_DO_NOT_USE
#        define ucnvsel_selectForUTF8_4_1.2 ucnvsel_selectForUTF8_DRAFT_API_DO_NOT_USE
#        define ucnvsel_serialize_4_1.2 ucnvsel_serialize_DRAFT_API_DO_NOT_USE
#        define ucurr_countCurrencies_4_1.2 ucurr_countCurrencies_DRAFT_API_DO_NOT_USE
#        define ucurr_forLocaleAndDate_4_1.2 ucurr_forLocaleAndDate_DRAFT_API_DO_NOT_USE
#        define uloc_addLikelySubtags_4_1.2 uloc_addLikelySubtags_DRAFT_API_DO_NOT_USE
#        define uloc_getCharacterOrientation_4_1.2 uloc_getCharacterOrientation_DRAFT_API_DO_NOT_USE
#        define uloc_getLineOrientation_4_1.2 uloc_getLineOrientation_DRAFT_API_DO_NOT_USE
#        define uloc_minimizeSubtags_4_1.2 uloc_minimizeSubtags_DRAFT_API_DO_NOT_USE
#        define uregex_getMatchCallback_4_1.2 uregex_getMatchCallback_DRAFT_API_DO_NOT_USE
#        define uregex_getStackLimit_4_1.2 uregex_getStackLimit_DRAFT_API_DO_NOT_USE
#        define uregex_getTimeLimit_4_1.2 uregex_getTimeLimit_DRAFT_API_DO_NOT_USE
#        define uregex_hasAnchoringBounds_4_1.2 uregex_hasAnchoringBounds_DRAFT_API_DO_NOT_USE
#        define uregex_hasTransparentBounds_4_1.2 uregex_hasTransparentBounds_DRAFT_API_DO_NOT_USE
#        define uregex_hitEnd_4_1.2 uregex_hitEnd_DRAFT_API_DO_NOT_USE
#        define uregex_regionEnd_4_1.2 uregex_regionEnd_DRAFT_API_DO_NOT_USE
#        define uregex_regionStart_4_1.2 uregex_regionStart_DRAFT_API_DO_NOT_USE
#        define uregex_requireEnd_4_1.2 uregex_requireEnd_DRAFT_API_DO_NOT_USE
#        define uregex_setMatchCallback_4_1.2 uregex_setMatchCallback_DRAFT_API_DO_NOT_USE
#        define uregex_setRegion_4_1.2 uregex_setRegion_DRAFT_API_DO_NOT_USE
#        define uregex_setStackLimit_4_1.2 uregex_setStackLimit_DRAFT_API_DO_NOT_USE
#        define uregex_setTimeLimit_4_1.2 uregex_setTimeLimit_DRAFT_API_DO_NOT_USE
#        define uregex_useAnchoringBounds_4_1.2 uregex_useAnchoringBounds_DRAFT_API_DO_NOT_USE
#        define uregex_useTransparentBounds_4_1.2 uregex_useTransparentBounds_DRAFT_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_DRAFT_API */
#endif /* UDRAFT_H */

