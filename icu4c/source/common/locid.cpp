/*
 **********************************************************************
 *   Copyright (C) 1997-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*
* File locid.cpp
*
* Created by: Richard Gillam
*
* Modification History:
*
*   Date        Name        Description
*   02/11/97    aliu        Changed gLocPath to fgDataDirectory and added 
*                           methods to get and set it.
*   04/02/97    aliu        Made operator!= inline; fixed return value 
*                           of getName().
*   04/15/97    aliu        Cleanup for AIX/Win32.
*   04/24/97    aliu        Numerous changes per code review.
*   08/18/98    stephen     Changed getDisplayName()
*                           Added SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE
*                           Added getISOCountries(), getISOLanguages(),
*                           getLanguagesForCountry()
*   03/16/99    bertrand    rehaul.
*   07/21/99    stephen     Added U_CFUNC setDefault
*   11/09/99    weiv        Added const char * getName() const;
*   04/12/00    srl         removing unicodestring api's and cached hash code
*   08/10/01    grhoten     Change the static Locales to accessor functions
******************************************************************************
*/


#include "unicode/locid.h"
#include "unicode/uloc.h"
#include "mutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "uhash.h"
#include "ucln_cmn.h"

U_NAMESPACE_BEGIN

static Locale*  availableLocaleList = NULL;
static int32_t  availableLocaleListCount;
typedef enum ELocalePos {
    eENGLISH,
    eFRENCH,
    eGERMAN,
    eITALIAN,
    eJAPANESE,
    eKOREAN,
    eCHINESE,

    eFRANCE,
    eGERMANY,
    eITALY,
    eJAPAN,
    eKOREA,
    eCHINA,      /* Alias for PRC */
    eTAIWAN,
    eUK,
    eUS,
    eCANADA,
    eCANADA_FRENCH,


    eDEFAULT,
    eMAX_LOCALES
} ELocalePos;

/* Use void * to make it properly aligned */
/* Add 1 for rounding */
static void *gByteLocaleCache[(eMAX_LOCALES + 1) * sizeof(Locale) / sizeof(void*)];

static Locale *gLocaleCache = NULL;

U_NAMESPACE_END

UBool
locale_cleanup(void)
{
    U_NAMESPACE_USE

    if (availableLocaleList) {
        delete []availableLocaleList;
        availableLocaleList = NULL;
    }
    availableLocaleListCount = 0;
    if (gLocaleCache) {
        gLocaleCache[eDEFAULT].~Locale();
        gLocaleCache = NULL;
    }
    return TRUE;
}

void U_NAMESPACE_QUALIFIER locale_set_default_internal(const char *id)
{
    U_NAMESPACE_USE

#ifdef ICU_LOCID_USE_DEPRECATES
    Locale::fgDefaultLocale.init(id);
#else
if (gLocaleCache == NULL) {
        Locale::initLocaleCache();
    }

    {
        Mutex lock;
        gLocaleCache[eDEFAULT].init(id);
    }
#endif
}

/* sfb 07/21/99 */
U_CFUNC void
locale_set_default(const char *id)
{
    U_NAMESPACE_USE

    locale_set_default_internal(id);
}
/* end */

U_CFUNC const char *
locale_get_default(void)
{
    U_NAMESPACE_USE

    return Locale::getDefault().getName();
}


U_NAMESPACE_BEGIN

/*Character separating the posix id fields*/
// '_'
// In the platform codepage.
#define SEP_CHAR '_'

/**
 * static variables
 */
#ifdef ICU_LOCID_USE_DEPRECATES
Locale Locale::fgDefaultLocale;

/**
 * Constant definitions
 */
const Locale  Locale::ENGLISH("en");
const Locale  Locale::FRENCH("fr");
const Locale  Locale::GERMAN("de");
const Locale  Locale::ITALIAN("it");
const Locale  Locale::JAPANESE("ja");
const Locale  Locale::KOREAN("ko");
const Locale  Locale::CHINESE("zh");
const Locale  Locale::SIMPLIFIED_CHINESE("zh", "CN");
const Locale  Locale::TRADITIONAL_CHINESE("zh", "TW");

// Useful constant for country.

const Locale  Locale::FRANCE    ("fr", "FR");
const Locale  Locale::GERMANY   ("de", "DE");
const Locale  Locale::ITALY     ("it", "IT");
const Locale  Locale::JAPAN     ("ja", "JP");
const Locale  Locale::KOREA     ("ko", "KR");
const Locale  Locale::CHINA     ("zh", "CN");
const Locale  Locale::PRC       ("zh", "CN");
const Locale  Locale::TAIWAN    ("zh", "TW");
const Locale  Locale::UK        ("en", "GB");
const Locale  Locale::US        ("en", "US");
const Locale  Locale::CANADA    ("en", "CA");
const Locale  Locale::CANADA_FRENCH("fr", "CA");

#else
const Locale::LocaleProxy Locale::ENGLISH  = {eENGLISH};
const Locale::LocaleProxy Locale::FRENCH   = {eFRENCH};
const Locale::LocaleProxy Locale::GERMAN   = {eGERMAN};
const Locale::LocaleProxy Locale::ITALIAN  = {eITALIAN};
const Locale::LocaleProxy Locale::JAPANESE = {eJAPANESE};
const Locale::LocaleProxy Locale::KOREAN   = {eKOREAN};
const Locale::LocaleProxy Locale::CHINESE  = {eCHINESE};
const Locale::LocaleProxy Locale::SIMPLIFIED_CHINESE={eCHINA};
const Locale::LocaleProxy Locale::TRADITIONAL_CHINESE={eTAIWAN};

const Locale::LocaleProxy Locale::FRANCE   = {eFRANCE};
const Locale::LocaleProxy Locale::GERMANY  = {eGERMANY};
const Locale::LocaleProxy Locale::ITALY    = {eITALY};
const Locale::LocaleProxy Locale::JAPAN    = {eJAPAN};
const Locale::LocaleProxy Locale::KOREA    = {eKOREA};
const Locale::LocaleProxy Locale::CHINA    = {eCHINA};
const Locale::LocaleProxy Locale::PRC      = {eCHINA};
const Locale::LocaleProxy Locale::TAIWAN   = {eTAIWAN};
const Locale::LocaleProxy Locale::UK       = {eUK};
const Locale::LocaleProxy Locale::US       = {eUS};
const Locale::LocaleProxy Locale::CANADA   = {eCANADA};
const Locale::LocaleProxy Locale::CANADA_FRENCH={eCANADA_FRENCH};

Locale::LocaleProxy::operator const Locale&(void) const
{
    return Locale::getLocale(magicLocaleNumber);
}

#endif

Locale::~Locale()
{   
    /*if fullName is on the heap, we delete it*/
    if (fullName != fullNameBuffer) 
    {
        delete []fullName;
        fullName = NULL;
    }
}

Locale::Locale()
    : fullName(fullNameBuffer)
{
    init(NULL);
}

Locale::Locale( const   char * newLanguage, 
                const   char * newCountry, 
                const   char * newVariant) 
    : fullName(fullNameBuffer)
{
    if( (newLanguage==NULL) && (newCountry == NULL) && (newVariant == NULL) )
    {
        init(NULL); /* shortcut */
    }
    else
    {
        char togo_stack[ULOC_FULLNAME_CAPACITY];
        char *togo;
        char *togo_heap = NULL;
        int32_t size = 0;
        int32_t lsize = 0;
        int32_t csize = 0;
        int32_t vsize = 0;
        char    *p;

        // Calculate the size of the resulting string.

        // Language
        if ( newLanguage != NULL )
        {
            lsize = (int32_t)uprv_strlen(newLanguage);
            size = lsize;
        }

        // _Country
        if ( newCountry != NULL )
        {
            csize = (int32_t)uprv_strlen(newCountry);
            size += csize;
        }

        // _Variant
        if ( newVariant != NULL )
        {
            // remove leading _'s
            while(newVariant[0] == SEP_CHAR)
            {
                newVariant++;
            }
            
            // remove trailing _'s
            vsize = (int32_t)uprv_strlen(newVariant);
            while( (vsize>1) && (newVariant[vsize-1] == SEP_CHAR) )
            {
                vsize--;
            }
        }

        if( vsize > 0 )
        {
            size += vsize;
        }

        // Separator rules:
        if ( vsize > 0 )
        {
            size += 2;  // at least: __v 
        }
        else if ( csize > 0 )
        {
            size += 1;  // at least: _v 
        }

        //  NOW we have the full locale string..

        /*if the whole string is longer than our internal limit, we need
        to go to the heap for temporary buffers*/
        if (size > ULOC_FULLNAME_CAPACITY)
        {
            togo_heap = new char[size+1];
            togo = togo_heap;
        }
        else
        {
            togo = togo_stack;
        }

        togo[0] = 0;

        // Now, copy it back.
        p = togo;
        if ( lsize != 0 )
        {
            uprv_strcpy(p, newLanguage);
            p += lsize;
        }

        if ( ( vsize != 0 ) || (csize != 0) )  // at least:  __v
        {                                      //            ^
            *p++ = SEP_CHAR;
        }

        if ( csize != 0 )
        { 
            uprv_strcpy(p, newCountry);
            p += csize;
        }

        if ( vsize != 0)
        {
            *p++ = SEP_CHAR; // at least: __v

            uprv_strncpy(p, newVariant, vsize);  // Must use strncpy because 
            p += vsize;                          // of trimming (above).
            *p = 0; // terminate
        }

        // Parse it, because for example 'language' might really be a complete
        // string.
        init(togo);

        delete [] togo_heap; /* If it was needed */
    }
}

Locale::Locale(const Locale &other)
    : fullName(fullNameBuffer)
{
    *this = other;
}

Locale &Locale::operator=(const Locale &other)
{
    /* Free our current storage */
    if(fullName != fullNameBuffer) {
        delete [] fullName;
        fullName = fullNameBuffer;
    }

    /* Allocate the full name if necessary */
    if(other.fullName != other.fullNameBuffer) {
        fullName = new char[(uprv_strlen(other.fullName)+1)];
    }

    /* Copy the full name */
    uprv_strcpy(fullName, other.fullName);

    /* Copy the language and country fields */
    uprv_strcpy(language, other.language);
    uprv_strcpy(country, other.country);

    /* The variantBegin is an offset into fullName, just copy it */
    variantBegin = other.variantBegin;
    return *this;
}

UBool
Locale::operator==( const   Locale& other) const
{
    return (uprv_strcmp(other.fullName, fullName) == 0);
}

/*This function initializes a Locale from a C locale ID*/
Locale& Locale::init(const char* localeID)
{
    /* Free our current storage */
    if(fullName != fullNameBuffer) {
        delete [] fullName;
        fullName = fullNameBuffer;
    }

    // not a loop:
    // just an easy way to have a common error-exit
    // without goto and without another function
    do {
        char *separator, *prev;
        int32_t length;
        UErrorCode err;

        if(localeID == NULL) {
            // not an error, just set the default locale
            break;
        }

        // "canonicalize" the locale ID to ICU/Java format
        err = U_ZERO_ERROR;
        length = uloc_getName(localeID, fullName, sizeof(fullNameBuffer), &err);
        if(U_FAILURE(err) || err == U_STRING_NOT_TERMINATED_WARNING) {
            /*Go to heap for the fullName if necessary*/
            fullName = new char[length + 1];
            if(fullName == 0) {
                fullName = fullNameBuffer;
                break;
            }
            err = U_ZERO_ERROR;
            length = uloc_getName(localeID, fullName, length + 1, &err);
        }
        if(U_FAILURE(err) || err == U_STRING_NOT_TERMINATED_WARNING) {
            /* should never occur */
            break;
        }

        /* preset all fields to empty */
        language[0] = country[0] = 0;
        variantBegin = uprv_strlen(fullName);

        /* after uloc_getName() we know that only '_' are separators */
        separator = uprv_strchr(fullName, SEP_CHAR);
        if(separator != 0) {
            /* there is a country field */
            length = separator - fullName;
            if(length > 0) {
                if(length >= (int32_t)sizeof(language)) {
                    break;
                }
                uprv_memcpy(language, fullName, length);
            }
            language[length] = 0;

            prev = separator + 1;
            separator = uprv_strchr(prev, SEP_CHAR);
            if(separator != 0) {
                /* there is a variant field */
                length = separator - prev;
                if(length > 0) {
                    if(length >= (int32_t)sizeof(country)) {
                        break;
                    }
                    uprv_memcpy(country, prev, length);
                }
                country[length] = 0;

                variantBegin = (separator + 1) - fullName;
            } else {
                /* variantBegin==strlen(fullName), length==strlen(language)==prev-1-fullName */
                if((variantBegin - length - 1) >= (int32_t)sizeof(country)) {
                    break;
                }
                uprv_strcpy(country, prev);
            }
        } else {
            /* variantBegin==strlen(fullName) */
            if(variantBegin >= (int32_t)sizeof(language)) {
                break;
            }
            uprv_strcpy(language, fullName);
        }

        // successful end of init()
        return *this;
    } while(0);

    if (gLocaleCache) {
        // when an error occurs, then set the default locale (there is no UErrorCode here)
        return *this = getLocale(eDEFAULT);
    }
    else {
        // Prevent any possible infinite recursion
        // for bad default Locale IDs
        return init("en");
    }
}

int32_t
Locale::hashCode() const 
{
    UHashTok hashKey;
    hashKey.pointer = fullName;
    return uhash_hashChars(hashKey);
}


const Locale&
Locale::getDefault() 
{
#ifdef ICU_LOCID_USE_DEPRECATES
    return fgDefaultLocale;
#else
    return getLocale(eDEFAULT);
#endif
}

void 
Locale::setDefault( const   Locale&     newLocale, 
                            UErrorCode&  status) 
{
    if (U_FAILURE(status))
        return;

#ifdef ICU_LOCID_USE_DEPRECATES
    fgDefaultLocale = newLocale;
#else
    if (gLocaleCache == NULL) {
        initLocaleCache();
    }

    {
        Mutex lock;
        gLocaleCache[eDEFAULT] = newLocale;
    }
#endif
}

Locale
Locale::createFromName (const char *name)
{
    if (name) {
        Locale l;
        l.init(name);
        return l;
    }
    else {
        return getDefault();
    }
}


const char *
Locale::getISO3Language() const
{
    return uloc_getISO3Language(fullName);
}


const char *
Locale::getISO3Country() const
{
    return uloc_getISO3Country(fullName);
}

/**
 * Return the LCID value as specified in the "LocaleID" resource for this
 * locale.  The LocaleID must be expressed as a hexadecimal number, from
 * one to four digits.  If the LocaleID resource is not present, or is
 * in an incorrect format, 0 is returned.  The LocaleID is for use in
 * Windows (it is an LCID), but is available on all platforms.
 */
uint32_t 
Locale::getLCID() const
{
    return uloc_getLCID(fullName);
}

UnicodeString& 
Locale::getDisplayLanguage(UnicodeString& dispLang) const
{
    return this->getDisplayLanguage(getDefault(), dispLang);
}

/*We cannot make any assumptions on the size of the output display strings
* Yet, since we are calling through to a C API, we need to set limits on
* buffer size. For all the following getDisplay functions we first attempt
* to fill up a stack allocated buffer. If it is to small we heap allocated
* the exact buffer we need copy it to the UnicodeString and delete it*/

UnicodeString&
Locale::getDisplayLanguage(const Locale &displayLocale,
                           UnicodeString &result) const {
    UChar *buffer;
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;

    buffer=result.getBuffer(ULOC_FULLNAME_CAPACITY);
    if(buffer==0) {
        result.truncate(0);
        return result;
    }

    length=uloc_getDisplayLanguage(fullName, displayLocale.fullName,
                                   buffer, result.getCapacity(),
                                   &errorCode);
    result.releaseBuffer(length);

    if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
        buffer=result.getBuffer(length);
        if(buffer==0) {
            result.truncate(0);
            return result;
        }
        errorCode=U_ZERO_ERROR;
        length=uloc_getDisplayLanguage(fullName, displayLocale.fullName,
                                       buffer, result.getCapacity(),
                                       &errorCode);
        result.releaseBuffer(length);
    }

    if(U_FAILURE(errorCode)) {
        result.truncate(0);
    }

    return result;
}

UnicodeString& 
Locale::getDisplayCountry(UnicodeString& dispCntry) const
{
    return this->getDisplayCountry(getDefault(), dispCntry);
}

UnicodeString&
Locale::getDisplayCountry(const Locale &displayLocale,
                          UnicodeString &result) const {
    UChar *buffer;
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;

    buffer=result.getBuffer(ULOC_FULLNAME_CAPACITY);
    if(buffer==0) {
        result.truncate(0);
        return result;
    }

    length=uloc_getDisplayCountry(fullName, displayLocale.fullName,
                                  buffer, result.getCapacity(),
                                  &errorCode);
    result.releaseBuffer(length);

    if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
        buffer=result.getBuffer(length);
        if(buffer==0) {
            result.truncate(0);
            return result;
        }
        errorCode=U_ZERO_ERROR;
        length=uloc_getDisplayCountry(fullName, displayLocale.fullName,
                                      buffer, result.getCapacity(),
                                      &errorCode);
        result.releaseBuffer(length);
    }

    if(U_FAILURE(errorCode)) {
        result.truncate(0);
    }

    return result;
}

UnicodeString& 
Locale::getDisplayVariant(UnicodeString& dispVar) const
{
    return this->getDisplayVariant(getDefault(), dispVar);
}

UnicodeString&
Locale::getDisplayVariant(const Locale &displayLocale,
                          UnicodeString &result) const {
    UChar *buffer;
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;

    buffer=result.getBuffer(ULOC_FULLNAME_CAPACITY);
    if(buffer==0) {
        result.truncate(0);
        return result;
    }

    length=uloc_getDisplayVariant(fullName, displayLocale.fullName,
                                  buffer, result.getCapacity(),
                                  &errorCode);
    result.releaseBuffer(length);

    if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
        buffer=result.getBuffer(length);
        if(buffer==0) {
            result.truncate(0);
            return result;
        }
        errorCode=U_ZERO_ERROR;
        length=uloc_getDisplayVariant(fullName, displayLocale.fullName,
                                      buffer, result.getCapacity(),
                                      &errorCode);
        result.releaseBuffer(length);
    }

    if(U_FAILURE(errorCode)) {
        result.truncate(0);
    }

    return result;
}

UnicodeString& 
Locale::getDisplayName( UnicodeString& name ) const
{
    return this->getDisplayName(getDefault(), name);
}

UnicodeString&
Locale::getDisplayName(const Locale &displayLocale,
                       UnicodeString &result) const {
    UChar *buffer;
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t length;

    buffer=result.getBuffer(ULOC_FULLNAME_CAPACITY);
    if(buffer==0) {
        result.truncate(0);
        return result;
    }

    length=uloc_getDisplayName(fullName, displayLocale.fullName,
                               buffer, result.getCapacity(),
                               &errorCode);
    result.releaseBuffer(length);

    if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
        buffer=result.getBuffer(length);
        if(buffer==0) {
            result.truncate(0);
            return result;
        }
        errorCode=U_ZERO_ERROR;
        length=uloc_getDisplayName(fullName, displayLocale.fullName,
                                   buffer, result.getCapacity(),
                                   &errorCode);
        result.releaseBuffer(length);
    }

    if(U_FAILURE(errorCode)) {
        result.truncate(0);
    }

    return result;
}
const Locale*
Locale::getAvailableLocales(int32_t& count) 
{
    // for now, there is a hardcoded list, so just walk through that list and set it up.
    if (availableLocaleList == 0) {
        int32_t locCount = uloc_countAvailable();
        Locale *newLocaleList = new Locale[locCount];

        count = locCount;

        while(--locCount >= 0) {
            newLocaleList[locCount].setFromPOSIXID(uloc_getAvailable(locCount));
        }

        Mutex mutex;
        if(availableLocaleList != 0) {
            delete []newLocaleList;
        }
        else {
            availableLocaleListCount = count;
            availableLocaleList = newLocaleList;
        }
    }
    count = availableLocaleListCount;
    return availableLocaleList;
}

const char* const* Locale::getISOCountries()
{
    return uloc_getISOCountries();
}

const char* const* Locale::getISOLanguages()
{
    return uloc_getISOLanguages();
}

// Set the locale's data based on a posix id. 
void Locale::setFromPOSIXID(const char *posixID)
{
    init(posixID);
}

#ifndef ICU_LOCID_USE_DEPRECATES
const Locale &
Locale::getEnglish(void)
{
    return getLocale(eENGLISH);
}

const Locale &
Locale::getFrench(void)
{
    return getLocale(eFRENCH);
}

const Locale &
Locale::getGerman(void)
{
    return getLocale(eGERMAN);
}

const Locale &
Locale::getItalian(void)
{
    return getLocale(eITALIAN);
}

const Locale &
Locale::getJapanese(void)
{
    return getLocale(eJAPANESE);
}

const Locale &
Locale::getKorean(void)
{
    return getLocale(eKOREAN);
}

const Locale &
Locale::getChinese(void)
{
    return getLocale(eCHINESE);
}

const Locale &
Locale::getSimplifiedChinese(void)
{
    return getLocale(eCHINESE);
}

const Locale &
Locale::getTraditionalChinese(void)
{
    return getLocale(eTAIWAN);
}


const Locale &
Locale::getFrance(void)
{
    return getLocale(eFRANCE);
}

const Locale &
Locale::getGermany(void)
{
    return getLocale(eGERMANY);
}

const Locale &
Locale::getItaly(void)
{
    return getLocale(eITALY);
}

const Locale &
Locale::getJapan(void)
{
    return getLocale(eJAPAN);
}

const Locale &
Locale::getKorea(void)
{
    return getLocale(eKOREA);
}

const Locale &
Locale::getChina(void)
{
    return getLocale(eCHINESE);
}

const Locale &
Locale::getPRC(void)
{
    return getLocale(eCHINESE);
}

const Locale &
Locale::getTaiwan(void)
{
    return getLocale(eTAIWAN);
}

const Locale &
Locale::getUK(void)
{
    return getLocale(eUK);
}

const Locale &
Locale::getUS(void)
{
    return getLocale(eUS);
}

const Locale &
Locale::getCanada(void)
{
    return getLocale(eCANADA);
}

const Locale &
Locale::getCanadaFrench(void)
{
    return getLocale(eFRENCH);
}

const Locale &
Locale::getLocale(int locid)
{
    if (gLocaleCache) {
        return gLocaleCache[locid];
    }

    initLocaleCache();

    return gLocaleCache[locid];
}

/*
This function is defined this way in order to get around static
initialization and static destruction.
 */
void
Locale::initLocaleCache(void)
{
    const char *defaultLocale = uprv_getDefaultLocaleID();
    // Can't use empty parameters, or we'll call this function again.
    Locale newLocales[] = {
        Locale("en"),
        Locale("fr"),
        Locale("de"),
        Locale("it"),
        Locale("ja"),
        Locale("ko"),
        Locale("zh"),
        Locale("fr", "FR"),
        Locale("de", "DE"),
        Locale("it", "IT"),
        Locale("ja", "JP"),
        Locale("ko", "KR"),
        Locale("zh", "CN"),
        Locale("zh", "TW"),
        Locale("en", "GB"),
        Locale("en", "US"),
        Locale("en", "CA"),
        Locale("fr", "CA"),
        Locale(defaultLocale)
    };
    Locale *localeCache = (Locale *)(gByteLocaleCache);

    {
        Mutex lock;
        if (gLocaleCache != NULL) {
            return;
        }
        uprv_memcpy(gByteLocaleCache, newLocales, sizeof(newLocales));

        for (int idx = 0; idx < eMAX_LOCALES; idx++)
        {
            if (localeCache[idx].fullName == newLocales[idx].fullNameBuffer)
            {
                localeCache[idx].fullName = localeCache[idx].fullNameBuffer;
            }
            else
            {
                // Since we did a memcpy we need to make sure that the local
                // Locales do not destroy the memory of the permanent locales.
                //
                // This can be a memory leak for an extra long default locale,
                // but this code shouldn't normally get executed.
                localeCache[idx].fullName = new char[uprv_strlen(localeCache[idx].fullNameBuffer) + 1];
                uprv_strcpy(localeCache[idx].fullName, localeCache[idx].fullNameBuffer);
            }
        }
        gLocaleCache = localeCache;
    }
}

#endif

//eof
U_NAMESPACE_END
