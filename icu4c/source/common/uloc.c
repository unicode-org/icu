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


static const char * const _languages[] = {
"aa","ab","af","am","ar","as","ay","az",
"ba","be","bg","bh","bi","bn","bo","br",
"ca","co","cs","cy","da","de","dz",
"el","en","eo","es","et","eu","fa","fi","fj","fo","fr","fy",
"ga","gd","gl","gn","gu","ha","he","hi","hr","hu","hy",
"ia","id","ie","ik","in","is","it","iu","iw",
"ja","ji","jw","ka","kk","kl","km","kn","ko","ks","ku","ky",
"la","ln","lo","lt","lv",
"mg","mi","mk","ml","mn","mo","mr","ms","mt","my",
"na","ne","nl","no","oc","om","or",
"pa","pl","ps","pt","qu","rm","rn","ro","ru","rw",
"sa","sd","sg","sh","si","sk","sl","sm","sn","so","sq","sr","ss","st","su","sv","sw",
"ta","te","tg","th","ti","tk","tl","tn","to","tr","ts","tt","tw",
"ug","uk","ur","uz","vi","vo","wo","xh","yi","yo","za","zh","zu",
NULL
};
/* This list MUST be in sorted order, and MUST contain only two-letter codes! */

static const char * const _languages3[] = {
"aar","abk","afr","amh","ara","asm","aym","aze",
"bak","bel","bul","bih","bis","ben","bod","bre",
"cat","cos","ces","cym","dan","deu","dzo",
"ell","eng","epo","spa","est","eus","fas","fin","fij","fao","fra","fry",
"gai","gdh","glg","grn","guj","hau","heb","hin","hrv","hun","hye",
"ina","ind","ile","ipk","ind","isl","ita","iku","heb",
"jpn","yid","jaw","kat","kaz","kal","khm","kan","kor","kas","kur","kir",
"lat","lin","lao","lit","lav",
"mlg","mri","mkd","mal","mon","mol","mar","msa","mlt","mya",
"nau","nep","nld","nor","oci","orm","ori",
"pan","pol","pus","por","que","roh","run","ron","rus","kin",
"san","snd","sag","srp","sin","slk","slv","smo","sna","som","sqi","srp","ssw","sot","sun","swe","swa",
"tat","tel","tgk","tha","tir","tuk","tgl","tsn","ton","tur","tsn","tat","twi",
"uig","ukr","urd","uzb","vie","vol","wol","xho","yid","yor","zha","zho","zul",
NULL
};
/* This list MUST contain a three-letter code for every two-letter code in the
   list above, and they MUST ne in the same order (i.e., the same language must
   be in the same place in both lists)! */

static const char * const _countries[] = {
"AD","AE","AF","AG","AI","AL","AM","AN","AO","AQ","AR","AS","AT","AU","AW","AZ",
"BA","BB","BD","BE","BF","BG","BH","BI","BJ","BM","BN","BO","BR","BS","BT","BV","BW","BY","BZ",
"CA","CC","CF","CG","CH","CI","CK","CL","CM","CN","CO","CR","CU","CV","CX","CY","CZ",
"DE","DJ","DK","DM","DO","DZ","EC","EE","EG","EH","ER","ES","ET",
"FI","FJ","FK","FM","FO","FR","FX",
"GA","GB","GD","GE","GF","GH","GI","GL","GM","GN","GP","GQ","GR","GS","GT","GU","GW","GY",
"HK","HM","HN","HR","HT","HU","ID","IE","IL","IN","IO","IQ","IR","IS","IT",
"JM","JO","JP","KE","KG","KH","KI","KM","KN","KP","KR","KW","KY","KZ",
"LA","LB","LC","LI","LK","LR","LS","LT","LU","LV","LY",
"MA","MC","MD","MG","MH","MK","ML","MM","MN","MO","MP","MQ","MR","MS","MT","MU","MV","MW","MX","MY","MZ",
"NA","NC","NE","NF","NG","NI","NL","NO","NP","NR","NU","NZ","OM",
"PA","PE","PF","PG","PH","PK","PL","PM","PN","PR","PT","PW","PY","QA","RE","RO","RU","RW",
"SA","SB","SC","SD","SE","SG","SH","SI","SJ","SK","SL","SM","SN","SO","SR","ST","SV","SY","SZ",
"TC","TD","TF","TG","TH","TJ","TK","TM","TN","TO","TP","TR","TT","TV","TW","TZ",
"UA","UG","UM","US","UY","UZ","VA","VC","VE","VG","VI","VN","VU",
"WF","WS","YE","YT","YU","ZA","ZM","ZR","ZW",
NULL
};
/* This list MUST be in sorted order, and MUST contain only two-letter codes! */

static const char * const _countries3[] = {
"AND","ARE","AFG","ATG","AIA","ALB","ARM","ANT","AGO","ATA","ARG","ASM","AUT","AUS","ABW","AZE",
"BIH","BRB","BGD","BEL","BFA","BGR","BHR","BDI","BEN","BMU","BRN","BOL","BRA","BHS","BTN","BVT","BWA","BLR","BLZ",
"CAN","CCK","CAF","COG","CHE","CIV","COK","CHL","CMR","CHN","COL","CRI","CUB","CPV","CXR","CYP","CZE",
"DEU","DJI","DNK","DMA","DOM","DZA","ECU","EST","EGY","ESH","ERI","ESP","ETH",
"FIN","FJI","FLK","FSM","FRO","FRA","FXX",
"GAB","GBR","GRD","GEO","GUF","GHA","GIB","GRL","GMB","GIN","GLP","GNQ","GRC","SGS","GTM","GUM","GNB","GUY",
"HKG","HMD","HND","HRV","HTI","HUN","IDN","IRL","ISR","IND","IOT","IRQ","IRN","ISL","ITA",
"JAM","JOR","JPN","KEN","KGZ","KHM","KIR","COM","KNA","PRK","KOR","KWT","CYM","KAZ",
"LAO","LBN","LCA","LIE","LKA","LBR","LSO","LTU","LUX","LVA","LBY",
"MAR","MCO","MDA","MDG","MHL","MKD","MLI","MMR","MNG","MAC","MNP","MTQ","MRT","MSR","MLT","MUS",
"MDV","MWI","MEX","MYS","MOZ",
"NAM","NCL","NER","NFK","NGA","NIC","NLD","NOR","NPL","NRU","NIU","NZL","OMN",
"PAN","PER","PYF","PNG","PHL","PAK","POL","SPM","PCN","PRI","PRT","PLW","PRY","QAT","REU","ROM","RUS","RWA",
"SAU","SLB","SYC","SDN","SWE","SGP","SHN","SVN","SJM","SVK","SLE","SMR","SEN","SOM","SUR","STP","SLV","SYR","SWZ",
"TCA","TCD","ATF","TGO","THA","TJK","TKL","TKM","TUN","TON","TMP","TUR","TTO","TUV","TWN","TZA",
"UKR","UGA","UMI","USA","URY","UZB","VAT","VCT","VEN","VGB","VIR","VNM","VUT",
"WLF","WSM","YEM","MYT","YUG","ZAF","ZMB","ZAR","ZWE",
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
        i++;
        localeID++;
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
    int32_t i;

    if(err==NULL || U_FAILURE(*err)) {
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

    /* copy the country as far as possible and count its length */
    while(!_isTerminator(*localeID) && !_isIDSeparator(*localeID)) {
        if(i<countryCapacity) {
            country[i]=(char)uprv_toupper(*localeID);
        }
        i++;
        localeID++;
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
    index = ures_open(NULL, _kIndexLocaleName, &status);
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
