/*
**********************************************************************
*   Copyright (C) 1997-1999, International Business Machines
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
******************************************************************************/


#include "unicode/uloc.h"

#include "unicode/utypes.h"
#include "uresimp.h"
#include "unicode/uchar.h"
#include "umutex.h"
#include "cstring.h"
#include "unicode/ustring.h"
#include "cmemory.h"

/****************************************************************************
  Global variable and type definitions
******************************************************************************/

/* UnicodeString stuff */
typedef struct UnicodeString UnicodeString;

U_CFUNC int32_t T_UnicodeString_length(const UnicodeString *s);

U_CAPI int32_t
T_UnicodeString_extract(const UnicodeString *s, char *dst);

/* Locale stuff */
U_CAPI void locale_set_default(const char *id);

/* These strings describe the resources we attempt to load from
 the locale ResourceBundle data file.*/
static const char* _kLocaleString   = "LocaleString";
static const char* _kShortLanguage  = "ShortLanguage";
static const char* _kShortCountry   = "ShortCountry";
static const char* _kLocaleID       = "LocaleID";
static const char* _kLanguages      = "Languages";
static const char* _kCountries      = "Countries";


#define TEMPBUFSIZE 8

/*Some static strings needed in the getDisplay* functions*/
static const UChar openParen[] = { (UChar)0x0020 /* space */, (UChar)0x0028 /* ( */, (UChar)0x0000};
static const UChar comma[] = { (UChar)0x002C /* space */, (UChar)0x0020 /* , */, (UChar)0x0000};
static const UChar closeParen[] = { (UChar)0x0029 /* ( */, (UChar)0x0000};


static char* _defaultLocale = NULL;

static char** _installedLocales = NULL;
static int32_t _installedLocalesCount = 0;


static const char _languages[] =
"aa\0ab\0af\0am\0ar\0as\0ay\0az\0"
"ba\0be\0bg\0bh\0bi\0bn\0bo\0br\0"
"ca\0co\0cs\0cy\0da\0de\0dz\0"
"el\0en\0eo\0es\0et\0eu\0fa\0fi\0fj\0fo\0fr\0fy\0"
"ga\0gd\0gl\0gn\0gu\0ha\0he\0hi\0hr\0hu\0hy\0"
"ia\0id\0ie\0ik\0in\0is\0it\0iu\0iw\0"
"ja\0ji\0jw\0ka\0kk\0kl\0km\0kn\0ko\0ks\0ku\0ky\0"
"la\0ln\0lo\0lt\0lv\0"
"mg\0mi\0mk\0ml\0mn\0mo\0mr\0ms\0mt\0my\0"
"na\0ne\0nl\0no\0oc\0om\0or\0"
"pa\0pl\0ps\0pt\0qu\0rm\0rn\0ro\0ru\0rw\0"
"sa\0sd\0sg\0sh\0si\0sk\0sl\0sm\0sn\0so\0sq\0sr\0ss\0st\0su\0sv\0sw\0"
"ta\0te\0tg\0th\0ti\0tk\0tl\0tn\0to\0tr\0ts\0tt\0tw\0"
"ug\0uk\0ur\0uz\0vi\0vo\0wo\0xh\0yi\0yo\0za\0zh\0zu";
/* This list MUST be in sorted order, and MUST contain only two-letter codes! */

static const char _languages3[] =
"aar\0abk\0afr\0amh\0ara\0asm\0aym\0aze\0"
"bak\0bel\0bul\0bih\0bis\0ben\0bod\0bre\0"
"cat\0cos\0ces\0cym\0dan\0deu\0dzo\0"
"ell\0eng\0epo\0spa\0est\0eus\0fas\0fin\0fij\0fao\0fra\0fry\0"
"gai\0gdh\0glg\0grn\0guj\0hau\0heb\0hin\0hrv\0hun\0hye\0"
"ina\0ind\0ile\0ipk\0ind\0isl\0ita\0iku\0heb\0"
"jpn\0yid\0jaw\0kat\0kaz\0kal\0khm\0kan\0kor\0kas\0kur\0kir\0"
"lat\0lin\0lao\0lit\0lav\0"
    "mlg\0mri\0mkd\0mal\0mon\0mol\0mar\0msa\0mlt\0mya\0"
"nau\0nep\0nld\0nor\0oci\0orm\0ori\0"
"pan\0pol\0pus\0por\0que\0roh\0run\0ron\0rus\0kin\0"
"san\0snd\0sag\0srp\0sin\0slk\0slv\0smo\0sna\0som\0sqi\0srp\0ssw\0sot\0sun\0swe\0swa\0"
"tat\0tel\0tgk\0tha\0tir\0tuk\0tgl\0tsn\0ton\0tur\0tsn\0tat\0twi\0"
"uig\0ukr\0urd\0uzb\0vie\0vol\0wol\0xho\0yid\0yor\0zha\0zho\0zul";
/* This list MUST contain a three-letter code for every two-letter code in the
   list above, and they MUST ne in the same order (i.e., the same language must
   be in the same place in both lists)! */

static const char _countries[] =
"AD\0AE\0AF\0AG\0AI\0AL\0AM\0AN\0AO\0AQ\0AR\0AS\0AT\0AU\0AW\0AZ\0"
"BA\0BB\0BD\0BE\0BF\0BG\0BH\0BI\0BJ\0BM\0BN\0BO\0BR\0BS\0BT\0BV\0BW\0BY\0BZ\0"
"CA\0CC\0CF\0CG\0CH\0CI\0CK\0CL\0CM\0CN\0CO\0CR\0CU\0CV\0CX\0CY\0CZ\0"
"DE\0DJ\0DK\0DM\0DO\0DZ\0EC\0EE\0EG\0EH\0ER\0ES\0ET\0"
"FI\0FJ\0FK\0FM\0FO\0FR\0FX\0"
"GA\0GB\0GD\0GE\0GF\0GH\0GI\0GL\0GM\0GN\0GP\0GQ\0GR\0GS\0GT\0GU\0GW\0GY\0"
"HK\0HM\0HN\0HR\0HT\0HU\0ID\0IE\0IL\0IN\0IO\0IQ\0IR\0IS\0IT\0"
"JM\0JO\0JP\0KE\0KG\0KH\0KI\0KM\0KN\0KP\0KR\0KW\0KY\0KZ\0"
"LA\0LB\0LC\0LI\0LK\0LR\0LS\0LT\0LU\0LV\0LY\0"
"MA\0MC\0MD\0MG\0MH\0MK\0ML\0MM\0MN\0MO\0MP\0MQ\0MR\0MS\0MT\0MU\0MV\0MW\0MX\0MY\0MZ\0"
"NA\0NC\0NE\0NF\0NG\0NI\0NL\0NO\0NP\0NR\0NU\0NZ\0OM\0"
"PA\0PE\0PF\0PG\0PH\0PK\0PL\0PM\0PN\0PR\0PT\0PW\0PY\0QA\0RE\0RO\0RU\0RW\0"
"SA\0SB\0SC\0SD\0SE\0SG\0SH\0SI\0SJ\0SK\0SL\0SM\0SN\0SO\0SR\0ST\0SV\0SY\0SZ\0"
"TC\0TD\0TF\0TG\0TH\0TJ\0TK\0TM\0TN\0TO\0TP\0TR\0TT\0TV\0TW\0TZ\0"
"UA\0UG\0UM\0US\0UY\0UZ\0VA\0VC\0VE\0VG\0VI\0VN\0VU\0"
"WF\0WS\0YE\0YT\0YU\0ZA\0ZM\0ZR\0ZW";
/* This list MUST be in sorted order, and MUST contain only two-letter codes! */

static const char _countries3[] =
"AND\0ARE\0AFG\0ATG\0AIA\0ALB\0ARM\0ANT\0AGO\0ATA\0ARG\0ASM\0AUT\0AUS\0ABW\0AZE\0"
"BIH\0BRB\0BGD\0BEL\0BFA\0BGR\0BHR\0BDI\0BEN\0BMU\0BRN\0BOL\0BRA\0BHS\0BTN\0BVT\0BWA"
"\0BLR\0BLZ\0"
"CAN\0CCK\0CAF\0COG\0CHE\0CIV\0COK\0CHL\0CMR\0CHN\0COL\0CRI\0CUB\0CPV\0CXR\0CYP\0CZE\0"
"DEU\0DJI\0DNK\0DMA\0DOM\0DZA\0ECU\0EST\0EGY\0ESH\0ERI\0ESP\0ETH\0"
"FIN\0FJI\0FLK\0FSM\0FRO\0FRA\0FXX\0"
"GAB\0GBR\0GRD\0GEO\0GUF\0GHA\0GIB\0GRL\0GMB\0GIN\0GLP\0GNQ\0GRC\0SGS\0GTM\0GUM"
"\0GNB\0GUY\0"
"HKG\0HMD\0HND\0HRV\0HTI\0HUN\0IDN\0IRL\0ISR\0IND\0IOT\0IRQ\0IRN\0ISL\0ITA\0"
"JAM\0JOR\0JPN\0KEN\0KGZ\0KHM\0KIR\0COM\0KNA\0PRK\0KOR\0KWT\0CYM\0KAZ\0"
"LAO\0LBN\0LCA\0LIE\0LKA\0LBR\0LSO\0LTU\0LUX\0LVA\0LBY\0"
"MAR\0MCO\0MDA\0MDG\0MHL\0MKD\0MLI\0MMR\0MNG\0MAC\0MNP\0MTQ\0MRT\0MSR\0MLT\0MUS\0"
"MDV\0MWI\0MEX\0MYS\0MOZ\0"
"NAM\0NCL\0NER\0NFK\0NGA\0NIC\0NLD\0NOR\0NPL\0NRU\0NIU\0NZL\0OMN\0"
"PAN\0PER\0PYF\0PNG\0PHL\0PAK\0POL\0SPM\0PCN\0PRI\0PRT\0PLW\0PRY\0QAT\0REU\0ROM"
"\0RUS\0RWA\0"
"SAU\0SLB\0SYC\0SDN\0SWE\0SGP\0SHN\0SVN\0SJM\0SVK\0SLE\0SMR\0SEN\0SOM\0SUR\0STP"
"\0SLV\0SYR\0SWZ\0"
"TCA\0TCD\0ATF\0TGO\0THA\0TJK\0TKL\0TKM\0TUN\0TON\0TMP\0TUR\0TTO\0TUV\0TWN\0TZA\0"
"UKR\0UGA\0UMI\0USA\0URY\0UZB\0VAT\0VCT\0VEN\0VGB\0VIR\0VNM\0VUT\0"
"WLF\0WSM\0YEM\0MYT\0YUG\0ZAF\0ZMB\0ZAR\0ZWE";
/* This list MUST contain a three-letter code for every two-letter code in
   the above list, and they MUST be listed in the same order! */

static char** _isoLanguages = NULL;
static char** _isoCountries = NULL;

/*******************************************************************************
  Implementation function definitions
*******************************************************************************/

static int16_t _findIndex(const char* list, int32_t listLength, const char* key);

/*Works like strchr with '_' pr '-'*/
static const char* _findCharSeparator(const char* string);

/*Lazy evaluated the list of installed locales*/
static void _lazyEvaluate_installedLocales(void);

/*returns TRUE if a is an ID separator FALSE otherwise*/
#define _isIDSeparator(a) (a == '_' || a == '-')


/*******************************************************************************
  API function definitions
*******************************************************************************/


const char* _findCharSeparator(const char* string)
{
  if (string == NULL) return NULL;
  /*Keeps iterating until an ID separator is found*/
  while (*string && !_isIDSeparator(*string)) string++;
  if (*string) return string;
  else return NULL;
}


int16_t _findIndex(const char* list, int32_t listLength, const char* key)
{
  const char* anchor = list;
  const char* listEnd = anchor + listLength;
  UBool found = FALSE;
  int tokenSize = uprv_strlen(list)+1; /*gets the size of the tokens*/
  
  while (!found && list<listEnd)
    {
      if (uprv_strcmp(key, list) == 0) 
    {
      found = TRUE;
      break;
    }
      list += tokenSize;
    }
  if (found == TRUE) return (int16_t)((list - anchor)/tokenSize);
  else return -1;
}

const char* uloc_getDefault()
{
  const char* result = _defaultLocale;
  UErrorCode err = U_ZERO_ERROR;
  
  /*lazy evaluates _defaultLocale*/
  if (result == NULL) 
    {
      uloc_setDefault(NULL, &err);
      result = _defaultLocale;
    }
  
  return result;
}

void uloc_setDefault(const char*   newDefaultLocale,
             UErrorCode* err) 
{

  if (U_FAILURE(*err))    return;
  /* the error code isn't currently used for anything by this function*/
  
  if (newDefaultLocale == NULL) 
    {
      newDefaultLocale = uprv_getDefaultLocaleID();
    }
  
  umtx_lock(NULL);
  if(_defaultLocale == NULL)
    _defaultLocale = (char*)uprv_malloc(sizeof(char) * (uprv_strlen(newDefaultLocale) + 1));
  else
    _defaultLocale = (char*)uprv_realloc(_defaultLocale, 
                         sizeof(char) * (uprv_strlen(newDefaultLocale) + 1));
  uprv_strcpy(_defaultLocale, newDefaultLocale);
  umtx_unlock(NULL);

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

  if (U_FAILURE(*err)) return 0;

  if (localeID == NULL)    localeID = uloc_getDefault();
  
 
 while (localeID[offset]&&(count < 2))
    {
      if (_isIDSeparator(localeID[offset++])) count++;
    }
  
  /*finds the second IDSeparator*/
  while (offset && !_isIDSeparator(localeID[offset])) offset--;

  
  /*Loop updates i to the size of the parent
    but only copies into the buffer as much as the buffer can bare*/
  while (i < offset)
    {
      if (parentCapacity > i) parent[i] = localeID[i];
      i++;
    }
  
  /*Sets the error code on case of need*/
  if (i >= parentCapacity )
    {
      *err = U_BUFFER_OVERFLOW_ERROR;
    }
  
  if (parentCapacity>0)   parent[uprv_min(i,parentCapacity-1)] = '\0';
 
  
  return i+1;
}

int32_t
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err)
{
  int i=0;
  
  
  if (U_FAILURE(*err)) return 0;
  
  if (localeID == NULL)    localeID = uloc_getDefault();

  /*Loop updates i to the size of the language
    but only copies into the buffer as much as the buffer can bare*/
  while ((*localeID != '\0') && !_isIDSeparator(*localeID))
    {
      if (languageCapacity > i) language[i] = (char)tolower(*localeID);
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
  
  if (U_FAILURE(*err)) return 0;
  if (localeID == NULL)    localeID = uloc_getDefault();
  
  localeID = _findCharSeparator(localeID);
  
  /*Loop updates i to the size of the language
    but only copies into the buffer as much as the buffer can bare*/
  if (localeID)
    {
      ++localeID;
      while ((*localeID != '\0') && !_isIDSeparator(*localeID))
      {
        if (countryCapacity > i) country[i] = (char)toupper(*localeID);
        i++;
        localeID++;
      }
    }
  
  if (i >= countryCapacity )
    {
      *err = U_BUFFER_OVERFLOW_ERROR;
    }
  
  if (countryCapacity > 0) {country[uprv_min(i,countryCapacity-1)] = '\0';}
  return i+1;
}

int32_t uloc_getVariant(const char* localeID,
                        char* variant,
                        int32_t variantCapacity,
                        UErrorCode* err) 
{
  int i=0;
  
  if (U_FAILURE(*err)) return 0;
  if (localeID == NULL)    localeID = uloc_getDefault();
  
  localeID = _findCharSeparator(localeID);
  if (localeID)    localeID = _findCharSeparator(++localeID);
  
  if (localeID)
    {
      ++localeID;
      /*Loop updates i to the size of the language
    but only copies into the buffer as much as the buffer can bare*/
      while (*localeID != '\0')
    {
      if (variantCapacity > i) variant[i] = (char)toupper(*localeID);
      i++;
      localeID++;
    }
      
    }
  
  if (i >= variantCapacity )
    {
      *err = U_BUFFER_OVERFLOW_ERROR;
    }
  
  
  if (variantCapacity>0) {variant[uprv_min(i,variantCapacity-1)] = '\0';}
  return i+1;
}

int32_t uloc_getName(const char* localeID,
             char* name,
             int32_t nameCapacity,
             UErrorCode* err)  
{
  int i= 0;
  int varSze = 0;
  int cntSze = 0;
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

  if (cntSze) i++;
  if (varSze) i++;
  i += cntSze + varSze;
  
  int_err = U_ZERO_ERROR;
  
  uloc_getLanguage(localeID, 
           name,
           nameCapacity, 
           &int_err);

  /*We fill in the users buffer*/
  if ((nameCapacity>0) && cntSze)
    {
      if (U_SUCCESS(int_err)) uprv_strcat(name, "_");
      
      uloc_getCountry(localeID,
          name + uprv_strlen(name),
              nameCapacity - uprv_strlen(name),
              &int_err);
      
      if (varSze)
    {
      if (U_SUCCESS(int_err)) uprv_strcat(name, "_");
      
      uloc_getVariant(localeID,
                   name + uprv_strlen(name),
                   nameCapacity - uprv_strlen(name), 
                   &int_err);
    }
      
    }
  *err  = int_err;
  
  return i;
}

const char* uloc_getISO3Language(const char* localeID) 
{
  int16_t offset;
  char lang[TEMPBUFSIZE];
  UErrorCode err = U_ZERO_ERROR;
  
  if (localeID == NULL)    localeID = uloc_getDefault();
  uloc_getLanguage(localeID, lang, TEMPBUFSIZE, &err);
  if (U_FAILURE(err)) return "";
  offset = _findIndex(_languages, sizeof(_languages),lang);
  if (offset < 0) return "";
  return &(_languages3[offset * 4]);
}

const char* uloc_getISO3Country(const char* localeID) 
{
  int16_t offset;
  char cntry[TEMPBUFSIZE];
  UErrorCode err = U_ZERO_ERROR;
  
  if (localeID == NULL)    localeID = uloc_getDefault();
  uloc_getCountry(localeID, cntry, TEMPBUFSIZE, &err);
  if (U_FAILURE(err)) return "";
  offset = _findIndex(_countries, sizeof(_countries), cntry);
  if (offset < 0) return "";

  return &(_countries3[offset * 4]);
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
  int32_t i = 0;
  int langBufSize;
  char inLanguageBuffer[TEMPBUFSIZE];
  char inLocaleBuffer[TEMPBUFSIZE];
  UErrorCode err = U_ZERO_ERROR;
  UResourceBundle* bundle;
  const UChar* temp = NULL;  
  UBool isDefaultLocale = FALSE;
  UBool done = FALSE;

  if (U_FAILURE(*status)) return 0;
  
  if (inLocale == NULL)    
    {
      inLocale = uloc_getDefault();
      isDefaultLocale = TRUE;
    }
  else if (uprv_strcmp(inLocale, uloc_getDefault()) == 0) isDefaultLocale = TRUE;
  /*truncates the fallback mechanism if we start out with a defaultLocale*/
  
  if (locale == NULL) locale = uloc_getDefault();
  
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
          else   done = TRUE;
        }
      
      
      bundle = ures_open(NULL, inLocale, &err);
      
      if (U_SUCCESS(err))
        {
          err = U_ZERO_ERROR;
          temp = ures_getTaggedArrayItem(bundle,
                         _kLanguages,
                         inLanguageBuffer, 
                         &err);
          if (U_SUCCESS(err))        result = temp;
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
      i = u_strlen(result)+1;
      if (i > languageCapacity)
    {
      *status = U_BUFFER_OVERFLOW_ERROR;
      
      if (languageCapacity >= 1) 
        {
          u_strncpy(language, result, languageCapacity-1);
          language[languageCapacity-1] = (UChar)0x0000;
        }
    }
      else u_strcpy(language, result);
    }
  else 
    {
      /*Falls back to ISO Name*/
      i = langBufSize;
      if (i > languageCapacity)
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
  return i;
}

int32_t uloc_getDisplayCountry(const char* locale,
                   const char* inLocale,
                   UChar* country,
                   int32_t countryCapacity,
                   UErrorCode* status)
{
  /* NULL may be used to specify the default */
  const UChar* result = NULL;
  int32_t i = 0;
  int cntryBufSize;
  char inCountryBuffer[TEMPBUFSIZE];
  UErrorCode err = U_ZERO_ERROR;
  UResourceBundle* bundle = NULL;
  char inLocaleBuffer[TEMPBUFSIZE];
  UBool isDefaultLocale = FALSE;
  UBool done = FALSE;

  if (U_FAILURE(*status)) return 0;
  
  

  if (inLocale == NULL)    
    {
      inLocale = uloc_getDefault();
      isDefaultLocale = TRUE;
    }
  else if (uprv_strcmp(inLocale, uloc_getDefault()) == 0) isDefaultLocale = TRUE;
    /*truncates the fallback mechanism if we start out with a defaultLocale*/

  if (locale == NULL) locale = uloc_getDefault();
  
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
      else   done = TRUE;
    }
      

      bundle = ures_open(NULL, inLocale, &err);      
      
      if (U_SUCCESS(err))
    {
      const UChar* temp;
      
      temp = ures_getTaggedArrayItem(bundle,
                     _kCountries,
                     inCountryBuffer, 
                     &err);
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
      i = u_strlen(result)+1;
      if (i > countryCapacity)
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
      i = cntryBufSize;
      if (i > countryCapacity)
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

  return i;
}

int32_t uloc_getDisplayVariant(const char* locale,
                   const char* inLocale,
                   UChar* variant,
                   int32_t variantCapacity,
                   UErrorCode* status)
{
  const UChar* result = NULL;
  int32_t i = 0;
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

  if (U_FAILURE(*status)) return 0;
  
  inVariantTagBuffer[0] = '\0';
  
  if (inLocale == NULL)    
    {
      inLocale = uloc_getDefault();
      isDefaultLocale = TRUE;
    }
  else if (uprv_strcmp(inLocale, uloc_getDefault()) == 0) isDefaultLocale = TRUE;
    /*truncates the fallback mechanism if we start out with a defaultLocale*/

  if (locale == NULL) locale = uloc_getDefault();
    
  /*extracts the variant*/
  varBufSize = uloc_getVariant(locale, inVariant, TEMPBUFSIZE, &err);
  
  if (varBufSize > 1)
    {
      /*In case the variant is longer than our stack buffers*/
      if (err == U_BUFFER_OVERFLOW_ERROR)
    {
      inVariant = (char*)uprv_malloc(varBufSize*sizeof(char)+1);
      if (inVariant == NULL) goto NO_MEMORY;
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
      else   done = TRUE;
    }

      
      bundle = ures_open(NULL, inLocale, &err);      
      
      if (U_SUCCESS(err))
    {
      const UChar* temp;
      
      temp = ures_get(bundle,
              inVariantTag, 
              &err);
      if (U_SUCCESS(err))  result = temp;
      ures_close(bundle);
    }

      err = U_ZERO_ERROR;
      uloc_getParent(inLocale, inLocaleBuffer, TEMPBUFSIZE, &err);
      
      inLocale = inLocaleBuffer;
    } while ((result == NULL) && !done);
 
 
          
    }
      
      
      if (result)
    {
      i = u_strlen(result)+1;
      if (i > variantCapacity)
    {
      *status = U_BUFFER_OVERFLOW_ERROR;

      if (variantCapacity >= 1) 
        {
          variant[variantCapacity-1] = (UChar)0x0000;
          u_strncpy(variant, result, variantCapacity-1);
        }
    }
      else u_strcpy(variant, result);
    }
  else 
    {
      /*Falls back to user's Name*/
      i = varBufSize;
      if (i > variantCapacity)
    {
      *status = U_BUFFER_OVERFLOW_ERROR;
      
      if (variantCapacity >= 1) 
        {
          u_uastrncpy(variant, inVariant, variantCapacity-1);
          variant[variantCapacity-1] = (UChar)0x0000;
        }
    }
      else u_uastrcpy(variant, inVariant);
    }
 
  /*Clean up memory*/
  if (inVariant != inVariantBuffer)
    {
      uprv_free(inVariant);
      uprv_free(inVariantTag);
    } 
  return i;

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
      if (varSze) i += cntSze + varSze + 5;
      else i += cntSze + 3;
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
      
      if (U_SUCCESS(int_err)&&has_lang) u_strcat(result, closeParen);
    }
  
  *err  = int_err;
  
  return i;
}

const char*
uloc_getAvailable(int32_t offset) 
{
  
  if (_installedLocales == NULL) _lazyEvaluate_installedLocales();
  
  if (offset > _installedLocalesCount) return NULL;
  else  return _installedLocales[offset];
  
}

int32_t uloc_countAvailable()
{
  if (_installedLocales == NULL) _lazyEvaluate_installedLocales();
  
    return _installedLocalesCount;
}

void _lazyEvaluate_installedLocales()
{

    UResourceBundle *index = NULL;
    UResourceBundle installed;
    UErrorCode status = U_ZERO_ERROR;
    const UChar *lname;
    char ** temp;
    int32_t i = 0;
    int32_t len = 0;

    index = ures_open(NULL, kIndexLocaleName, &status);
    ures_getByKey(index, kIndexTag, &installed, &status);

    if(U_SUCCESS(status)) {
      _installedLocalesCount = ures_getSize(&installed);
      temp = (char **) uprv_malloc(sizeof(char*) * (_installedLocalesCount+1));

      ures_resetIterator(&installed);
      while(ures_hasNext(&installed)) {
        lname = ures_getNextString(&installed, &len, NULL, &status);
        temp[i] = (char*) uprv_malloc(sizeof(char) * (len + 1));

        u_UCharsToChars(lname, temp[i], len);
        temp[i][len] = 0; /* Terminate the string */
        i++;
      }
      {
	umtx_lock(NULL);
	if (_installedLocales == NULL)
	{
	  _installedLocales = temp;
	  temp = NULL;
	} else {
	  for (i = 0; i < _installedLocalesCount; i++) uprv_free(temp[i]);
	  uprv_free(temp);
	}
	umtx_unlock(NULL);
    
      }
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
  const char *from, *end;
  char **to;
  
  if (_isoLanguages == NULL) 
    {
      
      {
    umtx_lock(NULL);
    
    if (_isoLanguages == NULL) 
      {
        _isoLanguages = (char**) uprv_malloc(sizeof(char*)*(1+(sizeof(_languages) / 3)));
        
        end = _languages + (sizeof(_languages));
        from = _languages; 
        to = _isoLanguages;
        
        while (from < end) 
          {
        *to = (char*)from;
        ++to;
        from += 3;
          }
        *to = NULL;
      }
    umtx_unlock(NULL);
      }
    }
  return (const char* const*)_isoLanguages;
}

/**
 * Returns a list of all 2-letter country codes defined in ISO 639.  This is a
 * pointer to an array of pointers to arrays of char.  All of these pointers are
 * owned by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
const char* const* uloc_getISOCountries() 
{
  if (_isoCountries == NULL) 
    {
      const char *from, *end;
      char **to;
      {
    umtx_lock(NULL);
    
    if (_isoCountries == NULL) 
      {
        _isoCountries = (char**) uprv_malloc(sizeof(char*)*(1+(sizeof(_countries) / 3)));
        
        end = _countries + (sizeof(_countries));
        from = _countries;
        to = _isoCountries;
        
        while (from < end) 
          {
        *to = (char*)from;
        ++to;
        from += 3;
          }
        *to = NULL;
      }
    umtx_unlock(NULL);
      }
    }
  return (const char* const*)_isoCountries;
}
