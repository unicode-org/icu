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

/**
 * Constants for Unicode script values from ScriptNames.txt .
 *
 * @draft ICU 2.0
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
      USCRIPT_COPTIC       =  7,  /* Qaac */
      USCRIPT_CYRILLIC     =  8,  /* Cyrl (Cyrs) */
      USCRIPT_DESERET      =  9,  /* Dsrt */
      USCRIPT_DEVANAGARI   = 10,  /* Deva */
      USCRIPT_ETHIOPIC     = 11,  /* Ethi */
      USCRIPT_GEORGIAN     = 12,  /* Geor (Geon, Geoa) */
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
      USCRIPT_LATIN        = 25,  /* Latn (Latf, Latg) */
      USCRIPT_MALAYALAM    = 26,  /* Mlym */
      USCRIPT_MONGOLIAN    = 27,  /* Mong */
      USCRIPT_MYANMAR      = 28,  /* Mymr */
      USCRIPT_OGHAM        = 29,  /* Ogam */
      USCRIPT_OLD_ITALIC   = 30,  /* Ital */
      USCRIPT_ORIYA        = 31,  /* Orya */
      USCRIPT_RUNIC        = 32,  /* Runr */
      USCRIPT_SINHALA      = 33,  /* Sinh */
      USCRIPT_SYRIAC       = 34,  /* Syrc (Syrj, Syrn, Syre) */
      USCRIPT_TAMIL        = 35,  /* Taml */
      USCRIPT_TELUGU       = 36,  /* Telu */
      USCRIPT_THAANA       = 37,  /* Thaa */
      USCRIPT_THAI         = 38,  /* Thai */
      USCRIPT_TIBETAN      = 39,  /* Tibt */
      USCRIPT_UCAS         = 40,  /* Cans */
      USCRIPT_YI           = 41,  /* Yiii */
      USCRIPT_TAGALOG      = 42,  /* Tglg */
      USCRIPT_HANUNOO      = 43,  /* Hano */
      USCRIPT_BUHID        = 44,  /* Buhd */
      USCRIPT_TAGBANWA     = 45,  /* Tagb */
      USCRIPT_CODE_LIMIT
} UScriptCode;

/**
 * Gets script codes associated with the given locale or ISO 15924 abbreviation or name. 
 * Fills in USCRIPT_MALAYALAM given "Malayam" OR "Mlym".
 * Fills in USCRIPT_LATIN given "en" OR "en_US" 
 * If required capacity is greater than capacity of the destination buffer then the error code
 * is set to U_BUFFER_OVERFLOW_ERROR and the required capacity is returned
 * @param nameOrAbbrOrLocale name of the script or ISO 15924 code or locale
 * @param fillIn the UScriptCode buffer to fill in the script code
 * @param capacity the capacity (size) fo UScriptCode buffer passed in.
 * @param err the error status code.
 * @return The number of script codes filled in the buffer passed in 
 * @draft ICU 2.0
 */
U_CAPI int32_t  U_EXPORT2 
uscript_getCode(const char* nameOrAbbrOrLocale,UScriptCode* fillIn,int32_t capacity,UErrorCode *err);

/**
 * Gets a script name associated with the given script code. 
 * Returns  "Malayam" given USCRIPT_MALAYALAM
 * @param scriptCode UScriptCode enum
 * @return script name as a string in full as given in TR#24
 * @draft ICU 2.0
 */
U_CAPI const char*  U_EXPORT2 
uscript_getName(UScriptCode scriptCode);

/**
 * Gets a script name associated with the given script code. 
 * Returns  "Mlym" given USCRIPT_MALAYALAM
 * @param scriptCode UScriptCode enum
 * @return script abbreviated name as a string  as given in TR#24
 * @draft ICU 2.0
 */
U_CAPI const char*  U_EXPORT2 
uscript_getShortName(UScriptCode scriptCode);

/** 
 * Gets the script code associated with the given codepoint.
 * Returns USCRIPT_MALAYALAM given 0x0D02 
 * @param codepoint UChar32 codepoint
 * @param err the error status code.
 * @return The UScriptCode 
 * @draft ICU 2.0
 */
U_CAPI UScriptCode  U_EXPORT2 
uscript_getScript(UChar32 codepoint, UErrorCode *err);

/**
 * <code>UScriptRun</code> is used to find runs of characters in
 * the same script. It implements a simple iterator over an array
 * of characters. The iterator will resolve script-neutral characters
 * like punctuation into the script of the surrounding characters.
 *
 * The iterator will try to match paired punctuation. If it sees an
 * opening punctuation character, it will remember the script that
 * was assigned to that character, and assign the same script to the
 * matching closing punctuation.
 *
 * Scripts are chosen based on the <code>UScriptCode</code> enumeration.
 * No attempt is made to combine related scripts into a single run. In
 * particular, Hiragana, Katakana, and Han characters will appear in seperate
 * runs.

 * Here is an example of how to iterate over script runs:
 * <pre>
 * \code
 * void printScriptRuns(const UChar *text, int32_t length)
 * {
 *     UErrorCode error = U_ZERO_ERROR;
 *     UScriptRun *scriptRun = uscript_openRun(text, testLength, &error);
 *     int32_t start = 0, limit = 0;
 *     UScriptCode code = USCRIPT_INVALID_CODE;
 *
 *     while (uscript_nextRun(&start, &limit, &code)) {
 *         printf("Script '%s' from %d to %d.\n", uscript_getName(code), start, limit);
 *     }
 *
 *     uscript_closeRun(scriptRun);
 *  }
 * </pre>
 *
 * @draft ICU 2.2
 */
struct UScriptRun;

typedef struct UScriptRun UScriptRun;

/**
 * Create a <code>UScriptRun</code> object for iterating over the given text. This object must
 * be freed using <code>uscript_closeRun()</code>. Note that this object does not copy the source text,
 * only the pointer to it. You must make sure that the pointer remains valid until you call
 * <code>uscript_closeRun()</code> or <code>uscript_setRunText()</code>.
 *
 * @param src is the address of the array of characters over which to iterate.
 *        if <code>src == NULL</code> and <code>length == 0</code>,
 *        an empty <code>UScriptRun</code> object will be returned.
 *
 * @param length is the number of characters over which to iterate.
 *
 * @param pErrorCode is a pointer to a valid <code>UErrorCode</code> value. If this value
 *        indicates a failure on entry, the function will immediately return.
 *        On exit the value will indicate the success of the operation.
 *
 * @return the address of <code>UScriptRun</code> object which will iterate over the text,
 *         or <code>NULL</code> if the operation failed.
 *
 * @draft ICU 2.2
 */
U_CAPI UScriptRun * U_EXPORT2
uscript_openRun(const UChar *src, int32_t length, UErrorCode *pErrorCode);

/**
 * Frees the given <code>UScriptRun</code> object and any storage associated with it.
 * On return, scriptRun no longer points to a valid <code>UScriptRun</code> object.
 *
 * @param scriptRun is the <code>UScriptRun</code> object which will be freed.
 *
 * @draft ICU 2.2
 */
U_CAPI void U_EXPORT2
uscript_closeRun(UScriptRun *scriptRun);

/**
 * Reset the <code>UScriptRun</code> object so that it will start iterating from
 * the beginning.
 *
 * @param scriptRun is the address of the <code>UScriptRun</code> object to be reset.
 *
 * @draft ICU 2.2
 */
U_CAPI void U_EXPORT2
uscript_resetRun(UScriptRun *scriptRun);

/**
 * Change the text over which the given <code>UScriptRun</code> object iterates.
 *
 * @param scriptRun is the <code>UScriptRun</code> object which will be changed.
 *
 * @param src is the address of the new array of characters over which to iterate.
 *        If <code>src == NULL</code> and <code>length == 0</code>,
 *        the <code>UScriptRun</code> object will become empty.
 *
 * @param length is the new number of characters over which to iterate
 *
 * @param pErrorCode is a pointer to a valid <code>UErrorCode</code> value. If this value
 *        indicates a failure on entry, the function will immediately return.
 *        On exit the value will indicate the success of the operation.
 *
 * @draft ICU 2.2
 */
U_CAPI void U_EXPORT2
uscript_setRunText(UScriptRun *scriptRun, const UChar *src, int32_t length, UErrorCode *pErrorCode);

/**
 * Advance the <code>UScriptRun</code> object to the next script run, return the start and limit
 * offsets, and the script of the run.
 *
 * @param scriptRun is the address of the <code>UScriptRun</code> object.
 *
 * @param pRunStart is a pointer to the variable to receive the starting offset of the next run.
 *        This pointer can be <code>NULL</code> if the value is not needed.
 *
 * @param pRunLimit is a pointer to the variable to receive the limit offset of the next run.
 *        This pointer can be <code>NULL</code> if the value is not needed.
 *
 * @param pRunScript is a pointer to the variable to receive the UScriptCode for the
 *        script of the current run. This pointer can be <code>NULL</code> if the value is not needed.
 *
 * @return true if there was another script run.
 *
 * @draft ICU 2.2
 */
U_CAPI UBool U_EXPORT2
uscript_nextRun(UScriptRun *scriptRun, int32_t *pRunStart, int32_t *pRunLimit, UScriptCode *pRunScript);

#endif


