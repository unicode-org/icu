/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1999      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
* File ULOC.H
*
* Modification History:
*
*   Date        Name        Description
*   04/01/97    aliu        Creation.
*   08/22/98    stephen     JDK 1.2 sync.
*   12/08/98    rtg         New C API for Locale
*   03/30/99    damiba      overhaul
*   03/31/99    helena      Javadoc for uloc functions.
*   04/15/99    Madhu       Updated Javadoc
********************************************************************************
*/

#ifndef ULOC_H
#define ULOC_H

#include "utypes.h"

/**    
 *
 * A <code>Locale</code> represents a specific geographical, political,
 * or cultural region. An operation that requires a <code>Locale</code> to perform
 * its task is called <em>locale-sensitive</em> and uses the <code>Locale</code>
 * to tailor information for the user. For example, displaying a number
 * is a locale-sensitive operation--the number should be formatted
 * according to the customs/conventions of the user's native country,
 * region, or culture.  In the C APIs, a locales is simply a const char string.
 *
 * <P>
 * You create a <code>Locale</code> with one of the three options listed below.
 * Each of the component is separated by '_' in the locale string.
 * <blockquote>
 * <pre>
 * .      newLanguage
 * .
 * .      newLanguage + newCountry
 * .
 * .      newLanguage + newCountry + newVariant
 * </pre>
 * </blockquote>
 * The first option is a valid <STRONG>ISO
 * Language Code.</STRONG> These codes are the lower-case two-letter
 * codes as defined by ISO-639.
 * You can find a full list of these codes at a number of sites, such as:
 * <BR><a href ="http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt">
 * <code>http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt</code></a>
 *
 * <P>
 * The second option includes an additonal <STRONG>ISO Country
 * Code.</STRONG> These codes are the upper-case two-letter codes
 * as defined by ISO-3166.
 * You can find a full list of these codes at a number of sites, such as:
 * <BR><a href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html">
 * <code>http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html</code></a>
 *
 * <P>
 * The third option requires another additonal information--the 
 * <STRONG>Variant.</STRONG>
 * The Variant codes are vendor and browser-specific.
 * For example, use WIN for Windows, MAC for Macintosh, and POSIX for POSIX.
 * Where there are two variants, separate them with an underscore, and
 * put the most important one first. For
 * example, a Traditional Spanish collation might be referenced, with
 * "ES", "ES", "Traditional_WIN".
 *
 * <P>
 * Because a <code>Locale</code> is just an identifier for a region,
 * no validity check is performed when you specify a <code>Locale</code>.
 * If you want to see whether particular resources are available for the
 * <code>Locale</code> you asked for, you must query those resources. For
 * example, ask the <code>UNumberFormat</code> for the locales it supports
 * using its <code>getAvailable</code> method.
 * <BR><STRONG>Note:</STRONG> When you ask for a resource for a particular
 * locale, you get back the best available match, not necessarily
 * precisely what you asked for. For more information, look at
 * <a href="ures.html"><code>UResourceBundle</code></a>.
 *
 * <P>
 * The <code>Locale</code> provides a number of convenient constants
 * that you can use to specify the commonly used
 * locales. For example, the following refers to a locale
 * for the United States:
 * <blockquote>
 * <pre>
 * .      ULOC_US
 * </pre>
 * </blockquote>
 *
 * <P>
 * Once you've specified a locale you can query it for information about
 * itself. Use <code>uloc_getCountry</code> to get the ISO Country Code and
 * <code>uloc_getLanguage</code> to get the ISO Language Code. You can
 * use <code>uloc_getDisplayCountry</code> to get the
 * name of the country suitable for displaying to the user. Similarly,
 * you can use <code>uloc_getDisplayLanguage</code> to get the name of
 * the language suitable for displaying to the user. Interestingly,
 * the <code>uloc_getDisplayXXX</code> methods are themselves locale-sensitive
 * and have two versions: one that uses the default locale and one
 * that takes a locale as an argument and displays the name or country in
 * a language appropriate to that locale.
 *
 * <P>
 * The ICU provides a number of services that perform locale-sensitive
 * operations. For example, the <code>unum_xxx</code> functions format
 * numbers, currency, or percentages in a locale-sensitive manner. 
 * </P>
 * <blockquote>
 * <pre>
 * .    UErrorCode success = U_ZERO_ERROR;
 * .    UNumberFormat *nf;
 * .    const char* myLocale = "fr_FR";
 * .
 * .    nf = unum_open( UNUM_DEFAULT, NULL, success );          
 * .    unum_close(nf);
 * .    nf = unum_open( UNUM_CURRENCY, NULL, success );
 * .    unum_close(nf);
 * .    nf = unum_open( UNUM_PERCENT, NULL, success );   
 * .    unum_close(nf);
 * </pre>
 * </blockquote>
 * Each of these methods has two variants; one with an explicit locale
 * and one without; the latter using the default locale.
 * <blockquote>
 * <pre>
 * .
 * .    nf = unum_open( UNUM_DEFAULT, myLocale, success );          
 * .    unum_close(nf);
 * .    nf = unum_open( UNUM_CURRENCY, myLocale, success );
 * .    unum_close(nf);
 * .    nf = unum_open( UNUM_PERCENT, myLocale, success );   
 * .    unum_close(nf);
 * </pre>
 * </blockquote>
 * A <code>Locale</code> is the mechanism for identifying the kind of services
 * (<code>UNumberFormat</code>) that you would like to get. The locale is
 * <STRONG>just</STRONG> a mechanism for identifying these services.
 *
 * <P>
 * Each international serivce that performs locale-sensitive operations 
 * allows you
 * to get all the available objects of that type. You can sift
 * through these objects by language, country, or variant,
 * and use the display names to present a menu to the user.
 * For example, you can create a menu of all the collation objects
 * suitable for a given language. Such classes implement these
 * three class methods:
 * <blockquote>
 * <pre>
 * .      const char* uloc_getAvailable(int32_t index);
 * .      int32_t uloc_countAvailable();
 * .      int32_t
 * .      uloc_getDisplayName(const char* localeID,
 * .                const char* inLocaleID, 
 * .                UChar* result,
 * .                int32_t maxResultSize,
 * .                 UErrorCode* err);
 * .
 * </pre>
 * </blockquote>
 */

/**
 *
 * Useful constants for language.
 */
#define ULOC_ENGLISH "en"
#define ULOC_FRENCH "fr"
#define ULOC_GERMAN "de"
#define ULOC_ITALIAN "it"
#define ULOC_JAPANESE "ja"
#define ULOC_KOREAN "ko"
#define ULOC_CHINESE "zh"
#define ULOC_SIMPLIFIED_CHINESE "zh_CN"
#define ULOC_TRADITIONAL_CHINESE "zh_TW"

/**
 *
 * Useful constants for country.
 */
#define ULOC_FRANCE "fr_FR"
#define ULOC_GERMANY "de_DE"
#define ULOC_ITALY "it_IT"
#define ULOC_JAPAN "ja_JP"
#define ULOC_KOREA "ko_KR"
#define ULOC_CHINA "zh_CN"
#define ULOC_PRC "zh_CN"
#define ULOC_TAIWAN "zh_TW"
#define ULOC_UK "en_GB"
#define ULOC_US "en_US"
#define ULOC_CANADA "en_CA"
#define ULOC_CANADA_FRENCH "fr_CA"




/**
 *
 * Gets the system's default locale.
 * @return the system default locale
 */

U_CAPI const char* U_EXPORT2
uloc_getDefault(void);

/**
 *
 * Sets the system's default locale.
 * @param localeID the new system default locale
 * @param status the error information if the setting of default locale fails
 */
U_CAPI void U_EXPORT2
uloc_setDefault(const char* localeID,
        UErrorCode*       status);

/**
 *
 * Gets the language code for the specified locale.
 * @param localeID the locale to get the ISO langauge code with
 * @param language the langauge code for localeID
 * @param languageCapacity the size of the language buffer to store the  
 * language code with
 * @param err error information if retrieving the  language code failed
 * @return the actual buffer size needed for the  langauge code.  If it's greater 
 * than languageCapacity, the returned language code will be truncated.  
 */

U_CAPI int32_t U_EXPORT2
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err);

/**
 *
 * Gets the  country code for the specified locale.
 * @param localeID the locale to get the country code with
 * @param country the country code for localeID
 * @param languageCapacity the size of the coutry buffer to store the  
 * country code with
 * @param err error information if retrieving the country code failed
 * @return the actual buffer size needed for the country code.  If it's greater 
 * than countryCapacity, the returned country code will be truncated.  
 */

U_CAPI int32_t U_EXPORT2
uloc_getCountry(const char*    localeID,
        char* country,
        int32_t countryCapacity,
        UErrorCode* err);

/**
 *
 * Gets the variant code for the specified locale.
 * @param localeID the locale to get the variant code with
 * @param variant the variant code for localeID
 * @param variantCapacity the size of the variant buffer to store the 
 * variant code with
 * @param err error information if retrieving the variant code failed
 * @return the actual buffer size needed for the variant code.  If it's greater 
 * than variantCapacity, the returned variant code will be truncated.  
 */

U_CAPI int32_t U_EXPORT2
uloc_getVariant(const char*    localeID,
        char* variant,
        int32_t variantCapacity,
        UErrorCode* err);
/**
 *
 * Gets the full name for the specified locale.
 * @param localeID the locale to get the full name with
 * @param name the full name for localeID
 * @param nameCapacity the size of the name buffer to store the 
 * full name with
 * @param err error information if retrieving the full name failed
 * @return the actual buffer size needed for the full name.  If it's greater 
 * than nameCapacity, the returned full name will be truncated.  
 */

U_CAPI int32_t U_EXPORT2
uloc_getName(const char*    localeID,
         char* name,
         int32_t nameCapacity,
         UErrorCode* err);

/**
 *
 * Gets the ISO language code for the specified locale.
 * @param localeID the locale to get the ISO langauge code with
 * @return language the ISO langauge code for localeID
 */
U_CAPI const char* U_EXPORT2
uloc_getISO3Language(const char* localeID);


/**
 *
 * Gets the ISO country code for the specified locale.
 * @param localeID the locale to get the ISO country code with
 * @return country the ISO country code for localeID
 */

U_CAPI const char* U_EXPORT2
uloc_getISO3Country(const char* localeID);

/**
 *
 * Gets the Win32 LCID value for the specified locale.
 * @param localeID the locale to get the Win32 LCID value with
 * @return country the Win32 LCID for localeID
 */

U_CAPI uint32_t U_EXPORT2
uloc_getLCID(const char* localeID);

/**
 *
 * Gets the language name suitable for display for the specified locale.
 * @param localeID the locale to get the ISO langauge code with
 * @param language the displayable langauge code for localeID
 * @param languageCapacity the size of the language buffer to store the  
 * displayable language code with
 * @param err error information if retrieving the displayable language code failed
 * @return the actual buffer size needed for the displayable langauge code.  If it's greater 
 * than languageCapacity, the returned language code will be truncated.  
 */
U_CAPI int32_t U_EXPORT2
uloc_getDisplayLanguage(const char* locale,
            const char* inLocale,
            UChar* language,
            int32_t languageCapacity,
            UErrorCode* status);

/**
 *
 * Gets the country name suitable for display for the specified locale.
 * @param localeID the locale to get the displayable country code with
 * @param country the displayable country code for localeID
 * @param languageCapacity the size of the coutry buffer to store the  
 * displayable country code with
 * @param err error information if retrieving the displayable country code failed
 * @return the actual buffer size needed for the displayable country code.  If it's greater 
 * than countryCapacity, the returned displayable country code will be truncated.  
 */

U_CAPI int32_t U_EXPORT2
uloc_getDisplayCountry(const char* locale,
            const char* inLocale,
            UChar* country,
            int32_t countryCapacity,
            UErrorCode* status);    /* NULL may be used to specify the default */


/**
 *
 * Gets the variant code suitable for display for the specified locale.
 * @param localeID the locale to get the displayable variant code with
 * @param variant the displayable variant code for localeID
 * @param variantCapacity the size of the variant buffer to store the 
 * displayable variant code with
 * @param err error information if retrieving the displayable variant code failed
 * @return the actual buffer size needed for the displayable variant code.  If it's greater 
 * than variantCapacity, the returned displayable variant code will be truncated.  
 */
 
U_CAPI int32_t U_EXPORT2
uloc_getDisplayVariant(const char* locale,
            const char* inLocale,
               UChar* variant,
             int32_t variantCapacity,
            UErrorCode* status);    /* NULL may be used to specify the default */

/**
 *
 * Gets the full name suitable for display for the specified locale.
 * @param localeID the locale to get the displayable name with
 * @param variant the displayable name for localeID
 * @param variantCapacity the size of the name buffer to store the 
 * displayable full name with
 * @param err error information if retrieving the displayable name failed
 * @return the actual buffer size needed for the displayable name.  If it's greater 
 * than variantCapacity, the returned displayable name will be truncated.  
 */

U_CAPI int32_t U_EXPORT2
uloc_getDisplayName(const char* localeID,
            const char* inLocaleID, /* NULL may be used to specify the default */
            UChar* result,
            int32_t maxResultSize,
            UErrorCode* err);


/**
 *
 * Gets the specified locale from a list of all available locales.  
 * The return value is a pointer to an item of 
 * a locale name array.  Both this array and the pointers
 * it contains are owned by ICU and should not be deleted or written through
 * by the caller.  The locale name is terminated by a null pointer.
 * @param index the specific locale name index of the available locale list
 * @return a specified locale name of all available locales
 */
U_CAPI const char* U_EXPORT2
uloc_getAvailable(int32_t index);

/**
 *
 * Gets the size of the all available locale list.
 * @return the size of the locale list
 */
U_CAPI int32_t U_EXPORT2 uloc_countAvailable(void);

/**
 *
 * Gets a list of all available language codes defined in ISO 639.  This is a pointer
 * to an array of pointers to arrays of char.  All of these pointers are owned
 * by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 * @return a list of all available language codes
 */
U_CAPI const char* const* U_EXPORT2
uloc_getISOLanguages(void);

/**
 *
 * Gets a list of all available 2-letter country codes defined in ISO 639.  This is a
 * pointer to an array of pointers to arrays of char.  All of these pointers are
 * owned by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 * @return a list of all available country codes
 */
U_CAPI const char* const* U_EXPORT2
uloc_getISOCountries(void);

/**
 *
 * Gets the directory containing the locale data files.
 * @return the locale data file directory
 */
U_CAPI const char* U_EXPORT2
uloc_getDataDirectory(void);

/**
 *
 * Sets the directory containing the locale data files.
 * @return the new directory to fetch locale data from
 */

U_CAPI void U_EXPORT2
uloc_setDataDirectory(const char* newDirectory);

/*Internal function */
int32_t U_EXPORT2
uloc_getParent(const char*    localeID,
                 char* parent,
                 int32_t parentCapacity,
                 UErrorCode* err);

/*eof*/


#endif /*_ULOC*/



