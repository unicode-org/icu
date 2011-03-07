/*
*******************************************************************************
*   Copyright (C) 2004-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  uintrnal.h
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
#        define LE_ARRAY_SIZE(array) LE_ARRAY_SIZE(array)_INTERNAL_API_DO_NOT_USE
#        define RegexPatternDump(pat) RegexPatternDump(pat)_INTERNAL_API_DO_NOT_USE
#        define bms_close(BMS *bms) bms_close(BMS *bms)_INTERNAL_API_DO_NOT_USE
#        define bms_empty(BMS *bms) bms_empty(BMS *bms)_INTERNAL_API_DO_NOT_USE
#        define bms_getData(BMS *bms) bms_getData(BMS *bms)_INTERNAL_API_DO_NOT_USE
#        define bms_open(UCD *ucd, const UChar *pattern, int32_t patternLength, const UChar *target, int32_t targetLength, UErrorCode *status) bms_open(UCD *ucd, const UChar *pattern, int32_t patternLength, const UChar *target, int32_t targetLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define bms_search(BMS *bms, int32_t offset, int32_t *start, int32_t *end) bms_search(BMS *bms, int32_t offset, int32_t *start, int32_t *end)_INTERNAL_API_DO_NOT_USE
#        define bms_setTargetString(BMS *bms, const UChar *target, int32_t targetLength, UErrorCode *status) bms_setTargetString(BMS *bms, const UChar *target, int32_t targetLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define pl_addFontRun(pl_fontRuns *fontRuns, const le_font *font, le_int32 limit) pl_addFontRun(pl_fontRuns *fontRuns, const le_font *font, le_int32 limit)_INTERNAL_API_DO_NOT_USE
#        define pl_addLocaleRun(pl_localeRuns *localeRuns, const char *locale, le_int32 limit) pl_addLocaleRun(pl_localeRuns *localeRuns, const char *locale, le_int32 limit)_INTERNAL_API_DO_NOT_USE
#        define pl_addValueRun(pl_valueRuns *valueRuns, le_int32 value, le_int32 limit) pl_addValueRun(pl_valueRuns *valueRuns, le_int32 value, le_int32 limit)_INTERNAL_API_DO_NOT_USE
#        define pl_close(pl_paragraph *paragraph) pl_close(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_closeFontRuns(pl_fontRuns *fontRuns) pl_closeFontRuns(pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_closeLine(pl_line *line) pl_closeLine(pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_closeLocaleRuns(pl_localeRuns *localeRuns) pl_closeLocaleRuns(pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_closeValueRuns(pl_valueRuns *valueRuns) pl_closeValueRuns(pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_countLineRuns(const pl_line *line) pl_countLineRuns(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getAscent(const pl_paragraph *paragraph) pl_getAscent(const pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getDescent(const pl_paragraph *paragraph) pl_getDescent(const pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunCount(const pl_fontRuns *fontRuns) pl_getFontRunCount(const pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunFont(const pl_fontRuns *fontRuns, le_int32 run) pl_getFontRunFont(const pl_fontRuns *fontRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLastLimit(const pl_fontRuns *fontRuns) pl_getFontRunLastLimit(const pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLimit(const pl_fontRuns *fontRuns, le_int32 run) pl_getFontRunLimit(const pl_fontRuns *fontRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getLeading(const pl_paragraph *paragraph) pl_getLeading(const pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineAscent(const pl_line *line) pl_getLineAscent(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineDescent(const pl_line *line) pl_getLineDescent(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineLeading(const pl_line *line) pl_getLineLeading(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineVisualRun(const pl_line *line, le_int32 runIndex) pl_getLineVisualRun(const pl_line *line, le_int32 runIndex)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineWidth(const pl_line *line) pl_getLineWidth(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunCount(const pl_localeRuns *localeRuns) pl_getLocaleRunCount(const pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLastLimit(const pl_localeRuns *localeRuns) pl_getLocaleRunLastLimit(const pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLimit(const pl_localeRuns *localeRuns, le_int32 run) pl_getLocaleRunLimit(const pl_localeRuns *localeRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLocale(const pl_localeRuns *localeRuns, le_int32 run) pl_getLocaleRunLocale(const pl_localeRuns *localeRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getParagraphLevel(pl_paragraph *paragraph) pl_getParagraphLevel(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getTextDirection(pl_paragraph *paragraph) pl_getTextDirection(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunCount(const pl_valueRuns *valueRuns) pl_getValueRunCount(const pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLastLimit(const pl_valueRuns *valueRuns) pl_getValueRunLastLimit(const pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLimit(const pl_valueRuns *valueRuns, le_int32 run) pl_getValueRunLimit(const pl_valueRuns *valueRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunValue(const pl_valueRuns *valueRuns, le_int32 run) pl_getValueRunValue(const pl_valueRuns *valueRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunAscent(const pl_visualRun *run) pl_getVisualRunAscent(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDescent(const pl_visualRun *run) pl_getVisualRunDescent(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDirection(const pl_visualRun *run) pl_getVisualRunDirection(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunFont(const pl_visualRun *run) pl_getVisualRunFont(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphCount(const pl_visualRun *run) pl_getVisualRunGlyphCount(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphToCharMap(const pl_visualRun *run) pl_getVisualRunGlyphToCharMap(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphs(const pl_visualRun *run) pl_getVisualRunGlyphs(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunLeading(const pl_visualRun *run) pl_getVisualRunLeading(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunPositions(const pl_visualRun *run) pl_getVisualRunPositions(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_line pl_line_INTERNAL_API_DO_NOT_USE
#        define pl_nextLine(pl_paragraph *paragraph, float width) pl_nextLine(pl_paragraph *paragraph, float width)_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyFontRuns(le_int32 initialCapacity) pl_openEmptyFontRuns(le_int32 initialCapacity)_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyLocaleRuns(le_int32 initialCapacity) pl_openEmptyLocaleRuns(le_int32 initialCapacity)_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyValueRuns(le_int32 initialCapacity) pl_openEmptyValueRuns(le_int32 initialCapacity)_INTERNAL_API_DO_NOT_USE
#        define pl_openFontRuns(const le_font **fonts, const le_int32 *limits, le_int32 count) pl_openFontRuns(const le_font **fonts, const le_int32 *limits, le_int32 count)_INTERNAL_API_DO_NOT_USE
#        define pl_openLocaleRuns(const char **locales, const le_int32 *limits, le_int32 count) pl_openLocaleRuns(const char **locales, const le_int32 *limits, le_int32 count)_INTERNAL_API_DO_NOT_USE
#        define pl_openValueRuns(const le_int32 *values, const le_int32 *limits, le_int32 count) pl_openValueRuns(const le_int32 *values, const le_int32 *limits, le_int32 count)_INTERNAL_API_DO_NOT_USE
#        define pl_paragraph pl_paragraph_INTERNAL_API_DO_NOT_USE
#        define pl_reflow(pl_paragraph *paragraph) pl_reflow(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_resetFontRuns(pl_fontRuns *fontRuns) pl_resetFontRuns(pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_resetLocaleRuns(pl_localeRuns *localeRuns) pl_resetLocaleRuns(pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_resetValueRuns(pl_valueRuns *valueRuns) pl_resetValueRuns(pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_visualRun pl_visualRun_INTERNAL_API_DO_NOT_USE
#        define ucd_close(UCD *ucd) ucd_close(UCD *ucd)_INTERNAL_API_DO_NOT_USE
#        define ucd_flushCache() ucd_flushCache()_INTERNAL_API_DO_NOT_USE
#        define ucd_freeCache() ucd_freeCache()_INTERNAL_API_DO_NOT_USE
#        define ucd_getCollator(UCD *ucd) ucd_getCollator(UCD *ucd)_INTERNAL_API_DO_NOT_USE
#        define ucd_open(UCollator *coll, UErrorCode *status) ucd_open(UCollator *coll, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_equals(const UCollator *source, const UCollator *target) ucol_equals(const UCollator *source, const UCollator *target)_INTERNAL_API_DO_NOT_USE
#        define ucol_forceHanImplicit(UCollationElements *elems, UErrorCode *status) ucol_forceHanImplicit(UCollationElements *elems, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_forgetUCA(void) ucol_forgetUCA(void)_INTERNAL_API_DO_NOT_USE
#        define ucol_getAttributeOrDefault(const UCollator *coll, UColAttribute attr, UErrorCode *status) ucol_getAttributeOrDefault(const UCollator *coll, UColAttribute attr, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_getReorderCodes(const UCollator *coll, int32_t *dest, int32_t destCapacity, UErrorCode *pErrorCode) ucol_getReorderCodes(const UCollator *coll, int32_t *dest, int32_t destCapacity, UErrorCode *pErrorCode)_INTERNAL_API_DO_NOT_USE
#        define ucol_getUnsafeSet(const UCollator *coll, USet *unsafe, UErrorCode *status) ucol_getUnsafeSet(const UCollator *coll, USet *unsafe, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_nextProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status) ucol_nextProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_previousProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status) ucol_previousProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_setReorderCodes(UCollator *coll, const int32_t *reorderCodes, int32_t reorderCodesLength, UErrorCode *pErrorCode) ucol_setReorderCodes(UCollator *coll, const int32_t *reorderCodes, int32_t reorderCodesLength, UErrorCode *pErrorCode)_INTERNAL_API_DO_NOT_USE
#        define udat_applyPatternRelative(UDateFormat *format, const UChar *datePattern, int32_t datePatternLength, const UChar *timePattern, int32_t timePatternLength, UErrorCode *status) udat_applyPatternRelative(UDateFormat *format, const UChar *datePattern, int32_t datePatternLength, const UChar *timePattern, int32_t timePatternLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define udat_toPatternRelativeDate(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status) udat_toPatternRelativeDate(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define udat_toPatternRelativeTime(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status) udat_toPatternRelativeTime(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_getConfiguration(UPlugData *plug) uplug_getConfiguration(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getContext(UPlugData *plug) uplug_getContext(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getCurrentLevel(void) uplug_getCurrentLevel(void)_INTERNAL_API_DO_NOT_USE
#        define uplug_getLibrary(UPlugData *plug) uplug_getLibrary(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getLibraryName(UPlugData *plug, UErrorCode *status) uplug_getLibraryName(UPlugData *plug, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_getPlugLevel(UPlugData *plug) uplug_getPlugLevel(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getPlugLoadStatus(UPlugData *plug) uplug_getPlugLoadStatus(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getPlugName(UPlugData *plug) uplug_getPlugName(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getSymbolName(UPlugData *plug) uplug_getSymbolName(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_loadPlugFromEntrypoint(UPlugEntrypoint *entrypoint, const char *config, UErrorCode *status) uplug_loadPlugFromEntrypoint(UPlugEntrypoint *entrypoint, const char *config, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_loadPlugFromLibrary(const char *libName, const char *sym, const char *config, UErrorCode *status) uplug_loadPlugFromLibrary(const char *libName, const char *sym, const char *config, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_nextPlug(UPlugData *prior) uplug_nextPlug(UPlugData *prior)_INTERNAL_API_DO_NOT_USE
#        define uplug_removePlug(UPlugData *plug, UErrorCode *status) uplug_removePlug(UPlugData *plug, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_setContext(UPlugData *plug, void *context) uplug_setContext(UPlugData *plug, void *context)_INTERNAL_API_DO_NOT_USE
#        define uplug_setPlugLevel(UPlugData *plug, UPlugLevel level) uplug_setPlugLevel(UPlugData *plug, UPlugLevel level)_INTERNAL_API_DO_NOT_USE
#        define uplug_setPlugName(UPlugData *plug, const char *name) uplug_setPlugName(UPlugData *plug, const char *name)_INTERNAL_API_DO_NOT_USE
#        define uplug_setPlugNoUnload(UPlugData *plug, UBool dontUnload) uplug_setPlugNoUnload(UPlugData *plug, UBool dontUnload)_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultCodepage(void) uprv_getDefaultCodepage(void)_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultLocaleID(void) uprv_getDefaultLocaleID(void)_INTERNAL_API_DO_NOT_USE
#        define ures_openFillIn(UResourceBundle *r, const char *packageName, const char *localeID, UErrorCode *status) ures_openFillIn(UResourceBundle *r, const char *packageName, const char *localeID, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define usearch_search(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status) usearch_search(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define usearch_searchBackwards(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status) usearch_searchBackwards(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define utf8_appendCharSafeBody(uint8_t *s, int32_t i, int32_t length, UChar32 c, UBool *pIsError) utf8_appendCharSafeBody(uint8_t *s, int32_t i, int32_t length, UChar32 c, UBool *pIsError)_INTERNAL_API_DO_NOT_USE
#        define utf8_back1SafeBody(const uint8_t *s, int32_t start, int32_t i) utf8_back1SafeBody(const uint8_t *s, int32_t start, int32_t i)_INTERNAL_API_DO_NOT_USE
#        define utf8_countTrailBytes[256] utf8_countTrailBytes[256]_INTERNAL_API_DO_NOT_USE
#        define utf8_nextCharSafeBody(const uint8_t *s, int32_t *pi, int32_t length, UChar32 c, UBool strict) utf8_nextCharSafeBody(const uint8_t *s, int32_t *pi, int32_t length, UChar32 c, UBool strict)_INTERNAL_API_DO_NOT_USE
#        define utf8_prevCharSafeBody(const uint8_t *s, int32_t start, int32_t *pi, UChar32 c, UBool strict) utf8_prevCharSafeBody(const uint8_t *s, int32_t start, int32_t *pi, UChar32 c, UBool strict)_INTERNAL_API_DO_NOT_USE
#    else
#        define LE_ARRAY_SIZE(array)_4_7 LE_ARRAY_SIZE(array)_INTERNAL_API_DO_NOT_USE
#        define RegexPatternDump(pat)_4_7 RegexPatternDump(pat)_INTERNAL_API_DO_NOT_USE
#        define bms_close(BMS *bms)_4_7 bms_close(BMS *bms)_INTERNAL_API_DO_NOT_USE
#        define bms_empty(BMS *bms)_4_7 bms_empty(BMS *bms)_INTERNAL_API_DO_NOT_USE
#        define bms_getData(BMS *bms)_4_7 bms_getData(BMS *bms)_INTERNAL_API_DO_NOT_USE
#        define bms_open(UCD *ucd, const UChar *pattern, int32_t patternLength, const UChar *target, int32_t targetLength, UErrorCode *status)_4_7 bms_open(UCD *ucd, const UChar *pattern, int32_t patternLength, const UChar *target, int32_t targetLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define bms_search(BMS *bms, int32_t offset, int32_t *start, int32_t *end)_4_7 bms_search(BMS *bms, int32_t offset, int32_t *start, int32_t *end)_INTERNAL_API_DO_NOT_USE
#        define bms_setTargetString(BMS *bms, const UChar *target, int32_t targetLength, UErrorCode *status)_4_7 bms_setTargetString(BMS *bms, const UChar *target, int32_t targetLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define pl_addFontRun(pl_fontRuns *fontRuns, const le_font *font, le_int32 limit)_4_7 pl_addFontRun(pl_fontRuns *fontRuns, const le_font *font, le_int32 limit)_INTERNAL_API_DO_NOT_USE
#        define pl_addLocaleRun(pl_localeRuns *localeRuns, const char *locale, le_int32 limit)_4_7 pl_addLocaleRun(pl_localeRuns *localeRuns, const char *locale, le_int32 limit)_INTERNAL_API_DO_NOT_USE
#        define pl_addValueRun(pl_valueRuns *valueRuns, le_int32 value, le_int32 limit)_4_7 pl_addValueRun(pl_valueRuns *valueRuns, le_int32 value, le_int32 limit)_INTERNAL_API_DO_NOT_USE
#        define pl_close(pl_paragraph *paragraph)_4_7 pl_close(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_closeFontRuns(pl_fontRuns *fontRuns)_4_7 pl_closeFontRuns(pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_closeLine(pl_line *line)_4_7 pl_closeLine(pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_closeLocaleRuns(pl_localeRuns *localeRuns)_4_7 pl_closeLocaleRuns(pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_closeValueRuns(pl_valueRuns *valueRuns)_4_7 pl_closeValueRuns(pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_countLineRuns(const pl_line *line)_4_7 pl_countLineRuns(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getAscent(const pl_paragraph *paragraph)_4_7 pl_getAscent(const pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getDescent(const pl_paragraph *paragraph)_4_7 pl_getDescent(const pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunCount(const pl_fontRuns *fontRuns)_4_7 pl_getFontRunCount(const pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunFont(const pl_fontRuns *fontRuns, le_int32 run)_4_7 pl_getFontRunFont(const pl_fontRuns *fontRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLastLimit(const pl_fontRuns *fontRuns)_4_7 pl_getFontRunLastLimit(const pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLimit(const pl_fontRuns *fontRuns, le_int32 run)_4_7 pl_getFontRunLimit(const pl_fontRuns *fontRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getLeading(const pl_paragraph *paragraph)_4_7 pl_getLeading(const pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineAscent(const pl_line *line)_4_7 pl_getLineAscent(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineDescent(const pl_line *line)_4_7 pl_getLineDescent(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineLeading(const pl_line *line)_4_7 pl_getLineLeading(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineVisualRun(const pl_line *line, le_int32 runIndex)_4_7 pl_getLineVisualRun(const pl_line *line, le_int32 runIndex)_INTERNAL_API_DO_NOT_USE
#        define pl_getLineWidth(const pl_line *line)_4_7 pl_getLineWidth(const pl_line *line)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunCount(const pl_localeRuns *localeRuns)_4_7 pl_getLocaleRunCount(const pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLastLimit(const pl_localeRuns *localeRuns)_4_7 pl_getLocaleRunLastLimit(const pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLimit(const pl_localeRuns *localeRuns, le_int32 run)_4_7 pl_getLocaleRunLimit(const pl_localeRuns *localeRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLocale(const pl_localeRuns *localeRuns, le_int32 run)_4_7 pl_getLocaleRunLocale(const pl_localeRuns *localeRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getParagraphLevel(pl_paragraph *paragraph)_4_7 pl_getParagraphLevel(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getTextDirection(pl_paragraph *paragraph)_4_7 pl_getTextDirection(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunCount(const pl_valueRuns *valueRuns)_4_7 pl_getValueRunCount(const pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLastLimit(const pl_valueRuns *valueRuns)_4_7 pl_getValueRunLastLimit(const pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLimit(const pl_valueRuns *valueRuns, le_int32 run)_4_7 pl_getValueRunLimit(const pl_valueRuns *valueRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunValue(const pl_valueRuns *valueRuns, le_int32 run)_4_7 pl_getValueRunValue(const pl_valueRuns *valueRuns, le_int32 run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunAscent(const pl_visualRun *run)_4_7 pl_getVisualRunAscent(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDescent(const pl_visualRun *run)_4_7 pl_getVisualRunDescent(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDirection(const pl_visualRun *run)_4_7 pl_getVisualRunDirection(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunFont(const pl_visualRun *run)_4_7 pl_getVisualRunFont(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphCount(const pl_visualRun *run)_4_7 pl_getVisualRunGlyphCount(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphToCharMap(const pl_visualRun *run)_4_7 pl_getVisualRunGlyphToCharMap(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphs(const pl_visualRun *run)_4_7 pl_getVisualRunGlyphs(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunLeading(const pl_visualRun *run)_4_7 pl_getVisualRunLeading(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunPositions(const pl_visualRun *run)_4_7 pl_getVisualRunPositions(const pl_visualRun *run)_INTERNAL_API_DO_NOT_USE
#        define pl_line_4_7 pl_line_INTERNAL_API_DO_NOT_USE
#        define pl_nextLine(pl_paragraph *paragraph, float width)_4_7 pl_nextLine(pl_paragraph *paragraph, float width)_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyFontRuns(le_int32 initialCapacity)_4_7 pl_openEmptyFontRuns(le_int32 initialCapacity)_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyLocaleRuns(le_int32 initialCapacity)_4_7 pl_openEmptyLocaleRuns(le_int32 initialCapacity)_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyValueRuns(le_int32 initialCapacity)_4_7 pl_openEmptyValueRuns(le_int32 initialCapacity)_INTERNAL_API_DO_NOT_USE
#        define pl_openFontRuns(const le_font **fonts, const le_int32 *limits, le_int32 count)_4_7 pl_openFontRuns(const le_font **fonts, const le_int32 *limits, le_int32 count)_INTERNAL_API_DO_NOT_USE
#        define pl_openLocaleRuns(const char **locales, const le_int32 *limits, le_int32 count)_4_7 pl_openLocaleRuns(const char **locales, const le_int32 *limits, le_int32 count)_INTERNAL_API_DO_NOT_USE
#        define pl_openValueRuns(const le_int32 *values, const le_int32 *limits, le_int32 count)_4_7 pl_openValueRuns(const le_int32 *values, const le_int32 *limits, le_int32 count)_INTERNAL_API_DO_NOT_USE
#        define pl_paragraph_4_7 pl_paragraph_INTERNAL_API_DO_NOT_USE
#        define pl_reflow(pl_paragraph *paragraph)_4_7 pl_reflow(pl_paragraph *paragraph)_INTERNAL_API_DO_NOT_USE
#        define pl_resetFontRuns(pl_fontRuns *fontRuns)_4_7 pl_resetFontRuns(pl_fontRuns *fontRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_resetLocaleRuns(pl_localeRuns *localeRuns)_4_7 pl_resetLocaleRuns(pl_localeRuns *localeRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_resetValueRuns(pl_valueRuns *valueRuns)_4_7 pl_resetValueRuns(pl_valueRuns *valueRuns)_INTERNAL_API_DO_NOT_USE
#        define pl_visualRun_4_7 pl_visualRun_INTERNAL_API_DO_NOT_USE
#        define ucd_close(UCD *ucd)_4_7 ucd_close(UCD *ucd)_INTERNAL_API_DO_NOT_USE
#        define ucd_flushCache()_4_7 ucd_flushCache()_INTERNAL_API_DO_NOT_USE
#        define ucd_freeCache()_4_7 ucd_freeCache()_INTERNAL_API_DO_NOT_USE
#        define ucd_getCollator(UCD *ucd)_4_7 ucd_getCollator(UCD *ucd)_INTERNAL_API_DO_NOT_USE
#        define ucd_open(UCollator *coll, UErrorCode *status)_4_7 ucd_open(UCollator *coll, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_equals(const UCollator *source, const UCollator *target)_4_7 ucol_equals(const UCollator *source, const UCollator *target)_INTERNAL_API_DO_NOT_USE
#        define ucol_forceHanImplicit(UCollationElements *elems, UErrorCode *status)_4_7 ucol_forceHanImplicit(UCollationElements *elems, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_forgetUCA(void)_4_7 ucol_forgetUCA(void)_INTERNAL_API_DO_NOT_USE
#        define ucol_getAttributeOrDefault(const UCollator *coll, UColAttribute attr, UErrorCode *status)_4_7 ucol_getAttributeOrDefault(const UCollator *coll, UColAttribute attr, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_getReorderCodes(const UCollator *coll, int32_t *dest, int32_t destCapacity, UErrorCode *pErrorCode)_4_7 ucol_getReorderCodes(const UCollator *coll, int32_t *dest, int32_t destCapacity, UErrorCode *pErrorCode)_INTERNAL_API_DO_NOT_USE
#        define ucol_getUnsafeSet(const UCollator *coll, USet *unsafe, UErrorCode *status)_4_7 ucol_getUnsafeSet(const UCollator *coll, USet *unsafe, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_nextProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status)_4_7 ucol_nextProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_previousProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status)_4_7 ucol_previousProcessed(UCollationElements *elems, int32_t *ixLow, int32_t *ixHigh, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define ucol_setReorderCodes(UCollator *coll, const int32_t *reorderCodes, int32_t reorderCodesLength, UErrorCode *pErrorCode)_4_7 ucol_setReorderCodes(UCollator *coll, const int32_t *reorderCodes, int32_t reorderCodesLength, UErrorCode *pErrorCode)_INTERNAL_API_DO_NOT_USE
#        define udat_applyPatternRelative(UDateFormat *format, const UChar *datePattern, int32_t datePatternLength, const UChar *timePattern, int32_t timePatternLength, UErrorCode *status)_4_7 udat_applyPatternRelative(UDateFormat *format, const UChar *datePattern, int32_t datePatternLength, const UChar *timePattern, int32_t timePatternLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define udat_toPatternRelativeDate(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status)_4_7 udat_toPatternRelativeDate(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define udat_toPatternRelativeTime(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status)_4_7 udat_toPatternRelativeTime(const UDateFormat *fmt, UChar *result, int32_t resultLength, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_getConfiguration(UPlugData *plug)_4_7 uplug_getConfiguration(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getContext(UPlugData *plug)_4_7 uplug_getContext(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getCurrentLevel(void)_4_7 uplug_getCurrentLevel(void)_INTERNAL_API_DO_NOT_USE
#        define uplug_getLibrary(UPlugData *plug)_4_7 uplug_getLibrary(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getLibraryName(UPlugData *plug, UErrorCode *status)_4_7 uplug_getLibraryName(UPlugData *plug, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_getPlugLevel(UPlugData *plug)_4_7 uplug_getPlugLevel(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getPlugLoadStatus(UPlugData *plug)_4_7 uplug_getPlugLoadStatus(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getPlugName(UPlugData *plug)_4_7 uplug_getPlugName(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_getSymbolName(UPlugData *plug)_4_7 uplug_getSymbolName(UPlugData *plug)_INTERNAL_API_DO_NOT_USE
#        define uplug_loadPlugFromEntrypoint(UPlugEntrypoint *entrypoint, const char *config, UErrorCode *status)_4_7 uplug_loadPlugFromEntrypoint(UPlugEntrypoint *entrypoint, const char *config, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_loadPlugFromLibrary(const char *libName, const char *sym, const char *config, UErrorCode *status)_4_7 uplug_loadPlugFromLibrary(const char *libName, const char *sym, const char *config, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_nextPlug(UPlugData *prior)_4_7 uplug_nextPlug(UPlugData *prior)_INTERNAL_API_DO_NOT_USE
#        define uplug_removePlug(UPlugData *plug, UErrorCode *status)_4_7 uplug_removePlug(UPlugData *plug, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define uplug_setContext(UPlugData *plug, void *context)_4_7 uplug_setContext(UPlugData *plug, void *context)_INTERNAL_API_DO_NOT_USE
#        define uplug_setPlugLevel(UPlugData *plug, UPlugLevel level)_4_7 uplug_setPlugLevel(UPlugData *plug, UPlugLevel level)_INTERNAL_API_DO_NOT_USE
#        define uplug_setPlugName(UPlugData *plug, const char *name)_4_7 uplug_setPlugName(UPlugData *plug, const char *name)_INTERNAL_API_DO_NOT_USE
#        define uplug_setPlugNoUnload(UPlugData *plug, UBool dontUnload)_4_7 uplug_setPlugNoUnload(UPlugData *plug, UBool dontUnload)_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultCodepage(void)_4_7 uprv_getDefaultCodepage(void)_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultLocaleID(void)_4_7 uprv_getDefaultLocaleID(void)_INTERNAL_API_DO_NOT_USE
#        define ures_openFillIn(UResourceBundle *r, const char *packageName, const char *localeID, UErrorCode *status)_4_7 ures_openFillIn(UResourceBundle *r, const char *packageName, const char *localeID, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define usearch_search(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status)_4_7 usearch_search(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define usearch_searchBackwards(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status)_4_7 usearch_searchBackwards(UStringSearch *strsrch, int32_t startIdx, int32_t *matchStart, int32_t *matchLimit, UErrorCode *status)_INTERNAL_API_DO_NOT_USE
#        define utf8_appendCharSafeBody(uint8_t *s, int32_t i, int32_t length, UChar32 c, UBool *pIsError)_4_7 utf8_appendCharSafeBody(uint8_t *s, int32_t i, int32_t length, UChar32 c, UBool *pIsError)_INTERNAL_API_DO_NOT_USE
#        define utf8_back1SafeBody(const uint8_t *s, int32_t start, int32_t i)_4_7 utf8_back1SafeBody(const uint8_t *s, int32_t start, int32_t i)_INTERNAL_API_DO_NOT_USE
#        define utf8_countTrailBytes[256]_4_7 utf8_countTrailBytes[256]_INTERNAL_API_DO_NOT_USE
#        define utf8_nextCharSafeBody(const uint8_t *s, int32_t *pi, int32_t length, UChar32 c, UBool strict)_4_7 utf8_nextCharSafeBody(const uint8_t *s, int32_t *pi, int32_t length, UChar32 c, UBool strict)_INTERNAL_API_DO_NOT_USE
#        define utf8_prevCharSafeBody(const uint8_t *s, int32_t start, int32_t *pi, UChar32 c, UBool strict)_4_7 utf8_prevCharSafeBody(const uint8_t *s, int32_t start, int32_t *pi, UChar32 c, UBool strict)_INTERNAL_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_INTERNAL_API */
#endif /* UINTRNAL_H */

