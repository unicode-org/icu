/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
 *
 * File CTSTDEP.C
 *
 * Modification History:
 *        Name                     Description
 *    Ram Viswanadha              Creation
 *
 * Test for deprecated formatting APIs.
 *
 * Note: This test tests all deprecated C API which follow the method of application 
 * complication using preporcessor magic suggested by SRL.Compilation of this test 
 * will fail for every release when U_USE_DEPRECATED_FORMAT_API is set, as a reminder  
 * for updating macros for deprecated APIs and eventual removal of those APIs.
 *********************************************************************************
 */

#include "unicode/unum.h"
#include "unicode/udat.h"
#include "unicode/ustring.h"
#include "unicode/utrans.h"
#include "unicode/uchar.h"
#include <string.h>
#include "cintltst.h"
#include <stdlib.h>
#include <stdio.h>


#ifdef U_USE_DEPRECATED_FORMAT_API
static void TestDeprecatedNumFmtAPI(void);
static void TestDeprecatedDateFmtAPI(void);
static void TestDeprecatedUErrorCode(void);
static void TestDeprecatedUCharScript(void);
#endif

void addTestDeprecatedAPI(TestNode** root);

void 
addTestDeprecatedAPI(TestNode** root)
{
#ifdef U_USE_DEPRECATED_FORMAT_API
    addTest(root, &TestDeprecatedNumFmtAPI,   "ctstdep/TestDeprecatedNumFmtAPI");
    addTest(root, &TestDeprecatedDateFmtAPI,  "ctstdep/TestDeprecatedDateFmtAPI");
    addTest(root, &TestDeprecatedUErrorCode,  "ctstdep/TestDeprecatedUErrorCode");
    addTest(root, &TestDeprecatedUCharScript, "ctstdep/TestDeprecatedUCharScript");
#endif
}

#ifdef U_USE_DEPRECATED_FORMAT_API
/*
 *TODO: The unum_open,unum_applyPattern, which does not take UParseError as one of their params
 *and unum_openPattern methods have been deprecated in 2.0 release.Please remove this API by 10/1/2002
 */

static void 
TestDeprecatedNumFmtAPI(void)
{
    int32_t pat_length, i, lneed;
    UNumberFormat *fmt;
    UChar upat[5];
    UChar unewpat[5];
    UChar unum[5];
    UChar *unewp=NULL;
    UChar *str=NULL;
    UErrorCode status = U_ZERO_ERROR;
    const char* pat[]    = { "#.#", "#.", ".#", "#" };
    const char* newpat[] = { "#0.#", "#0.", "#.0", "#" };
    const char* num[]    = { "0",   "0.", ".0", "0" };

    log_verbose("\nTesting different format patterns\n");
    pat_length = sizeof(pat) / sizeof(pat[0]);
    for (i=0; i < pat_length; ++i)
    {
        status = U_ZERO_ERROR;
        u_uastrcpy(upat, pat[i]);
        fmt= unum_openPattern(upat, u_strlen(upat), "en_US", &status);
        if (U_FAILURE(status)) {
            log_err("FAIL: Number format constructor failed for pattern %s\n", pat[i]);
            continue; 
        }
        lneed=0;
        lneed=unum_toPattern(fmt, FALSE, NULL, lneed, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status= U_ZERO_ERROR;
            unewp=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
            unum_toPattern(fmt, FALSE, unewp, lneed+1, &status);
        }
        if(U_FAILURE(status)){
            log_err("FAIL: Number format extracting the pattern failed for %s\n", pat[i]);
        }
        u_uastrcpy(unewpat, newpat[i]);
        if(u_strcmp(unewp, unewpat) != 0)
            log_err("FAIL: Pattern  %s should be transmute to %s; %s seen instead\n", pat[i], newpat[i],  austrdup(unewp) );

        lneed=0;
        lneed=unum_format(fmt, 0, NULL, lneed, NULL, &status);
        if(status==U_BUFFER_OVERFLOW_ERROR){
            status=U_ZERO_ERROR;
            str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
            unum_format(fmt, 0, str, lneed+1,  NULL, &status);
        }
        if(U_FAILURE(status)) {
            log_err("Error in formatting using unum_format(.....): %s\n", myErrorName(status) );
        }
        u_uastrcpy(unum, num[i]);
        if (u_strcmp(str, unum) != 0)
        {
            log_err("FAIL: Pattern %s should format zero as %s; %s Seen instead\n", pat[i], num[i], austrdup(str) );
        }
        free(unewp);
        free(str);
        unum_close(fmt);
    }

    {
        const char* locale[]={"fr_CA", "de_DE_PREEURO", "fr_FR_PREEURO"};
        const char* result[]={"1,50 $", "1,50 DM", "1,50 F"};
        UNumberFormat *currencyFmt;
        UChar *res=NULL;
        UFieldPosition pos;
        status = U_ZERO_ERROR;
        log_verbose("\nTesting the number format with different currency patterns\n");
        for(i=0; i < 3; i++)
        {
            currencyFmt = unum_open(UNUM_CURRENCY, locale[i], &status);
            if(U_FAILURE(status)){
                log_err("Error in the construction of number format with style currency:\n%s\n",
                    myErrorName(status));
            }
            lneed=0;
            lneed= unum_formatDouble(currencyFmt, 1.50, NULL, lneed, NULL, &status);
            if(status==U_BUFFER_OVERFLOW_ERROR){
                status=U_ZERO_ERROR;
                str=(UChar*)malloc(sizeof(UChar) * (lneed+1) );
                pos.field = 0;
                unum_formatDouble(currencyFmt, 1.50, str, lneed+1, &pos, &status);
            }
            if(U_FAILURE(status)) {
                log_err("Error in formatting using unum_formatDouble(.....): %s\n", myErrorName(status) );
            }
            res=(UChar*)malloc(sizeof(UChar) * (strlen(result[i])+1) );
            u_uastrcpy(res, result[i]);
            if (u_strcmp(str, res) != 0)
                log_err("FAIL: Expected %s\n", result[i]);
            unum_close(currencyFmt);
            free(str);
            free(res);
        }
    }
}

/*
 *TODO: udat_open and udat_openPatterns methods have been unified and deprecated in 2.0 release.
 *Please remove this API by 10/1/2002.
 */
static void 
TestDeprecatedDateFmtAPI(void)
{
    UDateFormat *def, *fr, *fr_pat ;
    UErrorCode status = U_ZERO_ERROR;
    UChar temp[30];

    fr = udat_open(UDAT_FULL, UDAT_DEFAULT, "fr_FR", NULL,0, &status);
    if(U_FAILURE(status))
    {
        log_err("FAIL: error in creating the dateformat using full time style with french locale\n %s\n", 
            myErrorName(status) );
    }
    /* this is supposed to open default date format, but later on it treats it like it is "en_US" 
       - very bad if you try to run the tests on machine where default locale is NOT "en_US" */
    /* def = udat_open(UDAT_SHORT, UDAT_SHORT, NULL, NULL, 0, &status); */
    def = udat_open(UDAT_SHORT, UDAT_SHORT, "en_US", NULL, 0, &status);
    if(U_FAILURE(status))
    {
        log_err("FAIL: error in creating the dateformat using short date and time style\n %s\n", 
            myErrorName(status) );
    }

    /*Testing udat_openPattern()  */
    status=U_ZERO_ERROR;
    log_verbose("\nTesting the udat_openPattern with a specified pattern\n");
    /*for french locale */
    fr_pat=udat_openPattern(temp, u_strlen(temp), "fr_FR", &status);
    if(U_FAILURE(status))
    {
        log_err("FAIL: Error in creating a date format using udat_openPattern \n %s\n", 
            myErrorName(status) );
    }
    else
        log_verbose("PASS: creating dateformat using udat_openPattern() succesful\n");

    udat_close(fr);
    udat_close(def);
    udat_close(fr_pat);
}

static void
TestDeprecatedUCharScript(void)
{
    const UCharScript scriptArray[] = {
        /* Script names */
        /** */
        U_BASIC_LATIN,
        /** */
        U_LATIN_1_SUPPLEMENT,
        /** */
        U_LATIN_EXTENDED_A,
        /** */
        U_LATIN_EXTENDED_B,
        /** */
        U_IPA_EXTENSIONS,
        /** */
        U_SPACING_MODIFIER_LETTERS,
        /** */
        U_COMBINING_DIACRITICAL_MARKS,
        /** */
        U_GREEK,
        /** */
        U_CYRILLIC,
        /** */
        U_ARMENIAN,
        /** */
        U_HEBREW,
        /** */
        U_ARABIC,
        /** */
        U_SYRIAC,
        /** */
        U_THAANA,
        /** */
        U_DEVANAGARI,
        /** */
        U_BENGALI,
        /** */
        U_GURMUKHI,
        /** */
        U_GUJARATI,
        /** */
        U_ORIYA,
        /** */
        U_TAMIL,
        /** */
        U_TELUGU,
        /** */
        U_KANNADA,
        /** */
        U_MALAYALAM,
        /** */
        U_SINHALA,
        /** */
        U_THAI,
        /** */
        U_LAO,
        /** */
        U_TIBETAN,
        /** */
        U_MYANMAR,
        /** */
        U_GEORGIAN,
        /** */
        U_HANGUL_JAMO,
        /** */
        U_ETHIOPIC,
        /** */
        U_CHEROKEE,
        /** */
        U_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS,
        /** */
        U_OGHAM,
        /** */
        U_RUNIC,
        /** */
        U_KHMER,
        /** */
        U_MONGOLIAN,
        /** */
        U_LATIN_EXTENDED_ADDITIONAL,
        /** */
        U_GREEK_EXTENDED,
        /** */
        U_GENERAL_PUNCTUATION,
        /** */
        U_SUPERSCRIPTS_AND_SUBSCRIPTS,
        /** */
        U_CURRENCY_SYMBOLS,
        /** */
        U_COMBINING_MARKS_FOR_SYMBOLS,
        /** */
        U_LETTERLIKE_SYMBOLS,
        /** */
        U_NUMBER_FORMS,
        /** */
        U_ARROWS,
        /** */
        U_MATHEMATICAL_OPERATORS,
        /** */
        U_MISCELLANEOUS_TECHNICAL,
        /** */
        U_CONTROL_PICTURES,
        /** */
        U_OPTICAL_CHARACTER_RECOGNITION,
        /** */
        U_ENCLOSED_ALPHANUMERICS,
        /** */
        U_BOX_DRAWING,
        /** */
        U_BLOCK_ELEMENTS,
        /** */
        U_GEOMETRIC_SHAPES,
        /** */
        U_MISCELLANEOUS_SYMBOLS,
        /** */
        U_DINGBATS,
        /** */
        U_BRAILLE_PATTERNS,
        /** */
        U_CJK_RADICALS_SUPPLEMENT,
        /** */
        U_KANGXI_RADICALS,
        /** */
        U_IDEOGRAPHIC_DESCRIPTION_CHARACTERS,
        /** */
        U_CJK_SYMBOLS_AND_PUNCTUATION,
        /** */
        U_HIRAGANA,
        /** */
        U_KATAKANA,
        /** */
        U_BOPOMOFO,
        /** */
        U_HANGUL_COMPATIBILITY_JAMO,
        /** */
        U_KANBUN,
        /** */
        U_BOPOMOFO_EXTENDED,
        /** */
        U_ENCLOSED_CJK_LETTERS_AND_MONTHS,
        /** */
        U_CJK_COMPATIBILITY,
        /** */
        U_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        /** */
        U_CJK_UNIFIED_IDEOGRAPHS,
        /** */
        U_YI_SYLLABLES,
        /** */
        U_YI_RADICALS,
        /** */
        U_HANGUL_SYLLABLES,
        /** */
        U_HIGH_SURROGATES,
        /** */
        U_HIGH_PRIVATE_USE_SURROGATES,
        /** */
        U_LOW_SURROGATES,
        /** */
        U_PRIVATE_USE_AREA,
        /** */
        U_CJK_COMPATIBILITY_IDEOGRAPHS,
        /** */
        U_ALPHABETIC_PRESENTATION_FORMS,
        /** */
        U_ARABIC_PRESENTATION_FORMS_A,
        /** */
        U_COMBINING_HALF_MARKS,
        /** */
        U_CJK_COMPATIBILITY_FORMS,
        /** */
        U_SMALL_FORM_VARIANTS,
        /** */
        U_ARABIC_PRESENTATION_FORMS_B,
        /** */
        U_SPECIALS,
        /** */
        U_HALFWIDTH_AND_FULLWIDTH_FORMS,
        /** */
        U_CHAR_SCRIPT_COUNT,
        /** */
        U_NO_SCRIPT
    };
    if (UBLOCK_BASIC_LATIN != scriptArray[0]) {
        log_err("UBLOCK_BASIC_LATIN != U_BASIC_LATIN");
    }
}

static void
TestDeprecatedUErrorCode(void){
    const UErrorCode code[]= {
        U_ERROR_INFO_START,
        U_USING_FALLBACK_ERROR,
        U_USING_DEFAULT_ERROR,
        U_SAFECLONE_ALLOCATED_ERROR,
        U_ERROR_INFO_LIMIT,
        U_ZERO_ERROR,
        U_ILLEGAL_ARGUMENT_ERROR,
        U_MISSING_RESOURCE_ERROR,
        U_INVALID_FORMAT_ERROR,
        U_FILE_ACCESS_ERROR,
        U_INTERNAL_PROGRAM_ERROR,
        U_MESSAGE_PARSE_ERROR,
        U_MEMORY_ALLOCATION_ERROR,
        U_INDEX_OUTOFBOUNDS_ERROR,
        U_PARSE_ERROR,
        U_INVALID_CHAR_FOUND,
        U_TRUNCATED_CHAR_FOUND,
        U_ILLEGAL_CHAR_FOUND,
        U_INVALID_TABLE_FORMAT,
        U_INVALID_TABLE_FILE,
        U_BUFFER_OVERFLOW_ERROR,
        U_UNSUPPORTED_ERROR,
        U_RESOURCE_TYPE_MISMATCH,
        U_ILLEGAL_ESCAPE_SEQUENCE,
        U_UNSUPPORTED_ESCAPE_SEQUENCE,
        U_NO_SPACE_AVAILABLE,
        U_ERROR_LIMIT,
    };
    if (U_USING_FALLBACK_WARNING != code[1]) {
        log_err("U_USING_FALLBACK_WARNING != U_USING_FALLBACK_ERROR");
    }
}
#endif
