/*
**********************************************************************
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File ULOC.CPP
*
* Modification History:
*
*   Date        Name        Description
*   04/01/97    aliu        Creation.
*   08/21/98    stephen     JDK 1.2 sync
*   12/08/98    rtg         New Locale implementation and C API
*   03/15/99    damiba      overhaul.
*   04/06/99    stephen     changed setDefault() to realloc and copy
*   06/14/99    stephen     Changed calls to ures_open for new params
*   07/21/99    stephen     Modified setDefault() to propagate to C++
*****************************************************************************/

/*
   POSIX's locale format, from putil.c: [no spaces]

     ll [ _CC ] [ . MM ] [ @ VV]

     l = lang, C = ctry, M = charmap, V = variant
*/


#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"

#include "ustr_imp.h"
#include "uresimp.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "ucln_cmn.h"

/****************************************************************************
  Global variable and type definitions
*****************************************************************************/

/* Locale stuff from locid.cpp */
U_CFUNC void locale_set_default(const char *id);
U_CFUNC const char *locale_get_default(void);

/* These strings describe the resources we attempt to load from
 the locale ResourceBundle data file.*/
static const char _kLocaleID[]       = "LocaleID";
static const char _kLanguages[]      = "Languages";
static const char _kCountries[]      = "Countries";
static const char _kIndexLocaleName[] = "res_index";
static const char _kIndexTag[]       = "InstalledLocales";

#if 0
/* We don't use these resources currently */
static const char* _kLocaleString   = "LocaleString";
static const char* _kShortLanguage  = "ShortLanguage";
static const char* _kShortCountry   = "ShortCountry";
#endif

#define TEMPBUFSIZE 8

static char** _installedLocales = NULL;
static int32_t _installedLocalesCount = 0;

/* tables updated per http://www.egt.ie/standards/iso639 
    and http://lcweb.loc.gov/standards/iso639-2/ 
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
static const char * const _languages[] = {
    "aa",  "ab",  "ace", "ach", "ada", "ae",  "af",  "afa",
    "afh", "ak",  "akk", "ale", "alg", "am",  "ang", "apa",
    "ar",  "arc", "arn", "arp", "art", "arw", "as",  "ast",
    "ath", "aus", "av",  "awa", "ay",  "az",  "ba",  "bad",
    "bai", "bal", "bam", "ban", "bas", "bat", "be",  "bej",
    "bem", "ber", "bg",  "bh",  "bho", "bi",  "bik", "bin",
    "bla", "bm",  "bn",  "bnt", "bo",  "br",  "bra", "bs",
    "btk", "bua", "bug", "ca",  "cad", "cai", "car", "cau",
    "ce",  "ceb", "cel", "ch",  "chb", "chg", "chk", "chm",
    "chn", "cho", "chp", "chr", "chy", "cmc", "co",  "cop",
    "cpe", "cpf", "cpp", "cre", "crp", "cs",  "cu",  "cus",
    "cv",  "cy",  "da",  "dak", "day", "de",  "del", "den",
    "dgr", "din", "doi", "dra", "dua", "dum", "dv",  "dyu",
    "dz",  "ee",  "efi", "egy", "eka", "el",  "elx", "en",
    "enm", "eo",  "es",  "et",  "eu",  "ewe", "ewo", "fa",
    "fan", "fat", "ff",  "fi",  "fiu", "fj",  "fo",  "fon",
    "fr",  "frm", "fro", "fur", "fy",  "ga",  "gaa", "gay",
    "gba", "gd",  "gem", "gez", "gil", "gl",  "gmh", "gn",
    "goh", "gon", "gor", "got", "grb", "grc", "gu",  "gv",
    "gwi", "ha",  "hai", "haw", "he",  "hi",  "hil", "him",
    "hit", "hmn", "ho",  "hr",  "hu",  "hup", "hy",  "hz",
    "ia",  "iba", "id",  "ie",  "ig",  "ii",  "ijo", "ik",
    "ilo", "inc", "ine", "inh", "ira", "iro", "is",  "it",
    "iu",  "ja",  "jpr", "jrb", "jv",  "ka",  "kaa", "kab",
    "kac", "kam", "kar", "kaw", "kbd", "kg",  "kha", "khi",
    "kho", "ki",  "kj",  "kk",  "kl",  "km",  "kmb", "kn",
    "ko",  "kok", "kos", "kpe", "kr",  "kro", "kru", "ks",
    "ku",  "kum", "kut", "kv",  "kw",  "ky",  "la",  "lad",
    "lah", "lam", "lb",  "lez", "lg",  "ln",  "lo",  "lol",
    "loz", "lt",  "lu",  "lua", "lui", "lun", "luo", "lus",
    "lv",  "mad", "mag", "mai", "mak", "man", "map", "mas",
    "mdr", "men", "mg",  "mga", "mh",  "mi",  "mic", "min",
    "mis", "mk",  "mkh", "ml",  "mn",  "mnc", "mni", "mno",
    "mo",  "moh", "mos", "mr",  "ms",  "mt",  "mul", "mun",
    "mus", "mwr", "my",  "myn", "na",  "nah", "nai", "nap",
    "nb",  "nd",  "nds", "ne",  "new", "ng",  "nia", "nic",
    "niu", "nl",  "nn",  "no",  "non", "nr",  "nso", "nub",
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
    "ty",  "tyv", "ug",  "uga", "uk",  "umb", "und", "ur",
    "uz",  "vai", "ve",  "vi",  "vo",  "vot", "wa",  "wak",
    "wal", "war", "was", "wen", "wo",  "xh",  "yao", "yap",
    "yi",  "yo",  "ypk", "za",  "zap", "zen", "zh",  "znd",
    "zu",  "zun", 
NULL,
    "in",  "iw",  "ji",  "jw",  "sh",    /* obsolete language codes */
NULL
};

/* This list MUST contain a three-letter code for every two-letter code in the
   list above, and they MUST ne in the same order (i.e., the same language must
   be in the same place in both lists)! */
static const char * const _languages3[] = {
/*  "aa",  "ab",  "ace", "ach", "ada", "ae",  "af",  "afa",    */
    "aar", "abk", "ace", "ach", "ada", "ave", "afr", "afa",
/*  "afh", "ak",  "akk", "ale", "alg", "am",  "ang", "apa",    */
    "afh", "aka", "akk", "ale", "alg", "amh", "ang", "apa",
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
/*  "btk", "bua", "bug", "ca",  "cad", "cai", "car", "cau",    */
    "btk", "bua", "bug", "cat", "cad", "cai", "car", "cau",
/*  "ce",  "ceb", "cel", "ch",  "chb", "chg", "chk", "chm",    */
    "che", "ceb", "cel", "cha", "chb", "chg", "chk", "chm",
/*  "chn", "cho", "chp", "chr", "chy", "cmc", "co",  "cop",    */
    "chn", "cho", "chp", "chr", "chy", "cmc", "cos", "cop",
/*  "cpe", "cpf", "cpp", "cre", "crp", "cs",  "cu",  "cus",    */
    "cpe", "cpf", "cpp", "cre", "crp", "ces", "chu", "cus",
/*  "cv",  "cy",  "da",  "dak", "day", "de",  "del", "den",    */
    "chv", "cym", "dan", "dak", "day", "deu", "del", "den",
/*  "dgr", "din", "doi", "dra", "dua", "dum", "dv",  "dyu",    */
    "dgr", "din", "doi", "dra", "dua", "dum", "div", "dyu",
/*  "dz",  "ee",  "efi", "egy", "eka", "el",  "elx", "en",     */
    "dzo", "ewe", "efi", "egy", "eka", "ell", "elx", "eng",
/*  "enm", "eo",  "es",  "et",  "eu",  "ewe", "ewo", "fa",     */
    "enm", "epo", "spa", "est", "eus", "ewe", "ewo", "fas",
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
/*  "hit", "hmn", "ho",  "hr",  "hu",  "hup", "hy",  "hz",     */
    "hit", "hmn", "hmo", "hrv", "hun", "hup", "hye", "her",
/*  "ia",  "iba", "id",  "ie",  "ig",  "ii",  "ijo", "ik",     */
    "ina", "iba", "ind", "ile", "ibo", "iii", "ijo", "ipk",
/*  "ilo", "inc", "ine", "inh", "ira", "iro", "is",  "it",      */
    "ilo", "inc", "ine", "inh", "ira", "iro", "isl", "ita",
/*  "iu",  "ja",  "jpr", "jrb", "jv",  "ka",  "kaa", "kab",   */
    "iku", "jpn", "jpr", "jrb", "jaw", "kat", "kaa", "kab",
/*  "kac", "kam", "kar", "kaw", "kbd", "kg",  "kha", "khi",    */
    "kac", "kam", "kar", "kaw", "kbd", "kon", "kha", "khi",
/*  "kho", "ki",  "kj",  "kk",  "kl",  "km",  "kmb", "kn",     */
    "kho", "kik", "kua", "kaz", "kal", "khm", "kmb", "kan",
/*  "ko",  "kok", "kos", "kpe", "kr",  "kro", "kru", "ks",     */
    "kor", "kok", "kos", "kpe", "kau", "kro", "kru", "kas",
/*  "ku",  "kum", "kut", "kv",  "kw",  "ky",  "la",  "lad",    */
    "kur", "kum", "kut", "kom", "cor", "kir", "lat", "lad",
/*  "lah", "lam", "lb",  "lez", "lg",  "ln",  "lo",  "lol",    */
    "lah", "lam", "ltz", "lez", "lug", "lin", "lao", "lol",
/*  "loz", "lt",  "lu",  "lua", "lui", "lun", "luo", "lus",    */
    "loz", "lit", "lub", "lua", "lui", "lun", "luo", "lus",
/*  "lv",  "mad", "mag", "mai", "mak", "man", "map", "mas",    */
    "lav", "mad", "mag", "mai", "mak", "man", "map", "mas",
/*  "mdr", "men", "mg",  "mga", "mh",  "mi",  "mic", "min",    */
    "mdr", "men", "mlg", "mga", "mah", "mri", "mic", "min",
/*  "mis", "mk",  "mkh", "ml",  "mn",  "mnc", "mni", "mno",    */
    "mis", "mkd", "mkh", "mal", "mon", "mnc", "mni", "mno",
/*  "mo",  "moh", "mos", "mr",  "ms",  "mt",  "mul", "mun",    */
    "mol", "moh", "mos", "mar", "msa", "mlt", "mul", "mun",
/*  "mus", "mwr", "my",  "myn", "na",  "nah", "nai", "nap",    */
    "mus", "mwr", "mya", "myn", "nau", "nah", "nai", "nap",
/*  "nb",  "nd",  "nds", "ne",  "new", "ng",  "nia", "nic",    */
    "nob", "nde", "nds", "nep", "new", "ndo", "nia", "nic",
/*  "niu", "nl",  "nn",  "no",  "non", "nr",  "nso", "nub",    */
    "niu", "nld", "nno", "nor", "non", "nbl", "nso", "nub",
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
/*  "ty",  "tyv", "ug",  "uga", "uk",  "umb", "und", "ur",     */
    "tah", "tyv", "uig", "uga", "ukr", "umb", "und", "urd",
/*  "uz",  "vai", "ve",  "vi",  "vo",  "vot", "wa",  "wak",    */
    "uzb", "vai", "ven", "vie", "vol", "vot", "wln", "wak",
/*  "wal", "war", "was", "wen", "wo",  "xh",  "yao", "yap",    */
    "wal", "war", "was", "wen", "wol", "xho", "yao", "yap",
/*  "yi",  "yo",  "ypk", "za",  "zap", "zen", "zh",  "znd",    */
    "yid", "yor", "ypk", "zha", "zap", "zen", "zho", "znd",
/*  "zu",  "zun",                                              */
    "zul", "zun",  
NULL,
/*  "in",  "iw",  "ji",  "jw",  "sh",                          */
    "ind", "heb", "yid", "jaw", "srp",
NULL
};

/* ZR(ZAR) is now CD(COD) and FX(FXX) is PS(PSE) as per
 http://www.evertype.com/standards/iso3166/iso3166-1-en.html 
 added new codes keeping the old ones for compatibility
 updated to include 1999/12/03 revisions *CWB*/

/* RO(ROM) is now RO(ROU) according to 
 http://www.iso.org/iso/en/prods-services/iso3166ma/03updates-on-iso-3166/nlv3e-rou.html
*/

/* This list MUST be in sorted order, and MUST contain only two-letter codes! */
static const char * const _countries[] = {
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
NULL,
    "FX",  "RO",  "TP",  "ZR",   /* obsolete country codes */
NULL
};

/* This list MUST contain a three-letter code for every two-letter code in
   the above list, and they MUST be listed in the same order! */
static const char * const _countries3[] = {
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
NULL,
/*  "FX",  "RO",  "TP",  "ZR",   */
    "FXX", "ROM", "TMP", "ZAR",
NULL
};

/*******************************************************************************
  Implementation function definitions
*******************************************************************************/

/*Lazy evaluated the list of installed locales*/
static void _load_installedLocales(void);

/*returns TRUE if a is an ID separator FALSE otherwise*/
#define _isIDSeparator(a) (a == '_' || a == '-')

#define _isPrefixLetter(a) ((a=='x')||(a=='X')||(a=='i')||(a=='I'))
/*returns TRUE if one of the special prefixes is here (s=string)
  'x-' or 'i-' */
#define _isIDPrefix(s) (_isPrefixLetter(s[0])&&_isIDSeparator(s[1]))

/* Dot terminates it because of POSIX form  where dot precedes the codepage
 * except for variant
 */
#define _isTerminator(a)  ((a==0)||(a=='.')||(a=='@'))

static int16_t _findIndex(const char* const* list, const char* key)
{
    const char* const* anchor = list;
    
    while (*list)
    {
        if (uprv_strcmp(key, *list) == 0) 
        {
            return (int16_t)(list - anchor);
        }
        list++;
    }
    list++;     /* skip first NULL *CWB*/
    while (*list)    /* scan obsolete table */
    {
        if (uprv_strcmp(key, *list) == 0) 
        {
            return (int16_t)(list - anchor);
        }
        list++;
    }
    return -1;
}

/* count the length of src while copying it to dest; return strlen(src) */
static U_INLINE int32_t
_copyCount(char *dest, int32_t destCapacity, const char *src) {
    const char *anchor;
    char c;

    anchor=src;
    for(;;) {
        if((c=*src)==0) {
            return (int32_t)(src-anchor);
        }
        if(destCapacity<=0) {
            return (int32_t)((src-anchor)+uprv_strlen(src));
        }
        ++src;
        *dest++=c;
        --destCapacity;
    }
}

/*******************************************************************************
  API function definitions
*******************************************************************************/

U_CAPI const char*  U_EXPORT2
uloc_getDefault()
{
    return locale_get_default();
}

U_CAPI void  U_EXPORT2
uloc_setDefault(const char*   newDefaultLocale,
             UErrorCode* err) 
{
    if (U_FAILURE(*err))
        return;
    /* the error code isn't currently used for anything by this function*/
    
    /* propagate change to C++ */
    locale_set_default(newDefaultLocale);
}


U_CAPI int32_t  U_EXPORT2
uloc_getParent(const char*    localeID,
               char* parent,
               int32_t parentCapacity,
               UErrorCode* err)
{
    const char *lastUnderscore;
    int32_t i;
    
    if (U_FAILURE(*err))
        return 0;
    
    if (localeID == NULL)
        localeID = uloc_getDefault();

    lastUnderscore=uprv_strrchr(localeID, '_');
    if(lastUnderscore!=NULL) {
        i=(int32_t)(lastUnderscore-localeID);
    } else {
        i=0;
    }

    if(i>0) {
        uprv_memcpy(parent, localeID, uprv_min(i, parentCapacity));
    }
    return u_terminateChars(parent, parentCapacity, i, err);
}

/*
 * the internal functions _getLanguage(), _getCountry(), _getVariant()
 * avoid duplicating code to handle the earlier locale ID pieces
 * in the functions for the later ones by
 * setting the *pEnd pointer to where they stopped parsing
 *
 * TODO try to use this in Locale
 */
static int32_t
_getLanguage(const char *localeID,
             char *language, int32_t languageCapacity,
             const char **pEnd) {
    int32_t i=0;
    int32_t offset;
    char lang[4]={ 0, 0, 0, 0 }; /* temporary buffer to hold language code for searching */

    /* if it starts with i- or x- then copy that prefix */
    if(_isIDPrefix(localeID)) {
        if(i<languageCapacity) {
            language[i]=(char)uprv_tolower(*localeID);
        }
        if(i<languageCapacity) {
            language[i+1]='-';
        }
        i+=2;
        localeID+=2;
    }
    
    /* copy the language as far as possible and count its length */
    while(!_isTerminator(*localeID) && !_isIDSeparator(*localeID)) {
        if(i<languageCapacity) {
            language[i]=(char)uprv_tolower(*localeID);
        }
        if(i<3) {
            lang[i]=(char)uprv_tolower(*localeID);
        }
        i++;
        localeID++;
    }

    if(i==3) {
        /* convert 3 character code to 2 character code if possible *CWB*/
        offset=_findIndex(_languages3, lang);
        if(offset>=0) {
            i=_copyCount(language, languageCapacity, _languages[offset]);
        }
    }

    if(pEnd!=NULL) {
        *pEnd=localeID;
    }
    return i;
}

U_CAPI int32_t U_EXPORT2
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err)
{
    /* uloc_getLanguage will return a 2 character iso-639 code if one exists. *CWB*/
    int32_t i=0;

    if (err==NULL || U_FAILURE(*err)) {
        return 0;
    }
    
    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    i=_getLanguage(localeID, language, languageCapacity, NULL);
    return u_terminateChars(language, languageCapacity, i, err);
}

static int32_t
_getCountry(const char *localeID,
            char *country, int32_t countryCapacity,
            const char **pEnd) {
    int32_t i=0;
    char cnty[4]={ 0, 0, 0, 0 };
    int32_t offset;

    /* copy the country as far as possible and count its length */
    while(!_isTerminator(*localeID) && !_isIDSeparator(*localeID)) {
        if(i<countryCapacity) {
            country[i]=(char)uprv_toupper(*localeID);
        }
        if(i<3) {   /*CWB*/
            cnty[i]=(char)uprv_toupper(*localeID);
        }
        i++;
        localeID++;
    }

    /* convert 3 character code to 2 character code if possible *CWB*/
    if(i==3) {
        offset=_findIndex(_countries3, cnty);
        if(offset>=0) {
            i=_copyCount(country, countryCapacity, _countries[offset]);
        }
    }

    if(pEnd!=NULL) {
        *pEnd=localeID;
    }
    return i;
}

U_CAPI int32_t  U_EXPORT2
uloc_getCountry(const char* localeID,
            char* country,
            int32_t countryCapacity,
            UErrorCode* err) 
{
    int32_t i=0;

    if(err==NULL || U_FAILURE(*err)) {
        return 0;
    }

    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    /* skip the language */
    _getLanguage(localeID, NULL, 0, &localeID);
    if(_isIDSeparator(*localeID)) {
        i=_getCountry(localeID+1, country, countryCapacity, NULL);
    }
    return u_terminateChars(country, countryCapacity, i, err);
}

static int32_t
_getVariant(const char *localeID,
            char prev,
            char *variant, int32_t variantCapacity) {
    int32_t i=0;

    /* get one or more variant tags and separate them with '_' */
    if(_isIDSeparator(prev)) {
        /* get a variant string after a '-' or '_' */
        while(!_isTerminator(*localeID)) {
            if(i<variantCapacity) {
                variant[i]=(char)uprv_toupper(*localeID);
                if(variant[i]=='-') {
                    variant[i]='_';
                }
            }
            i++;
            localeID++;
        }
    }

    /* if there is no variant tag after a '-' or '_' then look for '@' */
    if(i==0) {
        if(prev=='@') {
            /* keep localeID */
        } else if((localeID=uprv_strrchr(localeID, '@'))!=NULL) {
            ++localeID; /* point after the '@' */
        } else {
            return 0;
        }
        while(!_isTerminator(*localeID)) {
            if(i<variantCapacity) {
                variant[i]=(char)uprv_toupper(*localeID);
                if(variant[i]=='-' || variant[i]==',') {
                    variant[i]='_';
                }
            }
            i++;
            localeID++;
        }
    }

    return i;
}

U_CAPI int32_t  U_EXPORT2
uloc_getVariant(const char* localeID,
                        char* variant,
                        int32_t variantCapacity,
                        UErrorCode* err) 
{
    int32_t i=0;
    UBool haveVariant=FALSE;

    if(err==NULL || U_FAILURE(*err)) {
        return 0;
    }
    
    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    /* skip the language and the country */
    _getLanguage(localeID, NULL, 0, &localeID);
    if(_isIDSeparator(*localeID)) {
        _getCountry(localeID+1, NULL, 0, &localeID);
        if(_isIDSeparator(*localeID)) {
            haveVariant=TRUE;
            i=_getVariant(localeID+1, *localeID, variant, variantCapacity);
        }
    }

    /* if we do not have a variant tag yet then try a POSIX variant after '@' */
    if(!haveVariant && (localeID=uprv_strrchr(localeID, '@'))!=NULL) {
        i=_getVariant(localeID+1, '@', variant, variantCapacity);
    }
    return u_terminateChars(variant, variantCapacity, i, err);
}

U_CAPI int32_t  U_EXPORT2
uloc_getName(const char* localeID,
             char* name,
             int32_t nameCapacity,
             UErrorCode* err)  
{
    int32_t i, fieldCount;

    if(err==NULL || U_FAILURE(*err)) {
        return 0;
    }
    
    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    /* get all pieces, one after another, and separate with '_' */
    fieldCount=0;
    i=_getLanguage(localeID, name, nameCapacity, &localeID);
    if(_isIDSeparator(*localeID)) {
        ++fieldCount;
        if(i<nameCapacity) {
            name[i]='_';
        }
        ++i;
        i+=_getCountry(localeID+1, name+i, nameCapacity-i, &localeID);
        if(_isIDSeparator(*localeID)) {
            ++fieldCount;
            if(i<nameCapacity) {
                name[i]='_';
            }
            ++i;
            i+=_getVariant(localeID+1, *localeID, name+i, nameCapacity-i);
        }
    }

    /* if we do not have a variant tag yet then try a POSIX variant after '@' */
    if(fieldCount<2 && (localeID=uprv_strrchr(localeID, '@'))!=NULL) {
        do {
            if(i<nameCapacity) {
                name[i]='_';
            }
            ++i;
            ++fieldCount;
        } while(fieldCount<2);
        i+=_getVariant(localeID+1, '@', name+i, nameCapacity-i);
    }
    return u_terminateChars(name, nameCapacity, i, err);
}
       
U_CAPI const char*  U_EXPORT2
uloc_getISO3Language(const char* localeID) 
{
  int16_t offset;
  char lang[TEMPBUFSIZE];
  UErrorCode err = U_ZERO_ERROR;

  if (localeID == NULL)
  {
      localeID = uloc_getDefault();
  }
  uloc_getLanguage(localeID, lang, TEMPBUFSIZE, &err);
  if (U_FAILURE(err))
      return "";
  offset = _findIndex(_languages, lang);
  if (offset < 0)
      return "";
  return _languages3[offset];
}

U_CAPI const char*  U_EXPORT2
uloc_getISO3Country(const char* localeID) 
{
    int16_t offset;
    char cntry[TEMPBUFSIZE];
    UErrorCode err = U_ZERO_ERROR;
    
    if (localeID == NULL)
    {
        localeID = uloc_getDefault();
    }
    uloc_getCountry(localeID, cntry, TEMPBUFSIZE, &err);
    if (U_FAILURE(err))
        return "";
    offset = _findIndex(_countries, cntry);
    if (offset < 0)
        return "";
    
    return _countries3[offset];
}

U_CAPI uint32_t  U_EXPORT2
uloc_getLCID(const char* localeID) 
{
    UErrorCode err = U_ZERO_ERROR;
    uint32_t result = 0;
    UResourceBundle* bundle = ures_open(NULL, localeID, &err);
    
    if (U_SUCCESS(err))
    {
        UResourceBundle *resLocaleID = ures_getByKey(bundle, _kLocaleID, NULL, &err);
        if (U_SUCCESS(err))
        {
            result = ures_getInt(resLocaleID, &err);
            if (U_FAILURE(err))
            {
                result = 0;
            }
            ures_close(resLocaleID);
        }
        ures_close(bundle);
    }
    
    return result;
}

/*
 * Lookup a resource bundle table item with fallback on the table level.
 * Regular resource bundle lookups perform fallback to parent locale bundles
 * and eventually the root bundle, but only for top-level items.
 * This function takes the name of a top-level table and of an item in that table
 * and performs a lookup of both, falling back until a bundle contains a table
 * with this item.
 *
 * Note: Only the opening of entire bundles falls back through the default locale
 * before root. Once a bundle is open, item lookups do not go through the
 * default locale because that would result in a mix of languages that is
 * unpredictable to the programmer and most likely useless.
 */
static const UChar *
_res_getTableStringWithFallback(const char *path, const char *locale,
                              const char *tableKey, const char *itemKey,
                              int32_t *pLength,
                              UErrorCode *pErrorCode)
{
    char localeBuffer[80];
    UResourceBundle *rb, table;
    const UChar *item;
    UErrorCode errorCode;

    for(;;) {
        /*
         * open the bundle for the current locale
         * this falls back through the locale's chain to root
         */
        errorCode=U_ZERO_ERROR;
        rb=ures_open(path, locale, &errorCode);
        if(U_FAILURE(errorCode)) {
            /* total failure, not even root could be opened */
            *pErrorCode=errorCode;
            return NULL;
        } else if(errorCode==U_USING_DEFAULT_WARNING ||
                  (errorCode==U_USING_FALLBACK_WARNING && *pErrorCode!=U_USING_DEFAULT_WARNING)
        ) {
            /* set the "strongest" error code (success->fallback->default->failure) */
            *pErrorCode=errorCode;
        }

        /*
         * try to open the requested table
         * this falls back through the locale's chain to root, but not through the default locale
         */
        errorCode=U_ZERO_ERROR;
        ures_initStackObject(&table);
        ures_getByKey(rb, tableKey, &table, &errorCode);
        if(U_FAILURE(errorCode)) {
            /* no such table anywhere in this fallback chain */
            ures_close(rb);
            *pErrorCode=errorCode;
            return NULL;
        } else if(errorCode==U_USING_DEFAULT_WARNING ||
                  (errorCode==U_USING_FALLBACK_WARNING && *pErrorCode!=U_USING_DEFAULT_WARNING)
        ) {
            /* set the "strongest" error code (success->fallback->default->failure) */
            *pErrorCode=errorCode;
        }

        /* try to open the requested item in the table */
        errorCode=U_ZERO_ERROR;
        item=ures_getStringByKey(&table, itemKey, pLength, &errorCode);
        if(U_SUCCESS(errorCode)) {
            /* we got the requested item! */
            ures_close(&table);
            ures_close(rb);

            if(errorCode==U_USING_DEFAULT_WARNING ||
               (errorCode==U_USING_FALLBACK_WARNING && *pErrorCode!=U_USING_DEFAULT_WARNING)
            ) {
                /* set the "strongest" error code (success->fallback->default->failure) */
                *pErrorCode=errorCode;
            }

            /*
             * It is safe to close the bundle and still return the
             * string pointer because resource bundles are
             * cached until u_cleanup().
             */
            return item;
        }

        /*
         * We get here if the item was not found.
         * We will follow the chain to the parent locale bundle and look in
         * the table there.
         */

        /* get the real locale ID for this table */
        errorCode=U_ZERO_ERROR;
        locale=ures_getLocale(&table, &errorCode);
        /* keep table and rb open until we are done using the locale string owned by the table bundle */
        if(U_FAILURE(errorCode)) {
            /* error getting the locale ID for an open RB - should never happen */
            ures_close(&table);
            ures_close(rb);
            *pErrorCode=U_INTERNAL_PROGRAM_ERROR;
            return NULL;
        }

        if(*locale==0 || 0==uprv_strcmp(locale, "root")) {
            /* end of fallback; even root does not have the requested item either */
            ures_close(&table);
            ures_close(rb);
            *pErrorCode=U_MISSING_RESOURCE_ERROR;
            return NULL;
        }

        /* could not find the table, or its item, try to fall back to a different RB and table */
        errorCode=U_ZERO_ERROR;
        uloc_getParent(locale, localeBuffer, sizeof(localeBuffer), &errorCode);
        /* done with the locale string - ready to close table and rb */
        ures_close(&table);
        ures_close(rb);
        if(U_FAILURE(errorCode) || errorCode==U_STRING_NOT_TERMINATED_WARNING) {
            /* error getting the parent locale ID - should never happen */
            *pErrorCode=U_INTERNAL_PROGRAM_ERROR;
            return NULL;
        }

        /* continue the fallback lookup with the parent locale ID */
        locale=localeBuffer;
    }
}

static int32_t
_getStringOrCopyKey(const char *path, const char *locale,
                    const char *tableKey, const char *itemKey,
                    const char *substitute,
                    UChar *dest, int32_t destCapacity,
                    UErrorCode *pErrorCode) {
    const UChar *s;
    int32_t length;

    if(itemKey==NULL) {
        /* top-level item: normal resource bundle access */
        UResourceBundle *rb;

        rb=ures_open(path, locale, pErrorCode);
        if(U_SUCCESS(*pErrorCode)) {
            s=ures_getStringByKey(rb, tableKey, &length, pErrorCode);
            /* see comment about closing rb near "return item;" in _res_getTableStringWithFallback() */
            ures_close(rb);
        }
    } else {
        /* second-level item, use special fallback */
        s=_res_getTableStringWithFallback(path, locale,
                                           tableKey, itemKey,
                                           &length,
                                           pErrorCode);
    }
    if(U_SUCCESS(*pErrorCode)) {
        int32_t copyLength=uprv_min(length, destCapacity);
        if(copyLength>0) {
            u_memcpy(dest, s, copyLength);
        }
    } else {
        /* no string from a resource bundle: convert the substitute */
        length=(int32_t)uprv_strlen(substitute);
        u_charsToUChars(substitute, dest, uprv_min(length, destCapacity));
        *pErrorCode=U_USING_DEFAULT_WARNING;
    }

    return u_terminateUChars(dest, destCapacity, length, pErrorCode);
}

U_CAPI int32_t U_EXPORT2
uloc_getDisplayLanguage(const char *locale,
                        const char *displayLocale,
                        UChar *dest, int32_t destCapacity,
                        UErrorCode *pErrorCode) {
    char localeBuffer[200];
    int32_t length;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    *pErrorCode=U_ZERO_ERROR;   /* necessary because we will check for a warning code */
    length=uloc_getLanguage(locale, localeBuffer, sizeof(localeBuffer), pErrorCode);
    if(U_FAILURE(*pErrorCode) || *pErrorCode==U_STRING_NOT_TERMINATED_WARNING) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(length==0) {
        return u_terminateUChars(dest, destCapacity, 0, pErrorCode);
    }

    return _getStringOrCopyKey(NULL, displayLocale,
                               _kLanguages, localeBuffer,
                               localeBuffer,
                               dest, destCapacity,
                               pErrorCode);
}

U_CAPI int32_t U_EXPORT2
uloc_getDisplayCountry(const char *locale,
                       const char *displayLocale,
                       UChar *dest, int32_t destCapacity,
                       UErrorCode *pErrorCode) {
    char localeBuffer[200];
    int32_t length;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    *pErrorCode=U_ZERO_ERROR;   /* necessary because we will check for a warning code */
    length=uloc_getCountry(locale, localeBuffer, sizeof(localeBuffer), pErrorCode);
    if(U_FAILURE(*pErrorCode) || *pErrorCode==U_STRING_NOT_TERMINATED_WARNING) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(length==0) {
        return u_terminateUChars(dest, destCapacity, 0, pErrorCode);
    }

    return _getStringOrCopyKey(NULL, displayLocale,
                               _kCountries, localeBuffer,
                               localeBuffer,
                               dest, destCapacity,
                               pErrorCode);
}

/*
 * TODO separate variant1_variant2_variant3...
 * by getting each tag's display string and concatenating them with ", "
 * in between - similar to uloc_getDisplayName()
 */
U_CAPI int32_t U_EXPORT2
uloc_getDisplayVariant(const char *locale,
                       const char *displayLocale,
                       UChar *dest, int32_t destCapacity,
                       UErrorCode *pErrorCode) {
    char localeBuffer[200];
    int32_t length;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /*
     * display names for variants are top-level items of
     * locale resource bundles
     * the rb keys are "%%" followed by the variant tags
     */
    *pErrorCode=U_ZERO_ERROR;   /* necessary because we will check for a warning code */
    localeBuffer[0]=localeBuffer[1]='%';
    length=uloc_getVariant(locale, localeBuffer+2, sizeof(localeBuffer)-2, pErrorCode);
    if(U_FAILURE(*pErrorCode) || *pErrorCode==U_STRING_NOT_TERMINATED_WARNING) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(length==0) {
        return u_terminateUChars(dest, destCapacity, 0, pErrorCode);
    }

    /* pass itemKey=NULL to look for a top-level item */
    return _getStringOrCopyKey(NULL, displayLocale,
                               localeBuffer, NULL,
                               localeBuffer+2,      /* substitute=variant without %% */
                               dest, destCapacity,
                               pErrorCode);
}

U_CAPI int32_t U_EXPORT2
uloc_getDisplayName(const char *locale,
                    const char *displayLocale,
                    UChar *dest, int32_t destCapacity,
                    UErrorCode *pErrorCode) {
    int32_t length, length2;
    UBool hasLanguage, hasCountry, hasVariant;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /*
     * if there is a language, then write "language (country, variant)"
     * otherwise write "country, variant"
     */

    /* write the language */
    length=uloc_getDisplayLanguage(locale, displayLocale,
                                   dest, destCapacity,
                                   pErrorCode);
    hasLanguage= length>0;

    if(hasLanguage) {
        /* append " (" */
        if(length<destCapacity) {
            dest[length]=0x20;
        }
        ++length;
        if(length<destCapacity) {
            dest[length]=0x28;
        }
        ++length;
    }

    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
        /* keep preflighting */
        *pErrorCode=U_ZERO_ERROR;
    }

    /* append the country */
    if(length<destCapacity) {
        length2=uloc_getDisplayCountry(locale, displayLocale,
                                       dest+length, destCapacity-length,
                                       pErrorCode);
    } else {
        length2=uloc_getDisplayCountry(locale, displayLocale,
                                       NULL, 0,
                                       pErrorCode);
    }
    hasCountry= length2>0;
    length+=length2;

    if(hasCountry) {
        /* append ", " */
        if(length<destCapacity) {
            dest[length]=0x2c;
        }
        ++length;
        if(length<destCapacity) {
            dest[length]=0x20;
        }
        ++length;
    }

    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
        /* keep preflighting */
        *pErrorCode=U_ZERO_ERROR;
    }

    /* append the variant */
    if(length<destCapacity) {
        length2=uloc_getDisplayVariant(locale, displayLocale,
                                       dest+length, destCapacity-length,
                                       pErrorCode);
    } else {
        length2=uloc_getDisplayVariant(locale, displayLocale,
                                       NULL, 0,
                                       pErrorCode);
    }
    hasVariant= length2>0;
    length+=length2;

    if(hasCountry && !hasVariant) {
        /* remove ", " */
        length-=2;
    }

    if(hasLanguage) {
        if(hasCountry || hasVariant) {
            /* append ")" */
            if(length<destCapacity) {
                dest[length]=0x29;
            }
            ++length;
        } else {
            /* remove " (" */
            length-=2;
        }
    }

    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
        /* keep preflighting */
        *pErrorCode=U_ZERO_ERROR;
    }

    return u_terminateUChars(dest, destCapacity, length, pErrorCode);
}

U_CAPI const char* U_EXPORT2
uloc_getAvailable(int32_t offset) 
{
    
    _load_installedLocales();
    
    if (offset > _installedLocalesCount)
        return NULL;
    else
        return _installedLocales[offset];

}

U_CAPI int32_t  U_EXPORT2
uloc_countAvailable()
{
    _load_installedLocales();
    return _installedLocalesCount;
}

UBool uloc_cleanup(void) {
    char ** temp;

    if (_installedLocales) {
        temp = _installedLocales;
        _installedLocales = NULL;

        _installedLocalesCount = 0;

        uprv_free(temp);
    }
    return TRUE;
}

static void _load_installedLocales()
{
    UBool   localesLoaded;

    umtx_lock(NULL);
    localesLoaded = _installedLocales != NULL;
    umtx_unlock(NULL);
    
    if (localesLoaded == FALSE) {
        UResourceBundle *index = NULL;
        UResourceBundle installed;
        UErrorCode status = U_ZERO_ERROR;
        char ** temp;
        int32_t i = 0;
        int32_t localeCount;
        
        ures_initStackObject(&installed);
        index = ures_openDirect(NULL, _kIndexLocaleName, &status);
        ures_getByKey(index, _kIndexTag, &installed, &status);
        
        if(U_SUCCESS(status)) {
            localeCount = ures_getSize(&installed);
            temp = (char **) uprv_malloc(sizeof(char*) * (localeCount+1));
            
            ures_resetIterator(&installed);
            while(ures_hasNext(&installed)) {
                ures_getNextString(&installed, NULL, (const char **)&temp[i++], &status);
            }
            temp[i] = NULL;
            
            umtx_lock(NULL);
            if (_installedLocales == NULL)
            {
                _installedLocales = temp;
                _installedLocalesCount = localeCount;
                temp = NULL;
            } 
            umtx_unlock(NULL);

            uprv_free(temp);
            ures_close(&installed);
        }
        ures_close(index);
    }
}



/**
 * Returns a list of all language codes defined in ISO 639.  This is a pointer
 * to an array of pointers to arrays of char.  All of these pointers are owned
 * by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
U_CAPI const char* const*  U_EXPORT2
uloc_getISOLanguages() 
{
    return _languages;
}

/**
 * Returns a list of all 2-letter country codes defined in ISO 639.  This is a
 * pointer to an array of pointers to arrays of char.  All of these pointers are
 * owned by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
U_CAPI const char* const*  U_EXPORT2
uloc_getISOCountries() 
{
    return _countries;
}
