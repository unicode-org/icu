/**
*******************************************************************************
* Copyright (C) 2001-2009 International Business Machines Corporation and     *
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
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int BRAILLE      = 46;  /* Brai */
    /**
     * Cypriot
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */  
    public static final int CYPRIOT              = 47;  /* Cprt */ 
    /**
     * Limbu
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */       
    public static final int LIMBU                = 48;  /* Limb */ 
    /**
     * Linear B
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int LINEAR_B     = 49;  /* Linb */ 
    /**
     * Osmanya
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int OSMANYA              = 50;  /* Osma */ 
    /**
     * Shavian
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int SHAVIAN              = 51;  /* Shaw */ 
    /**
     * Tai Le
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int TAI_LE               = 52;  /* Tale */ 
    /**
     * Ugaritic
     * Script in Unicode 4 
     * @stable ICU 2.6 
     * 
     */ 
    public static final int UGARITIC     = 53;  /* Ugar */ 
    /**
     * Script in Unicode 4.0.1
     * @stable ICU 3.0
     */
    public static final int KATAKANA_OR_HIRAGANA = 54;  /*Hrkt */

    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int BUGINESE = 55;           /* Bugi */
    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int GLAGOLITIC = 56;         /* Glag */
    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int KHAROSHTHI = 57;         /* Khar */
    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int SYLOTI_NAGRI = 58;       /* Sylo */
    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int NEW_TAI_LUE = 59;        /* Talu */
    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int TIFINAGH = 60;           /* Tfng */
    /**
     * Script in Unicode 4.1
     * @stable ICU 3.4
     */
    public static final int OLD_PERSIAN = 61;        /* Xpeo */


    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int BALINESE                      = 62; /* Bali */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int BATAK                         = 63; /* Batk */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int BLISSYMBOLS                   = 64; /* Blis */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int BRAHMI                        = 65; /* Brah */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int CHAM                          = 66; /* Cham */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int CIRTH                         = 67; /* Cirt */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int OLD_CHURCH_SLAVONIC_CYRILLIC  = 68; /* Cyrs */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int DEMOTIC_EGYPTIAN              = 69; /* Egyd */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int HIERATIC_EGYPTIAN             = 70; /* Egyh */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int EGYPTIAN_HIEROGLYPHS          = 71; /* Egyp */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int KHUTSURI                      = 72; /* Geok */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int SIMPLIFIED_HAN                = 73; /* Hans */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int TRADITIONAL_HAN               = 74; /* Hant */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int PAHAWH_HMONG                  = 75; /* Hmng */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int OLD_HUNGARIAN                 = 76; /* Hung */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int HARAPPAN_INDUS                = 77; /* Inds */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int JAVANESE                      = 78; /* Java */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int KAYAH_LI                      = 79; /* Kali */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int LATIN_FRAKTUR                 = 80; /* Latf */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int LATIN_GAELIC                  = 81; /* Latg */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int LEPCHA                        = 82; /* Lepc */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int LINEAR_A                      = 83; /* Lina */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int MANDAEAN                      = 84; /* Mand */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int MAYAN_HIEROGLYPHS             = 85; /* Maya */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int MEROITIC                      = 86; /* Mero */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int NKO                           = 87; /* Nkoo */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int ORKHON                        = 88; /* Orkh */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int OLD_PERMIC                    = 89; /* Perm */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int PHAGS_PA                      = 90; /* Phag */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int PHOENICIAN                    = 91; /* Phnx */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int PHONETIC_POLLARD              = 92; /* Plrd */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int RONGORONGO                    = 93; /* Roro */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int SARATI                        = 94; /* Sara */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int ESTRANGELO_SYRIAC             = 95; /* Syre */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int WESTERN_SYRIAC                = 96; /* Syrj */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int EASTERN_SYRIAC                = 97; /* Syrn */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int TENGWAR                       = 98; /* Teng */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int VAI                           = 99; /* Vaii */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int VISIBLE_SPEECH                = 100;/* Visp */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int CUNEIFORM                     = 101;/* Xsux */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int UNWRITTEN_LANGUAGES           = 102;/* Zxxx */
    /**
     * ISO 15924 script code
     * @stable ICU 3.6
     */
    public static final int UNKNOWN                       = 103;/* Zzzz */ /* Unknown="Code for uncoded script", for unassigned code points */
    
    /* Private use codes from Qaaa - Qabx are not supported*/
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */ 
    public static final int CARIAN                        = 104;/* Cari */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int JAPANESE                      = 105;/* Jpan */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int LANNA                         = 106;/* Lana */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int LYCIAN                        = 107;/* Lyci */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int LYDIAN                        = 108;/* Lydi */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int OL_CHIKI                      = 109;/* Olck */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int REJANG                        = 110;/* Rjng */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int SAURASHTRA                    = 111;/* Saur */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int SIGN_WRITING                  = 112;/* Sgnw */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int SUNDANESE                     = 113;/* Sund */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int MOON                          = 114;/* Moon */
    /**
     * ISO 15924 script code
     * @stable ICU 3.8
     */
    public static final int MEITEI_MAYEK                  = 115;/* Mtei */
    
    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int IMPERIAL_ARAMAIC              = 116;/* Armi */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int AVESTAN                       = 117;/* Avst */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int CHAKMA                        = 118;/* Cakm */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int KOREAN                        = 119;/* Kore */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int KAITHI                        = 120;/* Kthi */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int MANICHAEAN                    = 121;/* Mani */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int INSCRIPTIONAL_PAHLAVI         = 122;/* Phli */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int PSALTER_PAHLAVI               = 123;/* Phlp */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int BOOK_PAHLAVI                  = 124;/* Phlv */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int INSCRIPTIONAL_PARTHIAN        = 125;/* Prti */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int SAMARITAN                     = 126;/* Samr */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int TAI_VIET                      = 127;/* Tavt */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int MATHEMATICAL_NOTATION         = 128;/* Zmth */

    /**
     * ISO 15924 script code
     * @stable ICU 4.0
     */
    public static final int SYMBOLS                       = 129;/* Zsym */
    /**
     * Limit
     * @stable ICU 2.4
     */
    public static final int CODE_LIMIT   = 130;

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
        } catch (MissingResourceException e) {
            /* This part seems to never be called since "UResourceBundle.getBundleInstance"
             * corrects this by setting to ICUResourceBundle.FROM_DEFAULT
             * when such an invalid locale is passed.
             */
            ///CLOVER:OFF
            return null;
            ///CLOVER:ON
        }
        
        rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        
        // if rb is not a strict fallback of the requested locale, return null
        //if(!LocaleUtility.isFallbackOf(rb.getULocale().toString(), locale.toString())){
        //    return null;
        //}
        //non existent locale check
        if(rb.getLoadingStatus()==ICUResourceBundle.FROM_DEFAULT && ! locale.equals(ULocale.getDefault())){
            return null;
        }
        UResourceBundle sub = rb.get(kLocaleScript);
        
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
     * @stable ICU 3.0
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
     * @deprecated This API is ICU internal only.
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
