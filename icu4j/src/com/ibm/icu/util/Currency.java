/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/Currency.java,v $
 * $Date: 2003/06/03 18:49:35 $
 * $Revision: 1.16 $
 *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.LocaleUtility;

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
 * @draft ICU 2.2
 */
public class Currency implements Serializable {

    /**
     * ISO 4217 3-letter code.
     */
    private String isoCode;

    /**
     * Selector for getName() indicating a symbolic name for a
     * currency, such as "$" for USD.
     * @draft ICU 2.6
     */
    public static final int SYMBOL_NAME = 0;

    /**
     * Selector for ucurr_getName indicating the long name for a
     * currency, such as "US Dollar" for USD.
     * @draft ICU 2.6
     */
    public static final int LONG_NAME = 1;

    // begin registry stuff 

    // shim for service code
    /* package */ static abstract class ServiceShim {
        abstract Locale[] getAvailableLocales();
        abstract Currency createInstance(Locale l);
        abstract Object registerInstance(Currency c, Locale l);
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
                Class cls = Class.forName("com.ibm.icu.util.CurrencyServiceShim");
                shim = (ServiceShim)cls.newInstance();
            }
            catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
        return shim;
    }

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     * @draft ICU 2.2
     */
    public static Currency getInstance(Locale locale) {
        if (shim == null) {
            return createCurrency(locale);
        }
        return shim.createInstance(locale);
    }

    /**
     * Instantiate a currency from a resource bundle found in Locale loc.
     */
    /* package */ static Currency createCurrency(Locale loc) {
        String country = loc.getCountry();
        String variant = loc.getVariant();
        if (variant.equals("PREEURO") || variant.equals("EURO")) {
            country = country + '_' + variant;
        }
        ResourceBundle bundle = ICULocaleData.getLocaleElements(new Locale("", "", ""));
        Object[][] cm = (Object[][]) bundle.getObject("CurrencyMap");

        // Do a linear search
        String curriso = null;
        for (int i=0; i<cm.length; ++i) {
            if (country.equals((String) cm[i][0])) {
                curriso = (String) cm[i][1];
                break;
            }
        }
            
        return curriso != null ? new Currency(curriso) : null;
    }

    /**
     * Returns a currency object given an ISO 4217 3-letter code.
     * @draft ICU 2.2
     */
    public static Currency getInstance(String theISOCode) {
        return new Currency(theISOCode);
    }

    /**
     * Registers a new currency for the provided locale.  The returned object
     * is a key that can be used to unregister this currency object.
     * @draft ICU 2.6
     */
    public static Object registerInstance(Currency currency, Locale locale) {
        return getShim().registerInstance(currency, locale);
    }

    /**
     * Unregister the currency associated with this key (obtained from
     * registerInstance).
     * @draft ICU 2.6
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
     * @draft ICU 2.2
     */
    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICULocaleData.getAvailableLocales();
        } else {
            return shim.getAvailableLocales();
        }
    }

    // end registry stuff

    /**
     * Return a hashcode for this currency.
     * @draft ICU 2.2
     */
    public int hashCode() {
        return isoCode.hashCode();
    }

    /**
     * Return true if rhs is a Currency instance, 
     * is non-null, and has the same currency code.
     * @draft ICU 2.2
     */
    public boolean equals(Object rhs) {
        try {
            return equals((Currency)rhs);
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Return true if c is non-null and has the same currency code.
     * @draft ICU 2.2
     */
    public boolean equals(Currency c) {
        if (c == null) return false;
        if (c == this) return true;
        return c.getClass() == Currency.class &&
            this.isoCode.equals(c.isoCode);
    }

    /**
     * Returns the ISO 4217 3-letter code for this currency object.
     * @draft ICU 2.2
     */
    public String getCurrencyCode() {
        return isoCode;
    }

    /**
     * Returns the display name for the given currency in the
     * given locale.  For example, the display name for the USD
     * currency object in the en_US locale is "$".
     * @param locale locale in which to display currency
     * @param nameStyle selector for which kind of name to return
     * @param isChoiceFormat fill-in; isChoiceFormat[0] is set to true
     * if the returned value is a ChoiceFormat pattern; otherwise it
     * is set to false
     * @return display string for this currency.  If the resource data
     * contains no entry for this currency, then the ISO 4217 code is
     * returned.  If isChoiceFormat[0] is true, then the result is a
     * ChoiceFormat pattern.  Otherwise it is a static string.
     * @draft ICU 2.6
     */
    public String getName(Locale locale,
                          int nameStyle,
                          boolean[] isChoiceFormat) {

        // Look up the Currencies resource for the given locale.  The
        // Currencies locale data looks like this:
        //|en {
        //|  Currencies { 
        //|    USD { "US$", "US Dollar" }
        //|    CHF { "Sw F", "Swiss Franc" }
        //|    INR { "=0#Rs|1#Re|1<Rs", "=0#Rupees|1#Rupee|1<Rupees" }
        //|    //...
        //|  }
        //|}

        if (nameStyle < 0 || nameStyle > 1) {
            throw new IllegalArgumentException();
        }
    
        // In the future, resource bundles may implement multi-level
        // fallback.  That is, if a currency is not found in the en_US
        // Currencies data, then the en Currencies data will be searched.
        // Currently, if a Currencies datum exists in en_US and en, the
        // en_US entry hides that in en.

        // We want multi-level fallback for this resource, so we implement
        // it manually.

        String s = null;

        // Multi-level resource inheritance fallback loop
        while (locale != null) {
            ResourceBundle rb = ICULocaleData.getLocaleElements(locale);
            // We can't cast this to String[][]; the cast has to happen later
            try {
                Object[][] currencies = (Object[][]) rb.getObject("Currencies");
                // Do a linear search
                for (int i=0; i<currencies.length; ++i) {
                    if (isoCode.equals((String) currencies[i][0])) {
                        s = ((String[]) currencies[i][1])[nameStyle];
                        break;
                    }
                }                
            }
            catch (MissingResourceException e) {}

            // If we've succeeded we're done.  Otherwise, try to fallback.
            // If that fails (because we are already at root) then exit.
            if (s != null) {
                break;
            }
            locale = LocaleUtility.fallback(locale);
        }

        // Determine if this is a ChoiceFormat pattern.  One leading mark
        // indicates a ChoiceFormat.  Two indicates a static string that
        // starts with a mark.  In either case, the first mark is ignored,
        // if present.  Marks in the rest of the string have no special
        // meaning.
        isChoiceFormat[0] = false;
        if (s != null) {
            int i=0;
            while (i < s.length() && s.charAt(i) == '=' && i < 2) {
                ++i;
            }
            isChoiceFormat[0]= (i == 1);
            if (i != 0) {
                // Skip over first mark
                s = s.substring(1);
            }
            return s;
        }

        // If we fail to find a match, use the ISO 4217 code
        return isoCode;        
    }

    /**
     * Returns the number of the number of fraction digits that should
     * be displayed for this currency.
     * @return a non-negative number of fraction digits to be
     * displayed
     * @draft ICU 2.2
     */
    public int getDefaultFractionDigits() {
        return (findData())[0].intValue();
    }

    /**
     * Returns the rounding increment for this currency, or 0.0 if no
     * rounding is done by this currency.
     * @return the non-negative rounding increment, or 0.0 if none
     * @draft ICU 2.2
     */
    public double getRoundingIncrement() {
        Integer[] data = findData();

        int data1 = data[1].intValue(); // rounding increment

        // If there is no rounding return 0.0 to indicate no rounding.
        // This is the high-runner case, by far.
        if (data1 == 0) {
            return 0.0;
        }

        int data0 = data[0].intValue(); // fraction digits

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
     * @draft ICU 2.2
     */
    public String toString() {
        return isoCode;
    }

    /**
     * Constructs a currency object for the given ISO 4217 3-letter
     * code.  This constructor assumes that the code is valid.
     */
    private Currency(String theISOCode) {
        isoCode = theISOCode;
    }

    /**
     * Internal function to look up currency data.  Result is an array of
     * two Integers.  The first is the fraction digits.  The second is the
     * rounding increment, or 0 if none.  The rounding increment is in
     * units of 10^(-fraction_digits).
     */
    private Integer[] findData() {

        try {
            // Get CurrencyMeta resource out of root locale file.  [This may
            // move out of the root locale file later; if it does, update this
            // code.]
            ResourceBundle root = ICULocaleData.getLocaleElements("");

            Object[][] currencyMeta = (Object[][]) root.getObject("CurrencyMeta");

            Integer[] i = null;
            int defaultPos = -1;

            // Do a linear search for isoCode.  At the same time,
            // record the position of the DEFAULT meta data.  If the
            // meta data becomes large, make this faster.
            for (int j=0; j<currencyMeta.length; ++j) {
                Object[] row = currencyMeta[j];
                String s = (String) row[0];
                int c = isoCode.compareToIgnoreCase(s);
                if (c == 0) {
                    i = (Integer[]) row[1];
                    break;
                }
                if ("DEFAULT".equalsIgnoreCase(s)) {
                    defaultPos = j;
                }
                if (c < 0 && defaultPos >= 0) {
                    break;
                }
            }

            if (i == null && defaultPos >= 0) {
                i = (Integer[]) currencyMeta[defaultPos][1];
            }

            if (i != null && i.length >= 2) {
                return i;
            }
        }
        catch (MissingResourceException e) {}

        // Config/build error; return hard-coded defaults
        return LAST_RESORT_DATA;
    }

    // Default currency meta data of last resort.  We try to use the
    // defaults encoded in the meta data resource bundle.  If there is a
    // configuration/build error and these are not available, we use these
    // hard-coded defaults (which should be identical).
    private static final Integer[] LAST_RESORT_DATA =
        new Integer[] { new Integer(2), new Integer(0) };

    // POW10[i] = 10^i
    private static final int[] POW10 = { 1, 10, 100, 1000, 10000, 100000,
                                1000000, 10000000, 100000000, 1000000000 };
}

//eof
