/*
 **********************************************************************
 *   Copyright (C) 1996-2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/
/*
 *
 * Provides functionality for mapping between
 * LCID and Posix IDs or ICU locale to codepage
 *
 * Note: All classes and code in this file are
 *       intended for internal use only.
 *
 * Methods of interest:
 *   unsigned long convertToLCID(const int8_t*);
 *   const int8_t* convertToPosix(unsigned long);
 *
 * Kathleen Wilson, 4/30/96
 *
 *  Date        Name        Description
 *  3/11/97     aliu        Fixed off-by-one bug in assignment operator. Added
 *                          setId() method and safety check against 
 *                          MAX_ID_LENGTH.
 * 04/23/99     stephen     Added C wrapper for convertToPosix.
 * 09/18/00     george      Removed the memory leaks.
 * 08/23/01     george      Convert to C
 */

#include "unicode/uloc.h"
#include "locmap.h"
#include "cstring.h"

static struct
{
  const char *loc;
  const char *charmap;
}
const _localeToDefaultCharmapTable [] =
{
/*
  See:         http://czyborra.com/charsets/iso8859.html
*/

/* xx_XX locales first, so they will match: */
 { "zh_CN", "gb2312" },  /* Chinese (Simplified) */
 { "zh_TW", "Big5" },    /* Chinese (Traditional) */

 { "af", "iso-8859-1" },  /* Afrikaans */
 { "ar", "iso-8859-6" },  /* Arabic */
 { "be", "iso-8859-5" },  /* Byelorussian */
 { "bg", "iso-8859-5" },  /* Bulgarian */
 { "ca", "iso-8859-1" },  /* Catalan */
 { "cs", "iso-8859-2" },  /* Czech */
 { "da", "iso-8859-1" },  /* Danish */
 { "de", "iso-8859-1" },  /* German */
 { "el", "iso-8859-7" },  /* Greek */
 { "en", "iso-8859-1" },  /* English */
 { "eo", "iso-8859-3" },  /* Esperanto */
 { "es", "iso-8859-1" },  /* Spanish */
 { "et", "iso-8859-4" },  /* Estonian  */
 { "eu", "iso-8859-1" },  /* basque */
 { "fi", "iso-8859-1" },  /* Finnish */
 { "fo", "iso-8859-1" },  /* faroese */
 { "fr", "iso-8859-1" },  /* French */
 { "ga", "iso-8859-1" },  /* Irish (Gaelic) */
 { "gd", "iso-8859-1" },  /* Scottish */
 { "he", "iso-8859-8" },  /* hebrew */
 { "hr", "iso-8859-2" },  /* Croatian */
 { "hu", "iso-8859-2" },  /* Hungarian */
 { "in", "iso-8859-1" },  /* Indonesian */
 { "is", "iso-8859-1" },  /* Icelandic */
 { "it", "iso-8859-1" },  /* Italian  */
 { "ja", "Shift_JIS"  },  /* Japanese [was: ja_JP ] */
 { "ji", "iso-8859-8" },  /* Yiddish */
 { "kl", "iso-8859-4" },  /* Greenlandic */
 { "ko", "euc-kr"     },  /* korean [was: ko_KR ] */
 { "lt", "iso-8859-4" },  /* Lithuanian */
 { "lv", "iso-8859-4" },  /* latvian (lettish) */
 { "mk", "iso-8859-5" },  /* Macedonian */
 { "mt", "iso-8859-3" },  /* Maltese  */
 { "nb", "iso-8859-1" },  /* Norwegian Bokmal */
 { "nl", "iso-8859-1" },  /* dutch */
 { "no", "iso-8859-1" },  /* Norwegian old ISO name*/
 { "nn", "iso-8859-1" },  /* Norwegian Nynorsk */
 { "pl", "iso-8859-2" },  /* Polish */
 { "pt", "iso-8859-1" },  /* Portugese */
 { "rm", "iso-8859-1" },  /* Rhaeto-romance */
 { "ro", "iso-8859-2" },  /* Romanian */
 { "ru", "iso-8859-5" },  /* Russian */
 { "sk", "iso-8859-2" },  /* Slovak  */
 { "sl", "iso-8859-2" },  /* Slovenian */
 { "sq", "iso-8859-1" },  /* albanian */
 { "sr", "iso-8859-5" },  /* Serbian */
 { "sv", "iso-8859-1" },  /* Swedish */
 { "sw", "iso-8859-1" },  /* Swahili */
 { "th", "tis-620"    },  /* Thai [windows-874] */
 { "tr", "iso-8859-9" },  /* Turkish */
 { "uk", "iso-8859-5" },  /* pre 1990 Ukranian... see: <http://czyborra.com/charsets/cyrillic.html#KOI8-U>  */
 { "zh", "Big-5"      },  /* Chinese (Traditional) */
 { NULL, NULL         }
};

U_CAPI const char* U_EXPORT2
uprv_defaultCodePageForLocale(const char *locale)
{
    int32_t i;
    int32_t locale_len;

    if (locale == NULL)
    {
        return NULL;
    }
    locale_len = (int32_t)uprv_strlen(locale);

    if(locale_len < 2)
    {
        return NULL; /* non existent. Not a complete check, but it will
                      * make sure that 'c' doesn't match catalan, etc.
                      */
    }

    for(i=0; _localeToDefaultCharmapTable[i].loc; i++)
    {
        if(uprv_strncmp(locale, _localeToDefaultCharmapTable[i].loc,
                        uprv_min(locale_len,
                                 (int32_t)uprv_strlen(_localeToDefaultCharmapTable[i].loc)))
            == 0)
        {
            return _localeToDefaultCharmapTable[i].charmap;
        }
    }

    return NULL;
}

/*
 * Note:
 * The mapping from Win32 locale ID numbers to POSIX locale strings should
 * be the faster one.
 *
 * Many LCID values come from winnt.h
 * Some also come from http://www.microsoft.com/globaldev/reference/lcid-all.mspx
 */

/*
////////////////////////////////////////////////
//
// Internal Classes for LCID <--> POSIX Mapping
//
/////////////////////////////////////////////////
*/

typedef struct ILcidPosixElement
{
    const uint32_t hostID;
    const char * const posixID;
} ILcidPosixElement;

typedef struct ILcidPosixMap
{
    const uint32_t numRegions;
    const struct ILcidPosixElement* const regionMaps;
} ILcidPosixMap;


/*
/////////////////////////////////////////////////
//
// Easy macros to make the LCID <--> POSIX Mapping
//
/////////////////////////////////////////////////
*/

/*
 The standard one language/one country mapping for LCID.
 The first element must be the language, and the following
 elements are the language with the country.
 */
#define ILCID_POSIX_ELEMENT_ARRAY(hostID, languageID, posixID) \
static const ILcidPosixElement languageID[] = { \
    {LANGUAGE_LCID(hostID), #languageID},     /* parent locale */ \
    {hostID, #posixID}, \
};

/*
 Create the map for the posixID. This macro supposes that the language string
 name is the same as the global variable name, and that the first element
 in the ILcidPosixElement is just the language.
 */
#define ILCID_POSIX_MAP(_posixID) \
    {sizeof(_posixID)/sizeof(ILcidPosixElement), _posixID}

/*
////////////////////////////////////////////
//
// Create the table of LCID to POSIX Mapping
// None of it should be dynamically created.
//
// Keep static locale variables inside the function so that
// it can be created properly during static init.
//
////////////////////////////////////////////
*/

ILCID_POSIX_ELEMENT_ARRAY(0x0436, af, af_ZA)

static const ILcidPosixElement ar[] = {
    {0x01,   "ar"},
    {0x3801, "ar_AE"},
    {0x3c01, "ar_BH"},
    {0x1401, "ar_DZ"},
    {0x0c01, "ar_EG"},
    {0x0801, "ar_IQ"},
    {0x2c01, "ar_JO"},
    {0x3401, "ar_KW"},
    {0x3001, "ar_LB"},
    {0x1001, "ar_LY"},
    {0x1801, "ar_MA"},
    {0x2001, "ar_OM"},
    {0x4001, "ar_QA"},
    {0x0401, "ar_SA"},
    {0x2801, "ar_SY"},
    {0x1c01, "ar_TN"},
    {0x2401, "ar_YE"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x044d, as, as_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x045e, am, am_ET)

static const ILcidPosixElement az[] = {
    {0x2c,   "az"},
    {0x082c, "az_AZ"},  /* Cyrillic based */
    {0x042c, "az_Latn_AZ"} /* Latin based */
};

ILCID_POSIX_ELEMENT_ARRAY(0x0423, be, be_BY)
ILCID_POSIX_ELEMENT_ARRAY(0x0402, bg, bg_BG)

static const ILcidPosixElement bn[] = {
    {0x45,   "bn"},
    {0x0845, "bn_BD"},
    {0x0445, "bn_IN"}
};


static const ILcidPosixElement bo[] = {
    {0x51,   "bo"},
    {0x0851, "bo_BT"},
    {0x0451, "bo_CN"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0403, ca, ca_ES)
ILCID_POSIX_ELEMENT_ARRAY(0x045c, chr,chr_US)

/* Declared as cs_CZ to get around compiler errors on z/OS, which defines cs as a function */
static const ILcidPosixElement cs_CZ[] = {
    {0x05,   "cs"},
    {0x0405, "cs_CZ"},
};

ILCID_POSIX_ELEMENT_ARRAY(0x0452, cy, cy_GB)
ILCID_POSIX_ELEMENT_ARRAY(0x0406, da, da_DK)

static const ILcidPosixElement de[] = {
    {0x07,   "de"},
    {0x0c07, "de_AT"},
    {0x0807, "de_CH"},
    {0x0407, "de_DE"},
    {0x1407, "de_LI"},
    {0x1007, "de_LU"},
    {0x10407,"de__PHONEBOOK"}  /*This is really de_DE_PHONEBOOK on Windows, maybe 10007*/
};

ILCID_POSIX_ELEMENT_ARRAY(0x0465, dv, dv_MV)
ILCID_POSIX_ELEMENT_ARRAY(0x0408, el, el_GR)

static const ILcidPosixElement en[] = {
    {0x09,   "en"},
    {0x0c09, "en_AU"},
    {0x2809, "en_BZ"},
    {0x1009, "en_CA"},
    {0x0809, "en_GB"},
    {0x1809, "en_IE"},
    {0x2009, "en_JM"},
    {0x1409, "en_NZ"},
    {0x3409, "en_PH"},
    {0x2C09, "en_TT"},
    {0x0409, "en_US"},
    {0x007f, "en_US_POSIX"}, /* duplicate for roundtripping */
    {0x2409, "en_VI"},  /* Virgin Islands AKA Caribbean Islands (en_CB). */
    {0x1c09, "en_ZA"},
    {0x3009, "en_ZW"},
    {0x0409, "en_AS"},  /* Alias for en_US. Leave last. */
    {0x0409, "en_GU"},  /* Alias for en_US. Leave last. */
    {0x0409, "en_MH"},  /* Alias for en_US. Leave last. */
    {0x0409, "en_MP"},  /* Alias for en_US. Leave last. */
    {0x0409, "en_UM"}   /* Alias for en_US. Leave last. */
};

static const ILcidPosixElement en_US_POSIX[] = {
    {0x007f, "en_US_POSIX"}, /* duplicate for roundtripping */
};

static const ILcidPosixElement es[] = {
    {0x0a,   "es"},
    {0x2c0a, "es_AR"},
    {0x400a, "es_BO"},
    {0x340a, "es_CL"},
    {0x240a, "es_CO"},
    {0x140a, "es_CR"},
    {0x1c0a, "es_DO"},
    {0x300a, "es_EC"},
    {0x0c0a, "es_ES"},      /*Modern sort.*/
    {0x100a, "es_GT"},
    {0x480a, "es_HN"},
    {0x080a, "es_MX"},
    {0x4c0a, "es_NI"},
    {0x180a, "es_PA"},
    {0x280a, "es_PE"},
    {0x500a, "es_PR"},
    {0x3c0a, "es_PY"},
    {0x440a, "es_SV"},
    {0x380a, "es_UY"},
    {0x200a, "es_VE"},
    {0x040a, "es__TRADITIONAL"}  /*This is really es_ES_TRADITIONAL on Windows */
};

ILCID_POSIX_ELEMENT_ARRAY(0x0425, et, et_EE)
ILCID_POSIX_ELEMENT_ARRAY(0x042d, eu, eu_ES)
ILCID_POSIX_ELEMENT_ARRAY(0x0429, fa, fa_IR)
ILCID_POSIX_ELEMENT_ARRAY(0x040b, fi, fi_FI)
ILCID_POSIX_ELEMENT_ARRAY(0x0438, fo, fo_FO)

static const ILcidPosixElement fr[] = {
    {0x0c,   "fr"},
    {0x080c, "fr_BE"},
    {0x0c0c, "fr_CA"},
    {0x240c, "fr_CD"},
    {0x100c, "fr_CH"},
    {0x300c, "fr_CI"},
    {0x2c0c, "fr_CM"},
    {0x040c, "fr_FR"},
    {0x3c0c, "fr_HT"},
    {0x140c, "fr_LU"},
    {0x380c, "fr_MA"},
    {0x180c, "fr_MC"},
    {0x340c, "fr_ML"},
    {0x200c, "fr_RE"},
    {0x280c, "fr_SN"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0462, fy, fy_NL)

/* This LCID is really two different locales.*/
static const ILcidPosixElement ga[] = {
    {0x3c,   "ga"},
    {0x3c,   "gd"},
    {0x083c, "ga_IE"},  /* Gaelic (Ireland) */
    {0x043c, "gd_GB"}   /* Gaelic (Scotland) */
};

ILCID_POSIX_ELEMENT_ARRAY(0x0456, gl, gl_ES)
ILCID_POSIX_ELEMENT_ARRAY(0x0447, gu, gu_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x0474, gn, gn_PY)
ILCID_POSIX_ELEMENT_ARRAY(0x0468, ha, ha_NG)
ILCID_POSIX_ELEMENT_ARRAY(0x0475, haw,haw_US)
ILCID_POSIX_ELEMENT_ARRAY(0x040d, he, he_IL)
ILCID_POSIX_ELEMENT_ARRAY(0x0439, hi, hi_IN)

/* This LCID is really four different locales.*/
static const ILcidPosixElement hr[] = {
    {0x1a,   "hr"},
    {0x141a, "bs_BA"},  /* Bosnian, Bosnia and Herzegovina */
    {0x141a, "bs"},     /* Bosnian */
    {0x041a, "hr_HR"},  /* Croatian*/
    {0x081a, "sh_YU"},  /* Serbo-Croatian*/
    {0x081a, "sh"},     /* It's 0x1a or 0x081a, pick one to make the test program happy. */
    {0x0c1a, "sr_YU"},  /* Serbian*/
    {0x0c1a, "sr"},     /* It's 0x1a or 0x0c1a, pick one to make the test program happy. */
};

ILCID_POSIX_ELEMENT_ARRAY(0x040e, hu, hu_HU)
ILCID_POSIX_ELEMENT_ARRAY(0x042b, hy, hy_AM)
ILCID_POSIX_ELEMENT_ARRAY(0x0421, id, id_ID)
ILCID_POSIX_ELEMENT_ARRAY(0x0470, ig, ig_NG)
ILCID_POSIX_ELEMENT_ARRAY(0x040f, is, is_IS)

static const ILcidPosixElement it[] = {
    {0x10,   "it"},
    {0x0810, "it_CH"},
    {0x0410, "it_IT"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x045d, iu, iu_CA)    /* TODO: verify country */
ILCID_POSIX_ELEMENT_ARRAY(0x040d, iw, iw_IL)    /*Left in for compatibility*/
ILCID_POSIX_ELEMENT_ARRAY(0x0411, ja, ja_JP)
ILCID_POSIX_ELEMENT_ARRAY(0x0437, ka, ka_GE)
ILCID_POSIX_ELEMENT_ARRAY(0x043f, kk, kk_KZ)
ILCID_POSIX_ELEMENT_ARRAY(0x0453, km, km_KH)
ILCID_POSIX_ELEMENT_ARRAY(0x044b, kn, kn_IN)

static const ILcidPosixElement ko[] = {
    {0x12,   "ko"},
    {0x0812, "ko_KP"},
    {0x0412, "ko_KR"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0457, kok, kok_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x0471, kr,  kr_NG)

static const ILcidPosixElement ks[] = {         /* We could add PK and CN too */
    {0x60,   "ks"},
    {0x0860, "ks_IN"},              /* Documentation doesn't mention script */
    {0x0460, "ks_Arab_IN"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0440, ky, ky_KG)   /* Kyrgyz is spoken in Kyrgyzstan */
ILCID_POSIX_ELEMENT_ARRAY(0x0476, la, la_IT)   /* TODO: Verify the country */
ILCID_POSIX_ELEMENT_ARRAY(0x0454, lo, lo_LA)
ILCID_POSIX_ELEMENT_ARRAY(0x0427, lt, lt_LT)
ILCID_POSIX_ELEMENT_ARRAY(0x0426, lv, lv_LV)
ILCID_POSIX_ELEMENT_ARRAY(0x0481, mi, mi_NZ)
ILCID_POSIX_ELEMENT_ARRAY(0x042f, mk, mk_MK)
ILCID_POSIX_ELEMENT_ARRAY(0x044c, ml, ml_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x0450, mn, mn_MN)
ILCID_POSIX_ELEMENT_ARRAY(0x0458, mni,mni_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x044e, mr, mr_IN)

static const ILcidPosixElement ms[] = {
    {0x3e,   "ms"},
    {0x083e, "ms_BN"},   /* Brunei Darussalam*/
    {0x043e, "ms_MY"}    /* Malaysia*/
};

/* The MSJDK documentation says this is maltese, but it's not supported.*/
ILCID_POSIX_ELEMENT_ARRAY(0x043a, mt, mt_MT)

static const ILcidPosixElement ne[] = {
    {0x61,   "ne"},
    {0x0861, "ne_IN"},   /* India*/
    {0x0461, "ne_NP"}    /* Nepal*/
};

static const ILcidPosixElement nl[] = {
    {0x13,   "nl"},
    {0x0813, "nl_BE"},
    {0x0413, "nl_NL"}
};

/* The "no" locale split into nb and nn.  By default in ICU, "no" is nb.*/
static const ILcidPosixElement no[] = {
    {0x14,   "nb"},     /* really nb */
    {0x0414, "nb_NO"},  /* really nb_NO. Keep first in the 414 list. */
    {0x0414, "no"},     /* really nb_NO */
    {0x0414, "no_NO"},  /* really nb_NO */
    {0x0814, "nn_NO"},  /* really nn_NO. Keep first in the 814 list.  */
    {0x0814, "nn"},     /* It's 0x14 or 0x814, pick one to make the test program happy. */
    {0x0814, "no_NO_NY"}/* really nn_NO */
};

ILCID_POSIX_ELEMENT_ARRAY(0x046c, nso,nso_ZA)   /* TODO: Verify the country */
ILCID_POSIX_ELEMENT_ARRAY(0x0472, om, om_ET)    /* TODO: Verify the country */

/* Declared as or_IN to get around compiler errors*/
static const ILcidPosixElement or_IN[] = {
    {0x48,   "or"},
    {0x0448, "or_IN"},
};

static const ILcidPosixElement pa[] = {
    {0x46,   "pa"},
    {0x0446, "pa_IN"},
    {0x0846, "pa_PK"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0415, pl, pl_PL)
ILCID_POSIX_ELEMENT_ARRAY(0x0463, ps, ps_AF)

static const ILcidPosixElement pt[] = {
    {0x16,   "pt"},
    {0x0416, "pt_BR"},
    {0x0816, "pt_PT"}
};

static const ILcidPosixElement qu[] = {
    {0x6B,   "qu"},
    {0x046B, "qu_BO"},
    {0x086B, "qu_EC"},
    {0x0C6B, "qu_PE"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0418, ro, ro_RO)

static const ILcidPosixElement root[] = {
    {0x00,   "root"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0419, ru, ru_RU)
ILCID_POSIX_ELEMENT_ARRAY(0x044f, sa, sa_IN)

static const ILcidPosixElement sd[] = {
    {0x59,   "sd"},
    {0x0459, "sd_IN"},
    {0x0859, "sd_PK"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x045b, si, si_LK)
ILCID_POSIX_ELEMENT_ARRAY(0x041b, sk, sk_SK)
ILCID_POSIX_ELEMENT_ARRAY(0x0424, sl, sl_SI)
ILCID_POSIX_ELEMENT_ARRAY(0x0477, so, so_ET)    /* TODO: Verify the country */
ILCID_POSIX_ELEMENT_ARRAY(0x041c, sq, sq_AL)

static const ILcidPosixElement sv[] = {
    {0x1d,   "sv"},
    {0x081d, "sv_FI"},
    {0x041d, "sv_SE"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0441, sw, sw_KE)
ILCID_POSIX_ELEMENT_ARRAY(0x045A, syr, syr_SY)
ILCID_POSIX_ELEMENT_ARRAY(0x0449, ta, ta_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x044a, te, te_IN)
ILCID_POSIX_ELEMENT_ARRAY(0x041e, th, th_TH)

static const ILcidPosixElement ti[] = {
    {0x73,   "ti"},
    {0x0873, "ti_ER"},
    {0x0473, "ti_ET"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0442, tk, tk_TM)
ILCID_POSIX_ELEMENT_ARRAY(0x0464, tl, tl_PH)
ILCID_POSIX_ELEMENT_ARRAY(0x0432, tn, tn_BW)
ILCID_POSIX_ELEMENT_ARRAY(0x041f, tr, tr_TR)
ILCID_POSIX_ELEMENT_ARRAY(0x0444, tt, tt_RU)
ILCID_POSIX_ELEMENT_ARRAY(0x0480, ug, ug_CN)
ILCID_POSIX_ELEMENT_ARRAY(0x0422, uk, uk_UA)

static const ILcidPosixElement ur[] = {
    {0x20,   "ur"},
    {0x0820, "ur_IN"},
    {0x0420, "ur_PK"}
};

static const ILcidPosixElement uz[] = {
    {0x43,   "uz"},
    {0x0843, "uz_UZ"},  /* Cyrillic based */
    {0x0443, "uz_Latn_UZ"} /* Latin based */
};

ILCID_POSIX_ELEMENT_ARRAY(0x0433, ve, ve_ZA)    /* TODO: Verify the country */
ILCID_POSIX_ELEMENT_ARRAY(0x042a, vi, vi_VN)
ILCID_POSIX_ELEMENT_ARRAY(0x0434, xh, xh_ZA)    /* TODO: Verify the country */
ILCID_POSIX_ELEMENT_ARRAY(0x046a, yo, yo_NG)    /* TODO: Verify the country */

/* TODO: Make the locales with the script the primary locale once the names are implemented in the resources. */
static const ILcidPosixElement zh[] = {
    {0x04,   "zh"},
    {0x0804, "zh_CN"},
    {0x0804, "zh_Hans_CN"},
    {0x0c04, "zh_HK"},
    {0x0c04, "zh_Hant_HK"},
    {0x1404, "zh_MO"},
    {0x1404, "zh_Hant_MO"},
    {0x1004, "zh_SG"},
    {0x1004, "zh_Hans_SG"},
    {0x0404, "zh_TW"},
    {0x0404, "zh_Hant_TW"},
    {0x30404,"zh_TW"},          /* Bopomofo order */
    {0x30404,"zh_Hant_TW"},
    {0x20404,"zh_TW@collation=STROKE"},
    {0x20404,"zh_TW_STROKE"},   /* remove? */
    {0x20404,"zh_Hant_TW_STROKE"}
};

ILCID_POSIX_ELEMENT_ARRAY(0x0435, zu, zu_ZA)    /* TODO: Verify the country */

/* This must be static and grouped by LCID. */

/* non-existent ISO-639 codes */
/*
0x466   Edo
0x467   Fulfulde - Nigeria
0x43b   Sami (Lappish)
0x42e   Sorbian (iso639 = dsb, hsb, wen)
0x430   Sutu
0x45f   Tamazight (Arabic script)
0x85f   Tamazight (Latin script)
0x478   Yi
*/
static const ILcidPosixMap gPosixIDmap[] = {
    ILCID_POSIX_MAP(af),    /*  af  Afrikaans                 0x36 */
    ILCID_POSIX_MAP(am),    /*  am  Amharic                   0x5e */
    ILCID_POSIX_MAP(ar),    /*  ar  Arabic                    0x01 */
    ILCID_POSIX_MAP(as),    /*  as  Assamese                  0x4d */
    ILCID_POSIX_MAP(az),    /*  az  Azerbaijani               0x2c */
    ILCID_POSIX_MAP(be),    /*  be  Belarusian                0x23 */
    ILCID_POSIX_MAP(bg),    /*  bg  Bulgarian                 0x02 */
    ILCID_POSIX_MAP(bn),    /*  bn  Bengali; Bangla           0x45 */
    ILCID_POSIX_MAP(bo),    /*  bo  Tibetan                   0x51 */
    ILCID_POSIX_MAP(ca),    /*  ca  Catalan                   0x03 */
    ILCID_POSIX_MAP(chr),   /*  chr Cherokee                  0x5c */
    ILCID_POSIX_MAP(cs_CZ), /*  cs  Czech                     0x05 */
    ILCID_POSIX_MAP(cy),    /*  cy  Welsh                     0x52 */
    ILCID_POSIX_MAP(da),    /*  da  Danish                    0x06 */
    ILCID_POSIX_MAP(de),    /*  de  German                    0x07 */
    ILCID_POSIX_MAP(dv),    /*  dv  Divehi                    0x65 */
    ILCID_POSIX_MAP(el),    /*  el  Greek                     0x08 */
    ILCID_POSIX_MAP(en),    /*  en  English                   0x09 */
    ILCID_POSIX_MAP(en_US_POSIX), /*    invariant             0x7f */
    ILCID_POSIX_MAP(es),    /*  es  Spanish                   0x0a */
    ILCID_POSIX_MAP(et),    /*  et  Estonian                  0x25 */
    ILCID_POSIX_MAP(eu),    /*  eu  Basque                    0x2d */
    ILCID_POSIX_MAP(fa),    /*  fa  Farsi                     0x29 */
    ILCID_POSIX_MAP(fi),    /*  fi  Finnish                   0x0b */
    ILCID_POSIX_MAP(fo),    /*  fo  Faroese                   0x38 */
    ILCID_POSIX_MAP(fr),    /*  fr  French                    0x0c */
    ILCID_POSIX_MAP(fy),    /*  fy  Frisian                   0x62 */
    ILCID_POSIX_MAP(ga),    /*  *   Gaelic (Ireland,Scotland) 0x3c */
    ILCID_POSIX_MAP(gl),    /*  gl  Galician                  0x56 */
    ILCID_POSIX_MAP(gn),    /*  gn  Guarani                   0x74 */
    ILCID_POSIX_MAP(gu),    /*  gu  Gujarati                  0x47 */
    ILCID_POSIX_MAP(ha),    /*  ha  Hausa                     0x68 */
    ILCID_POSIX_MAP(haw),   /*  haw Hawaiian                  0x75 */
    ILCID_POSIX_MAP(he),    /*  he  Hebrew (formerly iw)      0x0d */
    ILCID_POSIX_MAP(hi),    /*  hi  Hindi                     0x39 */
    ILCID_POSIX_MAP(hr),    /*  *   Croatian and others       0x1a */
    ILCID_POSIX_MAP(hu),    /*  hu  Hungarian                 0x0e */
    ILCID_POSIX_MAP(hy),    /*  hy  Armenian                  0x2b */
    ILCID_POSIX_MAP(id),    /*  id  Indonesian (formerly in)  0x21 */
    ILCID_POSIX_MAP(ig),    /*  ig  Igbo                      0x70 */
    ILCID_POSIX_MAP(is),    /*  is  Icelandic                 0x0f */
    ILCID_POSIX_MAP(it),    /*  it  Italian                   0x10 */
    ILCID_POSIX_MAP(iu),    /*  iu  Inuktitut                 0x5d */
    ILCID_POSIX_MAP(iw),    /*  iw  Hebrew                    0x0d */
    ILCID_POSIX_MAP(ja),    /*  ja  Japanese                  0x11 */
    ILCID_POSIX_MAP(ka),    /*  ka  Georgian                  0x37 */
    ILCID_POSIX_MAP(kk),    /*  kk  Kazakh                    0x3f */
    ILCID_POSIX_MAP(km),    /*  km  Khmer                     0x53 */
    ILCID_POSIX_MAP(kn),    /*  kn  Kannada                   0x4b */
    ILCID_POSIX_MAP(ko),    /*  ko  Korean                    0x12 */
    ILCID_POSIX_MAP(kok),   /*  kok Konkani                   0x57 */
    ILCID_POSIX_MAP(kr),    /*  kr  Kanuri                    0x71 */
    ILCID_POSIX_MAP(ks),    /*  ks  Kashmiri                  0x60 */
    ILCID_POSIX_MAP(ky),    /*  ky  Kyrgyz                    0x40 */
    ILCID_POSIX_MAP(la),    /*  la  Latin                     0x76 */
    ILCID_POSIX_MAP(lo),    /*  lo  Lao                       0x54 */
    ILCID_POSIX_MAP(lt),    /*  lt  Lithuanian                0x27 */
    ILCID_POSIX_MAP(lv),    /*  lv  Latvian, Lettish          0x26 */
    ILCID_POSIX_MAP(mi),    /*  mi  Maori                     0x81 */
    ILCID_POSIX_MAP(mk),    /*  mk  Macedonian                0x2f */
    ILCID_POSIX_MAP(ml),    /*  ml  Malayalam                 0x4c */
    ILCID_POSIX_MAP(mn),    /*  mn  Mongolian                 0x50 */
    ILCID_POSIX_MAP(mni),   /*  mni Manipuri                  0x58 */
    ILCID_POSIX_MAP(mr),    /*  mr  Marathi                   0x4e */
    ILCID_POSIX_MAP(ms),    /*  ms  Malay                     0x3e */
    ILCID_POSIX_MAP(mt),    /*  mt  Maltese                   0x3a */
/*    ILCID_POSIX_MAP(nb),    //  no  Norwegian                 0x14 */
    ILCID_POSIX_MAP(ne),    /*  ne  Nepali                    0x61 */
    ILCID_POSIX_MAP(nl),    /*  nl  Dutch                     0x13 */
/*    ILCID_POSIX_MAP(nn),    //  no  Norwegian                 0x14 */
    ILCID_POSIX_MAP(no),    /*  *   Norwegian                 0x14 */
    ILCID_POSIX_MAP(nso),   /*  nso Sotho, Northern (Sepedi dialect) 0x6c */
    ILCID_POSIX_MAP(om),    /*  om  Oromo                     0x72 */
    ILCID_POSIX_MAP(or_IN), /*  or  Oriya                     0x48 */
    ILCID_POSIX_MAP(pa),    /*  pa  Punjabi                   0x46 */
    ILCID_POSIX_MAP(pl),    /*  pl  Polish                    0x15 */
    ILCID_POSIX_MAP(ps),    /*  ps  Pashto                    0x63 */
    ILCID_POSIX_MAP(pt),    /*  pt  Portuguese                0x16 */
    ILCID_POSIX_MAP(qu),    /*  qu  Quechua (correct spelling)0x6B */
    ILCID_POSIX_MAP(ro),    /*  ro  Romanian                  0x18 */
    ILCID_POSIX_MAP(root),  /*  root                          0x00 */
    ILCID_POSIX_MAP(ru),    /*  ru  Russian                   0x19 */
    ILCID_POSIX_MAP(sa),    /*  sa  Sanskrit                  0x4f */
    ILCID_POSIX_MAP(sd),    /*  sd  Sindhi                    0x59 */
/*    ILCID_POSIX_MAP(sh),    //  sh  Serbo-Croatian            0x1a */
    ILCID_POSIX_MAP(si),    /*  si  Sinhalese                 0x5b */
    ILCID_POSIX_MAP(sk),    /*  sk  Slovak                    0x1b */
    ILCID_POSIX_MAP(sl),    /*  sl  Slovenian                 0x24 */
    ILCID_POSIX_MAP(so),    /*  so  Somali                    0x77 */
    ILCID_POSIX_MAP(sq),    /*  sq  Albanian                  0x1c */
/*    ILCID_POSIX_MAP(sr),    //  sr  Serbian                   0x1a */
    ILCID_POSIX_MAP(sv),    /*  sv  Swedish                   0x1d */
    ILCID_POSIX_MAP(sw),    /*  sw  Swahili                   0x41 */
    ILCID_POSIX_MAP(syr),   /*  syr Syriac                    0x5A */
    ILCID_POSIX_MAP(ta),    /*  ta  Tamil                     0x49 */
    ILCID_POSIX_MAP(te),    /*  te  Telugu                    0x4a */
    ILCID_POSIX_MAP(th),    /*  th  Thai                      0x1e */
    ILCID_POSIX_MAP(ti),    /*  ti  Tigrigna                  0x73 */
    ILCID_POSIX_MAP(tk),    /*  tk  Turkmen                   0x42 */
    ILCID_POSIX_MAP(tl),    /*  tl  Tagalog (Filipino)        0x64 */
    ILCID_POSIX_MAP(tn),    /*  tn  Tswana                    0x32 */
    ILCID_POSIX_MAP(tr),    /*  tr  Turkish                   0x1f */
    ILCID_POSIX_MAP(tt),    /*  tt  Tatar                     0x44 */
    ILCID_POSIX_MAP(ug),    /*  ug  Uighur                    0x80 */
    ILCID_POSIX_MAP(uk),    /*  uk  Ukrainian                 0x22 */
    ILCID_POSIX_MAP(ur),    /*  ur  Urdu                      0x20 */
    ILCID_POSIX_MAP(uz),    /*  uz  Uzbek                     0x43 */
    ILCID_POSIX_MAP(ve),    /*  ve  Venda                     0x33 */
    ILCID_POSIX_MAP(vi),    /*  vi  Vietnamese                0x2a */
    ILCID_POSIX_MAP(xh),    /*  xh  Xhosa                     0x34 */
    ILCID_POSIX_MAP(yo),    /*  yo  Yoruba                    0x6a */
    ILCID_POSIX_MAP(zh),    /*  zh  Chinese                   0x04 */
    ILCID_POSIX_MAP(zu),    /*  zu  Zulu                      0x35 */
};

static const uint32_t gLocaleCount = sizeof(gPosixIDmap)/sizeof(ILcidPosixMap);

/**
 * Do not call this function. It is called by hostID.
 * The function is not private because this struct must stay as a C struct,
 * and this is an internal class.
 */
static int32_t
idCmp(const char* id1, const char* id2)
{
    int32_t diffIdx = 0;
    while (*id1 == *id2 && *id1 != 0) {
        diffIdx++;
        id1++;
        id2++;
    }
    return diffIdx;
}

/**
 * Searches for a Windows LCID
 *
 * @param posixid the Posix style locale id.
 * @param status gets set to U_ILLEGAL_ARGUMENT_ERROR when the Posix ID has
 *               no equivalent Windows LCID.
 * @return the LCID
 */
static uint32_t
getHostID(const ILcidPosixMap *this_0, const char* posixID, UErrorCode* status)
{
    int32_t bestIdx = 0;
    int32_t bestIdxDiff = 0;
    int32_t posixIDlen = (int32_t)uprv_strlen(posixID) + 1;
    uint32_t idx;

    for (idx = 0; idx < this_0->numRegions; idx++ ) {
        int32_t sameChars = idCmp(posixID, this_0->regionMaps[idx].posixID);
        if (sameChars > bestIdxDiff && this_0->regionMaps[idx].posixID[sameChars] == 0) {
            if (posixIDlen == sameChars) {
                /* Exact match */
                return this_0->regionMaps[idx].hostID;
            }
            bestIdxDiff = sameChars;
            bestIdx = idx;
        }
    }
    if (this_0->regionMaps[bestIdx].posixID[bestIdxDiff] == 0) {
        *status = U_USING_FALLBACK_WARNING;
        return this_0->regionMaps[bestIdx].hostID;
    }

    /*no match found */
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return this_0->regionMaps->hostID;
}

static const char*
getPosixID(const ILcidPosixMap *this_0, uint32_t hostID)
{
    uint32_t i;
    for (i = 0; i <= this_0->numRegions; i++)
    {
        if (this_0->regionMaps[i].hostID == hostID)
        {
            return this_0->regionMaps[i].posixID;
        }
    }

    /* If you get here, then no matching region was found,
       so return the language id with the wild card region. */
    return this_0->regionMaps[0].posixID;
}

/*
//////////////////////////////////////
//
// LCID --> POSIX
//
/////////////////////////////////////
*/

U_CAPI const char *
uprv_convertToPosix(uint32_t hostid, UErrorCode* status)
{
    uint16_t langID = LANGUAGE_LCID(hostid);
    uint32_t index;

    for (index = 0; index < gLocaleCount; index++)
    {
        if (langID == gPosixIDmap[index].regionMaps->hostID)
        {
            return getPosixID(&gPosixIDmap[index], hostid);
        }
    }

    /* no match found */
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
}

/*
//////////////////////////////////////
//
// POSIX --> LCID
//
/////////////////////////////////////
*/

U_CAPI uint32_t
uprv_convertToLCID(const char* posixID, UErrorCode* status)
{

    uint32_t   low    = 0;
    uint32_t   high   = gLocaleCount;
    uint32_t   mid    = high;
    uint32_t   oldmid = 0;
    int32_t    compVal;
    char       langID[ULOC_FULLNAME_CAPACITY];

    uint32_t   value         = 0;
    uint32_t   fallbackValue = (uint32_t)-1;
    UErrorCode myStatus;
    uint32_t   idx;

    /* Check for incomplete id. */
    if (!posixID || uprv_strlen(posixID) < 2) {
        return 0;
    }

    uloc_getLanguage(posixID, langID, sizeof(langID), status);
    if (U_FAILURE(*status)) {
        return 0;
    }

    /*Binary search for the map entry for normal cases */

    while (high > low)  /*binary search*/{

        mid = (high+low) >> 1; /*Finds median*/

        if (mid == oldmid) 
            break;

        compVal = uprv_strcmp(langID, gPosixIDmap[mid].regionMaps->posixID);
        if (compVal < 0){
            high = mid;
        }
        else if (compVal > 0){
            low = mid;
        }
        else /*we found it*/{
            return getHostID(&gPosixIDmap[mid], posixID, status);
        }
        oldmid = mid;
    }

    /*
     * Sometimes we can't do a binary search on posixID because some LCIDs
     * go to different locales.  We hit one of those special cases.
     */
    for (idx = 0; idx < gLocaleCount; idx++ ) {
        myStatus = U_ZERO_ERROR;
        value = getHostID(&gPosixIDmap[idx], posixID, &myStatus);
        if (myStatus == U_ZERO_ERROR) {
            return value;
        }
        else if (myStatus == U_USING_FALLBACK_WARNING) {
            fallbackValue = value;
        }
    }

    if (fallbackValue != (uint32_t)-1) {
        *status = U_USING_FALLBACK_WARNING;
        return fallbackValue;
    }

    /* no match found */
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;   /* return international (root) */
}

