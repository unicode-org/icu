/*
 **********************************************************************
 *   Copyright (C) 1997-2003, International Business Machines
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
#include "umutex.h"
#include "uassert.h"
#include "cmemory.h"
#include "cstring.h"
#include "uhash.h"
#include "ucln_cmn.h"

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


    //eDEFAULT,
    eMAX_LOCALES
} ELocalePos;

/* Use void * to make it properly aligned */
/* Add 1 for rounding */
// static void *gByteLocaleCache[(eMAX_LOCALES + 1) * sizeof(Locale) / sizeof(void*)];

static Locale *gLocaleCache   = NULL;
static Locale *gDefaultLocale = NULL;

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
        delete [] gLocaleCache;
        gLocaleCache = NULL;
    }
    if (gDefaultLocale) {
        delete gDefaultLocale;
        gDefaultLocale = NULL;
    }
    return TRUE;
}

U_NAMESPACE_BEGIN
const char Locale::fgClassID=0;

void locale_set_default_internal(const char *id)
{
    U_NAMESPACE_USE
    Locale tempLocale(Locale::eBOGUS);

    if (id == NULL) 
    {
        umtx_lock(NULL);
        id = uprv_getDefaultLocaleID();
        umtx_unlock(NULL);
    }

    tempLocale.init(id);   // Note:  we do not want to hold the mutex through init(),
                           //        which is a relatively large, complex function.
                           //        Hence, the use of a temporary locale.
    const Locale *defLocale = &Locale::getDefault();
    
    umtx_lock(NULL);
    Locale *ncDefLocale = (Locale *)defLocale;
    *ncDefLocale = tempLocale;
    umtx_unlock(NULL);
}
U_NAMESPACE_END

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

Locale::~Locale()
{   
    /*if fullName is on the heap, we free it*/
    if (fullName != fullNameBuffer) 
    {
        uprv_free(fullName);
        fullName = NULL;
    }
}

Locale::Locale()
    : UObject(), fullName(fullNameBuffer)
{
    init(NULL);
}

Locale::Locale(Locale::ELocaleType t) 
    : UObject(), fullName(fullNameBuffer)
{
    setToBogus();
}


Locale::Locale( const   char * newLanguage, 
                const   char * newCountry, 
                const   char * newVariant) 
    : UObject(), fullName(fullNameBuffer)
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
            togo_heap = (char *)uprv_malloc(sizeof(char)*(size+1));
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

        if (togo_heap) {
            uprv_free(togo_heap);
        }
    }
}

Locale::Locale(const Locale &other)
    : UObject(other), fullName(fullNameBuffer)
{
    *this = other;
}

Locale &Locale::operator=(const Locale &other)
{
    if (this == &other) {
        return *this;
    }

    if (&other == NULL) {
        this->setToBogus();
        return *this;
    }

    /* Free our current storage */
    if(fullName != fullNameBuffer) {
        uprv_free(fullName);
        fullName = fullNameBuffer;
    }

    /* Allocate the full name if necessary */
    if(other.fullName != other.fullNameBuffer) {
        fullName = (char *)uprv_malloc(sizeof(char)*(uprv_strlen(other.fullName)+1));
    }

    /* Copy the full name */
    uprv_strcpy(fullName, other.fullName);

    /* Copy the language and country fields */
    uprv_strcpy(language, other.language);
    uprv_strcpy(country, other.country);

    /* The variantBegin is an offset into fullName, just copy it */
    variantBegin = other.variantBegin;
    fIsBogus = other.fIsBogus;
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
    fIsBogus = FALSE;
    /* Free our current storage */
    if(fullName != fullNameBuffer) {
        uprv_free(fullName);
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
            return *this = getDefault();
        }

        // "canonicalize" the locale ID to ICU/Java format
        err = U_ZERO_ERROR;
        length = uloc_getName(localeID, fullName, sizeof(fullNameBuffer), &err);
        if(U_FAILURE(err) || err == U_STRING_NOT_TERMINATED_WARNING) {
            /*Go to heap for the fullName if necessary*/
            fullName = (char *)uprv_malloc(sizeof(char)*(length + 1));
            if(fullName == 0) {
                fullName = fullNameBuffer;
                break; // error: out of memory
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
        variantBegin = (int32_t)uprv_strlen(fullName);

        /* after uloc_getName() we know that only '_' are separators */
        separator = uprv_strchr(fullName, SEP_CHAR);
        if(separator != 0) {
            /* there is a country field */
            length = (int32_t)(separator - fullName);
            if(length > 0) {
                if(length >= (int32_t)sizeof(language)) {
                    break; // error: language code too long
                }
                uprv_memcpy(language, fullName, length);
            }
            language[length] = 0;

            prev = separator + 1;
            separator = uprv_strchr(prev, SEP_CHAR);
            if(separator != 0) {
                /* there is a variant field */
                length = (int32_t)(separator - prev);
                if(length > 0) {
                    if(length >= (int32_t)sizeof(country)) {
                        break; // error: country code too long
                    }
                    uprv_memcpy(country, prev, length);
                }
                country[length] = 0;

                variantBegin = (int32_t)((separator + 1) - fullName);
            } else {
                /* variantBegin==strlen(fullName), length==strlen(language)==prev-1-fullName */
                if((variantBegin - length - 1) >= (int32_t)sizeof(country)) {
                    break; // error: country code too long
                }
                uprv_strcpy(country, prev);
            }
        } else {
            /* variantBegin==strlen(fullName) */
            if(variantBegin >= (int32_t)sizeof(language)) {
                break; // error: language code too long
            }
            uprv_strcpy(language, fullName);
        }

        // successful end of init()
        return *this;
    } while(0);

    // when an error occurs, then set this object to "bogus" (there is no UErrorCode here)
    setToBogus();

    return *this;
}

int32_t
Locale::hashCode() const 
{
    UHashTok hashKey;
    hashKey.pointer = fullName;
    return uhash_hashChars(hashKey);
}

void 
Locale::setToBogus() {
  /* Free our current storage */
  if(fullName != fullNameBuffer) {
      uprv_free(fullName);
      fullName = fullNameBuffer;
  }
  *fullNameBuffer = 0;
  *language = 0;
  *country = 0;
  fIsBogus = TRUE;
}

const Locale&
Locale::getDefault() 
{
    umtx_lock(NULL);
    UBool needInit = (gDefaultLocale == NULL);
    umtx_unlock(NULL);
    if (needInit) {
        Locale *tLocale = new Locale(Locale::eBOGUS);
        if (tLocale != NULL) {
            const char *cLocale;

            umtx_lock(NULL);
            /* uprv_getDefaultLocaleID is not thread safe, so we surround it with a mutex */
            cLocale = uprv_getDefaultLocaleID();
            umtx_unlock(NULL);

            tLocale->init(cLocale);
            umtx_lock(NULL);
            if (gDefaultLocale == NULL) {
                gDefaultLocale = tLocale;
                tLocale = NULL;
            }
            umtx_unlock(NULL);
            delete tLocale;
        }
    }
    return *gDefaultLocale;
}

void 
Locale::setDefault( const   Locale&     newLocale, 
                            UErrorCode&  status) 
{
    if (U_FAILURE(status))
        return;
    
    const Locale *defLocale = &Locale::getDefault();
    umtx_lock(NULL);
    Locale *ncDefLocale = (Locale *)defLocale;
    *ncDefLocale = newLocale;
    umtx_unlock(NULL);
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
    umtx_lock(NULL);
    UBool needInit = availableLocaleList == 0;
    umtx_unlock(NULL);
    
    if (needInit) {
        int32_t locCount = uloc_countAvailable();
        Locale *newLocaleList = 0;
        if(locCount) {
           newLocaleList = new Locale[locCount];
        }
        if (newLocaleList == NULL) {
            return NULL;
        }
      
        count = locCount;
      
        while(--locCount >= 0) {
            newLocaleList[locCount].setFromPOSIXID(uloc_getAvailable(locCount));
        }
      
        umtx_lock(NULL);
        if(availableLocaleList == 0) {
            availableLocaleListCount = count;
            availableLocaleList = newLocaleList;
            newLocaleList = NULL;
        }
        umtx_unlock(NULL);
        delete []newLocaleList;
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
    return getLocale(eCHINA);
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
    return getLocale(eCHINA);
}

const Locale &
Locale::getPRC(void)
{
    return getLocale(eCHINA);
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
    return getLocale(eCANADA_FRENCH);
}

const Locale &
Locale::getLocale(int locid)
{
    Locale *localeCache = getLocaleCache();
    U_ASSERT(locid < eMAX_LOCALES);
    if (localeCache == NULL) {
        // Failure allocating the locale cache.
        //   The best we can do is return a NULL reference.
        locid = 0;
    }
    return localeCache[locid];
}

/*
This function is defined this way in order to get around static
initialization and static destruction.
 */
Locale *
Locale::getLocaleCache(void)
{
    umtx_lock(NULL);
    UBool needInit = (gLocaleCache == NULL);
    umtx_unlock(NULL);
    
    if (needInit) {
        Locale *tLocaleCache = new Locale[eMAX_LOCALES];
        if (tLocaleCache == NULL) {
            return NULL;
        }
        tLocaleCache[eENGLISH]       = Locale("en");
        tLocaleCache[eFRENCH]        = Locale("fr");
        tLocaleCache[eGERMAN]        = Locale("de");
        tLocaleCache[eITALIAN]       = Locale("it");
        tLocaleCache[eJAPANESE]      = Locale("ja");
        tLocaleCache[eKOREAN]        = Locale("ko");
        tLocaleCache[eCHINESE]       = Locale("zh");
        tLocaleCache[eFRANCE]        = Locale("fr", "FR");
        tLocaleCache[eGERMANY]       = Locale("de", "DE");
        tLocaleCache[eITALY]         = Locale("it", "IT");
        tLocaleCache[eJAPAN]         = Locale("ja", "JP");
        tLocaleCache[eKOREA]         = Locale("ko", "KR");
        tLocaleCache[eCHINA]         = Locale("zh", "CN");
        tLocaleCache[eTAIWAN]        = Locale("zh", "TW");
        tLocaleCache[eUK]            = Locale("en", "GB");
        tLocaleCache[eUS]            = Locale("en", "US");
        tLocaleCache[eCANADA]        = Locale("en", "CA");
        tLocaleCache[eCANADA_FRENCH] = Locale("fr", "CA");
        
        umtx_lock(NULL);
        if (gLocaleCache == NULL) {
            gLocaleCache = tLocaleCache;
            tLocaleCache = NULL;
        }
        umtx_unlock(NULL);
        if (tLocaleCache) {
            delete [] tLocaleCache;  // Fancy array delete will destruct each member.
        }
    }
    return gLocaleCache;
}

//eof
U_NAMESPACE_END
