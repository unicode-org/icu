/*
**********************************************************************
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
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

#include "unicode/utypes.h"

/**    
 * \file
 * \brief  C API: Locale 
 *
 * <h2> ULoc C API for Locale </h2>
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
 * \code
 *       newLanguage
 * 
 *       newLanguage + newCountry
 * 
 *       newLanguage + newCountry + newVariant
 * \endcode
 * </pre>
 * </blockquote>
 * The first option is a valid <STRONG>ISO
 * Language Code.</STRONG> These codes are the lower-case two-letter
 * codes as defined by ISO-639.
 * You can find a full list of these codes at a number of sites, such as:
 * <BR><a href ="http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt">
 * http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt</a>
 *
 * <P>
 * The second option includes an additonal <STRONG>ISO Country
 * Code.</STRONG> These codes are the upper-case two-letter codes
 * as defined by ISO-3166.
 * You can find a full list of these codes at a number of sites, such as:
 * <BR><a href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html">
 * http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html</a>
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
 * <code>UResourceBundle</code>.
 *
 * <P>
 * The <code>Locale</code> provides a number of convenient constants
 * that you can use to specify the commonly used
 * locales. For example, the following refers to a locale
 * for the United States:
 * <blockquote>
 * <pre>
 * \code
 *       ULOC_US
 * \endcode
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
 * \code
 *     UErrorCode success = U_ZERO_ERROR;
 *     UNumberFormat *nf;
 *     const char* myLocale = "fr_FR";
 * 
 *     nf = unum_open( UNUM_DEFAULT, NULL, success );          
 *     unum_close(nf);
 *     nf = unum_open( UNUM_CURRENCY, NULL, success );
 *     unum_close(nf);
 *     nf = unum_open( UNUM_PERCENT, NULL, success );   
 *     unum_close(nf);
 * \endcode
 * </pre>
 * </blockquote>
 * Each of these methods has two variants; one with an explicit locale
 * and one without; the latter using the default locale.
 * <blockquote>
 * <pre>
 * \code 
 * 
 *     nf = unum_open( UNUM_DEFAULT, myLocale, success );          
 *     unum_close(nf);
 *     nf = unum_open( UNUM_CURRENCY, myLocale, success );
 *     unum_close(nf);
 *     nf = unum_open( UNUM_PERCENT, myLocale, success );   
 *     unum_close(nf);
 * \endcode
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
 * \code
 *       const char* uloc_getAvailable(int32_t index);
 *       int32_t uloc_countAvailable();
 *       int32_t
 *       uloc_getDisplayName(const char* localeID,
 *                 const char* inLocaleID, 
 *                 UChar* result,
 *                 int32_t maxResultSize,
 *                  UErrorCode* err);
 * 
 * \endcode
 * </pre>
 * </blockquote>
 * <P>
 * Concerning POSIX/RFC1766 Locale IDs, 
 *  the getLanguage/getCountry/getVariant/getName functions do understand
 * the POSIX type form of  language_COUNTRY.ENCODING@VARIANT
 * and if there is not an ICU-stype variant, uloc_getVariant() for example
 * will return the one listed after the @at sign. As well, the hyphen
 * "-" is recognized as a country/variant separator similarly to RFC1766.
 * So for example, "en-us" will be interpreted as en_US.  
 * As a result, uloc_getName() is far from a no-op, and will have the
 * effect of converting POSIX/RFC1766 IDs into ICU form, although it does
 * NOT map any of the actual codes (i.e. russian->ru) in any way.
 * Applications should call uloc_getName() at the point where a locale ID
 * is coming from an external source (user entry, OS, web browser)
 * and pass the resulting string to other ICU functions.  For example,
 * don't use de-de@EURO as an argument to resourcebundle.
 *
 * @see UResourceBundle
 */

/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_CHINESE            "zh"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_ENGLISH            "en"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_FRENCH             "fr"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_GERMAN             "de"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_ITALIAN            "it"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_JAPANESE           "ja"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_KOREAN             "ko"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_SIMPLIFIED_CHINESE "zh_CN"
/** Useful constant for this language. @stable ICU 2.0 */
#define ULOC_TRADITIONAL_CHINESE "zh_TW"

/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_CANADA         "en_CA"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_CANADA_FRENCH  "fr_CA"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_CHINA          "zh_CN"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_PRC            "zh_CN"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_FRANCE         "fr_FR"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_GERMANY        "de_DE"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_ITALY          "it_IT"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_JAPAN          "ja_JP"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_KOREA          "ko_KR"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_TAIWAN         "zh_TW"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_UK             "en_GB"
/** Useful constant for this country/region. @stable ICU 2.0 */
#define ULOC_US             "en_US"

/**
 * Useful constant for the maximum size of the language part of a locale ID.
 * (including the terminating NULL).
 * @stable ICU 2.0
 */
#define ULOC_LANG_CAPACITY 12
/**
 * Useful constant for the maximum size of the country part of a locale ID
 * (including the terminating NULL).
 * @stable ICU 2.0
 */
#define ULOC_COUNTRY_CAPACITY 4
/**
 * Useful constant for the maximum size of the whole locale ID
 * (including the terminating NULL).
 * @stable ICU 2.0
 */
#define ULOC_FULLNAME_CAPACITY 50


/**
 * Constants for *_getLocale()
 * Allow user to select whether she wants information on 
 * requested, valid or actual locale.
 * For example, a collator for "en_US_CALIFORNIA" was
 * requested. In the current state of ICU (2.0), 
 * the requested locale is "en_US_CALIFORNIA",
 * the valid locale is "en_US" (most specific locale supported by ICU)
 * and the actual locale is "root" (the collation data comes unmodified 
 * from the UCA)
 * The locale is considered supported by ICU if there is a core ICU bundle 
 * for that locale (although it may be empty).
 * @stable ICU 2.1
 */
typedef enum {
  /** This is locale the data actually comes from */
  ULOC_ACTUAL_LOCALE    = 0,
  /** This is the most specific locale supported by ICU */
  ULOC_VALID_LOCALE    = 1,
  /** This is the requested locale */
  ULOC_REQUESTED_LOCALE = 2,
  ULOC_DATA_LOCALE_TYPE_LIMIT
} ULocDataLocaleType ;


/**
 * Gets ICU's default locale.  This pointer and/or the contents of the pointer may
 * become invalid if the uloc_setDefault() is called, so copy the contents of the
 * pointer before calling uloc_setDefault().
 *
 * @return the ICU default locale
 * @system
 * @stable ICU 2.0
 */
U_CAPI const char* U_EXPORT2
uloc_getDefault(void);

/**
 * Sets ICU's default locale.  Call this once during setup or program initialization.  
 *
 * @param localeID the new ICU default locale. A value of NULL will try to get
 *                 the system's default locale.
 * @param status the error information if the setting of default locale fails
 * @system
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2
uloc_setDefault(const char* localeID,
        UErrorCode*       status);

/**
 * Gets the language code for the specified locale.
 *
 * @param localeID the locale to get the ISO langauge code with
 * @param language the langauge code for localeID
 * @param languageCapacity the size of the language buffer to store the  
 * language code with
 * @param err error information if retrieving the  language code failed
 * @return the actual buffer size needed for the  langauge code.  If it's greater 
 * than languageCapacity, the returned language code will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err);

/**
 * Gets the  country code for the specified locale.
 *
 * @param localeID the locale to get the country code with
 * @param country the country code for localeID
 * @param countryCapacity the size of the country buffer to store the  
 * country code with
 * @param err error information if retrieving the country code failed
 * @return the actual buffer size needed for the country code.  If it's greater 
 * than countryCapacity, the returned country code will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getCountry(const char*    localeID,
        char* country,
        int32_t countryCapacity,
        UErrorCode* err);

/**
 * Gets the variant code for the specified locale.
 *
 * @param localeID the locale to get the variant code with
 * @param variant the variant code for localeID
 * @param variantCapacity the size of the variant buffer to store the 
 * variant code with
 * @param err error information if retrieving the variant code failed
 * @return the actual buffer size needed for the variant code.  If it's greater 
 * than variantCapacity, the returned variant code will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getVariant(const char*    localeID,
        char* variant,
        int32_t variantCapacity,
        UErrorCode* err);
/**
 * Gets the full name for the specified locale.
 * Note: This has the effect of 'canonicalizing' the string to
 * a certain extent. Upper and lower case are set as needed,
 * and if the components were in 'POSIX' format they are changed to
 * ICU format.  It does NOT map aliased names in any way.
 * See the top of this header file.
 *
 * @param localeID the locale to get the full name with
 * @param name the full name for localeID
 * @param nameCapacity the size of the name buffer to store the 
 * full name with
 * @param err error information if retrieving the full name failed
 * @return the actual buffer size needed for the full name.  If it's greater 
 * than nameCapacity, the returned full name will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getName(const char*    localeID,
         char* name,
         int32_t nameCapacity,
         UErrorCode* err);

/**
 * Gets the ISO language code for the specified locale.
 *
 * @param localeID the locale to get the ISO langauge code with
 * @return language the ISO langauge code for localeID
 * @stable ICU 2.0
 */
U_CAPI const char* U_EXPORT2
uloc_getISO3Language(const char* localeID);


/**
 * Gets the ISO country code for the specified locale.
 *
 * @param localeID the locale to get the ISO country code with
 * @return country the ISO country code for localeID
 * @stable ICU 2.0
 */
U_CAPI const char* U_EXPORT2
uloc_getISO3Country(const char* localeID);

/**
 * Gets the Win32 LCID value for the specified locale.
 *
 * @param localeID the locale to get the Win32 LCID value with
 * @return country the Win32 LCID for localeID
 * @stable ICU 2.0
 */
U_CAPI uint32_t U_EXPORT2
uloc_getLCID(const char* localeID);

/**
 * Gets the language name suitable for display for the specified locale.
 *
 * @param locale the locale to get the ISO langauge code with
 * @param inLocale Specifies the locale to be used to display the name.  In other words,
 *                 if the locale's language code is "en", passing Locale::getFrench() for
 *                 inLocale would result in "Anglais", while passing Locale::getGerman()
 *                 for inLocale would result in "Englisch".
 * @param language the displayable langauge code for localeID
 * @param languageCapacity the size of the language buffer to store the  
 * displayable language code with
 * @param status error information if retrieving the displayable language code failed
 * @return the actual buffer size needed for the displayable langauge code.  If it's greater 
 * than languageCapacity, the returned language code will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getDisplayLanguage(const char* locale,
            const char* inLocale,
            UChar* language,
            int32_t languageCapacity,
            UErrorCode* status);

/**
 * Gets the country name suitable for display for the specified locale.
 *
 * @param locale the locale to get the displayable country code with
 * @param inLocale Specifies the locale to be used to display the name.  In other words,
 *                 if the locale's language code is "en", passing Locale::getFrench() for
 *                 inLocale would result in "Anglais", while passing Locale::getGerman()
 *                 for inLocale would result in "Englisch".
 * @param country the displayable country code for localeID
 * @param countryCapacity the size of the country buffer to store the  
 * displayable country code with
 * @param status error information if retrieving the displayable country code failed
 * @return the actual buffer size needed for the displayable country code.  If it's greater 
 * than countryCapacity, the returned displayable country code will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getDisplayCountry(const char* locale,
            const char* inLocale,
            UChar* country,
            int32_t countryCapacity,
            UErrorCode* status);    /* NULL may be used to specify the default */


/**
 * Gets the variant code suitable for display for the specified locale.
 *
 * @param locale the locale to get the displayable variant code with
 * @param inLocale Specifies the locale to be used to display the name.  In other words,
 *                 if the locale's language code is "en", passing Locale::getFrench() for
 *                 inLocale would result in "Anglais", while passing Locale::getGerman()
 *                 for inLocale would result in "Englisch".
 * @param variant the displayable variant code for localeID
 * @param variantCapacity the size of the variant buffer to store the 
 * displayable variant code with
 * @param status error information if retrieving the displayable variant code failed
 * @return the actual buffer size needed for the displayable variant code.  If it's greater 
 * than variantCapacity, the returned displayable variant code will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getDisplayVariant(const char* locale,
            const char* inLocale,
               UChar* variant,
             int32_t variantCapacity,
            UErrorCode* status);    /* NULL may be used to specify the default */

/**
 * Gets the full name suitable for display for the specified locale.
 *
 * @param localeID the locale to get the displayable name with
 * @param inLocaleID Specifies the locale to be used to display the name.  In other words,
 *                   if the locale's language code is "en", passing Locale::getFrench() for
 *                   inLocale would result in "Anglais", while passing Locale::getGerman()
 *                   for inLocale would result in "Englisch".
 * @param result the displayable name for localeID
 * @param maxResultSize the size of the name buffer to store the 
 * displayable full name with
 * @param err error information if retrieving the displayable name failed
 * @return the actual buffer size needed for the displayable name.  If it's greater 
 * than variantCapacity, the returned displayable name will be truncated.  
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getDisplayName(const char* localeID,
            const char* inLocaleID, /* NULL may be used to specify the default */
            UChar* result,
            int32_t maxResultSize,
            UErrorCode* err);


/**
 * Gets the specified locale from a list of all available locales.  
 * The return value is a pointer to an item of 
 * a locale name array.  Both this array and the pointers
 * it contains are owned by ICU and should not be deleted or written through
 * by the caller.  The locale name is terminated by a null pointer.
 * @param n the specific locale name index of the available locale list
 * @return a specified locale name of all available locales
 * @stable ICU 2.0
 */
U_CAPI const char* U_EXPORT2
uloc_getAvailable(int32_t n);

/**
 * Gets the size of the all available locale list.
 *
 * @return the size of the locale list
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 uloc_countAvailable(void);

/**
 *
 * Gets a list of all available language codes defined in ISO 639.  This is a pointer
 * to an array of pointers to arrays of char.  All of these pointers are owned
 * by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 * @return a list of all available language codes
 * @stable ICU 2.0
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
 * @stable ICU 2.0
 */
U_CAPI const char* const* U_EXPORT2
uloc_getISOCountries(void);

/**
 * Truncate the locale ID string to get the parent locale ID.
 * Copies the part of the string before the last underscore.
 * The parent locale ID will be an empty string if there is no
 * underscore, or if there is only one underscore at localeID[0].
 *
 * @param localeID Input locale ID string.
 * @param parent   Output string buffer for the parent locale ID.
 * @param parentCapacity Size of the output buffer.
 * @param err A UErrorCode value.
 * @return The length of the parent locale ID.
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2
uloc_getParent(const char*    localeID,
                 char* parent,
                 int32_t parentCapacity,
                 UErrorCode* err);

/*eof*/


#endif /*_ULOC*/



