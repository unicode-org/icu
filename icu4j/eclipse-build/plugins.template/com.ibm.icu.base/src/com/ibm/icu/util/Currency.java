// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2001-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Locale;

import com.ibm.icu.util.ULocale.Category;

/**
 * A class encapsulating a currency, as defined by ISO 4217.  A
 * <tt>Currency</tt> object can be created given a <tt>Locale</tt> or
 * given an ISO 4217 code.  Once created, the <tt>Currency</tt> object
 * can return various data necessary to its proper display:
 *
 * <ul><li>A display symbol, for a specific locale
 * <li>The number of fraction digits to display
 * <li>A rounding increment
 * </ul>
 *
 * The <tt>DecimalFormat</tt> class uses these data to display
 * currencies.
 *
 * <p>Note: This class deliberately resembles
 * <tt>java.util.Currency</tt> but it has a completely independent
 * implementation, and adds features not present in the JDK.
 * @author Alan Liu
 * @stable ICU 2.2
 */
public class Currency implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * @internal
     */
    public final java.util.Currency currency;

    /**
     * @internal
     * @param delegate the NumberFormat to which to delegate
     */
    public Currency(java.util.Currency delegate) {
        this.currency = delegate;
    }

    /**
     * Selector for getName() indicating a symbolic name for a
     * currency, such as "$" for USD.
     * @stable ICU 2.6
     */
    public static final int SYMBOL_NAME = 0;

    /**
     * Selector for ucurr_getName indicating the long name for a
     * currency, such as "US Dollar" for USD.
     * @stable ICU 2.6
     */
    public static final int LONG_NAME = 1;
   
    /**
     * Selector for getName() indicating the plural long name for a 
     * currency, such as "US dollar" for USD in "1 US dollar", 
     * and "US dollars" for USD in "2 US dollars".
     * @stable ICU 4.2
     */
    public static final int PLURAL_LONG_NAME = 2;

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     * @param locale the locale
     * @return the currency object for this locale
     * @stable ICU 2.2
     */
    public static Currency getInstance(Locale locale) {
        return new Currency(java.util.Currency.getInstance(locale));
    }

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     * @stable ICU 3.2
     */
    public static Currency getInstance(ULocale locale) {
        return new Currency(java.util.Currency.getInstance(locale.toLocale()));
    }

//    /**
//     * Returns an array of Strings which contain the currency
//     * identifiers that are valid for the given locale on the 
//     * given date.  If there are no such identifiers, returns null.
//     * Returned identifiers are in preference order.
//     * @param loc the locale for which to retrieve currency codes.
//     * @param d the date for which to retrieve currency codes for the given locale.
//     * @return The array of ISO currency codes.
//     * @stable ICU 4.0
//     */
//    public static String[] getAvailableCurrencyCodes(ULocale loc, Date d) {
//        throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

//    /**
//     * Returns the set of available currencies. The returned set of currencies contains all of the
//     * available currencies, including obsolete ones. The result set can be modified without
//     * affecting the available currencies in the runtime.
//     * 
//     * @return The set of available currencies. The returned set could be empty if there is no
//     * currency data available.
//     * 
//     * @stable ICU 49
//     */
//    public static Set<Currency> getAvailableCurrencies() {
//        throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

    /**
     * Returns a currency object given an ISO 4217 3-letter code.
     * @param theISOCode the iso code
     * @return the currency for this iso code
     * @throws NullPointerException if <code>theISOCode</code> is null.
     * @throws IllegalArgumentException if <code>theISOCode</code> is not a
     *         3-letter alpha code.
     * @stable ICU 2.2
     */
    public static Currency getInstance(String theISOCode) {
        return new Currency(java.util.Currency.getInstance(theISOCode));
    }

//    /**
//     * Registers a new currency for the provided locale.  The returned object
//     * is a key that can be used to unregister this currency object.
//     * @param currency the currency to register
//     * @param locale the ulocale under which to register the currency
//     * @return a registry key that can be used to unregister this currency
//     * @see #unregister
//     * @stable ICU 3.2
//     */
//    public static Object registerInstance(Currency currency, ULocale locale) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Unregister the currency associated with this key (obtained from
//     * registerInstance).
//     * @param registryKey the registry key returned from registerInstance
//     * @see #registerInstance
//     * @stable ICU 2.6
//     */
//    public static boolean unregister(Object registryKey) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Return an array of the locales for which a currency
//     * is defined.
//     * @return an array of the available locales
//     * @stable ICU 2.2
//     */
//    public static Locale[] getAvailableLocales() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Return an array of the ulocales for which a currency
//     * is defined.
//     * @return an array of the available ulocales
//     * @stable ICU 3.2
//     */
//    public static ULocale[] getAvailableULocales() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Given a key and a locale, returns an array of values for the key for which data
//     * exists.  If commonlyUsed is true, these are the values that typically are used
//     * with this locale, otherwise these are all values for which data exists.  
//     * This is a common service API.
//     * <p>
//     * The only supported key is "currency", other values return an empty array.
//     * <p>
//     * Currency information is based on the region of the locale.  If the locale does not
//     * indicate a region, {@link ULocale#addLikelySubtags(ULocale)} is used to infer a region,
//     * except for the 'und' locale.
//     * <p>
//     * If commonlyUsed is true, only the currencies known to be in use as of the current date
//     * are returned.  When there are more than one, these are returned in preference order
//     * (typically, this occurs when a country is transitioning to a new currency, and the
//     * newer currency is preferred), see 
//     * <a href="http://unicode.org/reports/tr35/#Supplemental_Currency_Data">Unicode TR#35 Sec. C1</a>.  
//     * If commonlyUsed is false, all currencies ever used in any locale are returned, in no
//     * particular order.
//     * 
//     * @param key           key whose values to look up.  the only recognized key is "currency"
//     * @param locale        the locale
//     * @param commonlyUsed  if true, return only values that are currently used in the locale.
//     *                      Otherwise returns all values.
//     * @return an array of values for the given key and the locale.  If there is no data, the
//     *   array will be empty.
//     * @stable ICU 4.2
//     */
//    public static final String[] getKeywordValuesForLocale(String key, ULocale locale, 
//            boolean commonlyUsed) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Return a hashcode for this currency.
     * @stable ICU 2.2
     */
    public int hashCode() {
        return currency.hashCode();
    }

    /**
     * Return true if rhs is a Currency instance,
     * is non-null, and has the same currency code.
     * @stable ICU 2.2
     */
    public boolean equals(Object rhs) {
        try {
            return currency.equals(((Currency)rhs).currency);
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the ISO 4217 3-letter code for this currency object.
     * @stable ICU 2.2
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

//    /**
//     * Returns the ISO 4217 numeric code for this currency object.
//     * <p>Note: If the ISO 4217 numeric code is not assigned for the currency or
//     * the currency is unknown, this method returns 0.</p>
//     * @return The ISO 4217 numeric code of this currency.
//     * @stable ICU 49
//     */
//    public int getNumericCode() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Convenience and compatibility override of getName that
     * requests the symbol name.
     * @see #getName
     * @stable ICU 3.4
     */
    public String getSymbol() {
        return currency.getSymbol(ULocale.getDefault(Category.DISPLAY).toLocale());
    }

    /**
     * Convenience and compatibility override of getName that
     * requests the symbol name.
     * @param loc the Locale for the symbol
     * @see #getName
     * @stable ICU 3.4
     */
    public String getSymbol(Locale loc) {
        return currency.getSymbol(loc);
    }

    /**
     * Convenience and compatibility override of getName that
     * requests the symbol name.
     * @param uloc the ULocale for the symbol
     * @see #getName
     * @stable ICU 3.4
     */
    public String getSymbol(ULocale uloc) {
        return currency.getSymbol(uloc.toLocale());
    }

//    /**
//     * Returns the display name for the given currency in the
//     * given locale.  
//     * This is a convenient method for 
//     * getName(ULocale, int, boolean[]); 
//     * @stable ICU 3.2
//     */
//    public String getName(Locale locale,
//                          int nameStyle,
//                          boolean[] isChoiceFormat) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the display name for the given currency in the
//     * given locale.  For example, the display name for the USD
//     * currency object in the en_US locale is "$".
//     * @param locale locale in which to display currency
//     * @param nameStyle selector for which kind of name to return.
//     *                  The nameStyle should be either SYMBOL_NAME or 
//     *                  LONG_NAME. Otherwise, throw IllegalArgumentException.
//     * @param isChoiceFormat fill-in; isChoiceFormat[0] is set to true
//     * if the returned value is a ChoiceFormat pattern; otherwise it
//     * is set to false
//     * @return display string for this currency.  If the resource data
//     * contains no entry for this currency, then the ISO 4217 code is
//     * returned.  If isChoiceFormat[0] is true, then the result is a
//     * ChoiceFormat pattern.  Otherwise it is a static string. <b>Note:</b>
//     * as of ICU 4.4, choice formats are not used, and the value returned
//     * in isChoiceFormat is always false.
//     * <p>
//     * @throws  IllegalArgumentException  if the nameStyle is not SYMBOL_NAME
//     *                                    or LONG_NAME.
//     * @see #getName(ULocale, int, String, boolean[])
//     * @stable ICU 3.2
//     */
//    public String getName(ULocale locale, int nameStyle, boolean[] isChoiceFormat) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the display name for the given currency in the given locale.  
//     * This is a convenience overload of getName(ULocale, int, String, boolean[]);
//     * @stable ICU 4.2
//     */
//    public String getName(Locale locale, int nameStyle, String pluralCount,
//            boolean[] isChoiceFormat) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the display name for this currency in the default locale.
//     * If the resource data for the default locale contains no entry for this currency,
//     * then the ISO 4217 code is returned.
//     * <p>
//     * Note: This method was added for JDK compatibility support and equivalent to
//     * <code>getName(Locale.getDefault(), LONG_NAME, null)</code>.
//     * 
//     * @return The display name of this currency
//     * @see #getDisplayName(Locale)
//     * @see #getName(Locale, int, boolean[])
//     * @stable ICU 49
//     */
//    public String getDisplayName() {
//        //return getName(Locale.getDefault(), LONG_NAME, null);
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the display name for this currency in the given locale.
//     * If the resource data for the given locale contains no entry for this currency,
//     * then the ISO 4217 code is returned.
//     * <p>
//     * Note: This method was added for JDK compatibility support and equivalent to
//     * <code>getName(locale, LONG_NAME, null)</code>.
//     * 
//     * @param locale locale in which to display currency
//     * @return The display name of this currency for the specified locale
//     * @see #getDisplayName(Locale)
//     * @see #getName(Locale, int, boolean[])
//     * @stable ICU 49
//     */
//    public String getDisplayName(Locale locale) {
//        //return getName(locale, LONG_NAME, null);
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the display name for the given currency in the
//     * given locale.  For example, the SYMBOL_NAME for the USD
//     * currency object in the en_US locale is "$".
//     * The PLURAL_LONG_NAME for the USD currency object when the currency 
//     * amount is plural is "US dollars", such as in "3.00 US dollars";
//     * while the PLURAL_LONG_NAME for the USD currency object when the currency
//     * amount is singular is "US dollar", such as in "1.00 US dollar".
//     * @param locale locale in which to display currency
//     * @param nameStyle selector for which kind of name to return
//     * @param pluralCount plural count string for this locale
//     * @param isChoiceFormat fill-in; isChoiceFormat[0] is set to true
//     * if the returned value is a ChoiceFormat pattern; otherwise it
//     * is set to false
//     * @return display string for this currency.  If the resource data
//     * contains no entry for this currency, then the ISO 4217 code is
//     * returned.  If isChoiceFormat[0] is true, then the result is a
//     * ChoiceFormat pattern.  Otherwise it is a static string. <b>Note:</b>
//     * as of ICU 4.4, choice formats are not used, and the value returned
//     * in isChoiceFormat is always false.
//     * @throws  IllegalArgumentException  if the nameStyle is not SYMBOL_NAME,
//     *                                    LONG_NAME, or PLURAL_LONG_NAME.
//     * @stable ICU 4.2
//     */
//    public String getName(ULocale locale, int nameStyle, String pluralCount,
//            boolean[] isChoiceFormat) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Attempt to parse the given string as a currency, either as a
//     * display name in the given locale, or as a 3-letter ISO 4217
//     * code.  If multiple display names match, then the longest one is
//     * selected.  If both a display name and a 3-letter ISO code
//     * match, then the display name is preferred, unless it's length
//     * is less than 3.
//     *
//     * @param locale the locale of the display names to match
//     * @param text the text to parse
//     * @param type parse against currency type: LONG_NAME only or not
//     * @param pos input-output position; on input, the position within
//     * text to match; must have 0 <= pos.getIndex() < text.length();
//     * on output, the position after the last matched character. If
//     * the parse fails, the position in unchanged upon output.
//     * @return the ISO 4217 code, as a string, of the best match, or
//     * null if there is no match
//     *
//     * @internal
//     * @deprecated This API is ICU internal only.
//     */
//    public static String parse(ULocale locale, String text, int type, ParsePosition pos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns the number of the number of fraction digits that should
     * be displayed for this currency.
     * @return a non-negative number of fraction digits to be
     * displayed
     * @stable ICU 2.2
     */
    public int getDefaultFractionDigits() {
        return currency.getDefaultFractionDigits();
    }

//    /**
//     * Returns the rounding increment for this currency, or 0.0 if no
//     * rounding is done by this currency.
//     * @return the non-negative rounding increment, or 0.0 if none
//     * @stable ICU 2.2
//     */
//    public double getRoundingIncrement() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns the ISO 4217 code for this currency.
     * @stable ICU 2.2
     */
    public String toString() {
        return currency.toString();
    }

//    /**
//     * Return the locale that was used to create this object, or null.
//     * This may may differ from the locale requested at the time of
//     * this object's creation.  For example, if an object is created
//     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
//     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
//     * <tt>en_US</tt> may be the most specific locale that exists (the
//     * <i>valid</i> locale).
//     *
//     * <p>Note: This method will be obsoleted.  The implementation is
//     * no longer locale-specific and so there is no longer a valid or
//     * actual locale associated with the Currency object.  Until
//     * it is removed, this method will return the root locale.
//     * @param type type of information requested, either {@link
//     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
//     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
//     * @return the information specified by <i>type</i>, or null if
//     * this object was not constructed from locale data.
//     * @see com.ibm.icu.util.ULocale
//     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
//     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
//     * @obsolete ICU 3.2 to be removed
//     * @deprecated This API is obsolete.
//     */
//    public final ULocale getLocale(ULocale.Type type) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Queries if the given ISO 4217 3-letter code is available on the specified date range.
//     * <p>
//     * Note: For checking availability of a currency on a specific date, specify the date on both <code>from</code> and
//     * <code>to</code>. When both <code>from</code> and <code>to</code> are null, this method checks if the specified
//     * currency is available all time.
//     * 
//     * @param code
//     *            The ISO 4217 3-letter code.
//     * @param from
//     *            The lower bound of the date range, inclusive. When <code>from</code> is null, check the availability
//     *            of the currency any date before <code>to</code>
//     * @param to
//     *            The upper bound of the date range, inclusive. When <code>to</code> is null, check the availability of
//     *            the currency any date after <code>from</code>
//     * @return true if the given ISO 4217 3-letter code is supported on the specified date range.
//     * @throws IllegalArgumentException when <code>to</code> is before <code>from</code>.
//     * 
//     * @draft ICU 4.6
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static boolean isAvailable(String code, Date from, Date to) {
//    	throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

}

//eof
