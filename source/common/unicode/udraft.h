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

#ifndef UDRAFT_H
#define UDRAFT_H

#ifdef U_HIDE_DRAFT_API

#    if U_DISABLE_RENAMING
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
#        define u_strFromUTF8Lenient u_strFromUTF8Lenient_DRAFT_API_DO_NOT_USE
#        define u_strFromUTF8WithSub u_strFromUTF8WithSub_DRAFT_API_DO_NOT_USE
#        define u_strToUTF8WithSub u_strToUTF8WithSub_DRAFT_API_DO_NOT_USE
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
#        define ubidi_getProcessedLength ubidi_getProcessedLength_DRAFT_API_DO_NOT_USE
#        define ubidi_getReorderingMode ubidi_getReorderingMode_DRAFT_API_DO_NOT_USE
#        define ubidi_getReorderingOptions ubidi_getReorderingOptions_DRAFT_API_DO_NOT_USE
#        define ubidi_getResultLength ubidi_getResultLength_DRAFT_API_DO_NOT_USE
#        define ubidi_setReorderingMode ubidi_setReorderingMode_DRAFT_API_DO_NOT_USE
#        define ubidi_setReorderingOptions ubidi_setReorderingOptions_DRAFT_API_DO_NOT_USE
#        define ubrk_setUText ubrk_setUText_DRAFT_API_DO_NOT_USE
#        define ucal_getGregorianChange ucal_getGregorianChange_DRAFT_API_DO_NOT_USE
#        define ucal_setGregorianChange ucal_setGregorianChange_DRAFT_API_DO_NOT_USE
#        define ucasemap_close ucasemap_close_DRAFT_API_DO_NOT_USE
#        define ucasemap_getLocale ucasemap_getLocale_DRAFT_API_DO_NOT_USE
#        define ucasemap_getOptions ucasemap_getOptions_DRAFT_API_DO_NOT_USE
#        define ucasemap_open ucasemap_open_DRAFT_API_DO_NOT_USE
#        define ucasemap_setLocale ucasemap_setLocale_DRAFT_API_DO_NOT_USE
#        define ucasemap_setOptions ucasemap_setOptions_DRAFT_API_DO_NOT_USE
#        define ucasemap_utf8ToLower ucasemap_utf8ToLower_DRAFT_API_DO_NOT_USE
#        define ucasemap_utf8ToUpper ucasemap_utf8ToUpper_DRAFT_API_DO_NOT_USE
#        define ucnv_fromUCountPending ucnv_fromUCountPending_DRAFT_API_DO_NOT_USE
#        define ucnv_setSubstString ucnv_setSubstString_DRAFT_API_DO_NOT_USE
#        define ucnv_toUCountPending ucnv_toUCountPending_DRAFT_API_DO_NOT_USE
#        define ucol_getContractionsAndExpansions ucol_getContractionsAndExpansions_DRAFT_API_DO_NOT_USE
#        define ucsdet_close ucsdet_close_DRAFT_API_DO_NOT_USE
#        define ucsdet_detect ucsdet_detect_DRAFT_API_DO_NOT_USE
#        define ucsdet_detectAll ucsdet_detectAll_DRAFT_API_DO_NOT_USE
#        define ucsdet_enableInputFilter ucsdet_enableInputFilter_DRAFT_API_DO_NOT_USE
#        define ucsdet_getAllDetectableCharsets ucsdet_getAllDetectableCharsets_DRAFT_API_DO_NOT_USE
#        define ucsdet_getConfidence ucsdet_getConfidence_DRAFT_API_DO_NOT_USE
#        define ucsdet_getLanguage ucsdet_getLanguage_DRAFT_API_DO_NOT_USE
#        define ucsdet_getName ucsdet_getName_DRAFT_API_DO_NOT_USE
#        define ucsdet_getUChars ucsdet_getUChars_DRAFT_API_DO_NOT_USE
#        define ucsdet_isInputFilterEnabled ucsdet_isInputFilterEnabled_DRAFT_API_DO_NOT_USE
#        define ucsdet_open ucsdet_open_DRAFT_API_DO_NOT_USE
#        define ucsdet_setDeclaredEncoding ucsdet_setDeclaredEncoding_DRAFT_API_DO_NOT_USE
#        define ucsdet_setText ucsdet_setText_DRAFT_API_DO_NOT_USE
#        define udata_setFileAccess udata_setFileAccess_DRAFT_API_DO_NOT_USE
#        define ulocdata_close ulocdata_close_DRAFT_API_DO_NOT_USE
#        define ulocdata_getDelimiter ulocdata_getDelimiter_DRAFT_API_DO_NOT_USE
#        define ulocdata_getExemplarSet ulocdata_getExemplarSet_DRAFT_API_DO_NOT_USE
#        define ulocdata_getNoSubstitute ulocdata_getNoSubstitute_DRAFT_API_DO_NOT_USE
#        define ulocdata_open ulocdata_open_DRAFT_API_DO_NOT_USE
#        define ulocdata_setNoSubstitute ulocdata_setNoSubstitute_DRAFT_API_DO_NOT_USE
#        define ures_getUTF8String ures_getUTF8String_DRAFT_API_DO_NOT_USE
#        define ures_getUTF8StringByIndex ures_getUTF8StringByIndex_DRAFT_API_DO_NOT_USE
#        define ures_getUTF8StringByKey ures_getUTF8StringByKey_DRAFT_API_DO_NOT_USE
#        define uset_addAllCodePoints uset_addAllCodePoints_DRAFT_API_DO_NOT_USE
#        define uset_containsAllCodePoints uset_containsAllCodePoints_DRAFT_API_DO_NOT_USE
#        define utext_char32At utext_char32At_DRAFT_API_DO_NOT_USE
#        define utext_clone utext_clone_DRAFT_API_DO_NOT_USE
#        define utext_close utext_close_DRAFT_API_DO_NOT_USE
#        define utext_copy utext_copy_DRAFT_API_DO_NOT_USE
#        define utext_current32 utext_current32_DRAFT_API_DO_NOT_USE
#        define utext_equals utext_equals_DRAFT_API_DO_NOT_USE
#        define utext_extract utext_extract_DRAFT_API_DO_NOT_USE
#        define utext_freeze utext_freeze_DRAFT_API_DO_NOT_USE
#        define utext_getNativeIndex utext_getNativeIndex_DRAFT_API_DO_NOT_USE
#        define utext_getPreviousNativeIndex utext_getPreviousNativeIndex_DRAFT_API_DO_NOT_USE
#        define utext_hasMetaData utext_hasMetaData_DRAFT_API_DO_NOT_USE
#        define utext_isLengthExpensive utext_isLengthExpensive_DRAFT_API_DO_NOT_USE
#        define utext_isWritable utext_isWritable_DRAFT_API_DO_NOT_USE
#        define utext_moveIndex32 utext_moveIndex32_DRAFT_API_DO_NOT_USE
#        define utext_nativeLength utext_nativeLength_DRAFT_API_DO_NOT_USE
#        define utext_next32 utext_next32_DRAFT_API_DO_NOT_USE
#        define utext_next32From utext_next32From_DRAFT_API_DO_NOT_USE
#        define utext_openUChars utext_openUChars_DRAFT_API_DO_NOT_USE
#        define utext_openUTF8 utext_openUTF8_DRAFT_API_DO_NOT_USE
#        define utext_previous32 utext_previous32_DRAFT_API_DO_NOT_USE
#        define utext_previous32From utext_previous32From_DRAFT_API_DO_NOT_USE
#        define utext_replace utext_replace_DRAFT_API_DO_NOT_USE
#        define utext_setNativeIndex utext_setNativeIndex_DRAFT_API_DO_NOT_USE
#        define utext_setup utext_setup_DRAFT_API_DO_NOT_USE
#    else
#        define u_fclose_3_6 u_fclose_DRAFT_API_DO_NOT_USE
#        define u_feof_3_6 u_feof_DRAFT_API_DO_NOT_USE
#        define u_fflush_3_6 u_fflush_DRAFT_API_DO_NOT_USE
#        define u_fgetConverter_3_6 u_fgetConverter_DRAFT_API_DO_NOT_USE
#        define u_fgetc_3_6 u_fgetc_DRAFT_API_DO_NOT_USE
#        define u_fgetcodepage_3_6 u_fgetcodepage_DRAFT_API_DO_NOT_USE
#        define u_fgetcx_3_6 u_fgetcx_DRAFT_API_DO_NOT_USE
#        define u_fgetfile_3_6 u_fgetfile_DRAFT_API_DO_NOT_USE
#        define u_fgetlocale_3_6 u_fgetlocale_DRAFT_API_DO_NOT_USE
#        define u_fgets_3_6 u_fgets_DRAFT_API_DO_NOT_USE
#        define u_file_read_3_6 u_file_read_DRAFT_API_DO_NOT_USE
#        define u_file_write_3_6 u_file_write_DRAFT_API_DO_NOT_USE
#        define u_finit_3_6 u_finit_DRAFT_API_DO_NOT_USE
#        define u_fopen_3_6 u_fopen_DRAFT_API_DO_NOT_USE
#        define u_fprintf_3_6 u_fprintf_DRAFT_API_DO_NOT_USE
#        define u_fprintf_u_3_6 u_fprintf_u_DRAFT_API_DO_NOT_USE
#        define u_fputc_3_6 u_fputc_DRAFT_API_DO_NOT_USE
#        define u_fputs_3_6 u_fputs_DRAFT_API_DO_NOT_USE
#        define u_frewind_3_6 u_frewind_DRAFT_API_DO_NOT_USE
#        define u_fscanf_3_6 u_fscanf_DRAFT_API_DO_NOT_USE
#        define u_fscanf_u_3_6 u_fscanf_u_DRAFT_API_DO_NOT_USE
#        define u_fsetcodepage_3_6 u_fsetcodepage_DRAFT_API_DO_NOT_USE
#        define u_fsetlocale_3_6 u_fsetlocale_DRAFT_API_DO_NOT_USE
#        define u_fsettransliterator_3_6 u_fsettransliterator_DRAFT_API_DO_NOT_USE
#        define u_fstropen_3_6 u_fstropen_DRAFT_API_DO_NOT_USE
#        define u_fungetc_3_6 u_fungetc_DRAFT_API_DO_NOT_USE
#        define u_snprintf_3_6 u_snprintf_DRAFT_API_DO_NOT_USE
#        define u_snprintf_u_3_6 u_snprintf_u_DRAFT_API_DO_NOT_USE
#        define u_sprintf_3_6 u_sprintf_DRAFT_API_DO_NOT_USE
#        define u_sprintf_u_3_6 u_sprintf_u_DRAFT_API_DO_NOT_USE
#        define u_sscanf_3_6 u_sscanf_DRAFT_API_DO_NOT_USE
#        define u_sscanf_u_3_6 u_sscanf_u_DRAFT_API_DO_NOT_USE
#        define u_strFromUTF8Lenient_3_6 u_strFromUTF8Lenient_DRAFT_API_DO_NOT_USE
#        define u_strFromUTF8WithSub_3_6 u_strFromUTF8WithSub_DRAFT_API_DO_NOT_USE
#        define u_strToUTF8WithSub_3_6 u_strToUTF8WithSub_DRAFT_API_DO_NOT_USE
#        define u_vfprintf_3_6 u_vfprintf_DRAFT_API_DO_NOT_USE
#        define u_vfprintf_u_3_6 u_vfprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vfscanf_3_6 u_vfscanf_DRAFT_API_DO_NOT_USE
#        define u_vfscanf_u_3_6 u_vfscanf_u_DRAFT_API_DO_NOT_USE
#        define u_vsnprintf_3_6 u_vsnprintf_DRAFT_API_DO_NOT_USE
#        define u_vsnprintf_u_3_6 u_vsnprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vsprintf_3_6 u_vsprintf_DRAFT_API_DO_NOT_USE
#        define u_vsprintf_u_3_6 u_vsprintf_u_DRAFT_API_DO_NOT_USE
#        define u_vsscanf_3_6 u_vsscanf_DRAFT_API_DO_NOT_USE
#        define u_vsscanf_u_3_6 u_vsscanf_u_DRAFT_API_DO_NOT_USE
#        define ubidi_getProcessedLength_3_6 ubidi_getProcessedLength_DRAFT_API_DO_NOT_USE
#        define ubidi_getReorderingMode_3_6 ubidi_getReorderingMode_DRAFT_API_DO_NOT_USE
#        define ubidi_getReorderingOptions_3_6 ubidi_getReorderingOptions_DRAFT_API_DO_NOT_USE
#        define ubidi_getResultLength_3_6 ubidi_getResultLength_DRAFT_API_DO_NOT_USE
#        define ubidi_setReorderingMode_3_6 ubidi_setReorderingMode_DRAFT_API_DO_NOT_USE
#        define ubidi_setReorderingOptions_3_6 ubidi_setReorderingOptions_DRAFT_API_DO_NOT_USE
#        define ubrk_setUText_3_6 ubrk_setUText_DRAFT_API_DO_NOT_USE
#        define ucal_getGregorianChange_3_6 ucal_getGregorianChange_DRAFT_API_DO_NOT_USE
#        define ucal_setGregorianChange_3_6 ucal_setGregorianChange_DRAFT_API_DO_NOT_USE
#        define ucasemap_close_3_6 ucasemap_close_DRAFT_API_DO_NOT_USE
#        define ucasemap_getLocale_3_6 ucasemap_getLocale_DRAFT_API_DO_NOT_USE
#        define ucasemap_getOptions_3_6 ucasemap_getOptions_DRAFT_API_DO_NOT_USE
#        define ucasemap_open_3_6 ucasemap_open_DRAFT_API_DO_NOT_USE
#        define ucasemap_setLocale_3_6 ucasemap_setLocale_DRAFT_API_DO_NOT_USE
#        define ucasemap_setOptions_3_6 ucasemap_setOptions_DRAFT_API_DO_NOT_USE
#        define ucasemap_utf8ToLower_3_6 ucasemap_utf8ToLower_DRAFT_API_DO_NOT_USE
#        define ucasemap_utf8ToUpper_3_6 ucasemap_utf8ToUpper_DRAFT_API_DO_NOT_USE
#        define ucnv_fromUCountPending_3_6 ucnv_fromUCountPending_DRAFT_API_DO_NOT_USE
#        define ucnv_setSubstString_3_6 ucnv_setSubstString_DRAFT_API_DO_NOT_USE
#        define ucnv_toUCountPending_3_6 ucnv_toUCountPending_DRAFT_API_DO_NOT_USE
#        define ucol_getContractionsAndExpansions_3_6 ucol_getContractionsAndExpansions_DRAFT_API_DO_NOT_USE
#        define ucsdet_close_3_6 ucsdet_close_DRAFT_API_DO_NOT_USE
#        define ucsdet_detectAll_3_6 ucsdet_detectAll_DRAFT_API_DO_NOT_USE
#        define ucsdet_detect_3_6 ucsdet_detect_DRAFT_API_DO_NOT_USE
#        define ucsdet_enableInputFilter_3_6 ucsdet_enableInputFilter_DRAFT_API_DO_NOT_USE
#        define ucsdet_getAllDetectableCharsets_3_6 ucsdet_getAllDetectableCharsets_DRAFT_API_DO_NOT_USE
#        define ucsdet_getConfidence_3_6 ucsdet_getConfidence_DRAFT_API_DO_NOT_USE
#        define ucsdet_getLanguage_3_6 ucsdet_getLanguage_DRAFT_API_DO_NOT_USE
#        define ucsdet_getName_3_6 ucsdet_getName_DRAFT_API_DO_NOT_USE
#        define ucsdet_getUChars_3_6 ucsdet_getUChars_DRAFT_API_DO_NOT_USE
#        define ucsdet_isInputFilterEnabled_3_6 ucsdet_isInputFilterEnabled_DRAFT_API_DO_NOT_USE
#        define ucsdet_open_3_6 ucsdet_open_DRAFT_API_DO_NOT_USE
#        define ucsdet_setDeclaredEncoding_3_6 ucsdet_setDeclaredEncoding_DRAFT_API_DO_NOT_USE
#        define ucsdet_setText_3_6 ucsdet_setText_DRAFT_API_DO_NOT_USE
#        define udata_setFileAccess_3_6 udata_setFileAccess_DRAFT_API_DO_NOT_USE
#        define ulocdata_close_3_6 ulocdata_close_DRAFT_API_DO_NOT_USE
#        define ulocdata_getDelimiter_3_6 ulocdata_getDelimiter_DRAFT_API_DO_NOT_USE
#        define ulocdata_getExemplarSet_3_6 ulocdata_getExemplarSet_DRAFT_API_DO_NOT_USE
#        define ulocdata_getNoSubstitute_3_6 ulocdata_getNoSubstitute_DRAFT_API_DO_NOT_USE
#        define ulocdata_open_3_6 ulocdata_open_DRAFT_API_DO_NOT_USE
#        define ulocdata_setNoSubstitute_3_6 ulocdata_setNoSubstitute_DRAFT_API_DO_NOT_USE
#        define ures_getUTF8StringByIndex_3_6 ures_getUTF8StringByIndex_DRAFT_API_DO_NOT_USE
#        define ures_getUTF8StringByKey_3_6 ures_getUTF8StringByKey_DRAFT_API_DO_NOT_USE
#        define ures_getUTF8String_3_6 ures_getUTF8String_DRAFT_API_DO_NOT_USE
#        define uset_addAllCodePoints_3_6 uset_addAllCodePoints_DRAFT_API_DO_NOT_USE
#        define uset_containsAllCodePoints_3_6 uset_containsAllCodePoints_DRAFT_API_DO_NOT_USE
#        define utext_char32At_3_6 utext_char32At_DRAFT_API_DO_NOT_USE
#        define utext_clone_3_6 utext_clone_DRAFT_API_DO_NOT_USE
#        define utext_close_3_6 utext_close_DRAFT_API_DO_NOT_USE
#        define utext_copy_3_6 utext_copy_DRAFT_API_DO_NOT_USE
#        define utext_current32_3_6 utext_current32_DRAFT_API_DO_NOT_USE
#        define utext_equals_3_6 utext_equals_DRAFT_API_DO_NOT_USE
#        define utext_extract_3_6 utext_extract_DRAFT_API_DO_NOT_USE
#        define utext_freeze_3_6 utext_freeze_DRAFT_API_DO_NOT_USE
#        define utext_getNativeIndex_3_6 utext_getNativeIndex_DRAFT_API_DO_NOT_USE
#        define utext_getPreviousNativeIndex_3_6 utext_getPreviousNativeIndex_DRAFT_API_DO_NOT_USE
#        define utext_hasMetaData_3_6 utext_hasMetaData_DRAFT_API_DO_NOT_USE
#        define utext_isLengthExpensive_3_6 utext_isLengthExpensive_DRAFT_API_DO_NOT_USE
#        define utext_isWritable_3_6 utext_isWritable_DRAFT_API_DO_NOT_USE
#        define utext_moveIndex32_3_6 utext_moveIndex32_DRAFT_API_DO_NOT_USE
#        define utext_nativeLength_3_6 utext_nativeLength_DRAFT_API_DO_NOT_USE
#        define utext_next32From_3_6 utext_next32From_DRAFT_API_DO_NOT_USE
#        define utext_next32_3_6 utext_next32_DRAFT_API_DO_NOT_USE
#        define utext_openUChars_3_6 utext_openUChars_DRAFT_API_DO_NOT_USE
#        define utext_openUTF8_3_6 utext_openUTF8_DRAFT_API_DO_NOT_USE
#        define utext_previous32From_3_6 utext_previous32From_DRAFT_API_DO_NOT_USE
#        define utext_previous32_3_6 utext_previous32_DRAFT_API_DO_NOT_USE
#        define utext_replace_3_6 utext_replace_DRAFT_API_DO_NOT_USE
#        define utext_setNativeIndex_3_6 utext_setNativeIndex_DRAFT_API_DO_NOT_USE
#        define utext_setup_3_6 utext_setup_DRAFT_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_DRAFT_API */
#endif /* UDRAFT_H */

