/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.MissingResourceException;

import com.ibm.icu.util.ULocale;


/**
 * Utilities for mapping between old and new language, country, and other
 * locale ID related names.
 */
public class LocaleIDs {

    /**
     * Returns a list of all 2-letter country codes defined in ISO 3166.
     * Can be used to create Locales.
     * @stable ICU 3.0
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
     * @stable ICU 3.0
     */
    public static String[] getISOLanguages() {
        initLanguageTables();
        return (String[])_languages.clone();
    }

    /**
     * Returns a three-letter abbreviation for the provided country.  If the provided
     * country is empty, returns the empty string.  Otherwise, returns
     * an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @stable ICU 3.0
     */
    public static String getISO3Country(String country){
        initCountryTables();

        int offset = findIndex(_countries, country);
        if(offset>=0){
            return _countries3[offset];
        }else{
            offset = findIndex(_obsoleteCountries, country);
            if(offset>=0){
                return _obsoleteCountries3[offset];
            }
        }
        return "";
    }
    /**
     * Returns a three-letter abbreviation for the language.  If language is
     * empty, returns the empty string.  Otherwise, returns
     * a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @stable ICU 3.0
     */
    public static String getISO3Language(String language) {
        initLanguageTables();

        int offset = findIndex(_languages, language);
        if(offset>=0){
            return _languages3[offset];
        } else {
            offset = findIndex(_obsoleteLanguages, language);
            if (offset >= 0) {
                return _obsoleteLanguages3[offset];
            }
        }
        return "";
    }

    public static String threeToTwoLetterLanguage(String lang) {
        initLanguageTables();

        /* convert 3 character code to 2 character code if possible *CWB*/
        int offset = findIndex(_languages3, lang);
        if (offset >= 0) {
            return _languages[offset];
        }

        offset = findIndex(_obsoleteLanguages3, lang);
        if (offset >= 0) {
            return _obsoleteLanguages[offset];
        }

        return null;
    }

    public static String threeToTwoLetterRegion(String region) {
        initCountryTables();

        /* convert 3 character code to 2 character code if possible *CWB*/
        int offset = findIndex(_countries3, region);
        if (offset >= 0) {
            return _countries[offset];
        }

        offset = findIndex(_obsoleteCountries3, region);
        if (offset >= 0) {
            return _obsoleteCountries[offset];
        }

        return null;
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
                "ru",  "rup", "rw",  "sa",  "sad", "sah", "sai", "sal", "sam",
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
                /*"ru",  "rup", "rw",  "sa",  "sad", "sah", "sai", "sal", "sam",    */
                "rus", "rup", "kin", "san", "sad", "sah", "sai", "sal", "sam",
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
    private static String[] _deprecatedCountries;
    private static String[] _replacementCountries;
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
                "AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AX",  "AZ",
                "BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",
                "BJ",  "BL",  "BM",  "BN",  "BO",  "BR",  "BS",  "BT",  "BV",
                "BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",
                "CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",
                "CU",  "CV",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",
                "DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",
                "ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",
                "GA",  "GB",  "GD",  "GE",  "GF",  "GG",  "GH",  "GI",  "GL",
                "GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",
                "GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",
                "ID",  "IE",  "IL",  "IM",  "IN",  "IO",  "IQ",  "IR",  "IS",
                "IT",  "JE",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",
                "KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",
                "LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",
                "LV",  "LY",  "MA",  "MC",  "MD",  "ME",  "MF",  "MG",  "MH",  "MK",
                "ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",
                "MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",
                "NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",
                "NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",
                "PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",
                "PW",  "PY",  "QA",  "RE",  "RO",  "RS",  "RU",  "RW",  "SA",
                "SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",
                "SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "ST",  "SV",
                "SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",
                "TK",  "TL",  "TM",  "TN",  "TO",  "TR",  "TT",  "TV",
                "TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",
                "VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",
                "WS",  "YE",  "YT",  "ZA",  "ZM",  "ZW",
            };

            /* this table is used for 3 letter codes */
            String[] tempObsoleteCountries = {
                "FX",  "CS",  "RO",  "TP",  "YU",  "ZR",  /* obsolete country codes */
            };

            String[] tempDeprecatedCountries = {
               "BU", "CS", "DY", "FX", "HV", "NH", "RH", "TP", "YU", "ZR" /* deprecated country list */
            };
            String[] tempReplacementCountries = {
           /*  "BU", "CS", "DY", "FX", "HV", "NH", "RH", "TP", "YU", "ZR" */
               "MM", "RS", "BJ", "FR", "BF", "VU", "ZW", "TL", "RS", "CD",   /* replacement country codes */
            };

            /* This list MUST contain a three-letter code for every two-letter code in
               the above list, and they MUST be listed in the same order! */
            String[] tempCountries3 = {
                /*  "AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",  "AN",     */
                    "AND", "ARE", "AFG", "ATG", "AIA", "ALB", "ARM", "ANT",
                /*  "AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AX",  "AZ",     */
                    "AGO", "ATA", "ARG", "ASM", "AUT", "AUS", "ABW", "ALA", "AZE",
                /*  "BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",     */
                    "BIH", "BRB", "BGD", "BEL", "BFA", "BGR", "BHR", "BDI",
                /*  "BJ",  "BL",  "BM",  "BN",  "BO",  "BR",  "BS",  "BT",  "BV",     */
                    "BEN", "BLM", "BMU", "BRN", "BOL", "BRA", "BHS", "BTN", "BVT",
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
                /*  "GA",  "GB",  "GD",  "GE",  "GF",  "GG",  "GH",  "GI",  "GL",     */
                    "GAB", "GBR", "GRD", "GEO", "GUF", "GGY", "GHA", "GIB", "GRL",
                /*  "GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",     */
                    "GMB", "GIN", "GLP", "GNQ", "GRC", "SGS", "GTM", "GUM",
                /*  "GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",     */
                    "GNB", "GUY", "HKG", "HMD", "HND", "HRV", "HTI", "HUN",
                /*  "ID",  "IE",  "IL",  "IM",  "IN",  "IO",  "IQ",  "IR",  "IS" */
                    "IDN", "IRL", "ISR", "IMN", "IND", "IOT", "IRQ", "IRN", "ISL",
                /*  "IT",  "JE",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",     */
                    "ITA", "JEY", "JAM", "JOR", "JPN", "KEN", "KGZ", "KHM", "KIR",
                /*  "KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",     */
                    "COM", "KNA", "PRK", "KOR", "KWT", "CYM", "KAZ", "LAO",
                /*  "LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",     */
                    "LBN", "LCA", "LIE", "LKA", "LBR", "LSO", "LTU", "LUX",
                /*  "LV",  "LY",  "MA",  "MC",  "MD",  "ME",  "MF",  "MG",  "MH",  "MK",     */
                    "LVA", "LBY", "MAR", "MCO", "MDA", "MNE", "MAF", "MDG", "MHL", "MKD",
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
                /*  "PW",  "PY",  "QA",  "RE",  "RO",  "RS",  "RU",  "RW",  "SA",     */
                    "PLW", "PRY", "QAT", "REU", "ROU", "SRB", "RUS", "RWA", "SAU",
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
                /*  "WS",  "YE",  "YT",  "ZA",  "ZM",  "ZW"          */
                    "WSM", "YEM", "MYT", "ZAF", "ZMB", "ZWE",
            };

            String[] tempObsoleteCountries3 = {
                /*"FX",  "CS",  "RO",  "TP",  "YU",  "ZR",   */
                "FXX", "SCG", "ROM", "TMP", "YUG", "ZAR",
            };

            synchronized (ULocale.class) {
                if (_countries == null) {
                    _countries = tempCountries;
                    _deprecatedCountries = tempDeprecatedCountries;
                    _replacementCountries = tempReplacementCountries;
                    _obsoleteCountries = tempObsoleteCountries;
                    _countries3 = tempCountries3;
                    _obsoleteCountries3 = tempObsoleteCountries3;
                }
            }
        }
    }

    public static String getCurrentCountryID(String oldID){
        initCountryTables();
        int offset = findIndex(_deprecatedCountries, oldID);
        if (offset >= 0) {
            return _replacementCountries[offset];
        }
        return oldID;
    }

    public static String getCurrentLanguageID(String oldID){
        initLanguageTables();
        int offset = findIndex(_obsoleteLanguages, oldID);
        if (offset >= 0) {
            return _replacementLanguages[offset];
        }
        return oldID;
    }


}
