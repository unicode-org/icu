/*
******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.util;

import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;
import java.io.Serializable;
import java.io.IOException;
import java.util.Enumeration;

import com.ibm.icu.impl.ICUResourceBundle;

/**
 * A class is analogous to {@link java.util.Locale} and provides additional
 * support for ICU protocol.  In ICU 3.0 this class is enhanced to support
 * RFC 3066 language identifiers.
 *
 * <p>Many classes and services in ICU follow a factory idiom, in which a
 * factory method or object responds to a client request with an
 * object.  The request includes a locale (the <i>requested</i>
 * locale), and the returned object is constructed using data for that
 * locale.  The system may lack data for the requested locale, in
 * which case the locale fallback mechanism will be invoked until a
 * populated locale is found (the <i>valid</i> locale).  Furthermore,
 * even when a valid locale is found, further fallback may be required
 * to reach a locale containing the specific data required by the
 * service (the <i>actual</i> locale).
 *
 * <p>This class provides selectors {@link #VALID_LOCALE} and {@link
 * #ACTUAL_LOCALE} intended for use in methods named
 * <tt>getLocale()</tt>.  These methods exist in several ICU classes,
 * including {@link com.ibm.icu.util.Calendar}, {@link
 * com.ibm.icu.util.Currency}, {@link com.ibm.icu.text.UFormat},
 * {@link com.ibm.icu.text.BreakIterator}, {@link
 * com.ibm.icu.text.Collator}, {@link
 * com.ibm.icu.text.DateFormatSymbols}, and {@link
 * com.ibm.icu.text.DecimalFormatSymbols} and their subclasses, if
 * any.  Once an object of one of these classes has been created,
 * <tt>getLocale()</tt> may be called on it to determine the valid and
 * actual locale arrived at during the object's construction.
 *
 * <p>Note: The <tt>getLocale()</tt> method will be implemented in ICU
 * 3.0; ICU 2.8 contains a partial preview implementation.  The
 * <i>actual</i> locale is returned correctly, but the <i>valid</i>
 * locale is not, in most cases.
 *
 * @see java.util.Locale
 * @author weiv
 * @author Alan Liu
 * @author Ram Viswanadha
 * @draft ICU 2.8
 */
public final class ULocale implements Serializable {
    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale ENGLISH = new ULocale("en");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale FRENCH = new ULocale("fr");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale GERMAN = new ULocale("de");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale ITALIAN = new ULocale("it");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale JAPANESE = new ULocale("ja");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale KOREAN = new ULocale("ko");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale CHINESE = new ULocale("zh");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale SIMPLIFIED_CHINESE = new ULocale("zh_Hans");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale TRADITIONAL_CHINESE = new ULocale("zh_Hant");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale FRANCE = new ULocale("fr_FR");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale GERMANY = new ULocale("de_DE");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale ITALY = new ULocale("it_IT");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale JAPAN = new ULocale("ja_JP");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale KOREA = new ULocale("ko_KR");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale CHINA = new ULocale("zh_Hans_CN");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale PRC = CHINA;

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale TAIWAN = new ULocale("zh_Hant_TW");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale UK = new ULocale("en_GB");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale US = new ULocale("en_US");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale CANADA = new ULocale("en_CA");

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     */
    public static ULocale CANADA_FRENCH = new ULocale("fr_CA");

    /**
     * The root ULocale.
     * @draft ICU 2.8
     */ 
    public static ULocale ROOT = new ULocale("");
    
    // Either 'locale' will be non-null, or 'locName' will be
    // non-null, or both.  Null members are instantiated on demand.
    private transient Locale locale;

    private String localeID;

	private static char UNDERSCORE            = '_';
    private static char KEYWORD_SEPARATOR     = '@';
    private static char HYPHEN                = '-';
    private static char KEYWORD_ASSIGN        = '=';
    private static char COMA                  = ',';
    private static char ITEM_SEPARATOR        = ';';
    private static char DOT                   = '.';
    
    /* tables updated per http://lcweb.loc.gov/standards/iso639-2/ 
        to include the revisions up to 2001/7/27 *CWB*/
    /* The 3 character codes are the terminology codes like RFC 3066.  
        This is compatible with prior ICU codes */
    /* "in" "iw" "ji" "jw" & "sh" have been withdrawn but are still in 
        the table but now at the end of the table because 
        3 character codes are duplicates.  This avoids bad searches
        going from 3 to 2 character codes.*/
    /* The range qaa-qtz is reserved for local use. */
    
    /* This list MUST be in sorted order, and MUST contain the two-letter codes
    if one exists otherwise use the three letter code */
    private static String[] languages = new String[]{
        "aa",  "ab",  "ace", "ach", "ada", "ady", "ae",  "af",  "afa",
        "afh", "ak",  "akk", "ale", "alg", "am",  "an",  "ang", "apa",
        "ar",  "arc", "arn", "arp", "art", "arw", "as",  "ast",
        "ath", "aus", "av",  "awa", "ay",  "az",  "ba",  "bad",
        "bai", "bal", "bam", "ban", "bas", "bat", "be",  "bej",
        "bem", "ber", "bg",  "bh",  "bho", "bi",  "bik", "bin",
        "bla", "bm",  "bn",  "bnt", "bo",  "br",  "bra", "bs",
        "btk", "bua", "bug", "byn", "ca",  "cad", "cai", "car", "cau",
        "ce",  "ceb", "cel", "ch",  "chb", "chg", "chk", "chm",
        "chn", "cho", "chp", "chr", "chy", "cmc", "co",  "cop",
        "cpe", "cpf", "cpp", "cr",  "crh", "crp", "cs",  "csb", "cu",  "cus",
        "cv",  "cy",  "da",  "dak", "dar", "day", "de",  "del", "den",
        "dgr", "din", "doi", "dra", "dsb", "dua", "dum", "dv",  "dyu",
        "dz",  "ee",  "efi", "egy", "eka", "el",  "elx", "en",
        "enm", "eo",  "es",  "et",  "eu",  "ewo", "fa",
        "fan", "fat", "ff",  "fi",  "fiu", "fj",  "fo",  "fon",
        "fr",  "frm", "fro", "fur", "fy",  "ga",  "gaa", "gay",
        "gba", "gd",  "gem", "gez", "gil", "gl",  "gmh", "gn",
        "goh", "gon", "gor", "got", "grb", "grc", "gu",  "gv",
        "gwi", "ha",  "hai", "haw", "he",  "hi",  "hil", "him",
        "hit", "hmn", "ho",  "hr",  "hsb", "ht",  "hu",  "hup", "hy",  "hz",
        "ia",  "iba", "id",  "ie",  "ig",  "ii",  "ijo", "ik",
        "ilo", "inc", "ine", "inh", "io",  "ira", "iro", "is",  "it",
        "iu",  "ja",  "jbo", "jpr", "jrb", "jv",  "ka",  "kaa", "kab",
        "kac", "kam", "kar", "kaw", "kbd", "kg",  "kha", "khi",
        "kho", "ki",  "kj",  "kk",  "kl",  "km",  "kmb", "kn",
        "ko",  "kok", "kos", "kpe", "kr",  "krc", "kro", "kru", "ks",
        "ku",  "kum", "kut", "kv",  "kw",  "ky",  "la",  "lad",
        "lah", "lam", "lb",  "lez", "lg",  "li",  "ln",  "lo",  "lol",
        "loz", "lt",  "lu",  "lua", "lui", "lun", "luo", "lus",
        "lv",  "mad", "mag", "mai", "mak", "man", "map", "mas",
        "mdf", "mdr", "men", "mg",  "mga", "mh",  "mi",  "mic", "min",
        "mis", "mk",  "mkh", "ml",  "mn",  "mnc", "mni", "mno",
        "mo",  "moh", "mos", "mr",  "ms",  "mt",  "mul", "mun",
        "mus", "mwr", "my",  "myn", "myv", "na",  "nah", "nai", "nap",
        "nb",  "nd",  "nds", "ne",  "new", "ng",  "nia", "nic",
        "niu", "nl",  "nn",  "no",  "nog", "non", "nr",  "nso", "nub",
        "nv",  "ny",  "nym", "nyn", "nyo", "nzi", "oc",  "oj",
        "om",  "or",  "os",  "osa", "ota", "oto", "pa",  "paa",
        "pag", "pal", "pam", "pap", "pau", "peo", "phi", "phn",
        "pi",  "pl",  "pon", "pra", "pro", "ps",  "pt",  "qu",
        "raj", "rap", "rar", "rm",  "rn",  "ro",  "roa", "rom",
        "ru",  "rw",  "sa",  "sad", "sah", "sai", "sal", "sam",
        "sas", "sat", "sc",  "sco", "sd",  "se",  "sel", "sem",
        "sg",  "sga", "sgn", "shn", "si",  "sid", "sio", "sit",
        "sk",  "sl",  "sla", "sm",  "sma", "smi", "smj", "smn",
        "sms", "sn",  "snk", "so",  "sog", "son", "sq",  "sr",
        "srr", "ss",  "ssa", "st",  "su",  "suk", "sus", "sux",
        "sv",  "sw",  "syr", "ta",  "tai", "te",  "tem", "ter",
        "tet", "tg",  "th",  "ti",  "tig", "tiv", "tk",  "tkl",
        "tl",  "tli", "tmh", "tn",  "to",  "tog", "tpi", "tr",
        "ts",  "tsi", "tt",  "tum", "tup", "tut", "tvl", "tw",
        "ty",  "tyv", "udm", "ug",  "uga", "uk",  "umb", "und", "ur",
        "uz",  "vai", "ve",  "vi",  "vo",  "vot", "wa",  "wak",
        "wal", "war", "was", "wen", "wo",  "xal", "xh",  "yao", "yap",
        "yi",  "yo",  "ypk", "za",  "zap", "zen", "zh",  "znd",
        "zu",  "zun", 
        null,
        "in",  "iw",  "ji",  "jw",  "sh",    /* obsolete language codes */    
        
    };
    
    
    /* This list MUST contain a three-letter code for every two-letter code in the
       list above, and they MUST ne in the same order (i.e., the same language must
       be in the same place in both lists)! */
    private static String[] languages3 = new String[]{
    /*  "aa",  "ab",  "ace", "ach", "ada", "ady", "ae",  "af",  "afa",    */
        "aar", "abk", "ace", "ach", "ada", "ady", "ave", "afr", "afa",
    /*  "afh", "ak",  "akk", "ale", "alg", "am",  "an",  "ang", "apa",    */
        "afh", "aka", "akk", "ale", "alg", "amh", "arg", "ang", "apa",
    /*  "ar",  "arc", "arn", "arp", "art", "arw", "as",  "ast",    */
        "ara", "arc", "arn", "arp", "art", "arw", "asm", "ast",
    /*  "ath", "aus", "av",  "awa", "ay",  "az",  "ba",  "bad",    */
        "ath", "aus", "ava", "awa", "aym", "aze", "bak", "bad",
    /*  "bai", "bal", "bam", "ban", "bas", "bat", "be",  "bej",    */
        "bai", "bal", "bam", "ban", "bas", "bat", "bel", "bej",
    /*  "bem", "ber", "bg",  "bh",  "bho", "bi",  "bik", "bin",    */
        "bem", "ber", "bul", "bih", "bho", "bis", "bik", "bin",
    /*  "bla", "bm",  "bn",  "bnt", "bo",  "br",  "bra", "bs",     */
        "bla", "bm",  "ben", "bnt", "bod", "bre", "bra", "bos",
    /*  "btk", "bua", "bug", "byn", "ca",  "cad", "cai", "car", "cau",    */
        "btk", "bua", "bug", "byn", "cat", "cad", "cai", "car", "cau",
    /*  "ce",  "ceb", "cel", "ch",  "chb", "chg", "chk", "chm",    */
        "che", "ceb", "cel", "cha", "chb", "chg", "chk", "chm",
    /*  "chn", "cho", "chp", "chr", "chy", "cmc", "co",  "cop",    */
        "chn", "cho", "chp", "chr", "chy", "cmc", "cos", "cop",
    /*  "cpe", "cpf", "cpp", "cr",  "crh", "crp", "cs",  "csb", "cu",  "cus",    */
        "cpe", "cpf", "cpp", "cre", "crh", "crp", "ces", "csb", "chu", "cus",
    /*  "cv",  "cy",  "da",  "dak", "dar", "day", "de",  "del", "den",    */
        "chv", "cym", "dan", "dak", "dar", "day", "deu", "del", "den",
    /*  "dgr", "din", "doi", "dra", "dsb", "dua", "dum", "dv",  "dyu",    */
        "dgr", "din", "doi", "dra", "dsb", "dua", "dum", "div", "dyu",
    /*  "dz",  "ee",  "efi", "egy", "eka", "el",  "elx", "en",     */
        "dzo", "ewe", "efi", "egy", "eka", "ell", "elx", "eng",
    /*  "enm", "eo",  "es",  "et",  "eu",  "ewo", "fa",     */
        "enm", "epo", "spa", "est", "eus", "ewo", "fas",
    /*  "fan", "fat", "ff",  "fi",  "fiu", "fj",  "fo",  "fon",    */
        "fan", "fat", "ful", "fin", "fiu", "fij", "fao", "fon",
    /*  "fr",  "frm", "fro", "fur", "fy",  "ga",  "gaa", "gay",    */
        "fra", "frm", "fro", "fur", "fry", "gle", "gaa", "gay",
    /*  "gba", "gd",  "gem", "gez", "gil", "gl",  "gmh", "gn",     */
        "gba", "gla", "gem", "gez", "gil", "glg", "gmh", "grn",
    /*  "goh", "gon", "gor", "got", "grb", "grc", "gu",  "gv",     */
        "goh", "gon", "gor", "got", "grb", "grc", "guj", "glv",
    /*  "gwi", "ha",  "hai", "haw", "he",  "hi",  "hil", "him",    */
        "gwi", "hau", "hai", "haw", "heb", "hin", "hil", "him",
    /*  "hit", "hmn", "ho",  "hr",  "hsb", "ht",  "hu",  "hup", "hy",  "hz",     */
        "hit", "hmn", "hmo", "hrv", "hsb", "hat", "hun", "hup", "hye", "her",
    /*  "ia",  "iba", "id",  "ie",  "ig",  "ii",  "ijo", "ik",     */
        "ina", "iba", "ind", "ile", "ibo", "iii", "ijo", "ipk",
    /*  "ilo", "inc", "ine", "inh", "io",  "ira", "iro", "is",  "it",      */
        "ilo", "inc", "ine", "inh", "ido", "ira", "iro", "isl", "ita",
    /*  "iu",  "ja",  "jbo", "jpr", "jrb", "jv",  "ka",  "kaa", "kab",   */
        "iku", "jpn", "jbo", "jpr", "jrb", "jaw", "kat", "kaa", "kab",
    /*  "kac", "kam", "kar", "kaw", "kbd", "kg",  "kha", "khi",    */
        "kac", "kam", "kar", "kaw", "kbd", "kon", "kha", "khi",
    /*  "kho", "ki",  "kj",  "kk",  "kl",  "km",  "kmb", "kn",     */
        "kho", "kik", "kua", "kaz", "kal", "khm", "kmb", "kan",
    /*  "ko",  "kok", "kos", "kpe", "kr",  "krc", "kro", "kru", "ks",     */
        "kor", "kok", "kos", "kpe", "kau", "krc", "kro", "kru", "kas",
    /*  "ku",  "kum", "kut", "kv",  "kw",  "ky",  "la",  "lad",    */
        "kur", "kum", "kut", "kom", "cor", "kir", "lat", "lad",
    /*  "lah", "lam", "lb",  "lez", "lg",  "li",  "ln",  "lo",  "lol",    */
        "lah", "lam", "ltz", "lez", "lug", "lim", "lin", "lao", "lol",
    /*  "loz", "lt",  "lu",  "lua", "lui", "lun", "luo", "lus",    */
        "loz", "lit", "lub", "lua", "lui", "lun", "luo", "lus",
    /*  "lv",  "mad", "mag", "mai", "mak", "man", "map", "mas",    */
        "lav", "mad", "mag", "mai", "mak", "man", "map", "mas",
    /*  "mdf", "mdr", "men", "mg",  "mga", "mh",  "mi",  "mic", "min",    */
        "mdf", "mdr", "men", "mlg", "mga", "mah", "mri", "mic", "min",
    /*  "mis", "mk",  "mkh", "ml",  "mn",  "mnc", "mni", "mno",    */
        "mis", "mkd", "mkh", "mal", "mon", "mnc", "mni", "mno",
    /*  "mo",  "moh", "mos", "mr",  "ms",  "mt",  "mul", "mun",    */
        "mol", "moh", "mos", "mar", "msa", "mlt", "mul", "mun",
    /*  "mus", "mwr", "my",  "myn", "myv", "na",  "nah", "nai", "nap",    */
        "mus", "mwr", "mya", "myn", "myv", "nau", "nah", "nai", "nap",
    /*  "nb",  "nd",  "nds", "ne",  "new", "ng",  "nia", "nic",    */
        "nob", "nde", "nds", "nep", "new", "ndo", "nia", "nic",
    /*  "niu", "nl",  "nn",  "no",  "nog", "non", "nr",  "nso", "nub",    */
        "niu", "nld", "nno", "nor", "nog", "non", "nbl", "nso", "nub",
    /*  "nv",  "ny",  "nym", "nyn", "nyo", "nzi", "oc",  "oj",     */
        "nav", "nya", "nym", "nyn", "nyo", "nzi", "oci", "oji",
    /*  "om",  "or",  "os",  "osa", "ota", "oto", "pa",  "paa",    */
        "orm", "ori", "oss", "osa", "ota", "oto", "pan", "paa",
    /*  "pag", "pal", "pam", "pap", "pau", "peo", "phi", "phn",    */
        "pag", "pal", "pam", "pap", "pau", "peo", "phi", "phn",
    /*  "pi",  "pl",  "pon", "pra", "pro", "ps",  "pt",  "qu",     */
        "pli", "pol", "pon", "pra", "pro", "pus", "por", "que",
    /*  "raj", "rap", "rar", "rm",  "rn",  "ro",  "roa", "rom",    */
        "raj", "rap", "rar", "roh", "run", "ron", "roa", "rom",
    /*  "ru",  "rw",  "sa",  "sad", "sah", "sai", "sal", "sam",    */
        "rus", "kin", "san", "sad", "sah", "sai", "sal", "sam",
    /*  "sas", "sat", "sc",  "sco", "sd",  "se",  "sel", "sem",    */
        "sas", "sat", "srd", "sco", "snd", "sme", "sel", "sem",
    /*  "sg",  "sga", "sgn", "shn", "si",  "sid", "sio", "sit",    */
        "sag", "sga", "sgn", "shn", "sin", "sid", "sio", "sit",
    /*  "sk",  "sl",  "sla", "sm",  "sma", "smi", "smj", "smn",    */
        "slk", "slv", "sla", "smo", "sma", "smi", "smj", "smn",
    /*  "sms", "sn",  "snk", "so",  "sog", "son", "sq",  "sr",     */
        "sms", "sna", "snk", "som", "sog", "son", "sqi", "srp",
    /*  "srr", "ss",  "ssa", "st",  "su",  "suk", "sus", "sux",    */
        "srr", "ssw", "ssa", "sot", "sun", "suk", "sus", "sux",
    /*  "sv",  "sw",  "syr", "ta",  "tai", "te",  "tem", "ter",    */
        "swe", "swa", "syr", "tam", "tai", "tel", "tem", "ter",
    /*  "tet", "tg",  "th",  "ti",  "tig", "tiv", "tk",  "tkl",    */
        "tet", "tgk", "tha", "tir", "tig", "tiv", "tuk", "tkl",
    /*  "tl",  "tli", "tmh", "tn",  "to",  "tog", "tpi", "tr",     */
        "tgl", "tli", "tmh", "tsn", "ton", "tog", "tpi", "tur",
    /*  "ts",  "tsi", "tt",  "tum", "tup", "tut", "tvl", "tw",     */
        "tso", "tsi", "tat", "tum", "tup", "tut", "tvl", "twi",
    /*  "ty",  "tyv", "udm", "ug",  "uga", "uk",  "umb", "und", "ur",     */
        "tah", "tyv", "udm", "uig", "uga", "ukr", "umb", "und", "urd",
    /*  "uz",  "vai", "ve",  "vi",  "vo",  "vot", "wa",  "wak",    */
        "uzb", "vai", "ven", "vie", "vol", "vot", "wln", "wak",
    /*  "wal", "war", "was", "wen", "wo",  "xal", "xh",  "yao", "yap",    */
        "wal", "war", "was", "wen", "wol", "xal", "xho", "yao", "yap",
    /*  "yi",  "yo",  "ypk", "za",  "zap", "zen", "zh",  "znd",    */
        "yid", "yor", "ypk", "zha", "zap", "zen", "zho", "znd",
    /*  "zu",  "zun",                                              */
        "zul", "zun",  
        null,
         "ind", "heb", "yid", "jaw", "srp", 
    };
    
    
    /* ZR(ZAR) is now CD(COD) and FX(FXX) is PS(PSE) as per
     http://www.evertype.com/standards/iso3166/iso3166-1-en.html 
     added new codes keeping the old ones for compatibility
     updated to include 1999/12/03 revisions *CWB*/
    
    /* RO(ROM) is now RO(ROU) according to 
     http://www.iso.org/iso/en/prods-services/iso3166ma/03updates-on-iso-3166/nlv3e-rou.html
    */
    
    /* This list MUST be in sorted order, and MUST contain only two-letter codes! */
    private static String[] countries = {
        "AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",  "AN",
        "AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AZ",
        "BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",
        "BJ",  "BM",  "BN",  "BO",  "BR",  "BS",  "BT",  "BV",
        "BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",
        "CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",
        "CU",  "CV",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",
        "DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",
        "ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",
        "GA",  "GB",  "GD",  "GE",  "GF",  "GH",  "GI",  "GL",
        "GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",
        "GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",
        "ID",  "IE",  "IL",  "IN",  "IO",  "IQ",  "IR",  "IS",
        "IT",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",
        "KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",
        "LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",
        "LV",  "LY",  "MA",  "MC",  "MD",  "MG",  "MH",  "MK",
        "ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",
        "MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",
        "NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",
        "NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",
        "PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",
        "PW",  "PY",  "QA",  "RE",  "RO",  "RU",  "RW",  "SA",
        "SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",
        "SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "ST",  "SV",
        "SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",
        "TK",  "TL",  "TM",  "TN",  "TO",  "TR",  "TT",  "TV",
        "TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",
        "VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",
        "WS",  "YE",  "YT",  "YU",  "ZA",  "ZM",  "ZW",  
    };
    
    private static String[] obsoleteCountries = new String[]{
            "FX",  "RO",  "TP",  "ZR",   /* obsolete country codes */      
    };
    
    /* This list MUST contain a three-letter code for every two-letter code in
       the above list, and they MUST be listed in the same order! */
    private static String[] countries3 = new String[]{
    /*  "AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",  "AN",     */
        "AND", "ARE", "AFG", "ATG", "AIA", "ALB", "ARM", "ANT",
    /*  "AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AZ",     */
        "AGO", "ATA", "ARG", "ASM", "AUT", "AUS", "ABW", "AZE",
    /*  "BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",     */
        "BIH", "BRB", "BGD", "BEL", "BFA", "BGR", "BHR", "BDI",
    /*  "BJ",  "BM",  "BN",  "BO",  "BR",  "BS",  "BT",  "BV",     */
        "BEN", "BMU", "BRN", "BOL", "BRA", "BHS", "BTN", "BVT",
    /*  "BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",     */
        "BWA", "BLR", "BLZ", "CAN", "CCK", "COD", "CAF", "COG",
    /*  "CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",     */
        "CHE", "CIV", "COK", "CHL", "CMR", "CHN", "COL", "CRI",
    /*  "CU",  "CV",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",     */
        "CUB", "CPV", "CXR", "CYP", "CZE", "DEU", "DJI", "DNK",
    /*  "DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",     */
        "DMA", "DOM", "DZA", "ECU", "EST", "EGY", "ESH", "ERI",
    /*  "ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",     */
        "ESP", "ETH", "FIN", "FJI", "FLK", "FSM", "FRO", "FRA",
    /*  "GA",  "GB",  "GD",  "GE",  "GF",  "GH",  "GI",  "GL",     */
        "GAB", "GBR", "GRD", "GEO", "GUF", "GHA", "GIB", "GRL",
    /*  "GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",     */
        "GMB", "GIN", "GLP", "GNQ", "GRC", "SGS", "GTM", "GUM",
    /*  "GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",     */
        "GNB", "GUY", "HKG", "HMD", "HND", "HRV", "HTI", "HUN",
    /*  "ID",  "IE",  "IL",  "IN",  "IO",  "IQ",  "IR",  "IS",     */
        "IDN", "IRL", "ISR", "IND", "IOT", "IRQ", "IRN", "ISL",
    /*  "IT",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",     */
        "ITA", "JAM", "JOR", "JPN", "KEN", "KGZ", "KHM", "KIR",
    /*  "KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",     */
        "COM", "KNA", "PRK", "KOR", "KWT", "CYM", "KAZ", "LAO",
    /*  "LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",     */
        "LBN", "LCA", "LIE", "LKA", "LBR", "LSO", "LTU", "LUX",
    /*  "LV",  "LY",  "MA",  "MC",  "MD",  "MG",  "MH",  "MK",     */
        "LVA", "LBY", "MAR", "MCO", "MDA", "MDG", "MHL", "MKD",
    /*  "ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",     */
        "MLI", "MMR", "MNG", "MAC", "MNP", "MTQ", "MRT", "MSR",
    /*  "MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",     */
        "MLT", "MUS", "MDV", "MWI", "MEX", "MYS", "MOZ", "NAM",
    /*  "NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",     */
        "NCL", "NER", "NFK", "NGA", "NIC", "NLD", "NOR", "NPL",
    /*  "NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",     */
        "NRU", "NIU", "NZL", "OMN", "PAN", "PER", "PYF", "PNG",
    /*  "PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",     */
        "PHL", "PAK", "POL", "SPM", "PCN", "PRI", "PSE", "PRT",
    /*  "PW",  "PY",  "QA",  "RE",  "RO",  "RU",  "RW",  "SA",     */
        "PLW", "PRY", "QAT", "REU", "ROU", "RUS", "RWA", "SAU",
    /*  "SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",     */
        "SLB", "SYC", "SDN", "SWE", "SGP", "SHN", "SVN", "SJM",
    /*  "SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "ST",  "SV",     */
        "SVK", "SLE", "SMR", "SEN", "SOM", "SUR", "STP", "SLV",
    /*  "SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",     */
        "SYR", "SWZ", "TCA", "TCD", "ATF", "TGO", "THA", "TJK",
    /*  "TK",  "TL",  "TM",  "TN",  "TO",  "TR",  "TT",  "TV",     */
        "TKL", "TLS", "TKM", "TUN", "TON", "TUR", "TTO", "TUV",
    /*  "TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",     */
        "TWN", "TZA", "UKR", "UGA", "UMI", "USA", "URY", "UZB",
    /*  "VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",     */
        "VAT", "VCT", "VEN", "VGB", "VIR", "VNM", "VUT", "WLF",
    /*  "WS",  "YE",  "YT",  "YU",  "ZA",  "ZM",  "ZW",            */
        "WSM", "YEM", "MYT", "YUG", "ZAF", "ZMB", "ZWE",
    };
    
    private static String[] obsoleteCountries3 = {
    /*  "FX",  "RO",  "TP",  "ZR",   */
        "FXX", "ROM", "TMP", "ZAR",    
    };
    /* The left side is the result after getName is processes the name */
    /* The right side is what the locale should be converted to. */
    private static String[][] variantsToKeywords = new String[][]{
        { "",               "en_US_POSIX" }, /* .NET name */
        { "C",              "en_US_POSIX" }, /* POSIX name */
        { "art_LOJBAN",     "jbo" }, /* registered name */
        { "az_AZ_CYRL",     "az_Cyrl_AZ" }, /* .NET name */
        { "az_AZ_LATN",     "az_Latn_AZ" }, /* .NET name */
        { "ca_ES_PREEURO",  "ca_ES@currency=ESP" },
        { "cel_GAULISH",    "cel__GAULISH" }, /* registered name */
        { "de_1901",        "de__1901" }, /* registered name */
        { "de_1906",        "de__1906" }, /* registered name */
        { "de__PHONEBOOK",  "de@collation=phonebook" },
        { "de_AT_PREEURO",  "de_AT@currency=ATS" },
        { "de_DE_PREEURO",  "de_DE@currency=DEM" },
        { "de_LU_PREEURO",  "de_LU@currency=EUR" },
        { "el_GR_PREEURO",  "el_GR@currency=GRD" },
        { "en_BOONT",       "en__BOONT" }, /* registered name */
        { "en_SCOUSE",      "en__SCOUSE" }, /* registered name */
        { "en_BE_PREEURO",  "en_BE@currency=BEF" },
        { "en_IE_PREEURO",  "en_IE@currency=IEP" },
        { "es__TRADITIONAL", "es@collation=traditional" },
        { "es_ES_PREEURO",  "es_ES@currency=ESP" },
        { "eu_ES_PREEURO",  "eu_ES@currency=ESP" },
        { "fi_FI_PREEURO",  "fi_FI@currency=FIM" },
        { "fr_BE_PREEURO",  "fr_BE@currency=BEF" },
        { "fr_FR_PREEURO",  "fr_FR@currency=FRF" },
        { "fr_LU_PREEURO",  "fr_LU@currency=LUF" },
        { "ga_IE_PREEURO",  "ga_IE@currency=IEP" },
        { "gl_ES_PREEURO",  "gl_ES@currency=ESP" },
        { "hi__DIRECT",     "hi@collation=direct" },
        { "it_IT_PREEURO",  "it_IT@currency=ITL" },
        { "ja_JP_TRADITIONAL", "ja_JP@calendar=japanese" },
        { "nl_BE_PREEURO",  "nl_BE@currency=BEF" },
        { "nl_NL_PREEURO",  "nl_NL@currency=NLG" },
        { "pt_PT_PREEURO",  "pt_PT@currency=PTE" },
        { "sl_ROZAJ",       "sl__ROZAJ" }, /* registered name */
        { "sr_SP_CYRL",     "sr_Cyrl_SP" }, /* .NET name */
        { "sr_SP_LATN",     "sr_Latn_SP" }, /* .NET name */
        { "uz_UZ_CYRL",     "uz_Cyrl_UZ" }, /* .NET name */
        { "uz_UZ_LATN",     "uz_Latn_UZ" }, /* .NET name */
        { "zh_CHS",         "zh_Hans" }, /* .NET name */
        { "zh_CHT",         "zh_TW" }, /* .NET name TODO: This should really be zh_Hant once the locale structure is fixed. */
        { "zh_GAN",         "zh__GAN" }, /* registered name */
        { "zh_GUOYU",       "zh" }, /* registered name */
        { "zh_HAKKA",       "zh__HAKKA" }, /* registered name */
        { "zh_MIN",         "zh__MIN" }, /* registered name */
        { "zh_MIN_NAN",     "zh__MINNAN" }, /* registered name */
        { "zh_WUU",         "zh__WUU" }, /* registered name */
        { "zh_XIANG",       "zh__XIANG" }, /* registered name */
        { "zh_YUE",         "zh__YUE" }, /* registered name */
        { "th_TH_TRADITIONAL", "th_TH@calendar=buddhist" },
        { "zh_TW_STROKE",   "zh_TW@collation=stroke" },
        { "zh__PINYIN",     "zh@collation=pinyin" }
    };
    /**
     * Construct a ULocale object from a {@link java.util.Locale}.
     * @param loc a JDK locale
     * @draft ICU 2.8
     */
    public ULocale(Locale loc) {
        if(loc == null){
            throw new IllegalArgumentException("The argument locale cannot be null");   
        }
        this.locale = loc;
        this.localeID = loc.toString();
    }
    /**
     * Construct a ULocale from a string of the form "sv_FI_ALAND".
     * By default this constructor will not normalize the localeID. 
     * 
     * @param locName string representation of the locale, e.g:
     * "en_US", "sy-Cyrl-YU"
     * @param localeID The locale identifier as a string
     * @draft ICU 2.8
     */ 
    public ULocale(String localeID) {
        this.localeID = getName(localeID);
    }


    /**
     * Construct a ULocale from a string of the form "sv_FI_ALAND".
	 * @param lang
	 * @param script
	 * @param country
     * @draft ICU 3.0
	 */
	public ULocale(String lang, String script, String country) {
		this(lang,script,country, "");
	}
    /**
     * Construct a ULocale from a string of the form "sv_FI_ALAND".
     * @param lang
     * @param script
     * @param country
     * @param variant
     * @draft ICU 3.0
     */
    public ULocale(String lang, String script, String country, String variant) {
        StringBuffer temp = new StringBuffer();
        
        temp.append(lang);
        if(script!=null && script.length() > 0){
            temp.append(UNDERSCORE);
            temp.append(toTitleCase(script));
        }
        if(country!=null && country.length() > 0){
            temp.append(UNDERSCORE);
            temp.append(country.toUpperCase());
        }
        if(variant!=null && variant.length() > 0){
            temp.append(UNDERSCORE);
            temp.append(variant.toUpperCase());
        }
        
        this.localeID = temp.toString();
    }

	/**
     * Convert this ULocale object to a {@link java.util.Locale}.
     * @return a JDK locale that either exactly represents this object
     * or is the closest approximation.
     * @draft ICU 2.8
     */
    public Locale toLocale() {
        if (locale == null) {
            locale = new Locale(localeID, "");
        }
        return locale;
    }
    
    /**
     * Return the current default ULocale.
     * @draft ICU 2.8
     */ 
    public static ULocale getDefault() {
        return new ULocale(Locale.getDefault());
    }
    /**
     * Sets the default locale for this instance of the Java Virtual Machine.
     * This does not affect the host locale.
     * <p>
     * If there is a security manager, its <code>checkPermission</code>
     * method is called with a <code>PropertyPermission("user.language", "write")</code>
     * permission before the default locale is changed.
     * <p>
     * The Java Virtual Machine sets the default locale during startup
     * based on the host environment. It is used by many locale-sensitive
     * methods if no locale is explicitly specified.
     * <p>
     * Since changing the default locale may affect many different areas
     * of functionality, this method should only be used if the caller
     * is prepared to reinitialize locale-sensitive code running
     * within the same Java Virtual Machine, such as the user interface.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        <code>checkPermission</code> method doesn't allow the operation.
     * @throws NullPointerException if <code>newLocale</code> is null
     * @param newLocale the new default locale
     * @see SecurityManager#checkPermission
     * @see java.util.PropertyPermission
     * @draft ICU 3.0
     */
    public static synchronized void setDefault(ULocale newLocale){
        Locale.setDefault(newLocale.locale);
    }
    
    /**
     * Overrides Cloneable
     * @draft ICU 3.0
     */
    public Object clone(){
        try {
            ULocale that = (ULocale)super.clone();
            return that;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Override hashCode.
     * Since Locales are often used in hashtables, caches the value
     * for speed.
     * @draft ICU 3.0
     */
      // TODO Depending on performance of synchronized, may want to just compute in constructor.
    public synchronized int hashCode() {
        return localeID.hashCode();
    }
    
    /**
     * Returns true if this Locale is equal to another object.  A Locale is
     * deemed equal to another Locale with identical language, country,
     * and variant, and unequal to all other objects.
     *
     * @return true if this Locale is equal to the specified object.
     * @draft ICU 3.0
     */
    public boolean equals(Object obj) {
        if (this == obj){// quick check
            return true;
        }
        if ((obj instanceof Locale)){
            return this.locale.equals(obj);
        }
        if((obj)instanceof String){
            return localeID.equals(obj);   
        }
        if(!(obj instanceof ULocale)){
            return false;
        }
        ULocale other = (ULocale) obj;
        
        return other.localeID.equals(localeID); // we made it through the guantlet.
        // (1)  We don't check super.equals since it is Object.
        //      Since Locale is final, we don't have to check both directions.
    }
    
    /**
     * Returns a list of all installed locales.
     * @draft ICU 3.0
     */
    public static ULocale[] getAvailableLocales() {
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME,"res_index");
        ICUResourceBundle index = rb.get("InstalledLocales");
        Enumeration enum = index.getKeys();
        ULocale[] locales = new ULocale[index.getSize()];
        int i=0;
        while(enum.hasMoreElements()){
           String id = (String) enum.nextElement();
           locales[i++] = new ULocale(id);
        }
        return locales;
    }

    /**
     * Returns a list of all 2-letter country codes defined in ISO 3166.
     * Can be used to create Locales.
     * @draft ICU 3.0
     */
    public static String[] getISOCountries() {
        return countries;
    }

    /**
     * Returns a list of all 2-letter language codes defined in ISO 639.
     * Can be used to create Locales.
     * [NOTE:  ISO 639 is not a stable standard-- some languages' codes have changed.
     * The list this function returns includes both the new and the old codes for the
     * languages whose codes have changed.]
     * @draft ICU 3.0
     */
    public static String[] getISOLanguages() {
        return languages;
    }

    /**
     * Returns the language code for this locale, which will either be the empty string
     * or a lowercase ISO 639 code.
     * @see #getDisplayLanguage
     * @draft ICU 3.0
     */
    public String getLanguage() {
        return getLanguage(localeID);
    }
    
    /**
     * Returns the language code for the locale ID specified,
     * which will either be the empty string
     * or a lowercase ISO 639 code.
     * @see #getDisplayLanguage
     * @draft ICU 3.0
     */
    public static String getLanguage(String localeID) {
        IDStruct struct = new IDStruct(localeID);
        return getLanguage(struct);
    }
     
    /**
     * Returns the country/region code for this locale, which will either be the empty string
     * or an upercase ISO 3166 2-letter code.
     * @see #getDisplayCountry
     * @draft ICU 3.0
     */
    public String getCountry() {
        return getCountry(localeID);
    }
    /**
     * Returns the country/region code for this locale, which will either be the empty string
     * or an upercase ISO 3166 2-letter code.
     * @param localeID
     * @see #getDisplayCountry
     * @draft ICU 3.0
     */
    public static String getCountry(String localeID) {
        IDStruct struct = new IDStruct(localeID);
        /* Skip the language */
        getLanguage(struct);
        if(struct.index<struct.idChars.length &&
                isIDSeparator(struct.idChars[struct.index])) {
            struct.index++;
            /* Skip the script if available */
            String scriptID = getScript(struct);
            if(scriptID.length()==0){
                struct.index--;   
            }
            if(struct.index<struct.idChars.length &&
                    isIDSeparator(struct.idChars[struct.index])) {
                struct.index++;
                return getCountry(struct);
            }
        }
        return "";
    }
    
    /**
     * Returns the script code for the specified locale.
     * @see #getDisplayScript
     * @draft ICU 3.0
     */
    public String getScript() {
        return getScript(localeID);
    }
    /**
     * Returns the script code for the specified locale.
     * @see #getDisplayScript
     * @draft ICU 3.0
     */
    public static String getScript(String localeID) {
        IDStruct struct = new IDStruct(localeID);
        getLanguage(struct);

        if(struct.index<struct.idChars.length &&
                isIDSeparator(struct.idChars[struct.index])) {
            struct.index++; // skip the sepeartor char
            return getScript(struct);
        }
        return "";
    }
    
    /**
     * Returns the variant code for this locale.
     * @see #getDisplayVariant
     * @draft ICU 3.0
     */
    public String getVariant() {
        return getVariant(localeID);
    }
    /**
     * Returns the variant code for this locale.
     * @see #getDisplayVariant
     * @draft ICU 3.0
     */
    public static String getVariant(String localeID) {
        IDStruct struct = new IDStruct(localeID);
        
        /* Skip the language */
        getLanguage(struct);
        if(struct.index<struct.idChars.length &&
                isIDSeparator(struct.idChars[struct.index])) {
            struct.index++;
            /* Skip the script if available */
            String scriptID = getScript(struct);
            if(scriptID.length()==0){
                struct.index--;   
            }
            /* Skip the Country */
            if(struct.index<struct.idChars.length &&
                    isIDSeparator(struct.idChars[struct.index])) {
                struct.index++;
                getCountry(struct);
                if(struct.index< struct.idChars.length && 
                        isIDSeparator(struct.idChars[struct.index])) {
                    struct.index++;
                    return getVariant(struct);
                }
            }
        }
        return "";     
    }
    /**
     * Gets the full name for the specified locale.
     * Note: This has the effect of 'canonicalizing' the ICU locale ID to
     * a certain extent. Upper and lower case are set as needed.
     *
     * @param localeID the locale to get the full name with
     * @return String the full name of the localeID
     * @draft ICU 3.0
     */
    public static String getName(String localeID){
        IDStruct struct = new IDStruct(localeID);
        return getName(struct,false);   
    }

    /**
     * Gets the full name for the this locale.
     * Note: This has the effect of 'canonicalizing' the ICU locale ID to
     * a certain extent. Upper and lower case are set as needed.
     *
     * @return String the full name of the localeID
     * @draft ICU 3.0
     */ 
    public String getName(){
        return getName(localeID);   
    }
    /**
     * Return a string representation of this object.
     * @draft ICU 3.0
     */
    public String toString() {
        return localeID;
    }
    private static class IDStruct{
        String id;
        char[] idChars;
        int index;
        private IDStruct(String localeID){
            id = localeID;
            idChars = localeID.toCharArray();
            index = 0;
        }
    }
    
    /*returns TRUE if a is an ID separator FALSE otherwise*/
    private static boolean isIDSeparator(char a){
        return (a == UNDERSCORE || a == HYPHEN);
    }

    private static boolean isPrefixLetter(char a){
        return ((a=='x')||(a=='X')||(a=='i')||(a=='I'));
    }
    
    /*returns TRUE if one of the special prefixes is here (s=string)
      'x-' or 'i-' */
    private static boolean isIDPrefix(char[] s) {
            return (isPrefixLetter(s[0]) && isIDSeparator(s[1]));
    }

    /* Dot terminates it because of POSIX form  where dot precedes the codepage
     * except for variant
     */
    private static boolean isTerminator(char a){
        return ((a==0)||(a==KEYWORD_SEPARATOR)||(a==DOT));   
    }

    /* linearly search the string array */
    private static int findIndex(String[] array, String target){
        for(int i=0; i<array.length;i++){
            if(array[i]!=null && array[i].compareTo(target)==0){
                return i;   
            }
        }
        return -1;
    }    
    private String toTitleCase(String str){
         char[] chars = str.toCharArray();
         chars[0] = Character.toUpperCase(chars[0]);
         return new String(chars);
    }
    /*
     * the internal functions _getLanguage(), _getCountry(), _getVariant()
     * avoid duplicating code to handle the earlier locale ID pieces
     * in the functions for the later ones by
     * setting the *pEnd pointer to where they stopped parsing
     *
     * TODO try to use this in Locale
     */
    private static String getLanguage(IDStruct struct) {

        int offset;
        StringBuffer lang = new StringBuffer(); /* temporary buffer to hold language code for searching */
        StringBuffer language = new StringBuffer();
        
        /* if it starts with i- or x- then copy that prefix */
        if(struct.index<struct.idChars.length &&
                isIDPrefix(struct.idChars)) {
            language.append(Character.toLowerCase(struct.idChars[struct.index]));
            language.append(HYPHEN);
            struct.index+=2;
        }
        
        /* copy the language as far as possible and count its length */
        while((struct.index < struct.idChars.length) && 
                !isTerminator(struct.idChars[struct.index]) && 
                !isIDSeparator(struct.idChars[struct.index])) {
            
            char ch = Character.toLowerCase(struct.idChars[struct.index]);
            
            language.append(ch);
            
            if(lang.length()<3){
                lang.append(ch);
            }
            
            struct.index++;
        }

        if(language.length()==3) {
            /* convert 3 character code to 2 character code if possible *CWB*/
            offset = findIndex(languages3, lang.toString());
            if(offset>=0) {
                language.setLength(0);
                language.append(languages[offset]);
            }
            
        }
        return language.toString();
    }
    private static String getCountry(IDStruct struct){
        
        StringBuffer cnty = new StringBuffer();
        StringBuffer country = new StringBuffer();
        int offset;

        /* copy the country as far as possible and count its length */
        while((struct.index < struct.idChars.length) && 
                !isTerminator(struct.idChars[struct.index]) && 
                !isIDSeparator(struct.idChars[struct.index])) {
            
            char ch = Character.toUpperCase(struct.idChars[struct.index]);
            country.append(ch);
            
            if(cnty.length()<3) {   /*CWB*/
                cnty.append(ch);
            }
            
            struct.index++;
        }

        /* convert 3 character code to 2 character code if possible *CWB*/
        if(cnty.length()==3) {
            offset= findIndex(countries3, cnty.toString());
            if(offset>=0) {
                country.setLength(0);
                country.append(countries[offset]);
            }else{
                offset = findIndex(obsoleteCountries3, cnty.toString());
                if(offset>=0){
                    country.setLength(0);
                    country.append(obsoleteCountries[offset]);   
                }
            }
        }

        return country.toString();
    }
    private static String getScript(IDStruct struct){
        int idLen = 0;
        StringBuffer script = new StringBuffer();
        /* copy the second item as far as possible and count its length */
        while((struct.index+idLen< struct.idChars.length) && 
                !isTerminator(struct.idChars[struct.index+idLen]) && 
                !isIDSeparator(struct.idChars[struct.index+idLen])) {
            idLen++;
        }

        /* If it's exactly 4 characters long, then it's a script and not a country. */
        if (idLen == 4) {
            
            if (idLen >= 1) {
                script.append(Character.toUpperCase(struct.idChars[struct.index++]));
            }
            for (int i = 1; i < idLen; i++) {
                script.append(struct.idChars[struct.index++]);
            }
        }
        else {
            idLen = 0;
        }
        return script.toString();
    }
    private static int locale_getKeywordsStart(IDStruct struct){
        int i = struct.idChars.length;
        while(--i>0){
            if(struct.idChars[i]==KEYWORD_SEPARATOR){
                return i;   
            }
        }
        return -1;
    }
    private static int strchr(IDStruct struct, char ch){
        return strchr(struct.idChars, struct.index, ch);
    }
    private static int strchr(char[] in, int start, char ch){
        for(int i=start; i<in.length; i++){
            if(ch==in[i]){
                return i;   
            }
        }
        return -1; 
    }
    private static String getVariant(IDStruct struct) {
        StringBuffer variant = new StringBuffer();
        char prev = struct.idChars[struct.index-1];
        int oldIndex = struct.index;

        /* get one or more variant tags and separate them with '_' */
        if(isIDSeparator(prev)) {
            /* get a variant string after a '-' or '_' */
            while((struct.index < struct.idChars.length) && 
                    !isTerminator(struct.idChars[struct.index])) {
                char ch = Character.toUpperCase(struct.idChars[struct.index]);
                if(ch==HYPHEN) {
                    ch=UNDERSCORE;
                }
                variant.append(ch);
                struct.index++;
            }
        }
        int temp = 0;
        /* if there is no variant tag after a '-' or '_' then look for '@' */
        if(oldIndex==struct.index) {
            if(prev==KEYWORD_SEPARATOR) {
                /* keep localeID */
            } else if((temp=locale_getKeywordsStart(struct))!=-1) {
                struct.index = temp;
                ++struct.index; /* point after the '@' */
            } else {
                return null;
            }
            while((struct.index < struct.idChars.length) && 
                  !isTerminator(struct.idChars[struct.index])) {
                char ch = Character.toUpperCase(struct.idChars[struct.index]);
                if(ch==HYPHEN || ch==COMA) {
                    ch = UNDERSCORE;
                }
                variant.append(ch);
                struct.index++;
            }
        }

        return variant.toString();
    }

    private static String getNext(IDStruct struct, char separator){
        int oldIndex = struct.index;
        String str = struct.id;
        while(struct.index < struct.idChars.length){
            char ch = str.charAt(struct.index);
            if(ch==separator){
                String temp = str.substring(oldIndex, struct.index);  
                struct.index++; /* increment the position so that we swallow the separator*/    
                return temp;
            }
            struct.index++;
        }
        return str.substring(oldIndex, struct.index);
    }

    private static int locale_getKeywords(IDStruct struct, TreeMap map, StringBuffer keyString){
        // here we assume that the IDStruct.index points
        // to begining of the keyword list
        int numKeywords=0;
        if(map==null){
            map = new TreeMap();   
        }
        while(struct.index < struct.idChars.length){
            struct.index++;
            String part = getNext(struct, ITEM_SEPARATOR).toLowerCase();
            int idx = part.indexOf(KEYWORD_ASSIGN);
            String keyword = part.substring(0, idx);
            String value   = part.substring(idx+1, part.length());
            map.put(keyword, value);
            numKeywords++;
        }
        if(keyString!=null){
            Set keyset = map.keySet();
            Iterator iter = keyset.iterator();
            while(iter.hasNext()){
                if(keyString.length()>0){
                    keyString.append(ITEM_SEPARATOR);   
                }
                String key = (String)iter.next();
                String value = (String)map.get(key);
                keyString.append(key);
                keyString.append(KEYWORD_ASSIGN);
                keyString.append(value);
            }
        }
        return numKeywords;

    }

    private static String getName(IDStruct struct,boolean stripKeywords){
       int fieldCount, scriptSize;
       boolean alreadyAddedAKeyword = false;
       StringBuffer name = new StringBuffer();
       /* get all pieces, one after another, and separate with '_' */
       fieldCount=0;
       scriptSize=0;
       
       /* get the language */
       name.append(getLanguage(struct));
       if(struct.index<struct.idChars.length &&
            isIDSeparator(struct.idChars[struct.index])) {
        
           ++fieldCount;
           name.append(UNDERSCORE);
           
           struct.index++; //skip past the separator
           String scriptID = getScript(struct); 
           name.append(scriptID);
           
           if(scriptID.length() > 0) {
               /* Found optional script */
               ++fieldCount;
               
               if (struct.index<struct.idChars.length &&
                    isIDSeparator(struct.idChars[struct.index])) {
                   /* If there is something else, then we add the _ */
                   name.append(UNDERSCORE);
                   
               }
           }else{
                struct.index--;
           }
    
           if (struct.index<struct.idChars.length &&
                isIDSeparator(struct.idChars[struct.index])) {
               struct.index++; //skip past the separator
               name.append(getCountry(struct));
               if(struct.index < struct.idChars.length &&
                    isIDSeparator(struct.idChars[struct.index])) {
                   ++fieldCount;
                   name.append(UNDERSCORE);
                   struct.index++; //skip past the separator
                   name.append(getVariant(struct));
               }
           }
       }
    
       if(!stripKeywords) {
           /* if we do not have a variant tag yet then try a POSIX variant after '@' */
           if((locale_getKeywordsStart(struct))!=-1) {
               int keywordIndicator = strchr(struct, KEYWORD_ASSIGN);
               int separatorIndicator = strchr(struct, ITEM_SEPARATOR);
               if(keywordIndicator > -1 && (separatorIndicator<0 || separatorIndicator > keywordIndicator)) {
                   
                   if(alreadyAddedAKeyword) {
                       name.append(ITEM_SEPARATOR);
                   } else {
                       name.append(KEYWORD_SEPARATOR);
                   }
                   
                   ++fieldCount;
                   StringBuffer keyString = new StringBuffer();
                   locale_getKeywords(struct, null, keyString);
                   name.append(keyString);
               } else if(fieldCount < 2 || (fieldCount < 3 && scriptSize > 0)) {
                
                   do {
                       name.append(UNDERSCORE);
                       ++fieldCount;
                   } while(fieldCount<2);
                   name.append(getVariant(struct));
               }
           }
       }
       return name.toString();
    }
    /**
     * Gets the full name for the specified locale.
     * Note: This has the effect of 'canonicalizing' the string to
     * a certain extent. Upper and lower case are set as needed.
     * It does NOT map aliased names in any way.
     *
     * @param localeID the locale to get the full name with
     * @return StringBuffer the canonicalized string buffer
     * @draft ICU 3.0
     */
    public static String canonicalize(String localeID){
        IDStruct struct = new IDStruct(localeID);

        // now we have an ID in the form xx_Yyyy_ZZ_KKKKK
        /* See if this is an already known locale */
        String locStr = getName(struct, false);
        for (int idx = 0; idx < variantsToKeywords.length; idx++) {
            if( variantsToKeywords[idx][0].compareTo(locStr) == 0) {
                locStr = variantsToKeywords[idx][1];
                break;
            }
        }
        /* convert the Euro variant to appropriate ID */
        int idx =locStr.indexOf("_EURO");
        if(idx>-1){
            locStr = locStr.substring(0,idx)+"@currency=EUR";       
        }
        
        return locStr;
    }
    
    /**
     * Returns a three-letter abbreviation for this locale's language.  If the locale
     * doesn't specify a language, this will be the empty string.  Otherwise, this will
     * be a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @draft ICU 3.0
     */
    public String getISO3Language(){
        return getISO3Language(localeID);
    }

    /**
     * Returns a three-letter abbreviation for this locale's language.  If the locale
     * doesn't specify a language, this will be the empty string.  Otherwise, this will
     * be a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @draft ICU 3.0
     */
    public static String getISO3Language(String localeID){
        String language = getLanguage(localeID);
        int offset = findIndex(languages, language);
        if(offset>=0){
            return languages3[offset];
        }
        return "";
    }
    
    /**
     * Returns a three-letter abbreviation for this locale's country.  If the locale
     * doesn't specify a country, this will be tbe the empty string.  Otherwise, this will
     * be an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @draft ICU 3.0
     */
    public String getISO3Country(){
        return getISO3Country(localeID);
    }
    /**
     * Returns a three-letter abbreviation for this locale's country.  If the locale
     * doesn't specify a country, this will be tbe the empty string.  Otherwise, this will
     * be an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @draft ICU 3.0
     */
    public static String getISO3Country(String localeID){
        String country = getCountry(localeID);
        int offset = findIndex(countries, country);
        if(offset>=0){
            return countries3[offset];
        }else{
            offset = findIndex(obsoleteCountries, country);
            if(offset>=0){
                return obsoleteCountries3[offset];   
            }
        }
        return "";
    }
    
    /**
     * Returns a name for the locale's language that is appropriate for display to the
     * user's defalt locale.
     * @return String the display string
     * @draft ICU 3.0
     */
    public String getDisplayLanguage() {
        return getDisplayLanguage(getDefault());
    }

    /**
     * Gets the language name suitable for display for the specified locale.
     *
     * @param displayLocale Specifies the locale to be used to display the name.  In other words,
     *                 if the locale's language code is "en", passing Locale::getFrench() for
     *                 inLocale would result in "Anglais", while passing Locale::getGerman()
     *                 for inLocale would result in "Englisch".
     * @return String the displayable language string.
     * @draft ICU 3.0
     */
    public String getDisplayLanguage(ULocale displayLocale) {
        return getDisplayLanguage(localeID, displayLocale.localeID);
    }
    
    /**
     * Returns a name for the locale's language that is appropriate for display to the
     * user's defalt locale.
     * @param localeID
     * @param displayLocaleID
     * @return String the display string
     * @draft ICU 3.0
     */
    public static String getDisplayLanguage(String localeID, String displaylocaleID) {
        return null;
    }
    
    /**
     * Gets the script name suitable for display for the user's default locale.
     * @return String the displayable script string. 
     * @draft ICU 3.0   
     */
    public String getDisplayScript() {
        return getDisplayScript(getDefault());
    }
    /**
     * Gets the script name suitable for display for the specified locale.
     * @param displayLocale the locale to get the displayable script code with. Null may be used to specify the default.
     * @return String the displayable script string.
     * @draft ICU 3.0
     */
    public String getDisplayScript(ULocale displayLocale) {
        return getDisplayScript(localeID, displayLocale.localeID);
    }
    
    /**
     * Gets the script name suitable for display for the specified locale.
     * @param localeID
     * @param displayLocale the locale to get the displayable script code with. Null may be used to specify the default.
     * @return String the displayable script string.
     * @draft ICU 3.0
     */
    public static String getDisplayScript(String localeID, String displaylocaleID) {
        return null;
    }
    
    /**
     * Gets the country name suitable for display for the user's default locale.
     * @return String the displayable country string. 
     * @draft ICU 3.0   
     */
    public String getDisplayCountry() {
        return getDisplayCountry(getDefault());
    }
    
    /**
     * Gets the country name suitable for display for the specified locale.
     * @param displayLocale the locale to get the displayable country code with. Null may be used to specify the default.
     * @return String the displayable country string.
     * @draft ICU 3.0
     */
    public String getDisplayCountry(ULocale displayLocale){
        return getDisplayCountry(localeID, displayLocale.localeID);   
    }
    
    /**
     * Gets the country name suitable for display for the user's default locale.
     * @return String the displayable country string. 
     * @draft ICU 3.0   
     */
    public static String getDisplayCountry(String localeID, String displaylocaleID) {
        return null;
    }

    /**
     * Gets the variant name suitable for display for the specified locale.
     * @return String the displayable variant string.
     * @draft ICU 3.0
     */
    public String getDisplayVariant(){
        return getDisplayVariant(getDefault());   
    }
    /**
     * Gets the variant name suitable for display for the specified locale.
     * @param displayLocale the locale to get the displayable variant code with. Null may be used to specify the default.
     * @return String the displayable variant string.
     * @draft ICU 3.0
     */
    public String getDisplayVariant(ULocale displayLocale){
        return getDisplayVariant(localeID, displayLocale.localeID);   
    }
    
    /**
     * Gets the variant name suitable for display for the specified locale.
     * @return String the displayable variant string.
     * @draft ICU 3.0
     */
    public static String getDisplayVariant(String localeID, String displaylocaleID){
        return null;   
    }
    
    /**
     * Gets the keyword name suitable for display for the user's default locale.
     * E.g: for the locale string de_DE@collation=PHONEBOOK, this API gets the display 
     * string for the keyword collation. 
     * Usage:
     * <code>
     *    TODO
     * </code>
     * @param keyword           The keyword whose display string needs to be returned.
     * @return String the display keyword string.  
     * @see #getKeywords
     * @draft ICU 3.0
     */
    public static String getDisplayKeyword(String keyword){
        return getDisplayKeyword(keyword, getDefault());   
    }
    
    /**
     * Gets the keyword name suitable for display for the specified locale.
     * E.g: for the locale string de_DE@collation=PHONEBOOK, this API gets the display 
     * string for the keyword collation. 
     * Usage:
     * <code>
     *    TODO
     * </code>
     * @param keyword           The keyword whose display string needs to be returned.
     * @param displayLocale     Specifies the locale to be used to display the name.  In other words,
     *                          if the locale's language code is "en", passing Locale::getFrench() for
     *                          inLocale would result in "Anglais", while passing Locale::getGerman()
     *                          for inLocale would result in "Englisch". NULL may be used to specify the default.
     * @return String the display keyword string.  
     * @see #getKeywords
     * @draft ICU 3.0
     */
    public static String getDisplayKeyword(String keyword, ULocale displayLocale){
        return getDisplayKeyword(keyword, displayLocale.localeID);   
    }

    /**
     * Gets the keyword name suitable for display for the user's default locale.
     * E.g: for the locale string de_DE@collation=PHONEBOOK, this API gets the display 
     * string for the keyword collation. 
     * Usage:
     * <code>
     *    TODO
     * </code>
     * @param keyword           The keyword whose display string needs to be returned.
     * @return String the display keyword string.  
     * @see #getKeywords
     * @draft ICU 3.0
     */
    public static String getDisplayKeyword(String keyword, String displayLocaleID){
        return null;   
    }
    
    /**
     * Gets the value of the keyword suitable for display for the user's default locale.
     * E.g: for the locale string de_DE@collation=PHONEBOOK, this API gets the display 
     * string for PHONEBOOK, in the display locale, when "collation" is specified as the keyword.
     *
     * @param keyword           The keyword for whose value should be used.
     * @return String the displayable keyword value.  
     * @draft ICU 3.0
     */
    public String getDisplayKeywordValue(String keyword){
        return getDisplayKeywordValue(keyword,getDefault());   
    }
    
    /**
     * Gets the value of the keyword suitable for display for the specified locale.
     * E.g: for the locale string de_DE@collation=PHONEBOOK, this API gets the display 
     * string for PHONEBOOK, in the display locale, when "collation" is specified as the keyword.
     *
     * @param keyword           The keyword for whose value should be used.
     * @param displayLocale     Specifies the locale to be used to display the name.  In other words,
     *                          if the locale's language code is "en", passing Locale::getFrench() for
     *                          inLocale would result in "Anglais", while passing Locale::getGerman()
     *                          for inLocale would result in "Englisch". NULL may be used to specify the default.
     * @return String the displayable keyword value.  
     * @draft ICU 3.0
     */
    public String getDisplayKeywordValue(String keyword, ULocale displayLocale){
        return getDisplayKeywordValue(localeID, keyword, displayLocale.localeID);   
    }

    /**
     * Gets the value of the keyword suitable for display for the specified locale.
     * E.g: for the locale string de_DE@collation=PHONEBOOK, this API gets the display 
     * string for PHONEBOOK, in the display locale, when "collation" is specified as the keyword.
     *
     * @param keyword           The keyword for whose value should be used.
     * @param displayLocale     Specifies the locale to be used to display the name.  In other words,
     *                          if the locale's language code is "en", passing Locale::getFrench() for
     *                          inLocale would result in "Anglais", while passing Locale::getGerman()
     *                          for inLocale would result in "Englisch". NULL may be used to specify the default.
     * @return String the displayable keyword value.  
     * @draft ICU 3.0
     */
    public static String getDisplayKeywordValue(String localeID, String keyword, String displaylocaleID){
        return null;   
    }
    
    /**
     * Gets the full name suitable for display for the user's default locale.
     * @return  the displayable name for this locale
     * @draft ICU 3.0
     */
    public String getDisplayName() {
        return getDisplayName(getDefault());
    }
    
    /**
     * Gets the full name suitable for display for the specified locale.
     *
     * @param displayLocale Specifies the locale to be used to display the name.  In other words,
     *                   if the locale's language code is "en", passing ULocale(Locale::getFrench()) for
     *                   inLocale would result in "Anglais", while passing ULocale(Locale::getGerman())
     *                   for displayLocale would result in "Englisch". NULL may be used to specify the default.
     * @return the displayable name for this locale
     * @draft ICU 3.0
     */
    public String getDisplayName(ULocale displayLocale) {
        return getDisplayName(localeID, displayLocale.localeID);
    }
    
    /**
     * Gets the full name suitable for display for the user's default locale.
     * @return  the displayable name for this locale
     * @draft ICU 3.0
     */
    public static String getDisplayName(String localeID, String displaylocaleID) {
        return null;
    }

  
    /**
     * Gets the base name for the specified locale after stripping the keywords.
     * Note: This has the effect of 'canonicalizing' the string to
     * a certain extent. Upper and lower case are set as needed.
     * This API strips off the keyword part, so "de_DE@collation=phonebook" 
     * will become "de_DE". 
     * @return the base name as a String.
     * @draft ICU 3.0
     */
    public String getBaseName(){
        return getBaseName(localeID);
    }
    
    /**
     * Gets the full name for the specified locale.
     * Note: This has the effect of 'canonicalizing' the string to
     * a certain extent. Upper and lower case are set as needed.
     * This API strips off the keyword part, so "de_DE@collation=phonebook" 
     * will become "de_DE". 
     * @param localeID the locale ID as a string
     * @return the base name as a String.
     * @draft ICU 3.0
     */
    public static String getBaseName(String localeID){
        IDStruct struct = new IDStruct(localeID);
        return getName(struct,true);    
    }

    /**
     * Gets an enumeration of keywords for the specified locale. Enumeration
     * @return enumeration of keywords or null if there are no keywords.
     * @draft ICU 3.0
     */
    public Enumeration getKeywords(){
        return getKeywords(localeID);
    }

    /**
     * Gets an enumeration of keywords for the specified locale. Enumeration
     * @return enumeration of keywords or null if there are no keywords.
     * @draft ICU 3.0
     */
    public static Enumeration getKeywords(String localeID){
        return null;
    }

    /**
     * Get the value for a keyword. Locale name does not need to be normalized.
     * @param keywordName name of the keyword for which we want the value. Case insensitive.
     * @return String the value of the keyword as a string
     * @draft ICU 3.0
     */
    public String getKeywordValue(String keywordName){
        return getKeywordValue(localeID, keywordName);
    }
    
    /**
     * Get the value for a keyword. Locale name does not need to be normalized.
     * @param keywordName name of the keyword for which we want the value. Case insensitive.
     * @return String the value of the keyword as a string
     * @draft ICU 3.0
     */
    public static String getKeywordValue(String localeID, String keywordName){
        return null;
    }
    
    /** 
     * Selector for <tt>getLocale()</tt> indicating the locale of the
     * resource containing the data.  This is always at or above the
     * valid locale.  If the valid locale does not contain the
     * specific data being requested, then the actual locale will be
     * above the valid locale.  If the object was not constructed from
     * locale data, then the valid locale is <i>null</i>.
     *
     * @draft ICU 2.8
     */
    public static Type ACTUAL_LOCALE = new Type(0);
 
    /** 
     * Selector for <tt>getLocale()</tt> indicating the most specific
     * locale for which any data exists.  This is always at or above
     * the requested locale, and at or below the actual locale.  If
     * the requested locale does not correspond to any resource data,
     * then the valid locale will be above the requested locale.  If
     * the object was not constructed from locale data, then the
     * actual locale is <i>null</i>.
     *
     * <p>Note: The valid locale will be returned correctly in ICU
     * 3.0 or later.  In ICU 2.8, it is not returned correctly.
     * @draft ICU 2.8
     */ 
    public static Type VALID_LOCALE = new Type(1);
    
    /**
     * Opaque selector enum for <tt>getLocale()</tt>.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @draft ICU 2.8
     */
    public static final class Type {
        private int localeType;
        private Type(int type) { localeType = type; }
    }
        
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        if (localeID == null) {
            localeID = locale.toString();
        }
        out.writeObject(localeID);
    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        localeID = (String)in.readObject();
        locale = null;
    }
    
   
}
