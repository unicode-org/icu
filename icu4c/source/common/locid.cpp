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
#include "unicode/resbund.h"
#include "uresimp.h"
#include "mutex.h"
#include "unicode/unicode.h"
#include "cmemory.h"
#include "cstring.h"

/*Character separating the posix id fields*/
// '_'
#define SEP_UCHAR 0x005F
// In the platform codepage.
#define SEP_CHAR '_'

/**
 * static variables
 */
Locale*  availableLocaleList = NULL;
int32_t  availableLocaleListCount;

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

#define LOCALE_CACHE_SIZE (eMAX_LOCALES * sizeof(Locale))
uint8_t gByteLocaleCache[LOCALE_CACHE_SIZE];

Locale *gLocaleCache = NULL;

Locale::LocaleProxy::operator const class Locale&(void) const
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
    }
}

Locale::Locale()
{
    init(uloc_getDefault());
}

Locale::Locale( const   char * newLanguage, 
                const   char * newCountry, 
                const   char * newVariant) 
{
    char togo_stack[ULOC_FULLNAME_CAPACITY];
    char *togo;
    char *togo_heap = NULL;
    int32_t size = 0;
    int32_t lsize = 0;
    int32_t csize = 0;
    int32_t vsize = 0;
    char    *p;

    if( (newLanguage==NULL) && (newCountry == NULL) && (newVariant == NULL) )
    {
        init(NULL); /* shortcut */
    }
    else
    {
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

Locale::Locale(const    Locale& other)

{
    int j;
    /*Copy the language and country fields*/
    uprv_strcpy(language, other.language);
    uprv_strcpy(country, other.country);

    /*make fullName point to the heap if necessary*/
    if ((j=(int)uprv_strlen(other.fullName)) > ULOC_FULLNAME_CAPACITY)
    {
        fullName = new char[j+1];
    }
    else
        fullName = fullNameBuffer;

    uprv_strcpy(fullName, other.fullName);

    /*Make the variant point to the same offset as the copied*/
    variantBegin = other.variantBegin;
}

UBool
Locale::operator==( const   Locale& other) const
{
    if (uprv_strcmp(other.fullName, fullName) == 0)
    {
        return TRUE;
    }

    return FALSE;
}

/*This function initializes a Locale from a C locale ID*/
Locale& Locale::init(const char* localeID)
{
    int k,l;
    UErrorCode err = U_ZERO_ERROR;

    if (localeID == NULL)
        localeID = uloc_getDefault();

    l = uloc_getLanguage(localeID, 
        this->language,
        ULOC_LANG_CAPACITY,
        &err);

    l += k = uloc_getCountry(localeID,
        this->country,
        ULOC_COUNTRY_CAPACITY,
        &err);

    l--; //adjust for the 2 zero terminators

    /*Go to heap for the fullName if necessary*/
    int j;
    if ((j=(int)uprv_strlen(localeID)) > ULOC_FULLNAME_CAPACITY)
    {
        this->fullName = new char[j+1];
    }
    else {
        this->fullName = this->fullNameBuffer;
    }

    uprv_strcpy(this->fullName, localeID);

    /*Setting up the variant:
    -point to the zero terminator of fullName if there is none
    -point to the first character of the variant if ther is one
    */
    if (k > 1)
    {
        if (this->fullName[l] == '\0')
        {
            this->variantBegin = l;
        }
        else
        {
            int32_t varLength;
            UErrorCode intErr = U_ZERO_ERROR;
            varLength = uloc_getVariant(this->fullName, NULL, 0, &intErr);

            if((U_FAILURE(intErr) && (intErr != U_BUFFER_OVERFLOW_ERROR)) || (varLength <= 0))
            {   /* bail  - point at the null*/
                this->variantBegin = j;
            }
            else
            {
                /* variant is at the end. We just don't know where exactly it might be. */
                this->variantBegin = j - varLength + 1 ;
            }
        }
    }
    else
        this->variantBegin = l - 1;

    return *this;
}

Locale& Locale::operator=(const Locale& other)
{
    uprv_strcpy(language, other.language);
    uprv_strcpy(country, other.country);
    if (other.fullName == other.fullNameBuffer)
    {
        fullName = fullNameBuffer;
    }
    else 
    {
    /*In case the assigner has some of its data on the heap
        * we need to do the same*/
        if (fullName != fullNameBuffer)
            delete []fullName;
        fullName = new char[(uprv_strlen(other.fullName)+1)];
    }
    uprv_strcpy(fullName, other.fullName);
    /*Make the variant point to the same offset as the assigner*/
    variantBegin = other.variantBegin;

    return *this;
}

int32_t
Locale::hashCode() const 
{
    return UnicodeString(fullName, "").hashCode();
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


void locale_set_default_internal(const char *id)
{
#ifdef ICU_LOCID_USE_DEPRECATES
    Locale::fgDefaultLocale.init(id);
#else
    if (gLocaleCache == NULL) {
        Locale::initLocaleCache();
    }

    gLocaleCache[eDEFAULT].init(id);
#endif
}

/* sfb 07/21/99 */
U_CFUNC void
locale_set_default(const char *id)
{
    locale_set_default_internal(id);
}
/* end */


void 
Locale::setDefault( const   Locale&     newLocale, 
                            UErrorCode&  status) 
{
    if (U_FAILURE(status))
        return;

    uloc_setDefault(newLocale.fullName, &status);

#ifdef ICU_LOCID_USE_DEPRECATES
    fgDefaultLocale = newLocale;
#else
    if (gLocaleCache == NULL) {
        initLocaleCache();
    }

    gLocaleCache[eDEFAULT] = newLocale;
#endif
}

Locale
Locale::createFromName (const char *name)
{
    UErrorCode status = U_ZERO_ERROR;
    char stack[ULOC_FULLNAME_CAPACITY];
    char *heap = NULL;
    char *buf = stack;
    int32_t buflen = ULOC_FULLNAME_CAPACITY;
    int32_t namelen = (int32_t)uprv_strlen(name);

    /* for some reason */
    if(namelen > buflen) {
        buflen = namelen+1;
        heap = (char*)uprv_malloc(buflen);
        buf = heap;
    }

    uloc_getName(name, buf, buflen, &status);

    Locale l(buf);
    if(heap != NULL)
    {
        free(heap);
    }
    return l;
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
Locale::getDisplayLanguage( const   Locale&         inLocale,
                UnicodeString&  dispLang) const
{
    UErrorCode status = U_ZERO_ERROR;
    UChar bufBuffer[ULOC_FULLNAME_CAPACITY];
    UChar* buf = bufBuffer;
    
    //  dispLang = "result";
    //  return dispLang;
    int size = uloc_getDisplayLanguage(fullName,
        inLocale.fullName,
        buf,
        ULOC_FULLNAME_CAPACITY,
        &status);
    
    
    if (status == U_BUFFER_OVERFLOW_ERROR)
    {
        status = U_ZERO_ERROR;
        buf = new UChar[size];
        
        uloc_getDisplayLanguage(fullName,
            inLocale.fullName,
            buf,
            size,
            &status);
        
    }
    
    dispLang = buf;
    
    if (buf != bufBuffer)
        delete []buf;
    
    return dispLang;
}

UnicodeString& 
Locale::getDisplayCountry(UnicodeString& dispCntry) const
{
    return this->getDisplayCountry(getDefault(), dispCntry);
}

UnicodeString& 
Locale::getDisplayCountry(  const   Locale&         inLocale,
                                    UnicodeString&  dispCntry) const
{
    UErrorCode status = U_ZERO_ERROR;
    UChar bufBuffer[ULOC_FULLNAME_CAPACITY];
    UChar* buf = bufBuffer;

    int size = uloc_getDisplayCountry(fullName,
        inLocale.fullName,
        buf,
        ULOC_FULLNAME_CAPACITY,
        &status);

    if (status == U_BUFFER_OVERFLOW_ERROR)
    {
        status = U_ZERO_ERROR;
        buf = new UChar[size];
        uloc_getDisplayCountry(fullName,
            inLocale.fullName,
            buf,
            size,
            &status);
    }

    dispCntry = buf;

    if (buf != bufBuffer)
        delete []buf;

    return dispCntry;
}

UnicodeString& 
Locale::getDisplayVariant(UnicodeString& dispVar) const
{
    return this->getDisplayVariant(getDefault(), dispVar);
}

UnicodeString& Locale::getDisplayVariant(const Locale& inLocale,
                     UnicodeString& dispVar) const
{
    UErrorCode status = U_ZERO_ERROR;
    UChar bufBuffer[ULOC_FULLNAME_CAPACITY];
    UChar* buf = bufBuffer;

    int size = uloc_getDisplayVariant(fullName,
        inLocale.fullName,
        buf,
        ULOC_FULLNAME_CAPACITY,
        &status);

    if (status == U_BUFFER_OVERFLOW_ERROR)
    {
        status = U_ZERO_ERROR;
        buf = new UChar[size];
        uloc_getDisplayVariant(fullName,
            inLocale.fullName,
            buf,
            size,
            &status);
    }

    dispVar = buf;

    if (buf != bufBuffer)
        delete []buf;

    return dispVar;
}

UnicodeString& 
Locale::getDisplayName( UnicodeString& name ) const
{
    return this->getDisplayName(getDefault(), name);
}

UnicodeString& 
Locale::getDisplayName( const   Locale&     inLocale,
            UnicodeString& result) const
{
    UErrorCode status = U_ZERO_ERROR;
    UChar bufBuffer[ULOC_FULLNAME_CAPACITY];
    UChar* buf = bufBuffer;

    int size = uloc_getDisplayName(fullName,
        inLocale.fullName,
        buf,
        ULOC_FULLNAME_CAPACITY,
        &status);

    if (status == U_BUFFER_OVERFLOW_ERROR)
    {
        status = U_ZERO_ERROR;
        
        buf = new UChar[size];
        uloc_getDisplayName(fullName,
            inLocale.fullName,
            buf,
            size,
            &status);
    }

    result = buf;

    if (buf != bufBuffer) {
        delete []buf;
    }

    return result;
}

const Locale*
Locale::getAvailableLocales(int32_t& count) 
{
    // for now, there is a hardcoded list, so just walk through that list and set it up.
    if (availableLocaleList == 0) {
        UErrorCode status = U_ZERO_ERROR;
        ResourceBundle index(UnicodeString(""), Locale(kIndexLocaleName), status);
        ResourceBundle locales = index.get(kIndexTag, status);

        char name[96];
        locales.resetIterator();

        count = locales.getSize();

        Locale *newLocaleList = new Locale[count];

        int32_t i = 0;
        UnicodeString temp;
        while(locales.hasNext()) {
            temp = locales.getNextString(status);
            temp.extract(0, temp.length(), name);
            name[temp.length()] = '\0';
            newLocaleList[i++].setFromPOSIXID(name);
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
        Locale()    // This can use a mutex
    };
    Locale *localeCache = reinterpret_cast<Locale *>(gByteLocaleCache);

    {
        Mutex lock;
        if (gLocaleCache != NULL) {
            return;
        }
        uprv_memcpy(gByteLocaleCache, newLocales ,sizeof(newLocales));

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
                // This can be a memory leak, but this code shouldn't normally
                // get executed.
                localeCache[idx].fullName = new char[uprv_strlen(localeCache[idx].fullNameBuffer) + 1];
                uprv_strcpy(localeCache[idx].fullName, localeCache[idx].fullNameBuffer);
            }
        }
        gLocaleCache = localeCache;
    }
}

#endif

//eof


