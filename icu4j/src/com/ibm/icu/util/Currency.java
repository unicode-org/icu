/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/Currency.java,v $
 * $Date: 2002/09/07 00:15:35 $
 * $Revision: 1.5 $
 *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.Serializable;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.text.DecimalFormatSymbols;

import com.ibm.icu.impl.ICUService;
import com.ibm.icu.impl.ICUService.Key;
import com.ibm.icu.impl.ICUService.Factory;
import com.ibm.icu.impl.ICULocaleService;
import com.ibm.icu.impl.ICULocaleService.ICUResourceBundleFactory;

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
 * @since ICU 2.2
 */
public class Currency implements Serializable {

    /**
     * ISO 4217 3-letter code.
     */
    private String isoCode;

    private static ICULocaleService service;

    private static ICULocaleService getService() {
        if (service == null) {
            service = new ICULocaleService();

            class CurrencyFactory extends ICUResourceBundleFactory {
                CurrencyFactory() {
                    super ("LocaleElements", "CurrencyElements", true);
                }

                protected Object createFromBundle(ResourceBundle bundle, Key key) {
                    String[] ce = bundle.getStringArray("CurrencyElements");
                    return new Currency(ce[1]);
                }
            }
                
            service.registerFactory(new CurrencyFactory());
        }
        return service;
    }

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     */
    public static Currency getInstance(Locale locale) {
        return (Currency)getService().get(locale);
    }

    /**
     * Returns a currency object given an ISO 4217 3-letter code.
     */
    public static Currency getInstance(String theISOCode) {
        return new Currency(theISOCode);
    }

    /**
     * Registers a new currency for the provided locale.  The returned object
     * is a key that can be used to unregister this currency object.
     */
    public static Object register(Currency currency, Locale locale) {
        return getService().registerObject(currency, locale);
    }

    /**
     * Unregister the currency associated with this key (obtained from
     * registerInstance).
     */
    public static boolean unregister(Object registryKey) {
        return getService().unregisterFactory((Factory)registryKey);
    }

    /**
     * Return an array of the locales for which a currency
     * is defined.
     */
    public static Locale[] getAvailableLocales() {
        return getService().getAvailableLocales();
    }

    /**
     * Return a hashcode for this currency.
     */
    public int hashCode() {
        return isoCode.hashCode();
    }

    /**
     * Return true if rhs is a Currency instance, 
     * is non-null, and has the same currency code.
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
     */
    public boolean equals(Currency c) {
        if (c == null) return false;
        if (c == this) return true;
        return c.getClass() == Currency.class &&
            this.isoCode.equals(c.isoCode);
    }

    /**
     * Returns the ISO 4217 3-letter code for this currency object.
     */
    public String getCurrencyCode() {
        return isoCode;
    }

    /**
     * Returns the display string for this currency object in the
     * given locale.  For example, the display string for the USD
     * currency object in the en_US locale is "$".
     */
    public String getSymbol(Locale locale) {
        // Look up the Currencies resource for the given locale.  The
        // Currencies locale looks like this in the original C
        // resource file:
        //|en {
        //|  Currencies { 
        //|    USD { "$" }
        //|    CHF { "sFr" }
        //|    //...
        //|  }
        //|}
        ResourceBundle rb = ICULocaleData.getLocaleElements(locale);
        // We can't cast this to String[][]; the cast has to happen later
        try {
            Object[][] currencies = (Object[][]) rb.getObject("Currencies");
            // Do a linear search
            for (int i=0; i<currencies.length; ++i) {
                if (isoCode.equals((String) currencies[i][0])) {
                    return (String) currencies[i][1];
                }
            }
        }
        catch (MissingResourceException e) {}

        try {
            // Since the Currencies resource is not fully populated yet,
            // check to see if we can find what we want in the CurrencyElements
            // resource.
            String[] currencyElements = rb.getStringArray("CurrencyElements");
            if (currencyElements[1].equals(isoCode)) {
                return currencyElements[0];
            }
        }
        catch (MissingResourceException e2) {}

        // If we fail to find a match, use the full ISO code
        return isoCode;
    }

    /**
     * Returns the number of the number of fraction digits that should
     * be displayed for this currency.
     * @return a non-negative number of fraction digits to be
     * displayed
     */
    public int getDefaultFractionDigits() {
        return (findData())[0].intValue();
    }

    /**
     * Returns the rounding increment for this currency, or 0.0 if no
     * rounding is done by this currency.
     * @return the non-negative rounding increment, or 0.0 if none
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
