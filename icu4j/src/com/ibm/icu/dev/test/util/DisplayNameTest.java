/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

public class DisplayNameTest extends TestFmwk {
    static final boolean SHOW_ALL = false;
    
    public static void main(String[] args) throws Exception {
        new DisplayNameTest().run(args);
    }
        
    interface DisplayNameGetter {
        public String get(ULocale locale, String code, Object context);
    }

    Map[] codeToName = new Map[10];
    {
        for (int k = 0; k < codeToName.length; ++k) codeToName[k] = new HashMap();
    }
    
    static final Object[] zoneFormats = {new Integer(0), new Integer(1), new Integer(2),
        new Integer(3), new Integer(4), new Integer(5), new Integer(6), new Integer(7)};
    static final Object[] currencyFormats = {new Integer(Currency.SYMBOL_NAME), new Integer(Currency.LONG_NAME)};
    static final Object[] NO_CONTEXT = {null};
    
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
                return ULocale.getDisplayScript("en_"+code, locale);
            }
        });
        check("Country", locale, countries, null, new DisplayNameGetter() {
            public String get(ULocale locale, String code, Object context) {
                // TODO This is kinda a hack; ought to be direct way.
                return ULocale.getDisplayCountry("en_"+code, locale);
            }
        });
        check("Currencies", locale, currencies, currencyFormats, new DisplayNameGetter() {
            public String get(ULocale locale, String code, Object context) {
                Currency s = Currency.getInstance(code);
                return s.getName(locale.toLocale(), ((Integer)context).intValue(), new boolean[1]);
            }
        });
        // comment this out, because the zone string information is lost
        // we'd have to access the resources directly to test them

        check("Zones", locale, zones, zoneFormats, new DisplayNameGetter() {
            // TODO replace once we have real API
            public String get(ULocale locale, String code, Object context) {
                return getZoneString(locale, code, ((Integer)context).intValue());
            }
        });

    }
    
    Map zoneData = new HashMap();
    
    private String getZoneString(ULocale locale, String olsonID, int item) {
        Map data = (Map)zoneData.get(locale);
        if (data == null) {
            data = new HashMap();
            ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(locale);
            ICUResourceBundle table = bundle.getWithFallback("zoneStrings");
            for (int i = 0; ; ++i) {
                ICUResourceBundle stringSet = table.get(i);
                if (stringSet == null) break;
                String key = stringSet.getString(0);
                ArrayList list = new ArrayList();
                for (int j = 1; ; ++j) {
                    String entry = stringSet.getString(j);
                    if (entry == null) break;
                    list.add(entry);
                }
                data.put(key, list.toArray(new String[list.size()]));
            }
            zoneData.put(locale, data);
        }
        String[] strings = (String[]) data.get(olsonID);
        if (strings == null || item >= strings.length) return olsonID;
        return strings[item];
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
      String[] codes, Object[] contextList, DisplayNameGetter getter) {
        if (contextList == null) contextList = NO_CONTEXT;
        for (int k = 0; k < contextList.length; ++k) codeToName[k].clear();
        for (int j = 0; j < codes.length; ++j) {
            String code = codes[j];
            for (int k = 0; k < contextList.length; ++k) {
                Object context = contextList[k];
                String name = getter.get(locale, code, context);
                if (name == null || name.length() == 0) {
                    errln(
                        locale + "[" + locale.getDisplayName(ULocale.ENGLISH) + "]"
                        + ": Null or Zero-Length Display Name " + type
                        + ((context != null) ? "\t(" + context + "]" : "")
                        + "\t" + code + "[" + getter.get(ULocale.ENGLISH, code, context) + "]"
                    );
                    continue;            
                }
                String otherCode = (String) codeToName[k].get(name);
                if (otherCode != null) {
                    errln(
                        locale + "[" + locale.getDisplayName(ULocale.ENGLISH) + "]"
                        + ": Display Names collide for " + type
                        + ((context != null) ? "\t(" + context + "]" : "")
                        + "\t" + code + "[" + getter.get(ULocale.ENGLISH, code, context) + "]"
                        + " & " + otherCode + "[" + getter.get(ULocale.ENGLISH, otherCode, context) + "]"
                        + "\t=> " + name
                    );
                } else {
                    codeToName[k].put(name, code);
                    if (SHOW_ALL) logln(
                        locale + "[" + locale.getDisplayName(ULocale.ENGLISH) + "]"
                        + "\t" + type 
                        + ((context != null) ? "\t(" + context + "]" : "")
                        + "\t" + code + "[" + getter.get(ULocale.ENGLISH, code, context) + "]"
                        + "\t=> " + name 
                    );
                }
            }
        }
    }
}