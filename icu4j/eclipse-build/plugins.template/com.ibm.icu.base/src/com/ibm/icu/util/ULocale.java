// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
* Copyright (C) 2003-2012, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.LocaleIDParser;
import com.ibm.icu.impl.LocaleIDs;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.locale.AsciiUtil;
import com.ibm.icu.impl.locale.BaseLocale;
import com.ibm.icu.impl.locale.Extension;
import com.ibm.icu.impl.locale.InternalLocaleBuilder;
import com.ibm.icu.impl.locale.LanguageTag;
import com.ibm.icu.impl.locale.LocaleExtensions;
import com.ibm.icu.impl.locale.LocaleSyntaxException;
import com.ibm.icu.impl.locale.ParseStatus;
import com.ibm.icu.impl.locale.UnicodeLocaleExtension;

/**
 * {@icuenhanced java.util.Locale}.{@icu _usage_}
 *
 * A class analogous to {@link java.util.Locale} that provides additional
 * support for ICU protocol.  In ICU 3.0 this class is enhanced to support
 * RFC 3066 language identifiers.
 *
 * <p>Many classes and services in ICU follow a factory idiom, in
 * which a factory method or object responds to a client request with
 * an object.  The request includes a locale (the <i>requested</i>
 * locale), and the returned object is constructed using data for that
 * locale.  The system may lack data for the requested locale, in
 * which case the locale fallback mechanism will be invoked until a
 * populated locale is found (the <i>valid</i> locale).  Furthermore,
 * even when a populated locale is found (the <i>valid</i> locale),
 * further fallback may be required to reach a locale containing the
 * specific data required by the service (the <i>actual</i> locale).
 *
 * <p>ULocale performs <b>'normalization'</b> and <b>'canonicalization'</b> of locale ids.
 * Normalization 'cleans up' ICU locale ids as follows:
 * <ul>
 * <li>language, script, country, variant, and keywords are properly cased<br>
 * (lower, title, upper, upper, and lower case respectively)</li>
 * <li>hyphens used as separators are converted to underscores</li>
 * <li>three-letter language and country ids are converted to two-letter
 * equivalents where available</li>
 * <li>surrounding spaces are removed from keywords and values</li>
 * <li>if there are multiple keywords, they are put in sorted order</li>
 * </ul>
 * Canonicalization additionally performs the following:
 * <ul>
 * <li>POSIX ids are converted to ICU format IDs</li>
 * <li>Legacy language tags (marked as “Type: grandfathered” in BCP 47)
 * are converted to ICU standard form</li>
 * <li>'PREEURO' and 'EURO' variants are converted to currency keyword form,
 * with the currency
 * id appropriate to the country of the locale (for PREEURO) or EUR (for EURO).
 * </ul>
 * All ULocale constructors automatically normalize the locale id.  To handle
 * POSIX ids, <code>canonicalize</code> can be called to convert the id
 * to canonical form, or the <code>canonicalInstance</code> factory method
 * can be called.</p>
 *
 * <p>This class provides selectors {@link #VALID_LOCALE} and {@link
 * #ACTUAL_LOCALE} intended for use in methods named
 * <tt>getLocale()</tt>.  These methods exist in several ICU classes,
 * including {@link com.ibm.icu.util.Calendar}, {@link
 * com.ibm.icu.util.Currency}, {@link com.ibm.icu.text.UFormat},
 * {@link com.ibm.icu.text.BreakIterator},
 * <a href="../text/Collator.html" title="class in com.ibm.icu.text"><code>Collator</code></a>,
 * {@link com.ibm.icu.text.DateFormatSymbols}, and {@link
 * com.ibm.icu.text.DecimalFormatSymbols} and their subclasses, if
 * any.  Once an object of one of these classes has been created,
 * <tt>getLocale()</tt> may be called on it to determine the valid and
 * actual locale arrived at during the object's construction.
 *
 * <p>Note: The <i>actual</i> locale is returned correctly, but the <i>valid</i>
 * locale is not, in most cases.
 *
 * @see java.util.Locale
 * @author weiv
 * @author Alan Liu
 * @author Ram Viswanadha
 * @stable ICU 2.8
 */
public final class ULocale implements Serializable {
    // using serialver from jdk1.4.2_05
    private static final long serialVersionUID = 3715177670352309217L;

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale ENGLISH = new ULocale("en", Locale.ENGLISH);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale FRENCH = new ULocale("fr", Locale.FRENCH);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale GERMAN = new ULocale("de", Locale.GERMAN);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale ITALIAN = new ULocale("it", Locale.ITALIAN);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale JAPANESE = new ULocale("ja", Locale.JAPANESE);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale KOREAN = new ULocale("ko", Locale.KOREAN);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale CHINESE = new ULocale("zh", Locale.CHINESE);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale SIMPLIFIED_CHINESE = new ULocale("zh_Hans", Locale.CHINESE);

    /**
     * Useful constant for language.
     * @stable ICU 3.0
     */
    public static final ULocale TRADITIONAL_CHINESE = new ULocale("zh_Hant", Locale.CHINESE);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale FRANCE = new ULocale("fr_FR", Locale.FRANCE);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale GERMANY = new ULocale("de_DE", Locale.GERMANY);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale ITALY = new ULocale("it_IT", Locale.ITALY);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale JAPAN = new ULocale("ja_JP", Locale.JAPAN);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale KOREA = new ULocale("ko_KR", Locale.KOREA);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale CHINA = new ULocale("zh_Hans_CN", Locale.CHINA);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale PRC = CHINA;

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale TAIWAN = new ULocale("zh_Hant_TW", Locale.TAIWAN);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale UK = new ULocale("en_GB", Locale.UK);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale US = new ULocale("en_US", Locale.US);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale CANADA = new ULocale("en_CA", Locale.CANADA);

    /**
     * Useful constant for country/region.
     * @stable ICU 3.0
     */
    public static final ULocale CANADA_FRENCH = new ULocale("fr_CA", Locale.CANADA_FRENCH);

    /**
     * Handy constant.
     */
    private static final String EMPTY_STRING = "";

    // Used in both ULocale and LocaleIDParser, so moved up here.
    private static final char UNDERSCORE            = '_';

    // default empty locale
    private static final Locale EMPTY_LOCALE = new Locale("", "");

    // special keyword key for Unicode locale attributes
    private static final String LOCALE_ATTRIBUTE_KEY = "attribute";

    /**
     * The root ULocale.
     * @stable ICU 2.8
     */
    public static final ULocale ROOT = new ULocale("", EMPTY_LOCALE);

    /**
     * Enum for locale categories. These locale categories are used to get/set the default locale for
     * the specific functionality represented by the category.
     * @stable ICU 49
     */
    public enum Category {
        /**
         * Category used to represent the default locale for displaying user interfaces.
         * @stable ICU 49
         */
        DISPLAY,
        /**
         * Category used to represent the default locale for formatting date, number and/or currency.
         * @stable ICU 49
         */
        FORMAT
    }

    private static final SimpleCache<Locale, ULocale> CACHE = new SimpleCache<Locale, ULocale>();

    /**
     * Cache the locale.
     */
    private transient volatile Locale locale;

    /**
     * The raw localeID that we were passed in.
     */
    private String localeID;

    /**
     * Cache the locale data container fields.
     * In future, we want to use them as the primary locale identifier storage.
     */
    private transient volatile BaseLocale baseLocale;
    private transient volatile LocaleExtensions extensions;


    private static String[][] CANONICALIZE_MAP;
    private static String[][] variantsToKeywords;

    private static void initCANONICALIZE_MAP() {
        if (CANONICALIZE_MAP == null) {
            /**
             * This table lists pairs of locale ids for canonicalization.  The
             * The 1st item is the normalized id. The 2nd item is the
             * canonicalized id. The 3rd is the keyword. The 4th is the keyword value.
             */
            String[][] tempCANONICALIZE_MAP = {
//              { EMPTY_STRING,     "en_US_POSIX", null, null }, /* .NET name */
                { "C",              "en_US_POSIX", null, null }, /* POSIX name */
                { "art_LOJBAN",     "jbo", null, null }, /* registered name */
                { "az_AZ_CYRL",     "az_Cyrl_AZ", null, null }, /* .NET name */
                { "az_AZ_LATN",     "az_Latn_AZ", null, null }, /* .NET name */
                { "ca_ES_PREEURO",  "ca_ES", "currency", "ESP" },
                { "cel_GAULISH",    "cel__GAULISH", null, null }, /* registered name */
                { "de_1901",        "de__1901", null, null }, /* registered name */
                { "de_1906",        "de__1906", null, null }, /* registered name */
                { "de__PHONEBOOK",  "de", "collation", "phonebook" }, /* Old ICU name */
                { "de_AT_PREEURO",  "de_AT", "currency", "ATS" },
                { "de_DE_PREEURO",  "de_DE", "currency", "DEM" },
                { "de_LU_PREEURO",  "de_LU", "currency", "EUR" },
                { "el_GR_PREEURO",  "el_GR", "currency", "GRD" },
                { "en_BOONT",       "en__BOONT", null, null }, /* registered name */
                { "en_SCOUSE",      "en__SCOUSE", null, null }, /* registered name */
                { "en_BE_PREEURO",  "en_BE", "currency", "BEF" },
                { "en_IE_PREEURO",  "en_IE", "currency", "IEP" },
                { "es__TRADITIONAL", "es", "collation", "traditional" }, /* Old ICU name */
                { "es_ES_PREEURO",  "es_ES", "currency", "ESP" },
                { "eu_ES_PREEURO",  "eu_ES", "currency", "ESP" },
                { "fi_FI_PREEURO",  "fi_FI", "currency", "FIM" },
                { "fr_BE_PREEURO",  "fr_BE", "currency", "BEF" },
                { "fr_FR_PREEURO",  "fr_FR", "currency", "FRF" },
                { "fr_LU_PREEURO",  "fr_LU", "currency", "LUF" },
                { "ga_IE_PREEURO",  "ga_IE", "currency", "IEP" },
                { "gl_ES_PREEURO",  "gl_ES", "currency", "ESP" },
                { "hi__DIRECT",     "hi", "collation", "direct" }, /* Old ICU name */
                { "it_IT_PREEURO",  "it_IT", "currency", "ITL" },
                { "ja_JP_TRADITIONAL", "ja_JP", "calendar", "japanese" },
//              { "nb_NO_NY",       "nn_NO", null, null },
                { "nl_BE_PREEURO",  "nl_BE", "currency", "BEF" },
                { "nl_NL_PREEURO",  "nl_NL", "currency", "NLG" },
                { "pt_PT_PREEURO",  "pt_PT", "currency", "PTE" },
                { "sl_ROZAJ",       "sl__ROZAJ", null, null }, /* registered name */
                { "sr_SP_CYRL",     "sr_Cyrl_RS", null, null }, /* .NET name */
                { "sr_SP_LATN",     "sr_Latn_RS", null, null }, /* .NET name */
                { "sr_YU_CYRILLIC", "sr_Cyrl_RS", null, null }, /* Linux name */
                { "th_TH_TRADITIONAL", "th_TH", "calendar", "buddhist" }, /* Old ICU name */
                { "uz_UZ_CYRILLIC", "uz_Cyrl_UZ", null, null }, /* Linux name */
                { "uz_UZ_CYRL",     "uz_Cyrl_UZ", null, null }, /* .NET name */
                { "uz_UZ_LATN",     "uz_Latn_UZ", null, null }, /* .NET name */
                { "zh_CHS",         "zh_Hans", null, null }, /* .NET name */
                { "zh_CHT",         "zh_Hant", null, null }, /* .NET name */
                { "zh_GAN",         "zh__GAN", null, null }, /* registered name */
                { "zh_GUOYU",       "zh", null, null }, /* registered name */
                { "zh_HAKKA",       "zh__HAKKA", null, null }, /* registered name */
                { "zh_MIN",         "zh__MIN", null, null }, /* registered name */
                { "zh_MIN_NAN",     "zh__MINNAN", null, null }, /* registered name */
                { "zh_WUU",         "zh__WUU", null, null }, /* registered name */
                { "zh_XIANG",       "zh__XIANG", null, null }, /* registered name */
                { "zh_YUE",         "zh__YUE", null, null } /* registered name */
            };

            synchronized (ULocale.class) {
                if (CANONICALIZE_MAP == null) {
                    CANONICALIZE_MAP = tempCANONICALIZE_MAP;
                }
            }
        }
        if (variantsToKeywords == null) {
            /**
             * This table lists pairs of locale ids for canonicalization.  The
             * The first item is the normalized variant id.
             */
            String[][] tempVariantsToKeywords = {
                    { "EURO",   "currency", "EUR" },
                    { "PINYIN", "collation", "pinyin" }, /* Solaris variant */
                    { "STROKE", "collation", "stroke" }  /* Solaris variant */
            };

            synchronized (ULocale.class) {
                if (variantsToKeywords == null) {
                    variantsToKeywords = tempVariantsToKeywords;
                }
            }
        }
    }

    /**
     * Private constructor used by static initializers.
     */
    private ULocale(String localeID, Locale locale) {
        this.localeID = localeID;
        this.locale = locale;
    }

    /**
     * Construct a ULocale object from a {@link java.util.Locale}.
     * @param loc a JDK locale
     */
    private ULocale(Locale loc) {
        this.localeID = getName(forLocale(loc).toString());
        this.locale = loc;
    }

    /**
     * {@icu} Returns a ULocale object for a {@link java.util.Locale}.
     * The ULocale is canonicalized.
     * @param loc a JDK locale
     * @stable ICU 3.2
     */
    public static ULocale forLocale(Locale loc) {
        if (loc == null) {
            return null;
        }
        ULocale result = CACHE.get(loc);
        if (result == null) {
            result = JDKLocaleHelper.toULocale(loc);
            CACHE.put(loc, result);
        }
        return result;
    }

    /**
     * {@icu} Constructs a ULocale from a RFC 3066 locale ID. The locale ID consists
     * of optional language, script, country, and variant fields in that order,
     * separated by underscores, followed by an optional keyword list.  The
     * script, if present, is four characters long-- this distinguishes it
     * from a country code, which is two characters long.  Other fields
     * are distinguished by position as indicated by the underscores.  The
     * start of the keyword list is indicated by '@', and consists of two
     * or more keyword/value pairs separated by semicolons(';').
     * 
     * <p>This constructor does not canonicalize the localeID.  So, for
     * example, "zh__pinyin" remains unchanged instead of converting
     * to "zh@collation=pinyin".  By default ICU only recognizes the
     * latter as specifying pinyin collation.  Use {@link #createCanonical}
     * or {@link #canonicalize} if you need to canonicalize the localeID.
     *
     * @param localeID string representation of the locale, e.g:
     * "en_US", "sy_Cyrl_YU", "zh__pinyin", "es_ES@currency=EUR;collation=traditional"
     * @stable ICU 2.8
     */
    public ULocale(String localeID) {
        this.localeID = getName(localeID);
    }

    /**
     * Convenience overload of ULocale(String, String, String) for
     * compatibility with java.util.Locale.
     * @see #ULocale(String, String, String)
     * @stable ICU 3.4
     */
    public ULocale(String a, String b) {
        this(a, b, null);
    }

    /**
     * Constructs a ULocale from a localeID constructed from the three 'fields' a, b, and
     * c.  These fields are concatenated using underscores to form a localeID of the form
     * a_b_c, which is then handled like the localeID passed to <code>ULocale(String
     * localeID)</code>.
     *
     * <p>Java locale strings consisting of language, country, and
     * variant will be handled by this form, since the country code
     * (being shorter than four letters long) will not be interpreted
     * as a script code.  If a script code is present, the final
     * argument ('c') will be interpreted as the country code.  It is
     * recommended that this constructor only be used to ease porting,
     * and that clients instead use the single-argument constructor
     * when constructing a ULocale from a localeID.
     * @param a first component of the locale id
     * @param b second component of the locale id
     * @param c third component of the locale id
     * @see #ULocale(String)
     * @stable ICU 3.0
     */
    public ULocale(String a, String b, String c) {
        localeID = getName(lscvToID(a, b, c, EMPTY_STRING));
    }

    /**
     * {@icu} Creates a ULocale from the id by first canonicalizing the id.
     * @param nonCanonicalID the locale id to canonicalize
     * @return the locale created from the canonical version of the ID.
     * @stable ICU 3.0
     */
    public static ULocale createCanonical(String nonCanonicalID) {
        return new ULocale(canonicalize(nonCanonicalID), (Locale)null);
    }

    private static String lscvToID(String lang, String script, String country, String variant) {
        StringBuilder buf = new StringBuilder();

        if (lang != null && lang.length() > 0) {
            buf.append(lang);
        }
        if (script != null && script.length() > 0) {
            buf.append(UNDERSCORE);
            buf.append(script);
        }
        if (country != null && country.length() > 0) {
            buf.append(UNDERSCORE);
            buf.append(country);
        }
        if (variant != null && variant.length() > 0) {
            if (country == null || country.length() == 0) {
                buf.append(UNDERSCORE);
            }
            buf.append(UNDERSCORE);
            buf.append(variant);
        }
        return buf.toString();
    }

    /**
     * {@icu} Converts this ULocale object to a {@link java.util.Locale}.
     * @return a JDK locale that either exactly represents this object
     * or is the closest approximation.
     * @stable ICU 2.8
     */
    public Locale toLocale() {
        if (locale == null) {
            locale = JDKLocaleHelper.toLocale(this);
        }
        return locale;
    }

    private static ICUCache<String, String> nameCache = new SimpleCache<String, String>();

    /**
     * Keep our own default ULocale.
     */
    private static Locale defaultLocale = Locale.getDefault();
    private static ULocale defaultULocale = forLocale(defaultLocale);

    private static Locale[] defaultCategoryLocales = new Locale[Category.values().length];
    private static ULocale[] defaultCategoryULocales = new ULocale[Category.values().length];

    static {
        for (Category cat: Category.values()) {
            int idx = cat.ordinal();
            defaultCategoryLocales[idx] = JDKLocaleHelper.getDefault(cat);
            defaultCategoryULocales[idx] = forLocale(defaultCategoryLocales[idx]);
        }
    }

    /**
     * Returns the current default ULocale.
     * @return the default ULocale.
     * @stable ICU 2.8
     */
    public static ULocale getDefault() {
        synchronized (ULocale.class) {
            if (defaultULocale == null) {
                // When Java's default locale has extensions (such as ja-JP-u-ca-japanese),
                // Locale -> ULocale mapping requires BCP47 keyword mapping data that is currently
                // stored in a resource bundle. However, UResourceBundle currently requires
                // non-null default ULocale. For now, this implementation returns ULocale.ROOT
                // to avoid the problem.

                // TODO: Consider moving BCP47 mapping data out of resource bundle later.

                return ULocale.ROOT;
            }
            Locale currentDefault = Locale.getDefault();
            if (!defaultLocale.equals(currentDefault)) {
                defaultLocale = currentDefault;
                defaultULocale = forLocale(currentDefault);

                if (!JDKLocaleHelper.isJava7orNewer()) {
                    // Detected Java default Locale change.
                    // We need to update category defaults to match the
                    // Java 7's behavior on Java 6 or older environment.
                    for (Category cat : Category.values()) {
                        int idx = cat.ordinal();
                        defaultCategoryLocales[idx] = currentDefault;
                        defaultCategoryULocales[idx] = forLocale(currentDefault);
                    }
                }
            }
            return defaultULocale;
        }
    }

    /**
     * {@icu} Sets the default ULocale.  This also sets the default Locale.
     * If the caller does not have write permission to the
     * user.language property, a security exception will be thrown,
     * and the default ULocale will remain unchanged.
     * <p>
     * By setting the default ULocale with this method, all of the default categoy locales
     * are also set to the specified default ULocale.
     * @param newLocale the new default locale
     * @throws SecurityException if a security manager exists and its
     *        <code>checkPermission</code> method doesn't allow the operation.
     * @throws NullPointerException if <code>newLocale</code> is null
     * @see SecurityManager#checkPermission(java.security.Permission)
     * @see java.util.PropertyPermission
     * @see ULocale#setDefault(Category, ULocale)
     * @stable ICU 3.0
     */
    public static synchronized void setDefault(ULocale newLocale){
        defaultLocale = newLocale.toLocale();
        Locale.setDefault(defaultLocale);
        defaultULocale = newLocale;
        // This method also updates all category default locales
        for (Category cat : Category.values()) {
            setDefault(cat, newLocale);
        }
    }

    /**
     * Returns the current default ULocale for the specified category.
     * 
     * @param category the category
     * @return the default ULocale for the specified category.
     * @stable ICU 49
     */
    public static ULocale getDefault(Category category) {
        synchronized (ULocale.class) {
            int idx = category.ordinal();
            if (defaultCategoryULocales[idx] == null) {
                // Just in case this method is called during ULocale class
                // initialization. Unlike getDefault(), we do not have
                // cyclic dependency for category default.
                return ULocale.ROOT;
            }
            if (JDKLocaleHelper.isJava7orNewer()) {
                Locale currentCategoryDefault = JDKLocaleHelper.getDefault(category);
                if (!defaultCategoryLocales[idx].equals(currentCategoryDefault)) {
                    defaultCategoryLocales[idx] = currentCategoryDefault;
                    defaultCategoryULocales[idx] = forLocale(currentCategoryDefault);
                }
            } else {
                // java.util.Locale.setDefault(Locale) in Java 7 updates
                // category locale defaults. On Java 6 or older environment,
                // ICU4J checks if the default locale has changed and update
                // category ULocales here if necessary.
                
                // Note: When java.util.Locale.setDefault(Locale) is called
                // with a Locale same with the previous one, Java 7 still
                // updates category locale defaults. On Java 6 or older env,
                // there is no good way to detect the event, ICU4J simply
                // check if the default Java Locale has changed since last
                // time.

                Locale currentDefault = Locale.getDefault();
                if (!defaultLocale.equals(currentDefault)) {
                    defaultLocale = currentDefault;
                    defaultULocale = forLocale(currentDefault);

                    for (Category cat : Category.values()) {
                        int tmpIdx = cat.ordinal();
                        defaultCategoryLocales[tmpIdx] = currentDefault;
                        defaultCategoryULocales[tmpIdx] = forLocale(currentDefault);
                    }
                }
                
                // No synchronization with JDK Locale, because category default
                // is not supported in Java 6 or older versions
            }
            return defaultCategoryULocales[idx];
        }
    }

    /**
     * Sets the default <code>ULocale</code> for the specified <code>Category</code>.
     * This also sets the default <code>Locale</code> for the specified <code>Category</code>
     * of the JVM. If the caller does not have write permission to the
     * user.language property, a security exception will be thrown,
     * and the default ULocale for the specified Category will remain unchanged.
     * 
     * @param category the specified category to set the default locale
     * @param newLocale the new default locale
     * @see SecurityManager#checkPermission(java.security.Permission)
     * @see java.util.PropertyPermission
     * @stable ICU 49
     */
    public static synchronized void setDefault(Category category, ULocale newLocale) {
        Locale newJavaDefault = newLocale.toLocale();
        int idx = category.ordinal();
        defaultCategoryULocales[idx] = newLocale;
        defaultCategoryLocales[idx] = newJavaDefault;
        JDKLocaleHelper.setDefault(category, newJavaDefault);
    }

    /**
     * This is for compatibility with Locale-- in actuality, since ULocale is
     * immutable, there is no reason to clone it, so this API returns 'this'.
     * @stable ICU 3.0
     */
    public Object clone() {
        return this;
    }

    /**
     * Returns the hashCode.
     * @stable ICU 3.0
     */
    public int hashCode() {
        return localeID.hashCode();
    }

    /**
     * Returns true if the other object is another ULocale with the
     * same full name, or is a String localeID that matches the full name.
     * Note that since names are not canonicalized, two ULocales that
     * function identically might not compare equal.
     *
     * @return true if this Locale is equal to the specified object.
     * @stable ICU 3.0
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof String) {
            return localeID.equals((String)obj);
        }
        if (obj instanceof ULocale) {
            return localeID.equals(((ULocale)obj).localeID);
        }
        return false;
    }

    /**
     * {@icunote} Unlike the Locale API, this returns an array of <code>ULocale</code>,
     * not <code>Locale</code>.  Returns a list of all installed locales.
     * @stable ICU 3.0
     */
    public static ULocale[] getAvailableLocales() {
        //#com.ibm.icu.base
        if (availableLocales == null) {
            synchronized (ULocale.class) {
                if (availableLocales == null) {
                    Locale[] locales = Locale.getAvailableLocales();
                    availableLocales = new ULocale[locales.length];
                    for (int i = 0; i < locales.length; i++) {
                        availableLocales[i] = ULocale.forLocale(locales[i]);
                    }
                }
            }
        }
        return availableLocales.clone();
    }
    private static volatile ULocale[] availableLocales = null;

    /**
     * Returns a list of all 2-letter country codes defined in ISO 3166.
     * Can be used to create Locales.
     * @stable ICU 3.0
     */
    public static String[] getISOCountries() {
        return LocaleIDs.getISOCountries();
    }

    /**
     * Returns a list of all 2-letter language codes defined in ISO 639.
     * Can be used to create Locales.
     * [NOTE:  ISO 639 is not a stable standard-- some languages' codes have changed.
     * The list this function returns includes both the new and the old codes for the
     * languages whose codes have changed.]
     * @stable ICU 3.0
     */
    public static String[] getISOLanguages() {
        return LocaleIDs.getISOLanguages();
    }

    /**
     * Returns the language code for this locale, which will either be the empty string
     * or a lowercase ISO 639 code.
     * @see #getDisplayLanguage()
     * @see #getDisplayLanguage(ULocale)
     * @stable ICU 3.0
     */
    public String getLanguage() {
        return getLanguage(localeID);
    }

    /**
     * Returns the language code for the locale ID,
     * which will either be the empty string
     * or a lowercase ISO 639 code.
     * @see #getDisplayLanguage()
     * @see #getDisplayLanguage(ULocale)
     * @stable ICU 3.0
     */
    public static String getLanguage(String localeID) {
        return new LocaleIDParser(localeID).getLanguage();
    }

    /**
     * {@icu} Returns the script code for this locale, which might be the empty string.
     * @see #getDisplayScript()
     * @see #getDisplayScript(ULocale)
     * @stable ICU 3.0
     */
    public String getScript() {
        return getScript(localeID);
    }

    /**
     * {@icu} Returns the script code for the specified locale, which might be the empty
     * string.
     * @see #getDisplayScript()
     * @see #getDisplayScript(ULocale)
     * @stable ICU 3.0
     */
    public static String getScript(String localeID) {
        return new LocaleIDParser(localeID).getScript();
    }

    /**
     * Returns the country/region code for this locale, which will either be the empty string
     * or an uppercase ISO 3166 2-letter code.
     * @see #getDisplayCountry()
     * @see #getDisplayCountry(ULocale)
     * @stable ICU 3.0
     */
    public String getCountry() {
        return getCountry(localeID);
    }

    /**
     * Returns the country/region code for this locale, which will either be the empty string
     * or an uppercase ISO 3166 2-letter code.
     * @param localeID The locale identification string.
     * @see #getDisplayCountry()
     * @see #getDisplayCountry(ULocale)
     * @stable ICU 3.0
     */
    public static String getCountry(String localeID) {
        return new LocaleIDParser(localeID).getCountry();
    }

    /**
     * Returns the variant code for this locale, which might be the empty string.
     * @see #getDisplayVariant()
     * @see #getDisplayVariant(ULocale)
     * @stable ICU 3.0
     */
    public String getVariant() {
        return getVariant(localeID);
    }

    /**
     * Returns the variant code for the specified locale, which might be the empty string.
     * @see #getDisplayVariant()
     * @see #getDisplayVariant(ULocale)
     * @stable ICU 3.0
     */
    public static String getVariant(String localeID) {
        return new LocaleIDParser(localeID).getVariant();
    }

    /**
     * {@icu} Returns the fallback locale for the specified locale, which might be the
     * empty string.
     * @stable ICU 3.2
     */
    public static String getFallback(String localeID) {
        return getFallbackString(getName(localeID));
    }

    /**
     * {@icu} Returns the fallback locale for this locale.  If this locale is root,
     * returns null.
     * @stable ICU 3.2
     */
    public ULocale getFallback() {
        if (localeID.length() == 0 || localeID.charAt(0) == '@') {
            return null;
        }
        return new ULocale(getFallbackString(localeID), (Locale)null);
    }

    /**
     * Returns the given (canonical) locale id minus the last part before the tags.
     */
    private static String getFallbackString(String fallback) {
        int extStart = fallback.indexOf('@');
        if (extStart == -1) {
            extStart = fallback.length();
        }
        int last = fallback.lastIndexOf('_', extStart);
        if (last == -1) {
            last = 0;
        } else {
            // truncate empty segment
            while (last > 0) {
                if (fallback.charAt(last - 1) != '_') {
                    break;
                }
                last--;
            }
        }
        return fallback.substring(0, last) + fallback.substring(extStart);
    }

    /**
     * {@icu} Returns the (normalized) base name for this locale.
     * @return the base name as a String.
     * @stable ICU 3.0
     */
    public String getBaseName() {
        return getBaseName(localeID);
    }

    /**
     * {@icu} Returns the (normalized) base name for the specified locale.
     * @param localeID the locale ID as a string
     * @return the base name as a String.
     * @stable ICU 3.0
     */
    public static String getBaseName(String localeID){
        if (localeID.indexOf('@') == -1) {
            return localeID;
        }
        return new LocaleIDParser(localeID).getBaseName();
    }

    /**
     * {@icu} Returns the (normalized) full name for this locale.
     *
     * @return String the full name of the localeID
     * @stable ICU 3.0
     */
    public String getName() {
        return localeID; // always normalized
    }
    
    /**
     * Gets the shortest length subtag's size.
     *
     * @param localeID
     * @return The size of the shortest length subtag
     **/
    private static int getShortestSubtagLength(String localeID) {
        int localeIDLength = localeID.length();
        int length = localeIDLength;
        boolean reset = true;
        int tmpLength = 0;
        
        for (int i = 0; i < localeIDLength; i++) {
            if (localeID.charAt(i) != '_' && localeID.charAt(i) != '-') {
                if (reset) {
                    reset = false;
                    tmpLength = 0;
                }
                tmpLength++;
            } else {
                if (tmpLength != 0 && tmpLength < length) {
                    length = tmpLength;
                }
                reset = true;
            }
        }
        
        return length;
    }

    /**
     * {@icu} Returns the (normalized) full name for the specified locale.
     *
     * @param localeID the localeID as a string
     * @return String the full name of the localeID
     * @stable ICU 3.0
     */
    public static String getName(String localeID){
        String tmpLocaleID;
        // Convert BCP47 id if necessary
        if (localeID != null && !localeID.contains("@") && getShortestSubtagLength(localeID) == 1) {
            tmpLocaleID = forLanguageTag(localeID).getName();
            if (tmpLocaleID.length() == 0) {
                tmpLocaleID = localeID;
            }
        } else {
            tmpLocaleID = localeID;
        }
        String name = nameCache.get(tmpLocaleID);
        if (name == null) {
            name = new LocaleIDParser(tmpLocaleID).getName();
            nameCache.put(tmpLocaleID, name);
        }
        return name;
    }

    /**
     * Returns a string representation of this object.
     * @stable ICU 3.0
     */
    public String toString() {
        return localeID;
    }

    /**
     * {@icu} Returns an iterator over keywords for this locale.  If there
     * are no keywords, returns null.
     * @return iterator over keywords, or null if there are no keywords.
     * @stable ICU 3.0
     */
    public Iterator<String> getKeywords() {
        return getKeywords(localeID);
    }

    /**
     * {@icu} Returns an iterator over keywords for the specified locale.  If there
     * are no keywords, returns null.
     * @return an iterator over the keywords in the specified locale, or null
     * if there are no keywords.
     * @stable ICU 3.0
     */
    public static Iterator<String> getKeywords(String localeID){
        return new LocaleIDParser(localeID).getKeywords();
    }

    /**
     * {@icu} Returns the value for a keyword in this locale. If the keyword is not
     * defined, returns null.
     * @param keywordName name of the keyword whose value is desired. Case insensitive.
     * @return the value of the keyword, or null.
     * @stable ICU 3.0
     */
    public String getKeywordValue(String keywordName){
        return getKeywordValue(localeID, keywordName);
    }

    /**
     * {@icu} Returns the value for a keyword in the specified locale. If the keyword is
     * not defined, returns null.  The locale name does not need to be normalized.
     * @param keywordName name of the keyword whose value is desired. Case insensitive.
     * @return String the value of the keyword as a string
     * @stable ICU 3.0
     */
    public static String getKeywordValue(String localeID, String keywordName) {
        return new LocaleIDParser(localeID).getKeywordValue(keywordName);
    }

    /**
     * {@icu} Returns the canonical name for the specified locale ID.  This is used to
     * convert POSIX and other legacy IDs to standard ICU form.
     * @param localeID the locale id
     * @return the canonicalized id
     * @stable ICU 3.0
     */
    public static String canonicalize(String localeID){
        LocaleIDParser parser = new LocaleIDParser(localeID, true);
        String baseName = parser.getBaseName();
        boolean foundVariant = false;

        // formerly, we always set to en_US_POSIX if the basename was empty, but
        // now we require that the entire id be empty, so that "@foo=bar"
        // will pass through unchanged.
        // {dlf} I'd rather keep "" unchanged.
        if (localeID.equals("")) {
            return "";
//              return "en_US_POSIX";
        }

        // we have an ID in the form xx_Yyyy_ZZ_KKKKK

        initCANONICALIZE_MAP();

        /* convert the variants to appropriate ID */
        for (int i = 0; i < variantsToKeywords.length; i++) {
            String[] vals = variantsToKeywords[i];
            int idx = baseName.lastIndexOf("_" + vals[0]);
            if (idx > -1) {
                foundVariant = true;

                baseName = baseName.substring(0, idx);
                if (baseName.endsWith("_")) {
                    baseName = baseName.substring(0, --idx);
                }
                parser.setBaseName(baseName);
                parser.defaultKeywordValue(vals[1], vals[2]);
                break;
            }
        }

        /* See if this is an already known locale */
        for (int i = 0; i < CANONICALIZE_MAP.length; i++) {
            if (CANONICALIZE_MAP[i][0].equals(baseName)) {
                foundVariant = true;

                String[] vals = CANONICALIZE_MAP[i];
                parser.setBaseName(vals[1]);
                if (vals[2] != null) {
                    parser.defaultKeywordValue(vals[2], vals[3]);
                }
                break;
            }
        }

        /* total mondo hack for Norwegian, fortunately the main NY case is handled earlier */
        if (!foundVariant) {
            if (parser.getLanguage().equals("nb") && parser.getVariant().equals("NY")) {
                parser.setBaseName(lscvToID("nn", parser.getScript(), parser.getCountry(), null));
            }
        }

        return parser.getName();
    }

    /**
     * Given a keyword and a value, return a new locale with an updated
     * keyword and value.  If keyword is null, this removes all keywords from the locale id.
     * Otherwise, if the value is null, this removes the value for this keyword from the
     * locale id.  Otherwise, this adds/replaces the value for this keyword in the locale id.
     * The keyword and value must not be empty.
     * @param keyword the keyword to add/remove, or null to remove all keywords.
     * @param value the value to add/set, or null to remove this particular keyword.
     * @return the updated locale
     * @stable ICU 3.2
     */
    public ULocale setKeywordValue(String keyword, String value) {
        return new ULocale(setKeywordValue(localeID, keyword, value), (Locale)null);
    }

    /**
     * Given a locale id, a keyword, and a value, return a new locale id with an updated
     * keyword and value.  If keyword is null, this removes all keywords from the locale id.
     * Otherwise, if the value is null, this removes the value for this keyword from the
     * locale id.  Otherwise, this adds/replaces the value for this keyword in the locale id.
     * The keyword and value must not be empty.
     * @param localeID the locale id to modify
     * @param keyword the keyword to add/remove, or null to remove all keywords.
     * @param value the value to add/set, or null to remove this particular keyword.
     * @return the updated locale id
     * @stable ICU 3.2
     */
    public static String setKeywordValue(String localeID, String keyword, String value) {
        LocaleIDParser parser = new LocaleIDParser(localeID);
        parser.setKeywordValue(keyword, value);
        return parser.getName();
    }

    /*
     * Given a locale id, a keyword, and a value, return a new locale id with an updated
     * keyword and value, if the keyword does not already have a value.  The keyword and
     * value must not be null or empty.
     * @param localeID the locale id to modify
     * @param keyword the keyword to add, if not already present
     * @param value the value to add, if not already present
     * @return the updated locale id
     */
/*    private static String defaultKeywordValue(String localeID, String keyword, String value) {
        LocaleIDParser parser = new LocaleIDParser(localeID);
        parser.defaultKeywordValue(keyword, value);
        return parser.getName();
    }*/

    /**
     * Returns a three-letter abbreviation for this locale's language.  If the locale
     * doesn't specify a language, returns the empty string.  Otherwise, returns
     * a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @stable ICU 3.0
     */
    public String getISO3Language(){
        return getISO3Language(localeID);
    }

    /**
     * Returns a three-letter abbreviation for this locale's language.  If the locale
     * doesn't specify a language, returns the empty string.  Otherwise, returns
     * a lowercase ISO 639-2/T language code.
     * The ISO 639-2 language codes can be found on-line at
     *   <a href="ftp://dkuug.dk/i18n/iso-639-2.txt"><code>ftp://dkuug.dk/i18n/iso-639-2.txt</code></a>
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter language abbreviation is not available for this locale.
     * @stable ICU 3.0
     */
    public static String getISO3Language(String localeID) {
        return LocaleIDs.getISO3Language(getLanguage(localeID));
    }

    /**
     * Returns a three-letter abbreviation for this locale's country/region.  If the locale
     * doesn't specify a country, returns the empty string.  Otherwise, returns
     * an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @stable ICU 3.0
     */
    public String getISO3Country() {
        return getISO3Country(localeID);
    }

    /**
     * Returns a three-letter abbreviation for this locale's country/region.  If the locale
     * doesn't specify a country, returns the empty string.  Otherwise, returns
     * an uppercase ISO 3166 3-letter country code.
     * @exception MissingResourceException Throws MissingResourceException if the
     * three-letter country abbreviation is not available for this locale.
     * @stable ICU 3.0
     */
    public static String getISO3Country(String localeID) {
        return LocaleIDs.getISO3Country(getCountry(localeID));
    }

    // display names

    /**
     * Returns this locale's language localized for display in the default <code>DISPLAY</code> locale.
     * @return the localized language name.
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public String getDisplayLanguage() {
        return getDisplayLanguageInternal(this, getDefault(Category.DISPLAY), false);
    }

    /**
     * {@icu} Returns this locale's language localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized language name.
     * @stable ICU 3.0
     */
    public String getDisplayLanguage(ULocale displayLocale) {
        return getDisplayLanguageInternal(this, displayLocale, false);
    }

    /**
     * Returns a locale's language localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose language will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized language name.
     * @stable ICU 3.0
     */
    public static String getDisplayLanguage(String localeID, String displayLocaleID) {
        return getDisplayLanguageInternal(new ULocale(localeID), new ULocale(displayLocaleID),
                false);
    }

    /**
     * Returns a locale's language localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose language will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized language name.
     * @stable ICU 3.0
     */
    public static String getDisplayLanguage(String localeID, ULocale displayLocale) {
        return getDisplayLanguageInternal(new ULocale(localeID), displayLocale, false);
    }
    /**
     * {@icu} Returns this locale's language localized for display in the default <code>DISPLAY</code> locale.
     * If a dialect name is present in the data, then it is returned.
     * @return the localized language name.
     * @see Category#DISPLAY
     * @stable ICU 4.4
     */
    public String getDisplayLanguageWithDialect() {
        return getDisplayLanguageInternal(this, getDefault(Category.DISPLAY), true);
    }

    /**
     * {@icu} Returns this locale's language localized for display in the provided locale.
     * If a dialect name is present in the data, then it is returned.
     * @param displayLocale the locale in which to display the name.
     * @return the localized language name.
     * @stable ICU 4.4
     */
    public String getDisplayLanguageWithDialect(ULocale displayLocale) {
        return getDisplayLanguageInternal(this, displayLocale, true);
    }

    /**
     * {@icu} Returns a locale's language localized for display in the provided locale.
     * If a dialect name is present in the data, then it is returned.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose language will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized language name.
     * @stable ICU 4.4
     */
    public static String getDisplayLanguageWithDialect(String localeID, String displayLocaleID) {
        return getDisplayLanguageInternal(new ULocale(localeID), new ULocale(displayLocaleID),
                true);
    }

    /**
     * {@icu} Returns a locale's language localized for display in the provided locale.
     * If a dialect name is present in the data, then it is returned.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose language will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized language name.
     * @stable ICU 4.4
     */
    public static String getDisplayLanguageWithDialect(String localeID, ULocale displayLocale) {
        return getDisplayLanguageInternal(new ULocale(localeID), displayLocale, true);
    }

    private static String getDisplayLanguageInternal(ULocale locale, ULocale displayLocale,
            boolean useDialect) {
        //#com.ibm.icu.base
        // No dialect support
        return locale.toLocale().getDisplayLanguage(displayLocale.toLocale());
    }

    /**
     * {@icu} Returns this locale's script localized for display in the default <code>DISPLAY</code> locale.
     * @return the localized script name.
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public String getDisplayScript() {
        return getDisplayScriptInternal(this, getDefault(Category.DISPLAY));
    }

    /**
     * {@icu} Returns this locale's script localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized script name.
     * @stable ICU 3.0
     */
    public String getDisplayScript(ULocale displayLocale) {
        return getDisplayScriptInternal(this, displayLocale);
    }

    /**
     * {@icu} Returns a locale's script localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose script will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized script name.
     * @stable ICU 3.0
     */
    public static String getDisplayScript(String localeID, String displayLocaleID) {
        return getDisplayScriptInternal(new ULocale(localeID), new ULocale(displayLocaleID));
    }

    /**
     * {@icu} Returns a locale's script localized for display in the provided locale.
     * @param localeID the id of the locale whose script will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized script name.
     * @stable ICU 3.0
     */
    public static String getDisplayScript(String localeID, ULocale displayLocale) {
        return getDisplayScriptInternal(new ULocale(localeID), displayLocale);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayScriptInternal(ULocale locale, ULocale displayLocale) {
        //#com.ibm.icu.base
        String dispScript = null;
        try {
            // Calling Locale#getDisplayScript on Java 7 or later
            Method mGetDisplayScript = Locale.class.getMethod("getDisplayScript", Locale.class);
            dispScript = (String) mGetDisplayScript.invoke(locale.toLocale(), displayLocale.toLocale());
            
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }

        if (dispScript == null) {
            dispScript = locale.getScript();
        }
        return dispScript;
    }

    /**
     * Returns this locale's country localized for display in the default <code>DISPLAY</code> locale.
     * @return the localized country name.
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public String getDisplayCountry() {
        return getDisplayCountryInternal(this, getDefault(Category.DISPLAY));
    }

    /**
     * Returns this locale's country localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized country name.
     * @stable ICU 3.0
     */
    public String getDisplayCountry(ULocale displayLocale){
        return getDisplayCountryInternal(this, displayLocale);
    }

    /**
     * Returns a locale's country localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose country will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized country name.
     * @stable ICU 3.0
     */
    public static String getDisplayCountry(String localeID, String displayLocaleID) {
        return getDisplayCountryInternal(new ULocale(localeID), new ULocale(displayLocaleID));
    }

    /**
     * Returns a locale's country localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose country will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized country name.
     * @stable ICU 3.0
     */
    public static String getDisplayCountry(String localeID, ULocale displayLocale) {
        return getDisplayCountryInternal(new ULocale(localeID), displayLocale);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayCountryInternal(ULocale locale, ULocale displayLocale) {
        //#com.ibm.icu.base
        return locale.toLocale().getDisplayCountry(displayLocale.toLocale());
    }

    /**
     * Returns this locale's variant localized for display in the default <code>DISPLAY</code> locale.
     * @return the localized variant name.
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public String getDisplayVariant() {
        return getDisplayVariantInternal(this, getDefault(Category.DISPLAY));
    }

    /**
     * Returns this locale's variant localized for display in the provided locale.
     * @param displayLocale the locale in which to display the name.
     * @return the localized variant name.
     * @stable ICU 3.0
     */
    public String getDisplayVariant(ULocale displayLocale) {
        return getDisplayVariantInternal(this, displayLocale);
    }

    /**
     * Returns a locale's variant localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose variant will be displayed
     * @param displayLocaleID the id of the locale in which to display the name.
     * @return the localized variant name.
     * @stable ICU 3.0
     */
    public static String getDisplayVariant(String localeID, String displayLocaleID){
        return getDisplayVariantInternal(new ULocale(localeID), new ULocale(displayLocaleID));
    }

    /**
     * Returns a locale's variant localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose variant will be displayed.
     * @param displayLocale the locale in which to display the name.
     * @return the localized variant name.
     * @stable ICU 3.0
     */
    public static String getDisplayVariant(String localeID, ULocale displayLocale) {
        return getDisplayVariantInternal(new ULocale(localeID), displayLocale);
    }

    private static String getDisplayVariantInternal(ULocale locale, ULocale displayLocale) {
        //#com.ibm.icu.base
        return locale.toLocale().getDisplayVariant(displayLocale.toLocale());
    }

    /**
     * {@icu} Returns a keyword localized for display in the default <code>DISPLAY</code> locale.
     * @param keyword the keyword to be displayed.
     * @return the localized keyword name.
     * @see #getKeywords()
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public static String getDisplayKeyword(String keyword) {
        return getDisplayKeywordInternal(keyword, getDefault(Category.DISPLAY));
    }

    /**
     * {@icu} Returns a keyword localized for display in the specified locale.
     * @param keyword the keyword to be displayed.
     * @param displayLocaleID the id of the locale in which to display the keyword.
     * @return the localized keyword name.
     * @see #getKeywords(String)
     * @stable ICU 3.0
     */
    public static String getDisplayKeyword(String keyword, String displayLocaleID) {
        return getDisplayKeywordInternal(keyword, new ULocale(displayLocaleID));
    }

    /**
     * {@icu} Returns a keyword localized for display in the specified locale.
     * @param keyword the keyword to be displayed.
     * @param displayLocale the locale in which to display the keyword.
     * @return the localized keyword name.
     * @see #getKeywords(String)
     * @stable ICU 3.0
     */
    public static String getDisplayKeyword(String keyword, ULocale displayLocale) {
        return getDisplayKeywordInternal(keyword, displayLocale);
    }

    private static String getDisplayKeywordInternal(String keyword, ULocale displayLocale) {
        //#com.ibm.icu.base
        // No localization
        return keyword;
    }

    /**
     * {@icu} Returns a keyword value localized for display in the default <code>DISPLAY</code> locale.
     * @param keyword the keyword whose value is to be displayed.
     * @return the localized value name.
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public String getDisplayKeywordValue(String keyword) {
        return getDisplayKeywordValueInternal(this, keyword, getDefault(Category.DISPLAY));
    }

    /**
     * {@icu} Returns a keyword value localized for display in the specified locale.
     * @param keyword the keyword whose value is to be displayed.
     * @param displayLocale the locale in which to display the value.
     * @return the localized value name.
     * @stable ICU 3.0
     */
    public String getDisplayKeywordValue(String keyword, ULocale displayLocale) {
        return getDisplayKeywordValueInternal(this, keyword, displayLocale);
    }

    /**
     * {@icu} Returns a keyword value localized for display in the specified locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose keyword value is to be displayed.
     * @param keyword the keyword whose value is to be displayed.
     * @param displayLocaleID the id of the locale in which to display the value.
     * @return the localized value name.
     * @stable ICU 3.0
     */
    public static String getDisplayKeywordValue(String localeID, String keyword,
            String displayLocaleID) {
        return getDisplayKeywordValueInternal(new ULocale(localeID), keyword,
                new ULocale(displayLocaleID));
    }

    /**
     * {@icu} Returns a keyword value localized for display in the specified locale.
     * This is a cover for the ICU4C API.
     * @param localeID the id of the locale whose keyword value is to be displayed.
     * @param keyword the keyword whose value is to be displayed.
     * @param displayLocale the id of the locale in which to display the value.
     * @return the localized value name.
     * @stable ICU 3.0
     */
    public static String getDisplayKeywordValue(String localeID, String keyword,
            ULocale displayLocale) {
        return getDisplayKeywordValueInternal(new ULocale(localeID), keyword, displayLocale);
    }

    // displayLocaleID is canonical, localeID need not be since parsing will fix this.
    private static String getDisplayKeywordValueInternal(ULocale locale, String keyword,
            ULocale displayLocale) {
        //#com.ibm.icu.base
        keyword = AsciiUtil.toLowerString(keyword.trim());
        String value = locale.getKeywordValue(keyword);
        // No localization
        return value;
    }

    /**
     * Returns this locale name localized for display in the default <code>DISPLAY</code> locale.
     * @return the localized locale name.
     * @see Category#DISPLAY
     * @stable ICU 3.0
     */
    public String getDisplayName() {
        return getDisplayNameInternal(this, getDefault(Category.DISPLAY));
    }

    /**
     * Returns this locale name localized for display in the provided locale.
     * @param displayLocale the locale in which to display the locale name.
     * @return the localized locale name.
     * @stable ICU 3.0
     */
    public String getDisplayName(ULocale displayLocale) {
        return getDisplayNameInternal(this, displayLocale);
    }

    /**
     * Returns the locale ID localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the locale whose name is to be displayed.
     * @param displayLocaleID the id of the locale in which to display the locale name.
     * @return the localized locale name.
     * @stable ICU 3.0
     */
    public static String getDisplayName(String localeID, String displayLocaleID) {
        return getDisplayNameInternal(new ULocale(localeID), new ULocale(displayLocaleID));
    }

    /**
     * Returns the locale ID localized for display in the provided locale.
     * This is a cover for the ICU4C API.
     * @param localeID the locale whose name is to be displayed.
     * @param displayLocale the locale in which to display the locale name.
     * @return the localized locale name.
     * @stable ICU 3.0
     */
    public static String getDisplayName(String localeID, ULocale displayLocale) {
        return getDisplayNameInternal(new ULocale(localeID), displayLocale);
    }

    private static String getDisplayNameInternal(ULocale locale, ULocale displayLocale) {
        //#com.ibm.icu.base
        return locale.toLocale().getDisplayName(displayLocale.toLocale());
    }

    /**
     * {@icu} Returns this locale name localized for display in the default <code>DISPLAY</code> locale.
     * If a dialect name is present in the locale data, then it is returned.
     * @return the localized locale name.
     * @see Category#DISPLAY
     * @stable ICU 4.4
     */
    public String getDisplayNameWithDialect() {
        return getDisplayNameWithDialectInternal(this, getDefault(Category.DISPLAY));
    }

    /**
     * {@icu} Returns this locale name localized for display in the provided locale.
     * If a dialect name is present in the locale data, then it is returned.
     * @param displayLocale the locale in which to display the locale name.
     * @return the localized locale name.
     * @stable ICU 4.4
     */
    public String getDisplayNameWithDialect(ULocale displayLocale) {
        return getDisplayNameWithDialectInternal(this, displayLocale);
    }

    /**
     * {@icu} Returns the locale ID localized for display in the provided locale.
     * If a dialect name is present in the locale data, then it is returned.
     * This is a cover for the ICU4C API.
     * @param localeID the locale whose name is to be displayed.
     * @param displayLocaleID the id of the locale in which to display the locale name.
     * @return the localized locale name.
     * @stable ICU 4.4
     */
    public static String getDisplayNameWithDialect(String localeID, String displayLocaleID) {
        return getDisplayNameWithDialectInternal(new ULocale(localeID),
                new ULocale(displayLocaleID));
    }

    /**
     * {@icu} Returns the locale ID localized for display in the provided locale.
     * If a dialect name is present in the locale data, then it is returned.
     * This is a cover for the ICU4C API.
     * @param localeID the locale whose name is to be displayed.
     * @param displayLocale the locale in which to display the locale name.
     * @return the localized locale name.
     * @stable ICU 4.4
     */
    public static String getDisplayNameWithDialect(String localeID, ULocale displayLocale) {
        return getDisplayNameWithDialectInternal(new ULocale(localeID), displayLocale);
    }

    private static String getDisplayNameWithDialectInternal(ULocale locale, ULocale displayLocale) {
        //#com.ibm.icu.base
        // No dialect handling
        return locale.toLocale().getDisplayName(displayLocale.toLocale());
    }

    /**
     * {@icu} Returns this locale's layout orientation for characters.  The possible
     * values are "left-to-right", "right-to-left", "top-to-bottom" or
     * "bottom-to-top".
     * @return The locale's layout orientation for characters.
     * @stable ICU 4.0
     */
    public String getCharacterOrientation() {
        //#com.ibm.icu.base
        // Hardcoded
        String lang = getLanguage();
        if (lang.equals("ar") || lang.equals("fa") || lang.equals("he") || lang.equals("ps") || lang.equals("ur")) {
            return "right-to-left";
        }
        String script = getScript();
        if (script.equals("Arab")) {
            return "right-to-left";
        }
        return "left-to-right";
    }

    /**
     * {@icu} Returns this locale's layout orientation for lines.  The possible
     * values are "left-to-right", "right-to-left", "top-to-bottom" or
     * "bottom-to-top".
     * @return The locale's layout orientation for lines.
     * @stable ICU 4.0
     */
    public String getLineOrientation() {
        //#com.ibm.icu.base
        return "top-to-bottom";
    }

    /**
     * {@icu} Selector for <tt>getLocale()</tt> indicating the locale of the
     * resource containing the data.  This is always at or above the
     * valid locale.  If the valid locale does not contain the
     * specific data being requested, then the actual locale will be
     * above the valid locale.  If the object was not constructed from
     * locale data, then the valid locale is <i>null</i>.
     *
     * @draft ICU 2.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public static Type ACTUAL_LOCALE = new Type();

    /**
     * {@icu} Selector for <tt>getLocale()</tt> indicating the most specific
     * locale for which any data exists.  This is always at or above
     * the requested locale, and at or below the actual locale.  If
     * the requested locale does not correspond to any resource data,
     * then the valid locale will be above the requested locale.  If
     * the object was not constructed from locale data, then the
     * actual locale is <i>null</i>.
     *
     * <p>Note: The valid locale will be returned correctly in ICU
     * 3.0 or later.  In ICU 2.8, it is not returned correctly.
     * @draft ICU 2.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public static Type VALID_LOCALE = new Type();

    /**
     * Opaque selector enum for <tt>getLocale()</tt>.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @draft ICU 2.8 (retainAll)
     * @provisional This API might change or be removed in a future release.
     */
    public static final class Type {
        private Type() {}
    }

  /**
    * {@icu} Based on a HTTP formatted list of acceptable locales, determine an available
    * locale for the user.  NullPointerException is thrown if acceptLanguageList or
    * availableLocales is null.  If fallback is non-null, it will contain true if a
    * fallback locale (one not in the acceptLanguageList) was returned.  The value on
    * entry is ignored.  ULocale will be one of the locales in availableLocales, or the
    * ROOT ULocale if if a ROOT locale was used as a fallback (because nothing else in
    * availableLocales matched).  No ULocale array element should be null; behavior is
    * undefined if this is the case.
    * @param acceptLanguageList list in HTTP "Accept-Language:" format of acceptable locales
    * @param availableLocales list of available locales. One of these will be returned.
    * @param fallback if non-null, a 1-element array containing a boolean to be set with
    * the fallback status
    * @return one of the locales from the availableLocales list, or null if none match
    * @stable ICU 3.4
    */
    public static ULocale acceptLanguage(String acceptLanguageList, ULocale[] availableLocales,
                                         boolean[] fallback) {
        if (acceptLanguageList == null) {
            throw new NullPointerException();
        }
        ULocale acceptList[] = null;
        try {
            acceptList = parseAcceptLanguage(acceptLanguageList, true);
        } catch (ParseException pe) {
            acceptList = null;
        }
        if (acceptList == null) {
            return null;
        }
        return acceptLanguage(acceptList, availableLocales, fallback);
    }

    /**
    * {@icu} Based on a list of acceptable locales, determine an available locale for the
    * user.  NullPointerException is thrown if acceptLanguageList or availableLocales is
    * null.  If fallback is non-null, it will contain true if a fallback locale (one not
    * in the acceptLanguageList) was returned.  The value on entry is ignored.  ULocale
    * will be one of the locales in availableLocales, or the ROOT ULocale if if a ROOT
    * locale was used as a fallback (because nothing else in availableLocales matched).
    * No ULocale array element should be null; behavior is undefined if this is the case.
    * @param acceptLanguageList list of acceptable locales
    * @param availableLocales list of available locales. One of these will be returned.
    * @param fallback if non-null, a 1-element array containing a boolean to be set with
    * the fallback status
    * @return one of the locales from the availableLocales list, or null if none match
    * @stable ICU 3.4
    */

    public static ULocale acceptLanguage(ULocale[] acceptLanguageList, ULocale[]
    availableLocales, boolean[] fallback) {
        // fallbacklist
        int i,j;
        if(fallback != null) {
            fallback[0]=true;
        }
        for(i=0;i<acceptLanguageList.length;i++) {
            ULocale aLocale = acceptLanguageList[i];
            boolean[] setFallback = fallback;
            do {
                for(j=0;j<availableLocales.length;j++) {
                    if(availableLocales[j].equals(aLocale)) {
                        if(setFallback != null) {
                            setFallback[0]=false; // first time with this locale - not a fallback.
                        }
                        return availableLocales[j];
                    }
                    // compare to scriptless alias, so locales such as
                    // zh_TW, zh_CN are considered as available locales - see #7190
                    if (aLocale.getScript().length() == 0
                            && availableLocales[j].getScript().length() > 0
                            && availableLocales[j].getLanguage().equals(aLocale.getLanguage())
                            && availableLocales[j].getCountry().equals(aLocale.getCountry())
                            && availableLocales[j].getVariant().equals(aLocale.getVariant())) {
                        ULocale minAvail = ULocale.minimizeSubtags(availableLocales[j]);
                        if (minAvail.getScript().length() == 0) {
                            if(setFallback != null) {
                                setFallback[0] = false; // not a fallback.
                            }
                            return aLocale;
                        }
                    }
                }
                Locale loc = aLocale.toLocale();
                Locale parent = LocaleUtility.fallback(loc);
                if(parent != null) {
                    aLocale = new ULocale(parent);
                } else {
                    aLocale = null;
                }
                setFallback = null; // Do not set fallback in later iterations
            } while (aLocale != null);
        }
        return null;
    }

   /**
    * {@icu} Based on a HTTP formatted list of acceptable locales, determine an available
    * locale for the user.  NullPointerException is thrown if acceptLanguageList or
    * availableLocales is null.  If fallback is non-null, it will contain true if a
    * fallback locale (one not in the acceptLanguageList) was returned.  The value on
    * entry is ignored.  ULocale will be one of the locales in availableLocales, or the
    * ROOT ULocale if if a ROOT locale was used as a fallback (because nothing else in
    * availableLocales matched).  No ULocale array element should be null; behavior is
    * undefined if this is the case.  This function will choose a locale from the
    * ULocale.getAvailableLocales() list as available.
    * @param acceptLanguageList list in HTTP "Accept-Language:" format of acceptable locales
    * @param fallback if non-null, a 1-element array containing a boolean to be set with
    * the fallback status
    * @return one of the locales from the ULocale.getAvailableLocales() list, or null if
    * none match
    * @stable ICU 3.4
    */
    public static ULocale acceptLanguage(String acceptLanguageList, boolean[] fallback) {
        return acceptLanguage(acceptLanguageList, ULocale.getAvailableLocales(),
                                fallback);
    }

   /**
    * {@icu} Based on an ordered array of acceptable locales, determine an available
    * locale for the user.  NullPointerException is thrown if acceptLanguageList or
    * availableLocales is null.  If fallback is non-null, it will contain true if a
    * fallback locale (one not in the acceptLanguageList) was returned.  The value on
    * entry is ignored.  ULocale will be one of the locales in availableLocales, or the
    * ROOT ULocale if if a ROOT locale was used as a fallback (because nothing else in
    * availableLocales matched).  No ULocale array element should be null; behavior is
    * undefined if this is the case.  This function will choose a locale from the
    * ULocale.getAvailableLocales() list as available.
    * @param acceptLanguageList ordered array of acceptable locales (preferred are listed first)
    * @param fallback if non-null, a 1-element array containing a boolean to be set with
    * the fallback status
    * @return one of the locales from the ULocale.getAvailableLocales() list, or null if none match
    * @stable ICU 3.4
    */
    public static ULocale acceptLanguage(ULocale[] acceptLanguageList, boolean[] fallback) {
        return acceptLanguage(acceptLanguageList, ULocale.getAvailableLocales(),
                fallback);
    }

    /**
     * Package local method used for parsing Accept-Language string
     */
    static ULocale[] parseAcceptLanguage(String acceptLanguage, boolean isLenient) 
        throws ParseException {
        class ULocaleAcceptLanguageQ implements Comparable<ULocaleAcceptLanguageQ> {
            private double q;
            private double serial;
            public ULocaleAcceptLanguageQ(double theq, int theserial) {
                q = theq;
                serial = theserial;
            }
            public int compareTo(ULocaleAcceptLanguageQ other) {
                if (q > other.q) { // reverse - to sort in descending order
                    return -1;
                } else if (q < other.q) {
                    return 1;
                }
                if (serial < other.serial) {
                    return -1;
                } else if (serial > other.serial) {
                    return 1;
                } else {
                    return 0; // same object
                }
            }
        }

        // parse out the acceptLanguage into an array
        TreeMap<ULocaleAcceptLanguageQ, ULocale> map = 
            new TreeMap<ULocaleAcceptLanguageQ, ULocale>();
        StringBuilder languageRangeBuf = new StringBuilder();
        StringBuilder qvalBuf = new StringBuilder();
        int state = 0;
        acceptLanguage += ","; // append comma to simplify the parsing code
        int n;
        boolean subTag = false;
        boolean q1 = false;
        for (n = 0; n < acceptLanguage.length(); n++) {
            boolean gotLanguageQ = false;
            char c = acceptLanguage.charAt(n);
            switch (state) {
            case 0: // before language-range start
                if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                    // in language-range
                    languageRangeBuf.append(c);
                    state = 1;
                    subTag = false;
                } else if (c == '*') {
                    languageRangeBuf.append(c);
                    state = 2;
                } else if (c != ' ' && c != '\t') {
                    // invalid character
                    state = -1;
                }
                break;
            case 1: // in language-range
                if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                    languageRangeBuf.append(c);
                } else if (c == '-') {
                    subTag = true;
                    languageRangeBuf.append(c);
                } else if (c == '_') {
                    if (isLenient) {
                        subTag = true;
                        languageRangeBuf.append(c);
                    } else {
                        state = -1;
                    }
                } else if ('0' <= c && c <= '9') {
                    if (subTag) {
                        languageRangeBuf.append(c);
                    } else {
                        // DIGIT is allowed only in language sub tag
                        state = -1;
                    }
                } else if (c == ',') {
                    // language-q end
                    gotLanguageQ = true;
                } else if (c == ' ' || c == '\t') {
                    // language-range end
                    state = 3;
                } else if (c == ';') {
                    // before q
                    state = 4;
                } else {
                    // invalid character for language-range
                    state = -1;
                }
                break;
            case 2: // saw wild card range
                if (c == ',') {
                    // language-q end
                    gotLanguageQ = true;
                } else if (c == ' ' || c == '\t') {
                    // language-range end
                    state = 3;
                } else if (c == ';') {
                    // before q
                    state = 4;
                } else {
                    // invalid
                    state = -1;
                }
                break;
            case 3: // language-range end
                if (c == ',') {
                    // language-q end
                    gotLanguageQ = true;
                } else if (c == ';') {
                    // before q
                    state =4;
                } else if (c != ' ' && c != '\t') {
                    // invalid
                    state = -1;
                }
                break;
            case 4: // before q
                if (c == 'q') {
                    // before equal
                    state = 5;
                } else if (c != ' ' && c != '\t') {
                    // invalid
                    state = -1;
                }
                break;
            case 5: // before equal
                if (c == '=') {
                    // before q value
                    state = 6;
                } else if (c != ' ' && c != '\t') {
                    // invalid
                    state = -1;
                }
                break;
            case 6: // before q value
                if (c == '0') {
                    // q value start with 0
                    q1 = false;
                    qvalBuf.append(c);
                    state = 7;
                } else if (c == '1') {
                    // q value start with 1
                    qvalBuf.append(c);
                    state = 7;
                } else if (c == '.') {
                    if (isLenient) {
                        qvalBuf.append(c);
                        state = 8;
                    } else {
                        state = -1;
                    }
                } else if (c != ' ' && c != '\t') {
                    // invalid
                    state = -1;
                }
                break;
            case 7: // q value start
                if (c == '.') {
                    // before q value fraction part
                    qvalBuf.append(c);
                    state = 8;
                } else if (c == ',') {
                    // language-q end
                    gotLanguageQ = true;
                } else if (c == ' ' || c == '\t') {
                    // after q value
                    state = 10;
                } else {
                    // invalid
                    state = -1;
                }
                break;
            case 8: // before q value fraction part
                if ('0' <= c || c <= '9') {
                    if (q1 && c != '0' && !isLenient) {
                        // if q value starts with 1, the fraction part must be 0
                        state = -1;
                    } else {
                        // in q value fraction part
                        qvalBuf.append(c);
                        state = 9;
                    }
                } else {
                    // invalid
                    state = -1;
                }
                break;
            case 9: // in q value fraction part
                if ('0' <= c && c <= '9') {
                    if (q1 && c != '0') {
                        // if q value starts with 1, the fraction part must be 0
                        state = -1;
                    } else {
                        qvalBuf.append(c);
                    }
                } else if (c == ',') {
                    // language-q end
                    gotLanguageQ = true;
                } else if (c == ' ' || c == '\t') {
                    // after q value
                    state = 10;
                } else {
                    // invalid
                    state = -1;
                }
                break;
            case 10: // after q value
                if (c == ',') {
                    // language-q end
                    gotLanguageQ = true;
                } else if (c != ' ' && c != '\t') {
                    // invalid
                    state = -1;
                }
                break;
            }
            if (state == -1) {
                // error state
                throw new ParseException("Invalid Accept-Language", n);
            }
            if (gotLanguageQ) {
                double q = 1.0;
                if (qvalBuf.length() != 0) {
                    try {
                        q = Double.parseDouble(qvalBuf.toString());
                    } catch (NumberFormatException nfe) {
                        // Already validated, so it should never happen
                        q = 1.0;
                    }
                    if (q > 1.0) {
                        q = 1.0;
                    }
                }
                if (languageRangeBuf.charAt(0) != '*') {
                    int serial = map.size();
                    ULocaleAcceptLanguageQ entry = new ULocaleAcceptLanguageQ(q, serial);
                    // sort in reverse order..   1.0, 0.9, 0.8 .. etc
                    map.put(entry, new ULocale(canonicalize(languageRangeBuf.toString())));
                }

                // reset buffer and parse state
                languageRangeBuf.setLength(0);
                qvalBuf.setLength(0);
                state = 0;
            }
        }
        if (state != 0) {
            // Well, the parser should handle all cases.  So just in case.
            throw new ParseException("Invalid AcceptlLanguage", n);
        }

        // pull out the map
        ULocale acceptList[] = map.values().toArray(new ULocale[map.size()]);
        return acceptList;
    }

    private static final String UNDEFINED_LANGUAGE = "und";

    /**
     * {@icu} Adds the likely subtags for a provided locale ID, per the algorithm
     * described in the following CLDR technical report:
     *
     *   http://www.unicode.org/reports/tr35/#Likely_Subtags
     *
     * If the provided ULocale instance is already in the maximal form, or there is no
     * data available available for maximization, it will be returned.  For example,
     * "und-Zzzz" cannot be maximized, since there is no reasonable maximization.
     * Otherwise, a new ULocale instance with the maximal form is returned.
     *
     * Examples:
     *
     * "en" maximizes to "en_Latn_US"
     *
     * "de" maximizes to "de_Latn_US"
     *
     * "sr" maximizes to "sr_Cyrl_RS"
     *
     * "sh" maximizes to "sr_Latn_RS" (Note this will not reverse.)
     *
     * "zh_Hani" maximizes to "zh_Hans_CN" (Note this will not reverse.)
     *
     * @param loc The ULocale to maximize
     * @return The maximized ULocale instance.
     * @stable ICU 4.0
     */
    public static ULocale addLikelySubtags(ULocale loc) {
        //#com.ibm.icu.base
        return loc;
    }

    /**
     * {@icu} Minimizes the subtags for a provided locale ID, per the algorithm described
     * in the following CLDR technical report:<blockquote>
     *
     *   <a href="http://www.unicode.org/reports/tr35/#Likely_Subtags"
     *>http://www.unicode.org/reports/tr35/#Likely_Subtags</a></blockquote>
     *
     * If the provided ULocale instance is already in the minimal form, or there
     * is no data available for minimization, it will be returned.  Since the
     * minimization algorithm relies on proper maximization, see the comments
     * for addLikelySubtags for reasons why there might not be any data.
     *
     * Examples:<pre>
     *
     * "en_Latn_US" minimizes to "en"
     *
     * "de_Latn_US" minimizes to "de"
     *
     * "sr_Cyrl_RS" minimizes to "sr"
     *
     * "zh_Hant_TW" minimizes to "zh_TW" (The region is preferred to the
     * script, and minimizing to "zh" would imply "zh_Hans_CN".) </pre>
     *
     * @param loc The ULocale to minimize
     * @return The minimized ULocale instance.
     * @stable ICU 4.0
     */
    public static ULocale minimizeSubtags(ULocale loc) {
        //#com.ibm.icu.base
        return loc;
    }

    /**
     * A trivial utility function that checks for a null
     * reference or checks the length of the supplied String.
     *
     *   @param string The string to check
     *
     *   @return true if the String is empty, or if the reference is null.
     */
    private static boolean isEmptyString(String string) {
      return string == null || string.length() == 0;
    }

    /**
     * Append a tag to a StringBuilder, adding the separator if necessary.The tag must
     * not be a zero-length string.
     *
     * @param tag The tag to add.
     * @param buffer The output buffer.
     **/
    private static void appendTag(String tag, StringBuilder buffer) {
        if (buffer.length() != 0) {
            buffer.append(UNDERSCORE);
        }

        buffer.append(tag);
    }

    /**
     * Create a tag string from the supplied parameters.  The lang, script and region
     * parameters may be null references.
     *
     * If any of the language, script or region parameters are empty, and the alternateTags
     * parameter is not null, it will be parsed for potential language, script and region tags
     * to be used when constructing the new tag.  If the alternateTags parameter is null, or
     * it contains no language tag, the default tag for the unknown language is used.
     *
     * @param lang The language tag to use.
     * @param script The script tag to use.
     * @param region The region tag to use.
     * @param trailing Any trailing data to append to the new tag.
     * @param alternateTags A string containing any alternate tags.
     * @return The new tag string.
     **/
    private static String createTagString(String lang, String script, String region,
        String trailing, String alternateTags) {

        LocaleIDParser parser = null;
        boolean regionAppended = false;

        StringBuilder tag = new StringBuilder();

        if (!isEmptyString(lang)) {
            appendTag(
                lang,
                tag);
        }
        else if (isEmptyString(alternateTags)) {
            /*
             * Append the value for an unknown language, if
             * we found no language.
             */
            appendTag(
                UNDEFINED_LANGUAGE,
                tag);
        }
        else {
            parser = new LocaleIDParser(alternateTags);

            String alternateLang = parser.getLanguage();

            /*
             * Append the value for an unknown language, if
             * we found no language.
             */
            appendTag(
                !isEmptyString(alternateLang) ? alternateLang : UNDEFINED_LANGUAGE,
                tag);
        }

        if (!isEmptyString(script)) {
            appendTag(
                script,
                tag);
        }
        else if (!isEmptyString(alternateTags)) {
            /*
             * Parse the alternateTags string for the script.
             */
            if (parser == null) {
                parser = new LocaleIDParser(alternateTags);
            }

            String alternateScript = parser.getScript();

            if (!isEmptyString(alternateScript)) {
                appendTag(
                    alternateScript,
                    tag);
            }
        }

        if (!isEmptyString(region)) {
            appendTag(
                region,
                tag);

            regionAppended = true;
        }
        else if (!isEmptyString(alternateTags)) {
            /*
             * Parse the alternateTags string for the region.
             */
            if (parser == null) {
                parser = new LocaleIDParser(alternateTags);
            }

            String alternateRegion = parser.getCountry();

            if (!isEmptyString(alternateRegion)) {
                appendTag(
                    alternateRegion,
                    tag);

                regionAppended = true;
            }
        }

        if (trailing != null && trailing.length() > 1) {
            /*
             * The current ICU format expects two underscores
             * will separate the variant from the preceeding
             * parts of the tag, if there is no region.
             */
            int separators = 0;

            if (trailing.charAt(0) == UNDERSCORE) {
                if (trailing.charAt(1) == UNDERSCORE) {
                    separators = 2;
                }
                }
                else {
                    separators = 1;
                }

            if (regionAppended) {
                /*
                 * If we appended a region, we may need to strip
                 * the extra separator from the variant portion.
                 */
                if (separators == 2) {
                    tag.append(trailing.substring(1));
                }
                else {
                    tag.append(trailing);
                }
            }
            else {
                /*
                 * If we did not append a region, we may need to add
                 * an extra separator to the variant portion.
                 */
                if (separators == 1) {
                    tag.append(UNDERSCORE);
                }
                tag.append(trailing);
            }
        }

        return tag.toString();
    }

    /**
     * Create a tag string from the supplied parameters.  The lang, script and region
     * parameters may be null references.If the lang parameter is an empty string, the
     * default value for an unknown language is written to the output buffer.
     *
     * @param lang The language tag to use.
     * @param script The script tag to use.
     * @param region The region tag to use.
     * @param trailing Any trailing data to append to the new tag.
     * @return The new String.
     **/
    static String createTagString(String lang, String script, String region, String trailing) {
        return createTagString(lang, script, region, trailing, null);
    }

    // --------------------------------
    //      BCP47/OpenJDK APIs
    // --------------------------------

    /**
     * {@icu} The key for the private use locale extension ('x').
     *
     * @see #getExtension(char)
     * @see Builder#setExtension(char, String)
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static final char PRIVATE_USE_EXTENSION = 'x';

    /**
     * {@icu} The key for Unicode locale extension ('u').
     *
     * @see #getExtension(char)
     * @see Builder#setExtension(char, String)
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static final char UNICODE_LOCALE_EXTENSION = 'u';

    /**
     * {@icu} Returns the extension (or private use) value associated with
     * the specified key, or null if there is no extension
     * associated with the key. To be well-formed, the key must be one
     * of <code>[0-9A-Za-z]</code>. Keys are case-insensitive, so
     * for example 'z' and 'Z' represent the same extension.
     *
     * @param key the extension key
     * @return The extension, or null if this locale defines no
     * extension for the specified key.
     * @throws IllegalArgumentException if key is not well-formed
     * @see #PRIVATE_USE_EXTENSION
     * @see #UNICODE_LOCALE_EXTENSION
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public String getExtension(char key) {
        if (!LocaleExtensions.isValidKey(key)) {
            throw new IllegalArgumentException("Invalid extension key: " + key);
        }
        return extensions().getExtensionValue(key);
    }

    /**
     * {@icu} Returns the set of extension keys associated with this locale, or the
     * empty set if it has no extensions. The returned set is unmodifiable.
     * The keys will all be lower-case.
     *
     * @return the set of extension keys, or the empty set if this locale has
     * no extensions
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public Set<Character> getExtensionKeys() {
        return extensions().getKeys();
    }

    /**
     * {@icu} Returns the set of unicode locale attributes associated with
     * this locale, or the empty set if it has no attributes. The
     * returned set is unmodifiable.
     *
     * @return The set of attributes.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public Set<String> getUnicodeLocaleAttributes() {
        return extensions().getUnicodeLocaleAttributes();
    }

    /**
     * {@icu} Returns the Unicode locale type associated with the specified Unicode locale key
     * for this locale. Returns the empty string for keys that are defined with no type.
     * Returns null if the key is not defined. Keys are case-insensitive. The key must
     * be two alphanumeric characters ([0-9a-zA-Z]), or an IllegalArgumentException is
     * thrown.
     *
     * @param key the Unicode locale key
     * @return The Unicode locale type associated with the key, or null if the
     * locale does not define the key.
     * @throws IllegalArgumentException if the key is not well-formed
     * @throws NullPointerException if <code>key</code> is null
     * 
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String getUnicodeLocaleType(String key) {
        if (!LocaleExtensions.isValidUnicodeLocaleKey(key)) {
            throw new IllegalArgumentException("Invalid Unicode locale key: " + key);
        }
        return extensions().getUnicodeLocaleType(key);
    }

    /**
     * {@icu} Returns the set of Unicode locale keys defined by this locale, or the empty set if
     * this locale has none.  The returned set is immutable.  Keys are all lower case.
     *
     * @return The set of Unicode locale keys, or the empty set if this locale has
     * no Unicode locale keywords.
     * 
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public Set<String> getUnicodeLocaleKeys() {
        return extensions().getUnicodeLocaleKeys();
    }

    /**
     * {@icu} Returns a well-formed IETF BCP 47 language tag representing
     * this locale.
     *
     * <p>If this <code>ULocale</code> has a language, script, country, or
     * variant that does not satisfy the IETF BCP 47 language tag
     * syntax requirements, this method handles these fields as
     * described below:
     *
     * <p><b>Language:</b> If language is empty, or not well-formed
     * (for example "a" or "e2"), it will be emitted as "und" (Undetermined).
     *
     * <p><b>Script:</b> If script is not well-formed (for example "12"
     * or "Latin"), it will be omitted.
     * 
     * <p><b>Country:</b> If country is not well-formed (for example "12"
     * or "USA"), it will be omitted.
     *
     * <p><b>Variant:</b> If variant <b>is</b> well-formed, each sub-segment
     * (delimited by '-' or '_') is emitted as a subtag.  Otherwise:
     * <ul>
     *
     * <li>if all sub-segments match <code>[0-9a-zA-Z]{1,8}</code>
     * (for example "WIN" or "Oracle_JDK_Standard_Edition"), the first
     * ill-formed sub-segment and all following will be appended to
     * the private use subtag.  The first appended subtag will be
     * "lvariant", followed by the sub-segments in order, separated by
     * hyphen. For example, "x-lvariant-WIN",
     * "Oracle-x-lvariant-JDK-Standard-Edition".
     *
     * <li>if any sub-segment does not match
     * <code>[0-9a-zA-Z]{1,8}</code>, the variant will be truncated
     * and the problematic sub-segment and all following sub-segments
     * will be omitted.  If the remainder is non-empty, it will be
     * emitted as a private use subtag as above (even if the remainder
     * turns out to be well-formed).  For example,
     * "Solaris_isjustthecoolestthing" is emitted as
     * "x-lvariant-Solaris", not as "solaris".</li></ul>
     *
     * <p><b>Note:</b> Although the language tag created by this
     * method is well-formed (satisfies the syntax requirements
     * defined by the IETF BCP 47 specification), it is not
     * necessarily a valid BCP 47 language tag.  For example,
     * <pre>
     *   new Locale("xx", "YY").toLanguageTag();</pre>
     * 
     * will return "xx-YY", but the language subtag "xx" and the
     * region subtag "YY" are invalid because they are not registered
     * in the IANA Language Subtag Registry.
     *
     * @return a BCP47 language tag representing the locale
     * @see #forLanguageTag(String)
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public String toLanguageTag() {
        BaseLocale base = base();
        LocaleExtensions exts = extensions();

        if (base.getVariant().equalsIgnoreCase("POSIX")) {
            // special handling for variant POSIX
            base = BaseLocale.getInstance(base.getLanguage(), base.getScript(), base.getRegion(), "");
            if (exts.getUnicodeLocaleType("va") == null) {
                // add va-posix
                InternalLocaleBuilder ilocbld = new InternalLocaleBuilder();
                try {
                    ilocbld.setLocale(BaseLocale.ROOT, exts);
                    ilocbld.setUnicodeLocaleKeyword("va", "posix");
                    exts = ilocbld.getLocaleExtensions();
                } catch (LocaleSyntaxException e) {
                    // this should not happen
                    throw new RuntimeException(e);
                }
            }
        }

        LanguageTag tag = LanguageTag.parseLocale(base, exts);

        StringBuilder buf = new StringBuilder();
        String subtag = tag.getLanguage();
        if (subtag.length() > 0) {
            buf.append(LanguageTag.canonicalizeLanguage(subtag));
        }
 
        subtag = tag.getScript();
        if (subtag.length() > 0) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeScript(subtag));
        }

        subtag = tag.getRegion();
        if (subtag.length() > 0) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeRegion(subtag));
        }

        List<String>subtags = tag.getVariants();
        for (String s : subtags) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeVariant(s));
        }

        subtags = tag.getExtensions();
        for (String s : subtags) {
            buf.append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizeExtension(s));
        }

        subtag = tag.getPrivateuse();
        if (subtag.length() > 0) {
            if (buf.length() > 0) {
                buf.append(LanguageTag.SEP);
            }
            buf.append(LanguageTag.PRIVATEUSE).append(LanguageTag.SEP);
            buf.append(LanguageTag.canonicalizePrivateuse(subtag));
        }

        return buf.toString();
    }

    /**
     * {@icu} Returns a locale for the specified IETF BCP 47 language tag string.
     *
     * <p>If the specified language tag contains any ill-formed subtags,
     * the first such subtag and all following subtags are ignored.  Compare
     * to {@link ULocale.Builder#setLanguageTag} which throws an exception
     * in this case.
     *
     * <p>The following <b>conversions</b> are performed:<ul>
     *
     * <li>The language code "und" is mapped to language "".
     *
     * <li>The portion of a private use subtag prefixed by "lvariant",
     * if any, is removed and appended to the variant field in the
     * result locale (without case normalization).  If it is then
     * empty, the private use subtag is discarded:
     *
     * <pre>
     *     ULocale loc;
     *     loc = ULocale.forLanguageTag("en-US-x-lvariant-icu4j);
     *     loc.getVariant(); // returns "ICU4J"
     *     loc.getExtension('x'); // returns null
     *
     *     loc = Locale.forLanguageTag("de-icu4j-x-URP-lvariant-Abc-Def");
     *     loc.getVariant(); // returns "ICU4J_ABC_DEF"
     *     loc.getExtension('x'); // returns "urp"
     * </pre>
     *
     * <li>When the languageTag argument contains an extlang subtag,
     * the first such subtag is used as the language, and the primary
     * language subtag and other extlang subtags are ignored:
     *
     * <pre>
     *     ULocale.forLanguageTag("ar-aao").getLanguage(); // returns "aao"
     *     ULocale.forLanguageTag("en-abc-def-us").toString(); // returns "abc_US"
     * </pre>
     *
     * <li>Case is normalized. Language is normalized to lower case,
     * script to title case, country to upper case, variant to upper case,
     * and extensions to lower case.
     *
     * <p>This implements the 'Language-Tag' production of BCP 47, and so
     * supports legacy language tags (marked as “Type: grandfathered” in BCP 47)
     * (regular and irregular) as well as private use language tags.
     *
     * <p>Stand-alone private use tags are represented as empty language and extension 'x-whatever',
     * and legacy tags are converted to their canonical replacements where they exist.
     *
     * <p>Note that a few legacy tags have no modern replacement;
     * these will be converted using the fallback described in
     * the first paragraph, so some information might be lost.
     *
     * <p><b>Note</b>: There is no guarantee that <code>toLanguageTag</code>
     * and <code>forLanguageTag</code> will round-trip.
     *
     * @param languageTag the language tag
     * @return The locale that best represents the language tag.
     * @throws NullPointerException if <code>languageTag</code> is <code>null</code>
     * @see #toLanguageTag()
     * @see ULocale.Builder#setLanguageTag(String)
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale forLanguageTag(String languageTag) {
        LanguageTag tag = LanguageTag.parse(languageTag, null);
        InternalLocaleBuilder bldr = new InternalLocaleBuilder();
        bldr.setLanguageTag(tag);
        return getInstance(bldr.getBaseLocale(), bldr.getLocaleExtensions());
    }


    /**
     * <code>Builder</code> is used to build instances of <code>ULocale</code>
     * from values configured by the setters.  Unlike the <code>ULocale</code>
     * constructors, the <code>Builder</code> checks if a value configured by a
     * setter satisfies the syntax requirements defined by the <code>ULocale</code>
     * class.  A <code>ULocale</code> object created by a <code>Builder</code> is
     * well-formed and can be transformed to a well-formed IETF BCP 47 language tag
     * without losing information.
     *
     * <p><b>Note:</b> The <code>ULocale</code> class does not provide any
     * syntactic restrictions on variant, while BCP 47 requires each variant
     * subtag to be 5 to 8 alphanumerics or a single numeric followed by 3
     * alphanumerics.  The method <code>setVariant</code> throws
     * <code>IllformedLocaleException</code> for a variant that does not satisfy
     * this restriction. If it is necessary to support such a variant, use a
     * ULocale constructor.  However, keep in mind that a <code>ULocale</code>
     * object created this way might lose the variant information when
     * transformed to a BCP 47 language tag.
     *
     * <p>The following example shows how to create a <code>Locale</code> object
     * with the <code>Builder</code>.
     * <blockquote>
     * <pre>
     *     ULocale aLocale = new Builder().setLanguage("sr").setScript("Latn").setRegion("RS").build();
     * </pre>
     * </blockquote>
     *
     * <p>Builders can be reused; <code>clear()</code> resets all
     * fields to their default values.
     *
     * @see ULocale#toLanguageTag()
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static final class Builder {

        private final InternalLocaleBuilder _locbld;

        /**
         * Constructs an empty Builder. The default value of all
         * fields, extensions, and private use information is the
         * empty string.
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder() {
            _locbld = new InternalLocaleBuilder();
        }

        /**
         * Resets the <code>Builder</code> to match the provided
         * <code>locale</code>.  Existing state is discarded.
         *
         * <p>All fields of the locale must be well-formed, see {@link Locale}.
         *
         * <p>Locales with any ill-formed fields cause
         * <code>IllformedLocaleException</code> to be thrown.
         *
         * @param locale the locale
         * @return This builder.
         * @throws IllformedLocaleException if <code>locale</code> has
         * any ill-formed fields.
         * @throws NullPointerException if <code>locale</code> is null.
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setLocale(ULocale locale) {
            try {
                _locbld.setLocale(locale.base(), locale.extensions());
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Resets the Builder to match the provided IETF BCP 47
         * language tag.  Discards the existing state.  Null and the
         * empty string cause the builder to be reset, like {@link
         * #clear}.  Legacy tags (see {@link
         * ULocale#forLanguageTag}) are converted to their canonical
         * form before being processed.  Otherwise, the language tag
         * must be well-formed (see {@link ULocale}) or an exception is
         * thrown (unlike <code>ULocale.forLanguageTag</code>, which
         * just discards ill-formed and following portions of the
         * tag).
         *
         * @param languageTag the language tag
         * @return This builder.
         * @throws IllformedLocaleException if <code>languageTag</code> is ill-formed
         * @see ULocale#forLanguageTag(String)
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setLanguageTag(String languageTag) {
            ParseStatus sts = new ParseStatus();
            LanguageTag tag = LanguageTag.parse(languageTag, sts);
            if (sts.isError()) {
                throw new IllformedLocaleException(sts.getErrorMessage(), sts.getErrorIndex());
            }
            _locbld.setLanguageTag(tag);

            return this;
        }

        /**
         * Sets the language.  If <code>language</code> is the empty string or
         * null, the language in this <code>Builder</code> is removed.  Otherwise,
         * the language must be <a href="./Locale.html#def_language">well-formed</a>
         * or an exception is thrown.
         *
         * <p>The typical language value is a two or three-letter language
         * code as defined in ISO639.
         *
         * @param language the language
         * @return This builder.
         * @throws IllformedLocaleException if <code>language</code> is ill-formed
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setLanguage(String language) {
            try {
                _locbld.setLanguage(language);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the script. If <code>script</code> is null or the empty string,
         * the script in this <code>Builder</code> is removed.
         * Otherwise, the script must be well-formed or an exception is thrown.
         *
         * <p>The typical script value is a four-letter script code as defined by ISO 15924.
         *
         * @param script the script
         * @return This builder.
         * @throws IllformedLocaleException if <code>script</code> is ill-formed
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setScript(String script) {
            try {
                _locbld.setScript(script);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the region.  If region is null or the empty string, the region
         * in this <code>Builder</code> is removed.  Otherwise,
         * the region must be well-formed or an exception is thrown.
         *
         * <p>The typical region value is a two-letter ISO 3166 code or a
         * three-digit UN M.49 area code.
         *
         * <p>The country value in the <code>Locale</code> created by the
         * <code>Builder</code> is always normalized to upper case.
         *
         * @param region the region
         * @return This builder.
         * @throws IllformedLocaleException if <code>region</code> is ill-formed
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setRegion(String region) {
            try {
                _locbld.setRegion(region);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the variant.  If variant is null or the empty string, the
         * variant in this <code>Builder</code> is removed.  Otherwise, it
         * must consist of one or more well-formed subtags, or an exception is thrown.
         *
         * <p><b>Note:</b> This method checks if <code>variant</code>
         * satisfies the IETF BCP 47 variant subtag's syntax requirements,
         * and normalizes the value to lowercase letters.  However,
         * the <code>ULocale</code> class does not impose any syntactic
         * restriction on variant.  To set such a variant,
         * use a ULocale constructor.
         *
         * @param variant the variant
         * @return This builder.
         * @throws IllformedLocaleException if <code>variant</code> is ill-formed
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setVariant(String variant) {
            try {
                _locbld.setVariant(variant);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the extension for the given key. If the value is null or the
         * empty string, the extension is removed.  Otherwise, the extension
         * must be well-formed or an exception is thrown.
         *
         * <p><b>Note:</b> The key {@link ULocale#UNICODE_LOCALE_EXTENSION
         * UNICODE_LOCALE_EXTENSION} ('u') is used for the Unicode locale extension.
         * Setting a value for this key replaces any existing Unicode locale key/type
         * pairs with those defined in the extension.
         *
         * <p><b>Note:</b> The key {@link ULocale#PRIVATE_USE_EXTENSION
         * PRIVATE_USE_EXTENSION} ('x') is used for the private use code. To be
         * well-formed, the value for this key needs only to have subtags of one to
         * eight alphanumeric characters, not two to eight as in the general case.
         *
         * @param key the extension key
         * @param value the extension value
         * @return This builder.
         * @throws IllformedLocaleException if <code>key</code> is illegal
         * or <code>value</code> is ill-formed
         * @see #setUnicodeLocaleKeyword(String, String)
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setExtension(char key, String value) {
            try {
                _locbld.setExtension(key, value);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Sets the Unicode locale keyword type for the given key.  If the type
         * is null, the Unicode keyword is removed.  Otherwise, the key must be
         * non-null and both key and type must be well-formed or an exception
         * is thrown.
         *
         * <p>Keys and types are converted to lower case.
         *
         * <p><b>Note</b>:Setting the 'u' extension via {@link #setExtension}
         * replaces all Unicode locale keywords with those defined in the
         * extension.
         *
         * @param key the Unicode locale key
         * @param type the Unicode locale type
         * @return This builder.
         * @throws IllformedLocaleException if <code>key</code> or <code>type</code>
         * is ill-formed
         * @throws NullPointerException if <code>key</code> is null
         * @see #setExtension(char, String)
         *
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setUnicodeLocaleKeyword(String key, String type) {
            try {
                _locbld.setUnicodeLocaleKeyword(key, type);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Adds a unicode locale attribute, if not already present, otherwise
         * has no effect.  The attribute must not be null and must be well-formed
         * or an exception is thrown.
         *
         * @param attribute the attribute
         * @return This builder.
         * @throws NullPointerException if <code>attribute</code> is null
         * @throws IllformedLocaleException if <code>attribute</code> is ill-formed
         * @see #setExtension(char, String)
         *
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public Builder addUnicodeLocaleAttribute(String attribute) {
            try {
                _locbld.addUnicodeLocaleAttribute(attribute);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Removes a unicode locale attribute, if present, otherwise has no
         * effect.  The attribute must not be null and must be well-formed
         * or an exception is thrown.
         *
         * <p>Attribute comparision for removal is case-insensitive.
         *
         * @param attribute the attribute
         * @return This builder.
         * @throws NullPointerException if <code>attribute</code> is null
         * @throws IllformedLocaleException if <code>attribute</code> is ill-formed
         * @see #setExtension(char, String)
         *
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public Builder removeUnicodeLocaleAttribute(String attribute) {
            try {
                _locbld.removeUnicodeLocaleAttribute(attribute);
            } catch (LocaleSyntaxException e) {
                throw new IllformedLocaleException(e.getMessage(), e.getErrorIndex());
            }
            return this;
        }

        /**
         * Resets the builder to its initial, empty state.
         *
         * @return this builder
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder clear() {
            _locbld.clear();
            return this;
        }

        /**
         * Resets the extensions to their initial, empty state.
         * Language, script, region and variant are unchanged.
         *
         * @return this builder
         * @see #setExtension(char, String)
         *
         * @draft ICU 4.2
         * @provisional This API might change or be removed in a future release.
         */
        public Builder clearExtensions() {
            _locbld.clearExtensions();
            return this;
        }

        /**
         * Returns an instance of <code>ULocale</code> created from the fields set
         * on this builder.
         *
         * @return a new Locale
         *
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        public ULocale build() {
            return getInstance(_locbld.getBaseLocale(), _locbld.getLocaleExtensions());
        }
    }

    private static ULocale getInstance(BaseLocale base, LocaleExtensions exts) {
        String id = lscvToID(base.getLanguage(), base.getScript(), base.getRegion(),
                base.getVariant());

        Set<Character> extKeys = exts.getKeys();
        if (!extKeys.isEmpty()) {
            // legacy locale ID assume Unicode locale keywords and
            // other extensions are at the same level.
            // e.g. @a=ext-for-aa;calendar=japanese;m=ext-for-mm;x=priv-use

            TreeMap<String, String> kwds = new TreeMap<String, String>();
            for (Character key : extKeys) {
                Extension ext = exts.getExtension(key);
                if (ext instanceof UnicodeLocaleExtension) {
                    UnicodeLocaleExtension uext = (UnicodeLocaleExtension)ext;
                    Set<String> ukeys = uext.getUnicodeLocaleKeys();
                    for (String bcpKey : ukeys) {
                        String bcpType = uext.getUnicodeLocaleType(bcpKey);
                        // convert to legacy key/type
                        String lkey = bcp47ToLDMLKey(bcpKey);
                        String ltype = bcp47ToLDMLType(lkey, ((bcpType.length() == 0) ? "yes" : bcpType)); // use "yes" as the value of typeless keywords
                        // special handling for u-va-posix, since this is a variant, not a keyword
                        if (lkey.equals("va") && ltype.equals("posix") && base.getVariant().length() == 0) {
                            id = id + "_POSIX";
                        } else {
                            kwds.put(lkey, ltype);
                        }
                    }
                    // Mapping Unicode locale attribute to the special keyword, attribute=xxx-yyy
                    Set<String> uattributes = uext.getUnicodeLocaleAttributes();
                    if (uattributes.size() > 0) {
                        StringBuilder attrbuf = new StringBuilder();
                        for (String attr : uattributes) {
                            if (attrbuf.length() > 0) {
                                attrbuf.append('-');
                            }
                            attrbuf.append(attr);
                        }
                        kwds.put(LOCALE_ATTRIBUTE_KEY, attrbuf.toString());
                    }
                } else {
                    kwds.put(String.valueOf(key), ext.getValue());
                }
            }

            if (!kwds.isEmpty()) {
                StringBuilder buf = new StringBuilder(id);
                buf.append("@");
                Set<Map.Entry<String, String>> kset = kwds.entrySet();
                boolean insertSep = false;
                for (Map.Entry<String, String> kwd : kset) {
                    if (insertSep) {
                        buf.append(";");
                    } else {
                        insertSep = true;
                    }
                    buf.append(kwd.getKey());
                    buf.append("=");
                    buf.append(kwd.getValue());
                }

                id = buf.toString();
            }
        }
        return new ULocale(id);
    }

    private BaseLocale base() {
        if (baseLocale == null) {
            String language = getLanguage();
            if (equals(ULocale.ROOT)) {
                language = "";
            }
            baseLocale = BaseLocale.getInstance(language, getScript(), getCountry(), getVariant());
        }
        return baseLocale;
    }

    private LocaleExtensions extensions() {
        if (extensions == null) {
            Iterator<String> kwitr = getKeywords();
            if (kwitr == null) {
                extensions = LocaleExtensions.EMPTY_EXTENSIONS;
            } else {
                InternalLocaleBuilder intbld = new InternalLocaleBuilder();
                while (kwitr.hasNext()) {
                    String key = kwitr.next();
                    if (key.equals(LOCALE_ATTRIBUTE_KEY)) {
                        // special keyword used for representing Unicode locale attributes
                        String[] uattributes = getKeywordValue(key).split("[-_]");
                        for (String uattr : uattributes) {
                            try {
                                intbld.addUnicodeLocaleAttribute(uattr);
                            } catch (LocaleSyntaxException e) {
                                // ignore and fall through
                            }
                        }
                    } else if (key.length() >= 2) {
                        String bcpKey = ldmlKeyToBCP47(key);
                        String bcpType = ldmlTypeToBCP47(key, getKeywordValue(key));
                        if (bcpKey != null && bcpType != null) {
                            try {
                                intbld.setUnicodeLocaleKeyword(bcpKey, bcpType);
                            } catch (LocaleSyntaxException e) {
                                // ignore and fall through
                            }
                        }
                    } else if (key.length() == 1 && (key.charAt(0) != UNICODE_LOCALE_EXTENSION)) {
                        try  {
                            intbld.setExtension(key.charAt(0), getKeywordValue(key).replace("_",
                                    LanguageTag.SEP));
                        } catch (LocaleSyntaxException e) {
                            // ignore and fall through
                        }
                    }
                }
                extensions = intbld.getLocaleExtensions();
            }
        }
        return extensions;
    }

    //
    // LDML legacy/BCP47 key and type mapping functions
    //
    private static String ldmlKeyToBCP47(String key) {
        //#com.ibm.icu.base
        // normalize key to lowercase
        key = AsciiUtil.toLowerString(key);
        String bcpKey = null;

        for (int i = 0; i < KEYMAP.length; i += 2) {
            if (key.equals(KEYMAP[i])) {
                bcpKey = KEYMAP[i + 1];
                break;
            }
        }

        if (bcpKey == null) {
            if (key.length() == 2 && LanguageTag.isExtensionSubtag(key)) {
                return key;
            }
            return null;
        }

        return bcpKey;
    }

    private static String bcp47ToLDMLKey(String bcpKey) {
        //#com.ibm.icu.base
        // normalize bcp key to lowercase
        bcpKey = AsciiUtil.toLowerString(bcpKey);
        String key = null;

        for (int i = 0; i < KEYMAP.length; i += 2) {
            if (bcpKey.equals(KEYMAP[i + 1])) {
                key = KEYMAP[i];
                break;
            }
        }

        if (key == null) {
            return bcpKey;
        }

        return key;
    }

    private static String ldmlTypeToBCP47(String key, String type) {
        //#com.ibm.icu.base

        // keys are case-insensitive, while types are case-sensitive
        key = AsciiUtil.toLowerString(key);
        String bcpType = null;
        String[] map = null;
        String[] aliasMap = null;

        if (key.equals("calendar")) {
            map = TYPEMAP_CALENDAR;
        } else if (key.equals("colalternate")) {
            map = TYPEMAP_COLALTERNATE;
        } else if (key.equals("colbackwards")) {
            map = TYPEMAP_COLBACKWARDS;
        } else if (key.equals("colcasefirst")) {
            map = TYPEMAP_COLCASEFIRST;
        } else if (key.equals("colcaselevel")) {
            map = TYPEMAP_COLCASELEVEL;
        } else if (key.equals("colhiraganaquaternary")) {
            map = TYPEMAP_COLHIRAGANAQUATERNARY;
        } else if (key.equals("collation")) {
            map = TYPEMAP_COLLATION;
        } else if (key.equals("colnormalization")) {
            map = TYPEMAP_COLNORMALIZATION;
        } else if (key.equals("colnumeric")) {
            map = TYPEMAP_COLNUMERIC;
        } else if (key.equals("colstrength")) {
            map = TYPEMAP_COLSTRENGTH;
            aliasMap = TYPEALIAS_COLSTRENGTH;
        } else if (key.equals("numbers")) {
            map = TYPEMAP_NUMBERS;
        } else if (key.equals("timezone")) {
            map = TYPEMAP_TIMEZONE;
            aliasMap = TYPEALIAS_TIMEZONE;
        }

        // LDML alias -> LDML canonical
        if (aliasMap != null) {
            for (int i = 0; i < aliasMap.length; i += 2) {
                if (type.equals(aliasMap[i])) {
                    type = aliasMap[i + 1];
                    break;
                }
            }
        }

        // LDML type -> BCP47 type
        if (map != null) {
            for (int i = 0; i < map.length; i += 2) {
                if (type.equals(map[i])) {
                    bcpType = map[i + 1];
                    break;
                }
            }
        }

        if (bcpType == null) {
            int typeLen = type.length();
            if (typeLen >= 3 && typeLen <= 8 && LanguageTag.isExtensionSubtag(type)) {
                return type;
            }
            return null;
        }
        return bcpType;
    }

    private static String bcp47ToLDMLType(String key, String bcpType) {
        //#com.ibm.icu.base

        // normalize key/bcpType to lowercase
        key = AsciiUtil.toLowerString(key);
        bcpType = AsciiUtil.toLowerString(bcpType);
        String type = null;
        String[] map = null;

        if (key.equals("calendar")) {
            map = TYPEMAP_CALENDAR;
        } else if (key.equals("colalternate")) {
            map = TYPEMAP_COLALTERNATE;
        } else if (key.equals("colbackwards")) {
            map = TYPEMAP_COLBACKWARDS;
        } else if (key.equals("colcasefirst")) {
            map = TYPEMAP_COLCASEFIRST;
        } else if (key.equals("colcaselevel")) {
            map = TYPEMAP_COLCASELEVEL;
        } else if (key.equals("colhiraganaquaternary")) {
            map = TYPEMAP_COLHIRAGANAQUATERNARY;
        } else if (key.equals("collation")) {
            map = TYPEMAP_COLLATION;
        } else if (key.equals("colnormalization")) {
            map = TYPEMAP_COLNORMALIZATION;
        } else if (key.equals("colnumeric")) {
            map = TYPEMAP_COLNUMERIC;
        } else if (key.equals("colstrength")) {
            map = TYPEMAP_COLSTRENGTH;
        } else if (key.equals("timezone")) {
            map = TYPEMAP_TIMEZONE;
        }

        if (map != null) {
            for (int i = 0; i < map.length; i += 2) {
                if (bcpType.equals(map[i + 1])) {
                    type = map[i];
                    break;
                }
            }
        }

        return (type != null) ? type : bcpType;
    }

    /*
     * JDK Locale Helper
     */
    private static final class JDKLocaleHelper {
        private static boolean isJava7orNewer = false;

        /*
         * New methods in Java 7 Locale class
         */
        private static Method mGetScript;
        private static Method mGetExtensionKeys;
        private static Method mGetExtension;
        private static Method mGetUnicodeLocaleKeys;
        private static Method mGetUnicodeLocaleAttributes;
        private static Method mGetUnicodeLocaleType;
        private static Method mForLanguageTag;

        private static Method mGetDefault;
        private static Method mSetDefault;
        private static Object eDISPLAY;
        private static Object eFORMAT;

        /*
         * This table is used for mapping between ICU and special Java
         * 6 locales.  When an ICU locale matches <minumum base> with
         * <keyword>/<value>, the ICU locale is mapped to <Java> locale.
         * For example, both ja_JP@calendar=japanese and ja@calendar=japanese
         * are mapped to Java locale "ja_JP_JP".  ICU locale "nn" is mapped
         * to Java locale "no_NO_NY".
         */
        private static final String[][] JAVA6_MAPDATA = {
        //  { <Java>,       <ICU base>, <keyword>,  <value>,    <minimum base>
            { "ja_JP_JP",   "ja_JP",    "calendar", "japanese", "ja"},
            { "no_NO_NY",   "nn_NO",    null,       null,       "nn"},
            { "th_TH_TH",   "th_TH",    "numbers",  "thai",     "th"},
        };

        static {
            do {
                try {
                    mGetScript = Locale.class.getMethod("getScript", (Class[]) null);
                    mGetExtensionKeys = Locale.class.getMethod("getExtensionKeys", (Class[]) null);
                    mGetExtension = Locale.class.getMethod("getExtension", char.class);
                    mGetUnicodeLocaleKeys = Locale.class.getMethod("getUnicodeLocaleKeys", (Class[]) null);
                    mGetUnicodeLocaleAttributes = Locale.class.getMethod("getUnicodeLocaleAttributes", (Class[]) null);
                    mGetUnicodeLocaleType = Locale.class.getMethod("getUnicodeLocaleType", String.class);
                    mForLanguageTag = Locale.class.getMethod("forLanguageTag", String.class);
    
                    Class<?> cCategory = null;
                    Class<?>[] classes = Locale.class.getDeclaredClasses();
                    for (Class<?> c : classes) {
                        if (c.getName().equals("java.util.Locale$Category")) {
                            cCategory = c;
                            break;
                        }
                    }
                    if (cCategory == null) {
                        break;
                    }
                    mGetDefault = Locale.class.getDeclaredMethod("getDefault", cCategory);
                    mSetDefault = Locale.class.getDeclaredMethod("setDefault", cCategory, Locale.class);
    
                    Method mName = cCategory.getMethod("name", (Class[]) null);
                    Object[] enumConstants = cCategory.getEnumConstants();
                    for (Object e : enumConstants) {
                        String catVal = (String)mName.invoke(e, (Object[])null);
                        if (catVal.equals("DISPLAY")) {
                            eDISPLAY = e;
                        } else if (catVal.equals("FORMAT")) {
                            eFORMAT = e;
                        }
                    }
                    if (eDISPLAY == null || eFORMAT == null) {
                        break;
                    }    
                    isJava7orNewer = true;
                } catch (NoSuchMethodException e) {
                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                } catch (SecurityException e) {
                    // TODO : report?
                }
            } while (false);
        }

        private JDKLocaleHelper() {
        }

        public static boolean isJava7orNewer() {
            return isJava7orNewer;
        }

        public static ULocale toULocale(Locale loc) {
            return isJava7orNewer ? toULocale7(loc) : toULocale6(loc);
        }

        public static Locale toLocale(ULocale uloc) {
            return isJava7orNewer ? toLocale7(uloc) : toLocale6(uloc);
        }

        private static ULocale toULocale7(Locale loc) {
            String language = loc.getLanguage();
            String script = "";
            String country = loc.getCountry();
            String variant = loc.getVariant();

            Set<String> attributes = null;
            Map<String, String> keywords = null;

            try {
                script = (String) mGetScript.invoke(loc, (Object[]) null);
                @SuppressWarnings("unchecked")
                Set<Character> extKeys = (Set<Character>) mGetExtensionKeys.invoke(loc, (Object[]) null);
                if (!extKeys.isEmpty()) {
                    for (Character extKey : extKeys) {
                        if (extKey.charValue() == 'u') {
                            // Found Unicode locale extension

                            // attributes
                            @SuppressWarnings("unchecked")
                            Set<String> uAttributes = (Set<String>) mGetUnicodeLocaleAttributes.invoke(loc, (Object[]) null);
                            if (!uAttributes.isEmpty()) {
                                attributes = new TreeSet<String>();
                                for (String attr : uAttributes) {
                                    attributes.add(attr);
                                }
                            }

                            // keywords
                            @SuppressWarnings("unchecked")
                            Set<String> uKeys = (Set<String>) mGetUnicodeLocaleKeys.invoke(loc, (Object[]) null);
                            for (String kwKey : uKeys) {
                                String kwVal = (String) mGetUnicodeLocaleType.invoke(loc, kwKey);
                                if (kwVal != null) {
                                    if (kwKey.equals("va")) {
                                        // va-* is interpreted as a variant
                                        variant = (variant.length() == 0) ? kwVal : kwVal + "_" + variant;
                                    } else {
                                        if (keywords == null) {
                                            keywords = new TreeMap<String, String>();
                                        }
                                        keywords.put(kwKey, kwVal);
                                    }
                                }
                            }
                        } else {
                            String extVal = (String) mGetExtension.invoke(loc, extKey);
                            if (extVal != null) {
                                if (keywords == null) {
                                    keywords = new TreeMap<String, String>();
                                }
                                keywords.put(String.valueOf(extKey), extVal);
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            // JDK locale no_NO_NY is not interpreted as Nynorsk by ICU,
            // and it should be transformed to nn_NO.

            // Note: JDK7+ unerstand both no_NO_NY and nn_NO. When convert
            // ICU locale to JDK, we do not need to map nn_NO back to no_NO_NY.

            if (language.equals("no") && country.equals("NO") && variant.equals("NY")) {
                language = "nn";
                variant = "";
            }

            // Constructing ID
            StringBuilder buf = new StringBuilder(language);

            if (script.length() > 0) {
                buf.append('_');
                buf.append(script);
            }

            if (country.length() > 0) {
                buf.append('_');
                buf.append(country);
            }

            if (variant.length() > 0) {
                if (country.length() == 0) {
                    buf.append('_');
                }
                buf.append('_');
                buf.append(variant);
            }

            if (attributes != null) {
                // transform Unicode attributes into a keyword
                StringBuilder attrBuf = new StringBuilder();
                for (String attr : attributes) {
                    if (attrBuf.length() != 0) {
                        attrBuf.append('-');
                    }
                    attrBuf.append(attr);
                }
                if (keywords == null) {
                    keywords = new TreeMap<String, String>();
                }
                keywords.put(LOCALE_ATTRIBUTE_KEY, attrBuf.toString());
            }

            if (keywords != null) {
                buf.append('@');
                boolean addSep = false;
                for (Entry<String, String> kwEntry : keywords.entrySet()) {
                    String kwKey = kwEntry.getKey();
                    String kwVal = kwEntry.getValue();

                    if (kwKey.length() != 1) {
                        // Unicode locale key
                        kwKey = bcp47ToLDMLKey(kwKey);
                        // use "yes" as the value of typeless keywords
                        kwVal = bcp47ToLDMLType(kwKey, ((kwVal.length() == 0) ? "yes" : kwVal));
                    }

                    if (addSep) {
                        buf.append(';');
                    } else {
                        addSep = true;
                    }
                    buf.append(kwKey);
                    buf.append('=');
                    buf.append(kwVal);
                }
            }

            return new ULocale(getName(buf.toString()), loc);
        }

        private static ULocale toULocale6(Locale loc) {
            ULocale uloc = null;
            String locStr = loc.toString();
            if (locStr.length() == 0) {
                uloc = ULocale.ROOT;
            } else {
                for (int i = 0; i < JAVA6_MAPDATA.length; i++) {
                    if (JAVA6_MAPDATA[i][0].equals(locStr)) {
                        LocaleIDParser p = new LocaleIDParser(JAVA6_MAPDATA[i][1]);
                        p.setKeywordValue(JAVA6_MAPDATA[i][2], JAVA6_MAPDATA[i][3]);
                        locStr = p.getName();
                        break;
                    }
                }
                uloc = new ULocale(getName(locStr), loc);
            }
            return uloc;
        }

        private static Locale toLocale7(ULocale uloc) {
            Locale loc = null;
            String ulocStr = uloc.getName();
            if (uloc.getScript().length() > 0 || ulocStr.contains("@")) {
                // With script or keywords available, the best way
                // to get a mapped Locale is to go through a language tag.
                // A Locale with script or keywords can only have variants
                // that is 1 to 8 alphanum. If this ULocale has a variant
                // subtag not satisfying the criteria, the variant subtag
                // will be lost.
                String tag = uloc.toLanguageTag();

                // Workaround for variant casing problem:
                //
                // The variant field in ICU is case insensitive and normalized
                // to upper case letters by getVariant(), while
                // the variant field in JDK Locale is case sensitive.
                // ULocale#toLanguageTag use lower case characters for
                // BCP 47 variant and private use x-lvariant.
                //
                // Locale#forLanguageTag in JDK preserves character casing
                // for variant. Because ICU always normalizes variant to
                // upper case, we convert language tag to upper case here.
                tag = AsciiUtil.toUpperString(tag);

                try {
                    loc = (Locale)mForLanguageTag.invoke(null, tag);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
            if (loc == null) {
                // Without script or keywords, use a Locale constructor,
                // so we can preserve any ill-formed variants.
                loc = new Locale(uloc.getLanguage(), uloc.getCountry(), uloc.getVariant());
            }
            return loc;
        }

        private static Locale toLocale6(ULocale uloc) {
            String locstr = uloc.getBaseName();
            for (int i = 0; i < JAVA6_MAPDATA.length; i++) {
                if (locstr.equals(JAVA6_MAPDATA[i][1]) || locstr.equals(JAVA6_MAPDATA[i][4])) {
                    if (JAVA6_MAPDATA[i][2] != null) {
                        String val = uloc.getKeywordValue(JAVA6_MAPDATA[i][2]);
                        if (val != null && val.equals(JAVA6_MAPDATA[i][3])) {
                            locstr = JAVA6_MAPDATA[i][0];
                            break;
                        }
                    } else {
                        locstr = JAVA6_MAPDATA[i][0];
                        break;
                    }
                }
            }
            LocaleIDParser p = new LocaleIDParser(locstr);
            String[] names = p.getLanguageScriptCountryVariant();
            return new Locale(names[0], names[2], names[3]);
        }

        public static Locale getDefault(Category category) {
            Locale loc = Locale.getDefault();
            if (isJava7orNewer) {
                Object cat = null;
                switch (category) {
                case DISPLAY:
                    cat = eDISPLAY;
                    break;
                case FORMAT:
                    cat = eFORMAT;
                    break;
                }
                if (cat != null) {
                    try {
                        loc = (Locale)mGetDefault.invoke(null, cat);
                    } catch (InvocationTargetException e) {
                        // fall through - use the base default
                    } catch (IllegalArgumentException e) {
                        // fall through - use the base default
                    } catch (IllegalAccessException e) {
                        // fall through - use the base default
                    }
                }
            }
            return loc;
        }

        public static void setDefault(Category category, Locale newLocale) {
            if (isJava7orNewer) {
                Object cat = null;
                switch (category) {
                case DISPLAY:
                    cat = eDISPLAY;
                    break;
                case FORMAT:
                    cat = eFORMAT;
                    break;
                }
                if (cat != null) {
                    try {
                        mSetDefault.invoke(null, cat, newLocale);
                    } catch (InvocationTargetException e) {
                        // fall through - no effects
                    } catch (IllegalArgumentException e) {
                        // fall through - no effects
                    } catch (IllegalAccessException e) {
                        // fall through - no effects
                    }
                }
            }
        }
    }

    private static final String[] KEYMAP = {
        "calendar", "ca",
        "colalternate", "ka",
        "colbackwards", "kb",
        "colcasefirst", "kf",
        "colcaselevel", "kc",
        "colhiraganaquaternary", "kh",
        "collation", "co",
        "colnormalization", "kk",
        "colnumeric", "kn",
        "colstrength", "ks",
        "currency", "cu",
        "numbers", "nu",
        "timezone", "tz",
        "variabletop", "vt",
    };

    private static final String[] TYPEMAP_CALENDAR = {
        "ethiopic-amete-alem", "ethioaa",
        "gregorian", "gregory",
        "islamic-civil", "islamicc",
    };

    private static final String[] TYPEMAP_COLALTERNATE = {
        "non-ignorable", "noignore",
    };

    private static final String[] TYPEMAP_COLBACKWARDS = {
        "no", "false",
        "yes", "true",
    };

    private static final String[] TYPEMAP_COLCASEFIRST = {
        "no", "false",
    };

    private static final String[] TYPEMAP_COLCASELEVEL = {
        "no", "false",
        "yes", "true",
    };

    private static final String[] TYPEMAP_COLHIRAGANAQUATERNARY = {
        "no", "false",
        "yes", "true",
    };

    private static final String[] TYPEMAP_COLLATION = {
        "dictionary", "dict",
        "gb2312han", "gb2312",
        "phonebook", "phonebk",
        "traditional", "trad",
    };

    private static final String[] TYPEMAP_COLNORMALIZATION = {
        "no", "false",
        "yes", "true",
    };

    private static final String[] TYPEMAP_COLNUMERIC = {
        "no", "false",
        "yes", "true",
    };

    private static final String[] TYPEMAP_COLSTRENGTH = {
        "identical", "identic",
        "primary", "level1",
        "quaternary", "level4",
        "secondary", "level2",
        "tertiary", "level3",
    };

    private static final String[] TYPEMAP_NUMBERS = {
        "traditional", "traditio",
    };

    private static final String[] TYPEMAP_TIMEZONE = {
        "Africa/Abidjan", "ciabj",
        "Africa/Accra", "ghacc",
        "Africa/Addis_Ababa", "etadd",
        "Africa/Algiers", "dzalg",
        "Africa/Asmera", "erasm",
        "Africa/Bamako", "mlbko",
        "Africa/Bangui", "cfbgf",
        "Africa/Banjul", "gmbjl",
        "Africa/Bissau", "gwoxb",
        "Africa/Blantyre", "mwblz",
        "Africa/Brazzaville", "cgbzv",
        "Africa/Bujumbura", "bibjm",
        "Africa/Cairo", "egcai",
        "Africa/Casablanca", "macas",
        "Africa/Ceuta", "esceu",
        "Africa/Conakry", "gncky",
        "Africa/Dakar", "sndkr",
        "Africa/Dar_es_Salaam", "tzdar",
        "Africa/Djibouti", "djjib",
        "Africa/Douala", "cmdla",
        "Africa/El_Aaiun", "eheai",
        "Africa/Freetown", "slfna",
        "Africa/Gaborone", "bwgbe",
        "Africa/Harare", "zwhre",
        "Africa/Johannesburg", "zajnb",
        "Africa/Juba", "ssjub",
        "Africa/Kampala", "ugkla",
        "Africa/Khartoum", "sdkrt",
        "Africa/Kigali", "rwkgl",
        "Africa/Kinshasa", "cdfih",
        "Africa/Lagos", "nglos",
        "Africa/Libreville", "galbv",
        "Africa/Lome", "tglfw",
        "Africa/Luanda", "aolad",
        "Africa/Lubumbashi", "cdfbm",
        "Africa/Lusaka", "zmlun",
        "Africa/Malabo", "gqssg",
        "Africa/Maputo", "mzmpm",
        "Africa/Maseru", "lsmsu",
        "Africa/Mbabane", "szqmn",
        "Africa/Mogadishu", "somgq",
        "Africa/Monrovia", "lrmlw",
        "Africa/Nairobi", "kenbo",
        "Africa/Ndjamena", "tdndj",
        "Africa/Niamey", "nenim",
        "Africa/Nouakchott", "mrnkc",
        "Africa/Ouagadougou", "bfoua",
        "Africa/Porto-Novo", "bjptn",
        "Africa/Sao_Tome", "sttms",
        "Africa/Tripoli", "lytip",
        "Africa/Tunis", "tntun",
        "Africa/Windhoek", "nawdh",
        "America/Adak", "usadk",
        "America/Anchorage", "usanc",
        "America/Anguilla", "aiaxa",
        "America/Antigua", "aganu",
        "America/Araguaina", "braux",
        "America/Argentina/La_Rioja", "arirj",
        "America/Argentina/Rio_Gallegos", "arrgl",
        "America/Argentina/Salta", "arsla",
        "America/Argentina/San_Juan", "aruaq",
        "America/Argentina/San_Luis", "arluq",
        "America/Argentina/Tucuman", "artuc",
        "America/Argentina/Ushuaia", "arush",
        "America/Aruba", "awaua",
        "America/Asuncion", "pyasu",
        "America/Bahia", "brssa",
        "America/Bahia_Banderas", "mxpvr",
        "America/Barbados", "bbbgi",
        "America/Belem", "brbel",
        "America/Belize", "bzbze",
        "America/Blanc-Sablon", "caybx",
        "America/Boa_Vista", "brbvb",
        "America/Bogota", "cobog",
        "America/Boise", "usboi",
        "America/Buenos_Aires", "arbue",
        "America/Cambridge_Bay", "caycb",
        "America/Campo_Grande", "brcgr",
        "America/Cancun", "mxcun",
        "America/Caracas", "veccs",
        "America/Catamarca", "arctc",
        "America/Cayenne", "gfcay",
        "America/Cayman", "kygec",
        "America/Chicago", "uschi",
        "America/Chihuahua", "mxchi",
        "America/Coral_Harbour", "cayzs",
        "America/Cordoba", "arcor",
        "America/Costa_Rica", "crsjo",
        "America/Creston", "cacfq",
        "America/Cuiaba", "brcgb",
        "America/Curacao", "ancur",
        "America/Danmarkshavn", "gldkshvn",
        "America/Dawson", "cayda",
        "America/Dawson_Creek", "caydq",
        "America/Denver", "usden",
        "America/Detroit", "usdet",
        "America/Dominica", "dmdom",
        "America/Edmonton", "caedm",
        "America/Eirunepe", "brern",
        "America/El_Salvador", "svsal",
        "America/Fortaleza", "brfor",
        "America/Glace_Bay", "caglb",
        "America/Godthab", "glgoh",
        "America/Goose_Bay", "cagoo",
        "America/Grand_Turk", "tcgdt",
        "America/Grenada", "gdgnd",
        "America/Guadeloupe", "gpbbr",
        "America/Guatemala", "gtgua",
        "America/Guayaquil", "ecgye",
        "America/Guyana", "gygeo",
        "America/Halifax", "cahal",
        "America/Havana", "cuhav",
        "America/Hermosillo", "mxhmo",
        "America/Indiana/Knox", "usknx",
        "America/Indiana/Marengo", "usaeg",
        "America/Indiana/Petersburg", "uswsq",
        "America/Indiana/Tell_City", "ustel",
        "America/Indiana/Vevay", "usinvev",
        "America/Indiana/Vincennes", "usoea",
        "America/Indiana/Winamac", "uswlz",
        "America/Indianapolis", "usind",
        "America/Inuvik", "cayev",
        "America/Iqaluit", "caiql",
        "America/Jamaica", "jmkin",
        "America/Jujuy", "arjuj",
        "America/Juneau", "usjnu",
        "America/Kentucky/Monticello", "usmoc",
        "America/Kralendijk", "bqkra",
        "America/La_Paz", "bolpb",
        "America/Lima", "pelim",
        "America/Los_Angeles", "uslax",
        "America/Louisville", "uslui",
        "America/Lower_Princes", "sxphi",
        "America/Maceio", "brmcz",
        "America/Managua", "nimga",
        "America/Manaus", "brmao",
        "America/Marigot", "gpmsb",
        "America/Martinique", "mqfdf",
        "America/Matamoros", "mxmam",
        "America/Mazatlan", "mxmzt",
        "America/Mendoza", "armdz",
        "America/Menominee", "usmnm",
        "America/Merida", "mxmid",
        "America/Metlakatla", "usmtm",
        "America/Mexico_City", "mxmex",
        "America/Miquelon", "pmmqc",
        "America/Moncton", "camon",
        "America/Monterrey", "mxmty",
        "America/Montevideo", "uymvd",
        "America/Montreal", "camtr",
        "America/Montserrat", "msmni",
        "America/Nassau", "bsnas",
        "America/New_York", "usnyc",
        "America/Nipigon", "canpg",
        "America/Nome", "usome",
        "America/Noronha", "brfen",
        "America/North_Dakota/Beulah", "usxul",
        "America/North_Dakota/Center", "usndcnt",
        "America/North_Dakota/New_Salem", "usndnsl",
        "America/Ojinaga", "mxoji",
        "America/Panama", "papty",
        "America/Pangnirtung", "capnt",
        "America/Paramaribo", "srpbm",
        "America/Phoenix", "usphx",
        "America/Port_of_Spain", "ttpos",
        "America/Port-au-Prince", "htpap",
        "America/Porto_Velho", "brpvh",
        "America/Puerto_Rico", "prsju",
        "America/Rainy_River", "caffs",
        "America/Rankin_Inlet", "cayek",
        "America/Recife", "brrec",
        "America/Regina", "careg",
        "America/Resolute", "careb",
        "America/Rio_Branco", "brrbr",
        "America/Santa_Isabel", "mxstis",
        "America/Santarem", "brstm",
        "America/Santiago", "clscl",
        "America/Santo_Domingo", "dosdq",
        "America/Sao_Paulo", "brsao",
        "America/Scoresbysund", "globy",
        "America/Shiprock", "usnavajo",
        "America/Sitka", "ussit",
        "America/St_Barthelemy", "gpsbh",
        "America/St_Johns", "casjf",
        "America/St_Kitts", "knbas",
        "America/St_Lucia", "lccas",
        "America/St_Thomas", "vistt",
        "America/St_Vincent", "vcsvd",
        "America/Swift_Current", "cayyn",
        "America/Tegucigalpa", "hntgu",
        "America/Thule", "glthu",
        "America/Thunder_Bay", "cathu",
        "America/Tijuana", "mxtij",
        "America/Toronto", "cator",
        "America/Tortola", "vgtov",
        "America/Vancouver", "cavan",
        "America/Whitehorse", "cayxy",
        "America/Winnipeg", "cawnp",
        "America/Yakutat", "usyak",
        "America/Yellowknife", "cayzf",
        "Antarctica/Casey", "aqcas",
        "Antarctica/Davis", "aqdav",
        "Antarctica/DumontDUrville", "aqddu",
        "Antarctica/Macquarie", "aumqi",
        "Antarctica/Mawson", "aqmaw",
        "Antarctica/McMurdo", "aqmcm",
        "Antarctica/Palmer", "aqplm",
        "Antarctica/Rothera", "aqrot",
        "Antarctica/South_Pole", "aqams",
        "Antarctica/Syowa", "aqsyw",
        "Antarctica/Vostok", "aqvos",
        "Arctic/Longyearbyen", "sjlyr",
        "Asia/Aden", "yeade",
        "Asia/Almaty", "kzala",
        "Asia/Amman", "joamm",
        "Asia/Anadyr", "rudyr",
        "Asia/Aqtau", "kzaau",
        "Asia/Aqtobe", "kzakx",
        "Asia/Ashgabat", "tmasb",
        "Asia/Baghdad", "iqbgw",
        "Asia/Bahrain", "bhbah",
        "Asia/Baku", "azbak",
        "Asia/Bangkok", "thbkk",
        "Asia/Beirut", "lbbey",
        "Asia/Bishkek", "kgfru",
        "Asia/Brunei", "bnbwn",
        "Asia/Calcutta", "inccu",
        "Asia/Choibalsan", "mncoq",
        "Asia/Chongqing", "cnckg",
        "Asia/Colombo", "lkcmb",
        "Asia/Damascus", "sydam",
        "Asia/Dhaka", "bddac",
        "Asia/Dili", "tldil",
        "Asia/Dubai", "aedxb",
        "Asia/Dushanbe", "tjdyu",
        "Asia/Gaza", "gaza",
        "Asia/Harbin", "cnhrb",
        "Asia/Hebron", "hebron",
        "Asia/Hong_Kong", "hkhkg",
        "Asia/Hovd", "mnhvd",
        "Asia/Irkutsk", "ruikt",
        "Asia/Jakarta", "idjkt",
        "Asia/Jayapura", "iddjj",
        "Asia/Jerusalem", "jeruslm",
        "Asia/Kabul", "afkbl",
        "Asia/Kamchatka", "rupkc",
        "Asia/Karachi", "pkkhi",
        "Asia/Kashgar", "cnkhg",
        "Asia/Katmandu", "npktm",
        "Asia/Krasnoyarsk", "rukra",
        "Asia/Kuala_Lumpur", "mykul",
        "Asia/Kuching", "mykch",
        "Asia/Kuwait", "kwkwi",
        "Asia/Macau", "momfm",
        "Asia/Magadan", "rugdx",
        "Asia/Makassar", "idmak",
        "Asia/Manila", "phmnl",
        "Asia/Muscat", "ommct",
        "Asia/Nicosia", "cynic",
        "Asia/Novokuznetsk", "runoz",
        "Asia/Novosibirsk", "ruovb",
        "Asia/Omsk", "ruoms",
        "Asia/Oral", "kzura",
        "Asia/Phnom_Penh", "khpnh",
        "Asia/Pontianak", "idpnk",
        "Asia/Pyongyang", "kpfnj",
        "Asia/Qatar", "qadoh",
        "Asia/Qyzylorda", "kzkzo",
        "Asia/Rangoon", "mmrgn",
        "Asia/Riyadh", "saruh",
        "Asia/Saigon", "vnsgn",
        "Asia/Sakhalin", "ruuus",
        "Asia/Samarkand", "uzskd",
        "Asia/Seoul", "krsel",
        "Asia/Shanghai", "cnsha",
        "Asia/Singapore", "sgsin",
        "Asia/Taipei", "twtpe",
        "Asia/Tashkent", "uztas",
        "Asia/Tbilisi", "getbs",
        "Asia/Tehran", "irthr",
        "Asia/Thimphu", "btthi",
        "Asia/Tokyo", "jptyo",
        "Asia/Ulaanbaatar", "mnuln",
        "Asia/Urumqi", "cnurc",
        "Asia/Vientiane", "lavte",
        "Asia/Vladivostok", "ruvvo",
        "Asia/Yakutsk", "ruyks",
        "Asia/Yekaterinburg", "ruyek",
        "Asia/Yerevan", "amevn",
        "Atlantic/Azores", "ptpdl",
        "Atlantic/Bermuda", "bmbda",
        "Atlantic/Canary", "eslpa",
        "Atlantic/Cape_Verde", "cvrai",
        "Atlantic/Faeroe", "fotho",
        "Atlantic/Madeira", "ptfnc",
        "Atlantic/Reykjavik", "isrey",
        "Atlantic/South_Georgia", "gsgrv",
        "Atlantic/St_Helena", "shshn",
        "Atlantic/Stanley", "fkpsy",
        "Australia/Adelaide", "auadl",
        "Australia/Brisbane", "aubne",
        "Australia/Broken_Hill", "aubhq",
        "Australia/Currie", "aukns",
        "Australia/Darwin", "audrw",
        "Australia/Eucla", "aueuc",
        "Australia/Hobart", "auhba",
        "Australia/Lindeman", "auldc",
        "Australia/Lord_Howe", "auldh",
        "Australia/Melbourne", "aumel",
        "Australia/Perth", "auper",
        "Australia/Sydney", "ausyd",
        "CST6CDT", "cst6cdt",
        "EST5EDT", "est5edt",
        "Etc/GMT", "utc",
        "Etc/GMT+1", "utcw01",
        "Etc/GMT+10", "utcw10",
        "Etc/GMT+11", "utcw11",
        "Etc/GMT+12", "utcw12",
        "Etc/GMT+2", "utcw02",
        "Etc/GMT+3", "utcw03",
        "Etc/GMT+4", "utcw04",
        "Etc/GMT+5", "utcw05",
        "Etc/GMT+6", "utcw06",
        "Etc/GMT+7", "utcw07",
        "Etc/GMT+8", "utcw08",
        "Etc/GMT+9", "utcw09",
        "Etc/GMT-1", "utce01",
        "Etc/GMT-10", "utce10",
        "Etc/GMT-11", "utce11",
        "Etc/GMT-12", "utce12",
        "Etc/GMT-13", "utce13",
        "Etc/GMT-14", "utce14",
        "Etc/GMT-2", "utce02",
        "Etc/GMT-3", "utce03",
        "Etc/GMT-4", "utce04",
        "Etc/GMT-5", "utce05",
        "Etc/GMT-6", "utce06",
        "Etc/GMT-7", "utce07",
        "Etc/GMT-8", "utce08",
        "Etc/GMT-9", "utce09",
        "Etc/Unknown", "unk",
        "Europe/Amsterdam", "nlams",
        "Europe/Andorra", "adalv",
        "Europe/Athens", "grath",
        "Europe/Belgrade", "rsbeg",
        "Europe/Berlin", "deber",
        "Europe/Bratislava", "skbts",
        "Europe/Brussels", "bebru",
        "Europe/Bucharest", "robuh",
        "Europe/Budapest", "hubud",
        "Europe/Chisinau", "mdkiv",
        "Europe/Copenhagen", "dkcph",
        "Europe/Dublin", "iedub",
        "Europe/Gibraltar", "gigib",
        "Europe/Guernsey", "gggci",
        "Europe/Helsinki", "fihel",
        "Europe/Isle_of_Man", "imdgs",
        "Europe/Istanbul", "trist",
        "Europe/Jersey", "jesth",
        "Europe/Kaliningrad", "rukgd",
        "Europe/Kiev", "uaiev",
        "Europe/Lisbon", "ptlis",
        "Europe/Ljubljana", "silju",
        "Europe/London", "gblon",
        "Europe/Luxembourg", "lulux",
        "Europe/Madrid", "esmad",
        "Europe/Malta", "mtmla",
        "Europe/Mariehamn", "fimhq",
        "Europe/Minsk", "bymsq",
        "Europe/Monaco", "mcmon",
        "Europe/Moscow", "rumow",
        "Europe/Oslo", "noosl",
        "Europe/Paris", "frpar",
        "Europe/Podgorica", "metgd",
        "Europe/Prague", "czprg",
        "Europe/Riga", "lvrix",
        "Europe/Rome", "itrom",
        "Europe/Samara", "rukuf",
        "Europe/San_Marino", "smsai",
        "Europe/Sarajevo", "basjj",
        "Europe/Simferopol", "uasip",
        "Europe/Skopje", "mkskp",
        "Europe/Sofia", "bgsof",
        "Europe/Stockholm", "sesto",
        "Europe/Tallinn", "eetll",
        "Europe/Tirane", "altia",
        "Europe/Uzhgorod", "uauzh",
        "Europe/Vaduz", "livdz",
        "Europe/Vatican", "vavat",
        "Europe/Vienna", "atvie",
        "Europe/Vilnius", "ltvno",
        "Europe/Volgograd", "ruvog",
        "Europe/Warsaw", "plwaw",
        "Europe/Zagreb", "hrzag",
        "Europe/Zaporozhye", "uaozh",
        "Europe/Zurich", "chzrh",
        "Indian/Antananarivo", "mgtnr",
        "Indian/Chagos", "iodga",
        "Indian/Christmas", "cxxch",
        "Indian/Cocos", "cccck",
        "Indian/Comoro", "kmyva",
        "Indian/Kerguelen", "tfpfr",
        "Indian/Mahe", "scmaw",
        "Indian/Maldives", "mvmle",
        "Indian/Mauritius", "muplu",
        "Indian/Mayotte", "ytmam",
        "Indian/Reunion", "rereu",
        "MST7MDT", "mst7mdt",
        "Pacific/Apia", "wsapw",
        "Pacific/Auckland", "nzakl",
        "Pacific/Chatham", "nzcht",
        "Pacific/Easter", "clipc",
        "Pacific/Efate", "vuvli",
        "Pacific/Enderbury", "kipho",
        "Pacific/Fakaofo", "tkfko",
        "Pacific/Fiji", "fjsuv",
        "Pacific/Funafuti", "tvfun",
        "Pacific/Galapagos", "ecgps",
        "Pacific/Gambier", "pfgmr",
        "Pacific/Guadalcanal", "sbhir",
        "Pacific/Guam", "gugum",
        "Pacific/Honolulu", "ushnl",
        "Pacific/Johnston", "umjon",
        "Pacific/Kiritimati", "kicxi",
        "Pacific/Kosrae", "fmksa",
        "Pacific/Kwajalein", "mhkwa",
        "Pacific/Majuro", "mhmaj",
        "Pacific/Marquesas", "pfnhv",
        "Pacific/Midway", "ummdy",
        "Pacific/Nauru", "nrinu",
        "Pacific/Niue", "nuiue",
        "Pacific/Norfolk", "nfnlk",
        "Pacific/Noumea", "ncnou",
        "Pacific/Pago_Pago", "asppg",
        "Pacific/Palau", "pwror",
        "Pacific/Pitcairn", "pnpcn",
        "Pacific/Ponape", "fmpni",
        "Pacific/Port_Moresby", "pgpom",
        "Pacific/Rarotonga", "ckrar",
        "Pacific/Saipan", "mpspn",
        "Pacific/Tahiti", "pfppt",
        "Pacific/Tarawa", "kitrw",
        "Pacific/Tongatapu", "totbu",
        "Pacific/Truk", "fmtkk",
        "Pacific/Wake", "umawk",
        "Pacific/Wallis", "wfmau",
        "PST8PDT", "pst8pdt",
    };

    private static final String[] TYPEALIAS_COLSTRENGTH = {
        "quarternary", "quaternary",
    };

    private static final String[] TYPEALIAS_TIMEZONE = {
        "Africa/Asmara", "Africa/Asmera",
        "Africa/Timbuktu", "Africa/Bamako",
        "America/Argentina/Buenos_Aires", "America/Buenos_Aires",
        "America/Argentina/Catamarca", "America/Catamarca",
        "America/Argentina/ComodRivadavia", "America/Catamarca",
        "America/Argentina/Cordoba", "America/Cordoba",
        "America/Argentina/Jujuy", "America/Jujuy",
        "America/Argentina/Mendoza", "America/Mendoza",
        "America/Atikokan", "America/Coral_Harbour",
        "America/Atka", "America/Adak",
        "America/Ensenada", "America/Tijuana",
        "America/Fort_Wayne", "America/Indianapolis",
        "America/Indiana/Indianapolis", "America/Indianapolis",
        "America/Kentucky/Louisville", "America/Louisville",
        "America/Knox_IN", "America/Indiana/Knox",
        "America/Porto_Acre", "America/Rio_Branco",
        "America/Rosario", "America/Cordoba",
        "America/Virgin", "America/St_Thomas",
        "Asia/Ashkhabad", "Asia/Ashgabat",
        "Asia/Chungking", "Asia/Chongqing",
        "Asia/Dacca", "Asia/Dhaka",
        "Asia/Ho_Chi_Minh", "Asia/Saigon",
        "Asia/Istanbul", "Europe/Istanbul",
        "Asia/Kathmandu", "Asia/Katmandu",
        "Asia/Kolkata", "Asia/Calcutta",
        "Asia/Macao", "Asia/Macau",
        "Asia/Tel_Aviv", "Asia/Jerusalem",
        "Asia/Thimbu", "Asia/Thimphu",
        "Asia/Ujung_Pandang", "Asia/Makassar",
        "Asia/Ulan_Bator", "Asia/Ulaanbaatar",
        "Atlantic/Faroe", "Atlantic/Faeroe",
        "Atlantic/Jan_Mayen", "Arctic/Longyearbyen",
        "Australia/ACT", "Australia/Sydney",
        "Australia/Canberra", "Australia/Sydney",
        "Australia/LHI", "Australia/Lord_Howe",
        "Australia/North", "Australia/Darwin",
        "Australia/NSW", "Australia/Sydney",
        "Australia/Queensland", "Australia/Brisbane",
        "Australia/South", "Australia/Adelaide",
        "Australia/Tasmania", "Australia/Hobart",
        "Australia/Victoria", "Australia/Melbourne",
        "Australia/West", "Australia/Perth",
        "Australia/Yancowinna", "Australia/Broken_Hill",
        "Brazil/Acre", "America/Rio_Branco",
        "Brazil/DeNoronha", "America/Noronha",
        "Brazil/East", "America/Sao_Paulo",
        "Brazil/West", "America/Manaus",
        "Canada/Atlantic", "America/Halifax",
        "Canada/Central", "America/Winnipeg",
        "Canada/Eastern", "America/Toronto",
        "Canada/East-Saskatchewan", "America/Regina",
        "Canada/Mountain", "America/Edmonton",
        "Canada/Newfoundland", "America/St_Johns",
        "Canada/Pacific", "America/Vancouver",
        "Canada/Saskatchewan", "America/Regina",
        "Canada/Yukon", "America/Whitehorse",
        "Chile/Continental", "America/Santiago",
        "Chile/EasterIsland", "Pacific/Easter",
        "Cuba", "America/Havana",
        "Egypt", "Africa/Cairo",
        "Eire", "Europe/Dublin",
        "EST", "Etc/GMT+5",
        "Etc/GMT+0", "Etc/GMT",
        "Etc/GMT0", "Etc/GMT",
        "Etc/GMT-0", "Etc/GMT",
        "Etc/Greenwich", "Etc/GMT",
        "Etc/UCT", "Etc/GMT",
        "Etc/Universal", "Etc/GMT",
        "Etc/UTC", "Etc/GMT",
        "Etc/Zulu", "Etc/GMT",
        "Europe/Belfast", "Europe/London",
        "Europe/Nicosia", "Asia/Nicosia",
        "Europe/Tiraspol", "Europe/Chisinau",
        "GB", "Europe/London",
        "GB-Eire", "Europe/London",
        "GMT", "Etc/GMT",
        "GMT+0", "Etc/GMT",
        "GMT0", "Etc/GMT",
        "GMT-0", "Etc/GMT",
        "Greenwich", "Etc/GMT",
        "Hongkong", "Asia/Hong_Kong",
        "HST", "Etc/GMT+10",
        "Iceland", "Atlantic/Reykjavik",
        "Iran", "Asia/Tehran",
        "Israel", "Asia/Jerusalem",
        "Jamaica", "America/Jamaica",
        "Japan", "Asia/Tokyo",
        "Kwajalein", "Pacific/Kwajalein",
        "Libya", "Africa/Tripoli",
        "Mexico/BajaNorte", "America/Tijuana",
        "Mexico/BajaSur", "America/Mazatlan",
        "Mexico/General", "America/Mexico_City",
        "MST", "Etc/GMT+7",
        "Navajo", "America/Shiprock",
        "NZ", "Pacific/Auckland",
        "NZ-CHAT", "Pacific/Chatham",
        "Pacific/Chuuk", "Pacific/Truk",
        "Pacific/Pohnpei", "Pacific/Ponape",
        "Pacific/Samoa", "Pacific/Pago_Pago",
        "Pacific/Yap", "Pacific/Truk",
        "Poland", "Europe/Warsaw",
        "Portugal", "Europe/Lisbon",
        "PRC", "Asia/Shanghai",
        "ROC", "Asia/Taipei",
        "ROK", "Asia/Seoul",
        "Singapore", "Asia/Singapore",
        "Turkey", "Europe/Istanbul",
        "UCT", "Etc/GMT",
        "Universal", "Etc/GMT",
        "US/Alaska", "America/Anchorage",
        "US/Aleutian", "America/Adak",
        "US/Arizona", "America/Phoenix",
        "US/Central", "America/Chicago",
        "US/Eastern", "America/New_York",
        "US/East-Indiana", "America/Indianapolis",
        "US/Hawaii", "Pacific/Honolulu",
        "US/Indiana-Starke", "America/Indiana/Knox",
        "US/Michigan", "America/Detroit",
        "US/Mountain", "America/Denver",
        "US/Pacific", "America/Los_Angeles",
        "US/Pacific-New", "America/Los_Angeles",
        "US/Samoa", "Pacific/Pago_Pago",
        "UTC", "Etc/GMT",
        "W-SU", "Europe/Moscow",
        "Zulu", "Etc/GMT",
    };
}
