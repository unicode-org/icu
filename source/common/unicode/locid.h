/*
******************************************************************************
*
*   Copyright (C) 1996-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File locid.h
*
* Created by: Helena Shih
*
* Modification History:
*
*   Date        Name        Description
*   02/11/97    aliu        Changed gLocPath to fgLocPath and added methods to
*                           get and set it.
*   04/02/97    aliu        Made operator!= inline; fixed return value of getName().
*   04/15/97    aliu        Cleanup for AIX/Win32.
*   04/24/97    aliu        Numerous changes per code review.
*   08/18/98    stephen     Added tokenizeString(),changed getDisplayName()
*   09/08/98    stephen     Moved definition of kEmptyString for Mac Port
*   11/09/99    weiv        Added const char * getName() const;
*   04/12/00    srl         removing unicodestring api's and cached hash code
*   08/10/01    grhoten     Change the static Locales to accessor functions
******************************************************************************
*/

#ifndef LOCID_H
#define LOCID_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/putil.h"
#include "unicode/uloc.h"

/**
 * \file
 * \brief C++ API: Locale ID object.
 */

/**
 * A <code>Locale</code> object represents a specific geographical, political,
 * or cultural region. An operation that requires a <code>Locale</code> to perform
 * its task is called <em>locale-sensitive</em> and uses the <code>Locale</code>
 * to tailor information for the user. For example, displaying a number
 * is a locale-sensitive operation--the number should be formatted
 * according to the customs/conventions of the user's native country,
 * region, or culture.
 *
 * The Locale class is not suitable for subclassing.
 *
 * <P>
 * You create a <code>Locale</code> object using the constructor in
 * this class:
 * <blockquote>
 * <pre>
 * .      Locale( const   char*  language, 
 * .              const   char*  country, 
 * .              const   char*  variant);
 * </pre>
 * </blockquote>
 * The first argument to the constructors is a valid <STRONG>ISO
 * Language Code.</STRONG> These codes are the lower-case two-letter
 * codes as defined by ISO-639.
 * You can find a full list of these codes at a number of sites, such as:
 * <BR><a href ="http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt">
 * http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt</a>
 *
 * <P>
 * The second argument to the constructors is a valid <STRONG>ISO Country
 * Code.</STRONG> These codes are the upper-case two-letter codes
 * as defined by ISO-3166.
 * You can find a full list of these codes at a number of sites, such as:
 * <BR><a href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html">
 * http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html</a>
 *
 * <P>
 * The third constructor requires a third argument--the <STRONG>Variant.</STRONG>
 * The Variant codes are vendor and browser-specific.
 * For example, use WIN for Windows, MAC for Macintosh, and POSIX for POSIX.
 * Where there are two variants, separate them with an underscore, and
 * put the most important one first. For
 * example, a Traditional Spanish collation might be referenced, with
 * "ES", "ES", "Traditional_WIN".
 *
 * <P>
 * Because a <code>Locale</code> object is just an identifier for a region,
 * no validity check is performed when you construct a <code>Locale</code>.
 * If you want to see whether particular resources are available for the
 * <code>Locale</code> you construct, you must query those resources. For
 * example, ask the <code>NumberFormat</code> for the locales it supports
 * using its <code>getAvailableLocales</code> method.
 * <BR><STRONG>Note:</STRONG> When you ask for a resource for a particular
 * locale, you get back the best available match, not necessarily
 * precisely what you asked for. For more information, look at
 * <code>ResourceBundle</code>.
 *
 * <P>
 * The <code>Locale</code> class provides a number of convenient constants
 * that you can use to create <code>Locale</code> objects for commonly used
 * locales. For example, the following refers to a <code>Locale</code> object
 * for the United States:
 * <blockquote>
 * <pre>
 *       Locale::getUS()
 * </pre>
 * </blockquote>
 *
 * <P>
 * Once you've created a <code>Locale</code> you can query it for information about
 * itself. Use <code>getCountry</code> to get the ISO Country Code and
 * <code>getLanguage</code> to get the ISO Language Code. You can
 * use <code>getDisplayCountry</code> to get the
 * name of the country suitable for displaying to the user. Similarly,
 * you can use <code>getDisplayLanguage</code> to get the name of
 * the language suitable for displaying to the user. Interestingly,
 * the <code>getDisplayXXX</code> methods are themselves locale-sensitive
 * and have two versions: one that uses the default locale and one
 * that takes a locale as an argument and displays the name or country in
 * a language appropriate to that locale.
 *
 * <P>
 * The TIFC provides a number of classes that perform locale-sensitive
 * operations. For example, the <code>NumberFormat</code> class formats
 * numbers, currency, or percentages in a locale-sensitive manner. Classes
 * such as <code>NumberFormat</code> have a number of convenience methods
 * for creating a default object of that type. For example, the
 * <code>NumberFormat</code> class provides these three convenience methods
 * for creating a default <code>NumberFormat</code> object:
 * <blockquote>
 * <pre>
 *     UErrorCode success = U_ZERO_ERROR;
 *     Locale myLocale;
 *     NumberFormat *nf;
 * 
 *     nf = NumberFormat::createInstance( success );          delete nf;
 *     nf = NumberFormat::createCurrencyInstance( success );  delete nf;
 *     nf = NumberFormat::createPercentInstance( success );   delete nf;
 * </pre>
 * </blockquote>
 * Each of these methods has two variants; one with an explicit locale
 * and one without; the latter using the default locale.
 * <blockquote>
 * <pre>
 *     nf = NumberFormat::createInstance( myLocale, success );          delete nf;
 *     nf = NumberFormat::createCurrencyInstance( myLocale, success );  delete nf;
 *     nf = NumberFormat::createPercentInstance( myLocale, success );   delete nf;
 * </pre>
 * </blockquote>
 * A <code>Locale</code> is the mechanism for identifying the kind of object
 * (<code>NumberFormat</code>) that you would like to get. The locale is
 * <STRONG>just</STRONG> a mechanism for identifying objects,
 * <STRONG>not</STRONG> a container for the objects themselves.
 *
 * <P>
 * Each class that performs locale-sensitive operations allows you
 * to get all the available objects of that type. You can sift
 * through these objects by language, country, or variant,
 * and use the display names to present a menu to the user.
 * For example, you can create a menu of all the collation objects
 * suitable for a given language. Such classes implement these
 * three class methods:
 * <blockquote>
 * <pre>
 *       static Locale* getAvailableLocales(int32_t& numLocales)
 *       static UnicodeString& getDisplayName(const Locale&  objectLocale,
 *                                            const Locale&  displayLocale,
 *                                            UnicodeString& displayName)
 *       static UnicodeString& getDisplayName(const Locale&  objectLocale,
 *                                            UnicodeString& displayName)
 * </pre>
 * </blockquote>
 *
 * @stable ICU 2.0
 * @see ResourceBundle
 */
U_NAMESPACE_BEGIN
class U_COMMON_API Locale : public UObject {
public:
#ifdef ICU_LOCID_USE_DEPRECATES
    /**
     * A proxy for the Locale. 
     * This is defined to stay source code compatible and to remove static initialization.
     * Treat it just like a Locale.
     * @obsolete ICU 2.4. Use one of the other Locale get functions instead since this API will be removed in that release.
     */
    typedef struct U_COMMON_API LocaleProxy {

        /* Try not to access this. This is not meant for normal use. */
        int32_t magicLocaleNumber;

        /** Calls Locale's equivalent function */
        inline const char *getLanguage( ) const;
        /** Calls Locale's equivalent function */
        inline const char *getCountry( ) const;
        /** Calls Locale's equivalent function */
        inline const char *getVariant( ) const;
        /** Calls Locale's equivalent function */
        inline const char *getName() const;
        /** Calls Locale's equivalent function */
        inline const char *getISO3Language() const;
        /** Calls Locale's equivalent function */
        inline const char *getISO3Country() const;
        /** Calls Locale's equivalent function */
        inline uint32_t getLCID(void) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayLanguage(UnicodeString&   dispLang) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayLanguage( const   Locale&         inLocale,
                                                    UnicodeString&  dispLang) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayCountry(          UnicodeString& dispCountry) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayCountry(  const   Locale&         inLocale,
                                                    UnicodeString&  dispCountry) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayVariant(      UnicodeString& dispVar) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayVariant(  const   Locale&         inLocale,
                                                    UnicodeString&  dispVar) const;

        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayName(         UnicodeString&  name) const;
        /** Calls Locale's equivalent function */
        inline UnicodeString&  getDisplayName( const   Locale&         inLocale,
                                                UnicodeString&  name) const;
        /** Calls Locale's equivalent function */
        inline int32_t         hashCode(void) const;

        /** Cast a LocaleProxy into a Locale. This is the magic behind this proxy. */
        operator const Locale&() const;
    } LocaleProxy;

    /**
     * Useful constants for language.
     */
    /** @obsolete ICU 2.4. Use getEnglish() instead since this API will be removed in that release. */
    static const LocaleProxy ENGLISH;
    /** @obsolete ICU 2.4. Use getFrench() instead since this API will be removed in that release. */
    static const LocaleProxy FRENCH;
    /** @obsolete ICU 2.4. Use getGerman() instead since this API will be removed in that release. */
    static const LocaleProxy GERMAN;
    /** @obsolete ICU 2.4. Use getItalian() instead since this API will be removed in that release. */
    static const LocaleProxy ITALIAN;
    /** @obsolete ICU 2.4. Use getJapanese() instead since this API will be removed in that release. */
    static const LocaleProxy JAPANESE;
    /** @obsolete ICU 2.4. Use getKorean() instead since this API will be removed in that release. */
    static const LocaleProxy KOREAN;
    /** @obsolete ICU 2.4. Use getChinese() instead since this API will be removed in that release. */
    static const LocaleProxy CHINESE;
    /** @obsolete ICU 2.4. Use getSimplifiedChinese() instead since this API will be removed in that release. */
    static const LocaleProxy SIMPLIFIED_CHINESE;
    /** @obsolete ICU 2.4. Use getTraditionalChinese() instead since this API will be removed in that release. */
    static const LocaleProxy TRADITIONAL_CHINESE;

    /**
     * Useful constants for country.
     */
    /** @obsolete ICU 2.4. Use getFrance() instead since this API will be removed in that release. */
    static const LocaleProxy FRANCE;
    /** @obsolete ICU 2.4. Use getGermany() instead since this API will be removed in that release. */
    static const LocaleProxy GERMANY;
    /** @obsolete ICU 2.4. Use getItaly() instead since this API will be removed in that release. */
    static const LocaleProxy ITALY;
    /** @obsolete ICU 2.4. Use getJapan() instead since this API will be removed in that release. */
    static const LocaleProxy JAPAN;
    /** @obsolete ICU 2.4. Use getKorea() instead since this API will be removed in that release. */
    static const LocaleProxy KOREA;
    /** @obsolete ICU 2.4. Use getChina() instead since this API will be removed in that release. */
    static const LocaleProxy CHINA;      /* Alias for PRC */
    /** @obsolete ICU 2.4. Use getPRC() instead since this API will be removed in that release. */
    static const LocaleProxy PRC;        /* Peoples Republic of China */
    /** @obsolete ICU 2.4. Use getTaiwan() instead since this API will be removed in that release. */
    static const LocaleProxy TAIWAN;
    /** @obsolete ICU 2.4. Use getUK() instead since this API will be removed in that release. */
    static const LocaleProxy UK;
    /** @obsolete ICU 2.4. Use getUS() instead since this API will be removed in that release. */
    static const LocaleProxy US;
    /** @obsolete ICU 2.4. Use getCanada() instead since this API will be removed in that release. */
    static const LocaleProxy CANADA;
    /** @obsolete ICU 2.4. Use getCanadaFrench() instead since this API will be removed in that release. */
    static const LocaleProxy CANADA_FRENCH;

#endif /* ICU_LOCID_USE_DEPRECATES */

    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getEnglish(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getFrench(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getGerman(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getItalian(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getJapanese(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getKorean(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getChinese(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getSimplifiedChinese(void);
    /** Useful constant for this language. @stable ICU 2.0 */
    static const Locale &getTraditionalChinese(void);

    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getFrance(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getGermany(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getItaly(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getJapan(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getKorea(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getChina(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getPRC(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getTaiwan(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getUK(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getUS(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getCanada(void);
    /** Useful constant for this country/region. @stable ICU 2.0 */
    static const Locale &getCanadaFrench(void);


   /**
    * Construct an empty locale. It's only used when a fill-in parameter is
    * needed.
    * @stable ICU 2.0
    */
    Locale(); 

    /**
     * Construct a locale from language, country, variant.
     *
     * @param language Lowercase two-letter ISO-639 code.  This parameter can
     *  instead be an ICU style C locale (e.g. "en_US"), but the other
     *  parameters must not be used.  This parameter can be null, if so
     *  the locale is initialized to match the current default locale.
     *  Please note: The Java Locale class does NOT accept the form 
     *  'new Locale("en_US")' but only 'new Locale("en","US")'
     * 
     * @param country  Uppercase two-letter ISO-3166 code. (optional)
     * @param variant  Uppercase vendor and browser specific code. See class
     *                 description. (optional)
     * @stable ICU 2.0
     */
    Locale( const   char * language,
            const   char * country  = 0, 
            const   char * variant  = 0);

    /**
     * Initializes a Locale object from another Locale object.
     *
     * @param other The Locale object being copied in.
     * @stable ICU 2.0
     */
    Locale(const    Locale& other);


    /**
     * Destructor
     * @stable ICU 2.0
     */
    ~Locale() ;

    /**
     * Replaces the entire contents of *this with the specified value.
     *
     * @param other The Locale object being copied in.
     * @return      *this
     * @stable ICU 2.0
     */
    Locale& operator=(const Locale& other);

    /**
     * Checks if two locale keys are the same.
     *
     * @param other The locale key object to be compared with this.
     * @return      True if the two locale keys are the same, false otherwise.
     * @stable ICU 2.0
     */
    UBool   operator==(const    Locale&     other) const;

    /**
     * Checks if two locale keys are not the same.
     *
     * @param other The locale key object to be compared with this.
     * @return      True if the two locale keys are not the same, false
     *              otherwise.
     * @stable ICU 2.0
     */
    UBool   operator!=(const    Locale&     other) const;

    /**
     * Common methods of getting the current default Locale. Used for the
     * presentation: menus, dialogs, etc. Generally set once when your applet or
     * application is initialized, then never reset. (If you do reset the
     * default locale, you probably want to reload your GUI, so that the change
     * is reflected in your interface.)
     *
     * More advanced programs will allow users to use different locales for
     * different fields, e.g. in a spreadsheet.
     *
     * Note that the initial setting will match the host system.
     * @retrun the default locale for this instance of the Java Virtual Machine
     * @system
     * @stable ICU 2.0
     */
    static  const Locale& getDefault(void);

    /**
     * Sets the default. Normally set once at the beginning of applet or
     * application, then never reset. setDefault does NOT reset the host locale.
     *
     * @param newLocale Locale to set to.
     * @param success The error code.
     * @system
     * @stable ICU 2.0
     */
    static  void    setDefault(const    Locale&     newLocale,
                                                    UErrorCode&  success);

    
    /**
     * Creates a locale which has had minimal canonicalization 
     * as per uloc_getName(). 
     * @param name The name to create from.  If name is null,
     *  the default Locale is used.
     * @return new locale object
     * @stable ICU 2.0
     * @see uloc_getName
     */
    static Locale createFromName(const char *name);

    
    /**
     * Returns the locale's ISO-639 language code.
     * @return      An alias to the code
     * @stable ICU 2.0
     */
    inline const char *  getLanguage( ) const;

    /**
     * Returns the locale's ISO-3166 country code.
     * @return      An alias to the code
     * @stable ICU 2.0
     */
    inline const char *  getCountry( ) const;

    /**
     * Returns the locale's variant code.
     * @return      An alias to the code
     * @stable ICU 2.0
     */
    inline const char *  getVariant( ) const;

    /**
     * Returns the programmatic name of the entire locale, with the language,
     * country and variant separated by underbars. If a field is missing, up
     * to two leading underbars will occur. Example: "en", "de_DE", "en_US_WIN",
     * "de__POSIX", "fr__MAC", "__MAC", "_MT", "_FR_EURO"
     * @return      A pointer to "name".
     * @stable ICU 2.0
     */
    inline const char * getName() const;

    /**
     * returns the locale's three-letter language code, as specified
     * in ISO draft standard ISO-639-2..
     * @return      An alias to the code, or NULL
     * @stable ICU 2.0
     */
    const char * getISO3Language() const;

    /**
     * Fills in "name" with the locale's three-letter ISO-3166 country code.
     * @return      An alias to the code, or NULL
     * @stable ICU 2.0
     */
    const char * getISO3Country() const;

    /**
     * Returns the Windows LCID value corresponding to this locale.
     * This value is stored in the resource data for the locale as a one-to-four-digit
     * hexadecimal number.  If the resource is missing, in the wrong format, or
     * there is no Windows LCID value that corresponds to this locale, returns 0.
     * @stable ICU 2.0
     */
    uint32_t        getLCID(void) const;

    /**
     * Fills in "dispLang" with the name of this locale's language in a format suitable for
     * user display in the default locale.  For example, if the locale's language code is
     * "fr" and the default locale's language code is "en", this function would set
     * dispLang to "French".
     * @param dispLang  Receives the language's display name.
     * @return          A reference to "dispLang".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayLanguage(UnicodeString&   dispLang) const;

    /**
     * Fills in "dispLang" with the name of this locale's language in a format suitable for
     * user display in the locale specified by "inLocale".  For example, if the locale's
     * language code is "en" and inLocale's language code is "fr", this function would set
     * dispLang to "Anglais".
     * @param inLocale  Specifies the locale to be used to display the name.  In other words,
     *                  if the locale's language code is "en", passing Locale::getFrench() for
     *                  inLocale would result in "Anglais", while passing Locale::getGerman()
     *                  for inLocale would result in "Englisch".
     * @param dispLang  Receives the language's display name.
     * @return          A reference to "dispLang".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayLanguage( const   Locale&         inLocale,
                                                UnicodeString&  dispLang) const;

    /**
     * Fills in "dispCountry" with the name of this locale's country in a format suitable
     * for user display in the default locale.  For example, if the locale's country code
     * is "FR" and the default locale's language code is "en", this function would set
     * dispCountry to "France".
     * @param dispCountry   Receives the country's display name.
     * @return              A reference to "dispCountry".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayCountry(          UnicodeString& dispCountry) const;

    /**
     * Fills in "dispCountry" with the name of this locale's country in a format suitable
     * for user display in the locale specified by "inLocale".  For example, if the locale's
     * country code is "US" and inLocale's language code is "fr", this function would set
     * dispCountry to "Etats-Unis".
     * @param inLocale      Specifies the locale to be used to display the name.  In other
     *                      words, if the locale's country code is "US", passing
     *                      Locale::getFrench() for inLocale would result in "États-Unis", while
     *                      passing Locale::getGerman() for inLocale would result in
     *                      "Vereinigte Staaten".
     * @param dispCountry   Receives the country's display name.
     * @return              A reference to "dispCountry".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayCountry(  const   Locale&         inLocale,
                                                UnicodeString&  dispCountry) const;

    /**
     * Fills in "dispVar" with the name of this locale's variant code in a format suitable
     * for user display in the default locale.
     * @param dispVar   Receives the variant's name.
     * @return          A reference to "dispVar".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayVariant(      UnicodeString& dispVar) const;

    /**
     * Fills in "dispVar" with the name of this locale's variant code in a format
     * suitable for user display in the locale specified by "inLocale".
     * @param inLocale  Specifies the locale to be used to display the name.
     * @param dispVar   Receives the variant's display name.
     * @return          A reference to "dispVar".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayVariant(  const   Locale&         inLocale,
                                                UnicodeString&  dispVar) const;

    /**
     * Fills in "name" with the name of this locale in a format suitable for user display 
     * in the default locale.  This function uses getDisplayLanguage(), getDisplayCountry(),
     * and getDisplayVariant() to do its work, and outputs the display name in the format
     * "language (country[,variant])".  For example, if the default locale is en_US, then
     * fr_FR's display name would be "French (France)", and es_MX_Traditional's display name
     * would be "Spanish (Mexico,Traditional)".
     * @param name  Receives the locale's display name.
     * @return      A reference to "name".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayName(         UnicodeString&  name) const;

    /**
     * Fills in "name" with the name of this locale in a format suitable for user display 
     * in the locale specfied by "inLocale".  This function uses getDisplayLanguage(),
     * getDisplayCountry(), and getDisplayVariant() to do its work, and outputs the display
     * name in the format "language (country[,variant])".  For example, if inLocale is
     * fr_FR, then en_US's display name would be "Anglais (États-Unis)", and no_NO_NY's
     * display name would be "norvégien (Norvège,NY)".
     * @param inLocale  Specifies the locale to be used to display the name.
     * @param name      Receives the locale's display name.
     * @return          A reference to "name".
     * @stable ICU 2.0
     */
    UnicodeString&  getDisplayName( const   Locale&         inLocale,
                                            UnicodeString&  name) const;

    /**
     * Generates a hash code for the locale.
     * @stable ICU 2.0
     */
    int32_t         hashCode(void) const;

    /** 
     * Sets the locale to bogus 
     * A bogus locale represents a non-existing locale associated 
     * with services that can be instantiated from non-locale data 
     * in addition to locale (for example, collation can be 
     * instantiated from a locale and from a rule set). 
     * @draft ICU 2.1
     */
    void setToBogus();

    /**
     * Gets the bogus state. Locale object can be bogus if it doesn't exist
     * @return FALSE if it is a real locale, TRUE if it is a bogus locale
     * @draft ICU 2.1
     */
    UBool isBogus(void) const;

    /**
     * Returns a list of all installed locales.
     * @param count Receives the number of locales in the list.
     * @return      A pointer to an array of Locale objects.  This array is the list
     *              of all locales with installed resource files.  The called does NOT
     *              get ownership of this list, and must NOT delete it.
     * @stable ICU 2.0
     */
    static  const   Locale*     getAvailableLocales(int32_t& count);

    /**
     * Gets a list of all available 2-letter country codes defined in ISO 639.  This is a
     * pointer to an array of pointers to arrays of char.  All of these pointers are
     * owned by ICU-- do not delete them, and do not write through them.  The array is
     * terminated with a null pointer.
     * @return a list of all available country codes
     * @stable ICU 2.0
     */
    static const char* const* getISOCountries();

    /**
     * Gets a list of all available language codes defined in ISO 639.  This is a pointer
     * to an array of pointers to arrays of char.  All of these pointers are owned
     * by ICU-- do not delete them, and do not write through them.  The array is
     * terminated with a null pointer.
     * @return a list of all available language codes
     * @stable ICU 2.0
     */
    static const char* const*  getISOLanguages();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID();

protected: /* only protected for testing purposes. DO NOT USE. */
    /**
     * Set this from a single POSIX style locale string.
     * @internal
     */
    void setFromPOSIXID(const char *posixID);

private:
    /**
     * Initialize the locale object with a new name.
     * Was deprecated - used in implementation - moved internal
     *
     * @param cLocaleID The new locale name.
     */
    Locale& init(const char* cLocaleID);

    /**
     * Initialize the locale cache for commonly used locales
     */
    static void initLocaleCache(void);

    char language[ULOC_LANG_CAPACITY];
    char country[ULOC_COUNTRY_CAPACITY];
    int32_t variantBegin;
    char* fullName;
    char fullNameBuffer[ULOC_FULLNAME_CAPACITY];

    UBool fIsBogus;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
    
//    static Locale *localeList;
//    static int32_t localeListCount;

#ifdef ICU_LOCID_USE_DEPRECATES
    friend struct LocaleProxy;
#endif

    static const Locale &getLocale(int locid);

    friend void locale_set_default_internal(const char *);
};

inline UClassID
Locale::getStaticClassID()
{ return (UClassID)&fgClassID; }

inline UClassID
Locale::getDynamicClassID() const
{ return Locale::getStaticClassID(); }

inline UBool
Locale::operator!=(const    Locale&     other) const
{
    return !operator==(other);
}

inline const char *
Locale::getCountry() const
{
    return country;
}

inline const char *
Locale::getLanguage() const
{
    return language;
}

inline const char *
Locale::getVariant() const
{
    return &fullName[variantBegin];
}

inline const char * 
Locale::getName() const
{
    return fullName;
}

inline UBool 
Locale::isBogus(void) const {
  return fIsBogus;
}

#ifdef ICU_LOCID_USE_DEPRECATES
/* Proxy functions */
inline const char *Locale::LocaleProxy::getLanguage( ) const
{
    return ((const Locale)*this).getLanguage();
}

inline const char *Locale::LocaleProxy::getCountry( ) const
{
    return ((const Locale)*this).getCountry();
}

inline const char *Locale::LocaleProxy::getVariant( ) const
{
    return ((const Locale)*this).getVariant();
}

inline const char *Locale::LocaleProxy::getName() const
{
    return ((const Locale)*this).getName();
}

inline const char *Locale::LocaleProxy::getISO3Language() const
{
    return ((const Locale)*this).getISO3Language();
}

inline const char *Locale::LocaleProxy::getISO3Country() const
{
    return ((const Locale)*this).getISO3Country();
}

inline uint32_t Locale::LocaleProxy::getLCID(void) const
{
    return ((const Locale)*this).getLCID();
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayLanguage(UnicodeString&   dispLang) const
{
    return ((const Locale)*this).getDisplayLanguage(dispLang);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayLanguage( const   Locale&         inLocale,
                                            UnicodeString&  dispLang) const
{
    return ((const Locale)*this).getDisplayLanguage(inLocale, dispLang);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayCountry(UnicodeString& dispCountry) const
{
    return ((const Locale)*this).getDisplayCountry(dispCountry);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayCountry(const   Locale&         inLocale,
                                            UnicodeString&  dispCountry) const
{
    return ((const Locale)*this).getDisplayCountry(inLocale, dispCountry);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayVariant(UnicodeString& dispVar) const
{
    return ((const Locale)*this).getDisplayVariant(dispVar);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayVariant(const   Locale&         inLocale,
                                            UnicodeString&  dispVar) const
{
    return ((const Locale)*this).getDisplayVariant(inLocale, dispVar);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayName(UnicodeString&  name) const
{
    return ((const Locale)*this).getDisplayName(name);
}

inline UnicodeString&  Locale::LocaleProxy::getDisplayName(const   Locale&         inLocale,
                                        UnicodeString&  name) const
{
    return ((const Locale)*this).getDisplayName(inLocale, name);
}

inline int32_t         Locale::LocaleProxy::hashCode(void) const
{
    return ((const Locale)*this).hashCode();
}
#endif /* !ICU_LOCID_USE_DEPRECATES */
U_NAMESPACE_END

#endif
