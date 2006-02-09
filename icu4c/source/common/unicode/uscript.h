/*
**********************************************************************
*   Copyright (C) 1997-2006, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File USCRIPT.H
*
* Modification History:
*
*   Date        Name        Description
*   07/06/2001    Ram         Creation.
******************************************************************************
*/
#ifndef USCRIPT_H
#define USCRIPT_H
#include "unicode/utypes.h"

/**
 * \file
 * \brief C API: Unicode Script Information
 */
 
/**
 * Constants for ISO 15924 script codes.
 *
 * Many of these script codes - those from Unicode's ScriptNames.txt -
 * are character property values for Unicode's Script property.
 * See UAX #24 Script Names (http://www.unicode.org/reports/tr24/).
 *
 * Starting with ICU 3.6, constants for most ISO 15924 script codes
 * are included (currently excluding private-use codes Qaaa..Qabx).
 * For scripts for which there are codes in ISO 15924 but which are not
 * used in the Unicode Character Database (UCD), there are no Unicode characters
 * associated with those scripts.
 *
 * For example, there are no characters that have a UCD script code of
 * Hans or Hant. All Han ideographs have the Hani script code.
 * The Hans and Hant script codes are used with CLDR data.
 *
 * ISO 15924 script codes are included for use with CLDR and similar.
 *
 * @stable ICU 2.2
 */
typedef enum UScriptCode {
      USCRIPT_INVALID_CODE = -1,
      USCRIPT_COMMON       =  0 , /* Zyyy */
      USCRIPT_INHERITED    =  1,  /* Qaai */
      USCRIPT_ARABIC       =  2,  /* Arab */
      USCRIPT_ARMENIAN     =  3,  /* Armn */
      USCRIPT_BENGALI      =  4,  /* Beng */
      USCRIPT_BOPOMOFO     =  5,  /* Bopo */
      USCRIPT_CHEROKEE     =  6,  /* Cher */
      USCRIPT_COPTIC       =  7,  /* Copt */
      USCRIPT_CYRILLIC     =  8,  /* Cyrl */
      USCRIPT_DESERET      =  9,  /* Dsrt */
      USCRIPT_DEVANAGARI   = 10,  /* Deva */
      USCRIPT_ETHIOPIC     = 11,  /* Ethi */
      USCRIPT_GEORGIAN     = 12,  /* Geor */
      USCRIPT_GOTHIC       = 13,  /* Goth */
      USCRIPT_GREEK        = 14,  /* Grek */
      USCRIPT_GUJARATI     = 15,  /* Gujr */
      USCRIPT_GURMUKHI     = 16,  /* Guru */
      USCRIPT_HAN          = 17,  /* Hani */
      USCRIPT_HANGUL       = 18,  /* Hang */
      USCRIPT_HEBREW       = 19,  /* Hebr */
      USCRIPT_HIRAGANA     = 20,  /* Hira */
      USCRIPT_KANNADA      = 21,  /* Knda */
      USCRIPT_KATAKANA     = 22,  /* Kana */
      USCRIPT_KHMER        = 23,  /* Khmr */
      USCRIPT_LAO          = 24,  /* Laoo */
      USCRIPT_LATIN        = 25,  /* Latn */
      USCRIPT_MALAYALAM    = 26,  /* Mlym */
      USCRIPT_MONGOLIAN    = 27,  /* Mong */
      USCRIPT_MYANMAR      = 28,  /* Mymr */
      USCRIPT_OGHAM        = 29,  /* Ogam */
      USCRIPT_OLD_ITALIC   = 30,  /* Ital */
      USCRIPT_ORIYA        = 31,  /* Orya */
      USCRIPT_RUNIC        = 32,  /* Runr */
      USCRIPT_SINHALA      = 33,  /* Sinh */
      USCRIPT_SYRIAC       = 34,  /* Syrc */
      USCRIPT_TAMIL        = 35,  /* Taml */
      USCRIPT_TELUGU       = 36,  /* Telu */
      USCRIPT_THAANA       = 37,  /* Thaa */
      USCRIPT_THAI         = 38,  /* Thai */
      USCRIPT_TIBETAN      = 39,  /* Tibt */
      /** Canadian_Aboriginal script. @stable ICU 2.6 */
      USCRIPT_CANADIAN_ABORIGINAL = 40,  /* Cans */
      /** Canadian_Aboriginal script (alias). @stable ICU 2.2 */
      USCRIPT_UCAS         = USCRIPT_CANADIAN_ABORIGINAL,
      USCRIPT_YI           = 41,  /* Yiii */
      USCRIPT_TAGALOG      = 42,  /* Tglg */
      USCRIPT_HANUNOO      = 43,  /* Hano */
      USCRIPT_BUHID        = 44,  /* Buhd */
      USCRIPT_TAGBANWA     = 45,  /* Tagb */

      /* New scripts in Unicode 4 @stable ICU 2.6 */
      USCRIPT_BRAILLE      = 46,  /* Brai */
      USCRIPT_CYPRIOT      = 47,  /* Cprt */
      USCRIPT_LIMBU        = 48,  /* Limb */
      USCRIPT_LINEAR_B     = 49,  /* Linb */
      USCRIPT_OSMANYA      = 50,  /* Osma */
      USCRIPT_SHAVIAN      = 51,  /* Shaw */
      USCRIPT_TAI_LE       = 52,  /* Tale */
      USCRIPT_UGARITIC     = 53,  /* Ugar */

      /** New script code in Unicode 4.0.1 @stable ICU 3.0 */
      USCRIPT_KATAKANA_OR_HIRAGANA = 54,/*Hrkt */
      
#ifndef U_HIDE_DRAFT_API
      /* New scripts in Unicode 4.1 @draft ICU 3.4 */
      USCRIPT_BUGINESE      = 55, /* Bugi */
      USCRIPT_GLAGOLITIC    = 56, /* Glag */
      USCRIPT_KHAROSHTHI    = 57, /* Khar */
      USCRIPT_SYLOTI_NAGRI  = 58, /* Sylo */
      USCRIPT_NEW_TAI_LUE   = 59, /* Talu */
      USCRIPT_TIFINAGH      = 60, /* Tfng */
      USCRIPT_OLD_PERSIAN   = 61, /* Xpeo */

      /* end of script codes that are in Unicode */
      USCRIPT_UNICODE_LIMIT = 62, 

      /* New script codes from ISO 15924 @draft ICU 3.6 */
      USCRIPT_BALINESE      = 63, /* Bali */
      USCRIPT_BATAK         = 64, /* Batk */
      USCRIPT_BLISSYMBOLS   = 65, /* Blis */
      USCRIPT_BRAHMI        = 66, /* Brah */
      USCRIPT_CHAM          = 67, /* Cham */
      USCRIPT_CIRTH         = 68, /* Cirt */
      USCRIPT_CYRS          = 69, /* Cyrs */
      USCRIPT_EGYD          = 70, /* Egyd */
      USCRIPT_EGYH          = 71, /* Egyh */
      USCRIPT_EGYP          = 72, /* Egyp */
      USCRIPT_GEOK          = 73, /* Geok */
      USCRIPT_HANS          = 74, /* Hans */
      USCRIPT_HANT          = 75, /* Hant */
      USCRIPT_HMNG          = 76, /* Hmng */
      USCRIPT_HUNG          = 77, /* Hung */
      USCRIPT_INDS          = 78, /* Inds */
      USCRIPT_JAVANESE      = 79, /* Java */
      USCRIPT_KALI          = 80, /* Kali */
      USCRIPT_LATF          = 81, /* Latf */
      USCRIPT_LATG          = 82, /* Latg */
      USCRIPT_LEPC          = 83, /* Lepc */
      USCRIPT_LINA          = 84, /* Lina */
      USCRIPT_MANDAEAN      = 85, /* Mand */
      USCRIPT_MAYA          = 86, /* Maya */
      USCRIPT_MEROITIC      = 87, /* Mero */
      USCRIPT_NKOO          = 88, /* Nkoo */
      USCRIPT_ORKHON        = 89, /* Orkh */
      USCRIPT_PERM          = 90, /* Perm */
      USCRIPT_PHAGS_PA      = 91, /* Phag */
      USCRIPT_PHOENICIAN    = 92, /* Phnx */
      USCRIPT_PLRD          = 93, /* Plrd */
      USCRIPT_RONGORONGO    = 94, /* Roro */
      USCRIPT_SARATI        = 95, /* Sara */
      USCRIPT_SYRE          = 96, /* Syre */
      USCRIPT_SYRJ          = 97, /* Syrj */
      USCRIPT_SYRN          = 98, /* Syrn */
      USCRIPT_TENGWAR       = 99, /* Teng */
      USCRIPT_VAI           = 100, /* Vaii */
      USCRIPT_VISP          = 101, /* Visp */
      USCRIPT_XSUX          = 102,/* Xsux */
      USCRIPT_ZXXX          = 103,/* Zxxx */
      USCRIPT_ZZZZ          = 104,/* Zzzz */
      /* Private use codes from Qaaa - Qabx are not supported*/
#endif /* U_HIDE_DRAFT_API */
      USCRIPT_CODE_LIMIT    = 105
} UScriptCode;

/**
 * Gets script codes associated with the given locale or ISO 15924 abbreviation or name. 
 * Fills in USCRIPT_MALAYALAM given "Malayam" OR "Mlym".
 * Fills in USCRIPT_LATIN given "en" OR "en_US" 
 * If required capacity is greater than capacity of the destination buffer then the error code
 * is set to U_BUFFER_OVERFLOW_ERROR and the required capacity is returned
 *
 * <p>Note: To search by short or long script alias only, use
 * u_getPropertyValueEnum(UCHAR_SCRIPT, alias) instead.  This does
 * a fast lookup with no access of the locale data.
 * @param nameOrAbbrOrLocale name of the script, as given in
 * PropertyValueAliases.txt, or ISO 15924 code or locale
 * @param fillIn the UScriptCode buffer to fill in the script code
 * @param capacity the capacity (size) fo UScriptCode buffer passed in.
 * @param err the error status code.
 * @return The number of script codes filled in the buffer passed in 
 * @stable ICU 2.4
 */
U_STABLE int32_t  U_EXPORT2 
uscript_getCode(const char* nameOrAbbrOrLocale,UScriptCode* fillIn,int32_t capacity,UErrorCode *err);

/**
 * Gets a script name associated with the given script code. 
 * Returns  "Malayam" given USCRIPT_MALAYALAM
 * @param scriptCode UScriptCode enum
 * @return script long name as given in
 * PropertyValueAliases.txt, or NULL if scriptCode is invalid
 * @stable ICU 2.4
 */
U_STABLE const char*  U_EXPORT2 
uscript_getName(UScriptCode scriptCode);

/**
 * Gets a script name associated with the given script code. 
 * Returns  "Mlym" given USCRIPT_MALAYALAM
 * @param scriptCode UScriptCode enum
 * @return script abbreviated name as given in
 * PropertyValueAliases.txt, or NULL if scriptCode is invalid
 * @stable ICU 2.4
 */
U_STABLE const char*  U_EXPORT2 
uscript_getShortName(UScriptCode scriptCode);

/** 
 * Gets the script code associated with the given codepoint.
 * Returns USCRIPT_MALAYALAM given 0x0D02 
 * @param codepoint UChar32 codepoint
 * @param err the error status code.
 * @return The UScriptCode, or 0 if codepoint is invalid 
 * @stable ICU 2.4
 */
U_STABLE UScriptCode  U_EXPORT2 
uscript_getScript(UChar32 codepoint, UErrorCode *err);

#endif


