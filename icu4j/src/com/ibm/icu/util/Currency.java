package com.ibm.icu.util;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.Serializable;
import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.text.DecimalFormatSymbols;

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

    /**
     * Returns a currency object for the default currency in the given
     * locale.
     */
    public static Currency getInstance(Locale locale) {
        // Look up the CurrencyElements resource for this locale.
        // It contains: [0] = currency symbol, e.g. "$";
        // [1] = intl. currency symbol, e.g. "USD";
        // [2] = monetary decimal separator, e.g. ".".
        ResourceBundle rb = ICULocaleData.getLocaleElements(locale);
        String[] currencyElements = rb.getStringArray("CurrencyElements");
        return getInstance(currencyElements[1]);
    }

    /**
     * Returns a currency object given an ISO 4217 3-letter code.
     */
    public static Currency getInstance(String theISOCode) {
        return new Currency(theISOCode);
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
        // TODO Temporary implementation; Redo this
        int i = findData();
        return (i >= 0) ? Integer.parseInt(DATA[i+1]) : 2;
    }

    /**
     * Returns the rounding increment for this currency, or 0.0 if no
     * rounding is done by this currency.
     * @return the non-negative rounding increment, or 0.0 if none
     */
    public double getRoundingIncrement() {
        // TODO Temporary implementation; Redo this
        int i = findData();
        return (i >= 0) ? Double.parseDouble(DATA[i+2]) : 0.0;
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
     * TEMPORARY Internal function to look up currency data.
     */
    private int findData() {
        // TODO Temporary implementation; Redo this
        for (int i=0; i<DATA.length; i+=3) {
            int c = DATA[i].compareTo(isoCode);
            if (c == 0) {
                return i;
            } else if (c > 0) {
                break;
            }
        }
        return -1;
    }

    /**
     * TEMPORARY Static data block giving currency fraction digits and
     * rounding increments.  Currencies that are not listed have the
     * default fraction digits (2) and no rounding.
     */
    private static String[] DATA = {
        // TODO Temporary implementation; Redo this
        // Code, Fraction digits, Rounding increment
        "BYB", "0", "0",
        "CHF", "2", "0.25",
        "ESP", "0", "0",
        "IQD", "3", "0",
        "ITL", "0", "0",
        "JOD", "3", "0",
        "JPY", "0", "0",
        "KWD", "3", "0",
        "LUF", "0", "0",
        "LYD", "3", "0",
        "PTE", "0", "0",
        "PYG", "0", "0",
        "TND", "3", "0",
        "TRL", "0", "0",
    };
}

//eof
