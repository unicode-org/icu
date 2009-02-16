/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.HashMap;
import java.util.Map;

public class LanguageCode {

    private static final Map/*<String,String>*/ THREE_TO_TWO;

    static {
        THREE_TO_TWO = new HashMap/*<String,String>*/();

        String[] alpha3to2 = {
            "aar", "aa",    // Afar
            "abk", "ab",    // Abkhazian
            "afr", "af",    // Afrikaans
            "aka", "ak",    // Akan
            "amh", "am",    // Amharic
            "ara", "ar",    // Arabic
            "arg", "an",    // Aragonese
            "asm", "as",    // Assamese
            "ava", "av",    // Avaric
            "ave", "ae",    // Avestan
            "aym", "ay",    // Aymara
            "aze", "az",    // Azerbaijani
            "bak", "ba",    // Bashkir
            "bam", "bm",    // Bambara
            "bel", "be",    // Belarusian
            "ben", "bn",    // Bengali
            "bih", "bh",    // Bihari
            "bis", "bi",    // Bislama
            "bod", "bo",    // Tibetan
            "bos", "bs",    // Bosnian
            "bre", "br",    // Breton
            "bul", "bg",    // Bulgarian
            "cat", "ca",    // Catalan; Valencian
            "ces", "cs",    // Czech
            "cha", "ch",    // Chamorro
            "che", "ce",    // Chechen
            "chu", "cu",    // Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic
            "chv", "cv",    // Chuvash
            "cor", "kw",    // Cornish
            "cos", "co",    // Corsican
            "cre", "cr",    // Cree
            "cym", "cy",    // Welsh
            "dan", "da",    // Danish
            "deu", "de",    // German
            "div", "dv",    // Divehi; Dhivehi; Maldivian
            "dzo", "dz",    // Dzongkha
            "ell", "el",    // Greek, Modern (1453-)
            "eng", "en",    // English
            "epo", "eo",    // Esperanto
            "est", "et",    // Estonian
            "eus", "eu",    // Basque
            "ewe", "ee",    // Ewe
            "fao", "fo",    // Faroese
            "fas", "fa",    // Persian
            "fij", "fj",    // Fijian
            "fin", "fi",    // Finnish
            "fra", "fr",    // French
            "fry", "fy",    // Western Frisian
            "ful", "ff",    // Fulah
            "gla", "gd",    // Gaelic; Scottish Gaelic
            "gle", "ga",    // Irish
            "glg", "gl",    // Galician
            "glv", "gv",    // Manx
            "grn", "gn",    // Guarani
            "guj", "gu",    // Gujarati
            "hat", "ht",    // Haitian; Haitian Creole
            "hau", "ha",    // Hausa
            "heb", "he",    // Hebrew
            "her", "hz",    // Herero
            "hin", "hi",    // Hindi
            "hmo", "ho",    // Hiri Motu
            "hrv", "hr",    // Croatian
            "hun", "hu",    // Hungarian
            "hye", "hy",    // Armenian
            "ibo", "ig",    // Igbo
            "ido", "io",    // Ido
            "iii", "ii",    // Sichuan Yi; Nuosu
            "iku", "iu",    // Inuktitut
            "ile", "ie",    // Interlingue; Occidental
            "ina", "ia",    // Interlingua (International Auxiliary Language Association)
            "ind", "id",    // Indonesian
            "ipk", "ik",    // Inupiaq
            "isl", "is",    // Icelandic
            "ita", "it",    // Italian
            "jav", "jv",    // Javanese
            "jpn", "ja",    // Japanese
            "kal", "kl",    // Kalaallisut; Greenlandic
            "kan", "kn",    // Kannada
            "kas", "ks",    // Kashmiri
            "kat", "ka",    // Georgian
            "kau", "kr",    // Kanuri
            "kaz", "kk",    // Kazakh
            "khm", "km",    // Central Khmer
            "kik", "ki",    // Kikuyu; Gikuyu
            "kin", "rw",    // Kinyarwanda
            "kir", "ky",    // Kirghiz; Kyrgyz
            "kom", "kv",    // Komi
            "kon", "kg",    // Kongo
            "kor", "ko",    // Korean
            "kua", "kj",    // Kuanyama; Kwanyama
            "kur", "ku",    // Kurdish
            "lao", "lo",    // Lao
            "lat", "la",    // Latin
            "lav", "lv",    // Latvian
            "lim", "li",    // Limburgan; Limburger; Limburgish
            "lin", "ln",    // Lingala
            "lit", "lt",    // Lithuanian
            "ltz", "lb",    // Luxembourgish; Letzeburgesch
            "lub", "lu",    // Luba-Katanga
            "lug", "lg",    // Ganda
            "mah", "mh",    // Marshallese
            "mal", "ml",    // Malayalam
            "mar", "mr",    // Marathi
            "mkd", "mk",    // Macedonian
            "mlg", "mg",    // Malagasy
            "mlt", "mt",    // Maltese
            "mon", "mn",    // Mongolian
            "mri", "mi",    // Maori
            "msa", "ms",    // Malay
            "mya", "my",    // Burmese
            "nau", "na",    // Nauru
            "nav", "nv",    // Navajo; Navaho
            "nbl", "nr",    // Ndebele, South; South Ndebele
            "nde", "nd",    // Ndebele, North; North Ndebele
            "ndo", "ng",    // Ndonga
            "nep", "ne",    // Nepali
            "nld", "nl",    // Dutch; Flemish
            "nno", "nn",    // Norwegian Nynorsk; Nynorsk, Norwegian
            "nob", "nb",    // Bokmal, Norwegian; Norwegian Bokmal
            "nor", "no",    // Norwegian
            "nya", "ny",    // Chichewa; Chewa; Nyanja
            "oci", "oc",    // Occitan (post 1500); Provencal
            "oji", "oj",    // Ojibwa
            "ori", "or",    // Oriya
            "orm", "om",    // Oromo
            "oss", "os",    // Ossetian; Ossetic
            "pan", "pa",    // Panjabi; Punjabi
            "pli", "pi",    // Pali
            "pol", "pl",    // Polish
            "por", "pt",    // Portuguese
            "pus", "ps",    // Pushto; Pashto
            "que", "qu",    // Quechua
            "roh", "rm",    // Romansh
            "ron", "ro",    // Romanian; Moldavian; Moldovan
            "run", "rn",    // Rundi
            "rus", "ru",    // Russian
            "sag", "sg",    // Sango
            "san", "sa",    // Sanskrit
            "sin", "si",    // Sinhala; Sinhalese
            "slk", "sk",    // Slovak
            "slv", "sl",    // Slovenian
            "sme", "se",    // Northern Sami
            "smo", "sm",    // Samoan
            "sna", "sn",    // Shona
            "snd", "sd",    // Sindhi
            "som", "so",    // Somali
            "sot", "st",    // Sotho, Southern
            "spa", "es",    // Spanish; Castilian
            "sqi", "sq",    // Albanian
            "srd", "sc",    // Sardinian
            "srp", "sr",    // Serbian
            "ssw", "ss",    // Swati
            "sun", "su",    // Sundanese
            "swa", "sw",    // Swahili
            "swe", "sv",    // Swedish
            "tah", "ty",    // Tahitian
            "tam", "ta",    // Tamil
            "tat", "tt",    // Tatar
            "tel", "te",    // Telugu
            "tgk", "tg",    // Tajik
            "tgl", "tl",    // Tagalog
            "tha", "th",    // Thai
            "tir", "ti",    // Tigrinya
            "ton", "to",    // Tonga (Tonga Islands)
            "tsn", "tn",    // Tswana
            "tso", "ts",    // Tsonga
            "tuk", "tk",    // Turkmen
            "tur", "tr",    // Turkish
            "twi", "tw",    // Twi
            "uig", "ug",    // Uighur; Uyghur
            "ukr", "uk",    // Ukrainian
            "urd", "ur",    // Urdu
            "uzb", "uz",    // Uzbek
            "ven", "ve",    // Venda
            "vie", "vi",    // Vietnamese
            "vol", "vo",    // Volapuk
            "wln", "wa",    // Walloon
            "wol", "wo",    // Wolof
            "xho", "xh",    // Xhosa
            "yid", "yi",    // Yiddish
            "yor", "yo",    // Yoruba
            "zha", "za",    // Zhuang; Chuang
            "zho", "zh",    // Chinese
            "zul", "zu",    // Zulu
        };
        int i = 0;
        while (i < alpha3to2.length) {
            THREE_TO_TWO.put(alpha3to2[i], alpha3to2[i+1]);
            i += 2;
        }
    }

    public static String getShortest(String code) {
        if (code.length() == 3) {
            String code3 = AsciiUtil.toLowerString(code);
            String code2 = (String)THREE_TO_TWO.get(code3);
            if (code2 != null)
                return code2;
        }
        return code;
    }
}
