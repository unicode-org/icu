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


#include "unicode/uloc.h"

#include "unicode/utypes.h"
#include "uresimp.h"
#include "umutex.h"
#include "cstring.h"
#include "unicode/ustring.h"
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

#if 0
/* We don't use these resources currently */
static const char* _kLocaleString   = "LocaleString";
static const char* _kShortLanguage  = "ShortLanguage";
static const char* _kShortCountry   = "ShortCountry";
#endif

#define TEMPBUFSIZE 8

/*Some static strings needed in the getDisplay* functions*/
static const UChar openParen[]  = { 0x20, 0x28, 0}; /* " (" */
static const UChar comma[]      = { 0x2C, 0x20, 0}; /* ", " */
static const UChar closeParen[] = { 0x29, 0};       /* ")" */

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
    int i=0;
    int offset = 0;
    int count = 0;
    
    if (U_FAILURE(*err))
        return 0;
    
    if (localeID == NULL)
        localeID = uloc_getDefault();
    
    
    while (localeID[offset]&&(count < 2))
    {
        if (_isIDSeparator(localeID[offset++]))
            count++;
    }
    
    /*finds the second IDSeparator*/
    while (offset && !_isIDSeparator(localeID[offset]))
    {
        offset--;
    }
    
    
    /*Loop updates i to the size of the parent
    but only copies into the buffer as much as the buffer can bare*/
    while (i < offset)
    {
        if (parentCapacity > i)
            parent[i] = localeID[i];
        i++;
    }
    
    /*Sets the error code on case of need*/
    if (i >= parentCapacity )
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
    
    if (parentCapacity>0)
        parent[uprv_min(i,parentCapacity-1)] = '\0';
    
    return i+1;
}

int32_t
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err)
{
    int i=0;
    
    
    if (U_FAILURE(*err))
        return 0;
    
    if (localeID == NULL)
        localeID = uloc_getDefault();
    
    /* If it starts with i- or x- */
    if(_isIDPrefix(localeID))
    {
        if(languageCapacity > i)
        {
            language[i] = (char)uprv_tolower(*localeID);
        }
        i++;
        localeID++;
        
        if(languageCapacity > i)
        {
            language[i] = '-';
        }
        i++;
        localeID++;
    }
    
    /*Loop updates i to the size of the language
    but only copies into the buffer as much as the buffer can bare*/
    while (!_isTerminator(*localeID) && !_isIDSeparator(*localeID))
    {
        if (languageCapacity > i)
            language[i] = (char)uprv_tolower(*localeID);
        i++;
        localeID++;
    }
    
    if (i >= languageCapacity )
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
    
    if (languageCapacity > 0) 
    {
        language[uprv_min(i,languageCapacity-1)] = '\0';
    }
    
    return i+1;
}


int32_t uloc_getCountry(const char* localeID,
            char* country,
            int32_t countryCapacity,
            UErrorCode* err) 
{
    int i=0;
    
    if (U_FAILURE(*err))
        return 0;
    if (localeID == NULL)
        localeID = uloc_getDefault();
    
    
    /* skip over i- or x- */
    if(_isIDPrefix(localeID))
    {
        localeID += 2;
    }
    
    localeID = _findCharSeparator(localeID);
    
    /*Loop updates i to the size of the language
    but only copies into the buffer as much as the buffer can bare*/
    if (localeID)
    {
        ++localeID;
        while (!_isTerminator(*localeID) && !_isIDSeparator(*localeID))
        {
            if (countryCapacity > i)
                country[i] = (char)uprv_toupper(*localeID);
            i++;
            localeID++;
        }
    }
    
    if (i >= countryCapacity )
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
    
    if (countryCapacity > 0) {
        country[uprv_min(i,countryCapacity-1)] = '\0';
    }
    return i+1;
}

int32_t uloc_getVariant(const char* localeID,
                        char* variant,
                        int32_t variantCapacity,
                        UErrorCode* err) 
{
    int i=0;
    const char *p = localeID;
    
    if (U_FAILURE(*err))
        return 0;
    if (localeID == NULL)
    {
        localeID = uloc_getDefault();
    }
    
    /* skip over i- or x- */
    if(_isIDPrefix(localeID))
    {
        localeID += 2;
    }
    
    localeID = _findCharSeparator(localeID);
    if (localeID)
    {
        localeID = _findCharSeparator(++localeID);
    }
    
    if (localeID)
    {
        ++localeID;
        /*Loop updates i to the size of the language
        but only copies into the buffer as much as the buffer can bear*/
        while (!_isTerminator(*localeID))
        {
            if (variantCapacity > i)
                variant[i] = (char)uprv_toupper(*localeID);
            i++;
            localeID++;
        }
    }
    
    /* But wait, there's more! 
    **IFF** no variant was otherwise found, take one from @...
    */
    if ( (i == 0) &&  /* Found nothing (zero chars copied) */
        (localeID = uprv_strrchr(p, '@')) != NULL)
    {
        localeID++; /* point after the @ */
                    /* Note that we will stop at a period if the user accidentally
                       put a period after the @ sign */

        /* repeat above copying loop */
        while (!_isTerminator(*localeID))
        {
            if (variantCapacity > i)
                variant[i] = (char)uprv_toupper(*localeID);
            i++;
            localeID++;
        }
    }

    if (i >= variantCapacity )
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    if (variantCapacity>0) {
        variant[uprv_min(i,variantCapacity-1)] = '\0';
    }
    return i+1;
}

int32_t uloc_getName(const char* localeID,
             char* name,
             int32_t nameCapacity,
             UErrorCode* err)  
{
    int i= 0;       /* total required size */
    int n= 0;       /* How much has been copied currently */
    int varSze = 0; /* How big the variant is */
    int cntSze = 0; /* How big the country is */
    
    UErrorCode int_err = U_ZERO_ERROR;
    
    if (U_FAILURE(*err)) return 0;
    
    /*First we preflight the components in order to ensure a valid return value*/
    if (localeID == NULL)    localeID = uloc_getDefault();
    
    cntSze = uloc_getCountry(localeID, 
        NULL , 
        0,
        &int_err);
    int_err = U_ZERO_ERROR;
    varSze = uloc_getVariant(localeID, 
        NULL , 
        0,
        &int_err);
    
    int_err = U_ZERO_ERROR;
    i = uloc_getLanguage(localeID, 
        NULL,
        0, 
        &int_err);
    
    /*Adjust for the zero terminators*/
    --varSze;
    --cntSze;
    /* i is still languagesize+1 for the terminator */
    
    /* Add space for underscores */
    if (varSze)
    {
        i+= 2;  /* if theres a variant, it will ALWAYS contain two underscores. */
    }
    else if (cntSze)
    {
        i++; /* Otherwise - only language _ country. */
    }
    
    /* Update i (total req'd size) */
    i += cntSze + varSze;
    
    if(nameCapacity)  /* If size is zero, skip the actual copy */
    {
        /* Now, the real copying */
        int_err = U_ZERO_ERROR;
        
        uloc_getLanguage(localeID, 
            name,
            nameCapacity /* -(n=0) */,  
            &int_err);
        
        n += uprv_strlen(name);
        
        /*We fill in the users buffer*/
        if ((n<nameCapacity) && cntSze)
        {
            if(U_SUCCESS(int_err))
            {
                name[n++] = '_';
            }
            
            uloc_getCountry(localeID,
                name + n,
                nameCapacity - n,
                &int_err);
            n += cntSze;
            
            if (varSze && (n<nameCapacity))
            {
                if(U_SUCCESS(int_err))
                {
                    name[n++] = '_';
                }
                
                uloc_getVariant(localeID,
                    name + n,
                    nameCapacity - n,
                    &int_err);
            }
            
        }
        else if((n<nameCapacity) && varSze)
        {
            if (U_SUCCESS(int_err))
            {
                name[n++] = '_';
                if(n<nameCapacity)
                    name[n++] = '_';
            }
            
            uloc_getVariant(localeID,
                name + n,
                nameCapacity - n,
                &int_err);
        }
        
        /* Tie it off */
        name[uprv_min(i,nameCapacity-1)] = '\0';
    }   /* end (if nameCapacity > 0) */
    
    *err  = int_err;
    
    return i;
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
        u_austrcpy(temp, lcid);
        result = (uint32_t)T_CString_stringToInteger(temp, 16);
    }
    
    return result;
}

int32_t uloc_getDisplayLanguage(const char* locale,
                const char* inLocale,
                UChar* language,
                int32_t languageCapacity,
                UErrorCode* status) 
{
    const UChar* result = NULL;
    int32_t resultLen = 0;
    int langBufSize;
    char inLanguageBuffer[TEMPBUFSIZE];
    char inLocaleBuffer[TEMPBUFSIZE];
    UErrorCode err = U_ZERO_ERROR;
    UResourceBundle* bundle;
    UBool isDefaultLocale = FALSE;
    UBool done = FALSE;
    
    if (U_FAILURE(*status))
        return 0;
    
    if (inLocale == NULL) 
    {
        inLocale = uloc_getDefault();
        isDefaultLocale = TRUE;
    }
    else if (uprv_strcmp(inLocale, uloc_getDefault()) == 0)
    {
        isDefaultLocale = TRUE;
    }
    /*truncates the fallback mechanism if we start out with a defaultLocale*/
    
    if (locale == NULL)
        locale = uloc_getDefault();
    
    /*extracts the language*/
    langBufSize = uloc_getLanguage(locale,
        inLanguageBuffer,
        TEMPBUFSIZE,
        &err);  
    
    
    
    /*We need to implement a fallback mechanism here because we are getting keys out of a
    tagged array, there is no capability of doing this with fallback through the resource
    bundle API*/
    
    if (langBufSize > 1)
    {
        do 
        {    
            /*
            If we are at the root locale ("")
            The first time we fall back to the full default locale
            As we iterate down the latter, if we hit the root locale ("")
            we pass it to the resource bundle api so it checks default.txt
            */
            
            if (inLocale[0] == '\0')
            {
                if (!isDefaultLocale)
                {
                    isDefaultLocale = TRUE;
                    inLocale = uloc_getDefault();
                }
                else {
                    done = TRUE;
                }
            }
            
            
            bundle = ures_open(NULL, inLocale, &err);
            
            if (U_SUCCESS(err))
            {
                const UChar* temp = NULL;  
                UResourceBundle* langBundle;

                err = U_ZERO_ERROR;
                langBundle = ures_getByKey(bundle, _kLanguages, NULL, &err);
                if (U_SUCCESS(err))
                {
                    temp = ures_getStringByKey(langBundle,
                        inLanguageBuffer,
                        &resultLen,
                        &err);
                    resultLen++;
                    if (U_SUCCESS(err))
                        result = temp;
                    ures_close(langBundle);
                }
                ures_close(bundle);
            }
            
            
            err = U_ZERO_ERROR;
            
            /*Iterates down the Locale ID*/
            
            uloc_getParent(inLocale, inLocaleBuffer, TEMPBUFSIZE, &err);
            inLocale = inLocaleBuffer;
            
        } while ((result == NULL) && !done);
    }
    
    
    if (result)
    {
        if (resultLen > languageCapacity)
        {
            *status = U_BUFFER_OVERFLOW_ERROR;
            
            if (languageCapacity >= 1) 
            {
                u_strncpy(language, result, languageCapacity-1);
                language[languageCapacity-1] = (UChar)0x0000;
            }
        }
        else {
            u_strcpy(language, result);
        }
    }
    else 
    {
        /*Falls back to ISO Name*/
        resultLen = langBufSize;
        if (resultLen > languageCapacity)
        {
            *status = U_BUFFER_OVERFLOW_ERROR;
            
            if (languageCapacity >= 1) 
            {
                
                language[languageCapacity-1] = (UChar)0x0000;
                u_uastrncpy(language, inLanguageBuffer, languageCapacity-1);
            }
        }
        else u_uastrcpy(language, inLanguageBuffer);
    }
    return resultLen;
}

int32_t uloc_getDisplayCountry(const char* locale,
                   const char* inLocale,
                   UChar* country,
                   int32_t countryCapacity,
                   UErrorCode* status)
{
    /* NULL may be used to specify the default */
    const UChar* result = NULL;
    int32_t resultLen = 0;
    int cntryBufSize;
    char inCountryBuffer[TEMPBUFSIZE];
    UErrorCode err = U_ZERO_ERROR;
    UResourceBundle* bundle = NULL;
    char inLocaleBuffer[TEMPBUFSIZE];
    UBool isDefaultLocale = FALSE;
    UBool done = FALSE;
    
    if (U_FAILURE(*status))
        return 0;
    
    if (inLocale == NULL)    
    {
        inLocale = uloc_getDefault();
        isDefaultLocale = TRUE;
    }
    else if (uprv_strcmp(inLocale, uloc_getDefault()) == 0)
    {
        isDefaultLocale = TRUE;
    }
    /*truncates the fallback mechanism if we start out with a defaultLocale*/
    
    if (locale == NULL) {
        locale = uloc_getDefault();
    }
    
    /*extracts the country*/
    cntryBufSize = uloc_getCountry(locale, inCountryBuffer, TEMPBUFSIZE, &err);
    
    
    if (cntryBufSize > 1)
    {
        /*
        We need to implement a fallback mechanism here because we are getting keys out of a
        tagged array, there is no capability of doing this with fallback through the resource
        bundle API
        */
        do 
        {
            /*
            If we are at the root locale ("")
            The first time we fall back to the full default locale
            As we iterate down the latter, if we hit the root locale ("")
            we pass it to the resource bundle api so it checks default.txt
            */
            
            if (inLocale[0] == '\0')
            {
                if (!isDefaultLocale)
                {
                    isDefaultLocale = TRUE;
                    inLocale = uloc_getDefault();
                }
                else
                    done = TRUE;
            }
            
            
            bundle = ures_open(NULL, inLocale, &err);      
            
            if (U_SUCCESS(err))
            {
                const UChar* temp;
                UResourceBundle* countryBundle;

                err = U_ZERO_ERROR;
                countryBundle = ures_getByKey(bundle, _kCountries, NULL, &err);
                if (U_SUCCESS(err))
                {
                    temp = ures_getStringByKey(countryBundle,
                        inCountryBuffer,
                        &resultLen,
                        &err);
                    resultLen++;
                    if (U_SUCCESS(err))
                        result = temp;
                    ures_close(countryBundle);
                }
                ures_close(bundle);
            }
            
            err = U_ZERO_ERROR;
            uloc_getParent(inLocale, inLocaleBuffer, TEMPBUFSIZE, &err);
            
            inLocale = inLocaleBuffer;
        } while ((result == NULL) && !done);
    }
    
    if (result)
    {
        if (resultLen > countryCapacity)
        {
            *status = U_BUFFER_OVERFLOW_ERROR;
            
            if (countryCapacity >= 1)
            {
                country[countryCapacity-1] = (UChar)0x0000;
                u_strncpy(country, result, countryCapacity-1);
            }
        }
        else u_strcpy(country, result);
    }
    else 
    {
        /*Falls back to ISO Name*/
        resultLen = cntryBufSize;
        if (resultLen > countryCapacity)
        {
            *status = U_BUFFER_OVERFLOW_ERROR;
            
            if (countryCapacity >= 1) 
            {
                u_uastrncpy(country, inCountryBuffer, countryCapacity-1);
                country[countryCapacity-1] = (UChar)0x0000;
            }
        }
        else u_uastrcpy(country, inCountryBuffer);
    }
    
    return resultLen;
}

int32_t uloc_getDisplayVariant(const char* locale,
                   const char* inLocale,
                   UChar* variant,
                   int32_t variantCapacity,
                   UErrorCode* status)
{
    const UChar* result = NULL;
    int32_t resultLen = 0;
    int varBufSize;
    char inVariantBuffer[TEMPBUFSIZE];
    char* inVariant = inVariantBuffer;
    UErrorCode err = U_ZERO_ERROR;
    UResourceBundle* bundle;
    char inLocaleBuffer[TEMPBUFSIZE];
    UBool isDefaultLocale = FALSE;
    char inVariantTagBuffer[TEMPBUFSIZE+2];
    char* inVariantTag = inVariantTagBuffer;
    UBool done = FALSE;
    
    if (U_FAILURE(*status))
        return 0;
    
    inVariantTagBuffer[0] = '\0';
    
    if (inLocale == NULL)    
    {
        inLocale = uloc_getDefault();
        isDefaultLocale = TRUE;
    }
    else if (uprv_strcmp(inLocale, uloc_getDefault()) == 0)
    {
        isDefaultLocale = TRUE;
    }
    /*truncates the fallback mechanism if we start out with a defaultLocale*/
    
    if (locale == NULL)
        locale = uloc_getDefault();
    
    /*extracts the variant*/
    varBufSize = uloc_getVariant(locale, inVariant, TEMPBUFSIZE, &err);
    
    if (varBufSize > 1)
    {
        /*In case the variant is longer than our stack buffers*/
        if (err == U_BUFFER_OVERFLOW_ERROR)
        {
            inVariant = (char*)uprv_malloc(varBufSize*sizeof(char)+1);
            if (inVariant == NULL)
                goto NO_MEMORY;
            inVariantTag = (char*)uprv_malloc(varBufSize*sizeof(char)+uprv_strlen("%%")+1);
            if (inVariantTag == NULL) 
            {
                uprv_free(inVariant);
                goto NO_MEMORY;
            }
            err = U_ZERO_ERROR;
            uloc_getVariant(locale, inVariant, varBufSize, &err);
        }
        
        uprv_strcpy(inVariantTag,"%%");  
        uprv_strcat(inVariantTag, inVariant);
        
        /*We need to implement a fallback mechanism here because we are getting keys out of a
        tagged array, there is no capability of doing this with fallback through the resource
        bundle API*/
        do {
            /*
            If we are at the root locale ("")
            The first time we fall back to the full default locale
            As we iterate down the latter, if we hit the root locale ("")
            we pass it to the resource bundle api so it checks default.txt
            */
            
            if (inLocale[0] == '\0')
            {
                if (!isDefaultLocale)
                {
                    isDefaultLocale = TRUE;
                    inLocale = uloc_getDefault();
                }
                else
                    done = TRUE;
            }
            
            
            bundle = ures_open(NULL, inLocale, &err);      
            
            if (U_SUCCESS(err))
            {
                const UChar* temp;
                
                temp = ures_getStringByKey(bundle,
                    inVariantTag,
                    &resultLen,
                    &err);
                resultLen++;
                if (U_SUCCESS(err))
                    result = temp;
                ures_close(bundle);
            }
            
            err = U_ZERO_ERROR;
            uloc_getParent(inLocale, inLocaleBuffer, TEMPBUFSIZE, &err);
            
            inLocale = inLocaleBuffer;
        } while ((result == NULL) && !done);
    }
    
    
    if (result)
    {
        if (resultLen > variantCapacity)
        {
            *status = U_BUFFER_OVERFLOW_ERROR;
            
            if (variantCapacity >= 1) 
            {
                variant[variantCapacity-1] = (UChar)0x0000;
                u_strncpy(variant, result, variantCapacity-1);
            }
        }
        else
        {
            u_strcpy(variant, result);
        }
    }
    else 
    {
        /*Falls back to user's Name*/
        resultLen = varBufSize;
        if (resultLen > variantCapacity)
        {
            *status = U_BUFFER_OVERFLOW_ERROR;
            
            if (variantCapacity >= 1) 
            {
                u_uastrncpy(variant, inVariant, variantCapacity-1);
                variant[variantCapacity-1] = (UChar)0x0000;
            }
        }
        else
        {
            u_uastrcpy(variant, inVariant);
        }
    }
    
    /*Clean up memory*/
    if (inVariant != inVariantBuffer)
    {
        uprv_free(inVariant);
        uprv_free(inVariantTag);
    } 
    return resultLen;
    
NO_MEMORY:
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
}

int32_t uloc_getDisplayName(const char* locale,
                const char* inLocale, 
                UChar* result,
                int32_t nameCapacity,
                UErrorCode* err) 
{
    UErrorCode int_err = U_ZERO_ERROR;
    int i = 0;
    int cntSze, varSze;
    UBool has_lang = TRUE;
    int result_size;
    
    int_err = U_ZERO_ERROR;
    
    /*Preflights all the components*/
    cntSze = uloc_getDisplayCountry(locale, 
        inLocale,
        NULL , 
        0,
        &int_err);
    int_err = U_ZERO_ERROR;
    varSze = uloc_getDisplayVariant(locale, 
        inLocale,
        NULL , 
        0,
        &int_err);
    
    int_err = U_ZERO_ERROR;
    i = uloc_getDisplayLanguage(locale, 
        inLocale,
        NULL,
        0, 
        &int_err);
    /*Decrement duplicative zero-terminators*/
    --varSze;
    --cntSze;
    
    /*Logic below adjusts pre-flight information with additional characters "(", ",", " ", ")"
    when neeed be*/
    if ((i-1 == 0) && (varSze == 0)) /*No language field*/
    {
        has_lang = FALSE;
        i = cntSze+1;
    }
    else if (cntSze)
    {
        if (varSze)
        {
            i += cntSze + varSze + 5;
        }
        else
        {
            i += cntSze + 3;
        }
    }
    
    int_err = U_ZERO_ERROR;
    
    result_size = uloc_getDisplayLanguage(locale, 
        inLocale,
        result,
        nameCapacity, 
        &int_err) - 1;
    
    if (U_SUCCESS(int_err)&&cntSze)
    {
        if (U_SUCCESS(int_err))
        {
            if (has_lang) 
            {
                u_strcat(result, openParen);
                result_size += 2;
            }
            
            result_size += uloc_getDisplayCountry(locale,
                inLocale,
                result + result_size,
                nameCapacity - result_size,
                &int_err) - 1;
        }
        
        if (varSze)
        {
            if (U_SUCCESS(int_err))      
            {
                u_strcat(result, comma);
                result_size += 2;
                
                result_size += uloc_getDisplayVariant(locale,
                    inLocale,
                    result + result_size,
                    nameCapacity - result_size, 
                    &int_err) - 1;
            }
        }
        
        if (U_SUCCESS(int_err)&&has_lang)
        {
            u_strcat(result, closeParen);
        }
    }
    
    *err  = int_err;
    
    return i;
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
    index = ures_open(NULL, kIndexLocaleName, &status);
    ures_getByKey(index, kIndexTag, &installed, &status);
    
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
