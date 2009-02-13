/**
*******************************************************************************
* Copyright (C) 1996-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICUDebug;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.VersionInfo;

/**
* <p>Collator performs locale-sensitive string comparison. A concrete
* subclass, RuleBasedCollator, allows customization of the collation
* ordering by the use of rule sets.</p>
*
* <p>Following the <a href=http://www.unicode.org>Unicode
* Consortium</a>'s specifications for the
* <a href="http://www.unicode.org/unicode/reports/tr10/">Unicode Collation
* Algorithm (UCA)</a>, there are 5 different levels of strength used
* in comparisons:
*
* <ul>
* <li>PRIMARY strength: Typically, this is used to denote differences between
*     base characters (for example, "a" &lt; "b").
*     It is the strongest difference. For example, dictionaries are divided
*     into different sections by base character.
* <li>SECONDARY strength: Accents in the characters are considered secondary
*     differences (for example, "as" &lt; "&agrave;s" &lt; "at"). Other
*     differences
*     between letters can also be considered secondary differences, depending
*     on the language. A secondary difference is ignored when there is a
*     primary difference anywhere in the strings.
* <li>TERTIARY strength: Upper and lower case differences in characters are
*     distinguished at tertiary strength (for example, "ao" &lt; "Ao" &lt;
*     "a&ograve;"). In addition, a variant of a letter differs from the base
*     form on the tertiary strength (such as "A" and "&#9398;"). Another
*     example is the
*     difference between large and small Kana. A tertiary difference is ignored
*     when there is a primary or secondary difference anywhere in the strings.
* <li>QUATERNARY strength: When punctuation is ignored
*     <a href="http://www.icu-project.org/userguide/Collate_Concepts.html#Ignoring_Punctuation">
*     (see Ignoring Punctuations in the user guide)</a> at PRIMARY to TERTIARY
*     strength, an additional strength level can
*     be used to distinguish words with and without punctuation (for example,
*     "ab" &lt; "a-b" &lt; "aB").
*     This difference is ignored when there is a PRIMARY, SECONDARY or TERTIARY
*     difference. The QUATERNARY strength should only be used if ignoring
*     punctuation is required.
* <li>IDENTICAL strength:
*     When all other strengths are equal, the IDENTICAL strength is used as a
*     tiebreaker. The Unicode code point values of the NFD form of each string
*     are compared, just in case there is no difference.
*     For example, Hebrew cantellation marks are only distinguished at this
*     strength. This strength should be used sparingly, as only code point
*     value differences between two strings is an extremely rare occurrence.
*     Using this strength substantially decreases the performance for both
*     comparison and collation key generation APIs. This strength also
*     increases the size of the collation key.
* </ul>
*
* Unlike the JDK, ICU4J's Collator deals only with 2 decomposition modes,
* the canonical decomposition mode and one that does not use any decomposition.
* The compatibility decomposition mode, java.text.Collator.FULL_DECOMPOSITION
* is not supported here. If the canonical
* decomposition mode is set, the Collator handles un-normalized text properly,
* producing the same results as if the text were normalized in NFD. If
* canonical decomposition is turned off, it is the user's responsibility to
* ensure that all text is already in the appropriate form before performing
* a comparison or before getting a CollationKey.</p>
*
* <p>For more information about the collation service see the
* <a href="http://www.icu-project.org/userguide/Collate_Intro.html">users
* guide</a>.</p>
*
* <p>Examples of use
* <pre>
* // Get the Collator for US English and set its strength to PRIMARY
* Collator usCollator = Collator.getInstance(Locale.US);
* usCollator.setStrength(Collator.PRIMARY);
* if (usCollator.compare("abc", "ABC") == 0) {
*     System.out.println("Strings are equivalent");
* }
*
* The following example shows how to compare two strings using the
* Collator for the default locale.
*
* // Compare two strings in the default locale
* Collator myCollator = Collator.getInstance();
* myCollator.setDecomposition(NO_DECOMPOSITION);
* if (myCollator.compare("&agrave;&#92;u0325", "a&#92;u0325&#768;") != 0) {
*     System.out.println("&agrave;&#92;u0325 is not equals to a&#92;u0325&#768; without decomposition");
*     myCollator.setDecomposition(CANONICAL_DECOMPOSITION);
*     if (myCollator.compare("&agrave;&#92;u0325", "a&#92;u0325&#768;") != 0) {
*         System.out.println("Error: &agrave;&#92;u0325 should be equals to a&#92;u0325&#768; with decomposition");
*     }
*     else {
*         System.out.println("&agrave;&#92;u0325 is equals to a&#92;u0325&#768; with decomposition");
*     }
* }
* else {
*     System.out.println("Error: &agrave;&#92;u0325 should be not equals to a&#92;u0325&#768; without decomposition");
* }
* </pre>
* </p>
* @see RuleBasedCollator
* @see CollationKey
* @author Syn Wee Quek
* @stable ICU 2.8
*/
public abstract class Collator implements Comparator, Cloneable
{
    // public data members ---------------------------------------------------

    /**
     * Strongest collator strength value. Typically used to denote differences
     * between base characters. See class documentation for more explanation.
     * @see #setStrength
     * @see #getStrength
     * @stable ICU 2.8
     */
    public final static int PRIMARY = 0;

    /**
     * Second level collator strength value.
     * Accents in the characters are considered secondary differences.
     * Other differences between letters can also be considered secondary
     * differences, depending on the language.
     * See class documentation for more explanation.
     * @see #setStrength
     * @see #getStrength
     * @stable ICU 2.8
     */
    public final static int SECONDARY = 1;

    /**
     * Third level collator strength value.
     * Upper and lower case differences in characters are distinguished at this
     * strength level. In addition, a variant of a letter differs from the base
     * form on the tertiary level.
     * See class documentation for more explanation.
     * @see #setStrength
     * @see #getStrength
     * @stable ICU 2.8
     */
    public final static int TERTIARY = 2;

    /**
     * Fourth level collator strength value.
     * When punctuation is ignored
     * <a href="http://www.icu-project.org/userguide/Collate_Concepts.html#Ignoring_Punctuation">
     * (see Ignoring Punctuations in the user guide)</a> at PRIMARY to TERTIARY
     * strength, an additional strength level can
     * be used to distinguish words with and without punctuation.
     * See class documentation for more explanation.
     * @see #setStrength
     * @see #getStrength
     * @stable ICU 2.8
     */
    public final static int QUATERNARY = 3;

    /**
     * <p>
     * Smallest Collator strength value. When all other strengths are equal,
     * the IDENTICAL strength is used as a tiebreaker. The Unicode code point
     * values of the NFD form of each string are compared, just in case there
     * is no difference.
     * See class documentation for more explanation.
     * </p>
     * <p>
     * Note this value is different from JDK's
     * </p>
     * @stable ICU 2.8
     */
    public final static int IDENTICAL = 15;

    /**
     * This is for backwards compatibility with Java APIs only.  It
     * should not be used, IDENTICAL should be used instead.  ICU's
     * collation does not support Java's FULL_DECOMPOSITION mode.
     * @stable ICU 3.4
     */
    public final static int FULL_DECOMPOSITION = IDENTICAL;

    /**
     * <p>Decomposition mode value. With NO_DECOMPOSITION set, Strings
     * will not be decomposed for collation. This is the default
     * decomposition setting unless otherwise specified by the locale
     * used to create the Collator.</p>
     *
     * <p><strong>Note</strong> this value is different from the JDK's.</p>
     * @see #CANONICAL_DECOMPOSITION
     * @see #getDecomposition
     * @see #setDecomposition
     * @stable ICU 2.8
     */
    public final static int NO_DECOMPOSITION = 16;

    /**
     * <p>Decomposition mode value. With CANONICAL_DECOMPOSITION set,
     * characters that are canonical variants according to the Unicode standard
     * will be decomposed for collation.</p>
     *
     * <p>CANONICAL_DECOMPOSITION corresponds to Normalization Form D as
     * described in <a href="http://www.unicode.org/unicode/reports/tr15/">
     * Unicode Technical Report #15</a>.
     * </p>
     * @see #NO_DECOMPOSITION
     * @see #getDecomposition
     * @see #setDecomposition
     * @stable ICU 2.8
     */
    public final static int CANONICAL_DECOMPOSITION = 17;

    // public methods --------------------------------------------------------

    // public setters --------------------------------------------------------

    /**
     * <p>Sets this Collator's strength property. The strength property
     * determines the minimum level of difference considered significant
     * during comparison.</p>
     *
     * <p>The default strength for the Collator is TERTIARY, unless specified
     * otherwise by the locale used to create the Collator.</p>
     *
     * <p>See the Collator class description for an example of use.</p>
     * @param newStrength the new strength value.
     * @see #getStrength
     * @see #PRIMARY
     * @see #SECONDARY
     * @see #TERTIARY
     * @see #QUATERNARY
     * @see #IDENTICAL
     * @exception IllegalArgumentException if the new strength value is not one
     *                of PRIMARY, SECONDARY, TERTIARY, QUATERNARY or IDENTICAL.
     * @stable ICU 2.8
     */
    public void setStrength(int newStrength)
    {
        if ((newStrength != PRIMARY) &&
            (newStrength != SECONDARY) &&
            (newStrength != TERTIARY) &&
            (newStrength != QUATERNARY) &&
            (newStrength != IDENTICAL)) {
            throw new IllegalArgumentException("Incorrect comparison level.");
        }
        m_strength_ = newStrength;
    }

    /**
     * <p>Set the decomposition mode of this Collator.  Setting this
     * decomposition property with CANONICAL_DECOMPOSITION allows the
     * Collator to handle un-normalized text properly, producing the
     * same results as if the text were normalized. If
     * NO_DECOMPOSITION is set, it is the user's responsibility to
     * insure that all text is already in the appropriate form before
     * a comparison or before getting a CollationKey. Adjusting
     * decomposition mode allows the user to select between faster and
     * more complete collation behavior.</p>
     *
     * <p>Since a great many of the world's languages do not require
     * text normalization, most locales set NO_DECOMPOSITION as the
     * default decomposition mode.</p>
     *
     * The default decompositon mode for the Collator is
     * NO_DECOMPOSITON, unless specified otherwise by the locale used
     * to create the Collator.</p>
     *
     * <p>See getDecomposition for a description of decomposition
     * mode.</p>
     *
     * @param decomposition the new decomposition mode
     * @see #getDecomposition
     * @see #NO_DECOMPOSITION
     * @see #CANONICAL_DECOMPOSITION
     * @exception IllegalArgumentException If the given value is not a valid
     *            decomposition mode.
     * @stable ICU 2.8
     */
    public void setDecomposition(int decomposition)
    {
        if ((decomposition != NO_DECOMPOSITION) &&
            (decomposition != CANONICAL_DECOMPOSITION)) {
            throw new IllegalArgumentException("Wrong decomposition mode.");
        }
        m_decomposition_ = decomposition;
    }

    // public getters --------------------------------------------------------

    /**
     * Gets the Collator for the current default locale.
     * The default locale is determined by java.util.Locale.getDefault().
     * @return the Collator for the default locale (for example, en_US) if it
     *         is created successfully. Otherwise if there is no Collator
     *         associated with the current locale, the default UCA collator
     *         will be returned.
     * @see java.util.Locale#getDefault()
     * @see #getInstance(Locale)
     * @stable ICU 2.8
     */
    public static final Collator getInstance()
    {
        return getInstance(ULocale.getDefault());
    }

    /**
     * Clone the collator.
     * @stable ICU 2.6
     * @return a clone of this collator.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // begin registry stuff

    /**
     * A factory used with registerFactory to register multiple collators and provide
     * display names for them.  If standard locale display names are sufficient, 
     * Collator instances may be registered instead.
     * <p><b>Note:</b> as of ICU4J 3.2, the default API for CollatorFactory uses
     * ULocale instead of Locale.  Instead of overriding createCollator(Locale),
     * new implementations should override createCollator(ULocale).  Note that
     * one of these two methods <b>MUST</b> be overridden or else an infinite
     * loop will occur.
     * @stable ICU 2.6
     */
    public static abstract class CollatorFactory {
        /**
         * Return true if this factory will be visible.  Default is true.
         * If not visible, the locales supported by this factory will not
         * be listed by getAvailableLocales.
         *
         * @return true if this factory is visible
         * @stable ICU 2.6
         */
        public boolean visible() {
            return true;
        }

        /**
         * Return an instance of the appropriate collator.  If the locale
         * is not supported, return null.
         * <b>Note:</b> as of ICU4J 3.2, implementations should override
         * this method instead of createCollator(Locale).
         * @param loc the locale for which this collator is to be created.
         * @return the newly created collator.
         * @stable ICU 3.2
         */
        public Collator createCollator(ULocale loc) {
            return createCollator(loc.toLocale());
        }

        /**
         * Return an instance of the appropriate collator.  If the locale
         * is not supported, return null.
         * <p><b>Note:</b> as of ICU4J 3.2, implementations should override
         * createCollator(ULocale) instead of this method, and inherit this
         * method's implementation.  This method is no longer abstract
         * and instead delegates to createCollator(ULocale).
         * @param loc the locale for which this collator is to be created.
         * @return the newly created collator.
         * @stable ICU 2.6
         */
         public Collator createCollator(Locale loc) {
            return createCollator(ULocale.forLocale(loc));
        }

        /**
         * Return the name of the collator for the objectLocale, localized for the displayLocale.
         * If objectLocale is not visible or not defined by the factory, return null.
         * @param objectLocale the locale identifying the collator
         * @param displayLocale the locale for which the display name of the collator should be localized
         * @return the display name
         * @stable ICU 2.6
         */
        public String getDisplayName(Locale objectLocale, Locale displayLocale) {
            return getDisplayName(ULocale.forLocale(objectLocale), ULocale.forLocale(displayLocale));
        }

        /**
         * Return the name of the collator for the objectLocale, localized for the displayLocale.
         * If objectLocale is not visible or not defined by the factory, return null.
         * @param objectLocale the locale identifying the collator
         * @param displayLocale the locale for which the display name of the collator should be localized
         * @return the display name
         * @stable ICU 3.2
         */
        public String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
            if (visible()) {
                Set supported = getSupportedLocaleIDs();
                String name = objectLocale.getBaseName();
                if (supported.contains(name)) {
                    return objectLocale.getDisplayName(displayLocale);
                }
            }
            return null;
        }

        /**
         * Return an unmodifiable collection of the locale names directly 
         * supported by this factory.
         *
         * @return the set of supported locale IDs.
         * @stable ICU 2.6
         */
        public abstract Set getSupportedLocaleIDs();

        /**
         * Empty default constructor.
         * @stable ICU 2.6
         */
        protected CollatorFactory() {
        }
    }

    static abstract class ServiceShim {
        abstract Collator getInstance(ULocale l);
        abstract Object registerInstance(Collator c, ULocale l);
        abstract Object registerFactory(CollatorFactory f);
        abstract boolean unregister(Object k);
        abstract Locale[] getAvailableLocales(); // TODO remove
        abstract ULocale[] getAvailableULocales();
        abstract String getDisplayName(ULocale ol, ULocale dl);
    }
    
    private static ServiceShim shim;
    private static ServiceShim getShim() {
        // Note: this instantiation is safe on loose-memory-model configurations
        // despite lack of synchronization, since the shim instance has no state--
        // it's all in the class init.  The worst problem is we might instantiate
        // two shim instances, but they'll share the same state so that's ok.
        if (shim == null) {
            try {
                Class cls = Class.forName("com.ibm.icu.text.CollatorServiceShim");
                shim = (ServiceShim)cls.newInstance();
            }
            catch (MissingResourceException e)
            {
                throw e;
            }
            catch (Exception e) {
                ///CLOVER:OFF
                if(DEBUG){
                    e.printStackTrace();
                }
                throw new RuntimeException(e.getMessage());
                ///CLOVER:ON
            }
        }
        return shim;
    }

    /**
     * Gets the Collator for the desired locale.
     * @param locale the desired locale.
     * @return Collator for the desired locale if it is created successfully.
     *         Otherwise if there is no Collator
     *         associated with the current locale, a default UCA collator will
     *         be returned.
     * @see java.util.Locale
     * @see java.util.ResourceBundle
     * @see #getInstance(Locale)
     * @see #getInstance()
     * @stable ICU 3.0
     */
    public static final Collator getInstance(ULocale locale) {
        // fetching from service cache is faster than instantiation
        return getShim().getInstance(locale);
    }

    /**
     * Gets the Collator for the desired locale.
     * @param locale the desired locale.
     * @return Collator for the desired locale if it is created successfully.
     *         Otherwise if there is no Collator
     *         associated with the current locale, a default UCA collator will
     *         be returned.
     * @see java.util.Locale
     * @see java.util.ResourceBundle
     * @see #getInstance(ULocale)
     * @see #getInstance()
     * @stable ICU 2.8
     */
    public static final Collator getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Register a collator as the default collator for the provided locale.  The
     * collator should not be modified after it is registered.
     *
     * @param collator the collator to register
     * @param locale the locale for which this is the default collator
     * @return an object that can be used to unregister the registered collator.
     *
     * @stable ICU 3.2
     */
    public static final Object registerInstance(Collator collator, ULocale locale) {
        return getShim().registerInstance(collator, locale);
    }

    /**
     * Register a collator factory.
     *
     * @param factory the factory to register
     * @return an object that can be used to unregister the registered factory.
     *
     * @stable ICU 2.6
     */
    public static final Object registerFactory(CollatorFactory factory) {
        return getShim().registerFactory(factory);
    }

    /**
     * Unregister a collator previously registered using registerInstance.
     * @param registryKey the object previously returned by registerInstance.
     * @return true if the collator was successfully unregistered.
     * @stable ICU 2.6
     */
    public static final boolean unregister(Object registryKey) {
        if (shim == null) {
            return false;
        }
        return shim.unregister(registryKey);
    }

    /**
     * Get the set of locales, as Locale objects, for which collators
     * are installed.  Note that Locale objects do not support RFC 3066.
     * @return the list of locales in which collators are installed.
     * This list includes any that have been registered, in addition to
     * those that are installed with ICU4J.
     * @stable ICU 2.4
     */
    public static Locale[] getAvailableLocales() {
        // TODO make this wrap getAvailableULocales later
        if (shim == null) {
            return ICUResourceBundle.getAvailableLocales(ICUResourceBundle.ICU_COLLATION_BASE_NAME);
        }
        return shim.getAvailableLocales();
    }

    /**
     * Get the set of locales, as ULocale objects, for which collators
     * are installed.  ULocale objects support RFC 3066.
     * @return the list of locales in which collators are installed.
     * This list includes any that have been registered, in addition to
     * those that are installed with ICU4J.
     * @stable ICU 3.0
     */
    public static final ULocale[] getAvailableULocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableULocales(ICUResourceBundle.ICU_COLLATION_BASE_NAME);
        }
        return shim.getAvailableULocales();
    }
    
    /**
     * The list of keywords for this service.  This must be kept in sync with
     * the resource data.
     * @since ICU 3.0
     */
    private static final String[] KEYWORDS = { "collation" };

    /**
     * The resource name for this service.  Note that this is not the same as
     * the keyword for this service.
     * @since ICU 3.0
     */
    private static final String RESOURCE = "collations";

    /**
     * The resource bundle base name for this service.
     * *since ICU 3.0
     */
    private static final String BASE = ICUResourceBundle.ICU_COLLATION_BASE_NAME;

    /**
     * Return an array of all possible keywords that are relevant to
     * collation. At this point, the only recognized keyword for this
     * service is "collation".
     * @return an array of valid collation keywords.
     * @see #getKeywordValues
     * @stable ICU 3.0
     */
    public static final String[] getKeywords() {
        return KEYWORDS;
    }
    
    /**
     * Given a keyword, return an array of all values for
     * that keyword that are currently in use.
     * @param keyword one of the keywords returned by getKeywords.
     * @see #getKeywords
     * @stable ICU 3.0
     */
    public static final String[] getKeywordValues(String keyword) {
        if (!keyword.equals(KEYWORDS[0])) {
            throw new IllegalArgumentException("Invalid keyword: " + keyword);
        }
        return ICUResourceBundle.getKeywordValues(BASE, RESOURCE);
    }

    /**
     * Return the functionally equivalent locale for the given
     * requested locale, with respect to given keyword, for the
     * collation service.  If two locales return the same result, then
     * collators instantiated for these locales will behave
     * equivalently.  The converse is not always true; two collators
     * may in fact be equivalent, but return different results, due to
     * internal details.  The return result has no other meaning than
     * that stated above, and implies nothing as to the relationship
     * between the two locales.  This is intended for use by
     * applications who wish to cache collators, or otherwise reuse
     * collators when possible.  The functional equivalent may change
     * over time.  For more information, please see the <a
     * href="http://www.icu-project.org/userguide/locale.html#services">
     * Locales and Services</a> section of the ICU User Guide.
     * @param keyword a particular keyword as enumerated by
     * getKeywords.
     * @param locID The requested locale
     * @param isAvailable If non-null, isAvailable[0] will receive and
     * output boolean that indicates whether the requested locale was
     * 'available' to the collation service. If non-null, isAvailable 
     * must have length >= 1.
     * @return the locale
     * @stable ICU 3.0
     */
    public static final ULocale getFunctionalEquivalent(String keyword,
                                                        ULocale locID,
                                                        boolean isAvailable[]) {
        return ICUResourceBundle.getFunctionalEquivalent(
                                                         BASE, RESOURCE, keyword, locID, isAvailable, true);
    }
    
    /**
     * Return the functionally equivalent locale for the given
     * requested locale, with respect to given keyword, for the
     * collation service.
     * @param keyword a particular keyword as enumerated by
     * getKeywords.
     * @param locID The requested locale
     * @return the locale
     * @see #getFunctionalEquivalent(String,ULocale,boolean[])
     * @stable ICU 3.0
     */
    public static final ULocale getFunctionalEquivalent(String keyword,
                                                        ULocale locID) {
        return getFunctionalEquivalent(keyword, locID, null);
    }
    
    /**
     * Get the name of the collator for the objectLocale, localized for the displayLocale.
     * @param objectLocale the locale of the collator
     * @param displayLocale the locale for the collator's display name
     * @return the display name
     * @stable ICU 2.6
     */
    static public String getDisplayName(Locale objectLocale, Locale displayLocale) {
        return getShim().getDisplayName(ULocale.forLocale(objectLocale), 
                                        ULocale.forLocale(displayLocale));
    }

    /**
     * Get the name of the collator for the objectLocale, localized for the displayLocale.
     * @param objectLocale the locale of the collator
     * @param displayLocale the locale for the collator's display name
     * @return the display name
     * @stable ICU 3.2
     */
    static public String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
        return getShim().getDisplayName(objectLocale, displayLocale);
    }

    /**
     * Get the name of the collator for the objectLocale, localized for the current locale.
     * @param objectLocale the locale of the collator
     * @return the display name
     * @stable ICU 2.6
     */
    static public String getDisplayName(Locale objectLocale) {
        return getShim().getDisplayName(ULocale.forLocale(objectLocale), ULocale.getDefault());
    }

    /**
     * Get the name of the collator for the objectLocale, localized for the current locale.
     * @param objectLocale the locale of the collator
     * @return the display name
     * @stable ICU 3.2
     */
    static public String getDisplayName(ULocale objectLocale) {
        return getShim().getDisplayName(objectLocale, ULocale.getDefault());
    }

    /**
     * <p>Returns this Collator's strength property. The strength property
     * determines the minimum level of difference considered significant.
     * </p>
     * <p>
     * See the Collator class description for more details.
     * </p>
     * @return this Collator's current strength property.
     * @see #setStrength
     * @see #PRIMARY
     * @see #SECONDARY
     * @see #TERTIARY
     * @see #QUATERNARY
     * @see #IDENTICAL
     * @stable ICU 2.8
     */
    public int getStrength()
    {
        return m_strength_;
    }

    /**
     * <p>
     * Get the decomposition mode of this Collator. Decomposition mode
     * determines how Unicode composed characters are handled.
     * </p>
     * <p>
     * See the Collator class description for more details.
     * </p>
     * @return the decomposition mode
     * @see #setDecomposition
     * @see #NO_DECOMPOSITION
     * @see #CANONICAL_DECOMPOSITION
     * @stable ICU 2.8
     */
    public int getDecomposition()
    {
        return m_decomposition_;
    }

    /**
     * <p>
     * Compares the source text String to the target text String according to
     * this Collator's rules, strength and decomposition mode.
     * Returns an integer less than,
     * equal to or greater than zero depending on whether the source String is
     * less than, equal to or greater than the target String. See the Collator
     * class description for an example of use.
     * </p>
     * @param source the source String.
     * @param target the target String.
     * @return Returns an integer value. Value is less than zero if source is
     *         less than target, value is zero if source and target are equal,
     *         value is greater than zero if source is greater than target.
     * @see CollationKey
     * @see #getCollationKey
     * @exception NullPointerException thrown if either arguments is null.
     *            IllegalArgumentException thrown if either source or target is
     *            not of the class String.
     * @stable ICU 2.8
     */
    public int compare(Object source, Object target)
    {
        if (!(source instanceof String) || !(target instanceof String)) {
            throw new IllegalArgumentException("Arguments have to be of type String");
        }
        return compare((String)source, (String)target);
    }

    // public other methods -------------------------------------------------

    /**
     * Convenience method for comparing the equality of two text Strings using
     * this Collator's rules, strength and decomposition mode.
     * @param source the source string to be compared.
     * @param target the target string to be compared.
     * @return true if the strings are equal according to the collation
     *         rules, otherwise false.
     * @see #compare
     * @exception NullPointerException thrown if either arguments is null.
     * @stable ICU 2.8
     */
    public boolean equals(String source, String target)
    {
        return (compare(source, target) == 0);
    }

    /**
     * Get an UnicodeSet that contains all the characters and sequences
     * tailored in this collator.
     * @return a pointer to a UnicodeSet object containing all the
     *         code points and sequences that may sort differently than
     *         in the UCA.
     * @stable ICU 2.4
     */
    public UnicodeSet getTailoredSet()
    {
        return new UnicodeSet(0, 0x10FFFF);
    }

    /**
     * <p>
     * Compares the source text String to the target text String according to
     * this Collator's rules, strength and decomposition mode.
     * Returns an integer less than,
     * equal to or greater than zero depending on whether the source String is
     * less than, equal to or greater than the target String. See the Collator
     * class description for an example of use.
     * </p>
     * @param source the source String.
     * @param target the target String.
     * @return Returns an integer value. Value is less than zero if source is
     *         less than target, value is zero if source and target are equal,
     *         value is greater than zero if source is greater than target.
     * @see CollationKey
     * @see #getCollationKey
     * @exception NullPointerException thrown if either arguments is null.
     * @stable ICU 2.8
     */
    public abstract int compare(String source, String target);

    /**
     * <p>
     * Transforms the String into a CollationKey suitable for efficient
     * repeated comparison.  The resulting key depends on the collator's
     * rules, strength and decomposition mode.
     * </p>
     * <p>See the CollationKey class documentation for more information.</p>
     * @param source the string to be transformed into a CollationKey.
     * @return the CollationKey for the given String based on this Collator's
     *         collation rules. If the source String is null, a null
     *         CollationKey is returned.
     * @see CollationKey
     * @see #compare(String, String)
     * @see #getRawCollationKey
     * @stable ICU 2.8
     */
    public abstract CollationKey getCollationKey(String source);
    
    /**
     * Gets the simpler form of a CollationKey for the String source following
     * the rules of this Collator and stores the result into the user provided 
     * argument key. 
     * If key has a internal byte array of length that's too small for the 
     * result, the internal byte array will be grown to the exact required 
     * size.
     * @param source the text String to be transformed into a RawCollationKey  
     * @return If key is null, a new instance of RawCollationKey will be 
     *         created and returned, otherwise the user provided key will be 
     *         returned.
     * @see #compare(String, String)
     * @see #getCollationKey 
     * @see RawCollationKey
     * @stable ICU 2.8
     */
    public abstract RawCollationKey getRawCollationKey(String source, 
                                                       RawCollationKey key);

    /** 
     * <p>
     * Variable top is a two byte primary value which causes all the codepoints 
     * with primary values that are less or equal than the variable top to be 
     * shifted when alternate handling is set to SHIFTED.
     * </p>
     * <p>
     * Sets the variable top to a collation element value of a string supplied.
     * </p> 
     * @param varTop one or more (if contraction) characters to which the 
     *               variable top should be set
     * @return a int value containing the value of the variable top in upper 16
     *         bits. Lower 16 bits are undefined.
     * @exception IllegalArgumentException is thrown if varTop argument is not 
     *            a valid variable top element. A variable top element is 
     *            invalid when it is a contraction that does not exist in the
     *            Collation order or when the PRIMARY strength collation 
     *            element for the variable top has more than two bytes
     * @see #getVariableTop
     * @see RuleBasedCollator#setAlternateHandlingShifted
     * @stable ICU 2.6
     */
    public abstract int setVariableTop(String varTop);
    
    /** 
     * Gets the variable top value of a Collator. 
     * Lower 16 bits are undefined and should be ignored.
     * @return the variable top value of a Collator.
     * @see #setVariableTop
     * @stable ICU 2.6
     */
    public abstract int getVariableTop();
    
    /** 
     * Sets the variable top to a collation element value supplied.
     * Variable top is set to the upper 16 bits. 
     * Lower 16 bits are ignored.
     * @param varTop Collation element value, as returned by setVariableTop or 
     *               getVariableTop
     * @see #getVariableTop
     * @see #setVariableTop
     * @stable ICU 2.6
     */
    public abstract void setVariableTop(int varTop);

    /** 
     * Get the version of this collator object.
     * @return the version object associated with this collator
     * @stable ICU 2.8
     */
    public abstract VersionInfo getVersion();
        
    /** 
     * Get the UCA version of this collator object.
     * @return the version object associated with this collator
     * @stable ICU 2.8
     */
    public abstract VersionInfo getUCAVersion();
        
    // protected constructor -------------------------------------------------

    /**
     * Empty default constructor to make javadocs happy
     * @stable ICU 2.4
     */
    protected Collator()
    {
    }
    
    // package private methods -----------------------------------------------

    // private data members --------------------------------------------------

    /**
     * Collation strength
     */
    private int m_strength_ = TERTIARY;

    /**
     * Decomposition mode
     */
    private int m_decomposition_ = CANONICAL_DECOMPOSITION;
    
    private static final boolean DEBUG = ICUDebug.enabled("collator");
    
    // private methods -------------------------------------------------------
    
    // end registry stuff

    // -------- BEGIN ULocale boilerplate --------

    /**
     * Return the locale that was used to create this object, or null.
     * This may may differ from the locale requested at the time of
     * this object's creation.  For example, if an object is created
     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
     * <tt>en_US</tt> may be the most specific locale that exists (the
     * <i>valid</i> locale).
     *
     * <p>Note: This method will be implemented in ICU 3.0; ICU 2.8
     * contains a partial preview implementation.  The * <i>actual</i>
     * locale is returned correctly, but the <i>valid</i> locale is
     * not, in most cases.
     * @param type type of information requested, either {@link
     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
     * @return the information specified by <i>type</i>, or null if
     * this object was not constructed from locale data.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @draft ICU 2.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ?
            this.actualLocale : this.validLocale;
    }

    /**
     * Set information about the locales that were used to create this
     * object.  If the object was not constructed from locale data,
     * both arguments should be set to null.  Otherwise, neither
     * should be null.  The actual locale must be at the same level or
     * less specific than the valid locale.  This method is intended
     * for use by factories or other entities that create objects of
     * this class.
     * @param valid the most specific locale containing any resource
     * data, or null
     * @param actual the locale containing data used to construct this
     * object, or null
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @internal
     */
    final void setLocale(ULocale valid, ULocale actual) {
        // Change the following to an assertion later
        if ((valid == null) != (actual == null)) {
            ///CLOVER:OFF
            throw new IllegalArgumentException();
            ///CLOVER:ON
        }
        // Another check we could do is that the actual locale is at
        // the same level or less specific than the valid locale.
        this.validLocale = valid;
        this.actualLocale = actual;
    }

    /**
     * The most specific locale containing any resource data, or null.
     * @see com.ibm.icu.util.ULocale
     * @internal
     */
    private ULocale validLocale;

    /**
     * The locale containing data used to construct this object, or
     * null.
     * @see com.ibm.icu.util.ULocale
     * @internal
     */
    private ULocale actualLocale;

    // -------- END ULocale boilerplate --------
    /**
     * Given a key and a locale, returns an array of string values in a preferred
     * order that would make a difference. These are all and only those values where
     * the open (creation) of the service with the locale formed from the input locale
     * plus input keyword and that value has different behavior than creation with the
     * input locale alone.
     * @param key           one of the keys supported by this service.  For now, only
     *                      "collation" is supported.
     * @param locale        the locale
     * @param commonlyUsed  if set to true it will return only commonly used values
     *                      with the given locale in preferred order.  Otherwise,
     *                      it will return all the available values for the locale.
     * @return an array of string values for the given key and the locale.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static final String[] getKeywordValues(String key, ULocale locale, boolean commonlyUsed) {
        // Note: The parameter commonlyUsed is actually not used.
        // The switch is in the method signature for consistency
        // with other locale services.

        // Read available collation values from collation bundles
        String baseLoc = locale.getBaseName();
        LinkedList values = new LinkedList();

        UResourceBundle bundle = UResourceBundle.getBundleInstance(
                ICUResourceBundle.ICU_BASE_NAME + "/coll", baseLoc);

        String defcoll = null;
        while (bundle != null) {
            UResourceBundle collations = bundle.get("collations");
            Enumeration collEnum = collations.getKeys();
            while (collEnum.hasMoreElements()) {
                String collkey = (String)collEnum.nextElement();
                if (collkey.equals("default")) {
                    if (defcoll == null) {
                        // Keep the default
                        defcoll = collations.getString("default");
                    }
                } else if (!values.contains(collkey)) {
                    values.add(collkey);
                }
            }
            bundle = ((ICUResourceBundle)bundle).getParent();
        }
        // Reordering
        Iterator itr = values.iterator();
        String[] result = new String[values.size()];
        result[0] = defcoll;
        int idx = 1;
        while (itr.hasNext()) {
            String collKey = (String)itr.next();
            if (!collKey.equals(defcoll)) {
                result[idx++] = collKey;
            }
        }
        return result;
    }
}
