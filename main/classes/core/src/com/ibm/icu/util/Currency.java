/**
 *******************************************************************************
 * Copyright (C) 2001-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.Serializable;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUDebug;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.TextTrieMap;
import com.ibm.icu.text.CurrencyDisplayNames;
import com.ibm.icu.text.CurrencyMetaInfo;
import com.ibm.icu.text.CurrencyMetaInfo.CurrencyDigits;
import com.ibm.icu.text.CurrencyMetaInfo.CurrencyFilter;

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
public class Currency extends MeasureUnit implements Serializable {
    // using serialver from jdk1.4.2_05
    private static final long serialVersionUID = -5839973855554750484L;
    private static final boolean DEBUG = ICUDebug.enabled("currency");

    // Cache to save currency name trie
    private static ICUCache<ULocale, Vector<TextTrieMap<CurrencyStringInfo>>> CURRENCY_NAME_CACHE =
        new SimpleCache<ULocale, Vector<TextTrieMap<CurrencyStringInfo>>>();

    /**
     * ISO 4217 3-letter code.
     */
    private String isoCode;

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

    // begin registry stuff

    // shim for service code
    /* package */ static abstract class ServiceShim {
        abstract ULocale[] getAvailableULocales();
        abstract Locale[] getAvailableLocales();
        abstract Currency createInstance(ULocale l);
        abstract Object registerInstance(Currency c, ULocale l);
        abstract boolean unregister(Object f);
    }

    private static ServiceShim shim;
    private static ServiceShim getShim() {
        // Note: this instantiation is safe on loose-memory-model configurations
        // despite lack of synchronization, since the shim instance has no state--
        // it's all in the class init.  The worst problem is we might instantiate
        // two shim instances, but they'll share the same state so that's ok.
        if (shim == null) {
            try {
                Class<?> cls = Class.forName("com.ibm.icu.util.CurrencyServiceShim");
                shim = (ServiceShim)cls.newInstance();
            }
            catch (Exception e) {
                if(DEBUG){
                    e.printStackTrace();
                }
                throw new RuntimeException(e.getMessage());
            }
        }
        return shim;
    }

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     * @param locale the locale
     * @return the currency object for this locale
     * @stable ICU 2.2
     */
    public static Currency getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     * @stable ICU 3.2
     */
    public static Currency getInstance(ULocale locale) {
        String currency = locale.getKeywordValue("currency");
        if (currency != null) {
            return getInstance(currency);
        }

        if (shim == null) {
            return createCurrency(locale);
        }

        return shim.createInstance(locale);
    }

    /**
     * Returns an array of Strings which contain the currency
     * identifiers that are valid for the given locale on the 
     * given date.  If there are no such identifiers, returns null.
     * Returned identifiers are in preference order.
     * @param loc the locale for which to retrieve currency codes.
     * @param d the date for which to retrieve currency codes for the given locale.
     * @return The array of ISO currency codes.
     * @stable ICU 4.0
     */
    public static String[] getAvailableCurrencyCodes(ULocale loc, Date d) {
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        CurrencyFilter filter = CurrencyFilter.onDate(d).withRegion(loc.getCountry());
        List<String> list = info.currencies(filter);
        // Note: Prior to 4.4 the spec didn't say that we return null if there are no results, but 
        // the test assumed it did.  Kept the behavior and amended the spec.
        if (list.isEmpty()) {
            return null;
        }
        return list.toArray(new String[list.size()]);
    }

    private static final String EUR_STR = "EUR";
    
    /**
     * Instantiate a currency from resource data.
     */
    /* package */ static Currency createCurrency(ULocale loc) {
        String variant = loc.getVariant();
        if ("EURO".equals(variant)) {
            return new Currency(EUR_STR);
        }
        
        String country = loc.getCountry();
        
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        List<String> list = info.currencies(CurrencyFilter.onRegion(country));
        if (list.size() > 0) {
            String code = list.get(0);
            boolean isPreEuro = "PREEURO".equals(variant);
            if (isPreEuro && EUR_STR.equals(code)) {
                if (list.size() < 2) {
                    return null;
                }
                code = list.get(1);
            }
            return new Currency(code);
        }
        return null;
    }

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
        if (theISOCode == null) {
            throw new NullPointerException("The input currency code is null.");
        }
        boolean is3alpha = true;
        if (theISOCode.length() != 3) {
            is3alpha = false;
        } else {
            for (int i = 0; i < 3; i++) {
                char ch = theISOCode.charAt(i);
                if (ch < 'A' || (ch > 'Z' && ch < 'a') || ch > 'z') {
                    is3alpha = false;
                    break;
                }
            }
        }
        if (!is3alpha) {
            throw new IllegalArgumentException(
                    "The input currency code is not 3-letter alphabetic code.");
        }
        return new Currency(theISOCode.toUpperCase(Locale.US));
    }

    /**
     * Registers a new currency for the provided locale.  The returned object
     * is a key that can be used to unregister this currency object.
     * @param currency the currency to register
     * @param locale the ulocale under which to register the currency
     * @return a registry key that can be used to unregister this currency
     * @see #unregister
     * @stable ICU 3.2
     */
    public static Object registerInstance(Currency currency, ULocale locale) {
        return getShim().registerInstance(currency, locale);
    }

    /**
     * Unregister the currency associated with this key (obtained from
     * registerInstance).
     * @param registryKey the registry key returned from registerInstance
     * @see #registerInstance
     * @stable ICU 2.6
     */
    public static boolean unregister(Object registryKey) {
        if (registryKey == null) {
            throw new IllegalArgumentException("registryKey must not be null");
        }
        if (shim == null) {
            return false;
        }
        return shim.unregister(registryKey);
    }

    /**
     * Return an array of the locales for which a currency
     * is defined.
     * @return an array of the available locales
     * @stable ICU 2.2
     */
    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableLocales();
        } else {
            return shim.getAvailableLocales();
        }
    }

    /**
     * Return an array of the ulocales for which a currency
     * is defined.
     * @return an array of the available ulocales
     * @stable ICU 3.2
     */
    public static ULocale[] getAvailableULocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableULocales();
        } else {
            return shim.getAvailableULocales();
        }
    }

    // end registry stuff

    /**
     * Given a key and a locale, returns an array of values for the key for which data
     * exists.  If commonlyUsed is true, these are the values that typically are used
     * with this locale, otherwise these are all values for which data exists.  
     * This is a common service API.
     * <p>
     * The only supported key is "currency", other values return an empty array.
     * <p>
     * Currency information is based on the region of the locale.  If the locale does not
     * indicate a region, {@link ULocale#addLikelySubtags(ULocale)} is used to infer a region,
     * except for the 'und' locale.
     * <p>
     * If commonlyUsed is true, only the currencies known to be in use as of the current date
     * are returned.  When there are more than one, these are returned in preference order
     * (typically, this occurs when a country is transitioning to a new currency, and the
     * newer currency is preferred), see 
     * <a href="http://unicode.org/reports/tr35/#Supplemental_Currency_Data">Unicode TR#35 Sec. C1</a>.  
     * If commonlyUsed is false, all currencies ever used in any locale are returned, in no
     * particular order.
     * 
     * @param key           key whose values to look up.  the only recognized key is "currency"
     * @param locale        the locale
     * @param commonlyUsed  if true, return only values that are currently used in the locale.
     *                      Otherwise returns all values.
     * @return an array of values for the given key and the locale.  If there is no data, the
     *   array will be empty.
     * @stable ICU 4.2
     */
    public static final String[] getKeywordValuesForLocale(String key, ULocale locale, 
            boolean commonlyUsed) {
        
        // The only keyword we recognize is 'currency'
        if (!"currency".equals(key)) {
            return EMPTY_STRING_ARRAY;
        }
        
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        if (!commonlyUsed) {
            // Behavior change from 4.3.3, no longer sort the currencies
            List<String> result = info.currencies(null);
            return result.toArray(new String[result.size()]);
        }
        
        // Don't resolve region if the requested locale is 'und', it will resolve to US
        // which we don't want.
        String prefRegion = locale.getCountry();
        if (prefRegion.length() == 0) {
            if (UND.equals(locale)) {
                return EMPTY_STRING_ARRAY;
            }
            ULocale loc = ULocale.addLikelySubtags(locale);
            prefRegion = loc.getCountry();
       }

        CurrencyFilter filter = CurrencyFilter.now().withRegion(prefRegion);
        
        // currencies are in region's preferred order when we're filtering on region, which
        // matches our spec
        List<String> result = info.currencies(filter);
        
        // No fallback anymore (change from 4.3.3)
        if (result.size() == 0) {
            return EMPTY_STRING_ARRAY;
        }

        return result.toArray(new String[result.size()]);
    }
    
    private static final ULocale UND = new ULocale("und");
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Return a hashcode for this currency.
     * @stable ICU 2.2
     */
    public int hashCode() {
        return isoCode.hashCode();
    }

    /**
     * Return true if rhs is a Currency instance,
     * is non-null, and has the same currency code.
     * @stable ICU 2.2
     */
    public boolean equals(Object rhs) {
        if (rhs == null) return false;
        if (rhs == this) return true;
        try {
            Currency c = (Currency) rhs;
            return isoCode.equals(c.isoCode);
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns the ISO 4217 3-letter code for this currency object.
     * @stable ICU 2.2
     */
    public String getCurrencyCode() {
        return isoCode;
    }

    /**
     * Convenience and compatibility override of getName that
     * requests the symbol name.
     * @see #getName
     * @stable ICU 3.4
     */
    public String getSymbol() {
        return getSymbol(ULocale.getDefault());
    }

    /**
     * Convenience and compatibility override of getName that
     * requests the symbol name.
     * @param loc the Locale for the symbol
     * @see #getName
     * @stable ICU 3.4
     */
    public String getSymbol(Locale loc) {
        return getSymbol(ULocale.forLocale(loc));
    }

    /**
     * Convenience and compatibility override of getName that
     * requests the symbol name.
     * @param uloc the ULocale for the symbol
     * @see #getName
     * @stable ICU 3.4
     */
    public String getSymbol(ULocale uloc) {
        return getName(uloc, SYMBOL_NAME, new boolean[1]);
    }

    /**
     * Returns the display name for the given currency in the
     * given locale.  
     * This is a convenient method for 
     * getName(ULocale, int, boolean[]); 
     * @stable ICU 3.2
     */
    public String getName(Locale locale,
                          int nameStyle,
                          boolean[] isChoiceFormat) {
        return getName(ULocale.forLocale(locale), nameStyle, isChoiceFormat);
    }

    /**
     * Returns the display name for the given currency in the
     * given locale.  For example, the display name for the USD
     * currency object in the en_US locale is "$".
     * @param locale locale in which to display currency
     * @param nameStyle selector for which kind of name to return.
     *                  The nameStyle should be either SYMBOL_NAME or 
     *                  LONG_NAME. Otherwise, throw IllegalArgumentException.
     * @param isChoiceFormat fill-in; isChoiceFormat[0] is set to true
     * if the returned value is a ChoiceFormat pattern; otherwise it
     * is set to false
     * @return display string for this currency.  If the resource data
     * contains no entry for this currency, then the ISO 4217 code is
     * returned.  If isChoiceFormat[0] is true, then the result is a
     * ChoiceFormat pattern.  Otherwise it is a static string. <b>Note:</b>
     * as of ICU 4.4, choice formats are not used, and the value returned
     * in isChoiceFormat is always false.
     * <p>
     * @throws  IllegalArgumentException  if the nameStyle is not SYMBOL_NAME
     *                                    or LONG_NAME.
     * @see #getName(ULocale, int, String, boolean[])
     * @stable ICU 3.2
     */
    public String getName(ULocale locale, int nameStyle, boolean[] isChoiceFormat) {
        if (!(nameStyle == SYMBOL_NAME || nameStyle == LONG_NAME)) {
            throw new IllegalArgumentException("bad name style: " + nameStyle);
        }

        // We no longer support choice format data in names.  Data should not contain
        // choice patterns.
        if (isChoiceFormat != null) {
            isChoiceFormat[0] = false;
        }

        CurrencyDisplayNames names = CurrencyDisplayNames.getInstance(locale);
        return nameStyle == SYMBOL_NAME ? names.getSymbol(isoCode) : names.getName(isoCode);
    }

    /**
     * Returns the display name for the given currency in the given locale.  
     * This is a convenience overload of getName(ULocale, int, String, boolean[]);
     * @stable ICU 4.2
     */
    public String getName(Locale locale, int nameStyle, String pluralCount,
            boolean[] isChoiceFormat) {
        return getName(ULocale.forLocale(locale), nameStyle, pluralCount, isChoiceFormat);
    }

    /**
     * Returns the display name for the given currency in the
     * given locale.  For example, the SYMBOL_NAME for the USD
     * currency object in the en_US locale is "$".
     * The PLURAL_LONG_NAME for the USD currency object when the currency 
     * amount is plural is "US dollars", such as in "3.00 US dollars";
     * while the PLURAL_LONG_NAME for the USD currency object when the currency
     * amount is singular is "US dollar", such as in "1.00 US dollar".
     * @param locale locale in which to display currency
     * @param nameStyle selector for which kind of name to return
     * @param pluralCount plural count string for this locale
     * @param isChoiceFormat fill-in; isChoiceFormat[0] is set to true
     * if the returned value is a ChoiceFormat pattern; otherwise it
     * is set to false
     * @return display string for this currency.  If the resource data
     * contains no entry for this currency, then the ISO 4217 code is
     * returned.  If isChoiceFormat[0] is true, then the result is a
     * ChoiceFormat pattern.  Otherwise it is a static string. <b>Note:</b>
     * as of ICU 4.4, choice formats are not used, and the value returned
     * in isChoiceFormat is always false.
     * @throws  IllegalArgumentException  if the nameStyle is not SYMBOL_NAME,
     *                                    LONG_NAME, or PLURAL_LONG_NAME.
     * @stable ICU 4.2
     */
    public String getName(ULocale locale, int nameStyle, String pluralCount,
            boolean[] isChoiceFormat) {
        if (nameStyle != PLURAL_LONG_NAME) {
            return getName(locale, nameStyle, isChoiceFormat);
        }

        // We no longer support choice format
        if (isChoiceFormat != null) {
            isChoiceFormat[0] = false;
        }
        
        CurrencyDisplayNames names = CurrencyDisplayNames.getInstance(locale);
        return names.getPluralName(isoCode, pluralCount);
    }

    /**
     * Attempt to parse the given string as a currency, either as a
     * display name in the given locale, or as a 3-letter ISO 4217
     * code.  If multiple display names match, then the longest one is
     * selected.  If both a display name and a 3-letter ISO code
     * match, then the display name is preferred, unless it's length
     * is less than 3.
     *
     * @param locale the locale of the display names to match
     * @param text the text to parse
     * @param type parse against currency type: LONG_NAME only or not
     * @param pos input-output position; on input, the position within
     * text to match; must have 0 <= pos.getIndex() < text.length();
     * on output, the position after the last matched character. If
     * the parse fails, the position in unchanged upon output.
     * @return the ISO 4217 code, as a string, of the best match, or
     * null if there is no match
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static String parse(ULocale locale, String text, int type, ParsePosition pos) {
        Vector<TextTrieMap<CurrencyStringInfo>> currencyTrieVec = CURRENCY_NAME_CACHE.get(locale);
        if (currencyTrieVec == null) {
            TextTrieMap<CurrencyStringInfo> currencyNameTrie = 
                new TextTrieMap<CurrencyStringInfo>(true);
            TextTrieMap<CurrencyStringInfo> currencySymbolTrie = 
                new TextTrieMap<CurrencyStringInfo>(false);
            currencyTrieVec = new Vector<TextTrieMap<CurrencyStringInfo>>();
            currencyTrieVec.addElement(currencySymbolTrie);
            currencyTrieVec.addElement(currencyNameTrie);
            setupCurrencyTrieVec(locale, currencyTrieVec);
            CURRENCY_NAME_CACHE.put(locale, currencyTrieVec);
        }
        
        int maxLength = 0;
        String isoResult = null;

          // look for the names
        TextTrieMap<CurrencyStringInfo> currencyNameTrie = currencyTrieVec.elementAt(1);
        CurrencyNameResultHandler handler = new CurrencyNameResultHandler();
        currencyNameTrie.find(text, pos.getIndex(), handler);
        List<CurrencyStringInfo> list = handler.getMatchedCurrencyNames();
        if (list != null && list.size() != 0) {
            for (CurrencyStringInfo info : list) {
                String isoCode = info.getISOCode();
                String currencyString = info.getCurrencyString();
                if (currencyString.length() > maxLength) {
                    maxLength = currencyString.length();
                    isoResult = isoCode;
                }
            }
        }

        if (type != Currency.LONG_NAME) {  // not long name only
            TextTrieMap<CurrencyStringInfo> currencySymbolTrie = currencyTrieVec.elementAt(0);
            handler = new CurrencyNameResultHandler();
            currencySymbolTrie.find(text, pos.getIndex(), handler);
            list = handler.getMatchedCurrencyNames();
            if (list != null && list.size() != 0) {
                for (CurrencyStringInfo info : list) {
                    String isoCode = info.getISOCode();
                    String currencyString = info.getCurrencyString();
                    if (currencyString.length() > maxLength) {
                        maxLength = currencyString.length();
                        isoResult = isoCode;
                    }
                }
            }
        }

        int start = pos.getIndex();
        pos.setIndex(start + maxLength);
        return isoResult;
    }

    private static void setupCurrencyTrieVec(ULocale locale, 
            Vector<TextTrieMap<CurrencyStringInfo>> trieVec) {

        TextTrieMap<CurrencyStringInfo> symTrie = trieVec.elementAt(0);
        TextTrieMap<CurrencyStringInfo> trie = trieVec.elementAt(1);

        CurrencyDisplayNames names = CurrencyDisplayNames.getInstance(locale);
        for (Map.Entry<String, String> e : names.symbolMap().entrySet()) {
            String symbol = e.getKey();
            String isoCode = e.getValue();
            symTrie.put(symbol, new CurrencyStringInfo(isoCode, symbol));
        }
        for (Map.Entry<String, String> e : names.nameMap().entrySet()) {
            String name = e.getKey();
            String isoCode = e.getValue();
            trie.put(name, new CurrencyStringInfo(isoCode, name));
        }
    }

    private static final class CurrencyStringInfo {
        private String isoCode;
        private String currencyString;

        public CurrencyStringInfo(String isoCode, String currencyString) {
            this.isoCode = isoCode;
            this.currencyString = currencyString;
        }

        private String getISOCode() {
            return isoCode;
        }

        private String getCurrencyString() {
            return currencyString;
        }
    }

    private static class CurrencyNameResultHandler 
            implements TextTrieMap.ResultHandler<CurrencyStringInfo> {
        private ArrayList<CurrencyStringInfo> resultList;
    
        public boolean handlePrefixMatch(int matchLength, Iterator<CurrencyStringInfo> values) {
            if (resultList == null) {
                resultList = new ArrayList<CurrencyStringInfo>();
            }
            while (values.hasNext()) {
                CurrencyStringInfo item = values.next();
                if (item == null) {
                    break;
                }
                int i = 0;
                for (; i < resultList.size(); i++) {
                    CurrencyStringInfo tmp = resultList.get(i);
                    if (item.getISOCode() == tmp.getISOCode()) {
                        if (matchLength > tmp.getCurrencyString().length()) {
                            resultList.set(i, item);
                        }
                        break;
                    }
                }
                if (i == resultList.size()) {
                    // not found in the current list
                    resultList.add(item);
                }
            }
            return true;
        }

        List<CurrencyStringInfo> getMatchedCurrencyNames() {
            if (resultList == null || resultList.size() == 0) {
                return null;
            }
            return resultList;
        }
    }

    /**
     * Returns the number of the number of fraction digits that should
     * be displayed for this currency.
     * @return a non-negative number of fraction digits to be
     * displayed
     * @stable ICU 2.2
     */
    public int getDefaultFractionDigits() {
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        CurrencyDigits digits = info.currencyDigits(isoCode);
        return digits.fractionDigits;
    }

    /**
     * Returns the rounding increment for this currency, or 0.0 if no
     * rounding is done by this currency.
     * @return the non-negative rounding increment, or 0.0 if none
     * @stable ICU 2.2
     */
    public double getRoundingIncrement() {
        CurrencyMetaInfo info = CurrencyMetaInfo.getInstance();
        CurrencyDigits digits = info.currencyDigits(isoCode);

        int data1 = digits.roundingIncrement;

        // If there is no rounding return 0.0 to indicate no rounding.
        // This is the high-runner case, by far.
        if (data1 == 0) {
            return 0.0;
        }

        int data0 = digits.fractionDigits;

        // If the meta data is invalid, return 0.0 to indicate no rounding.
        if (data0 < 0 || data0 >= POW10.length) {
            return 0.0;
        }

        // Return data[1] / 10^(data[0]).  The only actual rounding data,
        // as of this writing, is CHF { 2, 25 }.
        return (double) data1 / POW10[data0];
    }

    /**
     * Returns the ISO 4217 code for this currency.
     * @stable ICU 2.2
     */
    public String toString() {
        return isoCode;
    }

    /**
     * Constructs a currency object for the given ISO 4217 3-letter
     * code.  This constructor assumes that the code is valid.
     * 
     * @param theISOCode The iso code used to construct the currency.
     * @stable ICU 3.4
     */
    protected Currency(String theISOCode) {
        isoCode = theISOCode;
    }

    // POW10[i] = 10^i
    private static final int[] POW10 = { 
        1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000 
    };

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
     * <p>Note: This method will be obsoleted.  The implementation is
     * no longer locale-specific and so there is no longer a valid or
     * actual locale associated with the Currency object.  Until
     * it is removed, this method will return the root locale.
     * @param type type of information requested, either {@link
     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
     * @return the information specified by <i>type</i>, or null if
     * this object was not constructed from locale data.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @obsolete ICU 3.2 to be removed
     * @deprecated This API is obsolete.
     */
    public final ULocale getLocale(ULocale.Type type) {
        ULocale result = (type == ULocale.ACTUAL_LOCALE) ? actualLocale : validLocale;
        if (result == null) {
            return ULocale.ROOT;
        }
        return result;
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

    /*
     * The most specific locale containing any resource data, or null.
     */
    private ULocale validLocale;

    /*
     * The locale containing data used to construct this object, or null.
     */
    private ULocale actualLocale;

    // -------- END ULocale boilerplate --------
}

//eof
