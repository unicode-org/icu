/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * This class represents the information needed by 
 * DecimalFormat to format currency plural, 
 * such as "3.00 US dollars" or "1.00 US dollar". 
 * DecimalFormat creates for itself an instance of
 * CurrencyPluralInfo from its locale data.  
 * If you need to change any of these symbols, you can get the
 * CurrencyPluralInfo object from your 
 * DecimalFormat and modify it.
 *
 * Following are the information needed for currency plural format and parse:
 * locale information,
 * plural rule of the locale,
 * currency plural pattern of the locale.
 *
 * @draft ICU 4.2
 * @provisional This API might change or be removed in a future release.
 */

public class CurrencyPluralInfo implements Cloneable, Serializable {
    private static final long serialVersionUID = 1;

    /**
     * Create a CurrencyPluralInfo object for the default locale.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public CurrencyPluralInfo() {
        initialize( ULocale.getDefault() );
    }

    /**
     * Create a CurrencyPluralInfo object for the given locale.
     * @param locale the locale
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public CurrencyPluralInfo( Locale locale ) {
        initialize( ULocale.forLocale(locale) );
    }

    /**
     * Create a CurrencyPluralInfo object for the given locale.
     * @param locale the locale
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public CurrencyPluralInfo( ULocale locale ) {
        initialize( locale );
    }

    /**
     * Gets a CurrencyPluralInfo instance for the default locale.
     * 
     * @return A CurrencyPluralInfo instance.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static CurrencyPluralInfo getInstance() {
        return new CurrencyPluralInfo();
    }

    /**
     * Gets a CurrencyPluralInfo instance for the given locale.
     * 
     * @param locale the locale.
     * @return A CurrencyPluralInfo instance.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static CurrencyPluralInfo getInstance(Locale locale) {
        return new CurrencyPluralInfo(locale);
    }

    /**
     * Gets a CurrencyPluralInfo instance for the given locale.
     * 
     * @param locale the locale.
     * @return A CurrencyPluralInfo instance.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static CurrencyPluralInfo getInstance(ULocale locale) {
        return new CurrencyPluralInfo(locale);
    }

    /**
     * Gets plural rules of this locale, used for currency plural format
     *
     * @return plural rule
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public PluralRules getPluralRules() {
        return pluralRules;
    }

    /**
     * Given a plural count, gets currency plural pattern of this locale, 
     * used for currency plural format
     *
     * @param  pluralCount currency plural count
     * @return a currency plural pattern based on plural count
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public String getCurrencyPluralPattern(String pluralCount) {
        String currencyPluralPattern = 
            (String)pluralCountToCurrencyUnitPattern.get(pluralCount);
        if (currencyPluralPattern == null) {
            // fall back to "other"
            if (!pluralCount.equals("other")) {
                currencyPluralPattern = 
                    (String)pluralCountToCurrencyUnitPattern.get("other");
            }
            if (currencyPluralPattern == null) {
                // no currencyUnitPatterns defined, 
                // fallback to predefined defult.
                // This should never happen when ICU resource files are
                // available, since currencyUnitPattern of "other" is always
                // defined in root.
                currencyPluralPattern = defaultCurrencyPluralPattern;
            }
        }
        return currencyPluralPattern;
    }

    /**
     * Get locale 
     *
     * @return locale
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public ULocale getLocale() {
        return ulocale;
    }

    /**
     * Set plural rules.
     * The plural rule is set when CurrencyPluralInfo
     * instance is created.
     * You can call this method to reset plural rules only if you want
     * to modify the default plural rule of the locale.
     *
     * @param ruleDescription new plural rule description
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public void setPluralRules(String ruleDescription) {
        pluralRules = PluralRules.createRules(ruleDescription);
    }

    /**
     * Set currency plural patterns.
     * The currency plural pattern is set when CurrencyPluralInfo
     * instance is created.
     * You can call this method to reset currency plural patterns only if 
     * you want to modify the default currency plural pattern of the locale.
     *
     * @param pluralCount the plural count for which the currency pattern will 
     *                    be overridden.
     * @param pattern     the new currency plural pattern
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public void setCurrencyPluralPattern(String pluralCount, String pattern) {
        pluralCountToCurrencyUnitPattern.put(pluralCount, pattern);
    }

    /**
     * Set locale
     *
     * @param loc the new locale to set
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public void setLocale(ULocale loc) {
        ulocale = loc;
        initialize(loc);
    }

    /**
     * Standard override
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public Object clone() {
        try {
            CurrencyPluralInfo other = (CurrencyPluralInfo) super.clone();
            // locale is immutable
            other.ulocale = (ULocale)ulocale.clone();
            // plural rule is immutable
            //other.pluralRules = pluralRules;
            // clone content
            //other.pluralCountToCurrencyUnitPattern = pluralCountToCurrencyUnitPattern;
            other.pluralCountToCurrencyUnitPattern = new HashMap();
            Iterator iter = pluralCountToCurrencyUnitPattern.keySet().iterator();
            while (iter.hasNext()) {
                String pluralCount = (String)iter.next();
                String currencyPattern = (String)pluralCountToCurrencyUnitPattern.get(pluralCount);
                other.pluralCountToCurrencyUnitPattern.put(pluralCount, currencyPattern);
            }
            return other;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException();
        }
    }

    /**
     * Override equals
     *
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public boolean equals(Object a) {
        if (a instanceof CurrencyPluralInfo) {
            CurrencyPluralInfo other = (CurrencyPluralInfo)a;
            return pluralRules.equals(other.pluralRules) && 
                   pluralCountToCurrencyUnitPattern.equals(other.pluralCountToCurrencyUnitPattern);
        }
        return false;
    }

    /**
     * Given a number, returns the keyword of the first rule that applies 
     * to the number
     */
    String select(double number) {
        return pluralRules.select(number);
    }

    /**
     * Currency plural pattern iterator.
     *
     * @return a iterator on currency plural pattern key set.
     */
    Iterator pluralPatternIterator() {
        return pluralCountToCurrencyUnitPattern.keySet().iterator();
    }

    private void initialize(ULocale uloc) {
        ulocale = uloc;
        pluralRules = PluralRules.forLocale(uloc);
        setupCurrencyPluralPattern(uloc);
    }

   
    private void setupCurrencyPluralPattern(ULocale uloc) {
        pluralCountToCurrencyUnitPattern = new HashMap();
        Set pluralCountSet = new HashSet();
        ULocale parentLocale = uloc;
        String numberStylePattern = NumberFormat.getPattern(uloc, NumberFormat.NUMBERSTYLE);
        while (parentLocale != null) {
            try {
                ICUResourceBundle resource = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, parentLocale);
                ICUResourceBundle currencyRes = resource.getWithFallback("CurrencyUnitPatterns");
                int size = currencyRes.getSize();
                for (int index = 0; index < size; ++index) {
                    String pluralCount = currencyRes.get(index).getKey();
                    if (pluralCountSet.contains(pluralCount)) {
                        continue;
                    }
                    String pattern = currencyRes.get(index).getString();
                    // replace {0} with numberStylePattern
                    // and {1} with triple currency sign
                    String patternWithNumber = Utility.replace(pattern, "{0}", numberStylePattern);
                    String patternWithCurrencySign = Utility.replace(patternWithNumber, "{1}", tripleCurrencyStr);
                    pluralCountToCurrencyUnitPattern.put(pluralCount, patternWithCurrencySign);
                    pluralCountSet.add(pluralCount);
                }
            } catch (MissingResourceException e) {
            }
            parentLocale = parentLocale.getFallback();
        } 
    }



    //-------------------- private data member ---------------------
    //
    // triple currency sign char array
    private static final char[] tripleCurrencySign = {0xA4, 0xA4, 0xA4};
    // triple currency sign string
    private static final String tripleCurrencyStr = new String(tripleCurrencySign);

    // default currency plural pattern char array
    private static final char[] defaultCurrencyPluralPatternChar = {0, '.', '#', '#', ' ', 0xA4, 0xA4, 0xA4};
    // default currency plural pattern string
    private static final String defaultCurrencyPluralPattern = new String(defaultCurrencyPluralPatternChar);

    // map from plural count to currency plural pattern, for example
    // one (plural count) --> {0} {1} (currency plural pattern, 
    // in which, {0} is the amount number, and {1} is the currency plural name.
    private Map pluralCountToCurrencyUnitPattern = null;

    /*
     * The plural rule is used to format currency plural name,
     * for example: "3.00 US Dollars".
     * If there are 3 currency signs in the currency patttern,
     * the 3 currency signs will be replaced by currency plural name.
     */
    private PluralRules pluralRules = null;

    // locale
    private ULocale ulocale = null;
}
