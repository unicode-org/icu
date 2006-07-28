/**
*******************************************************************************
* Copyright (C) 2001-2006, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.lang;

import com.ibm.icu.impl.ICUResourceBundle;
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
     * @provisional This API might change or be removed in a future release.
     */
    public static final int KATAKANA_OR_HIRAGANA = 54;  /*Hrkt */

    /* New scripts in Unicode 4.1 @draft ICU 3.4 */

    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int BUGINESE = 55;           /* Bugi */
    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int GLAGOLITIC = 56;         /* Glag */
    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int KHAROSHTHI = 57;         /* Khar */
    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int SYLOTI_NAGRI = 58;       /* Sylo */
    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int NEW_TAI_LUE = 59;        /* Talu */
    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int TIFINAGH = 60;           /* Tfng */
    /**
     * New script code in Unicode 4.1
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static final int OLD_PERSIAN = 61;        /* Xpeo */


    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int BALINESE                      = 62; /* Bali */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int BATAK                         = 63; /* Batk */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int BLISSYMBOLS                   = 64; /* Blis */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int BRAHMI                        = 65; /* Brah */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int CHAM                          = 66; /* Cham */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int CIRTH                         = 67; /* Cirt */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int OLD_CHURCH_SLAVONIC_CYRILLIC  = 68; /* Cyrs */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int DEMOTIC_EGYPTIAN              = 69; /* Egyd */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int HIERATIC_EGYPTIAN             = 70; /* Egyh */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int EGYPTIAN_HIEROGLYPHS          = 71; /* Egyp */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int KHUTSURI                      = 72; /* Geok */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int SIMPLIFIED_HAN                = 73; /* Hans */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int TRADITIONAL_HAN               = 74; /* Hant */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int PAHAWH_HMONG                  = 75; /* Hmng */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int OLD_HUNGARIAN                 = 76; /* Hung */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int HARAPPAN_INDUS                = 77; /* Inds */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int JAVANESE                      = 78; /* Java */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int KAYAH_LI                      = 79; /* Kali */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int LATIN_FRAKTUR                 = 80; /* Latf */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int LATIN_GAELIC                  = 81; /* Latg */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int LEPCHA                        = 82; /* Lepc */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int LINEAR_A                      = 83; /* Lina */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int MANDAEAN                      = 84; /* Mand */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int MAYAN_HIEROGLYPHS             = 85; /* Maya */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int MEROITIC                      = 86; /* Mero */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int NKO                           = 87; /* Nkoo */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int ORKHON                        = 88; /* Orkh */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int OLD_PERMIC                    = 89; /* Perm */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int PHAGS_PA                      = 90; /* Phag */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int PHOENICIAN                    = 91; /* Phnx */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int PHONETIC_POLLARD              = 92; /* Plrd */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int RONGORONGO                    = 93; /* Roro */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int SARATI                        = 94; /* Sara */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int ESTRANGELO_SYRIAC             = 95; /* Syre */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int WESTERN_SYRIAC                = 96; /* Syrj */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int EASTERN_SYRIAC                = 97; /* Syrn */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int TENGWAR                       = 98; /* Teng */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int VAI                           = 99; /* Vaii */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int VISIBLE_SPEECH                = 100;/* Visp */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int CUNEIFORM                     = 101;/* Xsux */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int UNWRITTEN_LANGUAGES           = 102;/* Zxxx */
    /**
     * New script codes from ISO 15924 
     * @draft ICU 3.6 
     */
    public static final int UNKNOWN                       = 103;/* Zzzz */ /* Unknown="Code for uncoded script", for unassigned code points */
    
    /* Private use codes from Qaaa - Qabx are not supported*/

    /**
     * Limit
     * @stable ICU 2.4
     */
    public static final int CODE_LIMIT   = 104;

    private static final int SCRIPT_MASK   = 0x0000007f;
    private static final UCharacterProperty prop= UCharacterProperty.getInstance();
    private static final String kLocaleScript = "LocaleScript";
    
    //private static final String INVALID_NAME = "Invalid";
    /**
     * Helper function to find the code from locale.
     * @param locale The locale.
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
        ICUResourceBundle sub = rb.get(kLocaleScript);
        
        int[] result = new int[sub.getSize()];
        int w = 0;
        for (int i = 0; i < result.length; ++i) {
            int code = UCharacter.getPropertyValueEnum(UProperty.SCRIPT,
                                                       sub.getString(i));
            result[w++] = code;

        }

        if (w < result.length) {
            throw new IllegalStateException("bad locale data, listed " + 
                 result.length + " scripts but found only " + w);
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
     * @provisional This API might change or be removed in a future release.
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

