/*
**********************************************************************
*   Copyright (C) 1997-2001, International Business Machines
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
static const char _kIndexLocaleName[] = "index";
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

static const char * const _languages[] = {
	"aa",  "ab",  "ace", "ach", "ada", "ae",  "af",  "afa",
	"afh", "aka", "akk", "ale", "alg", "am",  "ang", "apa",
	"ar",  "arc", "arn", "arp", "art", "arw", "as",  "ath",
	"aus", "ava", "awa", "ay",  "az",  "ba",  "bad", "bai",
	"bal", "bam", "ban", "bas", "bat", "be",  "bej", "bem",
	"ber", "bg",  "bh",  "bho", "bi",  "bik", "bin", "bla",
	"bn",  "bnt", "bo",  "br",  "bra", "bs",  "btk", "bua",
	"bug", "ca",  "cad", "cai", "car", "cau", "ce",  "ceb",
	"cel", "ch",  "chb", "chg", "chk", "chm", "chn", "cho",
	"chp", "chr", "chy", "cmc", "co",  "cop", "cpe", "cpf",
	"cpp", "cre", "crp", "cs",  "cu",  "cus", "cv",  "cy",
	"da",  "dak", "day", "de",  "del", "den", "dgr", "din",
	"div", "doi", "dra", "dua", "dum", "dyu", "dz",  "efi",
	"egy", "eka", "el",  "elx", "en",  "enm", "eo",  "es",
	"et",  "eu",  "ewe", "ewo", "fa",  "fan", "fat", "fi",
	"fiu", "fj",  "fo",  "fon", "fr",  "frm", "fro", "ful",
	"fur", "fy",  "ga",  "gaa", "gay", "gba", "gd",  "gem",
	"gez", "gil", "gl",  "gmh", "gn",  "goh", "gon", "gor",
	"got", "grb", "grc", "gu",  "gv",  "gwi", "ha",  "hai",
	"haw", "he",  "hi",  "hil", "him", "hit", "hmn", "ho",
	"hr",  "hu",  "hup", "hy",  "hz",  "ia",  "iba", "ibo",
	"id",  "ie",  "ijo", "ik",  "ilo", "inc", "ine", "ira",
	"iro", "is",  "it",  "iu",  "ja",  "jpr", "jrb", "jv",
	"ka",  "kaa", "kab", "kac", "kam", "kar", "kau", "kaw",
	"kha", "khi", "kho", "ki",  "kj",  "kk",  "kl",  "km",
	"kmb", "kn",  "ko",  "kok", "kon", "kos", "kpe", "kro",
	"kru", "ks",  "ku",  "kum", "kut", "kv",  "kw",  "ky",
	"la",  "lad", "lah", "lam", "lb",  "lez", "ln",  "lo",
	"lol", "loz", "lt",  "lua", "lub", "lug", "lui", "lun",
	"luo", "lus", "lv",  "mad", "mag", "mai", "mak", "man",
	"map", "mas", "mdr", "men", "mg",  "mga", "mh",  "mi",
	"mic", "min", "mis", "mk",  "mkh", "ml",  "mn",  "mnc",
	"mni", "mno", "mo",  "moh", "mos", "mr",  "ms",  "mt",
	"mul", "mun", "mus", "mwr", "my",  "myn", "na",  "nah",
	"nai", "nb",  "nd",  "nds", "ne",  "new", "ng",  "nia",
	"nic", "niu", "nl",  "nn",  "no",  "non", "nr",  "nso",
	"nub", "nv",  "ny",  "nym", "nyn", "nyo", "nzi", "oc",
	"oji", "om",  "or",  "os",  "osa", "ota", "oto", "pa",
	"paa", "pag", "pal", "pam", "pap", "pau", "peo", "phi",
	"phn", "pi",  "pl",  "pon", "pra", "pro", "ps",  "pt",
	"qu",  "raj", "rap", "rar", "rm",  "rn",  "ro",  "roa",
	"rom", "ru",  "rw",  "sa",  "sad", "sah", "sai", "sal",
	"sam", "sas", "sat", "sc",  "sco", "sd",  "se",  "sel",
	"sem", "sg",  "sga", "sgn", "shn", "si",  "sid", "sio",
	"sit", "sk",  "sl",  "sla", "sm",  "smi", "sn",  "snk",
	"so",  "sog", "son", "sq",  "sr",  "srr", "ss",  "ssa",
	"st",  "su",  "suk", "sus", "sux", "sv",  "sw",  "syr",
	"ta",  "tai", "te",  "tem", "ter", "tet", "tg",  "th",
	"ti",  "tig", "tiv", "tk",  "tkl", "tl",  "tli", "tmh",
	"tn",  "to",  "tog", "tpi", "tr",  "ts",  "tsi", "tt",
	"tum", "tut", "tvl", "tw",  "ty",  "tyv", "ug",  "uga",
	"uk",  "umb", "und", "ur",  "uz",  "vai", "ven", "vi",
	"vo",  "vot", "wak", "wal", "war", "was", "wen", "wo",
	"xh",  "yao", "yap", "yi",  "yo",  "ypk", "za",  "zap",
	"zen", "zh",  "znd", "zu",  "zun", 
NULL,
	"in",  "iw",  "ji",  "jw",  "sh",    /* obsolete language codes */
NULL
};

/* This list MUST be in sorted order, and MUST contain the two-letter codes
if one exists otherwise use the three letter code */

static const char * const _languages3[] = {
/*	"aa",  "ab",  "ace", "ach", "ada", "ae",  "af",  "afa",    */
	"aar", "abk", "ace", "ach", "ada", "ave", "afr", "afa",
/*	"afh", "aka", "akk", "ale", "alg", "am",  "ang", "apa",    */
	"afh", "aka", "akk", "ale", "alg", "amh", "ang", "apa",
/*	"ar",  "arc", "arn", "arp", "art", "arw", "as",  "ath",    */
	"ara", "arc", "arn", "arp", "art", "arw", "asm", "ath",
/*	"aus", "ava", "awa", "ay",  "az",  "ba",  "bad", "bai",    */
	"aus", "ava", "awa", "aym", "aze", "bak", "bad", "bai",
/*	"bal", "bam", "ban", "bas", "bat", "be",  "bej", "bem",    */
	"bal", "bam", "ban", "bas", "bat", "bel", "bej", "bem",
/*	"ber", "bg",  "bh",  "bho", "bi",  "bik", "bin", "bla",    */
	"ber", "bul", "bih", "bho", "bis", "bik", "bin", "bla",
/*	"bn",  "bnt", "bo",  "br",  "bra", "bs",  "btk", "bua",    */
	"ben", "bnt", "bod", "bre", "bra", "bos", "btk", "bua",
/*	"bug", "ca",  "cad", "cai", "car", "cau", "ce",  "ceb",    */
	"bug", "cat", "cad", "cai", "car", "cau", "che", "ceb",
/*	"cel", "ch",  "chb", "chg", "chk", "chm", "chn", "cho",    */
	"cel", "cha", "chb", "chg", "chk", "chm", "chn", "cho",
/*	"chp", "chr", "chy", "cmc", "co",  "cop", "cpe", "cpf",    */
	"chp", "chr", "chy", "cmc", "cos", "cop", "cpe", "cpf",
/*	"cpp", "cre", "crp", "cs",  "cu",  "cus", "cv",  "cy",     */
	"cpp", "cre", "crp", "ces", "chu", "cus", "chv", "cym",
/*	"da",  "dak", "day", "de",  "del", "den", "dgr", "din",    */
	"dan", "dak", "day", "deu", "del", "den", "dgr", "din",
/*	"div", "doi", "dra", "dua", "dum", "dyu", "dz",  "efi",    */
	"div", "doi", "dra", "dua", "dum", "dyu", "dzo", "efi",
/*	"egy", "eka", "el",  "elx", "en",  "enm", "eo",  "es",     */
	"egy", "eka", "ell", "elx", "eng", "enm", "epo", "spa",
/*	"et",  "eu",  "ewe", "ewo", "fa",  "fan", "fat", "fi",     */
	"est", "eus", "ewe", "ewo", "fas", "fan", "fat", "fin",
/*	"fiu", "fj",  "fo",  "fon", "fr",  "frm", "fro", "ful",    */
	"fiu", "fij", "fao", "fon", "fra", "frm", "fro", "ful",
/*	"fur", "fy",  "ga",  "gaa", "gay", "gba", "gd",  "gem",    */
	"fur", "fry", "gle", "gaa", "gay", "gba", "gla", "gem",
/*	"gez", "gil", "gl",  "gmh", "gn",  "goh", "gon", "gor",    */
	"gez", "gil", "glg", "gmh", "grn", "goh", "gon", "gor",
/*	"got", "grb", "grc", "gu",  "gv",  "gwi", "ha",  "hai",    */
	"got", "grb", "grc", "guj", "glv", "gwi", "hau", "hai",
/*	"haw", "he",  "hi",  "hil", "him", "hit", "hmn", "ho",     */
	"haw", "heb", "hin", "hil", "him", "hit", "hmn", "hmo",
/*	"hr",  "hu",  "hup", "hy",  "hz",  "ia",  "iba", "ibo",    */
	"hrv", "hun", "hup", "hye", "her", "ina", "iba", "ibo",
/*	"id",  "ie",  "ijo", "ik",  "ilo", "inc", "ine", "ira",    */
	"ind", "ile", "ijo", "ipk", "ilo", "inc", "ine", "ira",
/*	"iro", "is",  "it",  "iu",  "ja",  "jpr", "jrb", "jv",     */
	"iro", "isl", "ita", "iku", "jpn", "jpr", "jrb", "jaw",
/*	"ka",  "kaa", "kab", "kac", "kam", "kar", "kau", "kaw",    */
	"kat", "kaa", "kab", "kac", "kam", "kar", "kau", "kaw",
/*	"kha", "khi", "kho", "ki",  "kj",  "kk",  "kl",  "km",     */
	"kha", "khi", "kho", "kik", "kua", "kaz", "kal", "khm",
/*	"kmb", "kn",  "ko",  "kok", "kon", "kos", "kpe", "kro",    */
	"kmb", "kan", "kor", "kok", "kon", "kos", "kpe", "kro",
/*	"kru", "ks",  "ku",  "kum", "kut", "kv",  "kw",  "ky",     */
	"kru", "kas", "kur", "kum", "kut", "kom", "cor", "kir",
/*	"la",  "lad", "lah", "lam", "lb",  "lez", "ln",  "lo",     */
	"lat", "lad", "lah", "lam", "ltz", "lez", "lin", "lao",
/*	"lol", "loz", "lt",  "lua", "lub", "lug", "lui", "lun",    */
	"lol", "loz", "lit", "lua", "lub", "lug", "lui", "lun",
/*	"luo", "lus", "lv",  "mad", "mag", "mai", "mak", "man",    */
	"luo", "lus", "lav", "mad", "mag", "mai", "mak", "man",
/*	"map", "mas", "mdr", "men", "mg",  "mga", "mh",  "mi",     */
	"map", "mas", "mdr", "men", "mlg", "mga", "mah", "mri",
/*	"mic", "min", "mis", "mk",  "mkh", "ml",  "mn",  "mnc",    */
	"mic", "min", "mis", "mkd", "mkh", "mal", "mon", "mnc",
/*	"mni", "mno", "mo",  "moh", "mos", "mr",  "ms",  "mt",     */
	"mni", "mno", "mol", "moh", "mos", "mar", "msa", "mlt",
/*	"mul", "mun", "mus", "mwr", "my",  "myn", "na",  "nah",    */
	"mul", "mun", "mus", "mwr", "mya", "myn", "nau", "nah",
/*	"nai", "nb",  "nd",  "nds", "ne",  "new", "ng",  "nia",    */
	"nai", "nob", "nde", "nds", "nep", "new", "ndo", "nia",
/*	"nic", "niu", "nl",  "nn",  "no",  "non", "nr",  "nso",    */
	"nic", "niu", "nld", "nno", "nor", "non", "nbl", "nso",
/*	"nub", "nv",  "ny",  "nym", "nyn", "nyo", "nzi", "oc",     */
	"nub", "nav", "nya", "nym", "nyn", "nyo", "nzi", "oci",
/*	"oji", "om",  "or",  "os",  "osa", "ota", "oto", "pa",     */
	"oji", "orm", "ori", "oss", "osa", "ota", "oto", "pan",
/*	"paa", "pag", "pal", "pam", "pap", "pau", "peo", "phi",    */
	"paa", "pag", "pal", "pam", "pap", "pau", "peo", "phi",
/*	"phn", "pi",  "pl",  "pon", "pra", "pro", "ps",  "pt",     */
	"phn", "pli", "pol", "pon", "pra", "pro", "pus", "por",
/*	"qu",  "raj", "rap", "rar", "rm",  "rn",  "ro",  "roa",    */
	"que", "raj", "rap", "rar", "roh", "run", "ron", "roa",
/*	"rom", "ru",  "rw",  "sa",  "sad", "sah", "sai", "sal",    */
	"rom", "rus", "kin", "san", "sad", "sah", "sai", "sal",
/*	"sam", "sas", "sat", "sc",  "sco", "sd",  "se",  "sel",    */
	"sam", "sas", "sat", "srd", "sco", "snd", "sme", "sel",
/*	"sem", "sg",  "sga", "sgn", "shn", "si",  "sid", "sio",    */
	"sem", "sag", "sga", "sgn", "shn", "sin", "sid", "sio",
/*	"sit", "sk",  "sl",  "sla", "sm",  "smi", "sn",  "snk",    */
	"sit", "slk", "slv", "sla", "smo", "smi", "sna", "snk",
/*	"so",  "sog", "son", "sq",  "sr",  "srr", "ss",  "ssa",    */
	"som", "sog", "son", "sqi", "srp", "srr", "ssw", "ssa",
/*	"st",  "su",  "suk", "sus", "sux", "sv",  "sw",  "syr",    */
	"sot", "sun", "suk", "sus", "sux", "swe", "swa", "syr",
/*	"ta",  "tai", "te",  "tem", "ter", "tet", "tg",  "th",     */
	"tam", "tai", "tel", "tem", "ter", "tet", "tgk", "tha",
/*	"ti",  "tig", "tiv", "tk",  "tkl", "tl",  "tli", "tmh",    */
	"tir", "tig", "tiv", "tuk", "tkl", "tgl", "tli", "tmh",
/*	"tn",  "to",  "tog", "tpi", "tr",  "ts",  "tsi", "tt",     */
	"tsn", "ton", "tog", "tpi", "tur", "tso", "tsi", "tat",
/*	"tum", "tut", "tvl", "tw",  "ty",  "tyv", "ug",  "uga",    */
	"tum", "tut", "tvl", "twi", "tah", "tyv", "uig", "uga",
/*	"uk",  "umb", "und", "ur",  "uz",  "vai", "ven", "vi",     */
	"ukr", "umb", "und", "urd", "uzb", "vai", "ven", "vie",
/*	"vo",  "vot", "wak", "wal", "war", "was", "wen", "wo",     */
	"vol", "vot", "wak", "wal", "war", "was", "wen", "wol",
/*	"xh",  "yao", "yap", "yi",  "yo",  "ypk", "za",  "zap",    */
	"xho", "yao", "yap", "yid", "yor", "ypk", "zha", "zap",
/*	"zen", "zh",  "znd", "zu",  "zun",                         */
	"zen", "zho", "znd", "zul", "zun",  
NULL,
/*	"in",  "iw",  "ji",  "jw",  "sh",                          */
	"ind", "heb", "yid", "jaw", "srp",
NULL
};
/* This list MUST contain a three-letter code for every two-letter code in the
   list above, and they MUST ne in the same order (i.e., the same language must
   be in the same place in both lists)! */

/* ZR(ZAR) is now CD(COD) and FX(FXX) is PS(PSE) as per
 http://www.evertype.com/standards/iso3166/iso3166-1-en.html 
 added new codes keeping the old ones for compatibility
 updated to include 1999/12/03 revisions *CWB*/

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
	"TK",  "TM",  "TN",  "TO",  "TP",  "TR",  "TT",  "TV",
	"TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",
	"VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",
	"WS",  "YE",  "YT",  "YU",  "ZA",  "ZM",  "ZW",  
NULL,
	"FX",  "ZR",   /* obsolete country codes */
NULL
};
/* This list MUST be in sorted order, and MUST contain only two-letter codes! */

static const char * const _countries3[] = {
/*	"AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",  "AN",     */
	"AND", "ARE", "AFG", "ATG", "AIA", "ALB", "ARM", "ANT",
/*	"AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AZ",     */
	"AGO", "ATA", "ARG", "ASM", "AUT", "AUS", "ABW", "AZE",
/*	"BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",     */
	"BIH", "BRB", "BGD", "BEL", "BFA", "BGR", "BHR", "BDI",
/*	"BJ",  "BM",  "BN",  "BO",  "BR",  "BS",  "BT",  "BV",     */
	"BEN", "BMU", "BRN", "BOL", "BRA", "BHS", "BTN", "BVT",
/*	"BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",     */
	"BWA", "BLR", "BLZ", "CAN", "CCK", "COD", "CAF", "COG",
/*	"CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",     */
	"CHE", "CIV", "COK", "CHL", "CMR", "CHN", "COL", "CRI",
/*	"CU",  "CV",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",     */
	"CUB", "CPV", "CXR", "CYP", "CZE", "DEU", "DJI", "DNK",
/*	"DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",     */
	"DMA", "DOM", "DZA", "ECU", "EST", "EGY", "ESH", "ERI",
/*	"ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",     */
	"ESP", "ETH", "FIN", "FJI", "FLK", "FSM", "FRO", "FRA",
/*	"GA",  "GB",  "GD",  "GE",  "GF",  "GH",  "GI",  "GL",     */
	"GAB", "GBR", "GRD", "GEO", "GUF", "GHA", "GIB", "GRL",
/*	"GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",     */
	"GMB", "GIN", "GLP", "GNQ", "GRC", "SGS", "GTM", "GUM",
/*	"GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",     */
	"GNB", "GUY", "HKG", "HMD", "HND", "HRV", "HTI", "HUN",
/*	"ID",  "IE",  "IL",  "IN",  "IO",  "IQ",  "IR",  "IS",     */
	"IDN", "IRL", "ISR", "IND", "IOT", "IRQ", "IRN", "ISL",
/*	"IT",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",     */
	"ITA", "JAM", "JOR", "JPN", "KEN", "KGZ", "KHM", "KIR",
/*	"KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",     */
	"COM", "KNA", "PRK", "KOR", "KWT", "CYM", "KAZ", "LAO",
/*	"LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",     */
	"LBN", "LCA", "LIE", "LKA", "LBR", "LSO", "LTU", "LUX",
/*	"LV",  "LY",  "MA",  "MC",  "MD",  "MG",  "MH",  "MK",     */
	"LVA", "LBY", "MAR", "MCO", "MDA", "MDG", "MHL", "MKD",
/*	"ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",     */
	"MLI", "MMR", "MNG", "MAC", "MNP", "MTQ", "MRT", "MSR",
/*	"MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",     */
	"MLT", "MUS", "MDV", "MWI", "MEX", "MYS", "MOZ", "NAM",
/*	"NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",     */
	"NCL", "NER", "NFK", "NGA", "NIC", "NLD", "NOR", "NPL",
/*	"NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",     */
	"NRU", "NIU", "NZL", "OMN", "PAN", "PER", "PYF", "PNG",
/*	"PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",     */
	"PHL", "PAK", "POL", "SPM", "PCN", "PRI", "PSE", "PRT",
/*	"PW",  "PY",  "QA",  "RE",  "RO",  "RU",  "RW",  "SA",     */
	"PLW", "PRY", "QAT", "REU", "ROM", "RUS", "RWA", "SAU",
/*	"SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",     */
	"SLB", "SYC", "SDN", "SWE", "SGP", "SHN", "SVN", "SJM",
/*	"SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "ST",  "SV",     */
	"SVK", "SLE", "SMR", "SEN", "SOM", "SUR", "STP", "SLV",
/*	"SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",     */
	"SYR", "SWZ", "TCA", "TCD", "ATF", "TGO", "THA", "TJK",
/*	"TK",  "TM",  "TN",  "TO",  "TP",  "TR",  "TT",  "TV",     */
	"TKL", "TKM", "TUN", "TON", "TMP", "TUR", "TTO", "TUV",
/*	"TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",     */
	"TWN", "TZA", "UKR", "UGA", "UMI", "USA", "URY", "UZB",
/*	"VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",     */
	"VAT", "VCT", "VEN", "VGB", "VIR", "VNM", "VUT", "WLF",
/*	"WS",  "YE",  "YT",  "YU",  "ZA",  "ZM",  "ZW",            */
	"WSM", "YEM", "MYT", "YUG", "ZAF", "ZMB", "ZWE",
NULL,
/*	"FX",  "ZR",   */
	"FXX", "ZAR",
NULL
};
/* This list MUST contain a three-letter code for every two-letter code in
   the above list, and they MUST be listed in the same order! */

/*******************************************************************************
  Implementation function definitions
*******************************************************************************/

/*Lazy evaluated the list of installed locales*/
static void _lazyEvaluate_installedLocales(void);

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



/*******************************************************************************
  API function definitions
*******************************************************************************/


/*Works like strchr with '_' pr '-'*/
static const char* _findCharSeparator(const char* string)
{
    if (string != NULL) {
        /*Keeps iterating until an ID separator is found*/
        while (*string) {
            if (_isIDSeparator(*string)) {
                return string;
            }
            string++;
        }
    }
    return NULL;
}


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

const char* uloc_getDefault()
{
    return locale_get_default();
}

void uloc_setDefault(const char*   newDefaultLocale,
             UErrorCode* err) 
{
    if (U_FAILURE(*err))
        return;
    /* the error code isn't currently used for anything by this function*/
    
    if (newDefaultLocale == NULL) 
    {
        newDefaultLocale = uprv_getDefaultLocaleID();
    }
    
    /* propagate change to C++ */
    locale_set_default(newDefaultLocale);
}


int32_t uloc_getParent(const char*    localeID,
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
    char lang[5]; /* temporary buffer to hold language code for searching */

    uprv_memset(lang,0,4);

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
	if(i < 3)
	{
	  lang[i] = (char)uprv_tolower(*localeID);
	}
        i++;
        localeID++;
    }

    if(i == 3)
    {
      /* convert 3 character code to 2 character code if possible *CWB*/
      offset = _findIndex(_languages3, lang);
      if(offset >= 0)
      {
	if(languageCapacity >= 2)
	  uprv_strncpy(language, _languages[offset], languageCapacity);
	i = strlen(_languages[offset]);
      }
    }

    if(pEnd!=NULL) {
        *pEnd=localeID;
    }
    return i;
}

int32_t
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err)
{
	/* uloc_getLanguage will return a 2 character iso-639
		code if one exists. *CWB*/
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
    char cnty[4];
    int32_t offset;

    uprv_memset(cnty,0,4);
    /* copy the country as far as possible and count its length */
    while(!_isTerminator(*localeID) && !_isIDSeparator(*localeID)) {
        if(i<countryCapacity) {
            country[i]=(char)uprv_toupper(*localeID);
        }
        if (i < 3)		/*CWB*/
	{
	  cnty[i] = (char)uprv_toupper(*localeID);
	}
        i++;
        localeID++;
    }

    /* convert 3 character code to 2 character code if possible *CWB*/
    if (i == 3)
    {
	offset = _findIndex(_countries3,cnty);
	if (offset >= 0) 
	{
	  if (countryCapacity >= 2)
	    uprv_strncpy(country, _countries[offset],countryCapacity);
	     i = uprv_strlen(_countries[offset]);
	}
    }

    if(pEnd!=NULL) {
        *pEnd=localeID;
    }
    return i;
}

int32_t uloc_getCountry(const char* localeID,
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

int32_t uloc_getVariant(const char* localeID,
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

int32_t uloc_getName(const char* localeID,
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
       
const char* uloc_getISO3Language(const char* localeID) 
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

const char* uloc_getISO3Country(const char* localeID) 
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

uint32_t uloc_getLCID(const char* localeID) 
{
    UErrorCode err = U_ZERO_ERROR;
    char temp[30];
    const UChar* lcid = NULL;
    int32_t lcidLen = 0;
    uint32_t result = 0;
    UResourceBundle* bundle = ures_open(NULL, localeID, &err);
    
    if (U_SUCCESS(err))
    {
        lcid = ures_getStringByKey(bundle, _kLocaleID, &lcidLen, &err);
        ures_close(bundle);
        if (U_FAILURE(err) || !lcid || lcidLen == 0)
        {
            return 0;
        }
        u_UCharsToChars(lcid, temp, lcidLen + 1);
        result = (uint32_t)T_CString_stringToInteger(temp, 16);
    }
    
    return result;
}

static UBool
_startsWith(const char *s, const char *possiblePrefix) {
    while(*possiblePrefix!=0) {
        if(*possiblePrefix!=*s) {
            return FALSE;
        }
        ++s;
        ++possiblePrefix;
    }
    return TRUE;
}

/*
 * TODO check fallback semantics - fall back through default??
 * Needs discussion!
 *
 * Regular resource bundle lookup falls back through default only when a bundle
 * is opened. Once open, the lookup for an item in that bundle follows only
 * the bundle's chain, without going through default.
 *
 * This lookup for the display strings does go through the default locale
 * for the sub-item.
 * It seems to be inconsistent with how the resource bundle mechanism is documented.
 *
 * Note also that using this mechanism (with itemKey=NULL) for the variant's
 * display string, which is a top-level item and should always be available
 * at least in root, is effectively the same as opening the displayLocale's
 * bundle and getting the string directly from there - the fallback is the same
 * (right?!).
 * So, for variant's strings, one could either do that, or if the above elaborate
 * mechanism is desired, one could move variant display strings into their
 * own table "Variants" like the Languages and Countries tables.
 */
static UResourceBundle *
_res_getTableItemWithFallback(const char *path, const char *locale,
                              const char *tableKey, const char *itemKey,
                              UResourceBundle **pMainRB,
                              UErrorCode *pErrorCode) {
    char localeBuffer[200];
    UResourceBundle *rb, *table, *item;
    const char *defaultLocale;
    UBool lookedAtDefault;

    *pMainRB=NULL;
    lookedAtDefault=FALSE;
    defaultLocale=uloc_getDefault();

    /* normalize the input locale name */
    if(locale==NULL) {
        locale=defaultLocale;
        lookedAtDefault=TRUE;
    } else {
        uloc_getName(locale, localeBuffer, sizeof(localeBuffer), pErrorCode);
        if(U_FAILURE(*pErrorCode) || *pErrorCode==U_STRING_NOT_TERMINATED_WARNING) {
            *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return NULL;
        }
        locale=localeBuffer;

        /* is the requested locale the root locale, or part of the default locale? */
        if(*locale==0 || 0==uprv_strcmp(locale, "root") || _startsWith(defaultLocale, locale)) {
            lookedAtDefault=TRUE;
        }
    }

    for(;;) {
        /*
         * open the bundle for the current locale
         * this falls back through the locale's chain to the default locale's chain to root
         */
        *pErrorCode=U_ZERO_ERROR;
        rb=ures_open(path, locale, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return NULL;
        } else if(*pErrorCode==U_USING_DEFAULT_WARNING) {
            lookedAtDefault=TRUE;
        }

        /* get the real locale ID for this bundle in case of aliases & fallbacks */
        locale=ures_getLocale(rb, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            /* error getting the locale ID for an open RB - should never happen */
            ures_close(rb);
            return NULL;
        }
        if(!lookedAtDefault && _startsWith(defaultLocale, locale)) {
            lookedAtDefault=TRUE;
        }

        /*
         * try to open the requested table
         * this falls back through the locale's chain to root, but not through the default locale
         */
        *pErrorCode=U_ZERO_ERROR;
        table=ures_getByKey(rb, tableKey, NULL, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            /* no such table anywhere in this fallback chain */
            ures_close(rb);
            if(lookedAtDefault) {
                return NULL;
            }

            /* try fallback through the default locale */
            locale=defaultLocale;
            lookedAtDefault=TRUE;
            continue;
        }

        /*
         * Disable (#if 0) the following check:
         * Assume that only the language that is the same as the root language does not
         * have its own override of this item.
         * Therefore, we _do_ want to use the item even if it is from root before default,
         * because for the languages where this happens, it is exactly what we need.
         * Markus Scherer 2001-oct-02
         */
#if 0
        /* do not use the root bundle if we did not look at the default locale yet */
        if(*pErrorCode==U_USING_DEFAULT_WARNING && !lookedAtDefault) {
            ures_close(table);
            ures_close(rb);

            /* try fallback through the default locale */
            locale=defaultLocale;
            lookedAtDefault=TRUE;
            continue;
        }
#endif

        /* get the real locale ID for this table in case of aliases & fallbacks */
        locale=ures_getLocale(table, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            /* error getting the locale ID for an open RB - should never happen */
            ures_close(table);
            ures_close(rb);
            return NULL;
        }

        if(itemKey!=NULL) {
            /* try to open the requested item in the table */
            item=ures_getByKey(table, itemKey, NULL, pErrorCode);
            ures_close(table); /* we will not need the table any more */
            if(U_SUCCESS(*pErrorCode)) {
                /* we got the requested item! */
                *pMainRB=rb;
                return item;
            }
        } else {
            /* return the "table" resource itself, not an item from it */
            *pMainRB=rb;
            return table;
        }

        ures_close(rb);
        if(lookedAtDefault && (*locale==0 || 0==uprv_strcmp(locale, "root"))) {
            /* end of fallback, default and root do not have the requested item either */
            return NULL;
        }

        /* could not find the table, or its item, try to fall back to a different RB and table */
        *pErrorCode=U_ZERO_ERROR;
        uloc_getParent(locale, localeBuffer, sizeof(localeBuffer), pErrorCode);
        if(U_FAILURE(*pErrorCode) || *pErrorCode==U_STRING_NOT_TERMINATED_WARNING) {
            *pErrorCode=U_INTERNAL_PROGRAM_ERROR;
            return NULL;
        }
        locale=localeBuffer;

        /* parent==root? try the default locale if not done so already */
        if(!lookedAtDefault && (*locale==0 || 0==uprv_strcmp(locale, "root"))) {
            /* try fallback through the default locale */
            locale=defaultLocale;
            lookedAtDefault=TRUE;
        }
    }
}

static int32_t
_getStringOrCopyKey(const char *path, const char *locale,
                    const char *tableKey, const char *itemKey,
                    const char *substitute,
                    UChar *dest, int32_t destCapacity,
                    UErrorCode *pErrorCode) {
    UResourceBundle *rb, *item;
    const UChar *s;
    int32_t length;

    length=-1;
    item=_res_getTableItemWithFallback(path, locale,
                                       tableKey, itemKey,
                                       &rb,
                                       pErrorCode);
    if(U_SUCCESS(*pErrorCode)) {
        s=ures_getString(item, &length, pErrorCode);
        if(U_SUCCESS(*pErrorCode)) {
            int32_t copyLength=uprv_min(length, destCapacity);
            if(copyLength>0) {
                u_memcpy(dest, s, copyLength);
            }
        } else {
            length=-1;
        }
        ures_close(item);
        ures_close(rb);
    }

    /* no string from a resource bundle: convert the substitute */
    if(length==-1) {
        length=uprv_strlen(substitute);
        u_charsToUChars(substitute, dest, uprv_min(length, destCapacity));
        *pErrorCode=U_ZERO_ERROR;
    }

    return u_terminateUChars(dest, destCapacity, length, pErrorCode);
}

int32_t
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

int32_t
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
int32_t
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

int32_t
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

const char*
uloc_getAvailable(int32_t offset) 
{
    
    if (_installedLocales == NULL)
        _lazyEvaluate_installedLocales();
    
    if (offset > _installedLocalesCount)
        return NULL;
    else
        return _installedLocales[offset];

}

int32_t uloc_countAvailable()
{
    if (_installedLocales == NULL)
        _lazyEvaluate_installedLocales();

    return _installedLocalesCount;
}

UBool uloc_cleanup(void) {
    char ** temp;
    int32_t localeCount;
    int32_t i;

    if (_installedLocales) {
        temp = _installedLocales;
        _installedLocales = NULL;

        localeCount = _installedLocalesCount;
        _installedLocalesCount = 0;

        for (i = 0; i < localeCount; i++) {
            uprv_free(temp[i]);
        }
        uprv_free(temp);
    }
    return TRUE;
}

static void _lazyEvaluate_installedLocales()
{
    UResourceBundle *index = NULL;
    UResourceBundle installed;
    UErrorCode status = U_ZERO_ERROR;
    const UChar *lname;
    char ** temp;
    int32_t i = 0;
    int32_t len = 0;
    int32_t localeCount;
    
    ures_setIsStackObject(&installed, TRUE);
    index = ures_openDirect(NULL, _kIndexLocaleName, &status);
    ures_getByKey(index, _kIndexTag, &installed, &status);
    
    if(U_SUCCESS(status)) {
        localeCount = ures_getSize(&installed);
        temp = (char **) uprv_malloc(sizeof(char*) * (localeCount+1));

        ures_resetIterator(&installed);
        while(ures_hasNext(&installed)) {
            lname = ures_getNextString(&installed, &len, NULL, &status);
            temp[i] = (char*) uprv_malloc(sizeof(char) * (len + 1));
            
            u_UCharsToChars(lname, temp[i], len);
            temp[i][len] = 0; /* Terminate the string */
            i++;
        }

        umtx_lock(NULL);
        if (_installedLocales == NULL)
        {
            _installedLocales = temp;
            _installedLocalesCount = localeCount;
        } else {
            for (i = 0; i < localeCount; i++) {
                uprv_free(temp[i]);
            }
            uprv_free(temp);
        }
        umtx_unlock(NULL);

        ures_close(&installed);
    }
    ures_close(index);
}

/**
 * Returns a list of all language codes defined in ISO 639.  This is a pointer
 * to an array of pointers to arrays of char.  All of these pointers are owned
 * by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
const char* const* uloc_getISOLanguages() 
{
    return _languages;
}

/**
 * Returns a list of all 2-letter country codes defined in ISO 639.  This is a
 * pointer to an array of pointers to arrays of char.  All of these pointers are
 * owned by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
const char* const* uloc_getISOCountries() 
{
    return _countries;
}
