// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 1996-2012, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
* {@icuenhanced java.text.Collator}.{@icu _usage_}
*
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
public class Collator implements Comparator<Object>, Cloneable
{
    /**
     * @internal
     */
    private final java.text.Collator collator;

    /**
     * @internal
     */
    private Collator(java.text.Collator delegate) {
        this.collator = delegate;
    }

    /**
     * Create a collator with a null delegate.
     * For use by possible subclassers.  This is present since
     * the original Collator is abstract, and so, in theory
     * subclassable.  All member APIs must be overridden.
     */
    protected Collator() {
        this.collator = null;
    }

    // public data members ---------------------------------------------------

    /**
     * Strongest collator strength value. Typically used to denote differences
     * between base characters. See class documentation for more explanation.
     * @see #setStrength
     * @see #getStrength
     * @stable ICU 2.8
     */
    public final static int PRIMARY = java.text.Collator.PRIMARY;

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
    public final static int SECONDARY = java.text.Collator.SECONDARY;

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
    public final static int TERTIARY = java.text.Collator.TERTIARY;

    /**
     * {@icu} Fourth level collator strength value.
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
    public final static int QUATERNARY = java.text.Collator.IDENTICAL;

    /**
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
    public final static int IDENTICAL = java.text.Collator.FULL_DECOMPOSITION;

    /**
     * {@icunote} This is for backwards compatibility with Java APIs only.  It
     * should not be used, IDENTICAL should be used instead.  ICU's
     * collation does not support Java's FULL_DECOMPOSITION mode.
     * @stable ICU 3.4
     */
    public final static int FULL_DECOMPOSITION = java.text.Collator.FULL_DECOMPOSITION;

    /**
     * Decomposition mode value. With NO_DECOMPOSITION set, Strings
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
    public final static int NO_DECOMPOSITION = java.text.Collator.NO_DECOMPOSITION;

    /**
     * Decomposition mode value. With CANONICAL_DECOMPOSITION set,
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
    public final static int CANONICAL_DECOMPOSITION = java.text.Collator.CANONICAL_DECOMPOSITION;

    // public methods --------------------------------------------------------

    // public setters --------------------------------------------------------

    /**
     * Sets this Collator's strength property. The strength property
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
     * @throws IllegalArgumentException if the new strength value is not one
     *                of PRIMARY, SECONDARY, TERTIARY, QUATERNARY or IDENTICAL.
     * @stable ICU 2.8
     */
    public void setStrength(int newStrength)
    {
        if (isFrozen) {
            throw new UnsupportedOperationException("Attempt to modify a frozen Collator instance.");
        }
        collator.setStrength(newStrength);
    }

    /**
     * Sets the decomposition mode of this Collator.  Setting this
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
     * @throws IllegalArgumentException If the given value is not a valid
     *            decomposition mode.
     * @stable ICU 2.8
     */
    public void setDecomposition(int decomposition)
    {
        if (isFrozen) {
            throw new UnsupportedOperationException("Attempt to modify a frozen Collator instance.");
        }
        collator.setDecomposition(decomposition);
    }

    // public getters --------------------------------------------------------

    /**
     * Returns the Collator for the current default locale.
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
        return new Collator(java.text.Collator.getInstance());
    }

    /**
     * Clones the collator.
     * @stable ICU 2.6
     * @return a clone of this collator.
     */
    public Object clone() throws CloneNotSupportedException {
        return new Collator((java.text.Collator)collator.clone());
    }

    // Freezable interface implementation -------------------------------------------------

    private transient boolean isFrozen = false;

    /**
     * Determines whether the object has been frozen or not.
     * @draft ICU 4.8
     */
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * Freezes the collator.
     * @return the collator itself.
     * @draft ICU 4.8
     */
    public Collator freeze() {
        isFrozen = true;
        return this;
    }

    /**
     * Provides for the clone operation. Any clone is initially unfrozen.
     * @draft ICU 4.8
     */
    public Collator cloneAsThawed() {
        try {
            Collator other = (Collator) super.clone();
            other.isFrozen = false;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    // begin registry stuff

//    /**
//     * A factory used with registerFactory to register multiple collators and provide
//     * display names for them.  If standard locale display names are sufficient,
//     * Collator instances may be registered instead.
//     * <p><b>Note:</b> as of ICU4J 3.2, the default API for CollatorFactory uses
//     * ULocale instead of Locale.  Instead of overriding createCollator(Locale),
//     * new implementations should override createCollator(ULocale).  Note that
//     * one of these two methods <b>MUST</b> be overridden or else an infinite
//     * loop will occur.
//     * @stable ICU 2.6
//     */
//    public static abstract class CollatorFactory {
//        /**
//         * Return true if this factory will be visible.  Default is true.
//         * If not visible, the locales supported by this factory will not
//         * be listed by getAvailableLocales.
//         *
//         * @return true if this factory is visible
//         * @stable ICU 2.6
//         */
//        public boolean visible() {
//            return true;
//        }
//
//        /**
//         * Return an instance of the appropriate collator.  If the locale
//         * is not supported, return null.
//         * <b>Note:</b> as of ICU4J 3.2, implementations should override
//         * this method instead of createCollator(Locale).
//         * @param loc the locale for which this collator is to be created.
//         * @return the newly created collator.
//         * @stable ICU 3.2
//         */
//        public Collator createCollator(ULocale loc) {
//            return createCollator(loc.toLocale());
//        }
//
//        /**
//         * Return an instance of the appropriate collator.  If the locale
//         * is not supported, return null.
//         * <p><b>Note:</b> as of ICU4J 3.2, implementations should override
//         * createCollator(ULocale) instead of this method, and inherit this
//         * method's implementation.  This method is no longer abstract
//         * and instead delegates to createCollator(ULocale).
//         * @param loc the locale for which this collator is to be created.
//         * @return the newly created collator.
//         * @stable ICU 2.6
//         */
//         public Collator createCollator(Locale loc) {
//            return createCollator(ULocale.forLocale(loc));
//        }
//
//        /**
//         * Return the name of the collator for the objectLocale, localized for the displayLocale.
//         * If objectLocale is not visible or not defined by the factory, return null.
//         * @param objectLocale the locale identifying the collator
//         * @param displayLocale the locale for which the display name of the collator should be localized
//         * @return the display name
//         * @stable ICU 2.6
//         */
//        public String getDisplayName(Locale objectLocale, Locale displayLocale) {
//            return getDisplayName(ULocale.forLocale(objectLocale), ULocale.forLocale(displayLocale));
//        }
//
//        /**
//         * Return the name of the collator for the objectLocale, localized for the displayLocale.
//         * If objectLocale is not visible or not defined by the factory, return null.
//         * @param objectLocale the locale identifying the collator
//         * @param displayLocale the locale for which the display name of the collator should be localized
//         * @return the display name
//         * @stable ICU 3.2
//         */
//        public String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
//            if (visible()) {
//                Set<String> supported = getSupportedLocaleIDs();
//                String name = objectLocale.getBaseName();
//                if (supported.contains(name)) {
//                    return objectLocale.getDisplayName(displayLocale);
//                }
//            }
//            return null;
//        }
//
//        /**
//         * Return an unmodifiable collection of the locale names directly
//         * supported by this factory.
//         *
//         * @return the set of supported locale IDs.
//         * @stable ICU 2.6
//         */
//        public abstract Set<String> getSupportedLocaleIDs();
//
//        /**
//         * Empty default constructor.
//         * @stable ICU 2.6
//         */
//        protected CollatorFactory() {
//        }
//    }

    /**
     * {@icu} Returns the Collator for the desired locale.
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
        return getInstance(locale.toLocale());
    }

    /**
     * Returns the Collator for the desired locale.
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
        return new Collator(java.text.Collator.getInstance(locale));
    }

//    /**
//     * {@icu} Registers a collator as the default collator for the provided locale.  The
//     * collator should not be modified after it is registered.
//     *
//     * @param collator the collator to register
//     * @param locale the locale for which this is the default collator
//     * @return an object that can be used to unregister the registered collator.
//     *
//     * @stable ICU 3.2
//     */
//    public static final Object registerInstance(Collator collator, ULocale locale) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Registers a collator factory.
//     *
//     * @param factory the factory to register
//     * @return an object that can be used to unregister the registered factory.
//     *
//     * @stable ICU 2.6
//     */
//    public static final Object registerFactory(CollatorFactory factory) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Unregisters a collator previously registered using registerInstance.
//     * @param registryKey the object previously returned by registerInstance.
//     * @return true if the collator was successfully unregistered.
//     * @stable ICU 2.6
//     */
//    public static final boolean unregister(Object registryKey) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns the set of locales, as Locale objects, for which collators
     * are installed.  Note that Locale objects do not support RFC 3066.
     * @return the list of locales in which collators are installed.
     * This list includes any that have been registered, in addition to
     * those that are installed with ICU4J.
     * @stable ICU 2.4
     */
    public static Locale[] getAvailableLocales() {
        return java.text.Collator.getAvailableLocales();
    }

    /**
     * {@icu} Returns the set of locales, as ULocale objects, for which collators
     * are installed.  ULocale objects support RFC 3066.
     * @return the list of locales in which collators are installed.
     * This list includes any that have been registered, in addition to
     * those that are installed with ICU4J.
     * @stable ICU 3.0
     */
    public static final ULocale[] getAvailableULocales() {
        Locale[] locales = java.text.Collator.getAvailableLocales();
        ULocale[] ulocales = new ULocale[locales.length];
        for (int i = 0; i < locales.length; ++i) {
            ulocales[i] = ULocale.forLocale(locales[i]);
        }
        return ulocales;
    }

    /**
     * {@icu} Returns an array of all possible keywords that are relevant to
     * collation. At this point, the only recognized keyword for this
     * service is "collation".
     * @return an array of valid collation keywords.
     * @see #getKeywordValues
     * @stable ICU 3.0
     */
    public static final String[] getKeywords() {
        // No keywords support in com.ibm.icu.base
        return new String[0];
    }

    /**
     * {@icu} Given a keyword, returns an array of all values for
     * that keyword that are currently in use.
     * @param keyword one of the keywords returned by getKeywords.
     * @see #getKeywords
     * @stable ICU 3.0
     */
    public static final String[] getKeywordValues(String keyword) {
        // No keywords support in com.ibm.icu.base
        return new String[0];
    }

//    /**
//     * {@icu} Given a key and a locale, returns an array of string values in a preferred
//     * order that would make a difference. These are all and only those values where
//     * the open (creation) of the service with the locale formed from the input locale
//     * plus input keyword and that value has different behavior than creation with the
//     * input locale alone.
//     * @param key           one of the keys supported by this service.  For now, only
//     *                      "collation" is supported.
//     * @param locale        the locale
//     * @param commonlyUsed  if set to true it will return only commonly used values
//     *                      with the given locale in preferred order.  Otherwise,
//     *                      it will return all the available values for the locale.
//     * @return an array of string values for the given key and the locale.
//     * @stable ICU 4.2
//     */
//    public static final String[] getKeywordValuesForLocale(String key, ULocale locale, 
//                                                           boolean commonlyUsed) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the functionally equivalent locale for the given
//     * requested locale, with respect to given keyword, for the
//     * collation service.  If two locales return the same result, then
//     * collators instantiated for these locales will behave
//     * equivalently.  The converse is not always true; two collators
//     * may in fact be equivalent, but return different results, due to
//     * internal details.  The return result has no other meaning than
//     * that stated above, and implies nothing as to the relationship
//     * between the two locales.  This is intended for use by
//     * applications who wish to cache collators, or otherwise reuse
//     * collators when possible.  The functional equivalent may change
//     * over time.  For more information, please see the <a
//     * href="http://www.icu-project.org/userguide/locale.html#services">
//     * Locales and Services</a> section of the ICU User Guide.
//     * @param keyword a particular keyword as enumerated by
//     * getKeywords.
//     * @param locID The requested locale
//     * @param isAvailable If non-null, isAvailable[0] will receive and
//     * output boolean that indicates whether the requested locale was
//     * 'available' to the collation service. If non-null, isAvailable
//     * must have length >= 1.
//     * @return the locale
//     * @stable ICU 3.0
//     */
//    public static final ULocale getFunctionalEquivalent(String keyword,
//                                                        ULocale locID,
//                                                        boolean isAvailable[]) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the functionally equivalent locale for the given
//     * requested locale, with respect to given keyword, for the
//     * collation service.
//     * @param keyword a particular keyword as enumerated by
//     * getKeywords.
//     * @param locID The requested locale
//     * @return the locale
//     * @see #getFunctionalEquivalent(String,ULocale,boolean[])
//     * @stable ICU 3.0
//     */
//    public static final ULocale getFunctionalEquivalent(String keyword,
//                                                        ULocale locID) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the name of the collator for the objectLocale, localized for the
//     * displayLocale.
//     * @param objectLocale the locale of the collator
//     * @param displayLocale the locale for the collator's display name
//     * @return the display name
//     * @stable ICU 2.6
//     */
//    static public String getDisplayName(Locale objectLocale, Locale displayLocale) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the name of the collator for the objectLocale, localized for the
//     * displayLocale.
//     * @param objectLocale the locale of the collator
//     * @param displayLocale the locale for the collator's display name
//     * @return the display name
//     * @stable ICU 3.2
//     */
//    static public String getDisplayName(ULocale objectLocale, ULocale displayLocale) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the name of the collator for the objectLocale, localized for the
//     * current locale.
//     * @param objectLocale the locale of the collator
//     * @return the display name
//     * @stable ICU 2.6
//     */
//    static public String getDisplayName(Locale objectLocale) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the name of the collator for the objectLocale, localized for the
//     * current locale.
//     * @param objectLocale the locale of the collator
//     * @return the display name
//     * @stable ICU 3.2
//     */
//    static public String getDisplayName(ULocale objectLocale) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns this Collator's strength property. The strength property
     * determines the minimum level of difference considered significant.
     * </p>
     * {@icunote} This can return QUATERNARY strength, which is not supported by the
     * JDK version.
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
        return collator.getStrength();
    }

    /**
     * Returns the decomposition mode of this Collator. The decomposition mode
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
        return collator.getDecomposition();
    }

    // public other methods -------------------------------------------------

    /**
     * Compares the equality of two text Strings using
     * this Collator's rules, strength and decomposition mode.  Convenience method.
     * @param source the source string to be compared.
     * @param target the target string to be compared.
     * @return true if the strings are equal according to the collation
     *         rules, otherwise false.
     * @see #compare
     * @throws NullPointerException thrown if either arguments is null.
     * @stable ICU 2.8
     */
    public boolean equals(String source, String target)
    {
        return (compare(source, target) == 0);
    }

//    /**
//     * {@icu} Returns a UnicodeSet that contains all the characters and sequences tailored
//     * in this collator.
//     * @return a pointer to a UnicodeSet object containing all the
//     *         code points and sequences that may sort differently than
//     *         in the UCA.
//     * @stable ICU 2.4
//     */
//    public UnicodeSet getTailoredSet()
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
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
     * @throws NullPointerException thrown if either argument is null.
     * @stable ICU 2.8
     */
    public int compare(String source, String target) {
        return collator.compare(source, target);
    }

    /**
     * Compares the source Object to the target Object.
     * </p>
     * @param source the source Object.
     * @param target the target Object.
     * @return Returns an integer value. Value is less than zero if source is
     *         less than target, value is zero if source and target are equal,
     *         value is greater than zero if source is greater than target.
     * @throws ClassCastException thrown if either arguments cannot be cast to String.
     * @stable ICU 4.2
     */
    public int compare(Object source, Object target) {
        return compare((String)source, (String)target);
    }

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
    public CollationKey getCollationKey(String source) {
        return new CollationKey(collator.getCollationKey(source));
    }

//    /**
//     * {@icu} Returns the simpler form of a CollationKey for the String source following
//     * the rules of this Collator and stores the result into the user provided argument
//     * key.  If key has a internal byte array of length that's too small for the result,
//     * the internal byte array will be grown to the exact required size.
//     * @param source the text String to be transformed into a RawCollationKey
//     * @return If key is null, a new instance of RawCollationKey will be
//     *         created and returned, otherwise the user provided key will be
//     *         returned.
//     * @see #compare(String, String)
//     * @see #getCollationKey
//     * @see RawCollationKey
//     * @stable ICU 2.8
//     */
//    public RawCollationKey getRawCollationKey(String source, RawCollationKey key) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Variable top is a two byte primary value which causes all the codepoints
//     * with primary values that are less or equal than the variable top to be
//     * shifted when alternate handling is set to SHIFTED.
//     * </p>
//     * <p>
//     * Sets the variable top to a collation element value of a string supplied.
//     * </p>
//     * @param varTop one or more (if contraction) characters to which the
//     *               variable top should be set
//     * @return a int value containing the value of the variable top in upper 16
//     *         bits. Lower 16 bits are undefined.
//     * @throws IllegalArgumentException is thrown if varTop argument is not
//     *            a valid variable top element. A variable top element is
//     *            invalid when it is a contraction that does not exist in the
//     *            Collation order or when the PRIMARY strength collation
//     *            element for the variable top has more than two bytes
//     * @see #getVariableTop
//     * @see RuleBasedCollator#setAlternateHandlingShifted
//     * @stable ICU 2.6
//     */
//    public int setVariableTop(String varTop) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the variable top value of a Collator.
//     * Lower 16 bits are undefined and should be ignored.
//     * @return the variable top value of a Collator.
//     * @see #setVariableTop
//     * @stable ICU 2.6
//     */
//    public int getVariableTop() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the variable top to a collation element value supplied.
//     * Variable top is set to the upper 16 bits.
//     * Lower 16 bits are ignored.
//     * @param varTop Collation element value, as returned by setVariableTop or
//     *               getVariableTop
//     * @see #getVariableTop
//     * @see #setVariableTop
//     * @stable ICU 2.6
//     */
//    public void setVariableTop(int varTop) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the version of this collator object.
//     * @return the version object associated with this collator
//     * @stable ICU 2.8
//     */
//    public VersionInfo getVersion() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the UCA version of this collator object.
//     * @return the version object associated with this collator
//     * @stable ICU 2.8
//     */
//    public VersionInfo getUCAVersion() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**  
//     * Retrieves the reordering codes for this collator.
//     * These reordering codes are a combination of UScript codes and ReorderCodes.
//     * @return a copy of the reordering codes for this collator; 
//     * if none are set then returns an empty array
//     * @see #setReorderCodes
//     * @see #getEquivalentReorderCodes
//     * @draft ICU 4.8
//     */ 
//    public int[] getReorderCodes() 
//    { 
//        throw new UnsupportedOperationException(); 
//    }   

//    /** 
//     * Sets the reordering codes for this collator.
//     * Reordering codes allow the collation ordering for groups of characters to be changed.
//     * The reordering codes are a combination of UScript  codes and ReorderCodes.
//     * These allow the ordering of characters belonging to these groups to be changed as a group.  
//     * @param order the reordering codes to apply to this collator; if this is null or an empty array
//     * then this clears any existing reordering
//     * @see #getReorderCodes
//     * @see #getEquivalentReorderCodes
//     * @draft ICU 4.8
//     */ 
//    public void setReorderCodes(int... order) 
//    { 
//        throw new UnsupportedOperationException(); 
//    } 

//    /**
//     * Retrieves all the reorder codes that are grouped with the given reorder code. Some reorder
//     * codes are grouped and must reorder together.
//     * 
//     * @param reorderCode code for which equivalents to be retrieved
//     * @return the set of all reorder codes in the same group as the given reorder code.
//     * @see #setReorderCodes
//     * @see #getReorderCodes
//     * @draft ICU 4.8
//     */
//    public static int[] getEquivalentReorderCodes(int reorderCode)
//    { 
//        throw new UnsupportedOperationException(); 
//    }

//    /**
//     * {@icu} Returns the locale that was used to create this object, or null.
//     * This may may differ from the locale requested at the time of
//     * this object's creation.  For example, if an object is created
//     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
//     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
//     * <tt>en_US</tt> may be the most specific locale that exists (the
//     * <i>valid</i> locale).
//     *
//     * <p>Note: This method will be implemented in ICU 3.0; ICU 2.8
//     * contains a partial preview implementation.  The * <i>actual</i>
//     * locale is returned correctly, but the <i>valid</i> locale is
//     * not, in most cases.
//     * @param type type of information requested, either {@link
//     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
//     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
//     * @return the information specified by <i>type</i>, or null if
//     * this object was not constructed from locale data.
//     * @see com.ibm.icu.util.ULocale
//     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
//     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
//     * @draft ICU 2.8 (retain)
//     * @provisional This API might change or be removed in a future release.
//     */
//    public final ULocale getLocale(ULocale.Type type) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    // com.ibm.icu.base specific overrides
    public String toString() {
        return collator.toString();
    }

    public boolean equals(Object rhs) {
        try {
            return collator.equals(((Collator)rhs).collator);
        }
        catch (Exception e) {
            return false;
        }
    }

    public int hashCode() {
        return collator.hashCode();
    }
}
