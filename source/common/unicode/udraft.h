/*
*******************************************************************************
*   Copyright (C) 2004-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  udraft.h
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
#        define StringPiece&amp;y) StringPiece&amp;y)_DRAFT_API_DO_NOT_USE
#        define ubidi_getBaseDirection(const UChar *text, int32_t length) ubidi_getBaseDirection(const UChar *text, int32_t length)_DRAFT_API_DO_NOT_USE
#        define ucal_openTimeZoneIDEnumeration(USystemTimeZoneType zoneType, const char *region, const int32_t *rawOffset, UErrorCode *ec) ucal_openTimeZoneIDEnumeration(USystemTimeZoneType zoneType, const char *region, const int32_t *rawOffset, UErrorCode *ec)_DRAFT_API_DO_NOT_USE
#        define ucnv_isFixedWidth(UConverter *cnv, UErrorCode *status) ucnv_isFixedWidth(UConverter *cnv, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uidna_close(UIDNA *idna) uidna_close(UIDNA *idna)_DRAFT_API_DO_NOT_USE
#        define uidna_openUTS46(uint32_t options, UErrorCode *pErrorCode) uidna_openUTS46(uint32_t options, UErrorCode *pErrorCode)_DRAFT_API_DO_NOT_USE
#        define uloc_forLanguageTag(const char *langtag, char *localeID, int32_t localeIDCapacity, int32_t *parsedLength, UErrorCode *err) uloc_forLanguageTag(const char *langtag, char *localeID, int32_t localeIDCapacity, int32_t *parsedLength, UErrorCode *err)_DRAFT_API_DO_NOT_USE
#        define uloc_toLanguageTag(const char *localeID, char *langtag, int32_t langtagCapacity, UBool strict, UErrorCode *err) uloc_toLanguageTag(const char *localeID, char *langtag, int32_t langtagCapacity, UBool strict, UErrorCode *err)_DRAFT_API_DO_NOT_USE
#        define unorm2_getDecomposition(const UNormalizer2 *norm2, UChar32 c, UChar *decomposition, int32_t capacity, UErrorCode *pErrorCode) unorm2_getDecomposition(const UNormalizer2 *norm2, UChar32 c, UChar *decomposition, int32_t capacity, UErrorCode *pErrorCode)_DRAFT_API_DO_NOT_USE
#        define uregex_end64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status) uregex_end64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_find64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status) uregex_find64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_getFindProgressCallback(const URegularExpression *regexp, URegexFindProgressCallback **callback, const void **context, UErrorCode *status) uregex_getFindProgressCallback(const URegularExpression *regexp, URegexFindProgressCallback **callback, const void **context, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_lookingAt64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status) uregex_lookingAt64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_matches64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status) uregex_matches64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_patternUText(const URegularExpression *regexp, UErrorCode *status) uregex_patternUText(const URegularExpression *regexp, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_regionEnd64(const URegularExpression *regexp, UErrorCode *status) uregex_regionEnd64(const URegularExpression *regexp, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_regionStart64(const URegularExpression *regexp, UErrorCode *status) uregex_regionStart64(const URegularExpression *regexp, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_reset64(URegularExpression *regexp, int64_t index, UErrorCode *status) uregex_reset64(URegularExpression *regexp, int64_t index, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_setFindProgressCallback(URegularExpression *regexp, URegexFindProgressCallback *callback, const void *context, UErrorCode *status) uregex_setFindProgressCallback(URegularExpression *regexp, URegexFindProgressCallback *callback, const void *context, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_setRegion64(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, UErrorCode *status) uregex_setRegion64(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_setRegionAndStart(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, int64_t startIndex, UErrorCode *status) uregex_setRegionAndStart(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_start64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status) uregex_start64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uscript_getScriptExtensions(UChar32 c, UScriptCode *scripts, int32_t capacity, UErrorCode *pErrorCode) uscript_getScriptExtensions(UChar32 c, UScriptCode *scripts, int32_t capacity, UErrorCode *pErrorCode)_DRAFT_API_DO_NOT_USE
#        define uscript_hasScript(UChar32 c, UScriptCode sc) uscript_hasScript(UChar32 c, UScriptCode sc)_DRAFT_API_DO_NOT_USE
#    else
#        define StringPiece&amp;y)_4_7 StringPiece&amp;y)_DRAFT_API_DO_NOT_USE
#        define ubidi_getBaseDirection(const UChar *text, int32_t length)_4_7 ubidi_getBaseDirection(const UChar *text, int32_t length)_DRAFT_API_DO_NOT_USE
#        define ucal_openTimeZoneIDEnumeration(USystemTimeZoneType zoneType, const char *region, const int32_t *rawOffset, UErrorCode *ec)_4_7 ucal_openTimeZoneIDEnumeration(USystemTimeZoneType zoneType, const char *region, const int32_t *rawOffset, UErrorCode *ec)_DRAFT_API_DO_NOT_USE
#        define ucnv_isFixedWidth(UConverter *cnv, UErrorCode *status)_4_7 ucnv_isFixedWidth(UConverter *cnv, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uidna_close(UIDNA *idna)_4_7 uidna_close(UIDNA *idna)_DRAFT_API_DO_NOT_USE
#        define uidna_openUTS46(uint32_t options, UErrorCode *pErrorCode)_4_7 uidna_openUTS46(uint32_t options, UErrorCode *pErrorCode)_DRAFT_API_DO_NOT_USE
#        define uloc_forLanguageTag(const char *langtag, char *localeID, int32_t localeIDCapacity, int32_t *parsedLength, UErrorCode *err)_4_7 uloc_forLanguageTag(const char *langtag, char *localeID, int32_t localeIDCapacity, int32_t *parsedLength, UErrorCode *err)_DRAFT_API_DO_NOT_USE
#        define uloc_toLanguageTag(const char *localeID, char *langtag, int32_t langtagCapacity, UBool strict, UErrorCode *err)_4_7 uloc_toLanguageTag(const char *localeID, char *langtag, int32_t langtagCapacity, UBool strict, UErrorCode *err)_DRAFT_API_DO_NOT_USE
#        define unorm2_getDecomposition(const UNormalizer2 *norm2, UChar32 c, UChar *decomposition, int32_t capacity, UErrorCode *pErrorCode)_4_7 unorm2_getDecomposition(const UNormalizer2 *norm2, UChar32 c, UChar *decomposition, int32_t capacity, UErrorCode *pErrorCode)_DRAFT_API_DO_NOT_USE
#        define uregex_end64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status)_4_7 uregex_end64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_find64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_4_7 uregex_find64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_getFindProgressCallback(const URegularExpression *regexp, URegexFindProgressCallback **callback, const void **context, UErrorCode *status)_4_7 uregex_getFindProgressCallback(const URegularExpression *regexp, URegexFindProgressCallback **callback, const void **context, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_lookingAt64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_4_7 uregex_lookingAt64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_matches64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_4_7 uregex_matches64(URegularExpression *regexp, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_patternUText(const URegularExpression *regexp, UErrorCode *status)_4_7 uregex_patternUText(const URegularExpression *regexp, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_regionEnd64(const URegularExpression *regexp, UErrorCode *status)_4_7 uregex_regionEnd64(const URegularExpression *regexp, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_regionStart64(const URegularExpression *regexp, UErrorCode *status)_4_7 uregex_regionStart64(const URegularExpression *regexp, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_reset64(URegularExpression *regexp, int64_t index, UErrorCode *status)_4_7 uregex_reset64(URegularExpression *regexp, int64_t index, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_setFindProgressCallback(URegularExpression *regexp, URegexFindProgressCallback *callback, const void *context, UErrorCode *status)_4_7 uregex_setFindProgressCallback(URegularExpression *regexp, URegexFindProgressCallback *callback, const void *context, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_setRegion64(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, UErrorCode *status)_4_7 uregex_setRegion64(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_setRegionAndStart(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, int64_t startIndex, UErrorCode *status)_4_7 uregex_setRegionAndStart(URegularExpression *regexp, int64_t regionStart, int64_t regionLimit, int64_t startIndex, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uregex_start64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status)_4_7 uregex_start64(URegularExpression *regexp, int32_t groupNum, UErrorCode *status)_DRAFT_API_DO_NOT_USE
#        define uscript_getScriptExtensions(UChar32 c, UScriptCode *scripts, int32_t capacity, UErrorCode *pErrorCode)_4_7 uscript_getScriptExtensions(UChar32 c, UScriptCode *scripts, int32_t capacity, UErrorCode *pErrorCode)_DRAFT_API_DO_NOT_USE
#        define uscript_hasScript(UChar32 c, UScriptCode sc)_4_7 uscript_hasScript(UChar32 c, UScriptCode sc)_DRAFT_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_DRAFT_API */
#endif /* UDRAFT_H */

