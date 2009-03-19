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

#ifndef UINTRNAL_H
#define UINTRNAL_H

#ifdef U_HIDE_INTERNAL_API

#    if U_DISABLE_RENAMING
#        define RegexPatternDump RegexPatternDump_INTERNAL_API_DO_NOT_USE
#        define pl_addFontRun pl_addFontRun_INTERNAL_API_DO_NOT_USE
#        define pl_addLocaleRun pl_addLocaleRun_INTERNAL_API_DO_NOT_USE
#        define pl_addValueRun pl_addValueRun_INTERNAL_API_DO_NOT_USE
#        define pl_close pl_close_INTERNAL_API_DO_NOT_USE
#        define pl_closeFontRuns pl_closeFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_closeLine pl_closeLine_INTERNAL_API_DO_NOT_USE
#        define pl_closeLocaleRuns pl_closeLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_closeValueRuns pl_closeValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_countLineRuns pl_countLineRuns_INTERNAL_API_DO_NOT_USE
#        define pl_create pl_create_INTERNAL_API_DO_NOT_USE
#        define pl_getAscent pl_getAscent_INTERNAL_API_DO_NOT_USE
#        define pl_getDescent pl_getDescent_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunCount pl_getFontRunCount_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunFont pl_getFontRunFont_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLastLimit pl_getFontRunLastLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLimit pl_getFontRunLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getLeading pl_getLeading_INTERNAL_API_DO_NOT_USE
#        define pl_getLineAscent pl_getLineAscent_INTERNAL_API_DO_NOT_USE
#        define pl_getLineDescent pl_getLineDescent_INTERNAL_API_DO_NOT_USE
#        define pl_getLineLeading pl_getLineLeading_INTERNAL_API_DO_NOT_USE
#        define pl_getLineVisualRun pl_getLineVisualRun_INTERNAL_API_DO_NOT_USE
#        define pl_getLineWidth pl_getLineWidth_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunCount pl_getLocaleRunCount_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLastLimit pl_getLocaleRunLastLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLimit pl_getLocaleRunLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLocale pl_getLocaleRunLocale_INTERNAL_API_DO_NOT_USE
#        define pl_getParagraphLevel pl_getParagraphLevel_INTERNAL_API_DO_NOT_USE
#        define pl_getTextDirection pl_getTextDirection_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunCount pl_getValueRunCount_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLastLimit pl_getValueRunLastLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLimit pl_getValueRunLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunValue pl_getValueRunValue_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunAscent pl_getVisualRunAscent_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDescent pl_getVisualRunDescent_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDirection pl_getVisualRunDirection_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunFont pl_getVisualRunFont_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphCount pl_getVisualRunGlyphCount_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphToCharMap pl_getVisualRunGlyphToCharMap_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphs pl_getVisualRunGlyphs_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunLeading pl_getVisualRunLeading_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunPositions pl_getVisualRunPositions_INTERNAL_API_DO_NOT_USE
#        define pl_isComplex pl_isComplex_INTERNAL_API_DO_NOT_USE
#        define pl_line pl_line_INTERNAL_API_DO_NOT_USE
#        define pl_nextLine pl_nextLine_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyFontRuns pl_openEmptyFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyLocaleRuns pl_openEmptyLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyValueRuns pl_openEmptyValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openFontRuns pl_openFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openLocaleRuns pl_openLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openValueRuns pl_openValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_paragraph pl_paragraph_INTERNAL_API_DO_NOT_USE
#        define pl_reflow pl_reflow_INTERNAL_API_DO_NOT_USE
#        define pl_resetFontRuns pl_resetFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_resetLocaleRuns pl_resetLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_resetValueRuns pl_resetValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_visualRun pl_visualRun_INTERNAL_API_DO_NOT_USE
#        define ucol_equals ucol_equals_INTERNAL_API_DO_NOT_USE
#        define ucol_forgetUCA ucol_forgetUCA_INTERNAL_API_DO_NOT_USE
#        define ucol_getAttributeOrDefault ucol_getAttributeOrDefault_INTERNAL_API_DO_NOT_USE
#        define ucol_getUnsafeSet ucol_getUnsafeSet_INTERNAL_API_DO_NOT_USE
#        define ucol_nextProcessed ucol_nextProcessed_INTERNAL_API_DO_NOT_USE
#        define ucol_prepareShortStringOpen ucol_prepareShortStringOpen_INTERNAL_API_DO_NOT_USE
#        define ucol_previousProcessed ucol_previousProcessed_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultCodepage uprv_getDefaultCodepage_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultLocaleID uprv_getDefaultLocaleID_INTERNAL_API_DO_NOT_USE
#        define ures_openFillIn ures_openFillIn_INTERNAL_API_DO_NOT_USE
#        define usearch_search usearch_search_INTERNAL_API_DO_NOT_USE
#        define usearch_searchBackwards usearch_searchBackwards_INTERNAL_API_DO_NOT_USE
#        define utf8_appendCharSafeBody utf8_appendCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_back1SafeBody utf8_back1SafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_countTrailBytes utf8_countTrailBytes_INTERNAL_API_DO_NOT_USE
#        define utf8_nextCharSafeBody utf8_nextCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_prevCharSafeBody utf8_prevCharSafeBody_INTERNAL_API_DO_NOT_USE
#    else
#        define RegexPatternDump_4_1.2 RegexPatternDump_INTERNAL_API_DO_NOT_USE
#        define pl_addFontRun_4_1.2 pl_addFontRun_INTERNAL_API_DO_NOT_USE
#        define pl_addLocaleRun_4_1.2 pl_addLocaleRun_INTERNAL_API_DO_NOT_USE
#        define pl_addValueRun_4_1.2 pl_addValueRun_INTERNAL_API_DO_NOT_USE
#        define pl_closeFontRuns_4_1.2 pl_closeFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_closeLine_4_1.2 pl_closeLine_INTERNAL_API_DO_NOT_USE
#        define pl_closeLocaleRuns_4_1.2 pl_closeLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_closeValueRuns_4_1.2 pl_closeValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_close_4_1.2 pl_close_INTERNAL_API_DO_NOT_USE
#        define pl_countLineRuns_4_1.2 pl_countLineRuns_INTERNAL_API_DO_NOT_USE
#        define pl_create_4_1.2 pl_create_INTERNAL_API_DO_NOT_USE
#        define pl_getAscent_4_1.2 pl_getAscent_INTERNAL_API_DO_NOT_USE
#        define pl_getDescent_4_1.2 pl_getDescent_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunCount_4_1.2 pl_getFontRunCount_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunFont_4_1.2 pl_getFontRunFont_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLastLimit_4_1.2 pl_getFontRunLastLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getFontRunLimit_4_1.2 pl_getFontRunLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getLeading_4_1.2 pl_getLeading_INTERNAL_API_DO_NOT_USE
#        define pl_getLineAscent_4_1.2 pl_getLineAscent_INTERNAL_API_DO_NOT_USE
#        define pl_getLineDescent_4_1.2 pl_getLineDescent_INTERNAL_API_DO_NOT_USE
#        define pl_getLineLeading_4_1.2 pl_getLineLeading_INTERNAL_API_DO_NOT_USE
#        define pl_getLineVisualRun_4_1.2 pl_getLineVisualRun_INTERNAL_API_DO_NOT_USE
#        define pl_getLineWidth_4_1.2 pl_getLineWidth_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunCount_4_1.2 pl_getLocaleRunCount_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLastLimit_4_1.2 pl_getLocaleRunLastLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLimit_4_1.2 pl_getLocaleRunLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getLocaleRunLocale_4_1.2 pl_getLocaleRunLocale_INTERNAL_API_DO_NOT_USE
#        define pl_getParagraphLevel_4_1.2 pl_getParagraphLevel_INTERNAL_API_DO_NOT_USE
#        define pl_getTextDirection_4_1.2 pl_getTextDirection_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunCount_4_1.2 pl_getValueRunCount_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLastLimit_4_1.2 pl_getValueRunLastLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunLimit_4_1.2 pl_getValueRunLimit_INTERNAL_API_DO_NOT_USE
#        define pl_getValueRunValue_4_1.2 pl_getValueRunValue_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunAscent_4_1.2 pl_getVisualRunAscent_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDescent_4_1.2 pl_getVisualRunDescent_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunDirection_4_1.2 pl_getVisualRunDirection_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunFont_4_1.2 pl_getVisualRunFont_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphCount_4_1.2 pl_getVisualRunGlyphCount_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphToCharMap_4_1.2 pl_getVisualRunGlyphToCharMap_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunGlyphs_4_1.2 pl_getVisualRunGlyphs_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunLeading_4_1.2 pl_getVisualRunLeading_INTERNAL_API_DO_NOT_USE
#        define pl_getVisualRunPositions_4_1.2 pl_getVisualRunPositions_INTERNAL_API_DO_NOT_USE
#        define pl_isComplex_4_1.2 pl_isComplex_INTERNAL_API_DO_NOT_USE
#        define pl_line_4_1.2 pl_line_INTERNAL_API_DO_NOT_USE
#        define pl_nextLine_4_1.2 pl_nextLine_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyFontRuns_4_1.2 pl_openEmptyFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyLocaleRuns_4_1.2 pl_openEmptyLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openEmptyValueRuns_4_1.2 pl_openEmptyValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openFontRuns_4_1.2 pl_openFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openLocaleRuns_4_1.2 pl_openLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_openValueRuns_4_1.2 pl_openValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_paragraph_4_1.2 pl_paragraph_INTERNAL_API_DO_NOT_USE
#        define pl_reflow_4_1.2 pl_reflow_INTERNAL_API_DO_NOT_USE
#        define pl_resetFontRuns_4_1.2 pl_resetFontRuns_INTERNAL_API_DO_NOT_USE
#        define pl_resetLocaleRuns_4_1.2 pl_resetLocaleRuns_INTERNAL_API_DO_NOT_USE
#        define pl_resetValueRuns_4_1.2 pl_resetValueRuns_INTERNAL_API_DO_NOT_USE
#        define pl_visualRun_4_1.2 pl_visualRun_INTERNAL_API_DO_NOT_USE
#        define ucol_equals_4_1.2 ucol_equals_INTERNAL_API_DO_NOT_USE
#        define ucol_forgetUCA_4_1.2 ucol_forgetUCA_INTERNAL_API_DO_NOT_USE
#        define ucol_getAttributeOrDefault_4_1.2 ucol_getAttributeOrDefault_INTERNAL_API_DO_NOT_USE
#        define ucol_getUnsafeSet_4_1.2 ucol_getUnsafeSet_INTERNAL_API_DO_NOT_USE
#        define ucol_nextProcessed_4_1.2 ucol_nextProcessed_INTERNAL_API_DO_NOT_USE
#        define ucol_prepareShortStringOpen_4_1.2 ucol_prepareShortStringOpen_INTERNAL_API_DO_NOT_USE
#        define ucol_previousProcessed_4_1.2 ucol_previousProcessed_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultCodepage_4_1.2 uprv_getDefaultCodepage_INTERNAL_API_DO_NOT_USE
#        define uprv_getDefaultLocaleID_4_1.2 uprv_getDefaultLocaleID_INTERNAL_API_DO_NOT_USE
#        define ures_openFillIn_4_1.2 ures_openFillIn_INTERNAL_API_DO_NOT_USE
#        define usearch_searchBackwards_4_1.2 usearch_searchBackwards_INTERNAL_API_DO_NOT_USE
#        define usearch_search_4_1.2 usearch_search_INTERNAL_API_DO_NOT_USE
#        define utf8_appendCharSafeBody_4_1.2 utf8_appendCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_back1SafeBody_4_1.2 utf8_back1SafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_countTrailBytes_4_1.2 utf8_countTrailBytes_INTERNAL_API_DO_NOT_USE
#        define utf8_nextCharSafeBody_4_1.2 utf8_nextCharSafeBody_INTERNAL_API_DO_NOT_USE
#        define utf8_prevCharSafeBody_4_1.2 utf8_prevCharSafeBody_INTERNAL_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_INTERNAL_API */
#endif /* UINTRNAL_H */

