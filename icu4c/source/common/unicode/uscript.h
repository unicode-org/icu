/*
**********************************************************************
*   Copyright (C) 1997-2001, International Business Machines
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

#include "utypes.h"
#include "ures.h"
#include "cstring.h"

typedef enum UScriptCode UScriptCode;

enum UScriptCode {
      U_INVALID_CODE = -1,
      U_COMMON      =  0 , /* Zyyy */
      U_INHERITED   =  1,  /* Qaai */
      U_ARABIC      =  2,  /* Arab */
      U_ARMENIAN    =  3,  /* Armn */
      U_BENGALI     =  4,  /* Beng */
      U_BOPOMOFO    =  5,  /* Bopo */
      U_CHEROKEE    =  6,  /* Cher */
      U_COPTIC      =  7,  /* Qaac */
      U_CYRILLIC    =  8,  /* Cyrl (Cyrs) */
      U_DESERET     =  9,  /* Dsrt */
      U_DEVANAGARI  = 10,  /* Deva */
      U_ETHIOPIC    = 11,  /* Ethi */
      U_GEORGIAN    = 12,  /* Geor (Geon, Geoa) */
      U_GOTHIC      = 13,  /* Goth */
      U_GREEK       = 14,  /* Grek */
      U_GUJARATI    = 15,  /* Gujr */
      U_GURMUKHI    = 16,  /* Guru */
      U_HAN         = 17,  /* Hani */
      U_HANGUL      = 18,  /* Hang */
      U_HEBREW      = 19,  /* Hebr */
      U_HIRAGANA    = 20,  /* Hira */
      U_KANNADA     = 21,  /* Knda */
      U_KATAKANA    = 22,  /* Kana */
      U_KHMER       = 23,  /* Khmr */
      U_LAO         = 24,  /* Laoo */
      U_LATIN       = 25,  /* Latn (Latf, Latg) */
      U_MALAYALAM   = 26,  /* Mlym */
      U_MONGOLIAN   = 27,  /* Mong */
      U_MYANMAR     = 28,  /* Mymr */
      U_OGHAM       = 29,  /* Ogam */
      U_OLD_ITALIC  = 30,  /* Ital */
      U_ORIYA       = 31,  /* Orya */
      U_RUNIC       = 32,  /* Runr */
      U_SINHALA     = 33,  /* Sinh */
      U_SYRIAC      = 34,  /* Syrc (Syrj, Syrn, Syre) */
      U_TAMIL       = 35,  /* Taml */
      U_TELUGU      = 36,  /* Telu */
      U_THAANA      = 37,  /* Thaa */
      U_THAI        = 38,  /* Thai */
      U_TIBETAN     = 39,  /* Tibt */
      U_UCAS        = 40,  /* Cans */
      U_YI          = 41,   /* Yiii */
      U_SCRIPT_CODE_LIMIT = 43 
  };
static const char* scriptCodeName[]={
    "U_INVALID_CODE",
    "U_COMMON",    
    "U_INHERITED", 
    "U_ARABIC",    
    "U_ARMENIAN",  
    "U_BENGALI",   
    "U_BOPOMOFO",  
    "U_CHEROKEE",  
    "U_COPTIC",    
    "U_CYRILLIC",  
    "U_DESERET",   
    "U_DEVANAGARI",
    "U_ETHIOPIC",  
    "U_GEORGIAN",  
    "U_GOTHIC",    
    "U_GREEK",     
    "U_GUJARATI",  
    "U_GURMUKHI",  
    "U_HAN",       
    "U_HANGUL",   
    "U_HEBREW",    
    "U_HIRAGANA",  
    "U_KANNADA",   
    "U_KATAKANA",  
    "U_KHMER",     
    "U_LAO",       
    "U_LATIN",     
    "U_MALAYALAM", 
    "U_MONGOLIAN", 
    "U_MYANMAR",   
    "U_OGHAM",     
    "U_OLD_ITALIC",
    "U_ORIYA",     
    "U_RUNIC",     
    "U_SINHALA",   
    "U_SYRIAC",    
    "U_TAMIL",     
    "U_TELUGU",    
    "U_THAANA",    
    "U_THAI",      
    "U_TIBETAN",   
    "U_UCAS",      
    "U_YI",  
    "U_SCRIPT_CODE_LIMIT",
};



/**
 * Gets a script code associated with the given locale or ISO 15924 abbreviation or name. 
 * Returns U_MALAYAM given "Malayam" OR "Mlym".
 * Returns U_LATIN given "en" OR "en_US" 
 * @param nameOrAbbrOrLocale name of th script or ISO 15924 code or locale
 * @param err the error status code.
 * @return The UScriptCode 
 * @draft
 */
U_CAPI UScriptCode 
uchar_getScriptCode(const char* nameOrAbbrOrLocale,UErrorCode *err);

/**
 * Gets a script code name as a string given the script code. 
 * Returns "U_MALAYAM" given U_MALAYALAM
 * @param  code script code
 * @return script code as a string
 * @draft
 */
U_CAPI const char*
uchar_scriptCodeName(UScriptCode code);


/**
 * Gets a script name associated with the given script code. 
 * Returns  "Malayam" given U_MALAYAM
 * @param scriptCode UScriptCode enum
 * @return script name as a string
 * @draft
 */
U_CAPI const char* 
uchar_getScriptName(UScriptCode scriptCode);

/**
 * Gets a script name associated with the given script code. 
 * Returns  "Mlym" given U_MALAYAM
 * @param scriptCode UScriptCode enum
 * @return script abbr as a string
 * @draft
 */
U_CAPI const char* 
uchar_getScriptAbbr(UScriptCode scriptCode);

#endif


