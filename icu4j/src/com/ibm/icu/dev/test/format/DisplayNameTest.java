/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class DisplayNameTest extends TestFmwk {
    static final boolean SHOW_ALL = true;
    
    public static void main(String[] args) throws Exception {
        new DisplayNameTest().run(args);
    }
        
    interface DisplayNameGetter {
        public String get(ULocale locale, String code, Object context);
    }

    Map test = new HashMap();
    static final String[] zoneFormats = {"zzz", "zzzz", "Z"};
    static final String[] currencyFormats = {"\u00a4", "\u00a4\u00a4"};
    
    static final Date JAN1 = new Date(2004-1900,0,1);
    static final Date JULY1 = new Date(2004-1900,6,1);

    String[] countries = addUnknown(ULocale.getISOCountries(),2);
    String[] languages = addUnknown(ULocale.getISOLanguages(),2);
    String[] zones = addUnknown(TimeZone.getAvailableIDs(),5);
    String[] scripts = addUnknown(getCodes(new ULocale("en","US",""), "Scripts"),4);
    // TODO fix once there is a way to get a list of all script codes
    String[] currencies = addUnknown(getCodes(new ULocale("en","",""), "Currencies"),3);
    // TODO fix once there is a way to get a list of all currency codes

    public void TestLocales() {
        ULocale[] locales = ULocale.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            checkLocale(locales[i]);
        }
    }

    public void TestEnglish() {
        checkLocale(ULocale.ENGLISH);
    }

    private void checkLocale(ULocale locale) {
        logln("Checking " + locale);
        check("Language", locale, languages, null, new DisplayNameGetter() {
            public String get(ULocale locale, String code, Object context) {
                return ULocale.getDisplayLanguage(code, locale);
            }
        });
        check("Script", locale, scripts, null, new DisplayNameGetter() {
            public String get(ULocale locale, String code, Object context) {
                // TODO This is kinda a hack; ought to be direct way.
                return ULocale.getDisplayCountry("en-"+code, locale);
            }
        });
        check("Country", locale, countries, null, new DisplayNameGetter() {
            public String get(ULocale locale, String code, Object context) {
                // TODO This is kinda a hack; ought to be direct way.
                return ULocale.getDisplayScript("en-"+code, locale);
            }
        });
        for (int j = 0; j < currencyFormats.length; ++j)
          check("Currencies", locale, currencies, currencyFormats[j], new DisplayNameGetter() {
            // TODO: fix once SimpleDateFormat takes a ULocale
            public String get(ULocale locale, String code, Object context) {
                DecimalFormat sdf = new DecimalFormat(context.toString(),
                    new DecimalFormatSymbols(locale.toLocale()));
                return sdf.format(0);
            }
        });
        // comment this out, because the zone string information is lost
        // we'd have to access the resources directly to test them
        if (false) for (int j = 0; j < zoneFormats.length; ++j) {
            check("Zones", locale, zones, zoneFormats[j], new DisplayNameGetter() {
                // TODO: fix once SimpleDateFormat takes a ULocale
                public String get(ULocale locale, String code, Object context) {
                    SimpleDateFormat sdf = new SimpleDateFormat(context.toString(), locale.toLocale());
                    sdf.setTimeZone(TimeZone.getTimeZone(code));
                    return sdf.format(JULY1);
                }
            });
            check("Zones", locale, zones, zoneFormats[j], new DisplayNameGetter() {
                // TODO: fix once SimpleDateFormat takes a ULocale
                public String get(ULocale locale, String code, Object context) {
                    SimpleDateFormat sdf = new SimpleDateFormat(context.toString(), locale.toLocale());
                    sdf.setTimeZone(TimeZone.getTimeZone(code));
                    return sdf.format(JAN1);
                }
            });
        }
    }
    
    /**
     * Hack to get code list
     * @return
     */
    private static String[] getCodes(ULocale locale, String tableName) {
        // TODO remove Ugly Hack
        // get stuff
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(locale);
        ICUResourceBundle table = bundle.getWithFallback(tableName);
        // copy into array
        ArrayList stuff = new ArrayList();
        for (Enumeration keys = table.getKeys(); keys.hasMoreElements();) {
            stuff.add(keys.nextElement());
        }
        String[] result = new String[stuff.size()];
        return (String[]) stuff.toArray(result);
        //return new String[] {"Latn", "Cyrl"};
    }

    /**
     * Add two unknown strings, just to make sure they get passed through without colliding
     * @param strings
     * @return
     */
    private String[] addUnknown(String[] strings, int len) {
        String[] result = new String[strings.length + 2];
        result[0] = "x1unknown".substring(0,len);
        result[1] = "y1nknown".substring(0,len);
        System.arraycopy(strings,0,result,2,strings.length);
        return result;
    }

    private void check(String type, ULocale locale, 
      String[] codes, Object context, DisplayNameGetter getter) {
        test.clear();
        for (int j = 0; j < codes.length; ++j) {
            String code = codes[j];
            String name = getter.get(locale, code, context);
            if (name == null || name.length() == 0) {
                errln(
                    locale // .getDisplayName(ULocale.ENGLISH) causes exception
                    + ": Null or Zero-Length Display Name " + type
                    + "\t" + code
                    + ((context != null) ? "\t(" + context + ")" : "")
                );
                continue;            
            }
            Object otherCode = test.get(name);
            if (otherCode != null) {
                errln(
                    locale // .getDisplayName(ULocale.ENGLISH) causes exception
                    + ": Display Names collide for " + type
                    + "\t" + code
                    + "\t" + otherCode
                    + "\t" + name
                    + ((context != null) ? "\t(" + context + ")" : "")
                );
            } else {
                test.put(name, code);
                if (SHOW_ALL) logln(
                    locale + "\t" + type + "\t" + code + "\t" + name 
                    + ((context != null) ? "\t(" + context + ")" : "")
                );
            }
        }
    }
}