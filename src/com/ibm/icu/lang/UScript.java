/**
*******************************************************************************
* Copyright (C) 2001-2004, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.lang;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import java.util.Locale;
import java.util.MissingResourceException;

/**
 * A class to reflect UTR #24: Script Names
 * (based on ISO 15924:2000, "Code for the representation of names of
 * scripts").  UTR #24 describes the basis for a new Unicode data file,
 * Scripts.txt.
 * @stable ICU 2.4
 */
public final class UScript {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a92001 IBM Corp.  All rights reserved.";

    /**
     * Invalid code
     * @stable ICU 2.4
     */
    public static final int INVALID_CODE = -1;
    /**
     * Common
     * @stable ICU 2.4
     */
    public static final int COMMON       =  0;  /* Zyyy */
    /**
     * Inherited
     * @stable ICU 2.4
     */
    public static final int INHERITED    =  1;  /* Qaai */
    /**
     * Arabic
     * @stable ICU 2.4
     */
    public static final int ARABIC       =  2;  /* Arab */
    /**
     * Armenian
     * @stable ICU 2.4
     */
    public static final int ARMENIAN     =  3;  /* Armn */
    /**
     * Bengali
     * @stable ICU 2.4
     */
    public static final int BENGALI      =  4;  /* Beng */
    /**
     * Bopomofo
     * @stable ICU 2.4
     */
    public static final int BOPOMOFO     =  5;  /* Bopo */
    /**
     * Cherokee
     * @stable ICU 2.4
     */
    public static final int CHEROKEE     =  6;  /* Cher */
    /**
     * Coptic
     * @stable ICU 2.4
     */
    public static final int COPTIC       =  7;  /* Qaac */
    /**
     * Cyrillic
     * @stable ICU 2.4
     */
    public static final int CYRILLIC     =  8;  /* Cyrl (Cyrs) */
    /**
     * Deseret
     * @stable ICU 2.4
     */
    public static final int DESERET      =  9;  /* Dsrt */
    /**
     * Devanagari
     * @stable ICU 2.4
     */
    public static final int DEVANAGARI   = 10;  /* Deva */
    /**
     * Ethiopic
     * @stable ICU 2.4
     */
    public static final int ETHIOPIC     = 11;  /* Ethi */
    /**
     * Georgian
     * @stable ICU 2.4
     */
    public static final int GEORGIAN     = 12;  /* Geor (Geon; Geoa) */
    /**
     * Gothic
     * @stable ICU 2.4
     */
    public static final int GOTHIC       = 13;  /* Goth */
    /**
     * Greek
     * @stable ICU 2.4
     */
    public static final int GREEK        = 14;  /* Grek */
    /**
     * Gujarati
     * @stable ICU 2.4
     */
    public static final int GUJARATI     = 15;  /* Gujr */
    /**
     * Gurmukhi
     * @stable ICU 2.4
     */
    public static final int GURMUKHI     = 16;  /* Guru */
    /**
     * Han
     * @stable ICU 2.4
     */
    public static final int HAN          = 17;  /* Hani */
    /**
     * Hangul
     * @stable ICU 2.4
     */
    public static final int HANGUL       = 18;  /* Hang */
    /**
     * Hebrew
     * @stable ICU 2.4
     */
    public static final int HEBREW       = 19;  /* Hebr */
    /**
     * Hiragana
     * @stable ICU 2.4
     */
    public static final int HIRAGANA     = 20;  /* Hira */
    /**
     * Kannada
     * @stable ICU 2.4
     */
    public static final int KANNADA      = 21;  /* Knda */
    /**
     * Katakana
     * @stable ICU 2.4
     */
    public static final int KATAKANA     = 22;  /* Kana */
    /**
     * Khmer
     * @stable ICU 2.4
     */
    public static final int KHMER        = 23;  /* Khmr */
    /**
     * Lao
     * @stable ICU 2.4
     */
    public static final int LAO          = 24;  /* Laoo */
    /**
     * Latin
     * @stable ICU 2.4
     */
    public static final int LATIN        = 25;  /* Latn (Latf; Latg) */
    /**
     * Malayalam
     * @stable ICU 2.4
     */
    public static final int MALAYALAM    = 26;  /* Mlym */
    /**
     * Mangolian
     * @stable ICU 2.4
     */
    public static final int MONGOLIAN    = 27;  /* Mong */
    /**
     * Myammar
     * @stable ICU 2.4
     */
    public static final int MYANMAR      = 28;  /* Mymr */
    /**
     * Ogham
     * @stable ICU 2.4
     */
    public static final int OGHAM        = 29;  /* Ogam */
    /**
     * Old Itallic
     * @stable ICU 2.4
     */
    public static final int OLD_ITALIC   = 30;  /* Ital */
    /**
     * Oriya
     * @stable ICU 2.4
     */
    public static final int ORIYA        = 31;  /* Orya */
    /**
     * Runic
     * @stable ICU 2.4
     */
    public static final int RUNIC        = 32;  /* Runr */
    /**
     * Sinhala
     * @stable ICU 2.4
     */
    public static final int SINHALA      = 33;  /* Sinh */
    /**
     * Syriac
     * @stable ICU 2.4
     */
    public static final int SYRIAC       = 34;  /* Syrc (Syrj; Syrn; Syre) */
    /**
     * Tamil
     * @stable ICU 2.4
     */
    public static final int TAMIL        = 35;  /* Taml */
    /**
     * Telugu
     * @stable ICU 2.4
     */
    public static final int TELUGU       = 36;  /* Telu */
    /**
     * Thana
     * @stable ICU 2.4
     */
    public static final int THAANA       = 37;  /* Thaa */
    /**
     * Thai
     * @stable ICU 2.4
     */
    public static final int THAI         = 38;  /* Thai */
    /**
     * Tibetan
     * @stable ICU 2.4
     */
    public static final int TIBETAN      = 39;  /* Tibt */
    /**
     * Unified Canadian Aboriginal Symbols
     * @stable ICU 2.6
     */
    public static final int CANADIAN_ABORIGINAL = 40;  /* Cans */
    /**
     * Unified Canadian Aboriginal Symbols (alias)
     * @stable ICU 2.4
     */
    public static final int UCAS         = CANADIAN_ABORIGINAL;  /* Cans */
    /**
     * Yi syllables
     * @stable ICU 2.4
     */
    public static final int YI           = 41;  /* Yiii */
    /**
     * Tagalog
     * @stable ICU 2.4
     */
    public static final int TAGALOG      = 42;  /* Tglg */
    /**
     * Hanunooo
     * @stable ICU 2.4
     */
    public static final int HANUNOO      = 43;  /* Hano */
    /**
     * Buhid
     * @stable ICU 2.4
     */
    public static final int BUHID        = 44;  /* Buhd */
    /**
     * Tagbanwa
     * @stable ICU 2.4
     */
    public static final int TAGBANWA     = 45;  /* Tagb */
    /**
     * Braille
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int BRAILLE      = 46;  /* Brai */
    /**
     * Cypriot
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */  
    public static final int CYPRIOT              = 47;  /* Cprt */ 
    /**
     * Limbu
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */       
    public static final int LIMBU                = 48;  /* Limb */ 
    /**
     * Linear B
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int LINEAR_B     = 49;  /* Linb */ 
    /**
     * Osmanya
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int OSMANYA              = 50;  /* Osma */ 
    /**
     * Shavian
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int SHAVIAN              = 51;  /* Shaw */ 
    /**
     * Tai Le
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int TAI_LE               = 52;  /* Tale */ 
    /**
     * Ugaritic
     * New script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int UGARITIC     = 53;  /* Ugar */ 
    /**
     * New script code in Unicode 4.0.1
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int KATAKANA_OR_HIRAGANA = 54;  /*Hrkt */

    /**
     * Limit
     * @stable ICU 2.4
     */
    public static final int CODE_LIMIT   = 55;

    private static final int SCRIPT_MASK   = 0x0000007f;
    private static final UCharacterProperty prop= UCharacterProperty.getInstance();

    //private static final String INVALID_NAME = "Invalid";
    /**
     * Helper function to find the code from locale.
     * @param Locale the locale.
     */
    private static int[] findCodeFromLocale(ULocale locale) {
        ICUResourceBundle rb;
        try {
            rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        }
        catch (MissingResourceException e) {
            return null;
        }
        
        // if rb is not a strict fallback of the requested locale, return null
        //if(!LocaleUtility.isFallbackOf(rb.getULocale().toString(), locale.toString())){
        //    return null;
        //}
        //non existent locale check
        if(rb.getLoadingStatus()==ICUResourceBundle.FROM_DEFAULT && ! locale.equals(ULocale.getDefault())){
            return null;
        }
        ICUResourceBundle sub = rb.get("LocaleScript");
        
        int[] result = new int[sub.getSize()];
        int w = 0;
        for (int i = 0; i < result.length; ++i) {
            int code = UCharacter.getPropertyValueEnum(UProperty.SCRIPT,
                                                       sub.getString(i));
            result[w++] = code;

        }

        if (w < result.length) {
            throw new InternalError("bad locale data, listed " + result.length + " scripts but found only " + w);
        }

        return result;
    }

    /**
     * Gets a script codes associated with the given locale or ISO 15924 abbreviation or name.
     * Returns MALAYAM given "Malayam" OR "Mlym".
     * Returns LATIN given "en" OR "en_US"
     * @param locale Locale
     * @return The script codes array. null if the the code cannot be found.
     * @stable ICU 2.4
     */
    public static final int[] getCode(Locale locale){
        return findCodeFromLocale(ULocale.forLocale(locale));
    }
    /**
     * Gets a script codes associated with the given locale or ISO 15924 abbreviation or name.
     * Returns MALAYAM given "Malayam" OR "Mlym".
     * Returns LATIN given "en" OR "en_US"
     * @param locale ULocale
     * @return The script codes array. null if the the code cannot be found.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int[] getCode(ULocale locale){
        return findCodeFromLocale(locale);
    }
    /**
     * Gets a script codes associated with the given locale or ISO 15924 abbreviation or name.
     * Returns MALAYAM given "Malayam" OR "Mlym".
     * Returns LATIN given "en" OR "en_US"
     *
     * <p>Note: To search by short or long script alias only, use
     * UCharacater.getPropertyValueEnum(UProperty.SCRIPT, alias)
     * instead.  This does a fast lookup with no access of the locale
     * data.
     * @param nameOrAbbrOrLocale name of the script or ISO 15924 code or locale
     * @return The script codes array. null if the the code cannot be found.
     * @stable ICU 2.4
     */
    public static final int[] getCode(String nameOrAbbrOrLocale){
        try {
            return new int[] {
                UCharacter.getPropertyValueEnum(UProperty.SCRIPT,
                                                nameOrAbbrOrLocale)
            };
        } catch (IllegalArgumentException e) {
            return findCodeFromLocale(new ULocale(nameOrAbbrOrLocale));
        }
    }

    /**
     * Gets a script codes associated with the given ISO 15924 abbreviation or name.
     * Returns MALAYAM given "Malayam" OR "Mlym".
     *
     * @param nameOrAbbr name of the script or ISO 15924 code
     * @return The script code value or INVALID_CODE if the code cannot be found.
     * @internal
     */
    public static final int getCodeFromName(String nameOrAbbr) {
        try {
            return UCharacter.getPropertyValueEnum(UProperty.SCRIPT,
                                                   nameOrAbbr);
        } catch (IllegalArgumentException e) {
            return INVALID_CODE;
        }
    }

    /**
     * Gets the script code associated with the given codepoint.
     * Returns UScript.MALAYAM given 0x0D02
     * @param codepoint UChar32 codepoint
     * @return The script code
     * @stable ICU 2.4
     */
    public static final int getScript(int codepoint){
        if (codepoint >= UCharacter.MIN_VALUE & codepoint <= UCharacter.MAX_VALUE) {
            return (prop.getAdditional(codepoint,0) & SCRIPT_MASK);
        }else{
            throw new IllegalArgumentException(Integer.toString(codepoint));
        }
    }

    /**
     * Gets a script name associated with the given script code.
     * Returns  "Malayam" given MALAYAM
     * @param scriptCode int script code
     * @return script name as a string in full as given in TR#24
     * @stable ICU 2.4
     */
    public static final String getName(int scriptCode){
        return UCharacter.getPropertyValueName(UProperty.SCRIPT,
                                               scriptCode,
                                               UProperty.NameChoice.LONG);
    }

    /**
     * Gets a script name associated with the given script code.
     * Returns  "Mlym" given MALAYAM
     * @param scriptCode int script code
     * @return script abbreviated name as a string  as given in TR#24
     * @stable ICU 2.4
     */
    public static final String getShortName(int scriptCode){
        return UCharacter.getPropertyValueName(UProperty.SCRIPT,
                                               scriptCode,
                                               UProperty.NameChoice.SHORT);
    }
    ///CLOVER:OFF
    /**
     *  Private Constructor. Never default construct
     */
    private UScript(){}
    ///CLOVER:ON
}

