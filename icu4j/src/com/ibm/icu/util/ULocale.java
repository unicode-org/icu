/*
******************************************************************************
* Copyright (C) 2003-2004, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUResourceBundle;

/**
 * A class analogous to {@link java.util.Locale} that provides additional
 * support for ICU protocol.  In ICU 3.0 this class is enhanced to support
 * RFC 3066 language identifiers.
 *
 * <p>Many classes and services in ICU follow a factory idiom, in
 * which a factory method or object responds to a client request with
 * an object.  The request includes a locale (the <i>requested</i>
 * locale), and the returned object is constructed using data for that
 * locale.  The system may lack data for the requested locale, in
 * which case the locale fallback mechanism will be invoked until a
 * populated locale is found (the <i>valid</i> locale).  Furthermore,
 * even when a populated locale is found (the <i>valid</i> locale),
 * further fallback may be required to reach a locale containing the
 * specific data required by the service (the <i>actual</i> locale).
 *
 * <p>ULocale performs <b>'normalization'</b> and <b>'canonicalization'</b> of locale ids.
 * Normalization 'cleans up' ICU locale ids as follows:
 * <ul>
 * <li>language, script, country, variant, and keywords are properly cased<br>
 * (lower, title, upper, upper, and lower case respectively)</li>
 * <li>hyphens used as separators are converted to underscores</li>
 * <li>three-letter language and country ids are converted to two-letter
 * equivalents where available</li>
 * <li>surrounding spaces are removed from keywords and values</li>
 * <li>if there are multiple keywords, they are put in sorted order</li>
 * </ul>
 * Canonicalization additionally performs the following:
 * <ul>
 * <li>POSIX ids are converted to ICU format IDs</li>
 * <li>'grandfathered' 3066 ids are converted to ICU standard form</li>
 * <li>'PREEURO' and 'EURO' variants are converted to currency keyword form, with the currency
 * id appropriate to the country of the locale (for PREEURO) or EUR (for EURO).
 * </ul>
 * All ULocale constructors automatically normalize the locale id.  To handle
 * POSIX ids, <code>canonicalize</code> can be called to convert the id
 * to canonical form, or the <code>canonicalInstance</code> factory method
 * can be called.</p>
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
 * @deprecated This is a draft API and might change in a future release of ICU.
 * @draft ICU 2.8 
 */
public final class ULocale implements Serializable {
    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale ENGLISH = new ULocale("en", Locale.ENGLISH);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale FRENCH = new ULocale("fr", Locale.FRENCH);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale GERMAN = new ULocale("de", Locale.GERMAN);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale ITALIAN = new ULocale("it", Locale.ITALIAN);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale JAPANESE = new ULocale("ja", Locale.JAPANESE);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale KOREAN = new ULocale("ko", Locale.KOREAN);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale CHINESE = new ULocale("zh", Locale.CHINESE);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale SIMPLIFIED_CHINESE = new ULocale("zh_Hans", Locale.CHINESE);

    /** 
     * Useful constant for language.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale TRADITIONAL_CHINESE = new ULocale("zh_Hant", Locale.CHINESE);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale FRANCE = new ULocale("fr_FR", Locale.FRANCE);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale GERMANY = new ULocale("de_DE", Locale.GERMANY);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale ITALY = new ULocale("it_IT", Locale.ITALY);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale JAPAN = new ULocale("ja_JP", Locale.JAPAN);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale KOREA = new ULocale("ko_KR", Locale.KOREA);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale CHINA = new ULocale("zh_Hans_CN", Locale.CHINA);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale PRC = CHINA;

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale TAIWAN = new ULocale("zh_Hant_TW", Locale.TAIWAN);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale UK = new ULocale("en_GB", Locale.UK);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale US = new ULocale("en_US", Locale.US);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale CANADA = new ULocale("en_CA", Locale.CANADA);

    /** 
     * Useful constant for country/region.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final ULocale CANADA_FRENCH = new ULocale("fr_CA", Locale.CANADA_FRENCH);

    private static final HashMap CACHE = new HashMap(20);
    static {
        CACHE.put(Locale.ENGLISH, ENGLISH);
        CACHE.put(Locale.FRENCH, FRENCH);
        CACHE.put(Locale.GERMAN, GERMAN);
        CACHE.put(Locale.ITALIAN, ITALIAN);
        CACHE.put(Locale.JAPANESE, JAPANESE);
        CACHE.put(Locale.KOREAN, KOREAN);
        CACHE.put(Locale.CHINESE, CHINESE);
        CACHE.put(Locale.SIMPLIFIED_CHINESE, SIMPLIFIED_CHINESE);
        CACHE.put(Locale.TRADITIONAL_CHINESE, TRADITIONAL_CHINESE);
        CACHE.put(Locale.FRANCE, FRANCE);
        CACHE.put(Locale.GERMANY, GERMANY);
        CACHE.put(Locale.ITALY, ITALY);
        CACHE.put(Locale.JAPAN, JAPAN);
        CACHE.put(Locale.KOREA, KOREA);
        CACHE.put(Locale.CHINA, CHINA);
        CACHE.put(Locale.TAIWAN, TAIWAN);
        CACHE.put(Locale.UK, UK);
        CACHE.put(Locale.US, US);
        CACHE.put(Locale.CANADA, CANADA);
        CACHE.put(Locale.CANADA_FRENCH, CANADA_FRENCH);
    }

    /**
     * Handy constant.
     */
    private static final String EMPTY_STRING = "";

    // Used in both ULocale and IDParser, so moved up here.
    private static final char UNDERSCORE            = '_';

    /**
     * The root ULocale.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */ 
    public static final ULocale ROOT = new ULocale(EMPTY_STRING, null);
    
    /**
     * Cache the locale.
     */
    private transient Locale locale;

    /**
     * The raw localeID that we were passed in.
     */
    private String localeID;

    /**
     * Tables used in normalizing portions of the id.
     */
    /* tables updated per http://lcweb.loc.gov/standards/iso639-2/ 
       to include the revisions up to 2001/7/27 *CWB*/
    /* The 3 character codes are the terminology codes like RFC 3066.  
       This is compatible with prior ICU codes */
    /* "in" "iw" "ji" "jw" & "sh" have been withdrawn but are still in 
       the table but now at the end of the table because 
       3 character codes are duplicates.  This avoids bad searches
       going from 3 to 2 character codes.*/
    /* The range qaa-qtz is reserved for local use. */

    private static String[] _languages;
    private static String[] _replacementLanguages;
    private static String[] _obsoleteLanguages;
    private static String[] _languages3;
    private static String[] _obsoleteLanguages3;

    // Avoid initializing languages tables unless we have to.
    private static void initLanguageTables() {
        if (_languages == null) {

            /* This list MUST be in sorted order, and MUST contain the two-letter codes
               if one exists otherwise use the three letter code */
            String[] tempLanguages = {
                "aa",  "ab",  "ace", "ach", "ada", "ady", "ae",  "af",  "afa",
                "afh", "ak",  "akk", "ale", "alg", "am",  "an",  "ang", "apa",
                "ar",  "arc", "arn", "arp", "art", "arw", "as",  "ast",
                "ath", "aus", "av",  "awa", "ay",  "az",  "ba",  "bad",
                "bai", "bal", "ban", "bas", "bat", "be",  "bej",
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
                "nv",  "nwc", "ny",  "nym", "nyn", "nyo", "nzi", "oc",  "oj",
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
                "tl",  "tlh", "tli", "tmh", "tn",  "to",  "tog", "tpi", "tr",
                "ts",  "tsi", "tt",  "tum", "tup", "tut", "tvl", "tw",
                "ty",  "tyv", "udm", "ug",  "uga", "uk",  "umb", "und", "ur",
                "uz",  "vai", "ve",  "vi",  "vo",  "vot", "wa",  "wak",
                "wal", "war", "was", "wen", "wo",  "xal", "xh",  "yao", "yap",
                "yi",  "yo",  "ypk", "za",  "zap", "zen", "zh",  "znd",
                "zu",  "zun", 
            };

            String[] tempReplacementLanguages = {
                "id", "he", "yi", "jv", "sr", "nb",/* replacement language codes */
            };

            String[] tempObsoleteLanguages = {
                "in", "iw", "ji", "jw", "sh", "no",    /* obsolete language codes */         
            };

            /* This list MUST contain a three-letter code for every two-letter code in the
               list above, and they MUST ne in the same order (i.e., the same language must
               be in the same place in both lists)! */
            String[] tempLanguages3 = {
                /*"aa",  "ab",  "ace", "ach", "ada", "ady", "ae",  "af",  "afa",    */
                "aar", "abk", "ace", "ach", "ada", "ady", "ave", "afr", "afa",
                /*"afh", "ak",  "akk", "ale", "alg", "am",  "an",  "ang", "apa",    */
                "afh", "aka", "akk", "ale", "alg", "amh", "arg", "ang", "apa",
                /*"ar",  "arc", "arn", "arp", "art", "arw", "as",  "ast",    */
                "ara", "arc", "arn", "arp", "art", "arw", "asm", "ast",
                /*"ath", "aus", "av",  "awa", "ay",  "az",  "ba",  "bad",    */
                "ath", "aus", "ava", "awa", "aym", "aze", "bak", "bad",
                /*"bai", "bal", "ban", "bas", "bat", "be",  "bej",    */
                "bai", "bal", "ban", "bas", "bat", "bel", "bej",
                /*"bem", "ber", "bg",  "bh",  "bho", "bi",  "bik", "bin",    */
                "bem", "ber", "bul", "bih", "bho", "bis", "bik", "bin",
                /*"bla", "bm",  "bn",  "bnt", "bo",  "br",  "bra", "bs",     */
                "bla", "bam",  "ben", "bnt", "bod", "bre", "bra", "bos",
                /*"btk", "bua", "bug", "byn", "ca",  "cad", "cai", "car", "cau",    */
                "btk", "bua", "bug", "byn", "cat", "cad", "cai", "car", "cau",
                /*"ce",  "ceb", "cel", "ch",  "chb", "chg", "chk", "chm",    */
                "che", "ceb", "cel", "cha", "chb", "chg", "chk", "chm",
                /*"chn", "cho", "chp", "chr", "chy", "cmc", "co",  "cop",    */
                "chn", "cho", "chp", "chr", "chy", "cmc", "cos", "cop",
                /*"cpe", "cpf", "cpp", "cr",  "crh", "crp", "cs",  "csb", "cu",  "cus",    */
                "cpe", "cpf", "cpp", "cre", "crh", "crp", "ces", "csb", "chu", "cus",
                /*"cv",  "cy",  "da",  "dak", "dar", "day", "de",  "del", "den",    */
                "chv", "cym", "dan", "dak", "dar", "day", "deu", "del", "den",
                /*"dgr", "din", "doi", "dra", "dsb", "dua", "dum", "dv",  "dyu",    */
                "dgr", "din", "doi", "dra", "dsb", "dua", "dum", "div", "dyu",
                /*"dz",  "ee",  "efi", "egy", "eka", "el",  "elx", "en",     */
                "dzo", "ewe", "efi", "egy", "eka", "ell", "elx", "eng",
                /*"enm", "eo",  "es",  "et",  "eu",  "ewo", "fa",     */
                "enm", "epo", "spa", "est", "eus", "ewo", "fas",
                /*"fan", "fat", "ff",  "fi",  "fiu", "fj",  "fo",  "fon",    */
                "fan", "fat", "ful", "fin", "fiu", "fij", "fao", "fon",
                /*"fr",  "frm", "fro", "fur", "fy",  "ga",  "gaa", "gay",    */
                "fra", "frm", "fro", "fur", "fry", "gle", "gaa", "gay",
                /*"gba", "gd",  "gem", "gez", "gil", "gl",  "gmh", "gn",     */
                "gba", "gla", "gem", "gez", "gil", "glg", "gmh", "grn",
                /*"goh", "gon", "gor", "got", "grb", "grc", "gu",  "gv",     */
                "goh", "gon", "gor", "got", "grb", "grc", "guj", "glv",
                /*"gwi", "ha",  "hai", "haw", "he",  "hi",  "hil", "him",    */
                "gwi", "hau", "hai", "haw", "heb", "hin", "hil", "him",
                /*"hit", "hmn", "ho",  "hr",  "hsb", "ht",  "hu",  "hup", "hy",  "hz",     */
                "hit", "hmn", "hmo", "hrv", "hsb", "hat", "hun", "hup", "hye", "her",
                /*"ia",  "iba", "id",  "ie",  "ig",  "ii",  "ijo", "ik",     */
                "ina", "iba", "ind", "ile", "ibo", "iii", "ijo", "ipk",
                /*"ilo", "inc", "ine", "inh", "io",  "ira", "iro", "is",  "it",      */
                "ilo", "inc", "ine", "inh", "ido", "ira", "iro", "isl", "ita",
                /*"iu",  "ja",  "jbo", "jpr", "jrb", "jv",  "ka",  "kaa", "kab",   */
                "iku", "jpn", "jbo", "jpr", "jrb", "jaw", "kat", "kaa", "kab",
                /*"kac", "kam", "kar", "kaw", "kbd", "kg",  "kha", "khi",    */
                "kac", "kam", "kar", "kaw", "kbd", "kon", "kha", "khi",
                /*"kho", "ki",  "kj",  "kk",  "kl",  "km",  "kmb", "kn",     */
                "kho", "kik", "kua", "kaz", "kal", "khm", "kmb", "kan",
                /*"ko",  "kok", "kos", "kpe", "kr",  "krc", "kro", "kru", "ks",     */
                "kor", "kok", "kos", "kpe", "kau", "krc", "kro", "kru", "kas",
                /*"ku",  "kum", "kut", "kv",  "kw",  "ky",  "la",  "lad",    */
                "kur", "kum", "kut", "kom", "cor", "kir", "lat", "lad",
                /*"lah", "lam", "lb",  "lez", "lg",  "li",  "ln",  "lo",  "lol",    */
                "lah", "lam", "ltz", "lez", "lug", "lim", "lin", "lao", "lol",
                /*"loz", "lt",  "lu",  "lua", "lui", "lun", "luo", "lus",    */
                "loz", "lit", "lub", "lua", "lui", "lun", "luo", "lus",
                /*"lv",  "mad", "mag", "mai", "mak", "man", "map", "mas",    */
                "lav", "mad", "mag", "mai", "mak", "man", "map", "mas",
                /*"mdf", "mdr", "men", "mg",  "mga", "mh",  "mi",  "mic", "min",    */
                "mdf", "mdr", "men", "mlg", "mga", "mah", "mri", "mic", "min",
                /*"mis", "mk",  "mkh", "ml",  "mn",  "mnc", "mni", "mno",    */
                "mis", "mkd", "mkh", "mal", "mon", "mnc", "mni", "mno",
                /*"mo",  "moh", "mos", "mr",  "ms",  "mt",  "mul", "mun",    */
                "mol", "moh", "mos", "mar", "msa", "mlt", "mul", "mun",
                /*"mus", "mwr", "my",  "myn", "myv", "na",  "nah", "nai", "nap",    */
                "mus", "mwr", "mya", "myn", "myv", "nau", "nah", "nai", "nap",
                /*"nb",  "nd",  "nds", "ne",  "new", "ng",  "nia", "nic",    */
                "nob", "nde", "nds", "nep", "new", "ndo", "nia", "nic",
                /*"niu", "nl",  "nn",  "no",  "nog", "non", "nr",  "nso", "nub",    */
                "niu", "nld", "nno", "nor", "nog", "non", "nbl", "nso", "nub",
                /*"nv",  "nwc", "ny",  "nym", "nyn", "nyo", "nzi", "oc",  "oj",     */
                "nav", "nwc", "nya", "nym", "nyn", "nyo", "nzi", "oci", "oji",
                /*"om",  "or",  "os",  "osa", "ota", "oto", "pa",  "paa",    */
                "orm", "ori", "oss", "osa", "ota", "oto", "pan", "paa",
                /*"pag", "pal", "pam", "pap", "pau", "peo", "phi", "phn",    */
                "pag", "pal", "pam", "pap", "pau", "peo", "phi", "phn",
                /*"pi",  "pl",  "pon", "pra", "pro", "ps",  "pt",  "qu",     */
                "pli", "pol", "pon", "pra", "pro", "pus", "por", "que",
                /*"raj", "rap", "rar", "rm",  "rn",  "ro",  "roa", "rom",    */
                "raj", "rap", "rar", "roh", "run", "ron", "roa", "rom",
                /*"ru",  "rw",  "sa",  "sad", "sah", "sai", "sal", "sam",    */
                "rus", "kin", "san", "sad", "sah", "sai", "sal", "sam",
                /*"sas", "sat", "sc",  "sco", "sd",  "se",  "sel", "sem",    */
                "sas", "sat", "srd", "sco", "snd", "sme", "sel", "sem",
                /*"sg",  "sga", "sgn", "shn", "si",  "sid", "sio", "sit",    */
                "sag", "sga", "sgn", "shn", "sin", "sid", "sio", "sit",
                /*"sk",  "sl",  "sla", "sm",  "sma", "smi", "smj", "smn",    */
                "slk", "slv", "sla", "smo", "sma", "smi", "smj", "smn",
                /*"sms", "sn",  "snk", "so",  "sog", "son", "sq",  "sr",     */
                "sms", "sna", "snk", "som", "sog", "son", "sqi", "srp",
                /*"srr", "ss",  "ssa", "st",  "su",  "suk", "sus", "sux",    */
                "srr", "ssw", "ssa", "sot", "sun", "suk", "sus", "sux",
                /*"sv",  "sw",  "syr", "ta",  "tai", "te",  "tem", "ter",    */
                "swe", "swa", "syr", "tam", "tai", "tel", "tem", "ter",
                /*"tet", "tg",  "th",  "ti",  "tig", "tiv", "tk",  "tkl",    */
                "tet", "tgk", "tha", "tir", "tig", "tiv", "tuk", "tkl",
                /*"tl",  "tlh", "tli", "tmh", "tn",  "to",  "tog", "tpi", "tr",     */
                "tgl", "tlh", "tli", "tmh", "tsn", "ton", "tog", "tpi", "tur",
                /*"ts",  "tsi", "tt",  "tum", "tup", "tut", "tvl", "tw",     */
                "tso", "tsi", "tat", "tum", "tup", "tut", "tvl", "twi",
                /*"ty",  "tyv", "udm", "ug",  "uga", "uk",  "umb", "und", "ur",     */
                "tah", "tyv", "udm", "uig", "uga", "ukr", "umb", "und", "urd",
                /*"uz",  "vai", "ve",  "vi",  "vo",  "vot", "wa",  "wak",    */
                "uzb", "vai", "ven", "vie", "vol", "vot", "wln", "wak",
                /*"wal", "war", "was", "wen", "wo",  "xal", "xh",  "yao", "yap",    */
                "wal", "war", "was", "wen", "wol", "xal", "xho", "yao", "yap",
                /*"yi",  "yo",  "ypk", "za",  "zap", "zen", "zh",  "znd",    */
                "yid", "yor", "ypk", "zha", "zap", "zen", "zho", "znd",
                /*"zu",  "zun",                                              */
                "zul", "zun",  
            };
    
            String[] tempObsoleteLanguages3 = {
                /* "in",  "iw",  "ji",  "jw",  "sh", */
                "ind", "heb", "yid", "jaw", "srp", 
            };

            synchronized (ULocale.class) {
                if (_languages == null) {
                    _languages = tempLanguages;
                    _replacementLanguages = tempReplacementLanguages;
                    _obsoleteLanguages = tempObsoleteLanguages;
                    _languages3 = tempLanguages3;
                    _obsoleteLanguages3 = tempObsoleteLanguages3;
                }
            }
        }
    }

    private static String[] _countries;
    private static String[] _obsoleteCountries;
    private static String[] _countries3;
    private static String[] _obsoleteCountries3;

    // Avoid initializing country tables unless we have to.
    private static void initCountryTables() {    
        if (_countries == null) {
            /* ZR(ZAR) is now CD(COD) and FX(FXX) is PS(PSE) as per
               http://www.evertype.com/standards/iso3166/iso3166-1-en.html 
               added new codes keeping the old ones for compatibility
               updated to include 1999/12/03 revisions *CWB*/
    
            /* RO(ROM) is now RO(ROU) according to 
               http://www.iso.org/iso/en/prods-services/iso3166ma/03updates-on-iso-3166/nlv3e-rou.html
            */
    
            /* This list MUST be in sorted order, and MUST contain only two-letter codes! */
            String[] tempCountries = {
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
    
            String[] tempObsoleteCountries = {
                "FX",  "RO",  "TP",  "ZR",   /* obsolete country codes */      
            };
    
            /* This list MUST contain a three-letter code for every two-letter code in
               the above list, and they MUST be listed in the same order! */
            String[] tempCountries3 = {
                /*"AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",  "AN",     */
                "AND", "ARE", "AFG", "ATG", "AIA", "ALB", "ARM", "ANT",
                /*"AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AZ",     */
                "AGO", "ATA", "ARG", "ASM", "AUT", "AUS", "ABW", "AZE",
                /*"BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",     */
                "BIH", "BRB", "BGD", "BEL", "BFA", "BGR", "BHR", "BDI",
                /*"BJ",  "BM",  "BN",  "BO",  "BR",  "BS",  "BT",  "BV",     */
                "BEN", "BMU", "BRN", "BOL", "BRA", "BHS", "BTN", "BVT",
                /*"BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",     */
                "BWA", "BLR", "BLZ", "CAN", "CCK", "COD", "CAF", "COG",
                /*"CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",     */
                "CHE", "CIV", "COK", "CHL", "CMR", "CHN", "COL", "CRI",
                /*"CU",  "CV",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",     */
                "CUB", "CPV", "CXR", "CYP", "CZE", "DEU", "DJI", "DNK",
                /*"DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",     */
                "DMA", "DOM", "DZA", "ECU", "EST", "EGY", "ESH", "ERI",
                /*"ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",     */
                "ESP", "ETH", "FIN", "FJI", "FLK", "FSM", "FRO", "FRA",
                /*"GA",  "GB",  "GD",  "GE",  "GF",  "GH",  "GI",  "GL",     */
                "GAB", "GBR", "GRD", "GEO", "GUF", "GHA", "GIB", "GRL",
                /*"GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",     */
                "GMB", "GIN", "GLP", "GNQ", "GRC", "SGS", "GTM", "GUM",
                /*"GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",     */
                "GNB", "GUY", "HKG", "HMD", "HND", "HRV", "HTI", "HUN",
                /*"ID",  "IE",  "IL",  "IN",  "IO",  "IQ",  "IR",  "IS",     */
                "IDN", "IRL", "ISR", "IND", "IOT", "IRQ", "IRN", "ISL",
                /*"IT",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",     */
                "ITA", "JAM", "JOR", "JPN", "KEN", "KGZ", "KHM", "KIR",
                /*"KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",     */
                "COM", "KNA", "PRK", "KOR", "KWT", "CYM", "KAZ", "LAO",
                /*"LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",     */
                "LBN", "LCA", "LIE", "LKA", "LBR", "LSO", "LTU", "LUX",
                /*"LV",  "LY",  "MA",  "MC",  "MD",  "MG",  "MH",  "MK",     */
                "LVA", "LBY", "MAR", "MCO", "MDA", "MDG", "MHL", "MKD",
                /*"ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",     */
                "MLI", "MMR", "MNG", "MAC", "MNP", "MTQ", "MRT", "MSR",
                /*"MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",     */
                "MLT", "MUS", "MDV", "MWI", "MEX", "MYS", "MOZ", "NAM",
                /*"NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",     */
                "NCL", "NER", "NFK", "NGA", "NIC", "NLD", "NOR", "NPL",
                /*"NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",     */
                "NRU", "NIU", "NZL", "OMN", "PAN", "PER", "PYF", "PNG",
                /*"PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",     */
                "PHL", "PAK", "POL", "SPM", "PCN", "PRI", "PSE", "PRT",
                /*"PW",  "PY",  "QA",  "RE",  "RO",  "RU",  "RW",  "SA",     */
                "PLW", "PRY", "QAT", "REU", "ROU", "RUS", "RWA", "SAU",
                /*"SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",     */
                "SLB", "SYC", "SDN", "SWE", "SGP", "SHN", "SVN", "SJM",
                /*"SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "ST",  "SV",     */
                "SVK", "SLE", "SMR", "SEN", "SOM", "SUR", "STP", "SLV",
                /*"SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",     */
                "SYR", "SWZ", "TCA", "TCD", "ATF", "TGO", "THA", "TJK",
                /*"TK",  "TL",  "TM",  "TN",  "TO",  "TR",  "TT",  "TV",     */
                "TKL", "TLS", "TKM", "TUN", "TON", "TUR", "TTO", "TUV",
                /*"TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",     */
                "TWN", "TZA", "UKR", "UGA", "UMI", "USA", "URY", "UZB",
                /*"VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",     */
                "VAT", "VCT", "VEN", "VGB", "VIR", "VNM", "VUT", "WLF",
                /*"WS",  "YE",  "YT",  "YU",  "ZA",  "ZM",  "ZW",            */
                "WSM", "YEM", "MYT", "YUG", "ZAF", "ZMB", "ZWE",
            };
    
            String[] tempObsoleteCountries3 = {
                /*"FX",  "RO",  "TP",  "ZR",   */
                "FXX", "ROM", "TMP", "ZAR",    
            };

            synchronized (ULocale.class) {
                if (_countries == null) {
                    _countries = tempCountries;
                    _obsoleteCountries = tempObsoleteCountries;
                    _countries3 = tempCountries3;
                    _obsoleteCountries3 = tempObsoleteCountries3;
                }
            }
        }
    }

    private static String[][] _variantsToKeywords;

    private static void initVariantsTable() {
        if (_variantsToKeywords == null) {
            /**
             * This table lists pairs of locale ids for canonicalization.  The
             * The first item is the normalized id, the second item is the
             * canonicalized id.
             */
            String[][] tempVariantsToKeywords = {
//              { EMPTY_STRING,     "en_US_POSIX", null, null }, /* .NET name */
                { "C",              "en_US_POSIX", null, null }, /* POSIX name */
                { "art_LOJBAN",     "jbo", null, null }, /* registered name */
                { "az_AZ_CYRL",     "az_Cyrl_AZ", null, null }, /* .NET name */
                { "az_AZ_LATN",     "az_Latn_AZ", null, null }, /* .NET name */
                { "ca_ES_PREEURO",  "ca_ES", "currency", "ESP" },
                { "cel_GAULISH",    "cel__GAULISH", null, null }, /* registered name */
                { "de_1901",        "de__1901", null, null }, /* registered name */
                { "de_1906",        "de__1906", null, null }, /* registered name */
                { "de__PHONEBOOK",  "de", "collation", "phonebook" },
                { "de_AT_PREEURO",  "de_AT", "currency", "ATS" },
                { "de_DE_PREEURO",  "de_DE", "currency", "DEM" },
                { "de_LU_PREEURO",  "de_LU", "currency", "EUR" },
                { "el_GR_PREEURO",  "el_GR", "currency", "GRD" },
                { "en_BOONT",       "en__BOONT", null, null }, /* registered name */
                { "en_SCOUSE",      "en__SCOUSE", null, null }, /* registered name */
                { "en_BE_PREEURO",  "en_BE", "currency", "BEF" },
                { "en_IE_PREEURO",  "en_IE", "currency", "IEP" },
                { "es__TRADITIONAL", "es", "collation", "traditional" },
                { "es_ES_PREEURO",  "es_ES", "currency", "ESP" },
                { "eu_ES_PREEURO",  "eu_ES", "currency", "ESP" },
                { "fi_FI_PREEURO",  "fi_FI", "currency", "FIM" },
                { "fr_BE_PREEURO",  "fr_BE", "currency", "BEF" },
                { "fr_FR_PREEURO",  "fr_FR", "currency", "FRF" },
                { "fr_LU_PREEURO",  "fr_LU", "currency", "LUF" },
                { "ga_IE_PREEURO",  "ga_IE", "currency", "IEP" },
                { "gl_ES_PREEURO",  "gl_ES", "currency", "ESP" },
                { "hi__DIRECT",     "hi", "collation", "direct" },
                { "it_IT_PREEURO",  "it_IT", "currency", "ITL" },
                { "ja_JP_TRADITIONAL", "ja_JP", "calendar", "japanese" },
//              { "nb_NO_NY",       "nn_NO", null, null },
                { "nl_BE_PREEURO",  "nl_BE", "currency", "BEF" },
                { "nl_NL_PREEURO",  "nl_NL", "currency", "NLG" },
                { "pt_PT_PREEURO",  "pt_PT", "currency", "PTE" },
                { "sl_ROZAJ",       "sl__ROZAJ", null, null }, /* registered name */
                { "sr_SP_CYRL",     "sr_Cyrl_SP", null, null }, /* .NET name */
                { "sr_SP_LATN",     "sr_Latn_SP", null, null }, /* .NET name */
                { "uz_UZ_CYRL",     "uz_Cyrl_UZ", null, null }, /* .NET name */
                { "uz_UZ_LATN",     "uz_Latn_UZ", null, null }, /* .NET name */
                { "zh_CHS",         "zh_Hans", null, null }, /* .NET name */
                { "zh_CHT",         "zh_Hant", null, null }, /* .NET name TODO: This should be zh_Hant once the locale structure is fixed. */
                { "zh_GAN",         "zh__GAN", null, null }, /* registered name */
                { "zh_GUOYU",       "zh", null, null }, /* registered name */
                { "zh_HAKKA",       "zh__HAKKA", null, null }, /* registered name */
                { "zh_MIN",         "zh__MIN", null, null }, /* registered name */
                { "zh_MIN_NAN",     "zh__MINNAN", null, null }, /* registered name */
                { "zh_WUU",         "zh__WUU", null, null }, /* registered name */
                { "zh_XIANG",       "zh__XIANG", null, null }, /* registered name */
                { "zh_YUE",         "zh__YUE", null, null }, /* registered name */
                { "th_TH_TRADITIONAL", "th_TH", "calendar", "buddhist" },
                { "zh_TW_STROKE",   "zh_TW", "collation", "stroke" },
                { "zh__PINYIN",     "zh", "collation", "pinyin" }
            };
    
            synchronized (ULocale.class) {
                if (_variantsToKeywords == null) {
                    _variantsToKeywords = tempVariantsToKeywords;
                }
            }
        }
    }

    /**
     * Private constructor used by static initializers.
     */
    private ULocale(String localeID, Locale locale) {
        this.localeID = localeID;
        this.locale = locale;
    }

    /**
     * Construct a ULocale object from a {@link java.util.Locale}.
     * @param loc a JDK locale
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    private ULocale(Locale loc) {
        this.localeID = getName(loc.toString());
        this.locale = loc;
    }

    /**
     * Return a ULocale object for a {@link java.util.Locale}.
     * The ULocale is canonicalized.
     * @param loc a JDK locale
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static ULocale forLocale(Locale loc) {
        if (loc == null) {
            return null;
        }
        ULocale result = (ULocale)CACHE.get(loc);
        if (result == null && defaultULocale != null && loc == defaultULocale.locale) {
            result = defaultULocale;
        } else {
            result = new ULocale(loc.toString(), loc);
        }
        return result;
    }

    /**
     * Construct a ULocale from a RFC 3066 locale ID. The locale ID consists
     * of optional language, script, country, and variant fields in that order, 
     * separated by underscores, followed by an optional keyword list.  The
     * script, if present, is four characters long-- this distinguishes it
     * from a country code, which is two characters long.  Other fields
     * are distinguished by position as indicated by the underscores.  The
     * start of the keyword list is indicated by '@', and consists of one
     * or more keyword/value pairs separated by commas.
     * <p>
     * This constructor not canonicalize the localeID.
     * 
     * @param localeID string representation of the locale, e.g:
     * "en_US", "sy_Cyrl_YU", "zh__pinyin", "es_ES@currency=EUR,collation=traditional"
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */ 
    public ULocale(String localeID) {
        this.localeID = getName(localeID);
    }

    /**
     * Construct a ULocale from a localeID constructed from the three 'fields' a, b, and c.  These
     * fields are concatenated using underscores to form a localeID of
     * the form a_b_c, which is then handled like the localeID passed
     * to <code>ULocale(String localeID)</code>.  
     *
     * <p>Java locale strings consisting of language, country, and
     * variant will be handled by this form, since the country code
     * (being shorter than four letters long) will not be interpreted
     * as a script code.  If a script code is present, the final
     * argument ('c') will be interpreted as the country code.  It is
     * recommended that this constructor only be used to ease porting,
     * and that clients instead use the single-argument constructor
     * when constructing a ULocale from a localeID.
     * @param a first component of the locale id
     * @param b second component of the locale id
     * @param c third component of the locale id
     * @see #ULocale(String)
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public ULocale(String a, String b, String c) {
        localeID = getName(lscvToID(a, b, c, EMPTY_STRING));
    }

    /**
     * Create a ULocale from the id by first canonicalizing the id.
     * @param nonCanonicalID the locale id to canonicalize
     * @return the locale created from the canonical version of the ID.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static ULocale createCanonical(String nonCanonicalID) {
        return new ULocale(canonicalize(nonCanonicalID), null);
    }

    private static String lscvToID(String lang, String script, String country, String variant) {
        StringBuffer buf = new StringBuffer();
     
        if (lang != null && lang.length() > 0) {
            buf.append(lang);
        }
        if (script != null && script.length() > 0) {
            buf.append(UNDERSCORE);
            buf.append(script);
        }
        if (country != null && country.length() > 0) {
            buf.append(UNDERSCORE);
            buf.append(country);
        }
        if (variant != null && variant.length() > 0) {
            if (country == null || country.length() == 0) {
                buf.append(UNDERSCORE);
            }
            buf.append(UNDERSCORE);
            buf.append(variant);
        }
        return buf.toString();
    }

    /**
     * Convert this ULocale object to a {@link java.util.Locale}.
     * @return a JDK locale that either exactly represents this object
     * or is the closest approximation.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Locale toLocale() {
        if (locale == null) {
            String[] names = new IDParser(localeID).getLanguageScriptCountryVariant();
            locale = new Locale(names[0], names[2], names[3]);
        }
        return locale;
    }
    
    /**
     * Keep our own default ULocale.
     */
    private static ULocale defaultULocale;

    /**
     * Returns the current default ULocale.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */ 
    public static ULocale getDefault() {
        synchronized (ULocale.class) {
            Locale defaultLocale = Locale.getDefault();
            if (defaultULocale == null || defaultULocale.toLocale() != defaultLocale) {
                defaultULocale = new ULocale(defaultLocale);
            }
            return defaultULocale;
        }
    }

    /**
     * Sets the default ULocale.  This also sets the default Locale.
     * If the caller does not have write permission to the
     * user.language property, a security exception will be thrown,
     * and the default ULocale will remain unchanged.
     * @throws SecurityException
     *        if a security manager exists and its
     *        <code>checkPermission</code> method doesn't allow the operation.
     * @throws NullPointerException if <code>newLocale</code> is null
     * @param newLocale the new default locale
     * @see SecurityManager#checkPermission
     * @see java.util.PropertyPermission
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static synchronized void setDefault(ULocale newLocale){
        Locale.setDefault(newLocale.toLocale());
        defaultULocale = newLocale;
    }
    
    /**
     * This is for compatibility with Locale-- in actuality, since ULocale is
     * immutable, there is no reason to clone it, so this API returns 'this'.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Object clone() {
        return this;
    }

    /**
     * Returns the hashCode.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int hashCode() {
        return localeID.hashCode();
    }
    
    /**
     * Returns true if the other object is another ULocale with the
     * same full name, or is a String localeID that matches the full name.
     * Note that since names are not canonicalized, two ULocales that
     * function identically might not compare equal.
     *
     * @return true if this Locale is equal to the specified object.
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof String) {
            return localeID.equals((String)obj);   
        }
        if (obj instanceof ULocale) {
            return localeID.equals(((ULocale)obj).localeID);
        }
        return false;
    }
    
    /**
     * Returns a list of all installed locales.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static ULocale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableULocales();
    }

    /**
     * Returns a list of all 2-letter country codes defined in ISO 3166.
     * Can be used to create Locales.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String[] getISOCountries() {
        initCountryTables();
        return (String[])_countries.clone();
    }

    /**
     * Returns a list of all 2-letter language codes defined in ISO 639.
     * Can be used to create Locales.
     * [NOTE:  ISO 639 is not a stable standard-- some languages' codes have changed.
     * The list this function returns includes both the new and the old codes for the
     * languages whose codes have changed.]
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String[] getISOLanguages() {
        initLanguageTables();
        return (String[])_languages.clone();
    }

    /**
     * Returns the language code for this locale, which will either be the empty string
     * or a lowercase ISO 639 code.
     * @see #getDisplayLanguage
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getLanguage() {
        return getLanguage(localeID);
    }
    
    /**
     * Returns the language code for the locale ID,
     * which will either be the empty string
     * or a lowercase ISO 639 code.
     * @see #getDisplayLanguage
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getLanguage(String localeID) {
        return new IDParser(localeID).getLanguage();
    }
     
    /**
     * Returns the script code for this locale, which might be the empty string.
     * @see #getDisplayScript
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getScript() {
        return getScript(localeID);
    }

    /**
     * Returns the script code for the specified locale, which might be the empty string.
     * @see #getDisplayScript
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getScript(String localeID) {
        return new IDParser(localeID).getScript();
    }
    
    /**
     * Returns the country/region code for this locale, which will either be the empty string
     * or an uppercase ISO 3166 2-letter code.
     * @see #getDisplayCountry
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getCountry() {
        return getCountry(localeID);
    }

    /**
     * Returns the country/region code for this locale, which will either be the empty string
     * or an uppercase ISO 3166 2-letter code.
     * @param localeID
     * @see #getDisplayCountry
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getCountry(String localeID) {
        return new IDParser(localeID).getCountry();
    }
    
    /**
     * Returns the variant code for this locale, which might be the empty string.
     * @see #getDisplayVariant
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getVariant() {
        return getVariant(localeID);
    }

    /**
     * Returns the variant code for the specified locale, which might be the empty string.
     * @see #getDisplayVariant
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getVariant(String localeID) {
        return new IDParser(localeID).getVariant();
    }

    /**
     * Returns the fallback locale for the specified locale, which might be the empty string.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getFallback(String localeID) {
        return getFallbackString(getName(localeID));
    }

    /**
     * Returns the fallback locale for this locale.  If this locale is root, returns null.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public ULocale getFallback() {
        if (localeID.length() == 0 || localeID.charAt(0) == '@') {
            return null;
        }
        return new ULocale(getFallbackString(localeID), null);
    }

    /**
     * Return the given (canonical) locale id minus the last part before the tags.
     */
    private static String getFallbackString(String fallback) {
        int limit = fallback.indexOf('@');
        if (limit == -1) {
            limit = fallback.length();
        }
        int start = fallback.lastIndexOf('_', limit);
        if (start == -1) {
            start = 0;
        }
        return fallback.substring(0, start) + fallback.substring(limit);
    }

    /**
     * Returns the (normalized) base name for this locale.
     * @return the base name as a String.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getBaseName() {
        return getBaseName(localeID);
    }
    
    /**
     * Returns the (normalized) base name for the specified locale.
     * @param localeID the locale ID as a string
     * @return the base name as a String.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getBaseName(String localeID){
        return new IDParser(localeID).getBaseName();
    }

    /**
     * Returns the (normalized) full name for this locale.
     *
     * @return String the full name of the localeID
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */ 
    public String getName() {
        return localeID; // always normalized
    }

    /**
     * Returns the (normalized) full name for the specified locale.
     *
     * @param localeID the localeID as a string
     * @return String the full name of the localeID
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getName(String localeID){
        return new IDParser(localeID).getName();
    }

    /**
     * Returns a string representation of this object.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String toString() {
        return localeID;
    }

    /**
     * Returns an iterator over keywords for this locale.  If there 
     * are no keywords, returns null.
     * @return iterator over keywords, or null if there are no keywords.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Iterator getKeywords() {
        return getKeywords(localeID);
    }

    /**
     * Returns an iterator over keywords for the specified locale.  If there 
     * are no keywords, returns null.
     * @return an iterator over the keywords in the specified locale, or null
     * if there are no keywords.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static Iterator getKeywords(String localeID){
        return new IDParser(localeID).getKeywords();
    }

    /**
     * Returns the value for a keyword in this locale. If the keyword is not defined, returns null.
     * @param keywordName name of the keyword whose value is desired. Case insensitive.
     * @return the value of the keyword, or null.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getKeywordValue(String keywordName){
        return getKeywordValue(localeID, keywordName);
    }
    
    /**
     * Returns the value for a keyword in the specified locale. If the keyword is not defined, returns null. 
     * The locale name does not need to be normalized.
     * @param keywordName name of the keyword whose value is desired. Case insensitive.
     * @return String the value of the keyword as a string
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getKeywordValue(String localeID, String keywordName) {
        return new IDParser(localeID).getKeywordValue(keywordName);
    }

    /**
     * Utility class to parse and normalize locale ids (including POSIX style)
     */
    private static final class IDParser {
        private char[] id;
        private int index;
        private char[] buffer;
        private int blen;
        // um, don't handle POSIX ids unless we request it.  why not?  well... because.
        private boolean canonicalize;
        private boolean hadCountry;

        // used when canonicalizing
        Map keywords;
        String baseName;

        /**
         * Parsing constants.
         */
        private static final char KEYWORD_SEPARATOR     = '@';
        private static final char HYPHEN                = '-';
        private static final char KEYWORD_ASSIGN        = '=';
        private static final char COMMA                 = ',';
        private static final char ITEM_SEPARATOR        = ';';
        private static final char DOT                   = '.';

        private IDParser(String localeID) {
            this(localeID, false);
        }

        private IDParser(String localeID, boolean canonicalize) {
            id = localeID.toCharArray();
            index = 0;
            buffer = new char[id.length + 5];
            blen = 0;
            this.canonicalize = canonicalize;
        }

        private void reset() {
            index = blen = 0;
        }

        // utilities for working on text in the buffer

        /**
         * Append c to the buffer.
         */
        private void append(char c) {
            try {
                buffer[blen] = c;
            }
            catch (IndexOutOfBoundsException e) {
                if (buffer.length > 512) {
                    // something is seriously wrong, let this go
                    throw e;
                }
                char[] nbuffer = new char[buffer.length * 2];
                System.arraycopy(buffer, 0, nbuffer, 0, buffer.length);
                nbuffer[blen] = c;
                buffer = nbuffer;
            }
            ++blen;
        }

        private void addSeparator() {
            append(UNDERSCORE);
        }

        /**
         * Returns the text in the buffer from start to blen as a String.
         */
        private String getString(int start) {
            if (start == blen) {
                return EMPTY_STRING;
            }
            return new String(buffer, start, blen-start);
        }

        /**
         * Set the length of the buffer to pos, then append the string.
         */
        private void set(int pos, String s) {
            this.blen = pos; // no safety
            append(s);
        }

        /**
         * Append the string to the buffer.
         */
        private void append(String s) {
            for (int i = 0; i < s.length(); ++i) {
                append(s.charAt(i));
            }
        }

        // utilities for parsing text out of the id

        /**
         * Character to indicate no more text is available in the id.
         */
        private static final char DONE = '\uffff';

        /**
         * Returns the character at index in the id, and advance index.  The returned character
         * is DONE if index was at the limit of the buffer.  The index is advanced regardless
         * so that decrementing the index will always 'unget' the last character returned.
         */
        private char next() {
            if (index == id.length) {
                index++;
                return DONE; 
            }

            return id[index++];
        }

        /**
         * Advance index until the next terminator or id separator, and leave it there.
         */
        private void skipUntilTerminatorOrIDSeparator() {
            while (!isTerminatorOrIDSeparator(next()));
            --index;
        }

        /**
         * Returns true if the character at index in the id is a terminator.
         */
        private boolean atTerminator() {
            return index >= id.length || isTerminator(id[index]);
        }

        /**
         * Returns true if the character is an id separator (underscore or hyphen).
         */
        private boolean isIDSeparator(char c) {
            return c == UNDERSCORE || c == HYPHEN;
        }

        /**
         * Returns true if the character is a terminator (keyword separator, dot, or DONE).
         * Dot is a terminator because of the POSIX form, where dot precedes the codepage.
         */
        private boolean isTerminator(char c) {
            // always terminate at DOT, even if not handling POSIX.  It's an error...
            return c == KEYWORD_SEPARATOR || c == DONE || c == DOT;
        }

        /**
         * Returns true if the character is a terminator or id separator.
         */
        private boolean isTerminatorOrIDSeparator(char c) {
            return c == KEYWORD_SEPARATOR || c == UNDERSCORE || c == HYPHEN || 
                c == DONE || c == DOT;   
        }

        /**
         * Returns true if the start of the buffer has an experimental or private language 
         * prefix, the pattern '[ixIX][-_].' shows the syntax checked.
         */
        private boolean haveExperimentalLanguagePrefix() {
            if (id.length > 2) {
                char c = id[1];
                if (c == HYPHEN || c == UNDERSCORE) {
                    c = id[0];
                    return c == 'x' || c == 'X' || c == 'i' || c == 'I';
                }
            }
            return false;
        }

        /**
         * Returns true if a value separator occurs at or after index.
         */
        private boolean haveKeywordAssign() {
            // assume it is safe to start from index
            for (int i = index; i < id.length; ++i) {
                if (id[i] == KEYWORD_ASSIGN) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Advance index past language, and accumulate normalized language code in buffer.
         * Index must be at 0 when this is called.  Index is left at a terminator or id 
         * separator.  Returns the start of the language code in the buffer.
         */
        private int parseLanguage() {
            if (haveExperimentalLanguagePrefix()) {
                append(Character.toLowerCase(id[0]));
                append(HYPHEN);
                index = 2;
            }
        
            char c;
            while(!isTerminatorOrIDSeparator(c = next())) {
                append(Character.toLowerCase(c));
            }
            --index; // unget

            if (blen == 3) {
                initLanguageTables();

                /* convert 3 character code to 2 character code if possible *CWB*/
                String lang = getString(0);
                int offset = findIndex(_languages3, lang);
                if (offset >= 0) {
                    set(0, _languages[offset]);
                } else {
                    offset = findIndex(_obsoleteLanguages3, lang);
                    if (offset >= 0) {
                        set(0, _obsoleteLanguages[offset]);
                    }
                }
            }

            return 0;
        }

        /**
         * Advance index past language.  Index must be at 0 when this is called.  Index
         * is left at a terminator or id separator.
         */
        private void skipLanguage() {
            if (haveExperimentalLanguagePrefix()) {
                index = 2;
            }
            skipUntilTerminatorOrIDSeparator();
        }

        /**
         * Advance index past script, and accumulate normalized script in buffer.
         * Index must be immediately after the language.
         * If the item at this position is not a script (is not four characters
         * long) leave index and buffer unchanged.  Otherwise index is left at
         * a terminator or id separator.  Returns the start of the script code
         * in the buffer (this may be equal to the buffer length, if there is no
         * script).
         */
        private int parseScript() {
            if (!atTerminator()) {
                int oldIndex = index; // save original index
                ++index;

                int oldBlen = blen; // get before append hyphen, if we truncate everything is undone
                char c;
                while(!isTerminatorOrIDSeparator(c = next())) {
                    if (blen == oldBlen) { // first pass
                        addSeparator();
                        append(Character.toUpperCase(c));
                    } else {
                        append(Character.toLowerCase(c));
                    }
                }
                --index; // unget

                /* If it's not exactly 4 characters long, then it's not a script. */
                if (index - oldIndex != 5) { // +1 to account for separator
                    index = oldIndex;
                    blen = oldBlen;
                } else {
                    oldBlen++; // index past hyphen, for clients who want to extract just the script
                }

                return oldBlen;
            }
            return blen;
        }

        /**
         * Advance index past script.
         * Index must be immediately after the language and IDSeparator.
         * If the item at this position is not a script (is not four characters
         * long) leave index.  Otherwise index is left at a terminator or
         * id separator.
         */
        private void skipScript() {
            if (!atTerminator()) {
                int oldIndex = index;
                ++index;

                skipUntilTerminatorOrIDSeparator();
                if (index - oldIndex != 5) { // +1 to account for separator
                    index = oldIndex;
                }
            }
        }

        /**
         * Advance index past country, and accumulate normalized country in buffer.
         * Index must be immediately after the script (if there is one, else language)
         * and IDSeparator.  Return the start of the country code in the buffer.
         */
        private int parseCountry() {
            if (!atTerminator()) {
                ++index;

                int oldBlen = blen;
                char c;
                while (!isTerminatorOrIDSeparator(c = next())) {
                    if (oldBlen == blen) { // first, add hyphen
                        hadCountry = true; // we have a country, let variant parsing know
                        addSeparator();
                        ++oldBlen; // increment past hyphen
                    }
                    append(Character.toUpperCase(c));
                }
                --index; // unget

                if (blen - oldBlen == 3) {
                    initCountryTables();

                    /* convert 3 character code to 2 character code if possible *CWB*/
                    int offset = findIndex(_countries3, getString(oldBlen));
                    if (offset >= 0) {
                        set(oldBlen, _countries[offset]);
                    } else {
                        offset = findIndex(_obsoleteCountries3, getString(oldBlen));
                        if (offset >= 0) {
                            set(oldBlen, _obsoleteCountries[offset]);
                        }
                    }
                }

                return oldBlen;
            }

            return blen;
        }

        /**
         * Advance index past country.
         * Index must be immediately after the script (if there is one, else language)
         * and IDSeparator.
         */
        private void skipCountry() {
            if (!atTerminator()) {
                ++index;
                skipUntilTerminatorOrIDSeparator();
            }
        }

        /**
         * Advance index past variant, and accumulate normalized variant in buffer.  This ignores
         * the codepage information from POSIX ids.  Index must be immediately after the country
         * or script.  Index is left at the keyword separator or at the end of the text.  Return
         * the start of the variant code in the buffer.
         *
         * In standard form, we can have the following forms:
         * ll__VVVV
         * ll_CC_VVVV
         * ll_Ssss_VVVV
         * ll_Ssss_CC_VVVV
         *
         * This also handles POSIX ids, which can have the following forms (pppp is code page id):
         * ll_CC.pppp          --> ll_CC
         * ll_CC.pppp@VVVV     --> ll_CC_VVVV
         * ll_CC@VVVV          --> ll_CC_VVVV
         *
         * We identify this use of '@' in POSIX ids by looking for an '=' following
         * the '@'.  If there is one, we consider '@' to start a keyword list, instead of
         * being part of a POSIX id.
         *
         * Note:  since it was decided that we want an option to not handle POSIX ids, this
         * becomes a bit more complex.
         */
        private int parseVariant() {
            int oldBlen = blen;

            boolean start = true;
            boolean needSeparator = true;
            boolean skipping = false;
            char c;
            while ((c = next()) != DONE) {
                if (c == DOT) {
                    start = false;
                    skipping = true;
                } else if (c == KEYWORD_SEPARATOR) {
                    if (haveKeywordAssign()) {
                        break;
                    }
                    skipping = false;
                    start = false;
                    needSeparator = true; // add another underscore if we have more text
                } else if (start) {
                    start = false;
                } else if (!skipping) {
                    if (needSeparator) {
                        boolean incOldBlen = blen == oldBlen; // need to skip separators
                        needSeparator = false;
                        if (incOldBlen && !hadCountry) { // no country, we'll need two
                            addSeparator();
                            ++oldBlen; // for sure
                        }
                        addSeparator();
                        if (incOldBlen) { // only for the first separator
                            ++oldBlen;
                        }
                    }
                    c = Character.toUpperCase(c);
                    if (c == HYPHEN || c == COMMA) {
                        c = UNDERSCORE;
                    }
                    append(c);
                }
            }
            --index; // unget
            
            return oldBlen;
        }

        // no need for skipvariant, to get the keywords we'll just scan directly for 
        // the keyword separator

        /**
         * Returns the normalized language id, or the empty string.
         */
        public String getLanguage() {
            reset();
            return getString(parseLanguage());
        }
   
        /**
         * Returns the normalized script id, or the empty string.
         */
        public String getScript() {
            reset();
            skipLanguage();
            return getString(parseScript());
        }
    
        /**
         * return the normalized country id, or the empty string.
         */
        public String getCountry() {
            reset();
            skipLanguage();
            skipScript();
            return getString(parseCountry());
        }

        /**
         * Returns the normalized variant id, or the empty string.
         */
        public String getVariant() {
            reset();
            skipLanguage();
            skipScript();
            skipCountry();
            return getString(parseVariant());
        }

        /**
         * Returns the language, script, country, and variant as separate strings.
         */
        public String[] getLanguageScriptCountryVariant() {
            reset();
            return new String[] {
                getString(parseLanguage()),
                getString(parseScript()),
                getString(parseCountry()),
                getString(parseVariant())
            };
        }

        public void setBaseName(String baseName) {
            this.baseName = baseName;
        }

        public void parseBaseName() {
            if (baseName != null) {
                set(0, baseName);
            } else {
                reset();
                parseLanguage();
                parseScript();
                parseCountry();
                parseVariant();
            
                // catch unwanted trailing underscore after country if there was no variant
                if (blen > 1 && buffer[blen-1] == UNDERSCORE) {
                    --blen;
                }
            }
        }

        /**
         * Returns the normalized base form of the locale id.  The base
         * form does not include keywords.
         */
        public String getBaseName() {
            if (baseName != null) {
                return baseName;
            }
            parseBaseName();
            return getString(0);
        }

        /**
         * Returns the normalized full form of the locale id.  The full
         * form includes keywords if they are present.
         */
        public String getName() {
            parseBaseName();
            parseKeywords();
            return getString(0);
        }

        // keyword utilities

        /**
         * If we have keywords, advance index to the start of the keywords and return true, 
         * otherwise return false.
         */
        private boolean setToKeywordStart() {
            for (int i = index; i < id.length; ++i) {
                if (id[i] == KEYWORD_SEPARATOR) {
                    if (canonicalize) {
                        for (int j = ++i; j < id.length; ++j) { // increment i past separator for return
                            if (id[j] == KEYWORD_ASSIGN) {
                                index = i;
                                return true;
                            }
                        }
                    } else {
                        if (++i < id.length) {
                            index = i;
                            return true;
                        }
                    }
                    break;
                }
            }
            return false;
        }
        
        private static boolean isDoneOrKeywordAssign(char c) {
            return c == DONE || c == KEYWORD_ASSIGN;
        }

        private static boolean isDoneOrItemSeparator(char c) {
            return c == DONE || c == ITEM_SEPARATOR;
        }

        private String getKeyword() {
            int start = index;
            while (!isDoneOrKeywordAssign(next()));
            --index;
            return new String(id, start, index-start).trim().toLowerCase();
        }

        private String getValue() {
            int start = index;
            while (!isDoneOrItemSeparator(next()));
            --index;
            return new String(id, start, index-start).trim(); // leave case alone
        }

        private Comparator getKeyComparator() {
            final Comparator comp = new Comparator() {
                    public int compare(Object lhs, Object rhs) {
                        return ((String)lhs).compareTo(rhs);
                    }
                };
            return comp;
        }

        /**
         * Returns a map of the keywords and values, or null if there are none.
         */
        private Map getKeywordMap() {
            if (keywords == null) {
                TreeMap m = null;
                if (setToKeywordStart()) {
                    // trim spaces and convert to lower case, both keywords and values.
                    do {
                        String key = getKeyword();
                        if (key.length() == 0) {
                            break;
                        }
                        char c = next();
                        if (c != KEYWORD_ASSIGN) {
                            // throw new IllegalArgumentException("key '" + key + "' missing a value.");
                            if (c == DONE) {
                                break;
                            } else {
                                continue;
                            }
                        }
                        String value = getValue();
                        if (value.length() == 0) {
                            // throw new IllegalArgumentException("key '" + key + "' missing a value.");
                            continue;
                        }
                        if (m == null) {
                            m = new TreeMap(getKeyComparator());
                        } else if (m.containsKey(key)) {
                            // throw new IllegalArgumentException("key '" + key + "' already has a value.");
                            continue;
                        }
                        m.put(key, value);
                    } while (next() == ITEM_SEPARATOR);
                }               
                keywords = m != null ? m : Collections.EMPTY_MAP;
            }

            return keywords;
        }

        /**
         * Parse the keywords and return start of the string in the buffer.
         */
        private int parseKeywords() {
            int oldBlen = blen;
            Map m = getKeywordMap();
            if (!m.isEmpty()) {
                Iterator iter = m.entrySet().iterator();
                boolean first = true;
                while (iter.hasNext()) {
                    append(first ? KEYWORD_SEPARATOR : ITEM_SEPARATOR);
                    first = false;
                    Map.Entry e = (Map.Entry)iter.next();
                    append((String)e.getKey());
                    append(KEYWORD_ASSIGN);
                    append((String)e.getValue());
                }
                if (blen != oldBlen) {
                    ++oldBlen;
                }
            }
            return oldBlen;
        }

        /**
         * Returns an iterator over the keywords, or null if we have an empty map.
         */
        public Iterator getKeywords() {
            Map m = getKeywordMap();
            return m.isEmpty() ? null : m.keySet().iterator();
        }

        /**
         * Returns the value for the named keyword, or null if the keyword is not
         * present.
         */
        public String getKeywordValue(String keywordName) {
            Map m = getKeywordMap();
            return m.isEmpty() ? null : (String)m.get(keywordName.trim().toLowerCase());
        }

        /**
         * Set the keyword value only if it is not already set to something else.
         */
        public void defaultKeywordValue(String keywordName, String value) {
            setKeywordValue(keywordName, value, false);
        }
            
        /**
         * Set the value for the named keyword, or unset it if value is null.  If
         * keywordName itself is null, unset all keywords.  If keywordName is not null,
         * value must not be null.
         */
        public void setKeywordValue(String keywordName, String value) {
            setKeywordValue(keywordName, value, true);
        }

        /**
         * Set the value for the named keyword, or unset it if value is null.  If
         * keywordName itself is null, unset all keywords.  If keywordName is not null,
         * value must not be null.  If reset is true, ignore any previous value for 
         * the keyword, otherwise do not change the keyword (including removal of
         * one or all keywords).
         */
        private void setKeywordValue(String keywordName, String value, boolean reset) {
            if (keywordName == null) {
                if (reset) {
                    // force new map, ignore value
                    keywords = Collections.EMPTY_MAP;
                }
            } else {
                keywordName = keywordName.trim().toLowerCase();
                if (keywordName.length() == 0) {
                    throw new IllegalArgumentException("keyword must not be empty");
                }
                if (value != null) {
                    value = value.trim();
                    if (value.length() == 0) {
                        throw new IllegalArgumentException("value must not be empty");
                    }
                }
                Map m = getKeywordMap();
                if (m.isEmpty()) { // it is EMPTY_MAP
                    if (value != null) {
                        // force new map
                        keywords = new TreeMap(getKeyComparator());
                        keywords.put(keywordName, value.trim());
                    }
                } else {
                    if (reset || !m.containsKey(keywordName)) {
                        if (value != null) {
                            m.put(keywordName, value);
                        } else {
                            m.remove(keywordName);
                            if (m.isEmpty()) {
                                // force new map
                                keywords = Collections.EMPTY_MAP;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * linear search of the string array. the arrays are unfortunately ordered by the
     * two-letter target code, not the three-letter search code, which seems backwards.
     */
    private static int findIndex(String[] array, String target){
        for (int i = 0; i < array.length; i++) {
            if (target.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }    

    /**
     * Returns the canonical name for the specified locale ID.  This is used to convert POSIX
     * and other grandfathered IDs to standard ICU form.
     * @param localeID the locale id
     * @return the canonicalized id
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String canonicalize(String localeID){
        IDParser parser = new IDParser(localeID, true);
        String baseName = parser.getBaseName();
        boolean foundVariant = false;
      
        // formerly, we always set to en_US_POSIX if the basename was empty, but
        // now we require that the entire id be empty, so that "@foo=bar"
        // will pass through unchanged.
        // {dlf} I'd rather keep "" unchanged.
        if (localeID.equals("")) {
            return "";
//              return "en_US_POSIX";
        }

        // we have an ID in the form xx_Yyyy_ZZ_KKKKK

        initVariantsTable();

        /* See if this is an already known locale */
        for (int i = 0; i < _variantsToKeywords.length; i++) {
            if (_variantsToKeywords[i][0].equals(baseName)) {
                foundVariant = true;

                String[] vals = _variantsToKeywords[i];
                parser.setBaseName(vals[1]);
                if (vals[2] != null) {
                    parser.defaultKeywordValue(vals[2], vals[3]);
                }
                break;
            }
        }

        /* convert the Euro variant to appropriate ID */
        if (!foundVariant) {
            int idx = baseName.indexOf("_EURO");
            if (idx > -1) {
                parser.setBaseName(baseName.substring(0, idx));
                parser.defaultKeywordValue("currency", "EUR");
            }
        }

        /* total mondo hack for Norwegian, fortunately the main NY case is handled earlier */
        if (!foundVariant) {
            if (parser.getLanguage().equals("nb") && parser.getVariant().equals("NY")) {
                parser.setBaseName(lscvToID("nn", parser.getScript(), parser.getCountry(), null));
            }
        }

        return parser.getName();
    }
    
    /**
     * Given a keyword and a value, return a new locale with an updated
     * keyword and value.  If keyword is null, this removes all keywords from the locale id.
     * Otherwise, if the value is null, this removes the value for this keyword from the
     * locale id.  Otherwise, this adds/replaces the value for this keyword in the locale id.
     * The keyword and value must not be empty.
     * @param keyword the keyword to add/remove, or null to remove all keywords.
     * @param value the value to add/set, or null to remove this particular keyword.
     * @return the updated locale
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public ULocale setKeywordValue(String keyword, String value) {
        return new ULocale(setKeywordValue(localeID, keyword, value), null);
    }

    /**
     * Given a locale id, a keyword, and a value, return a new locale id with an updated
     * keyword and value.  If keyword is null, this removes all keywords from the locale id.
     * Otherwise, if the value is null, this removes the value for this keyword from the
     * locale id.  Otherwise, this adds/replaces the value for this keyword in the locale id.
     * The keyword and value must not be empty.
     * @param localeID the locale id to modify
     * @param keyword the keyword to add/remove, or null to remove all keywords.
     * @param value the value to add/set, or null to remove this particular keyword.
     * @return the updated locale id
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String setKeywordValue(String localeID, String keyword, String value) {
        IDParser parser = new IDParser(localeID);
        parser.setKeywordValue(keyword, value);
        return parser.getName();
    }

    /**
     * Given a locale id, a keyword, and a value, return a new locale id with an updated
     * keyword and value, if the keyword does not already have a value.  The keyword and
     * value must not be null or empty.
     * @param localeID the locale id to modify
     * @param keyword the keyword to add, if not already present
     * @param the value to add, if not already present
     * @return the updated locale id
     * @internal
     */
    private static String defaultKeywordValue(String localeID, String keyword, String value) {
        IDParser parser = new IDParser(localeID);
        parser.defaultKeywordValue(keyword, value);
        return parser.getName();
    }

    /**
     * Returns a three-letter abbreviation for this locale's language.  If the locale
     * doesn't specify a language, returns the empty string.  Otherwise, returns
     * a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getISO3Language(){
        return getISO3Language(localeID);
    }

    /**
     * Returns a three-letter abbreviation for this locale's language.  If the locale
     * doesn't specify a language, returns the empty string.  Otherwise, returns
     * a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getISO3Language(String localeID){
        initLanguageTables();

        String language = getLanguage(localeID);
        int offset = findIndex(_languages, language);
        if(offset>=0){
            return _languages3[offset];
        } else {
            offset = findIndex(_obsoleteLanguages, language);
            if (offset >= 0) {
                return _obsoleteLanguages3[offset];
            }
        }
        return EMPTY_STRING;
    }
    
    /**
     * Returns a three-letter abbreviation for this locale's country/region.  If the locale
     * doesn't specify a country, returns the empty string.  Otherwise, returns
     * an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getISO3Country(){
        return getISO3Country(localeID);
    }
    /**
     * Returns a three-letter abbreviation for this locale's country/region.  If the locale
     * doesn't specify a country, returns the empty string.  Otherwise, returns
     * an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getISO3Country(String localeID){
        initCountryTables();

        String country = getCountry(localeID);
        int offset = findIndex(_countries, country);
        if(offset>=0){
            return _countries3[offset];
        }else{
            offset = findIndex(_obsoleteCountries, country);
            if(offset>=0){
                return _obsoleteCountries3[offset];   
            }
        }
        return EMPTY_STRING;
    }
    
    // display names

    /**
     * Utility to fetch locale display data from resource bundle tables.
     */
    private static String getTableString(String tableName, String subtableName, String item, String displayLocaleID) {
        if (item.length() > 0) {
            try {
                ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.
                  getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, displayLocaleID);
                return getTableString(tableName, subtableName, item, bundle);
            } catch (Exception e) {
//              System.out.println("gtsu: " + e.getMessage());
            }
        }
        return item;
    }
        
    /**
     * Utility to fetch locale display data from resource bundle tables.
     */
    private static String getTableString(String tableName, String subtableName, String item, ICUResourceBundle bundle) {
//      System.out.println("gts table: " + tableName + 
//                         " subtable: " + subtableName +
//                         " item: " + item +
//                         " bundle: " + bundle.getULocale());
        try {
            for (;;) {
                // special case currency
                if ("currency".equals(subtableName)) {
                    ICUResourceBundle table = bundle.getWithFallback("Currencies");
                    table = table.getWithFallback(item);
                    return table.getString(1);
                } else {
                    ICUResourceBundle table = bundle.getWithFallback(tableName);
                    try {
                        if (subtableName != null) {
                            table = table.getWithFallback(subtableName);
                        }
                        return table.getStringWithFallback(item);
                    }
                    catch (MissingResourceException e) {
                        String fallbackLocale = table.getWithFallback("Fallback").getString();
                        if (fallbackLocale.length() == 0) {
                            fallbackLocale = "root";
                        }
//                      System.out.println("bundle: " + bundle.getULocale() + " fallback: " + fallbackLocale);
                        if(fallbackLocale.equals(table.getULocale().localeID)){
                            return item;
                        }
                        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, 
                                                                                      fallbackLocale);
//                          System.out.println("fallback from " + table.getULocale() + " to " + fallbackLocale + 
//                                             ", got bundle " + bundle.getULocale());
                    }
                }
            }
        }
        catch (Exception e) {
//          System.out.println("gtsi: " + e.getMessage());
        }
        return item;
    }

    /**
     * Returns this locale's language localized for display in the default locale.
     * @return the localized language name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayLanguage() {
        return getDisplayLanguageInternal(localeID, getDefault().localeID);
    }

    /**
     * Returns this locale's language localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized language name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayLanguage(ULocale displayLocale) {
        return getDisplayLanguageInternal(localeID, displayLocale.localeID);
    }
    
    /**
     * Returns a locale's language localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose language will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized language name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayLanguage(String localeID, String displayLocaleID) {
        return getDisplayLanguageInternal(localeID, getName(displayLocaleID));
    }

    /**
     * Returns a locale's language localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose language will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized language name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayLanguage(String localeID, ULocale displayLocale) {
        return getDisplayLanguageInternal(localeID, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayLanguageInternal(String localeID, String displayLocaleID) {
        return getTableString("Languages", null, new IDParser(localeID).getLanguage(), displayLocaleID);
    }
    
    /**
     * Returns this locale's script localized for display in the default locale.
     * @return the localized script name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayScript() {
        return getDisplayScriptInternal(localeID, getDefault().localeID);
    }

    /**
     * Returns this locale's script localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized script name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayScript(ULocale displayLocale) {
        return getDisplayScriptInternal(localeID, displayLocale.localeID);
    }
    
    /**
     * Returns a locale's script localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose script will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized script name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayScript(String localeID, String displayLocaleID) {
        return getDisplayScriptInternal(localeID, getName(displayLocaleID));
    }

    /**
     * Returns a locale's script localized for display in the provided locale.
     * @param localeID the id of the locale whose script will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized script name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayScript(String localeID, ULocale displayLocale) {
        return getDisplayScriptInternal(localeID, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayScriptInternal(String localeID, String displayLocaleID) {
        return getTableString("Scripts", null, new IDParser(localeID).getScript(), displayLocaleID);
    }

    /**
     * Returns this locale's country localized for display in the default locale.
     * @return the localized country name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayCountry() {
        return getDisplayCountryInternal(localeID, getDefault().localeID);
    }
    
    /**
     * Returns this locale's country localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized country name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayCountry(ULocale displayLocale){
        return getDisplayCountryInternal(localeID, displayLocale.localeID);   
    }
    
    /**
     * Returns a locale's country localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose country will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized country name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayCountry(String localeID, String displayLocaleID) {
        return getDisplayCountryInternal(localeID, getName(displayLocaleID));
    }

    /**
     * Returns a locale's country localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose country will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized country name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayCountry(String localeID, ULocale displayLocale) {
        return getDisplayCountryInternal(localeID, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayCountryInternal(String localeID, String displayLocaleID) {
        return getTableString("Countries", null, new IDParser(localeID).getCountry(), displayLocaleID);
    }
    
    /**
     * Returns this locale's variant localized for display in the default locale.
     * @return the localized variant name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayVariant() {
        return getDisplayVariantInternal(localeID, getDefault().localeID);   
    }

    /**
     * Returns this locale's variant localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized variant name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayVariant(ULocale displayLocale) {
        return getDisplayVariantInternal(localeID, displayLocale.localeID);   
    }
    
    /**
     * Returns a locale's variant localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose variant will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized variant name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayVariant(String localeID, String displayLocaleID){
        return getDisplayVariantInternal(localeID, getName(displayLocaleID));
    }
    
    /**
     * Returns a locale's variant localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose variant will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized variant name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayVariant(String localeID, ULocale displayLocale) {
        return getDisplayVariantInternal(localeID, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayVariantInternal(String localeID, String displayLocaleID) {
        return getTableString("Variants", null, new IDParser(localeID).getVariant(), displayLocaleID);
    }

    /**
     * Returns a keyword localized for display in the default locale.
     * @param keyword the keyword to be displayed.
     * @return the localized keyword name.
     * @see #getKeywords
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayKeyword(String keyword) {
        return getDisplayKeywordInternal(keyword, getDefault().localeID);   
    }
    
    /**
     * Returns a keyword localized for display in the specified locale.
     * @param keyword the keyword to be displayed.
     * @param displayLocaleID the id of the locale in which to display the keyword.
     * @return the localized keyword name.
     * @see #getKeywords
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayKeyword(String keyword, String displayLocaleID) {
        return getDisplayKeywordInternal(keyword, getName(displayLocaleID));   
    }

    /**
     * Returns a keyword localized for display in the specified locale.
     * @param keyword the keyword to be displayed.
     * @param displayLocale the locale in which to display the keyword.
     * @return the localized keyword name.
     * @see #getKeywords
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayKeyword(String keyword, ULocale displayLocale) {
        return getDisplayKeywordInternal(keyword, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayKeywordInternal(String keyword, String displayLocaleID) {
        return getTableString("Keys", null, keyword.trim().toLowerCase(), displayLocaleID);
    }

    /**
     * Returns a keyword value localized for display in the default locale.
     * @param keyword the keyword whose value is to be displayed.
     * @return the localized value name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayKeywordValue(String keyword) {
        return getDisplayKeywordValueInternal(localeID, keyword, getDefault().localeID);
    }
    
    /**
     * Returns a keyword value localized for display in the specified locale.
     * @param keyword the keyword whose value is to be displayed.
     * @param displayLocale the locale in which to display the value.
     * @return the localized value name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayKeywordValue(String keyword, ULocale displayLocale) {
        return getDisplayKeywordValueInternal(localeID, keyword, displayLocale.localeID);   
    }

    /**
     * Returns a keyword value localized for display in the specified locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose keyword value is to be displayed.
     * @param keyword the keyword whose value is to be displayed.
     * @param displayLocaleID the id of the locale in which to display the value.
     * @return the localized value name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayKeywordValue(String localeID, String keyword, String displayLocaleID) {
        return getDisplayKeywordValueInternal(localeID, keyword, getName(displayLocaleID));
    }

    /**
     * Returns a keyword value localized for display in the specified locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose keyword value is to be displayed.
     * @param keyword the keyword whose value is to be displayed.
     * @param displayLocale the id of the locale in which to display the value.
     * @return the localized value name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayKeywordValue(String localeID, String keyword, ULocale displayLocale) {
        return getDisplayKeywordValueInternal(localeID, keyword, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayKeywordValueInternal(String localeID, String keyword, String displayLocaleID) {
        keyword = keyword.trim().toLowerCase();
        String value = new IDParser(localeID).getKeywordValue(keyword);
        return getTableString("Types", keyword, value, displayLocaleID);
    }
    
    /**
     * Returns this locale name localized for display in the default locale.
     * @return the localized locale name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayName() {
        return getDisplayNameInternal(localeID, getDefault().localeID);
    }
    
    /**
     * Returns this locale name localized for display in the provided locale.
     * @param displayLocale the locale in which to display the locale name.
     * @return the localized locale name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayName(ULocale displayLocale) {
        return getDisplayNameInternal(localeID, displayLocale.localeID);
    }
    
    /**
     * Returns the locale ID localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the locale whose name is to be displayed.
     * @param displayLocaleID the id of the locale in which to display the locale name.
     * @return the localized locale name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayName(String localeID, String displayLocaleID) {
        return getDisplayNameInternal(localeID, getName(displayLocaleID));
    }

    /**
     * Returns the locale ID localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the locale whose name is to be displayed.
     * @param displayLocale the locale in which to display the locale name.
     * @return the localized locale name.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static String getDisplayName(String localeID, ULocale displayLocale) {
        return getDisplayNameInternal(localeID, displayLocale.localeID);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayNameInternal(String localeID, String displayLocaleID) {
        // lang
        // lang (script, country, variant, keyword=value, ...)
        // script, country, variant, keyword=value, ...

        final String[] tableNames = { "Languages", "Scripts", "Countries", "Variants" };

        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, displayLocaleID);

        StringBuffer buf = new StringBuffer();

        IDParser parser = new IDParser(localeID);
        String[] names = parser.getLanguageScriptCountryVariant();

        boolean haveLanguage = names[0].length() > 0;
        boolean openParen = false;
        for (int i = 0; i < names.length; ++i) {
            String name = names[i];
            if (name.length() > 0) {
                name = getTableString(tableNames[i], null, name, bundle);
                if (buf.length() > 0) { // need a separator
                    if (haveLanguage & !openParen) {
                        buf.append(" (");
                        openParen = true;
                    } else {
                        buf.append(", ");
                    }
                }
                buf.append(name);
            }
        }

        Map m = parser.getKeywordMap();
        if (!m.isEmpty()) {
            Iterator keys = m.entrySet().iterator();
            while (keys.hasNext()) {
                if (buf.length() > 0) {
                    if (haveLanguage & !openParen) {
                        buf.append(" (");
                        openParen = true;
                    } else {
                        buf.append(", ");
                    }
                }
                Map.Entry e = (Map.Entry)keys.next();
                String key = (String)e.getKey();
                String val = (String)e.getValue();
                buf.append(getTableString("Keys", null, key, bundle));
                buf.append("=");
                buf.append(getTableString("Types", key, val, bundle));
            }
        }

        if (openParen) {
            buf.append(")");
        }
            
        return buf.toString();
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
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @deprecated This is a draft API and might change in a future release of ICU.
     */ 
    public static Type VALID_LOCALE = new Type(1);
    
    /**
     * Opaque selector enum for <tt>getLocale()</tt>.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final class Type {
        private int localeType;
        private Type(int type) { localeType = type; }
    }
}
